#pragma once

// HIIR (Laurent de Soras, WTFPL v2, engine/third_party/hiir): polyphase IIR half-band filters,
// cascaded for 4x. The coefficient set C4x099 rejects images by 99 dB and gives the round trip
// (up then down) a phase delay of exactly kDelay base-rate frames: a dry path delayed by kDelay
// lines up with the processed one. Everything is inline state: no allocation, real-time safe.
#include "hiir/Downsampler2xFpu.h"
#include "hiir/Upsampler2xFpu.h"
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

}  // namespace ps
