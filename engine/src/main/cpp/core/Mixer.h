#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <memory>

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
    // commands, saved automation): its strip is never fed. 16..31 = the 16 drum pads' own strips
    // (their effects on one pad): they feed the drums' strip, not the master, and follow its mute
    // and solo.
    static constexpr int kChannels = 32;
    static constexpr int kFirstPad = 16;
    static constexpr int kPads = 16;
    static constexpr int kDrums = 0;
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
    void setMacro(int ch, int macro, float v);                // TrackMacros: colour, dirt, motion, space
    void setBusParam(int bus, int param, float v);
    // The master: a gain before the limiter (linear, 0..4), the limiter on or off (a pre-master
    // export keeps its headroom), and its inserts on or off (stems: each strip without the master's
    // processing, so the stems add back up to the mix).
    void setMasterGain(float gain) { masterGain_.setTarget(std::clamp(std::isfinite(gain) ? gain : 1.0f, 0.0f, 4.0f)); }
    void setLimiter(bool on) { limiterOn_ = on; }
    // Every fader, pan and send gain at its target at once (an offline render, before its first
    // beat: a muted or not-soloed strip must not bleed in while its gain glides down).
    void settle() {
        for (auto& s : strips_) {
            s.gainL.snap(s.gainL.target());
            s.gainR.snap(s.gainR.target());
            for (auto& g : s.sendGain) g.snap(g.target());
        }
        masterGain_.snap(masterGain_.target());
    }
    void setMasterFx(bool on) { masterFxOn_ = on; }
    bool limiterOn() const { return limiterOn_; }
    // The send delay's time in frames: an export's tail waits at least this long in silence for a repeat.
    float echoFrames() const { return delay_.delayFrames(); }
    bool echoInUse() const {
        for (const auto& s : strips_) {
            if (s.sendGain[1].target() > 0.0f) return true;
        }
        return false;
    }
    void setStepFrames(double framesPerSixteenth) {
        delay_.setStepFrames(framesPerSixteenth);
        perf_.setStepFrames(framesPerSixteenth);
        // The rhythmic effects know the beat's length at once (not only after two beats).
        for (auto& strip : inserts_) {
            for (auto& slot : strip) slot.setStepFrames(framesPerSixteenth);
        }
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

    /** True once after a NaN was caught and silenced (for the reports). */
    bool takeBlewUp() { return blewUp_.exchange(false, std::memory_order_relaxed); }

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
    // Large (each chorus holds its delay lines, about 20 kB): on the heap, allocated once with the
    // mixer, so an engine never weighs megabytes on a thread's stack.
    using InsertBank = std::array<std::array<InsertSlot, kInserts>, kChannels>;
    using MacroBank = std::array<TrackMacros, kChannels>;
    std::unique_ptr<InsertBank> insertBank_ = std::make_unique<InsertBank>();
    std::unique_ptr<MacroBank> macroBank_ = std::make_unique<MacroBank>();
    InsertBank& inserts_ = *insertBank_;
    std::array<int, kChannels> activeInserts_{};  // count of non-empty slots
    MacroBank& macros_ = *macroBank_;
    Reverb reverb_;
    TempoDelay delay_;
    std::atomic<bool> blewUp_{false};  // a NaN was caught (a strip or the master) since the last look
    PerformanceFx perf_;
    std::array<float, kChunk> chL_{}, chR_{}, mL_{}, mR_{};
    // The pads' strips, summed before the drums' strip processes them.
    std::array<float, kChunk> padsL_{}, padsR_{};
    std::array<std::array<float, kChunk>, kBuses> busL_{}, busR_{};
    std::array<std::atomic<float>, kChannels> peaks_{};
    std::atomic<float> masterPeak_{0.0f};
    std::atomic<float> limiterGainOut_{1.0f};
    std::array<int, 32> eventOffset_{};
    std::array<bool, 32> eventKick_{}, eventBeat_{};
    int events_ = 0;
    float envelope_ = 0.0f;
    float release_ = 0.999f;
    Smoother masterGain_{1.0f};
    bool limiterOn_ = true;
    bool masterFxOn_ = true;
};

}  // namespace ps
