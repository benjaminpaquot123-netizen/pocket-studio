// Pad Live (n° 25, lot B1): four pages of 16 live pads (A to D), all live at once: a pad of page D
// plays on the beat like one of page A, a loop of page A keeps going while page D plays, and the
// 64 slots render without allocating. Included by engine_tests.cpp.

namespace padlive {

static void pageDPlaysWithPageA() {
    ps::Engine engine(kRate);  // 120 BPM: a beat is 24000 frames
    CHECK(ps::ClipLauncher::kSlots == 64);
    // Page A, pad 1: a loop of 1 beat; page D, last pad: a one-shot.
    installClip(engine, 0, constantSample(100, 1, 0.25f));
    engine.post({ps::CommandType::SetClipParam, 1.0f, 0, ps::ClipLauncher::Beats});
    installClip(engine, 63, constantSample(2000, 1, 0.3f));
    engine.post({ps::CommandType::SetClipParam, 1.0f, 63, ps::ClipLauncher::OneShot});
    engine.post({ps::CommandType::SetClipParam, 1.0f, 63, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    render(engine, 6000, 256);  // a quarter into beat 0
    engine.post({ps::CommandType::ToggleClip, 0.0f, 63});
    render(engine, 256, 256);
    CHECK(engine.clipState(63) == ps::ClipLauncher::Waiting);
    auto out = render(engine, 24000 - 6256 + 1000, 256);  // past beat 1
    CHECK(engine.clipState(63) == ps::ClipLauncher::Playing);
    CHECK(engine.clipState(0) == ps::ClipLauncher::Playing);
    // On beat 1: the loop's click and the hit together.
    const size_t at = static_cast<size_t>(24000 - 6256 + 3 + kLate) * 2;
    CHECK(std::fabs(out[at] - 0.55f) < 1e-3f);
    render(engine, 24000, 256);
    CHECK(engine.clipState(63) == ps::ClipLauncher::Off);  // a one-shot ends by itself
    CHECK(engine.clipState(0) == ps::ClipLauncher::Playing);
    // Out of range: ignored.
    engine.post({ps::CommandType::ToggleClip, 0.0f, 64});
    render(engine, 512, 256);
    CHECK(engine.clipState(0) == ps::ClipLauncher::Playing);
}

static void sixtyFourPadsWithoutAllocating() {
    ps::Engine engine(kRate);
    for (int pad = 0; pad < ps::ClipLauncher::kSlots; ++pad) {
        installClip(engine, pad, constantSample(4000, 2, 0.002f));
        engine.post({ps::CommandType::SetClipParam, float(1 + pad % 4), pad, ps::ClipLauncher::Beats});
    }
    std::vector<float> out(256 * 2);
    for (int i = 0; i < 20; ++i) engine.process(nullptr, 0, 0, out.data(), 256, 2);  // warm-up
    g_allocations = 0;
    g_countAllocations = true;
    for (int i = 0; i < 400; ++i) {
        if (i % 5 == 0) engine.post({ps::CommandType::ToggleClip, 1.0f, (i / 5) % ps::ClipLauncher::kSlots});
        engine.process(nullptr, 0, 0, out.data(), 256, 2);
    }
    g_countAllocations = false;
    CHECK(g_allocations.load() == 0);
    CHECK(finiteAndUnclipped(out));
}

}  // namespace padlive

#define PS_PADLIVE_TESTS \
    {"padlive_page_d_plays_with_page_a", padlive::pageDPlaysWithPageA}, \
    {"padlive_64_pads_without_allocating", padlive::sixtyFourPadsWithoutAllocating},
