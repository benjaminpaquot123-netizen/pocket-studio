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
    for (Voice& t : s.tails) {
        if (t.sample == s.held) t = Voice{};
    }
    retire(s.held);
    s.held = nullptr;
}

void ClipLauncher::fadeAway(Slot& s, int frames) {
    if (!s.voice.sample) return;
    Voice* to = &s.tails[0];
    for (Voice& t : s.tails) {
        if (!t.sample) {
            to = &t;
            break;
        }
        if (to->sample && t.fade < to->fade) to = &t;
    }
    *to = s.voice;
    if (to->fade == 0) {
        to->fadeLen = std::max(fadeFrames_, frames);
        to->fade = to->fadeLen;
    }
    s.voice = Voice{};
}

void ClipLauncher::stopSlot(Slot& s) {
    if (s.state == Playing && !s.oneShot) fadeAway(s, s.release);
    // A hit stopped by « Tout couper » or its row: it fades out too (a hit tapped again plays again).
    else if (s.state == Playing) fadeAway(s, s.release);
    s.state = Off;
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
    fadeAway(s);
    if (s.sample && s.tailOf(s.sample)) {
        s.held = s.sample;
    } else {
        retire(s.sample);
    }
    s.sample = sample;
    if (!sample) s.state = Off;
    // A loop refitted to a new tempo: where the beat clock is in its pass, faded in.
    if (picksUp) {
        const double pos = std::floor((beat_ - s.startBeat) * beatFrames_ * s.speed);
        if (pos >= 0.0 && pos < static_cast<double>(sample->frames())) s.voice = Voice{sample, pos, 0, fadeFrames_, s.velocity, 1, fadeFrames_};
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
        case Pitch: s.speed = std::exp2(static_cast<double>(std::clamp(value, -24.0f, 24.0f)) / 12.0); break;
        case Reverse: s.reverse = value > 0.5f; break;
        case Attack: s.attack = static_cast<int>(std::lround(std::clamp(value, 0.0f, kMaxAttack) * static_cast<float>(rate_))); break;
        case Release: s.release = static_cast<int>(std::lround(std::clamp(value, 0.0f, kMaxRelease) * static_cast<float>(rate_))); break;
        default: break;
    }
}

void ClipLauncher::toggle(int slot, bool idleClock, float velocity) {
    if (slot < 0 || slot >= kSlots) return;
    Slot& s = slots_[static_cast<size_t>(slot)];
    if (!s.sample) return;
    if (s.state == Playing && !s.oneShot) {
        stopSlot(s);
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

void ClipLauncher::launchRow(int first, int count, bool idleClock) {
    const int from = std::clamp(first, 0, kSlots);
    const int to = std::clamp(first + std::max(count, 0), from, kSlots);
    bool loops = false, allOn = true, any = false;
    for (int k = from; k < to; ++k) {
        const Slot& s = slots_[static_cast<size_t>(k)];
        if (!s.sample) continue;
        any = true;
        if (s.oneShot) continue;
        loops = true;
        if (s.state == Off) allOn = false;
    }
    if (!any) return;
    if (loops && allOn) {
        for (int k = from; k < to; ++k) {
            Slot& s = slots_[static_cast<size_t>(k)];
            if (!s.sample || s.oneShot) continue;
            stopSlot(s);
            states_[static_cast<size_t>(k)].store(Off, std::memory_order_relaxed);
        }
        return;
    }
    if (idleClock && std::none_of(slots_.begin(), slots_.end(), [](const Slot& o) { return o.state != Off; })) beat_ = 0.0;
    const double bar = beatsPerBar_;
    const double start = std::ceil(beat_ / bar - 1e-9) * bar;
    for (int k = from; k < to; ++k) {
        Slot& s = slots_[static_cast<size_t>(k)];
        if (!s.sample || (s.state == Playing && !s.oneShot)) continue;
        s.startBeat = start;
        if (s.state != Waiting) s.velocity = 1.0f;
        s.state = Waiting;
        states_[static_cast<size_t>(k)].store(Waiting, std::memory_order_relaxed);
    }
}

void ClipLauncher::stopAll() {
    for (size_t i = 0; i < slots_.size(); ++i) {
        Slot& s = slots_[i];
        stopSlot(s);
        states_[i].store(Off, std::memory_order_relaxed);
    }
}

void ClipLauncher::restart(Slot& s) {
    // The previous pass, if still sounding, fades out instead of being cut.
    fadeAway(s);
    s.voice = Voice{s.sample, 0.0, 0, s.attack, s.velocity, 1, std::max(1, s.attack)};
}

void ClipLauncher::publishLevels(int numFrames) {
    // A peak lights the pad at once; it then falls back over kLevelFallSeconds (one exp a block).
    const float fall = std::exp(-static_cast<float>(numFrames) / (kLevelFallSeconds * static_cast<float>(rate_)));
    for (size_t k = 0; k < slots_.size(); ++k) {
        const float was = level_[k] * fall;
        const float now = blockPeak_[k] > was ? blockPeak_[k] : (was < 1e-4f ? 0.0f : was);
        blockPeak_[k] = 0.0f;
        if (now == level_[k]) continue;
        level_[k] = now;
        levels_[k].store(now, std::memory_order_relaxed);
    }
}

void ClipLauncher::play(Voice& v, const Slot& s, float& l, float& r) {
    if (!v.sample) return;
    const int64_t frames = v.sample->frames();
    if (v.pos >= static_cast<double>(frames)) {
        v = Voice{};
        return;
    }
    float g = s.gain * v.level;
    if (v.fade > 0) {
        g *= static_cast<float>(v.fade) / static_cast<float>(v.fadeLen);
        if (--v.fade == 0) {
            v = Voice{};
            return;
        }
    }
    if (v.rise > 0) g *= 1.0f - static_cast<float>(v.rise--) / static_cast<float>(v.riseLen);
    const float* d = v.sample->data();
    const int ch = v.sample->channels();
    const auto i0 = static_cast<int64_t>(v.pos);
    const auto frac = static_cast<float>(v.pos - static_cast<double>(i0));
    auto at = [&](int64_t i, int c) {
        const int64_t k = s.reverse ? frames - 1 - i : i;
        return d[k * ch + (ch == 2 ? c : 0)];
    };
    float a = at(i0, 0), b = at(i0, 1);
    // Between two frames (a pad pitched): read in a straight line from one to the next.
    if (frac > 0.0f && i0 + 1 < frames) {
        a += frac * (at(i0 + 1, 0) - a);
        b += frac * (at(i0 + 1, 1) - b);
    }
    l += a * g;
    r += b * g;
    v.pos += s.speed;
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
    // Only the slots up to the last one with something sounding or waiting are walked each frame
    // (a slot only wakes up through a command, between blocks): Pad Live's pages left empty cost nothing.
    size_t used = 0;
    for (size_t k = 0; k < slots_.size(); ++k) {
        if (!slots_[k].idle()) used = k + 1;
    }
    barAt_ = -1;
    barSounding_ = false;
    for (int i = 0; i < numFrames; ++i) {
        if (i == syncAt_) align();
        // A bar line crossed (or the clock started over on one): the first of this block is kept.
        const auto bar = static_cast<int64_t>(std::floor(beat_ / beatsPerBar_ + 1e-9));
        if (bar != lastBar_) {
            lastBar_ = bar;
            if (barAt_ < 0 && beat_ - static_cast<double>(bar * beatsPerBar_) < 2.0 * perFrame) {
                barAt_ = i;
                barSounding_ = std::any_of(slots_.begin(), slots_.begin() + static_cast<std::ptrdiff_t>(used), [](const Slot& o) { return o.state != Off; });
            }
        }
        float l = 0.0f, r = 0.0f;
        for (size_t k = 0; k < used; ++k) {
            Slot& s = slots_[k];
            if (s.idle()) continue;
            if (!s.sample) {
                // Emptied while it played: its last pass fades out.
                const float l0 = l, r0 = r;
                for (Voice& t : s.tails) play(t, s, l, r);
                blockPeak_[k] = std::max(blockPeak_[k], std::max(std::fabs(l - l0), std::fabs(r - r0)));
                continue;
            }
            // The clock adds up a fraction a frame: a line is reached a hair before its exact frame
            // count (the same tolerance as the bar lines above), never one frame late.
            if (s.state == Waiting && beat_ + 1e-9 >= s.startBeat) {
                s.state = Playing;
                restart(s);
                states_[k].store(Playing, std::memory_order_relaxed);
            } else if (s.state == Playing && !s.oneShot && beat_ + 1e-9 >= s.startBeat + s.beats) {
                s.startBeat += s.beats;
                // Far behind (the clock snapped or jumped): line up with the clock again.
                if (beat_ >= s.startBeat + s.beats) s.startBeat = std::floor(beat_ / s.beats) * s.beats;
                restart(s);
            }
            const float l0 = l, r0 = r;
            play(s.voice, s, l, r);
            for (Voice& t : s.tails) play(t, s, l, r);
            const float peak = std::max(std::fabs(l - l0), std::fabs(r - r0));
            if (peak > blockPeak_[k]) blockPeak_[k] = peak;
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
        if (s.held && s.voice.sample != s.held && !s.tailOf(s.held)) releaseHeld(s);
    }
    publishLevels(numFrames);
    syncAt_ = -1;
}

}  // namespace ps
