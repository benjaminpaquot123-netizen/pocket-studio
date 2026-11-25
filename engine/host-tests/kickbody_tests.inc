// Tour 9, lot « kick-fable » (docs/decisions/tour-9-kick-fable.md): Benjamin, « le kick ne tape pas
// assez » (a hollow kick). The studio kick used to add a swept body and a sub at the tune in free
// phase: they beat and cancelled. Now one sine, saturated before its envelope, its pitch falling
// from 4.5 x the tune over 35 ms, a 60 ms full hold, no separate sub.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace kickbody_tests {

// One studio kick at the techno settings (DrumKit::KickStudio's defaults), tuned to `tune` Hz.
std::vector<float> renderKick(float tune, bool hard = false) {
    ps::studio::Settings s;
    s.tune = tune;
    s.decay = 0.25f;
    s.drive = hard ? 0.9f : 0.6f;
    s.click = hard ? 0.4f : 0.5f;
    s.sub = hard ? 0.3f : 0.5f;
    ps::studio::StudioKick k;
    k.hard = hard;
    k.prepare(kRate, s);
    k.hit(kRate, s, 1.0f);
    std::vector<float> x(static_cast<size_t>(kRate / 2));
    for (auto& v : x) v = k.process(kRate, s);
    return x;
}

// Hann-windowed single-bin DFT magnitude at `hz` over [from, to).
double binMag(const std::vector<float>& x, size_t from, size_t to, double hz) {
    double re = 0.0, im = 0.0;
    const double n = static_cast<double>(to - from);
    for (size_t i = from; i < to; ++i) {
        const double k = static_cast<double>(i - from);
        const double w = 0.5 - 0.5 * std::cos(2.0 * M_PI * k / n);
        const double ph = 2.0 * M_PI * hz * static_cast<double>(i) / kRate;
        re += w * x[i] * std::cos(ph);
        im += w * x[i] * std::sin(ph);
    }
    return std::sqrt(re * re + im * im);
}

double rms(const std::vector<float>& x, size_t from, size_t to) {
    double e = 0.0;
    for (size_t i = from; i < to; ++i) e += static_cast<double>(x[i]) * x[i];
    return std::sqrt(e / static_cast<double>(to - from));
}

const size_t kMs = static_cast<size_t>(kRate / 1000);

// The pitch at 70 ms: still well above the tune (the « boum » that is heard), 75..110 Hz for 55 Hz.
void testPitchAt70ms() {
    const auto x = renderKick(55.0f);
    // The period that holds 70 ms: the upward crossing just before it to the one just after.
    size_t a = 70 * kMs, b = 70 * kMs;
    while (a > 1 && !(x[a - 1] < 0.0f && x[a] >= 0.0f)) --a;
    while (b + 1 < x.size() && !(x[b] < 0.0f && x[b + 1] >= 0.0f)) ++b;
    ++b;
    const double hz = static_cast<double>(kRate) / static_cast<double>(b - a);
    std::printf("  kick pitch at 70 ms (tune 55): %.1f Hz\n", hz);
    CHECK(hz > 75.0 && hz < 110.0);
}

// The body: 40-150 Hz between 20 and 100 ms carries most of the low weight of the whole hit (a
// full, held body, not a blip that fades at once), and its level stays high (rms of the full mix).
void testBodyEnergy() {
    const auto x = renderKick(55.0f);
    // 40-150 Hz: a 2-pole low-pass at 150 Hz then a 2-pole high-pass at 40 Hz.
    ps::studio::Svf2 lp, hp;
    lp.set(150.0f, 0.707f, kRate);
    hp.set(40.0f, 0.707f, kRate);
    std::vector<float> y(x.size());
    for (size_t i = 0; i < x.size(); ++i) y[i] = hp.high(lp.low(x[i]));
    const double body = studio_tests::energy(y, 20 * kMs, 100 * kMs);
    const double whole = studio_tests::energy(y, 0, 400 * kMs);
    const double level = rms(y, 20 * kMs, 100 * kMs);
    std::printf("  kick body: 40-150 Hz in 20-100 ms = %.0f %% of the hit's, %.2f rms\n", 100.0 * body / whole, level);
    CHECK(body > 0.78 * whole);
    CHECK(level > 0.45);
}

// No hollow: the level (30 ms frames, every 5 ms: more than a period) never comes back up by more
// than 1.5 dB after 20 ms (a dip then a rise is two layers beating).
void testNoBeating() {
    for (float tune : {45.0f, 55.0f, 65.0f}) {
        const auto x = renderKick(tune);
        double worst = 0.0;
        double low = 1e9;
        for (size_t f = 20 * kMs; f + 30 * kMs <= 250 * kMs; f += 5 * kMs) {
            const double r = rms(x, f, f + 30 * kMs);
            if (r < low) low = r;
            worst = std::max(worst, 20.0 * std::log10(r / std::max(low, 1e-12)));
        }
        std::printf("  kick tuned %.0f Hz: worst rise after a dip %.2f dB\n", tune, worst);
        CHECK(worst < 1.5);
    }
}

// The saturation sits before the envelope: the tail keeps its 3rd harmonic 9 to 18 dB under the
// fundamental (heard on a phone), instead of a pure sine fading out.
void testThirdHarmonic() {
    const auto x = renderKick(55.0f);
    const size_t from = 150 * kMs, to = 350 * kMs;
    // The fundamental: the strongest 0.5 Hz bin between 35 and 90 Hz.
    double f0 = 35.0;
    for (double hz = 35.0; hz < 90.0; hz += 0.5) if (binMag(x, from, to, hz) > binMag(x, from, to, f0)) f0 = hz;
    const double h1 = binMag(x, from, to, f0), h3 = binMag(x, from, to, 3.0 * f0);
    const double db = 20.0 * std::log10(std::max(h3, 1e-12) / std::max(h1, 1e-12));
    std::printf("  kick tail: %.1f Hz, 3rd harmonic %+.1f dB\n", f0, db);
    CHECK(db > -18.0 && db < -9.0);
}

// The hard kick: one sine too (no beating), still the deeper « doof », finite and bounded.
void testHardKick() {
    const auto x = renderKick(55.0f, true);
    CHECK(studio_tests::allFinite(x));
    CHECK(studio_tests::peakOf(x) < 0.95f);
    double worst = 0.0, low = 1e9;
    for (size_t f = 20 * kMs; f + 30 * kMs <= 200 * kMs; f += 5 * kMs) {
        const double r = rms(x, f, f + 30 * kMs);
        if (r < low) low = r;
        worst = std::max(worst, 20.0 * std::log10(r / std::max(low, 1e-12)));
    }
    std::printf("  hard kick: worst rise after a dip %.2f dB\n", worst);
    CHECK(worst < 1.5);
}

// Per-voice settings (the styles' kick families, lot kick-fable b): through the kit, PitchDrop is the
// sweep's start over the note (semitones), DropTime its time constant, Drive the saturation, Decay
// the length. A slower sweep is higher at 70 ms; a cleaner drive has less 3rd harmonic.
std::vector<float> kitKick(float drop, float dropTime, float drive, float decay) {
    ps::DrumKit kit;
    kit.prepare(kRate);
    kit.setKind(0, ps::DrumKit::KickStudio);
    kit.setVoiceParam(0, ps::DrumKit::Tune, 49.0f);
    if (drop >= 0.0f) kit.setVoiceParam(0, ps::DrumKit::PitchDrop, drop);
    if (dropTime > 0.0f) kit.setVoiceParam(0, ps::DrumKit::DropTime, dropTime);
    if (drive >= 0.0f) kit.setVoiceParam(0, ps::DrumKit::Drive, drive);
    if (decay > 0.0f) kit.setVoiceParam(0, ps::DrumKit::Decay, decay);
    return studio_tests::renderMonoKit(kit, kRate, {{0, 0, 1.0f, 0.0f, 0}});
}

double pitchAt(const std::vector<float>& x, size_t at) {
    size_t a = at, b = at;
    while (a > 1 && !(x[a - 1] < 0.0f && x[a] >= 0.0f)) --a;
    while (b + 1 < x.size() && !(x[b] < 0.0f && x[b + 1] >= 0.0f)) ++b;
    ++b;
    return static_cast<double>(kRate) / static_cast<double>(b - a);
}

double third(const std::vector<float>& x) {
    const size_t from = 150 * kMs, to = 350 * kMs;
    double f0 = 35.0;
    for (double hz = 35.0; hz < 90.0; hz += 0.5) if (binMag(x, from, to, hz) > binMag(x, from, to, f0)) f0 = hz;
    return 20.0 * std::log10(std::max(binMag(x, from, to, 3.0 * f0), 1e-12) / std::max(binMag(x, from, to, f0), 1e-12));
}

void testVoiceSettings() {
    // Defaults (no setting sent): as before, 4.5 x over 35 ms.
    const auto d = kitKick(-1.0f, 0.0f, -1.0f, 0.0f);
    const auto slow = kitKick(28.0f, 0.045f, -1.0f, 0.0f);
    const auto fast = kitKick(24.0f, 0.025f, -1.0f, 0.0f);
    const double pd = pitchAt(d, 70 * kMs), ps = pitchAt(slow, 70 * kMs), pf = pitchAt(fast, 70 * kMs);
    std::printf("  kick at 70 ms: default %.1f Hz, x5/45 ms %.1f Hz, x4/25 ms %.1f Hz\n", pd, ps, pf);
    CHECK(ps > pd + 8.0);
    CHECK(pf < pd - 5.0);
    CHECK(pf > 49.0);
    // The saturation: clean (house, trance) against dirty (big room, hard).
    const double clean = third(kitKick(-1.0f, 0.0f, 0.1f, 0.0f)), dirty = third(kitKick(-1.0f, 0.0f, 1.0f, 0.0f));
    std::printf("  kick 3rd harmonic: drive 0.1 %+.1f dB, drive 1 %+.1f dB\n", clean, dirty);
    CHECK(clean < dirty - 6.0);
    // The length: a long gabber-like decay against a short lofi one.
    CHECK(studio_tests::lengthTo(kitKick(-1.0f, 0.0f, -1.0f, 0.6f), -40.0) > kRate * 45 / 100);
    CHECK(studio_tests::lengthTo(kitKick(-1.0f, 0.0f, -1.0f, 0.12f), -40.0) < kRate * 18 / 100);
}

}  // namespace kickbody_tests

#define PS_KICKBODY_TESTS                                         \
    {"kb_pitch_at_70ms", kickbody_tests::testPitchAt70ms},       \
    {"kb_body_energy", kickbody_tests::testBodyEnergy},          \
    {"kb_no_beating", kickbody_tests::testNoBeating},            \
    {"kb_third_harmonic", kickbody_tests::testThirdHarmonic},    \
    {"kb_hard_kick", kickbody_tests::testHardKick},              \
    {"kb_voice_settings", kickbody_tests::testVoiceSettings},
