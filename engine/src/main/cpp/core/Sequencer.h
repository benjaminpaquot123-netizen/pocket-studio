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
    static constexpr int kStepsPerBar = 16;  // in 4/4; see stepsPerBar()

    void prepare(int sampleRate) { sampleRate_ = sampleRate; }

    // A NaN would slip through the clamp and make the step loop spin: ignored.
    void setTempo(double bpm) { if (std::isfinite(bpm)) bpm_ = std::clamp(bpm, 40.0, 300.0); }
    void setSwing(double swing) { if (std::isfinite(swing)) swing_ = std::clamp(swing, 0.0, 0.75); }
    double tempo() const { return bpm_; }
    // Time signature n/4: a bar is 4n sixteenths (count-in, bar lines, metronome accent).
    void setBeatsPerBar(int beats) { stepsPerBar_ = std::clamp(beats, 2, 7) * kStepsPerBeat; }
    int stepsPerBar() const { return stepsPerBar_; }

    // Starts so that the first pattern step (step 0) sounds after `countInBars` bars of clicks.
    void start(int countInBars) {
        running_ = true;
        nextStep_ = -countInBars * stepsPerBar_;
        frame_ = 0;
        gridFrame_ = 0.0;
        stepLength_ = samplesPerStep();
    }
    void stop() { running_ = false; }
    bool running() const { return running_; }

    double swing() const { return swing_; }
    double samplesPerStep() const { return sampleRate_ * 60.0 / (bpm_ * kStepsPerBeat); }
    // Where the transport is at the next block's first frame, in steps of the unswung grid (counted
    // from step 0; negative in the count-in), with a fraction: the step in progress keeps the length
    // it started with when the tempo changes, as advance() counts it. Running only.
    double position() const { return nextStep_ - (gridFrame_ - static_cast<double>(frame_)) / stepLength_; }

    // Calls onStep(frameOffsetInBlock, step) for every step that starts inside the next
    // `numFrames` frames, in order (until one of them stops the clock), then moves the clock forward.
    template <typename F>
    void advance(int numFrames, F&& onStep) {
        if (!running_) return;
        const int64_t blockEnd = frame_ + numFrames;
        while (running_) {  // a step may stop the clock (the song's end): no step after it
            const double eventFrame = gridFrame_ + (isOdd(nextStep_) ? swing_ * samplesPerStep() : 0.0);
            const auto eventSample = static_cast<int64_t>(std::llround(eventFrame));
            if (eventSample >= blockEnd) break;
            const int offset = static_cast<int>(std::max<int64_t>(0, eventSample - frame_));
            onStep(offset, nextStep_);
            stepLength_ = samplesPerStep();
            gridFrame_ += stepLength_;
            ++nextStep_;
        }
        frame_ = blockEnd;
    }

private:
    static bool isOdd(int step) { return (step & 1) != 0; }

    int sampleRate_ = 48000;
    int stepsPerBar_ = kStepsPerBar;
    double bpm_ = 120.0;
    double swing_ = 0.0;
    bool running_ = false;
    int nextStep_ = 0;
    int64_t frame_ = 0;
    double gridFrame_ = 0.0;
    double stepLength_ = 6000.0;  // frames of the step in progress (ending on gridFrame_)
};

}  // namespace ps
