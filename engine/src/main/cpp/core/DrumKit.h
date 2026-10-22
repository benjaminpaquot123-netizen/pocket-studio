#pragma once

#include <array>
#include <cstdint>

#include "Drums/analogsnaredrum.h"
#include "Drums/hihat.h"
#include "Drums/synthbassdrum.h"
#include "Drums/synthsnaredrum.h"
#include "Filters/svf.h"
#include "Synthesis/oscillator.h"

namespace ps {

// 16 synthesized drum voices (808/909 flavour), no samples. Voices from DaisySP (MIT)
// where it has them; clap, rim, cowbell and shaker are built here from noise and filters.
// trigger() and render() are real-time safe.
class DrumKit {
public:
    static constexpr int kVoices = 16;
    enum Voice : int {
        Kick = 0, Kick2, Snare, Snare2, Clap, ClosedHat, OpenHat, LowTom,
        MidTom, HighTom, Rim, Cowbell, Cymbal, Shaker, Conga, Zap,
    };

    void prepare(int sampleRate);
    // Schedules a hit `frameOffset` frames into the next render() call, panned -1..1.
    void trigger(int voice, float velocity, int frameOffset, float pan = 0.0f);
    // Every voice's noise from its first value again (the transport starts: an export, which
    // starts there too, gets the same noise as the take).
    void resetNoise();
    // Adds all voices into a mono buffer (pans ignored).
    void render(float* mono, int numFrames);
    // Adds all voices into a stereo pair, each voice at the pan of its last hit.
    void render(float* left, float* right, int numFrames);
    // Each voice into its own pair (lefts[voice], rights[voice]; several voices may share one).
    void render(float* const* lefts, float* const* rights, int numFrames);

private:
    struct Envelope {
        float level = 0.0f;
        float decay = 0.999f;
        void hit(float l) { level = l; }
        float next() {
            const float v = level;
            level *= decay;
            return v;
        }
    };

    // 808-style pitched drum: sine with a fast downward pitch sweep, amplitude decay and a
    // short attack click. Used for the main kick, toms and conga (DaisySP's AnalogBassDrum
    // only produced a click without body).
    struct PitchedDrum {
        float baseHz = 50.0f;
        float sweep = 3.0f;         // start frequency = baseHz * sweep
        float pitchDecay = 0.999f;  // per-sample decay of the sweep
        float ampDecay = 0.9999f;
        float click = 0.3f;
        float phase = 0.0f;
        float pitchEnv = 0.0f;
        float amp = 0.0f;
        float clickEnv = 0.0f;
        void configure(int sampleRate, float hz, float sweepRatio, float sweepSeconds, float decaySeconds, float clickAmount);
        void hit(float velocity);
        float process(int sampleRate);
    };

    float renderVoice(int voice, bool trig, float velocity);
    // Calls sink(voice, frame, value, gainLeft, gainRight) for every sample a voice adds.
    template <typename Sink> void renderTo(int numFrames, Sink&& sink);

    int sampleRate_ = 48000;
    // Noise: each voice its own generators (ours, and DaisySP's rand() swapped in while it
    // renders), so what one voice draws never depends on the others nor on the block size.
    float noise(int voice);
    void warmUp(int voice);
    std::array<uint32_t, kVoices> noiseState_{};
    std::array<uint32_t, kVoices> randState_{};
    std::array<int, kVoices> pending_{};
    std::array<float, kVoices> velocity_{};
    std::array<float, kVoices> pendingPan_{};
    std::array<float, kVoices> gainL_ = filled(1.0f), gainR_ = filled(1.0f);
    static constexpr std::array<float, kVoices> filled(float v) {
        std::array<float, kVoices> a{};
        for (auto& x : a) x = v;
        return a;
    }
    std::array<int, kVoices> activeFrames_{};
    std::array<float, kVoices> hitGain_{};  // the sounding hit's level (its velocity's)
    float openHatChoke_ = 1.0f;
    float chokeDecay_ = 0.99f;

    PitchedDrum kick_, lowTom_, midTom_, highTom_, conga_;
    daisysp::SyntheticBassDrum kick2_, zap_;
    daisysp::AnalogSnareDrum snare_;
    daisysp::SyntheticSnareDrum snare2_;
    daisysp::HiHat<> closedHat_, openHat_;
    daisysp::HiHat<daisysp::RingModNoise> cymbal_;

    daisysp::Svf clapFilter_, rimFilter_, cowbellFilter_, shakerFilter_;
    daisysp::Oscillator cowbellA_, cowbellB_;
    Envelope clapBurst_, clapTail_, rimEnv_, cowbellFast_, cowbellSlow_, shakerEnv_;
    int clapCounter_ = -1;
};

}  // namespace ps
