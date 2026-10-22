#include "Synth.h"
#include "Params.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {
constexpr int kControlRate = 16;  // filter and pitch modulation updated every 16 samples

float envTime(float v) { return 0.001f * std::pow(5000.0f, v); }  // 1 ms .. 5 s
float midiToHz(int note) { return 440.0f * std::pow(2.0f, (note - 69) / 12.0f); }
uint8_t waveFor(float v) {
    switch (static_cast<int>(v * 2.999f)) {
        case 1: return daisysp::Oscillator::WAVE_POLYBLEP_SQUARE;
        case 2: return daisysp::Oscillator::WAVE_POLYBLEP_TRI;
        default: return daisysp::Oscillator::WAVE_POLYBLEP_SAW;
    }
}
int octaveFor(float v) { return v < 0.33f ? -1 : (v < 0.66f ? 0 : 1); }
}  // namespace

void Synth::prepare(int sampleRate) {
    if (prepared_ && sampleRate == sampleRate_) return;
    sampleRate_ = sampleRate;
    const float sr = static_cast<float>(sampleRate);
    for (auto& v : voices_) {
        v.osc1.Init(sr);
        v.osc2.Init(sr);
        v.sub.Init(sr);
        v.sub.SetWaveform(daisysp::Oscillator::WAVE_POLYBLEP_SQUARE);
        for (auto& u : v.unison) u.Init(sr);
        v.ladder.Init(sr);
        v.ladder.SetPassbandGain(0.5f);  // keeps the level up when resonance rises
        v.ladder.SetInputDrive(1.5f);
        v.svf.Init(sr);
        v.ampEnv.Init(sr);
        v.filterEnv.Init(sr);
        v.note = -1;
        v.gate = false;
    }
    lfo1_.Init(sr);
    lfo1_.SetWaveform(daisysp::Oscillator::WAVE_TRI);
    lfo2_.Init(sr);
    lfo2_.SetWaveform(daisysp::Oscillator::WAVE_TRI);
    if (!prepared_) {
        // A warm, playable default patch.
        const float defaults[ParamCount] = {
            0.0f, 0.0f, 0.3f, 0.5f, 0.5f, 0.2f, 0.0f,
            0.0f, 0.55f, 0.3f, 0.4f,
            0.0f, 0.3f, 0.3f, 0.3f,
            0.05f, 0.3f, 0.8f, 0.35f,
            0.5f, 0.0f, 0.3f, 0.0f, 0.6f,
            0.0f, 0.3f,
            0.0f, 0.0f,
        };
        for (int i = 0; i < ParamCount; ++i) params_[static_cast<size_t>(i)] = defaults[i];
    }
    prepared_ = true;
    for (auto& v : voices_) updateVoiceSettings(v);
    lfo1_.SetFreq(0.05f * std::pow(400.0f, param(Lfo1Rate)));
    sweepHz_ = 0.0f;
    updateSweepRate();
}

float Synth::syncCyclesPerBeat(float sync) {
    // Free, then a bar, a half, a quarter, an eighth, a sixteenth, quarter and eighth triplets.
    static constexpr float kCycles[kSyncDivisions] = {0.0f, 0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 1.5f, 3.0f};
    const int i = std::clamp(static_cast<int>(std::lround(sync * static_cast<float>(kSyncDivisions - 1))), 0, kSyncDivisions - 1);
    return kCycles[i];
}

void Synth::updateSweepRate() {
    const float cycles = syncCyclesPerBeat(param(Lfo2Sync));
    const float hz = cycles > 0.0f ? cycles * static_cast<float>(sampleRate_ / beatFrames_)
                                   : 0.05f * std::pow(400.0f, param(Lfo2Rate));
    if (hz == sweepHz_) return;
    sweepHz_ = hz;
    lfo2_.SetFreq(hz);
}

void Synth::setBeatFrames(double frames) {
    if (!(frames > 1.0) || std::fabs(frames - beatFrames_) < 0.5) return;
    beatFrames_ = frames;
    if (param(Lfo2Sync) > 0.0f) updateSweepRate();
}

void Synth::setParam(Param p, float value) {
    if (p < 0 || p >= ParamCount || !clampSetting(params_[static_cast<size_t>(p)], value, 0.0f, 1.0f)) return;
    if (p == Lfo1Rate) lfo1_.SetFreq(0.05f * std::pow(400.0f, param(Lfo1Rate)));
    if (p == Lfo2Rate || p == Lfo2Sync) updateSweepRate();
    for (auto& v : voices_) updateVoiceSettings(v);
}

void Synth::updateVoiceSettings(Voice& v) {
    v.osc1.SetWaveform(waveFor(param(Osc1Wave)));
    for (auto& u : v.unison) u.SetWaveform(waveFor(param(Osc1Wave)));
    v.osc2.SetWaveform(waveFor(param(Osc2Wave)));
    v.ampEnv.SetAttackTime(envTime(param(AmpAttack)));
    v.ampEnv.SetDecayTime(envTime(param(AmpDecay)));
    v.ampEnv.SetSustainLevel(param(AmpSustain));
    v.ampEnv.SetReleaseTime(envTime(param(AmpRelease)));
    v.filterEnv.SetAttackTime(envTime(param(FilterAttack)));
    v.filterEnv.SetDecayTime(envTime(param(FilterDecay)));
    v.filterEnv.SetSustainLevel(param(FilterSustain));
    v.filterEnv.SetReleaseTime(envTime(param(FilterRelease)));
    v.ladder.SetRes(param(Resonance) * 0.95f);
    v.svf.SetRes(param(Resonance) * 0.95f);
}

void Synth::queue(const Event& e) {
    if (eventCount_ >= static_cast<int>(events_.size())) return;  // burst of events: drop extras
    // Keep the queue sorted by offset (stable): notes from the piano roll and the keyboard mix.
    int i = eventCount_++;
    while (i > 0 && events_[static_cast<size_t>(i - 1)].offset > e.offset) {
        events_[static_cast<size_t>(i)] = events_[static_cast<size_t>(i - 1)];
        --i;
    }
    events_[static_cast<size_t>(i)] = e;
}

void Synth::noteOn(int midiNote, float velocity, int frameOffset) {
    queue({std::max(0, frameOffset), std::clamp(midiNote, 0, 127), std::clamp(velocity, 0.0f, 1.0f), true});
}

void Synth::noteOff(int midiNote, int frameOffset) {
    queue({std::max(0, frameOffset), midiNote, 0.0f, false});
}

void Synth::allNotesOff() {
    eventCount_ = 0;
    for (auto& v : voices_) v.gate = false;
}

void Synth::applyEvent(const Event& e) {
    if (!e.on) {
        for (auto& v : voices_) {
            if (v.note == e.note && v.gate) v.gate = false;
        }
        return;
    }
    // Glide: a key pressed while another is held takes over its voice and slides to the new
    // pitch, without a new attack (the 808 or "talking" bass slide of a line whose notes overlap).
    if (param(Glide) > 0.0f) {
        Voice* held = nullptr;
        for (auto& v : voices_) {
            if (v.gate && (held == nullptr || v.age > held->age)) held = &v;
        }
        if (held != nullptr) {
            const float seconds = 0.5f * param(Glide) * param(Glide);
            const float ticks = std::max(1.0f, seconds * static_cast<float>(sampleRate_) / static_cast<float>(kControlRate));
            held->note = e.note;
            held->target = static_cast<float>(e.note);
            held->glideStep = (held->target - held->semis) / ticks;
            held->age = ++clock_;
            return;
        }
    }
    // Same note already sounding: retrigger it. Otherwise a free voice, else the quietest one
    // already let go, else the oldest held one (a held chord outlives quick notes fading out).
    Voice* target = nullptr;
    for (auto& v : voices_) {
        if (v.note == e.note) target = &v;
    }
    if (target == nullptr) {
        for (auto& v : voices_) {
            if (!v.gate && !v.ampEnv.IsRunning()) {
                target = &v;
                break;
            }
        }
    }
    if (target == nullptr) {
        for (auto& v : voices_) {
            if (!v.gate && (target == nullptr || v.level < target->level)) target = &v;
        }
    }
    if (target == nullptr) {
        target = &voices_[0];
        for (auto& v : voices_) {
            if (v.age < target->age) target = &v;
        }
    }
    // A voice still sounding (same key again, stolen) keeps its phases: a jump there would click.
    const bool idle = !target->gate && !target->ampEnv.IsRunning();
    target->note = e.note;
    target->velocity = e.velocity;
    target->gate = true;
    target->age = ++clock_;
    target->baseHz = midiToHz(e.note);
    target->semis = target->target = static_cast<float>(e.note);
    target->glideStep = 0.0f;
    target->fresh = true;
    // A tempo-locked sweep starts each attack from its lowest point: every note "wobbles" the same way.
    if (param(Lfo2Sync) > 0.0f) lfo2_.Reset(0.5f);
    if (idle && param(Unison) > 0.0f) {
        // Unison voices start at spread phases, oscillator 1 included: wide from the first cycle,
        // the same level for every note.
        target->osc1.Reset(0.0f);
        for (size_t k = 0; k < target->unison.size(); ++k) target->unison[k].Reset(0.137f * static_cast<float>(k + 1));
    }
    target->ampEnv.Retrigger(false);
    target->filterEnv.Retrigger(false);
}

float Synth::noise() {
    noiseState_ ^= noiseState_ << 13;
    noiseState_ ^= noiseState_ >> 17;
    noiseState_ ^= noiseState_ << 5;
    return static_cast<float>(noiseState_) * (2.0f / 4294967296.0f) - 1.0f;
}

void Synth::render(float* mono, int numFrames) {
    const float detuneRatio = std::pow(2.0f, param(Osc2Detune) * 30.0f / 1200.0f);
    const float octaveRatio = std::pow(2.0f, static_cast<float>(octaveFor(param(Osc2Octave))));
    const float mix = param(OscMix);
    const float subLevel = param(SubLevel);
    const float noiseLevel = param(NoiseLevel) * 0.5f;
    const int filterType = static_cast<int>(param(FilterType) * 3.999f);
    const float cutoffHz = 20.0f * std::pow(900.0f, param(Cutoff));
    const float envOctaves = param(FilterEnvAmount) * 5.0f;
    // The mod wheel adds up to half a semitone of vibrato (at least a 5 Hz wobble).
    const float vibratoSemis = param(Lfo1ToPitch) * 2.0f + mod_ * 0.5f;
    const float bendRatio = std::pow(2.0f, bend_ / 12.0f);
    const float sweepOctaves = param(Lfo2ToCutoff) * 3.0f;
    const float level = param(Level) * 1.4f;  // one note around -10 dBFS; chords rely on the master soft clip
    const float nyquistGuard = 0.45f * static_cast<float>(sampleRate_);
    // Unison: oscillator 1 and up to six copies spread evenly on both sides, the whole stack
    // centred on the note (an odd number of copies would otherwise pull it flat). A few cents of
    // spread at least: copies at exactly the same pitch only colour the sound.
    const int copies = std::clamp(static_cast<int>(std::lround(param(Unison) * 6.0f)), 0, 6);
    const float spreadCents = copies > 0 ? 3.0f + param(UnisonDetune) * 47.0f : 0.0f;
    std::array<float, 6> cents{};
    float mean = 0.0f;
    for (int k = 0; k < copies; ++k) {
        const float pos = static_cast<float>(k / 2 + 1) / static_cast<float>((copies + 1) / 2);
        cents[static_cast<size_t>(k)] = (k % 2 == 0 ? -pos : pos) * spreadCents;
        mean += cents[static_cast<size_t>(k)];
    }
    mean /= static_cast<float>(copies + 1);
    const float osc1Ratio = copies > 0 ? std::pow(2.0f, -mean / 1200.0f) : 1.0f;
    std::array<float, 6> ratios{};
    for (size_t k = 0; k < ratios.size(); ++k) ratios[k] = std::pow(2.0f, (cents[k] - mean) / 1200.0f);
    // About the same loudness whatever the number of copies (they add up like noise, not in phase).
    const float stackGain = 1.0f / std::sqrt(1.0f + static_cast<float>(copies));

    int nextEvent = 0;
    // Events are kept sorted by offset (queue()).
    for (int i = 0; i < numFrames; ++i) {
        while (nextEvent < eventCount_ && events_[static_cast<size_t>(nextEvent)].offset <= i) {
            applyEvent(events_[static_cast<size_t>(nextEvent++)]);
        }
        const float lfo1 = lfo1_.Process();
        const float lfo2 = lfo2_.Process();
        const bool controlTick = controlCountdown_ <= 0;
        controlCountdown_ = controlTick ? kControlRate - 1 : controlCountdown_ - 1;
        const float n = noiseLevel > 0.0f ? noise() : 0.0f;
        // A change of the number of copies fades in over a few milliseconds instead of a step.
        unisonGain_ += (stackGain - unisonGain_) * 0.005f;
        float out = 0.0f;
        for (auto& v : voices_) {
            if (!v.gate && !v.ampEnv.IsRunning()) continue;
            const float amp = v.ampEnv.Process(v.gate);
            v.level = amp;
            const float fenv = v.filterEnv.Process(v.gate);
            if (controlTick || v.fresh) {
                v.fresh = false;
                if (controlTick && v.glideStep != 0.0f) {
                    v.semis += v.glideStep;
                    if ((v.glideStep > 0.0f && v.semis >= v.target) || (v.glideStep < 0.0f && v.semis <= v.target)) {
                        v.semis = v.target;
                        v.glideStep = 0.0f;
                    }
                    v.baseHz = 440.0f * std::pow(2.0f, (v.semis - 69.0f) / 12.0f);
                }
                const float hz = v.baseHz * bendRatio * std::pow(2.0f, vibratoSemis * lfo1 / 12.0f);
                v.osc1.SetFreq(hz * osc1Ratio);
                // All six follow the note, so a copy switched on mid-note starts at the right pitch.
                for (size_t k = 0; k < v.unison.size(); ++k) v.unison[k].SetFreq(hz * ratios[k]);
                v.osc2.SetFreq(hz * detuneRatio * octaveRatio);
                v.sub.SetFreq(hz * 0.5f);
                const float fc = std::min(nyquistGuard, cutoffHz * std::pow(2.0f, envOctaves * fenv + sweepOctaves * lfo2));
                v.ladder.SetFreq(fc);
                v.svf.SetFreq(fc);
            }
            float osc1 = v.osc1.Process();
            if (copies > 0) {
                for (int k = 0; k < copies; ++k) osc1 += v.unison[static_cast<size_t>(k)].Process();
                osc1 *= unisonGain_;
            } else if (unisonGain_ < 0.999f) {
                osc1 *= unisonGain_;  // the last copies just went: back to full level smoothly
            }
            const float source = (1.0f - mix) * osc1 + mix * v.osc2.Process() + subLevel * v.sub.Process() + n;
            float filtered;
            if (filterType == 0) {
                filtered = v.ladder.Process(source);
            } else {
                v.svf.Process(source);
                filtered = filterType == 1 ? v.svf.Low() : (filterType == 2 ? v.svf.High() : v.svf.Band());
            }
            out += filtered * amp * (0.3f + 0.7f * v.velocity);
        }
        mono[i] += level * out;
    }
    // Keep events that belong to a later block.
    int kept = 0;
    for (int e = nextEvent; e < eventCount_; ++e) {
        Event ev = events_[static_cast<size_t>(e)];
        ev.offset -= numFrames;
        events_[static_cast<size_t>(kept++)] = ev;
    }
    eventCount_ = kept;
}

int Synth::activeVoices() const {
    int n = 0;
    for (const auto& v : voices_) n += (v.gate || v.ampEnv.IsRunning()) ? 1 : 0;
    return n;
}

}  // namespace ps
