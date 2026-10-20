#include "NoteTrack.h"

#include <algorithm>
#include <cmath>

namespace ps {

void NoteTrack::setNote(int index, int32_t packed, float velocity) {
    if (index < 0 || index >= kMaxNotes || !std::isfinite(velocity)) return;
    Note n;
    n.start = packed & 0xFFF;
    n.length = std::max(1, (packed >> 12) & 0xFFF);
    n.pitch = (packed >> 24) & 0x7F;
    n.velocity = std::clamp(velocity, 0.0f, 1.0f);
    notes_[static_cast<size_t>(index)] = n;
}

void NoteTrack::setCount(int count) { count_ = std::clamp(count, 0, kMaxNotes); }

void NoteTrack::setSteps(int steps) { steps_ = std::clamp(steps, 1, kMaxSteps); }

void NoteTrack::push(int64_t at, int pitch, float velocity) {
    if (pendingCount_ >= static_cast<int>(pending_.size())) return;  // never in practice: drop
    pending_[static_cast<size_t>(pendingCount_++)] = {at, pitch, velocity};
}

void NoteTrack::onStep(int offset, int step, double samplesPerStep) {
    if (step < 0 || count_ == 0) return;
    const int first = (step % steps_) * kTicksPerStep;
    const double perTick = samplesPerStep / kTicksPerStep;
    for (int i = 0; i < count_; ++i) {
        const Note& n = notes_[static_cast<size_t>(i)];
        if (n.velocity <= 0.0f || n.start < first || n.start >= first + kTicksPerStep) continue;
        const int64_t on = offset + std::llround((n.start - first) * perTick);
        const int64_t off = on + std::max<int64_t>(1, std::llround(n.length * perTick));
        // The same pitch still held by an earlier note (rounding, swing, overlapping notes): that
        // note ends where this one starts, so its late note-off cannot cut the new one.
        for (int k = 0; k < pendingCount_; ++k) {
            Pending& p = pending_[static_cast<size_t>(k)];
            if (p.velocity == 0.0f && p.pitch == n.pitch && p.at > on) p.at = on;
        }
        // Full (hundreds of long notes at once): this note is dropped whole, never a note-on
        // without its note-off (it would hang).
        if (pendingCount_ + 2 > static_cast<int>(pending_.size())) continue;
        push(on, n.pitch, n.velocity);
        push(off, n.pitch, 0.0f);
    }
}

}  // namespace ps
