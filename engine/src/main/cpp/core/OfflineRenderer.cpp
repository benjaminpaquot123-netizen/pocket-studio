#include "OfflineRenderer.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <string>

#include <samplerate.h>

#include "Loudness.h"
#include "MasterChain.h"
#include "WavWriter.h"

namespace ps {

// --- The music before the master chain, between the two passes ----------------------------

class OfflineRenderer::Store {
public:
    virtual ~Store() = default;
    virtual bool write(const float* frames, int n) = 0;
    virtual bool rewind() = 0;
    virtual int read(float* frames, int max) = 0;  // 0 at the end, -1 on an error
};

// A raw float file next to the WAV (a 3-minute song is 70 MB: never in memory).
class OfflineRenderer::FileStore : public Store {
public:
    explicit FileStore(std::string path) : path_(std::move(path)), file_(std::fopen(path_.c_str(), "w+b")) {}
    ~FileStore() override {
        if (file_) std::fclose(file_);
        std::remove(path_.c_str());
    }
    bool ok() const { return file_ != nullptr; }
    bool write(const float* frames, int n) override {
        return std::fwrite(frames, sizeof(float) * 2, static_cast<size_t>(n), file_) == static_cast<size_t>(n);
    }
    bool rewind() override { return std::fflush(file_) == 0 && std::fseek(file_, 0, SEEK_SET) == 0; }
    int read(float* frames, int max) override {
        const size_t n = std::fread(frames, sizeof(float) * 2, static_cast<size_t>(max), file_);
        return n == 0 && std::ferror(file_) ? -1 : static_cast<int>(n);
    }

private:
    std::string path_;
    std::FILE* file_;
};

// In memory (renderToBuffer): the mastered frames are written back over the ones already read.
class OfflineRenderer::MemoryStore : public Store {
public:
    explicit MemoryStore(std::vector<float>& data) : data_(data) {}
    bool write(const float* frames, int n) override {
        data_.insert(data_.end(), frames, frames + 2 * n);
        return true;
    }
    bool rewind() override {
        read_ = 0;
        return true;
    }
    int read(float* frames, int max) override {
        const size_t n = std::min(static_cast<size_t>(max), data_.size() / 2 - read_);
        std::copy(data_.begin() + static_cast<std::ptrdiff_t>(2 * read_), data_.begin() + static_cast<std::ptrdiff_t>(2 * (read_ + n)), frames);
        read_ += n;
        return static_cast<int>(n);
    }

private:
    std::vector<float>& data_;
    size_t read_ = 0;
};

// The last stage, whatever the passes: the fades (counted from the render's length cap), the
// resampling to 44.1 kHz, the report's meter, then the file or the buffer.
class OfflineRenderer::Output {
public:
    Output(int engineRate, int outRate, int64_t frames, int64_t tailFrames, int64_t fadeOut, Sink sink)
        : total_(frames + tailFrames), sink_(std::move(sink)), ratio_(static_cast<double>(outRate) / engineRate) {
        // The tail's last half second fades out: a long reverb is not cut dead at the file's end
        // (with the automatic tail, only one still ringing at the cap gets there).
        fade_ = std::min<int64_t>(tailFrames, engineRate / 2);
        fadeOut_ = std::min(fadeOut, total_);
        meter_.prepare(outRate);
        if (outRate != engineRate) {
            int err = 0;
            src_ = src_new(SRC_SINC_BEST_QUALITY, 2, &err);
            resampled_.resize(static_cast<size_t>(2 * (static_cast<int>(kBlock * ratio_) + 64)));
        }
    }
    ~Output() {
        if (src_) src_delete(src_);
    }
    Output(const Output&) = delete;
    Output& operator=(const Output&) = delete;

    bool write(float* out, int m) {
        // A requested fade-out takes the end of the file down to silence along a squared curve
        // (closer to how loudness is heard than a straight line). Gains never exceed 1: no clipping.
        for (int i = 0; i < m && (fade_ > 0 || fadeOut_ > 0); ++i) {
            const int64_t left = total_ - (done_ + i);
            if (left >= fade_ && left >= fadeOut_) continue;
            float g = 1.0f;
            if (left < fade_) g = static_cast<float>(left) / static_cast<float>(fade_);
            if (left < fadeOut_) {
                const float x = static_cast<float>(left) / static_cast<float>(fadeOut_);
                g = std::min(g, x * x);
            }
            out[2 * i] *= g;
            out[2 * i + 1] *= g;
        }
        done_ += m;
        if (!src_) return deliver(out, m);
        return resample(out, m, false);
    }
    bool finish() { return src_ ? resample(nullptr, 0, true) : !failed_; }
    void report(Report& r) const {
        r.lufs = meter_.integratedLufs();
        r.truePeak = meter_.peakSinceReset();
        r.frames = written_;
    }
    bool ok() const { return !failed_ && (src_ != nullptr || ratio_ == 1.0); }

private:
    bool deliver(float* out, int m) {
        for (int i = 0; i < m; ++i) meter_.add(out[2 * i], out[2 * i + 1]);
        written_ += m;
        if (!sink_(out, m)) failed_ = true;
        return !failed_;
    }
    bool resample(const float* in, int m, bool last) {
        SRC_DATA d{};
        d.data_in = in;
        d.input_frames = m;
        d.src_ratio = ratio_;
        d.end_of_input = last ? 1 : 0;
        do {
            d.data_out = resampled_.data();
            d.output_frames = static_cast<long>(resampled_.size() / 2);
            if (src_process(src_, &d) != 0) return false;
            if (d.output_frames_gen > 0 && !deliver(resampled_.data(), static_cast<int>(d.output_frames_gen))) return false;
            d.data_in += 2 * d.input_frames_used;
            d.input_frames -= d.input_frames_used;
        } while (d.input_frames > 0 || (last && d.output_frames_gen > 0));
        return true;
    }

    int64_t total_;
    int64_t fade_ = 0, fadeOut_ = 0;
    int64_t done_ = 0;     // frames received (the engine's rate)
    int64_t written_ = 0;  // frames delivered (the file's rate)
    Sink sink_;
    double ratio_;
    SRC_STATE* src_ = nullptr;
    std::vector<float> resampled_;
    LoudnessMeter meter_;
    bool failed_ = false;
};

// --- The renderer ---------------------------------------------------------------------------

OfflineRenderer::OfflineRenderer(int sampleRate)
    : sampleRate_(sampleRate), engine_(std::make_unique<Engine>(sampleRate)), outRate_(sampleRate) {
    engine_->setCommandBudget(0);  // every command at once: the scheduled ones land on their frame
}

OfflineRenderer::~OfflineRenderer() {
    // Scheduled commands never reached (cancelled render) may still own references.
    for (size_t i = nextScheduled_; i < scheduled_.size(); ++i) {
        const Command& c = scheduled_[i].second;
        if (c.sample && c.sample->release()) delete c.sample;
        SoundFontPlayer::close(c.font);
        if (c.stems && c.stems->release()) delete c.stems;
    }
}

void OfflineRenderer::post(const Command& command) {
    if (scheduleAt_ >= 0) {
        // Keep the order: a later frame never runs before an earlier one already scheduled.
        const int64_t at = scheduled_.empty() ? scheduleAt_ : std::max(scheduleAt_, scheduled_.back().first);
        scheduled_.emplace_back(at, command);
        return;
    }
    postNow(command);
}

void OfflineRenderer::postNow(const Command& command) {
    while (!engine_->post(command)) {
        // Queue full: let the engine apply what it holds (zero frames rendered).
        engine_->process(nullptr, 0, 0, block_, 0, 2);
    }
}

void OfflineRenderer::postDue(int64_t frame) {
    while (nextScheduled_ < scheduled_.size() && scheduled_[nextScheduled_].first <= frame) {
        postNow(scheduled_[nextScheduled_].second);
        ++nextScheduled_;
    }
}

int64_t OfflineRenderer::nextDue() const {
    return nextScheduled_ < scheduled_.size() ? scheduled_[nextScheduled_].first : INT64_MAX;
}

void OfflineRenderer::renderSilent(int frames) {
    for (int done = 0; done < frames; done += kBlock) {
        engine_->process(nullptr, 0, 0, block_, std::min(kBlock, frames - done), 2);
    }
}

OfflineRenderer::Result OfflineRenderer::renderEngine(int64_t frames, int64_t tailFrames, const Sink& sink, float progressTo) {
    // Let parameter smoothers reach the project's settings before the first beat.
    renderSilent(sampleRate_ / 10);
    engine_->mixer().settle();
    postNow({CommandType::SetTransport, 1.0f, 0});

    const int64_t total = frames + tailFrames;
    int64_t quiet = 0;  // frames in a row under -90 dBFS, in the tail
    // The master chain's look-ahead delays the output: the file drops its first frames, so that it
    // starts on the song's first frame (a hit on step 0 is on frame 0, as on the grid). The engine
    // runs in the same blocks as without it: the same sound as live, sample for sample.
    const int64_t skip = engine_->mixer().outputLatency();
    for (int64_t at = 0; at < total + skip;) {  // `at`: the engine's frame
        if (cancelled_.load(std::memory_order_relaxed)) return Result::Cancelled;
        // Stop exactly at the end of the loop, then render the tail.
        int n = static_cast<int>(std::min<int64_t>(kBlock, total + skip - at));
        if (at < frames) n = static_cast<int>(std::min<int64_t>(n, frames - at));
        // Scheduled commands land on their exact frame: the block stops there.
        postDue(at);
        n = static_cast<int>(std::min<int64_t>(n, std::max<int64_t>(1, nextDue() - at)));
        if (at == frames) postNow({CommandType::SetTransport, 0.0f, 0});
        engine_->process(nullptr, 0, 0, block_, n, 2);
        const int first = static_cast<int>(std::clamp<int64_t>(skip - at, 0, n));
        if (first == n) {
            at += n;
            continue;
        }
        float* out = block_ + 2 * first;
        const int m = n - first;
        const int64_t done = at + first - skip;  // the file's frame of out[0]
        // The automatic tail ends on the frame where the output has been quiet long enough.
        int keep = m;
        bool ended = false;
        if (autoTail_ && done + m > frames) {
            auto& mixer = engine_->mixer();
            const int64_t window = sampleRate_ / 5 + (mixer.echoInUse() ? static_cast<int64_t>(mixer.echoFrames()) : 0);
            for (int i = static_cast<int>(std::max<int64_t>(0, frames - done)); i < m; ++i) {
                const bool silent = std::fabs(out[2 * i]) < kSilence && std::fabs(out[2 * i + 1]) < kSilence;
                quiet = silent ? quiet + 1 : 0;
                if (quiet >= window) {
                    keep = i + 1;
                    ended = true;
                    break;
                }
            }
        }
        if (!sink(out, keep)) return Result::IoError;
        at += first + keep;
        if (ended) break;
        progress_.store(progressTo * static_cast<float>(done + keep) / static_cast<float>(total), std::memory_order_relaxed);
    }
    return Result::Ok;
}

OfflineRenderer::Result OfflineRenderer::masterPass(Store& store, float driveDb, const Sink& sink, float progressFrom, float progressTo,
                                                    double* lufs) {
    if (!store.rewind()) return Result::IoError;
    // A chain like the engine's (its ceiling), fresh, at a held drive.
    auto chain = std::make_unique<MasterChain>();
    chain->setCeilingDb(engine_->mixer().masterChain().ceilingDb());
    chain->prepare(sampleRate_);
    chain->holdDrive(driveDb);
    const int latency = chain->latency();
    std::vector<float> buf(2 * kBlock), l(kBlock), r(kBlock);
    int64_t read = 0, skipped = 0, flushed = 0;
    // Progress: the frames read, against the stored length (known to the caller through `read`).
    for (;;) {
        if (cancelled_.load(std::memory_order_relaxed)) return Result::Cancelled;
        int n = store.read(buf.data(), kBlock);
        if (n < 0) return Result::IoError;
        if (n == 0) {
            // The end: zeros push the last frames out of the look-ahead.
            if (flushed >= latency) break;
            n = static_cast<int>(std::min<int64_t>(kBlock, latency - flushed));
            std::fill(buf.begin(), buf.begin() + 2 * n, 0.0f);
            flushed += n;
        } else {
            read += n;
        }
        for (int i = 0; i < n; ++i) {
            l[static_cast<size_t>(i)] = buf[static_cast<size_t>(2 * i)];
            r[static_cast<size_t>(i)] = buf[static_cast<size_t>(2 * i + 1)];
        }
        chain->process(l.data(), r.data(), nullptr, n);
        const int first = static_cast<int>(std::clamp<int64_t>(latency - skipped, 0, n));
        skipped += first;
        for (int i = first; i < n; ++i) {
            buf[static_cast<size_t>(2 * (i - first))] = l[static_cast<size_t>(i)];
            buf[static_cast<size_t>(2 * (i - first) + 1)] = r[static_cast<size_t>(i)];
        }
        if (n > first && !sink(buf.data(), n - first)) return Result::IoError;
        const float done = std::min(1.0f, static_cast<float>(read) / static_cast<float>(std::max<int64_t>(1, report_.frames)));
        progress_.store(progressFrom + (progressTo - progressFrom) * done, std::memory_order_relaxed);
    }
    if (lufs) *lufs = chain->meter().integratedLufs();
    return Result::Ok;
}

OfflineRenderer::Result OfflineRenderer::render(int64_t frames, int64_t tailFrames, const std::function<std::unique_ptr<Store>()>& makeStore,
                                                Output& output) {
    progress_.store(0.0f, std::memory_order_relaxed);
    report_ = Report{};
    // The commands posted so far applied (zero frames rendered): the master's target is known.
    engine_->process(nullptr, 0, 0, block_, 0, 2);
    auto& mixer = engine_->mixer();
    MasterChain& live = mixer.masterChain();
    Result result = Result::Ok;
    if (!live.targetActive()) {
        mixer.setMasterChainBypass(false);
        result = renderEngine(frames, tailFrames, [&output](float* p, int n) { return output.write(p, n); }, 1.0f);
        report_.driveDb = 0.0f;
    } else {
        // 1. The music, before the master chain, and its loudness.
        auto store = makeStore();
        if (!store) return Result::IoError;
        LoudnessMeter music;
        music.prepare(sampleRate_);
        int64_t stored = 0;
        mixer.setMasterChainBypass(true);
        result = renderEngine(frames, tailFrames, [&](float* p, int n) {
            for (int i = 0; i < n; ++i) music.add(p[2 * i], p[2 * i + 1]);
            stored += n;
            return store->write(p, n);
        }, 0.8f);
        mixer.setMasterChainBypass(false);
        if (result != Result::Ok) return result;
        report_.frames = stored;  // the passes' progress counts against it
        // 2. The drive: from the music's loudness, corrected by what the chain takes off (its
        // loudness bends as it is pushed): two measured tries at most, then the secant.
        const double target = live.target(), pre = music.integratedLufs();
        report_.musicLufs = pre;
        auto clampDrive = [](double d) { return std::clamp(d, double(MasterChain::kMinDriveDb), double(MasterChain::kMaxDriveDb)); };
        double drive = 0.0;
        if (pre > GatedLoudness::kGateAbs) {
            const auto measure = [&](double d, float from, float to, double& lufs) {
                return masterPass(*store, static_cast<float>(d), [](float*, int) { return true; }, from, to, &lufs);
            };
            // What the chain takes off the driven music (LU): past kMaxLossDb it only squashes.
            const auto loss = [pre](double d, double l) { return pre + d - l; };
            const double d0 = clampDrive(target - pre);
            double l0 = 0.0, l1 = 0.0;
            if ((result = measure(d0, 0.8f, 0.875f, l0)) != Result::Ok) return result;
            drive = d0;
            if (std::fabs(l0 - target) > 0.1 && !(l0 < target && loss(d0, l0) >= MasterChain::kMaxLossDb)) {
                const double d1 = clampDrive(d0 + (target - l0));
                if ((result = measure(d1, 0.875f, 0.95f, l1)) != Result::Ok) return result;
                drive = d1;
                if (std::fabs(l1 - target) > 0.1) {
                    const double slope = d1 != d0 ? (l1 - l0) / (d1 - d0) : 0.0;
                    double d2 = d1 + (target - l1) / (slope > 0.05 ? slope : 1.0);
                    // Louder only while the chain gives loudness for its reduction.
                    const double lossSlope = 1.0 - slope;
                    if (d2 > d1 && lossSlope > 0.05) d2 = std::min(d2, d1 + (MasterChain::kMaxLossDb - loss(d1, l1)) / lossSlope);
                    drive = clampDrive(d2);
                }
            }
        }
        // 3. The master chain at that drive, into the file.
        result = masterPass(*store, static_cast<float>(drive), [&output](float* p, int n) { return output.write(p, n); }, 0.95f, 1.0f, nullptr);
        report_.driveDb = static_cast<float>(drive);
    }
    if (result != Result::Ok) return result;
    if (!output.finish()) return Result::IoError;
    output.report(report_);
    progress_.store(1.0f, std::memory_order_relaxed);
    return Result::Ok;
}

OfflineRenderer::Result OfflineRenderer::renderToWav(const char* path, int64_t frames, int64_t tailFrames) {
    WavWriter wav;
    if (!wav.openBits(path, outRate_, 2, bits_)) return Result::IoError;
    Output output(sampleRate_, outRate_, frames, tailFrames, fadeOut_, [&wav](float* b, int n) { return wav.write(b, n); });
    if (!output.ok()) return Result::IoError;
    const std::string temp = std::string(path) + ".premaster";
    const Result r = render(frames, tailFrames, [&temp]() -> std::unique_ptr<Store> {
        auto store = std::make_unique<FileStore>(temp);
        if (!store->ok()) return nullptr;
        return store;
    }, output);
    report_.sampleRate = outRate_;
    const bool closed = wav.close();
    if (r != Result::Ok) return r;
    return closed ? Result::Ok : Result::IoError;
}

std::vector<float> OfflineRenderer::renderToBuffer(int64_t frames, int64_t tailFrames) {
    std::vector<float> out;
    out.reserve(static_cast<size_t>((frames + tailFrames) * 2));
    // Two passes: the music is kept in `out`, and the mastered frames are written back over it
    // (never ahead of what the chain has read).
    bool stored = false;
    size_t written = 0;
    Output output(sampleRate_, sampleRate_, frames, tailFrames, fadeOut_, [&out, &stored, &written](float* b, int n) {
        if (stored) {
            std::copy(b, b + 2 * n, out.begin() + static_cast<std::ptrdiff_t>(written));
        } else {
            out.insert(out.end(), b, b + n * 2);
        }
        written += static_cast<size_t>(2 * n);
        return true;
    });
    const Result r = render(frames, tailFrames, [&out, &stored]() -> std::unique_ptr<Store> {
        stored = true;
        return std::make_unique<MemoryStore>(out);
    }, output);
    report_.sampleRate = sampleRate_;
    if (r != Result::Ok) out.clear();
    else out.resize(written);
    return out;
}

}  // namespace ps
