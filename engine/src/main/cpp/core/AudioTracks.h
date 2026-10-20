#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cstdint>

#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

// Audio tracks: each plays one recorded or imported clip in time with the song. The clip starts
// on step `start` of the scene and comes back every `loopSteps` steps (crossfaded with the end
// of the previous pass). Fades in and out are part of the clip; a short fade removes clicks
// when it is stopped or cut. Real-time safe except where noted: never allocates or frees.
class AudioTracks {
public:
    static constexpr int kTracks = 4;
    enum Param { Start = 0, LoopSteps = 1, Gain = 2, FadeIn = 3, FadeOut = 4 };  // fades in frames

    AudioTracks() = default;
    ~AudioTracks();
    AudioTracks(const AudioTracks&) = delete;
    AudioTracks& operator=(const AudioTracks&) = delete;

    void prepare(int sampleRate) { declick_ = std::max(1, sampleRate / 200); }
    // Takes over the caller's reference (nullptr empties the track).
    void install(int track, Sample* sample);
    void setParam(int track, int param, float value);
    // A sequencer step of the playing scene (`local` counts from the scene start).
    void onStep(int offset, int local);
    // Transport stop: every clip fades out.
    void stop();
    // Adds each track into its own stereo pair.
    void render(float* const* lefts, float* const* rights, int numFrames);
    bool has(int track) const { return tracks_[static_cast<size_t>(track)].sample != nullptr; }
    bool playing(int track) const { return tracks_[static_cast<size_t>(track)].voice.sample != nullptr; }  // tests

    // Not the audio thread: frees samples released by the audio thread.
    void collectGarbage();

private:
    struct Voice {
        const Sample* sample = nullptr;
        int64_t pos = 0;
        int64_t due = 0;   // frames before it starts (inside the current block)
        int fade = 0;      // > 0: fading out over the remaining frames
    };
    struct Track {
        Sample* sample = nullptr;
        int start = 0;
        int loopSteps = 16;
        float gain = 1.0f;
        int fadeIn = 0, fadeOut = 0;
        Voice voice, tail;
    };
    void retire(Sample* s);
    void play(const Track& t, Voice& v, float& l, float& r);

    std::array<Track, kTracks> tracks_{};
    SpscQueue<Sample*, 64> retired_;
    int declick_ = 240;
};

// Records the microphone into a buffer the app hands over, starting exactly on the step where the
// transport starts (or right away), until the buffer is full or recording stops.
struct RecordBuffer {
    explicit RecordBuffer(int64_t capacity) : data(static_cast<size_t>(capacity), 0.0f) {}
    std::vector<float> data;               // mono
    std::atomic<int64_t> written{0};
    // 0 armed (waiting for the start), 1 recording, 2 finished (the engine no longer touches it).
    std::atomic<int> state{0};
    // Loudest input of the last callback (level meter while recording).
    std::atomic<float> peak{0.0f};
};

}  // namespace ps
