#pragma once

#include <algorithm>
#include <array>
#include <cstdint>

#include "NoteTrack.h"
#include "PlaitsSynth.h"
#include "SoundFontPlayer.h"
#include "Synth.h"

namespace ps {

// One instrument track: its piano-roll pattern and its sound (a General MIDI instrument when one
// is loaded, else Plaits or the subtractive synth). Everything is preallocated: real-time safe.
struct InstrumentTrack {
    Synth synth;
    SoundFontPlayer instrument;
    PlaitsSynth plaits;
    NoteTrack notes;
    bool usePlaits = false;
    // Switched off tracks are neither rendered nor mixed (track 0 is always on).
    bool on = false;

    // The waiting scene's notes for this track, copied over `notes` at the swap.
    struct Staged {
        std::array<int32_t, NoteTrack::kMaxNotes> packed{};
        std::array<float, NoteTrack::kMaxNotes> velocity{};
        int count = 0;
        int steps = 16;
    } staged;

    void prepare(int sampleRate) { synth.prepare(sampleRate); plaits.prepare(sampleRate); }

    void noteOn(int n, float v, int at) {
        if (instrument.active()) instrument.noteOn(n, v, at);
        else if (usePlaits) plaits.noteOn(n, v, at);
        else synth.noteOn(n, v, at);
    }
    void noteOff(int n, int at) {
        // Note-offs go everywhere: a source switched while a key was held must not hang.
        instrument.noteOff(n, at);
        plaits.noteOff(n, at);
        synth.noteOff(n, at);
    }
    void allNotesOff() {
        synth.allNotesOff();
        instrument.allNotesOff();
        plaits.allNotesOff();
    }
    void setUsePlaits(bool p) {
        if (p == usePlaits) return;
        synth.allNotesOff();
        plaits.allNotesOff();
        usePlaits = p;
    }
    void swapIn() {
        for (int i = 0; i < staged.count; ++i) notes.setNote(i, staged.packed[static_cast<size_t>(i)], staged.velocity[static_cast<size_t>(i)]);
        notes.setCount(staged.count);
        notes.setSteps(staged.steps);
    }
    void render(float* out, int numFrames) {
        synth.render(out, numFrames);
        instrument.render(out, numFrames);
        plaits.render(out, numFrames);
    }
};

}  // namespace ps
