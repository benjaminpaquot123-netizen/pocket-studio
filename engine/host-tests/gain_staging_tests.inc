// Phase 33, honest gain staging (docs/plan/son.md step 1): the compressor's make-up, detector,
// side-chain high-pass, mix and gain-reduction meter, the saturation without its hidden low-pass,
// and kit v2. Included by engine_tests.cpp (kept apart to ease merging).

namespace gain_staging {

// Gain (dB) of a sine at `amp` through `chain` in order, measured as sineGainDb does.
float chainGainDb(std::vector<ps::InsertSlot>& chain, float freq, float amp) {
    constexpr size_t kN = 9600;
    std::vector<float> l(kN), r(kN);
    double sum = 0, ref = 0;
    for (auto& fx : chain) fx.reset();
    for (int pass = 0; pass < 3; ++pass) {
        for (size_t i = 0; i < kN; ++i) l[i] = r[i] = amp * static_cast<float>(std::sin(2 * M_PI * freq * (pass * kN + i) / kRate));
        for (auto& fx : chain) fx.process(l.data(), r.data(), static_cast<int>(kN));
    }
    for (size_t i = kN / 2; i < kN; ++i) {
        sum += l[i] * l[i];
        const double x = amp * std::sin(2 * M_PI * freq * (2 * kN + i) / kRate);
        ref += x * x;
    }
    return static_cast<float>(10 * std::log10(sum / ref));
}

std::vector<ps::InsertSlot> chainOf(const std::vector<std::pair<int, std::vector<float>>>& fx) {
    std::vector<ps::InsertSlot> chain(fx.size());
    for (size_t k = 0; k < fx.size(); ++k) {
        chain[k].prepare(kRate);
        chain[k].setType(fx[k].first);
        for (size_t p = 0; p < fx[k].second.size(); ++p) chain[k].setParam(static_cast<int>(p), fx[k].second[p]);
    }
    return chain;
}

// The glue chains the assistant puts on its songs (WizardMix.withGlue: keep the numbers in step):
// under the threshold they leave the sound alone, 0 dB from 20 Hz to 18 kHz. Before phase 33 the
// master's added +3.7 dB (the compressor's fixed make-up) and lost 3.1 dB at 18 kHz (the saturation's
// low-pass).
void testGlueChainsAreFlatUnderTheThreshold() {
    using S = ps::InsertSlot;
    const std::vector<std::pair<const char*, std::vector<std::pair<int, std::vector<float>>>>> chains = {
        {"master", {{S::Comp, {0.6f, 0.2f, 0.7f, 0.45f, 0.0f, 0.0f, 0.0f, 0.5f, 1.0f}}, {S::Saturation, {0.2f, 1.0f, 1.0f, 0.7f, 1.0f}}}},
        {"drum bus", {{S::Comp, {0.6f, 0.25f, 0.45f, 0.3f, 0.0f, 0.0f, 0.0f, 0.5f, 1.0f}}, {S::Saturation, {0.15f, 1.0f, 1.0f, 0.7f, 1.0f}}}},
    };
    const float freqs[] = {20, 50, 100, 1000, 5000, 10000, 16000, 18000};
    for (const auto& [name, fx] : chains) {
        auto chain = chainOf(fx);
        std::printf("  %s chain at -40 dBFS:", name);
        for (float f : freqs) {
            const float g = chainGainDb(chain, f, 0.01f);
            std::printf(" %g Hz %+.2f", f, g);
            CHECK(std::fabs(g) <= (f == 1000 ? 0.2f : 0.3f));
        }
        std::printf("\n");
    }
}

// The compressor: no make-up of its own (the user's, or « Auto »), RMS or peak detector, a
// side-chain high-pass, a dry/wet mix; the settings of a project saved before phase 33 (the fixed
// make-up written as its « Gain » by Effects.kt) sound as they did.
void testCompressorMakeUpDetectorAndMix() {
    using S = ps::InsertSlot;
    auto comp = [](std::vector<float> params) {
        params.resize(ps::Compressor::kParams, 0.0f);
        if (params.size() > 8 && params[8] == 0.0f) params[8] = 1.0f;  // mix: compressed unless said
        return chainOf({{S::Comp, params}});
    };
    // Below the threshold: 0 dB, auto or not.
    auto plain = comp({0.6f, 0.2f, 0.2f, 0.3f});
    auto automatic = comp({0.6f, 0.2f, 0.2f, 0.3f, 0.0f, 1.0f});
    CHECK(std::fabs(chainGainDb(plain, 1000, 0.01f)) < 0.05f);
    CHECK(std::fabs(chainGainDb(automatic, 1000, 0.01f)) < 0.05f);
    // A project saved before: threshold -16 dB, ratio 1.82, the old make-up (half the full-scale
    // reduction, 3.61 dB) now its Gain: the same +3.61 dB as before.
    const float threshold = -40.0f + 40.0f * 0.6f, ratio = std::pow(20.0f, 0.2f);
    const float oldMakeup = -threshold * (1.0f - 1.0f / ratio) * 0.5f;
    auto old = comp({0.6f, 0.2f, 0.2f, 0.3f, oldMakeup / 24.0f});
    const float oldGain = chainGainDb(old, 1000, 0.01f);
    std::printf("  compressor: old project's make-up %+.2f dB (was %+.2f)\n", oldGain, oldMakeup);
    CHECK(std::fabs(oldGain - oldMakeup) < 0.02f);
    // « Auto » gives back the reduction it makes on average: a loud steady sine comes out about as
    // loud as it went in, where the plain compressor pulls it down.
    auto squeeze = [&](bool autoOn) {
        auto c = comp({0.5f, 0.54f, 0.2f, 0.3f, 0.0f, autoOn ? 1.0f : 0.0f});  // -20 dB, ratio ~5
        std::vector<float> l(static_cast<size_t>(kRate) * 4), r(l.size());
        for (size_t i = 0; i < l.size(); ++i) l[i] = r[i] = 0.5f * static_cast<float>(std::sin(2 * M_PI * 200.0 * i / kRate));
        c[0].process(l.data(), r.data(), static_cast<int>(l.size()));
        double sum = 0;
        for (size_t i = l.size() - kRate / 2; i < l.size(); ++i) sum += l[i] * l[i];
        return std::make_pair(static_cast<float>(10 * std::log10(sum / (kRate / 2) / (0.5 * 0.5 / 2))), c[0].compressor().autoMakeupDb());
    };
    const auto [down, none] = squeeze(false);
    const auto [back, given] = squeeze(true);
    std::printf("  compressor: loud sine %+.1f dB plain, %+.1f dB with auto make-up (+%.1f dB)\n", down, back, given);
    CHECK(down < -6.0f && none == 0.0f);
    CHECK(std::fabs(back) < 1.5f && given > 6.0f);
    // RMS reads a sine 3 dB under its peak: it squeezes less.
    auto reductionOf = [&](std::vector<float> params, float freq, float amp) {
        auto c = comp(std::move(params));
        chainGainDb(c, freq, amp);
        return c[0].compressor().gainReductionDb();
    };
    const float peak = reductionOf({0.5f, 0.54f, 0.2f, 0.3f}, 1000, 0.5f);
    const float rms = reductionOf({0.5f, 0.54f, 0.2f, 0.3f, 0.0f, 0.0f, 1.0f}, 1000, 0.5f);
    std::printf("  compressor: reduction %.1f dB peak, %.1f dB RMS\n", peak, rms);
    CHECK(peak < -6.0f && rms > peak + 1.5f);
    // The side-chain high-pass (0.5: ~90 Hz): a loud 40 Hz no longer drives it, 1 kHz still does.
    const float bass = reductionOf({0.5f, 0.54f, 0.2f, 0.3f}, 40, 0.5f);
    const float bassFiltered = reductionOf({0.5f, 0.54f, 0.2f, 0.3f, 0.0f, 0.0f, 0.0f, 0.5f}, 40, 0.5f);
    const float midFiltered = reductionOf({0.5f, 0.54f, 0.2f, 0.3f, 0.0f, 0.0f, 0.0f, 0.5f}, 1000, 0.5f);
    std::printf("  compressor: 40 Hz reduction %.1f dB, with the side-chain high-pass %.1f dB (1 kHz %.1f dB)\n", bass, bassFiltered, midFiltered);
    CHECK(bass < -6.0f && bassFiltered > -1.5f && midFiltered < -6.0f);
    // Mix: 0 is the dry sound exactly, 0.5 in between.
    auto dry = comp({0.0f, 1.0f, 0.0f, 0.3f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f});
    dry[0].setParam(8, 0.0f);
    std::vector<float> l(4800), r(4800), l0(4800);
    for (size_t i = 0; i < l.size(); ++i) l0[i] = l[i] = r[i] = 0.8f * static_cast<float>(std::sin(2 * M_PI * 300.0 * i / kRate));
    dry[0].process(l.data(), r.data(), 4800);
    CHECK(l == l0);
    const float full = reductionOf({0.0f, 1.0f, 0.0f, 0.3f}, 300, 0.8f);
    auto half = comp({0.0f, 1.0f, 0.0f, 0.3f});
    half[0].setParam(8, 0.5f);
    const float halfGain = chainGainDb(half, 300, 0.8f);
    std::printf("  compressor: squeezed %.1f dB, half mix %+.1f dB\n", full, halfGain);
    CHECK(halfGain < -3.0f && halfGain > full + 3.0f);
#if defined(PS_COUNT_ALLOCATIONS)
    // Every new setting on the audio thread: nothing allocated.
    auto all = comp({0.3f, 0.7f, 0.1f, 0.5f, 0.2f, 1.0f, 1.0f, 0.6f, 0.7f});
    auto sat = chainOf({{S::Saturation, {0.6f, 0.4f, 0.8f, 0.7f, 1.0f}}});
    g_allocations = 0;
    g_countAllocations = true;
    for (int p = 0; p < ps::Compressor::kParams; ++p) all[0].setParam(p, 0.4f);
    for (int p = 0; p < ps::Saturator::kParams; ++p) sat[0].setParam(p, 0.6f);
    all[0].process(l.data(), r.data(), 4800);
    sat[0].process(l.data(), r.data(), 4800);
    g_countAllocations = false;
    CHECK(g_allocations == 0);
#endif
}

// The compressor's reduction reaches the app through the mixer's atomics (its meter in the box).
void testCompressorReductionMeter() {
    ps::Engine engine(kRate);
    const int slot = ps::Mixer::kDrums * ps::Mixer::kInserts + 2;
    engine.post({ps::CommandType::SetInsertType, 0.0f, slot, ps::InsertSlot::Comp});
    engine.post({ps::CommandType::SetInsertParam, 0.0f, slot, 0});  // -40 dB
    engine.post({ps::CommandType::SetInsertParam, 1.0f, slot, 1});  // 20:1
    engine.post({ps::CommandType::SetTempo, 120.0f});
    for (int step = 0; step < 16; step += 2) engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, step});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, kRate / 2, 256);
    const float reduction = engine.mixer().takeInsertReduction(ps::Mixer::kDrums, 2);
    std::printf("  compressor meter: %.1f dB\n", reduction);
    CHECK(reduction < -10.0f);
    CHECK(engine.mixer().takeInsertReduction(ps::Mixer::kDrums, 2) == 0.0f);  // read: back to 0
    CHECK(engine.mixer().takeInsertReduction(ps::Mixer::kDrums, 3) == 0.0f);  // no compressor there
    CHECK(engine.mixer().takeInsertReduction(99, 0) == 0.0f);
    render(engine, kRate / 4, 256);
    CHECK(engine.mixer().takeInsertReduction(ps::Mixer::kDrums, 2) < -10.0f);
}

// The saturation: its tone at the top is open (no low-pass), and with compensation the drive no
// longer changes the level; without it (the projects saved before phase 33) it sounds as it did.
void testSaturationToneAndCompensation() {
    using S = ps::InsertSlot;
    auto open = chainOf({{S::Saturation, {0.3f, 1.0f, 1.0f, 0.7f, 1.0f}}});
    for (float f : {50.0f, 1000.0f, 10000.0f, 18000.0f}) {
        const float g = chainGainDb(open, f, 0.01f);
        CHECK(std::fabs(g) < 0.1f);
    }
    // Compensated: a sound at a usual level (-15 dBFS RMS) keeps it whatever the drive; with a
    // light drive, a quiet one too. Not compensated (the default), the drive raises it.
    for (float drive : {0.1f, 0.2f, 0.5f, 0.8f}) {
        auto c = chainOf({{S::Saturation, {drive, 1.0f, 1.0f, 0.7f, 1.0f}}});
        auto raw = chainOf({{S::Saturation, {drive, 1.0f, 1.0f, 0.7f, 0.0f}}});
        const float usual = chainGainDb(c, 1000, 0.25f), quiet = chainGainDb(c, 1000, 0.01f), before = chainGainDb(raw, 1000, 0.25f);
        std::printf("  saturation drive %.1f: compensated %+.2f dB (quiet %+.2f), not compensated %+.2f dB\n", drive, usual, quiet, before);
        CHECK(std::fabs(usual) < 0.25f);
        if (drive <= 0.2f) CHECK(std::fabs(quiet) < 0.1f);
        if (drive >= 0.5f) CHECK(before > 3.0f);
    }
    // The master's saturation of a project saved before (tone 0.85: the 12.4 kHz low-pass kept).
    auto old = chainOf({{S::Saturation, {0.2f, 0.85f, 1.0f, 0.8f}}});
    const float at1k = chainGainDb(old, 1000, 0.01f), at18k = chainGainDb(old, 18000, 0.01f);
    std::printf("  saturation as before: 1 kHz %+.2f dB, 18 kHz %+.2f dB\n", at1k, at18k);
    CHECK(std::fabs(at1k - 0.10f) < 0.1f && at18k < -2.5f && at18k > -3.5f);
}

// Kit v2: the voices near their role's loudness (kick 6 dB down, the closed hat brought from a
// 3 ms tick to a ringing hat 17 dB louder, never over -1 dBFS); kit v1 (the default: projects
// saved before) untouched.
void testKitVersionLevels() {
    auto hit = [](int version, int voice, float* peak = nullptr) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        if (version > 0) kit.setVersion(version);
        std::vector<float> l(kRate), r(kRate);
        kit.trigger(voice, 1.0f, 0);
        kit.render(l.data(), r.data(), kRate);
        double sum = 0;
        float p = 0.0f;
        for (size_t i = 0; i < l.size(); ++i) {
            sum += l[i] * l[i] + r[i] * r[i];
            p = std::max({p, std::fabs(l[i]), std::fabs(r[i])});
        }
        if (peak) *peak = p;
        return sum;
    };
    for (int voice = 0; voice < ps::DrumKit::kVoices; ++voice) CHECK(hit(0, voice) == hit(1, voice));
    float hatPeak = 0.0f;
    const double kick = 10 * std::log10(hit(2, ps::DrumKit::Kick) / hit(1, ps::DrumKit::Kick));
    const double hat = 10 * std::log10(hit(2, ps::DrumKit::ClosedHat, &hatPeak) / hit(1, ps::DrumKit::ClosedHat));
    std::printf("  kit v2: kick %+.2f dB, closed hat %+.2f dB (peak %.1f dBFS)\n", kick, hat, 20 * std::log10(hatPeak));
    CHECK(std::fabs(kick + 6.0) < 0.05);
    CHECK(hat > 14.0 && hat < 20.0 && hatPeak < 0.9f);
    // Through the engine's command (soft hits, far from the limiter); back to v1 on a new command.
    auto kicks = [](std::vector<float> versions) {
        ps::Engine engine(kRate);
        for (float v : versions) engine.post({ps::CommandType::SetKitVersion, v});
        engine.post({ps::CommandType::SetTempo, 120.0f});
        engine.post({ps::CommandType::SetDrumHit, 0.5f, ps::DrumKit::Kick, 0});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto out = render(engine, kRate, 256);
        double sum = 0;
        for (float v : out) sum += static_cast<double>(v) * v;
        return sum;
    };
    const double viaEngine = 10 * std::log10(kicks({2.0f}) / kicks({}));
    std::printf("  kit v2 through the engine: kick %+.2f dB\n", viaEngine);
    CHECK(std::fabs(viaEngine + 6.0) < 0.05);
    CHECK(kicks({2.0f, 1.0f}) == kicks({}));
}

}  // namespace gain_staging
