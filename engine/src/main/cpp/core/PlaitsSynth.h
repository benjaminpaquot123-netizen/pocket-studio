#pragma once

#include <array>
#include <memory>

namespace ps {

// Six voices of Mutable Instruments Plaits (MIT): 24 synthesis models from virtual analog to
// DX7-style six-operator FM, strings, modal, speech and drums. Rendered in fixed 12-sample blocks
// (Plaits' own rhythm), so a note starts within 0.25 ms of its step, always 1.25 ms late (constant).
// Real-time safe: everything is allocated by the constructor.
class PlaitsSynth {
public:
    enum Param { Model = 0, Harmonics = 1, Timbre = 2, Morph = 3, Decay = 4, Colour = 5, Release = 6, Level = 7,
        // Appended: the sound's loudness calibration, as Synth::Gain (0.5 = 0 dB).
        Gain = 8, ParamCount = 9 };
    static constexpr int kVoices = 6;
    static constexpr int kModels = 24;

    PlaitsSynth();
    ~PlaitsSynth();
    PlaitsSynth(const PlaitsSynth&) = delete;
    PlaitsSynth& operator=(const PlaitsSynth&) = delete;

    void prepare(int sampleRate);
    void setParam(Param p, float value);  // 0..1 (Model: index / (kModels - 1))
    // Live expression: pitch bend in semitones, mod wheel 0..1 (opens the timbre).
    void setBend(float semitones);
    void setMod(float amount);
    void noteOn(int note, float velocity, int offset);
    void noteOff(int note, int offset);
    void allNotesOff();
    // Adds the voices into `mono`.
    void render(float* mono, int numFrames);
    int activeVoices() const;
    // The transport starts: Plaits' noise from its first value (an export starts there too).
    void restartNoise();
    // Leads chantier (sound version 3): the six-operator FM models (2, 3, 4: the DX7 banks) play
    // louder the harder a note is struck, 0.3 + 0.7 x velocity as the subtractive synth does. Those
    // models carry their own envelopes, so the velocity otherwise reaches them only as accent
    // (0.2 dB between 0.4 and 1.0). Off (as before, sample for sample) by default.
    void setVelocityLevel(bool on);
    static bool velocityShapesLevel(int model) { return model >= 2 && model <= 4; }

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace ps
