// The Live page's performance effects on the master (PerformanceFx) and the DJ isolator.
// Same rules as Effects.cpp: prepare() may allocate, setParam()/process() never do (no lock, no
// log either). Every timing decision is made here, on the frame of a sequencer step.

#include "Effects.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {

float expMap(float v, float lo, float hi) { return lo * std::pow(hi / lo, std::clamp(v, 0.0f, 1.0f)); }

int indexOf(float v, int count) { return std::clamp(static_cast<int>(v * static_cast<float>(count)), 0, count - 1); }

// Repeat and chopper divisions in sixteenths, a bin of 1/8 each (the old four values 0.125,
// 0.375, 0.5 / 0.625 and 0.875 still mean 1/4, 1/8, 1/16 and 1/32).
constexpr double kDivisions[8] = {4.0, 4.0, 4.0 / 3.0, 2.0, 1.0, 1.0, 2.0 / 3.0, 0.5};
// Brake and backspin lengths in beats; the brake's restart (none, then 1/4, 1/2, 1 beat).
constexpr double kBrakeBeats[6] = {0.25, 0.5, 1.0, 2.0, 4.0, 8.0};
constexpr double kSpinBeats[4] = {0.5, 1.0, 2.0, 4.0};
constexpr double kRestartBeats[4] = {0.0, 0.25, 0.5, 1.0};
constexpr double kReverseSteps[3] = {1.0, 2.0, 4.0};
constexpr int kRiserBars[4] = {1, 2, 4, 8};
// A riser's end (and a reset during one) fades out over this long: noise up to 0.25 needs more than 1 ms.
constexpr float kRiserFade = 0.004f;

// White noise -1..1 (xorshift: no libc rand, no signed overflow).
float noise(uint32_t& s) {
    s ^= s << 13;
    s ^= s >> 17;
    s ^= s << 5;
    return static_cast<float>(s) * (2.0f / 4294967295.0f) - 1.0f;
}

void initFilter(daisysp::Svf& f, float rate) {
    f.Init(rate);
    f.SetDrive(0.0f);  // see SweepFilter::prepare
}

}  // namespace

// --- Isolator --------------------------------------------------------------------------------

namespace {
constexpr float kLowSplit = 250.0f, kHighSplit = 2500.0f;
}

void Isolator::prepare(float rate) {
    rate_ = rate;
    step_ = 1.0f / std::max(1.0f, 0.01f * rate);
    for (Split* s : {&left_, &right_}) {
        s->lp1a.lowPass(rate, kLowSplit);
        s->lp1b.lowPass(rate, kLowSplit);
        s->hp1a.highPass(rate, kLowSplit);
        s->hp1b.highPass(rate, kLowSplit);
        s->lp2a.lowPass(rate, kHighSplit);
        s->lp2b.lowPass(rate, kHighSplit);
        s->hp2a.highPass(rate, kHighSplit);
        s->hp2b.highPass(rate, kHighSplit);
    }
    reset();
}

void Isolator::reset() {
    for (Split* s : {&left_, &right_}) {
        for (Biquad* b : {&s->lp1a, &s->lp1b, &s->hp1a, &s->hp1b, &s->lp2a, &s->lp2b, &s->hp2a, &s->hp2b}) b->reset();
    }
    mix_ = 0.0f;
    gain_ = {1.0f, 1.0f, 1.0f};
}

void Isolator::process(float* l, float* r, int n) {
    if (!active()) return;
    // From rest: the split starts from silence, not from what it held long ago.
    if (mix_ <= 0.0f) {
        for (Split* s : {&left_, &right_}) {
            for (Biquad* b : {&s->lp1a, &s->lp1b, &s->hp1a, &s->hp1b, &s->lp2a, &s->lp2b, &s->hp2a, &s->hp2b}) b->reset();
        }
    }
    const float mixTarget = engaged() ? 1.0f : 0.0f;
    for (int i = 0; i < n; ++i) {
        for (int b = 0; b < kBands; ++b) {
            auto& g = gain_[static_cast<size_t>(b)];
            g = std::clamp(g + (kill_[static_cast<size_t>(b)] ? -step_ : step_), 0.0f, 1.0f);
        }
        // Back at rest: the bands come back first, then the split fades out.
        const bool bandsBack = gain_[0] >= 1.0f && gain_[1] >= 1.0f && gain_[2] >= 1.0f;
        mix_ = std::clamp(mix_ + (mixTarget > 0.0f || !bandsBack ? step_ : -step_), 0.0f, 1.0f);
        for (int side = 0; side < 2; ++side) {
            Split& s = side == 0 ? left_ : right_;
            float& x = side == 0 ? l[i] : r[i];
            const float low = s.lp1b.process(s.lp1a.process(x));
            const float rest = s.hp1b.process(s.hp1a.process(x));
            const float mid = s.lp2b.process(s.lp2a.process(rest));
            const float high = s.hp2b.process(s.hp2a.process(rest));
            const float split = gain_[0] * low + gain_[1] * mid + gain_[2] * high;
            x += mix_ * (split - x);
        }
    }
}

// --- Performance effects -----------------------------------------------------------------------

double PerformanceFx::divisionSteps(float v) { return kDivisions[indexOf(v, 8)]; }

PerformanceFx::PerformanceFx() {
    // The engine's defaults (the app sends the project's): the filter open, 1/16 repeats and
    // chops, a one-beat brake and backspin without restart, half time by the bar, a reverse of a
    // beat, risers of 4 bars, no grid (at once) and no release effect.
    settings_.fill(0.0f);
    settings_[Filter] = 0.5f;
    settings_[Resonance] = 0.3f;
    settings_[RepeatDivision] = 0.5f;
    settings_[BrakeLength] = 2.5f / 6.0f;
    settings_[BackspinLength] = 1.5f / 4.0f;
    settings_[ChopDivision] = 0.5f;
    settings_[HalfTimeLength] = 1.0f;
    settings_[RiserLength] = 2.5f / 4.0f;
    settings_[ReverseLength] = 2.5f / 3.0f;
}

void PerformanceFx::prepare(float rate) {
    rate_ = rate;
    ringL_.assign(static_cast<size_t>(kBufferSeconds * rate), 0.0f);
    ringR_.assign(ringL_.size(), 0.0f);
    loopL_.assign(static_cast<size_t>(kLoopSeconds * rate), 0.0f);
    loopR_.assign(loopL_.size(), 0.0f);
    frame_ = -1;
    fade_ = 1.0f / (0.01f * rate);
    buildCoef_ = 1.0f - std::exp(-1.0f / (0.03f * rate));  // the build-up knob glides (30 ms)
    dropFadeStep_ = 1.0f / (0.002f * rate);
    initFilter(svfL_, rate);
    initFilter(svfR_, rate);
    initFilter(riserL_, rate);
    initFilter(riserR_, rate);
    initFilter(buildL_, rate);
    initFilter(buildR_, rate);
    initFilter(buildNoiseL_, rate);
    initFilter(buildNoiseR_, rate);
    // Echo out: dotted eighths, a long tail (about 2 bars at 120 BPM), darker repeats, a little wide.
    echo_.prepare(rate);
    echo_.setParam(0, 0.4f);   // 3/16
    echo_.setParam(1, 0.62f);  // feedback 0.56
    echo_.setParam(2, 0.35f);
    echo_.setParam(3, 0.45f);
    echo_.setStepFrames(stepFrames_);
    isolator_.prepare(rate);
    applyFilter();  // Init left the filter with no damping: its settings back at once
}

void PerformanceFx::recover() {
    initFilter(svfL_, rate_);
    initFilter(svfR_, rate_);
    applyFilter();  // the finger may still be on the pad: its cutoff and resonance, not an undamped filter
    initFilter(riserL_, rate_);
    initFilter(riserR_, rate_);
    initFilter(buildL_, rate_);
    initFilter(buildR_, rate_);
    initFilter(buildNoiseL_, rate_);
    initFilter(buildNoiseR_, rate_);
    std::fill(ringL_.begin(), ringL_.end(), 0.0f);
    std::fill(ringR_.begin(), ringR_.end(), 0.0f);
    std::fill(loopL_.begin(), loopL_.end(), 0.0f);
    std::fill(loopR_.begin(), loopR_.end(), 0.0f);
    echo_.reset();
    echoAlive_ = 0;
    isolator_.reset();
    for (auto& k : kills_) isolator_.setKill(static_cast<int>(&k - kills_.data()), k.on);
}

void PerformanceFx::setClock(bool running) {
    clock_ = running;
    if (!running && dropPending_) reset(true);
}

void PerformanceFx::onStep(int offset, int step, int stepsPerBar) {
    stepAt_ = offset;
    pendingStep_ = step;
    if (stepsPerBar > 0) stepsPerBar_ = stepsPerBar;
}

void PerformanceFx::setParam(int p, float v) {
    if (p < 0 || p >= kParams || !std::isfinite(v)) return;
    v = std::clamp(v, 0.0f, 1.0f);
    const bool on = v > 0.5f;
    settings_[static_cast<size_t>(p)] = v;
    switch (p) {
        case Filter: {
            const bool was = filterOn_;
            filter_ = v;
            filterOn_ = std::fabs(v - 0.5f) > 0.02f;
            if (filterOn_ && !was) interruptRelease();
            break;
        }
        case Resonance: resonance_ = v; break;
        case Repeat: userRepeat_ = on; break;
        case RepeatDivision: division_ = v; break;
        case TapeStop: brake_.want = on; break;
        case Chop: chop_.want = on; break;
        case Backspin: spin_.want = on; break;
        case Reverse: reverse_.want = on; break;
        case HalfTime: half_.want = on; break;
        case EchoOut: echoOut_.want = on; break;
        case KillLow:
        case KillMid:
        case KillHigh: kills_[static_cast<size_t>(p - KillLow)].want = on; break;
        case Grid: {
            static constexpr int kGrid[3] = {0, 1, 4};  // off, 1/16, beat
            gridSteps_ = kGrid[indexOf(v, 3)];
            break;
        }
        case Riser:
            if (on && !riserWant_) { riserPending_ = true; pendingDown_ = false; }
            riserWant_ = on;
            break;
        case Downlifter:
            if (on && !downWant_) { riserPending_ = true; pendingDown_ = true; }
            downWant_ = on;
            break;
        case BuildUp:
            if (v > 0.0f && buildTarget_ <= 0.0f) interruptRelease();
            buildTarget_ = v;
            if (v > 0.0f) dropFade_ = 0.0f;  // raised again during the drop's fade: it carries on from there
            break;
        case Drop:
            // On the next bar line (at once with the transport stopped).
            if (on && !dropWant_) {
                if (clock_) dropPending_ = true;
                else reset(true);
            }
            dropWant_ = on;
            break;
        case ResetAll:
            if (on && !resetWant_) reset(false);
            resetWant_ = on;
            break;
        default: break;  // settings read where they are used; freeze and throws are the mixer's
    }
    if (p == Filter || p == Resonance) {
        if (filterOn_) applyFilter();
    }
}

void PerformanceFx::applyFilter() {
    // Below the middle: low-pass from 18 kHz down to 150 Hz; above: high-pass from 30 Hz up to 6 kHz.
    const float f = filter_ < 0.5f ? expMap(filter_ * 2.0f, 150.0f, 18000.0f) : expMap((filter_ - 0.5f) * 2.0f, 30.0f, 6000.0f);
    const float res = 0.9f * resonance_;
    svfL_.SetFreq(std::min(f, rate_ * 0.45f));
    svfR_.SetFreq(std::min(f, rate_ * 0.45f));
    svfL_.SetRes(res);
    svfR_.SetRes(res);
    filterGain_ = resonanceCompensation(res);
}

bool PerformanceFx::active() const {
    return filterOn_ || filterMix_ > 0.0f || userRepeat_ || repeating_ || brake_.want || brakeRunning_ || brakeMix_ > 0.0f ||
           restartPending_ || spin_.want || spinRunning_ || spinMix_ > 0.0f || chop_.want || chop_.on || chopGain_ < 1.0f ||
           reverse_.want || reverseMix_ > 0.0f || half_.want || halfMix_ > 0.0f || echoOut_.want || echoAuto_ || dry_ < 1.0f ||
           echoAlive_ > 0 || isolator_.active() || riserPending_ || riserRunning_ || buildTarget_ > 0.0f || build_ > 0.0f;
}

float PerformanceFx::readRing(const std::vector<float>& buf, double frame) const {
    const auto size = static_cast<double>(buf.size());
    double pos = std::fmod(frame, size);
    if (pos < 0) pos += size;
    const auto i0 = static_cast<size_t>(pos);
    const float frac = static_cast<float>(pos - static_cast<double>(i0));
    const float a = buf[i0 % buf.size()], b = buf[(i0 + 1) % buf.size()];
    return a + frac * (b - a);
}

double PerformanceFx::sinceLine(double steps) const {
    double since = sinceBeat_;  // triplets count from the beat
    if (steps == static_cast<double>(stepsPerBar_)) since = sinceBar_;
    else if (steps == 4.0) since = sinceBeat_;
    else if (steps == 2.0) since = sinceEighth_;
    else if (steps == 1.0 || steps == 0.5) since = sinceStep_;
    return std::fmod(since, std::max(1.0, steps * stepFrames_));
}

double PerformanceFx::beatsToFrames(double beats, double maxSeconds) const {
    return std::clamp(beats * 4.0 * stepFrames_, 64.0, maxSeconds * rate_);
}

void PerformanceFx::startRepeat(double steps) {
    const int capacity = static_cast<int>(loopL_.size());
    const int len = static_cast<int>(std::clamp(std::llround(steps * stepFrames_), 64LL, static_cast<long long>(capacity)));
    loopSteps_ = steps;
    pendingSteps_ = 0.0;
    loopLen_ = len;
    // On the grid: the slice starts on the division's last line (the music since then goes on to
    // its end, then repeats). Off the grid: the last division, ending now.
    const bool onGrid = clock_ && gridSteps_ > 0;
    const int elapsed = onGrid ? std::min(len - 1, static_cast<int>(sinceLine(steps))) : len;
    const int64_t first = frame_ - elapsed;
    for (int k = 0; k < elapsed; ++k) {
        loopL_[static_cast<size_t>(k)] = readRing(ringL_, static_cast<double>(first + k));
        loopR_[static_cast<size_t>(k)] = readRing(ringR_, static_cast<double>(first + k));
    }
    loopFilled_ = elapsed;
    loopPos_ = onGrid ? elapsed : 0;
    loopWrapped_ = !onGrid;
    repeating_ = true;
}

void PerformanceFx::startBrake() {
    brakeRunning_ = true;
    restarting_ = false;
    restartPending_ = false;
    brakePos_ = static_cast<double>(frame_);
    brakeT_ = 0.0;
    // Half its length is how far the head falls behind: within the ring.
    brakeLen_ = beatsToFrames(kBrakeBeats[indexOf(settings_[BrakeLength], 6)], 2.0 * (kBufferSeconds - 0.25f));
}

void PerformanceFx::startSpin() {
    spinRunning_ = true;
    spinPos_ = static_cast<double>(frame_);
    spinT_ = 0.0;
    // The head goes back about once its length while the music goes on: 2.5 lengths in the ring.
    spinLen_ = beatsToFrames(kSpinBeats[indexOf(settings_[BackspinLength], 4)], 1.5);
}

void PerformanceFx::startEcho() {
    if (echoAlive_ <= 0) echo_.reset();  // a tail long gone: no stale repeat (constant time)
    echoAlive_ = static_cast<int64_t>(12.0f * rate_);
}

void PerformanceFx::startRelease() {
    const int mode = indexOf(settings_[ReleaseMode], 4);
    if (mode == ReleaseCut) return;
    double frames = 4.0 * stepFrames_;
    if (mode == ReleaseEcho) {
        echoAuto_ = true;
        startEcho();
    } else if (mode == ReleaseBrake) {
        brakeAuto_ = true;
        startBrake();
        frames = brakeLen_;
    } else {
        spinAuto_ = true;
        startSpin();
        frames = spinLen_;
    }
    // The dry sound comes back on the first bar line once the transition is over.
    if (clock_ && step_ >= 0) {
        const auto done = step_ + static_cast<int64_t>(std::ceil((sinceStep_ + frames) / stepFrames_));
        returnStep_ = (done + stepsPerBar_ - 1) / stepsPerBar_ * stepsPerBar_;
        returnFrames_ = -1.0;
    } else {
        returnStep_ = -1;
        returnFrames_ = frames;
    }
}

void PerformanceFx::endRelease() {
    returnStep_ = -1;
    returnFrames_ = -1.0;
    echoAuto_ = false;
    if (brakeAuto_) {
        brakeAuto_ = false;
        if (!brake_.on) brakeRunning_ = false;
    }
    if (spinAuto_) {
        spinAuto_ = false;
        if (!spin_.on) spinRunning_ = false;
    }
}

// Another effect starting while a release effect plays (roll, let go, roll again): the release
// ends now with its 10 ms fades, so the new effect is heard at once, not under a dry sound fading
// out until the bar line. Song curves start it on the same line: the export does the same.
void PerformanceFx::interruptRelease() {
    if (returnStep_ >= 0 || returnFrames_ >= 0.0) endRelease();
}

void PerformanceFx::startRiser(bool down) {
    riserPending_ = false;
    riserRunning_ = true;
    riserEnding_ = false;
    riserOut_ = 1.0f;
    riserDown_ = down;
    riserT_ = 0.0;
    const int bars = kRiserBars[indexOf(settings_[RiserLength], 4)];
    riserTotal_ = std::max(1.0, static_cast<double>(bars) * stepsPerBar_ * stepFrames_);
    riserEndStep_ = clock_ && step_ >= 0 ? step_ + static_cast<int64_t>(bars) * stepsPerBar_ : -1;
    // The same noise every time (an export sounds like the take).
    seedL_ = 0x9E3779B9u;
    seedR_ = 0x7F4A7C15u;
    initFilter(riserL_, rate_);
    initFilter(riserR_, rate_);
    riserL_.SetRes(0.35f);
    riserR_.SetRes(0.35f);
    riserCount_ = 0;
}

void PerformanceFx::reset(bool hard) {
    userRepeat_ = false;
    for (Hold* h : {&repeat_, &brake_, &spin_, &chop_, &reverse_, &half_, &echoOut_}) *h = Hold{};
    for (auto& k : kills_) k = Hold{};
    for (int b = 0; b < Isolator::kBands; ++b) isolator_.setKill(b, false);
    pendingSteps_ = 0.0;
    brakeRunning_ = brakeAuto_ = restarting_ = restartPending_ = false;
    spinRunning_ = spinAuto_ = false;
    echoAuto_ = false;
    returnStep_ = -1;
    returnFrames_ = -1.0;
    riserPending_ = false;
    if (riserRunning_) riserEnding_ = true;
    buildTarget_ = 0.0f;
    if (hard && build_ > 0.0f) dropFade_ = 1.0f;  // the drop: the build-up gone on the bar (in 2 ms)
    filter_ = 0.5f;
    filterOn_ = false;
    resonance_ = 0.3f;
    settings_[Filter] = 0.5f;
    settings_[Resonance] = 0.3f;
    dropPending_ = false;
}

void PerformanceFx::onLine() {
    // A step's frame: what waits for a bar line (the drop, a release effect's end, a riser's end).
    if (dropPending_ && barLine()) reset(true);
    if (returnStep_ >= 0 && step_ >= returnStep_) endRelease();
    // Its bar line: the riser is over (its last millisecond has faded out just before).
    if (riserRunning_ && riserEndStep_ >= 0 && step_ >= riserEndStep_) riserRunning_ = false;
}

void PerformanceFx::updateHolds() {
    const bool now = due(gridSteps_);
    // Kills switch on the grid both ways (« basses coupées, basses de retour » on the beat).
    for (size_t b = 0; b < kills_.size(); ++b) {
        auto& k = kills_[b];
        if (k.want != k.on && now) {
            if (k.want) interruptRelease();
            k.on = k.want;
            isolator_.setKill(static_cast<int>(b), k.on);
        }
    }
    // Repeat: the performer's, or the build-up's (faster and faster rolls).
    const bool buildRepeat = build_ >= 0.55f;
    repeat_.want = userRepeat_ || buildRepeat;
    const double steps = userRepeat_ ? divisionSteps(division_)
                                     : build_ < 0.7f ? 4.0 : build_ < 0.82f ? 2.0 : build_ < 0.93f ? 1.0 : 0.5;
    if (repeat_.want && !repeat_.on && now) {
        interruptRelease();
        repeat_.on = true;
        repeat_.user = userRepeat_;
        fadeIn_ = 1.0f;
        startRepeat(steps);
    } else if (repeat_.on && repeat_.want) {
        repeat_.user |= userRepeat_;
        // Another division (a roll pad slid to): it takes over at the end of the slice playing.
        pendingSteps_ = steps != loopSteps_ ? steps : 0.0;
    } else if (repeat_.on && !repeat_.want) {
        const bool released = repeat_.user;
        repeat_ = Hold{};
        if (released) startRelease();
    }
    auto hold = [&](Hold& h, auto start, bool releasable) {
        if (h.want && !h.on && now) {
            interruptRelease();
            h.on = true;
            h.user = true;
            start();
        } else if (!h.want && h.on) {
            h.on = false;
            if (releasable) startRelease();
        }
    };
    hold(chop_, [&] { chopStart_ = frame_; }, true);
    hold(reverse_, [&] { reverseStart_ = frame_; }, true);
    hold(half_, [&] { halfStart_ = frame_; }, true);
    hold(echoOut_, [&] { startEcho(); }, false);
    hold(spin_, [&] { startSpin(); }, false);
    if (!spin_.on && spinRunning_ && !spinAuto_) spinRunning_ = false;
    // The brake: let go, it stops (fading back) or restarts like a tape on the next grid line.
    if (brake_.want && !brake_.on && now) {
        interruptRelease();
        brake_.on = true;
        startBrake();
    } else if (!brake_.want && brake_.on) {
        brake_.on = false;
        if (!brakeAuto_) {
            if (indexOf(settings_[BrakeRestart], 4) > 0 && brakeRunning_) restartPending_ = true;
            else brakeRunning_ = false;
        }
    }
    if (restartPending_ && now) {
        restartPending_ = false;
        restarting_ = true;
        restartFrom_ = static_cast<double>(frame_);
        restartT_ = 0.0;
        restartLen_ = beatsToFrames(kRestartBeats[indexOf(settings_[BrakeRestart], 4)], 2.0);
    }
    if (riserPending_ && (!clock_ || barLine())) startRiser(pendingDown_);
}

void PerformanceFx::process(float* l, float* r, int n) {
    if (ringL_.empty()) return;
    const auto ringSize = static_cast<int64_t>(ringL_.size());
    for (int i = 0; i < n; ++i) {
        // --- The clock: a step on this frame.
        stepHere_ = i == stepAt_;
        if (stepHere_) {
            step_ = pendingStep_ >= 0 ? pendingStep_ : step_ + 1;
            sinceStep_ = 0.0;
            if (step_ % 2 == 0) sinceEighth_ = 0.0;
            if (step_ % 4 == 0) sinceBeat_ = 0.0;
            if (step_ % stepsPerBar_ == 0) sinceBar_ = 0.0;
        }
        // --- Everything heard goes into the ring first: the heads read it back.
        ++frame_;
        const auto w = static_cast<size_t>(frame_ % ringSize);
        const float inL = l[i], inR = r[i];
        ringL_[w] = std::isfinite(inL) ? inL : 0.0f;
        ringR_[w] = std::isfinite(inR) ? inR : 0.0f;
        // --- Switches due on this frame.
        if (stepHere_) onLine();
        if (returnFrames_ >= 0.0) {
            returnFrames_ -= 1.0;
            if (returnFrames_ < 0.0) endRelease();
        }
        updateHolds();
        float xl = ringL_[w], xr = ringR_[w];

        // --- Repeat: the slice, filled live up to its end, then looped.
        repeatMix_ = std::clamp(repeatMix_ + (repeat_.on ? fade_ : -fade_), 0.0f, 1.0f);
        if (repeating_) {
            if (loopPos_ == loopFilled_ && loopFilled_ < loopLen_) {
                loopL_[static_cast<size_t>(loopFilled_)] = xl;
                loopR_[static_cast<size_t>(loopFilled_)] = xr;
                ++loopFilled_;
            }
            // Short fades at the seams (each turn after the first, and into another division).
            const int edge = std::max(1, std::min(240, loopLen_ / 4));
            float seam = 1.0f;
            if (loopPos_ > loopLen_ - edge) seam = static_cast<float>(loopLen_ - loopPos_) / static_cast<float>(edge);
            else if (loopWrapped_ && loopPos_ < edge) seam = static_cast<float>(loopPos_) / static_cast<float>(edge);
            fadeIn_ = std::min(1.0f, fadeIn_ + 1.0f / static_cast<float>(edge));
            seam *= fadeIn_;
            const auto p = static_cast<size_t>(std::min(loopPos_, loopFilled_ - 1));
            const float rl = loopL_[p] * seam, rr = loopR_[p] * seam;
            xl += repeatMix_ * (rl - xl);
            xr += repeatMix_ * (rr - xr);
            if (++loopPos_ >= loopLen_) {
                loopPos_ = 0;
                loopWrapped_ = true;
                if (pendingSteps_ > 0.0 && repeat_.on) {
                    // At the seam (faded out): the new division from its own last line.
                    startRepeat(pendingSteps_);
                    fadeIn_ = 0.0f;
                }
            }
            if (!repeat_.on && repeatMix_ <= 0.0f) repeating_ = false;
        }

        // --- Reverse: the division before, played backwards, division after division.
        reverseMix_ = std::clamp(reverseMix_ + (reverse_.on ? fade_ : -fade_), 0.0f, 1.0f);
        if (reverseMix_ > 0.0f) {
            const double steps = kReverseSteps[indexOf(settings_[ReverseLength], 3)];
            const double len = std::clamp(steps * stepFrames_, 64.0, 1.9 * rate_);
            const double e = clock_ ? std::fmod(sinceLine(steps), len) : std::fmod(static_cast<double>(frame_ - reverseStart_), len);
            const double src = static_cast<double>(frame_) - 2.0 * e - 1.0;
            const double edge = std::min(240.0, len / 8.0);
            const float seam = static_cast<float>(std::min(1.0, std::min(e, len - e) / edge));
            xl += reverseMix_ * (readRing(ringL_, src) * seam - xl);
            xr += reverseMix_ * (readRing(ringR_, src) * seam - xr);
        }

        // --- Half time: each beat (or bar) plays its first half at half speed.
        halfMix_ = std::clamp(halfMix_ + (half_.on ? fade_ : -fade_), 0.0f, 1.0f);
        if (halfMix_ > 0.0f) {
            double steps = settings_[HalfTimeLength] >= 0.5f ? static_cast<double>(stepsPerBar_) : 4.0;
            if (steps * stepFrames_ * 0.5 > (kBufferSeconds - 0.5) * rate_) steps = 4.0;
            const double len = std::max(64.0, steps * stepFrames_);
            const double e = clock_ ? std::fmod(sinceLine(steps), len) : std::fmod(static_cast<double>(frame_ - halfStart_), len);
            const double src = static_cast<double>(frame_) - 0.5 * e;
            const double edge = std::min(240.0, len / 16.0);
            const float seam = static_cast<float>(std::min(1.0, std::min(e, len - e) / edge));
            xl += halfMix_ * (readRing(ringL_, src) * seam - xl);
            xr += halfMix_ * (readRing(ringR_, src) * seam - xr);
        }

        // --- Backspin: the record pulled back fast, slowing to a stop (then silence while held).
        spinMix_ = std::clamp(spinMix_ + (spinRunning_ ? 4.0f * fade_ : -fade_), 0.0f, 1.0f);
        if (spinMix_ > 0.0f) {
            const double grab = std::min(0.04 * rate_, 0.25 * spinLen_);
            double speed = 0.0;
            float level = 0.0f;
            if (spinT_ < grab) {
                speed = 1.0 - 4.0 * spinT_ / grab;
                level = 1.0f;
            } else if (spinT_ < spinLen_) {
                const double u = (spinT_ - grab) / std::max(1.0, spinLen_ - grab);
                speed = -3.0 * (1.0 - u) * (1.0 - u);
                level = static_cast<float>(std::min(1.0, -speed));
            }
            const float sl = readRing(ringL_, spinPos_) * level, sr = readRing(ringR_, spinPos_) * level;
            if (spinRunning_) {
                spinPos_ += speed;
                spinT_ += 1.0;
            }
            xl += spinMix_ * (sl - xl);
            xr += spinMix_ * (sr - xr);
        }

        // --- Brake: the tape slows to a halt behind the music; let go, it fades back or restarts.
        if (restarting_) {
            const double u = std::min(1.0, restartT_ / restartLen_);
            // Speed 3u - 2u³: from rest up past normal and back to it, so the head catches up
            // with the music exactly at the end (its position is then the live one).
            brakePos_ = restartFrom_ + restartLen_ * (1.5 * u * u - 0.5 * u * u * u * u);
            const float level = static_cast<float>(std::min(1.0, 3.0 * u - 2.0 * u * u * u));
            const float sl = readRing(ringL_, brakePos_) * level, sr = readRing(ringR_, brakePos_) * level;
            xl += brakeMix_ * (sl - xl);
            xr += brakeMix_ * (sr - xr);
            restartT_ += 1.0;
            if (u >= 1.0) {
                restarting_ = false;
                brakeRunning_ = false;
                brakeMix_ = 0.0f;  // the head is on the live frame: no fade needed
            }
        } else {
            brakeMix_ = std::clamp(brakeMix_ + (brakeRunning_ ? 4.0f * fade_ : -fade_), 0.0f, 1.0f);
            if (brakeMix_ > 0.0f) {
                const double speed = brakeRunning_ ? std::max(0.0, 1.0 - brakeT_ / brakeLen_) : 0.0;
                const float level = static_cast<float>(speed);
                const float sl = readRing(ringL_, brakePos_) * level, sr = readRing(ringR_, brakePos_) * level;
                if (brakeRunning_) {
                    brakePos_ += speed;
                    brakeT_ += 1.0;
                }
                xl += brakeMix_ * (sl - xl);
                xr += brakeMix_ * (sr - xr);
            }
        }

        // --- Chopper: on for the first half of each division, in time with the steps.
        if (chop_.on || chopGain_ < 1.0f) {
            const double steps = divisionSteps(settings_[ChopDivision]);
            const double len = std::max(2.0, steps * stepFrames_);
            const double e = std::fmod(sinceLine(steps), len);
            const float target = !chop_.on || e < 0.5 * len ? 1.0f : 0.0f;
            chopGain_ += std::clamp(target - chopGain_, -fade_ * 5, fade_ * 5);
            xl *= chopGain_;
            xr *= chopGain_;
        }

        // --- DJ filter: fades in when the pad is pressed, out when it is let go. A jump from the
        // low-pass to the high-pass side fades out on one before fading in on the other.
        {
            const bool wantLow = filter_ < 0.5f;
            const float filterTarget = filterOn_ && wantLow == filterLow_ ? 1.0f : 0.0f;
            filterMix_ += std::clamp(filterTarget - filterMix_, -fade_, fade_);
            if (filterMix_ <= 0.0f && filterOn_) filterLow_ = wantLow;
            svfL_.Process(xl);
            svfR_.Process(xr);
            if (filterMix_ > 0.0f) {
                const float fl = filterGain_ * (filterLow_ ? svfL_.Low() : svfL_.High());
                const float fr = filterGain_ * (filterLow_ ? svfR_.Low() : svfR_.High());
                xl += filterMix_ * (fl - xl);
                xr += filterMix_ * (fr - xr);
            }
        }

        // --- Build-up: a high-pass rising with the knob, and noise under it.
        if (std::fabs(buildTarget_ - build_) > 1e-5f) build_ += buildCoef_ * (buildTarget_ - build_);
        else build_ = buildTarget_;
        float dropGain = 1.0f;
        if (dropFade_ > 0.0f) {
            dropFade_ = std::max(0.0f, dropFade_ - dropFadeStep_);
            dropGain = dropFade_;
            if (dropFade_ <= 0.0f) build_ = 0.0f;  // faded out: gone
        }
        float buildNoiseL = 0.0f, buildNoiseR = 0.0f;
        if (build_ > 1e-4f) {
            if (!buildOn_) {
                buildOn_ = true;
                initFilter(buildL_, rate_);
                initFilter(buildR_, rate_);
                initFilter(buildNoiseL_, rate_);
                initFilter(buildNoiseR_, rate_);
                buildL_.SetRes(0.3f);
                buildR_.SetRes(0.3f);
                buildNoiseL_.SetRes(0.45f);
                buildNoiseR_.SetRes(0.45f);
                buildSeedL_ = 0x2545F491u;
                buildSeedR_ = 0x6C8E9CF5u;
                buildCount_ = 0;
            }
            if (buildCount_-- <= 0) {
                buildCount_ = 16;
                buildL_.SetFreq(expMap(build_, 20.0f, 1500.0f));
                buildR_.SetFreq(expMap(build_, 20.0f, 1500.0f));
                buildNoiseL_.SetFreq(expMap(build_, 500.0f, 9000.0f));
                buildNoiseR_.SetFreq(expMap(build_, 500.0f, 9000.0f));
                buildMix_ = std::min(1.0f, build_ * 10.0f);
            }
            buildL_.Process(xl);
            buildR_.Process(xr);
            const float comp = resonanceCompensation(0.3f);
            xl += dropGain * buildMix_ * (comp * buildL_.High() - xl);
            xr += dropGain * buildMix_ * (comp * buildR_.High() - xr);
            const float g = dropGain * 0.15f * build_ * build_;
            buildNoiseL_.Process(noise(buildSeedL_));
            buildNoiseR_.Process(noise(buildSeedR_));
            buildNoiseL = g * buildNoiseL_.Band();
            buildNoiseR = g * buildNoiseR_.Band();
        } else {
            buildOn_ = false;
        }

        // --- Kills.
        if (isolator_.active()) isolator_.process(&xl, &xr, 1);

        // --- Echo out: the dry sound fades out over a beat into the echo, whose tail rings on.
        {
            const bool echoing = echoOut_.on || echoAuto_;
            if (echoing) dry_ = std::max(0.0f, dry_ - static_cast<float>(1.0 / std::max(1.0, 4.0 * stepFrames_)));
            else dry_ = std::min(1.0f, dry_ + fade_);
            send_ = echoing ? 1.0f : std::max(0.0f, send_ - fade_);
            if (echoing || send_ > 0.0f) echoAlive_ = static_cast<int64_t>(12.0f * rate_);
            float el = 0.0f, er = 0.0f;
            if (echoAlive_ > 0) {
                const float g = dry_ * send_;
                const float il = xl * g, ir = xr * g;
                echo_.process(&il, &ir, &el, &er, 1);
                --echoAlive_;
            }
            xl = xl * dry_ + el;
            xr = xr * dry_ + er;
        }

        // --- Riser / downlifter, and the build-up's noise, on top.
        float riserL = 0.0f, riserR = 0.0f;
        if (riserRunning_) {
            const double u = std::min(1.0, riserT_ / riserTotal_);
            if (riserCount_-- <= 0) {
                riserCount_ = 16;
                const float f = riserDown_ ? expMap(static_cast<float>(1.0 - u), 150.0f, 9000.0f) : expMap(static_cast<float>(u), 200.0f, 12000.0f);
                riserL_.SetFreq(f);
                riserR_.SetFreq(f);
                riserGain_ = riserDown_ ? 0.25f * static_cast<float>((1.0 - u) * (1.0 - u)) : 0.25f * static_cast<float>(u * std::sqrt(u));
            }
            riserL_.Process(noise(seedL_));
            riserR_.Process(noise(seedR_));
            if (riserEnding_) riserOut_ = std::max(0.0f, riserOut_ - 1.0f / (kRiserFade * rate_));
            const float g = riserGain_ * riserOut_;
            riserL = g * (0.6f * riserL_.Band() + 0.4f * riserL_.High());
            riserR = g * (0.6f * riserR_.Band() + 0.4f * riserR_.High());
            riserT_ += 1.0;
            // Its last milliseconds fade out, so it is silent on the bar line it ends on (onLine).
            if (riserT_ >= riserTotal_ - kRiserFade * rate_) riserEnding_ = true;
            if (riserEnding_ && riserOut_ <= 0.0f && riserEndStep_ < 0) riserRunning_ = false;
        }
        // Its own sounds run the same in every mode (an export's file of them sounds like the mix).
        const float noiseL = riserL + buildNoiseL, noiseR = riserR + buildNoiseR;
        l[i] = noises_ == NoisesOnly ? noiseL : noises_ == NoisesOff ? xl : xl + noiseL;
        r[i] = noises_ == NoisesOnly ? noiseR : noises_ == NoisesOff ? xr : xr + noiseR;

        sinceStep_ += 1.0;
        sinceEighth_ += 1.0;
        sinceBeat_ += 1.0;
        sinceBar_ += 1.0;
    }
    stepAt_ = -1;
    stepHere_ = false;
    if (!clock_) {
        // The transport stopped while something waited for a bar line: a release effect's end
        // (the dry sound now), a riser's (it ends after its length).
        if (returnStep_ >= 0) endRelease();
        riserEndStep_ = -1;
    }
}

}  // namespace ps
