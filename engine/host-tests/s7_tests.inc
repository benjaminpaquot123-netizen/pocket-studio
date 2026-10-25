// S7 son (docs/son/banc/S7.md): the widener's spread (a mono-compatible stereo chorus made from the
// sound itself, insert 11 param 2) and mix version 4 (the Drop mode without the master's low cut).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace s7_tests {

using studio_tests::allFinite;
using studio_tests::bandEnergy;

// A mono test sound (the same in both channels): a 70 Hz bass and a 660 Hz tone with a little noise.
void monoSound(std::vector<float>& l, std::vector<float>& r, size_t n) {
    l.assign(n, 0.0f);
    r.assign(n, 0.0f);
    uint32_t seed = 12345u;
    for (size_t i = 0; i < n; ++i) {
        seed = seed * 1664525u + 1013904223u;
        const float noise = (static_cast<float>(seed >> 8) / 16777216.0f - 0.5f) * 0.05f;
        const double t = static_cast<double>(i) / kRate;
        const float v = 0.3f * static_cast<float>(std::sin(2 * M_PI * 70.0 * t)) + 0.2f * static_cast<float>(std::sin(2 * M_PI * 660.0 * t)) + noise;
        l[i] = r[i] = v;
    }
}

void runSlot(ps::InsertSlot& fx, std::vector<float>& l, std::vector<float>& r, int block) {
    for (size_t pos = 0; pos < l.size(); pos += static_cast<size_t>(block)) {
        const int n = static_cast<int>(std::min<size_t>(static_cast<size_t>(block), l.size() - pos));
        fx.process(l.data() + pos, r.data() + pos, n);
    }
}

void testWidenerSpread() {
    const size_t n = size_t(kRate) * 4;
    std::vector<float> l, r, inL, inR;
    monoSound(inL, inR, n);
    // Spread 0: bit for bit the widener as it always was (old projects sound the same).
    {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(ps::InsertSlot::WidenerFx);
        fx.setParam(0, 0.8f);
        fx.setParam(1, 0.3f);
        std::vector<float> sl = inL, sr = inR;
        for (size_t i = 0; i < n; ++i) sl[i] += 0.1f * static_cast<float>(std::sin(2 * M_PI * 300.0 * double(i) / kRate));  // some side
        l = sl;
        r = sr;
        runSlot(fx, l, r, 192);
        // The same arithmetic as before S7, here.
        float lowS = 0.0f;
        const float coef = 1.0f - std::exp(-2.0f * static_cast<float>(M_PI) * (40.0f * std::pow(300.0f / 40.0f, 0.3f)) / static_cast<float>(kRate));
        bool same = true;
        for (size_t i = 0; i < n && same; ++i) {
            const float m = 0.5f * (sl[i] + sr[i]);
            float s = 0.5f * (sl[i] - sr[i]);
            lowS += (s - lowS) * coef;
            s -= lowS;
            s *= 1.6f;
            same = l[i] == m + s && r[i] == m - s;
        }
        CHECK(same);
    }
    // Spread 0.6 on a mono sound: a side of its own, from 250 Hz up; the lows stay in the centre and
    // L + R stays the sound (a mono speaker hears no comb).
    auto spreadOf = [&](float spread, int block, double& sideHighDb, double& sideLowDb, double& monoErr) {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(ps::InsertSlot::WidenerFx);
        fx.setParam(0, 0.5f);
        fx.setParam(2, spread);
        l = inL;
        r = inR;
        runSlot(fx, l, r, block);
        CHECK(allFinite(l) && allFinite(r));
        std::vector<float> mid(n), side(n);
        monoErr = 0.0;
        for (size_t i = 0; i < n; ++i) {
            mid[i] = 0.5f * (l[i] + r[i]);
            side[i] = 0.5f * (l[i] - r[i]);
            monoErr = std::max(monoErr, std::fabs(double(mid[i]) - inL[i]));
        }
        const size_t from = size_t(kRate), to = n;
        sideHighDb = 10.0 * std::log10(std::max(1e-30, bandEnergy(side, from, to, 500.0, 900.0)) / std::max(1e-30, bandEnergy(mid, from, to, 500.0, 900.0)));
        sideLowDb = 10.0 * std::log10(std::max(1e-30, bandEnergy(side, from, to, 50.0, 90.0)) / std::max(1e-30, bandEnergy(mid, from, to, 50.0, 90.0)));
    };
    double hi = 0, lo = 0, err = 0;
    spreadOf(0.6f, 192, hi, lo, err);
    std::printf("  widener spread 0.6: side/centre %+.1f dB at 660 Hz, %+.1f dB at 70 Hz, mono error %.2g\n", hi, lo, err);
    CHECK(hi > -12.0 && hi < 0.0);  // wide, not wider than the centre
    CHECK(lo < -20.0);              // the bass stays mono
    CHECK(err < 1e-5);              // L + R untouched
    // The block size does not change it (the taps move once a block, interpolated in between).
    double hi2 = 0, lo2 = 0, err2 = 0;
    spreadOf(0.6f, 37, hi2, lo2, err2);
    CHECK(std::fabs(hi2 - hi) < 0.5);
    // More spread, more side.
    double hi3 = 0, lo3 = 0, err3 = 0;
    spreadOf(1.0f, 192, hi3, lo3, err3);
    CHECK(hi3 > hi + 2.0);
    // A NaN reset (the slot's reset) clears the line: silence in, silence out.
    {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(ps::InsertSlot::WidenerFx);
        fx.setParam(2, 1.0f);
        l = inL;
        r = inR;
        runSlot(fx, l, r, 192);
        fx.reset();
        std::vector<float> zl(4800, 0.0f), zr(4800, 0.0f);
        runSlot(fx, zl, zr, 192);
        double e = 0;
        for (size_t i = 0; i < zl.size(); ++i) e += std::fabs(double(zl[i])) + std::fabs(double(zr[i]));
        CHECK(e == 0.0);
    }
}

// SetMixVersion 4 (S7): the Drop mode, without the master's low cut (version 3 only).
void testMixVersion4() {
    ps::Engine e(kRate);
    auto step = [&] { render(e, 256, 256); };
    e.post({ps::CommandType::SetMixVersion, 4.0f});
    step();
    CHECK(e.mixer().masterChain().mode() == ps::MasterChain::Mode::Drop);
    CHECK(!e.mixer().masterChain().lowCut());
    e.post({ps::CommandType::SetMixVersion, 3.0f});
    step();
    CHECK(e.mixer().masterChain().lowCut());
    e.post({ps::CommandType::SetMixVersion, 1.0f});
    step();
    CHECK(e.mixer().masterChain().mode() == ps::MasterChain::Mode::Song);
}

// renderForReport (the live gain's estimate, S6 review): the same report as renderToBuffer, without
// the samples (Drop mode, two passes: the drive found, the loud part measured).
void testRenderForReport() {
    const int64_t frames = int64_t(kRate) * 6;
    auto prepared = [](ps::OfflineRenderer& r) {
        prepareLoudLoop(r, 0.5f);
        r.post({ps::CommandType::SetMixVersion, 4.0f});
        r.post({ps::CommandType::SetMaster, -9.0f, 4});
    };
    ps::OfflineRenderer a(kRate), b(kRate);
    prepared(a);
    prepared(b);
    const auto out = a.renderToBuffer(frames, 0);
    CHECK(!out.empty());
    CHECK(b.renderForReport(frames, 0) == ps::OfflineRenderer::Result::Ok);
    const auto& ra = a.report();
    const auto& rb = b.report();
    std::printf("  report only: drive %+.2f dB (buffer %+.2f), loud part %.2f LUFS (%.2f)\n", rb.driveDb, ra.driveDb, rb.loudLufs, ra.loudLufs);
    CHECK(std::fabs(ra.driveDb - rb.driveDb) < 1e-4);
    CHECK(std::fabs(ra.lufs - rb.lufs) < 1e-6);
    CHECK(std::fabs(ra.loudLufs - rb.loudLufs) < 1e-6);
    CHECK(ra.frames == rb.frames && rb.frames == frames);
}

// Mix version 4, live: the master's fader before the chain, as an export has it. A tone pushed 12 dB
// into the limiter with the fader at -12 dB: before S7 (versions 1-3) the limiter flattens it and the
// fader takes 12 dB off what it left; version 4 lets it through the limiter at the level an export
// writes. An export with a loudness target always had it so (its chain masters the stored music);
// one without (a single pass) follows the version, as the live engine does.
void testFaderBeforeChain() {
    using ps::CommandType;
    auto setUp = [](auto& target, float version) {
        target.post({CommandType::SetMixVersion, version});
        target.post({CommandType::SetTempo, 120.0f});
        std::vector<float> tone(kRate * 4);
        for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.5f * static_cast<float>(std::sin(2.0 * M_PI * 220.0 * double(i) / kRate));
        target.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(tone.data(), static_cast<int64_t>(tone.size()), 1, kRate, kRate)});
        target.post({CommandType::SetDrumHit, 1.0f, 5, 0});
        target.post({CommandType::SetMaster, 4.0f, 0});   // +12 dB into the chain
        target.post({CommandType::SetMaster, 1.0f, 1});   // the limiter on
        target.post({CommandType::SetChannelVolume, 0.4f, ps::Mixer::kMaster});  // the fader at -12 dB
    };
    auto peakOf = [](const std::vector<float>& out, size_t from, size_t to) {
        float p = 0.0f;
        for (size_t i = from * 2; i < std::min(out.size(), to * 2); ++i) p = std::max(p, std::fabs(out[i]));
        return 20.0 * std::log10(std::max(1e-9f, p));
    };
    auto live = [&](float version) {
        ps::Engine e(kRate);
        setUp(e, version);
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        const auto out = render(e, kRate * 3, 256);
        CHECK(e.mixer().faderBeforeChain() == (version > 3.5f));
        return peakOf(out, kRate, kRate * 3);
    };
    auto exported = [&](float version) {
        ps::OfflineRenderer r(kRate);
        setUp(r, version);
        return peakOf(r.renderToBuffer(kRate * 3, 0), kRate, kRate * 3);
    };
    const double v2 = live(2.0f), v4 = live(4.0f), e2 = exported(2.0f), e4 = exported(4.0f);
    std::printf("  fader -12 dB, limiter pushed: live peak %.1f dBFS (version 2), %.1f (version 4); export %.1f / %.1f\n", v2, v4, e2, e4);
    CHECK(v4 > v2 + 6.0);
    CHECK(std::fabs(v4 - e4) < 0.5);       // live = export
    CHECK(std::fabs(v2 - e2) < 1.5);       // before S7 too, without a loudness target (one pass)
}

// MasterChain::setKnownLevel (mix version 4, live): a part quieter than the loud part the known drive
// was found on, limited past the cap at that drive (as the export limits it), leaves the drive where
// it is; the same loss on a part as loud as that one still brings it down (a louder mix since).
void testKnownLevel() {
    auto run = [](float knownLevel) {
        ps::MasterChain chain;
        chain.prepare(kRate);
        chain.setMode(ps::MasterChain::Mode::Drop);
        chain.setTarget(-8.0f);
        chain.setKnownLevel(knownLevel);
        chain.setKnownDrive(12.0f);
        std::vector<float> l(480), r(480);
        for (int b = 0; b < 2000; ++b) {  // 20 s of a part pushed hard into the limiter
            for (int i = 0; i < 480; ++i) l[size_t(i)] = r[size_t(i)] = 0.6f * std::sin(0.05f * float(b * 480 + i));  // about -5 LUFS
            chain.process(l.data(), r.data(), nullptr, 480);
        }
        return std::pair<float, float>(chain.driveDb(), chain.lossDb());
    };
    const auto none = run(NAN), quieter = run(-2.0f), same = run(-5.0f);
    std::printf("  S7 known level: drive %+.2f dB (loss %.2f) without it, %+.2f with a louder known part, %+.2f with this part's level\n",
                double(none.first), double(none.second), double(quieter.first), double(same.first));
    CHECK(none.first < 11.0f);                              // as in S6: past the cap, it comes down
    CHECK(std::fabs(quieter.first - 12.0f) < 0.01f);        // a quieter part than the export's loud one: held
    CHECK(same.first < 11.0f);                              // the export's own loud part, louder now: down
    ps::MasterChain chain;
    chain.setKnownLevel(-9.0f);
    chain.resetEstimate();  // another project
    CHECK(std::isnan(chain.knownLevel()));
}

}  // namespace s7_tests

#define PS_S7_TESTS \
    {"widener_spread", s7_tests::testWidenerSpread}, \
    {"mix_version_4", s7_tests::testMixVersion4}, \
    {"render_for_report", s7_tests::testRenderForReport}, \
    {"fader_before_chain", s7_tests::testFaderBeforeChain}, \
    {"known_level", s7_tests::testKnownLevel},
