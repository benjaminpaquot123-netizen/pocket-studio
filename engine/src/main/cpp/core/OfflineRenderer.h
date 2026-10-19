#pragma once

#include <atomic>
#include <cstdint>
#include <functional>
#include <vector>
#include <memory>

#include "Engine.h"

namespace ps {

// Renders a private Engine (not the live one) faster than real time into a WAV file.
// Commands are posted to engine() exactly as the UI posts them to the live engine.
// Runs on a worker thread; progress() and cancel() may be called from any thread.
class OfflineRenderer {
public:
    enum class Result { Ok, IoError, Cancelled };

    explicit OfflineRenderer(int sampleRate = 48000);

    // Posts a command, draining the queue into the engine when it is full.
    void post(const Command& command);

    // Plays the transport for `frames`, then renders `tailFrames` stopped so
    // releases ring out. Output is 24-bit stereo.
    Result renderToWav(const char* path, int64_t frames, int64_t tailFrames);
    // Same, into memory (interleaved stereo). Empty when cancelled.
    std::vector<float> renderToBuffer(int64_t frames, int64_t tailFrames);

    float progress() const { return progress_.load(std::memory_order_relaxed); }
    void cancel() { cancelled_.store(true, std::memory_order_relaxed); }

private:
    static constexpr int kBlock = 512;
    void renderSilent(int frames);
    // Plays the song through `sink` block by block; the sink returns false to stop with an I/O error.
    Result render(int64_t frames, int64_t tailFrames, const std::function<bool(const float*, int)>& sink);

    int sampleRate_;
    std::unique_ptr<Engine> engine_;
    std::atomic<float> progress_{0.0f};
    std::atomic<bool> cancelled_{false};
    float block_[kBlock * 2];
};

}  // namespace ps
