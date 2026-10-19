#include "Mixer.h"

#include <algorithm>

namespace ps {

void Mixer::prepare(int sampleRate) {
    for (auto& s : strips_) {
        s.gainL.configure(static_cast<float>(sampleRate), 15.0f);
        s.gainR.configure(static_cast<float>(sampleRate), 15.0f);
    }
    release_ = std::exp(-1.0f / (0.12f * static_cast<float>(sampleRate)));  // 120 ms release
    updateTargets();
    for (auto& s : strips_) {
        s.gainL.snap(s.gainL.target());
        s.gainR.snap(s.gainR.target());
    }
}

void Mixer::setVolume(int ch, float v) {
    if (ch < 0 || ch >= kChannels) return;
    strips_[static_cast<size_t>(ch)].volume = std::clamp(v, 0.0f, 1.0f);
    updateTargets();
}

void Mixer::setPan(int ch, float pan) {
    if (ch < 0 || ch >= kChannels) return;
    strips_[static_cast<size_t>(ch)].pan = std::clamp(pan, -1.0f, 1.0f);
    updateTargets();
}

void Mixer::setMute(int ch, bool on) {
    if (ch < 0 || ch >= kChannels) return;
    strips_[static_cast<size_t>(ch)].mute = on;
    updateTargets();
}

void Mixer::setSolo(int ch, bool on) {
    if (ch < 0 || ch >= kChannels) return;
    strips_[static_cast<size_t>(ch)].solo = on;
    updateTargets();
}

void Mixer::updateTargets() {
    bool anySolo = false;
    for (const auto& s : strips_) anySolo |= s.solo;
    for (auto& s : strips_) {
        const bool audible = !s.mute && (!anySolo || s.solo);
        // Fader: 0.8 = unity gain, 1.0 = +2 dB, quadratic below for a natural feel.
        const float gain = audible ? (s.volume / 0.8f) * (s.volume / 0.8f) : 0.0f;
        // Equal-power pan; centre = -3 dB per side, compensated so centre stays at unity.
        const float angle = (s.pan + 1.0f) * 0.25f * 3.14159265f;
        s.gainL.setTarget(gain * std::cos(angle) * 1.41421356f);
        s.gainR.setTarget(gain * std::sin(angle) * 1.41421356f);
    }
}

void Mixer::storeMax(std::atomic<float>& a, float v) {
    float prev = a.load(std::memory_order_relaxed);
    while (v > prev && !a.compare_exchange_weak(prev, v, std::memory_order_acq_rel)) {
    }
}

void Mixer::process(const float* const* channels, const float* const* rights, const float* direct, float* out, int numFrames,
                    int outChannels) {
    std::array<float, kChannels> blockPeaks{};
    float masterPeak = 0.0f;
    float minGain = 1.0f;
    for (int i = 0; i < numFrames; ++i) {
        float l = direct ? direct[i] : 0.0f;
        float r = l;
        for (int ch = 0; ch < kChannels; ++ch) {
            auto& s = strips_[static_cast<size_t>(ch)];
            const float x = channels[ch][i];
            const float xr = rights && rights[ch] ? rights[ch][i] : x;
            const float gl = s.gainL.next();
            const float gr = s.gainR.next();
            l += x * gl;
            r += xr * gr;
            blockPeaks[static_cast<size_t>(ch)] =
                std::max(blockPeaks[static_cast<size_t>(ch)], std::max(std::fabs(x) * gl, std::fabs(xr) * gr));
        }
        // Peak limiter: instant attack, smooth release, ceiling at -1 dBFS.
        const float peak = std::max(std::fabs(l), std::fabs(r));
        envelope_ = peak > envelope_ ? peak : envelope_ * release_ + peak * (1.0f - release_);
        const float gain = envelope_ > kCeiling ? kCeiling / envelope_ : 1.0f;
        minGain = std::min(minGain, gain);
        l *= gain;
        r *= gain;
        masterPeak = std::max(masterPeak, std::max(std::fabs(l), std::fabs(r)));
        if (outChannels == 1) {
            out[i] = 0.5f * (l + r);
        } else {
            out[i * outChannels] = l;
            out[i * outChannels + 1] = r;
            for (int c = 2; c < outChannels; ++c) out[i * outChannels + c] = 0.0f;
        }
    }
    for (int ch = 0; ch < kChannels; ++ch) storeMax(peaks_[static_cast<size_t>(ch)], blockPeaks[static_cast<size_t>(ch)]);
    storeMax(masterPeak_, masterPeak);
    limiterGainOut_.store(minGain, std::memory_order_relaxed);
}

}  // namespace ps
