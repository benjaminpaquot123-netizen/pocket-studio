// ps_measure: measures a rendered song (WAV) for the song bench (docs/plan/son.md § 2.3) and prints
// one JSON object.
//
//   ps_measure <song.wav> [<song.pscmd>]
//
// The .pscmd (optional) gives the song's sections (SECTION lines written by SongBenchExport): each
// section's loudness, the Drop / Groove / Break contrast, and the "main" section (the Drop, else
// the Refrain, else the loudest) on which bands, slope and stereo are measured.
//
// Loudness, LRA: libebur128 (BS.1770-4 / EBU R 128). True peak: x4 as FFmpeg's ebur128 (see truePeak),
// libebur128's BS.1770 reading also given (true_peak_bs1770).
// Spectrum: Signalsmith Linear's real FFT, 32768 points, Hann window, 50 % overlap, on L and R.
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <map>
#include <sstream>
#include <string>
#include <vector>

#include "Loudness.h"

#include "ebur128.h"
#include "signalsmith-linear/fft.h"

namespace {

struct Audio {
    int rate = 48000;
    std::vector<float> l, r;
    size_t frames() const { return l.size(); }
};

uint32_t le32(const unsigned char* p) { return p[0] | (p[1] << 8) | (p[2] << 16) | (static_cast<uint32_t>(p[3]) << 24); }
uint16_t le16(const unsigned char* p) { return static_cast<uint16_t>(p[0] | (p[1] << 8)); }

bool readWav(const char* path, Audio& a) {
    std::ifstream f(path, std::ios::binary);
    if (!f) return false;
    std::vector<unsigned char> b((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>());
    if (b.size() < 12 || std::memcmp(b.data(), "RIFF", 4) || std::memcmp(b.data() + 8, "WAVE", 4)) return false;
    int format = 0, channels = 0, bits = 0;
    size_t pos = 12;
    while (pos + 8 <= b.size()) {
        const uint32_t size = le32(&b[pos + 4]);
        const unsigned char* body = &b[pos + 8];
        if (!std::memcmp(&b[pos], "fmt ", 4)) {
            format = le16(body);
            channels = le16(body + 2);
            a.rate = static_cast<int>(le32(body + 4));
            bits = le16(body + 14);
            if (format == 0xFFFE && size >= 26) format = le16(body + 24);  // WAVE_FORMAT_EXTENSIBLE
        } else if (!std::memcmp(&b[pos], "data", 4)) {
            if (channels < 1 || channels > 2) return false;
            const size_t bytes = std::min<size_t>(size, b.size() - pos - 8);
            const int width = bits / 8;
            const size_t n = bytes / static_cast<size_t>(width * channels);
            a.l.resize(n);
            a.r.resize(n);
            for (size_t i = 0; i < n; ++i) {
                for (int c = 0; c < channels; ++c) {
                    const unsigned char* s = body + (i * static_cast<size_t>(channels) + static_cast<size_t>(c)) * static_cast<size_t>(width);
                    float v = 0.0f;
                    if (format == 3 && bits == 32) {
                        uint32_t u = le32(s);
                        std::memcpy(&v, &u, 4);
                    } else if (format == 1 && bits == 24) {
                        int32_t x = static_cast<int32_t>(s[0] | (s[1] << 8) | (s[2] << 16) | (static_cast<uint32_t>(s[2] & 0x80 ? 0xFF : 0) << 24));
                        v = static_cast<float>(x / 8388608.0);
                    } else if (format == 1 && bits == 16) {
                        v = static_cast<float>(static_cast<int16_t>(le16(s)) / 32768.0);
                    } else {
                        return false;
                    }
                    (c == 0 ? a.l : a.r)[i] = v;
                }
                if (channels == 1) a.r[i] = a.l[i];
            }
            return true;
        }
        pos += 8 + size + (size & 1);
    }
    return false;
}

struct Section {
    int64_t start = 0, end = 0;
    std::string label;
};

double db(double power) { return 10.0 * std::log10(std::max(power, 1e-30)); }
double dbAmp(double amp) { return 20.0 * std::log10(std::max(amp, 1e-15)); }

// One libebur128 state over a list of [start, end) ranges of the audio (fed in order).
struct Loudness {
    double lufs = -HUGE_VAL, lra = 0, truePeak = 0, samplePeak = 0;
};

Loudness loudness(const Audio& a, const std::vector<std::pair<int64_t, int64_t>>& ranges, bool mono = false, bool peaks = false) {
    const int mode = EBUR128_MODE_I | EBUR128_MODE_LRA | (peaks ? EBUR128_MODE_TRUE_PEAK | EBUR128_MODE_SAMPLE_PEAK : 0);
    ebur128_state* st = ebur128_init(2, static_cast<unsigned long>(a.rate), mode);
    std::vector<float> buf;
    for (auto [s, e] : ranges) {
        for (int64_t i = s; i < e;) {
            const int64_t n = std::min<int64_t>(e - i, 65536);
            buf.resize(static_cast<size_t>(n) * 2);
            for (int64_t k = 0; k < n; ++k) {
                const float l = a.l[static_cast<size_t>(i + k)], r = a.r[static_cast<size_t>(i + k)];
                const float m = 0.5f * (l + r);
                buf[static_cast<size_t>(2 * k)] = mono ? m : l;
                buf[static_cast<size_t>(2 * k + 1)] = mono ? m : r;
            }
            ebur128_add_frames_float(st, buf.data(), static_cast<size_t>(n));
            i += n;
        }
    }
    Loudness out;
    ebur128_loudness_global(st, &out.lufs);
    ebur128_loudness_range(st, &out.lra);
    if (peaks) {
        for (unsigned c = 0; c < 2; ++c) {
            double tp = 0, sp = 0;
            ebur128_true_peak(st, c, &tp);
            ebur128_sample_peak(st, c, &sp);
            out.truePeak = std::max(out.truePeak, tp);
            out.samplePeak = std::max(out.samplePeak, sp);
        }
    }
    ebur128_destroy(&st);
    return out;
}

// True peak as FFmpeg's ebur128 filter reads it (it resamples to 192 kHz with libswresample's
// defaults: a 32-tap Kaiser-windowed sinc, beta 9): within a few hundredths of a dB of it, and a
// little above libebur128's BS.1770 filter (which reads up to 0.2 dB under a long interpolator).
// The ceiling the bench checks (-1 dBTP) is read with this one, the more cautious.
double truePeak(const Audio& a, const std::vector<std::pair<int64_t, int64_t>>& ranges) {
    constexpr int kUp = 4, kHalf = 16;
    static double weights[kUp][2 * kHalf];
    static bool ready = false;
    if (!ready) {
        auto bessel0 = [](double x) {
            double sum = 1, term = 1;
            for (int k = 1; k < 50; ++k) {
                term *= (x / (2 * k)) * (x / (2 * k));
                sum += term;
            }
            return sum;
        };
        for (int p = 0; p < kUp; ++p) {
            for (int i = 0; i < 2 * kHalf; ++i) {
                // Output at n + p/4 from inputs n - kHalf + 1 + i: t = distance in input samples.
                const double t = static_cast<double>(p) / kUp + kHalf - 1 - i;
                const double sinc = t == 0 ? 1 : std::sin(M_PI * t) / (M_PI * t);
                const double r = t / kHalf;
                const double window = std::fabs(r) < 1 ? bessel0(9.0 * std::sqrt(1 - r * r)) / bessel0(9.0) : 0;
                weights[p][i] = sinc * window;
            }
        }
        ready = true;
    }
    double peak = 0;
    for (int c = 0; c < 2; ++c) {
        const auto& x = c == 0 ? a.l : a.r;
        const int64_t n = static_cast<int64_t>(x.size());
        for (auto [s, e] : ranges) {
            for (int64_t i = s; i < e; ++i) {
                peak = std::max(peak, static_cast<double>(std::fabs(x[static_cast<size_t>(i)])));
                for (int p = 1; p < kUp; ++p) {
                    double v = 0;
                    for (int k = 0; k < 2 * kHalf; ++k) {
                        const int64_t j = i - kHalf + 1 + k;
                        if (j >= 0 && j < n) v += x[static_cast<size_t>(j)] * weights[p][k];
                    }
                    peak = std::max(peak, std::fabs(v));
                }
            }
        }
    }
    return peak;
}

// RBJ low-pass biquad (direct form I, double), for the < 120 Hz stereo measures.
struct LowPass {
    double b0, b1, b2, a1, a2, x1 = 0, x2 = 0, y1 = 0, y2 = 0;
    LowPass(double rate, double hz, double q) {
        const double w = 2 * M_PI * hz / rate, alpha = std::sin(w) / (2 * q), c = std::cos(w), a0 = 1 + alpha;
        b0 = (1 - c) / 2 / a0;
        b1 = (1 - c) / a0;
        b2 = b0;
        a1 = -2 * c / a0;
        a2 = (1 - alpha) / a0;
    }
    double operator()(double x) {
        const double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = x;
        y2 = y1;
        y1 = y;
        return y;
    }
};

struct Stereo {
    double sideMid = 0, corr = 0, sideMidLow = 0, corrLow = 0;
};

double pearson(double sl, double sr, double sll, double srr, double slr, double n) {
    const double cov = slr - sl * sr / n, vl = sll - sl * sl / n, vr = srr - sr * sr / n;
    return vl > 0 && vr > 0 ? cov / std::sqrt(vl * vr) : 1.0;
}

Stereo stereo(const Audio& a, const std::vector<std::pair<int64_t, int64_t>>& ranges) {
    double ss = 0, mm = 0, sl = 0, sr = 0, sll = 0, srr = 0, slr = 0, n = 0;
    double ssL = 0, mmL = 0, lsl = 0, lsr = 0, lsll = 0, lsrr = 0, lslr = 0;
    for (auto [s, e] : ranges) {
        // 4th-order Butterworth low-pass at 120 Hz: two biquads (Q 0.5412, 1.3066), per range.
        LowPass l1(a.rate, 120, 0.54119610), l2(a.rate, 120, 1.30656296), r1(a.rate, 120, 0.54119610), r2(a.rate, 120, 1.30656296);
        for (int64_t i = s; i < e; ++i) {
            const double l = a.l[static_cast<size_t>(i)], r = a.r[static_cast<size_t>(i)];
            ss += (l - r) * (l - r);
            mm += (l + r) * (l + r);
            sl += l;
            sr += r;
            sll += l * l;
            srr += r * r;
            slr += l * r;
            const double ll = l2(l1(l)), rl = r2(r1(r));
            ssL += (ll - rl) * (ll - rl);
            mmL += (ll + rl) * (ll + rl);
            lsl += ll;
            lsr += rl;
            lsll += ll * ll;
            lsrr += rl * rl;
            lslr += ll * rl;
            n += 1;
        }
    }
    Stereo out;
    if (n < 2) return out;
    out.sideMid = db(ss / std::max(mm, 1e-30));
    out.corr = pearson(sl, sr, sll, srr, slr, n);
    out.sideMidLow = db(ssL / std::max(mmL, 1e-30));
    out.corrLow = pearson(lsl, lsr, lsll, lsrr, lslr, n);
    return out;
}

// Power spectrum of L and R summed (Hann, 50 % overlap) over the ranges; bin k = k * rate / N.
constexpr size_t kFft = 32768;

std::vector<double> spectrum(const Audio& a, const std::vector<std::pair<int64_t, int64_t>>& ranges) {
    signalsmith::linear::RealFFT<double> fft;
    fft.resize(kFft);
    std::vector<double> window(kFft), in(kFft), power(kFft / 2 + 1, 0.0);
    std::vector<std::complex<double>> out(kFft / 2);
    for (size_t i = 0; i < kFft; ++i) window[i] = 0.5 - 0.5 * std::cos(2 * M_PI * static_cast<double>(i) / kFft);
    for (auto [s, e] : ranges) {
        for (int64_t at = s; at + static_cast<int64_t>(kFft) <= e; at += kFft / 2) {
            for (int c = 0; c < 2; ++c) {
                const auto& x = c == 0 ? a.l : a.r;
                for (size_t i = 0; i < kFft; ++i) in[i] = x[static_cast<size_t>(at) + i] * window[i];
                fft.fft(in.data(), out.data());
                // out[0] packs DC (real) and Nyquist (imaginary).
                for (size_t k = 1; k < kFft / 2; ++k) power[k] += std::norm(out[k]);
            }
        }
    }
    return power;
}

struct Bands {
    double sub, low, lowMid, mid, highMid, air, slope;
    bool valid = false;
};

Bands bands(const std::vector<double>& p, int rate) {
    Bands b{};
    double total = 0;
    for (size_t k = 1; k < p.size(); ++k) total += p[k];
    if (total <= 0) return b;
    auto sum = [&](double lo, double hi) {
        double s = 0;
        for (size_t k = 1; k < p.size(); ++k) {
            const double f = static_cast<double>(k) * rate / kFft;
            if (f >= lo && f < hi) s += p[k];
        }
        return db(s / total);
    };
    b.sub = sum(20, 60);
    b.low = sum(60, 250);
    b.lowMid = sum(250, 500);
    b.mid = sum(500, 2000);
    b.highMid = sum(2000, 6000);
    b.air = sum(6000, 20000);
    // Spectral slope: a straight line through the 1/3-octave levels (mean power density per band)
    // from 100 Hz to 8 kHz, in dB per octave (a pink spectrum is -3).
    std::vector<double> xs, ys;
    for (int i = 0; i < 20; ++i) {
        const double centre = 100.0 * std::pow(2.0, i / 3.0), lo = centre / std::pow(2.0, 1.0 / 6), hi = centre * std::pow(2.0, 1.0 / 6);
        double s = 0;
        int count = 0;
        for (size_t k = 1; k < p.size(); ++k) {
            const double f = static_cast<double>(k) * rate / kFft;
            if (f >= lo && f < hi) {
                s += p[k];
                ++count;
            }
        }
        if (count == 0) continue;
        xs.push_back(std::log2(centre / 100.0));
        ys.push_back(db(s / count));
    }
    const double n = static_cast<double>(xs.size());
    double mx = 0, my = 0;
    for (size_t i = 0; i < xs.size(); ++i) {
        mx += xs[i] / n;
        my += ys[i] / n;
    }
    double num = 0, den = 0;
    for (size_t i = 0; i < xs.size(); ++i) {
        num += (xs[i] - mx) * (ys[i] - my);
        den += (xs[i] - mx) * (xs[i] - mx);
    }
    b.slope = den > 0 ? num / den : 0;
    b.valid = true;
    return b;
}

std::string num(double v, int decimals = 2) {
    if (!std::isfinite(v)) return "null";
    char s[64];
    std::snprintf(s, sizeof(s), "%.*f", decimals, v);
    if (!std::strcmp(s, "-0.00") || !std::strcmp(s, "-0.0") || !std::strcmp(s, "-0.000")) std::snprintf(s, sizeof(s), "%.*f", decimals, 0.0);
    return s;
}

std::string quoted(const std::string& s) {
    std::string out = "\"";
    for (char c : s) {
        if (c == '"' || c == '\\') out += '\\';
        out += c;
    }
    return out + "\"";
}

}  // namespace

// S4: how much the lead's brightness moves along the song. Its track alone (ps_render --solo), the
// spectral centroid (60 Hz..16 kHz) of each bar where it plays (within 30 dB of its loudest bar):
// the spread between the 10th and the 90th percentile, as +-% around their middle. Bar by bar, the
// notes of a phrase average out: what is left is the filter's movement (the 8-bar cycle, the curves).
struct Centroid {
    double median = 0, variation = 0;
    int bars = 0;
};

Centroid barCentroids(const Audio& a, int64_t songFrames, double bpm, int beatsPerBar = 4) {
    Centroid c;
    if (!(bpm > 0)) return c;
    const int64_t bar = static_cast<int64_t>(std::llround(beatsPerBar * 60.0 / bpm * a.rate));
    std::vector<std::pair<double, double>> perBar;  // centroid, energy
    double loudest = 0;
    for (int64_t s = 0; s + bar <= songFrames; s += bar) {
        const auto p = spectrum(a, {{s, s + bar}});
        double num = 0, den = 0;
        for (size_t k = 1; k < p.size(); ++k) {
            const double f = static_cast<double>(k) * a.rate / kFft;
            if (f < 60 || f > 16000) continue;
            num += f * p[k];
            den += p[k];
        }
        if (den <= 0) continue;
        perBar.emplace_back(num / den, den);
        loudest = std::max(loudest, den);
    }
    std::vector<double> kept;
    for (auto [hz, e] : perBar) {
        if (e >= loudest * 1e-3) kept.push_back(hz);
    }
    if (kept.size() < 4) return c;
    std::sort(kept.begin(), kept.end());
    auto pct = [&](double q) { return kept[static_cast<size_t>(std::lround(q * static_cast<double>(kept.size() - 1)))]; };
    const double lo = pct(0.1), hi = pct(0.9);
    c.median = pct(0.5);
    c.variation = 100.0 * (hi - lo) / (hi + lo);
    c.bars = static_cast<int>(kept.size());
    return c;
}

int main(int argc, char** argv) {
    // ps_measure <song.wav> [<song.pscmd>] [--lead <lead.wav>]: --lead adds the lead's centroid movement.
    std::string leadPath;
    std::vector<char*> args{argv[0]};
    for (int i = 1; i < argc; ++i) {
        if (!std::strcmp(argv[i], "--lead") && i + 1 < argc) leadPath = argv[++i];
        else args.push_back(argv[i]);
    }
    argc = static_cast<int>(args.size());
    argv = args.data();
    if (argc < 2 || argc > 3) {
        std::fprintf(stderr, "usage: ps_measure <song.wav> [<song.pscmd>] [--lead <lead.wav>]\n");
        return 1;
    }
    Audio a;
    if (!readWav(argv[1], a) || a.frames() == 0) {
        std::fprintf(stderr, "ps_measure: cannot read %s\n", argv[1]);
        return 1;
    }
    const int64_t total = static_cast<int64_t>(a.frames());

    // Sections from the .pscmd: SONG frames=<n> ..., SECTION <index> <frame> <label>.
    std::vector<Section> sections;
    int64_t songFrames = total;
    double bpm = 0;
    std::string style;
    if (argc == 3) {
        std::ifstream in(argv[2]);
        std::string text;
        while (std::getline(in, text)) {
            std::istringstream ls(text);
            std::string op;
            ls >> op;
            if (op == "SONG") {
                std::string kv;
                while (ls >> kv) {
                    if (kv.rfind("frames=", 0) == 0) songFrames = std::stoll(kv.substr(7));
                    if (kv.rfind("bpm=", 0) == 0) bpm = std::stod(kv.substr(4));
                }
            } else if (op == "SECTION") {
                int index = 0;
                long long frame = 0;
                std::string label;
                ls >> index >> frame;
                std::getline(ls, label);
                label.erase(0, label.find_first_not_of(' '));
                sections.push_back({frame, 0, label});
            } else if (op == "META") {
                std::string key, value;
                ls >> key;
                std::getline(ls, value);
                value.erase(0, value.find_first_not_of(' '));
                if (key == "style") style = value;
            }
        }
    }
    songFrames = std::min(songFrames, total);
    for (size_t i = 0; i < sections.size(); ++i) {
        sections[i].start = std::min(sections[i].start, songFrames);
        sections[i].end = i + 1 < sections.size() ? std::min(sections[i + 1].start, songFrames) : songFrames;
    }

    // Whole file: loudness, LRA, peaks, faults.
    const Loudness whole = loudness(a, {{0, total}}, false, true);
    int64_t clipped = 0, nonFinite = 0;
    double meanL = 0, meanR = 0;
    for (int64_t i = 0; i < total; ++i) {
        const float l = a.l[static_cast<size_t>(i)], r = a.r[static_cast<size_t>(i)];
        if (!std::isfinite(l) || !std::isfinite(r)) {
            ++nonFinite;
            continue;
        }
        if (std::fabs(l) >= 0.999f) ++clipped;
        if (std::fabs(r) >= 0.999f) ++clipped;
        meanL += l;
        meanR += r;
    }
    const double dc = std::max(std::fabs(meanL), std::fabs(meanR)) / static_cast<double>(total);
    double endPeak = 0;
    for (int64_t i = std::max<int64_t>(0, total - a.rate / 2); i < total; ++i) {
        endPeak = std::max({endPeak, static_cast<double>(std::fabs(a.l[static_cast<size_t>(i)])), static_cast<double>(std::fabs(a.r[static_cast<size_t>(i)]))});
    }

    // Per label: every section of that name fed to one meter.
    std::map<std::string, std::vector<std::pair<int64_t, int64_t>>> byLabel;
    std::vector<std::string> order;
    for (const auto& s : sections) {
        if (s.end <= s.start) continue;
        if (!byLabel.count(s.label)) order.push_back(s.label);
        byLabel[s.label].emplace_back(s.start, s.end);
    }
    std::map<std::string, double> labelLufs;
    for (const auto& label : order) labelLufs[label] = loudness(a, byLabel[label]).lufs;

    // The main section: the Drop, else the Refrain, else the loudest; the whole song without sections.
    std::string main;
    if (byLabel.count("Drop")) main = "Drop";
    else if (byLabel.count("Refrain")) main = "Refrain";
    else {
        for (const auto& label : order) {
            if (main.empty() || labelLufs[label] > labelLufs[main]) main = label;
        }
    }
    const auto mainRanges = main.empty() ? std::vector<std::pair<int64_t, int64_t>>{{0, songFrames}} : byLabel[main];
    const Loudness mainLoud = loudness(a, mainRanges, false, true);
    const double wholeTruePeak = truePeak(a, {{0, total}});
    double mainTruePeak = 0;
    for (auto range : mainRanges) mainTruePeak = std::max(mainTruePeak, truePeak(a, {range}));
    const Loudness mainMono = loudness(a, mainRanges, true);
    double peak = 0, energy = 0, count = 0;
    for (auto [s, e] : mainRanges) {
        for (int64_t i = s; i < e; ++i) {
            const double l = a.l[static_cast<size_t>(i)], r = a.r[static_cast<size_t>(i)];
            peak = std::max({peak, std::fabs(l), std::fabs(r)});
            energy += l * l + r * r;
            count += 2;
        }
    }
    const double crest = count > 0 ? dbAmp(peak) - db(energy / count) : 0;
    const Stereo st = stereo(a, mainRanges);
    const Bands b = bands(spectrum(a, mainRanges), a.rate);

    // The main section against the one before it in a song: the Groove (club), the Couplet (song)
    // or the Montée (build-up); and against the Break.
    auto contrast = [&](std::initializer_list<const char*> others) -> std::string {
        if (main.empty() || !labelLufs.count(main)) return "null";
        for (const char* y : others) {
            if (labelLufs.count(y)) return num(labelLufs[main] - labelLufs[y]);
        }
        return "null";
    };

    std::printf("{\n");
    std::printf("  \"file\": %s,\n", quoted(argv[1] + std::string(argv[1]).find_last_of('/') + 1).c_str());
    if (!style.empty()) std::printf("  \"style\": %s,\n", quoted(style).c_str());
    std::printf("  \"seconds\": %s,\n", num(static_cast<double>(total) / a.rate).c_str());
    std::printf("  \"lufs_i\": %s,\n", num(whole.lufs).c_str());
    std::printf("  \"lra\": %s,\n", num(whole.lra).c_str());
    std::printf("  \"true_peak\": %s,\n", num(dbAmp(wholeTruePeak)).c_str());
    std::printf("  \"true_peak_bs1770\": %s,\n", num(dbAmp(whole.truePeak)).c_str());
    std::printf("  \"sample_peak\": %s,\n", num(dbAmp(whole.samplePeak)).c_str());
    std::printf("  \"plr\": %s,\n", num(dbAmp(wholeTruePeak) - whole.lufs).c_str());
    std::printf("  \"clipped\": %lld,\n", static_cast<long long>(clipped));
    std::printf("  \"non_finite\": %lld,\n", static_cast<long long>(nonFinite));
    std::printf("  \"dc\": %s,\n", num(dc, 6).c_str());
    std::printf("  \"end_dbfs\": %s,\n", num(dbAmp(endPeak)).c_str());
    std::printf("  \"main\": %s,\n", quoted(main.empty() ? "song" : main).c_str());
    std::printf("  \"main_lufs\": %s,\n", num(mainLoud.lufs).c_str());
    // S5: the loudness of the song's loudest part as the engine's Drop-mode target reads it (LoudPart).
    {
        ps::LoudPartMeter loudPart;
        loudPart.prepare(a.rate);
        for (int64_t i = 0; i < songFrames; ++i) loudPart.add(a.l[static_cast<size_t>(i)], a.r[static_cast<size_t>(i)]);
        std::printf("  \"loud_lufs\": %s,\n", num(loudPart.lufs()).c_str());
    }
    std::printf("  \"main_true_peak\": %s,\n", num(dbAmp(mainTruePeak)).c_str());
    std::printf("  \"main_crest\": %s,\n", num(crest).c_str());
    std::printf("  \"mono_loss_lu\": %s,\n", num(mainMono.lufs - mainLoud.lufs).c_str());
    std::printf("  \"side_mid\": %s,\n", num(st.sideMid).c_str());
    std::printf("  \"corr\": %s,\n", num(st.corr, 3).c_str());
    std::printf("  \"side_mid_below_120\": %s,\n", num(st.sideMidLow).c_str());
    std::printf("  \"corr_below_120\": %s,\n", num(st.corrLow, 3).c_str());
    std::printf("  \"bands\": {\"sub\": %s, \"low\": %s, \"low_mid\": %s, \"mid\": %s, \"high_mid\": %s, \"air\": %s},\n",
                num(b.sub).c_str(), num(b.low).c_str(), num(b.lowMid).c_str(), num(b.mid).c_str(), num(b.highMid).c_str(), num(b.air).c_str());
    std::printf("  \"slope_db_oct\": %s,\n", num(b.slope).c_str());
    if (!leadPath.empty()) {
        Audio lead;
        Centroid c;
        if (readWav(leadPath.c_str(), lead) && lead.frames() > 0) c = barCentroids(lead, std::min<int64_t>(songFrames, static_cast<int64_t>(lead.frames())), bpm);
        std::printf("  \"lead_centroid_hz\": %s,\n", c.bars ? num(c.median, 0).c_str() : "null");
        std::printf("  \"lead_centroid_var\": %s,\n", c.bars ? num(c.variation).c_str() : "null");
    }
    std::printf("  \"drop_minus_groove\": %s,\n", contrast({"Groove", "Couplet", "Montée"}).c_str());
    std::printf("  \"drop_minus_break\": %s,\n", contrast({"Break"}).c_str());
    // S5: how much each Montée rises over its last 4 bars: the short-term loudness (3 s) just before
    // its last beat (its silence) minus the one 3 s into those 4 bars (4/4; the last section of each
    // run of sections named « Montée »).
    {
        std::vector<double> rises;
        const double beat = bpm > 0 ? 60.0 / bpm * a.rate : 0.0;
        const auto st3 = static_cast<int64_t>(3 * a.rate);
        // The drummer's one-bar fills and crashes (S3) belong to the section they are in.
        std::vector<std::string> owner(sections.size());
        for (size_t i = 0; i < sections.size(); ++i) {
            const bool bar = sections[i].label == "Fill" || sections[i].label == "Crash";
            owner[i] = bar && i > 0 ? owner[i - 1] : sections[i].label;
        }
        for (size_t i = 0; i < sections.size() && beat > 0; ++i) {
            const auto& s = sections[i];
            const bool last = i + 1 == sections.size() || owner[i + 1] != owner[i];
            if (owner[i] != "Montée" || !last) continue;
            const auto end = static_cast<int64_t>(static_cast<double>(s.end) - beat);
            const auto from = static_cast<int64_t>(static_cast<double>(s.end) - 16 * beat) + st3;
            if (from - st3 < 0 || end - st3 <= from) continue;
            const double hi = loudness(a, {{end - st3, end}}).lufs, lo = loudness(a, {{from - st3, from}}).lufs;
            if (std::isfinite(hi) && std::isfinite(lo) && hi > -70 && lo > -70) rises.push_back(hi - lo);
        }
        auto list = [](const char* key, const std::vector<double>& v) {
            std::printf("  \"%s\": [", key);
            for (size_t i = 0; i < v.size(); ++i) std::printf("%s%s", i ? ", " : "", num(v[i]).c_str());
            std::printf("],\n");
        };
        list("montee_rise", rises);
        // S5b: each Drop against the end of the Montée before it: the loudness of the Drop's first 4
        // bars minus that of the Montée's last 2 (its silent last beat left out); a fake drop between
        // them is skipped (the real Drop is the one that must land). About 3 s each, the short-term
        // window. Benjamin's A/B test found Montées that ended louder than their Drop.
        std::vector<double> landings, ends, starts;
        for (size_t i = 0; i < sections.size() && beat > 0; ++i) {
            const auto& s = sections[i];
            const bool last = i + 1 == sections.size() || owner[i + 1] != owner[i];
            if (owner[i] != "Montée" || !last) continue;
            size_t d = i + 1;
            while (d < sections.size() && owner[d] == "Faux drop") ++d;
            if (d >= sections.size() || (owner[d] != "Drop" && owner[d] != "Refrain")) continue;
            const auto endFrom = static_cast<int64_t>(static_cast<double>(s.end) - 9 * beat);
            const auto endTo = static_cast<int64_t>(static_cast<double>(s.end) - beat);
            const int64_t dropFrom = sections[d].start;
            const auto dropTo = std::min<int64_t>(songFrames, static_cast<int64_t>(static_cast<double>(dropFrom) + 16 * beat));
            if (endFrom < 0 || dropTo <= dropFrom) continue;
            const double hi = loudness(a, {{dropFrom, dropTo}}).lufs, lo = loudness(a, {{endFrom, endTo}}).lufs;
            if (std::isfinite(hi) && std::isfinite(lo) && hi > -70 && lo > -70) {
                landings.push_back(hi - lo);
                ends.push_back(lo);
                starts.push_back(hi);
            }
        }
        list("drop_minus_montee_end", landings);
        list("montee_end_lufs", ends);
        list("drop_start_lufs", starts);
    }
    // S5b: the song's Intro (its first sections named « Intro »; the outro, the same scene, left out)
    // against the Drop (or the Refrain): an intro may be sparse, not far under it.
    {
        int64_t introEnd = 0;
        for (const auto& s : sections) {
            if (s.label != "Intro") break;
            introEnd = s.end;
        }
        const double intro = introEnd > 0 ? loudness(a, {{0, introEnd}}).lufs : -HUGE_VAL;
        const bool ok = std::isfinite(intro) && labelLufs.count(main) && std::isfinite(labelLufs[main]);
        std::printf("  \"intro_lufs\": %s,\n", ok ? num(intro).c_str() : "null");
        std::printf("  \"intro_minus_main\": %s,\n", ok ? num(intro - labelLufs[main]).c_str() : "null");
    }
    std::printf("  \"sections\": [");
    for (size_t i = 0; i < sections.size(); ++i) {
        const auto& s = sections[i];
        const double l = s.end > s.start ? loudness(a, {{s.start, s.end}}).lufs : -HUGE_VAL;
        std::printf("%s{\"label\": %s, \"start\": %s, \"seconds\": %s, \"lufs\": %s}", i ? ", " : "", quoted(s.label).c_str(),
                    num(static_cast<double>(s.start) / a.rate).c_str(), num(static_cast<double>(s.end - s.start) / a.rate).c_str(), num(l).c_str());
    }
    std::printf("],\n");
    std::printf("  \"labels\": {");
    for (size_t i = 0; i < order.size(); ++i) {
        std::printf("%s%s: %s", i ? ", " : "", quoted(order[i]).c_str(), num(labelLufs[order[i]]).c_str());
    }
    std::printf("}\n}\n");
    return 0;
}
