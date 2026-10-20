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
    }
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
                lastLocal_ = -1;
                playPosition_.store(-1.0, std::memory_order_relaxed);
            } else {
                swapAt_ = -1;
                staging_ = false;  // a lost "staging off" must never trap later edits
                sequencer_.stop();
                rollCount_ = 0;
                stems_.stop();
                acid_.stop();
                sampler_.releaseLoops();
                for (auto& t : tracks_) {
                    t.notes.stop(t);
                    // Belt and braces: a note-off dropped by a full queue must not leave a note hanging.
                    t.allNotesOff();
                }
                currentStep_.store(-1, std::memory_order_relaxed);
                absoluteStep_.store(-1, std::memory_order_relaxed);
                playPosition_.store(-1.0, std::memory_order_relaxed);
                lastLocal_ = -1;
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
            if (staging_) staged_.drumSteps = std::clamp(command.a, 16, kMaxSteps);
            else patternSteps_ = std::clamp(command.a, 16, kMaxSteps);
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
            tracks_[static_cast<size_t>(trackOf(command.a))].setUsePlaits(trackArg(command.a) == 1);
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
            tracks_[static_cast<size_t>(trackOf(command.a))].noteOn(trackArg(command.a) & 0x7F, command.value, 0);
            break;
        case CommandType::SynthNoteOff:
            tracks_[static_cast<size_t>(trackOf(command.a))].noteOff(trackArg(command.a) & 0x7F, 0);
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
        case CommandType::SetInsertParam:
            mixer_.setInsertParam(command.a / Mixer::kInserts, command.a % Mixer::kInserts, command.b, command.value);
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
                lane.values.fill(-1.0f);
            }
            break;
        case CommandType::SetAutomationPoint:
            if (command.a >= 0 && command.a < Automation::kLanes && command.b >= 0 && command.b < Automation::kSteps) {
                automation_.lanes[command.a].values[static_cast<size_t>(command.b)] = command.value < 0.0f ? -1.0f : std::min(command.value, 1.0f);
            }
            break;
        case CommandType::SetStemStart:
            stems_.setStartFrame(command.a);
            break;
        case CommandType::SetInstrument: {
            auto& t = tracks_[static_cast<size_t>(trackOf(command.a))];
            t.synth.allNotesOff();
            t.instrument.install(command.font, trackArg(command.a));  // takes the instance
            break;
        }
        case CommandType::TriggerPad:
            if (command.a >= 0 && command.a < kLanes) {
                if (sampler_.has(command.a)) sampler_.trigger(command.a, command.value, 0);
                else drums_.trigger(command.a, command.value, 0);
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
            }
            break;
        }
    }
}

void Engine::onStep(int offset, int step) {
    const int beatStep = ((step % Sequencer::kStepsPerBar) + Sequencer::kStepsPerBar) % Sequencer::kStepsPerBar;
    const bool onBeat = beatStep % Sequencer::kStepsPerBeat == 0;
    // Clicks during the count-in always; afterwards only with the metronome on.
    if (onBeat && (step < 0 || metronomeOn_)) {
        metronome_.trigger(beatStep == 0, offset);
        metronomeClicks_.fetch_add(1, std::memory_order_relaxed);
    }
    if (step < 0) return;
    if (step == 0) stems_.start(offset);
    if (onBeat) clips_.syncBeat(offset, step / Sequencer::kStepsPerBeat);
    // A waiting scene takes over on its step (or the next bar), before anything of this step plays.
    if (swapAt_ >= 0) {
        const int local = step - sceneStart_;
        // Always on a bar line: a swap armed late waits for the next bar instead of cutting one.
        const bool onBar = local > 0 && local % Sequencer::kStepsPerBar == 0;
        if (onBar && (swapAt_ == 0 || local >= swapAt_)) swapScene(step);
    }
    // Patterns count from the start of their scene; the remix stems follow the transport.
    const int local = step - sceneStart_;
    acid_.onStep(offset, local, sequencer_.samplesPerStep());
    for (auto& t : tracks_) {
        if (t.on) t.notes.onStep(offset, local, sequencer_.samplesPerStep());
    }
    const int patternStep = local % patternSteps_;
    applyAutomation(patternStep);
    // Sidechain pumps follow the kick lane (pad 1) or every quarter note.
    mixer_.sidechainEvent(offset, pattern_[0][patternStep] > 0.0f, onBeat);
    currentStep_.store(patternStep, std::memory_order_relaxed);
    absoluteStep_.store(local, std::memory_order_relaxed);
    lastLocal_ = local;
    stepOffset_ = offset;
    for (int lane = 0; lane < kLanes; ++lane) {
        const float v = pattern_[lane][patternStep];
        if (v > 0.0f) {
            hitLane(lane, v, offset);
            // A roll: the other hits share the step evenly, each a little softer.
            const int n = repeats_[lane][patternStep];
            const double every = sequencer_.samplesPerStep() / std::max(1, n);
            for (int k = 1; k < n && rollCount_ < static_cast<int>(rolls_.size()); ++k) {
                rolls_[static_cast<size_t>(rollCount_++)] = {lane, v * std::pow(0.85f, static_cast<float>(k)), offset + static_cast<int64_t>(k * every)};
            }
        }
    }
}

void Engine::hitLane(int lane, float velocity, int offset) {
    laneTriggers_[static_cast<size_t>(lane)].fetch_add(1, std::memory_order_relaxed);
    if (sampler_.has(lane)) sampler_.trigger(lane, velocity, offset);
    else drums_.trigger(lane, velocity, offset);
}

void Engine::playRolls(int numFrames) {
    int kept = 0;
    for (int i = 0; i < rollCount_; ++i) {
        PendingHit h = rolls_[static_cast<size_t>(i)];
        if (h.at < numFrames) {
            hitLane(h.lane, h.velocity, static_cast<int>(h.at));
        } else {
            h.at -= numFrames;
            rolls_[static_cast<size_t>(kept++)] = h;
        }
    }
    rollCount_ = kept;
}

void Engine::swapScene(int step) {
    std::copy(&staged_.drums[0][0], &staged_.drums[0][0] + kLanes * kMaxSteps, &pattern_[0][0]);
    std::copy(&staged_.repeats[0][0], &staged_.repeats[0][0] + kLanes * kMaxSteps, &repeats_[0][0]);
    patternSteps_ = staged_.drumSteps;
    for (int i = 0; i < AcidTrack::kMaxSteps; ++i) acid_.setStep(i, staged_.acid[static_cast<size_t>(i)]);
    acid_.setLength(staged_.acidLength);
    for (auto& t : tracks_) t.swapIn();
    sceneStart_ = step;
    swapAt_ = -1;
    sceneSwaps_.fetch_add(1, std::memory_order_relaxed);
}

void Engine::applyAutomation(int patternStep) {
    if (patternStep < 0 || patternStep >= Automation::kSteps) return;
    for (const auto& lane : automation_.lanes) {
        const float v = lane.values[static_cast<size_t>(patternStep)];
        if (lane.target < 0 || v < 0.0f) continue;
        const int kind = lane.target >> 16, ch = (lane.target >> 12) & 0xF, slot = (lane.target >> 8) & 0xF, param = lane.target & 0xFF;
        switch (kind) {
            case Automation::Volume: mixer_.setVolume(ch, v); break;
            case Automation::Pan: mixer_.setPan(ch, v * 2.0f - 1.0f); break;
            case Automation::Send: mixer_.setSend(ch, slot, v); break;
            case Automation::InsertParam: mixer_.setInsertParam(ch, slot, param, v); break;
            case Automation::BusParam: mixer_.setBusParam(slot, param, v); break;
            default: break;
        }
    }
}

void Engine::renderBlock(float* output, int numFrames, int outputChannels) {
    float* direct = mix_.data();  // metronome + test tone: straight to the master
    float* drums = drumsBuf_.data();
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
    if (lastLocal_ >= 0 && sequencer_.running()) {
        sinceStep_ = stepOffset_ >= 0 ? static_cast<double>(numFrames - stepOffset_) : sinceStep_ + numFrames;
        playPosition_.store(lastLocal_ + sinceStep_ / sequencer_.samplesPerStep(), std::memory_order_relaxed);
    }
    for (auto& t : tracks_) {
        if (t.on) t.notes.advance(numFrames, t);
    }
    metronome_.render(direct, numFrames);
    drums_.render(drums, numFrames);
    sampler_.render(drums, numFrames);
    acid_.render(acid, numFrames);
    const float* channels[Mixer::kChannels] = {drums, acid, nullptr, stemsL};
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
    const float* rights[Mixer::kChannels] = {nullptr, nullptr, nullptr, stemsR};
    mixer_.process(channels, rights, direct, output, numFrames, outputChannels);
}

void Engine::process(const float* input, int numInputFrames, int inputChannels,
                     float* output, int numFrames, int outputChannels) {
    DenormalGuard denormals;
    Command command;
    while (commands_.pop(command)) applyCommand(command);

    if (meter_.state() == RoundTripMeter::State::Running) {
        meter_.process(input, numInputFrames, inputChannels, output, numFrames, outputChannels);
        gain_.snap(0.0f);
    } else {
        // Fixed-size scratch buffers: render long callbacks in chunks.
        for (int done = 0; done < numFrames;) {
            const int n = std::min(kMaxBlock, numFrames - done);
            renderBlock(output + done * outputChannels, n, outputChannels);
            done += n;
        }
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
    if (!finite) nonFinite_.store(true, std::memory_order_relaxed);

    float previous = peak_.load(std::memory_order_relaxed);
    while (blockPeak > previous &&
           !peak_.compare_exchange_weak(previous, blockPeak, std::memory_order_acq_rel)) {
    }
    callbacks_.fetch_add(1, std::memory_order_relaxed);
    frames_.fetch_add(static_cast<uint64_t>(numFrames), std::memory_order_relaxed);
}

}  // namespace ps
