// Host-side tests for the platform-independent audio core.
// Run with scripts/host-tests.sh; pass --update-references to regenerate golden renders.

#include <cmath>
#include <complex>
#include <cstdio>
#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <filesystem>
#include <fstream>
#include <functional>
#include <limits>
#include <memory>
#include <random>
#include <string>
#include <thread>
#include <tuple>
#include <vector>

#include "AcidTrack.h"
#include "Analysis.h"
#include "DrumKit.h"
#include "Effects.h"
#include "Mixer.h"
#include "PlaitsSynth.h"
#include "PlateReverb.h"
#include "OfflineRenderer.h"
#include "OnsetDetector.h"
#include "TimeStretch.h"
#include "SoundFontPlayer.h"
#include "Sample.h"
#include "Sampler.h"
#include "MultiSampler.h"
#include "Track808.h"
#include "WavWriter.h"
#include "Engine.h"
#include "RoundTripMeter.h"
#include "Sequencer.h"
#include "Synth.h"
#include "SpscFloatRing.h"
#include "SpscQueue.h"
#include "StemPlayer.h"

// The sampler renders stereo; centred mono sounds are the same on both sides: their mean.
static void renderMono(ps::Sampler& sampler, float* out, int n) {
    std::vector<float> l(static_cast<size_t>(n)), r(static_cast<size_t>(n));
    sampler.render(l.data(), r.data(), n);
    for (int i = 0; i < n; ++i) out[i] += 0.5f * (l[static_cast<size_t>(i)] + r[static_cast<size_t>(i)]);
}

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

// CPU time spent by this thread (not wall time): other processes taking the cores do not count,
// so a timing test means the same on an idle laptop and on a busy CI runner.
double threadSeconds() {
    timespec ts{};
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &ts);
    return static_cast<double>(ts.tv_sec) + static_cast<double>(ts.tv_nsec) * 1e-9;
}

// Clearly overloaded: more runnable work than cores (caches and memory bandwidth shared, clocks
// down). A timing bound missed then is reported, not counted: never a flaky failure.
bool machineOverloaded() {
    double avg[1] = {0.0};
    if (getloadavg(avg, 1) < 1) return false;
    return avg[0] > static_cast<double>(std::max(1u, std::thread::hardware_concurrency()));
}

#define CHECK_TIMING(cond)                                                                        \
    do {                                                                                          \
        if (!(cond)) {                                                                            \
            if (machineOverloaded()) {                                                            \
                std::printf("  timing missed on an overloaded machine (not counted): %s\n", #cond); \
            } else {                                                                              \
                CHECK(cond);                                                                      \
            }                                                                                     \
        }                                                                                         \
    } while (0)

constexpr int kRate = 48000;
constexpr int kChannels = 2;
// The limiter's look-ahead: the live output comes this many frames late (an export drops them).
const int kLate = ps::MasterChain::latencyFor(kRate);  // the master chain's delay

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
    // The same tone, the limiter's look-ahead later.
    float maxDiff = 0.0f;
    const size_t late = static_cast<size_t>(kLate) * kChannels;
    for (size_t i = 0; i < late; ++i) maxDiff = std::max(maxDiff, std::fabs(out[i]));
    for (size_t i = late; i < ref.size(); ++i) maxDiff = std::max(maxDiff, std::fabs(ref[i - late] - out[i]));
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

void testPositionDuringTheCountIn() {
    // 120 BPM: a sixteenth = 6000 frames; one bar of count-in = 16 sixteenths before the first step.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 1});
    render(engine, 6000 * 4, 250);
    CHECK(engine.playPosition() > -12.1 && engine.playPosition() < -11.9);  // 12 sixteenths to go
    render(engine, 6000 * 11 + 3000, 250);
    CHECK(engine.playPosition() > -0.55 && engine.playPosition() < -0.45);  // half a sixteenth before the "1"
    render(engine, 6000, 250);
    CHECK(engine.playPosition() > 0.45 && engine.playPosition() < 0.55);
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 256, 256);
    CHECK(engine.playPosition() == -1.0);
}

void testHotMasterFilterKeepsPlaying() {
    // A very hot mix (+14 dB, high-pitched) through the master's Filter at its top cutoff: the
    // filter used to diverge into NaN and the master went silent for good.
    std::vector<float> loud(48000);
    std::mt19937 rng(3);
    std::uniform_real_distribution<float> noise(-1.0f, 1.0f);
    for (size_t i = 0; i < loud.size(); ++i) loud[i] = 5.0f * noise(rng);
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::InstallSample, 0.0f, 0, 0, ps::Sample::create(loud.data(), 48000, 1, kRate, kRate)});
    engine.post({ps::CommandType::SetPadParam, 1.0f, 0, ps::Sampler::Loop});
    const int master = ps::Mixer::kMaster * ps::Mixer::kInserts;
    engine.post({ps::CommandType::SetInsertType, 0.0f, master, ps::InsertSlot::Filter});
    engine.post({ps::CommandType::SetInsertParam, 1.0f, master, 0});   // cutoff at the top
    engine.post({ps::CommandType::SetInsertParam, 0.9f, master, 1});   // strong resonance
    engine.post({ps::CommandType::SetInsertParam, 0.0f, master, 2});   // low-pass
    render(engine, 2400, 240);
    engine.post({ps::CommandType::TriggerPad, 1.0f, 0});
    render(engine, kRate * 2, 240);
    const auto tail = render(engine, kRate / 2, 240);
    double energy = 0.0;
    for (float v : tail) energy += static_cast<double>(v) * v;
    CHECK(energy > 1.0);  // still sounding
    CHECK(!engine.hasNonFinite());
}

void testBigBlocksLoseNoHits() {
    // Rolls of 4 on every sixteenth at 180 BPM (a hit every 1000 frames): Bluetooth hands the
    // engine 960 to 1920 frames at once, and each hit must still sound, as with small blocks.
    auto onsets = [](const std::vector<float>& x) {
        int n = 0, quiet = 1000;
        float env = 0.0f;
        for (size_t i = 0; i < x.size(); i += kChannels) {
            env = std::max(std::fabs(x[i]), env * 0.995f);
            if (env > 0.15f && quiet > 150) { ++n; quiet = 0; }
            if (env < 0.05f) ++quiet; else if (quiet < 1000 && quiet > 0) quiet = 0;
        }
        return n;
    };
    auto run = [&](int block) {
        ps::Engine e(kRate);
        e.post({ps::CommandType::SetTempo, 180.0f});
        for (int s = 0; s < 16; ++s) e.post({ps::CommandType::SetDrumHit, 1.0f, 10, s | (4 << 16)});
        e.post({ps::CommandType::SetChannelMute, 1.0f, 1});
        e.post({ps::CommandType::SetTransport, 1.0f, 0});
        return onsets(render(e, kRate * 4, block));
    };
    const int small = run(96);
    CHECK(small > 150);
    CHECK(run(1920) == small);
    CHECK(run(1024) == small);
}

void testSustainPedalReleasesEveryNote() {
    // The pedal down, 72 different notes played and let go, then the pedal up: all must stop.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::Sustain});
    for (int n = 30; n < 102; ++n) {
        engine.post({ps::CommandType::SynthNoteOn, 0.8f, n});
        render(engine, 256, 256);
        engine.post({ps::CommandType::SynthNoteOff, 0.0f, n});
    }
    render(engine, kRate / 2, 256);
    engine.post({ps::CommandType::SetExpression, 0.0f, ps::InstrumentTrack::Sustain});
    render(engine, kRate * 6, 256);
    const auto after = render(engine, kRate / 2, 256);
    double energy = 0.0;
    for (float v : after) energy += static_cast<double>(v) * v;
    CHECK(energy < 1e-6);
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
    CHECK(firstSoundAfter(0) == 1 + kLate);
    CHECK(firstSoundAfter(20000) == 24001 + kLate);
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

// A kit built on memory full of garbage (all bytes 0xFF: NaN floats) still renders clean kicks:
// DaisySP's SyntheticBassDrum::Init leaves its transient envelope unset.
void testKitOnDirtyMemoryStaysFinite() {
    alignas(ps::DrumKit) static unsigned char storage[sizeof(ps::DrumKit)];
    std::memset(storage, 0xFF, sizeof storage);
    auto* kit = new (storage) ps::DrumKit;  // default-init, as a member of Engine: no zeroing
    kit->prepare(kRate);
    const auto out = renderKit(*kit, kRate, 256, {{100, ps::DrumKit::Kick2, 1.0f}, {12000, ps::DrumKit::Zap, 1.0f}});
    bool finite = true;
    for (float v : out) finite &= std::isfinite(v);
    CHECK(finite);
    kit->~DrumKit();
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
    CHECK(first >= 24000 + kLate && first <= 24001 + kLate);
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
    // Best of three passes: a busy machine (CI) only ever makes a pass slower, never faster.
    std::vector<float> out;
    double load = 1e9;
    for (int pass = 0; pass < 3; ++pass) {
        const double start = threadSeconds();
        out = render(engine, frames, 256);
        const double seconds = (threadSeconds() - start);
        load = std::min(load, seconds / (static_cast<double>(frames) / kRate));
    }
    std::printf("  full kit: %.1f %% of real time on this machine\n", load * 100.0);
#if defined(__SANITIZE_ADDRESS__)
    (void)load;  // sanitizer builds are several times slower: timing is meaningless there
#else
    CHECK_TIMING(load < 0.10);  // leaves a large margin for a phone and for synths/effects
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

// The saturation (tanh) squashes the taller side of the voice's lopsided wave and leaves an
// offset (+0.056 at drive 0.8 on this line; 0.07 on the bench's acidcore through its Saturation
// insert): the blocker after it keeps a dense, driven line at a mean under 1e-3.
void testAcidDriveLeavesNoDc() {
    ps::AcidTrack acid;
    acid.prepare(kRate);
    acid.setParam(ps::AcidTrack::Drive, 0.8f);
    acid.setParam(ps::AcidTrack::Resonance, 0.8f);
    for (int i = 0; i < 16; ++i) acid.setStep(i, note(i % 3 == 0 ? 12 : 0, i % 4 == 0));
    const auto out = renderAcid(acid, 4);
    double sum = 0.0, e = 0.0;
    for (float v : out) {
        sum += v;
        e += static_cast<double>(v) * v;
    }
    const double mean = sum / static_cast<double>(out.size()), rms = std::sqrt(e / static_cast<double>(out.size()));
    std::printf("  acid at drive 0.8: mean %+.5f, rms %.3f\n", mean, rms);
    CHECK(rms > 0.05);
    CHECK(std::fabs(mean) < 1e-3);
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
    const double start = threadSeconds();
    renderAcid(acid, 64);
    const double seconds = (threadSeconds() - start);
    const double load = seconds / (64 * 6000.0 / kRate);
    std::printf("  acid voice: %.1f %% of real time on this machine\n", load * 100.0);
#if !defined(__SANITIZE_ADDRESS__)
    CHECK_TIMING(load < 0.05);
#endif
}


// Brightness of a stretch of signal: the energy of its sample-to-sample differences over its energy
// (a closed low-pass gives little of the first).
double brightness(const std::vector<float>& x, size_t from, size_t to, size_t stride = 1) {
    double d = 0.0, e = 0.0;
    for (size_t i = from + stride; i < to; i += stride) {
        const double a = x[i], b = x[i - stride];
        d += (a - b) * (a - b);
        e += a * a;
    }
    return e > 0.0 ? d / e : 0.0;
}

void testAcidKnobAutomation() {
    // A busy 303 line at 185 BPM, its cutoff automated: closed on the first half of the bar, open
    // on the second. The engine's step frame carries the change (with the step's note), so any
    // block size renders the same samples: live and export sound alike.
    auto run = [](int block, bool automated) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 185.0f});
        for (int i = 0; i < 16; ++i) engine.post({ps::CommandType::SetAcidStep, 0.0f, i, note(i % 2 ? 12 : 0, i % 4 == 2).pack()});
        engine.post({ps::CommandType::SetAcidParam, 0.5f, ps::AcidTrack::Cutoff});
        engine.post({ps::CommandType::SetAcidParam, 0.0f, ps::AcidTrack::EnvMod});
        if (automated) {
            engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Acid, 1, 0, ps::AcidTrack::Cutoff)});
            for (int st = 0; st < 16; ++st) engine.post({ps::CommandType::SetAutomationPoint, st < 8 ? 0.0f : 1.0f, 0, st});
            // Resonance and saturation lanes too: every 303 knob goes through the same path.
            engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 1, ps::Automation::pack(ps::Automation::Acid, 1, 0, ps::AcidTrack::Drive)});
            engine.post({ps::CommandType::SetAutomationPoint, 0.8f, 1, 0});
        }
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        return render(engine, 2 * 16 * 3892, block);  // two bars
    };
    const auto live = run(192, true);
    const auto big = run(ps::OfflineRenderer::kBlock, true);
    const auto odd = run(997, true);
    float maxDiff = 0.0f;
    bool finite = true;
    for (size_t i = 0; i < live.size(); ++i) {
        maxDiff = std::max({maxDiff, std::fabs(live[i] - big[i]), std::fabs(live[i] - odd[i])});
        finite &= std::isfinite(live[i]);
    }
    std::printf("  acid automation: block sizes differ by %.2e\n", static_cast<double>(maxDiff));
    CHECK(finite);
    CHECK(maxDiff < 1e-6f);
    // Second bar (the lane loops): closed half against open half, left channel.
    const size_t step = 3892, bar2 = 16 * step;
    const double closed = brightness(live, (bar2 + step) * 2, (bar2 + 8 * step) * 2, 2);
    const double open = brightness(live, (bar2 + 9 * step) * 2, (bar2 + 16 * step) * 2, 2);
    const auto plain = run(192, false);
    const double plainOpen = brightness(plain, (bar2 + 9 * step) * 2, (bar2 + 16 * step) * 2, 2);
    std::printf("  acid automation: brightness closed %.4f, open %.4f (no lane %.4f)\n", closed, open, plainOpen);
    CHECK(open > 3.0 * closed);
    CHECK(open > 1.5 * plainOpen);  // the lane did open it beyond the knob's own setting
    // Stopped, the next start plays the knob where the lane left it until a point says otherwise.
}

void testAcidAutomationWaitsForTheStep() {
    // The new setting lands on its frame, never at the start of the block before it: the frames
    // before the step are those of an untouched track.
    auto run = [](bool automate) {
        ps::AcidTrack acid;
        acid.prepare(kRate);
        acid.setParam(ps::AcidTrack::Cutoff, 0.0f);
        acid.setStep(0, note(0));
        acid.setStep(1, note(12));
        ps::Sequencer seq;
        seq.prepare(kRate);
        seq.setTempo(120.0);
        seq.start(0);
        std::vector<float> out(12000, 0.0f);
        for (int pos = 0; pos < 12000; pos += 1024) {
            const int n = std::min(1024, 12000 - pos);
            seq.advance(n, [&](int offset, int step) {
                acid.onStep(offset, step, seq.samplesPerStep());
                if (automate && step == 1) acid.automate(ps::AcidTrack::Cutoff, 1.0f, offset);
            });
            acid.render(out.data() + pos, n);
        }
        return out;
    };
    const auto a = run(false), b = run(true);
    float before = 0.0f, after = 0.0f;
    for (int i = 0; i < 6000; ++i) before = std::max(before, std::fabs(a[static_cast<size_t>(i)] - b[static_cast<size_t>(i)]));
    for (int i = 6000; i < 12000; ++i) after = std::max(after, std::fabs(a[static_cast<size_t>(i)] - b[static_cast<size_t>(i)]));
    CHECK(before == 0.0f);
    CHECK(after > 0.01f);
}


// Touch take-over: while a finger holds a 303 knob, its automation lane leaves it alone (the
// knob is heard as turned); let go, the lane drives it again from the next step. Generic: any
// target (a mixer fader here too), several at once, a release never seen held is harmless.
void testAutomationHoldTakesOver() {
    static_assert(static_cast<int>(ps::CommandType::SetInsertOn) == 69 && static_cast<int>(ps::CommandType::SetAutomationHold) == 70,
                  "NativeEngine.kt's CMD_INSERT_ON and CMD_AUTOMATION_HOLD");
    const int32_t cutoff = ps::Automation::pack(ps::Automation::Acid, 1, 0, ps::AcidTrack::Cutoff);
    const size_t step = 3892, bar = 16 * step;  // 185 BPM
    // Bar 1: the lane keeps the filter closed. Bar 2: the knob turned wide open (held or not).
    // Bar 3: let go.
    auto run = [&](bool hold) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 185.0f});
        for (int i = 0; i < 16; ++i) engine.post({ps::CommandType::SetAcidStep, 0.0f, i, note(i % 2 ? 12 : 0, i % 4 == 2).pack()});
        engine.post({ps::CommandType::SetAcidParam, 0.0f, ps::AcidTrack::Cutoff});
        engine.post({ps::CommandType::SetAcidParam, 0.0f, ps::AcidTrack::EnvMod});
        engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, cutoff});
        for (int st = 0; st < 16; ++st) engine.post({ps::CommandType::SetAutomationPoint, 0.0f, 0, st});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        auto out = render(engine, static_cast<int>(bar + step / 2), 192);
        if (hold) engine.post({ps::CommandType::SetAutomationHold, 1.0f, cutoff});
        engine.post({ps::CommandType::SetAcidParam, 1.0f, ps::AcidTrack::Cutoff});
        auto more = render(engine, static_cast<int>(bar), 192);
        out.insert(out.end(), more.begin(), more.end());
        if (hold) engine.post({ps::CommandType::SetAutomationHold, 0.0f, cutoff});
        more = render(engine, static_cast<int>(bar), 192);
        out.insert(out.end(), more.begin(), more.end());
        return out;
    };
    const auto held = run(true), loose = run(false);
    auto bright = [&](const std::vector<float>& v, size_t from, size_t to) { return brightness(v, from * 2, to * 2, 2); };
    const size_t b2 = bar + 2 * step, b3 = 2 * bar + 2 * step;
    const double heldOpen = bright(held, b2, b2 + 12 * step), freeOpen = bright(loose, b2, b2 + 12 * step);
    const double heldBack = bright(held, b3, b3 + 12 * step), closed = bright(held, step, 14 * step);
    std::printf("  hold: turned and held %.4f, pulled back by the lane %.4f; let go %.4f (closed %.4f)\n", heldOpen, freeOpen, heldBack, closed);
    CHECK(heldOpen > 3.0 * freeOpen);  // the finger wins while it holds
    CHECK(freeOpen < 1.5 * closed);    // without a hold, the lane closes it again at once
    CHECK(heldBack < 1.5 * closed);    // let go: the lane has it again
    // Generic and bounded: a fader held, more holds than slots, releases of what was never held.
    ps::Automation a;
    const int32_t fader = ps::Automation::pack(ps::Automation::Volume, 2, 0, 0);
    a.hold(fader, true);
    a.hold(fader, true);  // twice: still one slot
    CHECK(a.isHeld(fader) && !a.isHeld(cutoff));
    for (int i = 0; i < 2 * ps::Automation::kHolds; ++i) a.hold(ps::Automation::pack(ps::Automation::Acid, 1, 0, i), true);
    CHECK(a.isHeld(fader));
    a.hold(fader, false);
    CHECK(!a.isHeld(fader));
    a.hold(ps::Automation::pack(ps::Automation::Pan, 3, 0, 0), false);  // never held: nothing happens
    a.hold(-1, true);
    CHECK(!a.isHeld(-1));
    for (int i = 0; i < 2 * ps::Automation::kHolds; ++i) a.hold(ps::Automation::pack(ps::Automation::Acid, 1, 0, i), false);
    for (int32_t h : a.held) CHECK(h == -1);
}

// A slot given another effect, or emptied, plays in every scene again: an off gate left by the
// effect before does not keep the new one silent (the app sends « Joue dans » after it).
void testNewEffectInASlotIsHeard() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    mixer.setInsertType(ps::Mixer::kDrums, 1, ps::InsertSlot::Saturation);
    mixer.setInsertOn(ps::Mixer::kDrums, 1, false);
    CHECK(!mixer.insertOn(ps::Mixer::kDrums, 1));
    mixer.setInsertType(ps::Mixer::kDrums, 1, ps::InsertSlot::Saturation);  // the same: unchanged
    CHECK(!mixer.insertOn(ps::Mixer::kDrums, 1));
    mixer.setInsertType(ps::Mixer::kDrums, 1, ps::InsertSlot::Chorus);
    CHECK(mixer.insertOn(ps::Mixer::kDrums, 1));
    mixer.setInsertOn(ps::Mixer::kDrums, 1, false);
    mixer.setInsertOn(ps::Mixer::kDrums, 3, false);
    mixer.clearInserts(ps::Mixer::kDrums);
    CHECK(mixer.insertOn(ps::Mixer::kDrums, 1) && mixer.insertOn(ps::Mixer::kDrums, 3));
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
    const double start = threadSeconds();
    const auto out = renderSynth(synth, kRate * 2);
    const double load = (threadSeconds() - start) / 2.0;
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
    CHECK_TIMING(load < 0.15);
#endif
}

/** Rising zero crossings of [x] in [from, to), as a frequency in Hz. */
double crossingHz(const std::vector<float>& x, int from, int to) {
    int crossings = 0;
    for (int i = from + 1; i < to; ++i) {
        if (x[static_cast<size_t>(i - 1)] < 0.0f && x[static_cast<size_t>(i)] >= 0.0f) ++crossings;
    }
    return crossings * static_cast<double>(kRate) / (to - from);
}

void testSynthGlide() {
    auto make = [](float glide) {
        auto synth = std::make_unique<ps::Synth>();
        synth->prepare(kRate);
        openFilter(*synth);
        synth->setParam(ps::Synth::Osc1Wave, 1.0f);
        synth->setParam(ps::Synth::OscMix, 0.0f);
        synth->setParam(ps::Synth::Glide, glide);
        // A3, then A4 starting while A3 is still held (a line whose notes overlap), then A3 let go.
        synth->noteOn(57, 1.0f, 0);
        synth->noteOn(69, 1.0f, kRate / 2);
        synth->noteOff(57, kRate / 2 + 480);
        return synth;
    };
    // 0.4 = 80 ms: one voice slides from 220 to 440 Hz, without a second attack.
    auto gliding = make(0.4f);
    std::vector<float> out(static_cast<size_t>(kRate), 0.0f);
    int most = 0;
    for (int pos = 0; pos < kRate; pos += 256) {
        gliding->render(out.data() + pos, std::min(256, kRate - pos));
        most = std::max(most, gliding->activeVoices());
    }
    const double before = crossingHz(out, kRate / 4, kRate / 2), after = crossingHz(out, kRate * 3 / 4, kRate);
    const double during = crossingHz(out, kRate / 2, kRate / 2 + 1920);  // the first 40 ms of the slide
    std::printf("  synth glide: %.0f Hz -> %.0f Hz (%.0f Hz while sliding), %d voice(s)\n", before, after, during, most);
    CHECK(std::abs(before - 220.0) < 6.0 && std::abs(after - 440.0) < 6.0);
    CHECK(during > 240.0 && during < 420.0);
    CHECK(most == 1);
    // Without glide the same notes are two voices, the second one at its pitch at once.
    auto plain = make(0.0f);
    int voices = 0;
    std::vector<float> dry(static_cast<size_t>(kRate), 0.0f);
    for (int pos = 0; pos < kRate; pos += 256) {
        plain->render(dry.data() + pos, std::min(256, kRate - pos));
        if (pos <= kRate / 2 + 100 && kRate / 2 + 100 < pos + 256) voices = plain->activeVoices();
    }
    CHECK(voices == 2);
    CHECK(std::abs(crossingHz(dry, kRate * 3 / 4, kRate) - 440.0) < 6.0);
}

void testSynthSweepFollowsTheTempo() {
    ps::Synth synth;
    synth.prepare(kRate);
    synth.setParam(ps::Synth::Lfo2Rate, 0.3f);
    const float free = synth.sweepHz();
    CHECK(std::abs(free - 0.05f * std::pow(400.0f, 0.3f)) < 1e-4f);
    // Locked to a quarter note at 120 BPM (a beat of 24000 frames): 2 Hz, whatever the rate knob.
    synth.setBeatFrames(kRate * 60.0 / 120.0);
    synth.setParam(ps::Synth::Lfo2Sync, 3.0f / 7.0f);
    CHECK(std::abs(synth.sweepHz() - 2.0f) < 1e-4f);
    // The tempo changes: the sweep follows (140 BPM, eighth notes: 4.67 Hz).
    synth.setParam(ps::Synth::Lfo2Sync, 4.0f / 7.0f);
    synth.setBeatFrames(kRate * 60.0 / 140.0);
    CHECK(std::abs(synth.sweepHz() - 140.0f / 60.0f * 2.0f) < 1e-3f);
    CHECK(std::abs(ps::Synth::syncCyclesPerBeat(1.0f) - 3.0f) < 1e-6f);  // eighth-note triplets
    // Back to free: the knob's rate again.
    synth.setParam(ps::Synth::Lfo2Sync, 0.0f);
    CHECK(synth.sweepHz() == free);

    // Heard: a held saw under a deep sweep breathes once per beat, its loudest moments a beat apart.
    ps::Synth wob;
    wob.prepare(kRate);
    wob.setParam(ps::Synth::Cutoff, 0.35f);
    wob.setParam(ps::Synth::Resonance, 0.0f);
    wob.setParam(ps::Synth::FilterEnvAmount, 0.0f);
    wob.setParam(ps::Synth::Lfo2ToCutoff, 0.9f);
    wob.setParam(ps::Synth::AmpSustain, 1.0f);
    wob.setBeatFrames(kRate * 60.0 / 120.0);
    wob.setParam(ps::Synth::Lfo2Sync, 3.0f / 7.0f);
    wob.noteOn(45, 1.0f);
    const auto out = renderSynth(wob, kRate * 2);
    constexpr int kWindow = 480;  // 10 ms
    std::vector<double> rms;
    for (int w = 0; w + kWindow <= kRate * 2; w += kWindow) {
        double e = 0.0;
        for (int i = w; i < w + kWindow; ++i) e += double(out[static_cast<size_t>(i)]) * out[static_cast<size_t>(i)];
        rms.push_back(std::sqrt(e / kWindow));
    }
    // The strongest repeat of the loudness curve, between 0.3 and 0.8 s: a beat (0.5 s).
    int best = 0;
    double bestScore = -1e30;
    for (int lag = 30; lag <= 80; ++lag) {
        double score = 0.0;
        for (size_t i = 0; i + static_cast<size_t>(lag) < rms.size(); ++i) score += rms[i] * rms[i + static_cast<size_t>(lag)];
        score /= static_cast<double>(rms.size() - static_cast<size_t>(lag));
        if (score > bestScore) { bestScore = score; best = lag; }
    }
    std::printf("  synced sweep repeats every %d ms\n", best * 10);
    CHECK(std::abs(best - 50) <= 2);
}

void testSynthUnison() {
    auto rms = [](const std::vector<float>& x, size_t from) {
        double e = 0.0;
        for (size_t i = from; i < x.size(); ++i) e += double(x[i]) * x[i];
        return std::sqrt(e / double(x.size() - from));
    };
    auto make = [](float unison, float detune) {
        auto synth = std::make_unique<ps::Synth>();
        synth->prepare(kRate);
        openFilter(*synth);
        synth->setParam(ps::Synth::OscMix, 0.0f);
        synth->setParam(ps::Synth::Unison, unison);
        synth->setParam(ps::Synth::UnisonDetune, detune);
        return synth;
    };
    // Off, the sound is exactly as before (the spread alone changes nothing).
    auto plain = make(0.0f, 0.0f);
    auto spreadOnly = make(0.0f, 0.8f);
    plain->noteOn(57, 1.0f);
    spreadOnly->noteOn(57, 1.0f);
    const auto a = renderSynth(*plain, kRate / 2);
    const auto b = renderSynth(*spreadOnly, kRate / 2);
    CHECK(a == b);
    // Seven saws: about as loud, but a different (wider) sound.
    auto wide = make(1.0f, 0.5f);
    wide->noteOn(57, 1.0f);
    const auto c = renderSynth(*wide, kRate / 2);
    const double ratio = rms(c, kRate / 10) / rms(a, kRate / 10);
    std::printf("  unison level: %.2f of one oscillator\n", ratio);
    CHECK(ratio > 0.5 && ratio < 1.6);
    CHECK(a != c);
    // Without spread the copies share the pitch: the sound repeats every A4 period (480/4.4 frames).
    auto same = make(1.0f, 0.0f);
    same->noteOn(69, 1.0f);
    const auto t = renderSynth(*same, kRate);
    double xy = 0.0, xx = 0.0, yy = 0.0;
    for (size_t i = kRate / 2; i + 109 < t.size(); ++i) {
        xy += double(t[i]) * t[i + 109];
        xx += double(t[i]) * t[i];
        yy += double(t[i + 109]) * t[i + 109];
    }
    const double periodic = xy / std::sqrt(xx * yy);
    std::printf("  unison without spread repeats at A4: %.3f\n", periodic);
    CHECK(periodic > 0.9);
    // The stack stays centred on the note with an odd number of copies (2 voices at full spread:
    // one 25 cents up, one 25 cents down, not 0 and -50): A4 still averages 440 Hz.
    for (float voices : {1.0f / 6.0f, 3.0f / 6.0f}) {
        auto centred = make(voices, 1.0f);
        centred->setParam(ps::Synth::Osc1Wave, 1.0f);  // triangles: clean zero crossings
        centred->noteOn(69, 1.0f);
        const auto c2 = renderSynth(*centred, kRate * 2);
        // Power-weighted mean frequency around A4 (Goertzel scan, Hann window, last second).
        double weighted = 0.0, total = 0.0;
        for (double f = 400.0; f <= 480.0; f += 0.25) {
            const double w = 2.0 * std::cos(2.0 * M_PI * f / kRate);
            double s1 = 0.0, s2 = 0.0;
            for (int i = 0; i < kRate; ++i) {
                const double hann = 0.5 - 0.5 * std::cos(2.0 * M_PI * i / (kRate - 1));
                const double s0 = c2[static_cast<size_t>(kRate + i)] * hann + w * s1 - s2;
                s2 = s1;
                s1 = s0;
            }
            const double power = s1 * s1 + s2 * s2 - w * s1 * s2;
            weighted += f * power;
            total += power;
        }
        const double centre = weighted / total;
        std::printf("  unison %d copies at full spread: centred on %.1f Hz\n", static_cast<int>(voices * 6.0f + 0.5f), centre);
        CHECK(std::abs(centre - 440.0) < 1.5);
    }
    // A note played again while it rings keeps its phases: the sound goes on as if it had not been
    // touched (a reset of the six copies would change it at once).
    {
        auto again = make(1.0f, 0.5f);
        auto alone = make(1.0f, 0.5f);
        for (auto* s : {again.get(), alone.get()}) {
            s->setParam(ps::Synth::AmpAttack, 0.0f);
            s->setParam(ps::Synth::AmpSustain, 1.0f);
            s->noteOn(57, 1.0f);
            renderSynth(*s, kRate / 4, 256);
        }
        again->noteOn(57, 1.0f);
        const auto a2 = renderSynth(*again, 512, 256);
        const auto b2 = renderSynth(*alone, 512, 256);
        float diff = 0.0f;
        for (size_t i = 0; i < a2.size(); ++i) diff = std::max(diff, std::abs(a2[i] - b2[i]));
        std::printf("  unison retrigger: %.4f from the untouched note\n", diff);
        CHECK(diff < 0.02f);
    }
    // Eight voices of seven saws stay affordable (best of three passes).
    double best = 1e9;
    for (int pass = 0; pass < 3; ++pass) {
        auto full = make(1.0f, 0.6f);
        for (int n = 0; n < 8; ++n) full->noteOn(48 + n * 3, 1.0f);
        const double start = threadSeconds();
        const auto out = renderSynth(*full, kRate);
        best = std::min(best, (threadSeconds() - start));
        bool finite = true;
        for (float v : out) finite &= std::isfinite(v);
        CHECK(finite);
    }
    std::printf("  unison 8 voices x 7 saws: %.1f %% of real time on this machine\n", best * 100.0);
#if !defined(__SANITIZE_ADDRESS__)
    CHECK_TIMING(best < 0.25);
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
    // A long reverb rings into the tail, which fades to nothing instead of stopping dead.
    ps::OfflineRenderer wet(kRate);
    wet.post({ps::CommandType::SetTempo, 120.0f});
    wet.post({ps::CommandType::SetDrumHit, 1.0f, 0, 12});
    wet.post({ps::CommandType::SetSend, 1.0f, 0, 0});
    wet.post({ps::CommandType::SetBusParam, 1.0f, 0, 0});  // the longest room
    const auto tail = wet.renderToBuffer(bar, kRate / 2);
    float lastQuarter = 0.0f, ending = 0.0f;
    for (size_t i = tail.size() - size_t(kRate / 4) * 2; i < tail.size() - size_t(kRate / 8) * 2; ++i) lastQuarter = std::max(lastQuarter, std::fabs(tail[i]));
    for (size_t i = tail.size() - 64; i < tail.size(); ++i) ending = std::max(ending, std::fabs(tail[i]));
    CHECK(lastQuarter > 1e-3f);  // still ringing a quarter of a second before the end
    CHECK(ending < lastQuarter * 0.02f);

    ps::OfflineRenderer cancelled(kRate);
    cancelled.cancel();
    CHECK(cancelled.renderToWav(path.c_str(), bar, 0) == ps::OfflineRenderer::Result::Cancelled);
    CHECK(ps::OfflineRenderer(kRate).renderToWav("/nonexistent-dir/x.wav", bar, 0) == ps::OfflineRenderer::Result::IoError);
    std::filesystem::remove(path);
}

// "Fondu de fin": the song's last seconds fade to silence, the rest is untouched and nothing gets louder.
void testOfflineExportFadesOutTheEnd() {
    auto beat = [](ps::OfflineRenderer& r) {
        r.post({ps::CommandType::SetTempo, 120.0f});
        // The kick alone: no noise source, so two renders give the very same samples.
        for (int step = 0; step < 16; step += 2) r.post({ps::CommandType::SetDrumHit, step % 4 == 0 ? 1.0f : 0.7f, 0, step});
    };
    const int64_t bars = kRate * 2 * 4;  // four bars at 120 BPM
    const int64_t fade = kRate * 3;      // the short fade
    ps::OfflineRenderer plainR(kRate), fadedR(kRate);
    beat(plainR);
    beat(fadedR);
    fadedR.setFadeOut(fade);
    const auto plain = plainR.renderToBuffer(bars, 0);
    const auto faded = fadedR.renderToBuffer(bars, 0);
    CHECK(plain.size() == size_t(bars) * 2 && faded.size() == plain.size());
    if (faded.size() != plain.size() || plain.empty()) return;
    const size_t fadeStart = size_t(bars - fade) * 2;
    float diffBefore = 0.0f, peakPlain = 0.0f, peakFaded = 0.0f;
    bool louder = false, finite = true;
    for (size_t i = 0; i < plain.size(); ++i) {
        if (!std::isfinite(faded[i])) finite = false;
        if (i < fadeStart) diffBefore = std::max(diffBefore, std::fabs(plain[i] - faded[i]));
        if (std::fabs(faded[i]) > std::fabs(plain[i]) + 1e-6f) louder = true;
        peakPlain = std::max(peakPlain, std::fabs(plain[i]));
        peakFaded = std::max(peakFaded, std::fabs(faded[i]));
    }
    CHECK(finite);
    CHECK(diffBefore == 0.0f);  // before the fade: the very same samples
    CHECK(!louder);             // the fade never adds level, so it cannot clip
    CHECK(peakFaded <= peakPlain && peakFaded <= 0.9f);
    // The level goes down along the fade: its last quarter is far quieter than the start, and
    // the very end is silent.
    auto peakIn = [&](const std::vector<float>& b, int64_t from, int64_t to) {
        float m = 0.0f;
        for (size_t i = size_t(from) * 2; i < size_t(to) * 2; ++i) m = std::max(m, std::fabs(b[i]));
        return m;
    };
    const float fadeHead = peakIn(faded, bars - fade, bars - fade + kRate / 2);
    const float fadeEnd = peakIn(faded, bars - fade / 4, bars);
    CHECK(fadeHead > 0.1f);
    CHECK(fadeEnd < fadeHead * 0.1f);
    CHECK(peakIn(faded, bars - 64, bars) < 1e-3f);
    // Longer than the song: the whole render fades, still no NaN and nothing louder.
    ps::OfflineRenderer shortR(kRate);
    beat(shortR);
    shortR.setFadeOut(bars * 10);
    const auto whole = shortR.renderToBuffer(bars, 0);
    CHECK(whole.size() == plain.size());
    for (size_t i = 0; i < whole.size() && i < plain.size(); ++i) {
        if (!std::isfinite(whole[i]) || std::fabs(whole[i]) > std::fabs(plain[i]) + 1e-6f) { CHECK(false); break; }
    }
}

// Stems: with the master's inserts and limiter bypassed, the stems (each strip soloed) add back
// up to the mix before the master, sample for sample; the limiter does not touch them.
void testStemsAddUpToTheMix() {
    auto song = [](ps::OfflineRenderer& r, int solo, bool bypass) {
        r.post({ps::CommandType::SetTempo, 120.0f});
        // The kick and the acid line, loud: together they go far over the limiter's ceiling.
        for (int step = 0; step < 16; step += 2) r.post({ps::CommandType::SetDrumHit, 1.0f, 0, step});
        for (int st = 0; st < 16; ++st) r.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{st % 7, 0, st % 3 == 0, st % 4 == 1, true}.pack()});
        r.post({ps::CommandType::SetChannelVolume, 1.0f, ps::Mixer::kDrums});
        r.post({ps::CommandType::SetChannelVolume, 1.0f, 1});
        // Both in the reverb and the echo: a stem carries its own share of the send returns.
        r.post({ps::CommandType::SetSend, 0.5f, ps::Mixer::kDrums, 0});
        r.post({ps::CommandType::SetSend, 0.4f, 1, 1});
        // A squashing compressor on the master, and its gain up.
        r.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kMaster * ps::Mixer::kInserts, ps::InsertSlot::Comp});
        const float comp[4] = {0.0f, 1.0f, 0.0f, 0.5f};  // -40 dB threshold, 20:1, fast
        for (int prm = 0; prm < 4; ++prm) r.post({ps::CommandType::SetInsertParam, comp[prm], ps::Mixer::kMaster * ps::Mixer::kInserts, prm});
        r.post({ps::CommandType::SetMaster, 1.4f, 0});
        if (solo >= 0) r.post({ps::CommandType::SetChannelSolo, 1.0f, solo});
        if (bypass) {
            r.post({ps::CommandType::SetMaster, 0.0f, 1});
            r.post({ps::CommandType::SetMaster, 0.0f, 2});
        }
    };
    const int64_t bars = kRate * 2 * 2;
    auto renderOf = [&](int solo, bool bypass) {
        ps::OfflineRenderer r(kRate);
        song(r, solo, bypass);
        return r.renderToBuffer(bars, kRate);
    };
    const auto mixed = renderOf(-1, false);
    const auto pre = renderOf(-1, true);
    const auto again = renderOf(-1, true);
    const auto drums = renderOf(ps::Mixer::kDrums, true);
    const auto acid = renderOf(1, true);
    CHECK(pre == again);  // the renders are repeatable: the comparison below means something
    CHECK(mixed.size() == pre.size() && drums.size() == pre.size() && acid.size() == pre.size());
    if (drums.size() != pre.size() || acid.size() != pre.size() || mixed.size() != pre.size()) return;
    float diff = 0.0f, peakPre = 0.0f, peakMixed = 0.0f, peakDrums = 0.0f, peakAcid = 0.0f, mixDiff = 0.0f;
    for (size_t i = 0; i < pre.size(); ++i) {
        diff = std::max(diff, std::fabs(drums[i] + acid[i] - pre[i]));
        mixDiff = std::max(mixDiff, std::fabs(drums[i] + acid[i] - mixed[i]));
        peakPre = std::max(peakPre, std::fabs(pre[i]));
        peakMixed = std::max(peakMixed, std::fabs(mixed[i]));
        peakDrums = std::max(peakDrums, std::fabs(drums[i]));
        peakAcid = std::max(peakAcid, std::fabs(acid[i]));
    }
    CHECK(peakDrums > 0.1f && peakAcid > 0.05f);
    CHECK(diff < 1e-4f);                    // the stems add up to the mix before the master
    CHECK(peakPre > ps::Mixer::kCeiling);   // no limiter: the peaks over the ceiling stay
    CHECK(peakMixed <= ps::Mixer::kCeiling + 1e-3f);  // with it, the mix is held under -1 dBFS
    CHECK(mixDiff > 0.05f);                 // and the master's processing really changes the sound
    // The master's gain is a plain gain before the limiter: half of it, half the level.
    ps::OfflineRenderer half(kRate);
    song(half, -1, true);
    half.post({ps::CommandType::SetMaster, 0.7f, 0});
    const auto halved = half.renderToBuffer(bars, kRate);
    float halfDiff = 0.0f;
    for (size_t i = 0; i < halved.size() && i < pre.size(); ++i) halfDiff = std::max(halfDiff, std::fabs(halved[i] * 2.0f - pre[i]));
    CHECK(halved.size() == pre.size() && halfDiff < 1e-4f);
}

// The automatic tail: rendered until the output stays under -90 dBFS, longer than the old fixed
// two seconds for a long reverb, through the gaps of an echo, and capped (with a fade) when it never ends.
void testOfflineExportAutoTail() {
    const int64_t bar = kRate * 2;
    const int64_t cap = kRate * 10;
    auto peakIn = [](const std::vector<float>& b, int64_t from, int64_t to) {
        float m = 0.0f;
        for (size_t i = size_t(std::max<int64_t>(0, from)) * 2; i < size_t(to) * 2 && i < b.size(); ++i) m = std::max(m, std::fabs(b[i]));
        return m;
    };
    // A long reverb on a last hit.
    ps::OfflineRenderer wet(kRate);
    wet.post({ps::CommandType::SetTempo, 120.0f});
    wet.post({ps::CommandType::SetDrumHit, 1.0f, 0, 12});
    wet.post({ps::CommandType::SetSend, 1.0f, 0, 0});
    wet.post({ps::CommandType::SetBusParam, 0.85f, 0, 0});  // a big room
    wet.setAutoTail(true);
    const auto room = wet.renderToBuffer(bar, cap);
    const int64_t roomFrames = int64_t(room.size() / 2);
    CHECK(roomFrames > bar + 2 * kRate);  // the reverb rings longer than the old fixed tail
    CHECK(roomFrames < bar + cap);        // and still ends by itself
    CHECK(peakIn(room, roomFrames - kRate / 5, roomFrames) < ps::OfflineRenderer::kSilence);
    CHECK(peakIn(room, bar, bar + kRate) > 1e-3f);  // not faded: still the full reverb after the stop
    CHECK(wet.progress() == 1.0f);

    // An echo of a short hit, half a note apart (1 s at 120 BPM): silent gaps between repeats
    // must not end the tail before the last audible one.
    ps::OfflineRenderer echo(kRate);
    echo.post({ps::CommandType::SetTempo, 120.0f});
    echo.post({ps::CommandType::SetDrumHit, 1.0f, 2, 15});  // a closed hat: short
    echo.post({ps::CommandType::SetSend, 1.0f, ps::Mixer::kDrums, 1});
    echo.post({ps::CommandType::SetBusParam, 1.0f, 1, 0});   // 1/2 note
    echo.post({ps::CommandType::SetBusParam, 0.25f, 1, 1});  // some feedback
    echo.setAutoTail(true);
    const auto echoes = echo.renderToBuffer(bar, cap);
    const int64_t echoFrames = int64_t(echoes.size() / 2);
    const int64_t hit = bar - bar / 16;  // the hat on step 15
    // The third repeat (3 s after the hat) is in the file, and audible.
    CHECK(echoFrames > hit + 3 * kRate + kRate / 10);
    CHECK(peakIn(echoes, hit + 3 * kRate - kRate / 50, hit + 3 * kRate + kRate / 20) > 1e-3f);
    CHECK(echoFrames < bar + cap);

    // Feedback at its highest: the echo outlives the cap, which the tail stops at, fading.
    ps::OfflineRenderer endless(kRate);
    endless.post({ps::CommandType::SetTempo, 120.0f});
    endless.post({ps::CommandType::SetDrumHit, 1.0f, 0, 15});
    endless.post({ps::CommandType::SetSend, 1.0f, 0, 1});
    endless.post({ps::CommandType::SetBusParam, 0.5f, 1, 0});
    endless.post({ps::CommandType::SetBusParam, 1.0f, 1, 1});
    endless.post({ps::CommandType::SetBusParam, 1.0f, 1, 3});  // bright: little lost per repeat
    endless.setAutoTail(true);
    const auto capped = endless.renderToBuffer(bar, cap);
    CHECK(int64_t(capped.size() / 2) == bar + cap);
    CHECK(peakIn(capped, bar + cap - 64, bar + cap) < 1e-3f);
    CHECK(peakIn(capped, bar + cap - kRate, bar + cap - kRate / 2) > 1e-3f);

    // Float WAV: values above 0 dBFS are kept, the header says IEEE float.
    const std::string path = (std::filesystem::temp_directory_path() / "ps_float_test.wav").string();
    ps::WavWriter w;
    CHECK(w.open(path.c_str(), kRate, 2, true));
    const float loud[4] = {1.5f, -0.25f, 0.5f, -2.0f};
    CHECK(w.write(loud, 2));
    CHECK(w.close());
    const auto f = readFile(path);
    CHECK(f.size() == 58 + 16);
    CHECK(f[20] == 3 && f[34] == 32 && std::memcmp(f.data() + 50, "data", 4) == 0 && le32(f.data() + 54) == 16);
    CHECK(le32(f.data() + 4) == f.size() - 8 && le32(f.data() + 46) == 2);
    float first = 0.0f, last = 0.0f;
    std::memcpy(&first, f.data() + 58, 4);
    std::memcpy(&last, f.data() + 58 + 12, 4);
    CHECK(first == 1.5f && last == -2.0f);
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
    CHECK(first == 24000 + kLate);
    // The sample's shape, not a kick: a click, silence, a smaller negative click, then nothing.
    const float a = out[size_t(24000 + kLate) * kChannels], b = out[size_t(24100 + kLate) * kChannels];
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
        CHECK(std::fabs(out[size_t(kLate) * kChannels] - 0.25f) < 1e-6f);
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
    renderMono(sampler, out.data(), 1000);
    sampler.trigger(0, 1.0f, 500);
    std::fill(out.begin(), out.end(), 0.0f);
    renderMono(sampler, out.data(), 1000);
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
    for (int done = 0; done < frames; done += block) renderMono(sampler, out.data() + done, std::min(block, frames - done));
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
    renderMono(sampler, tail.data(), 1000);
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
    renderMono(sampler, out.data(), 1000);
    CHECK(std::fabs(out[999] - 0.5f) < 1e-4f);
    sampler.trigger(1, 1.0f, 0);
    renderMono(sampler, out.data() + 1000, 1000);
    CHECK(out[1000 + 200] == 0.0f);
}

// --- Phase 29 B: sound continuity across sound and tempo changes -------------------------------

// Largest jump between two neighbouring samples of x[from..to).
float largestStep(const std::vector<float>& x, int from, int to) {
    float most = 0.0f;
    for (int i = std::max(1, from); i < to && i < static_cast<int>(x.size()); ++i) most = std::max(most, std::fabs(x[size_t(i)] - x[size_t(i) - 1]));
    return most;
}

void renderSamplerInto(ps::Sampler& sampler, std::vector<float>& out, int from, int to, int block = 128) {
    for (int done = from; done < to; done += block) renderMono(sampler, out.data() + done, std::min(block, to - done));
}

void testPadSoundSwapFadesAndKeepsTheLoop() {
    // A tempo-fitted loop: a 1 s ramp 0..1 looping; half way, its copy for a tempo 0.8 times as
    // fast lands (1.25 s, same ramp): the loop goes on from the same point, no jump, no gap.
    std::vector<float> rampA(48000), rampB(60000);
    for (int i = 0; i < 48000; ++i) rampA[size_t(i)] = i / 48000.0f;
    for (int i = 0; i < 60000; ++i) rampB[size_t(i)] = i / 60000.0f;
    ps::Sampler sampler;
    sampler.prepare(kRate);
    ps::Sample* a = ps::Sample::create(rampA.data(), 48000, 1, kRate, kRate);
    a->retain();  // ours, to watch when the sampler lets go
    sampler.install(0, a);
    sampler.setParam(0, ps::Sampler::Loop, 1.0f);
    std::vector<float> out(48000, 0.0f);
    sampler.trigger(0, 1.0f, 0);
    renderSamplerInto(sampler, out, 0, 24000);
    sampler.install(0, ps::Sample::create(rampB.data(), 60000, 1, kRate, kRate));
    CHECK(a->refCount() == 2);  // still fading out on the tail: not let go yet
    renderSamplerInto(sampler, out, 24000, 48000);
    const float jump = largestStep(out, 23900, 30000);
    std::printf("  loop swap: largest step %.5f, after %.4f (0.5167 wanted)\n", jump, out[25000]);
    CHECK(jump < 0.005f);
    CHECK(std::fabs(out[25000] - (0.5f + 1000.0f / 60000.0f)) < 0.002f);  // the new copy, same point
    CHECK(out[47000] > 0.85f);  // still looping on the new sound
    CHECK(a->refCount() == 1);  // the tail ended: the sampler let go of the old sound
    CHECK(a->release());
    delete a;

    // A one-shot replaced while it plays fades out in 5 ms instead of being cut; the next hit
    // plays the new sound.
    std::vector<float> dcHalf(48000, 0.5f), dcQuarter(48000, 0.25f);
    sampler.install(0, nullptr);  // the loop above leaves the mix
    ps::Sampler shots;
    shots.prepare(kRate);
    shots.install(1, ps::Sample::create(dcHalf.data(), 48000, 1, kRate, kRate));
    std::vector<float> shot(4000, 0.0f);
    shots.trigger(1, 1.0f, 0);
    renderSamplerInto(shots, shot, 0, 1000);
    shots.install(1, ps::Sample::create(dcQuarter.data(), 48000, 1, kRate, kRate));
    renderSamplerInto(shots, shot, 1000, 2000);
    std::printf("  one-shot swap: largest step %.5f\n", largestStep(shot, 990, 2000));
    CHECK(largestStep(shot, 990, 2000) < 0.01f);
    CHECK(shot[1100] > 0.2f);    // still fading
    CHECK(shot[1300] == 0.0f);   // then gone: a one-shot does not start again by itself
    shots.trigger(1, 1.0f, 0);
    renderSamplerInto(shots, shot, 2000, 4000);
    CHECK(std::fabs(shot[3000] - 0.25f) < 1e-4f);

    // Two swaps inside one fade, then none: every reference is released exactly once (ASan).
    ps::Sample* c = ps::Sample::create(dcHalf.data(), 48000, 1, kRate, kRate);
    c->retain();
    sampler.install(2, c);
    sampler.trigger(2, 1.0f, 0);
    std::vector<float> scratch(512, 0.0f);
    renderSamplerInto(sampler, scratch, 0, 256);
    sampler.install(2, ps::Sample::create(dcQuarter.data(), 48000, 1, kRate, kRate));
    sampler.install(2, ps::Sample::create(dcHalf.data(), 48000, 1, kRate, kRate));
    sampler.install(2, nullptr);
    CHECK(c->refCount() == 1);
    CHECK(!sampler.has(2));
    renderSamplerInto(sampler, scratch, 256, 512);
    sampler.collectGarbage();
    CHECK(c->release());
    delete c;
}

double pitchHz(const std::vector<float>& x, int from, int len);
std::vector<uint8_t> tinySoundFont();

void testArpeggioKeepsHeldKeysAcrossStop() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::Lfo1ToPitch});
    engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::SubLevel});
    engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::OscMix});
    engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::AmpRelease});
    engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::ArpOn});
    engine.post({ps::CommandType::SetExpression, 0.9f, ps::InstrumentTrack::ArpGate});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    for (int n : {57, 64}) engine.post({ps::CommandType::SynthNoteOn, 0.9f, n});
    render(engine, 6000 * 4, 100);
    // Stop with the keys still down: the arpeggio goes on with them.
    engine.post({ps::CommandType::SetTransport, 0.0f});
    auto out = render(engine, 6000 * 4, 100);
    std::vector<float> mono(out.size() / 2);
    float loud = 0.0f;
    for (size_t i = 0; i < mono.size(); ++i) {
        mono[i] = out[i * 2];
        if (i > 6000) loud = std::max(loud, std::fabs(mono[i]));
    }
    std::printf("  arpeggio after stop: peak %.3f\n", loud);
    CHECK(loud > 0.05f);
    // Keys let go while stopped leave it: silence, and ▶ does not bring them back.
    for (int n : {57, 64}) engine.post({ps::CommandType::SynthNoteOff, 0.0f, n});
    render(engine, kRate, 256);
    CHECK(engine.synthActiveVoices() == 0);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 3, 100);
    CHECK(engine.synthActiveVoices() == 0);
    // A panic forgets held keys.
    engine.post({ps::CommandType::SynthNoteOn, 0.9f, 60});
    render(engine, 6000, 100);
    engine.post({ps::CommandType::SynthAllOff});
    render(engine, kRate, 256);
    CHECK(engine.synthActiveVoices() == 0);
}

void testGeneralMidiPitchBend() {
    const std::string path = (std::filesystem::temp_directory_path() / "ps_bend.sf2").string();
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
        auto plain = render(engine, kRate / 4, 256);
        // The wheel up two semitones: the held A4 goes to B4.
        engine.post({ps::CommandType::SetExpression, 2.0f, ps::InstrumentTrack::Bend});
        auto up = render(engine, kRate / 4, 256);
        std::printf("  GM bend: %.1f Hz -> %.1f Hz (493.9 wanted)\n", pitchOf(plain, 2400, 12000), pitchOf(up, 2400, 12000));
        CHECK(std::fabs(pitchOf(plain, 2400, 12000) - 440.0) < 6.0);
        CHECK(std::fabs(pitchOf(up, 2400, 12000) - 493.88) < 7.0);
        // A note played while the wheel is down starts bent; back to the centre, back in tune.
        engine.post({ps::CommandType::SynthNoteOff, 0.0f, 69});
        engine.post({ps::CommandType::SetExpression, -12.0f, ps::InstrumentTrack::Bend});
        engine.post({ps::CommandType::SynthNoteOn, 1.0f, 81});
        auto down = render(engine, kRate / 4, 256);
        CHECK(std::fabs(pitchOf(down, 2400, 12000) - 440.0) < 6.0);
        engine.post({ps::CommandType::SetExpression, 0.0f, ps::InstrumentTrack::Bend});
        auto centre = render(engine, kRate / 4, 256);
        CHECK(std::fabs(pitchOf(centre, 2400, 12000) - 880.0) < 12.0);
        engine.post({ps::CommandType::SetExpression, NAN, ps::InstrumentTrack::Bend});  // ignored
        CHECK(!engine.hasNonFinite());
    }
    ps::SoundFontPlayer::close(master);
    std::filesystem::remove(path);
}

void testSoundSwapsNeverAllocate() {
#ifdef PS_COUNT_ALLOCATIONS
    // Pad sounds and Remix copies swapped while they play, a GM instrument bent, the transport
    // stopped and started with arpeggio keys held: no allocation in process().
    const std::string path = (std::filesystem::temp_directory_path() / "ps_swap_alloc.sf2").string();
    { auto bytes = tinySoundFont(); std::ofstream(path, std::ios::binary).write(reinterpret_cast<const char*>(bytes.data()), std::streamsize(bytes.size())); }
    tsf* master = ps::SoundFontPlayer::loadFile(path.c_str());
    std::vector<float> wave(4800);
    for (size_t i = 0; i < wave.size(); ++i) wave[i] = 0.3f * std::sin(0.05f * float(i));
    ps::Sample* one = ps::Sample::create(wave.data(), 4800, 1, kRate, kRate);
    ps::Sample* two = ps::Sample::create(wave.data(), 3000, 1, kRate, kRate);
    auto* song = ps::StemBundle::fromFloat(std::vector<std::vector<float>>(4, std::vector<float>(kRate * 20, 0.1f)));
    auto* copy = ps::StemBundle::fromFloat(std::vector<std::vector<float>>(4, std::vector<float>(kRate * 25, 0.1f)), song);
    {
        ps::Engine engine(kRate);
        one->retain();
        ps::Command pad{ps::CommandType::InstallSample, 0.0f, 3, 0};
        pad.sample = one;
        engine.post(pad);
        engine.post({ps::CommandType::SetPadParam, 1.0f, 3, ps::Sampler::Loop});
        song->retain();
        ps::Command stems{ps::CommandType::InstallStems};
        stems.stems = song;
        engine.post(stems);
        ps::Command inst{ps::CommandType::SetInstrument, 0.0f, 1};
        inst.font = ps::SoundFontPlayer::makeInstance(master, kRate);
        engine.post({ps::CommandType::SetTrackOn, 1.0f, 1});
        engine.post(inst);
        engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::ArpOn});
        for (int n : {60, 64, 67}) engine.post({ps::CommandType::SynthNoteOn, 0.8f, n});
        engine.post({ps::CommandType::SynthNoteOn, 0.8f, 69 | (1 << 16)});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        engine.post({ps::CommandType::TriggerPad, 1.0f, 3});
        std::vector<float> out(512 * 2);
        for (int i = 0; i < 30; ++i) engine.process(nullptr, 0, 0, out.data(), 512, 2);  // warm-up
        g_allocations = 0;
        g_countAllocations = true;
        for (int i = 0; i < 300; ++i) {
            if (i % 20 == 0) {
                ps::Sample* s = (i / 20) % 2 ? one : two;
                s->retain();
                ps::Command swap{ps::CommandType::InstallSample, 0.0f, 3, 0};
                swap.sample = s;
                engine.post(swap);
            }
            if (i % 33 == 0) engine.post({ps::CommandType::TriggerPad, 1.0f, 3});
            if (i % 40 == 7) {
                ps::StemBundle* b = (i / 40) % 2 ? song : copy;
                b->retain();
                ps::Command swap{ps::CommandType::InstallStems};
                swap.stems = b;
                engine.post(swap);
            }
            if (i % 9 == 0) engine.post({ps::CommandType::SetExpression, float(i % 5) - 2.0f, ps::InstrumentTrack::Bend | (1 << 16)});
            if (i % 75 == 30) engine.post({ps::CommandType::SetTransport, 0.0f});
            if (i % 75 == 45) engine.post({ps::CommandType::SetTransport, 1.0f, 0});
            engine.process(nullptr, 0, 0, out.data(), 512, 2);
        }
        g_countAllocations = false;
        std::printf("  allocations during sound swaps: %d\n", g_allocations.load());
        CHECK(g_allocations.load() == 0);
        CHECK(!engine.hasNonFinite());
        engine.collectGarbage();
    }
    CHECK(one->refCount() == 1 && two->refCount() == 1);  // none leaked by the swaps
    CHECK(song->refCount() == 1 && copy->refCount() == 1);
    CHECK(one->release() && two->release() && song->release() && copy->release());
    delete one;
    delete two;
    delete song;
    delete copy;
    ps::SoundFontPlayer::close(master);
    std::filesystem::remove(path);
#endif
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

void testPadStrips() {
    // Two fixed sounds (the synthesized snare draws noise: never the same twice): a low tone on pad
    // 0, a high one on pad 2, hit live after half a second for the faders to settle.
    std::vector<float> low(4800), high(4800);
    for (int i = 0; i < 4800; ++i) {
        const float fade = 1.0f - i / 4800.0f;
        low[size_t(i)] = 0.5f * fade * std::sin(2.0f * float(M_PI) * 110.0f * i / kRate);
        high[size_t(i)] = 0.5f * fade * std::sin(2.0f * float(M_PI) * 1760.0f * i / kRate);
    }
    auto engineWithSounds = [&]() {
        auto e = std::make_unique<ps::Engine>(kRate);
        e->post({ps::CommandType::InstallSample, 0.0f, 0, 0, ps::Sample::create(low.data(), 4800, 1, kRate, kRate)});
        e->post({ps::CommandType::InstallSample, 0.0f, 2, 0, ps::Sample::create(high.data(), 4800, 1, kRate, kRate)});
        return e;
    };
    auto hit = [](ps::Engine& e, bool kick, bool snare) {
        render(e, 24000, 256);
        if (kick) e.post({ps::CommandType::TriggerPad, 1.0f, 0});
        if (snare) e.post({ps::CommandType::TriggerPad, 1.0f, 2});
        return render(e, 9600, 256);
    };
    auto distance = [](const std::vector<float>& a, const std::vector<float>& b) {
        double d = 0.0;
        for (size_t i = 0; i < a.size(); ++i) d = std::max(d, static_cast<double>(std::fabs(a[i] - b[i])));
        return d;
    };
    auto energy = [](const std::vector<float>& v) {
        double e = 0.0;
        for (float x : v) e += static_cast<double>(x) * x;
        return e;
    };
    auto plain = engineWithSounds();
    auto alone = engineWithSounds();
    const auto both = hit(*plain, true, true);
    const auto snare = hit(*alone, false, true);
    CHECK(energy(both) > 1.5 * energy(snare));

    // A pad through its own strip, nothing on it: the same sound.
    auto routed = engineWithSounds();
    routed->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    CHECK(distance(hit(*routed, true, true), both) < 1e-4);

    // The kick's strip down: the snare alone is left, untouched.
    auto quiet = engineWithSounds();
    quiet->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    quiet->post({ps::CommandType::SetChannelVolume, 0.0f, ps::Mixer::kFirstPad + 0});
    CHECK(distance(hit(*quiet, true, true), snare) < 1e-4);

    // A pad not routed ignores its strip's settings.
    auto unrouted = engineWithSounds();
    unrouted->post({ps::CommandType::SetChannelVolume, 0.0f, ps::Mixer::kFirstPad + 2});
    CHECK(distance(hit(*unrouted, false, true), snare) < 1e-4);

    // The drums muted: their pads with them, strips included.
    auto muted = engineWithSounds();
    muted->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    muted->post({ps::CommandType::SetChannelMute, 1.0f, ps::Mixer::kDrums});
    CHECK(energy(hit(*muted, true, true)) < 1e-9);

    // The drums' fader and saturation apply exactly once to a routed pad: the same as unrouted.
    auto drumsSettings = [](ps::Engine& e) {
        e.post({ps::CommandType::SetChannelVolume, 0.5f, ps::Mixer::kDrums});
        e.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kDrums * ps::Mixer::kInserts, ps::InsertSlot::Saturation});
        e.post({ps::CommandType::SetInsertParam, 0.8f, ps::Mixer::kDrums * ps::Mixer::kInserts, 0});
    };
    auto shapedPlain = engineWithSounds();
    drumsSettings(*shapedPlain);
    auto shapedRouted = engineWithSounds();
    drumsSettings(*shapedRouted);
    shapedRouted->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    const auto shaped = hit(*shapedPlain, true, true);
    CHECK(distance(hit(*shapedRouted, true, true), shaped) < 1e-4);
    CHECK(distance(shaped, both) > 1e-2);  // the settings do change the sound

    // An effect on the kick's strip changes the kick, not the snare.
    auto crushed = engineWithSounds();
    crushed->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    crushed->post({ps::CommandType::SetInsertType, 0.0f, (ps::Mixer::kFirstPad + 0) * ps::Mixer::kInserts, ps::InsertSlot::Crusher});
    CHECK(distance(hit(*crushed, true, true), both) > 1e-2);
    auto crushedSnare = engineWithSounds();
    crushedSnare->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    crushedSnare->post({ps::CommandType::SetInsertType, 0.0f, (ps::Mixer::kFirstPad + 0) * ps::Mixer::kInserts, ps::InsertSlot::Crusher});
    CHECK(distance(hit(*crushedSnare, false, true), snare) < 1e-4);

    // A pad's reverb follows the drums' fader: drums down, nothing left, reverb included.
    auto wet = engineWithSounds();
    wet->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    wet->post({ps::CommandType::SetSend, 1.0f, ps::Mixer::kFirstPad + 0, 0});
    wet->post({ps::CommandType::SetChannelVolume, 0.4f, ps::Mixer::kDrums});
    auto dry = engineWithSounds();
    dry->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    dry->post({ps::CommandType::SetChannelVolume, 0.4f, ps::Mixer::kDrums});
    const double reverb = energy(hit(*wet, true, false)) - energy(hit(*dry, true, false));
    CHECK(reverb > 1e-6);
    auto wetDown = engineWithSounds();
    wetDown->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    wetDown->post({ps::CommandType::SetSend, 1.0f, ps::Mixer::kFirstPad + 0, 0});
    wetDown->post({ps::CommandType::SetChannelVolume, 0.0f, ps::Mixer::kDrums});
    CHECK(energy(hit(*wetDown, true, false)) < 1e-9);

    // Another strip soloed: the pads (routed or not) are cut with the drums.
    auto solo = engineWithSounds();
    solo->post({ps::CommandType::SetPadStrip, 1.0f, 0});
    solo->post({ps::CommandType::SetChannelSolo, 1.0f, 1});
    CHECK(energy(hit(*solo, true, true)) < 1e-9);
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

void testInstrumentTracks() {
    // 120 BPM: a step is 6000 frames. Track 2 plays an A4 at step 1; track 1 (channel 2) has no notes.
    ps::Engine engine(kRate);
    auto& mixer = engine.mixer();
    const int t = 2;
    engine.post({ps::CommandType::SetTempo, 120.0f});
    engine.post({ps::CommandType::SetTrackOn, 1.0f, t});
    engine.post({ps::CommandType::SetSynthSteps, 0.0f, 16 | (t << 16)});
    engine.post({ps::CommandType::SetSynthNote, 0.9f, 0 | (t << 16), ps::NoteTrack::pack(24, 96, 69)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1 | (t << 16)});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto out = render(engine, 5900, 256);
    CHECK(mixer.takeChannelPeak(ps::trackChannel(t)) == 0.0f);
    out = render(engine, 6000, 100);
    const float own = mixer.takeChannelPeak(ps::trackChannel(t));
    CHECK(own > 0.01f);
    CHECK(mixer.takeChannelPeak(ps::trackChannel(0)) == 0.0f);
    CHECK(engine.synthActiveVoices(t) == 1 && engine.synthActiveVoices(0) == 0);
    // Its own mixer channel: muting it silences the track and only it.
    engine.post({ps::CommandType::SetChannelMute, 1.0f, ps::trackChannel(t)});
    render(engine, 8000, 256);  // the fader glides down
    float loud = 0.0f;
    out = render(engine, 2000, 256);
    for (float v : out) loud = std::max(loud, std::fabs(v));
    CHECK(loud < 1e-4f);
    CHECK(engine.synthActiveVoices(t) == 1);  // still playing, just not heard
    engine.post({ps::CommandType::SetChannelMute, 0.0f, ps::trackChannel(t)});
    // Live notes go to the named track; a track switched off is silent and keeps nothing queued.
    engine.post({ps::CommandType::SynthNoteOn, 0.8f, 64 | (3 << 16)});
    render(engine, 512, 256);
    CHECK(engine.synthActiveVoices(3) == 0);  // track 3 is off: not rendered
    engine.post({ps::CommandType::SetTrackOn, 1.0f, 3});
    engine.post({ps::CommandType::SynthNoteOn, 0.8f, 64 | (3 << 16)});
    render(engine, 512, 256);
    CHECK(engine.synthActiveVoices(3) >= 1);
    engine.post({ps::CommandType::SetTrackOn, 0.0f, t});
    engine.post({ps::CommandType::SetTrackOn, 0.0f, 3});
    render(engine, kRate, 256);
    CHECK(engine.notesPending(t) == 0);
    mixer.takeChannelPeak(ps::trackChannel(t));
    render(engine, 6000 * 16, 256);
    CHECK(mixer.takeChannelPeak(ps::trackChannel(t)) == 0.0f);
    // Scenes: the waiting scene's notes of each track take over at the swap.
    engine.post({ps::CommandType::SetTrackOn, 1.0f, t});
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetSynthNote, 0.9f, 0 | (t << 16), ps::NoteTrack::pack(0, 48, 60)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1 | (t << 16)});
    engine.post({ps::CommandType::SetSynthSteps, 0.0f, 16 | (t << 16)});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    render(engine, 6000 * 16 + 3000, 256);  // through the next bar line
    CHECK(engine.sceneSwaps() == 1);
    CHECK(mixer.takeChannelPeak(ps::trackChannel(t)) > 0.01f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 256, 256);
    CHECK(engine.notesPending(t) == 0);
}

void testAudioTracks() {
    // 120 BPM: a step is 6000 frames. A clip (a loud mark, then quiet) on audio track 1 from step 4,
    // coming back every 16 steps.
    ps::Engine engine(kRate);
    auto& mixer = engine.mixer();
    std::vector<float> d(12000, 0.1f);
    std::fill(d.begin(), d.begin() + 100, 0.8f);
    ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 1};
    c.sample = ps::Sample::create(d.data(), 12000, 1, kRate, kRate);
    engine.post(c);
    engine.post({ps::CommandType::SetAudioClipParam, 4.0f, 1, ps::AudioTracks::Start});
    engine.post({ps::CommandType::SetAudioClipParam, 16.0f, 1, ps::AudioTracks::LoopSteps});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto out = render(engine, 6000 * 4 - 10, 256);
    CHECK(mixer.takeChannelPeak(ps::audioChannel(1)) == 0.0f);
    out = render(engine, 200, 50);
    CHECK(std::fabs(out[size_t(10 + 3 + kLate) * kChannels]) > 0.7f);  // sample-accurate start on step 4
    CHECK(mixer.takeChannelPeak(ps::audioChannel(1)) > 0.7f);
    out = render(engine, 6000 * 16, 256);  // past its end, then once more on the next pass
    CHECK(engine.audioClipPlaying(1));
    // A fade-in softens the start of each pass.
    engine.post({ps::CommandType::SetAudioClipParam, 4800.0f, 1, ps::AudioTracks::FadeIn});
    render(engine, 6000 * 16, 256);
    // A whole cycle with the fade-in: the loud first 100 frames of each pass are faded down.
    out = render(engine, 6000 * 16, 50);
    float mark = 0.0f;
    for (int i = 0; i < 6000 * 16; ++i) mark = std::max(mark, std::fabs(out[size_t(i) * kChannels]));
    CHECK(mark > 0.05f && mark < 0.2f);  // the 0.8 mark sits inside the fade-in: at most 100/4800 of it, plus the 0.1 body
    // Stop: fades out, nothing left.
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 2048, 256);
    CHECK(!engine.audioClipPlaying(1));
    ps::Command off{ps::CommandType::InstallAudioClip, 0.0f, 1};
    engine.post(off);
    render(engine, 256, 256);
    engine.collectGarbage();
}

// A song clip: its start counts from the song, it plays once straight across part changes (never
// restarted by a scene swap), a part's mute only silences it, and a song started from a later part
// picks it up in its middle. The offline render puts it on the same sample.
void testSongAudioClip() {
    // 120 BPM: a step is 6000 frames. The clip: a ramp (its frame index / 1e6) with a loud mark at 0.
    const int len = 6000 * 40;
    std::vector<float> d(static_cast<size_t>(len));
    for (int i = 0; i < len; ++i) d[size_t(i)] = float(i) * 1e-6f;
    std::fill(d.begin(), d.begin() + 50, 0.9f);
    auto install = [&](auto& target, int track) {
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, track};
        c.sample = ps::Sample::create(d.data(), len, 1, kRate, kRate);
        target.post(c);
        target.post({ps::CommandType::SetAudioClipParam, 1.0f, track, ps::AudioTracks::Song});
        target.post({ps::CommandType::SetAudioClipParam, 4.0f, track, ps::AudioTracks::Start});
        target.post({ps::CommandType::SetAudioClipParam, 8.0f, track, ps::AudioTracks::LoopSteps});  // ignored by a song clip
    };
    // The audio strip alone at unity (0.8 = 0 dB): the output is the clip itself.
    auto scene = [&](auto& target) {
        target.post({ps::CommandType::SetStaging, 1.0f});
        target.post({ps::CommandType::SetStaging, 0.0f});
        target.post({ps::CommandType::ArmSceneSwap, 0.0f, 16});  // the next part after one bar
    };
    ps::Engine engine(kRate);
    install(engine, 0);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    scene(engine);
    auto out = render(engine, 6000 * 30, 64);
    CHECK(engine.sceneSwaps() == 1);
    // Starts exactly on song step 4; a part change at step 16 does not restart it: the ramp goes on.
    CHECK(std::fabs(out[size_t(6000 * 4 - 1 + kLate) * kChannels]) < 1e-6f);
    CHECK(out[size_t(6000 * 4 + kLate) * kChannels] > 0.5f);
    const float at20 = out[size_t(6000 * 20) * kChannels];
    const float at16 = out[size_t(6000 * 16 + 100) * kChannels];
    const float at25 = out[size_t(6000 * 25) * kChannels];
    CHECK(at16 < 0.5f && at20 < 0.5f && at25 < 0.5f);  // no second mark after the swap
    CHECK(at25 > at20 && at20 > at16);
    const float ratio = out[size_t(6000 * 24) * kChannels] / out[size_t(6000 * 14) * kChannels];
    CHECK(std::fabs(ratio - 2.0f) < 0.01f);  // 20 steps vs 10 steps into the clip
    // A part that switches it off silences it; the next one brings it back where the song is.
    engine.post({ps::CommandType::SetAudioOn, 0.0f, 0});
    out = render(engine, 6000, 64);
    CHECK(std::fabs(out[size_t(5000) * kChannels]) < 1e-7f);
    CHECK(engine.audioClipPlaying(0));
    engine.post({ps::CommandType::SetAudioOn, 1.0f, 0});
    out = render(engine, 6000, 64);
    const float back = out[size_t(3000) * kChannels];
    CHECK(std::fabs(back - float(6000 * 27 + 3000) * 1e-6f) < 2e-4f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 1024, 256);
    CHECK(!engine.audioClipPlaying(0));

    // Started from a later part (song step 12): it plays from 8 steps into the clip at once.
    engine.post({ps::CommandType::SetTransport, 1.0f, 0, 12});
    out = render(engine, 6000, 64);
    CHECK(engine.songStep() == 12);
    CHECK(std::fabs(out[size_t(1000 + kLate) * kChannels] - float(6000 * 8 + 1000) * 1e-6f) < 2e-5f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 1024, 256);
    // The same with swing, from an odd start step: that step sounded half a step late, so the
    // clip is half a step less far in.
    engine.post({ps::CommandType::SetSwing, 0.5f});
    engine.post({ps::CommandType::SetAudioClipParam, 5.0f, 0, ps::AudioTracks::Start});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0, 14});
    out = render(engine, 6000, 64);
    CHECK(std::fabs(out[size_t(1000 + kLate) * kChannels] - float(6000 * 9 - 3000 + 1000) * 1e-6f) < 2e-5f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::SetSwing, 0.0f});
    engine.post({ps::CommandType::SetAudioClipParam, 4.0f, 0, ps::AudioTracks::Start});
    render(engine, 1024, 256);

    // Moved 120 frames later: the mark lands 120 frames after step 4; earlier: the clip starts 120 frames in.
    engine.post({ps::CommandType::SetAudioClipParam, 120.0f, 0, ps::AudioTracks::Offset});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    out = render(engine, 6000 * 5, 64);
    CHECK(std::fabs(out[size_t(6000 * 4 + 119 + kLate) * kChannels]) < 1e-6f && out[size_t(6000 * 4 + 120 + kLate) * kChannels] > 0.5f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 1024, 256);
    engine.post({ps::CommandType::SetAudioClipParam, -120.0f, 0, ps::AudioTracks::Offset});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    out = render(engine, 6000 * 5, 64);
    CHECK(std::fabs(out[size_t(6000 * 4 + kLate) * kChannels] - 120e-6f) < 1e-7f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::SetAudioClipParam, 0.0f, 0, ps::AudioTracks::Offset});
    render(engine, 1024, 256);

    // The export: the same song (swap after one bar) renders the clip on the same samples.
    ps::OfflineRenderer r(kRate);
    install(r, 0);
    r.scheduleFrom(3000);  // half a step in, like the app's song export
    scene(r);
    r.scheduleFrom(-1);
    const auto file = r.renderToBuffer(6000 * 30, 0);
    ps::Engine live(kRate);
    install(live, 0);
    live.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(live, 3000, 3000);
    scene(live);
    const auto heard = render(live, 6000 * 30 - 3000, 100);
    float diff = 0.0f;
    // Heard the limiter's look-ahead later than the file has it.
    for (int i = 0; i < 6000 * 30 - 3000 - kLate; ++i) diff = std::max(diff, std::fabs(file[size_t(i + 3000) * 2] - heard[size_t(i + kLate) * kChannels]));
    CHECK(file[size_t(6000 * 4) * 2] > 0.5f && std::fabs(file[size_t(6000 * 4 - 1) * 2]) < 1e-6f);
    CHECK(diff < 1e-6f);
    engine.post({ps::CommandType::InstallAudioClip, 0.0f, 0});
    live.post({ps::CommandType::InstallAudioClip, 0.0f, 0});
    render(engine, 256, 256);
    render(live, 256, 256);
    engine.collectGarbage();
    live.collectGarbage();
}

// A scene clip that plays once (loop 0): it does not come back before its scene starts again; a
// part's own loop, staged with its scene, takes over at the swap.
void testOnceAudioClip() {
    ps::Engine engine(kRate);
    std::vector<float> d(1000, 0.5f);
    ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 2};
    c.sample = ps::Sample::create(d.data(), 1000, 1, kRate, kRate);
    engine.post(c);
    engine.post({ps::CommandType::SetAudioClipParam, 0.0f, 2, ps::AudioTracks::LoopSteps});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 6000 * 2, 256);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(2)) > 0.3f);  // played on step 0
    render(engine, 6000 * 30, 256);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(2)) == 0.0f);  // never again
    // The next part: it plays again from its start, then every 4 steps (that part's own loop).
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetAudioClipParam, 4.0f, 2, ps::AudioTracks::LoopSteps});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    render(engine, 6000, 256);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(2)) == 0.0f);  // staged: not yet
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    render(engine, 6000 * 16, 256);
    CHECK(engine.sceneSwaps() == 1);
    engine.mixer().takeChannelPeak(ps::audioChannel(2));
    render(engine, 6000 * 4, 256);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(2)) > 0.3f);
    // A part staged with "once" (loop 0) that never came (Stop, or a cancelled swap) leaves
    // nothing behind: a later swap keeps the loop set since (every 4 steps).
    for (const bool cancel : {false, true}) {
        engine.post({ps::CommandType::SetStaging, 1.0f});
        engine.post({ps::CommandType::SetAudioClipParam, 0.0f, 2, ps::AudioTracks::LoopSteps});
        engine.post({ps::CommandType::SetStaging, 0.0f});
        if (cancel) engine.post({ps::CommandType::ArmSceneSwap, 0.0f, -1});
        engine.post({ps::CommandType::SetTransport, 0.0f, 0});
        engine.post({ps::CommandType::SetAudioClipParam, 4.0f, 2, ps::AudioTracks::LoopSteps});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});  // a plain scene change, nothing staged for audio
        render(engine, 6000 * 17, 256);
        CHECK(engine.sceneSwaps() == (cancel ? 3u : 2u));
        engine.mixer().takeChannelPeak(ps::audioChannel(2));
        render(engine, 6000 * 4 + 100, 256);
        CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(2)) > 0.3f);  // still every 4 steps after the swap
    }
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::InstallAudioClip, 0.0f, 2});
    render(engine, 512, 256);
    engine.collectGarbage();
}

// « Joue dans »: an effect kept to some scenes lets the sound through untouched elsewhere and
// switches on the scene swap's very frame (a 5 ms glide from there), live as in the export, whatever
// the block size. Here a saturation at volume 0 (silence) plays in scene B only: scene A's kicks
// come through untouched, scene B is silent once the glide is over, A again after the next swap.
void testInsertPlaysInSomeScenes() {
    const int step = 6000;  // 120 BPM
    const int slot = ps::Mixer::kDrums * ps::Mixer::kInserts + 2;
    auto setup = [&](auto& target, bool limited) {
        target.post({ps::CommandType::SetTempo, 120.0f});
        for (int st = 0; st < 16; st += 2) target.post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
        target.post({ps::CommandType::SetInsertType, 0.0f, slot, ps::InsertSlot::Saturation});
        target.post({ps::CommandType::SetInsertParam, 0.0f, slot, 3});  // volume 0: silence when heard
        if (limited) target.post({ps::CommandType::SetInsertOn, 0.0f, slot});  // not in scene A
    };
    // Scene B (the same kicks), its effect on, takes over after 16 steps; then A again, 16 later.
    auto stage = [&](auto& target, bool on, bool limited) {
        target.post({ps::CommandType::SetStaging, 1.0f});
        for (int st = 0; st < 16; st += 2) target.post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
        if (limited) target.post({ps::CommandType::SetInsertOn, on ? 1.0f : 0.0f, slot});
        target.post({ps::CommandType::SetStaging, 0.0f});
        target.post({ps::CommandType::ArmSceneSwap, 0.0f, 16});
    };
    const int frames = step * 40;
    auto exportOf = [&](bool limited) {
        ps::OfflineRenderer r(kRate);
        setup(r, limited);
        r.scheduleFrom(step / 2);
        stage(r, true, limited);
        r.scheduleFrom(step * 16 + step / 2);
        stage(r, false, limited);
        r.scheduleFrom(-1);
        return r.renderToBuffer(frames, 0);
    };
    const auto exported = exportOf(true);
    const auto always = exportOf(false);  // « Tout »: the effect plays everywhere (silence)
    // Live, in odd blocks, the scenes staged while the part before plays.
    ps::Engine engine(kRate);
    setup(engine, true);
    render(engine, kRate / 10, 100);
    engine.mixer().settle();
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto live = render(engine, step / 2, 97);
    stage(engine, true, true);
    auto more = render(engine, step * 16, 97);
    live.insert(live.end(), more.begin(), more.end());
    stage(engine, false, true);
    more = render(engine, frames - step / 2 - step * 16, 97);
    live.insert(live.end(), more.begin(), more.end());
    CHECK(engine.sceneSwaps() == 2u);
    CHECK(live.size() == exported.size());
    if (live.size() != exported.size() || always.size() != exported.size()) return;
    float diff = 0.0f;
    const size_t late = size_t(kLate) * kChannels;  // heard the limiter's look-ahead later
    for (size_t i = 0; i + late < live.size(); ++i) diff = std::max(diff, std::fabs(live[i + late] - exported[i]));
    CHECK(diff < 1e-6f);  // the export sounds as the live engine plays
    auto peak = [&](const std::vector<float>& v, int from, int to) {
        float p = 0.0f;
        for (int i = from * kChannels; i < to * kChannels; ++i) p = std::max(p, std::fabs(v[size_t(i)]));
        return p;
    };
    const int swap = step * 16, back = step * 32;
    const int glide = kRate * 5 / 1000;
    CHECK(peak(always, 0, frames) < 1e-6f);             // heard everywhere: silence all along
    CHECK(peak(exported, swap - step, swap) > 0.2f);    // scene A: the kicks, untouched
    CHECK(peak(exported, swap - 1, swap) > 1e-4f);      // still sounding on the last frame before the swap
    CHECK(peak(exported, swap + glide, back) < 1e-6f);  // scene B: silent once the glide is over
    CHECK(peak(exported, back + 1, back + step) > 0.2f);  // A again from the next swap
    // The switch starts on the swap's frame: its first frames are still almost the dry kick.
    CHECK(peak(exported, swap, swap + 10) > 0.5f * peak(exported, swap - 10, swap));
    // Stopped, a change of scene by the user switches it at once; a staged one that never comes
    // (cancelled) changes nothing at a later swap.
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::SetInsertOn, 1.0f, slot});
    render(engine, 256, 256);
    CHECK(engine.mixer().insertOn(ps::Mixer::kDrums, 2));
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetInsertOn, 0.0f, slot});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, -1});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    render(engine, step * 17, 256);
    CHECK(engine.sceneSwaps() == 3u);
    CHECK(engine.mixer().insertOn(ps::Mixer::kDrums, 2));
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 256, 256);
}

// Started from a later part, the song position (song automation, what the app records on) is the
// part's place in the song, not 0; the same when a scene hands over to that part.
void testSongStartsFromALaterPart() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTransport, 1.0f, 1, 64});  // a bar of count-in, then song step 64
    render(engine, kRate * 2 - 100, 100);
    CHECK(engine.songStep() == -1);
    render(engine, 6000 * 2, 100);
    CHECK(engine.songStep() == 65);
    // A song curve reads its value for bar 4 (step 64) at once, not bar 0's.
    ps::Engine curve(kRate);
    curve.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
    curve.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
    curve.post({ps::CommandType::SetAutomationMode, 8.0f, 0, ps::Automation::Song});
    for (int b = 0; b < 8; ++b) curve.post({ps::CommandType::SetAutomationSong, b >= 4 ? 0.8f : 0.0f, 0, b});
    curve.post({ps::CommandType::SetTransport, 1.0f, 0, 64});
    auto out = render(curve, 12000, 256);
    double sum = 0;
    for (int i = 6000; i < 12000; ++i) sum += double(out[size_t(i) * 2]) * out[size_t(i) * 2];
    CHECK(std::sqrt(sum / 6000) > 0.01);  // bar 0 would be silent
    // A scene playing hands over to song part 3 (song step 48): the song counts from there.
    ps::Engine scenes(kRate);
    scenes.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(scenes, 6000 * 6, 256);
    scenes.post({ps::CommandType::ArmSceneSwap, 48.0f, 0, 1});
    render(scenes, 6000 * 11, 256);  // past step 16
    CHECK(scenes.sceneSwaps() == 1);
    CHECK(scenes.songStep() == 48);
    // The song starting over from its first part counts from 0 again.
    scenes.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 1});
    render(scenes, 6000 * 16, 256);
    CHECK(scenes.songStep() == 0);
}

// A scene played on its own (transport started outside the song, b < 0) has no song position:
// song curves do not drive the mixer and song clips wait; a part that starts the song brings
// both back, and a scene that leaves the song (b = 3) stops them again.
void testSceneAloneIsOutsideTheSong() {
    auto setup = [](ps::Engine& e) {
        e.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
        // A song curve that silences the synth on every bar of the song.
        e.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
        e.post({ps::CommandType::SetAutomationMode, 8.0f, 0, ps::Automation::Song});
        for (int b = 0; b < 8; ++b) e.post({ps::CommandType::SetAutomationSong, 0.0f, 0, b});
        // A song clip on song step 0.
        std::vector<float> d(48000, 0.5f);
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 1};
        c.sample = ps::Sample::create(d.data(), 48000, 1, kRate, kRate);
        e.post(c);
        e.post({ps::CommandType::SetAudioClipParam, 1.0f, 1, ps::AudioTracks::Song});
    };
    // In the song: the curve silences the synth, the clip plays.
    ps::Engine song(kRate);
    setup(song);
    song.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
    render(song, 12000, 256);
    CHECK(song.songStep() == 1);
    CHECK(song.audioClipPlaying(1));
    song.mixer().takeChannelPeak(2);
    render(song, 6000, 256);
    CHECK(song.mixer().takeChannelPeak(2) < 1e-3f);
    // On its own: the synth keeps the mixer's level, the song clip stays silent, no song position.
    ps::Engine alone(kRate);
    setup(alone);
    alone.post({ps::CommandType::SetTransport, 1.0f, 0, -1});
    render(alone, 12000, 256);
    CHECK(alone.songStep() == -1);
    CHECK(!alone.audioClipPlaying(1));
    alone.mixer().takeChannelPeak(2);
    render(alone, 6000, 256);
    CHECK(alone.mixer().takeChannelPeak(2) > 0.01f);
    // The song starts from its first part at the next bar: song position from 0, the clip starts.
    alone.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 1});
    render(alone, 6000 * 16, 256);
    CHECK(alone.sceneSwaps() == 1);
    CHECK(alone.songStep() >= 0 && alone.songStep() < 4);
    CHECK(alone.audioClipPlaying(1));
    // A scene chosen to play on its own takes over at the next bar: outside the song again.
    alone.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 3});
    render(alone, 6000 * 16, 256);
    CHECK(alone.sceneSwaps() == 2);
    CHECK(alone.songStep() == -1);
    // The song again, then left right away (the song stops being followed: the scene loops on).
    alone.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 1});
    render(alone, 6000 * 16, 256);
    CHECK(alone.songStep() >= 0);
    alone.post({ps::CommandType::ArmSceneSwap, 0.0f, -1, 3});
    render(alone, 6000 * 2, 256);
    CHECK(alone.songStep() == -1);
    // A plain swap (b = 0, the song export's) keeps the song going.
    ps::Engine plain(kRate);
    plain.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
    plain.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 0});
    render(plain, 6000 * 17, 256);
    CHECK(plain.sceneSwaps() == 1);
    CHECK(plain.songStep() == 16);
    for (ps::Engine* e : {&song, &alone}) {
        e->post({ps::CommandType::SetTransport, 0.0f, 0});
        e->post({ps::CommandType::InstallAudioClip, 0.0f, 1});
        render(*e, 512, 256);
        e->collectGarbage();
    }
}

void testRecordingStartsOnTheFirstStep() {
    // The microphone sends a ramp (frame index); recording armed for the transport start keeps
    // exactly what came in from the first step, in order, and stops with the transport.
    ps::Engine engine(kRate);
    ps::RecordBuffer buffer(kRate);
    ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
    arm.record = &buffer;
    engine.post(arm);
    std::vector<float> out(256 * 2), in(256);
    int64_t micFrame = 0;
    auto block = [&](int n) {
        for (int i = 0; i < n; ++i) in[size_t(i)] = float(micFrame++);
        engine.process(in.data(), n, 1, out.data(), n, 2);
    };
    block(256);
    block(256);
    CHECK(buffer.state.load() == 0 && buffer.written.load() == 0);  // waiting for the transport
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    block(256);  // the transport starts at the top of this block
    CHECK(buffer.state.load() == 1);
    for (int k = 0; k < 10; ++k) block(256);
    CHECK(buffer.written.load() == 11 * 256);
    CHECK(buffer.data[0] == 512.0f && buffer.data[2815] == 512.0f + 2815.0f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    block(256);
    CHECK(buffer.state.load() == 2);
    CHECK(buffer.written.load() == 11 * 256);
    // Armed to start now, it stops by itself when the buffer is full.
    ps::RecordBuffer small(1000);
    ps::Command now{ps::CommandType::RecordArm, 0.0f, 0};
    now.record = &small;
    engine.post(now);
    for (int k = 0; k < 6; ++k) block(256);
    CHECK(small.state.load() == 2 && small.written.load() == 1000);
    // The microphone drops out mid-take (audio rerouted, being reopened): those callbacks record
    // silence, so what is sung afterwards stays in time with the transport.
    ps::RecordBuffer take(kRate);
    ps::Command go{ps::CommandType::RecordArm, 0.0f, 0};
    go.record = &take;
    engine.post(go);
    block(256);
    block(256);
    for (int k = 0; k < 3; ++k) engine.process(nullptr, 0, 0, out.data(), 256, 2);
    const float next = float(micFrame);
    block(256);
    CHECK(take.written.load() == 6 * 256);
    CHECK(take.data[600] == 0.0f && take.data[5 * 256] == next);
}

void testThreeFourAndPartMutes() {
    // 3/4 at 120 BPM: a bar is 12 sixteenths (72000 frames). A bar of count-in, then step 0.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetBeatsPerBar, 0.0f, 3});
    engine.post({ps::CommandType::SetMetronome, 1.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 1});
    render(engine, 72000 - 100, 100);
    CHECK(engine.currentStep() == -1);
    CHECK(engine.metronomeClicks() == 3);  // three clicks of count-in
    render(engine, 200, 100);
    CHECK(engine.currentStep() == 0);
    // A waiting scene takes over on the 3/4 bar line (local step 12), and with it the part's
    // audio track mute.
    std::vector<float> d(4000, 0.5f);
    ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 0};
    c.sample = ps::Sample::create(d.data(), 4000, 1, kRate, kRate);
    engine.post(c);
    engine.post({ps::CommandType::SetAudioClipParam, 4.0f, 0, ps::AudioTracks::LoopSteps});
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetAudioOn, 0.0f, 0});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    render(engine, 6000 * 11 + 100, 100);  // step 11
    CHECK(engine.sceneSwaps() == 0);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) > 0.1f);  // passes at steps 0, 4 and 8 sounded
    render(engine, 6000, 100);  // through step 12
    CHECK(engine.sceneSwaps() == 1);
    render(engine, 6000 * 4, 100);  // the next pass would start at step 16: the track is off
    engine.mixer().takeChannelPeak(ps::audioChannel(0));
    render(engine, 6000 * 4, 100);
    CHECK(engine.mixer().takeChannelPeak(ps::audioChannel(0)) == 0.0f);
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    engine.post({ps::CommandType::InstallAudioClip, 0.0f, 0});
    render(engine, 512, 256);
    engine.collectGarbage();
    // A one-bar 3/4 drum loop (12 steps) loops on its bar: the kick on step 0 fires every 72000 frames.
    ps::Engine waltz(kRate);
    waltz.post({ps::CommandType::SetBeatsPerBar, 0.0f, 3});
    waltz.post({ps::CommandType::SetPatternSteps, 0.0f, 12});
    waltz.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    waltz.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(waltz, 72000 * 3 - 1000, 100);
    CHECK(waltz.laneTriggerCount(0) == 3);
    render(waltz, 2000, 100);
    CHECK(waltz.laneTriggerCount(0) == 4);
    CHECK(waltz.currentStep() == 0);
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
    CHECK(first >= 9000 + kLate && first <= 9002 + kLate);
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
        // Two more instrument tracks: Plaits and the subtractive synth, each with its own notes.
        for (int t = 1; t <= 2; ++t) {
            engine.post({ps::CommandType::SetTrackOn, 1.0f, t});
            engine.post({ps::CommandType::SetSynthEngine, 0.0f, (t == 1 ? 1 : 0) | (t << 16)});
            for (int n = 0; n < 16; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n | (t << 16), ps::NoteTrack::pack(n * 24, 20, 60 + n % 12)});
            engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 16 | (t << 16)});
        }
        // Track 3: the sample played across the keys, gliding, with its own notes.
        engine.post({ps::CommandType::SetTrackOn, 1.0f, 3});
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, 2 | (3 << 16)});
        sample->retain();
        ps::Command keys{ps::CommandType::InstallTrackSample, 0.0f, 3 << 16};
        keys.sample = sample;
        engine.post(keys);
        engine.post({ps::CommandType::SetTrackSampleParam, 0.08f, ps::SampleInstrument::Glide | (3 << 16)});
        engine.post({ps::CommandType::SetTrackSampleParam, 1.0f, ps::SampleInstrument::Loop | (3 << 16)});
        for (int n = 0; n < 16; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n | (3 << 16), ps::NoteTrack::pack(n * 24, 30, 36 + n % 7)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 16 | (3 << 16)});
        // Phase 38: track 4 a multizone instrument (two 16-bit zones, a round robin), track 5 the 808.
        engine.post({ps::CommandType::SetTrackOn, 1.0f, 4});
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, 3 | (4 << 16)});
        for (int z = 0; z < 2; ++z) {
            ps::Command zone{ps::CommandType::InstallTrackSample, 0.0f, (4 << 16) | (z + 1)};
            zone.sample = ps::Sample::createCompact(wave.data(), 4800, 1, kRate, kRate);
            engine.post(zone);
            engine.post({ps::CommandType::SetTrackZone, 127.0f, ps::MultiSampler::HiKey | (4 << 16), z});
            engine.post({ps::CommandType::SetTrackZone, 2.0f, ps::MultiSampler::SeqLength | (4 << 16), z});
            engine.post({ps::CommandType::SetTrackZone, float(z + 1), ps::MultiSampler::SeqPosition | (4 << 16), z});
        }
        engine.post({ps::CommandType::SetTrackZone, 2.0f, ps::MultiSampler::Count | (4 << 16), -1});
        engine.post({ps::CommandType::SetTrackOn, 1.0f, 5});
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, 4 | (5 << 16)});
        for (int t = 4; t <= 5; ++t) {
            for (int n = 0; n < 16; ++n) engine.post({ps::CommandType::SetSynthNote, 0.8f, n | (t << 16), ps::NoteTrack::pack(n * 24, 30, 36 + n % 7)});
            engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 16 | (t << 16)});
        }
        // Live playing on track 2: arpeggio with sustain, bend and modulation.
        engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::ArpOn | (2 << 16)});
        engine.post({ps::CommandType::SetExpression, 0.5f, ps::InstrumentTrack::ArpRate | (2 << 16)});
        engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::Sustain | (2 << 16)});
        engine.post({ps::CommandType::SetExpression, 1.5f, ps::InstrumentTrack::Bend | (2 << 16)});
        engine.post({ps::CommandType::SetExpression, 0.7f, ps::InstrumentTrack::Mod | (2 << 16)});
        for (int n : {60, 64, 67}) engine.post({ps::CommandType::SynthNoteOn, 0.8f, n | (2 << 16)});
        // A song automation curve.
        engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 3, ps::Automation::pack(ps::Automation::Volume, 5, 0, 0)});
        engine.post({ps::CommandType::SetAutomationMode, 16.0f, 3, ps::Automation::Song});
        for (int b = 0; b < 16; ++b) engine.post({ps::CommandType::SetAutomationSong, b / 16.0f, 3, b});
        // And a 303 knob on a loop lane (it lands on the step's frame).
        engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 4, ps::Automation::pack(ps::Automation::Acid, 1, 0, ps::AcidTrack::Cutoff)});
        for (int st = 0; st < 16; ++st) engine.post({ps::CommandType::SetAutomationPoint, st / 16.0f, 4, st});
        // An audio track looping a clip, and the microphone being recorded.
        sample->retain();
        ps::Command audio{ps::CommandType::InstallAudioClip, 0.0f, 2};
        audio.sample = sample;
        engine.post(audio);
        engine.post({ps::CommandType::SetAudioClipParam, 4.0f, 2, ps::AudioTracks::LoopSteps});
        engine.tapStart();  // « Mode retours »: the output copied too (the ring fills, then drops)
        static ps::RecordBuffer recording(kRate * 4);
        ps::Command arm{ps::CommandType::RecordArm, 0.0f, 1};
        arm.record = &recording;
        engine.post(arm);
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
                engine.post({ps::CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts + slot, 1 + (ch + slot) % (ps::InsertSlot::kTypes - 1)});
            }
            if (ch < ps::Mixer::kMaster) {
                engine.post({ps::CommandType::SetSend, 0.5f, ch, 0});
                engine.post({ps::CommandType::SetSend, 0.5f, ch, 1});
            }
        }
        // Two pads through their own strips, with an effect and a send of their own.
        for (int lane : {0, 3}) {
            const int ch = ps::Mixer::kFirstPad + lane;
            engine.post({ps::CommandType::SetPadStrip, 1.0f, lane});
            engine.post({ps::CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts, 1 + lane % 13});
            engine.post({ps::CommandType::SetSend, 0.5f, ch, 0});
        }
        // A strip of long effects only (two of each kind and two short ones), for the swaps below.
        const int longCh = ps::Mixer::kFirstPad + 6;
        {
            const int types[ps::Mixer::kInserts] = {ps::InsertSlot::EchoFx, ps::InsertSlot::ReverbFx, ps::InsertSlot::TapeStopFx, ps::InsertSlot::PingPongFx,
                                                    ps::InsertSlot::ReverbFx, ps::InsertSlot::TapeStopFx, ps::InsertSlot::Chorus, ps::InsertSlot::FlangerFx};
            for (int k = 0; k < ps::Mixer::kInserts; ++k) engine.post({ps::CommandType::SetInsertType, 0.0f, longCh * ps::Mixer::kInserts + k, types[k]});
            engine.post({ps::CommandType::SetPadStrip, 1.0f, 6});
        }
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        std::vector<float> out(512 * 2);
        for (int i = 0; i < 50; ++i) engine.process(nullptr, 0, 0, out.data(), 512, 2);  // warm-up
        g_allocations = 0;
        g_countAllocations = true;
        uint32_t swapsSeen = engine.sceneSwaps();
        bool stagedOn = true;
        for (int i = 0; i < 400; ++i) {
            if (i % 50 == 0) engine.post({ps::CommandType::TriggerPad, 1.0f, 3});
            if (i % 35 == 0) engine.post({ps::CommandType::SetPadStrip, (i / 35) % 2 ? 0.0f : 1.0f, 3});  // routed, then not
            if (i % 70 == 0) engine.post({ps::CommandType::SynthNoteOn, 0.9f, 60 + i % 12});
            if (i % 40 == 0) engine.post({ps::CommandType::SetInsertParam, (i % 80) / 80.0f, 1, 0});
            if (i % 60 == 0) engine.post({ps::CommandType::SetInsertType, 0.0f, 5, 1 + (i / 60) % (ps::InsertSlot::kTypes - 1)});
            if (i % 65 == 0) engine.post({ps::CommandType::SetInsertType, 0.0f, 9, (i / 65) % 2 ? ps::InsertSlot::ReverbFx : ps::InsertSlot::EchoFx});
            if (i % 65 == 0) engine.post({ps::CommandType::SetInsertParam, 0.7f, 9, 3});  // waits for the swap's glide
            if (i % 90 == 45) engine.post({ps::CommandType::ClearInserts, 0.0f, ps::Mixer::kFirstPad + 3});  // a sounding strip emptied
            if (i % 55 == 0) engine.post({ps::CommandType::SetBusParam, (i % 110) / 110.0f, 1, 0});
            // A finger on the automated 303 cutoff, then let go (touch take-over).
            if (i % 20 == 0) engine.post({ps::CommandType::SetAutomationHold, (i / 20) % 2 ? 0.0f : 1.0f, 0,
                                          ps::Automation::pack(ps::Automation::Acid, 1, 0, ps::AcidTrack::Cutoff)});
            // « Joue dans » at the scene swaps (one per bar): the long effects (echo, reverb, tape
            // stop) and short ones leave at the first swap and come back at the next (back from
            // silence, each one's state is reset in the callback).
            if (i == 5 || engine.sceneSwaps() != swapsSeen) {
                swapsSeen = engine.sceneSwaps();
                stagedOn = !stagedOn;
                engine.post({ps::CommandType::SetStaging, 1.0f});
                for (int k = 0; k < ps::Mixer::kInserts; ++k) engine.post({ps::CommandType::SetInsertOn, stagedOn ? 1.0f : 0.0f, longCh * ps::Mixer::kInserts + k});
                for (int k = 0; k < ps::Mixer::kInserts; k += 2) engine.post({ps::CommandType::SetInsertOn, stagedOn ? 1.0f : 0.0f, k});
                engine.post({ps::CommandType::SetStaging, 0.0f});
                engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
            }
            if (i % 45 == 0) {
                for (int m = 0; m < ps::TrackMacros::kCount; ++m) engine.post({ps::CommandType::SetMacro, (i % 90) / 90.0f, (i / 45) % 4, m});
            }
            // Every live effect and setting, on and off (the grid on: they wait for its lines).
            if (i == 1) engine.post({ps::CommandType::SetPerformance, 0.5f, ps::PerformanceFx::Grid});
            if (i % 6 == 0 && (i / 6) % ps::PerformanceFx::kParams != ps::PerformanceFx::Grid) {
                engine.post({ps::CommandType::SetPerformance, (i % 12) ? 0.0f : 0.9f, (i / 6) % ps::PerformanceFx::kParams});
            }
            if (i % 15 == 0) engine.post({ps::CommandType::SetMacro, (i % 30) ? 0.0f : 1.0f, 2, ps::Mixer::kKillMacro + (i / 15) % 5});
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
        std::printf("  allocations during %d blocks: %d (%u scene swaps)\n", 400, g_allocations.load(), engine.sceneSwaps());
        CHECK(engine.sceneSwaps() >= 2u);  // the effects left at a swap and came back at the next
        if (engine.sceneSwaps() == 2u) CHECK(engine.mixer().insertOn(longCh, 0));  // back on at the second
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

// The look-ahead limiter: a burst 12 dB over the ceiling comes out under it from its very first
// frame (no overshoot), the gain glides down over the look-ahead instead of jumping on the peak
// (a jump is a click and distortion), the meter before the limiter shows the overload and the
// gain reduction is read. An instant-attack limiter jumps by the whole reduction in one frame.
void testLimiterLooksAhead() {
    ps::Mixer mixer;
    mixer.prepare(kRate);
    CHECK(mixer.latency() == kLate && kLate <= 72);  // at most 1.5 ms
    constexpr int frames = kRate / 2;
    const float burst = ps::Mixer::kCeiling * 4.0f;  // +12 dB over the ceiling
    std::vector<float> in(frames), zero(frames, 0.0f), out(size_t(frames) * 2);
    for (int i = 0; i < frames; ++i) in[size_t(i)] = i >= 12000 && i < 18000 ? burst : 0.3f;  // DC: the gain is out / in
    const float* ch[ps::Mixer::kChannels] = {in.data(), zero.data(), zero.data(), zero.data()};
    for (int pos = 0; pos < frames; pos += 192) {
        const int n = std::min(192, frames - pos);
        const float* part[ps::Mixer::kChannels] = {in.data() + pos, zero.data(), zero.data(), zero.data()};
        mixer.process(part, nullptr, out.data() + size_t(pos) * 2, n, 2);
    }
    (void)ch;
    float peak = 0.0f, worstStep = 0.0f, lowest = 1.0f;
    for (int i = 0; i + kLate < frames; ++i) {
        const float y = out[size_t(i + kLate) * 2];
        peak = std::max(peak, std::fabs(y));
        const float g = y / in[size_t(i)];
        lowest = std::min(lowest, g);
        // Before the burst the DC is only scaled (the burst's own edge, band-limited through the
        // clipper, starts a few frames early).
        if (i > 0 && i < 11990) worstStep = std::max(worstStep, std::fabs(g - out[size_t(i - 1 + kLate) * 2] / in[size_t(i - 1)]));
    }
    const float settled = out[size_t(17000 + kLate) * 2];
    std::printf("  limiter: peak %.4f, lowest gain %.3f, largest gain step %.4f, settled %.4f\n", double(peak), double(lowest), double(worstStep),
                double(settled));
    CHECK(peak <= ps::Mixer::kCeiling + 1e-6f);      // held from the first frame of the burst
    // -12 dB, plus under 2.5 dB where the clipper shaves the hard edge's first milliseconds (the
    // edge, band-limited, rings over the ceiling: the true-peak limiter takes that too).
    CHECK(lowest > 0.185f && lowest < 0.26f);
    CHECK(worstStep < 0.02f);  // glides over the look-ahead (an instant attack: 0.75 in one frame)
    CHECK(out[size_t(12000 + kLate - 1) * 2] < 0.3f);  // already gliding down before the burst
    CHECK(settled > 0.86f && settled < ps::Mixer::kCeiling);  // then just under the ceiling (its true-peak aim)
    // A loud sine through it: no distortion from the gain (it only glides), still under the ceiling.
    std::vector<float> sine(frames);
    for (int i = 0; i < frames; ++i) sine[size_t(i)] = 2.0f * static_cast<float>(std::sin(2.0 * M_PI * 100.0 * i / kRate));
    const float* sch[ps::Mixer::kChannels] = {sine.data(), zero.data(), zero.data(), zero.data()};
    mixer.takeMasterPeak();
    mixer.takeMasterInputPeak();
    mixer.takeLimiterGain();
    mixer.process(sch, nullptr, out.data(), frames, 2);
    float sinePeak = 0.0f;
    for (float v : out) sinePeak = std::max(sinePeak, std::fabs(v));
    CHECK(sinePeak <= ps::Mixer::kCeiling + 1e-6f && sinePeak > 0.85f);
    // The meters: before the limiter the overload (2.0), after it the ceiling, and the reduction.
    CHECK(mixer.takeMasterInputPeak() > 1.9f);
    CHECK(mixer.takeMasterPeak() <= ps::Mixer::kCeiling + 1e-6f);
    const float gr = mixer.takeLimiterGain();
    CHECK(gr < 0.5f && gr > 0.4f);
    CHECK(mixer.takeLimiterGain() == 1.0f);  // reading resets it
}

// Headroom: the default kit, the acid line and a synth chord at their default levels, four bars.
// At a master gain of 0 dB the limiter works hard; a new project's master starts lower (the app's
// Mixer.NEW_PROJECT_DB, -6 dB) and the limiter then takes at most 3 dB off the peaks.
float gainReductionDb(float masterGain) {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 130.0f});
    // As the app's starter project: kick on the beats, snare on 2 and 4, closed hats on the eighths, an open hat.
    for (int st : {0, 4, 8, 12}) engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, st});
    for (int st : {4, 12}) engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Snare, st});
    for (int st = 0; st < 16; st += 2) engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::ClosedHat, st});
    engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::OpenHat, 14});
    for (int st = 0; st < 16; ++st) {
        engine.post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{(st * 5) % 12, st % 7 == 3 ? 1 : 0, st % 4 == 2, st % 6 == 5, st % 8 != 7}.pack()});
    }
    engine.post({ps::CommandType::SetSynthSteps, 0.0f, 16});
    const int chord[3] = {57, 60, 64};
    for (int k = 0; k < 3; ++k) engine.post({ps::CommandType::SetSynthNote, 0.8f, k, ps::NoteTrack::pack(0, 16 * 24 - 12, chord[k])});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 3});
    engine.post({ps::CommandType::SetMaster, masterGain, 0});
    render(engine, kRate / 10, 256);
    engine.mixer().settle();
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    engine.mixer().takeLimiterGain();
    engine.mixer().takeMasterInputPeak();
    const int fourBars = static_cast<int>(4 * 4 * 60.0 / 130.0 * kRate);
    render(engine, fourBars, 256);
    const float gain = engine.mixer().takeLimiterGain();
    std::printf("  master %.1f dB: peak into the limiter %.2f, gain reduction %.1f dB\n", 20.0 * std::log10(double(masterGain)),
                double(engine.mixer().takeMasterInputPeak()), -20.0 * std::log10(double(gain)));
    return -20.0f * std::log10(gain);
}

void testNewProjectsHaveHeadroom() {
    const float atZero = gainReductionDb(1.0f);
    const float trimmed = gainReductionDb(0.5011872f);  // -6 dB
    CHECK(atZero > 3.0f);   // the old default: the limiter squashes the peaks
    CHECK(trimmed <= 3.0f);
}

// Swapping an insert while the strip sounds: the old effect fades out, the new one fades in, no
// click (a closed low-pass swapped for nothing used to jump from silence to the full tone at once).
// Parameters sent meanwhile go to the new effect; the swap lands on the same frame whatever the blocks.
void testInsertSwapHasNoClick() {
    constexpr int frames = kRate / 4;
    std::vector<float> tone(frames), zero(frames, 0.0f);
    for (int i = 0; i < frames; ++i) tone[size_t(i)] = 0.5f * static_cast<float>(std::sin(2.0 * M_PI * 220.0 * i / kRate));
    auto run = [&](int block, auto&& during) {
        auto mixer = std::make_unique<ps::Mixer>();
        mixer->prepare(kRate);
        mixer->setInsertType(0, 0, ps::InsertSlot::Filter);
        const float closed[4] = {0.0f, 0.0f, 0.0f, 1.0f};  // low-pass shut, fully wet: near silence
        for (int p = 0; p < 4; ++p) mixer->setInsertParam(0, 0, p, closed[p]);
        std::vector<float> out(size_t(frames) * 2);
        for (int pos = 0; pos < frames; pos += block) {
            const int n = std::min(block, frames - pos);
            if (pos == 23 * 256) during(*mixer);  // in the middle of a cycle of the tone
            const float* ch[ps::Mixer::kChannels] = {tone.data() + pos, zero.data(), zero.data(), zero.data()};
            mixer->process(ch, nullptr, out.data() + size_t(pos) * 2, n, 2);
        }
        return std::make_pair(std::move(mixer), out);
    };
    auto worstStep = [](const std::vector<float>& out) {
        float worst = 0.0f;
        for (size_t i = 2; i < out.size(); i += 2) worst = std::max(worst, std::fabs(out[i] - out[i - 2]));
        return worst;
    };
    const float toneStep = 0.5f * static_cast<float>(2.0 * M_PI * 220.0 / kRate);  // the tone's own largest step
    // Taken out (type None) while it plays: glides from near silence to the tone.
    auto [removed, out] = run(256, [](ps::Mixer& m) {
        m.setInsertType(0, 0, ps::InsertSlot::None);
        CHECK(m.pendingType(0, 0) == ps::InsertSlot::None);
        CHECK(m.insert(0, 0).type() == ps::InsertSlot::Filter);  // still fading out
    });
    std::printf("  insert swap: largest step %.4f (the tone's own %.4f)\n", double(worstStep(out)), double(toneStep));
    CHECK(worstStep(out) < 2.5f * toneStep);
    CHECK(removed->insert(0, 0).type() == ps::InsertSlot::None && removed->pendingType(0, 0) == -1);
    float tail = 0.0f;
    for (int i = frames - 2000; i < frames; ++i) tail = std::max(tail, std::fabs(out[size_t(i) * 2]));
    CHECK(tail > 0.45f);  // the tone, untouched, once the glide is over
    // Swapped for a crusher whose settings follow the swap: they reach the crusher, not the filter.
    auto swap = [](ps::Mixer& m) {
        m.setInsertType(0, 0, ps::InsertSlot::Saturation);
        m.setInsertParam(0, 0, 3, 0.0f);  // its volume at 0: silence once in
    };
    auto [swapped, a] = run(256, swap);
    auto [again, b] = run(64, swap);
    CHECK(worstStep(a) < 2.5f * toneStep);
    CHECK(swapped->insert(0, 0).type() == ps::InsertSlot::Saturation);
    float end = 0.0f;
    for (int i = frames - 2000; i < frames; ++i) end = std::max(end, std::fabs(a[size_t(i) * 2]));
    CHECK(end < 1e-3f);  // the new effect has its volume at 0
    float diff = 0.0f;
    for (size_t i = 0; i < a.size(); ++i) diff = std::max(diff, std::fabs(a[i] - b[i]));
    CHECK(diff < 1e-6f);  // the same in blocks of 256 and of 64 frames (the glide spans several)
    // Emptied while it plays (a strip let go): the long effect's unit goes back after the glide.
    auto [cleared, c] = run(256, [](ps::Mixer& m) {
        m.setInsertType(0, 1, ps::InsertSlot::ReverbFx);  // in from silence on the next frames
        m.clearInserts(0);
    });
    CHECK(worstStep(c) < 2.5f * toneStep);
    CHECK(cleared->insert(0, 0).type() == ps::InsertSlot::None && cleared->insert(0, 1).type() == ps::InsertSlot::None);
    CHECK(cleared->longFxPool().reverbs.available() == ps::LongFxPool::kReverbs);
    // « Joue dans » sent during the glide: the new effect starts out of the scene.
    auto [gated, d] = run(256, [](ps::Mixer& m) {
        m.setInsertType(0, 0, ps::InsertSlot::Saturation);
        m.setInsertParam(0, 0, 3, 0.0f);
        m.setInsertOn(0, 0, false);
    });
    CHECK(!gated->insertOn(0, 0));
    float through = 0.0f;
    for (int i = frames - 2000; i < frames; ++i) through = std::max(through, std::fabs(d[size_t(i) * 2]));
    CHECK(through > 0.45f);  // passed by: the tone untouched
    // Silent strip (before the first beat, an export's set-up): in at once, as always.
    ps::Mixer quiet;
    quiet.prepare(kRate);
    quiet.setInsertType(0, 0, ps::InsertSlot::Comp);
    CHECK(quiet.insert(0, 0).type() == ps::InsertSlot::Comp && quiet.pendingType(0, 0) == -1);
}

// The metronome (and the test tone) after the master's effects and the Live effects: a master
// low-pass shut or a tape stop leaves the click as it is.
void testMetronomeAfterTheMasterChain() {
    auto clickPeak = [](bool masterFx, bool tapeStop) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTempo, 120.0f});
        engine.post({ps::CommandType::SetMetronome, 1.0f});
        if (masterFx) {
            const int slot = ps::Mixer::kMaster * ps::Mixer::kInserts;
            engine.post({ps::CommandType::SetInsertType, 0.0f, slot, ps::InsertSlot::Filter});
            const float closed[4] = {0.0f, 0.0f, 0.0f, 1.0f};
            for (int p = 0; p < 4; ++p) engine.post({ps::CommandType::SetInsertParam, closed[p], slot, p});
        }
        if (tapeStop) engine.post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::TapeStop});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto out = render(engine, kRate * 2, 256);
        float peak = 0.0f;
        for (int i = kRate; i < kRate * 2; ++i) peak = std::max(peak, std::fabs(out[size_t(i) * kChannels]));
        return peak;
    };
    const float plain = clickPeak(false, false), filtered = clickPeak(true, false), stopped = clickPeak(false, true);
    std::printf("  click peak %.3f, under a shut master low-pass %.3f, in a tape stop %.3f\n", double(plain), double(filtered), double(stopped));
    CHECK(plain > 0.3f);
    CHECK(std::fabs(filtered - plain) < 1e-3f);
    CHECK(std::fabs(stopped - plain) < 1e-3f);
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
        CHECK(std::fabs(out[static_cast<size_t>(start - 1 + kLate) * 2]) < 1e-4f);  // silent through the count-in (metronome off-beat aside)
        const float left = out[static_cast<size_t>(start + 4800) * 2];
        CHECK(std::fabs(left - 0.5f) < 2e-3f);  // all four stems at unity on the remix channel
        // Right side = the ramp: the song position at step 0 is exactly the start frame (after a
        // 5 ms fade in from silence; the output is kLate frames late through the limiter).
        CHECK(std::fabs(out[static_cast<size_t>(start + kLate) * 2 + 1]) < 1e-4f);
        const float right = out[static_cast<size_t>(start + kLate + 480) * 2 + 1];
        CHECK(std::fabs(right - 0.25f * (kRate + 480) / songFrames) < 1e-3f);
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

void testCrazyEffects() {
    const size_t n = kRate;
    std::vector<float> l(n), r(n);
    auto sine = [&](float amp, float hz) {
        for (size_t i = 0; i < n; ++i) l[i] = r[i] = amp * float(std::sin(2.0 * M_PI * hz * double(i) / kRate));
    };
    auto run = [&](ps::InsertSlot& fx) {
        bool ok = true;
#if !defined(__SANITIZE_ADDRESS__)
        g_allocations = 0;
        g_countAllocations = true;
#endif
        for (size_t at = 0; at < n; at += 256) fx.process(l.data() + at, r.data() + at, int(std::min<size_t>(256, n - at)));
#if !defined(__SANITIZE_ADDRESS__)
        g_countAllocations = false;
        CHECK(g_allocations.load() == 0);  // nothing allocated in the audio callback (not countable under ASan)
#endif
        for (size_t i = 0; i < n; ++i) ok &= std::isfinite(l[i]) && std::isfinite(r[i]) && std::fabs(l[i]) < 1.5f && std::fabs(r[i]) < 1.5f;
        return ok;
    };
    auto rms = [&](size_t from, size_t to) {
        double s = 0;
        for (size_t i = from; i < to; ++i) s += double(l[i]) * l[i];
        return std::sqrt(s / double(to - from));
    };
    // Robot: the original pitch is gone (a ring modulator keeps only sum and difference tones).
    {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(ps::InsertSlot::RobotFx);
        sine(0.3f, 440.0f);
        std::vector<float> dry = l;
        CHECK(run(fx));
        double dot = 0, a = 0, b = 0;
        for (size_t i = n / 2; i < n; ++i) { dot += double(l[i]) * dry[i]; a += double(l[i]) * l[i]; b += double(dry[i]) * dry[i]; }
        CHECK(std::fabs(dot) / std::sqrt(a * b) < 0.1);
        CHECK(rms(n / 2, n) > 0.05);  // and something plays
    }
    // Wah-wah: a loud note opens the filter far above a quiet one.
    {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(ps::InsertSlot::WahFx);
        sine(0.002f, 220.0f);
        CHECK(run(fx));
        ps::InsertSlot loud;
        loud.prepare(kRate);
        loud.setType(ps::InsertSlot::WahFx);
        sine(0.5f, 220.0f);
        CHECK(run(loud));
        // Quiet: near the lowest frequency; loud: more than two octaves up (read through the slot's sound).
        ps::AutoWah quietWah, loudWah;
        quietWah.prepare(kRate);
        loudWah.prepare(kRate);
        sine(0.002f, 220.0f);
        quietWah.process(l.data(), r.data(), int(n));
        sine(0.5f, 220.0f);
        loudWah.process(l.data(), r.data(), int(n));
        CHECK(loudWah.cutoff() > 4.0f * quietWah.cutoff());
    }
    // Old record: crackles on silence with crackle up, true silence without; wow 0 adds no delay.
    {
        ps::Vinyl vinyl;
        vinyl.prepare(kRate);
        vinyl.setParam(0, 0.0f);
        vinyl.setParam(1, 1.0f);
        vinyl.setParam(2, 0.0f);  // no hiss: only the crackles
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        vinyl.process(l.data(), r.data(), int(n));
        float peak = 0;
        for (float x : l) peak = std::max(peak, std::fabs(x));
        CHECK(peak > 0.01f);
        ps::Vinyl quiet;
        quiet.prepare(kRate);
        quiet.setParam(0, 0.0f);
        quiet.setParam(1, 0.0f);
        quiet.setParam(2, 0.0f);
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        quiet.process(l.data(), r.data(), int(n));
        peak = 0;
        for (float x : l) peak = std::max(peak, std::fabs(x));
        CHECK(peak == 0.0f);
        // An impulse comes out at once (no delay without wow), dulled by nothing at age 0.
        ps::Vinyl direct;
        direct.prepare(kRate);
        direct.setParam(0, 0.0f);
        direct.setParam(1, 0.0f);
        direct.setParam(2, 0.0f);
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        l[0] = r[0] = 1.0f;
        direct.process(l.data(), r.data(), 64);
        int at = 0;
        for (int i = 1; i < 64; ++i) if (std::fabs(l[i]) > std::fabs(l[at])) at = i;
        CHECK(at < 4);
    }
    // Old record: turning the wow knob while a tone plays makes no jump (the delay glides).
    {
        ps::Vinyl vinyl;
        vinyl.prepare(kRate);
        vinyl.setParam(1, 0.0f);
        vinyl.setParam(2, 0.0f);
        vinyl.setParam(0, 0.4f);
        vinyl.reset();
        std::vector<float> a(256), b(256);
        double t = 0;
        float prev = 0, worst = 0, normal = 0;
        for (int block = 0; block < 200; ++block) {
            if (block == 100) vinyl.setParam(0, 0.45f);  // one notch, mid-tone
            for (int i = 0; i < 256; ++i, t += 1.0) a[size_t(i)] = b[size_t(i)] = 0.75f * float(std::sin(2.0 * M_PI * 700.0 * t / kRate));
            vinyl.process(a.data(), b.data(), 256);
            for (int i = 0; i < 256; ++i) {
                const float step = std::fabs(a[size_t(i)] - prev);
                prev = a[size_t(i)];
                if (block > 10 && block < 100) normal = std::max(normal, step);
                if (block >= 100) worst = std::max(worst, step);
            }
        }
        CHECK(worst < 1.5f * normal);
    }
    // Old record and walkie-talkie: clean numbers; the radio drops the bass (a 60 Hz hum).
    for (int type : {ps::InsertSlot::VinylFx, ps::InsertSlot::RadioFx}) {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(type);
        fx.setParam(1, 1.0f);  // all the crackle / all the drive
        sine(0.4f, 440.0f);
        CHECK(run(fx));
        CHECK(rms(n / 2, n) > 0.02);
    }
    {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(ps::InsertSlot::RadioFx);
        fx.setParam(1, 0.0f);
        fx.setParam(2, 0.0f);
        sine(0.3f, 60.0f);
        CHECK(run(fx));
        CHECK(rms(n / 2, n) < 0.3 * 0.71 * 0.1);  // more than 20 dB down
    }
    // Hachoir: the tempo comes from the sequencer's step length (no beat needed to measure it)
    // and a beat landing mid-block restarts the pattern there.
    {
        ps::Chopper chop;
        chop.prepare(kRate);
        chop.setParam(1, 1.0f);
        chop.setParam(2, 0.0f);
        chop.setStepFrames(7500.0);
        std::vector<float> a(256, 0.5f), b(256, 0.5f);
        chop.beat(100);  // the beat lands 100 frames into this block
        chop.process(a.data(), b.data(), 256);
        CHECK(std::fabs(a[255] - 0.5f) < 0.01f);  // the 1st sixteenth plays
        // Half a sixteenth later than a full one (7500 + 3750 frames after the beat): cut.
        int64_t done = 256 - 100;
        while (done < 7500 + 3750) {
            std::fill(a.begin(), a.end(), 0.5f);
            std::fill(b.begin(), b.end(), 0.5f);
            chop.process(a.data(), b.data(), 256);
            done += 256;
        }
        CHECK(std::fabs(a[255]) < 0.01f);
    }
    // Hachoir: in time with the beats it is given. Pattern "x.x." at depth 1, a beat every 24000
    // frames: the 1st sixteenth plays, the 2nd is cut.
    {
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(ps::InsertSlot::ChopperFx);
        fx.setParam(0, 0.0f);
        fx.setParam(1, 1.0f);
        fx.setParam(2, 0.0f);  // 1 ms smoothing
        fx.setStepFrames(7500.0);  // 96 BPM, as the sequencer tells every block
        std::vector<float> one(1000, 0.5f), two(1000, 0.5f);
        const int beat = 30000;
        for (int at = 0; at < 4 * beat; at += 1000) {
            if (at % beat == 0) fx.sidechain(0, false, true);
            std::fill(one.begin(), one.end(), 0.5f);
            std::fill(two.begin(), two.end(), 0.5f);
            fx.process(one.data(), two.data(), 1000);
            const int inBeat = at % beat;
            if (at >= 2 * beat) {
                if (inBeat >= 1000 && inBeat + 1000 < beat / 4) CHECK(std::fabs(one[999] - 0.5f) < 0.01f);  // 1st sixteenth: plays
                if (inBeat >= beat / 4 + 1000 && inBeat + 1000 < beat / 2) CHECK(std::fabs(one[999]) < 0.01f);  // 2nd: cut
            }
        }
    }
}

void testNewEffects() {
    // Every new effect: finite, bounded, and audibly doing its job on a stereo sine.
    auto sine = [](std::vector<float>& l, std::vector<float>& r, float amp, float hz, float phaseR) {
        for (size_t i = 0; i < l.size(); ++i) {
            l[i] = amp * float(std::sin(2.0 * 3.14159265 * hz * double(i) / kRate));
            r[i] = amp * float(std::sin(2.0 * 3.14159265 * hz * double(i) / kRate + phaseR));
        }
    };
    auto run = [](ps::InsertSlot& fx, std::vector<float>& l, std::vector<float>& r) {
        for (size_t at = 0; at < l.size(); at += 256) fx.process(l.data() + at, r.data() + at, int(std::min<size_t>(256, l.size() - at)));
        bool ok = true;
        for (size_t i = 0; i < l.size(); ++i) ok &= std::isfinite(l[i]) && std::isfinite(r[i]) && std::fabs(l[i]) < 2.0f && std::fabs(r[i]) < 2.0f;
        return ok;
    };
    auto rms = [](const std::vector<float>& x, size_t from, size_t to) {
        double s = 0;
        for (size_t i = from; i < to; ++i) s += double(x[i]) * x[i];
        return std::sqrt(s / double(to - from));
    };
    const size_t n = kRate;
    std::vector<float> l(n), r(n), dryL(n);
    for (int type = ps::InsertSlot::FlangerFx; type < ps::InsertSlot::kTypes; ++type) {
        if (ps::InsertSlot::isLong(type)) continue;  // they need the mixer's pool: testElectronicEffects
        ps::InsertSlot fx;
        fx.prepare(kRate);
        fx.setType(type);
        sine(l, r, 0.3f, 440.0f, 0.0f);
        dryL = l;
        CHECK(run(fx, l, r));
        double diff = 0;
        for (size_t i = n / 2; i < n; ++i) diff += std::fabs(double(l[i]) - dryL[i]) + std::fabs(double(r[i]) - dryL[i]);
        // The bass exciter adds nothing to a 440 Hz sine (no low band): its job is checked on a bass (bass_exciter).
        if (type != ps::InsertSlot::WidenerFx && type != ps::InsertSlot::GateFx && type != ps::InsertSlot::ParamEqFx && type != ps::InsertSlot::BassExciterFx) {
            CHECK(diff / double(n) > 1e-3);  // it changes the sound
        }
    }
    // Gate: a quiet signal (-50 dB) is shut, a loud one passes.
    ps::InsertSlot gate;
    gate.prepare(kRate);
    gate.setType(ps::InsertSlot::GateFx);
    gate.setParam(0, 0.5f);  // -40 dB threshold
    sine(l, r, 0.003f, 440.0f, 0.0f);
    run(gate, l, r);
    CHECK(rms(l, n / 2, n) < 0.003 * 0.71 * 0.05);
    sine(l, r, 0.3f, 440.0f, 0.0f);
    run(gate, l, r);
    CHECK(rms(l, n / 2, n) > 0.3 * 0.71 * 0.95);
    // Widener: width 0 makes it mono, 1 makes the sides wider.
    ps::InsertSlot wide;
    wide.prepare(kRate);
    wide.setType(ps::InsertSlot::WidenerFx);
    wide.setParam(0, 0.0f);
    sine(l, r, 0.3f, 440.0f, 1.0f);
    run(wide, l, r);
    double side = 0;
    for (size_t i = 0; i < n; ++i) side += std::fabs(double(l[i]) - r[i]);
    CHECK(side < 1e-6);
    // Bitcrusher at 2 bits: only a few levels remain.
    ps::InsertSlot crush;
    crush.prepare(kRate);
    crush.setType(ps::InsertSlot::Crusher);
    crush.setParam(0, 1.0f);
    crush.setParam(3, std::sqrt(0.5f));  // volume x1
    sine(l, r, 0.9f, 440.0f, 0.0f);
    run(crush, l, r);
    std::vector<float> levels;
    for (float v : l) if (std::none_of(levels.begin(), levels.end(), [&](float x) { return std::fabs(x - v) < 1e-4f; })) levels.push_back(v);
    CHECK(levels.size() <= 5);
    // Tremolo at full depth: the level swings between nearly silent and full.
    ps::InsertSlot trem;
    trem.prepare(kRate);
    trem.setType(ps::InsertSlot::TremoloFx);
    trem.setParam(1, 1.0f);
    sine(l, r, 0.3f, 440.0f, 0.0f);
    run(trem, l, r);
    double lo = 1, hi = 0;
    for (size_t at = 0; at + 480 <= n; at += 480) { const double v = rms(l, at, at + 480); lo = std::min(lo, v); hi = std::max(hi, v); }
    CHECK(lo < 0.03 && hi > 0.18);
}

// Goertzel: the level of `hz` in x[from, to).
double toneLevel(const std::vector<float>& x, size_t from, size_t to, double hz) {
    double re = 0, im = 0;
    for (size_t i = from; i < to; ++i) {
        re += x[i] * std::cos(2 * M_PI * hz * double(i) / kRate);
        im += x[i] * std::sin(2 * M_PI * hz * double(i) / kRate);
    }
    return 2.0 * std::sqrt(re * re + im * im) / double(to - from);
}

void testElectronicEffects() {
    // Phase 25: distortion, echo, ping-pong, reverb, auto-pan, OTT, frequency shifter, tape stop.
    const int kNew[] = {ps::InsertSlot::DistortionFx, ps::InsertSlot::EchoFx, ps::InsertSlot::PingPongFx, ps::InsertSlot::ReverbFx,
                        ps::InsertSlot::AutoPanFx, ps::InsertSlot::OttFx, ps::InsertSlot::FreqShiftFx, ps::InsertSlot::TapeStopFx};
    // 1. Every one on a mixer strip (the long ones borrow from the pool): no allocation, no NaN,
    // bounded even with a loud, noisy input and every knob at 0, in the middle, then at 1.
    {
        std::mt19937 rng(7);
        std::uniform_real_distribution<float> noise(-0.3f, 0.3f);
        const int frames = 4096;
        std::vector<float> in(frames), out(size_t(frames) * 2), zero(frames, 0.0f);
        for (int i = 0; i < frames; ++i) in[size_t(i)] = 0.7f * float(std::sin(2 * M_PI * 110.0 * i / kRate)) + noise(rng);
        for (int type : kNew) {
            for (float knob : {0.0f, 0.5f, 1.0f}) {
                ps::Mixer mixer;
                mixer.prepare(kRate);
                mixer.setLimiter(false);  // the effect's own level is what is bounded, not the limiter's
                mixer.setStepFrames(6000.0);
                mixer.setInsertType(2, 0, type);
                for (int k = 0; k < 4; ++k) mixer.setInsertParam(2, 0, k, knob);
                CHECK(mixer.insert(2, 0).hasUnit() == ps::InsertSlot::isLong(type));
                const float* ch[ps::Mixer::kChannels] = {zero.data(), zero.data(), in.data(), zero.data()};
                bool ok = true;
                float peak = 0.0f;
#if !defined(__SANITIZE_ADDRESS__)
                g_allocations = 0;
                g_countAllocations = true;
#endif
                for (int pass = 0; pass < 24; ++pass) {
                    if (pass % 4 == 0) mixer.sidechainEvent(100, true, true);
                    mixer.process(ch, nullptr, out.data(), frames, 2);
                    for (float v : out) { ok &= std::isfinite(v); peak = std::max(peak, std::fabs(v)); }
                }
#if !defined(__SANITIZE_ADDRESS__)
                g_countAllocations = false;
                CHECK(g_allocations.load() == 0);
#endif
                if (!ok) std::fprintf(stderr, "  type %d knob %.1f: not finite\n", type, knob);
                CHECK(ok);
                CHECK(!mixer.takeBlewUp());
                if (peak >= 4.0f) std::fprintf(stderr, "  type %d knob %.1f: peak %.2f\n", type, knob, peak);
                CHECK(peak < 4.0f);
            }
            // Straight on the slot (pre-limiter): bounded too.
            ps::Mixer mixer;
            mixer.prepare(kRate);
            mixer.setInsertType(2, 0, type);
            for (int k = 0; k < 4; ++k) mixer.setInsertParam(2, 0, k, 1.0f);
            std::vector<float> l(in), r(in);
            auto& slot = const_cast<ps::InsertSlot&>(mixer.insert(2, 0));
            float peak = 0.0f;
            for (int pass = 0; pass < 12; ++pass) {
                l = in;
                r = in;
                slot.process(l.data(), r.data(), frames);
                for (int i = 0; i < frames; ++i) peak = std::max(peak, std::max(std::fabs(l[size_t(i)]), std::fabs(r[size_t(i)])));
            }
            std::printf("  type %d at full: peak %.2f\n", type, peak);
            CHECK(std::isfinite(peak) && peak < 4.0f);
        }
    }
    // 2. Distortion: every curve stays in [-1, 1]; it adds harmonics; a hot drive is not much
    // louder than a light one (levelled); the four types sound different.
    {
        for (int t = 0; t < ps::Distortion::kTypes; ++t) {
            for (float x = -80.0f; x <= 80.0f; x += 0.37f) CHECK(std::fabs(ps::Distortion::shape(t, x)) <= 1.0001f);
        }
        auto run = [](float drive, float type) {
            ps::Distortion d;
            d.prepare(kRate);
            d.setParam(0, drive);
            d.setParam(1, 1.0f);
            d.setParam(2, type);
            std::vector<float> l(9600), r(9600);
            for (size_t i = 0; i < l.size(); ++i) l[i] = r[i] = 0.5f * float(std::sin(2 * M_PI * 1000.0 * double(i) / kRate));
            d.process(l.data(), r.data(), int(l.size()));
            return l;
        };
        const auto light = run(0.1f, 0.0f), hot = run(1.0f, 0.0f);
        double rl = 0, rh = 0;
        for (size_t i = 4800; i < 9600; ++i) { rl += light[i] * light[i]; rh += hot[i] * hot[i]; }
        std::printf("  distortion: light %.3f, hot %.3f rms; 3rd harmonic %.3f\n", std::sqrt(rl / 4800), std::sqrt(rh / 4800), toneLevel(hot, 4800, 9600, 3000));
        CHECK(std::sqrt(rh / rl) < 2.0);
        CHECK(toneLevel(hot, 4800, 9600, 3000) > 0.05);
        CHECK(toneLevel(light, 4800, 9600, 3000) < toneLevel(hot, 4800, 9600, 3000));
        // Fuzz is lopsided: an even harmonic (2 kHz) the symmetric overdrive does not make.
        const auto fuzz = run(0.7f, 0.3f), over = run(0.7f, 0.0f);
        CHECK(toneLevel(fuzz, 4800, 9600, 2000) > 0.02);
        CHECK(toneLevel(over, 4800, 9600, 2000) < 0.005);
        const auto soft = run(0.35f, 0.0f), clip = run(0.35f, 0.6f), fold = run(0.35f, 0.9f);
        double d1 = 0, d2 = 0;
        for (size_t i = 4800; i < 9600; ++i) { d1 += std::fabs(clip[i] - soft[i]); d2 += std::fabs(fold[i] - clip[i]); }
        std::printf("  distortion types: clip vs overdrive %.3f, foldback vs clip %.3f\n", d1 / 4800, d2 / 4800);
        CHECK(d1 / 4800 > 0.01 && d2 / 4800 > 0.01);
    }
    // 3. Insert echo: an impulse comes back after a dotted eighth (3 sixteenths: 18000 frames at
    // 120 BPM), the dry sound untouched below mid-mix; ping-pong: the first repeat on the left only,
    // the second on the right.
    {
        ps::InsertEcho echo;
        echo.prepare(kRate);
        echo.setStepFrames(6000.0);
        echo.setParam(0, 0.42f);  // dotted 1/8
        echo.setParam(1, 0.5f);
        echo.setParam(2, 1.0f);   // bright repeats
        echo.setParam(3, 0.5f);
        echo.reset();
        std::vector<float> l(kRate, 0.0f), r(kRate, 0.0f);
        l[100] = r[100] = 1.0f;
        echo.process(l.data(), r.data(), kRate);
        CHECK(std::fabs(l[100] - 1.0f) < 1e-4f);
        const auto first = std::max_element(l.begin() + 200, l.begin() + 30000) - l.begin();
        std::printf("  insert echo at frame %ld (expected %d)\n", long(first), 18100);
        CHECK(std::labs(long(first) - 18100) <= 2);
        CHECK(l[size_t(first)] > 0.3f && std::fabs(r[size_t(first)] - l[size_t(first)]) < 1e-3f);
        echo.setPingPong(true);
        echo.setParam(0, 0.25f);  // 1/8
        echo.reset();
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        l[100] = r[100] = 1.0f;
        echo.process(l.data(), r.data(), kRate);
        CHECK(l[12100] > 0.2f && std::fabs(r[12100]) < 1e-3f);
        CHECK(std::fabs(l[24100]) < 1e-3f && r[24100] > 0.05f);
        // Mix 0: only the dry sound.
        echo.setParam(3, 0.0f);
        echo.reset();
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        l[100] = r[100] = 1.0f;
        echo.process(l.data(), r.data(), kRate);
        float rest = 0;
        for (size_t i = 200; i < l.size(); ++i) rest = std::max(rest, std::fabs(l[i]));
        CHECK(rest < 1e-6f);
    }
    // 4. Insert reverb: a tail that lasts and decays after a click; dry at mix 0.
    {
        ps::InsertReverb verb;
        verb.prepare(kRate);
        std::vector<float> l(kRate, 0.0f), r(kRate, 0.0f);
        l[100] = r[100] = 1.0f;
        verb.process(l.data(), r.data(), kRate);
        double early = 0, late = 0;
        for (size_t i = 1000; i < size_t(kRate); ++i) (i < 12000 ? early : late) += l[i] * l[i];
        CHECK(early > 1e-4 && late > 1e-6 && late / (kRate - 12000) < early / 11000);
        verb.setParam(3, 0.0f);
        verb.reset();
        std::fill(l.begin(), l.end(), 0.0f);
        std::fill(r.begin(), r.end(), 0.0f);
        l[100] = r[100] = 1.0f;
        verb.process(l.data(), r.data(), kRate);
        float rest = 0;
        for (size_t i = 200; i < l.size(); ++i) rest = std::max(rest, std::fabs(l[i]));
        CHECK(rest < 1e-6f);
    }
    // 5. Auto-pan at a quarter note, full depth, square: left in the first half of each beat, right
    // in the second; a beat puts it back on the grid.
    {
        ps::AutoPan pan;
        pan.prepare(kRate);
        pan.setStepFrames(6000.0);  // a beat = 24000 frames
        pan.setParam(0, 0.55f);     // 1/4
        pan.setParam(1, 1.0f);
        pan.setParam(2, 1.0f);
        CHECK(ps::AutoPan::stepsOf(0.55f) == 4);
        std::vector<float> l(256), r(256);
        double lFirst = 0, rFirst = 0, lSecond = 0, rSecond = 0;
        for (int at = 0; at < 48000; at += 256) {
            if (at == 0) pan.beat(0);
            std::fill(l.begin(), l.end(), 0.5f);
            std::fill(r.begin(), r.end(), 0.5f);
            pan.process(l.data(), r.data(), 256);
            const int inBeat = at % 24000;
            if (inBeat > 2000 && inBeat < 10000) { lFirst += l[128]; rFirst += r[128]; }
            if (inBeat > 14000 && inBeat < 22000) { lSecond += l[128]; rSecond += r[128]; }
        }
        // sin starts upwards: pan > 0 = right first.
        CHECK(rFirst > 3 * lFirst && lSecond > 3 * rSecond);
        // Off the grid (a tempo change), then a beat: back on it.
        for (int i = 0; i < 37; ++i) pan.process(l.data(), r.data(), 100);
        pan.beat(10);
        pan.process(l.data(), r.data(), 11);
        CHECK(pan.phase() < 2.0 / 24000.0);
    }
    // 6. OTT at full depth: a quiet sound comes up a lot, a loud one does not get louder.
    {
        auto gainDb = [](float amp, float hz) {
            ps::Ott ott;
            ott.prepare(kRate);
            ott.setParam(0, 1.0f);
            std::vector<float> l(kRate), r(kRate);
            for (size_t i = 0; i < l.size(); ++i) l[i] = r[i] = amp * float(std::sin(2 * M_PI * hz * double(i) / kRate));
            ott.process(l.data(), r.data(), kRate);
            return 20.0 * std::log10(toneLevel(l, kRate / 2, kRate, hz) / amp);
        };
        const double quiet = gainDb(0.01f, 1000.0f), loud = gainDb(0.7f, 1000.0f), quietBass = gainDb(0.01f, 60.0f), loudHigh = gainDb(0.7f, 6000.0f);
        std::printf("  OTT: -40 dB %+.1f dB, -3 dB %+.1f dB; bass -40 dB %+.1f dB, highs -3 dB %+.1f dB\n", quiet, loud, quietBass, loudHigh);
        CHECK(quiet > 8.0 && quietBass > 8.0);
        CHECK(loud < -2.0 && loudHigh < -2.0);
        // At depth 0: untouched.
        ps::Ott dry;
        dry.prepare(kRate);
        dry.setParam(0, 0.0f);
        std::vector<float> l(4800), r(4800);
        for (size_t i = 0; i < l.size(); ++i) l[i] = r[i] = 0.3f * float(std::sin(0.01 * double(i)));
        const std::vector<float> before = l;
        dry.process(l.data(), r.data(), 4800);
        CHECK(l == before);
    }
    // 7. Frequency shifter: a 1 kHz tone comes out at 1.2 kHz (+200 Hz), or 800 Hz (-200 Hz).
    {
        for (float sign : {1.0f, -1.0f}) {
            ps::FreqShifter fs;
            fs.prepare(kRate);
            const float v = 0.5f + 0.5f * sign * std::sqrt(200.0f / 2000.0f);
            CHECK(std::fabs(ps::FreqShifter::shiftHz(v) - 200.0f * sign) < 0.5f);
            fs.setParam(0, v);
            fs.setParam(2, 1.0f);  // all wet
            std::vector<float> l(kRate), r(kRate);
            for (size_t i = 0; i < l.size(); ++i) l[i] = r[i] = 0.5f * float(std::sin(2 * M_PI * 1000.0 * double(i) / kRate));
            fs.process(l.data(), r.data(), kRate);
            const double target = toneLevel(l, kRate / 2, kRate, 1000.0 + 200.0 * sign);
            const double original = toneLevel(l, kRate / 2, kRate, 1000.0), mirror = toneLevel(l, kRate / 2, kRate, 1000.0 - 200.0 * sign);
            std::printf("  shifter %+.0f Hz: target %.3f, original %.4f, mirror %.4f\n", 200.0 * sign, target, original, mirror);
            CHECK(target > 0.4 && original < 0.02 && mirror < 0.02);
        }
    }
    // 8. Tape stop: the knob past its middle slows the sound to silence in the stop time; back
    // under it, the sound returns.
    {
        ps::TapeStop tape;
        tape.prepare(kRate);
        tape.setParam(1, 0.0f);  // 0.1 s
        tape.setParam(2, 0.0f);  // no fade back
        std::vector<float> l(480), r(480);
        auto block = [&](double& t) {
            for (size_t i = 0; i < l.size(); ++i, t += 1.0) l[i] = r[i] = 0.5f * float(std::sin(2 * M_PI * 440.0 * t / kRate));
            tape.process(l.data(), r.data(), int(l.size()));
            float peak = 0;
            for (float v : l) peak = std::max(peak, std::fabs(v));
            return peak;
        };
        double t = 0;
        for (int b = 0; b < 10; ++b) CHECK(block(t) > 0.45f);  // playing: untouched
        tape.setParam(0, 1.0f);
        float during = block(t);
        CHECK(during > 0.1f && tape.speed() < 0.95f);
        for (int b = 0; b < 12; ++b) block(t);  // > 0.1 s
        CHECK(tape.speed() == 0.0f && block(t) == 0.0f);
        tape.setParam(0, 0.0f);
        block(t);
        CHECK(block(t) > 0.45f);
    }
    // 9. The pool: as many echoes as it holds play (on channels 0..2, all eight slots), one more
    // waits and gets the unit the first one gives back.
    {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        static_assert(ps::LongFxPool::kEchoes < 3 * ps::Mixer::kInserts);
        for (int i = 0; i < ps::LongFxPool::kEchoes; ++i) mixer.setInsertType(i / ps::Mixer::kInserts, i % ps::Mixer::kInserts, ps::InsertSlot::EchoFx);
        CHECK(mixer.longFxPool().echoes.available() == 0);
        mixer.setInsertType(5, 0, ps::InsertSlot::EchoFx);
        CHECK(!mixer.insert(5, 0).hasUnit() && mixer.insert(5, 0).waitingForUnit());
        mixer.setInsertParam(5, 0, 3, 0.9f);  // remembered for when it plays
        mixer.setInsertType(0, 0, ps::InsertSlot::None);
        CHECK(mixer.insert(5, 0).hasUnit() && !mixer.insert(0, 0).hasUnit());
        CHECK(mixer.echoInUse() && mixer.echoFrames() > 0.0f);
        // Another type in place of an echo gives it back; a reverb comes from its own pool.
        mixer.setInsertType(1, 0, ps::InsertSlot::ReverbFx);
        CHECK(mixer.longFxPool().echoes.available() == 1 && mixer.insert(1, 0).hasUnit());
    }
}

void testParametricEq() {
    // +12 dB bell at 1 kHz: 1 kHz up ~12 dB, 100 Hz untouched; low cut at 500 Hz: 50 Hz way down.
    ps::InsertSlot eq;
    eq.prepare(kRate);
    eq.setType(ps::InsertSlot::ParamEqFx);
    eq.setParam(0, std::log(1000.0f / 20.0f) / std::log(1000.0f));  // 1 kHz on the 20 Hz..20 kHz log scale
    eq.setParam(1, 0.5f + 12.0f / 36.0f);
    const float atBell = sineGainDb(eq, 1000.0f), below = sineGainDb(eq, 100.0f);
    std::printf("  param EQ: 1 kHz %+.1f dB, 100 Hz %+.1f dB\n", atBell, below);
    CHECK(std::fabs(atBell - 12.0f) < 0.5f);
    CHECK(std::fabs(below) < 0.5f);
    eq.setParam(3, 1.0f);
    CHECK(sineGainDb(eq, 50.0f) < -15.0f);
}

void testScopes() {
    // The analyser sees the last output frames in order; the tuner the last microphone frames.
    ps::Engine engine(kRate);
    std::vector<float> out(512 * 2), in(512);
    for (int k = 0; k < 10; ++k) {
        for (int i = 0; i < 512; ++i) in[size_t(i)] = float(k * 512 + i);
        engine.process(in.data(), 512, 1, out.data(), 512, 2);
    }
    std::vector<float> mic(ps::Engine::kScopeFrames);
    engine.readScope(true, mic.data());
    CHECK(mic.back() == 5119.0f && mic.front() == 5120.0f - ps::Engine::kScopeFrames);
    engine.post({ps::CommandType::SetFrequency, 440.0f});
    engine.post({ps::CommandType::SetGain, 0.5f});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    render(engine, kRate / 2, 256);
    std::vector<float> scope(ps::Engine::kScopeFrames);
    engine.readScope(false, scope.data());
    float peak = 0.0f;
    for (float v : scope) peak = std::max(peak, std::fabs(v));
    CHECK(peak > 0.2f);
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
    CHECK(std::fabs(out[(100 + kLate) * 2] - 0.5f) < 1e-3f);
    CHECK(std::fabs(out[(100 + kLate + 12000) * 2]) < 1e-6f);
    // Delay send on the drums (1/8 at 120 BPM): an echo 12000 frames later, on the master.
    mixer.setBusParam(1, 0, 0.2f);
    mixer.setBusParam(1, 2, 0.0f);
    mixer.setSend(0, 1, 1.0f);
    mixer.process(ch, nullptr, out.data(), frames, 2);  // settle the send smoothing
    mixer.process(ch, nullptr, out.data(), frames, 2);
    float echo = 0.0f;
    for (int i = 100 + kLate + 12000 - 2; i <= 100 + kLate + 12000 + 2; ++i) echo = std::max(echo, std::fabs(out[static_cast<size_t>(i) * 2]));
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

// Eight effects per strip: commands and automation reach slots 4..7 (channel x 8 + slot, the
// app's packing), a slot-7 effect is heard, its automated knob moves on the step, and the export
// renders exactly what the live engine plays.
void testEightInsertSlots() {
    static_assert(ps::Mixer::kInserts == 8);
    // 1. Packing: channel 2's slot 7 and the master's slot 7, not channel 3's slot 3 (4-wide packing).
    {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetInsertType, 0.0f, 2 * ps::Mixer::kInserts + 7, ps::InsertSlot::Filter});
        engine.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kMaster * ps::Mixer::kInserts + 7, ps::InsertSlot::Comp});
        engine.post({ps::CommandType::SetInsertParam, 0.25f, 2 * ps::Mixer::kInserts + 7, 0});
        render(engine, 512, 256);
        CHECK(engine.mixer().insert(2, 7).type() == ps::InsertSlot::Filter);
        CHECK(engine.mixer().insert(3, 3).type() == ps::InsertSlot::None && engine.mixer().insert(3, 7).type() == ps::InsertSlot::None);
        CHECK(engine.mixer().insert(ps::Mixer::kMaster, 7).type() == ps::InsertSlot::Comp);
        // Out of range: ignored, nothing else touched.
        engine.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kChannels * ps::Mixer::kInserts, ps::InsertSlot::Filter});
        render(engine, 256, 256);
        CHECK(ps::Automation::pack(ps::Automation::InsertParam, 15, 7, 3) == ((3 << 16) | (15 << 12) | (7 << 8) | 3));
    }
    // 2. A synth pad through all eight slots of its strip: seven gentle effects, then a low-pass
    // on slot 7 whose cutoff a lane closes on step 4 and opens again on step 8.
    // The long effects really heard (echo at a sixteenth and reverb, half wet): their pool units
    // must sound the same live and in the export.
    auto setUp = [](auto& target, bool automate, float wet = 0.5f) {
        target.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
        const int types[7] = {ps::InsertSlot::Eq, ps::InsertSlot::Comp, ps::InsertSlot::Saturation, ps::InsertSlot::Chorus,
                              ps::InsertSlot::ReverbFx, ps::InsertSlot::ParamEqFx, ps::InsertSlot::EchoFx};
        for (int slot = 0; slot < 7; ++slot) target.post({ps::CommandType::SetInsertType, 0.0f, 2 * ps::Mixer::kInserts + slot, types[slot]});
        target.post({ps::CommandType::SetInsertParam, wet, 2 * ps::Mixer::kInserts + 4, 3});  // reverb mix
        target.post({ps::CommandType::SetInsertParam, 0.0f, 2 * ps::Mixer::kInserts + 6, 0});  // echo every sixteenth
        target.post({ps::CommandType::SetInsertParam, wet, 2 * ps::Mixer::kInserts + 6, 3});  // echo mix
        const int a = 2 * ps::Mixer::kInserts + 7;
        target.post({ps::CommandType::SetInsertType, 0.0f, a, ps::InsertSlot::Filter});
        const float filter[4] = {1.0f, 0.0f, 0.0f, 1.0f};  // open low-pass, no resonance, fully wet
        for (int prm = 0; prm < 4; ++prm) target.post({ps::CommandType::SetInsertParam, filter[prm], a, prm});
        if (automate) {
            target.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::InsertParam, 2, 7, 0)});
            target.post({ps::CommandType::SetAutomationPoint, 0.0f, 0, 4});
            target.post({ps::CommandType::SetAutomationPoint, 1.0f, 0, 8});
        }
    };
    const int frames = kRate * 2;  // 16 steps of 6000 frames at 120 BPM
    ps::OfflineRenderer exporter(kRate);
    setUp(exporter, true);
    const auto exported = exporter.renderToBuffer(frames, 0);
    // The live engine, driven the way the export drives its own: the same commands, a tenth of a
    // second to settle, then play (in the export's block size).
    ps::Engine live(kRate);
    setUp(live, true);
    render(live, kRate / 10, ps::OfflineRenderer::kBlock);
    live.mixer().settle();
    live.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto played = render(live, frames, ps::OfflineRenderer::kBlock);
    CHECK(exported.size() == played.size());
    float diff = 0.0f;
    const size_t late = size_t(kLate) * kChannels;  // heard the limiter's look-ahead later
    for (size_t i = 0; i + late < std::min(exported.size(), played.size()); ++i) diff = std::max(diff, std::fabs(exported[i] - played[i + late]));
    CHECK(diff == 0.0f);
    auto rms = [](const std::vector<float>& o, int from) {
        double sum = 0;
        for (int i = from; i < from + 2400 && size_t(i) * 2 < o.size(); ++i) sum += double(o[size_t(i) * 2]) * o[size_t(i) * 2];
        return std::sqrt(sum / 2400);
    };
    const double open = rms(exported, 6000 * 2 + 1000), closed = rms(exported, 6000 * 5 + 1000), back = rms(exported, 6000 * 9 + 1000);
    std::printf("  slot 7 automated: step 2 %.4f, step 5 %.4f, step 9 %.4f (live vs export %.2g)\n", open, closed, back, double(diff));
    CHECK(open > 0.02 && closed < open * 0.2 && back > open * 0.5);  // the reverb and echo tails are filtered too
    // Without the lane, step 5 stays open: the lane on slot 7 is what closed it.
    ps::OfflineRenderer still(kRate);
    setUp(still, false);
    const auto plain = still.renderToBuffer(frames, 0);
    CHECK(rms(plain, 6000 * 5 + 1000) > open * 0.5);
    // The echo and the reverb are really in what was compared: fully dry, the render differs.
    ps::OfflineRenderer dry(kRate);
    setUp(dry, true, 0.0f);
    const auto dryOut = dry.renderToBuffer(frames, 0);
    float wetDiff = 0.0f;
    for (size_t i = 0; i < std::min(dryOut.size(), exported.size()); ++i) wetDiff = std::max(wetDiff, std::fabs(dryOut[i] - exported[i]));
    std::printf("  echo and reverb half wet vs dry: %.3f\n", double(wetDiff));
    CHECK(wetDiff > 0.02f);
}

// The app's long effects (echo, reverb, tape stop) and the pool they borrow from.
void testLongEffectsFollowTheApp() {
    static_assert(static_cast<int>(ps::CommandType::SetMaster) == 67 && static_cast<int>(ps::CommandType::ClearInserts) == 68,
                  "NativeEngine.kt's CMD_MASTER and CMD_CLEAR_INSERTS");
    // 1. A strip the app no longer uses (track deleted, undone, another project) is emptied in one
    // command: its units go back, and an echo placed elsewhere afterwards is heard.
    {
        ps::Engine engine(kRate);
        for (int i = 0; i < ps::LongFxPool::kEchoes; ++i) {
            const int ch = ps::Mixer::kFirstPad + i / ps::Mixer::kInserts;
            engine.post({ps::CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts + i % ps::Mixer::kInserts, ps::InsertSlot::EchoFx});
        }
        engine.post({ps::CommandType::SetInsertType, 0.0f, (ps::Mixer::kFirstPad + 2) * ps::Mixer::kInserts, ps::InsertSlot::EchoFx});
        render(engine, 256, 256);
        CHECK(engine.mixer().longFxPool().echoes.available() == 0);
        CHECK(engine.mixer().insert(ps::Mixer::kFirstPad + 2, 0).waitingForUnit());
        engine.post({ps::CommandType::ClearInserts, 0.0f, ps::Mixer::kFirstPad});
        engine.post({ps::CommandType::ClearInserts, 0.0f, ps::Mixer::kFirstPad + 1});
        engine.post({ps::CommandType::ClearInserts, 0.0f, ps::Mixer::kFirstPad + 2});
        engine.post({ps::CommandType::ClearInserts, 0.0f, ps::Mixer::kChannels});  // out of range: ignored
        render(engine, 256, 256);
        CHECK(engine.mixer().longFxPool().echoes.available() == ps::LongFxPool::kEchoes);
        for (int ch = ps::Mixer::kFirstPad; ch < ps::Mixer::kFirstPad + 3; ++ch) {
            for (int slot = 0; slot < ps::Mixer::kInserts; ++slot) CHECK(engine.mixer().insert(ch, slot).type() == ps::InsertSlot::None);
        }
        engine.post({ps::CommandType::SetInsertType, 0.0f, 2 * ps::Mixer::kInserts, ps::InsertSlot::EchoFx});
        render(engine, 256, 256);
        CHECK(engine.mixer().insert(2, 0).hasUnit());
    }
    // 2. The first repeat of a new echo is on time, without a glide from the defaults (3/16) to
    // its real time (1/16 = 6000 frames at 120 BPM): live, the type and its settings arrive in
    // the same callback.
    {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setStepFrames(6000.0);
        mixer.setInsertType(2, 0, ps::InsertSlot::EchoFx);
        mixer.setInsertParam(2, 0, 0, 0.0f);  // 1/16
        mixer.setInsertParam(2, 0, 1, 0.0f);  // a single repeat
        mixer.setInsertParam(2, 0, 3, 1.0f);  // wet only
        auto& slot = const_cast<ps::InsertSlot&>(mixer.insert(2, 0));
        std::vector<float> l(kRate, 0.0f), r(kRate, 0.0f);
        l[0] = r[0] = 1.0f;
        for (int at = 0; at < kRate; at += 192) {
            mixer.setStepFrames(6000.0);
            slot.process(l.data() + at, r.data() + at, std::min(192, kRate - at));
        }
        int peakAt = 0;
        for (int i = 1; i < kRate; ++i) {
            if (std::fabs(l[size_t(i)]) > std::fabs(l[size_t(peakAt)])) peakAt = i;
        }
        std::printf("  new echo: first repeat at frame %d (6000 expected), %.3f\n", peakAt, double(l[size_t(peakAt)]));
        CHECK(peakAt == 6000);
        CHECK(std::fabs(l[size_t(peakAt)]) > 0.9f);
    }
    // ... and in the export (whose pre-roll used to glide it): a kick at step 0, echoed wet only.
    {
        ps::OfflineRenderer exporter(kRate);
        exporter.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
        exporter.post({ps::CommandType::SetInsertType, 0.0f, ps::Mixer::kDrums * ps::Mixer::kInserts, ps::InsertSlot::EchoFx});
        exporter.post({ps::CommandType::SetInsertParam, 0.0f, ps::Mixer::kDrums * ps::Mixer::kInserts, 0});
        exporter.post({ps::CommandType::SetInsertParam, 0.0f, ps::Mixer::kDrums * ps::Mixer::kInserts, 1});
        exporter.post({ps::CommandType::SetInsertParam, 1.0f, ps::Mixer::kDrums * ps::Mixer::kInserts, 3});
        const auto out = exporter.renderToBuffer(9000, 0);
        int first = -1;
        for (size_t i = 0; i * 2 < out.size(); ++i) {
            if (std::fabs(out[i * 2]) > 1e-3f) {
                first = static_cast<int>(i);
                break;
            }
        }
        std::printf("  exported echo: first sound at frame %d (6000 expected)\n", first);
        CHECK(first >= 6000 && first <= 6004);
    }
    // 3. A unit given back and borrowed again starts silent (its reset clears nothing: what the
    // last user left there is never heard), echo and tape stop alike.
    {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setStepFrames(6000.0);
        for (int i = 0; i < ps::LongFxPool::kEchoes; ++i) {
            mixer.setInsertType(i / ps::Mixer::kInserts, i % ps::Mixer::kInserts, ps::InsertSlot::EchoFx);
            mixer.setInsertParam(i / ps::Mixer::kInserts, i % ps::Mixer::kInserts, 0, 1.0f);  // half notes
        }
        for (int i = 0; i < ps::LongFxPool::kTapeStops; ++i) mixer.setInsertType(3, i, ps::InsertSlot::TapeStopFx);
        std::vector<float> l(kRate), r(kRate);
        auto fill = [&](float level) {
            for (int i = 0; i < kRate; ++i) l[size_t(i)] = r[size_t(i)] = level * float(std::sin(2 * M_PI * 220.0 * i / kRate));
        };
        for (int i = 0; i < ps::LongFxPool::kEchoes; ++i) {
            fill(0.8f);
            const_cast<ps::InsertSlot&>(mixer.insert(i / ps::Mixer::kInserts, i % ps::Mixer::kInserts)).process(l.data(), r.data(), kRate);
        }
        for (int i = 0; i < ps::LongFxPool::kTapeStops; ++i) {
            fill(0.8f);
            const_cast<ps::InsertSlot&>(mixer.insert(3, i)).process(l.data(), r.data(), kRate);
        }
        for (int ch = 0; ch < 4; ++ch) mixer.clearInserts(ch);
        CHECK(mixer.longFxPool().echoes.available() == ps::LongFxPool::kEchoes);
        CHECK(mixer.longFxPool().tapeStops.available() == ps::LongFxPool::kTapeStops);
        float stale = 0.0f;
        for (int i = 0; i < ps::LongFxPool::kEchoes; ++i) {
            mixer.setInsertType(5 + i / ps::Mixer::kInserts, i % ps::Mixer::kInserts, ps::InsertSlot::EchoFx);
            mixer.setInsertParam(5 + i / ps::Mixer::kInserts, i % ps::Mixer::kInserts, 0, 1.0f);
            mixer.setInsertParam(5 + i / ps::Mixer::kInserts, i % ps::Mixer::kInserts, 3, 1.0f);
            fill(0.0f);
            const_cast<ps::InsertSlot&>(mixer.insert(5 + i / ps::Mixer::kInserts, i % ps::Mixer::kInserts)).process(l.data(), r.data(), kRate);
            for (int k = 0; k < kRate; ++k) stale = std::max(stale, std::max(std::fabs(l[size_t(k)]), std::fabs(r[size_t(k)])));
        }
        for (int i = 0; i < ps::LongFxPool::kTapeStops; ++i) {
            mixer.setInsertType(7, i, ps::InsertSlot::TapeStopFx);
            mixer.setInsertParam(7, i, 0, 1.0f);  // stopping at once: it plays back what it holds
            fill(0.0f);
            const_cast<ps::InsertSlot&>(mixer.insert(7, i)).process(l.data(), r.data(), kRate);
            for (int k = 0; k < kRate; ++k) stale = std::max(stale, std::max(std::fabs(l[size_t(k)]), std::fabs(r[size_t(k)])));
        }
        std::printf("  borrowed units: %.3g left from their last user\n", double(stale));
        CHECK(stale == 0.0f);
    }
}

// The worst case of eight effects everywhere: eight of the heaviest effects (long ones included:
// beyond the pool's units they pass the sound through) on every strip of a full project (the 15
// channels and the master), then on every strip there is (the 16 pads' own strips too). No
// allocation; the time per 192-frame block is noted. A phone is several times slower than this
// machine: past what it can take, the app warns (Engine::cpuLoad) rather than refusing effects.
void testEightInsertsCpuWorstCase() {
    const int heavy[] = {ps::InsertSlot::PhaserFx, ps::InsertSlot::ReverbFx, ps::InsertSlot::VinylFx, ps::InsertSlot::TremoloFx,
                         ps::InsertSlot::EchoFx, ps::InsertSlot::AutoPanFx, ps::InsertSlot::RadioFx, ps::InsertSlot::FreqShiftFx,
                         ps::InsertSlot::OttFx, ps::InsertSlot::RobotFx, ps::InsertSlot::FlangerFx, ps::InsertSlot::PingPongFx};
    constexpr int kHeavy = sizeof(heavy) / sizeof(heavy[0]);
    constexpr int kBlock = 192;  // a usual phone callback (4 ms at 48 kHz)
    auto loadWith = [&](int strips) {
        ps::Mixer mixer;
        mixer.prepare(kRate);
        mixer.setStepFrames(6000.0);
        std::vector<std::vector<float>> inputs(ps::Mixer::kChannels, std::vector<float>(kBlock));
        std::vector<const float*> channels(ps::Mixer::kChannels, nullptr);
        for (int ch = 0; ch < strips; ++ch) {
            for (int slot = 0; slot < ps::Mixer::kInserts; ++slot) {
                mixer.setInsertType(ch, slot, heavy[(ch * 5 + slot) % kHeavy]);
                for (int p = 0; p < 4; ++p) mixer.setInsertParam(ch, slot, p, 0.6f);
            }
            if (ch == ps::Mixer::kMaster) continue;
            mixer.setVolume(ch, 0.1f);
            for (int i = 0; i < kBlock; ++i) inputs[size_t(ch)][size_t(i)] = 0.2f * float(std::sin(2 * M_PI * (110.0 + 37.0 * ch) * i / kRate));
            channels[size_t(ch)] = inputs[size_t(ch)].data();
        }
        std::vector<float> out(kBlock * 2);
        const int blocks = kRate * 2 / kBlock;
        for (int b = 0; b < 50; ++b) mixer.process(channels.data(), nullptr, out.data(), kBlock, 2);  // warm-up
        double best = 1e9;
        bool finite = true;
#if defined(PS_COUNT_ALLOCATIONS)
        g_allocations = 0;
        g_countAllocations = true;
#endif
        // Best of three passes: a busy machine only ever makes a pass slower.
        for (int pass = 0; pass < 3; ++pass) {
            const double start = threadSeconds();
            for (int b = 0; b < blocks; ++b) {
                if (b % 100 == 0) mixer.sidechainEvent(0, true, true);
                mixer.process(channels.data(), nullptr, out.data(), kBlock, 2);
                for (float v : out) finite &= std::isfinite(v);
            }
            best = std::min(best, (threadSeconds() - start) / 2.0);
        }
#if defined(PS_COUNT_ALLOCATIONS)
        g_countAllocations = false;
        CHECK(g_allocations == 0);
#endif
        CHECK(finite);
        std::printf("  %d strips x %d heavy effects: %.1f %% of real time on this machine, %.0f us per %d-frame block (%.0f us)\n",
                    strips, ps::Mixer::kInserts, best * 100.0, best * kBlock * 1e6 / kRate, kBlock, kBlock * 1e6 / kRate);
        return best;
    };
    const double project = loadWith(ps::Mixer::kFirstPad), everything = loadWith(ps::Mixer::kChannels);
#if defined(__SANITIZE_ADDRESS__)
    (void)project;
    (void)everything;  // sanitizer builds are several times slower: timing is meaningless there
#else
    CHECK_TIMING(project < 0.75);     // a full project, every slot taken by a heavy effect
    CHECK_TIMING(everything < 1.5);   // every strip: the cost grows in step with the effects, nothing worse
#endif
    // The engine reports its load (the app's warning): far more with heavy effects than without.
    auto loadOf = [&](bool effects) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
        for (int ch : {0, 1, 2, 3, ps::Mixer::kMaster}) {
            for (int slot = 0; effects && slot < ps::Mixer::kInserts; ++slot) {
                engine.post({ps::CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts + slot, heavy[(ch * 5 + slot) % kHeavy]});
            }
        }
        render(engine, kRate, kBlock);
        return engine.cpuLoad();
    };
    const float idle = loadOf(false), loaded = loadOf(true);
    std::printf("  engine load: %.1f %% without effects, %.1f %% with 40 heavy ones\n", idle * 100.0, loaded * 100.0);
    CHECK(idle > 0.0f);
    CHECK_TIMING(idle < 0.5f);
    CHECK_TIMING(loaded > idle * 1.5f);  // the engine's own clock is wall time
}

void testAutomationClips() {
    // A song-long lane on the synth fader: silent on bar 1, rising to full on bar 2, then held.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
    engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
    engine.post({ps::CommandType::SetAutomationMode, 2.0f, 0, ps::Automation::Song});
    engine.post({ps::CommandType::SetAutomationSong, 0.0f, 0, 0});
    engine.post({ps::CommandType::SetAutomationSong, 0.8f, 0, 1});
    render(engine, kRate / 2, 256);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto out = render(engine, kRate * 6, 256);  // 3 bars
    auto rms = [&](const std::vector<float>& o, int from) {
        double sum = 0;
        for (int i = from; i < from + 2400; ++i) sum += double(o[size_t(i) * 2]) * o[size_t(i) * 2];
        return std::sqrt(sum / 2400);
    };
    const double start = rms(out, 6000 * 1), middle = rms(out, 6000 * 9), later = rms(out, 6000 * 40);
    std::printf("  song automation: %.4f, %.4f, %.4f\n", start, middle, later);
    CHECK(start < later * 0.05 && middle > later * 0.15 && middle < later * 0.9);
    // The song loops: a swap back to its first part restarts the curve.
    {
        ps::Engine loop(kRate);
        loop.post({ps::CommandType::SetTransport, 1.0f, 0});
        render(loop, 6000 * 20, 256);
        CHECK(loop.songStep() >= 19);
        loop.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 1});  // next bar: the song starts over
        render(loop, 6000 * 13, 256);                             // past step 32
        CHECK(loop.songStep() >= 0 && loop.songStep() < 6);
    }
    // A per-scene lane: the waiting scene brings its own values at the swap.
    ps::Engine scenes(kRate);
    scenes.post({ps::CommandType::SynthNoteOn, 0.8f, 57});
    scenes.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
    scenes.post({ps::CommandType::SetAutomationMode, 0.0f, 0, ps::Automation::PerScene});
    scenes.post({ps::CommandType::SetAutomationPoint, 0.0f, 0, 0});  // scene A: silent
    scenes.post({ps::CommandType::SetTransport, 1.0f, 0});
    scenes.post({ps::CommandType::SetStaging, 1.0f});
    scenes.post({ps::CommandType::SetAutomationPoint, 0.8f, 0, 0});  // scene B: full
    scenes.post({ps::CommandType::SetStaging, 0.0f});
    scenes.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    out = render(scenes, kRate * 2 + 12000, 256);  // bar 1 and two steps of bar 2
    const double a = rms(out, 6000 * 6), b = rms(out, 6000 * 16 + 4000);
    CHECK(scenes.sceneSwaps() == 1);
    CHECK(a < b * 0.05 && b > 0.01);
}

double pitchHz(const std::vector<float>& x, int from, int len);
double toneAmplitude(const std::vector<float>& x, int from, int len, double hz);

void testExpressionAndArpeggio() {
    // Pitch bend: +12 semitones doubles the pitch; the mod wheel is accepted (vibrato).
    {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::Lfo1ToPitch});
        engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::SubLevel});  // one clear pitch to measure
        engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::OscMix});
        engine.post({ps::CommandType::SynthNoteOn, 0.9f, 57});
        auto out = render(engine, kRate / 2, 256);
        std::vector<float> mono(out.size() / 2);
        for (size_t i = 0; i < mono.size(); ++i) mono[i] = out[i * 2];
        const double before = pitchHz(mono, kRate / 4, 4800);
        engine.post({ps::CommandType::SetExpression, 12.0f, ps::InstrumentTrack::Bend});
        out = render(engine, kRate / 2, 256);
        for (size_t i = 0; i < mono.size(); ++i) mono[i] = out[i * 2];
        const double after = pitchHz(mono, kRate / 4, 4800);
        std::printf("  bend: %.1f Hz -> %.1f Hz\n", before, after);
        CHECK(std::fabs(before - 220.0) < 2.0 && std::fabs(after - 440.0) < 4.0);
    }
    // Sustain pedal: a released key keeps sounding until the pedal comes up.
    {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::Sustain});
        engine.post({ps::CommandType::SynthNoteOn, 0.9f, 60});
        render(engine, 4800, 256);
        engine.post({ps::CommandType::SynthNoteOff, 0.0f, 60});
        render(engine, kRate, 256);
        CHECK(engine.synthActiveVoices() == 1);
        engine.post({ps::CommandType::SetExpression, 0.0f, ps::InstrumentTrack::Sustain});
        render(engine, kRate * 3, 256);
        CHECK(engine.synthActiveVoices() == 0);
    }
    // Arpeggiator, 1/16 at 120 BPM (6000 frames), up: A3, E4, A4, then A3 again.
    {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::Lfo1ToPitch});
        engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::SubLevel});
        engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::OscMix});
        engine.post({ps::CommandType::SynthParam, 0.0f, ps::Synth::AmpRelease});  // no tail under the next note
        engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::ArpOn});
        engine.post({ps::CommandType::SetExpression, 0.9f, ps::InstrumentTrack::ArpGate});
        for (int n : {64, 57, 69}) engine.post({ps::CommandType::SynthNoteOn, 0.9f, n});
        auto out = render(engine, 6000 * 4, 100);
        std::vector<float> mono(out.size() / 2);
        for (size_t i = 0; i < mono.size(); ++i) mono[i] = out[i * 2];
        const double expected[4] = {220.0, 329.63, 440.0, 220.0};
        for (int k = 0; k < 4; ++k) {
            const double f = pitchHz(mono, k * 6000 + 500, 3000);
            if (std::fabs(f - expected[k]) > expected[k] * 0.02) std::printf("  arp note %d: %.1f Hz\n", k, f);
            CHECK(std::fabs(f - expected[k]) < expected[k] * 0.02);
        }
        // A key held since before the arpeggiator came on is released normally (no stuck note).
        engine.post({ps::CommandType::SetExpression, 0.0f, ps::InstrumentTrack::ArpOn});
        render(engine, 256, 256);
        engine.post({ps::CommandType::SynthNoteOn, 0.9f, 72});
        engine.post({ps::CommandType::SetExpression, 1.0f, ps::InstrumentTrack::ArpOn});
        render(engine, 4800, 256);
        engine.post({ps::CommandType::SynthNoteOff, 0.0f, 72});
        render(engine, kRate, 256);
        CHECK(engine.synthActiveVoices() == 0);
        // Keys up: the arpeggio stops.
        for (int n : {64, 57, 69}) engine.post({ps::CommandType::SynthNoteOff, 0.0f, n});
        render(engine, kRate * 2, 256);
        CHECK(engine.synthActiveVoices() == 0);
    }
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

void testSongEndsOnTheBar() {
    ps::Engine engine(kRate);
    // The last part: pad 1 on every beat, the metronome on, a held piano-roll note. The song
    // ends after two bars: the transport stops exactly on that bar line, before its step plays.
    for (int st = 0; st < 16; st += 4) engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
    engine.post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 16, 60)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
    engine.post({ps::CommandType::SetMetronome, 1.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 32, 2});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    constexpr int kBar = kRate * 2;  // 120 BPM, 4/4
    render(engine, 2 * kBar, 256);   // up to the last frame before the end
    CHECK(engine.transportRunning());
    CHECK(engine.laneTriggerCount(0) == 8 && engine.metronomeClicks() == 8 && engine.sceneSwaps() == 0);
    render(engine, 1, 1);  // the bar line itself
    std::printf("  song end: running %d, pad1 %u, clicks %llu, swaps %u\n", engine.transportRunning() ? 1 : 0,
                engine.laneTriggerCount(0), static_cast<unsigned long long>(engine.metronomeClicks()), engine.sceneSwaps());
    CHECK(!engine.transportRunning());
    CHECK(engine.sceneSwaps() == 1);                 // the app sees it as the swap it armed
    CHECK(engine.laneTriggerCount(0) == 8);          // no kick on the bar line
    CHECK(engine.metronomeClicks() == 8);            // nor a click
    CHECK(engine.currentStep() == -1 && engine.songStep() == -1);
    // What still rings fades out: no NaN, no clipping, and silence after a while.
    const auto tail = render(engine, kRate * 2, 256);
    float peakEnd = 0.0f;
    for (size_t i = 0; i < tail.size(); ++i) {
        CHECK(std::isfinite(tail[i]) && std::fabs(tail[i]) <= 1.0f);
        if (i >= tail.size() - kRate / 2 * kChannels) peakEnd = std::max(peakEnd, std::fabs(tail[i]));
    }
    CHECK(peakEnd < 1e-3f);
    CHECK(engine.laneTriggerCount(0) == 8 && engine.synthActiveVoices() == 0);
    // Playing again starts clean: the end armed before is gone.
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 3 * kBar, 256);
    CHECK(engine.transportRunning() && engine.laneTriggerCount(0) == 8 + 12);
    // An end armed "at the next bar" in the middle of one waits for that bar line; a plain
    // swap armed afterwards replaces it (the song goes on).
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 2});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, -1});
    render(engine, 2 * kBar, 256);
    CHECK(engine.transportRunning());
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 2});
    render(engine, kBar + 256, 256);
    CHECK(!engine.transportRunning() && engine.sceneSwaps() == 2);
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

void testPerformanceFilterFadesInAndOut() {
    // A low tone through the live pad's filter, pressed on the high-pass side (the tone nearly
    // vanishes), slid across to the low-pass side and let go, as the engine applies UI commands
    // (between blocks): no step much bigger than the tone's own, then the dry sound untouched.
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    constexpr float amp = 0.5f, hz = 220.0f;
    std::vector<float> l(256), r(256);
    int64_t frame = 0;
    float prev = 0.0f, worst = 0.0f, dryError = 0.0f;
    auto run = [&](int blocks) {
        for (int b = 0; b < blocks; ++b) {
            for (int i = 0; i < 256; ++i) l[size_t(i)] = r[size_t(i)] = amp * float(std::sin(2 * M_PI * hz * double(frame + i) / kRate));
            const std::vector<float> dry = l;
            fx.process(l.data(), r.data(), 256);
            dryError = 0.0f;
            for (int i = 0; i < 256; ++i) {
                worst = std::max(worst, std::fabs(l[size_t(i)] - prev));
                prev = l[size_t(i)];
                dryError = std::max(dryError, std::fabs(l[size_t(i)] - dry[size_t(i)]));
            }
            frame += 256;
        }
    };
    run(20);
    fx.setParam(ps::PerformanceFx::Resonance, 0.5f);
    fx.setParam(ps::PerformanceFx::Filter, 0.9f);  // high-pass around 2 kHz
    run(40);
    float filtered = 0.0f;
    for (float v : l) filtered = std::max(filtered, std::fabs(v));
    CHECK(filtered < 0.25f * amp);  // it really filters
    fx.setParam(ps::PerformanceFx::Filter, 0.1f);  // straight across to the low-pass side
    run(40);
    fx.setParam(ps::PerformanceFx::Filter, 0.5f);  // let go
    fx.setParam(ps::PerformanceFx::Resonance, 0.3f);
    run(40);
    const float natural = amp * naturalStep(hz);
    std::printf("  live filter: largest step %.4f (the tone's own %.4f)\n", worst, natural);
    CHECK(worst < 3.0f * natural);
    CHECK(dryError < 1e-6f);
    CHECK(!fx.active());
}

// Amplitude of the [hz] component of x[from, from + len) (one DFT bin).
double toneAmplitude(const std::vector<float>& x, int from, int len, double hz) {
    double re = 0.0, im = 0.0;
    for (int i = 0; i < len; ++i) {
        const double w = 2 * M_PI * hz * i / kRate;
        re += x[size_t(from + i)] * std::cos(w);
        im += x[size_t(from + i)] * std::sin(w);
    }
    return 2.0 * std::sqrt(re * re + im * im) / len;
}

// A four-note chord held, then five quick notes while it rings (or not): the instrument's output.
template <typename Instrument>
std::vector<float> chordAndQuickNotes(Instrument& inst, bool quick) {
    std::vector<float> out(size_t(kRate) * 3 / 2, 0.0f);
    int at = 0;
    auto renderFor = [&](int frames) {
        for (int done = 0; done < frames;) {
            const int n = std::min(256, frames - done);
            inst.render(out.data() + at, n);
            at += n;
            done += n;
        }
    };
    for (int note : {45, 48, 52, 55}) inst.noteOn(note, 0.8f, 0);
    renderFor(kRate / 4);
    for (int k = 0; k < 5; ++k) {
        if (quick) inst.noteOn(84 + 2 * k, 0.8f, 0);
        renderFor(kRate / 40);
        if (quick) inst.noteOff(84 + 2 * k, 0);
        renderFor(kRate / 40);
    }
    renderFor(int(out.size()) - at);
    return out;
}

// Each held chord note keeps its level although quick notes, released but still ringing (long
// release), had to take voices.
template <typename Instrument, typename Setup>
void checkChordSurvivesQuickNotes(const char* name, Setup setup) {
    Instrument alone, busy;
    setup(alone);
    setup(busy);
    const auto a = chordAndQuickNotes(alone, false);
    const auto b = chordAndQuickNotes(busy, true);
    double worst = 1e9;
    for (int note : {45, 48, 52, 55}) {
        const double hz = 440.0 * std::pow(2.0, (note - 69) / 12.0);
        const double ref = toneAmplitude(a, kRate, kRate / 2, hz);
        worst = std::min(worst, toneAmplitude(b, kRate, kRate / 2, hz) / std::max(ref, 1e-9));
    }
    std::printf("  %s: weakest chord note at %.0f %% of its level\n", name, worst * 100);
    CHECK(worst > 0.8);
}

void testVoiceStealingSparesHeldNotes() {
    checkChordSurvivesQuickNotes<ps::Synth>("synth", [](ps::Synth& s) {
        s.prepare(kRate);
        openFilter(s);
        s.setParam(ps::Synth::AmpSustain, 1.0f);
        s.setParam(ps::Synth::AmpRelease, 1.0f);  // 5 s
    });
    checkChordSurvivesQuickNotes<ps::PlaitsSynth>("plaits", [](ps::PlaitsSynth& p) {
        p.prepare(kRate);
        p.setParam(ps::PlaitsSynth::Model, 0.0f);
        p.setParam(ps::PlaitsSynth::Release, 1.0f);  // 2 s
    });
    std::vector<float> wave(static_cast<size_t>(kRate));
    for (size_t i = 0; i < wave.size(); ++i) wave[i] = 0.5f * float(std::sin(2.0 * M_PI * 440.0 * double(i) / kRate));
    checkChordSurvivesQuickNotes<ps::SampleInstrument>("sampled", [&](ps::SampleInstrument& s) {
        s.prepare(kRate);
        s.install(ps::Sample::create(wave.data(), kRate, 1, kRate, kRate));
        s.setParam(ps::SampleInstrument::Root, 69.0f);
        s.setParam(ps::SampleInstrument::Loop, 1.0f);
        s.setParam(ps::SampleInstrument::Release, 4.0f);
    });
}

void testNoteTrackKeepsNoteOffs() {
    // One long note, then 300 long notes on the next step: more than the queue holds. Whatever is
    // dropped, every note that started also ends.
    struct Sink {
        int on = 0, off = 0;
        void noteOn(int, float, int) { ++on; }
        void noteOff(int, int) { ++off; }
    } sink;
    ps::NoteTrack track;
    track.setNote(0, ps::NoteTrack::pack(0, 2000, 30), 0.8f);
    for (int i = 1; i <= 300; ++i) track.setNote(i, ps::NoteTrack::pack(24 + i % 24, 2000, i % 128), 0.8f);
    track.setCount(301);
    track.onStep(0, 0, 6000.0);
    track.advance(6000, sink);
    track.onStep(0, 1, 6000.0);
    for (int k = 0; k < 200; ++k) track.advance(6000, sink);
    std::printf("  overflowing note queue: %d on, %d off\n", sink.on, sink.off);
    CHECK(sink.on > 100);
    CHECK(sink.on == sink.off);
    CHECK(track.pendingCount() == 0);
}

// Counts the note-ons that would glide (another note still held when it starts), with the events
// of each block sorted by frame like the synth's queue.
struct GlideCounter {
    struct Event { int at; bool on; int pitch; };
    std::vector<Event> block;
    std::vector<int> held;
    int glides = 0, attacks = 0;
    void noteOn(int pitch, float, int at) { block.push_back({at, true, pitch}); }
    void noteOff(int pitch, int at) { block.push_back({at, false, pitch}); }
    void flush() {
        std::stable_sort(block.begin(), block.end(), [](const Event& a, const Event& b) { return a.at < b.at; });
        for (const Event& e : block) {
            if (!e.on) { held.erase(std::remove(held.begin(), held.end(), e.pitch), held.end()); continue; }
            if (held.empty()) ++attacks; else ++glides;
            held.push_back(e.pitch);
        }
        block.clear();
    }
};

GlideCounter playNotes(const std::vector<std::array<int, 3>>& notes, double bpm, double swing, int steps, int bars) {
    ps::Sequencer seq;
    seq.prepare(kRate);
    seq.setTempo(bpm);
    seq.setSwing(swing);
    ps::NoteTrack track;
    track.setSteps(steps);
    for (size_t i = 0; i < notes.size(); ++i) track.setNote(int(i), ps::NoteTrack::pack(notes[i][0], notes[i][1], notes[i][2]), 0.8f);
    track.setCount(int(notes.size()));
    GlideCounter sink;
    seq.start(0);
    const int frames = int(seq.samplesPerStep() * steps * bars) - 512;  // ends before the next loop starts
    for (int pos = 0; pos < frames; pos += 256) {
        seq.advance(256, [&](int offset, int step) { track.onStep(offset, step, seq.samplesPerStep(), seq.swing()); });
        track.advance(256, sink);
        sink.flush();
    }
    return sink;
}

void testTouchingNotesNeverGlide() {
    // A bass line of one-step notes that only touch, changing pitch every step.
    std::vector<std::array<int, 3>> line;
    for (int s = 0; s < 16; ++s) line.push_back({s * 24, 24, s % 2 == 0 ? 36 : 43});
    for (double swing : {0.0, 0.25, 0.6}) {
        for (double bpm : {130.0, 134.0, 97.0}) {
            const auto g = playNotes(line, bpm, swing, 16, 8);
            if (g.glides != 0) std::printf("  touching notes glide: swing %.2f, %.0f BPM: %d glides\n", swing, bpm, g.glides);
            CHECK(g.glides == 0);
            CHECK(g.attacks == 16 * 8);
        }
    }
    // Phonk at 130 BPM, "R-5" (steps 10 to 12): the frame rounding never makes it slide.
    const auto phonk = playNotes({{240, 48, 36}, {288, 24, 41}}, 130.0, 0.0, 16, 64);
    CHECK(phonk.glides == 0);
    // A written slide ('/': the note lasts half a step into the next one) still glides, swung or not.
    for (double swing : {0.0, 0.25}) {
        const auto slide = playNotes({{0, 24 + 12, 36}, {24, 24, 43}, {96, 24, 36}, {120, 24, 41}}, 134.0, swing, 16, 4);
        std::printf("  written slide, swing %.2f: %d glides, %d attacks\n", swing, slide.glides, slide.attacks);
        CHECK(slide.glides == 4);
        CHECK(slide.attacks == 12);
    }
}

void testTempoIgnoresNonFiniteValues() {
    ps::Sequencer seq;
    seq.prepare(kRate);
    seq.setTempo(120.0);
    seq.setTempo(std::nan(""));
    seq.setSwing(0.2);
    seq.setSwing(std::nan(""));
    CHECK(seq.tempo() == 120.0);
    // Through the engine: the transport keeps stepping at the last good tempo.
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTempo, 150.0f});
    engine.post({ps::CommandType::SetTempo, std::nanf("")});
    engine.post({ps::CommandType::SetSwing, std::nanf("")});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    const auto out = render(engine, kRate - 100, 256);
    bool finite = true;
    for (float v : out) finite &= std::isfinite(v);
    CHECK(finite);
    CHECK(engine.currentStep() == 9);  // 10 sixteenths a second at 150 BPM
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
    {
        // At a 44.1 kHz stream too (the tuning follows the rate given to prepare()).
        ps::PlaitsSynth slow;
        slow.prepare(44100);
        slow.setParam(ps::PlaitsSynth::Model, 0.0f);
        slow.setParam(ps::PlaitsSynth::Harmonics, 0.5f);
        slow.noteOn(69, 1.0f, 0);
        std::vector<float> o(44100, 0.0f);
        slow.render(o.data(), 44100);
        // The strongest frequency near A4, read as 48 kHz samples (then scaled back).
        double f44 = 0.0, best = -1.0;
        for (double hz = 460.0; hz < 500.0; hz += 0.05) {
            const double a = toneAmplitude(o, 22050, 22050, hz);
            if (a > best) { best = a; f44 = hz * 44100.0 / kRate; }
        }
        std::printf("  plaits A4 at 44.1 kHz: %.2f Hz\n", f44);
        CHECK(std::fabs(f44 - 440.0) < 1.0);
    }
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
    // A very short note (on and off 4 samples apart, inside one 12-sample block) is still heard.
    plaits.noteOn(69, 1.0f, 2);
    plaits.noteOff(69, 6);
    std::fill(out.begin(), out.end(), 0.0f);
    plaits.render(out.data(), 4800);
    float blip = 0.0f;
    for (int i = 0; i < 4800; ++i) blip = std::max(blip, std::fabs(out[static_cast<size_t>(i)]));
    CHECK(blip > 1e-3f);
    plaits.render(out.data(), kRate);
    CHECK(plaits.activeVoices() == 0);
    // Every model makes sound, stays finite and bounded; six voices at once within a CPU budget.
    for (int model = 0; model < ps::PlaitsSynth::kModels; ++model) {
        plaits.setParam(ps::PlaitsSynth::Model, model / float(ps::PlaitsSynth::kModels - 1));
        for (int v = 0; v < ps::PlaitsSynth::kVoices; ++v) plaits.noteOn(48 + v * 3, 0.9f, v * 7);
        std::vector<float> buf(kRate / 4, 0.0f);
        const double start = threadSeconds();
        plaits.render(buf.data(), kRate / 4);
        const double load = (threadSeconds() - start) / 0.25;
        float pk = 0.0f;
        bool finite = true;
        for (float v : buf) { pk = std::max(pk, std::fabs(v)); finite &= std::isfinite(v); }
        if (!finite || pk < 1e-3f || pk > 3.0f || load > 0.12) std::printf("  model %d: peak %.3f load %.0f %% finite %d\n", model, pk, load * 100, finite);
        CHECK(finite && pk > 1e-3f && pk < 3.0f);
#if !defined(__SANITIZE_ADDRESS__)
        CHECK_TIMING(load < 0.12);  // heavy models are capped in voices: any model stays cheap here
#endif
        plaits.allNotesOff();
        std::vector<float> flush(kRate, 0.0f);
        plaits.render(flush.data(), kRate);
    }
}

// Audit 28 review m8: an export (or a preview) renders Plaits on its own thread while the audio
// thread does: they share stmlib's noise generator, now a relaxed atomic (a plain static before:
// a data race, which ThreadSanitizer reports). Each synth alone keeps its own noise sequence.
// A sound's calibration gain (Synth::Gain, PlaitsSynth::Gain): 0.5 is exactly the sound as before
// (old projects unchanged), +6 dB doubles it, and it stays finite at both ends.
void testSoundCalibrationGain() {
    auto rms = [](const std::vector<float>& v) {
        double s = 0.0;
        for (float x : v) s += static_cast<double>(x) * x;
        return std::sqrt(s / static_cast<double>(v.size()));
    };
    auto synthAt = [&](float gain, bool set) {
        ps::Synth synth;
        synth.prepare(kRate);
        if (set) synth.setParam(ps::Synth::Gain, gain);
        synth.noteOn(48, 0.8f);
        return renderSynth(synth, kRate / 2);
    };
    const auto unset = synthAt(0.5f, false), unity = synthAt(0.5f, true), up = synthAt(0.6f, true);
    CHECK(unset == unity);
    const double ratio = rms(up) / rms(unity);
    std::printf("  synth +6 dB gain: x%.3f\n", ratio);
    CHECK(std::fabs(ratio - 1.995) < 0.02);
    for (float g : {0.0f, 1.0f}) for (float v : synthAt(g, true)) CHECK(std::isfinite(v));
    auto plaitsAt = [&](float gain, bool set) {
        ps::PlaitsSynth plaits;
        plaits.prepare(kRate);
        plaits.setParam(ps::PlaitsSynth::Model, 0.0f);
        if (set) plaits.setParam(ps::PlaitsSynth::Gain, gain);
        plaits.noteOn(57, 1.0f, 0);
        std::vector<float> out(kRate / 2, 0.0f);
        plaits.render(out.data(), kRate / 2);
        return out;
    };
    const auto pUnset = plaitsAt(0.5f, false), pUnity = plaitsAt(0.5f, true), pDown = plaitsAt(0.4f, true);
    CHECK(pUnset == pUnity);
    const double pRatio = rms(pDown) / rms(pUnity);
    std::printf("  plaits -6 dB gain: x%.3f\n", pRatio);
    CHECK(std::fabs(pRatio - 0.501) < 0.01);
}

void testPlaitsOnTwoThreads() {
    auto render = [](int model, int frames) {
        ps::PlaitsSynth plaits;
        plaits.prepare(kRate);
        plaits.setParam(ps::PlaitsSynth::Model, model / float(ps::PlaitsSynth::kModels - 1));
        plaits.setParam(ps::PlaitsSynth::Release, 1.0f);
        std::vector<float> out(static_cast<size_t>(frames), 0.0f);
        for (int at = 0; at < frames; at += 256) {
            if (at % 4800 == 0) plaits.noteOn(48 + (at / 4800) % 12, 0.9f, 0);
            plaits.render(out.data() + at, std::min(256, frames - at));
        }
        return out;
    };
    // Alone: the same noise each time (its own sequence, whatever was drawn before).
    for (int model = 0; model < ps::PlaitsSynth::kModels; ++model) CHECK(render(model, 9600) == render(model, 9600));
    // Two threads at once, every model: finite and bounded.
    bool ok[2] = {true, true};
    std::thread other([&] {
        for (int model = 0; model < ps::PlaitsSynth::kModels; ++model)
            for (float v : render(model, 9600)) ok[1] = ok[1] && std::isfinite(v) && std::fabs(v) < 4.0f;
    });
    for (int model = ps::PlaitsSynth::kModels - 1; model >= 0; --model)
        for (float v : render(model, 9600)) ok[0] = ok[0] && std::isfinite(v) && std::fabs(v) < 4.0f;
    other.join();
    CHECK(ok[0] && ok[1]);
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

#include "master_tests.inc"

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
        const size_t at = static_cast<size_t>(b * 24000 + 3 + kLate) * 2;
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

void testLivePadsFollowTheBar() {
    ps::Engine engine(kRate);  // 120 BPM: a beat is 24000 frames
    // A 4-beat loop that starts with a loud mark, launched while the transport is stopped.
    std::vector<float> d(96000, 0.1f);
    std::fill(d.begin(), d.begin() + 200, 0.8f);
    installClip(engine, 0, ps::Sample::create(d.data(), 96000, 1, kRate, kRate));
    engine.post({ps::CommandType::SetClipParam, 4.0f, 0, ps::ClipLauncher::Beats});
    engine.post({ps::CommandType::ToggleClip, 0.0f, 0});
    render(engine, 24000 * 6 + 12000, 256);  // the idle clock is mid-bar (beat 6.5)
    // Play: the clock jumps to the song's beat 0 and the loop starts over with the bar.
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    auto out = render(engine, 1024, 256);
    CHECK(std::fabs(engine.clipBeat() - 1024.0 / 24000.0) < 1e-6);
    CHECK(out[static_cast<size_t>(53 + kLate) * 2] > 0.75f);
    // The next pass starts exactly on the next bar of the song.
    out = render(engine, 96000 - 1024 + 100, 256);
    CHECK(out[static_cast<size_t>(96000 - 1024 + 3 + kLate) * 2] > 0.75f);
    CHECK(engine.clipState(0) == ps::ClipLauncher::Playing);
    engine.post({ps::CommandType::ToggleClip, 0.0f, -1});
    render(engine, 4096, 256);
    installClip(engine, 0, nullptr);
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
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0 | (4 << 16)});  // a roll of 4, carried with the hit
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 2, 4});  // a plain hit: one trigger
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 1400, 256);
    CHECK(engine.laneTriggerCount(0) == 1);   // second hit at 1500 frames
    render(engine, 200, 256);
    CHECK(engine.laneTriggerCount(0) == 2);
    render(engine, 6000 * 4 - 1600 + 100, 256);
    CHECK(engine.laneTriggerCount(0) == 4);   // four in the step, none after
    CHECK(engine.laneTriggerCount(2) == 1);
    // Back to a plain hit (same command, no roll bits).
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    render(engine, 6000 * 12, 512);
    CHECK(engine.laneTriggerCount(0) == 5);
    // Stopping in the middle of a roll drops the hits still to come.
    engine.post({ps::CommandType::SetDrumRepeat, 4.0f, 0, 0});
    engine.post({ps::CommandType::SetTransport, 0.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    render(engine, 2000, 256);                 // first hit, then the second at 1500
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 6000 * 2, 256);
    CHECK(engine.laneTriggerCount(0) == 7);
}

// Rising zero crossings per second of a mono signal (a clean tone).
static double toneHz(const std::vector<float>& x, size_t from, size_t to) {
    int crossings = 0;
    for (size_t i = from + 1; i < to; ++i) {
        if (x[i - 1] < 0.0f && x[i] >= 0.0f) ++crossings;
    }
    return crossings * double(kRate) / double(to - from);
}

void testSampledInstrument() {
    // A one-second 440 Hz sine recorded at A4 (root 69).
    std::vector<float> wave(kRate);
    for (size_t i = 0; i < wave.size(); ++i) wave[i] = 0.5f * std::sin(2.0 * M_PI * 440.0 * double(i) / kRate);
    auto make = [&] { return ps::Sample::create(wave.data(), kRate, 1, kRate, kRate); };
    auto render = [](ps::SampleInstrument& inst, int frames) {
        std::vector<float> out(static_cast<size_t>(frames), 0.0f);
        for (int at = 0; at < frames; at += 256) inst.render(out.data() + at, std::min(256, frames - at));
        return out;
    };
    {
        // An octave up plays twice as fast, an octave down half as fast; notes start on their frame.
        ps::SampleInstrument inst;
        inst.prepare(kRate);
        inst.install(make());
        inst.setParam(ps::SampleInstrument::Root, 69.0f);
        inst.noteOn(81, 1.0f, 100);
        auto out = render(inst, 12000);
        CHECK(out[99] == 0.0f);
        CHECK(std::abs(toneHz(out, 1000, 12000) - 880.0) < 6.0);
        inst.allNotesOff();
        render(inst, 4800);
        CHECK(inst.activeVoices() == 0);
        inst.noteOn(57, 1.0f, 0);
        out = render(inst, 24000);
        CHECK(std::abs(toneHz(out, 1000, 24000) - 220.0) < 4.0);
        // Pitch bend: +12 semitones on the held A3 gives A4.
        inst.setBend(12.0f);
        out = render(inst, 12000);
        CHECK(std::abs(toneHz(out, 1000, 12000) - 440.0) < 5.0);
        inst.setBend(0.0f);
    }
    {
        // Release: the note dies away after its key is let go, without a click.
        ps::SampleInstrument inst;
        inst.prepare(kRate);
        inst.install(make());
        inst.setParam(ps::SampleInstrument::Root, 69.0f);
        inst.setParam(ps::SampleInstrument::Release, 0.1f);
        inst.setParam(ps::SampleInstrument::Loop, 1.0f);
        inst.noteOn(69, 1.0f, 0);
        render(inst, 2400);
        inst.noteOff(69, 0);
        auto out = render(inst, 9600);
        float firstPeak = 0.0f, lastPeak = 0.0f, jump = 0.0f;
        for (size_t i = 0; i < 480; ++i) firstPeak = std::max(firstPeak, std::abs(out[i]));
        for (size_t i = 4320; i < 4800; ++i) lastPeak = std::max(lastPeak, std::abs(out[i]));
        for (size_t i = 1; i < out.size(); ++i) jump = std::max(jump, std::abs(out[i] - out[i - 1]));
        CHECK(firstPeak > 0.3f);
        CHECK(lastPeak < 0.05f);
        CHECK(jump < 0.06f);  // a 440 Hz sine at 0.5 moves at most 0.029 per frame
        CHECK(inst.activeVoices() == 0);
        // A looping sound keeps going while held, well past the end of the sample.
        inst.noteOn(69, 1.0f, 0);
        out = render(inst, kRate * 3);
        float tail = 0.0f;
        for (size_t i = kRate * 2; i < out.size(); ++i) tail = std::max(tail, std::abs(out[i]));
        CHECK(tail > 0.3f);
    }
    {
        // A one-shot ends with the sample, fading over its last frames.
        ps::SampleInstrument inst;
        inst.prepare(kRate);
        inst.install(make());
        inst.setParam(ps::SampleInstrument::Root, 69.0f);
        inst.noteOn(69, 1.0f, 0);
        auto out = render(inst, kRate + 4800);
        float after = 0.0f;
        for (size_t i = kRate; i < out.size(); ++i) after = std::max(after, std::abs(out[i]));
        CHECK(after == 0.0f);
        CHECK(std::abs(out[kRate - 2]) < 0.02f);
        CHECK(inst.activeVoices() == 0);
    }
    {
        // Glide: one voice slides from A4 to A5 while both keys are held, and back when A5 is let go.
        ps::SampleInstrument inst;
        inst.prepare(kRate);
        inst.install(make());
        inst.setParam(ps::SampleInstrument::Root, 69.0f);
        inst.setParam(ps::SampleInstrument::Glide, 0.05f);
        inst.setParam(ps::SampleInstrument::Loop, 1.0f);
        inst.noteOn(69, 1.0f, 0);
        render(inst, 4800);
        inst.noteOn(81, 1.0f, 0);
        auto out = render(inst, 9600);
        CHECK(inst.activeVoices() == 1);
        const double mid = toneHz(out, 0, 2400);
        CHECK(mid > 470.0 && mid < 860.0);  // on its way up
        CHECK(std::abs(toneHz(out, 4800, 9600) - 880.0) < 8.0);
        inst.noteOff(81, 0);
        out = render(inst, 9600);
        CHECK(std::abs(toneHz(out, 4800, 9600) - 440.0) < 6.0);
        inst.noteOff(69, 0);
        render(inst, 9600);
        CHECK(inst.activeVoices() == 0);
    }
    {
        // Polyphony: ten keys at once keep at most eight voices once the cut ones have faded.
        ps::SampleInstrument inst;
        inst.prepare(kRate);
        inst.install(make());
        inst.setParam(ps::SampleInstrument::Loop, 1.0f);
        for (int n = 0; n < 10; ++n) inst.noteOn(60 + n, 0.8f, n * 10);
        auto out = render(inst, 2400);
        CHECK(inst.activeVoices() == ps::SampleInstrument::kPolyphony);
        bool finite = true;
        for (float v : out) finite = finite && std::isfinite(v);
        CHECK(finite);
        // Replacing the sound stops the voices before the old one can be freed.
        inst.install(make());
        CHECK(inst.activeVoices() == 0);
        inst.collectGarbage();
    }
    {
        // Through the engine: track 1 with the sampled source plays its piano-roll note.
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetTrackOn, 1.0f, 1});
        engine.post({ps::CommandType::SetSynthEngine, 0.0f, 2 | (1 << 16)});
        ps::Command install{ps::CommandType::InstallTrackSample, 0.0f, 1 << 16};
        install.sample = make();
        engine.post(install);
        engine.post({ps::CommandType::SetTrackSampleParam, 69.0f, ps::SampleInstrument::Root | (1 << 16)});
        engine.post({ps::CommandType::SynthNoteOn, 1.0f, 81 | (1 << 16)});
        std::vector<float> out(2 * 9600);
        for (int at = 0; at < 9600; at += 480) engine.process(nullptr, 0, 0, out.data() + 2 * at, 480, 2);
        std::vector<float> left(9600);
        for (size_t i = 0; i < left.size(); ++i) left[i] = out[2 * i];
        CHECK(engine.sampledActiveVoices(1) == 1);
        CHECK(std::abs(toneHz(left, 2400, 9600) - 880.0) < 8.0);
    }
    {
        // Switching the glide on or off with keys held releases them: no voice is left stuck.
        for (float from : {0.0f, 0.05f}) {
            ps::SampleInstrument inst;
            inst.prepare(kRate);
            inst.install(make());
            inst.setParam(ps::SampleInstrument::Loop, 1.0f);
            inst.setParam(ps::SampleInstrument::Glide, from);
            for (int n : {60, 64, 67}) inst.noteOn(n, 1.0f, 0);
            render(inst, 2400);
            inst.setParam(ps::SampleInstrument::Glide, from > 0.0f ? 0.0f : 0.05f);
            for (int n : {67, 60, 64}) inst.noteOff(n, 0);  // in another order than pressed
            render(inst, kRate);
            CHECK(inst.activeVoices() == 0);
        }
    }
    {
        // Back-to-back sequenced notes of one pitch at 140 BPM (5142.86 frames a step): each lasts
        // its whole step, none cut short by the previous one's note-off (rounding), with or
        // without glide.
        for (float glide : {0.0f, 0.08f}) {
            ps::Engine engine(kRate);
            engine.post({ps::CommandType::SetTempo, 140.0f});
            engine.post({ps::CommandType::SetTrackOn, 1.0f, 1});
            engine.post({ps::CommandType::SetSynthEngine, 0.0f, 2 | (1 << 16)});
            ps::Command install{ps::CommandType::InstallTrackSample, 0.0f, 1 << 16};
            install.sample = make();
            engine.post(install);
            engine.post({ps::CommandType::SetTrackSampleParam, 69.0f, ps::SampleInstrument::Root | (1 << 16)});
            engine.post({ps::CommandType::SetTrackSampleParam, 1.0f, ps::SampleInstrument::Loop | (1 << 16)});
            engine.post({ps::CommandType::SetTrackSampleParam, 0.05f, ps::SampleInstrument::Release | (1 << 16)});
            engine.post({ps::CommandType::SetTrackSampleParam, glide, ps::SampleInstrument::Glide | (1 << 16)});
            for (int n = 0; n < 16; ++n) {
                engine.post({ps::CommandType::SetSynthNote, 1.0f, n | (1 << 16), ps::NoteTrack::pack(n * ps::NoteTrack::kTicksPerStep, ps::NoteTrack::kTicksPerStep, 69)});
            }
            engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 16 | (1 << 16)});
            engine.post({ps::CommandType::SetSynthSteps, 0.0f, 16 | (1 << 16)});
            engine.post({ps::CommandType::SetTransport, 1.0f, 0});
            const double step = kRate * 60.0 / (140.0 * 4.0);
            const int frames = static_cast<int>(step * 16);
            std::vector<float> out(2 * static_cast<size_t>(frames));
            for (int at = 0; at < frames; at += 256) engine.process(nullptr, 0, 0, out.data() + 2 * at, std::min(256, frames - at), 2);
            int cut = 0;
            for (int k = 0; k < 15; ++k) {
                // The last quarter of each step still sounds.
                double e = 0.0;
                const int from = static_cast<int>(step * (k + 0.7));
                const int to = static_cast<int>(step * (k + 0.95));
                for (int i = from; i < to; ++i) e += double(out[2 * size_t(i)]) * out[2 * size_t(i)];
                if (std::sqrt(e / (to - from)) < 0.05) ++cut;
            }
            std::printf("  glide %.2f: %d of 15 steps cut short\n", glide, cut);
            CHECK(cut == 0);
        }
    }
}

void testDrumPanAndStepPitch() {
    auto energy = [](const std::vector<float>& out, int c) {
        double e = 0.0;
        for (size_t i = static_cast<size_t>(c); i < out.size(); i += kChannels) e += double(out[i]) * out[i];
        return e;
    };
    {
        // The kick panned hard left: nothing on the right; a step pan back to the centre evens it out.
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetPadPan, -1.0f, ps::DrumKit::Kick});
        engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, 0});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto left = render(engine, 6000, 256);
        CHECK(energy(left, 0) > 1.0 && energy(left, 1) < energy(left, 0) * 1e-6);
        engine.post({ps::CommandType::SetTransport, 0.0f});
        render(engine, 6000 * 4, 256);
        engine.post({ps::CommandType::SetDrumStepDetail, 1.0f, ps::DrumKit::Kick, 0 | (64 << 16)});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto centre = render(engine, 6000, 256);
        CHECK(std::fabs(energy(centre, 0) / energy(centre, 1) - 1.0) < 0.01);
        // A new SetDrumHit puts the step back to plain (the pad's own pan again).
        engine.post({ps::CommandType::SetDrumHit, 1.0f, ps::DrumKit::Kick, 0});
        engine.post({ps::CommandType::SetTransport, 0.0f});
        render(engine, 6000 * 4, 256);
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        auto again = render(engine, 6000, 256);
        // (The centred kick still rings in the limiter's look-ahead until the new hit cuts it.)
        again.erase(again.begin(), again.begin() + size_t(kLate) * kChannels);
        CHECK(energy(again, 1) < energy(again, 0) * 1e-6);
    }
    {
        // A 500 Hz tone on pad 4, the step one octave up: 1 kHz.
        std::vector<float> tone(static_cast<size_t>(kRate / 10));
        for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.5f * static_cast<float>(std::sin(2 * M_PI * 500.0 * i / kRate));
        ps::Sample* s = ps::Sample::create(tone.data(), static_cast<int64_t>(tone.size()), 1, kRate, kRate);
        ps::Engine engine(kRate);
        s->retain();
        engine.post({ps::CommandType::InstallSample, 0.0f, 3, 0, s});
        engine.post({ps::CommandType::SetDrumHit, 1.0f, 3, 0});
        engine.post({ps::CommandType::SetDrumStepDetail, 0.0f, 3, 0 | ((64 + 12) << 16)});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        const auto out = render(engine, 2400, 256);
        int crossings = 0;
        for (size_t i = 200; i < 2200; ++i) {
            if ((out[(i - 1) * kChannels] < 0.0f) != (out[i * kChannels] < 0.0f)) ++crossings;
        }
        std::printf("  step pitch +12: %d crossings in 2000 frames (1 kHz = ~83)\n", crossings);
        // Stereo sounds stay stereo: a sound on the left only is heard on the left only.
        std::vector<float> leftOnly(static_cast<size_t>(kRate / 10) * 2, 0.0f);
        for (size_t i = 0; i < leftOnly.size() / 2; ++i) leftOnly[i * 2] = 0.5f * static_cast<float>(std::sin(2 * M_PI * 300.0 * i / kRate));
        ps::Sample* st = ps::Sample::create(leftOnly.data(), static_cast<int64_t>(leftOnly.size() / 2), 2, kRate, kRate);
        st->retain();
        engine.post({ps::CommandType::InstallSample, 0.0f, 5, 0, st});
        engine.post({ps::CommandType::TriggerPad, 1.0f, 5});
        const auto stereo = render(engine, 2400, 256);
        double el = 0.0, er = 0.0;
        for (size_t i = 0; i < 2400; ++i) {
            el += double(stereo[i * kChannels]) * stereo[i * kChannels];
            er += double(stereo[i * kChannels + 1]) * stereo[i * kChannels + 1];
        }
        CHECK(el > 1.0 && er < el * 0.01 + 1e-3);  // the 500 Hz tone of pad 4 still rings on both sides
        st->release();
        CHECK(crossings > 75 && crossings < 90);
        s->release();
    }
}

void testTrackMacros() {
    // A bright synth chord through channel 2 (the first instrument track).
    auto run = [](std::initializer_list<std::pair<int, float>> macros, int frames, bool stopEarly) {
        ps::Engine engine(kRate);
        for (auto [m, v] : macros) engine.post({ps::CommandType::SetMacro, v, 2, m});
        engine.post({ps::CommandType::SynthNoteOn, 1.0f, 72});
        engine.post({ps::CommandType::SynthNoteOn, 1.0f, 79});
        auto out = render(engine, stopEarly ? kRate / 4 : frames, 256);
        if (stopEarly) {
            engine.post({ps::CommandType::SynthNoteOff, 0.0f, 72});
            engine.post({ps::CommandType::SynthNoteOff, 0.0f, 79});
            render(engine, kRate / 2, 256);  // the note's own release
            out = render(engine, frames, 256);
        }
        return out;
    };
    auto rms = [](const std::vector<float>& v) {
        double e = 0.0;
        for (float x : v) e += double(x) * x;
        return std::sqrt(e / std::max<size_t>(1, v.size()));
    };
    // High-frequency energy: first difference of the left channel.
    auto bright = [](const std::vector<float>& v) {
        double e = 0.0;
        for (size_t i = kChannels; i < v.size(); i += kChannels) e += std::pow(double(v[i]) - v[i - kChannels], 2);
        return e;
    };
    const auto plain = run({}, kRate / 2, false);
    const auto rest = run({{ps::TrackMacros::Color, 0.5f}, {ps::TrackMacros::Dirt, 0.0f}}, kRate / 2, false);
    CHECK(plain == rest);  // at rest the macros leave the sound untouched, sample for sample
    const auto dark = run({{ps::TrackMacros::Color, 0.05f}}, kRate / 2, false);
    const auto thin = run({{ps::TrackMacros::Color, 0.95f}}, kRate / 2, false);
    std::printf("  colour: plain %.3g, dark %.3g, high-pass rms %.3f vs %.3f\n", bright(plain), bright(dark), rms(thin), rms(plain));
    CHECK(bright(dark) < bright(plain) * 0.2);
    CHECK(rms(thin) < rms(plain));
    const auto dirty = run({{ps::TrackMacros::Dirt, 1.0f}}, kRate / 2, false);
    const auto half = run({{ps::TrackMacros::Dirt, 0.5f}}, kRate / 2, false);
    std::printf("  dirt: rms %.3f (half %.3f) vs %.3f\n", rms(dirty), rms(half), rms(plain));
    CHECK(rms(half) > 0.5 * rms(plain) && rms(half) < 2.0 * rms(plain));
    CHECK(dirty != plain && rms(dirty) > 0.2 * rms(plain) && rms(dirty) < 3.0 * rms(plain));
    const auto moving = run({{ps::TrackMacros::Motion, 1.0f}}, kRate / 2, false);
    CHECK(moving != plain);
    // Space: a reverb tail after the note has died away.
    const auto dryTail = run({}, kRate / 4, true);
    const auto wetTail = run({{ps::TrackMacros::Space, 1.0f}}, kRate / 4, true);
    std::printf("  space tail: dry %.5f, wet %.5f\n", rms(dryTail), rms(wetTail));
    CHECK(rms(wetTail) > 10 * rms(dryTail) + 1e-4);
    for (const auto* v : {&dark, &thin, &dirty, &moving, &wetTail}) {
        for (float x : *v) CHECK(std::isfinite(x));
    }
    // Very loud noise with the colour just below the middle (cutoff near the top, where an
    // overdriven state-variable filter blows up): it stays finite and bounded.
    for (float colour : {0.47f, 0.485f, 0.495f, 0.02f, 0.98f}) {
        ps::TrackMacros m;
        m.prepare(kRate);
        m.set(ps::TrackMacros::Color, colour);
        m.set(ps::TrackMacros::Dirt, 1.0f);
        m.set(ps::TrackMacros::Motion, 1.0f);
        std::mt19937 rng(7);
        std::uniform_real_distribution<float> noise(-3.0f, 3.0f);
        float worst = 0.0f;
        bool finite = true;
        std::vector<float> l(256), r(256);
        for (int block = 0; block < kRate / 256; ++block) {
            for (int i = 0; i < 256; ++i) l[i] = r[i] = noise(rng);
            m.process(l.data(), r.data(), 256);
            for (int i = 0; i < 256; ++i) {
                finite &= std::isfinite(l[i]) && std::isfinite(r[i]);
                worst = std::max(worst, std::max(std::fabs(l[i]), std::fabs(r[i])));
            }
        }
        CHECK(finite && worst < 50.0f);
    }
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

void testStemsKeepTheirPlaceAcrossATempoChange() {
    // A ramp on every stem's left side reads the song position. Two seconds in, the copy of the
    // song for a tempo 0.8 times as fast lands (1.25 times as long, same ramp): it goes on from the
    // same musical point (frame x 1.25), crossfading in, no jump, no restart.
    constexpr int songFrames = kRate * 8;
    auto ramp = [](int frames) {
        std::vector<float> x(static_cast<size_t>(frames) * 2, 0.0f);
        for (int i = 0; i < frames; ++i) x[static_cast<size_t>(i) * 2] = 0.1f * static_cast<float>(i) / frames;
        return std::vector<std::vector<float>>(4, x);
    };
    auto* song = ps::StemBundle::fromFloat(ramp(songFrames));
    auto* slow = ps::StemBundle::fromFloat(ramp(songFrames * 5 / 4), song);
    auto* other = ps::StemBundle::fromFloat(ramp(songFrames));  // another song: no mapping
    CHECK(slow->song() == song->song() && other->song() != song->song());
    ps::StemPlayer player;
    player.prepare(kRate);
    song->retain();
    player.install(song);
    player.start(0);
    std::vector<float> l(kRate * 4, 0.0f), r(kRate * 4, 0.0f);
    auto play = [&](int from, int to) {
        for (int at = from; at < to; at += 256) player.render(l.data() + at, r.data() + at, std::min(256, to - at));
    };
    play(0, kRate * 2);
    CHECK(std::fabs(l[0]) < 1e-6f);  // start: a fade in, no click
    slow->retain();
    player.install(slow);
    play(kRate * 2, kRate * 3);
    const float jump = largestStep(l, kRate * 2 - 10, kRate * 3);
    // A second later the new copy is a second further: (2 x 1.25 + 1) s of its 10 s (the old,
    // unmapped position would read 3 s of 10 s: 0.12 instead of 0.14).
    const float want = 0.4f * static_cast<float>(kRate * 3.5 - 1) / (songFrames * 1.25f);
    std::printf("  stems tempo swap: largest step %.6f, at 3 s %.5f (%.5f wanted)\n", jump, l[kRate * 3 - 1], want);
    CHECK(jump < 1e-3f);
    CHECK(std::fabs(l[kRate * 3 - 1] - want) < 1e-3f);
    CHECK(player.position() == std::llround(kRate * 2 * 1.25) + kRate);
    // Another song replacing it crossfades too, at the same frame (no mapping).
    other->retain();
    player.install(other);
    play(kRate * 3, kRate * 3 + 2048);
    CHECK(player.position() == std::llround(kRate * 2 * 1.25) + kRate + 2048);
    CHECK(largestStep(l, kRate * 3 - 10, kRate * 3 + 2048) < 0.01f);
    // Stop: faded out, then silence. Unload while silent: let go at once.
    player.stop();
    play(kRate * 3 + 2048, kRate * 4);
    CHECK(largestStep(l, kRate * 3 + 2048, kRate * 4) < 0.01f);
    CHECK(l[kRate * 4 - 1] == 0.0f);
    CHECK(player.position() == -1);
    player.install(nullptr);
    player.collectGarbage();
    CHECK(song->refCount() == 1 && slow->refCount() == 1 && other->refCount() == 1);
    // Unload while playing: a fade out, not a cut; the bundle is let go after it.
    other->retain();
    player.install(other);
    player.start(0);
    std::fill(l.begin(), l.end(), 0.0f);
    play(0, kRate);
    player.install(nullptr);
    CHECK(other->refCount() == 2);  // still fading
    play(kRate, kRate + 2048);
    CHECK(largestStep(l, kRate - 10, kRate + 2048) < 0.01f);
    CHECK(l[kRate + 2047] == 0.0f);
    player.collectGarbage();
    CHECK(other->refCount() == 1);
    for (auto* b : {song, slow, other}) {
        CHECK(b->release());
        delete b;
    }
}

void testStemsComeInLaterInTheSong() {
    // R2 (remix lot 1): the song placed later in the project (its voice on the drop) comes in on
    // its own frame: a negative start frame is that much silence after step 0, then the song from
    // its first frame with the 5 ms fade in starting there (not spent in the silence).
    constexpr int songFrames = kRate * 2;
    std::vector<float> ramp(static_cast<size_t>(songFrames) * 2, 0.0f);
    for (int i = 0; i < songFrames; ++i) {
        ramp[static_cast<size_t>(i) * 2] = 0.2f;  // left: constant
        ramp[static_cast<size_t>(i) * 2 + 1] = 0.2f * static_cast<float>(i) / songFrames;  // right: the position
    }
    auto* song = ps::StemBundle::fromFloat(std::vector<std::vector<float>>(4, ramp));
    ps::StemPlayer player;
    player.prepare(kRate);
    song->retain();
    player.install(song);
    const int lead = kRate / 2;  // the song comes in 0.5 s after step 0
    player.setStartFrame(-lead);
    player.start(100);
    std::vector<float> l(kRate * 2, 0.0f), r(kRate * 2, 0.0f);
    int64_t positionInLead = -1;
    for (int at = 0; at < kRate * 2; at += 256) {
        player.render(l.data() + at, r.data() + at, std::min(256, kRate * 2 - at));
        if (at + 256 < 100 + lead && player.position() != -1) positionInLead = player.position();
    }
    const int in = 100 + lead;  // the song's first frame
    float before = 0.0f;
    for (int i = 0; i < in; ++i) before = std::max(before, std::fabs(l[static_cast<size_t>(i)]));
    std::printf("  stems placed later: peak before %.6f, at +2.5 ms %.4f, at +10 ms %.4f, position in the lead %lld\n",
                before, l[static_cast<size_t>(in + 120)], l[static_cast<size_t>(in + 480)], static_cast<long long>(positionInLead));
    CHECK(before == 0.0f);                 // silence until the song comes in
    CHECK(positionInLead == -1);           // nothing to show yet
    CHECK(l[static_cast<size_t>(in)] == 0.0f);  // the fade in starts on its first frame (no click)
    CHECK(l[static_cast<size_t>(in + 120)] > 0.1f * l[static_cast<size_t>(in + 480)]);
    CHECK(l[static_cast<size_t>(in + 120)] < 0.9f * l[static_cast<size_t>(in + 480)]);
    CHECK(largestStep(l, in - 10, in + 2000) < 0.01f);
    // The right side reads the song's own position: frame 0 at the entry, so ~1 s later 1 s in.
    const float heard = r[static_cast<size_t>(in + kRate - 1)] / l[static_cast<size_t>(in + kRate - 1)];
    CHECK(std::fabs(heard - static_cast<float>(kRate - 1) / songFrames) < 2e-3f);
    CHECK(player.position() == kRate * 2 - in);
    // A start frame inside the song still starts there (nothing changed for 0 and up).
    player.stop();
    player.setStartFrame(kRate / 4);
    player.start(0);
    std::fill(l.begin(), l.end(), 0.0f);
    std::fill(r.begin(), r.end(), 0.0f);
    player.render(l.data(), r.data(), 256);
    player.render(l.data() + 256, r.data() + 256, 256);
    CHECK(player.position() == kRate / 4 + 512);
    player.install(nullptr);
    player.render(l.data(), r.data(), 2048);  // its fade out, then it is let go
    player.collectGarbage();
    CHECK(song->refCount() == 1);
    CHECK(song->release());
    delete song;
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
    // Audit 02 M7: apart from the project's strips: a track left in solo, or the remix's strip
    // muted, never silences what is listened to.
    engine.post({ps::CommandType::SetChannelSolo, 1.0f, 0});
    engine.post({ps::CommandType::SetChannelMute, 1.0f, 3});
    out = render(engine, 9600, 256);
    CHECK(std::fabs(out[out.size() - 2] - 0.4f) < 2e-3f);
    engine.post({ps::CommandType::SetChannelSolo, 0.0f, 0});
    engine.post({ps::CommandType::SetChannelMute, 0.0f, 3});
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


// --- Live grid: one slot per track, launched on a global quantum ------------------------------

constexpr int kLiveStep = 6000;  // 120 BPM: frames in a sixteenth

// Fills the waiting scene's drums with `hits` (lane, step) only, in a loop of `steps`.
void stageDrums(ps::Engine& engine, std::initializer_list<std::pair<int, int>> hits, int steps) {
    engine.post({ps::CommandType::SetStaging, 1.0f});
    for (int lane = 0; lane < ps::Engine::kLanes; ++lane)
        for (int st = 0; st < ps::Engine::kMaxSteps; ++st) engine.post({ps::CommandType::SetDrumHit, 0.0f, lane, st});
    for (const auto& [lane, st] : hits) engine.post({ps::CommandType::SetDrumHit, 1.0f, lane, st});
    engine.post({ps::CommandType::SetPatternSteps, 0.0f, steps});
    engine.post({ps::CommandType::SetStaging, 0.0f});
}

// Renders up to (not including) the frame of transport step `step` (the engine's clock at `now` frames).
void renderToStep(ps::Engine& engine, int64_t& now, int step, int extra = 0) {
    const int64_t to = static_cast<int64_t>(step) * kLiveStep + extra;
    if (to > now) render(engine, static_cast<int>(to - now), 256);
    now = std::max(now, to);
}

void testLiveSlotLaunchesOnTheBar() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});  // the scene: pad 1 on step 0
    stageDrums(engine, {{2, 0}, {2, 8}}, 16);                // waiting for the drums: pad 3 on 0 and 8
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 6);
    engine.post({ps::CommandType::ArmLaunch, 1.0f, 1 << ps::kSlotDrums, 16});
    renderToStep(engine, now, 16);
    ps::LaunchEvent e;
    CHECK(engine.laneTriggerCount(0) == 1 && engine.laneTriggerCount(2) == 0);
    CHECK(engine.slotPending(ps::kSlotDrums) == 1 && engine.slotTag(ps::kSlotDrums) == ps::Engine::kRowTag);
    CHECK(!engine.popLaunchEvent(e));
    renderToStep(engine, now, 16, 10);  // the bar line: the drums take over before its step sounds
    CHECK(engine.laneTriggerCount(0) == 1 && engine.laneTriggerCount(2) == 1);
    CHECK(engine.slotTag(ps::kSlotDrums) == 1 && engine.slotStart(ps::kSlotDrums) == 16);
    CHECK(engine.slotPending(ps::kSlotDrums) == ps::Engine::kNoLaunch && engine.transportStep() == 16);
    CHECK(engine.popLaunchEvent(e) && e.slot == ps::kSlotDrums && e.tag == 1 && e.step == 16);
    CHECK(!engine.popLaunchEvent(e));
    CHECK(engine.sceneSwaps() == 0);  // one track, not the scene
    CHECK(engine.slotTag(ps::kSlotAcid) == ps::Engine::kRowTag && engine.slotStart(ps::kSlotAcid) == 0);
    renderToStep(engine, now, 32);
    CHECK(engine.laneTriggerCount(2) == 2 && engine.laneTriggerCount(0) == 1);
    // A two-bar quantum armed in bar 3 (step 36): it waits for step 64, not the next bar (48).
    stageDrums(engine, {{5, 0}}, 16);
    renderToStep(engine, now, 36);
    engine.post({ps::CommandType::ArmLaunch, 2.0f, 1 << ps::kSlotDrums, 32});
    renderToStep(engine, now, 64);
    CHECK(engine.laneTriggerCount(5) == 0 && engine.slotTag(ps::kSlotDrums) == 1);
    renderToStep(engine, now, 64, 10);
    CHECK(engine.laneTriggerCount(5) == 1 && engine.slotTag(ps::kSlotDrums) == 2 && engine.slotStart(ps::kSlotDrums) == 64);
    std::printf("  live launch: pad3 %u, pad6 %u, drums from step %d\n", engine.laneTriggerCount(2), engine.laneTriggerCount(5), engine.slotStart(ps::kSlotDrums));
}

// A launch that comes just after its line (a tap on the « one » heard, the output's latency later)
// launches on that line: counted from it, from the next step on. Past the grace it waits as before.
void testLiveLaunchGraceTakesTheLineJustPassed() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});  // the scene: pad 1 on step 0
    stageDrums(engine, {{2, 0}, {2, 8}}, 16);                // waiting: pad 3 on 0 and 8
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 16, kLiveStep / 2);  // half a step past the bar line (step 16.5)
    const int grace = 2 * 64;                       // two steps
    engine.post({ps::CommandType::ArmLaunch, 1.0f, 1 << ps::kSlotDrums, 16 | grace << 17});
    renderToStep(engine, now, 17, 10);
    ps::LaunchEvent e;
    CHECK(engine.slotTag(ps::kSlotDrums) == 1 && engine.slotStart(ps::kSlotDrums) == 16);
    CHECK(engine.slotPending(ps::kSlotDrums) == ps::Engine::kNoLaunch);
    CHECK(engine.popLaunchEvent(e) && e.slot == ps::kSlotDrums && e.tag == 1 && e.step == 16);
    CHECK(engine.laneTriggerCount(2) == 0);  // its step 0 already passed: not played late
    renderToStep(engine, now, 24, 10);
    CHECK(engine.laneTriggerCount(2) == 1);  // its step 8 on transport step 24: in phase with the line
    // Past the grace (three steps after the line): the next line, as always.
    stageDrums(engine, {{5, 0}}, 16);
    renderToStep(engine, now, 35, 10);
    engine.post({ps::CommandType::ArmLaunch, 2.0f, 1 << ps::kSlotDrums, 16 | grace << 17});
    renderToStep(engine, now, 47, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 1 && engine.slotPending(ps::kSlotDrums) == 2);
    renderToStep(engine, now, 48, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 2 && engine.slotStart(ps::kSlotDrums) == 48 && engine.laneTriggerCount(5) == 1);
    CHECK(engine.popLaunchEvent(e) && e.step == 48);
    // No grace (0): just past the line still waits for the next one.
    stageDrums(engine, {{6, 0}}, 16);
    renderToStep(engine, now, 64, kLiveStep / 2);
    engine.post({ps::CommandType::ArmLaunch, 3.0f, 1 << ps::kSlotDrums, 16});
    renderToStep(engine, now, 79, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 2);
    renderToStep(engine, now, 80, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 3 && engine.slotStart(ps::kSlotDrums) == 80);
    // A whole row within its grace: the scene starts on the line too.
    stageDrums(engine, {{7, 0}}, 16);
    renderToStep(engine, now, 96, kLiveStep / 4);
    engine.post({ps::CommandType::ArmLaunch, 4.0f, ps::kAllSlots, 16 | grace << 17});
    renderToStep(engine, now, 98, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 4 && engine.slotStart(ps::kSlotAcid) == 96 && engine.sceneSwaps() == 1);
    std::printf("  launch grace: drums from %d, row from %d\n", engine.slotStart(ps::kSlotDrums), engine.slotStart(ps::kSlotAcid));
}

// Each slot counts from its own launch: patterns of different lengths launched at different bars
// play their steps from there (not from the scene's start).
void testLiveSlotsKeepTheirOwnLoops() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    engine.post({ps::CommandType::SetAcidStep, 0.0f, 0, ps::AcidStep{0, 0, false, false, true}.pack()});
    // Waiting: drums of 32 steps with pad 4 on step 16; acid of 32 with a note on step 8 only; a
    // piano-roll note on step 8 of a 32-step clip.
    stageDrums(engine, {{3, 16}}, 32);
    engine.post({ps::CommandType::SetStaging, 1.0f});
    for (int i = 0; i < ps::AcidTrack::kMaxSteps; ++i) engine.post({ps::CommandType::SetAcidStep, 0.0f, i, ps::AcidStep{}.pack()});
    engine.post({ps::CommandType::SetAcidStep, 0.0f, 8, ps::AcidStep{0, 0, false, false, true}.pack()});
    engine.post({ps::CommandType::SetAcidLength, 0.0f, 32});
    engine.post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(8 * ps::NoteTrack::kTicksPerStep, 12, 60)});
    engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
    engine.post({ps::CommandType::SetSynthSteps, 0.0f, 32});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 4);
    engine.post({ps::CommandType::ArmLaunch, 1.0f, (1 << ps::kSlotDrums) | (1 << ps::kSlotAcid) | (1 << ps::kSlotInst), 16});
    renderToStep(engine, now, 16, 10);
    const uint32_t acidAt16 = engine.acidNotesPlayed();
    CHECK(engine.laneTriggerCount(3) == 0);  // counted from the scene, step 16 would hit here
    renderToStep(engine, now, 24);
    CHECK(engine.acidNotesPlayed() == acidAt16 && engine.synthActiveVoices() == 0);
    renderToStep(engine, now, 24, 200);  // the acid's and the piano roll's step 8, from their launch
    CHECK(engine.acidNotesPlayed() == acidAt16 + 1);
    CHECK(engine.synthActiveVoices() == 1);
    renderToStep(engine, now, 32, 10);  // the drums' step 16, from their launch
    CHECK(engine.laneTriggerCount(3) == 1);
    renderToStep(engine, now, 64, 10);
    CHECK(engine.laneTriggerCount(3) == 2 && engine.acidNotesPlayed() == acidAt16 + 2);
    CHECK(engine.laneTriggerCount(0) == 1);  // the scene's kick, never again
}

// Stopping a slot sounds exactly like launching an empty part on it, sample for sample, for each
// kind of track; an acid note sliding into the stop lets go.
void testLiveStopIsEmptiedContent() {
    std::vector<float> clip(6000 * 6, 0.2f);
    for (int i = 0; i < 6000 * 6; ++i) clip[size_t(i)] = 0.3f * std::sin(i * 0.01f);
    auto make = [&](int slot, bool viaStop) {
        auto engine = std::make_unique<ps::Engine>(kRate);
        for (int st = 0; st < 16; st += 4) engine->post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
        for (int st = 0; st < 16; st += 2) engine->post({ps::CommandType::SetAcidStep, 0.0f, st, ps::AcidStep{st % 12, 0, false, false, true}.pack()});
        // The last step slides into the first.
        engine->post({ps::CommandType::SetAcidStep, 0.0f, 15, ps::AcidStep{3, 0, false, true, true}.pack()});
        engine->post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 6 * 24, 64)});
        engine->post({ps::CommandType::SetSynthNote, 0.9f, 1, ps::NoteTrack::pack(12 * 24, 8 * 24, 67)});  // rings over the bar line
        engine->post({ps::CommandType::SetSynthNoteCount, 0.0f, 2});
        ps::Command c{ps::CommandType::InstallAudioClip, 0.0f, 0};
        c.sample = ps::Sample::create(clip.data(), int(clip.size()), 1, kRate, kRate);
        engine->post(c);
        engine->post({ps::CommandType::SetAudioClipParam, 16.0f, 0, ps::AudioTracks::LoopSteps});
        engine->post({ps::CommandType::SetTransport, 1.0f, 0});
        auto first = render(*engine, kLiveStep * 8, 256);
        if (viaStop) {
            engine->post({ps::CommandType::ArmLaunch, -1.0f, 1 << slot, 16});
        } else {
            engine->post({ps::CommandType::SetStaging, 1.0f});
            if (slot == ps::kSlotDrums) {
                for (int lane = 0; lane < ps::Engine::kLanes; ++lane)
                    for (int st = 0; st < ps::Engine::kMaxSteps; ++st) engine->post({ps::CommandType::SetDrumHit, 0.0f, lane, st});
                engine->post({ps::CommandType::SetPatternSteps, 0.0f, 16});
            } else if (slot == ps::kSlotAcid) {
                for (int i = 0; i < ps::AcidTrack::kMaxSteps; ++i) engine->post({ps::CommandType::SetAcidStep, 0.0f, i, ps::AcidStep{}.pack()});
                engine->post({ps::CommandType::SetAcidLength, 0.0f, 16});
            } else if (slot == ps::kSlotInst) {
                engine->post({ps::CommandType::SetSynthNoteCount, 0.0f, 0});
                engine->post({ps::CommandType::SetSynthSteps, 0.0f, 16});
            } else {
                engine->post({ps::CommandType::SetAudioOn, 0.0f, 0});
            }
            engine->post({ps::CommandType::SetStaging, 0.0f});
            engine->post({ps::CommandType::ArmLaunch, 7.0f, 1 << slot, 16});
        }
        auto rest = render(*engine, kLiveStep * 40, 256);
        first.insert(first.end(), rest.begin(), rest.end());
        // The track's own strip, over the last bar.
        const int ch = slot == ps::kSlotDrums ? 0 : slot == ps::kSlotAcid ? 1 : slot == ps::kSlotInst ? 2 : ps::audioChannel(0);
        engine->mixer().takeChannelPeak(ch);
        render(*engine, kLiveStep * 16, 256);
        const float tail = engine->mixer().takeChannelPeak(ch);
        engine->post({ps::CommandType::SetTransport, 0.0f});
        engine->post({ps::CommandType::InstallAudioClip, 0.0f, 0});
        render(*engine, 512, 256);
        engine->collectGarbage();
        return std::make_pair(first, tail);
    };
    for (const int slot : {ps::kSlotDrums, ps::kSlotAcid, ps::kSlotInst, ps::kSlotAudio}) {
        const auto [stopped, tail] = make(slot, true);
        const auto [emptied, tail2] = make(slot, false);
        bool same = stopped.size() == emptied.size();
        for (size_t i = 0; same && i < stopped.size(); ++i) same = stopped[i] == emptied[i];
        bool finite = true;
        for (float v : stopped) finite &= std::isfinite(v) && std::fabs(v) <= 1.0f;
        std::printf("  live stop, slot %d: identical %d, strip after %.6f\n", slot, same ? 1 : 0, tail);
        CHECK(same && finite);
        CHECK(tail < 1e-3f && tail2 < 1e-3f);  // silent after: the sliding acid note let go too
    }
}

void testLiveLastCommandWins() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 2);
    const int32_t drums = 1 << ps::kSlotDrums;
    engine.post({ps::CommandType::ArmLaunch, 1.0f, drums, 16});
    engine.post({ps::CommandType::ArmLaunch, 2.0f, drums, 16});
    renderToStep(engine, now, 16, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 2);
    // A stop armed then cancelled: nothing happens.
    engine.post({ps::CommandType::ArmLaunch, -1.0f, drums, 16});
    engine.post({ps::CommandType::CancelLaunch, 0.0f, drums});
    renderToStep(engine, now, 32, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 2 && engine.slotPending(ps::kSlotDrums) == ps::Engine::kNoLaunch);
    // A scene armed, then a stop for the same slot: the stop.
    engine.post({ps::CommandType::ArmLaunch, 3.0f, drums, 16});
    engine.post({ps::CommandType::ArmLaunch, -1.0f, drums, 16});
    renderToStep(engine, now, 48, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == -1);
    ps::LaunchEvent e;
    int events = 0;
    while (engine.popLaunchEvent(e)) ++events;
    CHECK(events == 2);  // one per launch that happened
}

void testSceneSwapCancelsLaunches() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 2);
    engine.post({ps::CommandType::ArmLaunch, 1.0f, (1 << ps::kSlotDrums) | (1 << ps::kSlotRemix), 16});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    renderToStep(engine, now, 16, 10);
    ps::LaunchEvent e;
    CHECK(engine.sceneSwaps() == 1 && !engine.popLaunchEvent(e));
    CHECK(engine.slotTag(ps::kSlotDrums) == ps::Engine::kRowTag && engine.slotStart(ps::kSlotDrums) == 16);
    CHECK(engine.slotPending(ps::kSlotDrums) == ps::Engine::kNoLaunch);
    // A whole row armed after a scene swap wins over it (both due on step 32: one change only); it
    // changes the scene (sceneSwaps).
    renderToStep(engine, now, 20);
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0});
    engine.post({ps::CommandType::ArmLaunch, 4.0f, ps::kAllSlots, 32});
    renderToStep(engine, now, 32);
    CHECK(engine.sceneSwaps() == 1);
    renderToStep(engine, now, 32, 10);
    std::printf("  row launch: swaps %u, absolute step %d\n", engine.sceneSwaps(), engine.absoluteStep());
    CHECK(engine.sceneSwaps() == 2);
    bool all = true;
    for (int k = 0; k < ps::kSlots; ++k) all &= engine.slotTag(k) == 4 && engine.slotStart(k) == 32;
    CHECK(all);
    CHECK(engine.absoluteStep() == 0);  // the scene counts from the row's launch
    // Stop: nothing armed survives, every slot back to the row.
    engine.post({ps::CommandType::ArmLaunch, 5.0f, 1 << ps::kSlotAcid, 16});
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 256, 256);
    CHECK(engine.slotPending(ps::kSlotAcid) == ps::Engine::kNoLaunch && engine.slotTag(ps::kSlotAcid) == ps::Engine::kRowTag);
    CHECK(engine.transportStep() == -1);
}

// ■ Fin of a take (leaveSong: ArmSceneSwap(-1, 3)) while a pad waits: it still launches on its
// line, with what it staged (audit 28 live M1). During the song (a swap armed), leaving it drops
// what was armed, as before.
void testLeaveSongKeepsTheGridsLaunches() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, 0});
    stageDrums(engine, {{2, 0}}, 16);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 6);
    engine.post({ps::CommandType::ArmLaunch, 1.0f, 1 << ps::kSlotDrums, 16});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, -1, 3});
    renderToStep(engine, now, 16, 10);
    const unsigned before = engine.laneTriggerCount(2);
    renderToStep(engine, now, 20);
    std::printf("  leave song with a pad waiting: drums tag %d, pending %d, lane 3 hits %u\n",
                engine.slotTag(ps::kSlotDrums), engine.slotPending(ps::kSlotDrums), engine.laneTriggerCount(2));
    CHECK(engine.slotTag(ps::kSlotDrums) == 1 && engine.slotStart(ps::kSlotDrums) == 16);
    CHECK(engine.slotPending(ps::kSlotDrums) == ps::Engine::kNoLaunch);
    CHECK(engine.laneTriggerCount(2) >= 1 && before >= 1);
    // A scene swap armed (the song following its parts): leaving the song drops the launches too.
    renderToStep(engine, now, 22);
    engine.post({ps::CommandType::ArmLaunch, 2.0f, 1 << ps::kSlotAcid, 16});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, 0, 1});
    engine.post({ps::CommandType::ArmLaunch, 2.0f, 1 << ps::kSlotAcid, 16});
    engine.post({ps::CommandType::ArmSceneSwap, 0.0f, -1, 3});
    renderToStep(engine, now, 32, 10);
    CHECK(engine.slotPending(ps::kSlotAcid) == ps::Engine::kNoLaunch && engine.slotTag(ps::kSlotAcid) != 2);
}

// Armed while stopped: it fires on the very first step. A slot can start part-way into its part.
void testLiveLaunchFromStopAndOffset() {
    ps::Engine engine(kRate);
    stageDrums(engine, {{1, 0}, {4, 16}}, 32);
    // As the app sends it: with the part staged (a launch takes the staged offset).
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetSlotOffset, 0.0f, ps::kSlotDrums, 16});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmLaunch, 1.0f, 1 << ps::kSlotDrums, 16});
    engine.post({ps::CommandType::ArmLaunch, -1.0f, 1 << ps::kSlotAcid, 16});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 0, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 1 && engine.slotTag(ps::kSlotAcid) == -1);
    CHECK(engine.slotStart(ps::kSlotDrums) == -16);  // it had played 16 steps already
    CHECK(engine.laneTriggerCount(4) == 1 && engine.laneTriggerCount(1) == 0);  // its step 16 on the first step
    renderToStep(engine, now, 16, 10);
    CHECK(engine.laneTriggerCount(1) == 1);  // then its step 0
}

// The remix slot: stopped, the song goes quiet but keeps its place; launched, it is back in time.
void testLiveRemixSlot() {
    const float levels[4] = {0.1f, 0.1f, 0.1f, 0.1f};
    std::vector<std::vector<float>> stems(4);
    for (int st = 0; st < 4; ++st) stems[st].assign(static_cast<size_t>(kRate) * 20 * 2, levels[st]);
    auto* bundle = ps::StemBundle::fromFloat(stems);
    ps::Engine engine(kRate);
    ps::Command install{ps::CommandType::InstallStems};
    install.stems = bundle;
    engine.post(install);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 4);
    CHECK(engine.mixer().takeChannelPeak(3) > 0.05f);
    engine.post({ps::CommandType::ArmLaunch, -1.0f, 1 << ps::kSlotRemix, 16});
    renderToStep(engine, now, 16);
    CHECK(engine.mixer().takeChannelPeak(3) > 0.05f);
    renderToStep(engine, now, 18);
    engine.mixer().takeChannelPeak(3);
    renderToStep(engine, now, 30);
    CHECK(engine.mixer().takeChannelPeak(3) < 1e-6f);
    const int64_t at = engine.stemPosition();
    CHECK(at > kLiveStep * 29);  // still running with the transport
    engine.post({ps::CommandType::ArmLaunch, 0.0f, 1 << ps::kSlotRemix, 16});
    renderToStep(engine, now, 34);
    CHECK(engine.mixer().takeChannelPeak(3) > 0.05f && engine.slotTag(ps::kSlotRemix) == 0);
    engine.post({ps::CommandType::SetTransport, 0.0f});
    engine.post({ps::CommandType::InstallStems});
    render(engine, 512, 256);
    engine.collectGarbage();
}

// Launching, stopping and whole rows allocate nothing on the audio thread; a full event queue
// (the app not reading) loses events, never the state.
void testLiveLaunchesAllocateNothing() {
    ps::Engine engine(kRate);
    for (int st = 0; st < 16; st += 4) engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
    // A recorded performance's curves: the DJ filter and a strip's chopper, a point per beat.
    engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Live, ps::Mixer::kMaster, 0, ps::PerformanceFx::Filter)});
    engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 1, ps::Automation::pack(ps::Automation::Macro, 0, 0, ps::Mixer::kChopMacro)});
    for (int b = 0; b < 600; ++b) {
        engine.post({ps::CommandType::SetAutomationSong, 0.5f + 0.4f * std::sin(b * 0.3f), 0, b});
        engine.post({ps::CommandType::SetAutomationSong, (b / 3) % 2 ? 1.0f : 0.0f, 1, b});
    }
    engine.post({ps::CommandType::SetAutomationMode, 600.0f, 0, ps::Automation::Song | ps::Automation::kPerBeat});
    engine.post({ps::CommandType::SetAutomationMode, 600.0f, 1, ps::Automation::Song | ps::Automation::kPerBeat | ps::Automation::kHeld});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
    std::vector<float> out(2048 * 2);
    for (int i = 0; i < 20; ++i) engine.process(nullptr, 0, 0, out.data(), 2048, 2);
#if defined(PS_COUNT_ALLOCATIONS)
    g_allocations = 0;
    g_countAllocations = true;
#endif
    // 1600 blocks of 2048 frames: 34 bars at 120 BPM; each bar something new is armed (over 300 launches).
    for (int i = 0; i < 1600; ++i) {
        if (i % 47 == 0) {
            const int what = (i / 47) % 4;
            if (what == 0) engine.post({ps::CommandType::ArmLaunch, 1.0f, ps::kAllSlots, 16});
            else if (what == 1) engine.post({ps::CommandType::ArmLaunch, -1.0f, ps::kAllSlots, 16});
            else if (what == 2) engine.post({ps::CommandType::ArmLaunch, 2.0f, (1 << ps::kSlotAcid) | (1 << ps::kSlotRemix), 16});
            else engine.post({ps::CommandType::ArmLaunch, 3.0f, ps::kAllSlots, 16});
            if (what == 1) engine.post({ps::CommandType::MarkSong, float(i), 16});
        }
        engine.process(nullptr, 0, 0, out.data(), 2048, 2);
    }
#if defined(PS_COUNT_ALLOCATIONS)
    g_countAllocations = false;
    std::printf("  allocations during live launches: %d\n", g_allocations.load());
    CHECK(g_allocations.load() == 0);
#endif
    // Never read: the queue is full (more than 256 slot launches happened), the rest dropped.
    ps::LaunchEvent e;
    int events = 0;
    while (engine.popLaunchEvent(e)) ++events;
    std::printf("  launch events kept: %d of more\n", events);
    CHECK(events == ps::Engine::kLaunchEvents);
    // The state is still right, and new events come in again.
    const int tag = engine.slotTag(ps::kSlotDrums);
    CHECK(tag == 1 || tag == -1 || tag == 3);
    engine.post({ps::CommandType::ArmLaunch, 6.0f, 1 << ps::kSlotDrums, 16});
    render(engine, kLiveStep * 17, 256);
    CHECK(engine.popLaunchEvent(e) && e.tag == 6 && engine.slotTag(ps::kSlotDrums) == 6);
    CHECK(!engine.hasNonFinite());
}


// The live grid's reverb and echo returns: untouched (1), the mix is the plain one sample for
// sample; at 0 the effect's sound is gone (the dry mix as with no send), at 2 it is louder.
void testLiveBusReturn() {
    auto make = [](float send, float ret) {
        auto engine = std::make_unique<ps::Engine>(kRate);
        for (int st = 0; st < 16; st += 4) engine->post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
        engine->post({ps::CommandType::SetSend, send, 0, 0});
        engine->post({ps::CommandType::SetMaster, 0.0f, 1});           // no limiter: the sums stay linear
        engine->post({ps::CommandType::SetChannelVolume, 0.3f, 0});
        if (ret >= 0.0f) engine->post({ps::CommandType::SetBusReturn, ret, 0});
        engine->post({ps::CommandType::SetTransport, 1.0f, 0});
        return render(*engine, kRate, 256);
    };
    const auto plain = make(0.8f, -1.0f);
    const auto same = make(0.8f, 1.0f);
    CHECK(plain == same);
    const auto dry = make(0.0f, -1.0f);
    const auto off = make(0.8f, 0.0f);
    const auto loud = make(0.8f, 2.0f);
    float diffOff = 0.0f, wetPlain = 0.0f, wetLoud = 0.0f;
    for (size_t i = size_t(kRate / 2) * kChannels; i < plain.size(); ++i) {
        diffOff = std::max(diffOff, std::fabs(off[i] - dry[i]));
        wetPlain = std::max(wetPlain, std::fabs(plain[i] - dry[i]));
        wetLoud = std::max(wetLoud, std::fabs(loud[i] - dry[i]));
        CHECK(std::isfinite(loud[i]));
    }
    std::printf("  bus return: off %.6f, plain wet %.4f, loud wet %.4f\n", diffOff, wetPlain, wetLoud);
    CHECK(diffOff < 1e-4f && wetPlain > 0.01f && wetLoud > 1.5f * wetPlain);
}

// --- Recorded performance (« Enregistrer la performance ») -----------------------------------

// The waiting scene's acid line: a note every 3 steps over `length` steps.
void stageAcid(ps::Engine& engine, int length, int note) {
    engine.post({ps::CommandType::SetStaging, 1.0f});
    for (int i = 0; i < ps::AcidTrack::kMaxSteps; ++i) {
        const bool gate = i < length && i % 3 == 0;
        engine.post({ps::CommandType::SetAcidStep, 0.0f, i, ps::AcidStep{(note + i) % 12, 0, i % 6 == 0, false, gate}.pack()});
    }
    engine.post({ps::CommandType::SetAcidLength, 0.0f, length});
    engine.post({ps::CommandType::SetStaging, 0.0f});
}

// Scene A: four kicks in 16 steps and a 16-step acid line.
void sceneA(ps::Engine& engine) {
    for (int st = 0; st < 16; st += 4) engine.post({ps::CommandType::SetDrumHit, 1.0f, 0, st});
    engine.post({ps::CommandType::SetPatternSteps, 0.0f, 16});
    for (int i = 0; i < 16; i += 2) engine.post({ps::CommandType::SetAcidStep, 0.0f, i, ps::AcidStep{i % 7, 0, false, false, true}.pack()});
    engine.post({ps::CommandType::SetAcidLength, 0.0f, 16});
}

// The song written from a live take sounds like the take, sample for sample (tonal drums only:
// the noise generators are shared by every engine of the process): the acid launched
// alone on B (a 48-step line) at bar 2, the drums on C at bar 4 while the acid is two bars into
// its line, and the whole-mix chopper held from beat 10 to beat 14. Played as song parts (each
// track's scene, the acid carrying on with SetSlotOffset, the chopper as a held per-beat song
// curve) and rendered offline, it is the same audio.
void testPerformanceSongMatchesTheTake() {
    const int32_t chop = ps::Automation::pack(ps::Automation::Live, ps::Mixer::kMaster, 0, ps::PerformanceFx::Chop);
    // The take (launches on the grid, a held button) or the song written from it.
    auto record = [&](bool asSong) {
        auto e = std::make_unique<ps::Engine>(kRate);
        sceneA(*e);
        std::vector<float> out;
        int64_t at = 0;
        auto to = [&](int step) {
            const int64_t end = static_cast<int64_t>(step) * kLiveStep;
            if (end > at) {
                auto part = render(*e, static_cast<int>(end - at), 256);
                out.insert(out.end(), part.begin(), part.end());
                at = end;
            }
        };
        if (!asSong) {
            e->post({ps::CommandType::MarkSong, 0.0f, 16});
            e->post({ps::CommandType::SetTransport, 1.0f, 0});
            to(5);
            stageAcid(*e, 48, 3);
            e->post({ps::CommandType::ArmLaunch, 1.0f, 1 << ps::kSlotAcid, 16});
            to(37);
            stageDrums(*e, {{7, 0}, {7, 6}, {8, 20}}, 32);
            e->post({ps::CommandType::ArmLaunch, 2.0f, 1 << ps::kSlotDrums, 16});
            to(40);
            e->post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::Chop});
            to(56);
            e->post({ps::CommandType::SetPerformance, 0.0f, ps::PerformanceFx::Chop});
            to(96);
            return out;
        }
        // The written song: part 1 = A (16 steps); part 2 = A with the acid on B (32 steps);
        // part 3 = A with the drums on C and the acid on B two bars in (48 steps).
        e->post({ps::CommandType::SetAutomationTarget, 0.0f, 0, chop});
        // As the app sends it: runs of equal values (beats 0-9 off, 10-13 on, 14-23 off).
        e->post({ps::CommandType::SetAutomationSongRun, 0.0f, 0, 0 | (10 << 16)});
        e->post({ps::CommandType::SetAutomationSongRun, 1.0f, 0, 10 | (4 << 16)});
        e->post({ps::CommandType::SetAutomationSongRun, 0.0f, 0, 14 | (10 << 16)});
        e->post({ps::CommandType::SetAutomationMode, 24.0f, 0, ps::Automation::Song | ps::Automation::kPerBeat | ps::Automation::kHeld});
        e->post({ps::CommandType::SetTransport, 1.0f, 0, 0});
        to(5);
        // Part 2 staged: the drums of A again, the acid of B.
        stageDrums(*e, {{0, 0}, {0, 4}, {0, 8}, {0, 12}}, 16);
        stageAcid(*e, 48, 3);
        e->post({ps::CommandType::SetStaging, 1.0f});  // the offsets go with the staged part
        for (int k = 0; k < ps::kSlots; ++k) e->post({ps::CommandType::SetSlotOffset, 0.0f, k, 0});
        e->post({ps::CommandType::SetStaging, 0.0f});
        e->post({ps::CommandType::ArmSceneSwap, 16.0f, 16, 1});  // after 16 steps; the song is at step 16 there
        to(20);
        // Part 3: the drums of C, the acid of B carrying on (32 steps played).
        stageDrums(*e, {{7, 0}, {7, 6}, {8, 20}}, 32);
        stageAcid(*e, 48, 3);
        e->post({ps::CommandType::SetStaging, 1.0f});
        for (int k = 0; k < ps::kSlots; ++k) e->post({ps::CommandType::SetSlotOffset, 0.0f, k, k == ps::kSlotAcid ? 32 : 0});
        e->post({ps::CommandType::SetStaging, 0.0f});
        // The part playing sent again meanwhile (the app's syncEngine): its offsets never reach the swap staged.
        for (int k = 0; k < ps::kSlots; ++k) e->post({ps::CommandType::SetSlotOffset, 0.0f, k, 0});
        e->post({ps::CommandType::ArmSceneSwap, 48.0f, 32, 1});
        to(96);
        return out;
    };
    const auto takeOut = record(false);
    const auto songOut = record(true);
    bool same = takeOut.size() == songOut.size();
    float diff = 0.0f, peak = 0.0f;
    for (size_t i = 0; same && i < takeOut.size(); ++i) {
        diff = std::max(diff, std::fabs(takeOut[i] - songOut[i]));
        peak = std::max(peak, std::fabs(takeOut[i]));
    }
    size_t first = 0;
    while (first < std::min(takeOut.size(), songOut.size()) && takeOut[first] == songOut[first]) ++first;
    std::printf("  performance written as a song: max difference %.3g (peak %.3f), first at step %.2f\n", diff, peak,
                double(first / kChannels) / kLiveStep);
    CHECK(same && diff == 0.0f && peak > 0.05f);
}

// A recorded performance's start: the song position becomes the one given on the quantum.
void testMarkSongSetsTheSongPosition() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0, -1});  // a scene on its own: no song position
    int64_t now = 0;
    renderToStep(engine, now, 5, 10);
    CHECK(engine.songStep() == -1 && engine.songMarkStep() == -1);
    engine.post({ps::CommandType::MarkSong, 64.0f, 16});
    renderToStep(engine, now, 16);
    CHECK(engine.songStep() == -1);
    renderToStep(engine, now, 16, 10);
    CHECK(engine.songMarkStep() == 16 && engine.songStep() == 64);
    renderToStep(engine, now, 20, 10);
    CHECK(engine.songStep() == 68);
    const double pos = engine.transportPosition();
    std::printf("  mark: song step %d at transport %d (position %.2f)\n", engine.songStep(), engine.transportStep(), pos);
    CHECK(pos >= 20.0 && pos < 21.0);
    // Cancelled before it is made: nothing changes.
    engine.post({ps::CommandType::MarkSong, 0.0f, 16});
    engine.post({ps::CommandType::MarkSong, 0.0f, 0});
    renderToStep(engine, now, 32, 10);
    CHECK(engine.songMarkStep() == 16 && engine.songStep() == 80);
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 256, 256);
    CHECK(engine.songMarkStep() == 16 && engine.transportPosition() < 0.0);
}

// Song curves with a point per beat: they slide beat by beat, or hold each point (on/off).
void testPerBeatSongCurves() {
    auto level = [](bool held) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 64 * 24, 60)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
        engine.post({ps::CommandType::SetSynthSteps, 0.0f, 64});
        engine.post({ps::CommandType::SetMaster, 0.0f, 1});
        engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 3, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
        // Beat 0: 0.8, beat 1: 0 (a point a beat).
        engine.post({ps::CommandType::SetAutomationSong, 0.8f, 3, 0});
        engine.post({ps::CommandType::SetAutomationSong, 0.0f, 3, 1});
        engine.post({ps::CommandType::SetAutomationMode, 2.0f, 3, ps::Automation::Song | ps::Automation::kPerBeat | (held ? ps::Automation::kHeld : 0)});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
        int64_t now = 0;
        renderToStep(engine, now, 2, 2000);  // half-way through the first beat
        engine.mixer().takeChannelPeak(2);
        render(engine, 3000, 250);
        return engine.mixer().takeChannelPeak(2);
    };
    const float slid = level(false), held = level(true);
    std::printf("  per-beat curve, half a beat in: sliding %.4f, held %.4f\n", slid, held);
    CHECK(held > 1.3f * slid && slid > 0.0f);
}

// A long set's per-beat curves: points past the old 1024 (256 bars) are heard (audit 28 live G2).
void testPerBeatSongCurvesOfALongSet() {
    auto level = [](int songStep) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 64 * 24, 60)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
        engine.post({ps::CommandType::SetSynthSteps, 0.0f, 64});
        engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 3, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
        // Loud up to beat 3000, silent from beat 3001 (a beat each, held).
        engine.post({ps::CommandType::SetAutomationSongRun, 0.8f, 3, 0 | (3001 << 16)});
        engine.post({ps::CommandType::SetAutomationSong, 0.0f, 3, 3001});
        engine.post({ps::CommandType::SetAutomationMode, 3010.0f, 3, ps::Automation::Song | ps::Automation::kPerBeat | ps::Automation::kHeld});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0, songStep});
        int64_t now = 0;
        renderToStep(engine, now, 1, 3000);
        engine.mixer().takeChannelPeak(2);
        render(engine, 3000, 250);
        return engine.mixer().takeChannelPeak(2);
    };
    const float before = level(3000 * 4), after = level(3001 * 4);
    std::printf("  per-beat curve at beat 3000: %.4f, at beat 3001: %.4f\n", before, after);
    CHECK(before > 0.05f && after < 0.01f);
    CHECK(ps::Automation::kSongPoints >= 4096);
}

// The Live page's « Hachoir de voie »: a strip chopped in sixteenths, then plain again.
void testTrackChopper() {
    auto run = [](bool chopped) {
        auto engine = std::make_unique<ps::Engine>(kRate);
        engine->post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 64 * 24, 57)});
        engine->post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
        engine->post({ps::CommandType::SetSynthSteps, 0.0f, 64});
        if (chopped) engine->post({ps::CommandType::SetMacro, 1.0f, 2, ps::Mixer::kChopMacro});
        engine->post({ps::CommandType::SetTransport, 1.0f, 0});
        auto out = render(*engine, kLiveStep * 8, 256);
        CHECK(engine->mixer().chopOn(2) == chopped);
        engine->post({ps::CommandType::SetMacro, 0.0f, 2, ps::Mixer::kChopMacro});
        auto after = render(*engine, kLiveStep * 8, 256);
        out.insert(out.end(), after.begin(), after.end());
        return out;
    };
    const auto plain = run(false), chopped = run(true);
    // In each sixteenth the chopper lets every other one through: the quiet ones are much lower.
    auto rms = [](const std::vector<float>& v, size_t from, size_t to) {
        double sum = 0;
        for (size_t i = from; i < to; ++i) sum += double(v[i]) * v[i];
        return std::sqrt(sum / double(to - from));
    };
    const size_t step = size_t(kLiveStep) * kChannels;
    const double on = rms(chopped, 4 * step + step / 4, 5 * step - step / 4);
    const double off = rms(chopped, 5 * step + step / 4, 6 * step - step / 4);
    const double ref = rms(plain, 5 * step + step / 4, 6 * step - step / 4);
    const double back = rms(chopped, 14 * step, 15 * step), backRef = rms(plain, 14 * step, 15 * step);
    std::printf("  track chopper: on %.4f, off %.4f (plain %.4f), let go %.4f / %.4f\n", on, off, ref, back, backRef);
    CHECK(off < 0.05 * ref && on > 0.5 * ref);
    CHECK(std::fabs(back - backRef) < 0.02 * backRef);
}


// --- Phase 27 review fixes ----------------------------------------------------------------------

// SetSlotOffset follows SetStaging: the part playing sent again (non-staged) never replaces the
// offsets staged for the next song part; a non-staged offset is the transport's start. ArmLaunch
// ends a staging burst (its "off" may be lost). Huge values are bounded.
void testSlotOffsetFollowsStaging() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 5);
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetSlotOffset, 0.0f, ps::kSlotAcid, 32});  // the next part carries the acid on
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::SetSlotOffset, 0.0f, ps::kSlotAcid, 0});   // the part playing, sent again
    engine.post({ps::CommandType::ArmSceneSwap, 16.0f, 16, 1});
    renderToStep(engine, now, 16, 10);
    std::printf("  staged offset: acid starts at %d after the swap on 16\n", engine.slotStart(ps::kSlotAcid));
    CHECK(engine.sceneSwaps() == 1 && engine.slotStart(ps::kSlotAcid) == 16 - 32);
    // A launch armed while staging, its "staging off" dropped: what follows is no longer staged.
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::ArmLaunch, 1.0f, 1 << ps::kSlotDrums, 16});
    engine.post({ps::CommandType::SetSlotOffset, 0.0f, ps::kSlotDrums, 32});
    renderToStep(engine, now, 32, 10);
    CHECK(engine.slotTag(ps::kSlotDrums) == 1 && engine.slotStart(ps::kSlotDrums) == 32);
    // Not staged, stopped: the transport's start takes it (a song played from a part that carries on).
    engine.post({ps::CommandType::SetTransport, 0.0f});
    engine.post({ps::CommandType::SetSlotOffset, 0.0f, ps::kSlotDrums, 16});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    now = 0;
    renderToStep(engine, now, 0, 10);
    CHECK(engine.slotStart(ps::kSlotDrums) == -16 && engine.slotStart(ps::kSlotAcid) == 0);
    // Bounded: an absurd offset or song step neither overflows nor converts out of range.
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetSlotOffset, 0.0f, ps::kSlotAcid, std::numeric_limits<int32_t>::max()});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmLaunch, 2.0f, 1 << ps::kSlotAcid, 16});
    engine.post({ps::CommandType::MarkSong, 1e30f, 16});
    engine.post({ps::CommandType::SetAutomationMode, 1e30f, 0, ps::Automation::Song});
    engine.post({ps::CommandType::SetAutomationSong, std::numeric_limits<float>::quiet_NaN(), 0, 3});
    engine.post({ps::CommandType::SetAutomationPoint, std::numeric_limits<float>::quiet_NaN(), 0, 3});
    renderToStep(engine, now, 20, 10);
    std::printf("  bounded: acid start %d, song step %d\n", engine.slotStart(ps::kSlotAcid), engine.songStep());
    CHECK(engine.slotStart(ps::kSlotAcid) == 16 - ps::Engine::kMaxSongStep);
    CHECK(engine.songStep() == ps::Engine::kMaxSongStep + 4);
    CHECK(!engine.hasNonFinite());
}

// A song part that mutes the remix: silent from its swap on (keeping its place), heard again with
// the next part; sent at once (a song played from that part), silent from the first step.
void testSongPartMutesTheRemix() {
    static_assert(static_cast<int>(ps::CommandType::SetRemixOn) == 77 && static_cast<int>(ps::CommandType::SetAutomationSongRamp) == 78 &&
                      static_cast<int>(ps::CommandType::SetAutomationSongFlat) == 79,
                  "NativeEngine.kt's CMD_REMIX_ON and CMD_AUTOMATION_SONG_RAMP");
    std::vector<std::vector<float>> stems(4);
    for (int st = 0; st < 4; ++st) stems[st].assign(static_cast<size_t>(kRate) * 20 * 2, 0.1f);
    auto* bundle = ps::StemBundle::fromFloat(stems);
    ps::Engine engine(kRate);
    ps::Command install{ps::CommandType::InstallStems};
    install.stems = bundle;
    engine.post(install);
    engine.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
    int64_t now = 0;
    renderToStep(engine, now, 4);
    CHECK(engine.mixer().takeChannelPeak(3) > 0.05f);
    engine.post({ps::CommandType::SetStaging, 1.0f});
    engine.post({ps::CommandType::SetRemixOn, 0.0f});
    engine.post({ps::CommandType::SetStaging, 0.0f});
    engine.post({ps::CommandType::ArmSceneSwap, 16.0f, 16, 1});
    renderToStep(engine, now, 18);
    engine.mixer().takeChannelPeak(3);
    renderToStep(engine, now, 30);
    const float muted = engine.mixer().takeChannelPeak(3);
    CHECK(engine.stemPosition() > kLiveStep * 29);  // still running with the transport
    // The next part does not mute it: back in time.
    engine.post({ps::CommandType::ArmSceneSwap, 32.0f, 16, 1});
    renderToStep(engine, now, 36);
    const float back = engine.mixer().takeChannelPeak(3);
    std::printf("  remix muted by a song part: %.6f, next part %.4f\n", muted, back);
    CHECK(muted < 1e-6f && back > 0.05f);
    // Played from a part that mutes it: silent from the first step; Stop brings it back.
    engine.post({ps::CommandType::SetTransport, 0.0f});
    render(engine, 512, 256);
    engine.post({ps::CommandType::SetRemixOn, 0.0f});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
    now = 0;
    renderToStep(engine, now, 2);
    engine.mixer().takeChannelPeak(3);
    renderToStep(engine, now, 8);
    CHECK(engine.mixer().takeChannelPeak(3) < 1e-6f);
    engine.post({ps::CommandType::SetTransport, 0.0f});
    engine.post({ps::CommandType::InstallStems});
    render(engine, 512, 256);
    engine.collectGarbage();
}

// A slide of a song curve in one command plays exactly as its points sent one by one.
void testSongCurveRamp() {
    auto run = [](bool ramp) {
        ps::Engine engine(kRate);
        engine.post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 64 * 24, 60)});
        engine.post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
        engine.post({ps::CommandType::SetSynthSteps, 0.0f, 64});
        engine.post({ps::CommandType::SetAutomationTarget, 0.0f, 2, ps::Automation::pack(ps::Automation::Volume, 2, 0, 0)});
        const float from = 0.8f, to = 0.2f;  // both exact in sixteen bits (52428 and 13107 / 65535)
        if (ramp) {
            const int32_t end = static_cast<int32_t>(std::lround(to * 65535.0f));
            engine.post({ps::CommandType::SetAutomationSongRamp, from, 2 | (end << 8), 0 | (8 << 16)});
        } else {
            for (int i = 0; i < 8; ++i) engine.post({ps::CommandType::SetAutomationSong, from + (to - from) * static_cast<float>(i) / 8.0f, 2, i});
        }
        engine.post({ps::CommandType::SetAutomationSongRun, to, 2, 8 | (4 << 16)});
        engine.post({ps::CommandType::SetAutomationMode, 12.0f, 2, ps::Automation::Song | ps::Automation::kPerBeat});
        engine.post({ps::CommandType::SetTransport, 1.0f, 0, 0});
        return render(engine, kLiveStep * 48, 256);
    };
    const auto points = run(false), ramp = run(true);
    float peak = 0.0f;
    for (float v : points) peak = std::max(peak, std::fabs(v));
    std::printf("  song ramp: same as its points %s (peak %.3f)\n", points == ramp ? "yes" : "no", peak);
    CHECK(points == ramp && peak > 0.01f);
}

// « Hachoir de voie » switched on between two beats starts in phase with the beat (as it does
// from a song curve on a beat): on step 5 (a beat's second sixteenth) it cuts, as "x.x." says.
void testTrackChopperStartsInPhase() {
    auto run = [](int onAt) {
        auto engine = std::make_unique<ps::Engine>(kRate);
        engine->post({ps::CommandType::SetSynthNote, 0.9f, 0, ps::NoteTrack::pack(0, 64 * 24, 57)});
        engine->post({ps::CommandType::SetSynthNoteCount, 0.0f, 1});
        engine->post({ps::CommandType::SetSynthSteps, 0.0f, 64});
        engine->post({ps::CommandType::SetTransport, 1.0f, 0});
        int64_t now = 0;
        renderToStep(*engine, now, onAt);
        engine->post({ps::CommandType::SetMacro, 1.0f, 2, ps::Mixer::kChopMacro});
        return render(*engine, kLiveStep * (8 - onAt), 256);
    };
    auto rms = [](const std::vector<float>& v, size_t from, size_t to) {
        double sum = 0;
        for (size_t i = from; i < to; ++i) sum += double(v[i]) * v[i];
        return std::sqrt(sum / double(to - from));
    };
    const size_t step = size_t(kLiveStep) * kChannels;
    const auto late = run(5);  // starts on step 5
    const double cut = rms(late, step / 4, step - step / 4);           // step 5: '.'
    const double open = rms(late, step + step / 4, 2 * step - step / 4);  // step 6: 'x'
    std::printf("  chopper on mid-beat: step 5 %.4f (cut), step 6 %.4f (open)\n", cut, open);
    CHECK(cut < 0.1 * open && open > 0.01);
    ps::Automation a;
    for (int i = 0; i < 24; ++i) a.hold(ps::Automation::pack(ps::Automation::Macro, i % 16, 0, i / 16), true);
    bool all = true;
    for (int i = 0; i < 24; ++i) all &= a.isHeld(ps::Automation::pack(ps::Automation::Macro, i % 16, 0, i / 16));
    CHECK(all);  // the Live page's controls and a take's settings held at once
}

// --- Live performance effects (phase 27 effects research: the sound side) ---------------------

// The Live page's PerformanceFx with a transport: a step every kLiveStep frames from frame 0 (step
// 0), a block ending on every step and on every frame of `commands` (run before the block that
// starts there), at most `block` frames. Mono in, both sides; returns the left side.
std::vector<float> runLive(ps::PerformanceFx& fx, const std::vector<float>& in, const std::vector<std::pair<int, std::function<void()>>>& commands,
                           int block = 256, bool clock = true, int stepsPerBar = 16) {
    std::vector<float> l(in), r(in);
    fx.setStepFrames(kLiveStep);
    fx.setClock(clock);
    size_t next = 0;
    for (int pos = 0; pos < static_cast<int>(in.size());) {
        while (next < commands.size() && commands[next].first <= pos) commands[next++].second();
        int end = std::min<int>(static_cast<int>(in.size()), pos + block);
        const int nextStep = (pos / kLiveStep + 1) * kLiveStep;
        end = std::min(end, nextStep);
        if (next < commands.size()) end = std::min(end, commands[next].first);
        if (clock && pos % kLiveStep == 0) fx.onStep(0, pos / kLiveStep, stepsPerBar);
        fx.process(l.data() + pos, r.data() + pos, end - pos);
        pos = end;
    }
    return l;
}

// A rising ramp (each frame its own value): easy to see which frame is played.
std::vector<float> rampInput(int frames) {
    std::vector<float> v(static_cast<size_t>(frames));
    for (int i = 0; i < frames; ++i) v[static_cast<size_t>(i)] = 0.5f * static_cast<float>(i % 100000) / 100000.0f;
    return v;
}

std::vector<float> sineInput(int frames, double hz, float amp = 0.5f) {
    std::vector<float> v(static_cast<size_t>(frames));
    for (int i = 0; i < frames; ++i) v[static_cast<size_t>(i)] = amp * static_cast<float>(std::sin(2 * M_PI * hz * i / kRate));
    return v;
}

double rmsOf(const std::vector<float>& v, int from, int to) {
    double s = 0.0;
    for (int i = from; i < to; ++i) s += double(v[size_t(i)]) * v[size_t(i)];
    return std::sqrt(s / std::max(1, to - from));
}

bool finiteAndUnclipped(const std::vector<float>& v, float limit = 1.0f) {
    for (float x : v) {
        if (!std::isfinite(x) || std::fabs(x) > limit) return false;
    }
    return true;
}

int firstDifference(const std::vector<float>& a, const std::vector<float>& b, int from = 0) {
    for (int i = from; i < static_cast<int>(std::min(a.size(), b.size())); ++i) {
        if (a[size_t(i)] != b[size_t(i)]) return i;
    }
    return -1;
}

constexpr float kGridSixteenth = 0.5f, kGridBeat = 0.9f, kGridOff = 0.0f;

// A held effect starts on the next line of the grid, sample-exact: a reverse pressed in the middle
// of step 2 starts on step 3 (1/16 grid) or on step 4 (beat grid); without a transport, at once.
void testLiveFxStartOnTheGrid() {
    const auto in = rampInput(kLiveStep * 8);
    for (const auto& [grid, line] : {std::pair<float, int>{kGridSixteenth, 3}, {kGridBeat, 4}}) {
        ps::PerformanceFx fx;
        fx.prepare(kRate);
        fx.setParam(ps::PerformanceFx::Grid, grid);
        const auto out = runLive(fx, in, {{kLiveStep * 2 + 2500, [&] { fx.setParam(ps::PerformanceFx::Reverse, 1.0f); }}});
        const int first = firstDifference(in, out);
        std::printf("  reverse pressed at step 2.4, grid %s: starts at frame %d (line %d)\n", grid < 0.7f ? "1/16" : "beat", first, line * kLiveStep);
        CHECK(first == line * kLiveStep);
        CHECK(finiteAndUnclipped(out));
    }
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    fx.setParam(ps::PerformanceFx::Grid, kGridBeat);
    const auto out = runLive(fx, in, {{kLiveStep * 2 + 2500, [&] { fx.setParam(ps::PerformanceFx::Reverse, 1.0f); }}}, 256, false);
    CHECK(firstDifference(in, out) == kLiveStep * 2 + 2500);  // stopped: at once
}

// Repeat locked to the grid: the slice starts on its division's line, not where the finger landed.
void testLiveRepeatOnTheGrid() {
    const auto in = rampInput(kLiveStep * 12);
    auto run = [&](float division, int pressAt, float grid) {
        ps::PerformanceFx fx;
        fx.prepare(kRate);
        fx.setParam(ps::PerformanceFx::Grid, grid);
        return runLive(fx, in, {{pressAt, [&fx, division] {
                                     fx.setParam(ps::PerformanceFx::RepeatDivision, division);
                                     fx.setParam(ps::PerformanceFx::Repeat, 1.0f);
                                 }}});
    };
    // 1/8 (two steps) pressed in step 2 with the 1/16 grid: on from step 3, the slice is steps 2-3,
    // heard live up to step 4, then again and again.
    auto out = run(0.4375f, kLiveStep * 2 + 3000, kGridSixteenth);
    const int s = kLiveStep;
    CHECK(out[size_t(3 * s + 500)] == in[size_t(3 * s + 500)]);  // the slice's first pass: live
    CHECK(std::fabs(out[size_t(4 * s + 500)] - in[size_t(2 * s + 500)]) < 1e-6f);
    CHECK(std::fabs(out[size_t(7 * s + 500)] - in[size_t(3 * s + 500)]) < 1e-6f);
    CHECK(std::fabs(out[size_t(10 * s + 500)] - in[size_t(2 * s + 500)]) < 1e-6f);
    // 1/8 triplet (8000 frames, three to a beat) pressed in step 5: on from step 6; its last line
    // is 32000 (a beat at 24000, then every 8000), so the slice is 32000-40000.
    out = run(0.3f, kLiveStep * 5 + 1000, kGridSixteenth);
    CHECK(out[size_t(38000)] == in[size_t(38000)]);
    CHECK(std::fabs(out[size_t(40500)] - in[size_t(32500)]) < 1e-6f);
    CHECK(std::fabs(out[size_t(52500)] - in[size_t(36500)]) < 1e-6f);
    // 1/32: half a step.
    out = run(0.95f, kLiveStep * 2 + 1000, kGridSixteenth);
    CHECK(std::fabs(out[size_t(3 * s + 3500)] - in[size_t(3 * s + 500)]) < 1e-6f);
    // Without the grid: the division that just played, from the press (as before).
    out = run(0.4375f, kLiveStep * 2 + 3000, kGridOff);
    CHECK(std::fabs(out[size_t(2 * s + 3000 + 1000)] - in[size_t(3000 + 1000)]) < 1e-6f);
    std::printf("  grid-locked repeat: 1/8, 1/8T and 1/32 slices on their lines\n");
    CHECK(finiteAndUnclipped(out));
    CHECK(ps::PerformanceFx::divisionSteps(0.125f) == 4.0 && ps::PerformanceFx::divisionSteps(0.375f) == 2.0 &&
          ps::PerformanceFx::divisionSteps(0.5f) == 1.0 && ps::PerformanceFx::divisionSteps(0.875f) == 0.5);  // older values
}

// The brake lasts its length (a beat here), stays silent, and restarts like a tape: back on the
// live sound exactly, without a jump.
void testLiveBrakeAndRestart() {
    const auto in = sineInput(kLiveStep * 18, 220.0);
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    fx.setParam(ps::PerformanceFx::BrakeLength, 2.5f / 6.0f);   // 1 beat = 24000 frames
    fx.setParam(ps::PerformanceFx::BrakeRestart, 3.5f / 4.0f);  // restart over a beat
    fx.setParam(ps::PerformanceFx::Grid, kGridBeat);
    const int s = kLiveStep;
    const auto out = runLive(fx, in, {{s * 1 + 100, [&] { fx.setParam(ps::PerformanceFx::TapeStop, 1.0f); }},
                                      {s * 9 + 100, [&] { fx.setParam(ps::PerformanceFx::TapeStop, 0.0f); }}});
    const int brakeFrom = firstDifference(in, out);  // on the next beat (its first frame is still the live one)
    CHECK(brakeFrom >= 4 * s && brakeFrom < 4 * s + 50);
    CHECK(rmsOf(out, 8 * s + 200, 9 * s) < 1e-4);     // stopped after its beat
    CHECK(rmsOf(out, 9 * s + 200, 12 * s) < 1e-4);    // let go: silent until the next beat line
    CHECK(rmsOf(out, 12 * s + 6000, 16 * s) > 0.2);   // restarting from step 12
    CHECK(firstDifference(in, out, 16 * s + 10) < 0);  // and on the music exactly a beat later
    float jump = 0.0f;
    for (int i = 12 * s; i < 16 * s + 100; ++i) jump = std::max(jump, std::fabs(out[size_t(i)] - out[size_t(i - 1)]));
    std::printf("  brake: largest step during the restart %.4f (the tone's own %.4f)\n", jump, 0.5f * naturalStep(220.0f));
    CHECK(jump < 4.0f * 0.5f * naturalStep(220.0f));
    CHECK(finiteAndUnclipped(out));
}

// Backspin, reverse and half time read the last seconds the right way.
void testLiveBackspinReverseHalfTime() {
    const int s = kLiveStep;
    const auto ramp = rampInput(s * 16);
    {
        // Reverse by the beat (grid beat), pressed in step 3: from step 4, each beat plays the beat
        // before backwards.
        ps::PerformanceFx fx;
        fx.prepare(kRate);
        fx.setParam(ps::PerformanceFx::Grid, kGridBeat);
        fx.setParam(ps::PerformanceFx::ReverseLength, 0.9f);  // a beat
        const auto out = runLive(fx, ramp, {{3 * s + 10, [&] { fx.setParam(ps::PerformanceFx::Reverse, 1.0f); }}});
        CHECK(std::fabs(out[size_t(4 * s + 3000)] - ramp[size_t(4 * s - 1 - 3000)]) < 1e-6f);
        CHECK(std::fabs(out[size_t(8 * s + 5000)] - ramp[size_t(8 * s - 1 - 5000)]) < 1e-6f);
    }
    {
        // Half time by the bar, pressed before the first bar line: each bar plays its first half.
        ps::PerformanceFx fx;
        fx.prepare(kRate);
        fx.setParam(ps::PerformanceFx::Grid, kGridSixteenth);
        fx.setParam(ps::PerformanceFx::HalfTimeLength, 1.0f);
        const auto out = runLive(fx, ramp, {{0, [&] { fx.setParam(ps::PerformanceFx::HalfTime, 1.0f); }}});
        CHECK(std::fabs(out[size_t(5000)] - ramp[size_t(2500)]) < 1e-6f);
        CHECK(std::fabs(out[size_t(10 * s)] - ramp[size_t(5 * s)]) < 1e-6f);
    }
    {
        // Backspin of a beat: sound while it spins back, then silence while held; let go: the music.
        const auto tone = sineInput(s * 16, 330.0);
        ps::PerformanceFx fx;
        fx.prepare(kRate);
        fx.setParam(ps::PerformanceFx::BackspinLength, 0.3f);  // 1 beat
        fx.setParam(ps::PerformanceFx::Grid, kGridSixteenth);
        const auto out = runLive(fx, tone, {{2 * s + 100, [&] { fx.setParam(ps::PerformanceFx::Backspin, 1.0f); }},
                                            {10 * s, [&] { fx.setParam(ps::PerformanceFx::Backspin, 0.0f); }}});
        const int spinFrom = firstDifference(tone, out);
        CHECK(spinFrom >= 3 * s && spinFrom < 3 * s + 50);
        CHECK(rmsOf(out, 3 * s + 2000, 5 * s) > 0.1);
        CHECK(rmsOf(out, 8 * s, 10 * s) < 1e-4);
        CHECK(firstDifference(tone, out, 10 * s + 600) < 0);  // back on the music after its 10 ms fade
        CHECK(finiteAndUnclipped(out));
    }
}

// Echo out: the dry sound fades out over a beat into the echo, the tail rings on and dies;
// let go, the music comes back.
void testLiveEchoOut() {
    const int s = kLiveStep;
    const auto tone = sineInput(s * 48, 440.0, 0.4f);
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    fx.setParam(ps::PerformanceFx::Grid, kGridBeat);
    std::vector<float> in(tone);
    for (size_t i = size_t(8 * s); i < size_t(40 * s); ++i) in[i] = 0.0f;  // the music stops after the echo starts
    const auto out = runLive(fx, in, {{3 * s, [&] { fx.setParam(ps::PerformanceFx::EchoOut, 1.0f); }},
                                      {40 * s, [&] { fx.setParam(ps::PerformanceFx::EchoOut, 0.0f); }}});
    CHECK(firstDifference(in, out) == 4 * s);
    const double early = rmsOf(out, 9 * s, 13 * s), late = rmsOf(out, 30 * s, 34 * s);
    std::printf("  echo out: tail %.4f a beat after, %.5f five beats later\n", early, late);
    CHECK(early > 0.02 && late < 0.25 * early);
    CHECK(rmsOf(out, 44 * s, 48 * s) > 0.25);  // the music is back
    CHECK(finiteAndUnclipped(out));
}

// Kills: each band cut deeply, the others untouched; let go, the dry sound sample for sample.
void testLiveKills() {
    const int s = kLiveStep;
    auto mixOf = [&](double a, double b) {
        auto x = sineInput(s * 16, a, 0.3f);
        auto y = sineInput(s * 16, b, 0.3f);
        for (size_t i = 0; i < x.size(); ++i) x[i] += y[i];
        return x;
    };
    const auto in = mixOf(60.0, 6000.0);
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    fx.setParam(ps::PerformanceFx::Grid, kGridBeat);
    const auto out = runLive(fx, in, {{1 * s, [&] { fx.setParam(ps::PerformanceFx::KillLow, 1.0f); }},
                                      {5 * s + 10, [&] { fx.setParam(ps::PerformanceFx::KillLow, 0.0f); }}});
    CHECK(firstDifference(in, out) == 4 * s);  // on the beat
    const double low = toneAmplitude(out, 6 * s, 6000, 60.0), high = toneAmplitude(out, 6 * s, 6000, 6000.0);
    std::printf("  bass kill: 60 Hz at %.4f, 6 kHz at %.4f (both 0.3 dry)\n", low, high);
    CHECK(low < 0.02 && high > 0.27);
    CHECK(firstDifference(in, out, 8 * s + 1500) < 0);  // back at rest from the next beat line on
    // The high kill on a strip of the mixer (a track), from its macro.
    ps::Mixer mixer;
    mixer.prepare(kRate);
    mixer.setMacro(2, ps::Mixer::kKillMacro + 2, 1.0f);
    CHECK(mixer.isolator(2).kill(2) && !mixer.isolator(2).kill(0));
    CHECK(finiteAndUnclipped(out));
}

// Riser and downlifter: noise starting on the next bar line, rising for its bars, gone on the bar
// line it ends on; the downlifter falls.
void testLiveRiser() {
    const int s = kLiveStep;
    std::vector<float> silence(size_t(s) * 64, 0.0f);
    for (bool down : {false, true}) {
        ps::PerformanceFx fx;
        fx.prepare(kRate);
        fx.setParam(ps::PerformanceFx::RiserLength, 0.3f);  // 2 bars
        const int param = down ? ps::PerformanceFx::Downlifter : ps::PerformanceFx::Riser;
        const auto out = runLive(fx, silence, {{5 * s + 77, [&] { fx.setParam(param, 1.0f); }}, {6 * s, [&] { fx.setParam(param, 0.0f); }}});
        const int first = firstDifference(silence, out);
        int last = -1;
        for (int i = 0; i < static_cast<int>(out.size()); ++i) if (out[size_t(i)] != 0.0f) last = i;
        const double a = rmsOf(out, 16 * s, 24 * s), b = rmsOf(out, 40 * s, 48 * s);
        std::printf("  %s: from frame %d to %d (bars 16-48 steps), first half %.4f, second %.4f\n", down ? "downlifter" : "riser", first, last, a, b);
        CHECK(down ? first == 16 * s : first >= 16 * s && first < 16 * s + 2000);
        CHECK(last < 48 * s && last > 48 * s - 200);
        CHECK(down ? a > 3 * b : b > 3 * a);
        CHECK(finiteAndUnclipped(out, 0.6f));
    }
}

// The build-up knob: a rising high-pass, sends, and rolls; « Drop » on the next bar line puts
// everything back at once; « Tout remettre » at once.
void testLiveBuildUpAndDrop() {
    const int s = kLiveStep;
    const auto bass = sineInput(s * 48, 50.0, 0.4f);
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    fx.setParam(ps::PerformanceFx::Grid, kGridSixteenth);
    const auto out = runLive(fx, bass, {{2 * s, [&] { fx.setParam(ps::PerformanceFx::BuildUp, 0.5f); }},
                                        {8 * s, [&] { fx.setParam(ps::PerformanceFx::BuildUp, 0.95f); }},
                                        {18 * s + 300, [&] {
                                             CHECK(fx.repeating());
                                             CHECK(fx.buildSend(0) > 0.5f);
                                             fx.setParam(ps::PerformanceFx::Drop, 1.0f);
                                             fx.setParam(ps::PerformanceFx::Drop, 0.0f);
                                         }}});
    const double hp = toneAmplitude(out, 6 * s, 6000, 50.0);
    std::printf("  build-up at half: 50 Hz at %.3f (0.4 dry)\n", hp);
    CHECK(hp < 0.25);
    CHECK(out[size_t(32 * s - 1)] != bass[size_t(32 * s - 1)]);   // up to the bar line: built up
    CHECK(firstDifference(bass, out, 32 * s + 600) < 0);           // from it: the plain music (after the roll's fade)
    CHECK(fx.buildUp() == 0.0f && !fx.repeating());
    // « Tout remettre »: at once.
    ps::PerformanceFx fx2;
    fx2.prepare(kRate);
    const auto out2 = runLive(fx2, bass, {{s, [&] { fx2.setParam(ps::PerformanceFx::KillLow, 1.0f); fx2.setParam(ps::PerformanceFx::Chop, 1.0f); }},
                                          {5 * s + 123, [&] { fx2.setParam(ps::PerformanceFx::ResetAll, 1.0f); }}});
    CHECK(firstDifference(bass, out2, 5 * s + 123 + 1500) < 0);
    CHECK(!fx2.active());
}

// The mixer's part: reverb freeze holds the tail without taking more in; throws open a send for
// one beat from the next beat line; the build-up sends the mix; the drop resets them on the bar.
void testLiveFreezeAndThrows() {
    {
        ps::Reverb rv;
        rv.prepare(kRate);
        std::vector<float> in(kRate, 0.0f), outL(kRate, 0.0f), outR(kRate, 0.0f);
        for (int i = 0; i < 2400; ++i) in[size_t(i)] = 0.5f * std::sin(0.1f * i);
        rv.process(in.data(), in.data(), outL.data(), outR.data(), kRate / 2);
        rv.setFreeze(true);
        std::vector<float> loud(kRate, 0.8f);
        std::vector<float> a(kRate, 0.0f), b(kRate, 0.0f), c(kRate, 0.0f), d(kRate, 0.0f);
        rv.process(loud.data(), loud.data(), a.data(), b.data(), kRate);  // ignored while frozen
        std::fill(c.begin(), c.end(), 0.0f);
        for (int k = 0; k < 5; ++k) {
            std::fill(c.begin(), c.end(), 0.0f);
            rv.process(in.data() + kRate / 2, in.data() + kRate / 2, c.data(), d.data(), kRate / 2);
        }
        const double held = rmsOf(a, kRate / 2, kRate), later = rmsOf(c, 0, kRate / 2);
        std::printf("  reverb freeze: tail %.4f, 3 s later %.4f\n", held, later);
        CHECK(held > 0.005 && later > 0.8 * held && later < 1.2 * held);
        rv.setFreeze(false);
        for (int k = 0; k < 8; ++k) {
            std::fill(c.begin(), c.end(), 0.0f);
            rv.process(in.data() + kRate / 2, in.data() + kRate / 2, c.data(), d.data(), kRate / 2);
        }
        CHECK(rmsOf(c, 0, kRate / 2) < 0.1 * held);  // unfrozen: it dies away
    }
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetPerformance, kGridSixteenth, ps::PerformanceFx::Grid});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 5, 100);
    engine.post({ps::CommandType::SetMacro, 1.0f, 2, ps::Mixer::kThrowMacro + 1});  // the synth's echo throw
    engine.post({ps::CommandType::SetMacro, 0.0f, 2, ps::Mixer::kThrowMacro + 1});
    engine.post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::ThrowReverb});  // the whole mix's
    renderToStep(engine, now, 7, 100);
    CHECK(!engine.mixer().throwing(2, 1) && !engine.mixer().throwing(ps::Mixer::kMaster, 0));  // waiting for the beat
    renderToStep(engine, now, 8, 100);
    CHECK(engine.mixer().throwing(2, 1) && engine.mixer().throwing(ps::Mixer::kMaster, 0));
    CHECK(engine.mixer().masterSend(0) == 1.0f);
    renderToStep(engine, now, 11, 100);
    CHECK(engine.mixer().throwing(2, 1));
    renderToStep(engine, now, 12, 100);
    CHECK(!engine.mixer().throwing(2, 1) && !engine.mixer().throwing(ps::Mixer::kMaster, 0));  // one beat
    engine.post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::ReverbFreeze});
    engine.post({ps::CommandType::SetPerformance, 0.8f, ps::PerformanceFx::BuildUp});
    renderToStep(engine, now, 13, 100);
    CHECK(engine.mixer().reverb().frozen() && engine.mixer().masterSend(0) > 0.4f);
    engine.post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::Drop});
    renderToStep(engine, now, 15, 100);
    CHECK(engine.mixer().reverb().frozen());  // until the bar line
    renderToStep(engine, now, 16, 100);
    CHECK(!engine.mixer().reverb().frozen() && engine.mixer().masterSend(0) == 0.0f && engine.mixer().performance().buildUp() == 0.0f);
}

// The wobble insert: its LFO follows the transport (the same phase whatever the blocks), the
// cutoff swings over its range, nothing blows up at full resonance.
void testWobbleFollowsTheSong() {
    auto run = [](int block) {
        ps::Engine engine(kRate);
        sceneA(engine);  // the acid line through a wobble
        engine.post({ps::CommandType::SetInsertType, 0.0f, 1 * ps::Mixer::kInserts, ps::InsertSlot::WobbleFx});
        engine.post({ps::CommandType::SetInsertParam, 0.5f, 1 * ps::Mixer::kInserts, 0});  // 1/8
        engine.post({ps::CommandType::SetInsertParam, 0.9f, 1 * ps::Mixer::kInserts, 2});  // resonance
        engine.post({ps::CommandType::SetTransport, 1.0f, 0});
        return render(engine, kLiveStep * 32, block);
    };
    const auto a = run(256), b = run(97);
    CHECK(a == b);
    CHECK(finiteAndUnclipped(a));
    CHECK(rmsOf(a, 0, int(a.size())) > 0.01);
    ps::Wobble w;
    w.prepare(kRate);
    w.setStepFrames(kLiveStep);
    w.setParam(0, 0.5f);  // 1/8: a cycle every two steps
    w.setParam(1, 0.75f);
    std::vector<float> l(kLiveStep * 8, 0.1f), r(l);
    float lo = 1e9f, hi = 0.0f;
    for (int pos = 0; pos < static_cast<int>(l.size()); pos += 64) {
        if (pos % (4 * kLiveStep) == 0) w.beat(0, pos / kLiveStep + 6);  // a beat at step 6, 10..: in phase
        w.process(l.data() + pos, r.data() + pos, 64);
        lo = std::min(lo, w.cutoff());
        hi = std::max(hi, w.cutoff());
    }
    std::printf("  wobble 1/8: cutoff from %.0f to %.0f Hz\n", lo, hi);
    CHECK(lo < 150.0f && hi > 3000.0f);
    CHECK(ps::Wobble::stepsOf(0.0f) == 16.0 && ps::Wobble::stepsOf(0.99f) == 0.5);
    CHECK(ps::Wobble::shape(0, 0.0) == 0.0f && ps::Wobble::shape(2, 0.0) == 1.0f && ps::Wobble::shape(3, 0.75) == 0.0f);
}

// The take and the song written from it sound the same, sample for sample: each new live effect
// pressed a little before a beat (it waits for the beat line) and let go on a beat, against its
// song curve (a point per beat, held). Throws, freeze, kills and risers included.
void testLiveFxSongMatchesTheTake() {
    struct Gesture {
        int param;
        int onBeat, offBeat;
        float value;
    };
    const std::vector<Gesture> gestures = {
        {ps::PerformanceFx::Reverse, 2, 4, 1.0f},   {ps::PerformanceFx::HalfTime, 5, 7, 1.0f},
        {ps::PerformanceFx::Backspin, 8, 9, 1.0f},  {ps::PerformanceFx::TapeStop, 10, 11, 1.0f},
        {ps::PerformanceFx::EchoOut, 12, 13, 1.0f}, {ps::PerformanceFx::KillLow, 14, 16, 1.0f},
        {ps::PerformanceFx::Riser, 16, 17, 1.0f},   {ps::PerformanceFx::ThrowEcho, 19, 20, 1.0f},
        {ps::PerformanceFx::ReverbFreeze, 21, 23, 1.0f}, {ps::PerformanceFx::BuildUp, 24, 27, 0.7f},
        {ps::PerformanceFx::Repeat, 28, 30, 1.0f},
    };
    constexpr int kBeats = 34;
    auto record = [&](bool asSong, int block) {
        auto e = std::make_unique<ps::Engine>(kRate);
        sceneA(*e);
        e->post({ps::CommandType::SetSend, 0.4f, 1, 0});
        e->post({ps::CommandType::SetSend, 0.3f, 1, 1});
        e->post({ps::CommandType::SetPerformance, kGridBeat, ps::PerformanceFx::Grid});
        e->post({ps::CommandType::SetPerformance, 0.1f, ps::PerformanceFx::RiserLength});  // 1 bar
        e->post({ps::CommandType::SetPerformance, 0.3f, ps::PerformanceFx::RepeatDivision});  // 1/8T
        std::vector<float> out;
        int64_t at = 0;
        auto to = [&](int64_t frame) {
            if (frame > at) {
                auto part = render(*e, static_cast<int>(frame - at), block);
                out.insert(out.end(), part.begin(), part.end());
                at = frame;
            }
        };
        if (asSong) {
            for (size_t k = 0; k < gestures.size(); ++k) {
                const auto& g = gestures[k];
                const int lane = static_cast<int>(k);
                e->post({ps::CommandType::SetAutomationTarget, 0.0f, lane, ps::Automation::pack(ps::Automation::Live, ps::Mixer::kMaster, 0, g.param)});
                for (int beat = 0; beat < kBeats; ++beat) {
                    e->post({ps::CommandType::SetAutomationSong, beat >= g.onBeat && beat < g.offBeat ? g.value : 0.0f, lane, beat});
                }
                e->post({ps::CommandType::SetAutomationMode, float(kBeats), lane, ps::Automation::Song | ps::Automation::kPerBeat | ps::Automation::kHeld});
            }
            e->post({ps::CommandType::SetTransport, 1.0f, 0, 0});
            to(int64_t(kBeats) * 4 * kLiveStep);
            return out;
        }
        e->post({ps::CommandType::SetTransport, 1.0f, 0, 0});
        // Every event in time order: a press 1.5 steps before its beat (the build-up's knob, not a
        // held effect, on it), a release on its beat.
        std::vector<std::pair<int64_t, std::pair<int, float>>> events;
        for (const auto& g : gestures) {
            const int64_t early = g.param == ps::PerformanceFx::BuildUp ? 0 : kLiveStep + kLiveStep / 2;
            events.push_back({int64_t(g.onBeat * 4) * kLiveStep - early, {g.param, g.value}});
            events.push_back({int64_t(g.offBeat * 4) * kLiveStep, {g.param, 0.0f}});
        }
        std::stable_sort(events.begin(), events.end(), [](const auto& a, const auto& b) { return a.first < b.first; });
        for (const auto& [frame, ev] : events) {
            to(frame);
            e->post({ps::CommandType::SetPerformance, ev.second, ev.first});
        }
        to(int64_t(kBeats) * 4 * kLiveStep);
        return out;
    };
    const auto take = record(false, 256);
    const auto song = record(true, 256);
    const auto songOdd = record(true, 113);
    float diff = 0.0f, peak = 0.0f;
    for (size_t i = 0; i < std::min(take.size(), song.size()); ++i) {
        diff = std::max(diff, std::fabs(take[i] - song[i]));
        peak = std::max(peak, std::fabs(take[i]));
    }
    const int first = firstDifference(take, song);
    std::printf("  live effects written as a song: max difference %.3g (peak %.3f), first at step %.2f\n", diff, peak,
                first < 0 ? -1.0 : double(first / kChannels) / kLiveStep);
    CHECK(take.size() == song.size() && first < 0 && peak > 0.05f);
    CHECK(song == songOdd);  // and whatever the size of the blocks
    CHECK(finiteAndUnclipped(take));
}


// Drop pressed, then Stop before its bar line: nothing waits any more, everything is at rest at
// once (a kill or a held effect would otherwise stay on while stopped, under the pads).
void testLiveDropThenStop() {
    const int s = kLiveStep;
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    fx.setParam(ps::PerformanceFx::Grid, kGridSixteenth);
    const auto bass = sineInput(s * 12, 50.0, 0.4f);
    runLive(fx, bass, {{s, [&] { fx.setParam(ps::PerformanceFx::KillLow, 1.0f); fx.setParam(ps::PerformanceFx::Chop, 1.0f); }},
                       {5 * s + 100, [&] { fx.setParam(ps::PerformanceFx::Drop, 1.0f); fx.setParam(ps::PerformanceFx::Drop, 0.0f); }}});
    CHECK(fx.killOn(0) && fx.active());  // the drop waits for step 16
    fx.setClock(false);                  // Stop
    std::vector<float> l(4800, 0.1f), r(l);
    fx.process(l.data(), r.data(), 4800);
    std::printf("  drop then stop: active %d, bass kill %d\n", fx.active() ? 1 : 0, fx.killOn(0) ? 1 : 0);
    CHECK(!fx.killOn(0) && !fx.active() && l[4799] == 0.1f);
    // Through the engine: the transport stopped, the mixer's part too (a strip's kill, the returns).
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::KillMid});
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    int64_t now = 0;
    renderToStep(engine, now, 3, 100);
    engine.post({ps::CommandType::SetMacro, 1.0f, 2, ps::Mixer::kKillMacro});
    engine.post({ps::CommandType::SetPerformance, 1.0f, ps::PerformanceFx::Drop});
    engine.post({ps::CommandType::SetPerformance, 0.0f, ps::PerformanceFx::Drop});
    renderToStep(engine, now, 6, 100);
    CHECK(engine.mixer().performance().killOn(1));
    engine.post({ps::CommandType::SetTransport, 0.0f, 0});
    render(engine, 9600, 256);
    CHECK(!engine.mixer().performance().killOn(1) && !engine.mixer().isolator(2).kill(0));
}

// A release effect (here the echo) playing when another effect starts: the release ends there, so
// the new roll is heard at once, not under a dry sound fading out until the bar line.
void testLiveGestureAfterARelease() {
    const int s = kLiveStep;
    const auto tone = sineInput(s * 20, 330.0, 0.4f);
    auto run = [&](float release) {
        ps::PerformanceFx fx;
        fx.prepare(kRate);
        fx.setParam(ps::PerformanceFx::Grid, kGridSixteenth);
        fx.setParam(ps::PerformanceFx::ReleaseMode, release);
        fx.setParam(ps::PerformanceFx::RepeatDivision, 0.4375f);  // 1/8
        return runLive(fx, tone, {{2 * s + 100, [&] { fx.setParam(ps::PerformanceFx::Repeat, 1.0f); }},
                                  {6 * s + 300, [&] { fx.setParam(ps::PerformanceFx::Repeat, 0.0f); }},  // let go: the echo
                                  {6 * s + 2000, [&] { fx.setParam(ps::PerformanceFx::Repeat, 1.0f); }},  // and again at once
                                  {14 * s + 10, [&] { fx.setParam(ps::PerformanceFx::Repeat, 0.0f); }}});
    };
    const auto cut = run(0.0f), echo = run(0.3f);  // ReleaseCut, ReleaseEcho
    const double a = rmsOf(cut, 8 * s, 14 * s), b = rmsOf(echo, 8 * s, 14 * s);
    std::printf("  a roll right after its echo release: %.4f (%.4f without the release)\n", b, a);
    CHECK(a > 0.2 && b > 0.8 * a);
    CHECK(finiteAndUnclipped(echo));
}

// The drop: no click where the build-up vanishes (a short fade, not a jump), at a mid Montée where
// the noise does not hide it.
void testLiveDropHasNoClick() {
    const int s = kLiveStep;
    const auto bass = sineInput(s * 24, 50.0, 0.4f);
    ps::PerformanceFx fx;
    fx.prepare(kRate);
    const auto out = runLive(fx, bass, {{s, [&] { fx.setParam(ps::PerformanceFx::BuildUp, 0.4f); }},
                                        {10 * s, [&] { fx.setParam(ps::PerformanceFx::Drop, 1.0f); fx.setParam(ps::PerformanceFx::Drop, 0.0f); }}});
    auto largestStep = [&](int from, int to) {
        float m = 0.0f;
        for (int i = from; i < to; ++i) m = std::max(m, std::fabs(out[size_t(i)] - out[size_t(i - 1)]));
        return m;
    };
    const float before = largestStep(12 * s, 16 * s - 50), at = largestStep(16 * s - 50, 16 * s + 400);
    std::printf("  drop at Montée 0.4: largest step %.4f at the bar line, %.4f in the beat before\n", at, before);
    CHECK(at < 1.2f * before);
    CHECK(fx.buildUp() == 0.0f);
}

// « Montée » then « Drop » written into the song (a point per beat, jumps held: SetAutomationSongFlat)
// sounds like the take: built up through the last beat, gone on the drop's bar line, the rolls
// there to the end. Gestures off the beat (the Montée raised mid-beat, a reverse let go mid-beat)
// are written on the nearest beat, as the app's take does. The effects really change the sound.
void testLiveBuildUpAndDropSongMatchesTheTake() {
    const int s = kLiveStep;
    constexpr int kSteps = 56;
    const int32_t build = ps::Automation::pack(ps::Automation::Live, ps::Mixer::kMaster, 0, ps::PerformanceFx::BuildUp);
    const int32_t drop = ps::Automation::pack(ps::Automation::Live, ps::Mixer::kMaster, 0, ps::PerformanceFx::Drop);
    const int32_t reverse = ps::Automation::pack(ps::Automation::Live, ps::Mixer::kMaster, 0, ps::PerformanceFx::Reverse);
    struct State { float build; bool repeating; };
    // mode 0: the take (gestures live), 1: the song, 2: dry (nothing touched).
    auto record = [&](int mode, int block, std::vector<State>* states) {
        auto e = std::make_unique<ps::Engine>(kRate);
        sceneA(*e);
        e->post({ps::CommandType::SetSend, 0.3f, 1, 0});
        e->post({ps::CommandType::SetPerformance, kGridSixteenth, ps::PerformanceFx::Grid});
        if (mode == 1) {
            auto lane = [&](int k, int32_t target, const std::vector<float>& beats, bool held, const std::vector<int>& flats) {
                e->post({ps::CommandType::SetAutomationTarget, 0.0f, k, target});
                const int n = static_cast<int>(beats.size());
                e->post({ps::CommandType::SetAutomationMode, float(n), k, ps::Automation::Song | ps::Automation::kPerBeat | (held ? ps::Automation::kHeld : 0)});
                for (int b = 0; b < n; ++b) e->post({ps::CommandType::SetAutomationSong, beats[size_t(b)], k, b});
                for (int f : flats) e->post({ps::CommandType::SetAutomationSongFlat, 1.0f, k, f | (1 << 16)});
            };
            // Montée: 0.3 (step 5.5 -> beat 1), 0.6 (9.3 -> 2), 0.8 (13.7 -> 3), each after staying
            // still: jumps; the Drop's 0 on beat 8, a jump (flat until there).
            lane(0, build, {0, 0.3f, 0.6f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0, 0, 0, 0, 0, 0}, false, {0, 1, 2, 7});
            lane(1, drop, {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0}, true, {});
            // Reverse: pressed at 34.5 (starts on 35 -> beat 9), let go at 41.3 (-> beat 10).
            lane(2, reverse, {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, true, {});
        }
        e->post({ps::CommandType::SetTransport, 1.0f, 0, 0});
        std::vector<std::pair<double, std::pair<int, float>>> events;
        if (mode == 0) {
            events = {{5.5, {ps::PerformanceFx::BuildUp, 0.3f}}, {9.3, {ps::PerformanceFx::BuildUp, 0.6f}},
                      {13.7, {ps::PerformanceFx::BuildUp, 0.8f}}, {25.3, {ps::PerformanceFx::Drop, 1.0f}},
                      {25.3, {ps::PerformanceFx::Drop, 0.0f}}, {34.5, {ps::PerformanceFx::Reverse, 1.0f}},
                      {41.3, {ps::PerformanceFx::Reverse, 0.0f}}};
        }
        std::vector<float> out;
        int64_t at = 0;
        auto to = [&](int64_t frame) {
            while (frame > at) {
                // Step by step: the state on each step's first frames.
                const int64_t next = std::min(frame, (at / s + 1) * s);
                auto part = render(*e, static_cast<int>(next - at), std::min<int>(block, static_cast<int>(next - at)));
                out.insert(out.end(), part.begin(), part.end());
                at = next;
                if (states != nullptr && at % s == 0) {
                    auto& p = e->mixer().performance();
                    states->push_back({p.buildUp(), p.repeating()});
                }
            }
        };
        for (const auto& [step, ev] : events) {
            to(static_cast<int64_t>(step * s));
            e->post({ps::CommandType::SetPerformance, ev.second, ev.first});
        }
        to(int64_t(kSteps) * s);
        return out;
    };
    std::vector<State> takeStates, songStates;
    const auto take = record(0, 256, &takeStates);
    const auto song = record(1, 256, &songStates);
    const auto songOdd = record(1, 113, nullptr);
    const auto dry = record(2, 256, nullptr);
    // takeStates[k]: the state at the end of step k (k + 1 steps rendered).
    for (int k = 28; k < 31; ++k) {
        std::printf("  step %d: take build %.3f%s, song build %.3f%s\n", k + 1, takeStates[size_t(k)].build, takeStates[size_t(k)].repeating ? " rolling" : "",
                    songStates[size_t(k)].build, songStates[size_t(k)].repeating ? " rolling" : "");
        // The last beat before the drop: built up and rolling, in the song as in the take.
        CHECK(std::fabs(songStates[size_t(k)].build - 0.8f) < 0.01f && songStates[size_t(k)].repeating);
        CHECK(std::fabs(takeStates[size_t(k)].build - 0.8f) < 0.01f && takeStates[size_t(k)].repeating);
    }
    // From the drop's bar line: nothing built up in either.
    CHECK(songStates[32].build == 0.0f && takeStates[32].build == 0.0f);
    // Beat by beat, the song sounds like the take (left side); the effects changed the sound.
    auto rmsL = [&](const std::vector<float>& v, int fromStep, int toStep) {
        double sum = 0.0;
        for (int64_t i = int64_t(fromStep) * s; i < int64_t(toStep) * s; ++i) sum += double(v[size_t(i * kChannels)]) * v[size_t(i * kChannels)];
        return std::sqrt(sum / double(int64_t(toStep - fromStep) * s));
    };
    auto diffL = [&](const std::vector<float>& a, const std::vector<float>& b, int fromStep, int toStep) {
        double sum = 0.0;
        for (int64_t i = int64_t(fromStep) * s; i < int64_t(toStep) * s; ++i) {
            const double d = double(a[size_t(i * kChannels)]) - b[size_t(i * kChannels)];
            sum += d * d;
        }
        return std::sqrt(sum / double(int64_t(toStep - fromStep) * s));
    };
    for (int beat : {4, 5, 6, 7, 11, 12}) {
        const double t = rmsL(take, beat * 4, beat * 4 + 4), g = rmsL(song, beat * 4, beat * 4 + 4);
        std::printf("  beat %d: take %.4f, song %.4f, dry %.4f\n", beat, t, g, rmsL(dry, beat * 4, beat * 4 + 4));
        CHECK(std::fabs(t - g) < 0.15 * t);
    }
    const double changed = diffL(song, dry, 16, 32), level = rmsL(dry, 16, 32);
    std::printf("  the Montée changes the sound: %.4f from the dry render (dry %.4f)\n", changed, level);
    CHECK(changed > 0.3 * level);
    CHECK(song == songOdd);
    CHECK(finiteAndUnclipped(take) && finiteAndUnclipped(song));
}

// --- Phase 28 (engine audit) ----------------------------------------------------------------

namespace p28 {

void post(ps::Engine& e, ps::CommandType t, float v = 0.0f, int32_t a = 0, int32_t b = 0) {
    ps::Command c;
    c.type = t;
    c.value = v;
    c.a = a;
    c.b = b;
    while (!e.post(c)) {
        float tmp[2];
        e.process(nullptr, 0, 0, tmp, 0, 2);
    }
}

// Residual of `sum` against `ref`, in dB below `ref`.
double residualDb(const std::vector<float>& sum, const std::vector<float>& ref) {
    double err = 0.0, level = 0.0;
    for (size_t i = 0; i < ref.size() && i < sum.size(); ++i) {
        const double d = double(sum[i]) - ref[i];
        err += d * d;
        level += double(ref[i]) * ref[i];
    }
    return 10.0 * std::log10(err / std::max(level, 1e-30) + 1e-30);
}

}  // namespace p28

// « Piste par piste » of a recorded Live performance with the build-up, the riser or the
// downlifter: their noise comes from no track. Each track's stem leaves it out (SetMaster a=3,
// 1) and the stem « Effets live » holds it alone (2): together they add back up to the mix.
void testLiveNoisesStemAddsUp() {
    using ps::CommandType;
    auto setup = [](ps::Engine& e, int liveParam, int solo, int noises) {
        p28::post(e, CommandType::SetTempo, 128.0f);
        // The kick and the acid line only: the kit's noisy voices (hats, snare) draw from DaisySP's
        // shared random state, so two engines never play quite the same noise (inaudible, but
        // the residual measured below would be that, not the Live noise).
        for (int s = 0; s < 16; ++s) {
            if (s % 4 == 0) p28::post(e, CommandType::SetDrumHit, 1.0f, 0, s);
            ps::AcidStep st;
            st.note = (s * 5) % 12;
            st.gate = s % 3 != 2;
            p28::post(e, CommandType::SetAcidStep, 0.0f, s, st.pack());
        }
        p28::post(e, CommandType::SetSend, 0.3f, 1, 0);
        // The recorded performance: a song curve per beat on the Live param, on for beats 8..23.
        p28::post(e, CommandType::SetAutomationTarget, 0.0f, 0, ps::Automation::pack(ps::Automation::Live, 0, 0, liveParam));
        p28::post(e, CommandType::SetAutomationMode, 64.0f, 0, ps::Automation::Song | ps::Automation::kPerBeat | ps::Automation::kHeld);
        for (int b = 0; b < 64; ++b) {
            const float on = liveParam == ps::PerformanceFx::BuildUp ? 0.9f : 1.0f;
            p28::post(e, CommandType::SetAutomationSong, (b >= 8 && b < 24) ? on : 0.0f, 0, b);
        }
        // As describe() does: no limiter, stems without the master's effects.
        p28::post(e, CommandType::SetMaster, 0.0f, 1);
        if (noises != 0) {
            p28::post(e, CommandType::SetMaster, 0.0f, 2);
            p28::post(e, CommandType::SetMaster, static_cast<float>(noises), 3);
        }
        if (solo >= 0) p28::post(e, CommandType::SetChannelSolo, 1.0f, solo);
        render(e, 4800, 512);
        e.mixer().settle();
        p28::post(e, CommandType::SetTransport, 1.0f, 0, 0);
    };
    const int frames = kRate * 9;
    for (int param : {ps::PerformanceFx::BuildUp, ps::PerformanceFx::Riser, ps::PerformanceFx::Downlifter}) {
        auto run = [&](int solo, int noises) {
            auto e = std::make_unique<ps::Engine>(kRate);
            setup(*e, param, solo, noises);
            return render(*e, frames, 512);
        };
        const auto mix = run(-1, 0);
        const auto drums = run(0, 1), acid = run(1, 1), live = run(-1, 2);
        const auto drumsWithNoise = run(0, 0);
        std::vector<float> sum(mix.size()), old(mix.size()), tracks(mix.size());
        double liveEnergy = 0.0;
        for (size_t i = 0; i < mix.size(); ++i) {
            tracks[i] = drums[i] + acid[i];
            sum[i] = tracks[i] + live[i];
            old[i] = drumsWithNoise[i] + acid[i] + live[i];  // what each stem gave before: the noise twice
            liveEnergy += double(live[i]) * live[i];
        }
        const double res = p28::residualDb(sum, mix), without = p28::residualDb(tracks, mix), doubled = p28::residualDb(old, mix);
        std::printf("  param %d: stems + « Effets live » %.1f dB, without it %.1f dB, noise in a track's stem %.1f dB\n", param, res, without, doubled);
        CHECK(res < -80.0);
        CHECK(without > -40.0);  // the noise is really there, and only in the « Effets live » file
        CHECK(doubled > -40.0);
        CHECK(liveEnergy > 1.0);
        // Before the first press and after the last fade, the « Effets live » file is silent.
        float early = 0.0f;
        for (size_t i = 0; i < size_t(kRate) * 2 * 3; ++i) early = std::max(early, std::fabs(live[i]));
        CHECK(early == 0.0f);
    }
}

// Every echo note value at every tempo (40..300 BPM): the first repeat is on time, for the insert
// echo (it used to stop at 1.6 s: a half note under 75 BPM came early) and the send delay (2.5 s).
void testEchoTimesAtSlowTempos() {
    const int steps[6] = {1, 2, 3, 4, 6, 8};  // 1/16, 1/8, 1/8 dotted, 1/4, 1/4 dotted, 1/2
    const int n = kRate * 4;
    int worst = 0;
    for (int insert = 0; insert < 2; ++insert) {
        for (float bpm : {40.0f, 47.0f, 60.0f, 70.0f, 74.0f, 120.0f, 300.0f}) {
            const double stepFrames = kRate * 60.0 / (bpm * 4.0);
            for (int c = 0; c < 6; ++c) {
                std::vector<float> l(size_t(n), 0.0f), r(size_t(n), 0.0f), ol(size_t(n), 0.0f), orr(size_t(n), 0.0f);
                l[0] = r[0] = 1.0f;
                const float choice = (static_cast<float>(c) + 0.5f) / 6.0f;  // the middle of the app's bin
                if (insert == 1) {
                    auto echo = std::make_unique<ps::InsertEcho>();
                    echo->prepare(kRate);
                    echo->setStepFrames(stepFrames);
                    echo->setParam(0, choice);
                    echo->setParam(1, 0.0f);  // no feedback
                    echo->setParam(2, 1.0f);  // bright
                    echo->setParam(3, 1.0f);  // wet only
                    for (int at = 0; at < n; at += 256) echo->process(l.data() + at, r.data() + at, std::min(256, n - at));
                    ol = l;
                } else {
                    auto delay = std::make_unique<ps::TempoDelay>();
                    delay->prepare(kRate);
                    delay->setStepFrames(stepFrames);
                    delay->setParam(0, choice);
                    delay->setParam(1, 0.0f);
                    delay->setParam(2, 0.0f);
                    delay->setParam(3, 1.0f);
                    for (int at = 0; at < n; at += 256) {
                        const int m = std::min(256, n - at);
                        delay->process(l.data() + at, r.data() + at, ol.data() + at, orr.data() + at, m);
                    }
                }
                int first = -1;
                for (int i = 1; i < n; ++i) {
                    if (std::fabs(ol[size_t(i)]) > 0.05f) { first = i; break; }
                }
                const int want = static_cast<int>(std::lround(steps[c] * stepFrames));
                const int off = first < 0 ? n : std::abs(first - want);
                worst = std::max(worst, off);
                if (off > 48) std::printf("  %s %.0f BPM, %d sixteenths: repeat at %d, want %d\n", insert ? "insert" : "send", bpm, steps[c], first, want);
                CHECK(off <= 48);  // within 1 ms
            }
        }
    }
    std::printf("  worst first repeat: %d frames off\n", worst);
}

// A roll on a swung (odd) step shares that step's real length: none of its hits lands on or after
// the next step's hit (it used to: a flam at swing 0.3, hits doubled at 0.6). An even step's roll
// keeps the plain step length.
void testRollsOnSwungStepsStayInside() {
    using ps::CommandType;
    for (float swing : {0.0f, 0.3f, 0.6f, 0.75f}) {
        ps::Engine e(kRate);
        p28::post(e, CommandType::SetTempo, 120.0f);  // a step = 6000 frames
        p28::post(e, CommandType::SetSwing, swing);
        std::vector<float> click(64, 0.0f);
        click[0] = 1.0f;
        e.post({CommandType::InstallSample, 0.0f, 5, 0, ps::Sample::create(click.data(), 64, 1, kRate, kRate)});
        p28::post(e, CommandType::SetPadParam, 0.0f, 5, ps::Sampler::Release);
        p28::post(e, CommandType::SetDrumHit, 1.0f, 5, 0 | (4 << 16));  // a roll of 4 on step 0 (even)
        p28::post(e, CommandType::SetDrumHit, 1.0f, 5, 1 | (4 << 16));  // a roll of 4 on step 1 (odd: swung)
        p28::post(e, CommandType::SetDrumHit, 1.0f, 5, 2);              // a plain hit on step 2
        p28::post(e, CommandType::SetTransport, 1.0f, 0, 0);
        const auto out = render(e, 13000, 192);
        std::vector<int> hits;
        for (int i = 0; i < 13000; ++i) {
            const bool on = std::fabs(out[size_t(i) * 2]) > 0.05f;
            if (on && (i == 0 || std::fabs(out[size_t(i - 1) * 2]) <= 0.05f)) hits.push_back(i);
        }
        const int odd = static_cast<int>(std::lround(6000.0 * (1.0 + swing)));
        const double oddEvery = 6000.0 * (1.0 - swing) / 4.0;
        std::vector<int> want = {0, 1500, 3000, 4500};
        for (int k = 0; k < 4; ++k) want.push_back(odd + static_cast<int>(k * oddEvery));
        want.push_back(12000);
        for (int& w : want) w += kLate;  // the limiter's look-ahead
        std::printf("  swing %.2f: hits at", swing);
        for (int h : hits) std::printf(" %d", h);
        std::printf("\n");
        CHECK(hits.size() == want.size());
        for (size_t k = 0; k < hits.size() && k < want.size(); ++k) CHECK(std::abs(hits[k] - want[k]) <= 1);
    }
}

// The master's NaN guard: a NaN reaching the master (here a master EQ overflowing on a huge, still
// finite sample, which then keeps an infinity in its state) is caught before the Live effects
// (which make every NaN 0 in their memory, so the guard never saw it), reported, and the master's
// effects start again: the sound comes back on the next block instead of a silence for good.
// A NaN on the direct path (metronome, test tone; after the master's effects) is reported too.
void testMasterNanGuardReportsAndRecovers() {
    auto mixer = std::make_unique<ps::Mixer>();
    mixer->prepare(kRate);
    mixer->setInsertType(ps::Mixer::kMaster, 0, ps::InsertSlot::Eq);
    mixer->setInsertParam(ps::Mixer::kMaster, 0, 0, 0.8f);
    const int n = 256;
    std::vector<float> tone(static_cast<size_t>(n)), direct(static_cast<size_t>(n), 0.0f), out(static_cast<size_t>(n) * 2);
    const float* channels[ps::Mixer::kChannels] = {};
    channels[ps::Mixer::kDrums] = tone.data();
    double phase = 0.0;
    auto block = [&](bool nan) {
        for (int i = 0; i < n; ++i) {
            tone[size_t(i)] = 0.4f * static_cast<float>(std::sin(phase));
            phase += 2.0 * M_PI * 440.0 / kRate;
        }
        std::fill(direct.begin(), direct.end(), 0.0f);
        if (nan) tone[10] = std::numeric_limits<float>::max();
        mixer->process(channels, direct.data(), out.data(), n, 2);
        float peak = 0.0f;
        bool finite = true;
        for (float v : out) {
            finite &= std::isfinite(v);
            peak = std::max(peak, std::fabs(v));
        }
        CHECK(finite);
        return peak;
    };
    for (int k = 0; k < 20; ++k) block(false);
    CHECK(!mixer->takeBlewUp());
    const float before = block(false);
    block(true);
    CHECK(mixer->takeBlewUp());  // reported (crash reports, « hasNonFinite »)
    float after = 0.0f;
    for (int k = 0; k < 20; ++k) after = block(false);
    std::printf("  peak before %.3f, after the NaN %.3f\n", before, after);
    CHECK(before > 0.1f);
    CHECK(after > 0.5f * before);  // the sound is back
    CHECK(!mixer->takeBlewUp());
    std::fill(direct.begin(), direct.end(), 0.0f);
    direct[3] = std::numeric_limits<float>::quiet_NaN();
    mixer->process(channels, direct.data(), out.data(), n, 2);
    CHECK(mixer->takeBlewUp());
    for (float v : out) CHECK(std::isfinite(v));
}

// NaN, +-Inf and huge values on every setting path, then a sane value: never a crash (an echo time
// of NaN used to index kSteps[INT_MIN], a Plaits decay or colour of NaN a table inside Plaits),
// never a NaN out, and the sound comes back (a NaN volume, pan, send, reverb or echo setting, test
// tone frequency, synth, acid or effect knob used to silence it for good).
void testNanSettingsAreIgnored() {
    using ps::CommandType;
    ps::Engine e(kRate);
    // A busy project: drums with rolls, the acid line, the synth (track 0) and Plaits (track 1), sends.
    p28::post(e, CommandType::SetTempo, 128.0f);
    p28::post(e, CommandType::SetPatternSteps, 0.0f, 16);
    for (int st = 0; st < 16; ++st) {
        if (st % 4 == 0) p28::post(e, CommandType::SetDrumHit, 1.0f, 0, st);
        if (st % 8 == 4) p28::post(e, CommandType::SetDrumHit, 0.9f, 1, st);
        p28::post(e, CommandType::SetDrumHit, 0.6f, 2, st | (st % 4 == 3 ? 3 << 16 : 0));
        ps::AcidStep a;
        a.note = (st * 5) % 12;
        a.gate = st % 3 != 2;
        p28::post(e, CommandType::SetAcidStep, 0.0f, st, a.pack());
    }
    p28::post(e, CommandType::SetSynthSteps, 0.0f, 16);
    for (int k = 0; k < 4; ++k) p28::post(e, CommandType::SetSynthNote, 0.8f, k, ps::NoteTrack::pack(k * 4 * 24, 3 * 24, 60 + k * 3));
    p28::post(e, CommandType::SetSynthNoteCount, 0.0f, 4);
    p28::post(e, CommandType::SetTrackOn, 1.0f, 1);
    p28::post(e, CommandType::SetSynthEngine, 0.0f, (1 << 16) | 1);
    p28::post(e, CommandType::SetSynthSteps, 0.0f, (1 << 16) | 16);
    for (int k = 0; k < 8; ++k) p28::post(e, CommandType::SetSynthNote, 0.7f, (1 << 16) | k, ps::NoteTrack::pack(k * 2 * 24, 24, 72 + k));
    p28::post(e, CommandType::SetSynthNoteCount, 0.0f, (1 << 16) | 8);
    p28::post(e, CommandType::SetSend, 0.4f, 2, 0);
    p28::post(e, CommandType::SetSend, 0.3f, 1, 1);
    p28::post(e, CommandType::SetTransport, 1.0f, 0, 0);
    const auto first = render(e, kRate * 2, 192);
    double baseline = 0.0;
    for (float v : first) baseline += double(v) * v;
    baseline = std::sqrt(baseline / double(first.size()));
    CHECK(baseline > 0.05);

    const float bad[] = {std::numeric_limits<float>::quiet_NaN(), std::numeric_limits<float>::infinity(),
                         -std::numeric_limits<float>::infinity(), 1e30f, -1e30f};
    int cases = 0, dead = 0;
    double ref = baseline;  // the level expected back (an effect's own, with its sane settings)
    auto level = [&](int frames) {
        const auto out = render(e, frames, 192);
        double sum = 0.0;
        for (float x : out) sum += double(x) * x;
        return std::sqrt(sum / double(out.size()));
    };
    // Sends `type` (a, b) a bad value, then `sane`: the output stays finite and comes back.
    auto check = [&](CommandType type, int32_t a, int32_t b, float sane, const char* what) {
        for (float v : bad) {
            p28::post(e, type, v, a, b);
            const auto during = render(e, kRate / 20, 192);
            p28::post(e, type, sane, a, b);
            const auto after = render(e, kRate / 6, 192);
            bool finite = true;
            double sum = 0.0;
            for (float x : during) finite &= std::isfinite(x);
            for (float x : after) {
                finite &= std::isfinite(x);
                sum += double(x) * x;
            }
            const double rms = std::sqrt(sum / double(after.size()));
            ++cases;
            if (!finite || rms < 0.2 * ref) {
                ++dead;
                std::printf("  %s (a=%d, b=%d) = %g then %g: rms %.4f (expected about %.4f)%s\n", what, a, b, double(v), double(sane), rms, ref,
                            finite ? "" : ", not finite");
            }
        }
    };
    check(CommandType::SetGain, 0, 0, 0.5f, "SetGain");
    check(CommandType::SetFrequency, 0, 0, 440.0f, "SetFrequency");
    for (int ch : {0, 1, 2, 5}) {
        check(CommandType::SetChannelVolume, ch, 0, 0.8f, "SetChannelVolume");
        check(CommandType::SetChannelPan, ch, 0, 0.0f, "SetChannelPan");
        check(CommandType::SetSend, ch, 0, 0.2f, "SetSend");
        check(CommandType::SetSend, ch, 1, 0.2f, "SetSend");
    }
    for (int bus = 0; bus < 2; ++bus)
        for (int prm = 0; prm < 4; ++prm) check(CommandType::SetBusParam, bus, prm, 0.5f, "SetBusParam");
    for (int prm = 0; prm < ps::AcidTrack::ParamCount; ++prm) check(CommandType::SetAcidParam, prm, 0, 0.5f, "SetAcidParam");
    for (int prm = 0; prm < ps::Synth::ParamCount; ++prm) check(CommandType::SynthParam, prm, 0, 0.5f, "SynthParam");
    for (int prm = 0; prm < ps::PlaitsSynth::ParamCount; ++prm) check(CommandType::SetPlaitsParam, (1 << 16) | prm, 0, 0.5f, "SetPlaitsParam");
    // Every effect, on the drums' strip and on the master, each knob.
    for (int type = 1; type < ps::InsertSlot::kTypes; ++type) {
        for (int ch : {0, ps::Mixer::kMaster}) {
            p28::post(e, CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts, type);
            for (int prm = 0; prm < 4; ++prm) p28::post(e, CommandType::SetInsertParam, 0.3f, ch * ps::Mixer::kInserts, prm);
        }
        ref = level(kRate / 2);
        const int deadBefore = dead;
        for (int prm = 0; prm < 4; ++prm) {
            check(CommandType::SetInsertParam, 0, prm, 0.3f, "SetInsertParam (drums)");
            check(CommandType::SetInsertParam, ps::Mixer::kMaster * ps::Mixer::kInserts, prm, 0.3f, "SetInsertParam (master)");
        }
        if (dead > deadBefore) std::printf("  (effect type %d)\n", type);
        for (int ch : {0, ps::Mixer::kMaster}) p28::post(e, CommandType::SetInsertType, 0.0f, ch * ps::Mixer::kInserts, 0);
    }
    std::printf("  %d cases, %d without their sound back\n", cases, dead);
    CHECK(dead == 0);
    CHECK(!e.hasNonFinite());  // ignored before reaching the sound: no NaN ever caught
}

// The same project rendered in the phone's bursts (192, 7, 997 frames) and in the export's blocks
// of 512 gives the same samples: every drum voice (noise included, a new hit's level from its own
// frame), the synth (its control ticks counted across calls, a new note's pitch set on its first
// frame), Plaits (its notes on the first 12-frame block at or after them, its noise its own) and
// the wah (its updates counted across calls).
void testRenderIsBlockSizeIndependent() {
    using ps::CommandType;
    auto setup = [](ps::Engine& e) {
        p28::post(e, CommandType::SetTempo, 133.0f);
        p28::post(e, CommandType::SetSwing, 0.31f);
        p28::post(e, CommandType::SetPatternSteps, 0.0f, 16);
        for (int lane = 0; lane < 16; ++lane)
            for (int st = lane % 3; st < 16; st += 3 + lane % 4) p28::post(e, CommandType::SetDrumHit, 0.5f + 0.03f * lane, lane, st | (st % 5 == 4 ? 3 << 16 : 0));
        // Track 0: the synth with unison, a synced sweep, vibrato and noise; track 1: Plaits.
        p28::post(e, CommandType::SynthParam, 0.5f, ps::Synth::Unison);
        p28::post(e, CommandType::SynthParam, 0.5f, ps::Synth::Lfo2Sync);
        p28::post(e, CommandType::SynthParam, 0.5f, ps::Synth::Lfo2ToCutoff);
        p28::post(e, CommandType::SynthParam, 0.4f, ps::Synth::Lfo1ToPitch);
        p28::post(e, CommandType::SynthParam, 0.3f, ps::Synth::NoiseLevel);
        p28::post(e, CommandType::SetSynthSteps, 0.0f, 16);
        int idx = 0;
        for (int st = 0; st < 16; st += 2) p28::post(e, CommandType::SetSynthNote, 0.8f, idx++, ps::NoteTrack::pack(st * 24 + 5, 30, 48 + st * 2));
        p28::post(e, CommandType::SetSynthNoteCount, 0.0f, idx);
        p28::post(e, CommandType::SetTrackOn, 1.0f, 1);
        p28::post(e, CommandType::SetSynthEngine, 0.0f, (1 << 16) | 1);
        p28::post(e, CommandType::SetSynthSteps, 0.0f, (1 << 16) | 16);
        for (int k = 0; k < 8; ++k) p28::post(e, CommandType::SetSynthNote, 0.7f, (1 << 16) | k, ps::NoteTrack::pack(k * 2 * 24 + 7, 24, 60 + 3 * k));
        p28::post(e, CommandType::SetSynthNoteCount, 0.0f, (1 << 16) | 8);
        // A wah on the drums, a flanger on the synth.
        p28::post(e, CommandType::SetInsertType, 0.0f, 0 * ps::Mixer::kInserts, ps::InsertSlot::WahFx);
        p28::post(e, CommandType::SetInsertType, 0.0f, 2 * ps::Mixer::kInserts, ps::InsertSlot::FlangerFx);
        p28::post(e, CommandType::SetTransport, 1.0f, 0, 0);
    };
    const int frames = kRate * 5;
    auto run = [&](int block) {
        auto e = std::make_unique<ps::Engine>(kRate);
        setup(*e);
        return render(*e, frames, block);
    };
    const auto ref = run(512);
    double level = 0.0;
    for (float v : ref) level = std::max(level, double(std::fabs(v)));
    CHECK(level > 0.3);
    for (int block : {192, 7, 997}) {
        const auto out = run(block);
        float worst = 0.0f;
        for (size_t i = 0; i < ref.size(); ++i) worst = std::max(worst, std::fabs(out[i] - ref[i]));
        std::printf("  blocks of %d: max diff %.2g\n", block, double(worst));
        CHECK(worst < 1e-5f);
    }
}

// A take starts its effects' free LFOs (flanger, phaser, tremolo, robot, vinyl) with the
// transport, as the export does: a live engine that has been playing for a while
// sounds, from ▶, like a fresh export engine.
void testLfosRestartWithTheTransport() {
    using ps::CommandType;
    auto setup = [](ps::Engine& e) {
        p28::post(e, CommandType::SetTempo, 120.0f);
        p28::post(e, CommandType::SetPatternSteps, 0.0f, 16);
        // A sampled pad: a sound with no state of its own. (The kit's hi-hats keep their metallic
        // oscillators' phases from one hit to the next, a take's and an export's alike: not here.)
        for (int st = 0; st < 16; st += 2) p28::post(e, CommandType::SetDrumHit, 0.8f, 3, st);
        const int types[] = {ps::InsertSlot::FlangerFx, ps::InsertSlot::PhaserFx, ps::InsertSlot::TremoloFx, ps::InsertSlot::RobotFx,
                             ps::InsertSlot::VinylFx};
        for (int k = 0; k < 5; ++k) {
            p28::post(e, CommandType::SetInsertType, 0.0f, 0 * ps::Mixer::kInserts + k, types[k]);
            p28::post(e, CommandType::SetInsertParam, 0.25f, 0 * ps::Mixer::kInserts + k, 0);
        }
        p28::post(e, CommandType::SetTransport, 1.0f, 0, 0);
    };
    auto run = [&](bool warm) {
        auto e = std::make_unique<ps::Engine>(kRate);
        std::vector<float> tone(4800);
        for (size_t i = 0; i < tone.size(); ++i) tone[i] = 0.5f * static_cast<float>(std::sin(2.0 * M_PI * 330.0 * double(i) / kRate)) * (1.0f - float(i) / float(tone.size()));
        e->post({CommandType::InstallSample, 0.0f, 3, 0, ps::Sample::create(tone.data(), static_cast<int>(tone.size()), 1, kRate, kRate)});
        if (warm) {
            // Played for a while, stopped long enough for everything to have died away.
            setup(*e);
            render(*e, kRate + 4321, 192);
            p28::post(*e, CommandType::SetTransport, 0.0f);
            render(*e, kRate * 4, 192);
        }
        setup(*e);
        return render(*e, kRate * 3, 192);
    };
    const auto fresh = run(false), warm = run(true);
    float worst = 0.0f, peak = 0.0f;
    // From the take's first frame (the limiter's look-ahead still holds what played before it).
    for (size_t i = size_t(kLate) * kChannels; i < fresh.size(); ++i) {
        worst = std::max(worst, std::fabs(fresh[i] - warm[i]));
        peak = std::max(peak, std::fabs(fresh[i]));
    }
    std::printf("  a take after playing a while vs a fresh export: max diff %.2g (peak %.2f)\n", double(worst), double(peak));
    CHECK(peak > 0.1f);
    CHECK(worst < 1e-4f);
}

// A synth note sounds at its own pitch from its first frame, not at the voice's last one until
// the next control tick (up to 15 frames): the same note started 9 frames into a callback is the
// same sound as one started on a tick.
void testSynthNoteHasItsPitchFromItsFirstFrame() {
    auto run = [](int offset) {
        auto synth = std::make_unique<ps::Synth>();
        synth->prepare(kRate);
        openFilter(*synth);
        synth->setParam(ps::Synth::Lfo1ToPitch, 0.0f);
        synth->setParam(ps::Synth::AmpAttack, 0.0f);  // 1 ms: loud from the start
        // The voice played a low note first (its last pitch), long released.
        synth->noteOn(30, 1.0f, 0);
        std::vector<float> out(4096, 0.0f);
        synth->render(out.data(), 1024);
        synth->noteOff(30, 0);
        std::fill(out.begin(), out.end(), 0.0f);
        for (int k = 0; k < 64; ++k) synth->render(out.data(), 1024);
        std::fill(out.begin(), out.end(), 0.0f);
        synth->noteOn(96, 1.0f, offset);
        synth->render(out.data(), 256);
        return std::vector<float>(out.begin() + offset, out.begin() + offset + 64);
    };
    const auto onTick = run(0), between = run(9);
    float worst = 0.0f, peak = 0.0f;
    for (size_t i = 0; i < onTick.size(); ++i) {
        worst = std::max(worst, std::fabs(onTick[i] - between[i]));
        peak = std::max(peak, std::fabs(onTick[i]));
    }
    std::printf("  note on a tick vs 9 frames off it: max diff %.2g (peak %.2f)\n", double(worst), double(peak));
    CHECK(peak > 0.05f);
    CHECK(worst < 1e-5f);
}

// The phone's stream applies at most a budget of commands per callback (a full resync in one
// callback could miss its deadline: 4096 reverb switches took 8 ms), effect switches counting
// more; a staged scene is still taken whole; an offline render takes everything at once.
void testCommandBudgetPerCallback() {
    using ps::CommandType;
    float out[2 * 64];
    {
        ps::Engine e(kRate);
        e.setCommandBudget(ps::Engine::kLiveCommandBudget);
        for (int i = 0; i < 1500; ++i) CHECK(e.post({CommandType::SetChannelVolume, 0.5f + 0.0001f * float(i % 7), 0}));
        CHECK(e.post({CommandType::SetPlaying, 1.0f}));
        e.process(nullptr, 0, 0, out, 64, 2);
        CHECK(!e.isPlaying());  // 1024 applied: the rest waits
        e.process(nullptr, 0, 0, out, 64, 2);
        CHECK(e.isPlaying());
        // Effect switches weigh kHeavyCommand.
        for (int i = 0; i < 200; ++i) CHECK(e.post({CommandType::SetInsertType, 0.0f, (i % 4) * ps::Mixer::kInserts + 7, i % 2 ? ps::InsertSlot::ReverbFx : 0}));
        CHECK(e.post({CommandType::SetPlaying, 0.0f}));
        e.process(nullptr, 0, 0, out, 64, 2);
        CHECK(e.isPlaying());
        e.process(nullptr, 0, 0, out, 64, 2);
        CHECK(!e.isPlaying());
        // A staged scene of more than the budget, taken whole in one callback.
        CHECK(e.post({CommandType::SetStaging, 1.0f}));
        for (int lane = 0; lane < ps::Engine::kLanes; ++lane)
            for (int st = 0; st < ps::Engine::kMaxSteps; ++st) CHECK(e.post({CommandType::SetDrumHit, st % 4 == 0 ? 1.0f : 0.0f, lane, st}));
        CHECK(e.post({CommandType::SetPlaying, 1.0f}));  // its last command (not a staged one: it shows)
        CHECK(e.post({CommandType::SetStaging, 0.0f}));
        e.process(nullptr, 0, 0, out, 64, 2);
        CHECK(e.isPlaying());
    }
    {
        // Without a budget (the default: tests, the export), everything at once.
        ps::Engine e(kRate);
        for (int i = 0; i < 3000; ++i) CHECK(e.post({CommandType::SetChannelVolume, 0.5f, 0}));
        CHECK(e.post({CommandType::SetPlaying, 1.0f}));
        e.process(nullptr, 0, 0, out, 64, 2);
        CHECK(e.isPlaying());
    }
}

#include "gain_staging_tests.inc"
#include "studio_tests.inc"
#include "wide_tests.inc"
#include "mix_tests.inc"
#include "phase37_tests.inc"
#include "multizone_tests.inc"
#include "restes_tests.inc"
#include "review3539_tests.inc"
#include "leads_tests.inc"
#include "analysis_tests.inc"
#include "livefade_tests.inc"
#include "layers_tests.inc"
#include "basses_tests.inc"
#include "s7_tests.inc"
#include "leads2_tests.inc"
#include "audit_moteur_tests.inc"
#include "voice_tests.inc"
#include "triplets_tests.inc"
#include "tempo_tests.inc"

int main(int argc, char** argv) {
    const char* only = nullptr;  // a test's name: run that one alone
    for (int i = 1; i < argc; ++i) {
        if (std::strcmp(argv[i], "--update-references") == 0) g_updateReferences = true;
        else only = argv[i];
    }
    const TestCase tests[] = {
        PS_MASTER_TESTS
        {"spsc_queue", testSpscQueue},
        {"pad_strips", testPadStrips},
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
        {"position_during_count_in", testPositionDuringTheCountIn},
        {"hot_master_filter", testHotMasterFilterKeepsPlaying},
        {"big_blocks_lose_no_hits", testBigBlocksLoseNoHits},
        {"sustain_releases_every_note", testSustainPedalReleasesEveryNote},
        {"metronome_sample_accurate", testMetronomeIsSampleAccurate},
        {"every_drum_voice", testEveryDrumVoiceSoundsAndDecays},
        {"kit_on_dirty_memory", testKitOnDirtyMemoryStaysFinite},
        {"drum_velocity", testVelocityChangesLoudness},
        {"hat_choke", testClosedHatChokesOpenHat},
        {"pattern_drives_drums", testPatternDrivesDrums},
        {"full_kit_cpu", testFullKitCpuBudget},
        {"kicks_are_punchy", testKicksArePunchy},
        {"toms_have_body", testTomsHaveBody},
        {"acid_step_packing", testAcidStepPacking},
        {"acid_notes_on_step", testAcidNotesStartOnTheStep},
        {"acid_accent", testAcidAccentIsLouder},
        {"acid_drive_leaves_no_dc", testAcidDriveLeavesNoDc},
        {"acid_slide", testAcidSlideHoldsTheNote},
        {"acid_stops_cleanly", testAcidStopsCleanly},
        {"acid_cpu", testAcidCpuBudget},
        {"acid_knob_automation", testAcidKnobAutomation},
        {"automation_hold_takes_over", testAutomationHoldTakesOver},
        {"new_effect_in_a_slot_is_heard", testNewEffectInASlotIsHeard},
        {"acid_automation_on_step", testAcidAutomationWaitsForTheStep},
        {"synth_pitch", testSynthPitchIsRight},
        {"synth_on_time_and_release", testSynthNoteStartsOnTimeAndReleases},
        {"synth_voices_and_cpu", testSynthVoiceStealingAndCpu},
        {"synth_through_engine", testSynthThroughEngine},
        {"synth_unison", testSynthUnison},
        {"synth_glide", testSynthGlide},
        {"synth_sweep_follows_tempo", testSynthSweepFollowsTheTempo},
        {"mixer_pan_mute_solo", testMixerPanMuteSolo},
        {"limiter_ceiling", testLimiterHoldsTheCeiling},
        {"limiter_looks_ahead", testLimiterLooksAhead},
        {"new_projects_have_headroom", testNewProjectsHaveHeadroom},
        {"insert_swap_has_no_click", testInsertSwapHasNoClick},
        {"metronome_after_the_master_chain", testMetronomeAfterTheMasterChain},
        {"wav_writer", testWavWriterRoundTrip},
        {"offline_export", testOfflineExportRendersTheBeat},
        {"offline_fade_out", testOfflineExportFadesOutTheEnd},
        {"stems_add_up_to_the_mix", testStemsAddUpToTheMix},
        {"offline_auto_tail", testOfflineExportAutoTail},
        {"sample_resample", testSampleResamplesCleanly},
        {"pad_sample", testPadSampleReplacesTheDrum},
        {"sample_refs", testSampleReferencesAreNeverLeakedOrFreedEarly},
        {"sample_retrigger", testRetriggerFadesInsteadOfClicking},
        {"pad_pitch_region_reverse", testPadPitchRegionAndReverse},
        {"pad_loop_envelope_choke", testPadLoopEnvelopeAndChoke},
        {"pad_param_commands", testPadParamCommandsReachTheSampler},
        {"pad_sound_swap", testPadSoundSwapFadesAndKeepsTheLoop},
        {"arp_keeps_keys_across_stop", testArpeggioKeepsHeldKeysAcrossStop},
        {"gm_pitch_bend", testGeneralMidiPitchBend},
        {"sound_swaps_never_allocate", testSoundSwapsNeverAllocate},
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
        {"song-end", testSongEndsOnTheBar},
        {"performance_fx", testPerformanceEffects},
        {"performance_filter_fades", testPerformanceFilterFadesInAndOut},
        {"voice_stealing_spares_held", testVoiceStealingSparesHeldNotes},
        {"note_track_keeps_note_offs", testNoteTrackKeepsNoteOffs},
        {"touching_notes_never_glide", testTouchingNotesNeverGlide},
        {"tempo_ignores_non_finite", testTempoIgnoresNonFiniteValues},
        {"plaits", testPlaitsVoices},
        {"plaits_engine", testPlaitsThroughEngine},
        {"sound_calibration_gain", testSoundCalibrationGain},
        {"plaits_two_threads", testPlaitsOnTwoThreads},
        {"tempo_and_key", testTempoAndKeyDetection},
        {"onsets_vs_aubio", testOnsetsMatchAubio},
        {"tempo_vs_aubio", testTempoMatchesAubio},
        {"key_vs_aubio", testKeyMatchesAubio},
        {"analysis_edge_cases", testAnalysisEdgeCases},
        {"stems_follow_transport", testStemsPlayInStepWithTheTransport},
        {"live_pads", testLivePadsLoopOnTheBeat},
        {"instrument_tracks", testInstrumentTracks},
        {"new_effects", testNewEffects},
        {"crazy_effects", testCrazyEffects},
        {"param_eq", testParametricEq},
        {"scopes", testScopes},
        {"automation_clips", testAutomationClips},
        {"expression_arp", testExpressionAndArpeggio},
        {"audio_tracks", testAudioTracks},
        {"song_audio_clip", testSongAudioClip},
        {"once_audio_clip", testOnceAudioClip},
        {"live_slot_launches_on_the_bar", testLiveSlotLaunchesOnTheBar},
        {"live_slots_keep_their_own_loops", testLiveSlotsKeepTheirOwnLoops},
        {"live_stop_is_emptied_content", testLiveStopIsEmptiedContent},
        {"live_last_command_wins", testLiveLastCommandWins},
        {"scene_swap_cancels_launches", testSceneSwapCancelsLaunches},
        {"per_beat_song_curves_of_a_long_set", testPerBeatSongCurvesOfALongSet},
        {"leave_song_keeps_the_grids_launches", testLeaveSongKeepsTheGridsLaunches},
        {"live_launch_from_stop_and_offset", testLiveLaunchFromStopAndOffset},
        {"live_remix_slot", testLiveRemixSlot},
        {"live_launches_allocate_nothing", testLiveLaunchesAllocateNothing},
        {"live_bus_return", testLiveBusReturn},
        {"performance_song_matches_the_take", testPerformanceSongMatchesTheTake},
        {"mark_song_sets_the_song_position", testMarkSongSetsTheSongPosition},
        {"per_beat_song_curves", testPerBeatSongCurves},
        {"track_chopper", testTrackChopper},
        {"slot_offset_follows_staging", testSlotOffsetFollowsStaging},
        {"song_part_mutes_the_remix", testSongPartMutesTheRemix},
        {"song_curve_ramp", testSongCurveRamp},
        {"track_chopper_starts_in_phase", testTrackChopperStartsInPhase},
        {"live_fx_start_on_the_grid", testLiveFxStartOnTheGrid},
        {"live_repeat_on_the_grid", testLiveRepeatOnTheGrid},
        {"live_brake_and_restart", testLiveBrakeAndRestart},
        {"live_backspin_reverse_half_time", testLiveBackspinReverseHalfTime},
        {"live_echo_out", testLiveEchoOut},
        {"live_kills", testLiveKills},
        {"live_riser", testLiveRiser},
        {"live_build_up_and_drop", testLiveBuildUpAndDrop},
        {"live_freeze_and_throws", testLiveFreezeAndThrows},
        {"wobble_follows_the_song", testWobbleFollowsTheSong},
        {"live_fx_song_matches_the_take", testLiveFxSongMatchesTheTake},
        {"live_drop_then_stop", testLiveDropThenStop},
        {"live_gesture_after_a_release", testLiveGestureAfterARelease},
        {"live_drop_has_no_click", testLiveDropHasNoClick},
        {"live_build_up_and_drop_song_matches_the_take", testLiveBuildUpAndDropSongMatchesTheTake},
        {"insert_plays_in_some_scenes", testInsertPlaysInSomeScenes},
        {"song_from_a_later_part", testSongStartsFromALaterPart},
        {"scene_alone_outside_song", testSceneAloneIsOutsideTheSong},
        {"recording", testRecordingStartsOnTheFirstStep},
        {"three_four", testThreeFourAndPartMutes},
        {"live_pads_bar", testLivePadsFollowTheBar},
        {"stem_preview", testStemPreviewPlaysWithoutTheTransport},
        {"stems_stretch", testStemsStretchToTheProjectTempo},
        {"stems_keep_place_across_tempo", testStemsKeepTheirPlaceAcrossATempoChange},
        {"stems_come_in_later", testStemsComeInLaterInTheSong},
        {"play_position", testPlayPositionForLiveRecording},
        {"song_export", testSongExportSwapsScenesOnTime},
        {"drum_rolls", testDrumRollsSplitTheStep},
        {"drum_pan_pitch", testDrumPanAndStepPitch},
        {"track_macros", testTrackMacros},
        {"sampled_instrument", testSampledInstrument},
        {"no_alloc_in_process", testProcessNeverAllocates},
        {"electronic_effects", testElectronicEffects},
        {"eight_insert_slots", testEightInsertSlots},
        {"eight_inserts_cpu", testEightInsertsCpuWorstCase},
        {"long_effects_follow_the_app", testLongEffectsFollowTheApp},
        {"live_noises_stem_adds_up", testLiveNoisesStemAddsUp},
        {"echo_times_at_slow_tempos", testEchoTimesAtSlowTempos},
        {"rolls_on_swung_steps_stay_inside", testRollsOnSwungStepsStayInside},
        {"master_nan_guard_reports_and_recovers", testMasterNanGuardReportsAndRecovers},
        {"nan_settings_are_ignored", testNanSettingsAreIgnored},
        {"render_is_block_size_independent", testRenderIsBlockSizeIndependent},
        {"lfos_restart_with_the_transport", testLfosRestartWithTheTransport},
        {"live_launch_grace", testLiveLaunchGraceTakesTheLineJustPassed},
        {"synth_note_has_its_pitch_from_its_first_frame", testSynthNoteHasItsPitchFromItsFirstFrame},
        {"command_budget_per_callback", testCommandBudgetPerCallback},
        {"glue_chains_flat_under_threshold", gain_staging::testGlueChainsAreFlatUnderTheThreshold},
        {"compressor_makeup_detector_mix", gain_staging::testCompressorMakeUpDetectorAndMix},
        {"compressor_reduction_meter", gain_staging::testCompressorReductionMeter},
        {"saturation_tone_and_compensation", gain_staging::testSaturationToneAndCompensation},
        {"kit_version_levels", gain_staging::testKitVersionLevels},
        {"step_offsets_and_rolls", studio_tests::testStepOffsetsAndRolls},
        {"bass_808", studio_tests::testBass808},
        {"bass_808_in_the_engine", studio_tests::testBass808ThroughTheEngine},
        {"studio_kicks", studio_tests::testStudioKicks},
        {"kit_v3", studio_tests::testKitV3},
        {"pad_layers", studio_tests::testPadLayers},
        {"kit_v3_cpu", studio_tests::testKitV3CpuBudget},
        {"master_volume_automation", studio_tests::testMasterVolumeAutomation},
        {"pad_solo", studio_tests::testPadSolo},
        PS_WIDE_TESTS
        PS_MIX_TESTS
        PS_PHASE37_TESTS
        PS_MULTIZONE_TESTS
        PS_RESTES_TESTS
        PS_VOICE_TESTS
        PS_REVIEW3539_TESTS
        PS_LEADS_TESTS
        PS_LIVEFADE_TESTS
        PS_LAYERS_TESTS
        PS_BASSES_TESTS
        PS_S7_TESTS
        PS_LEADS2_TESTS
        PS_AUDIT_MOTEUR_TESTS
        PS_TRIPLETS_TESTS
        PS_TEMPO4E_TESTS
    };
    for (const auto& t : tests) {
        if (only && std::strcmp(only, t.name) != 0) continue;
        const int before = g_failures;
        t.fn();
        std::printf("[%s] %s\n", g_failures == before ? " OK " : "FAIL", t.name);
    }
    std::printf("%s (%d failure%s)\n", g_failures == 0 ? "ALL PASSED" : "FAILURES", g_failures,
                g_failures == 1 ? "" : "s");
    return g_failures == 0 ? 0 : 1;
}
