// End-to-end check of the on-device separator on Linux: a synthetic mix of drums, a bass line
// and a chord goes in; each stem must hold its own instrument (signal-to-distortion ratio).
#include <cmath>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <random>
#include <vector>

#include "Separator.h"

static std::vector<float> readStem(const std::string& path) {
    std::ifstream in(path, std::ios::binary);
    std::vector<char> bytes((std::istreambuf_iterator<char>(in)), {});
    if (bytes.size() < 44) return {};
    std::vector<float> out((bytes.size() - 44) / 4);
    std::memcpy(out.data(), bytes.data() + 44, out.size() * 4);
    return out;
}

int main(int argc, char** argv) {
    if (argc < 2) { std::fprintf(stderr, "usage: separation_test model.onnx\n"); return 2; }
    const int sr = ps::kModelSampleRate, n = sr * 10;
    std::vector<float> mix(size_t(n) * 2), refs[3];
    for (auto& r : refs) r.resize(size_t(n));
    std::mt19937 rng(1);
    std::uniform_real_distribution<float> u(-1, 1);
    for (int i = 0; i < n; ++i) {
        const float t = i / float(sr), tb = t - int(t * 2) * 0.5f;
        refs[0][i] = 0.6f * u(rng) * std::exp(-tb * 30) + 0.8f * std::sin(2 * M_PI * 60 * tb) * std::exp(-tb * 15);
        refs[1][i] = 0.35f * std::sin(2 * M_PI * 55 * t) * (0.6f + 0.4f * std::sin(2 * M_PI * 0.5f * t));
        refs[2][i] = 0.12f * (std::sin(2 * M_PI * 440 * t) + 0.5f * std::sin(2 * M_PI * 554.4f * t) + 0.5f * std::sin(2 * M_PI * 659.3f * t));
        mix[size_t(i) * 2] = mix[size_t(i) * 2 + 1] = refs[0][i] + refs[1][i] + refs[2][i];
    }
    const auto dir = std::filesystem::temp_directory_path() / "ps_stems";
    std::filesystem::create_directories(dir);
    float last = -1.0f;
    bool monotonic = true;
    auto r = ps::separate(argv[1], mix.data(), n, dir.c_str(), [&](float p) {
        if (p < last) monotonic = false;
        last = p;
        return true;
    }, 4);
    int failures = 0;
    auto check = [&](bool ok, const char* what) { if (!ok) { std::printf("  FAILED %s\n", what); ++failures; } };
    check(r.code == ps::SeparationResult::Ok, r.message.c_str());
    check(last == 1.0f && monotonic, "progress reaches 1, never goes back");
    for (int s = 0; s < 3 && r.code == ps::SeparationResult::Ok; ++s) {
        const auto stem = readStem((dir / (std::string(ps::kStemNames[s]) + ".wav")).string());
        check(stem.size() == size_t(n) * 2, "stem length");
        if (stem.size() != size_t(n) * 2) continue;
        double er = 0, err = 0;
        for (int i = 0; i < n; ++i) { er += double(refs[s][i]) * refs[s][i]; const double d = stem[size_t(i) * 2] - refs[s][i]; err += d * d; }
        const double sdr = 10 * std::log10(er / err);
        std::printf("  %-6s SDR %.1f dB\n", ps::kStemNames[s], sdr);
        check(sdr > 10.0, "stem holds its instrument (SDR > 10 dB)");
    }
    // Cancel at the first progress report: stops, writes nothing new.
    std::filesystem::remove_all(dir);
    std::filesystem::create_directories(dir);
    auto c = ps::separate(argv[1], mix.data(), n, dir.c_str(), [](float) { return false; }, 4);
    check(c.code == ps::SeparationResult::Cancelled, "cancel");
    check(!std::filesystem::exists(dir / "drums.wav"), "nothing written after cancel");
    check(ps::separate(argv[1], mix.data(), 100, dir.c_str(), [](float) { return true; }, 1).code == ps::SeparationResult::BadInput, "too short");
    check(ps::separate("/nonexistent.onnx", mix.data(), n, dir.c_str(), [](float) { return true; }, 1).code == ps::SeparationResult::ModelError, "missing model");
    std::printf(failures ? "SEPARATION FAILURES (%d)\n" : "SEPARATION OK\n", failures);
    return failures ? 1 : 0;
}
