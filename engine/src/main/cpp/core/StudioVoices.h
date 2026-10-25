#pragma once

#include <algorithm>
#include <cmath>
#include <cstdint>

namespace ps {

// The « studio » drum voices of phase 34 (docs/PLAN-SON-PRO.md, S3): a dedicated 808 bass, a
// short 909-style kick (and its hard / gabber variant) and a rumble tail. Any pad of the built-in
// kit may play one of them instead of its own voice (DrumKit::setKind). Everything here is
// fixed-size state and plain arithmetic: real-time safe, no Android.
namespace studio {

constexpr float kTwoPi = 6.2831853f;

inline float decayCoef(float seconds, int sampleRate) {
    return std::exp(-1.0f / (std::max(seconds, 1e-4f) * static_cast<float>(sampleRate)));
}
// Coefficient for a decay that reaches -40 dB (the audible end of a drum) after `seconds`.
inline float decayTo40dB(float seconds, int sampleRate) { return decayCoef(seconds / 4.605f, sampleRate); }
inline float noteHz(float midi) { return 440.0f * std::exp2((midi - 69.0f) / 12.0f); }
// A soft clip with a little asymmetry: odd and even harmonics (a transformer, a tape). Scaled so
// that a full-scale input comes out near full scale whatever the drive (the drive adds harmonics
// and density, not level).
inline float softClip(float x, float drive) {
    const float g = 1.0f + 6.0f * drive;
    const float bias = 0.25f * drive;
    const float top = std::tanh(g + bias) - std::tanh(bias);
    return (std::tanh(g * x + bias) - std::tanh(bias)) / top;
}
// A sine and a triangle of the same phase (both rise from 0 at phase 0).
inline float sineOf(float phase) { return std::sin(kTwoPi * phase); }
inline float triOf(float phase) {
    float p = phase + 0.25f;
    p -= std::floor(p);
    return 1.0f - 4.0f * std::fabs(p - 0.5f);
}

// One-pole low-pass and high-pass (6 dB/oct), for the layers' tone.
struct OnePole {
    float coef = 0.1f;
    float state = 0.0f;
    void set(float hz, int sampleRate) {
        coef = std::clamp(1.0f - std::exp(-kTwoPi * hz / static_cast<float>(sampleRate)), 1e-5f, 1.0f);
    }
    float low(float x) {
        state += coef * (x - state);
        return state;
    }
    float high(float x) { return x - low(x); }
    void reset() { state = 0.0f; }
};

// A 2-pole state-variable filter, trapezoidal (Zavalishin's TPT form, as Andrew Simper writes
// it): stable and true to its response up to Nyquist. set() takes a tan(): call it on a hit or a
// setting, never per sample.
struct Svf2 {
    float a1 = 1.0f, a2 = 0.0f, a3 = 0.0f, k = 1.414f;
    float ic1 = 0.0f, ic2 = 0.0f;
    float lowOut = 0.0f, bandOut = 0.0f, highOut = 0.0f;
    void set(float hz, float resonance, int sampleRate) {
        const float fc = std::clamp(hz, 10.0f, 0.45f * static_cast<float>(sampleRate));
        const float g = std::tan(3.14159265f * fc / static_cast<float>(sampleRate));
        k = 1.0f / std::clamp(resonance, 0.5f, 20.0f);
        a1 = 1.0f / (1.0f + g * (g + k));
        a2 = g * a1;
        a3 = g * a2;
    }
    void tick(float x) {
        const float v3 = x - ic2;
        const float v1 = a1 * ic1 + a2 * v3;
        const float v2 = ic2 + a2 * ic1 + a3 * v3;
        ic1 = 2.0f * v1 - ic1;
        ic2 = 2.0f * v2 - ic2;
        lowOut = v2;
        bandOut = v1;
        highOut = x - k * v1 - v2;
    }
    float low(float x) {
        tick(x);
        return lowOut;
    }
    float band(float x) {
        tick(x);
        return bandOut;
    }
    float high(float x) {
        tick(x);
        return highOut;
    }
    void reset() { ic1 = ic2 = lowOut = bandOut = highOut = 0.0f; }
};

// Settings shared by the studio voices (DrumKit::VoiceParam gives their units).
struct Settings {
    float tune = 36.0f;    // 808: MIDI note; kicks and rumble: Hz
    float decay = 0.8f;    // seconds to -40 dB
    float drive = 0.3f;    // 0..1
    float click = 0.5f;    // 0..1, the attack layer
    float glide = 0.08f;   // seconds, 808: slide between pitched hits
    float sub = 0.5f;      // 0..1, the kicks' sub layer
    float tone = 0.2f;     // 0..1, the 808's octave and twelfth layer (v2: its 4th to 8th harmonics), the rumble's cut-off
    // « L'808 qui tape » (docs/hits/basses-qui-tapent.md, P2), appended: the 808's version, 0 as it
    // was (the projects saved before keep their sound), 1 the punchy one: the attack starts `drop`
    // semitones over the note and falls with a time constant of `dropTime` seconds, « tone » adds its
    // 4th to 8th harmonics (heard on a phone), a drive over 0.75 adds a second, asymmetric clip.
    float version = 0.0f;
    float drop = 24.0f;      // semitones 0..36 (v2)
    float dropTime = 0.025f; // seconds 0.005..0.15 (v2)
};

// The 808 bass: a sine body with a short pitch drop at the attack, a click, a saturation and an
// octave / twelfth layer that give it its 60-250 Hz harmonics, a long tuned note (it holds, then
// falls), and a glide between pitched hits (legato: the phase runs on, the pitch slides over the
// glide time). Version 1 (Settings::version, « l'808 qui tape »): a drop of its own in semitones
// and time (+24 st over 25 ms: the « boum »), its 4th to 8th harmonics for « tone » (the octave and
// the twelfth stay under 200 Hz: a phone, which plays almost nothing there, never heard them), and a
// second asymmetric clip for a hot drive (phonk).
// Mono: below 120 Hz it is the same on both sides (the kit pans it to the centre).
struct Bass808 {
    // State
    float phase = 0.0f;
    float hz = 65.0f, targetHz = 65.0f, glideRatio = 1.0f;
    int glideLeft = 0;
    float pitchEnv = 0.0f;
    float dropCoef = 0.9965f;  // v2: the drop's fall per frame (its time constant)
    float env = 0.0f, envTarget = 0.0f;
    float clickEnv = 0.0f;
    float ampCoef = 0.9999f;
    int holdLeft = 0;  // frames the level holds before it falls (a long 808 note sustains)
    OnePole clickHp, toneLp;
    uint32_t noise = 0x9E3779B9u;

    void prepare(int sampleRate, const Settings& s) {
        clickHp.set(1800.0f, sampleRate);
        toneLp.set(2200.0f, sampleRate);
        setDecay(sampleRate, s);
    }
    // Holds 60 % of the decay, then falls to -40 dB over the rest.
    void setDecay(int sampleRate, const Settings& s) {
        const float decay = std::clamp(s.decay, 0.05f, 2.5f);
        ampCoef = decayTo40dB(0.4f * decay, sampleRate);
        holdLeft = static_cast<int>(0.6f * decay * static_cast<float>(sampleRate));
    }
    bool sounding() const { return env > 1e-4f || envTarget > 1e-4f; }
    // Version 1: the 4th-8th harmonics' share at tone 1, the attack's lift at its start.
    static constexpr float kUpper = 0.9f;
    static constexpr float kPunch = 0.8f;
    static float shape(float x, float drive) {
        if (drive <= 0.0f) return x;
        const float g = 1.0f + 10.0f * drive;
        return std::tanh(g * x) / std::tanh(g);
    }
    // `pitch`: semitones added to the tune for this hit; `glideFrames` > 0 while sounding: slide.
    void hit(int sampleRate, const Settings& s, float velocity, float pitch, int glideFrames) {
        const float target = std::clamp(noteHz(std::clamp(s.tune + pitch, 12.0f, 72.0f)), 20.0f, 400.0f);
        setDecay(sampleRate, s);
        if (sounding() && glideFrames > 0) {
            // Legato: the body goes on, its pitch slides to the new note (exponential: equal
            // semitones per frame), its level moves to the new velocity over 3 ms.
            targetHz = target;
            glideLeft = glideFrames;
            glideRatio = std::exp(std::log(target / std::max(hz, 1.0f)) / static_cast<float>(glideFrames));
            envTarget = velocity;
            return;
        }
        phase = 0.0f;  // a sine from zero: no click of its own
        hz = targetHz = target;
        glideLeft = 0;
        glideRatio = 1.0f;
        pitchEnv = 1.0f;
        dropCoef = decayCoef(std::clamp(s.dropTime, 0.005f, 0.15f), sampleRate);
        env = envTarget = velocity;
        clickEnv = velocity * s.click;
    }
    float process(int sampleRate, const Settings& s) {
        if (!sounding() && clickEnv < 1e-5f) return 0.0f;
        if (glideLeft > 0) {
            hz *= glideRatio;
            if (--glideLeft == 0) hz = targetHz;
        }
        const bool v2 = s.version >= 0.5f;
        // The attack's drop. Version 0: from 2.5 x the note down to it, a time constant of 6 ms at
        // 48 kHz (0.9965 a frame: gone in about 25 ms; its comment said 12 ms, the voice always had
        // 6, and the projects saved with it keep it). Version 1: `drop` semitones over the note,
        // falling with its own time constant (+24 st and 25 ms in trap: the « boum »).
        const float f = v2 ? hz * std::exp2(std::clamp(s.drop, 0.0f, 36.0f) * pitchEnv * (1.0f / 12.0f)) : hz * (1.0f + 1.5f * pitchEnv);
        pitchEnv *= v2 ? dropCoef : 0.9965f;
        phase += f / static_cast<float>(sampleRate);
        if (phase >= 1.0f) phase -= 1.0f;
        const float body = sineOf(phase);
        env += (envTarget - env) * 0.007f;  // 3 ms
        if (holdLeft > 0) {
            --holdLeft;
        } else {
            envTarget *= ampCoef;
            env *= ampCoef;
        }
        // Click: 2 ms of bright noise (the beater of the drum the 808 imitates).
        noise ^= noise << 13;
        noise ^= noise >> 17;
        noise ^= noise << 5;
        const float n = static_cast<float>(noise) * (2.0f / 4294967296.0f) - 1.0f;
        const float click = clickHp.high(n) * clickEnv * 0.5f;
        clickEnv *= 0.99f;  // 2 ms
        // The saturation squares the body off (its odd harmonics), its level kept; the tone adds
        // the octave and the twelfth in parallel, after it (inside it, the clipping would flatten
        // them again): what a phone, which plays nothing under 100 Hz, hears of an 808.
        const float tone = std::clamp(s.tone, 0.0f, 1.0f);
        const float drive = std::clamp(s.drive, 0.0f, 1.0f);
        if (v2) {
            // A drive over 0.75 squares it off harder and asymmetrically (even harmonics too: phonk);
            // « tone » adds the 4th to 8th harmonics (175-350 Hz for F1), following the note's level.
            float driven = shape(body * env, drive);
            if (drive > 0.75f) driven = softClip(driven, 4.0f * (drive - 0.75f));
            // Each harmonic faded out as it nears 0.45 x the rate (a high note on its +36 semitone
            // drop: its 8th would otherwise fold back down as aliasing).
            const float top = 0.45f * static_cast<float>(sampleRate), fade = 1.0f / (0.05f * static_cast<float>(sampleRate));
            auto under = [&](float k) { return std::clamp((top - k * f) * fade, 0.0f, 1.0f); };
            const float upper = 0.55f * under(4.0f) * sineOf(4.0f * phase) + 0.45f * under(5.0f) * sineOf(5.0f * phase) +
                                0.35f * under(6.0f) * sineOf(6.0f * phase) + 0.28f * under(7.0f) * sineOf(7.0f * phase) +
                                0.22f * under(8.0f) * sineOf(8.0f * phase);
            const float harmonics = tone * kUpper * upper * env;
            // The attack lifted while the pitch drops (+5 dB at its start, gone with the drop): the
            // first 5 to 40 ms louder than the note, the « boum » that hits (B5 >= +4 dB).
            const float punch = 1.0f + kPunch * pitchEnv;
            return (toneLp.low((driven + harmonics) * punch) + click) * (1.0f / (1.0f + kUpper * tone));
        }
        const float driven = shape(body * env, drive);
        const float harmonics = tone * (0.8f * sineOf(2.0f * phase) + 0.4f * sineOf(3.0f * phase)) * env;
        return (toneLp.low(driven + harmonics) + click) * (1.0f / (1.0f + 0.9f * tone));
    }
};

// A short 909-style kick: a sine with a fast two-stage pitch envelope (about 4 x the tune down
// to it), a separate sub layer at the tune, a click / attack layer (noise through a band-pass
// around 3 kHz plus the first half-cycle brightened), and a soft clip. -40 dB within 350 ms by
// default. The hard variant (gabber): more drive into the clip, a longer and deeper sweep, and a
// low-pass after the clip: the « doof ».
struct StudioKick {
    bool hard = false;
    float phase = 0.0f, subPhase = 0.0f;
    float fast = 0.0f, slow = 0.0f;  // pitch envelopes
    float amp = 0.0f, subAmp = 0.0f, clickEnv = 0.0f;
    float ampCoef = 0.999f, subCoef = 0.999f, fastCoef = 0.99f, slowCoef = 0.999f;
    Svf2 clickBand;
    OnePole hardLp;
    uint32_t noise = 0x7F4A7C15u;

    void prepare(int sampleRate, const Settings& s) {
        clickBand.set(hard ? 2200.0f : 3200.0f, 1.2f, sampleRate);
        hardLp.set(2500.0f, sampleRate);
        configure(sampleRate, s);
    }
    void configure(int sampleRate, const Settings& s) {
        const float decay = std::clamp(s.decay, 0.08f, 1.2f);
        ampCoef = decayTo40dB(decay, sampleRate);
        subCoef = decayTo40dB(decay, sampleRate);
        fastCoef = decayCoef(hard ? 0.030f : 0.014f, sampleRate);
        slowCoef = decayCoef(hard ? 0.09f : 0.06f, sampleRate);
    }
    void hit(int sampleRate, const Settings& s, float velocity) {
        configure(sampleRate, s);
        phase = subPhase = 0.0f;
        fast = slow = 1.0f;
        amp = velocity;
        subAmp = velocity * std::clamp(s.sub, 0.0f, 1.0f);
        clickEnv = velocity * std::clamp(s.click, 0.0f, 1.0f);
    }
    bool sounding() const { return amp > 1e-5f || subAmp > 1e-5f || clickEnv > 1e-5f; }
    float process(int sampleRate, const Settings& s) {
        if (!sounding()) return 0.0f;
        const float tune = std::clamp(s.tune, 30.0f, 120.0f);
        // Two stages: 3 x the tune gone in ~15 ms, a quarter above it gone in ~60 ms.
        const float f = tune * (1.0f + (hard ? 4.0f : 3.0f) * fast + 0.25f * slow);
        fast *= fastCoef;
        slow *= slowCoef;
        phase += f / static_cast<float>(sampleRate);
        if (phase >= 1.0f) phase -= 1.0f;
        subPhase += tune / static_cast<float>(sampleRate);
        if (subPhase >= 1.0f) subPhase -= 1.0f;
        const float body = sineOf(phase) * amp;
        const float sub = sineOf(subPhase) * subAmp;
        amp *= ampCoef;
        subAmp *= subCoef;
        noise ^= noise << 13;
        noise ^= noise >> 17;
        noise ^= noise << 5;
        const float n = static_cast<float>(noise) * (2.0f / 4294967296.0f) - 1.0f;
        // The attack: 6 ms of band-passed noise (the beater) plus the body's own first half-cycle.
        const float click = (clickBand.band(n) * 4.0f + (fast > 0.5f ? 0.5f * body : 0.0f)) * clickEnv;
        clickEnv *= 0.9965f;  // 6 ms
        const float drive = std::clamp(s.drive, 0.0f, 1.0f);
        if (hard) {
            // Into the clip hard: a square-ish « doof », then a low-pass takes the fizz off; the
            // click goes over it.
            const float x = (body + sub) * (1.5f + 6.0f * drive);
            return hardLp.low(std::tanh(x)) * 0.9f + click * 0.4f;
        }
        // The click goes into the clip with the body: the peak stays bounded (a dense kick).
        return softClip((body + sub) * 0.8f + click * 0.5f, drive) * 0.9f;
    }
};

// A rumble: the low tail techno puts under its kick (a kick into a long reverb, distorted and
// low-passed). Here: a tuned low tone with its fifth and some noise, a 20 ms rise, a decay, a
// soft clip, then a 2-pole low-pass (tone: 60..240 Hz) and a high-pass at 30 Hz.
struct Rumble {
    float phase = 0.0f, phase2 = 0.0f;
    float env = 0.0f, rise = 0.0f;
    float ampCoef = 0.9999f;
    Svf2 lp;
    OnePole hp;
    uint32_t noise = 0x2545F491u;

    void prepare(int sampleRate, const Settings& s) {
        hp.set(30.0f, sampleRate);
        configure(sampleRate, s);
    }
    void configure(int sampleRate, const Settings& s) {
        ampCoef = decayTo40dB(std::clamp(s.decay, 0.1f, 2.0f), sampleRate);
        lp.set(60.0f + 180.0f * std::clamp(s.tone, 0.0f, 1.0f), 1.5f, sampleRate);
    }
    void hit(int sampleRate, const Settings& s, float velocity) {
        configure(sampleRate, s);
        env = velocity;
        rise = 0.0f;
    }
    bool sounding() const { return env > 1e-5f; }
    float process(int sampleRate, const Settings& s) {
        if (!sounding()) return 0.0f;
        const float tune = std::clamp(s.tune, 30.0f, 120.0f);
        phase += tune / static_cast<float>(sampleRate);
        if (phase >= 1.0f) phase -= 1.0f;
        phase2 += tune * 1.5f / static_cast<float>(sampleRate);
        if (phase2 >= 1.0f) phase2 -= 1.0f;
        noise ^= noise << 13;
        noise ^= noise >> 17;
        noise ^= noise << 5;
        const float n = static_cast<float>(noise) * (2.0f / 4294967296.0f) - 1.0f;
        rise += (1.0f - rise) * 0.001f;  // 20 ms
        const float source = (0.8f * sineOf(phase) + 0.35f * sineOf(phase2) + 0.5f * n) * env * rise;
        env *= ampCoef;
        const float driven = softClip(source, std::clamp(s.drive, 0.0f, 1.0f));
        return hp.high(lp.low(driven)) * 0.8f;
    }
};

}  // namespace studio
}  // namespace ps
