#include "RoundTripMeter.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {
constexpr double kPi = 3.14159265358979323846;
}

void RoundTripMeter::prepare(int sampleRate) {
    sampleRate_ = sampleRate;
    burstStart_ = sampleRate / 4;              // 250 ms of silence first
    const int burstLength = sampleRate / 24;   // ~42 ms of noise
    record_.assign(static_cast<size_t>(sampleRate + sampleRate / 4), 0.0f);
    burst_.resize(static_cast<size_t>(burstLength));
    uint32_t seed = 0x1234567u;
    for (int i = 0; i < burstLength; ++i) {
        seed = seed * 1664525u + 1013904223u;
        const float white = (static_cast<float>(seed >> 8) / 8388608.0f) - 1.0f;
        const float window = 0.5f - 0.5f * static_cast<float>(std::cos(2.0 * kPi * i / (burstLength - 1)));
        burst_[static_cast<size_t>(i)] = 0.6f * white * window;
    }
    state_.store(static_cast<int>(State::Idle), std::memory_order_release);
}

void RoundTripMeter::start() {
    position_ = 0;
    std::fill(record_.begin(), record_.end(), 0.0f);
    state_.store(static_cast<int>(State::Running), std::memory_order_release);
}

void RoundTripMeter::cancel() {
    state_.store(static_cast<int>(State::Idle), std::memory_order_release);
}

void RoundTripMeter::process(const float* input, int numInputFrames, int inputChannels,
                             float* output, int numOutputFrames, int outputChannels) {
    if (state() != State::Running) return;
    const int recordLength = static_cast<int>(record_.size());
    const int burstLength = static_cast<int>(burst_.size());
    for (int i = 0; i < numOutputFrames; ++i) {
        const int pos = position_ + i;
        float sample = 0.0f;
        const int burstIndex = pos - burstStart_;
        if (burstIndex >= 0 && burstIndex < burstLength) sample = burst_[static_cast<size_t>(burstIndex)];
        for (int c = 0; c < outputChannels; ++c) output[i * outputChannels + c] = sample;
        if (pos < recordLength) {
            record_[static_cast<size_t>(pos)] =
                (input != nullptr && i < numInputFrames) ? input[i * inputChannels] : 0.0f;
        }
    }
    position_ += numOutputFrames;
    if (position_ >= recordLength) state_.store(static_cast<int>(State::Done), std::memory_order_release);
}

RoundTripMeter::Result RoundTripMeter::analyze() const {
    Result result;
    const int recordLength = static_cast<int>(record_.size());
    const int burstLength = static_cast<int>(burst_.size());
    const int maxLag = recordLength - burstLength;
    if (maxLag <= burstStart_) return result;

    std::vector<double> correlation(static_cast<size_t>(maxLag - burstStart_), 0.0);
    double bestValue = 0.0;
    int bestLag = 0;
    for (int lag = burstStart_; lag < maxLag; ++lag) {
        double sum = 0.0;
        for (int i = 0; i < burstLength; ++i) {
            sum += static_cast<double>(record_[static_cast<size_t>(lag + i)]) * burst_[static_cast<size_t>(i)];
        }
        const double magnitude = std::fabs(sum);
        correlation[static_cast<size_t>(lag - burstStart_)] = magnitude;
        if (magnitude > bestValue) {
            bestValue = magnitude;
            bestLag = lag;
        }
    }
    double energy = 0.0;
    for (double v : correlation) energy += v * v;
    const double rms = std::sqrt(energy / static_cast<double>(correlation.size()));
    if (rms <= 0.0 || bestValue <= 0.0) return result;

    result.latencyFrames = bestLag - burstStart_;
    result.latencyMs = 1000.0 * result.latencyFrames / sampleRate_;
    result.confidence = bestValue / rms;
    result.valid = result.confidence >= kMinConfidence;
    return result;
}

}  // namespace ps
