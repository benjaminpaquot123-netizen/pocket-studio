// Tour 5, Hard techno (docs/audits/ultra/tour-5-hardtechno-rapport.md): « Tout tester » on Benjamin's
// phone counted 25 underruns in Hard techno while its « pire bloc » read 67 %. That reading is the
// engine's load smoothed over half a second and capped at 4: one callback twenty times too slow moves
// it by a few percent, so the phone could not say whether the callback itself stalled or started
// late. These tests hold the two readings the engine now keeps for that: the worst block and the
// longest wait between two callbacks, neither smoothed nor capped.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace hardtechno {

constexpr float kBurst = 96.0f / kRate;  // a 96-frame callback lasts 2 ms

// The stall the phone heard (13 underruns in a row, about 26 ms), inside one callback among normal ones.
void testWorstBlockSeesOneSlowCallback() {
    ps::CallbackLoad load;
    double t = 100.0;
    for (int i = 0; i < 2000; ++i) {
        const float took = (i == 1200 ? 13.0f : 0.45f) * kBurst;
        load.add(took, kBurst, t);
        t += std::max(double(took), double(kBurst));
    }
    const float smoothed = load.smoothed(), worst = load.takeWorstBlock();
    std::printf("  one callback 13x its time among 0.45x ones: smoothed %.2f, worst block %.2f\n", smoothed, worst);
    CHECK(worst >= 12.9f && worst <= 13.1f);  // the stall itself, neither smoothed nor capped at 4
    CHECK(smoothed < 0.6f);                   // the smoothed load (the « charge ») hides it, as on the phone
    // A take starts a new window: the next callbacks only.
    for (int i = 0; i < 50; ++i, t += kBurst) load.add(0.5f * kBurst, kBurst, t);
    const float next = load.takeWorstBlock();
    CHECK(next >= 0.49f && next <= 0.51f);
    CHECK(load.takeWorstBlock() == 0.0f);  // nothing since: nothing to report
}

// A callback started 30 ms late (the thread kept off its core) while every callback itself is quick:
// the gap shows it, the worst block does not, so the two causes are told apart. A stream opened again
// (a pause of seconds) is no late callback.
void testWorstGapSeesALateCallback() {
    ps::CallbackLoad load;
    double t = 50.0;
    for (int i = 0; i < 1000; ++i) {
        if (i == 600) t += 0.030;
        load.add(0.4f * kBurst, kBurst, t);
        t += kBurst;
    }
    const float gap = load.takeWorstGap(), block = load.takeWorstBlock();
    std::printf("  a callback 30 ms late: worst gap %.1f callbacks, worst block %.2f\n", gap, block);
    CHECK(gap >= 15.9f && gap <= 16.1f);  // 2 ms + 30 ms late over 2 ms
    CHECK(block < 0.5f);
    t += 3.0;  // the stream stopped and opened again
    for (int i = 0; i < 10; ++i, t += kBurst) load.add(0.4f * kBurst, kBurst, t);
    const float after = load.takeWorstGap();
    CHECK(after >= 0.99f && after <= 1.01f);
}

// Unequal blocks (tour 9, C3: four styles read « 4.1 to 4.6 blocks » on a tablet). A device that asks
// every 2 ms for a small block then a large one starves nobody (192 frames every 4 ms), yet the wait
// after the small block, over that small block's duration, read as six blocks.
void testWorstGapWithUnequalBlocksOnTime() {
    ps::CallbackLoad load;
    const float small = 16.0f / kRate, large = 176.0f / kRate;
    double t = 50.0;
    for (int i = 0; i < 2000; ++i, t += kBurst) load.add(0.2f * kBurst, i % 2 ? large : small, t);
    const float gap = load.takeWorstGap();
    std::printf("  16/176 frames asked every 2 ms, none late: worst gap %.2f blocks\n", gap);
    CHECK(gap >= 1.0f && gap < 2.0f);
}

// The same 6 ms of lateness reads the same whichever block came before it (it read 19 blocks after
// the small one and 2.6 after the large one).
void testWorstGapSameLatenessSameReading() {
    const float small = 16.0f / kRate, large = 176.0f / kRate;
    float seen[2] = {0.0f, 0.0f};
    for (int which = 0; which < 2; ++which) {
        ps::CallbackLoad load;
        double t = 50.0;
        for (int i = 0; i < 2000; ++i) {
            const float lasted = i % 2 ? large : small;
            if (i == 1200 + which) t += 0.006;  // which 0: after a large block; 1: after a small one
            load.add(0.2f * lasted, lasted, t);
            t += lasted;  // each callback starts when the frames before it are played
        }
        seen[which] = load.takeWorstGap();
    }
    std::printf("  6 ms late after a large block: %.2f blocks; after a small one: %.2f\n", seen[0], seen[1]);
    CHECK(seen[0] > 1.5f && seen[1] > 1.5f);
    CHECK(std::fabs(seen[0] - seen[1]) < 0.05f * seen[0]);
}

// The engine keeps both while it renders: real callbacks, measured from outside too.
void testEngineReportsItsWorstBlock() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(96 * kChannels);
    double outside = 0.0;
    for (int i = 0; i < 1000; ++i) {
        const auto t0 = std::chrono::steady_clock::now();
        engine.process(nullptr, 0, 0, out.data(), 96, kChannels);
        outside = std::max(outside, std::chrono::duration<double>(std::chrono::steady_clock::now() - t0).count() / kBurst);
    }
    const float worst = engine.takeWorstBlock(), gap = engine.takeWorstGap();
    std::printf("  engine: worst block %.3f (measured outside %.3f), smoothed %.3f, worst gap %.2f\n", worst, outside, engine.cpuLoad(), gap);
    CHECK(worst > 0.0f && worst <= outside * 1.01 + 1e-4);  // inside process(): never more than its whole call
    CHECK(worst >= engine.cpuLoad());                      // the worst is at least the average
    CHECK(gap > 0.0f);
    CHECK(engine.takeWorstBlock() == 0.0f);
}

}  // namespace hardtechno

#define PS_HARDTECHNO_TESTS                                                        \
    {"ht_worst_block_sees_one_slow_callback", hardtechno::testWorstBlockSeesOneSlowCallback}, \
    {"ht_worst_gap_sees_a_late_callback", hardtechno::testWorstGapSeesALateCallback},       \
    {"ht_worst_gap_unequal_blocks_on_time", hardtechno::testWorstGapWithUnequalBlocksOnTime}, \
    {"ht_worst_gap_same_lateness_same_reading", hardtechno::testWorstGapSameLatenessSameReading}, \
    {"ht_engine_reports_its_worst_block", hardtechno::testEngineReportsItsWorstBlock},
