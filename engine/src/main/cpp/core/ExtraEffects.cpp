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
    const float from = drive_;
    drive_ += (target - drive_) * (1.0f - std::exp(-static_cast<float>(n) / (0.010f * rate_)));
    struct Curve {
        int type;
        double f(double x) const { return Distortion::shape(type, static_cast<float>(x)); }
        double F(double x) const { return Distortion::antiderivative(type, x); }
    } const curve{type};
    // Levelled: a half-scale input comes out about half scale whatever the drive. The fuzz is
    // biased: its wave spends longer on one side (even harmonics, the "fuzz" grit); silence still
    // gives silence.
    const bool gliding = from != drive_;
    float drive = drive_, level = 0.5f / std::tanh(0.5f * drive), bias = type == 1 ? 0.15f * drive : 0.0f;
    float rest = type == 1 ? shape(type, bias) : 0.0f;
    for (int i = 0; i < n; ++i) {
        if (gliding) {
            drive = from + (drive_ - from) * static_cast<float>(i + 1) / static_cast<float>(n);
            level = 0.5f / std::tanh(0.5f * drive);
            bias = type == 1 ? 0.15f * drive : 0.0f;
            rest = type == 1 ? shape(type, bias) : 0.0f;
        }
        const float yl = (shL_.process(finiteOr0(l[i]), drive, bias, curve) - rest) * level;
        const float yr = (shR_.process(finiteOr0(r[i]), drive, bias, curve) - rest) * level;
        const float xl = shL_.dry(), xr = shR_.dry();
        const float bl = yl - dcInL_ + dc * dcOutL_;
        dcInL_ = yl;
        dcOutL_ = bl;
        const float br = yr - dcInR_ + dc * dcOutR_;
        dcInR_ = yr;
        dcOutR_ = br;
        lp1L_ += (bl - lp1L_) * tone;
        lp2L_ += (lp1L_ - lp2L_) * tone;
        lp1R_ += (br - lp1R_) * tone;
        lp2R_ += (lp1R_ - lp2R_) * tone;
        l[i] = xl + mix * (lp2L_ - xl);
        r[i] = xr + mix * (lp2R_ - xr);
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
    for (Split* s : {&left_, &right_}) {
        for (Biquad* q : {&s->lp1a, &s->lp1b, &s->hp1a, &s->hp1b, &s->lp2a, &s->lp2b, &s->hp2a, &s->hp2b}) q->reset();
    }
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
        float bl[3], br[3];
        auto split = [](Split& s, float x, float* band) {
            band[0] = s.lp1b.process(s.lp1a.process(x));
            const float rest = s.hp1b.process(s.hp1a.process(x));
            band[1] = s.lp2b.process(s.lp2a.process(rest));
            band[2] = s.hp2b.process(s.hp2a.process(rest));
        };
        split(left_, xl, bl);
        split(right_, xr, br);
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

}  // namespace ps
