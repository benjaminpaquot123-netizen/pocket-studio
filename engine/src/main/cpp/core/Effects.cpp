#include "Effects.h"

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

// --- EQ --------------------------------------------------------------------------------------

void Eq3::prepare(float rate) {
    rate_ = rate;
    update();
    reset();
}

void Eq3::setParam(int p, float v) {
    if (p < 0 || p >= 3) return;
    gain_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
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
    if (p < 0 || p >= 4) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
    update();
}

void Compressor::update() {
    threshold_ = -40.0f + 40.0f * p_[0];
    ratio_ = expMap(p_[1], 1.0f, 20.0f);
    const float attackMs = expMap(p_[2], 0.1f, 80.0f), releaseMs = expMap(p_[3], 20.0f, 800.0f);
    attack_ = std::exp(-1.0f / (0.001f * attackMs * rate_));
    release_ = std::exp(-1.0f / (0.001f * releaseMs * rate_));
    // Half of the reduction a full-scale signal would get, given back.
    makeup_ = dbToGain(-threshold_ * (1.0f - 1.0f / ratio_) * 0.5f);
}

void Compressor::process(float* l, float* r, int n) {
    constexpr float kKnee = 6.0f;
    float reduction = 0.0f;
    for (int i = 0; i < n; ++i) {
        const float level = std::max(std::fabs(l[i]), std::fabs(r[i]));
        const float inDb = level > 1e-6f ? 20.0f * std::log10(level) : -120.0f;
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
        const float g = dbToGain(gr) * makeup_;
        l[i] *= g;
        r[i] *= g;
    }
    reduction_ = reduction;
}

// --- Saturation ------------------------------------------------------------------------------

void Saturator::prepare(float rate) {
    rate_ = rate;
    odL_.Init();
    odR_.Init();
    update();
    reset();
}

void Saturator::setParam(int p, float v) {
    if (p < 0 || p >= 4) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
    update();
}

void Saturator::update() {
    // DaisySP's drive above ~0.9 is a hard square: keep the useful range.
    odL_.SetDrive(0.05f + 0.8f * p_[0]);
    odR_.SetDrive(0.05f + 0.8f * p_[0]);
    toneCoef_ = onePoleCoef(rate_, expMap(p_[1], 1500.0f, 18000.0f));
    mix_ = p_[2];
    level_ = p_[3] / 0.7f;
}

void Saturator::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        lpL_ += toneCoef_ * (odL_.Process(l[i]) - lpL_);
        lpR_ += toneCoef_ * (odR_.Process(r[i]) - lpR_);
        l[i] = (l[i] + mix_ * (lpL_ - l[i])) * level_;
        r[i] = (r[i] + mix_ * (lpR_ - r[i])) * level_;
    }
}

// --- Chorus ----------------------------------------------------------------------------------

void StereoChorus::prepare(float rate) {
    chorus_.Init(rate);
    update();
}

void StereoChorus::setParam(int p, float v) {
    if (p < 0 || p >= 4) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
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

// --- Filter ----------------------------------------------------------------------------------

void SweepFilter::prepare(float rate) {
    rate_ = rate;
    left_.Init(rate);
    right_.Init(rate);
    update();
}

void SweepFilter::setParam(int p, float v) {
    if (p < 0 || p >= 4) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
    update();
}

void SweepFilter::update() {
    const float f = std::min(expMap(p_[0], 40.0f, 18000.0f), rate_ * 0.45f);
    left_.SetFreq(f);
    right_.SetFreq(f);
    left_.SetRes(0.95f * p_[1]);
    right_.SetRes(0.95f * p_[1]);
    type_ = std::min(2, static_cast<int>(p_[2] * 3.0f));
    mix_ = p_[3];
}

void SweepFilter::process(float* l, float* r, int n) {
    for (int i = 0; i < n; ++i) {
        left_.Process(l[i]);
        right_.Process(r[i]);
        const float a = type_ == 0 ? left_.Low() : type_ == 1 ? left_.Band() : left_.High();
        const float b = type_ == 0 ? right_.Low() : type_ == 1 ? right_.Band() : right_.High();
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
    if (p < 0 || p >= 4) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
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

// --- Insert slot -----------------------------------------------------------------------------

void InsertSlot::prepare(float rate) {
    eq_.prepare(rate);
    comp_.prepare(rate);
    sat_.prepare(rate);
    chorus_.prepare(rate);
    filter_.prepare(rate);
    pump_.prepare(rate);
}

void InsertSlot::setType(int type) {
    if (type < 0 || type >= kTypes || type == type_) return;
    type_ = type;
    switch (type_) {
        case Eq: eq_.reset(); break;
        case Comp: comp_.reset(); break;
        case Saturation: sat_.reset(); break;
        case Chorus: chorus_.reset(); break;
        case Filter: filter_.reset(); break;
        case Pump: pump_.reset(); break;
        default: break;
    }
}

void InsertSlot::setParam(int p, float v) {
    switch (type_) {
        case Eq: eq_.setParam(p, v); break;
        case Comp: comp_.setParam(p, v); break;
        case Saturation: sat_.setParam(p, v); break;
        case Chorus: chorus_.setParam(p, v); break;
        case Filter: filter_.setParam(p, v); break;
        case Pump: pump_.setParam(p, v); break;
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
    update();
    reset();
}

void Reverb::setParam(int p, float v) {
    if (p < 0 || p >= 4) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
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
        // Pre-delay on the mono sum, then the tank.
        const float x = (inL[i] + inR[i]) * kFixedGain;
        const size_t readIdx = (preIdx_ + pre_.size() - preLen_) % pre_.size();
        const float input = preLen_ <= 1 ? x : pre_[readIdx];
        pre_[preIdx_] = x;
        preIdx_ = (preIdx_ + 1) % pre_.size();
        float l = 0.0f, r = 0.0f;
        for (size_t k = 0; k < 8; ++k) {
            for (int side = 0; side < 2; ++side) {
                Comb& c = side == 0 ? combL_[k] : combR_[k];
                const float out = c.buf[c.idx];
                c.store = out * damp2_ + c.store * damp1_;
                c.buf[c.idx] = input + c.store * feedback_;
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

void TempoDelay::prepare(float rate) {
    rate_ = rate;
    bufL_.assign(static_cast<size_t>(kMaxSeconds * rate) + 4, 0.0f);
    bufR_.assign(bufL_.size(), 0.0f);
    smooth_ = 1.0f - std::exp(-1.0f / (0.05f * rate));  // 50 ms glide on time changes
    update();
    current_ = target_;
    reset();
}

void TempoDelay::setParam(int p, float v) {
    if (p < 0 || p >= 4) return;
    p_[static_cast<size_t>(p)] = std::clamp(v, 0.0f, 1.0f);
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
    const int steps = kSteps[std::min(5, static_cast<int>(p_[0] * 6.0f))];
    const float maxFrames = static_cast<float>(bufL_.empty() ? 1 : bufL_.size() - 2);
    target_ = std::clamp(static_cast<float>(steps * stepFrames_), 1.0f, maxFrames);
    feedback_ = 0.9f * p_[1];
    pingPong_ = p_[2];
    toneCoef_ = onePoleCoef(rate_, expMap(p_[3], 800.0f, 16000.0f));
}

void TempoDelay::reset() {
    std::fill(bufL_.begin(), bufL_.end(), 0.0f);
    std::fill(bufR_.begin(), bufR_.end(), 0.0f);
    write_ = 0;
    lpL_ = lpR_ = 0.0f;
}

float TempoDelay::read(const std::vector<float>& buf, float delay) const {
    const float pos = static_cast<float>(write_) - delay + static_cast<float>(buf.size());
    const auto i0 = static_cast<size_t>(pos);
    const float frac = pos - static_cast<float>(i0);
    const float a = buf[i0 % buf.size()], b = buf[(i0 + 1) % buf.size()];
    return a + frac * (b - a);
}

void TempoDelay::process(const float* inL, const float* inR, float* outL, float* outR, int n) {
    if (bufL_.empty()) return;
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
        outL[i] += dl;
        outR[i] += dr;
    }
}

}  // namespace ps
