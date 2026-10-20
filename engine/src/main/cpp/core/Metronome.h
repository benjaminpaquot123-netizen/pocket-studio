#pragma once

#include <cmath>

namespace ps {

// Short sine "tick" with an exponential decay; higher pitch on the first beat of a bar.
class Metronome {
public:
    void prepare(int sampleRate) {
        sampleRate_ = sampleRate;
        decay_ = std::exp(-1.0f / (0.025f * sampleRate));  // ~25 ms
    }

    void trigger(bool downbeat, int frameOffset) {
        pendingOffset_ = frameOffset;
        pendingFrequency_ = downbeat ? 1600.0f : 1000.0f;
        pendingGain_ = (downbeat ? 0.5f : 0.35f) * level_ * 2.0f;
    }

    // 0..1, 0.5 = the original click level.
    void setLevel(float level) { level_ = level < 0.0f ? 0.0f : (level > 1.0f ? 1.0f : level); }

    // Adds the click into a mono buffer of `numFrames` frames.
    void render(float* mono, int numFrames) {
        for (int i = 0; i < numFrames; ++i) {
            if (i == pendingOffset_) {
                phase_ = 0.0f;
                frequency_ = pendingFrequency_;
                envelope_ = pendingGain_;
                pendingOffset_ = -1;
            }
            if (envelope_ < 1e-5f) continue;
            mono[i] += envelope_ * std::sin(phase_);
            phase_ += 6.2831853f * frequency_ / sampleRate_;
            if (phase_ > 6.2831853f) phase_ -= 6.2831853f;
            envelope_ *= decay_;
        }
        if (pendingOffset_ >= numFrames) pendingOffset_ -= numFrames;
    }

private:
    int sampleRate_ = 48000;
    float decay_ = 0.999f;
    float phase_ = 0.0f;
    float frequency_ = 1000.0f;
    float envelope_ = 0.0f;
    int pendingOffset_ = -1;
    float pendingFrequency_ = 1000.0f;
    float pendingGain_ = 0.0f;
    float level_ = 0.5f;
};

}  // namespace ps
