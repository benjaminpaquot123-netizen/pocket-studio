#pragma once

#include <array>
#include <cstdint>
#include <memory>

namespace ps {

class AcidVoice;

// One step of an acid bass line, the way the classic machines store it.
struct AcidStep {
    int note = 0;       // 0..12 semitones above the root
    int octave = 0;     // -1..+2
    bool accent = false;
    bool slide = false;  // glide into the next step (which then sounds legato)
    bool gate = false;   // false = rest

    // Packed form used by engine commands: note | octave+1 << 4 | accent << 6 | slide << 7 | gate << 8.
    int32_t pack() const;
    static AcidStep unpack(int32_t bits);
};

// Monophonic acid bass: Open303 (Robin Schmidt, MIT) driven step by step by the engine's
// sequencer, followed by a saturation stage. Real-time safe after prepare().
class AcidTrack {
public:
    static constexpr int kMaxSteps = 128;  // 8 bars of sixteenths

    enum Param : int { Cutoff = 0, Resonance, EnvMod, Decay, Accent, Tuning, Waveform, Drive, Level, ParamCount };

    AcidTrack();
    ~AcidTrack();

    void prepare(int sampleRate);  // not real-time safe (builds wavetables); keeps settings

    void setStep(int index, AcidStep step);
    void setLength(int steps);
    void setRoot(int midiNote);
    // value: 0..1 for every parameter (mapped to musical ranges inside).
    void setParam(Param param, float value);
    // Automation: `param` takes `value` at `frameOffset` of the coming block (with the step's note,
    // whatever the block size). Real-time safe.
    void automate(Param param, float value, int frameOffset);

    // Called by the sequencer for each 16th step at `frameOffset` in the coming block.
    void onStep(int frameOffset, int step, double samplesPerStep);
    void stop();
    // Adds the bass into a mono buffer.
    void render(float* mono, int numFrames);

    // Number of notes started since startup (tests, UI).
    uint32_t notesPlayed() const { return notesPlayed_; }

private:
    struct PendingEvent {
        int offset = -1;
        int key = 0;
        bool accent = false;
        bool slide = false;
        int releaseAfter = -1;  // frames after the event, -1 = held (slide into next)
    };

    std::unique_ptr<AcidVoice> voice_;
    int sampleRate_ = 0;
    std::array<AcidStep, kMaxSteps> steps_{};
    int length_ = 16;
    int root_ = 36;
    float drive_ = 0.3f;
    float driveGain_ = 1.0f;  // from drive_: input gain of the saturation and its loudness makeup
    float makeup_ = 1.0f;
    // Automated settings waiting for their frame (negative = none).
    std::array<float, ParamCount> automated_{};
    int automatedAt_ = -1;
    void applyAutomated();
    float level_ = 0.7f;
    bool previousSlide_ = false;
    PendingEvent pending_;
    int releaseCountdown_ = -1;
    bool sounding_ = false;
    uint32_t notesPlayed_ = 0;
};

}  // namespace ps
