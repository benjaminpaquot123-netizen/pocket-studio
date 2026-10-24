// Phase 38 (« le son présentable »): the multizone sampled instruments (MultiSampler: key ranges,
// velocity layers, round robins, release samples, stereo, 16-bit zones) and the 808 on an
// instrument track (Track808). Included by engine_tests.cpp (kept apart to ease merging).

namespace multizone_tests {

// `seconds` of a sine at `hz` (amplitude `amp`), mono or stereo (the right at `hzRight`).
std::vector<float> sine(double hz, float amp, double seconds, int channels = 1, double hzRight = 0.0) {
    const size_t frames = static_cast<size_t>(seconds * kRate);
    std::vector<float> w(frames * static_cast<size_t>(channels));
    for (size_t i = 0; i < frames; ++i) {
        w[i * size_t(channels)] = amp * float(std::sin(2.0 * M_PI * hz * double(i) / kRate));
        if (channels == 2) w[i * 2 + 1] = amp * float(std::sin(2.0 * M_PI * (hzRight > 0 ? hzRight : hz) * double(i) / kRate));
    }
    return w;
}

double midiHz(double note) { return 440.0 * std::pow(2.0, (note - 69.0) / 12.0); }

ps::Sample* make(const std::vector<float>& w, int channels, bool compact = false) {
    const int64_t frames = static_cast<int64_t>(w.size()) / channels;
    return compact ? ps::Sample::createCompact(w.data(), frames, channels, kRate, kRate)
                   : ps::Sample::create(w.data(), frames, channels, kRate, kRate);
}

// One zone: keys lo..hi, velocities vlo..vhi, recorded at `root`.
void zone(ps::MultiSampler& m, int i, ps::Sample* s, int lo, int hi, float root, int vlo = 1, int vhi = 127) {
    m.installZone(i, s);
    m.setZoneParam(i, ps::MultiSampler::LoKey, float(lo));
    m.setZoneParam(i, ps::MultiSampler::HiKey, float(hi));
    m.setZoneParam(i, ps::MultiSampler::LoVel, float(vlo));
    m.setZoneParam(i, ps::MultiSampler::HiVel, float(vhi));
    m.setZoneParam(i, ps::MultiSampler::Root, root);
}

struct Stereo {
    std::vector<float> l, r;
};
Stereo renderMulti(ps::MultiSampler& m, int frames, int block = 256) {
    Stereo out{std::vector<float>(size_t(frames)), std::vector<float>(size_t(frames))};
    for (int at = 0; at < frames; at += block) m.render(out.l.data() + at, out.r.data() + at, std::min(block, frames - at));
    return out;
}

double rms(const std::vector<float>& x, size_t from, size_t to) {
    double e = 0.0;
    for (size_t i = from; i < to; ++i) e += double(x[i]) * x[i];
    return std::sqrt(e / double(std::max<size_t>(1, to - from)));
}

// 1. Each key plays the zone that covers it, shifted from that zone's root: a piano sampled
// every minor third plays every key in tune.
void testZoneSelection() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 3);
    m.setParam(ps::MultiSampler::VelTrack, 0.0f);
    zone(m, 0, make(sine(midiHz(48), 0.5f, 2.0), 1), 0, 53, 48);
    zone(m, 1, make(sine(midiHz(60), 0.5f, 2.0), 1), 54, 65, 60);
    zone(m, 2, make(sine(midiHz(72), 0.5f, 2.0), 1), 66, 127, 72);
    CHECK(m.active());
    const int notes[] = {45, 53, 54, 62, 66, 79};
    const int zones[] = {0, 0, 1, 1, 2, 2};
    for (int k = 0; k < 6; ++k) {
        m.allNotesOff();
        renderMulti(m, kRate);
        m.noteOn(notes[k], 0.8f, 10);
        auto out = renderMulti(m, kRate);
        CHECK(m.lastZone() == zones[k]);
        CHECK(out.l[9] == 0.0f);  // on its frame, not before
        const double hz = toneHz(out.l, 2400, out.l.size());
        CHECK(std::fabs(hz / midiHz(notes[k]) - 1.0) < 0.012);
        m.noteOff(notes[k], 0);
    }
    // A key no zone covers stays silent; zones past Count are ignored.
    m.setParam(ps::MultiSampler::Count, 2);
    m.allNotesOff();
    renderMulti(m, kRate);
    m.noteOn(80, 0.8f, 0);
    auto out = renderMulti(m, 4800);
    CHECK(rms(out.l, 0, out.l.size()) == 0.0);
    CHECK(m.lastZone() == -1);
}

// 2. Velocity layers: a soft key plays the soft recording, a hard one the loud one; within a
// layer the velocity tracks the level as asked (VelTrack).
void testVelocityLayers() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 2);
    m.setParam(ps::MultiSampler::VelTrack, 0.0f);
    zone(m, 0, make(sine(440, 0.1f, 1.0), 1), 0, 127, 69, 1, 64);
    zone(m, 1, make(sine(440, 0.8f, 1.0), 1), 0, 127, 69, 65, 127);
    auto level = [&](float vel) {
        m.allNotesOff();
        renderMulti(m, kRate);
        m.noteOn(69, vel, 0);
        auto out = renderMulti(m, 9600);
        m.noteOff(69, 0);
        return rms(out.l, 960, out.l.size());
    };
    const double soft = level(0.3f), hard = level(0.9f);
    std::printf("  velocity layers: soft %.3f, hard %.3f\n", soft, hard);
    CHECK(std::fabs(soft - 0.1 / std::sqrt(2.0)) < 0.005);
    CHECK(std::fabs(hard - 0.8 / std::sqrt(2.0)) < 0.02);
    // Full tracking: half the velocity is 12 dB softer (SFZ's 40 log10).
    m.setParam(ps::MultiSampler::VelTrack, 1.0f);
    const double full = level(1.0f);
    const double v80 = level(80.0f / 127.0f), v100 = level(100.0f / 127.0f);
    CHECK(std::fabs(20.0 * std::log10(v80 / v100) - 40.0 * std::log10(80.0 / 100.0)) < 0.2);
    CHECK(full > v100);
}

// 3. Round robins take turns in a fixed order, start over with the transport, and two engines
// fed the same notes play the same samples (an export sounds like the live take).
void testRoundRobinIsDeterministic() {
    auto build = [](ps::MultiSampler& m) {
        m.prepare(kRate);
        m.setParam(ps::MultiSampler::Count, 3);
        for (int i = 0; i < 3; ++i) {
            zone(m, i, make(sine(440.0 * (1.0 + 0.01 * i), 0.5f, 0.5), 1), 0, 127, 69);
            m.setZoneParam(i, ps::MultiSampler::SeqLength, 3);
            m.setZoneParam(i, ps::MultiSampler::SeqPosition, float(i + 1));
        }
    };
    ps::MultiSampler a, b;
    build(a);
    build(b);
    std::vector<int> turns;
    for (int k = 0; k < 7; ++k) {
        a.noteOn(69, 0.8f, 0);
        b.noteOn(69, 0.8f, 0);
        auto oa = renderMulti(a, 2400), ob = renderMulti(b, 2400);
        turns.push_back(a.lastZone());
        CHECK(a.lastZone() == b.lastZone());
        CHECK(oa.l == ob.l);
        a.noteOff(69, 0);
        b.noteOff(69, 0);
    }
    CHECK((turns == std::vector<int>{0, 1, 2, 0, 1, 2, 0}));
    // The transport stops (every note off): the turns start over.
    a.allNotesOff();
    a.noteOn(69, 0.8f, 0);
    renderMulti(a, 256);
    CHECK(a.lastZone() == 0);
    // A turn whose recording is missing: the first zone that matched plays (never a silent key).
    a.installZone(1, nullptr);
    a.allNotesOff();
    std::vector<int> missing;
    for (int k = 0; k < 3; ++k) {
        a.noteOn(69, 0.8f, 0);
        renderMulti(a, 256);
        missing.push_back(a.lastZone());
    }
    CHECK((missing == std::vector<int>{0, 0, 2}));
    a.collectGarbage();
}

// 4. Release samples play when the key is let go, as loud as the key press, softer the longer
// it was held (ReleaseDecay); the held note dies away over Release.
void testReleaseSamples() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 2);
    m.setParam(ps::MultiSampler::Release, 0.1f);
    m.setParam(ps::MultiSampler::ReleaseDecay, 10.0f);  // 10 dB a second held
    m.setParam(ps::MultiSampler::VelTrack, 0.0f);
    zone(m, 0, make(sine(220, 0.5f, 4.0), 1), 0, 127, 57);
    zone(m, 1, make(sine(1000, 0.4f, 0.3), 1), 0, 127, 57);
    m.setZoneParam(1, ps::MultiSampler::Trigger, 1);
    auto releaseLevel = [&](int heldFrames) {
        m.allNotesOff();
        renderMulti(m, kRate);
        m.noteOn(57, 0.8f, 0);
        auto held = renderMulti(m, heldFrames);
        CHECK(std::fabs(toneHz(held.l, 0, held.l.size()) - 220.0) < 12.0);  // no release sound while held
        m.noteOff(57, 0);
        auto after = renderMulti(m, kRate / 4);
        // The held note is down 60 dB after 0.1 s: past it only the release sample rings, at 1 kHz.
        const double hz = toneHz(after.l, 4800 + 480, 9600);
        CHECK(std::fabs(hz - 1000.0) < 20.0);
        return rms(after.l, 5280, 9600);
    };
    const double quick = releaseLevel(4800), long1 = releaseLevel(48000);
    std::printf("  release sample: after 0.1 s %.4f, after 1 s %.4f (%.1f dB)\n", quick, long1, 20 * std::log10(long1 / quick));
    CHECK(std::fabs(20 * std::log10(long1 / quick) + 9.0) < 0.6);  // 0.9 s more held: 9 dB softer
    // A key let go that was never pressed (or cut by allNotesOff) plays no release sample.
    m.allNotesOff();
    renderMulti(m, kRate);
    m.noteOff(57, 0);
    auto none = renderMulti(m, 4800);
    CHECK(rms(none.l, 0, none.l.size()) == 0.0);
}

// 5. Stereo recordings play in stereo, mono ones the same on both sides; a mono mix (no right
// buffer) is the mean of the two.
void testStereoZonesAndCompactSamples() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 2);
    m.setParam(ps::MultiSampler::VelTrack, 0.0f);
    zone(m, 0, make(sine(300, 0.4f, 1.0, 2, 500), 2, true), 0, 63, 60);
    zone(m, 1, make(sine(300, 0.4f, 1.0), 1, true), 64, 127, 64);
    CHECK(m.stereo());
    m.noteOn(60, 0.8f, 0);
    auto st = renderMulti(m, 9600);
    CHECK(std::fabs(toneHz(st.l, 960, 9600) - 300.0) < 6.0);
    CHECK(std::fabs(toneHz(st.r, 960, 9600) - 500.0) < 6.0);
    m.allNotesOff();
    renderMulti(m, kRate);
    m.noteOn(64, 0.8f, 0);
    auto mono = renderMulti(m, 9600);
    CHECK(mono.l == mono.r);
    // A 16-bit sample keeps a 16-bit recording exactly, at half the memory.
    std::vector<float> steps(4800);
    for (size_t i = 0; i < steps.size(); ++i) steps[i] = float(int(i % 2001) - 1000) / 32768.0f;
    ps::Sample* c = ps::Sample::createCompact(steps.data(), int64_t(steps.size()), 1, kRate, kRate);
    ps::Sample* f = ps::Sample::create(steps.data(), int64_t(steps.size()), 1, kRate, kRate);
    CHECK(c && c->compact() && c->data() == nullptr && c->frames() == 4800);
    bool exact = true;
    for (size_t i = 0; i < steps.size(); ++i) exact = exact && float(c->pcm16()[i]) / 32768.0f == steps[i];
    CHECK(exact);
    CHECK(c->bytes() * 2 == f->bytes());
    // The same zone, float or 16-bit: the same sound (to the 16-bit step).
    auto play = [&](bool compact) {
        ps::MultiSampler s;
        s.prepare(kRate);
        s.setParam(ps::MultiSampler::Count, 1);
        zone(s, 0, make(sine(440, 0.5f, 0.5), 1, compact), 0, 127, 69);
        s.noteOn(71, 0.8f, 0);
        return renderMulti(s, 9600);
    };
    const auto pf = play(false), pc = play(true);
    double worst = 0.0;
    for (size_t i = 0; i < pf.l.size(); ++i) worst = std::max(worst, double(std::fabs(pf.l[i] - pc.l[i])));
    CHECK(worst < 1e-4);
    delete c;
    delete f;
}

// 6. A zone replaced while it rings: the note goes on (its voice keeps its own reference) and
// the old recording is freed only by collectGarbage, once nobody plays it.
void testZoneReplacedWhilePlaying() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 1);
    ps::Sample* old = make(sine(440, 0.5f, 1.0), 1);
    old->retain();  // the test's own look at it
    zone(m, 0, old, 0, 127, 69);
    m.noteOn(69, 0.8f, 0);
    renderMulti(m, 4800);
    m.installZone(0, make(sine(880, 0.5f, 1.0), 1));
    auto out = renderMulti(m, 4800);
    CHECK(std::fabs(toneHz(out.l, 0, 4800) - 440.0) < 12.0);  // the old note rings on
    CHECK(old->refCount() == 2);                               // the test's and the voice's
    renderMulti(m, kRate);                                     // its recording ends: the voice lets go
    CHECK(m.activeVoices() == 0);
    CHECK(old->refCount() == 1);
    m.noteOn(69, 0.8f, 0);
    out = renderMulti(m, 4800);
    CHECK(std::fabs(toneHz(out.l, 0, 4800) - 880.0) < 12.0);
    m.collectGarbage();
    // Clearing every zone lets go of the recordings (the note ringing rings on).
    m.setParam(ps::MultiSampler::ClearZones, 1.0f);
    CHECK(!m.active());
    renderMulti(m, kRate);
    m.collectGarbage();
    CHECK(m.activeVoices() == 0);
    if (old->release()) delete old;
}

// 6b. A sustain loop: a held string note goes on past the end of its recording, without a
// click at the wrap, and dies away over the release once let go.
void testSustainLoop() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 1);
    m.setParam(ps::MultiSampler::Release, 0.2f);
    // 0.5 s of 440 Hz, looped on whole cycles between 0.1 s and 0.4 s (440 Hz: 109.09 frames a cycle).
    zone(m, 0, make(sine(440, 0.5f, 0.5), 1), 0, 127, 69);
    m.setZoneParam(0, ps::MultiSampler::LoopStart, 4800.0f);
    m.setZoneParam(0, ps::MultiSampler::LoopEnd, 18000.0f);  // 13200 frames: 121 whole cycles
    m.noteOn(69, 0.8f, 0);
    auto out = renderMulti(m, kRate * 2);
    CHECK(rms(out.l, kRate, kRate * 2) > 0.25);  // still sounding at 2 s, four times its recording
    float jump = 0.0f;
    for (size_t i = 1; i < out.l.size(); ++i) jump = std::max(jump, std::fabs(out.l[i] - out.l[i - 1]));
    std::printf("  sustain loop: largest step %.4f (a 440 Hz sine at 0.5 moves 0.029)\n", jump);
    CHECK(jump < 0.04f);
    m.noteOff(69, 0);
    auto tail = renderMulti(m, kRate / 2);
    CHECK(rms(tail.l, 14400, 24000) < 0.002);
    renderMulti(m, kRate / 2);
    CHECK(m.activeVoices() == 0);
}

// 7. Through the engine: a track's multizone source (SetSynthEngine 3, zones installed with
// InstallTrackSample's zone, set with SetTrackZone) plays its notes; a track whose instrument is
// not there yet (a download not done) plays its synth instead of silence.
void testMultizoneThroughTheEngine() {
    auto play = [&](bool zones) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, (1 << 16) | 3});  // track 1, multizone
        engine.post({ps::CommandType::SetTrackOn, 1.0f, 1});
        if (zones) {
            ps::Command c{ps::CommandType::InstallTrackSample, 0.0f, (1 << 16) | 1};  // zone 0
            c.sample = make(sine(440, 0.5f, 1.0, 2, 660), 2, true);
            engine.post(c);
            engine.post({ps::CommandType::SetTrackZone, 127.0f, (1 << 16) | ps::MultiSampler::HiKey, 0});
            engine.post({ps::CommandType::SetTrackZone, 69.0f, (1 << 16) | ps::MultiSampler::Root, 0});
            engine.post({ps::CommandType::SetTrackZone, 1.0f, (1 << 16) | ps::MultiSampler::Count, -1});
        }
        engine.post({ps::CommandType::SynthNoteOn, 0.9f, (1 << 16) | 69});
        auto out = render(engine, kRate / 2, 256);
        engine.collectGarbage();
        return out;
    };
    const auto withZones = play(true), fallback = play(false);
    std::vector<float> l, r, fl;
    for (size_t i = size_t(kLate + 2400) * 2; i + 1 < withZones.size(); i += 2) {
        l.push_back(withZones[i]);
        r.push_back(withZones[i + 1]);
        fl.push_back(fallback[i]);
    }
    std::printf("  through the engine: left %.0f Hz, right %.0f Hz; without its zones the synth plays (rms %.3f)\n",
                toneHz(l, 0, l.size()), toneHz(r, 0, r.size()), rms(fl, 0, fl.size()));
    CHECK(std::fabs(toneHz(l, 0, l.size()) - 440.0) < 8.0);
    CHECK(std::fabs(toneHz(r, 0, r.size()) - 660.0) < 8.0);  // a multizone instrument plays in stereo
    CHECK(rms(fl, 0, fl.size()) > 0.01);
}

// 8. Real-time rules: notes, zone changes and rendering never allocate; 32 stereo voices of
// 16-bit zones fit the CPU budget.
void testMultizoneAllocatesNothingAndFitsTheBudget() {
    ps::MultiSampler m;
    m.prepare(kRate);
    m.setParam(ps::MultiSampler::Count, 60);
    std::vector<ps::Sample*> spare;
    for (int i = 0; i < 60; ++i) {
        zone(m, i, make(sine(midiHz(36 + i), 0.3f, 4.0, 2, midiHz(36 + i) * 1.001), 2, true), 36 + i, 36 + i, float(36 + i));
        spare.push_back(make(sine(midiHz(36 + i), 0.3f, 0.2), 1, true));
    }
    std::vector<float> l(256), r(256);
#ifdef PS_COUNT_ALLOCATIONS
    g_allocations = 0;
    g_countAllocations = true;
#endif
    for (int k = 0; k < 40; ++k) m.noteOn(36 + k, 0.8f, k);
    m.render(l.data(), r.data(), 256);
    m.installZone(3, spare[3]);  // a zone replaced while its voice rings
    m.setZoneParam(3, ps::MultiSampler::Root, 39.0f);
    for (int k = 0; k < 40; ++k) m.noteOff(36 + k, 0);
    for (int k = 0; k < 40; ++k) m.noteOn(36 + k, 0.5f, 0);
    m.render(l.data(), r.data(), 256);
    m.allNotesOff();
    m.render(l.data(), r.data(), 256);
#ifdef PS_COUNT_ALLOCATIONS
    g_countAllocations = false;
    CHECK(g_allocations == 0);
#endif
    m.collectGarbage();
    // CPU: 32 voices (the most it plays) for 10 s of audio.
    for (int k = 0; k < 32; ++k) m.noteOn(40 + k, 0.8f, 0);
    const int frames = kRate * 10;
    const double t0 = threadSeconds();
    for (int at = 0; at < frames; at += 256) {
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        m.render(l.data(), r.data(), 256);
        if (at % (kRate * 3) == 0) {
            for (int k = 0; k < 32; ++k) m.noteOn(40 + k, 0.8f, 0);  // the 4 s recordings restart
        }
    }
    const double load = (threadSeconds() - t0) / 10.0;
    std::printf("  multizone: 32 stereo voices at %.2f %% of real time (budget 5 %%)\n", 100.0 * load);
    CHECK_TIMING(load < 0.05);
    for (size_t i = 0; i < spare.size(); ++i) {
        if (i != 3 && spare[i]->release()) delete spare[i];
    }
}

// 9. The 808 on an instrument track: its notes give its pitch, a key pressed while another is
// held slides to it (legato), a key let go fades it out quickly; the same voice as the pads'.
void testTrack808() {
    ps::Track808 b;
    b.prepare(kRate);
    auto run = [&](int frames) {
        std::vector<float> out(size_t(frames), 0.0f);
        for (int at = 0; at < frames; at += 256) b.render(out.data() + at, std::min(256, frames - at));
        return out;
    };
    b.noteOn(36, 0.9f, 0);
    auto out = run(kRate / 2);
    const double c1 = toneHz(out, 4800, out.size());
    std::printf("  808 track: C1 %.1f Hz\n", c1);
    CHECK(std::fabs(c1 - midiHz(36)) < 2.0);
    // Legato: G1 pressed while C1 is held slides there over the glide (80 ms), no new attack.
    b.noteOn(43, 0.9f, 0);
    auto slide = run(kRate / 2);
    const double early = toneHz(slide, 0, 1920), late = toneHz(slide, 9600, slide.size());
    std::printf("  808 track: slide %.1f Hz in the first 40 ms, %.1f Hz after\n", early, late);
    CHECK(early > midiHz(36) - 3.0 && early < midiHz(43) - 3.0);
    CHECK(std::fabs(late - midiHz(43)) < 2.5);
    // Let go of both: silence within the release (80 ms to -60 dB).
    b.noteOff(43, 0);
    b.noteOff(36, 0);
    auto tail = run(kRate / 4);
    float after = 0.0f;
    for (size_t i = 6000; i < tail.size(); ++i) after = std::max(after, std::fabs(tail[i]));
    CHECK(after < 1e-3f);
    CHECK(!b.sounding());
    // No glide setting: a new note starts afresh.
    b.setParam(ps::Track808::Glide, 0.0f);
    b.noteOn(36, 0.9f, 0);
    run(4800);
    b.noteOn(43, 0.9f, 0);
    auto jump = run(4800);
    CHECK(std::fabs(toneHz(jump, 960, jump.size()) - midiHz(43)) < 6.0);
    // Through the engine: track 2 as an 808 (source 4), its settings by SetTrack808Param.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetSynthEngine, 0.0f, (2 << 16) | 4});
    engine.post({ps::CommandType::SetTrackOn, 1.0f, 2});
    engine.post({ps::CommandType::SetTrack808Param, 0.9f, (2 << 16) | ps::Track808::Drive});
    engine.post({ps::CommandType::SynthNoteOn, 0.9f, (2 << 16) | 33});
    // (No allocation with it on a track: testProcessNeverAllocates plays one.)
    auto e = render(engine, kRate / 2, 256);
    std::vector<float> left;
    for (size_t i = size_t(kLate + 4800) * 2; i < e.size(); i += 2) left.push_back(e[i]);
    CHECK(rms(left, 0, left.size()) > 0.02);
    CHECK(std::fabs(toneHz(left, 0, left.size()) / midiHz(33) - 1.0) < 0.2);
}

}  // namespace multizone_tests

#define PS_MULTIZONE_TESTS                                                                         \
    {"p38_zone_selection", multizone_tests::testZoneSelection},                                    \
    {"p38_velocity_layers", multizone_tests::testVelocityLayers},                                  \
    {"p38_round_robin_deterministic", multizone_tests::testRoundRobinIsDeterministic},             \
    {"p38_release_samples", multizone_tests::testReleaseSamples},                                  \
    {"p38_stereo_and_compact_zones", multizone_tests::testStereoZonesAndCompactSamples},           \
    {"p38_zone_replaced_while_playing", multizone_tests::testZoneReplacedWhilePlaying},            \
    {"p38_sustain_loop", multizone_tests::testSustainLoop},                                        \
    {"p38_multizone_through_the_engine", multizone_tests::testMultizoneThroughTheEngine},          \
    {"p38_multizone_no_alloc_and_cpu", multizone_tests::testMultizoneAllocatesNothingAndFitsTheBudget}, \
    {"p38_track_808", multizone_tests::testTrack808},
