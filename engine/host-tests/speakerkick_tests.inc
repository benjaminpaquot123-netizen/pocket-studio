// Tour 5, lot « kick » (docs/audits/ultra/tour-5-kick-rapport.md): Benjamin's phone speaker
// (« ça tape mais pas assez, surtout le kick »). While the app says the music goes to the phone's own
// speaker (CommandType::SetSpeakerOutput), the kick's strip gets its harmonics and click (SpeakerKick.h);
// otherwise the output is the plain one, sample for sample. What a phone keeps: the bench's model, a
// 4th-order Butterworth high-pass at 300 Hz (basses_tests::highPass4).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace speakerkick_tests {

struct Song {
    int speaker = -1;      // SetSpeakerOutput sent (1 or 0); -1: never
    bool kick = true;      // the kick plays (pad 1, every quarter)
    bool kickStrip = true; // pad 1 through its own strip (the assistant's songs), else the drums' strip
    bool kickMuted = false;
};

// 4 bars at 128 BPM (5625 frames a step): the studio kick on every quarter and an 808 note held
// under it (pad 2, in the drums' strip), through the engine in 96-frame blocks.
std::vector<float> renderSong(const Song& song) {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 128.0f});
    engine.post({ps::CommandType::SetPadVoice, 2.0f, 0});  // the studio kick (kit v3's)
    if (song.kickStrip) engine.post({ps::CommandType::SetPadStrip, 1.0f, 0});
    for (int step = song.kickMuted ? 4 : 0; step < 16 && song.kick; step += 4) engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
    engine.post({ps::CommandType::SetPadVoice, 1.0f, 1});  // the 808 bass
    engine.post({ps::CommandType::SetDrumHit, 0.7f, 1, 2});
    if (song.kickMuted) engine.post({ps::CommandType::SetChannelMute, 1.0f, ps::Mixer::kFirstPad});
    if (song.speaker >= 0) engine.post({ps::CommandType::SetSpeakerOutput, static_cast<float>(song.speaker)});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    return render(engine, 5625 * 64, 96);
}

std::vector<float> monoOf(const std::vector<float>& x) {
    std::vector<float> m(x.size() / kChannels);
    for (size_t i = 0; i < m.size(); ++i) m[i] = 0.5f * (x[i * kChannels] + x[i * kChannels + 1]);
    return m;
}

// The energy of the first 100 ms after each kick (every 4 steps) of the phone-filtered mono, in dB.
double kickWindowsDb(const std::vector<float>& mono, float hz) {
    const auto y = basses_tests::highPass4(mono, hz);
    double e = 0.0;
    for (size_t at = 0; at + kLate + kRate / 10 < y.size(); at += 5625 * 4) e += basses_tests::energyOf(y, at + kLate, at + kLate + kRate / 10);
    return 10.0 * std::log10(std::max(e, 1e-30));
}

// On (1) against off (0): what the kick's hits gain through the phone model at `hz` (dB).
double liftDb(Song song, float hz) {
    song.speaker = 0;
    const auto off = monoOf(renderSong(song));
    song.speaker = 1;
    const auto on = monoOf(renderSong(song));
    return kickWindowsDb(on, hz) - kickWindowsDb(off, hz);
}

// Off (never sent, or sent 0): the engine's output is the plain one, sample for sample (headphones,
// Bluetooth and every export sound as before).
void testOffIsUntouched() {
    Song never, off;
    off.speaker = 0;
    const auto a = renderSong(never), b = renderSong(off);
    CHECK(a.size() == b.size());
    CHECK(std::equal(a.begin(), a.end(), b.begin()));
}

// On the phone's speaker, the kick's hits through the 300 Hz model are clearly louder (and through a
// good phone's 200 Hz one), the output finite and under the ceiling.
void testSpeakerLiftsTheKickOnAPhone() {
    const double lift300 = liftDb(Song{}, 300.0f), lift200 = liftDb(Song{}, 200.0f);
    std::printf("  speaker kick: %+.1f dB through 300 Hz, %+.1f dB through 200 Hz\n", lift300, lift200);
    CHECK(lift300 >= 4.0);
    CHECK(lift200 >= 3.0);
    Song on;
    on.speaker = 1;
    const auto stereo = renderSong(on);
    CHECK(studio_tests::allFinite(stereo));
    CHECK(studio_tests::peakOf(stereo) <= 1.0f);
}

// A kit whose kick has no strip of its own: the drums' strip gets it.
void testKickInTheDrumsStrip() {
    Song song;
    song.kickStrip = false;
    const double lift = liftDb(song, 300.0f);
    std::printf("  speaker kick in the drums' strip: %+.1f dB through 300 Hz\n", lift);
    CHECK(lift >= 3.0);
}

// Only the kick: without it (the 808 alone) or with its strip muted, the speaker changes nothing (the
// muted kick's first hit after the mute's 15 ms fade).
void testOnlyTheKick() {
    for (int c = 0; c < 2; ++c) {
        Song song;
        song.kick = c == 1;
        song.kickMuted = c == 1;
        song.speaker = 0;
        const auto off = renderSong(song);
        song.speaker = 1;
        const auto on = renderSong(song);
        CHECK(studio_tests::peakOf(off) > 0.01f);  // the 808 plays
        CHECK(std::equal(off.begin(), off.end(), on.begin()));
    }
}

// Back on headphones (on, then off): after its 20 ms glide the strip is the plain one again.
void testBackToHeadphones() {
    ps::SpeakerKick k;
    k.prepare(static_cast<float>(kRate));
    k.setOn(true);
    std::vector<float> l(kRate / 2), r(kRate / 2);
    for (size_t i = 0; i < l.size(); ++i) {
        l[i] = r[i] = 0.5f * static_cast<float>(std::sin(2.0 * M_PI * 50.0 * i / kRate) * std::exp(-static_cast<double>(i) / 4800.0));
    }
    auto a = l, b = r;
    k.process(a.data(), b.data(), static_cast<int>(a.size()));
    double changed = 0.0;
    for (size_t i = 0; i < l.size(); ++i) changed = std::max(changed, static_cast<double>(std::fabs(a[i] - l[i])));
    CHECK(changed > 0.01);  // on: the harmonics are heard
    CHECK(studio_tests::allFinite(a));
    k.setOn(false);
    a = l;
    b = r;
    k.process(a.data(), b.data(), static_cast<int>(a.size()));  // the glide down
    a = l;
    b = r;
    k.process(a.data(), b.data(), static_cast<int>(a.size()));
    CHECK(std::equal(a.begin(), a.end(), l.begin()));
    CHECK(!k.on());
}

}  // namespace speakerkick_tests

namespace speakerkick_tests {

// « Enregistrer ce que je joue » (ResampleArm) keeps the output in a take that is saved, played on
// headphones and exported: while one is armed the speaker's kick is left out (relecture neuve, grave).
void testNoSpeakerKickInATake() {
    auto withTake = [](int speaker, bool release) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 128.0f});
        engine.post({ps::CommandType::SetPadVoice, 2.0f, 0});
        engine.post({ps::CommandType::SetPadStrip, 1.0f, 0});
        for (int step = 0; step < 16; step += 4) engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
        engine.post({ps::CommandType::SetSpeakerOutput, static_cast<float>(speaker)});
        static ps::ResampleBuffer take(kRate * 20);
        ps::Command arm{ps::CommandType::ResampleArm, 0.0f, 0, 0};
        arm.resample = &take;
        engine.post(arm);
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        auto out = render(engine, 5625 * 32, 96);
        if (release) {
            engine.post({ps::CommandType::ResampleArm, 0.0f, 0, 0});  // the take let go: the speaker's kick again
            out = render(engine, 5625 * 32, 96);
        }
        return out;
    };
    const auto off = withTake(0, false), on = withTake(1, false);
    CHECK(studio_tests::peakOf(off) > 0.1f);
    CHECK(std::equal(off.begin(), off.end(), on.begin()));
    const auto offAfter = withTake(0, true), onAfter = withTake(1, true);
    CHECK(!std::equal(offAfter.begin(), offAfter.end(), onAfter.begin()));
}

}  // namespace speakerkick_tests

#define PS_SPEAKERKICK_TESTS                                                                      \
    {"sk_off_is_untouched", speakerkick_tests::testOffIsUntouched},                               \
    {"sk_speaker_lifts_the_kick_on_a_phone", speakerkick_tests::testSpeakerLiftsTheKickOnAPhone}, \
    {"sk_kick_in_the_drums_strip", speakerkick_tests::testKickInTheDrumsStrip},                   \
    {"sk_only_the_kick", speakerkick_tests::testOnlyTheKick},                                     \
    {"sk_back_to_headphones", speakerkick_tests::testBackToHeadphones},                           \
    {"sk_no_speaker_kick_in_a_take", speakerkick_tests::testNoSpeakerKickInATake},
