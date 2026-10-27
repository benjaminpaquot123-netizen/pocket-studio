#include "Sample.h"

#include <algorithm>
#include <cmath>

#include <samplerate.h>

namespace ps {

Sample* Sample::create(const float* interleaved, int64_t frames, int channels, int sourceRate, int targetRate) {
    if (!interleaved || frames <= 0 || channels < 1 || channels > 2 || sourceRate <= 0 || targetRate <= 0) return nullptr;
    auto* s = new Sample();
    s->channels_ = channels;
    if (sourceRate == targetRate) {
        s->data_.assign(interleaved, interleaved + frames * channels);
        s->frames_ = frames;
    } else {
        const double ratio = static_cast<double>(targetRate) / sourceRate;
        const auto outFrames = static_cast<int64_t>(std::ceil(frames * ratio)) + 16;
        s->data_.resize(static_cast<size_t>(outFrames * channels));
        SRC_DATA d{};
        d.data_in = interleaved;
        d.input_frames = static_cast<long>(frames);
        d.data_out = s->data_.data();
        d.output_frames = static_cast<long>(outFrames);
        d.src_ratio = ratio;
        if (src_simple(&d, SRC_SINC_BEST_QUALITY, channels) != 0 || d.output_frames_gen <= 0) {
            delete s;
            return nullptr;
        }
        s->frames_ = d.output_frames_gen;
        s->data_.resize(static_cast<size_t>(s->frames_ * channels));
    }
    for (float& v : s->data_) {
        if (!std::isfinite(v)) v = 0.0f;
    }
    return s;
}

Sample* Sample::createCompact(const float* interleaved, int64_t frames, int channels, int sourceRate, int targetRate) {
    Sample* s = create(interleaved, frames, channels, sourceRate, targetRate);
    if (!s) return nullptr;
    // Rounded to the nearest step: a 16-bit recording comes back exactly as it was. A zone over
    // full scale (a decoded MP3 or OGG, the resampler's peaks between samples) is stored scaled
    // down with its scale, never clipped (audit 13: the loud notes crackled).
    float peak = 0.0f;
    for (float v : s->data_) peak = std::max(peak, std::fabs(v));
    constexpr float kFull = 32767.0f / 32768.0f;
    const float down = peak > kFull ? kFull / peak : 1.0f;
    if (down < 1.0f) s->pcmScale_ = 1.0f / (32768.0f * down);
    s->pcm16_.resize(s->data_.size());
    for (size_t i = 0; i < s->data_.size(); ++i) {
        const float v = std::clamp(s->data_[i] * down, -1.0f, kFull) * 32768.0f;
        s->pcm16_[i] = static_cast<int16_t>(std::lrint(v));
    }
    std::vector<float>().swap(s->data_);
    if (s->pcm16_.empty()) {
        delete s;
        return nullptr;
    }
    return s;
}

}  // namespace ps
