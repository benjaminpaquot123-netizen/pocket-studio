#pragma once

#include <array>
#include <atomic>
#include <cmath>

#include "Effects.h"
#include "Smoother.h"

namespace ps {

// Channel strips (inserts, volume, pan, mute, solo, two sends, meter), two send buses
// (0 = reverb, 1 = tempo delay), master inserts and a peak limiter. Mono or stereo sources in,
// interleaved stereo out. prepare() may allocate (stream closed); everything else is real-time safe.
class Mixer {
public:
    // 0 = drums, 1 = acid bass, 2 = synth (instrument track 1), 3 = remix stems (stereo),
    // 5..11 = instrument tracks 2..8, 12..15 = audio tracks (stereo). Index 4 is the master (insert
    // commands, saved automation): its strip is never fed.
    static constexpr int kChannels = 16;
    static constexpr int kInserts = 4;   // per channel, and on the master
    static constexpr int kMaster = 4;    // insert "channel" index of the master
    static constexpr int kBuses = 2;
    static constexpr float kCeiling = 0.89f;  // -1 dBFS

    void prepare(int sampleRate);
    void setVolume(int ch, float v);   // 0..1 (0.8 = unity, up to +2 dB)
    void setPan(int ch, float pan);    // -1..1
    void setMute(int ch, bool on);
    void setSolo(int ch, bool on);
    void setInsertType(int ch, int slot, int type);           // ch 0..kMaster
    void setInsertParam(int ch, int slot, int param, float v);
    void setSend(int ch, int bus, float level);               // 0..1, post-fader
    void setBusParam(int bus, int param, float v);
    void setStepFrames(double framesPerSixteenth) {
        delay_.setStepFrames(framesPerSixteenth);
        perf_.setStepFrames(framesPerSixteenth);
    }
    void setPerformance(int param, float v) { perf_.setParam(param, v); }
    // A sequencer step lands `offset` frames into the next process() call (sidechain pumps).
    void sidechainEvent(int offset, bool kick, bool beat) {
        if (events_ < static_cast<int>(eventOffset_.size())) {
            eventOffset_[static_cast<size_t>(events_)] = offset;
            eventKick_[static_cast<size_t>(events_)] = kick;
            eventBeat_[static_cast<size_t>(events_)] = beat;
            ++events_;
        }
    }

    void process(const float* const* channels, const float* direct, float* out, int numFrames, int outChannels) {
        process(channels, nullptr, direct, out, numFrames, outChannels);
    }
    // `rights[ch]` non-null makes channel `ch` stereo (channels[ch] = left); pan then balances.
    // A null `channels[ch]` skips the channel.
    void process(const float* const* channels, const float* const* rights, const float* direct, float* out, int numFrames,
                 int outChannels);

    // Peak since last read, per channel and for the master (UI meters).
    float takeChannelPeak(int ch) { return peaks_[static_cast<size_t>(ch)].exchange(0.0f, std::memory_order_acq_rel); }
    float takeMasterPeak() { return masterPeak_.exchange(0.0f, std::memory_order_acq_rel); }
    // Current limiter gain (1 = not limiting).
    float limiterGain() const { return limiterGainOut_.load(std::memory_order_relaxed); }
    const InsertSlot& insert(int ch, int slot) const { return inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)]; }

private:
    static constexpr int kChunk = 256;
    struct Strip {
        float volume = 0.8f;
        float pan = 0.0f;
        bool mute = false;
        bool solo = false;
        Smoother gainL, gainR;
        std::array<float, kBuses> send{};
        std::array<Smoother, kBuses> sendGain;
    };
    void updateTargets();
    void processChunk(const float* const* channels, const float* const* rights, const float* direct, float* out, int n,
                      int outChannels, int offset);
    static void storeMax(std::atomic<float>& a, float v);

    std::array<Strip, kChannels> strips_{};
    std::array<std::array<InsertSlot, kInserts>, kChannels> inserts_{};
    std::array<int, kChannels> activeInserts_{};  // count of non-empty slots
    Reverb reverb_;
    TempoDelay delay_;
    PerformanceFx perf_;
    std::array<float, kChunk> chL_{}, chR_{}, mL_{}, mR_{};
    std::array<std::array<float, kChunk>, kBuses> busL_{}, busR_{};
    std::array<std::atomic<float>, kChannels> peaks_{};
    std::atomic<float> masterPeak_{0.0f};
    std::atomic<float> limiterGainOut_{1.0f};
    std::array<int, 32> eventOffset_{};
    std::array<bool, 32> eventKick_{}, eventBeat_{};
    int events_ = 0;
    float envelope_ = 0.0f;
    float release_ = 0.999f;
};

}  // namespace ps
