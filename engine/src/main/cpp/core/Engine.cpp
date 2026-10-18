#include "Engine.h"

#include "DenormalGuard.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {
constexpr double kTwoPi = 6.28318530717958647692;

// Safety stage until the master limiter (milestone 1.5): transparent below 0.8,
// rounds peaks above instead of hard clipping, never exceeds 1.
inline float softClip(float x) {
    const float a = std::fabs(x);
    if (a <= 0.8f) return x;
    const float over = (a - 0.8f) / 0.2f;
    const float shaped = 0.8f + 0.2f * (over / (1.0f + over));
    return x < 0.0f ? -shaped : shaped;
}
}  // namespace

Engine::Engine(int sampleRate) {
    frequency_.snap(440.0f);
    prepare(sampleRate);
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
    synth_.prepare(sampleRate);
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
            } else {
                sequencer_.stop();
                acid_.stop();
                currentStep_.store(-1, std::memory_order_relaxed);
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
        case CommandType::SetPatternSteps:
            patternSteps_ = std::clamp(command.a, 16, kMaxSteps);
            break;
        case CommandType::SetAcidStep:
            acid_.setStep(command.a, AcidStep::unpack(command.b));
            break;
        case CommandType::SetAcidLength:
            acid_.setLength(command.a);
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
            synth_.noteOn(command.a, command.value);
            break;
        case CommandType::SynthNoteOff:
            synth_.noteOff(command.a);
            break;
        case CommandType::SynthParam:
            if (command.a >= 0 && command.a < Synth::ParamCount) {
                synth_.setParam(static_cast<Synth::Param>(command.a), command.value);
            }
            break;
        case CommandType::SynthAllOff:
            synth_.allNotesOff();
            break;
        case CommandType::TriggerPad:
            if (command.a >= 0 && command.a < kLanes) {
                drums_.trigger(command.a, command.value, 0);
                laneTriggers_[static_cast<size_t>(command.a)].fetch_add(1, std::memory_order_relaxed);
            }
            break;
        case CommandType::SetDrumHit:
            if (command.a >= 0 && command.a < kLanes && command.b >= 0 && command.b < kMaxSteps) {
                pattern_[command.a][command.b] = std::clamp(command.value, 0.0f, 1.0f);
            }
            break;
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
    acid_.onStep(offset, step, sequencer_.samplesPerStep());
    const int patternStep = step % patternSteps_;
    currentStep_.store(patternStep, std::memory_order_relaxed);
    for (int lane = 0; lane < kLanes; ++lane) {
        if (pattern_[lane][patternStep] > 0.0f) {
            laneTriggers_[static_cast<size_t>(lane)].fetch_add(1, std::memory_order_relaxed);
            drums_.trigger(lane, pattern_[lane][patternStep], offset);
        }
    }
}

void Engine::renderBlock(float* output, int numFrames, int outputChannels) {
    float* mix = mix_.data();
    std::fill(mix, mix + numFrames, 0.0f);
    sequencer_.advance(numFrames, [this](int offset, int step) { onStep(offset, step); });
    metronome_.render(mix, numFrames);
    drums_.render(mix, numFrames);
    acid_.render(mix, numFrames);
    synth_.render(mix, numFrames);

    gain_.setTarget(playing_.load(std::memory_order_relaxed) ? volume_ : 0.0f);
    const double invRate = 1.0 / sampleRate_;
    for (int i = 0; i < numFrames; ++i) {
        const float sample = softClip(static_cast<float>(std::sin(phase_)) * gain_.next() + mix[i]);
        phase_ += kTwoPi * frequency_.next() * invRate;
        if (phase_ >= kTwoPi) phase_ -= kTwoPi;
        for (int c = 0; c < outputChannels; ++c) output[i * outputChannels + c] = sample;
    }
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
