#include "NoteTrack.h"

#include <algorithm>
#include <cmath>

namespace ps {

void NoteTrack::setNote(int index, int32_t packed, float velocity, int high) {
    if (index < 0 || index >= kMaxNotes || !std::isfinite(velocity)) return;
    Note n;
    n.start = (packed & 0xFFF) | ((high & 0x7) << 12);
    n.length = std::max(1, ((packed >> 12) & 0xFFF) | (((high >> 3) & 0x7) << 12));
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

void NoteTrack::onStep(int offset, int step, double samplesPerStep, double swing) {
    if (step < 0 || count_ == 0) return;
    const int first = (step % steps_) * kTicksPerStep;
    const double perTick = samplesPerStep / kTicksPerStep;
    // Odd steps are late by the swing, as the sequencer plays them, and the swing warps time inside
    // the steps as the drums' rolls do: an even step keeps its length, an odd one starts `swing` late
    // and is that much shorter. A note between two sixteenths (a triplet, a 1/32) keeps its place
    // between them, never past the next one (audit 06: flams). A note ends on the same warped grid
    // its successor starts on, so notes that only touch never overlap (no glide between them).
    const double s = std::isfinite(swing) ? std::clamp(swing, 0.0, 1.0) : 0.0;
    const double swingFrames = s * samplesPerStep;
    const double unswung = offset - ((step & 1) != 0 ? swingFrames : 0.0);
    for (int i = 0; i < count_; ++i) {
        const Note& n = notes_[static_cast<size_t>(i)];
        if (n.velocity <= 0.0f || n.start < first || n.start >= first + kTicksPerStep) continue;
        // Inside an odd step a tick is (1 - swing) of its length (nothing changes on the step's start).
        const int into = n.start - first;
        const int64_t on = offset + std::llround((step & 1) != 0 ? into * perTick * (1.0 - s) : into * perTick);
        const int end = n.start - first + n.length;  // ticks from this step's start
        const bool endSwung = ((step + end / kTicksPerStep) & 1) != 0;
        const double endLate = endSwung ? swingFrames - (end % kTicksPerStep) * perTick * s : 0.0;
        const int64_t off = std::max<int64_t>(on + 1, std::llround(unswung + end * perTick + endLate));
        for (int k = 0; k < pendingCount_; ++k) {
            Pending& p = pending_[static_cast<size_t>(k)];
            if (p.velocity != 0.0f || p.at <= on) continue;
            // The same pitch still held by an earlier note (overlapping notes): that note ends where
            // this one starts, so its late note-off cannot cut the new one. Another pitch ending
            // within a frame or two of this start only touches it (rounding): it ends here too, and
            // the new note gets its own attack. A longer overlap is a slide ('/'), kept.
            if (p.pitch == n.pitch || p.at - on <= kTouchFrames) p.at = on;
        }
        // Full (hundreds of long notes at once): this note is dropped whole, never a note-on
        // without its note-off (it would hang).
        if (pendingCount_ + 2 > static_cast<int>(pending_.size())) continue;
        push(on, n.pitch, n.velocity);
        push(off, n.pitch, 0.0f);
    }
}

}  // namespace ps
