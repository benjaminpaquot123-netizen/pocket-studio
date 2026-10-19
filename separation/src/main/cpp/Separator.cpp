#include "Separator.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <exception>
#include <vector>

#include "demucs.hpp"

namespace ps {

namespace {

struct CancelledError : std::exception {};

void put16(std::FILE* f, uint16_t v) { std::fputc(v & 0xFF, f); std::fputc(v >> 8, f); }
void put32(std::FILE* f, uint32_t v) { put16(f, v & 0xFFFF); put16(f, v >> 16); }

// 32-bit float stereo WAV, written to a temporary name then renamed.
bool writeStem(const std::string& path, const Eigen::Tensor3dXf& out, int source, int64_t frames) {
    const std::string tmp = path + ".tmp";
    std::FILE* f = std::fopen(tmp.c_str(), "wb");
    if (!f) return false;
    const uint32_t bytes = static_cast<uint32_t>(frames * 2 * 4);
    std::fwrite("RIFF", 1, 4, f); put32(f, 36 + bytes); std::fwrite("WAVEfmt ", 1, 8, f);
    put32(f, 16); put16(f, 3); put16(f, 2); put32(f, kModelSampleRate); put32(f, kModelSampleRate * 8); put16(f, 8); put16(f, 32);
    std::fwrite("data", 1, 4, f); put32(f, bytes);
    std::vector<float> chunk;
    chunk.reserve(8192);
    for (int64_t i = 0; i < frames; ++i) {
        for (int c = 0; c < 2; ++c) {
            const float v = out(source, c, i);
            chunk.push_back(std::isfinite(v) ? v : 0.0f);
        }
        if (chunk.size() >= 8192 || i == frames - 1) {
            if (std::fwrite(chunk.data(), sizeof(float), chunk.size(), f) != chunk.size()) {
                std::fclose(f);
                std::remove(tmp.c_str());
                return false;
            }
            chunk.clear();
        }
    }
    const bool ok = std::fflush(f) == 0 && std::fclose(f) == 0;
    return ok && std::rename(tmp.c_str(), path.c_str()) == 0;
}

// Same as demucsonnx::load_model, but ONNX Runtime maps the model file instead of us copying
// 174 MB into memory first.
bool loadModel(const char* path, demucsonnx::demucs_model& model, Ort::SessionOptions& options) {
    model.sess = std::make_unique<Ort::Session>(model.env, path, options);
    for (int i = 0; i < 2; ++i) {
        model.input_names.push_back(model.sess->GetInputNameAllocated(i, demucsonnx::allocator).get());
        model.output_names.push_back(model.sess->GetOutputNameAllocated(i, demucsonnx::allocator).get());
    }
    for (const auto& n : model.input_names) model.input_names_ptrs.push_back(n.c_str());
    for (const auto& n : model.output_names) model.output_names_ptrs.push_back(n.c_str());
    const auto s0 = model.sess->GetOutputTypeInfo(0).GetTensorTypeAndShapeInfo().GetShape();
    const auto s1 = model.sess->GetOutputTypeInfo(1).GetTensorTypeAndShapeInfo().GetShape();
    if (s0.size() < 2 || s1.size() < 2 || s0[1] != s1[1] || s0[1] != 4) return false;
    model.nb_sources = static_cast<int>(s0[1]);
    return true;
}

}  // namespace

SeparationResult separate(const char* modelPath, const float* interleaved, int64_t frames, const char* outDir,
                          const std::function<bool(float)>& progress, int threads) {
    if (!modelPath || !interleaved || !outDir || frames < kModelSampleRate) {
        return {SeparationResult::BadInput, "son trop court ou absent"};
    }
    try {
        demucsonnx::demucs_model model;
        Ort::SessionOptions options;
        options.SetIntraOpNumThreads(std::max(1, threads));
        options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
        if (!loadModel(modelPath, model, options)) return {SeparationResult::ModelError, "modèle inattendu"};

        Eigen::MatrixXf audio(2, frames);
        for (int64_t i = 0; i < frames; ++i) {
            audio(0, i) = interleaved[i * 2];
            audio(1, i) = interleaved[i * 2 + 1];
        }
        const auto out = demucsonnx::demucs_inference(model, audio, [&](float p, const std::string&) {
            if (!progress(std::clamp(p, 0.0f, 1.0f))) throw CancelledError();
        });
        for (int s = 0; s < 4; ++s) {
            if (!writeStem(std::string(outDir) + "/" + kStemNames[s] + ".wav", out, s, frames)) {
                return {SeparationResult::IoError, "écriture impossible (stockage plein ?)"};
            }
        }
        progress(1.0f);
        return {};
    } catch (const CancelledError&) {
        return {SeparationResult::Cancelled, "annulé"};
    } catch (const Ort::Exception& e) {
        return {SeparationResult::ModelError, e.what()};
    } catch (const std::bad_alloc&) {
        return {SeparationResult::IoError, "mémoire insuffisante"};
    } catch (const std::exception& e) {
        return {SeparationResult::ModelError, e.what()};
    }
}

}  // namespace ps
