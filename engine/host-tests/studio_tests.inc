// Phase 34, S3 (docs/PLAN-SON-PRO.md): the drums' micro-timing and roll descriptors, the 808
// bass, the studio kicks, the rumble and kit v3. Included by engine_tests.cpp (kept apart to
// ease merging).

namespace studio_tests {

// Energy of x[from, to) in the band [loHz, hiHz) (Goertzel at 1 Hz steps: fine enough for the
// bass bands measured here).
double bandEnergy(const std::vector<float>& x, size_t from, size_t to, double loHz, double hiHz, int stride = 1) {
    to = std::min(to, x.size());
    if (to <= from) return 0.0;
    const double n = static_cast<double>(to - from);
    double total = 0.0;
    for (double hz = loHz; hz < hiHz; hz += 1.0) {
        const double w = 2.0 * M_PI * hz * stride / kRate;
        const double coef = 2.0 * std::cos(w);
        double s1 = 0.0, s2 = 0.0;
        for (size_t i = from; i < to; i += static_cast<size_t>(stride)) {
            const double s0 = static_cast<double>(x[i]) + coef * s1 - s2;
            s2 = s1;
            s1 = s0;
        }
        total += (s1 * s1 + s2 * s2 - coef * s1 * s2) / n;
    }
    return total;
}

double energy(const std::vector<float>& x, size_t from = 0, size_t to = SIZE_MAX) {
    to = std::min(to, x.size());
    double e = 0.0;
    for (size_t i = from; i < to; ++i) e += static_cast<double>(x[i]) * x[i];
    return e;
}

float peakOf(const std::vector<float>& x, size_t from = 0, size_t to = SIZE_MAX) {
    to = std::min(to, x.size());
    float p = 0.0f;
    for (size_t i = from; i < to; ++i) p = std::max(p, std::fabs(x[i]));
    return p;
}

bool allFinite(const std::vector<float>& x) {
    for (float v : x) if (!std::isfinite(v)) return false;
    return true;
}

// Frames from the hit to the last sample above `db` under the peak (a drum's audible length).
int lengthTo(const std::vector<float>& x, double db) {
    const float floor = peakOf(x) * static_cast<float>(std::pow(10.0, db / 20.0));
    int last = 0;
    for (size_t i = 0; i < x.size(); ++i) if (std::fabs(x[i]) > floor) last = static_cast<int>(i);
    return last;
}

// The energy above `hz` against the whole (dB), through a 24 dB/oct high-pass: the bench's « air ».
double airDb(const std::vector<float>& x, float hz) {
    ps::studio::Svf2 a, b;
    a.set(hz, 0.707f, kRate);
    b.set(hz, 0.707f, kRate);
    double high = 0.0, total = 0.0;
    for (float v : x) {
        const float h = b.high(a.high(v));
        high += static_cast<double>(h) * h;
        total += static_cast<double>(v) * v;
    }
    return 10.0 * std::log10(std::max(high, 1e-30) / std::max(total, 1e-30));
}

std::vector<float> renderMonoKit(ps::DrumKit& kit, int frames, const std::vector<std::tuple<int, int, float, float, int>>& hits) {
    // frame, voice, velocity, pitch, glide frames
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    constexpr int kBlock = 192;
    for (int pos = 0; pos < frames; pos += kBlock) {
        const int n = std::min(kBlock, frames - pos);
        for (const auto& [frame, voice, velocity, pitch, glide] : hits) {
            if (frame >= pos && frame < pos + n) kit.trigger(voice, velocity, frame - pos, 0.0f, pitch, glide);
        }
        kit.render(out.data() + pos, n);
    }
    return out;
}

// Step offsets, roll descriptors (hits, span, ramps, shuffle) and glides land on the exact
// sample, whatever the block size, with no allocation on the audio thread.
void testStepOffsetsAndRolls() {
    using ps::CommandType;
    for (int block : {192, 256, 1024}) {
        ps::Engine e(kRate);
        e.post({CommandType::SetTempo, 120.0f});  // a step = 6000 frames
        std::vector<float> click(64, 0.0f);
        click[0] = 1.0f;
        e.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(click.data(), 64, 1, kRate, kRate)});
        e.post({CommandType::SetPadParam, 0.0f, 5, ps::Sampler::Release});
        e.post({CommandType::SetPatternSteps, 0.0f, 32});
        // Step 0: 5 ms late. Step 2: 5 ms early (queued from step 1). Step 4: 8 hits, flat.
        // Step 8: 8 hits over 4 steps (a 1/32 burst over a beat), rising to the hit's velocity.
        // Step 14: 2 hits shuffled. Step 16: 4 hits falling by half.
        e.post({CommandType::SetDrumHit, 1.0f, 5, 0});
        e.post({CommandType::SetDrumStepOffset, 5.0f, 5, 0});
        e.post({CommandType::SetDrumHit, 1.0f, 5, 2});
        e.post({CommandType::SetDrumStepOffset, -5.0f, 5, 2});
        e.post({CommandType::SetDrumHit, 1.0f, 5, 4});
        e.post({CommandType::SetDrumStepRoll, 0.0f, 5, 4 | (1 << 8) | (1 << 12) | (8 << 16)});
        e.post({CommandType::SetDrumHit, 0.5f, 5, 8});  // the ramps under the limiter's ceiling
        e.post({CommandType::SetDrumStepRoll, 0.6f, 5, 8 | (4 << 8) | (2 << 12) | (8 << 16)});
        e.post({CommandType::SetDrumHit, 1.0f, 5, 14});
        e.post({CommandType::SetDrumStepRoll, 0.0f, 5, 14 | (1 << 8) | (1 << 12) | (1 << 15) | (2 << 16)});
        e.post({CommandType::SetDrumHit, 0.5f, 5, 16});
        e.post({CommandType::SetDrumStepRoll, 0.5f, 5, 16 | (1 << 8) | (3 << 12) | (4 << 16)});
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        std::vector<float> out(static_cast<size_t>(6000 * 20) * kChannels, 0.0f);
#if PS_COUNT_ALLOCATIONS
        g_allocations = 0;
        g_countAllocations = true;
#endif
        for (int pos = 0; pos < 6000 * 20; pos += block) {
            e.process(nullptr, 0, 0, out.data() + static_cast<size_t>(pos) * kChannels, std::min(block, 6000 * 20 - pos), kChannels);
        }
#if PS_COUNT_ALLOCATIONS
        g_countAllocations = false;
        CHECK(g_allocations.load() == 0);
#endif
        std::vector<int> hits;
        std::vector<float> peaks;
        for (int i = 0; i < 6000 * 20; ++i) {
            const bool on = std::fabs(out[size_t(i) * 2]) > 0.02f;
            if (on && (i == 0 || std::fabs(out[size_t(i - 1) * 2]) <= 0.02f)) {
                hits.push_back(i);
                float p = 0.0f;
                for (int k = i; k < std::min(i + 64, 6000 * 20); ++k) p = std::max(p, std::fabs(out[size_t(k) * 2]));
                peaks.push_back(p);
            }
        }
        std::vector<int> want = {240, 12000 - 240};
        for (int k = 0; k < 8; ++k) want.push_back(24000 + k * 750);
        for (int k = 0; k < 8; ++k) want.push_back(48000 + k * 3000);
        want.push_back(84000);
        want.push_back(84000 + 3000 + 1000);
        for (int k = 0; k < 4; ++k) want.push_back(96000 + k * 1500);
        for (int& w : want) w += kLate;
        CHECK(hits.size() == want.size());
        int wrong = 0;
        for (size_t k = 0; k < std::min(hits.size(), want.size()); ++k) {
            if (hits[k] != want[k]) {
                ++wrong;
                if (wrong <= 4) std::printf("  block %d: hit %zu at %d, wanted %d\n", block, k, hits[k], want[k]);
            }
        }
        CHECK(wrong == 0);
        if (peaks.size() == want.size()) {
            if (block == 256) {
                std::printf("  roll peaks:");
                for (float p : peaks) std::printf(" %.2f", p);
                std::printf("\n");
            }
            // Flat: the 8 hits of step 4 equal. Up: the burst of step 8 rises every hit, from 0.4 x
            // to the full hit. Down: step 16 falls every hit, to half.
            for (int k = 1; k < 8; ++k) CHECK(std::fabs(peaks[size_t(2 + k)] - peaks[2]) < 0.02f);
            for (int k = 1; k < 8; ++k) CHECK(peaks[size_t(10 + k)] > peaks[size_t(9 + k)] + 0.02f);
            CHECK(std::fabs(peaks[10] / peaks[17] - 0.4f) < 0.06f);
            for (int k = 1; k < 4; ++k) CHECK(peaks[size_t(20 + k)] < peaks[size_t(19 + k)] - 0.02f);
            CHECK(std::fabs(peaks[23] / peaks[20] - 0.5f) < 0.06f);
        }
    }
    // An early hit on the first step of a waiting scene plays just before the bar line it takes
    // over on (queued from the playing scene's last step).
    {
        ps::Engine e(kRate);
        e.post({CommandType::SetTempo, 120.0f});
        std::vector<float> click(64, 0.0f);
        click[0] = 1.0f;
        e.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(click.data(), 64, 1, kRate, kRate)});
        e.post({CommandType::SetPadParam, 0.0f, 5, ps::Sampler::Release});
        e.post({CommandType::SetStaging, 1.0f});
        e.post({CommandType::SetDrumHit, 1.0f, 5, 0});
        e.post({CommandType::SetDrumStepOffset, -5.0f, 5, 0});
        e.post({CommandType::SetStaging, 0.0f});
        e.post({CommandType::ArmSceneSwap, 0.0f, 0, 0});
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        const auto out = render(e, 6000 * 18, 256);
        int first = -1;
        for (int i = 0; i < 6000 * 18 && first < 0; ++i) if (std::fabs(out[size_t(i) * 2]) > 0.02f) first = i;
        std::printf("  early hit of a waiting scene at %d (bar line %d)\n", first - kLate, 6000 * 16);
        CHECK(first == 6000 * 16 - 240 + kLate);
        CHECK(e.sceneSwaps() == 1);
    }
    // The ramp sent with the hit (SetDrumHit bits 24..25, Hit.ramp): rising from half the velocity,
    // falling to half; more than 8 hits a step are 8.
    for (int ramp : {1, 2}) {
        ps::Engine e(kRate);
        e.post({CommandType::SetTempo, 120.0f});
        std::vector<float> click(64, 0.0f);
        click[0] = 1.0f;
        e.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(click.data(), 64, 1, kRate, kRate)});
        e.post({CommandType::SetPadParam, 0.0f, 5, ps::Sampler::Release});
        e.post({CommandType::SetDrumHit, 0.5f, 5, 0 | (4 << 16) | (ramp << 24)});
        e.post({CommandType::SetDrumHit, 0.5f, 5, 4 | (16 << 16)});
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        const auto out = render(e, 6000 * 5, 256);
        std::vector<float> peaks;
        for (int k = 0; k < 4; ++k) peaks.push_back(std::fabs(out[size_t(k * 1500 + kLate) * 2]));
        std::printf("  hit ramp %d: %.3f %.3f %.3f %.3f\n", ramp, peaks[0], peaks[1], peaks[2], peaks[3]);
        for (int k = 1; k < 4; ++k) CHECK(ramp == 1 ? peaks[size_t(k)] > peaks[size_t(k - 1)] : peaks[size_t(k)] < peaks[size_t(k - 1)]);
        CHECK(std::fabs((ramp == 1 ? peaks[0] / peaks[3] : peaks[3] / peaks[0]) - 0.5f) < 0.03f);
        CHECK(e.laneTriggerCount(5) == 4 + 8);
    }
    // The first step after play cannot be early: it lands on the grid. A stop drops the burst's
    // hits still to come. Eight hits a step at 300 BPM (a hit every 300 frames) all sound.
    {
        ps::Engine e(kRate);
        e.post({CommandType::SetTempo, 120.0f});
        e.post({CommandType::SetDrumHit, 1.0f, 3, 0});
        e.post({CommandType::SetDrumStepOffset, -10.0f, 3, 0});
        e.post({CommandType::SetDrumHit, 1.0f, 3, 4});
        e.post({CommandType::SetDrumStepRoll, 0.0f, 3, 4 | (4 << 8) | (1 << 12) | (16 << 16)});
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        render(e, 100, 64);
        CHECK(e.laneTriggerCount(3) == 1);
        render(e, 6000 * 4 + 1600, 64);  // step 4's first hit, then the hits at 1500 and 3000
        CHECK(e.laneTriggerCount(3) == 3);
        e.post({CommandType::SetTransport, 0.0f});
        render(e, 6000 * 8, 64);
        CHECK(e.laneTriggerCount(3) == 3);
        ps::Engine fast(kRate);
        fast.post({CommandType::SetTempo, 300.0f});  // a step = 2400 frames
        // A click on the pad: each hit heard is a burst after silence (counted in the sound itself,
        // not only in the triggers: review m12).
        std::vector<float> click(64, 0.0f);
        click[0] = 1.0f;
        fast.post({CommandType::InstallSample, 0.0f, 2, 0, ps::Sample::create(click.data(), 64, 1, kRate, kRate)});
        fast.post({CommandType::SetPadParam, 0.0f, 2, ps::Sampler::Release});
        fast.post({CommandType::SetDrumHit, 1.0f, 2, 0});
        fast.post({CommandType::SetDrumStepRoll, 0.0f, 2, 0 | (1 << 8) | (1 << 12) | (8 << 16)});
        fast.post({CommandType::SetTransport, 1.0f, 0, 0});
        const auto heard = render(fast, 2400 + kLate + 100, 1024);
        CHECK(fast.laneTriggerCount(2) == 8);
        int onsets = 0, quiet = 1000;
        for (size_t i = 0; i < heard.size() / 2; ++i) {
            const bool loud = std::fabs(heard[2 * i]) > 0.05f;
            if (loud && quiet >= 100) ++onsets;
            quiet = loud ? 0 : quiet + 1;
        }
        std::printf("  8 hits a step at 300 BPM: %d heard\n", onsets);
        CHECK(onsets == 8);
        // More than 8 a step is refused: 16 over one step become 8.
        fast.post({CommandType::SetTransport, 0.0f});
        fast.post({CommandType::SetDrumStepRoll, 0.0f, 2, 0 | (1 << 8) | (1 << 12) | (16 << 16)});
        fast.post({CommandType::SetTransport, 1.0f, 0, 0});
        render(fast, 2400, 1024);
        CHECK(fast.laneTriggerCount(2) == 16);
    }
    // The song ends on a bar line (ArmSceneSwap b = 2): the early hit of that bar's first step,
    // which never plays, is not queued (review of phase 34: it rang past the song's end).
    {
        ps::Engine e(kRate);
        e.post({CommandType::SetTempo, 120.0f});
        e.post({CommandType::SetDrumHit, 1.0f, 3, 0});
        e.post({CommandType::SetDrumStepOffset, -10.0f, 3, 0});
        e.post({CommandType::ArmSceneSwap, 0.0f, 32, 2});
        e.post({CommandType::SetTransport, 1.0f, 0, 0});
        render(e, 6000 * 32 + 256, 256);
        std::printf("  song end with an early hit on its first step: %u hits, running %d\n", e.laneTriggerCount(3),
                    e.transportRunning() ? 1 : 0);
        CHECK(!e.transportRunning());
        CHECK(e.laneTriggerCount(3) == 2);  // bar 1 (on the grid after play) and bar 2 (early): not bar 3
    }
}

// The 808: its fundamental follows the note, its glide reaches the target in its time, the
// 60-250 Hz band (heard on a phone) sits within 6 dB of the fundamental's, it decays as asked,
// never clips nor goes non-finite, and costs little.
void testBass808() {
    ps::DrumKit kit;
    kit.prepare(kRate);
    kit.setKind(0, ps::DrumKit::Bass808);
    CHECK(kit.kind(0) == ps::DrumKit::Bass808);
    kit.setVoiceParam(0, ps::DrumKit::Tune, 33.0f);  // A0 = 55 Hz
    kit.setVoiceParam(0, ps::DrumKit::Decay, 2.0f);
    // One hit, then an octave up with a 100 ms glide at 1 s.
    const auto out = renderMonoKit(kit, kRate * 3, {{0, 0, 1.0f, 0.0f, 0}, {kRate, 0, 1.0f, 12.0f, 4800}});
    CHECK(allFinite(out));
    CHECK(peakOf(out) < 0.99f);
    CHECK(peakOf(out) > 0.3f);
    const double hz1 = toneHz(out, 4800, 24000);  // after the attack's drop
    std::printf("  808 at A0: %.1f Hz, after the glide %.1f Hz, peak %.2f\n", hz1, kit.bassHz(0), peakOf(out));
    CHECK(std::fabs(hz1 - 55.0) < 2.0);
    CHECK(std::fabs(kit.bassHz(0) - 110.0) < 0.05);
    const double hz2 = toneHz(out, kRate + 4800 + 2400, kRate + 4800 + 24000);
    CHECK(std::fabs(hz2 - 110.0) < 3.0);
    // The glide: half-way through, between the two (not jumped).
    {
        ps::DrumKit k2;
        k2.prepare(kRate);
        k2.setKind(0, ps::DrumKit::Bass808);
        k2.setVoiceParam(0, ps::DrumKit::Tune, 33.0f);
        renderMonoKit(k2, kRate + 2400, {{0, 0, 1.0f, 0.0f, 0}, {kRate, 0, 1.0f, 12.0f, 4800}});
        const float mid = k2.bassHz(0);
        std::printf("  808 glide half-way: %.1f Hz\n", mid);
        CHECK(mid > 70.0f && mid < 90.0f);
        renderMonoKit(k2, 2400, {});
        CHECK(std::fabs(k2.bassHz(0) - 110.0f) < 0.05f);
    }
    // Bands of the first second (note at 55 Hz): 60-250 Hz against 20-60 Hz.
    const double fundamental = bandEnergy(out, 2400, 2400 + kRate, 20.0, 60.0);
    const double low = bandEnergy(out, 2400, 2400 + kRate, 60.0, 250.0);
    const double lowDb = 10.0 * std::log10(low / fundamental);
    std::printf("  808: 60-250 Hz at %+.1f dB of 20-60 Hz\n", lowDb);
    CHECK(lowDb >= -6.0);
    // Decay: 2 s to -40 dB means still sounding at 1.5 s and under -40 dB after 2.2 s.
    {
        ps::DrumKit k3;
        k3.prepare(kRate);
        k3.setKind(0, ps::DrumKit::Bass808);
        k3.setVoiceParam(0, ps::DrumKit::Decay, 2.0f);
        const auto one = renderMonoKit(k3, kRate * 3, {{0, 0, 1.0f, 0.0f, 0}});
        const int length = lengthTo(one, -40.0);
        std::printf("  808 decay 2 s: -40 dB at %.2f s\n", length / double(kRate));
        CHECK(length > kRate * 1.5 && length < kRate * 2.6);
        k3.setVoiceParam(0, ps::DrumKit::Decay, 0.3f);
        const auto shortOne = renderMonoKit(k3, kRate, {{0, 0, 1.0f, 0.0f, 0}});
        CHECK(lengthTo(shortOne, -40.0) < kRate * 0.45);
    }
    // Mono: panned hard left, both sides get it the same.
    {
        ps::DrumKit k4;
        k4.prepare(kRate);
        k4.setKind(0, ps::DrumKit::Bass808);
        std::vector<float> l(kRate), r(kRate);
        k4.trigger(0, 1.0f, 0, -1.0f);
        k4.render(l.data(), r.data(), kRate);
        CHECK(std::fabs(energy(l) / energy(r) - 1.0) < 1e-6);
    }
    // CPU: a bar of 808 notes (4 a second) for 10 s.
    {
        ps::DrumKit k5;
        k5.prepare(kRate);
        k5.setKind(0, ps::DrumKit::Bass808);
        std::vector<std::tuple<int, int, float, float, int>> hits;
        for (int k = 0; k < 40; ++k) hits.emplace_back(k * 12000, 0, 0.9f, static_cast<float>(k % 5), k % 2 ? 2400 : 0);
        const double t0 = threadSeconds();
        const auto ten = renderMonoKit(k5, kRate * 10, hits);
        const double load = (threadSeconds() - t0) / 10.0;
        CHECK(allFinite(ten));
        std::printf("  808: %.2f %% of a core\n", 100.0 * load);
        CHECK_TIMING(load < 0.01);
    }
}

// The 808 through the engine: a pad's voice, the step's pitch and glide, kit v3's kicks alive.
void testBass808ThroughTheEngine() {
    using ps::CommandType;
    ps::Engine e(kRate);
    e.post({CommandType::SetTempo, 120.0f});
    e.post({CommandType::SetPadVoice, 1.0f, 7});
    e.post({CommandType::SetPadVoiceParam, 33.0f, 7, ps::DrumKit::Tune});
    e.post({CommandType::SetPadVoiceParam, 1.5f, 7, ps::DrumKit::Decay});
    e.post({CommandType::SetDrumHit, 1.0f, 7, 0});
    e.post({CommandType::SetDrumHit, 1.0f, 7, 4});
    e.post({CommandType::SetDrumStepDetail, 0.0f, 7, 4 | ((7 + 64) << 16)});  // a fifth up
    e.post({CommandType::SetDrumStepGlide, 100.0f, 7, 4});
    e.post({CommandType::SetTransport, 1.0f, 0, 0});
    render(e, 24000 + 2400, 256);
    const float mid = e.drums().bassHz(7);
    render(e, 2400 + 100, 256);
    const float after = e.drums().bassHz(7);
    std::printf("  808 in the engine: half-way %.1f Hz, after the glide %.1f Hz\n", mid, after);
    CHECK(mid > 60.0f && mid < 80.0f);
    CHECK(std::fabs(after - 82.41f) < 0.1f);
    CHECK(e.laneTriggerCount(7) == 2);
    CHECK(!e.hasNonFinite());
}

// The studio kicks: short (-40 dB within 350 ms), tuned (45..65 Hz), with a click; the rumble
// stays in its band; nothing clips nor goes non-finite.
void testStudioKicks() {
    auto one = [](int kind, float tune, float decay) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        kit.setKind(0, kind);
        if (tune > 0.0f) kit.setVoiceParam(0, ps::DrumKit::Tune, tune);
        if (decay > 0.0f) kit.setVoiceParam(0, ps::DrumKit::Decay, decay);
        return renderMonoKit(kit, kRate, {{0, 0, 1.0f, 0.0f, 0}});
    };
    for (int kind : {ps::DrumKit::KickStudio, ps::DrumKit::KickHard}) {
        const auto x = one(kind, 0.0f, 0.0f);
        CHECK(allFinite(x));
        const float peak = peakOf(x);
        const int length = lengthTo(x, -40.0);
        // 40-80 Hz carries the body (against 80-160 and 20-40), 2-5 kHz the attack in the first 20 ms.
        const double body = bandEnergy(x, 0, 24000, 40.0, 80.0);
        const double above = bandEnergy(x, 0, 24000, 80.0, 160.0);
        const double below = bandEnergy(x, 0, 24000, 20.0, 40.0);
        const double clickDb = 10.0 * std::log10(bandEnergy(x, 0, 960, 2000.0, 5000.0, 1) / std::max(bandEnergy(x, 0, 960, 20.0, 5000.0, 1), 1e-20));
        std::printf("  %s kick: peak %.2f, -40 dB at %d ms, 80-160 Hz %+.1f dB, 20-40 Hz %+.1f dB, 2-5 kHz in the first 20 ms %+.1f dB\n",
                    kind == ps::DrumKit::KickStudio ? "studio" : "hard", peak, length * 1000 / kRate,
                    10.0 * std::log10(above / body), 10.0 * std::log10(below / body), clickDb);
        CHECK(peak > 0.5f && peak < 0.95f);
        CHECK(length <= kRate * 350 / 1000);
        CHECK(below < body);
        if (kind == ps::DrumKit::KickStudio) CHECK(above < body);  // the hard kick's « doof » sits higher
        CHECK(clickDb > -25.0);
    }
    // Tune: the tail's fundamental follows it.
    for (float tune : {45.0f, 65.0f}) {
        const auto x = one(ps::DrumKit::KickStudio, tune, 0.5f);
        const double hz = toneHz(x, 6000, 14000);
        std::printf("  studio kick tuned to %.0f Hz: %.1f Hz in the tail\n", tune, hz);
        CHECK(std::fabs(hz - tune) < tune * 0.08);
    }
    // A longer decay is allowed (up to 1.2 s), a shorter one too.
    CHECK(lengthTo(one(ps::DrumKit::KickStudio, 0.0f, 0.8f), -40.0) > kRate * 0.6);
    CHECK(lengthTo(one(ps::DrumKit::KickStudio, 0.0f, 0.15f), -40.0) < kRate * 0.2);
    // Sub and click layers can be taken out.
    {
        ps::DrumKit kit;
        kit.prepare(kRate);
        kit.setKind(0, ps::DrumKit::KickStudio);
        kit.setVoiceParam(0, ps::DrumKit::Click, 0.0f);
        const auto noClick = renderMonoKit(kit, kRate, {{0, 0, 1.0f, 0.0f, 0}});
        const auto withClick = one(ps::DrumKit::KickStudio, 0.0f, 0.0f);
        CHECK(bandEnergy(noClick, 0, 960, 2000.0, 5000.0) < 0.25 * bandEnergy(withClick, 0, 960, 2000.0, 5000.0));
        kit.setVoiceParam(0, ps::DrumKit::Sub, 0.0f);
        const auto noSub = renderMonoKit(kit, kRate, {{0, 0, 1.0f, 0.0f, 0}});
        CHECK(energy(noSub, 9600, 24000) < 0.5 * energy(noClick, 9600, 24000));
    }
    // The rumble: 30-150 Hz holds at least 90 % of its energy; it rises (no click) and decays.
    {
        const auto x = one(ps::DrumKit::RumbleVoice, 0.0f, 0.0f);
        CHECK(allFinite(x));
        const double band = bandEnergy(x, 0, kRate, 30.0, 150.0, 2);
        const double all = bandEnergy(x, 0, kRate, 1.0, 2000.0, 2);
        std::printf("  rumble: peak %.2f, 30-150 Hz = %.0f %% of the energy, -40 dB at %d ms\n", peakOf(x), 100.0 * band / all, lengthTo(x, -40.0) * 1000 / kRate);
        CHECK(band > 0.9 * all);
        CHECK(peakOf(x, 0, 240) < 0.5f * peakOf(x));
        CHECK(lengthTo(x, -40.0) > kRate * 0.4 && lengthTo(x, -40.0) < kRate * 0.9);
    }
    // Back to the kit's own voice: the pad sounds as before.
    {
        ps::DrumKit a, b;
        a.prepare(kRate);
        b.prepare(kRate);
        b.setKind(0, ps::DrumKit::KickHard);
        b.setKind(0, ps::DrumKit::KitVoice);
        CHECK(renderMonoKit(a, 24000, {{0, 0, 1.0f, 0.0f, 0}}) == renderMonoKit(b, 24000, {{0, 0, 1.0f, 0.0f, 0}}));
        // In kit v3 too: the pad's own kick is the studio kick again, with its settings.
        ps::DrumKit c, d;
        c.prepare(kRate);
        c.setVersion(3);
        d.prepare(kRate);
        d.setVersion(3);
        d.setKind(0, ps::DrumKit::Bass808);
        d.setKind(0, ps::DrumKit::KitVoice);
        CHECK(renderMonoKit(c, 24000, {{0, 0, 1.0f, 0.0f, 0}}) == renderMonoKit(d, 24000, {{0, 0, 1.0f, 0.0f, 0}}));
    }
}

// Kit v3: the kick is the short studio kick, pad 2 the hard one, the hats and the clap bright;
// every voice stays near its kit v2 loudness (the kick a little under: it is short), the kit has
// air, and kits v1 and v2 are untouched.
void testKitV3() {
    // A voice's loudness as kit v2 set it: one hit's K-weighted energy over a beat at 128 BPM.
    auto hit = [](int version, int voice, float* peak = nullptr) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        kit.setVersion(version);
        std::vector<float> l(kRate), r(kRate);
        kit.trigger(voice, 1.0f, 0);
        kit.render(l.data(), r.data(), kRate);
        if (peak) *peak = std::max(peakOf(l), peakOf(r));
        ps::KWeighting kl, kr;
        kl.prepare(kRate);
        kr.prepare(kRate);
        double e = 0.0;
        for (int i = 0; i < kRate * 60 / 128; ++i) {
            const double a = kl.process(l[size_t(i)]), b = kr.process(r[size_t(i)]);
            e += a * a + b * b;
        }
        return e;
    };
    for (int voice = 0; voice < ps::DrumKit::kVoices; ++voice) {
        float peak = 0.0f, peak2 = 0.0f;
        const double v3 = hit(3, voice, &peak);
        const double db = 10.0 * std::log10(v3 / hit(2, voice, &peak2));
        if (db != 0.0) std::printf("  kit v3 voice %2d: %+.2f dB against v2 (peak %.2f, v2 %.2f)\n", voice, db, peak, peak2);
        CHECK(peak < std::max(0.95f, peak2 * 1.02f));  // the clap of v2 already peaked at 1.03
        CHECK(std::fabs(db) < 1.0);
    }
    // The v3 kick is the studio kick: short.
    {
        ps::DrumKit kit;
        kit.prepare(kRate);
        kit.setVersion(3);
        const auto kick = renderMonoKit(kit, kRate, {{0, ps::DrumKit::Kick, 1.0f, 0.0f, 0}});
        CHECK(lengthTo(kick, -40.0) <= kRate * 350 / 1000);
        const auto hard = renderMonoKit(kit, kRate, {{0, ps::DrumKit::Kick2, 1.0f, 0.0f, 0}});
        CHECK(lengthTo(hard, -40.0) <= kRate * 350 / 1000);
    }
    // Air: a bar of house (kick on the beats, clap on 2 and 4, closed hats on the eighths, the
    // open hat on the off-beats) at 128 BPM, above 6 kHz against the whole: more in kit v3 (the
    // mix's air, against its target of -20..-16 dB, is the bench's: docs/son/banc/apres-S3-moteur.md).
    auto airOf = [](int version) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        kit.setVersion(version);
        std::vector<std::tuple<int, int, float, float, int>> hits;
        const int beat = kRate * 60 / 128;
        for (int bar = 0; bar < 4; ++bar) {
            for (int b = 0; b < 4; ++b) {
                const int at = (bar * 4 + b) * beat;
                hits.emplace_back(at, ps::DrumKit::Kick, 1.0f, 0.0f, 0);
                if (b % 2 == 1) hits.emplace_back(at, ps::DrumKit::Clap, 0.9f, 0.0f, 0);
                hits.emplace_back(at, ps::DrumKit::ClosedHat, 0.7f, 0.0f, 0);
                hits.emplace_back(at + beat / 2, ps::DrumKit::OpenHat, 0.8f, 0.0f, 0);
            }
        }
        return airDb(renderMonoKit(kit, beat * 16 + kRate / 2, hits), 6000.0f);
    };
    const double air2 = airOf(2), air3 = airOf(3);
    std::printf("  air above 6 kHz: kit v2 %+.1f dB, kit v3 %+.1f dB\n", air2, air3);
    CHECK(air3 > air2 + 0.5);
    // Kits v1 and v2 unchanged by the new code paths: the clap and hats of v2 and v1 match their
    // own structure (three bursts 10 ms apart, no sizzle): their energy above 7 kHz is the old
    // one, well under v3's.
    for (int voice : {ps::DrumKit::ClosedHat, ps::DrumKit::OpenHat, ps::DrumKit::Clap}) {
        ps::DrumKit a, b;
        a.prepare(kRate);
        a.setVersion(2);
        b.prepare(kRate);
        b.setVersion(3);
        b.setVersion(2);
        CHECK(renderMonoKit(a, 24000, {{0, voice, 1.0f, 0.0f, 0}}) == renderMonoKit(b, 24000, {{0, voice, 1.0f, 0.0f, 0}}));
    }
    // Through the engine's command, back to v2 on a new one.
    {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetKitVersion, 3.0f});
        e.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, 0});
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto out = render(e, kRate, 256);
        CHECK(e.drums().version() == 3);
        std::vector<float> left(kRate);
        for (int i = 0; i < kRate; ++i) left[size_t(i)] = out[size_t(i) * 2];
        CHECK(lengthTo(left, -40.0) - kLate <= kRate * 400 / 1000);
        e.post({ps::CommandType::SetKitVersion, 2.0f});
        render(e, 256, 256);
        CHECK(e.drums().version() == 2);
    }
}

}  // namespace studio_tests

namespace studio_tests {

// Sampled kits: velocity layers and round robin on a pad, the replaced sounds kept alive while
// they ring (ASan), a base sound clearing the layers, and the round robin from the transport's
// start (an export plays the take's layers).
void testPadLayers() {
    auto tone = [](float level) {
        std::vector<float> x(4800);
        for (size_t i = 0; i < x.size(); ++i) x[i] = level;
        return ps::Sample::create(x.data(), static_cast<int64_t>(x.size()), 1, kRate, kRate);
    };
    ps::Sampler sampler;
    sampler.prepare(kRate);
    ps::Sample* base = tone(0.1f);
    ps::Sample* soft = tone(0.2f);
    ps::Sample* loudA = tone(0.3f);
    ps::Sample* loudB = tone(0.4f);
    for (ps::Sample* s : {base, soft, loudA, loudB}) s->retain();  // ours, to compare with
    sampler.install(3, base);
    sampler.installLayer(3, 0, 0, 63, soft);
    sampler.installLayer(3, 1, 64, 127, loudA);
    sampler.installLayer(3, 2, 64, 127, loudB);
    CHECK(sampler.layerCount(3) == 3);
    std::vector<float> l(256), r(256);
    auto hit = [&](float velocity) {
        sampler.trigger(3, velocity, 0);
        sampler.render(l.data(), r.data(), 256);
        return sampler.lastPlayed(3);
    };
    CHECK(hit(0.3f) == soft);
    CHECK(hit(0.9f) == loudA);
    CHECK(hit(0.9f) == loudB);
    CHECK(hit(0.9f) == loudA);
    CHECK(hit(0.2f) == soft);  // alone in its range: always it
    // Layer 2 replaced while it rings: it plays on (no use after free), then goes.
    CHECK(hit(1.0f) == loudB);
    ps::Sample* loudC = tone(0.5f);
    loudC->retain();
    sampler.installLayer(3, 2, 64, 127, loudC);
    for (int k = 0; k < 40; ++k) sampler.render(l.data(), r.data(), 256);
    sampler.collectGarbage();
    // A velocity in no range: the nearest range's layers.
    soft->retain();  // the reference handed over (the same sound again: only its range changes)
    sampler.installLayer(3, 0, 0, 20, soft);
    CHECK(hit(0.3f) == soft);  // 38: 18 from soft's range, 26 from the loud ones
    const ps::Sample* nearLoud = hit(0.4f);  // 51: the loud ones, in turn
    CHECK(nearLoud == loudA || nearLoud == loudC);
    // A base sound clears the layers.
    base->retain();
    sampler.install(3, base);
    CHECK(sampler.layerCount(3) == 0);
    CHECK(hit(0.9f) == base);
    for (int k = 0; k < 40; ++k) sampler.render(l.data(), r.data(), 256);
    sampler.collectGarbage();
    // Our references only are left on the removed layers (the engine let them go).
    for (ps::Sample* s : {soft, loudA, loudB, loudC}) CHECK(s->release());
    for (ps::Sample* s : {soft, loudA, loudB, loudC}) delete s;
    CHECK(!base->release());
    // Through the engine: layers installed by InstallSample, the round robin from the start.
    auto take = [&](ps::Engine& e) {
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        return render(e, 6000 * 8, 256);
    };
    ps::Engine e(kRate);
    e.post({ps::CommandType::SetTempo, 120.0f});
    for (int layer = 0; layer < 3; ++layer) {
        ps::Command c{ps::CommandType::InstallSample, 0.0f, ps::Sampler::layerArg(4, layer, 0, 127), 0};
        c.sample = tone(0.1f + 0.1f * static_cast<float>(layer));
        e.post(c);
    }
    for (int step = 0; step < 8; ++step) e.post({ps::CommandType::SetDrumHit, 1.0f, 4, step});
    const auto first = take(e);
    e.post({ps::CommandType::SetTransport, 0.0f});
    render(e, 6000 * 4, 256);
    const auto second = take(e);
    CHECK(first == second);
    CHECK(e.padHasSample(4));
    // Hits 0, 1, 2 play layers 0, 1, 2: three levels.
    auto at = [&](int step) { return std::fabs(first[size_t(step * 6000 + kLate + 1000) * 2]); };
    std::printf("  layers through the engine: %.3f %.3f %.3f %.3f\n", at(0), at(1), at(2), at(3));
    CHECK(at(0) < at(1) && at(1) < at(2) && std::fabs(at(3) - at(0)) < 0.02f * at(0));
}

// Worst case, kit v3: every pad on every step at 160 BPM, three of them studio voices (808,
// rumble, hard kick), the hats rolling 8 a step with offsets: within the kit's budget (10 %).
void testKitV3CpuBudget() {
    using ps::CommandType;
    ps::Engine engine(kRate);
    engine.post({CommandType::SetKitVersion, 3.0f});
    engine.post({CommandType::SetTempo, 160.0f});
    engine.post({CommandType::SetPadVoice, 1.0f, 7});
    engine.post({CommandType::SetPadVoice, 4.0f, 8});
    engine.post({CommandType::SetPadVoice, 3.0f, 9});
    for (int lane = 0; lane < 16; ++lane) {
        for (int step = 0; step < 16; ++step) {
            engine.post({CommandType::SetDrumHit, 0.8f, lane, step});
            if (lane == 7) {
                engine.post({CommandType::SetDrumStepDetail, 0.0f, 7, step | ((step % 5 + 64) << 16)});
                engine.post({CommandType::SetDrumStepGlide, 80.0f, 7, step});
            }
            if (lane == ps::DrumKit::ClosedHat) {
                engine.post({CommandType::SetDrumStepRoll, 0.5f, lane, step | (1 << 8) | (2 << 12) | (8 << 16)});
                engine.post({CommandType::SetDrumStepOffset, step % 2 ? 4.0f : -3.0f, lane, step});
            }
        }
    }
    engine.post({CommandType::SetTransport, 1.0f, 0});
    const int frames = kRate * 6;
    std::vector<float> out;
    double load = 1e9;
    for (int pass = 0; pass < 3; ++pass) {
        const double start = threadSeconds();
        out = render(engine, frames, 256);
        load = std::min(load, (threadSeconds() - start) / (static_cast<double>(frames) / kRate));
    }
    std::printf("  full kit v3 with studio voices and rolls: %.1f %% of real time on this machine\n", load * 100.0);
#if !defined(__SANITIZE_ADDRESS__)
    CHECK_TIMING(load < 0.10);
#endif
    CHECK(allFinite(out));
    CHECK(!engine.hasNonFinite());
}

// The master's volume curve (the song's fades: AutoTarget.masterVolume()) is heard, live and in
// the export, smoothed, after the limiter (never above unity); it goes back to unity when the
// transport stops or its curve goes.
void testMasterVolumeAutomation() {
    using ps::CommandType;
    auto setUp = [](auto& target) {
        target.post({CommandType::SetTempo, 120.0f});
        std::vector<float> tone(kRate * 4);
        for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.2f * static_cast<float>(std::sin(2.0 * M_PI * 220.0 * double(i) / kRate));
        target.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(tone.data(), static_cast<int64_t>(tone.size()), 1, kRate, kRate)});
        target.post({CommandType::SetDrumHit, 1.0f, 5, 0});
        target.post({CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, ps::Mixer::kMaster, 0, 0)});
        target.post({CommandType::SetAutomationPoint, 0.8f, 0, 0});  // unity
        target.post({CommandType::SetAutomationPoint, 0.4f, 0, 4});  // a quarter: -12 dB
    };
    auto rms = [](const std::vector<float>& out, int from, int to) {
        double e = 0.0;
        for (int i = from; i < to; ++i) e += double(out[size_t(i) * 2]) * out[size_t(i) * 2];
        return std::sqrt(e / (to - from));
    };
    ps::Engine e(kRate);
    setUp(e);
    e.post({CommandType::SetTransport, 1.0f, 0, 0});
    const auto live = render(e, 6000 * 8, 256);
    const double liveDb = 20.0 * std::log10(rms(live, 6000 * 6, 6000 * 7) / rms(live, 6000 * 2, 6000 * 3));
    // Smoothed: no step from one frame to the next at the change.
    float jump = 0.0f;
    for (int i = 6000 * 4 + kLate - 50; i < 6000 * 4 + kLate + 600; ++i) {
        jump = std::max(jump, std::fabs(std::fabs(live[size_t(i) * 2]) - std::fabs(live[size_t(i - 1) * 2])));
    }
    std::printf("  master fade live: %+.2f dB, largest frame-to-frame change %.4f\n", liveDb, jump);
    CHECK(std::fabs(liveDb + 12.0) < 0.3);
    CHECK(jump < 0.02f);
    e.post({CommandType::SetTransport, 0.0f});
    render(e, 256, 256);
    CHECK(e.mixer().masterFader() == 1.0f);
    // The curve gone while it held the master down: back to unity.
    e.post({CommandType::SetTransport, 1.0f, 0, 0});
    render(e, 6000 * 5, 256);
    CHECK(e.mixer().masterFader() < 0.3f);
    e.post({CommandType::SetAutomationTarget, 0.0f, 0, -1});
    render(e, 256, 256);
    CHECK(e.mixer().masterFader() == 1.0f);
    // Above 0.8 is still unity (no gain past the limiter).
    e.mixer().setVolume(ps::Mixer::kMaster, 1.0f);
    CHECK(e.mixer().masterFader() == 1.0f);
    // The export.
    ps::OfflineRenderer r(kRate);
    setUp(r);
    const auto out = r.renderToBuffer(6000 * 8, 0);
    const double exportDb = 20.0 * std::log10(rms(out, 6000 * 6, 6000 * 7) / rms(out, 6000 * 2, 6000 * 3));
    std::printf("  master fade in the export: %+.2f dB\n", exportDb);
    CHECK(std::fabs(exportDb + 12.0) < 0.3);
    // Leaving the song while a song curve holds the master down (a fade-in, the silent beat
    // before a Drop): the scene going on by itself is heard at its level (review of phase 34).
    ps::Engine s(kRate);
    s.post({CommandType::SetTempo, 120.0f});
    s.post({CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, ps::Mixer::kMaster, 0, 0)});
    s.post({CommandType::SetAutomationMode, 4.0f, 0, ps::Automation::Song});
    for (int b = 0; b < 4; ++b) s.post({CommandType::SetAutomationSong, 0.0f, 0, b});
    s.post({CommandType::SetTransport, 1.0f, 0, 0});
    render(s, 6000 * 6, 256);
    const float held = s.mixer().masterFader();
    s.post({CommandType::ArmSceneSwap, 0.0f, -1, 3});
    render(s, 6000 * 6, 256);
    std::printf("  master in the song %.2f, after leaving it %.2f\n", held, s.mixer().masterFader());
    CHECK(held < 0.05f);
    CHECK(s.mixer().masterFader() == 1.0f);
}

// Pad solo (channels 16..31): a soloed pad strip alone, through the drums' strip; the drums'
// other pads and the tracks quiet; nothing soloed: everything back.
void testPadSolo() {
    using ps::CommandType;
    auto take = [](bool solo, bool snare) {
        ps::Engine e(kRate);
        e.post({CommandType::SetTempo, 120.0f});
        e.post({CommandType::SetPadStrip, 1.0f, ps::DrumKit::Kick});
        e.post({CommandType::SetDrumHit, 0.6f, ps::DrumKit::Kick, 0});
        if (snare) e.post({CommandType::SetDrumHit, 0.6f, ps::DrumKit::Snare, 4});
        if (solo) e.post({CommandType::SetChannelSolo, 1.0f, ps::Mixer::kFirstPad + ps::DrumKit::Kick});
        e.post({CommandType::SetTransport, 1.0f, 0});
        return render(e, 6000 * 8, 256);
    };
    const auto kickAlone = take(false, false);
    const auto both = take(false, true);
    const auto soloed = take(true, true);
    const double alone = energy(kickAlone), all = energy(both), solo = energy(soloed);
    std::printf("  pad solo: kick alone %.2f, kick + snare %.2f, kick soloed with the snare on %.2f\n", alone, all, solo);
    CHECK(all > alone * 1.005);                          // the snare is heard
    CHECK(std::fabs(solo - alone) < 0.01 * (all - alone));  // not while the kick's pad is soloed
    // Solo off: both again.
    ps::Engine e(kRate);
    e.post({CommandType::SetPadStrip, 1.0f, ps::DrumKit::Kick});
    e.post({CommandType::SetChannelSolo, 1.0f, ps::Mixer::kFirstPad + ps::DrumKit::Kick});
    e.post({CommandType::SetChannelSolo, 0.0f, ps::Mixer::kFirstPad + ps::DrumKit::Kick});
    e.post({CommandType::SetDrumHit, 0.6f, ps::DrumKit::Kick, 0});
    e.post({CommandType::SetDrumHit, 0.6f, ps::DrumKit::Snare, 4});
    e.post({CommandType::SetTransport, 1.0f, 0});
    CHECK(std::fabs(energy(render(e, 6000 * 8, 256)) / all - 1.0) < 1e-6);
}

}  // namespace studio_tests
