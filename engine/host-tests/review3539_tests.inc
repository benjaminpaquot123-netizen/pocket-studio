// Review of phases 35 to 39 (engine): the 808 track's settings across a reopened stream, the
// multizone instrument's ClearZones, voice stealing, release zones, round robins, full event
// queues, huge values, and eight multizone tracks at once. Included by engine_tests.cpp after
// multizone_tests.inc (it uses its helpers).

namespace review3539_tests {

using multizone_tests::make;
using multizone_tests::midiHz;
using multizone_tests::rms;
using multizone_tests::sine;
using multizone_tests::zone;

std::vector<float> constant(float value, double seconds) { return std::vector<float>(size_t(seconds * kRate), value); }

std::vector<float> leftOf(const std::vector<float>& stereo, size_t fromFrame) {
    std::vector<float> l;
    for (size_t i = fromFrame * 2; i + 1 < stereo.size(); i += 2) l.push_back(stereo[i]);
    return l;
}

// 1. The 808 track's settings survive Engine::prepare (a stream reopened: headphones, Bluetooth,
// a latency change): prepare applies the waiting commands first, then prepares the tracks.
void testTrack808SettingsSurvivePrepare() {
    auto tail = [](bool reopen) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, (2 << 16) | 4});
        engine.post({ps::CommandType::SetTrackOn, 1.0f, 2});
        engine.post({ps::CommandType::SetTrack808Param, 0.05f, (2 << 16) | ps::Track808::Decay});  // a short note
        render(engine, 256, 256);
        if (reopen) engine.prepare(kRate);
        engine.post({ps::CommandType::SynthNoteOn, 0.9f, (2 << 16) | 33});
        const auto out = leftOf(render(engine, kRate, 256), size_t(kLate));
        return std::make_pair(rms(out, 0, 4800), rms(out, size_t(kRate) * 4 / 10, size_t(kRate) * 6 / 10));
    };
    const auto kept = tail(false), reopened = tail(true);
    std::printf("  808 track, decay 50 ms: rms at 0.5 s %.5f, after a reopened stream %.5f\n", kept.second, reopened.second);
    CHECK(kept.first > 0.02 && reopened.first > 0.02);
    CHECK(kept.second < 1e-3);
    CHECK(reopened.second < 1e-3);
}

// 2. ClearZones on a full instrument (512 zones) is cheap: it runs on the audio thread.
void testClearZonesIsCheap() {
    ps::MultiSampler m;
    m.prepare(kRate);
    ps::Sample* shared = make(sine(440, 0.3f, 0.05), 1, true);
    double best = 1e9;
    for (int pass = 0; pass < 5; ++pass) {
        for (int i = 0; i < ps::MultiSampler::kZones; ++i) {
            shared->retain();
            zone(m, i, shared, 0, 127, 69);
        }
        m.setParam(ps::MultiSampler::Count, float(ps::MultiSampler::kZones));
        CHECK(m.active());
        const double t0 = threadSeconds();
        m.setParam(ps::MultiSampler::ClearZones, 1.0f);
        best = std::min(best, threadSeconds() - t0);
        CHECK(!m.active());
        m.collectGarbage();
    }
    std::printf("  ClearZones over 512 zones: %.1f us\n", best * 1e6);
    CHECK_TIMING(best < 50e-6);
    if (shared->release()) delete shared;
}

// 3. A voice stolen (33 notes at once) fades out over 2 ms instead of being cut: no click.
void testStolenVoiceFadesOut() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 1);
    m.setParam(ps::MultiSampler::VelTrack, 0.0f);
    m.setParam(ps::MultiSampler::Attack, 0.02f);  // the new note comes in slowly: only the cut could click
    zone(m, 0, make(constant(0.25f, 3.0), 1), 0, 127, 60);
    for (int k = 0; k < ps::MultiSampler::kVoices; ++k) m.noteOn(40 + k, 1.0f, 0);
    std::vector<float> before(4800, 0.0f), after(4800, 0.0f);
    m.render(before.data(), nullptr, 4800);
    CHECK(m.activeVoices() == ps::MultiSampler::kVoices);
    m.noteOn(90, 1.0f, 0);
    m.render(after.data(), nullptr, 4800);
    float jump = std::fabs(after[0] - before.back());
    for (size_t i = 1; i < after.size(); ++i) jump = std::max(jump, std::fabs(after[i] - after[i - 1]));
    std::printf("  voice stolen: largest step %.4f (one voice is 0.25)\n", jump);
    CHECK(jump < 0.25f / 20.0f);
    CHECK(m.activeVoices() == ps::MultiSampler::kVoices);  // the stolen one is gone after its fade
    // A burst of notes (more than the spare slots at once) still never goes past the slots.
    for (int k = 0; k < 64; ++k) m.noteOn(k, 1.0f, 0);
    m.render(after.data(), nullptr, 4800);
    CHECK(m.activeVoices() == ps::MultiSampler::kVoices);
    m.allNotesOff();
    m.render(after.data(), nullptr, 4800);
    m.collectGarbage();
}

// 4. A release sample with a loop plays once: it ends (it has no envelope of its own).
void testLoopedReleaseZoneEnds() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 2);
    m.setParam(ps::MultiSampler::Release, 0.1f);
    zone(m, 0, make(sine(440, 0.3f, 2.0), 1), 0, 127, 69);
    zone(m, 1, make(sine(880, 0.3f, 0.5), 1), 0, 127, 69);
    m.setZoneParam(1, ps::MultiSampler::Trigger, 1.0f);
    m.setZoneParam(1, ps::MultiSampler::LoopStart, 100.0f);
    m.setZoneParam(1, ps::MultiSampler::LoopEnd, 4000.0f);
    m.noteOn(69, 0.8f, 0);
    multizone_tests::renderMulti(m, kRate / 10);
    m.noteOff(69, 0);
    multizone_tests::renderMulti(m, kRate / 5);
    CHECK(m.activeVoices() == 1);  // the held note is gone, the release sample plays
    multizone_tests::renderMulti(m, kRate * 2);
    std::printf("  looped release sample: %d voice(s) left 2 s after the key\n", m.activeVoices());
    CHECK(m.activeVoices() == 0);
    m.collectGarbage();
}

// 5. The round robins start over when the transport starts, even after notes played live while
// it was stopped: a song plays its turns the same way every time.
void testRoundRobinRestartsWithTheTransport() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetSynthEngine, 0.0f, (1 << 16) | 3});
    engine.post({ps::CommandType::SetTrackOn, 1.0f, 1});
    const double hz[2] = {440.0, 660.0};
    for (int i = 0; i < 2; ++i) {
        ps::Command c{ps::CommandType::InstallTrackSample, 0.0f, (1 << 16) | (i + 1)};
        c.sample = make(sine(hz[i], 0.5f, 2.0), 1, true);
        engine.post(c);
        engine.post({ps::CommandType::SetTrackZone, 127.0f, (1 << 16) | ps::MultiSampler::HiKey, i});
        engine.post({ps::CommandType::SetTrackZone, 69.0f, (1 << 16) | ps::MultiSampler::Root, i});
        engine.post({ps::CommandType::SetTrackZone, 2.0f, (1 << 16) | ps::MultiSampler::SeqLength, i});
        engine.post({ps::CommandType::SetTrackZone, float(i + 1), (1 << 16) | ps::MultiSampler::SeqPosition, i});
    }
    engine.post({ps::CommandType::SetTrackZone, 2.0f, (1 << 16) | ps::MultiSampler::Count, -1});
    // Played live while stopped: the first turn.
    engine.post({ps::CommandType::SynthNoteOn, 0.9f, (1 << 16) | 69});
    render(engine, kRate / 4, 256);
    engine.post({ps::CommandType::SynthNoteOff, 0.0f, (1 << 16) | 69});
    render(engine, kRate, 256);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    engine.post({ps::CommandType::SynthNoteOn, 0.9f, (1 << 16) | 69});
    const auto out = leftOf(render(engine, kRate / 2, 256), size_t(kLate + 2400));
    const double played = toneHz(out, 0, out.size());
    std::printf("  round robin after the transport starts: %.0f Hz (the first turn: 440)\n", played);
    CHECK(std::fabs(played - 440.0) < 8.0);
    engine.collectGarbage();
}

// 6. A full event queue never loses a key let go (the note would hold for ever): a key press
// gives it its place. The multizone instrument and the 808 track.
void testFullQueueKeepsNoteOffs() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 1);
    m.setParam(ps::MultiSampler::Release, 0.05f);
    zone(m, 0, make(sine(440, 0.3f, 4.0), 1), 0, 70, 69);
    m.noteOn(60, 0.8f, 0);
    multizone_tests::renderMulti(m, kRate / 20);
    for (int k = 0; k < 300; ++k) m.noteOn(100, 0.8f, kRate * 20);  // far ahead: they fill the queue
    m.noteOff(60, 0);
    multizone_tests::renderMulti(m, kRate / 2);
    std::printf("  full queue: multizone voices %d after the key let go\n", m.activeVoices());
    CHECK(m.activeVoices() == 0);
    m.collectGarbage();

    ps::Track808 b;
    b.prepare(kRate);
    std::vector<float> out(256);
    b.noteOn(36, 0.9f, 0);
    for (int at = 0; at < kRate / 10; at += 256) b.render(out.data(), 256);
    for (int k = 0; k < 200; ++k) b.noteOn(40, 0.9f, kRate * 20);
    b.noteOff(36, 0);
    for (int at = 0; at < kRate * 3 / 10; at += 256) b.render(out.data(), 256);
    CHECK(!b.sounding());
}

// 7. Huge values are clamped before they are rounded (lround of 1e30 is undefined): the highest key.
void testHugeZoneValuesAreClamped() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 1e30f);
    zone(m, 0, make(sine(440, 0.3f, 1.0), 1), 0, 0, 69);
    m.setZoneParam(0, ps::MultiSampler::HiKey, 1e30f);
    m.setParam(ps::MultiSampler::Count, 1e30f);
    m.noteOn(120, 0.8f, 0);
    multizone_tests::renderMulti(m, 256);
    CHECK(m.lastZone() == 0);
    m.allNotesOff();
    multizone_tests::renderMulti(m, kRate);
    m.collectGarbage();
}

// 8. Eight multizone tracks asked for 32 stereo voices each share the engine's 128 (16 each:
// 256 voices would take a phone's core) and fit the CPU budget.
void testEightMultizoneTracks() {
    ps::Engine engine(kRate);
    constexpr int kZonesPerTrack = ps::MultiSampler::kVoices;
    // One recording per key, shared by the eight tracks (each zone holds its own reference).
    std::vector<ps::Sample*> recordings;
    for (int i = 0; i < kZonesPerTrack; ++i) {
        const double hz = midiHz(40 + i);
        recordings.push_back(make(sine(hz, 0.05f, 5.0, 2, hz * 1.001), 2, true));
    }
    for (int t = 0; t < ps::kTracks; ++t) {
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, (t << 16) | 3});
        engine.post({ps::CommandType::SetTrackOn, 1.0f, t});
        for (int i = 0; i < kZonesPerTrack; ++i) {
            const int key = 40 + i;
            ps::Command c{ps::CommandType::InstallTrackSample, 0.0f, (t << 16) | (i + 1)};
            recordings[size_t(i)]->retain();
            c.sample = recordings[size_t(i)];
            engine.post(c);
            engine.post({ps::CommandType::SetTrackZone, float(key), (t << 16) | ps::MultiSampler::LoKey, i});
            engine.post({ps::CommandType::SetTrackZone, float(key), (t << 16) | ps::MultiSampler::HiKey, i});
            engine.post({ps::CommandType::SetTrackZone, float(key), (t << 16) | ps::MultiSampler::Root, i});
        }
        engine.post({ps::CommandType::SetTrackZone, float(kZonesPerTrack), (t << 16) | ps::MultiSampler::Count, -1});
        render(engine, 256, 256);  // the commands' queue has room for the next track
    }
    for (int t = 0; t < ps::kTracks; ++t) {
        for (int i = 0; i < kZonesPerTrack; ++i) engine.post({ps::CommandType::SynthNoteOn, 0.8f, (t << 16) | (40 + i)});
    }
    render(engine, 2048, 256);
    int voices = 0;
    for (int t = 0; t < ps::kTracks; ++t) {
        CHECK(engine.multiActiveVoices(t) == ps::kMultiVoices / ps::kTracks);
        voices += engine.multiActiveVoices(t);
    }
    // 4 s of the 5 s recordings: every voice plays all along.
    const int frames = kRate * 4;
    const double t0 = threadSeconds();
    const auto out = render(engine, frames, 256);
    const double load = (threadSeconds() - t0) / 4.0;
    const auto left = leftOf(out, size_t(frames) - 4800);
    std::printf("  eight multizone tracks, %d stereo voices: %.2f %% of real time (budget 20 %%), rms at the end %.3f\n",
                voices, 100.0 * load, rms(left, 0, left.size()));
    CHECK(rms(left, 0, left.size()) > 0.05);  // still all playing
    CHECK_TIMING(load < 0.20 * kSlowBuild);
    // The engine's zones hold the last references (freed with it).
    for (auto* r : recordings) {
        if (r->release()) delete r;
    }
}

}  // namespace review3539_tests

#define PS_REVIEW3539_TESTS                                                                       \
    {"r3539_808_settings_survive_prepare", review3539_tests::testTrack808SettingsSurvivePrepare},  \
    {"r3539_clear_zones_cheap", review3539_tests::testClearZonesIsCheap},                          \
    {"r3539_stolen_voice_fades", review3539_tests::testStolenVoiceFadesOut},                       \
    {"r3539_looped_release_zone_ends", review3539_tests::testLoopedReleaseZoneEnds},               \
    {"r3539_round_robin_transport", review3539_tests::testRoundRobinRestartsWithTheTransport},     \
    {"r3539_full_queue_keeps_note_offs", review3539_tests::testFullQueueKeepsNoteOffs},            \
    {"r3539_huge_zone_values", review3539_tests::testHugeZoneValuesAreClamped},                    \
    {"r3539_eight_multizone_tracks", review3539_tests::testEightMultizoneTracks},
