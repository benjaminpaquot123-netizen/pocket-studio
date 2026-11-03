#pragma once

#include <array>
#include <cmath>

#include "Oversampling.h"

namespace ps {

// DaisySP's SoftClip (MIT, Utility/dsp.h): x (27 + x^2) / (27 + 9 x^2), +-1 past +-3; and its
// antiderivative x^2/18 + 4/3 ln((x^2 + 3) / 3), then a straight line past +-3.
// (Saturator's shape; here for the tests.)
struct SoftClipShape {
    double f(double x) const { return x <= -3.0 ? -1.0 : x >= 3.0 ? 1.0 : x * (27.0 + x * x) / (27.0 + 9.0 * x * x); }
    double F(double x) const {
        const double a = std::fabs(x);
        if (a >= 3.0) return 2.348392481493110 + (a - 3.0);  // F(3) = 1/2 + 4/3 ln 4
        return x * x / 18.0 + (4.0 / 3.0) * std::log((x * x + 3.0) / 3.0);
    }
};

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
        pos_ = pos_ == kDelay ? 0 : pos_ + 1;
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

    // Audit stage 4: the same maths as process(), frame for frame and bit for bit, for a pair of
    // channels and up to kMaxBlock frames at once, in three passes (all the way up, the curve,
    // all the way down). Sample by sample, each frame waited on the one before through the filters'
    // and the curve's long chains; in passes the CPU overlaps them (about a third less time).
    // gain[i] and offset[i] (offset may be null: 0) are frame i's; wet and dry get frame i's shaped
    // output and its input kDelay frames back, as process() then dry() would.
    static constexpr int kMaxBlock = 64;
    template <class Shape>
    static void processPair(DriveShaper& a, DriveShaper& b, const float* xa, const float* xb, float* wetA, float* wetB, float* dryA,
                            float* dryB, int n, const float* gain, const float* offset, const Shape& shape) {
        constexpr int kF = Oversampler::kFactor;
        float upA[kMaxBlock * kF], upB[kMaxBlock * kF];
        for (int i = 0; i < n; ++i) {
            dryA[i] = a.delayDry(xa[i]);
            dryB[i] = b.delayDry(xb[i]);
            a.os_.up(xa[i], upA + i * kF);
            b.os_.up(xb[i], upB + i * kF);
        }
        shapePair(a, b, upA, upB, n, gain, offset, shape);
        for (int i = 0; i < n; ++i) {
            wetA[i] = a.os_.down(upA + i * kF);
            wetB[i] = b.os_.down(upB + i * kF);
        }
    }

private:
    float delayDry(float x) {
        dry_[static_cast<size_t>(pos_)] = x;
        pos_ = pos_ == kDelay ? 0 : pos_ + 1;
        return dry_[static_cast<size_t>(pos_)];
    }
    // The curve of process() on both channels' oversampled points, interleaved (two independent chains).
    template <class Shape>
    static void shapePair(DriveShaper& a, DriveShaper& b, float* va, float* vb, int n, const float* gain, const float* offset,
                          const Shape& shape) {
        constexpr int kF = Oversampler::kFactor;
        constexpr float kLift = 0.20710678f;
        double pUa = a.prevU_, pFa = a.prevF_, pUb = b.prevU_, pFb = b.prevF_;
        float pXa = a.prevX_, pXb = b.prevX_;
        for (int i = 0; i < n; ++i) {
            const float g = gain[i], o = offset ? offset[i] : 0.0f;
            for (int k = i * kF; k < (i + 1) * kF; ++k) {
                const float la = (1.0f + kLift) * va[k] - kLift * pXa;
                const float lb = (1.0f + kLift) * vb[k] - kLift * pXb;
                pXa = va[k];
                pXb = vb[k];
                const double ua = static_cast<double>(la) * g + o, ub = static_cast<double>(lb) * g + o;
                const double fa = shape.F(ua), fb = shape.F(ub);
                const double da = ua - pUa, db = ub - pUb;
                va[k] = static_cast<float>(std::fabs(da) > 1.0e-4 ? (fa - pFa) / da : shape.f(0.5 * (ua + pUa)));
                vb[k] = static_cast<float>(std::fabs(db) > 1.0e-4 ? (fb - pFb) / db : shape.f(0.5 * (ub + pUb)));
                pUa = ua;
                pFa = fa;
                pUb = ub;
                pFb = fb;
            }
        }
        a.prevU_ = pUa;
        a.prevF_ = pFa;
        a.prevX_ = pXa;
        b.prevU_ = pUb;
        b.prevF_ = pFb;
        b.prevX_ = pXb;
    }

    Oversampler os_;
    double prevU_ = 0.0, prevF_ = 0.0;
    float prevX_ = 0.0f;
    std::array<float, kDelay + 1> dry_{};
    int pos_ = 0;
};

}  // namespace ps
