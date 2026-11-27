#include "MeasuredDrums.h"

#include <algorithm>
#include <cstdint>

namespace ps::measured {

namespace {

// Envelope rule: a hold, then -40 dB over the fall (straight in dB), so the hit ends at
// t20 = hold + fall / 2 and t40 = hold + fall under its peak: hold = 2 t20 - t40, fall = 2 (t40 - t20),
// both from the medians of CIBLES-BATTERIE.md (values a little inside the p10..p90 range).

Recipe kick(int style) {
    // {steady note Hz (a key note inside the measured range), drop start Hz (median), hold, fall, click, drive}
    struct K { float hz, start, hold, fall, click, drive; };
    static constexpr K k[kStyles] = {
        {41.2f, 322.0f, 170.0f, 98.0f, 0.15f, 0.10f},   // house: E1, long round body
        {49.0f, 335.0f, 188.0f, 126.0f, 0.55f, 0.30f},  // tech house: G1, a firmer click (was too dark)
        {49.0f, 327.0f, 340.0f, 60.0f, 0.45f, 0.30f},   // techno: G1, harder and long (t20 / t40 near 373 / 394 ms)
        {43.65f, 190.0f, 65.0f, 64.0f, 0.05f, 0.05f},   // trap: F1, short (the 808 holds the low end)
        {49.0f, 208.0f, 79.0f, 99.0f, 0.45f, 0.30f},    // dnb: G1, short and punchy
        {43.65f, 233.0f, 169.0f, 114.0f, 0.45f, 0.25f}, // dubstep: F1
    };
    const K& c = k[style];
    Recipe r;
    r.bodyLevel = 0.9f;
    r.startHz = c.start;
    r.endHz = c.hz;
    r.dropHalfMs = 12.0f;
    r.bodyHoldMs = c.hold;
    r.bodyFallMs = c.fall;
    r.bodyDrive = c.drive;
    r.clickLevel = c.click;
    r.clickMs = 2.0f;
    r.clickHpHz = 2500.0f;
    r.gain = 0.75f;
    return r;
}

Recipe bass808(int style) {
    static constexpr float hz[kStyles] = {55.0f, 49.0f, 43.65f, 43.65f, 43.65f, 41.2f};
    Recipe r;
    r.bodyLevel = 0.85f;
    r.startHz = 200.0f;
    r.endHz = hz[style];
    r.dropHalfMs = 12.0f;
    r.bodyHoldMs = 1100.0f;
    r.bodyFallMs = 700.0f;
    r.bodyDrive = 0.2f;
    r.clickLevel = 0.1f;
    r.clickMs = 2.0f;
    r.clickHpHz = 3000.0f;
    return r;
}

Recipe clap(int style) {
    // {hp, lp, hold, fall}
    struct C { float hp, lp, hold, fall; };
    static constexpr C c[kStyles] = {
        {600.0f, 1400.0f, 10.0f, 104.0f},   // house
        {800.0f, 2200.0f, 30.0f, 113.0f},   // tech house
        {900.0f, 2600.0f, 56.0f, 94.0f},    // techno
        {900.0f, 1300.0f, 48.0f, 58.0f},    // trap
        {900.0f, 1800.0f, 10.0f, 133.0f},   // dnb
        {600.0f, 1300.0f, 10.0f, 266.0f},   // dubstep: long
    };
    const C& x = c[style];
    Recipe r;
    r.noiseLevel = 1.0f;
    r.noiseHpHz = x.hp;
    r.noiseLpHz = x.lp;
    r.noiseHoldMs = x.hold;
    r.noiseFallMs = x.fall;
    r.bursts = 3;
    r.burstGapMs = 9.0f;
    r.gain = 1.0f;
    return r;
}

Recipe snare(int style) {
    // {body Hz, body level, noise hp, noise lp, hold, fall}
    struct S { float hz, body, hp, lp, hold, fall; };
    static constexpr S s[kStyles] = {
        {167.0f, 0.9f, 400.0f, 1200.0f, 28.0f, 86.0f},    // house: strong body
        {187.0f, 0.9f, 400.0f, 1100.0f, 20.0f, 115.0f},   // tech house
        {187.0f, 0.9f, 400.0f, 1100.0f, 20.0f, 115.0f},   // techno
        {200.0f, 0.6f, 900.0f, 2800.0f, 49.0f, 147.0f},   // trap: brighter
        {210.0f, 0.35f, 1500.0f, 5000.0f, 14.0f, 86.0f},  // dnb: small body, bright
        {230.0f, 0.8f, 400.0f, 1500.0f, 0.0f, 500.0f},    // dubstep: long tail, darker
    };
    const S& x = s[style];
    Recipe r;
    r.bodyLevel = x.body;
    r.startHz = x.hz * 1.6f;
    r.endHz = x.hz;
    r.dropHalfMs = 6.0f;
    r.bodyHoldMs = x.hold;
    r.bodyFallMs = x.fall;
    r.noiseLevel = 0.4f;
    r.noiseHpHz = x.hp;
    r.noiseLpHz = x.lp;
    r.noiseHoldMs = x.hold;
    r.noiseFallMs = x.fall;
    r.clickLevel = 0.2f;
    r.clickMs = 1.5f;
    r.clickHpHz = 3000.0f;
    r.gain = 0.85f;  // the strong bodies peak near full scale otherwise
    return r;
}

Recipe hat(int style, bool open) {
    struct H { float hold, fall, hp, lp; };
    static constexpr H closed[kStyles] = {
        {23.0f, 62.0f, 4500.0f, 8000.0f},  {44.0f, 48.0f, 5000.0f, 8500.0f},  {48.0f, 47.0f, 5000.0f, 8500.0f},
        {21.0f, 50.0f, 5000.0f, 10500.0f}, {15.0f, 74.0f, 5500.0f, 11500.0f}, {26.0f, 64.0f, 6000.0f, 12000.0f},
    };
    static constexpr H opened[kStyles] = {
        {37.0f, 275.0f, 4000.0f, 8000.0f},   {37.0f, 275.0f, 4500.0f, 9000.0f}, {37.0f, 275.0f, 4500.0f, 9000.0f},
        {778.0f, 172.0f, 8000.0f, 20000.0f}, {37.0f, 275.0f, 4500.0f, 9000.0f}, {88.0f, 50.0f, 4500.0f, 9000.0f},
    };
    const H& x = open ? opened[style] : closed[style];
    Recipe r;
    r.noiseLevel = 0.6f;
    r.metalLevel = 0.8f;
    r.metalHz = 540.0f;
    r.noiseHpHz = x.hp;
    r.noiseLpHz = x.lp;
    r.noiseHoldMs = x.hold;
    r.noiseFallMs = x.fall;
    r.gain = 1.0f;
    return r;
}

Recipe rim() {
    Recipe r;
    r.bodyLevel = 0.7f;
    r.startHz = 1300.0f;
    r.endHz = 1100.0f;
    r.dropHalfMs = 3.0f;
    r.bodyHoldMs = 0.0f;
    r.bodyFallMs = 110.0f;
    r.noiseLevel = 0.5f;
    r.noiseHpHz = 1500.0f;
    r.noiseLpHz = 5000.0f;
    r.noiseFallMs = 40.0f;
    return r;
}

Recipe tom(int style) {
    Recipe r;
    r.bodyLevel = 0.85f;
    if (style == House) {
        r.startHz = 201.0f;
        r.endHz = 117.0f;
        r.dropHalfMs = 6.6f;
        r.bodyHoldMs = 65.0f;
        r.bodyFallMs = 164.0f;
    } else {
        r.startHz = 195.0f;
        r.endHz = 95.0f;
        r.dropHalfMs = 10.0f;
        r.bodyHoldMs = 10.0f;
        r.bodyFallMs = 504.0f;
    }
    r.bodyDrive = 0.1f;
    r.clickLevel = 0.1f;
    r.clickMs = 2.0f;
    r.clickHpHz = 1500.0f;
    return r;
}

Recipe crash(int style) {
    struct X { float hold, fall, hp, lp; };
    static constexpr X x[kStyles] = {
        {637.0f, 728.0f, 2000.0f, 7000.0f}, {637.0f, 728.0f, 2000.0f, 7000.0f}, {637.0f, 728.0f, 2000.0f, 7000.0f},
        {352.0f, 440.0f, 1800.0f, 5500.0f}, {500.0f, 600.0f, 3000.0f, 7700.0f}, {733.0f, 960.0f, 1400.0f, 4500.0f},
    };
    const X& c = x[style];
    Recipe r;
    r.noiseLevel = 0.7f;
    r.metalLevel = 0.6f;
    r.metalHz = 420.0f;
    r.noiseHpHz = c.hp;
    r.noiseLpHz = c.lp;
    r.noiseHoldMs = c.hold;
    r.noiseFallMs = c.fall;
    return r;
}

}  // namespace

Recipe recipe(int family, int style) {
    if (style < 0 || style >= kStyles) style = House;
    switch (family) {
        case Kick: return kick(style);
        case Bass808: return bass808(style);
        case Clap: return clap(style);
        case Snare: return snare(style);
        case ClosedHat: return hat(style, false);
        case OpenHat: return hat(style, true);
        case Rim: return rim();
        case Tom: return tom(style);
        case Crash: return crash(style);
        default: return Recipe{};
    }
}

Recipe variant(int family, int style, int which) {
    Recipe r = recipe(family, style);
    if (which == 0) return r;
    const bool low = which < 0;
    if (family == Kick) {
        // {note Hz, hold, fall} at the style's 25th / 75th percentiles: low = the long, low hits
        // (t20, t40 at p75, note at p25), dry = the short, high ones (t20, t40 at p25, note at p75).
        // A modern attack on both (quicker drop, a click, a little drive: no « tambour »).
        struct V { float hz, hold, fall; };
        static constexpr V v[kStyles][2] = {
            {{38.9f, 202.0f, 92.0f}, {46.25f, 140.0f, 104.0f}},   // house
            {{43.65f, 300.0f, 120.0f}, {51.9f, 179.0f, 78.0f}},   // tech house
            {{46.25f, 320.0f, 120.0f}, {55.0f, 175.0f, 88.0f}},   // techno
            {{41.2f, 83.0f, 60.0f}, {49.0f, 56.0f, 58.0f}},       // trap
            {{41.2f, 129.0f, 106.0f}, {51.9f, 70.0f, 80.0f}},     // dnb (the recipe is already short: dry goes under p25)
            {{43.65f, 158.0f, 160.0f}, {51.9f, 120.0f, 96.0f}},   // dubstep
        };
        const V& c = v[style][low ? 0 : 1];
        r.endHz = c.hz;
        r.bodyHoldMs = c.hold;
        r.bodyFallMs = c.fall;
        r.dropHalfMs = 8.0f;
        r.clickLevel = std::max(r.clickLevel, 0.35f);
        r.bodyDrive = std::max(r.bodyDrive, 0.25f);
        return r;
    }
    if (family == Clap || family == Snare || family == ClosedHat) {
        // Longer and darker, or shorter and brighter.
        const float len = low ? 1.4f : 0.65f, tone = low ? 0.8f : 1.25f;
        r.noiseHoldMs *= len;
        r.noiseFallMs *= len;
        r.bodyHoldMs *= len;
        r.bodyFallMs *= len;
        r.noiseLpHz = std::min(r.noiseLpHz * tone, 18000.0f);
        r.noiseHpHz *= low ? 0.85f : 1.15f;
    }
    return r;
}

Recipe deepTechnoKick() {
    // A clean, round body that hits hard: G1, a quick drop from a firm start, a short click, a touch of warmth.
    Recipe r;
    r.bodyLevel = 0.9f;
    r.startHz = 260.0f;
    r.endHz = 49.0f;
    r.dropHalfMs = 7.0f;
    r.bodyHoldMs = 230.0f;
    r.bodyFallMs = 140.0f;
    r.bodyDrive = 0.15f;
    r.clickLevel = 0.3f;
    r.clickMs = 1.5f;
    r.clickHpHz = 3000.0f;
    r.gain = 0.8f;
    return r;
}

namespace {
// Printed by scripts/son/matin_choix.py (kick, 808, clap, snare, closed hat, open hat, rim, tom,
// crash x house, tech house, techno, trap, dnb, dubstep); matin_choix.sh checks it is copied here.
static constexpr int8_t kChoice[kFamilies][kStyles] = {
    {0, 0, 0, 0, 0, 0},  // kick
    {9, 9, 9, 9, 9, 9},  // 808
    {0, 0, 0, 9, 9, 0},  // clap
    {9, 9, 9, 0, 0, 0},  // caisse_claire
    {0, 0, 0, 0, 0, 0},  // charley_ferme
    {9, 0, 0, 0, 0, 0},  // charley_ouvert
    {9, 9, 9, 9, 9, 9},  // rim
    {9, 0, 0, 0, 0, 0},  // tom
    {0, 0, 0, 0, 9, 0},  // crash
};
}  // namespace

int choice(int family, int style) {
    if (family < 0 || family >= kFamilies) return ChoiceKeep;
    if (style < 0 || style >= kStyles) style = House;
    return kChoice[family][style];
}

Recipe chosen(int family, int style) {
    const int c = choice(family, style);
    if (c == ChoiceDeep) return deepTechnoKick();
    return variant(family, style, c == ChoiceKeep ? 0 : c);
}

}  // namespace ps::measured
