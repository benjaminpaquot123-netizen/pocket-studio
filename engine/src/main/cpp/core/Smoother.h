#pragma once

#include <cmath>

namespace ps {

// One-pole parameter smoother used to avoid zipper noise and clicks.
class Smoother {
public:
    Smoother() = default;
    explicit Smoother(float value) : current_(value), target_(value) {}
    void configure(float sampleRate, float timeMs) {
        coeff_ = std::exp(-1.0f / (0.001f * timeMs * sampleRate));
    }
    void setTarget(float target) { target_ = target; }
    void snap(float value) { current_ = target_ = value; }
    float next() {
        current_ = target_ + coeff_ * (current_ - target_);
        return current_;
    }
    float current() const { return current_; }
    float target() const { return target_; }

private:
    float coeff_ = 0.0f;
    float current_ = 0.0f;
    float target_ = 0.0f;
};

}  // namespace ps
