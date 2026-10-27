#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>

#include "Control/adsr.h"
#include "DriveShaper.h"
#include "Filters/ladder.h"
#include "Filters/svf.h"
#include "Synthesis/oscillator.h"
#include "hiir/Downsampler2xFpu.h"
#include "hiir/coef/C2x.h"

namespace ps {

// 8-voice subtractive synthesizer: 2 oscillators + sub + noise -> ladder or state-variable
// filter, filter and amp ADSR, 2 LFOs (vibrato, filter sweep). Building blocks from DaisySP.
// All voices are preallocated: noteOn/noteOff/setParam/render are real-time safe.
class Synth {
public:
    static constexpr int kVoices = 8;
    /** Decibels of the Gain parameter at 0 and 1 (0.5 = unity), shared with PlaitsSynth and the model. */
    static constexpr float kGainRangeDb = 30.0f;
    /** The linear gain of a Gain parameter value (0.5 = 1). */
    static float gainOf(float value) { return std::pow(10.0f, (value - 0.5f) * 2.0f * kGainRangeDb / 20.0f); }

    enum Param : int {
        Osc1Wave = 0, Osc2Wave, Osc2Detune, Osc2Octave, OscMix, SubLevel, NoiseLevel,
        FilterType, Cutoff, Resonance, FilterEnvAmount,
        FilterAttack, FilterDecay, FilterSustain, FilterRelease,
        AmpAttack, AmpDecay, AmpSustain, AmpRelease,
        Lfo1Rate, Lfo1ToPitch, Lfo2Rate, Lfo2ToCutoff, Level,
        // Appended (the JNI contract and saved projects keep the order above).
        Unison,        // 0 = oscillator 1 alone, up to 1 = six copies with it (seven voices: supersaw, Reese)
        UnisonDetune,  // 0..1 = spread of the voices, ±3 .. ±50 cents, centred on the note
        Lfo2Sync,      // 0 = the sweep runs at Lfo2Rate; above, locked to the tempo (see syncCyclesPerBeat), restarted on each attack
        Glide,         // 0 = none; above, portamento up to 0.5 s: a note starting while another is held slides from it (one voice)
        Gain,          // calibration of the sound's loudness: 0.5 = 0 dB, 0..1 = -30..+30 dB (see kGainRangeDb)
        // Phase 34 (S4): width and movement. Each one at its default leaves the sound exactly as it was
        // (render() then takes the same path, sample for sample, as before them).
        Width,       // 0..1: the unison copies (without copies, oscillators 1 and 2) spread left and right. Their
                     // mono sum stays the sound without width: only the side (L - R) is added
        OctaveDown,  // 0..1: level of a copy of oscillator 1's wave an octave below the note, centred
        Drive,       // 0..1: saturation after the filter (4x oversampled, anti-aliased), its level compensated
        MonoBelow,   // the stereo folded to mono under 60 x 5^v Hz (default 0.43: 120 Hz; 0.89: 250 Hz)
        Lfo2Shape,   // the sweep's shape: 0 triangle (as before), 1/3 square (a riddim gate), 2/3 falling saw, 1 sine
        CycleBars,   // a slow filter sweep locked to the song's bars: 0 off, then 1, 2, 4, 8, 16 bars a cycle (index / 5)
        CycleDepth,  // its depth: 0..1 = 0..+-2 octaves around the cutoff (closed at the cycle's start, open midway)
        Formant,     // 0 = off, up to 1: the vowel filter's share (the neuro bass that talks)
        Vowel,       // 0..1: A, E, I, O, U; the sweep (Lfo2ToCutoff) moves it too
        MidLayer,    // 0..1: a layer of the sound above 250 Hz, driven and wide (the bass music « mid » over a mono sub)
        Oversample,  // > 0.5: the oscillators run at twice the rate and are filtered down (aliasing under -60 dB)
        // « Des basses qui tapent » (docs/hits/basses-qui-tapent.md, P3), appended: each one at its
        // default leaves the sound exactly as it was (plain() then holds: the same path as before).
        PitchEnv,      // 0..1: the attack starts 0..48 semitones over the note and falls to it (the 808, the donk, the « wub »)
        PitchDecay,    // 0..1: that fall's time constant, 1..500 ms (500^v ms; 0.5: 22 ms)
        PhaseReset,    // > 0.5: each new note starts its oscillators at phase 0 (a voice that was silent): every hit attacks alike
        VelocityCutoff,// 0..1: the velocity opens the filter: +-2 octaves x this at velocity 1 / 0.4, none at 0.7
        SubShape,      // the sub's wave: 0 square (as before), 0.5 triangle, 1 sine
        SubFloor,      // > 0.5: the sub, the octave-down copy and oscillator 2 an octave down never under 30 Hz: an octave up there
        MidLayerHz,    // the mid layer's high-pass: 250 x 1.6^(2v - 1) Hz (0.5: 250 Hz, as before; 156..400)
        MidLayerDrive, // the mid layer's drive: 3 x 3^(2v - 1) (0.5: 3, as before; 1..9)
        // Leads chantier, lots B to D (docs/hits/leads-pros.md § 6 and § 8), appended: each one at its
        // default leaves the sound exactly as it was (plain() then holds: the same path as before).
        KeyTrack,      // 0..1: the cutoff follows the note, 0..1 octave per octave around C4 (60)
        VelocityEnv,   // 0..1: the velocity scales the filter envelope, x(1 +- this) at velocity 1 / 0.4, as is at 0.7
        Click,         // 0..1: a noise click of about 4 ms high-passed at 2 kHz on each attack (over the filter)
        VibratoDelay,  // 0..1: each voice's vibrato waits 0..1 s after its attack, then fades in over as long
        CycleOffset,   // 0..1: CycleBars' phase shifted by this part of a cycle (0.5: open at the cycle's start)
        Supersaw,      // > 0.5: the unison as a JP-8000: Szabo's ratios, free phases on each note, drift, centre/side mix, high-pass at f0
        CleanLadder,   // > 0.5: the ladder linear (zero-delay feedback, no inner saturation): no intermodulation on dense stacks
        PreDrive,      // 0..1: saturation before the filter, on each voice (antiderivative anti-aliased)
        Pwm,           // 0..1: the square waves' pulse width swept by a 0.7 Hz triangle, 50 % +- 40 % x this (the hoover)
        Fm,            // 0..1: a 2-operator FM pair (sine carrier on the note, sine modulator) taking over oscillator 1, its index falling with the filter envelope
        FmRatio,       // the modulator's ratio to the note: 0.5, 1, 1.41, 2, 2.76, 3, 3.5, 4, 7 (index / 8)
        ParamCount
    };
    // The MonoBelow value of 120 Hz (its default) and its frequency.
    static constexpr float kMonoBelowDefault = 0.43f;
    static float monoBelowHz(float v) { return 60.0f * std::pow(5.0f, v); }
    // PitchDecay's time constant (seconds), MidLayerHz's frequency, MidLayerDrive's gain; the sub's floor.
    static float pitchDecaySeconds(float v) { return 0.001f * std::pow(500.0f, v); }
    static float midLayerHz(float v) { return 250.0f * std::pow(1.6f, 2.0f * v - 1.0f); }
    static float midLayerDrive(float v) { return 3.0f * std::pow(3.0f, 2.0f * v - 1.0f); }
    static constexpr float kSubFloorHz = 30.0f;
    static constexpr float kPitchEnvSemitones = 48.0f;
    // FmRatio's choices; the click's high-pass and length; Pwm's sweep rate; Supersaw's drift.
    static constexpr int kFmRatios = 9;
    static float fmRatioOf(float v);
    static constexpr float kClickHz = 2000.0f;
    static constexpr float kClickSeconds = 0.0015f;  // its time constant: about 4 ms to -24 dB
    static constexpr float kPwmHz = 0.7f;
    static constexpr float kDriftCents = 4.0f;  // a copy's drift never goes past this
    // Szabo's JP-8000 detune offsets (« How to Emulate the Super Saw », 2010) over the outer one: the
    // copies' places in Supersaw mode, in pairs from the outside in.
    static constexpr float kJpOffsets[6] = {-1.0f, 0.977f, -0.573f, 0.565f, -0.177f, 0.181f};
    // CycleBars' cycle lengths in bars (index 0: off).
    static constexpr int kCycleChoices = 6;
    static int cycleBarsOf(float v);

    void prepare(int sampleRate);  // not real-time safe the first time (filter setup)
    void setParam(Param param, float value);  // value 0..1
    float param(Param param) const { return params_[static_cast<size_t>(param)]; }

    void noteOn(int midiNote, float velocity, int frameOffset = 0);
    void noteOff(int midiNote, int frameOffset = 0);
    void allNotesOff();
    // Live expression: pitch bend in semitones, mod wheel 0..1 (more vibrato).
    // The most voices played at once (1..kVoices; G1b: a track's layer plays kLayerVoices). A voice
    // past the limit still sounding (the limit just lowered) finishes its note.
    void setVoiceLimit(int voices) { voiceLimit_ = std::clamp(voices, 1, kVoices); }
    int voiceLimit() const { return voiceLimit_; }
    void setBend(float semitones) { bend_ = semitones; }
    void setMod(float amount) { mod_ = amount; }
    // The length of a beat in frames, from the sequencer, every block: the tempo-locked sweep follows it.
    void setBeatFrames(double frames);
    // Sweep cycles per beat (Lfo2Sync's divisions): free, 1 bar, 1/2, 1/4, 1/8, 1/16, 1/4 triplet, 1/8 triplet.
    static constexpr int kSyncDivisions = 8;
    static float syncCyclesPerBeat(float sync);
    // The sweep's rate in Hz as set now (free or locked), for tests.
    float sweepHz() const { return sweepHz_; }
    // Adds the voices into `left` (and `right`). Without a right channel, or while the sound is mono
    // (stereo() false), only `left` is written: the mono sound.
    void render(float* left, float* right, int numFrames);
    void render(float* mono, int numFrames) { render(mono, nullptr, numFrames); }
    // The sound has a side (Width or MidLayer above 0, or its side still fading out): render()
    // writes both channels.
    bool stereo() const { return wideWanted() || widthNow_ > 0.0f || layerNow_ > 0.0f; }
    bool wideWanted() const { return param(Width) > 0.0f || param(MidLayer) > 0.0f; }
    // Every S4 setting at its default: the sound as before them (tests).
    bool plain() const;
    // A setting from an automation lane, `offset` frames into the next render() (on its step's frame).
    void automate(Param param, float value, int offset);
    // The song's position for CycleBars: sequencer step `step` (in sixteenths, from the song's start)
    // falls `offset` frames into the next render(); a bar is `stepsPerBar` steps.
    void syncCycle(int step, int offset, int stepsPerBar = 16);
    float cyclePosition() const { return static_cast<float>(cycleSteps_); }  // tests
    // The transport starts: both LFOs and the noise from their start, as in an export.
    void restartLfos() {
        lfo1_.Reset(0.0f);
        lfo2_.Reset(0.0f);
        noiseState_ = kNoiseSeed;
        // Lots B to D: the click's noise, the supersaw's phases and drift, the pulse sweep from their start.
        clickState_ = kClickSeed;
        phaseState_ = kPhaseSeed;
        pwmPhase_ = 0.0f;
    }

    int activeVoices() const;
    // Supersaw: voice `voice`'s copy `copy` (0: oscillator 1) drift in cents now (tests).
    float driftOf(int voice, int copy) const { return voices_[static_cast<size_t>(voice)].drift[static_cast<size_t>(copy)]; }

private:
    using Down2x = hiir::Downsampler2xFpu<hiir::coef::C2x094::X2::_nbr_coef>;
    struct Bandpass {  // Zavalishin's state-variable filter (TPT), band output normalised to 1 at its centre
        float g = 0.0f, k = 0.1f, s1 = 0.0f, s2 = 0.0f;
        void set(float hz, float q, float rate);
        float band(float x);
        float high(float x);
        void reset() { s1 = s2 = 0.0f; }
    };
    struct Voice {
        daisysp::Oscillator osc1, osc2, sub, low;  // low: oscillator 1's wave an octave down (OctaveDown)
        std::array<daisysp::Oscillator, 6> unison;  // detuned copies of oscillator 1
        daisysp::LadderFilter ladder, ladderR;  // the right channel's filter: only while the sound is wide
        daisysp::Svf svf, svfR;
        Down2x downL, downR;  // Oversample: the oscillators' two samples per frame filtered down
        daisysp::Adsr ampEnv, filterEnv;
        int note = -1;
        float velocity = 0.0f;
        bool gate = false;
        uint32_t age = 0;
        float baseHz = 440.0f;
        float level = 0.0f;  // last amplitude envelope value (voice stealing takes the quietest released one)
        float semis = 0.0f;      // pitch now (MIDI note, fractional while gliding)
        float target = 0.0f;     // where the glide goes
        float glideStep = 0.0f;  // semitones per control tick, 0 = not gliding
        bool fresh = false;      // a new note: its pitch and cutoff are set on its first frame (not the next tick)
        float cycle = 0.0f;      // CycleBars' octaves at the last control tick
        float pitchEnv = 0.0f;   // PitchEnv: 1 at the attack, falling to 0
        // Lots B to D.
        float click = 0.0f;      // Click's envelope: 1 at the attack, falling
        uint32_t frames = 0;     // since the attack (VibratoDelay)
        std::array<float, 7> drift{};   // Supersaw: each copy's drift (cents), oscillator 1 first
        std::array<float, 7> driftTo{}; // and where it heads
        Bandpass hpMid{}, hpSide{};     // Supersaw: the high-pass at f0
        struct Ladder4 {                // CleanLadder: Zavalishin's linear 4-pole ladder (TPT)
            float s[4] = {0, 0, 0, 0};
            float process(float x, float G, float k);
        } cleanL{}, cleanR{};
        float ladderG = 0.0f;           // CleanLadder's one-pole gain g / (1 + g), at the last tick
        float preU[2] = {0, 0}, preF[2] = {1, 1};  // PreDrive's antiderivative states (u, F(u)), left and right
        float fmCar = 0.0f, fmMod = 0.0f;          // Fm: the carrier's and the modulator's phases (cycles)
        float fmInc = 0.0f, fmModInc = 0.0f;       // and their steps per (oversampled) frame
        float vibrato = 1.0f;                      // VibratoDelay: the share of the vibrato heard now
    };

    struct Event {
        int offset = -1;
        int note = 0;  // a note; for a setting (param >= 0): unused
        float velocity = 0.0f;  // a note's; for a setting: its value
        bool on = false;
        int param = -1;  // >= 0: an automated setting (automate())
    };

    void applyEvent(const Event& e);
    // Frames [from, to) of render()'s call (the events' frames) into tmpL_ / tmpR_ (index i - from).
    template <bool Wide, bool Over>
    void renderVoices(int from, int to);
    // The same with the leads chantier's lots B to D, while one of their settings is on (else
    // renderVoices, the code from before them: the same sound, sample for sample).
    template <bool Wide, bool Over>
    void renderLeads(int from, int to);
    bool leadsAtRest() const;
    void renderLegacy(float* mono, int numFrames);
    void setOversampled(bool on);
    void updateShape();
    // The track-wide stages after the voices (S4): formant, drive, mid layer, mono lows.
    void postProcess(float* l, float* r, int n, bool wide);
    struct SoftTanh {
        double f(double x) const { return std::tanh(x); }
        double F(double x) const {
            const double a = std::fabs(x);
            return a + std::log1p(std::exp(-2.0 * a)) - 0.69314718055994531;  // log(cosh(x)), without overflow
        }
    };
    void queue(const Event& e);
    void updateVoiceSettings(Voice& v);
    void updateSweepRate();
    float noise();

    int sampleRate_ = 48000;
    bool prepared_ = false;
    std::array<Voice, kVoices> voices_{};
    int voiceLimit_ = kVoices;
    std::array<float, ParamCount> params_{};
    std::array<Event, 256> events_{};  // room to release all 128 notes at once (sustain pedal let go)
    int eventCount_ = 0;
    daisysp::Oscillator lfo1_, lfo2_;
    uint32_t clock_ = 0;
    float bend_ = 0.0f, mod_ = 0.0f;
    float unisonGain_ = 1.0f;  // follows the stack gain smoothly when the number of copies changes
    // Frames to the next control tick: counted across calls, so the ticks fall on the same frames
    // whatever the callback's size (an export in blocks of 512 sounds like the phone's 192).
    int controlCountdown_ = 0;
    static constexpr uint32_t kNoiseSeed = 1234567u;
    uint32_t noiseState_ = kNoiseSeed;
    double beatFrames_ = 24000.0;  // 120 BPM at 48 kHz until the sequencer says otherwise
    float sweepHz_ = 0.0f;
    // --- S4 state.
    bool oversampled_ = false;
    // Review m3: no click on a switch while notes sound. Oversampling (it starts the oscillators
    // again) waits for the sound to dip to silence over 3 ms, then comes back over 3 ms; the side of
    // a wide sound glides (widthNow_, layerNow_: Width and MidLayer as played, a few milliseconds
    // behind), and the sound goes back to mono only once its side has died away: no jump.
    bool overWanted_ = false;
    float duckGain_ = 1.0f, duckStep_ = 1.0f / 144.0f;
    float widthNow_ = 0.0f, layerNow_ = 0.0f, widthGlide_ = 0.007f;
    bool switching() const {
        return overWanted_ != oversampled_ || duckGain_ < 1.0f || widthNow_ != param(Width) || layerNow_ != param(MidLayer);
    }
    float sweepSmooth_ = 0.0f;  // the square sweep's edges, softened over about a millisecond
    double cycleSteps_ = 0.0;   // the song position (sixteenths) at the next frame rendered
    int stepsPerBar_ = 16;
    int cycleSyncStep_ = -1, cycleSyncOffset_ = 0;  // a step announced for the next render()
    bool wasWide_ = false;      // the right channel's filters ran last frame (else they start from the left's state)
    float driveComp_ = 1.0f;    // the drive's level compensation
    // The voices of a block before the track's stages (at most kChunk frames at a time).
    static constexpr int kChunk = 256;
    std::array<float, kChunk> tmpL_{}, tmpR_{};
    std::array<float, kChunk> sweepBuf_{};  // the sweep's value on each frame of the chunk (the vowel follows it)
    int eventCursor_ = 0;
    std::array<Bandpass, 3> formantL_{}, formantR_{};
    std::array<DriveShaper<Oversampler4x>, 2> drive_{};
    DriveShaper<Oversampler4x> midDrive_;
    Bandpass midHigh_{}, monoSide_[2]{};
    std::array<float, 1024> midDelay_{};  // the mid layer's side: itself 11 ms later (mono-compatible width)
    int midDelayPos_ = 0;
    bool postActive_ = false;  // the post stages ran last block (their state is warm)
    // --- Lots B to D state (deterministic: from their seeds again when the transport starts).
    static constexpr uint32_t kClickSeed = 0x2545F491u, kPhaseSeed = 0x9E3779B9u;
    uint32_t clickState_ = kClickSeed, phaseState_ = kPhaseSeed;
    float random(uint32_t& state);  // -1..1
    Bandpass clickHigh_{};
    float pwmPhase_ = 0.0f;  // Pwm's triangle, 0..1
    bool pwmWas_ = false;    // Pwm moved the pulse widths: back to 50 % once when it goes to 0
    float preComp_ = 1.0f;   // PreDrive's level compensation
};

}  // namespace ps
