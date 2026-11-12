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
        (void)started;
    }

    float smoothed() const { return published_.load(std::memory_order_relaxed); }
    float takeWorstBlock() { return smoothed(); }
    float takeWorstGap() { return smoothed(); }

private:
    float smoothed_ = 0.0f;  // audio thread
    std::atomic<float> published_{0.0f};
};

}  // namespace ps
