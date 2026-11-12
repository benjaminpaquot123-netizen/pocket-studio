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
    (void)l;
    (void)r;
    (void)n;
}

}  // namespace ps
