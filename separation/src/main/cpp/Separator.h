#pragma once

#include <cstdint>
#include <functional>
#include <string>

namespace ps {

// Splits a song into stems with HT-Demucs v4 (ONNX, via demucs.onnx). Heavy: minutes of CPU
// and hundreds of MB; run on a worker thread, never near the audio engine.
struct SeparationResult {
    enum Code { Ok, ModelError, Cancelled, IoError, BadInput };
    Code code = Ok;
    std::string message;
};

// The stems written, in the model's order.
constexpr const char* kStemNames[] = {"drums", "bass", "other", "vocals"};
constexpr int kModelSampleRate = 44100;

// `interleaved` is stereo at 44.1 kHz. Writes outDir/<stem>.wav (32-bit float, stereo, 44.1 kHz).
// `progress` gets 0..1 and returns false to cancel.
SeparationResult separate(const char* modelPath, const float* interleaved, int64_t frames, const char* outDir,
                          const std::function<bool(float)>& progress, int threads);

}  // namespace ps
