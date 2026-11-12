// Runs the song analysis (onsets, tempo, key) over the test corpus and writes the results as JSON.
//   analysis_reference <out.json> [extra.wav ...]
// It wrote reference/analysis_aubio.json while the engine still used aubio (GPL-3.0), so the
// in-house analysers can be checked against it (test "analysis_vs_aubio"). Extra WAVs (e.g. song
// renders) are analysed too and listed under "extra"; only the in-repo corpus is in the reference.
// For tuning: PS_TEMPO_ONLY=1 skips the key and onset corpus, PS_ONSETS_ONLY=1 the tempo and key one.

#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <string>
#include <vector>

#include "Analysis.h"
#include "OnsetDetector.h"
#include "analysis_corpus.h"

namespace {

std::vector<float> readMono(const std::string& path, int& rate) {
    std::vector<float> x;
    if (!ps::readWavMono(path, rate, [&](const float* b, int n) { x.insert(x.end(), b, b + n); })) x.clear();
    return x;
}

void writeOnsets(std::FILE* f, const std::vector<float>& x, int rate) {
    static const float sens[] = {0.25f, 0.5f, 0.75f};
    for (float s : sens) {
        ps::OnsetOptions o;
        o.sensitivity = s;
        o.maxSlices = 100000;
        const auto on = ps::detectOnsets(x.data(), static_cast<int64_t>(x.size()), rate, o);
        std::fprintf(f, "\"s%02d\": [", static_cast<int>(s * 100));
        for (size_t i = 0; i < on.size(); ++i) std::fprintf(f, "%s%lld", i ? ", " : "", static_cast<long long>(on[i]));
        std::fprintf(f, "], ");
    }
    const auto capped = ps::detectOnsets(x.data(), static_cast<int64_t>(x.size()), rate);
    std::fprintf(f, "\"capped16\": [");
    for (size_t i = 0; i < capped.size(); ++i) std::fprintf(f, "%s%lld", i ? ", " : "", static_cast<long long>(capped[i]));
    std::fprintf(f, "]");
}

}  // namespace

int main(int argc, char** argv) {
    if (argc < 2) {
        std::fprintf(stderr, "usage: analysis_reference <out.json> [extra.wav ...]\n");
        return 2;
    }
    std::FILE* f = std::fopen(argv[1], "w");
    if (!f) return 1;
    std::fprintf(f, "{\n\"tempo\": [\n");
    auto tempos = corpus::tempoCases();
    if (std::getenv("PS_ONSETS_ONLY")) tempos.clear();
    for (size_t i = 0; i < tempos.size(); ++i) {
        const auto& c = tempos[i];
        const auto x = corpus::renderGroove(c, 30.0);
        const auto r = ps::detectTempo(x.data(), static_cast<int64_t>(x.size()), c.rate);
        std::fprintf(f, "  {\"name\": \"%s\", \"truth\": %.2f, \"firstTruth\": %.3f, \"bpm\": %.2f, \"confidence\": %.3f, \"firstBeat\": %.4f}%s\n",
                     c.name, c.bpm, c.start, r.bpm, r.confidence, r.firstBeat, i + 1 < tempos.size() ? "," : "");
        std::printf("tempo %-12s truth %6.2f -> %6.2f (conf %.2f, first %.3f / %.3f)\n", c.name, c.bpm, r.bpm, r.confidence, r.firstBeat, c.start);
    }
    std::fprintf(f, "],\n\"key\": [\n");
    auto keys = corpus::keyCases();
    if (std::getenv("PS_TEMPO_ONLY") || std::getenv("PS_ONSETS_ONLY")) keys.clear();
    int keyRight = 0;
    for (size_t i = 0; i < keys.size(); ++i) {
        const auto& c = keys[i];
        const auto x = corpus::renderKey(c);
        const auto r = ps::detectKey(x.data(), static_cast<int64_t>(x.size()), c.rate);
        if (r.tonic == c.tonic && r.minor == c.minor) ++keyRight;
        std::fprintf(f, "  {\"name\": \"%s\", \"truthTonic\": %d, \"truthMinor\": %d, \"tonic\": %d, \"minor\": %d, \"confidence\": %.4f}%s\n",
                     c.name.c_str(), c.tonic, c.minor ? 1 : 0, r.tonic, r.minor ? 1 : 0, r.confidence, i + 1 < keys.size() ? "," : "");
    }
    std::printf("key: %d / %zu right\n", keyRight, keys.size());
    std::fprintf(f, "],\n\"onsets\": [\n");
    auto onsetCases = corpus::syntheticOnsetCases();
    if (std::getenv("PS_TEMPO_ONLY")) onsetCases.clear();
    const std::filesystem::path samples(PS_SAMPLES_DIR);
    std::vector<std::filesystem::path> wavs;
    for (const auto& e : std::filesystem::directory_iterator(samples)) {
        if (e.path().extension() == ".wav") wavs.push_back(e.path());
    }
    std::sort(wavs.begin(), wavs.end());
    for (const auto& p : wavs) {
        if (std::getenv("PS_TEMPO_ONLY")) break;
        corpus::OnsetCase c{"sample_" + p.stem().string(), 0, {}, {}};
        c.mono = readMono(p.string(), c.rate);
        if (!c.mono.empty()) onsetCases.push_back(std::move(c));
    }
    for (size_t i = 0; i < onsetCases.size(); ++i) {
        const auto& c = onsetCases[i];
        std::fprintf(f, "  {\"name\": \"%s\", \"rate\": %d, \"frames\": %zu, \"truth\": [", c.name.c_str(), c.rate, c.mono.size());
        for (size_t k = 0; k < c.truth.size(); ++k) std::fprintf(f, "%s%lld", k ? ", " : "", static_cast<long long>(std::llround(c.truth[k] * c.rate)));
        std::fprintf(f, "], ");
        writeOnsets(f, c.mono, c.rate);
        std::fprintf(f, "}%s\n", i + 1 < onsetCases.size() ? "," : "");
    }
    std::fprintf(f, "],\n\"extra\": [\n");
    for (int a = 2; a < argc; ++a) {
        int rate = 0;
        const auto x = readMono(argv[a], rate);
        const auto t = ps::detectTempo(x.data(), static_cast<int64_t>(x.size()), rate);
        const auto k = ps::detectKey(x.data(), static_cast<int64_t>(x.size()), rate);
        std::fprintf(f, "  {\"name\": \"%s\", \"bpm\": %.2f, \"confidence\": %.3f, \"firstBeat\": %.4f, \"tonic\": %d, \"minor\": %d, \"rate\": %d, ",
                     std::filesystem::path(argv[a]).filename().c_str(), t.bpm, t.confidence, t.firstBeat, k.tonic, k.minor ? 1 : 0, rate);
        // Onsets on the first 4 s (a loop-sized excerpt).
        const std::vector<float> excerpt(x.begin(), x.begin() + std::min<std::ptrdiff_t>(static_cast<std::ptrdiff_t>(x.size()), rate * 4));
        writeOnsets(f, excerpt, rate);
        std::fprintf(f, "}%s\n", a + 1 < argc ? "," : "");
        std::printf("extra %-40s %6.2f BPM (conf %.2f, first %.3f) key %d%s\n", argv[a], t.bpm, t.confidence, t.firstBeat, k.tonic, k.minor ? "m" : "");
    }
    std::fprintf(f, "]\n}\n");
    std::fclose(f);
    return 0;
}
