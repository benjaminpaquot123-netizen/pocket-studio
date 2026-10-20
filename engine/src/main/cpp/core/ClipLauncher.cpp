#include "ClipLauncher.h"

#include <cmath>

namespace ps {

ClipLauncher::~ClipLauncher() {
    for (auto& s : slots_) {
        if (s.sample && s.sample->release()) delete s.sample;
        s.sample = nullptr;
    }
    collectGarbage();
}

void ClipLauncher::retire(Sample* s) {
    if (s && s->release() && !retired_.push(s)) {
        // Queue full (64 releases without a collection): leak rather than free here.
    }
}

void ClipLauncher::collectGarbage() {
    Sample* s = nullptr;
    while (retired_.pop(s)) delete s;
}

void ClipLauncher::install(int slot, Sample* sample) {
    if (slot < 0 || slot >= kSlots) {
        retire(sample);
        return;
    }
    Slot& s = slots_[static_cast<size_t>(slot)];
    // Same sound again (resync, undo): keep playing, drop the extra reference.
    if (sample == s.sample) {
        retire(sample);
        return;
    }
    // Voices may still point at the old sample: silence them before it can be freed.
    s.voice = Voice{};
    s.tail = Voice{};
    retire(s.sample);
    s.sample = sample;
    if (!sample) s.state = Off;
    states_[static_cast<size_t>(slot)].store(s.state, std::memory_order_relaxed);
}

void ClipLauncher::setParam(int slot, int param, float value) {
    if (slot < 0 || slot >= kSlots || !std::isfinite(value)) return;
    Slot& s = slots_[static_cast<size_t>(slot)];
    switch (param) {
        case Beats: s.beats = std::clamp(static_cast<int>(std::lround(value)), 1, kMaxBeats); break;
        case OneShot: s.oneShot = value > 0.5f; break;
        case Gain: s.gain = std::clamp(value, 0.0f, 2.0f); break;
        default: break;
    }
}

void ClipLauncher::toggle(int slot, bool idleClock) {
    if (slot < 0 || slot >= kSlots) return;
    Slot& s = slots_[static_cast<size_t>(slot)];
    if (!s.sample) return;
    if (s.state == Playing && !s.oneShot) {
        s.tail = s.voice;
        s.tail.fade = fadeFrames_;
        s.voice = Voice{};
        s.state = Off;
    } else if (s.state == Waiting) {
        s.state = Off;
    } else {
        // A one-shot that is still sounding is simply hit again on the grid.
        if (idleClock && std::none_of(slots_.begin(), slots_.end(), [](const Slot& o) { return o.state != Off; })) beat_ = 0.0;
        const double grid = gridOf(s);
        s.startBeat = std::ceil(beat_ / grid - 1e-9) * grid;
        s.state = Waiting;
    }
    states_[static_cast<size_t>(slot)].store(s.state, std::memory_order_relaxed);
}

void ClipLauncher::stopAll() {
    for (size_t i = 0; i < slots_.size(); ++i) {
        Slot& s = slots_[i];
        if (s.voice.sample) {
            s.tail = s.voice;
            s.tail.fade = fadeFrames_;
            s.voice = Voice{};
        }
        s.state = Off;
        states_[i].store(Off, std::memory_order_relaxed);
    }
}

void ClipLauncher::restart(Slot& s) {
    // The previous pass, if still sounding, fades out instead of being cut.
    if (s.voice.sample) {
        s.tail = s.voice;
        s.tail.fade = fadeFrames_;
    }
    s.voice = Voice{s.sample, 0, 0};
}

void ClipLauncher::play(Voice& v, float gain, float& l, float& r) {
    if (!v.sample) return;
    if (v.pos >= v.sample->frames()) {
        v = Voice{};
        return;
    }
    float g = gain;
    if (v.fade > 0) {
        g *= static_cast<float>(v.fade) / static_cast<float>(fadeFrames_);
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

void ClipLauncher::render(float* left, float* right, int numFrames) {
    const double perFrame = 1.0 / beatFrames_;
    for (int i = 0; i < numFrames; ++i) {
        if (i == syncAt_) beat_ = std::round(beat_);
        float l = 0.0f, r = 0.0f;
        for (size_t k = 0; k < slots_.size(); ++k) {
            Slot& s = slots_[k];
            if (!s.sample) continue;
            if (s.state == Waiting && beat_ >= s.startBeat) {
                s.state = Playing;
                restart(s);
                states_[k].store(Playing, std::memory_order_relaxed);
            } else if (s.state == Playing && !s.oneShot && beat_ >= s.startBeat + s.beats) {
                s.startBeat += s.beats;
                // Far behind (the clock snapped or jumped): line up with the clock again.
                if (beat_ >= s.startBeat + s.beats) s.startBeat = std::floor(beat_ / s.beats) * s.beats;
                restart(s);
            }
            play(s.voice, s.gain, l, r);
            play(s.tail, s.gain, l, r);
            if (s.state == Playing && s.oneShot && !s.voice.sample) {
                s.state = Off;
                states_[k].store(Off, std::memory_order_relaxed);
            }
        }
        left[i] += l;
        right[i] += r;
        beat_ += perFrame;
    }
    syncAt_ = -1;
}

}  // namespace ps
