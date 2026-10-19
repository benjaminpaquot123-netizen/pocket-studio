#include "Sampler.h"

#include <algorithm>

namespace ps {

Sampler::~Sampler() {
    for (auto*& s : slots_) {
        if (s && s->release()) delete s;
        s = nullptr;
    }
    collectGarbage();
}

void Sampler::prepare(int) {
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
    // Voices may still point at the old sample: silence them before it can be freed.
    voices_[slot] = Voice{};
    tails_[slot] = Voice{};
    pendingOffset_[slot] = -1;
    retire(slots_[slot]);
    slots_[slot] = sample;
}

void Sampler::trigger(int slot, float velocity, int frameOffset) {
    if (!has(slot)) return;
    pendingOffset_[slot] = std::max(0, frameOffset);
    pendingVelocity_[slot] = std::clamp(velocity, 0.0f, 1.0f);
}

void Sampler::stopAll() {
    voices_.fill(Voice{});
    tails_.fill(Voice{});
    pendingOffset_.fill(-1);
}

void Sampler::renderVoice(Voice& v, float* out, int from, int to) {
    const Sample* s = v.sample;
    if (!s) return;
    const int ch = s->channels();
    const float* d = s->data();
    const float scale = ch == 2 ? 0.5f * v.gain : v.gain;
    for (int i = from; i < to; ++i) {
        if (v.pos >= s->frames()) {
            v.sample = nullptr;
            return;
        }
        float x = ch == 2 ? d[v.pos * 2] + d[v.pos * 2 + 1] : d[v.pos];
        x *= scale;
        if (v.fade > 0) {
            x *= static_cast<float>(v.fade) / kFadeFrames;
            if (--v.fade == 0) {
                out[i] += x;
                v.sample = nullptr;
                return;
            }
        }
        out[i] += x;
        ++v.pos;
    }
}

void Sampler::render(float* out, int numFrames) {
    for (int slot = 0; slot < kSlots; ++slot) {
        const int at = pendingOffset_[slot];
        if (at >= 0 && at < numFrames) {
            renderVoice(voices_[slot], out, 0, at);
            renderVoice(tails_[slot], out, 0, at);
            if (voices_[slot].sample) {
                tails_[slot] = voices_[slot];
                tails_[slot].fade = kFadeFrames;
            }
            voices_[slot] = Voice{slots_[slot], 0, pendingVelocity_[slot], 0};
            pendingOffset_[slot] = -1;
            renderVoice(voices_[slot], out, at, numFrames);
            renderVoice(tails_[slot], out, at, numFrames);
        } else {
            if (at >= numFrames) pendingOffset_[slot] = at - numFrames;
            renderVoice(voices_[slot], out, 0, numFrames);
            renderVoice(tails_[slot], out, 0, numFrames);
        }
    }
}

void Sampler::collectGarbage() {
    Sample* s = nullptr;
    while (retired_.pop(s)) delete s;
}

}  // namespace ps
