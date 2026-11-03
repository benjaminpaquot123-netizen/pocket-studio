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
// Pad Live (n° 25): four pages of 16 pads, A to D, all live at once (a loop of page A keeps
// playing while page B is shown): 64 slots; a slot with nothing sounding costs one test a frame.
// B2: each pad's pitch (its sound read faster or slower), reverse, attack and release; a whole
// row of pads launched together on the next bar line; the bar lines the clock crosses (where
// « Enregistrer ce que je joue » starts and ends).
class ClipLauncher {
public:
    static constexpr int kSlots = 64;
    static constexpr int kMaxBeats = 64;
    // Pitch: semitones -24..24 (the sound read faster or slower, its length with it); Reverse: > 0.5
    // plays it backwards; Attack, Release: seconds 0..2 and 0..4 (a pass fades in; a pad stopped fades out).
    enum Param { Beats = 0, OneShot = 1, Gain = 2, Pitch = 3, Reverse = 4, Attack = 5, Release = 6 };
    static constexpr float kMaxAttack = 2.0f;
    static constexpr float kMaxRelease = 4.0f;
    enum State : uint8_t { Off = 0, Waiting = 1, Playing = 2 };

    ClipLauncher() = default;
    ~ClipLauncher();
    ClipLauncher(const ClipLauncher&) = delete;
    ClipLauncher& operator=(const ClipLauncher&) = delete;

    void prepare(int sampleRate) {
        rate_ = std::max(1, sampleRate);
        fadeFrames_ = std::max(1, sampleRate / 200);
    }
    // Takes over the caller's reference (nullptr clears the slot). `refit`: the same sound at
    // another length (a loop stretched to a new tempo): a loop playing picks up at its place on the
    // beat clock in the new copy (the old one fades under it), instead of waiting for its next pass.
    void install(int slot, Sample* sample, bool refit = false);
    void setParam(int slot, int param, float value);
    // Off -> waits for the grid; waiting or playing -> off. `idleClock`: nothing else keeps
    // time (transport stopped), so the first pad to start restarts the clock and plays at once.
    // `velocity` (0, 1]: how loud the loop or hit it starts plays (a controller's pad); <= 0: full.
    void toggle(int slot, bool idleClock, float velocity = 1.0f);
    // A row of pads [first, first + count): its loops all playing (or waiting): they stop; else every
    // pad of it with a sound that is not playing (and every hit) starts on the next bar line, all
    // together (those already waiting move to it), the loops playing go on. `idleClock` as toggle().
    void launchRow(int first, int count, bool idleClock);
    void stopAll();
    // Time signature n/4: launches wait for at most a bar; long loops stay in phase over 16 bars.
    void setBeatsPerBar(int beats) { beatsPerBar_ = std::clamp(beats, 2, 7); }
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
    // The frame of the last render() where the clock crossed a bar line (-1: none), and whether a pad
    // was playing or waiting there (a recording that waits for the music starts on such a line).
    int barAt() const { return barAt_; }
    bool barSounding() const { return barSounding_; }

    // Not the audio thread: frees samples released by the audio thread.
    void collectGarbage();

private:
    struct Voice {
        const Sample* sample = nullptr;
        double pos = 0.0;  // frames of the sound read (from its end when the slot plays reversed)
        int fade = 0;  // > 0: fading out over the remaining frames (of fadeLen)
        int rise = 0;  // > 0: fading in over the remaining frames of riseLen (an attack, a refit picked up mid-loop)
        float level = 1.0f;  // the velocity of the tap that started it
        int fadeLen = 1;
        int riseLen = 1;
    };
    struct Slot {
        Sample* sample = nullptr;
        int beats = 4;
        bool oneShot = false;
        float gain = 1.0f;
        float velocity = 1.0f;  // of the tap that started it: each pass plays at it
        double speed = 1.0;     // frames of the sound per frame played (its pitch)
        bool reverse = false;
        int attack = 0;         // frames each pass fades in over
        int release = 0;        // frames a pad stopped fades out over (at least the click-free fade)
        State state = Off;
        double startBeat = 0.0;  // waiting: when it starts; playing: when the current pass began
        Voice voice;
        // Passes fading out: two, so that a stop just after a loop started over (its last pass
        // still fading under the new one) never cuts that one dead.
        std::array<Voice, 2> tails;
        bool tailOf(const Sample* x) const { return tails[0].sample == x || tails[1].sample == x; }
        // Nothing sounds nor waits: render() skips it.
        bool idle() const { return state == Off && !voice.sample && !tails[0].sample && !tails[1].sample; }
        // The sound replaced while it played: kept until its fading voice is done (no click).
        Sample* held = nullptr;
    };
    void retire(Sample* s);
    void releaseHeld(Slot& s);
    void restart(Slot& s);
    // The voice playing fades out on a free tail (the quietest one if both still sound), over
    // `frames` (0: the short click-free fade).
    void fadeAway(Slot& s, int frames = 0);
    // A pad stopped by a tap (or « Tout couper », or a row): its release.
    void stopSlot(Slot& s);
    void align();
    void play(Voice& v, const Slot& s, float& l, float& r);
    double gridOf(const Slot& s) const { return s.beats < beatsPerBar_ ? s.beats : beatsPerBar_; }

    std::array<Slot, kSlots> slots_{};
    std::array<std::atomic<uint8_t>, kSlots> states_{};
    SpscQueue<Sample*, 64> retired_;
    double beatFrames_ = 24000.0;
    double beat_ = 0.0;
    int syncAt_ = -1;
    int syncBeat_ = 0;
    int beatsPerBar_ = 4;
    int fadeFrames_ = 240;
    int rate_ = 48000;
    int64_t lastBar_ = INT64_MIN;
    int barAt_ = -1;
    bool barSounding_ = false;
};

}  // namespace ps
