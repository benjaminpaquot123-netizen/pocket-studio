// Step 4f (Benjamin's n° 11): a controller's pad hit softly starts its live pad soft. The velocity
// rides on the ToggleClip command (its value; 0 from older callers: full), kept per pass on the
// audio thread, nothing allocated. Included by engine_tests.cpp.

namespace live4f {

// The loudest frame of live pad 0 (a DC loop of 0.5, 1 beat) started with `velocity`.
static float peakOfPadStartedAt(float velocity) {
    ps::Engine engine(kRate);
    std::vector<float> dc(24000, 0.5f);
    ps::Command clip{ps::CommandType::InstallClip, 0.0f, 0};
    clip.sample = ps::Sample::create(dc.data(), int64_t(dc.size()), 1, kRate, kRate);
    engine.post(clip);
    engine.post({ps::CommandType::SetClipParam, 1.0f, 0, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::ToggleClip, velocity, 0});
    std::vector<float> out(4096 * 2);
    float peak = 0.0f;
    for (int b = 0; b < 12; ++b) {  // a second: the loop starts and goes round
        engine.process(nullptr, 0, 0, out.data(), 4096, 2);
        for (float v : out) peak = std::max(peak, std::fabs(v));
    }
    engine.collectGarbage();
    return peak;
}

void testControllerVelocityReachesTheLivePad() {
    const float full = peakOfPadStartedAt(0.0f);  // the screen's tap (value 0: full, as before)
    const float one = peakOfPadStartedAt(1.0f);
    const float soft = peakOfPadStartedAt(0.5f);
    std::printf("  live pad peak: tap %.3f, velocity 1 %.3f, velocity 0.5 %.3f\n", double(full), double(one), double(soft));
    CHECK(full > 0.05f);
    CHECK(std::fabs(full - one) < 1e-4f);
    CHECK(std::fabs(soft / full - 0.5f) < 0.02f);
    // No allocation on the audio thread while a soft pad starts and loops.
    ps::Engine engine(kRate);
    std::vector<float> dc(24000, 0.5f);
    ps::Command clip{ps::CommandType::InstallClip, 0.0f, 0};
    clip.sample = ps::Sample::create(dc.data(), int64_t(dc.size()), 1, kRate, kRate);
    engine.post(clip);
    std::vector<float> out(256 * 2);
    engine.process(nullptr, 0, 0, out.data(), 256, 2);
    engine.post({ps::CommandType::ToggleClip, 0.3f, 0});
    g_allocations = 0;
    g_countAllocations = true;
    for (int b = 0; b < 200; ++b) engine.process(nullptr, 0, 0, out.data(), 256, 2);
    g_countAllocations = false;
    CHECK(g_allocations.load() == 0);
    engine.collectGarbage();
}

}  // namespace live4f

#define PS_LIVE4F_TESTS \
    {"live4f_controller_velocity_reaches_the_live_pad", live4f::testControllerVelocityReachesTheLivePad},
