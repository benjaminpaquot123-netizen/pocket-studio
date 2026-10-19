#pragma once

#include <cstdint>
#include <vector>

namespace ps {

// Changes the length of a sound without changing its pitch (and/or its pitch without changing
// its length), with Signalsmith Stretch. Not real-time safe: run on a worker thread.
// `in` is interleaved (1 or 2 channels); returns `outFrames` interleaved frames, or an empty
// vector if the input is too short to stretch (a few hundred milliseconds at least).
std::vector<float> timeStretch(const float* in, int64_t inFrames, int channels, int64_t outFrames,
                               float semitones, int sampleRate);

}  // namespace ps
