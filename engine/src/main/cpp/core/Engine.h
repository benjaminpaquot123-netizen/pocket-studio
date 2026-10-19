#pragma once

#include <array>
#include <atomic>
#include <cstdint>

#include "AcidTrack.h"
#include "DrumKit.h"
#include "Metronome.h"
#include "Mixer.h"
#include "NoteTrack.h"
#include "RoundTripMeter.h"
#include "Sampler.h"
#include "SoundFontPlayer.h"
#include "Sequencer.h"
#include "Synth.h"
#include "Smoother.h"
#include "StemPlayer.h"
#include "SpscQueue.h"

namespace ps {

enum class CommandType : uint8_t {
    SetPlaying,
    SetFrequency,
    SetGain,
    StartLatencyTest,
    CancelLatencyTest,
    // Values below are part of the JNI contract (NativeEngine.kt): append only.
    SetTransport,     // value: 1 = play, 0 = stop; a = count-in bars
    SetTempo,         // value: BPM
    SetSwing,         // value: 0..0.75 of a 16th
    SetMetronome,     // value: 1 = on
    SetPatternSteps,  // a: 16..64
    SetDrumHit,       // a: lane, b: step, value: velocity (0 = remove)
    TriggerPad,       // a: lane, value: velocity (live play, sounds immediately)
    SetAcidStep,      // a: step index, b: AcidStep::pack()
    SetAcidLength,    // a: 1..64
    SetAcidRoot,      // a: MIDI note of the root
    SetAcidParam,     // a: AcidTrack::Param, value: 0..1
    SynthNoteOn,      // a: MIDI note, value: velocity
    SynthNoteOff,     // a: MIDI note
    SynthParam,       // a: Synth::Param, value: 0..1
    SynthAllOff,
    SetChannelVolume,  // a: channel (Mixer), value: 0..1
    SetChannelPan,     // a: channel, value: -1..1
    SetChannelMute,    // a: channel, value: 1 = muted
    SetChannelSolo,    // a: channel, value: 1 = solo
    InstallSample,     // a: pad, sample: owned reference (nullptr = back to the drum sound)
    SetPadParam,       // a: pad, b: Sampler::Param, value in that parameter's unit
    SetSynthNote,      // a: index, b: NoteTrack::pack(start, length, pitch), value: velocity
    SetSynthNoteCount, // a: number of notes in use
    SetSynthSteps,     // a: piano-roll pattern length in steps (1..128)
    SetInstrument,     // font: owned instance (nullptr = back to the synth), a: preset index
    InstallStems,      // stems: owned reference (nullptr = no song)
    SetStemParam,      // a: stem, b: StemPlayer::Param, value
    SetStemStart,      // a: song frame where playback starts on step 0
};

struct Command {
    CommandType type = CommandType::SetPlaying;
    float value = 0.0f;
    int32_t a = 0;
    int32_t b = 0;
    // InstallSample only: a reference the engine takes over (released if the command is dropped).
    Sample* sample = nullptr;
    // SetInstrument only: an instance the engine takes over (closed if the command is dropped).
    tsf* font = nullptr;
    StemBundle* stems = nullptr;
};

// Platform-independent audio core. The platform layer (Oboe on Android,
// offline renderer in host tests) calls process() from the audio thread.
class Engine {
public:
    static constexpr float kMinFrequency = 20.0f;
    static constexpr float kMaxFrequency = 20000.0f;

    explicit Engine(int sampleRate = 48000);
    ~Engine();
    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;

    // Not real-time safe: call while no stream is running. Keeps frequency, volume and play state.
    void prepare(int sampleRate);

    // Producer side; callers must serialise (single producer).
    bool post(const Command& command) { return commands_.push(command); }

    // Real-time safe.
    void process(const float* input, int numInputFrames, int inputChannels,
                 float* output, int numFrames, int outputChannels);

    int sampleRate() const { return sampleRate_; }
    uint64_t callbackCount() const { return callbacks_.load(std::memory_order_relaxed); }
    uint64_t framesRendered() const { return frames_.load(std::memory_order_relaxed); }
    // Highest absolute sample since the last call.
    float takePeak() { return peak_.exchange(0.0f, std::memory_order_acq_rel); }
    bool isPlaying() const { return playing_.load(std::memory_order_relaxed); }
    bool hasNonFinite() const { return nonFinite_.load(std::memory_order_relaxed); }

    RoundTripMeter& latencyMeter() { return meter_; }

    // Not the audio thread (one caller at a time): frees samples the audio thread let go of.
    void collectGarbage() {
        sampler_.collectGarbage();
        instrument_.collectGarbage();
        stems_.collectGarbage();
    }
    bool stemsLoaded() const { return stems_.loaded(); }  // tests
    int64_t stemPosition() const { return stems_.position(); }
    bool instrumentActive() const { return instrument_.active(); }  // tests
    bool padHasSample(int pad) const { return sampler_.has(pad); }  // tests
    const Sampler::PadParams& padParams(int pad) const { return sampler_.params(pad); }  // tests

    static constexpr int kLanes = 16;
    static constexpr int kMaxSteps = 64;

    // Transport state for the UI: current pattern step (-1 when stopped or counting in).
    int currentStep() const { return currentStep_.load(std::memory_order_relaxed); }
    // Steps since play (no wrap), -1 when stopped: the piano roll has its own loop length.
    int absoluteStep() const { return absoluteStep_.load(std::memory_order_relaxed); }
    bool transportRunning() const { return sequencer_.running(); }
    // Increments every time a lane is triggered (pad lights, tests).
    uint32_t laneTriggerCount(int lane) const { return laneTriggers_[lane].load(std::memory_order_relaxed); }
    // Number of metronome clicks triggered since startup (tests).
    Mixer& mixer() { return mixer_; }
    int synthActiveVoices() const { return synth_.activeVoices(); }
    int notesPending() const { return notes_.pendingCount(); }  // tests
    uint32_t acidNotesPlayed() const { return acid_.notesPlayed(); }
    uint64_t metronomeClicks() const { return metronomeClicks_.load(std::memory_order_relaxed); }

private:
    static constexpr int kMaxBlock = 1024;

    void applyCommand(const Command& command);
    void renderBlock(float* output, int numFrames, int outputChannels);
    void onStep(int offset, int step);

    int sampleRate_ = 48000;
    double phase_ = 0.0;
    Smoother frequency_;
    Smoother gain_;
    float volume_ = 0.5f;
    SpscQueue<Command, 4096> commands_;
    RoundTripMeter meter_;

    Sequencer sequencer_;
    Metronome metronome_;
    DrumKit drums_;
    AcidTrack acid_;
    Synth synth_;
    NoteTrack notes_;
    SoundFontPlayer instrument_;
    // The synth track's sound: the instrument when one is loaded, else the subtractive synth.
    struct NoteRouter {
        Synth& synth;
        SoundFontPlayer& instrument;
        void noteOn(int n, float v, int at) { instrument.active() ? instrument.noteOn(n, v, at) : synth.noteOn(n, v, at); }
        void noteOff(int n, int at) { instrument.active() ? instrument.noteOff(n, at) : synth.noteOff(n, at); }
    };
    NoteRouter router_{synth_, instrument_};
    Sampler sampler_;
    StemPlayer stems_;
    Mixer mixer_;
    std::array<float, kMaxBlock> stemsL_{};
    std::array<float, kMaxBlock> stemsR_{};
    std::array<float, kMaxBlock> drumsBuf_{};
    std::array<float, kMaxBlock> acidBuf_{};
    std::array<float, kMaxBlock> synthBuf_{};
    bool metronomeOn_ = false;
    int patternSteps_ = 16;
    float pattern_[kLanes][kMaxSteps] = {};
    std::array<float, kMaxBlock> mix_{};
    std::atomic<int> currentStep_{-1};
    std::atomic<int> absoluteStep_{-1};
    std::array<std::atomic<uint32_t>, kLanes> laneTriggers_{};
    std::atomic<uint64_t> metronomeClicks_{0};

    std::atomic<bool> playing_{false};
    std::atomic<bool> nonFinite_{false};
    std::atomic<uint64_t> callbacks_{0};
    std::atomic<uint64_t> frames_{0};
    std::atomic<float> peak_{0.0f};
};

}  // namespace ps
