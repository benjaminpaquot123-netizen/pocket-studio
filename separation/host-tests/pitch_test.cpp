// Basic Pitch transcription on Linux: synthetic plucked notes go in, the same notes must come out.
// usage: pitch_test model.onnx [dumpDir]   (dumpDir: writes the inputs as WAV and the notes as
// CSV, to compare with the upstream Python package)
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <string>
#include <vector>

#include "PitchTranscriber.h"

namespace {

struct Expected { double start, end; int pitch; };
const Expected kMelody[] = {{0.2, 0.8, 69}, {1.0, 1.6, 72}, {2.0, 2.6, 64}, {2.0, 2.6, 67}};

// Piano-ish tone: six harmonics, short attack, exponential decay, short release.
std::vector<float> render(int sr, double seconds) {
    std::vector<float> out(static_cast<size_t>(sr * seconds), 0.0f);
    for (const auto& n : kMelody) {
        const double f0 = 440.0 * std::pow(2.0, (n.pitch - 69) / 12.0);
        const auto from = static_cast<size_t>(n.start * sr), to = static_cast<size_t>((n.end + 0.02) * sr);
        for (size_t i = from; i < to && i < out.size(); ++i) {
            const double t = double(i) / sr - n.start;
            double env = std::min(1.0, t / 0.005) * std::exp(-3.0 * t);
            if (t > n.end - n.start) env *= std::max(0.0, 1.0 - (t - (n.end - n.start)) / 0.02);
            double v = 0.0;
            for (int h = 1; h <= 6; ++h) v += std::sin(2.0 * M_PI * f0 * h * t) / (h * h * 0.5 + 0.5);
            out[i] += static_cast<float>(0.25 * env * v);
        }
    }
    return out;
}

void writeWav(const std::string& path, const std::vector<float>& x, int sr) {
    std::FILE* f = std::fopen(path.c_str(), "wb");
    if (!f) return;
    auto u32 = [&](uint32_t v) { std::fwrite(&v, 4, 1, f); };
    auto u16 = [&](uint16_t v) { std::fwrite(&v, 2, 1, f); };
    const auto bytes = static_cast<uint32_t>(x.size() * 4);
    std::fwrite("RIFF", 1, 4, f); u32(36 + bytes); std::fwrite("WAVEfmt ", 1, 8, f);
    u32(16); u16(3); u16(1); u32(uint32_t(sr)); u32(uint32_t(sr) * 4); u16(4); u16(32);
    std::fwrite("data", 1, 4, f); u32(bytes);
    std::fwrite(x.data(), 4, x.size(), f);
    std::fclose(f);
}

int failures = 0;
void check(bool ok, const std::string& what) {
    if (!ok) { std::printf("  FAILED %s\n", what.c_str()); ++failures; }
}

void checkMelody(const std::vector<ps::PitchNote>& notes, const char* label) {
    std::printf("  %s: %zu notes\n", label, notes.size());
    for (const auto& n : notes) std::printf("    pitch %3d  %.3f - %.3f s  amp %.3f\n", n.pitch, n.start, n.end, n.amplitude);
    for (const auto& e : kMelody) {
        bool found = false;
        for (const auto& n : notes) {
            found |= n.pitch == e.pitch && std::fabs(n.start - e.start) < 0.06 && std::fabs(n.end - e.end) < 0.15;
        }
        check(found, std::string(label) + ": note " + std::to_string(e.pitch) + " at " + std::to_string(e.start) + " s");
    }
    // Nothing else, except what upstream Basic Pitch also reports on this input (checked against
    // the Python package): a short ghost note on a strong upper harmonic (C5's 5th = E7) while
    // the real note sounds.
    const int harmonics[] = {12, 19, 24, 28, 31};
    for (const auto& n : notes) {
        bool expected = false;
        for (const auto& e : kMelody) {
            const bool during = n.start > e.start - 0.06 && n.end < e.end + 0.15;
            expected |= n.pitch == e.pitch && during;
            for (int h : harmonics) expected |= n.pitch == e.pitch + h && during && n.amplitude < 0.5f;
        }
        check(expected, std::string(label) + ": unexpected note " + std::to_string(n.pitch) + " at " + std::to_string(n.start) + " s");
        check(n.amplitude > 0.0f && n.amplitude <= 1.0f && n.end > n.start, std::string(label) + ": sane note");
    }
}

void dump(const std::string& path, const std::vector<ps::PitchNote>& notes) {
    std::ofstream out(path);
    for (const auto& n : notes) out << n.start << "," << n.end << "," << n.pitch << "," << n.amplitude << "\n";
}

}  // namespace

int main(int argc, char** argv) {
    if (argc < 2) { std::fprintf(stderr, "usage: pitch_test model.onnx [dumpDir]\n"); return 2; }
    const std::string model = argv[1];

    const auto* empty = reinterpret_cast<const unsigned char*>("");
    const auto* abc = reinterpret_cast<const unsigned char*>("abc");
    check(ps::sha256Hex(empty, 0) == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "sha256('')");
    check(ps::sha256Hex(abc, 3) == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", "sha256('abc')");
    std::vector<unsigned char> million(1000000, 'a');
    check(ps::sha256Hex(million.data(), million.size()) == "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
          "sha256(1M x 'a')");

    // 44.1 kHz input (resampled natively) and 22.05 kHz input (used as is).
    const auto in44 = render(44100, 3.2);
    const auto in22 = render(22050, 3.2);
    std::string error;
    const auto notes44 = ps::transcribe(model, in44.data(), in44.size(), 44100, &error);
    check(error.empty(), "transcribe 44.1 kHz: " + error);
    checkMelody(notes44, "44.1 kHz");
    const auto notes22 = ps::transcribe(model, in22.data(), in22.size(), 22050, &error);
    check(error.empty(), "transcribe 22.05 kHz: " + error);
    checkMelody(notes22, "22.05 kHz");

    // Silence: no note, no error. Long input: several windows stitched, same notes shifted.
    std::vector<float> silence(48000 * 2, 0.0f);
    const auto none = ps::transcribe(model, silence.data(), silence.size(), 48000, &error);
    check(none.empty() && error.empty(), "silence gives no note");
    std::vector<float> longer(static_cast<size_t>(44100 * 5.0), 0.0f);
    longer.insert(longer.end(), in44.begin(), in44.end());
    const auto shifted = ps::transcribe(model, longer.data(), longer.size(), 44100, &error);
    std::vector<ps::PitchNote> back;
    for (auto n : shifted) { n.start -= 5.0; n.end -= 5.0; back.push_back(n); }
    checkMelody(back, "after 5 s of silence (window stitching)");

    // Refusals: bad input, missing model, corrupted model (never parsed).
    check(ps::transcribe(model, nullptr, 10, 44100, &error).empty() && !error.empty(), "null input refused");
    check(ps::transcribe(model, in44.data(), in44.size(), 100, &error).empty() && !error.empty(), "bad rate refused");
    check(ps::transcribe("/nonexistent.onnx", in44.data(), in44.size(), 44100, &error).empty() && !error.empty(), "missing model");
    {
        std::ifstream in(model, std::ios::binary);
        std::vector<char> bytes((std::istreambuf_iterator<char>(in)), {});
        bytes[bytes.size() / 2] ^= 1;
        const auto bad = (std::filesystem::temp_directory_path() / "ps_bad_nmp.onnx").string();
        std::ofstream(bad, std::ios::binary).write(bytes.data(), static_cast<std::streamsize>(bytes.size()));
        check(ps::transcribe(bad, in44.data(), in44.size(), 44100, &error).empty() && error == "modèle corrompu",
              "corrupted model refused before parsing");
        std::filesystem::remove(bad);
    }

    if (argc > 2) {
        const std::string dir = argv[2];
        writeWav(dir + "/in44.wav", in44, 44100);
        writeWav(dir + "/in22.wav", in22, 22050);
        writeWav(dir + "/long44.wav", longer, 44100);
        dump(dir + "/notes44.csv", notes44);
        dump(dir + "/notes22.csv", notes22);
        dump(dir + "/long44.csv", shifted);
    }
    std::printf(failures ? "PITCH FAILURES (%d)\n" : "PITCH OK\n", failures);
    return failures ? 1 : 0;
}
