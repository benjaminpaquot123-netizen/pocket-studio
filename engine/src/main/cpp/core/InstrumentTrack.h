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

    // Sequenced or live, the note sounds (a General MIDI instrument wins, then Plaits, then the synth).
    void noteOn(int n, float v, int at) {
        if (n >= 0 && n < 128) sustained[static_cast<size_t>(n)] = false;
        if (instrument.active()) instrument.noteOn(n, v, at);
        else if (usePlaits) plaits.noteOn(n, v, at);
        else synth.noteOn(n, v, at);
    }
    // Held by the sustain pedal until it is released.
    void noteOff(int n, int at) {
        if (sustain && n >= 0 && n < 128) {
            sustained[static_cast<size_t>(n)] = true;
            return;
        }
        soundOff(n, at);
    }
    void allNotesOff() {
        synth.allNotesOff();
        instrument.allNotesOff();
        plaits.allNotesOff();
        sustained.fill(false);
        arp.count = 0;
        arp.sounding = -1;
    }
    void setUsePlaits(bool p) {
        if (p == usePlaits) return;
        synth.allNotesOff();
        plaits.allNotesOff();
        usePlaits = p;
    }

    // --- Live playing: expression, sustain pedal and arpeggiator --------------------------------

    enum Expression { Bend = 0, Mod = 1, Sustain = 2, ArpOn = 3, ArpRate = 4, ArpMode = 5, ArpOctaves = 6, ArpGate = 7 };
    enum ArpModes { Up = 0, Down = 1, UpDown = 2, Random = 3 };

    // A key from the keyboard or MIDI: into the arpeggiator when it is on.
    void liveOn(int n, float v) {
        if (!arp.on) {
            noteOn(n, v, 0);
            return;
        }
        if (arp.count >= static_cast<int>(arp.held.size())) return;
        for (int i = 0; i < arp.count; ++i) if (arp.held[static_cast<size_t>(i)] == n) return;
        // Kept in pitch order.
        int at = arp.count;
        while (at > 0 && arp.held[static_cast<size_t>(at - 1)] > n) {
            arp.held[static_cast<size_t>(at)] = arp.held[static_cast<size_t>(at - 1)];
            --at;
        }
        arp.held[static_cast<size_t>(at)] = n;
        arp.velocity = v;
        if (arp.count++ == 0) {
            arp.next = 0.0;  // the first key starts at once
            arp.index = 0;
        }
    }
    void liveOff(int n) {
        if (arp.on) {
            for (int i = 0; i < arp.count; ++i) {
                if (arp.held[static_cast<size_t>(i)] != n) continue;
                for (int k = i; k + 1 < arp.count; ++k) arp.held[static_cast<size_t>(k)] = arp.held[static_cast<size_t>(k + 1)];
                --arp.count;
                return;
            }
        }
        // Not an arpeggiated key (or held since before the arpeggiator came on): it sounds, release it.
        noteOff(n, 0);
    }
    void setExpression(int kind, float value) {
        switch (kind) {
            case Bend: synth.setBend(value); plaits.setBend(value); break;
            case Mod: synth.setMod(value); plaits.setMod(value); break;
            case Sustain:
                sustain = value > 0.5f;
                if (!sustain) {
                    for (int n = 0; n < 128; ++n) {
                        if (!sustained[static_cast<size_t>(n)]) continue;
                        sustained[static_cast<size_t>(n)] = false;
                        soundOff(n, 0);
                    }
                }
                break;
            case ArpOn:
                if ((value > 0.5f) != arp.on) {
                    arp.on = value > 0.5f;
                    if (arp.sounding >= 0) soundOff(arp.sounding, 0);
                    arp.sounding = -1;
                    arp.count = 0;
                }
                break;
            case ArpRate: arp.steps = std::clamp(value, 0.5f, 4.0f); break;
            case ArpMode: arp.mode = std::clamp(static_cast<int>(value), 0, 3); break;
            case ArpOctaves: arp.octaves = std::clamp(static_cast<int>(value), 1, 4); break;
            case ArpGate: arp.gate = std::clamp(value, 0.05f, 1.0f); break;
            default: break;
        }
    }
    // A sequencer step (transport running): the arpeggio lines up with the grid.
    void arpSync(int offset, int step, double stepFrames) {
        if (!arp.on || arp.count == 0) return;
        const int half = static_cast<int>(arp.steps * 2.0f + 0.5f);  // rate in 32nds
        if (half > 1 && (step * 2) % half != 0) return;
        // Just fired (on the edge of the previous block): already in time, no second note.
        if (arp.next - offset > 0.5 * stepFrames * arp.steps) return;
        arp.next = offset;
    }
    // Plays the arpeggio for this block (before rendering), `stepFrames` = a sixteenth.
    void arpAdvance(int numFrames, double stepFrames) {
        if (!arp.on) return;
        if (arp.offIn >= 0.0 && arp.offIn < numFrames && arp.sounding >= 0) {
            soundOff(arp.sounding, static_cast<int>(arp.offIn));
            arp.sounding = -1;
            arp.offIn = -1.0;
        }
        if (arp.count == 0) {
            if (arp.sounding >= 0) soundOff(arp.sounding, 0);
            arp.sounding = -1;
            arp.offIn = -1.0;
            return;
        }
        const double period = std::max(32.0, stepFrames * arp.steps);
        while (arp.next < numFrames) {
            const int at = std::max(0, static_cast<int>(arp.next));
            if (arp.sounding >= 0) soundOff(arp.sounding, at);
            const int n = arpNote();
            noteOn(n, arp.velocity, at);
            arp.sounding = n;
            arp.offIn = arp.next + period * arp.gate;
            if (arp.offIn < numFrames) {
                soundOff(n, static_cast<int>(arp.offIn));
                arp.sounding = -1;
                arp.offIn = -1.0;
            }
            arp.next += period;
        }
        arp.next -= numFrames;
        if (arp.offIn >= 0.0) arp.offIn -= numFrames;
    }

    void swapIn() {
        for (int i = 0; i < staged.count; ++i) notes.setNote(i, staged.packed[static_cast<size_t>(i)], staged.velocity[static_cast<size_t>(i)]);
        notes.setCount(staged.count);
        notes.setSteps(staged.steps);
    }
    struct Arp {
        bool on = false;
        float steps = 1.0f;  // sixteenths per note (0.5 = 1/32 .. 4 = 1/4)
        int mode = Up, octaves = 1;
        float gate = 0.5f, velocity = 0.8f;
        std::array<int, 16> held{};
        int count = 0, index = 0, sounding = -1;
        double next = 0.0, offIn = -1.0;
        uint32_t rng = 12345u;
    } arp;
    bool sustain = false;
    std::array<bool, 128> sustained{};

    void render(float* out, int numFrames) {
        synth.render(out, numFrames);
        instrument.render(out, numFrames);
        plaits.render(out, numFrames);
    }

private:
    void soundOff(int n, int at) {
        // Note-offs go everywhere: a source switched while a key was held must not hang.
        instrument.noteOff(n, at);
        plaits.noteOff(n, at);
        synth.noteOff(n, at);
    }
    int arpNote() {
        const int span = arp.count * arp.octaves;
        int i = 0;
        switch (arp.mode) {
            case Down: i = span - 1 - (arp.index % span); break;
            case UpDown: {
                const int cycle = span > 1 ? 2 * span - 2 : 1;
                const int k = arp.index % cycle;
                i = k < span ? k : cycle - k;
                break;
            }
            case Random:
                arp.rng = arp.rng * 1664525u + 1013904223u;
                i = static_cast<int>((arp.rng >> 8) % static_cast<uint32_t>(span));
                break;
            default: i = arp.index % span; break;
        }
        ++arp.index;
        return std::min(127, arp.held[static_cast<size_t>(i % arp.count)] + 12 * (i / arp.count));
    }
};

}  // namespace ps
