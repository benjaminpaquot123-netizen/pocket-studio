#include "StemPlayer.h"

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

StemBundle* StemBundle::fromFloat(const std::vector<std::vector<float>>& stereo) {
    if (stereo.size() != kStems) return nullptr;
    auto* b = new StemBundle();
    for (int i = 0; i < kStems; ++i) {
        const auto& src = stereo[static_cast<size_t>(i)];
        auto& dst = b->data_[static_cast<size_t>(i)];
        dst.resize(src.size());
        std::transform(src.begin(), src.end(), dst.begin(), toPcm);
    }
    b->frames_ = static_cast<int64_t>(stereo[0].size() / 2);
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
    collectGarbage();
}

void StemPlayer::prepare(int sampleRate) {
    for (auto& s : smooth_) s.configure(static_cast<float>(sampleRate), 20.0f);
    fadeLength_ = std::max(1, sampleRate / 200);  // 5 ms
    updateTargets();
    for (auto& s : smooth_) s.snap(s.target());
}

void StemPlayer::retire(StemBundle* b) {
    if (b && b->release() && !retired_.push(b)) {
        // Queue full (16 songs swapped without a collection): leak rather than free here.
    }
}

void StemPlayer::install(StemBundle* bundle) {
    retire(bundle_);
    bundle_ = bundle;
    if (!bundle_) {
        playing_ = false;
        position_.store(-1, std::memory_order_relaxed);
    }
}

void StemPlayer::collectGarbage() {
    StemBundle* b = nullptr;
    while (retired_.pop(b)) delete b;
}

void StemPlayer::setParam(int stem, int param, float value) {
    if (stem < 0 || stem >= StemBundle::kStems) return;
    const auto i = static_cast<size_t>(stem);
    switch (param) {
        case Gain: gain_[i] = std::clamp(value, 0.0f, 1.0f); break;
        case Mute: mute_[i] = value > 0.5f; break;
        case Solo: solo_[i] = value > 0.5f; break;
        default: return;
    }
    updateTargets();
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

void StemPlayer::start(int offset) {
    if (!bundle_) return;
    pendingStart_ = std::max(0, offset);
    fadeLeft_ = 0;
}

void StemPlayer::stop() {
    pendingStart_ = -1;
    if (playing_) fadeLeft_ = fadeLength_;
}

void StemPlayer::render(float* left, float* right, int numFrames) {
    if (!bundle_) return;
    int from = 0;
    if (pendingStart_ >= 0 && pendingStart_ < numFrames) {
        from = pendingStart_;
        pendingStart_ = -1;
        pos_ = startFrame_;
        playing_ = true;
    } else if (pendingStart_ >= numFrames) {
        pendingStart_ -= numFrames;
    }
    if (!playing_) return;
    const int64_t total = bundle_->frames();
    const int16_t* stems[StemBundle::kStems];
    for (int s = 0; s < StemBundle::kStems; ++s) stems[s] = bundle_->stem(s);
    constexpr float kScale = kHeadroom / 32768.0f;
    for (int i = from; i < numFrames; ++i) {
        if (pos_ >= total) {
            playing_ = false;
            break;
        }
        float fade = 1.0f;
        if (fadeLeft_ > 0) {
            fade = static_cast<float>(fadeLeft_) / static_cast<float>(fadeLength_);
            if (--fadeLeft_ == 0) {
                playing_ = false;
                break;
            }
        }
        float l = 0.0f, r = 0.0f;
        const auto at = static_cast<size_t>(pos_) * 2;
        for (int s = 0; s < StemBundle::kStems; ++s) {
            const float g = smooth_[static_cast<size_t>(s)].next() * kScale;
            l += stems[s][at] * g;
            r += stems[s][at + 1] * g;
        }
        left[i] += l * fade;
        right[i] += r * fade;
        ++pos_;
    }
    position_.store(playing_ ? pos_ : -1, std::memory_order_relaxed);
}

}  // namespace ps
