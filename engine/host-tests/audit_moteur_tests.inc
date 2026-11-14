#include "ReopenBackoff.h"

// Final audit, lot « moteur » (reports 13 and 06): voice stealing in the
// General MIDI player, the master's known drive across a stream reopen, the swing inside the steps
// of the piano roll. Included by engine_tests.cpp (kept apart to ease merging).

namespace audit_moteur {

// 13 M4: Android reopening the stream (headphones, Bluetooth, the microphone rerouting) prepares the
// engine again at the same rate. The drive an export found stays settled: a quiet part heard after
// the reopen does not raise it (before: the estimate started afresh and the live level drifted from
// the export for half a minute). Another rate, or another project, still starts afresh.
void testReopenKeepsTheKnownDrive() {
    std::vector<float> l(480), r(480);
    auto run = [&](ps::MasterChain& chain, int blocks, float amp) {
        for (int b = 0; b < blocks; ++b) {
            for (int i = 0; i < 480; ++i) l[size_t(i)] = r[size_t(i)] = amp * std::sin(0.05f * float(b * 480 + i));
            chain.process(l.data(), r.data(), nullptr, 480);
        }
    };
    ps::MasterChain chain;
    chain.prepare(kRate);
    chain.setMode(ps::MasterChain::Mode::Drop);
    chain.setTarget(-8.0f);
    chain.setKnownDrive(6.0f);
    chain.setKnownLevel(-14.0f);
    run(chain, 200, 0.05f);  // 2 s
    CHECK(std::fabs(chain.driveDb() - 6.0f) < 0.01f);
    chain.prepare(kRate);  // the stream reopened
    CHECK(chain.knownLevel() == -14.0f);
    CHECK(std::fabs(chain.driveDb() - 6.0f) < 0.01f);
    run(chain, 4000, 0.02f);  // 40 s of a quiet part: a fresh estimate would raise the drive
    std::printf("  13 M4 known drive after a reopen and 40 s of a quiet part: %.2f dB\n", double(chain.driveDb()));
    CHECK(chain.driveDb() <= 6.0f + 0.01f);
    // Another rate: what was heard does not hold there.
    chain.prepare(44100);
    CHECK(std::isnan(chain.knownLevel()));
    // Through the engine: Engine::prepare (what AudioHost calls on a reopen) keeps it too.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetMaster, -10.0f, 10});  // the known level (S7)
    render(engine, 64, 64);
    CHECK(engine.mixer().masterChain().knownLevel() == -10.0f);
    engine.prepare(kRate);
    CHECK(engine.mixer().masterChain().knownLevel() == -10.0f);
    engine.post({ps::CommandType::SetMaster, 0.0f, 6});  // another project: forgotten
    render(engine, 64, 64);
    CHECK(std::isnan(engine.mixer().masterChain().knownLevel()));
}

// 13 M6: a General MIDI instrument with the sustain pedal down, more notes than voices: the new
// notes still sound (the oldest held ones make room), without allocating on the audio thread.
void testSoundFontStealsTheOldestVoice() {
    const std::string path = (std::filesystem::temp_directory_path() / "ps_steal.sf2").string();
    { auto bytes = tinySoundFont(); std::ofstream(path, std::ios::binary).write(reinterpret_cast<const char*>(bytes.data()), std::streamsize(bytes.size())); }
    tsf* master = ps::SoundFontPlayer::loadFile(path.c_str());
    CHECK(master != nullptr);
    if (!master) return;
    {
        ps::SoundFontPlayer player;
        player.install(ps::SoundFontPlayer::makeInstance(master, kRate), 0);
        std::vector<float> out(size_t(kRate) / 5, 0.0f);
        // 59 notes held (pedal down: never a note-off), 20 ms apart, MIDI 30 (49 Hz) to 88 (1319 Hz).
#if defined(PS_COUNT_ALLOCATIONS)
        g_allocations = 0;
        g_countAllocations = true;
#endif
        for (int k = 0; k < 59; ++k) {
            player.noteOn(30 + k, 0.8f, 0);
            std::fill(out.begin(), out.end(), 0.0f);
            player.render(out.data(), kRate / 50);
        }
        // The 60th: three octaves over the sample's A4 (3520 Hz), far above every note held.
        player.noteOn(105, 0.8f, 0);
        std::fill(out.begin(), out.end(), 0.0f);
        player.render(out.data(), int(out.size()));
#if defined(PS_COUNT_ALLOCATIONS)
        g_countAllocations = false;
        std::printf("  13 M6 allocations while stealing: %d\n", g_allocations.load());
        CHECK(g_allocations.load() == 0);
#endif
        const double level = toneLevel(out, out.size() / 2, out.size(), 3520.0);
        std::printf("  13 M6 the 60th note held (3520 Hz): level %.4f\n", level);
        CHECK(level > 0.02);
        bool finite = true;
        for (float v : out) finite = finite && std::isfinite(v);
        CHECK(finite);
    }
    ps::SoundFontPlayer::close(master);
    std::filesystem::remove(path);
}

// 06: notes between the sixteenths (1/32, 1/16 triplets) under the swing keep their order and
// never land on (or past) the next sixteenth: the odd step is squeezed, not shifted whole.
void testSwingKeepsNotesBetweenSixteenthsInOrder() {
    struct Sink {
        std::vector<std::pair<int64_t, int>> ons;  // frame, pitch
        int64_t base = 0;
        void noteOn(int pitch, float, int at) { ons.push_back({base + at, pitch}); }
        void noteOff(int, int) {}
    };
    for (double swing : {0.375, 0.5, 0.75}) {
        for (int grid : {12, 16}) {
            ps::Sequencer seq;
            seq.prepare(kRate);
            seq.setTempo(120.0);
            seq.setSwing(swing);
            ps::NoteTrack track;
            track.setSteps(16);
            const int count = 16 * 24 / grid;
            for (int i = 0; i < count; ++i) track.setNote(i, ps::NoteTrack::pack(i * grid, grid, 40 + i), 0.8f);
            track.setCount(count);
            Sink sink;
            seq.start(0);
            for (int pos = 0; pos < int(seq.samplesPerStep() * 16) - 256; pos += 256) {
                seq.advance(256, [&](int offset, int step) { track.onStep(offset, step, seq.samplesPerStep(), seq.swing()); });
                track.advance(256, sink);
                sink.base += 256;
            }
            std::stable_sort(sink.ons.begin(), sink.ons.end());
            bool ordered = int(sink.ons.size()) == count;
            const double tick = seq.samplesPerStep() / 24.0;
            for (size_t i = 1; i < sink.ons.size(); ++i) {
                ordered = ordered && sink.ons[i].second == sink.ons[i - 1].second + 1;
                // At least a tick of the squeezed step apart (no flam).
                ordered = ordered && double(sink.ons[i].first - sink.ons[i - 1].first) >= tick * (1.0 - swing) - 1.0;
            }
            if (!ordered) std::printf("  06 swing %.3f grid %d: notes out of order or flammed\n", swing, grid);
            CHECK(ordered);
            // A note written on a sixteenth stays exactly where the drums' hit is (the swing as ever).
            for (const auto& [at, pitch] : sink.ons) {
                const int tickPos = (pitch - 40) * grid;
                if (tickPos % 24 != 0) continue;
                const int step = tickPos / 24;
                const int64_t want = std::llround(step * seq.samplesPerStep() + ((step & 1) != 0 ? swing * seq.samplesPerStep() : 0.0));
                CHECK(std::llabs(at - want) <= 1);
            }
        }
    }
}

// 13 M5: a take armed for the transport start skips the frames before the first step, counted in
// the callback where it lands. When that callback's microphone batch is empty (the microphone
// delivers in bigger bursts than the output), the rest of the skip comes off the next batch instead
// of being lost (here the first step lands in a callback without microphone, the next one brings
// two callbacks' worth). Bursts not aligned with the callbacks still leave a jitter (timestamps:
// see the lot's leftovers).
void testTakeStartSurvivesMicrophoneBursts() {
    for (int burst : {1, 2}) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 130.0f});
        ps::RecordBuffer take(kRate);
        ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
        arm.record = &take;
        engine.post(arm);
        engine.post({ps::CommandType::SetTransport, 1.0f, 1});  // a bar of count-in: step 0 mid-callback
        constexpr int kBlock = 256;
        std::vector<float> out(kBlock * 2), in(size_t(kBlock) * 8);
        int64_t micFrame = 0, delivered = 0, outFrame = 0;
        int64_t micAtStart = -1;
        for (int k = 0; k < 400 && take.written.load() < 2048; ++k) {
            // The microphone: `burst` callbacks' worth at once, every `burst` callbacks.
            micFrame += kBlock;
            int n = 0;
            if ((k + 1) % burst == 0) {
                n = int(micFrame - delivered);
                for (int i = 0; i < n; ++i) in[size_t(i)] = float(delivered + i);
                delivered = micFrame;
            }
            const int before = take.state.load();
            engine.process(in.data(), n, 1, out.data(), kBlock, 2);
            // The first step's frame: where the take should start, in the microphone's frames.
            if (before == 0 && take.state.load() == 1) micAtStart = outFrame;
            outFrame += kBlock;
        }
        const int64_t bar = std::llround(4 * 60.0 / 130.0 * kRate);
        CHECK(micAtStart >= 0 && take.written.load() > 0);
        const float first = take.data[0];
        std::printf("  13 M5 bursts of %d callbacks: the take starts on microphone frame %.0f (the first step: %lld)\n", burst, double(first),
                    static_cast<long long>(bar));
        CHECK(std::llabs(static_cast<int64_t>(first) - bar) <= 1);
    }
}

// 06: a loop curve with a length of its own (the part's: 2 bars over a 1-bar drum loop) runs from
// the scene's start over that length, not over the drums'. Without one, as ever (the drums').
void testLoopCurveFollowsItsOwnLength() {
    using ps::CommandType;
    for (int own : {0, 32}) {
        ps::Engine e(kRate);
        e.post({CommandType::SetTempo, 120.0f});
        e.post({CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, ps::Mixer::kMaster, 0, 0)});
        e.post({CommandType::SetAutomationMode, 0.0f, 0, ps::Automation::Loop | (own << 8)});
        e.post({CommandType::SetAutomationPoint, 0.8f, 0, 0});   // unity
        e.post({CommandType::SetAutomationPoint, 0.4f, 0, 20});  // a quarter, in the second bar
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        render(e, 6000 * 22, 256);  // to step 22
        const float second = e.mixer().masterFader();
        render(e, 6000 * 11, 256);  // step 33: the loop came back to its start
        const float again = e.mixer().masterFader();
        std::printf("  06 loop curve of %d steps: master %.3f in the second bar, %.3f after the loop\n", own, double(second), double(again));
        if (own == 0) {
            CHECK(second == 1.0f);  // the drums' 16 steps: step 20 is never reached
        } else {
            CHECK(second < 0.3f);
            CHECK(again == 1.0f);
        }
    }
}

// 13 (minor): an audio clip or a live clip replaced while it sounds fades out (5 ms) instead of
// being cut (a click); the old sound is let go once its fade is done, never before.
void testReplacedClipFadesOut() {
    std::vector<float> dc(size_t(kRate), 0.5f);
    {
        ps::AudioTracks tracks;
        tracks.prepare(kRate);
        ps::Sample* old = ps::Sample::create(dc.data(), int64_t(dc.size()), 1, kRate, kRate);
        old->retain();  // ours, to watch it
        tracks.install(0, old);
        tracks.setParam(0, ps::AudioTracks::LoopSteps, 0.0f);
        tracks.onStep(0, 0, -1);
        std::vector<float> l(512, 0.0f), r(512, 0.0f);
        float* ls[ps::AudioTracks::kTracks] = {};
        float* rs[ps::AudioTracks::kTracks] = {};
        std::vector<std::vector<float>> scratch(ps::AudioTracks::kTracks * 2, std::vector<float>(512, 0.0f));
        for (int k = 0; k < ps::AudioTracks::kTracks; ++k) { ls[k] = scratch[size_t(2 * k)].data(); rs[k] = scratch[size_t(2 * k + 1)].data(); }
        ls[0] = l.data();
        rs[0] = r.data();
        tracks.render(ls, rs, 256);
        CHECK(std::fabs(l[255] - 0.5f) < 1e-6f);
        tracks.install(0, nullptr);  // emptied while it plays
        std::fill(l.begin(), l.end(), 0.0f);
        tracks.render(ls, rs, 512);
        float jump = std::fabs(l[0] - 0.5f);
        for (int i = 1; i < 512; ++i) jump = std::max(jump, std::fabs(l[size_t(i)] - l[size_t(i - 1)]));
        std::printf("  13 clip replaced: largest frame-to-frame change %.4f, after the fade %.4f\n", double(jump), double(l[511]));
        CHECK(jump < 0.01f);
        CHECK(l[511] == 0.0f);
        tracks.collectGarbage();
        CHECK(old->refCount() == 1);  // let go after its fade: only ours is left
        if (old->release()) delete old;
    }
    {
        ps::ClipLauncher clips;
        clips.prepare(kRate);
        clips.setBeatFrames(24000.0);
        clips.install(0, ps::Sample::create(dc.data(), int64_t(dc.size()), 1, kRate, kRate));
        clips.toggle(0, true);
        std::vector<float> l(512, 0.0f), r(512, 0.0f);
        clips.render(l.data(), r.data(), 256);
        const float before = l[255];
        clips.install(0, ps::Sample::create(dc.data(), int64_t(dc.size()), 1, kRate, kRate));  // another take of it
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        clips.render(l.data(), r.data(), 512);
        float jump = std::fabs(l[0] - before);
        for (int i = 1; i < 300; ++i) jump = std::max(jump, std::fabs(l[size_t(i)] - l[size_t(i - 1)]));
        std::printf("  13 live clip replaced: %.3f before, largest change %.4f\n", double(before), double(jump));
        CHECK(before > 0.4f);
        CHECK(jump < 0.01f);
        clips.collectGarbage();
    }
}

// 13 (minor): a multizone zone over full scale (a decoded MP3, the resampler's peaks) is kept with
// its scale, never clipped; one within full scale reads back as before (1/32768).
void testCompactZoneOverFullScaleIsNotClipped() {
    std::vector<float> loud(4800), soft(4800);
    for (size_t i = 0; i < loud.size(); ++i) {
        loud[i] = 1.5f * std::sin(0.01f * float(i));
        soft[i] = 0.5f * std::sin(0.01f * float(i));
    }
    ps::Sample* a = ps::Sample::createCompact(loud.data(), int64_t(loud.size()), 1, kRate, kRate);
    ps::Sample* b = ps::Sample::createCompact(soft.data(), int64_t(soft.size()), 1, kRate, kRate);
    CHECK(a && b);
    if (!a || !b) return;
    float errA = 0.0f, errB = 0.0f;
    for (size_t i = 0; i < loud.size(); ++i) {
        errA = std::max(errA, std::fabs(float(a->pcm16()[i]) * a->pcmScale() - loud[i]));
        errB = std::max(errB, std::fabs(float(b->pcm16()[i]) * b->pcmScale() - soft[i]));
    }
    std::printf("  13 compact zones: error %.6f over full scale, %.6f under\n", double(errA), double(errB));
    CHECK(errA < 1e-4f);
    CHECK(errB < 1e-4f);
    CHECK(b->pcmScale() == 1.0f / 32768.0f);
    if (a->release()) delete a;
    if (b->release()) delete b;
}

// 13 M3: after the quick retries (5 × 200 ms), the output is retried after 1, 2, 4 s and then every
// 8 s for as long as the app wants sound: never silent for good.
void testOutputRevivalSchedule() {
    CHECK(ps::ReopenBackoff::kQuickAttempts * ps::ReopenBackoff::kQuickDelayMs <= 1000);
    CHECK(ps::ReopenBackoff::delayMs(0) == 1000);
    CHECK(ps::ReopenBackoff::delayMs(1) == 2000);
    CHECK(ps::ReopenBackoff::delayMs(2) == 4000);
    CHECK(ps::ReopenBackoff::delayMs(3) == 8000);
    CHECK(ps::ReopenBackoff::delayMs(1000000) == 8000);
    CHECK(ps::ReopenBackoff::delayMs(-1) == 1000);
}

}  // namespace audit_moteur

#define PS_AUDIT_MOTEUR_TESTS                                                                      \
    {"audit13_reopen_keeps_the_known_drive", audit_moteur::testReopenKeepsTheKnownDrive},        \
    {"audit13_soundfont_steals_the_oldest_voice", audit_moteur::testSoundFontStealsTheOldestVoice}, \
    {"audit06_swing_keeps_notes_between_sixteenths", audit_moteur::testSwingKeepsNotesBetweenSixteenthsInOrder}, \
    {"audit13_take_start_survives_microphone_bursts", audit_moteur::testTakeStartSurvivesMicrophoneBursts}, \
    {"audit06_loop_curve_follows_its_own_length", audit_moteur::testLoopCurveFollowsItsOwnLength}, \
    {"audit13_replaced_clip_fades_out", audit_moteur::testReplacedClipFadesOut},                \
    {"audit13_compact_zone_over_full_scale", audit_moteur::testCompactZoneOverFullScaleIsNotClipped}, \
    {"audit13_output_revival_schedule", audit_moteur::testOutputRevivalSchedule},
