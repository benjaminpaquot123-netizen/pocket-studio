#pragma once

#include <atomic>
#include <cstdint>
#include <vector>

namespace ps {

// Immutable audio data (interleaved float, 1 or 2 channels, at the engine rate) shared by the
// live engine and export engines. Reference counted; the audio thread only ever moves
// references and hands a sample whose count reaches zero back to a non-audio thread to free.
class Sample {
public:
    // Not real-time safe (allocates, resamples). Returns nullptr on bad input.
    // Starts with one reference, owned by the caller.
    static Sample* create(const float* interleaved, int64_t frames, int channels, int sourceRate, int targetRate);
    // The same, kept as 16-bit integers (half the memory): the multizone instruments' zones
    // (MultiSampler), recorded at 16 bits anyway. Such a sample has no float data(): only
    // MultiSampler reads it (the JNI refuses it anywhere else).
    static Sample* createCompact(const float* interleaved, int64_t frames, int channels, int sourceRate, int targetRate);

    int64_t frames() const { return frames_; }
    int channels() const { return channels_; }
    // nullptr for a compact sample (pcm16()).
    const float* data() const { return compact() ? nullptr : data_.data(); }
    bool compact() const { return !pcm16_.empty(); }
    const int16_t* pcm16() const { return pcm16_.empty() ? nullptr : pcm16_.data(); }
    // Bytes of audio held (the instruments' memory budget).
    int64_t bytes() const { return static_cast<int64_t>(data_.size() * sizeof(float) + pcm16_.size() * sizeof(int16_t)); }

    void retain() { refs_.fetch_add(1, std::memory_order_relaxed); }
    // Returns true when this was the last reference: the caller must then delete it
    // (or, on the audio thread, hand it to a thread that may).
    bool release() { return refs_.fetch_sub(1, std::memory_order_acq_rel) == 1; }
    int refCount() const { return refs_.load(std::memory_order_acquire); }

private:
    Sample() = default;
    std::vector<float> data_;
    std::vector<int16_t> pcm16_;
    int64_t frames_ = 0;
    int channels_ = 1;
    std::atomic<int> refs_{1};
};

}  // namespace ps
