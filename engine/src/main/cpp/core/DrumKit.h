#pragma once

#include <array>
#include <cstdint>

#include "Drums/analogbassdrum.h"
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
    // Schedules a hit `frameOffset` frames into the next render() call.
    void trigger(int voice, float velocity, int frameOffset);
    // Adds all voices into a mono buffer.
    void render(float* mono, int numFrames);

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

    float renderVoice(int voice, bool trig, float velocity);
    float noise();

    int sampleRate_ = 48000;
    uint32_t noiseState_ = 22222u;
    std::array<int, kVoices> pending_{};
    std::array<float, kVoices> velocity_{};
    std::array<int, kVoices> activeFrames_{};
    float openHatChoke_ = 1.0f;
    float chokeDecay_ = 0.99f;

    daisysp::AnalogBassDrum kick_, lowTom_, midTom_, highTom_, conga_;
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
