#pragma once

#include <array>
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

// Live performance effects on the master, played from the XY pad and hold buttons:
// DJ filter (bipolar low/high-pass), beat repeat (loops the last 1/4..1/32 note, tempo-synced),
// tape stop (slows to a halt) and a sixteenth-note chopper. Stereo, in place.
class PerformanceFx {
public:
    enum Param { Filter = 0, Resonance = 1, Repeat = 2, RepeatDivision = 3, TapeStop = 4, Chop = 5, kParams = 6 };
    static constexpr float kBufferSeconds = 4.0f;

    void prepare(float rate);
    void setParam(int p, float v);
    void setStepFrames(double frames) { stepFrames_ = frames > 0 ? frames : stepFrames_; }
    // A sequencer step starts `offset` frames into the next process() (keeps the chopper on the grid).
    void onStep(int offset) { stepOffset_ = offset; }
    void process(float* l, float* r, int n);
    bool active() const { return filterOn_ || repeatOn_ || stopOn_ || stopping_ || chopOn_ || repeating_; }
    // After a NaN reached the master: the filter and the recorded seconds start again from silence
    // (no allocation; clearing the 4 s of memory is a one-off).
    void recover();
    // Cutoff and resonance from filter_ and resonance_ (no allocation).
    void applyFilter();

private:
    float readRing(const std::vector<float>& buf, double pos) const;
    float rate_ = 48000;
    std::vector<float> ringL_, ringR_;
    size_t write_ = 0;
    double stepFrames_ = 6000.0;
    // Filter: 0.5 = off, below = low-pass sweeping down, above = high-pass sweeping up.
    float filter_ = 0.5f, resonance_ = 0.3f;
    bool filterOn_ = false;
    daisysp::Svf svfL_, svfR_;
    float filterGain_ = 1.0f;  // resonance compensation
    // Repeat.
    bool repeatOn_ = false, repeating_ = false;
    float division_ = 0.5f;
    double loopStart_ = 0, loopLen_ = 0, loopPos_ = 0;
    float repeatMix_ = 0.0f;
    // Tape stop.
    bool stopOn_ = false, stopping_ = false;
    double speed_ = 1.0, readPos_ = 0;
    float stopMix_ = 0.0f;
    // Chopper.
    bool chopOn_ = false;
    double chopPhase_ = 0;
    float chopGain_ = 1.0f;
    int stepOffset_ = -1;
    float fade_ = 0.002f;  // per-sample crossfade speed (about 10 ms)
};

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
    // A beat lands `offset` frames into the next process() call.
    void beat(int offset) { pending_ = offset; }
    void process(float* l, float* r, int n);
    float gain() const { return gain_; }  // tests

private:
    float rate_ = 48000;
    std::array<float, 4> p_{0.0f, 1.0f, 0.2f, 0.0f};
    float gain_ = 1.0f;
    int64_t sinceBeat_ = 0, beatFrames_ = 24000;  // 120 BPM until the song tells
    int pending_ = -1;
};

class InsertSlot {
public:
    enum Type {
        None = 0, Eq = 1, Comp = 2, Saturation = 3, Chorus = 4, Filter = 5, Pump = 6,
        FlangerFx = 7, PhaserFx = 8, GateFx = 9, TremoloFx = 10, WidenerFx = 11, Crusher = 12, ParamEqFx = 13,
        RobotFx = 14, WahFx = 15, VinylFx = 16, RadioFx = 17, ChopperFx = 18, kTypes = 19
    };
    static constexpr int kParams = 4;

    void prepare(float rate);
    void setType(int type);
    void setParam(int p, float v);
    int type() const { return type_; }
    void process(float* l, float* r, int n);
    void reset();  // every effect's state back to silence (after a NaN), no allocation
    const Compressor& compressor() const { return comp_; }  // tests
    const Ducker& ducker() const { return pump_; }          // tests
    // Sidechain events (only the Pump type listens). `beat`: the step is a quarter note.
    void sidechain(int offset, bool kick, bool beat) {
        if (type_ == Pump && (pump_.everyBeat() ? beat : kick)) pump_.trigger(offset);
        if (type_ == ChopperFx && beat) chopper_.beat(offset);
    }

private:
    int type_ = None;
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
};

// Freeverb (Jezar at Dreampoint, public domain): 8 parallel combs + 4 allpasses per side.
// 100 % wet (it sits on a send bus). Params: size, damping, width, pre-delay (0..200 ms).
class Reverb {
public:
    void prepare(float rate);
    void setParam(int p, float v);
    void reset();
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
};

// Tempo-synced stereo delay with ping-pong, feedback tone and smooth time changes.
// 100 % wet. Params: time (1/16, 1/8, 3/16, 1/4, 3/8, 1/2 note), feedback, ping-pong, tone.
class TempoDelay {
public:
    static constexpr float kMaxSeconds = 2.5f;
    void prepare(float rate);
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
    double stepFrames_ = 6000.0;
    float target_ = 12000.0f, current_ = 12000.0f, smooth_ = 0.001f;
    float feedback_ = 0.4f, pingPong_ = 1.0f, toneCoef_ = 0.5f, lpL_ = 0, lpR_ = 0;
    float rate_ = 48000;
};

}  // namespace ps
