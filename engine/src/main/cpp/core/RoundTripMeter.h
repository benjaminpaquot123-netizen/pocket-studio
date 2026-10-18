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

    // Why a measurement failed, so the UI can tell the user what to fix.
    enum class Failure : int { None = 0, NoInput = 1, SilentInput = 2, LowConfidence = 3 };

    struct Result {
        bool valid = false;
        double latencyMs = 0.0;
        int latencyFrames = 0;
        double confidence = 0.0;
        float inputPeak = 0.0f;   // loudest microphone sample during the test
        int inputFrames = 0;      // microphone frames actually received
        Failure failure = Failure::NoInput;
    };

    void prepare(int sampleRate);
    void start();   // audio thread
    void cancel();  // audio thread
    void process(const float* input, int numInputFrames, int inputChannels,
                 float* output, int numOutputFrames, int outputChannels);

    State state() const { return static_cast<State>(state_.load(std::memory_order_acquire)); }
    // Input frames are recorded back to back, whatever chunk sizes the microphone delivers;
    // the output side emits the burst on its own frame clock. Both start together.
    // Call analyze() once state() == Done, from a non-audio thread.
    Result analyze() const;

    const std::vector<float>& burst() const { return burst_; }
    int burstStartFrame() const { return burstStart_; }
    int recordLength() const { return static_cast<int>(record_.size()); }

    static constexpr float kMinConfidence = 6.0f;
    static constexpr float kSilenceThreshold = 1e-4f;

private:
    int sampleRate_ = 48000;
    int burstStart_ = 0;
    int position_ = 0;     // output frames emitted
    int recordPos_ = 0;    // input frames recorded
    float inputPeak_ = 0.0f;
    std::vector<float> burst_;
    std::vector<float> record_;
    std::atomic<int> state_{static_cast<int>(State::Idle)};
};

}  // namespace ps
