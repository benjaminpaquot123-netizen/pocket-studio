// « Batterie du matin » (docs/sons/matin/BATTERIE.md): renders the before / after files.
// For each drum family and style: the app's current sound (DrumKit, kit v3, with the style's kick
// and 808 settings) four times, one second of silence, then the measured variant (MeasuredDrums.h)
// played the same way, both at the same loudness (EBU R128, libebur128). For each style, an
// 8-bar loop before, one second, the same 8 bars after. Writes 16-bit mono 48 kHz WAV files;
// scripts/son/matin_batterie.sh turns them into MP3.
// Usage: matin_batterie_render <output dir>

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <string>
#include <vector>

#include "DrumKit.h"
#include "MeasuredDrums.h"
#include "ebur128.h"

namespace {

constexpr int kRate = 48000;
constexpr int kBlock = 256;
constexpr double kTargetLufs = -16.0;
constexpr float kCeiling = 0.89f;  // about -1 dBFS

const char* const kFamilies[] = {"kick", "808", "clap", "caisse_claire", "charley_ferme", "charley_ouvert", "rim", "tom", "crash"};
const char* const kStyles[] = {"house", "techhouse", "techno", "trap", "dnb", "dubstep"};
const double kBpm[] = {122.0, 126.0, 132.0, 140.0, 174.0, 140.0};
// The app's style roots (semitones above C): the 808 plays them.
const int kRoot[] = {9, 7, 5, 5, 5, 4};

using ps::DrumKit;
namespace m = ps::measured;

int kitVoice(int family) {
    switch (family) {
        case m::Kick: return DrumKit::Kick;
        case m::Bass808: return DrumKit::Kick2;
        case m::Clap: return DrumKit::Clap;
        case m::Snare: return DrumKit::Snare;
        case m::ClosedHat: return DrumKit::ClosedHat;
        case m::OpenHat: return DrumKit::OpenHat;
        case m::Rim: return DrumKit::Rim;
        case m::Tom: return DrumKit::LowTom;
        default: return DrumKit::Cymbal;
    }
}

// The kit as the app sets it for a style (StudioDrums.kt): kit v3, the style's kick and 808.
void setUpKit(DrumKit& kit, int style) {
    kit.prepare(kRate);
    kit.setVersion(3);
    kit.setKind(DrumKit::Kick, DrumKit::KickStudio);
    float kickHz = 52.0f, decay = -1.0f, click = -1.0f;
    switch (style) {
        case m::House: kickHz = 55.0f; break;
        case m::TechHouse: kickHz = 49.0f; break;
        case m::Techno: kickHz = 43.65f; break;
        case m::Trap: kickHz = 43.65f; decay = 0.2f; break;
        case m::Dnb: kickHz = 43.65f; decay = 0.18f; click = 0.7f; break;
        default: break;  // dubstep: the kit's own studio kick (52 Hz)
    }
    kit.setVoiceParam(DrumKit::Kick, DrumKit::Tune, kickHz);
    if (decay > 0.0f) kit.setVoiceParam(DrumKit::Kick, DrumKit::Decay, decay);
    if (click >= 0.0f) kit.setVoiceParam(DrumKit::Kick, DrumKit::Click, click);
    // The trap 808 (voice808), on the style's root.
    const int v = DrumKit::Kick2;
    kit.setKind(v, DrumKit::Bass808);
    kit.setVoiceParam(v, DrumKit::Tune, static_cast<float>(24 + kRoot[style]));
    kit.setVoiceParam(v, DrumKit::Decay, 1.3f);
    kit.setVoiceParam(v, DrumKit::Drive, 0.7f);
    kit.setVoiceParam(v, DrumKit::Click, 0.5f);
    kit.setVoiceParam(v, DrumKit::Tone, 0.5f);
    kit.setVoiceParam(v, DrumKit::Version, 1.0f);
    kit.setVoiceParam(v, DrumKit::PitchDrop, 24.0f);
    kit.setVoiceParam(v, DrumKit::DropTime, 0.025f);
}

struct Hit {
    int frame;
    int family;
    float velocity;
};

// The app's kit plays the hits (in blocks, each hit at its frame).
std::vector<float> renderKit(int style, const std::vector<Hit>& hits, int frames) {
    DrumKit kit;
    setUpKit(kit, style);
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    size_t next = 0;
    for (int at = 0; at < frames; at += kBlock) {
        const int n = std::min(kBlock, frames - at);
        while (next < hits.size() && hits[next].frame < at + n) {
            kit.trigger(kitVoice(hits[next].family), hits[next].velocity, hits[next].frame - at);
            ++next;
        }
        kit.render(out.data() + at, n);
    }
    return out;
}

// The measured voices play the same hits (sample-accurate: a block is cut at each hit).
std::vector<float> renderMeasured(int style, const std::vector<Hit>& hits, int frames) {
    m::Voice voices[m::kFamilies];
    for (int f = 0; f < m::kFamilies; ++f) {
        voices[f].prepare(kRate);
        voices[f].setRecipe(m::recipe(f, style));
    }
    std::vector<float> out(static_cast<size_t>(frames), 0.0f);
    size_t next = 0;
    int at = 0;
    while (at < frames) {
        while (next < hits.size() && hits[next].frame <= at) {
            voices[hits[next].family].trigger(hits[next].velocity);
            ++next;
        }
        int end = std::min(at + kBlock, frames);
        if (next < hits.size()) end = std::min(end, hits[next].frame);
        for (auto& v : voices) v.render(out.data() + at, end - at);
        at = end;
    }
    return out;
}

double loudness(const std::vector<float>& x) {
    ebur128_state* st = ebur128_init(1, kRate, EBUR128_MODE_I);
    if (!st) return -70.0;
    ebur128_add_frames_float(st, x.data(), x.size());
    double l = -70.0;
    if (ebur128_loudness_global(st, &l) != EBUR128_SUCCESS || !std::isfinite(l)) l = -70.0;
    ebur128_destroy(&st);
    return l;
}

float peakOf(const std::vector<float>& x) {
    float p = 0.0f;
    for (float v : x) p = std::max(p, std::fabs(v));
    return p;
}

// Both parts to the same loudness, then both down together under the ceiling.
void match(std::vector<float>& a, std::vector<float>& b) {
    const double ga = std::pow(10.0, (kTargetLufs - loudness(a)) / 20.0);
    const double gb = std::pow(10.0, (kTargetLufs - loudness(b)) / 20.0);
    for (auto& v : a) v = static_cast<float>(v * ga);
    for (auto& v : b) v = static_cast<float>(v * gb);
    const float peak = std::max(peakOf(a), peakOf(b));
    if (peak > kCeiling) {
        const float g = kCeiling / peak;
        for (auto& v : a) v *= g;
        for (auto& v : b) v *= g;
    }
}

bool writeWav(const std::string& path, const std::vector<float>& x) {
    FILE* f = std::fopen(path.c_str(), "wb");
    if (!f) return false;
    const uint32_t data = static_cast<uint32_t>(x.size() * 2), riff = 36 + data, fmt = 16, rate = kRate, bytes = kRate * 2;
    const uint16_t pcm = 1, ch = 1, align = 2, bits = 16;
    std::fwrite("RIFF", 1, 4, f);
    std::fwrite(&riff, 4, 1, f);
    std::fwrite("WAVEfmt ", 1, 8, f);
    std::fwrite(&fmt, 4, 1, f);
    std::fwrite(&pcm, 2, 1, f);
    std::fwrite(&ch, 2, 1, f);
    std::fwrite(&rate, 4, 1, f);
    std::fwrite(&bytes, 4, 1, f);
    std::fwrite(&align, 2, 1, f);
    std::fwrite(&bits, 2, 1, f);
    std::fwrite("data", 1, 4, f);
    std::fwrite(&data, 4, 1, f);
    for (float v : x) {
        const int16_t s = static_cast<int16_t>(std::lround(std::clamp(v, -1.0f, 1.0f) * 32767.0f));
        std::fwrite(&s, 2, 1, f);
    }
    return std::fclose(f) == 0;
}

// before, 1 s of silence, after.
bool writePair(const std::string& path, std::vector<float> before, std::vector<float> after) {
    match(before, after);
    std::vector<float> all = before;
    all.insert(all.end(), kRate, 0.0f);
    all.insert(all.end(), after.begin(), after.end());
    for (float v : all)
        if (!std::isfinite(v)) return false;
    return writeWav(path, all);
}

// Four hits of one family: a beat apart at the style's tempo (two seconds for the 808 and the crash).
std::vector<Hit> fourHits(int family, int style, int& frames) {
    const double beat = 60.0 / kBpm[style];
    const double gap = (family == m::Bass808 || family == m::Crash) ? 2.0 : beat;
    std::vector<Hit> hits;
    for (int i = 0; i < 4; ++i) hits.push_back({static_cast<int>(i * gap * kRate), family, 1.0f});
    const double tail = (family == m::Bass808 || family == m::Crash) ? 2.0 : std::max(beat, 0.6);
    frames = static_cast<int>((3.0 * gap + tail) * kRate);
    return hits;
}

// An 8-bar loop of the style (16 steps a bar), the same hits for both kits.
std::vector<Hit> loop(int style, int& frames, bool measured) {
    const double step = 60.0 / kBpm[style] / 4.0;
    std::vector<Hit> hits;
    auto add = [&](int bar, int s, int family, float vel) {
        // The measured voices have no mix of their own: their levels in the loop (the kit's are its own).
        static constexpr float kMix[m::kFamilies] = {1.0f, 0.8f, 0.55f, 0.6f, 0.28f, 0.3f, 0.4f, 0.5f, 0.3f};
        hits.push_back({static_cast<int>(((bar * 16 + s) * step) * kRate), family, vel * (measured ? kMix[family] : 1.0f)});
    };
    for (int bar = 0; bar < 8; ++bar) {
        switch (style) {
            case m::House:
            case m::TechHouse:
            case m::Techno:
                for (int s = 0; s < 16; s += 4) add(bar, s, m::Kick, 1.0f);
                add(bar, 4, style == m::Techno ? m::Clap : m::Clap, 0.9f);
                add(bar, 12, m::Clap, 0.9f);
                for (int s = 2; s < 16; s += 4) add(bar, s, m::OpenHat, 0.7f);
                if (style != m::House)
                    for (int s = 0; s < 16; s += 2) add(bar, s + 1, m::ClosedHat, 0.6f);
                else
                    for (int s = 0; s < 16; s += 4) add(bar, s, m::ClosedHat, 0.5f);
                if (style == m::Techno && bar % 2 == 1) add(bar, 15, m::Rim, 0.7f);
                if (bar == 0) add(bar, 0, m::Crash, 0.8f);
                break;
            case m::Trap:
                add(bar, 0, m::Bass808, 1.0f);
                add(bar, 0, m::Kick, 1.0f);
                add(bar, 10, m::Kick, 0.9f);
                add(bar, 10, m::Bass808, 0.9f);
                add(bar, 8, m::Clap, 1.0f);
                add(bar, 8, m::Snare, 0.8f);
                for (int s = 0; s < 16; s += 2) add(bar, s, m::ClosedHat, 0.7f);
                if (bar % 2 == 1) {
                    add(bar, 13, m::ClosedHat, 0.5f);
                    add(bar, 15, m::ClosedHat, 0.5f);
                }
                if (bar % 4 == 3) add(bar, 14, m::OpenHat, 0.6f);
                break;
            case m::Dnb:
                add(bar, 0, m::Kick, 1.0f);
                add(bar, 10, m::Kick, 0.9f);
                add(bar, 4, m::Snare, 1.0f);
                add(bar, 12, m::Snare, 1.0f);
                for (int s = 0; s < 16; s += 2) add(bar, s, m::ClosedHat, 0.6f);
                if (bar % 2 == 1) add(bar, 14, m::OpenHat, 0.6f);
                if (bar % 4 == 3) {
                    add(bar, 13, m::Tom, 0.7f);
                    add(bar, 15, m::Tom, 0.7f);
                }
                if (bar == 0) add(bar, 0, m::Crash, 0.8f);
                break;
            default:  // dubstep, half time
                add(bar, 0, m::Kick, 1.0f);
                add(bar, 0, m::Bass808, 0.9f);
                add(bar, 8, m::Snare, 1.0f);
                add(bar, 8, m::Clap, 0.7f);
                if (bar % 2 == 1) add(bar, 11, m::Kick, 0.8f);
                for (int s = 0; s < 16; s += 2) add(bar, s, m::ClosedHat, 0.6f);
                if (bar % 2 == 0) add(bar, 14, m::OpenHat, 0.6f);
                if (bar == 0) add(bar, 0, m::Crash, 0.8f);
                break;
        }
    }
    std::stable_sort(hits.begin(), hits.end(), [](const Hit& a, const Hit& b) { return a.frame < b.frame; });
    frames = static_cast<int>((8 * 16 * step + 1.0) * kRate);
    return hits;
}

}  // namespace

int main(int argc, char** argv) {
    if (argc < 2) {
        std::fprintf(stderr, "usage: %s <output dir>\n", argv[0]);
        return 2;
    }
    const std::string dir = argv[1];
    int written = 0;
    for (int f = 0; f < m::kFamilies; ++f) {
        for (int s = 0; s < m::kStyles; ++s) {
            int frames = 0;
            const auto hits = fourHits(f, s, frames);
            const std::string path = dir + "/" + kFamilies[f] + "_" + kStyles[s] + ".wav";
            if (!writePair(path, renderKit(s, hits, frames), renderMeasured(s, hits, frames))) {
                std::fprintf(stderr, "cannot write %s\n", path.c_str());
                return 1;
            }
            ++written;
        }
    }
    for (int s = 0; s < m::kStyles; ++s) {
        int frames = 0;
        const auto before = loop(s, frames, false);
        const auto after = loop(s, frames, true);
        const std::string path = dir + "/boucle_" + kStyles[s] + ".wav";
        if (!writePair(path, renderKit(s, before, frames), renderMeasured(s, after, frames))) {
            std::fprintf(stderr, "cannot write %s\n", path.c_str());
            return 1;
        }
        ++written;
    }
    std::printf("%d WAV written in %s\n", written, dir.c_str());
    return 0;
}
