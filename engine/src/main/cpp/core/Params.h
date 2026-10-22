#pragma once

#include <algorithm>
#include <cmath>

namespace ps {

// Settings that come from outside the audio thread (the app, automation, a project, MIDI) go
// through here. std::clamp lets a NaN through, and one NaN in a filter's, a gain's or a delay's
// state silences it for good (or, turned into a table index, reads out of bounds): a NaN is
// ignored (the setting keeps its value), while +-Inf and huge values clamp to the range.
// (The engine is built with -ffast-math but -fno-finite-math-only, so std::isnan is kept.)
inline bool clampSetting(float& setting, float v, float lo, float hi) {
    if (std::isnan(v)) return false;
    setting = std::clamp(v, lo, hi);
    return true;
}

// `v` clamped to the range, or `fallback` for a NaN.
inline float clampOr(float v, float lo, float hi, float fallback) { return std::isnan(v) ? fallback : std::clamp(v, lo, hi); }

}  // namespace ps
