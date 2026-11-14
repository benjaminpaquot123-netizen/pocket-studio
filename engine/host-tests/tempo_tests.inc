// Step 4e (Benjamin's list n° 16): the tempo changed live while loops play. The app stretches each
// loop off the audio thread and hands the engine the new copy flagged kRefit; a loop that sounds
// picks up at the same place of its pass in the new copy (faded in over 5 ms, the old one fading
// under it) instead of going silent until its next pass. Included by engine_tests.cpp.

namespace tempo4e {

// A pitch-kept "stretch" of a sine loop: the same tone, `frames` long (what Signalsmith gives a
// steady tone).
static ps::Sample* sineLoop(int frames, float hz, float amp) {
    std::vector<float> d(size_t(frames) * 2);
    for (int i = 0; i < frames; ++i) d[size_t(i) * 2] = d[size_t(i) * 2 + 1] = amp * std::sin(2.0f * float(M_PI) * hz * float(i) / float(kRate));
    return ps::Sample::create(d.data(), frames, 2, kRate, kRate);
}

struct Heard {
    float worstWindowRms = 1e9f;  // the quietest 10 ms after the loops came in
    float largestStep = 0.0f;     // frame to frame, left channel
    int allocations = 0;
    bool finite = true;
};

// An audio track loop (1 bar) and a live pad loop (4 beats) at 120 BPM; the tempo goes to `bpm`
// mid-bar and, `refit` or not, the two loops get their copies at that tempo 150 ms later.
static Heard tempoChange(double bpm, bool refit) {
    ps::Engine engine(kRate);
    const int bar120 = 4 * 24000;
    const int barNew = int(std::lround(4 * 60.0 / bpm * kRate));
    ps::Command clip{ps::CommandType::InstallAudioClip, 0.0f, 0};
    clip.sample = sineLoop(bar120, 110.0f, 0.25f);
    engine.post(clip);
    engine.post({ps::CommandType::SetAudioClipParam, 0.0f, 0, ps::AudioTracks::Start});
    engine.post({ps::CommandType::SetAudioClipParam, 16.0f, 0, ps::AudioTracks::LoopSteps});
    engine.post({ps::CommandType::SetAudioClipParam, 480.0f, 0, ps::AudioTracks::FadeOut});
    ps::Command pad{ps::CommandType::InstallClip, 0.0f, 0};
    pad.sample = sineLoop(bar120, 165.0f, 0.25f);
    engine.post(pad);
    engine.post({ps::CommandType::SetClipParam, 4.0f, 0, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    // The copies at the new tempo, made before (the app makes them on a worker thread).
    ps::Sample* track = sineLoop(barNew, 110.0f, 0.25f);
    ps::Sample* live = sineLoop(barNew, 165.0f, 0.25f);
    const int block = 96;
    std::vector<float> out(size_t(block) * 2);
    Heard h;
    float prev = 0.0f;
    std::vector<float> window;
    const int total = 3 * bar120;
    const int changeAt = bar120 + bar120 / 3;          // a third into the second bar
    const int swapAt = changeAt + kRate * 150 / 1000;  // the copies ready 150 ms later
    bool swapped = false;
    for (int done = 0; done < total; done += block) {
        if (done >= changeAt && done < changeAt + block) engine.post({ps::CommandType::SetTempo, float(bpm)});
        if (!swapped && done >= swapAt) {
            swapped = true;
            ps::Command a{ps::CommandType::InstallAudioClip, 0.0f, refit ? (0 | ps::kRefit) : 0};
            a.sample = track;
            engine.post(a);
            ps::Command b{ps::CommandType::InstallClip, 0.0f, refit ? (0 | ps::kRefit) : 0};
            b.sample = live;
            engine.post(b);
        }
        const bool counted = done >= bar120 / 2;
        if (counted) {
            g_allocations = 0;
            g_countAllocations = true;
        }
        engine.process(nullptr, 0, 0, out.data(), block, 2);
        if (counted) {
            g_countAllocations = false;
            h.allocations += g_allocations.load();
        }
        for (int i = 0; i < block; ++i) {
            const float l = out[size_t(i) * 2];
            if (!std::isfinite(l)) h.finite = false;
            if (done + i > 4800) h.largestStep = std::max(h.largestStep, std::fabs(l - prev));
            prev = l;
            if (done + i >= bar120 / 2) {  // both loops in (the live pad waits for its grid line)
                window.push_back(l);
                if (window.size() == 480) {
                    double e = 0.0;
                    for (float v : window) e += double(v) * v;
                    h.worstWindowRms = std::min(h.worstWindowRms, float(std::sqrt(e / 480.0)));
                    window.clear();
                }
            }
        }
    }
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.process(nullptr, 0, 0, out.data(), block, 2);
    engine.collectGarbage();
    return h;
}

// Faster (120 -> 132) and slower (120 -> 100): the loops keep sounding through the swap (no gap),
// no step bigger than the tones' own (no click), nothing allocated in the callback, no NaN.
// Without kRefit the same swap leaves a gap (the test sees the difference).
void testLiveTempoChangeRefitsLoops() {
    for (double bpm : {132.0, 100.0}) {
        const Heard on = tempoChange(bpm, true);
        const Heard off = tempoChange(bpm, false);
        std::printf("  4e tempo 120 -> %.0f: quietest 10 ms %.3f (without refit %.3f), largest step %.4f, %d allocations\n",
                    bpm, double(on.worstWindowRms), double(off.worstWindowRms), double(on.largestStep), on.allocations);
        CHECK(on.finite);
        CHECK(on.worstWindowRms > 0.04f);  // never silent: the dropout is gone
        CHECK(off.worstWindowRms < on.worstWindowRms * 0.6f);
        // Two tones of 0.25 through the mix: their own largest step is about 0.01; a cut is far more.
        CHECK(on.largestStep < 0.03f);
#ifdef PS_COUNT_ALLOCATIONS
        CHECK(on.allocations == 0);
#endif
    }
}

// The pick-up itself: a loop refitted mid-pass carries on at the same place of its pass (the new
// copy's frame at the same fraction), faded in; an empty slot or a one-shot gets no pick-up.
void testRefitPicksUpAtTheSamePlace() {
    std::vector<float> ramp(10000), ramp2(12500);
    for (size_t i = 0; i < ramp.size(); ++i) ramp[i] = float(i) / 10000.0f;
    for (size_t i = 0; i < ramp2.size(); ++i) ramp2[i] = float(i) / 12500.0f;
    ps::AudioTracks tracks;
    tracks.prepare(kRate);
    tracks.install(0, ps::Sample::create(ramp.data(), 10000, 1, kRate, kRate));
    tracks.setParam(0, ps::AudioTracks::LoopSteps, 16.0f);
    tracks.setParam(0, ps::AudioTracks::FadeOut, 0.0f);
    tracks.onStep(0, 0, -1);
    std::vector<std::vector<float>> buf(ps::AudioTracks::kTracks * 2, std::vector<float>(4000, 0.0f));
    float* ls[ps::AudioTracks::kTracks];
    float* rs[ps::AudioTracks::kTracks];
    for (int k = 0; k < ps::AudioTracks::kTracks; ++k) { ls[k] = buf[size_t(2 * k)].data(); rs[k] = buf[size_t(2 * k + 1)].data(); }
    tracks.render(ls, rs, 4000);  // 40 % into the pass
    tracks.install(0, ps::Sample::create(ramp2.data(), 12500, 1, kRate, kRate), true);
    std::fill(buf[0].begin(), buf[0].end(), 0.0f);
    tracks.render(ls, rs, 1000);
    std::printf("  4e refit: %.4f just after the fade, %.4f expected\n", double(buf[0][500]), 0.4 + 500.0 / 12500.0);
    CHECK(std::fabs(buf[0][500] - (0.4f + 500.0f / 12500.0f)) < 0.002f);
    CHECK(tracks.playing(0));
    tracks.install(0, nullptr);
    tracks.render(ls, rs, 1000);
    tracks.collectGarbage();
}


// The live bench of step 4e (n° 16 and n° 17): a project with 8 audio track loops, 16 live pad
// loops and a separated song stretched to the project's tempo, played as the phone plays it
// (96-frame callbacks). The tempo moves twice while everything plays (120 -> 128 -> 113): the
// song's copy at each tempo and each loop's copy (Signalsmith, made beforehand as the app's worker
// thread makes them) come in as the app hands them over. Prints the audio thread's CPU per
// callback against its budget, the largest frame-to-frame step at the swaps against the music's
// own, the quietest 50 ms while playing, the allocations in the callback.
void testLiveTempoBench() {
    const int block = 96;
    const double budget = double(block) / kRate;
    // Loops: chords of three partials (a steady, rich sound: a cut would show), 1 bar at 120.
    const int bar120 = 4 * 24000;
    auto loop = [&](int k) {
        std::vector<float> d(size_t(bar120) * 2);
        const float f = 80.0f + 23.0f * float(k);
        for (int i = 0; i < bar120; ++i) {
            const float t = float(i) / float(kRate);
            const float v = 0.02f * (std::sin(2.0f * float(M_PI) * f * t) + 0.5f * std::sin(2.0f * float(M_PI) * 1.5f * f * t) + 0.3f * std::sin(2.0f * float(M_PI) * 2.0f * f * t));
            d[size_t(i) * 2] = d[size_t(i) * 2 + 1] = v;
        }
        return d;
    };
    std::vector<std::vector<float>> loops;
    for (int k = 0; k < 24; ++k) loops.push_back(loop(k));
    auto fitted = [&](int k, double bpm) {
        const auto frames = int64_t(std::llround(bar120 * 120.0 / bpm));
        const auto out = ps::timeStretch(loops[size_t(k)].data(), bar120, 2, frames, 0.0f, kRate);
        return ps::Sample::create(out.data(), frames, 2, kRate, kRate);
    };
    // The song: 30 s, four stems of tones, then its copies at 128 and 113 (ratio song / project).
    std::vector<std::vector<float>> stems(4, std::vector<float>(size_t(kRate) * 30 * 2));
    for (int st = 0; st < 4; ++st)
        for (int i = 0; i < kRate * 30; ++i) stems[size_t(st)][size_t(i) * 2] = stems[size_t(st)][size_t(i) * 2 + 1] = 0.05f * std::sin(2.0f * float(M_PI) * (110.0f * float(st + 1)) * float(i) / float(kRate));
    auto* song = ps::StemBundle::fromFloat(stems);
    const double prep0 = threadSeconds();
    auto* song128 = song->stretched(120.0 / 128.0, kRate);
    auto* song113 = song->stretched(120.0 / 113.0, kRate);
    std::vector<ps::Sample*> at128, at113;
    for (int k = 0; k < 24; ++k) {
        at128.push_back(fitted(k, 128.0));
        at113.push_back(fitted(k, 113.0));
    }
    const double prep = threadSeconds() - prep0;
    CHECK(song128 && song113);
    if (!song128 || !song113) return;

    ps::Engine engine(kRate);
    for (int t = 0; t < 8; ++t) {
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, t};
        c.sample = ps::Sample::create(loops[size_t(t)].data(), bar120, 2, kRate, kRate);
        engine.post(c);
        engine.post({ps::CommandType::SetAudioClipParam, 0.0f, t, ps::AudioTracks::Start});
        engine.post({ps::CommandType::SetAudioClipParam, 16.0f, t, ps::AudioTracks::LoopSteps});
        engine.post({ps::CommandType::SetAudioClipParam, 480.0f, t, ps::AudioTracks::FadeOut});
    }
    for (int k = 0; k < 16; ++k) {
        ps::Command c{ps::CommandType::InstallClip, 0.0f, k};
        c.sample = ps::Sample::create(loops[size_t(8 + k)].data(), bar120, 2, kRate, kRate);
        engine.post(c);
        engine.post({ps::CommandType::SetClipParam, 4.0f, k, ps::ClipLauncher::Beats});
    }
    ps::Command st{ps::CommandType::InstallStems};
    st.stems = song;
    engine.post(st);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    for (int k = 0; k < 16; ++k) engine.post({ps::CommandType::ToggleClip, 0.0f, k});

    const int total = kRate * 20;
    struct Change { int at; float bpm; ps::StemBundle* song; std::vector<ps::Sample*>* loops; };
    // The tempo moves; 250 ms later (the app's settle) the copies come in.
    const Change changes[] = {{kRate * 5 + 7777, 128.0f, song128, &at128}, {kRate * 12 + 3333, 113.0f, song113, &at113}};
    const int settle = kRate / 4;
    std::vector<float> out(size_t(block) * 2);
    std::vector<double> cpu;
    float prev = 0.0f, musicStep = 0.0f, swapStep = 0.0f, quietest = 1e9f;
    double e = 0.0;
    int n = 0, allocations = 0;
    bool finite = true;
    std::vector<int> swaps;
    for (int done = 0; done < total; done += block) {
        for (const auto& c : changes) {
            if (done <= c.at && c.at < done + block) engine.post({ps::CommandType::SetTempo, c.bpm});
            if (done <= c.at + settle && c.at + settle < done + block) {
                ps::Command s{ps::CommandType::InstallStems};
                s.stems = c.song;
                c.song->retain();
                engine.post(s);
                for (int t = 0; t < 8; ++t) {
                    ps::Command a{ps::CommandType::InstallAudioClip, 0.0f, t | ps::kRefit};
                    a.sample = (*c.loops)[size_t(t)];
                    a.sample->retain();
                    engine.post(a);
                }
                for (int k = 0; k < 16; ++k) {
                    ps::Command b{ps::CommandType::InstallClip, 0.0f, k | ps::kRefit};
                    b.sample = (*c.loops)[size_t(8 + k)];
                    b.sample->retain();
                    engine.post(b);
                }
                swaps.push_back(done);
            }
        }
        const bool counted = done >= kRate;
        if (counted) {
            g_allocations = 0;
            g_countAllocations = true;
        }
        const double t0 = threadSeconds();
        engine.process(nullptr, 0, 0, out.data(), block, 2);
        const double spent = threadSeconds() - t0;
        if (counted) {
            g_countAllocations = false;
            allocations += g_allocations.load();
            cpu.push_back(spent / budget);
        }
        for (int i = 0; i < block; ++i) {
            const float l = out[size_t(i) * 2];
            if (!std::isfinite(l)) finite = false;
            const float step = std::fabs(l - prev);
            prev = l;
            if (done + i < kRate) continue;  // everything in (the live pads start on their grid line)
            bool nearSwap = false;
            for (int s : swaps) nearSwap = nearSwap || (done + i >= s && done + i < s + kRate / 50);
            for (const auto& c : changes) nearSwap = nearSwap || (done + i >= c.at && done + i < c.at + kRate / 50);
            (nearSwap ? swapStep : musicStep) = std::max(nearSwap ? swapStep : musicStep, step);
            e += double(l) * l;
            if (++n == kRate / 20) {
                quietest = std::min(quietest, float(std::sqrt(e / n)));
                e = 0.0;
                n = 0;
            }
        }
    }
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.process(nullptr, 0, 0, out.data(), block, 2);
    std::sort(cpu.begin(), cpu.end());
    double mean = 0.0;
    for (double c : cpu) mean += c;
    mean /= double(cpu.size());
    const double p99 = cpu[cpu.size() * 99 / 100], p999 = cpu[cpu.size() * 999 / 1000], worst = cpu.back();
    std::printf("  4e live bench (8 audio loops + 16 live pad loops + a song of 4 stems, 96-frame callbacks, tempo 120 -> 128 -> 113):\n");
    std::printf("     audio thread CPU per callback: mean %.1f %%, p99 %.1f %%, p99.9 %.1f %%, worst %.1f %% of its %.2f ms\n",
                mean * 100, p99 * 100, p999 * 100, worst * 100, budget * 1000);
    std::printf("     largest step at the tempo changes and swaps %.4f, the music's own %.4f; quietest 50 ms %.3f; %d allocations\n",
                double(swapStep), double(musicStep), double(quietest), allocations);
    std::printf("     copies made off the audio thread beforehand: %.2f s of CPU (2 songs of 30 s, 48 loops)\n", prep);
    CHECK(finite);
    CHECK(swapStep <= musicStep * 1.5f + 1e-4f);  // no click: nothing sharper than the music itself
    CHECK(quietest > 0.05f);                        // no dropout
#ifdef PS_COUNT_ALLOCATIONS
    CHECK(allocations == 0);
#endif
    if (!machineOverloaded()) CHECK(p99 < 0.5);  // a laptop: the phone is slower (ps_live --cpu-scale 3)
    engine.collectGarbage();
    for (auto* s : at128) if (s->release()) delete s;
    for (auto* s : at113) if (s->release()) delete s;
    if (song128->release()) delete song128;
    if (song113->release()) delete song113;
    engine.collectGarbage();
}

}  // namespace tempo4e

#define PS_TEMPO4E_TESTS                                                                \
    {"tempo4e_live_tempo_change_refits_loops", tempo4e::testLiveTempoChangeRefitsLoops}, \
    {"tempo4e_refit_picks_up_at_the_same_place", tempo4e::testRefitPicksUpAtTheSamePlace},      \
    {"tempo4e_live_tempo_bench", tempo4e::testLiveTempoBench},
