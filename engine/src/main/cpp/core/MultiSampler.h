#pragma once

#include <algorithm>
#include <array>
#include <cstdint>

#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

// A multizone sampled instrument (phase 38: the downloadable grand pianos, strings, brass and
// woodwinds): up to kZones recordings, each one played over a range of keys and velocities from
// the note it was recorded at, with round robins (SFZ's seq_length / seq_position: zones of the
// same key and velocity take turns, in a fixed order) and release samples (SFZ's trigger=release:
// played when the key is let go, softer the longer it was held). Stereo zones play in stereo.
//
// The zones' recordings are Samples installed one by one (installZone; compact 16-bit ones are
// the rule: half the memory) and shared with the export engines. Every voice keeps its own
// reference to the recording it reads, so replacing a zone never cuts or frees a sound playing:
// the last reference goes to retired_ and collectGarbage() (not the audio thread) frees it.
// Real-time safe except collectGarbage(): never allocates, never locks.
class MultiSampler {
public:
    static constexpr int kZones = 512;
    static constexpr int kVoices = 32;
    // Slots past kVoices for the voices being cut (a voice stolen fades out over 2 ms there while
    // the new one starts: no click). At most kVoices voices play at full level.
    static constexpr int kSpareVoices = 8;
    // Zones that one key press may start together (layers of the same key and velocity).
    static constexpr int kLayersPerNote = 4;

    // The instrument's settings (SetTrackZone with zone -1). Order is part of the JNI contract
    // (MultiInstrument.engineParams in Kotlin): append only.
    enum Param : int {
        Count,         // zones in use (the ones from Count on are ignored): 0 = no instrument
        Release,       // seconds to -60 dB after the key is let go, 0.02..10
        Attack,        // seconds, 0..2 (a short fade-in at least: no click on a cut recording)
        VelTrack,      // 0..1: how much softer a soft note plays on top of its velocity layer (SFZ amp_veltrack)
        Gain,          // linear, 0..4: the instrument's calibration
        ReleaseDecay,  // dB per second held that a release sample loses (SFZ rt_decay), 0..60
        ClearZones,    // any value: every zone lets go of its recording (Count back to 0); notes ringing ring on
        kParamCount
    };
    // A zone's settings (SetTrackZone with the zone's index). Append only.
    enum ZoneParam : int {
        LoKey,        // 0..127
        HiKey,        // 0..127 (< LoKey: the zone is off)
        LoVel,        // 1..127
        HiVel,        // 1..127
        Root,         // MIDI note the recording sounds at (its tuning in the fraction)
        ZoneGain,     // dB, -60..24
        SeqPosition,  // 1..SeqLength: this zone's turn among its round robins; 0 = every time
        SeqLength,    // 1..16
        Trigger,      // 0 = the key press, 1 = the key let go (a release sample)
        LoopStart,    // frame (at the engine rate) the sustain loop goes back to
        LoopEnd,      // frame the loop wraps at (<= LoopStart: no loop, the recording plays once). The loop
                      // goes on through the release (SFZ loop_continuous): a held string chord never runs out
        kZoneParamCount
    };

    MultiSampler() = default;
    ~MultiSampler();
    MultiSampler(const MultiSampler&) = delete;
    MultiSampler& operator=(const MultiSampler&) = delete;

    void prepare(int sampleRate);
    // Takes over the caller's reference (nullptr = the zone has no recording).
    void installZone(int zone, Sample* sample);
    void setParam(int param, float value);
    void setZoneParam(int zone, int param, float value);
    void setBend(float semitones) { bend_ = semitones; }
    // The most voices it plays at once (1..kVoices; the engine shares kMultiVoices between the
    // tracks): past it a new note steals one. Voices already playing are never cut by it.
    void setVoiceLimit(int voices) { voiceLimit_ = std::clamp(voices, 1, kVoices); }

    // Playable: zones in use and at least one with its recording.
    bool active() const { return count_ > 0 && loaded_ > 0; }
    // Some zone in use is a stereo recording.
    bool stereo() const { return stereoZones_ > 0; }

    void noteOn(int note, float velocity, int offset);
    void noteOff(int note, int offset);
    // Every key let go (their release samples are not played); the round robins start over, so a
    // song plays its turns the same way every time it starts.
    void allNotesOff();
    // The round robins start over (the transport starts: a song plays its turns the same way
    // every time, whatever was played live while it was stopped).
    void resetRoundRobin();
    // Adds the voices into `left` and `right` (both the same length). `right` nullptr: a mono mix
    // into `left`.
    void render(float* left, float* right, int numFrames);

    // Voices sounding (fading out ones included: up to kVoices + kSpareVoices for 2 ms).
    int activeVoices() const;
    // Tests: the zone the last key press started first (-1: none).
    int lastZone() const { return lastZone_; }

    // Not the audio thread: frees the recordings the audio thread let go of.
    void collectGarbage();

private:
    struct Zone {
        Sample* sample = nullptr;
        int loKey = 0, hiKey = -1, loVel = 1, hiVel = 127;
        float root = 60.0f, gain = 1.0f;
        int seqPosition = 0, seqLength = 1;
        bool release = false;
        double loopStart = 0.0, loopEnd = 0.0;
        uint32_t turns = 0;  // how many key presses this zone matched (its round-robin counter)
    };
    struct Voice {
        Sample* sample = nullptr;  // its own reference, nullptr = free
        int note = -1;
        bool gate = false;
        double pos = 0.0;
        double rate = 1.0;
        double loopStart = 0.0, loopEnd = 0.0;  // loopEnd > loopStart: the voice loops
        float pitch = 0.0f;       // semitones from the zone's root, before the bend
        float ratePitch = 1e9f;   // the pitch `rate` was computed for
        float gain = 0.0f;
        float env = 0.0f;
        float attackStep = 1.0f;
        float releaseCoef = 1.0f;  // < 1 once the key is let go
        int fade = 0;              // > 0: being cut, fades out over the remaining frames
        uint32_t age = 0;
    };
    struct Event {
        int offset = 0;
        int note = 0;
        float velocity = 0.0f;
        bool on = false;
    };
    static constexpr int kFadeFrames = 96;  // 2 ms

    void queue(const Event& e);
    void apply(const Event& e);
    // Starts zone `z` for `note`; false when no voice could be had.
    bool start(int z, int note, float velocity, float extraGain);
    void freeVoice(Voice& v);
    Voice& slotForNewVoice();
    void recount();
    template <typename Reader>
    void renderVoice(Voice& v, const Reader& read, float* left, float* right, int from, int to);

    int sampleRate_ = 48000;
    std::array<Zone, kZones> zones_{};
    int count_ = 0;
    int loaded_ = 0;       // zones in use with a recording
    int stereoZones_ = 0;  // ... of them stereo
    float release_ = 0.5f, attack_ = 0.001f, velTrack_ = 0.5f, gain_ = 1.0f, releaseDecay_ = 0.0f;
    float bend_ = 0.0f;
    int voiceLimit_ = kVoices;
    std::array<Voice, kVoices + kSpareVoices> voices_{};
    std::array<Event, 256> events_{};  // room to release all 128 notes at once (sustain pedal let go)
    int eventCount_ = 0;
    // Per key: the velocity it was pressed with and the frame it was pressed on (release samples).
    std::array<float, 128> pressVelocity_{};
    std::array<int64_t, 128> pressFrame_{};
    int64_t blockStart_ = 0;  // frames rendered before this block
    uint32_t clock_ = 0;
    int lastZone_ = -1;
    SpscQueue<Sample*, 1024> retired_;
};

}  // namespace ps
