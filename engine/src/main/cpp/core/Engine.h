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
    SetDrumHit,       // a: lane, b: step | hits in the step << 16 (0/1 plain, 2..8 roll) | ramp << 24 (0 each hit 0.85 x the
                      // one before, as always; 1 rising, 2 falling: from / to half the velocity; 3 even), value: velocity (0 = remove)
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
    InstallSample,     // a: pad, sample: owned reference (nullptr = back to the drum sound); a: Sampler::layerArg (pad | layer + 1 << 8
                       // | velocity low << 16 | high << 24, 0..127): a layer of a sampled kit (nullptr = that layer removed)
    SetPadParam,       // a: pad, b: Sampler::Param, value in that parameter's unit
    SetSynthNote,      // a: NoteTrack::packIndex(index, start, length) (the index alone up to 4095 ticks), b: NoteTrack::pack(start,
                       // length, pitch), value: velocity
    SetSynthNoteCount, // a: number of notes in use
    SetSynthSteps,     // a: piano-roll pattern length in steps (1..256: 16 bars)
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
    SetSynthEngine,       // a: 0 = subtractive synth, 1 = Plaits, 2 = sampled, 3 = multizone, 4 = the 808 (a General MIDI instrument still wins)
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
    InstallTrackSample,   // a: track << 16, sample: owned reference (nullptr = none): the sampled source's sound;
                          // a: track << 16 | zone + 1 (phase 38): zone `zone` of the track's multizone instrument (MultiSampler)
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
    // Phase 34 (S3): the drums' micro-timing, rolls, glides and studio voices.
    SetDrumStepOffset,    // a: pad, b: step, value: milliseconds -50..50 this step's hit (and its roll) plays early (< 0) or
                          // late. An early hit is scheduled from the step before it (its pattern as known then); on the
                          // first step after play it lands on the grid. After the step's SetDrumHit, which resets it
    SetDrumStepRoll,      // a: pad, b: step | span << 8 | ramp << 12 | triplet << 15 | hits << 16: the step's roll: `hits`
                          // (1..16, at most 8 a step) spread evenly over `span` steps (1..4), ramp 0 = as before (each
                          // hit 0.85 x the one before), 1 = flat, 2 = up (the first hit at velocity x (1 - value), the
                          // last at the velocity), 3 = down (the reverse); triplet = 1: a shuffle (every second gap
                          // longer, 2:1). value: the ramp's amount 0..1. After the step's SetDrumHit, which resets it
    SetDrumStepGlide,     // a: pad, b: step, value: milliseconds 0..500 the 808 slides to this hit's pitch from the one it
                          // plays (a legato hit; 0: a new note). After the step's SetDrumHit, which resets it
    SetPadVoice,          // a: pad, value: DrumKit::Kind (0 = the kit's own voice, 1 = the 808 bass, 2 = the studio kick,
                          // 3 = the hard kick, 4 = the rumble); the voice's settings go back to its defaults
    SetPadVoiceParam,     // a: pad, b: DrumKit::VoiceParam, value in that parameter's unit (after SetPadVoice)
    // Numbered apart (phase 33), so that commands appended above in the meantime never take its number.
    SetKitVersion = 100,  // value: the built-in kit's levels, 1 (projects saved before), 2 (kit v2) or 3 (kit v3: DrumKit::setVersion)
    SetSoundVersion = 101,  // value: 1 (projects saved before S4: every instrument track mono, as always), 2 (S4: General MIDI
                            // instruments and stereo samples play in stereo on the instrument tracks)
    // Phase 34 (S5): the real sidechain and the « Musique » bus (Mixer.h).
    SetSidechain = 102,     // a = 0: the key, value = a mixer channel (a pad's own strip: Mixer::kFirstPad + pad), < 0 none;
                            // a = 1, 2, 3: the Musique bus's duck: depth, low depth (under 150 Hz), release (0..1: 50..600 ms);
                            // a = 4: channel b in the Musique bus (value 1) or not; a = 5: send return b (0 reverb, 1 echo) in it
    SetMixVersion = 103,    // value: 1 (projects saved before S5: the loudness target aims at the whole song, the chain may take
                            // 6 LU), 2 (S5: it aims at the loudest part, the Drop, and takes 3 LU at most: MasterChain::Mode)
    // Phase 37, numbered apart (other branches append from 102): polyrhythm and chance on the drums.
    SetDrumLaneSteps = 120,   // a: pad, value: steps its lane loops over on its own (1..64; 0 = the pattern's length,
                              // as always): a polyrhythm. Follows SetStaging (a scene's own); counted from the drums' start
    SetDrumStepChance = 121,  // a: pad, b: step, value 0..1: the chance its hit (and its roll) plays on each pass (1 = always).
                              // Drawn from the seed (SetDrumSeed), the pad and the step's place in the song (outside the
                              // song: in the drums' loop since they started): the same passes every time, an export
                              // included. After the step's SetDrumHit, which resets it to 1
    SetDrumSeed = 122,        // value: the chances' seed (a whole number 0..2^24): another draw of the same chances
    // Phase 38, numbered apart (130 on: 102 and 103 are the S5 commands above): multizone instruments and
    // the 808 on instrument tracks.
    SetTrackZone = 130,     // a: MultiSampler::ZoneParam | track << 16, b: zone (0..kZones-1), value in that parameter's
                            // unit; b = -1: a = MultiSampler::Param | track << 16, the instrument's own settings
    SetTrack808Param = 131, // a: Track808::Param (DrumKit::VoiceParam) | track << 16, value in that parameter's unit
    // G1b, numbered apart (140 on): a track's layers (InstrumentTrack::Layer). A layer's sound takes the
    // sound commands above (SetSynthEngine, SynthParam, SetPlaitsParam, SetInstrument,
    // InstallTrackSample, SetTrackSampleParam, SetTrackZone, SetTrack808Param) with its sound index
    // layerSound(track, layer) in place of the track; its strip (Mixer::layerChannel) takes the mixer
    // commands (SetChannelPan, SetChannelMute, SetSend, SetInsertType, SetInsertParam, ClearInserts).
    SetLayer = 140,        // a: LayerParam | layer << 8 | track << 16, value: on (1 plays the track's notes), low / high
                           // note (MIDI), transpose (semitones), fine (-1..1 semitone), gain (linear 0..4, after its
                           // effects), through (1: into the track's inserts, 0: after them)
    SetLayerListen = 141,  // a: track, value: mask of its sounds heard (bit 0 its own, bit 1 + k layer k; 15 = all, as mixed)
};
static_assert(static_cast<int>(CommandType::SetPadVoiceParam) < 100, "the commands in order must stay under SetKitVersion");
static_assert(static_cast<int>(CommandType::SetDrumStepOffset) == 80, "NativeEngine.kt numbers the phase 34 commands from 80");
static_assert(static_cast<int>(CommandType::SetDrumLaneSteps) == 120, "NativeEngine.kt numbers the phase 37 commands from 120");
static_assert(static_cast<int>(CommandType::SetTrackZone) == 130, "NativeEngine.kt numbers the phase 38 commands from 130");
static_assert(static_cast<int>(CommandType::SetLayer) == 140, "NativeEngine.kt numbers the G1b commands from 140");

// The live grid's slots: one per track, each playing its own scene's part from its own start.
enum Slot : int {
    kSlotDrums = 0,
    kSlotAcid = 1,
    kSlotInst = 2,    // + instrument track 0..kTracks-1
    kSlotAudio = 10,  // + audio track 0..3
    kSlotRemix = 14,
    kSlotAudio2 = 15,  // + audio track 4..7 - 4 (after the remix: the numbers of saved takes stay)
    kSlots = 19,
};
// The live slot of audio track `track` (0..AudioTracks::kTracks-1), and the reverse (-1: not one).
constexpr int audioSlot(int track) { return track < 4 ? kSlotAudio + track : kSlotAudio2 + track - 4; }
constexpr int audioOfSlot(int k) {
    return k >= kSlotAudio && k < kSlotAudio + 4 ? k - kSlotAudio : k >= kSlotAudio2 && k < kSlotAudio2 + AudioTracks::kTracks - 4 ? k - kSlotAudio2 + 4 : -1;
}
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
// The multizone instruments' voices together (8 tracks of 32 stereo voices would take a phone's
// core): the tracks playing one share them, 32 each at most, 16 each with all eight.
constexpr int kMultiVoices = 128;
inline int trackOf(int32_t a) { return std::clamp((a >> 16) & 0xFF, 0, kTracks - 1); }
inline int32_t trackArg(int32_t a) { return a & 0xFFFF; }
// G1b: the sounds the sound commands address, the tracks' own (0..kTracks-1) then their layers'.
constexpr int kLayers = InstrumentTrack::kLayers;
constexpr int kSounds = kTracks + kTracks * kLayers;
constexpr int layerSound(int track, int layer) { return kTracks + track * kLayers + layer; }
inline int soundOf(int32_t a) { return std::clamp((a >> 16) & 0xFF, 0, kSounds - 1); }
// A layer's synth plays at most this many notes at once (the CPU budget: layers_cpu_budget): a
// six-note chord whole, the seventh note steals (NativeEngine.LAYER_VOICES, Layers.MAX_VOICES).
constexpr int kLayerVoices = 6;
// SetLayer's last value of LayerParam (after InstrumentTrack's own): the gain and the routing.
enum LayerMix { LayerGain = 5, LayerThrough = 6 };
static_assert(Mixer::kLayersPerTrack == kLayers && Mixer::kLayerTracks == kTracks, "one strip per layer");
// The mixer channel of each instrument track: track 0 on channel 2, the others after the master's
// index (Mixer::kMaster = 4).
inline int trackChannel(int track) { return track == 0 ? 2 : 4 + track; }
static_assert(Mixer::trackStrip(3) == 7 && Mixer::trackStrip(0) == 2, "Mixer::trackStrip is trackChannel");
// Audio tracks come after the instrument tracks (1..4) and after the pads' strips (5..8).
constexpr int audioChannel(int track) { return track < 4 ? 12 + track : Mixer::kFirstAudio2 + track - 4; }
// The audio track on mixer channel `ch` (audioChannel's inverse), -1: none.
constexpr int audioOfChannel(int ch) {
    return ch >= 12 && ch < 16 ? ch - 12 : ch >= Mixer::kFirstAudio2 && ch < Mixer::kFirstAudio2 + AudioTracks::kTracks - 4 ? ch - Mixer::kFirstAudio2 + 4 : -1;
}

// Step automation of mixer settings, played on the audio thread at each sequencer step.
struct Automation {
    // Acid: a knob of the 303 (param: AcidTrack::Param), taking effect on the step's frame. Macro: a strip's
    // macro (param 0..3), its chopper (4), its kills (5..7) or throws (8, 9) (Mixer::setMacro). Live: the
    // master's PerformanceFx (PerformanceFx::Param), except 6 and 7: the reverb and echo returns (0.5 = as mixed).
    // Synth (S4): a knob of an instrument track's synth (param: Synth::Param) on the step's frame; Plaits:
    // one of its Plaits' (PlaitsSynth::Param). For both, ch is the track's mixer channel (trackChannel).
    enum Kind { Volume = 0, Pan = 1, Send = 2, InsertParam = 3, BusParam = 4, Acid = 5, Macro = 6, Live = 7, Synth = 8, Plaits = 9 };
    static constexpr int kLanes = 16;
    static constexpr int kSteps = 64;
    static constexpr int kSongBars = 256;
    static constexpr int kSongPoints = 4096;  // a point per beat: 1024 bars of 4/4 (AutomationLane.MAX_SONG_BEATS)
    enum Mode { Loop = 0, PerScene = 1, Song = 2 };
    static constexpr int kPerBeat = 16;  // SetAutomationMode flags
    static constexpr int kHeld = 32;
    // The channel's low 4 bits at 12, its bits 4..5 at 24 (channels past 15: the audio tracks 5..8;
    // 0 there for every target saved before them).
    static int32_t pack(int kind, int ch, int slot, int param) { return ((ch >> 4) << 24) | (kind << 16) | ((ch & 0xF) << 12) | (slot << 8) | param; }
    static int kindOf(int32_t target) { return (target >> 16) & 0xFF; }
    static int channelOf(int32_t target) { return ((target >> 12) & 0xF) | (((target >> 24) & 0x3) << 4); }
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
        for (int k = 0; k < kSounds; ++k) {
            auto& t = sound(k);
            t.instrument.collectGarbage();
            t.sampled.collectGarbage();
            t.multi.collectGarbage();
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
    // G1b: sound `s` (a track's own, 0..kTracks-1, or a layer's, layerSound()). Tests, and the engine.
    InstrumentTrack& sound(int s) {
        return s < kTracks ? tracks_[static_cast<size_t>(s)] : (*layers_)[static_cast<size_t>(std::clamp(s - kTracks, 0, kTracks * kLayers - 1))];
    }
    const InstrumentTrack& track(int t) const { return tracks_[static_cast<size_t>(t)]; }  // tests
    // Whether layer `layer` of track `track` is rendered (on, or its release still ringing). Tests.
    bool layerRendered(int track, int layer) const { return layerTail_[static_cast<size_t>(track * kLayers + layer)] > 0; }
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
    const DrumKit& drums() const { return drums_; }  // tests
    int synthActiveVoices(int track = 0) const { return tracks_[static_cast<size_t>(track)].synth.activeVoices(); }
    int sampledActiveVoices(int track) const { return tracks_[static_cast<size_t>(track)].sampled.activeVoices(); }  // tests
    int multiActiveVoices(int track) const { return tracks_[static_cast<size_t>(track)].multi.activeVoices(); }  // tests
    int notesPending(int track = 0) const { return tracks_[static_cast<size_t>(track)].notes.pendingCount(); }  // tests
    uint32_t acidNotesPlayed() const { return acid_.notesPlayed(); }
    uint64_t metronomeClicks() const { return metronomeClicks_.load(std::memory_order_relaxed); }
    // Phase 37: whether a hit of chance `skip` (100 - %) plays on pad `lane` at place `key` with `seed`.
    // The app draws the same (DrumChance.kt, the MIDI export), so keep both in step.
    static bool chancePlays(uint32_t seed, int lane, int key, int skip) {
        if (skip <= 0) return true;
        if (skip >= 100) return false;
        const uint32_t h = chanceHash(seed ^ chanceHash(static_cast<uint32_t>(key) * 16u + static_cast<uint32_t>(lane)));
        return static_cast<int>(h % 100u) >= skip;
    }
    // lowbias32 (Chris Wellons, public domain): a well-mixed 32-bit integer hash.
    static uint32_t chanceHash(uint32_t x) {
        x ^= x >> 16;
        x *= 0x7feb352du;
        x ^= x >> 15;
        x *= 0x846ca68bu;
        x ^= x >> 16;
        return x;
    }
    int drumLaneSteps(int lane) const { return laneSteps_[lane]; }  // tests

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
    // G1b: the tracks' layers' sounds (track t's layer k at t * kLayers + k), on the heap (2.6 MB),
    // with their buffers, and how long each still renders once switched off (its release).
    using LayerSounds = std::array<InstrumentTrack, kTracks * kLayers>;
    std::unique_ptr<LayerSounds> layers_ = std::make_unique<LayerSounds>();
    using LayerBuffers = std::array<std::array<float, kMaxBlock>, kTracks * kLayers>;
    std::unique_ptr<LayerBuffers> layerL_ = std::make_unique<LayerBuffers>(), layerR_ = std::make_unique<LayerBuffers>();
    std::array<int, kTracks * kLayers> layerTail_{};
    // A layer switched off still renders this long (its notes' release), fading out over the last kLayerFadeMs.
    static constexpr float kLayerTailSeconds = 4.0f;
    static constexpr float kLayerFadeMs = 20.0f;
    void renderLayers(const float** channels, const float** rights, int numFrames);
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

    // A step's micro-timing, roll shape and glide (SetDrumStepOffset, SetDrumStepRoll, SetDrumStepGlide).
    struct DrumStepExtra {
        int16_t offset = 0;   // tenths of a millisecond, -500..500
        uint8_t span = 1;     // steps the roll covers, 1..4
        uint8_t ramp = 0;     // 0 as before (0.85^k), 1 flat, 2 up, 3 down
        uint8_t amount = 0;   // the ramp's amount, 0..100
        uint8_t triplet = 0;  // 1: shuffled sub-hits
        uint8_t glide = 0;    // milliseconds / 2, 0..250 (0..500 ms)
    };
    static constexpr int kMaxRollHits = 16, kMaxRollHitsPerStep = 8, kMaxRollSpan = 4;
    // A rising or falling roll sent with its hit (SetDrumHit): the first (last) hit at half the velocity.
    static constexpr uint8_t kHitRampAmount = 50;
    // The waiting scene (filled while staging_), copied over the playing patterns at the swap.
    struct StagedScene {
        float drums[kLanes][kMaxSteps] = {};
        uint8_t repeats[kLanes][kMaxSteps] = {};
        int8_t pan[kLanes][kMaxSteps] = {};    // -100..100 = -1..1
        int8_t pitch[kLanes][kMaxSteps] = {};  // semitones
        DrumStepExtra extra[kLanes][kMaxSteps] = {};
        uint8_t skip[kLanes][kMaxSteps] = {};  // 100 - the hit's chance in %, 0 = always plays
        uint8_t laneSteps[kLanes] = {};        // 0: the pattern's length
        int drumSteps = 16;
        std::array<bool, AudioTracks::kTracks> audioOn = filled<bool, AudioTracks::kTracks>(true);
        std::array<int, AudioTracks::kTracks> audioLoop = filled<int, AudioTracks::kTracks>(-1);  // -1: unchanged
        std::array<AcidStep, AcidTrack::kMaxSteps> acid{};
        int acidLength = 16;
        // Per insert slot: 1 plays, 0 passed by in the waiting scene, -1 unchanged.
        std::array<int8_t, Mixer::kChannels * Mixer::kInserts> insertOn = unchangedInserts();
        bool remixOn = true;  // SetRemixOn while staging: false when the waiting song part mutes the remix
    };
    template <typename T, size_t N>
    static std::array<T, N> filled(T v) {
        std::array<T, N> a{};
        a.fill(v);
        return a;
    }
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
    std::array<std::array<float, kMaxBlock>, kTracks> trackBufR_{};  // a stereo track's right channel
    // The instrument track of mixer channel `ch` (trackChannel's inverse), -1: none.
    static int trackOfChannel(int ch) { return ch == 2 ? 0 : (ch >= 5 && ch < 4 + kTracks ? ch - 4 : -1); }
    bool metronomeOn_ = false;
    int patternSteps_ = 16;
    float pattern_[kLanes][kMaxSteps] = {};
    uint8_t repeats_[kLanes][kMaxSteps] = {};  // 0 or 1 = one hit
    int8_t stepPan_[kLanes][kMaxSteps] = {};    // -100..100 = -1..1, added to the pad's pan
    int8_t stepPitch_[kLanes][kMaxSteps] = {};  // semitones (sampled sounds, the 808)
    DrumStepExtra stepExtra_[kLanes][kMaxSteps] = {};
    // Phase 37: each hit's chance (stored as 100 - %, so the zeroed arrays mean « always ») and each
    // lane's own loop (0: the pattern's), with the draw's seed.
    uint8_t skip_[kLanes][kMaxSteps] = {};
    uint8_t laneSteps_[kLanes] = {};
    uint32_t chanceSeed_ = 0;
    // The pattern step lane `lane` plays at drums step `local` (its own loop, or the pattern's of `steps`).
    static int laneStepOf(const uint8_t* laneSteps, int lane, int local, int steps) {
        const int n = laneSteps[lane] > 0 ? laneSteps[lane] : steps;
        return ((local % n) + n) % n;
    }
    // Where the draws of the step after `step` are taken from: its place in the song (or in the drums' loop).
    int chanceKeyAfter(int step, bool swapNext, int nextDrumsLocal) const;
    std::array<float, kLanes> padPan_{};
    // Pads with their own strip (Mixer::kFirstPad + pad) and their buffers (on the heap: 128 kB).
    std::array<bool, kLanes> padStrip_{};
    int keyPad_ = -1;  // S5: the drum pad the sidechain key listens to (-1: none, or not a pad)
    using PadBuffers = std::array<std::array<float, kMaxBlock>, kLanes>;
    std::unique_ptr<PadBuffers> padL_ = std::make_unique<PadBuffers>();
    std::unique_ptr<PadBuffers> padR_ = std::make_unique<PadBuffers>();
    // Hits still to come (a step's hit with its offset, a roll's hits, an early hit of the next
    // step): frames from the start of the current block (negative: before it, played at once).
    struct PendingHit {
        int lane;
        float velocity;
        int64_t at;
        float pan;
        float pitch;
        int glide;  // frames
    };
    // Every lane on a 16-hit roll over 4 steps, overlapping, and the next step's early hits.
    std::array<PendingHit, 512> rolls_{};
    int rollCount_ = 0;
    void hitLane(int lane, float velocity, int offset, float pan = 0.0f, float pitch = 0.0f, int glideFrames = 0);
    // A pad's voice takes one hit per render; a second hit of the same pad in the same slice (a late
    // hit followed by the next step's early one, a roll over a hit: review m4) waits in deferred_
    // and the drums are rendered in pieces, each hit on its own frame. `pan` includes the pad's.
    struct LaneHit {
        int lane = 0;
        int at = -1;  // frame in the slice, -1: none
        float velocity = 0.0f, pan = 0.0f, pitch = 0.0f;
        int glide = 0;
    };
    void triggerPad(int lane, float velocity, int offset, float pan, float pitch, int glideFrames);
    void startHit(const LaneHit& h, int offset);
    std::array<LaneHit, kLanes> laneHit_{};  // the hit each pad's voice holds for this slice
    static constexpr int kDeferred = 64;
    std::array<LaneHit, kDeferred> deferred_{};
    int deferredCount_ = 0;
    // Drums and samples of the slice, in pieces split at the deferred hits.
    void renderPads(float* const* lefts, float* const* rights, int numFrames);
    void playRolls(int numFrames);
    // Queues the hits of `lane` on pattern step `patternStep` (the hit with its offset, then its
    // roll), the step's grid frame at `base` frames from the block's start, `step` the transport step
    // (its swing); `fromStaged`: the waiting scene's pattern (a swap on that step).
    // `key`: the step's place for its chance (SetDrumStepChance).
    void queueStepHits(int lane, int patternStep, int64_t base, int step, bool fromStaged, int key);
    // The grid frames from transport step `step` to the next one (the swing's share included).
    double stepFrames(int step) const;
    // Early hits (a negative offset) of the next transport step are queued from this one: that
    // step then skips those lanes (the mask), -1: none.
    int lookaheadStep_ = -1;
    uint32_t lookaheadLanes_ = 0;
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
