// Audit stage 2, point 5 (« contrats interface ↔ moteur »): every engine command the interface sends
// has a test proving what it does to the sound (AppContractsTest, on the Kotlin side, fails when a
// CommandType is in no host test). These four were only tested on their parts (AcidTrack, the
// metronome, the meter), never through Engine::process as the app sends them.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace contract_tests {

float peakOf(const std::vector<float>& v, size_t from = 0, size_t to = SIZE_MAX) {
    float p = 0.0f;
    for (size_t i = from; i < std::min(to, v.size()); ++i) p = std::max(p, std::fabs(v[i]));
    return p;
}

int crossingsOf(const std::vector<float>& v) {
    int c = 0;
    for (size_t i = 1; i < v.size() / kChannels; ++i) {
        if ((v[(i - 1) * kChannels] < 0.0f) != (v[i * kChannels] < 0.0f)) ++c;
    }
    return c;
}

// A key played live on the 303 (MIDI keyboard, Live « Acid 303 » keys) sounds with the transport
// stopped; let go, it dies away.
void testAcidLiveNoteThroughTheEngine() {
    ps::Engine engine(kRate);
    const auto silent = render(engine, kRate / 4, 256);
    engine.post({ps::CommandType::AcidLiveNote, 0.9f, 48});
    const auto held = render(engine, kRate / 2, 256);
    engine.post({ps::CommandType::AcidLiveNote, 0.0f, -1});
    render(engine, kRate * 2, 256);
    const auto after = render(engine, kRate / 4, 256);
    std::printf("  303 live key: silent %.4f, held %.3f, let go %.5f\n", peakOf(silent), peakOf(held), peakOf(after));
    CHECK(peakOf(silent) < 1e-6f);
    CHECK(peakOf(held) > 0.02f);
    CHECK(peakOf(after) < 0.005f);
}

// The 303's root (the song's key): an octave up doubles the line's pitch.
void testAcidRootTransposesTheLine() {
    auto run = [](int root) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 120.0f});
        for (int i = 0; i < 16; ++i) engine.post({ps::CommandType::SetAcidStep, 0.0f, i, note(0).pack()});
        engine.post({ps::CommandType::SetAcidParam, 1.0f, ps::AcidTrack::Cutoff});
        engine.post({ps::CommandType::SetAcidParam, 0.0f, ps::AcidTrack::EnvMod});
        engine.post({ps::CommandType::SetAcidRoot, 0.0f, root});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        return render(engine, kRate, 256);
    };
    const auto low = run(36), high = run(48);
    const int cl = crossingsOf(low), ch = crossingsOf(high);
    std::printf("  303 root 36: %d crossings, root 48: %d\n", cl, ch);
    CHECK(peakOf(low) > 0.02f && peakOf(high) > 0.02f);
    CHECK(ch > cl * 3 / 2);
}

// « Volume du métronome » (Réglages): the click follows the level.
void testMetronomeLevelSetsTheClick() {
    auto run = [](float level) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 120.0f});
        engine.post({ps::CommandType::SetMetronome, 1.0f});
        engine.post({ps::CommandType::SetMetronomeLevel, level});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        return peakOf(render(engine, kRate, 256));
    };
    const float quiet = run(0.1f), loud = run(1.0f), off = run(0.0f);
    std::printf("  metronome level 0: %.4f, 0.1: %.3f, 1: %.3f\n", off, quiet, loud);
    CHECK(loud > quiet * 2.0f);
    CHECK(quiet > 0.001f);
    CHECK(off < quiet);
}

// « Annuler » during the latency test: the meter stops and its test burst is no longer played.
void testCancelLatencyTestStopsTheBurst() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::StartLatencyTest, 0.0f});
    std::vector<float> in(256, 0.0f), out(256 * kChannels, 0.0f);
    bool running = false;
    for (int i = 0; i < 400 && !running; ++i) {
        engine.process(in.data(), 256, 1, out.data(), 256, kChannels);
        running = engine.latencyMeter().state() == ps::RoundTripMeter::State::Running;
    }
    CHECK(running);
    engine.post({ps::CommandType::CancelLatencyTest, 0.0f});
    float peak = 0.0f;
    for (int i = 0; i < 200; ++i) {
        std::fill(out.begin(), out.end(), 0.0f);
        engine.process(in.data(), 256, 1, out.data(), 256, kChannels);
        if (i > 4) peak = std::max(peak, peakOf(out));
    }
    std::printf("  latency test cancelled: state %d, output after %.5f\n", static_cast<int>(engine.latencyMeter().state()), peak);
    CHECK(engine.latencyMeter().state() == ps::RoundTripMeter::State::Idle);
    CHECK(peak < 1e-4f);
}

}  // namespace contract_tests

#define PS_CONTRACT_TESTS                                                                \
    {"contract_acid_live_note", contract_tests::testAcidLiveNoteThroughTheEngine},       \
    {"contract_acid_root", contract_tests::testAcidRootTransposesTheLine},               \
    {"contract_metronome_level", contract_tests::testMetronomeLevelSetsTheClick},        \
    {"contract_cancel_latency_test", contract_tests::testCancelLatencyTestStopsTheBurst},
