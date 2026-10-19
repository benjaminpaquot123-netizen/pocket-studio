#pragma once

#include <cstdint>
#include <cstdio>

namespace ps {

// Streams interleaved float audio to a 24-bit PCM WAV file with TPDF dither.
// Not for the audio thread: it does file I/O.
class WavWriter {
public:
    WavWriter() = default;
    ~WavWriter();
    WavWriter(const WavWriter&) = delete;
    WavWriter& operator=(const WavWriter&) = delete;

    bool open(const char* path, int sampleRate, int channels);
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
    uint32_t seed_ = 0x9E3779B9u;
    uint8_t buffer_[4096 * 3];
};

}  // namespace ps
