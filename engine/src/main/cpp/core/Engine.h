#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cstdint>
#include <memory>

#include "AcidTrack.h"
#include "AudioTracks.h"
#include "ClipLauncher.h"
#include "DrumKit.h"
#include "InstrumentTrack.h"
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
    SetDrumHit,       // a: lane, b: step | hits in the step << 16 (0/1 plain, 2..4 roll), value: velocity (0 = remove)
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
                          // current scene; a = 0: at the next bar; a < 0: cancel. b = 1: it starts
                          // the song over (song automation counts from there)
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
    SetTrackOn,           // a: instrument track 1..kTracks-1, value 1 = in use (0 = silent, not rendered)
    InstallAudioClip,     // a: audio track, sample: owned reference (nullptr = empty track)
    SetAudioClipParam,    // a: audio track, b: AudioTracks::Param, value
    RecordArm,            // record: buffer the app keeps; a = 1: starts when the transport reaches its first step, 0: now
    RecordStop,           // the armed or running recording ends (the buffer's state becomes 2)
    SetAudioOn,           // a: audio track, value 1 = plays (follows SetStaging: a song part's mutes)
    SetBeatsPerBar,       // a: 2..7 (time signature a/4): metronome accent, bar lines, live pads grid
    SetAutomationMode,    // a: lane, b: 0 loop / 1 per scene / 2 song (value: bars of song values)
    SetAutomationSong,    // a: lane, b: bar (0..255), value 0..1: the song-long curve, one value per bar
    SetExpression,        // a: InstrumentTrack::Expression | track << 16, value (bend in semitones, mod 0..1, sustain 0/1, arpeggiator settings)
    SetPadPan,            // a: pad, value -1..1 (synthesized and sampled sounds alike)
    SetDrumStepDetail,    // a: pad, b: step | (pitch + 64) << 16 (semitones, sampled sounds), value: pan -1..1 added to the pad's;
                          // after the step's SetDrumHit, which resets both
    SetMacro,             // a: mixer channel, b: TrackMacros::Macro (colour, dirt, motion, space), value 0..1
    InstallTrackSample,   // a: track << 16, sample: owned reference (nullptr = none): the sampled source's sound
    SetTrackSampleParam,  // a: SampleInstrument::Param | track << 16, value in that parameter's unit
    SetPadStrip,          // a: pad 0..15, value > 0.5: the pad plays through its own mixer strip (its effects)
};

// Instrument tracks: the synth commands (SynthNoteOn/Off, SynthParam, SetSynthNote, SetSynthNoteCount,
// SetSynthSteps, SetInstrument, SetSynthEngine (0 synth, 1 Plaits, 2 sampled), SetPlaitsParam,
// InstallTrackSample, SetTrackSampleParam) name their track in bits 16..23 of
// `a` (0 = the first track, as before tracks existed); the low 16 bits keep their old meaning.
constexpr int kTracks = 8;
inline int trackOf(int32_t a) { return std::clamp((a >> 16) & 0xFF, 0, kTracks - 1); }
inline int32_t trackArg(int32_t a) { return a & 0xFFFF; }
// The mixer channel of each instrument track: track 0 on channel 2, the others after the master's
// index (Mixer::kMaster = 4).
inline int trackChannel(int track) { return track == 0 ? 2 : 4 + track; }
// Audio tracks come after the instrument tracks.
inline int audioChannel(int track) { return 12 + track; }

// Step automation of mixer settings, played on the audio thread at each sequencer step.
struct Automation {
    enum Kind { Volume = 0, Pan = 1, Send = 2, InsertParam = 3, BusParam = 4 };
    static constexpr int kLanes = 8;
    static constexpr int kSteps = 64;
    static constexpr int kSongBars = 256;
    enum Mode { Loop = 0, PerScene = 1, Song = 2 };
    static int32_t pack(int kind, int ch, int slot, int param) { return (kind << 16) | (ch << 12) | (slot << 8) | param; }
    struct Lane {
        int32_t target = -1;
        int mode = Loop;
        std::array<float, kSteps> values{};
        std::array<float, kSteps> staged{};  // per-scene lanes: the waiting scene's values
        bool hasStaged = false;
        std::array<float, kSongBars> song{};  // song lanes: a value per bar, linear in between
        int songBars = 0;
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
    // InstallSample, InstallClip, InstallAudioClip, InstallTrackSample: a reference the engine takes
    // over (released if the command is dropped).
    Sample* sample = nullptr;
    // SetInstrument only: an instance the engine takes over (closed if the command is dropped).
    tsf* font = nullptr;
    StemBundle* stems = nullptr;
    // RecordArm only: the app keeps ownership; the engine writes into it until its state is 2.
    RecordBuffer* record = nullptr;
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
        for (auto& t : tracks_) {
            t.instrument.collectGarbage();
            t.sampled.collectGarbage();
        }
        stems_.collectGarbage();
        preview_.collectGarbage();
        clips_.collectGarbage();
        audio_.collectGarbage();
    }
    bool audioClipPlaying(int track) const { return audio_.playing(track); }  // tests

    // The last kScopeFrames of the output (mono) or of the microphone, oldest first, for the
    // spectrum analyser and the tuner. Any thread; the copy may straddle one block (harmless here).
    static constexpr int kScopeFrames = 4096;
    void readScope(bool microphone, float* out) const {
        const auto& ring = microphone ? micScope_ : outScope_;
        const uint32_t end = (microphone ? micScopeAt_ : outScopeAt_).load(std::memory_order_acquire);
        for (int i = 0; i < kScopeFrames; ++i) out[i] = ring[(end + static_cast<uint32_t>(i)) % kScopeFrames].load(std::memory_order_relaxed);
    }
    bool stemsLoaded() const { return stems_.loaded(); }  // tests
    int64_t stemPosition() const { return stems_.position(); }
    // Song position of the preview player in frames, -1 when stopped.
    int64_t previewPosition() const { return preview_.position(); }
    ClipLauncher::State clipState(int pad) const { return clips_.state(pad); }
    double clipBeat() const { return clips_.beat(); }  // tests
    bool instrumentActive(int track = 0) const { return tracks_[static_cast<size_t>(track)].instrument.active(); }  // tests
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
    // Steps since the song (re)started, as song automation reads it; -1 before the first step.
    int songStep() const { return songStep_.load(std::memory_order_relaxed); }
    bool transportRunning() const { return sequencer_.running(); }
    // Increments every time a lane is triggered (pad lights, tests).
    uint32_t laneTriggerCount(int lane) const { return laneTriggers_[lane].load(std::memory_order_relaxed); }
    // Number of metronome clicks triggered since startup (tests).
    Mixer& mixer() { return mixer_; }
    int synthActiveVoices(int track = 0) const { return tracks_[static_cast<size_t>(track)].synth.activeVoices(); }
    int sampledActiveVoices(int track) const { return tracks_[static_cast<size_t>(track)].sampled.activeVoices(); }  // tests
    int notesPending(int track = 0) const { return tracks_[static_cast<size_t>(track)].notes.pendingCount(); }  // tests
    uint32_t acidNotesPlayed() const { return acid_.notesPlayed(); }
    uint64_t metronomeClicks() const { return metronomeClicks_.load(std::memory_order_relaxed); }

private:
    // Rendered in slices of at most this: a voice takes one new hit per slice, and two hits of a
    // pad are always more than 256 frames apart (a 4-hit roll at 300 BPM: 600). Bluetooth hands
    // over 960 to 1920 frames at once.
    static constexpr int kMaxBlock = 256;

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
    // Instrument tracks (piano roll + sound); each has its own mixer channel (trackChannel).
    std::array<InstrumentTrack, kTracks> tracks_{};
    Sampler sampler_;
    StemPlayer stems_;
    // The remix screen's own listening of a song's stems, free of the transport.
    StemPlayer preview_;
    ClipLauncher clips_;
    AudioTracks audio_;
    std::array<std::array<float, kMaxBlock>, AudioTracks::kTracks> audioL_{}, audioR_{};
    // Microphone recording (audio tracks).
    RecordBuffer* record_ = nullptr;
    bool recordOnStart_ = false;  // waiting for the transport's first step
    int recordSkip_ = -1;         // input frames to skip in this callback (recording just started), -1 = none
    void finishRecording();
    void recordInput(const float* input, int numInputFrames, int inputChannels);
    Automation automation_;
    void applyAutomation(int patternStep, int songStep);
    std::array<std::atomic<float>, kScopeFrames> outScope_{}, micScope_{};
    std::atomic<uint32_t> outScopeAt_{0}, micScopeAt_{0};
    static void scopeWrite(std::array<std::atomic<float>, kScopeFrames>& ring, std::atomic<uint32_t>& at, float v) {
        const uint32_t i = at.load(std::memory_order_relaxed);
        ring[i].store(v, std::memory_order_relaxed);
        at.store((i + 1) % kScopeFrames, std::memory_order_release);
    }

    // The waiting scene (filled while staging_), copied over the playing patterns at the swap.
    struct StagedScene {
        float drums[kLanes][kMaxSteps] = {};
        uint8_t repeats[kLanes][kMaxSteps] = {};
        int8_t pan[kLanes][kMaxSteps] = {};    // -100..100 = -1..1
        int8_t pitch[kLanes][kMaxSteps] = {};  // semitones
        int drumSteps = 16;
        std::array<bool, AudioTracks::kTracks> audioOn{true, true, true, true};
        std::array<AcidStep, AcidTrack::kMaxSteps> acid{};
        int acidLength = 16;
    };
    StagedScene staged_;
    bool staging_ = false;
    int swapAt_ = -1;      // local step of the swap; 0 = next bar; -1 = none
    int sceneStart_ = 0;   // absolute step where the playing scene began
    int songStart_ = 0;    // absolute step where the song (last) began
    bool songRestart_ = false;
    std::atomic<int> songStep_{-1};
    std::atomic<uint32_t> sceneSwaps_{0};
    void swapScene(int step);
    Mixer mixer_;
    std::array<float, kMaxBlock> stemsL_{};
    std::array<float, kMaxBlock> stemsR_{};
    std::array<float, kMaxBlock> drumsBuf_{};
    std::array<float, kMaxBlock> drumsRBuf_{};
    std::array<float, kMaxBlock> acidBuf_{};
    std::array<std::array<float, kMaxBlock>, kTracks> trackBuf_{};
    bool metronomeOn_ = false;
    int patternSteps_ = 16;
    float pattern_[kLanes][kMaxSteps] = {};
    uint8_t repeats_[kLanes][kMaxSteps] = {};  // 0 or 1 = one hit
    int8_t stepPan_[kLanes][kMaxSteps] = {};    // -100..100 = -1..1, added to the pad's pan
    int8_t stepPitch_[kLanes][kMaxSteps] = {};  // semitones (sampled sounds)
    std::array<float, kLanes> padPan_{};
    // Pads with their own strip (Mixer::kFirstPad + pad) and their buffers (on the heap: 128 kB).
    std::array<bool, kLanes> padStrip_{};
    using PadBuffers = std::array<std::array<float, kMaxBlock>, kLanes>;
    std::unique_ptr<PadBuffers> padL_ = std::make_unique<PadBuffers>();
    std::unique_ptr<PadBuffers> padR_ = std::make_unique<PadBuffers>();
    // Extra hits of rolls still to come: frames from the start of the next block.
    struct PendingHit {
        int lane;
        float velocity;
        int64_t at;
        float pan;
        float pitch;
    };
    std::array<PendingHit, 64> rolls_{};
    int rollCount_ = 0;
    void hitLane(int lane, float velocity, int offset, float pan = 0.0f, float pitch = 0.0f);
    void playRolls(int numFrames);
    std::array<float, kMaxBlock> mix_{};
    std::atomic<int> currentStep_{-1};
    std::atomic<int> absoluteStep_{-1};
    std::atomic<double> playPosition_{-1.0};
    int lastLocal_ = -1;        // local step of the last step played
    int countInStep_ = 0;       // the count-in's last click step (negative); only read before the first step has played
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
