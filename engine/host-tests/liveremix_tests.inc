// Lot D2 (« case Remix du Live »): a cell of the Live grid's remix column holds a passage of the
// separated song (a stem or the whole song, whole bars, stretched to the tempo off the audio
// thread). Launched on the bar like the other cells, sample-accurate; the whole song makes way for
// it; a tempo change never leaves a gap nor a click (the copy playing is read faster until its
// refit, picked up at the same place); a song part gives the slot its passage; nothing allocates.
// Included by engine_tests.cpp.

namespace liveremix {

constexpr int kBar = 96000;  // 120 BPM: a bar of 4/4 in frames

// A passage of `frames` frames, stereo: a sine of `hz` at `level` (in phase with frame 0), and a
// click of `click` on its first frame when > 0.
static ps::Sample* passage(int frames, float hz, float level, float click = 0.0f) {
    std::vector<float> d(static_cast<size_t>(frames) * 2);
    for (int i = 0; i < frames; ++i) {
        const float v = level * std::sin(2.0f * 3.14159265358979f * hz * static_cast<float>(i) / kRate);
        d[static_cast<size_t>(i) * 2] = d[static_cast<size_t>(i) * 2 + 1] = v;
    }
    if (click > 0.0f) d[0] = d[1] = click;
    return ps::Sample::create(d.data(), frames, 2, kRate, kRate);
}

static void installCell(ps::Engine& engine, int cell, ps::Sample* s, int loopSteps, bool refit = false) {
    ps::Command c{ps::CommandType::InstallRemixCell, 0.0f, cell | (refit ? ps::kRefit : 0), loopSteps};
    c.sample = s;  // the engine owns this reference
    engine.post(c);
}

// The remix slot staged with cell `cell`, then armed on the next bar (as the Live page does it).
static void launchCell(ps::Engine& engine, int cell, int tag) {
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetRemixCell, 0.0f, cell});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmLaunch, static_cast<float>(tag), 1 << ps::kSlotRemix, 16});
}

static size_t firstAbove(const std::vector<float>& out, float level, size_t from = 0) {
    for (size_t i = from; i < out.size() / 2; ++i) {
        if (std::fabs(out[i * 2]) > level) return i;
    }
    return out.size();
}

static float rms(const std::vector<float>& out, size_t from, size_t frames) {
    double sum = 0.0;
    for (size_t i = from; i < from + frames && i < out.size() / 2; ++i) sum += double(out[i * 2]) * out[i * 2];
    return static_cast<float>(std::sqrt(sum / static_cast<double>(std::max<size_t>(1, frames))));
}

// The passage starts on the bar line, to the frame, and loops on its bars; the whole song, heard
// before, makes way for it; a cell set to none (-1) gives the song back.
static void startsOnTheBar() {
    ps::Engine engine(kRate);
    auto* song = ps::StemBundle::fromFloat(std::vector<std::vector<float>>(4, [] {
        std::vector<float> v(static_cast<size_t>(kRate) * 20 * 2);
        for (size_t i = 0; i < v.size() / 2; ++i) v[i * 2] = v[i * 2 + 1] = 0.05f * std::sin(2.0f * 3.14159265f * 200.0f * float(i) / kRate);
        return v;
    }()));
    ps::Command stems{ps::CommandType::InstallStems};
    stems.stems = song;
    engine.post(stems);
    // Brick C's passage: one bar, a click on its first frame, nothing else.
    installCell(engine, 2, passage(kBar, 375.0f, 0.0f, 0.5f), 16);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto before = render(engine, 6 * 6000, 256);  // step 6
    CHECK(rms(before, 12000, 12000) > 0.05f);    // the whole song plays
    CHECK(engine.remixCellPlaying() == -1);
    launchCell(engine, 2, 2);
    auto out = render(engine, 3 * kBar, 256);    // steps 6 .. 54
    const int64_t base = 6 * 6000;
    // On the bar line (step 16, frame 96000): the click, to the frame; again a bar later.
    const size_t first = firstAbove(out, 0.3f);
    std::printf("  remix cell: first click at %zu (bar %d + latency %d)\n", static_cast<size_t>(base + first), kBar, kLate);
    CHECK(static_cast<int64_t>(first) + base == kBar + kLate);
    CHECK(static_cast<int64_t>(firstAbove(out, 0.3f, first + 1)) + base == 2 * kBar + kLate);
    CHECK(engine.remixCellPlaying() == 2);
    CHECK(engine.slotTag(ps::kSlotRemix) == 2 && engine.slotStart(ps::kSlotRemix) == 16);
    // The song made way: silence between the clicks (its 5 ms fade done).
    CHECK(rms(out, static_cast<size_t>(kBar - base + kLate + 2000), 40000) < 1e-4f);
    // None: the whole song again, at once.
    engine.post({ps::CommandType::SetRemixCell, 0.0f, -1});
    auto back = render(engine, kBar / 2, 256);
    CHECK(engine.remixCellPlaying() == -1);
    CHECK(rms(back, 4000, 20000) > 0.02f);
    // A scene swap whose part gives the slot a passage: on the swap's bar line.
    installCell(engine, 5, passage(2 * kBar, 375.0f, 0.0f, 0.5f), 32);
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetRemixCell, 0.0f, 5});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    const int64_t at = 3 * kBar + kBar / 2 + base;  // frames rendered so far
    auto swap = render(engine, 2 * kBar, 256);
    const int64_t nextBar = (at / kBar + 1) * kBar;
    CHECK(static_cast<int64_t>(firstAbove(swap, 0.3f)) + at == nextBar + kLate);
    CHECK(engine.remixCellPlaying() == 5);
    // The transport stops: silent; started again on a scene with no passage (sent at once): the song.
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 2000, 256);
    CHECK(engine.remixCellPlaying() == -1);
    engine.post({ps::CommandType::SetRemixCell, 0.0f, -1});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto again = render(engine, kBar / 2, 256);
    CHECK(engine.remixCellPlaying() == -1 && rms(again, 4000, 20000) > 0.02f);
}

// A tempo change while the passage plays: never a gap nor a click, before its refit (read faster)
// and through it (the copy for the new tempo picked up at the same place, cross-faded); still
// on the bar after.
static void tempoChangeWithoutClickOrGap() {
    ps::Engine engine(kRate);
    // 375 Hz: 750 whole cycles in a bar at 120 BPM, 720 in a bar at 125 BPM (92160 frames): the
    // loop goes round without a step in the sine at either tempo.
    installCell(engine, 0, passage(kBar, 375.0f, 0.3f), 16);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    launchCell(engine, 0, 0);
    std::vector<float> out;
    auto add = [&](int frames, int block) {
        auto o = render(engine, frames, block);
        out.insert(out.end(), o.begin(), o.end());
    };
    add(kBar + kBar / 2, 128);  // launched on step 0 (armed while stopped), half a bar played
    CHECK(engine.remixCellPlaying() == 0);
    const size_t change = out.size() / 2;
    engine.post({ps::CommandType::SetTempo, 125.0f});
    add(kBar, 64);  // the copy for 120 BPM read 4 % faster
    const size_t refit = out.size() / 2;
    installCell(engine, 0, passage(92160, 375.0f, 0.3f), 16, true);
    add(kBar, 128);
    CHECK(engine.remixCellPlaying() == 0);
    // From half a bar before the change to the end: every 10 ms heard, no step between two frames
    // larger than the sine's own (0.3 x 2 pi x 375 / 48000 x 1.042 = 0.0153, the strip's gain on it).
    float worstGap = 1.0f, worstStep = 0.0f;
    for (size_t i = change - kBar / 2; i + 480 <= out.size() / 2; i += 480) worstGap = std::min(worstGap, rms(out, i, 480));
    for (size_t i = change - kBar / 2; i < out.size() / 2; ++i) worstStep = std::max(worstStep, std::fabs(out[i * 2] - out[(i - 1) * 2]));
    std::printf("  remix cell tempo change: quietest 10 ms %.3f, largest step %.4f (refit at %zu)\n", worstGap, worstStep, refit);
    CHECK(worstGap > 0.12f);
    CHECK(worstStep < 0.03f);
    CHECK(finiteAndUnclipped(out));
}

// Launches, switches, refits, mutes, tempo changes and stops: no allocation on the audio thread.
static void withoutAllocating() {
    ps::Engine engine(kRate);
    std::vector<ps::Sample*> copies;
    for (int c = 0; c < 4; ++c) installCell(engine, c, passage(kBar * (1 + c % 2), 300.0f + 50.0f * c, 0.1f), 16 * (1 + c % 2));
    for (int c = 0; c < 8; ++c) copies.push_back(passage(92160 * (1 + c % 2), 300.0f, 0.1f));
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(256 * 2);
    for (int i = 0; i < 20; ++i) engine.process(nullptr, 0, 0, out.data(), 256, 2);  // warm-up
    g_allocations = 0;
    g_countAllocations = true;
    size_t next = 0;
    for (int i = 0; i < 3000; ++i) {
        if (i % 150 == 0) launchCell(engine, (i / 150) % 4, i / 150);
        if (i % 150 == 75) engine.post({ps::CommandType::SetRemixCell, 0.0f, (i / 150 + 1) % 4});
        if (i % 400 == 200) engine.post({ps::CommandType::SetTempo, (i / 400) % 2 ? 120.0f : 125.0f});
        if (i % 400 == 300 && next < copies.size()) {
            const int c = static_cast<int>(next % 2);
            installCell(engine, c, copies[next++], 16 * (1 + c), true);
        }
        if (i % 700 == 350) engine.post({ps::CommandType::SetRemixOn, 0.0f});
        if (i % 700 == 400) engine.post({ps::CommandType::SetRemixOn, 1.0f});
        if (i == 2000) engine.post({ps::CommandType::SetTransport, 0.0f});
        if (i == 2100) engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        engine.process(nullptr, 0, 0, out.data(), 256, 2);
        CHECK(finiteAndUnclipped(out));
    }
    g_countAllocations = false;
    std::printf("  remix cells: %d allocations\n", g_allocations.load());
    CHECK(g_allocations.load() == 0);
    CHECK(!engine.hasNonFinite());
    engine.collectGarbage();
}

}  // namespace liveremix

#define PS_LIVEREMIX_TESTS \
    {"liveremix_starts_on_the_bar", liveremix::startsOnTheBar}, \
    {"liveremix_tempo_change_without_click_or_gap", liveremix::tempoChangeWithoutClickOrGap}, \
    {"liveremix_without_allocating", liveremix::withoutAllocating},
