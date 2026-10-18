#pragma once

#include <oboe/Oboe.h>

#include <atomic>
#include <memory>
#include <mutex>
#include <vector>

#include "Engine.h"

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

    // Re-opens streams in full duplex and launches the round-trip measurement.
    bool startLatencyTest();
    // Closes the input stream once the measurement is finished.
    void finishLatencyTest();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    bool openOutputLocked();
    bool openInputLocked();
    void closeLocked();

    std::mutex lock_;
    std::mutex postLock_;
    Engine engine_;
    std::shared_ptr<oboe::AudioStream> output_;
    std::shared_ptr<oboe::AudioStream> input_;
    std::vector<float> inputBuffer_;
    std::atomic<bool> drainInput_{false};
    bool wantRunning_ = false;
    int xrunBase_ = 0;
    int lastXruns_ = 0;
};

}  // namespace ps
