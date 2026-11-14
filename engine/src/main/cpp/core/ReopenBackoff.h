#pragma once

#include <algorithm>

namespace ps {

// The output stream's revival after a disconnect (headphones, Bluetooth, a reroute) whose quick
// retries all failed (a Bluetooth headset takes 1 to 3 s to connect, with several route changes):
// AudioHost keeps trying, off the audio thread, after these delays (audit 13 M3). Never giving up
// while the app wants sound; an output device appearing tries at once.
struct ReopenBackoff {
    static constexpr int kQuickAttempts = 5;  // in the error callback, kQuickDelayMs apart
    static constexpr int kQuickDelayMs = 200;
    // Then 1, 2, 4, 8 s, and every 8 s from there on.
    static int delayMs(int attempt) { return 1000 << std::clamp(attempt, 0, 3); }
};

}  // namespace ps
