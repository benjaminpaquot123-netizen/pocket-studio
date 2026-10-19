#pragma once

#include <cstdint>
#include <functional>
#include <string>
#include <vector>

namespace ps {

// Song analysis for the Remix mode. Off the audio thread only (seconds of CPU for a song).

struct TempoResult {
    float bpm = 0.0f;         // 0 when nothing rhythmic was found
    float confidence = 0.0f;  // 0..1: share of beat intervals agreeing with bpm
    float firstBeat = 0.0f;   // seconds
};

struct KeyResult {
    int tonic = -1;            // pitch class 0 = C … 11 = B; -1 when silent
    bool minor = false;
    float confidence = 0.0f;   // correlation gap between the best and the second-best key
};

// Streams a WAV file (PCM 16/24/32-bit or 32-bit float, any channel count) as mono blocks.
// Returns false when the file is not a readable WAV. `sink(block, frames)`.
bool readWavMono(const std::string& path, int& sampleRate,
                 const std::function<void(const float*, int)>& sink);

// Beat tracking with aubio (GPL-3.0); the tempo is the median beat interval, folded into 70..180.
TempoResult detectTempo(const std::string& wavPath);
TempoResult detectTempo(const float* mono, int64_t frames, int sampleRate);

// Krumhansl–Kessler key profiles against a chromagram summed over every file (e.g. bass + other).
KeyResult detectKey(const std::vector<std::string>& wavPaths);
KeyResult detectKey(const float* mono, int64_t frames, int sampleRate);

}  // namespace ps
