#pragma once

#include <atomic>
#include <cstdint>
#include <vector>

namespace ps {

// Measures round-trip latency (speaker -> microphone) by emitting a windowed
// noise burst and locating it in the recorded input with cross-correlation.
// process() is real-time safe; prepare() and analyze() are not.
class RoundTripMeter {
public:
    enum class State : int { Idle = 0, Running = 1, Done = 2 };

    struct Result {
        bool valid = false;
        double latencyMs = 0.0;
        int latencyFrames = 0;
        double confidence = 0.0;
    };

    void prepare(int sampleRate);
    void start();   // audio thread
    void cancel();  // audio thread
    void process(const float* input, int numInputFrames, int inputChannels,
                 float* output, int numOutputFrames, int outputChannels);

    State state() const { return static_cast<State>(state_.load(std::memory_order_acquire)); }
    // Call once state() == Done, from a non-audio thread.
    Result analyze() const;

    const std::vector<float>& burst() const { return burst_; }
    int burstStartFrame() const { return burstStart_; }
    int recordLength() const { return static_cast<int>(record_.size()); }

    static constexpr float kMinConfidence = 6.0f;

private:
    int sampleRate_ = 48000;
    int burstStart_ = 0;
    int position_ = 0;
    std::vector<float> burst_;
    std::vector<float> record_;
    std::atomic<int> state_{static_cast<int>(State::Idle)};
};

}  // namespace ps
