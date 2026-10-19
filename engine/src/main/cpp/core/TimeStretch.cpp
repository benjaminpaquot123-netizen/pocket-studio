#include "TimeStretch.h"

#include <algorithm>
#include <cmath>

#include "signalsmith-stretch.h"

namespace ps {

std::vector<float> timeStretch(const float* in, int64_t inFrames, int channels, int64_t outFrames,
                               float semitones, int sampleRate) {
    if (!in || inFrames <= 0 || outFrames <= 0 || channels < 1 || channels > 2 || sampleRate <= 0) return {};
    // Fixed seed: the same input always gives the same output (tests, undo/redo).
    signalsmith::stretch::SignalsmithStretch<float> stretch(12345);
    stretch.presetDefault(channels, static_cast<float>(sampleRate));
    stretch.setTransposeSemitones(std::clamp(semitones, -24.0f, 24.0f));

    std::vector<std::vector<float>> input(static_cast<size_t>(channels), std::vector<float>(static_cast<size_t>(inFrames)));
    for (int64_t i = 0; i < inFrames; ++i) {
        for (int c = 0; c < channels; ++c) input[size_t(c)][size_t(i)] = in[i * channels + c];
    }
    std::vector<std::vector<float>> output(static_cast<size_t>(channels), std::vector<float>(static_cast<size_t>(outFrames)));
    if (!stretch.exact(input, static_cast<int>(inFrames), output, static_cast<int>(outFrames))) return {};

    std::vector<float> out(static_cast<size_t>(outFrames * channels));
    for (int64_t i = 0; i < outFrames; ++i) {
        for (int c = 0; c < channels; ++c) {
            const float v = output[size_t(c)][size_t(i)];
            out[size_t(i * channels + c)] = std::isfinite(v) ? v : 0.0f;
        }
    }
    return out;
}

}  // namespace ps
