#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cstdint>

#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

// Live pads: each slot holds a sound that a tap switches on and the next tap switches off.
// A looping slot restarts every `beats` beats on a shared beat clock (so a kick of any length
// set to 1 beat plays four on the floor, a 2-bar melody set to 8 beats loops on the bar);
// a one-shot slot plays its sound once per tap. Launches wait for the next grid line
// (the loop length, at most a bar). The clock runs on its own when the transport is stopped
// and snaps to the sequencer's beats while it plays.
// Real-time safe except where noted: never allocates or frees.
class ClipLauncher {
public:
    static constexpr int kSlots = 16;
    static constexpr int kMaxBeats = 64;
    enum Param { Beats = 0, OneShot = 1, Gain = 2 };
    enum State : uint8_t { Off = 0, Waiting = 1, Playing = 2 };

    ClipLauncher() = default;
    ~ClipLauncher();
    ClipLauncher(const ClipLauncher&) = delete;
    ClipLauncher& operator=(const ClipLauncher&) = delete;

    void prepare(int sampleRate) { fadeFrames_ = std::max(1, sampleRate / 200); }
    // Takes over the caller's reference (nullptr clears the slot).
    void install(int slot, Sample* sample);
    void setParam(int slot, int param, float value);
    // Off -> waits for the grid; waiting or playing -> off. `idleClock`: nothing else keeps
    // time (transport stopped), so the first pad to start restarts the clock and plays at once.
    void toggle(int slot, bool idleClock);
    void stopAll();
    // Frames per beat at the current tempo (each block).
    void setBeatFrames(double frames) {
        if (frames > 1.0) beatFrames_ = frames;
    }
    // The sequencer has its beat number `beat` (counted from the transport start) `offset` frames
    // into the next render(): the clock snaps to it, bars and long loops in phase with the song.
    void syncBeat(int offset, int beat) {
        syncAt_ = offset;
        syncBeat_ = beat;
    }
    // Adds the slots' sound into the stereo pair.
    void render(float* left, float* right, int numFrames);

    State state(int slot) const { return static_cast<State>(states_[static_cast<size_t>(slot)].load(std::memory_order_relaxed)); }
    double beat() const { return beat_; }  // tests

    // Not the audio thread: frees samples released by the audio thread.
    void collectGarbage();

private:
    struct Voice {
        const Sample* sample = nullptr;
        int64_t pos = 0;
        int fade = 0;  // > 0: fading out over the remaining frames
    };
    struct Slot {
        Sample* sample = nullptr;
        int beats = 4;
        bool oneShot = false;
        float gain = 1.0f;
        State state = Off;
        double startBeat = 0.0;  // waiting: when it starts; playing: when the current pass began
        Voice voice, tail;
    };
    void retire(Sample* s);
    void restart(Slot& s);
    void align();
    void play(Voice& v, float gain, float& l, float& r);
    double gridOf(const Slot& s) const { return s.beats < 4 ? s.beats : 4.0; }

    std::array<Slot, kSlots> slots_{};
    std::array<std::atomic<uint8_t>, kSlots> states_{};
    SpscQueue<Sample*, 64> retired_;
    double beatFrames_ = 24000.0;
    double beat_ = 0.0;
    int syncAt_ = -1;
    int syncBeat_ = 0;
    int fadeFrames_ = 240;
};

}  // namespace ps
