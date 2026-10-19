#pragma once

#include <algorithm>
#include <cmath>
#include <cstdint>

namespace ps {

// Sample-accurate 16th-note clock, driven from the audio callback.
//
// Steps are counted from the start of playback; negative steps are the count-in.
// The unswung grid advances by one 16th at the current tempo; every odd step is
// delayed by `swing` x one 16th. Tempo changes apply from the next step on, so
// the groove never jumps.
class Sequencer {
public:
    static constexpr int kStepsPerBeat = 4;
    static constexpr int kStepsPerBar = 16;

    void prepare(int sampleRate) { sampleRate_ = sampleRate; }

    void setTempo(double bpm) { bpm_ = std::clamp(bpm, 40.0, 300.0); }
    void setSwing(double swing) { swing_ = std::clamp(swing, 0.0, 0.75); }
    double tempo() const { return bpm_; }

    // Starts so that the first pattern step (step 0) sounds after `countInBars` bars of clicks.
    void start(int countInBars) {
        running_ = true;
        nextStep_ = -countInBars * kStepsPerBar;
        frame_ = 0;
        gridFrame_ = 0.0;
    }
    void stop() { running_ = false; }
    bool running() const { return running_; }

    double samplesPerStep() const { return sampleRate_ * 60.0 / (bpm_ * kStepsPerBeat); }

    // Calls onStep(frameOffsetInBlock, step) for every step that starts inside the next
    // `numFrames` frames, in order, then moves the clock forward.
    template <typename F>
    void advance(int numFrames, F&& onStep) {
        if (!running_) return;
        const int64_t blockEnd = frame_ + numFrames;
        while (true) {
            const double eventFrame = gridFrame_ + (isOdd(nextStep_) ? swing_ * samplesPerStep() : 0.0);
            const auto eventSample = static_cast<int64_t>(std::llround(eventFrame));
            if (eventSample >= blockEnd) break;
            const int offset = static_cast<int>(std::max<int64_t>(0, eventSample - frame_));
            onStep(offset, nextStep_);
            gridFrame_ += samplesPerStep();
            ++nextStep_;
        }
        frame_ = blockEnd;
    }

private:
    static bool isOdd(int step) { return (step & 1) != 0; }

    int sampleRate_ = 48000;
    double bpm_ = 120.0;
    double swing_ = 0.0;
    bool running_ = false;
    int nextStep_ = 0;
    int64_t frame_ = 0;
    double gridFrame_ = 0.0;
};

}  // namespace ps
