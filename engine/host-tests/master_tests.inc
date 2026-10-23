// Phase 33 « son », step 2: the master chain (clipper + true-peak limiter), the loudness meter and
// target, the drive effects' oversampling, the export's formats and report. Included by
// engine_tests.cpp (its CHECK, kRate, kLate, render…); registered by PS_MASTER_TESTS in main().

// --- Measures --------------------------------------------------------------------------------

// The sanitizers' build (CI) runs the same code several times slower: its time budgets widen.
#if defined(__SANITIZE_ADDRESS__)
constexpr double kSlowBuild = 6.0;
#else
constexpr double kSlowBuild = 1.0;
#endif

// The true peak read independently of the engine's readers, through a long windowed sinc (64 taps
// a phase): at 4x (`every` 4: the points BS.1770 reads, what dBTP means, as libebur128 and FFmpeg
// read it) or 16x (`every` 1: close to what a high-grade converter rebuilds; content near
// Nyquist can peak a few tenths of a dB over its 4x reading).
// The first and last `edge` frames are left out: a signal cut dead at the end of a test buffer
// has an edge of its own there (an export ends on its tail or a fade).
double independentTruePeak(const std::vector<float>& interleaved, int every = 4, int channels = 2, size_t edge = 2048) {
    constexpr int kUp = 16, kHalf = 32;
    std::vector<double> h(static_cast<size_t>(kUp * 2 * kHalf + 1));
    const double beta = 9.0;
    auto bessel0 = [](double x) {
        double sum = 1.0, term = 1.0;
        for (int k = 1; k < 40; ++k) {
            term *= (x / (2.0 * k)) * (x / (2.0 * k));
            sum += term;
        }
        return sum;
    };
    for (int j = 0; j < static_cast<int>(h.size()); ++j) {
        const double t = double(j - kUp * kHalf) / kUp;  // in samples
        const double sinc = t == 0.0 ? 1.0 : std::sin(M_PI * t) / (M_PI * t);
        const double w = t / kHalf;
        h[size_t(j)] = sinc * bessel0(beta * std::sqrt(std::max(0.0, 1.0 - w * w))) / bessel0(beta);
    }
    const size_t frames = interleaved.size() / size_t(channels);
    double peak = 0.0;
    for (int c = 0; c < channels; ++c) {
        auto at = [&](long i) { return i < 0 || i >= long(frames) ? 0.0 : double(interleaved[size_t(i) * size_t(channels) + size_t(c)]); };
        for (size_t n = std::min(edge, frames); n + edge < frames; ++n) {
            peak = std::max(peak, std::fabs(at(long(n))));
            // Only near a loud sample: the points between it and its neighbours.
            if (std::fabs(at(long(n))) < 0.5 * peak && std::fabs(at(long(n) + 1)) < 0.5 * peak) continue;
            for (int p = every; p < kUp; p += every) {
                double v = 0.0;
                for (int k = -kHalf + 1; k <= kHalf; ++k) v += at(long(n) + k) * h[size_t(p - k * kUp + kUp * kHalf)];
                peak = std::max(peak, std::fabs(v));
            }
        }
    }
    return peak;
}

double toDb(double x) { return 20.0 * std::log10(std::max(x, 1e-12)); }

// In-place radix-2 FFT (size a power of two).
void fft(std::vector<double>& re, std::vector<double>& im) {
    const size_t n = re.size();
    for (size_t i = 1, j = 0; i < n; ++i) {
        size_t bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) {
            std::swap(re[i], re[j]);
            std::swap(im[i], im[j]);
        }
    }
    for (size_t len = 2; len <= n; len <<= 1) {
        const double a = -2.0 * M_PI / double(len);
        for (size_t i = 0; i < n; i += len) {
            for (size_t k = 0; k < len / 2; ++k) {
                const double wr = std::cos(a * double(k)), wi = std::sin(a * double(k));
                const size_t u = i + k, v = i + k + len / 2;
                const double xr = re[v] * wr - im[v] * wi, xi = re[v] * wi + im[v] * wr;
                re[v] = re[u] - xr;
                im[v] = im[u] - xi;
                re[u] += xr;
                im[u] += xi;
            }
        }
    }
}

// Aliasing as son/tools/alias.py measures it: Blackman-Harris window, the harmonics of f0 (+-6 Hz)
// and DC left out. `worst`: the highest other line re the fundamental (dBc); `share`: all of
// them re the whole (dB).
struct Alias {
    double worst, share;
};
Alias aliasOf(const std::vector<float>& x, size_t from, double f0, double maxHz = kRate / 2.0) {
    size_t n = 1;
    while (n * 2 <= x.size() - from) n *= 2;
    std::vector<double> re(n), im(n, 0.0);
    for (size_t i = 0; i < n; ++i) {
        const double t = 2.0 * M_PI * double(i) / double(n - 1);
        const double w = 0.35875 - 0.48829 * std::cos(t) + 0.14128 * std::cos(2 * t) - 0.01168 * std::cos(3 * t);
        re[i] = x[from + i] * w;
    }
    fft(re, im);
    double fund = 0.0, worst = 0.0, total = 0.0, other = 0.0;
    for (size_t k = 0; k <= n / 2; ++k) {
        const double f = double(k) * kRate / double(n), e = re[k] * re[k] + im[k] * im[k];
        total += e;
        bool harmonic = f < 10.0;
        for (int h = 1; h * f0 < kRate / 2.0; ++h) harmonic |= std::fabs(f - h * f0) < 6.0;
        if (std::fabs(f - f0) < 6.0) fund = std::max(fund, e);
        if (!harmonic && f <= maxHz) {
            worst = std::max(worst, e);
            other += e;
        }
    }
    return {10.0 * std::log10(worst / fund + 1e-30), 10.0 * std::log10(other / total + 1e-30)};
}

// A mixer strip fed `mono` (both sides through channel 0's pan law at the centre): the output.
std::vector<float> throughMixer(ps::Mixer& mixer, const std::vector<float>& mono, int block = 256) {
    std::vector<float> out(mono.size() * 2), zero(mono.size(), 0.0f);
    for (size_t pos = 0; pos < mono.size(); pos += size_t(block)) {
        const int n = int(std::min(size_t(block), mono.size() - pos));
        const float* ch[ps::Mixer::kChannels] = {mono.data() + pos, zero.data(), zero.data(), zero.data()};
        mixer.process(ch, nullptr, out.data() + pos * 2, n, 2);
    }
    return out;
}

// One side of an interleaved stereo signal.
std::vector<float> side(const std::vector<float>& interleaved, int c) {
    std::vector<float> s(interleaved.size() / 2);
    for (size_t i = 0; i < s.size(); ++i) s[i] = interleaved[2 * i + size_t(c)];
    return s;
}

// A loud, dense piece: kicks (a pitch-dropping sine with a click), a saw bass (band-limited), a
// hi-hat (filtered noise) and a chord, 128 BPM; `gain` over it.
std::vector<float> loudMaterial(int frames, float gain, unsigned seed = 3) {
    std::vector<float> x(size_t(frames), 0.0f);
    std::mt19937 rng(seed);
    std::uniform_real_distribution<float> noise(-1.0f, 1.0f);
    const int beat = kRate * 60 / 128;
    double phase = 0.0;
    float hp = 0.0f, prev = 0.0f;
    for (int i = 0; i < frames; ++i) {
        const int inBeat = i % beat;
        const double tb = double(inBeat) / kRate;
        // Kick: 160 Hz down to 50 Hz, 300 ms.
        const double f = 50.0 + 110.0 * std::exp(-tb * 40.0);
        phase += 2.0 * M_PI * f / kRate;
        double v = 0.9 * std::sin(phase) * std::exp(-tb * 6.0) + (inBeat < 48 ? 0.3 * noise(rng) : 0.0);
        // Bass on the off-beats: a saw at 55 Hz, band-limited.
        const int off = (i + beat / 2) % beat;
        if (off < beat / 2) {
            double saw = 0.0;
            for (int k = 1; k * 55.0 < 18000.0; ++k) saw += std::sin(2.0 * M_PI * k * 55.0 * i / kRate) / k;
            v += 0.35 * saw * std::exp(-double(off) / kRate * 3.0);
        }
        // Hat on the off-beat: high-passed noise, 60 ms.
        const float n = noise(rng);
        hp = 0.7f * (hp + n - prev);
        prev = n;
        if (off < kRate * 6 / 100) v += 0.25 * hp * std::exp(-double(off) / kRate * 50.0);
        // A held chord.
        for (double cf : {220.0, 277.18, 329.63}) v += 0.08 * std::sin(2.0 * M_PI * cf * i / kRate);
        x[size_t(i)] = gain * float(v);
    }
    return x;
}

// --- The master chain --------------------------------------------------------------------------

// Under the ceiling, without a target, the chain is a delay: sample for sample.
void testMasterTransparent() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    std::vector<float> x(kRate);
    for (size_t i = 0; i < x.size(); ++i) x[i] = 0.6f * float(std::sin(2 * M_PI * 997.0 * double(i) / kRate)) + 0.2f * float(std::sin(2 * M_PI * 61.0 * double(i) / kRate));
    const auto out = throughMixer(mixer, x);
    float diff = 0.0f;
    // The strip at unity and in the middle: each side is the input itself.
    for (size_t i = 0; i + size_t(kLate) < x.size(); ++i) diff = std::max(diff, std::fabs(out[(i + size_t(kLate)) * 2] - x[i]));
    std::printf("  under the ceiling: max diff %.3g\n", double(diff));
    CHECK(diff < 1e-6f);
    CHECK(mixer.latency() == kLate);
    // 5 (clipper) + 16 (true-peak reader) + 48 (look-ahead) - 1: the docs say 68 frames (1.42 ms).
    CHECK(ps::MasterChain::latencyFor(48000) == 68);
}

// Loud dense material pushed 12 dB into the master: true peak <= -1 dBTP read by an independent
// 16x reader; no NaN.
void testMasterTruePeak() {
    for (float pushDb : {6.0f, 12.0f, 18.0f}) {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setMasterGain(std::pow(10.0f, pushDb / 20.0f));
        mixer.settle();
        const auto x = loudMaterial(kRate * 4, 0.5f, unsigned(pushDb));
        const auto out = throughMixer(mixer, x, 192);
        bool finite = true;
        for (float v : out) finite &= std::isfinite(v);
        const double tp = independentTruePeak(out), tp16 = independentTruePeak(out, 1);
        std::printf("  pushed %+.0f dB: true peak %.2f dBTP (4x), %.2f at 16x, meter %.2f dBTP\n", double(pushDb), toDb(tp), toDb(tp16),
                    toDb(mixer.loudness().truePeak()));
        CHECK(finite);
        CHECK(toDb(tp) <= -1.0);
        CHECK(toDb(tp16) <= -0.95);
        CHECK(toDb(mixer.loudness().truePeak()) <= -1.0);
        CHECK(std::fabs(toDb(tp) - toDb(mixer.loudness().truePeak())) < 0.2);
    }
}

// A 50 Hz sine 6 dB over the ceiling: the limiter's gain holds through the wave (the bass hold)
// and the clipper leaves a held bass alone: THD (harmonics 2..10) < 0.3 %.
void testMasterBassDistortion() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    const float amp = ps::Mixer::kCeiling * 2.0f;  // +6 dB
    std::vector<float> x(size_t(kRate) * 3);
    for (size_t i = 0; i < x.size(); ++i) x[i] = amp * float(std::sin(2 * M_PI * 50.0 * double(i) / kRate));
    const auto out = side(throughMixer(mixer, x), 0);
    // The last second: 50 whole periods, every harmonic on its own bin.
    const size_t from = out.size() - size_t(kRate), to = out.size();
    const double fundamental = toneLevel(out, from, to, 50.0);
    double harmonics = 0.0;
    for (int k = 2; k <= 10; ++k) harmonics += std::pow(toneLevel(out, from, to, 50.0 * k), 2.0);
    const double thd = std::sqrt(harmonics) / fundamental;
    const float reduction = mixer.takeLimiterGain();
    std::printf("  50 Hz, %.1f dB of reduction: THD %.4f %%\n", -toDb(reduction), 100.0 * thd);
    CHECK(-toDb(reduction) > 5.5 && -toDb(reduction) < 7.0);
    CHECK(thd < 0.003);
}

// A 7 kHz sine 6 dB over the ceiling: what the chain adds off the harmonics <= -70 dB (son.md 2.6).
void testMasterAliasing() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    std::vector<float> x(size_t(kRate) * 2);
    for (size_t i = 0; i < x.size(); ++i) x[i] = ps::Mixer::kCeiling * 2.0f * float(std::sin(2 * M_PI * 7000.0 * double(i) / kRate));
    const auto out = side(throughMixer(mixer, x), 0);
    const Alias a = aliasOf(out, size_t(kRate) / 2, 7000.0);
    std::printf("  7 kHz +6 dB: inharmonic %.1f dB, worst line %.1f dBc\n", a.share, a.worst);
    CHECK(a.share <= -70.0);
}

// The clipper only takes peaks far over the ceiling, and at most kMaxClipDb of them; it never
// touches a held level (a bass), and a short burst over the ceiling comes out at its true peak.
void testMasterClipperOnAttacks() {
    ps::MasterChain chain;
    chain.prepare(kRate);
    // Kicks 6 dB over the ceiling: the clipper shaves their first milliseconds.
    const auto x = loudMaterial(kRate * 2, 1.0f);
    std::vector<float> l(x), r(x);
    float lowest = 1.0f;
    for (size_t pos = 0; pos < l.size(); pos += 256) {
        const int n = int(std::min<size_t>(256, l.size() - pos));
        lowest = std::min(lowest, chain.process(l.data() + pos, r.data() + pos, nullptr, n).minGain);
    }
    std::vector<float> both(l.size() * 2);
    for (size_t i = 0; i < l.size(); ++i) both[2 * i] = l[i], both[2 * i + 1] = r[i];
    const double tp = independentTruePeak(both);
    std::printf("  kicks: true peak %.2f dBTP, deepest reduction %.1f dB\n", toDb(tp), -toDb(lowest));
    CHECK(toDb(tp) <= -1.0);
    CHECK(lowest > 0.05f);
}

// With a target, an offset (an asymmetric saturation, a sample's DC) is taken out before the
// clipper and the limiter; turning the target on fades the blocker in (no step). Without one the
// master leaves it (the chain stays the input, delayed).
void testMasterDcBlocker() {
    auto run = [](float target, bool late) {
        ps::MasterChain chain;
        chain.prepare(kRate);
        if (!late) chain.setTarget(target);
        // 12 s: the sub-sonic blocker after the limiter answers the offset's first burst with a
        // slow tail (0.05 Hz, 3 s) that has died away by the last half second.
        std::vector<float> l(size_t(kRate) * 12), r;
        for (size_t i = 0; i < l.size(); ++i) l[i] = 0.1f + 0.2f * float(std::sin(2 * M_PI * 220.0 * double(i) / kRate));
        r = l;
        for (size_t pos = 0; pos < l.size(); pos += 256) {
            if (late && pos == size_t(kRate) / 2) chain.setTarget(target);
            chain.process(l.data() + pos, r.data() + pos, nullptr, int(std::min<size_t>(256, l.size() - pos)));
        }
        return l;
    };
    auto mean = [](const std::vector<float>& x, size_t from, size_t to) {
        double s = 0.0;
        for (size_t i = from; i < to; ++i) s += x[i];
        return s / double(to - from);
    };
    const auto with = run(-14.0f, false), without = run(NAN, false), late = run(-14.0f, true);
    const size_t end = with.size();
    std::printf("  DC 0.1: with a target %.5f, without %.5f\n", mean(with, end - size_t(kRate) / 2, end), mean(without, end - size_t(kRate) / 2, end));
    CHECK(std::fabs(mean(with, end - size_t(kRate) / 2, end)) < 1e-3);
    CHECK(std::fabs(mean(without, end - size_t(kRate) / 2, end) - 0.1) < 1e-3);
    // Turned on mid-way: the offset leaves gently (no sample-to-sample jump over the tone's own).
    float worst = 0.0f;
    for (size_t i = size_t(kRate) / 2; i < size_t(kRate); ++i) worst = std::max(worst, std::fabs(late[i] - late[i - 1]));
    CHECK(worst < 0.02f);
}

// Real time: the chain (4x clipper, true-peak limiter, meter, estimate) on loud stereo material.
void testMasterCpuBudget() {
    ps::MasterChain chain;
    chain.prepare(kRate);
    chain.setTarget(-8.0f);
    const int frames = kRate * 20;
    const auto x = loudMaterial(frames, 1.0f);
    std::vector<float> l(x), r(x);
    for (size_t i = 0; i < r.size(); ++i) r[i] *= 0.9f;
#if PS_COUNT_ALLOCATIONS
    g_allocations = 0;
    g_countAllocations = true;
#endif
    const auto t0 = std::chrono::steady_clock::now();
    for (int pos = 0; pos < frames; pos += 192) chain.process(l.data() + pos, r.data() + pos, nullptr, std::min(192, frames - pos));
    const double seconds = std::chrono::duration<double>(std::chrono::steady_clock::now() - t0).count();
#if PS_COUNT_ALLOCATIONS
    g_countAllocations = false;
    CHECK(g_allocations.load() == 0);
#endif
    const double load = seconds / (double(frames) / kRate);
    std::printf("  master chain: %.2f %% of a core (20 s in %.0f ms)\n", 100.0 * load, seconds * 1000.0);
    // Budget (son.md 2.6): 5 % of a phone's big core, 3 to 4 times slower than this one. Measured
    // 1.6 % here with both true-peak readers gated (2.0 % before); the phone's own figure is read
    // in Outils > Latence (cpuLoad) on an assistant's song.
    CHECK_TIMING(load < 0.02 * kSlowBuild);
}

// « Gain auto »: the average reduction it gives back holds through a silence (a break), so the
// music comes back at the level it left instead of swelling over 1.5 s.
void testCompressorAutoMakeupHoldsThroughSilence() {
    ps::InsertSlot slot;
    slot.prepare(kRate);
    slot.setType(ps::InsertSlot::Comp);
    const float params[] = {0.5f, 0.54f, 0.2f, 0.3f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f};  // -20 dB, ratio ~5, auto, mix 1
    for (int p = 0; p < 9; ++p) slot.setParam(p, params[p]);
    // 6 s of a loud sine (the average has settled), 2 s of silence, 2 s of the sine again.
    std::vector<float> l(size_t(kRate) * 10), r;
    for (size_t i = 0; i < l.size(); ++i) {
        const bool on = i < size_t(kRate) * 6 || i >= size_t(kRate) * 8;
        l[i] = on ? 0.5f * float(std::sin(2 * M_PI * 220.0 * double(i) / kRate)) : 0.0f;
    }
    r = l;
    for (size_t pos = 0; pos < l.size(); pos += 256) slot.process(l.data() + pos, r.data() + pos, int(std::min<size_t>(256, l.size() - pos)));
    auto rmsDb = [&](double from, double to) {
        double sum = 0.0;
        const size_t a = size_t(from * kRate), b = size_t(to * kRate);
        for (size_t i = a; i < b; ++i) sum += double(l[i]) * l[i];
        return 10.0 * std::log10(sum / double(b - a) + 1e-20);
    };
    const double before = rmsDb(5.5, 6.0), back = rmsDb(8.05, 8.3), later = rmsDb(9.5, 10.0);
    std::printf("  auto make-up: %.2f dB before the break, %.2f dB 100 ms after it, %.2f dB later\n", before, back, later);
    CHECK(std::fabs(back - before) < 1.0);
    CHECK(std::fabs(later - before) < 0.5);
}

// --- The loudness meter ----------------------------------------------------------------------

// EBU Tech 3341's stereo cases: a 1 kHz sine at -23 / -33 dBFS reads -23 / -33 LUFS; the gating
// cases (-36/-23/-36 and -26/-20/-26 dBFS) read -23 LUFS integrated; a 0 dBFS sine at fs/4 sampled
// between its peaks reads 0 dBTP (+3 dB over its samples).
void testLoudnessMeterEbu() {
    auto feed = [](ps::LoudnessMeter& m, const std::vector<std::pair<double, double>>& parts, double hz = 1000.0, double phase = 0.0) {
        long i = 0;
        for (auto [dbfs, seconds] : parts) {
            const double a = std::pow(10.0, dbfs / 20.0);
            for (long k = 0; k < long(seconds * kRate); ++k, ++i) {
                // A 10 ms fade-in: a tone that starts dead has its own peak at its edge.
                const double in = std::min(1.0, double(i) / (0.01 * kRate));
                const float v = float(a * in * in * std::sin(2 * M_PI * hz * double(i) / kRate + phase));
                m.add(v, v);
            }
        }
        m.publish();
    };
    struct Case {
        std::vector<std::pair<double, double>> parts;
        double integrated;
    };
    const Case cases[] = {
        {{{-23.0, 20.0}}, -23.0},
        {{{-33.0, 20.0}}, -33.0},
        {{{-36.0, 10.0}, {-23.0, 60.0}, {-36.0, 10.0}}, -23.0},
        {{{-72.0, 10.0}, {-36.0, 10.0}, {-23.0, 60.0}, {-36.0, 10.0}, {-72.0, 10.0}}, -23.0},
        {{{-26.0, 20.0}, {-20.0, 20.1}, {-26.0, 20.0}}, -23.0},
    };
    for (const auto& c : cases) {
        ps::LoudnessMeter m;
        m.prepare(kRate);
        feed(m, c.parts);
        std::printf("  integrated %.2f LUFS (expected %.1f), momentary %.2f, short-term %.2f\n", m.integratedLufs(), c.integrated,
                    double(m.momentary()), double(m.shortTerm()));
        CHECK(std::fabs(m.integratedLufs() - c.integrated) < 0.1);
    }
    {
        ps::LoudnessMeter m;
        m.prepare(kRate);
        feed(m, {{-23.0, 5.0}});
        CHECK(std::fabs(m.momentary() + 23.0f) < 0.1f && std::fabs(m.shortTerm() + 23.0f) < 0.1f);
    }
    {
        ps::LoudnessMeter m;
        m.prepare(kRate);
        feed(m, {{0.0, 1.0}}, kRate / 4.0, M_PI / 4.0);  // samples at +-0.707
        std::printf("  fs/4 sine between its samples: true peak %.2f dBTP\n", toDb(m.truePeak()));
        CHECK(std::fabs(toDb(m.truePeak())) < 0.1);
    }
}

// The engine's meter against FFmpeg's ebur128 filter on the same rendered files (when FFmpeg is
// installed, as on the CI): integrated and true peak within 0.3.
void testLoudnessMatchesFfmpeg() {
    if (std::system("ffmpeg -hide_banner -version > /dev/null 2>&1") != 0) {
        std::printf("  ffmpeg not found: skipped\n");
        return;
    }
    const std::string dir = std::string(PS_OUTPUT_DIR) + "/loudness";
    std::filesystem::create_directories(dir);
    struct Signal {
        const char* name;
        std::vector<float> stereo;
    };
    std::vector<Signal> signals;
    {
        // A loud mix through the master chain (true peaks), pink-ish noise, a quiet song-like level.
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setMasterGain(4.0f);
        signals.push_back({"loud-master", throughMixer(mixer, loudMaterial(kRate * 8, 0.5f))});
        std::mt19937 rng(11);
        std::normal_distribution<float> g(0.0f, 0.1f);
        std::vector<float> noise(size_t(kRate) * 16);
        float b0 = 0, b1 = 0, b2 = 0;
        for (size_t i = 0; i < noise.size(); i += 2) {
            const float w = g(rng);
            b0 = 0.99765f * b0 + w * 0.0990460f;
            b1 = 0.96300f * b1 + w * 0.2965164f;
            b2 = 0.57000f * b2 + w * 1.0526913f;
            noise[i] = b0 + b1 + b2 + w * 0.1848f;
            noise[i + 1] = 0.8f * noise[i] + 0.2f * g(rng);
        }
        signals.push_back({"pink", noise});
        signals.push_back({"quiet", throughMixer(mixer, loudMaterial(kRate * 8, 0.02f))});
    }
    for (auto& s : signals) {
        const std::string path = dir + "/" + s.name + ".wav";
        writeWav(path, s.stereo);
        ps::LoudnessMeter m;
        m.prepare(kRate);
        for (size_t i = 0; i + 1 < s.stereo.size(); i += 2) m.add(s.stereo[i], s.stereo[i + 1]);
        m.publish();
        const std::string log = dir + "/" + s.name + ".txt";
        const std::string cmd = "ffmpeg -hide_banner -nostats -i '" + path + "' -af ebur128=peak=true -f null - > '" + log + "' 2>&1";
        if (std::system(cmd.c_str()) != 0) {
            CHECK(false);
            continue;
        }
        std::ifstream f(log);
        std::string text((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>());
        // The summary at the end: "I:   -14.2 LUFS" and "Peak:   -1.1 dBFS".
        const size_t summary = text.rfind("Summary:");
        double integrated = 0, peak = 0;
        const bool parsed = summary != std::string::npos &&
                            std::sscanf(text.c_str() + text.find("I:", summary), "I: %lf", &integrated) == 1 &&
                            std::sscanf(text.c_str() + text.find("Peak:", summary), "Peak: %lf", &peak) == 1;
        CHECK(parsed);
        if (!parsed) continue;
        std::printf("  %-12s ours %.2f LUFS, %.2f dBTP; ffmpeg %.2f LUFS, %.2f dBTP\n", s.name, m.integratedLufs(), toDb(m.truePeak()), integrated, peak);
        CHECK(std::fabs(m.integratedLufs() - integrated) <= 0.3);
        CHECK(std::fabs(toDb(m.truePeak()) - peak) <= 0.3);
    }
}

// --- The loudness target ------------------------------------------------------------------------

// A song's export with a target: two passes, the file at the target (+-0.5 LU) from its start,
// true peak <= -1 dBTP; Streaming -14 too; without a target the same render as before.
void prepareLoudLoop(ps::OfflineRenderer& r, float masterGain) {
    r.post({ps::CommandType::SetTempo, 128.0f});
    for (int st : {0, 4, 8, 12}) r.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, st});
    for (int st : {4, 12}) r.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Snare, st});
    for (int st = 0; st < 16; st += 2) r.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::ClosedHat, st});
    for (int st = 0; st < 16; ++st) {
        r.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 5) % 12, st % 7 == 3 ? 1 : 0, st % 4 == 2, st % 6 == 5, st % 8 != 7}.pack()});
    }
    r.post({ps::CommandType::SetMaster, masterGain, 0});
}

void testLoudnessTargetExport() {
    const int64_t frames = int64_t(kRate) * 12;
    for (float target : {-8.0f, -14.0f, -6.0f}) {
        ps::OfflineRenderer r(kRate);
        prepareLoudLoop(r, 0.5f);
        r.post({ps::CommandType::SetMaster, target, 4});
        const auto out = r.renderToBuffer(frames, 0);
        ps::LoudnessMeter m;
        m.prepare(kRate);
        bool finite = true;
        for (size_t i = 0; i + 1 < out.size(); i += 2) {
            m.add(out[i], out[i + 1]);
            finite &= std::isfinite(out[i]) && std::isfinite(out[i + 1]);
        }
        const double tp = independentTruePeak(out);
        // The first two seconds too: at the target from the start (no slow climb).
        ps::LoudnessMeter first;
        first.prepare(kRate);
        for (size_t i = 0; i + 1 < size_t(kRate) * 4; i += 2) first.add(out[i], out[i + 1]);
        std::printf("  target %.0f: %.2f LUFS (first 2 s %.2f), true peak %.2f dBTP, drive %+.1f dB, report %.2f LUFS %.2f dBTP\n", double(target),
                    m.integratedLufs(), first.integratedLufs(), toDb(tp), double(r.report().driveDb), r.report().lufs, toDb(r.report().truePeak));
        CHECK(finite);
        CHECK(out.size() == size_t(frames) * 2);
        if (target > -7.0f) {
            // A gabber's -6 on this dense loop: the chain stops near -7 (MasterChain::kMaxLossDb:
            // more drive would only squash it), as loud as it gets cleanly.
            CHECK(m.integratedLufs() > -7.5 && m.integratedLufs() < -6.5 + 0.5);
            CHECK(r.report().driveDb < 12.0f);
        } else {
            CHECK(std::fabs(m.integratedLufs() - target) <= 0.5);
            CHECK(std::fabs(first.integratedLufs() - target) <= 1.0);
        }
        CHECK(toDb(tp) <= -1.0);
        CHECK(std::fabs(r.report().lufs - m.integratedLufs()) < 0.01);
    }
    // No target: one pass, the same file as the plain render (drive 0).
    ps::OfflineRenderer a(kRate), b(kRate);
    prepareLoudLoop(a, 0.5f);
    prepareLoudLoop(b, 0.5f);
    b.post({ps::CommandType::SetMaster, 0.0f, 4});  // "no target"
    const auto x = a.renderToBuffer(kRate * 2, 0), y = b.renderToBuffer(kRate * 2, 0);
    CHECK(x == y);
    CHECK(a.report().driveDb == 0.0f);
}

// The live engine: the drive glides from its estimate (never more than 0.1 dB per 100 ms), the
// output gets near the target, a target change glides without a click.
void testLoudnessTargetLive() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 128.0f});
    for (int st : {0, 4, 8, 12}) engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, st});
    for (int st = 0; st < 16; st += 2) engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::ClosedHat, st});
    for (int st = 0; st < 16; ++st) {
        engine.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 5) % 12, 0, st % 4 == 2, false, true}.pack()});
    }
    engine.post({ps::CommandType::SetMaster, 0.5f, 0});
    engine.post({ps::CommandType::SetMaster, -9.0f, 4});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    float last = engine.mixer().masterChain().driveDb(), worstStep = 0.0f;
    bool finite = true;
    std::vector<float> tail;
    for (int piece = 0; piece < 600; ++piece) {  // 60 s
        auto out = render(engine, kRate / 10, 240);
        for (float v : out) finite &= std::isfinite(v);
        const float d = engine.mixer().masterChain().driveDb();
        worstStep = std::max(worstStep, std::fabs(d - last));
        last = d;
        if (piece >= 500) tail.insert(tail.end(), out.begin(), out.end());
    }
    ps::LoudnessMeter m;
    m.prepare(kRate);
    for (size_t i = 0; i + 1 < tail.size(); i += 2) m.add(tail[i], tail[i + 1]);
    std::printf("  live target -9: drive %+.1f dB, last 10 s %.2f LUFS, largest step %.3f dB per 100 ms\n", double(last), m.integratedLufs(),
                double(worstStep));
    CHECK(finite);
    CHECK(worstStep <= 0.101f);
    CHECK(std::fabs(m.integratedLufs() + 9.0) <= 1.0);
    CHECK(engine.mixer().loudness().integrated() < -5.0f && engine.mixer().loudness().integrated() > -13.0f);

    // A target change (Club -9 -> Streaming -14) on a steady tone glides: no click.
    ps::MasterChain chain;
    chain.prepare(kRate);
    chain.setTarget(-9.0f);
    std::vector<float> l(static_cast<size_t>(kRate) * 8), r;
    for (size_t i = 0; i < l.size(); ++i) l[i] = 0.2f * float(std::sin(2 * M_PI * 220.0 * double(i) / kRate));
    r = l;
    for (size_t pos = 0; pos < l.size(); pos += 256) chain.process(l.data() + pos, r.data() + pos, nullptr, int(std::min<size_t>(256, l.size() - pos)));
    std::vector<float> l2(static_cast<size_t>(kRate)), r2;
    for (size_t i = 0; i < l2.size(); ++i) l2[i] = 0.2f * float(std::sin(2 * M_PI * 220.0 * double(i + l.size()) / kRate));
    r2 = l2;
    chain.setTarget(-14.0f);
    for (size_t pos = 0; pos < l2.size(); pos += 256) chain.process(l2.data() + pos, r2.data() + pos, nullptr, int(std::min<size_t>(256, l2.size() - pos)));
    // The envelope of the tone (its peak per period) moves by under 0.2 dB from one period to the next.
    double worst = 0.0, prevPeak = -1.0;
    const size_t period = size_t(kRate / 220);
    for (size_t at = 0; at + period <= l2.size(); at += period) {
        double p = 0.0;
        for (size_t i = at; i < at + period; ++i) p = std::max(p, double(std::fabs(l2[i])));
        if (prevPeak > 0.0) worst = std::max(worst, std::fabs(toDb(p) - toDb(prevPeak)));
        prevPeak = p;
    }
    std::printf("  -9 -> -14: drive %+.1f dB, largest step %.3f dB per period\n", double(chain.driveDb()), worst);
    CHECK(worst < 0.3);  // 5 dB over 300 ms (a click: several dB at once)
}

// --- Drive effects --------------------------------------------------------------------------------

// The Distortion's four curves on a seventh-octave saw (C7, band-limited) and the Saturation pushed
// on a 2.75 kHz sine: worst aliased line <= -50 dBc (son.md 2.6). The fold at a moderate drive.
void testDriveEffectsAliasing() {
    const double f0 = 2093.0;
    std::vector<float> saw(size_t(kRate) * 2);
    for (size_t i = 0; i < saw.size(); ++i) {
        double s = 0.0;
        for (int k = 1; k * f0 < 20000.0; ++k) s += std::sin(2 * M_PI * k * f0 * double(i) / kRate) / k;
        saw[i] = float(0.5 * s * 2.0 / M_PI);
    }
    auto run = [](ps::InsertSlot& slot, std::vector<float> x) {
        std::vector<float> r = x;
        for (size_t pos = 0; pos < x.size(); pos += 256) slot.process(x.data() + pos, r.data() + pos, int(std::min<size_t>(256, x.size() - pos)));
        return x;
    };
    for (int type = 0; type < ps::Distortion::kTypes; ++type) {
        ps::InsertSlot slot;
        slot.prepare(kRate);
        slot.setType(ps::InsertSlot::DistortionFx);
        slot.setParam(0, type == 3 ? 0.3f : 0.6f);  // drive (the fold multiplies the wave: a lighter push)
        slot.setParam(1, 1.0f);                     // tone open
        slot.setParam(2, (type + 0.5f) / 4.0f);
        slot.setParam(3, 1.0f);
        const Alias a = aliasOf(run(slot, saw), size_t(kRate) / 2, f0);
        std::printf("  distortion type %d on C7 saw: worst alias %.1f dBc (inharmonic %.1f dB)\n", type, a.worst, a.share);
        CHECK(a.worst <= -50.0);
    }
    ps::InsertSlot sat;
    sat.prepare(kRate);
    sat.setType(ps::InsertSlot::Saturation);
    sat.setParam(0, 0.8f);
    sat.setParam(1, 1.0f);
    std::vector<float> sine(size_t(kRate) * 2);
    for (size_t i = 0; i < sine.size(); ++i) sine[i] = 0.5f * float(std::sin(2 * M_PI * 2750.0 * double(i) / kRate));
    // Its square's 9th harmonic (24.75 kHz) folds to 23.25 kHz through the last half-band's edge
    // (-6 dB at 24 kHz by design): over 20 kHz, unheard. What is heard, under 20 kHz:
    const auto saturated = run(sat, sine);
    const Alias a = aliasOf(saturated, size_t(kRate) / 2, 2750.0, 20000.0), full = aliasOf(saturated, size_t(kRate) / 2, 2750.0);
    std::printf("  saturation drive 0.8 on 2.75 kHz: worst alias %.1f dBc under 20 kHz (%.1f over the whole band)\n", a.worst, full.worst);
    CHECK(a.worst <= -50.0);
    // A drive turned during a tone glides: no jump in the output's level from one period to the next.
    ps::InsertSlot dist;
    dist.prepare(kRate);
    dist.setType(ps::InsertSlot::DistortionFx);
    std::vector<float> l(static_cast<size_t>(kRate)), r;
    for (size_t i = 0; i < l.size(); ++i) l[i] = 0.3f * float(std::sin(2 * M_PI * 200.0 * double(i) / kRate));
    r = l;
    double worstJump = 0.0;
    float prevLevel = -1.0f;
    for (size_t pos = 0; pos < l.size(); pos += 240) {
        if (pos == size_t(kRate) / 2) dist.setParam(0, 1.0f);
        dist.process(l.data() + pos, r.data() + pos, 240);
        float p = 0.0f;
        for (size_t i = pos; i < pos + 240; ++i) p = std::max(p, std::fabs(l[i]));
        if (prevLevel > 0.0f) worstJump = std::max(worstJump, std::fabs(toDb(p) - toDb(prevLevel)));
        prevLevel = p;
    }
    std::printf("  distortion drive turned: largest level step %.2f dB per 5 ms\n", worstJump);
    CHECK(worstJump < 1.5);
}

// Real time: Distortion and Saturation inserts, 4x with their anti-aliasing.
void testDriveEffectsCpu() {
    for (int type : {ps::InsertSlot::DistortionFx, ps::InsertSlot::Saturation}) {
        ps::InsertSlot slot;
        slot.prepare(kRate);
        slot.setType(type);
        slot.setParam(0, 0.8f);
        const int frames = kRate * 10;
        std::vector<float> l = loudMaterial(frames, 0.5f), r = l;
        const auto t0 = std::chrono::steady_clock::now();
        for (int pos = 0; pos < frames; pos += 192) slot.process(l.data() + pos, r.data() + pos, std::min(192, frames - pos));
        const double load = std::chrono::duration<double>(std::chrono::steady_clock::now() - t0).count() / 10.0;
        std::printf("  %s: %.2f %% of a core\n", type == ps::InsertSlot::Saturation ? "saturation" : "distortion", 100.0 * load);
        // son.md asks an insert under 1 % of a phone's core: about 0.4 % (saturation) and 0.8 %
        // (distortion, hot) here; the bound only catches a regression (shared machines vary).
        CHECK_TIMING(load < 0.015 * kSlowBuild);
    }
}

// --- Export --------------------------------------------------------------------------------------

// 44.1 kHz 16-bit with a target: the header, the length, the report (as measured on the file).
void testExportFormats() {
    const std::string dir = std::string(PS_OUTPUT_DIR) + "/formats";
    std::filesystem::create_directories(dir);
    const std::string path = dir + "/song-44k-16.wav";
    ps::OfflineRenderer r(kRate);
    prepareLoudLoop(r, 0.5f);
    r.post({ps::CommandType::SetMaster, -9.0f, 4});
    r.setOutputFormat(44100, 16);
    const int64_t frames = int64_t(kRate) * 6;
    CHECK(r.renderToWav(path.c_str(), frames, 0) == ps::OfflineRenderer::Result::Ok);
    CHECK(!std::filesystem::exists(path + ".premaster"));  // the first pass's music is gone
    std::ifstream f(path, std::ios::binary);
    std::vector<char> bytes((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>());
    CHECK(bytes.size() > 44);
    if (bytes.size() <= 44) return;
    auto u16 = [&](size_t at) { return uint16_t(uint8_t(bytes[at]) | uint8_t(bytes[at + 1]) << 8); };
    auto u32 = [&](size_t at) { return uint32_t(u16(at)) | uint32_t(u16(at + 2)) << 16; };
    const uint32_t rate = u32(24), dataBytes = u32(40);
    const uint16_t bits = u16(34);
    const double seconds = double(dataBytes) / 4.0 / rate;
    std::vector<float> pcm(dataBytes / 2);
    for (size_t i = 0; i < pcm.size(); ++i) pcm[i] = float(int16_t(u16(44 + 2 * i))) / 32768.0f;
    ps::LoudnessMeter m;
    m.prepare(int(rate));
    for (size_t i = 0; i + 1 < pcm.size(); i += 2) m.add(pcm[i], pcm[i + 1]);
    std::printf("  44.1 kHz %d-bit: %.3f s, %.2f LUFS (report %.2f), true peak %.2f dBTP (report %.2f)\n", bits, seconds, m.integratedLufs(),
                r.report().lufs, toDb(m.peakSinceReset()), toDb(r.report().truePeak));
    CHECK(rate == 44100 && bits == 16);
    CHECK(r.report().sampleRate == 44100 && r.report().frames == int64_t(dataBytes / 4));
    CHECK(std::fabs(seconds - 6.0) < 0.01);
    CHECK(std::fabs(m.integratedLufs() - r.report().lufs) < 0.05);
    CHECK(std::fabs(m.integratedLufs() + 9.0) < 0.5);
    CHECK(toDb(m.peakSinceReset()) <= -1.0);
}

#define PS_MASTER_TESTS                                         \
    {"master_transparent", testMasterTransparent},              \
    {"master_true_peak", testMasterTruePeak},                   \
    {"master_bass_distortion", testMasterBassDistortion},       \
    {"master_aliasing", testMasterAliasing},                    \
    {"master_clipper_on_attacks", testMasterClipperOnAttacks},  \
    {"master_dc_blocker", testMasterDcBlocker},                 \
    {"master_cpu_budget", testMasterCpuBudget},                 \
    {"compressor_auto_makeup_holds_through_silence", testCompressorAutoMakeupHoldsThroughSilence}, \
    {"loudness_meter_ebu", testLoudnessMeterEbu},               \
    {"loudness_matches_ffmpeg", testLoudnessMatchesFfmpeg},     \
    {"loudness_target_export", testLoudnessTargetExport},       \
    {"loudness_target_live", testLoudnessTargetLive},           \
    {"drive_effects_aliasing", testDriveEffectsAliasing},       \
    {"drive_effects_cpu", testDriveEffectsCpu},                 \
    {"export_formats", testExportFormats},
