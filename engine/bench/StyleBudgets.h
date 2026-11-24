#pragma once

// A load budget per style, relative (tour 9, B1): a style's song may cost so many times a fixed
// reference scene measured in the same pass, on the same thread. A ratio, not milliseconds: it
// reads the same on a fast PC and on a slow runner, and it moves when a brick of the engine gets
// dearer for that style. ps_live measures it on every live bench (the songs are recorded there);
// the engine tests hold the reference and the verdict (engine/host-tests/stylebudget_tests.inc).
// On a PC it guards the ORDER of magnitude of each style, not whether a tablet cuts out.

#include <algorithm>
#include <cstring>
#include <vector>

#include <time.h>

#include "Engine.h"

namespace ps::stylebudget {

struct Budget {
    const char* style;
    float measured;  // style over reference when the budget was set (150 s of song, 96-frame blocks)
};

// The margin over `measured`: the ratio moves with the processor (x86, ARM) and the song's length;
// a brick that gets half as dear again for a style passes it.
constexpr float kMargin = 1.5f;

// Measured with ps_live on the 37 recorded songs (150 s each, 96-frame blocks, x86_64), tour 9:
// the `ratio=` of each BUDGET line. To set one again: scripts/live-bench.sh, then copy the ratio.
inline constexpr Budget kBudgets[] = {
    {"acid_house", 0.80f},
    {"acid_techno", 0.70f},
    {"acidcore", 0.87f},
    {"afro_house", 1.33f},
    {"afrobeats", 1.10f},
    {"amapiano", 1.26f},
    {"boom_bap", 0.93f},
    {"deep_house", 1.15f},
    {"detroit", 1.01f},
    {"disco", 1.22f},
    {"dnb", 1.95f},
    {"drill", 1.16f},
    {"dubstep", 1.55f},
    {"gabber", 1.11f},
    {"grime", 1.06f},
    {"hard_techno", 0.96f},
    {"house", 1.29f},
    {"industrial_techno", 0.99f},
    {"jersey_club", 0.91f},
    {"jump_up", 2.26f},
    {"jungle", 1.41f},
    {"liquid_dnb", 2.25f},
    {"lofi", 1.13f},
    {"minimal", 0.80f},
    {"neurofunk", 1.86f},
    {"phonk", 1.22f},
    {"pop", 1.44f},
    {"reggaeton", 1.48f},
    {"retro_techno", 0.89f},
    {"riddim", 1.43f},
    {"speed_garage", 1.30f},
    {"synthwave", 1.26f},
    {"tech_house", 1.33f},
    {"techno", 0.99f},
    {"trance", 1.03f},
    {"trap", 1.30f},
    {"uk_garage", 1.32f},
};

inline const Budget* budgetOf(const char* style) {
    for (const Budget& b : kBudgets) {
        if (!std::strcmp(b.style, style)) return &b;
    }
    return nullptr;
}

enum class Verdict { Within, Over, NoBudget, NoMeasure };

// styleLoad and referenceLoad: computing time over music time, both from the same pass.
inline Verdict judge(const char* style, double styleLoad, double referenceLoad) {
    const Budget* b = budgetOf(style);
    if (!b) return Verdict::NoBudget;
    if (!(styleLoad > 0.0) || !(referenceLoad > 0.0)) return Verdict::NoMeasure;
    return styleLoad / referenceLoad <= static_cast<double>(b->measured * kMargin) ? Verdict::Within : Verdict::Over;
}

inline double threadCpuSeconds() {
    timespec ts{};
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &ts);
    return static_cast<double>(ts.tv_sec) + static_cast<double>(ts.tv_nsec) * 1e-9;
}

// The reference scene: the whole drum kit on every step and a six-note chord of the synth, at
// 140 BPM, through the mixer and the master chain. Its load (computing time over music time) in
// blocks of `burst` frames, the best of three two-second passes after half a second of warm-up.
// Not real-time code: it builds an engine. Call it outside the audio callback.
inline double referenceLoad(int burst = 96) {
    constexpr int kRate = 48000;
    Engine engine(kRate);
    engine.post({CommandType::SetTempo, 140.0f});
    for (int lane = 0; lane < 16; ++lane) {
        for (int step = 0; step < 16; ++step) engine.post({CommandType::SetDrumHit, 0.8f, lane, step});
    }
    for (int note : {45, 52, 57, 60, 64, 69}) engine.post({CommandType::SynthNoteOn, 0.7f, note});
    engine.post({CommandType::SetTransport, 1.0f, 0});
    std::vector<float> block(static_cast<size_t>(burst) * 2);
    const auto play = [&](int frames) {
        for (int done = 0; done < frames; done += burst) engine.process(nullptr, 0, 0, block.data(), burst, 2);
    };
    play(kRate / 2);
    double best = 1e9;
    for (int pass = 0; pass < 3; ++pass) {
        const double t0 = threadCpuSeconds();
        play(kRate * 2);
        best = std::min(best, (threadCpuSeconds() - t0) / 2.0);
    }
    return best;
}

}  // namespace ps::stylebudget
