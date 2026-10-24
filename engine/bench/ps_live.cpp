// ps_live: replays what the app sends the phone's LIVE engine (a .pslive file recorded by the app
// test LiveSongRecorder: the project loaded, ▶ pressed, each next song part staged after the
// engine's scene swap) through an Engine driven as the phone's stream drives it: the live command
// budget, callbacks of 96 frames, the queue (4096 commands) drained by those callbacks only. Not
// the OfflineRenderer: what is heard on the phone, not what an export writes.
//
//   ps_live [--seconds S (90)] [--burst N (96)] [--wav out.wav] [--csv out.csv] [--quiet] <in.pslive>
//
// Prints one line per second (--quiet: none): song step, scene swaps, transport, the whole mix's
// song curve (the master fader's target, « lane ») and the fade as heard (its smoothed gain), the
// live loudness gain (the target's drive), the loss its estimate reads and the loud part it heard,
// the limiter's gain, the output's level over the second and over 3 s (short-term), its peak and
// its largest frame-to-frame step. --csv writes the same. Then the flags (docs/son/BANC.md,
// « Écoute en direct »): NAN, GAIN (live gain out of its range), SILENCE (two seconds under
// -50 dBFS while playing, not planned by the song curve), FADE (the short-term level 6 dB under
// the 10 s before while the live gain fell 3 dB, the song curve not planning it), CLICK (a step
// over 0.3, 20x the RMS of the 16 steps before and after it, twice any such step of the 4 s around
// it: a discontinuity; best effort, a hit's attack or a 303's edge is not one). Exit 4 when something is flagged.
//
// File format: the .pscmd lines (C, SAMPLE, PAD, TRACKSAMPLE, AUDIOCLIP, STEMS 0, INSTRUMENT a 0;
// see ps_render.cpp), plus
//   WAIT ms <n>     the app's own pause (a coroutine's delay): the stream plays meanwhile
//   WAIT swaps <n>  the app reacts to the engine's n-th scene swap (its 40 ms watch): the stream
//                   plays until the engine has made it
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <map>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

#include "Engine.h"
#include "MasterChain.h"
#include "Sample.h"
#include "WavWriter.h"

namespace {

std::string dirOf(const std::string& path) {
    const auto slash = path.find_last_of('/');
    return slash == std::string::npos ? std::string(".") : path.substr(0, slash);
}

bool readFloats(const std::string& path, std::vector<float>& out) {
    std::FILE* f = std::fopen(path.c_str(), "rb");
    if (!f) return false;
    std::fseek(f, 0, SEEK_END);
    const long bytes = std::ftell(f);
    std::fseek(f, 0, SEEK_SET);
    out.resize(static_cast<size_t>(bytes) / sizeof(float));
    const size_t got = std::fread(out.data(), sizeof(float), out.size(), f);
    std::fclose(f);
    return got == out.size();
}

[[noreturn]] void fail(int line, const std::string& what) {
    std::fprintf(stderr, "ps_live: line %d: %s\n", line, what.c_str());
    std::exit(2);
}

// One second of what the phone plays.
struct Second {
    int t = 0;
    int songStep = -1;
    unsigned swaps = 0;
    bool running = false;
    float lane = 1.0f;      // the whole mix's song curve (the master fader's target), 1 = full
    float fade = 1.0f;      // the master fader as heard (its smoothed gain, lowest of the second)
    float drive = 0.0f;     // the live loudness gain (dB)
    float loss = 0.0f, loud = -1000.0f;
    float limiter = 1.0f;
    double rmsDb = -200.0;  // this second
    double shortDb = -200.0;  // the last 3 s (short-term level)
    float peak = 0.0f;
    float jump = 0.0f;      // largest frame-to-frame step
    float clickStep = 0.0f;  // the step (over 0.3) most alone among its neighbours, and how much (x their RMS)
    double isolation = 0.0;
    bool nonFinite = false;
};

struct Player {
    ps::Engine engine{48000};
    int burst = 96;
    int64_t frames = 0, limit = 0;
    std::vector<float> block;
    std::vector<float> wav;  // interleaved stereo, when asked
    bool keepWav = false;
    bool quiet = false;
    // This second.
    double sumSq = 0.0;
    float peak = 0.0f, jump = 0.0f, lowestFade = 1.0f, lastL = 0.0f, lastR = 0.0f;
    std::vector<float> steps;  // every frame-to-frame step (the larger channel's): the clicks are read from them at the end
    int64_t inSecond = 0;
    std::vector<Second> seconds;
    std::vector<double> lastSums;  // the mean squares of the seconds so far
    std::FILE* csv = nullptr;
    bool done() const { return frames >= limit; }

    void tick() {
        if (done()) return;
        block.assign(static_cast<size_t>(burst) * 2, 0.0f);
        engine.process(nullptr, 0, 0, block.data(), burst, 2);
        for (int i = 0; i < burst; ++i) {
            const float l = block[static_cast<size_t>(i) * 2], r = block[static_cast<size_t>(i) * 2 + 1];
            sumSq += 0.5 * (double(l) * l + double(r) * r);
            const float d = std::max(std::fabs(l - lastL), std::fabs(r - lastR));
            steps.push_back(d);
            jump = std::max(jump, d);
            peak = std::max(peak, std::max(std::fabs(l), std::fabs(r)));
            lastL = l;
            lastR = r;
        }
        lowestFade = std::min(lowestFade, engine.mixer().masterFaderNow());
        if (keepWav) wav.insert(wav.end(), block.begin(), block.end());
        frames += burst;
        inSecond += burst;
        if (inSecond >= 48000) report();
    }
    static double db(double meanSquare) { return meanSquare > 1e-20 ? 10.0 * std::log10(meanSquare) : -200.0; }
    void report() {
        auto& mixer = engine.mixer();
        Second s;
        s.t = static_cast<int>(seconds.size()) + 1;
        s.songStep = engine.songStep();
        s.swaps = engine.sceneSwaps();
        s.running = engine.transportRunning();
        s.lane = mixer.masterFader();
        s.fade = lowestFade;
        s.drive = mixer.masterChain().driveDb();
        s.loss = mixer.masterChain().lossDb();
        s.loud = mixer.masterChain().loudLevel();
        s.limiter = mixer.limiterGain();
        const double ms = sumSq / static_cast<double>(inSecond);
        lastSums.push_back(ms);
        s.rmsDb = db(ms);
        double three = 0.0;
        const size_t n = std::min<size_t>(3, lastSums.size());
        for (size_t k = lastSums.size() - n; k < lastSums.size(); ++k) three += lastSums[k];
        s.shortDb = db(three / static_cast<double>(n));
        s.peak = peak;
        s.jump = jump;
        s.nonFinite = engine.hasNonFinite();
        seconds.push_back(s);
        if (!quiet) {
            std::printf("t=%3ds step=%6d swaps=%3u %s lane=%.3f fade=%.3f drive=%+6.2fdB loss=%5.2fLU loud=%7.2fLUFS limiter=%.3f "
                        "level=%7.2fdBFS short=%7.2fdBFS peak=%.3f jump=%.3f%s\n",
                        s.t, s.songStep, s.swaps, s.running ? "play" : "stop", double(s.lane), double(s.fade), double(s.drive), double(s.loss),
                        double(s.loud), double(s.limiter), s.rmsDb, s.shortDb, double(s.peak), double(s.jump), s.nonFinite ? " NaN" : "");
        }
        if (csv) {
            std::fprintf(csv, "%d,%d,%u,%d,%.5f,%.5f,%.3f,%.3f,%.3f,%.4f,%.3f,%.3f,%.4f,%.4f,%d\n", s.t, s.songStep, s.swaps, s.running ? 1 : 0,
                         double(s.lane), double(s.fade), double(s.drive), double(s.loss), double(s.loud), double(s.limiter), s.rmsDb, s.shortDb,
                         double(s.peak), double(s.jump), s.nonFinite ? 1 : 0);
        }
        sumSq = 0.0;
        peak = jump = 0.0f;
        lowestFade = 1.0f;
        inSecond = 0;
    }
    // As AudioHost::post: a full queue waits for the audio thread.
    void post(const ps::Command& c) {
        while (!engine.post(c)) {
            if (done()) return;
            tick();
        }
    }
    void playFor(int64_t f) {
        const int64_t until = frames + f;
        while (frames < until && !done()) tick();
    }
};

// What a listener would call a fault, from the seconds played. The plan (the whole mix's song
// curve: a silent beat, a Montée's dip, a Break down, a fade the user drew) is not one.
// A click: a step over 0.3 that stands kClickAlone x out of the 16 steps before it AND the 16
// after it (a discontinuity: smooth on both sides). A sine of 100 Hz cut at half scale stands 70x
// out; a hit's attack or a 303's band-limited edge rings on after its step (measured over the 37
// styles: under 4x).
constexpr double kClickAlone = 20.0;

void findClicks(const std::vector<float>& steps, std::vector<Second>& sec) {
    constexpr int kSide = 16, kRate = 48000;
    for (size_t j = kSide; j + kSide + 1 < steps.size(); ++j) {
        const float d = steps[j];
        if (d <= 0.3f) continue;
        const size_t t = j / kRate;
        if (t >= sec.size()) break;
        double before = 0.0, after = 0.0;
        for (int k = 1; k <= kSide; ++k) {
            before += double(steps[j - size_t(k)]) * steps[j - size_t(k)];
            after += double(steps[j + 1 + size_t(k)]) * steps[j + 1 + size_t(k)];  // the frame after the step may still be on its edge
        }
        const double around = std::sqrt(std::max(before, after) / kSide);
        const double alone = around > 1e-9 ? double(d) / around : 1e9;
        if (alone > sec[t].isolation) {
            sec[t].isolation = alone;
            sec[t].clickStep = d;
        }
    }
}

std::vector<std::string> flags(const std::vector<Second>& sec) {
    std::vector<std::string> out;
    char buf[256];
    auto add = [&](const char* what, int t, const char* detail) {
        std::snprintf(buf, sizeof buf, "%s at %d s: %s", what, t, detail);
        out.emplace_back(buf);
    };
    // From the first second the music is heard.
    size_t first = sec.size();
    for (size_t i = 0; i < sec.size(); ++i) {
        if (sec[i].running && sec[i].rmsDb > -60.0) {
            first = i;
            break;
        }
    }
    if (first == sec.size()) {
        out.emplace_back("SILENCE: nothing heard at all");
        return out;
    }
    char detail[160];
    bool nonFinite = false, range = false;
    for (size_t i = first; i < sec.size(); ++i) {
        const Second& s = sec[i];
        if (s.nonFinite && !nonFinite) {
            add("NAN", s.t, "a non-finite sample reached the output");
            nonFinite = true;
        }
        if (!range && (s.drive < ps::MasterChain::kMinDriveDb || s.drive > ps::MasterChain::kMaxDriveDb)) {
            std::snprintf(detail, sizeof detail, "live gain %+.1f dB out of its range", double(s.drive));
            add("GAIN", s.t, detail);
            range = true;
        }
        // Silence while the transport runs: two seconds in a row under -50 dBFS, the song curve
        // not planning it (a fade the user drew down to nothing).
        if (i > first && s.running && sec[i - 1].running && s.rmsDb < -50.0 && sec[i - 1].rmsDb < -50.0 && s.lane >= 0.1f && sec[i - 1].lane >= 0.1f) {
            std::snprintf(detail, sizeof detail, "%.1f dBFS while playing (live gain %+.1f dB, song curve %.2f)", s.rmsDb, double(s.drive), double(s.lane));
            add("SILENCE", s.t, detail);
        }
        // An unplanned fade: the short-term level more than 6 dB under its best of the 10 s before
        // while the live gain fell (more than 3 dB) and the song curve did not plan it. A quieter
        // section with the gain steady is the music itself.
        if (i >= first + 10 && s.running) {
            double best = -200.0;
            float gainBefore = -100.0f, laneBefore = 0.0f;
            for (size_t k = i - 10; k < i; ++k) {
                best = std::max(best, sec[k].shortDb);
                gainBefore = std::max(gainBefore, sec[k].drive);
                laneBefore = std::max(laneBefore, sec[k].lane);
            }
            const double planned = laneBefore > 0.0f && s.lane > 0.0f ? 20.0 * std::log10(double(s.lane) / double(laneBefore)) : -200.0;
            if (s.shortDb < best - 6.0 && s.drive < gainBefore - 3.0f && planned > -6.0) {
                std::snprintf(detail, sizeof detail, "%.1f dB under the 10 s before, live gain down %.1f dB", best - s.shortDb, double(gainBefore - s.drive));
                add("FADE", s.t, detail);
            }
        }
        // A click (findClicks), twice as large as any other of the 4 s around it (a sound that
        // starts hard on every beat is the sound itself).
        if (s.isolation > kClickAlone) {
            float around = 0.0f;
            for (size_t k = i >= 4 ? i - 4 : 0; k < std::min(sec.size(), i + 5); ++k) {
                if (k != i && sec[k].isolation > kClickAlone) around = std::max(around, sec[k].clickStep);
            }
            if (s.clickStep > 2.0f * around) {
                std::snprintf(detail, sizeof detail, "a step of %.3f out of the steps around it %.0fx (4 s around: %.3f)", double(s.clickStep),
                              std::min(s.isolation, 1e6), double(around));
                add("CLICK", s.t, detail);
            }
        }
    }
    return out;
}

}  // namespace

int main(int argc, char** argv) {
    double seconds = 90.0;
    bool quiet = false;
    std::string wavPath, csvPath;
    std::vector<std::string> files;
    int burst = 96;
    for (int i = 1; i < argc; ++i) {
        if (!std::strcmp(argv[i], "--seconds") && i + 1 < argc) seconds = std::atof(argv[++i]);
        else if (!std::strcmp(argv[i], "--burst") && i + 1 < argc) burst = std::max(16, std::atoi(argv[++i]));
        else if (!std::strcmp(argv[i], "--wav") && i + 1 < argc) wavPath = argv[++i];
        else if (!std::strcmp(argv[i], "--csv") && i + 1 < argc) csvPath = argv[++i];
        else if (!std::strcmp(argv[i], "--quiet")) quiet = true;
        else files.emplace_back(argv[i]);
    }
    if (files.size() != 1) {
        std::fprintf(stderr, "usage: ps_live [--seconds S] [--burst N] [--wav out.wav] [--csv out.csv] [--quiet] <in.pslive>\n");
        return 1;
    }
    std::ifstream in(files[0]);
    if (!in) {
        std::fprintf(stderr, "ps_live: cannot read %s\n", files[0].c_str());
        return 1;
    }
    const std::string base = dirOf(files[0]);
    auto player = std::make_unique<Player>();
    player->burst = burst;
    player->limit = static_cast<int64_t>(seconds * 48000.0);
    player->keepWav = !wavPath.empty();
    player->quiet = quiet;
    player->engine.prepare(48000);
    player->engine.setCommandBudget(ps::Engine::kLiveCommandBudget);
    if (!csvPath.empty()) {
        player->csv = std::fopen(csvPath.c_str(), "w");
        if (player->csv) std::fprintf(player->csv, "second,song_step,swaps,running,lane,fade,drive_db,loss_db,loud_lufs,limiter,rms_db,short_db,peak,jump,non_finite\n");
    }

    std::map<long, ps::Sample*> samples;
    auto sampleOf = [&](long id, int line) -> ps::Sample* {
        if (id == 0) return nullptr;
        auto it = samples.find(id);
        if (it == samples.end()) fail(line, "unknown sample " + std::to_string(id));
        it->second->retain();
        return it->second;
    };
    auto install = [&](ps::CommandType type, int a, ps::Sample* s) {
        ps::Command c{type, 0.0f, a, 0};
        c.sample = s;
        player->post(c);
    };

    std::string text;
    int line = 0;
    while (std::getline(in, text) && !player->done()) {
        ++line;
        std::istringstream ls(text);
        std::string op;
        if (!(ls >> op) || op[0] == '#') continue;
        if (op == "C") {
            int type = 0;
            std::string value;
            int32_t a = 0, b = 0;
            if (!(ls >> type >> value >> a >> b)) fail(line, "bad command");
            player->post({static_cast<ps::CommandType>(type), std::strtof(value.c_str(), nullptr), a, b});
        } else if (op == "WAIT") {
            std::string what;
            long long n = 0;
            if (!(ls >> what >> n)) fail(line, "bad WAIT");
            if (what == "ms") {
                player->playFor(n * 48);
            } else if (what == "swaps") {
                while (static_cast<long long>(player->engine.sceneSwaps()) < n && !player->done()) player->tick();
            } else {
                fail(line, "bad WAIT " + what);
            }
        } else if (op == "SAMPLE") {
            long id = 0;
            int channels = 0, rate = 0;
            std::string file;
            if (!(ls >> id >> channels >> rate >> file)) fail(line, "bad SAMPLE");
            std::vector<float> data;
            if (!readFloats(base + "/" + file, data)) fail(line, "cannot read " + file);
            if (channels < 1 || static_cast<long>(data.size()) < channels) fail(line, "empty sample");
            ps::Sample* s = ps::Sample::create(data.data(), static_cast<int64_t>(data.size()) / channels, channels, rate, 48000);
            if (!s) fail(line, "sample rejected");
            samples[id] = s;
        } else if (op == "PAD" || op == "TRACKSAMPLE" || op == "AUDIOCLIP") {
            int index = 0;
            long id = 0;
            if (!(ls >> index >> id)) fail(line, "bad " + op);
            ps::Sample* s = sampleOf(id, line);
            if (op == "PAD") install(ps::CommandType::InstallSample, index, s);
            else if (op == "AUDIOCLIP") install(ps::CommandType::InstallAudioClip, index, s);
            else install(ps::CommandType::InstallTrackSample, (std::clamp(index & 0xFF, 0, ps::kTracks - 1) << 16) | ((index >> 8) & 0xFFFF), s);
        } else if (op == "STEMS") {
            ps::Command c{ps::CommandType::InstallStems};
            player->post(c);
        } else if (op == "INSTRUMENT") {
            int32_t a = 0;
            ls >> a;
            player->post({ps::CommandType::SetInstrument, 0.0f, a, 0});
        } else if (op == "PSCMD" || op == "RATE" || op == "META" || op == "SONG" || op == "SECTION") {
            continue;
        } else {
            fail(line, "unknown line: " + op);
        }
    }
    // The song plays on to the end of the time asked.
    while (!player->done()) player->tick();
    if (player->csv) std::fclose(player->csv);
    int status = 0;
    if (!wavPath.empty()) {
        ps::WavWriter w;
        if (!w.open(wavPath.c_str(), 48000, 2, true) || !w.write(player->wav.data(), static_cast<int>(player->wav.size() / 2)) || !w.close()) status = 3;
    }
    findClicks(player->steps, player->seconds);
    const auto found = flags(player->seconds);
    for (const auto& f : found) std::printf("FLAG %s\n", f.c_str());
    std::printf("%s: %zu flag%s over %zu s\n", found.empty() ? "OK" : "FLAGGED", found.size(), found.size() == 1 ? "" : "s", player->seconds.size());
    if (status == 0 && !found.empty()) status = 4;
    player.reset();
    for (auto& [id, s] : samples) {
        if (s->release()) delete s;
    }
    return status;
}
