#pragma once

#include <array>
#include <atomic>
#include <cmath>

#include "Smoother.h"

namespace ps {

// Channel strips (volume, pan, mute, solo, meter) summed to a stereo master with a peak
// limiter. Mono (or stereo) sources in, interleaved stereo out. Real-time safe.
class Mixer {
public:
    static constexpr int kChannels = 4;  // 0 = drums, 1 = acid bass, 2 = synth, 3 = remix stems (stereo)
    static constexpr float kCeiling = 0.89f;  // -1 dBFS

    void prepare(int sampleRate);
    void setVolume(int ch, float v);   // 0..1 (0.8 = unity, up to +2 dB)
    void setPan(int ch, float pan);    // -1..1
    void setMute(int ch, bool on);
    void setSolo(int ch, bool on);

    // Mixes the channel buffers (mono, numFrames each) plus an already-stereo-free
    // `direct` mono signal (metronome, test tone) into interleaved `out`.
    void process(const float* const* channels, const float* direct, float* out, int numFrames, int outChannels) {
        process(channels, nullptr, direct, out, numFrames, outChannels);
    }
    // `rights[ch]` non-null makes channel `ch` stereo (channels[ch] = left); pan then balances.
    void process(const float* const* channels, const float* const* rights, const float* direct, float* out, int numFrames,
                 int outChannels);

    // Peak since last read, per channel and for the master (UI meters).
    float takeChannelPeak(int ch) { return peaks_[static_cast<size_t>(ch)].exchange(0.0f, std::memory_order_acq_rel); }
    float takeMasterPeak() { return masterPeak_.exchange(0.0f, std::memory_order_acq_rel); }
    // Current limiter gain (1 = not limiting).
    float limiterGain() const { return limiterGainOut_.load(std::memory_order_relaxed); }

private:
    struct Strip {
        float volume = 0.8f;
        float pan = 0.0f;
        bool mute = false;
        bool solo = false;
        Smoother gainL, gainR;
    };
    void updateTargets();
    static void storeMax(std::atomic<float>& a, float v);

    std::array<Strip, kChannels> strips_{};
    std::array<std::atomic<float>, kChannels> peaks_{};
    std::atomic<float> masterPeak_{0.0f};
    std::atomic<float> limiterGainOut_{1.0f};
    float envelope_ = 0.0f;
    float release_ = 0.999f;
};

}  // namespace ps
