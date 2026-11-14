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

// --- Lot B2 (« la scène »): a row launched on the bar, each pad's pitch / reverse / attack /
// release, « Enregistrer ce que je joue » from bar line to bar line, the MIDI clock out. ---------

// The first frame (from `from`) where the left output is not silent.
static size_t firstSound(const std::vector<float>& out, size_t from = 0) {
    for (size_t i = from; i < out.size() / 2; ++i) {
        if (std::fabs(out[i * 2]) > 1e-6f) return i;
    }
    return out.size();
}

// A mono ramp: frame i holds i / frames * 0.5 (0 .. 0.5).
static ps::Sample* rampSample(int frames) {
    std::vector<float> d(static_cast<size_t>(frames));
    for (int i = 0; i < frames; ++i) d[static_cast<size_t>(i)] = 0.5f * static_cast<float>(i) / static_cast<float>(frames);
    return ps::Sample::create(d.data(), frames, 1, kRate, kRate);
}

static void rowStartsOnTheBar() {
    ps::Engine engine(kRate);  // 120 BPM: a beat is 24000 frames, a bar 96000
    installClip(engine, 0, constantSample(100, 1, 0.25f));  // a click every beat: the clock's reference
    engine.post({ps::CommandType::SetClipParam, 1.0f, 0, ps::ClipLauncher::Beats});
    // The row 4..7: a loop of 4 beats, a loop of 2, a hit, an empty pad.
    installClip(engine, 4, constantSample(500, 2, 0.1f));
    engine.post({ps::CommandType::SetClipParam, 4.0f, 4, ps::ClipLauncher::Beats});
    installClip(engine, 5, constantSample(500, 1, 0.05f));
    engine.post({ps::CommandType::SetClipParam, 2.0f, 5, ps::ClipLauncher::Beats});
    installClip(engine, 6, constantSample(500, 1, 0.02f));
    engine.post({ps::CommandType::SetClipParam, 1.0f, 6, ps::ClipLauncher::OneShot});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    auto head = render(engine, 36000, 256);  // beat 1.5
    const size_t onset = firstSound(head);
    CHECK(onset < 400);
    engine.post({ps::CommandType::LaunchClipRow, 0.0f, 4, 4});
    render(engine, 256, 256);
    CHECK(engine.clipState(4) == ps::ClipLauncher::Waiting && engine.clipState(5) == ps::ClipLauncher::Waiting);
    CHECK(engine.clipState(6) == ps::ClipLauncher::Waiting && engine.clipState(7) == ps::ClipLauncher::Off);
    // Beat 2 and 3 (the 2-beat loop's own grid, a hit's beat) pass: still waiting, for the bar.
    auto body = render(engine, 96000 - 36256 + 2000, 192);
    CHECK(engine.clipState(4) == ps::ClipLauncher::Playing && engine.clipState(5) == ps::ClipLauncher::Playing);
    const size_t bar = 96000 - 36256 + onset;  // where beat 4's click is in `body`
    CHECK(std::fabs(body[(bar - 1) * 2]) < 1e-6f);
    CHECK(std::fabs(body[bar * 2] - 0.42f) < 1e-3f);  // the click and the row's three sounds on one frame
    CHECK(std::fabs(body[(bar + 50) * 2] - 0.42f) < 1e-3f);
    // Again: its loops all play, so they stop (the hit is not a loop: left alone).
    engine.post({ps::CommandType::LaunchClipRow, 0.0f, 4, 4});
    render(engine, 2048, 256);
    CHECK(engine.clipState(4) == ps::ClipLauncher::Off && engine.clipState(5) == ps::ClipLauncher::Off);
    CHECK(engine.clipState(0) == ps::ClipLauncher::Playing);
    // Out of range or empty: nothing.
    engine.post({ps::CommandType::LaunchClipRow, 0.0f, 62, 8});
    engine.post({ps::CommandType::LaunchClipRow, 0.0f, -5, 3});
    render(engine, 512, 256);
    CHECK(engine.clipState(63) == ps::ClipLauncher::Off);
}

static void padPitchReverseAttackRelease() {
    auto hit = [](float pitch, float reverse, float attack) {
        ps::Engine engine(kRate);
        installClip(engine, 2, rampSample(1000));
        engine.post({ps::CommandType::SetClipParam, 1.0f, 2, ps::ClipLauncher::OneShot});
        engine.post({ps::CommandType::SetClipParam, pitch, 2, ps::ClipLauncher::Pitch});
        engine.post({ps::CommandType::SetClipParam, reverse, 2, ps::ClipLauncher::Reverse});
        engine.post({ps::CommandType::SetClipParam, attack, 2, ps::ClipLauncher::Attack});
        engine.post({ps::CommandType::ToggleClip, 0.0f, 2});
        return render(engine, 4000, 256);
    };
    auto ramp = [](double i) { return static_cast<float>(0.5 * i / 1000.0); };
    // As recorded: frame k of the sound on frame k; the reference onset.
    auto plain = hit(0.0f, 0.0f, 0.0f);
    const size_t at = firstSound(plain) - 1;  // frame 0 of the ramp is silent
    CHECK(std::fabs(plain[(at + 100) * 2] - ramp(100)) < 1e-4f);
    CHECK(std::fabs(plain[(at + 1100) * 2]) < 1e-6f);  // over after 1000 frames
    // An octave up: twice as fast, half as long.
    auto up = hit(12.0f, 0.0f, 0.0f);
    CHECK(std::fabs(up[(at + 100) * 2] - ramp(200)) < 1e-4f);
    CHECK(std::fabs(up[(at + 499) * 2] - ramp(998)) < 1e-4f);
    CHECK(std::fabs(up[(at + 520) * 2]) < 1e-6f);
    // An octave down: half as fast, the frames in between read on a straight line.
    auto down = hit(-12.0f, 0.0f, 0.0f);
    CHECK(std::fabs(down[(at + 201) * 2] - ramp(100.5)) < 1e-4f);
    CHECK(std::fabs(down[(at + 1990) * 2] - ramp(995)) < 1e-4f);
    // Backwards: the ramp's end first.
    auto back = hit(0.0f, 1.0f, 0.0f);
    CHECK(std::fabs(back[at * 2] - ramp(999)) < 1e-4f);
    CHECK(std::fabs(back[(at + 100) * 2] - ramp(899)) < 1e-4f);
    // An attack of 10 ms (480 frames): half way, half as loud.
    auto soft = hit(0.0f, 0.0f, 0.01f);
    CHECK(std::fabs(soft[(at + 240) * 2] - 0.5f * ramp(240)) < 2e-3f);
    CHECK(std::fabs(soft[(at + 700) * 2] - ramp(700)) < 1e-4f);

    // A release of 0.1 s (4800 frames): a loop stopped fades out over it, not cut.
    ps::Engine engine(kRate);
    installClip(engine, 9, constantSample(96000, 1, 0.2f));
    engine.post({ps::CommandType::SetClipParam, 4.0f, 9, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::SetClipParam, 0.1f, 9, ps::ClipLauncher::Release});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 9});
    auto start = render(engine, 10240, 256);
    const size_t on = firstSound(start);
    engine.post({ps::CommandType::ToggleClip, 0.0f, 9});
    auto tail = render(engine, 8000, 256);
    CHECK(std::fabs(tail[on * 2] - 0.2f) < 2e-3f);
    CHECK(std::fabs(tail[(on + 2400) * 2] - 0.1f) < 2e-3f);
    CHECK(std::fabs(tail[(on + 4810) * 2]) < 1e-6f);
    CHECK(engine.clipState(9) == ps::ClipLauncher::Off);
    // Out of range values are held to their limits (never a NaN).
    engine.post({ps::CommandType::SetClipParam, 99.0f, 9, ps::ClipLauncher::Pitch});
    engine.post({ps::CommandType::SetClipParam, NAN, 9, ps::ClipLauncher::Attack});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 9});
    CHECK(finiteAndUnclipped(render(engine, 24000, 256)));
}

static void resampleFromBarToBar() {
    ps::Engine engine(kRate);
    installClip(engine, 0, constantSample(100, 1, 0.5f));  // a click every beat
    engine.post({ps::CommandType::SetClipParam, 1.0f, 0, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    auto head = render(engine, 36000, 256);  // beat 1.5
    const size_t onset = firstSound(head);
    ps::ResampleBuffer take(kRate * 10);
    ps::Command arm{ps::CommandType::ResampleArm, 0.0f, 0, 0};
    arm.resample = &take;
    engine.post(arm);
    render(engine, 96000 - 36000 - 1000, 256);  // just before the bar line
    CHECK(take.state.load() == 0 && take.written.load() == 0);
    render(engine, 36000, 256);  // beat 5.5
    CHECK(take.state.load() == 1);
    engine.post({ps::CommandType::ResampleStop, 0.0f, 0});
    render(engine, 96000, 256);  // past beat 8
    CHECK(take.state.load() == 2);
    // From the bar line at beat 4 to the one at beat 8, heard: whole bars, the click where it was heard.
    CHECK(take.written.load() == 96000);
    const size_t click = onset - static_cast<size_t>(kLate);
    for (int beat = 0; beat < 4; ++beat) {
        const size_t i = (static_cast<size_t>(beat) * 24000 + click) * 2;
        CHECK(std::fabs(take.data[i] - 0.5f) < 1e-3f && std::fabs(take.data[i + 1] - 0.5f) < 1e-3f);
        if (i >= 2) CHECK(std::fabs(take.data[i - 2]) < 1e-6f);
    }
    std::printf("  click at frame %zu of the take (the output's own offset from the bar line)\n", click);

    // Armed in silence, waiting for the music: the free clock's bar lines pass, then the first pad
    // tapped starts it (the clock starts over on a bar line with it).
    ps::Engine quiet(kRate);
    installClip(quiet, 3, constantSample(100, 1, 0.5f));
    quiet.post({ps::CommandType::SetClipParam, 1.0f, 3, ps::ClipLauncher::Beats});
    ps::ResampleBuffer waits(kRate * 4);
    ps::Command wait{ps::CommandType::ResampleArm, 0.0f, 1, 0};
    wait.resample = &waits;
    quiet.post(wait);
    render(quiet, 96000 * 2 + 7000, 256);
    CHECK(waits.state.load() == 0);
    quiet.post({ps::CommandType::ToggleClip, 0.0f, 3});
    render(quiet, 30000, 256);
    CHECK(waits.state.load() == 1);
    CHECK(std::fabs(waits.data[click * 2] - 0.5f) < 1e-3f);
    CHECK(std::fabs(waits.data[(click + 24000) * 2] - 0.5f) < 1e-3f);
    // Full: it ends by itself, never past its buffer.
    render(quiet, kRate * 5, 256);
    CHECK(waits.state.load() == 2 && waits.written.load() == kRate * 4);
    // Stopped before its bar line: nothing kept.
    ps::ResampleBuffer early(kRate);
    ps::Command again{ps::CommandType::ResampleArm, 0.0f, 0, 0};
    again.resample = &early;
    quiet.post(again);
    quiet.post({ps::CommandType::ResampleStop, 0.0f, 0});
    render(quiet, 256, 256);
    CHECK(early.state.load() == 2 && early.written.load() == 0);
}

static void midiClockOutOnTheBeat() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 125.0f});  // a beat is 23040 frames, a tick 960
    engine.post({ps::CommandType::SetSwing, 0.5f});    // swung sixteenths: the clock stays straight
    engine.post({ps::CommandType::SetMidiClockOut, 1.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 23040 * 8 + 500, 192);
    std::vector<ps::MidiClockEvent> got;
    ps::MidiClockEvent e;
    while (engine.popClockEvent(e)) got.push_back(e);
    CHECK(got.size() >= 2 && got[0].status == 0xFA);
    CHECK(got[0].frame == kLate);  // the first step falls on frame 0, heard kLate later
    int ticks = 0;
    for (size_t i = 1; i < got.size(); ++i) {
        CHECK(got[i].status == 0xF8);
        CHECK(got[i].frame == kLate + static_cast<int64_t>(i - 1) * 960);
        ++ticks;
    }
    CHECK(ticks == 24 * 8 + 1);  // 8 beats and the next beat's first tick (before frame 23040 * 8 + 500)
    // The tempo changes: the next beat's ticks take the new spacing, none lost, none early.
    engine.post({ps::CommandType::SetTempo, 120.0f});
    render(engine, 23040 * 2, 192);
    int64_t last = got.back().frame;
    int more = 0;
    while (engine.popClockEvent(e)) {
        CHECK(e.status == 0xF8 && e.frame > last);
        last = e.frame;
        ++more;
    }
    CHECK(more > 40 && more < 48);
    // Stop: a Stop, then nothing.
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 23040, 192);
    int stops = 0;
    while (engine.popClockEvent(e)) {
        if (e.status == 0xFC) ++stops;
        else CHECK(e.status == 0xF8 && stops == 0);
    }
    CHECK(stops == 1);
    // Off: nothing at all.
    engine.post({ps::CommandType::SetMidiClockOut, 0.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 23040 * 2, 192);
    CHECK(!engine.popClockEvent(e));
    // Switched on while playing: a Continue, then the ticks.
    engine.post({ps::CommandType::SetMidiClockOut, 1.0f});
    render(engine, 23040, 192);
    CHECK(engine.popClockEvent(e) && e.status == 0xFB);
    CHECK(engine.popClockEvent(e) && e.status == 0xF8);
}

static void sceneWithoutAllocating() {
    ps::Engine engine(kRate);
    for (int pad = 0; pad < ps::ClipLauncher::kSlots; ++pad) {
        installClip(engine, pad, constantSample(6000, 1 + pad % 2, 0.002f));
        engine.post({ps::CommandType::SetClipParam, float(1 + pad % 4), pad, ps::ClipLauncher::Beats});
        engine.post({ps::CommandType::SetClipParam, float(pad % 25 - 12), pad, ps::ClipLauncher::Pitch});
        engine.post({ps::CommandType::SetClipParam, float(pad % 2), pad, ps::ClipLauncher::Reverse});
        engine.post({ps::CommandType::SetClipParam, 0.005f * float(pad % 3), pad, ps::ClipLauncher::Attack});
        engine.post({ps::CommandType::SetClipParam, 0.05f * float(pad % 3), pad, ps::ClipLauncher::Release});
    }
    std::vector<std::unique_ptr<ps::ResampleBuffer>> takes;
    for (int i = 0; i < 2; ++i) takes.push_back(std::make_unique<ps::ResampleBuffer>(kRate * 4));
    engine.post({ps::CommandType::SetMidiClockOut, 1.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(256 * 2);
    for (int i = 0; i < 20; ++i) engine.process(nullptr, 0, 0, out.data(), 256, 2);  // warm-up
    ps::MidiClockEvent e;
    int events = 0;
    g_allocations = 0;
    g_countAllocations = true;
    for (int i = 0; i < 1600; ++i) {
        if (i % 7 == 0) engine.post({ps::CommandType::LaunchClipRow, 0.0f, (i / 7 % 16) * 4, 4});
        if (i % 11 == 0) engine.post({ps::CommandType::ToggleClip, 1.0f, (i / 11) % ps::ClipLauncher::kSlots});
        // A take armed, stopped more than a bar later (a bar is 375 blocks at 120 BPM).
        if (i % 800 == 10) {
            ps::Command arm{ps::CommandType::ResampleArm, 0.0f, i == 10 ? 1 : 0, 0};
            arm.resample = takes[static_cast<size_t>(i / 800)].get();
            engine.post(arm);
        }
        if (i % 800 == 500) engine.post({ps::CommandType::ResampleStop, 0.0f, 0});
        if (i == 900) engine.post({ps::CommandType::SetTempo, 133.0f});
        engine.process(nullptr, 0, 0, out.data(), 256, 2);
        while (engine.popClockEvent(e)) ++events;
    }
    g_countAllocations = false;
    CHECK(g_allocations.load() == 0);
    CHECK(finiteAndUnclipped(out));
    CHECK(events > 300);
    int recorded = 0;
    for (auto& t : takes) recorded += t->written.load() > 0 ? 1 : 0;
    CHECK(recorded == 2);
}

// --- Lot B3 (« le jeu »): the lights follow the sound. ------------------------------------------

// Each pad's level is its own sound's peak (a loud pad bright, a quiet one dim, a silent one 0),
// it falls back within a few tenths of a second once the pad stops, and reading it allocates nothing.
static void lightsFollowTheSound() {
    ps::Engine engine(kRate);
    installClip(engine, 0, constantSample(48000, 1, 0.5f));   // loud
    installClip(engine, 1, constantSample(48000, 1, 0.05f));  // quiet
    installClip(engine, 2, constantSample(48000, 1, 0.5f));   // never played
    for (int pad = 0; pad < 3; ++pad) engine.post({ps::CommandType::SetClipParam, 4.0f, pad, ps::ClipLauncher::Beats});
    CHECK(engine.clipLevel(0) == 0.0f);
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 1});
    std::vector<float> out(256 * 2);
    g_allocations = 0;
    g_countAllocations = true;
    for (int i = 0; i < 40; ++i) engine.process(nullptr, 0, 0, out.data(), 256, 2);
    float read = 0.0f;
    for (int pad = 0; pad < ps::ClipLauncher::kSlots; ++pad) read += engine.clipLevel(pad);
    g_countAllocations = false;
    CHECK(g_allocations.load() == 0);
    CHECK(read > 0.5f);
    CHECK(std::fabs(engine.clipLevel(0) - 0.5f) < 0.02f);
    CHECK(std::fabs(engine.clipLevel(1) - 0.05f) < 0.01f);
    CHECK(engine.clipLevel(2) == 0.0f);
    CHECK(engine.clipLevel(-1) == 0.0f && engine.clipLevel(64) == 0.0f);
    // Stopped: dark again within half a second (its release is the short click-free fade).
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    for (int i = 0; i < 4; ++i) engine.process(nullptr, 0, 0, out.data(), 256, 2);
    const float falling = engine.clipLevel(0);
    CHECK(falling > 0.0f && falling < 0.5f);
    for (int i = 0; i < 94; ++i) engine.process(nullptr, 0, 0, out.data(), 256, 2);
    CHECK(engine.clipLevel(0) < 0.01f);
    CHECK(engine.clipLevel(1) > 0.04f);  // the other one still plays
}

// FIX-1: a stop names its take; a late one (sent for the take before) leaves the new one armed.
static void resampleStopTargetsItsTake() {
    ps::Engine engine(kRate);
    installClip(engine, 0, constantSample(100, 1, 0.5f));
    engine.post({ps::CommandType::SetClipParam, 1.0f, 0, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    render(engine, 1024, 256);
    ps::ResampleBuffer first(kRate * 4);
    ps::Command arm{ps::CommandType::ResampleArm, 0.0f, 0, 0};
    arm.resample = &first;
    engine.post(arm);
    render(engine, 96000, 256);
    CHECK(first.state.load() == 1);
    // The next take armed before the stop of the first one reaches the engine.
    ps::ResampleBuffer second(kRate * 4);
    ps::Command next{ps::CommandType::ResampleArm, 0.0f, 0, 0};
    next.resample = &second;
    engine.post(next);
    ps::Command late{ps::CommandType::ResampleStop, 0.0f, 1, 0};
    late.resample = &first;
    engine.post(late);
    render(engine, 256, 256);
    CHECK(first.state.load() == 2);
    CHECK(second.state.load() == 0);  // still armed: the stop was not its own
    render(engine, 96000, 256);
    CHECK(second.state.load() == 1);
    ps::Command own{ps::CommandType::ResampleStop, 0.0f, 1, 0};
    own.resample = &second;
    engine.post(own);
    render(engine, 256, 256);
    CHECK(second.state.load() == 2 && second.written.load() > 0);
}


// T5-PADRETRIG (plan 4.1, recette tel-17, Benjamin's phone 2026-11-08: « tant que le clap n'était pas fini il n'acceptait
// pas mon appui suivant »; R.7). Transport stopped, a « coup » pad (one-shot, 1 beat) touched again while its sound still
// plays started over on the next beat line of the PROJECT's grid, and a touch during that wait cancelled it: a rhythm kept
// by ear could not be played. Now a « coup » touched with the transport stopped sounds at once, each touch, never waits
// nor is cancelled by the next one (was « known_defect_t5_padretrig », inverted). Transport playing: on its grid, as before.
static void hitRetouchedSoundsAtOnce() {
    struct Pad {
        ps::ClipLauncher clips;
        std::vector<float> l = std::vector<float>(48), r = std::vector<float>(48);
        int64_t frame = 0;
        int64_t restarts = 0;  // passes started over (the level jumping back up from the clap's tail)
        Pad(double bpm, int clapFrames) {
            clips.prepare(kRate);
            clips.setBeatFrames(kRate * 60.0 / bpm);
            // A clap that fades out: each pass starts loud, so a restart is heard as a jump up.
            std::vector<float> clap(static_cast<size_t>(clapFrames));
            for (size_t i = 0; i < clap.size(); ++i) clap[i] = 0.5f * (1.0f - static_cast<float>(i) / static_cast<float>(clap.size()));
            clips.install(0, ps::Sample::create(clap.data(), int64_t(clap.size()), 1, kRate, kRate));
            clips.setParam(0, ps::ClipLauncher::Beats, 1.0f);
            clips.setParam(0, ps::ClipLauncher::OneShot, 1.0f);
        }
        // Renders up to [to] frames from the first touch, in 1 ms blocks; the loudest frame of the last block.
        float until(int64_t to) {
            float peak = 0.0f;
            while (frame + 48 <= to) {
                std::fill(l.begin(), l.end(), 0.0f);
                std::fill(r.begin(), r.end(), 0.0f);
                clips.render(l.data(), r.data(), 48);
                peak = 0.0f;
                for (float v : l) peak = std::max(peak, std::fabs(v));
                frame += 48;
            }
            return peak;
        }
        int state() const { return clips.state(0); }
        ~Pad() { clips.collectGarbage(); }
    };
    const int ms = kRate / 1000;
    {
        // 90 BPM (a beat: 32000 frames), a clap of 800 ms: touched again a hair after beat 1 (667 ms): heard at once.
        Pad pad(90.0, 800 * ms);
        pad.clips.toggle(0, true);
        pad.until(48);
        CHECK(pad.state() == ps::ClipLauncher::Playing);  // the first touch sounds at once
        const float tail = pad.until(667 * ms);
        pad.clips.toggle(0, true);
        CHECK(pad.state() != ps::ClipLauncher::Off);
        // Within 2 ms (the click-free fade of the old pass under it), the clap starts over: loud again.
        pad.until(667 * ms + 96);
        const float again = pad.until(667 * ms + 144);
        CHECK(pad.state() == ps::ClipLauncher::Playing);
        CHECK(again > tail + 0.2f);  // not waiting for beat 2 (1333 ms)
    }
    {
        // 90 BPM, eighth notes (333 ms) with a 400 ms clap: every touch sounds, none waits, none is cancelled.
        Pad pad(90.0, 400 * ms);
        pad.clips.toggle(0, true);
        for (int k = 1; k <= 3; ++k) {
            const float tail = pad.until(int64_t(k) * 333 * ms);
            pad.clips.toggle(0, true);
            CHECK(pad.state() != ps::ClipLauncher::Off);  // the 3rd touch used to cancel the 2nd
            pad.until(int64_t(k) * 333 * ms + 96);
            const float again = pad.until(int64_t(k) * 333 * ms + 144);
            CHECK(pad.state() == ps::ClipLauncher::Playing);
            CHECK(again > tail + 0.2f);
        }
    }
    {
        // 140 BPM project: touched again at 100 ms, heard at 100 ms (not on the project's beat 1, 429 ms).
        Pad pad(140.0, 800 * ms);
        pad.clips.toggle(0, true);
        const float tail = pad.until(100 * ms);
        pad.clips.toggle(0, true);
        pad.until(100 * ms + 96);
        CHECK(pad.state() == ps::ClipLauncher::Playing);
        CHECK(pad.until(100 * ms + 144) > tail + 0.04f);
    }
    {
        // Transport stopped, a loop on (pad 1): a hit comes on the loop's beat, as before; touched again while it waits,
        // it is not cancelled (it was): it still comes on that beat.
        Pad pad(120.0, 800 * ms);
        std::vector<float> loop(static_cast<size_t>(100 * ms), 0.1f);
        pad.clips.install(1, ps::Sample::create(loop.data(), int64_t(loop.size()), 1, kRate, kRate));
        pad.clips.setParam(1, ps::ClipLauncher::Beats, 1.0f);
        pad.clips.toggle(1, true);
        pad.until(100 * ms);
        pad.clips.toggle(0, true);
        CHECK(pad.state() == ps::ClipLauncher::Waiting);
        pad.until(200 * ms);
        pad.clips.toggle(0, true);
        CHECK(pad.state() == ps::ClipLauncher::Waiting);  // not cancelled
        pad.until(500 * ms + 48);
        CHECK(pad.state() == ps::ClipLauncher::Playing);  // on beat 1 (500 ms)
    }
    {
        // Transport playing (idleClock false): a « coup » still waits for its beat line, as before.
        Pad pad(120.0, 800 * ms);
        pad.clips.toggle(0, true);
        pad.until(100 * ms);
        pad.clips.toggle(0, false);
        CHECK(pad.state() == ps::ClipLauncher::Waiting);
        pad.until(500 * ms + 48);
        CHECK(pad.state() == ps::ClipLauncher::Playing);
    }
}

}  // namespace padlive

#define PS_PADLIVE_TESTS \
    {"padlive_page_d_plays_with_page_a", padlive::pageDPlaysWithPageA}, \
    {"padlive_hit_retouched_sounds_at_once", padlive::hitRetouchedSoundsAtOnce}, \
    {"padlive_64_pads_without_allocating", padlive::sixtyFourPadsWithoutAllocating}, \
    {"padlive_row_starts_on_the_bar", padlive::rowStartsOnTheBar}, \
    {"padlive_pad_pitch_reverse_attack_release", padlive::padPitchReverseAttackRelease}, \
    {"padlive_resample_from_bar_to_bar", padlive::resampleFromBarToBar}, \
    {"padlive_resample_stop_targets_its_take", padlive::resampleStopTargetsItsTake}, \
    {"padlive_midi_clock_out_on_the_beat", padlive::midiClockOutOnTheBeat}, \
    {"padlive_scene_without_allocating", padlive::sceneWithoutAllocating}, \
    {"padlive_lights_follow_the_sound", padlive::lightsFollowTheSound},
