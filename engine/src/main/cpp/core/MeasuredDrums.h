#pragma once

#include <cmath>
#include <cstdint>

namespace ps::measured {

// « Batterie du matin » (docs/sons/matin/BATTERIE.md): drum voices whose shape follows numbers
// measured on reference hits, per family and style (docs/sons/matin/CIBLES-BATTERIE.md): the
// fundamental note, its pitch drop (start Hz, half-time), the length (time to -20 and -40 dB under
// the peak: a hold, then a fall straight in dB), the noise share and the brightness (band of the
// noise). Listening material only: no pad, style or project plays them yet.
// trigger() and render() are real-time safe: no allocation, no lock, no log (plain POD state).

enum Family : int { Kick = 0, Bass808, Clap, Snare, ClosedHat, OpenHat, Rim, Tom, Crash, kFamilies };
enum Style : int { House = 0, TechHouse, Techno, Trap, Dnb, Dubstep, kStyles };

struct Recipe {
    // Tonal body: a sine from startHz down to endHz, the gap halving every dropHalfMs.
    float bodyLevel = 0.0f;
    float startHz = 0.0f, endHz = 0.0f, dropHalfMs = 10.0f;
    float bodyHoldMs = 0.0f, bodyFallMs = 100.0f;  // flat, then -40 dB over fallMs (straight in dB)
    float bodyDrive = 0.0f;                        // 0: clean; > 0 tanh saturation (harmonics)
    // Noise (and the hats' metal): white noise through a 2-pole high-pass and low-pass.
    float noiseLevel = 0.0f;
    float noiseHpHz = 1000.0f, noiseLpHz = 8000.0f;
    float noiseHoldMs = 0.0f, noiseFallMs = 100.0f;
    float metalLevel = 0.0f;  // six square waves at the 808 cymbal ratios, under the noise filters
    float metalHz = 0.0f;     // the lowest of them
    int bursts = 0;           // clap: hand claps before the tail, burstGapMs apart
    float burstGapMs = 9.0f;
    // Attack click: a short burst of high noise.
    float clickLevel = 0.0f, clickMs = 3.0f, clickHpHz = 2000.0f;
    float gain = 1.0f;
};

// The recipe of a family in a style (the numbers come from CIBLES-BATTERIE.md; the reasons from BATTERIE.md).
Recipe recipe(int family, int style);

// Listening variants (bench only, scripts/son/matin_batterie.sh; nothing in the app plays them yet).
// which: -1 low and long, 0 the recipe above, +1 dry and short. Kick: the style's 25th / 75th
// percentiles of length and note; clap, snare, closed hat: the same voice longer and darker / shorter
// and brighter. Any other family: the recipe itself.
Recipe variant(int family, int style, int which);
// A pure kick for deep / techno (not industrial): a clean round body that hits hard.
Recipe deepTechnoKick();

// One voice (monophonic: a new hit restarts it).
class Voice {
public:
    void prepare(int sampleRate) { rate_ = static_cast<float>(sampleRate > 0 ? sampleRate : 48000); }
    void setRecipe(const Recipe& r) { r_ = r; }
    const Recipe& recipeInUse() const { return r_; }

    void trigger(float velocity) {
        vel_ = velocity;
        t_ = 0;
        phase_ = 0.0f;
        burst_ = 0;
        for (auto& f : filt_) f = {};
        for (auto& p : metalPhase_) p = 0.0f;
        active_ = true;
        const float ms = rate_ * 0.001f;
        bodyHold_ = static_cast<int>(r_.bodyHoldMs * ms);
        noiseHold_ = static_cast<int>(r_.noiseHoldMs * ms);
        bodyMul_ = fallMul(r_.bodyFallMs * ms);
        noiseMul_ = fallMul(r_.noiseFallMs * ms);
        clickMul_ = fallMul(r_.clickMs * ms * 4.0f);  // -40 dB over 4 x clickMs (about -10 dB at clickMs)
        bodyEnv_ = 1.0f;
        clickEnv_ = r_.clickLevel > 0.0f ? 1.0f : 0.0f;  // no click: nothing to wait for
        noiseEnv_ = r_.bursts > 0 ? 0.5f : 1.0f;  // a clap's tail starts under its hands
        dropMul_ = r_.dropHalfMs > 0.0f ? std::exp(-0.69314718f / (r_.dropHalfMs * ms)) : 0.0f;
        dropGap_ = r_.startHz - r_.endHz;
        burstLen_ = static_cast<int>(r_.burstGapMs * ms);
        tailStart_ = r_.bursts > 0 ? r_.bursts * burstLen_ : 0;
        hpG_ = tanG(r_.noiseHpHz);
        lpG_ = tanG(r_.noiseLpHz);
        clickG_ = tanG(r_.clickHpHz);
        driveNorm_ = r_.bodyDrive > 0.0f ? 1.0f / std::tanh(1.0f + 4.0f * r_.bodyDrive) : 1.0f;
    }

    bool active() const { return active_; }

    // Adds the voice into `out` (mono).
    void render(float* out, int numFrames) {
        if (!active_) return;
        const float twoPi = 6.28318531f;
        for (int i = 0; i < numFrames; ++i) {
            float s = 0.0f;
            // Body.
            if (r_.bodyLevel > 0.0f) {
                const float hz = r_.endHz + dropGap_;
                dropGap_ *= dropMul_;
                if (std::fabs(dropGap_) < 1e-3f) dropGap_ = 0.0f;  // no subnormals once the drop is done
                phase_ += hz / rate_;
                phase_ -= std::floor(phase_);
                float b = std::sin(twoPi * phase_);
                if (r_.bodyDrive > 0.0f) b = std::tanh(b * (1.0f + 4.0f * r_.bodyDrive)) * driveNorm_;
                s += r_.bodyLevel * b * bodyEnv_;
                if (t_ >= bodyHold_) bodyEnv_ *= bodyMul_;
            }
            // Noise and metal, band-limited.
            if (r_.noiseLevel > 0.0f || r_.metalLevel > 0.0f) {
                float n = r_.noiseLevel * white();
                if (r_.metalLevel > 0.0f) {
                    static constexpr float kRatios[6] = {1.0f, 1.4826f, 1.8007f, 2.5462f, 2.6303f, 3.8968f};
                    float m = 0.0f;
                    for (int k = 0; k < 6; ++k) {
                        metalPhase_[k] += r_.metalHz * kRatios[k] / rate_;
                        metalPhase_[k] -= std::floor(metalPhase_[k]);
                        m += metalPhase_[k] < 0.5f ? 1.0f : -1.0f;
                    }
                    n += r_.metalLevel * m * (1.0f / 6.0f);
                }
                n = filt_[0].hp(n, hpG_);
                n = filt_[1].hp(n, hpG_);
                n = filt_[2].lp(n, lpG_);
                n = filt_[3].lp(n, lpG_);
                float env = noiseEnv_;
                if (t_ < tailStart_) {
                    // A clap's hands: each burst snaps up and falls fast (about 12 dB a burst gap).
                    const int inBurst = t_ - burst_ * burstLen_;
                    if (inBurst >= burstLen_) ++burst_;
                    const float x = static_cast<float>(t_ - burst_ * burstLen_) / static_cast<float>(burstLen_ > 0 ? burstLen_ : 1);
                    env = std::exp(-2.8f * x);
                } else if (t_ - tailStart_ >= noiseHold_) {
                    noiseEnv_ *= noiseMul_;
                }
                s += n * env;
            }
            // Click.
            if (r_.clickLevel > 0.0f && clickEnv_ > 1e-4f) {
                s += r_.clickLevel * filt_[4].hp(white(), clickG_) * clickEnv_;
                clickEnv_ *= clickMul_;
            }
            out[i] += r_.gain * vel_ * s;
            ++t_;
        }
        const bool bodyDone = r_.bodyLevel <= 0.0f || (t_ > bodyHold_ && bodyEnv_ < 1e-5f);
        const bool noiseDone = (r_.noiseLevel <= 0.0f && r_.metalLevel <= 0.0f) || (t_ > tailStart_ + noiseHold_ && noiseEnv_ < 1e-5f);
        if (bodyDone && noiseDone && clickEnv_ < 1e-4f) active_ = false;
    }

private:
    // Cytomic's trapezoidal one-pole pair (a 2-pole state-variable section split in two 1-poles).
    struct OnePole {
        float z = 0.0f;
        float lp(float x, float g) {
            const float v = (x - z) * g / (1.0f + g);
            const float y = v + z;
            z = y + v;
            return y;
        }
        float hp(float x, float g) { return x - lp(x, g); }
    };
    float tanG(float hz) const {
        const float f = std::fmin(hz, 0.45f * rate_);
        return std::tan(3.14159265f * f / rate_);
    }
    static float fallMul(float frames) { return frames > 1.0f ? std::pow(10.0f, -2.0f / frames) : 0.0f; }
    float white() {
        noise_ ^= noise_ << 13;
        noise_ ^= noise_ >> 17;
        noise_ ^= noise_ << 5;
        return static_cast<float>(static_cast<int32_t>(noise_)) * (1.0f / 2147483648.0f);
    }

    Recipe r_{};
    float rate_ = 48000.0f;
    float vel_ = 1.0f;
    int t_ = 0;
    bool active_ = false;
    float phase_ = 0.0f;
    float dropGap_ = 0.0f, dropMul_ = 0.0f;
    int bodyHold_ = 0, noiseHold_ = 0;
    float bodyEnv_ = 0.0f, noiseEnv_ = 0.0f, clickEnv_ = 0.0f;
    float bodyMul_ = 0.0f, noiseMul_ = 0.0f, clickMul_ = 0.0f;
    float hpG_ = 0.0f, lpG_ = 0.0f, clickG_ = 0.0f;
    float driveNorm_ = 1.0f;
    int burst_ = 0, burstLen_ = 0, tailStart_ = 0;
    float metalPhase_[6] = {};
    OnePole filt_[5];
    uint32_t noise_ = 0x9E3779B9u;
};

}  // namespace ps::measured
