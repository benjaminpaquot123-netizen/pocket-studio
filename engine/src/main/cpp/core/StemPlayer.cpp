#include "StemPlayer.h"
#include "Params.h"

#include <algorithm>
#include <cmath>
#include <filesystem>
#include <memory>

#include "Analysis.h"
#include "TimeStretch.h"
#include "samplerate.h"

namespace ps {

namespace {

// Stored at half level: stems that peak up to +6 dBFS still fit without clipping.
constexpr float kHeadroom = 2.0f;

int16_t toPcm(float v) {
    if (!std::isfinite(v)) return 0;
    return static_cast<int16_t>(std::lround(std::clamp(v / kHeadroom, -1.0f, 1.0f) * 32767.0f));
}

// One WAV, resampled to `targetRate` stereo int16. Mono files are copied to both sides.
bool loadStem(const std::string& path, int targetRate, std::vector<int16_t>& out) {
    std::error_code ec;
    const auto fileBytes = std::filesystem::file_size(path, ec);
    if (ec) return false;
    int rate = 0;
    std::vector<float> stereo;
    std::unique_ptr<SRC_STATE, decltype(&src_delete)> src(nullptr, &src_delete);
    std::vector<float> resampled(8192 * 2 + 64);
    double ratio = 1.0;
    auto push = [&](const float* x, long frames, bool last) {
        if (!src) {
            for (long i = 0; i < frames * 2; ++i) out.push_back(toPcm(x[i]));
            return true;
        }
        SRC_DATA d{};
        d.data_in = x;
        d.input_frames = frames;
        d.src_ratio = ratio;
        d.end_of_input = last ? 1 : 0;
        do {
            d.data_out = resampled.data();
            d.output_frames = static_cast<long>(resampled.size() / 2);
            if (src_process(src.get(), &d) != 0) return false;
            for (long i = 0; i < d.output_frames_gen * 2; ++i) out.push_back(toPcm(resampled[static_cast<size_t>(i)]));
            d.data_in += d.input_frames_used * 2;
            d.input_frames -= d.input_frames_used;
        } while (d.input_frames > 0 || (last && d.output_frames_gen > 0));
        return true;
    };
    bool ok = true;
    const bool read = readWavStereo(path, rate, [&](const float* x, int frames) {
        if (!ok) return;
        if (out.empty() && out.capacity() == 0) {
            // Assume 32-bit samples: one allocation instead of repeated doubling.
            out.reserve(static_cast<size_t>(static_cast<double>(fileBytes) / 4.0 * targetRate / rate) + 4096);
        }
        if (!src && rate != targetRate && out.empty()) {
            int err = 0;
            src.reset(src_new(SRC_SINC_MEDIUM_QUALITY, 2, &err));
            ratio = static_cast<double>(targetRate) / rate;
            if (!src) { ok = false; return; }
        }
        ok = push(x, frames, false);
    });
    if (!read || !ok) return false;
    if (src) {
        const float none[2] = {0.0f, 0.0f};
        if (!push(none, 0, true)) return false;
    }
    return true;
}

}  // namespace

namespace {
std::atomic<uint64_t> g_nextSong{1};
}

StemBundle::StemBundle() : song_(g_nextSong.fetch_add(1, std::memory_order_relaxed)) {}

StemBundle* StemBundle::load(const std::vector<std::string>& wavPaths, int targetRate) {
    if (wavPaths.size() != kStems || targetRate <= 0) return nullptr;
    try {
        std::unique_ptr<StemBundle> b(new StemBundle());
        for (int i = 0; i < kStems; ++i) {
            if (!loadStem(wavPaths[static_cast<size_t>(i)], targetRate, b->data_[static_cast<size_t>(i)])) return nullptr;
        }
        // Stems come from one model run and share a length; trim to the shortest just in case.
        size_t shortest = b->data_[0].size();
        for (const auto& d : b->data_) shortest = std::min(shortest, d.size());
        for (auto& d : b->data_) { d.resize(shortest); d.shrink_to_fit(); }
        b->frames_ = static_cast<int64_t>(shortest / 2);
        if (b->frames_ <= 0) return nullptr;
        return b.release();
    } catch (const std::bad_alloc&) {
        return nullptr;
    }
}

StemBundle* StemBundle::fromFloat(const std::vector<std::vector<float>>& stereo, const StemBundle* sameSongAs) {
    if (stereo.size() != kStems) return nullptr;
    auto* b = new StemBundle();
    if (sameSongAs) b->song_ = sameSongAs->song_;
    for (int i = 0; i < kStems; ++i) {
        const auto& src = stereo[static_cast<size_t>(i)];
        auto& dst = b->data_[static_cast<size_t>(i)];
        dst.resize(src.size());
        std::transform(src.begin(), src.end(), dst.begin(), toPcm);
    }
    b->frames_ = static_cast<int64_t>(stereo[0].size() / 2);
    if (sameSongAs && sameSongAs->frames_ > 0)
        b->scale_ = sameSongAs->scale_ * static_cast<double>(b->frames_) / static_cast<double>(sameSongAs->frames_);
    return b;
}

std::vector<float> StemBundle::extract(int stem, int64_t start, int64_t count) const {
    std::vector<float> out;
    if (stem < 0 || stem >= kStems || start < 0 || start >= frames_ || count <= 0) return out;
    count = std::min(count, frames_ - start);
    out.resize(static_cast<size_t>(count) * 2);
    const int16_t* d = this->stem(stem) + start * 2;
    for (size_t i = 0; i < out.size(); ++i) out[i] = d[i] * (kHeadroom / 32768.0f);
    return out;
}

StemBundle* StemBundle::stretched(double ratio, int sampleRate) const {
    if (!(ratio > 0.25 && ratio < 4.0) || frames_ <= 0) return nullptr;
    const auto outFrames = static_cast<int64_t>(std::llround(static_cast<double>(frames_) * ratio));
    std::unique_ptr<StemBundle> b(new StemBundle());  // freed if an allocation below throws
    b->frames_ = outFrames;
    b->song_ = song_;  // the same song, at another tempo
    b->scale_ = scale_ * static_cast<double>(outFrames) / static_cast<double>(frames_);
    for (int i = 0; i < kStems; ++i) {
        // One stem at a time: only its float copy and its stretched copy are alive together.
        const std::vector<float> in = extract(i, 0, frames_);
        const std::vector<float> out = timeStretch(in.data(), frames_, 2, outFrames, 0.0f, sampleRate);
        if (out.size() != static_cast<size_t>(outFrames) * 2) return nullptr;
        auto& dst = b->data_[static_cast<size_t>(i)];
        dst.resize(out.size());
        std::transform(out.begin(), out.end(), dst.begin(), toPcm);
    }
    return b.release();
}

std::vector<float> StemBundle::peaks(int stem, int buckets) const {
    std::vector<float> out;
    if (stem < 0 || stem >= kStems || buckets <= 0 || frames_ <= 0) return out;
    out.assign(static_cast<size_t>(buckets), 0.0f);
    const int16_t* d = this->stem(stem);
    for (int64_t i = 0; i < frames_; ++i) {
        const auto b = static_cast<size_t>(i * buckets / frames_);
        const int a = std::max(std::abs(static_cast<int>(d[i * 2])), std::abs(static_cast<int>(d[i * 2 + 1])));
        out[b] = std::max(out[b], a * (kHeadroom / 32768.0f));
    }
    for (float& v : out) v = std::min(v, 1.0f);
    return out;
}

StemPlayer::~StemPlayer() {
    if (bundle_ && bundle_->release()) delete bundle_;
    for (auto& o : outs_) {
        if (o.bundle && o.bundle->release()) delete o.bundle;
    }
    collectGarbage();
}

void StemPlayer::prepare(int sampleRate) {
    for (auto& s : smooth_) s.configure(static_cast<float>(sampleRate), 20.0f);
    fadeLength_ = std::max(1, sampleRate / 200);  // 5 ms
    edge_ = std::max(1, sampleRate / 500);        // 2 ms
    crossfade_ = std::max(1, sampleRate / 100);   // 10 ms
    updateTargets();
    for (auto& s : smooth_) s.snap(s.target());
}

void StemPlayer::retire(StemBundle* b) {
    if (b && b->release() && !retired_.push(b)) {
        // Queue full (16 songs swapped without a collection): leak rather than free here.
    }
}

void StemPlayer::install(StemBundle* bundle, double steps, double stepFrames) {
    // The same bundle again (every push of the app): it keeps playing, the extra reference goes.
    if (bundle && bundle == bundle_) {
        retire(bundle);
        return;
    }
    StemBundle* old = bundle_;
    // Same song at another tempo: the same musical position in the new copy (applied once the old
    // one has taken its own position away to fade out there).
    int64_t pos = pos_;
    if (old && bundle && old->song() == bundle->song() && old->frames() > 0) {
        const double ratio = static_cast<double>(bundle->frames()) / static_cast<double>(old->frames());
        // A silence before the song (negative) is the project's time, not the song's: it stays.
        if (startFrame_ > 0) startFrame_ = static_cast<int64_t>(std::llround(static_cast<double>(startFrame_) * ratio));
        playStart_ = static_cast<int64_t>(std::llround(static_cast<double>(playStart_) * ratio));
        if (playing_ && std::isfinite(steps) && stepFrames > 1.0) {
            // A copy is made for the tempo playing now (the app's rule): the steps gone by since
            // the song started, at this tempo; a lead-in still going: the steps left of it.
            const double since = steps - startSteps_;
            if (playStart_ >= 0) pos = playStart_ + static_cast<int64_t>(std::llround(since * stepFrames));
            else pos = static_cast<int64_t>(std::llround((since - leadSteps_) * stepFrames));
        } else if (pos_ > 0) {
            pos = static_cast<int64_t>(std::llround(static_cast<double>(pos_) * ratio));
        }
    }
    if (playing_ && old) {
        const bool stopping = fadeLeft_ > 0;
        // The old one fades out at its own position while the new one fades in (no jump, no cut).
        fadeAway(crossfade_);
        bundle_ = bundle;
        if (bundle_ && !stopping) {
            playing_ = true;
            fadeIn_ = fadeInLength_ = crossfade_;
        }
    } else {
        retire(old);
        bundle_ = bundle;
    }
    pos_ = pos;
    scaleEdits(bundle_, playCount_, playEdits_);
    updateEnd();
    if (!bundle_) {
        playing_ = false;
        pendingStart_ = -1;
        position_.store(-1, std::memory_order_relaxed);
    }
}

void StemPlayer::fadeAway(int frames) {
    // A free place, else the quieter of the two fading (let go now: three fades within 10 ms).
    Outgoing* to = &outs_[0];
    for (auto& o : outs_) {
        if (!o.bundle) {
            to = &o;
            break;
        }
        if (to->bundle && static_cast<int64_t>(o.left) * to->length < static_cast<int64_t>(to->left) * o.length) to = &o;
    }
    Outgoing& out = *to;
    retire(out.bundle);
    out.bundle = bundle_;  // its reference moves over
    out.pos = pos_;
    out.count = playCount_;
    out.clips = playEdits_;
    out.cursor = cursor_;
    // From the level it is at: a bundle already fading (stop, start) goes on from there.
    float level = fadeLeft_ > 0 ? static_cast<float>(fadeLeft_) / static_cast<float>(fadeLength_) : 1.0f;
    if (fadeIn_ > 0) level *= 1.0f - static_cast<float>(fadeIn_) / static_cast<float>(fadeInLength_);
    out.length = std::max(1, frames);
    out.left = std::max(0, static_cast<int>(std::lround(level * static_cast<float>(out.length))));
    if (out.left == 0) {
        retire(out.bundle);
        out.bundle = nullptr;
    }
    bundle_ = nullptr;
    playing_ = false;
    fadeLeft_ = 0;
    fadeIn_ = 0;
}

void StemPlayer::collectGarbage() {
    StemBundle* b = nullptr;
    while (retired_.pop(b)) delete b;
}

void StemPlayer::setParam(int stem, int param, float value) {
    if (stem < 0 || stem >= StemBundle::kStems) return;
    const auto i = static_cast<size_t>(stem);
    switch (param) {
        case Gain:
            if (!clampSetting(gain_[i], value, 0.0f, 1.0f)) return;
            break;
        case Mute: mute_[i] = value > 0.5f; break;
        case Solo: solo_[i] = value > 0.5f; break;
        default: return;
    }
    updateTargets();
}

void StemPlayer::setClipField(int stem, int clip, int field, int64_t value) {
    if (stem < 0 || stem >= StemBundle::kStems || clip < 0 || clip >= kMaxClips) return;
    Clip& c = staged_[static_cast<size_t>(stem)][static_cast<size_t>(clip)];
    const int64_t v = std::max<int64_t>(0, value);
    const auto f32 = static_cast<int32_t>(std::min<int64_t>(v, std::numeric_limits<int32_t>::max()));
    switch (field) {
        case ClipAt: c.at = v; break;
        case ClipFrom: c.from = v; break;
        case ClipLength: c.length = v; break;
        case ClipFadeIn: c.fadeIn = f32; break;
        case ClipFadeOut: c.fadeOut = f32; break;
        default: break;
    }
}

void StemPlayer::commitClips(int stem, int count) {
    if (stem < 0 || stem >= StemBundle::kStems) return;
    const auto s = static_cast<size_t>(stem);
    editCount_[s] = count < 0 ? -1 : std::min(count, kMaxClips);
    edits_[s] = staged_[s];
    scaleEdits(bundle_, playCount_, playEdits_);
    updateEnd();
}

void StemPlayer::updateEnd() {
    int64_t end = bundle_ ? bundle_->frames() : 0;
    for (size_t s = 0; s < playEdits_.size(); ++s) {
        for (int i = 0; i < std::max(0, playCount_[s]); ++i) {
            const Clip& c = playEdits_[s][static_cast<size_t>(i)];
            end = std::max(end, c.at + c.length);
        }
    }
    playEnd_ = end;
}

void StemPlayer::scaleEdits(const StemBundle* b, std::array<int, StemBundle::kStems>& count,
                            std::array<std::array<Clip, kMaxClips>, StemBundle::kStems>& out) const {
    const double k = b ? b->scale() : 1.0;
    auto scaled = [k](int64_t v) { return static_cast<int64_t>(std::llround(static_cast<double>(v) * k)); };
    for (size_t s = 0; s < edits_.size(); ++s) {
        count[s] = editCount_[s];
        for (int i = 0; i < std::max(0, editCount_[s]); ++i) {
            const Clip& c = edits_[s][static_cast<size_t>(i)];
            Clip& d = out[s][static_cast<size_t>(i)];
            d.at = scaled(c.at);
            d.length = scaled(c.length);
            d.from = scaled(c.from);
            d.fadeIn = static_cast<int32_t>(scaled(c.fadeIn));
            d.fadeOut = static_cast<int32_t>(scaled(c.fadeOut));
        }
    }
}

int64_t StemPlayer::sourceOf(const Clip* clips, int count, int64_t pos, int64_t frames, int edge, int& cursor, float& gain) {
    gain = 1.0f;
    if (count < 0) return pos < frames ? pos : -1;
    // The piece found last first (it plays on for many frames), else the latest one holding pos.
    int hit = -1;
    if (cursor >= 0 && cursor < count) {
        const Clip& c = clips[cursor];
        if (pos >= c.at && pos < c.at + c.length) {
            hit = cursor;
            // A later piece starting inside it covers it.
            for (int i = count - 1; i > cursor; --i) {
                if (pos >= clips[i].at && pos < clips[i].at + clips[i].length) {
                    hit = i;
                    break;
                }
            }
        }
    }
    if (hit < 0) {
        for (int i = count - 1; i >= 0; --i) {
            if (pos >= clips[i].at && pos < clips[i].at + clips[i].length) {
                hit = i;
                break;
            }
        }
    }
    if (hit < 0) return -1;
    cursor = hit;
    const Clip& c = clips[hit];
    const int64_t src = c.from + (pos - c.at);
    if (src < 0 || src >= frames) return -1;
    const int64_t in = pos - c.at, out = c.at + c.length - 1 - pos;
    const int fi = std::max(edge, static_cast<int>(c.fadeIn)), fo = std::max(edge, static_cast<int>(c.fadeOut));
    if (in < fi) gain *= static_cast<float>(in + 1) / static_cast<float>(fi);
    if (out < fo) gain *= static_cast<float>(out + 1) / static_cast<float>(fo);
    return src;
}

void StemPlayer::updateTargets() {
    bool anySolo = false;
    for (bool s : solo_) anySolo |= s;
    for (size_t i = 0; i < smooth_.size(); ++i) {
        const bool audible = !mute_[i] && (!anySolo || solo_[i]);
        const float g = gain_[i] / 0.8f;  // same fader law as the mixer: 0.8 = unity
        smooth_[i].setTarget(audible ? g * g : 0.0f);
    }
}

void StemPlayer::start(int offset, double steps, double stepFrames) {
    if (!bundle_) return;
    pendingStart_ = std::max(0, offset);
    pendingSteps_ = steps;
    pendingStepFrames_ = stepFrames;
}

void StemPlayer::stop() {
    pendingStart_ = -1;
    if (playing_) fadeLeft_ = fadeLength_;
}

void StemPlayer::setLive(bool on, int offset) {
    if (on == live_) return;
    live_ = on;
    liveAt_ = std::max(0, offset);
}

void StemPlayer::render(float* left, float* right, int numFrames) {
    // Frames of this block before the live gate may move (-1: it moves from the first one). Silent,
    // it simply is where it should be by the next start.
    const int wait = liveAt_;
    liveAt_ = liveAt_ >= numFrames ? liveAt_ - numFrames : -1;
    int at = -1;
    if (pendingStart_ >= 0 && pendingStart_ < numFrames) {
        at = pendingStart_;
        pendingStart_ = -1;
    } else if (pendingStart_ >= numFrames) {
        pendingStart_ -= numFrames;
    }
    if (at < 0 || !bundle_) {
        renderFrames(left, right, 0, numFrames, wait);
    } else {
        renderFrames(left, right, 0, at, wait);
        // What was playing fades out where it was; the song starts again from its start frame.
        StemBundle* b = bundle_;
        if (playing_) {
            b->retain();  // the fading copy holds its own reference
            fadeAway(fadeLength_);
            bundle_ = b;
        }
        pos_ = startFrame_;
        playStart_ = startFrame_;
        startSteps_ = pendingSteps_;
        leadSteps_ = startFrame_ < 0 && pendingStepFrames_ > 1.0 ? static_cast<double>(-startFrame_) / pendingStepFrames_ : 0.0;
        playing_ = true;
        fadeLeft_ = 0;
        fadeIn_ = fadeInLength_ = fadeLength_;
        renderFrames(left, right, at, numFrames, wait);
    }
    position_.store(playing_ && pos_ >= 0 ? pos_ : -1, std::memory_order_relaxed);
}

void StemPlayer::renderFrames(float* left, float* right, int from, int to, int wait) {
    const float liveTarget = live_ ? 1.0f : 0.0f;
    if (from >= to) return;
    if (!playing_ && !fading()) {
        if (wait < to) liveGain_ = liveTarget;
        return;
    }
    const float liveStep = 1.0f / static_cast<float>(fadeLength_);
    constexpr float kScale = kHeadroom / 32768.0f;
    for (int i = from; i < to; ++i) {
        float g[StemBundle::kStems];
        for (int s = 0; s < StemBundle::kStems; ++s) g[s] = smooth_[static_cast<size_t>(s)].next() * kScale;
        float l = 0.0f, r = 0.0f;
        if (playing_ && pos_ >= playEnd_) playing_ = false;
        if (playing_ && pos_ < 0 && fadeLeft_ == 0) {
            // The song comes in later: silence up to its first frame (the fade in waits for it).
            ++pos_;
        } else if (playing_) {
            float fade = 1.0f;
            bool last = false;
            if (fadeLeft_ > 0) {
                fade = static_cast<float>(fadeLeft_) / static_cast<float>(fadeLength_);
                last = --fadeLeft_ == 0;
            }
            if (fadeIn_ > 0) {
                fade *= 1.0f - static_cast<float>(fadeIn_) / static_cast<float>(fadeInLength_);
                --fadeIn_;
            }
            if (last) {
                playing_ = false;
            } else {
                if (pos_ >= 0) {
                    for (int s = 0; s < StemBundle::kStems; ++s) {
                        const auto si = static_cast<size_t>(s);
                        float edit = 1.0f;
                        const int64_t src = sourceOf(playEdits_[si].data(), playCount_[si], pos_, bundle_->frames(), edge_, cursor_[si], edit);
                        if (src < 0) continue;
                        const int16_t* d = bundle_->stem(s) + src * 2;
                        const float k = g[s] * fade * edit;
                        l += d[0] * k;
                        r += d[1] * k;
                    }
                }
                ++pos_;
            }
        }
        for (auto& out : outs_) {
            if (!out.bundle) continue;
            const float fade = static_cast<float>(out.left) / static_cast<float>(out.length);
            if (out.pos >= 0) {
                for (int s = 0; s < StemBundle::kStems; ++s) {
                    const auto si = static_cast<size_t>(s);
                    float edit = 1.0f;
                    const int64_t src = sourceOf(out.clips[si].data(), out.count[si], out.pos, out.bundle->frames(), edge_, out.cursor[si], edit);
                    if (src < 0) continue;
                    const int16_t* d = out.bundle->stem(s) + src * 2;
                    const float k = g[s] * fade * edit;
                    l += d[0] * k;
                    r += d[1] * k;
                }
            }
            ++out.pos;
            if (--out.left <= 0) {
                retire(out.bundle);
                out.bundle = nullptr;
            }
        }
        // The remix slot stopped or launched: a short ramp from its frame (exactly 1 when heard).
        if (liveGain_ != liveTarget && i >= wait) {
            liveGain_ = live_ ? std::min(1.0f, liveGain_ + liveStep) : std::max(0.0f, liveGain_ - liveStep);
        }
        left[i] += l * liveGain_;
        right[i] += r * liveGain_;
        if (!playing_ && !fading()) {
            if (wait < to) liveGain_ = liveTarget;
            return;
        }
    }
}


// --- RemixCells (lot D2: « case Remix du Live ») ---------------------------------------------

RemixCells::~RemixCells() {
    for (auto& c : cells_) retire(c.sample);
    retire(const_cast<Sample*>(voice_.sample));
    for (auto& t : tails_) retire(const_cast<Sample*>(t.sample));
    collectGarbage();
}

void RemixCells::retire(Sample* s) {
    if (s && s->release() && !retired_.push(s)) {
        // Queue full (64 releases without a collection): leak rather than free here.
    }
}

void RemixCells::collectGarbage() {
    Sample* s = nullptr;
    while (retired_.pop(s)) delete s;
}

void RemixCells::install(int cell, Sample* sample, int loopSteps, bool refit) {
    if (cell < 0 || cell >= kCells || (sample && (sample->frames() <= 0 || !sample->data()))) {
        retire(sample);
        return;
    }
    Cell& c = cells_[static_cast<size_t>(cell)];
    const int steps = std::clamp(loopSteps, 1, 1 << 16);
    if (sample == c.sample) {
        retire(sample);  // the same passage again (a resync): the extra reference goes
        c.loopSteps = steps;
        return;
    }
    Sample* old = c.sample;
    c.sample = sample;
    c.loopSteps = steps;
    // The cell heard: the new copy takes over at the same place (a refit: the same passage at another
    // tempo; else another passage), cross-faded; emptied, it fades out.
    if (cell == cell_) {
        if (!sample) {
            toTail(stopFade_);
            cell_ = -1;
            heard_.store(-1, std::memory_order_relaxed);
        } else if (voice_.sample) {
            const double local = voice_.local;
            toTail(crossFade_);
            begin(cell, refit ? std::fmod(std::max(0.0, local), static_cast<double>(steps)) : std::max(0.0, local));
            voice_.rise = voice_.riseLen = crossFade_;
        }
    }
    retire(old);
}

void RemixCells::toTail(int frames) {
    if (!voice_.sample) return;
    // A free tail, else the one closest to its end.
    const size_t i = !tails_[0].sample ? 0 : !tails_[1].sample ? 1 : tails_[0].fade <= tails_[1].fade ? 0 : 1;
    Voice& t = tails_[i];
    retire(const_cast<Sample*>(t.sample));
    t = voice_;
    t.fade = t.fadeLen = std::max(1, frames);
    voice_ = Voice{};
}

void RemixCells::begin(int cell, double local) {
    // The same passage already at that place (a song part that carries on, the scene sent again):
    // it goes on, pulled to the place over the next beat.
    if (cell == cell_ && voice_.sample && voice_.sample == cells_[static_cast<size_t>(cell)].sample && local >= 0.0) {
        const double loop = voice_.loopSteps;
        double err = std::fmod(local - voice_.local, loop);
        if (err > loop * 0.5) err -= loop;
        if (err < -loop * 0.5) err += loop;
        if (std::fabs(err) < 0.05) {
            correction_ = err / (4.0 * stepFrames_);
            return;
        }
    }
    toTail(crossFade_);
    const Cell& c = cells_[static_cast<size_t>(cell)];
    cell_ = cell;
    heard_.store(c.sample ? cell : -1, std::memory_order_relaxed);
    correction_ = 0.0;
    if (!c.sample) return;
    c.sample->retain();  // the voice holds its own reference (a refit may retire the cell's meanwhile)
    voice_.sample = c.sample;
    voice_.loopSteps = c.loopSteps;
    voice_.local = local;
    voice_.fade = 0;
    // From its first frame: at full level, sample-accurate (the passage has its own short fades).
    double phase = local / c.loopSteps;
    phase -= std::floor(phase);
    const bool fromStart = phase * static_cast<double>(c.sample->frames()) < 0.5;
    voice_.rise = voice_.riseLen = fromStart ? 0 : stopFade_;
    if (voice_.riseLen == 0) voice_.riseLen = 1;
}

void RemixCells::play(int cell, int offset, double local) {
    if (cell < 0 || cell >= kCells || !std::isfinite(local)) return;
    stopAt_ = -1;
    startAt_ = std::max(0, offset);
    startCell_ = cell;
    startLocal_ = local;
}

void RemixCells::stop(int offset) {
    startAt_ = -1;
    if (cell_ < 0 && !voice_.sample) return;
    stopAt_ = std::max(0, offset);
}

void RemixCells::sync(int offset, double local) {
    if (!std::isfinite(local)) return;
    syncAt_ = std::max(0, offset);
    syncLocal_ = local;
}

float RemixCells::read(const Voice& v, float& r) const {
    const Sample* s = v.sample;
    const int64_t frames = s->frames();
    const int ch = s->channels();
    const float* d = s->data();
    if (v.local < 0.0) {
        r = 0.0f;
        return 0.0f;
    }
    double phase = v.local / v.loopSteps;
    phase -= std::floor(phase);
    const double x = phase * static_cast<double>(frames);
    const double loopFrames = v.loopSteps * stepFrames_;
    if (std::fabs(static_cast<double>(frames) - loopFrames) < 2.0) {
        // The copy for this tempo: read as it is, frame by frame (no interpolation dulling it).
        const int64_t i = static_cast<int64_t>(std::llround(x)) % frames;
        const float l = d[i * ch];
        r = ch > 1 ? d[i * ch + 1] : l;
        return l;
    }
    // Another tempo's copy, until its refit: read faster or slower (4-point Hermite), on the beat.
    const auto i1 = static_cast<int64_t>(x);
    const float t = static_cast<float>(x - static_cast<double>(i1));
    auto at = [&](int64_t i, int c) {
        i %= frames;
        if (i < 0) i += frames;
        return d[i * ch + (ch > 1 ? c : 0)];
    };
    auto hermite = [&](int c) {
        const float y0 = at(i1 - 1, c), y1 = at(i1, c), y2 = at(i1 + 1, c), y3 = at(i1 + 2, c);
        const float c1 = 0.5f * (y2 - y0);
        const float c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
        const float c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2);
        return ((c3 * t + c2) * t + c1) * t + y1;
    };
    r = hermite(1);
    return hermite(0);
}

void RemixCells::render(float* left, float* right, int numFrames) {
    if (!active() && stopAt_ < 0 && syncAt_ < 0) return;
    const double step = 1.0 / stepFrames_;
    for (int i = 0; i < numFrames; ++i) {
        if (i == stopAt_) {
            toTail(stopFade_);
            cell_ = -1;
            heard_.store(-1, std::memory_order_relaxed);
            stopAt_ = -1;
        }
        if (i == startAt_) {
            begin(startCell_, startLocal_);
            startAt_ = -1;
        }
        if (i == syncAt_) {
            syncAt_ = -1;
            if (voice_.sample) {
                const double loop = voice_.loopSteps;
                double err = std::fmod(syncLocal_ - voice_.local, loop);
                if (err > loop * 0.5) err -= loop;
                if (err < -loop * 0.5) err += loop;
                if (std::fabs(err) > 0.5) {
                    // The transport moved (a jump): picked up at its place, cross-faded.
                    const int cell = cell_;
                    begin(cell, syncLocal_);
                    voice_.rise = voice_.riseLen = crossFade_;
                } else {
                    // Pulled to the beat over the next one (a tempo change within a block): no jump.
                    correction_ = err / (4.0 * stepFrames_);
                }
            }
        }
        float l = 0.0f, r = 0.0f;
        if (voice_.sample) {
            float vr = 0.0f;
            const float vl = read(voice_, vr);
            float g = 1.0f;
            if (voice_.rise > 0) g = 1.0f - static_cast<float>(voice_.rise--) / static_cast<float>(voice_.riseLen);
            l += vl * g;
            r += vr * g;
            voice_.local += step + correction_;
        }
        for (auto& t : tails_) {
            if (!t.sample) continue;
            float tr = 0.0f;
            const float tl = read(t, tr);
            float g = static_cast<float>(t.fade) / static_cast<float>(t.fadeLen);
            if (t.rise > 0) g *= 1.0f - static_cast<float>(t.rise--) / static_cast<float>(t.riseLen);
            l += tl * g;
            r += tr * g;
            t.local += step;
            if (--t.fade <= 0) {
                retire(const_cast<Sample*>(t.sample));
                t = Voice{};
            }
        }
        left[i] += l;
        right[i] += r;
    }
    if (stopAt_ >= numFrames) stopAt_ -= numFrames;
    if (startAt_ >= numFrames) startAt_ -= numFrames;
    if (syncAt_ >= numFrames) syncAt_ -= numFrames;
}

}  // namespace ps
