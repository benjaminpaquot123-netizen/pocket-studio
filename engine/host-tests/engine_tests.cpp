// Host-side tests for the platform-independent audio core.
// Run with scripts/host-tests.sh; pass --update-references to regenerate golden renders.

#include <cmath>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <functional>
#include <random>
#include <string>
#include <vector>

#include "Engine.h"
#include "RoundTripMeter.h"
#include "SpscQueue.h"

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

void testStartRampsUp() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 1000.0f});
    engine.post({ps::CommandType::SetGain, 1.0f});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    const auto out = render(engine, kRate / 10, 256);
    // 1 ms after start the 10 ms ramp is still below 20 %; a hard start would already be at full scale.
    float firstMs = 0.0f;
    for (int i = 0; i < kRate / 1000; ++i) firstMs = std::max(firstMs, std::fabs(out[static_cast<size_t>(i) * kChannels]));
    CHECK(firstMs < 0.2f);
    float lastMs = 0.0f;
    for (int i = kRate / 10 - kRate / 1000; i < kRate / 10; ++i) {
        lastMs = std::max(lastMs, std::fabs(out[static_cast<size_t>(i) * kChannels]));
    }
    CHECK(lastMs > 0.99f);
    float maxStep = 0.0f;
    for (int i = 1; i < kRate / 10; ++i) {
        maxStep = std::max(maxStep, std::fabs(out[static_cast<size_t>(i) * kChannels] -
                                              out[static_cast<size_t>(i - 1) * kChannels]));
    }
    CHECK(maxStep <= naturalStep(1000.0f) * 1.01f);
}

void testStopFadesOut() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetFrequency, 1000.0f});
    engine.post({ps::CommandType::SetGain, 1.0f});
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    // Advance until the waveform sits near a peak, where a hard cut would click loudly.
    auto before = render(engine, 12000, 256);
    for (int i = 0; i < 48 && std::fabs(before[before.size() - kChannels]) < 0.95f; ++i) before = render(engine, 1, 1);
    CHECK(std::fabs(before[before.size() - kChannels]) >= 0.95f);
    engine.post({ps::CommandType::SetPlaying, 0.0f});
    const auto out = render(engine, kRate / 4, 256);
    float previous = before[before.size() - kChannels];
    float maxStep = 0.0f;
    for (size_t i = 0; i < out.size() / kChannels; ++i) {
        maxStep = std::max(maxStep, std::fabs(out[i * kChannels] - previous));
        previous = out[i * kChannels];
    }
    CHECK(maxStep <= naturalStep(1000.0f) * 1.01f);
    // Still sounding 1 ms after the stop (fading, not cut)...
    float firstMs = 0.0f;
    for (int i = 0; i < kRate / 1000; ++i) firstMs = std::max(firstMs, std::fabs(out[static_cast<size_t>(i) * kChannels]));
    CHECK(firstMs > 0.7f);
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

struct TestCase {
    const char* name;
    std::function<void()> fn;
};

}  // namespace

int main(int argc, char** argv) {
    for (int i = 1; i < argc; ++i) {
        if (std::strcmp(argv[i], "--update-references") == 0) g_updateReferences = true;
    }
    const TestCase tests[] = {
        {"spsc_queue", testSpscQueue},
        {"silent_when_stopped", testSilentWhenStopped},
        {"sine_four_bars", testSineFourBars},
        {"block_size_independence", testBlockSizeIndependence},
        {"start_ramps_up", testStartRampsUp},
        {"stop_fades_out", testStopFadesOut},
        {"settings_survive_prepare", testSettingsSurvivePrepare},
        {"reference_render", testReferenceRender},
        {"round_trip_meter", testRoundTripMeter},
        {"latency_test_through_engine", testLatencyTestThroughEngine},
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
