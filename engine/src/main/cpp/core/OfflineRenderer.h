#pragma once

#include <algorithm>
#include <array>
#include <atomic>
#include <cstdint>
#include <functional>
#include <vector>
#include <memory>

#include "Engine.h"

namespace ps {

// Renders a private Engine (not the live one) faster than real time into a WAV file.
// Commands are posted to engine() exactly as the UI posts them to the live engine.
// Runs on a worker thread; progress() and cancel() may be called from any thread.
//
// With a loudness target on the master (MasterChain::targetActive), the song is rendered in two
// passes: first the music before the master chain (kept in a file next to the WAV, or in the
// buffer) and its loudness; then the master chain over it at the drive that brings the whole
// song to the target (found with at most two quick passes of the chain alone). The file is at
// the target from its first second, whatever its form (the live engine can only estimate).
class OfflineRenderer {
public:
    enum class Result { Ok, IoError, Cancelled };

    explicit OfflineRenderer(int sampleRate = 48000);
    ~OfflineRenderer();

    // Posts a command, draining the queue into the engine when it is full. After scheduleFrom(f)
    // with f >= 0, commands are kept and posted when the render reaches frame f of the song.
    void post(const Command& command);
    // Later post() calls take effect at `frame` of the song (-1: at once again). A song export
    // stages each next scene this way while the previous one plays.
    void scheduleFrom(int64_t frame) { scheduleAt_ = frame; }
    // The last `frames` of the next render fade to silence (a song's "fondu de fin"); 0: none.
    // Applied on this private engine's output, never on the live audio thread.
    void setFadeOut(int64_t frames) { fadeOut_ = std::max<int64_t>(0, frames); }
    // Automatic tail: the `tailFrames` of the next render become a cap. The tail ends once the
    // output has stayed under -90 dBFS for 200 ms (longer than the send echo's time when it is in
    // use, so a gap between two repeats does not end it); only a tail that reaches the cap fades out.
    void setAutoTail(bool on) { autoTail_ = on; }
    // renderToWav writes 32-bit float samples (no clipping above 0 dBFS: a mix without limiter).
    void setFloatWav(bool on) { bits_ = on ? 32 : 24; }
    // renderToWav's format: 44100 Hz (resampled with libsamplerate's best sinc) or the engine's
    // rate; 16 or 24 bits (TPDF dither) or 32 (float). Frame counts given to renders stay at the
    // engine's rate.
    void setOutputFormat(int sampleRate, int bits) {
        outRate_ = sampleRate == 44100 ? 44100 : sampleRate_;
        bits_ = bits == 16 || bits == 32 ? bits : 24;
    }
    static constexpr float kSilence = 3.1623e-5f;  // -90 dBFS

    // Plays the transport for `frames`, then renders `tailFrames` stopped so
    // releases ring out. Output is 24-bit stereo unless setOutputFormat says otherwise.
    Result renderToWav(const char* path, int64_t frames, int64_t tailFrames);
    // Same, into memory (interleaved stereo at the engine's rate). Empty when cancelled. `mono`: one
    // channel, the mean of left and right (a long frozen track: half the memory, and the stereo is
    // never held in full when the render has a single pass, i.e. without a loudness target).
    std::vector<float> renderToBuffer(int64_t frames, int64_t tailFrames, bool mono = false);

    // What the last render delivered, measured on what was written (fades and resampling
    // included): integrated loudness (LUFS; -1000: silence), true peak (linear), length.
    struct Report {
        double lufs = -1000.0;
        float truePeak = 0.0f;
        int64_t frames = 0;
        int sampleRate = 48000;
        float driveDb = 0.0f;  // the loudness target's drive (0 without one)
        double musicLufs = -1000.0;  // the music before the master chain (two-pass render only)
        // S5 (two-pass render only): the loudness of the loudest part (LoudPart, the Drop) of the
        // music before the chain and of the master, and what the clipper and the limiter took off
        // (LU: in Drop mode over the loud part, else over the whole song); the target's mode.
        double musicLoudLufs = -1000.0;
        double loudLufs = -1000.0;
        float lossDb = 0.0f;
        int mode = 0;  // 0 Song, 1 Drop (MasterChain::Mode)
        float targetLufs = 0.0f;  // 0: none
    };
    const Report& report() const { return report_; }

    float progress() const { return progress_.load(std::memory_order_relaxed); }
    void cancel() { cancelled_.store(true, std::memory_order_relaxed); }

    static constexpr int kBlock = 512;  // frames per engine call (tests drive a live engine alike)

private:
    // Frames on their way to the file (interleaved stereo); false stops with an I/O error.
    using Sink = std::function<bool(float*, int)>;
    class Store;
    class FileStore;
    class MemoryStore;
    class Output;
    void renderSilent(int frames);
    // Plays the song through `sink` block by block (the master chain's latency dropped);
    // progress goes up to `progressTo`.
    Result renderEngine(int64_t frames, int64_t tailFrames, const Sink& sink, float progressTo);
    // The whole render: one pass, or two (into `makeStore()`) with a loudness target.
    Result render(int64_t frames, int64_t tailFrames, const std::function<std::unique_ptr<Store>()>& makeStore, Output& output);
    // The master chain at a held drive over the stored music, its latency dropped, into `sink`.
    Result masterPass(Store& store, float driveDb, const Sink& sink, float progressFrom, float progressTo, double* lufs,
                      double* loud = nullptr);

    // S5, Drop mode: the drive bringing the loud part (`preLoud` before the chain) to `target`
    // within MasterChain::kMaxLossDropDb, from measured passes of the chain over the stored music.
    Result dropDrive(Store& store, double preLoud, double target, float& drive);

    void postNow(const Command& command);
    void postDue(int64_t frame);
    int64_t nextDue() const;

    int sampleRate_;
    std::unique_ptr<Engine> engine_;
    int64_t scheduleAt_ = -1;
    int64_t fadeOut_ = 0;
    bool autoTail_ = false;
    int outRate_;
    int bits_ = 24;
    Report report_;
    std::vector<std::pair<int64_t, Command>> scheduled_;  // in posting order
    size_t nextScheduled_ = 0;
    std::atomic<float> progress_{0.0f};
    std::atomic<bool> cancelled_{false};
    float block_[kBlock * 2];
};

}  // namespace ps
