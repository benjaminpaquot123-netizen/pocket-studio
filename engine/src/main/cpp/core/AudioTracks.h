#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cstdint>
#include <memory>

#include "Sample.h"
#include "SpscQueue.h"

namespace ps {

// Audio tracks: each plays one recorded or imported clip in time with the song.
// - A scene clip starts on step `start` of the scene and comes back every `loopSteps` steps
//   (crossfaded with the end of the previous pass); loopSteps 0: once, until its scene starts again.
// - A song clip (Song = 1) starts on step `start` of the song and plays once, straight across part
//   changes; a part that switches it off only silences it (it goes on underneath). Started in the
//   middle of the song, it picks up where the song is.
// `offset` moves the clip later (> 0) or earlier (< 0) than its step, in frames. Fades in and out
// are part of the clip; a short fade removes clicks when it is stopped or cut. Real-time safe
// except where noted: never allocates or frees.
class AudioTracks {
public:
    static constexpr int kTracks = 8;
    // Fades and offset in frames.
    enum Param { Start = 0, LoopSteps = 1, Gain = 2, FadeIn = 3, FadeOut = 4, Song = 5, Offset = 6 };
    static constexpr int kMaxOffset = 14400;  // 300 ms at 48 kHz

    AudioTracks() = default;
    ~AudioTracks();
    AudioTracks(const AudioTracks&) = delete;
    AudioTracks& operator=(const AudioTracks&) = delete;

    void prepare(int sampleRate) { declick_ = std::max(1, sampleRate / 200); }
    // Takes over the caller's reference (nullptr empties the track). `refit`: the same sound at
    // another length (a loop stretched to a new tempo): a scene clip sounding picks up at the same
    // place of its loop in the new copy (the old one fades under it), instead of waiting for its
    // next pass.
    void install(int track, Sample* sample, bool refit = false);
    void setParam(int track, int param, float value);
    // A track switched off (by the song part playing) does not start again; a sounding pass fades.
    void setOn(int track, bool on);
    bool on(int track) const { return track >= 0 && track < kTracks && tracks_[static_cast<size_t>(track)].on; }
    // A sequencer step: `local` counts from the scene start, `songStep` from the song start.
    // `samplesPerStep` and `swing` (fraction of a step that odd sequencer steps are late; song step
    // 0 is sequencer step `songOrigin`) place a song clip picked up in its middle exactly where it
    // would be had it played from its start.
    void onStep(int offset, int local, int songStep, double samplesPerStep = 0.0, double swing = 0.0, int songOrigin = 0) {
        std::array<int, kTracks> locals{};
        locals.fill(local);
        onStep(offset, locals, songStep, samplesPerStep, swing, songOrigin);
    }
    // The same with each track's own `local` (the live grid: each track counts from its own launch).
    void onStep(int offset, const std::array<int, kTracks>& locals, int songStep, double samplesPerStep, double swing, int songOrigin);
    // The song position jumped (transport start, the song started over or from another part):
    // song clips stop and pick up where the song is at the next step.
    void songJump();
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
        int rise = 0;      // > 0: fading in over the remaining frames (a refit picked up mid-loop)
    };
    struct Track {
        Sample* sample = nullptr;
        int start = 0;
        int loopSteps = 16;
        float gain = 1.0f;
        int fadeIn = 0, fadeOut = 0;
        bool on = true;
        bool song = false;
        int offset = 0;
        bool resync = true;  // song clip: pick up mid-clip at the next step
        float level = 1.0f;  // song clip: 0..1 towards `on` (a part's mute without stopping it)
        Voice voice, tail;
        // The clip replaced while it sounded: kept until its fading voice is done (no click).
        Sample* held = nullptr;
    };
    void retire(Sample* s);
    void releaseHeld(Track& t);
    void play(const Track& t, Voice& v, float& l, float& r);
    void startVoice(Track& t, int offset, int64_t pos);

    std::array<Track, kTracks> tracks_{};
    SpscQueue<Sample*, 64> retired_;
    int declick_ = 240;
};

// Records the microphone into a buffer the app hands over, starting exactly on the step where the
// transport starts (or right away), until the buffer is full or recording stops.
// With a pre-roll, data[0, preroll) is a ring that keeps what came in just before the start (the
// count-in: a word sung ahead of the "1"); `prerollHead` is its oldest frame once the take starts
// (rotate before reading), and `written` counts the pre-roll too (0 until the take starts).
struct RecordBuffer {
    // Reserved, not filled (audit 13: a 300 s buffer is 57 MB): only the pages a take writes cost
    // memory. Only [0, written) is ever read, and the pre-roll ring is zeroed (a short count-in
    // leaves part of it unwritten). May throw std::bad_alloc (the caller catches it).
    struct Frames {
        explicit Frames(int64_t n) : p(new float[static_cast<size_t>(std::max<int64_t>(n, 0))]), n(static_cast<size_t>(std::max<int64_t>(n, 0))) {}
        size_t size() const { return n; }
        float* data() { return p.get(); }
        const float* data() const { return p.get(); }
        float* begin() { return p.get(); }
        float* end() { return p.get() + n; }
        float& operator[](size_t i) { return p[i]; }
        const float& operator[](size_t i) const { return p[i]; }
        std::unique_ptr<float[]> p;
        size_t n;
    };
    explicit RecordBuffer(int64_t capacity, int64_t prerollFrames = 0)
        : data(capacity + std::max<int64_t>(0, prerollFrames)), preroll(std::max<int64_t>(0, prerollFrames)) {
        std::fill(data.begin(), data.begin() + preroll, 0.0f);
    }
    Frames data;                           // mono
    const int64_t preroll;
    std::atomic<int64_t> prerollHead{0};
    std::atomic<int64_t> written{0};
    // 0 armed (waiting for the start), 1 recording, 2 finished (the engine no longer touches it).
    std::atomic<int> state{0};
    // Loudest input of the last callback (level meter while recording).
    std::atomic<float> peak{0.0f};
};

}  // namespace ps
