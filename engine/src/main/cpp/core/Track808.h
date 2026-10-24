#pragma once

#include <algorithm>
#include <array>
#include <cmath>

#include "StudioVoices.h"

namespace ps {

// The studio 808 (StudioVoices.h, the voice a drum pad may play) on an instrument track (phase
// 38: « Ajouter une piste › 808 »): the piano roll's notes give its pitch, one voice, a key
// pressed while another is held slides to its note (legato, the glide time of its settings), the
// note holds while the key is down (up to its decay) and fades out over a short release after.
// The same voice, settings and level as on a pad: an 808 sounds the same on the pads and on a track.
// Real-time safe: fixed-size state only.
class Track808 {
public:
    // DrumKit::VoiceParam (the pads' studio voice settings), append only. Tune is the pads' own
    // (a track's pitch is its notes'): ignored here.
    enum Param : int { Tune = 0, Decay = 1, Drive = 2, Click = 3, Glide = 4, Sub = 5, Tone = 6, kParamCount };
    // The pad's level of the 808 (DrumKit kKindLevels).
    static constexpr float kLevel = 0.40f;
    static constexpr float kReleaseSeconds = 0.08f;

    // The settings start at a track's defaults once: a reopened stream (Engine::prepare, after the
    // commands it applies) keeps the user's.
    Track808() {
        // A track's notes hold: the longest decay by default (held 1.5 s, then falling).
        settings_.tune = 36.0f;
        settings_.decay = 2.5f;
        settings_.drive = 0.4f;
        settings_.click = 0.4f;
        settings_.glide = 0.08f;
        settings_.tone = 0.5f;
    }

    void prepare(int sampleRate) {
        sampleRate_ = sampleRate;
        voice_ = studio::Bass808{};
        voice_.prepare(sampleRate, settings_);
        releaseCoef_ = std::exp(std::log(0.001f) / (kReleaseSeconds * static_cast<float>(sampleRate)));
        gate_ = 0.0f;
        releasing_ = true;
        heldCount_ = 0;
        eventCount_ = 0;
    }

    void setParam(int param, float v) {
        if (!std::isfinite(v)) return;
        switch (param) {
            case Decay: settings_.decay = std::clamp(v, 0.02f, 2.5f); break;
            case Drive: settings_.drive = std::clamp(v, 0.0f, 1.0f); break;
            case Click: settings_.click = std::clamp(v, 0.0f, 1.0f); break;
            case Glide: settings_.glide = std::clamp(v, 0.0f, 0.5f); break;
            case Sub: settings_.sub = std::clamp(v, 0.0f, 1.0f); break;
            case Tone: settings_.tone = std::clamp(v, 0.0f, 1.0f); break;
            default: break;
        }
    }
    void setBend(float semitones) { bend_ = semitones; }

    void noteOn(int note, float velocity, int offset) { queue(note, velocity, offset, true); }
    void noteOff(int note, int offset) { queue(note, 0.0f, offset, false); }
    void allNotesOff() {
        eventCount_ = 0;
        heldCount_ = 0;
        releasing_ = true;
    }
    bool sounding() const { return gate_ > 1e-5f && voice_.sounding(); }

    // Adds the 808 into `out` (mono: it is the same on both sides).
    void render(float* out, int numFrames) {
        int next = 0;
        for (int i = 0; i < numFrames; ++i) {
            while (next < eventCount_ && events_[static_cast<size_t>(next)].offset <= i) apply(events_[static_cast<size_t>(next++)]);
            if (gate_ <= 1e-5f && releasing_) {
                gate_ = 0.0f;
                continue;
            }
            if (releasing_) gate_ *= releaseCoef_;
            out[i] += kLevel * gate_ * voice_.process(sampleRate_, settings_);
        }
        int kept = 0;
        for (int e = next; e < eventCount_; ++e) {
            Event ev = events_[static_cast<size_t>(e)];
            ev.offset -= numFrames;
            events_[static_cast<size_t>(kept++)] = ev;
        }
        eventCount_ = kept;
    }

private:
    struct Event {
        int offset = 0;
        int note = 0;
        float velocity = 0.0f;
        bool on = false;
    };
    void queue(int note, float velocity, int offset, bool on) {
        if (note < 0 || note > 127 || !std::isfinite(velocity)) return;
        if (eventCount_ >= static_cast<int>(events_.size())) {
            // Full: drop extras, but never a key let go (the note would hold): the latest key
            // press gives it its place.
            if (on) return;
            int last = eventCount_ - 1;
            while (last >= 0 && !events_[static_cast<size_t>(last)].on) --last;
            if (last < 0) return;
            for (int k = last; k + 1 < eventCount_; ++k) events_[static_cast<size_t>(k)] = events_[static_cast<size_t>(k + 1)];
            --eventCount_;
        }
        int i = eventCount_++;
        const Event e{std::max(0, offset), note, std::clamp(velocity, 0.0f, 1.0f), on};
        while (i > 0 && events_[static_cast<size_t>(i - 1)].offset > e.offset) {
            events_[static_cast<size_t>(i)] = events_[static_cast<size_t>(i - 1)];
            --i;
        }
        events_[static_cast<size_t>(i)] = e;
    }
    void strike(int note, float velocity, bool legato) {
        // Bass808 plays settings.tune + pitch: the note itself, bent.
        settings_.tune = 0.0f;
        const float pitch = static_cast<float>(note) + bend_;
        const int glideFrames = legato ? static_cast<int>(settings_.glide * static_cast<float>(sampleRate_)) : 0;
        voice_.hit(sampleRate_, settings_, std::max(velocity, 0.05f), pitch, glideFrames);
        gate_ = 1.0f;
        releasing_ = false;
        playing_ = note;
    }
    void apply(const Event& e) {
        // The keys held, the latest last: letting go of it slides back to the one under it.
        for (int i = 0; i < heldCount_; ++i) {
            if (held_[static_cast<size_t>(i)] != e.note) continue;
            for (int k = i; k + 1 < heldCount_; ++k) held_[static_cast<size_t>(k)] = held_[static_cast<size_t>(k + 1)];
            --heldCount_;
            break;
        }
        if (e.on) {
            const bool legato = heldCount_ > 0 && !releasing_ && settings_.glide > 0.0f;
            if (heldCount_ == static_cast<int>(held_.size())) {
                for (int k = 0; k + 1 < heldCount_; ++k) held_[static_cast<size_t>(k)] = held_[static_cast<size_t>(k + 1)];
                --heldCount_;
            }
            held_[static_cast<size_t>(heldCount_++)] = e.note;
            heldVelocity_ = e.velocity;
            strike(e.note, e.velocity, legato);
            return;
        }
        if (heldCount_ > 0) {
            // Still a key down: back to it if the sounding one was let go.
            if (e.note == playing_) strike(held_[static_cast<size_t>(heldCount_ - 1)], heldVelocity_, settings_.glide > 0.0f);
            return;
        }
        if (e.note == playing_) releasing_ = true;
    }

    int sampleRate_ = 48000;
    studio::Settings settings_{};
    studio::Bass808 voice_{};
    float gate_ = 0.0f, releaseCoef_ = 0.999f, bend_ = 0.0f, heldVelocity_ = 0.8f;
    bool releasing_ = true;
    int playing_ = -1;
    std::array<int, 16> held_{};
    int heldCount_ = 0;
    std::array<Event, 128> events_{};
    int eventCount_ = 0;
};

}  // namespace ps
