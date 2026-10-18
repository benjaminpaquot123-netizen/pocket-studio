#include "WavWriter.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace ps {

namespace {
void put16(uint8_t* p, uint16_t v) { p[0] = v & 0xFF; p[1] = v >> 8; }
void put32(uint8_t* p, uint32_t v) { for (int i = 0; i < 4; ++i) p[i] = (v >> (8 * i)) & 0xFF; }
constexpr int kBytesPerSample = 3;
}  // namespace

WavWriter::~WavWriter() {
    if (file_) std::fclose(file_);
}

bool WavWriter::open(const char* path, int sampleRate, int channels) {
    if (file_) return false;
    file_ = std::fopen(path, "wb");
    if (!file_) return false;
    sampleRate_ = sampleRate;
    channels_ = channels;
    frames_ = 0;
    ok_ = writeHeader(0);
    return ok_;
}

bool WavWriter::writeHeader(uint32_t dataBytes) {
    uint8_t h[44];
    std::memcpy(h, "RIFF", 4);
    put32(h + 4, 36 + dataBytes);
    std::memcpy(h + 8, "WAVEfmt ", 8);
    put32(h + 16, 16);
    put16(h + 20, 1);  // integer PCM
    put16(h + 22, static_cast<uint16_t>(channels_));
    put32(h + 24, static_cast<uint32_t>(sampleRate_));
    put32(h + 28, static_cast<uint32_t>(sampleRate_ * channels_ * kBytesPerSample));
    put16(h + 32, static_cast<uint16_t>(channels_ * kBytesPerSample));
    put16(h + 34, 8 * kBytesPerSample);
    std::memcpy(h + 36, "data", 4);
    put32(h + 40, dataBytes);
    return std::fwrite(h, 1, sizeof h, file_) == sizeof h;
}

float WavWriter::dither() {
    // Triangular dither of +-1 LSB from two uniform values (xorshift32).
    auto next = [this] {
        seed_ ^= seed_ << 13;
        seed_ ^= seed_ >> 17;
        seed_ ^= seed_ << 5;
        return static_cast<float>(seed_) * (1.0f / 4294967296.0f);
    };
    return next() - next();
}

bool WavWriter::write(const float* interleaved, int numFrames) {
    if (!file_ || !ok_) return false;
    constexpr float kScale = 8388607.0f;
    const int total = numFrames * channels_;
    const int perChunk = static_cast<int>(sizeof buffer_) / kBytesPerSample;
    for (int start = 0; start < total; start += perChunk) {
        const int n = std::min(perChunk, total - start);
        for (int i = 0; i < n; ++i) {
            float v = interleaved[start + i];
            if (!std::isfinite(v)) v = 0.0f;
            const float s = std::clamp(v * kScale + dither(), -8388608.0f, 8388607.0f);
            const int32_t q = static_cast<int32_t>(std::lrint(s));
            uint8_t* p = buffer_ + i * kBytesPerSample;
            p[0] = q & 0xFF;
            p[1] = (q >> 8) & 0xFF;
            p[2] = (q >> 16) & 0xFF;
        }
        if (std::fwrite(buffer_, kBytesPerSample, n, file_) != static_cast<size_t>(n)) {
            ok_ = false;
            return false;
        }
    }
    frames_ += numFrames;
    return true;
}

bool WavWriter::close() {
    if (!file_) return false;
    const int64_t bytes = frames_ * channels_ * kBytesPerSample;
    bool ok = ok_ && bytes <= 0xFFFFFFFFll - 36;
    if (ok) ok = std::fseek(file_, 0, SEEK_SET) == 0 && writeHeader(static_cast<uint32_t>(bytes));
    ok = (std::fflush(file_) == 0) && ok;
    ok = (std::fclose(file_) == 0) && ok;
    file_ = nullptr;
    return ok;
}

}  // namespace ps
