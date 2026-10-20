#include "Sampler.h"

#include <algorithm>
#include <cmath>

#include "Pan.h"

namespace ps {

Sampler::~Sampler() {
    for (auto*& s : slots_) {
        if (s && s->release()) delete s;
        s = nullptr;
    }
    collectGarbage();
}

void Sampler::prepare(int sampleRate) {
    sampleRate_ = sampleRate;
    stopAll();
}

void Sampler::retire(Sample* s) {
    if (s && s->release() && !retired_.push(s)) {
        // Queue full (64 releases without a collection): leak rather than free here.
    }
}

void Sampler::install(int slot, Sample* sample) {
    if (slot < 0 || slot >= kSlots) {
        retire(sample);
        return;
    }
    // Same sound again (undo, back to the app, resync): keep playing, drop the extra reference.
    if (sample == slots_[slot]) {
        retire(sample);
        return;
    }
    // Voices may still point at the old sample: silence them before it can be freed.
    voices_[slot] = Voice{};
    tails_[slot] = Voice{};
    pendingOffset_[slot] = -1;
    retire(slots_[slot]);
    slots_[slot] = sample;
}

void Sampler::setParam(int slot, int param, float v) {
    if (slot < 0 || slot >= kSlots || !std::isfinite(v)) return;
    PadParams& p = params_[slot];
    switch (param) {
        case Start: p.start = std::clamp(v, 0.0f, 0.999f); break;
        case End: p.end = std::clamp(v, 0.001f, 1.0f); break;
        case Pitch: p.pitch = std::clamp(v, -24.0f, 24.0f); break;
        case Reverse: p.reverse = v > 0.5f; break;
        case Loop: p.loop = v > 0.5f; break;
        case Attack: p.attack = std::clamp(v, 0.0f, 2.0f); break;
        case Decay: p.decay = std::clamp(v, 0.0f, 2.0f); break;
        case Sustain: p.sustain = std::clamp(v, 0.0f, 1.0f); break;
        case Release: p.release = std::clamp(v, 0.0f, 2.0f); break;
        case Choke: p.choke = std::clamp(static_cast<int>(std::lround(v)), 0, kChokeGroups); break;
        case Gain: p.gain = std::clamp(v, 0.0f, 2.0f); break;
        default: break;
    }
}

void Sampler::trigger(int slot, float velocity, int frameOffset, float pan, float pitch) {
    if (!has(slot)) return;
    pendingOffset_[slot] = std::max(0, frameOffset);
    pendingVelocity_[slot] = std::clamp(velocity, 0.0f, 1.0f);
    pendingPan_[slot] = std::isfinite(pan) ? pan : 0.0f;
    pendingPitch_[slot] = std::isfinite(pitch) ? pitch : 0.0f;
}

void Sampler::fadeOut(int slot) {
    if (!voices_[slot].sample) return;
    tails_[slot] = voices_[slot];
    tails_[slot].fade = kFadeFrames;
    voices_[slot] = Voice{};
}

void Sampler::start(int slot, float velocity, float pan, float pitch) {
    const Sample* s = slots_[slot];
    const PadParams& p = params_[slot];
    fadeOut(slot);  // the other pads of a choke group were cut at this frame by render()
    const double frames = static_cast<double>(s->frames());
    double lo = std::floor(std::min(p.start, p.end) * frames);
    double hi = std::floor(std::max(p.start, p.end) * frames);
    if (hi - lo < 1.0) hi = std::min(frames, lo + 1.0);
    const double rate = std::pow(2.0, std::clamp(p.pitch + pitch, -36.0f, 36.0f) / 12.0);
    Voice v;
    v.sample = s;
    v.lo = lo;
    v.hi = hi;
    v.step = p.reverse ? -rate : rate;
    v.pos = p.reverse ? hi - 1.0 : lo;
    v.loop = p.loop;
    v.gain = velocity * p.gain;
    panGains(pan, v.gainL, v.gainR);
    v.length = static_cast<int64_t>((hi - lo) / rate);
    const float sr = static_cast<float>(sampleRate_);
    v.attackFrames = p.attack * sr;
    v.decayFrames = p.decay * sr;
    v.sustain = p.sustain;
    // Never let the release eat more than half of a short sound.
    v.releaseFrames = std::max(1.0f, std::min(p.release * sr, 0.5f * static_cast<float>(v.length)));
    voices_[slot] = v;
}

void Sampler::releaseLoops() {
    for (int slot = 0; slot < kSlots; ++slot) {
        if (voices_[slot].loop) fadeOut(slot);
    }
}

void Sampler::stopAll() {
    voices_.fill(Voice{});
    tails_.fill(Voice{});
    pendingOffset_.fill(-1);
}

void Sampler::renderVoice(Voice& v, float* left, float* right, int from, int to) {
    const Sample* s = v.sample;
    if (!s) return;
    const int ch = s->channels();
    const float* d = s->data();
    const int64_t last = s->frames() - 1;
    // Channel c of frame i (a mono sound gives the same to both sides).
    auto at = [&](int64_t i, int c) {
        i = std::clamp<int64_t>(i, 0, last);
        return ch == 2 ? d[i * 2 + c] : d[i];
    };
    auto hermite = [&](int64_t i1, float t, int c) {
        const float y0 = at(i1 - 1, c), y1 = at(i1, c), y2 = at(i1 + 1, c), y3 = at(i1 + 2, c);
        const float c1 = 0.5f * (y2 - y0);
        const float c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
        const float c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2);
        return ((c3 * t + c2) * t + c1) * t + y1;
    };
    for (int i = from; i < to; ++i) {
        if (!v.loop && (v.pos >= v.hi || v.pos < v.lo)) {
            v.sample = nullptr;
            return;
        }
        // 4-point Hermite interpolation: clean pitch shifts.
        const int64_t i1 = static_cast<int64_t>(std::floor(v.pos));
        const float t = static_cast<float>(v.pos - static_cast<double>(i1));
        const float xl = hermite(i1, t, 0);
        const float xr = ch == 2 ? hermite(i1, t, 1) : xl;

        // Envelope: attack, decay to sustain, release before the end of a one-shot.
        const float age = static_cast<float>(v.age);
        float env = 1.0f;
        if (age < v.attackFrames) {
            env = age / v.attackFrames;
        } else if (v.decayFrames > 0.0f) {
            const float k = std::min(1.0f, (age - v.attackFrames) / v.decayFrames);
            env = 1.0f - (1.0f - v.sustain) * k;
        } else {
            env = v.sustain;
        }
        if (!v.loop) {
            const float remaining = static_cast<float>(v.length - v.age);
            if (remaining < v.releaseFrames) env *= std::max(0.0f, remaining / v.releaseFrames);
        }
        float g = env * v.gain;
        if (v.fade > 0) g *= static_cast<float>(v.fade) / kFadeFrames;
        left[i] += xl * g * v.gainL;
        right[i] += xr * g * v.gainR;
        if (v.fade > 0 && --v.fade == 0) {
            v.sample = nullptr;
            return;
        }
        ++v.age;
        v.pos += v.step;
        if (v.loop) {
            const double span = v.hi - v.lo;
            if (v.pos >= v.hi) v.pos -= span * std::floor((v.pos - v.lo) / span);
            else if (v.pos < v.lo) v.pos += span * std::ceil((v.lo - v.pos) / span);
        }
    }
}

void Sampler::render(float* left, float* right, int numFrames) {
    std::array<float*, kSlots> ls, rs;
    ls.fill(left);
    rs.fill(right);
    render(ls.data(), rs.data(), numFrames);
}

void Sampler::render(float* const* lefts, float* const* rights, int numFrames) {
    // Choke groups: a hit cuts the other pads of its group at the hit's own frame.
    std::array<int, kSlots> cutAt;
    cutAt.fill(-1);
    for (int slot = 0; slot < kSlots; ++slot) {
        const int at = pendingOffset_[slot];
        const int group = params_[slot].choke;
        if (at < 0 || at >= numFrames || group == 0) continue;
        for (int other = 0; other < kSlots; ++other) {
            if (other != slot && params_[other].choke == group && (cutAt[other] < 0 || at < cutAt[other])) cutAt[other] = at;
        }
    }
    for (int slot = 0; slot < kSlots; ++slot) {
        float* left = lefts[slot];
        float* right = rights[slot];
        int at = pendingOffset_[slot];
        if (at >= numFrames) {
            pendingOffset_[slot] = at - numFrames;
            at = -1;
        }
        int cut = cutAt[slot];
        int pos = 0;
        // Up to two events in time order: a cut from the group, this pad's own hit.
        while (cut >= 0 || at >= 0) {
            const bool cutFirst = cut >= 0 && (at < 0 || cut <= at);
            const int when = cutFirst ? cut : at;
            renderVoice(voices_[slot], left, right, pos, when);
            renderVoice(tails_[slot], left, right, pos, when);
            pos = when;
            if (cutFirst) {
                fadeOut(slot);
                cut = -1;
            } else {
                start(slot, pendingVelocity_[slot], pendingPan_[slot], pendingPitch_[slot]);
                pendingOffset_[slot] = -1;
                at = -1;
            }
        }
        renderVoice(voices_[slot], left, right, pos, numFrames);
        renderVoice(tails_[slot], left, right, pos, numFrames);
    }
}

void Sampler::collectGarbage() {
    Sample* s = nullptr;
    while (retired_.pop(s)) delete s;
}

}  // namespace ps
