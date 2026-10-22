#pragma once

#include <array>
#include <cmath>
#include <cstdint>

#include "Control/adsr.h"
#include "Filters/ladder.h"
#include "Filters/svf.h"
#include "Synthesis/oscillator.h"

namespace ps {

// 8-voice subtractive synthesizer: 2 oscillators + sub + noise -> ladder or state-variable
// filter, filter and amp ADSR, 2 LFOs (vibrato, filter sweep). Building blocks from DaisySP.
// All voices are preallocated: noteOn/noteOff/setParam/render are real-time safe.
class Synth {
public:
    static constexpr int kVoices = 8;
    /** Decibels of the Gain parameter at 0 and 1 (0.5 = unity), shared with PlaitsSynth and the model. */
    static constexpr float kGainRangeDb = 30.0f;
    /** The linear gain of a Gain parameter value (0.5 = 1). */
    static float gainOf(float value) { return std::pow(10.0f, (value - 0.5f) * 2.0f * kGainRangeDb / 20.0f); }

    enum Param : int {
        Osc1Wave = 0, Osc2Wave, Osc2Detune, Osc2Octave, OscMix, SubLevel, NoiseLevel,
        FilterType, Cutoff, Resonance, FilterEnvAmount,
        FilterAttack, FilterDecay, FilterSustain, FilterRelease,
        AmpAttack, AmpDecay, AmpSustain, AmpRelease,
        Lfo1Rate, Lfo1ToPitch, Lfo2Rate, Lfo2ToCutoff, Level,
        // Appended (the JNI contract and saved projects keep the order above).
        Unison,        // 0 = oscillator 1 alone, up to 1 = six copies with it (seven voices: supersaw, Reese)
        UnisonDetune,  // 0..1 = spread of the voices, ±3 .. ±50 cents, centred on the note
        Lfo2Sync,      // 0 = the sweep runs at Lfo2Rate; above, locked to the tempo (see syncCyclesPerBeat), restarted on each attack
        Glide,         // 0 = none; above, portamento up to 0.5 s: a note starting while another is held slides from it (one voice)
        Gain,          // calibration of the sound's loudness: 0.5 = 0 dB, 0..1 = -30..+30 dB (see kGainRangeDb)
        ParamCount
    };

    void prepare(int sampleRate);  // not real-time safe the first time (filter setup)
    void setParam(Param param, float value);  // value 0..1
    float param(Param param) const { return params_[static_cast<size_t>(param)]; }

    void noteOn(int midiNote, float velocity, int frameOffset = 0);
    void noteOff(int midiNote, int frameOffset = 0);
    void allNotesOff();
    // Live expression: pitch bend in semitones, mod wheel 0..1 (more vibrato).
    void setBend(float semitones) { bend_ = semitones; }
    void setMod(float amount) { mod_ = amount; }
    // The length of a beat in frames, from the sequencer, every block: the tempo-locked sweep follows it.
    void setBeatFrames(double frames);
    // Sweep cycles per beat (Lfo2Sync's divisions): free, 1 bar, 1/2, 1/4, 1/8, 1/16, 1/4 triplet, 1/8 triplet.
    static constexpr int kSyncDivisions = 8;
    static float syncCyclesPerBeat(float sync);
    // The sweep's rate in Hz as set now (free or locked), for tests.
    float sweepHz() const { return sweepHz_; }
    void render(float* mono, int numFrames);
    // The transport starts: both LFOs and the noise from their start, as in an export.
    void restartLfos() {
        lfo1_.Reset(0.0f);
        lfo2_.Reset(0.0f);
        noiseState_ = kNoiseSeed;
    }

    int activeVoices() const;

private:
    struct Voice {
        daisysp::Oscillator osc1, osc2, sub;
        std::array<daisysp::Oscillator, 6> unison;  // detuned copies of oscillator 1
        daisysp::LadderFilter ladder;
        daisysp::Svf svf;
        daisysp::Adsr ampEnv, filterEnv;
        int note = -1;
        float velocity = 0.0f;
        bool gate = false;
        uint32_t age = 0;
        float baseHz = 440.0f;
        float level = 0.0f;  // last amplitude envelope value (voice stealing takes the quietest released one)
        float semis = 0.0f;      // pitch now (MIDI note, fractional while gliding)
        float target = 0.0f;     // where the glide goes
        float glideStep = 0.0f;  // semitones per control tick, 0 = not gliding
        bool fresh = false;      // a new note: its pitch and cutoff are set on its first frame (not the next tick)
    };

    struct Event {
        int offset = -1;
        int note = 0;
        float velocity = 0.0f;
        bool on = false;
    };

    void applyEvent(const Event& e);
    void queue(const Event& e);
    void updateVoiceSettings(Voice& v);
    void updateSweepRate();
    float noise();

    int sampleRate_ = 48000;
    bool prepared_ = false;
    std::array<Voice, kVoices> voices_{};
    std::array<float, ParamCount> params_{};
    std::array<Event, 256> events_{};  // room to release all 128 notes at once (sustain pedal let go)
    int eventCount_ = 0;
    daisysp::Oscillator lfo1_, lfo2_;
    uint32_t clock_ = 0;
    float bend_ = 0.0f, mod_ = 0.0f;
    float unisonGain_ = 1.0f;  // follows the stack gain smoothly when the number of copies changes
    // Frames to the next control tick: counted across calls, so the ticks fall on the same frames
    // whatever the callback's size (an export in blocks of 512 sounds like the phone's 192).
    int controlCountdown_ = 0;
    static constexpr uint32_t kNoiseSeed = 1234567u;
    uint32_t noiseState_ = kNoiseSeed;
    double beatFrames_ = 24000.0;  // 120 BPM at 48 kHz until the sequencer says otherwise
    float sweepHz_ = 0.0f;
};

}  // namespace ps
