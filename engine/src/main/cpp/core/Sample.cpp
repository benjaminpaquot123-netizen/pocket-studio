#include "Sample.h"

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

}  // namespace ps
