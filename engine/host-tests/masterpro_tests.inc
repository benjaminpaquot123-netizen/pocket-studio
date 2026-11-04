// Lot E (« Master pro », PLAN-AUDITS-FINAUX §1 bis): the pro readings (LoudnessRange.h), « écoute A/B »
// and the reference at the same loudness (MasterCompare.h), the stems that add up to the mix (dry
// strips plus each send's return), the file's finish (fade-in, trims, silence after, a selection,
// 96 kHz, 16 bits with shaped dither), the limiter's attack and release, the integrated target, and
// none of it allocating on the audio thread. Included by engine_tests.cpp (after master_tests.inc).

#include "LoudnessRange.h"

namespace masterpro {

// A loop that keeps the limiter busy (master_tests' drums and 303), on a live engine.
static void loudLoop(ps::Engine& e, float masterGain) {
    e.post({ps::CommandType::SetTempo, 128.0f});
    for (int st : {0, 4, 8, 12}) e.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, st});
    for (int st : {4, 12}) e.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Snare, st});
    for (int st = 0; st < 16; st += 2) e.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::ClosedHat, st});
    for (int st = 0; st < 16; ++st) {
        e.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 5) % 12, st % 7 == 3 ? 1 : 0, st % 4 == 2, st % 6 == 5, st % 8 != 7}.pack()});
    }
    e.post({ps::CommandType::SetMaster, masterGain, 0});
}

static double lufsOfRange(const std::vector<float>& out, size_t fromFrame, size_t toFrame) {
    ps::LoudnessMeter m;
    m.prepare(kRate);
    for (size_t i = fromFrame; i < toFrame && 2 * i + 1 < out.size(); ++i) m.add(out[2 * i], out[2 * i + 1]);
    return m.integratedLufs();
}

static std::vector<float> tone(int frames, float hz, float amp, float rightSign = 1.0f) {
    std::vector<float> v(size_t(frames) * 2);
    for (int i = 0; i < frames; ++i) {
        const float s = amp * std::sin(2.0f * float(M_PI) * hz * float(i) / float(kRate));
        v[size_t(i) * 2] = s;
        v[size_t(i) * 2 + 1] = rightSign * s;
    }
    return v;
}

// EBU Tech 3342's idea: 20 s at one level then 20 s 10 LU lower read an LRA of 10 (±1); a steady tone 0.
// A tone's loudest 400 ms and 3 s are its own loudness; correlation +1 / -1; out of phase folds to silence.
static void testLoudnessRangeReadsKnownSignals() {
    auto feed = [](ps::LoudnessRange& r, const std::vector<float>& v) {
        for (size_t i = 0; i + 1 < v.size(); i += 2) r.add(v[i], v[i + 1]);
    };
    ps::LoudnessRange steady;
    steady.prepare(kRate);
    const auto t = tone(kRate * 10, 1000.0f, 0.1f);
    feed(steady, t);
    ps::LoudnessMeter m;
    m.prepare(kRate);
    for (size_t i = 0; i + 1 < t.size(); i += 2) m.add(t[i], t[i + 1]);
    std::printf("  steady: LRA %.2f, max M %.2f, max S %.2f, I %.2f, mono %.2f, corr %.3f\n", steady.lra(), steady.maxMomentary(),
                steady.maxShortTerm(), m.integratedLufs(), steady.monoLufs(), steady.correlation());
    CHECK(steady.lra() < 0.3);
    CHECK(std::fabs(steady.maxMomentary() - m.integratedLufs()) < 0.2);
    CHECK(std::fabs(steady.maxShortTerm() - m.integratedLufs()) < 0.2);
    CHECK(std::fabs(steady.monoLufs() - m.integratedLufs()) < 0.1);  // the same on both sides: nothing lost
    CHECK(steady.correlation() > 0.999);

    ps::LoudnessRange stepped;
    stepped.prepare(kRate);
    feed(stepped, tone(kRate * 20, 1000.0f, 0.1f));
    feed(stepped, tone(kRate * 20, 1000.0f, 0.1f * std::pow(10.0f, -10.0f / 20.0f)));
    std::printf("  20 s, then 20 s 10 LU lower: LRA %.2f LU\n", stepped.lra());
    CHECK(std::fabs(stepped.lra() - 10.0) <= 1.0);

    ps::LoudnessRange opposite;
    opposite.prepare(kRate);
    feed(opposite, tone(kRate * 5, 440.0f, 0.2f, -1.0f));
    CHECK(opposite.correlation() < -0.999);
    CHECK(opposite.monoLufs() < -200.0);  // the mono fold is silence
}

// « A/B au même volume »: the master and the mix without it (a chain that changes the loudness by
// several dB) heard at the same loudness, measured on the output; a reference likewise; every side at
// the quietest one's (never raised: nothing clips); disarmed, the master is heard as it always was.
static void testAbAndReferenceAtTheSameLoudness() {
    ps::Engine e(kRate);
    loudLoop(e, 0.25f);  // -12 dB into the master: its target brings it up
    using IS = ps::InsertSlot;
    e.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kMaster * ps::Mixer::kInserts + 0, IS::Comp});
    e.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kMaster * ps::Mixer::kInserts + 1, IS::Saturation});
    e.post({ps::CommandType::SetMaster, -9.0f, 4});
    e.post({ps::CommandType::SetMasterPro, 1.0f, ps::ProForceSong});
    e.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(e, kRate * 30, 256);  // the drive settles
    const auto plain = render(e, kRate * 8, 256);
    const double p = lufsOfRange(plain, 0, size_t(kRate) * 8);
    e.post({ps::CommandType::SetMasterPro, 1.0f, ps::ProArmed});
    render(e, kRate * 12, 256);  // both sides measured, the gains settled
    const auto master = render(e, kRate * 8, 256);
    e.post({ps::CommandType::SetMasterPro, 1.0f, ps::ProListen});
    render(e, kRate * 2, 256);
    const auto without = render(e, kRate * 8, 256);
    const double a = lufsOfRange(master, 0, size_t(kRate) * 8), b = lufsOfRange(without, 0, size_t(kRate) * 8);
    const auto& cmp = e.mixer().masterCompare();
    std::printf("  master alone %.2f LUFS; comparing: master %.2f (%+.1f dB), without it %.2f (%+.1f dB)\n", p, a, double(cmp.masterGainDb()), b,
                double(cmp.withoutGainDb()));
    CHECK(std::fabs(a - b) <= 0.5);
    CHECK(std::min(cmp.masterGainDb(), cmp.withoutGainDb()) < -3.0f);  // one side really had to be matched
    CHECK(cmp.masterGainDb() <= 0.0f && cmp.withoutGainDb() <= 0.0f);
    // A reference, quieter still: every side at its loudness.
    const auto refPcm = tone(kRate * 4, 220.0f, 0.05f);
    ps::LoudnessMeter rm;
    rm.prepare(kRate);
    for (size_t i = 0; i + 1 < refPcm.size(); i += 2) rm.add(refPcm[i], refPcm[i + 1]);
    ps::Command install{ps::CommandType::InstallReference, static_cast<float>(rm.integratedLufs())};
    install.sample = ps::Sample::create(refPcm.data(), kRate * 4, 2, kRate, kRate);
    e.post(install);
    e.post({ps::CommandType::SetMasterPro, 2.0f, ps::ProListen});
    render(e, kRate * 4, 256);
    const auto reference = render(e, kRate * 8, 256);
    e.post({ps::CommandType::SetMasterPro, 0.0f, ps::ProListen});
    render(e, kRate * 2, 256);
    const auto masterAgain = render(e, kRate * 8, 256);
    const double c = lufsOfRange(reference, 0, size_t(kRate) * 8), d = lufsOfRange(masterAgain, 0, size_t(kRate) * 8);
    std::printf("  the reference (%.1f LUFS on its own) heard at %.2f LUFS, the master then at %.2f LUFS\n", rm.integratedLufs(), c, d);
    CHECK(std::fabs(c - d) <= 0.5);
    CHECK(std::fabs(c - rm.integratedLufs()) <= 0.5);
    // Disarmed: the master as it always was.
    e.post({ps::CommandType::SetMasterPro, 0.0f, ps::ProArmed});
    render(e, kRate * 3, 256);
    const auto back = render(e, kRate * 8, 256);
    CHECK(std::fabs(lufsOfRange(back, 0, size_t(kRate) * 8) - p) <= 0.5);
    CHECK(!e.mixer().masterCompare().active());
    CHECK(finiteAndUnclipped(master) && finiteAndUnclipped(without) && finiteAndUnclipped(reference));
    e.post({ps::CommandType::InstallReference, 0.0f});  // none: the old one goes back to be freed
    render(e, 512, 256);
    e.collectGarbage();
}

// Separate tracks that add up to the mix: each strip alone (its sends' returns with it), or each strip
// dry plus each send's return alone, with a Musique bus ducked under the kick. Against the mix without
// the master's processing (as the stems are rendered): under -60 dB.
static void testStemsAddUpToTheMix() {
    auto describe = [](ps::OfflineRenderer& r) {
        r.post({ps::CommandType::SetTempo, 124.0f});
        for (int st : {0, 4, 8, 12}) r.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, st});
        for (int st = 2; st < 16; st += 4) r.post({ps::CommandType::SetDrumHit, 0.8f, ps::DrumKit::ClosedHat, st});
        for (int st = 0; st < 16; ++st) {
            r.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 7) % 12, 0, st % 4 == 2, st % 5 == 1, st % 3 != 2}.pack()});
        }
        // Sends into the reverb and the echo, a strip's own compressor and saturation.
        r.post({ps::CommandType::SetSend, 0.4f, 0, 0});
        r.post({ps::CommandType::SetSend, 0.5f, 1, 0});
        r.post({ps::CommandType::SetSend, 0.35f, 1, 1});
        r.post({ps::CommandType::SetInsertType, 0.0f, 1 * ps::Mixer::kInserts, ps::InsertSlot::Saturation});
        r.post({ps::CommandType::SetInsertType, 0.0f, 0 * ps::Mixer::kInserts, ps::InsertSlot::Comp});
        // The 303 in a Musique bus ducked under the kick (its pad's strip), the reverb's return with it.
        r.post({ps::CommandType::SetSidechain, float(ps::Mixer::kFirstPad + ps::DrumKit::Kick), 0});
        r.post({ps::CommandType::SetSidechain, 1.0f, 4, 1});
        r.post({ps::CommandType::SetSidechain, 1.0f, 5, 0});
        r.post({ps::CommandType::SetSidechain, 0.6f, 1});
        r.post({ps::CommandType::SetSidechain, 0.8f, 2});
        // A master effect (left out of the stems, and of the mix they are checked against).
        r.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kMaster * ps::Mixer::kInserts, ps::InsertSlot::OttFx});
    };
    const int64_t frames = int64_t(kRate) * 6, tail = kRate * 2;
    auto renderWith = [&](int solo, int stemMode) {
        ps::OfflineRenderer r(kRate);
        describe(r);
        r.post({ps::CommandType::SetMaster, 0.0f, 1});  // no limiter
        r.post({ps::CommandType::SetMaster, 0.0f, 2});  // no master effects
        if (solo >= 0) r.post({ps::CommandType::SetChannelSolo, 1.0f, solo, 0});
        r.post({ps::CommandType::SetMasterPro, float(stemMode), ps::ProStemMode});
        return r.renderToBuffer(frames, tail);
    };
    const auto mix = renderWith(-1, ps::Mixer::StemNormal);
    double mixPeak = 0.0;
    for (float v : mix) mixPeak = std::max(mixPeak, double(std::fabs(v)));
    auto residual = [&](const std::vector<std::vector<float>>& stems) {
        double worst = 0.0;
        for (size_t i = 0; i < mix.size(); ++i) {
            double sum = 0.0;
            for (const auto& s : stems) sum += i < s.size() ? s[i] : 0.0f;
            worst = std::max(worst, std::fabs(sum - mix[i]));
        }
        return toDb(worst / std::max(mixPeak, 1e-9));
    };
    const auto drums = renderWith(0, ps::Mixer::StemNormal), acid = renderWith(1, ps::Mixer::StemNormal);
    const double withSends = residual({drums, acid});
    const auto drumsDry = renderWith(0, ps::Mixer::StemDry), acidDry = renderWith(1, ps::Mixer::StemDry);
    const auto reverb = renderWith(-1, ps::Mixer::StemReturn0), echo = renderWith(-1, ps::Mixer::StemReturn1);
    const double dry = residual({drumsDry, acidDry, reverb, echo});
    double reverbPeak = 0.0;
    for (float v : reverb) reverbPeak = std::max(reverbPeak, double(std::fabs(v)));
    std::printf("  mix peak %.2f dBFS; stems with their sends: residual %.1f dB; dry + reverb + echo: %.1f dB (reverb %.1f dBFS)\n",
                toDb(mixPeak), withSends, dry, toDb(reverbPeak));
    CHECK(mixPeak > 0.05);
    CHECK(withSends < -60.0);
    CHECK(dry < -60.0);
    CHECK(toDb(reverbPeak) > -40.0);  // the returns really are in their own files
    // The same length from frame 0 (no automatic tail here: one length for all).
    CHECK(drums.size() == mix.size() && reverb.size() == mix.size() && acidDry.size() == mix.size());
}

// The file's finish: a song that starts after half a second of silence starts on its first sound
// (trimmed), fades in from there, ends on its last sound (trimmed), then 0.25 s of digital silence.
static void testFinishTrimsFadesAndPads() {
    std::vector<float> clip(size_t(kRate) * 2 * 2, 0.0f);
    for (int i = kRate / 2; i < kRate; ++i) clip[size_t(i) * 2] = clip[size_t(i) * 2 + 1] = 0.3f * std::sin(float(i) * 0.05f);
    auto make = [&](ps::OfflineRenderer& r) {
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 0};
        c.sample = ps::Sample::create(clip.data(), kRate * 2, 2, kRate, kRate);
        r.post(c);
        r.post({ps::CommandType::SetAudioClipParam, 0.0f, 0, ps::AudioTracks::Start});
        r.post({ps::CommandType::SetMaster, 0.0f, 1});
        r.setAutoTail(true);
    };
    ps::OfflineRenderer plain(kRate);
    make(plain);
    const auto a = plain.renderToBuffer(kRate * 2, kRate);
    ps::OfflineRenderer done(kRate);
    make(done);
    ps::OfflineRenderer::Finish f;
    f.trimStart = f.trimEnd = true;
    f.fadeInFrames = 480;
    f.endSilenceFrames = kRate / 4;
    done.setFinish(f);
    const auto b = done.renderToBuffer(kRate * 2, kRate);
    size_t firstA = 0;
    while (firstA < a.size() / 2 && std::fabs(a[firstA * 2]) < ps::OfflineRenderer::kSilence) ++firstA;
    const size_t frames = b.size() / 2;
    std::printf("  plain %zu frames (first sound at %zu), finished %zu frames\n", a.size() / 2, firstA, frames);
    CHECK(firstA >= size_t(kRate / 2) - 2);
    // Starts on its sound, faded in over 480 frames along a squared curve.
    CHECK(std::fabs(b[0]) < 1e-6f || std::fabs(b[0]) < std::fabs(a[firstA * 2]));
    CHECK(std::fabs(b[480 * 2] - a[(firstA + 480) * 2]) < 1e-6f);
    CHECK(std::fabs(b[240 * 2] - 0.25f * a[(firstA + 240) * 2]) < 1e-5f);
    // The sound itself, then exactly the silence asked for.
    const size_t sound = frames - size_t(kRate / 4);
    CHECK(sound > size_t(kRate / 2) - 10 && sound <= size_t(kRate / 2) + 10);
    CHECK(std::fabs(b[(sound - 1) * 2]) >= ps::OfflineRenderer::kSilence || std::fabs(b[(sound - 1) * 2 + 1]) >= ps::OfflineRenderer::kSilence);
    bool zeros = true;
    for (size_t i = sound * 2; i < b.size(); ++i) zeros &= b[i] == 0.0f;
    CHECK(zeros);
}

// A part or a selection: the song's frames from its start, sample for sample as in the whole render.
static void testRangeStartKeepsTheSelection() {
    auto make = [](ps::OfflineRenderer& r) {
        prepareLoudLoop(r, 0.5f);
        r.post({ps::CommandType::SetMaster, 0.0f, 1});
    };
    ps::OfflineRenderer whole(kRate), part(kRate);
    make(whole);
    make(part);
    ps::OfflineRenderer::Finish f;
    f.rangeStart = 12345;
    part.setFinish(f);
    const auto a = whole.renderToBuffer(kRate * 3, 0), b = part.renderToBuffer(kRate * 3, 0);
    CHECK(b.size() == a.size() - 12345 * 2);
    bool same = b.size() + 12345 * 2 == a.size();
    for (size_t i = 0; same && i < b.size(); ++i) same = b[i] == a[i + 12345 * 2];
    CHECK(same);
}

static std::vector<float> readWav16(const std::string& path, int& rate) {
    std::vector<float> out;
    std::FILE* f = std::fopen(path.c_str(), "rb");
    if (!f) return out;
    std::vector<uint8_t> bytes;
    uint8_t buf[65536];
    size_t n;
    while ((n = std::fread(buf, 1, sizeof buf, f)) > 0) bytes.insert(bytes.end(), buf, buf + n);
    std::fclose(f);
    if (bytes.size() < 44) return out;
    rate = int(bytes[24] | (bytes[25] << 8) | (bytes[26] << 16) | (bytes[27] << 24));
    const int bits = bytes[34];
    if (bits != 16) return out;
    for (size_t i = 44; i + 1 < bytes.size(); i += 2) out.push_back(float(int16_t(bytes[i] | (bytes[i + 1] << 8))) / 32767.0f);
    return out;
}

// 96 kHz (resampled after the master) and 16 bits: the shaped dither leaves less noise under 4 kHz than
// the plain one (and more above 15 kHz), against the same render in 32-bit float.
static void testHighRateAndShapedDither() {
    // A quiet tone (-50 dBFS) on an audio track: the quantizer's noise counts.
    const auto quiet = tone(kRate * 3, 1000.0f, 0.003f);
    auto make = [&quiet](ps::OfflineRenderer& r) {
        r.post({ps::CommandType::SetMaster, 0.0f, 1});
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 0};
        c.sample = ps::Sample::create(quiet.data(), kRate * 3, 2, kRate, kRate);
        r.post(c);
        r.post({ps::CommandType::SetAudioClipParam, 0.0f, 0, ps::AudioTracks::Start});
    };
    const char* dir = std::getenv("TMPDIR") ? std::getenv("TMPDIR") : "/tmp";
    const std::string plainPath = std::string(dir) + "/ps_mp_plain.wav", shapedPath = std::string(dir) + "/ps_mp_shaped.wav";
    std::vector<float> reference;
    double low[2] = {0, 0}, high[2] = {0, 0};
    for (int shaped = 0; shaped < 2; ++shaped) {
        ps::OfflineRenderer r(kRate);
        make(r);
        r.setOutputFormat(96000, 16);
        ps::OfflineRenderer::Finish f;
        f.noiseShaping = shaped == 1;
        r.setFinish(f);
        const std::string path = shaped ? shapedPath : plainPath;
        CHECK(r.renderToWav(path.c_str(), kRate * 2, 0) == ps::OfflineRenderer::Result::Ok);
        CHECK(r.report().sampleRate == 96000);
        int rate = 0;
        const auto pcm = readWav16(path, rate);
        std::remove(path.c_str());
        CHECK(rate == 96000);
        std::printf("  %s: %zu samples\n", path.c_str(), pcm.size());
        CHECK(pcm.size() > size_t(96000) * 2 * 2 - 4000);
        if (reference.empty()) {
            ps::OfflineRenderer fr(kRate);
            make(fr);
            fr.setOutputFormat(96000, 32);
            const std::string fp = std::string(dir) + "/ps_mp_float.wav";
            CHECK(fr.renderToWav(fp.c_str(), kRate * 2, 0) == ps::OfflineRenderer::Result::Ok);
            std::FILE* f2 = std::fopen(fp.c_str(), "rb");
            if (f2) {
                std::fseek(f2, 58, SEEK_SET);
                float v;
                while (std::fread(&v, sizeof v, 1, f2) == 1) reference.push_back(v);
                std::fclose(f2);
            }
            std::remove(fp.c_str());
        }
        // The error's energy below 4 kHz and above 15 kHz (one-pole splits, left channel).
        const size_t n = std::min(pcm.size(), reference.size()) / 2;
        // Four one-poles in a row each way (24 dB / octave): the shaped noise far up does not leak down.
        double lp[4] = {}, hpPrev[4] = {}, hpOut[4] = {};
        const double aLow = 1.0 - std::exp(-2.0 * M_PI * 2000.0 / 96000.0), aHigh = std::exp(-2.0 * M_PI * 20000.0 / 96000.0);
        for (size_t i = 0; i < n; ++i) {
            double x = double(pcm[i * 2]) - double(reference[i * 2]);
            double y = x;
            for (int k = 0; k < 4; ++k) y = lp[k] += aLow * (y - lp[k]);
            for (int k = 0; k < 4; ++k) {
                hpOut[k] = aHigh * (hpOut[k] + x - hpPrev[k]);
                hpPrev[k] = x;
                x = hpOut[k];
            }
            if (i < 2000) continue;
            low[shaped] += y * y;
            high[shaped] += x * x;
        }
    }
    std::printf("  16 bits at 96 kHz, noise under 2 kHz: plain %.1f dB, shaped %.1f dB; over 20 kHz: plain %.1f dB, shaped %.1f dB\n",
                10 * std::log10(low[0] + 1e-30), 10 * std::log10(low[1] + 1e-30), 10 * std::log10(high[0] + 1e-30), 10 * std::log10(high[1] + 1e-30));
    CHECK(reference.size() > size_t(96000) * 2 * 2 - 4000);
    CHECK(low[1] < low[0] * 0.5);
    CHECK(high[1] > high[0]);
}

// The limiter's attack and release (Master pro): shortest and longest, the true peak stays under the
// ceiling; the default (1, 1) is the limiter as it always was.
static void testLimiterSettingsHoldTheCeiling() {
    for (float attack : {0.2f, 1.0f}) {
        for (float release : {0.25f, 4.0f}) {
            ps::OfflineRenderer r(kRate);
            prepareLoudLoop(r, 2.0f);  // +6 dB into the limiter
            r.post({ps::CommandType::SetMasterPro, attack, ps::ProLimiterAttack});
            r.post({ps::CommandType::SetMasterPro, release, ps::ProLimiterRelease});
            r.post({ps::CommandType::SetMaster, -9.0f, 4});
            const auto out = r.renderToBuffer(kRate * 6, 0);
            const double tp = independentTruePeak(out);
            std::printf("  attack %.1f, release x%.2f: true peak %.2f dBTP, %.2f LUFS\n", double(attack), double(release), toDb(tp), r.report().lufs);
            CHECK(toDb(tp) <= -1.0);
            CHECK(finiteAndUnclipped(out));
        }
    }
}

// A target in integrated LUFS (« Apple Music -16 »): the whole song at it (two passes), even with the
// mix version that aims the style's target at the Drop.
static void testIntegratedTargetOnADropAimedSong() {
    for (float target : {-16.0f, -9.0f}) {
        ps::OfflineRenderer r(kRate);
        prepareLoudLoop(r, 0.5f);
        r.post({ps::CommandType::SetMixVersion, 4.0f, 0});  // the Drop mode
        r.post({ps::CommandType::SetMasterPro, 1.0f, ps::ProForceSong});
        r.post({ps::CommandType::SetMaster, target, 4});
        r.post({ps::CommandType::SetMaster, -0.3f, 5});  // the ceiling at -0.3 dBTP
        const auto out = r.renderToBuffer(kRate * 10, 0);
        const auto& rep = r.report();
        std::printf("  target %.0f: %.2f LUFS, %.2f dBTP, mode %d, LRA %.1f, PLR %.1f, max S %.1f, corr %.2f\n", double(target), rep.lufs,
                    toDb(rep.truePeak), rep.mode, rep.lra, toDb(rep.truePeak) - rep.lufs, rep.maxShortTerm, rep.correlation);
        CHECK(rep.mode == 0);
        CHECK(std::fabs(rep.lufs - target) <= 0.5);
        CHECK(toDb(independentTruePeak(out)) <= -0.3);
        CHECK(rep.maxShortTerm >= rep.lufs - 0.5);
        CHECK(rep.correlation > 0.0 && rep.correlation <= 1.0);
    }
}

// Audit ultra round 1 (n° 13, seats A c, E d, F c): a club export and a streaming export of the same
// song, as the app describes them (mix version 4: « Club » aims at the Drop, -8 LUFS; « Streaming »
// at the whole song, -14 LUFS), are two different files: 6 LU apart, each at its target, both under -1 dBTP.
static void testClubAndStreamingExportsDiffer() {
    double club = 0.0, streaming = 0.0;
    for (bool wholeSong : {false, true}) {
        ps::OfflineRenderer r(kRate);
        prepareLoudLoop(r, 0.5f);
        r.post({ps::CommandType::SetMixVersion, 4.0f, 0});
        r.post({ps::CommandType::SetMasterPro, wholeSong ? 1.0f : 0.0f, ps::ProForceSong});
        r.post({ps::CommandType::SetMaster, wholeSong ? -14.0f : -8.0f, 4});
        const auto out = r.renderToBuffer(kRate * 12, 0);
        const auto& rep = r.report();
        const double reached = wholeSong ? rep.lufs : (rep.loudLufs > -100.0 ? rep.loudLufs : rep.lufs);
        std::printf("  %s: %.2f LUFS (loud part %.2f), %.2f dBTP, mode %d\n", wholeSong ? "streaming" : "club", rep.lufs, rep.loudLufs, toDb(rep.truePeak), rep.mode);
        CHECK(rep.mode == (wholeSong ? 0 : 1));
        CHECK(std::fabs(reached - (wholeSong ? -14.0 : -8.0)) <= 0.5);
        CHECK(toDb(independentTruePeak(out)) <= -1.0);
        CHECK(finiteAndUnclipped(out));
        (wholeSong ? streaming : club) = rep.lufs;
    }
    CHECK(club - streaming >= 5.0);
}

// Master pro on a live engine: A/B measured and switched, a reference playing, the limiter's settings
// and a stem mode changed, nothing allocated on the audio thread.
static void testMasterProWithoutAllocating() {
    ps::Engine e(kRate);
    loudLoop(e, 0.5f);
    e.post({ps::CommandType::SetMaster, -9.0f, 4});
    const auto pcm = tone(kRate, 330.0f, 0.1f);
    ps::Command install{ps::CommandType::InstallReference, -20.0f};
    install.sample = ps::Sample::create(pcm.data(), kRate, 2, kRate, kRate);
    e.post(install);
    e.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(256 * 2);
    for (int i = 0; i < 50; ++i) e.process(nullptr, 0, 0, out.data(), 256, 2);  // warm-up
    g_allocations = 0;
    g_countAllocations = true;
    bool finite = true;
    for (int i = 0; i < 2000; ++i) {
        if (i == 10) e.post({ps::CommandType::SetMasterPro, 1.0f, ps::ProArmed});
        if (i % 300 == 50) e.post({ps::CommandType::SetMasterPro, float((i / 300) % 3), ps::ProListen});
        if (i == 400) e.post({ps::CommandType::SetMasterPro, 0.3f, ps::ProLimiterAttack});
        if (i == 500) e.post({ps::CommandType::SetMasterPro, 2.0f, ps::ProLimiterRelease});
        if (i == 700) e.post({ps::CommandType::SetMasterPro, 1.0f, ps::ProForceSong});
        if (i == 900) e.post({ps::CommandType::SetMasterPro, 2.0f, ps::ProStemMode});
        if (i == 1100) e.post({ps::CommandType::SetMasterPro, 0.0f, ps::ProStemMode});
        e.process(nullptr, 0, 0, out.data(), 256, 2);
        for (float v : out) finite &= std::isfinite(v) && std::fabs(v) <= 1.0f;
    }
    g_countAllocations = false;
    CHECK(g_allocations.load() == 0);
    CHECK(finite);
}

}  // namespace masterpro

#define PS_MASTERPRO_TESTS                                                                      \
    {"mp_loudness_range_reads_known_signals", masterpro::testLoudnessRangeReadsKnownSignals},   \
    {"mp_ab_and_reference_at_the_same_loudness", masterpro::testAbAndReferenceAtTheSameLoudness}, \
    {"mp_stems_add_up_to_the_mix", masterpro::testStemsAddUpToTheMix},                          \
    {"mp_finish_trims_fades_and_pads", masterpro::testFinishTrimsFadesAndPads},                 \
    {"mp_range_start_keeps_the_selection", masterpro::testRangeStartKeepsTheSelection},         \
    {"mp_high_rate_and_shaped_dither", masterpro::testHighRateAndShapedDither},                 \
    {"mp_limiter_settings_hold_the_ceiling", masterpro::testLimiterSettingsHoldTheCeiling},     \
    {"mp_integrated_target_on_a_drop_aimed_song", masterpro::testIntegratedTargetOnADropAimedSong}, \
    {"mp_club_and_streaming_exports_differ", masterpro::testClubAndStreamingExportsDiffer},     \
    {"mp_master_pro_without_allocating", masterpro::testMasterProWithoutAllocating},
