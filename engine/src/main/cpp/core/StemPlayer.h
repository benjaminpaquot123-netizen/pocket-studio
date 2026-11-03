#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <limits>
#include <string>
#include <vector>

#include "Sample.h"
#include "Smoother.h"
#include "SpscQueue.h"

namespace ps {

// The four stems of a separated song (drums, bass, other, vocals) as 16-bit interleaved stereo
// at the engine rate: 0.77 MB per second of song for all four at 48 kHz.
// Immutable and reference counted like Sample.
class StemBundle {
public:
    static constexpr int kStems = 4;

    // Not real-time safe: reads the WAVs (any rate, float or PCM), resamples, converts.
    // nullptr when a file is missing or unreadable. Starts with one reference.
    static StemBundle* load(const std::vector<std::string>& wavPaths, int targetRate);
    // Tests: interleaved stereo float at the target rate, one vector per stem, same length.
    // `sameSongAs`: the new bundle stands for a copy of that song at another tempo.
    static StemBundle* fromFloat(const std::vector<std::vector<float>>& stereo, const StemBundle* sameSongAs = nullptr);

    int64_t frames() const { return frames_; }
    const int16_t* stem(int i) const { return data_[static_cast<size_t>(i)].data(); }
    // A region of one stem as interleaved stereo float (for a pad). Not real-time safe.
    std::vector<float> extract(int stem, int64_t start, int64_t count) const;
    // A copy lasting `ratio` times as long, pitch kept (the song at another tempo: ratio =
    // songBpm / projectBpm). Not real-time safe: seconds of work, about 2 x 23 MB per minute
    // of song while one stem is stretched. nullptr if the song is too short. Starts with one reference.
    StemBundle* stretched(double ratio, int sampleRate) const;
    // Loudest sample of each of `buckets` equal slices of one stem, 0..1 (waveform overview).
    std::vector<float> peaks(int stem, int buckets) const;

    void retain() { refs_.fetch_add(1, std::memory_order_relaxed); }
    bool release() { return refs_.fetch_sub(1, std::memory_order_acq_rel) == 1; }
    int refCount() const { return refs_.load(std::memory_order_acquire); }
    // Same for a song and every copy stretched from it (the player maps its position between them).
    uint64_t song() const { return song_; }
    // This copy's length over the song's as separated (1: the song itself; a copy stretched for
    // another tempo: its ratio). Stem edits are told in the song's own frames and scaled by it.
    double scale() const { return scale_; }

private:
    StemBundle();
    double scale_ = 1.0;
    std::array<std::vector<int16_t>, kStems> data_;
    int64_t frames_ = 0;
    uint64_t song_ = 0;
    std::atomic<int> refs_{1};
};

// Plays a StemBundle in step with the transport: starts on step 0, from `startFrame` of the
// song, each stem with its own gain / mute / solo, summed to stereo. A negative `startFrame` is a
// silence first: the song comes in that many frames after step 0 (placed later in the song).
// Real-time safe.
class StemPlayer {
public:
    enum Param { Gain = 0, Mute = 1, Solo = 2 };
    // Step 5b: a stem's non-destructive edits, pieces of it laid out on the song's timeline: frames
    // [at, at + length) of the timeline play frames from `from` on, with linear fades in and out
    // (frames). All in frames of the song as separated, at the engine rate (scaled for a copy).
    enum ClipField { ClipAt = 0, ClipFrom = 1, ClipLength = 2, ClipFadeIn = 3, ClipFadeOut = 4 };
    static constexpr int kMaxClips = 32;
    struct Clip {
        int64_t at = 0, from = 0, length = 0;
        int32_t fadeIn = 0, fadeOut = 0;
    };

    ~StemPlayer();
    void prepare(int sampleRate);
    // Takes over one reference (nullptr = no song). The previous bundle is retired. While it
    // plays, the old one crossfades into the new one over 10 ms (nullptr: it fades out); a copy
    // of the same song at another tempo goes on from the same musical position (the position
    // and the start frame scaled by the ratio of their lengths).
    // `steps`, `stepFrames`: where the transport is now (Sequencer::position) and its step length:
    // a copy for the tempo playing now picks up there (not where the old copy, which played on at
    // the old tempo while the copy was made, had got to). NaN: no transport (the preview).
    void install(StemBundle* bundle, double steps = std::numeric_limits<double>::quiet_NaN(), double stepFrames = 0.0);
    void setParam(int stem, int param, float value);
    // Step 5b: a field of piece `clip` of `stem`'s next edits (kept aside until committed).
    void setClipField(int stem, int clip, int field, int64_t value);
    // The pieces set aside become `stem`'s edits, its first `count` ones (-1: no edits, the stem
    // plays as separated; 0: silent). Pieces overlapping: the later one is heard.
    void commitClips(int stem, int count);
    // Negative: that many frames of silence before the song's first frame.
    void setStartFrame(int64_t frame) { startFrame_ = frame; }

    // Starts `offset` frames into the next render() call, with a 5 ms fade in (what was playing
    // fades out under it).
    // `steps`, `stepFrames`: the transport's step it starts on and the step length then (a lead-in,
    // the negative start frame, is the project's time: so many steps).
    void start(int offset, double steps = 0.0, double stepFrames = 0.0);
    // Short fade then silence.
    void stop();
    // The live grid's remix slot: on, it is heard; off, it goes quiet in a few milliseconds from
    // `offset` frames into the next render() while the song keeps its place (back in time when on).
    void setLive(bool on, int offset = 0);
    bool live() const { return live_; }
    void render(float* left, float* right, int numFrames);

    bool loaded() const { return bundle_ != nullptr; }
    bool playing() const { return playing_; }
    // Something may sound in the next render(): playing, about to start, or a bundle fading out.
    bool active() const { return playing_ || pendingStart_ >= 0 || fading(); }
    // Song position in frames (UI). -1 when stopped, or while the song has not come in yet.
    int64_t position() const { return position_.load(std::memory_order_relaxed); }

    // Frees retired bundles. Not on the audio thread.
    void collectGarbage();

private:
    void retire(StemBundle* b);
    void updateTargets();
    // The bundle playing now fades out on its own over `frames` (from its current fade level).
    void fadeAway(int frames);
    void renderFrames(float* left, float* right, int from, int to, int wait);
    // The edits in frames of `b` (its scale), into `out` (count -1: none).
    void scaleEdits(const StemBundle* b, std::array<int, StemBundle::kStems>& count,
                    std::array<std::array<Clip, kMaxClips>, StemBundle::kStems>& out) const;
    void updateEnd();
    // Stem `s`'s sample at timeline frame `pos` of a bundle `frames` long, through its scaled
    // edits: the source frame (-1: silent) and the fade gain. `cursor`: the piece found last.
    static int64_t sourceOf(const Clip* clips, int count, int64_t pos, int64_t frames, int edge, int& cursor, float& gain);

    StemBundle* bundle_ = nullptr;
    SpscQueue<StemBundle*, 64> retired_;
    std::array<float, StemBundle::kStems> gain_{0.8f, 0.8f, 0.8f, 0.8f};
    std::array<bool, StemBundle::kStems> mute_{};
    std::array<bool, StemBundle::kStems> solo_{};
    std::array<Smoother, StemBundle::kStems> smooth_{};
    int64_t startFrame_ = 0;
    int64_t pos_ = 0;
    // The transport's step the song started on, the start frame it started from in the bundle
    // playing, and its lead-in in steps (a negative start frame).
    double startSteps_ = 0.0, pendingSteps_ = 0.0, pendingStepFrames_ = 0.0, leadSteps_ = 0.0;
    int64_t playStart_ = 0;
    int pendingStart_ = -1;
    bool playing_ = false;
    int fadeLeft_ = 0;
    int fadeLength_ = 240;
    int fadeIn_ = 0;          // > 0: fading in (start, or the new bundle of a crossfade)
    int fadeInLength_ = 1;
    int crossfade_ = 480;     // 10 ms
    // The bundle that played before, fading out at its own position (it holds a reference).
    struct Outgoing {
        StemBundle* bundle = nullptr;
        int64_t pos = 0;
        int left = 0, length = 1;
        // Its stems' edits, scaled for it when it started to fade.
        std::array<int, StemBundle::kStems> count{-1, -1, -1, -1};
        std::array<std::array<Clip, kMaxClips>, StemBundle::kStems> clips{};
        std::array<int, StemBundle::kStems> cursor{};
    };
    // Two: a copy fading under a new one is still there when the next fade starts (the transport
    // started over, another copy) a few milliseconds later, never cut dead.
    std::array<Outgoing, 2> outs_{};
    bool fading() const { return outs_[0].bundle != nullptr || outs_[1].bundle != nullptr; }
    bool live_ = true;
    int liveAt_ = -1;       // frame of the next render() where the live gain starts to move, -1 none
    float liveGain_ = 1.0f; // 1 = heard: the plain mix, sample for sample
    std::atomic<int64_t> position_{-1};
    // Step 5b: each stem's edits in song frames (staged until committed), and scaled for the
    // bundle playing (its scale when they were worked out). Outgoing copies get theirs on the fly.
    std::array<int, StemBundle::kStems> editCount_{-1, -1, -1, -1};
    std::array<std::array<Clip, kMaxClips>, StemBundle::kStems> edits_{}, staged_{};
    std::array<int, StemBundle::kStems> playCount_{-1, -1, -1, -1};
    std::array<std::array<Clip, kMaxClips>, StemBundle::kStems> playEdits_{};
    // Where the bundle playing ends: its last frame, or later when a piece was moved past it.
    int64_t playEnd_ = 0;
    std::array<int, StemBundle::kStems> cursor_{};
    int edge_ = 96;  // 2 ms: the shortest fade at a piece's edge (no click at a cut)
};


// Lot D2 (« case Remix du Live »): a passage of the separated song per cell of the Live grid's
// remix column (one per brick): a stem or the whole song, cut on whole bars and stretched to the
// project's tempo off the audio thread (as the loops of step 4e). The remix slot plays one of them
// in a loop counted from its launch on the transport's grid (sample-accurate on the bar), instead
// of the whole song. Its read position follows the transport's beats: when the tempo changes, the
// copy playing is read faster or slower until the copy for the new tempo comes (a refit, picked up
// at the same place and cross-faded): never a gap nor a jump. Real-time safe except where noted.
class RemixCells {
public:
    static constexpr int kCells = 32;
    RemixCells() = default;
    ~RemixCells();
    RemixCells(const RemixCells&) = delete;
    RemixCells& operator=(const RemixCells&) = delete;

    void prepare(int sampleRate) {
        rate_ = std::max(1, sampleRate);
        stopFade_ = std::max(1, sampleRate / 200);   // 5 ms
        crossFade_ = std::max(1, sampleRate / 100);  // 10 ms
    }
    // Takes over one reference (nullptr: the cell is emptied). `loopSteps`: the passage's length in
    // transport steps (whole bars). `refit`: the same passage at another tempo (no restart).
    void install(int cell, Sample* sample, int loopSteps, bool refit);
    bool has(int cell) const { return cell >= 0 && cell < kCells && cells_[static_cast<size_t>(cell)].sample != nullptr; }
    // Cell `cell` plays from `offset` frames into the next render(), `local` steps into its loop
    // (0: from its first frame, at full level; elsewhere it fades in). A cell already playing goes on.
    void play(int cell, int offset, double local);
    // Fades out from `offset` frames into the next render().
    void stop(int offset = 0);
    // The transport's beat at `offset` frames into the next render(): the cell playing is `local`
    // steps from its start there (its clock is pulled to it over the next beat, or snapped when far).
    void sync(int offset, double local);
    // Frames per step at the current tempo (each block).
    void setStepFrames(double frames) {
        if (frames > 1.0 && std::isfinite(frames)) stepFrames_ = frames;
    }
    // Adds the passage into the stereo pair.
    void render(float* left, float* right, int numFrames);
    bool active() const { return voice_.sample != nullptr || tails_[0].sample != nullptr || tails_[1].sample != nullptr || startAt_ >= 0; }
    // The cell heard (-1: none), any thread.
    int playing() const { return heard_.load(std::memory_order_relaxed); }
    // Not the audio thread: frees the passages let go of.
    void collectGarbage();

private:
    struct Voice {
        const Sample* sample = nullptr;
        double local = 0.0;  // steps since the cell's start
        int loopSteps = 16;
        int rise = 0, riseLen = 1;  // > 0: fading in
        int fade = 0, fadeLen = 1;  // > 0: fading out (a tail)
    };
    struct Cell {
        Sample* sample = nullptr;
        int loopSteps = 16;
    };
    void retire(Sample* s);
    void toTail(int frames);
    void begin(int cell, double local);
    float read(const Voice& v, float& r) const;

    std::array<Cell, kCells> cells_{};
    Voice voice_{};
    int cell_ = -1;
    std::array<Voice, 2> tails_{};
    std::atomic<int> heard_{-1};
    int startAt_ = -1, startCell_ = -1;
    double startLocal_ = 0.0;
    int stopAt_ = -1;
    int syncAt_ = -1;
    double syncLocal_ = 0.0;
    double correction_ = 0.0;  // steps added per frame until the next beat (the clock pulled to the transport)
    double stepFrames_ = 6000.0;
    int rate_ = 48000, stopFade_ = 240, crossFade_ = 480;
    SpscQueue<Sample*, 64> retired_;
};

}  // namespace ps
