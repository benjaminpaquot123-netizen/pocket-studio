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
    // Effects per channel, and on the master. Every slot is built and prepared with the mixer,
    // never in the callback: about 38 kB each, 32 x 8 = 9.7 MB per engine on the heap. The long
    // effects' seconds of memory are the pool's (LongFxPool, 11.7 MB): about 25 MB per mixer.
    static constexpr int kInserts = 8;
    static constexpr int kMaster = 4;    // insert "channel" index of the master
    static constexpr int kBuses = 2;
    static constexpr float kCeiling = 0.89f;  // -1 dBFS

    void prepare(int sampleRate);
    void setVolume(int ch, float v);   // 0..1 (0.8 = unity, up to +2 dB)
    void setPan(int ch, float pan);    // -1..1
    void setMute(int ch, bool on);
    void setSolo(int ch, bool on);
    void setInsertType(int ch, int slot, int type);           // ch 0..kMaster
    // Every slot of `ch` emptied at once (a strip the app no longer uses: its pool units go back).
    void clearInserts(int ch);
    void setInsertParam(int ch, int slot, int param, float v);
    // « Joue dans »: a slot outside the scenes it plays in lets the sound through untouched (and
    // costs nothing). The switch glides over kGateMs from its frame; switched back on, the effect
    // starts again from silence (no stale tail from the last scene it played in).
    static constexpr float kGateMs = 5.0f;
    void setInsertOn(int ch, int slot, bool on);  // from the next frame processed
    // The same `offset` frames into the next process() call (a scene swap, sample-accurate).
    void scheduleInsertOn(int ch, int slot, bool on, int offset);
    bool insertOn(int ch, int slot) const { return gates_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].on; }  // tests
    void setSend(int ch, int bus, float level);               // 0..1, post-fader
    // TrackMacros: colour, dirt, motion, space; kChopMacro: the strip's chopper on (v > 0.5) or off;
    // kKillMacro + band (0 low, 1 mid, 2 high): that band of the strip cut while on (on the grid,
    // both ways); kThrowMacro + bus (0 reverb, 1 echo): on a rise, the strip's send to that bus wide
    // open for one beat from the next beat line (a dub « throw »).
    static constexpr int kChopMacro = 4;
    static constexpr int kKillMacro = 5;
    static constexpr int kThrowMacro = 8;
    static constexpr int kMacroParams = 10;
    void setMacro(int ch, int macro, float v);
    bool chopOn(int ch) const { return chopOn_[static_cast<size_t>(ch)]; }  // tests
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
    // The longest repeat time among the send delay and the tracks' own echoes.
    float echoFrames() const {
        float frames = delay_.delayFrames();
        for (const auto& strip : inserts_) {
            for (const auto& slot : strip) frames = std::max(frames, slot.echoFrames());
        }
        return frames;
    }
    bool echoInUse() const {
        for (const auto& s : strips_) {
            if (s.sendGain[1].target() > 0.0f) return true;
        }
        for (const auto& strip : inserts_) {
            for (const auto& slot : strip) {
                if (slot.echoFrames() > 0.0f) return true;
            }
        }
        return false;
    }
    const LongFxPool& longFxPool() const { return *pool_; }  // tests
    void setStepFrames(double framesPerSixteenth) {
        if (framesPerSixteenth > 0.0) stepFrames_ = framesPerSixteenth;
        delay_.setStepFrames(framesPerSixteenth);
        perf_.setStepFrames(framesPerSixteenth);
        for (auto& c : chops_) c.setStepFrames(framesPerSixteenth);
        // The rhythmic effects know the beat's length at once (not only after two beats).
        for (auto& strip : inserts_) {
            for (auto& slot : strip) slot.setStepFrames(framesPerSixteenth);
        }
    }
    // The Live page's effects (PerformanceFx::Param): the master's PerformanceFx, plus what acts on
    // the send buses here (reverb freeze, whole-mix throws, « Drop » and « Tout remettre »).
    void setPerformance(int param, float v);
    // A transport runs (steps arrive with sidechainEvent): live switches wait for the grid's lines.
    void setClockRunning(bool on);
    // A setting a song curve moves on a step (the Live page's effects: PerformanceFx, the bus
    // returns, a strip's macros and chopper), `offset` frames into the next process() call: on
    // that frame, whatever the size of the blocks (a recorded performance plays back as it was).
    enum SettingKind { PerformanceSetting = 0, ReturnSetting = 1, MacroSetting = 2 };
    void scheduleSetting(int kind, int a, int b, float v, int offset);
    // The live grid's return of send bus `bus` (0 reverb, 1 echo): its effect's output times
    // `gain` (0..2, 1 = as mixed). Smoothed; at 1 the mix is the plain one, sample for sample.
    void setBusReturn(int bus, float gain) {
        if (bus >= 0 && bus < kBuses) busReturn_[static_cast<size_t>(bus)].setTarget(std::clamp(std::isfinite(gain) ? gain : 1.0f, 0.0f, 2.0f));
    }
    float busReturn(int bus) const { return busReturn_[static_cast<size_t>(bus)].target(); }  // tests
    // A sequencer step lands `offset` frames into the next process() call (sidechain pumps, the live
    // effects' grid): transport step `step` (-1: unknown) of a bar of `stepsPerBar` steps.
    void sidechainEvent(int offset, bool kick, bool beat, int step = -1, int stepsPerBar = 16) {
        if (events_ < static_cast<int>(eventOffset_.size())) {
            eventOffset_[static_cast<size_t>(events_)] = offset;
            eventKick_[static_cast<size_t>(events_)] = kick;
            eventBeat_[static_cast<size_t>(events_)] = beat;
            eventStep_[static_cast<size_t>(events_)] = step;
            eventBar_[static_cast<size_t>(events_)] = stepsPerBar;
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
    // Tests.
    const PerformanceFx& performance() const { return perf_; }
    const Reverb& reverb() const { return reverb_; }
    const Isolator& isolator(int ch) const { return (*isolators_)[static_cast<size_t>(ch)]; }
    bool throwing(int ch, int bus) const { return throws_[static_cast<size_t>(ch)][static_cast<size_t>(bus)].on; }
    float masterSend(int bus) const { return masterSend_[static_cast<size_t>(bus)].target(); }

    /** True once after a NaN was caught and silenced (for the reports). */
    bool takeBlewUp() { return blewUp_.exchange(false, std::memory_order_relaxed); }

private:
    static constexpr int kChunk = 256;
    void afterInsertChange(int ch);
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
    // The inserts of `ch` over l/r, each slot heard as far as its « Joue dans » gate lets it.
    void processInserts(int ch, float* l, float* r, int n);
    void applyGate(int ch, int slot, bool on);
    struct Gate {
        bool on = true;
        float mix = 1.0f;  // 0: passed by, 1: heard
    };
    std::array<std::array<Gate, kInserts>, kChannels> gates_{};
    struct GateEvent {
        int ch, slot, offset;
        bool on;
    };
    std::array<GateEvent, kChannels * kInserts> gateEvents_{};
    int gateCount_ = 0;
    struct SettingEvent {
        int kind, a, b;
        float v;
        int offset;
    };
    void applySetting(const SettingEvent& e);
    std::array<SettingEvent, 64> settingEvents_{};
    int settingCount_ = 0;
    float gateStep_ = 1.0f / 240.0f;  // per frame (kGateMs at 48 kHz until prepared)
    std::array<float, kChunk> dryL_{}, dryR_{};
    static void storeMax(std::atomic<float>& a, float v);

    std::array<Strip, kChannels> strips_{};
    // Large (each chorus holds its delay lines, about 20 kB): on the heap, allocated once with the
    // mixer, so an engine never weighs megabytes on a thread's stack.
    using InsertBank = std::array<std::array<InsertSlot, kInserts>, kChannels>;
    using MacroBank = std::array<TrackMacros, kChannels>;
    // Each strip's chopper: on, or still gliding back to the plain sound.
    std::array<Chopper, kChannels> chops_{};
    std::array<bool, kChannels> chopOn_{};
    // Frames since the transport's last beat, at the end of the last chunk processed.
    int64_t framesSinceBeat_ = 0;
    bool chopActive(int ch) const {
        return chopOn_[static_cast<size_t>(ch)] || chops_[static_cast<size_t>(ch)].gain() < 0.9999f;
    }
    std::unique_ptr<InsertBank> insertBank_ = std::make_unique<InsertBank>();
    std::unique_ptr<MacroBank> macroBank_ = std::make_unique<MacroBank>();
    InsertBank& inserts_ = *insertBank_;
    // The few echoes, reverbs and tape stops the slots borrow (seconds of memory each).
    std::unique_ptr<LongFxPool> pool_ = std::make_unique<LongFxPool>();
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
    std::array<Smoother, kBuses> busReturn_{Smoother(1.0f), Smoother(1.0f)};
    std::array<float, kChunk> retL_{}, retR_{};
    std::array<std::atomic<float>, kChannels> peaks_{};
    std::atomic<float> masterPeak_{0.0f};
    std::atomic<float> limiterGainOut_{1.0f};
    std::array<int, 32> eventOffset_{}, eventStep_{}, eventBar_{};
    std::array<bool, 32> eventKick_{}, eventBeat_{};
    int events_ = 0;
    // --- The Live page's switches on the send buses and the strips (sample-exact: a chunk starts
    // on every step, and a switch waiting for the grid takes effect at the start of one).
    void liveLine(int step, int stepsPerBar);  // a step at the chunk's start
    bool lineWaits() const { return clock_ && perf_.gridSteps() > 0; }
    void throwEdge(int ch, int bus, bool on);
    void startThrow(int ch, int bus);
    void liveReset();
    void updateMasterSends();
    bool clock_ = false;
    double stepFrames_ = 6000.0;
    int lastStep_ = -1;
    bool freezeWant_ = false, dropWant_ = false, resetWant_ = false, dropPending_ = false;
    struct Throw {
        bool want = false, pending = false, on = false;
        int endStep = -1;
        double framesLeft = 0.0;
    };
    // Per strip and bus (the master's index, kMaster, is the whole mix's).
    std::array<std::array<Throw, kBuses>, kChannels> throws_{};
    bool anyThrow_ = false;
    std::array<std::array<bool, Isolator::kBands>, kChannels> killWant_{};
    std::unique_ptr<std::array<Isolator, kChannels>> isolators_ = std::make_unique<std::array<Isolator, kChannels>>();
    // The whole mix into the send buses (the build-up's sends, the whole-mix throws).
    std::array<Smoother, kBuses> masterSend_{Smoother(0.0f), Smoother(0.0f)};
    float envelope_ = 0.0f;
    float release_ = 0.999f;
    Smoother masterGain_{1.0f};
    bool limiterOn_ = true;
    bool masterFxOn_ = true;
};

}  // namespace ps
