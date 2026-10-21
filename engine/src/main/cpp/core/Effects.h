#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <vector>

#include "Effects/chorus.h"
#include "Effects/overdrive.h"
#include "Filters/svf.h"

namespace ps {

// Mixer effects. Every class: prepare() on a non-audio thread (it may allocate), then
// setParam()/process() on the audio thread (never allocate, never lock). Parameters are
// normalised 0..1; each effect maps them to musical ranges. Stereo, in place.

// RBJ cookbook biquad, transposed direct form II.
class Biquad {
public:
    void lowShelf(float rate, float freq, float gainDb);
    void highShelf(float rate, float freq, float gainDb);
    void peak(float rate, float freq, float q, float gainDb);
    void highPass(float rate, float freq);  // 2nd order, Q 0.707
    void lowPass(float rate, float freq);   // 2nd order, Q 0.707
    void bypass() { b0_ = 1; b1_ = b2_ = a1_ = a2_ = 0; }
    void reset() { z1_ = z2_ = 0.0f; }
    float process(float x) {
        const float y = b0_ * x + z1_;
        z1_ = b1_ * x - a1_ * y + z2_;
        z2_ = b2_ * x - a2_ * y;
        return y;
    }

private:
    void set(double b0, double b1, double b2, double a0, double a1, double a2);
    float b0_ = 1, b1_ = 0, b2_ = 0, a1_ = 0, a2_ = 0, z1_ = 0, z2_ = 0;
};

// Three bands: low shelf 100 Hz, bell 1 kHz, high shelf 8 kHz, each ±12 dB (0.5 = flat).
class Eq3 {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);

private:
    void update();
    float rate_ = 48000;
    std::array<float, 3> gain_{0.5f, 0.5f, 0.5f};
    std::array<Biquad, 3> left_{}, right_{};
};

// Feed-forward compressor, stereo-linked, soft knee, automatic make-up gain.
// Params: threshold (-40..0 dB), ratio (1..20), attack (0.1..80 ms), release (20..800 ms).
class Compressor {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() { envDb_ = -120.0f; reduction_ = 0.0f; }
    void process(float* l, float* r, int n);
    float gainReductionDb() const { return reduction_; }  // tests, meters (≤ 0)

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.6f, 0.2f, 0.2f, 0.3f};
    float threshold_ = -16, ratio_ = 4, attack_ = 0, release_ = 0, makeup_ = 1;
    float envDb_ = -120.0f;
    float reduction_ = 0.0f;
};

// Drive (DaisySP Overdrive), tone low-pass, dry/wet, output level.
class Saturator {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() { lpL_ = lpR_ = 0.0f; }
    void process(float* l, float* r, int n);

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.3f, 0.7f, 1.0f, 0.7f};
    daisysp::Overdrive odL_, odR_;
    float toneCoef_ = 1.0f, mix_ = 1.0f, level_ = 1.0f;
    float lpL_ = 0.0f, lpR_ = 0.0f;
};

// DaisySP stereo chorus. Params: rate, depth, delay, mix.
class StereoChorus {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() { prepare(rate_); }  // DaisySP's Init clears the delay lines, no allocation
    void process(float* l, float* r, int n);

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.3f, 0.5f, 0.4f, 0.5f};
    daisysp::Chorus chorus_;
    float mix_ = 0.5f;
};

// DaisySP state-variable filter. Params: cutoff (40 Hz..18 kHz), resonance, type (low/band/high), mix.
/** Output gain making up for a filter's resonant peak (no internal drive), see Effects.cpp. */
float resonanceCompensation(float res);

class SweepFilter {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() { prepare(rate_); }
    void process(float* l, float* r, int n);

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.6f, 0.2f, 0.0f, 1.0f};
    daisysp::Svf left_, right_;
    int type_ = 0;
    float mix_ = 1.0f;
    float gain_ = 1.0f;  // resonance compensation (see resonanceCompensation)
};

// Sidechain "pump": the signal ducks on every kick (or every beat) and swells back.
// Params: depth (0..1), release (50..600 ms), source (< 0.5 kick hits, else every beat).
class Ducker {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() { env_ = 0.0f; pending_ = -1; }
    // A kick lands `offset` frames into the next process() call.
    void trigger(int offset) { pending_ = offset; }
    bool everyBeat() const { return p_[2] >= 0.5f; }
    void process(float* l, float* r, int n);
    float gain() const { return 1.0f - depth_ * env_; }  // tests

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.7f, 0.4f, 0.0f, 0.0f};
    float depth_ = 0.7f, release_ = 0.999f, attack_ = 0.1f, env_ = 0.0f, target_ = 0.0f;
    int pending_ = -1;
};

// The live performance effects of the master (PerformanceFx) are at the end of this file: they
// use the delay and the band split declared below.

// One insert slot: any of the insert effects, chosen by type. All are kept prepared so a
// type change on the audio thread is just a reset.
// Parametric EQ: one bell (frequency 20 Hz..20 kHz, gain ±18 dB, width Q 0.3..10) and a
// low cut (0 off, else 20..500 Hz).
class ParamEq {
public:
    void prepare(float rate) { rate_ = rate; update(); reset(); }
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.5f, 0.5f, 0.4f, 0.0f};
    Biquad bellL_, bellR_, cutL_, cutR_;
};

// Our own light effects (no big fixed buffers: 68 insert slots each hold one of every type).

// Flanger: a 1-10 ms swept delay with feedback, the two sides a quarter turn apart.
// Params: rate (0.05..5 Hz), depth, feedback (0..0.9), mix.
class Flanger {
public:
    void prepare(float rate);  // allocates the delay lines
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.2f, 0.6f, 0.5f, 0.5f};
    std::vector<float> bufL_, bufR_;
    int pos_ = 0;
    double phase_ = 0.0;
    float read(const std::vector<float>& b, float delay) const;
};

// Phaser: six first-order allpass stages swept by an LFO, with feedback.
// Params: rate (0.05..5 Hz), depth, feedback (0..0.85), mix.
class Phaser {
public:
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);

private:
    static constexpr int kStages = 6;
    float rate_ = 48000;
    std::array<float, 4> p_{0.2f, 0.7f, 0.5f, 0.5f};
    std::array<float, kStages> zl_{}, zr_{};
    float fbL_ = 0.0f, fbR_ = 0.0f;
    double phase_ = 0.0;
};

// Noise gate: closes below the threshold (with hold), opens fast.
// Params: threshold (-80..0 dB), attack (0.1..50 ms), release (10..1000 ms), range (how far it closes).
class Gate {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() { env_ = 0.0f; gain_ = 1.0f; hold_ = 0; }
    void process(float* l, float* r, int n);
    float gain() const { return gain_; }  // tests

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.4f, 0.2f, 0.4f, 1.0f};
    float threshold_ = 0.01f, attack_ = 0.1f, release_ = 0.001f, floor_ = 0.0f;
    float env_ = 0.0f, gain_ = 1.0f;
    int hold_ = 0, holdFrames_ = 2400;
};

// Tremolo / auto-pan. Params: rate (0.1..20 Hz), depth, shape (sine .. square), stereo (0 tremolo .. 1 pan).
class Tremolo {
public:
    void prepare(float rate) { rate_ = rate; }
    void setParam(int p, float v);
    void reset() { phase_ = 0.0; }
    void process(float* l, float* r, int n);

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.4f, 0.6f, 0.0f, 0.0f};
    double phase_ = 0.0;
};

// Stereo widener (mid/side). Params: width (0 mono .. 0.5 as is .. 1 twice as wide), bass in mono below
// (0 off, else 40..300 Hz), -, -.
class Widener {
public:
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset() { lowS_ = 0.0f; }
    void process(float* l, float* r, int n);

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.75f, 0.0f, 0.0f, 0.0f};
    float lowS_ = 0.0f;
};

// Bitcrusher: fewer bits and a lower sample rate. Params: bits (16..2), downsample (1..32x), mix, volume.
class Bitcrusher {
public:
    void prepare(float) {}
    void setParam(int p, float v);
    void reset() { count_ = 0; holdL_ = holdR_ = 0.0f; }
    void process(float* l, float* r, int n);

private:
    std::array<float, 4> p_{0.5f, 0.3f, 1.0f, 0.7f};
    int count_ = 0;
    float holdL_ = 0.0f, holdR_ = 0.0f;
};

// The four "macros" of a track, for quick sound play (pad XY, big knobs): colour (0 = dark
// low-pass .. 0.5 = untouched .. 1 = thin high-pass), dirt (drive), motion (chorus) and space
// (added to the reverb and delay sends, applied by the mixer). Each stage is skipped at rest.
class TrackMacros {
public:
    enum Macro : int { Color, Dirt, Motion, Space, kCount };
    void prepare(float rate);
    void reset() { prepare(rate_); }  // after a NaN: every stage back to silence, no allocation
    void set(int macro, float v);
    float get(int macro) const { return v_[static_cast<size_t>(macro)]; }
    void process(float* l, float* r, int n);

private:
    void update();
    float rate_ = 48000;
    std::array<float, kCount> v_{0.5f, 0.0f, 0.0f, 0.0f};
    daisysp::Svf left_, right_;
    bool highPass_ = false;
    float filterMix_ = 0.0f;   // target
    float filterMixNow_ = 0.0f;  // smoothed: no click when the colour leaves or reaches the middle
    Saturator drive_;
    float inPower_ = 0.0f, outPower_ = 0.0f, makeup_ = 1.0f;
    bool dirtOn_ = false;
    StereoChorus chorus_;
};

// --- The "crazy" effects: fun first, each in a few lines and with no buffer of its own beyond a
// few milliseconds (there are 128 insert slots). ---------------------------------------------

// Ring modulator ("Robot"): the sound times a sine. Params: pitch (30..2000 Hz), mix, wobble
// (how far the pitch swings), wobble speed (0.1..8 Hz).
class RingMod {
public:
    void prepare(float rate) { rate_ = rate; }
    void setParam(int p, float v);
    void reset() { phase_ = 0.0; lfo_ = 0.0; }
    void process(float* l, float* r, int n);

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.45f, 1.0f, 0.0f, 0.3f};
    double phase_ = 0.0, lfo_ = 0.0;
};

// Envelope filter ("Wah-wah"): the louder the note, the higher a resonant band-pass opens.
// Params: sensitivity, lowest frequency (150..1200 Hz, opens 3.5 octaves up), resonance, mix.
class AutoWah {
public:
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset() { env_ = 0.0f; s1L_ = s2L_ = s1R_ = s2R_ = 0.0f; }
    void process(float* l, float* r, int n);
    float cutoff() const { return cutoff_; }  // tests

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.6f, 0.3f, 0.6f, 1.0f};
    float env_ = 0.0f, cutoff_ = 0.0f;
    float s1L_ = 0.0f, s2L_ = 0.0f, s1R_ = 0.0f, s2R_ = 0.0f;  // TPT state-variable filter
    float g_ = 0.0f, a1_ = 1.0f, a2_ = 0.0f;                    // its coefficients (every 8 frames)
};

// Old record ("Vieux vinyle"): wow and flutter (a slowly moving short delay), crackles, hiss and
// a dull, narrow tone. Params: wow, crackle, age (tone and hiss), mix.
class Vinyl {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);

private:
    float noise();
    float rate_ = 48000;
    std::array<float, 4> p_{0.4f, 0.4f, 0.5f, 1.0f};
    std::vector<float> bufL_, bufR_;
    size_t write_ = 0;
    double wow_ = 0.0, flutter_ = 0.0;
    float wowNow_ = 0.0f;  // the wow knob, smoothed: turning it never jumps the read point
    float lp1L_ = 0, lp2L_ = 0, lp1R_ = 0, lp2R_ = 0, click_ = 0.0f;
    uint32_t seed_ = 0x9E3779B9u;
};

// Walkie-talkie ("Talkie-walkie"): a narrow mono band, crunchy, with static under the voice.
// Params: narrowness, drive, static, mix.
class Radio {
public:
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset() { hp1_ = hp2_ = lp1_ = lp2_ = prevIn_ = prevHp_ = env_ = 0.0f; }
    void process(float* l, float* r, int n);

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.5f, 0.4f, 0.3f, 1.0f};
    float hp1_ = 0, hp2_ = 0, lp1_ = 0, lp2_ = 0, prevIn_ = 0, prevHp_ = 0, env_ = 0;
    uint32_t seed_ = 0x2545F491u;
};

// Rhythmic gate ("Hachoir"): chops the sound in sixteenths (or 32nds) after a pattern of one
// beat, in time with the song (each beat resyncs it). Params: pattern (8), depth, smoothness
// (1..30 ms), speed (sixteenths .. 32nds).
class Chopper {
public:
    static constexpr int kPatterns = 8;
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset() { gain_ = 1.0f; sinceBeat_ = 0; pending_ = -1; }
    // A beat lands `offset` frames into the next process() call: the pattern starts again there.
    void beat(int offset) { pending_ = offset; }
    // Starting to play [frames] after the last beat (it counts only while it plays): in phase at once.
    void syncTo(int64_t frames) { sinceBeat_ = std::max<int64_t>(0, frames); pending_ = -1; }
    // The song's tempo, from the sequencer every block: a beat is four sixteenths.
    void setStepFrames(double framesPerSixteenth) {
        if (framesPerSixteenth > 0.0) beatFrames_ = std::max<int64_t>(1, std::llround(4.0 * framesPerSixteenth));
    }
    void process(float* l, float* r, int n);
    float gain() const { return gain_; }  // tests

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.0f, 1.0f, 0.2f, 0.0f};
    float gain_ = 1.0f;
    int64_t sinceBeat_ = 0, beatFrames_ = 24000;  // 120 BPM until the song tells
    int pending_ = -1;
};


// --- The electronic music classics (phase 25): small ones, held by every slot ---------------

// Distortion: drive into one of four curves, then a tone low-pass and a dry/wet mix. The output
// is levelled (a hot drive is not much louder than a light one). Params: drive (x1..x60), tone
// (800 Hz..18 kHz), type (overdrive, fuzz, hard clip, foldback), mix.
class Distortion {
public:
    static constexpr int kTypes = 4;
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset() { lp1L_ = lp2L_ = lp1R_ = lp2R_ = dcInL_ = dcOutL_ = dcInR_ = dcOutR_ = 0.0f; }
    void process(float* l, float* r, int n);
    static float shape(int type, float x);  // tests

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.45f, 0.6f, 0.0f, 1.0f};
    float lp1L_ = 0, lp2L_ = 0, lp1R_ = 0, lp2R_ = 0;
    float dcInL_ = 0, dcOutL_ = 0, dcInR_ = 0, dcOutR_ = 0;
};

// Auto-pan in time with the song: the sound travels left and right once per 2 bars, bar, 1/2,
// 1/4, 1/8 or 1/16 note; each beat puts it back on the grid. Params: rate, depth, shape (sine ..
// square), -.
class AutoPan {
public:
    static constexpr int kRates = 6;
    void prepare(float rate) { rate_ = rate; }
    void setParam(int p, float v);
    void reset() { phase_ = 0.0; pending_ = -1; }
    void setStepFrames(double framesPerSixteenth) { if (framesPerSixteenth > 0.0) stepFrames_ = framesPerSixteenth; }
    void beat(int offset) { pending_ = offset; }
    void process(float* l, float* r, int n);
    double phase() const { return phase_; }  // tests
    static int stepsOf(float v);              // sixteenths per round trip

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.58f, 0.7f, 0.0f, 0.0f};
    double phase_ = 0.0, stepFrames_ = 6000.0;
    int pending_ = -1;
};

// "OTT": three-band compression, upwards and downwards at once (the loud parts squashed, the
// quiet details brought up), the famous over-the-top sound of EDM. Bands split at 120 Hz and
// 2.5 kHz (Linkwitz-Riley, 24 dB/octave). Params: depth (dry/wet), speed (attack and release
// x0.25..x4), upward, downward.
class Ott {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    float bandGainDb(int band) const { return gainDb_[static_cast<size_t>(band)]; }  // tests

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.6f, 0.5f, 0.7f, 0.7f};
    // Per side: low = LP1·LP1, rest = HP1·HP1, mid = LP2·LP2(rest), high = HP2·HP2(rest).
    struct Split {
        Biquad lp1a, lp1b, hp1a, hp1b, lp2a, lp2b, hp2a, hp2b;
    };
    Split left_, right_;
    std::array<float, 3> env_{}, gainDb_{}, gain_{1.0f, 1.0f, 1.0f}, attack_{}, release_{};
    int count_ = 0;
};

// Frequency shifter: every partial moves up (or down) by the same number of hertz, which no pitch
// shift does: metallic, bell-like, or a slow phasing when the shift is a few hertz. A Hilbert
// transform (two chains of allpasses, Olli Niemitalo's coefficients) and a quadrature oscillator.
// Params: shift (0.5 = none, -2000..+2000 Hz, finer near the middle), feedback, mix.
class FreqShifter {
public:
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    static float shiftHz(float v);

private:
    struct Allpass2 {
        float a2 = 0, x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        float run(float x) {
            const float y = a2 * (x + y2) - x2;
            x2 = x1; x1 = x; y2 = y1; y1 = y;
            return y;
        }
    };
    struct Hilbert {
        std::array<Allpass2, 4> a, b;
        float aDelay = 0;
        void init();
        void run(float x, float& re, float& im);
    };
    float rate_ = 48000;
    std::array<float, 4> p_{0.6f, 0.0f, 0.5f, 0.0f};
    Hilbert hL_, hR_;
    double phase_ = 0.0;
    float fbL_ = 0, fbR_ = 0;
};

// Effects with seconds of memory (insert echo, insert reverb, tape stop) are too big for every
// slot to hold one: a few of each wait in a pool the mixer fills before the stream starts, and a
// slot borrows one when it takes that type (no allocation on the audio thread).
class InsertEcho;
class InsertReverb;
class TapeStop;
class LongFxPool;

// Wobble (phase 27): a resonant low-pass whose cutoff an LFO moves in time with the song: the
// dubstep "wob", the drum & bass reese's movement. The LFO's phase follows the transport (each
// beat puts it back where the song is), so it plays the same live and in an export.
// Params: rate (1/1, 1/2, 1/4, 1/4T, 1/8, 1/8T, 1/16, 1/16T, 1/32), depth (how far up the
// filter opens from 80 Hz: up to 8 octaves), resonance, shape (sine, triangle, saw down, square).
class Wobble {
public:
    static constexpr int kRates = 9, kShapes = 4;
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void setStepFrames(double framesPerSixteenth) { if (framesPerSixteenth > 0.0) stepFrames_ = framesPerSixteenth; }
    // A beat lands `offset` frames into the next process(); `step`: the transport step it is (-1: unknown).
    void beat(int offset, int step) { pending_ = offset; pendingStep_ = step; }
    void process(float* l, float* r, int n);
    static double stepsOf(float v);  // sixteenths per LFO cycle
    static float shape(int shape, double phase);  // 0..1
    double phase() const { return phase_; }  // tests
    float cutoff() const { return cutoffNow_; }  // tests

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.45f, 0.7f, 0.5f, 0.0f};
    daisysp::Svf left_, right_;
    double phase_ = 0.0, stepFrames_ = 6000.0;
    int pending_ = -1, pendingStep_ = -1;
    float cutoffNow_ = 80.0f;
    int countdown_ = 0;
};

class InsertSlot {
public:
    enum Type {
        None = 0, Eq = 1, Comp = 2, Saturation = 3, Chorus = 4, Filter = 5, Pump = 6,
        FlangerFx = 7, PhaserFx = 8, GateFx = 9, TremoloFx = 10, WidenerFx = 11, Crusher = 12, ParamEqFx = 13,
        RobotFx = 14, WahFx = 15, VinylFx = 16, RadioFx = 17, ChopperFx = 18,
        DistortionFx = 19, EchoFx = 20, PingPongFx = 21, ReverbFx = 22, AutoPanFx = 23, OttFx = 24,
        FreqShiftFx = 25, TapeStopFx = 26, WobbleFx = 27, kTypes = 28
    };
    // Whether `type` needs a unit of the pool (seconds of memory).
    static bool isLong(int type) { return type == EchoFx || type == PingPongFx || type == ReverbFx || type == TapeStopFx; }
    static constexpr int kParams = 4;

    void prepare(float rate);
    void setType(int type);
    void setParam(int p, float v);
    int type() const { return type_; }
    void process(float* l, float* r, int n);
    void reset();  // the effect's state back to silence (after a NaN, back in a scene), no allocation
    const Compressor& compressor() const { return comp_; }  // tests
    const Ducker& ducker() const { return pump_; }          // tests
    // Sidechain events (only the Pump type listens). `beat`: the step is a quarter note.
    void setStepFrames(double framesPerSixteenth);
    // `step`: the transport step it is (-1: unknown), for the wobble's phase.
    void sidechain(int offset, bool kick, bool beat, int step = -1) {
        if (type_ == Pump && (pump_.everyBeat() ? beat : kick)) pump_.trigger(offset);
        if (type_ == ChopperFx && beat) chopper_.beat(offset);
        if (type_ == AutoPanFx && beat) autoPan_.beat(offset);
        if (type_ == WobbleFx && beat) wobble_.beat(offset, step);
    }
    const Wobble& wobble() const { return wobble_; }  // tests
    // The pool the long effects borrow from (none: those types let the sound through untouched).
    void attachPool(LongFxPool* pool) { pool_ = pool; }
    // A long type still waiting for a free unit (they were all taken): try again (audio thread, no allocation).
    bool waitingForUnit() const { return isLong(type_) && !hasUnit(); }
    void takeUnit();
    bool hasUnit() const { return echo_ != nullptr || reverb_ != nullptr || tape_ != nullptr; }
    // The insert echo's repeat time when one plays here (0: none), for the export's tail.
    float echoFrames() const;

private:
    void releaseUnit();
    void resetState();  // the state of the type in place (not the long units)
    int type_ = None;
    LongFxPool* pool_ = nullptr;
    InsertEcho* echo_ = nullptr;
    InsertReverb* reverb_ = nullptr;
    TapeStop* tape_ = nullptr;
    // The last parameters of the current type: a unit borrowed later starts from them.
    std::array<float, kParams> longParams_{};
    Distortion distortion_;
    AutoPan autoPan_;
    Ott ott_;
    FreqShifter shifter_;
    Eq3 eq_;
    Compressor comp_;
    Saturator sat_;
    StereoChorus chorus_;
    SweepFilter filter_;
    Ducker pump_;
    Flanger flanger_;
    Phaser phaser_;
    Gate gate_;
    Tremolo tremolo_;
    Widener widener_;
    Bitcrusher crusher_;
    ParamEq paramEq_;
    RingMod robot_;
    AutoWah wah_;
    Vinyl vinyl_;
    Radio radio_;
    Chopper chopper_;
    Wobble wobble_;
};

// Freeverb (Jezar at Dreampoint, public domain): 8 parallel combs + 4 allpasses per side.
// 100 % wet (it sits on a send bus). Params: size, damping, width, pre-delay (0..200 ms).
class Reverb {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    // Freeze (Freeverb's own): the input fades out and the tank rings on for ever (feedback 1, no
    // damping), gliding in and out over about 10 ms. Off, the tail dies away as usual.
    void setFreeze(bool on) { freeze_ = on; }
    bool frozen() const { return freeze_; }
    float freezeMix() const { return freezeMix_; }  // tests
    // Adds the reverb of (inL, inR) into (outL, outR).
    void process(const float* inL, const float* inR, float* outL, float* outR, int n);

private:
    struct Comb {
        std::vector<float> buf;
        size_t idx = 0;
        float store = 0;
    };
    struct Allpass {
        std::vector<float> buf;
        size_t idx = 0;
    };
    void update();
    std::array<float, 4> p_{0.6f, 0.5f, 1.0f, 0.1f};
    std::array<Comb, 8> combL_, combR_;
    std::array<Allpass, 4> apL_, apR_;
    std::vector<float> pre_;
    size_t preIdx_ = 0, preLen_ = 1;
    float feedback_ = 0.84f, damp1_ = 0.2f, damp2_ = 0.8f, wet1_ = 1.0f, wet2_ = 0.0f;
    float rate_ = 48000;
    bool freeze_ = false;
    float freezeMix_ = 0.0f, freezeStep_ = 1.0f / 480.0f;
};

// Tempo-synced stereo delay with ping-pong, feedback tone and smooth time changes.
// 100 % wet. Params: time (1/16, 1/8, 3/16, 1/4, 3/8, 1/2 note), feedback, ping-pong, tone.
class TempoDelay {
public:
    static constexpr float kMaxSeconds = 2.5f;
    void prepare(float rate) { prepare(rate, kMaxSeconds); }
    void prepare(float rate, float maxSeconds);
    void setParam(int p, float v);
    void setStepFrames(double framesPerSixteenth);  // from the sequencer, each block
    void reset();
    void process(const float* inL, const float* inR, float* outL, float* outR, int n);
    float delayFrames() const { return target_; }  // tests

private:
    void update();
    float read(const std::vector<float>& buf, float delay) const;
    std::array<float, 4> p_{0.33f, 0.4f, 1.0f, 0.6f};
    std::vector<float> bufL_, bufR_;
    size_t write_ = 0;
    // Frames written since the last reset (up to the buffer's size): older ones read as silence,
    // so a reset never has to clear seconds of memory (a pool unit borrowed in the callback).
    size_t filled_ = 0;
    // Reset and nothing played since: the time follows its target at once (the first repeat of
    // a new echo is on time, without a glide from the defaults to its real settings).
    bool fresh_ = true;
    double stepFrames_ = 6000.0;
    float target_ = 12000.0f, current_ = 12000.0f, smooth_ = 0.001f;
    float feedback_ = 0.4f, pingPong_ = 1.0f, toneCoef_ = 0.5f, lpL_ = 0, lpR_ = 0;
    float rate_ = 48000;
};

// Insert echo: the send delay's engine on one track, with a dry/wet mix. Two types share it:
// "Écho" (straight stereo repeats) and "Écho ping-pong" (repeats bouncing left and right).
// Params: time (1/16, 1/8, dotted 1/8, 1/4, dotted 1/4, 1/2 note), feedback, tone of the repeats, mix.
class InsertEcho {
public:
    static constexpr float kMaxSeconds = 1.6f;  // a half note at 40 BPM is 1.5 s
    void prepare(float rate) { delay_.prepare(rate, kMaxSeconds); applyParams(); }
    void setParam(int p, float v);
    void setPingPong(bool on) { pingPong_ = on; delay_.setParam(2, on ? 1.0f : 0.0f); }
    void setStepFrames(double framesPerSixteenth) { delay_.setStepFrames(framesPerSixteenth); }
    void reset() { delay_.reset(); }
    void process(float* l, float* r, int n);
    float delayFrames() const { return delay_.delayFrames(); }

private:
    void applyParams();
    TempoDelay delay_;
    std::array<float, 4> p_{0.42f, 0.4f, 0.55f, 0.35f};
    bool pingPong_ = false;
};

// Insert reverb: the send reverb (Freeverb) on one track, with a dry/wet mix.
// Params: size, damping, pre-delay (0..200 ms), mix.
class InsertReverb {
public:
    void prepare(float rate) { reverb_.prepare(rate); applyParams(); }
    void setParam(int p, float v);
    void reset() { reverb_.reset(); }
    void process(float* l, float* r, int n);

private:
    void applyParams();
    Reverb reverb_;
    std::array<float, 4> p_{0.6f, 0.5f, 0.1f, 0.3f};
};

// Tape stop ("Arrêt de bande"): the "stop" knob past its middle slows the sound to a halt like a
// tape machine losing power; back under the middle, the sound returns (faded in). Automate the
// knob to place it in a song. Params: stop (switch), stop time (0.1..2 s), return fade (0..300 ms).
class TapeStop {
public:
    static constexpr float kMaxSeconds = 1.05f;  // the most the reading falls behind: half the stop time
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    float speed() const { return static_cast<float>(speed_); }  // tests

private:
    std::array<float, 4> p_{0.0f, 0.5f, 0.2f, 0.0f};
    float rate_ = 48000;
    std::vector<float> bufL_, bufR_;
    size_t write_ = 0;
    double readPos_ = 0.0, speed_ = 1.0;
    bool stopping_ = false;
    float back_ = 1.0f;  // the return fade-in, 0..1
};

template <class T, int N>
class UnitPool {
public:
    void prepare(float rate) {
        for (auto& u : units_) u.prepare(rate);
    }
    T* take() {
        for (size_t i = 0; i < units_.size(); ++i) {
            if (!used_[i]) {
                used_[i] = true;
                units_[i].reset();
                return &units_[i];
            }
        }
        return nullptr;
    }
    void give(T* unit) {
        for (size_t i = 0; i < units_.size(); ++i) {
            if (&units_[i] == unit) used_[i] = false;
        }
    }
    int available() const { return static_cast<int>(std::count(used_.begin(), used_.end(), false)); }

private:
    std::array<T, N> units_{};
    std::array<bool, N> used_{};
};

class LongFxPool {
public:
    // Shared by every slot of the mixer (32 strips x 8 slots): about 7.5 MB of echoes (0.62 MB
    // each), 1.8 MB of reverbs (0.15 MB) and 2.5 MB of tape stops (0.41 MB) at 48 kHz, 11.7 MB
    // in all per engine (the export's engine holds the same while it renders).
    static constexpr int kEchoes = 12, kReverbs = 12, kTapeStops = 6;
    void prepare(float rate) {
        echoes.prepare(rate);
        reverbs.prepare(rate);
        tapeStops.prepare(rate);
    }
    UnitPool<InsertEcho, kEchoes> echoes;
    UnitPool<InsertReverb, kReverbs> reverbs;
    UnitPool<TapeStop, kTapeStops> tapeStops;
};

// DJ isolator (« kills »): three bands split at 250 Hz and 2.5 kHz (Linkwitz-Riley, 24 dB per
// octave), each of which can be cut. At rest it costs nothing and the sound is untouched; a kill
// crossfades the split in (about 10 ms: its phase is not quite the dry sound's), each band's gain
// glides in about 10 ms, and with no kill left the split crossfades back out.
class Isolator {
public:
    static constexpr int kBands = 3;
    void prepare(float rate);
    void reset();
    void setKill(int band, bool on) {
        if (band >= 0 && band < kBands) kill_[static_cast<size_t>(band)] = on;
    }
    bool kill(int band) const { return band >= 0 && band < kBands && kill_[static_cast<size_t>(band)]; }
    bool active() const { return engaged() || mix_ > 0.0f; }
    void process(float* l, float* r, int n);
    float bandGain(int band) const { return gain_[static_cast<size_t>(band)]; }  // tests

private:
    bool engaged() const { return kill_[0] || kill_[1] || kill_[2]; }
    struct Split {
        Biquad lp1a, lp1b, hp1a, hp1b, lp2a, lp2b, hp2a, hp2b;
    };
    float rate_ = 48000;
    Split left_, right_;
    std::array<bool, kBands> kill_{};
    std::array<float, kBands> gain_{1.0f, 1.0f, 1.0f};
    float mix_ = 0.0f, step_ = 1.0f / 480.0f;
};

// Live performance effects on the master, played from the Live page (and from the song curves a
// recorded performance wrote): DJ filter, beat repeat (grid-locked rolls 1/4..1/32 and triplets),
// tape stop with a restart, backspin, reverse, half time, chopper, echo out, isolator kills, a
// noise riser / downlifter and the one-knob « Montée » (build-up). Stereo, in place.
//
// Timing (all in the audio thread, from the sequencer's steps): a held effect starts on the next
// line of the grid (Grid: off, 1/16 or beat; with the transport stopped, at once); a repeat loops
// a slice that starts on its division's line (the music played since that line goes on to the
// next one, then repeats); a riser starts on the next bar line and ends on a bar line; « Drop »
// resets everything on the next bar line. What a song curve switches on a beat therefore sounds
// the same as the gesture it was recorded from.
//
// The reverb freeze and the throws are the mixer's (they act on its send buses); the mixer reads
// gridSteps() and buildSend() from here.
class PerformanceFx {
public:
    enum Param {
        Filter = 0, Resonance = 1, Repeat = 2, RepeatDivision = 3, TapeStop = 4, Chop = 5,
        // 6 and 7 are the reverb and echo returns (the mixer's: the same numbers as Automation::Live).
        Backspin = 8, Reverse = 9, HalfTime = 10, EchoOut = 11, KillLow = 12, KillMid = 13, KillHigh = 14,
        ReverbFreeze = 15, ThrowReverb = 16, ThrowEcho = 17,
        // Settings (the project's, sent with it: the export hears the same).
        BrakeLength = 18, BrakeRestart = 19, BackspinLength = 20, ChopDivision = 21, HalfTimeLength = 22,
        ReleaseMode = 23, Grid = 24, RiserLength = 25, ReverseLength = 26,
        // Riser and Downlifter start on a rise of their value (a press); BuildUp 0..1; Drop: everything
        // back at rest on the next bar line; ResetAll: at once (« Tout remettre »).
        Riser = 27, Downlifter = 28, BuildUp = 29, Drop = 30, ResetAll = 31,
        kParams = 32
    };
    static constexpr int kReverbReturn = 6, kEchoReturn = 7;
    // How a held repeat, chopper, reverse or half time ends when let go (ReleaseMode * 4).
    enum Release { ReleaseCut = 0, ReleaseEcho = 1, ReleaseBrake = 2, ReleaseBackspin = 3 };
    static constexpr float kBufferSeconds = 4.0f;  // the input's last seconds (the heads read them)
    static constexpr float kLoopSeconds = 2.0f;    // the longest repeat slice (a beat at 30 BPM)

    PerformanceFx();
    void prepare(float rate);  // allocates (the ring, the slice, the echo)
    void setParam(int p, float v);
    float setting(int p) const { return p >= 0 && p < kParams ? settings_[static_cast<size_t>(p)] : 0.0f; }  // tests
    void setStepFrames(double frames) {
        if (frames > 0) stepFrames_ = frames;
        echo_.setStepFrames(stepFrames_);
    }
    // A transport runs (steps arrive): held effects wait for the grid. Stopped: they start at once,
    // and a Drop still waiting for its bar line happens now (nothing stays on while stopped).
    void setClock(bool running);
    // A sequencer step starts `offset` frames into the next process(): transport step `step` (-1:
    // the one after the last) of a bar of `stepsPerBar` steps.
    void onStep(int offset, int step, int stepsPerBar);
    void onStep(int offset) { onStep(offset, -1, stepsPerBar_); }
    void process(float* l, float* r, int n);
    bool active() const;
    // After a NaN reached the master: everything starts again from silence (no allocation; clearing
    // the seconds of memory is a one-off).
    void recover();
    // Cutoff and resonance from filter_ and resonance_ (no allocation).
    void applyFilter();

    // The grid held effects start on, in steps (0: at once).
    int gridSteps() const { return gridSteps_; }
    // How much of the whole mix the build-up sends into send bus `bus` (0 reverb, 1 echo).
    // (Its target: the mixer's send glides on its own, and only moves on a chunk's first frame.)
    float buildSend(int bus) const { return buildTarget_ * (bus == 0 ? 0.55f : 0.4f); }
    // Sixteenths of a repeat or chopper division value (1/4, 1/8T, 1/8, 1/16, 1/16T, 1/32).
    static double divisionSteps(float v);
    // Tests.
    bool repeating() const { return repeating_; }
    int loopLength() const { return loopLen_; }
    bool riserRunning() const { return riserRunning_; }
    float buildUp() const { return build_; }
    bool killOn(int band) const { return isolator_.kill(band); }
    float dryGain() const { return dry_; }

private:
    struct Hold {
        bool want = false;  // asked (a finger, a song curve)
        bool on = false;    // playing (started on a grid line)
        bool user = false;  // asked by the performer while it played (its release may end with a release effect)
    };
    float readRing(const std::vector<float>& buf, double frame) const;  // absolute frame, interpolated
    bool due(int grid) const { return !clock_ || grid <= 0 || (stepHere_ && step_ % grid == 0); }
    bool barLine() const { return stepHere_ && step_ % stepsPerBar_ == 0; }
    double sinceLine(double steps) const;
    double beatsToFrames(double beats, double maxSeconds) const;
    void onLine();
    void updateHolds();
    void startRepeat(double steps);
    void startBrake();
    void startSpin();
    void startEcho();
    void startRelease();
    void endRelease();
    void interruptRelease();
    void startRiser(bool down);
    void reset(bool hard);

    float rate_ = 48000;
    float fade_ = 0.002f;  // per-sample crossfade speed (about 10 ms)
    // The input's last seconds (absolute frames modulo the size); frame_: the frame just written.
    std::vector<float> ringL_, ringR_;
    int64_t frame_ = -1;
    // The clock.
    bool clock_ = false;
    double stepFrames_ = 6000.0;
    int stepsPerBar_ = 16;
    int stepAt_ = -1, pendingStep_ = -1;
    int64_t step_ = -1;
    bool stepHere_ = false;
    double sinceStep_ = 0, sinceEighth_ = 0, sinceBeat_ = 0, sinceBar_ = 0;
    int gridSteps_ = 0;
    std::array<float, kParams> settings_{};
    // DJ filter: 0.5 = off, below = low-pass sweeping down, above = high-pass sweeping up. It
    // always runs (its memory follows the music); what is heard crossfades in about 10 ms.
    float filter_ = 0.5f, resonance_ = 0.3f;
    bool filterOn_ = false;
    float filterMix_ = 0.0f;
    bool filterLow_ = true;  // the side being heard (it changes only while faded out)
    daisysp::Svf svfL_, svfR_;
    float filterGain_ = 1.0f;  // resonance compensation
    // Held effects.
    Hold repeat_, brake_, spin_, chop_, reverse_, half_, echoOut_;
    std::array<Hold, Isolator::kBands> kills_{};
    bool userRepeat_ = false;
    // Repeat: a slice copied from the ring, then filled live up to its end.
    std::vector<float> loopL_, loopR_;
    float division_ = 0.5f;
    double loopSteps_ = 0.0;
    int loopLen_ = 0, loopFilled_ = 0, loopPos_ = 0;
    bool repeating_ = false, loopWrapped_ = false;
    double pendingSteps_ = 0.0;  // another division asked while repeating: taken at the slice's end
    float repeatMix_ = 0.0f, fadeIn_ = 1.0f;
    // Tape heads reading the ring: the brake (with its restart) and the backspin.
    bool brakeRunning_ = false, brakeAuto_ = false, restarting_ = false, restartPending_ = false;
    double brakePos_ = 0, brakeT_ = 0, brakeLen_ = 1, restartT_ = 0, restartLen_ = 1, restartFrom_ = 0;
    float brakeMix_ = 0.0f;
    bool spinRunning_ = false, spinAuto_ = false;
    double spinPos_ = 0, spinT_ = 0, spinLen_ = 1;
    float spinMix_ = 0.0f;
    // Reverse and half time (they read the ring by the clock), chopper.
    int64_t reverseStart_ = 0, halfStart_ = 0, chopStart_ = 0;
    float reverseMix_ = 0.0f, halfMix_ = 0.0f;
    float chopGain_ = 1.0f;
    // Echo out: its own echo; the dry sound fades out over a beat while the echo takes it.
    TempoDelay echo_;
    bool echoAuto_ = false;
    float dry_ = 1.0f, send_ = 0.0f;
    int64_t echoAlive_ = 0;
    // A release effect playing: the dry sound comes back on this bar line (or after these frames).
    int64_t returnStep_ = -1;
    double returnFrames_ = -1.0;
    Isolator isolator_;
    // Riser / downlifter: white noise through a sweeping filter.
    bool riserWant_ = false, downWant_ = false, riserPending_ = false, pendingDown_ = false;
    bool riserRunning_ = false, riserDown_ = false, riserEnding_ = false;
    int64_t riserEndStep_ = -1;
    double riserT_ = 0, riserTotal_ = 1;
    float riserOut_ = 1.0f;  // the last millisecond's fade at its end
    daisysp::Svf riserL_, riserR_;
    uint32_t seedL_ = 1, seedR_ = 2;
    float riserGain_ = 0.0f;
    // Build-up (« Montée »): a rising high-pass, noise, sends and faster repeats.
    float buildTarget_ = 0.0f, build_ = 0.0f, buildCoef_ = 0.001f, buildMix_ = 0.0f;
    bool buildOn_ = false;
    // The drop: the build-up (high-pass and noise) fades out in 2 ms on the bar line, no click.
    float dropFade_ = 0.0f, dropFadeStep_ = 0.01f;
    daisysp::Svf buildL_, buildR_, buildNoiseL_, buildNoiseR_;
    uint32_t buildSeedL_ = 3, buildSeedR_ = 4;
    int buildCount_ = 0, riserCount_ = 0;
    bool dropWant_ = false, resetWant_ = false, dropPending_ = false;
};

}  // namespace ps
