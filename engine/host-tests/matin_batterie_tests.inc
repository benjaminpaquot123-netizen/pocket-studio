// « Batterie du matin » (docs/sons/matin/BATTERIE.md): each measured drum variant (MeasuredDrums.h),
// rendered offline in 256-frame blocks like the callback, falls in the range measured on reference
// hits of its family and style (docs/sons/matin/CIBLES-BATTERIE.md: p10..p90, with a margin): length
// (-20 and -40 dB under the peak), steady note and drop start (pitched families), brightness
// (spectral centroid), an attack within 5 ms; finite, never clipping, no allocation while rendering.
// Included by engine_tests.cpp (kept apart to ease merging).

#include <complex>
#include <cstring>

#include "MeasuredDrums.h"

namespace matin_tests {

struct Range {
    float lo, hi;
    bool set() const { return hi > 0.0f; }
};
struct Target {
    int family, style;
    Range t20, t40, steady, start, centroid;
};

// p10..p90 of the reference measures (docs/sons/matin/CIBLES-BATTERIE.md); {0, 0}: not checked.
const Target kTargets[] = {
    {0, 0, {173.5f, 258.2f}, {222.8f, 326.0f}, {37.3f, 48.2f}, {242.7f, 374.1f}, {63.1f, 122.1f}},
    {0, 1, {193.8f, 384.3f}, {222.8f, 396.3f}, {41.3f, 59.8f}, {274.6f, 411.6f}, {85.9f, 167.6f}},
    {0, 2, {204.9f, 388.0f}, {239.6f, 397.8f}, {44.5f, 64.2f}, {252.8f, 499.6f}, {86.1f, 182.0f}},
    {0, 3, {71.0f, 125.0f}, {96.1f, 156.5f}, {35.1f, 52.8f}, {140.4f, 243.3f}, {56.2f, 79.8f}},
    {0, 4, {100.3f, 183.3f}, {152.9f, 261.2f}, {42.8f, 65.4f}, {119.0f, 309.3f}, {84.9f, 218.1f}},
    {0, 5, {189.6f, 261.3f}, {232.9f, 346.8f}, {39.8f, 54.9f}, {155.1f, 346.1f}, {64.0f, 131.8f}},
    {1, 0, {790.5f, 2283.1f}, {893.3f, 2780.6f}, {28.2f, 65.4f}, {90.3f, 322.8f}, {30.0f, 73.2f}},
    {1, 1, {790.5f, 2283.1f}, {893.3f, 2780.6f}, {28.2f, 65.4f}, {90.3f, 322.8f}, {30.0f, 73.2f}},
    {1, 2, {790.5f, 2283.1f}, {893.3f, 2780.6f}, {28.2f, 65.4f}, {90.3f, 322.8f}, {30.0f, 73.2f}},
    {1, 3, {790.5f, 2283.1f}, {893.3f, 2780.6f}, {28.2f, 65.4f}, {90.3f, 322.8f}, {30.0f, 73.2f}},
    {1, 4, {790.5f, 2283.1f}, {893.3f, 2780.6f}, {28.2f, 65.4f}, {90.3f, 322.8f}, {30.0f, 73.2f}},
    {1, 5, {790.5f, 2283.1f}, {893.3f, 2780.6f}, {28.2f, 65.4f}, {90.3f, 322.8f}, {30.0f, 73.2f}},
    {2, 0, {50.0f, 114.0f}, {83.9f, 213.9f}, {0, 0}, {0, 0}, {1826.4f, 4091.6f}},
    {2, 1, {68.0f, 166.0f}, {114.3f, 228.1f}, {0, 0}, {0, 0}, {2292.0f, 4725.5f}},
    {2, 2, {70.5f, 167.2f}, {141.7f, 239.9f}, {0, 0}, {0, 0}, {2409.7f, 5451.7f}},
    {2, 3, {61.0f, 136.5f}, {80.6f, 220.6f}, {0, 0}, {0, 0}, {2562.3f, 3382.9f}},
    {2, 4, {52.6f, 104.2f}, {81.8f, 183.4f}, {0, 0}, {0, 0}, {2714.2f, 4205.3f}},
    {2, 5, {93.0f, 289.0f}, {150.6f, 463.0f}, {0, 0}, {0, 0}, {1984.8f, 3843.7f}},
    {3, 0, {49.9f, 87.7f}, {80.5f, 141.6f}, {0, 0}, {0, 0}, {667.3f, 1604.7f}},
    {3, 1, {47.3f, 81.2f}, {93.7f, 173.7f}, {0, 0}, {0, 0}, {633.9f, 1391.3f}},
    {3, 2, {47.3f, 81.2f}, {93.7f, 173.7f}, {0, 0}, {0, 0}, {633.9f, 1391.3f}},
    {3, 3, {80.1f, 158.8f}, {127.0f, 242.6f}, {0, 0}, {0, 0}, {1592.1f, 3237.2f}},
    {3, 4, {42.4f, 74.1f}, {75.6f, 123.6f}, {0, 0}, {0, 0}, {2754.3f, 4240.5f}},
    {3, 5, {103.5f, 392.5f}, {293.9f, 708.7f}, {0, 0}, {0, 0}, {614.2f, 2929.0f}},
    {4, 0, {33.3f, 69.2f}, {47.3f, 114.3f}, {0, 0}, {0, 0}, {6610.7f, 9909.2f}},
    {4, 1, {42.2f, 83.1f}, {59.3f, 129.0f}, {0, 0}, {0, 0}, {6852.8f, 10193.8f}},
    {4, 2, {48.0f, 92.0f}, {87.4f, 146.9f}, {0, 0}, {0, 0}, {6856.3f, 10257.3f}},
    {4, 3, {33.8f, 65.7f}, {61.1f, 101.2f}, {0, 0}, {0, 0}, {4783.6f, 11947.8f}},
    {4, 4, {28.4f, 91.9f}, {43.9f, 175.5f}, {0, 0}, {0, 0}, {6138.7f, 12163.2f}},
    {4, 5, {34.9f, 57.7f}, {60.2f, 97.5f}, {0, 0}, {0, 0}, {7497.1f, 11034.5f}},
    {5, 0, {80.6f, 132.9f}, {119.7f, 178.7f}, {0, 0}, {0, 0}, {5197.0f, 7036.4f}},
    {5, 1, {70.3f, 911.2f}, {106.7f, 1006.0f}, {0, 0}, {0, 0}, {6022.8f, 11659.0f}},
    {5, 2, {70.3f, 911.2f}, {106.7f, 1006.0f}, {0, 0}, {0, 0}, {6022.8f, 11659.0f}},
    {5, 3, {550.8f, 1495.2f}, {622.6f, 1936.7f}, {0, 0}, {0, 0}, {8419.7f, 15544.6f}},
    {5, 4, {70.3f, 911.2f}, {106.7f, 1006.0f}, {0, 0}, {0, 0}, {6022.8f, 11659.0f}},
    {5, 5, {71.4f, 194.0f}, {108.9f, 226.4f}, {0, 0}, {0, 0}, {6573.5f, 11297.3f}},
    {6, 0, {30.4f, 64.1f}, {79.2f, 272.6f}, {0, 0}, {0, 0}, {952.7f, 2443.4f}},
    {6, 1, {30.4f, 64.1f}, {79.2f, 272.6f}, {0, 0}, {0, 0}, {952.7f, 2443.4f}},
    {6, 2, {30.4f, 64.1f}, {79.2f, 272.6f}, {0, 0}, {0, 0}, {952.7f, 2443.4f}},
    {6, 3, {30.4f, 64.1f}, {79.2f, 272.6f}, {0, 0}, {0, 0}, {952.7f, 2443.4f}},
    {6, 4, {30.4f, 64.1f}, {79.2f, 272.6f}, {0, 0}, {0, 0}, {952.7f, 2443.4f}},
    {6, 5, {30.4f, 64.1f}, {79.2f, 272.6f}, {0, 0}, {0, 0}, {952.7f, 2443.4f}},
    {7, 0, {100.6f, 194.6f}, {153.2f, 330.0f}, {93.8f, 158.5f}, {110.9f, 234.5f}, {106.3f, 599.3f}},
    {7, 1, {119.1f, 634.4f}, {215.5f, 1030.6f}, {48.0f, 146.6f}, {106.7f, 289.5f}, {118.8f, 1471.6f}},
    {7, 2, {119.1f, 634.4f}, {215.5f, 1030.6f}, {48.0f, 146.6f}, {106.7f, 289.5f}, {118.8f, 1471.6f}},
    {7, 3, {119.1f, 634.4f}, {215.5f, 1030.6f}, {48.0f, 146.6f}, {106.7f, 289.5f}, {118.8f, 1471.6f}},
    {7, 4, {119.1f, 634.4f}, {215.5f, 1030.6f}, {48.0f, 146.6f}, {106.7f, 289.5f}, {118.8f, 1471.6f}},
    {7, 5, {119.1f, 634.4f}, {215.5f, 1030.6f}, {48.0f, 146.6f}, {106.7f, 289.5f}, {118.8f, 1471.6f}},
    {8, 0, {499.3f, 1892.9f}, {835.7f, 2630.3f}, {0, 0}, {0, 0}, {4050.6f, 7576.6f}},
    {8, 1, {499.3f, 1892.9f}, {835.7f, 2630.3f}, {0, 0}, {0, 0}, {4050.6f, 7576.6f}},
    {8, 2, {499.3f, 1892.9f}, {835.7f, 2630.3f}, {0, 0}, {0, 0}, {4050.6f, 7576.6f}},
    {8, 3, {499.3f, 1892.9f}, {835.7f, 2630.3f}, {0, 0}, {0, 0}, {4050.6f, 7576.6f}},
    {8, 4, {367.8f, 765.2f}, {710.2f, 1424.1f}, {0, 0}, {0, 0}, {7435.8f, 11312.0f}},
    {8, 5, {1020.4f, 1536.9f}, {1342.2f, 2320.5f}, {0, 0}, {0, 0}, {3964.8f, 6093.6f}},};

const char* const kFamilyNames[] = {"kick", "808", "clap", "snare", "hh_closed", "hh_open", "rim", "tom", "crash"};
const char* const kStyleNames[] = {"house", "techhouse", "techno", "trap", "dnb", "dubstep"};

std::vector<float> renderHit(int family, int style, double seconds = 3.0) {
    ps::measured::Voice v;
    v.prepare(kRate);
    v.setRecipe(ps::measured::recipe(family, style));
    std::vector<float> out(static_cast<size_t>(seconds * kRate), 0.0f);
    g_allocations.store(0);
    g_countAllocations.store(true);
    v.trigger(1.0f);
    for (size_t i = 0; i < out.size(); i += 256) v.render(out.data() + i, static_cast<int>(std::min<size_t>(256, out.size() - i)));
    g_countAllocations.store(false);
    return out;
}

// 5 ms RMS every millisecond, in dB.
std::vector<double> envelopeDb(const std::vector<float>& x) {
    const size_t hop = kRate / 1000, win = kRate / 200;
    std::vector<double> e;
    for (size_t s = 0; s + win <= x.size(); s += hop) {
        double a = 0.0;
        for (size_t i = s; i < s + win; ++i) a += double(x[i]) * x[i];
        e.push_back(10.0 * std::log10(a / double(win) + 1e-20));
    }
    return e;
}

// The last millisecond still above (peak - below) dB.
double lastAbove(const std::vector<double>& e, double below) {
    const double peak = *std::max_element(e.begin(), e.end());
    for (size_t i = e.size(); i-- > 0;)
        if (e[i] >= peak - below) return double(i);
    return 0.0;
}

double firstAbove(const std::vector<double>& e, double below) {
    const double peak = *std::max_element(e.begin(), e.end());
    for (size_t i = 0; i < e.size(); ++i)
        if (e[i] >= peak - below) return double(i);
    return 0.0;
}

// Mean frequency from the upward zero crossings between two times (ms), low-passed first (one pole at 4 x the band).
double pitchHz(const std::vector<float>& x, double fromMs, double toMs, double lpHz) {
    const double a = std::exp(-2.0 * M_PI * lpHz / kRate);
    double y = 0.0, prev = 0.0, first = -1.0, last = -1.0;
    int crossings = 0;
    const size_t from = size_t(fromMs * kRate / 1000), to = std::min(x.size(), size_t(toMs * kRate / 1000));
    for (size_t i = 0; i < to; ++i) {
        y = (1.0 - a) * x[i] + a * y;
        if (i > from && prev < 0.0 && y >= 0.0) {
            const double t = double(i - 1) + prev / (prev - y);
            if (first < 0.0) first = t;
            else ++crossings;
            last = t;
        }
        prev = y;
    }
    return crossings > 0 ? crossings * kRate / (last - first) : 0.0;
}

void fft(std::vector<std::complex<double>>& a) {
    const size_t n = a.size();
    for (size_t i = 1, j = 0; i < n; ++i) {
        size_t bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) std::swap(a[i], a[j]);
    }
    for (size_t len = 2; len <= n; len <<= 1) {
        const std::complex<double> w = std::polar(1.0, -2.0 * M_PI / double(len));
        for (size_t i = 0; i < n; i += len) {
            std::complex<double> wk = 1.0;
            for (size_t k = 0; k < len / 2; ++k, wk *= w) {
                const auto u = a[i + k], v = a[i + k + len / 2] * wk;
                a[i + k] = u + v;
                a[i + k + len / 2] = u - v;
            }
        }
    }
}

// Magnitude-weighted spectral centroid of the hit until -40 dB (at most 1 s), from 20 Hz up.
double centroidHz(const std::vector<float>& x, double t40Ms) {
    const size_t len = std::max<size_t>(size_t(0.03 * kRate), std::min<size_t>(size_t(t40Ms * kRate / 1000), kRate));
    size_t n = 1;
    while (n < len) n <<= 1;
    std::vector<std::complex<double>> a(n, 0.0);
    for (size_t i = 0; i < len; ++i) a[i] = x[i] * (0.5 - 0.5 * std::cos(2.0 * M_PI * double(i) / double(len)));
    fft(a);
    double num = 0.0, den = 0.0;
    for (size_t k = 1; k < n / 2; ++k) {
        const double hz = double(k) * kRate / double(n);
        if (hz < 20.0) continue;
        num += hz * std::abs(a[k]);
        den += std::abs(a[k]);
    }
    return den > 0.0 ? num / den : 0.0;
}

// The drop's start: the first half period of the low-passed hit (two zero crossings, either way,
// after 0.3 ms; two one-poles at 1.5 kHz keep the click's noise out).
double startHz(const std::vector<float>& x) {
    const double a = std::exp(-2.0 * M_PI * 1500.0 / kRate);
    double y1 = 0.0, y2 = 0.0, prev = 0.0;
    double c[2] = {-1.0, -1.0};
    int found = 0;
    const size_t from = size_t(0.3 * kRate / 1000), to = std::min(x.size(), size_t(kRate / 50));
    for (size_t i = 0; i < to && found < 2; ++i) {
        y1 = (1.0 - a) * x[i] + a * y1;
        y2 = (1.0 - a) * y1 + a * y2;
        if (i > from && ((prev < 0.0) != (y2 < 0.0))) c[found++] = double(i - 1) + prev / (prev - y2);
        prev = y2;
    }
    return found == 2 && c[1] > c[0] ? kRate / (2.0 * (c[1] - c[0])) : 0.0;
}

bool within(double v, Range r, double margin) { return v >= r.lo * (1.0 - margin) && v <= r.hi * (1.0 + margin); }

void testMeasuredDrumsInRange() {
    int failed = 0;
    for (const Target& t : kTargets) {
        const std::vector<float> x = renderHit(t.family, t.style);
        const int allocations = g_allocations.load();
        bool finite = true;
        float peak = 0.0f;
        for (float v : x) {
            finite = finite && std::isfinite(v);
            peak = std::max(peak, std::fabs(v));
        }
        const std::vector<double> e = envelopeDb(x);
        const double t20 = lastAbove(e, 20.0), t40 = lastAbove(e, 40.0), onset = firstAbove(e, 6.0);
        const bool pitched = t.steady.set();
        // The note once the drop is over (from 60 ms, or 40 ms for a tom), until -20 dB (at least 120 ms: two periods of 40 Hz).
        const double steady = pitched ? pitchHz(x, t.family == 7 ? 40.0 : 60.0, std::max(120.0, std::min(t20, 300.0)), 4.0 * t.steady.hi) : 0.0;
        const double start = pitched ? startHz(x) : 0.0;
        const double centroid = centroidHz(x, t40);
        char why[64] = "";
        auto bad = [&why](bool cond, const char* what) {
            if (!cond) std::snprintf(why + std::strlen(why), sizeof(why) - std::strlen(why), " %s", what);
            return cond;
        };
        bool ok = bad(finite, "nan") & bad(peak > 0.1f && peak < 1.0f, "peak") & bad(allocations == 0, "alloc") & bad(onset <= 5.0, "onset");
        ok = ok & bad(!t.t20.set() || within(t20, t.t20, 0.1), "t20") & bad(within(t40, t.t40, 0.1), "t40");
        ok = ok & bad(!pitched || within(steady, t.steady, 0.08), "note") & bad(!pitched || within(start, t.start, 0.25), "start");
        ok = ok & bad(within(centroid, t.centroid, 0.25), "centroid");
        std::printf("  %-9s %-9s %s t20 %4.0f t40 %4.0f [%4.0f-%4.0f] note %5.1f [%3.0f-%3.0f] start %4.0f centroid %5.0f [%5.0f-%5.0f] peak %.2f%s\n",
                    kFamilyNames[t.family], kStyleNames[t.style], ok ? "ok  " : "FAIL", t20, t40, double(t.t40.lo), double(t.t40.hi), steady,
                    double(t.steady.lo), double(t.steady.hi), start, centroid, double(t.centroid.lo), double(t.centroid.hi), double(peak), why);
        if (!ok) ++failed;
    }
    CHECK(failed == 0);
}

// A hit restarted while it rings stays finite and bounded, and a silent voice adds nothing.
void testMeasuredDrumsRetrigger() {
    for (int f = 0; f < ps::measured::kFamilies; ++f) {
        ps::measured::Voice v;
        v.prepare(kRate);
        v.setRecipe(ps::measured::recipe(f, ps::measured::Trap));
        std::vector<float> out(kRate, 0.0f);
        v.render(out.data(), 256);
        CHECK(out[0] == 0.0f && out[255] == 0.0f);
        for (size_t i = 0, block = 0; i < out.size(); i += 256, ++block) {
            if (block % 19 == 0) v.trigger(block % 38 == 0 ? 1.0f : 0.6f);  // about every 100 ms
            v.render(out.data() + i, static_cast<int>(std::min<size_t>(256, out.size() - i)));
        }
        float peak = 0.0f;
        bool finite = true;
        for (float s : out) {
            finite = finite && std::isfinite(s);
            peak = std::max(peak, std::fabs(s));
        }
        CHECK(finite && peak < 1.0f && peak > 0.05f);
    }
}

}  // namespace matin_tests

#define PS_MATIN_TESTS                                                      \
    {"matin_measured_drums_in_range", matin_tests::testMeasuredDrumsInRange}, \
    {"matin_measured_drums_retrigger", matin_tests::testMeasuredDrumsRetrigger},
