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
    t.resync = true;  // a song clip replaced while the song plays picks up where the song is
}

void AudioTracks::setParam(int track, int param, float value) {
    if (track < 0 || track >= kTracks || !std::isfinite(value)) return;
    Track& t = tracks_[static_cast<size_t>(track)];
    switch (param) {
        case Start: {
            const int start = std::clamp(static_cast<int>(std::lround(value)), 0, 65535);
            if (start != t.start && t.song) t.resync = true;
            t.start = start;
            break;
        }
        case LoopSteps: t.loopSteps = std::clamp(static_cast<int>(std::lround(value)), 0, 4096); break;
        case Gain: t.gain = std::clamp(value, 0.0f, 4.0f); break;
        case FadeIn: t.fadeIn = std::clamp(static_cast<int>(value), 0, 48000 * 10); break;
        case FadeOut: t.fadeOut = std::clamp(static_cast<int>(value), 0, 48000 * 10); break;
        case Song: {
            const bool song = value > 0.5f;
            if (song == t.song) break;
            t.song = song;
            t.resync = true;
            t.level = t.on ? 1.0f : 0.0f;
            // What played in the other mode fades: the clip starts again from its new anchor.
            if (t.voice.sample && t.voice.fade == 0) t.voice.fade = declick_;
            break;
        }
        case Offset: {
            const int frames = std::clamp(static_cast<int>(std::lround(value)), -kMaxOffset, kMaxOffset);
            if (frames != t.offset && t.song) t.resync = true;
            t.offset = frames;
            break;
        }
        default: break;
    }
}

void AudioTracks::setOn(int track, bool on) {
    if (track < 0 || track >= kTracks) return;
    Track& t = tracks_[static_cast<size_t>(track)];
    t.on = on;
    // A song clip keeps its place: it only goes quiet (render() ramps its level).
    if (t.song) return;
    if (!on && t.voice.sample && t.voice.fade == 0) t.voice.fade = declick_;
}

void AudioTracks::songJump() {
    for (auto& t : tracks_) {
        if (!t.song) continue;
        t.resync = true;
        if (t.voice.sample && t.voice.fade == 0) t.voice.fade = declick_;
    }
}

// The clip `pos` frames past its step (before the offset) starts sounding `offset` frames into the block.
void AudioTracks::startVoice(Track& t, int offset, int64_t pos) {
    // The previous pass, if still sounding, fades out under the new one.
    if (t.voice.sample) {
        t.tail = t.voice;
        if (t.tail.fade == 0) t.tail.fade = declick_;
    }
    const int64_t at = pos - t.offset;  // a later clip (offset > 0) is that much behind
    t.voice = at >= 0 ? Voice{t.sample, at, offset, 0} : Voice{t.sample, 0, offset - at, 0};
}

void AudioTracks::onStep(int offset, const std::array<int, kTracks>& locals, int songStep, double samplesPerStep, double swing, int songOrigin) {
    for (size_t k = 0; k < tracks_.size(); ++k) {
        auto& t = tracks_[k];
        const int local = locals[k];
        if (!t.sample) continue;
        if (t.song) {
            if (songStep < 0) continue;
            const bool resync = t.resync;
            t.resync = false;
            if (songStep == t.start) {
                startVoice(t, offset, 0);
            } else if (resync && songStep > t.start && samplesPerStep > 0.0) {
                // Started in its middle: where it would be had it played from its step (odd
                // sequencer steps sound `swing` of a step late).
                const auto late = [&](int s) { return ((s + songOrigin) & 1) != 0 ? swing * samplesPerStep : 0.0; };
                const double elapsed = (songStep - t.start) * samplesPerStep + late(songStep) - late(t.start);
                const auto pos = static_cast<int64_t>(std::llround(elapsed));
                if (pos - t.offset < t.sample->frames()) startVoice(t, offset, pos);
            }
            continue;
        }
        if (!t.on || local < t.start) continue;
        const int since = local - t.start;
        if (t.loopSteps > 0 ? since % t.loopSteps != 0 : since != 0) continue;
        startVoice(t, offset, 0);
    }
}

void AudioTracks::stop() {
    for (auto& t : tracks_) {
        if (t.voice.sample && t.voice.fade == 0) t.voice.fade = declick_;
        t.tail = Voice{};
        t.resync = true;
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
    float g = t.gain * t.level;
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
        const float target = !t.song || t.on ? 1.0f : 0.0f;
        if (!t.voice.sample && !t.tail.sample) {
            t.level = target;
            continue;
        }
        const float ramp = 1.0f / static_cast<float>(declick_);
        float* L = lefts[k];
        float* R = rights[k];
        for (int i = 0; i < numFrames; ++i) {
            if (t.level != target) t.level = t.level < target ? std::min(target, t.level + ramp) : std::max(target, t.level - ramp);
            float l = 0.0f, r = 0.0f;
            play(t, t.voice, l, r);
            play(t, t.tail, l, r);
            L[i] += l;
            R[i] += r;
        }
    }
}

}  // namespace ps
