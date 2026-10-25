#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <memory>

#include "Effects.h"
#include "MasterChain.h"
#include "PlateReverb.h"
#include "Smoother.h"

namespace ps {

// Channel strips (inserts, volume, pan, mute, solo, two sends, meter), two send buses
// (0 = reverb, 1 = tempo delay), master inserts and the master chain (loudness target, clipper,
// true-peak limiter, loudness meter: MasterChain.h). Mono or stereo sources in, interleaved
// stereo out. prepare() may allocate (stream closed); everything else is real-time safe.
class Mixer {
public:
    // 0 = drums, 1 = acid bass, 2 = synth (instrument track 1), 3 = remix stems (stereo),
    // 5..11 = instrument tracks 2..8, 12..15 = audio tracks 1..4 (stereo). Index 4 is the master
    // (insert commands, saved automation): its strip is never fed. 16..31 = the 16 drum pads' own
    // strips (their effects on one pad): they feed the drums' strip, not the master, and follow its
    // mute and solo. 32..35 = audio tracks 5..8 (stereo), after the pads (their numbers are saved).
    // G1b: 36..59 = the instrument tracks' layers' own strips (InstrumentTrack::Layer), three per
    // track (track t's layer k: kFirstLayer + t * kLayersPerTrack + k). Like a pad's, a layer's strip
    // feeds its track's strip (before its inserts, or after them: setLayerThrough), not the master,
    // and follows its mute, solo and fader; its sends follow the track's fader. setLayerListen
    // hears some of a track's sounds only (« écouter cette couche seule »). Never in the app's
    // saved channel numbers (kProjectChannels: the meters, the project's strips).
    static constexpr int kProjectChannels = 36;
    static constexpr int kFirstLayer = 36;
    static constexpr int kLayersPerTrack = 3;
    static constexpr int kLayerTracks = 8;
    static constexpr int kChannels = kFirstLayer + kLayerTracks * kLayersPerTrack;
    static constexpr int kFirstPad = 16;
    static constexpr int kPads = 16;
    static constexpr int kFirstAudio2 = kFirstPad + kPads;
    static constexpr bool isPad(int ch) { return ch >= kFirstPad && ch < kFirstPad + kPads; }
    static constexpr bool isLayer(int ch) { return ch >= kFirstLayer && ch < kChannels; }
    static constexpr int layerChannel(int track, int layer) { return kFirstLayer + track * kLayersPerTrack + layer; }
    // The track (0..kLayerTracks-1) of a layer's strip, and that track's strip (Engine.h trackChannel).
    static constexpr int layerTrack(int ch) { return (ch - kFirstLayer) / kLayersPerTrack; }
    static constexpr int trackStrip(int track) { return track == 0 ? 2 : 4 + track; }
    static constexpr int layerParent(int ch) { return trackStrip(layerTrack(ch)); }
    // A layer's gain (linear 0..4, +12 dB), on top of its fader (left at 0.8, unity).
    void setLayerGain(int ch, float gain);
    // A layer's sound goes through its track's inserts (true, the default) or joins the track's
    // strip right after them (a clean sub under a driven bass: none of the track's effects). « After »
    // means after the inserts only: the track's quick controls, chopper and kills, fader, pan and
    // sends still act on the sum (the whole track, its sub included).
    void setLayerThrough(int ch, bool through);
    // Which of track `track`'s sounds are heard: bit 0 its own sound, bit 1 + k its layer k (all set:
    // as mixed). Each glides in or out over a few milliseconds (no click).
    void setLayerListen(int track, int mask);
    int layerListen(int track) const { return track >= 0 && track < kLayerTracks ? listen_[static_cast<size_t>(track)] : 0xF; }
    static constexpr int kDrums = 0;
    // Effects per channel, and on the master. Every slot is built and prepared with the mixer,
    // never in the callback: 24 to 38 kB each, 60 x 8 slots (G1b: the layers' 24 strips included)
    // = 12 to 18 MB per engine on the heap. The long
    // effects' seconds of memory are the pool's (LongFxPool, 18.4 MB): about 32 MB per mixer.
    static constexpr int kInserts = 8;
    static constexpr int kMaster = 4;    // insert "channel" index of the master
    static constexpr int kBuses = 2;
    static constexpr float kCeiling = 0.891251f;  // -1 dBTP, the master chain's default ceiling
    // The master chain's delay: the output is this many frames late (the same live and in an
    // export, which drops them). MasterChain::latencyFor(rate) before prepare().
    int latency() const { return chain_->latency(); }

    void prepare(int sampleRate);
    void setVolume(int ch, float v);   // 0..1 (0.8 = unity, up to +2 dB); kMaster: the master fader (after the
                                       // master chain, at most unity: the song's fades)
    void setPan(int ch, float pan);    // -1..1
    void setMute(int ch, bool on);
    void setSolo(int ch, bool on);     // a strip, or a pad's (kFirstPad..): heard alone through the drums' strip
    // ch 0..kMaster. While the strip sounds, the slot's effect fades out over kGateMs first, the new
    // one is put in from silence and fades in (no click); parameters sent meanwhile wait for it.
    void setInsertType(int ch, int slot, int type);
    // Every slot of `ch` emptied (a strip the app no longer uses: its pool units go back, after the
    // same fade when it sounds).
    void clearInserts(int ch);
    void setInsertParam(int ch, int slot, int param, float v);
    // The type a slot is fading out to put in (-1: none waiting). Tests.
    int pendingType(int ch, int slot) const { return swaps_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].type; }
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
    // A send bus's settings: its effect's (0..3, the reverb's type 4: Reverb), then (S4) its return's
    // equaliser: kBusLowCut (0 = off, else a high-pass at 40 x 25^v Hz: 0.5 = 200 Hz) and kBusHighCut
    // (1 = off, else a low-pass at 1 x 20^v kHz: 0.7 = 8 kHz). At rest the return is the plain one.
    static constexpr int kBusLowCut = 8;
    static constexpr int kBusHighCut = 9;
    void setBusParam(int bus, int param, float v);
    // S4: the whole mix's lows in mono under `hz` (its side high-passed twice, 24 dB / octave), before
    // the master chain; 0 = off (the mix untouched, as before).
    void setMonoBass(float hz);
    // S5: the sidechain's key, the sound the audio-keyed pumps and the Musique bus duck under: a
    // strip's input (0..kChannels-1, a pad's own strip kFirstPad + pad), or -1: none (no key: they
    // let the sound through). A pad without a strip of its own comes through setKeyInput.
    void setSidechainKey(int ch) { keyCh_ = ch >= 0 && ch < kChannels && ch != kMaster ? ch : -1; }
    int sidechainKey() const { return keyCh_; }
    // The key pad's sound when it has no strip of its own (the engine renders it apart for the
    // next process() call; null: none). Not kept past that call.
    void setKeyInput(const float* l, const float* r) {
        keyInL_ = l;
        keyInR_ = r;
    }
    // S5: the « Musique » group bus: the strips in it (and the send returns asked) are summed apart
    // and ducked once under the key (KeyedDucker: `depth` broadband, `lowDepth` under 150 Hz,
    // `release` 0..1 = 50..600 ms), then join the mix. Nothing in it: the mix as before.
    void setGroupMember(int ch, bool on);
    void setGroupReturn(int bus, bool on);
    void setGroupDuck(int param, float v);  // 0 depth, 1 low depth, 2 release (all 0..1)
    bool inGroup(int ch) const { return ch >= 0 && ch < kChannels && group_[static_cast<size_t>(ch)]; }
    const KeyedDucker& groupDucker() const { return groupDuck_; }  // tests
    // The master: a gain before the limiter (linear, 0..4), the limiter on or off (a pre-master
    // export keeps its headroom), and its inserts on or off (stems: each strip without the master's
    // processing, so the stems add back up to the mix).
    float masterFader() const { return masterFader_.target(); }  // tests
    float masterFaderNow() const { return masterFader_.current(); }  // tests, the live bench (the fade as heard)
    float masterGain() const { return masterGain_.target(); }  // tests, the live bench
    void setMasterGain(float gain) { masterGain_.setTarget(std::clamp(std::isfinite(gain) ? gain : 1.0f, 0.0f, 4.0f)); }
    void setLimiter(bool on) {
        limiterOn_ = on;
        chain_->setLimiter(on);
    }
    // The master chain (MasterChain.h): its ceiling (dBTP), its loudness target (LUFS, NaN: none),
    // a fixed drive (an offline render's mastering pass) and the estimate's memory.
    void setCeiling(float db) { chain_->setCeilingDb(db); }
    void setLoudnessTarget(float lufs) { chain_->setTarget(lufs); }
    void resetLoudnessEstimate() { chain_->resetEstimate(); }
    // An offline render's first pass: the output is the music before the master chain (its gain
    // and the click included), not delayed; the renderer masters it in a second pass.
    void setMasterChainBypass(bool on) { chainBypass_ = on; }
    // S7 son (mix version 4): the master's fader (the song curve's section levels and fades) before the
    // master chain, as an export has it (its first pass renders with the chain bypassed, the fader on,
    // and its own chain masters that): live = export, an Intro or a Groove brought lower is limited as
    // the export limits it. Off (versions 1 to 3): after the chain, as always. Exports are the same either way.
    void setFaderBeforeChain(bool on) { faderBeforeChain_ = on; }
    bool faderBeforeChain() const { return faderBeforeChain_; }
    int outputLatency() const { return chainBypass_ ? 0 : chain_->latency(); }
    MasterChain& masterChain() { return *chain_; }
    const MasterChain& masterChain() const { return *chain_; }
    // Momentary, short-term and integrated (since ▶) loudness of the output, its true peak.
    LoudnessMeter& loudness() { return chain_->meter(); }
    // Every fader, pan and send gain at its target at once (an offline render, before its first
    // beat: a muted or not-soloed strip must not bleed in while its gain glides down).
    void settle() {
        for (auto& s : strips_) {
            s.gainL.snap(s.gainL.target());
            s.gainR.snap(s.gainR.target());
            for (auto& g : s.sendGain) g.snap(g.target());
        }
        masterGain_.snap(masterGain_.target());
        for (auto& own : trackOwn_) own.snap(own.target());  // a layer heard alone from the first frame
    }
    void setMasterFx(bool on) {
        masterFxOn_ = on;
        chain_->setDriveAllowed(on);  // separate tracks add back up to the mix: no drive
    }
    void setLiveNoises(int mode) { perf_.setNoises(mode); }  // PerformanceFx::Noises
    // The transport starts: the effects' free LFOs from their start (InsertSlot::restartLfo).
    void restartLfos() {
        for (auto& strip : inserts_)
            for (auto& slot : strip) slot.restartLfo();
    }
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
    // The master's peak going into the limiter (after the master's gain, the metronome included),
    // since last read: above kCeiling, the limiter works.
    float takeMasterInputPeak() { return masterInPeak_.exchange(0.0f, std::memory_order_acq_rel); }
    // Current limiter gain (1 = not limiting): its lowest over the last block processed.
    float limiterGain() const { return limiterGainOut_.load(std::memory_order_relaxed); }
    // The limiter's lowest gain since last read (the meters).
    float takeLimiterGain() { return limiterMin_.exchange(1.0f, std::memory_order_acq_rel); }
    const InsertSlot& insert(int ch, int slot) const { return inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)]; }
    // The deepest gain reduction (dB, <= 0) of the compressor in insert `slot` of strip `ch` since
    // the last read (0: none, or not a compressor): its meter in the effect's box.
    float takeInsertReduction(int ch, int slot) {
        if (ch < 0 || ch >= kChannels || slot < 0 || slot >= kInserts) return 0.0f;
        return reductions_[static_cast<size_t>(ch * kInserts + slot)].exchange(0.0f, std::memory_order_acq_rel);
    }
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
        float linear = 1.0f;  // a layer's gain (setLayerGain)
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
    static void storeMin(std::atomic<float>& a, float v);
    static float blockPeak(const float* l, const float* r, int n);
    // --- Effect swaps without a click (setInsertType while the strip sounds).
    struct Swap {
        int type = -1;      // the type waiting for the old one to fade out (-1: none)
        float fade = 1.0f;  // the old effect's share (1 heard .. 0 gone)
        bool gateOn = true;  // « Joue dans » for the new effect (SetInsertOn sent meanwhile)
        unsigned mask = 0;   // parameters sent meanwhile (bits), for the new effect
        std::array<float, InsertSlot::kParams> params{};
    };
    std::array<std::array<Swap, kInserts>, kChannels> swaps_{};
    // Each strip's level going into and out of its inserts at the last block (the master's at
    // kMaster): under kSilentLevel, a swap needs no fade.
    std::array<float, kChannels> stripLevel_{};
    static constexpr float kSilentLevel = 1e-4f;  // -80 dBFS
    bool slotHeard(int ch, int slot) const;
    void requestType(int ch, int slot, int type);
    void swapNow(int ch, int slot, int type, bool fadeIn);
    void finishSwaps(int ch);  // the strip is not heard: every effect waiting goes in now
    // --- The master chain (drive, clipper, limiter, meter): on the heap, about 100 kB.
    std::unique_ptr<MasterChain> chain_ = std::make_unique<MasterChain>();
    std::array<float, kChunk> outL_{}, outR_{}, click_{};
    bool chainBypass_ = false;
    bool faderBeforeChain_ = false;

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
    std::unique_ptr<PlateReverb> plate_ = std::make_unique<PlateReverb>();
    TempoDelay delay_;
    // The returns' equalisers (S4), per bus: off at rest.
    struct ReturnEq {
        float lowCut = 0.0f, highCut = 1.0f;
        Biquad hpL, hpR, lpL, lpR;
        bool active() const { return lowCut > 0.0f || highCut < 1.0f; }
    };
    std::array<ReturnEq, kBuses> returnEq_{};
    void updateReturnEq(int bus);
    float rate_ = 48000.0f;
    float monoBassHz_ = 0.0f;
    // --- S5: the sidechain key and the « Musique » bus.
    int keyCh_ = -1;
    const float* keyInL_ = nullptr;
    const float* keyInR_ = nullptr;
    std::array<float, kChunk> keyBuf_{};
    const float* chunkKey_ = nullptr;  // keyBuf_ while a key sounds in this chunk, else null
    std::array<bool, kChannels> group_{};
    std::array<bool, kBuses> groupReturn_{};
    int groupCount_ = 0;
    std::array<float, 3> groupParams_{0.0f, 0.0f, 0.4f};
    KeyedDucker groupDuck_;
    std::array<float, kChunk> grpL_{}, grpR_{};
    void updateGroupDuck();
    std::array<Biquad, 2> monoBass_{};
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
    std::atomic<float> masterInPeak_{0.0f};
    std::atomic<float> limiterGainOut_{1.0f};
    std::atomic<float> limiterMin_{1.0f};
    std::array<std::atomic<float>, kChannels * kInserts> reductions_{};  // takeInsertReduction
    void meterReduction(int ch, size_t slot, const InsertSlot& s) {
        if (s.type() == InsertSlot::Comp) storeMin(reductions_[static_cast<size_t>(ch) * kInserts + slot], s.compressor().gainReductionDb());
    }
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
    Smoother masterGain_{1.0f};
    Smoother masterFader_{1.0f};  // setVolume(kMaster): after the master chain, at most unity
    Smoother drumsOwn_{1.0f};     // the drums' own sound, 0 while a pad is soloed
    // --- G1b: the layers' strips, summed into their track's strip (before its inserts: pre_; after: post_).
    using LayerSums = std::array<std::array<float, kChunk>, kLayerTracks>;
    std::unique_ptr<LayerSums> preL_ = std::make_unique<LayerSums>(), preR_ = std::make_unique<LayerSums>();
    std::unique_ptr<LayerSums> postL_ = std::make_unique<LayerSums>(), postR_ = std::make_unique<LayerSums>();
    std::array<bool, kChannels> through_ = filledTrue();
    std::array<int, kLayerTracks> listen_ = filledListen();
    std::array<Smoother, kLayerTracks> trackOwn_ = filledOwn();  // a track's own sound, 0 while only some of its layers are heard
    std::array<bool, kLayerTracks> fedPre_{}, fedPost_{};
    static std::array<Smoother, kLayerTracks> filledOwn() {
        std::array<Smoother, kLayerTracks> a{};
        a.fill(Smoother(1.0f));
        return a;
    }
    // The track whose strip `ch` is (trackStrip's inverse), -1: none.
    static constexpr int stripTrack(int ch) { return ch == 2 ? 0 : (ch >= 5 && ch < 4 + kLayerTracks ? ch - 4 : -1); }
    static std::array<bool, kChannels> filledTrue() {
        std::array<bool, kChannels> a{};
        a.fill(true);
        return a;
    }
    static std::array<int, kLayerTracks> filledListen() {
        std::array<int, kLayerTracks> a{};
        a.fill(0xF);
        return a;
    }

    bool limiterOn_ = true;
    bool masterFxOn_ = true;
};

}  // namespace ps
