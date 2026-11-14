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
    {"ht_engine_reports_its_worst_block", hardtechno::testEngineReportsItsWorstBlock},
