#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
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
    SetTransport,     // value: 1 = play, 0 = stop; a = count-in bars; b = song step the first step is (started from a later part),
                      // b < 0: a scene on its own, outside the song (no song position: song clips and curves wait)
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
                          // the song over (song automation counts from there, from song step
                          // `value` when it is a later part); b = 2: the song
                          // ends there: the transport stops on that bar line (nothing is swapped);
                          // b = 3: the waiting scene plays on its own, outside the song (with
                          // a < 0: right away, the scene playing; the live grid's launches still
                          // waiting are kept when no scene swap was armed: a take's end)
    SetPerformance,       // a: PerformanceFx::Param, value 0..1 (the Live page's effects and their settings; 6, 7: the
                          // reverb and echo returns, 0.5 = as mixed)
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
    SetAudioClipParam,    // a: audio track, b: AudioTracks::Param, value (LoopSteps follows SetStaging: a part's own)
    RecordArm,            // record: buffer the app keeps; a = 1: starts when the transport reaches its first step, 0: now
    RecordStop,           // the armed or running recording ends (the buffer's state becomes 2)
    SetAudioOn,           // a: audio track, value 1 = plays (follows SetStaging: a song part's mutes)
    SetBeatsPerBar,       // a: 2..7 (time signature a/4): metronome accent, bar lines, live pads grid
    SetAutomationMode,    // a: lane, b: 0 loop / 1 per scene / 2 song, | 16: a song point per beat, | 32: song points held (no
                          // slide: on/off controls); value: song points sent
    SetAutomationSong,    // a: lane, b: point (a bar 0..255, or a beat 0..4095), value 0..1: the song-long curve
    SetExpression,        // a: InstrumentTrack::Expression | track << 16, value (bend in semitones, mod 0..1, sustain 0/1, arpeggiator settings)
    SetPadPan,            // a: pad, value -1..1 (synthesized and sampled sounds alike)
    SetDrumStepDetail,    // a: pad, b: step | (pitch + 64) << 16 (semitones, sampled sounds), value: pan -1..1 added to the pad's;
                          // after the step's SetDrumHit, which resets both
    SetMacro,             // a: mixer channel, b: TrackMacros::Macro (colour, dirt, motion, space; 4 = the strip's
                          // chopper, on above 0.5; 5..7 its bass / mid / high kill; 8, 9 a throw into the reverb /
                          // echo on a rise: Mixer::setMacro), value 0..1
    InstallTrackSample,   // a: track << 16, sample: owned reference (nullptr = none): the sampled source's sound
    SetTrackSampleParam,  // a: SampleInstrument::Param | track << 16, value in that parameter's unit
    SetPadStrip,          // a: pad 0..15, value > 0.5: the pad plays through its own mixer strip (its effects)
    SetMaster,            // a: 0 = gain before the limiter (value: linear 0..4), 1 = limiter (value 1 = on),
                          // 2 = master inserts (value 1 = on; off for stems, which then add up to the mix)
                          // 3 = the Live page's own sounds (riser, downlifter, build-up noise; value 0 = in the mix,
                          // 1 = left out: a track's stem, 2 = alone: the stem « Effets live », PerformanceFx::Noises)
    ClearInserts,         // a: mixer channel: every insert slot emptied (a strip no longer used gives its pool units back)
    SetInsertOn,          // a: channel * Mixer::kInserts + slot, value 1: the effect plays in this scene, 0: the sound goes
                          // through it untouched (« Joue dans »). Follows SetStaging: the waiting scene's, switched at the swap
    SetAutomationHold,    // a: Automation::pack(kind, channel, slot, param) of a setting, value 1: a finger holds it (touch
                          // take-over: no lane writes it until let go), 0: let go (lanes drive it again from the next step)
    // Live grid (one slot per track, see Slot below):
    ArmLaunch,            // a: mask of slots (1 << slot), b: quantum in steps (>= 1 bar), value: scene tag >= 0 (its
                          // content was staged with SetStaging before) or -1 = stop. On the first transport step that is a
                          // multiple of b, before anything of that step sounds, each slot takes over its staged part (or is
                          // emptied: stop) and counts from there. The whole mask = a row: also master/bus curves and effects,
                          // the scene start and sceneSwaps(). The last command for a slot wins; ArmSceneSwap cancels.
    CancelLaunch,         // a: mask of slots whose armed launch is dropped
    SetSlotOffset,        // a: slot, b: steps the slot's content has already played when it takes over. Follows
                          // SetStaging: while staging, used by the next launch or scene swap of that slot (then back to
                          // 0); else by the transport's next start (SetTransport), never by a swap already staged
    SetBusReturn,         // a: send bus (0 reverb, 1 echo), value: its return 0..2 (1 = as mixed): the live grid's sliders
    MarkSong,             // a: quantum in steps (>= 1 bar), value: a song step. On the next transport step that is a multiple of
                          // a, the song position becomes `value` there (song curves and song clips follow): the start of a
                          // recorded performance (songMarkStep() tells the step). a <= 0: cancel a mark not yet made
    SetAutomationSongRun, // a: lane, b: first point | count << 16, value 0..1: that many song points at once (a curve
                          // sent as runs of equal values: a whole take fits in the command queue)
    SetRemixOn,           // value 1: the remix plays, 0: silent (keeping its place). Follows SetStaging: a song part's,
                          // taken at the scene swap (a swap without one plays it); else at once
    SetAutomationSongRamp,// a: lane | end << 8 (end value 0..65535 = 0..1), b: first point | count << 16, value: the first
                          // point's value: `count` points on a straight line from `value` toward the end value (the next
                          // point's): a slide of a song curve in one command
    SetAutomationSongFlat,// a: lane, b: first point | count << 16, value 1: the curve holds over those points instead
                          // of sliding toward the next one (the point after is a jump: a take's Drop, a let-go);
                          // 0: slides again. SetAutomationMode clears them (sent first with each curve)
    // Numbered apart (phase 33), so that commands appended above in the meantime never take its number.
    SetKitVersion = 100,  // value: the built-in kit's levels, 1 (projects saved before) or 2 (kit v2: DrumKit::setVersion)
};
static_assert(static_cast<int>(CommandType::SetAutomationSongFlat) < 100, "the commands in order must stay under SetKitVersion");

// The live grid's slots: one per track, each playing its own scene's part from its own start.
enum Slot : int {
    kSlotDrums = 0,
    kSlotAcid = 1,
    kSlotInst = 2,    // + instrument track 0..kTracks-1
    kSlotAudio = 10,  // + audio track 0..3
    kSlotRemix = 14,
    kSlots = 15,
};
constexpr int32_t kAllSlots = (1 << kSlots) - 1;

// A launch that happened (audio thread -> UI): the slot, its new tag (-1: stopped) and the
// transport step it took over on.
struct LaunchEvent {
    int16_t slot = 0;
    int16_t tag = -1;
    int32_t step = 0;
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
    // Acid: a knob of the 303 (param: AcidTrack::Param), taking effect on the step's frame. Macro: a strip's
    // macro (param 0..3), its chopper (4), its kills (5..7) or throws (8, 9) (Mixer::setMacro). Live: the
    // master's PerformanceFx (PerformanceFx::Param), except 6 and 7: the reverb and echo returns (0.5 = as mixed).
    enum Kind { Volume = 0, Pan = 1, Send = 2, InsertParam = 3, BusParam = 4, Acid = 5, Macro = 6, Live = 7 };
    static constexpr int kLanes = 16;
    static constexpr int kSteps = 64;
    static constexpr int kSongBars = 256;
    static constexpr int kSongPoints = 4096;  // a point per beat: 1024 bars of 4/4 (AutomationLane.MAX_SONG_BEATS)
    enum Mode { Loop = 0, PerScene = 1, Song = 2 };
    static constexpr int kPerBeat = 16;  // SetAutomationMode flags
    static constexpr int kHeld = 32;
    static int32_t pack(int kind, int ch, int slot, int param) { return (kind << 16) | (ch << 12) | (slot << 8) | param; }
    struct Lane {
        int32_t target = -1;
        int mode = Loop;
        std::array<float, kSteps> values{};
        std::array<float, kSteps> staged{};  // per-scene lanes: the waiting scene's values
        bool hasStaged = false;
        std::array<float, kSongPoints> song{};  // song lanes: a value per bar (or beat), linear in between
        int songPoints = 0;
        bool perBeat = false;  // a point per beat instead of per bar
        bool held = false;     // each point holds until the next (on/off controls)
        std::array<uint8_t, kSongPoints> flat{};  // 1: this point holds (the next one is a jump)
    };
    // On the heap (about 340 kB), allocated once with the engine.
    std::unique_ptr<std::array<Lane, kLanes>> laneBank = std::make_unique<std::array<Lane, kLanes>>();
    std::array<Lane, kLanes>& lanes = *laneBank;
    // Settings a finger holds (packed targets, -1 = free slot): lanes leave them alone meanwhile,
    // so a knob turned during the song is not pulled back by its curve. Audio thread only.
    // Room for every lane (16) and every control of the Live page at once (filter, held effects,
    // 15 track choppers, the 303's six), as a take holds what it moved until its end.
    static constexpr int kHolds = 32;
    std::array<int32_t, kHolds> held{};
    Automation() {
        for (auto& l : lanes) l.values.fill(-1.0f);
        held.fill(-1);
    }
    bool isHeld(int32_t target) const {
        if (target < 0) return false;
        for (const int32_t h : held) if (h == target) return true;
        return false;
    }
    // Real-time safe. More than kHolds held at once (ten fingers): the extra ones are not taken over.
    void hold(int32_t target, bool on) {
        if (target < 0) return;
        for (auto& h : held) if (h == target) { if (!on) h = -1; return; }
        if (!on) return;
        for (auto& h : held) if (h < 0) { h = target; return; }
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
    // How much of the queue one callback applies (0, the default: everything). The phone's stream
    // sets kLiveCommandBudget: a full resync (thousands of commands) is spread over a few callbacks
    // instead of making one of them miss its deadline; switching an effect type (a long one clears
    // seconds of memory) counts as kHeavyCommand. A staged scene (SetStaging on) is always taken
    // whole, so a swap never finds it half there. An offline render takes everything at once (its
    // commands land on exact frames). Not real-time: set it before the stream runs.
    static constexpr int kLiveCommandBudget = 1024, kHeavyCommand = 8;
    void setCommandBudget(int budget) { commandBudget_ = std::max(0, budget); }

    // Real-time safe.
    void process(const float* input, int numInputFrames, int inputChannels,
                 float* output, int numFrames, int outputChannels);

    int sampleRate() const { return sampleRate_; }
    uint64_t callbackCount() const { return callbacks_.load(std::memory_order_relaxed); }
    // How busy the audio thread is: the time process() takes over the time its frames last,
    // smoothed over about half a second (1 = no time to spare: the sound starts to crackle).
    float cpuLoad() const { return cpuLoad_.load(std::memory_order_relaxed); }
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
    // Steps since the song (re)started, as song automation reads it; -1 before the first step and
    // while a scene plays on its own, outside the song.
    int songStep() const { return songStep_.load(std::memory_order_relaxed); }
    // The transport step a MarkSong took effect on (-1: none yet since play).
    int songMarkStep() const { return songMarkStep_.load(std::memory_order_relaxed); }
    // Where the transport is, in sixteenths since play with the fraction (a gesture's place in a
    // recorded performance); -1 when stopped or counting in. Updated once per block.
    double transportPosition() const { return transportPosition_.load(std::memory_order_relaxed); }
    bool transportRunning() const { return sequencer_.running(); }
    // Transport steps since play (the live grid's clock, the quantum counts from it), -1 when
    // stopped or counting in.
    int transportStep() const { return transportStep_.load(std::memory_order_relaxed); }
    // Per slot: the tag playing (scene >= 0, -1 stopped, -2 the row's scene), the tag waiting
    // (-3 none, -1 stop, >= 0 a scene) and the transport step it started on.
    static constexpr int kNoLaunch = -3;
    static constexpr int kRowTag = -2;
    // The largest song step or slot offset a command may give (2^20 sixteenths: hours of song).
    static constexpr int kMaxSongStep = 1 << 20;
    int slotTag(int slot) const { return slotTag_[static_cast<size_t>(slot)].load(std::memory_order_relaxed); }
    int slotPending(int slot) const { return slotPending_[static_cast<size_t>(slot)].load(std::memory_order_relaxed); }
    int slotStart(int slot) const { return slotStart_[static_cast<size_t>(slot)].load(std::memory_order_relaxed); }
    // Consumer side of the launch events (one thread). False when none is waiting.
    bool popLaunchEvent(LaunchEvent& e) { return launchEvents_.pop(e); }
    static constexpr int kLaunchEvents = 256;
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
    int commandBudget_ = 0;
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
    // No input (nullptr) while recording writes `numFrames` of silence: the take stays in time.
    void recordInput(const float* input, int numInputFrames, int inputChannels, int numFrames);
    Automation automation_;
    void applyAutomation(int patternStep, int songStep, int offset);
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
        std::array<int, AudioTracks::kTracks> audioLoop{-1, -1, -1, -1};  // -1: unchanged
        std::array<AcidStep, AcidTrack::kMaxSteps> acid{};
        int acidLength = 16;
        // Per insert slot: 1 plays, 0 passed by in the waiting scene, -1 unchanged.
        std::array<int8_t, Mixer::kChannels * Mixer::kInserts> insertOn = unchangedInserts();
        bool remixOn = true;  // SetRemixOn while staging: false when the waiting song part mutes the remix
    };
    static std::array<int8_t, Mixer::kChannels * Mixer::kInserts> unchangedInserts() {
        std::array<int8_t, Mixer::kChannels * Mixer::kInserts> a{};
        a.fill(-1);
        return a;
    }
    StagedScene staged_;
    bool staging_ = false;
    int swapAt_ = -1;      // local step of the swap; 0 = next bar; -1 = none
    int sceneStart_ = 0;   // absolute step where the playing scene began
    int songStart_ = 0;    // absolute step where the song (last) began
    bool songRestart_ = false;
    int songRestartAt_ = 0;    // the song step the restarting part is (a later part than the first)
    bool stopAtSwap_ = false;  // the armed "swap" ends the song: the transport stops instead
    bool leaveSong_ = false;   // the armed swap leaves the song: its scene plays on its own
    bool inSong_ = true;       // the song plays (else a scene on its own: no song position)
    void leaveSongNow();
    void stopTransport();
    std::atomic<int> songStep_{-1};
    std::atomic<uint32_t> sceneSwaps_{0};
    // MarkSong: armed on a quantum (0: none), the song step it starts.
    int markQuantum_ = 0;
    int markSongStep_ = 0;
    std::atomic<int> songMarkStep_{-1};
    std::atomic<double> transportPosition_{-1.0};
    int lastTransport_ = -1;  // transport step of the last step played
    void swapScene(int step, int offset);

    // --- Live grid ---
    struct SlotPlay {
        int start = 0;           // transport step its content counts from
        int pending = kNoLaunch; // tag armed (kNoLaunch: none, -1: stop)
        int quantum = 16;        // steps: it fires on a multiple of this
        int stagedOffset = 0;    // SetSlotOffset while staging: steps already played when its next launch or swap takes over
        int transportOffset = 0; // SetSlotOffset otherwise: the same for the transport's next start (SetTransport)
        int tag = kRowTag;
    };
    std::array<SlotPlay, kSlots> slots_{};
    bool anyPending_ = false;
    std::array<std::atomic<int>, kSlots> slotTag_{}, slotPending_{}, slotStart_{};
    std::atomic<int> transportStep_{-1};
    SpscQueue<LaunchEvent, kLaunchEvents> launchEvents_;
    void resetSlots(int start);
    void publishSlot(int k);
    void cancelLaunches(int32_t mask, bool dropStaged);
    void launchSlots(int step, int offset);
    // Slot k takes over its part of the staging ([empty]: emptied instead, a stop).
    void swapSlot(int k, int step, int offset, bool empty);
    void emptySlot(int k);
    static int slotOfChannel(int ch);
    static int slotOfTarget(int32_t target);
    int localOf(int k, int step) const { return step - slots_[static_cast<size_t>(k)].start; }
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
    float load_ = 0.0f;  // audio thread: the smoothed load, published in cpuLoad_
    std::atomic<float> cpuLoad_{0.0f};
    std::atomic<uint64_t> frames_{0};
    std::atomic<float> peak_{0.0f};
};

}  // namespace ps
