#include "AudioTracks.h"

#include <cmath>

namespace ps {

AudioTracks::~AudioTracks() {
    for (auto& t : tracks_) {
        if (t.sample && t.sample->release()) delete t.sample;
        t.sample = nullptr;
    }
    collectGarbage();
}

void AudioTracks::retire(Sample* s) {
    if (s && s->release() && !retired_.push(s)) {
        // Queue full (64 releases without a collection): leak rather than free here.
    }
}

void AudioTracks::collectGarbage() {
    Sample* s = nullptr;
    while (retired_.pop(s)) delete s;
}

void AudioTracks::install(int track, Sample* sample) {
    if (track < 0 || track >= kTracks) {
        retire(sample);
        return;
    }
    Track& t = tracks_[static_cast<size_t>(track)];
    if (sample == t.sample) {  // the same clip again (resync, undo): keep playing
        retire(sample);
        return;
    }
    // Voices may still point at the old clip: silence them before it can be freed.
    t.voice = Voice{};
    t.tail = Voice{};
    retire(t.sample);
    t.sample = sample;
}

void AudioTracks::setParam(int track, int param, float value) {
    if (track < 0 || track >= kTracks || !std::isfinite(value)) return;
    Track& t = tracks_[static_cast<size_t>(track)];
    switch (param) {
        case Start: t.start = std::clamp(static_cast<int>(std::lround(value)), 0, 4096); break;
        case LoopSteps: t.loopSteps = std::clamp(static_cast<int>(std::lround(value)), 1, 4096); break;
        case Gain: t.gain = std::clamp(value, 0.0f, 4.0f); break;
        case FadeIn: t.fadeIn = std::clamp(static_cast<int>(value), 0, 48000 * 10); break;
        case FadeOut: t.fadeOut = std::clamp(static_cast<int>(value), 0, 48000 * 10); break;
        default: break;
    }
}

void AudioTracks::setOn(int track, bool on) {
    if (track < 0 || track >= kTracks) return;
    Track& t = tracks_[static_cast<size_t>(track)];
    t.on = on;
    if (!on && t.voice.sample && t.voice.fade == 0) t.voice.fade = declick_;
}

void AudioTracks::onStep(int offset, int local) {
    for (auto& t : tracks_) {
        if (!t.sample || !t.on || local < t.start || (local - t.start) % t.loopSteps != 0) continue;
        // The previous pass, if still sounding, fades out under the new one.
        if (t.voice.sample) {
            t.tail = t.voice;
            t.tail.fade = declick_;
        }
        t.voice = Voice{t.sample, 0, offset, 0};
    }
}

void AudioTracks::stop() {
    for (auto& t : tracks_) {
        if (t.voice.sample && t.voice.fade == 0) t.voice.fade = declick_;
        t.tail = Voice{};
    }
}

void AudioTracks::play(const Track& t, Voice& v, float& l, float& r) {
    if (!v.sample) return;
    if (v.due > 0) {
        --v.due;
        return;
    }
    const int64_t frames = v.sample->frames();
    if (v.pos >= frames) {
        v = Voice{};
        return;
    }
    float g = t.gain;
    if (t.fadeIn > 0 && v.pos < t.fadeIn) g *= static_cast<float>(v.pos) / static_cast<float>(t.fadeIn);
    if (t.fadeOut > 0 && frames - v.pos < t.fadeOut) g *= static_cast<float>(frames - v.pos) / static_cast<float>(t.fadeOut);
    if (v.fade > 0) {
        g *= static_cast<float>(v.fade) / static_cast<float>(declick_);
        if (--v.fade == 0) {
            v = Voice{};
            return;
        }
    }
    const float* d = v.sample->data();
    if (v.sample->channels() == 2) {
        l += d[v.pos * 2] * g;
        r += d[v.pos * 2 + 1] * g;
    } else {
        l += d[v.pos] * g;
        r += d[v.pos] * g;
    }
    ++v.pos;
}

void AudioTracks::render(float* const* lefts, float* const* rights, int numFrames) {
    for (int k = 0; k < kTracks; ++k) {
        Track& t = tracks_[static_cast<size_t>(k)];
        if (!t.voice.sample && !t.tail.sample) continue;
        float* L = lefts[k];
        float* R = rights[k];
        for (int i = 0; i < numFrames; ++i) {
            float l = 0.0f, r = 0.0f;
            play(t, t.voice, l, r);
            play(t, t.tail, l, r);
            L[i] += l;
            R[i] += r;
        }
    }
}

}  // namespace ps
