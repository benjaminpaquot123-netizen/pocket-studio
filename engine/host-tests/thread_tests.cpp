// Audit stage 0: the engine's threads at the same time, for ThreadSanitizer
// (scripts/host-tests-tsan.sh). An audio thread runs the callback while a UI thread posts
// commands (notes, transport, samples installed and replaced), frees what the audio thread let go
// of and reads the measures the app reads, all on one thread as the app does. Built in every
// host-tests build (it must compile), run under TSan by that script: any data race is a failure.
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <thread>
#include <vector>

#include "Engine.h"
#include "Sample.h"
#include "SpscFloatRing.h"
#include "SpscQueue.h"

namespace {
constexpr int kRate = 48000;
int g_failures = 0;
#define CHECK(c) do { if (!(c)) { std::fprintf(stderr, "FAILED %s:%d: %s\n", __FILE__, __LINE__, #c); ++g_failures; } } while (0)

void queueAndRing() {
    ps::SpscQueue<int, 256> q;
    constexpr int kItems = 200000;
    std::thread producer([&] { for (int i = 0; i < kItems; ++i) while (!q.push(i)) std::this_thread::yield(); });
    int expected = 0, v = -1;
    while (expected < kItems) if (q.pop(v)) { CHECK(v == expected); ++expected; }
    producer.join();

    ps::SpscFloatRing ring(1024);
    std::thread writer([&] {
        std::vector<float> block(64);
        for (int n = 0; n < 4000;) {
            for (int i = 0; i < 64; ++i) block[static_cast<size_t>(i)] = float(n * 64 + i);
            if (ring.write(block.data(), 64) == 64) ++n; else std::this_thread::yield();
        }
    });
    std::vector<float> out(64);
    long next = 0;
    while (next < 4000L * 64) {
        const auto got = ring.read(out.data(), 64);
        for (size_t i = 0; i < got; ++i) { CHECK(out[i] == float(next)); ++next; }
        if (got == 0) std::this_thread::yield();
    }
    writer.join();
}

void engineThreads() {
    ps::Engine engine(kRate);
    std::atomic<bool> stop{false};
    std::vector<float> tone(static_cast<size_t>(kRate / 4));
    for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.5f * static_cast<float>(std::sin(2 * M_PI * 110.0 * double(i) / kRate));
    std::thread audio([&] {
        std::vector<float> out(256 * 2);
        while (!stop.load(std::memory_order_relaxed)) {
            engine.process(nullptr, 0, 0, out.data(), 256, 2);
            for (float s : out) if (!std::isfinite(s)) { stop = true; break; }
        }
    });
    // The UI thread: commands and readings, for about two seconds.
    std::vector<float> scope(ps::Engine::kScopeFrames);
    for (int round = 0; round < 400 && !stop.load(); ++round) {
        engine.post({ps::CommandType::SetSynthNote, 0.8f, round % 8, ps::NoteTrack::pack((round % 8) * 48, 40, 48 + round % 12)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 8});
        if (round % 50 == 0) engine.post({ps::CommandType::SetTransport, round % 100 == 0 ? 1.0f : 0.0f, 0});
        if (round % 20 == 0) engine.post({ps::CommandType::InstallSample, 0.0f, round % 4, 0, ps::Sample::create(tone.data(), tone.size(), 1, kRate, kRate)});
        // Lot D2: the remix column's passages installed, refitted and switched while they play.
        if (round % 10 == 5) engine.post({ps::CommandType::InstallRemixCell, 0.0f, (round / 10) % 3 | (round % 20 == 5 ? ps::kRefit : 0), 16,
                                          ps::Sample::create(tone.data(), tone.size(), 1, kRate, kRate)});
        if (round % 7 == 0) engine.post({ps::CommandType::SetRemixCell, 0.0f, round % 4 - 1});
        (void)engine.remixCellPlaying();
        engine.post({ps::CommandType::SetGain, 0.3f + 0.1f * float(round % 5)});
        // The app frees what the audio thread let go of on this same thread (NativeEngine: main thread).
        engine.collectGarbage();
        engine.readScope(false, scope.data());
        int64_t frame = 0, nanos = 0;
        engine.callbackClock(frame, nanos);
        (void)engine.cpuLoad();
        (void)engine.currentStep();
        (void)engine.playPosition();
        (void)engine.isPlaying();
        (void)engine.framesRendered();
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
    stop = true;
    audio.join();
    CHECK(!engine.hasNonFinite());
    CHECK(engine.callbackCount() > 10);
    engine.collectGarbage();
}
}  // namespace

int main() {
    queueAndRing();
    engineThreads();
    std::printf(g_failures == 0 ? "THREADS OK\n" : "THREADS FAILED (%d)\n", g_failures);
    return g_failures == 0 ? 0 : 1;
}
