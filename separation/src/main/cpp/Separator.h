#pragma once

#include <cstdint>
#include <functional>
#include <string>
#include <vector>

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
// Stems are written with their peak at or below this level (-0.5 dBFS).
constexpr float kStemCeiling = 0.944f;

// `interleaved` is stereo at 44.1 kHz. Writes outDir/<stem>.wav (32-bit float, stereo, 44.1 kHz).
// `progress` gets 0..1 and returns false to cancel.
SeparationResult separate(const char* modelPath, const float* interleaved, int64_t frames, const char* outDir,
                          const std::function<bool(float)>& progress, int threads);

// Reads a raw interleaved 32-bit float file (1 or 2 channels, any rate) as interleaved stereo at
// 44.1 kHz (libsamplerate). Lets the app decode a whole song to disk instead of the Java heap.
bool loadRaw(const char* path, int channels, int sampleRate, std::vector<float>& stereo44k);

// separate() on a raw file (see loadRaw).
SeparationResult separateFile(const char* modelPath, const char* rawPath, int channels, int sampleRate,
                              const char* outDir, const std::function<bool(float)>& progress, int threads);

}  // namespace ps
