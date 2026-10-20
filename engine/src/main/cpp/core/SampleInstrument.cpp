#include "SampleInstrument.h"

#include <algorithm>
#include <cmath>

namespace ps {

SampleInstrument::~SampleInstrument() {
    if (sample_ && sample_->release()) delete sample_;
    sample_ = nullptr;
    collectGarbage();
}

void SampleInstrument::prepare(int sampleRate) {
    sampleRate_ = sampleRate;
    voices_.fill(Voice{});
    eventCount_ = 0;
    heldCount_ = 0;
}

void SampleInstrument::install(Sample* sample) {
    // Same sound again (undo, resync): keep playing, drop the extra reference.
    if (sample == sample_) {
        if (sample && sample->release() && !retired_.push(sample)) {
            // Queue full: leak rather than free on the audio thread.
        }
        return;
    }
    // Voices read the installed sound: silence them before the old one can be freed.
    voices_.fill(Voice{});
    heldCount_ = 0;
    if (sample_ && sample_->release() && !retired_.push(sample_)) {
        // Queue full: leak rather than free on the audio thread.
    }
    sample_ = sample;
}

void SampleInstrument::setParam(int param, float v) {
    if (!std::isfinite(v)) return;
    Params& p = params_;
    switch (param) {
        case Root: p.root = std::clamp(v, 0.0f, 127.0f); break;
        case Start: p.start = std::clamp(v, 0.0f, 0.999f); break;
        case End: p.end = std::clamp(v, 0.001f, 1.0f); break;
        case Attack: p.attack = std::clamp(v, 0.0f, 2.0f); break;
        case Decay: p.decay = std::clamp(v, 0.0f, 2.0f); break;
        case Sustain: p.sustain = std::clamp(v, 0.0f, 1.0f); break;
        case Release: p.release = std::clamp(v, 0.0f, 4.0f); break;
        case Glide: p.glide = std::clamp(v, 0.0f, 1.0f); break;
        case Loop: p.loop = v > 0.5f; break;
        case Gain: p.gain = std::clamp(v, 0.0f, 2.0f); break;
        default: break;
    }
}

void SampleInstrument::queue(const Event& e) {
    if (eventCount_ >= static_cast<int>(events_.size())) return;  // burst of events: drop extras
    // Sorted by offset (stable): piano-roll and keyboard notes mix.
    int i = eventCount_++;
    while (i > 0 && events_[static_cast<size_t>(i - 1)].offset > e.offset) {
        events_[static_cast<size_t>(i)] = events_[static_cast<size_t>(i - 1)];
        --i;
    }
    events_[static_cast<size_t>(i)] = e;
}

void SampleInstrument::noteOn(int note, float velocity, int offset) {
    queue({std::max(0, offset), std::clamp(note, 0, 127), std::clamp(velocity, 0.0f, 1.0f), true});
}

void SampleInstrument::noteOff(int note, int offset) {
    queue({std::max(0, offset), note, 0.0f, false});
}

void SampleInstrument::allNotesOff() {
    eventCount_ = 0;
    heldCount_ = 0;
    for (auto& v : voices_) {
        if (v.gate) release(v);
    }
}

int SampleInstrument::activeVoices() const {
    int n = 0;
    for (const auto& v : voices_) n += v.stage != Off ? 1 : 0;
    return n;
}

void SampleInstrument::release(Voice& v) {
    v.gate = false;
    v.stage = Releasing;
    v.releaseStep = v.env / std::max(1.0f, params_.release * static_cast<float>(sampleRate_));
}

SampleInstrument::Voice* SampleInstrument::freeSlot() {
    for (auto& v : voices_) {
        if (v.stage == Off) return &v;
    }
    // All busy: the quietest voice already fading out, else the oldest.
    Voice* best = nullptr;
    for (auto& v : voices_) {
        if (v.fade > 0 && (!best || v.fade < best->fade)) best = &v;
    }
    if (best) return best;
    best = &voices_[0];
    for (auto& v : voices_) {
        if (v.age < best->age) best = &v;
    }
    return best;
}

void SampleInstrument::start(Voice& v, int note, float velocity) {
    const Params& p = params_;
    const double frames = static_cast<double>(sample_->frames());
    v = Voice{};
    v.pos = std::floor(std::min(p.start, p.end) * frames);
    v.note = note;
    v.gate = true;
    v.semis = v.target = static_cast<float>(note);
    v.velocity = velocity;
    v.age = ++clock_;
    if (p.attack > 0.0f) {
        v.stage = Attack_;
        v.env = 0.0f;
    } else {
        v.stage = p.decay > 0.0f ? Decay_ : Held;
        v.env = 1.0f;
    }
}

void SampleInstrument::apply(const Event& e) {
    const bool mono = params_.glide > 0.0f;
    // The voice held by a key in glide mode (one at most).
    auto current = [&]() -> Voice* {
        for (auto& v : voices_) {
            if (v.gate && v.fade == 0 && v.stage != Off) return &v;
        }
        return nullptr;
    };
    const float glideFrames = std::max(1.0f, params_.glide * static_cast<float>(sampleRate_));
    auto slideTo = [&](Voice& v, int note) {
        v.note = note;
        v.target = static_cast<float>(note);
        v.glideStep = (v.target - v.semis) / glideFrames;
        if (v.glideStep == 0.0f) v.semis = v.target;
    };
    if (e.on) {
        if (mono) {
            for (int i = 0; i < heldCount_; ++i) {
                if (held_[static_cast<size_t>(i)] != e.note) continue;
                for (int k = i; k + 1 < heldCount_; ++k) held_[static_cast<size_t>(k)] = held_[static_cast<size_t>(k + 1)];
                --heldCount_;
                break;
            }
            if (heldCount_ == static_cast<int>(held_.size())) {
                for (int k = 0; k + 1 < heldCount_; ++k) held_[static_cast<size_t>(k)] = held_[static_cast<size_t>(k + 1)];
                --heldCount_;
            }
            held_[static_cast<size_t>(heldCount_++)] = e.note;
            if (Voice* cur = current()) {
                slideTo(*cur, e.note);  // legato: no new attack, the pitch slides
                return;
            }
            // One voice only: whatever still rings (a release tail) is cut.
            for (auto& v : voices_) {
                if (v.stage != Off && v.fade == 0) v.fade = kFadeFrames;
            }
        } else {
            int sounding = 0;
            for (auto& v : voices_) {
                if (v.stage == Off || v.fade > 0) continue;
                if (v.note == e.note) v.fade = kFadeFrames;  // the same key again: cut the old one
                else ++sounding;
            }
            if (sounding >= kPolyphony) {
                Voice* oldest = nullptr;
                for (auto& v : voices_) {
                    if (v.stage != Off && v.fade == 0 && (!oldest || v.age < oldest->age)) oldest = &v;
                }
                if (oldest) oldest->fade = kFadeFrames;
            }
        }
        start(*freeSlot(), e.note, e.velocity);
        return;
    }
    if (mono) {
        for (int i = 0; i < heldCount_; ++i) {
            if (held_[static_cast<size_t>(i)] != e.note) continue;
            for (int k = i; k + 1 < heldCount_; ++k) held_[static_cast<size_t>(k)] = held_[static_cast<size_t>(k + 1)];
            --heldCount_;
            break;
        }
        Voice* cur = current();
        if (!cur || cur->note != e.note) return;  // an older key let go: the sounding note stays
        if (heldCount_ > 0) slideTo(*cur, held_[static_cast<size_t>(heldCount_ - 1)]);
        else release(*cur);
        return;
    }
    for (auto& v : voices_) {
        if (v.gate && v.note == e.note) release(v);
    }
}

float SampleInstrument::renderVoice(Voice& v, double lo, double hi) {
    const Params& p = params_;
    const float sr = static_cast<float>(sampleRate_);
    // Envelope.
    switch (v.stage) {
        case Off: return 0.0f;
        case Attack_:
            v.env += 1.0f / std::max(1.0f, p.attack * sr);
            if (v.env >= 1.0f) {
                v.env = 1.0f;
                v.stage = p.decay > 0.0f ? Decay_ : Held;
            }
            break;
        case Decay_:
            v.env -= (1.0f - p.sustain) / std::max(1.0f, p.decay * sr);
            if (v.env <= p.sustain) {
                v.env = p.sustain;
                v.stage = Held;
            }
            break;
        case Held: v.env = p.decay > 0.0f ? p.sustain : 1.0f; break;
        case Releasing:
            v.env -= v.releaseStep;
            if (v.env <= 0.0f) {
                v = Voice{};
                return 0.0f;
            }
            break;
    }
    // Pitch: the semitones from the root, plus the bend; gliding moves a little every frame.
    if (v.glideStep != 0.0f) {
        v.semis += v.glideStep;
        if ((v.glideStep > 0.0f && v.semis >= v.target) || (v.glideStep < 0.0f && v.semis <= v.target)) {
            v.semis = v.target;
            v.glideStep = 0.0f;
        }
    }
    const double rate = std::exp2(std::clamp(static_cast<double>(v.semis + bend_ - p.root), -48.0, 48.0) / 12.0);

    if (v.pos >= hi) {
        if (!p.loop) {
            v = Voice{};
            return 0.0f;
        }
        v.pos = lo + std::fmod(v.pos - lo, hi - lo);
    }
    const Sample* s = sample_;
    const int ch = s->channels();
    const float* d = s->data();
    const int64_t last = s->frames() - 1;
    auto at = [&](int64_t i) {
        i = std::clamp<int64_t>(i, 0, last);
        return ch == 2 ? 0.5f * (d[i * 2] + d[i * 2 + 1]) : d[i];
    };
    // 4-point Hermite interpolation: clean pitch shifts.
    const int64_t i1 = static_cast<int64_t>(std::floor(v.pos));
    const float t = static_cast<float>(v.pos - static_cast<double>(i1));
    const float y0 = at(i1 - 1), y1 = at(i1), y2 = at(i1 + 1), y3 = at(i1 + 2);
    const float c1 = 0.5f * (y2 - y0);
    const float c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
    const float c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2);
    const float x = ((c3 * t + c2) * t + c1) * t + y1;

    float g = v.env * p.gain * (0.3f + 0.7f * v.velocity);
    if (!p.loop) {
        // A one-shot fades over its last 2 ms: no click when the region ends on a loud sample.
        const double remaining = (hi - v.pos) / rate;
        if (remaining < kFadeFrames) g *= static_cast<float>(remaining / kFadeFrames);
    }
    if (v.fade > 0) {
        g *= static_cast<float>(v.fade) / kFadeFrames;
        if (--v.fade == 0) {
            v = Voice{};
            return x * g;
        }
    }
    v.pos += rate;
    return x * g;
}

void SampleInstrument::render(float* mono, int numFrames) {
    if (!sample_) {
        eventCount_ = 0;
        return;
    }
    const double frames = static_cast<double>(sample_->frames());
    const double lo = std::floor(std::min(params_.start, params_.end) * frames);
    const double hi = std::max(lo + 1.0, std::floor(std::max(params_.start, params_.end) * frames));
    int nextEvent = 0;
    for (int i = 0; i < numFrames; ++i) {
        while (nextEvent < eventCount_ && events_[static_cast<size_t>(nextEvent)].offset <= i) {
            apply(events_[static_cast<size_t>(nextEvent++)]);
        }
        float out = 0.0f;
        for (auto& v : voices_) {
            if (v.stage != Off) out += renderVoice(v, lo, hi);
        }
        mono[i] += out;
    }
    // Keep events that belong to a later block.
    int kept = 0;
    for (int e = nextEvent; e < eventCount_; ++e) {
        Event ev = events_[static_cast<size_t>(e)];
        ev.offset -= numFrames;
        events_[static_cast<size_t>(kept++)] = ev;
    }
    eventCount_ = kept;
}

void SampleInstrument::collectGarbage() {
    Sample* s = nullptr;
    while (retired_.pop(s)) delete s;
}

}  // namespace ps
