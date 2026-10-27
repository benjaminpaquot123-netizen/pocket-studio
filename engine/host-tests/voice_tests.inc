// Final audit, voice lot: microphone takes (alignment, pre-roll), monitoring and punch-in mutes.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace voice_tests {

// A microphone that delivers bursts of `burst` frames (the first one complete at `phase`), the
// sample value being its frame index, read by output callbacks of `block` frames.
struct BurstyMic {
    int64_t phase = 0;
    int64_t burst = 960;
    int64_t consumed = 0;
    // Never a frame not yet captured: bursts end at phase + k * burst.
    int64_t delivered(int64_t t) const { return t < phase ? 0 : phase + (t - phase) / burst * burst; }
};

// 13 M5: wherever the bursts fall and whichever callback the first step lands in, a take starts
// on the same microphone frame (to within an output callback), and none of the start is lost
// when the callback where it starts brings no microphone frames.
void testTakeStartIgnoresMicrophoneBursts() {
    constexpr int kBlock = 192;
    std::vector<float> out(kBlock * 2), in(8192);
    int64_t lowest = INT64_MAX, highest = INT64_MIN;
    for (int64_t phase : {0, 100, 250, 480, 700, 959}) {
        for (int startBlock : {40, 41, 43, 47}) {
            ps::Engine engine(kRate);
            ps::RecordBuffer buffer(kRate);
            ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
            arm.record = &buffer;
            engine.post(arm);
            BurstyMic mic{phase};  // phase < burst
            int64_t t = 0, start = -1;
            for (int c = 0; c < 700; ++c) {
                if (c == startBlock) {
                    engine.post({ps::CommandType::SetTransport, 1.0f, 1});  // a bar of count-in
                    start = t + 96000;
                }
                const int n = static_cast<int>(mic.delivered(t) - mic.consumed);
                for (int i = 0; i < n; ++i) in[size_t(i)] = float(mic.consumed + i);
                engine.process(in.data(), n, 1, out.data(), kBlock, 2);
                mic.consumed += n;
                t += kBlock;
            }
            CHECK(buffer.state.load() == 1);
            CHECK(buffer.written.load() > kRate / 4);
            if (buffer.written.load() <= 0) continue;
            const int64_t first = static_cast<int64_t>(buffer.data[0]) - start;
            // Back to back from there: nothing skipped or doubled.
            CHECK(buffer.data[1000] == buffer.data[0] + 1000.0f);
            lowest = std::min(lowest, first);
            highest = std::max(highest, first);
        }
    }
    std::printf("  take start vs first step: %lld..%lld frames\n", static_cast<long long>(lowest), static_cast<long long>(highest));
    CHECK(highest - lowest <= kBlock);
    CHECK(lowest >= -3 * kBlock && highest <= kBlock);
}

// 03 M1: with a pre-roll, what came in just before the first step (a word sung ahead of the "1")
// is kept in front of the take, in order.
void testPrerollKeepsWhatCameBeforeTheOne() {
    constexpr int kPreroll = 12000;
    ps::Engine engine(kRate);
    ps::RecordBuffer buffer(kRate, kPreroll);
    ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
    arm.record = &buffer;
    engine.post(arm);
    std::vector<float> out(256 * 2), in(256);
    int64_t micFrame = 0;
    auto block = [&]() {
        for (int i = 0; i < 256; ++i) in[size_t(i)] = float(micFrame++);
        engine.process(in.data(), 256, 1, out.data(), 256, 2);
    };
    for (int k = 0; k < 10; ++k) block();
    engine.post({ps::CommandType::SetTransport, 1.0f, 1});
    const int64_t start = 10 * 256 + 96000;
    while (micFrame <= start) block();  // through the block where the first step falls
    CHECK(buffer.state.load() == 1);
    for (int k = 0; k < 40; ++k) block();
    const int64_t written = buffer.written.load();
    CHECK(written == kPreroll + (micFrame - start));
    // What the app does before reading it: the ring in order.
    const int64_t head = buffer.prerollHead.load();
    std::rotate(buffer.data.begin(), buffer.data.begin() + head, buffer.data.begin() + kPreroll);
    CHECK(buffer.data[kPreroll] == float(start));
    CHECK(buffer.data[kPreroll - 1] == float(start - 1));
    CHECK(buffer.data[0] == float(start - kPreroll));
    CHECK(buffer.data[kPreroll + 5000] == float(start + 5000));
    // Stopped during the count-in: nothing at all (the pre-roll alone is not a take).
    ps::RecordBuffer early(kRate, kPreroll);
    ps::Command again{ps::CommandType::RecordArm, 0.0f, 1};
    again.record = &early;
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post(again);
    block();
    engine.post({ps::CommandType::SetTransport, 1.0f, 1});
    for (int k = 0; k < 20; ++k) block();
    engine.post({ps::CommandType::RecordStop, 0.0f});
    block();
    CHECK(early.state.load() == 2 && early.written.load() == 0);
}

// 03 G1: monitoring adds the microphone to the recorded track's strip, only while a take is armed
// or running; off (or with no take), the output is what it was.
void testMonitorHearsTheMicrophoneOnItsTrack() {
    std::vector<float> out(256 * 2), in(256);
    double phase = 0.0;
    auto feed = [&](ps::Engine& e, int blocks) {
        float peak = 0.0f;
        for (int k = 0; k < blocks; ++k) {
            for (int i = 0; i < 256; ++i) {
                in[size_t(i)] = 0.5f * static_cast<float>(std::sin(phase));
                phase += 2.0 * 3.14159265358979 * 440.0 / kRate;
            }
            e.process(in.data(), 256, 1, out.data(), 256, 2);
            for (float v : out) peak = std::max(peak, std::fabs(v));
        }
        return peak;
    };
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetMonitor, 1.0f, 0});
    CHECK(feed(engine, 40) == 0.0f);  // no take: the microphone is never heard
    ps::RecordBuffer buffer(kRate * 4);
    ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
    arm.record = &buffer;
    engine.post(arm);
    feed(engine, 10);
    const float heard = feed(engine, 40);
    std::printf("  monitored peak %.3f\n", heard);
    CHECK(heard > 0.2f && heard < 0.6f);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) > 0.2f);  // through the track's strip
    // The track muted in the mixer: so is its monitoring.
    engine.post({ps::CommandType::SetChannelMute, 1.0f, ps::audioChannel(0)});
    feed(engine, 20);
    const float muted = feed(engine, 40);
    std::printf("  muted peak %.5f\n", muted);
    CHECK(muted < 1e-3f);  // -60 dB: the fader's ramp is still closing
    engine.post({ps::CommandType::SetChannelMute, 0.0f, ps::audioChannel(0)});
    engine.post({ps::CommandType::SetMonitor, 1.0f, -1});
    feed(engine, 20);  // the strip's tail
    CHECK(feed(engine, 40) < 1e-3f);
    // Back on, then the take ends: silent again.
    engine.post({ps::CommandType::SetMonitor, 1.0f, 0});
    CHECK(feed(engine, 40) > 0.2f);
    engine.post({ps::CommandType::RecordStop, 0.0f});
    feed(engine, 20);
    CHECK(feed(engine, 40) < 1e-3f);
    CHECK(!engine.hasNonFinite());
}

// 03 M4: in a punch-in, the clip being redone is heard up to the first bar sung again, then not.
void testPunchMutesTheClipFromItsStep() {
    ps::Engine engine(kRate);
    std::vector<float> tone(size_t(kRate) * 4);
    for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.5f * static_cast<float>(std::sin(2.0 * 3.14159265358979 * 220.0 * double(i) / kRate));
    ps::Command clip{ps::CommandType::InstallAudioClip, 0.0f, 0};
    clip.sample = ps::Sample::create(tone.data(), int(tone.size()), 1, kRate, kRate);
    engine.post(clip);
    engine.post({ps::CommandType::SetAudioClipParam, 64.0f, 0, ps::AudioTracks::LoopSteps});
    ps::RecordBuffer buffer(kRate * 4);
    ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
    arm.record = &buffer;
    engine.post(arm);
    engine.post({ps::CommandType::RecordMuteAt, 0.0f, 0, 8});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 7, 100);
    engine.mixer().takeChannelPeak(ps::audioChannel(0));
    render(engine, 5900, 100);  // just before step 8
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) > 0.2f);
    render(engine, 1000, 100);  // the fade at step 8
    engine.mixer().takeChannelPeak(ps::audioChannel(0));
    render(engine, 6000 * 4, 100);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) < 1e-4f);
    // The take ends: the mute goes with it (the app's own mutes take over).
    engine.post({ps::CommandType::RecordStop, 0.0f});
    engine.post({ps::CommandType::SetAudioOn, 1.0f, 0});
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 10, 100);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) > 0.2f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::InstallAudioClip, 0.0f, 0});
    render(engine, 512, 256);
    engine.collectGarbage();
}

// Revue neuve (MOYEN 3): a punch whose take is not kept (too short, silent, lost, failed) leaves the
// clip sounding again at the take's end, with no word from the app.
void testFailedPunchGivesTheClipBack() {
    ps::Engine engine(kRate);
    std::vector<float> tone(size_t(kRate) * 4);
    for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.5f * static_cast<float>(std::sin(2.0 * 3.14159265358979 * 220.0 * double(i) / kRate));
    ps::Command clip{ps::CommandType::InstallAudioClip, 0.0f, 0};
    clip.sample = ps::Sample::create(tone.data(), int(tone.size()), 1, kRate, kRate);
    engine.post(clip);
    engine.post({ps::CommandType::SetAudioClipParam, 64.0f, 0, ps::AudioTracks::LoopSteps});
    ps::RecordBuffer buffer(kRate * 4);
    ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
    arm.record = &buffer;
    engine.post(arm);
    engine.post({ps::CommandType::RecordMuteAt, 0.0f, 0, 8});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 12, 100);
    engine.mixer().takeChannelPeak(ps::audioChannel(0));
    render(engine, 6000 * 2, 100);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) < 1e-4f);  // muted from step 8
    // The take stops and is thrown away: no SetAudioOn follows.
    engine.post({ps::CommandType::RecordStop, 0.0f});
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 10, 100);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) > 0.2f);
    // A track the part had switched off before the punch stays off.
    ps::RecordBuffer second(kRate * 4);
    ps::Command again{ps::CommandType::RecordArm, 0.0f, 1};
    again.record = &second;
    engine.post({ps::CommandType::SetAudioOn, 0.0f, 0});
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post(again);
    engine.post({ps::CommandType::RecordMuteAt, 0.0f, 0, 8});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 12, 100);
    engine.post({ps::CommandType::RecordStop, 0.0f});
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 2, 100);
    engine.mixer().takeChannelPeak(ps::audioChannel(0));
    render(engine, 6000 * 8, 100);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) < 1e-4f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::InstallAudioClip, 0.0f, 0});
    render(engine, 512, 256);
    engine.collectGarbage();
}

// Revue neuve (MOYEN 4): the output reopened (headphones unplugged, the speaker now): the voice is
// no longer sent back, and the app can see it to turn it on again for headphones.
void testReopenedOutputCutsTheMonitor() {
    std::vector<float> out(256 * 2), in(256, 0.5f);
    auto feed = [&](ps::Engine& e, int blocks) {
        float peak = 0.0f;
        for (int k = 0; k < blocks; ++k) {
            for (int i = 0; i < 256; ++i) in[size_t(i)] = (i % 2) ? 0.5f : -0.5f;
            e.process(in.data(), 256, 1, out.data(), 256, 2);
            for (float v : out) peak = std::max(peak, std::fabs(v));
        }
        return peak;
    };
    ps::Engine engine(kRate);
    ps::RecordBuffer buffer(kRate * 4);
    ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
    arm.record = &buffer;
    engine.post(arm);
    engine.post({ps::CommandType::SetMonitor, 1.0f, 0});
    feed(engine, 10);
    CHECK(engine.monitorTrack() == 0);
    CHECK(feed(engine, 20) > 0.1f);
    engine.prepare(kRate);  // the stream reopened (no audio thread meanwhile)
    CHECK(engine.monitorTrack() == -1);
    feed(engine, 20);  // the strip's tail
    CHECK(feed(engine, 40) < 1e-3f);
    // A SetMonitor still queued when the output reopens is cut too.
    engine.post({ps::CommandType::SetMonitor, 1.0f, 0});
    engine.prepare(kRate);
    CHECK(engine.monitorTrack() == -1);
    // Turned on again by the app (headphones still there): heard.
    engine.post({ps::CommandType::SetMonitor, 1.0f, 0});
    feed(engine, 10);
    CHECK(engine.monitorTrack() == 0 && feed(engine, 20) > 0.1f);
    engine.post({ps::CommandType::RecordStop, 0.0f});
    feed(engine, 4);
    CHECK(!engine.hasNonFinite());
}

}  // namespace voice_tests

#define PS_VOICE_TESTS \
    {"voice_take_start_ignores_bursts", voice_tests::testTakeStartIgnoresMicrophoneBursts}, \
    {"voice_preroll_keeps_anacrusis", voice_tests::testPrerollKeepsWhatCameBeforeTheOne}, \
    {"voice_monitor_on_its_track", voice_tests::testMonitorHearsTheMicrophoneOnItsTrack}, \
    {"voice_punch_mutes_from_its_step", voice_tests::testPunchMutesTheClipFromItsStep}, \
    {"voice_failed_punch_gives_clip_back", voice_tests::testFailedPunchGivesTheClipBack}, \
    {"voice_reopen_cuts_monitor", voice_tests::testReopenedOutputCutsTheMonitor},
