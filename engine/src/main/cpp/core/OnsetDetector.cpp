#include "OnsetDetector.h"

#include <algorithm>
#include <cmath>

#include "MagnitudeStft.h"

// Attack detection, written for Pocket Studio from the published method (J. P. Bello et al.,
// "A Tutorial on Onset Detection in Music Signals", IEEE TSAP 2005; P. Brossier, "Automatic
// annotation of musical audio for interactive applications", PhD thesis, 2006), tuned so slices
// land where the previous detector (aubio, GPL-3.0) put them (engine/host-tests/analysis_tests.inc):
//  1. detection function: high-frequency content (sum over bins of k * magnitude) of 1024-sample
//     Hann frames every 256 samples; attacks are broadband, so it jumps at each hit;
//  2. adaptive threshold: the function minus the local median and a sensitivity-scaled local
//     mean (5 frames back, 1 ahead); an attack is a local maximum above zero, out of silence;
//  3. close attacks (under the minimum gap) keep the stronger; over the cap, the strongest stay.

namespace ps {

namespace {

constexpr int kWindow = 1024, kHop = 256;
constexpr int kPast = 5, kAhead = 1;         // threshold window, in frames
constexpr float kSilence = 1e-6f;            // -60 dB (mean square of a hop)
// An attack is seen best by the frame whose window centre has just passed it.
constexpr int64_t kLatency = 2 * kHop;

}  // namespace

std::vector<int64_t> detectOnsets(const float* mono, int64_t frames, int sampleRate, const OnsetOptions& options) {
    std::vector<int64_t> onsets{0};
    if (!mono || frames <= 0 || sampleRate <= 0) return onsets;

    // 1. Detection function, and each hop's level for the silence gate.
    MagnitudeStft stft(kWindow, kHop);
    std::vector<float> hfc;
    hfc.reserve(static_cast<size_t>(frames / kHop + 2));
    auto onFrame = [&](const float* mag, int bins) {
        double sum = 0;
        for (int k = 1; k < bins; ++k) sum += static_cast<double>(k) * mag[k];
        hfc.push_back(static_cast<float>(sum));
    };
    for (int64_t pos = 0; pos < frames; pos += 65536) stft.push(mono + pos, static_cast<int>(std::min<int64_t>(65536, frames - pos)), onFrame);
    stft.flush(onFrame);
    const int n = static_cast<int>(hfc.size());
    std::vector<float> level(static_cast<size_t>(n), 0.0f);
    for (int t = 0; t < n; ++t) {
        double e = 0;
        for (int64_t f = static_cast<int64_t>(t) * kHop; f < std::min<int64_t>(frames, static_cast<int64_t>(t + 1) * kHop); ++f) {
            if (std::isfinite(mono[f])) e += static_cast<double>(mono[f]) * mono[f];
        }
        level[static_cast<size_t>(t)] = static_cast<float>(e / kHop);
    }
    float peak = 0;
    for (float v : hfc) peak = std::max(peak, v);
    if (!(peak > 0) || !std::isfinite(peak)) return onsets;
    for (float& v : hfc) v /= peak;

    // 2. Height over the adaptive threshold. Sensitivity 0..1 lowers the mean's weight 0.9..0.05.
    const float lambda = 0.9f - 0.85f * std::clamp(options.sensitivity, 0.0f, 1.0f);
    std::vector<float> over(static_cast<size_t>(n), 0.0f), win;
    for (int t = 0; t < n; ++t) {
        win.assign(hfc.begin() + std::max(0, t - kPast), hfc.begin() + std::min(n, t + kAhead + 1));
        double mean = 0;
        for (float w : win) mean += w;
        mean /= static_cast<double>(win.size());
        std::nth_element(win.begin(), win.begin() + static_cast<std::ptrdiff_t>(win.size() / 2), win.end());
        over[static_cast<size_t>(t)] = hfc[static_cast<size_t>(t)] - win[win.size() / 2] - lambda * static_cast<float>(mean);
    }

    const auto minGap = static_cast<int64_t>(std::max(0.0f, options.minGapSeconds) * static_cast<float>(sampleRate));
    std::vector<std::pair<float, int64_t>> found;  // (height over the threshold, frame)
    for (int t = 1; t < n; ++t) {
        const float h = over[static_cast<size_t>(t)];
        if (!(h > 0.0f) || h <= over[static_cast<size_t>(t - 1)] || (t + 1 < n && h < over[static_cast<size_t>(t + 1)])) continue;
        float loud = 0;
        for (int k = std::max(0, t - 2); k <= std::min(n - 1, t + 1); ++k) loud = std::max(loud, level[static_cast<size_t>(k)]);
        if (loud < kSilence) continue;
        const int64_t at = static_cast<int64_t>(t + 1) * kHop - kLatency;
        if (at <= 0 || at >= frames || at - onsets.back() < minGap) continue;
        if (!found.empty() && at - found.back().second < minGap) {
            if (h > found.back().first) found.back() = {h, at};  // the stronger of two close attacks
            continue;
        }
        found.emplace_back(h, at);
    }

    // 3. Too many: keep the strongest, in time order.
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
