#include "AcidTrack.h"

#include <algorithm>
#include <cmath>

// Open303 headers are not written for -Wall -Wextra and type-pun through pointers.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wall"
#pragma GCC diagnostic ignored "-Wextra"
#pragma GCC diagnostic ignored "-Wstrict-aliasing"
#include "rosic_Open303.h"
#pragma GCC diagnostic pop

namespace ps {

// Exposes Open303's note primitives. Its public noteOn() keeps a std::list of held notes,
// which allocates: the engine never calls it from the audio thread.
class AcidVoice : public rosic::Open303 {
public:
    void trigger(int key, bool accent) { triggerNote(key, accent); }
    void slide(int key, bool accent) { slideToNote(key, accent); }
    void gateOff() { ampEnv.noteOff(); }
};

int32_t AcidStep::pack() const {
    return (note & 0xF) | ((std::clamp(octave, -1, 2) + 1) << 4) | (accent ? 1 << 6 : 0) |
           (slide ? 1 << 7 : 0) | (gate ? 1 << 8 : 0);
}

AcidStep AcidStep::unpack(int32_t bits) {
    AcidStep s;
    s.note = std::clamp(bits & 0xF, 0, 12);
    s.octave = std::clamp(((bits >> 4) & 0x3) - 1, -1, 2);
    s.accent = (bits >> 6) & 1;
    s.slide = (bits >> 7) & 1;
    s.gate = (bits >> 8) & 1;
    return s;
}

AcidTrack::AcidTrack() : voice_(std::make_unique<AcidVoice>()) {
    voice_->setVolume(-6.0);
    // Musical defaults: squelchy but not screaming.
    setParam(Cutoff, 0.35f);
    setParam(Resonance, 0.7f);
    setParam(EnvMod, 0.5f);
    setParam(Decay, 0.3f);
    setParam(Accent, 0.6f);
    setParam(Tuning, 0.5f);
    setParam(Waveform, 0.0f);
    setParam(Drive, 0.3f);
    setParam(Level, 0.7f);
}

AcidTrack::~AcidTrack() = default;

void AcidTrack::prepare(int sampleRate) {
    // Keeps the sound settings; only rebuilds what depends on the sample rate.
    if (sampleRate != sampleRate_) {
        voice_->setSampleRate(sampleRate);
        sampleRate_ = sampleRate;
    }
    pending_.offset = -1;
}

void AcidTrack::setStep(int index, AcidStep step) {
    if (index >= 0 && index < kMaxSteps) steps_[static_cast<size_t>(index)] = step;
}

void AcidTrack::setLength(int steps) { length_ = std::clamp(steps, 1, kMaxSteps); }

void AcidTrack::setRoot(int midiNote) { root_ = std::clamp(midiNote, 24, 60); }

void AcidTrack::setParam(Param param, float value) {
    const double v = std::clamp(static_cast<double>(value), 0.0, 1.0);
    switch (param) {
        case Cutoff: voice_->setCutoff(314.0 * std::pow(2394.0 / 314.0, v)); break;
        case Resonance: voice_->setResonance(100.0 * v); break;
        case EnvMod: voice_->setEnvMod(100.0 * v); break;
        case Decay: voice_->setDecay(200.0 * std::pow(10.0, v)); break;
        case Accent: voice_->setAccent(100.0 * v); break;
        case Tuning: voice_->setTuning(440.0 * std::pow(2.0, (v - 0.5) * 2.0)); break;  // +/- 1 octave
        case Waveform: voice_->setWaveform(v); break;
        case Drive: drive_ = static_cast<float>(v); break;
        case Level: level_ = static_cast<float>(v); break;
        default: break;
    }
}

void AcidTrack::onStep(int frameOffset, int step, double samplesPerStep) {
    const int index = step % length_;
    const AcidStep& s = steps_[static_cast<size_t>(index)];
    if (!s.gate) {
        previousSlide_ = false;
        return;
    }
    const AcidStep& next = steps_[static_cast<size_t>((index + 1) % length_)];
    const bool holdIntoNext = s.slide && next.gate;
    pending_.offset = frameOffset;
    pending_.key = std::clamp(root_ + s.note + 12 * s.octave, 0, 127);
    pending_.accent = s.accent;
    pending_.slide = previousSlide_ && sounding_;
    // Normal notes are released after half a step, like the original gate.
    pending_.releaseAfter = holdIntoNext ? -1 : static_cast<int>(samplesPerStep * 0.5);
    previousSlide_ = holdIntoNext;
}

void AcidTrack::stop() {
    pending_.offset = -1;
    releaseCountdown_ = 1;
    previousSlide_ = false;
}

void AcidTrack::render(float* mono, int numFrames) {
    const float driveGain = 1.0f + 9.0f * drive_;
    const float makeup = 1.0f / std::tanh(driveGain);  // keeps loudness steady as drive changes
    for (int i = 0; i < numFrames; ++i) {
        if (i == pending_.offset) {
            if (pending_.slide) {
                voice_->slide(pending_.key, pending_.accent);
            } else {
                voice_->trigger(pending_.key, pending_.accent);
            }
            ++notesPlayed_;
            sounding_ = true;
            releaseCountdown_ = pending_.releaseAfter;
            pending_.offset = -1;
        }
        if (releaseCountdown_ > 0 && --releaseCountdown_ == 0) {
            voice_->gateOff();
            sounding_ = false;
        }
        const float dry = static_cast<float>(voice_->getSample());
        mono[i] += level_ * makeup * std::tanh(driveGain * dry);
    }
    if (pending_.offset >= numFrames) pending_.offset -= numFrames;
}

}  // namespace ps
