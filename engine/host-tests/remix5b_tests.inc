// Step 5b (Remix in the project): the song's stems edited without touching their files: pieces cut,
// moved and faded on the song's timeline, heard live and in the export alike (the same player).
// Included by engine_tests.cpp.

namespace remix5b {

// Four stems `frames` long: stem 0's left channel a ramp 0 -> 0.5 (its value tells the frame
// heard), everything else silent.
static std::vector<std::vector<float>> rampStems(int64_t frames) {
    std::vector<std::vector<float>> s(4, std::vector<float>(size_t(frames) * 2, 0.0f));
    for (int64_t i = 0; i < frames; ++i) s[0][size_t(i) * 2] = 0.5f * float(i) / float(frames);
    return s;
}

static std::vector<float> play(ps::StemPlayer& player, int frames) {
    std::vector<float> l(size_t(frames), 0.0f), r(size_t(frames), 0.0f);
    for (int at = 0; at < frames; at += 256) {
        const int n = std::min(256, frames - at);
        player.render(l.data() + at, r.data() + at, n);
    }
    return l;
}

static void setClip(ps::StemPlayer& p, int stem, int clip, int64_t at, int64_t from, int64_t length, int fadeIn = 0, int fadeOut = 0) {
    p.setClipField(stem, clip, ps::StemPlayer::ClipAt, at);
    p.setClipField(stem, clip, ps::StemPlayer::ClipFrom, from);
    p.setClipField(stem, clip, ps::StemPlayer::ClipLength, length);
    p.setClipField(stem, clip, ps::StemPlayer::ClipFadeIn, fadeIn);
    p.setClipField(stem, clip, ps::StemPlayer::ClipFadeOut, fadeOut);
}

static void testStemPiecesCutMovedAndFaded() {
    constexpr int64_t n = 48000;
    auto ramp = [](int64_t frame, int64_t frames) { return 0.5f * float(frame) / float(frames); };
    ps::StemPlayer player;
    player.prepare(kRate);
    auto* song = ps::StemBundle::fromFloat(rampStems(n));
    song->retain();  // kept for the copy below
    player.install(song);
    // No edits: frame for frame.
    player.start(0);
    auto plain = play(player, 20000);
    CHECK(std::fabs(plain[10000] - ramp(10000, n)) < 2e-3f);
    // Cut at 12000, the second piece moved half a second later with a 0.1 s fade in.
    setClip(player, 0, 0, 0, 0, 12000);
    setClip(player, 0, 1, 24000, 12000, 12000, 4800);
    player.commitClips(0, 2);
    player.start(0);
    std::vector<float> edited(40000, 0.0f), right(40000, 0.0f);
    g_allocations = 0;
    g_countAllocations = true;
    for (int at = 0; at < 40000; at += 256) player.render(edited.data() + at, right.data() + at, std::min(256, 40000 - at));
    g_countAllocations = false;
    CHECK(g_allocations == 0);
    CHECK(std::fabs(edited[6000] - ramp(6000, n)) < 2e-3f);           // first piece as is
    CHECK(std::fabs(edited[18000]) < 1e-6f);                          // the gap left by the move
    CHECK(std::fabs(edited[24000 + 2400] - ramp(14400, n) * 2401.0f / 4800.0f) < 2e-3f);  // mid fade
    CHECK(std::fabs(edited[24000 + 8000] - ramp(20000, n)) < 2e-3f);  // the moved piece, its own frames
    CHECK(std::fabs(edited[36500]) < 1e-6f);                          // past its end
    // No click at the cut: the edge fades over 2 ms.
    CHECK(std::fabs(edited[11999]) < 0.002f);
    // A copy at another tempo (twice as long): the same pieces, at their place in it.
    auto copy = rampStems(2 * n);
    player.install(ps::StemBundle::fromFloat(copy, song));
    player.start(0);
    auto slow = play(player, 80000);
    CHECK(std::fabs(slow[48000 + 16000] - ramp(24000 + 16000, 2 * n)) < 2e-3f);
    CHECK(std::fabs(slow[36000]) < 1e-6f);
    // Edits taken away: the stem as separated again; zero pieces: silent.
    player.commitClips(0, -1);
    player.start(0);
    auto back = play(player, 40000);
    CHECK(std::fabs(back[36000] - ramp(36000, 2 * n)) < 2e-3f);
    player.commitClips(0, 0);
    player.start(0);
    auto none = play(player, 20000);
    CHECK(std::fabs(none[10000]) < 1e-6f);
    // A piece moved past the song's end still plays (the song itself again).
    player.install(song);  // its last reference goes to the player
    setClip(player, 0, 0, n + 4800, 0, 9600);
    player.commitClips(0, 1);
    player.start(0);
    auto late = play(player, int(n) + 12000);
    CHECK(std::fabs(late[size_t(n + 4800 + 4800)] - ramp(4800, n)) < 2e-3f);
    player.install(nullptr);
    player.collectGarbage();
}

}  // namespace remix5b

#define PS_REMIX5B_TESTS \
    {"remix5b_stem_pieces_cut_moved_and_faded", remix5b::testStemPiecesCutMovedAndFaded},
