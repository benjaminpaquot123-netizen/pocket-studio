#pragma once

#include <array>
#include <atomic>
#include <cstdint>

#include "AcidTrack.h"
#include "ClipLauncher.h"
#include "DrumKit.h"
#include "Metronome.h"
#include "Mixer.h"
#include "NoteTrack.h"
#include "PlaitsSynth.h"
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
    InstallStems,      // stems: owned reference (nullptr = no song); a = 1: the preview player
    SetStemParam,      // a: stem, b: StemPlayer::Param, value
    SetStemStart,      // a: song frame where playback starts on step 0
    SetInsertType,     // a: channel * Mixer::kInserts + slot (channel kMaster = master), b: InsertSlot::Type
    SetInsertParam,    // a: as above, b: parameter 0..3, value 0..1
    SetSend,           // a: channel, b: bus (0 reverb, 1 delay), value 0..1
    SetBusParam,       // a: bus, b: parameter 0..3, value 0..1
    SetAutomationTarget,  // a: lane, b: Automation::pack(kind, channel, slot, param), or -1 = lane off
    SetAutomationPoint,   // a: lane, b: step, value 0..1 (negative = no point on that step)
    SetStaging,           // value 1: the next pattern commands (drums, acid, piano roll) fill the
                          // waiting scene instead of the playing one; 0: back to the playing one
    ArmSceneSwap,         // a > 0: the waiting scene replaces the playing one after `a` steps of the
                          // current scene; a = 0: at the next bar; a < 0: cancel
    SetPerformance,       // a: PerformanceFx::Param, value 0..1 (live XY pad and hold buttons)
    SetSynthEngine,       // a: 0 = subtractive synth, 1 = Plaits (a General MIDI instrument still wins)
    SetPlaitsParam,       // a: PlaitsSynth::Param, value 0..1
    InstallClip,          // a: live pad, sample: owned reference (nullptr = empty pad)
    SetClipParam,         // a: live pad, b: ClipLauncher::Param, value
    ToggleClip,           // a: live pad (switches it on at the next grid line, or off); a < 0: all off
    PlayPreview,          // value 1: the preview stems (InstallStems with a = 1) play from the start; 0: stop
    SetPreviewParam,      // a: stem, b: StemPlayer::Param, value
    SetMetronomeLevel,    // value 0..1 (0.5 = default)
    SetDrumRepeat,        // a: lane, b: step, value: hits in that step (1 = plain, 2..4 = a roll)
};

// Step automation of mixer settings, played on the audio thread at each sequencer step.
struct Automation {
    enum Kind { Volume = 0, Pan = 1, Send = 2, InsertParam = 3, BusParam = 4 };
    static constexpr int kLanes = 8;
    static constexpr int kSteps = 64;
    static int32_t pack(int kind, int ch, int slot, int param) { return (kind << 16) | (ch << 12) | (slot << 8) | param; }
    struct Lane {
        int32_t target = -1;
        std::array<float, kSteps> values{};
    };
    Lane lanes[kLanes];
    Automation() {
        for (auto& l : lanes) l.values.fill(-1.0f);
    }
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
        preview_.collectGarbage();
        clips_.collectGarbage();
    }
    bool stemsLoaded() const { return stems_.loaded(); }  // tests
    int64_t stemPosition() const { return stems_.position(); }
    // Song position of the preview player in frames, -1 when stopped.
    int64_t previewPosition() const { return preview_.position(); }
    ClipLauncher::State clipState(int pad) const { return clips_.state(pad); }
    double clipBeat() const { return clips_.beat(); }  // tests
    bool instrumentActive() const { return instrument_.active(); }  // tests
    bool padHasSample(int pad) const { return sampler_.has(pad); }  // tests
    const Sampler::PadParams& padParams(int pad) const { return sampler_.params(pad); }  // tests

    static constexpr int kLanes = 16;
    static constexpr int kMaxSteps = 64;

    // Transport state for the UI: current pattern step (-1 when stopped or counting in).
    int currentStep() const { return currentStep_.load(std::memory_order_relaxed); }
    // Steps since play (no wrap), -1 when stopped: the piano roll has its own loop length.
    int absoluteStep() const { return absoluteStep_.load(std::memory_order_relaxed); }
    // Where the playing scene is, in sixteenths with the fraction (live recording), -1 when stopped
    // or counting in. Updated once per block.
    double playPosition() const { return playPosition_.load(std::memory_order_relaxed); }
    // How many times a waiting scene took over (the UI follows the playing scene with it).
    uint32_t sceneSwaps() const { return sceneSwaps_.load(std::memory_order_relaxed); }
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
    PlaitsSynth plaits_;
    struct NoteRouter {
        Synth& synth;
        SoundFontPlayer& instrument;
        PlaitsSynth& plaits;
        bool usePlaits = false;
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
    };
    NoteRouter router_{synth_, instrument_, plaits_};
    Sampler sampler_;
    StemPlayer stems_;
    // The remix screen's own listening of a song's stems, free of the transport.
    StemPlayer preview_;
    ClipLauncher clips_;
    Automation automation_;
    void applyAutomation(int patternStep);

    // The waiting scene (filled while staging_), copied over the playing patterns at the swap.
    struct StagedScene {
        float drums[kLanes][kMaxSteps] = {};
        uint8_t repeats[kLanes][kMaxSteps] = {};
        int drumSteps = 16;
        std::array<AcidStep, AcidTrack::kMaxSteps> acid{};
        int acidLength = 16;
        std::array<int32_t, NoteTrack::kMaxNotes> notePacked{};
        std::array<float, NoteTrack::kMaxNotes> noteVelocity{};
        int noteCount = 0;
        int noteSteps = 16;
    };
    StagedScene staged_;
    bool staging_ = false;
    int swapAt_ = -1;      // local step of the swap; 0 = next bar; -1 = none
    int sceneStart_ = 0;   // absolute step where the playing scene began
    std::atomic<uint32_t> sceneSwaps_{0};
    void swapScene(int step);
    Mixer mixer_;
    std::array<float, kMaxBlock> stemsL_{};
    std::array<float, kMaxBlock> stemsR_{};
    std::array<float, kMaxBlock> drumsBuf_{};
    std::array<float, kMaxBlock> acidBuf_{};
    std::array<float, kMaxBlock> synthBuf_{};
    bool metronomeOn_ = false;
    int patternSteps_ = 16;
    float pattern_[kLanes][kMaxSteps] = {};
    uint8_t repeats_[kLanes][kMaxSteps] = {};  // 0 or 1 = one hit
    // Extra hits of rolls still to come: frames from the start of the next block.
    struct PendingHit {
        int lane;
        float velocity;
        int64_t at;
    };
    std::array<PendingHit, 64> rolls_{};
    int rollCount_ = 0;
    void hitLane(int lane, float velocity, int offset);
    void playRolls(int numFrames);
    std::array<float, kMaxBlock> mix_{};
    std::atomic<int> currentStep_{-1};
    std::atomic<int> absoluteStep_{-1};
    std::atomic<double> playPosition_{-1.0};
    int lastLocal_ = -1;        // local step of the last step played
    int stepOffset_ = -1;       // its frame offset in the current block, -1 if it was in an earlier one
    double sinceStep_ = 0.0;    // frames played since that step
    std::array<std::atomic<uint32_t>, kLanes> laneTriggers_{};
    std::atomic<uint64_t> metronomeClicks_{0};

    std::atomic<bool> playing_{false};
    std::atomic<bool> nonFinite_{false};
    std::atomic<uint64_t> callbacks_{0};
    std::atomic<uint64_t> frames_{0};
    std::atomic<float> peak_{0.0f};
};

}  // namespace ps
