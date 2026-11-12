#include "SoundFontPlayer.h"

#include <algorithm>
#include <cmath>
#include <iterator>

#define TSF_IMPLEMENTATION
#include "tsf.h"

namespace ps {

SoundFontPlayer::~SoundFontPlayer() {
    close(instance_);
    instance_ = nullptr;
    collectGarbage();
}

tsf* SoundFontPlayer::loadFile(const char* path) { return path ? tsf_load_filename(path) : nullptr; }

tsf* SoundFontPlayer::makeInstance(tsf* master, int sampleRate) {
    if (!master) return nullptr;
    tsf* f = tsf_copy(master);
    if (!f) return nullptr;
    tsf_set_output(f, TSF_MONO, sampleRate, -4.0f);
    // Preallocated voices: from now on note_on steals a releasing voice instead of allocating.
    // Channel 0 made here too (it allocates): every note then follows its pitch wheel.
    if (!tsf_set_max_voices(f, kMaxVoices) || !tsf_channel_set_pitchrange(f, 0, kBendRange)) {
        tsf_close(f);
        return nullptr;
    }
    return f;
}

void SoundFontPlayer::close(tsf* f) {
    if (f) tsf_close(f);
}

bool SoundFontPlayer::renderNote(tsf* master, int preset, int note, float velocity, int holdFrames, float* out,
                                 int frames, int sampleRate) {
    tsf* f = makeInstance(master, sampleRate);
    if (!f || preset < 0 || preset >= tsf_get_presetcount(f)) {
        close(f);
        return false;
    }
    std::fill(out, out + frames, 0.0f);
    tsf_note_on(f, preset, note, std::clamp(velocity, 0.01f, 1.0f));
    const int hold = std::clamp(holdFrames, 0, frames);
    tsf_render_float(f, out, hold, 0);
    tsf_note_off(f, preset, note);
    tsf_render_float(f, out + hold, frames - hold, 0);
    close(f);
    return true;
}

int SoundFontPlayer::presetCount(tsf* f) { return f ? tsf_get_presetcount(f) : 0; }

void SoundFontPlayer::presetInfo(tsf* f, int index, int* bank, int* program, const char** name) {
    const auto& p = f->presets[index];
    *bank = p.bank;
    *program = p.preset;
    *name = p.presetName;
}

void SoundFontPlayer::install(tsf* instance, int preset) {
    if (instance_ && !retired_.push(instance_)) {
        // Queue full: keep the old one alive (leak) rather than free it here.
    }
    instance_ = instance;
    preset_ = instance ? std::clamp(preset, 0, std::max(0, tsf_get_presetcount(instance) - 1)) : 0;
    eventCount_ = 0;
    applyBend();
}

void SoundFontPlayer::setBend(float semitones) {
    if (!std::isfinite(semitones)) return;
    bend_ = std::clamp(semitones, -kBendRange, kBendRange);
    applyBend();
}

void SoundFontPlayer::applyBend() {
    // Only on an instance from makeInstance: its channel exists, so nothing is allocated here.
    if (!instance_ || !instance_->channels || instance_->channels->channelNum < 1) return;
    const int wheel = static_cast<int>(std::lround(8192.0f + bend_ / kBendRange * 8192.0f));
    tsf_channel_set_pitchwheel(instance_, 0, std::clamp(wheel, 0, 16383));
}

void SoundFontPlayer::queue(const Event& e) {
    if (eventCount_ >= static_cast<int>(std::size(events_))) return;
    int i = eventCount_++;
    while (i > 0 && events_[i - 1].offset > e.offset) {
        events_[i] = events_[i - 1];
        --i;
    }
    events_[i] = e;
}

void SoundFontPlayer::noteOn(int note, float velocity, int frameOffset) {
    queue({std::max(0, frameOffset), std::clamp(note, 0, 127), std::clamp(velocity, 0.01f, 1.0f)});
}

void SoundFontPlayer::noteOff(int note, int frameOffset) { queue({std::max(0, frameOffset), std::clamp(note, 0, 127), 0.0f}); }

void SoundFontPlayer::allNotesOff() {
    eventCount_ = 0;
    if (instance_) tsf_note_off_all(instance_);
}

void SoundFontPlayer::renderSpan(float* left, float* right, int frames) {
    const TSFOutputMode mode = right ? TSF_STEREO_INTERLEAVED : TSF_MONO;
    // The mode is a plain field (no allocation): a voice's pan was set at its note-on either way.
    if (instance_->outputmode != mode) tsf_set_output(instance_, mode, static_cast<int>(instance_->outSampleRate), instance_->globalGainDB);
    if (!right) {
        tsf_render_float(instance_, left, frames, 1);  // 1: mixes into out
        return;
    }
    // A centred sound is sqrt(1/2) on each side in stereo: sqrt(2) makes it the mono's level.
    constexpr float kCentre = 1.41421356f;
    for (int pos = 0; pos < frames; pos += kStereoChunk) {
        const int n = std::min(kStereoChunk, frames - pos);
        tsf_render_float(instance_, stereoBuf_, n, 0);
        for (int i = 0; i < n; ++i) {
            left[pos + i] += kCentre * stereoBuf_[2 * i];
            right[pos + i] += kCentre * stereoBuf_[2 * i + 1];
        }
    }
}

void SoundFontPlayer::render(float* out, float* right, int numFrames) {
    if (!instance_) {
        eventCount_ = 0;
        return;
    }
    int pos = 0;
    int next = 0;
    while (pos < numFrames) {
        while (next < eventCount_ && events_[next].offset <= pos) {
            const Event& e = events_[next++];
            if (e.velocity > 0.0f) {
                makeRoom(e.note, e.velocity);
                tsf_note_on(instance_, preset_, e.note, e.velocity);
            } else tsf_note_off(instance_, preset_, e.note);
        }
        const int until = next < eventCount_ ? std::min(numFrames, events_[next].offset) : numFrames;
        renderSpan(out + pos, right ? right + pos : nullptr, until - pos);
        pos = until;
    }
    // Events for later blocks.
    int kept = 0;
    for (int e = next; e < eventCount_; ++e) {
        Event ev = events_[e];
        ev.offset -= numFrames;
        events_[kept++] = ev;
    }
    eventCount_ = kept;
}

void SoundFontPlayer::makeRoom(int note, float velocity) {
    // TinySoundFont's preallocated voices only steal one already releasing: past kMaxVoices held
    // notes (a sustain pedal, a long arpeggio) a new note would be dropped, silent (audit 13 M6).
    // Kept: kReserve voices free or fading for the new note's regions. The oldest notes held make
    // room first, all their regions at once, over TinySoundFont's quick release (10 ms, no click);
    // the voices already releasing count as room (tsf_note_on takes the one furthest in first).
    tsf* f = instance_;
    if (f->presetNum <= preset_) return;
    const short midiVelocity = static_cast<short>(velocity * 127);
    int needed = 0;
    const tsf_preset& preset = f->presets[preset_];
    for (int i = 0; i < preset.regionNum; ++i) {
        const tsf_region& r = preset.regions[i];
        if (note >= r.lokey && note <= r.hikey && midiVelocity >= r.lovel && midiVelocity <= r.hivel) ++needed;
    }
    if (needed == 0) return;
    const int want = std::min(f->voiceNum, needed + kReserve);
    for (int guard = 0; guard < f->voiceNum; ++guard) {
        int room = 0;
        tsf_voice* oldest = nullptr;
        for (int i = 0; i < f->voiceNum; ++i) {
            tsf_voice& v = f->voices[i];
            if (v.playingPreset == -1 || v.ampenv.segment >= TSF_SEGMENT_RELEASE) ++room;
            else if (!oldest || v.playIndex < oldest->playIndex) oldest = &v;
        }
        if (room >= want || !oldest) return;
        // Every region of that note (they share its play index).
        const unsigned int index = oldest->playIndex;
        for (int i = 0; i < f->voiceNum; ++i) {
            tsf_voice& v = f->voices[i];
            if (v.playingPreset != -1 && v.ampenv.segment < TSF_SEGMENT_RELEASE && v.playIndex == index) tsf_voice_endquick(f, &v);
        }
    }
}

void SoundFontPlayer::collectGarbage() {
    tsf* f = nullptr;
    while (retired_.pop(f)) tsf_close(f);
}

}  // namespace ps
