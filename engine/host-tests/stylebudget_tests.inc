// Tour 9, B1: the load budget per style is relative. These tests hold what the live bench cannot
// (it has the songs, the engine tests do not): the verdict, the table, and a reference scene steady
// enough to divide by. Included by engine_tests.cpp.

#include <set>
#include <string>

#include "../bench/StyleBudgets.h"

namespace stylebudget_tests {

namespace sb = ps::stylebudget;

// A style inside its budget passes, one past the margin does not, and the verdict is the same on
// a machine ten times slower: only the ratio counts.
void testVerdictReadsTheRatioNotTheSpeed() {
    const sb::Budget* b = sb::budgetOf("techno");
    CHECK(b != nullptr);
    if (!b) return;
    const double limit = static_cast<double>(b->measured * sb::kMargin);
    for (double reference : {0.004, 0.04, 0.4}) {
        CHECK(sb::judge("techno", reference * b->measured, reference) == sb::Verdict::Within);
        CHECK(sb::judge("techno", reference * limit * 0.98, reference) == sb::Verdict::Within);
        CHECK(sb::judge("techno", reference * limit * 1.02, reference) == sb::Verdict::Over);
        CHECK(sb::judge("techno", reference * limit * 3.0, reference) == sb::Verdict::Over);
    }
}

// No budget and no measure are said as such, never as "within".
void testVerdictSaysWhenItCannotJudge() {
    CHECK(sb::judge("no_such_style", 0.1, 0.1) == sb::Verdict::NoBudget);
    CHECK(sb::judge("", 0.1, 0.1) == sb::Verdict::NoBudget);
    CHECK(sb::judge("techno", 0.0, 0.1) == sb::Verdict::NoMeasure);
    CHECK(sb::judge("techno", 0.1, 0.0) == sb::Verdict::NoMeasure);
    CHECK(sb::judge("techno", -1.0, 0.1) == sb::Verdict::NoMeasure);
}

// The table: the 37 styles of the app, each once, each with a plausible ratio, and a margin that
// leaves room for another processor without hiding a brick that doubles.
void testTableCoversEveryStyleOnce() {
    static const char* const kStyles[] = {
        "acid_house", "acid_techno", "acidcore", "afro_house", "afrobeats", "amapiano", "boom_bap",
        "deep_house", "detroit", "disco", "dnb", "drill", "dubstep", "gabber", "grime", "hard_techno",
        "house", "industrial_techno", "jersey_club", "jump_up", "jungle", "liquid_dnb", "lofi",
        "minimal", "neurofunk", "phonk", "pop", "reggaeton", "retro_techno", "riddim", "speed_garage",
        "synthwave", "tech_house", "techno", "trance", "trap", "uk_garage",
    };
    std::set<std::string> seen;
    for (const sb::Budget& b : sb::kBudgets) {
        CHECK(seen.insert(b.style).second);
        CHECK(b.measured > 0.3f && b.measured < 4.0f);
        CHECK(sb::budgetOf(b.style) == &b);
    }
    CHECK(seen.size() == 37);
    for (const char* style : kStyles) CHECK(seen.count(style) == 1);
    CHECK(sb::kMargin > 1.2f && sb::kMargin < 2.0f);
}

// The reference scene makes sound and costs something, and two measures of the same pass agree:
// a ratio over a reference that wanders would mean nothing.
void testReferenceSceneIsSteadyInOnePass() {
    const double a = sb::referenceLoad();
    const double b = sb::referenceLoad();
    CHECK(a > 0.0 && b > 0.0);
    const double spread = std::max(a, b) / std::min(a, b);
    std::printf("  reference load %.2f%% then %.2f%% (spread x%.2f)\n", 100.0 * a, 100.0 * b, spread);
    CHECK_TIMING(spread < 1.25);
    // The same scene against itself sits at a ratio of one, far inside the margin.
    CHECK_TIMING(spread < static_cast<double>(sb::kMargin));
}

// The reference is a real scene: it plays, and a heavier one costs more in the same pass (the
// measure follows the work, so a dearer brick moves a style's ratio).
void testReferenceFollowsTheWork() {
    ps::Engine engine(48000);
    engine.post({ps::CommandType::SetTempo, 140.0f});
    for (int lane = 0; lane < 16; ++lane) {
        for (int step = 0; step < 16; ++step) engine.post({ps::CommandType::SetDrumHit, 0.8f, lane, step});
    }
    for (int note : {45, 52, 57, 60, 64, 69}) engine.post({ps::CommandType::SynthNoteOn, 0.7f, note});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> block(96 * 2);
    float peak = 0.0f;
    bool finite = true;
    for (int done = 0; done < 48000; done += 96) {
        engine.process(nullptr, 0, 0, block.data(), 96, 2);
        for (float s : block) {
            finite = finite && std::isfinite(s);
            peak = std::max(peak, std::fabs(s));
        }
    }
    CHECK(finite);
    CHECK(peak > 0.05f);

    // An idle engine in the same pass: the reference must cost clearly more than silence.
    ps::Engine idle(48000);
    const auto cost = [&](ps::Engine& e) {
        double best = 1e9;
        for (int pass = 0; pass < 3; ++pass) {
            const double t0 = sb::threadCpuSeconds();
            for (int done = 0; done < 48000; done += 96) e.process(nullptr, 0, 0, block.data(), 96, 2);
            best = std::min(best, sb::threadCpuSeconds() - t0);
        }
        return best;
    };
    const double busy = cost(engine);
    const double quiet = cost(idle);
    std::printf("  reference %.2f%%, idle engine %.2f%%\n", 100.0 * busy, 100.0 * quiet);
    CHECK_TIMING(busy > quiet * 1.5);
}

}  // namespace stylebudget_tests

#define PS_STYLEBUDGET_TESTS                                                                      \
    {"sb_verdict_reads_the_ratio_not_the_speed", stylebudget_tests::testVerdictReadsTheRatioNotTheSpeed}, \
    {"sb_verdict_says_when_it_cannot_judge", stylebudget_tests::testVerdictSaysWhenItCannotJudge}, \
    {"sb_table_covers_every_style_once", stylebudget_tests::testTableCoversEveryStyleOnce},       \
    {"sb_reference_scene_is_steady_in_one_pass", stylebudget_tests::testReferenceSceneIsSteadyInOnePass}, \
    {"sb_reference_follows_the_work", stylebudget_tests::testReferenceFollowsTheWork},
