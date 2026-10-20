// Host-side tests for the platform-independent audio core.
// Run with scripts/host-tests.sh; pass --update-references to regenerate golden renders.

#include <cmath>
#include <cstdio>
#include <atomic>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <functional>
#include <random>
#include <string>
#include <tuple>
#include <vector>

#include "AcidTrack.h"
#include "Analysis.h"
#include "DrumKit.h"
#include "Effects.h"
#include "Mixer.h"
#include "PlaitsSynth.h"
#include "OfflineRenderer.h"
#include "OnsetDetector.h"
#include "TimeStretch.h"
#include "SoundFontPlayer.h"
#include "Sample.h"
#include "Sampler.h"
#include "WavWriter.h"
#include "Engine.h"
#include "RoundTripMeter.h"
#include "Sequencer.h"
#include "Synth.h"
#include "SpscFloatRing.h"
#include "SpscQueue.h"
#include "StemPlayer.h"

// Counts C++ heap allocations while g_countAllocations is set (real-time rule test). Not under
// AddressSanitizer, which owns operator new.
#if !defined(__SANITIZE_ADDRESS__)
#include <new>
// Our operator new is malloc-based, so free() in operator delete matches it.
#pragma GCC diagnostic ignored "-Wmismatched-new-delete"
static std::atomic<bool> g_countAllocations{false};
static std::atomic<int> g_allocations{0};
void* operator new(std::size_t n) {
    if (g_countAllocations.load(std::memory_order_relaxed)) g_allocations.fetch_add(1, std::memory_order_relaxed);
    if (void* p = std::malloc(n ? n : 1)) return p;
    throw std::bad_alloc();
}
void operator delete(void* p) noexcept { std::free(p); }
void operator delete(void* p, std::size_t) noexcept { std::free(p); }
#define PS_COUNT_ALLOCATIONS 1
#endif

namespace {

int g_failures = 0;
bool g_updateReferences = false;

#define CHECK(cond)                                                              \
    do {                                                                         \
        if (!(cond)) {                                                           \
            std::fprintf(stderr, "  FAILED %s:%d: %s\n", __FILE__, __LINE__, #cond); \
            ++g_failures;                                                        \
        }                                                                        \
    } while (0)

constexpr int kRate = 48000;
constexpr int kChannels = 2;

// Four bars of 4/4 at 120 BPM.
constexpr int kFourBarsFrames = kRate * 8;

std::vector<float> render(ps::Engine& engine, int frames, int blockSize) {
    std::vector<float> out(static_cast<size_t>(frames) * kChannels, 0.0f);
    for (int pos = 0; pos < frames; pos += blockSize) {
        const int n = std::min(blockSize, frames - pos);
        engine.process(nullptr, 0, 0, out.data() + static_cast<size_t>(pos) * kChannels, n, kChannels);
    }
    return out;
}

void writeWav(const std::string& path, const std::vector<float>& interleaved) {
    std::filesystem::create_directories(std::filesystem::path(path).parent_path());
    std::ofstream f(path, std::ios::binary);
    auto u32 = [&](uint32_t v) { f.write(reinterpret_cast<const char*>(&v), 4); };
    auto u16 = [&](uint16_t v) { f.write(reinterpret_cast<const char*>(&v), 2); };
    const uint32_t dataBytes = static_cast<uint32_t>(interleaved.size() * sizeof(float));
    f.write("RIFF", 4); u32(36 + dataBytes); f.write("WAVE", 4);
    f.write("fmt ", 4); u32(16); u16(3); u16(kChannels); u32(kRate);
    u32(kRate * kChannels * 4); u16(kChannels * 4); u16(32);
    f.write("data", 4); u32(dataBytes);
    f.write(reinterpret_cast<const char*>(interleaved.data()), dataBytes);
}

void testSpscQueue() {
    ps::SpscQueue<int, 8> q;
    for (int i = 0; i < 8; ++i) CHECK(q.push(i));
    CHECK(!q.push(99));
    int v = -1;
    for (int i = 0; i < 8; ++i) {
        CHECK(q.pop(v));
        CHECK(v == i);
    }
    CHECK(!q.pop(v));
}

void testSpscFloatRing() {
    ps::SpscFloatRing ring(8);
    const float stereo[] = {1, -1, 2, -2, 3, -3, 4, -4, 5, -5};
    CHECK(ring.write(stereo, 5, 2) == 5);  // channel 0 only
    float out[16] = {};
    CHECK(ring.read(out, 3) == 3);
    CHECK(out[0] == 1 && out[1] == 2 && out[2] == 3);
    const float more[] = {6, 7, 8, 9, 10, 11, 12};
    CHECK(ring.write(more, 7) == 6);  // 2 still buffered, capacity 8
    CHECK(ring.available() == 8);
    CHECK(ring.read(out, 16) == 8);
    CHECK(out[0] == 4 && out[2] == 6 && out[7] == 11);
    ring.write(more, 3);
    ring.discard();
    CHECK(ring.available() == 0);
}

void testSilentWhenStopped() {
    ps::Engine engine(kRate);
    const auto out = render(engine, kRate / 10, 256);
    float peak = 0.0f;
    for (float s : out) peak = std::max(peak, std::fabs(s));
    CHECK(peak == 0.0f);
}

void testSineFourBars() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 440.0f});
    engine.post({ps::CommandType::SetGain, 0.5f});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    const auto out = render(engine, kFourBarsFrames, 256);

    bool finite = true;
    float peak = 0.0f;
    for (float s : out) {
        finite &= std::isfinite(s);
        peak = std::max(peak, std::fabs(s));
    }
    CHECK(finite);
    CHECK(peak <= 0.5001f);
    CHECK(peak > 0.49f);
    CHECK(!engine.hasNonFinite());

    // Count rising zero crossings over the last 7 seconds (after the frequency glide).
    int crossings = 0;
    for (int i = kRate + 1; i < kFourBarsFrames; ++i) {
        const float a = out[static_cast<size_t>(i - 1) * kChannels];
        const float b = out[static_cast<size_t>(i) * kChannels];
        if (a < 0.0f && b >= 0.0f) ++crossings;
    }
    const double measured = crossings / 7.0;
    CHECK(std::fabs(measured - 440.0) < 0.5);

    CHECK(engine.framesRendered() == static_cast<uint64_t>(kFourBarsFrames));
    writeWav(std::string(PS_OUTPUT_DIR) + "/sine_four_bars.wav", out);
}

void testBlockSizeIndependence() {
    auto run = [](int block) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetFrequency, 1000.0f});
        engine.post({ps::CommandType::SetPlaying, 1.0f});
        return render(engine, kRate / 2, block);
    };
    const auto a = run(64);
    const auto b = run(256);
    const auto c = run(997);
    float maxDiff = 0.0f;
    for (size_t i = 0; i < a.size(); ++i) {
        maxDiff = std::max(maxDiff, std::fabs(a[i] - b[i]));
        maxDiff = std::max(maxDiff, std::fabs(a[i] - c[i]));
    }
    CHECK(maxDiff < 1e-6f);
}

// Largest sample-to-sample step of a full-scale sine at [hz]: anything bigger is a click.
float naturalStep(float hz) { return static_cast<float>(2.0 * 3.14159265358979 * hz / kRate); }

// Below the master soft clipper's threshold, so the tone itself is measured.
constexpr float kRampGain = 0.75f;

void testStartRampsUp() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 1000.0f});
    engine.post({ps::CommandType::SetGain, kRampGain});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    const auto out = render(engine, kRate / 10, 256);
    // 1 ms after start the 10 ms ramp is still below 20 %; a hard start would already be at full scale.
    float firstMs = 0.0f;
    for (int i = 0; i < kRate / 1000; ++i) firstMs = std::max(firstMs, std::fabs(out[static_cast<size_t>(i) * kChannels]));
    CHECK(firstMs < 0.2f * kRampGain);
    float lastMs = 0.0f;
    for (int i = kRate / 10 - kRate / 1000; i < kRate / 10; ++i) {
        lastMs = std::max(lastMs, std::fabs(out[static_cast<size_t>(i) * kChannels]));
    }
    CHECK(lastMs > 0.99f * kRampGain);
    float maxStep = 0.0f;
    for (int i = 1; i < kRate / 10; ++i) {
        maxStep = std::max(maxStep, std::fabs(out[static_cast<size_t>(i) * kChannels] -
                                              out[static_cast<size_t>(i - 1) * kChannels]));
    }
    CHECK(maxStep <= naturalStep(1000.0f) * kRampGain * 1.01f);
}

void testStopFadesOut() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 1000.0f});
    engine.post({ps::CommandType::SetGain, kRampGain});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    // Advance until the waveform sits near a peak, where a hard cut would click loudly.
    auto before = render(engine, 12000, 256);
    for (int i = 0; i < 48 && std::fabs(before[before.size() - kChannels]) < 0.95f * kRampGain; ++i) before = render(engine, 1, 1);
    CHECK(std::fabs(before[before.size() - kChannels]) >= 0.95f * kRampGain);
    engine.post({ps::CommandType::SetPlaying, 0.0f});
    const auto out = render(engine, kRate / 4, 256);
    float previous = before[before.size() - kChannels];
    float maxStep = 0.0f;
    for (size_t i = 0; i < out.size() / kChannels; ++i) {
        maxStep = std::max(maxStep, std::fabs(out[i * kChannels] - previous));
        previous = out[i * kChannels];
    }
    CHECK(maxStep <= naturalStep(1000.0f) * kRampGain * 1.01f);
    // Still sounding 1 ms after the stop (fading, not cut)...
    float firstMs = 0.0f;
    for (int i = 0; i < kRate / 1000; ++i) firstMs = std::max(firstMs, std::fabs(out[static_cast<size_t>(i) * kChannels]));
    CHECK(firstMs > 0.7f * kRampGain);
    // ...and silent at the end.
    float tail = 0.0f;
    for (size_t i = out.size() - 200; i < out.size(); ++i) tail = std::max(tail, std::fabs(out[i]));
    CHECK(tail < 1e-4f);
}

void testSettingsSurvivePrepare() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 1000.0f});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    render(engine, 256, 256);
    engine.post({ps::CommandType::StartLatencyTest, 0.0f});  // stale request, must not replay
    engine.prepare(kRate);                                    // e.g. stream reopened after unplugging headphones
    CHECK(engine.latencyMeter().state() == ps::RoundTripMeter::State::Idle);
    const auto out = render(engine, kRate, 256);
    CHECK(engine.latencyMeter().state() == ps::RoundTripMeter::State::Idle);
    int crossings = 0;
    for (int i = kRate / 2 + 1; i < kRate; ++i) {
        if (out[static_cast<size_t>(i - 1) * kChannels] < 0.0f && out[static_cast<size_t>(i) * kChannels] >= 0.0f) ++crossings;
    }
    CHECK(std::abs(crossings - 500) <= 1);
}

void testReferenceRender() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 220.0f});
    engine.post({ps::CommandType::SetGain, 0.8f});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    const auto out = render(engine, kRate / 4, 192);
    const std::string path = std::string(PS_REFERENCE_DIR) + "/sine220.f32";
    if (g_updateReferences) {
        std::filesystem::create_directories(PS_REFERENCE_DIR);
        std::ofstream f(path, std::ios::binary);
        f.write(reinterpret_cast<const char*>(out.data()), static_cast<std::streamsize>(out.size() * sizeof(float)));
        std::printf("  reference updated: %s\n", path.c_str());
        return;
    }
    std::ifstream f(path, std::ios::binary);
    CHECK(f.good());
    if (!f.good()) return;
    std::vector<float> ref(out.size());
    f.read(reinterpret_cast<char*>(ref.data()), static_cast<std::streamsize>(ref.size() * sizeof(float)));
    CHECK(static_cast<size_t>(f.gcount()) == ref.size() * sizeof(float));
    float maxDiff = 0.0f;
    for (size_t i = 0; i < ref.size(); ++i) maxDiff = std::max(maxDiff, std::fabs(ref[i] - out[i]));
    CHECK(maxDiff < 1e-4f);
}

// Simulates the speaker -> microphone path with a delay, attenuation and noise.
// The microphone delivers its frames through a FIFO in irregular chunks (sometimes none,
// sometimes a backlog), like a real input stream read without blocking.
ps::RoundTripMeter::Result simulateRoundTrip(int delayFrames, float gain, float noise, int blockSize,
                                             bool irregular = false, bool micDead = false) {
    ps::RoundTripMeter meter;
    meter.prepare(kRate);
    meter.start();
    std::vector<float> mic;  // everything the microphone has captured so far
    std::vector<float> pending(static_cast<size_t>(delayFrames), 0.0f);  // sound still travelling
    std::mt19937 rng(42);
    std::normal_distribution<float> dist(0.0f, noise);
    std::uniform_int_distribution<int> chunk(0, 2 * blockSize);
    std::vector<float> out(static_cast<size_t>(blockSize) * kChannels);
    std::vector<float> in(static_cast<size_t>(4 * blockSize));
    size_t readPos = 0;
    int guard = 0;
    while (meter.state() == ps::RoundTripMeter::State::Running && ++guard < 100000) {
        size_t available = mic.size() - readPos;
        size_t want = irregular ? static_cast<size_t>(chunk(rng)) : static_cast<size_t>(blockSize);
        size_t n = std::min({available, want, in.size()});
        for (size_t i = 0; i < n; ++i) in[i] = mic[readPos + i];
        readPos += n;
        meter.process(micDead ? nullptr : in.data(), micDead ? 0 : static_cast<int>(n), 1,
                      out.data(), blockSize, kChannels);
        // Speaker output reaches the microphone delayFrames later.
        for (int i = 0; i < blockSize; ++i) {
            pending.push_back(gain * out[static_cast<size_t>(i) * kChannels]);
            mic.push_back(pending.front() + dist(rng));
            pending.erase(pending.begin());
        }
    }
    CHECK(meter.state() == ps::RoundTripMeter::State::Done);
    return meter.analyze();
}

void testRoundTripMeter() {
    const auto r = simulateRoundTrip(1234, 0.05f, 0.01f, 192);
    CHECK(r.valid);
    CHECK(std::abs(r.latencyFrames - 1234) <= 1);
    CHECK(std::fabs(r.latencyMs - 1234.0 * 1000.0 / kRate) < 0.05);
    CHECK(r.failure == ps::RoundTripMeter::Failure::None);

    const auto r2 = simulateRoundTrip(480, 0.3f, 0.0f, 256);
    CHECK(r2.valid);
    CHECK(r2.latencyFrames == 480);

    // Real phones: input arrives in uneven bursts. The measurement must not drift.
    const auto bursty = simulateRoundTrip(900, 0.02f, 0.005f, 96, true);
    CHECK(bursty.valid);
    CHECK(std::abs(bursty.latencyFrames - 900) <= 1);

    const auto quiet = simulateRoundTrip(1000, 0.0f, 0.01f, 256);
    CHECK(!quiet.valid);
    CHECK(quiet.failure == ps::RoundTripMeter::Failure::LowConfidence);

    const auto silent = simulateRoundTrip(1000, 0.0f, 0.0f, 256);
    CHECK(!silent.valid);
    CHECK(silent.failure == ps::RoundTripMeter::Failure::SilentInput);

    const auto dead = simulateRoundTrip(1000, 0.3f, 0.0f, 256, false, true);
    CHECK(!dead.valid);
    CHECK(dead.failure == ps::RoundTripMeter::Failure::NoInput);
    CHECK(dead.inputFrames == 0);
}

// Full path through Engine::process: speaker -> simulated room (delay + noise) -> microphone.
void testLatencyTestThroughEngine() {
    constexpr int kDelay = 777;
    constexpr int kBlock = 192;
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 1000.0f});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    engine.post({ps::CommandType::StartLatencyTest, 0.0f});
    std::vector<float> out(kBlock * kChannels);
    std::vector<float> in(kBlock, 0.0f);
    std::vector<float> line(static_cast<size_t>(kRate) * 4, 0.0f);
    std::mt19937 rng(7);
    std::normal_distribution<float> noise(0.0f, 0.005f);
    int pos = 0;
    bool started = false;
    while (pos + kBlock + kDelay < static_cast<int>(line.size())) {
        for (int i = 0; i < kBlock; ++i) in[static_cast<size_t>(i)] = line[static_cast<size_t>(pos + i)] + noise(rng);
        engine.process(in.data(), kBlock, 1, out.data(), kBlock, kChannels);
        for (int i = 0; i < kBlock; ++i) line[static_cast<size_t>(pos + i + kDelay)] += 0.1f * out[static_cast<size_t>(i) * kChannels];
        pos += kBlock;
        if (engine.latencyMeter().state() == ps::RoundTripMeter::State::Running) started = true;
        if (started && engine.latencyMeter().state() == ps::RoundTripMeter::State::Done) break;
    }
    CHECK(started);
    CHECK(engine.latencyMeter().state() == ps::RoundTripMeter::State::Done);
    const auto r = engine.latencyMeter().analyze();
    CHECK(r.valid);
    CHECK(std::abs(r.latencyFrames - kDelay) <= 1);

    // The test tone comes back once the measurement is over.
    const auto after = render(engine, kRate / 2, 256);
    float peak = 0.0f;
    for (size_t i = after.size() / 2; i < after.size(); ++i) peak = std::max(peak, std::fabs(after[i]));
    CHECK(peak > 0.45f);
}


// Absolute frame of every step event over `frames` frames, rendered in blocks of `block`.
std::vector<std::pair<int64_t, int>> collectSteps(ps::Sequencer& seq, int frames, int block,
                                                  const std::function<void(int64_t, ps::Sequencer&)>& midway = {}) {
    std::vector<std::pair<int64_t, int>> events;
    for (int64_t pos = 0; pos < frames; pos += block) {
        const int n = static_cast<int>(std::min<int64_t>(block, frames - pos));
        if (midway) midway(pos, seq);
        seq.advance(n, [&](int offset, int step) { events.push_back({pos + offset, step}); });
    }
    return events;
}

void testSequencerTiming() {
    for (int block : {256, 97, 1024}) {
        ps::Sequencer seq;
        seq.prepare(kRate);
        seq.setTempo(120.0);  // 16th = 6000 samples at 48 kHz
        seq.start(0);
        const auto events = collectSteps(seq, kFourBarsFrames, block);
        CHECK(events.size() == 64);
        bool exact = true;
        for (size_t i = 0; i < events.size(); ++i) {
            exact &= events[i].first == static_cast<int64_t>(i) * 6000 && events[i].second == static_cast<int>(i);
        }
        CHECK(exact);
    }
}

void testSequencerSwing() {
    ps::Sequencer seq;
    seq.prepare(kRate);
    seq.setTempo(120.0);
    seq.setSwing(0.5);
    seq.start(0);
    const auto events = collectSteps(seq, 6000 * 16, 128);
    CHECK(events.size() == 16);
    bool ok = true;
    for (size_t i = 0; i < events.size(); ++i) {
        const int64_t expected = static_cast<int64_t>(i) * 6000 + ((i % 2) ? 3000 : 0);
        ok &= events[i].first == expected;
    }
    CHECK(ok);
}

void testSequencerTempoChangeKeepsGroove() {
    ps::Sequencer seq;
    seq.prepare(kRate);
    seq.setTempo(120.0);
    seq.start(0);
    // Halve the tempo once the 8th step has played (frame 48000 > step 7 at 42000).
    const auto events = collectSteps(seq, 200000, 100, [](int64_t pos, ps::Sequencer& s) {
        if (pos == 45000) s.setTempo(60.0);
    });
    CHECK(events.size() > 12);
    // Steps 0..7 every 6000; step 8 was already scheduled on the old grid (48000), then 12000 apart.
    CHECK(events[7].first == 42000);
    CHECK(events[8].first == 48000);
    CHECK(events[9].first == 60000);
    CHECK(events[10].first == 72000);
}

void testCountInAndPatternTriggers() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    for (int step : {0, 4, 8, 12}) engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
    engine.post({ps::CommandType::SetDrumHit, 0.8f, 2, 6});
    engine.post({ps::CommandType::SetTransport, 1.0f, 1});  // one bar of count-in
    // One bar of count-in (16 x 6000) then exactly two pattern bars.
    render(engine, 6000 * 16 * 3, 256);
    CHECK(engine.metronomeClicks() == 4);  // count-in only: metronome is off
    CHECK(engine.laneTriggerCount(0) == 8);
    CHECK(engine.laneTriggerCount(2) == 2);
    CHECK(engine.laneTriggerCount(1) == 0);
    CHECK(engine.currentStep() == 15);

    // Removing a hit and stopping.
    engine.post({ps::CommandType::SetDrumHit, 0.0f, 0, 4});
    render(engine, 6000 * 16, 256);
    CHECK(engine.laneTriggerCount(0) == 11);
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 6000 * 16, 256);
    CHECK(engine.laneTriggerCount(0) == 11);
    CHECK(engine.currentStep() == -1);
}

void testMetronomeIsSampleAccurate() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetMetronome, 1.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto out = render(engine, kRate, 333);
    // Quarter notes at 120 BPM = every 24000 samples; a click starts exactly there.
    auto firstSoundAfter = [&](int from) {
        for (int i = from; i < kRate; ++i) {
            if (std::fabs(out[static_cast<size_t>(i) * kChannels]) > 1e-6f) return i;
        }
        return -1;
    };
    // sin(0) = 0 at the onset, so the first non-zero sample is the one right after.
    CHECK(firstSoundAfter(0) == 1);
    CHECK(firstSoundAfter(20000) == 24001);
    float peak = 0.0f;
    for (float v : out) peak = std::max(peak, std::fabs(v));
    CHECK(peak > 0.3f && peak <= 0.5f);
}


std::vector<float> renderKit(ps::DrumKit& kit, int frames, int block,
                             const std::vector<std::tuple<int, int, float>>& hits) {  // frame, voice, velocity
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    for (int pos = 0; pos < frames; pos += block) {
        const int n = std::min(block, frames - pos);
        for (const auto& [frame, voice, velocity] : hits) {
            if (frame >= pos && frame < pos + n) kit.trigger(voice, velocity, frame - pos);
        }
        kit.render(out.data() + pos, n);
    }
    return out;
}

double energy(const std::vector<float>& v, int from, int to) {
    double e = 0.0;
    for (int i = from; i < to; ++i) e += static_cast<double>(v[static_cast<size_t>(i)]) * v[static_cast<size_t>(i)];
    return e;
}

void testEveryDrumVoiceSoundsAndDecays() {
    for (int voice = 0; voice < ps::DrumKit::kVoices; ++voice) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        const auto out = renderKit(kit, kRate * 3, 256, {{1000, voice, 1.0f}});
        bool silentBefore = true;
        for (int i = 0; i < 1000; ++i) silentBefore &= out[static_cast<size_t>(i)] == 0.0f;
        float peak = 0.0f;
        bool finite = true;
        for (float v : out) {
            finite &= std::isfinite(v);
            peak = std::max(peak, std::fabs(v));
        }
        float tail = 0.0f;
        for (int i = kRate * 3 - 4800; i < kRate * 3; ++i) tail = std::max(tail, std::fabs(out[static_cast<size_t>(i)]));
        const bool ok = silentBefore && finite && peak > 0.05f && peak < 1.5f && tail < 0.01f;
        if (!ok) std::fprintf(stderr, "  voice %d: silentBefore=%d finite=%d peak=%.3f tail=%.4f\n", voice, silentBefore, finite, peak, tail);
        CHECK(ok);
    }
}

void testVelocityChangesLoudness() {
    for (int voice : {ps::DrumKit::Kick, ps::DrumKit::Snare, ps::DrumKit::Clap, ps::DrumKit::ClosedHat}) {
        ps::DrumKit soft, loud;
        soft.prepare(kRate);
        loud.prepare(kRate);
        const auto a = renderKit(soft, kRate / 2, 256, {{0, voice, 0.2f}});
        const auto b = renderKit(loud, kRate / 2, 256, {{0, voice, 1.0f}});
        CHECK(energy(b, 0, kRate / 2) > 1.5 * energy(a, 0, kRate / 2));
    }
}

void testClosedHatChokesOpenHat() {
    ps::DrumKit alone, choked;
    alone.prepare(kRate);
    choked.prepare(kRate);
    const auto a = renderKit(alone, kRate, 256, {{0, ps::DrumKit::OpenHat, 1.0f}});
    const auto b = renderKit(choked, kRate, 256, {{0, ps::DrumKit::OpenHat, 1.0f}, {4800, ps::DrumKit::ClosedHat, 1.0f}});
    // 150 ms after the closed hat, the open hat must be gone (the closed one is short).
    CHECK(energy(b, 12000, 24000) < 0.2 * energy(a, 12000, 24000));
    // Before the choke both are the same sound.
    CHECK(energy(a, 0, 4800) > 0.0);
}

void testPatternDrivesDrums() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, 4});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto out = render(engine, 6000 * 8, 200);
    // Nothing until step 4 (frame 24000), then the kick.
    int first = -1;
    for (int i = 0; i < 6000 * 8 && first < 0; ++i) {
        if (out[static_cast<size_t>(i) * kChannels] != 0.0f) first = i;
    }
    CHECK(first >= 24000 && first <= 24001);
    float peak = 0.0f;
    for (float v : out) peak = std::max(peak, std::fabs(v));
    CHECK(peak > 0.1f && peak < 1.5f);
}

void testFullKitCpuBudget() {
    // Worst case: all 16 voices on every step of 4 bars at 160 BPM.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 160.0f});
    for (int lane = 0; lane < 16; ++lane) {
        for (int step = 0; step < 16; ++step) engine.post({ps::CommandType::SetDrumHit, 0.8f, lane, step});
    }
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const int frames = kRate * 6;
    const auto start = std::chrono::steady_clock::now();
    const auto out = render(engine, frames, 256);
    const double seconds = std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count();
    const double load = seconds / (static_cast<double>(frames) / kRate);
    std::printf("  full kit: %.1f %% of real time on this machine\n", load * 100.0);
#if defined(__SANITIZE_ADDRESS__)
    (void)load;  // sanitizer builds are several times slower: timing is meaningless there
#else
    CHECK(load < 0.10);  // leaves a large margin for a phone and for synths/effects
#endif
    bool finite = true;
    float peak = 0.0f;
    for (float v : out) {
        finite &= std::isfinite(v);
        peak = std::max(peak, std::fabs(v));
    }
    CHECK(finite);
    CHECK(peak <= 1.0f);  // the master soft clipper never lets the output clip
}

void testKicksArePunchy() {
    for (int voice : {ps::DrumKit::Kick, ps::DrumKit::Kick2}) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        const auto out = renderKit(kit, kRate / 2, 256, {{0, voice, 1.0f}});
        const double rms = std::sqrt(energy(out, 0, kRate * 3 / 10) / (kRate * 3 / 10));
        float peak = 0.0f;
        for (float v : out) peak = std::max(peak, std::fabs(v));
        if (rms < 0.25 || peak < 0.8f) std::fprintf(stderr, "  kick %d: rms=%.3f peak=%.3f\n", voice, rms, peak);
        CHECK(rms >= 0.25);
        CHECK(peak >= 0.8f);
    }
}

void testTomsHaveBody() {
    for (int voice : {ps::DrumKit::LowTom, ps::DrumKit::MidTom, ps::DrumKit::HighTom, ps::DrumKit::Conga}) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        const auto out = renderKit(kit, kRate / 2, 256, {{0, voice, 1.0f}});
        const double rms = std::sqrt(energy(out, 0, kRate / 10) / (kRate / 10));
        std::printf("  voice %d: rms(100 ms) %.3f\n", voice, rms);
        CHECK(rms >= 0.12);
    }
    for (int voice : {ps::DrumKit::Kick, ps::DrumKit::Kick2}) {
        ps::DrumKit kit;
        kit.prepare(kRate);
        const auto out = renderKit(kit, kRate / 2, 256, {{0, voice, 1.0f}});
        std::printf("  kick %d: rms(300 ms) %.3f\n", voice, std::sqrt(energy(out, 0, kRate * 3 / 10) / (kRate * 3 / 10)));
    }
}


// Renders the acid track alone, driven by a 120 BPM clock (6000 frames per 16th).
std::vector<float> renderAcid(ps::AcidTrack& acid, int steps, int block = 256) {
    ps::Sequencer seq;
    seq.prepare(kRate);
    seq.setTempo(120.0);
    seq.start(0);
    const int frames = steps * 6000;
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    for (int pos = 0; pos < frames; pos += block) {
        const int n = std::min(block, frames - pos);
        seq.advance(n, [&](int offset, int step) { acid.onStep(offset, step, seq.samplesPerStep()); });
        acid.render(out.data() + pos, n);
    }
    return out;
}

ps::AcidStep note(int n, bool accent = false, bool slide = false) {
    ps::AcidStep s;
    s.note = n;
    s.accent = accent;
    s.slide = slide;
    s.gate = true;
    return s;
}

void testAcidStepPacking() {
    for (int n = 0; n <= 12; ++n) {
        for (int o = -1; o <= 2; ++o) {
            ps::AcidStep s{n, o, (n & 1) != 0, (n & 2) != 0, true};
            const auto u = ps::AcidStep::unpack(s.pack());
            CHECK(u.note == n && u.octave == o && u.accent == s.accent && u.slide == s.slide && u.gate);
        }
    }
    CHECK(!ps::AcidStep::unpack(0).gate);
}

void testAcidNotesStartOnTheStep() {
    ps::AcidTrack acid;
    acid.prepare(kRate);
    acid.setLength(16);
    acid.setStep(4, note(0));
    const auto out = renderAcid(acid, 8, 333);
    int first = -1;
    for (int i = 0; i < static_cast<int>(out.size()) && first < 0; ++i) {
        if (std::fabs(out[static_cast<size_t>(i)]) > 1e-6f) first = i;
    }
    CHECK(first >= 24000 && first < 24000 + 48);  // within 1 ms of step 4
    CHECK(acid.notesPlayed() == 1);
    bool finite = true;
    float peak = 0.0f;
    for (float v : out) {
        finite &= std::isfinite(v);
        peak = std::max(peak, std::fabs(v));
    }
    CHECK(finite);
    CHECK(peak > 0.1f && peak < 1.0f);
}

void testAcidAccentIsLouder() {
    // Measured on the voice itself (no saturation), as energy over the note.
    auto rmsOf = [](bool accent) {
        ps::AcidTrack acid;
        acid.prepare(kRate);
        acid.setParam(ps::AcidTrack::Drive, 0.0f);
        acid.setStep(0, note(0, accent));
        const auto out = renderAcid(acid, 1);
        double e = 0.0;
        for (float v : out) e += static_cast<double>(v) * v;
        return std::sqrt(e / static_cast<double>(out.size()));
    };
    const double plain = rmsOf(false);
    const double accented = rmsOf(true);
    std::printf("  acid rms: plain %.3f, accent %.3f\n", plain, accented);
    CHECK(accented > plain * 1.15);
}

void testAcidSlideHoldsTheNote() {
    // Without slide the note is released after half a step: just before step 1 it is fading.
    // With slide the gate stays open and the pitch glides: the level stays up.
    auto levelBeforeStep1 = [](bool slide) {
        ps::AcidTrack acid;
        acid.prepare(kRate);
        acid.setParam(ps::AcidTrack::Decay, 1.0f);
        acid.setStep(0, note(0, false, slide));
        acid.setStep(1, note(12));
        const auto out = renderAcid(acid, 2);
        double e = 0.0;
        for (int i = 5000; i < 6000; ++i) e += out[static_cast<size_t>(i)] * out[static_cast<size_t>(i)];
        return std::sqrt(e / 1000.0);
    };
    const double plain = levelBeforeStep1(false);
    const double slid = levelBeforeStep1(true);
    std::printf("  acid level before step 2: no slide %.4f, slide %.4f\n", plain, slid);
    CHECK(slid > 2.0 * plain);

    // A slid pair counts two notes but only the first one retriggers the envelope.
    ps::AcidTrack acid;
    acid.prepare(kRate);
    acid.setStep(0, note(0, false, true));
    acid.setStep(1, note(7));
    renderAcid(acid, 2);
    CHECK(acid.notesPlayed() == 2);
}

void testAcidStopsCleanly() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    for (int i = 0; i < 16; ++i) engine.post({ps::CommandType::SetAcidStep, 0.0f, i, note(i % 3 * 5, i % 4 == 0, i % 5 == 0).pack()});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 16, 256);
    CHECK(engine.acidNotesPlayed() == 16);
    engine.post({ps::CommandType::SetTransport, 0.0f});
    const auto after = render(engine, kRate, 256);
    float tail = 0.0f;
    for (size_t i = after.size() - 4800; i < after.size(); ++i) tail = std::max(tail, std::fabs(after[i]));
    CHECK(tail < 1e-3f);  // silent half a second after stop
}

void testAcidCpuBudget() {
    ps::AcidTrack acid;
    acid.prepare(kRate);
    for (int i = 0; i < 16; ++i) acid.setStep(i, note(i % 12, i % 3 == 0, i % 4 == 0));
    const auto start = std::chrono::steady_clock::now();
    renderAcid(acid, 64);
    const double seconds = std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count();
    const double load = seconds / (64 * 6000.0 / kRate);
    std::printf("  acid voice: %.1f %% of real time on this machine\n", load * 100.0);
#if !defined(__SANITIZE_ADDRESS__)
    CHECK(load < 0.05);
#endif
}


std::vector<float> renderSynth(ps::Synth& synth, int frames, int block = 256) {
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    for (int pos = 0; pos < frames; pos += block) synth.render(out.data() + pos, std::min(block, frames - pos));
    return out;
}

void openFilter(ps::Synth& s) {
    s.setParam(ps::Synth::Cutoff, 1.0f);
    s.setParam(ps::Synth::Resonance, 0.0f);
    s.setParam(ps::Synth::FilterEnvAmount, 0.0f);
    s.setParam(ps::Synth::SubLevel, 0.0f);
}

void testSynthPitchIsRight() {
    ps::Synth synth;
    synth.prepare(kRate);
    openFilter(synth);
    synth.setParam(ps::Synth::Osc1Wave, 1.0f);  // triangle: clean zero crossings
    synth.setParam(ps::Synth::Osc2Wave, 1.0f);
    synth.setParam(ps::Synth::Osc2Detune, 0.0f);
    synth.noteOn(69, 1.0f);  // A4
    const auto out = renderSynth(synth, kRate);
    int crossings = 0;
    for (int i = kRate / 2 + 1; i < kRate; ++i) {
        if (out[static_cast<size_t>(i - 1)] < 0.0f && out[static_cast<size_t>(i)] >= 0.0f) ++crossings;
    }
    std::printf("  synth A4: %d Hz\n", crossings * 2);
    CHECK(std::abs(crossings * 2 - 440) <= 4);
}

void testSynthNoteStartsOnTimeAndReleases() {
    ps::Synth synth;
    synth.prepare(kRate);
    synth.setParam(ps::Synth::AmpRelease, 0.2f);
    synth.noteOn(60, 0.8f, 100);
    synth.noteOff(60, 12000);
    const auto out = renderSynth(synth, kRate, 333);
    bool silentBefore = true;
    for (int i = 0; i < 100; ++i) silentBefore &= out[static_cast<size_t>(i)] == 0.0f;
    CHECK(silentBefore);
    float during = 0.0f;
    for (int i = 2000; i < 12000; ++i) during = std::max(during, std::fabs(out[static_cast<size_t>(i)]));
    std::printf("  synth single note peak %.3f\n", during);
    CHECK(during > 0.15f);
    float tail = 0.0f;
    for (int i = kRate - 4800; i < kRate; ++i) tail = std::max(tail, std::fabs(out[static_cast<size_t>(i)]));
    CHECK(tail < 1e-3f);
    CHECK(synth.activeVoices() == 0);
}

void testSynthVoiceStealingAndCpu() {
    ps::Synth synth;
    synth.prepare(kRate);
    synth.setParam(ps::Synth::Resonance, 0.9f);
    synth.setParam(ps::Synth::NoiseLevel, 0.3f);
    synth.setParam(ps::Synth::Lfo1ToPitch, 0.5f);
    synth.setParam(ps::Synth::Lfo2ToCutoff, 0.5f);
    for (int n = 0; n < 10; ++n) synth.noteOn(48 + n * 3, 1.0f);
    const auto start = std::chrono::steady_clock::now();
    const auto out = renderSynth(synth, kRate * 2);
    const double load = std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count() / 2.0;
    std::printf("  synth 8 voices: %.1f %% of real time on this machine\n", load * 100.0);
    CHECK(synth.activeVoices() == ps::Synth::kVoices);
    bool finite = true;
    float peak = 0.0f;
    for (float v : out) {
        finite &= std::isfinite(v);
        peak = std::max(peak, std::fabs(v));
    }
    CHECK(finite);
    CHECK(peak < 4.0f);  // a loud 8-note cluster; the engine's master soft clip tames it
#if !defined(__SANITIZE_ADDRESS__)
    CHECK(load < 0.15);
#endif
}

void testSynthThroughEngine() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SynthNoteOn, 0.9f, 64});
    render(engine, kRate / 4, 256);
    CHECK(engine.synthActiveVoices() == 1);
    engine.post({ps::CommandType::SynthAllOff, 0.0f});
    const auto out = render(engine, kRate * 3, 256);
    CHECK(engine.synthActiveVoices() == 0);
    float tail = 0.0f;
    for (size_t i = out.size() - 4800; i < out.size(); ++i) tail = std::max(tail, std::fabs(out[i]));
    CHECK(tail < 1e-3f);
}


// Feeds constant-level mono sources through the mixer and returns interleaved stereo.
std::vector<float> mixConstant(ps::Mixer& mixer, float drums, float acid, float synth, int frames = 9600) {
    std::vector<float> a(static_cast<size_t>(frames), drums), b(static_cast<size_t>(frames), acid),
        c(static_cast<size_t>(frames), synth), d(static_cast<size_t>(frames), 0.0f), out(static_cast<size_t>(frames) * 2);
    const float* ch[ps::Mixer::kChannels] = {a.data(), b.data(), c.data(), d.data()};
    mixer.process(ch, nullptr, out.data(), frames, 2);
    return out;
}

void testMixerPanMuteSolo() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    auto last = [](const std::vector<float>& v, int side) { return v[v.size() - 2 + static_cast<size_t>(side)]; };

    auto centre = mixConstant(mixer, 0.2f, 0.0f, 0.0f);
    CHECK(std::fabs(last(centre, 0) - 0.2f) < 1e-3f && std::fabs(last(centre, 1) - 0.2f) < 1e-3f);  // unity at centre

    mixer.setPan(0, -1.0f);
    auto left = mixConstant(mixer, 0.2f, 0.0f, 0.0f);
    CHECK(last(left, 1) < 1e-3f && last(left, 0) > 0.25f);
    mixer.setPan(0, 0.0f);

    mixer.setMute(1, true);
    auto muted = mixConstant(mixer, 0.0f, 0.3f, 0.0f);
    CHECK(std::fabs(last(muted, 0)) < 1e-4f);
    mixer.setMute(1, false);

    mixer.setSolo(2, true);
    auto solo = mixConstant(mixer, 0.2f, 0.2f, 0.1f);
    CHECK(std::fabs(last(solo, 0) - 0.1f) < 1e-3f);  // only the synth
    mixer.setSolo(2, false);

    mixer.setVolume(0, 0.4f);  // half the unity fader -> -12 dB with the quadratic law
    auto quiet = mixConstant(mixer, 0.4f, 0.0f, 0.0f);
    CHECK(std::fabs(last(quiet, 0) - 0.1f) < 1e-3f);
}

std::vector<uint8_t> readFile(const std::string& path) {
    std::ifstream in(path, std::ios::binary);
    return std::vector<uint8_t>((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
}

uint32_t le32(const uint8_t* p) { return p[0] | (p[1] << 8) | (p[2] << 16) | (uint32_t(p[3]) << 24); }

int32_t sample24(const std::vector<uint8_t>& f, size_t index) {
    const uint8_t* p = f.data() + 44 + index * 3;
    int32_t v = p[0] | (p[1] << 8) | (p[2] << 16);
    return (v & 0x800000) ? v - 0x1000000 : v;
}

void testWavWriterRoundTrip() {
    const std::string path = (std::filesystem::temp_directory_path() / "ps_wav_test.wav").string();
    ps::WavWriter wav;
    CHECK(wav.open(path.c_str(), kRate, 2));
    std::vector<float> data(10000 * 2);
    for (size_t i = 0; i < data.size(); ++i) data[i] = (i % 2 == 0) ? 0.5f : -1.5f;  // right clips
    CHECK(wav.write(data.data(), 10000));
    CHECK(wav.close());
    const auto f = readFile(path);
    CHECK(f.size() == 44 + 10000 * 2 * 3);
    CHECK(std::memcmp(f.data(), "RIFF", 4) == 0 && std::memcmp(f.data() + 8, "WAVE", 4) == 0);
    CHECK(le32(f.data() + 4) == f.size() - 8);
    CHECK(le32(f.data() + 40) == 10000 * 2 * 3);
    CHECK(f[34] == 24 && f[22] == 2 && le32(f.data() + 24) == kRate);
    CHECK(std::abs(sample24(f, 100) - 4194304) <= 2);  // 0.5 within dither
    CHECK(sample24(f, 101) == -8388608);                 // clamped, no wrap-around
    std::filesystem::remove(path);
}

void testOfflineExportRendersTheBeat() {
    const std::string path = (std::filesystem::temp_directory_path() / "ps_export_test.wav").string();
    ps::OfflineRenderer r(kRate);
    r.post({ps::CommandType::SetTempo, 120.0f});
    for (int step = 0; step < 16; step += 4) r.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
    // Far more commands than the queue holds must still all arrive.
    for (int i = 0; i < 10000; ++i) r.post({ps::CommandType::SetDrumHit, 0.0f, 5, 3});
    const int64_t bar = kRate * 2;  // one bar at 120 BPM
    CHECK(r.renderToWav(path.c_str(), bar, kRate / 2) == ps::OfflineRenderer::Result::Ok);
    CHECK(r.progress() == 1.0f);
    const auto f = readFile(path);
    CHECK(f.size() == 44 + size_t(bar + kRate / 2) * 2 * 3);
    // A kick on every beat: energy right after each beat, from the very first sample.
    int32_t firstPeak = 0, peak = 0;
    for (size_t i = 0; i < size_t(kRate / 50) * 2; ++i) firstPeak = std::max(firstPeak, std::abs(sample24(f, i)));
    for (size_t i = 0; i < (f.size() - 44) / 3; ++i) peak = std::max(peak, std::abs(sample24(f, i)));
    CHECK(firstPeak > 8388607 / 10);
    CHECK(peak <= int32_t(8388607 * 0.9f));  // limiter ceiling holds
    // Same beat without the hit on beat 2: identical up to beat 2 (sample-accurate), different after.
    const std::string path2 = path + ".b.wav";
    ps::OfflineRenderer r2(kRate);
    r2.post({ps::CommandType::SetTempo, 120.0f});
    for (int step = 0; step < 16; step += 4) r2.post({ps::CommandType::SetDrumHit, step == 4 ? 0.0f : 1.0f, 0, step});
    CHECK(r2.renderToWav(path2.c_str(), bar, kRate / 2) == ps::OfflineRenderer::Result::Ok);
    const auto g = readFile(path2);
    CHECK(g.size() == f.size());
    const size_t beat2 = size_t(kRate / 2) * 2;
    int32_t diffBefore = 0, diffAfter = 0;
    for (size_t i = 0; i < beat2; ++i) diffBefore = std::max(diffBefore, std::abs(sample24(f, i) - sample24(g, i)));
    for (size_t i = beat2; i < beat2 + 2000; ++i) diffAfter = std::max(diffAfter, std::abs(sample24(f, i) - sample24(g, i)));
    CHECK(diffBefore <= 4);  // dither only (same seed, so in practice 0)
    CHECK(diffAfter > 8388607 / 10);
    std::filesystem::remove(path2);

    // Into memory (resampling the song onto a pad): same length, same sound as the file.
    ps::OfflineRenderer r3(kRate);
    r3.post({ps::CommandType::SetTempo, 120.0f});
    for (int step = 0; step < 16; step += 4) r3.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
    const auto mem = r3.renderToBuffer(bar, kRate / 2);
    CHECK(mem.size() == size_t(bar + kRate / 2) * 2);
    if (mem.size() > 2000) CHECK(std::abs(int32_t(std::lrint(mem[1000] * 8388607.0f)) - sample24(f, 1000)) <= 2);

    ps::OfflineRenderer cancelled(kRate);
    cancelled.cancel();
    CHECK(cancelled.renderToWav(path.c_str(), bar, 0) == ps::OfflineRenderer::Result::Cancelled);
    CHECK(ps::OfflineRenderer(kRate).renderToWav("/nonexistent-dir/x.wav", bar, 0) == ps::OfflineRenderer::Result::IoError);
    std::filesystem::remove(path);
}

void testSampleResamplesCleanly() {
    // 1 kHz sine at 44.1 kHz, 1 s, stereo -> 48 kHz: same duration and pitch, no NaN.
    const int inFrames = 44100;
    std::vector<float> in(static_cast<size_t>(inFrames) * 2);
    for (int i = 0; i < inFrames; ++i) {
        const float v = 0.5f * std::sin(2.0f * float(M_PI) * 1000.0f * i / 44100.0f);
        in[size_t(i) * 2] = v;
        in[size_t(i) * 2 + 1] = -v;
    }
    ps::Sample* s = ps::Sample::create(in.data(), inFrames, 2, 44100, kRate);
    CHECK(s != nullptr);
    if (!s) return;
    CHECK(s->channels() == 2);
    CHECK(std::abs(s->frames() - kRate) <= 4);
    int crossings = 0;
    float peak = 0.0f;
    for (int64_t i = 1000; i < s->frames() - 1000; ++i) {
        const float a = s->data()[(i - 1) * 2], b = s->data()[i * 2];
        if (a < 0.0f && b >= 0.0f) ++crossings;
        peak = std::max(peak, std::fabs(b));
        CHECK(std::isfinite(b));
    }
    const double hz = crossings / ((s->frames() - 2000.0) / kRate);
    CHECK(std::abs(hz - 1000.0) < 3.0);
    CHECK(peak > 0.48f && peak < 0.52f);
    CHECK(s->release());
    delete s;
    CHECK(ps::Sample::create(in.data(), 0, 2, 44100, kRate) == nullptr);
    CHECK(ps::Sample::create(in.data(), 10, 3, 44100, kRate) == nullptr);
}

void testPadSampleReplacesTheDrum() {
    // A pad with a sample plays that sample, starting exactly on the step.
    std::vector<float> click(480, 0.0f);
    click[0] = 0.9f;
    click[100] = -0.4f;
    ps::Sample* s = ps::Sample::create(click.data(), 480, 1, kRate, kRate);
    ps::Engine engine(kRate);
    s->retain();  // the command's reference
    engine.post({ps::CommandType::InstallSample, 0.0f, ps::DrumKit::Kick, 0, s});
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, 4});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto out = render(engine, 6000 * 6, 200);
    CHECK(engine.padHasSample(ps::DrumKit::Kick));
    int first = -1;
    for (int i = 0; i < 6000 * 6 && first < 0; ++i) {
        if (out[size_t(i) * kChannels] != 0.0f) first = i;
    }
    CHECK(first == 24000);
    // The sample's shape, not a kick: a click, silence, a smaller negative click, then nothing.
    const float a = out[size_t(24000) * kChannels], b = out[size_t(24100) * kChannels];
    CHECK(a > 0.3f && b < 0.0f && std::fabs(b / a - (-0.4f / 0.9f)) < 0.02f);
    float tail = 0.0f;
    for (int i = 24600; i < 6000 * 6; ++i) tail = std::max(tail, std::fabs(out[size_t(i) * kChannels]));
    CHECK(tail < 1e-6f);
    CHECK(s->release() == false);  // the engine still holds it
}

void testSampleReferencesAreNeverLeakedOrFreedEarly() {
    std::vector<float> one(48, 0.25f);
    // Owner lets go first: the engine keeps it alive, then hands it back for freeing.
    ps::Sample* s = ps::Sample::create(one.data(), 48, 1, kRate, kRate);
    {
        ps::Engine engine(kRate);
        s->retain();
        engine.post({ps::CommandType::InstallSample, 0.0f, 3, 0, s});
        render(engine, 256, 256);
        CHECK(s->refCount() == 2);
        CHECK(!s->release());  // owner done, engine still plays it
        engine.post({ps::CommandType::TriggerPad, 1.0f, 3});
        const auto out = render(engine, 256, 256);
        CHECK(std::fabs(out[0] - 0.25f) < 1e-6f);
        engine.post({ps::CommandType::InstallSample, 0.0f, 3, 0, nullptr});  // back to the drum
        render(engine, 256, 256);
        CHECK(!engine.padHasSample(3));
        engine.collectGarbage();  // frees s (ASan would flag a leak or a double free)
    }
    // Re-sending the same sound (undo, back to the app) must not cut what is playing.
    {
        std::vector<float> longDc(48000, 0.5f);
        ps::Sample* d = ps::Sample::create(longDc.data(), 48000, 1, kRate, kRate);
        ps::Engine engine(kRate);
        d->retain();
        engine.post({ps::CommandType::InstallSample, 0.0f, 2, 0, d});
        engine.post({ps::CommandType::TriggerPad, 1.0f, 2});
        render(engine, 256, 256);
        d->retain();
        engine.post({ps::CommandType::InstallSample, 0.0f, 2, 0, d});
        const auto still = render(engine, 256, 256);
        CHECK(std::fabs(still[0] - 0.5f) < 1e-4f && std::fabs(still[255 * kChannels] - 0.5f) < 1e-4f);
        CHECK(d->refCount() == 2);  // the duplicate reference was dropped
        CHECK(!d->release());
    }
    // Commands never applied release their reference when the engine goes away.
    ps::Sample* t = ps::Sample::create(one.data(), 48, 1, kRate, kRate);
    {
        ps::Engine engine(kRate);
        t->retain();
        engine.post({ps::CommandType::InstallSample, 0.0f, 1, 0, t});
    }
    CHECK(t->refCount() == 1);
    CHECK(t->release());
    delete t;
    // Out-of-range pad: the reference is released, not kept.
    ps::Sample* u = ps::Sample::create(one.data(), 48, 1, kRate, kRate);
    {
        ps::Engine engine(kRate);
        u->retain();
        engine.post({ps::CommandType::InstallSample, 0.0f, 99, 0, u});
        render(engine, 64, 64);
    }
    CHECK(u->refCount() == 1);
    CHECK(u->release());
    delete u;
}

void testRetriggerFadesInsteadOfClicking() {
    std::vector<float> dc(48000, 0.5f);
    ps::Sample* s = ps::Sample::create(dc.data(), 48000, 1, kRate, kRate);
    ps::Sampler sampler;
    sampler.install(0, s);
    std::vector<float> out(2000, 0.0f);
    sampler.trigger(0, 1.0f, 0);
    sampler.trigger(0, 1.0f, 0);
    sampler.render(out.data(), 1000);
    sampler.trigger(0, 1.0f, 500);
    std::fill(out.begin(), out.end(), 0.0f);
    sampler.render(out.data(), 1000);
    // Old hit fading (0.5 -> 0) plus new hit (0.5): never above 1, never a gap.
    float lo = 1.0f, hi = 0.0f;
    for (int i = 0; i < 1000; ++i) { lo = std::min(lo, out[size_t(i)]); hi = std::max(hi, out[size_t(i)]); }
    CHECK(lo >= 0.49f && hi <= 1.0001f);
    CHECK(out[600] > 0.99f * 0.5f);
}

// Renders one hit of `pad` straight from a Sampler (mono), for parameter tests.
std::vector<float> hitSampler(ps::Sampler& sampler, int pad, int frames, int block = 128) {
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    sampler.trigger(pad, 1.0f, 0);
    for (int done = 0; done < frames; done += block) sampler.render(out.data() + done, std::min(block, frames - done));
    return out;
}

int zeroCrossingsUp(const std::vector<float>& x, int from, int to) {
    int n = 0;
    for (int i = from + 1; i < to; ++i) if (x[size_t(i) - 1] < 0.0f && x[size_t(i)] >= 0.0f) ++n;
    return n;
}

void testPadPitchRegionAndReverse() {
    // 1 s ramp 0 -> 1 (to see direction and region), and a 1 s 500 Hz sine (to see pitch).
    std::vector<float> ramp(48000), sine(48000);
    for (int i = 0; i < 48000; ++i) {
        ramp[size_t(i)] = i / 48000.0f;
        sine[size_t(i)] = 0.5f * std::sin(2.0f * float(M_PI) * 500.0f * i / 48000.0f);
    }
    ps::Sampler sampler;
    sampler.install(0, ps::Sample::create(sine.data(), 48000, 1, kRate, kRate));
    sampler.install(1, ps::Sample::create(ramp.data(), 48000, 1, kRate, kRate));

    // +12 semitones: twice the pitch, half the length.
    sampler.setParam(0, ps::Sampler::Pitch, 12.0f);
    auto up = hitSampler(sampler, 0, 48000);
    CHECK(std::abs(zeroCrossingsUp(up, 0, 20000) - 1000 * 20000 / 48000) <= 2);
    float after = 0.0f;
    for (int i = 24100; i < 48000; ++i) after = std::max(after, std::fabs(up[size_t(i)]));
    CHECK(after == 0.0f);

    // Region 25 %..75 %, backwards: starts near 0.75 and falls to 0.25, lasts half a second.
    sampler.setParam(1, ps::Sampler::Start, 0.25f);
    sampler.setParam(1, ps::Sampler::End, 0.75f);
    sampler.setParam(1, ps::Sampler::Reverse, 1.0f);
    sampler.setParam(1, ps::Sampler::Release, 0.0f);
    auto rev = hitSampler(sampler, 1, 30000);
    CHECK(std::fabs(rev[0] - 0.75f) < 0.001f);
    CHECK(std::fabs(rev[23990] - (0.75f - 23990 / 48000.0f)) < 0.001f);
    CHECK(rev[24010] == 0.0f);
}

void testPadLoopEnvelopeAndChoke() {
    std::vector<float> dc(4800, 0.5f);  // 100 ms of DC
    ps::Sampler sampler;
    sampler.install(0, ps::Sample::create(dc.data(), 4800, 1, kRate, kRate));
    sampler.install(1, ps::Sample::create(dc.data(), 4800, 1, kRate, kRate));

    // Loop: keeps sounding well past the sound's length, until released.
    sampler.setParam(0, ps::Sampler::Loop, 1.0f);
    auto looped = hitSampler(sampler, 0, 24000);
    CHECK(std::fabs(looped[20000] - 0.5f) < 1e-4f);
    sampler.releaseLoops();
    std::vector<float> tail(1000, 0.0f);
    sampler.render(tail.data(), 1000);
    CHECK(tail[0] > 0.4f && tail[200] == 0.0f);  // 2 ms fade, then silence

    // Attack 10 ms: half way up after 5 ms; release 20 ms: fading at the end.
    sampler.setParam(1, ps::Sampler::Attack, 0.010f);
    sampler.setParam(1, ps::Sampler::Release, 0.020f);
    auto shaped = hitSampler(sampler, 1, 4800);
    CHECK(std::fabs(shaped[240] - 0.25f) < 0.01f);
    CHECK(std::fabs(shaped[2400] - 0.5f) < 1e-4f);
    CHECK(std::fabs(shaped[4800 - 480] - 0.25f) < 0.01f);

    // Choke group: hitting pad 1 silences pad 0 within 2 ms.
    sampler.setParam(0, ps::Sampler::Choke, 1.0f);
    sampler.setParam(1, ps::Sampler::Choke, 1.0f);
    sampler.setParam(1, ps::Sampler::Gain, 0.0f);  // pad 1 itself silent: only pad 0 is heard
    std::vector<float> out(2000, 0.0f);
    sampler.trigger(0, 1.0f, 0);
    sampler.render(out.data(), 1000);
    CHECK(std::fabs(out[999] - 0.5f) < 1e-4f);
    sampler.trigger(1, 1.0f, 0);
    sampler.render(out.data() + 1000, 1000);
    CHECK(out[1000 + 200] == 0.0f);
}

void testPadParamCommandsReachTheSampler() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetPadParam, 7.0f, 2, ps::Sampler::Pitch});
    engine.post({ps::CommandType::SetPadParam, 99.0f, 2, ps::Sampler::Pitch});  // clamped
    engine.post({ps::CommandType::SetPadParam, 1.0f, 99, ps::Sampler::Pitch});  // bad pad: ignored
    engine.post({ps::CommandType::SetPadParam, 1.0f, 2, 999});                  // bad param: ignored
    engine.post({ps::CommandType::SetPadParam, NAN, 2, ps::Sampler::Gain});     // NaN: ignored
    render(engine, 64, 64);
    CHECK(engine.padParams(2).pitch == 24.0f);
    CHECK(engine.padParams(2).gain == 1.0f);
}

void testOnsetsFindTheHits() {
    // Four decaying noise bursts (like drum hits) at known places, with soft noise between.
    const int frames = kRate * 2;
    std::vector<float> x(size_t(frames), 0.0f);
    std::mt19937 rng(7);
    std::uniform_real_distribution<float> noise(-1.0f, 1.0f);
    const int64_t hits[] = {0, 21000, 50000, 71000};
    const float levels[] = {0.9f, 0.6f, 0.8f, 0.5f};
    for (int i = 0; i < frames; ++i) x[size_t(i)] = 0.002f * noise(rng);
    for (int h = 0; h < 4; ++h) {
        for (int i = 0; i < 6000; ++i) x[size_t(hits[h] + i)] += levels[h] * noise(rng) * std::exp(-i / 900.0f);
    }
    auto onsets = ps::detectOnsets(x.data(), frames, kRate);
    std::printf("  onsets:");
    for (auto o : onsets) std::printf(" %lld", static_cast<long long>(o));
    std::printf("\n");
    CHECK(onsets.size() == 4);
    for (size_t h = 0; h < 4 && h < onsets.size(); ++h) CHECK(std::llabs(onsets[h] - hits[h]) < kRate / 100);

    // A cap keeps the strongest hits; silence gives just the start.
    ps::OnsetOptions two;
    two.maxSlices = 2;
    auto capped = ps::detectOnsets(x.data(), frames, kRate, two);
    CHECK(capped.size() == 2 && capped[0] == 0 && std::llabs(capped[1] - hits[2]) < kRate / 100);
    std::vector<float> silence(size_t(frames), 0.0f);
    CHECK(ps::detectOnsets(silence.data(), frames, kRate).size() == 1);
}

void testTimeStretchKeepsPitchAndLength() {
    const int n = kRate;  // 1 s of 1 kHz, stereo
    std::vector<float> in(size_t(n) * 2);
    for (int i = 0; i < n; ++i) in[size_t(i) * 2] = in[size_t(i) * 2 + 1] = 0.5f * std::sin(2.0f * float(M_PI) * 1000.0f * i / kRate);
    auto rms = [](const std::vector<float>& x, int from, int to) {
        double e = 0.0;
        for (int i = from; i < to; ++i) e += double(x[size_t(i) * 2]) * x[size_t(i) * 2];
        return std::sqrt(e / (to - from));
    };
    auto hz = [](const std::vector<float>& x, int from, int to) {
        int up = 0;
        for (int i = from + 1; i < to; ++i) if (x[size_t(i - 1) * 2] < 0.0f && x[size_t(i) * 2] >= 0.0f) ++up;
        return up / ((to - from) / double(kRate));
    };

    // 1.5x longer, same pitch, similar level.
    auto longer = ps::timeStretch(in.data(), n, 2, n * 3 / 2, 0.0f, kRate);
    CHECK(longer.size() == size_t(n * 3 / 2) * 2);
    if (longer.size() == size_t(n * 3 / 2) * 2) {
        std::printf("  stretched: %.1f Hz, rms %.3f\n", hz(longer, 12000, 60000), rms(longer, 12000, 60000));
        CHECK(std::fabs(hz(longer, 12000, 60000) - 1000.0) < 10.0);
        CHECK(std::fabs(rms(longer, 12000, 60000) - 0.3536) < 0.06);
    }
    // +12 semitones, same length: twice the pitch.
    auto up = ps::timeStretch(in.data(), n, 2, n, 12.0f, kRate);
    CHECK(up.size() == size_t(n) * 2);
    if (up.size() == size_t(n) * 2) {
        std::printf("  transposed: %.1f Hz\n", hz(up, 12000, 36000));
        CHECK(std::fabs(hz(up, 12000, 36000) - 2000.0) < 20.0);
    }
    for (float v : longer) CHECK(std::isfinite(v));
    // Bad input gives nothing rather than garbage.
    CHECK(ps::timeStretch(in.data(), n, 3, n, 0.0f, kRate).empty());
    CHECK(ps::timeStretch(nullptr, n, 2, n, 0.0f, kRate).empty());
}

void testPianoRollNotesAreSampleAccurate() {
    // 120 BPM: a step is 6000 frames, a tick 250. A note at tick 36 (1.5 steps), 48 ticks long.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetSynthSteps, 0.0f, 16});
    engine.post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(36, 48, 57)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto out = render(engine, 6000 * 16 + 20000, 256);
    auto firstSound = [&](int from) {
        for (int i = from; i < int(out.size() / kChannels); ++i) if (out[size_t(i) * kChannels] != 0.0f) return i;
        return -1;
    };
    const int first = firstSound(0);
    CHECK(first >= 9000 && first <= 9002);
    // Released at 9000 + 12000: by the next loop's note (96000 + 9000) it has died away, then sounds again.
    float quiet = 0.0f;
    for (int i = 96000 + 8000; i < 96000 + 9000; ++i) quiet = std::max(quiet, std::fabs(out[size_t(i) * kChannels]));
    float again = 0.0f;
    for (int i = 96000 + 9000; i < 96000 + 12000; ++i) again = std::max(again, std::fabs(out[size_t(i) * kChannels]));
    CHECK(again > 10.0f * quiet && again > 0.01f);
    // Stop: nothing left queued; a deleted note (count 0) is not played.
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 256, 256);
    CHECK(engine.notesPending() == 0);
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 0});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 3, 256);
    CHECK(engine.notesPending() == 0);
}

// A minimal SoundFont 2 file: one looping 440 Hz sine sample (root key 69), one instrument, one preset.
std::vector<uint8_t> tinySoundFont() {
    std::vector<uint8_t> out;
    auto u8 = [](std::vector<uint8_t>& b, int v) { b.push_back(uint8_t(v)); };
    auto u16 = [&](std::vector<uint8_t>& b, int v) { u8(b, v & 0xFF); u8(b, (v >> 8) & 0xFF); };
    auto u32 = [&](std::vector<uint8_t>& b, uint32_t v) { u16(b, int(v & 0xFFFF)); u16(b, int(v >> 16)); };
    auto tag = [](std::vector<uint8_t>& b, const char* t) { b.insert(b.end(), t, t + 4); };
    auto name20 = [](std::vector<uint8_t>& b, const char* n) { char z[20] = {}; std::strncpy(z, n, 19); b.insert(b.end(), z, z + 20); };
    auto chunk = [&](std::vector<uint8_t>& b, const char* id, const std::vector<uint8_t>& data) {
        tag(b, id); u32(b, uint32_t(data.size())); b.insert(b.end(), data.begin(), data.end());
        if (data.size() & 1) b.push_back(0);
    };
    auto list = [&](const char* type, const std::vector<uint8_t>& body) {
        std::vector<uint8_t> l; tag(l, type); l.insert(l.end(), body.begin(), body.end()); return l;
    };
    const int rate = 44100, n = rate;  // 1 s; exactly 440 cycles, so the loop is seamless
    std::vector<uint8_t> info, ifil, isng, inam;
    u16(ifil, 2); u16(ifil, 1);
    const char* engineName = "EMU8000"; isng.assign(engineName, engineName + 8);
    const char* bankName = "Test"; inam.assign(bankName, bankName + 5);
    chunk(info, "ifil", ifil); chunk(info, "isng", isng); chunk(info, "INAM", inam);
    std::vector<uint8_t> smpl;
    for (int i = 0; i < n + 46; ++i) u16(smpl, i < n ? int(std::lrint(16000.0 * std::sin(2.0 * M_PI * 440.0 * i / rate))) : 0);
    std::vector<uint8_t> sdta; chunk(sdta, "smpl", smpl);
    std::vector<uint8_t> phdr, pbag, pmod(10, 0), pgen, inst, ibag, imod(10, 0), igen, shdr;
    name20(phdr, "Sine"); u16(phdr, 0); u16(phdr, 0); u16(phdr, 0); u32(phdr, 0); u32(phdr, 0); u32(phdr, 0);
    name20(phdr, "EOP"); u16(phdr, 0); u16(phdr, 0); u16(phdr, 1); u32(phdr, 0); u32(phdr, 0); u32(phdr, 0);
    u16(pbag, 0); u16(pbag, 0); u16(pbag, 1); u16(pbag, 0);
    u16(pgen, 41); u16(pgen, 0);  // instrument 0
    u16(pgen, 0); u16(pgen, 0);
    name20(inst, "Sine"); u16(inst, 0);
    name20(inst, "EOI"); u16(inst, 1);
    u16(ibag, 0); u16(ibag, 0); u16(ibag, 2); u16(ibag, 0);
    u16(igen, 54); u16(igen, 1);  // sampleModes: loop
    u16(igen, 53); u16(igen, 0);  // sampleID 0
    u16(igen, 0); u16(igen, 0);
    name20(shdr, "Sine"); u32(shdr, 0); u32(shdr, n); u32(shdr, 0); u32(shdr, n); u32(shdr, rate); u8(shdr, 69); u8(shdr, 0); u16(shdr, 0); u16(shdr, 1);
    name20(shdr, "EOS"); for (int i = 0; i < 26; ++i) u8(shdr, 0);
    std::vector<uint8_t> pdta;
    chunk(pdta, "phdr", phdr); chunk(pdta, "pbag", pbag); chunk(pdta, "pmod", pmod); chunk(pdta, "pgen", pgen);
    chunk(pdta, "inst", inst); chunk(pdta, "ibag", ibag); chunk(pdta, "imod", imod); chunk(pdta, "igen", igen);
    chunk(pdta, "shdr", shdr);
    std::vector<uint8_t> body; tag(body, "sfbk");
    chunk(body, "LIST", list("INFO", info)); chunk(body, "LIST", list("sdta", sdta)); chunk(body, "LIST", list("pdta", pdta));
    chunk(out, "RIFF", body);
    return out;
}

void testInstrumentPlaysTheSoundFont() {
    const std::string path = (std::filesystem::temp_directory_path() / "ps_tiny.sf2").string();
    { auto bytes = tinySoundFont(); std::ofstream(path, std::ios::binary).write(reinterpret_cast<const char*>(bytes.data()), std::streamsize(bytes.size())); }
    tsf* master = ps::SoundFontPlayer::loadFile(path.c_str());
    CHECK(master != nullptr);
    if (!master) return;
    auto pitchOf = [](const std::vector<float>& x, int from, int to) {
        int up = 0;
        for (int i = from + 1; i < to; ++i) if (x[size_t(i - 1) * kChannels] < 0.0f && x[size_t(i) * kChannels] >= 0.0f) ++up;
        return up / ((to - from) / double(kRate));
    };
    {
        ps::Engine engine(kRate);
        ps::Command install{ps::CommandType::SetInstrument, 0.0f, 0};
        install.font = ps::SoundFontPlayer::makeInstance(master, kRate);
        engine.post(install);
        engine.post({ps::CommandType::SynthNoteOn, 1.0f, 69});
        auto a4 = render(engine, kRate / 2, 256);
        CHECK(engine.instrumentActive());
        std::printf("  instrument A4: %.1f Hz\n", pitchOf(a4, 4800, 24000));
        CHECK(std::fabs(pitchOf(a4, 4800, 24000) - 440.0) < 5.0);
        engine.post({ps::CommandType::SynthNoteOff, 0.0f, 69});
        engine.post({ps::CommandType::SynthNoteOn, 1.0f, 81});
        auto a5 = render(engine, kRate / 2, 256);
        CHECK(std::fabs(pitchOf(a5, 12000, 24000) - 880.0) < 10.0);
        // A replacement instance retires the first; back to the synth with nullptr.
        ps::Command again{ps::CommandType::SetInstrument, 0.0f, 0};
        again.font = ps::SoundFontPlayer::makeInstance(master, kRate);
        engine.post(again);
        render(engine, 64, 64);
        engine.collectGarbage();
        engine.post({ps::CommandType::SetInstrument, 0.0f, 0});
        render(engine, 64, 64);
        CHECK(!engine.instrumentActive());
        // A pending install is closed if the engine goes away first (ASan checks for leaks).
        ps::Command pending{ps::CommandType::SetInstrument, 0.0f, 0};
        pending.font = ps::SoundFontPlayer::makeInstance(master, kRate);
        engine.post(pending);
    }
    // Rendering one note offline (drum kit onto the pads): sound while held, then the release.
    std::vector<float> note(static_cast<size_t>(kRate), 0.0f);
    CHECK(ps::SoundFontPlayer::renderNote(master, 0, 69, 1.0f, kRate / 2, note.data(), kRate, kRate));
    float held = 0.0f;
    for (int i = 1000; i < kRate / 2; ++i) held = std::max(held, std::fabs(note[size_t(i)]));
    CHECK(held > 0.05f);
    CHECK(!ps::SoundFontPlayer::renderNote(master, 99, 69, 1.0f, 10, note.data(), 100, kRate));  // no such preset
    ps::SoundFontPlayer::close(master);
    CHECK(ps::SoundFontPlayer::loadFile("/nonexistent/x.sf2") == nullptr);
    std::filesystem::remove(path);
}

void testProcessNeverAllocates() {
#ifdef PS_COUNT_ALLOCATIONS
    // Everything at once: drums, a sampled pad (pitched, looping), piano-roll notes on an instrument,
    // the acid line, mixer and limiter. After warm-up, process() must not allocate at all.
    const std::string path = (std::filesystem::temp_directory_path() / "ps_alloc.sf2").string();
    { auto bytes = tinySoundFont(); std::ofstream(path, std::ios::binary).write(reinterpret_cast<const char*>(bytes.data()), std::streamsize(bytes.size())); }
    tsf* master = ps::SoundFontPlayer::loadFile(path.c_str());
    std::vector<float> wave(4800);
    for (size_t i = 0; i < wave.size(); ++i) wave[i] = 0.3f * std::sin(0.05f * float(i));
    ps::Sample* sample = ps::Sample::create(wave.data(), 4800, 1, kRate, kRate);
    {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 140.0f});
        for (int step = 0; step < 16; ++step) engine.post({ps::CommandType::SetDrumHit, 0.9f, step % 16, step});
        sample->retain();
        ps::Command pad{ps::CommandType::InstallSample, 0.0f, 3, 0};
        pad.sample = sample;
        engine.post(pad);
        engine.post({ps::CommandType::SetPadParam, 7.0f, 3, ps::Sampler::Pitch});
        engine.post({ps::CommandType::SetPadParam, 1.0f, 3, ps::Sampler::Loop});
        for (int n = 0; n < 32; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n, ps::NoteTrack::pack(n * 12, 30, 48 + n % 24)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 32});
        ps::Command inst{ps::CommandType::SetInstrument, 0.0f, 0};
        inst.font = ps::SoundFontPlayer::makeInstance(master, kRate);
        engine.post(inst);
        for (int st = 0; st < 16; ++st) engine.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{st % 12, 0, st % 3 == 0, st % 5 == 0, true}.pack()});
        ps::Command stems{ps::CommandType::InstallStems};
        auto* bundle = ps::StemBundle::fromFloat(std::vector<std::vector<float>>(4, std::vector<float>(kRate * 20, 0.1f)));
        stems.stems = bundle;
        bundle->retain();  // ours, for the re-installs below (what the app does on every push)
        engine.post(stems);
        engine.post({ps::CommandType::SetStemParam, 1.0f, 2, ps::StemPlayer::Solo});
        // The same song on the preview player, and live pads looping.
        bundle->retain();
        ps::Command preview{ps::CommandType::InstallStems, 0.0f, 1};
        preview.stems = bundle;
        engine.post(preview);
        engine.post({ps::CommandType::PlayPreview, 1.0f});
        for (int pad = 0; pad < 4; ++pad) {
            sample->retain();
            ps::Command clip{ps::CommandType::InstallClip, 0.0f, pad, 0};
            clip.sample = sample;
            engine.post(clip);
            engine.post({ps::CommandType::SetClipParam, float(1 + pad), pad, ps::ClipLauncher::Beats});
            engine.post({ps::CommandType::ToggleClip, 0.0f, pad});
        }
        // Every insert type somewhere, both sends, master inserts: effects must not allocate either.
        for (int ch = 0; ch <= ps::Mixer::kMaster; ++ch) {
            for (int slot = 0; slot < ps::Mixer::kInserts; ++slot) {
                engine.post({ps::CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts + slot, 1 + (ch + slot) % 6});
            }
            if (ch < ps::Mixer::kMaster) {
                engine.post({ps::CommandType::SetSend, 0.5f, ch, 0});
                engine.post({ps::CommandType::SetSend, 0.5f, ch, 1});
            }
        }
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        std::vector<float> out(512 * 2);
        for (int i = 0; i < 50; ++i) engine.process(nullptr, 0, 0, out.data(), 512, 2);  // warm-up
        g_allocations = 0;
        g_countAllocations = true;
        for (int i = 0; i < 400; ++i) {
            if (i % 50 == 0) engine.post({ps::CommandType::TriggerPad, 1.0f, 3});
            if (i % 70 == 0) engine.post({ps::CommandType::SynthNoteOn, 0.9f, 60 + i % 12});
            if (i % 40 == 0) engine.post({ps::CommandType::SetInsertParam, (i % 80) / 80.0f, 1, 0});
            if (i % 60 == 0) engine.post({ps::CommandType::SetInsertType, 0.0f, 5, 1 + (i / 60) % 6});
            if (i % 55 == 0) engine.post({ps::CommandType::SetBusParam, (i % 110) / 110.0f, 1, 0});
            if (i % 30 == 0) engine.post({ps::CommandType::SetPerformance, (i % 60) ? 1.0f : 0.0f, (i / 30) % 6});
            if (i % 25 == 0) engine.post({ps::CommandType::ToggleClip, 0.0f, (i / 25) % 4});
            if (i % 45 == 0) engine.post({ps::CommandType::SetPreviewParam, (i / 45) % 2 ? 1.0f : 0.0f, 1, ps::StemPlayer::Mute});
            if (i % 90 == 0) {  // same bundle again: the old reference goes through the retire queue
                bundle->retain();
                ps::Command again{ps::CommandType::InstallStems};
                again.stems = bundle;
                engine.post(again);
            }
            engine.process(nullptr, 0, 0, out.data(), 512, 2);
        }
        g_countAllocations = false;
        std::printf("  allocations during %d blocks: %d\n", 400, g_allocations.load());
        CHECK(g_allocations.load() == 0);
        engine.collectGarbage();
        CHECK(bundle->refCount() == 3);  // ours + the two players', none leaked by the re-installs
        CHECK(!bundle->release());
    }
    CHECK(sample->release());
    delete sample;
    ps::SoundFontPlayer::close(master);
    std::filesystem::remove(path);
#endif
}

void testLimiterHoldsTheCeiling() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    auto loud = mixConstant(mixer, 0.9f, 0.9f, 0.9f);  // 2.7 summed
    float peak = 0.0f;
    for (float v : loud) peak = std::max(peak, std::fabs(v));
    CHECK(peak <= ps::Mixer::kCeiling + 1e-6f);
    CHECK(mixer.limiterGain() < 0.5f);
    CHECK(mixer.takeMasterPeak() > 0.8f);
    CHECK(mixer.takeChannelPeak(0) > 0.8f);
    CHECK(mixer.takeChannelPeak(0) == 0.0f);  // reading resets the meter

    // Quiet material passes untouched once the limiter has recovered.
    mixConstant(mixer, 0.0f, 0.0f, 0.0f, kRate);
    auto quiet = mixConstant(mixer, 0.3f, 0.0f, 0.0f);
    CHECK(std::fabs(quiet.back() - 0.3f) < 1e-3f);
}

// Writes mono `x` as a WAV: 32-bit float, or 16-bit PCM when `pcm16`.
void writeTestWav(const std::string& path, const std::vector<float>& x, int rate, bool pcm16) {
    std::FILE* f = std::fopen(path.c_str(), "wb");
    auto u32 = [&](uint32_t v) { std::fwrite(&v, 4, 1, f); };
    auto u16 = [&](uint16_t v) { std::fwrite(&v, 2, 1, f); };
    const uint32_t bytes = static_cast<uint32_t>(x.size() * (pcm16 ? 2 : 4));
    std::fwrite("RIFF", 1, 4, f); u32(36 + bytes); std::fwrite("WAVEfmt ", 1, 8, f);
    u32(16); u16(pcm16 ? 1 : 3); u16(1); u32(static_cast<uint32_t>(rate)); u32(static_cast<uint32_t>(rate * (pcm16 ? 2 : 4))); u16(pcm16 ? 2 : 4); u16(pcm16 ? 16 : 32);
    std::fwrite("LIST", 1, 4, f); u32(4); std::fwrite("INFO", 1, 4, f);  // a chunk to skip
    std::fwrite("data", 1, 4, f); u32(bytes);
    for (float v : x) {
        if (pcm16) { const auto s16 = static_cast<int16_t>(std::lround(std::clamp(v, -1.0f, 1.0f) * 32767)); std::fwrite(&s16, 2, 1, f); }
        else std::fwrite(&v, 4, 1, f);
    }
    std::fclose(f);
}

void testTempoAndKeyDetection() {
    constexpr int rate = 44100;
    // 30 s of a four-on-the-floor beat at 124 BPM with off-beat hats.
    std::vector<float> beat(static_cast<size_t>(rate * 30), 0.0f);
    std::mt19937 rng(7);
    std::uniform_real_distribution<float> u(-1, 1);
    const double period = 60.0 / 124.0;
    for (double t = 0.25; t < 29.5; t += period) {
        const auto at = static_cast<size_t>(t * rate);
        for (int i = 0; i < rate / 5; ++i) {
            const float e = std::exp(-i / (0.03f * rate));
            beat[at + static_cast<size_t>(i)] += 0.9f * e * static_cast<float>(std::sin(2 * M_PI * 55 * i / rate));
        }
        const auto hat = at + static_cast<size_t>(period / 2 * rate);
        for (int i = 0; i < rate / 40; ++i) beat[hat + static_cast<size_t>(i)] += 0.3f * u(rng) * std::exp(-i / (0.005f * rate));
    }
    const auto direct = ps::detectTempo(beat.data(), static_cast<int64_t>(beat.size()), rate);
    std::printf("  tempo %.1f BPM (confidence %.2f, first beat %.2f s)\n", direct.bpm, direct.confidence, direct.firstBeat);
    CHECK(std::fabs(direct.bpm - 124.0f) < 1.5f);
    CHECK(direct.confidence > 0.5f);
    CHECK(std::fabs(direct.firstBeat - 0.25f) < 0.03f);  // the grid lines up with the kicks
    const auto dir = std::filesystem::temp_directory_path();
    const auto pcmPath = (dir / "ps_beat16.wav").string();
    writeTestWav(pcmPath, beat, rate, true);
    const auto fromFile = ps::detectTempo(pcmPath);
    CHECK(std::fabs(fromFile.bpm - 124.0f) < 1.5f);
    CHECK(ps::detectTempo((dir / "missing.wav").string()).bpm == 0.0f);
    std::vector<float> silence(static_cast<size_t>(rate * 5), 0.0f);
    CHECK(ps::detectTempo(silence.data(), static_cast<int64_t>(silence.size()), rate).bpm == 0.0f);

    // Chords with harmonics, 2 s each, three times round.
    auto progression = [&](const std::vector<std::vector<int>>& chords) {
        std::vector<float> x;
        for (int rep = 0; rep < 3; ++rep) {
            for (const auto& chord : chords) {
                for (int i = 0; i < rate * 2; ++i) {
                    float v = 0;
                    for (int note : chord) {
                        const double f = 440.0 * std::pow(2.0, (note - 69) / 12.0);
                        for (int h = 1; h <= 4; ++h) v += static_cast<float>(std::sin(2 * M_PI * f * h * i / rate) / (h * 6.0));
                    }
                    x.push_back(v);
                }
            }
        }
        return x;
    };
    // A minor: Am Dm E Am (bass note an octave down).
    const auto aMinor = progression({{45, 57, 60, 64}, {50, 62, 65, 69}, {52, 64, 68, 71}, {45, 57, 60, 64}});
    const auto k1 = ps::detectKey(aMinor.data(), static_cast<int64_t>(aMinor.size()), rate);
    std::printf("  key A minor -> tonic %d minor %d (confidence %.3f)\n", k1.tonic, k1.minor, k1.confidence);
    CHECK(k1.tonic == 9 && k1.minor);
    // C major: C F G C, read back from two float WAVs summed (like bass + other stems).
    const auto cMajor = progression({{48, 60, 64, 67}, {53, 65, 69, 72}, {55, 67, 71, 74}, {48, 60, 64, 67}});
    const auto half = cMajor.size() / 2;
    const auto p1 = (dir / "ps_key1.wav").string(), p2 = (dir / "ps_key2.wav").string();
    writeTestWav(p1, std::vector<float>(cMajor.begin(), cMajor.begin() + static_cast<std::ptrdiff_t>(half)), rate, false);
    writeTestWav(p2, std::vector<float>(cMajor.begin() + static_cast<std::ptrdiff_t>(half), cMajor.end()), rate, false);
    const auto k2 = ps::detectKey(std::vector<std::string>{p1, p2});
    std::printf("  key C major -> tonic %d minor %d (confidence %.3f)\n", k2.tonic, k2.minor, k2.confidence);
    CHECK(k2.tonic == 0 && !k2.minor);
    CHECK(ps::detectKey(silence.data(), static_cast<int64_t>(silence.size()), rate).tonic == -1);
    std::filesystem::remove(pcmPath);
    std::filesystem::remove(p1);
    std::filesystem::remove(p2);
}

void testStemsPlayInStepWithTheTransport() {
    // Four constant stems (left, right differ) plus a ramp on stem 0 to read the song position.
    constexpr int songFrames = kRate * 4;
    const float levels[4] = {0.05f, 0.1f, 0.15f, 0.2f};
    std::vector<std::vector<float>> stems(4);
    for (int st = 0; st < 4; ++st) {
        stems[st].resize(static_cast<size_t>(songFrames) * 2);
        for (int i = 0; i < songFrames; ++i) {
            stems[st][static_cast<size_t>(i) * 2] = levels[st];
            stems[st][static_cast<size_t>(i) * 2 + 1] = st == 0 ? 0.25f * static_cast<float>(i) / songFrames : 0.0f;
        }
    }
    auto* bundle = ps::StemBundle::fromFloat(stems);
    CHECK(bundle && bundle->frames() == songFrames);
    {
        ps::Engine engine(kRate);
        ps::Command install{ps::CommandType::InstallStems};
        install.stems = bundle;  // the engine owns this reference
        engine.post(install);
        engine.post({ps::CommandType::SetStemStart, 0.0f, kRate});  // play from 1 s into the song
        engine.post({ps::CommandType::SetTransport, 1.0f, 1});     // one bar of count-in at 120 BPM = 2 s
        const auto out = render(engine, kRate * 3, 256);
        CHECK(engine.stemsLoaded());
        const int start = kRate * 2;
        CHECK(std::fabs(out[static_cast<size_t>(start - 1) * 2]) < 1e-4f);  // silent through the count-in (metronome off-beat aside)
        const float left = out[static_cast<size_t>(start + 4800) * 2];
        CHECK(std::fabs(left - 0.5f) < 2e-3f);  // all four stems at unity on the remix channel
        // Right side = the ramp: the song position at step 0 is exactly the start frame.
        const float right = out[static_cast<size_t>(start) * 2 + 1];
        CHECK(std::fabs(right - 0.25f * kRate / songFrames) < 1e-3f);
        CHECK(engine.stemPosition() > kRate);
        // Mute the loudest stem, then solo the quietest.
        engine.post({ps::CommandType::SetStemParam, 1.0f, 3, ps::StemPlayer::Mute});
        auto muted = render(engine, 4800, 256);
        CHECK(std::fabs(muted[muted.size() - 2] - 0.3f) < 2e-3f);
        engine.post({ps::CommandType::SetStemParam, 1.0f, 0, ps::StemPlayer::Solo});
        auto solo = render(engine, 4800, 256);
        CHECK(std::fabs(solo[solo.size() - 2] - 0.05f) < 2e-3f);
        // Stop: a 5 ms fade, then silence and no position.
        engine.post({ps::CommandType::SetTransport, 0.0f});
        auto stopped = render(engine, 4800, 256);
        CHECK(std::fabs(stopped[stopped.size() - 2]) < 1e-6f);
        CHECK(engine.stemPosition() == -1);
        // Unload: the bundle goes back to the main thread to be freed.
        engine.post({ps::CommandType::InstallStems});
        render(engine, 256, 256);
        engine.collectGarbage();
        CHECK(!engine.stemsLoaded());
    }

    // From WAV files at 44.1 kHz: resampled to 48 kHz, pitch kept, regions extractable.
    const auto dir = std::filesystem::temp_directory_path();
    std::vector<std::string> paths;
    std::vector<float> tone(44100 * 3);  // several read and resampler chunks
    for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.5f * static_cast<float>(std::sin(2 * M_PI * 1000.0 * i / 44100.0));
    for (int st = 0; st < 4; ++st) {
        paths.push_back((dir / ("ps_stem" + std::to_string(st) + ".wav")).string());
        writeTestWav(paths.back(), tone, 44100, st == 1);  // one in 16-bit PCM
    }
    auto* loaded = ps::StemBundle::load(paths, kRate);
    CHECK(loaded != nullptr);
    if (loaded) {
        CHECK(std::llabs(loaded->frames() - kRate * 3) <= 2);
        // No click anywhere (chunk joins included): a 1 kHz sine at 0.5 never moves more than
        // 2*pi*1000/48000*0.5 = 0.065 between two samples.
        for (int st = 0; st < 4; ++st) {
            const auto whole = loaded->extract(st, 0, loaded->frames());
            float maxStep = 0.0f;
            for (size_t i = 2 + 200; i + 400 < whole.size(); i += 2) maxStep = std::max(maxStep, std::fabs(whole[i] - whole[i - 2]));
            if (st == 0) std::printf("  stem resample: largest step %.4f (smooth < 0.07)\n", maxStep);
            CHECK(maxStep < 0.07f);
        }
        const auto region = loaded->extract(2, 0, kRate / 2);
        CHECK(region.size() == static_cast<size_t>(kRate));  // half a second, stereo
        int crossings = 0;
        for (size_t i = 2; i < region.size(); i += 2) if ((region[i - 2] < 0) != (region[i] < 0)) ++crossings;
        CHECK(crossings >= 995 && crossings <= 1005);  // 1 kHz for 0.5 s
        CHECK(region[1000] == region[1001]);  // mono file on both sides
        CHECK(loaded->extract(9, 0, 10).empty() && loaded->extract(0, loaded->frames(), 10).empty());
        const auto overview = loaded->peaks(0, 300);
        CHECK(overview.size() == 300);
        CHECK(std::all_of(overview.begin(), overview.end(), [](float v) { return v > 0.45f && v < 0.55f; }));  // the 0.5 sine
        CHECK(loaded->peaks(7, 10).empty());
        CHECK(loaded->release());
        delete loaded;
    }
    paths[3] = (dir / "ps_missing.wav").string();
    CHECK(ps::StemBundle::load(paths, kRate) == nullptr);
    for (int st = 0; st < 3; ++st) std::filesystem::remove(paths[static_cast<size_t>(st)]);
}

// RMS of a stereo-identical sine through an insert, after it settles.
float sineGainDb(ps::InsertSlot& fx, float freq, float amp = 0.25f) {
    std::vector<float> l(9600), r(9600);
    double sum = 0, ref = 0;
    for (int pass = 0; pass < 2; ++pass) {
        for (size_t i = 0; i < l.size(); ++i) l[i] = r[i] = amp * static_cast<float>(std::sin(2 * M_PI * freq * (pass * 9600 + i) / kRate));
        fx.process(l.data(), r.data(), 9600);
    }
    for (size_t i = 4800; i < l.size(); ++i) {
        sum += l[i] * l[i];
        const double x = amp * std::sin(2 * M_PI * freq * (9600 + i) / kRate);
        ref += x * x;
    }
    return static_cast<float>(10 * std::log10(sum / ref));
}

void testInsertEffects() {
    ps::InsertSlot slot;
    slot.prepare(kRate);
    // Off: untouched.
    CHECK(std::fabs(sineGainDb(slot, 1000)) < 0.01f);
    // EQ: +12 dB low shelf boosts 50 Hz by ~12 dB, leaves 5 kHz alone; flat when centred.
    slot.setType(ps::InsertSlot::Eq);
    CHECK(std::fabs(sineGainDb(slot, 1000)) < 0.05f);
    slot.setParam(0, 1.0f);
    const float low = sineGainDb(slot, 50), high = sineGainDb(slot, 5000);
    std::printf("  EQ low +12 dB: 50 Hz %+.1f dB, 5 kHz %+.1f dB\n", low, high);
    CHECK(low > 10.5f && low < 12.5f);
    CHECK(std::fabs(high) < 0.5f);
    // Compressor: a loud sine is pulled down, a quiet one (below threshold) only gets make-up.
    slot.setType(ps::InsertSlot::Comp);
    slot.setParam(0, 0.5f);   // threshold -20 dB
    slot.setParam(1, 0.54f);  // ratio ~5
    const float loud = sineGainDb(slot, 200, 0.9f);
    const float reduction = slot.compressor().gainReductionDb();
    const float quiet = sineGainDb(slot, 200, 0.02f);
    std::printf("  compressor: loud %+.1f dB, quiet %+.1f dB, reduction %.1f dB\n", loud, quiet, reduction);
    CHECK(loud < quiet - 6.0f);  // the loud one gains much less than the quiet one
    CHECK(reduction < -3.0f);
    // Saturation: adds harmonics (energy at 3 kHz from a 1 kHz sine), stays finite and bounded.
    slot.setType(ps::InsertSlot::Saturation);
    slot.setParam(0, 0.9f);
    slot.setParam(1, 1.0f);
    std::vector<float> l(4800), r(4800);
    for (size_t i = 0; i < l.size(); ++i) l[i] = r[i] = 0.8f * static_cast<float>(std::sin(2 * M_PI * 1000.0 * i / kRate));
    slot.process(l.data(), r.data(), 4800);
    double h3 = 0, h3s = 0, peak = 0;
    for (size_t i = 0; i < l.size(); ++i) {
        h3 += l[i] * std::cos(2 * M_PI * 3000.0 * i / kRate);
        h3s += l[i] * std::sin(2 * M_PI * 3000.0 * i / kRate);
        peak = std::max(peak, static_cast<double>(std::fabs(l[i])));
        CHECK(std::isfinite(l[i]));
    }
    CHECK(std::sqrt(h3 * h3 + h3s * h3s) / 2400.0 > 0.01);
    CHECK(peak < 2.0);
    // Chorus and filter: finite, and the low-pass filter darkens a high tone.
    slot.setType(ps::InsertSlot::Chorus);
    CHECK(std::isfinite(sineGainDb(slot, 440)));
    slot.setType(ps::InsertSlot::Filter);
    slot.setParam(0, 0.3f);  // ~400 Hz low-pass
    CHECK(sineGainDb(slot, 8000) < -20.0f);
    slot.setType(99);  // ignored
    CHECK(slot.type() == ps::InsertSlot::Filter);
}

void testSendEffects() {
    // Delay: an impulse comes back after exactly one eighth note (2 sixteenths) at 120 BPM.
    ps::TempoDelay delay;
    delay.prepare(kRate);
    delay.setParam(0, 0.2f);  // 1/8
    delay.setParam(1, 0.5f);
    delay.setParam(2, 0.0f);  // no ping-pong
    delay.setStepFrames(6000.0);
    delay.reset();
    std::vector<float> inL(kRate, 0.0f), inR(kRate, 0.0f), outL(kRate, 0.0f), outR(kRate, 0.0f);
    inL[100] = inR[100] = 1.0f;
    delay.process(inL.data(), inR.data(), outL.data(), outR.data(), kRate);
    const auto first = std::max_element(outL.begin(), outL.begin() + 20000) - outL.begin();
    std::printf("  delay echo at frame %ld (expected %d)\n", static_cast<long>(first), 100 + 12000);
    CHECK(std::labs(static_cast<long>(first) - 12100) <= 1);
    CHECK(outL[24100] > 0.05f && outL[24100] < outL[12100]);  // second repeat, quieter
    // Ping-pong: the first repeat is on the left only, the second on the right.
    delay.setParam(2, 1.0f);
    delay.reset();
    std::fill(outL.begin(), outL.end(), 0.0f);
    std::fill(outR.begin(), outR.end(), 0.0f);
    delay.process(inL.data(), inR.data(), outL.data(), outR.data(), kRate);
    CHECK(outL[12100] > 0.3f && std::fabs(outR[12100]) < 1e-3f);
    CHECK(std::fabs(outL[24100]) < 1e-3f && outR[24100] > 0.05f);

    // Reverb: a tail that lasts, decays and stays finite.
    ps::Reverb reverb;
    reverb.prepare(kRate);
    std::fill(outL.begin(), outL.end(), 0.0f);
    std::fill(outR.begin(), outR.end(), 0.0f);
    reverb.process(inL.data(), inR.data(), outL.data(), outR.data(), kRate);
    double early = 0, late = 0;
    for (int i = 0; i < kRate; ++i) {
        CHECK(std::isfinite(outL[static_cast<size_t>(i)]) && std::isfinite(outR[static_cast<size_t>(i)]));
        (i < kRate / 4 ? early : late) += outL[static_cast<size_t>(i)] * outL[static_cast<size_t>(i)];
    }
    CHECK(early > 0 && late > 0 && late < early * 3);
    bool differ = false;
    for (int i = 0; i < kRate; ++i) differ |= std::fabs(outL[static_cast<size_t>(i)] - outR[static_cast<size_t>(i)]) > 1e-4f;
    CHECK(differ);  // stereo
}

void testMixerInsertsAndSends() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    mixer.setStepFrames(6000.0);  // 120 BPM
    constexpr int frames = kRate;
    std::vector<float> click(frames, 0.0f), zero(frames, 0.0f), out(static_cast<size_t>(frames) * 2);
    click[100] = 0.5f;
    const float* ch[ps::Mixer::kChannels] = {click.data(), zero.data(), zero.data(), zero.data()};
    // Dry: the click alone, nothing after it.
    mixer.process(ch, nullptr, out.data(), frames, 2);
    CHECK(std::fabs(out[200] - 0.5f) < 1e-3f);
    CHECK(std::fabs(out[(100 + 12000) * 2]) < 1e-6f);
    // Delay send on the drums (1/8 at 120 BPM): an echo 12000 frames later, on the master.
    mixer.setBusParam(1, 0, 0.2f);
    mixer.setBusParam(1, 2, 0.0f);
    mixer.setSend(0, 1, 1.0f);
    mixer.process(ch, nullptr, out.data(), frames, 2);  // settle the send smoothing
    mixer.process(ch, nullptr, out.data(), frames, 2);
    float echo = 0.0f;
    for (int i = 100 + 12000 - 2; i <= 100 + 12000 + 2; ++i) echo = std::max(echo, std::fabs(out[static_cast<size_t>(i) * 2]));
    CHECK(echo > 0.1f);
    mixer.setSend(0, 1, 0.0f);
    // Reverb send: a tail after the click.
    mixer.setSend(0, 0, 1.0f);
    mixer.process(ch, nullptr, out.data(), frames, 2);
    mixer.process(ch, nullptr, out.data(), frames, 2);
    double tail = 0;
    for (int i = 5000; i < 20000; ++i) tail += std::fabs(out[static_cast<size_t>(i) * 2]);
    CHECK(tail > 0.1);
    mixer.setSend(0, 0, 0.0f);
    // Insert on channel 0 only: a low-pass filter darkens drums, not the synth.
    std::vector<float> hiss(frames);
    for (int i = 0; i < frames; ++i) hiss[static_cast<size_t>(i)] = 0.2f * static_cast<float>(std::sin(2 * M_PI * 8000.0 * i / kRate));
    const float* drumsOnly[ps::Mixer::kChannels] = {hiss.data(), zero.data(), zero.data(), zero.data()};
    const float* synthOnly[ps::Mixer::kChannels] = {zero.data(), zero.data(), hiss.data(), zero.data()};
    mixer.setInsertType(0, 2, ps::InsertSlot::Filter);
    mixer.setInsertParam(0, 2, 0, 0.3f);
    for (int pass = 0; pass < 3; ++pass) mixer.process(drumsOnly, nullptr, out.data(), frames, 2);
    float drumsPeak = 0.0f;
    for (int i = frames / 2; i < frames; ++i) drumsPeak = std::max(drumsPeak, std::fabs(out[static_cast<size_t>(i) * 2]));
    mixer.process(synthOnly, nullptr, out.data(), frames, 2);
    float synthPeak = 0.0f;
    for (int i = frames / 2; i < frames; ++i) synthPeak = std::max(synthPeak, std::fabs(out[static_cast<size_t>(i) * 2]));
    CHECK(drumsPeak < 0.02f && synthPeak > 0.15f);
    // Master insert: affects everything.
    mixer.setInsertType(ps::Mixer::kMaster, 0, ps::InsertSlot::Filter);
    mixer.setInsertParam(ps::Mixer::kMaster, 0, 0, 0.3f);
    for (int pass = 0; pass < 3; ++pass) mixer.process(synthOnly, nullptr, out.data(), frames, 2);
    synthPeak = 0.0f;
    for (int i = frames / 2; i < frames; ++i) synthPeak = std::max(synthPeak, std::fabs(out[static_cast<size_t>(i) * 2]));
    CHECK(synthPeak < 0.02f);
    CHECK(mixer.insert(ps::Mixer::kMaster, 0).type() == ps::InsertSlot::Filter);
}

void testSidechainPumpFollowsTheKick() {
    // A steady synth pad, a pump on its channel, the kick on steps 0 and 8 of 16 at 120 BPM.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 8});
    engine.post({ps::CommandType::SetChannelMute, 1.0f, 0});  // hear only the pumped channel
    engine.post({ps::CommandType::SetInsertType, 0.0f, 2 * ps::Mixer::kInserts + 0, ps::InsertSlot::Pump});
    engine.post({ps::CommandType::SetInsertParam, 1.0f, 2 * ps::Mixer::kInserts + 0, 0});  // full depth
    engine.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
    render(engine, kRate / 2, 256);  // the pad settles
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto out = render(engine, kRate * 2, 256);  // one bar: kicks at 0 and 1 s
    auto rms = [&](int from, int len) {
        double s = 0;
        for (int i = from; i < from + len; ++i) s += double(out[static_cast<size_t>(i) * 2]) * out[static_cast<size_t>(i) * 2];
        return std::sqrt(s / len);
    };
    const double before = rms(kRate * 1 - 4800, 2400);  // just before the second kick: recovered
    const double ducked = rms(kRate * 1 + 240, 960);    // 5..25 ms after it
    const double between = rms(kRate / 2, 2400);        // half a beat later, between kicks
    std::printf("  pump: before kick %.4f, on kick %.4f, between %.4f\n", before, ducked, between);
    CHECK(ducked < before * 0.2);
    CHECK(between > ducked * 3);
    // "Every beat": steps 4 and 12 pump too, even without a kick there.
    engine.post({ps::CommandType::SetInsertParam, 1.0f, 2 * ps::Mixer::kInserts + 0, 2});
    const auto beats = render(engine, kRate * 2, 256);
    double onBeat2 = 0, offBeat = 0;
    for (int i = kRate / 2 + 240; i < kRate / 2 + 1200; ++i) onBeat2 += std::fabs(beats[static_cast<size_t>(i) * 2]);
    for (int i = kRate / 2 - 3600; i < kRate / 2 - 2640; ++i) offBeat += std::fabs(beats[static_cast<size_t>(i) * 2]);
    CHECK(onBeat2 < offBeat * 0.3);
}

void testAutomationMovesTheFader() {
    // Synth pad at full volume; automation pulls the synth fader to 0 on step 4 and back on step 8.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
    engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
    engine.post({ps::CommandType::SetAutomationPoint, 0.0f, 0, 4});
    engine.post({ps::CommandType::SetAutomationPoint, 0.8f, 0, 8});
    engine.post({ps::CommandType::SetAutomationPoint, -1.0f, 0, 63});  // removing an empty point is harmless
    render(engine, kRate / 2, 256);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto out = render(engine, kRate * 2, 256);  // 16 steps of 6000 frames
    auto rms = [&](int from) {
        double s = 0;
        for (int i = from; i < from + 2400; ++i) s += double(out[static_cast<size_t>(i) * 2]) * out[static_cast<size_t>(i) * 2];
        return std::sqrt(s / 2400);
    };
    const double loud = rms(6000 * 2), silent = rms(6000 * 5), back = rms(6000 * 9);
    std::printf("  automation: step 2 %.4f, step 5 %.4f, step 9 %.4f\n", loud, silent, back);
    CHECK(loud > 0.02 && silent < loud * 0.01 && back > loud * 0.5);
    // Turning the lane off stops it touching the fader.
    engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, -1});
    engine.post({ps::CommandType::SetChannelVolume, 0.8f, 2});
    const auto after = render(engine, kRate * 2, 256);
    double s = 0;
    for (int i = 6000 * 5; i < 6000 * 5 + 2400; ++i) s += double(after[static_cast<size_t>(i) * 2]) * after[static_cast<size_t>(i) * 2];
    CHECK(std::sqrt(s / 2400) > loud * 0.5);  // the pad's level wanders a little with its timbre
}

void testScenesSwapOnTheBar() {
    ps::Engine engine(kRate);
    // Scene A plays: pad 1 on step 0. Scene B waits: pad 3 on steps 0 and 8, a 32-step loop.
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    engine.post({ps::CommandType::SetStaging, 1.0f});
    for (int lane = 0; lane < ps::Engine::kLanes; ++lane)
        for (int st = 0; st < ps::Engine::kMaxSteps; ++st) engine.post({ps::CommandType::SetDrumHit, 0.0f, lane, st});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 2, 0});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 2, 8});
    engine.post({ps::CommandType::SetPatternSteps, 0.0f, 32});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 32});  // after two bars of scene A
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, kRate * 4 - 1000, 256);  // just under 2 bars at 120 BPM
    CHECK(engine.laneTriggerCount(0) == 2 && engine.laneTriggerCount(2) == 0 && engine.sceneSwaps() == 0);
    render(engine, kRate * 4, 256);  // bars 3-4: scene B, counted from its own start
    std::printf("  scenes: pad1 %u, pad3 %u, swaps %u\n", engine.laneTriggerCount(0), engine.laneTriggerCount(2), engine.sceneSwaps());
    CHECK(engine.laneTriggerCount(0) == 2);  // scene A's kick is gone
    CHECK(engine.laneTriggerCount(2) == 2);  // scene B: steps 0 and 8 of its 32-step loop
    CHECK(engine.sceneSwaps() == 1);
    CHECK(engine.absoluteStep() >= 0 && engine.absoluteStep() < 32);  // steps count from the scene's start
    // Swap "at the next bar" while a bar is under way.
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetDrumHit, 0.0f, 2, 0});
    engine.post({ps::CommandType::SetDrumHit, 0.0f, 2, 8});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 5, 0});
    engine.post({ps::CommandType::SetPatternSteps, 0.0f, 16});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    render(engine, kRate / 2, 256);  // a quarter into a bar
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    render(engine, kRate * 2, 256);
    CHECK(engine.sceneSwaps() == 2 && engine.laneTriggerCount(5) == 1);
    // Cancel: an armed swap that never happens.
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, -1});
    render(engine, kRate * 3, 256);
    CHECK(engine.sceneSwaps() == 2);
    // A staging burst whose "off" got lost: stopping the transport ends it, so later edits are heard.
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetTransport, 0.0f});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 7, 0});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto before7 = engine.laneTriggerCount(7);
    render(engine, kRate, 256);
    CHECK(engine.laneTriggerCount(7) == before7 + 1);
    // Staged acid and piano-roll notes arrive with the swap.
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetAcidStep, 0.0f, 0, ps::AcidStep{0, 0, false, false, true}.pack()});
    engine.post({ps::CommandType::SetAcidLength, 0.0f, 16});
    engine.post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 12, 60)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});  // ends staging by itself
    const auto acidBefore = engine.acidNotesPlayed();
    render(engine, kRate * 3, 256);
    CHECK(engine.sceneSwaps() == 3);
    CHECK(engine.acidNotesPlayed() > acidBefore);
}

void testPerformanceEffects() {
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    fx.setStepFrames(6000.0);  // 120 BPM
    constexpr int n = kRate;
    // A rising ramp: easy to see what is replayed.
    std::vector<float> l(n), r(n);
    auto feed = [&](int frames, int from) {
        for (int i = 0; i < frames; ++i) l[static_cast<size_t>(i)] = r[static_cast<size_t>(i)] = 0.001f * static_cast<float>((from + i) % 1000);
        fx.process(l.data(), r.data(), frames);
    };
    feed(n, 0);
    CHECK(!fx.active());
    CHECK(std::fabs(l[500] - 0.5f) < 1e-6f);  // untouched when nothing is held
    // Repeat 1/16 (6000 frames): after the fade, the output repeats with a 6000-frame period.
    fx.setParam(ps::PerformanceFx::RepeatDivision, 0.6f);
    fx.setParam(ps::PerformanceFx::Repeat, 1.0f);
    feed(n, 0);
    float worst = 0.0f;
    for (int i = 2000; i < 2000 + 3000; ++i) worst = std::max(worst, std::fabs(l[static_cast<size_t>(i)] - l[static_cast<size_t>(i + 6000)]));
    std::printf("  repeat: loop mismatch %.5f\n", worst);
    CHECK(worst < 1e-4f);
    fx.setParam(ps::PerformanceFx::Repeat, 0.0f);
    feed(n, 0);
    CHECK(std::fabs(l[n - 100] - 0.001f * ((n - 100) % 1000)) < 1e-4f);  // back to live
    // Tape stop: the level falls to silence within a second.
    fx.setParam(ps::PerformanceFx::TapeStop, 1.0f);
    for (int i = 0; i < n; ++i) l[static_cast<size_t>(i)] = r[static_cast<size_t>(i)] = 0.5f * static_cast<float>(std::sin(2 * M_PI * 440 * i / kRate));
    fx.process(l.data(), r.data(), n);
    float tail = 0.0f;
    for (int i = n - 4800; i < n; ++i) tail = std::max(tail, std::fabs(l[static_cast<size_t>(i)]));
    CHECK(tail < 1e-3f);
    fx.setParam(ps::PerformanceFx::TapeStop, 0.0f);
    // Chopper: silent in the second half of each sixteenth, open in the first.
    fx.setParam(ps::PerformanceFx::Chop, 1.0f);
    std::vector<float> ones(24000, 0.5f), ones2(24000, 0.5f);
    fx.onStep(0);
    fx.process(ones.data(), ones2.data(), 24000);
    CHECK(ones[6000 + 1500] > 0.45f && ones[6000 + 4500] < 0.01f);
    fx.setParam(ps::PerformanceFx::Chop, 0.0f);
    // DJ filter: low-pass at the left kills a high tone; centred, no change.
    fx.setParam(ps::PerformanceFx::Filter, 0.1f);
    for (int i = 0; i < n; ++i) l[static_cast<size_t>(i)] = r[static_cast<size_t>(i)] = 0.5f * static_cast<float>(std::sin(2 * M_PI * 6000 * i / kRate));
    fx.process(l.data(), r.data(), n);
    float hi = 0.0f;
    for (int i = n / 2; i < n; ++i) hi = std::max(hi, std::fabs(l[static_cast<size_t>(i)]));
    CHECK(hi < 0.05f);
    for (int i = 0; i < n; ++i) CHECK(std::isfinite(l[static_cast<size_t>(i)]));
}

// Fundamental frequency by autocorrelation (lag with the strongest correlation within 60..1000 Hz).
double pitchHz(const std::vector<float>& x, int from, int len) {
    int best = 0;
    double bestC = -1e30;
    for (int lag = kRate / 1000; lag <= kRate / 60; ++lag) {
        double c = 0;
        for (int i = from; i < from + len; ++i) c += double(x[static_cast<size_t>(i)]) * x[static_cast<size_t>(i + lag)];
        if (c > bestC) { bestC = c; best = lag; }
    }
    // Parabolic refinement around the peak.
    auto corr = [&](int lag) { double c = 0; for (int i = from; i < from + len; ++i) c += double(x[static_cast<size_t>(i)]) * x[static_cast<size_t>(i + lag)]; return c; };
    const double a = corr(best - 1), b = bestC, c = corr(best + 1);
    const double shift = 0.5 * (a - c) / (a - 2 * b + c);
    return kRate / (best + shift);
}

void testPlaitsVoices() {
    ps::PlaitsSynth plaits;
    plaits.prepare(kRate);
    // Model 0 (virtual analog), soft timbre: A4 must be 440 Hz (the 47872 Hz tuning compensated).
    plaits.setParam(ps::PlaitsSynth::Model, 0.0f);
    plaits.setParam(ps::PlaitsSynth::Harmonics, 0.5f);  // (0 would add detuned sub-octaves)
    plaits.setParam(ps::PlaitsSynth::Timbre, 0.5f);
    plaits.setParam(ps::PlaitsSynth::Morph, 0.5f);
    plaits.noteOn(69, 1.0f, 0);
    std::vector<float> out(kRate, 0.0f);
    plaits.render(out.data(), kRate);
    const double f = pitchHz(out, kRate / 2, 4800);
    std::printf("  plaits A4: %.2f Hz\n", f);
    CHECK(std::fabs(f - 440.0) < 1.0);
    float peak = 0.0f;
    for (float v : out) { peak = std::max(peak, std::fabs(v)); CHECK(std::isfinite(v)); }
    CHECK(peak > 0.05f && peak < 1.0f);
    // Release: silent soon after the note-off.
    plaits.noteOff(69, 0);
    std::fill(out.begin(), out.end(), 0.0f);
    plaits.render(out.data(), kRate);
    float tail = 0.0f;
    for (int i = kRate / 2; i < kRate; ++i) tail = std::max(tail, std::fabs(out[static_cast<size_t>(i)]));
    CHECK(tail < 1e-3f);
    CHECK(plaits.activeVoices() == 0);
    // Every model makes sound, stays finite and bounded; six voices at once within a CPU budget.
    for (int model = 0; model < ps::PlaitsSynth::kModels; ++model) {
        plaits.setParam(ps::PlaitsSynth::Model, model / float(ps::PlaitsSynth::kModels - 1));
        for (int v = 0; v < ps::PlaitsSynth::kVoices; ++v) plaits.noteOn(48 + v * 3, 0.9f, v * 7);
        std::vector<float> buf(kRate / 4, 0.0f);
        const auto start = std::chrono::steady_clock::now();
        plaits.render(buf.data(), kRate / 4);
        const double load = std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count() / 0.25;
        float pk = 0.0f;
        bool finite = true;
        for (float v : buf) { pk = std::max(pk, std::fabs(v)); finite &= std::isfinite(v); }
        if (!finite || pk < 1e-3f || pk > 3.0f || load > 0.12) std::printf("  model %d: peak %.3f load %.0f %% finite %d\n", model, pk, load * 100, finite);
        CHECK(finite && pk > 1e-3f && pk < 3.0f);
        CHECK(load < 0.12);  // heavy models are capped in voices: any model stays cheap here
        plaits.allNotesOff();
        std::vector<float> flush(kRate, 0.0f);
        plaits.render(flush.data(), kRate);
    }
}

void testPlaitsThroughEngine() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetSynthEngine, 0.0f, 1});
    engine.post({ps::CommandType::SetPlaitsParam, 2.0f / 23.0f, ps::PlaitsSynth::Model});  // six-op FM
    for (int n = 0; n < 8; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n, ps::NoteTrack::pack(n * 48, 40, 48 + (n % 4) * 5)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 8});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> out(512 * 2);
    for (int i = 0; i < 40; ++i) engine.process(nullptr, 0, 0, out.data(), 512, 2);  // warm-up
#ifdef PS_COUNT_ALLOCATIONS
    g_allocations = 0;
    g_countAllocations = true;
#endif
    float peak = 0.0f;
    for (int i = 0; i < 300; ++i) {
        if (i % 60 == 0) engine.post({ps::CommandType::SetPlaitsParam, (i % 120) / 120.0f, ps::PlaitsSynth::Timbre});
        engine.process(nullptr, 0, 0, out.data(), 512, 2);
        for (float v : out) peak = std::max(peak, std::fabs(v));
    }
#ifdef PS_COUNT_ALLOCATIONS
    g_countAllocations = false;
    CHECK(g_allocations.load() == 0);
#endif
    CHECK(peak > 0.02f);
    // Back to the subtractive synth while notes sound: Plaits goes quiet, nothing hangs.
    engine.post({ps::CommandType::SetTransport, 0.0f});
    engine.post({ps::CommandType::SetSynthEngine, 0.0f, 0});
    render(engine, kRate, 256);
    const auto quiet = render(engine, kRate / 2, 256);
    float rest = 0.0f;
    for (float v : quiet) rest = std::max(rest, std::fabs(v));
    CHECK(rest < 1e-3f);
}

struct TestCase {
    const char* name;
    std::function<void()> fn;
};

}  // namespace

ps::Sample* constantSample(int frames, int channels, float level) {
    std::vector<float> d(static_cast<size_t>(frames) * channels, level);
    return ps::Sample::create(d.data(), frames, channels, kRate, kRate);
}

void installClip(ps::Engine& engine, int pad, ps::Sample* s) {
    ps::Command c{ps::CommandType::InstallClip, 0.0f, pad, 0};
    c.sample = s;  // the engine owns this reference
    engine.post(c);
}

void testLivePadsLoopOnTheBeat() {
    ps::Engine engine(kRate);  // 120 BPM: a beat is 24000 frames
    // Pad 0: a 100-frame click set to 1 beat = four on the floor.
    installClip(engine, 0, constantSample(100, 1, 0.5f));
    engine.post({ps::CommandType::SetClipParam, 1.0f, 0, ps::ClipLauncher::Beats});
    // Transport stopped: the first pad starts at once.
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    auto out = render(engine, kRate * 2, 256);
    CHECK(engine.clipState(0) == ps::ClipLauncher::Playing);
    for (int b = 0; b < 4; ++b) {
        const size_t at = static_cast<size_t>(b * 24000 + 50) * 2;
        CHECK(std::fabs(out[at] - 0.5f) < 1e-3f && std::fabs(out[at + 1] - 0.5f) < 1e-3f);
        CHECK(std::fabs(out[static_cast<size_t>(b * 24000 + 12000) * 2]) < 1e-6f);  // silent between hits
    }
    // Pad 1: a stereo loop of 2 beats, launched mid-beat: waits for the next 2-beat line.
    installClip(engine, 1, constantSample(48000, 2, 0.1f));
    engine.post({ps::CommandType::SetClipParam, 2.0f, 1, ps::ClipLauncher::Beats});
    render(engine, 6000, 256);  // beat 4.25
    engine.post({ps::CommandType::ToggleClip, 0.0f, 1});
    render(engine, 256, 256);
    CHECK(engine.clipState(1) == ps::ClipLauncher::Waiting);
    out = render(engine, 48000 - 6256 - 1000, 256);  // just before beat 6
    CHECK(engine.clipState(1) == ps::ClipLauncher::Waiting);
    out = render(engine, 2000, 250);
    CHECK(engine.clipState(1) == ps::ClipLauncher::Playing);
    CHECK(std::fabs(out[static_cast<size_t>(1500) * 2 + 1] - 0.1f) < 1e-3f);  // right: the loop alone
    // Off again: a short fade, then silence; the other pad keeps going.
    engine.post({ps::CommandType::ToggleClip, 0.0f, 1});
    out = render(engine, 2000, 256);
    CHECK(engine.clipState(1) == ps::ClipLauncher::Off);
    CHECK(std::fabs(out[static_cast<size_t>(1500) * 2 + 1]) < 1e-6f);
    CHECK(engine.clipState(0) == ps::ClipLauncher::Playing);
    // A one-shot plays once and goes back to off by itself.
    installClip(engine, 2, constantSample(2000, 1, 0.3f));
    engine.post({ps::CommandType::SetClipParam, 1.0f, 2, ps::ClipLauncher::OneShot});
    engine.post({ps::CommandType::SetClipParam, 1.0f, 2, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 2});
    render(engine, 30000, 256);
    CHECK(engine.clipState(2) == ps::ClipLauncher::Off);
    // While the transport plays, the clock follows its beats.
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 24000 * 3 + 100, 256);
    const double beat = engine.clipBeat();
    CHECK(std::fabs(beat - std::round(beat) - 100.0 / 24000.0) < 1e-6);
    // All off; an empty pad ignores taps; the samples go back to be freed.
    engine.post({ps::CommandType::ToggleClip, 0.0f, -1});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 5});
    out = render(engine, 24000 * 2, 256);
    CHECK(engine.clipState(0) == ps::ClipLauncher::Off && engine.clipState(5) == ps::ClipLauncher::Off);
    float peak = 0.0f;
    for (size_t i = 2000; i < out.size(); ++i) peak = std::max(peak, std::fabs(out[i]));
    CHECK(peak < 1e-6f);
    for (int pad = 0; pad < 3; ++pad) installClip(engine, pad, nullptr);
    render(engine, 256, 256);
    engine.collectGarbage();
}

void testSongExportSwapsScenesOnTime() {
    // Scene A: a kick on every beat. Scene B: the same kick on beat 1 only. Song: A then B.
    const int64_t bar = kRate * 2;
    auto setup = [](ps::OfflineRenderer& r) {
        r.post({ps::CommandType::SetTempo, 120.0f});
        for (int step = 0; step < 16; step += 4) r.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
    };
    ps::OfflineRenderer song(kRate);
    setup(song);
    song.scheduleFrom(3000);  // half a step into A: stage B, swap after A's 16 steps
    song.post({ps::CommandType::SetStaging, 1.0f});
    for (int step = 0; step < 16; step += 4) song.post({ps::CommandType::SetDrumHit, step == 0 ? 1.0f : 0.0f, 0, step});
    song.post({ps::CommandType::SetStaging, 0.0f});
    song.post({ps::CommandType::ArmSceneSwap, 0.0f, 16});
    song.scheduleFrom(-1);
    const auto a = song.renderToBuffer(bar * 2, 0);
    ps::OfflineRenderer loop(kRate);
    setup(loop);
    const auto b = loop.renderToBuffer(bar * 2, 0);
    CHECK(a.size() == b.size() && a.size() == size_t(bar * 2) * 2);
    auto peakIn = [](const std::vector<float>& x, int64_t from, int64_t to) {
        float p = 0.0f;
        for (int64_t i = from; i < to; ++i) p = std::max(p, std::fabs(x[size_t(i) * 2]));
        return p;
    };
    float firstBarDiff = 0.0f;
    for (int64_t i = 0; i < bar; ++i) firstBarDiff = std::max(firstBarDiff, std::fabs(a[size_t(i) * 2] - b[size_t(i) * 2]));
    std::printf("  song export: bar 1 diff %.5f, bar 2 beat 1 %.3f, beat 3 %.3f (loop %.3f)\n", firstBarDiff,
                peakIn(a, bar, bar + 2000), peakIn(a, bar + bar / 2, bar + bar / 2 + 2000), peakIn(b, bar + bar / 2, bar + bar / 2 + 2000));
    CHECK(firstBarDiff < 1e-6f);                                         // staging does not touch what plays
    CHECK(peakIn(a, bar, bar + 2000) > 0.1f);                            // B starts on the bar with its kick
    CHECK(peakIn(a, bar + bar / 2, bar + bar / 2 + 2000) < 0.25f * peakIn(b, bar + bar / 2, bar + bar / 2 + 2000));  // no kick on beat 3
}

void testDrumRollsSplitTheStep() {
    ps::Engine engine(kRate);  // 120 BPM: a sixteenth is 6000 frames
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    engine.post({ps::CommandType::SetDrumRepeat, 4.0f, 0, 0});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 2, 4});  // a plain hit: one trigger
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 1400, 256);
    CHECK(engine.laneTriggerCount(0) == 1);   // second hit at 1500 frames
    render(engine, 200, 256);
    CHECK(engine.laneTriggerCount(0) == 2);
    render(engine, 6000 * 4 - 1600 + 100, 256);
    CHECK(engine.laneTriggerCount(0) == 4);   // four in the step, none after
    CHECK(engine.laneTriggerCount(2) == 1);
    // Back to a plain hit; stopping drops rolls still to come.
    engine.post({ps::CommandType::SetDrumRepeat, 1.0f, 0, 0});
    render(engine, 6000 * 12, 512);
    CHECK(engine.laneTriggerCount(0) == 5);
}

void testPlayPositionForLiveRecording() {
    ps::Engine engine(kRate);  // 120 BPM: a sixteenth is 6000 frames
    CHECK(engine.playPosition() < 0.0);
    engine.post({ps::CommandType::SetTransport, 1.0f, 1});  // one bar of count-in
    render(engine, 6000 * 8, 256);
    CHECK(engine.playPosition() < 0.0);                     // still counting in
    render(engine, 6000 * 8 + 6000 * 5 + 3000, 256);        // end of count-in, then 5.5 steps
    const double pos = engine.playPosition();
    std::printf("  play position: %.3f steps (expected 5.5)\n", pos);
    CHECK(std::fabs(pos - 5.5) < 256.0 / 6000.0 + 1e-6);    // within one block
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 256, 256);
    CHECK(engine.playPosition() < 0.0);
}

void testStemsStretchToTheProjectTempo() {
    // A 1 kHz tone on every stem: 120 BPM song played at 100 BPM lasts 1.2 times as long, same pitch.
    std::vector<float> tone(static_cast<size_t>(kRate) * 2 * 2);
    for (size_t i = 0; i < tone.size() / 2; ++i) tone[i * 2] = tone[i * 2 + 1] = 0.4f * static_cast<float>(std::sin(2 * M_PI * 1000.0 * i / kRate));
    auto* bundle = ps::StemBundle::fromFloat(std::vector<std::vector<float>>(4, tone));
    auto* slow = bundle->stretched(1.2, kRate);
    CHECK(slow != nullptr);
    if (slow) {
        CHECK(slow->frames() == std::llround(bundle->frames() * 1.2));
        const auto x = slow->extract(2, kRate / 2, kRate / 2);  // half a second from the middle
        int crossings = 0;
        float peak = 0.0f;
        for (size_t i = 2; i < x.size(); i += 2) {
            if ((x[i - 2] < 0.0f) != (x[i] < 0.0f)) ++crossings;
            peak = std::max(peak, std::fabs(x[i]));
        }
        std::printf("  stretched stems: %.0f Hz, peak %.2f\n", crossings / 2.0 / 0.5, peak);
        CHECK(std::abs(crossings / 2.0 / 0.5 - 1000.0) < 15.0);
        CHECK(peak > 0.3f && peak < 0.5f);
        CHECK(slow->refCount() == 1);
        CHECK(slow->release());
        delete slow;
    }
    CHECK(bundle->stretched(10.0, kRate) == nullptr);
    CHECK(bundle->release());
    delete bundle;
}

void testStemPreviewPlaysWithoutTheTransport() {
    const float levels[4] = {0.05f, 0.1f, 0.15f, 0.2f};
    std::vector<std::vector<float>> stems(4);
    for (int st = 0; st < 4; ++st) stems[st].assign(static_cast<size_t>(kRate) * 2 * 2, levels[st]);
    auto* bundle = ps::StemBundle::fromFloat(stems);
    ps::Engine engine(kRate);
    ps::Command install{ps::CommandType::InstallStems, 0.0f, 1};
    install.stems = bundle;
    engine.post(install);
    engine.post({ps::CommandType::PlayPreview, 1.0f});
    auto out = render(engine, 4800, 256);
    CHECK(std::fabs(out[out.size() - 2] - 0.5f) < 2e-3f);  // all four together
    CHECK(engine.previewPosition() > 0 && engine.stemPosition() == -1);
    // Switch stems on and off while it plays: it never stops.
    engine.post({ps::CommandType::SetPreviewParam, 1.0f, 3, ps::StemPlayer::Mute});
    engine.post({ps::CommandType::SetPreviewParam, 1.0f, 1, ps::StemPlayer::Mute});
    out = render(engine, 9600, 256);  // past the 20 ms smoothing
    CHECK(std::fabs(out[out.size() - 2] - 0.2f) < 2e-3f);
    engine.post({ps::CommandType::SetPreviewParam, 0.0f, 3, ps::StemPlayer::Mute});
    out = render(engine, 9600, 256);
    CHECK(std::fabs(out[out.size() - 2] - 0.4f) < 2e-3f);
    // The transport stopping does not stop it; its own stop does.
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 4800, 256);
    CHECK(engine.previewPosition() > 0);
    engine.post({ps::CommandType::PlayPreview, 0.0f});
    out = render(engine, 4800, 256);
    CHECK(std::fabs(out[out.size() - 2]) < 1e-6f && engine.previewPosition() == -1);
    engine.post({ps::CommandType::InstallStems, 0.0f, 1});
    render(engine, 256, 256);
    engine.collectGarbage();
}

int main(int argc, char** argv) {
    for (int i = 1; i < argc; ++i) {
        if (std::strcmp(argv[i], "--update-references") == 0) g_updateReferences = true;
    }
    const TestCase tests[] = {
        {"spsc_queue", testSpscQueue},
        {"spsc_float_ring", testSpscFloatRing},
        {"silent_when_stopped", testSilentWhenStopped},
        {"sine_four_bars", testSineFourBars},
        {"block_size_independence", testBlockSizeIndependence},
        {"start_ramps_up", testStartRampsUp},
        {"stop_fades_out", testStopFadesOut},
        {"settings_survive_prepare", testSettingsSurvivePrepare},
        {"reference_render", testReferenceRender},
        {"round_trip_meter", testRoundTripMeter},
        {"latency_test_through_engine", testLatencyTestThroughEngine},
        {"sequencer_timing", testSequencerTiming},
        {"sequencer_swing", testSequencerSwing},
        {"sequencer_tempo_change", testSequencerTempoChangeKeepsGroove},
        {"count_in_and_pattern_triggers", testCountInAndPatternTriggers},
        {"metronome_sample_accurate", testMetronomeIsSampleAccurate},
        {"every_drum_voice", testEveryDrumVoiceSoundsAndDecays},
        {"drum_velocity", testVelocityChangesLoudness},
        {"hat_choke", testClosedHatChokesOpenHat},
        {"pattern_drives_drums", testPatternDrivesDrums},
        {"full_kit_cpu", testFullKitCpuBudget},
        {"kicks_are_punchy", testKicksArePunchy},
        {"toms_have_body", testTomsHaveBody},
        {"acid_step_packing", testAcidStepPacking},
        {"acid_notes_on_step", testAcidNotesStartOnTheStep},
        {"acid_accent", testAcidAccentIsLouder},
        {"acid_slide", testAcidSlideHoldsTheNote},
        {"acid_stops_cleanly", testAcidStopsCleanly},
        {"acid_cpu", testAcidCpuBudget},
        {"synth_pitch", testSynthPitchIsRight},
        {"synth_on_time_and_release", testSynthNoteStartsOnTimeAndReleases},
        {"synth_voices_and_cpu", testSynthVoiceStealingAndCpu},
        {"synth_through_engine", testSynthThroughEngine},
        {"mixer_pan_mute_solo", testMixerPanMuteSolo},
        {"limiter_ceiling", testLimiterHoldsTheCeiling},
        {"wav_writer", testWavWriterRoundTrip},
        {"offline_export", testOfflineExportRendersTheBeat},
        {"sample_resample", testSampleResamplesCleanly},
        {"pad_sample", testPadSampleReplacesTheDrum},
        {"sample_refs", testSampleReferencesAreNeverLeakedOrFreedEarly},
        {"sample_retrigger", testRetriggerFadesInsteadOfClicking},
        {"pad_pitch_region_reverse", testPadPitchRegionAndReverse},
        {"pad_loop_envelope_choke", testPadLoopEnvelopeAndChoke},
        {"pad_param_commands", testPadParamCommandsReachTheSampler},
        {"onsets", testOnsetsFindTheHits},
        {"time_stretch", testTimeStretchKeepsPitchAndLength},
        {"piano_roll", testPianoRollNotesAreSampleAccurate},
        {"instrument", testInstrumentPlaysTheSoundFont},
        {"insert_effects", testInsertEffects},
        {"send_effects", testSendEffects},
        {"mixer_inserts_sends", testMixerInsertsAndSends},
        {"sidechain_pump", testSidechainPumpFollowsTheKick},
        {"automation", testAutomationMovesTheFader},
        {"scenes", testScenesSwapOnTheBar},
        {"performance_fx", testPerformanceEffects},
        {"plaits", testPlaitsVoices},
        {"plaits_engine", testPlaitsThroughEngine},
        {"tempo_and_key", testTempoAndKeyDetection},
        {"stems_follow_transport", testStemsPlayInStepWithTheTransport},
        {"live_pads", testLivePadsLoopOnTheBeat},
        {"stem_preview", testStemPreviewPlaysWithoutTheTransport},
        {"stems_stretch", testStemsStretchToTheProjectTempo},
        {"play_position", testPlayPositionForLiveRecording},
        {"song_export", testSongExportSwapsScenesOnTime},
        {"drum_rolls", testDrumRollsSplitTheStep},
        {"no_alloc_in_process", testProcessNeverAllocates},
    };
    for (const auto& t : tests) {
        const int before = g_failures;
        t.fn();
        std::printf("[%s] %s\n", g_failures == before ? " OK " : "FAIL", t.name);
    }
    std::printf("%s (%d failure%s)\n", g_failures == 0 ? "ALL PASSED" : "FAILURES", g_failures,
                g_failures == 1 ? "" : "s");
    return g_failures == 0 ? 0 : 1;
}
