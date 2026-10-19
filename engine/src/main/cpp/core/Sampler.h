#pragma once

#include <array>
#include <cstdint>

#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

// One-shot sample playback for the 16 pads. A pad with a sample plays it instead of its
// synthesized drum. Real-time safe except where noted: never allocates or frees.
class Sampler {
public:
    static constexpr int kSlots = 16;

    Sampler() { stopAll(); }
    ~Sampler();
    Sampler(const Sampler&) = delete;
    Sampler& operator=(const Sampler&) = delete;

    void prepare(int sampleRate);

    // Audio thread: takes over the caller's reference to `sample` (may be nullptr to clear).
    // The previous sample's reference is released; if it was the last one the sample is
    // queued for collectGarbage().
    void install(int slot, Sample* sample);
    bool has(int slot) const { return slot >= 0 && slot < kSlots && slots_[slot] != nullptr; }
    // Schedules a hit `frameOffset` frames into the next render() call.
    void trigger(int slot, float velocity, int frameOffset);
    // Adds the pads' sound (mono) into `out`.
    void render(float* out, int numFrames);
    void stopAll();

    // Not the audio thread: frees samples released by the audio thread.
    void collectGarbage();

private:
    struct Voice {
        const Sample* sample = nullptr;
        int64_t pos = 0;
        float gain = 0.0f;
        int fade = 0;  // > 0: fading out over the remaining frames
    };
    static constexpr int kFadeFrames = 96;  // 2 ms: no click when a hit cuts the previous one

    void retire(Sample* s);
    void renderVoice(Voice& v, float* out, int from, int to);

    std::array<Sample*, kSlots> slots_{};
    std::array<Voice, kSlots> voices_{};
    std::array<Voice, kSlots> tails_{};  // previous hit, fading out
    std::array<int, kSlots> pendingOffset_{};
    std::array<float, kSlots> pendingVelocity_{};
    SpscQueue<Sample*, 64> retired_;
};

}  // namespace ps
