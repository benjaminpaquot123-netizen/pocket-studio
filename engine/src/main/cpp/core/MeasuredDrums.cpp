#include "MeasuredDrums.h"

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
        {49.0f, 335.0f, 188.0f, 126.0f, 0.30f, 0.15f},  // tech house: G1
        {49.0f, 327.0f, 200.0f, 120.0f, 0.45f, 0.30f},  // techno: G1, harder
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
        {800.0f, 2500.0f, 30.0f, 110.0f},   // house
        {900.0f, 3200.0f, 50.0f, 130.0f},   // tech house
        {1000.0f, 3800.0f, 70.0f, 130.0f},  // techno
        {1200.0f, 2000.0f, 40.0f, 120.0f},  // trap
        {1300.0f, 3000.0f, 25.0f, 100.0f},  // dnb
        {800.0f, 2500.0f, 90.0f, 230.0f},   // dubstep: long
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
        {230.0f, 0.8f, 500.0f, 2500.0f, 0.0f, 500.0f},    // dubstep: long tail
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
        {23.0f, 62.0f, 6000.0f, 16000.0f}, {44.0f, 48.0f, 6500.0f, 16000.0f}, {48.0f, 47.0f, 6500.0f, 16000.0f},
        {21.0f, 50.0f, 5000.0f, 16000.0f}, {3.0f, 74.0f, 6000.0f, 18000.0f},  {18.0f, 64.0f, 7000.0f, 18000.0f},
    };
    static constexpr H opened[kStyles] = {
        {52.0f, 102.0f, 4000.0f, 8000.0f},  {52.0f, 158.0f, 6000.0f, 16000.0f}, {52.0f, 158.0f, 6000.0f, 16000.0f},
        {778.0f, 172.0f, 8000.0f, 20000.0f}, {52.0f, 158.0f, 6000.0f, 16000.0f}, {87.0f, 51.0f, 6500.0f, 16000.0f},
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
        {608.0f, 1084.0f, 3000.0f, 10000.0f}, {608.0f, 1084.0f, 3000.0f, 10000.0f}, {608.0f, 1084.0f, 3000.0f, 10000.0f},
        {608.0f, 1084.0f, 3000.0f, 10000.0f}, {0.0f, 1075.0f, 6000.0f, 16000.0f},   {733.0f, 960.0f, 2500.0f, 8500.0f},
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

}  // namespace ps::measured
