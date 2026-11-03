// Audit stage 0: the engine's threads at the same time, for ThreadSanitizer
// (scripts/host-tests-tsan.sh). An audio thread runs the callback while a UI thread posts
// commands (notes, transport, samples installed and replaced), frees what the audio thread let go
// of and reads the measures the app reads, all on one thread as the app does. Built in every
// host-tests build (it must compile), run under TSan by that script: any data race is a failure.
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <memory>
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

// FIX-1: the lot B2 / E / 5b paths with their own reader on the UI thread: « Enregistrer ce que je
// joue » (ResampleArm, a stop naming its take, the app's finish reading what was written), the MIDI
// clock's queue drained, the Master pro reference installed and taken out, the live pads' levels
// read, clips installed and refit to a new tempo, and the song's stem edits set and committed.
void liveThreads() {
    auto owner = std::make_unique<ps::Engine>(kRate);
    ps::Engine& engine = *owner;
    std::atomic<bool> stop{false};
    std::vector<float> tone(static_cast<size_t>(kRate / 2));
    for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.4f * static_cast<float>(std::sin(2 * M_PI * 220.0 * double(i) / kRate));
    std::vector<std::vector<float>> stems(ps::StemBundle::kStems, std::vector<float>(static_cast<size_t>(kRate) * 2));
    for (auto& st : stems) for (size_t i = 0; i < st.size(); ++i) st[i] = 0.2f * static_cast<float>(std::sin(double(i) * 0.01));
    std::thread audio([&] {
        std::vector<float> out(192 * 2);
        while (!stop.load(std::memory_order_relaxed)) {
            engine.process(nullptr, 0, 0, out.data(), 192, 2);
            for (float s : out) if (!std::isfinite(s)) { stop = true; break; }
        }
    });
    {
        ps::Command stemsIn{ps::CommandType::InstallStems, 0.0f, 0};
        stemsIn.stems = ps::StemBundle::fromFloat(stems);
        engine.post(stemsIn);
    }
    for (int pad = 0; pad < 4; ++pad) {
        engine.post({ps::CommandType::InstallClip, 0.0f, pad, 0, ps::Sample::create(tone.data(), tone.size(), 1, kRate, kRate)});
        engine.post({ps::CommandType::ToggleClip, 0.0f, pad});
    }
    engine.post({ps::CommandType::SetMidiClockOut, 1.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<ps::ResampleBuffer*> zombies;  // let go by the app before the engine (as JniBridge keeps them)
    ps::ResampleBuffer* take = nullptr;
    int64_t clockEvents = 0, takesRead = 0;
    float levels = 0.0f;
    for (int round = 0; round < 400 && !stop.load(); ++round) {
        // « Enregistrer ce que je joue »: armed, stopped (naming it), read and freed as resampleFinish does.
        if (round % 40 == 0) {
            take = new ps::ResampleBuffer(kRate / 4);
            ps::Command arm{ps::CommandType::ResampleArm, 0.0f, round % 80 == 0 ? 1 : 0, 0};
            arm.resample = take;
            engine.post(arm);
        } else if (take && round % 40 == 15) {
            ps::Command end{ps::CommandType::ResampleStop, 0.0f, 0};
            end.resample = take;
            engine.post(end);
        } else if (take && round % 40 == 30) {
            ps::Command now{ps::CommandType::ResampleStop, 0.0f, 1};
            now.resample = take;
            engine.post(now);
            for (int waited = 0; waited < 200 && take->state.load(std::memory_order_acquire) != 2; ++waited)
                std::this_thread::sleep_for(std::chrono::milliseconds(1));
            if (take->state.load(std::memory_order_acquire) == 2) {
                const int64_t n = std::clamp<int64_t>(take->written.load(std::memory_order_acquire), 0, take->capacity);
                float sum = 0.0f;
                for (int64_t i = 0; i < n * 2; ++i) sum += take->data[static_cast<size_t>(i)];
                CHECK(std::isfinite(sum));
                delete take;
                ++takesRead;
            } else {
                zombies.push_back(take);
            }
            take = nullptr;
        }
        // A late stop naming a take already let go (only compared, never touched).
        if (round % 40 == 35) {
            ps::Command late{ps::CommandType::ResampleStop, 0.0f, 0};
            late.resample = reinterpret_cast<ps::ResampleBuffer*>(uintptr_t{0x10});
            engine.post(late);
        }
        // The MIDI clock's queue: one reader, here.
        ps::MidiClockEvent e;
        while (engine.popClockEvent(e)) ++clockEvents;
        if (round % 100 == 50) engine.post({ps::CommandType::SetTempo, 100.0f + float(round % 7)});
        // Master pro: a reference installed, another, then none.
        if (round % 30 == 0) {
            ps::Command ref{ps::CommandType::InstallReference, -10.0f - float(round % 4), 0, 0,
                            round % 90 == 60 ? nullptr : ps::Sample::create(tone.data(), tone.size(), 1, kRate, kRate)};
            engine.post(ref);
            engine.post({ps::CommandType::SetMasterPro, float(round % 3), 0, ps::ProListen});
        }
        // The live pads' lights.
        for (int pad = 0; pad < 8; ++pad) levels += engine.clipLevel(pad);
        // A pad's loop stretched to a new tempo, picked up where it plays.
        if (round % 25 == 10)
            engine.post({ps::CommandType::InstallClip, 0.0f, (round % 4) | ps::kRefit, 0, ps::Sample::create(tone.data(), tone.size(), 1, kRate, kRate)});
        // The song's stem edits (step 5b): pieces set aside, then committed.
        if (round % 10 == 5) {
            const int stem = round % ps::StemBundle::kStems;
            engine.post({ps::CommandType::SetStemClip, 0.0f, stem << 16 | 0 << 8 | ps::StemPlayer::ClipAt, 0});
            engine.post({ps::CommandType::SetStemClip, 0.0f, stem << 16 | 0 << 8 | ps::StemPlayer::ClipFrom, 1000 * (round % 5)});
            engine.post({ps::CommandType::SetStemClip, 0.0f, stem << 16 | 0 << 8 | ps::StemPlayer::ClipLength, kRate / 2});
            engine.post({ps::CommandType::CommitStemClips, 0.0f, stem, round % 20 == 5 ? 1 : -1});
        }
        engine.collectGarbage();
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
    stop = true;
    audio.join();
    // The engine is stopped: what it still holds is let go with it (ResampleArm left armed: state 2).
    CHECK(!engine.hasNonFinite());
    CHECK(engine.callbackCount() > 10);
    CHECK(clockEvents > 0);
    CHECK(takesRead > 0);
    CHECK(std::isfinite(levels) && levels > 0.0f);
    std::printf("  live threads: %lld clock events, %lld takes read, %zu let go late\n",
                static_cast<long long>(clockEvents), static_cast<long long>(takesRead), zombies.size());
    engine.collectGarbage();
    owner.reset();  // lets go of the take still armed (its state 2) before the app frees it
    if (take) CHECK(take->state.load() == 2);
    delete take;
    for (auto* z : zombies) delete z;
}
}  // namespace

int main() {
    queueAndRing();
    engineThreads();
    liveThreads();
    std::printf(g_failures == 0 ? "THREADS OK\n" : "THREADS FAILED (%d)\n", g_failures);
    return g_failures == 0 ? 0 : 1;
}
