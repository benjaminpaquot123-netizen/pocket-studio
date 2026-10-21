#include "Engine.h"

#include "DenormalGuard.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {
constexpr double kTwoPi = 6.28318530717958647692;

}  // namespace

Engine::Engine(int sampleRate) {
    frequency_.snap(440.0f);
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
            frequency_.setTarget(std::clamp(command.value, kMinFrequency, kMaxFrequency));
            break;
        case CommandType::SetGain:
            volume_ = std::clamp(command.value, 0.0f, 1.0f);
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
                audio_.songJump();
                lastLocal_ = -1;
                countInStep_ = 0;
                playPosition_.store(-1.0, std::memory_order_relaxed);
            } else {
                stopTransport();
            }
            break;
        case CommandType::SetTempo:
            sequencer_.setTempo(command.value);
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
        case CommandType::ArmSceneSwap:
            swapAt_ = command.a < 0 ? -1 : command.a;
            if (command.a < 0) {
                // The cancelled scene's loops and effects never apply later.
                staged_.audioLoop.fill(-1);
                staged_.insertOn.fill(-1);
            }
            songRestart_ = command.b == 1;  // the waiting scene starts the song over (it loops)
            songRestartAt_ = command.b == 1 && std::isfinite(command.value) ? std::max(0, static_cast<int>(std::lround(command.value))) : 0;
            stopAtSwap_ = command.b == 2;   // the song ends there: the transport stops instead
            leaveSong_ = command.b == 3;    // the waiting scene plays on its own, outside the song
            if (command.a < 0 && command.b == 3) leaveSongNow();
            staging_ = false;  // arming ends a staging burst even if its "off" was dropped
            break;
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
                lane.songBars = 0;
            }
            break;
        case CommandType::SetAutomationPoint:
            if (command.a >= 0 && command.a < Automation::kLanes && command.b >= 0 && command.b < Automation::kSteps) {
                auto& lane = automation_.lanes[command.a];
                const float v = command.value < 0.0f ? -1.0f : std::min(command.value, 1.0f);
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
        case CommandType::SetAutomationMode:
            if (command.a >= 0 && command.a < Automation::kLanes) {
                auto& lane = automation_.lanes[command.a];
                lane.mode = std::clamp(command.b, 0, 2);
                lane.songBars = std::clamp(static_cast<int>(command.value), 0, Automation::kSongBars);
            }
            break;
        case CommandType::SetAutomationSong:
            if (command.a >= 0 && command.a < Automation::kLanes && command.b >= 0 && command.b < Automation::kSongBars) {
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
    acid_.stop();
    audio_.stop();
    if (record_ && !recordOnStart_) finishRecording();
    sampler_.releaseLoops();
    for (auto& t : tracks_) {
        t.notes.stop(t);
        // Belt and braces: a note-off dropped by a full queue must not leave a note hanging.
        t.allNotesOff();
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
    // A waiting scene takes over on its step (or the next bar), before anything of this step plays.
    if (swapAt_ >= 0) {
        const int local = step - sceneStart_;
        // Always on a bar line: a swap armed late waits for the next bar instead of cutting one.
        const bool onBar = local > 0 && local % bar == 0;
        if (onBar && !stopAtSwap_ && (swapAt_ == 0 || local >= swapAt_)) swapScene(step, offset);
    }
    // Patterns count from the start of their scene; the remix stems follow the transport.
    const int local = step - sceneStart_;
    acid_.onStep(offset, local, sequencer_.samplesPerStep());
    // Outside the song, no song position: song clips and song curves wait for the song.
    const int songStep = inSong_ ? step - songStart_ : -1;
    audio_.onStep(offset, local, songStep, sequencer_.samplesPerStep(), sequencer_.swing(), songStart_);
    for (auto& t : tracks_) {
        if (t.on) t.notes.onStep(offset, local, sequencer_.samplesPerStep(), sequencer_.swing());
        t.arpSync(offset, step, sequencer_.samplesPerStep());
    }
    const int patternStep = local % patternSteps_;
    songStep_.store(songStep, std::memory_order_relaxed);
    applyAutomation(patternStep, songStep, offset);
    // Sidechain pumps follow the kick lane (pad 1) or every quarter note.
    mixer_.sidechainEvent(offset, pattern_[0][patternStep] > 0.0f, onBeat);
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
            // A roll: the other hits share the step evenly, each a little softer.
            const int n = repeats_[lane][patternStep];
            const double every = sequencer_.samplesPerStep() / std::max(1, n);
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
            // From the transport start: bar by bar, sliding from one bar's value to the next.
            if (lane.songBars <= 0 || songStep < 0) continue;
            const int b = std::min(songStep / bar, lane.songBars - 1);
            const float a = lane.song[static_cast<size_t>(b)];
            const float c = lane.song[static_cast<size_t>(std::min(b + 1, lane.songBars - 1))];
            const float t = songStep / bar >= lane.songBars - 1 ? 0.0f : static_cast<float>(songStep % bar) / static_cast<float>(bar);
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
    while (commands_.pop(command)) applyCommand(command);

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
