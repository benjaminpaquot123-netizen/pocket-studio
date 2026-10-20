#pragma once

#include <cstdint>
#include <functional>
#include <string>
#include <vector>

namespace ps {

// Stable Audio Open Small output: 44.1 kHz stereo, up to 256 latent frames (11.9 s).
constexpr int kGenSampleRate = 44100;
constexpr float kGenMaxSeconds = 11.0f;

enum class GenerateResult : int { Ok = 0, BadModel = 1, Cancelled = 2, OutOfMemory = 3, Failed = 4 };

struct GenerateRequest {
    std::string dir;              // cond_q8.onnx, dit_l8.onnx, dec.onnx (checked by the caller)
    std::vector<int64_t> tokens;  // T5 ids of the prompt, ending with </s> (1); at most 64
    float seconds = 4.0f;         // 0.5 .. kGenMaxSeconds
    uint64_t seed = 1;
    int threads = 4;
};

// Text to sound: conditioning, 8 ping-pong denoising steps of the distilled DiT, decoding. Each
// model is loaded only for its stage, so memory peaks at the largest one. `progress` gets 0..1
// and returns false to cancel. `out` receives interleaved stereo at kGenSampleRate.
GenerateResult generateAudio(const GenerateRequest& request, std::vector<float>& out, const std::function<bool(float)>& progress);

}  // namespace ps
