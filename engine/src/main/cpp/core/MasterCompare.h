#pragma once

// Lot E (« Master pro »): « écoute A/B » of the master at the same loudness, and a reference song
// heard in its place at the same loudness too.
//   - Master: what the master chain gives (as always).
//   - Without: the mix before the master's processing (its inserts, the Live page's effects, the
//     mono bass, the master's gain, the drive, the clipper and the limiter), delayed as the chain delays.
//   - Reference: a song the user imported.
// While armed (« A/B au même volume » on), every side is brought to the loudness of the quietest one
// (never up: a mix without its limiter raised to the master's loudness would clip), whichever is heard:
// one hears what the master does, not that it is louder. Disarmed, the master is heard as always.
// The loudness of each side is a gated mean of 400 ms blocks over the last ~20 s (GatedLoudness with
// a forgetting factor), measured while armed and only then: at rest the mixer pays one branch.
// Switching glides over 20 ms (no click); a matching gain moves slowly (no pumping); what is heard
// while comparing is held within full scale. Real-time safe: fixed-size state; a reference's sample is
// handed back to a non-audio thread (collectGarbage) to free.

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>

#include "Loudness.h"
#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

class MasterCompare {
public:
    enum Listen { Master = 0, Without = 1, Reference = 2 };
    static constexpr int kMaxDelay = 256;
    static constexpr float kMaxMatchDb = 24.0f;
    // Each 400 ms block counts this much less per new 100 ms piece: about 20 s of memory.
    static constexpr double kForget = 0.995;

    ~MasterCompare() {
        collectGarbage();
        if (ref_ && ref_->release()) delete ref_;
    }

    void prepare(int rate, int delayFrames) {
        rate_ = rate;
        delay_ = std::clamp(delayFrames, 0, kMaxDelay - 1);
        dry_.prepare(rate);
        wet_.prepare(rate);
        dryGated_.setForget(kForget);
        wetGated_.setForget(kForget);
        fadeStep_ = 1.0f / std::max(1.0f, 0.02f * rate);
        matchCoef_ = 1.0f - std::exp(-1.0f / (0.3f * rate));
        resetMeasure();
    }
    // Measuring on (the panel is open) or off (nothing measured, the gains kept).
    void setArmed(bool on) {
        if (on && !armed_) {
            resetMeasure();
            masterTarget_ = withoutTarget_ = refTarget_ = 1.0f;
        }
        armed_ = on;
    }
    bool armed() const { return armed_; }
    void setListen(int listen) { listen_ = std::clamp(listen, 0, 2); }
    int listen() const { return listen_; }
    // The reference song (one reference taken over; nullptr: none). The previous one is retired.
    void install(Sample* s) {
        if (ref_ && !retired_.push(ref_)) {
            // Never happens (64 deep, emptied by collectGarbage); kept rather than freed here.
        }
        ref_ = s && s->data() ? s : nullptr;
        if (s && !ref_ && !retired_.push(s)) {
        }
        refPos_ = 0;
    }
    // The reference's integrated loudness (LUFS), measured by the app off the audio thread.
    void setReferenceLufs(float lufs) { refLufs_ = std::isfinite(lufs) && lufs > -70.0f ? lufs : NAN; }
    // Not the audio thread.
    void collectGarbage() {
        Sample* s = nullptr;
        while (retired_.pop(s)) {
            if (s->release()) delete s;
        }
    }
    // The gains last applied (dB, <= 0): the master, the mix without it, the reference.
    float masterGainDb() const { return masterPublished_.load(std::memory_order_relaxed); }
    float withoutGainDb() const { return withoutPublished_.load(std::memory_order_relaxed); }
    float referenceGainDb() const { return refPublished_.load(std::memory_order_relaxed); }
    // Whether process() has anything to do (armed, or a listen or a gain not yet back on the master).
    bool active() const { return armed_ || listen_ != Master || mixW_ < 1.0f || masterGain_ != 1.0f; }

    // `dryL/R`: the mix before the master's processing; `wetL/R`: the master chain's output, replaced in
    // place by what is heard. n <= the mixer's chunk.
    void process(const float* dryL, const float* dryR, float* wetL, float* wetR, int n) {
        for (int i = 0; i < n; ++i) {
            // The dry side, as late as the chain's output.
            delayL_[static_cast<size_t>(writePos_)] = dryL[i];
            delayR_[static_cast<size_t>(writePos_)] = dryR[i];
            const int readPos = (writePos_ - delay_ + kMaxDelay) % kMaxDelay;
            writePos_ = (writePos_ + 1) % kMaxDelay;
            const float dl = delayL_[static_cast<size_t>(readPos)], dr = delayR_[static_cast<size_t>(readPos)];
            const float wl = wetL[i], wr = wetR[i];
            if (armed_) measure(dl, dr, wl, wr);
            // The three sides glide to the one asked.
            const float wantW = listen_ == Master ? 1.0f : 0.0f, wantD = listen_ == Without ? 1.0f : 0.0f;
            const float wantR = listen_ == Reference && ref_ ? 1.0f : 0.0f;
            mixW_ = glide(mixW_, wantW);
            mixD_ = glide(mixD_, wantD);
            mixR_ = glide(mixR_, wantR);
            approach(masterGain_, armed_ ? masterTarget_ : 1.0f);
            approach(withoutGain_, withoutTarget_);
            approach(refGain_, refTarget_);
            float ol = wl * mixW_ * masterGain_, orr = wr * mixW_ * masterGain_;
            if (mixD_ > 0.0f) {
                ol += dl * mixD_ * withoutGain_;
                orr += dr * mixD_ * withoutGain_;
            }
            if (mixR_ > 0.0f && ref_) {
                const float* d = ref_->data();
                const int ch = ref_->channels();
                const float l = d[refPos_ * ch], r = ch > 1 ? d[refPos_ * ch + 1] : l;
                ol += l * mixR_ * refGain_;
                orr += r * mixR_ * refGain_;
            }
            // The reference keeps its place while it is heard (it starts over at its end).
            if (ref_ && mixR_ > 0.0f && ++refPos_ >= ref_->frames()) refPos_ = 0;
            // Comparing: never past full scale (the mix without its limiter, a loud reference).
            if (mixD_ > 0.0f || mixR_ > 0.0f) {
                ol = std::clamp(ol, -1.0f, 1.0f);
                orr = std::clamp(orr, -1.0f, 1.0f);
            }
            wetL[i] = ol;
            wetR[i] = orr;
        }
        masterPublished_.store(20.0f * std::log10(std::max(masterGain_, 1e-6f)), std::memory_order_relaxed);
        withoutPublished_.store(20.0f * std::log10(std::max(withoutGain_, 1e-6f)), std::memory_order_relaxed);
        refPublished_.store(20.0f * std::log10(std::max(refGain_, 1e-6f)), std::memory_order_relaxed);
    }

private:
    float glide(float v, float want) const {
        if (v < want) return std::min(want, v + fadeStep_);
        if (v > want) return std::max(want, v - fadeStep_);
        return v;
    }
    // A slow glide; close enough (0.02 dB: a float's steps would stall it short), there.
    void approach(float& g, float want) const {
        g += matchCoef_ * (want - g);
        if (std::fabs(want - g) < 2e-3f) g = want;
    }
    void resetMeasure() {
        dry_.reset();
        wet_.reset();
        dryGated_.reset();
        wetGated_.reset();
    }
    void measure(float dl, float dr, float wl, float wr) {
        const bool d = dry_.add(dl, dr);
        const bool w = wet_.add(wl, wr);
        if (d && dry_.pieces() >= 4) dryGated_.add(dry_.meanOfLast(4));
        if (!w || wet_.pieces() < 4) return;
        wetGated_.add(wet_.meanOfLast(4));
        // Once a second: the gates' sums are 800 bins each.
        if (wet_.pieces() % 10 != 0) return;
        const double wetLufs = wetGated_.lufs(), dryLufs = dryGated_.lufs();
        if (!(wetLufs > -70.0 && dryLufs > -70.0)) return;
        // Every side at the quietest one's loudness (the reference's counted once it is there).
        double quiet = std::min(wetLufs, dryLufs);
        const bool ref = ref_ != nullptr && std::isfinite(refLufs_);
        if (ref) quiet = std::min(quiet, double(refLufs_));
        auto down = [quiet](double lufs) { return dbToGain(static_cast<float>(std::clamp(quiet - lufs, double(-kMaxMatchDb), 0.0))); };
        masterTarget_ = down(wetLufs);
        withoutTarget_ = down(dryLufs);
        if (ref) refTarget_ = down(refLufs_);
    }
    static float dbToGain(float db) { return std::pow(10.0f, db / 20.0f); }

    int rate_ = 48000;
    int delay_ = 0;
    bool armed_ = false;
    int listen_ = Master;
    std::array<float, kMaxDelay> delayL_{}, delayR_{};
    int writePos_ = 0;
    BlockLoudness dry_, wet_;
    GatedLoudness dryGated_, wetGated_;
    float mixW_ = 1.0f, mixD_ = 0.0f, mixR_ = 0.0f;
    float fadeStep_ = 1.0f / 960.0f;
    float matchCoef_ = 1e-4f;
    float masterTarget_ = 1.0f, masterGain_ = 1.0f;
    float withoutTarget_ = 1.0f, withoutGain_ = 1.0f;
    float refTarget_ = 1.0f, refGain_ = 1.0f;
    float refLufs_ = NAN;
    Sample* ref_ = nullptr;
    int64_t refPos_ = 0;
    SpscQueue<Sample*, 64> retired_;
    std::atomic<float> masterPublished_{0.0f}, withoutPublished_{0.0f}, refPublished_{0.0f};
};

}  // namespace ps
