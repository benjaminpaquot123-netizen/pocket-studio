#include "OfflineRenderer.h"

#include <algorithm>
#include <cmath>
#include <cstdint>

#include "WavWriter.h"

namespace ps {

OfflineRenderer::OfflineRenderer(int sampleRate)
    : sampleRate_(sampleRate), engine_(std::make_unique<Engine>(sampleRate)) {}

OfflineRenderer::~OfflineRenderer() {
    // Scheduled commands never reached (cancelled render) may still own references.
    for (size_t i = nextScheduled_; i < scheduled_.size(); ++i) {
        const Command& c = scheduled_[i].second;
        if (c.sample && c.sample->release()) delete c.sample;
        SoundFontPlayer::close(c.font);
        if (c.stems && c.stems->release()) delete c.stems;
    }
}

void OfflineRenderer::post(const Command& command) {
    if (scheduleAt_ >= 0) {
        // Keep the order: a later frame never runs before an earlier one already scheduled.
        const int64_t at = scheduled_.empty() ? scheduleAt_ : std::max(scheduleAt_, scheduled_.back().first);
        scheduled_.emplace_back(at, command);
        return;
    }
    postNow(command);
}

void OfflineRenderer::postNow(const Command& command) {
    while (!engine_->post(command)) {
        // Queue full: let the engine apply what it holds (zero frames rendered).
        engine_->process(nullptr, 0, 0, block_, 0, 2);
    }
}

void OfflineRenderer::postDue(int64_t frame) {
    while (nextScheduled_ < scheduled_.size() && scheduled_[nextScheduled_].first <= frame) {
        postNow(scheduled_[nextScheduled_].second);
        ++nextScheduled_;
    }
}

int64_t OfflineRenderer::nextDue() const {
    return nextScheduled_ < scheduled_.size() ? scheduled_[nextScheduled_].first : INT64_MAX;
}

void OfflineRenderer::renderSilent(int frames) {
    for (int done = 0; done < frames; done += kBlock) {
        engine_->process(nullptr, 0, 0, block_, std::min(kBlock, frames - done), 2);
    }
}

OfflineRenderer::Result OfflineRenderer::render(int64_t frames, int64_t tailFrames,
                                                const std::function<bool(const float*, int)>& sink) {
    progress_.store(0.0f, std::memory_order_relaxed);
    // Let parameter smoothers reach the project's settings before the first beat.
    renderSilent(sampleRate_ / 10);
    engine_->mixer().settle();
    postNow({CommandType::SetTransport, 1.0f, 0});

    const int64_t total = frames + tailFrames;
    // The tail's last half second fades out: a long reverb is not cut dead at the file's end (with
    // the automatic tail, only one still ringing at the cap gets there).
    const int64_t fade = std::min<int64_t>(tailFrames, sampleRate_ / 2);
    const int64_t fadeOut = std::min<int64_t>(fadeOut_, total);
    int64_t quiet = 0;  // frames in a row under -90 dBFS, in the tail
    for (int64_t done = 0; done < total;) {
        if (cancelled_.load(std::memory_order_relaxed)) return Result::Cancelled;
        // Stop exactly at the end of the loop, then render the tail.
        int n = static_cast<int>(std::min<int64_t>(kBlock, total - done));
        if (done < frames) n = static_cast<int>(std::min<int64_t>(n, frames - done));
        // Scheduled commands land on their exact frame: the block stops there.
        postDue(done);
        n = static_cast<int>(std::min<int64_t>(n, std::max<int64_t>(1, nextDue() - done)));
        if (done == frames) postNow({CommandType::SetTransport, 0.0f, 0});
        engine_->process(nullptr, 0, 0, block_, n, 2);
        // A requested fade-out takes the end of the file down to silence along a squared curve
        // (closer to how loudness is heard than a straight line). Gains never exceed 1: no clipping.
        for (int i = 0; i < n && (fade > 0 || fadeOut > 0); ++i) {
            const int64_t left = total - (done + i);
            if (left >= fade && left >= fadeOut) continue;
            float g = 1.0f;
            if (left < fade) g = static_cast<float>(left) / static_cast<float>(fade);
            if (left < fadeOut) {
                const float x = static_cast<float>(left) / static_cast<float>(fadeOut);
                g = std::min(g, x * x);
            }
            block_[2 * i] *= g;
            block_[2 * i + 1] *= g;
        }
        // The automatic tail ends on the frame where the output has been quiet long enough.
        int keep = n;
        bool ended = false;
        if (autoTail_ && done + n > frames) {
            auto& mixer = engine_->mixer();
            const int64_t window = sampleRate_ / 5 + (mixer.echoInUse() ? static_cast<int64_t>(mixer.echoFrames()) : 0);
            for (int i = static_cast<int>(std::max<int64_t>(0, frames - done)); i < n; ++i) {
                const bool silent = std::fabs(block_[2 * i]) < kSilence && std::fabs(block_[2 * i + 1]) < kSilence;
                quiet = silent ? quiet + 1 : 0;
                if (quiet >= window) {
                    keep = i + 1;
                    ended = true;
                    break;
                }
            }
        }
        if (!sink(block_, keep)) return Result::IoError;
        done += keep;
        if (ended) break;
        progress_.store(static_cast<float>(done) / static_cast<float>(total), std::memory_order_relaxed);
    }
    progress_.store(1.0f, std::memory_order_relaxed);
    return Result::Ok;
}

OfflineRenderer::Result OfflineRenderer::renderToWav(const char* path, int64_t frames, int64_t tailFrames) {
    WavWriter wav;
    if (!wav.open(path, sampleRate_, 2, floatWav_)) return Result::IoError;
    const Result r = render(frames, tailFrames, [&wav](const float* b, int n) { return wav.write(b, n); });
    const bool closed = wav.close();
    if (r != Result::Ok) return r;
    return closed ? Result::Ok : Result::IoError;
}

std::vector<float> OfflineRenderer::renderToBuffer(int64_t frames, int64_t tailFrames) {
    std::vector<float> out;
    out.reserve(static_cast<size_t>((frames + tailFrames) * 2));
    const Result r = render(frames, tailFrames, [&out](const float* b, int n) {
        out.insert(out.end(), b, b + n * 2);
        return true;
    });
    if (r != Result::Ok) out.clear();
    return out;
}

}  // namespace ps
