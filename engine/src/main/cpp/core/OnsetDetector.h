#pragma once

#include <cstdint>
#include <vector>

namespace ps {

// Finds the attacks (drum hits, notes) in a sound (spectral flux + adaptive median threshold).
// Not real-time safe: run on a worker thread.
struct OnsetOptions {
    float sensitivity = 0.5f;     // 0 (only big hits) .. 1 (every small attack)
    float minGapSeconds = 0.05f;  // two attacks closer than this count as one
    int maxSlices = 16;
};

// Returns frame positions of the attacks in `mono` (always starting with 0), at most maxSlices.
std::vector<int64_t> detectOnsets(const float* mono, int64_t frames, int sampleRate, const OnsetOptions& options = {});

}  // namespace ps
