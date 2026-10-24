// Live playback keeps its level (Benjamin's report on the preview: « la musique se coupe
// progressivement après 15 s environ »). An assistant song played live with its loudness target in
// Drop mode: the drive's estimate read what the clipper and the limiter took off from two loud
// parts remembered over minutes, so once a loud part had been heard with a high drive the loss
// never came down with the drive: the drive fell 2 dB/s without end (-77 dB a minute later) and
// the song faded out. Exports hold a measured drive and never showed it.
// Included by engine_tests.cpp (kept apart to ease merging).

namespace livefade_tests {

// A song as the app plays it live: each part staged while the one before plays (SetStaging, then
// ArmSceneSwap with the song position of the next part), the whole mix's song curve a point per
// beat (the assistant's silent beat before each Drop, a Break 3 dB down), the loudness target in
// Drop mode (SetMixVersion 2) or in Song mode (projects saved before S5).
enum Kind { Intro, Drop, Break };
struct Part {
    Kind kind;
    int bars;
};

struct Case {
    const char* name;
    double bpm;
    int beats;      // a bar of `beats` quarter notes (SetBeatsPerBar)
    int seconds;    // played live
    bool dropMode;  // SetMixVersion 2 (the assistant's songs) or 1 (Song mode)
    bool inSong;    // the song (parts swapped in with their song position), or one scene looped alone that changes
};

float faderValue(double db) { return static_cast<float>(0.8 * std::pow(10.0, db / 40.0)); }  // Mixer::setVolume(kMaster): (v / 0.8)^2

void postPart(ps::Engine& e, Kind kind, int stepsPerBar) {
    using ps::CommandType;
    e.post({CommandType::SetPatternSteps, 0.0f, stepsPerBar});
    e.post({CommandType::SetAcidLength, 0.0f, stepsPerBar});
    for (int st = 0; st < stepsPerBar; ++st) {
        const bool beat = st % 4 == 0, offbeat = st % 4 == 2;
        const float kick = kind == Drop && beat ? 1.0f : 0.0f;
        const float hat = kind == Break ? 0.0f : offbeat ? (kind == Intro ? 0.25f : 0.8f) : 0.0f;
        const float clap = kind == Drop && st % 8 == 4 ? 0.9f : 0.0f;
        e.post({CommandType::SetDrumHit, kick, ps::DrumKit::Kick, st});
        e.post({CommandType::SetDrumHit, hat, ps::DrumKit::ClosedHat, st});
        e.post({CommandType::SetDrumHit, clap, ps::DrumKit::Clap, st});
        const bool gate = kind != Intro;
        e.post({CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 5) % 12, 0, kind == Drop && st % 4 == 2, false, gate}.pack()});
    }
}

struct Result {
    std::vector<double> rmsDb;     // per second
    std::vector<float> drive;      // per second
    std::vector<float> fader;      // per second (the master fader's target at its end)
    std::vector<int> loudSeconds;  // seconds wholly inside a Drop (song) or the loud scene, the silent beat left out
    std::vector<bool> inIntro;     // per second: some of it in an Intro (quiet on purpose: soft hats only)
    float lowestDrive = 0.0f, highestDrive = 0.0f;
};

Result play(const Case& c) {
    using ps::CommandType;
    ps::Engine e(kRate);
    e.setCommandBudget(ps::Engine::kLiveCommandBudget);
    const int stepsPerBar = 4 * c.beats;
    const std::vector<Part> form = {{Intro, 8}, {Drop, 16}, {Break, 8}, {Drop, 16}, {Break, 4}, {Drop, 32}, {Break, 8}, {Drop, 64}};
    // The parts in time, as long as the case plays (the form starts over at its end).
    const double stepFrames = 15.0 / c.bpm * kRate;
    std::vector<Part> parts;
    std::vector<int> partStart;  // song step of each part
    int steps = 0;
    for (size_t i = 0; steps * stepFrames < (c.seconds + 10) * double(kRate); ++i) {
        parts.push_back(form[i % form.size()]);
        partStart.push_back(steps);
        steps += parts.back().bars * stepsPerBar;
    }
    const int songBeats = steps / 4;
    e.post({CommandType::SetTempo, static_cast<float>(c.bpm)});
    e.post({CommandType::SetBeatsPerBar, 0.0f, c.beats});
    e.post({CommandType::SetMixVersion, c.dropMode ? 2.0f : 1.0f});
    e.post({CommandType::SetMaster, -7.0f, 4});  // jump up's target
    // The whole mix's curve (song only, a point per beat, each point held): full, 3 dB down in a
    // Break, silent on the beat before each Drop.
    if (c.inSong) {
        const int32_t master = ps::Automation::pack(ps::Automation::Volume, ps::Mixer::kMaster, 0, 0);
        e.post({CommandType::SetAutomationTarget, 0.0f, 0, master});
        e.post({CommandType::SetAutomationMode, static_cast<float>(songBeats), 0, ps::Automation::Song | ps::Automation::kPerBeat | ps::Automation::kHeld});
        for (size_t i = 0; i < parts.size(); ++i) {
            const int first = partStart[i] / 4, count = parts[i].bars * stepsPerBar / 4;
            const float v = parts[i].kind == Break ? faderValue(-3.0) : 1.0f;
            e.post({CommandType::SetAutomationSongRun, v, 0, first | (count << 16)});
            if (i + 1 < parts.size() && parts[i + 1].kind == Drop) e.post({CommandType::SetAutomationSongRun, 0.0f, 0, (first + count - 1) | (1 << 16)});
        }
    }
    postPart(e, parts[0].kind, stepsPerBar);
    e.post({CommandType::SetTransport, 1.0f, 0, c.inSong ? 0 : -1});

    Result r;
    r.lowestDrive = r.highestDrive = 0.0f;
    const int burst = 96;  // the phone's callback
    std::vector<float> block(size_t(burst) * 2);
    size_t staged = 0;  // the part staged last (0: none yet)
    uint32_t swaps = 0;
    int64_t frame = 0, stageAt = kRate * 150 / 1000;  // the app stages the next part 150 ms after ▶
    double sum = 0.0;
    int64_t inSecond = 0;
    const int64_t total = int64_t(c.seconds) * kRate;
    auto stage = [&](size_t next) {
        e.post({CommandType::SetStaging, 1.0f});
        postPart(e, parts[next].kind, stepsPerBar);
        e.post({CommandType::SetStaging, 0.0f});
        const int afterSteps = parts[next - 1].bars * stepsPerBar;
        if (c.inSong) e.post({CommandType::ArmSceneSwap, static_cast<float>(partStart[next]), afterSteps, 1});
        else e.post({CommandType::ArmSceneSwap, 0.0f, afterSteps, 3});  // a scene alone: outside the song
        staged = next;
    };
    while (frame < total) {
        if (stageAt >= 0 && frame >= stageAt && staged + 1 < parts.size()) {
            stage(staged + 1);
            stageAt = -1;
        }
        e.process(nullptr, 0, 0, block.data(), burst, 2);
        for (int i = 0; i < burst; ++i) sum += 0.5 * (double(block[size_t(i) * 2]) * block[size_t(i) * 2] + double(block[size_t(i) * 2 + 1]) * block[size_t(i) * 2 + 1]);
        frame += burst;
        inSecond += burst;
        const float d = e.mixer().masterChain().driveDb();
        r.lowestDrive = std::min(r.lowestDrive, d);
        r.highestDrive = std::max(r.highestDrive, d);
        if (e.sceneSwaps() != swaps) {
            swaps = e.sceneSwaps();
            stageAt = frame + kRate * 40 / 1000;  // the app's 40 ms watch
        }
        if (inSecond >= kRate) {
            r.rmsDb.push_back(sum > 0.0 ? 10.0 * std::log10(sum / double(inSecond)) : -200.0);
            r.drive.push_back(d);
            r.fader.push_back(e.mixer().masterFader());
            sum = 0.0;
            inSecond = 0;
        }
    }
    // Seconds wholly inside a Drop, its first beat and the beat before it left out.
    for (size_t s = 0; s < r.rmsDb.size(); ++s) {
        const double from = double(s) * kRate / stepFrames, to = double(s + 1) * kRate / stepFrames;  // in steps
        for (size_t i = 0; i < parts.size(); ++i) {
            const double a = partStart[i] + 4, b = partStart[i] + parts[i].bars * stepsPerBar - 4;
            if (parts[i].kind == Drop && from >= a && to <= b) r.loudSeconds.push_back(int(s));
        }
        bool intro = false;
        for (size_t i = 0; i < parts.size(); ++i) {
            const double a = partStart[i], b = partStart[i] + parts[i].bars * stepsPerBar;
            if (parts[i].kind == Intro && from < b && to > a) intro = true;
        }
        r.inIntro.push_back(intro);
    }
    return r;
}

void testLivePlaybackKeepsItsLevel() {
    const Case cases[] = {
        {"jump up 174 BPM 4/4, song", 174.0, 4, 75, true, true},
        {"60 BPM 3/4, song", 60.0, 3, 75, true, true},
        {"87 BPM 6/4 (6/8), song", 87.0, 6, 75, true, true},
        {"128 BPM 4/4, part looped alone", 128.0, 4, 75, true, false},
        {"200 BPM 7/4, song", 200.0, 7, 75, true, true},
        {"140 BPM 4/4, Song mode (older project)", 140.0, 4, 75, false, true},
        // The whole length in the plain build; the sanitizers' (6x slower) plays its first 2.5 minutes.
        {"128 BPM 4/4, a 6-minute club song", 128.0, 4, kSlowBuild > 1.0 ? 150 : 330, true, true},
    };
    for (const auto& c : cases) {
        const Result r = play(c);
        double loudest = -200.0, quietest = 0.0;
        for (int s : r.loudSeconds) {
            loudest = std::max(loudest, r.rmsDb[size_t(s)]);
            quietest = std::min(quietest, r.rmsDb[size_t(s)]);
        }
        // Per second: its level and the drive (the log a fade shows in).
        std::string log;
        for (size_t s = 0; s < r.rmsDb.size() && s < 75; s += 5) {
            char buf[48];
            std::snprintf(buf, sizeof buf, " %zus:%.0f/%+.0f", s + 1, r.rmsDb[s], double(r.drive[s]));
            log += buf;
        }
        std::printf("  %s: drive %+.1f..%+.1f dB, loud seconds %.1f..%.1f dBFS (%zu); per 5 s (dBFS/drive):%s\n", c.name, double(r.lowestDrive),
                    double(r.highestDrive), quietest, loudest, r.loudSeconds.size(), log.c_str());
        CHECK(!r.loudSeconds.empty());
        CHECK(r.lowestDrive >= ps::MasterChain::kMinDriveDb && r.highestDrive <= ps::MasterChain::kMaxDriveDb);
        // Every second of a Drop within 6 dB of the loudest one (the fade fell 2 dB a second, for good).
        CHECK(loudest > -30.0);
        CHECK(quietest > loudest - 6.0);
        // The last loud second as loud as the first ones: no slow slide down either.
        if (r.loudSeconds.size() >= 4) {
            const double first = r.rmsDb[size_t(r.loudSeconds.front())], last = r.rmsDb[size_t(r.loudSeconds.back())];
            CHECK(last > first - 6.0);
        }
        // The drive ends where a loud part needs it, never toward silence.
        CHECK(r.drive.back() > ps::MasterChain::kMinDriveDb + 6.0f);
        // Never silent while the transport runs, once the first Drop came (the fade ended in
        // silence): two seconds in a row under -50 dBFS (a silent beat lasts one beat; the form's
        // Intro, soft hats only, is that quiet on purpose when it comes round again).
        const size_t from = r.loudSeconds.empty() ? r.rmsDb.size() : size_t(r.loudSeconds.front());
        int silentRuns = 0;
        for (size_t s = from + 1; s < r.rmsDb.size(); ++s) {
            if (r.inIntro[s] || r.inIntro[s - 1] || r.rmsDb[s] >= -50.0 || r.rmsDb[s - 1] >= -50.0) continue;
            ++silentRuns;
            std::printf("  silent at %zu s: %.1f dBFS, drive %+.1f dB, fader %.3f\n", s + 1, r.rmsDb[s], double(r.drive[s]), double(r.fader[s]));
        }
        CHECK(silentRuns == 0);
        // In a Drop the whole mix's curve is back at unity (the silent beat and the Break let go).
        if (c.inSong) for (int s : r.loudSeconds) CHECK(r.fader[size_t(s)] == 1.0f);
    }
}

// The drive's estimate alone, in Drop mode: whatever the loss it once read, it never leaves its
// range, and a drive that took a loss past the cap off comes back to where the loss is the cap
// (it does not go on falling while the loss lags).
void testDropDriveStaysInRange() {
    ps::MasterChain chain;
    chain.prepare(kRate);
    chain.setMode(ps::MasterChain::Mode::Drop);
    chain.setTarget(-3.0f);  // far over what a limited loop can reach: the loss passes the cap
    std::vector<float> l(kRate / 10), rr(kRate / 10);
    float lowest = 0.0f, highest = 0.0f;
    uint32_t seed = 12345;
    for (int piece = 0; piece < 1200; ++piece) {  // 2 minutes
        // A quiet minute (hats) then a loud one (a square bass under a kick), 0.4 s phrases.
        const bool loud = piece >= 600;
        for (size_t i = 0; i < l.size(); ++i) {
            const int64_t t = int64_t(piece) * int64_t(l.size()) + int64_t(i);
            seed = seed * 1664525u + 1013904223u;
            const float noise = (float(seed >> 8) / float(1 << 24) - 0.5f);
            const float hat = (t % 12000) < 600 ? 0.05f * noise : 0.0f;
            const float bass = loud ? ((t / 120) % 2 ? 0.35f : -0.35f) : 0.0f;
            const float kick = loud && (t % 24000) < 2400 ? 0.6f * std::sin(float(t % 24000) * 0.008f) : 0.0f;
            l[i] = rr[i] = hat + bass + kick;
        }
        chain.process(l.data(), rr.data(), nullptr, int(l.size()));
        lowest = std::min(lowest, chain.driveDb());
        highest = std::max(highest, chain.driveDb());
    }
    std::printf("  Drop mode estimate: drive %+.1f..%+.1f dB, ends at %+.1f dB with a loss of %.2f LU\n", double(lowest), double(highest),
                double(chain.driveDb()), double(chain.lossDb()));
    CHECK(lowest >= ps::MasterChain::kMinDriveDb);
    CHECK(highest <= ps::MasterChain::kMaxDriveDb);
    CHECK(chain.driveDb() > ps::MasterChain::kMinDriveDb + 6.0f);
    CHECK(chain.lossDb() < ps::MasterChain::kMaxLossDropDb + 1.0f);
}

}  // namespace livefade_tests

#define PS_LIVEFADE_TESTS \
    {"live_playback_keeps_its_level", livefade_tests::testLivePlaybackKeepsItsLevel}, \
    {"drop_drive_stays_in_range", livefade_tests::testDropDriveStaysInRange},
