#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <vector>

#include "Effects/chorus.h"
#include "Effects/overdrive.h"
#include "Filters/svf.h"

#include "DriveShaper.h"

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
    // 2nd order with a Q of its own (two in a row with Q 0.5412 and 1.3066: a 4th-order Butterworth).
    void highPassQ(float rate, float freq, float q);
    void lowPassQ(float rate, float freq, float q);
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

// Feed-forward compressor, stereo-linked, soft knee. Below the threshold it leaves the sound as
// it is (0 dB): the make-up gain is the user's, or « Auto » (the reduction it makes on average).
// Params: threshold (-40..0 dB), ratio (1..20), attack (0.1..80 ms), release (20..800 ms),
// make-up (0..+24 dB), auto make-up (on above 0.5), detector (peak below 0.5, else RMS over 10 ms),
// detector high-pass (0: none, else 20..400 Hz, 0.5 ≈ 90 Hz: the kick stops driving a bus), mix
// (0 dry .. 1 compressed: parallel compression).
class Compressor {
public:
    static constexpr int kParams = 9;
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    float gainReductionDb() const { return reduction_; }  // its deepest over the last block (≤ 0): tests, meters
    float autoMakeupDb() const { return autoMakeup_ ? -averageDb_ : 0.0f; }  // tests

private:
    void update();
    float rate_ = 48000;
    std::array<float, kParams> p_{0.6f, 0.2f, 0.2f, 0.3f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    float threshold_ = -16, ratio_ = 4, attack_ = 0, release_ = 0, makeup_ = 1, mix_ = 1;
    bool autoMakeup_ = false, rms_ = false, highPass_ = false;
    float rmsCoef_ = 0, averageCoef_ = 0;
    float envDb_ = -120.0f;
    float meanSquare_ = 0.0f;
    float averageDb_ = 0.0f;  // the reduction made, averaged over seconds (auto make-up)
    float reduction_ = 0.0f;
    Biquad detL_, detR_;
};

// Drive (DaisySP Overdrive), tone low-pass (open at its top: no filter), dry/wet, output level,
// and compensation (on above 0.5; off by default, as before): the drive no longer changes the
// level of a sound at a usual level (-15 dBFS RMS), nor of quieter ones while the drive is light.
// A tone saved at its top before phase 33 stays an 18 kHz low-pass (Effects.kt).
class Saturator {
public:
    static constexpr int kParams = 5;
    static constexpr float kOpenTone = 1.0f;  // the tone at its top: no low-pass (Effects.kt TONE_OPEN)
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() {
        lpL_ = lpR_ = 0.0f;
        shL_.reset();
        shR_.reset();
        setDrive(driveTarget_);
    }
    void process(float* l, float* r, int n);

private:
    void update();
    void setDrive(float drive);
    float rate_ = 48000;
    std::array<float, kParams> p_{0.3f, 0.7f, 1.0f, 0.7f, 0.0f};
    // DaisySP Overdrive's curve and gains, run at 4x with antiderivative anti-aliasing
    // (DriveShaper.h): the dry signal is DriveShaper::kDelay frames late with it. The drive glides
    // (its two gains move together: the level holds).
    DriveShaper<Oversampler4x> shL_, shR_;
    float driveTarget_ = 0.0f, drive_ = 0.0f, pre_ = 1.0f, post_ = 1.0f, glide_ = 0.01f;
    float toneCoef_ = 1.0f, mix_ = 1.0f, level_ = 1.0f, wetGain_ = 1.0f;
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

// Phase 34 (S5): a sidechain keyed by a sound's audio (the kick's, wherever it plays: Mixer's
// key). The key's envelope (instant attack, `release` back down) over its own recent peak (held,
// falling over about 4 s: a soft kick ducks less, the depth does not depend on the kick's level)
// sets how far the sound goes down: while the key is within 6 dB of that peak (a kick's body), by
// `depth` of its level (1 - depth), less as it fades; its lows
// (under ~150 Hz) by `lowDepth` (one duck for a whole bus: the bass cleared deeper than the pads
// pump). The gain glides down in about 1 ms and up in 10 ms (no click). Without a key the sound
// comes out as it went in. Real-time safe; prepare() sets everything.
class KeyedDucker {
public:
    static constexpr float kLowHz = 150.0f;
    void prepare(float rate);
    void reset();
    // depth, lowDepth 0..1 (1 = silent at a full key); release in ms (envelope back down).
    void set(float depth, float lowDepth, float releaseMs);
    // `key`: n frames of the key, mono (null: silence).
    void process(float* l, float* r, const float* key, int n);
    float gain() const { return gHi_; }     // tests
    float lowGain() const { return gLo_; }  // tests
    bool active() const { return split_ || depth_ > 0.0f || lowDepth_ > 0.0f || gHi_ != 1.0f || gLo_ != 1.0f; }

private:
    float rate_ = 48000.0f;
    float depth_ = 0.0f, lowDepth_ = 0.0f;
    float release_ = 0.999f, refFall_ = 0.99999f, down_ = 0.02f, up_ = 0.002f;
    float env_ = 0.0f, ref_ = 0.0f, gHi_ = 1.0f, gLo_ = 1.0f;
    // A low depth of its own: the sound split at kLowHz (Linkwitz-Riley, 24 dB / octave: the bands
    // add back up to the sound, all-passed), each band its gain.
    std::array<Biquad, 2> lpL_, lpR_, hpL_, hpR_;
    // Split for good (until reset) once the depths differed: going back to the direct path would
    // jump in phase (review m2). Entering it, the split fades in over xfadeLen_ frames.
    bool split_ = false;
    int xfade_ = 0, xfadeLen_ = 480;
};

// Sidechain "pump": the signal ducks on every kick (or every beat) and swells back.
// Params: depth (0..1), release (50..600 ms), source (< 0.5 kick hits, else every beat), then
// (S5) key (>= 0.5: the kick's audio, the mixer's key, instead of the kick lane's steps: a
// KeyedDucker; the source setting is then not used).
class Ducker {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset() {
        env_ = 0.0f;
        pending_ = -1;
        keyed_.reset();
    }
    // A kick lands `offset` frames into the next process() call.
    void trigger(int offset) { pending_ = offset; }
    bool everyBeat() const { return p_[2] >= 0.5f; }
    bool audioKey() const { return p_[3] >= 0.5f; }
    // `key` (audio key mode only): n frames of the mixer's key, mono (null: none).
    void process(float* l, float* r, int n, const float* key = nullptr);
    float gain() const { return audioKey() ? keyed_.gain() : 1.0f - depth_ * env_; }  // tests

private:
    void update();
    float rate_ = 48000;
    std::array<float, 4> p_{0.7f, 0.4f, 0.0f, 0.0f};
    float depth_ = 0.7f, release_ = 0.999f, attack_ = 0.1f, env_ = 0.0f, target_ = 0.0f;
    int pending_ = -1;
    KeyedDucker keyed_;
};

// Phase 34 (S5): a soft clipper that follows the sound's level: what goes further than `margin`
// over its running level (RMS over ~200 ms, 3..15 dB) is rounded off toward that ceiling (straight
// up to `knee` of it, then a tanh curve), at 4x with antiderivative anti-aliasing (DriveShaper):
// the peaks of a bass come down before the master's limiter, which then takes less. Params:
// margin (0..1: 3..15 dB), softness (0..1: the knee from 95 % to 20 % of the ceiling), mix.
// The output is DriveShaper::kDelay frames late (the dry sound too).
class SoftClipper {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    float lastCeiling() const { return ceiling_; }  // tests: linear
    static float shape(float knee, float u);
    static double antiderivative(float knee, double u);

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.5f, 0.5f, 1.0f, 0.0f};
    // The running level is a bias-corrected mean: ms_ over the weight w_ it has gathered. In silence
    // the weight fades with the energy, so a sound after a blank is measured on its own first
    // samples instead of against the silence (review m1: its attack was clipped hard).
    float ms_ = 0.0f, w_ = 0.0f, msCoef_ = 0.0f, ceiling_ = 1.0f;
    DriveShaper<Oversampler4x> shL_, shR_;
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
    void restartLfo() { phase_ = 0.0; }

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
    void restartLfo() { phase_ = 0.0; }

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
    void restartLfo() { phase_ = 0.0; }

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.4f, 0.6f, 0.0f, 0.0f};
    double phase_ = 0.0;
};

// Stereo widener (mid/side). Params: width (0 mono .. 0.5 as is .. 1 twice as wide), bass in mono below
// (0 off, else 40..300 Hz), spread (S7 son: 0 off, as always .. 1), -.
// The spread makes a side out of the sound itself, so a mono pad or lead gets wide: its centre
// high-passed at 250 Hz (two poles: the lows stay mono) through two short delays that sway slowly
// (7-13 ms and 13-19 ms, 0.23 and 0.31 Hz: a stereo chorus), their difference added to the side.
// L + R stays what it was (mono-compatible: a phone's mono speaker hears no comb). Its delay line is
// preallocated (no allocation in process); at 0 the effect is bit for bit what it always was.
class Widener {
public:
    void prepare(float rate) { rate_ = rate; reset(); }
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    static constexpr int kSpreadLen = 2048;  // 42 ms at 48 kHz: the longest tap (19 ms) at up to 96 kHz

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.75f, 0.0f, 0.0f, 0.0f};
    float lowS_ = 0.0f;
    std::array<float, kSpreadLen> line_{};
    int write_ = 0;
    float hp1_ = 0.0f, hp2_ = 0.0f;  // the spread's high-pass (two one-pole low-passes taken off)
    double lfo1_ = 0.0, lfo2_ = 0.25;
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
    void restartLfo() { phase_ = 0.0; lfo_ = 0.0; }

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
    void reset() { env_ = 0.0f; s1L_ = s2L_ = s1R_ = s2R_ = 0.0f; tick_ = 0; }
    void process(float* l, float* r, int n);
    float cutoff() const { return cutoff_; }  // tests

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.6f, 0.3f, 0.6f, 1.0f};
    float env_ = 0.0f, cutoff_ = 0.0f;
    float s1L_ = 0.0f, s2L_ = 0.0f, s1R_ = 0.0f, s2R_ = 0.0f;  // TPT state-variable filter
    float g_ = 0.0f, a1_ = 1.0f, a2_ = 0.0f;                    // its coefficients (every 8 frames)
    int tick_ = 0;  // frames to the next update, across calls (the same frames whatever the block size)
};

// Old record ("Vieux vinyle"): wow and flutter (a slowly moving short delay), crackles, hiss and
// a dull, narrow tone. Params: wow, crackle, age (tone and hiss), mix.
class Vinyl {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    // The wow and flutter from their start (the knob's glide done), the crackles from their first draw.
    void restartLfo() {
        wow_ = flutter_ = 0.0;
        wowNow_ = p_[0];
        seed_ = 0x9E3779B9u;
    }

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
    void reset() {
        lp1L_ = lp2L_ = lp1R_ = lp2R_ = dcInL_ = dcOutL_ = dcInR_ = dcOutR_ = 0.0f;
        shL_.reset();
        shR_.reset();
        drive_ = -1.0f;  // the next block starts at its drive (no glide from a stale one)
    }
    void process(float* l, float* r, int n);
    static float shape(int type, float x);  // tests
    static double antiderivative(int type, double x);  // of shape(): DriveShaper's anti-aliasing

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.45f, 0.6f, 0.0f, 1.0f};
    float lp1L_ = 0, lp2L_ = 0, lp1R_ = 0, lp2R_ = 0;
    float dcInL_ = 0, dcOutL_ = 0, dcInR_ = 0, dcOutR_ = 0;
    // The curve at 4x with antiderivative anti-aliasing; the drive glides (no click on a turn).
    DriveShaper<Oversampler4x> shL_, shR_;
    float drive_ = -1.0f;
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
    void restartLfo() { phase_ = 0.0; }
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

// « Basse audible » (phase 40, docs/hits/basses-qui-tapent.md): a psychoacoustic bass exciter,
// the principle of Waves' MaxxBass. A phone's speaker plays almost nothing under 200-300 Hz, where
// the basses put their weight; the ear hears a low note that is not there when three or more of its
// harmonics are (the missing fundamental). The sound's low band (mono: L + R, a 4th-order
// Butterworth low-pass at the frequency) is divided by its own envelope (a peak follower, 2 ms up,
// 60 ms down) into a wave of steady size, shaped by tanh (odd harmonics) and tanh² (even ones; the
// colour mixes them), band-passed (a 4th-order high-pass at 1.25 x the frequency, at least 100 Hz:
// the low band's own fundamental out; a 2nd-order low-pass at the top), multiplied by the envelope
// again (the harmonics hit and fade with the note) and added to both sides, the dry sound left as it
// is. The shapes are smooth (their harmonics fall exponentially) and the low band is under a few
// hundred hertz: nothing folds back at 48 kHz (host test: inharmonic content under -60 dB), no
// oversampling, no latency. Params: amount (0..1), frequency (60..200 Hz, 0.5 = 110 Hz), colour
// (0 odd .. 1 even, 0.7 by default), top (250..1200 Hz, 0.55 = 600 Hz). The amount glides (10 ms).
class BassExciter {
public:
    static constexpr int kParams = 4;
    // Harmonics at amount 1 against the low band's envelope (tuned on the bench: an 808 at 0.5 goes
    // from -22 to about -13 dB through a 300 Hz phone model, for about +1 LU on its own full band).
    static constexpr float kGain = 3.5f;
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
    void process(float* l, float* r, int n);
    static float frequencyHz(float v);
    static float topHz(float v);

private:
    // The filters set for the band at `freqHz` and the harmonics' top at `topHz`.
    void update(float freqHz, float topHz);
    // Glides the band and the top towards their settings (every kStep samples, in the audio thread).
    void glideFilters();
    static constexpr int kStep = 16;
    float rate_ = 48000;
    std::array<float, kParams> p_{0.35f, 0.5f, 0.7f, 0.55f};
    Biquad lowA_, lowB_, hpA_, hpB_, top_;
    float env_ = 0.0f, attack_ = 0.0f, release_ = 0.0f;
    float amount_ = 0.0f, amountGlide_ = 0.002f;
    // « Fréquence » and « Haut » as the filters play them: a knob turned or a curve on them glides
    // (about 20 ms) instead of jumping the filters' coefficients (a click on a loud bass).
    float freqNow_ = 0.0f, topNow_ = 0.0f, filterGlide_ = 0.5f;
    int countdown_ = 0;
    bool fresh_ = true;  // the amount and the filters start at their settings (no glide after a reset)
};

class InsertSlot {
public:
    enum Type {
        None = 0, Eq = 1, Comp = 2, Saturation = 3, Chorus = 4, Filter = 5, Pump = 6,
        FlangerFx = 7, PhaserFx = 8, GateFx = 9, TremoloFx = 10, WidenerFx = 11, Crusher = 12, ParamEqFx = 13,
        RobotFx = 14, WahFx = 15, VinylFx = 16, RadioFx = 17, ChopperFx = 18,
        DistortionFx = 19, EchoFx = 20, PingPongFx = 21, ReverbFx = 22, AutoPanFx = 23, OttFx = 24,
        FreqShiftFx = 25, TapeStopFx = 26, WobbleFx = 27, ClipperFx = 28, BassExciterFx = 29, kTypes = 30
    };
    // Whether `type` needs a unit of the pool (seconds of memory).
    static bool isLong(int type) { return type == EchoFx || type == PingPongFx || type == ReverbFx || type == TapeStopFx; }
    // Settings per effect (most use 4; the compressor 9, the saturation 5).
    static constexpr int kParams = 12;

    void prepare(float rate);
    void setType(int type);
    void setParam(int p, float v);
    int type() const { return type_; }
    void process(float* l, float* r, int n);
    void reset();  // the effect's state back to silence (after a NaN, back in a scene), no allocation
    // The transport starts: free-running LFOs (flanger, phaser, tremolo, robot, vinyl, frequency
    // shifter) from their start, as in an export, which starts with them there. (The chorus is
    // DaisySP's, whose LFO cannot be restarted without clearing its delay line.)
    void restartLfo();
    const Compressor& compressor() const { return comp_; }  // tests
    const Ducker& ducker() const { return pump_; }          // tests
    // Sidechain events (only the Pump type listens). `beat`: the step is a quarter note.
    void setStepFrames(double framesPerSixteenth);
    // `step`: the transport step it is (-1: unknown), for the wobble's phase.
    void sidechain(int offset, bool kick, bool beat, int step = -1) {
        if (type_ == Pump && !pump_.audioKey() && (pump_.everyBeat() ? beat : kick)) pump_.trigger(offset);
        if (type_ == ChopperFx && beat) chopper_.beat(offset);
        if (type_ == AutoPanFx && beat) autoPan_.beat(offset);
        if (type_ == WobbleFx && beat) wobble_.beat(offset, step);
    }
    const Wobble& wobble() const { return wobble_; }  // tests
    const SoftClipper& clipper() const { return clipper_; }  // tests
    // S5: the mixer's sidechain key for the next process() call (n frames, mono; null: none). Only
    // a Pump on the audio key listens.
    void setKey(const float* key) { key_ = key; }
    bool listensToKey() const { return type_ == Pump && pump_.audioKey(); }
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
    SoftClipper clipper_;
    BassExciter exciter_;
    const float* key_ = nullptr;
};

// Freeverb (Jezar at Dreampoint, public domain): 8 parallel combs + 4 allpasses per side.
// 100 % wet (it sits on a send bus). Params: size, damping, width, pre-delay (0..200 ms).
class PlateReverb;

// The send reverb (and the insert one's engine). Params: size, damping, width, pre-delay, then (S4)
// the type: 0 the hall (Freeverb, as always), 0.5 the plate (PlateReverb, on the send bus only:
// attachPlate), 1 the room (a shorter, darker Freeverb with early reflections).
class Reverb {
public:
    static constexpr int kParams = 5;
    enum Type { Hall = 0, Plate = 1, Room = 2 };
    void prepare(float rate);
    void attachPlate(PlateReverb* plate) {
        plate_ = plate;
        update();
    }
    int type() const { return type_; }
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
    void plateSet(float size, float damping, float freeze = 0.0f);
    std::array<float, kParams> p_{0.6f, 0.5f, 1.0f, 0.1f, 0.0f};
    int type_ = Hall;
    PlateReverb* plate_ = nullptr;
    std::array<float, 256> plateIn_{};  // the plate's (pre-delayed) mono input, a chunk at a time
    std::array<Comb, 8> combL_, combR_;
    std::array<Allpass, 4> apL_, apR_;
    std::vector<float> pre_;
    size_t preIdx_ = 0, preLen_ = 1;
    float feedback_ = 0.84f, damp1_ = 0.2f, damp2_ = 0.8f, wet1_ = 1.0f, wet2_ = 0.0f;
    float rate_ = 48000;
    bool freeze_ = false;
    float freezeMix_ = 0.0f, freezeStep_ = 1.0f / 480.0f;
    // A change of type (hall, plate, room) crossfades over ~20 ms (review m3): the plate's share
    // and the room's early reflections' share glide to their new values; at rest each type plays
    // alone, as before.
    float plateW_ = 0.0f, roomW_ = 0.0f, typeStep_ = 1.0f / 960.0f;
    // The tank's feedback and damping as played, gliding to feedback_ and damp1_ (a type or size
    // change written at once into the combs would come back as a step a comb's length later).
    float feedbackNow_ = 0.84f, damp1Now_ = 0.2f, glide_ = 0.001f;
    bool weightsSet_ = false;
    void processBlend(const float* inL, const float* inR, float* outL, float* outR, int n, float plateTarget, float roomTarget);
    // One frame of the Freeverb tank (and the room's taps, by `room`) from the pre-delayed input `in`
    // (the sum of both sides, unscaled), added into l, r before the width.
    void tankFrame(float in, float room, float& l, float& r);
};

// Tempo-synced stereo delay with ping-pong, feedback tone and smooth time changes.
// 100 % wet. Params: time (1/16, 1/8, 3/16, 1/4, 3/8, 1/2 note), feedback, ping-pong, tone.
class TempoDelay {
public:
    // The longest time it plays: a half note at 40 BPM, the slowest tempo (8 sixteenths of 0.375 s
    // = 3 s), with a margin for the read's interpolation.
    static constexpr float kMaxSeconds = 3.05f;
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
    // Every note value at every tempo: a half note at 40 BPM is 3 s (the send delay's length).
    static constexpr float kMaxSeconds = TempoDelay::kMaxSeconds;
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
    // Shared by every slot of the mixer (32 strips x 8 slots): about 14 MB of echoes (1.17 MB
    // each: 3.05 s, a half note at 40 BPM), 1.8 MB of reverbs (0.15 MB) and 2.5 MB of tape stops
    // (0.41 MB) at 48 kHz, 18.4 MB in all per engine (the export's engine holds the same while it renders).
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
    // The sounds it makes on its own (the riser, the downlifter, the build-up's noise): they come
    // from no track, so a track-by-track export plays them in one file of their own.
    enum Noises { NoisesMixed = 0, NoisesOff = 1, NoisesOnly = 2 };
    void setNoises(int mode) { noises_ = mode == NoisesOff || mode == NoisesOnly ? mode : NoisesMixed; }
    int noises() const { return noises_; }
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
    int noises_ = NoisesMixed;
};

}  // namespace ps
