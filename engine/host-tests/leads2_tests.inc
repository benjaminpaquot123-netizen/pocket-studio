// Leads chantier, lots B to D (docs/hits/leads-pros.md § 6 and § 8): the synth's touch and attack
// (key tracking, velocity on the envelope, a click, a delayed vibrato, the cycle's offset), its
// professional unison (the JP-8000 supersaw: Szabo's ratios, free phases, drift, centre/side mix,
// high-pass at f0), a clean ladder, a drive before the filter, the hoover's pulse sweep and a
// 2-operator FM pair. Every one of them at its default leaves the sound as it was (plain()).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace leads2_tests {

using wide_tests::centroid;
using wide_tests::correlation;
using wide_tests::powerSpectrum;

using P = ps::Synth::Param;

// A synth set from the library's defaults plus `set`, at 48 kHz.
struct Patch {
    std::vector<std::pair<P, float>> set;
};

// Renders `notes` (note, start frame, end frame, velocity) for `frames`, in blocks of 192 like the
// phone. Returns the left channel, or the mono sum of both when `mono` (a wide sound's centre).
std::vector<float> play(const Patch& patch, const std::vector<std::tuple<int, int, int, float>>& notes, int frames,
                        std::vector<float>* right = nullptr, ps::Synth* keep = nullptr) {
    ps::Synth local;
    ps::Synth& s = keep ? *keep : local;
    s.prepare(kRate);
    for (auto [p, v] : patch.set) s.setParam(p, v);
    std::vector<float> l(static_cast<size_t>(frames), 0.0f), r(static_cast<size_t>(frames), 0.0f);
    for (int f0 = 0; f0 < frames; f0 += 192) {
        const int n = std::min(192, frames - f0);
        for (auto& [note, on, off, vel] : notes) {
            if (on >= f0 && on < f0 + n) s.noteOn(note, vel, on - f0);
            if (off >= f0 && off < f0 + n) s.noteOff(note, off - f0);
        }
        s.render(l.data() + f0, r.data() + f0, n);
        if (!s.stereo()) std::copy(l.begin() + f0, l.begin() + f0 + n, r.begin() + f0);
    }
    if (right) *right = r;
    return l;
}

// Goertzel: the level of `hz` in x[from, to).
double toneAt(const std::vector<float>& x, size_t from, size_t to, double hz) {
    double re = 0, im = 0;
    for (size_t i = from; i < to; ++i) {
        re += x[i] * std::cos(2 * M_PI * hz * double(i) / kRate);
        im += x[i] * std::sin(2 * M_PI * hz * double(i) / kRate);
    }
    return 2.0 * std::sqrt(re * re + im * im) / double(to - from);
}

double hzOf(int note) { return 440.0 * std::pow(2.0, (note - 69) / 12.0); }

// The pitch (Hz) of x over [from, to) from its rising zero crossings (linearly interpolated).
double pitchOf(const std::vector<float>& x, size_t from, size_t to) {
    double first = -1.0, last = -1.0;
    int count = 0;
    for (size_t i = from + 1; i < to; ++i) {
        if (x[i - 1] < 0.0f && x[i] >= 0.0f) {
            const double t = double(i - 1) + double(-x[i - 1]) / double(x[i] - x[i - 1]);
            if (first < 0.0) first = t;
            else ++count;
            last = t;
        }
    }
    return count > 0 ? count * kRate / (last - first) : 0.0;
}

double cents(double hz, double ref) { return 1200.0 * std::log2(hz / ref); }

// Energy of x[from, to) above `hz` (two 2nd-order high-passes) in dB.
double highBandDb(const std::vector<float>& x, size_t from, size_t to, double hz) {
    const auto y = wide_tests::twice(x, hz, true);
    double e = 0.0;
    for (size_t i = from; i < to; ++i) e += double(y[i]) * y[i];
    return 10.0 * std::log10(std::max(e / double(to - from), 1e-30));
}

// The leads report's non-harmonic measure (§ 2): the energy outside +-3.5 % of the multiples of f0 / 2
// (20 Hz..20 kHz) against all of it, over 0.3..1.0 s of a held note (dB).
double nonHarmonicDb(const std::vector<float>& x, double f0) {
    constexpr size_t kN = 32768;
    const auto p = powerSpectrum(x, size_t(kRate * 3 / 10), kN);
    double bad = 0.0, all = 1e-30;
    for (size_t k = 1; k < p.size(); ++k) {
        const double f = double(k) * kRate / kN;
        if (f < 20.0 || f > 20000.0) continue;
        const double base = f0 / 2.0, h = std::round(f / base);
        const bool harmonic = h >= 1.0 && std::fabs(f - h * base) / f < 0.035;
        all += p[k];
        if (!harmonic) bad += p[k];
    }
    return 10.0 * std::log10(std::max(bad, 1e-30) / all);
}

// A pluck-like voice: a saw through the state-variable low-pass at 1.5 x C4's pitch.
Patch darkSaw() {
    return {{{P::Osc1Wave, 0.0f}, {P::OscMix, 0.0f}, {P::SubLevel, 0.0f}, {P::FilterType, 0.3f}, {P::Resonance, 0.2f},
             {P::FilterEnvAmount, 0.0f}, {P::Cutoff, float(std::log(392.0 / 20.0) / std::log(900.0))}, {P::AmpAttack, 0.0f},
             {P::AmpSustain, 1.0f}}};
}

Patch with(Patch p, std::initializer_list<std::pair<P, float>> more) {
    for (auto m : more) p.set.push_back(m);
    return p;
}

// B1. KeyTrack 1: the brightness over the pitch (centroid / f0) at C3, C5 and C7 within a factor 2;
// without it, C7 is several times darker than C3.
void testKeyTrack() {
    // The spread of centroid / f0 from C3 to C7, and C7's level against C3's (dB).
    auto spreadOf = [](const Patch& p, double* drop) {
        double lo = 1e9, hi = 0.0, e3 = 0.0, e7 = 0.0;
        for (int note : {48, 72, 96}) {
            const auto x = play(p, {{note, 0, kRate / 2, 0.8f}}, kRate / 2);
            const double r = centroid(x, 4800) / hzOf(note);
            lo = std::min(lo, r);
            hi = std::max(hi, r);
            double e = 0.0;
            for (size_t i = 4800; i < x.size(); ++i) e += double(x[i]) * x[i];
            if (note == 48) e3 = e;
            if (note == 96) e7 = e;
        }
        *drop = 10 * std::log10(e7 / e3);
        return hi / lo;
    };
    double dropOff = 0.0, dropOn = 0.0;
    const double off = spreadOf(darkSaw(), &dropOff), on = spreadOf(with(darkSaw(), {{P::KeyTrack, 1.0f}}), &dropOn);
    std::printf("  centroid / f0 from C3 to C7: x%.2f apart without key tracking, x%.2f with it; C7 %.1f dB under C3 without, %.1f with\n",
                off, on, -dropOff, -dropOn);
    CHECK(on <= 2.0);
    CHECK(dropOff < -12.0);  // without it, the filter at C4's 1.5 x f0 swallows C7
    CHECK(dropOn > -6.0);
}

// B2. The velocity opens the filter: its cutoff (VelocityCutoff) and its envelope (VelocityEnv) give a
// centroid ratio of at least 1.15 between velocity 1.0 and 0.4; without either, the level only.
void testVelocityOpensTheFilter() {
    auto ratio = [](const Patch& p, size_t from) {
        const auto soft = play(p, {{60, 0, kRate / 2, 0.4f}}, kRate / 2), hard = play(p, {{60, 0, kRate / 2, 1.0f}}, kRate / 2);
        return centroid(hard, from) / centroid(soft, from);
    };
    const Patch env = with(darkSaw(), {{P::FilterEnvAmount, 0.5f}, {P::FilterDecay, 0.6f}, {P::FilterSustain, 0.0f}});
    const double none = ratio(env, 480), cut = ratio(with(darkSaw(), {{P::VelocityCutoff, 0.5f}}), 4800),
                 envelope = ratio(with(env, {{P::VelocityEnv, 1.0f}}), 480);
    std::printf("  centroid at velocity 1.0 over 0.4: %.2f as before, %.2f by the cutoff, %.2f by the envelope\n", none, cut, envelope);
    CHECK(std::fabs(none - 1.0) < 0.02);
    CHECK(cut >= 1.15);
    CHECK(envelope >= 1.15);
}

// B3. The pitch envelope (+12 semitones falling in 10 ms): over its first ms, the note is over 1.5 x its pitch;
// after 50 ms, within 5 cents of it.
void testPitchEnvelopeStartsHigh() {
    const Patch p{{{P::Osc1Wave, 1.0f}, {P::OscMix, 0.0f}, {P::SubLevel, 0.0f}, {P::FilterType, 0.3f}, {P::Cutoff, 1.0f},
                   {P::FilterEnvAmount, 0.0f}, {P::AmpAttack, 0.0f}, {P::AmpSustain, 1.0f},
                   {P::PitchEnv, 12.0f / 48.0f}, {P::PitchDecay, float(std::log(10.0) / std::log(500.0))}}};
    const auto x = play(p, {{72, 0, kRate, 0.8f}}, kRate);
    const double early = pitchOf(x, kRate / 1000, kRate * 5 / 1000), late = pitchOf(x, kRate / 20, kRate / 5);
    std::printf("  pitch envelope: %.0f Hz over 1-5 ms, %.2f cents off after 50 ms (C5 %.1f Hz)\n", early, cents(late, hzOf(72)), hzOf(72));
    CHECK(early > 1.5 * hzOf(72));
    CHECK(std::fabs(cents(late, hzOf(72))) < 5.0);
}

// B4. The click: the energy above 2 kHz of its first 5 ms at least 12 dB over 50-100 ms; nothing of
// it without Click (a dark saw has little up there either way).
void testClickIsShort() {
    const auto clicked = play(with(darkSaw(), {{P::Click, 1.0f}}), {{60, 0, kRate / 2, 0.9f}}, kRate / 2);
    const auto plain = play(darkSaw(), {{60, 0, kRate / 2, 0.9f}}, kRate / 2);
    const double attack = highBandDb(clicked, 0, kRate / 200, 2000.0), body = highBandDb(clicked, kRate / 20, kRate / 10, 2000.0);
    const double before = highBandDb(plain, 0, kRate / 200, 2000.0);
    std::printf("  click: above 2 kHz, %.1f dB in its first 5 ms against 50-100 ms (%.1f dB without it)\n", attack - body,
                before - highBandDb(plain, kRate / 20, kRate / 10, 2000.0));
    CHECK(attack - body >= 12.0);
    CHECK(attack - before >= 12.0);
}

// B5. The delayed vibrato (+-10 cents at 5 Hz, 300 ms late): under 2 cents off for its first 150 ms,
// then swinging by more than 3 cents once it is in (+-5 at its peaks).
void testVibratoDelay() {
    const Patch p{{{P::Osc1Wave, 1.0f}, {P::OscMix, 0.0f}, {P::SubLevel, 0.0f}, {P::FilterType, 0.3f}, {P::Cutoff, 1.0f},
                   {P::FilterEnvAmount, 0.0f}, {P::AmpAttack, 0.0f}, {P::AmpSustain, 1.0f}, {P::Lfo1Rate, 0.77f},
                   {P::Lfo1ToPitch, 0.05f}, {P::VibratoDelay, 0.3f}}};
    const auto x = play(p, {{72, 0, 2 * kRate, 0.8f}}, 2 * kRate);
    auto swing = [&](size_t from, size_t to) {
        double worst = 0.0;
        for (size_t at = from; at + kRate / 50 <= to; at += kRate / 100) worst = std::max(worst, std::fabs(cents(pitchOf(x, at, at + kRate / 50), hzOf(72))));
        return worst;
    };
    const double early = swing(kRate / 50, kRate * 3 / 20), late = swing(kRate, 2 * kRate);
    std::printf("  delayed vibrato: %.2f cents at most over 0-150 ms, %.1f once in\n", early, late);
    CHECK(early < 2.0);
    CHECK(late > 3.0);  // the synth's LFO is +-0.5: +-5 cents here
}

// B6. The cycle's offset: half a cycle and the cycle starts open (its 8 bars' first one the
// brightest, not the darkest).
void testCycleOffset() {
    auto firstOverFifth = [](float offset) {
        ps::Synth s;
        const Patch p = with(darkSaw(), {{P::CycleBars, 0.6f}, {P::CycleDepth, 0.5f}, {P::CycleOffset, offset}});
        s.prepare(kRate);
        for (auto [k, v] : p.set) s.setParam(k, v);
        s.setBeatFrames(kRate / 2.0);  // 120 BPM: a bar is 2 s, the 4-bar cycle 8 s
        const int frames = 8 * kRate;
        std::vector<float> l(size_t(frames), 0.0f), r(size_t(frames), 0.0f);
        s.syncCycle(0, 0);
        s.noteOn(60, 0.8f, 0);
        for (int f = 0; f < frames; f += 192) s.render(l.data() + f, r.data() + f, std::min(192, frames - f));
        return centroid(l, kRate / 2) / centroid(l, size_t(4 * kRate + kRate / 2));
    };
    const double closed = firstOverFifth(0.0f), open = firstOverFifth(0.5f);
    std::printf("  cycle: bar 1 over bar 3 brightness %.2f from its start, %.2f half a cycle on\n", closed, open);
    CHECK(closed < 0.8);
    CHECK(open > 1.25);
}

// B7. Every new setting at its default keeps the synth on its old path (plain()), each one set takes it off.
void testDefaultsArePlain() {
    ps::Synth s;
    s.prepare(kRate);
    CHECK(s.plain());
    const std::pair<P, float> each[] = {{P::KeyTrack, 0.5f}, {P::VelocityEnv, 0.5f}, {P::Click, 0.5f}, {P::VibratoDelay, 0.5f},
                                        {P::Supersaw, 1.0f}, {P::CleanLadder, 1.0f}, {P::PreDrive, 0.5f}, {P::Pwm, 0.5f}, {P::Fm, 0.5f}};
    for (auto [p, v] : each) {
        ps::Synth t;
        t.prepare(kRate);
        t.setParam(p, v);
        CHECK(!t.plain());
    }
    CHECK(s.leadsAtRest());
    // Golden: a plain synth and a wide, oversampled one with copies render exactly what the code from
    // before the lots rendered (FNV-1a of the samples' bits, taken from the engine at 8d29296).
    uint64_t h = 1469598103934665603ull;
    for (int variant = 0; variant < 2; ++variant) {
        ps::Synth g;
        g.prepare(kRate);
        if (variant == 1) {
            g.setParam(P::Width, 0.6f);
            g.setParam(P::Oversample, 1.0f);
            g.setParam(P::Unison, 0.5f);
        }
        CHECK(g.leadsAtRest());
        std::vector<float> l(24000, 0.0f), r(24000, 0.0f);
        for (int f = 0; f < 24000; f += 192) {
            if (f == 0) {
                g.noteOn(48, 0.9f, 7);
                g.noteOn(55, 0.6f, 100);
            }
            if (f == 12096) g.noteOff(48, 3);
            g.render(l.data() + f, r.data() + f, 192);
        }
        for (int i = 0; i < 24000; ++i) {
            for (float x : {l[size_t(i)], r[size_t(i)]}) {
                uint32_t b;
                std::memcpy(&b, &x, 4);
                h = (h ^ b) * 1099511628211ull;
            }
        }
    }
    std::printf("  golden render of the synth at rest: %llu\n", static_cast<unsigned long long>(h));
#if !defined(__SANITIZE_ADDRESS__)
    CHECK(h == 10214537652659317471ull);
#endif
    // FmRatio and CycleOffset alone change nothing that plays (no FM, no cycle).
    ps::Synth t;
    t.prepare(kRate);
    t.setParam(P::FmRatio, 0.8f);
    t.setParam(P::CycleOffset, 0.5f);
    CHECK(t.plain());
}

// A JP supersaw lead: seven saws, Szabo's places, wide, open.
Patch supersaw(bool jp) {
    return {{{P::Osc1Wave, 0.0f}, {P::OscMix, 0.0f}, {P::SubLevel, 0.0f}, {P::Unison, 1.0f}, {P::UnisonDetune, 0.6f},
             {P::FilterType, 0.3f}, {P::Cutoff, 0.93f}, {P::FilterEnvAmount, 0.0f}, {P::AmpAttack, 0.0f}, {P::AmpSustain, 1.0f},
             {P::AmpRelease, 0.2f}, {P::Width, 0.8f}, {P::Supersaw, jp ? 1.0f : 0.0f}}};
}

// C1. Free phases: two attacks of the same note correlate under 0.5 over their first 200 ms (0.94
// before), and two renders of the same notes are the same, sample for sample (an export).
void testUnisonPhasesDifferEachNote() {
    std::vector<std::tuple<int, int, int, float>> notes;
    for (int k = 0; k < 2; ++k) notes.push_back({60, k * kRate, k * kRate + kRate / 2, 0.8f});
    auto corr = [&](bool jp) {
        const auto x = play(supersaw(jp), notes, 2 * kRate);
        double ab = 0, aa = 0, bb = 0;
        for (int i = 0; i < kRate / 5; ++i) {
            const double a = x[size_t(i)], b = x[size_t(kRate + i)];
            ab += a * b;
            aa += a * a;
            bb += b * b;
        }
        return ab / std::sqrt(aa * bb);
    };
    const double before = corr(false), after = corr(true);
    std::printf("  unison: two attacks correlate %.2f linear, %.2f as a JP-8000\n", before, after);
    CHECK(before > 0.9);
    CHECK(std::fabs(after) <= 0.5);
    CHECK(play(supersaw(true), notes, 2 * kRate) == play(supersaw(true), notes, 2 * kRate));
    // The transport's restart draws the same phases again: the export of a song twice is the same.
    ps::Synth s;
    const auto first = play(supersaw(true), notes, 2 * kRate, nullptr, &s);
    s.restartLfos();
    std::vector<float> l(size_t(2 * kRate), 0.0f), r(size_t(2 * kRate), 0.0f);
    for (int f = 0; f < 2 * kRate; f += 192) {
        const int n = std::min(192, 2 * kRate - f);
        for (auto& [note, on, off, vel] : notes) {
            if (on >= f && on < f + n) s.noteOn(note, vel, on - f);
            if (off >= f && off < f + n) s.noteOff(note, off - f);
        }
        s.render(l.data() + f, r.data() + f, n);
    }
    // (Same phases, same drift; the filters' and envelopes' state after the first pass is silence again.)
    CHECK(correlation(first, l, 0, size_t(kRate / 5)) > 0.999);
}

// C2. Szabo's places: the copies' detunes in cents for the outer one at 30 cents, centred.
void testUnisonJpRatios() {
    // Held A6 (1760 Hz) at the outer pair's +-30 cents: the 7 peaks where the ratios put them (+-2 cents,
    // the drift moving them by a cent or two over a second), the centre among them.
    const float detune = (30.0f - 3.0f) / 47.0f;
    Patch p = supersaw(true);
    p.set.push_back({P::UnisonDetune, detune});
    p.set.push_back({P::Width, 0.0f});
    const auto x = play(p, {{93, 0, 2 * kRate, 0.8f}}, 2 * kRate);
    constexpr size_t kN = 65536;
    const auto spec = powerSpectrum(x, size_t(kRate / 4), kN);
    const double binHz = double(kRate) / kN, f0 = hzOf(93);
    float mean = 0.0f;
    for (float o : ps::Synth::kJpOffsets) mean += o * 30.0f;
    mean /= 7.0f;
    std::vector<double> want = {-mean};
    for (float o : ps::Synth::kJpOffsets) want.push_back(o * 30.0f - mean);
    int found = 0;
    for (double c : want) {
        const double f = f0 * std::pow(2.0, c / 1200.0);
        // The loudest bin within 2 cents of the place, against the floor 5 to 9 cents away from every place.
        double peak = 0.0;
        for (size_t k = size_t(f * std::pow(2.0, -2.0 / 1200) / binHz); k <= size_t(f * std::pow(2.0, 2.0 / 1200) / binHz) + 1; ++k) peak = std::max(peak, spec[k]);
        double total = 0.0;
        for (size_t k = size_t(f0 * 0.97 / binHz); k <= size_t(f0 * 1.03 / binHz); ++k) total = std::max(total, spec[k]);
        if (peak > total * 1e-2) ++found;
    }
    std::printf("  JP places: %d of 7 peaks within 2 cents of Szabo's ratios\n", found);
    CHECK(found == 7);
}

// C3. No periodic beat: the envelope of a held note (20 ms RMS) correlates with itself under 0.5 at
// every lag from 0.3 to 5 s as a JP-8000 (the linear spread beats at a fixed rate).
void testUnisonNoPeriodicBeat() {
    auto worstLag = [](bool jp) {
        Patch p = supersaw(jp);
        p.set.push_back({P::Width, 0.0f});
        const auto x = play(p, {{60, 0, 9 * kRate, 0.8f}}, 9 * kRate);
        const size_t w = kRate / 50;
        std::vector<double> env;
        for (size_t at = kRate / 2; at + w <= x.size(); at += w) {
            double e = 0.0;
            for (size_t i = at; i < at + w; ++i) e += double(x[i]) * x[i];
            env.push_back(std::sqrt(e / double(w)));
        }
        double mean = 0.0;
        for (double v : env) mean += v;
        mean /= double(env.size());
        for (double& v : env) v -= mean;
        double best = -1.0;
        for (size_t lag = 15; lag <= 250 && lag < env.size() / 2; ++lag) {
            double ab = 0, aa = 0, bb = 0;
            for (size_t i = 0; i + lag < env.size(); ++i) {
                ab += env[i] * env[i + lag];
                aa += env[i] * env[i];
                bb += env[i + lag] * env[i + lag];
            }
            best = std::max(best, ab / std::sqrt(aa * bb + 1e-30));
        }
        return best;
    };
    const double before = worstLag(false), after = worstLag(true);
    std::printf("  beat: envelope self-correlation at 0.3-5 s, %.2f linear, %.2f as a JP-8000\n", before, after);
    CHECK(after <= 0.5);
    CHECK(before > after);
}

// C4. A dense stack through the ladder: seven saws open at C6, the old ladder's inner saturation
// leaves -35 dB of intermodulation; the clean one under -60 dB.
void testLadderDenseStackClean() {
    auto dirt = [](bool clean) {
        Patch p = supersaw(false);
        p.set.push_back({P::FilterType, 0.0f});
        p.set.push_back({P::Oversample, 1.0f});
        p.set.push_back({P::Width, 0.0f});
        p.set.push_back({P::CleanLadder, clean ? 1.0f : 0.0f});
        return nonHarmonicDb(play(p, {{84, 0, kRate * 3 / 2, 0.8f}}, kRate * 3 / 2), hzOf(84));
    };
    const double before = dirt(false), after = dirt(true);
    std::printf("  seven saws through the ladder at C6: %.1f dB non-harmonic before, %.1f clean\n", before, after);
    CHECK(after <= -60.0);
    CHECK(before > -50.0);
}

// C5. The drift stays within +-5 cents over a minute, and moves (not a fixed detune).
void testDriftBounded() {
    ps::Synth s;
    Patch p = supersaw(true);
    p.set.push_back({P::Width, 0.0f});
    s.prepare(kRate);
    for (auto [k, v] : p.set) s.setParam(k, v);
    s.noteOn(60, 0.8f, 0);
    std::vector<float> l(192), r(192);
    float worst = 0.0f, lo = 1e9f, hi = -1e9f;
    for (int f = 0; f < 60 * kRate; f += 192) {
        std::fill(l.begin(), l.end(), 0.0f);
        s.render(l.data(), r.data(), 192);
        for (int k = 0; k < 7; ++k) worst = std::max(worst, std::fabs(s.driftOf(0, k)));
        lo = std::min(lo, s.driftOf(0, 3));
        hi = std::max(hi, s.driftOf(0, 3));
    }
    std::printf("  drift over 60 s: at most %.2f cents, one copy over %.2f cents\n", double(worst), double(hi - lo));
    CHECK(worst <= 5.0f);
    CHECK(hi - lo > 1.0f);
}

// D1. The pulse sweep: the width moves (the spectrum changes from one second to the next), no DC.
void testPwm() {
    const Patch p{{{P::Osc1Wave, 0.5f}, {P::OscMix, 0.0f}, {P::SubLevel, 0.0f}, {P::FilterType, 0.3f}, {P::Cutoff, 1.0f},
                   {P::FilterEnvAmount, 0.0f}, {P::AmpAttack, 0.0f}, {P::AmpSustain, 1.0f}, {P::Pwm, 1.0f}}};
    const auto x = play(p, {{48, 0, 3 * kRate, 0.8f}}, 3 * kRate);
    const auto still = play(with(p, {{P::Pwm, 0.0f}}), {{48, 0, 3 * kRate, 0.8f}}, 3 * kRate);
    double dc = 0.0, peak = 0.0;
    for (size_t i = size_t(kRate / 2); i < x.size(); ++i) {
        dc += x[i];
        peak = std::max(peak, double(std::fabs(x[i])));
    }
    dc /= double(x.size() - size_t(kRate / 2));
    // A square has no 2nd harmonic; a pulse away from 50 % does.
    const double h2 = toneAt(x, size_t(kRate * 3 / 4), size_t(kRate * 3 / 4 + 4800), 2 * hzOf(48));
    const double h2still = toneAt(still, size_t(kRate * 3 / 4), size_t(kRate * 3 / 4 + 4800), 2 * hzOf(48));
    std::printf("  pulse sweep: DC %.4f of a %.2f peak, 2nd harmonic %.4f (%.4f at 50 %%)\n", dc, peak, h2, h2still);
    CHECK(std::fabs(dc) < 0.02 * peak);
    CHECK(h2 > 10.0 * h2still + 1e-4);
}

// D2. The FM pair: non-harmonic partials at a ratio of 1.41 (a metallic stab), its index falling
// with the filter envelope (darker after 300 ms than at the attack); at a ratio of 1, harmonic.
void testFmPair() {
    const Patch p{{{P::Osc1Wave, 0.0f}, {P::OscMix, 0.0f}, {P::SubLevel, 0.0f}, {P::FilterType, 0.3f}, {P::Cutoff, 1.0f},
                   {P::FilterEnvAmount, 0.0f}, {P::FilterDecay, 0.55f}, {P::FilterSustain, 0.0f}, {P::AmpAttack, 0.0f},
                   {P::AmpSustain, 1.0f}, {P::Fm, 1.0f}, {P::FmRatio, 2.0f / 8.0f}}};
    const auto x = play(p, {{60, 0, kRate, 0.8f}}, kRate);
    const double early = centroid(x, 480), late = centroid(x, kRate / 2);
    const double metallic = nonHarmonicDb(play(with(p, {{P::FilterDecay, 1.0f}, {P::FilterSustain, 1.0f}}), {{60, 0, kRate * 3 / 2, 0.8f}}, kRate * 3 / 2), hzOf(60));
    const double harmonic = nonHarmonicDb(play(with(p, {{P::FilterDecay, 1.0f}, {P::FilterSustain, 1.0f}, {P::FmRatio, 1.0f / 8.0f}}), {{60, 0, kRate * 3 / 2, 0.8f}}, kRate * 3 / 2), hzOf(60));
    std::printf("  FM pair: centroid %.0f Hz at the attack, %.0f Hz at 0.5 s; non-harmonic %.1f dB at 1.41, %.1f at 1\n", early, late, metallic, harmonic);
    CHECK(early > 1.3 * late);
    CHECK(metallic > -20.0);
    CHECK(harmonic < -60.0);
}

// D3. The drive before the filter: a triangle's 3rd harmonic 4 dB louder at least against its fundamental,
// the level kept within 3 dB.
void testPreDrive() {
    const Patch p{{{P::Osc1Wave, 1.0f}, {P::OscMix, 0.0f}, {P::SubLevel, 0.0f}, {P::FilterType, 0.3f}, {P::Cutoff, 0.8f},
                   {P::FilterEnvAmount, 0.0f}, {P::AmpAttack, 0.0f}, {P::AmpSustain, 1.0f}}};
    const auto dry = play(p, {{48, 0, kRate, 0.8f}}, kRate), driven = play(with(p, {{P::PreDrive, 0.8f}}), {{48, 0, kRate, 0.8f}}, kRate);
    auto rms = [](const std::vector<float>& x) {
        double e = 0.0;
        for (size_t i = size_t(kRate / 4); i < x.size(); ++i) e += double(x[i]) * x[i];
        return std::sqrt(e / double(x.size() - size_t(kRate / 4)));
    };
    auto third = [](const std::vector<float>& x) {
        const size_t a = size_t(kRate / 2), b = a + size_t(kRate / 4);
        return 20 * std::log10(toneAt(x, a, b, 3 * hzOf(48)) / toneAt(x, a, b, hzOf(48)));
    };
    const double level = 20 * std::log10(rms(driven) / rms(dry));
    std::printf("  drive before the filter: 3rd harmonic %.1f -> %.1f dB under the fundamental, level %+.1f dB\n", -third(dry), -third(driven), level);
    CHECK(third(driven) - third(dry) >= 4.0);
    CHECK(std::fabs(level) < 3.0);
}

// Everything on at once: no allocation in render, finite, bounded.
void testNoAllocation() {
    Patch p = supersaw(true);
    for (auto m : std::initializer_list<std::pair<P, float>>{{P::KeyTrack, 0.5f}, {P::VelocityEnv, 0.5f}, {P::Click, 0.6f}, {P::VibratoDelay, 0.3f},
                                                            {P::Lfo1ToPitch, 0.05f}, {P::CleanLadder, 1.0f}, {P::FilterType, 0.0f}, {P::PreDrive, 0.4f},
                                                            {P::Pwm, 0.5f}, {P::Fm, 0.3f}, {P::Oversample, 1.0f}, {P::CycleBars, 0.8f},
                                                            {P::CycleDepth, 0.3f}, {P::CycleOffset, 0.5f}})
        p.set.push_back(m);
    ps::Synth s;
    s.prepare(kRate);
    for (auto [k, v] : p.set) s.setParam(k, v);
    std::vector<float> l(192), r(192);
    float peak = 0.0f;
    bool finite = true;
#if PS_COUNT_ALLOCATIONS
    g_allocations = 0;
    g_countAllocations = true;
#endif
    for (int f = 0; f < 4 * kRate; f += 192) {
        if (f % (kRate / 4) == 0) for (int k = 0; k < 4; ++k) s.noteOn(48 + 7 * k + (f / kRate) * 12, 0.5f + 0.1f * k, 0);
        if (f % (kRate / 4) == kRate / 8) for (int k = 0; k < 4; ++k) s.noteOff(48 + 7 * k + (f / kRate) * 12, 0);
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        s.render(l.data(), r.data(), 192);
        for (int i = 0; i < 192; ++i) {
            finite = finite && std::isfinite(l[size_t(i)]) && std::isfinite(r[size_t(i)]);
            peak = std::max(peak, std::max(std::fabs(l[size_t(i)]), std::fabs(r[size_t(i)])));
        }
    }
#if PS_COUNT_ALLOCATIONS
    g_countAllocations = false;
    std::printf("  lots B-D all on: %d allocations, peak %.2f\n", g_allocations.load(), double(peak));
    CHECK(g_allocations.load() == 0);
#endif
    CHECK(finite);
    CHECK(peak > 0.01f && peak < 4.0f);
}

// C6. The lead's chorus (0.3, 0.4, 0.4, 0.4) on a wide input: with its stereo input (param 4) the side
// kept within 1 dB; summed to mono first (as before), it loses more than 3 dB of it.
void testChorusKeepsInputSide() {
    auto sideDb = [](float stereo) {
        ps::InsertSlot slot;
        slot.prepare(static_cast<float>(kRate));
        slot.setType(ps::InsertSlot::Chorus);
        const float params[] = {0.3f, 0.4f, 0.4f, 0.4f, stereo};
        for (int k = 0; k < 5; ++k) slot.setParam(k, params[k]);
        std::mt19937 rng(11);
        std::uniform_real_distribution<float> u(-0.3f, 0.3f);
        const int frames = 2 * kRate;
        std::vector<float> l(static_cast<size_t>(frames)), r(static_cast<size_t>(frames));
        for (int i = 0; i < frames; ++i) {
            const float m = u(rng), sd = u(rng);
            l[size_t(i)] = m + 0.7f * sd;
            r[size_t(i)] = m - 0.7f * sd;
        }
        double in = 0.0, out = 0.0;
        for (int i = kRate / 2; i < frames; ++i) in += std::pow(0.5 * (l[size_t(i)] - r[size_t(i)]), 2);
        for (int f = 0; f < frames; f += 192) slot.process(l.data() + f, r.data() + f, std::min(192, frames - f));
        for (int i = kRate / 2; i < frames; ++i) out += std::pow(0.5 * (l[size_t(i)] - r[size_t(i)]), 2);
        return 10 * std::log10(out / in);
    };
    const double mono = sideDb(0.0f), stereo = sideDb(1.0f);
    std::printf("  lead chorus on a wide input: side %+.1f dB summed to mono, %+.1f dB with its stereo input\n", mono, stereo);
    CHECK(std::fabs(stereo) <= 1.0);
    CHECK(mono < -3.0);
}

// REVIEW S7: the stereo-input chorus restarts with the transport (InsertSlot::restartLfo): after it,
// two choruses that ran for different times play the same input alike, sample for sample (live =
// export). The mono-input one (every chorus saved before) runs on: its exports stay the same.
void testChorusRestartsWithTheTransport() {
    std::vector<float> x(static_cast<size_t>(kRate));
    for (size_t i = 0; i < x.size(); ++i) x[i] = 0.3f * static_cast<float>(std::sin(2 * M_PI * 330.0 * double(i) / kRate));
    auto run = [&](ps::InsertSlot& slot) {
        std::vector<float> l = x, r = x;
        for (size_t f = 0; f < l.size(); f += 192) slot.process(l.data() + f, r.data() + f, int(std::min<size_t>(192, l.size() - f)));
        return l;
    };
    ps::InsertSlot played, other;
    for (auto* s : {&played, &other}) {
        s->prepare(static_cast<float>(kRate));
        s->setType(ps::InsertSlot::Chorus);
        s->setParam(0, 0.6f);
        s->setParam(4, 1.0f);  // the stereo input: the assistant's choruses since the leads chantier
    }
    run(played);  // a second of the chorus running, its LFO somewhere (heading down, say)
    std::vector<float> half(x.begin(), x.begin() + kRate / 3), hr = half;
    for (size_t f = 0; f < half.size(); f += 192) other.process(half.data() + f, hr.data() + f, int(std::min<size_t>(192, half.size() - f)));
    played.restartLfo();
    other.restartLfo();
    CHECK(run(played) == run(other));
    ps::InsertSlot a, b;
    for (auto* s : {&a, &b}) {
        s->prepare(static_cast<float>(kRate));
        s->setType(ps::InsertSlot::Chorus);
        s->setParam(0, 0.6f);
    }
    run(a);
    a.restartLfo();
    b.restartLfo();
    CHECK(run(a) != run(b));  // the mono one: its LFO where it was
    // A chorus put in a slot after a stereo one is mono-input again (a project saved before sends 4 params).
    ps::InsertSlot slot;
    slot.prepare(static_cast<float>(kRate));
    slot.setType(ps::InsertSlot::Chorus);
    slot.setParam(4, 1.0f);
    slot.setType(ps::InsertSlot::Filter);
    slot.setType(ps::InsertSlot::Chorus);
    ps::InsertSlot plain;
    plain.prepare(static_cast<float>(kRate));
    plain.setType(ps::InsertSlot::Chorus);
    CHECK(run(slot) == run(plain));
}

// REVIEW S7: the widener's « Étalement » turned glides over a block: no step in the side when it goes
// from 1 to 0 (it fades out, then rests at exactly the plain widener), nor from 0.2 to 1.
void testWidenerSpreadGlides() {
    ps::InsertSlot slot;
    slot.prepare(static_cast<float>(kRate));
    slot.setType(ps::InsertSlot::WidenerFx);
    slot.setParam(0, 0.5f);
    slot.setParam(2, 1.0f);
    const int block = 512;
    std::vector<float> x(static_cast<size_t>(kRate));
    for (size_t i = 0; i < x.size(); ++i) x[i] = 0.3f * static_cast<float>(std::sin(2 * M_PI * 700.0 * double(i) / kRate));
    std::vector<float> l = x, r = x;
    double worstStep = 0.0, worstBefore = 0.0;
    float last = 0.0f;
    for (int f = 0; f < kRate; f += block) {
        if (f == 24 * block) slot.setParam(2, 0.0f);
        if (f == 48 * block) slot.setParam(2, 0.2f);
        if (f == 72 * block) slot.setParam(2, 1.0f);
        slot.process(l.data() + f, r.data() + f, std::min(block, kRate - f));
        for (int i = f; i < std::min(kRate, f + block); ++i) {
            const float sd = 0.5f * (l[size_t(i)] - r[size_t(i)]);
            const double step = std::fabs(double(sd) - last);
            if (i > block && (i % block == 0)) worstStep = std::max(worstStep, step);
            else if (i > 4 * block) worstBefore = std::max(worstBefore, step);
            last = sd;
        }
    }
    // The side right after the switch to 0 rests at none (a mid-only input).
    double restSide = 0.0;
    for (int i = 25 * block; i < 48 * block; ++i) restSide = std::max(restSide, std::fabs(0.5 * double(l[size_t(i)] - r[size_t(i)])));
    std::printf("  spread turned: largest side step at a block's edge %.4f, elsewhere %.4f; at rest %.6f\n", worstStep, worstBefore, restSide);
    CHECK(worstStep <= 1.5 * worstBefore);
    CHECK(restSide == 0.0);
}

// REVIEW S7: in mix version 4 (the fader before the chain) the metronome's click is turned down by the
// master's fader, as it was in version 2 (after the chain).
void testClickUnderTheFader() {
    using ps::CommandType;
    auto clickPeak = [](float version, float fader) {
        ps::Engine e(kRate);
        e.post({CommandType::SetMixVersion, version});
        e.post({CommandType::SetTempo, 120.0f});
        e.post({CommandType::SetMetronome, 1.0f});
        e.post({CommandType::SetChannelVolume, fader, ps::Mixer::kMaster});
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        const auto out = render(e, kRate * 2, 256);
        float p = 0.0f;
        for (size_t i = size_t(kRate) * 2 / 5 * 2; i < out.size(); ++i) p = std::max(p, std::fabs(out[i]));  // past the fader's glide
        return 20.0 * std::log10(std::max(1e-9f, p));
    };
    const double full2 = clickPeak(2.0f, 0.8f), low2 = clickPeak(2.0f, 0.4f), full4 = clickPeak(4.0f, 0.8f), low4 = clickPeak(4.0f, 0.4f);
    std::printf("  click with the fader down: %.1f dB in version 2, %.1f dB in version 4\n", low2 - full2, low4 - full4);
    CHECK(low2 - full2 < -3.0);
    CHECK(std::fabs((low4 - full4) - (low2 - full2)) < 1.0);
}

}  // namespace leads2_tests

#define PS_LEADS2_TESTS                                                              \
    {"keytrack_keeps_brightness_across_keys", leads2_tests::testKeyTrack},           \
    {"velocity_opens_the_filter", leads2_tests::testVelocityOpensTheFilter},        \
    {"pitch_env_starts_high", leads2_tests::testPitchEnvelopeStartsHigh},            \
    {"click_layer_is_short", leads2_tests::testClickIsShort},                        \
    {"vibrato_delay", leads2_tests::testVibratoDelay},                               \
    {"cycle_offset", leads2_tests::testCycleOffset},                                 \
    {"leads_new_params_plain", leads2_tests::testDefaultsArePlain},                  \
    {"unison_phases_differ_each_note", leads2_tests::testUnisonPhasesDifferEachNote}, \
    {"unison_jp_ratios", leads2_tests::testUnisonJpRatios},                          \
    {"unison_no_periodic_beat", leads2_tests::testUnisonNoPeriodicBeat},             \
    {"ladder_dense_stack_clean", leads2_tests::testLadderDenseStackClean},           \
    {"drift_bounded", leads2_tests::testDriftBounded},                               \
    {"pwm_sweeps_without_dc", leads2_tests::testPwm},                                \
    {"fm_pair", leads2_tests::testFmPair},                                           \
    {"pre_drive", leads2_tests::testPreDrive},                                       \
    {"leads_lots_bcd_no_allocation", leads2_tests::testNoAllocation},                \
    {"chorus_keeps_input_side", leads2_tests::testChorusKeepsInputSide},             \
    {"chorus_restarts_with_transport", leads2_tests::testChorusRestartsWithTheTransport}, \
    {"widener_spread_glides", leads2_tests::testWidenerSpreadGlides},                \
    {"metronome_under_the_fader", leads2_tests::testClickUnderTheFader},
