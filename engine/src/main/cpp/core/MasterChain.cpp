#include "MasterChain.h"

namespace ps {

namespace {

float dbToGain(float db) { return std::pow(10.0f, db / 20.0f); }

// ClipOnly2's softening: hardness is the x for which x == cos(x), softness 1 - hardness.
constexpr float kHard = 0.7390851332151606f;
constexpr float kSoft = 0.260914866784839f;
// ClipOnly2 clips at 0.9549925859 and guards its input at +-4.0: the guard, relative to the level.
constexpr float kGuard = 4.0f / 0.9549925859f;
// The clipper leaves alone what stays this close to the held level (+0.5 dB).
constexpr float kSustain = 1.06f;
// The bass path reads a little high: a pure bass is held by it alone (no ripple from the peak path).
constexpr float kLfScale = 1.01f;
// 4x samples after ClipOnly2's last change during which its output (delayed to a frame) may
// still differ from its input.
constexpr int kTouch = Oversampler4x::kFactor + 2;
// Frames without a clip after which the residual's way down is skipped (it has rung out).
constexpr int kIdleFrames = 4096;
// Frames the clipper listens after a sample over its level (the 4x signal comes about two frames
// later and rings a little around it). Otherwise it lets everything through: a step under its
// level that rings over it at 4x is the limiter's.
constexpr int kArmFrames = 8;

}  // namespace

void MasterChain::ClipChannel::reset() {
    os.reset();
    last = 0.0f;
    level = 1.0f;
    wasPos = wasNeg = false;
    intermediate.fill(0.0f);
    upDelay.fill(0.0f);
    outDelay.fill(0.0f);
    upPos = outPos = 0;
    dry.fill(0.0f);
    dryPos = 0;
    touched = 0;
    idle = 0;
    downIdle = true;
}

void MasterChain::LowCutSection::set(double rate, double hz, double q) {
    const double w = 2.0 * 3.14159265358979 * hz / rate, cs = std::cos(w), alpha = std::sin(w) / (2.0 * q);
    const double a0 = 1.0 + alpha;
    b0 = (1.0 + cs) / 2.0 / a0;
    b1 = -(1.0 + cs) / a0;
    b2 = b0;
    a1 = -2.0 * cs / a0;
    a2 = (1.0 - alpha) / a0;
}

void MasterChain::prepare(int rate) {
    rate_ = rate > 0 ? rate : 48000;
    const float fs = static_cast<float>(rate_);
    lookAhead_ = lookAheadFor(rate_);
    latency_ = latencyFor(rate_);
    lfHold_ = std::min(static_cast<int>(std::lround(0.020f * fs)), kLfQueue - kMaxLookAhead - 8);
    holdFrames_ = static_cast<int>(std::lround(0.025f * fs));
    envAttack_ = 1.0f - std::exp(-1.0f / (0.008f * fs));
    envRelease_ = std::exp(-1.0f / (0.150f * fs));
    lpCoef_ = 1.0f - std::exp(-2.0f * 3.14159265f * 500.0f / fs);
    relFast_ = std::exp(-1.0f / (0.030f * fs));
    relSlow_ = std::exp(-1.0f / (0.150f * fs));
    avgCoef_ = 1.0f - std::exp(-1.0f / (1.0f * fs));
    maxClip_ = dbToGain(kMaxClipDb);
    dcR_ = std::exp(-2.0 * 3.14159265358979 * 3.0 / fs);  // 3 Hz: -0.1 dB at 20 Hz
    dcPostR_ = std::exp(-2.0 * 3.14159265358979 * 0.05 / fs);  // 0.05 Hz: a kick's 20 Hz tail untouched
    dcStep_ = 1.0f / (0.050f * fs);
    dcInL_ = dcInR_ = dcOutL_ = dcOutR_ = 0.0;
    dcPostInL_ = dcPostInR_ = dcPostOutL_ = dcPostOutR_ = 0.0;
    dcMix_ = driveHeld_ || targetActive() ? 1.0f : 0.0f;
    lowCutFilter_[0].set(fs, kLowCutHz, 0.54119610);
    lowCutFilter_[1].set(fs, kLowCutHz, 1.30656296);
    for (auto& f : lowCutFilter_) f.reset();
    lowMix_ = lowCut_ && dcMix_ > 0.0f ? 1.0f : 0.0f;
    // The estimate forgets the music it heard over about a minute of sound (one piece = 100 ms).
    forget_ = std::exp(-0.1 / 60.0);
    estHist_.setForget(forget_);
    // Drop mode: the loud part over about three minutes of music.
    const double loudForget = std::exp(-0.1 / 180.0);
    loudIn_.setForget(loudForget);
    estIn_.prepare(rate_);
    estOut_.prepare(rate_);
    meter_.prepare(rate_);
    resetEstimate();
    setCeilingDb(ceilingDb_);

    clipL_.reset();
    clipR_.reset();
    held_ = env_ = 0.0f;
    holdLeft_ = 0;
    armed_ = 0;
    frame_ = 0;
    delayL_.fill(0.0f);
    delayR_.fill(0.0f);
    clipRatio_.fill(1.0f);
    tpL_.reset();
    tpR_.reset();
    prevSegment_ = 0.0f;
    minHead_ = minTail_ = 0;
    lfHead_ = lfTail_ = 0;
    lpL_ = lpR_ = 0.0f;
    box_.fill(1.0f);
    boxSum_ = lookAhead_;
    boxPos_ = 0;
    nonUnity_ = 0;
    rel_ = avg_ = 1.0f;
    // The drive survives (a stream reopened keeps the level it had reached).
    gain_ = rampTarget_ = dbToGain(driveDb_);
    rampLeft_ = 0;
    drivePublished_.store(driveDb_, std::memory_order_relaxed);
}

void MasterChain::setCeilingDb(float db) {
    if (std::isnan(db)) return;
    ceilingDb_ = std::clamp(db, -6.0f, 0.0f);
    ceiling_ = dbToGain(ceilingDb_);
    aim_ = dbToGain(ceilingDb_ - kMarginDb);
    tpFloor_ = 0.4f * aim_;
    clipFloor_ = dbToGain(ceilingDb_ - kMarginDb + kClipAboveDb);
}

void MasterChain::setTarget(float lufs) {
    const float t = std::isnan(lufs) || lufs >= 0.0f ? NAN : std::clamp(lufs, -30.0f, -3.0f);
    const bool was = std::isfinite(target_), now = std::isfinite(t);
    if (was == now && (!now || t == target_)) return;
    const int glide = rate_ * 3 / 10;  // 300 ms: a change the user asked for, like a fader
    if (was && now) {
        // The estimate does not depend on the target: the drive moves by the difference at once.
        rampDrive(std::clamp(driveDb_ + (t - target_), kMinDriveDb, kMaxDriveDb), glide);
    } else if (now && mode_ == Mode::Drop && loudIn_.windows() >= 30 && !driveHeld_) {
        target_ = t;
        const double level = loudIn_.lufs();
        const double loss = std::clamp(static_cast<double>(lossPublished_.load(std::memory_order_relaxed)), 0.0, 24.0);
        if (level > -999.0) rampDrive(static_cast<float>(std::clamp(t - level + loss, double(kMinDriveDb), double(kMaxDriveDb))), glide);
    } else if (now && mode_ == Mode::Song && estHist_.blocks() >= 30 && !driveHeld_) {
        target_ = t;
        const double loss = sumDriven_ > 0.0 && sumOut_ > 0.0 ? std::clamp(10.0 * std::log10(sumDriven_ / sumOut_), 0.0, 24.0) : 0.0;
        rampDrive(static_cast<float>(std::clamp(t - estHist_.lufs() + loss, double(kMinDriveDb), double(kMaxDriveDb))), glide);
    }
    target_ = t;
}

void MasterChain::holdDrive(float db) {
    driveHeld_ = true;
    driveDb_ = std::clamp(std::isfinite(db) ? db : 0.0f, kMinDriveDb, kMaxDriveDb);
    gain_ = rampTarget_ = dbToGain(driveDb_);
    rampLeft_ = 0;
    drivePublished_.store(driveDb_, std::memory_order_relaxed);
}

void MasterChain::resetEstimate() {
    estIn_.reset();
    estOut_.reset();
    estHist_.reset();
    driveSq_ = 0.0;
    pieceDrive_.fill(1.0);
    pieceHead_ = 0;
    sumDriven_ = sumOut_ = 0.0;
    loudIn_.reset();
    lossNow_ = lossDriveDb_ = 0.0;
    lossWindows_ = 0;
    driveRing_.fill(1.0);
    driveRingHead_ = 0;
    settled_ = false;
    settledLevel_ = 1000.0;
    pendingKnown_ = NAN;
    knownLevel_ = NAN;
    lossPublished_.store(0.0f, std::memory_order_relaxed);
    loudPublished_.store(-1000.0f, std::memory_order_relaxed);
}

void MasterChain::setKnownDrive(float db) {
    if (!std::isfinite(db) || driveHeld_) return;
    // S6 (G5): known before ▶, while the target is not on yet (the app sends it with ▶): kept, and
    // taken the moment the target is (process()), instead of gliding back to 0 dB for want of one.
    if (!targetActive()) {
        pendingKnown_ = std::clamp(db, kMinDriveDb, kMaxDriveDb);
        return;
    }
    // One was waiting for the target that just came on (the same ▶): taken as fast as it would have been.
    const bool waited = std::isfinite(pendingKnown_);
    pendingKnown_ = NAN;
    rampDrive(std::clamp(db, kMinDriveDb, kMaxDriveDb), waited ? std::max(1, rate_ / 50) : rate_ * 3 / 10);
    // The export heard the whole song: only a loss past the cap (a louder mix since) moves it.
    settled_ = true;
    settledLevel_ = 1000.0;
}

void MasterChain::rampDrive(float db, int frames) {
    // Never past its range, whoever asks (a drive of -77 dB is a song faded out).
    db = std::clamp(std::isfinite(db) ? db : 0.0f, kMinDriveDb, kMaxDriveDb);
    driveDb_ = db;
    rampTarget_ = dbToGain(db);
    rampLeft_ = std::max(1, frames);
    gainStep_ = (rampTarget_ - gain_) / static_cast<float>(rampLeft_);
    drivePublished_.store(db, std::memory_order_relaxed);
}

MasterChain::Stats MasterChain::process(float* l, float* r, const float* direct, int n) {
    Stats stats;
    // A drive known before the target came on (S6): taken at once (20 ms), settled.
    if (std::isfinite(pendingKnown_) && targetActive() && !driveHeld_) {
        rampDrive(pendingKnown_, std::max(1, rate_ / 50));
        pendingKnown_ = NAN;
        settled_ = true;
        settledLevel_ = 1000.0;
    }
    // No target (any more): back to 0 dB, gliding.
    if (!driveHeld_ && !targetActive() && driveDb_ != 0.0f && rampLeft_ == 0) rampDrive(0.0f, rate_ * 3 / 10);
    for (int i = 0; i < n; ++i) {
        float xl = l[i], xr = r[i];
        // A non-finite or absurd sample (only from a broken source) must not reach the filters.
        if (!(std::fabs(xl) < 1.0e6f)) xl = 0.0f;
        if (!(std::fabs(xr) < 1.0e6f)) xr = 0.0f;
        // The DC blocker: always running (ready when a target comes), heard with one only.
        {
            const double yl = xl - dcInL_ + dcR_ * dcOutL_, yr = xr - dcInR_ + dcR_ * dcOutR_;
            dcInL_ = xl;
            dcInR_ = xr;
            dcOutL_ = yl;
            dcOutR_ = yr;
            const float want = driveHeld_ || targetActive() ? 1.0f : 0.0f;
            if (dcMix_ != want) dcMix_ = want > dcMix_ ? std::min(want, dcMix_ + dcStep_) : std::max(want, dcMix_ - dcStep_);
            if (dcMix_ > 0.0f) {
                xl += dcMix_ * (static_cast<float>(yl) - xl);
                xr += dcMix_ * (static_cast<float>(yr) - xr);
            }
        }
        // The low cut (mix version 3): always running, heard with a target and the low cut on.
        {
            double hl = xl, hr = xr;
            lowCutFilter_[0].process(hl, hr);
            lowCutFilter_[1].process(hl, hr);
            const float want = lowCut_ && (driveHeld_ || targetActive()) ? 1.0f : 0.0f;
            if (lowMix_ != want) lowMix_ = want > lowMix_ ? std::min(want, lowMix_ + dcStep_) : std::max(want, lowMix_ - dcStep_);
            if (lowMix_ > 0.0f) {
                xl += lowMix_ * (static_cast<float>(hl) - xl);
                xr += lowMix_ * (static_cast<float>(hr) - xr);
            }
        }
        // The estimate hears the music before the drive (K-weighted: deaf to DC).
        const bool piece = driveHeld_ ? false : estIn_.add(xl, xr);
        if (rampLeft_ > 0) {
            gain_ += gainStep_;
            if (--rampLeft_ == 0) gain_ = rampTarget_;
        }
        const float drive = limiterOn_ ? gain_ : 1.0f;
        driveSq_ += static_cast<double>(drive) * drive;
        if (drive != 1.0f) {
            xl *= drive;
            xr *= drive;
        }
        if (direct) {
            const float d = std::isfinite(direct[i]) ? direct[i] : 0.0f;
            xl += d;
            xr += d;
        }
        const float a = std::max(std::fabs(xl), std::fabs(xr));
        stats.inPeak = std::max(stats.inPeak, a);
        // The held level of the music: a peak hold, then a slow fall; reached in about 8 ms.
        if (a >= held_) {
            held_ = a;
            holdLeft_ = holdFrames_;
        } else if (holdLeft_ > 0) {
            --holdLeft_;
        } else {
            held_ = std::max(a, held_ * envRelease_);
        }
        env_ = held_ > env_ ? env_ + (held_ - env_) * envAttack_ : held_;
        const float level = std::max(clipFloor_, env_ * kSustain);
        if (limiterOn_ && a > level) armed_ = kArmFrames;
        else if (armed_ > 0) --armed_;
        const bool armed = armed_ > 0;
        float ratioL = 1.0f, ratioR = 1.0f;
        float yl = clip(clipL_, xl, level, armed, ratioL);
        float yr = clip(clipR_, xr, level, armed, ratioR);
        float gain = 1.0f;
        limit(yl, yr, std::min(ratioL, ratioR), gain);
        stats.minGain = std::min(stats.minGain, gain);
        l[i] = yl;
        r[i] = yr;
        if (!driveHeld_) estOut_.add(yl, yr);
        meter_.add(yl, yr);
        if (piece) estimate();
    }
    meter_.publish();
    return stats;
}

float MasterChain::clipOnly2(ClipChannel& c, float in, float level, bool armed) {
    // Airwindows ClipOnly2 (Chris Johnson, MIT), the "compressed chunk" of ClipOnly2Proc.cpp with
    // its fixed level 0.9549925859 made a parameter: untouched below the level, and around a clip
    // the samples are drawn toward it instead of cut flat. Not armed: no new clip, but the sample
    // after one still eases toward that clip's level.
    if (armed) c.level = level;
    else level = c.level;
    bool changed = false;
    if (armed) {
        const float guard = kGuard * level;
        in = std::clamp(in, -guard, guard);
    }
    if (c.wasPos) {
        c.last = in < c.last ? level * kHard + in * kSoft : level * kSoft + c.last * kHard;
        changed = true;
    }
    c.wasPos = false;
    if (armed && in > level) {
        c.wasPos = true;
        in = level * kHard + c.last * kSoft;
        changed = true;
    }
    if (c.wasNeg) {
        c.last = in > c.last ? -level * kHard + in * kSoft : -level * kSoft + c.last * kHard;
        changed = true;
    }
    c.wasNeg = false;
    if (armed && in < -level) {
        c.wasNeg = true;
        in = -level * kHard + c.last * kSoft;
        changed = true;
    }
    c.intermediate[kSpacing] = in;
    const float out = c.last;  // latency: one 44.1 kHz sample, kSpacing samples here
    for (int x = kSpacing; x > 0; --x) c.intermediate[static_cast<size_t>(x - 1)] = c.intermediate[static_cast<size_t>(x)];
    c.last = c.intermediate[0];
    if (changed) c.touched = kTouch;
    return out;
}

float MasterChain::clip(ClipChannel& c, float x, float level, bool armed, float& ratio) {
    // The input kClipDelay frames back: what comes out where nothing was clipped.
    c.dry[static_cast<size_t>(c.dryPos)] = x;
    const float dry = c.dry[static_cast<size_t>((c.dryPos - kClipDelay) & 7)];
    c.dryPos = (c.dryPos + 1) & 7;
    // Only the clipper's change is brought back down (band-limited, so its harmonics do not fold
    // back): what it leaves alone comes out exactly as it went in.
    float up[Oversampler4x::kFactor];
    c.os.up(x, up);
    float residual[Oversampler4x::kFactor];
    bool any = false;
    for (int k = 0; k < Oversampler4x::kFactor; ++k) {
        // The clip level follows the held level; a peak far over it loses kMaxClipDb, no more.
        const float lv = std::max(level, std::fabs(up[k]) / maxClip_);
        float out = clipOnly2(c, up[k], lv, armed);
        if constexpr (Oversampler4x::kFactor > kSpacing) {
            const float late = c.outDelay[static_cast<size_t>(c.outPos)];
            c.outDelay[static_cast<size_t>(c.outPos)] = out;
            if (++c.outPos == Oversampler4x::kFactor - kSpacing) c.outPos = 0;
            out = late;
        }
        const float before = c.upDelay[static_cast<size_t>(c.upPos)];
        c.upDelay[static_cast<size_t>(c.upPos)] = up[k];
        if (++c.upPos == Oversampler4x::kFactor) c.upPos = 0;
        if (c.touched > 0) {
            residual[k] = out - before;
            --c.touched;
            any = true;
        } else {
            residual[k] = 0.0f;
        }
    }
    if (any) {
        c.downIdle = false;
        c.idle = 0;
    } else if (!c.downIdle && ++c.idle >= kIdleFrames) {
        // Rung out: the way down starts again from silence at the next clip.
        c.os.resetDown();
        c.downIdle = true;
    }
    if (c.downIdle) return dry;
    const float y = dry + c.os.down(residual);
    // Near its level only (around a zero crossing the band-limited residual says nothing).
    if (std::fabs(dry) > 0.5f * level) ratio = std::min(1.0f, std::fabs(y) / std::fabs(dry));
    return y;
}

void MasterChain::limit(float& l, float& r, float clipRatio, float& gain) {
    constexpr int kMask = kRing - 1;
    constexpr int kD = TruePeakFilter::kDelay;
    const int64_t t = frame_++;
    delayL_[static_cast<size_t>(t & kMask)] = l;
    delayR_[static_cast<size_t>(t & kMask)] = r;
    clipRatio_[static_cast<size_t>(t & kMask)] = clipRatio;
    // Frame f = t - kD: its true peak (the points between it and its neighbours on both sides).
    const int64_t f = t - kD;
    const float segment = std::max(tpL_.pushRefinedGated(l, tpFloor_), tpR_.pushRefinedGated(r, tpFloor_));
    const float peak = std::max(segment, prevSegment_);
    prevSegment_ = segment;
    const float need = peak > aim_ ? aim_ / peak : 1.0f;
    // The lowest gain over the look-ahead (a monotonic queue of (frame, gain)).
    while (minTail_ != minHead_ && minGain_[static_cast<size_t>((minTail_ - 1) & (kQueue - 1))] >= need) minTail_ = (minTail_ - 1) & (kQueue - 1);
    minFrame_[static_cast<size_t>(minTail_)] = f;
    minGain_[static_cast<size_t>(minTail_)] = need;
    minTail_ = (minTail_ + 1) & (kQueue - 1);
    while (minFrame_[static_cast<size_t>(minHead_)] <= f - lookAhead_) minHead_ = (minHead_ + 1) & (kQueue - 1);
    const float held = minGain_[static_cast<size_t>(minHead_)];
    // The bass (under ~500 Hz) holds its gain 20 ms longer: a low wave's peaks come back before
    // the gain could rise between them, so it does not ripple at the wave's rate (distortion).
    const float sl = delayL_[static_cast<size_t>(f & kMask)], sr = delayR_[static_cast<size_t>(f & kMask)];
    lpL_ += lpCoef_ * (sl - lpL_);
    lpR_ += lpCoef_ * (sr - lpR_);
    const float low = std::max(std::fabs(lpL_), std::fabs(lpR_)) * kLfScale;
    const float needLow = low > aim_ ? aim_ / low : 1.0f;
    while (lfTail_ != lfHead_ && lfGain_[static_cast<size_t>((lfTail_ - 1) & (kLfQueue - 1))] >= needLow) lfTail_ = (lfTail_ - 1) & (kLfQueue - 1);
    lfFrame_[static_cast<size_t>(lfTail_)] = f;
    lfGain_[static_cast<size_t>(lfTail_)] = needLow;
    lfTail_ = (lfTail_ + 1) & (kLfQueue - 1);
    while (lfFrame_[static_cast<size_t>(lfHead_)] <= f - lookAhead_ - lfHold_) lfHead_ = (lfHead_ + 1) & (kLfQueue - 1);
    const float target = std::min(held, lfGain_[static_cast<size_t>(lfHead_)]);
    // Release: fast back to the music's usual reduction after a short peak, slow above it.
    if (target <= rel_) rel_ = target;
    else rel_ = target + (rel_ - target) * (rel_ < avg_ ? relFast_ : relSlow_);
    avg_ += (rel_ - avg_) * avgCoef_;
    // Averaged over the look-ahead: the gain glides down along it and is at most the peak's own
    // when the peak comes out (each frame of the average saw it in its window).
    const auto at = static_cast<size_t>(boxPos_);
    if (++boxPos_ == lookAhead_) boxPos_ = 0;
    nonUnity_ += (rel_ != 1.0f) - (box_[at] != 1.0f);
    boxSum_ += static_cast<double>(rel_) - static_cast<double>(box_[at]);
    box_[at] = rel_;
    if (nonUnity_ == 0) boxSum_ = lookAhead_;  // back at rest: exactly unity again
    const float smooth = nonUnity_ == 0 ? 1.0f : std::min(1.0f, static_cast<float>(boxSum_ / lookAhead_));
    const auto out = static_cast<size_t>((f - lookAhead_ + 1) & kMask);
    l = delayL_[out];
    r = delayR_[out];
    if (!limiterOn_) return;
    // The meters: the frame's whole reduction, the clipper's included.
    gain = smooth * clipRatio_[out];
    if (smooth != 1.0f) {
        l *= smooth;
        r *= smooth;
    }
    // The offset that shaving and limiting the taller side of an asymmetric wave leaves (a few
    // thousandths over a song), taken out slowly (0.05 Hz: the peaks move by 0.06 dB at most on a
    // kick-heavy song, under the aim's 0.2 dB margin) with a target: the bench holds DC under
    // 1e-3. Heard only with one: without, the output stays the input, delayed.
    {
        const double yl = l - dcPostInL_ + dcPostR_ * dcPostOutL_, yr = r - dcPostInR_ + dcPostR_ * dcPostOutR_;
        dcPostInL_ = l;
        dcPostInR_ = r;
        dcPostOutL_ = yl;
        dcPostOutR_ = yr;
        if (dcMix_ > 0.0f) {
            l += dcMix_ * (static_cast<float>(yl) - l);
            r += dcMix_ * (static_cast<float>(yr) - r);
        }
    }
    // Rounding in the running sum: never a hair over the ceiling.
    l = std::clamp(l, -ceiling_, ceiling_);
    r = std::clamp(r, -ceiling_, ceiling_);
}

void MasterChain::estimate() {
    const int piece = std::max(1, rate_ / 10);
    pieceDrive_[static_cast<size_t>(pieceHead_)] = driveSq_ / piece;
    pieceHead_ = (pieceHead_ + 1) & 3;
    driveRing_[static_cast<size_t>(driveRingHead_)] = driveSq_ / piece;
    driveRingHead_ = (driveRingHead_ + 1) % BlockLoudness::kRing;
    driveSq_ = 0.0;
    if (mode_ == Mode::Drop) {
        estimateDrop();
        return;
    }
    if (estIn_.pieces() < 4) return;
    const double in = estIn_.meanOfLast(4), out = estOut_.meanOfLast(4);
    const double drive2 = 0.25 * (pieceDrive_[0] + pieceDrive_[1] + pieceDrive_[2] + pieceDrive_[3]);
    estHist_.add(in);
    const double level = estHist_.lufs();
    // What the clipper and the limiter take off the driven music, over the same gated blocks.
    if (lufsOf(in) > GatedLoudness::kGateAbs && lufsOf(in) > level - 10.0) {
        sumDriven_ = sumDriven_ * forget_ + in * drive2;
        sumOut_ = sumOut_ * forget_ + out;
    }
    if (driveHeld_ || !targetActive() || estHist_.blocks() < 30) return;  // 3 s of music at least
    const double loss = sumDriven_ > 0.0 && sumOut_ > 0.0 ? std::clamp(10.0 * std::log10(sumDriven_ / sumOut_), 0.0, 24.0) : 0.0;
    lossPublished_.store(static_cast<float>(loss), std::memory_order_relaxed);
    double want = std::clamp(target_ - level + loss, double(kMinDriveDb), double(kMaxDriveDb));
    // Squashing already: no more drive (it would only add reduction, not loudness).
    if (loss >= kMaxLossDb && want > driveDb_) want = driveDb_;
    // 1 dB/s while the estimate is young (30 s), then 0.5 dB/s: a slow glide, never a pump.
    const double step = estHist_.blocks() < 300 ? 0.1 : 0.05;
    const float next = static_cast<float>(driveDb_ + std::clamp(want - driveDb_, -step, step));
    if (next != driveDb_) rampDrive(next, piece);
}

void MasterChain::estimateDrop() {
    // 3 s windows (the short-term loudness) of the music before the drive, and the loud part of
    // them (LoudPart: the level the target is about, remembered over minutes).
    constexpr int kWindow = BlockLoudness::kRing;
    if (estIn_.pieces() < kWindow) return;
    double drive2 = 0.0;
    for (double d : driveRing_) drive2 += d;
    drive2 /= kWindow;
    const double in = estIn_.meanOfLast(kWindow), out = estOut_.meanOfLast(kWindow);
    loudIn_.add(in);
    const double level = loudIn_.lufs();  // also sets the loud part's threshold
    loudPublished_.store(static_cast<float>(level), std::memory_order_relaxed);
    if (level < -999.0) return;
    // What the clipper and the limiter take off now: a window of the loud part, its driven music
    // against its own output, at the drive it was heard with. Never two loud parts remembered over
    // minutes (they kept the loss of a drive long gone: the drive then fell 2 dB/s without end,
    // the song faded out live after its first loud seconds).
    if (in > 0.0 && out > 0.0 && drive2 > 0.0 && lufsOf(in) >= loudIn_.threshold()) {
        const double now = std::clamp(10.0 * std::log10(in * drive2 / out), 0.0, 24.0);
        const double heardWith = 10.0 * std::log10(drive2);
        constexpr double kFollow = 0.2;  // about half a second of windows (they overlap over 3 s)
        if (lossWindows_ == 0) {
            lossNow_ = now;
            lossDriveDb_ = heardWith;
        } else {
            lossNow_ += kFollow * (now - lossNow_);
            lossDriveDb_ += kFollow * (heardWith - lossDriveDb_);
        }
        ++lossWindows_;
    }
    const double loss = lossWindows_ > 0 ? lossNow_ : 0.0;
    lossPublished_.store(static_cast<float>(loss), std::memory_order_relaxed);
    if (driveHeld_ || !targetActive() || loudIn_.windows() < 30) return;  // 3 s of windows at least
    double want = std::clamp(target_ - level + loss, double(kMinDriveDb), double(kMaxDriveDb));
    // Over the cap: down to the drive that takes the cap off, from the drive the loss was heard
    // with (not from the drive now: a loss that lags would push it down again and again).
    // S7 (a known level, mix version 4): not on a part quieter than the one the drive was found on.
    const bool quieter = settled_ && std::isfinite(knownLevel_) && level < knownLevel_ - 1.0;
    const bool over = loss > kMaxLossDropDb && !quieter;
    if (over) want = std::min(want, lossDriveDb_ - (loss - kMaxLossDropDb));
    else if (loss >= kMaxLossDropDb - 0.2 && want > driveDb_) want = driveDb_;
    // Settled (30 s of music heard, or the drive of an export): it no longer rises, so the mix
    // one is making is heard as it is; a louder part than the one it settled on (a Drop not heard
    // yet, a louder mix) still brings it down, as does a loss past the cap.
    if (!settled_ && loudIn_.windows() >= 300) {
        settled_ = true;
        settledLevel_ = level;
    }
    if (settled_) {
        if (want > driveDb_) want = driveDb_;
        else if (!over && level <= settledLevel_ + 0.3) want = driveDb_;
    }
    // 2 dB/s down (a Drop arrives), 1 dB/s up while it is young: a glide, never a pump.
    const double step = want < driveDb_ ? 0.2 : 0.1;
    const float next = static_cast<float>(std::clamp(driveDb_ + std::clamp(want - driveDb_, -step, step), double(kMinDriveDb), double(kMaxDriveDb)));
    if (next != driveDb_) rampDrive(next, std::max(1, rate_ / 10));
}

}  // namespace ps
