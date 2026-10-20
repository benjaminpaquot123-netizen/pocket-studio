#include "DrumKit.h"

#include <algorithm>
#include <cmath>

#include "Pan.h"

namespace ps {

namespace {
// Per-voice output level, balanced by ear against the kick.
constexpr float kLevels[DrumKit::kVoices] = {
    1.0f, 1.0f, 0.7f, 0.6f, 0.7f, 0.45f, 0.4f, 0.7f,
    0.7f, 0.65f, 0.6f, 0.45f, 0.35f, 0.35f, 0.6f, 0.5f,
};
// Longest a hit may ring (seconds). Past it the voice fades out over kFadeSeconds and sleeps:
// saves CPU, and stops DaisySP's analog snare, whose noise never fully dies away.
constexpr float kMaxSeconds[DrumKit::kVoices] = {
    1.5f, 1.2f, 0.8f, 0.8f, 0.6f, 0.4f, 1.5f, 1.2f,
    1.0f, 1.0f, 0.3f, 1.0f, 2.5f, 0.4f, 0.6f, 0.8f,
};
constexpr float kFadeSeconds = 0.06f;

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

    // Kicks: low, long and punchy (Benjamin: "pas assez puissants"); see kKickDrive below.
    kick_.configure(sampleRate, 47.0f, 3.2f, 0.035f, 0.42f, 0.35f);

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
}

void DrumKit::trigger(int voice, float velocity, int frameOffset, float pan) {
    if (voice < 0 || voice >= kVoices) return;
    pending_[static_cast<size_t>(voice)] = std::max(0, frameOffset);
    velocity_[static_cast<size_t>(voice)] = std::clamp(velocity, 0.0f, 1.0f);
    pendingPan_[static_cast<size_t>(voice)] = std::isfinite(pan) ? pan : 0.0f;
}

float DrumKit::noise() {
    noiseState_ ^= noiseState_ << 13;
    noiseState_ ^= noiseState_ >> 17;
    noiseState_ ^= noiseState_ << 5;
    return static_cast<float>(noiseState_) * (2.0f / 4294967296.0f) - 1.0f;
}

float DrumKit::renderVoice(int voice, bool trig, float v) {
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
        case ClosedHat:
            if (trig) closedHat_.SetAccent(v);
            return closedHat_.Process(trig);
        case OpenHat: {
            if (trig) {
                openHat_.SetAccent(v);
                openHatChoke_ = 1.0f;
            } else if (openHatChoke_ < 1.0f) {
                openHatChoke_ *= chokeDecay_;
            }
            return openHat_.Process(trig) * openHatChoke_;
        }
        case Cymbal: if (trig) cymbal_.SetAccent(v); return cymbal_.Process(trig);
        case Clap: {
            // Three quick noise bursts 10 ms apart, then a short tail.
            if (trig) clapCounter_ = 0;
            if (clapCounter_ >= 0) {
                const int burstGap = sampleRate_ / 100;
                if (clapCounter_ == 0 || clapCounter_ == burstGap || clapCounter_ == 2 * burstGap) clapBurst_.hit(v);
                if (clapCounter_ == 2 * burstGap) clapTail_.hit(0.6f * v);
                if (++clapCounter_ > 2 * burstGap) clapCounter_ = -1;
            }
            clapFilter_.Process(noise());
            return clapFilter_.Band() * (clapBurst_.next() + clapTail_.next()) * 3.0f;
        }
        case Rim: {
            if (trig) rimEnv_.hit(v);
            rimFilter_.Process((trig ? 4.0f * v : 0.0f) + 0.05f * noise() * rimEnv_.level);
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
            shakerFilter_.Process(noise());
            return shakerFilter_.High() * shakerEnv_.next();
        }
        default: return 0.0f;
    }
}

void DrumKit::render(float* mono, int numFrames) {
    renderTo(numFrames, [mono](size_t, int i, float x) { mono[i] += x; });
}

void DrumKit::render(float* left, float* right, int numFrames) {
    renderTo(numFrames, [this, left, right](size_t voice, int i, float x) {
        left[i] += x * gainL_[voice];
        right[i] += x * gainR_[voice];
    });
}

template <typename Sink>
void DrumKit::renderTo(int numFrames, Sink&& sink) {
    const int fadeLength = static_cast<int>(kFadeSeconds * sampleRate_);
    const int closedHatHit = pending_[ClosedHat] < numFrames ? pending_[ClosedHat] : -1;
    for (int voice = 0; voice < kVoices; ++voice) {
        const auto idx = static_cast<size_t>(voice);
        const int pending = pending_[idx] < numFrames ? pending_[idx] : -1;
        if (activeFrames_[idx] <= 0 && pending < 0) continue;
        const int maxLength = static_cast<int>(kMaxSeconds[voice] * sampleRate_);
        const float level = kLevels[voice];
        const float velocity = velocity_[idx];
        const float gain = level * (0.4f + 0.6f * velocity);
        // A closed hat cuts the open one at the exact sample, like on a real machine.
        const int chokeAt = voice == OpenHat ? closedHatHit : -1;
        int& remaining = activeFrames_[idx];
        for (int i = 0; i < numFrames; ++i) {
            if (i == pending) {
                remaining = maxLength;
                panGains(pendingPan_[idx], gainL_[idx], gainR_[idx]);
            }
            if (i == chokeAt) openHatChoke_ = 0.999f;
            const float sample = renderVoice(voice, i == pending, velocity);
            if (remaining <= 0) continue;
            const float fade = remaining < fadeLength ? static_cast<float>(remaining) / fadeLength : 1.0f;
            sink(idx, i, gain * fade * sample);
            --remaining;
        }
        if (pending >= 0) pending_[idx] = -1;
    }
}

}  // namespace ps
