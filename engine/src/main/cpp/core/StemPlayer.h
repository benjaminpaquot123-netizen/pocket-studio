#pragma once

#include <array>
#include <atomic>
#include <cstdint>
#include <string>
#include <vector>

#include "Smoother.h"
#include "SpscQueue.h"

namespace ps {

// The four stems of a separated song (drums, bass, other, vocals) as 16-bit interleaved stereo
// at the engine rate: 0.77 MB per second of song for all four at 48 kHz.
// Immutable and reference counted like Sample.
class StemBundle {
public:
    static constexpr int kStems = 4;

    // Not real-time safe: reads the WAVs (any rate, float or PCM), resamples, converts.
    // nullptr when a file is missing or unreadable. Starts with one reference.
    static StemBundle* load(const std::vector<std::string>& wavPaths, int targetRate);
    // Tests: interleaved stereo float at the target rate, one vector per stem, same length.
    static StemBundle* fromFloat(const std::vector<std::vector<float>>& stereo);

    int64_t frames() const { return frames_; }
    const int16_t* stem(int i) const { return data_[static_cast<size_t>(i)].data(); }
    // A region of one stem as interleaved stereo float (for a pad). Not real-time safe.
    std::vector<float> extract(int stem, int64_t start, int64_t count) const;

    void retain() { refs_.fetch_add(1, std::memory_order_relaxed); }
    bool release() { return refs_.fetch_sub(1, std::memory_order_acq_rel) == 1; }
    int refCount() const { return refs_.load(std::memory_order_acquire); }

private:
    StemBundle() = default;
    std::array<std::vector<int16_t>, kStems> data_;
    int64_t frames_ = 0;
    std::atomic<int> refs_{1};
};

// Plays a StemBundle in step with the transport: starts on step 0, from `startFrame` of the
// song, each stem with its own gain / mute / solo, summed to stereo. Real-time safe.
class StemPlayer {
public:
    enum Param { Gain = 0, Mute = 1, Solo = 2 };

    ~StemPlayer();
    void prepare(int sampleRate);
    // Takes over one reference (nullptr = no song). The previous bundle is retired.
    void install(StemBundle* bundle);
    void setParam(int stem, int param, float value);
    void setStartFrame(int64_t frame) { startFrame_ = frame < 0 ? 0 : frame; }

    // Starts `offset` frames into the next render() call.
    void start(int offset);
    // Short fade then silence.
    void stop();
    void render(float* left, float* right, int numFrames);

    bool loaded() const { return bundle_ != nullptr; }
    bool playing() const { return playing_; }
    // Song position in frames (UI). -1 when stopped.
    int64_t position() const { return position_.load(std::memory_order_relaxed); }

    // Frees retired bundles. Not on the audio thread.
    void collectGarbage();

private:
    void retire(StemBundle* b);
    void updateTargets();

    StemBundle* bundle_ = nullptr;
    SpscQueue<StemBundle*, 16> retired_;
    std::array<float, StemBundle::kStems> gain_{0.8f, 0.8f, 0.8f, 0.8f};
    std::array<bool, StemBundle::kStems> mute_{};
    std::array<bool, StemBundle::kStems> solo_{};
    std::array<Smoother, StemBundle::kStems> smooth_{};
    int64_t startFrame_ = 0;
    int64_t pos_ = 0;
    int pendingStart_ = -1;
    bool playing_ = false;
    int fadeLeft_ = 0;
    int fadeLength_ = 240;
    std::atomic<int64_t> position_{-1};
};

}  // namespace ps
