// Phase 34, S4 (docs/PLAN-SON-PRO.md): wide and moving synths. The synth's stereo unison, its
// octave down, drive, formant, mid layer and oversampling; the slow filter cycle and the square
// sweep; synth and Plaits knobs on automation lanes; stereo instruments and samples (sound
// version 2); the plate and the room, the returns' equaliser and the mix's lows in mono.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace wide_tests {

using studio_tests::bandEnergy;
using studio_tests::energy;

// In-place radix-2 FFT (power of two).
void fft(std::vector<std::complex<double>>& a) {
    const size_t n = a.size();
    for (size_t i = 1, j = 0; i < n; ++i) {
        size_t bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) std::swap(a[i], a[j]);
    }
    for (size_t len = 2; len <= n; len <<= 1) {
        const double ang = -2.0 * M_PI / static_cast<double>(len);
        const std::complex<double> wl(std::cos(ang), std::sin(ang));
        for (size_t i = 0; i < n; i += len) {
            std::complex<double> w(1.0);
            for (size_t j = 0; j < len / 2; ++j) {
                const auto u = a[i + j], v = a[i + j + len / 2] * w;
                a[i + j] = u + v;
                a[i + j + len / 2] = u - v;
                w *= wl;
            }
        }
    }
}

// Power spectrum of x[from, from + n) (n a power of two) through a 4-term Blackman-Harris window
// (sidelobes under -92 dB: a loud harmonic does not hide the aliasing next to it).
std::vector<double> powerSpectrum(const std::vector<float>& x, size_t from, size_t n) {
    std::vector<std::complex<double>> a(n);
    for (size_t i = 0; i < n; ++i) {
        const double t = 2.0 * M_PI * static_cast<double>(i) / static_cast<double>(n);
        const double w = 0.35875 - 0.48829 * std::cos(t) + 0.14128 * std::cos(2 * t) - 0.01168 * std::cos(3 * t);
        a[i] = w * static_cast<double>(x[from + i]);
    }
    fft(a);
    std::vector<double> p(n / 2);
    for (size_t k = 0; k < n / 2; ++k) p[k] = std::norm(a[k]);
    return p;
}

// The loudest component (20 Hz..20 kHz) that is not a harmonic of f0, in dB under the fundamental.
double worstAliasDb(const std::vector<float>& x, size_t from, double f0) {
    constexpr size_t kN = 65536;
    const auto p = powerSpectrum(x, from, kN);
    const double binHz = static_cast<double>(kRate) / kN;
    double fund = 0.0, worst = 1e-30;
    for (size_t k = 1; k < p.size(); ++k) {
        const double f = static_cast<double>(k) * binHz;
        const double h = f / f0;
        const double off = std::fabs(h - std::round(h)) * f0 / binHz;  // bins from the nearest harmonic
        if (std::round(h) == 1.0 && off <= 10.0) fund = std::max(fund, p[k]);
        if (off <= 10.0 && std::round(h) >= 1.0) continue;
        if (f > 20.0 && f < 20000.0) worst = std::max(worst, p[k]);
    }
    return 10.0 * std::log10(worst / std::max(fund, 1e-30));
}

// Spectral centroid of x[from, from + 4096) (Hz).
double centroid(const std::vector<float>& x, size_t from) {
    constexpr size_t kN = 4096;
    const auto p = powerSpectrum(x, from, kN);
    double num = 0.0, den = 0.0;
    for (size_t k = 1; k < p.size(); ++k) {
        const double f = static_cast<double>(k) * kRate / kN;
        num += f * p[k];
        den += p[k];
    }
    return den > 0.0 ? num / den : 0.0;
}

// Correlation of two signals over [from, to).
double correlation(const std::vector<float>& a, const std::vector<float>& b, size_t from, size_t to) {
    double ab = 0.0, aa = 0.0, bb = 0.0;
    for (size_t i = from; i < to; ++i) {
        ab += static_cast<double>(a[i]) * b[i];
        aa += static_cast<double>(a[i]) * a[i];
        bb += static_cast<double>(b[i]) * b[i];
    }
    return aa > 0.0 && bb > 0.0 ? ab / std::sqrt(aa * bb) : 1.0;
}

// A 2nd-order low-pass / high-pass of a whole signal (RBJ, Q 0.707), for band measurements.
std::vector<float> filtered(const std::vector<float>& x, double hz, bool high) {
    const double w = 2.0 * M_PI * hz / kRate, cs = std::cos(w), alpha = std::sin(w) / (2.0 * 0.7071);
    const double b0 = high ? (1 + cs) / 2 : (1 - cs) / 2, b1 = high ? -(1 + cs) : 1 - cs, b2 = b0;
    const double a0 = 1 + alpha, a1 = -2 * cs, a2 = 1 - alpha;
    std::vector<float> y(x.size());
    double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
    for (size_t i = 0; i < x.size(); ++i) {
        const double v = (b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0;
        x2 = x1;
        x1 = x[i];
        y2 = y1;
        y1 = v;
        y[i] = static_cast<float>(v);
    }
    return y;
}

std::vector<float> twice(const std::vector<float>& x, double hz, bool high) { return filtered(filtered(x, hz, high), hz, high); }

struct Stereo {
    std::vector<float> l, r;
};

// Renders `frames` of a synth (both channels; a mono synth leaves `r` silent).
Stereo renderWide(ps::Synth& s, int frames, int block = 192) {
    Stereo o{std::vector<float>(static_cast<size_t>(frames), 0.0f), std::vector<float>(static_cast<size_t>(frames), 0.0f)};
    for (int pos = 0; pos < frames; pos += block) {
        const int n = std::min(block, frames - pos);
        s.render(o.l.data() + pos, o.r.data() + pos, n);
    }
    return o;
}

std::unique_ptr<ps::Synth> makeSynth() {
    auto s = std::make_unique<ps::Synth>();
    s->prepare(kRate);
    s->setParam(ps::Synth::AmpAttack, 0.0f);
    s->setParam(ps::Synth::AmpSustain, 1.0f);
    s->setParam(ps::Synth::FilterEnvAmount, 0.0f);
    s->setParam(ps::Synth::SubLevel, 0.0f);
    return s;
}

// 1. Every S4 setting at rest: the synth renders exactly as before them (same path, same samples),
// mono; the right channel is left alone.
void testPlainSynthIsUnchanged() {
    auto make = [] {
        auto s = std::make_unique<ps::Synth>();
        s->prepare(kRate);
        s->setParam(ps::Synth::Unison, 1.0f);
        s->setParam(ps::Synth::Lfo2ToCutoff, 0.4f);
        s->noteOn(48, 0.9f);
        s->noteOn(55, 0.7f, 300);
        s->noteOff(48, 20000);
        return s;
    };
    auto a = make(), b = make();
    CHECK(a->plain() && !a->stereo());
    std::vector<float> legacy(kRate, 0.0f);
    for (int pos = 0; pos < kRate; pos += 256) a->render(legacy.data() + pos, std::min(256, kRate - pos));
    const Stereo now = renderWide(*b, kRate, 256);
    bool same = true;
    for (int i = 0; i < kRate; ++i) same &= legacy[size_t(i)] == now.l[size_t(i)] && now.r[size_t(i)] == 0.0f;
    std::printf("  plain synth: %s\n", same ? "identical, mono" : "DIFFERENT");
    CHECK(same);
    CHECK(energy(legacy) > 1.0);
    // The sound from before S4 itself (review m11): this patch rendered by the synth of commit 88d750f
    // (the last before S4; same flags as here), kept in reference/. Never written by
    // --update-references: it is the old code's, not this one's.
    std::ifstream f(std::string(PS_REFERENCE_DIR) + "/synth_plain_pre_s4.f32", std::ios::binary);
    CHECK(f.good());
    if (!f.good()) return;
    std::vector<float> ref(static_cast<size_t>(kRate));
    f.read(reinterpret_cast<char*>(ref.data()), static_cast<std::streamsize>(ref.size() * sizeof(float)));
    CHECK(static_cast<size_t>(f.gcount()) == ref.size() * sizeof(float));
    float maxDiff = 0.0f, peak = 0.0f;
    for (size_t i = 0; i < ref.size(); ++i) {
        maxDiff = std::max(maxDiff, std::fabs(ref[i] - legacy[i]));
        peak = std::max(peak, std::fabs(ref[i]));
    }
    std::printf("  plain synth against the pre-S4 render: off by %.2g at most (peak %.3f)\n", double(maxDiff), double(peak));
    CHECK(peak > 0.05f);
    CHECK(maxDiff < 1e-5f);
}

// 2. Width: a supersaw spread left and right. Wide (side/mid between -15 and -3 dB), still
// correlated, its mono sum as loud as the narrow one, and the lows mono under MonoBelow.
void testUnisonWidthIsMonoCompatible() {
    auto make = [](float width) {
        auto s = makeSynth();
        s->setParam(ps::Synth::Unison, 1.0f);
        s->setParam(ps::Synth::UnisonDetune, 0.45f);
        s->setParam(ps::Synth::OscMix, 0.0f);
        s->setParam(ps::Synth::FilterType, 0.3f);  // the state-variable low-pass
        s->setParam(ps::Synth::Cutoff, 0.85f);
        s->setParam(ps::Synth::Width, width);
        s->noteOn(36, 0.9f);  // 65 Hz: a fundamental under 120 Hz
        s->noteOn(60, 0.9f);
        return s;
    };
    auto narrow = make(0.0f), wide = make(1.0f);
    CHECK(wide->stereo());
    const int n = kRate * 2;
    const Stereo a = renderWide(*narrow, n), b = renderWide(*wide, n);
    std::vector<float> mid(static_cast<size_t>(n)), side(static_cast<size_t>(n));
    for (size_t i = 0; i < size_t(n); ++i) {
        mid[i] = 0.5f * (b.l[i] + b.r[i]);
        side[i] = 0.5f * (b.l[i] - b.r[i]);
    }
    const size_t from = kRate / 2;
    const double sideMidDb = 10.0 * std::log10(energy(side, from) / energy(mid, from));
    const double corr = correlation(b.l, b.r, from, size_t(n));
    const double monoDb = 10.0 * std::log10(energy(mid, from) / energy(a.l, from));
    const auto lowL = twice(b.l, 100.0, false), lowR = twice(b.r, 100.0, false);
    const double lowCorr = correlation(lowL, lowR, from, size_t(n));
    std::printf("  width 1: side/mid %.1f dB, correlation %.2f (%.3f under 100 Hz), mono sum %+.2f dB against no width\n", sideMidDb,
                corr, lowCorr, monoDb);
    CHECK(sideMidDb > -15.0 && sideMidDb < -3.0);
    CHECK(corr > 0.3 && corr < 0.9);
    CHECK(lowCorr >= 0.95);
    CHECK(std::fabs(monoDb) < 1.0);
    // Without copies: oscillator 1 left, oscillator 2 (detuned) right.
    auto two = makeSynth();
    two->setParam(ps::Synth::Osc2Detune, 0.8f);
    two->setParam(ps::Synth::Width, 0.8f);
    two->noteOn(57, 0.9f);
    const Stereo c = renderWide(*two, kRate);
    const double twoCorr = correlation(c.l, c.r, kRate / 4, kRate);
    std::printf("  two oscillators, width 0.8: correlation %.2f\n", twoCorr);
    CHECK(twoCorr < 0.98 && twoCorr > 0.2);
}

// 3. Oversampling: a saw on C6 aliases under -60 dB (DaisySP's polyBLEP alone: about -52 dB), and
// with the drive (4x, anti-aliased) too.
void testOversamplingKeepsAliasingDown() {
    auto make = [](bool over, float drive, int note) {
        auto s = makeSynth();
        s->setParam(ps::Synth::OscMix, 0.0f);
        s->setParam(ps::Synth::FilterType, 0.3f);
        s->setParam(ps::Synth::Cutoff, 1.0f);
        s->setParam(ps::Synth::Resonance, 0.0f);
        s->setParam(ps::Synth::Oversample, over ? 1.0f : 0.0f);
        s->setParam(ps::Synth::Drive, drive);
        s->noteOn(note, 1.0f);
        return s;
    };
    const double c6 = 440.0 * std::pow(2.0, (84 - 69) / 12.0), c5 = c6 / 2;
    auto plain = make(false, 0.0f, 84), over = make(true, 0.0f, 84), driven = make(true, 0.4f, 72);
    const int n = 65536 + 9600;
    const double before = worstAliasDb(renderWide(*plain, n).l, 9600, c6);
    const double after = worstAliasDb(renderWide(*over, n).l, 9600, c6);
    const double drive = worstAliasDb(renderWide(*driven, n).l, 9600, c5);
    std::printf("  aliasing of a saw on C6: %.1f dB, oversampled %.1f dB; C5 oversampled and driven %.1f dB\n", before, after, drive);
    CHECK(after <= -60.0);
    CHECK(drive <= -60.0);
    CHECK(after < before - 6.0);
    // Its pitch is unchanged (the oscillators run at twice the rate, tuned for it).
    auto tone = make(true, 0.0f, 57);
    tone->setParam(ps::Synth::Osc1Wave, 1.0f);
    tone->setParam(ps::Synth::Cutoff, 0.3f);
    const auto t = renderWide(*tone, kRate).l;
    const double hz = crossingHz(t, kRate / 4, kRate);
    std::printf("  oversampled A3: %.1f Hz\n", hz);
    CHECK(std::fabs(hz - 220.0) < 3.0);
}

// 4. The slow cycle: a filter sweep over 8 bars, locked to the song's position; the brightness
// (spectral centroid) moves well over +-15 % around its middle, back where it started after 8 bars.
void testCycleSweepFollowsTheSong() {
    auto s = makeSynth();
    s->setParam(ps::Synth::Unison, 0.5f);
    s->setParam(ps::Synth::Cutoff, 0.45f);
    s->setParam(ps::Synth::CycleBars, 4.0f / 5.0f);  // 8 bars
    s->setParam(ps::Synth::CycleDepth, 0.5f);
    CHECK(ps::Synth::cycleBarsOf(4.0f / 5.0f) == 8);
    s->setBeatFrames(24000.0);  // 120 BPM: a bar = 2 s, 8 bars = 16 s
    s->noteOn(57, 0.9f);
    s->noteOn(64, 0.9f);
    const int frames = kRate * 18;
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    std::vector<float> r(static_cast<size_t>(frames), 0.0f);
    int step = 0;
    for (int pos = 0; pos < frames; pos += 192) {
        // The sequencer announces each sixteenth (6000 frames) on its frame.
        const int next = step * 6000;
        if (next >= pos && next < pos + 192) s->syncCycle(step++, next - pos);
        s->render(out.data() + pos, r.data() + pos, std::min(192, frames - pos));
    }
    std::vector<double> c;
    for (int bar = 0; bar <= 8; ++bar) c.push_back(centroid(out, size_t(std::min(frames - 4096, bar * 2 * kRate + kRate / 2))));
    const double lo = *std::min_element(c.begin(), c.end()), hi = *std::max_element(c.begin(), c.end());
    const double middle = std::sqrt(lo * hi);
    std::printf("  8-bar cycle: centroid %.0f Hz at bar 1, %.0f Hz at bar 5, %.0f Hz at bar 9 (+-%.0f %%)\n", c[0], c[4], c[8],
                100.0 * (hi / middle - 1.0));
    CHECK(hi / middle - 1.0 >= 0.15);
    CHECK(c[4] > c[0] * 1.3 && c[4] > c[8] * 1.3);
    CHECK(std::fabs(c[8] / c[0] - 1.0) < 0.1);
    CHECK(std::fabs(s->cyclePosition() - 18.0 * 8.0) < 0.5);  // 18 s at 8 sixteenths a second
}

// 5. A square sweep (riddim): the filter opens and closes like a gate, eighth notes, its edges
// softened (no click: the largest step stays a waveform's).
void testSquareSweepGates() {
    auto s = makeSynth();
    s->setParam(ps::Synth::Osc1Wave, 0.5f);
    s->setParam(ps::Synth::Cutoff, 0.3f);
    s->setParam(ps::Synth::Resonance, 0.3f);
    s->setParam(ps::Synth::Lfo2ToCutoff, 0.9f);
    s->setParam(ps::Synth::Lfo2Sync, ps::Synth::Lfo2Sync == 26 ? 4.0f / 7.0f : 0.0f);  // 1/8
    s->setParam(ps::Synth::Lfo2Shape, 1.0f / 3.0f);
    s->setBeatFrames(24000.0);
    s->noteOn(41, 1.0f);
    const auto out = renderWide(*s, kRate * 2).l;
    // Brightness (energy over 1 kHz) in each sixteenth: one high, one low (eighth-note gate).
    const auto high = filtered(filtered(out, 1000.0, true), 1000.0, true);
    std::vector<double> e;
    for (int k = 2; k < 14; ++k) e.push_back(energy(high, size_t(k * 3000 + 600), size_t(k * 3000 + 2400)));
    double open = 0.0, closed = 1e30;
    for (double v : e) {
        open = std::max(open, v);
        closed = std::min(closed, v);
    }
    float step = 0.0f, peak = 0.0f;
    for (size_t i = 1; i < out.size(); ++i) {
        step = std::max(step, std::fabs(out[i] - out[i - 1]));
        peak = std::max(peak, std::fabs(out[i]));
    }
    std::printf("  square sweep: open / closed %.1f dB over 1 kHz, largest step %.3f (peak %.3f)\n", 10.0 * std::log10(open / closed),
                double(step), double(peak));
    CHECK(10.0 * std::log10(open / closed) > 12.0);
    CHECK(step < peak);  // a square wave's own edges at most
}

// 6. Formant: the vowel moves the spectrum's peaks (A: around 700 Hz, I: 2.3 kHz).
void testFormantVowels() {
    auto make = [](float vowel) {
        auto s = makeSynth();
        s->setParam(ps::Synth::Cutoff, 0.9f);
        s->setParam(ps::Synth::Formant, 1.0f);
        s->setParam(ps::Synth::Vowel, vowel);
        s->noteOn(36, 1.0f);
        return s;
    };
    auto a = make(0.0f), i = make(0.5f);
    const auto xa = renderWide(*a, kRate).l, xi = renderWide(*i, kRate).l;
    auto ratio = [](const std::vector<float>& x) {
        return 10.0 * std::log10(bandEnergy(x, kRate / 4, kRate / 2, 600, 900) / bandEnergy(x, kRate / 4, kRate / 2, 2100, 2500));
    };
    std::printf("  formant: 600-900 Hz over 2.1-2.5 kHz: %.1f dB on A, %.1f dB on I\n", ratio(xa), ratio(xi));
    CHECK(ratio(xa) > ratio(xi) + 12.0);
    bool finite = true;
    for (float v : xa) finite &= std::isfinite(v);
    CHECK(finite);
}

// 7. The mid layer (bass music): above 250 Hz driven and wide, the sub still mono; the drive
// keeps the level within 3 dB.
void testMidLayerAndDrive() {
    auto make = [](float layer, float drive) {
        auto s = makeSynth();
        s->setParam(ps::Synth::Osc2Wave, 0.5f);
        s->setParam(ps::Synth::OscMix, 0.4f);
        s->setParam(ps::Synth::SubLevel, 0.6f);
        s->setParam(ps::Synth::Cutoff, 0.5f);
        s->setParam(ps::Synth::MidLayer, layer);
        s->setParam(ps::Synth::MonoBelow, 0.89f);  // 250 Hz
        s->setParam(ps::Synth::Drive, drive);
        s->noteOn(33, 1.0f);  // A1, 55 Hz
        return s;
    };
    auto dry = make(0.0f, 0.0f), layered = make(1.0f, 0.0f), driven = make(0.0f, 0.6f);
    const int n = kRate * 2;
    const Stereo a = renderWide(*dry, n), b = renderWide(*layered, n), c = renderWide(*driven, n);
    std::vector<float> mid(static_cast<size_t>(n)), side(static_cast<size_t>(n));
    for (size_t i = 0; i < size_t(n); ++i) {
        mid[i] = 0.5f * (b.l[i] + b.r[i]);
        side[i] = 0.5f * (b.l[i] - b.r[i]);
    }
    const size_t from = kRate / 2;
    const double sideMid = 10.0 * std::log10(energy(side, from) / energy(mid, from));
    const double lowCorr = correlation(twice(b.l, 120.0, false), twice(b.r, 120.0, false), from, size_t(n));
    const double body = 10.0 * std::log10(bandEnergy(mid, from, from + kRate / 2, 250, 1000) / bandEnergy(a.l, from, from + kRate / 2, 250, 1000));
    const double driveDb = 10.0 * std::log10(energy(c.l, from) / energy(a.l, from));
    std::printf("  mid layer: side/mid %.1f dB, correlation under 120 Hz %.3f, 250-1000 Hz %+.1f dB; drive 0.6: level %+.1f dB\n",
                sideMid, lowCorr, body, driveDb);
    CHECK(b.r != b.l);
    CHECK(sideMid > -15.0 && sideMid < -3.0);
    CHECK(lowCorr >= 0.95);
    CHECK(body > 1.0);
    CHECK(std::fabs(driveDb) < 3.0);
}

// 8. Synth and Plaits knobs on song lanes (automation kinds 8 and 9): a synth's level from silent
// to full over two bars; Plaits' level the same; a synth's sweep division changed on a block.
void testSynthAndPlaitsAutomation() {
    for (int kind : {ps::Automation::Synth, ps::Automation::Plaits}) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSynthEngine, kind == ps::Automation::Plaits ? 1.0f : 0.0f, kind == ps::Automation::Plaits ? 1 : 0});
        for (int k = 0; k < 8; ++k) engine.post({ps::CommandType::SetSynthNote, 0.9f, k, ps::NoteTrack::pack(k * 48, 40, 57)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 8});
        engine.post({ps::CommandType::SetSynthSteps, 0.0f, 16});
        const int param = kind == ps::Automation::Plaits ? int(ps::PlaitsSynth::Level) : int(ps::Synth::Level);
        engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(kind, 2, 0, param)});
        engine.post({ps::CommandType::SetAutomationMode, 3.0f, 0, ps::Automation::Song});  // 3 points
        engine.post({ps::CommandType::SetAutomationSong, 0.0f, 0, 0});
        engine.post({ps::CommandType::SetAutomationSong, 0.0f, 0, 1});
        engine.post({ps::CommandType::SetAutomationSong, 0.8f, 0, 2});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto out = render(engine, kRate * 6, 256);
        auto rms = [&](int from) {
            double e = 0;
            for (int i = from; i < from + kRate / 2; ++i) e += double(out[size_t(i) * 2]) * out[size_t(i) * 2];
            return std::sqrt(e / (kRate / 2));
        };
        const double start = rms(kRate / 8), middle = rms(kRate * 11 / 4), end = rms(kRate * 5);
        std::printf("  %s level on a song lane: %.4f, %.4f, %.4f\n", kind == ps::Automation::Plaits ? "Plaits" : "synth", start, middle, end);
        CHECK(start < end * 0.05);
        CHECK(middle > end * 0.1 && middle < end * 0.95);
    }
    // On the step's frame: an automated sweep division takes effect where its step falls.
    auto s = makeSynth();
    s->setParam(ps::Synth::Lfo2Sync, 3.0f / 7.0f);  // a quarter note
    s->setBeatFrames(24000.0);
    s->automate(ps::Synth::Lfo2Sync, 5.0f / 7.0f, 100);  // a sixteenth, 100 frames in
    std::vector<float> l(64, 0.0f), r(64, 0.0f);
    s->render(l.data(), r.data(), 64);
    CHECK(std::fabs(s->sweepHz() - 2.0f) < 0.01f);
    s->render(l.data(), r.data(), 64);
    CHECK(std::fabs(s->sweepHz() - 8.0f) < 0.01f);
}

// 9. Sound version 2: a stereo sample plays in stereo on an instrument track, and a General MIDI
// instrument's centred sound keeps its level; version 1 (projects saved before) stays mono.
void testStereoInstrumentsFollowTheSoundVersion() {
    std::vector<float> wave(static_cast<size_t>(kRate) * 2);
    for (int i = 0; i < kRate; ++i) {
        wave[size_t(i) * 2] = 0.3f * std::sin(2.0f * float(M_PI) * 300.0f * i / kRate);
        wave[size_t(i) * 2 + 1] = 0.3f * std::sin(2.0f * float(M_PI) * 500.0f * i / kRate);
    }
    auto play = [&](int version) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSoundVersion, float(version)});
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, 2});  // the sampled source
        ps::Command keys{ps::CommandType::InstallTrackSample, 0.0f, 0};
        keys.sample = ps::Sample::create(wave.data(), kRate, 2, kRate, kRate);
        engine.post(keys);
        engine.post({ps::CommandType::SetTrackSampleParam, 60.0f, ps::SampleInstrument::Root});
        engine.post({ps::CommandType::SynthNoteOn, 1.0f, 60});
        return render(engine, kRate / 2, 256);
    };
    const auto v1 = play(1), v2 = play(2);
    double diff1 = 0.0, diff2 = 0.0, midDiff = 0.0, total = 0.0;
    for (size_t i = size_t(kLate) * 2; i + 1 < v1.size(); i += 2) {
        diff1 += std::fabs(v1[i] - v1[i + 1]);
        diff2 += std::fabs(v2[i] - v2[i + 1]);
        midDiff += std::fabs((v2[i] + v2[i + 1]) - (v1[i] + v1[i + 1]));
        total += std::fabs(v1[i]);
    }
    std::printf("  stereo sample: L-R %.4f (version 1), %.4f (version 2); mid against mono %.5f\n", diff1 / total, diff2 / total, midDiff / total);
    CHECK(diff1 == 0.0);
    CHECK(diff2 > 0.5 * total);
    CHECK(midDiff < 0.01 * total);
    // The tiny SoundFont's centred sine: as loud in stereo as in mono.
    const std::string path = (std::filesystem::temp_directory_path() / "ps_wide.sf2").string();
    { auto bytes = tinySoundFont(); std::ofstream(path, std::ios::binary).write(reinterpret_cast<const char*>(bytes.data()), std::streamsize(bytes.size())); }
    tsf* master = ps::SoundFontPlayer::loadFile(path.c_str());
    CHECK(master != nullptr);
    if (!master) return;
    auto font = [&](int version) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSoundVersion, float(version)});
        ps::Command inst{ps::CommandType::SetInstrument, 0.0f, 0};
        inst.font = ps::SoundFontPlayer::makeInstance(master, kRate);
        engine.post(inst);
        engine.post({ps::CommandType::SynthNoteOn, 0.9f, 69});
        auto out = render(engine, kRate / 2, 256);
        engine.collectGarbage();
        return out;
    };
    const auto f1 = font(1), f2 = font(2);
    double e1 = 0, e2l = 0, e2r = 0;
    for (size_t i = size_t(kRate / 10) * 2; i + 1 < f1.size(); i += 2) {
        e1 += double(f1[i]) * f1[i];
        e2l += double(f2[i]) * f2[i];
        e2r += double(f2[i + 1]) * f2[i + 1];
    }
    std::printf("  SoundFont in stereo: left %+.2f dB, right %+.2f dB against mono\n", 10 * std::log10(e2l / e1), 10 * std::log10(e2r / e1));
    CHECK(std::fabs(10 * std::log10(e2l / e1)) < 0.2 && std::fabs(10 * std::log10(e2r / e1)) < 0.2);
    ps::SoundFontPlayer::close(master);
}

// 10. The plate and the room on the send bus: each rings and dies away (the room sooner), about
// as loud as the hall; the return's low cut takes the lows out; at rest the return is the plain one.
void testPlateRoomAndReturnEq() {
    auto tail = [](float type, float lowCut) {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setLimiter(false);
        mixer.setSend(2, 0, 1.0f);  // the dry burst is over at 0.1 s: after it, only the reverb
        mixer.setBusParam(0, 0, 0.6f);
        mixer.setBusParam(0, 4, type);
        if (lowCut > 0.0f) mixer.setBusParam(0, ps::Mixer::kBusLowCut, lowCut);
        mixer.settle();
        const int n = kRate * 5;
        std::vector<float> in(static_cast<size_t>(n), 0.0f);
        // A burst of noise (100 ms) then silence.
        uint32_t seed = 7;
        for (int i = 0; i < kRate / 10; ++i) {
            seed = seed * 1664525u + 1013904223u;
            in[size_t(i)] = 0.5f * (float(seed >> 8) / 16777216.0f - 0.5f);
        }
        std::vector<float> out(static_cast<size_t>(n) * 2);
        std::vector<const float*> channels(ps::Mixer::kChannels, nullptr);
        for (int pos = 0; pos < n; pos += 256) {
            channels[2] = in.data() + pos;
            mixer.process(channels.data(), nullptr, out.data() + size_t(pos) * 2, std::min(256, n - pos), 2);
        }
        std::vector<float> l(static_cast<size_t>(n));
        for (int i = 0; i < n; ++i) l[size_t(i)] = out[size_t(i) * 2];
        return l;
    };
    auto db = [](double a, double b) { return 10.0 * std::log10(a / b); };
    const auto hall = tail(0.0f, 0.0f), plate = tail(0.5f, 0.0f), room = tail(1.0f, 0.0f), cut = tail(0.5f, 0.5f);
    const size_t t0 = kRate * 15 / 100;
    const double hallE = energy(hall, t0, kRate), plateE = energy(plate, t0, kRate), roomE = energy(room, t0, kRate);
    // Decay: the second second against the first.
    const double hallDecay = db(energy(hall, kRate, 2 * kRate), hallE);
    const double plateDecay = db(energy(plate, kRate, 2 * kRate), plateE);
    const double roomDecay = db(energy(room, kRate, 2 * kRate), roomE);
    const double lows = db(bandEnergy(cut, t0, kRate / 2, 40, 100), bandEnergy(plate, t0, kRate / 2, 40, 100));
    std::printf("  reverbs: plate %+.1f dB, room %+.1f dB against the hall; decay over a second: hall %.1f, plate %.1f, room %.1f dB; "
                "low cut 200 Hz: %.1f dB at 40-100 Hz\n", db(plateE, hallE), db(roomE, hallE), hallDecay, plateDecay, roomDecay, lows);
    CHECK(std::fabs(db(plateE, hallE)) < 4.0);
    CHECK(std::fabs(db(roomE, hallE)) < 6.0);
    CHECK(plateDecay < -3.0 && roomDecay < hallDecay - 3.0);
    CHECK(lows < -12.0);
    const double last = energy(plate, size_t(kRate * 4), size_t(kRate * 5));
    CHECK(std::isfinite(last) && last < plateE * 1e-3);
    // The Live page's freeze holds the plate's tail (no decay) and lets it go again.
    {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setLimiter(false);
        mixer.setSend(2, 0, 1.0f);
        mixer.setBusParam(0, 4, 0.5f);
        mixer.settle();
        const int n = kRate * 4;
        std::vector<float> in(static_cast<size_t>(n), 0.0f), out(static_cast<size_t>(n) * 2);
        for (int i = 0; i < kRate / 10; ++i) in[size_t(i)] = 0.3f * std::sin(0.07f * float(i)) * std::sin(0.0031f * float(i));
        std::vector<const float*> channels(ps::Mixer::kChannels, nullptr);
        for (int pos = 0; pos < n; pos += 256) {
            if (pos == 256 * 40) mixer.setPerformance(ps::PerformanceFx::ReverbFreeze, 1.0f);   // at 0.21 s
            if (pos == 256 * 560) mixer.setPerformance(ps::PerformanceFx::ReverbFreeze, 0.0f);  // at 2.99 s
            channels[2] = in.data() + pos;
            mixer.process(channels.data(), nullptr, out.data() + size_t(pos) * 2, std::min(256, n - pos), 2);
        }
        std::vector<float> l(static_cast<size_t>(n));
        for (int i = 0; i < n; ++i) l[size_t(i)] = out[size_t(i) * 2];
        const double early = energy(l, kRate / 2, kRate), held = energy(l, kRate * 2, kRate * 5 / 2), gone = energy(l, kRate * 7 / 2, kRate * 4);
        std::printf("  plate frozen: %.1f dB after 1.5 s, %.1f dB once let go\n", db(held, early), db(gone, early));
        CHECK(db(held, early) > -3.0 && db(held, early) < 3.0);
        CHECK(db(gone, early) < -6.0);
    }
}

// 11. The mix's lows in mono (SetMaster 7): a 60 Hz difference between the sides is folded to the
// middle, a 1 kHz one stays.
void testMonoBassOnTheMaster() {
    auto run = [](float hz) {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setLimiter(false);
        mixer.setMonoBass(hz);
        mixer.settle();
        const int n = kRate;
        std::vector<float> l(static_cast<size_t>(n)), r(static_cast<size_t>(n));
        for (int i = 0; i < n; ++i) {
            const float low = 0.2f * std::sin(2.0f * float(M_PI) * 60.0f * i / kRate);
            const float high = 0.1f * std::sin(2.0f * float(M_PI) * 1000.0f * i / kRate);
            l[size_t(i)] = low + high;
            r[size_t(i)] = -low - high;  // all side
        }
        std::vector<float> out(static_cast<size_t>(n) * 2);
        std::vector<const float*> ch(ps::Mixer::kChannels, nullptr), rights(ps::Mixer::kChannels, nullptr);
        for (int pos = 0; pos < n; pos += 256) {
            ch[3] = l.data() + pos;
            rights[3] = r.data() + pos;
            mixer.process(ch.data(), rights.data(), nullptr, out.data() + size_t(pos) * 2, std::min(256, n - pos), 2);
        }
        std::vector<float> side(static_cast<size_t>(n));
        for (int i = 0; i < n; ++i) side[size_t(i)] = 0.5f * (out[size_t(i) * 2] - out[size_t(i) * 2 + 1]);
        return side;
    };
    const auto off = run(0.0f), on = run(120.0f);
    const double low = 10 * std::log10(bandEnergy(on, kRate / 2, kRate, 55, 65) / bandEnergy(off, kRate / 2, kRate, 55, 65));
    const double high = 10 * std::log10(bandEnergy(on, kRate / 2, kRate, 995, 1005) / bandEnergy(off, kRate / 2, kRate, 995, 1005));
    std::printf("  mono bass 120 Hz: side at 60 Hz %.1f dB, at 1 kHz %+.2f dB\n", low, high);
    CHECK(low < -12.0);
    CHECK(std::fabs(high) < 0.5);
}

// 12. Everything S4 at once in the engine: no allocation in process(), finite, and the synth's CPU
// within its budget (8 voices of a 7-copy oversampled wide supersaw, drive, formant, mid layer).
void testWideSynthsAllocateNothingAndFitTheBudget() {
#ifdef PS_COUNT_ALLOCATIONS
    std::vector<float> wave(static_cast<size_t>(kRate));
    for (size_t i = 0; i < wave.size(); ++i) wave[i] = 0.3f * std::sin(0.05f * float(i));
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetSoundVersion, 2.0f});
    for (int t = 0; t < 3; ++t) {
        engine.post({ps::CommandType::SetTrackOn, 1.0f, t});
        engine.post({ps::CommandType::SetSynthEngine, t == 2 ? 1.0f : 0.0f, t << 16});
        for (int n = 0; n < 16; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n | (t << 16), ps::NoteTrack::pack(n * 24, 30, 48 + n % 12)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 16 | (t << 16)});
        const float params[][2] = {{ps::Synth::Unison, 1.0f}, {ps::Synth::Width, 0.8f}, {ps::Synth::OctaveDown, 0.4f}, {ps::Synth::Drive, 0.4f},
                                   {ps::Synth::Lfo2Shape, 1.0f / 3.0f}, {ps::Synth::Lfo2ToCutoff, 0.5f}, {ps::Synth::Lfo2Sync, 4.0f / 7.0f},
                                   {ps::Synth::CycleBars, 0.8f}, {ps::Synth::CycleDepth, 0.4f}, {ps::Synth::Formant, 0.5f},
                                   {ps::Synth::MidLayer, 0.5f}, {ps::Synth::Oversample, 1.0f}};
        for (const auto& p : params) engine.post({ps::CommandType::SynthParam, p[1], int(p[0]) | (t << 16)});
    }
    ps::Command keys{ps::CommandType::InstallTrackSample, 0.0f, 3 << 16};
    std::vector<float> st(wave.size() * 2);
    for (size_t i = 0; i < wave.size(); ++i) st[i * 2] = st[i * 2 + 1] = wave[i];
    keys.sample = ps::Sample::create(st.data(), kRate, 2, kRate, kRate);
    engine.post({ps::CommandType::SetTrackOn, 1.0f, 3});
    engine.post({ps::CommandType::SetSynthEngine, 2.0f, 3 << 16});
    engine.post(keys);
    for (int n = 0; n < 8; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n | (3 << 16), ps::NoteTrack::pack(n * 48, 40, 60)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 8 | (3 << 16)});
    // Lanes on a synth knob (song) and a Plaits one (loop); the plate with its equalised return; mono bass.
    engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Synth, 5, 0, ps::Synth::Cutoff)});
    engine.post({ps::CommandType::SetAutomationMode, 2.0f, 0, ps::Automation::Song});
    for (int b = 0; b < 8; ++b) engine.post({ps::CommandType::SetAutomationSong, b / 8.0f, 0, b});
    engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 1, ps::Automation::pack(ps::Automation::Plaits, 6, 0, ps::PlaitsSynth::Timbre)});
    for (int s = 0; s < 16; ++s) engine.post({ps::CommandType::SetAutomationPoint, s / 16.0f, 1, s});
    engine.post({ps::CommandType::SetBusParam, 0.5f, 0, 4});
    engine.post({ps::CommandType::SetBusParam, 0.5f, 0, ps::Mixer::kBusLowCut});
    engine.post({ps::CommandType::SetBusParam, 0.7f, 0, ps::Mixer::kBusHighCut});
    engine.post({ps::CommandType::SetBusParam, 0.4f, 1, ps::Mixer::kBusLowCut});
    engine.post({ps::CommandType::SetMaster, 120.0f, 7});
    for (int ch : {2, 5, 6, 7}) engine.post({ps::CommandType::SetSend, 0.5f, ch, 0});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(512 * 2);
    for (int i = 0; i < 50; ++i) engine.process(nullptr, 0, 0, out.data(), 512, 2);
    g_allocations = 0;
    g_countAllocations = true;
    bool finite = true;
    for (int i = 0; i < 300; ++i) {
        if (i % 40 == 0) engine.post({ps::CommandType::SynthParam, (i % 80) ? 0.0f : 0.9f, ps::Synth::Width});
        if (i % 50 == 0) engine.post({ps::CommandType::SynthParam, (i % 100) ? 0.0f : 1.0f, ps::Synth::Oversample | (1 << 16)});
        if (i % 60 == 0) engine.post({ps::CommandType::SetBusParam, (i % 120) ? 0.0f : 1.0f, 0, 4});
        if (i % 70 == 0) engine.post({ps::CommandType::SetSoundVersion, (i % 140) ? 1.0f : 2.0f});
        engine.process(nullptr, 0, 0, out.data(), 512, 2);
        for (float v : out) finite &= std::isfinite(v);
    }
    g_countAllocations = false;
    std::printf("  S4 engine: %d allocations in 300 blocks\n", g_allocations.load());
    CHECK(g_allocations.load() == 0);
    CHECK(finite);
#endif
    // CPU: the heaviest synth there is now, 8 voices.
    auto s = makeSynth();
    for (auto [p, v] : {std::pair{ps::Synth::Unison, 1.0f}, {ps::Synth::Width, 1.0f}, {ps::Synth::OctaveDown, 0.5f}, {ps::Synth::Drive, 0.5f},
                        {ps::Synth::Formant, 0.5f}, {ps::Synth::MidLayer, 0.5f}, {ps::Synth::Oversample, 1.0f}, {ps::Synth::NoiseLevel, 0.2f},
                        {ps::Synth::CycleBars, 0.8f}, {ps::Synth::CycleDepth, 0.5f}, {ps::Synth::Resonance, 0.8f}}) {
        s->setParam(p, v);
    }
    for (int n = 0; n < 8; ++n) s->noteOn(48 + n * 3, 1.0f);
    const double start = threadSeconds();
    const Stereo o = renderWide(*s, kRate * 2);
    const double load = (threadSeconds() - start) / 2.0;
    std::printf("  wide synth, 8 voices x 7 copies, oversampled: %.1f %% of real time on this machine\n", load * 100.0);
    float peak = 0.0f;
    for (size_t i = 0; i < o.l.size(); ++i) peak = std::max(peak, std::max(std::fabs(o.l[i]), std::fabs(o.r[i])));
    CHECK(std::isfinite(peak) && peak < 6.0f);
#if !defined(__SANITIZE_ADDRESS__)
    CHECK_TIMING(load < 0.15);  // the synth's budget (synth_voices_and_cpu)
#endif
}

}  // namespace wide_tests

#define PS_WIDE_TESTS                                                                                 \
    {"s4_plain_synth_is_unchanged", wide_tests::testPlainSynthIsUnchanged},                           \
    {"s4_unison_width_mono_compatible", wide_tests::testUnisonWidthIsMonoCompatible},                 \
    {"s4_oversampling_aliasing", wide_tests::testOversamplingKeepsAliasingDown},                      \
    {"s4_cycle_sweep", wide_tests::testCycleSweepFollowsTheSong},                                     \
    {"s4_square_sweep", wide_tests::testSquareSweepGates},                                            \
    {"s4_formant", wide_tests::testFormantVowels},                                                    \
    {"s4_mid_layer_and_drive", wide_tests::testMidLayerAndDrive},                                     \
    {"s4_synth_plaits_automation", wide_tests::testSynthAndPlaitsAutomation},                         \
    {"s4_stereo_instruments", wide_tests::testStereoInstrumentsFollowTheSoundVersion},                \
    {"s4_plate_room_return_eq", wide_tests::testPlateRoomAndReturnEq},                                \
    {"s4_mono_bass", wide_tests::testMonoBassOnTheMaster},                                            \
    {"s4_no_alloc_and_cpu", wide_tests::testWideSynthsAllocateNothingAndFitTheBudget},
