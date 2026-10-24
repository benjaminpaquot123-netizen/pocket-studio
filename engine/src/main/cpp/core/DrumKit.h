#pragma once

#include <array>
#include <cstdint>

#include "Drums/analogsnaredrum.h"
#include "Drums/hihat.h"
#include "Drums/synthbassdrum.h"
#include "Drums/synthsnaredrum.h"
#include "Filters/svf.h"
#include "StudioVoices.h"
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

    // What a pad plays instead of its own voice (phase 34, S3): the studio voices of StudioVoices.h.
    // Order is part of the JNI contract (NativeEngine.PAD_VOICE_*): append only.
    enum Kind : int {
        KitVoice = 0,  // the pad's own voice of the kit (by version)
        Bass808,       // the tuned 808 bass (pitched by the step's pitch, glides between hits)
        KickStudio,    // the short 909-style kick (tune 45..65 Hz, sub and click layers)
        KickHard,      // its hard / gabber variant (soft clip, deeper sweep)
        RumbleVoice,   // the filtered low tail (30..150 Hz) techno puts under the kick
        kKinds
    };
    // The studio voices' settings (setVoiceParam). Order is part of the JNI contract: append only.
    enum VoiceParam : int {
        Tune,   // 808: MIDI note 12..72 (36 = 65.4 Hz, 33 = 55 Hz); kicks and rumble: Hz 30..120
        Decay,  // seconds to -40 dB (808: up to 2.5, kicks: up to 1.2, rumble: up to 2)
        Drive,  // 0..1 saturation
        Click,  // 0..1 the attack layer
        Glide,  // seconds 0..0.5: the 808 slides to a new pitched hit over this time (0: no glide)
        Sub,    // 0..1 the kicks' sub layer
        Tone,   // 0..1: the 808's octave and twelfth layer (heard on a phone); rumble low-pass 60 -> 240 Hz
        kVoiceParams
    };

    void prepare(int sampleRate);
    // The voices' levels: 1, the kit as it always was (the projects saved before phase 33); 2,
    // « kit v2 » (the assistant's songs since): each voice near the loudness of its role in a mix
    // (the kick 6 dB down, the hats bright and 17 dB up: WizardMix.KIT_LUFS_V2); 3, « kit v3 »
    // (phase 34): the kick is the short studio kick, the second kick its hard variant, the hats
    // get a bright sizzle layer and the clap four bursts with air. From the next hits on.
    void setVersion(int version) {
        version_ = std::clamp(version, 1, 3);
        applyVersion();
    }
    int version() const { return version_; }
    // Real-time safe: the pad plays `kind` from its next hit on (its settings kept).
    void setKind(int voice, int kind);
    int kind(int voice) const { return voice >= 0 && voice < kVoices ? kind_[static_cast<size_t>(voice)] : 0; }
    void setVoiceParam(int voice, int param, float value);
    float voiceParam(int voice, int param) const;
    // Schedules a hit `frameOffset` frames into the next render() call, panned -1..1. `pitch`:
    // semitones added to a pitched voice's tune (the 808) for this hit; `glideFrames` > 0: the 808
    // slides from the pitch it is playing to this one over that many frames (a legato hit).
    void trigger(int voice, float velocity, int frameOffset, float pan = 0.0f, float pitch = 0.0f, int glideFrames = 0);
    // The 808 of `voice` is still sounding (tests).
    bool bassSounding(int voice) const { return pro_[static_cast<size_t>(voice)].bass.sounding(); }
    float bassHz(int voice) const { return pro_[static_cast<size_t>(voice)].bass.hz; }  // tests
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
    float renderStudio(int voice, int kind, bool trig, float velocity);
    void applyVersion();  // the voices' settings that differ in kit v2 and v3 (real-time safe)
    // Calls sink(voice, frame, value, gainLeft, gainRight) for every sample a voice adds.
    template <typename Sink> void renderTo(int numFrames, Sink&& sink);

    int sampleRate_ = 48000;
    int version_ = 1;
    // Noise: each voice its own generators (ours, and DaisySP's rand() swapped in while it
    // renders), so what one voice draws never depends on the others nor on the block size.
    float noise(int voice);
    void warmUp(int voice);
    std::array<uint32_t, kVoices> noiseState_{};
    std::array<uint32_t, kVoices> randState_{};
    std::array<int, kVoices> pending_{};
    std::array<float, kVoices> velocity_{};
    std::array<float, kVoices> pendingPan_{};
    std::array<float, kVoices> pendingPitch_{};
    std::array<int, kVoices> pendingGlide_{};
    // The studio voices, one set per pad (any pad may play any of them; about 300 bytes each).
    struct Pro {
        studio::Settings settings;
        studio::Bass808 bass;
        studio::StudioKick kick;
        studio::Rumble rumble;
    };
    std::array<Pro, kVoices> pro_{};
    std::array<int, kVoices> kind_{};
    // Kit v3's extra layers: the hats' sizzle (bright noise) and the clap's air.
    studio::Svf2 sizzleClosedHp_, sizzleOpenHp_, clapAirHp_;
    float sizzleClosed_ = 0.0f, sizzleOpen_ = 0.0f, clapAir_ = 0.0f;
    float sizzleClosedCoef_ = 0.99f, sizzleOpenCoef_ = 0.999f, clapAirCoef_ = 0.99f;
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
