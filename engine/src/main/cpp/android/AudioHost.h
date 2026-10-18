#pragma once

#include <oboe/Oboe.h>

#include <atomic>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include "Engine.h"
#include "SpscFloatRing.h"

namespace ps {

struct StreamInfo {
    int sampleRate = 0;
    int framesPerBurst = 0;
    int bufferSize = 0;
    int bufferCapacity = 0;
    bool exclusive = false;
    bool aaudio = false;
    bool mmap = false;
    bool lowLatency = false;
    int xruns = 0;
    double outputLatencyMs = -1.0;
    bool running = false;
};

// Input-stream callback: pushes microphone samples (first channel) into a lock-free ring
// that the output callback drains. Each stream keeps its own audio thread, which works on
// devices where reading the input from the output thread returns nothing.
class InputCallback : public oboe::AudioStreamDataCallback {
public:
    explicit InputCallback(SpscFloatRing& ring) : ring_(ring) {}
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override {
        ring_.write(static_cast<const float*>(audioData), static_cast<std::size_t>(numFrames),
                    static_cast<std::size_t>(stream->getChannelCount()));
        frames_.fetch_add(numFrames, std::memory_order_relaxed);
        return oboe::DataCallbackResult::Continue;
    }
    void reset() { frames_.store(0, std::memory_order_relaxed); }
    int64_t frames() const { return frames_.load(std::memory_order_relaxed); }

private:
    SpscFloatRing& ring_;
    std::atomic<int64_t> frames_{0};
};

// Owns the Oboe output stream (and the input stream during latency tests)
// and drives the platform-independent Engine from the audio callback.
class AudioHost : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    AudioHost();
    ~AudioHost() override;

    bool start();
    void stop();
    bool post(const Command& command);
    StreamInfo info();
    Engine& engine() { return engine_; }

    enum class LatencyStart : int { Ok = 0, MicUnavailable = 1, OutputFailed = 2, NotForeground = 3, MicSilent = 4 };

    // Opens output then microphone with the current latency configuration, waits until the
    // microphone really delivers, then launches the round-trip measurement.
    LatencyStart startLatencyTest();
    // True when a stream was cut (e.g. audio rerouting) during the last test.
    bool latencyTestInterrupted() const { return testInterrupted_.load(std::memory_order_acquire); }
    // Human-readable description of the last microphone attempt (for reports).
    std::string inputDescription();
    // Closes the input stream once the measurement is finished. When the test failed for lack
    // of microphone data, the next test uses the next configuration.
    void finishLatencyTest(bool succeededInput);

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    bool openOutputLocked(oboe::SharingMode sharing = oboe::SharingMode::Exclusive);
    bool openInputLocked();
    void closeLocked();
    void restoreOutputLocked();
    void describeLocked(const char* status);
    void advanceConfigLocked();

    std::mutex lock_;
    std::mutex postLock_;
    Engine engine_;
    std::shared_ptr<oboe::AudioStream> output_;
    std::shared_ptr<oboe::AudioStream> input_;
    std::vector<float> inputBuffer_;
    SpscFloatRing inputRing_;
    InputCallback inputCallback_;
    int config_ = 0;
    std::string inputDescription_ = "aucun";
    std::atomic<bool> drainInput_{false};
    std::atomic<bool> testInterrupted_{false};
    bool wantRunning_ = false;
    int xrunBase_ = 0;
    int lastXruns_ = 0;
    StreamInfo lastInfo_;
};

}  // namespace ps
