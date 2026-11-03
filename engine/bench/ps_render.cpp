// ps_render: replays a .pscmd file (the engine commands of a real export, recorded by the app's
// RecordingSink during SongBenchExport) through the engine core's OfflineRenderer, exactly as the
// JNI export bridge (engine/src/main/cpp/android/JniBridge.cpp) does, and writes the result.
//
//   ps_render [--no-limiter] [--as-app] [--solo <channel>[,<channel>...]] [--no-exciter] [--no-duck] [--ceiling <dBTP>] [--drive-ceiling <dBTP>] [--report <file.json>] <in.pscmd> <out>
//
// RENDER wav: <out> is a WAV file, 32-bit float at 48 kHz by default (no dither: byte-identical
//   runs); --as-app writes what the app writes (the FORMAT line: 44.1 kHz, 16 or 24 bits dithered).
// RENDER float: <out> is raw interleaved stereo float32 (what OfflineExport.renderToArray returns).
// --no-limiter: the master's limiter off (a pre-master render).
// --solo: one mixer channel alone (what the app's separate tracks do), to find where a sound comes from;
// several (comma-separated): those together (the song's basses: META bass).
// --no-exciter: every « Basse audible » (the bass exciter, InsertSlot::BassExciterFx) left out, the rest
// as recorded: what the exciter adds to the song (the bench's B12).
// --no-duck: the « Musique » bus not ducked under the kick (its depths at 0), no pump on any strip, the rest as recorded: the
// basses alone this way are what the bench's B9 reads their dip against.
// --ceiling: the master limiter's ceiling (what the app sets for an M4A: -2).
// --drive-ceiling: (S6) the drive searched as at this ceiling, the music then limited at --ceiling
//   (what the app sets for an M4A of a Drop-aimed song: -1, the WAV's).
// --report: the renderer's end-of-export report (what the app shows) as a small JSON: integrated
//   loudness, true peak, length, the gain the loudness target added (drive_db, 0 without one) and
//   the loudness of the music before the master chain (music_lufs, -1000 in a one-pass render);
//   (S5) what the clipper and the limiter took off (loss_db: over the loud part in Drop mode), the
//   loud part's loudness after and before the chain (loud_lufs, music_loud_lufs: -1000 outside the
//   Drop mode), the target's mode (0 the whole song, 1 the Drop) and its LUFS (target_lufs, 0: none).
//
// File format (text, one call per line; see docs/plan/son.md § 2.2):
//   PSCMD 1 / RATE 48000
//   SAMPLE <id> <channels> <rate> <file.f32>   NativeSample.create (path relative to the .pscmd)
//   C <type> <value> <a> <b>                   NativeEngine.post
//   AT <frame>                                 captureAt (-1: at once)
//   PAD <pad> <id> / TRACKSAMPLE <track> <id> / AUDIOCLIP <track> <id> / STEMS 0 / INSTRUMENT <a> 0
//   FADE <frames> / OPTIONS <autoTail> <floatWav> / FORMAT <rate> <bits> / RENDER <wav|float> <frames> <tail>
//   SONG, SECTION, META, #: for ps_measure and people, ignored here.
//
// Lot E (« Master pro », scripts/export-matrix.sh), on top of the recording:
//   --target <LUFS>       the loudness target (0: none); --whole-song: aimed at the whole song (a pro target)
//   --rate <Hz> --bits <16|24|32>   the WAV's format (44100, 48000, 96000; 32 = float), dithered at 16 and 24
//   --shape               16 bits: the dither's noise shaped
//   --no-master-fx        the master's effects off (what the stems and the mix they add up to skip)
//   --stem-mode <0..3>    Mixer::StemMode (1 dry, 2 the reverb's return alone, 3 the echo's)
//   --fade-in <s> --end-silence <s> --trim --range-start <frame>   the file's finish (OfflineRenderer::Finish)
//   --attack <0.2..1> --release <0.25..4>   the limiter's settings
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <map>
#include <sstream>
#include <string>
#include <vector>

#include "Effects.h"
#include "OfflineRenderer.h"
#include "Sample.h"

namespace {

std::string dirOf(const std::string& path) {
    const auto slash = path.find_last_of('/');
    return slash == std::string::npos ? std::string(".") : path.substr(0, slash);
}

bool readFloats(const std::string& path, std::vector<float>& out) {
    std::FILE* f = std::fopen(path.c_str(), "rb");
    if (!f) return false;
    std::fseek(f, 0, SEEK_END);
    const long bytes = std::ftell(f);
    std::fseek(f, 0, SEEK_SET);
    out.resize(static_cast<size_t>(bytes) / sizeof(float));
    const size_t got = std::fread(out.data(), sizeof(float), out.size(), f);
    std::fclose(f);
    return got == out.size();
}

[[noreturn]] void fail(int line, const std::string& what) {
    std::fprintf(stderr, "ps_render: line %d: %s\n", line, what.c_str());
    std::exit(2);
}

}  // namespace

int main(int argc, char** argv) {
    bool noLimiter = false, asApp = false;
    std::vector<int> solo;
    bool noExciter = false, noDuck = false;
    float ceiling = NAN, driveCeiling = NAN;
    float target = NAN, attack = NAN, release = NAN;
    bool wholeSong = false, noMasterFx = false, shape = false, trim = false;
    int rate = 0, bits = 0, stemMode = -1;
    double fadeIn = 0.0, endSilence = 0.0;
    long long rangeStart = 0;
    std::string reportPath;
    std::vector<std::string> files;
    for (int i = 1; i < argc; ++i) {
        if (!std::strcmp(argv[i], "--no-limiter")) noLimiter = true;
        else if (!std::strcmp(argv[i], "--as-app")) asApp = true;
        else if (!std::strcmp(argv[i], "--solo") && i + 1 < argc) {
            std::istringstream list(argv[++i]);
            std::string ch;
            while (std::getline(list, ch, ',')) if (!ch.empty()) solo.push_back(std::atoi(ch.c_str()));
        }
        else if (!std::strcmp(argv[i], "--no-exciter")) noExciter = true;
        else if (!std::strcmp(argv[i], "--no-duck")) noDuck = true;
        else if (!std::strcmp(argv[i], "--report") && i + 1 < argc) reportPath = argv[++i];
        else if (!std::strcmp(argv[i], "--ceiling") && i + 1 < argc) ceiling = std::strtof(argv[++i], nullptr);
        else if (!std::strcmp(argv[i], "--drive-ceiling") && i + 1 < argc) driveCeiling = std::strtof(argv[++i], nullptr);
        else if (!std::strcmp(argv[i], "--target") && i + 1 < argc) target = std::strtof(argv[++i], nullptr);
        else if (!std::strcmp(argv[i], "--whole-song")) wholeSong = true;
        else if (!std::strcmp(argv[i], "--rate") && i + 1 < argc) rate = std::atoi(argv[++i]);
        else if (!std::strcmp(argv[i], "--bits") && i + 1 < argc) bits = std::atoi(argv[++i]);
        else if (!std::strcmp(argv[i], "--shape")) shape = true;
        else if (!std::strcmp(argv[i], "--no-master-fx")) noMasterFx = true;
        else if (!std::strcmp(argv[i], "--stem-mode") && i + 1 < argc) stemMode = std::atoi(argv[++i]);
        else if (!std::strcmp(argv[i], "--fade-in") && i + 1 < argc) fadeIn = std::strtod(argv[++i], nullptr);
        else if (!std::strcmp(argv[i], "--end-silence") && i + 1 < argc) endSilence = std::strtod(argv[++i], nullptr);
        else if (!std::strcmp(argv[i], "--trim")) trim = true;
        else if (!std::strcmp(argv[i], "--range-start") && i + 1 < argc) rangeStart = std::atoll(argv[++i]);
        else if (!std::strcmp(argv[i], "--attack") && i + 1 < argc) attack = std::strtof(argv[++i], nullptr);
        else if (!std::strcmp(argv[i], "--release") && i + 1 < argc) release = std::strtof(argv[++i], nullptr);
        else files.emplace_back(argv[i]);
    }
    if (files.size() != 2) {
        std::fprintf(stderr, "usage: ps_render [--no-limiter] [--as-app] [--solo <channel>[,...]] [--no-exciter] [--no-duck] [--ceiling <dBTP>] [--drive-ceiling <dBTP>] [--report <file.json>] <in.pscmd> <out>\n");
        return 1;
    }
    std::ifstream in(files[0]);
    if (!in) {
        std::fprintf(stderr, "ps_render: cannot read %s\n", files[0].c_str());
        return 1;
    }
    const std::string base = dirOf(files[0]);

    ps::OfflineRenderer renderer(48000);
    std::map<long, ps::Sample*> samples;  // our own reference to each (as the app's NativeSample holds one)
    auto sampleOf = [&](long id, int line) -> ps::Sample* {
        if (id == 0) return nullptr;
        auto it = samples.find(id);
        if (it == samples.end()) fail(line, "unknown sample " + std::to_string(id));
        it->second->retain();  // the engine takes this one over (JNI exportInstall*)
        return it->second;
    };
    auto install = [&](ps::CommandType type, int a, ps::Sample* s) {
        ps::Command c{type, 0.0f, a, 0};
        c.sample = s;
        renderer.post(c);
    };

    std::string kind;
    int64_t frames = 0, tail = 0;
    bool floatWav = false, rendered = false;
    int fmtRate = 48000, fmtBits = 0;  // FORMAT: the app's file format (0: none recorded)
    std::string text;
    int line = 0;
    while (std::getline(in, text)) {
        ++line;
        std::istringstream ls(text);
        std::string op;
        if (!(ls >> op) || op[0] == '#') continue;
        if (op == "C") {
            int type = 0;
            std::string value;
            int32_t a = 0, b = 0;
            if (!(ls >> type >> value >> a >> b)) fail(line, "bad command");
            if (noExciter && type == static_cast<int>(ps::CommandType::SetInsertType) && b == ps::InsertSlot::BassExciterFx) b = ps::InsertSlot::None;
            // --no-duck: a strip's own pump (the 808's pad under the kick) left out too.
            if (noDuck && type == static_cast<int>(ps::CommandType::SetInsertType) && b == ps::InsertSlot::Pump) b = ps::InsertSlot::None;
            float v = std::strtof(value.c_str(), nullptr);
            // SetSidechain 1 and 2: the Musique bus's depth and low depth (Engine.cpp).
            if (noDuck && type == static_cast<int>(ps::CommandType::SetSidechain) && (a == 1 || a == 2)) v = 0.0f;
            renderer.post({static_cast<ps::CommandType>(type), v, a, b});
        } else if (op == "AT") {
            long long frame = -1;
            ls >> frame;
            renderer.scheduleFrom(frame);
        } else if (op == "SAMPLE") {
            long id = 0;
            int channels = 0, rate = 0;
            std::string file;
            if (!(ls >> id >> channels >> rate >> file)) fail(line, "bad SAMPLE");
            std::vector<float> data;
            if (!readFloats(base + "/" + file, data)) fail(line, "cannot read " + file);
            // NativeEngine.sampleCreate: frames = length / channels, resampled to 48 kHz.
            if (channels < 1 || static_cast<long>(data.size()) < channels) fail(line, "empty sample");
            ps::Sample* s = ps::Sample::create(data.data(), static_cast<int64_t>(data.size()) / channels, channels, rate, 48000);
            if (!s) fail(line, "sample rejected");
            samples[id] = s;
        } else if (op == "PAD" || op == "TRACKSAMPLE" || op == "AUDIOCLIP") {
            int index = 0;
            long id = 0;
            if (!(ls >> index >> id)) fail(line, "bad " + op);
            ps::Sample* s = sampleOf(id, line);
            if (op == "PAD") install(ps::CommandType::InstallSample, index, s);
            else if (op == "AUDIOCLIP") install(ps::CommandType::InstallAudioClip, index, s);
            else install(ps::CommandType::InstallTrackSample, (std::clamp(index & 0xFF, 0, ps::kSounds - 1) << 16) | ((index >> 8) & 0xFFFF), s);  // | zone + 1 << 8: a multizone zone
        } else if (op == "STEMS") {
            long id = 0;
            ls >> id;
            if (id != 0) fail(line, "remix stems are not recorded by the bench");
            ps::Command c{ps::CommandType::InstallStems};
            renderer.post(c);
        } else if (op == "INSTRUMENT") {
            int32_t a = 0;
            long font = 0;
            ls >> a >> font;
            if (font != 0) fail(line, "SoundFont banks are not recorded by the bench");
            renderer.post({ps::CommandType::SetInstrument, 0.0f, a, 0});
        } else if (op == "FADE") {
            long long f = 0;
            ls >> f;
            renderer.setFadeOut(f);
        } else if (op == "OPTIONS") {
            int autoTail = 0, fw = 0;
            ls >> autoTail >> fw;
            renderer.setAutoTail(autoTail != 0);
            floatWav = fw != 0;
        } else if (op == "FORMAT") {
            if (!(ls >> fmtRate >> fmtBits)) fail(line, "bad FORMAT");
        } else if (op == "RENDER") {
            long long f = 0, t = 0;
            if (!(ls >> kind >> f >> t)) fail(line, "bad RENDER");
            frames = f;
            tail = t;
            rendered = true;
        } else if (op == "PSCMD" || op == "RATE" || op == "SONG" || op == "SECTION" || op == "META") {
            continue;
        } else {
            fail(line, "unknown line: " + op);
        }
    }
    if (!rendered) {
        std::fprintf(stderr, "ps_render: no RENDER line in %s\n", files[0].c_str());
        return 1;
    }
    if (noLimiter) {
        renderer.scheduleFrom(-1);
        renderer.post({ps::CommandType::SetMaster, 0.0f, 1, 0});
    }
    for (int ch : solo) {
        renderer.scheduleFrom(-1);
        renderer.post({ps::CommandType::SetChannelSolo, 1.0f, ch, 0});
    }
    if (std::isfinite(ceiling)) {
        renderer.scheduleFrom(-1);
        renderer.post({ps::CommandType::SetMaster, ceiling, 5, 0});
    }
    if (std::isfinite(driveCeiling)) {
        renderer.scheduleFrom(-1);
        renderer.post({ps::CommandType::SetMaster, driveCeiling, 9, 0});
    }
    auto now = [&](ps::CommandType type, float value, int a) {
        renderer.scheduleFrom(-1);
        renderer.post({type, value, a, 0});
    };
    if (noMasterFx) now(ps::CommandType::SetMaster, 0.0f, 2);
    if (stemMode >= 0) now(ps::CommandType::SetMasterPro, float(stemMode), ps::ProStemMode);
    if (wholeSong) now(ps::CommandType::SetMasterPro, 1.0f, ps::ProForceSong);
    if (std::isfinite(target)) now(ps::CommandType::SetMaster, target, 4);
    if (std::isfinite(attack)) now(ps::CommandType::SetMasterPro, attack, ps::ProLimiterAttack);
    if (std::isfinite(release)) now(ps::CommandType::SetMasterPro, release, ps::ProLimiterRelease);
    ps::OfflineRenderer::Finish finish;
    finish.fadeInFrames = static_cast<int64_t>(std::lround(fadeIn * 48000.0));
    finish.endSilenceFrames = static_cast<int64_t>(std::lround(endSilence * 48000.0));
    finish.trimStart = finish.trimEnd = trim;
    finish.rangeStart = rangeStart;
    finish.noiseShaping = shape;
    renderer.setFinish(finish);
    int status = 0;
    if (kind == "float") {
        const std::vector<float> out = renderer.renderToBuffer(frames, tail);
        std::FILE* f = std::fopen(files[1].c_str(), "wb");
        if (!f || std::fwrite(out.data(), sizeof(float), out.size(), f) != out.size()) status = 3;
        if (f) std::fclose(f);
    } else if (kind == "wav") {
        if (rate != 0 || bits != 0) renderer.setOutputFormat(rate != 0 ? rate : 48000, bits != 0 ? bits : 24);
        else if (asApp && fmtBits != 0) renderer.setOutputFormat(fmtRate, fmtBits);
        else renderer.setFloatWav(asApp ? floatWav : true);
        if (renderer.renderToWav(files[1].c_str(), frames, tail) != ps::OfflineRenderer::Result::Ok) status = 3;
    } else {
        std::fprintf(stderr, "ps_render: unknown RENDER kind %s\n", kind.c_str());
        status = 1;
    }
    if (status == 0 && !reportPath.empty()) {
        const ps::OfflineRenderer::Report& r = renderer.report();
        std::FILE* f = std::fopen(reportPath.c_str(), "w");
        if (!f) {
            status = 3;
        } else {
            const double tpDb = r.truePeak > 0.0f ? 20.0 * std::log10(static_cast<double>(r.truePeak)) : -200.0;
            std::fprintf(f, "{\"lufs\": %.3f, \"true_peak_db\": %.3f, \"seconds\": %.3f, \"sample_rate\": %d, \"drive_db\": %.3f, \"music_lufs\": %.3f, "
                            "\"loss_db\": %.3f, \"loud_lufs\": %.3f, \"music_loud_lufs\": %.3f, \"mode\": %d, \"target_lufs\": %.2f, "
                            "\"max_momentary\": %.3f, \"max_short_term\": %.3f, \"lra\": %.3f, \"correlation\": %.4f, \"mono_lufs\": %.3f}\n",
                         r.lufs, tpDb, static_cast<double>(r.frames) / static_cast<double>(r.sampleRate > 0 ? r.sampleRate : 48000), r.sampleRate,
                         static_cast<double>(r.driveDb), r.musicLufs, static_cast<double>(r.lossDb), r.loudLufs, r.musicLoudLufs, r.mode,
                         std::isfinite(r.targetLufs) ? static_cast<double>(r.targetLufs) : 0.0, r.maxMomentary, r.maxShortTerm, r.lra,
                         r.correlation, r.monoLufs);
            std::fclose(f);
        }
    }
    for (auto& [id, s] : samples) {
        if (s->release()) delete s;
    }
    if (status == 3) std::fprintf(stderr, "ps_render: cannot write %s\n", files[1].c_str());
    return status;
}
