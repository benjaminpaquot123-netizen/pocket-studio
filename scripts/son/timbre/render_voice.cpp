// Host tool for the timbre study (docs/sons/fin/RECHERCHE-TIMBRE.md). Bench only: nothing in the
// app uses it. It plays the engine's measured drum voice exactly as the app does, so a parameter
// search runs on the real voice, and it times candidate building blocks.
//
//   render_voice list            prints "family style <19 recipe numbers>" for the 54 chosen recipes
//   render_voice render FRAMES   reads recipes on stdin (19 numbers a line), writes float32 mono
//   render_voice bench           prints the cost of the voice and of candidate blocks (ns a sample)
#include <chrono>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <vector>

#include "MeasuredDrums.h"

using ps::measured::Recipe;
using ps::measured::Voice;

namespace {

constexpr int kRate = 48000;
constexpr int kFields = 19;

void toFields(const Recipe& r, double* f) {
    const double v[kFields] = {r.bodyLevel, r.startHz, r.endHz, r.dropHalfMs, r.bodyHoldMs, r.bodyFallMs,
                               r.bodyDrive, r.noiseLevel, r.noiseHpHz, r.noiseLpHz, r.noiseHoldMs,
                               r.noiseFallMs, r.metalLevel, r.metalHz, static_cast<double>(r.bursts),
                               r.burstGapMs, r.clickLevel, r.clickMs, r.clickHpHz};
    std::memcpy(f, v, sizeof v);
}

Recipe fromFields(const double* f) {
    Recipe r;
    r.bodyLevel = static_cast<float>(f[0]);
    r.startHz = static_cast<float>(f[1]);
    r.endHz = static_cast<float>(f[2]);
    r.dropHalfMs = static_cast<float>(f[3]);
    r.bodyHoldMs = static_cast<float>(f[4]);
    r.bodyFallMs = static_cast<float>(f[5]);
    r.bodyDrive = static_cast<float>(f[6]);
    r.noiseLevel = static_cast<float>(f[7]);
    r.noiseHpHz = static_cast<float>(f[8]);
    r.noiseLpHz = static_cast<float>(f[9]);
    r.noiseHoldMs = static_cast<float>(f[10]);
    r.noiseFallMs = static_cast<float>(f[11]);
    r.metalLevel = static_cast<float>(f[12]);
    r.metalHz = static_cast<float>(f[13]);
    r.bursts = static_cast<int>(f[14]);
    r.burstGapMs = static_cast<float>(f[15]);
    r.clickLevel = static_cast<float>(f[16]);
    r.clickMs = static_cast<float>(f[17]);
    r.clickHpHz = static_cast<float>(f[18]);
    return r;  // gain stays 1: the measures are level-free
}

int list() {
    for (int fam = 0; fam < ps::measured::kFamilies; ++fam) {
        for (int st = 0; st < ps::measured::kStyles; ++st) {
            double f[kFields];
            toFields(ps::measured::chosen(fam, st), f);
            std::printf("%d %d", fam, st);
            for (double v : f) std::printf(" %.6g", v);
            std::printf("\n");
        }
    }
    return 0;
}

int render(int frames) {
    std::vector<float> out(static_cast<size_t>(frames));
    char line[1024];
    while (std::fgets(line, sizeof line, stdin)) {
        double f[kFields];
        char* p = line;
        int n = 0;
        for (; n < kFields; ++n) {
            char* end = nullptr;
            f[n] = std::strtod(p, &end);
            if (end == p) break;
            p = end;
        }
        if (n != kFields) continue;
        Voice v;  // a fresh voice: the same noise sequence for every recipe
        v.prepare(kRate);
        v.setRecipe(fromFields(f));
        v.trigger(1.0f);
        std::fill(out.begin(), out.end(), 0.0f);
        for (int at = 0; at < frames; at += 256) v.render(out.data() + at, std::min(256, frames - at));
        if (std::fwrite(out.data(), sizeof(float), out.size(), stdout) != out.size()) return 1;
    }
    return 0;
}

// --- Candidate blocks, written the way the audio thread would run them: fixed arrays, no
// allocation, no lock, no log. They are sketches for timing, not the final voices.

uint32_t gNoise = 0x9E3779B9u;
inline float white() {
    gNoise ^= gNoise << 13;
    gNoise ^= gNoise >> 17;
    gNoise ^= gNoise << 5;
    return static_cast<float>(static_cast<int32_t>(gNoise)) * (1.0f / 2147483648.0f);
}

// A bank of decaying modes: one two-multiply rotation and one decay per mode and per sample.
template <int N> struct Modes {
    float re[N], im[N], c[N], s[N], d[N];
    void trigger() {
        for (int k = 0; k < N; ++k) {
            const float hz = 150.0f * (1.0f + 0.61f * static_cast<float>(k));
            const float w = 6.28318531f * hz / kRate;
            c[k] = std::cos(w);
            s[k] = std::sin(w);
            d[k] = std::exp(-1.0f / (0.2f * kRate / (1.0f + 0.3f * static_cast<float>(k))));
            re[k] = 0.0f;
            im[k] = 1.0f / static_cast<float>(N);
        }
    }
    inline float tick() {
        float sum = 0.0f;
        for (int k = 0; k < N; ++k) {
            const float r = (re[k] * c[k] - im[k] * s[k]) * d[k];
            im[k] = (re[k] * s[k] + im[k] * c[k]) * d[k];
            re[k] = r;
            sum += r;
        }
        return sum;
    }
};

// Noise through N band-pass filters, each band with its own level and its own decay.
template <int N> struct BandNoise {
    float ic1[N], ic2[N], g[N], a1[N], a2[N], env[N], mul[N];
    void trigger() {
        for (int b = 0; b < N; ++b) {
            const float hz = 60.0f * std::pow(2.0f, 8.4f * static_cast<float>(b) / static_cast<float>(N - 1));
            g[b] = std::tan(3.14159265f * hz / kRate);
            a1[b] = 1.0f / (1.0f + g[b] * (g[b] + 1.0f / 1.4f));
            a2[b] = g[b] * a1[b];
            ic1[b] = ic2[b] = 0.0f;
            env[b] = 1.0f;
            mul[b] = std::exp(-1.0f / (0.05f * kRate * (1.0f + static_cast<float>(N - b))));
        }
    }
    inline float tick() {
        const float x = white();
        float sum = 0.0f;
        for (int b = 0; b < N; ++b) {
            const float v3 = x - ic2[b];
            const float v1 = a1[b] * ic1[b] + a2[b] * v3;
            const float v2 = ic2[b] + g[b] * v1;
            ic1[b] = 2.0f * v1 - ic1[b];
            ic2[b] = 2.0f * v2 - ic2[b];
            sum += v1 * env[b];
            env[b] *= mul[b];
        }
        return sum;
    }
};

// A short response played by direct convolution (the "synthesized short response" option).
template <int N> struct Fir {
    float h[N], z[2 * N];
    int at = 0;
    void trigger() {
        for (int i = 0; i < N; ++i) h[i] = white() * std::exp(-4.0f * static_cast<float>(i) / N);
        std::memset(z, 0, sizeof z);
        at = 0;
    }
    inline float tick() {
        const float x = white();
        z[at] = z[at + N] = x;
        const float* p = z + at + 1;
        float sum = 0.0f;
        for (int i = 0; i < N; ++i) sum += h[i] * p[i];
        at = (at + 1) % N;
        return sum;
    }
};

// Best of seven runs. The block is re-triggered every quarter second, like a drum in a pattern:
// left to decay for seconds, a mode falls into subnormal numbers and costs ten times more, which
// is why a real voice must stop (or floor its state) once it is inaudible.
template <typename B> void timeBlock(const char* name) {
    static B b;
    const int frames = kRate * 2;
    volatile float sink = 0.0f;
    double ns = 1e30;
    for (int rep = 0; rep < 7; ++rep) {
        const auto t0 = std::chrono::steady_clock::now();
        float acc = 0.0f;
        for (int i = 0; i < frames; ++i) {
            if (i % (kRate / 4) == 0) b.trigger();
            acc += b.tick();
        }
        const auto t1 = std::chrono::steady_clock::now();
        sink = sink + acc;
        ns = std::min(ns, std::chrono::duration<double, std::nano>(t1 - t0).count() / frames);
    }
    std::printf("%s\t%.2f\t%.3f\n", name, ns, ns * kRate / 1e7);
}

int bench() {
    std::printf("block\tns_per_sample\tpercent_of_one_core\n");
    {
        // The voice as it is: a snare (body, noise, click), re-triggered so it never goes idle.
        Voice v;
        v.prepare(kRate);
        v.setRecipe(ps::measured::recipe(ps::measured::Snare, ps::measured::Dubstep));
        const int block = 256, blocks = 400;
        float buf[256];
        volatile float sink = 0.0f;
        double best = 1e30;
        for (int rep = 0; rep < 7; ++rep) {
            const auto t0 = std::chrono::steady_clock::now();
            for (int b = 0; b < blocks; ++b) {
                if (b % 40 == 0) v.trigger(1.0f);
                std::memset(buf, 0, sizeof buf);
                v.render(buf, block);
                sink = sink + buf[17];
            }
            const auto t1 = std::chrono::steady_clock::now();
            best = std::min(best, std::chrono::duration<double, std::nano>(t1 - t0).count() / (block * blocks));
        }
        std::printf("voice_today\t%.2f\t%.3f\n", best, best * kRate / 1e7);
    }
    timeBlock<Modes<8>>("modes_8");
    timeBlock<Modes<16>>("modes_16");
    timeBlock<Modes<32>>("modes_32");
    timeBlock<BandNoise<9>>("band_noise_9");
    timeBlock<BandNoise<24>>("band_noise_24");
    timeBlock<Fir<64>>("fir_64");
    timeBlock<Fir<256>>("fir_256");
    timeBlock<Fir<2048>>("fir_2048");
    return 0;
}

}  // namespace

int main(int argc, char** argv) {
    if (argc >= 2 && !std::strcmp(argv[1], "list")) return list();
    if (argc >= 3 && !std::strcmp(argv[1], "render")) return render(std::atoi(argv[2]));
    if (argc >= 2 && !std::strcmp(argv[1], "bench")) return bench();
    std::fprintf(stderr, "usage: render_voice list | render FRAMES | bench\n");
    return 2;
}
