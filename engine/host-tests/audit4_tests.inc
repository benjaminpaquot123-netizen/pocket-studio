// Audit stage 4 (« audio expert »): the audio thread's headroom regained without changing the sound.
// The overdrive's curve takes its logarithm from a table and a short series (lnFrom1To4) instead of
// libm, and the drive effects shape both channels block by block (DriveShaper::processPair) instead
// of frame by frame. These tests hold both to the code from before: bit for bit for the block form,
// under -140 dBFS for the logarithm (the anti-aliasing divides F's differences: a rough logarithm
// would show there first).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace audit4 {

// The shape as it was before stage 4: std::log.
struct LibmSoftClip {
    double f(double x) const { return ps::SoftClipShape{}.f(x); }
    double F(double x) const {
        const double a = std::fabs(x);
        if (a >= 3.0) return 2.348392481493110 + (a - 3.0);
        return x * x / 18.0 + (4.0 / 3.0) * std::log((x * x + 3.0) / 3.0);
    }
};

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

void testFastLogMatchesLibm() {
    double worst = 0.0;
    auto at = [&](double y) {
        const double e = std::fabs(ps::lnFrom1To4(y) - std::log(y));
        worst = std::max(worst, e);
    };
    for (int i = 0; i <= 3000000; ++i) at(1.0 + 3.0 * i / 3000000.0);
    // Each cell's edges, an ulp either side.
    for (int c = 0; c <= 64; ++c) {
        const double edge = c < 32 ? 1.0 + c / 32.0 : 2.0 + (c - 32) / 16.0;
        for (double y : {std::nextafter(edge, 0.0), edge, std::nextafter(edge, 8.0)})
            if (y >= 1.0 && y <= 4.0) at(y);
    }
    const bool nanStays = std::isnan(ps::lnFrom1To4(std::nan("")));
    std::printf("  lnFrom1To4 on [1, 4]: worst error %.3g (libm's own rounding: 2.2e-16 to 4.4e-16), NaN in, NaN out: %d\n", worst, nanStays);
    CHECK(worst < 1.0e-15);
    CHECK(nanStays);
}

// processPair() is process() then dry(), frame for frame, bit for bit: any block size, a gain and an
// offset moving every frame, both shapes.
template <class Shape>
bool pairMatchesPerSample(const Shape& shape, uint32_t seed) {
    const int frames = kRate;
    const auto a = driveInput(frames, seed), b = driveInput(frames, seed * 7u + 1u);
    ps::DriveShaper<> sa, sb, pa, pb;
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
        for (int i = 0; i < n; ++i) {
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
    const bool soft = pairMatchesPerSample(ps::SoftClipShape{}, 11u), libm = pairMatchesPerSample(LibmSoftClip{}, 12u),
               tanh = pairMatchesPerSample(Tanh{}, 13u);
    std::printf("  processPair == process + dry, bit for bit: soft clip %d, its libm form %d, tanh %d\n", soft, libm, tanh);
    CHECK(soft && libm && tanh);
}

// The overdrive with lnFrom1To4 against the one with std::log, through the whole drive (4x, the
// anti-aliasing): the residual stays under -140 dBFS at every gain the Saturator uses (DaisySP's
// pre-gain runs from 0.5 to ~1000).
void testSoftClipNullAgainstLibm() {
    const auto x = driveInput(kRate * 2, 5u);
    double worstDb = -400.0;
    float worstGain = 0.0f;
    for (float g : {0.5f, 1.0f, 2.0f, 3.0f, 6.0f, 12.0f, 40.0f, 150.0f, 1000.0f}) {
        ps::DriveShaper<> fast, ref;
        double worst = 0.0;
        for (float v : x) {
            const float a = fast.process(v, g, 0.0f, ps::SoftClipShape{});
            const float b = ref.process(v, g, 0.0f, LibmSoftClip{});
            worst = std::max(worst, static_cast<double>(std::fabs(a - b)));
        }
        const double db = 20.0 * std::log10(worst + 1e-30);
        if (db > worstDb) {
            worstDb = db;
            worstGain = g;
        }
    }
    std::printf("  overdrive, table logarithm against libm's: worst residual %.1f dBFS (at pre-gain %.1f)\n", worstDb, worstGain);
    CHECK(worstDb < -140.0);
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
    {"a4_fast_log_matches_libm", audit4::testFastLogMatchesLibm},                         \
    {"a4_drive_pair_matches_per_sample", audit4::testDrivePairMatchesPerSample},          \
    {"a4_soft_clip_null_against_libm", audit4::testSoftClipNullAgainstLibm},              \
    {"a4_saturator_block_sizes_agree", audit4::testSaturatorBlockSizesAgree},             \
    {"a4_worst_case_at_every_buffer_and_rate", audit4::testWorstCaseAtEveryBufferAndRate}, \
    {"a4_ten_minutes_at_44k_changing_buffers", audit4::testTenMinutesAt44kWithChangingBuffers},
