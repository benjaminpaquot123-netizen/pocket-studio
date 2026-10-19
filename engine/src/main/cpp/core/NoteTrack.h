#pragma once

#include <array>
#include <cstdint>


namespace ps {

// The synth's piano-roll pattern, played sample-accurately by the sequencer.
// Positions are in ticks: 24 per sixteenth (96 per beat). Real-time safe: fixed storage.
class NoteTrack {
public:
    static constexpr int kTicksPerStep = 24;
    static constexpr int kMaxNotes = 512;
    static constexpr int kMaxSteps = 128;  // 8 bars

    struct Note {
        int start = 0;   // ticks
        int length = 1;  // ticks
        int pitch = 60;
        float velocity = 0.0f;
    };

    // b packs start (12 bits) | length (12 bits) << 12 | pitch (7 bits) << 24.
    static int32_t pack(int start, int length, int pitch) {
        return (start & 0xFFF) | ((length & 0xFFF) << 12) | ((pitch & 0x7F) << 24);
    }
    void setNote(int index, int32_t packed, float velocity);
    void setCount(int count);
    void setSteps(int steps);
    int steps() const { return steps_; }

    // Called for every sequencer step (absolute index): queues the notes starting in it.
    void onStep(int offset, int step, double samplesPerStep);
    // Sends this block's note-ons and note-offs to `sink` (noteOn/noteOff with a frame offset),
    // before it renders.
    template <typename Sink>
    void advance(int numFrames, Sink& sink) {
        int kept = 0;
        for (int i = 0; i < pendingCount_; ++i) {
            Pending p = pending_[static_cast<size_t>(i)];
            if (p.at < numFrames) {
                if (p.velocity > 0.0f) sink.noteOn(p.pitch, p.velocity, static_cast<int>(p.at));
                else sink.noteOff(p.pitch, static_cast<int>(p.at));
            } else {
                p.at -= numFrames;
                pending_[static_cast<size_t>(kept++)] = p;
            }
        }
        pendingCount_ = kept;
    }
    // Transport stop: releases every sounding note.
    template <typename Sink>
    void stop(Sink& sink) {
        for (int i = 0; i < pendingCount_; ++i) {
            const Pending& p = pending_[static_cast<size_t>(i)];
            if (p.velocity == 0.0f) sink.noteOff(p.pitch, 0);  // a note that is sounding now
        }
        pendingCount_ = 0;
    }

    int pendingCount() const { return pendingCount_; }  // tests

private:
    struct Pending {
        int64_t at = 0;  // frames from the start of the current block
        int pitch = 0;
        float velocity = 0.0f;  // 0 = note-off
    };
    void push(int64_t at, int pitch, float velocity);

    std::array<Note, kMaxNotes> notes_{};
    int count_ = 0;
    int steps_ = 16;
    std::array<Pending, 256> pending_{};
    int pendingCount_ = 0;
};

}  // namespace ps
