#include "PlateReverb.h"

#include <algorithm>
#include <cmath>
#include <vector>

#include "plaits/dsp/fx/fx_engine.h"

namespace ps {

namespace {
// Clouds' lengths (32 kHz) times 1.5: the same room at 48 kHz.
constexpr size_t kMemory = 32768;
using E = plaits::FxEngine<kMemory, plaits::FORMAT_32_BIT>;
}  // namespace

struct PlateReverb::Impl {
    E engine;
    std::vector<float> buffer;
    float rate = 48000.0f;
    float time = 0.7f, lp = 0.7f, diffusion = 0.625f;
    float lp1 = 0.0f, lp2 = 0.0f;
    // The input's gain (Clouds: 0.2) and the output's, set so that a hit through the plate is
    // about as loud as through the hall (Freeverb) at the same settings.
    static constexpr float kInputGain = 0.2f;
    static constexpr float kOutputGain = 3.4f;
};

PlateReverb::PlateReverb() : impl_(std::make_unique<Impl>()) {}
PlateReverb::~PlateReverb() = default;

void PlateReverb::prepare(float rate) {
    impl_->rate = rate;
    impl_->buffer.assign(kMemory, 0.0f);
    impl_->engine.Init(impl_->buffer.data());
    // Clouds' LFOs: 0.5 and 0.3 Hz (their frequency is per sample).
    impl_->engine.SetLFOFrequency(plaits::LFO_1, 0.5f / rate);
    impl_->engine.SetLFOFrequency(plaits::LFO_2, 0.3f / rate);
    reset();
}

void PlateReverb::reset() {
    if (!impl_->buffer.empty()) std::fill(impl_->buffer.begin(), impl_->buffer.end(), 0.0f);
    impl_->lp1 = impl_->lp2 = 0.0f;
}

void PlateReverb::set(float time, float lp, float diffusion) {
    impl_->time = std::clamp(time, 0.0f, 1.0f);  // 1: frozen (the loop keeps everything)
    impl_->lp = std::clamp(lp, 0.05f, 1.0f);
    impl_->diffusion = std::clamp(diffusion, 0.0f, 0.8f);
}

void PlateReverb::process(const float* in, float* outL, float* outR, int n) {
    if (impl_->buffer.empty()) return;
    typedef E::Reserve<170,
      E::Reserve<243,
      E::Reserve<362,
      E::Reserve<599,
      E::Reserve<2480,
      E::Reserve<3057,
      E::Reserve<5117,
      E::Reserve<2870,
      E::Reserve<2495,
      E::Reserve<7173> > > > > > > > > > Memory;
    E::DelayLine<Memory, 0> ap1;
    E::DelayLine<Memory, 1> ap2;
    E::DelayLine<Memory, 2> ap3;
    E::DelayLine<Memory, 3> ap4;
    E::DelayLine<Memory, 4> dap1a;
    E::DelayLine<Memory, 5> dap1b;
    E::DelayLine<Memory, 6> del1;
    E::DelayLine<Memory, 7> dap2a;
    E::DelayLine<Memory, 8> dap2b;
    E::DelayLine<Memory, 9> del2;
    E::Context c;
    const float kap = impl_->diffusion;
    const float klp = impl_->lp;
    const float krt = impl_->time;
    float lp1 = impl_->lp1, lp2 = impl_->lp2;
    for (int i = 0; i < n; ++i) {
        float wet = 0.0f, apout = 0.0f;
        impl_->engine.Start(&c);
        // Smear AP1 inside the loop.
        c.Interpolate(ap1, 15.0f, plaits::LFO_1, 90.0f, 1.0f);
        c.Write(ap1, 150, 0.0f);
        c.Read(in[i], Impl::kInputGain);
        // Diffuse through 4 allpasses.
        c.Read(ap1 TAIL, kap);
        c.WriteAllPass(ap1, -kap);
        c.Read(ap2 TAIL, kap);
        c.WriteAllPass(ap2, -kap);
        c.Read(ap3 TAIL, kap);
        c.WriteAllPass(ap3, -kap);
        c.Read(ap4 TAIL, kap);
        c.WriteAllPass(ap4, -kap);
        c.Write(apout);
        // Main reverb loop.
        c.Load(apout);
        c.Interpolate(del2, 7020.0f, plaits::LFO_2, 150.0f, krt);
        c.Lp(lp1, klp);
        c.Read(dap1a TAIL, -kap);
        c.WriteAllPass(dap1a, kap);
        c.Read(dap1b TAIL, kap);
        c.WriteAllPass(dap1b, -kap);
        c.Write(del1, 2.0f);
        c.Write(wet, 0.0f);
        outL[i] += wet * Impl::kOutputGain;
        c.Load(apout);
        c.Read(del1 TAIL, krt);
        c.Lp(lp2, klp);
        c.Read(dap2a TAIL, kap);
        c.WriteAllPass(dap2a, -kap);
        c.Read(dap2b TAIL, -kap);
        c.WriteAllPass(dap2b, kap);
        c.Write(del2, 2.0f);
        c.Write(wet, 0.0f);
        outR[i] += wet * Impl::kOutputGain;
    }
    // A denormal-free tail: the loop's low-passes rest at exactly zero.
    impl_->lp1 = std::fabs(lp1) < 1e-20f ? 0.0f : lp1;
    impl_->lp2 = std::fabs(lp2) < 1e-20f ? 0.0f : lp2;
}

}  // namespace ps
