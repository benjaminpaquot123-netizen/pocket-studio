#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <cstring>

#include "Oversampling.h"

namespace ps {

// Audit stage 4: ln(y) for y in [1, 4], without a libm call (the overdrive's antiderivative took
// 9 % of the worst Live case in std::log). The range in 64 cells (the exponent and the mantissa's
// top 5 bits); in each, y = c (1 + r) with |r| < 1/64: ln c from the table and ln(1 + r) by its
// series to r^7 (error under 5e-16, a smooth one: the anti-aliasing divides F's differences, its
// rounding only reaches the output under 1e-11). Tables made in 40-digit decimal arithmetic:
// kInv[i] = 1 / (cell i's middle) rounded, kLn[i] = -ln(kInv[i]).
inline double lnFrom1To4(double y) {
    static constexpr double kInv[65] = {
        0.9846153846153847, 0.9552238805970149, 0.927536231884058, 0.9014084507042254, 0.8767123287671232,
        0.8533333333333334, 0.8311688311688312, 0.810126582278481, 0.7901234567901234, 0.7710843373493976,
        0.7529411764705882, 0.735632183908046, 0.7191011235955056, 0.7032967032967034, 0.6881720430107527,
        0.6736842105263158, 0.6597938144329897, 0.6464646464646465, 0.6336633663366337, 0.6213592233009708,
        0.6095238095238096, 0.5981308411214953, 0.5871559633027523, 0.5765765765765766, 0.5663716814159292,
        0.5565217391304348, 0.5470085470085471, 0.5378151260504201, 0.5289256198347108, 0.5203252032520326,
        0.512, 0.5039370078740157, 0.49230769230769234, 0.47761194029850745, 0.463768115942029,
        0.4507042253521127, 0.4383561643835616, 0.4266666666666667, 0.4155844155844156, 0.4050632911392405,
        0.3950617283950617, 0.3855421686746988, 0.3764705882352941, 0.367816091954023, 0.3595505617977528,
        0.3516483516483517, 0.34408602150537637, 0.3368421052631579, 0.32989690721649484,
        0.32323232323232326, 0.31683168316831684, 0.3106796116504854, 0.3047619047619048,
        0.29906542056074764, 0.29357798165137616, 0.2882882882882883, 0.2831858407079646, 0.2782608695652174,
        0.27350427350427353, 0.2689075630252101, 0.2644628099173554, 0.2601626016260163, 0.256,
        0.25196850393700787, 0.24615384615384617};
    static constexpr double kLn[65] = {
        0.015504186535965199, 0.04580953603129422, 0.07522342123758752, 0.10379679368164355,
        0.13157635778871932, 0.15860503017663852, 0.18492233849401193, 0.21056476910734964,
        0.23556607131276697, 0.259957524436926, 0.2837681731306446, 0.3070250352949119, 0.32975328637246804,
        0.3519764231571781, 0.373716409793584, 0.394993808240869, 0.415827895143711, 0.43623676677491796,
        0.4562374334815876, 0.475845904869964, 0.4950772667978514, 0.5139457511022344, 0.5324647988694717,
        0.5506471179526623, 0.5685047353526688, 0.5860490450035782, 0.6032908514380841, 0.6202404097518576,
        0.6369074622370692, 0.6533012720127456, 0.6694306539426292, 0.6853040030989195, 0.7086513670959105,
        0.7389567165912395, 0.7683706017975328, 0.7969439742415888, 0.8247235383486646, 0.8517522107365838,
        0.8780695190539572, 0.903711949667295, 0.9287132518727123, 0.9531047049968713, 0.9769153536905899,
        1.0001722158548572, 1.0229004669324133, 1.0451236037171234, 1.0668635903535293, 1.0881409888008142,
        1.1089750757036563, 1.1293839473348632, 1.149384614041533, 1.1689930854299093, 1.1882244473577968,
        1.2070929316621797, 1.2256119794294171, 1.2437942985126076, 1.261651915912614, 1.2791962255635234,
        1.2964380319980295, 1.3133875903118029, 1.3300546427970146, 1.3464484525726907, 1.3625778345025745,
        1.3784511836588647, 1.4017985476558559};
    uint64_t bits;
    std::memcpy(&bits, &y, sizeof bits);
    // y = 4: cell 64; a NaN (from a NaN input) stays in the table and comes out NaN, as std::log's.
    const auto i = std::min<size_t>(static_cast<size_t>((bits - 0x3FF0000000000000ull) >> 47), 64);
    const double r = y * kInv[i] - 1.0, r2 = r * r, r4 = r2 * r2;
    return kLn[i] + r + r2 * ((-0.5 + r * (1.0 / 3.0)) + r2 * (-0.25 + r * 0.2) + r4 * (-1.0 / 6.0 + r * (1.0 / 7.0)));
}

// DaisySP's SoftClip (MIT, Utility/dsp.h): x (27 + x^2) / (27 + 9 x^2), +-1 past +-3; and its
// antiderivative x^2/18 + 4/3 ln((x^2 + 3) / 3), then a straight line past +-3.
// (A drive shape for DriveShaper: Saturator's; here for the tests.)
struct SoftClipShape {
    double f(double x) const { return x <= -3.0 ? -1.0 : x >= 3.0 ? 1.0 : x * (27.0 + x * x) / (27.0 + 9.0 * x * x); }
    double F(double x) const {
        const double a = std::fabs(x);
        if (a >= 3.0) return 2.348392481493110 + (a - 3.0);  // F(3) = 1/2 + 4/3 ln 4
        const double x2 = x * x;
        return x2 * (1.0 / 18.0) + (4.0 / 3.0) * lnFrom1To4(x2 * (1.0 / 3.0) + 1.0);
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
