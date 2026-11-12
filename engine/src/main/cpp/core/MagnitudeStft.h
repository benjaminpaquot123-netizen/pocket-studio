#pragma once

// Streaming short-time magnitude spectrum for the analysers (onsets, tempo, key), on the
// Signalsmith Linear real FFT (MIT). Off the audio thread only: it allocates.
// Frame k covers the `window` samples ending at (k + 1) * hop (zeros before the start), Hann window.

#include <cmath>
#include <complex>
#include <vector>

#include "signalsmith-linear/fft.h"

namespace ps {

class MagnitudeStft {
public:
    MagnitudeStft(int window, int hop)
        : window_(window), hop_(hop), fft_(static_cast<size_t>(window)), ring_(static_cast<size_t>(window), 0.0f),
          frame_(static_cast<size_t>(window)), spectrum_(static_cast<size_t>(window / 2)), mag_(static_cast<size_t>(window / 2 + 1)),
          hann_(static_cast<size_t>(window)) {
        for (int i = 0; i < window; ++i) hann_[static_cast<size_t>(i)] = 0.5f - 0.5f * std::cos(2.0f * static_cast<float>(M_PI) * (static_cast<float>(i) + 0.5f) / static_cast<float>(window));
    }

    int window() const { return window_; }
    int hop() const { return hop_; }
    int bins() const { return window_ / 2 + 1; }  // 0 (DC) .. window/2 (Nyquist, always 0 here)

    // Adds samples; calls fn(magnitudes, bins) for every completed hop.
    template <typename Fn>
    void push(const float* x, int n, Fn&& fn) {
        for (int i = 0; i < n; ++i) {
            ring_[static_cast<size_t>(pos_)] = std::isfinite(x[i]) ? x[i] : 0.0f;
            pos_ = (pos_ + 1) % window_;
            if (++fill_ == hop_) {
                fill_ = 0;
                analyse();
                fn(mag_.data(), bins());
            }
        }
    }
    // Pads the last partial hop with zeros.
    template <typename Fn>
    void flush(Fn&& fn) {
        if (fill_ == 0) return;
        const float zero = 0.0f;
        while (fill_ != 0) push(&zero, 1, fn);
    }

private:
    void analyse() {
        for (int i = 0; i < window_; ++i) frame_[static_cast<size_t>(i)] = ring_[static_cast<size_t>((pos_ + i) % window_)] * hann_[static_cast<size_t>(i)];
        fft_.fft(frame_.data(), spectrum_.data());
        mag_[0] = std::fabs(spectrum_[0].real());
        for (size_t k = 1; k < spectrum_.size(); ++k) mag_[k] = std::abs(spectrum_[k]);
        mag_[spectrum_.size()] = 0.0f;
    }

    int window_, hop_;
    signalsmith::linear::RealFFT<float> fft_;
    std::vector<float> ring_, frame_;
    std::vector<std::complex<float>> spectrum_;
    std::vector<float> mag_, hann_;
    int pos_ = 0;   // oldest sample in ring_
    int fill_ = 0;  // samples since the last frame
};

}  // namespace ps
