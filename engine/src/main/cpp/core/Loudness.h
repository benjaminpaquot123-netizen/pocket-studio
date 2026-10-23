#pragma once

// Loudness as streaming services and EBU R128 measure it (ITU-R BS.1770-4): K-weighting, 400 ms
// blocks every 100 ms, absolute gate at -70 LUFS and relative gate 10 LU under the gated mean;
// momentary = the last 400 ms, short-term = the last 3 s. Plus the true peak (TruePeak.h).
// Fixed-size state only: everything here is real-time safe once prepared.

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>

#include "TruePeak.h"

namespace ps {

// The K-weighting pre-filter (shelf +4 dB above ~1.7 kHz, then a 38 Hz high-pass), derived for
// any rate as libebur128 does (at 48 kHz, exactly the coefficients printed in BS.1770-4).
// Double precision: the high-pass sits close to DC.
class KWeighting {
public:
    void prepare(int rate) {
        constexpr double kPi = 3.14159265358979323846;
        const double fs = rate > 0 ? rate : 48000;
        {
            const double q = 0.7071752369554196;
            const double k = std::tan(kPi * 1681.974450955533 / fs);
            const double vh = std::pow(10.0, 3.999843853973347 / 20.0);
            const double vb = std::pow(vh, 0.4996667741545416);
            const double a0 = 1.0 + k / q + k * k;
            s_ = {(vh + vb * k / q + k * k) / a0, 2.0 * (k * k - vh) / a0, (vh - vb * k / q + k * k) / a0, 2.0 * (k * k - 1.0) / a0,
                  (1.0 - k / q + k * k) / a0};
        }
        {
            const double q = 0.5003270373238773;
            const double k = std::tan(kPi * 38.13547087602444 / fs);
            const double a0 = 1.0 + k / q + k * k;
            h_ = {1.0, -2.0, 1.0, 2.0 * (k * k - 1.0) / a0, (1.0 - k / q + k * k) / a0};
        }
        reset();
    }
    void reset() { s1_ = s2_ = h1_ = h2_ = 0.0; }
    double process(float x) {
        // Transposed direct form II, both stages.
        const double in = x;
        const double y = s_[0] * in + s1_;
        s1_ = s_[1] * in - s_[3] * y + s2_;
        s2_ = s_[2] * in - s_[4] * y;
        const double z = h_[0] * y + h1_;
        h1_ = h_[1] * y - h_[3] * z + h2_;
        h2_ = h_[2] * y - h_[4] * z;
        return z;
    }

private:
    std::array<double, 5> s_{1, 0, 0, 0, 0}, h_{1, 0, 0, 0, 0};  // b0 b1 b2 a1 a2
    double s1_ = 0, s2_ = 0, h1_ = 0, h2_ = 0;
};

// The loudness of a mean square (K-weighted, the channels summed).
inline double lufsOf(double meanSquare) { return meanSquare > 0.0 ? -0.691 + 10.0 * std::log10(meanSquare) : -1000.0; }

// Stereo K-weighted energy in 100 ms pieces; the last 30 kept (3 s, the short-term window).
class BlockLoudness {
public:
    static constexpr int kRing = 30;
    void prepare(int rate) {
        piece_ = std::max(1, rate / 10);
        kl_.prepare(rate);
        kr_.prepare(rate);
        reset();
    }
    void reset() {
        kl_.reset();
        kr_.reset();
        ring_.fill(0.0);
        head_ = 0;
        pieces_ = 0;
        sum_ = 0.0;
        inPiece_ = 0;
    }
    // One frame; true when it completed a 100 ms piece.
    bool add(float l, float r) {
        const double yl = kl_.process(l), yr = kr_.process(r);
        sum_ += yl * yl + yr * yr;
        if (++inPiece_ < piece_) return false;
        ring_[static_cast<size_t>(head_)] = sum_ / piece_;
        head_ = (head_ + 1) % kRing;
        ++pieces_;
        sum_ = 0.0;
        inPiece_ = 0;
        return true;
    }
    // Pieces completed since reset.
    long pieces() const { return pieces_; }
    // The mean square of the last `count` pieces (as many as there are, at most kRing).
    double meanOfLast(int count) const {
        const int n = static_cast<int>(std::min<long>(std::min(count, kRing), pieces_));
        if (n <= 0) return 0.0;
        double s = 0.0;
        for (int i = 1; i <= n; ++i) s += ring_[static_cast<size_t>((head_ - i + kRing) % kRing)];
        return s / n;
    }

private:
    KWeighting kl_, kr_;
    std::array<double, kRing> ring_{};
    int head_ = 0;
    long pieces_ = 0;
    double sum_ = 0.0;
    int piece_ = 4800, inPiece_ = 0;
};

// The gated mean of 400 ms blocks, kept as a histogram (0.1 LU bins from -70 to +10 LUFS, the
// energy of each bin summed exactly): constant memory whatever the length. `forget` < 1 makes
// each block count less as later ones come (a running estimate); 1: the plain integrated loudness.
class GatedLoudness {
public:
    static constexpr double kGateAbs = -70.0;
    static constexpr double kTop = 10.0;
    static constexpr double kStep = 0.1;
    static constexpr int kBins = 800;
    void reset() {
        energy_.fill(0.0);
        weight_.fill(0.0);
        total_ = 0.0;
    }
    void setForget(double forget) { forget_ = std::clamp(forget, 0.5, 1.0); }
    // A 400 ms block's mean square. Under the absolute gate: not counted.
    void add(double meanSquare) {
        const double l = lufsOf(meanSquare);
        if (!(l > kGateAbs)) return;
        if (forget_ < 1.0) {
            for (int i = 0; i < kBins; ++i) {
                energy_[static_cast<size_t>(i)] *= forget_;
                weight_[static_cast<size_t>(i)] *= forget_;
            }
            total_ *= forget_;
        }
        const int bin = std::clamp(static_cast<int>((l - kGateAbs) / kStep), 0, kBins - 1);
        energy_[static_cast<size_t>(bin)] += meanSquare;
        weight_[static_cast<size_t>(bin)] += 1.0;
        total_ += 1.0;
    }
    // Blocks counted (forgotten ones less).
    double blocks() const { return total_; }
    // The gated loudness in LUFS; -1000 with nothing over the gates.
    double lufs() const {
        double e = 0.0, w = 0.0;
        for (int i = 0; i < kBins; ++i) {
            e += energy_[static_cast<size_t>(i)];
            w += weight_[static_cast<size_t>(i)];
        }
        if (w <= 0.0) return -1000.0;
        const double gate = lufsOf(e / w) - 10.0;
        // A bin counts when its middle is over the relative gate (at most 0.05 LU off for its blocks).
        const int first = std::clamp(static_cast<int>(std::floor((gate - kGateAbs) / kStep - 0.5)) + 1, 0, kBins);
        double ge = 0.0, gw = 0.0;
        for (int i = first; i < kBins; ++i) {
            ge += energy_[static_cast<size_t>(i)];
            gw += weight_[static_cast<size_t>(i)];
        }
        return gw > 0.0 ? lufsOf(ge / gw) : -1000.0;
    }

private:
    std::array<double, kBins> energy_{}, weight_{};
    double total_ = 0.0;
    double forget_ = 1.0;
};

// The master's meter: momentary, short-term and integrated (since reset: since ▶) loudness, and
// the true peak (4x). The audio thread calls add() per frame and publish() per block; any thread
// reads the published values.
class LoudnessMeter {
public:
    static constexpr float kNone = -100.0f;  // no value yet (silence, or not 400 ms of sound)
    void prepare(int rate) {
        blocks_.prepare(rate);
        reset();
    }
    void reset() {
        blocks_.reset();
        gated_.reset();
        tpL_.reset();
        tpR_.reset();
        peak_ = 0.0f;
        recent_ = 0.0f;
        resetPending_.store(false, std::memory_order_relaxed);
        momentary_.store(kNone, std::memory_order_relaxed);
        shortTerm_.store(kNone, std::memory_order_relaxed);
        integrated_.store(kNone, std::memory_order_relaxed);
        truePeak_.store(0.0f, std::memory_order_relaxed);
    }
    // From any thread: the audio thread resets at its next publish() (« integrated since ▶ »).
    void requestReset() { resetPending_.store(true, std::memory_order_relaxed); }
    void add(float l, float r) {
        // The interpolator only runs near a peak that could top the one already read.
        const float floor = 0.4f * peak_;
        recent_ = std::max(recent_, std::max(tpL_.pushGated(l, floor), tpR_.pushGated(r, floor)));
        if (!blocks_.add(l, r)) return;
        if (blocks_.pieces() >= 4) {
            const double block = blocks_.meanOfLast(4);
            gated_.add(block);
            momentary_.store(toLufs(block), std::memory_order_relaxed);
            shortTerm_.store(toLufs(blocks_.meanOfLast(BlockLoudness::kRing)), std::memory_order_relaxed);
            const double integrated = gated_.lufs();
            integrated_.store(integrated > -99.0 ? static_cast<float>(integrated) : kNone, std::memory_order_relaxed);
        }
    }
    void publish() {
        if (resetPending_.load(std::memory_order_relaxed)) {
            reset();
            return;
        }
        peak_ = std::max(peak_, recent_);
        truePeak_.store(peak_, std::memory_order_relaxed);
        float seen = recentPeak_.load(std::memory_order_relaxed);
        while (recent_ > seen && !recentPeak_.compare_exchange_weak(seen, recent_, std::memory_order_acq_rel)) {
        }
        recent_ = 0.0f;
    }
    float momentary() const { return momentary_.load(std::memory_order_relaxed); }
    float shortTerm() const { return shortTerm_.load(std::memory_order_relaxed); }
    float integrated() const { return integrated_.load(std::memory_order_relaxed); }
    // Linear true peak since reset, and the highest since the last take (the UI's moving bar).
    float truePeak() const { return truePeak_.load(std::memory_order_relaxed); }
    float takeRecentPeak() { return recentPeak_.exchange(0.0f, std::memory_order_acq_rel); }
    // The integrated loudness, exactly (an offline render's report).
    double integratedLufs() const { return gated_.lufs(); }
    float peakSinceReset() const { return std::max(peak_, recent_); }

private:
    static float toLufs(double meanSquare) {
        const double l = lufsOf(meanSquare);
        return l > -99.0 ? static_cast<float>(l) : kNone;
    }
    BlockLoudness blocks_;
    GatedLoudness gated_;
    TruePeakReader tpL_, tpR_;
    float peak_ = 0.0f, recent_ = 0.0f;
    std::atomic<bool> resetPending_{false};
    std::atomic<float> momentary_{kNone}, shortTerm_{kNone}, integrated_{kNone};
    std::atomic<float> truePeak_{0.0f}, recentPeak_{0.0f};
};

}  // namespace ps
