#include "DrumKit.h"

#include <algorithm>
#include <cmath>
#include <cstring>

#include "Pan.h"

extern "C" uint32_t ps_rand_exchange(uint32_t state);  // ps_rand.c

namespace ps {

namespace {

// DaisySP's SyntheticBassDrum::Init leaves its transient envelope unset: garbage (now and then NaN,
// which its low-pass never forgets). Start the voice from zeros before Init.
template <class T>
void zeroed(T& voice) {
    std::memset(static_cast<void*>(&voice), 0, sizeof(T));
}
// Per-voice output level, balanced by ear against the kick (kit v1: the projects saved before
// phase 33 keep it).
constexpr float kLevels[DrumKit::kVoices] = {
    1.0f, 1.0f, 0.7f, 0.6f, 0.7f, 0.45f, 0.4f, 0.7f,
    0.7f, 0.65f, 0.6f, 0.45f, 0.35f, 0.35f, 0.6f, 0.5f,
};
// Kit v2 (phase 33, docs/plan/son.md step 1): each voice alone, 4 hits a bar at 128 BPM, near the
// loudness its role has in a mix (WizardMix.ROLE_LUFS): kick -11.5 LUFS, snares and clap -16, hats
// -27, toms -18, ride -28, percussion -22, without raising a voice's peak over -1 dBFS (or over its
// kit v1 peak, when that was already higher): the rim, a click, stays under its target. The hats
// are brighter and the closed one rings longer (applyVersion). The assistant's mix no longer
// leans on extreme faders nor on a 9 dB shelf to lift the hats (measured: WizardMix.KIT_LUFS_V2).
constexpr float kLevelsV2[DrumKit::kVoices] = {
    0.501f, 1.0f, 0.7f, 0.462f, 0.561f, 0.359f, 0.181f, 0.275f,
    0.282f, 0.304f, 0.666f, 0.566f, 0.266f, 0.222f, 0.243f, 0.465f,
};
// Kit v3 (phase 34, S3): the levels of kit v2, except the voices that changed, each matched to
// its kit v2 loudness (K-weighted energy of one hit over a beat at 128 BPM, as kit v2 was set:
// testKitV3): the studio kick (short: -40 dB within 350 ms, its peak under -1 dBFS), the hard
// kick, the hats with their sizzle and the clap with its air.
constexpr float kLevelsV3[DrumKit::kVoices] = {
    0.72f, 0.635f, 0.7f, 0.462f, 0.426f, 0.34f, 0.169f, 0.275f,
    0.282f, 0.304f, 0.666f, 0.566f, 0.266f, 0.222f, 0.243f, 0.465f,
};
// The studio voices a pad may play instead of its own (DrumKit::Kind, index 1..): their levels
// (the 808 and the kicks near kit v2's kick, the rumble 6 dB under it) and the longest a hit of
// each may ring.
constexpr float kKindLevels[DrumKit::kKinds] = {1.0f, 0.40f, 0.72f, 0.67f, 0.38f};
constexpr float kKindMaxSeconds[DrumKit::kKinds] = {0.0f, 3.0f, 1.3f, 1.3f, 2.2f};
// Longest a hit may ring (seconds). Past it the voice fades out over kFadeSeconds and sleeps:
// saves CPU, and stops DaisySP's analog snare, whose noise never fully dies away.
constexpr float kMaxSeconds[DrumKit::kVoices] = {
    1.5f, 1.2f, 0.8f, 0.8f, 0.6f, 0.4f, 1.5f, 1.2f,
    1.0f, 1.0f, 0.3f, 1.0f, 2.5f, 0.4f, 0.6f, 0.8f,
};
constexpr float kFadeSeconds = 0.06f;
// A studio voice's settings when a pad takes it (setKind): the 808 at C1, the kicks at 52 Hz and
// -40 dB within 350 ms, the rumble at 48 Hz under the kick.
studio::Settings defaultsFor(int kind) {
    studio::Settings s;
    switch (kind) {
        case DrumKit::Bass808: s.tune = 36.0f; s.decay = 0.8f; s.drive = 0.4f; s.click = 0.4f; s.glide = 0.08f; s.tone = 0.5f; break;
        case DrumKit::KickStudio: s.tune = 52.0f; s.decay = 0.25f; s.drive = 0.6f; s.click = 0.5f; s.sub = 0.5f; break;
        case DrumKit::KickHard: s.tune = 55.0f; s.decay = 0.2f; s.drive = 0.9f; s.click = 0.4f; s.sub = 0.3f; break;
        case DrumKit::RumbleVoice: s.tune = 48.0f; s.decay = 0.6f; s.drive = 0.5f; s.tone = 0.4f; break;
        default: break;
    }
    return s;
}

// Drives the kicks into a soft saturation: more body and loudness without hard clipping.
constexpr float kKickDrive = 2.2f;
inline float punch(float x) { return std::tanh(kKickDrive * x) * 0.9f; }

float decayFor(float seconds, int sampleRate) { return std::exp(-1.0f / (seconds * sampleRate)); }
}  // namespace

void DrumKit::PitchedDrum::configure(int sampleRate, float hz, float sweepRatio, float sweepSeconds,
                                     float decaySeconds, float clickAmount) {
    baseHz = hz;
    sweep = sweepRatio;
    pitchDecay = decayFor(sweepSeconds, sampleRate);
    ampDecay = decayFor(decaySeconds, sampleRate);
    click = clickAmount;
}

void DrumKit::PitchedDrum::hit(float velocity) {
    phase = 0.0f;
    pitchEnv = 1.0f;
    amp = velocity;
    clickEnv = velocity;
}

float DrumKit::PitchedDrum::process(int sampleRate) {
    if (amp < 1e-5f) return 0.0f;
    const float hz = baseHz * (1.0f + (sweep - 1.0f) * pitchEnv);
    phase += hz / static_cast<float>(sampleRate);
    if (phase >= 1.0f) phase -= 1.0f;
    const float body = std::sin(6.2831853f * phase) * amp;
    // Attack click: a few milliseconds of the waveform's own transient, brightened.
    const float clickSample = clickEnv * click * (phase < 0.5f ? 1.0f : -1.0f);
    pitchEnv *= pitchDecay;
    amp *= ampDecay;
    clickEnv *= 0.97f;
    return body + clickSample;
}

void DrumKit::prepare(int sampleRate) {
    sampleRate_ = sampleRate;
    const float sr = static_cast<float>(sampleRate);
    pending_.fill(-1);
    activeFrames_.fill(0);
    resetNoise();

    // Kicks: low, long and punchy (Benjamin: "pas assez puissants"); see kKickDrive below.
    kick_.configure(sampleRate, 47.0f, 3.2f, 0.035f, 0.42f, 0.35f);

    zeroed(kick2_);
    kick2_.Init(sr);
    kick2_.SetFreq(50.0f);
    kick2_.SetTone(0.45f);
    kick2_.SetDecay(0.55f);
    kick2_.SetDirtiness(0.45f);
    kick2_.SetFmEnvelopeAmount(0.8f);
    kick2_.SetFmEnvelopeDecay(0.35f);

    lowTom_.configure(sampleRate, 95.0f, 1.7f, 0.05f, 0.30f, 0.15f);
    midTom_.configure(sampleRate, 135.0f, 1.6f, 0.05f, 0.25f, 0.15f);
    highTom_.configure(sampleRate, 190.0f, 1.5f, 0.04f, 0.20f, 0.15f);
    conga_.configure(sampleRate, 260.0f, 1.3f, 0.02f, 0.12f, 0.25f);

    snare_.Init(sr);
    snare_.SetFreq(190.0f);
    snare_.SetTone(0.5f);
    snare_.SetDecay(0.45f);
    snare_.SetSnappy(0.7f);

    snare2_.Init(sr);
    snare2_.SetFreq(210.0f);
    snare2_.SetFmAmount(0.2f);
    snare2_.SetDecay(0.4f);
    snare2_.SetSnappy(0.8f);

    closedHat_.Init(sr);
    closedHat_.SetFreq(3000.0f);
    closedHat_.SetTone(0.6f);
    closedHat_.SetDecay(0.25f);
    closedHat_.SetNoisiness(0.4f);

    openHat_.Init(sr);
    openHat_.SetFreq(3000.0f);
    openHat_.SetTone(0.6f);
    openHat_.SetDecay(0.75f);
    openHat_.SetNoisiness(0.4f);

    cymbal_.Init(sr);
    cymbal_.SetFreq(2400.0f);
    cymbal_.SetTone(0.7f);
    cymbal_.SetDecay(0.9f);
    cymbal_.SetNoisiness(0.6f);

    zeroed(zap_);
    zap_.Init(sr);
    zap_.SetFreq(110.0f);
    zap_.SetTone(0.8f);
    zap_.SetDecay(0.25f);
    zap_.SetFmEnvelopeAmount(1.0f);
    zap_.SetFmEnvelopeDecay(0.6f);

    clapFilter_.Init(sr);
    clapFilter_.SetFreq(1200.0f);
    clapFilter_.SetRes(0.35f);
    clapBurst_.decay = decayFor(0.006f, sampleRate);
    clapTail_.decay = decayFor(0.12f, sampleRate);

    rimFilter_.Init(sr);
    rimFilter_.SetFreq(1750.0f);
    rimFilter_.SetRes(0.85f);
    rimEnv_.decay = decayFor(0.025f, sampleRate);

    cowbellA_.Init(sr);
    cowbellA_.SetWaveform(daisysp::Oscillator::WAVE_POLYBLEP_SQUARE);
    cowbellA_.SetFreq(540.0f);
    cowbellB_.Init(sr);
    cowbellB_.SetWaveform(daisysp::Oscillator::WAVE_POLYBLEP_SQUARE);
    cowbellB_.SetFreq(800.0f);
    cowbellFilter_.Init(sr);
    cowbellFilter_.SetFreq(900.0f);
    cowbellFilter_.SetRes(0.3f);
    cowbellFast_.decay = decayFor(0.015f, sampleRate);
    cowbellSlow_.decay = decayFor(0.30f, sampleRate);

    shakerFilter_.Init(sr);
    shakerFilter_.SetFreq(6000.0f);
    shakerFilter_.SetRes(0.2f);
    shakerEnv_.decay = decayFor(0.06f, sampleRate);

    chokeDecay_ = decayFor(0.004f, sampleRate);
    for (size_t v = 0; v < pro_.size(); ++v) {
        Pro& p = pro_[v];
        p.settings = defaultsFor(kind_[v]);
        p.bass.prepare(sampleRate, p.settings);
        p.kick.hard = kind_[v] == KickHard;
        p.kick.prepare(sampleRate, p.settings);
        p.rumble.prepare(sampleRate, p.settings);
    }
    // Kit v3's layers: the hats' sizzle above 8 kHz, the clap's air above 4 kHz.
    sizzleClosedHp_.set(7000.0f, 0.7f, sampleRate);
    sizzleOpenHp_.set(7000.0f, 0.7f, sampleRate);
    clapAirHp_.set(3500.0f, 0.7f, sampleRate);
    sizzleClosedCoef_ = decayFor(0.018f, sampleRate);
    sizzleOpenCoef_ = decayFor(0.16f, sampleRate);
    clapAirCoef_ = decayFor(0.03f, sampleRate);
    applyVersion();
}

void DrumKit::setKind(int voice, int kind) {
    if (voice < 0 || voice >= kVoices || kind < 0 || kind >= kKinds) return;
    const auto idx = static_cast<size_t>(voice);
    if (kind_[idx] == kind) return;
    kind_[idx] = kind;
    Pro& p = pro_[idx];
    p.settings = defaultsFor(kind);
    p.kick.hard = kind == KickHard;
    p.kick.prepare(sampleRate_, p.settings);
    p.rumble.prepare(sampleRate_, p.settings);
    p.bass.prepare(sampleRate_, p.settings);
    // The kit's own voice, or the old studio voice, stops at once: the pad is another sound now.
    activeFrames_[idx] = 0;
    p.bass.env = p.bass.envTarget = p.bass.clickEnv = 0.0f;
    p.kick.amp = p.kick.subAmp = p.kick.clickEnv = 0.0f;
    p.rumble.env = 0.0f;
    // Back to the kit's own voice: kit v3's kicks take their studio settings again.
    if (kind == KitVoice) applyVersion();
}

void DrumKit::setVoiceParam(int voice, int param, float value) {
    if (voice < 0 || voice >= kVoices || !std::isfinite(value)) return;
    studio::Settings& s = pro_[static_cast<size_t>(voice)].settings;
    switch (param) {
        case Tune: s.tune = std::clamp(value, 12.0f, 400.0f); break;
        case Decay: s.decay = std::clamp(value, 0.02f, 2.5f); break;
        case Drive: s.drive = std::clamp(value, 0.0f, 1.0f); break;
        case Click: s.click = std::clamp(value, 0.0f, 1.0f); break;
        case Glide: s.glide = std::clamp(value, 0.0f, 0.5f); break;
        case Sub: s.sub = std::clamp(value, 0.0f, 1.0f); break;
        case Tone: s.tone = std::clamp(value, 0.0f, 1.0f); break;
        case Version: s.version = value >= 0.5f ? 1.0f : 0.0f; break;
        case PitchDrop: s.drop = std::clamp(value, 0.0f, 36.0f); break;
        case DropTime: s.dropTime = std::clamp(value, 0.005f, 0.15f); break;
        default: break;
    }
}

float DrumKit::voiceParam(int voice, int param) const {
    if (voice < 0 || voice >= kVoices) return 0.0f;
    const studio::Settings& s = pro_[static_cast<size_t>(voice)].settings;
    switch (param) {
        case Tune: return s.tune;
        case Decay: return s.decay;
        case Drive: return s.drive;
        case Click: return s.click;
        case Glide: return s.glide;
        case Sub: return s.sub;
        case Tone: return s.tone;
        case Version: return s.version;
        case PitchDrop: return s.drop;
        case DropTime: return s.dropTime;
        default: return 0.0f;
    }
}

void DrumKit::applyVersion() {
    // Kit v3's two kicks are the studio kick and its hard variant, with their settings (a
    // SetPadVoiceParam after the version tunes them); back to v1 or v2, the kit's own kicks.
    for (int voice : {static_cast<int>(Kick), static_cast<int>(Kick2)}) {
        const auto idx = static_cast<size_t>(voice);
        if (kind_[idx] != KitVoice) continue;
        Pro& p = pro_[idx];
        const int kind = voice == Kick ? KickStudio : KickHard;
        p.settings = defaultsFor(version_ >= 3 ? kind : KitVoice);
        p.kick.hard = kind == KickHard;
        p.kick.prepare(sampleRate_, p.settings);
    }
    // Kit v1's closed hat is a 3 ms tick (most of its energy in its first samples): brought to its
    // place in a mix it would be a loud click. Kit v2's rings like a closed hat (~40 ms).
    closedHat_.SetDecay(version_ >= 2 ? 0.55f : 0.25f);
    // Kit v1's hats are band-passed around 1.8 kHz: most of their energy under 6 kHz, where a real
    // hat has little. Kit v2's are bright (band around 6 kHz): the song gets its « air » from them.
    closedHat_.SetTone(version_ >= 2 ? 0.9f : 0.6f);
    openHat_.SetTone(version_ >= 2 ? 0.9f : 0.6f);
}

void DrumKit::resetNoise() {
    for (size_t v = 0; v < noiseState_.size(); ++v) {
        noiseState_[v] = 22222u + 7919u * static_cast<uint32_t>(v);
        randState_[v] = 0x2545F491u ^ (0x85EBCA6Bu * static_cast<uint32_t>(v + 1));
        if (randState_[v] == 0) randState_[v] = 1;
    }
}

void DrumKit::trigger(int voice, float velocity, int frameOffset, float pan, float pitch, int glideFrames) {
    if (voice < 0 || voice >= kVoices) return;
    const auto idx = static_cast<size_t>(voice);
    pending_[idx] = std::max(0, frameOffset);
    velocity_[idx] = std::clamp(velocity, 0.0f, 1.0f);
    // The 808 is mono and centred: its low end is the same on both sides.
    pendingPan_[idx] = std::isfinite(pan) && kind_[idx] != Bass808 ? pan : 0.0f;
    pendingPitch_[idx] = std::isfinite(pitch) ? std::clamp(pitch, -36.0f, 36.0f) : 0.0f;
    pendingGlide_[idx] = std::max(0, glideFrames);
}

float DrumKit::noise(int voice) {
    uint32_t& s = noiseState_[static_cast<size_t>(voice)];
    s ^= s << 13;
    s ^= s >> 17;
    s ^= s << 5;
    return static_cast<float>(s) * (2.0f / 4294967296.0f) - 1.0f;
}

void DrumKit::warmUp(int voice) {
    constexpr int kWarmFrames = 128;
    if (voice == Clap) {
        for (int k = 0; k < kWarmFrames; ++k) clapFilter_.Process(noise(Clap));
    } else if (voice == Shaker) {
        for (int k = 0; k < kWarmFrames; ++k) shakerFilter_.Process(noise(Shaker));
    }
}

float DrumKit::renderStudio(int voice, int kind, bool trig, float v) {
    Pro& p = pro_[static_cast<size_t>(voice)];
    switch (kind) {
        case Bass808:
            if (trig) p.bass.hit(sampleRate_, p.settings, v, pendingPitch_[static_cast<size_t>(voice)], pendingGlide_[static_cast<size_t>(voice)]);
            return p.bass.process(sampleRate_, p.settings);
        case KickStudio:
        case KickHard:
            if (trig) p.kick.hit(sampleRate_, p.settings, v);
            return p.kick.process(sampleRate_, p.settings);
        case RumbleVoice:
            if (trig) p.rumble.hit(sampleRate_, p.settings, v);
            return p.rumble.process(sampleRate_, p.settings);
        default: return 0.0f;
    }
}

float DrumKit::renderVoice(int voice, bool trig, float v) {
    if (const int kind = kind_[static_cast<size_t>(voice)]; kind != KitVoice) return renderStudio(voice, kind, trig, v);
    if (version_ >= 3) {
        // Kit v3: the two kicks are the studio voices (their settings those of the kinds).
        if (voice == Kick) return renderStudio(voice, KickStudio, trig, v);
        if (voice == Kick2) return renderStudio(voice, KickHard, trig, v);
    }
    switch (voice) {
        case Kick: if (trig) kick_.hit(v); return punch(kick_.process(sampleRate_));
        case Kick2: if (trig) kick2_.SetAccent(v); return punch(kick2_.Process(trig));
        case Snare: if (trig) snare_.SetAccent(v); return snare_.Process(trig);
        case Snare2: if (trig) snare2_.SetAccent(v); return snare2_.Process(trig);
        case LowTom: if (trig) lowTom_.hit(v); return lowTom_.process(sampleRate_);
        case MidTom: if (trig) midTom_.hit(v); return midTom_.process(sampleRate_);
        case HighTom: if (trig) highTom_.hit(v); return highTom_.process(sampleRate_);
        case Conga: if (trig) conga_.hit(v); return conga_.process(sampleRate_);
        case Zap: if (trig) zap_.SetAccent(v); return zap_.Process(trig);
        case ClosedHat: {
            if (trig) closedHat_.SetAccent(v);
            float out = closedHat_.Process(trig);
            if (version_ >= 3) {
                // The sizzle: bright noise (above 7 kHz) over the metallic body, 18 ms.
                if (trig) sizzleClosed_ = v;
                out += sizzleClosedHp_.high(noise(ClosedHat)) * sizzleClosed_ * 0.3f;
                sizzleClosed_ *= sizzleClosedCoef_;
            }
            return out;
        }
        case OpenHat: {
            if (trig) {
                openHat_.SetAccent(v);
                openHatChoke_ = 1.0f;
            } else if (openHatChoke_ < 1.0f) {
                openHatChoke_ *= chokeDecay_;
            }
            float out = openHat_.Process(trig);
            if (version_ >= 3) {
                if (trig) sizzleOpen_ = v;
                out += sizzleOpenHp_.high(noise(OpenHat)) * sizzleOpen_ * 0.22f;
                sizzleOpen_ *= sizzleOpenCoef_;
            }
            return out * openHatChoke_;
        }
        case Cymbal: if (trig) cymbal_.SetAccent(v); return cymbal_.Process(trig);
        case Clap: {
            // Three quick noise bursts 10 ms apart, then a short tail. Kit v3: four bursts 8 ms
            // apart (hands that do not land together), a longer tail, and air above 3.5 kHz.
            if (trig) clapCounter_ = 0;
            const int bursts = version_ >= 3 ? 4 : 3;
            const int burstGap = version_ >= 3 ? sampleRate_ / 125 : sampleRate_ / 100;
            if (clapCounter_ >= 0) {
                if (clapCounter_ % burstGap == 0) clapBurst_.hit(v);
                if (clapCounter_ == (bursts - 1) * burstGap) clapTail_.hit((version_ >= 3 ? 0.8f : 0.6f) * v);
                if (++clapCounter_ > (bursts - 1) * burstGap) clapCounter_ = -1;
            }
            const float n = noise(Clap);
            clapFilter_.Process(n);
            const float env = clapBurst_.next() + clapTail_.next();
            float out = clapFilter_.Band() * env * 3.0f;
            if (version_ >= 3) out += clapAirHp_.high(n) * env * 0.35f;
            return out;
        }
        case Rim: {
            if (trig) rimEnv_.hit(v);
            rimFilter_.Process((trig ? 4.0f * v : 0.0f) + 0.05f * noise(Rim) * rimEnv_.level);
            return rimFilter_.Band() * rimEnv_.next() * 2.0f;
        }
        case Cowbell: {
            if (trig) {
                cowbellFast_.hit(0.7f * v);
                cowbellSlow_.hit(0.3f * v);
            }
            cowbellFilter_.Process(0.5f * (cowbellA_.Process() + cowbellB_.Process()));
            return cowbellFilter_.Band() * (cowbellFast_.next() + cowbellSlow_.next()) * 2.0f;
        }
        case Shaker: {
            if (trig) shakerEnv_.hit(v);
            shakerFilter_.Process(noise(Shaker));
            return shakerFilter_.High() * shakerEnv_.next();
        }
        default: return 0.0f;
    }
}

void DrumKit::render(float* mono, int numFrames) {
    renderTo(numFrames, [mono](int, int i, float x, float, float) { mono[i] += x; });
}

void DrumKit::render(float* left, float* right, int numFrames) {
    renderTo(numFrames, [left, right](int, int i, float x, float gl, float gr) {
        left[i] += x * gl;
        right[i] += x * gr;
    });
}

void DrumKit::render(float* const* lefts, float* const* rights, int numFrames) {
    renderTo(numFrames, [lefts, rights](int voice, int i, float x, float gl, float gr) {
        lefts[voice][i] += x * gl;
        rights[voice][i] += x * gr;
    });
}

template <typename Sink>
void DrumKit::renderTo(int numFrames, Sink&& sink) {
    const int fadeLength = static_cast<int>(kFadeSeconds * sampleRate_);
    const int closedHatHit = pending_[ClosedHat] < numFrames ? pending_[ClosedHat] : -1;
    for (int voice = 0; voice < kVoices; ++voice) {
        const auto idx = static_cast<size_t>(voice);
        const int pending = pending_[idx] < numFrames ? pending_[idx] : -1;
        // A hit for a later render (the engine renders a slice in pieces): counted from its start.
        if (pending < 0 && pending_[idx] >= numFrames) pending_[idx] -= numFrames;
        if (activeFrames_[idx] <= 0 && pending < 0) continue;
        // A studio voice (the pad's kind, or kit v3's kicks) has its own length and level.
        const int padKind = kind_[idx];
        int kind = padKind;
        if (kind == KitVoice && version_ >= 3 && (voice == Kick || voice == Kick2)) kind = voice == Kick ? KickStudio : KickHard;
        const int maxLength = static_cast<int>((kind != KitVoice ? kKindMaxSeconds[kind] : kMaxSeconds[voice]) * sampleRate_);
        // The kit's own voices (kit v3's kicks included) at the kit's levels, a pad's studio voice at its own.
        const float level = padKind != KitVoice ? kKindLevels[padKind] : (version_ >= 3 ? kLevelsV3 : version_ >= 2 ? kLevelsV2 : kLevels)[voice];
        const float velocity = velocity_[idx];
        // The ringing hit keeps its own loudness up to the new hit's frame.
        float gain = hitGain_[idx];
        // A closed hat cuts the open one at the exact sample, like on a real machine.
        const int chokeAt = voice == OpenHat ? closedHatHit : -1;
        int& remaining = activeFrames_[idx];
        // Pan gains in locals: the output buffers could alias the members, which would keep the
        // compiler from hoisting them out of the sample loop.
        float gl = gainL_[idx], gr = gainR_[idx];
        const uint32_t sharedRand = ps_rand_exchange(randState_[idx]);
        for (int i = 0; i < numFrames; ++i) {
            if (i == pending) {
                // A noise voice waking up: its filter already running on its noise, as it always
                // was (a cold filter overshoots), for a fixed time (the same whatever the blocks).
                if (remaining <= 0) warmUp(voice);
                remaining = maxLength;
                panGains(pendingPan_[idx], gl, gr);
                // The 808 carries its velocity itself (a glide hit must not step its level).
                gain = kind == Bass808 ? level : level * (0.4f + 0.6f * velocity);
            }
            if (i == chokeAt) openHatChoke_ = 0.999f;
            if (remaining <= 0 && i != pending) {
                // Silent: nothing is computed until its hit (or the block's end), so the voice is
                // in the same state on its hit whatever the block size (an export sounds like live).
                if (pending > i) {
                    if (voice == OpenHat && chokeAt > i && chokeAt < pending) openHatChoke_ = 0.999f;
                    i = pending - 1;
                    continue;
                }
                break;
            }
            const float sample = renderVoice(voice, i == pending, velocity);
            if (remaining <= 0) continue;
            const float fade = remaining < fadeLength ? static_cast<float>(remaining) / fadeLength : 1.0f;
            sink(voice, i, gain * fade * sample, gl, gr);
            --remaining;
        }
        gainL_[idx] = gl;
        gainR_[idx] = gr;
        hitGain_[idx] = gain;
        randState_[idx] = ps_rand_exchange(sharedRand);
        if (pending >= 0) pending_[idx] = -1;
    }
}

}  // namespace ps
