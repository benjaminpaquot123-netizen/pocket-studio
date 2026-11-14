#pragma once

// HIIR (Laurent de Soras, WTFPL v2, engine/third_party/hiir): polyphase IIR half-band filters,
// cascaded for 4x. The coefficient set C4x099 rejects images by 99 dB and gives the round trip
// (up then down) a phase delay of exactly kDelay base-rate frames: a dry path delayed by kDelay
// lines up with the processed one. Everything is inline state: no allocation, real-time safe.
#include "hiir/Downsampler2xFpu.h"
#include "hiir/Downsampler2xTpl.h"
#include "hiir/Upsampler2xFpu.h"
#include "hiir/Upsampler2xTpl.h"
#include "hiir/coef/C4x.h"

namespace ps {

class Oversampler4x {
public:
    static constexpr int kFactor = 4;
    using Coefs = hiir::coef::C4x099;
    static constexpr int kDelay = Coefs::_delay;  // base-rate frames, up + down

    Oversampler4x() {
        up1_.set_coefs(Coefs::X2::_coef_list.data());
        up2_.set_coefs(Coefs::X4::_coef_list.data());
        down2_.set_coefs(Coefs::X4::_coef_list.data());
        down1_.set_coefs(Coefs::X2::_coef_list.data());
    }

    void reset() {
        up1_.clear_buffers();
        up2_.clear_buffers();
        resetDown();
    }
    // The way down only (a path whose input stayed at zero long enough to skip it).
    void resetDown() {
        down2_.clear_buffers();
        down1_.clear_buffers();
    }

    // One base-rate sample in, four at the high rate out.
    void up(float x, float out[kFactor]) {
        float a = 0.0f, b = 0.0f;
        up1_.process_sample(a, b, x);
        up2_.process_sample(out[0], out[1], a);
        up2_.process_sample(out[2], out[3], b);
    }

    // Four high-rate samples in, one base-rate sample out.
    float down(const float in[kFactor]) {
        const float mid[2] = {down2_.process_sample(in), down2_.process_sample(in + 2)};
        return down1_.process_sample(mid);
    }

private:
    hiir::Upsampler2xFpu<Coefs::X2::_nbr_coef> up1_;
    hiir::Upsampler2xFpu<Coefs::X4::_nbr_coef> up2_;
    hiir::Downsampler2xFpu<Coefs::X4::_nbr_coef> down2_;
    hiir::Downsampler2xFpu<Coefs::X2::_nbr_coef> down1_;
};

// Audit stage 4: two channels in one register (the GCC/Clang vector extension: SSE on x86, NEON on
// ARM), so HIIR's filters run a stereo pair at once: lane by lane the very float operations of
// Oversampler4x, half the instructions.
typedef float Float2Lanes __attribute__((vector_size(8)));
struct Float2 {
    Float2Lanes v;
    Float2() = default;
    Float2(Float2Lanes x) : v(x) {}
    Float2(float a, float b) : v(Float2Lanes{a, b}) {}
    // HIIR builds its coefficients and zeros from scalars: the same value in both lanes, rounded
    // to float as Oversampler4x's are.
    explicit Float2(double x) : v(Float2Lanes{static_cast<float>(x), static_cast<float>(x)}) {}
    explicit Float2(float x) : v(Float2Lanes{x, x}) {}
    explicit Float2(int x) : Float2(static_cast<float>(x)) {}
    Float2& operator+=(Float2 o) { v += o.v; return *this; }
    Float2& operator-=(Float2 o) { v -= o.v; return *this; }
    Float2& operator*=(Float2 o) { v *= o.v; return *this; }
    friend Float2 operator+(Float2 a, Float2 b) { return Float2(a.v + b.v); }
    friend Float2 operator-(Float2 a, Float2 b) { return Float2(a.v - b.v); }
    friend Float2 operator*(Float2 a, Float2 b) { return Float2(a.v * b.v); }
};

// Oversampler4x for a stereo pair: lane 0 is one channel, lane 1 the other, each exactly as its own
// Oversampler4x would make it.
class Oversampler4xPair {
public:
    static constexpr int kFactor = Oversampler4x::kFactor;
    static constexpr int kDelay = Oversampler4x::kDelay;
    using Coefs = Oversampler4x::Coefs;

    Oversampler4xPair() {
        up1_.set_coefs(Coefs::X2::_coef_list.data());
        up2_.set_coefs(Coefs::X4::_coef_list.data());
        down2_.set_coefs(Coefs::X4::_coef_list.data());
        down1_.set_coefs(Coefs::X2::_coef_list.data());
    }
    void reset() {
        up1_.clear_buffers();
        up2_.clear_buffers();
        down2_.clear_buffers();
        down1_.clear_buffers();
    }
    void up(Float2 x, Float2 out[kFactor]) {
        Float2 a, b;
        up1_.process_sample(a, b, x);
        up2_.process_sample(out[0], out[1], a);
        up2_.process_sample(out[2], out[3], b);
    }
    Float2 down(const Float2 in[kFactor]) {
        const Float2 mid[2] = {down2_.process_sample(in), down2_.process_sample(in + 2)};
        return down1_.process_sample(mid);
    }

private:
    hiir::Upsampler2xTpl<Coefs::X2::_nbr_coef, Float2, 2> up1_;
    hiir::Upsampler2xTpl<Coefs::X4::_nbr_coef, Float2, 2> up2_;
    hiir::Downsampler2xTpl<Coefs::X4::_nbr_coef, Float2, 2> down2_;
    hiir::Downsampler2xTpl<Coefs::X2::_nbr_coef, Float2, 2> down1_;
};

}  // namespace ps
