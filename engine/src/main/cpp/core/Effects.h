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
    void reset() {}
    void process(float* l, float* r, int n);

private:
    void update();
    std::array<float, 4> p_{0.3f, 0.5f, 0.4f, 0.5f};
    daisysp::Chorus chorus_;
    float mix_ = 0.5f;
};

// DaisySP state-variable filter. Params: cutoff (40 Hz..18 kHz), resonance, type (low/band/high), mix.
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
};

// One insert slot: any of the insert effects, chosen by type. All are kept prepared so a
// type change on the audio thread is just a reset.
class InsertSlot {
public:
    enum Type { None = 0, Eq = 1, Comp = 2, Saturation = 3, Chorus = 4, Filter = 5, kTypes = 6 };
    static constexpr int kParams = 4;

    void prepare(float rate);
    void setType(int type);
    void setParam(int p, float v);
    int type() const { return type_; }
    void process(float* l, float* r, int n);
    const Compressor& compressor() const { return comp_; }  // tests

private:
    int type_ = None;
    Eq3 eq_;
    Compressor comp_;
    Saturator sat_;
    StereoChorus chorus_;
    SweepFilter filter_;
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
