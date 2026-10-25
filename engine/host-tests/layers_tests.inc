// G1b (« une piste = une pile de couches », docs/PLAN-GUIDE.md § 3.4 and 4.1): up to three more sounds
// on an instrument track, playing its notes (note range, transposition, fine tune), each with its
// own strip (gain, pan, inserts, sends) summed into the track's strip. Included by engine_tests.cpp
// (kept apart to ease merging).

namespace layers_tests {

using ps::CommandType;

void post(ps::Engine& e, CommandType type, float value, int32_t a = 0, int32_t b = 0) { e.post({type, value, a, b}); }

int32_t layerArg(int track, int layer, int param) { return param | (layer << 8) | (track << 16); }

// Track `track` on, its own sound the 808 (a clean sine: its pitch can be read), and layer
// `layer` the 808 too, on, `transpose` semitones away.
void trackWith808Layer(ps::Engine& e, int track, int layer, int transpose) {
    post(e, CommandType::SetTrackOn, 1.0f, track);
    post(e, CommandType::SetSynthEngine, 0.0f, (track << 16) | ps::InstrumentTrack::Bass808Source);
    post(e, CommandType::SetSynthEngine, 0.0f, (ps::layerSound(track, layer) << 16) | ps::InstrumentTrack::Bass808Source);
    post(e, CommandType::SetLayer, float(transpose), layerArg(track, layer, ps::InstrumentTrack::LayerTranspose));
    post(e, CommandType::SetLayer, 1.0f, layerArg(track, layer, ps::InstrumentTrack::LayerOn));
}

std::vector<float> leftOf(const std::vector<float>& stereo, size_t from = 0) {
    std::vector<float> l;
    for (size_t i = from * 2; i < stereo.size(); i += 2) l.push_back(stereo[i]);
    return l;
}

double rmsOf(const std::vector<float>& x, size_t from, size_t to) {
    double s = 0.0;
    to = std::min(to, x.size());
    if (to <= from) return 0.0;
    for (size_t i = from; i < to; ++i) s += double(x[i]) * x[i];
    return std::sqrt(s / double(to - from));
}

// The largest jump from one frame to the next (a click stands out against a low tone's).
float worstStep(const std::vector<float>& x, size_t from, size_t to) {
    float w = 0.0f;
    for (size_t i = std::max<size_t>(from, 1); i < std::min(to, x.size()); ++i) w = std::max(w, std::fabs(x[i] - x[i - 1]));
    return w;
}

// The layer plays the track's notes, transposed; the track's listening hears one or the other;
// the sound commands reach the layer's sound by its index, not the track's.
void testLayerPlaysTheTracksNotes() {
    auto run = [](int listen, int note) {
        auto e = std::make_unique<ps::Engine>(kRate);
        trackWith808Layer(*e, 1, 0, 12);
        post(*e, CommandType::SetLayerListen, float(listen), 1);
        post(*e, CommandType::SynthNoteOn, 0.9f, (1 << 16) | note);
        auto out = render(*e, kRate / 2, 256);
        CHECK(e->sound(ps::layerSound(1, 0)).source == ps::InstrumentTrack::Bass808Source);
        CHECK(e->sound(1).source == ps::InstrumentTrack::Bass808Source);
        CHECK(e->sound(ps::layerSound(1, 1)).source == ps::InstrumentTrack::SynthSource);  // the other layers untouched
        return leftOf(out, size_t(kLate + 2400));
    };
    const auto own = run(1, 45), layer = run(2, 45), both = run(15, 45);
    const double ownHz = toneHz(own, 0, own.size()), layerHz = toneHz(layer, 0, layer.size());
    std::printf("  own sound %.1f Hz, layer (+12) %.1f Hz\n", ownHz, layerHz);
    CHECK(std::fabs(ownHz / 110.0 - 1.0) < 0.05);
    CHECK(std::fabs(layerHz / 220.0 - 1.0) < 0.05);
    CHECK(rmsOf(both, 0, both.size()) > 1.2 * rmsOf(own, 0, own.size()));
    // Nothing heard: silence (and every sound back at once).
    const auto none = run(0, 45);
    CHECK(rmsOf(none, 2400, none.size()) < 1e-4);
}

// A layer with the track's own sound and no transposition, heard alone, is the track's own sound
// heard alone, sample for sample: the same note events on the same frames (piano roll, swing).
void testLayerIsSampleAccurate() {
    auto run = [](int listen, int block) {
        auto e = std::make_unique<ps::Engine>(kRate);
        post(*e, CommandType::SetTempo, 131.0f);
        post(*e, CommandType::SetSwing, 0.27f);
        trackWith808Layer(*e, 3, 2, 0);
        post(*e, CommandType::SetSynthSteps, 0.0f, (3 << 16) | 16);
        int idx = 0;
        for (int st = 0; st < 16; st += 3) post(*e, CommandType::SetSynthNote, 0.8f, (3 << 16) | idx++, ps::NoteTrack::pack(st * 24 + 5, 20, 36 + st));
        post(*e, CommandType::SetSynthNoteCount, 0.0f, (3 << 16) | idx);
        post(*e, CommandType::SetLayerListen, float(listen), 3);
        render(*e, kRate / 5, block);  // the sounds not listened to glide out (15 ms) before the first note
        post(*e, CommandType::SetTransport, 1.0f, 0, 0);
        return render(*e, kRate * 3, block);
    };
    const auto own = run(1, 256);
    const auto layer = run(1 << 3, 256);
    const auto layerSmall = run(1 << 3, 96);  // the phone's callbacks
    float worst = 0.0f, worstBlocks = 0.0f;
    double level = 0.0;
    for (size_t i = 0; i < own.size(); ++i) {
        worst = std::max(worst, std::fabs(own[i] - layer[i]));
        worstBlocks = std::max(worstBlocks, std::fabs(layer[i] - layerSmall[i]));
        level = std::max(level, double(std::fabs(own[i])));
    }
    std::printf("  layer vs own sound: max diff %.2g (peak %.2f); 96- vs 256-frame blocks: %.2g\n", double(worst), level, double(worstBlocks));
    CHECK(level > 0.1);
    CHECK(worst < 1e-4f);
    CHECK(worstBlocks < 1e-5f);
}

// The note range keeps a layer to some notes (the sub under, the mid above); fine tune and the
// track's bend move it; a note-off reaches the layer whatever its range became meanwhile.
void testNoteRangeAndTuning() {
    auto e = std::make_unique<ps::Engine>(kRate);
    trackWith808Layer(*e, 2, 1, 0);
    post(*e, CommandType::SetLayer, 60.0f, layerArg(2, 1, ps::InstrumentTrack::LayerLow));
    post(*e, CommandType::SetLayerListen, float(1 << 2), 2);  // the layer alone
    render(*e, kRate / 5, 256);
    post(*e, CommandType::SynthNoteOn, 0.9f, (2 << 16) | 45);  // under its range
    auto below = leftOf(render(*e, kRate / 4, 256));
    CHECK(rmsOf(below, 0, below.size()) < 1e-4);
    post(*e, CommandType::SynthNoteOff, 0.0f, (2 << 16) | 45);
    post(*e, CommandType::SynthNoteOn, 0.9f, (2 << 16) | 64);  // in it
    auto in = leftOf(render(*e, kRate / 4, 256));
    CHECK(rmsOf(in, 2400, in.size()) > 0.02);
    CHECK(std::fabs(toneHz(in, 4800, in.size()) / multizone_tests::midiHz(64) - 1.0) < 0.05);
    // The range moves away while the note holds: its note-off still reaches it (no hanging note).
    post(*e, CommandType::SetLayer, 100.0f, layerArg(2, 1, ps::InstrumentTrack::LayerLow));
    post(*e, CommandType::SynthNoteOff, 0.0f, (2 << 16) | 64);
    render(*e, kRate * 3, 256);
    auto after = leftOf(render(*e, kRate / 4, 256));
    CHECK(rmsOf(after, 0, after.size()) < 1e-4);
    // Fine tune: +0.5 semitone.
    auto f = std::make_unique<ps::Engine>(kRate);
    trackWith808Layer(*f, 2, 1, 0);
    post(*f, CommandType::SetLayer, 0.5f, layerArg(2, 1, ps::InstrumentTrack::LayerFine));
    post(*f, CommandType::SetLayerListen, float(1 << 2), 2);
    post(*f, CommandType::SynthNoteOn, 0.9f, (2 << 16) | 57);
    auto tuned = leftOf(render(*f, kRate / 2, 256), size_t(kLate + 4800));
    const double hz = toneHz(tuned, 0, tuned.size());
    std::printf("  fine +50 cents on A3: %.1f Hz (%.1f expected)\n", hz, multizone_tests::midiHz(57.5));
    CHECK(std::fabs(hz / multizone_tests::midiHz(57.5) - 1.0) < 0.012);
}

// Layer switched on, muted, unmuted, heard alone and switched off while a note holds: never a
// click (each change glides), and a layer switched off stops being rendered once its release is over.
void testLayerChangesAreClickFree() {
    auto e = std::make_unique<ps::Engine>(kRate);
    post(*e, CommandType::SetTrackOn, 1.0f, 4);
    // Long sines, both at 55 Hz, so that the largest step between frames is small.
    for (int s : {4, ps::layerSound(4, 0)}) {
        post(*e, CommandType::SetSynthEngine, 0.0f, (s << 16) | ps::InstrumentTrack::Bass808Source);
        post(*e, CommandType::SetTrack808Param, 2.5f, (s << 16) | ps::Track808::Decay);
    }
    post(*e, CommandType::SetLayer, 1.0f, layerArg(4, 0, ps::InstrumentTrack::LayerOn));
    post(*e, CommandType::SynthNoteOn, 0.9f, (4 << 16) | 33);
    auto steady = leftOf(render(*e, kRate / 2, 192));
    const float plain = worstStep(steady, size_t(kLate + 960), steady.size());
    std::vector<float> all;
    auto go = [&](CommandType t, float v, int32_t a, int32_t b = 0) {
        post(*e, t, v, a, b);
        auto part = leftOf(render(*e, kRate / 10, 192));
        all.insert(all.end(), part.begin(), part.end());
    };
    const int ch = ps::Mixer::layerChannel(4, 0);
    go(CommandType::SetChannelMute, 1.0f, ch);
    go(CommandType::SetChannelMute, 0.0f, ch);
    go(CommandType::SetLayerListen, 2.0f, 4);  // the layer alone
    go(CommandType::SetLayerListen, 1.0f, 4);  // the track's own sound alone
    go(CommandType::SetLayerListen, 15.0f, 4);
    go(CommandType::SetLayer, 0.25f, layerArg(4, 0, ps::LayerGain));
    go(CommandType::SetLayer, 0.0f, layerArg(4, 0, ps::InstrumentTrack::LayerOn));
    const float changes = worstStep(all, 0, all.size());
    std::printf("  largest step: %.4f steady, %.4f through the changes\n", double(plain), double(changes));
    CHECK(plain > 0.0f);
    CHECK(changes < 1.6f * plain);
    // Off: still rendered while its release rings, then not at all.
    CHECK(e->layerRendered(4, 0));
    render(*e, kRate * 5, 192);
    CHECK(!e->layerRendered(4, 0));
    // Its own notes released with it (the 808's voice silent).
    std::vector<float> l(256, 0.0f), r(256, 0.0f);
    e->sound(ps::layerSound(4, 0)).render(l.data(), r.data(), 256);
    CHECK(rmsOf(l, 0, l.size()) < 1e-6);
}

// The track's mute and solo, and its fader, take its layers along (a stem of the track has them);
// a layer through = 0 passes the track's inserts by (a clean sub under a driven bass).
void testLayersFollowTheirTrack() {
    auto run = [](auto setup) {
        auto e = std::make_unique<ps::Engine>(kRate);
        trackWith808Layer(*e, 1, 0, 0);
        post(*e, CommandType::SetLayerListen, 2.0f, 1);  // the layer alone
        setup(*e);
        post(*e, CommandType::SynthNoteOn, 0.9f, (1 << 16) | 45);
        auto out = leftOf(render(*e, kRate / 2, 256), size_t(kLate + 4800));
        return rmsOf(out, 0, out.size());
    };
    const double plain = run([](ps::Engine&) {});
    const double muted = run([](ps::Engine& e) { post(e, CommandType::SetChannelMute, 1.0f, ps::trackChannel(1)); });
    const double otherSolo = run([](ps::Engine& e) { post(e, CommandType::SetChannelSolo, 1.0f, ps::trackChannel(2)); });
    const double ownSolo = run([](ps::Engine& e) { post(e, CommandType::SetChannelSolo, 1.0f, ps::trackChannel(1)); });
    const double faded = run([](ps::Engine& e) { post(e, CommandType::SetChannelVolume, 0.4f, ps::trackChannel(1)); });
    const double layerSolo = run([](ps::Engine& e) { post(e, CommandType::SetChannelSolo, 1.0f, ps::Mixer::layerChannel(1, 0)); });
    // A high-pass far above the 110 Hz tone on the track's strip: through it, the layer is gone; past it, untouched.
    auto highPass = [](ps::Engine& e) {
        const int slot = ps::trackChannel(1) * ps::Mixer::kInserts;
        post(e, CommandType::SetInsertType, 0.0f, slot, ps::InsertSlot::Filter);
        post(e, CommandType::SetInsertParam, 0.95f, slot, 0);
        post(e, CommandType::SetInsertParam, 0.1f, slot, 1);
        post(e, CommandType::SetInsertParam, 1.0f, slot, 2);
        post(e, CommandType::SetInsertParam, 1.0f, slot, 3);
    };
    const double through = run(highPass);
    const double past = run([&](ps::Engine& e) {
        highPass(e);
        post(e, CommandType::SetLayer, 0.0f, layerArg(1, 0, ps::LayerThrough));
    });
    std::printf("  layer rms %.4f; track muted %.5f, other solo %.5f, own solo %.4f, fader 0.4 %.4f; high-pass through %.5f, past %.4f\n",
                plain, muted, otherSolo, ownSolo, faded, through, past);
    CHECK(plain > 0.05);
    CHECK(muted < 1e-4);
    CHECK(otherSolo < 1e-4);
    CHECK(std::fabs(ownSolo / plain - 1.0) < 0.01);
    CHECK(std::fabs(faded / plain - 0.25) < 0.02);   // (0.4 / 0.8)^2
    CHECK(std::fabs(layerSolo / plain - 1.0) < 0.01);  // a layer has no solo of its own (its track's listening)
    CHECK(through < 0.2 * plain);
    CHECK(std::fabs(past / plain - 1.0) < 0.01);
}

// An export renders the layers as the live engine plays them (the same commands, the same sound).
void testExportRendersTheLayers() {
    auto setup = [](auto&& postTo) {
        postTo({CommandType::SetTempo, 124.0f, 0, 0});
        postTo({CommandType::SetTrackOn, 1.0f, 5, 0});
        postTo({CommandType::SetSynthEngine, 0.0f, (5 << 16) | ps::InstrumentTrack::Bass808Source, 0});
        postTo({CommandType::SetSynthEngine, 0.0f, (ps::layerSound(5, 1) << 16) | ps::InstrumentTrack::PlaitsSource, 0});
        postTo({CommandType::SetLayer, 1.0f, layerArg(5, 1, ps::InstrumentTrack::LayerOn), 0});
        postTo({CommandType::SetLayer, 12.0f, layerArg(5, 1, ps::InstrumentTrack::LayerTranspose), 0});
        postTo({CommandType::SetChannelPan, -0.6f, ps::Mixer::layerChannel(5, 1), 0});
        postTo({CommandType::SetInsertType, 0.0f, ps::Mixer::layerChannel(5, 1) * ps::Mixer::kInserts, ps::InsertSlot::Saturation});
        postTo({CommandType::SetSynthSteps, 0.0f, (5 << 16) | 16, 0});
        for (int k = 0; k < 4; ++k) postTo({CommandType::SetSynthNote, 0.8f, (5 << 16) | k, ps::NoteTrack::pack(k * 4 * 24, 60, 40 + k * 3)});
        postTo({CommandType::SetSynthNoteCount, 0.0f, (5 << 16) | 4, 0});
    };
    const int64_t frames = kRate * 2;
    ps::OfflineRenderer r(kRate);
    setup([&](ps::Command c) { r.post(c); });
    const auto offline = r.renderToBuffer(frames, 0);
    // Live, prepared as an export prepares its engine: 100 ms to settle, the faders at their place, then ▶.
    auto e = std::make_unique<ps::Engine>(kRate);
    setup([&](ps::Command c) { e->post(c); });
    render(*e, kRate / 10, ps::OfflineRenderer::kBlock);
    e->mixer().settle();
    e->post({CommandType::SetTransport, 1.0f, 0, 0});
    const auto live = render(*e, int(frames) + kLate, ps::OfflineRenderer::kBlock);
    // Without the layer: the export is quieter on the left (the layer is panned there).
    ps::OfflineRenderer plainR(kRate);
    setup([&](ps::Command c) { plainR.post(c); });
    plainR.post({CommandType::SetLayer, 0.0f, layerArg(5, 1, ps::InstrumentTrack::LayerOn), 0});
    const auto plain = plainR.renderToBuffer(frames, 0);
    CHECK(offline.size() >= size_t(frames) * 2);
    float worst = 0.0f;
    double level = 0.0, left = 0.0, leftPlain = 0.0;
    for (size_t i = 0; i < size_t(frames) * 2; ++i) {
        worst = std::max(worst, std::fabs(offline[i] - live[i + size_t(kLate) * 2]));
        level = std::max(level, double(std::fabs(offline[i])));
        if (i % 2 == 0) {
            left += double(offline[i]) * offline[i];
            leftPlain += double(plain[i]) * plain[i];
        }
    }
    std::printf("  export vs live: max diff %.2g (peak %.2f); left channel with / without the layer %+.1f dB\n", double(worst), level,
                10.0 * std::log10(left / std::max(1e-12, leftPlain)));
    CHECK(level > 0.05);
    CHECK(worst < 1e-6f);  // the same sound, sample for sample
    CHECK(left > 1.5 * leftPlain);
}

// Every layer of every track playing, every kind of sound: process() never allocates.
void testLayersAllocateNothing() {
#ifdef PS_COUNT_ALLOCATIONS
    std::vector<float> wave(4800);
    for (size_t i = 0; i < wave.size(); ++i) wave[i] = 0.3f * std::sin(0.05f * float(i));
    {
        auto e = std::make_unique<ps::Engine>(kRate);
        const int sources[] = {ps::InstrumentTrack::SynthSource, ps::InstrumentTrack::PlaitsSource, ps::InstrumentTrack::SampledSource,
                               ps::InstrumentTrack::MultiSource, ps::InstrumentTrack::Bass808Source};
        for (int t = 0; t < ps::kTracks; ++t) {
            if (t > 0) post(*e, CommandType::SetTrackOn, 1.0f, t);
            for (int k = 0; k < ps::kLayers; ++k) {
                const int s = ps::layerSound(t, k);
                const int source = sources[(t + k) % 5];
                post(*e, CommandType::SetSynthEngine, 0.0f, (s << 16) | source);
                if (source == ps::InstrumentTrack::SampledSource) {
                    ps::Command keys{CommandType::InstallTrackSample, 0.0f, s << 16};
                    keys.sample = ps::Sample::create(wave.data(), 4800, 1, kRate, kRate);
                    e->post(keys);
                } else if (source == ps::InstrumentTrack::MultiSource) {
                    ps::Command zone{CommandType::InstallTrackSample, 0.0f, (s << 16) | 1};
                    zone.sample = ps::Sample::createCompact(wave.data(), 4800, 1, kRate, kRate);
                    e->post(zone);
                    post(*e, CommandType::SetTrackZone, 127.0f, ps::MultiSampler::HiKey | (s << 16), 0);
                    post(*e, CommandType::SetTrackZone, 1.0f, ps::MultiSampler::Count | (s << 16), -1);
                }
                post(*e, CommandType::SetLayer, 1.0f, layerArg(t, k, ps::InstrumentTrack::LayerOn));
                post(*e, CommandType::SetLayer, float(k * 7 - 7), layerArg(t, k, ps::InstrumentTrack::LayerTranspose));
                post(*e, CommandType::SetInsertType, 0.0f, ps::Mixer::layerChannel(t, k) * ps::Mixer::kInserts, ps::InsertSlot::Comp);
                post(*e, CommandType::SetSend, 0.3f, ps::Mixer::layerChannel(t, k), k % 2);
            }
            for (int n = 0; n < 16; ++n) post(*e, CommandType::SetSynthNote, 0.8f, (t << 16) | n, ps::NoteTrack::pack(n * 24, 30, 40 + n % 12 + t));
            post(*e, CommandType::SetSynthNoteCount, 0.0f, (t << 16) | 16);
        }
        post(*e, CommandType::SetTransport, 1.0f, 0, 0);
        render(*e, kRate, 256);  // warm-up: every command applied, every voice started once
        std::vector<float> out(256 * 2);
        g_allocations = 0;
        g_countAllocations = true;
        for (int b = 0; b < kRate * 2 / 256; ++b) {
            if (b == 40) post(*e, CommandType::SetLayerListen, 3.0f, 2);
            if (b == 80) post(*e, CommandType::SetLayer, 0.0f, layerArg(3, 1, ps::InstrumentTrack::LayerOn));
            if (b == 120) post(*e, CommandType::SetLayer, 1.0f, layerArg(3, 1, ps::InstrumentTrack::LayerOn));
            if (b == 160) post(*e, CommandType::SetLayer, 0.0f, layerArg(6, 2, ps::LayerThrough));
            if (b == 200) post(*e, CommandType::SetChannelMute, 1.0f, ps::Mixer::layerChannel(7, 0));
            e->process(nullptr, 0, 0, out.data(), 256, 2);
        }
        g_countAllocations = false;
        CHECK(g_allocations == 0);
        CHECK(!e->hasNonFinite());
    }
#endif
}

// The bass music sub as a layer of its bass (Layers.kt subAsLayer) sounds as the sub on a track of
// its own did: the bass's own notes on track 1 (a saw, high-passed on its strip), the sub an octave
// down with its pump, either on track 2 or as track 1's layer past its inserts. The same commands
// the app sends for each form; rendered offline, the two mixes are the same to 1e-4.
void testSubLayerSoundsAsTheSubTrack() {
    auto bass = [](auto&& postTo) {
        postTo({CommandType::SetTempo, 172.0f, 0, 0});
        postTo({CommandType::SetTrackOn, 1.0f, 1, 0});
        postTo({CommandType::SetSynthSteps, 0.0f, (1 << 16) | 16, 0});
        const int slot = ps::trackChannel(1) * ps::Mixer::kInserts;
        postTo({CommandType::SetInsertType, 0.0f, slot, ps::InsertSlot::Filter});
        for (int k = 0; k < 4; ++k) postTo({CommandType::SetInsertParam, std::array{0.18f, 0.1f, 1.0f, 1.0f}[size_t(k)], slot, k});
        postTo({CommandType::SetInsertType, 0.0f, slot + 1, ps::InsertSlot::Saturation});
        for (int st = 0; st < 16; st += 4) postTo({CommandType::SetDrumHit, 1.0f, 0, st});  // the kick the pump follows
    };
    // The notes of track `t`, `shift` semitones away.
    auto notes = [](auto&& postTo, int t, int shift) {
        postTo({CommandType::SetSynthSteps, 0.0f, (t << 16) | 16, 0});
        for (int k = 0; k < 6; ++k) postTo({CommandType::SetSynthNote, 0.8f, (t << 16) | k, ps::NoteTrack::pack(k * 2 * 24 + 6, 40, 45 + (k % 3) * 2 + shift)});
        postTo({CommandType::SetSynthNoteCount, 0.0f, (t << 16) | 6, 0});
    };
    auto subSound = [](auto&& postTo, int s, int ch) {
        postTo({CommandType::SetSynthEngine, 0.0f, (s << 16) | ps::InstrumentTrack::Bass808Source, 0});
        postTo({CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts, ps::InsertSlot::Pump});
    };
    const int64_t frames = kRate * 3;
    ps::OfflineRenderer two(kRate), one(kRate);
    {
        auto postTo = [&](ps::Command c) { two.post(c); };
        bass(postTo);
        notes(postTo, 1, 0);
        postTo({CommandType::SetTrackOn, 1.0f, 2, 0});
        subSound(postTo, 2, ps::trackChannel(2));
        notes(postTo, 2, -12);
    }
    {
        auto postTo = [&](ps::Command c) { one.post(c); };
        bass(postTo);
        notes(postTo, 1, 0);
        subSound(postTo, ps::layerSound(1, 0), ps::Mixer::layerChannel(1, 0));
        postTo({CommandType::SetLayer, -12.0f, layerArg(1, 0, ps::InstrumentTrack::LayerTranspose), 0});
        postTo({CommandType::SetLayer, 0.0f, layerArg(1, 0, ps::LayerThrough), 0});
        postTo({CommandType::SetLayer, 1.0f, layerArg(1, 0, ps::InstrumentTrack::LayerOn), 0});
    }
    const auto a = two.renderToBuffer(frames, 0), b = one.renderToBuffer(frames, 0);
    CHECK(a.size() == b.size());
    float worst = 0.0f, peak = 0.0f;
    for (size_t i = 0; i < std::min(a.size(), b.size()); ++i) {
        worst = std::max(worst, std::fabs(a[i] - b[i]));
        peak = std::max(peak, std::fabs(a[i]));
    }
    std::printf("  sub on its track vs as the bass's layer: max diff %.2g (peak %.2f)\n", double(worst), double(peak));
    CHECK(peak > 0.1f);
    CHECK(worst < 1e-4f);
}

// A transposition past the keyboard's ends drops the notes it would push off it (never two keys
// on one layer note, whose note-off would cut the other).
void testTranspositionPastTheEnds() {
    auto e = std::make_unique<ps::Engine>(kRate);
    trackWith808Layer(*e, 1, 0, 40);
    post(*e, CommandType::SetLayerListen, 2.0f, 1);
    render(*e, kRate / 5, 256);
    post(*e, CommandType::SynthNoteOn, 0.9f, (1 << 16) | 100);  // 140: off the keyboard
    auto off = leftOf(render(*e, kRate / 4, 256));
    CHECK(rmsOf(off, 0, off.size()) < 1e-5);
    post(*e, CommandType::SynthNoteOn, 0.9f, (1 << 16) | 40);   // 80: plays
    auto on = leftOf(render(*e, kRate / 4, 256));
    CHECK(rmsOf(on, 2400, on.size()) > 0.02);
    post(*e, CommandType::SynthNoteOff, 0.0f, (1 << 16) | 100);  // the note dropped: nothing cut
    auto still = leftOf(render(*e, kRate / 8, 256));
    CHECK(rmsOf(still, 0, still.size()) > 0.02);
}

// The worst case: 8 tracks of 4 sounds each (their own and three layers), the heaviest sources
// (the synth with seven unison copies, oversampled, wide and driven; Plaits), chords of 6 notes.
// A layer's synth plays kLayerVoices notes at most: layers cost what their voices cost, nothing
// more; a track's usual layers (a sub, an attack, some air: the 808, Plaits, a sample) fit beside
// it. Measured against the same tracks without layers, alternately (A B A B, the best of three
// passes each): ratios, whatever the machine's speed.
void testLayersCpuBudget() {
    constexpr int kBlock = 192;
    // `layer`: the layers' source (-1: none; 99: two 808 subs and a Plaits layer).
    auto make = [](int layer) {
        auto e = std::make_unique<ps::Engine>(kRate);
        for (int t = 0; t < ps::kTracks; ++t) {
            if (t > 0) post(*e, CommandType::SetTrackOn, 1.0f, t);
            for (int k = -1; k < ps::kLayers; ++k) {
                if (k >= 0 && layer < 0) break;
                const int s = k < 0 ? t : ps::layerSound(t, k);
                const int source = k < 0 ? ps::InstrumentTrack::SynthSource : (layer == 99 ? std::array{4, 1, 4}[size_t(k)] : layer);
                post(*e, CommandType::SetSynthEngine, 0.0f, (s << 16) | source);
                if (source == ps::InstrumentTrack::SynthSource) {
                    for (auto [param, v] : {std::pair{ps::Synth::Unison, 1.0f}, {ps::Synth::Oversample, 1.0f}, {ps::Synth::Width, 0.8f},
                                            {ps::Synth::Drive, 0.5f}, {ps::Synth::OctaveDown, 0.5f}, {ps::Synth::AmpRelease, 0.6f}}) {
                        post(*e, CommandType::SynthParam, v, (s << 16) | param);
                    }
                }
                if (k >= 0) post(*e, CommandType::SetLayer, 1.0f, layerArg(t, k, ps::InstrumentTrack::LayerOn));
            }
            // Six-note chords, one every beat (each note held three quarters of it).
            int idx = 0;
            for (int beat = 0; beat < 4; ++beat) {
                for (int v = 0; v < 6; ++v) post(*e, CommandType::SetSynthNote, 0.8f, (t << 16) | idx++, ps::NoteTrack::pack(beat * 96, 72, 48 + v * 4 + t));
            }
            post(*e, CommandType::SetSynthNoteCount, 0.0f, (t << 16) | idx);
        }
        post(*e, CommandType::SetTransport, 1.0f, 0, 0);
        std::vector<float> out(kBlock * 2);
        for (int b = 0; b < 200; ++b) e->process(nullptr, 0, 0, out.data(), kBlock, 2);  // warm-up
        return e;
    };
    const int kinds[] = {-1, ps::InstrumentTrack::SynthSource, ps::InstrumentTrack::PlaitsSource, ps::InstrumentTrack::Bass808Source, 99};
    std::vector<std::unique_ptr<ps::Engine>> engines;
    for (int k : kinds) engines.push_back(make(k));
    CHECK(engines[1]->sound(ps::layerSound(3, 1)).synth.activeVoices() <= ps::kLayerVoices);
    CHECK(engines[1]->sound(3).synth.activeVoices() == 6);
    std::vector<double> best(engines.size(), 1e9);
    std::vector<float> out(kBlock * 2);
    for (int pass = 0; pass < 3; ++pass) {
        for (size_t i = 0; i < engines.size(); ++i) {
            const double start = threadSeconds();
            for (int b = 0; b < kRate / kBlock; ++b) engines[i]->process(nullptr, 0, 0, out.data(), kBlock, 2);
            best[i] = std::min(best[i], threadSeconds() - start);
        }
    }
    const double plain = best[0], synth = best[1] / plain, plaits = best[2] / plain, sub808 = best[3] / plain, usual = best[4] / plain;
    std::printf("  8 tracks of the heaviest synth (6-note chords): %.1f %% of real time; with 3 layers each (32 sounds), times that: "
                "heaviest synth x%.2f, Plaits x%.2f, 808 x%.2f, 808 + Plaits + 808 x%.2f\n",
                plain * 100.0, synth, plaits, sub808, usual);
#if defined(__SANITIZE_ADDRESS__)
    (void)synth;
    (void)plaits;
    (void)sub808;
    (void)usual;
#else
    // The heaviest synth on every layer: its voices' cost (6 own + 3 x 6) and 24 strips more, no more
    // (past a phone's budget: the app's CPU warning says so; a layer is meant for a sub, an attack, some air).
    CHECK_TIMING(synth < (1.0 + 3.0 * ps::kLayerVoices / 6.0) * 1.45);
    // The usual layers: a small share beside their track (the 808 and Plaits are light).
    // (Measured on a loaded 4-core host: 808 x1.24..1.28, mixed x1.32..1.35, Plaits x1.80..2.04, synth
    // x4.4..4.9: bounds with some room.)
    CHECK_TIMING(sub808 < 1.4);
    CHECK_TIMING(usual < 1.6);
    CHECK_TIMING(plaits < 2.4);
#endif
}

}  // namespace layers_tests

#define PS_LAYERS_TESTS                                                              \
    {"g1b_layer_plays_the_tracks_notes", layers_tests::testLayerPlaysTheTracksNotes},  \
    {"g1b_layer_is_sample_accurate", layers_tests::testLayerIsSampleAccurate},         \
    {"g1b_note_range_and_tuning", layers_tests::testNoteRangeAndTuning},               \
    {"g1b_layer_changes_are_click_free", layers_tests::testLayerChangesAreClickFree},  \
    {"g1b_layers_follow_their_track", layers_tests::testLayersFollowTheirTrack},       \
    {"g1b_export_renders_the_layers", layers_tests::testExportRendersTheLayers},       \
    {"g1b_layers_allocate_nothing", layers_tests::testLayersAllocateNothing},          \
    {"g1b_layers_cpu_budget", layers_tests::testLayersCpuBudget},                      \
    {"g1b_sub_layer_sounds_as_the_sub_track", layers_tests::testSubLayerSoundsAsTheSubTrack}, \
    {"g1b_transposition_past_the_ends", layers_tests::testTranspositionPastTheEnds},
