#pragma once

#include <cstdint>
#include <cstdio>

namespace ps {

// Streams interleaved float audio to a 24-bit (or 16-bit) PCM WAV file with TPDF dither of one
// least significant bit, or to a 32-bit float one (no dither, nothing clipped above 0 dBFS).
// Not for the audio thread: it does file I/O.
class WavWriter {
public:
    WavWriter() = default;
    ~WavWriter();
    WavWriter(const WavWriter&) = delete;
    WavWriter& operator=(const WavWriter&) = delete;

    bool open(const char* path, int sampleRate, int channels, bool floatSamples = false) {
        return openBits(path, sampleRate, channels, floatSamples ? 32 : 24);
    }
    // `bits`: 16 or 24 (integer PCM, dithered) or 32 (float).
    bool openBits(const char* path, int sampleRate, int channels, int bits);
    bool write(const float* interleaved, int numFrames);
    // Patches the header sizes and closes the file; false on any I/O error.
    bool close();

    int64_t framesWritten() const { return frames_; }

private:
    bool writeHeader(uint32_t dataBytes);
    float dither();

    std::FILE* file_ = nullptr;
    int sampleRate_ = 48000;
    int channels_ = 2;
    int64_t frames_ = 0;
    bool ok_ = true;
    bool float_ = false;
    int bits_ = 24;
    int bytesPerSample() const { return bits_ / 8; }
    uint32_t seed_ = 0x9E3779B9u;
    uint8_t buffer_[4096 * 3];
};

}  // namespace ps
