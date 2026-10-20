#include "PlaitsSynth.h"

#include <algorithm>
#include <cmath>

#include "plaits/dsp/voice.h"

namespace ps {

namespace {

constexpr int kBlock = 12;  // plaits::kBlockSize
// Plaits is tuned for the module's actual 47872 Hz clock: at an exact 48 kHz it would be 4.6 cents sharp.
const float kTuning = -12.0f * std::log2(48000.0f / plaits::kCorrectedSampleRate);

// Heavy models get fewer voices (measured cost per voice, Linux desktop, -O3): chords 1.9 %,
// particles 3 %, modal 6.2 %, bass drum 2.8 %; everything else under 1.1 %.
int voiceLimit(int model) {
    switch (model) {
        case 14: return 4;  // chords (already several notes per voice)
        case 18: return 3;  // particles
        case 20: return 2;  // modal resonator
        case 21: return 3;  // analog bass drum
        default: return PlaitsSynth::kVoices;
    }
}

struct Event {
    int at = 0;  // frames from the start of the current render() call
    int note = 0;
    float velocity = 0.0f;  // 0 = note off
};

}  // namespace

struct PlaitsSynth::Impl {
    struct Voice {
        plaits::Voice dsp;
        char buffer[16384];
        stmlib::BufferAllocator allocator;
        int note = -1;
        bool gate = false;
        bool retrigger = false;  // a held voice taken by a new note: one block of low trigger first
        float velocity = 0.0f;
        float level = 0.0f;  // our gate envelope, fed to Plaits' low-pass gate
        bool fastRelease = false;  // beyond a new model's voice limit: fade out within a few ms
        uint32_t age = 0;
        uint32_t onBlock = 0;  // block in which the current note started
    };
    std::array<Voice, kVoices> voices;
    plaits::Patch patch{};
    std::array<float, ParamCount> params{0.0f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.3f, 0.8f};
    std::array<Event, 256> events{};
    int eventCount = 0;
    float out[kBlock] = {};
    int outPos = kBlock;  // samples of `out` already used
    uint32_t clock = 0;
    uint32_t block = 0;  // counts rendered blocks
    float attackStep = 0.0f, releaseCoef = 0.0f;

    void updatePatch() {
        patch.engine = static_cast<int>(std::lround(params[Model] * (kModels - 1)));
        patch.harmonics = params[Harmonics];
        patch.timbre = params[Timbre];
        patch.morph = params[Morph];
        patch.decay = params[Decay];
        patch.lpg_colour = params[Colour];
        patch.frequency_modulation_amount = 0.0f;
        patch.timbre_modulation_amount = 0.0f;
        patch.morph_modulation_amount = 0.0f;
        // Release 10 ms .. 2 s, per 12-sample block.
        const float releaseSeconds = 0.01f * std::pow(200.0f, params[Release]);
        releaseCoef = std::exp(-static_cast<float>(kBlock) / (releaseSeconds * 48000.0f));
        attackStep = static_cast<float>(kBlock) / (0.003f * 48000.0f);  // 3 ms
    }

    // False when a note-off must wait for the next block: its note started in this one and
    // would otherwise never be heard (a very short MIDI note).
    bool apply(const Event& e) {
        if (e.velocity > 0.0f) {
            const int limit = voiceLimit(patch.engine);
            Voice* v = nullptr;
            for (int k = 0; k < limit; ++k) if (voices[static_cast<size_t>(k)].note == e.note) v = &voices[static_cast<size_t>(k)];  // retrigger
            if (!v) {
                for (int k = 0; k < limit; ++k) {
                    auto& c = voices[static_cast<size_t>(k)];
                    if (!c.gate && c.level < 0.001f) { v = &c; break; }
                }
            }
            if (!v) {  // steal the oldest
                v = &voices[0];
                for (int k = 0; k < limit; ++k) if (voices[static_cast<size_t>(k)].age < v->age) v = &voices[static_cast<size_t>(k)];
            }
            v->retrigger = v->gate || v->level > 0.001f;
            v->note = e.note;
            v->gate = true;
            v->velocity = std::clamp(e.velocity, 0.0f, 1.0f);
            v->fastRelease = false;
            v->age = ++clock;
            v->onBlock = block;
        } else {
            for (auto& c : voices) if (c.note == e.note && c.gate && c.onBlock == block) return false;
            for (auto& c : voices) if (c.note == e.note && c.gate) c.gate = false;
        }
        return true;
    }

    // One 12-sample block of every sounding voice into `out`.
    void renderBlock() {
        std::fill(out, out + kBlock, 0.0f);
        plaits::Voice::Frame frames[kBlock];
        // A Plaits voice is full scale: about -6 dB each keeps six notes from piling up too high.
        const float gain = params[Level] * 0.5f / 32768.0f;
        for (auto& v : voices) {
            if (!v.gate && v.level < 1e-4f) {
                v.level = 0.0f;
                continue;
            }
            if (v.gate) v.level = std::min(1.0f, v.level + attackStep);
            else v.level *= v.fastRelease ? 0.6f : releaseCoef;
            plaits::Modulations m{};
            m.trigger_patched = true;
            m.level_patched = true;
            m.trigger = v.gate && !v.retrigger ? 1.0f : 0.0f;
            v.retrigger = false;
            m.level = v.level * (0.4f + 0.6f * v.velocity);
            plaits::Patch p = patch;
            p.note = static_cast<float>(v.note) + kTuning;
            v.dsp.Render(p, m, frames, kBlock);
            for (int i = 0; i < kBlock; ++i) out[i] += frames[i].out * gain;
        }
    }
};

PlaitsSynth::PlaitsSynth() : impl_(std::make_unique<Impl>()) {
    for (auto& v : impl_->voices) {
        v.allocator.Init(v.buffer, sizeof(v.buffer));
        v.dsp.Init(&v.allocator);
    }
    impl_->updatePatch();
}

PlaitsSynth::~PlaitsSynth() = default;

void PlaitsSynth::prepare(int) {
    // Plaits always runs at 48 kHz (the engine's rate).
    impl_->updatePatch();
}

void PlaitsSynth::setParam(Param p, float value) {
    if (p < 0 || p >= ParamCount) return;
    impl_->params[static_cast<size_t>(p)] = std::clamp(value, 0.0f, 1.0f);
    impl_->updatePatch();
    if (p == Model) {
        // A heavier model has fewer voices: release the ones beyond its limit.
        for (int k = voiceLimit(impl_->patch.engine); k < kVoices; ++k) {
            auto& v = impl_->voices[static_cast<size_t>(k)];
            v.gate = false;
            v.fastRelease = true;
        }
    }
}

void PlaitsSynth::noteOn(int note, float velocity, int offset) {
    auto& im = *impl_;
    if (im.eventCount < static_cast<int>(im.events.size())) im.events[static_cast<size_t>(im.eventCount++)] = {std::max(0, offset), note, std::max(velocity, 0.01f)};
}

void PlaitsSynth::noteOff(int note, int offset) {
    auto& im = *impl_;
    if (im.eventCount < static_cast<int>(im.events.size())) im.events[static_cast<size_t>(im.eventCount++)] = {std::max(0, offset), note, 0.0f};
}

void PlaitsSynth::allNotesOff() {
    for (auto& v : impl_->voices) v.gate = false;
    impl_->eventCount = 0;
}

void PlaitsSynth::render(float* mono, int numFrames) {
    auto& im = *impl_;
    // Insertion sort, stable and allocation-free (std::stable_sort may allocate a buffer).
    for (int k = 1; k < im.eventCount; ++k) {
        const Event e = im.events[static_cast<size_t>(k)];
        int j = k - 1;
        while (j >= 0 && im.events[static_cast<size_t>(j)].at > e.at) {
            im.events[static_cast<size_t>(j + 1)] = im.events[static_cast<size_t>(j)];
            --j;
        }
        im.events[static_cast<size_t>(j + 1)] = e;
    }
    int next = 0;
    for (int i = 0; i < numFrames; ++i) {
        if (im.outPos >= kBlock) {
            // Events due before the end of this block start with it (at most 11 samples early).
            while (next < im.eventCount && im.events[static_cast<size_t>(next)].at < i + kBlock) {
                if (!im.apply(im.events[static_cast<size_t>(next)])) break;  // the rest waits a block
                ++next;
            }
            im.renderBlock();
            ++im.block;
            im.outPos = 0;
        }
        mono[i] += im.out[im.outPos++];
    }
    // Events past this call (offset beyond numFrames) move to the next one.
    int kept = 0;
    for (int k = next; k < im.eventCount; ++k) {
        Event e = im.events[static_cast<size_t>(k)];
        e.at = std::max(0, e.at - numFrames);
        im.events[static_cast<size_t>(kept++)] = e;
    }
    im.eventCount = kept;
}

int PlaitsSynth::activeVoices() const {
    int n = 0;
    for (const auto& v : impl_->voices) if (v.gate || v.level > 1e-4f) ++n;
    return n;
}

}  // namespace ps
