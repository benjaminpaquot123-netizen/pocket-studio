#include "ClipLauncher.h"

#include <cmath>

namespace ps {

ClipLauncher::~ClipLauncher() {
    for (auto& s : slots_) {
        if (s.sample && s.sample->release()) delete s.sample;
        if (s.held && s.held->release()) delete s.held;
        s.sample = s.held = nullptr;
    }
    collectGarbage();
}

void ClipLauncher::releaseHeld(Slot& s) {
    if (!s.held) return;
    if (s.voice.sample == s.held) s.voice = Voice{};
    if (s.tail.sample == s.held) s.tail = Voice{};
    retire(s.held);
    s.held = nullptr;
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

void ClipLauncher::install(int slot, Sample* sample, bool refit) {
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
    const bool picksUp = refit && sample && s.state == Playing && !s.oneShot && s.voice.sample && s.voice.fade == 0;
    // A sound replaced before (still fading): cut now, it is let go.
    releaseHeld(s);
    // The sounding pass fades out (audit 13: it was cut, a click), the old sound held until it is
    // done; render() lets it go then.
    if (s.voice.sample) {
        s.tail = s.voice;
        if (s.tail.fade == 0) s.tail.fade = fadeFrames_;
    }
    s.voice = Voice{};
    if (s.tail.sample && s.tail.sample == s.sample) {
        s.held = s.sample;
    } else {
        s.tail = Voice{};
        retire(s.sample);
    }
    s.sample = sample;
    if (!sample) s.state = Off;
    // A loop refitted to a new tempo: where the beat clock is in its pass, faded in.
    if (picksUp) {
        const auto pos = static_cast<int64_t>((beat_ - s.startBeat) * beatFrames_);
        if (pos >= 0 && pos < sample->frames()) s.voice = Voice{sample, pos, 0, fadeFrames_, s.velocity};
    }
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

void ClipLauncher::toggle(int slot, bool idleClock, float velocity) {
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
        s.velocity = std::isfinite(velocity) && velocity > 0.0f ? std::min(velocity, 1.0f) : 1.0f;
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
    s.voice = Voice{s.sample, 0, 0, 0, s.velocity};
}

void ClipLauncher::play(Voice& v, float gain, float& l, float& r) {
    if (!v.sample) return;
    if (v.pos >= v.sample->frames()) {
        v = Voice{};
        return;
    }
    float g = gain * v.level;
    if (v.fade > 0) {
        g *= static_cast<float>(v.fade) / static_cast<float>(fadeFrames_);
        if (--v.fade == 0) {
            v = Voice{};
            return;
        }
    }
    if (v.rise > 0) g *= 1.0f - static_cast<float>(v.rise--) / static_cast<float>(fadeFrames_);
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

void ClipLauncher::align() {
    const double was = std::round(beat_);
    // The nearest clock value with the sequencer's place in a kMaxBeats cycle.
    const int cycleBeats = beatsPerBar_ == 4 ? kMaxBeats : beatsPerBar_ * 16;
    const double cycle = cycleBeats;
    const double want = static_cast<double>(((syncBeat_ % cycleBeats) + cycleBeats) % cycleBeats);
    double target = was - std::fmod(was, cycle) + want;
    if (target - was > cycle / 2) target -= cycle;
    else if (was - target > cycle / 2) target += cycle;
    beat_ = target;
    if (target == was) return;
    // The clock jumped (the transport just started): loops start over with it, waiting clips
    // start on its grid.
    for (Slot& s : slots_) {
        if (s.state == Waiting) {
            const double grid = gridOf(s);
            s.startBeat = std::ceil(beat_ / grid - 1e-9) * grid;
        } else if (s.state == Playing && !s.oneShot) {
            s.startBeat = std::floor(beat_ / s.beats) * s.beats;
            restart(s);
        }
    }
}

void ClipLauncher::render(float* left, float* right, int numFrames) {
    const double perFrame = 1.0 / beatFrames_;
    for (int i = 0; i < numFrames; ++i) {
        if (i == syncAt_) align();
        float l = 0.0f, r = 0.0f;
        for (size_t k = 0; k < slots_.size(); ++k) {
            Slot& s = slots_[k];
            if (!s.sample) {
                // Emptied while it played: its last pass fades out.
                play(s.tail, s.gain, l, r);
                continue;
            }
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
    for (Slot& s : slots_) {
        if (s.held && s.voice.sample != s.held && s.tail.sample != s.held) releaseHeld(s);
    }
    syncAt_ = -1;
}

}  // namespace ps
