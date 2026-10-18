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

    // Fade-in must not click: max sample-to-sample step stays small.
    float maxStep = 0.0f;
    for (int i = 1; i < kRate / 10; ++i) {
        maxStep = std::max(maxStep, std::fabs(out[static_cast<size_t>(i) * kChannels] -
                                              out[static_cast<size_t>(i - 1) * kChannels]));
    }
    CHECK(maxStep < 0.06f);
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

void testStopFadesOut() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    render(engine, kRate / 4, 256);
    engine.post({ps::CommandType::SetPlaying, 0.0f});
    const auto out = render(engine, kRate / 4, 256);
    float maxStep = 0.0f;
    for (size_t i = 1; i < out.size() / kChannels; ++i) {
        maxStep = std::max(maxStep, std::fabs(out[i * kChannels] - out[(i - 1) * kChannels]));
    }
    CHECK(maxStep < 0.06f);
    float tail = 0.0f;
    for (size_t i = out.size() - 200; i < out.size(); ++i) tail = std::max(tail, std::fabs(out[i]));
    CHECK(tail < 1e-4f);
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
ps::RoundTripMeter::Result simulateRoundTrip(int delayFrames, float gain, float noise, int blockSize) {
    ps::RoundTripMeter meter;
    meter.prepare(kRate);
    meter.start();
    std::vector<float> line(static_cast<size_t>(meter.recordLength() + delayFrames + blockSize), 0.0f);
    std::mt19937 rng(42);
    std::normal_distribution<float> dist(0.0f, noise);
    std::vector<float> out(static_cast<size_t>(blockSize) * kChannels);
    std::vector<float> in(static_cast<size_t>(blockSize));
    int pos = 0;
    while (meter.state() == ps::RoundTripMeter::State::Running) {
        for (int i = 0; i < blockSize; ++i) in[static_cast<size_t>(i)] = line[static_cast<size_t>(pos + i)] + dist(rng);
        meter.process(in.data(), blockSize, 1, out.data(), blockSize, kChannels);
        for (int i = 0; i < blockSize; ++i) {
            const size_t target = static_cast<size_t>(pos + i + delayFrames);
            if (target < line.size()) line[target] += gain * out[static_cast<size_t>(i) * kChannels];
        }
        pos += blockSize;
    }
    CHECK(meter.state() == ps::RoundTripMeter::State::Done);
    return meter.analyze();
}

void testRoundTripMeter() {
    const auto r = simulateRoundTrip(1234, 0.05f, 0.01f, 192);
    CHECK(r.valid);
    CHECK(std::abs(r.latencyFrames - 1234) <= 1);
    CHECK(std::fabs(r.latencyMs - 1234.0 * 1000.0 / kRate) < 0.05);

    const auto r2 = simulateRoundTrip(480, 0.3f, 0.0f, 256);
    CHECK(r2.valid);
    CHECK(r2.latencyFrames == 480);

    const auto silent = simulateRoundTrip(1000, 0.0f, 0.01f, 256);
    CHECK(!silent.valid);
}

void testLatencyTestThroughEngine() {
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetPlaying, 1.0f});
    engine.post({ps::CommandType::StartLatencyTest, 0.0f});
    std::vector<float> out(256 * kChannels);
    std::vector<float> in(256, 0.0f);
    int guard = 0;
    do {
        engine.process(in.data(), 256, 1, out.data(), 256, kChannels);
    } while (engine.latencyMeter().state() != ps::RoundTripMeter::State::Done && ++guard < 10000);
    CHECK(engine.latencyMeter().state() == ps::RoundTripMeter::State::Done);
    CHECK(!engine.latencyMeter().analyze().valid);
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
        {"stop_fades_out", testStopFadesOut},
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
