#include "OnsetDetector.h"

#include <algorithm>
#include <cmath>

extern "C" {
#include "aubio_priv.h"
#include "fvec.h"
#include "onset/onset.h"
}

namespace ps {

std::vector<int64_t> detectOnsets(const float* mono, int64_t frames, int sampleRate, const OnsetOptions& options) {
    std::vector<int64_t> onsets{0};
    if (!mono || frames <= 0 || sampleRate <= 0) return onsets;
    constexpr uint_t kWindow = 1024, kHop = 256;
    aubio_onset_t* o = new_aubio_onset("hfc", kWindow, kHop, static_cast<uint_t>(sampleRate));
    fvec_t* in = new_fvec(kHop);
    fvec_t* out = new_fvec(1);
    if (!o || !in || !out) {
        if (o) del_aubio_onset(o);
        if (in) del_fvec(in);
        if (out) del_fvec(out);
        return onsets;
    }
    // Sensitivity 0..1 maps to aubio's peak-picking threshold 0.9..0.05 (lower = more onsets).
    const float s = std::clamp(options.sensitivity, 0.0f, 1.0f);
    aubio_onset_set_threshold(o, 0.9f - 0.85f * s);
    aubio_onset_set_minioi_s(o, std::max(0.01f, options.minGapSeconds));
    aubio_onset_set_silence(o, -60.0f);

    const auto minGap = static_cast<int64_t>(options.minGapSeconds * sampleRate);
    std::vector<std::pair<float, int64_t>> found;  // (strength, frame)
    for (int64_t pos = 0; pos < frames; pos += kHop) {
        for (uint_t i = 0; i < kHop; ++i) {
            const int64_t f = pos + i;
            in->data[i] = f < frames && std::isfinite(mono[f]) ? mono[f] : 0.0f;
        }
        aubio_onset_do(o, in, out);
        if (out->data[0] > 0.0f) {
            const auto at = static_cast<int64_t>(aubio_onset_get_last(o));
            if (at > 0 && at < frames && at - onsets.back() >= minGap && (found.empty() || at - found.back().second >= minGap)) {
                found.emplace_back(aubio_onset_get_descriptor(o), at);
            }
        }
    }
    del_aubio_onset(o);
    del_fvec(in);
    del_fvec(out);

    // Too many: keep the strongest, in time order.
    const auto keep = static_cast<size_t>(std::max(0, options.maxSlices - 1));
    if (found.size() > keep) {
        std::partial_sort(found.begin(), found.begin() + static_cast<std::ptrdiff_t>(keep), found.end(),
                          [](const auto& a, const auto& b) { return a.first > b.first; });
        found.resize(keep);
    }
    for (const auto& f : found) onsets.push_back(f.second);
    std::sort(onsets.begin(), onsets.end());
    return onsets;
}

}  // namespace ps
