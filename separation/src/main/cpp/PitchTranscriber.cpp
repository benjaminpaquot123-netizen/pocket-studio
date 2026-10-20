#include "PitchTranscriber.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <exception>
#include <queue>
#include <thread>

#include "onnxruntime_cxx_api.h"
#include "samplerate.h"

namespace ps {

namespace {

// basic_pitch/constants.py and note_creation.py.
constexpr int kFftHop = 256;
constexpr int kAudioNSamples = kBasicPitchSampleRate * 2 - kFftHop;  // 43844
constexpr int kAnnotFps = kBasicPitchSampleRate / kFftHop;           // 86
constexpr int kAnnotNFrames = kAnnotFps * 2;                         // 172 frames per window
constexpr int kNotes = 88;
constexpr int kMidiOffset = 21;
constexpr int kMaxFreqIdx = 87;
constexpr double kMagicAlignmentOffset = 0.0018;
// inference.py defaults.
constexpr int kOverlappingFrames = 30;
constexpr int kOverlapLen = kOverlappingFrames * kFftHop;  // 7680
constexpr int kHopSize = kAudioNSamples - kOverlapLen;     // 36164
constexpr double kOnsetThresh = 0.5;
constexpr double kFrameThresh = 0.3;
constexpr double kMinNoteLengthMs = 127.7;
constexpr int kEnergyTol = 11;

// ---- SHA-256 (FIPS 180-4), small and self-contained ----

constexpr uint32_t kK[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2};

inline uint32_t rotr(uint32_t x, int n) { return (x >> n) | (x << (32 - n)); }

void sha256Block(uint32_t h[8], const unsigned char* p) {
    uint32_t w[64];
    for (int i = 0; i < 16; ++i) {
        w[i] = (uint32_t(p[i * 4]) << 24) | (uint32_t(p[i * 4 + 1]) << 16) | (uint32_t(p[i * 4 + 2]) << 8) | p[i * 4 + 3];
    }
    for (int i = 16; i < 64; ++i) {
        const uint32_t s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3);
        const uint32_t s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10);
        w[i] = w[i - 16] + s0 + w[i - 7] + s1;
    }
    uint32_t a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
    for (int i = 0; i < 64; ++i) {
        const uint32_t t1 = hh + (rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)) + ((e & f) ^ (~e & g)) + kK[i] + w[i];
        const uint32_t t2 = (rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)) + ((a & b) ^ (a & c) ^ (b & c));
        hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2;
    }
    h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
}

// ---- Post-processing (note_creation.py) ----

// Row-major [n][88] matrices.
using Matrix = std::vector<float>;

// get_infered_onsets(onsets, frames, n_diff=2), in double like numpy (the zero padding is float64).
std::vector<double> inferOnsets(const Matrix& onsets, const Matrix& frames, int n) {
    std::vector<double> diff(static_cast<size_t>(n) * kNotes, 0.0);
    double maxDiff = 0.0;
    float maxOnset = 0.0f;
    for (float v : onsets) maxOnset = std::max(maxOnset, v);
    for (int t = 2; t < n; ++t) {  // frame_diff[:n_diff] = 0
        for (int f = 0; f < kNotes; ++f) {
            const double cur = frames[size_t(t) * kNotes + f];
            const double d1 = cur - frames[size_t(t - 1) * kNotes + f];
            const double d2 = cur - frames[size_t(t - 2) * kNotes + f];
            const double d = std::max(0.0, std::min(d1, d2));
            diff[size_t(t) * kNotes + f] = d;
            maxDiff = std::max(maxDiff, d);
        }
    }
    std::vector<double> out(onsets.size());
    for (size_t i = 0; i < out.size(); ++i) {
        // Upstream divides by max(frame_diff) unguarded (NaN when frames never rise); treat as 0.
        const double scaled = maxDiff > 0.0 ? (double(maxOnset) * diff[i]) / maxDiff : 0.0;
        out[i] = std::max(double(onsets[i]), scaled);
    }
    return out;
}

double meanOf(const Matrix& frames, int from, int to, int f) {
    double sum = 0.0;
    for (int t = from; t < to; ++t) sum += frames[size_t(t) * kNotes + f];
    return sum / (to - from);
}

struct FrameNote { int start, end, pitch; float amplitude; };

// output_to_notes_polyphonic(frames, onsets, 0.5, 0.3, min_note_len, infer_onsets=True,
// no frequency limits, melodia_trick=True, energy_tol=11).
std::vector<FrameNote> outputToNotes(const Matrix& frames, const Matrix& rawOnsets, int n, int minNoteLen) {
    std::vector<FrameNote> notes;
    if (n < 3) return notes;
    const std::vector<double> onsets = inferOnsets(rawOnsets, frames, n);
    auto on = [&](int t, int f) { return onsets[size_t(t) * kNotes + f]; };

    // scipy.signal.argrelmax(onsets, axis=0): strict maxima along time, both ends excluded; keep
    // those >= onset_thresh, visited in reverse row-major order (np.where(...)[::-1]).
    std::vector<std::pair<int, int>> peaks;
    for (int t = 1; t < n - 1; ++t) {
        for (int f = 0; f < kNotes; ++f) {
            const double v = on(t, f);
            if (v > on(t - 1, f) && v > on(t + 1, f) && v >= kOnsetThresh) peaks.emplace_back(t, f);
        }
    }
    std::reverse(peaks.begin(), peaks.end());

    Matrix energy = frames;  // remaining_energy (float32 values or 0: same comparisons as float64)
    auto e = [&](int t, int f) -> float& { return energy[size_t(t) * kNotes + f]; };
    auto clearAround = [&](int t, int f) {
        e(t, f) = 0.0f;
        if (f < kMaxFreqIdx) e(t, f + 1) = 0.0f;
        if (f > 0) e(t, f - 1) = 0.0f;
    };

    for (const auto& [start, f] : peaks) {
        if (start >= n - 1) continue;
        int i = start + 1, k = 0;
        while (i < n - 1 && k < kEnergyTol) {
            if (double(e(i, f)) < kFrameThresh) ++k; else k = 0;
            ++i;
        }
        i -= k;
        if (i - start <= minNoteLen) continue;
        for (int t = start; t < i; ++t) clearAround(t, f);
        notes.push_back({start, i, f + kMidiOffset, static_cast<float>(meanOf(frames, start, i, f))});
    }

    // Melodia trick: repeatedly take the strongest remaining cell (np.argmax: first in row-major
    // order on ties) and grow a note around it. Cells only ever drop to 0, so a heap with lazy
    // deletion visits them in exactly the same order as rescanning the matrix.
    struct Cell { float v; int idx; };
    auto weaker = [](const Cell& a, const Cell& b) { return a.v < b.v || (a.v == b.v && a.idx > b.idx); };
    std::priority_queue<Cell, std::vector<Cell>, decltype(weaker)> heap(weaker);
    for (int idx = 0; idx < n * kNotes; ++idx) {
        if (double(energy[size_t(idx)]) > kFrameThresh) heap.push({energy[size_t(idx)], idx});
    }
    while (!heap.empty()) {
        const Cell top = heap.top();
        heap.pop();
        if (energy[size_t(top.idx)] != top.v) continue;  // already cleared
        const int mid = top.idx / kNotes, f = top.idx % kNotes;
        e(mid, f) = 0.0f;

        int i = mid + 1, k = 0;
        while (i < n - 1 && k < kEnergyTol) {
            if (double(e(i, f)) < kFrameThresh) ++k; else k = 0;
            clearAround(i, f);
            ++i;
        }
        const int end = i - 1 - k;

        i = mid - 1;
        k = 0;
        while (i > 0 && k < kEnergyTol) {
            if (double(e(i, f)) < kFrameThresh) ++k; else k = 0;
            clearAround(i, f);
            --i;
        }
        const int start = i + 1 + k;

        if (end - start <= minNoteLen) continue;
        notes.push_back({start, end, f + kMidiOffset, static_cast<float>(meanOf(frames, start, end, f))});
    }
    return notes;
}

// model_frames_to_time for one frame index.
double frameToTime(int frame) {
    const double windowOffset = (double(kFftHop) / kBasicPitchSampleRate) *
                                    (kAnnotNFrames - (double(kAudioNSamples) / kFftHop)) +
                                kMagicAlignmentOffset;
    return double(frame) * kFftHop / kBasicPitchSampleRate - windowOffset * std::floor(double(frame) / kAnnotNFrames);
}

bool readFile(const std::string& path, std::vector<unsigned char>& out) {
    std::FILE* f = std::fopen(path.c_str(), "rb");
    if (!f) return false;
    bool ok = std::fseek(f, 0, SEEK_END) == 0;
    const long size = ok ? std::ftell(f) : -1;
    ok = ok && size > 0 && size < (64L << 20) && std::fseek(f, 0, SEEK_SET) == 0;
    if (ok) {
        out.resize(static_cast<size_t>(size));
        ok = std::fread(out.data(), 1, out.size(), f) == out.size();
    }
    std::fclose(f);
    return ok;
}

}  // namespace

std::string sha256Hex(const unsigned char* data, size_t size) {
    uint32_t h[8] = {0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19};
    size_t i = 0;
    for (; i + 64 <= size; i += 64) sha256Block(h, data + i);
    unsigned char tail[128] = {};
    const size_t rest = size - i;
    if (rest) std::memcpy(tail, data + i, rest);
    tail[rest] = 0x80;
    const size_t tailLen = rest + 9 <= 64 ? 64 : 128;
    const uint64_t bits = static_cast<uint64_t>(size) * 8;
    for (int b = 0; b < 8; ++b) tail[tailLen - 1 - b] = static_cast<unsigned char>(bits >> (8 * b));
    sha256Block(h, tail);
    if (tailLen == 128) sha256Block(h, tail + 64);
    std::string hex;
    char buf[9];
    for (uint32_t v : h) {
        std::snprintf(buf, sizeof buf, "%08x", v);
        hex += buf;
    }
    return hex;
}

std::vector<PitchNote> transcribe(const std::string& modelPath, const float* mono, size_t frames, int sampleRate,
                                  std::string* error, int threads) {
    std::string ignored;
    std::string& err = error ? *error : ignored;
    err.clear();
    if (!mono || frames == 0 || sampleRate < 8000 || sampleRate > 384000) {
        err = "son absent";
        return {};
    }
    if (double(frames) / sampleRate > kBasicPitchMaxSeconds) {
        err = "son trop long";
        return {};
    }
    try {
        // Never hand ONNX Runtime a model we have not checked.
        std::vector<unsigned char> model;
        if (!readFile(modelPath, model)) {
            err = "modèle introuvable";
            return {};
        }
        if (sha256Hex(model.data(), model.size()) != kBasicPitchSha256) {
            err = "modèle corrompu";
            return {};
        }

        // librosa.load(sr=22050, mono=True); then inference.py prepends overlap_len / 2 zeros.
        std::vector<float> audio;
        const size_t pad = kOverlapLen / 2;
        size_t length = frames;
        if (sampleRate == kBasicPitchSampleRate) {
            audio.assign(pad + frames, 0.0f);
            std::copy(mono, mono + frames, audio.begin() + static_cast<long>(pad));
        } else {
            const double ratio = double(kBasicPitchSampleRate) / sampleRate;
            std::vector<float> out(static_cast<size_t>(std::ceil(frames * ratio)) + 2, 0.0f);
            SRC_DATA data{};
            data.data_in = mono;
            data.input_frames = static_cast<long>(frames);
            data.data_out = out.data();
            data.output_frames = static_cast<long>(out.size());
            data.src_ratio = ratio;
            if (src_simple(&data, SRC_SINC_MEDIUM_QUALITY, 1) != 0) {
                err = "rééchantillonnage impossible";
                return {};
            }
            length = static_cast<size_t>(data.output_frames_gen);
            audio.assign(pad + length, 0.0f);
            std::copy(out.begin(), out.begin() + static_cast<long>(length), audio.begin() + static_cast<long>(pad));
        }
        for (float& v : audio) v = std::isfinite(v) ? std::clamp(v, -4.0f, 4.0f) : 0.0f;

        Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "basic-pitch");
        Ort::SessionOptions options;
        if (threads <= 0) threads = std::clamp(static_cast<int>(std::thread::hardware_concurrency()) / 2, 1, 4);
        options.SetIntraOpNumThreads(threads);
        options.SetInterOpNumThreads(1);
        options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
        options.DisableCpuMemArena();
        options.DisableMemPattern();
        Ort::Session session(env, model.data(), model.size(), options);
        std::vector<unsigned char>().swap(model);

        const char* inputNames[] = {"serving_default_input_2:0"};
        const char* outputNames[] = {"StatefulPartitionedCall:1", "StatefulPartitionedCall:2"};  // note, onset
        const auto memory = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
        const int64_t shape[] = {1, kAudioNSamples, 1};
        std::vector<float> window(kAudioNSamples);

        // One window at a time (batch 1): memory stays flat whatever the length. unwrap_output
        // drops the 15 overlapping frames at each end of every window.
        constexpr int kHalfOverlap = kOverlappingFrames / 2;
        constexpr int kKept = kAnnotNFrames - kOverlappingFrames;  // 142
        Matrix note, onset;
        for (size_t start = 0; start < audio.size(); start += kHopSize) {
            const size_t count = std::min<size_t>(kAudioNSamples, audio.size() - start);
            std::copy(audio.begin() + static_cast<long>(start), audio.begin() + static_cast<long>(start + count), window.begin());
            std::fill(window.begin() + static_cast<long>(count), window.end(), 0.0f);
            Ort::Value input = Ort::Value::CreateTensor<float>(memory, window.data(), window.size(), shape, 3);
            auto outputs = session.Run(Ort::RunOptions{nullptr}, inputNames, &input, 1, outputNames, 2);
            for (int o = 0; o < 2; ++o) {
                const auto dims = outputs[size_t(o)].GetTensorTypeAndShapeInfo().GetShape();
                if (dims.size() != 3 || dims[0] != 1 || dims[1] != kAnnotNFrames || dims[2] != kNotes) {
                    err = "modèle inattendu";
                    return {};
                }
                const float* p = outputs[size_t(o)].GetTensorData<float>() + size_t(kHalfOverlap) * kNotes;
                Matrix& dst = o == 0 ? note : onset;
                dst.insert(dst.end(), p, p + size_t(kKept) * kNotes);
            }
        }
        std::vector<float>().swap(audio);

        // Trim to int(n_expected_windows * n_frames_per_window), as upstream (float arithmetic).
        const double expectedWindows = double(length) / kHopSize;
        const size_t n = std::min(note.size() / kNotes, static_cast<size_t>(expectedWindows * kKept));
        note.resize(n * kNotes);
        onset.resize(n * kNotes);
        for (float& v : note) if (!std::isfinite(v)) v = 0.0f;
        for (float& v : onset) if (!std::isfinite(v)) v = 0.0f;

        const int minNoteLen = static_cast<int>(std::nearbyint(kMinNoteLengthMs / 1000.0 * (double(kBasicPitchSampleRate) / kFftHop)));
        const auto frameNotes = outputToNotes(note, onset, static_cast<int>(n), minNoteLen);
        std::vector<PitchNote> result;
        result.reserve(frameNotes.size());
        for (const auto& fn : frameNotes) {
            result.push_back({frameToTime(fn.start), frameToTime(fn.end), fn.pitch, fn.amplitude});
        }
        std::sort(result.begin(), result.end(), [](const PitchNote& a, const PitchNote& b) {
            return a.start < b.start || (a.start == b.start && a.pitch < b.pitch);
        });
        return result;
    } catch (const Ort::Exception& e) {
        err = e.what();
    } catch (const std::bad_alloc&) {
        err = "mémoire insuffisante";
    } catch (const std::exception& e) {
        err = e.what();
    }
    return {};
}

}  // namespace ps
