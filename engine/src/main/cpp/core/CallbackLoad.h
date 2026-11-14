#pragma once

#include <algorithm>
#include <atomic>

namespace ps {

// How the audio thread keeps up, callback by callback (docs/audits/ultra/tour-5-hardtechno-rapport.md).
// add() runs on the audio thread at the end of each callback: plain arithmetic and relaxed atomics,
// no allocation, no lock. The readings are taken from any other thread.
//  - smoothed(): the time a callback takes over the time its frames last, smoothed over about half a
//    second (the app's « charge »). One callback 20 times too slow moves it by a few percent only.
//  - takeWorstBlock(): the slowest callback since the last take, the same ratio, neither smoothed nor
//    capped (the « pire bloc »): a stall inside the callback.
//  - takeWorstGap(): the longest wait between the starts of two callbacks since the last take, over
//    the duration of the first one's frames: 1 is on time; a callback started late (the thread kept
//    off its core) shows here and not in the worst block.
class CallbackLoad {
public:
    // A gap longer than this is the stream stopped and opened again, not a callback late.
    static constexpr double kRestartGapSeconds = 1.0;

    // took: the callback's time (s); lasted: its frames' duration (s); started: when it began (s, any
    // steady clock).
    void add(float took, float lasted, double started) {
        if (!(lasted > 0.0f)) return;
        const float ratio = took / lasted;
        // About half a second of smoothing whatever the callback's size (capped as it always was).
        const float k = std::min(1.0f, lasted / 0.5f);
        smoothed_ += k * (std::min(ratio, 4.0f) - smoothed_);
        published_.store(smoothed_, std::memory_order_relaxed);
        raise(worstBlock_, ratio);
        if (lastLasted_ > 0.0f) {
            const double gap = started - lastStarted_;
            if (gap >= 0.0 && gap < kRestartGapSeconds) raise(worstGap_, static_cast<float>(gap / lastLasted_));
        }
        lastStarted_ = started;
        lastLasted_ = lasted;
    }

    float smoothed() const { return published_.load(std::memory_order_relaxed); }
    // One reader (the UI thread's health poll): each take starts a new window (0: no callback since).
    float takeWorstBlock() { return worstBlock_.exchange(0.0f, std::memory_order_relaxed); }
    float takeWorstGap() { return worstGap_.exchange(0.0f, std::memory_order_relaxed); }

private:
    // The largest of the window: the audio thread raises it, a take zeroes it (no lost maximum).
    static void raise(std::atomic<float>& worst, float v) {
        if (!(v >= 0.0f)) return;  // NaN: nothing to keep
        float seen = worst.load(std::memory_order_relaxed);
        while (v > seen && !worst.compare_exchange_weak(seen, v, std::memory_order_relaxed)) {
        }
    }

    float smoothed_ = 0.0f;  // audio thread
    double lastStarted_ = 0.0;
    float lastLasted_ = 0.0f;  // 0: no callback yet
    std::atomic<float> published_{0.0f};
    std::atomic<float> worstBlock_{0.0f};
    std::atomic<float> worstGap_{0.0f};
};

}  // namespace ps
