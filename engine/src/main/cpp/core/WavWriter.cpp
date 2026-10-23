#include "WavWriter.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace ps {

namespace {
void put16(uint8_t* p, uint16_t v) { p[0] = v & 0xFF; p[1] = v >> 8; }
void put32(uint8_t* p, uint32_t v) { for (int i = 0; i < 4; ++i) p[i] = (v >> (8 * i)) & 0xFF; }
}  // namespace

WavWriter::~WavWriter() {
    if (file_) std::fclose(file_);
}

bool WavWriter::openBits(const char* path, int sampleRate, int channels, int bits) {
    if (file_) return false;
    if (bits != 16 && bits != 24 && bits != 32) return false;
    file_ = std::fopen(path, "wb");
    if (!file_) return false;
    sampleRate_ = sampleRate;
    channels_ = channels;
    bits_ = bits;
    float_ = bits == 32;
    frames_ = 0;
    ok_ = writeHeader(0);
    return ok_;
}

bool WavWriter::writeHeader(uint32_t dataBytes) {
    const uint32_t bytes = static_cast<uint32_t>(bytesPerSample());
    if (float_) {
        // IEEE float: an 18-byte format chunk and the "fact" chunk (frame count) non-PCM files carry.
        uint8_t h[58];
        std::memcpy(h, "RIFF", 4);
        put32(h + 4, 50 + dataBytes);
        std::memcpy(h + 8, "WAVEfmt ", 8);
        put32(h + 16, 18);
        put16(h + 20, 3);  // IEEE float
        put16(h + 22, static_cast<uint16_t>(channels_));
        put32(h + 24, static_cast<uint32_t>(sampleRate_));
        put32(h + 28, static_cast<uint32_t>(sampleRate_) * channels_ * bytes);
        put16(h + 32, static_cast<uint16_t>(channels_ * bytes));
        put16(h + 34, static_cast<uint16_t>(8 * bytes));
        put16(h + 36, 0);
        std::memcpy(h + 38, "fact", 4);
        put32(h + 42, 4);
        put32(h + 46, dataBytes / (channels_ * bytes));
        std::memcpy(h + 50, "data", 4);
        put32(h + 54, dataBytes);
        return std::fwrite(h, 1, sizeof h, file_) == sizeof h;
    }
    uint8_t h[44];
    std::memcpy(h, "RIFF", 4);
    put32(h + 4, 36 + dataBytes);
    std::memcpy(h + 8, "WAVEfmt ", 8);
    put32(h + 16, 16);
    put16(h + 20, 1);  // integer PCM
    put16(h + 22, static_cast<uint16_t>(channels_));
    put32(h + 24, static_cast<uint32_t>(sampleRate_));
    put32(h + 28, static_cast<uint32_t>(sampleRate_) * channels_ * bytes);
    put16(h + 32, static_cast<uint16_t>(channels_ * bytes));
    put16(h + 34, static_cast<uint16_t>(8 * bytes));
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
    // Full scale of the integer formats; the dither is one least significant bit at either size.
    const float scale = bits_ == 16 ? 32767.0f : 8388607.0f;
    const float lowest = bits_ == 16 ? -32768.0f : -8388608.0f;
    const int bytes = bytesPerSample();
    const int total = numFrames * channels_;
    const int perChunk = static_cast<int>(sizeof buffer_) / bytes;
    for (int start = 0; start < total; start += perChunk) {
        const int n = std::min(perChunk, total - start);
        for (int i = 0; i < n; ++i) {
            float v = interleaved[start + i];
            if (!std::isfinite(v)) v = 0.0f;
            uint8_t* p = buffer_ + i * bytes;
            if (float_) {
                uint32_t bits;
                std::memcpy(&bits, &v, 4);
                put32(p, bits);
                continue;
            }
            const float s = std::clamp(v * scale + dither(), lowest, scale);
            const int32_t q = static_cast<int32_t>(std::lrint(s));
            p[0] = q & 0xFF;
            p[1] = (q >> 8) & 0xFF;
            if (bytes == 3) p[2] = (q >> 16) & 0xFF;
        }
        if (std::fwrite(buffer_, bytes, n, file_) != static_cast<size_t>(n)) {
            ok_ = false;
            return false;
        }
    }
    frames_ += numFrames;
    return true;
}

bool WavWriter::close() {
    if (!file_) return false;
    const int64_t bytes = frames_ * channels_ * bytesPerSample();
    bool ok = ok_ && bytes <= 0xFFFFFFFFll - 50;
    if (ok) ok = std::fseek(file_, 0, SEEK_SET) == 0 && writeHeader(static_cast<uint32_t>(bytes));
    ok = (std::fflush(file_) == 0) && ok;
    ok = (std::fclose(file_) == 0) && ok;
    file_ = nullptr;
    return ok;
}

}  // namespace ps
