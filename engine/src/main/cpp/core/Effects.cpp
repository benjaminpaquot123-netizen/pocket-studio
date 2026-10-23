#include "Effects.h"
#include "Params.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {

constexpr double kPi = 3.14159265358979323846;

float dbToGain(float db) { return std::pow(10.0f, db / 20.0f); }

// 0..1 -> lo..hi on a log scale.
float expMap(float v, float lo, float hi) { return lo * std::pow(hi / lo, std::clamp(v, 0.0f, 1.0f)); }

float onePoleCoef(float rate, float hz) { return 1.0f - std::exp(-2.0f * static_cast<float>(kPi) * hz / rate); }

}  // namespace

// --- Biquad ----------------------------------------------------------------------------------

void Biquad::set(double b0, double b1, double b2, double a0, double a1, double a2) {
    b0_ = static_cast<float>(b0 / a0);
    b1_ = static_cast<float>(b1 / a0);
    b2_ = static_cast<float>(b2 / a0);
    a1_ = static_cast<float>(a1 / a0);
    a2_ = static_cast<float>(a2 / a0);
}

void Biquad::lowShelf(float rate, float freq, float gainDb) {
    const double a = std::pow(10.0, gainDb / 40.0), w = 2 * kPi * freq / rate;
    const double cs = std::cos(w), alpha = std::sin(w) / 2 * std::sqrt(2.0), sq = 2 * std::sqrt(a) * alpha;
    set(a * ((a + 1) - (a - 1) * cs + sq), 2 * a * ((a - 1) - (a + 1) * cs), a * ((a + 1) - (a - 1) * cs - sq),
        (a + 1) + (a - 1) * cs + sq, -2 * ((a - 1) + (a + 1) * cs), (a + 1) + (a - 1) * cs - sq);
}

void Biquad::highShelf(float rate, float freq, float gainDb) {
    const double a = std::pow(10.0, gainDb / 40.0), w = 2 * kPi * freq / rate;
    const double cs = std::cos(w), alpha = std::sin(w) / 2 * std::sqrt(2.0), sq = 2 * std::sqrt(a) * alpha;
    set(a * ((a + 1) + (a - 1) * cs + sq), -2 * a * ((a - 1) + (a + 1) * cs), a * ((a + 1) + (a - 1) * cs - sq),
        (a + 1) - (a - 1) * cs + sq, 2 * ((a - 1) - (a + 1) * cs), (a + 1) - (a - 1) * cs - sq);
}

void Biquad::peak(float rate, float freq, float q, float gainDb) {
    const double a = std::pow(10.0, gainDb / 40.0), w = 2 * kPi * freq / rate;
    const double cs = std::cos(w), alpha = std::sin(w) / (2 * q);
    set(1 + alpha * a, -2 * cs, 1 - alpha * a, 1 + alpha / a, -2 * cs, 1 - alpha / a);
}

void Biquad::lowPass(float rate, float freq) {
    const double w = 2 * kPi * freq / rate, cs = std::cos(w), alpha = std::sin(w) / (2 * 0.7071);
    set((1 - cs) / 2, 1 - cs, (1 - cs) / 2, 1 + alpha, -2 * cs, 1 - alpha);
}

void Biquad::highPass(float rate, float freq) {
    const double w = 2 * kPi * freq / rate, cs = std::cos(w), alpha = std::sin(w) / (2 * 0.7071);
    set((1 + cs) / 2, -(1 + cs), (1 + cs) / 2, 1 + alpha, -2 * cs, 1 - alpha);
}

void ParamEq::setParam(int p, float v) {
    if (p < 0 || p >= 4 || !std::isfinite(v)) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
    update();
}

void ParamEq::reset() {
    bellL_.reset();
    bellR_.reset();
    cutL_.reset();
    cutR_.reset();
}

void ParamEq::update() {
    const float f = std::min(expMap(p_[0], 20.0f, 20000.0f), rate_ * 0.45f);
    const float db = (p_[1] - 0.5f) * 36.0f, q = expMap(p_[2], 0.3f, 10.0f);
    bellL_.peak(rate_, f, q, db);
    bellR_.peak(rate_, f, q, db);
    if (p_[3] < 0.01f) {
        cutL_.bypass();
        cutR_.bypass();
    } else {
        cutL_.highPass(rate_, expMap(p_[3], 20.0f, 500.0f));
        cutR_.highPass(rate_, expMap(p_[3], 20.0f, 500.0f));
    }
}

void ParamEq::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        l[i] = bellL_.process(cutL_.process(l[i]));
        r[i] = bellR_.process(cutR_.process(r[i]));
    }
}

// --- EQ --------------------------------------------------------------------------------------

void Eq3::prepare(float rate) {
    rate_ = rate;
    update();
    reset();
}

void Eq3::setParam(int p, float v) {
    if (p < 0 || p >= 3 || !clampSetting(gain_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

void Eq3::reset() {
    for (auto& b : left_) b.reset();
    for (auto& b : right_) b.reset();
}

void Eq3::update() {
    const auto db = [&](int i) { return (gain_[static_cast<size_t>(i)] - 0.5f) * 24.0f; };
    for (auto* side : {&left_, &right_}) {
        (*side)[0].lowShelf(rate_, 100.0f, db(0));
        (*side)[1].peak(rate_, 1000.0f, 0.9f, db(1));
        (*side)[2].highShelf(rate_, 8000.0f, db(2));
    }
}

void Eq3::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        float a = l[i], b = r[i];
        for (int k = 0; k < 3; ++k) {
            a = left_[static_cast<size_t>(k)].process(a);
            b = right_[static_cast<size_t>(k)].process(b);
        }
        l[i] = a;
        r[i] = b;
    }
}

// --- Compressor ------------------------------------------------------------------------------

void Compressor::prepare(float rate) {
    rate_ = rate;
    update();
    reset();
}

void Compressor::setParam(int p, float v) {
    if (p < 0 || p >= kParams || !clampSetting(p_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

void Compressor::reset() {
    envDb_ = -120.0f;
    meanSquare_ = 0.0f;
    averageDb_ = 0.0f;
    reduction_ = 0.0f;
    detL_.reset();
    detR_.reset();
}

void Compressor::update() {
    threshold_ = -40.0f + 40.0f * p_[0];
    ratio_ = expMap(p_[1], 1.0f, 20.0f);
    const float attackMs = expMap(p_[2], 0.1f, 80.0f), releaseMs = expMap(p_[3], 20.0f, 800.0f);
    attack_ = std::exp(-1.0f / (0.001f * attackMs * rate_));
    release_ = std::exp(-1.0f / (0.001f * releaseMs * rate_));
    // The user's make-up only: nothing is added below the threshold (before phase 33 a fixed half
    // of the full-scale reduction was; Effects.kt writes it here for the projects saved then).
    makeup_ = dbToGain(24.0f * p_[4]);
    autoMakeup_ = p_[5] >= 0.5f;
    rms_ = p_[6] >= 0.5f;
    highPass_ = p_[7] >= 0.01f;
    if (highPass_) {
        const float hz = expMap(p_[7], 20.0f, 400.0f);
        detL_.highPass(rate_, hz);
        detR_.highPass(rate_, hz);
    }
    mix_ = p_[8];
    rmsCoef_ = 1.0f - std::exp(-1.0f / (0.010f * rate_));     // RMS over ~10 ms
    averageCoef_ = 1.0f - std::exp(-1.0f / (1.5f * rate_));  // auto make-up follows over ~1.5 s
}

void Compressor::process(float* l, float* r, int n) {
    constexpr float kKnee = 6.0f;
    float reduction = 0.0f;
    for (int i = 0; i < n; ++i) {
        // The detector hears the sound (high-passed: the kick no longer drives a bus).
        const float dl = highPass_ ? detL_.process(l[i]) : l[i];
        const float dr = highPass_ ? detR_.process(r[i]) : r[i];
        float inDb;
        if (rms_) {
            meanSquare_ += rmsCoef_ * (0.5f * (dl * dl + dr * dr) - meanSquare_);
            inDb = meanSquare_ > 1e-12f ? 10.0f * std::log10(meanSquare_) : -120.0f;
        } else {
            const float level = std::max(std::fabs(dl), std::fabs(dr));
            inDb = level > 1e-6f ? 20.0f * std::log10(level) : -120.0f;
        }
        envDb_ = inDb > envDb_ ? attack_ * envDb_ + (1.0f - attack_) * inDb : release_ * envDb_ + (1.0f - release_) * inDb;
        const float over = envDb_ - threshold_;
        float gr = 0.0f;  // dB, ≤ 0
        if (over >= kKnee / 2) {
            gr = over * (1.0f / ratio_ - 1.0f);
        } else if (over > -kKnee / 2) {
            const float x = over + kKnee / 2;
            gr = (1.0f / ratio_ - 1.0f) * x * x / (2.0f * kKnee);
        }
        reduction = std::min(reduction, gr);
        // « Auto »: the reduction made on average is given back (nothing when nothing is squeezed).
        // The average only follows while the compressor works: through a silence or a break it
        // holds, so the music comes back at the level it left (it used to fall to 0 in 1.5 s and
        // the sound swelled back over 1.5 s at every restart).
        if (gr < -0.05f) averageDb_ += averageCoef_ * (gr - averageDb_);
        const float g = dbToGain(autoMakeup_ ? gr - averageDb_ : gr) * makeup_;
        const float k = mix_ >= 1.0f ? g : 1.0f + mix_ * (g - 1.0f);  // parallel: the dry sound under it
        l[i] *= k;
        r[i] *= k;
    }
    reduction_ = reduction;
}

// --- Saturation ------------------------------------------------------------------------------

void Saturator::prepare(float rate) {
    rate_ = rate;
    glide_ = 1.0f - std::exp(-1.0f / (0.010f * rate));  // 10 ms
    update();
    reset();
}

void Saturator::setParam(int p, float v) {
    if (p < 0 || p >= kParams || !clampSetting(p_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

namespace {

// DaisySP's SoftClip (MIT, Utility/dsp.h): x (27 + x^2) / (27 + 9 x^2), +-1 past +-3; and its
// antiderivative x^2/18 + 4/3 ln((x^2 + 3) / 3), then a straight line past +-3.
struct SoftClipShape {
    double f(double x) const { return x <= -3.0 ? -1.0 : x >= 3.0 ? 1.0 : x * (27.0 + x * x) / (27.0 + 9.0 * x * x); }
    double F(double x) const {
        const double a = std::fabs(x);
        if (a >= 3.0) return 2.348392481493110 + (a - 3.0);  // F(3) = 1/2 + 4/3 ln 4
        return x * x / 18.0 + (4.0 / 3.0) * std::log((x * x + 3.0) / 3.0);
    }
};

}  // namespace

namespace {

// DaisySP Overdrive::SetDrive's gains (MIT) for [drive].
void overdriveGains(float drive, float& pre, float& post) {
    const float d = drive * 2.0f, d2 = d * d;
    const float preA = d * 0.5f, preB = d2 * d2 * d * 24.0f;
    pre = preA + (preB - preA) * d2;
    const float squashed = d * (2.0f - d);
    post = 1.0f / static_cast<float>(SoftClipShape{}.f(0.33 + squashed * (pre - 0.33)));
}

}  // namespace

void Saturator::setDrive(float drive) {
    drive_ = drive;
    overdriveGains(drive, pre_, post_);
}

void Saturator::update() {
    // DaisySP's drive above ~0.9 is a hard square: keep the useful range.
    driveTarget_ = std::clamp(0.05f + 0.8f * p_[0], 0.0f, 1.0f);
    // The tone at its top: open, no low-pass (below, 1.5 to 18 kHz, one pole, as always).
    toneCoef_ = p_[1] >= kOpenTone ? 1.0f : onePoleCoef(rate_, expMap(p_[1], 1500.0f, 18000.0f));
    mix_ = p_[2];
    level_ = p_[3] / 0.7f;
    // Compensation: the overdrive's gain (at the drive it glides to) on a sine at -15 dBFS RMS (a sound at a usual level; the
    // overdrive is memoryless, one period tells it) divided out. A light drive is linear there:
    // quieter sounds keep their level too.
    wetGain_ = 1.0f;
    if (p_[4] >= 0.5f) {
        constexpr int kPoints = 64;
        constexpr float kAmp = 0.25f;
        float in = 0.0f, out = 0.0f, pre = 1.0f, post = 1.0f;
        overdriveGains(driveTarget_, pre, post);
        const SoftClipShape curve;
        for (int k = 0; k < kPoints; ++k) {
            const float x = kAmp * std::sin(2.0f * static_cast<float>(kPi) * (static_cast<float>(k) + 0.5f) / kPoints);
            const float y = static_cast<float>(curve.f(static_cast<double>(x) * pre)) * post;
            in += x * x;
            out += y * y;
        }
        if (out > 1e-12f) wetGain_ = std::sqrt(in / out);
    }
}

void Saturator::process(float* l, float* r, int n) {
    const SoftClipShape curve;
    const bool open = toneCoef_ >= 1.0f;
    for (int i = 0; i < n; ++i) {
        if (drive_ != driveTarget_) setDrive(std::fabs(driveTarget_ - drive_) < 1e-5f ? driveTarget_ : drive_ + (driveTarget_ - drive_) * glide_);
        const float wetL = shL_.process(l[i], pre_, 0.0f, curve) * post_ * wetGain_;
        const float wetR = shR_.process(r[i], pre_, 0.0f, curve) * post_ * wetGain_;
        const float dryL = shL_.dry(), dryR = shR_.dry();
        if (open) {
            lpL_ = wetL;
            lpR_ = wetR;
        } else {
            lpL_ += toneCoef_ * (wetL - lpL_);
            lpR_ += toneCoef_ * (wetR - lpR_);
        }
        l[i] = (dryL + mix_ * (lpL_ - dryL)) * level_;
        r[i] = (dryR + mix_ * (lpR_ - dryR)) * level_;
    }
}

// --- Chorus ----------------------------------------------------------------------------------

void StereoChorus::prepare(float rate) {
    rate_ = rate;
    chorus_.Init(rate);
    update();
}

void StereoChorus::setParam(int p, float v) {
    if (p < 0 || p >= 4 || !clampSetting(p_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

void StereoChorus::update() {
    chorus_.SetLfoFreq(expMap(p_[0], 0.05f, 5.0f), expMap(p_[0], 0.05f, 5.0f) * 1.13f);
    chorus_.SetLfoDepth(p_[1]);
    chorus_.SetDelay(0.1f + 0.8f * p_[2]);
    chorus_.SetFeedback(0.15f);
    mix_ = p_[3];
}

void StereoChorus::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        chorus_.Process(0.5f * (l[i] + r[i]));
        l[i] += mix_ * (chorus_.GetLeft() - l[i]);
        r[i] += mix_ * (chorus_.GetRight() - r[i]);
    }
}

// --- Track macros ------------------------------------------------------------------------

void TrackMacros::prepare(float rate) {
    rate_ = rate;
    left_.Init(rate);
    right_.Init(rate);
    // DaisySP's built-in cubic drive makes the filter blow up near its top cutoff on loud input.
    left_.SetDrive(0.0f);
    right_.SetDrive(0.0f);
    drive_.prepare(rate);
    chorus_.prepare(rate);
    inPower_ = outPower_ = 0.0f;
    makeup_ = 1.0f;
    dirtOn_ = false;
    update();
    filterMixNow_ = filterMix_;
}

void TrackMacros::set(int macro, float v) {
    if (macro < 0 || macro >= kCount || !std::isfinite(v)) return;
    v_[static_cast<size_t>(macro)] = std::clamp(v, 0.0f, 1.0f);
    update();
}

void TrackMacros::update() {
    // Colour: below the middle a low-pass closes down to 150 Hz, above it a high-pass opens up to
    // 2.5 kHz; the first 5 % around the middle fade the filter in, so the rest point is clean.
    const float c = v_[Color] - 0.5f;
    highPass_ = c > 0.0f;
    filterMix_ = std::min(1.0f, std::fabs(c) * 20.0f);
    const float amount = std::fabs(c) * 2.0f;
    const float f = highPass_ ? expMap(amount, 20.0f, 2500.0f) : expMap(1.0f - amount, 150.0f, 18000.0f);
    left_.SetFreq(std::min(f, rate_ * 0.45f));
    right_.SetFreq(std::min(f, rate_ * 0.45f));
    left_.SetRes(0.25f);
    right_.SetRes(0.25f);
    // Dirt: more drive and a little darker; process() keeps the loudness of the clean sound.
    const float d = v_[Dirt];
    drive_.setParam(0, d);
    drive_.setParam(1, 0.75f - 0.25f * d);
    drive_.setParam(2, std::min(1.0f, d * 3.0f));
    drive_.setParam(3, 0.7f);
    // Motion: a chorus that gets deeper and a bit faster.
    const float m = v_[Motion];
    chorus_.setParam(0, 0.2f + 0.35f * m);
    chorus_.setParam(1, m);
    chorus_.setParam(2, 0.4f);
    chorus_.setParam(3, std::min(0.6f, m * 1.2f));
}

void TrackMacros::process(float* l, float* r, int n) {
    if (filterMix_ > 0.0f || filterMixNow_ > 0.0f) {
        const float step = 1.0f / (0.01f * rate_);  // 10 ms to fade the filter in or out
        for (int i = 0; i < n; ++i) {
            filterMixNow_ += std::clamp(filterMix_ - filterMixNow_, -step, step);
            left_.Process(l[i]);
            right_.Process(r[i]);
            const float a = highPass_ ? left_.High() : left_.Low();
            const float b = highPass_ ? right_.High() : right_.Low();
            l[i] += filterMixNow_ * (a - l[i]);
            r[i] += filterMixNow_ * (b - r[i]);
        }
    }
    // Dirt: runs on until its loudness makeup is back at 1, so turning it off never jumps.
    const bool dirt = v_[Dirt] > 0.005f;
    if (!dirt && !dirtOn_) {
        // fully off
    } else if (!dirt && std::fabs(makeup_ - 1.0f) < 0.01f) {
        dirtOn_ = false;
        makeup_ = 1.0f;
    } else {
        if (!dirtOn_) {
            dirtOn_ = true;
            inPower_ = outPower_ = 0.0f;
            makeup_ = 1.0f;
        }
        // Loudness follows the input (50 ms power averages): drive changes the colour, not the level.
        float in = 0.0f;
        for (int i = 0; i < n; ++i) in += l[i] * l[i] + r[i] * r[i];
        drive_.process(l, r, n);
        float out = 0.0f;
        for (int i = 0; i < n; ++i) out += l[i] * l[i] + r[i] * r[i];
        const float k = 1.0f - std::exp(-static_cast<float>(n) / (0.05f * rate_));
        inPower_ += k * (in / static_cast<float>(n) - inPower_);
        outPower_ += k * (out / static_cast<float>(n) - outPower_);
        const float target = outPower_ > 1e-9f ? std::clamp(std::sqrt(inPower_ / outPower_), 0.1f, 2.0f) : 1.0f;
        for (int i = 0; i < n; ++i) {
            makeup_ += 0.002f * (target - makeup_);
            l[i] *= makeup_;
            r[i] *= makeup_;
        }
    }
    if (v_[Motion] > 0.005f) chorus_.process(l, r, n);
}

// --- Filter ----------------------------------------------------------------------------------

// The filters run without DaisySP's internal drive (it diverged into NaN on a hot signal). That
// drive also tamed the resonant peak: without it a high resonance is up to about 10 dB louder,
// so the output is lowered by as much (10 dB at full resonance, 3.6 dB at 0.6).
float resonanceCompensation(float res) {
    return std::pow(10.0f, -10.0f * res * res / 20.0f);
}

void SweepFilter::prepare(float rate) {
    rate_ = rate;
    left_.Init(rate);
    right_.Init(rate);
    // No input drive: DaisySP's default saturates inside the filter and, on a hot master with a
    // high cutoff, diverges into NaN.
    left_.SetDrive(0.0f);
    right_.SetDrive(0.0f);
    update();
}

void SweepFilter::setParam(int p, float v) {
    if (p < 0 || p >= 4 || !clampSetting(p_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

void SweepFilter::update() {
    const float f = std::min(expMap(p_[0], 40.0f, 18000.0f), rate_ * 0.45f);
    left_.SetFreq(f);
    right_.SetFreq(f);
    left_.SetRes(0.95f * p_[1]);
    right_.SetRes(0.95f * p_[1]);
    gain_ = resonanceCompensation(0.95f * p_[1]);
    type_ = std::min(2, static_cast<int>(p_[2] * 3.0f));
    mix_ = p_[3];
}

void SweepFilter::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        left_.Process(l[i]);
        right_.Process(r[i]);
        const float a = gain_ * (type_ == 0 ? left_.Low() : type_ == 1 ? left_.Band() : left_.High());
        const float b = gain_ * (type_ == 0 ? right_.Low() : type_ == 1 ? right_.Band() : right_.High());
        l[i] += mix_ * (a - l[i]);
        r[i] += mix_ * (b - r[i]);
    }
}

// --- Sidechain pump --------------------------------------------------------------------------

void Ducker::prepare(float rate) {
    rate_ = rate;
    attack_ = 1.0f - std::exp(-1.0f / (0.002f * rate));  // 2 ms down, no click
    update();
    reset();
}

void Ducker::setParam(int p, float v) {
    if (p < 0 || p >= 4 || !clampSetting(p_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

void Ducker::update() {
    depth_ = p_[0];
    release_ = std::exp(-1.0f / (0.001f * expMap(p_[1], 50.0f, 600.0f) * rate_));
}

void Ducker::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        if (i == pending_) {
            target_ = 1.0f;
            pending_ = -1;
        }
        // Fast duck towards the target, then a smooth release back up.
        if (target_ > 0.0f) {
            env_ += attack_ * (target_ - env_);
            if (env_ > 0.99f) target_ = 0.0f;
        } else {
            env_ *= release_;
        }
        const float g = 1.0f - depth_ * env_;
        l[i] *= g;
        r[i] *= g;
    }
    if (pending_ >= n) pending_ -= n;
}

// The live performance effects (PerformanceFx, Isolator) are in LiveEffects.cpp.

// --- Light effects ---------------------------------------------------------------------------

namespace {
void setClamped(std::array<float, 4>& p, int i, float v) {
    if (i >= 0 && i < 4) clampSetting(p[static_cast<size_t>(i)], v, 0.0f, 1.0f);
}
constexpr double kTwoPiD = 2.0 * kPi;
}  // namespace

void Flanger::prepare(float rate) {
    rate_ = rate;
    const auto size = static_cast<size_t>(rate * 0.012f) + 4;  // 12 ms
    bufL_.assign(size, 0.0f);
    bufR_.assign(size, 0.0f);
    pos_ = 0;
}

void Flanger::setParam(int p, float v) { setClamped(p_, p, v); }

void Flanger::reset() {
    std::fill(bufL_.begin(), bufL_.end(), 0.0f);
    std::fill(bufR_.begin(), bufR_.end(), 0.0f);
}

float Flanger::read(const std::vector<float>& b, float delay) const {
    const auto size = static_cast<int>(b.size());
    float at = static_cast<float>(pos_) - delay;
    while (at < 0.0f) at += static_cast<float>(size);
    const int i0 = static_cast<int>(at);
    const float f = at - static_cast<float>(i0);
    const float a = b[static_cast<size_t>(i0 % size)], c = b[static_cast<size_t>((i0 + 1) % size)];
    return a + f * (c - a);
}

void Flanger::process(float* l, float* r, int n) {
    if (bufL_.empty()) return;
    const double inc = expMap(p_[0], 0.05f, 5.0f) / rate_;
    const float minD = rate_ * 0.001f, span = rate_ * 0.009f * p_[1];
    const float fb = 0.9f * p_[2], mix = p_[3];
    const auto size = static_cast<int>(bufL_.size());
    for (int i = 0; i < n; ++i) {
        const float lfoL = 0.5f + 0.5f * static_cast<float>(std::sin(kTwoPiD * phase_));
        const float lfoR = 0.5f + 0.5f * static_cast<float>(std::cos(kTwoPiD * phase_));
        const float dl = read(bufL_, minD + span * lfoL), dr = read(bufR_, minD + span * lfoR);
        bufL_[static_cast<size_t>(pos_)] = l[i] + fb * dl;
        bufR_[static_cast<size_t>(pos_)] = r[i] + fb * dr;
        pos_ = (pos_ + 1) % size;
        l[i] += mix * (0.5f * (l[i] + dl) - l[i]);
        r[i] += mix * (0.5f * (r[i] + dr) - r[i]);
        phase_ += inc;
        if (phase_ >= 1.0) phase_ -= 1.0;
    }
}

void Phaser::setParam(int p, float v) { setClamped(p_, p, v); }

void Phaser::reset() {
    zl_.fill(0.0f);
    zr_.fill(0.0f);
    fbL_ = fbR_ = 0.0f;
}

void Phaser::process(float* l, float* r, int n) {
    const double inc = expMap(p_[0], 0.05f, 5.0f) / rate_;
    const float depth = p_[1], fb = 0.85f * p_[2], mix = p_[3];
    for (int i = 0; i < n; ++i) {
        // The notches sweep 200 Hz .. 200 Hz x 2^(depth x 5), the right side a quarter turn later.
        const float lfoL = 0.5f + 0.5f * static_cast<float>(std::sin(kTwoPiD * phase_));
        const float lfoR = 0.5f + 0.5f * static_cast<float>(std::cos(kTwoPiD * phase_));
        auto coef = [&](float lfo) {
            const float f = 200.0f * std::exp2(depth * 5.0f * lfo);
            const float t = std::tan(static_cast<float>(kPi) * std::min(f, rate_ * 0.45f) / rate_);
            return (t - 1.0f) / (t + 1.0f);
        };
        const float al = coef(lfoL), ar = coef(lfoR);
        float xl = l[i] + fb * fbL_, xr = r[i] + fb * fbR_;
        for (int k = 0; k < kStages; ++k) {
            const float yl = al * xl + zl_[static_cast<size_t>(k)];
            zl_[static_cast<size_t>(k)] = xl - al * yl;
            xl = yl;
            const float yr = ar * xr + zr_[static_cast<size_t>(k)];
            zr_[static_cast<size_t>(k)] = xr - ar * yr;
            xr = yr;
        }
        fbL_ = xl;
        fbR_ = xr;
        l[i] += mix * (0.5f * (l[i] + xl) - l[i]);
        r[i] += mix * (0.5f * (r[i] + xr) - r[i]);
        phase_ += inc;
        if (phase_ >= 1.0) phase_ -= 1.0;
    }
}

void Gate::prepare(float rate) {
    rate_ = rate;
    holdFrames_ = static_cast<int>(rate * 0.05f);
    update();
}

void Gate::setParam(int p, float v) {
    setClamped(p_, p, v);
    update();
}

void Gate::update() {
    threshold_ = dbToGain(-80.0f + 80.0f * p_[0]);
    // Time constants as labelled: a one-pole at 1 / (2π τ).
    const float twoPi = 2.0f * static_cast<float>(kPi);
    attack_ = onePoleCoef(rate_, 1000.0f / (twoPi * expMap(p_[1], 0.1f, 50.0f)));
    release_ = onePoleCoef(rate_, 1000.0f / (twoPi * expMap(p_[2], 10.0f, 1000.0f)));
    floor_ = 1.0f - p_[3];
}

void Gate::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        const float x = std::max(std::fabs(l[i]), std::fabs(r[i]));
        env_ = x > env_ ? x : env_ + (x - env_) * 0.002f;  // peak follower, ~10 ms fall
        if (env_ >= threshold_) hold_ = holdFrames_;
        else if (hold_ > 0) --hold_;
        const float target = hold_ > 0 ? 1.0f : floor_;
        gain_ += (target - gain_) * (target > gain_ ? attack_ : release_);
        l[i] *= gain_;
        r[i] *= gain_;
    }
}

void Tremolo::setParam(int p, float v) { setClamped(p_, p, v); }

void Tremolo::process(float* l, float* r, int n) {
    const double inc = expMap(p_[0], 0.1f, 20.0f) / rate_;
    const float depth = p_[1], shape = p_[2], pan = p_[3];
    for (int i = 0; i < n; ++i) {
        const float s = static_cast<float>(std::sin(kTwoPiD * phase_));
        // From a sine to a soft square: the shape steepens the wave.
        const float wave = std::tanh(s * (1.0f + 9.0f * shape)) / std::tanh(1.0f + 9.0f * shape);
        const float g = 1.0f - depth * (0.5f - 0.5f * wave);
        const float gOpp = 1.0f - depth * (0.5f + 0.5f * wave);
        // pan 0: both sides move together; 1: they move in turn (auto-pan).
        l[i] *= g;
        r[i] *= g + pan * (gOpp - g);
        phase_ += inc;
        if (phase_ >= 1.0) phase_ -= 1.0;
    }
}

void Widener::setParam(int p, float v) { setClamped(p_, p, v); }

void Widener::process(float* l, float* r, int n) {
    const float width = 2.0f * p_[0];
    const bool monoBass = p_[1] > 0.01f;
    const float coef = monoBass ? onePoleCoef(rate_, expMap(p_[1], 40.0f, 300.0f)) : 0.0f;
    for (int i = 0; i < n; ++i) {
        const float m = 0.5f * (l[i] + r[i]);
        float s = 0.5f * (l[i] - r[i]);
        if (monoBass) {
            lowS_ += (s - lowS_) * coef;
            s -= lowS_;  // the side's lows go: bass stays in the middle
        }
        s *= width;
        l[i] = m + s;
        r[i] = m - s;
    }
}

void Bitcrusher::setParam(int p, float v) { setClamped(p_, p, v); }

void Bitcrusher::process(float* l, float* r, int n) {
    const float bits = 16.0f - 14.0f * p_[0];
    const float steps = std::exp2(bits - 1.0f);
    const int every = 1 + static_cast<int>(std::lround(31.0f * p_[1] * p_[1]));
    const float mix = p_[2], volume = 2.0f * p_[3] * p_[3];
    for (int i = 0; i < n; ++i) {
        if (count_ <= 0) {
            holdL_ = std::round(l[i] * steps) / steps;
            holdR_ = std::round(r[i] * steps) / steps;
            count_ = every;
        }
        --count_;
        l[i] = (l[i] + mix * (holdL_ - l[i])) * volume;
        r[i] = (r[i] + mix * (holdR_ - r[i])) * volume;
    }
}

// --- The crazy effects -----------------------------------------------------------------------

void RingMod::setParam(int p, float v) { setClamped(p_, p, v); }

void RingMod::process(float* l, float* r, int n) {
    const double base = expMap(p_[0], 30.0f, 2000.0f);
    const float mix = p_[1], swing = p_[2];
    const double lfoInc = expMap(p_[3], 0.1f, 8.0f) / rate_;
    for (int i = 0; i < n; ++i) {
        const double f = base * (1.0 + 0.5 * swing * std::sin(kTwoPiD * lfo_));
        const float c = static_cast<float>(std::sin(kTwoPiD * phase_));
        l[i] += mix * (l[i] * c - l[i]);
        r[i] += mix * (r[i] * c - r[i]);
        phase_ += f / rate_;
        if (phase_ >= 1.0) phase_ -= 1.0;
        lfo_ += lfoInc;
        if (lfo_ >= 1.0) lfo_ -= 1.0;
    }
}

void AutoWah::setParam(int p, float v) { setClamped(p_, p, v); }

void AutoWah::process(float* l, float* r, int n) {
    const float gain = expMap(p_[0], 1.0f, 40.0f);
    const float low = expMap(p_[1], 150.0f, 1200.0f);
    const float k = 2.0f - 1.9f * p_[2];  // damping: 2 (soft) .. 0.1 (sharp)
    const float mix = p_[3];
    const float attack = onePoleCoef(rate_, 1000.0f / (2.0f * static_cast<float>(kPi) * 2.0f));   // ~2 ms
    const float release = onePoleCoef(rate_, 1000.0f / (2.0f * static_cast<float>(kPi) * 80.0f)); // ~80 ms
    for (int i = 0; i < n; ++i) {
        const float x = std::max(std::fabs(l[i]), std::fabs(r[i]));
        env_ += (x - env_) * (x > env_ ? attack : release);
        // The cutoff only moves every 8 frames: tan() is the costly part.
        if (tick_-- <= 0) {
            tick_ = 7;
            cutoff_ = std::min(low * std::exp2(3.5f * std::min(1.0f, env_ * gain)), 0.45f * rate_);
            g_ = std::tan(static_cast<float>(kPi) * cutoff_ / rate_);
            a1_ = 1.0f / (1.0f + g_ * (g_ + k));
            a2_ = g_ * a1_;
        }
        const float g = g_, a1 = a1_, a2 = a2_;
        auto band = [&](float in, float& s1, float& s2) {
            const float v3 = in - s2;
            const float v1 = a1 * s1 + a2 * v3;
            const float v2 = s2 + a2 * s1 + g * a2 * v3;
            s1 = 2.0f * v1 - s1;
            s2 = 2.0f * v2 - s2;
            return k * v1;  // band-pass, 0 dB at its peak
        };
        const float wl = band(l[i], s1L_, s2L_), wr = band(r[i], s1R_, s2R_);
        l[i] += mix * (wl - l[i]);
        r[i] += mix * (wr - r[i]);
    }
}

void Vinyl::prepare(float rate) {
    rate_ = rate;
    const auto size = static_cast<size_t>(rate * 0.03f) + 4;  // 30 ms: room for the wow
    bufL_.assign(size, 0.0f);
    bufR_.assign(size, 0.0f);
    reset();
}

void Vinyl::setParam(int p, float v) { setClamped(p_, p, v); }

void Vinyl::reset() {
    std::fill(bufL_.begin(), bufL_.end(), 0.0f);
    std::fill(bufR_.begin(), bufR_.end(), 0.0f);
    write_ = 0;
    wow_ = flutter_ = 0.0;
    wowNow_ = p_[0];
    lp1L_ = lp2L_ = lp1R_ = lp2R_ = click_ = 0.0f;
}

float Vinyl::noise() {
    seed_ = seed_ * 1664525u + 1013904223u;  // its own generator: no shared state, no lock
    return static_cast<float>(seed_ >> 8) / 8388608.0f - 1.0f;
}

void Vinyl::process(float* l, float* r, int n) {
    if (bufL_.empty()) return;
    const size_t size = bufL_.size();
    const float crackle = p_[1], age = p_[2], mix = p_[3];
    const float wowGlide = onePoleCoef(rate_, 3.0f);  // ~50 ms
    const float tone = onePoleCoef(rate_, expMap(1.0f - age, 2500.0f, 18000.0f));
    const float clicks = crackle * crackle * 0.0012f * 48000.0f / rate_;  // chance per frame, same count per second at any rate
    const float clickDecay = std::exp(-1.0f / (0.0007f * rate_));          // a click dies in about a millisecond
    for (int i = 0; i < n; ++i) {
        bufL_[write_] = l[i];
        bufR_[write_] = r[i];
        // A warped record: a slow pitch wobble (0.55 Hz) and a faster flutter (6 Hz). No wow, no
        // delay at all (a fixed one would comb the dry sound and put the track late).
        wowNow_ += (p_[0] - wowNow_) * wowGlide;
        const double delay = wowNow_ * (0.0067 + 0.006 * std::sin(kTwoPiD * wow_) + 0.0006 * std::sin(kTwoPiD * flutter_)) * rate_;
        wow_ += 0.55 / rate_;
        flutter_ += 6.0 / rate_;
        if (wow_ >= 1.0) wow_ -= 1.0;
        if (flutter_ >= 1.0) flutter_ -= 1.0;
        const double readPos = static_cast<double>(write_) + static_cast<double>(size) - delay;
        const auto i0 = static_cast<size_t>(readPos) % size;  // delay < size: readPos stays positive
        const auto i1 = (i0 + 1) % size;
        const float t = static_cast<float>(readPos - std::floor(readPos));
        float wl = bufL_[i0] + t * (bufL_[i1] - bufL_[i0]);
        float wr = bufR_[i0] + t * (bufR_[i1] - bufR_[i0]);
        write_ = (write_ + 1) % size;
        // Dull and narrow with age, two gentle low-passes.
        lp1L_ += (wl - lp1L_) * tone; lp2L_ += (lp1L_ - lp2L_) * tone;
        lp1R_ += (wr - lp1R_) * tone; lp2R_ += (lp1R_ - lp2R_) * tone;
        const float mid = 0.5f * (lp2L_ + lp2R_);
        const float narrow = 0.6f * age;
        wl = lp2L_ + narrow * (mid - lp2L_);
        wr = lp2R_ + narrow * (mid - lp2R_);
        // Crackles: rare short clicks that die in a millisecond; a soft hiss under them.
        if (0.5f + 0.5f * noise() < clicks) click_ = 0.25f * crackle * noise();
        click_ *= clickDecay;
        const float hiss = 0.004f * age * noise();
        wl += click_ + hiss;
        wr += click_ + hiss;
        l[i] += mix * (wl - l[i]);
        r[i] += mix * (wr - r[i]);
    }
}

void Radio::setParam(int p, float v) { setClamped(p_, p, v); }

void Radio::process(float* l, float* r, int n) {
    const float narrow = p_[0], drive = p_[1], hiss = p_[2], mix = p_[3];
    const float lo = 250.0f + 650.0f * narrow, hi = 4000.0f - 2200.0f * narrow;
    const float hpCoef = 1.0f - onePoleCoef(rate_, lo), lpCoef = onePoleCoef(rate_, hi);
    // More grain, not more level: the output comes down as the drive goes up.
    const float push = 1.0f + 15.0f * drive, norm = 1.0f / std::tanh(push) / (1.0f + 4.0f * drive);
    for (int i = 0; i < n; ++i) {
        const float x = 0.5f * (l[i] + r[i]);  // a radio is mono
        // High-pass (one pole, twice) then low-pass (twice): the telephone band.
        hp1_ = hpCoef * (hp1_ + x - prevIn_);
        prevIn_ = x;
        hp2_ = hpCoef * (hp2_ + hp1_ - prevHp_);
        prevHp_ = hp1_;
        lp1_ += (hp2_ - lp1_) * lpCoef;
        lp2_ += (lp1_ - lp2_) * lpCoef;
        float y = std::tanh(lp2_ * push) * norm * 0.7f;
        // Static rides on the voice (none in the silences).
        env_ += (std::fabs(x) - env_) * 0.001f;
        seed_ = seed_ * 1664525u + 1013904223u;
        const float noise = static_cast<float>(seed_ >> 8) / 8388608.0f - 1.0f;
        y += hiss * 0.15f * noise * std::min(1.0f, env_ * 6.0f);
        l[i] += mix * (y - l[i]);
        r[i] += mix * (y - r[i]);
    }
}

namespace {
// One beat in four sixteenths: x plays, . is cut.
constexpr const char* kChopPatterns[Chopper::kPatterns] = {"x.x.", "xx.x", "x..x", "x.xx", ".x.x", "xxx.", "x...", "xx.."};
}  // namespace

void Chopper::setParam(int p, float v) { setClamped(p_, p, v); }

void Chopper::process(float* l, float* r, int n) {
    const char* pattern = kChopPatterns[std::min(kPatterns - 1, static_cast<int>(p_[0] * kPatterns))];
    const float depth = p_[1];
    const float smooth = onePoleCoef(rate_, 1000.0f / (2.0f * static_cast<float>(kPi) * expMap(p_[2], 1.0f, 30.0f)));
    const int split = p_[3] >= 0.5f ? 8 : 4;  // 32nds or sixteenths in a beat
    for (int i = 0; i < n; ++i) {
        if (i == pending_) {
            sinceBeat_ = 0;  // the song's beat: the pattern starts again here
        }
        const int step = static_cast<int>((sinceBeat_ % beatFrames_) * split / beatFrames_);
        const bool on = pattern[step % 4] == 'x';
        const float target = on ? 1.0f : 1.0f - depth;
        gain_ += (target - gain_) * smooth;
        l[i] *= gain_;
        r[i] *= gain_;
        ++sinceBeat_;
    }
    pending_ = -1;
}

// --- Insert slot -----------------------------------------------------------------------------

void InsertSlot::reset() {
    // Only the effect in place: the others' states are cleared when their type is set (setType).
    // A slot coming back in at a scene change thus clears one small state (the long units clear in
    // constant time), not the delay lines of every effect it could hold.
    resetState();
    if (echo_) echo_->reset();
    if (reverb_) reverb_->reset();
    if (tape_) tape_->reset();
}

void InsertSlot::prepare(float rate) {
    eq_.prepare(rate);
    comp_.prepare(rate);
    sat_.prepare(rate);
    chorus_.prepare(rate);
    filter_.prepare(rate);
    pump_.prepare(rate);
    flanger_.prepare(rate);
    phaser_.prepare(rate);
    gate_.prepare(rate);
    tremolo_.prepare(rate);
    widener_.prepare(rate);
    crusher_.prepare(rate);
    paramEq_.prepare(rate);
    robot_.prepare(rate);
    wah_.prepare(rate);
    vinyl_.prepare(rate);
    radio_.prepare(rate);
    chopper_.prepare(rate);
    distortion_.prepare(rate);
    autoPan_.prepare(rate);
    ott_.prepare(rate);
    shifter_.prepare(rate);
    wobble_.prepare(rate);
}

namespace {
// The engine's defaults of the long effects (the same as the app's), for a unit borrowed before
// any parameter came.
std::array<float, InsertSlot::kParams> longDefaults(int type) {
    switch (type) {
        case InsertSlot::EchoFx: return {0.42f, 0.4f, 0.55f, 0.35f};
        case InsertSlot::PingPongFx: return {0.25f, 0.45f, 0.55f, 0.35f};
        case InsertSlot::ReverbFx: return {0.6f, 0.5f, 0.1f, 0.3f};
        case InsertSlot::TapeStopFx: return {0.0f, 0.5f, 0.2f, 0.0f};
        default: return {0.0f, 0.0f, 0.0f, 0.0f};
    }
}
}  // namespace

void InsertSlot::releaseUnit() {
    if (pool_) {
        if (echo_) pool_->echoes.give(echo_);
        if (reverb_) pool_->reverbs.give(reverb_);
        if (tape_) pool_->tapeStops.give(tape_);
    }
    echo_ = nullptr;
    reverb_ = nullptr;
    tape_ = nullptr;
}

void InsertSlot::takeUnit() {
    if (!pool_ || !isLong(type_) || hasUnit()) return;
    switch (type_) {
        case EchoFx:
        case PingPongFx:
            echo_ = pool_->echoes.take();
            if (echo_) {
                echo_->setPingPong(type_ == PingPongFx);
                // take() has just reset it (once: no second clear in the callback).
                for (int k = 0; k < kParams; ++k) echo_->setParam(k, longParams_[static_cast<size_t>(k)]);
            }
            break;
        case ReverbFx:
            reverb_ = pool_->reverbs.take();
            if (reverb_) for (int k = 0; k < kParams; ++k) reverb_->setParam(k, longParams_[static_cast<size_t>(k)]);
            break;
        case TapeStopFx:
            tape_ = pool_->tapeStops.take();
            if (tape_) for (int k = 0; k < kParams; ++k) tape_->setParam(k, longParams_[static_cast<size_t>(k)]);
            break;
        default: break;
    }
}

void InsertSlot::setStepFrames(double framesPerSixteenth) {
    chopper_.setStepFrames(framesPerSixteenth);
    autoPan_.setStepFrames(framesPerSixteenth);
    wobble_.setStepFrames(framesPerSixteenth);
    if (echo_) echo_->setStepFrames(framesPerSixteenth);
}

float InsertSlot::echoFrames() const { return echo_ ? echo_->delayFrames() : 0.0f; }

void InsertSlot::resetState() {
    switch (type_) {
        case Eq: eq_.reset(); break;
        case Comp: comp_.reset(); break;
        case Saturation: sat_.reset(); break;
        case Chorus: chorus_.reset(); break;
        case Filter: filter_.reset(); break;
        case Pump: pump_.reset(); break;
        case FlangerFx: flanger_.reset(); break;
        case PhaserFx: phaser_.reset(); break;
        case GateFx: gate_.reset(); break;
        case TremoloFx: tremolo_.reset(); break;
        case WidenerFx: widener_.reset(); break;
        case Crusher: crusher_.reset(); break;
        case ParamEqFx: paramEq_.reset(); break;
        case RobotFx: robot_.reset(); break;
        case WahFx: wah_.reset(); break;
        case VinylFx: vinyl_.reset(); break;
        case RadioFx: radio_.reset(); break;
        case ChopperFx: chopper_.reset(); break;
        case DistortionFx: distortion_.reset(); break;
        case AutoPanFx: autoPan_.reset(); break;
        case OttFx: ott_.reset(); break;
        case FreqShiftFx: shifter_.reset(); break;
        case WobbleFx: wobble_.reset(); break;
        default: break;
    }
}

void InsertSlot::setType(int type) {
    if (type < 0 || type >= kTypes || type == type_) return;
    releaseUnit();
    type_ = type;
    if (isLong(type_)) {
        longParams_ = longDefaults(type_);
        takeUnit();
    }
    resetState();
}

void InsertSlot::restartLfo() {
    switch (type_) {
        case FlangerFx: flanger_.restartLfo(); break;
        case PhaserFx: phaser_.restartLfo(); break;
        case TremoloFx: tremolo_.restartLfo(); break;
        case RobotFx: robot_.restartLfo(); break;
        case VinylFx: vinyl_.restartLfo(); break;
        case FreqShiftFx: shifter_.restartLfo(); break;
        default: break;
    }
}

void InsertSlot::setParam(int p, float v) {
    if (isLong(type_) && p >= 0 && p < kParams && std::isfinite(v)) longParams_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
    switch (type_) {
        case Eq: eq_.setParam(p, v); break;
        case Comp: comp_.setParam(p, v); break;
        case Saturation: sat_.setParam(p, v); break;
        case Chorus: chorus_.setParam(p, v); break;
        case Filter: filter_.setParam(p, v); break;
        case Pump: pump_.setParam(p, v); break;
        case FlangerFx: flanger_.setParam(p, v); break;
        case PhaserFx: phaser_.setParam(p, v); break;
        case GateFx: gate_.setParam(p, v); break;
        case TremoloFx: tremolo_.setParam(p, v); break;
        case WidenerFx: widener_.setParam(p, v); break;
        case Crusher: crusher_.setParam(p, v); break;
        case ParamEqFx: paramEq_.setParam(p, v); break;
        case RobotFx: robot_.setParam(p, v); break;
        case WahFx: wah_.setParam(p, v); break;
        case VinylFx: vinyl_.setParam(p, v); break;
        case RadioFx: radio_.setParam(p, v); break;
        case ChopperFx: chopper_.setParam(p, v); break;
        case DistortionFx: distortion_.setParam(p, v); break;
        case AutoPanFx: autoPan_.setParam(p, v); break;
        case OttFx: ott_.setParam(p, v); break;
        case FreqShiftFx: shifter_.setParam(p, v); break;
        case WobbleFx: wobble_.setParam(p, v); break;
        case EchoFx:
        case PingPongFx: if (echo_) echo_->setParam(p, v); break;
        case ReverbFx: if (reverb_) reverb_->setParam(p, v); break;
        case TapeStopFx: if (tape_) tape_->setParam(p, v); break;
        default: break;
    }
}

void InsertSlot::process(float* l, float* r, int n) {
    switch (type_) {
        case Eq: eq_.process(l, r, n); break;
        case Comp: comp_.process(l, r, n); break;
        case Saturation: sat_.process(l, r, n); break;
        case Chorus: chorus_.process(l, r, n); break;
        case Filter: filter_.process(l, r, n); break;
        case Pump: pump_.process(l, r, n); break;
        case FlangerFx: flanger_.process(l, r, n); break;
        case PhaserFx: phaser_.process(l, r, n); break;
        case GateFx: gate_.process(l, r, n); break;
        case TremoloFx: tremolo_.process(l, r, n); break;
        case WidenerFx: widener_.process(l, r, n); break;
        case Crusher: crusher_.process(l, r, n); break;
        case ParamEqFx: paramEq_.process(l, r, n); break;
        case RobotFx: robot_.process(l, r, n); break;
        case WahFx: wah_.process(l, r, n); break;
        case VinylFx: vinyl_.process(l, r, n); break;
        case RadioFx: radio_.process(l, r, n); break;
        case ChopperFx: chopper_.process(l, r, n); break;
        case DistortionFx: distortion_.process(l, r, n); break;
        case AutoPanFx: autoPan_.process(l, r, n); break;
        case OttFx: ott_.process(l, r, n); break;
        case FreqShiftFx: shifter_.process(l, r, n); break;
        case WobbleFx: wobble_.process(l, r, n); break;
        case EchoFx:
        case PingPongFx: if (echo_) echo_->process(l, r, n); break;
        case ReverbFx: if (reverb_) reverb_->process(l, r, n); break;
        case TapeStopFx: if (tape_) tape_->process(l, r, n); break;
        default: break;
    }
}

// --- Reverb (Freeverb) -----------------------------------------------------------------------

namespace {
constexpr int kCombTuning[8] = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
constexpr int kAllpassTuning[4] = {556, 441, 341, 225};
constexpr int kStereoSpread = 23;
constexpr float kFixedGain = 0.015f;
}  // namespace

void Reverb::prepare(float rate) {
    rate_ = rate;
    const double scale = rate / 44100.0;
    for (int i = 0; i < 8; ++i) {
        combL_[static_cast<size_t>(i)].buf.assign(static_cast<size_t>(kCombTuning[i] * scale), 0.0f);
        combR_[static_cast<size_t>(i)].buf.assign(static_cast<size_t>((kCombTuning[i] + kStereoSpread) * scale), 0.0f);
    }
    for (int i = 0; i < 4; ++i) {
        apL_[static_cast<size_t>(i)].buf.assign(static_cast<size_t>(kAllpassTuning[i] * scale), 0.0f);
        apR_[static_cast<size_t>(i)].buf.assign(static_cast<size_t>((kAllpassTuning[i] + kStereoSpread) * scale), 0.0f);
    }
    pre_.assign(static_cast<size_t>(0.2f * rate) + 1, 0.0f);
    freezeStep_ = 1.0f / std::max(1.0f, 0.01f * rate);
    update();
    reset();
}

void Reverb::setParam(int p, float v) {
    if (p < 0 || p >= 4 || !clampSetting(p_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

void Reverb::update() {
    feedback_ = 0.7f + 0.28f * p_[0];
    damp1_ = 0.4f * p_[1];
    damp2_ = 1.0f - damp1_;
    const float width = p_[2];
    wet1_ = 3.0f * (width / 2 + 0.5f);  // Freeverb's scalewet
    wet2_ = 3.0f * ((1 - width) / 2);
    preLen_ = std::clamp<size_t>(static_cast<size_t>(p_[3] * 0.2f * rate_), 1, pre_.empty() ? 1 : pre_.size());
}

void Reverb::reset() {
    for (auto* set : {&combL_, &combR_}) {
        for (auto& c : *set) { std::fill(c.buf.begin(), c.buf.end(), 0.0f); c.idx = 0; c.store = 0; }
    }
    for (auto* set : {&apL_, &apR_}) {
        for (auto& a : *set) { std::fill(a.buf.begin(), a.buf.end(), 0.0f); a.idx = 0; }
    }
    std::fill(pre_.begin(), pre_.end(), 0.0f);
    preIdx_ = 0;
}

void Reverb::process(const float* inL, const float* inR, float* outL, float* outR, int n) {
    if (pre_.empty()) return;
    for (int i = 0; i < n; ++i) {
        // Frozen (Freeverb's freeze): no more input, the combs keep everything (feedback 1, no
        // damping); the switch glides over about 10 ms.
        freezeMix_ = std::clamp(freezeMix_ + (freeze_ ? freezeStep_ : -freezeStep_), 0.0f, 1.0f);
        const float feedback = feedback_ + freezeMix_ * (1.0f - feedback_);
        const float damp1 = damp1_ * (1.0f - freezeMix_), damp2 = 1.0f - damp1;
        // Pre-delay on the mono sum, then the tank.
        const float x = (inL[i] + inR[i]) * kFixedGain;
        const size_t readIdx = (preIdx_ + pre_.size() - preLen_) % pre_.size();
        const float input = (preLen_ <= 1 ? x : pre_[readIdx]) * (1.0f - freezeMix_);
        pre_[preIdx_] = x;
        preIdx_ = (preIdx_ + 1) % pre_.size();
        float l = 0.0f, r = 0.0f;
        for (size_t k = 0; k < 8; ++k) {
            for (int side = 0; side < 2; ++side) {
                Comb& c = side == 0 ? combL_[k] : combR_[k];
                const float out = c.buf[c.idx];
                c.store = out * damp2 + c.store * damp1;
                c.buf[c.idx] = input + c.store * feedback;
                if (++c.idx >= c.buf.size()) c.idx = 0;
                (side == 0 ? l : r) += out;
            }
        }
        for (size_t k = 0; k < 4; ++k) {
            for (int side = 0; side < 2; ++side) {
                Allpass& a = side == 0 ? apL_[k] : apR_[k];
                float& v = side == 0 ? l : r;
                const float bufOut = a.buf[a.idx];
                a.buf[a.idx] = v + bufOut * 0.5f;
                v = bufOut - v;
                if (++a.idx >= a.buf.size()) a.idx = 0;
            }
        }
        outL[i] += l * wet1_ + r * wet2_;
        outR[i] += r * wet1_ + l * wet2_;
    }
}

// --- Tempo delay -----------------------------------------------------------------------------

void TempoDelay::prepare(float rate, float maxSeconds) {
    rate_ = rate;
    bufL_.assign(static_cast<size_t>(maxSeconds * rate) + 4, 0.0f);
    bufR_.assign(bufL_.size(), 0.0f);
    smooth_ = 1.0f - std::exp(-1.0f / (0.05f * rate));  // 50 ms glide on time changes
    update();
    current_ = target_;
    reset();
}

void TempoDelay::setParam(int p, float v) {
    if (p < 0 || p >= 4 || !clampSetting(p_[static_cast<size_t>(p)], v, 0.0f, 1.0f)) return;
    update();
}

void TempoDelay::setStepFrames(double framesPerSixteenth) {
    if (framesPerSixteenth > 0 && std::fabs(framesPerSixteenth - stepFrames_) > 1e-6) {
        stepFrames_ = framesPerSixteenth;
        update();
    }
}

void TempoDelay::update() {
    static constexpr int kSteps[6] = {1, 2, 3, 4, 6, 8};  // 1/16, 1/8, 3/16, 1/4, 3/8, 1/2
    const int steps = kSteps[std::clamp(static_cast<int>(p_[0] * 6.0f), 0, 5)];
    const float maxFrames = static_cast<float>(bufL_.empty() ? 1 : bufL_.size() - 2);
    target_ = std::clamp(static_cast<float>(steps * stepFrames_), 1.0f, maxFrames);
    feedback_ = 0.9f * p_[1];
    pingPong_ = p_[2];
    toneCoef_ = onePoleCoef(rate_, expMap(p_[3], 800.0f, 16000.0f));
    if (fresh_) current_ = target_;
}

void TempoDelay::reset() {
    // No clearing: what was written before reads as silence (filled_), in constant time.
    write_ = 0;
    filled_ = 0;
    fresh_ = true;
    current_ = target_;  // nothing to glide from: the time is right from the first repeat
    lpL_ = lpR_ = 0.0f;
}

float TempoDelay::read(const std::vector<float>& buf, float delay) const {
    const size_t size = buf.size();
    const float pos = static_cast<float>(write_) - delay + static_cast<float>(size);
    const auto i0 = static_cast<size_t>(pos);
    const float frac = pos - static_cast<float>(i0);
    const size_t j0 = i0 % size, j1 = (i0 + 1) % size;
    float a = buf[j0], b = buf[j1];
    if (filled_ < size) {
        // Only the frames written since the reset count (age 1 = the last one written).
        const size_t age0 = (write_ + size - j0) % size, age1 = (write_ + size - j1) % size;
        if (age0 == 0 || age0 > filled_) a = 0.0f;
        if (age1 == 0 || age1 > filled_) b = 0.0f;
    }
    return a + frac * (b - a);
}

void TempoDelay::process(const float* inL, const float* inR, float* outL, float* outR, int n) {
    if (bufL_.empty()) return;
    if (n > 0) fresh_ = false;
    for (int i = 0; i < n; ++i) {
        current_ += smooth_ * (target_ - current_);
        const float dl = read(bufL_, current_), dr = read(bufR_, current_);
        lpL_ += toneCoef_ * (dl - lpL_);
        lpR_ += toneCoef_ * (dr - lpR_);
        // Ping-pong: the input enters on the left only and the repeats cross sides.
        const float mono = 0.5f * (inL[i] + inR[i]);
        const float feedL = pingPong_ * lpR_ + (1.0f - pingPong_) * lpL_;
        const float feedR = pingPong_ * lpL_ + (1.0f - pingPong_) * lpR_;
        bufL_[write_] = pingPong_ * mono + (1.0f - pingPong_) * inL[i] + feedback_ * feedL;
        bufR_[write_] = (1.0f - pingPong_) * inR[i] + feedback_ * feedR;
        if (++write_ >= bufL_.size()) write_ = 0;
        if (filled_ < bufL_.size()) ++filled_;
        outL[i] += dl;
        outR[i] += dr;
    }
}

}  // namespace ps
