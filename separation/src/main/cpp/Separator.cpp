#include "Separator.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <exception>
#include <vector>

#include "demucs.hpp"
#include "samplerate.h"

namespace ps {

namespace {

struct CancelledError : std::exception {};

void put16(std::FILE* f, uint16_t v) { std::fputc(v & 0xFF, f); std::fputc(v >> 8, f); }
void put32(std::FILE* f, uint32_t v) { put16(f, v & 0xFFFF); put16(f, v >> 16); }

// 32-bit float stereo WAV, written to a temporary name then renamed.
bool writeStem(const std::string& path, const Eigen::Tensor3dXf& out, int source, int64_t frames, float gain) {
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
            const float v = out(source, c, i) * gain;
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

namespace {

// Runs the model on `audio` (2 x frames), which it frees as soon as the model has its own copy.
SeparationResult runModel(const char* modelPath, Eigen::MatrixXf& audio, const char* outDir,
                          const std::function<bool(float)>& progress, int threads) {
    const int64_t frames = audio.cols();
    try {
        demucsonnx::demucs_model model;
        Ort::SessionOptions options;
        options.SetIntraOpNumThreads(std::max(1, threads));
        options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
        // The CPU arena and memory patterns keep every intermediate tensor's peak: 5.1 GB for a
        // 30 s song on Linux, enough for a phone to kill us. Without them: 2.6 GB, and faster.
        options.DisableCpuMemArena();
        options.DisableMemPattern();
        if (!loadModel(modelPath, model, options)) return {SeparationResult::ModelError, "modèle inattendu"};
        const auto out = demucsonnx::demucs_inference(model, audio, [&](float p, const std::string&) {
            if (!progress(std::clamp(p, 0.0f, 1.0f))) throw CancelledError();
        });
        audio.resize(0, 0);
        // A stem can peak above full scale even when the song does not: players then clip it
        // (heard as crackle). Scale all four together, keeping their balance and their sum.
        float peak = 0.0f;
        for (int s = 0; s < 4; ++s) {
            for (int c = 0; c < 2; ++c) {
                for (int64_t i = 0; i < frames; ++i) {
                    const float v = std::fabs(out(s, c, i));
                    if (std::isfinite(v)) peak = std::max(peak, v);
                }
            }
        }
        const float gain = peak > kStemCeiling ? kStemCeiling / peak : 1.0f;
        for (int s = 0; s < 4; ++s) {
            if (!writeStem(std::string(outDir) + "/" + kStemNames[s] + ".wav", out, s, frames, gain)) {
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

}  // namespace

SeparationResult separate(const char* modelPath, const float* interleaved, int64_t frames, const char* outDir,
                          const std::function<bool(float)>& progress, int threads) {
    if (!modelPath || !interleaved || !outDir || frames < kModelSampleRate) {
        return {SeparationResult::BadInput, "son trop court ou absent"};
    }
    try {
        Eigen::MatrixXf audio(2, frames);
        for (int64_t i = 0; i < frames; ++i) {
            audio(0, i) = interleaved[i * 2];
            audio(1, i) = interleaved[i * 2 + 1];
        }
        return runModel(modelPath, audio, outDir, progress, threads);
    } catch (const std::bad_alloc&) {
        return {SeparationResult::IoError, "mémoire insuffisante"};
    }
}

bool loadRaw(const char* path, int channels, int sampleRate, std::vector<float>& stereo44k) {
    if (!path || channels < 1 || channels > 2 || sampleRate < 8000 || sampleRate > 384000) return false;
    std::FILE* f = std::fopen(path, "rb");
    if (!f) return false;
    std::fseek(f, 0, SEEK_END);
    const long bytes = std::ftell(f);
    std::fseek(f, 0, SEEK_SET);
    const int64_t frames = bytes / static_cast<long>(sizeof(float) * channels);
    std::vector<float> stereo;
    try {
        stereo.resize(static_cast<size_t>(frames) * 2);
        {
            // Read in chunks straight into the stereo buffer: no second full copy of the song.
            std::vector<float> chunk(static_cast<size_t>(65536) * channels);
            int64_t done = 0;
            while (done < frames) {
                const int64_t want = std::min<int64_t>(65536, frames - done);
                const size_t got = std::fread(chunk.data(), sizeof(float) * channels, static_cast<size_t>(want), f);
                if (got != static_cast<size_t>(want)) break;
                for (int64_t i = 0; i < want; ++i) {
                    for (int c = 0; c < 2; ++c) {
                        const float v = chunk[static_cast<size_t>(i * channels + (channels == 2 ? c : 0))];
                        stereo[static_cast<size_t>((done + i) * 2 + c)] = std::isfinite(v) ? std::clamp(v, -4.0f, 4.0f) : 0.0f;
                    }
                }
                done += want;
            }
            std::fclose(f);
            f = nullptr;
            if (done != frames) return false;
        }
        if (sampleRate == kModelSampleRate) {
            stereo44k = std::move(stereo);
            return true;
        }
        const double ratio = static_cast<double>(kModelSampleRate) / sampleRate;
        stereo44k.assign(static_cast<size_t>(std::ceil(frames * ratio)) * 2 + 2, 0.0f);
        SRC_DATA data{};
        data.data_in = stereo.data();
        data.input_frames = static_cast<long>(frames);
        data.data_out = stereo44k.data();
        data.output_frames = static_cast<long>(stereo44k.size() / 2);
        data.src_ratio = ratio;
        const int err = src_simple(&data, SRC_SINC_MEDIUM_QUALITY, 2);
        std::vector<float>().swap(stereo);
        if (err != 0) return false;
        stereo44k.resize(static_cast<size_t>(data.output_frames_gen) * 2);
        return true;
    } catch (const std::bad_alloc&) {
        if (f) std::fclose(f);
        return false;
    }
}

SeparationResult separateFile(const char* modelPath, const char* rawPath, int channels, int sampleRate,
                              const char* outDir, const std::function<bool(float)>& progress, int threads) {
    if (!modelPath || !outDir) return {SeparationResult::BadInput, "son absent"};
    try {
        Eigen::MatrixXf matrix;
        {
            std::vector<float> audio;
            if (!loadRaw(rawPath, channels, sampleRate, audio)) return {SeparationResult::BadInput, "lecture du son impossible"};
            const auto frames = static_cast<int64_t>(audio.size() / 2);
            if (frames < kModelSampleRate) return {SeparationResult::BadInput, "son trop court"};
            matrix.resize(2, frames);
            for (int64_t i = 0; i < frames; ++i) {
                matrix(0, i) = audio[static_cast<size_t>(i * 2)];
                matrix(1, i) = audio[static_cast<size_t>(i * 2 + 1)];
            }
        }  // the interleaved copy is gone before the model loads
        return runModel(modelPath, matrix, outDir, progress, threads);
    } catch (const std::bad_alloc&) {
        return {SeparationResult::IoError, "mémoire insuffisante"};
    }
}

}  // namespace ps
