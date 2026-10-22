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

void SoundFontPlayer::render(float* out, int numFrames) {
    if (!instance_) {
        eventCount_ = 0;
        return;
    }
    int pos = 0;
    int next = 0;
    while (pos < numFrames) {
        while (next < eventCount_ && events_[next].offset <= pos) {
            const Event& e = events_[next++];
            if (e.velocity > 0.0f) tsf_note_on(instance_, preset_, e.note, e.velocity);
            else tsf_note_off(instance_, preset_, e.note);
        }
        const int until = next < eventCount_ ? std::min(numFrames, events_[next].offset) : numFrames;
        tsf_render_float(instance_, out + pos, until - pos, 1);  // 1: mixes into out
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

void SoundFontPlayer::collectGarbage() {
    tsf* f = nullptr;
    while (retired_.pop(f)) tsf_close(f);
}

}  // namespace ps
