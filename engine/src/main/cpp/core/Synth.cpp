#include "Synth.h"

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
        };
        for (int i = 0; i < ParamCount; ++i) params_[static_cast<size_t>(i)] = defaults[i];
    }
    prepared_ = true;
    for (auto& v : voices_) updateVoiceSettings(v);
    lfo1_.SetFreq(0.05f * std::pow(400.0f, param(Lfo1Rate)));
    lfo2_.SetFreq(0.05f * std::pow(400.0f, param(Lfo2Rate)));
}

void Synth::setParam(Param p, float value) {
    if (p < 0 || p >= ParamCount) return;
    params_[static_cast<size_t>(p)] = std::clamp(value, 0.0f, 1.0f);
    if (p == Lfo1Rate) lfo1_.SetFreq(0.05f * std::pow(400.0f, param(Lfo1Rate)));
    if (p == Lfo2Rate) lfo2_.SetFreq(0.05f * std::pow(400.0f, param(Lfo2Rate)));
    for (auto& v : voices_) updateVoiceSettings(v);
}

void Synth::updateVoiceSettings(Voice& v) {
    v.osc1.SetWaveform(waveFor(param(Osc1Wave)));
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

void Synth::noteOn(int midiNote, float velocity, int frameOffset) {
    if (eventCount_ >= static_cast<int>(events_.size())) return;  // burst of events: drop extras
    events_[static_cast<size_t>(eventCount_++)] = {std::max(0, frameOffset), std::clamp(midiNote, 0, 127),
                                                   std::clamp(velocity, 0.0f, 1.0f), true};
}

void Synth::noteOff(int midiNote, int frameOffset) {
    if (eventCount_ >= static_cast<int>(events_.size())) return;
    events_[static_cast<size_t>(eventCount_++)] = {std::max(0, frameOffset), midiNote, 0.0f, false};
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
    // Same note already sounding: retrigger it. Otherwise a free voice, else the oldest.
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
        target = &voices_[0];
        for (auto& v : voices_) {
            if (v.age < target->age) target = &v;
        }
    }
    target->note = e.note;
    target->velocity = e.velocity;
    target->gate = true;
    target->age = ++clock_;
    target->baseHz = midiToHz(e.note);
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
    const float vibratoSemis = param(Lfo1ToPitch) * 2.0f;
    const float sweepOctaves = param(Lfo2ToCutoff) * 3.0f;
    const float level = param(Level) * 1.4f;  // one note around -10 dBFS; chords rely on the master soft clip
    const float nyquistGuard = 0.45f * static_cast<float>(sampleRate_);

    int nextEvent = 0;
    // Events are queued in arrival order; their offsets are non-decreasing in practice.
    for (int i = 0; i < numFrames; ++i) {
        while (nextEvent < eventCount_ && events_[static_cast<size_t>(nextEvent)].offset <= i) {
            applyEvent(events_[static_cast<size_t>(nextEvent++)]);
        }
        const float lfo1 = lfo1_.Process();
        const float lfo2 = lfo2_.Process();
        const bool controlTick = (i % kControlRate) == 0;
        const float n = noiseLevel > 0.0f ? noise() : 0.0f;
        float out = 0.0f;
        for (auto& v : voices_) {
            if (!v.gate && !v.ampEnv.IsRunning()) continue;
            const float amp = v.ampEnv.Process(v.gate);
            const float fenv = v.filterEnv.Process(v.gate);
            if (controlTick) {
                const float hz = v.baseHz * std::pow(2.0f, vibratoSemis * lfo1 / 12.0f);
                v.osc1.SetFreq(hz);
                v.osc2.SetFreq(hz * detuneRatio * octaveRatio);
                v.sub.SetFreq(hz * 0.5f);
                const float fc = std::min(nyquistGuard, cutoffHz * std::pow(2.0f, envOctaves * fenv + sweepOctaves * lfo2));
                v.ladder.SetFreq(fc);
                v.svf.SetFreq(fc);
            }
            const float source = (1.0f - mix) * v.osc1.Process() + mix * v.osc2.Process() +
                                 subLevel * v.sub.Process() + n;
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
