// L12 « Moteur : attaque et poids » (tour 9, docs/audits/GRAND-REMPLACEMENT-V2.md § 5): the transient
// shaper (« Attaque »), the drum bus saturation (« Bus batterie ») and the mid/side EQ (« Graves mono »).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace l12_tests {

using studio_tests::allFinite;

// A drum loop: four kicks a second (a 55 Hz sine falling in pitch and level, peak 0.8), a quiet hat
// (noise bursts at 0.08) in between, as a drum bus hears it.
void drums(std::vector<float>& l, std::vector<float>& r, size_t n) {
    l.assign(n, 0.0f);
    r.assign(n, 0.0f);
    uint32_t seed = 777u;
    const size_t beat = kRate / 4;
    for (size_t i = 0; i < n; ++i) {
        const size_t k = i % beat;
        const double t = static_cast<double>(k) / kRate;
        const double kick = 0.8 * std::exp(-t * 18.0) * std::sin(2 * M_PI * (55.0 * t + 40.0 * (1.0 - std::exp(-t * 30.0)) / 30.0));
        seed = seed * 1664525u + 1013904223u;
        const double noise = (static_cast<double>(seed >> 8) / 16777216.0 - 0.5) * 2.0;
        const size_t h = (i + beat / 2) % beat;
        const double hat = h < kRate / 50 ? 0.08 * noise * std::exp(-static_cast<double>(h) / kRate * 150.0) : 0.0;
        l[i] = r[i] = static_cast<float>(kick + hat);
    }
}

void run(ps::InsertSlot& fx, std::vector<float>& l, std::vector<float>& r, int block = 192) {
    for (size_t pos = 0; pos < l.size(); pos += static_cast<size_t>(block)) {
        const int n = static_cast<int>(std::min<size_t>(static_cast<size_t>(block), l.size() - pos));
        fx.process(l.data() + pos, r.data() + pos, n);
    }
}

float peak(const std::vector<float>& x, size_t from = 0, size_t to = SIZE_MAX) {
    float p = 0.0f;
    for (size_t i = from; i < std::min(to, x.size()); ++i) p = std::max(p, std::fabs(x[i]));
    return p;
}

double rmsDb(const std::vector<float>& x, size_t from, size_t to) {
    double s = 0.0;
    for (size_t i = from; i < to; ++i) s += double(x[i]) * x[i];
    return 10.0 * std::log10(s / double(to - from) + 1e-30);
}

ps::InsertSlot slot(int type, std::initializer_list<float> params) {
    ps::InsertSlot fx;
    fx.prepare(kRate);
    fx.setType(type);
    int p = 0;
    for (float v : params) fx.setParam(p++, v);
    return fx;
}

// The shaper's attack knob lifts the hit's first milliseconds, its sustain knob the tail; at 0.5 both
// leave the sound bit for bit.
void testTransientShaper() {
    const size_t n = kRate;
    std::vector<float> dl, dr;
    drums(dl, dr, n);
    const size_t beat = kRate / 4, at = 2 * beat;  // the third kick (the followers settled)
    {
        auto fx = slot(ps::InsertSlot::TransientFx, {0.5f, 0.5f, 1.0f, 0.5f});
        auto l = dl, r = dr;
        run(fx, l, r);
        CHECK(l == dl && r == dr);
    }
    {
        auto fx = slot(ps::InsertSlot::TransientFx, {1.0f, 0.5f, 1.0f, 0.5f});
        auto l = dl, r = dr;
        run(fx, l, r);
        CHECK(allFinite(l) && allFinite(r));
        const double hitUp = rmsDb(l, at, at + kRate / 200) - rmsDb(dl, at, at + kRate / 200);
        const double tailUp = rmsDb(l, at + kRate / 10, at + kRate / 5) - rmsDb(dl, at + kRate / 10, at + kRate / 5);
        CHECK(hitUp > 3.0);    // the attack's first 5 ms
        CHECK(tailUp < 1.0);   // the tail barely moves
        CHECK(peak(l) < 4.0f * peak(dl));  // at most +12 dB
    }
    {
        auto fx = slot(ps::InsertSlot::TransientFx, {0.5f, 1.0f, 1.0f, 0.5f});
        auto l = dl, r = dr;
        run(fx, l, r);
        const double tailUp = rmsDb(l, at + kRate / 10, at + kRate / 5) - rmsDb(dl, at + kRate / 10, at + kRate / 5);
        CHECK(tailUp > 2.0);
    }
    {
        // Attack down: the hit softer.
        auto fx = slot(ps::InsertSlot::TransientFx, {0.0f, 0.5f, 1.0f, 0.5f});
        auto l = dl, r = dr;
        run(fx, l, r);
        CHECK(rmsDb(l, at, at + kRate / 200) - rmsDb(dl, at, at + kRate / 200) < -2.0);
    }
}

// The drum bus: the kick's peak kept (within 0.5 dB), the drums' mean level up 1 to 3 dB at the
// default settings, nothing at mix 0 but the latency, and no jump when a louder hit comes after a
// quiet part (the scale lined up with the shaped sound).
void testDrumBus() {
    const size_t n = 2 * kRate;
    std::vector<float> dl, dr;
    drums(dl, dr, n);
    const size_t from = kRate / 2;
    {
        auto fx = slot(ps::InsertSlot::DrumBusFx, {});
        auto l = dl, r = dr;
        run(fx, l, r);
        CHECK(allFinite(l) && allFinite(r));
        const double peakDb = 20.0 * std::log10(peak(l, from) / peak(dl, from));
        const double meanUp = rmsDb(l, from, n) - rmsDb(dl, from, n);
        std::printf("    drum bus default: peak %+.2f dB, mean %+.2f dB\n", peakDb, meanUp);
        CHECK(std::fabs(peakDb) < 0.5);
        CHECK(meanUp > 1.0 && meanUp < 3.0);
    }
    {
        auto fx = slot(ps::InsertSlot::DrumBusFx, {0.8f, 0.0f, 0.5f});
        auto l = dl, r = dr;
        run(fx, l, r);
        const int d = ps::DriveShaperPair::kDelay;
        double err = 0.0;
        for (size_t i = static_cast<size_t>(d); i < n; ++i) err = std::max(err, std::fabs(double(l[i]) - dl[i - static_cast<size_t>(d)]));
        CHECK(err < 1e-5);
    }
    {
        // A second of quiet hats, then a kick 20 dB over them: no sample over the kick's own peak.
        std::vector<float> l(kRate + kRate / 4, 0.0f), r;
        for (size_t i = 0; i < size_t(kRate); ++i) l[i] = 0.08f * static_cast<float>(std::sin(2 * M_PI * 3000.0 * i / kRate));
        for (size_t i = kRate; i < l.size(); ++i) {
            const double t = double(i - kRate) / kRate;
            l[i] = static_cast<float>(0.8 * std::exp(-t * 18.0) * std::sin(2 * M_PI * 60.0 * t));
        }
        r = l;
        const auto in = l;
        auto fx = slot(ps::InsertSlot::DrumBusFx, {1.0f, 1.0f, 0.5f});
        run(fx, l, r, 64);
        CHECK(peak(l, kRate - 64) < 1.02f * peak(in, kRate));
        CHECK(allFinite(l));
    }
}

// The mid/side EQ: the sides' lows (60 Hz, out of phase) gone at the default cutoff, the sides' highs
// (2 kHz) and the middle's lows kept; at 0 the cutoff is off.
void testMidSideEq() {
    const size_t n = kRate;
    std::vector<float> l(n), r(n);
    auto make = [&]() {
        for (size_t i = 0; i < n; ++i) {
            const double t = double(i) / kRate;
            const float mid = 0.3f * static_cast<float>(std::sin(2 * M_PI * 50.0 * t));
            const float side = 0.2f * static_cast<float>(std::sin(2 * M_PI * 60.0 * t)) + 0.1f * static_cast<float>(std::sin(2 * M_PI * 2000.0 * t));
            l[i] = mid + side;
            r[i] = mid - side;
        }
    };
    auto sides = [&](std::vector<float>& s, std::vector<float>& m) {
        s.resize(n);
        m.resize(n);
        for (size_t i = 0; i < n; ++i) {
            s[i] = 0.5f * (l[i] - r[i]);
            m[i] = 0.5f * (l[i] + r[i]);
        }
    };
    std::vector<float> s0, m0, s1, m1;
    make();
    sides(s0, m0);
    auto fx = slot(ps::InsertSlot::MidSideEqFx, {});
    run(fx, l, r);
    CHECK(allFinite(l) && allFinite(r));
    sides(s1, m1);
    const size_t from = n / 2;
    using studio_tests::bandEnergy;
    const double sideLow = 10.0 * std::log10(bandEnergy(s1, from, n, 55, 65) / bandEnergy(s0, from, n, 55, 65));
    const double sideHigh = 10.0 * std::log10(bandEnergy(s1, from, n, 1995, 2005) / bandEnergy(s0, from, n, 1995, 2005));
    const double midLow = 10.0 * std::log10(bandEnergy(m1, from, n, 45, 55) / bandEnergy(m0, from, n, 45, 55));
    std::printf("    mid/side: side 60 Hz %+.1f dB, side 2 kHz %+.2f dB, mid 50 Hz %+.2f dB\n", sideLow, sideHigh, midLow);
    CHECK(sideLow < -20.0);
    CHECK(std::fabs(sideHigh) < 0.5);
    CHECK(std::fabs(midLow) < 0.5);
    // Off: the sides' lows stay.
    make();
    auto off = slot(ps::InsertSlot::MidSideEqFx, {0.0f});
    run(off, l, r);
    sides(s1, m1);
    CHECK(std::fabs(10.0 * std::log10(bandEnergy(s1, from, n, 55, 65) / bandEnergy(s0, from, n, 55, 65))) < 0.5);
    // Middle lows +6 dB at 1: the 50 Hz middle comes up.
    make();
    auto lows = slot(ps::InsertSlot::MidSideEqFx, {0.5f, 1.0f});
    run(lows, l, r);
    sides(s1, m1);
    CHECK(10.0 * std::log10(bandEnergy(m1, from, n, 45, 55) / bandEnergy(m0, from, n, 45, 55)) > 4.0);
}

// The three in the audio thread: no allocation, a NaN in comes out as silence for that frame and the
// sound comes back after it; every knob turned while it plays.
void testRealTimeRules() {
    for (int type : {int(ps::InsertSlot::TransientFx), int(ps::InsertSlot::DrumBusFx), int(ps::InsertSlot::MidSideEqFx)}) {
        auto fx = slot(type, {});
        std::vector<float> l, r;
        drums(l, r, kRate / 2);
        l[1000] = std::numeric_limits<float>::quiet_NaN();
        r[2000] = std::numeric_limits<float>::infinity();
        g_allocations = 0;
        g_countAllocations = true;
        for (size_t pos = 0; pos < l.size(); pos += 128) {
            fx.setParam(static_cast<int>(pos / 128 % 4), static_cast<float>(pos % 997) / 997.0f);
            fx.process(l.data() + pos, r.data() + pos, static_cast<int>(std::min<size_t>(128, l.size() - pos)));
        }
        g_countAllocations = false;
        CHECK(g_allocations.load() == 0);
        CHECK(allFinite(l) && allFinite(r));
        CHECK(peak(l, l.size() / 2) > 0.05f);
        CHECK(peak(l) < 4.0f);
    }
}

}  // namespace l12_tests

#define PS_L12_TESTS                                         \
    {"l12_transient_shaper", l12_tests::testTransientShaper}, \
    {"l12_drum_bus", l12_tests::testDrumBus},                 \
    {"l12_mid_side_eq", l12_tests::testMidSideEq},            \
    {"l12_real_time_rules", l12_tests::testRealTimeRules},
