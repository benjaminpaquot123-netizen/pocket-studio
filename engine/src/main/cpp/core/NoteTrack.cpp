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
        push(on, n.pitch, n.velocity);
        push(off, n.pitch, 0.0f);
    }
}

}  // namespace ps
