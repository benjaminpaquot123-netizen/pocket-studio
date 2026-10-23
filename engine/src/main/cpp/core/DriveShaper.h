#pragma once

#include <array>
#include <cmath>

#include "Oversampling.h"

namespace ps {

// One channel of a drive's waveshaper with its aliasing cut twice: run at 4x (HIIR), and with
// first-order antiderivative anti-aliasing (Parker, Zavalishin, Le Bivic, DAFx-16): each output
// is the shape's mean between two inputs, the integral of the shape over their segment, which
// takes off the harmonics the clipping's corners throw past the oversampled band. A Shape gives f
// (the curve) and F (its antiderivative). That mean is also a mean of two neighbouring samples
// (-0.4 dB at 18 kHz where the curve is straight): a one-zero lift before the curve, flat to
// 0.03 dB up to 18 kHz, puts it back. The output is kDelay frames late; dry() is the input as late.
template <class Oversampler = Oversampler4x>
class DriveShaper {
public:
    static constexpr int kDelay = Oversampler::kDelay;

    void reset() {
        os_.reset();
        prevU_ = prevF_ = 0.0;
        prevX_ = 0.0f;
        dry_.fill(0.0f);
        pos_ = 0;
    }

    // The curve at u = x * gain + offset, at each of the oversampled points.
    template <class Shape>
    float process(float x, float gain, float offset, const Shape& shape) {
        dry_[static_cast<size_t>(pos_)] = x;
        pos_ = (pos_ + 1) % (kDelay + 1);
        float up[Oversampler::kFactor];
        os_.up(x, up);
        for (float& v : up) {
            // The lift: (1 + a) x[n] - a x[n-1], |H|^2 = 1 + 2a(1 + a)(1 - cos w) against the
            // mean's 1 / cos^2(w / 2): a(1 + a) = 1/4.
            constexpr float kLift = 0.20710678f;
            const float lifted = (1.0f + kLift) * v - kLift * prevX_;
            prevX_ = v;
            const double u = static_cast<double>(lifted) * gain + offset;
            const double fu = shape.F(u);
            const double d = u - prevU_;
            // Two inputs too close to divide (F's rounding would show): the curve at their middle,
            // the same limit.
            v = static_cast<float>(std::fabs(d) > 1.0e-4 ? (fu - prevF_) / d : shape.f(0.5 * (u + prevU_)));
            prevU_ = u;
            prevF_ = fu;
        }
        return os_.down(up);
    }
    // The input kDelay frames back (after process()): the dry signal lined up with the shaped one.
    float dry() const { return dry_[static_cast<size_t>(pos_)]; }

private:
    Oversampler os_;
    double prevU_ = 0.0, prevF_ = 0.0;
    float prevX_ = 0.0f;
    std::array<float, kDelay + 1> dry_{};
    int pos_ = 0;
};

}  // namespace ps
