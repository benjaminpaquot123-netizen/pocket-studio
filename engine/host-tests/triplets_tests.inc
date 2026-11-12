// Triplets (« Triolet »): a roll of N hits spread over M steps (SetDrumStepRoll's span), the trap
// and drill hats' 3 hits over 2 sixteenths and 6 over a beat. Each hit lands on its exact sample,
// whatever the tempo, the swing and the block size, live and in the offline export. Included by
// engine_tests.cpp (kept apart to ease merging).

namespace triplets_tests {

// A step's first sample as the sequencer places it: its grid frame (a sum of step lengths, as the
// clock adds them) plus the swing on an odd step, rounded.
int64_t stepSample(int step, double bpm, double swing) {
    const double sps = kRate * 60.0 / (bpm * 4.0);
    double grid = 0.0;
    for (int i = 0; i < step; ++i) grid += sps;
    return std::llround(grid + ((step & 1) != 0 ? swing * sps : 0.0));
}

// A step's true length with the swing (an even step longer by it, an odd one shorter).
double stepLength(int step, double bpm, double swing) {
    const double sps = kRate * 60.0 / (bpm * 4.0);
    return sps * (1.0 + (((step + 1) & 1) != 0 ? swing : 0.0) - ((step & 1) != 0 ? swing : 0.0));
}

struct Spread { int step, hits, span; };

// The pattern: a plain hit on step 0 (the reference), a triplet (3 over 2) on step 4, another
// starting on an odd step (9, swung), and 6 hits over the last beat (12..15), all flat.
const Spread kSpreads[] = {{0, 1, 1}, {4, 3, 2}, {9, 3, 2}, {12, 6, 4}};

template <typename Target>
void postPattern(Target& t, double bpm, double swing) {
    using ps::CommandType;
    t.post({CommandType::SetTempo, static_cast<float>(bpm)});
    t.post({CommandType::SetSwing, static_cast<float>(swing)});
    std::vector<float> click(64, 0.0f);
    click[0] = 1.0f;
    t.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(click.data(), 64, 1, kRate, kRate)});
    t.post({CommandType::SetPadParam, 0.0f, 5, ps::Sampler::Release});
    t.post({CommandType::SetPatternSteps, 0.0f, 16});
    for (const Spread& s : kSpreads) {
        // The hit, then its roll: `hits` over `span` steps, flat (ramp 1).
        t.post({CommandType::SetDrumHit, 0.8f, 5, s.step | (std::min(s.hits, 8) << 16)});
        if (s.span > 1) t.post({CommandType::SetDrumStepRoll, 0.0f, 5, s.step | (s.span << 8) | (1 << 12) | (s.hits << 16)});
    }
}

// The samples each hit should start on, from the bar's first sample.
std::vector<int64_t> wanted(double bpm, double swing) {
    std::vector<int64_t> out;
    for (const Spread& s : kSpreads) {
        const int64_t base = stepSample(s.step, bpm, swing);
        double length = 0.0;
        for (int j = 0; j < s.span; ++j) length += stepLength(s.step + j, bpm, swing);
        if (s.span == 1) length = stepLength(s.step, bpm, 0.0);  // a one-step roll keeps the plain length
        for (int k = 0; k < s.hits; ++k) out.push_back(base + std::llround(k * length / s.hits));
    }
    out.push_back(stepSample(16, bpm, swing));  // the next loop's first hit
    return out;
}

// Where the left channel's clicks start (a click after silence).
std::vector<int64_t> onsets(const std::vector<float>& interleaved) {
    std::vector<int64_t> out;
    const size_t n = interleaved.size() / 2;
    for (size_t i = 0; i < n; ++i) {
        const bool on = std::fabs(interleaved[2 * i]) > 0.02f;
        if (on && (i == 0 || std::fabs(interleaved[2 * (i - 1)]) <= 0.02f)) out.push_back(static_cast<int64_t>(i));
    }
    return out;
}

// The triplets' hits, live: on their exact sample at three tempos (one with a step of a fractional
// length), straight and swung, in blocks of 64 to 1024 frames (one odd), with nothing allocated on
// the audio thread, no NaN and no clipping.
void testTripletsLandOnTheirSample() {
    using ps::CommandType;
    for (double bpm : {140.0, 97.3, 173.0}) {
        for (double swing : {0.0, 0.3}) {
            const auto want = wanted(bpm, swing);
            const int frames = static_cast<int>(stepSample(16, bpm, 0.0)) + 2000;
            for (int block : {64, 192, 256, 333, 1024}) {
                ps::Engine e(kRate);
                postPattern(e, bpm, swing);
                e.post({CommandType::SetTransport, 1.0f, 0, 0});
                std::vector<float> out(static_cast<size_t>(frames) * kChannels, 0.0f);
#if PS_COUNT_ALLOCATIONS
                g_allocations = 0;
                g_countAllocations = true;
#endif
                for (int pos = 0; pos < frames; pos += block) {
                    e.process(nullptr, 0, 0, out.data() + static_cast<size_t>(pos) * kChannels, std::min(block, frames - pos), kChannels);
                }
#if PS_COUNT_ALLOCATIONS
                g_countAllocations = false;
                CHECK(g_allocations.load() == 0);
#endif
                const auto got = onsets(out);
                CHECK(got.size() == want.size());
                if (got.size() != want.size()) {
                    std::printf("  %.1f BPM block %d: %zu hits, %d triggers:", bpm, block, got.size(), e.laneTriggerCount(5));
                    for (auto g : got) std::printf(" %lld", static_cast<long long>(g - kLate));
                    std::printf("\n");
                }
                int wrong = 0;
                for (size_t k = 0; k < std::min(got.size(), want.size()); ++k) {
                    if (got[k] != want[k] + kLate) {
                        if (++wrong <= 3) std::printf("  %.1f BPM swing %.1f block %d: hit %zu at %lld, wanted %lld\n", bpm, swing, block, k,
                                                      static_cast<long long>(got[k]), static_cast<long long>(want[k] + kLate));
                    }
                }
                CHECK(wrong == 0);
                CHECK(e.laneTriggerCount(5) == 1 + 3 + 3 + 6 + 1);
                float peak = 0.0f;
                bool finite = true;
                for (float v : out) {
                    finite = finite && std::isfinite(v);
                    peak = std::max(peak, std::fabs(v));
                }
                CHECK(finite);
                CHECK(peak < 1.0f);
            }
        }
    }
    // The triplet's three hits evenly apart: 2/3 of a step each (at 140 BPM, 3428.57 frames).
    const auto w = wanted(140.0, 0.0);
    std::printf("  triplet at 140 BPM: %lld %lld %lld (2 steps = %lld)\n", static_cast<long long>(w[1]), static_cast<long long>(w[2]),
                static_cast<long long>(w[3]), static_cast<long long>(stepSample(6, 140.0, 0.0) - stepSample(4, 140.0, 0.0)));
    CHECK(w[2] - w[1] == 3429 || w[2] - w[1] == 3428);
    CHECK(std::llabs((w[3] - w[1]) - 6857) <= 1);
}

// A covered step has nothing of its own (the app clears it); a hit sent on the step after the
// triplet's start anyway (another version's file) plays on its own, the triplet unchanged; and a
// plain SetDrumHit on the triplet's step makes it one hit again (the span goes back to 1).
void testTripletResetsWithItsHit() {
    using ps::CommandType;
    ps::Engine e(kRate);
    e.post({CommandType::SetTempo, 120.0f});  // a step = 6000 frames
    std::vector<float> click(64, 0.0f);
    click[0] = 1.0f;
    e.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(click.data(), 64, 1, kRate, kRate)});
    e.post({CommandType::SetPadParam, 0.0f, 5, ps::Sampler::Release});
    e.post({CommandType::SetDrumHit, 0.8f, 5, 0 | (3 << 16)});
    e.post({CommandType::SetDrumStepRoll, 0.0f, 5, 0 | (2 << 8) | (1 << 12) | (3 << 16)});
    e.post({CommandType::SetTransport, 1.0f, 0, 0});
    const auto first = onsets(render(e, 12000 + kLate + 100, 256));
    CHECK(first.size() == 3);
    if (first.size() == 3) {
        CHECK(first[0] == kLate);
        CHECK(first[1] == 4000 + kLate);
        CHECK(first[2] == 8000 + kLate);
    }
    e.post({CommandType::SetTransport, 0.0f});
    render(e, 48000, 256);
    // The hit sent again, plain: 3 hits in its own step (the roll in one step), no span left.
    e.post({CommandType::SetDrumHit, 0.8f, 5, 0 | (3 << 16)});
    e.post({CommandType::SetTransport, 1.0f, 0, 0});
    const auto again = onsets(render(e, 12000 + kLate + 100, 256));
    CHECK(again.size() == 3);
    if (again.size() == 3) {
        CHECK(again[1] == 2000 + kLate);
        CHECK(again[2] == 4000 + kLate);
    }
}

// The export renders the triplets as the live engine plays them: the same hits, sample for sample
// (relative to the bar's first hit: the export starts on the music itself).
void testTripletsExportAsPlayed() {
    using ps::CommandType;
    for (double bpm : {140.0, 97.3}) {
        const double swing = 0.2;
        const auto want = wanted(bpm, swing);
        const int64_t frames = stepSample(16, bpm, 0.0) + 2000;
        ps::OfflineRenderer r(kRate);
        postPattern(r, bpm, swing);
        const auto mem = r.renderToBuffer(frames, 0);
        const auto got = onsets(mem);
        CHECK(got.size() == want.size());
        int wrong = 0;
        for (size_t k = 0; k < std::min(got.size(), want.size()); ++k) {
            if (got[k] - got[0] != want[k] - want[0]) {
                if (++wrong <= 3) std::printf("  export %.1f BPM: hit %zu at +%lld, wanted +%lld\n", bpm, k,
                                              static_cast<long long>(got[k] - got[0]), static_cast<long long>(want[k] - want[0]));
            }
        }
        CHECK(wrong == 0);
        for (float v : mem) CHECK(std::isfinite(v));
    }
}

}  // namespace triplets_tests

#define PS_TRIPLETS_TESTS \
    {"triplets_land_on_their_sample", triplets_tests::testTripletsLandOnTheirSample}, \
    {"triplet_resets_with_its_hit", triplets_tests::testTripletResetsWithItsHit}, \
    {"triplets_export_as_played", triplets_tests::testTripletsExportAsPlayed},
