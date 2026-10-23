#include "Engine.h"
#include "Params.h"

#include "DenormalGuard.h"

#include <algorithm>
#include <cmath>
#include <limits>

namespace ps {

namespace {
constexpr double kTwoPi = 6.28318530717958647692;

}  // namespace

Engine::Engine(int sampleRate) {
    frequency_.snap(440.0f);
    resetSlots(0);
    for (int k = 0; k < kSlots; ++k) publishSlot(k);
    prepare(sampleRate);
}

Engine::~Engine() {
    // Commands never applied may still own sample references.
    Command command;
    while (commands_.pop(command)) {
        if (command.sample && command.sample->release()) delete command.sample;
        SoundFontPlayer::close(command.font);
        if (command.stems && command.stems->release()) delete command.stems;
        if (command.record) command.record->state.store(2, std::memory_order_release);
    }
    finishRecording();
}

void Engine::prepare(int sampleRate) {
    // Apply pending UI commands so a reopened stream keeps the user's settings,
    // but never replay a stale latency test request.
    Command command;
    while (commands_.pop(command)) {
        if (command.type != CommandType::StartLatencyTest) applyCommand(command);
    }
    sampleRate_ = sampleRate;
    frequency_.configure(static_cast<float>(sampleRate), 20.0f);
    gain_.configure(static_cast<float>(sampleRate), 10.0f);
    frequency_.snap(frequency_.target());
    gain_.snap(0.0f);
    phase_ = 0.0;
    meter_.prepare(sampleRate);
    sequencer_.prepare(sampleRate);
    metronome_.prepare(sampleRate);
    drums_.prepare(sampleRate);
    acid_.prepare(sampleRate);
    for (auto& t : tracks_) t.prepare(sampleRate);
    tracks_[0].on = true;
    mixer_.prepare(sampleRate);
    stems_.prepare(sampleRate);
    preview_.prepare(sampleRate);
    clips_.prepare(sampleRate);
    audio_.prepare(sampleRate);
    sampler_.prepare(sampleRate);
}

void Engine::applyCommand(const Command& command) {
    switch (command.type) {
        case CommandType::SetPlaying:
            playing_.store(command.value > 0.5f, std::memory_order_relaxed);
            break;
        case CommandType::SetFrequency:
            if (!std::isnan(command.value)) frequency_.setTarget(std::clamp(command.value, kMinFrequency, kMaxFrequency));
            break;
        case CommandType::SetGain:
            volume_ = clampOr(command.value, 0.0f, 1.0f, volume_);
            break;
        case CommandType::StartLatencyTest:
            meter_.start();
            break;
        case CommandType::CancelLatencyTest:
            meter_.cancel();
            break;
        case CommandType::SetTransport:
            if (command.value > 0.5f) {
                sequencer_.start(std::clamp(command.a, 0, 4));
                sceneStart_ = 0;
                // Started from a later song part: the song position counts from that part's place.
                // b < 0: a scene played on its own, outside the song (song automation and song
                // clips wait for the song).
                inSong_ = command.b >= 0;
                songStart_ = -std::max(0, command.b);
                songRestart_ = false;
                // Every slot counts from the first step (a song part carrying on: from where it was,
                // SetSlotOffset); launches armed while stopped fire on it.
                resetSlots(0);
                for (int k = 0; k < kSlots; ++k) {
                    auto& sl = slots_[static_cast<size_t>(k)];
                    const int offset = sl.transportOffset;
                    sl.transportOffset = 0;
                    if (sl.pending != kNoLaunch) continue;  // its launch on the first step takes its staged offset
                    sl.start = -offset;
                    publishSlot(k);
                }
                songMarkStep_.store(-1, std::memory_order_relaxed);
                transportPosition_.store(-1.0, std::memory_order_relaxed);
                lastTransport_ = -1;
                audio_.songJump();
                lastLocal_ = -1;
                countInStep_ = 0;
                playPosition_.store(-1.0, std::memory_order_relaxed);
                // Free LFOs and noise from their start: an export, which starts here, sounds like
                // the take (otherwise their phase is the time since the effect was put on).
                drums_.resetNoise();
                mixer_.restartLfos();
                for (auto& t : tracks_) {
                    t.synth.restartLfos();
                    t.plaits.restartNoise();
                }
            } else {
                stopTransport();
            }
            break;
        case CommandType::SetTempo:
            sequencer_.setTempo(command.value);
            break;
        case CommandType::SetKitVersion:
            if (std::isfinite(command.value)) drums_.setVersion(static_cast<int>(std::lround(command.value)));
            break;
        case CommandType::SetSwing:
            sequencer_.setSwing(command.value);
            break;
        case CommandType::SetMetronome:
            metronomeOn_ = command.value > 0.5f;
            break;
        case CommandType::SetMetronomeLevel:
            metronome_.setLevel(command.value);
            break;
        case CommandType::SetPatternSteps:
            if (staging_) staged_.drumSteps = std::clamp(command.a, 8, kMaxSteps);
            else patternSteps_ = std::clamp(command.a, 8, kMaxSteps);
            break;
        case CommandType::SetAcidStep:
            if (staging_) {
                if (command.a >= 0 && command.a < AcidTrack::kMaxSteps) staged_.acid[static_cast<size_t>(command.a)] = AcidStep::unpack(command.b);
            } else {
                acid_.setStep(command.a, AcidStep::unpack(command.b));
            }
            break;
        case CommandType::SetAcidLength:
            if (staging_) staged_.acidLength = std::clamp(command.a, 1, AcidTrack::kMaxSteps);
            else acid_.setLength(command.a);
            break;
        case CommandType::SetSynthEngine:
            tracks_[static_cast<size_t>(trackOf(command.a))].setSource(trackArg(command.a));
            break;
        case CommandType::InstallTrackSample:
            tracks_[static_cast<size_t>(trackOf(command.a))].sampled.install(command.sample);  // takes the reference
            break;
        case CommandType::SetTrackSampleParam:
            tracks_[static_cast<size_t>(trackOf(command.a))].sampled.setParam(trackArg(command.a), command.value);
            break;
        case CommandType::SetPadStrip:
            if (command.a >= 0 && command.a < kLanes) padStrip_[static_cast<size_t>(command.a)] = command.value > 0.5f;
            break;
        case CommandType::InstallAudioClip:
            audio_.install(command.a, command.sample);  // takes the reference
            break;
        case CommandType::SetAudioClipParam:
            // A part's own loop ("once" clips): it takes over with the waiting scene.
            if (staging_ && command.b == AudioTracks::LoopSteps && command.a >= 0 && command.a < AudioTracks::kTracks) {
                staged_.audioLoop[static_cast<size_t>(command.a)] = std::max(0, static_cast<int>(std::lround(command.value)));
            } else {
                audio_.setParam(command.a, command.b, command.value);
            }
            break;
        case CommandType::RecordArm:
            finishRecording();
            record_ = command.record;
            if (record_) {
                recordOnStart_ = command.a == 1;
                record_->written.store(0, std::memory_order_relaxed);
                record_->state.store(recordOnStart_ ? 0 : 1, std::memory_order_release);
                recordSkip_ = recordOnStart_ ? -1 : 0;
            }
            break;
        case CommandType::RecordStop:
            finishRecording();
            break;
        case CommandType::SetAudioOn:
            if (command.a >= 0 && command.a < AudioTracks::kTracks) {
                if (staging_) staged_.audioOn[static_cast<size_t>(command.a)] = command.value > 0.5f;
                else audio_.setOn(command.a, command.value > 0.5f);
            }
            break;
        case CommandType::SetBeatsPerBar:
            sequencer_.setBeatsPerBar(command.a);
            clips_.setBeatsPerBar(sequencer_.stepsPerBar() / Sequencer::kStepsPerBeat);
            break;
        case CommandType::SetTrackOn:
            if (command.a > 0 && command.a < kTracks) {
                auto& t = tracks_[static_cast<size_t>(command.a)];
                t.on = command.value > 0.5f;
                if (!t.on) {
                    t.notes.stop(t);
                    t.notes.setCount(0);
                    t.staged.count = 0;
                    t.allNotesOff();
                }
            }
            break;
        case CommandType::InstallClip:
            clips_.install(command.a, command.sample);  // takes the reference
            break;
        case CommandType::SetClipParam:
            clips_.setParam(command.a, command.b, command.value);
            break;
        case CommandType::ToggleClip:
            if (command.a < 0) clips_.stopAll();
            else clips_.toggle(command.a, !sequencer_.running());
            break;
        case CommandType::PlayPreview:
            if (command.value > 0.5f) {
                preview_.setStartFrame(0);
                preview_.start(0);
            } else {
                preview_.stop();
            }
            break;
        case CommandType::SetPreviewParam:
            preview_.setParam(command.a, command.b, command.value);
            break;
        case CommandType::SetPlaitsParam:
            if (trackArg(command.a) < PlaitsSynth::ParamCount) {
                tracks_[static_cast<size_t>(trackOf(command.a))].plaits.setParam(static_cast<PlaitsSynth::Param>(trackArg(command.a)), command.value);
            }
            break;
        case CommandType::SetPerformance:
            mixer_.setPerformance(command.a, command.value);
            break;
        case CommandType::SetStaging:
            staging_ = command.value > 0.5f;
            break;
        case CommandType::ArmSceneSwap: {
            // Leaving the song at once (a take's end) with no scene swap armed: the live grid's
            // launches still waiting stay armed, with what they staged (only the song position goes).
            bool gridWaits = false;
            for (const auto& sl : slots_) gridWaits |= sl.pending != kNoLaunch;
            const bool keepLaunches = command.a < 0 && command.b == 3 && swapAt_ < 0 && gridWaits;
            // The whole scene (the song, a scene chosen): what the live grid armed is dropped.
            if (!keepLaunches) cancelLaunches(kAllSlots, false);  // (its staging was just sent: kept)
            swapAt_ = command.a < 0 ? -1 : command.a;
            if (command.a < 0 && !keepLaunches) {
                // The cancelled scene's loops and effects never apply later.
                staged_.audioLoop.fill(-1);
                staged_.insertOn.fill(-1);
                staged_.remixOn = true;
            }
            songRestart_ = command.b == 1;  // the waiting scene starts the song over (it loops)
            songRestartAt_ = command.b == 1 && std::isfinite(command.value)
                ? static_cast<int>(std::lround(std::clamp(command.value, 0.0f, static_cast<float>(kMaxSongStep)))) : 0;
            stopAtSwap_ = command.b == 2;   // the song ends there: the transport stops instead
            leaveSong_ = command.b == 3;    // the waiting scene plays on its own, outside the song
            if (command.a < 0 && command.b == 3) leaveSongNow();
            staging_ = false;  // arming ends a staging burst even if its "off" was dropped
            break;
        }
        case CommandType::SetAcidRoot:
            acid_.setRoot(command.a);
            break;
        case CommandType::SetAcidParam:
            if (command.a >= 0 && command.a < AcidTrack::ParamCount) {
                acid_.setParam(static_cast<AcidTrack::Param>(command.a), command.value);
            }
            break;
        case CommandType::SynthNoteOn:
            tracks_[static_cast<size_t>(trackOf(command.a))].liveOn(trackArg(command.a) & 0x7F, command.value);
            break;
        case CommandType::SynthNoteOff:
            tracks_[static_cast<size_t>(trackOf(command.a))].liveOff(trackArg(command.a) & 0x7F);
            break;
        case CommandType::SetExpression:
            if (std::isfinite(command.value)) tracks_[static_cast<size_t>(trackOf(command.a))].setExpression(trackArg(command.a), command.value);
            break;
        case CommandType::SynthParam:
            if (trackArg(command.a) < Synth::ParamCount) {
                tracks_[static_cast<size_t>(trackOf(command.a))].synth.setParam(static_cast<Synth::Param>(trackArg(command.a)), command.value);
            }
            break;
        case CommandType::SynthAllOff:
            for (auto& t : tracks_) t.allNotesOff();
            break;
        case CommandType::SetChannelVolume:
            mixer_.setVolume(command.a, command.value);
            break;
        case CommandType::SetChannelPan:
            mixer_.setPan(command.a, command.value);
            break;
        case CommandType::SetChannelMute:
            mixer_.setMute(command.a, command.value > 0.5f);
            break;
        case CommandType::SetChannelSolo:
            mixer_.setSolo(command.a, command.value > 0.5f);
            break;
        case CommandType::InstallSample:
            sampler_.install(command.a, command.sample);  // takes the command's reference
            break;
        case CommandType::SetPadParam:
            sampler_.setParam(command.a, command.b, command.value);
            break;
        case CommandType::SetSynthNote: {
            auto& t = tracks_[static_cast<size_t>(trackOf(command.a))];
            const int index = trackArg(command.a);
            if (!staging_) {
                t.notes.setNote(index, command.b, command.value);
            } else if (index < NoteTrack::kMaxNotes) {
                t.staged.packed[static_cast<size_t>(index)] = command.b;
                t.staged.velocity[static_cast<size_t>(index)] = command.value;
            }
            break;
        }
        case CommandType::SetSynthNoteCount: {
            auto& t = tracks_[static_cast<size_t>(trackOf(command.a))];
            if (staging_) t.staged.count = std::clamp(trackArg(command.a), 0, NoteTrack::kMaxNotes);
            else t.notes.setCount(trackArg(command.a));
            break;
        }
        case CommandType::SetSynthSteps: {
            auto& t = tracks_[static_cast<size_t>(trackOf(command.a))];
            if (staging_) t.staged.steps = trackArg(command.a);
            else t.notes.setSteps(trackArg(command.a));
            break;
        }
        case CommandType::InstallStems:
            if (command.a == 1) preview_.install(command.stems);  // takes the reference
            else stems_.install(command.stems);
            break;
        case CommandType::SetStemParam:
            stems_.setParam(command.a, command.b, command.value);
            break;
        case CommandType::SetInsertType:
            mixer_.setInsertType(command.a / Mixer::kInserts, command.a % Mixer::kInserts, command.b);
            break;
        case CommandType::ClearInserts:
            mixer_.clearInserts(command.a);
            break;
        case CommandType::SetInsertOn:
            if (command.a >= 0 && command.a < Mixer::kChannels * Mixer::kInserts) {
                if (staging_) staged_.insertOn[static_cast<size_t>(command.a)] = command.value > 0.5f ? 1 : 0;
                else mixer_.setInsertOn(command.a / Mixer::kInserts, command.a % Mixer::kInserts, command.value > 0.5f);
            }
            break;
        case CommandType::SetInsertParam:
            mixer_.setInsertParam(command.a / Mixer::kInserts, command.a % Mixer::kInserts, command.b, command.value);
            break;
        case CommandType::SetMacro:
            mixer_.setMacro(command.a, command.b, command.value);
            break;
        case CommandType::SetMaster:
            if (command.a == 0) mixer_.setMasterGain(command.value);
            else if (command.a == 1) mixer_.setLimiter(command.value > 0.5f);
            else if (command.a == 2) mixer_.setMasterFx(command.value > 0.5f);
            else if (command.a == 3 && std::isfinite(command.value)) mixer_.setLiveNoises(static_cast<int>(std::lround(std::clamp(command.value, 0.0f, 2.0f))));
            else if (command.a == 4) mixer_.setLoudnessTarget(command.value);  // LUFS; 0 or NaN: no target
            else if (command.a == 5) mixer_.setCeiling(command.value);         // dBTP
            else if (command.a == 6) mixer_.resetLoudnessEstimate();           // another project
            break;
        case CommandType::SetSend:
            mixer_.setSend(command.a, command.b, command.value);
            break;
        case CommandType::SetBusParam:
            mixer_.setBusParam(command.a, command.b, command.value);
            break;
        case CommandType::SetAutomationTarget:
            if (command.a >= 0 && command.a < Automation::kLanes) {
                auto& lane = automation_.lanes[command.a];
                lane.target = command.b;
                lane.mode = Automation::Loop;
                lane.values.fill(-1.0f);
                lane.hasStaged = false;
                lane.songPoints = 0;
                lane.perBeat = false;
                lane.held = false;
                lane.flat.fill(0);
            }
            break;
        case CommandType::SetAutomationPoint:
            if (command.a >= 0 && command.a < Automation::kLanes && command.b >= 0 && command.b < Automation::kSteps) {
                auto& lane = automation_.lanes[command.a];
                const float v = !std::isfinite(command.value) || command.value < 0.0f ? -1.0f : std::min(command.value, 1.0f);
                if (staging_) {
                    // A per-scene lane: the waiting scene's values, taken over at the swap.
                    if (!lane.hasStaged) lane.staged.fill(-1.0f);
                    lane.hasStaged = true;
                    lane.staged[static_cast<size_t>(command.b)] = v;
                } else {
                    lane.values[static_cast<size_t>(command.b)] = v;
                }
            }
            break;
        case CommandType::SetAutomationHold:
            automation_.hold(command.a, command.value > 0.5f);
            break;
        case CommandType::ArmLaunch: {
            const int32_t mask = command.a & kAllSlots;
            if (mask == 0) break;
            const int tag = std::isfinite(command.value) && command.value >= 0.0f ? std::min(32767, static_cast<int>(std::lround(command.value))) : -1;
            const int quantum = std::clamp(command.b, 1, 1 << 16);
            for (int k = 0; k < kSlots; ++k) {
                if ((mask & (1 << k)) == 0) continue;
                auto& sl = slots_[static_cast<size_t>(k)];
                sl.pending = tag;
                sl.quantum = quantum;
                publishSlot(k);
            }
            anyPending_ = true;
            staging_ = false;  // arming ends a staging burst even if its "off" was dropped (as ArmSceneSwap)
            if (mask == kAllSlots) {
                // A whole row: it replaces a scene swap armed before (the last command wins).
                swapAt_ = -1;
                stopAtSwap_ = false;
                leaveSong_ = false;
                songRestart_ = false;
            }
            break;
        }
        case CommandType::CancelLaunch:
            cancelLaunches(command.a, true);
            break;
        case CommandType::SetBusReturn:
            mixer_.setBusReturn(command.a, command.value);
            break;
        case CommandType::SetAutomationSongRun: {
            const int first = command.b & 0xFFFF;
            const int count = (command.b >> 16) & 0xFFFF;
            if (command.a >= 0 && command.a < Automation::kLanes && first < Automation::kSongPoints && std::isfinite(command.value)) {
                auto& song = automation_.lanes[static_cast<size_t>(command.a)].song;
                const float v = std::clamp(command.value, 0.0f, 1.0f);
                std::fill(song.begin() + first, song.begin() + std::min(first + count, Automation::kSongPoints), v);
            }
            break;
        }
        case CommandType::MarkSong:
            markQuantum_ = std::clamp(command.a, 0, 1 << 16);
            // Bounded before the conversion (a huge float to int is undefined): far past any song.
            markSongStep_ = std::isfinite(command.value) ? static_cast<int>(std::lround(std::clamp(command.value, 0.0f, static_cast<float>(kMaxSongStep)))) : 0;
            break;
        case CommandType::SetSlotOffset:
            if (command.a >= 0 && command.a < kSlots) {
                // Bounded: `step - start` must never overflow (a negative local would index before a pattern).
                auto& sl = slots_[static_cast<size_t>(command.a)];
                (staging_ ? sl.stagedOffset : sl.transportOffset) = std::clamp(command.b, 0, kMaxSongStep);
            }
            break;
        case CommandType::SetRemixOn:
            if (staging_) staged_.remixOn = command.value > 0.5f;
            else stems_.setLive(command.value > 0.5f);
            break;
        case CommandType::SetAutomationSongRamp: {
            const int lane = command.a & 0xFF;
            const int first = command.b & 0xFFFF;
            const int count = (command.b >> 16) & 0xFFFF;
            if (lane < Automation::kLanes && first < Automation::kSongPoints && count > 0 && std::isfinite(command.value)) {
                auto& song = automation_.lanes[static_cast<size_t>(lane)].song;
                const float from = std::clamp(command.value, 0.0f, 1.0f);
                const float to = static_cast<float>((command.a >> 8) & 0xFFFF) / 65535.0f;
                const int last = std::min(first + count, Automation::kSongPoints);
                for (int i = first; i < last; ++i) song[static_cast<size_t>(i)] = from + (to - from) * static_cast<float>(i - first) / static_cast<float>(count);
            }
            break;
        }
        case CommandType::SetAutomationMode:
            if (command.a >= 0 && command.a < Automation::kLanes) {
                auto& lane = automation_.lanes[static_cast<size_t>(command.a)];
                lane.mode = std::clamp(command.b & 0xF, 0, 2);
                lane.perBeat = (command.b & Automation::kPerBeat) != 0;
                lane.held = (command.b & Automation::kHeld) != 0;
                const int most = lane.perBeat ? Automation::kSongPoints : Automation::kSongBars;
                lane.songPoints = std::isfinite(command.value) ? static_cast<int>(std::clamp(command.value, 0.0f, static_cast<float>(most))) : 0;
                lane.flat.fill(0);  // the curve's jumps follow it (SetAutomationSongFlat)
            }
            break;
        case CommandType::SetAutomationSongFlat: {
            const int first = command.b & 0xFFFF;
            const int count = (command.b >> 16) & 0xFFFF;
            if (command.a >= 0 && command.a < Automation::kLanes && first < Automation::kSongPoints) {
                auto& flat = automation_.lanes[static_cast<size_t>(command.a)].flat;
                std::fill(flat.begin() + first, flat.begin() + std::min(first + count, Automation::kSongPoints), command.value > 0.5f ? uint8_t{1} : uint8_t{0});
            }
            break;
        }
        case CommandType::SetAutomationSong:
            if (command.a >= 0 && command.a < Automation::kLanes && command.b >= 0 && command.b < Automation::kSongPoints && std::isfinite(command.value)) {
                automation_.lanes[command.a].song[static_cast<size_t>(command.b)] = std::clamp(command.value, 0.0f, 1.0f);
            }
            break;
        case CommandType::SetStemStart:
            stems_.setStartFrame(command.a);
            break;
        case CommandType::SetInstrument: {
            auto& t = tracks_[static_cast<size_t>(trackOf(command.a))];
            // "No instrument" on a track that has none changes nothing: its synth notes keep sounding.
            if (command.font == nullptr && !t.instrument.active()) break;
            t.synth.allNotesOff();
            t.instrument.install(command.font, trackArg(command.a));  // takes the instance
            break;
        }
        case CommandType::SetPadPan:
            if (command.a >= 0 && command.a < kLanes && std::isfinite(command.value)) {
                padPan_[static_cast<size_t>(command.a)] = std::clamp(command.value, -1.0f, 1.0f);
            }
            break;
        case CommandType::SetDrumStepDetail: {
            const int step = command.b & 0xFFFF;
            if (command.a >= 0 && command.a < kLanes && step < kMaxSteps && std::isfinite(command.value)) {
                const int pitch = std::clamp(((command.b >> 16) & 0xFF) - 64, -24, 24);
                (staging_ ? staged_.pan : stepPan_)[command.a][step] = static_cast<int8_t>(std::lround(std::clamp(command.value, -1.0f, 1.0f) * 100.0f));
                (staging_ ? staged_.pitch : stepPitch_)[command.a][step] = static_cast<int8_t>(pitch);
            }
            break;
        }
        case CommandType::TriggerPad:
            if (command.a >= 0 && command.a < kLanes) {
                const float pan = padPan_[static_cast<size_t>(command.a)];
                if (sampler_.has(command.a)) sampler_.trigger(command.a, command.value, 0, pan);
                else drums_.trigger(command.a, command.value, 0, pan);
                laneTriggers_[static_cast<size_t>(command.a)].fetch_add(1, std::memory_order_relaxed);
            }
            break;
        case CommandType::SetDrumRepeat:
            if (command.a >= 0 && command.a < kLanes && command.b >= 0 && command.b < kMaxSteps) {
                (staging_ ? staged_.repeats : repeats_)[command.a][command.b] = static_cast<uint8_t>(std::clamp(static_cast<int>(command.value), 1, 4));
            }
            break;
        case CommandType::SetDrumHit: {
            // b: step in the low 16 bits, hits in the step (0 or 1 = plain, up to 4 = a roll) above.
            const int step = command.b & 0xFFFF;
            const int repeats = std::clamp((command.b >> 16) & 0xFF, 1, 4);
            if (command.a >= 0 && command.a < kLanes && step < kMaxSteps) {
                (staging_ ? staged_.drums : pattern_)[command.a][step] = std::clamp(command.value, 0.0f, 1.0f);
                (staging_ ? staged_.repeats : repeats_)[command.a][step] = static_cast<uint8_t>(repeats);
                // Pan and pitch go back to plain: a SetDrumStepDetail follows when the hit has any.
                (staging_ ? staged_.pan : stepPan_)[command.a][step] = 0;
                (staging_ ? staged_.pitch : stepPitch_)[command.a][step] = 0;
            }
            break;
        }
    }
}

// Stop: everything the transport started goes quiet (the stop command, or a song that ends).
void Engine::stopTransport() {
    swapAt_ = -1;
    stopAtSwap_ = false;
    leaveSong_ = false;
    staging_ = false;  // a lost "staging off" must never trap later edits
    staged_.audioLoop.fill(-1);  // a part that never came must not change a clip's loop at a later swap
    staged_.insertOn.fill(-1);   // nor its effects
    sequencer_.stop();
    rollCount_ = 0;
    stems_.stop();
    // The live grid starts again from its rows: nothing armed, every slot heard (the app sends the
    // patterns of the scene shown again).
    cancelLaunches(kAllSlots, true);
    resetSlots(0);
    for (auto& sl : slots_) {
        sl.stagedOffset = 0;
        sl.transportOffset = 0;  // a part sent while playing never moves the next start
    }
    staged_.remixOn = true;
    stems_.setLive(true);
    transportStep_.store(-1, std::memory_order_relaxed);
    transportPosition_.store(-1.0, std::memory_order_relaxed);
    lastTransport_ = -1;
    markQuantum_ = 0;
    acid_.stop();
    audio_.stop();
    if (record_ && !recordOnStart_) finishRecording();
    sampler_.releaseLoops();
    for (auto& t : tracks_) {
        t.notes.stop(t);
        // Belt and braces: a note-off dropped by a full queue must not leave a note hanging. Keys
        // still held stay in the arpeggiator: it goes on with them.
        t.allNotesOff(true);
    }
    currentStep_.store(-1, std::memory_order_relaxed);
    absoluteStep_.store(-1, std::memory_order_relaxed);
    songStep_.store(-1, std::memory_order_relaxed);
    playPosition_.store(-1.0, std::memory_order_relaxed);
    lastLocal_ = -1;
    countInStep_ = 0;
}

void Engine::onStep(int offset, int step) {
    const int bar = sequencer_.stepsPerBar();
    const int beatStep = ((step % bar) + bar) % bar;
    const bool onBeat = beatStep % Sequencer::kStepsPerBeat == 0;
    // The song ends on this bar line: the transport stops before anything of this step sounds.
    if (step >= 0 && swapAt_ >= 0 && stopAtSwap_) {
        const int local = step - sceneStart_;
        if (local > 0 && local % bar == 0 && (swapAt_ == 0 || local >= swapAt_)) {
            stopTransport();
            sceneSwaps_.fetch_add(1, std::memory_order_relaxed);
            return;
        }
    }
    // Clicks during the count-in always; afterwards only with the metronome on.
    if (onBeat && (step < 0 || metronomeOn_)) {
        metronome_.trigger(beatStep == 0, offset);
        metronomeClicks_.fetch_add(1, std::memory_order_relaxed);
    }
    if (step < 0) {
        // Counting in: the position runs below zero up to the first step (a note played just
        // ahead of the "1" is still recorded on it).
        countInStep_ = step;
        stepOffset_ = offset;
        return;
    }
    if (step == 0) {
        stems_.start(offset);
        if (record_ && recordOnStart_) {
            recordOnStart_ = false;
            recordSkip_ = offset;
            record_->state.store(1, std::memory_order_release);
        }
    }
    if (onBeat) clips_.syncBeat(offset, step / Sequencer::kStepsPerBeat);
    transportStep_.store(step, std::memory_order_relaxed);
    lastTransport_ = step;
    // A recorded performance starts here: the song position is the one it will have in the song.
    if (markQuantum_ > 0 && step % markQuantum_ == 0) {
        const int start = step - markSongStep_;
        if (start != songStart_ || !inSong_) audio_.songJump();
        songStart_ = start;
        inSong_ = true;
        markQuantum_ = 0;
        songMarkStep_.store(step, std::memory_order_relaxed);
    }
    // A waiting scene takes over on its step (or the next bar), before anything of this step plays.
    if (swapAt_ >= 0) {
        const int local = step - sceneStart_;
        // Always on a bar line: a swap armed late waits for the next bar instead of cutting one.
        const bool onBar = local > 0 && local % bar == 0;
        if (onBar && !stopAtSwap_ && (swapAt_ == 0 || local >= swapAt_)) swapScene(step, offset);
    }
    // Live launches due on this step, after the scene swap (armed later, they win).
    if (anyPending_) launchSlots(step, offset);
    // Patterns count from the start of their slot (the scene's start unless the live grid launched
    // it on its own); the remix stems follow the transport.
    const int local = step - sceneStart_;
    acid_.onStep(offset, localOf(kSlotAcid, step), sequencer_.samplesPerStep());
    // Outside the song, no song position: song clips and song curves wait for the song.
    const int songStep = inSong_ ? step - songStart_ : -1;
    std::array<int, AudioTracks::kTracks> audioLocals{};
    for (int k = 0; k < AudioTracks::kTracks; ++k) audioLocals[static_cast<size_t>(k)] = localOf(kSlotAudio + k, step);
    audio_.onStep(offset, audioLocals, songStep, sequencer_.samplesPerStep(), sequencer_.swing(), songStart_);
    for (int k = 0; k < kTracks; ++k) {
        auto& t = tracks_[static_cast<size_t>(k)];
        if (t.on) t.notes.onStep(offset, localOf(kSlotInst + k, step), sequencer_.samplesPerStep(), sequencer_.swing());
        t.arpSync(offset, step, sequencer_.samplesPerStep());
    }
    // The drums' own step; loop curves follow the scene's.
    const int drumsLocal = localOf(kSlotDrums, step);
    const int patternStep = ((drumsLocal % patternSteps_) + patternSteps_) % patternSteps_;
    songStep_.store(songStep, std::memory_order_relaxed);
    // Loop and per-scene curves run with the drum loop (they carry on with it in a song part that
    // carries on, as during the live take it was written from).
    applyAutomation(patternStep, songStep, offset);
    // Sidechain pumps follow the kick lane (pad 1) or every quarter note.
    mixer_.sidechainEvent(offset, pattern_[0][patternStep] > 0.0f, onBeat, step, bar);
    currentStep_.store(patternStep, std::memory_order_relaxed);
    absoluteStep_.store(local, std::memory_order_relaxed);
    lastLocal_ = local;
    stepOffset_ = offset;
    for (int lane = 0; lane < kLanes; ++lane) {
        const float v = pattern_[lane][patternStep];
        if (v > 0.0f) {
            const float pan = stepPan_[lane][patternStep] * 0.01f;
            const float pitch = stepPitch_[lane][patternStep];
            hitLane(lane, v, offset, pan, pitch);
            // A roll: the other hits share the step evenly, each a little softer. A swung (odd)
            // step is shorter by the swing: its roll stays inside it, before the next step's hit.
            const int n = repeats_[lane][patternStep];
            const double length = sequencer_.samplesPerStep() * ((step & 1) != 0 ? 1.0 - sequencer_.swing() : 1.0);
            const double every = length / std::max(1, n);
            for (int k = 1; k < n && rollCount_ < static_cast<int>(rolls_.size()); ++k) {
                rolls_[static_cast<size_t>(rollCount_++)] = {lane, v * std::pow(0.85f, static_cast<float>(k)), offset + static_cast<int64_t>(k * every), pan, pitch};
            }
        }
    }
}

void Engine::hitLane(int lane, float velocity, int offset, float pan, float pitch) {
    laneTriggers_[static_cast<size_t>(lane)].fetch_add(1, std::memory_order_relaxed);
    pan += padPan_[static_cast<size_t>(lane)];
    if (sampler_.has(lane)) sampler_.trigger(lane, velocity, offset, pan, pitch);
    else drums_.trigger(lane, velocity, offset, pan);
}

void Engine::playRolls(int numFrames) {
    int kept = 0;
    for (int i = 0; i < rollCount_; ++i) {
        PendingHit h = rolls_[static_cast<size_t>(i)];
        if (h.at < numFrames) {
            hitLane(h.lane, h.velocity, static_cast<int>(h.at), h.pan, h.pitch);
        } else {
            h.at -= numFrames;
            rolls_[static_cast<size_t>(kept++)] = h;
        }
    }
    rollCount_ = kept;
}

void Engine::swapScene(int step, int offset) {
    std::copy(&staged_.drums[0][0], &staged_.drums[0][0] + kLanes * kMaxSteps, &pattern_[0][0]);
    std::copy(&staged_.repeats[0][0], &staged_.repeats[0][0] + kLanes * kMaxSteps, &repeats_[0][0]);
    std::copy(&staged_.pan[0][0], &staged_.pan[0][0] + kLanes * kMaxSteps, &stepPan_[0][0]);
    std::copy(&staged_.pitch[0][0], &staged_.pitch[0][0] + kLanes * kMaxSteps, &stepPitch_[0][0]);
    patternSteps_ = staged_.drumSteps;
    for (int i = 0; i < AcidTrack::kMaxSteps; ++i) acid_.setStep(i, staged_.acid[static_cast<size_t>(i)]);
    acid_.setLength(staged_.acidLength);
    for (auto& t : tracks_) t.swapIn();
    for (int k = 0; k < AudioTracks::kTracks; ++k) {
        audio_.setOn(k, staged_.audioOn[static_cast<size_t>(k)]);
        auto& loop = staged_.audioLoop[static_cast<size_t>(k)];
        if (loop >= 0) audio_.setParam(k, AudioTracks::LoopSteps, static_cast<float>(loop));
        loop = -1;
    }
    // Effects that play in some scenes only switch on the swap's frame.
    for (size_t i = 0; i < staged_.insertOn.size(); ++i) {
        auto& on = staged_.insertOn[i];
        if (on >= 0) mixer_.scheduleInsertOn(static_cast<int>(i) / Mixer::kInserts, static_cast<int>(i) % Mixer::kInserts, on == 1, offset);
        on = -1;
    }
    for (auto& lane : automation_.lanes) {
        if (!lane.hasStaged) continue;
        lane.values = lane.staged;
        lane.hasStaged = false;
    }
    sceneStart_ = step;
    // Every slot takes the new scene from here (a part started later in: from its place in it).
    for (int k = 0; k < kSlots; ++k) {
        auto& sl = slots_[static_cast<size_t>(k)];
        sl.start = step - sl.stagedOffset;
        sl.stagedOffset = 0;
        sl.tag = kRowTag;
        publishSlot(k);
    }
    // The remix plays unless the part taking over mutes it (then for the next swap: played again).
    stems_.setLive(staged_.remixOn, offset);
    staged_.remixOn = true;
    if (songRestart_) {
        // A part that simply follows keeps the song running; a jump (back to the start, or to
        // another part, or into the song from a scene played on its own) moves it, and song
        // clips pick up there.
        const int start = step - songRestartAt_;
        if (start != songStart_ || !inSong_) audio_.songJump();
        songStart_ = start;
        inSong_ = true;
    } else if (leaveSong_) {
        leaveSongNow();
    }
    songRestart_ = false;
    leaveSong_ = false;
    songRestartAt_ = 0;
    swapAt_ = -1;
    sceneSwaps_.fetch_add(1, std::memory_order_relaxed);
}

// --- Live grid ------------------------------------------------------------------------------

void Engine::resetSlots(int start) {
    for (int k = 0; k < kSlots; ++k) {
        auto& sl = slots_[static_cast<size_t>(k)];
        sl.start = start;
        sl.tag = kRowTag;
        publishSlot(k);
    }
}

void Engine::publishSlot(int k) {
    const auto& sl = slots_[static_cast<size_t>(k)];
    slotTag_[static_cast<size_t>(k)].store(sl.tag, std::memory_order_relaxed);
    slotPending_[static_cast<size_t>(k)].store(sl.pending, std::memory_order_relaxed);
    slotStart_[static_cast<size_t>(k)].store(sl.start, std::memory_order_relaxed);
}

void Engine::cancelLaunches(int32_t mask, bool dropStaged) {
    bool any = false;
    for (int k = 0; k < kSlots; ++k) {
        auto& sl = slots_[static_cast<size_t>(k)];
        if ((mask & (1 << k)) != 0 && sl.pending != kNoLaunch) {
            sl.pending = kNoLaunch;
            // What it staged never applies later: its clip's loop and effects.
            if (!dropStaged) {
                publishSlot(k);
                continue;
            }
            if (k >= kSlotAudio && k < kSlotAudio + AudioTracks::kTracks) staged_.audioLoop[static_cast<size_t>(k - kSlotAudio)] = -1;
            for (int ch = 0; ch < Mixer::kChannels; ++ch) {
                if (slotOfChannel(ch) != k) continue;
                for (int i = 0; i < Mixer::kInserts; ++i) staged_.insertOn[static_cast<size_t>(ch * Mixer::kInserts + i)] = -1;
            }
            publishSlot(k);
        }
        any |= sl.pending != kNoLaunch;
    }
    anyPending_ = any;
}

int Engine::slotOfChannel(int ch) {
    if (ch == 0 || ch >= Mixer::kFirstPad) return kSlotDrums;
    if (ch == 1) return kSlotAcid;
    if (ch == 2) return kSlotInst;
    if (ch == 3) return kSlotRemix;
    if (ch > Mixer::kMaster && ch < 12) return kSlotInst + (ch - Mixer::kMaster);
    if (ch >= 12 && ch < 12 + AudioTracks::kTracks) return kSlotAudio + (ch - 12);
    return -1;  // the master: it changes with a whole row only
}

int Engine::slotOfTarget(int32_t target) {
    if (target < 0) return -1;
    const int kind = target >> 16;
    if (kind == Automation::Acid) return kSlotAcid;
    if (kind == Automation::BusParam || kind == Automation::Live) return -1;
    return slotOfChannel((target >> 12) & 0xF);
}

void Engine::launchSlots(int step, int offset) {
    int32_t fired = 0;
    bool waiting = false;
    for (int k = 0; k < kSlots; ++k) {
        const auto& sl = slots_[static_cast<size_t>(k)];
        if (sl.pending == kNoLaunch) continue;
        if (step % sl.quantum == 0) fired |= 1 << k;
        else waiting = true;
    }
    anyPending_ = waiting;
    if (fired == 0) return;
    // A whole row launched on one scene: the scene changes as with a swap (master curves and effects too).
    const int rowTag = slots_[0].pending;
    bool row = fired == kAllSlots && rowTag >= 0;
    for (int k = 1; row && k < kSlots; ++k) row = slots_[static_cast<size_t>(k)].pending == rowTag;
    for (int k = 0; k < kSlots; ++k) {
        if ((fired & (1 << k)) == 0) continue;
        const int tag = slots_[static_cast<size_t>(k)].pending;
        swapSlot(k, step, offset, tag < 0);
        auto& sl = slots_[static_cast<size_t>(k)];
        sl.tag = tag < 0 ? -1 : tag;
        sl.pending = kNoLaunch;
        publishSlot(k);
        // A full queue (the app not reading): the app finds the state in slotTag() anyway.
        launchEvents_.push(LaunchEvent{static_cast<int16_t>(k), static_cast<int16_t>(sl.tag), step});
    }
    if (row) {
        for (auto& lane : automation_.lanes) {
            if (!lane.hasStaged || slotOfTarget(lane.target) >= 0) continue;
            lane.values = lane.staged;
            lane.hasStaged = false;
        }
        for (int i = 0; i < Mixer::kInserts; ++i) {
            auto& on = staged_.insertOn[static_cast<size_t>(Mixer::kMaster * Mixer::kInserts + i)];
            if (on >= 0) mixer_.scheduleInsertOn(Mixer::kMaster, i, on == 1, offset);
            on = -1;
        }
        sceneStart_ = step;
        sceneSwaps_.fetch_add(1, std::memory_order_relaxed);
    }
}

void Engine::swapSlot(int k, int step, int offset, bool empty) {
    if (k == kSlotDrums) {
        if (empty) {
            std::fill(&pattern_[0][0], &pattern_[0][0] + kLanes * kMaxSteps, 0.0f);
            std::fill(&repeats_[0][0], &repeats_[0][0] + kLanes * kMaxSteps, uint8_t{0});
            std::fill(&stepPan_[0][0], &stepPan_[0][0] + kLanes * kMaxSteps, int8_t{0});
            std::fill(&stepPitch_[0][0], &stepPitch_[0][0] + kLanes * kMaxSteps, int8_t{0});
        } else {
            std::copy(&staged_.drums[0][0], &staged_.drums[0][0] + kLanes * kMaxSteps, &pattern_[0][0]);
            std::copy(&staged_.repeats[0][0], &staged_.repeats[0][0] + kLanes * kMaxSteps, &repeats_[0][0]);
            std::copy(&staged_.pan[0][0], &staged_.pan[0][0] + kLanes * kMaxSteps, &stepPan_[0][0]);
            std::copy(&staged_.pitch[0][0], &staged_.pitch[0][0] + kLanes * kMaxSteps, &stepPitch_[0][0]);
            patternSteps_ = staged_.drumSteps;
        }
    } else if (k == kSlotAcid) {
        for (int i = 0; i < AcidTrack::kMaxSteps; ++i) acid_.setStep(i, empty ? AcidStep{} : staged_.acid[static_cast<size_t>(i)]);
        if (!empty) acid_.setLength(staged_.acidLength);
    } else if (k >= kSlotInst && k < kSlotInst + kTracks) {
        auto& t = tracks_[static_cast<size_t>(k - kSlotInst)];
        if (empty) t.notes.setCount(0);  // notes sounding still get their note-off
        else t.swapIn();
    } else if (k >= kSlotAudio && k < kSlotAudio + AudioTracks::kTracks) {
        const int a = k - kSlotAudio;
        audio_.setOn(a, !empty && staged_.audioOn[static_cast<size_t>(a)]);
        auto& loop = staged_.audioLoop[static_cast<size_t>(a)];
        if (!empty && loop >= 0) audio_.setParam(a, AudioTracks::LoopSteps, static_cast<float>(loop));
        if (!empty) loop = -1;
    } else if (k == kSlotRemix) {
        stems_.setLive(!empty, offset);
    }
    auto& sl = slots_[static_cast<size_t>(k)];
    sl.start = step - sl.stagedOffset;
    sl.stagedOffset = 0;
    if (empty) return;
    // The slot's own effects that play in some scenes only, and its per-scene curves.
    for (int ch = 0; ch < Mixer::kChannels; ++ch) {
        if (slotOfChannel(ch) != k) continue;
        for (int i = 0; i < Mixer::kInserts; ++i) {
            auto& on = staged_.insertOn[static_cast<size_t>(ch * Mixer::kInserts + i)];
            if (on >= 0) mixer_.scheduleInsertOn(ch, i, on == 1, offset);
            on = -1;
        }
    }
    for (auto& lane : automation_.lanes) {
        if (!lane.hasStaged || slotOfTarget(lane.target) != k) continue;
        lane.values = lane.staged;
        lane.hasStaged = false;
    }
}

// The scene playing goes on by itself, outside the song: song clips fade, song curves hold.
void Engine::leaveSongNow() {
    if (inSong_) audio_.songJump();
    inSong_ = false;
}

void Engine::applyAutomation(int patternStep, int songStep, int offset) {
    if (patternStep < 0 || patternStep >= Automation::kSteps) return;
    const int bar = sequencer_.stepsPerBar();
    for (const auto& lane : automation_.lanes) {
        float v = -1.0f;
        if (lane.mode == Automation::Song) {
            // From the transport start: bar by bar (or beat by beat), sliding from one point's value
            // to the next (or holding it: on/off controls).
            if (lane.songPoints <= 0 || songStep < 0) continue;
            const int unit = lane.perBeat ? Sequencer::kStepsPerBeat : bar;
            const int b = std::min(songStep / unit, lane.songPoints - 1);
            const float a = lane.song[static_cast<size_t>(b)];
            const float c = lane.song[static_cast<size_t>(std::min(b + 1, lane.songPoints - 1))];
            const bool flat = lane.held || lane.flat[static_cast<size_t>(b)] != 0 || songStep / unit >= lane.songPoints - 1;
            const float t = flat ? 0.0f : static_cast<float>(songStep % unit) / static_cast<float>(unit);
            v = a + t * (c - a);
        } else {
            v = lane.values[static_cast<size_t>(patternStep)];
        }
        if (lane.target < 0 || v < 0.0f) continue;
        // Touch take-over: the finger on it wins until let go.
        if (automation_.isHeld(lane.target)) continue;
        const int kind = lane.target >> 16, ch = (lane.target >> 12) & 0xF, slot = (lane.target >> 8) & 0xF, param = lane.target & 0xFF;
        switch (kind) {
            case Automation::Volume: mixer_.setVolume(ch, v); break;
            case Automation::Pan: mixer_.setPan(ch, v * 2.0f - 1.0f); break;
            case Automation::Send: mixer_.setSend(ch, slot, v); break;
            case Automation::InsertParam: mixer_.setInsertParam(ch, slot, param, v); break;
            case Automation::BusParam: mixer_.setBusParam(slot, param, v); break;
            case Automation::Acid:
                if (param < AcidTrack::ParamCount) acid_.automate(static_cast<AcidTrack::Param>(param), v, offset);
                break;
            // The Live page's gestures, on their step's frame.
            case Automation::Macro: mixer_.scheduleSetting(Mixer::MacroSetting, ch, param, v, offset); break;
            case Automation::Live:
                if (param == PerformanceFx::kReverbReturn || param == PerformanceFx::kEchoReturn) {
                    mixer_.scheduleSetting(Mixer::ReturnSetting, param - PerformanceFx::kReverbReturn, 0, v * 2.0f, offset);
                } else if (param < PerformanceFx::kParams) {
                    mixer_.scheduleSetting(Mixer::PerformanceSetting, param, 0, v, offset);
                }
                break;
            default: break;
        }
    }
}

void Engine::renderBlock(float* output, int numFrames, int outputChannels) {
    float* direct = mix_.data();  // metronome + test tone: straight to the master
    float* drums = drumsBuf_.data();
    float* drumsR = drumsRBuf_.data();
    std::fill(drumsR, drumsR + numFrames, 0.0f);
    float* acid = acidBuf_.data();
    std::fill(direct, direct + numFrames, 0.0f);
    std::fill(drums, drums + numFrames, 0.0f);
    std::fill(acid, acid + numFrames, 0.0f);
    float* stemsL = stemsL_.data();
    float* stemsR = stemsR_.data();
    std::fill(stemsL, stemsL + numFrames, 0.0f);
    std::fill(stemsR, stemsR + numFrames, 0.0f);

    stepOffset_ = -1;
    sequencer_.advance(numFrames, [this](int offset, int step) { onStep(offset, step); });
    playRolls(numFrames);
    if (sequencer_.running() && (lastLocal_ >= 0 || countInStep_ < 0)) {
        sinceStep_ = stepOffset_ >= 0 ? static_cast<double>(numFrames - stepOffset_) : sinceStep_ + numFrames;
        const int from = lastLocal_ >= 0 ? lastLocal_ : countInStep_;
        playPosition_.store(from + sinceStep_ / sequencer_.samplesPerStep(), std::memory_order_relaxed);
        if (lastTransport_ >= 0) transportPosition_.store(lastTransport_ + sinceStep_ / sequencer_.samplesPerStep(), std::memory_order_relaxed);
    }
    for (auto& t : tracks_) {
        if (!t.on) continue;
        // The tempo-locked filter sweep follows the beat (a plain store when the tempo is unchanged).
        t.synth.setBeatFrames(sequencer_.samplesPerStep() * 4.0);
        t.notes.advance(numFrames, t);
        t.arpAdvance(numFrames, sequencer_.samplesPerStep());
    }
    metronome_.render(direct, numFrames);
    const float* channels[Mixer::kChannels] = {drums, acid, nullptr, stemsL};
    const float* rights[Mixer::kChannels] = {drumsR, nullptr, nullptr, stemsR};
    // Pads with effects of their own play into their strip; the others into the drums' one.
    static_assert(kLanes == Mixer::kPads && kLanes == DrumKit::kVoices && kLanes == Sampler::kSlots,
                  "one strip and one render target per pad");
    float* padLeft[kLanes];
    float* padRight[kLanes];
    for (int lane = 0; lane < kLanes; ++lane) {
        if (padStrip_[static_cast<size_t>(lane)]) {
            padLeft[lane] = (*padL_)[static_cast<size_t>(lane)].data();
            padRight[lane] = (*padR_)[static_cast<size_t>(lane)].data();
            std::fill(padLeft[lane], padLeft[lane] + numFrames, 0.0f);
            std::fill(padRight[lane], padRight[lane] + numFrames, 0.0f);
            channels[Mixer::kFirstPad + lane] = padLeft[lane];
            rights[Mixer::kFirstPad + lane] = padRight[lane];
        } else {
            padLeft[lane] = drums;
            padRight[lane] = drumsR;
        }
    }
    drums_.render(padLeft, padRight, numFrames);
    sampler_.render(padLeft, padRight, numFrames);
    acid_.render(acid, numFrames);
    for (int k = 0; k < kTracks; ++k) {
        auto& t = tracks_[static_cast<size_t>(k)];
        if (!t.on) continue;
        float* buf = trackBuf_[static_cast<size_t>(k)].data();
        std::fill(buf, buf + numFrames, 0.0f);
        t.render(buf, numFrames);
        channels[trackChannel(k)] = buf;
    }
    stems_.render(stemsL, stemsR, numFrames);
    preview_.render(stemsL, stemsR, numFrames);
    clips_.setBeatFrames(sequencer_.samplesPerStep() * Sequencer::kStepsPerBeat);
    clips_.render(stemsL, stemsR, numFrames);

    gain_.setTarget(playing_.load(std::memory_order_relaxed) ? volume_ : 0.0f);
    const double invRate = 1.0 / sampleRate_;
    for (int i = 0; i < numFrames; ++i) {
        direct[i] += static_cast<float>(std::sin(phase_)) * gain_.next();
        phase_ += kTwoPi * frequency_.next() * invRate;
        if (phase_ >= kTwoPi) phase_ -= kTwoPi;
    }

    mixer_.setStepFrames(sequencer_.samplesPerStep());
    // Steps arrive (the count-in's do not count): the live effects wait for the grid's lines.
    mixer_.setClockRunning(sequencer_.running() && lastTransport_ >= 0);
    {
        float* lefts[AudioTracks::kTracks];
        float* rs[AudioTracks::kTracks];
        for (int k = 0; k < AudioTracks::kTracks; ++k) {
            lefts[k] = audioL_[static_cast<size_t>(k)].data();
            rs[k] = audioR_[static_cast<size_t>(k)].data();
            std::fill(lefts[k], lefts[k] + numFrames, 0.0f);
            std::fill(rs[k], rs[k] + numFrames, 0.0f);
            if (audio_.has(k)) {
                channels[audioChannel(k)] = lefts[k];
                rights[audioChannel(k)] = rs[k];
            }
        }
        audio_.render(lefts, rs, numFrames);
    }
    mixer_.process(channels, rights, direct, output, numFrames, outputChannels);
}

void Engine::process(const float* input, int numInputFrames, int inputChannels,
                     float* output, int numFrames, int outputChannels) {
    DenormalGuard denormals;
    // A clock read (no lock, no allocation): how long this callback takes, for cpuLoad().
    const auto started = std::chrono::steady_clock::now();
    Command command;
    int budget = commandBudget_ > 0 ? commandBudget_ : std::numeric_limits<int>::max();
    while ((budget > 0 || staging_) && commands_.pop(command)) {
        const bool heavy = command.type == CommandType::SetInsertType || command.type == CommandType::ClearInserts;
        budget -= heavy ? kHeavyCommand : 1;
        applyCommand(command);
    }

    if (meter_.state() == RoundTripMeter::State::Running) {
        meter_.process(input, numInputFrames, inputChannels, output, numFrames, outputChannels);
        gain_.snap(0.0f);
    } else {
        // Fixed-size scratch buffers: render long callbacks in chunks.
        for (int done = 0; done < numFrames;) {
            const int n = std::min(kMaxBlock, numFrames - done);
            const bool waiting = record_ && recordOnStart_;
            renderBlock(output + done * outputChannels, n, outputChannels);
            if (waiting && !recordOnStart_) recordSkip_ += done;  // started in this chunk
            done += n;
        }
        recordInput(input, numInputFrames, inputChannels, numFrames);
        // Microphone (tuner): whatever arrived this callback.
        if (input && inputChannels > 0) {
            for (int i = 0; i < numInputFrames; ++i) scopeWrite(micScope_, micScopeAt_, input[i * inputChannels]);
        }
    }

    // Output (analyser): mono, after the finite check below has cleaned it, one frame behind is fine.
    for (int i = 0; i < numFrames; ++i) {
        float m = 0.0f;
        for (int c = 0; c < outputChannels; ++c) m += output[i * outputChannels + c];
        scopeWrite(outScope_, outScopeAt_, std::isfinite(m) ? m / static_cast<float>(outputChannels) : 0.0f);
    }
    float blockPeak = 0.0f;
    bool finite = true;
    const int total = numFrames * outputChannels;
    for (int i = 0; i < total; ++i) {
        const float v = output[i];
        if (!std::isfinite(v)) {
            finite = false;
            output[i] = 0.0f;
            continue;
        }
        blockPeak = std::max(blockPeak, std::fabs(v));
    }
    if (!finite || mixer_.takeBlewUp()) nonFinite_.store(true, std::memory_order_relaxed);

    float previous = peak_.load(std::memory_order_relaxed);
    while (blockPeak > previous &&
           !peak_.compare_exchange_weak(previous, blockPeak, std::memory_order_acq_rel)) {
    }
    callbacks_.fetch_add(1, std::memory_order_relaxed);
    frames_.fetch_add(static_cast<uint64_t>(numFrames), std::memory_order_relaxed);
    if (numFrames > 0 && sampleRate_ > 0) {
        const float lasted = static_cast<float>(numFrames) / static_cast<float>(sampleRate_);
        const float took = std::chrono::duration<float>(std::chrono::steady_clock::now() - started).count();
        // About half a second of smoothing whatever the callback's size.
        const float k = std::min(1.0f, lasted / 0.5f);
        load_ += k * (std::min(took / lasted, 4.0f) - load_);
        cpuLoad_.store(load_, std::memory_order_relaxed);
    }
}

void Engine::finishRecording() {
    if (record_) record_->state.store(2, std::memory_order_release);
    record_ = nullptr;
    recordOnStart_ = false;
    recordSkip_ = -1;
}

void Engine::recordInput(const float* input, int numInputFrames, int inputChannels, int numFrames) {
    if (!record_ || recordOnStart_) return;
    const int64_t capacity = static_cast<int64_t>(record_->data.size());
    int64_t at = record_->written.load(std::memory_order_relaxed);
    float peak = 0.0f;
    if (input && inputChannels > 0) {
        // The microphone frames of this callback, from where the recording started (first channel).
        const int from = recordSkip_ > 0 ? std::min(recordSkip_, numInputFrames) : 0;
        for (int i = from; i < numInputFrames && at < capacity; ++i) {
            const float v = input[i * inputChannels];
            record_->data[static_cast<size_t>(at++)] = v;
            peak = std::max(peak, std::fabs(v));
        }
    } else {
        // No microphone this callback (the audio rerouted mid-take and it is being reopened):
        // silence for as long as the transport ran, so what is sung afterwards stays in time.
        const int from = recordSkip_ > 0 ? std::min(recordSkip_, numFrames) : 0;
        for (int i = from; i < numFrames && at < capacity; ++i) record_->data[static_cast<size_t>(at++)] = 0.0f;
    }
    recordSkip_ = 0;
    record_->peak.store(peak, std::memory_order_relaxed);
    record_->written.store(at, std::memory_order_release);
    if (at >= capacity) finishRecording();
}

}  // namespace ps
