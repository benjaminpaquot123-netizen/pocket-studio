// Phase 34, S5 (docs/PLAN-SON-PRO.md): an honest mix and master. The sidechain keyed by the
// kick's audio (any pad, with or without a strip of its own), the « Musique » bus that ducks once,
// the soft clipper, the loudness target aimed at the loudest part (Drop mode) with its 3 LU cap,
// live and in an export, and the live drive that settles. Included by engine_tests.cpp (kept
// apart to ease merging).

namespace mix_tests {

// The level of channel 0 of `out` (interleaved stereo) over [from, from + len), RMS.
double rmsOf(const std::vector<float>& out, size_t from, size_t len) {
    double s = 0.0;
    for (size_t i = from; i < from + len; ++i) s += double(out[i * 2]) * out[i * 2];
    return std::sqrt(s / double(len));
}

// A held synth note on the synth track (channel 2), the kick on pad `pad` on steps 0 and 8 at
// 120 BPM (a hit every second), the drums' strip muted (only the ducked sound is heard; the key
// still sounds). `strip`: the kick pad plays through a strip of its own.
void prepareKeyed(ps::Engine& engine, int pad, bool strip) {
    engine.post({ps::CommandType::SetKitVersion, 3.0f});  // the studio kick on pad 0 (0.32 s)
    engine.post({ps::CommandType::SetDrumHit, 1.0f, pad, 0});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, pad, 8});
    if (pad != 0) engine.post({ps::CommandType::SetPadVoice, 2.0f, pad});  // the studio kick on that pad
    engine.post({ps::CommandType::SetChannelMute, 1.0f, 0});
    if (strip) engine.post({ps::CommandType::SetPadStrip, 1.0f, pad});
    engine.post({ps::CommandType::SynthNoteOn, 0.8f, 45});
}

// A kick on any pad ducks a keyed Pump by its depth within 10 ms of the hit (0.5: -6 dB), and the
// sound is back by the next kick; with the key on another pad (silent there) nothing moves.
void testKeyedPumpFollowsAnyKick() {
    for (int pad : {0, 5}) {
        for (bool strip : {false, true}) {
            auto take = [&](bool pump, int keyPad) {
                ps::Engine engine(kRate);
                prepareKeyed(engine, pad, strip);
                const int slot = 2 * ps::Mixer::kInserts;
                if (pump) {
                    engine.post({ps::CommandType::SetInsertType, 0.0f, slot, ps::InsertSlot::Pump});
                    engine.post({ps::CommandType::SetInsertParam, 0.5f, slot, 0});  // depth 0.5
                    engine.post({ps::CommandType::SetInsertParam, 0.3f, slot, 1});  // release ~110 ms
                    engine.post({ps::CommandType::SetInsertParam, 1.0f, slot, 3});  // the audio key
                    engine.post({ps::CommandType::SetSidechain, float(ps::Mixer::kFirstPad + keyPad), 0});
                }
                render(engine, kRate / 2, 256);
                engine.post({ps::CommandType::SetTransport, 1.0f, 0});
                return render(engine, kRate * 2, 256);
            };
            const auto plain = take(false, pad), keyed = take(true, pad), other = take(true, (pad + 1) % 16);
            const size_t hit = size_t(kRate) + size_t(kLate);  // the second kick, as heard
            auto ratioDb = [&](const std::vector<float>& x, size_t from, size_t len) { return toDb(rmsOf(x, from, len) / rmsOf(plain, from, len)); };
            const double at5 = ratioDb(keyed, hit + 240, 480);    // 5..15 ms after the hit
            const double at10 = ratioDb(keyed, hit + 480, 480);   // 10..20 ms
            const double later = ratioDb(keyed, hit + 21600, 2400);  // 450..500 ms
            std::printf("  keyed pump, kick on pad %d%s: %.1f dB at 5 ms, %.1f dB at 10 ms, %.1f dB 450 ms later\n", pad,
                        strip ? " (own strip)" : "", at5, at10, later);
            CHECK(at10 <= -4.0 && at10 >= -8.0);
            CHECK(at5 <= -3.0);
            CHECK(later > -1.0);
            // The key on a pad that does not play: the sound goes through.
            CHECK(std::fabs(ratioDb(other, hit + 480, 480)) < 0.1);
        }
    }
}

// The Musique bus: two tracks in it duck together, once, under the key; its lows (a 55 Hz tone)
// deeper than the rest (a 2 kHz tone) with a low depth; at rest (no depth) the mix is the plain
// one (to float rounding); its members' sends and the returns still reach the mix.
void testMusicBusDucksOnce() {
    auto build = [](ps::Engine& e, bool group, float depth, float low) {
        e.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
        e.post({ps::CommandType::SetDrumHit, 1.0f, 0, 8});
        e.post({ps::CommandType::SetChannelMute, 1.0f, 0});
        e.post({ps::CommandType::SetTrackOn, 1.0f, 1});
        e.post({ps::CommandType::SynthNoteOn, 0.8f, 33});             // 55 Hz on the synth track (channel 2)
        e.post({ps::CommandType::SynthNoteOn, 0.6f, 107 | (1 << 16)});  // ~3.9 kHz on track 2 (channel 5)
        if (group) {
            e.post({ps::CommandType::SetSidechain, float(ps::Mixer::kFirstPad), 0});
            e.post({ps::CommandType::SetSidechain, 1.0f, 4, 2});
            e.post({ps::CommandType::SetSidechain, 1.0f, 4, 5});
            e.post({ps::CommandType::SetSidechain, depth, 1});
            e.post({ps::CommandType::SetSidechain, low, 2});
            e.post({ps::CommandType::SetSidechain, 0.3f, 3});
        }
    };
    // At rest: the same mix.
    {
        ps::Engine a(kRate), b(kRate);
        build(a, false, 0.0f, 0.0f);
        build(b, true, 0.0f, 0.0f);
        render(a, kRate / 2, 256);
        render(b, kRate / 2, 256);
        a.post({ps::CommandType::SetTransport, 1.0f, 0});
        b.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto x = render(a, kRate * 2, 256), y = render(b, kRate * 2, 256);
        double worst = 0.0;
        for (size_t i = 0; i < x.size(); ++i) worst = std::max(worst, double(std::fabs(x[i] - y[i])));
        std::printf("  Musique bus at rest: largest difference %.2g\n", worst);
        CHECK(worst < 1e-5);
    }
    // Ducked: lows by 0.8 (-14 dB), the rest by 0.2 (-1.9 dB); against the same song without the
    // bus (the synths' own envelopes move a little too).
    auto take = [&](bool group) {
        ps::Engine e(kRate);
        build(e, group, 0.2f, 0.8f);
        render(e, kRate / 2, 256);
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto out = render(e, kRate * 2, 256);
        std::vector<float> mono(out.size() / 2);
        for (size_t i = 0; i < mono.size(); ++i) mono[i] = out[i * 2];
        return mono;
    };
    const auto plain = take(false), ducked = take(true);
    const size_t hit = size_t(kRate) + size_t(kLate);
    auto band = [&](const std::vector<float>& x, double lo, double hi) { return studio_tests::bandEnergy(x, hit + 480, hit + 1680, lo, hi); };
    const double lowDrop = 10.0 * std::log10(band(ducked, 40, 70) / band(plain, 40, 70));
    const double highDrop = 10.0 * std::log10(band(ducked, 3800, 4000) / band(plain, 3800, 4000));
    std::printf("  Musique bus ducked: lows %.1f dB, highs %.1f dB (10 ms after the kick)\n", lowDrop, highDrop);
    CHECK(lowDrop < -8.0);
    CHECK(highDrop < -0.8 && highDrop > -4.0);
}

// The soft clipper: a steady sine (3 dB of crest) under its margin goes through (the input, a few
// frames late); short peaks 12 dB over a held level are brought under the ceiling (level + margin).
void testSoftClipper() {
    ps::SoftClipper c;
    c.prepare(float(kRate));
    c.setParam(0, 0.25f);  // margin 6 dB
    c.setParam(1, 0.5f);
    c.setParam(2, 1.0f);
    std::vector<float> l(size_t(kRate) * 2), r;
    for (size_t i = 0; i < l.size(); ++i) l[i] = 0.25f * float(std::sin(2 * M_PI * 110.0 * double(i) / kRate));
    r = l;
    const auto in = l;
    for (size_t pos = 0; pos < l.size(); pos += 256) c.process(l.data() + pos, r.data() + pos, int(std::min<size_t>(256, l.size() - pos)));
    double err = 0.0, ref = 0.0;
    const int d = ps::DriveShaper<ps::Oversampler4x>::kDelay;
    for (size_t i = size_t(kRate); i < l.size(); ++i) {
        err += std::pow(double(l[i]) - double(in[i - size_t(d)]), 2.0);
        ref += double(in[i]) * in[i];
    }
    std::printf("  soft clipper, sine under its margin: difference %.1f dB\n", 10.0 * std::log10(err / ref));
    CHECK(10.0 * std::log10(err / ref) < -50.0);
    // A 0.1 held tone with 2 ms bursts to 0.4 every 100 ms (12 dB): out under ~0.1 x 2 (6 dB) x 1.1.
    ps::SoftClipper p;
    p.prepare(float(kRate));
    p.setParam(0, 0.25f);
    p.setParam(1, 0.5f);
    p.setParam(2, 1.0f);
    std::vector<float> x(size_t(kRate) * 2), y;
    for (size_t i = 0; i < x.size(); ++i) {
        const bool burst = (i % 4800) < 96;
        x[i] = (burst ? 0.4f : 0.1f) * float(std::sin(2 * M_PI * 220.0 * double(i) / kRate));
    }
    y = x;
    for (size_t pos = 0; pos < x.size(); pos += 256) p.process(x.data() + pos, y.data() + pos, int(std::min<size_t>(256, x.size() - pos)));
    float peak = 0.0f;
    for (size_t i = size_t(kRate); i < x.size(); ++i) peak = std::max(peak, std::fabs(x[i]));
    std::printf("  soft clipper, 12 dB bursts over a 0.1 tone: peak %.3f (ceiling %.3f)\n", double(peak), double(p.lastCeiling()));
    CHECK(peak < 0.26f && peak > 0.12f);
    bool finite = true;
    for (float v : x) finite &= std::isfinite(v);
    CHECK(finite);
}

// A song for the Drop mode: a kick, hats and the acid line at a quiet level for 30 s, then 30 s
// three times as loud (the Drop), then the quiet part again. `dense`: the Drop adds a held, loud
// square-ish pad (the chain cannot get it louder without squashing).
void prepareDropSong(ps::OfflineRenderer& r, float target, bool drop, bool dense) {
    r.post({ps::CommandType::SetTempo, 128.0f});
    for (int st : {0, 4, 8, 12}) r.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, st});
    for (int st = 2; st < 16; st += 4) r.post({ps::CommandType::SetDrumHit, 0.8f, ps::DrumKit::ClosedHat, st});
    for (int st = 0; st < 16; ++st) {
        r.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 5) % 12, 0, st % 4 == 2, false, true}.pack()});
    }
    r.post({ps::CommandType::SetMaster, 0.5f, 0});
    r.post({ps::CommandType::SetMixVersion, drop ? 2.0f : 1.0f});
    r.post({ps::CommandType::SetMaster, target, 4});
    r.post({ps::CommandType::SetChannelVolume, 0.45f, 0});
    r.post({ps::CommandType::SetChannelVolume, 0.45f, 1});
    if (dense) {
        r.post({ps::CommandType::SetChannelVolume, 0.0f, 2});
        r.post({ps::CommandType::SynthNoteOn, 1.0f, 40});
        r.post({ps::CommandType::SynthNoteOn, 1.0f, 47});
    }
    r.scheduleFrom(int64_t(kRate) * 30);
    r.post({ps::CommandType::SetChannelVolume, 0.8f, 0});
    r.post({ps::CommandType::SetChannelVolume, 0.8f, 1});
    if (dense) r.post({ps::CommandType::SetChannelVolume, 1.0f, 2});
    r.scheduleFrom(int64_t(kRate) * 60);
    r.post({ps::CommandType::SetChannelVolume, 0.45f, 0});
    r.post({ps::CommandType::SetChannelVolume, 0.45f, 1});
    if (dense) r.post({ps::CommandType::SetChannelVolume, 0.0f, 2});
    r.scheduleFrom(-1);
}

double loudPartOf(const std::vector<float>& out) {
    ps::LoudPartMeter m;
    m.prepare(kRate);
    for (size_t i = 0; i + 1 < out.size(); i += 2) m.add(out[i], out[i + 1]);
    return m.lufs();
}

double sectionLufs(const std::vector<float>& out, size_t from, size_t to) {
    ps::LoudnessMeter m;
    m.prepare(kRate);
    for (size_t i = from * 2; i + 1 < to * 2 && i + 1 < out.size(); i += 2) m.add(out[i], out[i + 1]);
    return m.integratedLufs();
}

// The export in Drop mode: the Drop (its own loudness, and LoudPart's) at the target, the song's
// integrated loudness under it; the chain's loss within 3 LU; true peak under the ceiling. A Drop
// too dense to get there cleanly stays under the target with 3 LU taken, no more. Song mode (the
// projects saved before) still brings the whole song to the target.
void testDropTargetExport() {
    const int64_t frames = int64_t(kRate) * 90;
    {
        ps::OfflineRenderer r(kRate);
        prepareDropSong(r, -9.0f, true, false);
        const auto out = r.renderToBuffer(frames, 0);
        const double drop = sectionLufs(out, size_t(kRate) * 33, size_t(kRate) * 59), whole = sectionLufs(out, 0, out.size() / 2);
        const auto& rep = r.report();
        std::printf("  Drop mode -9: Drop %.2f LUFS (loud part %.2f, report %.2f), song %.2f, drive %+.1f dB, loss %.2f LU, TP %.2f\n",
                    drop, loudPartOf(out), rep.loudLufs, whole, double(rep.driveDb), double(rep.lossDb), toDb(independentTruePeak(out)));
        CHECK(rep.mode == 1);
        CHECK(std::fabs(drop + 9.0) <= 0.5);
        CHECK(std::fabs(rep.loudLufs + 9.0) <= 0.2);
        CHECK(whole < drop - 1.0);
        CHECK(rep.lossDb <= ps::MasterChain::kMaxLossDropDb + 0.1f);
        CHECK(toDb(independentTruePeak(out)) <= -1.0);
    }
    {
        ps::OfflineRenderer r(kRate);
        prepareDropSong(r, -5.0f, true, true);
        const auto out = r.renderToBuffer(frames, 0);
        const auto& rep = r.report();
        std::printf("  Drop mode -5, dense: loud part %.2f LUFS, drive %+.1f dB, loss %.2f LU\n", rep.loudLufs, double(rep.driveDb), double(rep.lossDb));
        CHECK(rep.lossDb <= ps::MasterChain::kMaxLossDropDb + 0.1f);
        CHECK(rep.lossDb >= ps::MasterChain::kMaxLossDropDb - 0.6f);
        CHECK(rep.loudLufs < -5.2);
        CHECK(toDb(independentTruePeak(out)) <= -1.0);
    }
    {
        ps::OfflineRenderer r(kRate);
        prepareDropSong(r, -9.0f, false, false);
        const auto out = r.renderToBuffer(frames, 0);
        std::printf("  Song mode -9: song %.2f LUFS, loss %.2f LU\n", sectionLufs(out, 0, out.size() / 2), double(r.report().lossDb));
        CHECK(r.report().mode == 0);
        CHECK(std::fabs(sectionLufs(out, 0, out.size() / 2) + 9.0) <= 0.5);
    }
}

// Live, Drop mode: the drive comes to the target over the first loud part heard, then settles: the
// mix turned 6 dB down for 30 s does not bring it up (no drift while one mixes), a louder part
// brings it down; the loss it reports stays within the cap; an export's drive is taken as known.
void testLiveDropDriveSettles() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 128.0f});
    for (int st : {0, 4, 8, 12}) engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, st});
    for (int st = 2; st < 16; st += 4) engine.post({ps::CommandType::SetDrumHit, 0.8f, ps::DrumKit::ClosedHat, st});
    for (int st = 0; st < 16; ++st) {
        engine.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 5) % 12, 0, st % 4 == 2, false, true}.pack()});
    }
    engine.post({ps::CommandType::SetMaster, 0.5f, 0});
    engine.post({ps::CommandType::SetMixVersion, 2.0f});
    engine.post({ps::CommandType::SetMaster, -9.0f, 4});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto run = [&](int pieces, float& worstStep) {
        float last = engine.mixer().masterChain().driveDb();
        std::vector<float> all;
        for (int k = 0; k < pieces; ++k) {
            auto out = render(engine, kRate / 10, 240);
            const float d = engine.mixer().masterChain().driveDb();
            worstStep = std::max(worstStep, std::fabs(d - last));
            last = d;
            all.insert(all.end(), out.begin(), out.end());
        }
        return all;
    };
    float step = 0.0f;
    const auto first = run(450, step);  // 45 s
    const float settled = engine.mixer().masterChain().driveDb();
    const double lastLoud = sectionLufs(first, first.size() / 2 - size_t(kRate) * 10, first.size() / 2);
    engine.post({ps::CommandType::SetChannelVolume, 0.4f, 0});  // the mix 6 dB down
    engine.post({ps::CommandType::SetChannelVolume, 0.4f, 1});
    const float levelBefore = engine.mixer().masterChain().loudLevel();
    run(300, step);
    const float afterQuiet = engine.mixer().masterChain().driveDb();
    const float levelQuiet = engine.mixer().masterChain().loudLevel();
    engine.post({ps::CommandType::SetChannelVolume, 1.0f, 0});  // louder than ever: +2 dB
    engine.post({ps::CommandType::SetChannelVolume, 1.0f, 1});
    run(200, step);
    const float afterLoud = engine.mixer().masterChain().driveDb();
    std::printf("  loud part heard: %.2f, after the quiet mix %.2f, after the louder one %.2f LUFS\n", double(levelBefore), double(levelQuiet),
                double(engine.mixer().masterChain().loudLevel()));
    const float loss = engine.mixer().masterChain().lossDb();
    std::printf("  live Drop mode -9: settled at %+.1f dB (last 10 s %.2f LUFS); mix 6 dB down: %+.1f dB; louder: %+.1f dB; loss %.2f LU;"
                " largest step %.2f dB / 100 ms\n", double(settled), lastLoud, double(afterQuiet), double(afterLoud), double(loss), double(step));
    CHECK(std::fabs(lastLoud + 9.0) <= 1.0);
    CHECK(afterQuiet <= settled + 0.05f);
    CHECK(afterLoud < afterQuiet - 1.0f);
    CHECK(loss <= ps::MasterChain::kMaxLossDropDb + 0.5f);
    CHECK(step <= 0.201f);
    // The drive of an export, as known: there, and settled (a quieter mix does not move it).
    const float known = afterLoud + 0.5f;
    engine.post({ps::CommandType::SetMaster, known, 8});
    run(30, step);
    CHECK(std::fabs(engine.mixer().masterChain().driveDb() - known) < 0.01f);
    engine.post({ps::CommandType::SetChannelVolume, 0.6f, 0});
    engine.post({ps::CommandType::SetChannelVolume, 0.6f, 1});
    run(100, step);
    CHECK(std::fabs(engine.mixer().masterChain().driveDb() - known) < 0.01f);
}

// Everything S5 at once in the live engine (keyed pumps, the Musique bus with its returns, the
// soft clipper, the Drop-mode target): no allocation in 300 blocks, nothing non-finite.
void testS5NeverAllocates() {
#ifdef PS_COUNT_ALLOCATIONS
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 126.0f});
    for (int st : {0, 4, 8, 12}) engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
    for (int st = 2; st < 16; st += 4) engine.post({ps::CommandType::SetDrumHit, 0.8f, ps::DrumKit::ClosedHat, st});
    engine.post({ps::CommandType::SetTrackOn, 1.0f, 1});
    for (int n = 0; n < 16; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n, ps::NoteTrack::pack(n * 24, 20, 36 + n % 5)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 16});
    for (int n = 0; n < 8; ++n) engine.post({ps::CommandType::SetSynthNote, 0.7f, n | (1 << 16), ps::NoteTrack::pack(n * 48, 40, 60 + n % 7)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 8 | (1 << 16)});
    const int bass = 2 * ps::Mixer::kInserts;
    engine.post({ps::CommandType::SetInsertType, 0.0f, bass, ps::InsertSlot::ClipperFx});
    engine.post({ps::CommandType::SetInsertType, 0.0f, bass + 1, ps::InsertSlot::Pump});
    engine.post({ps::CommandType::SetInsertParam, 1.0f, bass + 1, 3});
    engine.post({ps::CommandType::SetSidechain, float(ps::Mixer::kFirstPad), 0});
    engine.post({ps::CommandType::SetSidechain, 1.0f, 4, 5});
    engine.post({ps::CommandType::SetSidechain, 1.0f, 5, 0});
    engine.post({ps::CommandType::SetSidechain, 0.3f, 1});
    engine.post({ps::CommandType::SetSidechain, 0.6f, 2});
    engine.post({ps::CommandType::SetSend, 0.3f, 5, 0});
    engine.post({ps::CommandType::SetMixVersion, 2.0f});
    engine.post({ps::CommandType::SetMaster, -8.0f, 4});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(512 * 2);
    for (int b = 0; b < 400; ++b) engine.process(nullptr, 0, 0, out.data(), 192, 2);  // warm-up (40 s of estimate)
    g_allocations = 0;
    g_countAllocations = true;
    bool finite = true;
    for (int b = 0; b < 300; ++b) {
        engine.process(nullptr, 0, 0, out.data(), 192, 2);
        for (int i = 0; i < 384; ++i) finite &= std::isfinite(out[static_cast<size_t>(i)]);
    }
    g_countAllocations = false;
    std::printf("  S5: %d allocations in 300 blocks\n", g_allocations.load());
    CHECK(g_allocations.load() == 0);
    CHECK(finite);
#endif
}

}  // namespace mix_tests

#define PS_MIX_TESTS                                                                \
    {"s5_keyed_pump_follows_any_kick", mix_tests::testKeyedPumpFollowsAnyKick},    \
    {"s5_music_bus_ducks_once", mix_tests::testMusicBusDucksOnce},                  \
    {"s5_soft_clipper", mix_tests::testSoftClipper},                                \
    {"s5_drop_target_export", mix_tests::testDropTargetExport},                     \
    {"s5_live_drop_drive_settles", mix_tests::testLiveDropDriveSettles},            \
    {"s5_never_allocates", mix_tests::testS5NeverAllocates},
