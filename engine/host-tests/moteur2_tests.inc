// Tour 9, lot B2 (« Moteur, optimiser »): a drive at rest after a long silence costs nothing and
// changes nothing. The DriveShapers skip their 4x filters and their curve once the input has been
// exactly zero for ps::kQuietFrames frames (docs/audits/ultra/tour-9-moteur2-rapport.md).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace moteur2_tests {

// A curve that does not go through 0: its output at rest is a constant, so it must never rest.
struct Biased {
    double f(double x) const { return std::tanh(x) + 0.25; }
    double F(double x) const {
        const double a = std::fabs(x);
        return a + std::log1p(std::exp(-2.0 * a)) - 0.69314718055994531 + 0.25 * x;
    }
};

std::vector<float> burst(int frames, uint32_t seed) {
    std::vector<float> x(static_cast<size_t>(frames));
    uint32_t s = seed;
    for (int i = 0; i < frames; ++i) {
        s = s * 1664525u + 1013904223u;
        x[static_cast<size_t>(i)] = 0.6f * std::sin(0.031f * static_cast<float>(i)) + (static_cast<float>(s >> 8) / 16777216.0f - 0.5f) * 0.3f;
    }
    return x;
}

// Signal, then a silence long enough to rest, then signal again: during the rest every output is
// an exact zero, and what follows is bit for bit what a fresh shaper makes of the same signal (the
// rest state is the reset state), on the three forms (per sample, processPair, DriveShaperPair).
void testDriveRestsAfterSilence() {
    constexpr int kB = ps::DriveShaper<>::kMaxBlock;
    const int quiet = ps::kQuietFrames + 3 * kB;
    const auto a = burst(4096, 3u), b = burst(4096, 5u);
    std::vector<float> zeros(static_cast<size_t>(quiet), 0.0f);
    float gain[kB];
    std::fill(gain, gain + kB, 6.0f);
    ps::DriveShaper<> s1, pa, pb, f1, fa, fb;
    ps::DriveShaperPair both, fresh;
    const ps::SoftClipShape shape;
    float wa[kB], wb[kB], da[kB], db[kB], va[kB], vb[kB], ea[kB], eb[kB];
    // The history.
    for (int from = 0; from < 4096; from += kB) {
        ps::DriveShaper<>::processPair(pa, pb, &a[static_cast<size_t>(from)], &b[static_cast<size_t>(from)], wa, wb, da, db, kB, gain, nullptr, shape);
        both.process(&a[static_cast<size_t>(from)], &b[static_cast<size_t>(from)], va, vb, ea, eb, kB, gain, nullptr, shape);
        for (int i = 0; i < kB; ++i) s1.process(a[static_cast<size_t>(from + i)], 6.0f, 0.0f, shape);
    }
    // The silence: the last frames (at rest) all exact zeros.
    int restNonZero = 0;
    for (int from = 0; from < quiet; from += kB) {
        ps::DriveShaper<>::processPair(pa, pb, &zeros[static_cast<size_t>(from)], &zeros[static_cast<size_t>(from)], wa, wb, da, db, kB, gain, nullptr, shape);
        both.process(&zeros[static_cast<size_t>(from)], &zeros[static_cast<size_t>(from)], va, vb, ea, eb, kB, gain, nullptr, shape);
        for (int i = 0; i < kB; ++i) {
            const float y = s1.process(0.0f, 6.0f, 0.0f, shape);
            if (from + i >= ps::kQuietFrames + kB && (y != 0.0f || s1.dry() != 0.0f || wa[i] != 0.0f || wb[i] != 0.0f || va[i] != 0.0f ||
                                                       vb[i] != 0.0f || da[i] != 0.0f || ea[i] != 0.0f))
                ++restNonZero;
        }
    }
    const bool restedAll = s1.atRest() && pa.atRest() && pb.atRest() && both.atRest();
    // Back: the same as fresh shapers, bit for bit (and awake again).
    int mismatches = 0;
    for (int from = 0; from < 4096; from += kB) {
        const float* xa = &b[static_cast<size_t>(from)];
        const float* xb = &a[static_cast<size_t>(from)];
        ps::DriveShaper<>::processPair(pa, pb, xa, xb, wa, wb, da, db, kB, gain, nullptr, shape);
        both.process(xa, xb, va, vb, ea, eb, kB, gain, nullptr, shape);
        float ga[kB], gb[kB], ha[kB], hb[kB], ka[kB], kb[kB], la[kB], lb[kB];
        ps::DriveShaper<>::processPair(fa, fb, xa, xb, ga, gb, ha, hb, kB, gain, nullptr, shape);
        fresh.process(xa, xb, ka, kb, la, lb, kB, gain, nullptr, shape);
        for (int i = 0; i < kB; ++i) {
            const float y = s1.process(xa[i], 6.0f, 0.0f, shape), z = f1.process(xa[i], 6.0f, 0.0f, shape);
            if (y != z || s1.dry() != f1.dry() || wa[i] != ga[i] || wb[i] != gb[i] || da[i] != ha[i] || db[i] != hb[i] || va[i] != ka[i] ||
                vb[i] != kb[i] || ea[i] != la[i] || eb[i] != lb[i])
                ++mismatches;
        }
    }
    if (s1.atRest() || pa.atRest() || both.atRest()) ++mismatches;
    const bool rested = restedAll;
    std::printf("  drive at rest: rested %d, %d non-zero outputs in the rest, %d mismatches with a fresh drive after it\n", rested ? 1 : 0,
                restNonZero, mismatches);
    CHECK(rested);
    CHECK(restNonZero == 0);
    CHECK(mismatches == 0);
}

// What must never rest: a NaN is not a silence (it goes through as before), a non-zero offset is
// not a silence, and a curve not through 0 keeps its constant output.
void testDriveRestOnlyOnTrueSilence() {
    constexpr int kB = ps::DriveShaper<>::kMaxBlock;
    const int quiet = ps::kQuietFrames + 4 * kB;
    float zeros[kB] = {}, gain[kB], offset[kB], wa[kB], wb[kB], da[kB], db[kB];
    std::fill(gain, gain + kB, 3.0f);
    std::fill(offset, offset + kB, 0.2f);
    // A biased curve: its output in the silence stays f's constant (through the filters), never 0.
    ps::DriveShaperPair biased;
    ps::DriveShaper<> biasedOne;
    float lastPair = 0.0f, lastOne = 0.0f;
    for (int from = 0; from < quiet; from += kB) {
        biased.process(zeros, zeros, wa, wb, da, db, kB, gain, nullptr, Biased{});
        for (int i = 0; i < kB; ++i) lastOne = biasedOne.process(0.0f, 3.0f, 0.0f, Biased{});
        lastPair = wa[kB - 1];
    }
    // An offset: u = 0.2 the whole time (the offset is added after the gain), never a rest.
    ps::DriveShaperPair offsetPair;
    for (int from = 0; from < quiet; from += kB) offsetPair.process(zeros, zeros, wa, wb, da, db, kB, gain, offset, ps::SoftClipShape{});
    const float lastOffset = wa[kB - 1];
    // A NaN after a rest: it is shaped (NaN out), not taken for a zero.
    ps::DriveShaperPair nan;
    for (int from = 0; from < quiet; from += kB) nan.process(zeros, zeros, wa, wb, da, db, kB, gain, nullptr, ps::SoftClipShape{});
    float withNan[kB] = {};
    withNan[5] = std::nanf("");
    bool nanOut = false;
    for (int k = 0; k < 8; ++k) {
        nan.process(k == 0 ? withNan : zeros, zeros, wa, wb, da, db, kB, gain, nullptr, ps::SoftClipShape{});
        for (int i = 0; i < kB; ++i) nanOut = nanOut || std::isnan(wa[i]);
    }
    std::printf("  never at rest: biased curve %.4f / %.4f, offset %.4f, NaN shaped %d\n", lastPair, lastOne, lastOffset, nanOut ? 1 : 0);
    CHECK(!biased.atRest() && !biasedOne.atRest() && !offsetPair.atRest());
    CHECK(std::fabs(lastPair - 0.25f) < 1e-3f);
    CHECK(std::fabs(lastOne - 0.25f) < 1e-3f);
    CHECK(std::fabs(lastOffset - static_cast<float>(ps::SoftClipShape{}.f(0.2))) < 1e-3f);
    CHECK(nanOut);
}

// The rest comes on the frame where the zero run reaches kQuietFrames, whatever the block size:
// processPair at blocks of 1, 37 and 64 frames is bit for bit two per-sample shapers (each channel
// resting on its own), DriveShaperPair the same at every block size, with silences that start and
// end off the 64-frame grid and one channel playing while the other is silent.
void testDriveRestSameAtEveryBlockSize() {
    constexpr int kB = ps::DriveShaper<>::kMaxBlock;
    const int frames = 12000;
    auto a = burst(frames, 7u), b = burst(frames, 9u);
    for (int i = 1003; i < 1003 + ps::kQuietFrames + 517; ++i) a[static_cast<size_t>(i)] = 0.0f;
    for (int i = 2111; i < 2111 + ps::kQuietFrames + 1301; ++i) b[static_cast<size_t>(i)] = 0.0f;
    for (int i = 7001; i < 7001 + ps::kQuietFrames + 99; ++i) a[static_cast<size_t>(i)] = b[static_cast<size_t>(i)] = 0.0f;
    float gain[kB];
    std::fill(gain, gain + kB, 5.0f);
    const ps::SoftClipShape shape;
    std::vector<float> refA(static_cast<size_t>(frames)), refB(refA), refDa(refA), refDb(refA);
    ps::DriveShaper<> sa, sb;
    for (int i = 0; i < frames; ++i) {
        const auto k = static_cast<size_t>(i);
        refA[k] = sa.process(a[k], 5.0f, 0.0f, shape);
        refDa[k] = sa.dry();
        refB[k] = sb.process(b[k], 5.0f, 0.0f, shape);
        refDb[k] = sb.dry();
    }
    int pairMismatches = 0, stereoMismatches = 0;
    std::vector<float> stereo64;
    for (int block : {64, 1, 37}) {
        ps::DriveShaper<> pa, pb;
        ps::DriveShaperPair both;
        std::vector<float> st;
        for (int from = 0; from < frames; from += block) {
            const int n = std::min(block, frames - from);
            const auto f = static_cast<size_t>(from);
            float wa[kB], wb[kB], da[kB], db[kB];
            ps::DriveShaper<>::processPair(pa, pb, &a[f], &b[f], wa, wb, da, db, n, gain, nullptr, shape);
            for (int i = 0; i < n; ++i) {
                const auto k = f + static_cast<size_t>(i);
                if (wa[i] != refA[k] || wb[i] != refB[k] || da[i] != refDa[k] || db[i] != refDb[k]) ++pairMismatches;
            }
            both.process(&a[f], &b[f], wa, wb, da, db, n, gain, nullptr, shape);
            for (int i = 0; i < n; ++i) {
                st.push_back(wa[i]);
                st.push_back(wb[i]);
                st.push_back(da[i]);
                st.push_back(db[i]);
            }
        }
        if (block == 64) stereo64 = st;
        else if (st != stereo64) ++stereoMismatches;
    }
    std::printf("  drive rest at blocks of 1/37/64: %d pair frames off the per-sample shapers, %d stereo block sizes off\n",
                pairMismatches, stereoMismatches);
    CHECK(pairMismatches == 0);
    CHECK(stereoMismatches == 0);
}

}  // namespace moteur2_tests

#define PS_MOTEUR2_TESTS                                                                 \
    {"m2_drive_rests_after_silence", moteur2_tests::testDriveRestsAfterSilence},         \
    {"m2_drive_rest_only_on_true_silence", moteur2_tests::testDriveRestOnlyOnTrueSilence}, \
    {"m2_drive_rest_same_at_every_block_size", moteur2_tests::testDriveRestSameAtEveryBlockSize},
