// « Des basses qui tapent » (docs/hits/basses-qui-tapent.md): the bass exciter (« Basse audible »,
// insert 29), the 808's version 2, the synth's bass settings (pitch envelope, phase reset, velocity
// to cutoff, sub shape and floor, mid layer) and the master's low cut of mix version 3. What a phone
// keeps of a sound is read through the bench's model: a 4th-order Butterworth high-pass at 300 Hz.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace basses_tests {

using studio_tests::allFinite;
using studio_tests::bandEnergy;
using studio_tests::peakOf;

// x through a 4th-order Butterworth high-pass at hz (the phone model; at 25-30 Hz: the low cut).
std::vector<float> highPass4(const std::vector<float>& x, float hz) {
    ps::Biquad a, b;
    a.highPassQ(static_cast<float>(kRate), hz, 0.5412f);
    b.highPassQ(static_cast<float>(kRate), hz, 1.3066f);
    std::vector<float> y(x.size());
    for (size_t i = 0; i < x.size(); ++i) y[i] = b.process(a.process(x[i]));
    return y;
}

double energyOf(const std::vector<float>& x, size_t from = 0, size_t to = SIZE_MAX) {
    to = std::min(to, x.size());
    double e = 0.0;
    for (size_t i = from; i < to; ++i) e += static_cast<double>(x[i]) * x[i];
    return e;
}

// What a phone keeps of x (dB, energy through the 300 Hz model against its own; nearer 0 is better).
double phoneDb(const std::vector<float>& x, float hz = 300.0f, size_t from = 0) {
    const auto y = highPass4(x, hz);
    return 10.0 * std::log10(std::max(energyOf(y, from), 1e-30) / std::max(energyOf(x, from), 1e-30));
}

// B5, the attack of a note starting at `at`: the loudest 5 ms RMS of its first 40 ms against the
// median 5 ms RMS from 80 to 250 ms (dB).
double attackDb(const std::vector<float>& x, size_t at) {
    const size_t w = kRate / 200;
    auto rms = [&](size_t from) {
        double e = 0.0;
        for (size_t i = from; i < from + w && i < x.size(); ++i) e += static_cast<double>(x[i]) * x[i];
        return std::sqrt(e / static_cast<double>(w));
    };
    double top = 0.0;
    for (size_t k = at; k + w <= at + kRate / 25; k += w / 5) top = std::max(top, rms(k));
    std::vector<double> body;
    for (size_t k = at + kRate * 80 / 1000; k + w <= at + kRate / 4; k += w) body.push_back(rms(k));
    std::sort(body.begin(), body.end());
    const double med = body.empty() ? 1e-9 : body[body.size() / 2];
    return 20.0 * std::log10(std::max(top, 1e-12) / std::max(med, 1e-12));
}

std::vector<float> sine(double hz, float amp, int frames) {
    std::vector<float> x(static_cast<size_t>(frames));
    for (int i = 0; i < frames; ++i) x[static_cast<size_t>(i)] = amp * static_cast<float>(std::sin(2.0 * M_PI * hz * i / kRate));
    return x;
}

// The exciter through its insert slot, stereo in place (both sides the same input).
std::vector<float> excite(const std::vector<float>& in, float amount, std::vector<float>* right = nullptr) {
    ps::InsertSlot slot;
    slot.prepare(static_cast<float>(kRate));
    slot.setType(ps::InsertSlot::BassExciterFx);
    slot.setParam(0, amount);
    std::vector<float> l = in, r = in;
    for (size_t pos = 0; pos < l.size(); pos += 192) {
        const int n = static_cast<int>(std::min<size_t>(192, l.size() - pos));
        slot.process(l.data() + pos, r.data() + pos, n);
    }
    if (right) *right = r;
    return l;
}

// The exciter: off, the sound untouched (bit for bit); on, the harmonics of a 45 Hz sine (3rd to
// 10th) there, nothing between them (no aliasing: under -60 dB), the phone hearing it far better
// for little more energy on a big system, mono (the same on both sides), finite on silence and on
// a huge input, and no allocation.
void testBassExciter() {
    const auto in = sine(45.0, 0.3f, kRate * 2);
    {
        const auto off = excite(in, 0.0f);
        CHECK(firstDifference(off, in) < 0);
    }
    std::vector<float> right;
    const auto out = excite(in, 0.5f, &right);
    CHECK(allFinite(out));
    CHECK(firstDifference(out, right) < 0);
    // One second (45 whole periods) after the first half second: each harmonic's bin, then the bins between.
    const size_t from = kRate / 2, to = from + kRate;
    const double fundamental = bandEnergy(out, from, to, 44.0, 47.0);
    double worstBetween = -300.0;
    for (int k = 3; k <= 10; ++k) {
        const double h = bandEnergy(out, from, to, 45.0 * k - 1.0, 45.0 * k + 2.0);
        const double hDb = 10.0 * std::log10(std::max(h, 1e-30) / fundamental);
        std::printf("  exciter 0.5 on a 45 Hz sine: harmonic %d at %+.1f dB\n", k, hDb);
        CHECK(hDb > -45.0);
        const double between = bandEnergy(out, from, to, 45.0 * k + 20.0, 45.0 * k + 25.0);
        worstBetween = std::max(worstBetween, 10.0 * std::log10(std::max(between, 1e-30) / fundamental));
    }
    std::printf("  exciter: between the harmonics at most %+.1f dB\n", worstBetween);
    CHECK(worstBetween < -60.0);
    const double phoneDry = phoneDb(in, 300.0f, from), phoneWet = phoneDb(out, 300.0f, from);
    const double fullDb = 10.0 * std::log10(energyOf(out, from) / energyOf(in, from));
    std::printf("  exciter 0.5: phone (300 Hz) %+.1f -> %+.1f dB, full band %+.2f dB\n", phoneDry, phoneWet, fullDb);
    CHECK(phoneWet > phoneDry + 20.0);
    CHECK(fullDb < 1.0);
    // Nothing added under 28 Hz (the low cut in front of it is not undone): what the exciter adds
    // (wet minus dry) between 5 and 27 Hz stays far under its harmonics.
    {
        std::vector<float> added(out.size());
        for (size_t i = 0; i < out.size(); ++i) added[i] = out[i] - in[i];
        const double infra = bandEnergy(added, from, to, 5.0, 28.0);
        const double third = bandEnergy(added, from, to, 134.0, 137.0);
        const double infraDb = 10.0 * std::log10(std::max(infra, 1e-30) / third);
        std::printf("  exciter 0.5: added under 28 Hz %+.1f dB of its 3rd harmonic\n", infraDb);
        CHECK(infraDb < -60.0);
    }
    // « Fréquence » and « Haut » turned at once (a curve, a knob): the filters glide, no click. The
    // largest step between two samples of what it adds, while they move, stays that of the settings
    // held still, before (the old ones) and after (the new ones).
    {
        ps::InsertSlot slot;
        slot.prepare(static_cast<float>(kRate));
        slot.setType(ps::InsertSlot::BassExciterFx);
        slot.setParam(0, 1.0f);
        std::vector<float> l = in, r = in;
        for (size_t pos = 0; pos < l.size(); pos += 192) {
            if (pos == size_t(kRate)) {
                slot.setParam(1, 1.0f);
                slot.setParam(3, 0.0f);
            }
            slot.process(l.data() + pos, r.data() + pos, static_cast<int>(std::min<size_t>(192, l.size() - pos)));
        }
        auto worst = [&](size_t a, size_t b) {
            float w = 0.0f;
            for (size_t i = a; i < b; ++i) w = std::max(w, std::fabs((l[i] - in[i]) - (l[i - 1] - in[i - 1])));
            return w;
        };
        const float before = worst(kRate / 2, kRate), moving = worst(kRate, kRate + 4800), after = worst(kRate + 9600, kRate * 2);
        std::printf("  exciter: largest step of what it adds %.4f before, %.4f while turned, %.4f after\n", before, moving, after);
        CHECK(moving < 1.2f * std::max(before, after));
    }
    // A NaN in (a broken plug-in upstream): it goes on adding its harmonics once the input is finite again.
    {
        auto broken = in;
        broken[size_t(kRate) / 4] = std::numeric_limits<float>::quiet_NaN();
        const auto after = excite(broken, 0.5f);
        CHECK(std::isnan(after[size_t(kRate) / 4]));
        bool finiteAfter = true;
        for (size_t i = size_t(kRate) / 4 + 1; i < after.size(); ++i) finiteAfter = finiteAfter && std::isfinite(after[i]);
        CHECK(finiteAfter);
        const double third = bandEnergy(after, from, to, 134.0, 137.0) / fundamental;
        CHECK(10.0 * std::log10(std::max(third, 1e-30)) > -45.0);
    }
    // Silence in, silence out; a huge input stays finite.
    {
        const auto quiet = excite(std::vector<float>(kRate / 2, 0.0f), 1.0f);
        CHECK(peakOf(quiet) == 0.0f);
        const auto loud = excite(sine(50.0, 1000.0f, kRate / 2), 1.0f);
        CHECK(allFinite(loud));
    }
#ifdef PS_COUNT_ALLOCATIONS
    {
        ps::InsertSlot slot;
        slot.prepare(static_cast<float>(kRate));
        slot.setType(ps::InsertSlot::BassExciterFx);
        std::vector<float> l = in, r = in;
        g_allocations = 0;
        g_countAllocations = true;
        for (int k = 0; k < 50; ++k) {
            slot.setParam(0, (k % 10) / 10.0f);
            slot.setParam(1, (k % 7) / 7.0f);
            slot.process(l.data() + k * 192, r.data() + k * 192, 192);
        }
        slot.reset();
        g_countAllocations = false;
        CHECK(g_allocations.load() == 0);
    }
#endif
}

std::vector<float> render808(float version, float drive, float tone, int frames, float drop = 24.0f, float dropTime = 0.025f, float tune = 29.0f) {
    ps::DrumKit kit;
    kit.prepare(kRate);
    kit.setKind(0, ps::DrumKit::Bass808);
    kit.setVoiceParam(0, ps::DrumKit::Tune, tune);  // 29: F1, 43.7 Hz
    kit.setVoiceParam(0, ps::DrumKit::Decay, 1.3f);
    kit.setVoiceParam(0, ps::DrumKit::Drive, drive);
    kit.setVoiceParam(0, ps::DrumKit::Tone, tone);
    kit.setVoiceParam(0, ps::DrumKit::Click, 0.35f);
    if (version >= 0.0f) {
        kit.setVoiceParam(0, ps::DrumKit::Version, version);
        kit.setVoiceParam(0, ps::DrumKit::PitchDrop, drop);
        kit.setVoiceParam(0, ps::DrumKit::DropTime, dropTime);
    }
    return studio_tests::renderMonoKit(kit, frames, {{0, 0, 1.0f, 0.0f, 0}});
}

// The 808's version 2 (« l'808 qui tape »): version 0 is the 808 as before, bit for bit (the
// projects saved before keep it); version 1 drops from +24 semitones over 25 ms (the « boum »),
// its « tone » adds the 4th to 8th harmonics: through the phone model it is far louder than
// version 0 with the same settings, its attack stronger, it never clips nor goes non-finite.
void testBass808Version2() {
    const int frames = kRate;
    {
        const auto before = render808(-1.0f, 0.3f, 0.4f, frames);
        const auto zero = render808(0.0f, 0.3f, 0.4f, frames, 30.0f, 0.1f);
        CHECK(firstDifference(before, zero) < 0);
    }
    const auto v0 = render808(0.0f, 0.7f, 0.5f, frames);
    const auto v2 = render808(1.0f, 0.7f, 0.5f, frames);
    CHECK(allFinite(v2));
    CHECK(peakOf(v2) < 0.99f);
    const double p0 = phoneDb(v0), p2 = phoneDb(v2);
    const double a0 = attackDb(v0, 0), a2 = attackDb(v2, 0);
    std::printf("  808 F1, drive 0.7, tone 0.5: phone (300 Hz) v0 %+.1f, v2 %+.1f dB; attack v0 %+.1f, v2 %+.1f dB\n", p0, p2, a0, a2);
    std::printf("  808 v2 against v0: %+.1f dB of energy, peak %.2f\n", 10.0 * std::log10(energyOf(v2) / energyOf(v0)), peakOf(v2));
    CHECK(p2 > p0 + 4.0);
    CHECK(a2 >= 4.0);
    CHECK(a2 > a0);
    // The drop: the first 10 ms well over the note (+24 st falling over 25 ms), the note itself after 150 ms.
    const auto clean = render808(1.0f, 0.0f, 0.0f, frames);
    const double early = toneHz(clean, 0, kRate / 100), late = toneHz(clean, kRate * 15 / 100, kRate * 6 / 10);
    std::printf("  808 v2 drop: %.0f Hz over its first 10 ms, %.1f Hz after 150 ms\n", early, late);
    CHECK(early > 2.5 * 43.65);
    CHECK(std::fabs(late - 43.65) < 2.0);
    // Phonk's hot drive (over 0.75: the second clip) stays under full scale.
    const auto hot = render808(1.0f, 0.9f, 0.6f, frames);
    CHECK(allFinite(hot));
    CHECK(peakOf(hot) < 0.99f);
    // Its highest note (C5, the voice's top) on a +36 semitone drop, all its tone: the 6th to 8th
    // harmonics would be over 0.45 x the rate there (faded out, not folded back), it stays finite and clean.
    {
        const auto high = render808(1.0f, 0.0f, 1.0f, kRate / 2, 36.0f, 0.15f, 72.0f);
        CHECK(allFinite(high));
        CHECK(peakOf(high) < 0.99f);
    }
}

std::vector<float> synthNotes(ps::Synth& s, int note, const std::vector<float>& velocities, int every, int length) {
    const int frames = every * static_cast<int>(velocities.size()) + kRate / 4;
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    int k = 0;
    for (int pos = 0; pos < frames; pos += 192) {
        const int n = std::min(192, frames - pos);
        for (size_t v = 0; v < velocities.size(); ++v) {
            const int on = static_cast<int>(v) * every, off = on + length;
            if (on >= pos && on < pos + n) s.noteOn(note, velocities[v], on - pos);
            if (off >= pos && off < pos + n) s.noteOff(note, off - pos);
        }
        s.render(out.data() + pos, n);
        ++k;
    }
    return out;
}

void bassPatch(ps::Synth& s) {
    s.setParam(ps::Synth::Osc1Wave, 0.0f);
    s.setParam(ps::Synth::OscMix, 0.0f);
    s.setParam(ps::Synth::SubLevel, 0.5f);
    s.setParam(ps::Synth::Cutoff, 0.34f);
    s.setParam(ps::Synth::Resonance, 0.3f);
    s.setParam(ps::Synth::FilterEnvAmount, 0.55f);
    s.setParam(ps::Synth::FilterAttack, 0.0f);
    s.setParam(ps::Synth::FilterDecay, 0.42f);
    s.setParam(ps::Synth::FilterSustain, 0.0f);
    s.setParam(ps::Synth::AmpAttack, 0.0f);
    s.setParam(ps::Synth::AmpSustain, 0.6f);
    s.setParam(ps::Synth::AmpRelease, 0.2f);
}

// The synth's bass settings: each at its default leaves the sound as it was (plain(), bit for bit);
// the phase reset makes 16 hits of one note attack alike (B6: within 0.5 dB); the pitch envelope
// starts the note above it; the velocity opens the filter; the sub's floor takes it out of the
// infra (a sub of F#1 at 23 Hz); its sine has no odd harmonics to speak of; no allocation.
void testSynthBassSettings() {
    {
        ps::Synth a, b;
        a.prepare(kRate);
        b.prepare(kRate);
        bassPatch(a);
        bassPatch(b);
        b.setParam(ps::Synth::PitchEnv, 0.0f);
        b.setParam(ps::Synth::PitchDecay, 0.5f);
        b.setParam(ps::Synth::PhaseReset, 0.0f);
        b.setParam(ps::Synth::VelocityCutoff, 0.0f);
        b.setParam(ps::Synth::SubShape, 0.0f);
        b.setParam(ps::Synth::SubFloor, 0.0f);
        b.setParam(ps::Synth::MidLayerHz, 0.5f);
        b.setParam(ps::Synth::MidLayerDrive, 0.5f);
        CHECK(a.plain() && b.plain());
        const std::vector<float> vel{1.0f, 0.7f, 0.9f, 0.5f};
        const auto x = synthNotes(a, 41, vel, kRate / 8, kRate / 16), y = synthNotes(b, 41, vel, kRate / 8, kRate / 16);
        CHECK(firstDifference(x, y) < 0);
    }
    // 16 hits of F2, each one after the last has died away (the voice silent: its phase reset).
    auto spread = [](bool reset) {
        ps::Synth s;
        s.prepare(kRate);
        bassPatch(s);
        s.setParam(ps::Synth::PhaseReset, reset ? 1.0f : 0.0f);
        const int every = kRate * 37 / 100;  // not a whole number of the note's periods
        const auto out = synthNotes(s, 41, std::vector<float>(16, 0.9f), every, kRate / 4);
        double sum = 0.0, sq = 0.0;
        for (int k = 0; k < 16; ++k) {
            const double a = attackDb(out, static_cast<size_t>(k * every));
            sum += a;
            sq += a * a;
        }
        const double mean = sum / 16.0;
        return std::make_pair(mean, std::sqrt(std::max(0.0, sq / 16.0 - mean * mean)));
    };
    const auto [freeMean, freeSd] = spread(false);
    const auto [resetMean, resetSd] = spread(true);
    std::printf("  16 hits of F2: attack %+.1f dB (sd %.2f) free running, %+.1f dB (sd %.2f) with the phase reset\n", freeMean, freeSd,
                resetMean, resetSd);
    CHECK(resetSd <= 0.5);
    // The pitch envelope: +24 semitones falling over 22 ms.
    {
        ps::Synth s;
        s.prepare(kRate);
        openFilter(s);
        s.setParam(ps::Synth::Osc1Wave, 1.0f);
        s.setParam(ps::Synth::OscMix, 0.0f);
        s.setParam(ps::Synth::PhaseReset, 1.0f);
        s.setParam(ps::Synth::PitchEnv, 0.5f);
        s.setParam(ps::Synth::PitchDecay, 0.5f);
        CHECK(!s.plain());
        s.noteOn(45, 1.0f);  // A2, 110 Hz
        const auto out = renderSynth(s, kRate / 2);
        const double early = toneHz(out, 0, kRate / 100), late = toneHz(out, kRate / 5, kRate / 2);
        std::printf("  pitch envelope +24 st / 22 ms on A2: %.0f Hz over the first 10 ms, %.1f Hz after 200 ms\n", early, late);
        CHECK(early > 2.0 * 110.0);
        CHECK(std::fabs(late - 110.0) < 2.0);
    }
    // The velocity on the cutoff: a hit at 1 brighter than one at 0.4 (above 500 Hz against the whole).
    {
        auto bright = [](float velocity) {
            ps::Synth s;
            s.prepare(kRate);
            bassPatch(s);
            s.setParam(ps::Synth::VelocityCutoff, 0.5f);
            const auto out = synthNotes(s, 41, {velocity}, kRate / 4, kRate / 5);
            return phoneDb(out, 500.0f);
        };
        const double soft = bright(0.4f), hard = bright(1.0f);
        std::printf("  velocity -> cutoff 0.5: over 500 Hz at velocity 0.4 %+.1f dB, at 1 %+.1f dB\n", soft, hard);
        CHECK(hard > soft + 6.0);
    }
    // The sub's floor: F#1's square sub (23 Hz) an octave up; its sine has no 3rd harmonic to speak of.
    {
        auto infra = [](float floor, float shape, double* third) {
            ps::Synth s;
            s.prepare(kRate);
            openFilter(s);
            s.setParam(ps::Synth::Cutoff, 0.3f);
            s.setParam(ps::Synth::OscMix, 0.0f);
            s.setParam(ps::Synth::Osc1Wave, 1.0f);
            s.setParam(ps::Synth::SubLevel, 0.9f);
            s.setParam(ps::Synth::SubFloor, floor);
            s.setParam(ps::Synth::SubShape, shape);
            if (third) {
                // A2's sub (55 Hz): its 3rd harmonic (165 Hz) is the sub's alone (the triangle's are at 330, 550…).
                s.noteOn(45, 1.0f);
                const auto out = renderSynth(s, kRate * 2);
                *third = 10.0 * std::log10(bandEnergy(out, kRate / 2, kRate * 3 / 2, 164.0, 166.0) / bandEnergy(out, kRate / 2, kRate * 3 / 2, 54.0, 56.0));
                return 0.0;
            }
            s.noteOn(30, 1.0f);  // F#1, 46.2 Hz: its sub at 23.1 Hz
            const auto out = renderSynth(s, kRate * 2);
            const double total = energyOf(out, kRate / 2, kRate * 2);
            const double under = energyOf(highPass4(out, 30.0f), kRate / 2, kRate * 2);
            return 10.0 * std::log10(std::max(1e-30, total - under) / total);  // the share under 30 Hz
        };
        const double before = infra(0.0f, 0.0f, nullptr);
        double squareThird = 0.0, sineThird = 0.0;
        const double floored = infra(1.0f, 0.0f, nullptr);
        infra(0.0f, 0.0f, &squareThird);
        infra(0.0f, 1.0f, &sineThird);
        std::printf("  F#1 sub: under 30 Hz %+.1f dB of the sound, with the floor %+.1f dB; 3rd harmonic square %+.1f, sine %+.1f dB\n", before,
                    floored, squareThird, sineThird);
        CHECK(before > -6.0);
        CHECK(floored < -12.0);
        CHECK(sineThird < squareThird - 6.0);
        // « Basse ronde » (oscillator 2 an octave down, a square sub) on C1: with the floor, nothing under 30 Hz.
        auto ronde = [](float floor) {
            ps::Synth s;
            s.prepare(kRate);
            s.setParam(ps::Synth::Osc2Octave, 0.1f);
            s.setParam(ps::Synth::SubLevel, 0.6f);
            s.setParam(ps::Synth::Cutoff, 0.38f);
            s.setParam(ps::Synth::SubFloor, floor);
            s.noteOn(24, 1.0f);  // C1, 32.7 Hz: oscillator 2 and the sub at 16.4 Hz
            const auto out = renderSynth(s, kRate * 2);
            // 10 to 25 Hz against the note's own 32.7 Hz (one second, whole periods of both).
            return 10.0 * std::log10(std::max(1e-30, bandEnergy(out, kRate / 2, kRate * 3 / 2, 10.0, 25.0)) / bandEnergy(out, kRate / 2, kRate * 3 / 2, 32.0, 34.0));
        };
        const double rondeBefore = ronde(0.0f), rondeFloored = ronde(1.0f);
        std::printf("  « Basse ronde » on C1: 10-25 Hz at %+.1f dB of its note, with the floor %+.1f dB\n", rondeBefore, rondeFloored);
        CHECK(rondeFloored < rondeBefore - 10.0);
    }
#ifdef PS_COUNT_ALLOCATIONS
    {
        ps::Synth s;
        s.prepare(kRate);
        bassPatch(s);
        s.setParam(ps::Synth::PitchEnv, 0.4f);
        s.setParam(ps::Synth::PhaseReset, 1.0f);
        s.setParam(ps::Synth::VelocityCutoff, 0.5f);
        s.setParam(ps::Synth::SubShape, 1.0f);
        s.setParam(ps::Synth::SubFloor, 1.0f);
        s.setParam(ps::Synth::MidLayer, 0.5f);
        s.setParam(ps::Synth::MidLayerHz, 0.3f);
        s.setParam(ps::Synth::MidLayerDrive, 0.8f);
        std::vector<float> l(192), r(192);
        g_allocations = 0;
        g_countAllocations = true;
        for (int k = 0; k < 200; ++k) {
            if (k % 10 == 0) s.noteOn(36 + k % 7, 0.5f + (k % 3) * 0.2f);
            if (k % 10 == 6) s.noteOff(36 + (k - 6) % 7);
            s.render(l.data(), r.data(), 192);
        }
        g_countAllocations = false;
        CHECK(g_allocations.load() == 0);
        CHECK(allFinite(l) && allFinite(r));
    }
#endif
}

// The master's low cut (mix version 3): with a target, a 4th-order high-pass at 25 Hz takes the
// infra out (15 Hz down by more than 15 dB) and leaves the bass (50 Hz within 0.3 dB); without
// the low cut (versions 1 and 2) the chain is as before.
void testMasterLowCut() {
    auto through = [](bool lowCut, double hz) {
        ps::MasterChain chain;
        chain.prepare(kRate);
        chain.setMode(ps::MasterChain::Mode::Drop);
        chain.setLowCut(lowCut);
        chain.setTarget(-14.0f);
        chain.holdDrive(0.0f);
        auto x = sine(hz, 0.1f, kRate * 3);
        std::vector<float> l = x, r = x;
        for (size_t pos = 0; pos < l.size(); pos += 192) {
            const int n = static_cast<int>(std::min<size_t>(192, l.size() - pos));
            chain.process(l.data() + pos, r.data() + pos, nullptr, n);
        }
        return 10.0 * std::log10(energyOf(l, kRate * 2) / energyOf(x, kRate * 2));
    };
    const double infraOff = through(false, 15.0), infraOn = through(true, 15.0);
    const double bassOff = through(false, 50.0), bassOn = through(true, 50.0);
    std::printf("  master low cut: 15 Hz %+.1f -> %+.1f dB, 50 Hz %+.2f -> %+.2f dB\n", infraOff, infraOn, bassOff, bassOn);
    CHECK(infraOn < infraOff - 15.0);
    CHECK(std::fabs(bassOn - bassOff) < 0.3);
    CHECK(std::fabs(bassOff) < 0.1);
    // An export (the offline renderer's own master chain) keeps the low cut: the loud loop's lows under
    // 20 Hz go down, its loudness stays.
    {
        auto exported = [](float version) {
            ps::OfflineRenderer r(kRate);
            prepareLoudLoop(r, 0.5f);
            r.post({ps::CommandType::SetMixVersion, version});
            r.post({ps::CommandType::SetMaster, -10.0f, 4});
            const auto out = r.renderToBuffer(int64_t(kRate) * 8, 0);
            std::vector<float> mono(out.size() / 2);
            for (size_t i = 0; i < mono.size(); ++i) mono[i] = 0.5f * (out[2 * i] + out[2 * i + 1]);
            // Measured in the band (5 to 19 Hz, whole periods over 4 s), not as the total minus a
            // high-passed copy: that difference is the filter's own phase and ripple, and went negative.
            const size_t from = size_t(kRate) * 2, to = size_t(kRate) * 6;
            double total = 0.0;
            for (size_t i = from; i < to; ++i) total += double(mono[i]) * mono[i];
            return 10.0 * std::log10(std::max(1e-30, bandEnergy(mono, from, to, 5.0, 20.0)) / std::max(1e-30, total));
        };
        const double v2 = exported(2.0f), v3 = exported(3.0f);
        std::printf("  export: under 20 Hz %+.1f dB of the loop (mix version 2), %+.1f dB (version 3)\n", v2, v3);
        CHECK(v2 > -120.0);  // something there to take out (the check below is not met by two silences)
        CHECK(v3 < v2 - 3.0);
    }
    // Through the engine: SetMixVersion 3 is the Drop mode with the low cut, 2 without it.
    {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetMixVersion, 3.0f});
        render(e, 256, 256);
        CHECK(e.mixer().masterChain().mode() == ps::MasterChain::Mode::Drop);
        CHECK(e.mixer().masterChain().lowCut());
        e.post({ps::CommandType::SetMixVersion, 2.0f});
        render(e, 256, 256);
        CHECK(!e.mixer().masterChain().lowCut());
    }
}

}  // namespace basses_tests

#define PS_BASSES_TESTS \
    {"bass_exciter", basses_tests::testBassExciter}, \
    {"bass_808_version_2", basses_tests::testBass808Version2}, \
    {"synth_bass_settings", basses_tests::testSynthBassSettings}, \
    {"master_low_cut", basses_tests::testMasterLowCut},
