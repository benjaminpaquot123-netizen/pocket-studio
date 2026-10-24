#pragma once

#include <cstdint>

#include "SpscQueue.h"

struct tsf;

namespace ps {

// Plays one General MIDI instrument from a SoundFont (TinySoundFont, MIT) for the synth track.
// The tsf instance is created off the audio thread with its voices preallocated, so note-on,
// note-off and render never allocate. The audio thread hands a replaced instance back through
// a queue; collectGarbage() (not the audio thread) closes it.
class SoundFontPlayer {
public:
    static constexpr int kMaxVoices = 48;
    static constexpr float kBendRange = 12.0f;  // semitones of a full pitch wheel

    ~SoundFontPlayer();
    SoundFontPlayer() = default;
    SoundFontPlayer(const SoundFontPlayer&) = delete;
    SoundFontPlayer& operator=(const SoundFontPlayer&) = delete;

    // makeInstance, close and collectGarbage share TinySoundFont's non-atomic reference count:
    // call them from one thread only (the app's main thread).
    // Not the audio thread: a playable copy of `master` (shares its sample data), or nullptr.
    static tsf* makeInstance(tsf* master, int sampleRate);
    // Not the audio thread: loads a .sf2 file; nullptr if unreadable.
    static tsf* loadFile(const char* path);
    static void close(tsf* f);
    // Preset list of a loaded font (bank 128 = drum kits in General MIDI).
    static int presetCount(tsf* f);
    static void presetInfo(tsf* f, int index, int* bank, int* program, const char** name);
    // Same thread as makeInstance: renders one note of `preset` (held `holdFrames`) into `out`
    // (mono, `frames` long, at `sampleRate`); used to put a drum kit's sounds on the pads.
    static bool renderNote(tsf* master, int preset, int note, float velocity, int holdFrames, float* out, int frames,
                           int sampleRate);

    // Audio thread: takes ownership of `instance` (nullptr = back to the synth) playing `preset`.
    void install(tsf* instance, int preset);
    bool active() const { return instance_ != nullptr; }
    void noteOn(int note, float velocity, int frameOffset);
    void noteOff(int note, int frameOffset);
    void allNotesOff();
    // Pitch bend in semitones (-12..12), on the notes playing and the next ones; kept across installs.
    void setBend(float semitones);
    // Adds the instrument (mono) into `out`.
    void render(float* out, int numFrames) { render(out, nullptr, numFrames); }
    // With `right`: in stereo (each sound's pan in the SoundFont), its middle as loud as the mono.
    void render(float* left, float* right, int numFrames);

    void collectGarbage();

private:
    struct Event {
        int offset = 0;
        int note = 0;
        float velocity = 0.0f;  // 0 = off
    };
    void queue(const Event& e);
    void applyBend();

    void renderSpan(float* left, float* right, int frames);
    tsf* instance_ = nullptr;
    static constexpr int kStereoChunk = 256;
    float stereoBuf_[kStereoChunk * 2] = {};
    int preset_ = 0;
    float bend_ = 0.0f;
    Event events_[256];  // room to release all 128 notes at once (sustain pedal let go)
    int eventCount_ = 0;
    SpscQueue<tsf*, 16> retired_;
};

}  // namespace ps
