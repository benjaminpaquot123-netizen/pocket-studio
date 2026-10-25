// ps_live: replays what the app sends the phone's LIVE engine (a .pslive file recorded by the app
// test LiveSongRecorder: the project loaded, ▶ pressed, each next song part staged after the
// engine's scene swap) through an Engine driven as the phone's stream drives it: the live command
// budget, callbacks of 96 frames, the queue (4096 commands) drained by those callbacks only. Not
// the OfflineRenderer: what is heard on the phone, not what an export writes.
//
//   ps_live [--seconds S (90)] [--burst N (96)] [--wav out.wav] [--csv out.csv] [--quiet]
//           [--cpu-scale X (3)] [--buffer-bursts B (2)] [--cpu-out times.f32] [--alloc-probe] <in.pslive>
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
// it: a discontinuity; best effort, a hit's attack or a 303's edge is not one), ALLOC (the audio
// callback allocated or freed heap memory: the real-time rule; not counted under AddressSanitizer;
// --alloc-probe makes one such call itself, for the self-test),
// DRIFT (a loop of the song, when it starts over, 1 dB louder or softer than its first time round,
// or its live gain 1.5 dB away: a soak over many loops). Exit 4 when something is flagged.
//
// The audio thread's work (docs/TESTS-REELS.md, « Petit téléphone »): each callback's CPU time on
// this thread (not wall time: other processes do not count) against its real-time budget (burst /
// 48 kHz), printed as one CPU line: mean, 99th and 99.9th percentiles and worst block, in % of the
// budget. A slower phone is modelled by --cpu-scale (each block's time x X): the blocks that would
// then miss their deadline, and the glitches heard with a buffer of B bursts (the backlog of late
// blocks eating the buffer's margin). Never a flag: one run on a shared machine is noisy;
// --cpu-out writes each block's time (float32 seconds) for scripts/cpu-bench.sh, which keeps the
// fastest of several runs block by block (the replay is the same each time).
//
// File format: the .pscmd lines (C, SAMPLE, PAD, TRACKSAMPLE, AUDIOCLIP, STEMS 0, INSTRUMENT a 0;
// see ps_render.cpp), plus
//   WAIT ms <n>     the app's own pause (a coroutine's delay): the stream plays meanwhile
//   WAIT swaps <n>  the app reacts to the engine's n-th scene swap (its 40 ms watch): the stream
//                   plays until the engine has made it
#include <algorithm>
#include <atomic>
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

#include <time.h>

#include "Engine.h"
#include "MasterChain.h"
#include "Sample.h"
#include "WavWriter.h"

// Heap calls made inside Engine::process (the audio callback) by the thread running it, counted
// while g_inCallback is set: the real-time rule says none. Counted where every allocation goes
// through on glibc: malloc, calloc, realloc, aligned_alloc, posix_memalign, memalign and free,
// interposed here and handed to glibc's own (__libc_*). operator new and delete (plain, array,
// sized, nothrow, aligned) come down to those in libstdc++. Not counted: valloc and pvalloc (never
// used by the engine), and under AddressSanitizer (it owns the allocator) or without glibc.
static thread_local bool g_inCallback = false;
static std::atomic<long> g_callbackNews{0}, g_callbackDeletes{0};
#if !defined(__SANITIZE_ADDRESS__) && defined(__GLIBC__)
extern "C" {
void* __libc_malloc(size_t);
void* __libc_calloc(size_t, size_t);
void* __libc_realloc(void*, size_t);
void* __libc_memalign(size_t, size_t);
void __libc_free(void*);

static inline void countNew() {
    if (g_inCallback) g_callbackNews.fetch_add(1, std::memory_order_relaxed);
}
void* malloc(size_t n) {
    countNew();
    return __libc_malloc(n);
}
void* calloc(size_t n, size_t size) {
    countNew();
    return __libc_calloc(n, size);
}
void* realloc(void* p, size_t n) {
    countNew();
    return __libc_realloc(p, n);
}
void* memalign(size_t alignment, size_t n) {
    countNew();
    return __libc_memalign(alignment, n);
}
void* aligned_alloc(size_t alignment, size_t n) {
    countNew();
    return __libc_memalign(alignment, n);
}
int posix_memalign(void** out, size_t alignment, size_t n) {
    countNew();
    if (alignment < sizeof(void*) || (alignment & (alignment - 1)) != 0) return 22;  // EINVAL
    void* p = __libc_memalign(alignment, n);
    if (!p && n) return 12;  // ENOMEM
    *out = p;
    return 0;
}
void free(void* p) {
    if (p && g_inCallback) g_callbackDeletes.fetch_add(1, std::memory_order_relaxed);
    __libc_free(p);
}
}
#define PS_LIVE_COUNTS_ALLOCATIONS 1
#else
#define PS_LIVE_COUNTS_ALLOCATIONS 0
#endif

namespace {

void* volatile g_probe = nullptr;

// CPU time of this thread (not wall time: other processes taking the cores do not count).
double threadSeconds() {
    timespec ts{};
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &ts);
    return static_cast<double>(ts.tv_sec) + static_cast<double>(ts.tv_nsec) * 1e-9;
}

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

// A click: a step over 0.3 that stands kClickAlone x out of the 16 steps before it AND the 16
// after it (a discontinuity: smooth on both sides). A sine of 100 Hz cut at half scale stands 70x
// out; a hit's attack or a 303's band-limited edge rings on after its step (measured over the 37
// styles: under 4x).
constexpr double kClickAlone = 20.0;
constexpr int kClickSide = 16;

struct Player {
    ps::Engine engine{48000};
    int burst = 96;
    int64_t frames = 0, limit = 0;
    std::vector<float> block;
    std::vector<float> wav;  // interleaved stereo, when asked
    bool keepWav = false;
    bool quiet = false;
    // The self-test's probe (--alloc-probe): one heap call inside the callback's window, which the
    // ALLOC check must see (scripts/bench/live_selftest.py).
    bool allocProbe = false;
    // This second.
    double sumSq = 0.0;
    float peak = 0.0f, jump = 0.0f, lowestFade = 1.0f, lastL = 0.0f, lastR = 0.0f;
    int64_t inSecond = 0;
    std::vector<Second> seconds;
    std::vector<double> lastSums;  // the mean squares of the seconds so far
    std::FILE* csv = nullptr;
    // Each callback's CPU time (s) on this thread.
    std::vector<float> cpu;
    // Clicks, read as the steps come (a soak of 30 minutes keeps a few thousand steps, not all of
    // them): the steps not looked at yet with the kClickSide before them, from frame stepsBase.
    std::vector<float> steps;
    int64_t stepsBase = 0, nextLook = kClickSide;
    std::vector<double> isolationOf;  // per second: the step most alone among its neighbours, how much
    std::vector<float> clickStepOf;   // and its size
    bool done() const { return frames >= limit; }

    void tick() {
        if (done()) return;
        block.assign(static_cast<size_t>(burst) * 2, 0.0f);
        g_inCallback = true;
        const double t0 = threadSeconds();
        engine.process(nullptr, 0, 0, block.data(), burst, 2);
        if (allocProbe && cpu.size() == 100) {
            g_probe = ::operator new(16);  // a direct call: never elided, as a new-expression may be
            ::operator delete(g_probe);
        }
        const double t1 = threadSeconds();
        g_inCallback = false;
        cpu.push_back(static_cast<float>(t1 - t0));
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
        lookForClicks();
        lowestFade = std::min(lowestFade, engine.mixer().masterFaderNow());
        if (keepWav) wav.insert(wav.end(), block.begin(), block.end());
        frames += burst;
        inSecond += burst;
        if (inSecond >= 48000) report();
    }
    // Every step with kClickSide + 1 after it is looked at once (as the whole run's steps were).
    void lookForClicks() {
        constexpr int kRate = 48000;
        const int64_t end = stepsBase + static_cast<int64_t>(steps.size());
        for (; nextLook + kClickSide + 1 < end; ++nextLook) {
            const size_t j = static_cast<size_t>(nextLook - stepsBase);
            const float d = steps[j];
            if (d <= 0.3f) continue;
            double before = 0.0, after = 0.0;
            for (int k = 1; k <= kClickSide; ++k) {
                before += double(steps[j - size_t(k)]) * steps[j - size_t(k)];
                after += double(steps[j + 1 + size_t(k)]) * steps[j + 1 + size_t(k)];  // the frame after the step may still be on its edge
            }
            const double around = std::sqrt(std::max(before, after) / kClickSide);
            const double alone = around > 1e-9 ? double(d) / around : 1e9;
            const size_t t = static_cast<size_t>(nextLook / kRate);
            if (t >= isolationOf.size()) {
                isolationOf.resize(t + 1, 0.0);
                clickStepOf.resize(t + 1, 0.0f);
            }
            if (alone > isolationOf[t]) {
                isolationOf[t] = alone;
                clickStepOf[t] = d;
            }
        }
        // The steps looked at go, but the kClickSide the next look needs before it.
        const int64_t keepFrom = nextLook - kClickSide;
        if (keepFrom - stepsBase > 8192) {
            steps.erase(steps.begin(), steps.begin() + (keepFrom - stepsBase));
            stepsBase = keepFrom;
        }
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
    // The clicks found, into their seconds (the steps of the last partial second are left, as before).
    void clicksIntoSeconds() {
        for (size_t t = 0; t < seconds.size() && t < isolationOf.size(); ++t) {
            seconds[t].isolation = isolationOf[t];
            seconds[t].clickStep = clickStepOf[t];
        }
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

// The audio thread's time against its budget, a slower phone modelled by [scale].
struct CpuReport {
    double budget = 0.0, mean = 0.0, p99 = 0.0, p999 = 0.0, max = 0.0, maxAt = 0.0;
    long blocks = 0, over = 0, glitches = 0;
};

CpuReport cpuReport(const std::vector<float>& cpu, int burst, double scale, int bufferBursts) {
    CpuReport r;
    r.budget = burst / 48000.0;
    r.blocks = static_cast<long>(cpu.size());
    if (cpu.empty()) return r;
    double sum = 0.0, backlog = 0.0;
    const double margin = std::max(0, bufferBursts - 1) * r.budget;
    for (size_t i = 0; i < cpu.size(); ++i) {
        const double t = cpu[i];
        sum += t;
        if (t > r.max) {
            r.max = t;
            r.maxAt = double(i) * burst / 48000.0;
        }
        const double slow = t * scale;
        if (slow > r.budget) ++r.over;
        // The late blocks eat the buffer's margin; once it is gone, the next late one is heard.
        backlog = std::max(0.0, backlog + slow - r.budget);
        if (backlog > margin) {
            ++r.glitches;
            backlog = 0.0;
        }
    }
    r.mean = sum / double(cpu.size());
    std::vector<float> sorted(cpu);
    auto at = [&](double q) {
        const size_t k = std::min(sorted.size() - 1, static_cast<size_t>(q * double(sorted.size() - 1)));
        std::nth_element(sorted.begin(), sorted.begin() + long(k), sorted.end());
        return double(sorted[k]);
    };
    r.p99 = at(0.99);
    r.p999 = at(0.999);
    return r;
}

// What a listener would call a fault, from the seconds played. The plan (the whole mix's song
// curve: a silent beat, a Montée's dip, a Break down, a fade the user drew) is not one.
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
        // A click (Player::lookForClicks), twice as large as any other of the 4 s around it (a sound that
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

// The song looped (a soak): each time round, its level and live gain against its second time
// round (the first one is where the live gain learns the song). The song starting over is its
// position going back. Only whole loops count (the last one, cut by the time asked, does not).
struct Loop {
    int from = 0, to = 0;  // seconds [from, to)
    double levelDb = -200.0;
    double driveDb = 0.0;
};

std::vector<Loop> loopsOf(const std::vector<Second>& sec) {
    std::vector<Loop> out;
    int start = -1;
    for (size_t i = 0; i < sec.size(); ++i) {
        if (start < 0) {
            if (sec[i].running && sec[i].songStep >= 0) start = static_cast<int>(i);
            continue;
        }
        if (sec[i].songStep >= 0 && sec[i - 1].songStep >= 0 && sec[i].songStep + 16 < sec[i - 1].songStep) {
            out.push_back({start, static_cast<int>(i)});
            start = static_cast<int>(i);
        }
    }
    for (auto& l : out) {
        double sum = 0.0, drive = 0.0;
        int n = 0;
        for (int i = l.from; i < l.to; ++i) {
            sum += std::pow(10.0, sec[size_t(i)].rmsDb / 10.0);
            drive += sec[size_t(i)].drive;
            ++n;
        }
        if (n > 0) {
            l.levelDb = sum > 1e-20 * n ? 10.0 * std::log10(sum / n) : -200.0;
            l.driveDb = drive / n;
        }
    }
    return out;
}

void driftFlags(const std::vector<Loop>& loops, std::vector<std::string>& out) {
    if (loops.size() < 3) return;
    const Loop& ref = loops[1];
    char buf[256];
    for (size_t k = 2; k < loops.size(); ++k) {
        const double level = loops[k].levelDb - ref.levelDb, drive = loops[k].driveDb - ref.driveDb;
        if (std::fabs(level) > 1.0 || std::fabs(drive) > 1.5) {
            std::snprintf(buf, sizeof buf, "DRIFT at %d s: loop %zu is %+.2f dB (live gain %+.2f dB) from loop 2", loops[k].from + 1, k + 1, level, drive);
            out.emplace_back(buf);
        }
    }
}

}  // namespace

int main(int argc, char** argv) {
    double seconds = 90.0;
    bool quiet = false;
    std::string wavPath, csvPath;
    std::vector<std::string> files;
    int burst = 96, bufferBursts = 2;
    double cpuScale = 3.0;
    std::string cpuPath;
    bool allocProbe = false;
    for (int i = 1; i < argc; ++i) {
        if (!std::strcmp(argv[i], "--seconds") && i + 1 < argc) seconds = std::atof(argv[++i]);
        else if (!std::strcmp(argv[i], "--burst") && i + 1 < argc) burst = std::max(16, std::atoi(argv[++i]));
        else if (!std::strcmp(argv[i], "--wav") && i + 1 < argc) wavPath = argv[++i];
        else if (!std::strcmp(argv[i], "--csv") && i + 1 < argc) csvPath = argv[++i];
        else if (!std::strcmp(argv[i], "--quiet")) quiet = true;
        else if (!std::strcmp(argv[i], "--cpu-scale") && i + 1 < argc) cpuScale = std::max(0.01, std::atof(argv[++i]));
        else if (!std::strcmp(argv[i], "--buffer-bursts") && i + 1 < argc) bufferBursts = std::max(1, std::atoi(argv[++i]));
        else if (!std::strcmp(argv[i], "--cpu-out") && i + 1 < argc) cpuPath = argv[++i];
        else if (!std::strcmp(argv[i], "--alloc-probe")) allocProbe = true;
        else files.emplace_back(argv[i]);
    }
    if (files.size() != 1) {
        std::fprintf(stderr, "usage: ps_live [--seconds S] [--burst N] [--wav out.wav] [--csv out.csv] [--quiet] "
                             "[--cpu-scale X] [--buffer-bursts B] [--cpu-out times.f32] <in.pslive>\n");
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
    player->allocProbe = allocProbe;
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
            else install(ps::CommandType::InstallTrackSample, (std::clamp(index & 0xFF, 0, ps::kSounds - 1) << 16) | ((index >> 8) & 0xFFFF), s);
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
    player->clicksIntoSeconds();
    auto found = flags(player->seconds);
    // Many loops of the song (a soak): none drifts away from the others.
    const auto loops = loopsOf(player->seconds);
    if (loops.size() >= 2) {
        std::printf("LOOPS %zu:", loops.size());
        for (const auto& l : loops) std::printf(" %d-%ds %.2fdB/%+.2fdB", l.from + 1, l.to, l.levelDb, l.driveDb);
        std::printf("\n");
    }
    driftFlags(loops, found);
    // The real-time rule: the callback neither allocates nor frees.
    const long news = g_callbackNews.load(), deletes = g_callbackDeletes.load();
    if (PS_LIVE_COUNTS_ALLOCATIONS) {
        std::printf("ALLOC news=%ld deletes=%ld in %zu callbacks\n", news, deletes, player->cpu.size());
        if (news > 0 || deletes > 0) {
            char buf[160];
            std::snprintf(buf, sizeof buf, "ALLOC: the audio callback allocated %ld and freed %ld times", news, deletes);
            found.emplace_back(buf);
        }
    } else {
        std::printf("ALLOC not counted (sanitizer build)\n");
    }
    // The audio thread's time against its budget, and on a phone cpuScale times slower.
    const CpuReport cpu = cpuReport(player->cpu, burst, cpuScale, bufferBursts);
    const double pct = cpu.budget > 0.0 ? 100.0 / cpu.budget : 0.0;
    std::printf("CPU burst=%d budget_ms=%.3f blocks=%ld mean=%.2f%% p99=%.2f%% p999=%.2f%% max=%.2f%% max_at=%.1fs scale=%.2f buffer=%d over=%ld glitches=%ld\n",
                burst, cpu.budget * 1000.0, cpu.blocks, cpu.mean * pct, cpu.p99 * pct, cpu.p999 * pct, cpu.max * pct, cpu.maxAt, cpuScale, bufferBursts,
                cpu.over, cpu.glitches);
    if (!cpuPath.empty()) {
        std::FILE* f = std::fopen(cpuPath.c_str(), "wb");
        if (!f || std::fwrite(player->cpu.data(), sizeof(float), player->cpu.size(), f) != player->cpu.size()) status = 3;
        if (f) std::fclose(f);
    }
    for (const auto& f : found) std::printf("FLAG %s\n", f.c_str());
    std::printf("%s: %zu flag%s over %zu s\n", found.empty() ? "OK" : "FLAGGED", found.size(), found.size() == 1 ? "" : "s", player->seconds.size());
    if (status == 0 && !found.empty()) status = 4;
    player.reset();
    for (auto& [id, s] : samples) {
        if (s->release()) delete s;
    }
    return status;
}
