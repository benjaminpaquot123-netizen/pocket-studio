#pragma once

#include <algorithm>
#include <atomic>
#include <cstddef>
#include <vector>

namespace ps {

// Wait-free single-producer / single-consumer ring of floats with bulk read/write.
// Used to hand microphone samples from the input callback to the output callback.
// Storage is allocated once in the constructor; read/write never allocate.
class SpscFloatRing {
public:
    explicit SpscFloatRing(std::size_t capacityPowerOfTwo) : buffer_(capacityPowerOfTwo), mask_(capacityPowerOfTwo - 1) {}

    // Producer. Returns how many samples were stored (drops the rest when full).
    std::size_t write(const float* data, std::size_t count, std::size_t stride = 1) {
        const auto head = head_.load(std::memory_order_relaxed);
        const auto tail = tail_.load(std::memory_order_acquire);
        const std::size_t n = std::min(count, buffer_.size() - (head - tail));
        for (std::size_t i = 0; i < n; ++i) buffer_[(head + i) & mask_] = data[i * stride];
        head_.store(head + n, std::memory_order_release);
        return n;
    }

    // Consumer. Returns how many samples were read.
    std::size_t read(float* out, std::size_t maxCount) {
        const auto tail = tail_.load(std::memory_order_relaxed);
        const auto head = head_.load(std::memory_order_acquire);
        const std::size_t n = std::min(maxCount, head - tail);
        for (std::size_t i = 0; i < n; ++i) out[i] = buffer_[(tail + i) & mask_];
        tail_.store(tail + n, std::memory_order_release);
        return n;
    }

    // Consumer. Drops everything currently buffered.
    void discard() { tail_.store(head_.load(std::memory_order_acquire), std::memory_order_release); }

    std::size_t available() const {
        return head_.load(std::memory_order_acquire) - tail_.load(std::memory_order_acquire);
    }

private:
    std::vector<float> buffer_;
    std::size_t mask_;
    alignas(64) std::atomic<std::size_t> head_{0};
    alignas(64) std::atomic<std::size_t> tail_{0};
};

}  // namespace ps
