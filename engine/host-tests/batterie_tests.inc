// Tour 9, lot « batterie » (docs/audits/ultra/tour-9-batterie-rapport.md): the kit v3 voices moved
// toward the medians measured on 750 commercial one-shots (the sounds themselves never enter the
// repository: only their medians, written below as the targets). Each test renders one hit of the
// kit v3 voice at 48 kHz through DrumKit and reads it like the one-shots were read: a 5 ms RMS
// envelope on a 1 ms hop (times after the peak where it last stays above peak - 20/40/60 dB), the
// starting pitch from the first zero crossings, the octave cycle by cycle (the pitch still glides),
// and the spectrum of the first 150 ms. Tolerance on times: about 25 %.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace batterie_tests {

constexpr int kR = 48000;

std::vector<float> hit(int voice, int kind = 0, std::vector<std::pair<int, float>> params = {}) {
    ps::DrumKit k;
    k.prepare(kR);
    k.setVersion(3);
    if (kind) k.setKind(voice, kind);
    for (auto& p : params) k.setVoiceParam(voice, p.first, p.second);
    k.trigger(voice, 1.0f, 0);
    std::vector<float> x(static_cast<size_t>(kR * 2), 0.0f);
    for (int i = 0; i < static_cast<int>(x.size()); i += 256)
        k.render(x.data() + i, std::min(256, static_cast<int>(x.size()) - i));
    return x;
}

struct Env {
    std::vector<double> db;  // one value per ms
    size_t peak = 0;
    double after(double below) const {  // ms after the start where it last stays above peak - below
        size_t t = peak;
        for (size_t i = peak; i < db.size(); ++i)
            if (db[i] > db[peak] - below) t = i;
        return static_cast<double>(t);
    }
    double dip(size_t untilMs) const {  // the deepest fall that rises again before untilMs
        double worst = 0.0;
        for (size_t t = peak; t < untilMs && t < db.size(); ++t) {
            double later = -300.0;
            for (size_t u = t + 1; u <= untilMs && u < db.size(); ++u) later = std::max(later, db[u]);
            worst = std::max(worst, later - db[t]);
        }
        return worst;
    }
};

Env envelope(const std::vector<float>& x) {
    Env e;
    const int w = kR / 200, h = kR / 1000;
    for (size_t i = 0; i + w < x.size(); i += h) {
        double s = 0.0;
        for (int j = 0; j < w; ++j) s += static_cast<double>(x[i + j]) * x[i + j];
        e.db.push_back(10.0 * std::log10(s / w + 1e-30));
    }
    for (size_t i = 0; i < e.db.size(); ++i)
        if (e.db[i] > e.db[e.peak]) e.peak = i;
    return e;
}

std::vector<double> upCrossings(const std::vector<float>& x, size_t from, size_t to) {
    std::vector<double> z;
    for (size_t i = std::max<size_t>(from, 1); i < to && i < x.size(); ++i)
        if (x[i - 1] < 0.0f && x[i] >= 0.0f) z.push_back(static_cast<double>(i - 1) + (-x[i - 1]) / (x[i] - x[i - 1]));
    return z;
}

// The starting pitch: the first full cycle after 1 ms.
double startHz(const std::vector<float>& x) {
    const auto z = upCrossings(x, kR / 1000, kR / 2);
    return z.size() > 2 ? kR / (z[1] - z[0]) : 0.0;
}

// The octave against the fundamental, in dB, cycle by cycle between [fromMs, toMs).
double octaveDb(const std::vector<float>& x, int fromMs, int toMs) {
    const auto z = upCrossings(x, static_cast<size_t>(kR * fromMs / 1000), static_cast<size_t>(kR * toMs / 1000));
    double e1 = 0.0, e2 = 0.0;
    for (size_t c = 1; c < z.size(); ++c) {
        const size_t a = static_cast<size_t>(z[c - 1]), b = static_cast<size_t>(z[c]);
        if (b <= a + 4) continue;
        const double n = static_cast<double>(b - a);
        double r1 = 0, i1 = 0, r2 = 0, i2 = 0;
        for (size_t i = a; i < b; ++i) {
            const double ph = 2.0 * M_PI * static_cast<double>(i - a) / n;
            r1 += x[i] * std::cos(ph);
            i1 += x[i] * std::sin(ph);
            r2 += x[i] * std::cos(2.0 * ph);
            i2 += x[i] * std::sin(2.0 * ph);
        }
        e1 += r1 * r1 + i1 * i1;
        e2 += r2 * r2 + i2 * i2;
    }
    return 10.0 * std::log10((e2 + 1e-30) / (e1 + 1e-30));
}

double binPower(const std::vector<float>& x, size_t n, double hz) {
    double re = 0.0, im = 0.0;
    for (size_t i = 0; i < n; ++i) {
        const double w = 0.5 - 0.5 * std::cos(2.0 * M_PI * static_cast<double>(i) / static_cast<double>(n));
        const double ph = 2.0 * M_PI * hz * static_cast<double>(i) / kR;
        re += w * x[i] * std::cos(ph);
        im += w * x[i] * std::sin(ph);
    }
    return re * re + im * im;
}

struct Spectrum {
    double centroid = 0, above10kDb = 0, bodyDb = 0;
};

// The first 150 ms, 25 Hz bins. bodyDb: the 150..400 Hz density against the 400 Hz..10 kHz one.
Spectrum spectrum(const std::vector<float>& x) {
    const size_t n = static_cast<size_t>(kR * 15 / 100);
    double tot = 0, wsum = 0, hi = 0, body = 0, mid = 0;
    for (double f = 25.0; f < 20000.0; f += 25.0) {
        const double p = binPower(x, n, f);
        tot += p;
        wsum += p * f;
        if (f > 10000.0) hi += p;
        if (f >= 150.0 && f <= 400.0) body += p;
        if (f > 400.0 && f <= 10000.0) mid += p;
    }
    Spectrum s;
    s.centroid = wsum / tot;
    s.above10kDb = 10.0 * std::log10(hi / tot + 1e-30);
    s.bodyDb = 10.0 * std::log10((body / 11.0) / (mid / 384.0) + 1e-30);
    return s;
}

bool near(double v, double target, double tol = 0.25) { return v >= target * (1.0 - tol) && v <= target * (1.0 + tol); }

void report(const char* name, const Env& e) {
    std::printf("  %s: -20 %.0f ms, -40 %.0f ms, -60 %.0f ms\n", name, e.after(20), e.after(40), e.after(60));
}

// 1-3. The studio kick: gone by 230..300 ms (it rang to 430), no hollow dip, a sharper start.
void testKick() {
    const auto x = hit(ps::DrumKit::Kick);
    const auto e = envelope(x);
    report("kick", e);
    const double f0 = startHz(x);
    std::printf("  kick: dip %.1f dB, start %.0f Hz (%.2f x the tune)\n", e.dip(120), f0, f0 / 52.0);
    CHECK(e.after(60) >= 230.0 * 0.9 && e.after(60) <= 300.0 * 1.1);
    CHECK(e.dip(120) <= 2.0);
    CHECK(f0 / 52.0 >= 4.2 && f0 / 52.0 <= 5.6);  // the medians: 239 Hz over a 50 Hz note
    CHECK(ps::studio::StudioKick::kDropSt >= 27.5f && ps::studio::StudioKick::kDropSt <= 28.5f);
}

// 4. The hard kick: an octave at -30..-35 dB (even harmonic from a biased drive), decay 0.28.
void testHardKick() {
    ps::DrumKit k;
    k.prepare(kR);
    k.setVersion(3);
    CHECK(std::fabs(k.voiceParam(ps::DrumKit::Kick2, ps::DrumKit::Decay) - 0.28f) < 1e-4f);
    const auto x = hit(ps::DrumKit::Kick2);
    const double oct = octaveDb(x, 60, 160);
    report("hard kick", envelope(x));
    std::printf("  hard kick: octave %.1f dB\n", oct);
    CHECK(oct >= -37.0 && oct <= -28.0);
}

// 5. The 808 (voice808's new attack: 14 semitones over 40 ms, click 0.15): an octave near -34 dB.
void test808() {
    const auto x = hit(ps::DrumKit::Kick2, ps::DrumKit::Bass808,
                       {{ps::DrumKit::Tune, 36}, {ps::DrumKit::Decay, 1.3f}, {ps::DrumKit::Drive, 0.7f},
                        {ps::DrumKit::Click, 0.15f}, {ps::DrumKit::Tone, 0.5f}, {ps::DrumKit::Version, 1},
                        {ps::DrumKit::PitchDrop, 14}, {ps::DrumKit::DropTime, 0.04f}});
    const double oct = octaveDb(x, 100, 300);
    std::printf("  808: octave %.1f dB\n", oct);
    CHECK(oct >= -38.0 && oct <= -30.0);
}

// 6. The clap: -20 dB at ~67 ms, -40 at ~156 ms, above 10 kHz about -19 dB of its energy.
void testClap() {
    const auto x = hit(ps::DrumKit::Clap);
    const auto e = envelope(x);
    const auto s = spectrum(x);
    report("clap", e);
    std::printf("  clap: above 10 kHz %.1f dB\n", s.above10kDb);
    CHECK(near(e.after(20), 67.0));
    CHECK(near(e.after(40), 156.0));
    CHECK(s.above10kDb >= -23.0 && s.above10kDb <= -15.0);
}

// 7. The snare: -40 dB at ~200 ms (it rang to 784), its 150..400 Hz body about 5 dB up.
void testSnare() {
    const auto x = hit(ps::DrumKit::Snare);
    const auto e = envelope(x);
    const auto s = spectrum(x);
    report("snare", e);
    std::printf("  snare: body %.1f dB (was -9.7)\n", s.bodyDb);
    CHECK(near(e.after(40), 200.0));
    CHECK(s.bodyDb >= -9.7 + 4.0);
}

// 8. The closed hat: -20 dB at ~46 ms (it fell in 15), centroid ~7.6 kHz (it sat at 12.7).
void testClosedHat() {
    const auto x = hit(ps::DrumKit::ClosedHat);
    const auto e = envelope(x);
    const auto s = spectrum(x);
    report("closed hat", e);
    std::printf("  closed hat: centroid %.0f Hz\n", s.centroid);
    CHECK(near(e.after(20), 46.0));
    CHECK(s.centroid >= 6500.0 && s.centroid <= 8800.0);
}

// 9. The open hat: its drop at ~124 ms, its end at ~276 ms (it rang to 787).
void testOpenHat() {
    const auto e = envelope(hit(ps::DrumKit::OpenHat));
    report("open hat", e);
    CHECK(near(e.after(20), 124.0));
    CHECK(near(e.after(60), 276.0));
}

// 10. The toms: -40 dB at ~210 ms (they rang to about a second).
void testToms() {
    for (int v : {ps::DrumKit::LowTom, ps::DrumKit::MidTom, ps::DrumKit::HighTom}) {
        const auto e = envelope(hit(v));
        report("tom", e);
        CHECK(near(e.after(40), 210.0));
    }
}

}  // namespace batterie_tests

#define PS_BATTERIE_TESTS                                    \
    {"batterie_kick", batterie_tests::testKick},             \
    {"batterie_hard_kick", batterie_tests::testHardKick},    \
    {"batterie_808", batterie_tests::test808},               \
    {"batterie_clap", batterie_tests::testClap},             \
    {"batterie_snare", batterie_tests::testSnare},           \
    {"batterie_closed_hat", batterie_tests::testClosedHat},  \
    {"batterie_open_hat", batterie_tests::testOpenHat},      \
    {"batterie_toms", batterie_tests::testToms},
