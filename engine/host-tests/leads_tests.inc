// Leads chantier, lot 1 (docs/hits/leads-pros.md § 8): the real FM voices of Plaits (the DX7 banks,
// models 2 to 4) play louder the harder a note is struck at sound version 3 (the assistant's songs
// since this lot), and exactly as before at versions 1 and 2 (every project saved earlier).
// Included by engine_tests.cpp (kept apart to ease merging).

namespace leads_tests {

// One note of a Plaits patch, alone: its first 0.4 s (mono).
std::vector<float> plaitsNote(int model, float harmonics, float velocity, int velocityLevel) {
    ps::PlaitsSynth s;
    s.prepare(kRate);
    s.setParam(ps::PlaitsSynth::Model, model / 23.0f);
    s.setParam(ps::PlaitsSynth::Harmonics, harmonics);
    if (velocityLevel >= 0) s.setVelocityLevel(velocityLevel == 1);
    std::vector<float> out(size_t(kRate) * 2 / 5, 0.0f);
    s.noteOn(60, velocity, 0);
    for (size_t pos = 0; pos < out.size(); pos += 256) s.render(out.data() + pos, int(std::min<size_t>(256, out.size() - pos)));
    return out;
}

double rmsOf(const std::vector<float>& x, size_t from = 0, size_t step = 1) {
    double e = 0.0;
    size_t n = 0;
    for (size_t i = from; i < x.size(); i += step, ++n) e += double(x[i]) * x[i];
    return std::sqrt(e / std::max<size_t>(1, n));
}

// 1. E.PIANO 1 (model 3, the first patch): at version 3 a note at velocity 0.4 is at least 4 dB under
// one at 1.0 (0.3 + 0.7 v: 4.7 dB), while off it moves by under 1 dB (accent only), and off renders
// sample for sample what a synth never told anything renders. A model with Plaits' own low-pass gate
// (8, virtual analog) is left as it was.
void testFmVelocitySetsTheLevel() {
    const float ePiano = 0.5f / 32.0f / 1.02f;
    const auto quietOff = plaitsNote(3, ePiano, 0.4f, 0), loudOff = plaitsNote(3, ePiano, 1.0f, 0);
    const auto quietOn = plaitsNote(3, ePiano, 0.4f, 1), loudOn = plaitsNote(3, ePiano, 1.0f, 1);
    const double off = 20 * std::log10(rmsOf(loudOff) / rmsOf(quietOff)), on = 20 * std::log10(rmsOf(loudOn) / rmsOf(quietOn));
    std::printf("  E.PIANO 1, velocity 1.0 over 0.4: %.2f dB before, %.2f dB at sound version 3\n", off, on);
    CHECK(std::fabs(off) < 1.0);
    CHECK(on >= 4.0);
    // Off: exactly as a synth never told (every saved project); at full velocity on: the same too.
    CHECK(quietOff == plaitsNote(3, ePiano, 0.4f, -1));
    CHECK(loudOn == loudOff);
    // Virtual analog (model 8): its velocity already goes through the low-pass gate, untouched.
    CHECK(plaitsNote(8, 0.5f, 0.4f, 1) == plaitsNote(8, 0.5f, 0.4f, 0));
}

// 2. Through the engine: SetSoundVersion 1 and 2 render a soft FM note sample for sample as before
// (no command at all); 3 plays it quieter, and a full-velocity one as before.
void testSoundVersionGatesTheFmVelocity() {
    auto play = [](int version, float velocity) {
        ps::Engine engine(kRate);
        if (version > 0) engine.post({ps::CommandType::SetSoundVersion, float(version)});
        engine.post({ps::CommandType::SetSynthEngine, 1.0f, 1});  // track 0 on Plaits
        engine.post({ps::CommandType::SetPlaitsParam, 3.0f / 23.0f, ps::PlaitsSynth::Model});
        engine.post({ps::CommandType::SetPlaitsParam, 2.5f / 32.0f / 1.02f, ps::PlaitsSynth::Harmonics});  // WINTRHODES
        engine.post({ps::CommandType::SynthNoteOn, velocity, 60});
        return render(engine, kRate / 2, 256);
    };
    const auto none = play(0, 0.4f);
    CHECK(play(1, 0.4f) == none);
    CHECK(play(2, 0.4f) == none);
    CHECK(play(3, 1.0f) == play(2, 1.0f));
    const double drop = 20 * std::log10(rmsOf(play(3, 0.4f), size_t(kLate) * 2, 2) / rmsOf(none, size_t(kLate) * 2, 2));
    std::printf("  WINTRHODES at velocity 0.4, sound version 3 against 2: %.2f dB\n", drop);
    CHECK(drop < -3.0 && drop > -6.0);
}

}  // namespace leads_tests

#define PS_LEADS_TESTS                                                                \
    {"leads_fm_velocity_level", leads_tests::testFmVelocitySetsTheLevel},             \
    {"leads_sound_version_fm_velocity", leads_tests::testSoundVersionGatesTheFmVelocity},
