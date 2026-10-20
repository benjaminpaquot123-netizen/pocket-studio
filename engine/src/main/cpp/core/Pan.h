#pragma once

#include <algorithm>
#include <cmath>

namespace ps {

// The mixer's pan law: equal power, centre at unity on each side.
inline void panGains(float pan, float& left, float& right) {
    const float angle = (std::clamp(pan, -1.0f, 1.0f) + 1.0f) * 0.25f * 3.14159265f;
    left = std::cos(angle) * 1.41421356f;
    right = std::sin(angle) * 1.41421356f;
}

}  // namespace ps
