#include "MultiSampler.h"

#include <algorithm>
#include <cmath>

namespace ps {

namespace {

inline float dbToGain(float db) { return std::pow(10.0f, db / 20.0f); }

// A command's value rounded to an int (a huge or infinite value would overflow lround: clamped first).
inline int roundedInt(float v) { return static_cast<int>(std::lround(std::clamp(v, -1.0e6f, 1.0e6f))); }

// 4-point Hermite interpolation: clean pitch shifts of up to a few semitones.
inline float hermite(float t, float y0, float y1, float y2, float y3) {
    const float c1 = 0.5f * (y2 - y0);
    const float c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
    const float c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2);
    return ((c3 * t + c2) * t + c1) * t + y1;
}

// Reads frame i, channel c of a float or a 16-bit sample (i already within 0..frames-1).
struct FloatReader {
    const float* d;
    int ch;
    float operator()(int64_t i, int c) const { return d[i * ch + c]; }
};
struct Pcm16Reader {
    const int16_t* d;
    int ch;
    float operator()(int64_t i, int c) const { return static_cast<float>(d[i * ch + c]) * (1.0f / 32768.0f); }
};

}  // namespace

MultiSampler::~MultiSampler() {
    for (auto& v : voices_) {
        if (v.sample && v.sample->release()) delete v.sample;
        v.sample = nullptr;
    }
    for (auto& z : zones_) {
        if (z.sample && z.sample->release()) delete z.sample;
        z.sample = nullptr;
    }
    collectGarbage();
}

void MultiSampler::prepare(int sampleRate) {
    sampleRate_ = sampleRate;
    for (auto& v : voices_) freeVoice(v);
    eventCount_ = 0;
}

void MultiSampler::recount() {
    loaded_ = 0;
    stereoZones_ = 0;
    for (int i = 0; i < count_; ++i) {
        const Zone& z = zones_[static_cast<size_t>(i)];
        if (!z.sample) continue;
        ++loaded_;
        if (z.sample->channels() == 2) ++stereoZones_;
    }
}

void MultiSampler::installZone(int zone, Sample* sample) {
    if (zone < 0 || zone >= kZones) {
        if (sample && sample->release() && !retired_.push(sample)) {
            // Queue full: leak rather than free on the audio thread.
        }
        return;
    }
    Zone& z = zones_[static_cast<size_t>(zone)];
    // Same recording again (a resync): keep it, drop the extra reference.
    if (sample == z.sample) {
        if (sample && sample->release() && !retired_.push(sample)) {
        }
        return;
    }
    // Voices playing the old one keep their own reference: they ring on.
    if (z.sample && z.sample->release() && !retired_.push(z.sample)) {
    }
    z.sample = sample;
    z.turns = 0;
    recount();
}

void MultiSampler::setParam(int param, float v) {
    if (!std::isfinite(v)) return;
    switch (param) {
        case Count:
            count_ = std::clamp(roundedInt(v), 0, kZones);
            recount();
            break;
        case Release: release_ = std::clamp(v, 0.02f, 10.0f); break;
        case Attack: attack_ = std::clamp(v, 0.0f, 2.0f); break;
        case VelTrack: velTrack_ = std::clamp(v, 0.0f, 1.0f); break;
        case Gain: gain_ = std::clamp(v, 0.0f, 4.0f); break;
        case ReleaseDecay: releaseDecay_ = std::clamp(v, 0.0f, 60.0f); break;
        case ClearZones:
            // Count first: each installZone recounts the zones in use (none now), not all 512.
            count_ = 0;
            for (int i = 0; i < kZones; ++i) {
                if (zones_[static_cast<size_t>(i)].sample) installZone(i, nullptr);
            }
            count_ = 0;
            recount();
            break;
        default: break;
    }
}

void MultiSampler::setZoneParam(int zone, int param, float v) {
    if (zone < 0 || zone >= kZones || !std::isfinite(v)) return;
    Zone& z = zones_[static_cast<size_t>(zone)];
    const int i = roundedInt(v);
    switch (param) {
        case LoKey: z.loKey = std::clamp(i, 0, 127); break;
        case HiKey: z.hiKey = std::clamp(i, -1, 127); break;
        case LoVel: z.loVel = std::clamp(i, 1, 127); break;
        case HiVel: z.hiVel = std::clamp(i, 1, 127); break;
        case Root: z.root = std::clamp(v, 0.0f, 127.0f); break;
        case ZoneGain: z.gain = dbToGain(std::clamp(v, -60.0f, 24.0f)); break;
        case SeqPosition: z.seqPosition = std::clamp(i, 0, 16); z.turns = 0; break;
        case SeqLength: z.seqLength = std::clamp(i, 1, 16); z.turns = 0; break;
        case Trigger: z.release = i == 1; break;
        case LoopStart: z.loopStart = std::max(0.0, static_cast<double>(v)); break;
        case LoopEnd: z.loopEnd = std::max(0.0, static_cast<double>(v)); break;
        default: break;
    }
}

void MultiSampler::queue(const Event& e) {
    if (eventCount_ >= static_cast<int>(events_.size())) {
        // Burst of events: drop extras, but never a key let go (a note would hold for ever): the
        // latest key press gives it its place.
        if (e.on) return;
        int last = eventCount_ - 1;
        while (last >= 0 && !events_[static_cast<size_t>(last)].on) --last;
        if (last < 0) return;
        for (int k = last; k + 1 < eventCount_; ++k) events_[static_cast<size_t>(k)] = events_[static_cast<size_t>(k + 1)];
        --eventCount_;
    }
    // Sorted by offset (stable): piano-roll and keyboard notes mix.
    int i = eventCount_++;
    while (i > 0 && events_[static_cast<size_t>(i - 1)].offset > e.offset) {
        events_[static_cast<size_t>(i)] = events_[static_cast<size_t>(i - 1)];
        --i;
    }
    events_[static_cast<size_t>(i)] = e;
}

void MultiSampler::noteOn(int note, float velocity, int offset) {
    if (note < 0 || note > 127 || !std::isfinite(velocity)) return;
    queue({std::max(0, offset), note, std::clamp(velocity, 0.0f, 1.0f), true});
}

void MultiSampler::noteOff(int note, int offset) {
    if (note < 0 || note > 127) return;
    queue({std::max(0, offset), note, 0.0f, false});
}

void MultiSampler::allNotesOff() {
    eventCount_ = 0;
    const float coef = std::exp(std::log(0.001f) / (std::max(0.02f, release_) * static_cast<float>(sampleRate_)));
    for (auto& v : voices_) {
        if (v.sample && v.gate) {
            v.gate = false;
            v.releaseCoef = coef;
        }
    }
    resetRoundRobin();
}

void MultiSampler::resetRoundRobin() {
    for (auto& z : zones_) z.turns = 0;
}

int MultiSampler::activeVoices() const {
    int n = 0;
    for (const auto& v : voices_) n += v.sample ? 1 : 0;
    return n;
}

void MultiSampler::freeVoice(Voice& v) {
    if (v.sample && v.sample->release() && !retired_.push(v.sample)) {
        // Queue full: leak rather than free on the audio thread.
    }
    v = Voice{};
}

MultiSampler::Voice& MultiSampler::slotForNewVoice() {
    Voice* free = nullptr;
    int playing = 0;
    for (auto& v : voices_) {
        if (!v.sample) {
            if (!free) free = &v;
        } else if (v.fade == 0) {
            ++playing;
        }
    }
    if (playing >= voiceLimit_) {
        // All busy: the quietest one already let go, else the oldest, fades out over 2 ms in its
        // slot while the new one starts in a spare one.
        Voice* best = nullptr;
        for (auto& v : voices_) {
            if (v.sample && v.fade == 0 && !v.gate && (!best || v.env < best->env)) best = &v;
        }
        if (!best) {
            for (auto& v : voices_) {
                if (v.sample && v.fade == 0 && (!best || v.age < best->age)) best = &v;
            }
        }
        if (best) best->fade = kFadeFrames;
    }
    if (free) return *free;
    // Every spare slot fading too (a burst of notes): the fading voice nearest its end is cut.
    Voice* cut = &voices_[0];
    for (auto& v : voices_) {
        if (v.fade > 0 && (cut->fade == 0 || v.fade < cut->fade)) cut = &v;
    }
    freeVoice(*cut);
    return *cut;
}

bool MultiSampler::start(int zi, int note, float velocity, float extraGain) {
    Zone& z = zones_[static_cast<size_t>(zi)];
    if (!z.sample || z.sample->frames() < 4) return false;
    Voice& v = slotForNewVoice();
    z.sample->retain();
    v.sample = z.sample;
    v.note = note;
    v.gate = !z.release;
    v.pos = 0.0;
    // A loop must fit inside the recording with room for the interpolation's last points.
    const double lastFrame = static_cast<double>(z.sample->frames() - 3);
    const double loopEnd = std::min(z.loopEnd, lastFrame);
    // A release sample plays once (looped, with no envelope of its own, it would never end).
    const bool loops = !z.release && loopEnd - z.loopStart >= 16.0;
    v.loopStart = loops ? z.loopStart : 0.0;
    v.loopEnd = loops ? loopEnd : 0.0;
    v.pitch = static_cast<float>(note) - z.root;
    v.ratePitch = 1e9f;
    // SFZ's amp_veltrack: 40 log10(velocity) dB at full tracking.
    const float vel = std::max(velocity, 1.0f / 127.0f);
    v.gain = z.gain * gain_ * extraGain * std::pow(vel, 2.0f * velTrack_);
    const float attackFrames = std::max(1.0f, std::max(attack_, 0.0005f) * static_cast<float>(sampleRate_));
    v.attackStep = 1.0f / attackFrames;
    v.env = 0.0f;
    v.releaseCoef = 1.0f;
    v.fade = 0;
    v.age = ++clock_;
    return true;
}

void MultiSampler::apply(const Event& e) {
    const int vel127 = std::clamp(static_cast<int>(std::lround(e.velocity * 127.0f)), 1, 127);
    const float releaseCoef = std::exp(std::log(0.001f) / (std::max(0.02f, release_) * static_cast<float>(sampleRate_)));
    if (e.on) {
        // The same key again: the old note is damped quickly (a piano's re-strike), not cut.
        const float damp = std::exp(std::log(0.001f) / (std::min(0.15f, std::max(0.02f, release_)) * static_cast<float>(sampleRate_)));
        for (auto& v : voices_) {
            if (v.sample && v.note == e.note && v.gate) {
                v.gate = false;
                v.releaseCoef = std::min(v.releaseCoef, damp);
            }
        }
        pressVelocity_[static_cast<size_t>(e.note)] = e.velocity;
        pressFrame_[static_cast<size_t>(e.note)] = blockStart_ + e.offset;
        int started = 0, firstMatch = -1;
        lastZone_ = -1;
        for (int i = 0; i < count_ && started < kLayersPerNote; ++i) {
            Zone& z = zones_[static_cast<size_t>(i)];
            if (z.release || !z.sample || e.note < z.loKey || e.note > z.hiKey || vel127 < z.loVel || vel127 > z.hiVel) continue;
            if (firstMatch < 0) firstMatch = i;
            if (z.seqLength > 1 && z.seqPosition > 0) {
                const uint32_t turn = z.turns++ % static_cast<uint32_t>(z.seqLength);
                if (static_cast<int>(turn) != z.seqPosition - 1) continue;
            }
            if (start(i, e.note, e.velocity, 1.0f)) {
                if (lastZone_ < 0) lastZone_ = i;
                ++started;
            }
        }
        // Round robins with a turn missing (a zone never installed): the first that matched plays.
        if (started == 0 && firstMatch >= 0 && start(firstMatch, e.note, e.velocity, 1.0f)) lastZone_ = firstMatch;
        return;
    }
    bool held = false;
    for (auto& v : voices_) {
        if (v.sample && v.gate && v.note == e.note) {
            v.gate = false;
            v.releaseCoef = releaseCoef;
            held = true;
        }
    }
    if (!held) return;
    // Release samples: as loud as the key press, softer the longer the key was held.
    const float heldSeconds = static_cast<float>(blockStart_ + e.offset - pressFrame_[static_cast<size_t>(e.note)]) /
                              static_cast<float>(sampleRate_);
    const float decay = dbToGain(-releaseDecay_ * std::max(0.0f, heldSeconds));
    const float pressVel = pressVelocity_[static_cast<size_t>(e.note)];
    const int pressVel127 = std::clamp(static_cast<int>(std::lround(pressVel * 127.0f)), 1, 127);
    int started = 0;
    for (int i = 0; i < count_ && started < kLayersPerNote; ++i) {
        const Zone& z = zones_[static_cast<size_t>(i)];
        if (!z.release || !z.sample || e.note < z.loKey || e.note > z.hiKey || pressVel127 < z.loVel || pressVel127 > z.hiVel) continue;
        if (start(i, e.note, pressVel, decay)) ++started;
    }
}

template <typename Reader>
void MultiSampler::renderVoice(Voice& v, const Reader& read, float* left, float* right, int from, int to) {
    const int ch = v.sample->channels();
    const int64_t last = v.sample->frames() - 1;
    const float pitch = v.pitch + bend_;
    if (pitch != v.ratePitch) {
        v.ratePitch = pitch;
        v.rate = std::exp2(std::clamp(static_cast<double>(pitch), -48.0, 48.0) / 12.0);
    }
    const double rate = v.rate;
    const int rc = ch == 2 ? 1 : 0;
    const bool loops = v.loopEnd > v.loopStart;
    for (int i = from; i < to; ++i) {
        if (loops && v.pos >= v.loopEnd) v.pos -= v.loopEnd - v.loopStart;
        const int64_t i1 = static_cast<int64_t>(v.pos);
        if (i1 >= last) {
            freeVoice(v);
            return;
        }
        // Envelope: attack, the recording's own decay, then the release once let go.
        if (v.env < 1.0f && v.releaseCoef >= 1.0f) {
            v.env = std::min(1.0f, v.env + v.attackStep);
        } else if (v.releaseCoef < 1.0f) {
            v.env *= v.releaseCoef;
            if (v.env < 1e-4f) {
                freeVoice(v);
                return;
            }
        }
        float g = v.env * v.gain;
        if (v.fade > 0) {
            g *= static_cast<float>(v.fade) / kFadeFrames;
            if (--v.fade == 0) {
                freeVoice(v);
                return;
            }
        }
        // The last 2 ms of the recording fade out: no click where a cut recording ends.
        if (!loops) {
            const double remaining = static_cast<double>(last - i1) / rate;
            if (remaining < kFadeFrames) g *= static_cast<float>(remaining / kFadeFrames);
        }
        const float t = static_cast<float>(v.pos - static_cast<double>(i1));
        const int64_t i0 = i1 > 0 ? i1 - 1 : 0;
        const int64_t i2 = i1 + 1;
        const int64_t i3 = std::min(i1 + 2, last);
        const float l = hermite(t, read(i0, 0), read(i1, 0), read(i2, 0), read(i3, 0));
        if (right != nullptr) {
            const float r = rc ? hermite(t, read(i0, 1), read(i1, 1), read(i2, 1), read(i3, 1)) : l;
            left[i] += l * g;
            right[i] += r * g;
        } else {
            const float m = rc ? 0.5f * (l + hermite(t, read(i0, 1), read(i1, 1), read(i2, 1), read(i3, 1))) : l;
            left[i] += m * g;
        }
        v.pos += rate;
    }
}

void MultiSampler::render(float* left, float* right, int numFrames) {
    int nextEvent = 0;
    int from = 0;
    while (from < numFrames) {
        while (nextEvent < eventCount_ && events_[static_cast<size_t>(nextEvent)].offset <= from) {
            apply(events_[static_cast<size_t>(nextEvent++)]);
        }
        // Up to the next event (sample-accurate notes), the voices run on their own.
        int to = numFrames;
        if (nextEvent < eventCount_) to = std::min(numFrames, std::max(from + 1, events_[static_cast<size_t>(nextEvent)].offset));
        for (auto& v : voices_) {
            if (!v.sample) continue;
            if (const int16_t* pcm = v.sample->pcm16()) renderVoice(v, Pcm16Reader{pcm, v.sample->channels()}, left, right, from, to);
            else renderVoice(v, FloatReader{v.sample->data(), v.sample->channels()}, left, right, from, to);
        }
        from = to;
    }
    // Keep events that belong to a later block.
    int kept = 0;
    for (int e = nextEvent; e < eventCount_; ++e) {
        Event ev = events_[static_cast<size_t>(e)];
        ev.offset -= numFrames;
        events_[static_cast<size_t>(kept++)] = ev;
    }
    eventCount_ = kept;
    blockStart_ += numFrames;
}

void MultiSampler::collectGarbage() {
    Sample* s = nullptr;
    while (retired_.pop(s)) delete s;
}

}  // namespace ps
