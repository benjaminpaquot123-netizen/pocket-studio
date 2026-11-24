// The electronic music classics added in phase 25: distortion, auto-pan, OTT, frequency shifter,
// and the long ones a slot borrows from the pool (insert echo, insert reverb, tape stop).
// Same rules as Effects.cpp: prepare() may allocate, setParam()/process() never do.

#include "Effects.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {

constexpr double kPi = 3.14159265358979323846;
constexpr double kTwoPi = 2.0 * kPi;

float expMap(float v, float lo, float hi) { return lo * std::pow(hi / lo, std::clamp(v, 0.0f, 1.0f)); }

float onePoleCoef(float rate, float hz) { return 1.0f - std::exp(-2.0f * static_cast<float>(kPi) * hz / rate); }

// A one-pole smoothing coefficient for a time constant in milliseconds.
float timeCoef(float rate, float ms) { return 1.0f - std::exp(-1.0f / (std::max(0.01f, ms) * 0.001f * rate)); }

void setClamped(std::array<float, 4>& p, int i, float v) {
    if (i >= 0 && i < 4 && std::isfinite(v)) p[static_cast<size_t>(i)] = std::clamp(v, 0.0f, 1.0f);
}

float finiteOr0(float x) { return std::isfinite(x) ? x : 0.0f; }

}  // namespace

// --- Distortion ------------------------------------------------------------------------------

void Distortion::setParam(int p, float v) { setClamped(p_, p, v); }

float Distortion::shape(int type, float x) {
    switch (type) {
        case 1: {
            // Fuzz: lopsided and harder, rich in even harmonics (the DC it leaves is removed after).
            return x >= 0.0f ? 1.0f - std::exp(-2.0f * x) : -0.8f * (1.0f - std::exp(3.0f * x));
        }
        case 2: return std::clamp(x, -1.0f, 1.0f);  // hard clip
        case 3: {
            // Foldback: past the edge the wave folds back in (a triangle of period 4).
            const float m = std::fmod(x - 1.0f, 4.0f);
            const float w = m < 0.0f ? m + 4.0f : m;
            return std::fabs(w - 2.0f) - 1.0f;
        }
        default: return std::tanh(x);  // overdrive: round and warm
    }
}

double Distortion::antiderivative(int type, double x) {
    // The large parts in double (their differences are divided by small steps); the small,
    // smooth exponential parts in float (faster, and their rounding stays under 1e-7).
    switch (type) {
        case 1: {
            if (x >= 0.0) return x + 0.5 * (static_cast<double>(std::exp(static_cast<float>(-2.0 * x))) - 1.0);
            return -0.8 * (x - (static_cast<double>(std::exp(static_cast<float>(3.0 * x))) - 1.0) / 3.0);
        }
        case 2: return std::fabs(x) <= 1.0 ? 0.5 * x * x : std::fabs(x) - 0.5;
        case 3: {
            // The fold is a triangle of period 4 and zero mean: its integral is periodic too.
            double m = std::fmod(x - 1.0, 4.0);
            if (m < 0.0) m += 4.0;
            return (m <= 2.0 ? m - 0.5 * m * m : 0.5 * (m - 3.0) * (m - 3.0) - 0.5) + 0.5;
        }
        default: {
            // ln cosh x = |x| - ln 2 + ln(1 + e^-2|x|), without overflow; past 9 the last term is
            // under 1e-8. ln(1 + e) for e in [0, 1]: a degree-8 polynomial (error 1e-7, its slope's
            // 1e-5: the anti-aliased output is the slope's mean, -100 dB off).
            const double a = std::fabs(x);
            if (a >= 9.0) return a - 0.69314718055994531;
            const float e = std::exp(static_cast<float>(-2.0 * a));
            const float l = e * (0.9999914545712962f + e * (-0.4998011631988066f + e * (0.33133400570038224f + e * (-0.239190717256982f +
                            e * (0.1647834972080014f + e * (-0.09231376860434833f + e * (0.0344185935012013f + e * -0.006074877643463143f)))))));
            return a + static_cast<double>(l) - 0.69314718055994531;
        }
    }
}

void Distortion::process(float* l, float* r, int n) {
    const float target = expMap(p_[0], 1.0f, 60.0f);
    const float tone = onePoleCoef(rate_, expMap(p_[1], 800.0f, 18000.0f));
    const int type = std::min(kTypes - 1, static_cast<int>(p_[2] * kTypes));
    const float mix = p_[3];
    const float dc = 1.0f - static_cast<float>(kTwoPi) * 10.0f / rate_;  // 10 Hz DC blocker
    // The drive glides to a turned knob over about 10 ms, block by block (one step per block).
    if (drive_ < 0.0f) drive_ = target;
    const float startDrive = drive_;
    drive_ += (target - drive_) * (1.0f - std::exp(-static_cast<float>(n) / (0.010f * rate_)));
    struct Curve {
        int type;
        double f(double x) const { return Distortion::shape(type, static_cast<float>(x)); }
        double F(double x) const { return Distortion::antiderivative(type, x); }
    } const curve{type};
    // Levelled: a half-scale input comes out about half scale whatever the drive. The fuzz is
    // biased: its wave spends longer on one side (even harmonics, the "fuzz" grit); silence still
    // gives silence.
    const bool gliding = startDrive != drive_;
    float drive = drive_, level = 0.5f / std::tanh(0.5f * drive), bias = type == 1 ? 0.15f * drive : 0.0f;
    float rest = type == 1 ? shape(type, bias) : 0.0f;
    constexpr int kChunk = DriveShaperPair::kMaxBlock;
    float inL[kChunk], inR[kChunk], gain[kChunk], offset[kChunk], levels[kChunk], rests[kChunk];
    float wl[kChunk], wr[kChunk], dryL[kChunk], dryR[kChunk];
    for (int from = 0; from < n; from += kChunk) {
        const int m = std::min(kChunk, n - from);
        float* bl = l + from;
        float* br = r + from;
        for (int j = 0; j < m; ++j) {
            const int i = from + j;
            if (gliding) {
                drive = startDrive + (drive_ - startDrive) * static_cast<float>(i + 1) / static_cast<float>(n);
                level = 0.5f / std::tanh(0.5f * drive);
                bias = type == 1 ? 0.15f * drive : 0.0f;
                rest = type == 1 ? shape(type, bias) : 0.0f;
            }
            inL[j] = finiteOr0(bl[j]);
            inR[j] = finiteOr0(br[j]);
            gain[j] = drive;
            offset[j] = bias;
            levels[j] = level;
            rests[j] = rest;
        }
        // Both channels' curves in one go (audit stage 4: the same output, in less time).
        sh_.process(inL, inR, wl, wr, dryL, dryR, m, gain, offset, curve);
        for (int j = 0; j < m; ++j) {
            const float yl = (wl[j] - rests[j]) * levels[j];
            const float yr = (wr[j] - rests[j]) * levels[j];
            const float xl = dryL[j], xr = dryR[j];
            const float bl0 = yl - dcInL_ + dc * dcOutL_;
            dcInL_ = yl;
            dcOutL_ = bl0;
            const float br0 = yr - dcInR_ + dc * dcOutR_;
            dcInR_ = yr;
            dcOutR_ = br0;
            lp1L_ += (bl0 - lp1L_) * tone;
            lp2L_ += (lp1L_ - lp2L_) * tone;
            lp1R_ += (br0 - lp1R_) * tone;
            lp2R_ += (lp1R_ - lp2R_) * tone;
            bl[j] = xl + mix * (lp2L_ - xl);
            br[j] = xr + mix * (lp2R_ - xr);
        }
    }
}

// --- Auto-pan --------------------------------------------------------------------------------

void AutoPan::setParam(int p, float v) { setClamped(p_, p, v); }

int AutoPan::stepsOf(float v) {
    static constexpr int kSteps[kRates] = {32, 16, 8, 4, 2, 1};  // 2 bars, 1 bar, 1/2, 1/4, 1/8, 1/16
    return kSteps[std::clamp(static_cast<int>(v * kRates), 0, kRates - 1)];
}

void AutoPan::process(float* l, float* r, int n) {
    const int steps = stepsOf(p_[0]);
    const double inc = 1.0 / std::max(1.0, steps * stepFrames_);
    const float depth = p_[1], steep = 1.0f + 9.0f * p_[2];
    const float norm = 1.0f / std::tanh(steep);
    for (int i = 0; i < n; ++i) {
        if (i == pending_) {
            // A beat: back on the grid (a round trip of a beat or less starts again; a longer one
            // goes to its nearest beat).
            if (steps <= 4) {
                phase_ = 0.0;
            } else {
                const double beats = steps / 4.0;
                phase_ = std::round(phase_ * beats) / beats;
                if (phase_ >= 1.0) phase_ -= 1.0;
            }
        }
        const float s = static_cast<float>(std::sin(kTwoPi * phase_));
        const float pan = depth * std::tanh(s * steep) * norm;
        // Equal power, unity in the middle (the mixer's pan law).
        const float angle = (pan + 1.0f) * 0.25f * static_cast<float>(kPi);
        l[i] *= std::cos(angle) * 1.41421356f;
        r[i] *= std::sin(angle) * 1.41421356f;
        phase_ += inc;
        if (phase_ >= 1.0) phase_ -= 1.0;
    }
    pending_ = -1;
}

// --- OTT -------------------------------------------------------------------------------------

namespace {
constexpr float kLowSplit = 120.0f, kHighSplit = 2500.0f;
constexpr float kDownThreshold = -18.0f, kUpThreshold = -24.0f;
}  // namespace

void Ott::prepare(float rate) {
    rate_ = rate;
    Biquad lp1, hp1, lp2, hp2;
    lp1.lowPass(rate, kLowSplit);
    hp1.highPass(rate, kLowSplit);
    lp2.lowPass(rate, kHighSplit);
    hp2.highPass(rate, kHighSplit);
    split_.lp1a.set(lp1);
    split_.lp1b.set(lp1);
    split_.hp1a.set(hp1);
    split_.hp1b.set(hp1);
    split_.lp2a.set(lp2);
    split_.lp2b.set(lp2);
    split_.hp2a.set(hp2);
    split_.hp2b.set(hp2);
    update();
    reset();
}

void Ott::setParam(int p, float v) {
    setClamped(p_, p, v);
    update();
}

void Ott::update() {
    // Faster to the right; the bass band the slowest, the highs the quickest (as the original).
    const float scale = expMap(p_[1], 4.0f, 0.25f);
    static constexpr float kAttackMs[3] = {12.0f, 6.0f, 3.0f};
    static constexpr float kReleaseMs[3] = {160.0f, 110.0f, 70.0f};
    for (size_t b = 0; b < 3; ++b) {
        attack_[b] = timeCoef(rate_, kAttackMs[b] * scale);
        release_[b] = timeCoef(rate_, kReleaseMs[b] * scale);
    }
}

void Ott::reset() {
    for (Biquad2* q : {&split_.lp1a, &split_.lp1b, &split_.hp1a, &split_.hp1b, &split_.lp2a, &split_.lp2b, &split_.hp2a, &split_.hp2b}) q->reset();
    env_.fill(0.0f);
    gainDb_.fill(0.0f);
    gain_.fill(1.0f);
    count_ = 0;
}

void Ott::process(float* l, float* r, int n) {
    const float depth = p_[0], up = p_[2], down = p_[3];
    const float downSlope = 1.0f - 1.0f / (1.0f + 15.0f * down);
    const float upSlope = 1.0f - 1.0f / (1.0f + 3.0f * up);
    const float makeup = 4.0f * down;
    for (int i = 0; i < n; ++i) {
        const float xl = finiteOr0(l[i]), xr = finiteOr0(r[i]);
        // Both sides' band split at once (audit stage 4: the same sums, lane by lane).
        Split& s = split_;
        const Float2 x(xl, xr);
        const Float2 low = s.lp1b.process(s.lp1a.process(x));
        const Float2 rest = s.hp1b.process(s.hp1a.process(x));
        const Float2 mid = s.lp2b.process(s.lp2a.process(rest));
        const Float2 high = s.hp2b.process(s.hp2a.process(rest));
        const float bl[3] = {low.v[0], mid.v[0], high.v[0]}, br[3] = {low.v[1], mid.v[1], high.v[1]};
        for (size_t b = 0; b < 3; ++b) {
            const float e = std::max(std::fabs(bl[b]), std::fabs(br[b]));
            env_[b] += (e - env_[b]) * (e > env_[b] ? attack_[b] : release_[b]);
        }
        // The gains move every 8 frames (log and pow are the costly part).
        if ((count_++ & 7) == 0) {
            for (size_t b = 0; b < 3; ++b) {
                const float lev = 20.0f * std::log10(env_[b] + 1e-6f);
                float g = makeup;
                if (lev > kDownThreshold) g -= (lev - kDownThreshold) * downSlope;
                if (lev < kUpThreshold) {
                    // Up to 15 dB, and nothing for near-silence (hiss stays down).
                    const float fade = std::clamp((lev + 72.0f) / 12.0f, 0.0f, 1.0f);
                    g += std::min((kUpThreshold - lev) * upSlope, 15.0f * up) * fade;
                }
                gainDb_[b] = g;
                gain_[b] = std::pow(10.0f, g / 20.0f);
            }
        }
        const float wl = bl[0] * gain_[0] + bl[1] * gain_[1] + bl[2] * gain_[2];
        const float wr = br[0] * gain_[0] + br[1] * gain_[1] + br[2] * gain_[2];
        l[i] = xl + depth * (wl - xl);
        r[i] = xr + depth * (wr - xr);
    }
}

// --- Frequency shifter -----------------------------------------------------------------------

void FreqShifter::Hilbert::init() {
    // Olli Niemitalo's 90-degree phase difference network (about 15 Hz..20 kHz at 48 kHz).
    static constexpr float kA[4] = {0.6923878f, 0.9360654322959f, 0.9882295226860f, 0.9987488452737f};
    static constexpr float kB[4] = {0.4021921162426f, 0.8561710882420f, 0.9722909545651f, 0.9952884791278f};
    for (size_t k = 0; k < 4; ++k) {
        a[k] = Allpass2{};
        b[k] = Allpass2{};
        a[k].a2 = kA[k] * kA[k];
        b[k].a2 = kB[k] * kB[k];
    }
    aDelay = 0.0f;
}

void FreqShifter::Hilbert::run(float x, float& re, float& im) {
    float ya = x, yb = x;
    for (size_t k = 0; k < 4; ++k) {
        ya = a[k].run(ya);
        yb = b[k].run(yb);
    }
    re = aDelay;  // the first path, one sample later: then the two are 90 degrees apart
    aDelay = ya;
    im = yb;
}

void FreqShifter::reset() {
    hL_.init();
    hR_.init();
    phase_ = 0.0;
    fbL_ = fbR_ = 0.0f;
}

void FreqShifter::setParam(int p, float v) { setClamped(p_, p, v); }

float FreqShifter::shiftHz(float v) {
    const float s = 2.0f * std::clamp(v, 0.0f, 1.0f) - 1.0f;
    return 2000.0f * s * std::fabs(s);
}

void FreqShifter::process(float* l, float* r, int n) {
    const double inc = shiftHz(p_[0]) / rate_;
    const float feedback = 0.8f * p_[1], mix = p_[2];
    for (int i = 0; i < n; ++i) {
        const float c = static_cast<float>(std::cos(kTwoPi * phase_));
        const float s = static_cast<float>(std::sin(kTwoPi * phase_));
        const float xl = finiteOr0(l[i]), xr = finiteOr0(r[i]);
        float re, im;
        hL_.run(xl + feedback * fbL_, re, im);
        const float yl = re * c + im * s;
        hR_.run(xr + feedback * fbR_, re, im);
        const float yr = re * c + im * s;
        fbL_ = yl;
        fbR_ = yr;
        l[i] = xl + mix * (yl - xl);
        r[i] = xr + mix * (yr - xr);
        phase_ += inc;
        if (phase_ >= 1.0) phase_ -= 1.0;
        if (phase_ < 0.0) phase_ += 1.0;
    }
}

// --- Insert echo -----------------------------------------------------------------------------

namespace {
// Dry stays whole up to the middle of the mix knob, the wet reaches full there: one knob from
// "a touch" to "only the effect".
inline void dryWet(float mix, float& dry, float& wet) {
    dry = std::min(1.0f, 2.0f * (1.0f - mix));
    wet = std::min(1.0f, 2.0f * mix);
}
constexpr int kWetBlock = 64;
}  // namespace

void InsertEcho::setParam(int p, float v) {
    setClamped(p_, p, v);
    applyParams();
}

void InsertEcho::applyParams() {
    delay_.setParam(0, p_[0]);  // the same note values as the send delay
    delay_.setParam(1, p_[1]);
    delay_.setParam(2, pingPong_ ? 1.0f : 0.0f);
    delay_.setParam(3, p_[2]);
}

void InsertEcho::process(float* l, float* r, int n) {
    float dry, wet;
    dryWet(p_[3], dry, wet);
    std::array<float, kWetBlock> wl{}, wr{};
    for (int at = 0; at < n; at += kWetBlock) {
        const int m = std::min(kWetBlock, n - at);
        std::fill(wl.begin(), wl.begin() + m, 0.0f);
        std::fill(wr.begin(), wr.begin() + m, 0.0f);
        delay_.process(l + at, r + at, wl.data(), wr.data(), m);
        for (int i = 0; i < m; ++i) {
            l[at + i] = l[at + i] * dry + wl[static_cast<size_t>(i)] * wet;
            r[at + i] = r[at + i] * dry + wr[static_cast<size_t>(i)] * wet;
        }
    }
}

// --- Insert reverb ---------------------------------------------------------------------------

void InsertReverb::setParam(int p, float v) {
    setClamped(p_, p, v);
    applyParams();
}

void InsertReverb::applyParams() {
    reverb_.setParam(0, p_[0]);
    reverb_.setParam(1, p_[1]);
    reverb_.setParam(2, 1.0f);  // full width
    reverb_.setParam(3, p_[2]);
}

void InsertReverb::process(float* l, float* r, int n) {
    float dry, wet;
    dryWet(p_[3], dry, wet);
    // Freeverb's wet gain is made for a send at a few percent: on an insert the tail would
    // tower over the dry sound.
    wet *= 0.35f;
    std::array<float, kWetBlock> wl{}, wr{};
    for (int at = 0; at < n; at += kWetBlock) {
        const int m = std::min(kWetBlock, n - at);
        std::fill(wl.begin(), wl.begin() + m, 0.0f);
        std::fill(wr.begin(), wr.begin() + m, 0.0f);
        reverb_.process(l + at, r + at, wl.data(), wr.data(), m);
        for (int i = 0; i < m; ++i) {
            l[at + i] = l[at + i] * dry + wl[static_cast<size_t>(i)] * wet;
            r[at + i] = r[at + i] * dry + wr[static_cast<size_t>(i)] * wet;
        }
    }
}

// --- Tape stop -------------------------------------------------------------------------------

void TapeStop::prepare(float rate) {
    rate_ = rate;
    const auto size = static_cast<size_t>(kMaxSeconds * rate) + 4;
    bufL_.assign(size, 0.0f);
    bufR_.assign(size, 0.0f);
    reset();
}

void TapeStop::setParam(int p, float v) { setClamped(p_, p, v); }

void TapeStop::reset() {
    // No clearing (constant time, a pool unit borrowed in the callback): a stop reads only what
    // was written after it began (the reading starts at the writing and never overtakes it), and
    // every sample written is finite.
    write_ = 0;
    readPos_ = 0.0;
    speed_ = 1.0;
    stopping_ = false;
    back_ = 1.0f;
}

void TapeStop::process(float* l, float* r, int n) {
    if (bufL_.empty()) return;
    const size_t size = bufL_.size();
    const bool stop = p_[0] >= 0.5f;
    // Linear slow-down: the reading falls behind by half the stop time at most (< kMaxSeconds).
    const double slow = 1.0 / (expMap(p_[1], 0.1f, 2.0f) * rate_);
    const float fadeFrames = 0.3f * p_[2] * rate_;
    const float backStep = fadeFrames < 1.0f ? 1.0f : 1.0f / fadeFrames;
    for (int i = 0; i < n; ++i) {
        bufL_[write_] = finiteOr0(l[i]);
        bufR_[write_] = finiteOr0(r[i]);
        if (stop && !stopping_) {
            stopping_ = true;
            speed_ = 1.0;
            readPos_ = static_cast<double>(write_);
        } else if (!stop && stopping_) {
            stopping_ = false;  // back to the live sound, faded in
            speed_ = 1.0;
            back_ = 0.0f;
        }
        if (stopping_) {
            const auto i0 = static_cast<size_t>(readPos_) % size;
            const auto i1 = (i0 + 1) % size;
            const float t = static_cast<float>(readPos_ - std::floor(readPos_));
            const float level = static_cast<float>(std::sqrt(speed_));
            l[i] = (bufL_[i0] + t * (bufL_[i1] - bufL_[i0])) * level;
            r[i] = (bufR_[i0] + t * (bufR_[i1] - bufR_[i0])) * level;
            speed_ = std::max(0.0, speed_ - slow);
            readPos_ += speed_;
            if (readPos_ >= static_cast<double>(size)) readPos_ -= static_cast<double>(size);
        } else if (back_ < 1.0f) {
            back_ = std::min(1.0f, back_ + backStep);
            l[i] *= back_;
            r[i] *= back_;
        }
        if (++write_ >= size) write_ = 0;
    }
}

// --- Wobble ----------------------------------------------------------------------------------

namespace {
// Sixteenths per LFO cycle, slowest first: 1/1, 1/2, 1/4, 1/4T, 1/8, 1/8T, 1/16, 1/16T, 1/32.
constexpr double kWobbleSteps[Wobble::kRates] = {16.0, 8.0, 4.0, 8.0 / 3.0, 2.0, 4.0 / 3.0, 1.0, 2.0 / 3.0, 0.5};
// Every cycle above divides 48 sixteenths (three bars of 4/4): the phase a transport step gives.
constexpr int kWobbleSpan = 48;
}  // namespace

double Wobble::stepsOf(float v) { return kWobbleSteps[std::clamp(static_cast<int>(v * kRates), 0, kRates - 1)]; }

float Wobble::shape(int s, double phase) {
    const double p = phase - std::floor(phase);
    switch (s) {
        case 1: return static_cast<float>(1.0 - std::fabs(2.0 * p - 1.0));      // triangle, from the bottom
        case 2: return static_cast<float>(1.0 - p);                             // saw down: opens, then closes
        case 3: return p < 0.5 ? 1.0f : 0.0f;                                   // square
        default: return static_cast<float>(0.5 - 0.5 * std::cos(kTwoPi * p));  // sine, from the bottom
    }
}

void Wobble::prepare(float rate) {
    rate_ = rate;
    reset();
}

void Wobble::reset() {
    left_.Init(rate_);
    right_.Init(rate_);
    left_.SetDrive(0.0f);  // see SweepFilter::prepare
    right_.SetDrive(0.0f);
    phase_ = 0.0;
    pending_ = -1;
    pendingStep_ = -1;
    countdown_ = 0;
    cutoffNow_ = 80.0f;
}

void Wobble::setParam(int p, float v) { setClamped(p_, p, v); }

void Wobble::process(float* l, float* r, int n) {
    const double steps = stepsOf(p_[0]);
    const double inc = 1.0 / std::max(1.0, steps * stepFrames_);
    const int shapeIndex = std::min(kShapes - 1, static_cast<int>(p_[3] * kShapes));
    const float octaves = 8.0f * p_[1];
    const float res = 0.92f * p_[2];
    const float gain = resonanceCompensation(res);
    const float top = 0.45f * rate_;
    // The cutoff glides (about 1.5 ms): the square shape opens and shuts without a click.
    const float glide = 1.0f - std::exp(-4.0f / (0.0015f * rate_));
    left_.SetRes(res);
    right_.SetRes(res);
    for (int i = 0; i < n; ++i) {
        if (i == pending_) {
            if (pendingStep_ >= 0) {
                // The transport's step: the phase the song is at, whatever happened before.
                const double pos = static_cast<double>(pendingStep_ % kWobbleSpan) / steps;
                phase_ = pos - std::floor(pos);
            } else if (steps <= 4.0 && std::fmod(4.0, steps) == 0.0) {
                phase_ = 0.0;  // whole cycles in a beat: back to the start
            }
        }
        if (countdown_ <= 0) {
            // Every 4 frames: the filter's coefficients cost a sine.
            const float target = std::min(top, 80.0f * std::exp2(octaves * shape(shapeIndex, phase_)));
            cutoffNow_ += glide * (target - cutoffNow_);
            left_.SetFreq(cutoffNow_);
            right_.SetFreq(cutoffNow_);
            countdown_ = 4;
        }
        --countdown_;
        left_.Process(finiteOr0(l[i]));
        right_.Process(finiteOr0(r[i]));
        l[i] = gain * left_.Low();
        r[i] = gain * right_.Low();
        phase_ += inc;
        if (phase_ >= 1.0) phase_ -= 1.0;
    }
    pending_ = -1;
}

// --- Bass exciter (« Basse audible ») -------------------------------------------------------

float BassExciter::frequencyHz(float v) { return expMap(v, 60.0f, 200.0f); }
float BassExciter::topHz(float v) { return expMap(v, 250.0f, 1200.0f); }

void BassExciter::prepare(float rate) {
    rate_ = rate;
    attack_ = timeCoef(rate, 2.0f);
    release_ = timeCoef(rate, 60.0f);
    amountGlide_ = timeCoef(rate, 10.0f);
    filterGlide_ = 1.0f - std::exp(-static_cast<float>(kStep) / (0.02f * rate));
    freqNow_ = frequencyHz(p_[1]);
    topNow_ = topHz(p_[3]);
    update(freqNow_, topNow_);
    reset();
}

void BassExciter::setParam(int p, float v) {
    if (p < 0 || p >= kParams || !std::isfinite(v)) return;
    // Stored only: the audio thread glides the filters to it (glideFilters).
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
}

void BassExciter::update(float f, float topHz) {
    lowA_.lowPassQ(rate_, f, 0.5412f);
    lowB_.lowPassQ(rate_, f, 1.3066f);
    const float hp = std::max(100.0f, 1.25f * f);
    hpA_.highPassQ(rate_, hp, 0.5412f);
    hpB_.highPassQ(rate_, hp, 1.3066f);
    top_.lowPass(rate_, std::min(topHz, 0.45f * rate_));
}

void BassExciter::glideFilters() {
    const float f = frequencyHz(p_[1]), top = topHz(p_[3]);
    if (f == freqNow_ && top == topNow_) return;
    freqNow_ += (f - freqNow_) * filterGlide_;
    topNow_ += (top - topNow_) * filterGlide_;
    // Close enough (0.1 %): there, so that the coefficients stop being worked out.
    if (std::fabs(f - freqNow_) < 1e-3f * f) freqNow_ = f;
    if (std::fabs(top - topNow_) < 1e-3f * top) topNow_ = top;
    update(freqNow_, topNow_);
}

void BassExciter::reset() {
    lowA_.reset();
    lowB_.reset();
    hpA_.reset();
    hpB_.reset();
    top_.reset();
    env_ = 0.0f;
    countdown_ = 0;
    fresh_ = true;
}

void BassExciter::process(float* l, float* r, int n) {
    const float target = p_[0];
    if (fresh_) {
        amount_ = target;
        if (freqNow_ != frequencyHz(p_[1]) || topNow_ != topHz(p_[3])) {
            freqNow_ = frequencyHz(p_[1]);
            topNow_ = topHz(p_[3]);
            update(freqNow_, topNow_);
        }
        fresh_ = false;
    }
    if (target <= 0.0f && amount_ < 1e-5f) {
        amount_ = 0.0f;
        return;  // nothing to add (the filters wait at their last state)
    }
    const float colour = p_[2];
    constexpr float kShape = 4.0f;
    constexpr float kFloor = 1e-5f;  // under it the low band is silence: its noise is not blown up
    for (int i = 0; i < n; ++i) {
        if (--countdown_ < 0) {
            glideFilters();
            countdown_ = kStep - 1;
        }
        const float in = 0.5f * (l[i] + r[i]);
        if (!std::isfinite(in)) {
            // A NaN or an infinity in: the filters and the envelope start again from silence
            // (left as they were, they would stay NaN and silence the exciter for good).
            lowA_.reset();
            lowB_.reset();
            hpA_.reset();
            hpB_.reset();
            top_.reset();
            env_ = 0.0f;
            continue;
        }
        const float low = lowB_.process(lowA_.process(in));
        const float a = std::fabs(low);
        env_ += (a > env_ ? attack_ : release_) * (a - env_);
        const float t = std::tanh(kShape * low / (env_ + kFloor));
        const float shaped = (1.0f - colour) * t + colour * t * t;
        const float harmonics = top_.process(hpB_.process(hpA_.process(shaped)));
        amount_ += (target - amount_) * amountGlide_;
        const float add = finiteOr0(amount_ * kGain * env_ * harmonics);
        l[i] += add;
        r[i] += add;
    }
}

// --- L12: transient shaper (« Attaque ») -----------------------------------------------------

namespace {
float dbGain(float db) { return std::exp2(db * 0.16609640f); }  // 10^(db/20)
float ratioDb(float a, float b) {
    constexpr float kFloor = 1e-6f;  // under -120 dBFS: silence, no gap read
    if (a < kFloor || b < kFloor) return 0.0f;
    return 6.0205999f * std::log2(a / b);
}
}  // namespace

void TransientShaper::prepare(float rate) {
    fastA_ = timeCoef(rate, 0.3f);
    fastR_ = timeCoef(rate, 40.0f);
    slowA_ = timeCoef(rate, 15.0f);
    slowR_ = fastR_;
    longR_ = timeCoef(rate, 250.0f);
    shortR_ = timeCoef(rate, 30.0f);
    glide_ = timeCoef(rate, 10.0f);
    reset();
}

void TransientShaper::setParam(int p, float v) {
    if (p < 0 || p >= kParams || !std::isfinite(v)) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
}

void TransientShaper::reset() {
    fast_ = slow_ = long_ = short_ = 0.0f;
    gainDb_ = 0.0f;
    fresh_ = true;
}

void TransientShaper::process(float* l, float* r, int n) {
    const float attack = 2.0f * p_[0] - 1.0f, sustain = 2.0f * p_[1] - 1.0f;
    const float mix = p_[2], out = dbGain(12.0f * p_[3] - 6.0f);
    if (fresh_) {
        attack_ = attack;
        sustain_ = sustain;
        mix_ = mix;
        out_ = out;
        fresh_ = false;
    }
    for (int i = 0; i < n; ++i) {
        const float xl = l[i], xr = r[i];
        if (!std::isfinite(xl) || !std::isfinite(xr)) {
            // A NaN or an infinity: silence out, the followers start again (left NaN, they would stay so).
            fast_ = slow_ = long_ = short_ = 0.0f;
            l[i] = r[i] = 0.0f;
            continue;
        }
        const float a = std::max(std::fabs(xl), std::fabs(xr));
        fast_ += (a > fast_ ? fastA_ : fastR_) * (a - fast_);
        slow_ += (a > slow_ ? slowA_ : slowR_) * (a - slow_);
        long_ += (a > long_ ? fastA_ : longR_) * (a - long_);
        short_ += (a > short_ ? fastA_ : shortR_) * (a - short_);
        attack_ += (attack - attack_) * glide_;
        sustain_ += (sustain - sustain_) * glide_;
        mix_ += (mix - mix_) * glide_;
        out_ += (out - out_) * glide_;
        // Each gap at most 24 dB counts, halved: a knob at its end moves a hit by up to 12 dB.
        const float hit = std::min(24.0f, std::max(0.0f, ratioDb(fast_, slow_)));
        const float tail = std::min(24.0f, std::max(0.0f, ratioDb(long_, short_)));
        gainDb_ = std::clamp(0.5f * (attack_ * hit + sustain_ * tail), -kMaxDb, kMaxDb);
        const float g = out_ * (1.0f + mix_ * (dbGain(gainDb_) - 1.0f));
        l[i] = finiteOr0(xl * g);
        r[i] = finiteOr0(xr * g);
    }
}

// --- L12: drum bus saturation (« Bus batterie ») ---------------------------------------------

// The curve's input at the running peak: -6 dB of it at drive 0 (nearly straight: +0.6 dB under
// the peak), +12 dB at the top (the soft clip's ceiling: up to +12 dB under the peak).
float DrumBus::driveGain(float v) { return 0.5f * dbGain(18.0f * std::clamp(v, 0.0f, 1.0f)); }

void DrumBus::prepare(float rate) {
    release_ = timeCoef(rate, 1500.0f);
    glide_ = timeCoef(rate, 10.0f);
    reset();
}

void DrumBus::setParam(int p, float v) { setClamped(p_, p, v); }

void DrumBus::reset() {
    sh_.reset();
    postRing_.fill(0.0f);
    ring_ = 0;
    peak_ = 0.0f;
    fresh_ = true;
}

void DrumBus::process(float* l, float* r, int n) {
    const float drive = driveGain(p_[0]), mix = p_[1], out = dbGain(12.0f * p_[2] - 6.0f);
    if (fresh_) {
        drive_ = drive;
        mix_ = mix;
        out_ = out;
        fresh_ = false;
    }
    // The curve at gain g, scaled so that 1 (the running peak) comes out at 1: f(g u) / f(g).
    struct Curve {
        double f(double x) const { return shape.f(x); }
        double F(double x) const { return shape.F(x); }
        SoftClipShape shape;
    } const curve{};
    constexpr int kChunk = DriveShaperPair::kMaxBlock;
    constexpr float kFloor = 1e-3f;  // under -60 dBFS the scale stops: silence is not lifted up
    float xl[kChunk], xr[kChunk], gain[kChunk], post[kChunk], yl[kChunk], yr[kChunk], dl[kChunk], dr[kChunk];
    float mixAt[kChunk];
    for (int from = 0; from < n; from += kChunk) {
        const int m = std::min(kChunk, n - from);
        float* bl = l + from;
        float* br = r + from;
        for (int i = 0; i < m; ++i) {
            xl[i] = std::isfinite(bl[i]) ? bl[i] : 0.0f;
            xr[i] = std::isfinite(br[i]) ? br[i] : 0.0f;
            const float a = std::max(std::fabs(xl[i]), std::fabs(xr[i]));
            peak_ = a > peak_ ? a : peak_ + (a - peak_) * release_;
            drive_ += (drive - drive_) * glide_;
            mix_ += (mix - mix_) * glide_;
            out_ += (out - out_) * glide_;
            const float ref = std::max(kFloor, peak_);
            gain[i] = drive_ / ref;
            // The output's scale lined up with the shaped sound (kDelay frames old): a louder hit
            // raises the peak at once, and the frames shaped before it keep the scale they had.
            postRing_[static_cast<size_t>(ring_)] = ref / static_cast<float>(curve.f(drive_)) * out_;
            ring_ = ring_ == DriveShaperPair::kDelay ? 0 : ring_ + 1;
            post[i] = postRing_[static_cast<size_t>(ring_)];
            mixAt[i] = mix_;
        }
        sh_.process(xl, xr, yl, yr, dl, dr, m, gain, nullptr, curve);
        for (int i = 0; i < m; ++i) {
            const float wl = yl[i] * post[i], wr = yr[i] * post[i];
            bl[i] = finiteOr0(out_ * dl[i] + mixAt[i] * (wl - out_ * dl[i]));
            br[i] = finiteOr0(out_ * dr[i] + mixAt[i] * (wr - out_ * dr[i]));
        }
    }
}

// --- L12: mid/side EQ (« Graves mono ») ------------------------------------------------------

float MidSideEq::monoHz(float v) { return v <= 0.0f ? 10.0f : expMap(v, 40.0f, 300.0f); }

void MidSideEq::prepare(float rate) {
    rate_ = rate;
    filterGlide_ = 1.0f - std::exp(-static_cast<float>(kStep) / (0.02f * rate));
    hzNow_ = monoHz(p_[0]);
    lowDbNow_ = 12.0f * p_[1] - 6.0f;
    highDbNow_ = 12.0f * p_[2] - 6.0f;
    update();
    reset();
}

void MidSideEq::setParam(int p, float v) { setClamped(p_, p, v); }

void MidSideEq::update() {
    sideA_.highPassQ(rate_, hzNow_, 0.5412f);
    sideB_.highPassQ(rate_, hzNow_, 1.3066f);
    midLow_.lowShelf(rate_, 120.0f, lowDbNow_);
    sideHigh_.highShelf(rate_, std::min(6000.0f, 0.4f * rate_), highDbNow_);
}

void MidSideEq::glide() {
    const float hz = monoHz(p_[0]), low = 12.0f * p_[1] - 6.0f, high = 12.0f * p_[2] - 6.0f;
    if (hz == hzNow_ && low == lowDbNow_ && high == highDbNow_) return;
    hzNow_ += (hz - hzNow_) * filterGlide_;
    lowDbNow_ += (low - lowDbNow_) * filterGlide_;
    highDbNow_ += (high - highDbNow_) * filterGlide_;
    if (std::fabs(hz - hzNow_) < 1e-3f * hz) hzNow_ = hz;
    if (std::fabs(low - lowDbNow_) < 1e-3f) lowDbNow_ = low;
    if (std::fabs(high - highDbNow_) < 1e-3f) highDbNow_ = high;
    update();
}

void MidSideEq::reset() {
    sideA_.reset();
    sideB_.reset();
    midLow_.reset();
    sideHigh_.reset();
    countdown_ = 0;
    fresh_ = true;
}

void MidSideEq::process(float* l, float* r, int n) {
    if (fresh_) {
        const float hz = monoHz(p_[0]), low = 12.0f * p_[1] - 6.0f, high = 12.0f * p_[2] - 6.0f;
        if (hz != hzNow_ || low != lowDbNow_ || high != highDbNow_) {
            hzNow_ = hz;
            lowDbNow_ = low;
            highDbNow_ = high;
            update();
        }
        fresh_ = false;
    }
    for (int i = 0; i < n; ++i) {
        if (--countdown_ < 0) {
            glide();
            countdown_ = kStep - 1;
        }
        const float xl = l[i], xr = r[i];
        if (!std::isfinite(xl) || !std::isfinite(xr)) {
            sideA_.reset();
            sideB_.reset();
            midLow_.reset();
            sideHigh_.reset();
            l[i] = r[i] = 0.0f;
            continue;
        }
        const float mid = midLow_.process(0.5f * (xl + xr));
        const float side = sideHigh_.process(sideB_.process(sideA_.process(0.5f * (xl - xr))));
        l[i] = finiteOr0(mid + side);
        r[i] = finiteOr0(mid - side);
    }
}

}  // namespace ps
