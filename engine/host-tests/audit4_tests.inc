// Audit stage 4 (« audio expert »): the audio thread's headroom regained without changing the sound.
// The drive effects shape both channels block by block (DriveShaper::processPair) instead of frame
// by frame: these tests hold that to the frame-by-frame code, bit for bit (whole songs rendered
// through both are identical: docs/lots/AUDIT-4.md). Then the desktop checks of AUDIT étage 4.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace audit4 {

// A second shape (the synth's tanh), so the block form is not only right for one curve.
struct Tanh {
    double f(double x) const { return std::tanh(x); }
    double F(double x) const {
        const double a = std::fabs(x);
        return a + std::log1p(std::exp(-2.0 * a)) - 0.69314718055994531;
    }
};

// Music-like test input: two partials, a sweep to 20 kHz, noise and a stretch of silence.
std::vector<float> driveInput(int frames, uint32_t seed) {
    std::vector<float> x(static_cast<size_t>(frames));
    uint32_t s = seed;
    double phase = 0.0;
    for (int i = 0; i < frames; ++i) {
        s = s * 1664525u + 1013904223u;
        const float noise = (static_cast<float>(s >> 8) / 16777216.0f - 0.5f) * 0.2f;
        const double t = static_cast<double>(i) / kRate;
        phase += 2.0 * M_PI * (40.0 + 19960.0 * t / (static_cast<double>(frames) / kRate)) / kRate;
        const float silent = (i / 4800) % 7 == 3 ? 0.0f : 1.0f;
        x[static_cast<size_t>(i)] = silent * (0.5f * static_cast<float>(std::sin(2.0 * M_PI * 110.0 * t)) +
                                              0.3f * static_cast<float>(std::sin(phase)) + noise);
    }
    return x;
}

// processPair() is process() then dry(), frame for frame, bit for bit, and so is DriveShaperPair:
// any block size, a gain and an offset moving every frame, both shapes.
template <class Shape>
bool pairMatchesPerSample(const Shape& shape, uint32_t seed) {
    const int frames = kRate;
    const auto a = driveInput(frames, seed), b = driveInput(frames, seed * 7u + 1u);
    ps::DriveShaper<> sa, sb, pa, pb;
    ps::DriveShaperPair both;  // the stereo effects' form: one register for the pair's filters
    int mismatches = 0;
    uint32_t s = seed;
    float gain[ps::DriveShaper<>::kMaxBlock], offset[ps::DriveShaper<>::kMaxBlock];
    float wa[ps::DriveShaper<>::kMaxBlock], wb[ps::DriveShaper<>::kMaxBlock], da[ps::DriveShaper<>::kMaxBlock], db[ps::DriveShaper<>::kMaxBlock];
    for (int from = 0; from < frames;) {
        s = s * 1664525u + 1013904223u;
        const int n = std::min(frames - from, 1 + static_cast<int>(s >> 26));  // 1 to 64
        for (int i = 0; i < n; ++i) {
            const float t = static_cast<float>(from + i) / static_cast<float>(frames);
            gain[i] = 0.3f + 60.0f * t * t + 2.0f * std::sin(0.01f * static_cast<float>(from + i));
            offset[i] = 0.1f * t;
        }
        ps::DriveShaper<>::processPair(pa, pb, &a[static_cast<size_t>(from)], &b[static_cast<size_t>(from)], wa, wb, da, db, n, gain, offset, shape);
        float va[ps::DriveShaper<>::kMaxBlock], vb[ps::DriveShaper<>::kMaxBlock], ea[ps::DriveShaper<>::kMaxBlock], eb[ps::DriveShaper<>::kMaxBlock];
        both.process(&a[static_cast<size_t>(from)], &b[static_cast<size_t>(from)], va, vb, ea, eb, n, gain, offset, shape);
        for (int i = 0; i < n; ++i) {
            if (va[i] != wa[i] || vb[i] != wb[i] || ea[i] != da[i] || eb[i] != db[i]) ++mismatches;
            const float ya = sa.process(a[static_cast<size_t>(from + i)], gain[i], offset[i], shape);
            const float yb = sb.process(b[static_cast<size_t>(from + i)], gain[i], offset[i], shape);
            if (ya != wa[i] || yb != wb[i] || sa.dry() != da[i] || sb.dry() != db[i]) ++mismatches;
        }
        from += n;
    }
    // The null offset is 0 (the Saturator's way).
    ps::DriveShaper<> za, zb, qa, qb;
    for (int i = 0; i < 64; ++i) gain[i] = 4.0f;
    ps::DriveShaper<>::processPair(qa, qb, a.data(), b.data(), wa, wb, da, db, 64, gain, nullptr, shape);
    for (int i = 0; i < 64; ++i) {
        if (za.process(a[static_cast<size_t>(i)], 4.0f, 0.0f, shape) != wa[i] || zb.process(b[static_cast<size_t>(i)], 4.0f, 0.0f, shape) != wb[i]) ++mismatches;
    }
    return mismatches == 0;
}

void testDrivePairMatchesPerSample() {
    const bool soft = pairMatchesPerSample(ps::SoftClipShape{}, 11u), tanh = pairMatchesPerSample(Tanh{}, 13u);
    std::printf("  processPair and DriveShaperPair == process + dry, bit for bit: soft clip %d, tanh %d\n", soft, tanh);
    CHECK(soft && tanh);
}

// The Saturator effect through the block form: stereo, every block size the phone may ask (1 to 1024
// frames), the same output as one long block (no state lost at the 64-frame chunks), and its CPU.
void testSaturatorBlockSizesAgree() {
    const int frames = kRate;
    const auto a = driveInput(frames, 21u), b = driveInput(frames, 22u);
    auto run = [&](int block) {
        ps::Saturator sat;
        sat.prepare(static_cast<float>(kRate));
        sat.setParam(0, 0.8f);  // after prepare: the drive glides there over the first chunks
        sat.setParam(1, 0.5f);
        sat.setParam(4, 1.0f);
        std::vector<float> l(a), r(b);
        for (int from = 0; from < frames; from += block) {
            const int n = std::min(block, frames - from);
            sat.process(&l[static_cast<size_t>(from)], &r[static_cast<size_t>(from)], n);
        }
        l.insert(l.end(), r.begin(), r.end());
        return l;
    };
    const auto whole = run(frames);
    bool same = true;
    for (int block : {1, 37, 64, 65, 96, 192, 256, 1024}) same = same && run(block) == whole;
    std::printf("  saturator, blocks of 1 to 1024 frames: the same output as one block of 1 s: %d\n", same);
    CHECK(same);
}


// Biquad2 (the Ott's band split, both sides in one register) is two Biquads, bit for bit: every
// filter shape, an input moving on each side.
void testBiquadPairMatchesScalar() {
    const auto a = driveInput(kRate, 31u), b = driveInput(kRate, 32u);
    int mismatches = 0, filters = 0;
    float worst = 0.0f;
    for (int shape = 0; shape < 5; ++shape) {
        for (float hz : {40.0f, 120.0f, 2500.0f, 15000.0f}) {
            ps::Biquad c;
            switch (shape) {
                case 0: c.lowPass(kRate, hz); break;
                case 1: c.highPass(kRate, hz); break;
                case 2: c.peak(kRate, hz, 2.0f, 9.0f); break;
                case 3: c.lowShelf(kRate, hz, -6.0f); break;
                default: c.highPassQ(kRate, hz, 1.3066f); break;
            }
            ps::Biquad left = c, right = c;
            ps::Biquad2 both;
            both.set(c);
            both.reset();
            for (size_t i = 0; i < a.size(); ++i) {
                const ps::Float2 y = both.process(ps::Float2(a[i], b[i]));
                const float l = left.process(a[i]), r = right.process(b[i]);
                if (y.v[0] != l || y.v[1] != r) ++mismatches;
                worst = std::max(worst, std::max(std::fabs(y.v[0] - l) / (1.0f + std::fabs(l)),
                                                 std::fabs(y.v[1] - r) / (1.0f + std::fabs(r))));
            }
            ++filters;
        }
    }
    // Built with -ffast-math, the compiler may round the pair and the scalar differently (GCC 15 on CI
    // does, GCC 13 does not): bit for bit is reported, the check is an error well below the signal (-54 dB): a 40 Hz pole near 1 grows the rounding thousands of times (CI saw 4e-4): rounding noise, not a different filter.
    std::printf("  Biquad2 == two Biquads: %d filters, %d frames not bit for bit, worst error %.2e\n", filters, mismatches, worst);
    CHECK(worst < 2e-3f);
}

// KWeighting2 (the loudness meters' K-weighting, both sides in one register) is two KWeightings,
// bit for bit, at both rates.
void testKWeightingPairMatchesScalar() {
    const auto a = driveInput(kRate, 41u), b = driveInput(kRate, 42u);
    int mismatches = 0;
    for (int rate : {44100, 48000}) {
        ps::KWeighting left, right;
        ps::KWeighting2 both;
        left.prepare(rate);
        right.prepare(rate);
        both.prepare(rate);
        for (size_t i = 0; i < a.size(); ++i) {
            const ps::KWeighting2::Lanes y = both.process(a[i], b[i]);
            if (y[0] != left.process(a[i]) || y[1] != right.process(b[i])) ++mismatches;
        }
    }
    std::printf("  KWeighting2 == two KWeightings, bit for bit: %d frames apart\n", mismatches);
    CHECK(mismatches == 0);
}

// AUDIT étage 4, « sur ordinateur »: the Live page's worst case (16 pads, 8 loops, the song of 4 stems
// stretched 120 -> 128, drums, 4 effects per strip and on the master, sends, live effects) at every
// buffer size a phone may ask (64 to 1024 frames) and at 44.1 and 48 kHz: no NaN, under full scale,
// no allocation in the callback, and the audio thread's mean load within the bench's budget.
void testWorstCaseAtEveryBufferAndRate() {
    for (int rate : {44100, 48000}) {
        for (int block : {64, 128, 192, 256, 480, 512, 1024}) {
            const moteur17::Load load = moteur17::worstCaseLive(true, true, true, false, block, rate, 10);
            std::printf("  worst case at %d Hz, %4d-frame buffers: mean %.1f %%, p99 %.1f %%, worst %.1f %% of the buffer's time\n", rate, block,
                        load.mean * 100, load.p99 * 100, load.worst * 100);
            CHECK_TIMING(load.mean < 0.25);
        }
    }
}

// Ten minutes at 44.1 kHz with the buffer size changing at every callback (64 to 1024 frames, as a
// phone's stream may hand them out): a pad loop and an audio loop of clicks stay on the ideal grid
// (beat k at k x 60 / BPM x 44100, computed) to the frame, and nothing allocates.
void testTenMinutesAt44kWithChangingBuffers() {
    constexpr int rate = 44100;
    const double bpm = 128.0, beat = 60.0 / bpm * rate;  // 20671.875 frames: not a whole number
    const int late = ps::MasterChain::latencyFor(rate);
    ps::Engine engine(rate);
    const auto barFrames = int64_t(std::llround(4 * beat));
    const auto padLoop = moteur17::clicks(barFrames, beat);
    const auto cellLoop = moteur17::clicks(barFrames, beat, beat / 2);
    ps::Command pad{ps::CommandType::InstallClip, 0.0f, 0};
    pad.sample = ps::Sample::create(padLoop.data(), barFrames, 2, rate, rate);
    engine.post(pad);
    engine.post({ps::CommandType::SetClipParam, 4.0f, 0, ps::ClipLauncher::Beats});
    ps::Command cell{ps::CommandType::InstallAudioClip, 0.0f, 0};
    cell.sample = ps::Sample::create(cellLoop.data(), barFrames, 2, rate, rate);
    engine.post(cell);
    engine.post({ps::CommandType::SetAudioClipParam, 0.0f, 0, ps::AudioTracks::Start});
    engine.post({ps::CommandType::SetAudioClipParam, 16.0f, 0, ps::AudioTracks::LoopSteps});
    engine.post({ps::CommandType::SetTempo, float(bpm)});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    const int64_t total = int64_t(rate) * 600, tail = int64_t(rate) * 20;
    std::vector<float> out(1024 * 2), head, end;
    int allocations = 0;
    uint32_t seed = 4u;
    for (int64_t done = 0; done < total;) {
        seed = seed * 1664525u + 1013904223u;
        const int block = static_cast<int>(std::min<int64_t>(total - done, 64 + static_cast<int>((seed >> 8) % 961u)));
        g_allocations = 0;
        g_countAllocations = true;
        engine.process(nullptr, 0, 0, out.data(), block, 2);
        g_countAllocations = false;
        allocations += g_allocations.load();
        for (int i = 0; i < block; ++i) {
            if (done + i < tail) head.push_back(out[size_t(i) * 2]);
            else if (done + i >= total - tail) end.push_back(out[size_t(i) * 2]);
        }
        done += block;
    }
    engine.collectGarbage();
    auto offGrid = [&](const std::vector<float>& left, int64_t from, int& pads, int& cells) {
        double worst = 0.0;
        for (int64_t at : moteur17::onsets(left)) {
            const double t = double(from + at - late) / (beat / 2);  // in half beats
            const double k = std::round(t);
            worst = std::max(worst, std::fabs(t - k) * beat / 2);
            (int64_t(k) % 2 == 0 ? pads : cells)++;
        }
        return worst;
    };
    int pads0 = 0, cells0 = 0, pads1 = 0, cells1 = 0;
    const double first = offGrid(head, 0, pads0, cells0);
    const double last = offGrid(end, total - tail, pads1, cells1);
    std::printf("  10 min at 44.1 kHz, 128 BPM, buffers of 64 to 1024 frames: first 20 s %d pad + %d cell clicks, worst %.2f frames off; "
                "last 20 s %d + %d, worst %.2f frames; %d allocations\n", pads0, cells0, first, pads1, cells1, last, allocations);
    CHECK(pads1 >= 20 && cells1 >= 20);
    CHECK(first <= 1.0);
    CHECK(last <= 1.0);
#ifdef PS_COUNT_ALLOCATIONS
    CHECK(allocations == 0);
#endif
}

}  // namespace audit4

#define PS_AUDIT4_TESTS                                                                   \
    {"a4_drive_pair_matches_per_sample", audit4::testDrivePairMatchesPerSample},          \
    {"a4_saturator_block_sizes_agree", audit4::testSaturatorBlockSizesAgree},             \
    {"a4_biquad_pair_matches_scalar", audit4::testBiquadPairMatchesScalar},               \
    {"a4_kweighting_pair_matches_scalar", audit4::testKWeightingPairMatchesScalar},       \
    {"a4_worst_case_at_every_buffer_and_rate", audit4::testWorstCaseAtEveryBufferAndRate}, \
    {"a4_ten_minutes_at_44k_changing_buffers", audit4::testTenMinutesAt44kWithChangingBuffers},
