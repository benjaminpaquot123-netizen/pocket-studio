#include "OfflineRenderer.h"

#include <algorithm>

#include "WavWriter.h"

namespace ps {

OfflineRenderer::OfflineRenderer(int sampleRate)
    : sampleRate_(sampleRate), engine_(std::make_unique<Engine>(sampleRate)) {}

void OfflineRenderer::post(const Command& command) {
    while (!engine_->post(command)) {
        // Queue full: let the engine apply what it holds (zero frames rendered).
        engine_->process(nullptr, 0, 0, block_, 0, 2);
    }
}

void OfflineRenderer::renderSilent(int frames) {
    for (int done = 0; done < frames; done += kBlock) {
        engine_->process(nullptr, 0, 0, block_, std::min(kBlock, frames - done), 2);
    }
}

OfflineRenderer::Result OfflineRenderer::renderToWav(const char* path, int64_t frames, int64_t tailFrames) {
    progress_.store(0.0f, std::memory_order_relaxed);
    // Let parameter smoothers reach the project's settings before the first beat.
    renderSilent(sampleRate_ / 10);
    post({CommandType::SetTransport, 1.0f, 0});

    WavWriter wav;
    if (!wav.open(path, sampleRate_, 2)) return Result::IoError;
    const int64_t total = frames + tailFrames;
    for (int64_t done = 0; done < total;) {
        if (cancelled_.load(std::memory_order_relaxed)) {
            wav.close();
            return Result::Cancelled;
        }
        // Stop exactly at the end of the loop, then render the tail.
        int n = static_cast<int>(std::min<int64_t>(kBlock, total - done));
        if (done < frames) n = static_cast<int>(std::min<int64_t>(n, frames - done));
        if (done == frames) post({CommandType::SetTransport, 0.0f, 0});
        engine_->process(nullptr, 0, 0, block_, n, 2);
        if (!wav.write(block_, n)) {
            wav.close();
            return Result::IoError;
        }
        done += n;
        progress_.store(static_cast<float>(done) / static_cast<float>(total), std::memory_order_relaxed);
    }
    return wav.close() ? Result::Ok : Result::IoError;
}

}  // namespace ps
