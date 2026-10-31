// Benjamin's list n° 17 (« le moteur audio doit être nickel »): the audit of what lots 1 to 4 added
// (live pads, cells, loops and the separated song following a live tempo change). Included by
// engine_tests.cpp.

namespace moteur17 {

// A stereo click train: a 2 ms burst every `spacing` frames from frame `first`, `frames` long.
static std::vector<float> clicks(int64_t frames, double spacing, double first = 0.0) {
    std::vector<float> d(size_t(frames) * 2, 0.0f);
    for (double at = first; at < double(frames); at += spacing) {
        const auto s = int64_t(std::llround(at));
        for (int64_t i = s; i < std::min<int64_t>(frames, s + 96); ++i) d[size_t(i) * 2] = d[size_t(i) * 2 + 1] = 0.4f;
    }
    return d;
}

// Onsets in the left channel: the first frame above 0.05 after 2000 frames under 0.01.
static std::vector<int64_t> onsets(const std::vector<float>& left) {
    std::vector<int64_t> out;
    int64_t quiet = 0;
    for (size_t i = 0; i < left.size(); ++i) {
        const float a = std::fabs(left[i]);
        if (a > 0.05f && quiet >= 2000) out.push_back(int64_t(i));
        quiet = a < 0.01f ? quiet + 1 : 0;
    }
    return out;
}

enum class Source { Song, AudioLoop, LivePad };

struct Phase {
    double worst = 0.0;  // frames between an onset after the swap and the nearest transport beat
    int counted = 0;     // onsets measured after the swap
};

// The tempo goes 120 -> `bpm` while one source of beat clicks plays, and its copy at the new tempo
// (the app stretches it on a worker thread: `settle` frames later) comes in. After the swap every
// click must land on the transport's beats again: the copy picks up at the musical place the
// transport is at, not at the place the old copy (played at the old tempo meanwhile) had reached.
static Phase phaseAfterTempoChange(Source src, double bpm, int settle, int64_t stemStart = 0) {
    ps::Engine engine(kRate);
    const int bar120 = 4 * 24000;
    const double beatNew = 60.0 / bpm * kRate;
    ps::StemBundle* songCopy = nullptr;
    ps::Sample* loopCopy = nullptr;
    if (src == Source::Song) {
        const int64_t len = int64_t(kRate) * 30;
        std::vector<std::vector<float>> stems(4, clicks(len, 24000.0));
        for (int s = 1; s < 4; ++s) std::fill(stems[size_t(s)].begin(), stems[size_t(s)].end(), 0.0f);
        auto* song = ps::StemBundle::fromFloat(stems);
        const int64_t lenNew = int64_t(std::llround(double(len) * 120.0 / bpm));
        std::vector<std::vector<float>> copy(4, clicks(lenNew, beatNew));
        for (int s = 1; s < 4; ++s) std::fill(copy[size_t(s)].begin(), copy[size_t(s)].end(), 0.0f);
        songCopy = ps::StemBundle::fromFloat(copy, song);
        ps::Command c{ps::CommandType::InstallStems};
        c.stems = song;
        engine.post(c);
        engine.post({ps::CommandType::SetStemStart, 0.0f, int(stemStart)});
    } else {
        const auto loop = clicks(bar120, 24000.0);
        const auto barNew = int64_t(std::llround(4 * beatNew));
        const auto copy = clicks(barNew, beatNew);
        loopCopy = ps::Sample::create(copy.data(), barNew, 2, kRate, kRate);
        if (src == Source::AudioLoop) {
            ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 0};
            c.sample = ps::Sample::create(loop.data(), bar120, 2, kRate, kRate);
            engine.post(c);
            engine.post({ps::CommandType::SetAudioClipParam, 0.0f, 0, ps::AudioTracks::Start});
            engine.post({ps::CommandType::SetAudioClipParam, 16.0f, 0, ps::AudioTracks::LoopSteps});
        } else {
            ps::Command c{ps::CommandType::InstallClip, 0.0f, 0};
            c.sample = ps::Sample::create(loop.data(), bar120, 2, kRate, kRate);
            engine.post(c);
            engine.post({ps::CommandType::SetClipParam, 4.0f, 0, ps::ClipLauncher::Beats});
        }
    }
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    if (src == Source::LivePad) engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    // The transport's own clock, driven block by block as the engine's (same tempo changes).
    ps::Sequencer clock;
    clock.prepare(kRate);
    clock.setTempo(120.0);
    clock.start(0);
    std::vector<int64_t> beats;
    const int block = 96;
    const int total = kRate * 12;
    const int changeAt = bar120 + 9600;  // a fifth into the second bar
    const int swapAt = changeAt + settle;
    std::vector<float> out(size_t(block) * 2), left;
    left.reserve(size_t(total));
    int64_t swappedAt = -1;
    for (int done = 0; done < total; done += block) {
        if (done <= changeAt && changeAt < done + block) {
            engine.post({ps::CommandType::SetTempo, float(bpm)});
            clock.setTempo(bpm);
        }
        if (swappedAt < 0 && done >= swapAt) {
            swappedAt = done;
            if (src == Source::Song) {
                ps::Command c{ps::CommandType::InstallStems};
                c.stems = songCopy;
                engine.post(c);
            } else {
                ps::Command c{src == Source::AudioLoop ? ps::CommandType::InstallAudioClip : ps::CommandType::InstallClip, 0.0f, 0 | ps::kRefit};
                c.sample = loopCopy;
                engine.post(c);
            }
        }
        clock.advance(block, [&](int offset, int step) {
            if (step % 4 == 0) beats.push_back(done + offset);
        });
        engine.process(nullptr, 0, 0, out.data(), block, 2);
        for (int i = 0; i < block; ++i) left.push_back(out[size_t(i) * 2]);
    }
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.process(nullptr, 0, 0, out.data(), block, 2);
    engine.collectGarbage();
    const auto heard = onsets(left);
    Phase p;
    if (heard.empty() || beats.empty()) return p;
    // The output's latency (the master's look-ahead): the first click is the transport's first beat
    // (a negative stem start: the song's first click lands on a later beat, the nearest one).
    int64_t late = kLate;
    for (int64_t at : heard) {
        if (at <= swappedAt + kRate / 50) continue;  // the 10 ms crossfade: the old copy still in it
        int64_t best = INT64_MAX;
        for (int64_t b : beats) best = std::min<int64_t>(best, std::llabs(at - (b + late)));
        p.worst = std::max(p.worst, double(best));
        ++p.counted;
    }
    return p;
}

// Live tempo change (n° 16, 17): the stretched song, an audio loop (a cell) and a live pad loop
// stay on the transport's beats once their copy at the new tempo is in, however long the copy took
// (the app waits 0.6 s for a drag to settle, then stretches the four stems: seconds on a phone).
void testCopiesKeepThePhaseAfterALiveTempoChange() {
    struct Case { const char* what; Source src; double bpm; int settle; int64_t start; };
    const Case cases[] = {
        {"song 120 -> 128, copy after 1.5 s", Source::Song, 128.0, kRate * 3 / 2, 0},
        {"song 120 -> 113, copy after 3 s", Source::Song, 113.0, kRate * 3, 0},
        {"song placed 4 bars later, 120 -> 128 in its lead-in", Source::Song, 128.0, kRate * 3 / 2, -4 * 96000},
        {"audio loop 120 -> 128, copy after 1 s", Source::AudioLoop, 128.0, kRate, 0},
        {"audio loop 120 -> 100, copy after 0.8 s", Source::AudioLoop, 100.0, kRate * 4 / 5, 0},
        {"live pad 120 -> 128, copy after 1 s", Source::LivePad, 128.0, kRate, 0},
    };
    for (const auto& c : cases) {
        const Phase p = phaseAfterTempoChange(c.src, c.bpm, c.settle, c.start);
        std::printf("  m17 %s: %d clicks after the swap, worst %.0f frames off the beat\n", c.what, p.counted, p.worst);
        CHECK(p.counted >= 4);
        CHECK(p.worst <= 2.0);
    }
}


// A steady tone (two partials), `frames` long: what a cut would show at once.
static std::vector<float> tone(int64_t frames, float amp = 0.3f) {
    std::vector<float> d(size_t(frames) * 2);
    for (int64_t i = 0; i < frames; ++i) {
        const float t = float(i) / float(kRate);
        const float v = amp * (0.7f * std::sin(2.0f * float(M_PI) * 220.0f * t) + 0.3f * std::sin(2.0f * float(M_PI) * 330.0f * t));
        d[size_t(i) * 2] = d[size_t(i) * 2 + 1] = v;
    }
    return d;
}

static float largestStep(const std::vector<float>& left) {
    float worst = 0.0f;
    for (size_t i = 1; i < left.size(); ++i) worst = std::max(worst, std::fabs(left[i] - left[i - 1]));
    return worst;
}

// Stops and cuts right after a loop started over (its last pass still fading out under the new
// one, a sound longer than its loop): nothing is cut dead, the fading pass included. The tone's
// own largest step is 0.0098 (two passes overlapping: 0.02); a fading pass cut dead: 0.23.
void testStopsRightAfterALoopStartsOverDoNotClick() {
    const int beat = 24000;
    const auto longTone = tone(beat + beat / 4);  // 1.25 beats on a 1-beat loop: the pass goes on
    float worst[4] = {};
    const char* what[4] = {"live pad switched off", "every live pad off", "live pad emptied", "transport stop (audio loop)"};
    for (int k = 0; k < 4; ++k) {
        std::vector<float> l, r;
        auto run = [&](auto&& render, int n) {
            std::vector<float> a(size_t(n), 0.0f), b(size_t(n), 0.0f);
            render(a.data(), b.data(), n);
            l.insert(l.end(), a.begin(), a.end());
        };
        if (k < 3) {
            ps::ClipLauncher clips;
            clips.prepare(kRate);
            clips.install(0, ps::Sample::create(longTone.data(), int64_t(longTone.size() / 2), 2, kRate, kRate));
            clips.setParam(0, ps::ClipLauncher::Beats, 1.0f);
            clips.toggle(0, true);
            auto render = [&](float* a, float* b, int n) { clips.render(a, b, n); };
            run(render, beat + 48);  // the second pass started 1 ms ago, the first fading under it
            if (k == 0) clips.toggle(0, true);
            else if (k == 1) clips.stopAll();
            else clips.install(0, nullptr);
            run(render, 2000);
            clips.collectGarbage();
        } else {
            ps::AudioTracks tracks;
            tracks.prepare(kRate);
            tracks.install(0, ps::Sample::create(longTone.data(), int64_t(longTone.size() / 2), 2, kRate, kRate));
            tracks.setParam(0, ps::AudioTracks::LoopSteps, 4.0f);
            std::vector<std::vector<float>> buf(ps::AudioTracks::kTracks * 2, std::vector<float>(size_t(beat), 0.0f));
            float* ls[ps::AudioTracks::kTracks];
            float* rs[ps::AudioTracks::kTracks];
            for (int t = 0; t < ps::AudioTracks::kTracks; ++t) { ls[t] = buf[size_t(2 * t)].data(); rs[t] = buf[size_t(2 * t + 1)].data(); }
            auto render = [&](float* a, float* b, int n) {
                for (auto& v : buf) std::fill(v.begin(), v.end(), 0.0f);
                tracks.render(ls, rs, n);
                for (int i = 0; i < n; ++i) { a[i] = ls[0][i]; b[i] = rs[0][i]; }
            };
            tracks.onStep(0, 0, -1);
            run(render, beat);
            tracks.onStep(0, 4, -1);
            run(render, 48);
            tracks.stop();
            run(render, 2000);
            tracks.collectGarbage();
        }
        worst[k] = largestStep(l);
        std::printf("  m17 %s 1 ms after the loop started over: largest step %.4f (the tone's own 0.0098)\n", what[k], double(worst[k]));
        CHECK(worst[k] < 0.03f);  // two passes overlap for 5 ms (0.0196): music, not a cut
    }
}

// The worst case of the new Live (n° 17): 16 live pad loops, 8 audio loops (cells), the separated
// song (4 stems) fitted to the tempo, a drum beat, four heavy effects on every strip used and on
// the master, both sends, the Live page's effects on; the tempo moves (120 -> 128) and every copy
// comes in, as the phone's stream plays it (96-frame callbacks). The audio thread's CPU per
// callback against its 2 ms budget (this thread's CPU time: other processes do not count), no
// allocation, nothing over full scale, no NaN.
struct Load { double mean, p99, p999, worst; };
static Load worstCaseLive(bool stripFx, bool masterFx, bool liveFx, bool print);

void testWorstCaseLiveCpuBench() {
    const Load all = worstCaseLive(true, true, true, true);
    // What costs what (the same run with a part left out), for the phone's measure.
    const Load noStrips = worstCaseLive(false, true, true, false);
    const Load noMaster = worstCaseLive(true, false, true, false);
    const Load noLive = worstCaseLive(true, true, false, false);
    const Load bare = worstCaseLive(false, false, false, false);
    std::printf("     mean without the strips' effects %.1f %%, without the master's %.1f %%, without the Live page's %.1f %%, with none %.1f %%\n",
                noStrips.mean * 100, noMaster.mean * 100, noLive.mean * 100, bare.mean * 100);
    std::printf("     the engine's own delay (the master's look-ahead): %d frames, %.2f ms, on top of the phone's buffer (2 bursts)\n",
                kLate, kLate * 1000.0 / kRate);
    // A laptop core; Benjamin's phone is about 3 times slower (ps_live --cpu-scale 3): a mean of 25 %
    // here is 75 % there; the p99 within the budget there too.
    CHECK_TIMING(all.mean < 0.25);
    CHECK_TIMING(all.p99 < 0.33);
}

static Load worstCaseLive(bool stripFx, bool masterFx, bool liveFx, bool print) {
    const int block = 96;
    const double budget = double(block) / kRate;
    auto chord = [&](int k, int64_t frames) {
        std::vector<float> d(size_t(frames) * 2);
        const float f = 80.0f + 23.0f * float(k);
        for (int64_t i = 0; i < frames; ++i) {
            const float t = float(i) / float(kRate);
            const float v = 0.02f * (std::sin(2.0f * float(M_PI) * f * t) + 0.5f * std::sin(2.0f * float(M_PI) * 1.5f * f * t));
            d[size_t(i) * 2] = d[size_t(i) * 2 + 1] = v;
        }
        return ps::Sample::create(d.data(), frames, 2, kRate, kRate);
    };
    const int bar120 = 96000, bar128 = 90000;
    std::vector<std::vector<float>> stems(4, std::vector<float>(size_t(kRate) * 30 * 2));
    for (int st = 0; st < 4; ++st)
        for (int i = 0; i < kRate * 30; ++i) stems[size_t(st)][size_t(i) * 2] = stems[size_t(st)][size_t(i) * 2 + 1] = 0.05f * std::sin(2.0f * float(M_PI) * (110.0f * float(st + 1)) * float(i) / float(kRate));
    auto* song = ps::StemBundle::fromFloat(stems);
    std::vector<std::vector<float>> stems128(4, std::vector<float>(size_t(kRate) * 30 * 120 / 128 * 2));
    for (int st = 0; st < 4; ++st)
        for (size_t i = 0; i < stems128[0].size() / 2; ++i) stems128[size_t(st)][i * 2] = stems128[size_t(st)][i * 2 + 1] = 0.05f * std::sin(2.0f * float(M_PI) * (110.0f * float(st + 1)) * float(i) / float(kRate));
    auto* song128 = ps::StemBundle::fromFloat(stems128, song);

    ps::Engine engine(kRate);
    engine.setCommandBudget(ps::Engine::kLiveCommandBudget);
    auto inserts = [&](int ch, std::initializer_list<int> types) {
        if (ch == ps::Mixer::kMaster ? !masterFx : !stripFx) return;
        int slot = 0;
        for (int t : types) engine.post({ps::CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts + slot++, t});
        engine.post({ps::CommandType::SetSend, 0.3f, ch, 0});
        engine.post({ps::CommandType::SetSend, 0.3f, ch, 1});
    };
    using IS = ps::InsertSlot;
    for (int t = 0; t < 8; ++t) {
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, t};
        c.sample = chord(t, bar120);
        engine.post(c);
        engine.post({ps::CommandType::SetAudioClipParam, 0.0f, t, ps::AudioTracks::Start});
        engine.post({ps::CommandType::SetAudioClipParam, 16.0f, t, ps::AudioTracks::LoopSteps});
        inserts(ps::audioChannel(t), {IS::OttFx, IS::Comp, IS::Saturation, IS::Eq});
    }
    for (int k = 0; k < 16; ++k) {
        ps::Command c{ps::CommandType::InstallClip, 0.0f, k};
        c.sample = chord(8 + k, bar120);
        engine.post(c);
        engine.post({ps::CommandType::SetClipParam, 4.0f, k, ps::ClipLauncher::Beats});
    }
    inserts(3, {IS::OttFx, IS::Comp, IS::Saturation, IS::Chorus, IS::EchoFx});  // the song and the live pads
    inserts(0, {IS::Comp, IS::Saturation, IS::Eq, IS::ReverbFx});              // the drums
    inserts(ps::Mixer::kMaster, {IS::OttFx, IS::Comp, IS::Saturation, IS::ClipperFx});
    engine.post({ps::CommandType::SetMaster, masterFx ? 1.0f : 0.0f, 2});
    for (int step = 0; step < 16; ++step) {
        if (step % 4 == 0) engine.post({ps::CommandType::SetDrumHit, 0.9f, 0, step});
        if (step % 8 == 4) engine.post({ps::CommandType::SetDrumHit, 0.8f, 1, step});
        if (step % 2 == 0) engine.post({ps::CommandType::SetDrumHit, 0.6f, 2, step});
    }
    ps::Command st{ps::CommandType::InstallStems};
    st.stems = song;
    engine.post(st);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    if (liveFx) {
        engine.post({ps::CommandType::SetPerformance, 0.3f, ps::PerformanceFx::Filter});
        engine.post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::Riser});
    }
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    for (int k = 0; k < 16; ++k) engine.post({ps::CommandType::ToggleClip, 0.0f, k});

    const int total = kRate * 20, changeAt = kRate * 8 + 4321, swapAt = changeAt + kRate;
    std::vector<float> out(size_t(block) * 2);
    std::vector<double> cpu;
    int allocations = 0;
    float peak = 0.0f;
    bool finite = true;
    for (int done = 0; done < total; done += block) {
        if (done <= changeAt && changeAt < done + block) engine.post({ps::CommandType::SetTempo, 128.0f});
        if (done <= swapAt && swapAt < done + block) {
            ps::Command s{ps::CommandType::InstallStems};
            s.stems = song128;
            engine.post(s);
            for (int t = 0; t < 8; ++t) {
                ps::Command a{ps::CommandType::InstallAudioClip, 0.0f, t | ps::kRefit};
                a.sample = chord(t, bar128);
                engine.post(a);
            }
            for (int k = 0; k < 16; ++k) {
                ps::Command b{ps::CommandType::InstallClip, 0.0f, k | ps::kRefit};
                b.sample = chord(8 + k, bar128);
                engine.post(b);
            }
        }
        const bool counted = done >= kRate;  // the setup commands are in
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
        for (float v : out) {
            finite = finite && std::isfinite(v);
            peak = std::max(peak, std::fabs(v));
        }
        if (done % (kRate * 2) < block) engine.collectGarbage();  // the app's own thread, now and then
    }
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.process(nullptr, 0, 0, out.data(), block, 2);
    engine.collectGarbage();
    std::sort(cpu.begin(), cpu.end());
    double mean = 0.0;
    for (double c : cpu) mean += c;
    mean /= double(cpu.size());
    const double p99 = cpu[cpu.size() * 99 / 100], p999 = cpu[cpu.size() * 999 / 1000], worst = cpu.back();
    if (print) std::printf("  m17 worst case (16 pads + 8 loops + song of 4 stems + drums + 4 effects per strip and on the master + sends + live effects, 120 -> 128):\n");
    if (print) std::printf("     audio thread CPU per 96-frame callback: mean %.1f %%, p99 %.1f %%, p99.9 %.1f %%, worst %.1f %% of %.1f ms; peak %.3f; %d allocations\n",
                mean * 100, p99 * 100, p999 * 100, worst * 100, budget * 1000, double(peak), allocations);
    CHECK(finite);
    CHECK(peak <= 1.0f);
#ifdef PS_COUNT_ALLOCATIONS
    CHECK(allocations == 0);
#endif
    return {mean, p99, p999, worst};
}

// Ten minutes of play (n° 17, timing): a live pad loop clicking on each beat and an audio loop (a
// cell) clicking half a beat later, at tempos whose step is not a whole number of frames. At the
// end, every click is still on the ideal grid (beat k at k x 60 / BPM x 48000, computed, not
// counted): no drift of the transport, the pads' beat clock or the loops.
void testTenMinutesDoNotDrift() {
    for (double bpm : {93.0, 174.0}) {
        const double beat = 60.0 / bpm * kRate;
        ps::Engine engine(kRate);
        const auto barFrames = int64_t(std::llround(4 * beat));
        const auto padLoop = clicks(barFrames, beat);
        const auto cellLoop = clicks(barFrames, beat, beat / 2);
        ps::Command pad{ps::CommandType::InstallClip, 0.0f, 0};
        pad.sample = ps::Sample::create(padLoop.data(), barFrames, 2, kRate, kRate);
        engine.post(pad);
        engine.post({ps::CommandType::SetClipParam, 4.0f, 0, ps::ClipLauncher::Beats});
        ps::Command cell{ps::CommandType::InstallAudioClip, 0.0f, 0};
        cell.sample = ps::Sample::create(cellLoop.data(), barFrames, 2, kRate, kRate);
        engine.post(cell);
        engine.post({ps::CommandType::SetAudioClipParam, 0.0f, 0, ps::AudioTracks::Start});
        engine.post({ps::CommandType::SetAudioClipParam, 16.0f, 0, ps::AudioTracks::LoopSteps});
        engine.post({ps::CommandType::SetTempo, float(bpm)});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
        const int block = 256;
        const int64_t total = int64_t(kRate) * 600, tail = int64_t(kRate) * 20;
        std::vector<float> out(size_t(block) * 2), head, end;
        int allocations = 0;
        for (int64_t done = 0; done < total; done += block) {
            g_allocations = 0;
            g_countAllocations = true;
            engine.process(nullptr, 0, 0, out.data(), block, 2);
            g_countAllocations = false;
            allocations += g_allocations.load();
            for (int i = 0; i < block; ++i) {
                if (done + i < tail) head.push_back(out[size_t(i) * 2]);
                else if (done + i >= total - tail) end.push_back(out[size_t(i) * 2]);
            }
        }
        engine.collectGarbage();
        auto offGrid = [&](const std::vector<float>& left, int64_t from, int& pads, int& cells) {
            double worst = 0.0;
            for (int64_t at : onsets(left)) {
                const double t = double(from + at - kLate) / (beat / 2);  // in half beats
                const double k = std::round(t);
                worst = std::max(worst, std::fabs(t - k) * beat / 2);
                (int64_t(k) % 2 == 0 ? pads : cells)++;
            }
            return worst;
        };
        int pads0 = 0, cells0 = 0, pads1 = 0, cells1 = 0;
        const double first = offGrid(head, 0, pads0, cells0);
        const double last = offGrid(end, total - tail, pads1, cells1);
        std::printf("  m17 10 min at %.0f BPM: first 20 s %d pad + %d cell clicks, worst %.2f frames off the grid; last 20 s %d + %d, worst %.2f frames; %d allocations\n",
                    bpm, pads0, cells0, first, pads1, cells1, last, allocations);
        CHECK(pads1 >= 20 && cells1 >= 20);
        CHECK(first <= 1.0);
        CHECK(last <= 1.0);
#ifdef PS_COUNT_ALLOCATIONS
        CHECK(allocations == 0);
#endif
    }
}

// The song fading under a new copy (10 ms) is still there when something else makes it fade again:
// the transport started over, or another copy, a few milliseconds later. Neither cuts the first
// fading copy dead.
void testSongCrossfadesInARowDoNotClick() {
    for (int second = 0; second < 2; ++second) {
        ps::StemPlayer player;
        player.prepare(kRate);
        const int64_t len = int64_t(kRate) * 4;
        auto toneStems = [&](float hz) {
            std::vector<std::vector<float>> st(4, std::vector<float>(size_t(len) * 2, 0.0f));
            for (int64_t i = 0; i < len; ++i) st[0][size_t(i) * 2] = st[0][size_t(i) * 2 + 1] = 0.3f * std::sin(2.0f * float(M_PI) * hz * float(i) / float(kRate));
            return st;
        };
        auto* a = ps::StemBundle::fromFloat(toneStems(220.0f));
        player.install(a);
        player.start(0);
        std::vector<float> l, buf(static_cast<size_t>(kRate)), bufR(static_cast<size_t>(kRate));
        auto run = [&](int n) {
            std::fill(buf.begin(), buf.begin() + n, 0.0f);
            std::fill(bufR.begin(), bufR.begin() + n, 0.0f);
            player.render(buf.data(), bufR.data(), n);
            l.insert(l.end(), buf.begin(), buf.begin() + n);
        };
        run(24000);
        player.install(ps::StemBundle::fromFloat(toneStems(220.0f), a));  // its copy: crossfades
        run(144);                                                         // 3 ms into the crossfade
        if (second == 0) player.start(0);  // ▶ again: from the start
        else player.install(ps::StemBundle::fromFloat(toneStems(220.0f), a));
        run(4800);
        const float worst = largestStep(l);
        std::printf("  m17 song copy then %s 3 ms later: largest step %.4f (the tone's own 0.0086)\n", second == 0 ? "a restart" : "another copy", double(worst));
        CHECK(worst < 0.03f);
        player.install(nullptr);
        run(960);
        player.collectGarbage();
    }
}

}  // namespace moteur17

#define PS_MOTEUR17_TESTS \
    {"m17_copies_keep_the_phase_after_a_live_tempo_change", moteur17::testCopiesKeepThePhaseAfterALiveTempoChange}, \
    {"m17_stops_right_after_a_loop_starts_over_do_not_click", moteur17::testStopsRightAfterALoopStartsOverDoNotClick}, \
    {"m17_worst_case_live_cpu_bench", moteur17::testWorstCaseLiveCpuBench}, \
    {"m17_ten_minutes_do_not_drift", moteur17::testTenMinutesDoNotDrift}, \
    {"m17_song_crossfades_in_a_row_do_not_click", moteur17::testSongCrossfadesInARowDoNotClick},
