// Finishing chantier (« restes »): eight audio tracks, 16-bar piano-roll loops, long frozen tracks,
// and the phase 34 review's minor points. Included by engine_tests.cpp (kept apart to ease merging).

namespace restes_tests {

// A frozen track longer than two minutes is rendered in mono: the mean of the stereo render, with
// one pass (no loudness target) and with two (a target: the music is kept, then folded down).
void testMonoRenderIsTheMeanOfTheStereoOne() {
    const int64_t bar = kRate * 2;
    for (const bool target : {false, true}) {
        auto describe = [target](ps::OfflineRenderer& r) {
            r.post({ps::CommandType::SetTempo, 120.0f});
            for (int step = 0; step < 16; step += 4) r.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
            r.post({ps::CommandType::SetDrumHit, 0.8f, 2, 2});
            r.post({ps::CommandType::SetChannelPan, -0.8f, 0});  // the drums to the left: the channels differ
            if (target) r.post({ps::CommandType::SetMaster, -14.0f, 4});
        };
        ps::OfflineRenderer stereoR(kRate), monoR(kRate);
        describe(stereoR);
        describe(monoR);
        const auto stereo = stereoR.renderToBuffer(bar, kRate / 2);
        const auto mono = monoR.renderToBuffer(bar, kRate / 2, true);
        CHECK(stereo.size() == size_t(bar + kRate / 2) * 2);
        CHECK(mono.size() * 2 == stereo.size());
        if (mono.size() * 2 != stereo.size()) continue;
        float diff = 0.0f, peak = 0.0f, sideways = 0.0f;
        for (size_t i = 0; i < mono.size(); ++i) {
            diff = std::max(diff, std::fabs(mono[i] - 0.5f * (stereo[2 * i] + stereo[2 * i + 1])));
            peak = std::max(peak, std::fabs(mono[i]));
            sideways = std::max(sideways, std::fabs(stereo[2 * i] - stereo[2 * i + 1]));
        }
        std::printf("  mono render (%s): peak %.3f, off the stereo mean by %.2g (L-R up to %.2f)\n",
                    target ? "two passes" : "one pass", peak, diff, sideways);
        CHECK(peak > 0.05f);
        CHECK(sideways > 0.05f);
        CHECK(diff < 1e-6f);
    }
}

// 16-bar piano-roll loops: a note starting in bar 16 (5760 ticks, past the 12 bits of b) and a note
// lasting 12.5 bars (4800 ticks) play where they are, live, from a waiting scene and in an export.
void testSixteenBarLoops() {
    constexpr int kTick = 250;  // 120 BPM: a sixteenth is 6000 frames, 24 ticks
    const int lateStart = 15 * 384, longLength = 4800;
    auto describe = [&](auto& target, bool staged) {
        target.post({ps::CommandType::SetTempo, 120.0f});
        if (staged) target.post({ps::CommandType::SetStaging, 1.0f});
        target.post({ps::CommandType::SetSynthSteps, 0.0f, 256});
        target.post({ps::CommandType::SetSynthNote, 0.9f, ps::NoteTrack::packIndex(0, lateStart, 96), ps::NoteTrack::pack(lateStart, 96, 69)});
        target.post({ps::CommandType::SetSynthNote, 0.9f, ps::NoteTrack::packIndex(1, 0, longLength), ps::NoteTrack::pack(0, longLength, 57)});
        target.post({ps::CommandType::SetSynthNoteCount, 0.0f, 2});
        if (staged) {
            target.post({ps::CommandType::SetStaging, 0.0f});
            target.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});  // at the next bar line: the scene starts on step 16
        }
        target.post({ps::CommandType::SetTransport, 1.0f, 0});
    };
    // Live, then from a waiting scene (it starts a bar later).
    for (const bool staged : {false, true}) {
        ps::Engine engine(kRate);
        auto& mixer = engine.mixer();
        describe(engine, staged);
        const int64_t origin = staged ? 16 * 6000 : 0;
        const int ch = ps::trackChannel(0);
        int64_t at = 0;
        // Renders up to frame `to` of the scene (counted from its start), then the channel's peak since the last reading.
        auto upTo = [&](int64_t to) {
            render(engine, static_cast<int>(origin + to - at), 256);
            at = origin + to;
            return mixer.takeChannelPeak(ch);
        };
        const float start = upTo(6000);
        upTo(int64_t(longLength) * kTick - 48000);
        const float held = upTo(int64_t(longLength) * kTick - 8000);  // a sixth of a second before the long note's end
        upTo(int64_t(longLength) * kTick + 3 * kRate);                 // its release rung out
        const float before = upTo(int64_t(lateStart) * kTick - 100);
        const float late = upTo(int64_t(lateStart) * kTick + 8000);
        std::printf("  16 bars (%s): start %.3f, held at 12 bars %.3f, before bar 16 %.5f, bar 16 %.3f\n",
                    staged ? "waiting scene" : "live", start, held, before, late);
        CHECK(start > 0.01f);
        CHECK(held > 0.01f);
        CHECK(before < 1e-3f);
        CHECK(late > 0.01f);
        CHECK(engine.sceneSwaps() == (staged ? 1u : 0u));
    }
    // An export: the same engine offline, the bar-16 note where it belongs (sample-accurate).
    ps::OfflineRenderer r(kRate);
    describe(r, false);
    const int64_t frames = 256 * 6000;
    const auto out = r.renderToBuffer(frames, 0);
    CHECK(out.size() == size_t(frames) * 2);
    if (out.size() == size_t(frames) * 2) {
        const size_t at = size_t(lateStart) * kTick;
        float quiet = 0.0f, loud = 0.0f;
        for (size_t i = at - 24000; i < at; ++i) quiet = std::max(quiet, std::fabs(out[2 * i]));
        for (size_t i = at; i < at + 6000; ++i) loud = std::max(loud, std::fabs(out[2 * i]));
        std::printf("  16 bars (export): before bar 16 %.5f, bar 16 %.3f\n", quiet, loud);
        CHECK(quiet < 1e-3f);
        CHECK(loud > 0.01f);
    }
}

// Review m1: the soft clipper's running level after a silence. A detached bass note (a 0.5 sine at
// 55 Hz, 200 ms) after 1 s of silence, with the level of the note before it 1.2 s back: its attack
// goes through (it is under the margin of its own level), where it used to be squashed to -60 dBFS.
void testSoftClipperAfterSilence() {
    ps::SoftClipper c;
    c.prepare(float(kRate));
    c.setParam(0, 0.25f);  // margin 6 dB
    c.setParam(1, 0.5f);
    c.setParam(2, 1.0f);
    const size_t note = size_t(kRate) / 5, gap = size_t(kRate);
    // A tail of silence after the last note: the check below reads the output kDelay samples late.
    std::vector<float> x(3 * (note + gap) + gap, 0.0f);
    for (size_t k = 0; k < 3; ++k)
        for (size_t i = 0; i < note; ++i) x[k * (note + gap) + gap + i] = 0.5f * float(std::sin(2 * M_PI * 55.0 * double(i) / kRate));
    std::vector<float> l = x, r = x;
    for (size_t pos = 0; pos < l.size(); pos += 256) c.process(l.data() + pos, r.data() + pos, int(std::min<size_t>(256, l.size() - pos)));
    const int d = ps::DriveShaper<ps::Oversampler4x>::kDelay;
    for (size_t k = 0; k < 3; ++k) {
        const size_t on = k * (note + gap) + gap;
        // The first 5 ms (a quarter of the bass's cycle: its rise), and the whole note.
        float inFirst = 0.0f, outFirst = 0.0f, err = 0.0f;
        for (size_t i = on; i < on + size_t(kRate) / 200; ++i) {
            inFirst = std::max(inFirst, std::fabs(x[i]));
            outFirst = std::max(outFirst, std::fabs(l[i + size_t(d)]));
        }
        for (size_t i = on; i < on + note; ++i) err = std::max(err, std::fabs(l[i + size_t(d)] - x[i]));
        std::printf("  soft clipper after %s: first 5 ms %.3f of %.3f, largest change %.3f\n",
                    k == 0 ? "a start" : "a second of silence", double(outFirst), double(inFirst), double(err));
        CHECK(outFirst > 0.9f * inFirst);
        CHECK(err < 0.05f);  // a steady sine 3 dB over its RMS: under the 6 dB margin, untouched
    }
}

// The largest second difference of y in [from, to): a sine's is tiny, a jump or a kink shows.
static float largestBend(const std::vector<float>& y, size_t from, size_t to) {
    float m = 0.0f;
    for (size_t i = std::max<size_t>(from, 1); i + 1 < to && i + 1 < y.size(); ++i) m = std::max(m, std::fabs(y[i + 1] - 2.0f * y[i] + y[i - 1]));
    return m;
}

// Review m2: the Musique bus's duck goes from one depth to a low depth of its own (the split path,
// all-passed) and back to equal depths: no click either way (it faded in, and it stays split).
void testKeyedDuckerSplitWithoutClicks() {
    ps::KeyedDucker d;
    d.prepare(float(kRate));
    const size_t n = size_t(kRate) * 3;
    std::vector<float> l(n), r(n);
    for (size_t i = 0; i < n; ++i) {
        const double t = double(i) / kRate;
        l[i] = r[i] = float(0.5 * std::sin(2 * M_PI * 90.0 * t));
    }
    const auto in = l;
    const std::vector<float> key(240, 0.0f);  // no key: the gains stay at 1, only the path changes
    d.set(0.5f, 0.5f, 150.0f);
    for (size_t pos = 0; pos < n; pos += 240) {  // whole blocks up to each second
        if (pos == size_t(kRate)) d.set(0.5f, 0.9f, 150.0f);       // a low depth of its own: split
        if (pos == 2 * size_t(kRate)) d.set(0.5f, 0.5f, 150.0f);   // equal again
        d.process(l.data() + pos, r.data() + pos, key.data(), int(std::min<size_t>(240, n - pos)));
    }
    // A bend more than 20 times the sine's own (0.5 x (2 pi 90 / 48000)^2 = 7e-5) is a click.
    const float own = largestBend(in, 0, n);
    const float in1 = largestBend(l, size_t(kRate) - 512, size_t(kRate) + 4096);
    const float in2 = largestBend(l, 2 * size_t(kRate) - 512, 2 * size_t(kRate) + 4096);
    std::printf("  keyed duck: largest bend %.2g into the split, %.2g back to equal depths (the sine's own %.2g)\n",
                double(in1), double(in2), double(own));
    float apart = 0.0f;
    for (size_t i = size_t(kRate) + 4800; i < 2 * size_t(kRate); ++i) apart = std::max(apart, std::fabs(l[i] - in[i]));
    std::printf("  keyed duck: the split all-passes the sound (off the input by up to %.3f)\n", double(apart));
    CHECK(apart > 0.1f);
    CHECK(in1 < own * 20.0f);
    CHECK(in2 < own * 20.0f);
    CHECK(d.active());  // stays split
}

// Review m3: the send reverb's type changed while it rings (hall -> plate -> room -> hall): each
// change crossfades (~20 ms) instead of cutting one tank and starting the other. A steady 200 Hz
// tone in: the reverb's output is a steady tone too, so a cut shows as a sharp bend.
void testReverbTypeChangeCrossfades() {
    ps::PlateReverb plate;
    plate.prepare(float(kRate));
    ps::Reverb rv;
    rv.prepare(float(kRate));
    rv.attachPlate(&plate);
    rv.setParam(0, 0.5f);
    rv.setParam(3, 0.0f);  // no pre-delay
    const size_t n = size_t(kRate) * 8, block = 240;
    std::vector<float> in(n), outL(n, 0.0f), outR(n, 0.0f);
    for (size_t i = 0; i < n; ++i) in[i] = float(0.3 * std::sin(2 * M_PI * 200.0 * double(i) / kRate));
    const float types[] = {0.5f, 1.0f, 0.0f};  // plate, room, hall (the type setting: 0, 0.5, 1)
    for (size_t pos = 0; pos < n; pos += block) {
        if (pos > 0 && pos % (2 * size_t(kRate)) == 0) rv.setParam(4, types[pos / (2 * size_t(kRate)) - 1]);
        rv.process(in.data() + pos, in.data() + pos, outL.data() + pos, outR.data() + pos, int(block));
    }
    // The tone's own bend once the tail is steady, then around each change.
    const float own = largestBend(outL, size_t(kRate) + size_t(kRate) / 2, 2 * size_t(kRate) - 100);
    // Each change against the roughest of the two types' own steady tones (the plate's tank is
    // modulated: its tone bends more than the hall's).
    float worst = 0.0f, before = own;
    for (int k = 1; k <= 3; ++k) {
        const size_t at = size_t(k) * 2 * size_t(kRate);
        const float b = largestBend(outL, at - 100, at + size_t(kRate) / 20);
        const float after = largestBend(outL, at + size_t(kRate), at + 2 * size_t(kRate) - 100);
        std::printf("  type change %d: bend %.3g (the types' own %.3g, %.3g)\n", k, double(b), double(before), double(after));
        worst = std::max(worst, b / std::max(before, after));
        before = after;
    }
    float level = 0.0f;
    for (size_t i = n - size_t(kRate) / 2; i < n; ++i) level = std::max(level, std::fabs(outL[i]));
    std::printf("  reverb type changes: at worst %.2f times the types' own bend (level %.3f)\n", double(worst), double(level));
    CHECK(level > 0.01f);
    CHECK(worst < 2.0f);
}

// Review m3: a held synth note whose width goes back to 0 (wide -> mono), then whose oversampling is
// switched on and off (it starts the oscillators again): no step in the sound. A soft triangle,
// filtered, so that its own bends are small; the track's mono is the left channel on both sides.
void testSynthSwitchesWithoutClicks() {
    ps::Synth synth;
    synth.prepare(kRate);
    synth.setParam(ps::Synth::Osc1Wave, 1.0f);   // triangle
    synth.setParam(ps::Synth::OscMix, 0.0f);     // oscillator 1 only
    synth.setParam(ps::Synth::SubLevel, 0.0f);
    synth.setParam(ps::Synth::Cutoff, 0.45f);
    synth.setParam(ps::Synth::Resonance, 0.0f);
    synth.setParam(ps::Synth::FilterEnvAmount, 0.0f);
    synth.setParam(ps::Synth::AmpSustain, 1.0f);
    synth.setParam(ps::Synth::Unison, 0.34f);    // two copies
    synth.setParam(ps::Synth::Width, 0.8f);
    synth.noteOn(45, 0.9f, 0);
    const int block = 240, blocks = 4 * kRate / block;
    std::vector<float> l(size_t(blocks) * block, 0.0f), r(l.size(), 0.0f);
    for (int b = 0; b < blocks; ++b) {
        const size_t at = size_t(b) * block;
        if (at == size_t(kRate)) synth.setParam(ps::Synth::Width, 0.0f);
        // As an automation lane moves it: in the middle of a block.
        if (at == 2 * size_t(kRate)) synth.automate(ps::Synth::Oversample, 1.0f, 100);
        if (at == 3 * size_t(kRate)) synth.automate(ps::Synth::Oversample, 0.0f, 100);
        const bool wide = synth.stereo();
        synth.render(l.data() + at, r.data() + at, block);
        if (!wide) std::copy(l.begin() + long(at), l.begin() + long(at) + block, r.begin() + long(at));
    }
    float own = 0.0f;
    for (const auto* y : {&l, &r}) own = std::max(own, largestBend(*y, size_t(kRate) / 2, size_t(kRate) - 100));
    float worst = 0.0f;
    for (int k = 1; k <= 3; ++k) {
        const size_t at = size_t(k) * size_t(kRate);
        float b = 0.0f;
        for (const auto* y : {&l, &r}) b = std::max(b, largestBend(*y, at - 100, at + size_t(kRate) / 20));
        std::printf("  synth switch %d (%s): bend %.3g (steady %.3g)\n", k, k == 1 ? "wide to mono" : k == 2 ? "oversampling on" : "off", double(b), double(own));
        worst = std::max(worst, b);
    }
    CHECK(own > 0.0f);
    CHECK(worst < 3.0f * own);
    float sound = 0.0f;
    for (size_t i = l.size() - 4800; i < l.size(); ++i) sound = std::max(sound, std::fabs(l[i]));
    CHECK(sound > 0.05f);  // still playing after the switches
}

// The frames where a burst starts after at least 50 silent frames (a click per hit).
static std::vector<int> onsetsOf(const std::vector<float>& out, float threshold = 0.05f) {
    std::vector<int> at;
    int quiet = 1000;
    for (size_t i = 0; i < out.size() / 2; ++i) {
        const bool loud = std::fabs(out[2 * i]) > threshold;
        if (loud && quiet >= 50) at.push_back(int(i));
        quiet = loud ? 0 : quiet + 1;
    }
    return at;
}

// Review m4: two hits of one pad in the same rendered slice both sound, each on its frame: a late
// hit (+40 ms) followed by the next step's early one (-8.3 ms), 80 frames apart at 300 BPM; and a
// live tap with a sequenced hit later in the same slice.
void testTwoHitsOfAPadInOneSlice() {
    for (const bool sampled : {true, false}) {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetTempo, 300.0f});  // a step = 2400 frames
        const int pad = 5;
        if (sampled) {
            std::vector<float> click(32, 0.0f);
            click[0] = 1.0f;
            e.post({ps::CommandType::InstallSample, 0.0f, pad, 0, ps::Sample::create(click.data(), 32, 1, kRate, kRate)});
            e.post({ps::CommandType::SetPadParam, 0.0f, pad, ps::Sampler::Release});
        }
        e.post({ps::CommandType::SetDrumHit, 1.0f, pad, 0});
        e.post({ps::CommandType::SetDrumStepOffset, 40.0f, pad, 0});    // frame 1920
        e.post({ps::CommandType::SetDrumHit, 1.0f, pad, 1});
        e.post({ps::CommandType::SetDrumStepOffset, -8.3f, pad, 1});   // frame 2001
        e.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
        const auto out = render(e, 2400 + kLate + 400, 256);  // both in the slice 1792..2047
        CHECK(e.laneTriggerCount(pad) == 2);
        if (sampled) {
            const auto at = onsetsOf(out);
            std::printf("  two hits of a pad in one slice (sample): heard at");
            for (int a : at) std::printf(" %d", a - kLate);
            std::printf("\n");
            CHECK(at.size() == 2);
            if (at.size() == 2) {
                CHECK(std::abs(at[0] - kLate - 1920) <= 1);
                CHECK(std::abs(at[1] - kLate - 2001) <= 1);
            }
        } else {
            // The kit's voice (the snare's noise on pad 5): the second hit starts it again, so the
            // sound right after frame 2001 is a fresh attack, as loud as the first.
            float first = 0.0f, second = 0.0f;
            for (int i = 1920; i < 2000; ++i) first = std::max(first, std::fabs(out[size_t(i + kLate) * 2]));
            for (int i = 2002; i < 2082; ++i) second = std::max(second, std::fabs(out[size_t(i + kLate) * 2]));
            std::printf("  two hits of a pad in one slice (kit voice): %.3f then %.3f\n", double(first), double(second));
            CHECK(first > 0.05f);
            CHECK(second > 0.8f * first);
        }
    }
    // A live tap at the start of a slice and a sequenced hit later in it: both heard.
    ps::Engine e(kRate);
    e.post({ps::CommandType::SetTempo, 120.0f});
    std::vector<float> click(32, 0.0f);
    click[0] = 1.0f;
    e.post({ps::CommandType::InstallSample, 0.0f, 7, 0, ps::Sample::create(click.data(), 32, 1, kRate, kRate)});
    e.post({ps::CommandType::SetPadParam, 0.0f, 7, ps::Sampler::Release});
    e.post({ps::CommandType::SetDrumHit, 1.0f, 7, 1});
    e.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
    render(e, 6000 - 100, 100);
    e.post({ps::CommandType::TriggerPad, 1.0f, 7});  // 100 frames before step 1's hit
    const auto out = render(e, 400 + kLate, 256);
    const auto at = onsetsOf(out);
    std::printf("  live tap then a hit 100 frames later: %zu heard\n", at.size());
    CHECK(at.size() == 2);
}

// Review m5: a layered kit (16 pads x a base and 8 layers) replaced by another, then by a third,
// before the app collects: every sound let go reaches the retire queue (none leaked), and all are
// freed by the collection.
void testLayeredKitSwapsLeakNothing() {
    ps::Sampler sampler;
    sampler.prepare(kRate);
    std::vector<float> d(64, 0.1f);
    std::vector<ps::Sample*> all;
    for (int kit = 0; kit < 3; ++kit) {
        for (int pad = 0; pad < ps::Sampler::kSlots; ++pad) {
            ps::Sample* base = ps::Sample::create(d.data(), 64, 1, kRate, kRate);
            all.push_back(base);
            sampler.install(pad, base);
            for (int layer = 0; layer < ps::Sampler::kLayers; ++layer) {
                ps::Sample* s = ps::Sample::create(d.data(), 64, 1, kRate, kRate);
                all.push_back(s);
                sampler.installLayer(pad, layer, layer * 16, layer * 16 + 15, s);
            }
        }
    }
    std::printf("  layered kits swapped twice before a collection: %d sounds leaked\n", sampler.leaked());
    CHECK(sampler.leaked() == 0);
    sampler.collectGarbage();
}

// Eight audio tracks: 1..4 on their strips 12..15 as before, 5..8 on 32..35 after the pads', each
// with its live slot (15..18) and its automation (the channel's high bits at 24). All eight play
// at once with nothing allocated, within a small CPU budget.
static ps::Sample* audioClip(float level, int frames = kRate) {
    std::vector<float> d(size_t(frames) * 2);
    for (int i = 0; i < frames; ++i) {
        const float v = level * float(std::sin(2 * M_PI * 220.0 * i / kRate));
        d[size_t(2 * i)] = v;
        d[size_t(2 * i + 1)] = -v;
    }
    return ps::Sample::create(d.data(), frames, 2, kRate, kRate);
}

void testEightAudioTracks() {
    static_assert(ps::AudioTracks::kTracks == 8, "eight audio tracks");
    CHECK(ps::audioChannel(3) == 15 && ps::audioChannel(4) == 32 && ps::audioChannel(7) == 35);
    CHECK(ps::audioSlot(3) == 13 && ps::audioSlot(4) == 15 && ps::audioSlot(7) == 18 && ps::kSlots == 19);
    for (int k = 0; k < 8; ++k) CHECK(ps::audioOfChannel(ps::audioChannel(k)) == k && ps::audioOfSlot(ps::audioSlot(k)) == k);
    CHECK(!ps::Mixer::isPad(32) && ps::Mixer::isPad(31) && ps::audioOfChannel(16) == -1);
    // Every track heard on its own strip; none of them goes through the drums (the pads' route).
    {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetTempo, 120.0f});
        for (int k = 0; k < 8; ++k) {
            ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, k};
            c.sample = audioClip(0.05f * float(k + 1));
            e.post(c);
        }
        e.post({ps::CommandType::SetChannelMute, 1.0f, 0});  // the drums' strip
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto out = render(e, kRate / 2, 256);
        auto& m = e.mixer();
        std::printf("  eight audio tracks, strip peaks:");
        for (int k = 0; k < 8; ++k) {
            const float peak = m.takeChannelPeak(ps::audioChannel(k));
            std::printf(" %.3f", double(peak));
            CHECK(peak > 0.04f * float(k + 1) && peak < 0.06f * float(k + 1));
        }
        std::printf("\n");
        float outPeak = 0.0f;
        for (size_t i = out.size() / 2; i < out.size(); ++i) outPeak = std::max(outPeak, std::fabs(out[i]));
        CHECK(outPeak > 0.1f);
    }
    // Track 7's live slot: a stop launched on it at the next bar, the others play on.
    {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetTempo, 120.0f});
        for (int k : {1, 6}) {
            ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, k};
            c.sample = audioClip(0.3f);
            e.post(c);
        }
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        render(e, 6000 * 4, 256);
        e.post({ps::CommandType::ArmLaunch, -1.0f, 1 << ps::audioSlot(6), 16});
        render(e, 6000 * 12 + 9000, 256);  // past the bar line and the fade
        CHECK(e.slotTag(ps::audioSlot(6)) == -1);
        auto& m = e.mixer();
        m.takeChannelPeak(ps::audioChannel(6));
        m.takeChannelPeak(ps::audioChannel(1));
        render(e, 6000, 256);
        const float stopped = m.takeChannelPeak(ps::audioChannel(6)), playing = m.takeChannelPeak(ps::audioChannel(1));
        std::printf("  audio track 7's live slot stopped: %.4f (track 2 plays on: %.3f)\n", double(stopped), double(playing));
        CHECK(stopped < 1e-4f);
        CHECK(playing > 0.2f);
    }
    // A volume curve on track 8's strip (channel 35): down to 0 from step 4.
    {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetTempo, 120.0f});
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 7};
        c.sample = audioClip(0.3f);
        e.post(c);
        const int32_t target = ps::Automation::pack(ps::Automation::Volume, 35, 0, 0);
        CHECK(ps::Automation::channelOf(target) == 35 && ps::Automation::kindOf(target) == ps::Automation::Volume);
        e.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, target});
        for (int st = 0; st < 16; ++st) e.post({ps::CommandType::SetAutomationPoint, st < 4 ? 0.8f : 0.0f, 0, st});
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        auto& m = e.mixer();
        render(e, 6000 * 3, 256);
        const float before = m.takeChannelPeak(ps::audioChannel(7));
        render(e, 6000 * 2, 256);
        m.takeChannelPeak(ps::audioChannel(7));
        render(e, 6000 * 4, 256);
        const float after = m.takeChannelPeak(ps::audioChannel(7));
        std::printf("  volume curve on audio track 8: %.3f, then %.5f\n", double(before), double(after));
        CHECK(before > 0.2f);
        CHECK(after < 1e-3f);
    }
#ifdef PS_COUNT_ALLOCATIONS
    // All eight playing, launched and automated, with this chantier's other paths (two hits of a
    // pad in a slice, a 16-bar piano roll, the reverb's and the synth's switches): nothing allocated.
    {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetTempo, 300.0f});
        for (int k = 0; k < 8; ++k) {
            ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, k};
            c.sample = audioClip(0.05f, kRate / 4);
            e.post(c);
        }
        e.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Pan, 33, 0, 0)});
        for (int st = 0; st < 16; ++st) e.post({ps::CommandType::SetAutomationPoint, float(st) / 16.0f, 0, st});
        e.post({ps::CommandType::SetDrumHit, 1.0f, 5, 0});
        e.post({ps::CommandType::SetDrumStepOffset, 40.0f, 5, 0});
        e.post({ps::CommandType::SetDrumHit, 1.0f, 5, 1});
        e.post({ps::CommandType::SetDrumStepOffset, -8.3f, 5, 1});
        e.post({ps::CommandType::SetSynthSteps, 0.0f, 256});
        e.post({ps::CommandType::SetSynthNote, 0.9f, ps::NoteTrack::packIndex(0, 5760, 4800), ps::NoteTrack::pack(5760, 4800, 57)});
        e.post({ps::CommandType::SetSynthNote, 0.9f, 1, ps::NoteTrack::pack(0, 48, 64)});
        e.post({ps::CommandType::SetSynthNoteCount, 0.0f, 2});
        e.post({ps::CommandType::SynthParam, 0.8f, ps::Synth::Width});
        e.post({ps::CommandType::SetSend, 0.6f, 2, 0});
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        std::vector<float> out(512 * 2);
        for (int i = 0; i < 50; ++i) e.process(nullptr, 0, 0, out.data(), 512, 2);
        g_allocations = 0;
        g_countAllocations = true;
        for (int i = 0; i < 400; ++i) {
            if (i % 50 == 10) e.post({ps::CommandType::ArmLaunch, -1.0f, 1 << ps::audioSlot(i / 50 % 8), 16});
            if (i % 50 == 30) e.post({ps::CommandType::ArmLaunch, 0.0f, 1 << ps::audioSlot(i / 50 % 8), 16});
            if (i % 40 == 5) e.post({ps::CommandType::SetBusParam, float(i / 40 % 3) * 0.5f, 0, 4});  // reverb type
            if (i % 60 == 7) e.post({ps::CommandType::SynthParam, (i / 60) % 2 ? 0.0f : 0.8f, ps::Synth::Width});
            if (i % 70 == 9) e.post({ps::CommandType::SynthParam, (i / 70) % 2 ? 1.0f : 0.0f, ps::Synth::Oversample});
            if (i % 33 == 3) e.post({ps::CommandType::TriggerPad, 0.8f, 5});
            e.process(nullptr, 0, 0, out.data(), 512, 2);
        }
        g_countAllocations = false;
        std::printf("  allocations with eight audio tracks and the chantier's paths: %d\n", g_allocations.load());
        CHECK(g_allocations.load() == 0);
        CHECK(!e.hasNonFinite());
    }
#endif
    // CPU: eight stereo clips at once.
    {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetTempo, 120.0f});
        for (int k = 0; k < 8; ++k) {
            ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, k};
            c.sample = audioClip(0.05f, kRate * 4);
            e.post(c);
            e.post({ps::CommandType::SetAudioClipParam, 64.0f, k, ps::AudioTracks::LoopSteps});
        }
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        const int frames = kRate * 4;
        double load = 1e9;
        for (int pass = 0; pass < 3; ++pass) {
            const double start = threadSeconds();
            render(e, frames, 256);
            load = std::min(load, (threadSeconds() - start) / (static_cast<double>(frames) / kRate));
        }
        std::printf("  eight stereo audio tracks: %.2f %% of real time on this machine\n", load * 100.0);
        CHECK_TIMING(load < 0.05);
    }
}

}  // namespace restes_tests

#define PS_RESTES_TESTS \
    {"restes_mono_render", restes_tests::testMonoRenderIsTheMeanOfTheStereoOne}, \
    {"restes_sixteen_bar_loops", restes_tests::testSixteenBarLoops}, \
    {"restes_soft_clipper_after_silence", restes_tests::testSoftClipperAfterSilence}, \
    {"restes_keyed_duck_split", restes_tests::testKeyedDuckerSplitWithoutClicks}, \
    {"restes_reverb_type_crossfade", restes_tests::testReverbTypeChangeCrossfades}, \
    {"restes_synth_switches", restes_tests::testSynthSwitchesWithoutClicks}, \
    {"restes_two_hits_one_slice", restes_tests::testTwoHitsOfAPadInOneSlice}, \
    {"restes_layered_kit_swaps", restes_tests::testLayeredKitSwapsLeakNothing}, \
    {"restes_eight_audio_tracks", restes_tests::testEightAudioTracks},
