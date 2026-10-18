// Host-side tests for the platform-independent audio core.
// Run with scripts/host-tests.sh; pass --update-references to regenerate golden renders.

#include <cmath>
#include <cstdio>
#include <chrono>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <functional>
#include <random>
#include <string>
#include <tuple>
#include <vector>

#include "AcidTrack.h"
#include "DrumKit.h"
#include "Engine.h"
#include "RoundTripMeter.h"
#include "Sequencer.h"
#include "SpscFloatRing.h"
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
