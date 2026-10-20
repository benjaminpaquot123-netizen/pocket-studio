#pragma once

#include <array>
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

    enum Param : int {
        Osc1Wave = 0, Osc2Wave, Osc2Detune, Osc2Octave, OscMix, SubLevel, NoiseLevel,
        FilterType, Cutoff, Resonance, FilterEnvAmount,
        FilterAttack, FilterDecay, FilterSustain, FilterRelease,
        AmpAttack, AmpDecay, AmpSustain, AmpRelease,
        Lfo1Rate, Lfo1ToPitch, Lfo2Rate, Lfo2ToCutoff, Level,
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
    void render(float* mono, int numFrames);

    int activeVoices() const;

private:
    struct Voice {
        daisysp::Oscillator osc1, osc2, sub;
        daisysp::LadderFilter ladder;
        daisysp::Svf svf;
        daisysp::Adsr ampEnv, filterEnv;
        int note = -1;
        float velocity = 0.0f;
        bool gate = false;
        uint32_t age = 0;
        float baseHz = 440.0f;
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
    float noise();

    int sampleRate_ = 48000;
    bool prepared_ = false;
    std::array<Voice, kVoices> voices_{};
    std::array<float, ParamCount> params_{};
    std::array<Event, 64> events_{};
    int eventCount_ = 0;
    daisysp::Oscillator lfo1_, lfo2_;
    uint32_t clock_ = 0;
    float bend_ = 0.0f, mod_ = 0.0f;
    uint32_t noiseState_ = 1234567u;
};

}  // namespace ps
