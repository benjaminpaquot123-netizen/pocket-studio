#pragma once

#include <array>
#include <cstdint>

#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

// A sound played across the keyboard: each note replays the sample faster or slower from its
// root note (a pack's 808, bass or stab, or any imported sound). Eight voices, or a single
// gliding voice when Glide is set (808 slides: a key pressed while another is held bends to it).
// Real-time safe except where noted: never allocates or frees.
class SampleInstrument {
public:
    static constexpr int kPolyphony = 8;

    // Order is part of the JNI contract (SampleParam in Kotlin): append only.
    enum Param : int {
        Root,     // MIDI note the sample sounds at, 0..127 (fractions allowed: a slightly sharp recording)
        Start,    // 0..1 of the sound
        End,      // 0..1 of the sound (> Start)
        Attack,   // seconds, 0..2
        Decay,    // seconds, 0..2
        Sustain,  // 0..1 level after the decay
        Release,  // seconds, 0..4 after the key is let go
        Glide,    // seconds, 0..1; > 0 = one voice that slides between held notes
        Loop,     // 1 = the region repeats while the key is held
        Gain,     // 0..2
        kParamCount
    };

    struct Params {
        float root = 60.0f, start = 0.0f, end = 1.0f;
        float attack = 0.0f, decay = 0.0f, sustain = 1.0f, release = 0.05f, glide = 0.0f;
        bool loop = false;
        float gain = 1.0f;
    };

    SampleInstrument() = default;
    ~SampleInstrument();
    SampleInstrument(const SampleInstrument&) = delete;
    SampleInstrument& operator=(const SampleInstrument&) = delete;

    void prepare(int sampleRate);
    // Takes over the caller's reference (nullptr = no sound). The previous sample is released;
    // if that was its last reference it waits for collectGarbage().
    void install(Sample* sample);
    bool active() const { return sample_ != nullptr; }
    void setParam(int param, float value);
    const Params& params() const { return params_; }
    void setBend(float semitones) { bend_ = semitones; }

    void noteOn(int note, float velocity, int offset);
    void noteOff(int note, int offset);
    void allNotesOff();
    // Adds the voices into `mono` (a stereo sound is folded to mono, like the other track sources).
    void render(float* mono, int numFrames);
    int activeVoices() const;

    // Not the audio thread: frees samples released by the audio thread.
    void collectGarbage();

private:
    enum Stage { Off, Attack_, Decay_, Held, Releasing };
    struct Voice {
        Stage stage = Off;
        int note = -1;
        bool gate = false;
        double pos = 0.0;
        float semis = 0.0f;       // current pitch (MIDI note), moves towards target while gliding
        float target = 0.0f;
        float glideStep = 0.0f;   // semitones per frame, 0 = not gliding
        float velocity = 0.0f;
        float env = 0.0f;
        float releaseStep = 0.0f;
        float ratePitch = 1e9f;   // the pitch [rate] was computed for (recomputed only when it moves)
        double rate = 1.0;
        int fade = 0;             // > 0: being cut, fades over the remaining frames
        uint32_t age = 0;
    };
    struct Event {
        int offset = 0;
        int note = 0;
        float velocity = 0.0f;
        bool on = false;
    };
    static constexpr int kSlots = kPolyphony + 4;  // room for the voices fading out after a cut
    static constexpr int kFadeFrames = 96;         // 2 ms

    void queue(const Event& e);
    void apply(const Event& e);
    void start(Voice& v, int note, float velocity);
    void release(Voice& v);
    Voice* freeSlot();
    float renderVoice(Voice& v, double lo, double hi);

    int sampleRate_ = 48000;
    Sample* sample_ = nullptr;
    Params params_;
    float bend_ = 0.0f;
    std::array<Voice, kSlots> voices_{};
    std::array<Event, 64> events_{};
    int eventCount_ = 0;
    // Glide mode: the keys held, most recent last (letting go of the top one slides back).
    std::array<int, 16> held_{};
    int heldCount_ = 0;
    uint32_t clock_ = 0;
    SpscQueue<Sample*, 16> retired_;
};

}  // namespace ps
