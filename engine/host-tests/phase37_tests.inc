// Phase 37 (docs/PLAN-AMELIORATION.md, « Écrire comme un pro »): a length per drum pad (each lane
// loops over its own steps: a polyrhythm) and a chance per hit, drawn the same way every time from
// a seed, the pad and the step's place in the song (so an export is the song heard).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace phase37_tests {

constexpr int kStep = 6000;  // a sixteenth at 120 BPM

// The hits of `lane` on each of `steps` transport steps (one step rendered at a time: a step's hit
// lands on its first frame, inside its own block).
std::vector<int> hitsPerStep(ps::Engine& engine, int lane, int steps) {
    std::vector<int> out;
    std::vector<float> buf(static_cast<size_t>(kStep) * kChannels);
    for (int s = 0; s < steps; ++s) {
        const uint32_t before = engine.laneTriggerCount(lane);
        engine.process(nullptr, 0, 0, buf.data(), kStep, kChannels);
        out.push_back(static_cast<int>(engine.laneTriggerCount(lane) - before));
    }
    return out;
}

void testLanesLoopOnTheirOwnLength() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    engine.post({ps::CommandType::SetDrumLaneSteps, 3.0f, 0});  // the kick: every 3 steps (3 against 4)
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 1, 0});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 1, 2});
    engine.post({ps::CommandType::SetDrumLaneSteps, 5.0f, 1});  // two hits over 5 steps
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 2, 0});     // the pattern's own loop (16)
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto kick = hitsPerStep(engine, 0, 48);
    CHECK(engine.drumLaneSteps(0) == 3);
    int kicks = 0, onThree = 0;
    for (int s = 0; s < 48; ++s) {
        kicks += kick[static_cast<size_t>(s)];
        if (kick[static_cast<size_t>(s)] > 0 && s % 3 == 0) ++onThree;
    }
    std::printf("  lane of 3 steps: %d hits in 48 steps (%d on its own beat)\n", kicks, onThree);
    CHECK(kicks == 16 && onThree == 16);
    CHECK(engine.laneTriggerCount(1) == 20);  // steps 0, 2, 5, 7, … 45, 47
    CHECK(engine.laneTriggerCount(2) == 3);   // bars 1, 2, 3

    // Back to the pattern's length (0): the lane plays its step 0 once a bar again.
    engine.post({ps::CommandType::SetDrumLaneSteps, 0.0f, 0});
    engine.post({ps::CommandType::SetTransport, 0.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto plain = hitsPerStep(engine, 0, 32);
    int plainHits = 0;
    for (int h : plain) plainHits += h;
    CHECK(plainHits == 2 && plain[0] == 1 && plain[16] == 1);
}

void testLaneLengthIsSampleAccurate() {
    // Lane 0 looping over 3 steps with its hit on step 0 sounds, over its first 6 steps, exactly as a
    // plain pattern with hits on steps 0 and 3 (the same frames, sample for sample).
    auto make = [](bool poly) {
        auto e = std::make_unique<ps::Engine>(kRate);
        e->post({ps::CommandType::SetTempo, 120.0f});
        e->post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
        if (poly) e->post({ps::CommandType::SetDrumLaneSteps, 3.0f, 0});
        else e->post({ps::CommandType::SetDrumHit, 1.0f, 0, 3});
        e->post({ps::CommandType::SetTransport, 1.0f, 0});
        return e;
    };
    auto a = make(true);
    auto b = make(false);
    const auto ra = render(*a, kStep * 6, 97);
    const auto rb = render(*b, kStep * 6, 256);
    float diff = 0.0f, peak = 0.0f;
    for (size_t i = 0; i < ra.size(); ++i) {
        diff = std::max(diff, std::fabs(ra[i] - rb[i]));
        peak = std::max(peak, std::fabs(ra[i]));
    }
    std::printf("  lane of 3 vs hits on 0 and 3: diff %.2e (peak %.3f)\n", diff, peak);
    CHECK(peak > 0.05f);
    CHECK(diff < 1e-6f);
}

void testWaitingSceneBringsItsLaneLengths() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, kStep * 4, 256);
    // The next scene: the same kick on a lane of 4 steps (one a beat), staged then swapped at the bar.
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    engine.post({ps::CommandType::SetDrumLaneSteps, 4.0f, 0});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    CHECK(engine.drumLaneSteps(0) == 0);  // staged, not playing yet
    const auto rest = hitsPerStep(engine, 0, 12);  // steps 4..15 of the first scene: no kick
    int before = 0;
    for (int h : rest) before += h;
    const auto after = hitsPerStep(engine, 0, 16);
    int hits = 0;
    for (int h : after) hits += h;
    std::printf("  staged lane of 4: %d kicks before the swap, %d in the next bar\n", before, hits);
    CHECK(before == 0);
    CHECK(hits == 4 && after[0] == 1 && after[4] == 1 && after[8] == 1 && after[12] == 1);
    CHECK(engine.drumLaneSteps(0) == 4);
}

void testChancesAreDrawnTheSameEveryTime() {
    // The app's draw (DrumChance.kt) and the engine's agree: the same masks in both tests.
    unsigned m = 0, m2 = 0;
    for (int k = 0; k < 32; ++k) {
        if (ps::Engine::chancePlays(7, 3, k, 50)) m |= 1u << k;
        if (ps::Engine::chancePlays(0, 0, k, 25)) m2 |= 1u << k;
    }
    std::printf("  chance masks: %08x %08x\n", m, m2);
    CHECK(m == 0x40b5c267u);
    CHECK(m2 == 0x5b7df3f6u);
    CHECK(ps::Engine::chancePlays(5, 1, 9, 0));     // 100 %: always
    CHECK(!ps::Engine::chancePlays(5, 1, 9, 100));  // 0 %: never

    auto make = [](float chance, int seed) {
        auto e = std::make_unique<ps::Engine>(kRate);
        e->post({ps::CommandType::SetTempo, 120.0f});
        e->post({ps::CommandType::SetDrumSeed, static_cast<float>(seed)});
        for (int s = 0; s < 16; ++s) {
            e->post({ps::CommandType::SetDrumHit, 1.0f, 4, s});
            e->post({ps::CommandType::SetDrumStepChance, chance, 4, s});
        }
        e->post({ps::CommandType::SetTransport, 1.0f, 0});
        return e;
    };
    auto a = make(0.5f, 1);
    auto b = make(0.5f, 1);
    auto c = make(0.5f, 2);
    const auto ha = hitsPerStep(*a, 4, 256);
    const auto hb = hitsPerStep(*b, 4, 256);
    const auto hc = hitsPerStep(*c, 4, 256);
    int na = 0, differ = 0;
    for (size_t i = 0; i < ha.size(); ++i) {
        na += ha[i];
        if (ha[i] != hc[i]) ++differ;
    }
    std::printf("  chance 50 %%: %d of 256 hits played, another seed changes %d steps\n", na, differ);
    CHECK(ha == hb);                 // the same seed: the same passes
    CHECK(na > 100 && na < 156);     // about half (binomial: 128 +- 3.5 sd)
    CHECK(differ > 60);              // another seed: another draw
    // Each pass of the loop is its own draw: the 16 bars are not all the same.
    int sameBars = 0;
    for (int bar = 1; bar < 16; ++bar) {
        if (std::equal(ha.begin(), ha.begin() + 16, ha.begin() + bar * 16)) ++sameBars;
    }
    CHECK(sameBars < 3);

    auto always = make(1.0f, 1);
    auto never = make(0.0f, 1);
    int nAlways = 0, nNever = 0;
    for (int h : hitsPerStep(*always, 4, 64)) nAlways += h;
    for (int h : hitsPerStep(*never, 4, 64)) nNever += h;
    CHECK(nAlways == 64 && nNever == 0);

    // A new hit on a step forgets the chance it had (always plays until told otherwise).
    never->post({ps::CommandType::SetDrumHit, 1.0f, 4, 0});
    int again = 0;
    for (int h : hitsPerStep(*never, 4, 16)) again += h;
    CHECK(again == 1);
}

void testChancesFollowTheSongPosition() {
    // Started from song step 32 (a later part), each step draws what the whole song draws there:
    // what is heard from a part is what the export has.
    auto make = [](int songStep) {
        auto e = std::make_unique<ps::Engine>(kRate);
        e->post({ps::CommandType::SetTempo, 120.0f});
        for (int s = 0; s < 16; ++s) {
            e->post({ps::CommandType::SetDrumHit, 1.0f, 6, s});
            e->post({ps::CommandType::SetDrumStepChance, 0.4f, 6, s});
        }
        e->post({ps::CommandType::SetTransport, 1.0f, 0, songStep});
        return e;
    };
    auto whole = make(0);
    auto later = make(32);
    const auto hw = hitsPerStep(*whole, 6, 96);
    const auto hl = hitsPerStep(*later, 6, 64);
    CHECK(std::equal(hl.begin(), hl.end(), hw.begin() + 32));

    // The offline export draws the same (two renders, byte for byte).
    auto offline = []() {
        ps::OfflineRenderer r(kRate);
        r.post({ps::CommandType::SetTempo, 120.0f});
        r.post({ps::CommandType::SetDrumSeed, 9.0f});
        for (int s = 0; s < 16; ++s) {
            r.post({ps::CommandType::SetDrumHit, 1.0f, 2, s});
            r.post({ps::CommandType::SetDrumStepChance, 0.5f, 2, s});
        }
        r.post({ps::CommandType::SetDrumLaneSteps, 7.0f, 2});
        return r.renderToBuffer(int64_t(kStep) * 64, 0);
    };
    const auto x = offline();
    const auto y = offline();
    CHECK(!x.empty() && x == y);
    bool finite = true;
    float peak = 0.0f;
    for (float v : x) {
        if (!std::isfinite(v)) finite = false;
        peak = std::max(peak, std::fabs(v));
    }
    CHECK(finite && peak > 0.01f && peak < 1.0f);
}

void testPolyrhythmAndChancesAllocateNothing() {
#ifdef PS_COUNT_ALLOCATIONS
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 140.0f});
    for (int lane = 0; lane < 16; ++lane) {
        for (int s = 0; s < 64; s += 1 + lane % 3) {
            engine.post({ps::CommandType::SetDrumHit, 0.9f, lane, s | ((lane % 4 + 1) << 16)});
            engine.post({ps::CommandType::SetDrumStepChance, 0.3f + 0.04f * static_cast<float>(lane), lane, s});
        }
        engine.post({ps::CommandType::SetDrumLaneSteps, static_cast<float>(3 + lane), lane});
    }
    engine.post({ps::CommandType::SetPatternSteps, 0.0f, 64});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(512 * 2);
    for (int i = 0; i < 50; ++i) engine.process(nullptr, 0, 0, out.data(), 512, 2);  // warm-up
    g_allocations = 0;
    g_countAllocations = true;
    for (int i = 0; i < 400; ++i) {
        if (i % 40 == 0) {
            // A waiting scene with other lengths and chances, swapped at the next bar.
            engine.post({ps::CommandType::SetStaging, 1.0f});
            for (int lane = 0; lane < 16; ++lane) {
                engine.post({ps::CommandType::SetDrumLaneSteps, static_cast<float>((i / 40 + lane) % 17), lane});
                engine.post({ps::CommandType::SetDrumStepChance, 0.5f, lane, (i / 40) % 64});
            }
            engine.post({ps::CommandType::SetStaging, 0.0f});
            engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
        }
        if (i % 25 == 0) engine.post({ps::CommandType::SetDrumSeed, static_cast<float>(i)});
        if (i % 30 == 0) engine.post({ps::CommandType::SetDrumLaneSteps, static_cast<float>(i % 9), 5});
        engine.process(nullptr, 0, 0, out.data(), 512, 2);
    }
    g_countAllocations = false;
    std::printf("  allocations with polyrhythm and chances: %d\n", g_allocations.load());
    CHECK(g_allocations.load() == 0);
    CHECK(!engine.hasNonFinite());
#endif
}

}  // namespace phase37_tests

#define PS_PHASE37_TESTS                                                                              \
    {"p37_lanes_loop_on_their_own_length", phase37_tests::testLanesLoopOnTheirOwnLength},              \
    {"p37_lane_length_sample_accurate", phase37_tests::testLaneLengthIsSampleAccurate},                \
    {"p37_staged_lane_lengths", phase37_tests::testWaitingSceneBringsItsLaneLengths},                  \
    {"p37_chances_deterministic", phase37_tests::testChancesAreDrawnTheSameEveryTime},                 \
    {"p37_chances_follow_the_song", phase37_tests::testChancesFollowTheSongPosition},                  \
    {"p37_no_alloc", phase37_tests::testPolyrhythmAndChancesAllocateNothing},
