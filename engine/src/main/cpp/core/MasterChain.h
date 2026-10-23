#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <cstdint>

#include "Loudness.h"
#include "Oversampling.h"
#include "TruePeak.h"

namespace ps {

// The master's last stage, after the master's gain:
//   0. with a loudness target (mastering), a DC blocker (3 Hz): an offset left by an asymmetric
//      saturation or a sample takes headroom from the clipper and the limiter;
//   1. the loudness target's drive (« Cible »): a gain moved slowly from an integrated estimate of
//      the music's loudness, never per sample;
//   2. a clipper that only takes the attack of the peaks: Airwindows ClipOnly2 (MIT, see
//      engine/third_party/airwindows), run at 4x through HIIR. Its clip level follows the held
//      level of the music, so a kick's first milliseconds over the ceiling are shaved (at most
//      kMaxClipDb) while a held bass is left whole to the limiter (no waveform distortion);
//   3. a true-peak look-ahead limiter after x42 dpl's approach (Fons Adriaensen's peaklim): 4x
//      peak reading, a hold on the bass (the gain does not ripple with a 50 Hz wave) and a release
//      that is fast after a short peak and slow once the music sits in the limiter;
//   4. with a target, a sub-sonic blocker (0.05 Hz) after the limiter: shaving and limiting the
//      taller side of an asymmetric wave (a kick's first cycles) leaves an offset of a few
//      thousandths over a song; this slow filter takes it out without moving a kick's 20 Hz tail
//      (the peaks move by 0.06 dB at most, under the ceiling's margin).
// The output is latency() frames late. Without a target and under the ceiling it is the input,
// delayed, sample for sample. prepare() off the audio thread; the rest is real-time safe.
class MasterChain {
public:
    static constexpr float kDefaultCeilingDb = -1.0f;
    // The limiter aims this far under the ceiling: 4x readers (ffmpeg's, libebur128's, the
    // meter's) differ by a few hundredths of a dB on the same peak.
    static constexpr float kMarginDb = 0.2f;
    static constexpr float kLookAheadMs = 1.0f;
    static constexpr int kMaxLookAhead = 96;
    // The clipper starts this far over the limiter's aim: smaller overs are the limiter's alone
    // (a pure gain), the clipper shaves what goes further, kMaxClipDb at most.
    static constexpr float kClipAboveDb = 1.0f;
    static constexpr float kMaxClipDb = 2.0f;
    static constexpr float kMinDriveDb = -24.0f;
    static constexpr float kMaxDriveDb = 18.0f;
    // The drive stops rising once the clipper and the limiter take this much loudness off the
    // driven music: past it they only squash it (a dense loop's loudness stops growing), and a
    // target that loud is not met (a gabber's -6 LUFS on a dense loop ends near -7).
    static constexpr float kMaxLossDb = 6.0f;
    // ClipOnly2 runs as at 44.1 kHz (its `spacing` 1: `lastSample` is the previous sample), its
    // output then delayed to a whole frame (kFactor samples at the 4x rate).
    static constexpr int kSpacing = 1;
    static constexpr int kClipDelay = Oversampler4x::kDelay + 1;
    static_assert(kClipDelay < 8, "ClipChannel::dry holds 8 frames");

    static int lookAheadFor(int rate) {
        return std::clamp(static_cast<int>(std::lround(kLookAheadMs * 0.001 * rate)), 8, kMaxLookAhead);
    }
    // Frames the output is late at `rate`.
    static int latencyFor(int rate) { return kClipDelay + TruePeakFilter::kDelay + lookAheadFor(rate) - 1; }

    void prepare(int rate);
    int latency() const { return latency_; }

    // -6..0 dBTP (-1 by default).
    void setCeilingDb(float db);
    float ceilingDb() const { return ceilingDb_; }
    // The loudness the drive aims at, in LUFS (-30..-3); NaN or >= 0: no target (drive 0 dB).
    void setTarget(float lufs);
    float target() const { return target_; }
    // Off: no drive, no clipper, no limiter, the same latency (a pre-master export).
    void setLimiter(bool on) { limiterOn_ = on; }
    bool limiterOn() const { return limiterOn_; }
    // The drive needs the master's effects on (separate tracks add back up to the mix without it).
    void setDriveAllowed(bool on) { driveAllowed_ = on; }
    bool targetActive() const { return limiterOn_ && driveAllowed_ && std::isfinite(target_); }
    // A fixed drive (an offline render's second pass): the estimate no longer moves it.
    void holdDrive(float db);
    // The drive the estimate has reached (dB): the UI reads it.
    float driveDb() const { return drivePublished_.load(std::memory_order_relaxed); }
    // What the estimate knows of the music's loudness is forgotten (another project).
    void resetEstimate();

    struct Stats {
        float inPeak = 0.0f;   // into the clipper (drive and click included)
        float minGain = 1.0f;  // the clipper's and the limiter's lowest gains, multiplied
    };
    // In place. `direct` (may be null): the click, added after the drive, limited with the music.
    Stats process(float* l, float* r, const float* direct, int n);

    LoudnessMeter& meter() { return meter_; }
    const LoudnessMeter& meter() const { return meter_; }

private:
    struct ClipChannel {
        Oversampler4x os;
        float last = 0.0f;
        float level = 1.0f;  // the level of the last clip (ClipOnly2 eases out of a clip toward it)
        bool wasPos = false, wasNeg = false;
        std::array<float, kSpacing + 1> intermediate{};
        std::array<float, Oversampler4x::kFactor> upDelay{};  // the 4x signal, one frame back
        std::array<float, Oversampler4x::kFactor - kSpacing> outDelay{};  // ClipOnly2's output, to one frame
        int upPos = 0, outPos = 0;
        std::array<float, 8> dry{};             // the input, kClipDelay frames back
        int dryPos = 0;
        int touched = 0;  // 4x samples ClipOnly2 may still change: the residual is 0 after
        int idle = 0;     // frames since the last clip
        bool downIdle = true;
        void reset();
    };
    float clipOnly2(ClipChannel& c, float in, float level, bool armed);
    float clip(ClipChannel& c, float x, float level, bool armed, float& ratio);
    void limit(float& l, float& r, float clipRatio, float& gain);
    void estimate();
    void rampDrive(float db, int frames);

    int rate_ = 48000;
    int latency_ = latencyFor(48000);
    float ceilingDb_ = kDefaultCeilingDb;
    float ceiling_ = 0.891251f;  // linear
    float aim_ = 0.881f;         // the limiter's, kMarginDb under
    float tpFloor_ = 0.352f;     // under it the true-peak filter is skipped (0.4 of the aim)
    float clipFloor_ = 0.988f;   // the clipper's lowest level, kClipAboveDb over the aim
    float target_ = NAN;
    bool limiterOn_ = true;
    bool driveAllowed_ = true;
    bool driveHeld_ = false;

    // --- Drive.
    float driveDb_ = 0.0f, rampTarget_ = 1.0f;
    float gain_ = 1.0f, gainStep_ = 0.0f;
    int rampLeft_ = 0;
    std::atomic<float> drivePublished_{0.0f};
    BlockLoudness estIn_, estOut_;
    GatedLoudness estHist_;
    double driveSq_ = 0.0;
    std::array<double, 4> pieceDrive_{1.0, 1.0, 1.0, 1.0};
    int pieceHead_ = 0;
    double sumDriven_ = 0.0, sumOut_ = 0.0, forget_ = 1.0;

    // --- DC blocker ahead of the drive (3 Hz) and sub-sonic blocker after the limiter (0.05 Hz):
    // always running, heard with a target (dcMix_ glides 0..1 over 50 ms).
    double dcR_ = 0.9996, dcPostR_ = 0.99999;
    double dcInL_ = 0.0, dcInR_ = 0.0, dcOutL_ = 0.0, dcOutR_ = 0.0;
    double dcPostInL_ = 0.0, dcPostInR_ = 0.0, dcPostOutL_ = 0.0, dcPostOutR_ = 0.0;
    float dcMix_ = 0.0f, dcStep_ = 1.0f / 2400.0f;

    // --- Clipper.
    ClipChannel clipL_, clipR_;
    float held_ = 0.0f, env_ = 0.0f;
    int armed_ = 0;  // frames the clipper still listens after a sample over its level
    int holdLeft_ = 0, holdFrames_ = 1200;
    float envAttack_ = 0.0f, envRelease_ = 0.0f;
    float maxClip_ = 1.2589f;   // kMaxClipDb, linear

    // --- Limiter.
    static constexpr int kRing = 256;
    static constexpr int kQueue = 128;
    static constexpr int kLfQueue = 4096;
    int lookAhead_ = 48, lfHold_ = 960;
    int64_t frame_ = 0;
    std::array<float, kRing> delayL_{}, delayR_{};
    std::array<float, kRing> clipRatio_{};  // what the clipper left of each frame (the meters)
    TruePeakReader tpL_, tpR_;
    float prevSegment_ = 0.0f;
    std::array<int64_t, kQueue> minFrame_{};
    std::array<float, kQueue> minGain_{};
    int minHead_ = 0, minTail_ = 0;
    std::array<int64_t, kLfQueue> lfFrame_{};
    std::array<float, kLfQueue> lfGain_{};
    int lfHead_ = 0, lfTail_ = 0;
    float lpL_ = 0.0f, lpR_ = 0.0f, lpCoef_ = 0.06f;
    std::array<float, kMaxLookAhead> box_{};
    double boxSum_ = kMaxLookAhead;
    int nonUnity_ = 0;  // entries of box_ under 1
    int boxPos_ = 0;
    float rel_ = 1.0f, avg_ = 1.0f;
    float relFast_ = 0.0f, relSlow_ = 0.0f, avgCoef_ = 0.0f;

    LoudnessMeter meter_;
};

}  // namespace ps
