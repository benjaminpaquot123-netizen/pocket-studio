#pragma once

#include <array>
#include <cstdint>

#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

// Sample playback for the 16 pads. A pad with a sample plays it instead of its synthesized
// drum. Each pad has its own region, pitch, direction, loop, envelope and choke group.
// Real-time safe except where noted: never allocates or frees.
class Sampler {
public:
    static constexpr int kSlots = 16;
    static constexpr int kChokeGroups = 4;

    // Order is part of the JNI contract (PadParam in Kotlin): append only.
    enum Param : int {
        Start,    // 0..1 of the sound
        End,      // 0..1 of the sound (> Start)
        Pitch,    // semitones, -24..24
        Reverse,  // 1 = backwards
        Loop,     // 1 = repeats the region until the pad is hit again, choked or stopped
        Attack,   // seconds, 0..2
        Decay,    // seconds, 0..2
        Sustain,  // 0..1 level after the decay
        Release,  // seconds, 0..2: fade before the end of the region
        Choke,    // 0 = none, 1..4: a hit silences the other pads of the same group
        Gain,     // 0..2
        kParamCount
    };

    struct PadParams {
        float start = 0.0f, end = 1.0f, pitch = 0.0f;
        bool reverse = false, loop = false;
        float attack = 0.0f, decay = 0.0f, sustain = 1.0f, release = 0.003f;
        int choke = 0;
        float gain = 1.0f;
    };

    Sampler() { stopAll(); }
    ~Sampler();
    Sampler(const Sampler&) = delete;
    Sampler& operator=(const Sampler&) = delete;

    void prepare(int sampleRate);

    // Audio thread: takes over the caller's reference to `sample` (may be nullptr to clear).
    // A sound still playing on the slot fades out over 5 ms instead of being cut (a looping pad
    // goes on with the new sound at the same point of its region: a tempo change keeps the loop);
    // the previous sample's reference is released once nothing plays it any more, and if it was
    // the last one the sample is queued for collectGarbage().
    void install(int slot, Sample* sample);
    bool has(int slot) const { return slot >= 0 && slot < kSlots && slots_[slot] != nullptr; }
    void setParam(int slot, int param, float value);
    const PadParams& params(int slot) const { return params_[slot]; }
    // Schedules a hit `frameOffset` frames into the next render() call, panned -1..1, `pitch`
    // semitones added to the pad's tuning for this hit only.
    void trigger(int slot, float velocity, int frameOffset, float pan = 0.0f, float pitch = 0.0f);
    // Adds the pads' sound into `left` / `right` (stereo sounds stay stereo).
    void render(float* left, float* right, int numFrames);
    // Each pad into its own pair (lefts[slot], rights[slot]; several pads may share one).
    void render(float* const* lefts, float* const* rights, int numFrames);
    // Fades out looping pads (transport stop).
    void releaseLoops();
    void stopAll();

    // Not the audio thread: frees samples released by the audio thread.
    void collectGarbage();

private:
    struct Voice {
        const Sample* sample = nullptr;
        double pos = 0.0;      // in source frames
        double step = 1.0;     // source frames per output frame (negative when reversed)
        double lo = 0.0, hi = 0.0;  // region [lo, hi)
        bool loop = false;
        float gain = 0.0f;
        float gainL = 1.0f, gainR = 1.0f;  // pan, centre = unity on each side
        int64_t age = 0;       // output frames since the hit
        int64_t length = 0;    // output frames of one pass (for the release)
        float attackFrames = 0.0f, decayFrames = 0.0f, sustain = 1.0f, releaseFrames = 1.0f;
        float velocity = 0.0f, hitPitch = 0.0f;  // of the hit (a swapped sound keeps them)
        int fade = 0;          // > 0: fading out over the remaining frames
        int fadeLength = 1;    // frames of that fade
        int fadeIn = 0;        // > 0: fading in (a loop going on with a swapped sound)
        int fadeInLength = 1;
    };
    static constexpr int kFadeFrames = 96;  // 2 ms: no click when a hit is cut

    void retire(Sample* s);
    // Region, speed and envelope of the slot's settings on `s` for a hit (position left alone).
    void shape(Voice& v, const Sample* s, const PadParams& p, float velocity, float pitch) const;
    void start(int slot, float velocity, float pan, float pitch);
    void fadeOut(int slot);
    void renderVoice(Voice& v, float* left, float* right, int from, int to);

    int sampleRate_ = 48000;
    std::array<Sample*, kSlots> slots_{};
    std::array<PadParams, kSlots> params_{};
    std::array<Voice, kSlots> voices_{};
    std::array<Voice, kSlots> tails_{};  // previous hit, fading out
    // A replaced sound the slot's tail still plays: its reference is released when the tail ends.
    std::array<Sample*, kSlots> held_{};
    int swapFadeFrames_ = 240;  // 5 ms: a replaced sound fades out (and a loop's new sound in)
    std::array<int, kSlots> pendingOffset_{};
    std::array<float, kSlots> pendingVelocity_{};
    std::array<float, kSlots> pendingPan_{};
    std::array<float, kSlots> pendingPitch_{};
    SpscQueue<Sample*, 64> retired_;
};

}  // namespace ps
