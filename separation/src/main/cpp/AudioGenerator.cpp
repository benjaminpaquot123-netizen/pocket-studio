#include "AudioGenerator.h"

#include <algorithm>
#include <cmath>
#include <exception>
#include <new>
#include <random>

#include "onnxruntime_cxx_api.h"

namespace ps {

namespace {

constexpr int kTokens = 64;
constexpr int kLatentChannels = 64;
constexpr int kLatentFrames = 256;
constexpr int kSamplesPerLatent = 2048;
constexpr int kSteps = 8;
constexpr int kCrossTokens = 65;
constexpr int kWidth = 768;

struct CancelledError : std::exception {};

Ort::Env& env() {
    static Ort::Env e(ORT_LOGGING_LEVEL_WARNING, "pocketstudio-generate");
    return e;
}

Ort::Session open(const std::string& path, int threads) {
    Ort::SessionOptions options;
    options.SetIntraOpNumThreads(std::max(1, threads));
    options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
    // Arena and memory patterns keep peak buffers alive between stages: off, like the separator.
    options.DisableCpuMemArena();
    options.DisableMemPattern();
    return Ort::Session(env(), path.c_str(), options);
}

// The distilled model's schedule (stable-audio-tools, LogSNR shift rate 0): sigmoid of a line,
// endpoints kept at 1 and 0.
std::vector<float> schedule() {
    std::vector<float> t(kSteps + 1);
    for (int i = 0; i <= kSteps; ++i) {
        const double u = 1.0 - static_cast<double>(i) / kSteps;
        t[static_cast<size_t>(i)] = static_cast<float>(1.0 / (1.0 + std::exp(-(8.2 * u - 2.0))));
    }
    t.front() = 1.0f;
    t.back() = 0.0f;
    return t;
}

}  // namespace

GenerateResult generateAudio(const GenerateRequest& request, std::vector<float>& out, const std::function<bool(float)>& progress) {
    out.clear();
    if (request.tokens.empty() || request.tokens.size() > static_cast<size_t>(kTokens)) return GenerateResult::Failed;
    const float seconds = std::clamp(request.seconds, 0.5f, kGenMaxSeconds);
    auto step = [&](float p) {
        if (progress && !progress(p)) throw CancelledError();
    };
    try {
        const auto memory = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
        // 1. Text and length -> conditioning.
        std::vector<float> cross(static_cast<size_t>(kCrossTokens) * kWidth), global(kWidth);
        {
            Ort::Session cond = open(request.dir + "/cond_q8.onnx", request.threads);
            std::vector<int64_t> ids(kTokens, 0), mask(kTokens, 0);
            for (size_t i = 0; i < request.tokens.size(); ++i) {
                ids[i] = request.tokens[i];
                mask[i] = 1;
            }
            const int64_t tokShape[2] = {1, kTokens};
            const int64_t oneShape[1] = {1};
            float secs = seconds;
            Ort::Value inputs[3] = {
                Ort::Value::CreateTensor<int64_t>(memory, ids.data(), ids.size(), tokShape, 2),
                Ort::Value::CreateTensor<int64_t>(memory, mask.data(), mask.size(), tokShape, 2),
                Ort::Value::CreateTensor<float>(memory, &secs, 1, oneShape, 1),
            };
            const char* in[3] = {"input_ids", "attention_mask", "seconds_total"};
            const char* outNames[2] = {"cross", "global"};
            auto result = cond.Run(Ort::RunOptions{nullptr}, in, inputs, 3, outNames, 2);
            const float* c = result[0].GetTensorData<float>();
            const float* g = result[1].GetTensorData<float>();
            std::copy(c, c + cross.size(), cross.begin());
            std::copy(g, g + global.size(), global.begin());
        }
        step(0.05f);

        // 2. Noise -> latent: ping-pong sampling, fresh noise at each step.
        std::mt19937_64 rng(request.seed);
        std::normal_distribution<float> normal(0.0f, 1.0f);
        const size_t latentSize = static_cast<size_t>(kLatentChannels) * kLatentFrames;
        std::vector<float> x(latentSize), den(latentSize);
        for (auto& v : x) v = normal(rng);
        {
            Ort::Session dit = open(request.dir + "/dit_l8.onnx", request.threads);
            const auto t = schedule();
            const int64_t xShape[3] = {1, kLatentChannels, kLatentFrames};
            const int64_t oneShape[1] = {1};
            const int64_t crossShape[3] = {1, kCrossTokens, kWidth};
            const int64_t globalShape[2] = {1, kWidth};
            const char* in[4] = {"x", "t", "cross", "global"};
            const char* outNames[1] = {"v"};
            for (int i = 0; i < kSteps; ++i) {
                float ti = t[static_cast<size_t>(i)];
                Ort::Value inputs[4] = {
                    Ort::Value::CreateTensor<float>(memory, x.data(), x.size(), xShape, 3),
                    Ort::Value::CreateTensor<float>(memory, &ti, 1, oneShape, 1),
                    Ort::Value::CreateTensor<float>(memory, cross.data(), cross.size(), crossShape, 3),
                    Ort::Value::CreateTensor<float>(memory, global.data(), global.size(), globalShape, 2),
                };
                auto result = dit.Run(Ort::RunOptions{nullptr}, in, inputs, 4, outNames, 1);
                const float* v = result[0].GetTensorData<float>();
                const float next = t[static_cast<size_t>(i + 1)];
                for (size_t k = 0; k < latentSize; ++k) {
                    den[k] = x[k] - ti * v[k];
                    x[k] = (1.0f - next) * den[k] + next * normal(rng);
                }
                step(0.05f + 0.75f * static_cast<float>(i + 1) / kSteps);
            }
        }

        // 3. Latent -> sound, only the frames the length needs.
        const int frames = std::clamp(static_cast<int>(std::ceil(seconds * kGenSampleRate / kSamplesPerLatent)), 1, kLatentFrames);
        std::vector<float> z(static_cast<size_t>(kLatentChannels) * frames);
        for (int c = 0; c < kLatentChannels; ++c) {
            std::copy(x.begin() + static_cast<ptrdiff_t>(c) * kLatentFrames, x.begin() + static_cast<ptrdiff_t>(c) * kLatentFrames + frames,
                      z.begin() + static_cast<ptrdiff_t>(c) * frames);
        }
        {
            Ort::Session dec = open(request.dir + "/dec.onnx", request.threads);
            const int64_t zShape[3] = {1, kLatentChannels, frames};
            Ort::Value input = Ort::Value::CreateTensor<float>(memory, z.data(), z.size(), zShape, 3);
            const char* in[1] = {"z"};
            const char* outNames[1] = {"audio"};
            auto result = dec.Run(Ort::RunOptions{nullptr}, in, &input, 1, outNames, 1);
            const auto shape = result[0].GetTensorTypeAndShapeInfo().GetShape();
            if (shape.size() != 3 || shape[1] != 2) return GenerateResult::Failed;
            const int64_t samples = std::min<int64_t>(shape[2], static_cast<int64_t>(seconds * kGenSampleRate));
            const float* a = result[0].GetTensorData<float>();
            out.resize(static_cast<size_t>(samples) * 2);
            for (int64_t i = 0; i < samples; ++i) {
                const float l = a[i], r = a[shape[2] + i];
                out[static_cast<size_t>(i) * 2] = std::isfinite(l) ? l : 0.0f;
                out[static_cast<size_t>(i) * 2 + 1] = std::isfinite(r) ? r : 0.0f;
            }
        }
        step(1.0f);
        return GenerateResult::Ok;
    } catch (const CancelledError&) {
        out.clear();
        return GenerateResult::Cancelled;
    } catch (const std::bad_alloc&) {
        out.clear();
        return GenerateResult::OutOfMemory;
    } catch (const Ort::Exception& e) {
        out.clear();
        return e.GetOrtErrorCode() == ORT_NO_SUCHFILE || e.GetOrtErrorCode() == ORT_INVALID_PROTOBUF ? GenerateResult::BadModel : GenerateResult::Failed;
    } catch (...) {
        out.clear();
        return GenerateResult::Failed;
    }
}

}  // namespace ps
