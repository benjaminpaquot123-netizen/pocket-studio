#include "SpeakerKick.h"

#include <cmath>

namespace ps {

namespace {
float coefMs(float rate, float ms) { return 1.0f - std::exp(-1.0f / (0.001f * ms * rate)); }
}  // namespace

void SpeakerKick::prepare(float rate) {
    lowA_.lowPassQ(rate, kLowHz, 0.5412f);
    lowB_.lowPassQ(rate, kLowHz, 1.3066f);
    hpA_.highPassQ(rate, kHarmLowHz, 0.5412f);
    hpB_.highPassQ(rate, kHarmLowHz, 1.3066f);
    top_.lowPass(rate, kHarmTopHz);
    clickHp_.highPass(rate, kClickLowHz);
    clickLp_.lowPass(rate, kClickTopHz);
    envUp_ = coefMs(rate, 2.0f);
    envDown_ = coefMs(rate, 60.0f);
    glide_ = coefMs(rate, 20.0f);
    reset();
}

void SpeakerKick::clear() {
    for (Biquad* b : {&lowA_, &lowB_, &hpA_, &hpB_, &top_, &clickHp_, &clickLp_}) b->reset();
    env_ = 0.0f;
}

void SpeakerKick::reset() {
    clear();
    level_ = on_ ? 1.0f : 0.0f;
}

void SpeakerKick::setOn(bool on) {
    // Switched on from rest: the filters start from silence, not from an old state.
    if (on && !on_ && level_ == 0.0f) clear();
    on_ = on;
}

void SpeakerKick::process(float* l, float* r, int n) {
    if (!on_ && level_ == 0.0f) return;  // off and at rest: untouched
    const float target = on_ ? 1.0f : 0.0f;
    for (int i = 0; i < n; ++i) {
        level_ += (target - level_) * glide_;
        const float mid = 0.5f * (l[i] + r[i]);
        const float low = lowB_.process(lowA_.process(mid));
        const float mag = std::fabs(low);
        env_ += (mag - env_) * (mag > env_ ? envUp_ : envDown_);
        // The low band as a wave of steady size (silence stays silence).
        const float w = env_ > 1e-5f ? low / env_ : 0.0f;
        const float odd = std::tanh(kDrive * w);
        const float shaped = 0.7f * odd + 0.3f * odd * odd;
        const float harm = top_.process(hpB_.process(hpA_.process(shaped))) * env_;
        const float click = clickLp_.process(clickHp_.process(mid));
        const float add = level_ * (kHarmonics * harm + kClick * click);
        l[i] += add;
        r[i] += add;
    }
    if (!on_ && level_ < 1e-4f) level_ = 0.0f;
}

}  // namespace ps
