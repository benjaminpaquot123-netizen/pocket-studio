#pragma once

#include <memory>

namespace ps {

// A plate: Mutable Instruments Clouds' reverb (MIT, Emilie Gillet; the Griesinger topology of
// Dattorro's paper: four input diffusers, then a loop of two allpass + delay arms, modulated),
// ported to 48 kHz (its delay lengths, set for 32 kHz, times 1.5) and to float storage (Clouds keeps
// 12 bits). Plaits' FxEngine, already vendored, runs it. 100 % wet. The memory is the heap's,
// allocated by prepare(); process() is real-time safe.
class PlateReverb {
public:
    PlateReverb();
    ~PlateReverb();
    PlateReverb(const PlateReverb&) = delete;
    PlateReverb& operator=(const PlateReverb&) = delete;

    void prepare(float rate);
    void reset();
    // time 0..1 (the loop's gain: 0.5 about 2 s, 1 holds for ever), lp 0..1 (the tail's brightness), diffusion 0..0.8.
    void set(float time, float lp, float diffusion);
    // Adds the wet signal of the mono `in` into `outL`, `outR` (n frames).
    void process(const float* in, float* outL, float* outR, int n);

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace ps
