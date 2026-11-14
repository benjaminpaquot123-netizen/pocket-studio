#pragma once
// Deterministic audio corpus for the song-analysis tests (onsets, tempo, key). Everything is
// synthesised here (no files to ship), with its own random generator so the sound is the same on
// every compiler; plus the real samples already committed under app/src/test/resources.
// The reference outputs of the previous analyser (aubio) on this corpus are in
// reference/analysis_aubio.json (written by analysis_reference.cpp).

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>
#include <vector>

namespace corpus {

struct Noise {
    uint32_t s;
    explicit Noise(uint32_t seed) : s(seed * 2654435761u + 1u) {}
    float next() {  // -1..1, xorshift32
        s ^= s << 13;
        s ^= s >> 17;
        s ^= s << 5;
        return static_cast<float>(s) / 2147483648.0f - 1.0f;
    }
};

inline void addKick(std::vector<float>& x, double at, int rate, float gain, Noise& n) {
    const auto start = static_cast<int64_t>(at * rate);
    double phase = 0;
    for (int i = 0; i < rate * 35 / 100 && start + i < static_cast<int64_t>(x.size()); ++i) {
        const double t = static_cast<double>(i) / rate;
        phase += 2 * M_PI * (48.0 + 90.0 * std::exp(-t / 0.025)) / rate;
        float v = static_cast<float>(std::sin(phase) * std::exp(-t / 0.11));
        if (t < 0.003) v += 0.3f * n.next() * static_cast<float>(1.0 - t / 0.003);
        x[static_cast<size_t>(start + i)] += gain * v;
    }
}

inline void addSnare(std::vector<float>& x, double at, int rate, float gain, Noise& n) {
    const auto start = static_cast<int64_t>(at * rate);
    for (int i = 0; i < rate / 4 && start + i < static_cast<int64_t>(x.size()); ++i) {
        const double t = static_cast<double>(i) / rate;
        const double v = 0.55 * n.next() * std::exp(-t / 0.055) + 0.45 * std::sin(2 * M_PI * 185.0 * t) * std::exp(-t / 0.035);
        x[static_cast<size_t>(start + i)] += gain * static_cast<float>(v);
    }
}

inline void addHat(std::vector<float>& x, double at, int rate, float gain, Noise& n) {
    const auto start = static_cast<int64_t>(at * rate);
    float prev = 0;
    for (int i = 0; i < rate / 16 && start + i < static_cast<int64_t>(x.size()); ++i) {
        const double t = static_cast<double>(i) / rate;
        const float w = n.next();
        x[static_cast<size_t>(start + i)] += gain * (w - prev) * 0.5f * static_cast<float>(std::exp(-t / 0.011));
        prev = w;
    }
}

// A note with a few harmonics (piano-ish decay when `pluck`, else a held pad).
inline void addNote(std::vector<float>& x, double at, double length, int midi, int rate, float gain, int harmonics, bool pluck) {
    const auto start = static_cast<int64_t>(at * rate);
    const auto len = static_cast<int64_t>(length * rate);
    const double f = 440.0 * std::pow(2.0, (midi - 69) / 12.0);
    for (int64_t i = 0; i < len && start + i < static_cast<int64_t>(x.size()); ++i) {
        const double t = static_cast<double>(i) / rate;
        double env = pluck ? std::exp(-t / 0.5) : std::min(1.0, t / 0.05) * std::min(1.0, (length - t) / 0.05);
        double v = 0;
        for (int h = 1; h <= harmonics; ++h) {
            if (f * h > rate * 0.45) break;
            v += std::sin(2 * M_PI * f * h * t) / h;
        }
        x[static_cast<size_t>(start + i)] += gain * static_cast<float>(env * v);
    }
}

// ---- Tempo ----

enum class Groove { Four, Break, Trap, HipHop, Pop };

struct TempoCase {
    const char* name;
    Groove groove;
    double bpm;
    int rate;
    double start;  // seconds before the first beat
};

inline std::vector<TempoCase> tempoCases() {
    return {
        {"four_120", Groove::Four, 120.0, 48000, 0.10},   {"four_124", Groove::Four, 124.0, 44100, 0.25},
        {"four_126_5", Groove::Four, 126.5, 48000, 0.00}, {"four_128", Groove::Four, 128.0, 44100, 0.40},
        {"four_132", Groove::Four, 132.0, 48000, 0.05},   {"four_138", Groove::Four, 138.0, 44100, 0.30},
        {"four_145", Groove::Four, 145.0, 48000, 0.20},   {"break_165", Groove::Break, 165.0, 44100, 0.10},
        {"break_170", Groove::Break, 170.0, 48000, 0.30}, {"break_174", Groove::Break, 174.0, 44100, 0.00},
        {"trap_140", Groove::Trap, 140.0, 48000, 0.15},   {"trap_150", Groove::Trap, 150.0, 44100, 0.05},
        {"hiphop_88", Groove::HipHop, 88.0, 48000, 0.20}, {"hiphop_92", Groove::HipHop, 92.0, 44100, 0.00},
        {"hiphop_97_3", Groove::HipHop, 97.3, 48000, 0.35}, {"pop_100", Groove::Pop, 100.0, 44100, 0.10},
        {"pop_105", Groove::Pop, 105.0, 48000, 0.25},     {"pop_110", Groove::Pop, 110.0, 44100, 0.05},
        {"pop_115", Groove::Pop, 115.0, 48000, 0.00},     {"four_75", Groove::Pop, 75.0, 44100, 0.30},
        {"pop_82", Groove::Pop, 82.0, 48000, 0.12},       {"four_118_2", Groove::Four, 118.2, 48000, 0.18},
    };
}

// `seconds` of the groove, with a bass line and chords, and the times of every drum hit.
inline std::vector<float> renderGroove(const TempoCase& c, double seconds, std::vector<double>* hits = nullptr, uint32_t seed = 1) {
    std::vector<float> x(static_cast<size_t>(seconds * c.rate), 0.0f);
    Noise n(seed + static_cast<uint32_t>(c.bpm * 10));
    const double beat = 60.0 / c.bpm;
    const double s16 = beat / 4;
    std::vector<double> at;
    auto kick = [&](double t, float g) { addKick(x, t, c.rate, g, n); at.push_back(t); };
    auto snare = [&](double t, float g) { addSnare(x, t, c.rate, g, n); at.push_back(t); };
    auto hat = [&](double t, float g) { addHat(x, t, c.rate, g, n); at.push_back(t); };
    static const int roots[4] = {45, 41, 48, 43};  // A F C G
    int bar = 0;
    for (double b0 = c.start; b0 + 4 * beat < seconds - 0.3; b0 += 4 * beat, ++bar) {
        const int root = roots[bar % 4];
        for (int s = 0; s < 16; ++s) {
            const double t = b0 + s * s16;
            switch (c.groove) {
                case Groove::Four:
                    if (s % 4 == 0) kick(t, 0.9f);
                    if (s == 4 || s == 12) snare(t, 0.5f);
                    if (s % 4 == 2) hat(t, 0.35f);
                    else if (s % 2 == 1) hat(t, 0.12f);
                    break;
                case Groove::Break:  // two-step: kick 1 and 3+, snare 2 and 4
                    if (s == 0 || s == 10) kick(t, 0.9f);
                    if (s == 4 || s == 12) snare(t, 0.7f);
                    if (s % 2 == 0) hat(t, 0.2f);
                    break;
                case Groove::Trap:  // half-time snare on 3, rolling hats
                    if (s == 0 || s == 7 || s == 11) kick(t, 0.9f);
                    if (s == 8) snare(t, 0.7f);
                    hat(t, s % 2 == 0 ? 0.25f : 0.12f);
                    break;
                case Groove::HipHop: {
                    const double swing = s % 2 == 1 ? s16 * 0.15 : 0.0;
                    if (s == 0 || s == 6 || s == 10) kick(t + swing, 0.9f);
                    if (s == 4 || s == 12) snare(t + swing, 0.6f);
                    if (s % 2 == 0) hat(t + swing, 0.22f);
                    break;
                }
                case Groove::Pop:
                    if (s == 0 || s == 8 || s == 10) kick(t, 0.85f);
                    if (s == 4 || s == 12) snare(t, 0.6f);
                    if (s % 2 == 0) hat(t, 0.18f);
                    break;
            }
        }
        for (int q = 0; q < 4; ++q) addNote(x, b0 + q * beat, beat * 0.9, root - 12 + 12 * (q % 2), c.rate, 0.18f, 3, true);
        for (int k : {0, 4, 7}) addNote(x, b0, 4 * beat, root + 12 + k - (k == 4 && bar % 4 == 0 ? 1 : 0), c.rate, 0.05f, 5, false);
    }
    if (hits) *hits = at;
    return x;
}

// ---- Key ----

struct KeyCase {
    std::string name;
    int tonic;   // 0 = C … 11 = B
    bool minor;
    int variant; // progression, voicing and timbre
    int rate;
};

inline std::vector<KeyCase> keyCases() {
    static const char* names[12] = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
    std::vector<KeyCase> out;
    for (int variant = 0; variant < 2; ++variant) {
        for (int minor = 0; minor < 2; ++minor) {
            for (int t = 0; t < 12; ++t) {
                out.push_back({std::string(names[t]) + (minor ? "m" : "") + "_v" + std::to_string(variant), t, minor == 1, variant,
                               (t + variant) % 2 ? 44100 : 48000});
            }
        }
    }
    return out;
}

// About 16 s of a chord progression in the key, with bass (and drums for variant 1).
inline std::vector<float> renderKey(const KeyCase& c) {
    const double seconds = 16.0;
    std::vector<float> x(static_cast<size_t>(seconds * c.rate), 0.0f);
    Noise n(static_cast<uint32_t>(c.tonic * 7 + c.minor * 3 + c.variant * 101 + 5));
    // Scale degrees as semitone offsets; chords as (root offset, third, fifth).
    struct Chord { int root, third, fifth; };
    std::vector<Chord> prog;
    if (!c.minor) {
        if (c.variant == 0) prog = {{0, 4, 7}, {5, 4, 7}, {7, 4, 7}, {0, 4, 7}};   // I IV V I
        else prog = {{0, 4, 7}, {9, 3, 7}, {5, 4, 7}, {7, 4, 7}};                  // I vi IV V
    } else {
        if (c.variant == 0) prog = {{0, 3, 7}, {5, 3, 7}, {7, 4, 7}, {0, 3, 7}};   // i iv V i (harmonic)
        else prog = {{0, 3, 7}, {8, 4, 7}, {3, 4, 7}, {10, 4, 7}};                 // i VI III VII
    }
    const int base = 48 + c.tonic - (c.tonic > 6 ? 12 : 0);  // around C3
    const double chordLen = 2.0;
    int i = 0;
    for (double t = 0; t + chordLen <= seconds; t += chordLen, ++i) {
        const Chord& ch = prog[static_cast<size_t>(i) % prog.size()];
        const int r = base + ch.root;
        addNote(x, t, chordLen, r - 12, c.rate, 0.25f, 4, c.variant == 1);
        const int voicing = (i / 4 + c.variant) % 2;
        addNote(x, t, chordLen, r + 12 * voicing, c.rate, 0.10f, c.variant ? 8 : 4, false);
        addNote(x, t, chordLen, r + ch.third + 12, c.rate, 0.10f, c.variant ? 8 : 4, false);
        addNote(x, t, chordLen, r + ch.fifth + 12 * voicing, c.rate, 0.10f, c.variant ? 8 : 4, false);
        // A melody on scale notes (tonic, third, fifth of the key) for colour.
        const int scale[3] = {0, c.minor ? 3 : 4, 7};
        for (int k = 0; k < 4; ++k) addNote(x, t + k * 0.5, 0.45, base + 24 + scale[(i + k) % 3], c.rate, 0.06f, 3, true);
        if (c.variant == 1) {
            for (int k = 0; k < 4; ++k) addKick(x, t + k * 0.5, c.rate, 0.5f, n);
            addSnare(x, t + 0.5, c.rate, 0.3f, n);
            addSnare(x, t + 1.5, c.rate, 0.3f, n);
        }
    }
    return x;
}

// ---- Onsets ----

struct OnsetCase {
    std::string name;
    int rate;
    std::vector<float> mono;
    std::vector<double> truth;  // attack times (s); empty for real samples
};

// Short drum excerpts (2 bars at most, like a sliced loop) and single-instrument phrases.
inline std::vector<OnsetCase> syntheticOnsetCases() {
    std::vector<OnsetCase> out;
    for (const auto& c : tempoCases()) {
        const double seconds = std::min(4.0, 2 * 4 * 60.0 / c.bpm + 0.3);
        std::vector<double> hits;
        auto x = renderGroove(c, seconds, &hits, 3);
        out.push_back({std::string("loop_") + c.name, c.rate, std::move(x), {}});
        std::sort(hits.begin(), hits.end());
        auto& truth = out.back().truth;
        for (double h : hits) {
            if (truth.empty() || h - truth.back() > 0.001) truth.push_back(h);
        }
    }
    // A plucked melody (no drums): note attacks only.
    for (int v = 0; v < 4; ++v) {
        const int rate = v % 2 ? 44100 : 48000;
        std::vector<float> x(static_cast<size_t>(rate * 3), 0.0f);
        std::vector<double> truth;
        double t = 0.05 + 0.03 * v;
        for (int k = 0; t < 2.6; ++k) {
            addNote(x, t, 0.6, 52 + (k * 5 + v * 3) % 17, rate, 0.4f, 6, true);
            truth.push_back(t);
            t += 0.18 + 0.07 * ((k + v) % 3);
        }
        out.push_back({"pluck_" + std::to_string(v), rate, std::move(x), truth});
    }
    return out;
}

}  // namespace corpus
