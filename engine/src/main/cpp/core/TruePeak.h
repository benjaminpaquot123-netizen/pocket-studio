#pragma once

#include <algorithm>
#include <array>
#include <cmath>

namespace ps {

// True peak (ITU-R BS.1770-4, annex 2): the signal read 4 times per sample through a linear-phase
// interpolator, here a Kaiser-windowed sinc of 32 taps per phase (the standard's own has 12), flat
// to 0.002 dB up to 20 kHz at 48 kHz: it reads as ffmpeg's ebur128 (libswresample's sinc) does,
// which the bench and the app's report are checked against. 16 taps read noisy material 0.2 to
// 0.4 dB under it (tried: the limiter then lets -0.8 dBTP through). The peaks between the samples
// are what a converter (or an AAC decoder) rebuilds and what clips after a lossy encode. Shared
// coefficients, a few floats of state per channel: real-time safe. Two readers per channel run on
// the master (the limiter's and the meter's): the taps are the chain's main cost, so both only
// run the filter near a peak that matters (pushGated, pushRefinedGated).
class TruePeakFilter {
public:
    static constexpr int kTaps = 32;              // per phase
    static constexpr int kDelay = kTaps / 2;      // frames: push() describes the sample this far back
    static constexpr int kPhases = 3;             // the points at 1/4, 2/4 and 3/4 of a sample

    // The interpolator, built once when the program loads (a few hundred std::sin calls: never in
    // the callback, and no first-use guard to take there).
    static const TruePeakFilter& shared();

    const std::array<std::array<float, kTaps>, kPhases>& coefs() const { return coefs_; }

    TruePeakFilter() {
        constexpr double kPi = 3.14159265358979323846;
        constexpr double kBeta = 8.0;  // with 32 taps: 0.002 dB up to 20 kHz
        auto bessel0 = [](double x) {
            double sum = 1.0, term = 1.0;
            for (int k = 1; k < 32; ++k) {
                term *= (x / (2.0 * k)) * (x / (2.0 * k));
                sum += term;
            }
            return sum;
        };
        const double norm = bessel0(kBeta);
        for (int p = 0; p < kPhases; ++p) {
            const double frac = (p + 1) / 4.0;
            double sum = 0.0;
            std::array<double, kTaps> h{};
            for (int j = 0; j < kTaps; ++j) {
                // Tap j weighs the sample j frames back; the point read sits kDelay - frac frames back.
                const double t = (kDelay - frac) - j;
                const double sinc = std::sin(kPi * t) / (kPi * t);
                const double w = t / kDelay;
                const double kaiser = std::fabs(w) < 1.0 ? bessel0(kBeta * std::sqrt(1.0 - w * w)) / norm : 0.0;
                h[static_cast<size_t>(j)] = sinc * kaiser;
                sum += h[static_cast<size_t>(j)];
            }
            for (int j = 0; j < kTaps; ++j) coefs_[static_cast<size_t>(p)][static_cast<size_t>(j)] = static_cast<float>(h[static_cast<size_t>(j)] / sum);
        }
    }
private:
    std::array<std::array<float, kTaps>, kPhases> coefs_{};
};

inline const TruePeakFilter kTruePeakFilter{};
inline const TruePeakFilter& TruePeakFilter::shared() { return kTruePeakFilter; }

// One channel's true-peak reader. push() takes the newest sample and returns the largest
// magnitude over the sample kDelay frames back and the three points read after it (the BS.1770
// meter). pushRefined() also draws a parabola through each local peak of the 4x points and its
// two neighbours: a peak between two points is read within a few thousandths of a dB instead of
// up to 0.4 dB under (at 18 kHz), what a limiter needs to hold a ceiling for any reader.
class TruePeakReader {
public:
    void reset() {
        history_.fill(0.0f);
        pos_ = 0;
        before_ = last_ = 0.0f;
    }
    float push(float x) {
        float points[4];
        read(x, points);
        float peak = 0.0f;
        for (float p : points) peak = std::max(peak, std::fabs(p));
        return peak;
    }
    float pushRefined(float x) {
        float points[4];
        read(x, points);
        float peak = 0.0f;
        for (float p : points) {
            peak = std::max(peak, std::max(std::fabs(p), vertex(before_, last_, p)));
            before_ = last_;
            last_ = p;
        }
        return peak;
    }
    // push() and pushRefined() with the filter skipped while it cannot matter: the three points
    // between two samples are read only when one of those samples is over `floor`, else the
    // sample's own magnitude comes back. A peak between two samples of music never stands over
    // twice their size (only a pure tone near 20 kHz could), so a floor at 0.4 of the peak sought
    // (the meter's highest so far, the limiter's aim) keeps the reading exact where it counts; the
    // filter runs on the few samples near a peak instead of all of them.
    float pushGated(float x, float floor) {
        float points[4];
        if (!readGated(x, floor, points)) return std::fabs(points[0]);
        float peak = 0.0f;
        for (float p : points) peak = std::max(peak, std::fabs(p));
        return peak;
    }
    float pushRefinedGated(float x, float floor) {
        float points[4];
        if (!readGated(x, floor, points)) {
            before_ = last_ = points[0];  // flat neighbours for the next parabola
            return std::fabs(points[0]);
        }
        float peak = 0.0f;
        for (float p : points) {
            peak = std::max(peak, std::max(std::fabs(p), vertex(before_, last_, p)));
            before_ = last_;
            last_ = p;
        }
        return peak;
    }

private:
    static constexpr int kTaps = TruePeakFilter::kTaps;
    // The 4x points of the sample kDelay frames back: the sample itself, then its three readings.
    void read(float x, float points[4]) {
        if (++pos_ == kTaps) pos_ = 0;
        // Each sample written twice: the last kTaps samples are always contiguous.
        history_[static_cast<size_t>(pos_)] = x;
        history_[static_cast<size_t>(pos_ + kTaps)] = x;
        const float* newest = history_.data() + pos_ + kTaps;  // newest[-j]: j frames back
        const auto& c = TruePeakFilter::shared().coefs();
        points[0] = newest[-TruePeakFilter::kDelay];
        for (int p = 0; p < TruePeakFilter::kPhases; ++p) {
            float v = 0.0f;
            for (int j = 0; j < kTaps; ++j) v += c[static_cast<size_t>(p)][static_cast<size_t>(j)] * newest[-j];
            points[p + 1] = v;
        }
    }
    // read() unless both samples around the points are under `floor`: then only points[0] (the
    // sample kDelay frames back) is set and false comes back.
    bool readGated(float x, float floor, float points[4]) {
        if (++pos_ == kTaps) pos_ = 0;
        history_[static_cast<size_t>(pos_)] = x;
        history_[static_cast<size_t>(pos_ + kTaps)] = x;
        const float* newest = history_.data() + pos_ + kTaps;
        points[0] = newest[-TruePeakFilter::kDelay];
        if (std::fabs(points[0]) <= floor && std::fabs(newest[-TruePeakFilter::kDelay + 1]) <= floor) return false;
        const auto& c = TruePeakFilter::shared().coefs();
        for (int p = 0; p < TruePeakFilter::kPhases; ++p) {
            float v = 0.0f;
            for (int j = 0; j < kTaps; ++j) v += c[static_cast<size_t>(p)][static_cast<size_t>(j)] * newest[-j];
            points[p + 1] = v;
        }
        return true;
    }
    // The top of the parabola through three points whose middle one is a local peak (of either
    // sign); its own magnitude otherwise.
    static float vertex(float a, float b, float c) {
        const float s = b < 0.0f ? -1.0f : 1.0f;
        const float A = s * a, B = s * b, C = s * c;
        if (B < A || B < C) return B;
        const float curve = A - 2.0f * B + C;
        if (!(curve < 0.0f)) return B;
        return std::min(B - (A - C) * (A - C) / (8.0f * curve), 1.1f * B);
    }
    std::array<float, 2 * kTaps> history_{};
    int pos_ = 0;
    float before_ = 0.0f, last_ = 0.0f;  // the last two 4x points
};

}  // namespace ps
