#pragma once

// Lot E (« Master pro »): the readings a mastering engineer asks of a file, besides the integrated
// loudness and the true peak (Loudness.h, TruePeak.h):
//   - the highest momentary (400 ms) and short-term (3 s) loudness, every 100 ms as EBU R128 reads them;
//   - the loudness range (LRA, EBU Tech 3342): the short-term values over the absolute gate (-70 LUFS)
//     and 20 LU under their power mean, from their 10th to their 95th percentile;
//   - the stereo correlation (+1 mono, 0 unrelated, -1 out of phase) and the loudness of the mono fold
//     ((L + R) / 2 on both speakers): what a phone speaker or a club's mono keeps of the mix.
// PLR (the true peak over the integrated loudness) is read from those. Fixed-size state only: real-time
// safe once prepared (an offline render calls it per frame; the live engine does not run it).

#include <algorithm>
#include <array>
#include <cmath>

#include "Loudness.h"

namespace ps {

class LoudnessRange {
public:
    static constexpr double kGateAbs = -70.0;
    static constexpr double kGateRel = -20.0;
    static constexpr double kStep = 0.1;
    static constexpr int kBins = 800;  // -70 .. +10 LUFS

    void prepare(int rate) {
        stereo_.prepare(rate);
        mono_.prepare(rate);
        reset();
    }
    void reset() {
        stereo_.reset();
        mono_.reset();
        monoGated_.reset();
        hist_.fill(0.0);
        energy_.fill(0.0);
        maxMomentary_ = maxShortTerm_ = shortest_ = -1000.0;
        sumLR_ = sumLL_ = sumRR_ = 0.0;
    }
    void add(float l, float r) {
        sumLR_ += double(l) * r;
        sumLL_ += double(l) * l;
        sumRR_ += double(r) * r;
        const float m = 0.5f * (l + r);
        if (mono_.add(m, m) && mono_.pieces() >= 4) monoGated_.add(mono_.meanOfLast(4));
        if (!stereo_.add(l, r)) return;
        const long pieces = stereo_.pieces();
        if (pieces >= 4) maxMomentary_ = std::max(maxMomentary_, lufsOf(stereo_.meanOfLast(4)));
        // Short-term from its first full 3 s (a file shorter than that: its whole length so far).
        if (pieces >= 4) {
            const double ms = stereo_.meanOfLast(BlockLoudness::kRing);
            const double st = lufsOf(ms);
            if (pieces >= BlockLoudness::kRing) {
                maxShortTerm_ = std::max(maxShortTerm_, st);
                if (st > kGateAbs) {
                    const int bin = std::clamp(static_cast<int>((st - kGateAbs) / kStep), 0, kBins - 1);
                    hist_[static_cast<size_t>(bin)] += 1.0;
                    energy_[static_cast<size_t>(bin)] += ms;
                }
            } else {
                shortest_ = std::max(shortest_, st);
            }
        }
    }

    double maxMomentary() const { return maxMomentary_; }
    // Under 3 s of sound: the loudest of the shorter windows (else -1000).
    double maxShortTerm() const { return maxShortTerm_ > -999.0 ? maxShortTerm_ : shortest_; }
    // LU; 0 under 3 s of sound.
    double lra() const {
        double e = 0.0, w = 0.0;
        for (int i = 0; i < kBins; ++i) {
            e += energy_[static_cast<size_t>(i)];
            w += hist_[static_cast<size_t>(i)];
        }
        if (w <= 0.0) return 0.0;
        const double gate = lufsOf(e / w) + kGateRel;
        const int first = std::clamp(static_cast<int>(std::floor((gate - kGateAbs) / kStep - 0.5)) + 1, 0, kBins);
        double n = 0.0;
        for (int i = first; i < kBins; ++i) n += hist_[static_cast<size_t>(i)];
        if (n <= 0.0) return 0.0;
        const double lo = percentile(first, n, 0.10), hi = percentile(first, n, 0.95);
        return std::max(0.0, hi - lo);
    }
    // -1..1 (1 for silence: nothing out of phase).
    double correlation() const {
        const double d = std::sqrt(sumLL_ * sumRR_);
        return d > 1e-20 ? std::clamp(sumLR_ / d, -1.0, 1.0) : 1.0;
    }
    // The mono fold's integrated loudness (LUFS, -1000: silence).
    double monoLufs() const { return monoGated_.lufs(); }

private:
    // The value (bin middle) under which `share` of the gated windows lie.
    double percentile(int first, double n, double share) const {
        const double want = share * n;
        double seen = 0.0;
        for (int i = first; i < kBins; ++i) {
            seen += hist_[static_cast<size_t>(i)];
            if (seen >= want) return kGateAbs + kStep * (i + 0.5);
        }
        return kGateAbs + kStep * (kBins - 0.5);
    }

    BlockLoudness stereo_, mono_;
    GatedLoudness monoGated_;
    std::array<double, kBins> hist_{}, energy_{};
    double maxMomentary_ = -1000.0, maxShortTerm_ = -1000.0, shortest_ = -1000.0;
    double sumLR_ = 0.0, sumLL_ = 0.0, sumRR_ = 0.0;
};

}  // namespace ps
