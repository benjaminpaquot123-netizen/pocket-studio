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
        v.low.Init(sr);
        for (auto& u : v.unison) u.Init(sr);
        for (auto* l : {&v.ladder, &v.ladderR}) {
            l->Init(sr);
            l->SetPassbandGain(0.5f);  // keeps the level up when resonance rises
            l->SetInputDrive(1.5f);
        }
        v.svf.Init(sr);
        v.svfR.Init(sr);
        v.downL.set_coefs(hiir::coef::C2x094::X2::_coef_list.data());
        v.downR.set_coefs(hiir::coef::C2x094::X2::_coef_list.data());
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
            0.5f,
            // S4: width, octave down, drive, mono below (120 Hz), sweep shape, cycle, its depth,
            // formant, vowel, mid layer, oversampling: all off.
            0.0f, 0.0f, 0.0f, kMonoBelowDefault, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f,
        };
        static_assert(sizeof(defaults) / sizeof(defaults[0]) == ParamCount, "a default for every parameter");
        for (int i = 0; i < ParamCount; ++i) params_[static_cast<size_t>(i)] = defaults[i];
    }
    prepared_ = true;
    oversampled_ = false;
    overWanted_ = param(Oversample) > 0.5f;
    setOversampled(overWanted_);
    duckGain_ = 1.0f;
    duckStep_ = 1.0f / std::max(1.0f, 0.003f * static_cast<float>(sampleRate_));
    widthNow_ = param(Width);
    layerNow_ = param(MidLayer);
    widthGlide_ = 1.0f - std::exp(-1.0f / (0.003f * static_cast<float>(sampleRate_)));
    updateShape();
    for (auto& d : drive_) d.reset();
    midDrive_.reset();
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
    // Taken at the next render, between two chunks (after a dip to silence when notes sound).
    if (p == Oversample) overWanted_ = param(Oversample) > 0.5f;
    if (p == Lfo2Shape) updateShape();
    if (p == Drive) {
        // The drive's gain, and its compensation: a sine at -15 dBFS RMS (a sound at its usual
        // level) keeps its level through the curve.
        const float g = 1.0f + 15.0f * param(Drive) * param(Drive);
        constexpr int kPoints = 64;
        float in = 0.0f, out = 0.0f;
        for (int k = 0; k < kPoints; ++k) {
            const float x = 0.25f * std::sin(6.2831853f * (static_cast<float>(k) + 0.5f) / kPoints);
            const float y = std::tanh(x * g);
            in += x * x;
            out += y * y;
        }
        driveComp_ = out > 1e-12f ? std::sqrt(in / out) : 1.0f;
    }
    for (auto& v : voices_) updateVoiceSettings(v);
}

int Synth::cycleBarsOf(float v) {
    static constexpr int kBars[kCycleChoices] = {0, 1, 2, 4, 8, 16};
    return kBars[std::clamp(static_cast<int>(std::lround(v * (kCycleChoices - 1))), 0, kCycleChoices - 1)];
}

bool Synth::plain() const {
    return param(Width) == 0.0f && param(OctaveDown) == 0.0f && param(Drive) == 0.0f && param(Lfo2Shape) == 0.0f &&
           (param(CycleDepth) == 0.0f || param(CycleBars) == 0.0f) && param(Formant) == 0.0f && param(MidLayer) == 0.0f &&
           param(Oversample) <= 0.5f;
}

void Synth::setOversampled(bool on) {
    if (on == oversampled_ && prepared_) return;
    oversampled_ = on;
    // The oscillators count their phase at twice the rate: Init (no allocation) sets it, then the
    // voices' waves and pitches are set again (the next frame's control tick does the pitches).
    const float sr = static_cast<float>(sampleRate_) * (on ? 2.0f : 1.0f);
    for (auto& v : voices_) {
        v.osc1.Init(sr);
        v.osc2.Init(sr);
        v.sub.Init(sr);
        v.sub.SetWaveform(daisysp::Oscillator::WAVE_POLYBLEP_SQUARE);
        v.low.Init(sr);
        for (auto& u : v.unison) u.Init(sr);
        v.downL.clear_buffers();
        v.downR.clear_buffers();
        v.fresh = true;
        updateVoiceSettings(v);
    }
}

void Synth::updateShape() {
    switch (std::clamp(static_cast<int>(std::lround(param(Lfo2Shape) * 3.0f)), 0, 3)) {
        case 1: lfo2_.SetWaveform(daisysp::Oscillator::WAVE_SQUARE); break;
        case 2: lfo2_.SetWaveform(daisysp::Oscillator::WAVE_SAW); break;  // DaisySP's saw falls
        case 3: lfo2_.SetWaveform(daisysp::Oscillator::WAVE_SIN); break;
        default: lfo2_.SetWaveform(daisysp::Oscillator::WAVE_TRI); break;
    }
}

void Synth::automate(Param p, float value, int offset) {
    if (p < 0 || p >= ParamCount || std::isnan(value)) return;
    queue({std::max(0, offset), 0, value, false, static_cast<int>(p)});
}

void Synth::syncCycle(int step, int offset, int stepsPerBar) {
    if (step < 0) return;
    cycleSyncStep_ = step;
    cycleSyncOffset_ = std::max(0, offset);
    stepsPerBar_ = std::max(1, stepsPerBar);
}

void Synth::updateVoiceSettings(Voice& v) {
    v.osc1.SetWaveform(waveFor(param(Osc1Wave)));
    v.low.SetWaveform(waveFor(param(Osc1Wave)));
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
    v.ladderR.SetRes(param(Resonance) * 0.95f);
    v.svfR.SetRes(param(Resonance) * 0.95f);
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
    if (e.param >= 0) {
        setParam(static_cast<Param>(e.param), e.velocity);
        return;
    }
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
    if (idle) {
        target->downL.clear_buffers();
        target->downR.clear_buffers();
    }
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

void Synth::renderLegacy(float* mono, int numFrames) {
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
    const float level = param(Level) * 1.4f * gainOf(param(Gain));  // one note around -10 dBFS (Gain 0 dB); chords rely on the master soft clip
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

// --- S4: the wide and moving synth -------------------------------------------------------------

namespace ps {

namespace {
// Vowel formants (Hz) of a low voice, A E I O U (Peterson & Barney's averages, rounded): the
// third one gives the « talk » its edge.
constexpr float kVowels[5][3] = {{730, 1090, 2440}, {530, 1840, 2480}, {270, 2290, 3010}, {570, 840, 2410}, {300, 870, 2240}};
constexpr float kFormantGains[3] = {1.0f, 0.5f, 0.25f};
constexpr float kFormantQ = 8.0f;
constexpr float kFormantMakeup = 2.5f;
constexpr float kMidLayerHz = 250.0f;
constexpr float kMidLayerDrive = 3.0f;
constexpr float kMidDelaySeconds = 0.011f;
}  // namespace

void Synth::Bandpass::set(float hz, float q, float rate) {
    g = std::tan(3.14159265f * std::min(hz, 0.45f * rate) / rate);
    k = 1.0f / q;
}

float Synth::Bandpass::band(float x) {
    // Zavalishin, « The Art of VA Filter Design », the TPT state-variable filter.
    const float a1 = 1.0f / (1.0f + g * (g + k));
    const float v1 = a1 * (s1 + g * (x - s2));
    const float v2 = s2 + g * v1;
    s1 = 2.0f * v1 - s1;
    s2 = 2.0f * v2 - s2;
    return k * v1;  // 1 at the centre
}

float Synth::Bandpass::high(float x) {
    const float a1 = 1.0f / (1.0f + g * (g + k));
    const float v1 = a1 * (s1 + g * (x - s2));
    const float v2 = s2 + g * v1;
    s1 = 2.0f * v1 - s1;
    s2 = 2.0f * v2 - s2;
    return x - k * v1 - v2;
}

void Synth::render(float* left, float* right, int numFrames) {
    const double stepFrames = std::max(1.0, beatFrames_ / 4.0);
    if (activeVoices() == 0) {
        // Nothing sounds: a switch is heard by no one, made at once.
        if (overWanted_ != oversampled_) setOversampled(overWanted_);
        duckGain_ = 1.0f;
        widthNow_ = param(Width);
        layerNow_ = param(MidLayer);
    }
    if (plain() && !postActive_ && !switching()) {
        // Every S4 setting at rest: exactly the sound from before them.
        renderLegacy(left, numFrames);
        wasWide_ = false;
    } else {
        const bool wide = stereo();
        if (wide && !wasWide_) {
            // The right channel's filters take over the left's state: no step when the width opens.
            for (auto& v : voices_) {
                v.ladderR = v.ladder;
                v.svfR = v.svf;
            }
        }
        wasWide_ = wide;
        eventCursor_ = 0;
        // The voices a chunk at a time, then the track's stages (frame indices count from the
        // call's start, as the events' offsets do).
        for (int from = 0; from < numFrames; from += kChunk) {
            const int n = std::min(kChunk, numFrames - from);
            std::fill(tmpL_.begin(), tmpL_.begin() + n, 0.0f);
            std::fill(tmpR_.begin(), tmpR_.begin() + n, 0.0f);
            if (oversampled_) {
                if (wide) renderVoices<true, true>(from, from + n);
                else renderVoices<false, true>(from, from + n);
            } else {
                if (wide) renderVoices<true, false>(from, from + n);
                else renderVoices<false, false>(from, from + n);
            }
            postProcess(tmpL_.data(), tmpR_.data(), n, wide);
            if (overWanted_ != oversampled_ || duckGain_ < 1.0f) {
                // Oversampling switched: down to silence, the switch between two chunks, back up.
                const bool down = overWanted_ != oversampled_;
                for (int i = 0; i < n; ++i) {
                    duckGain_ = down ? std::max(0.0f, duckGain_ - duckStep_) : std::min(1.0f, duckGain_ + duckStep_);
                    const float t = duckGain_;
                    const float g = t * t * t * (t * (6.0f * t - 15.0f) + 10.0f);  // smooth (even its bend) at both ends
                    tmpL_[static_cast<size_t>(i)] *= g;
                    tmpR_[static_cast<size_t>(i)] *= g;
                }
                if (down && duckGain_ <= 0.0f) setOversampled(overWanted_);
            }
            if (!wide) {
                for (int i = 0; i < n; ++i) left[from + i] += tmpL_[static_cast<size_t>(i)];
            } else if (right != nullptr) {
                for (int i = 0; i < n; ++i) {
                    left[from + i] += tmpL_[static_cast<size_t>(i)];
                    right[from + i] += tmpR_[static_cast<size_t>(i)];
                }
            } else {
                // No right channel: the mono sum, the sound without its side.
                for (int i = 0; i < n; ++i) left[from + i] += 0.5f * (tmpL_[static_cast<size_t>(i)] + tmpR_[static_cast<size_t>(i)]);
            }
        }
        postActive_ = !plain() || switching();
        // Keep events that belong to a later block.
        int kept = 0;
        for (int e = eventCursor_; e < eventCount_; ++e) {
            Event ev = events_[static_cast<size_t>(e)];
            ev.offset -= numFrames;
            events_[static_cast<size_t>(kept++)] = ev;
        }
        eventCount_ = kept;
    }
    // The song position (CycleBars) moves whatever the path: a cycle switched on mid-song starts
    // where the song is.
    if (cycleSyncStep_ >= 0 && cycleSyncOffset_ < numFrames) {
        cycleSteps_ = cycleSyncStep_ + (numFrames - cycleSyncOffset_) / stepFrames;
        cycleSyncStep_ = -1;
    } else {
        cycleSteps_ += numFrames / stepFrames;
        if (cycleSyncStep_ >= 0) cycleSyncOffset_ -= numFrames;
    }
}

template <bool Wide, bool Over>
void Synth::renderVoices(int from, int to) {
    const float detuneRatio = std::pow(2.0f, param(Osc2Detune) * 30.0f / 1200.0f);
    const float octaveRatio = std::pow(2.0f, static_cast<float>(octaveFor(param(Osc2Octave))));
    const float mix = param(OscMix);
    const float subLevel = param(SubLevel);
    const float lowLevel = param(OctaveDown);
    const float noiseLevel = param(NoiseLevel) * 0.5f;
    const int filterType = static_cast<int>(param(FilterType) * 3.999f);
    const float cutoffHz = 20.0f * std::pow(900.0f, param(Cutoff));
    const float envOctaves = param(FilterEnvAmount) * 5.0f;
    const float vibratoSemis = param(Lfo1ToPitch) * 2.0f + mod_ * 0.5f;
    const float bendRatio = std::pow(2.0f, bend_ / 12.0f);
    const float sweepOctaves = param(Lfo2ToCutoff) * 3.0f;
    const float level = param(Level) * 1.4f * gainOf(param(Gain));
    const float nyquistGuard = 0.45f * static_cast<float>(sampleRate_);
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
    const float stackGain = 1.0f / std::sqrt(1.0f + static_cast<float>(copies));
    // Width: each copy's place, from its detune (flat ones left, sharp ones right); without copies,
    // oscillator 1 left and oscillator 2 right. Added as a side signal: the mono sum is unchanged.
    // The side is made at full width and scaled by widthNow_, which glides to the setting over a
    // few milliseconds (review m3: a width turned down at once, or to 0, is no step).
    const float width = 1.0f;
    const float widthTarget = param(Width);
    std::array<float, 6> pans{};
    for (int k = 0; k < copies; ++k) pans[static_cast<size_t>(k)] = width * (cents[static_cast<size_t>(k)] < 0.0f ? -1.0f : 1.0f) *
                                                                    std::fabs(cents[static_cast<size_t>(k)]) / std::max(1e-3f, spreadCents);
    const float pan1 = copies > 0 ? 0.0f : -0.5f * width, pan2 = copies > 0 ? 0.0f : 0.5f * width;
    // The slow cycle: closed at its start, open half-way (a raised cosine), +-2 octaves at most.
    const int cycleBars = cycleBarsOf(param(CycleBars));
    const float cycleOctaves = cycleBars > 0 ? 2.0f * param(CycleDepth) : 0.0f;
    const double cycleLen = static_cast<double>(cycleBars) * stepsPerBar_;
    const double stepFrames = std::max(1.0, beatFrames_ / 4.0);
    const bool square = std::lround(param(Lfo2Shape) * 3.0f) == 1;
    constexpr int kSub = Over ? 2 : 1;

    for (int i = from; i < to; ++i) {
        while (eventCursor_ < eventCount_ && events_[static_cast<size_t>(eventCursor_)].offset <= i) {
            applyEvent(events_[static_cast<size_t>(eventCursor_++)]);
        }
        const float lfo1 = lfo1_.Process();
        float lfo2 = lfo2_.Process();
        if (square) {
            // A gate, not a click: the square's edges over about a millisecond.
            sweepSmooth_ += (lfo2 - sweepSmooth_) * 0.02f;
            lfo2 = sweepSmooth_;
        }
        sweepBuf_[static_cast<size_t>(i - from)] = lfo2;
        const bool controlTick = controlCountdown_ <= 0;
        controlCountdown_ = controlTick ? kControlRate - 1 : controlCountdown_ - 1;
        float cycle = 0.0f;
        if (controlTick && cycleOctaves > 0.0f) {
            double pos = cycleSteps_ + i / stepFrames;
            if (cycleSyncStep_ >= 0 && i >= cycleSyncOffset_) pos = cycleSyncStep_ + (i - cycleSyncOffset_) / stepFrames;
            const double phase = std::fmod(std::max(0.0, pos), cycleLen) / cycleLen;
            cycle = cycleOctaves * static_cast<float>(-std::cos(6.283185307179586 * phase));
        }
        const float n = noiseLevel > 0.0f ? noise() : 0.0f;
        unisonGain_ += (stackGain - unisonGain_) * 0.005f;
        if constexpr (Wide) {
            if (widthNow_ != widthTarget) {
                widthNow_ += (widthTarget - widthNow_) * widthGlide_;
                if (std::fabs(widthTarget - widthNow_) < 1e-5f) widthNow_ = widthTarget;
            }
        }
        float outL = 0.0f, outR = 0.0f;
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
                for (size_t k = 0; k < v.unison.size(); ++k) v.unison[k].SetFreq(hz * ratios[k]);
                v.osc2.SetFreq(hz * detuneRatio * octaveRatio);
                v.sub.SetFreq(hz * 0.5f);
                v.low.SetFreq(hz * 0.5f);
                // The cycle's value at this tick (a voice starting between ticks keeps the last one).
                if (controlTick) v.cycle = cycle;
                const float fc = std::min(nyquistGuard, cutoffHz * std::pow(2.0f, envOctaves * fenv + sweepOctaves * lfo2 + v.cycle));
                v.ladder.SetFreq(fc);
                v.svf.SetFreq(fc);
                if constexpr (Wide) {
                    v.ladderR.SetFreq(fc);
                    v.svfR.SetFreq(fc);
                }
            }
            float mid[kSub], side[kSub];
            for (int o = 0; o < kSub; ++o) {
                float osc1 = v.osc1.Process();
                float sideSum = pan1 * osc1;
                if (copies > 0) {
                    for (int k = 0; k < copies; ++k) {
                        const float c = v.unison[static_cast<size_t>(k)].Process();
                        osc1 += c;
                        if constexpr (Wide) sideSum += pans[static_cast<size_t>(k)] * c;
                    }
                    osc1 *= unisonGain_;
                    sideSum *= unisonGain_;
                } else if (unisonGain_ < 0.999f) {
                    osc1 *= unisonGain_;
                    sideSum *= unisonGain_;
                }
                const float o2 = v.osc2.Process();
                float src = (1.0f - mix) * osc1 + mix * o2 + subLevel * v.sub.Process() + n;
                if (lowLevel > 0.0f) src += lowLevel * v.low.Process();
                mid[o] = src;
                side[o] = (1.0f - mix) * sideSum + mix * pan2 * o2;
            }
            float m, sd = 0.0f;
            if constexpr (Over) {
                m = v.downL.process_sample(mid);
                if constexpr (Wide) sd = v.downR.process_sample(side);
            } else {
                m = mid[0];
                if constexpr (Wide) sd = side[0];
            }
            if constexpr (Wide) sd *= widthNow_;
            const float gain = amp * (0.3f + 0.7f * v.velocity);
            auto filter = [&](daisysp::LadderFilter& ladder, daisysp::Svf& svf, float x) {
                if (filterType == 0) return ladder.Process(x);
                svf.Process(x);
                return filterType == 1 ? svf.Low() : (filterType == 2 ? svf.High() : svf.Band());
            };
            if constexpr (Wide) {
                outL += filter(v.ladder, v.svf, m + sd) * gain;
                outR += filter(v.ladderR, v.svfR, m - sd) * gain;
            } else {
                outL += filter(v.ladder, v.svf, m) * gain;
            }
        }
        tmpL_[static_cast<size_t>(i - from)] = level * outL;
        if constexpr (Wide) tmpR_[static_cast<size_t>(i - from)] = level * outR;
    }
}

void Synth::postProcess(float* l, float* r, int n, bool wide) {
    const float rate = static_cast<float>(sampleRate_);
    // Formant: three band-passes on the vowel (moved by the sweep), mixed with the sound.
    const float formant = param(Formant);
    if (formant > 0.0f) {
        const float sweep = param(Lfo2ToCutoff);
        for (int start = 0; start < n; start += kControlRate) {
            // The vowel follows the sweep (its last value), A..U over 0..1.
            const float pos = std::clamp(param(Vowel) + sweep * sweepBuf_[static_cast<size_t>(start)], 0.0f, 1.0f) * 4.0f;
            const int a = std::min(3, static_cast<int>(pos));
            const float t = pos - static_cast<float>(a);
            for (int f = 0; f < 3; ++f) {
                const float hz = kVowels[a][f] * std::pow(kVowels[a + 1][f] / kVowels[a][f], t);
                formantL_[static_cast<size_t>(f)].set(hz, kFormantQ, rate);
                formantR_[static_cast<size_t>(f)].set(hz, kFormantQ, rate);
            }
            const int end = std::min(n, start + kControlRate);
            for (int i = start; i < end; ++i) {
                float y = 0.0f;
                for (int f = 0; f < 3; ++f) y += kFormantGains[f] * formantL_[static_cast<size_t>(f)].band(l[i]);
                l[i] += formant * (kFormantMakeup * y - l[i]);
                if (wide) {
                    float yr = 0.0f;
                    for (int f = 0; f < 3; ++f) yr += kFormantGains[f] * formantR_[static_cast<size_t>(f)].band(r[i]);
                    r[i] += formant * (kFormantMakeup * yr - r[i]);
                }
            }
        }
    }
    // Drive: tanh, 4x oversampled with first-order anti-aliasing (DriveShaper), level kept.
    const float drive = param(Drive);
    if (drive > 0.0f) {
        const float g = 1.0f + 15.0f * drive * drive;
        const SoftTanh shape;
        for (int i = 0; i < n; ++i) l[i] = drive_[0].process(l[i], g, 0.0f, shape) * driveComp_;
        if (wide) {
            for (int i = 0; i < n; ++i) r[i] = drive_[1].process(r[i], g, 0.0f, shape) * driveComp_;
        }
    }
    // The mid layer: the sound above 250 Hz, driven, added in the middle and (11 ms later) on the
    // side: wide, and gone in mono's sum only as far as its side goes.
    const float layerTarget = param(MidLayer);
    if (layerTarget > 0.0f || layerNow_ > 0.0f) {
        midHigh_.set(kMidLayerHz, 0.7071f, rate);
        const int delay = std::clamp(static_cast<int>(kMidDelaySeconds * rate), 1, static_cast<int>(midDelay_.size()) - 1);
        const SoftTanh shape;
        const float comp = 1.0f / std::tanh(kMidLayerDrive * 0.5f);
        for (int i = 0; i < n; ++i) {
            const float m = 0.5f * (l[i] + r[i]);
            const float y = midDrive_.process(midHigh_.high(m), kMidLayerDrive, 0.0f, shape) * comp * 0.5f;
            midDelay_[static_cast<size_t>(midDelayPos_)] = y;
            const float d = midDelay_[static_cast<size_t>((midDelayPos_ + static_cast<int>(midDelay_.size()) - delay) % static_cast<int>(midDelay_.size()))];
            midDelayPos_ = (midDelayPos_ + 1) % static_cast<int>(midDelay_.size());
            // (A mid layer makes the sound stereo(): `r` is the right channel here.) Its amount glides.
            if (layerNow_ != layerTarget) {
                layerNow_ += (layerTarget - layerNow_) * widthGlide_;
                if (std::fabs(layerTarget - layerNow_) < 1e-5f) layerNow_ = layerTarget;
            }
            l[i] += layerNow_ * (0.5f * y + d);
            r[i] += layerNow_ * (0.5f * y - d);
        }
    }
    // Lows in mono: the side (L - R) high-passed twice (24 dB / octave) under MonoBelow.
    if (wide) {
        const float hz = monoBelowHz(param(MonoBelow));
        for (auto& f : monoSide_) f.set(hz, 0.7071f, rate);
        for (int i = 0; i < n; ++i) {
            const float m = 0.5f * (l[i] + r[i]);
            const float s = monoSide_[1].high(monoSide_[0].high(0.5f * (l[i] - r[i])));
            l[i] = m + s;
            r[i] = m - s;
        }
    }
}

template void Synth::renderVoices<false, false>(int, int);
template void Synth::renderVoices<true, false>(int, int);
template void Synth::renderVoices<false, true>(int, int);
template void Synth::renderVoices<true, true>(int, int);

}  // namespace ps
