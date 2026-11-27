// « Batterie du matin » (docs/sons/matin/BATTERIE.md): each measured drum variant (MeasuredDrums.h),
// rendered offline in 256-frame blocks like the callback, falls in the range measured on reference
// hits of its family and style (docs/sons/matin/CIBLES-BATTERIE.md: p10..p90, with a margin): length
// (-20 and -40 dB under the peak), steady note and drop start (pitched families), brightness
// (spectral centroid), an attack within 5 ms; finite, never clipping, no allocation while rendering.
// Included by engine_tests.cpp (kept apart to ease merging).

#include <complex>
#include <cstring>

#include "MeasuredDrums.h"

extern "C" uint32_t ps_rand_exchange(uint32_t state);  // ps_rand.c

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

std::vector<float> renderRecipe(const ps::measured::Recipe& r, double seconds = 3.0) {
    ps::measured::Voice v;
    v.prepare(kRate);
    v.setRecipe(r);
    std::vector<float> out(static_cast<size_t>(seconds * kRate), 0.0f);
    g_allocations.store(0);
    g_countAllocations.store(true);
    v.trigger(1.0f);
    for (size_t i = 0; i < out.size(); i += 256) v.render(out.data() + i, static_cast<int>(std::min<size_t>(256, out.size() - i)));
    g_countAllocations.store(false);
    return out;
}

std::vector<float> renderHit(int family, int style, double seconds = 3.0) {
    return renderRecipe(ps::measured::recipe(family, style), seconds);
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

bool tightened(int family, int style);  // below: the voices point 3 holds to p25..p75 instead

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
        // A voice the tight test holds keeps only the checks above here (its lengths and brightness: there).
        const bool loose = !tightened(t.family, t.style);
        ok = ok & bad(!loose || !t.t20.set() || within(t20, t.t20, 0.1), "t20") & bad(!loose || within(t40, t.t40, 0.1), "t40");
        ok = ok & bad(!pitched || within(steady, t.steady, 0.08), "note") & bad(!pitched || within(start, t.start, 0.25), "start");
        ok = ok & bad(!loose || within(centroid, t.centroid, 0.25), "centroid");
        std::printf("  %-9s %-9s %s t20 %4.0f t40 %4.0f [%4.0f-%4.0f] note %5.1f [%3.0f-%3.0f] start %4.0f centroid %5.0f [%5.0f-%5.0f] peak %.2f%s\n",
                    kFamilyNames[t.family], kStyleNames[t.style], ok ? "ok  " : "FAIL", t20, t40, double(t.t40.lo), double(t.t40.hi), steady,
                    double(t.steady.lo), double(t.steady.hi), start, centroid, double(t.centroid.lo), double(t.centroid.hi), double(peak), why);
        if (!ok) ++failed;
    }
    CHECK(failed == 0);
}

// Point 3 of the morning list: the voices that passed only thanks to the margin (hats, crash, clap,
// the techhouse kick, the dubstep snare) pulled toward the style's median. Hard limit, no margin: the
// 25th..75th percentiles of the same reference hits (hats sorted closed / open by their measured length,
// not their label). Length at -20 and -60 dB, and brightness (an indication only, but the one the ear flagged).
struct Tight {
    int family, style;
    Range t20, t60, centroid;
};
const Tight kTight[] = {
    {4, 0, {44, 76}, {80, 124}, {6581, 8076}},   {4, 1, {51, 75}, {94, 119}, {7275, 8644}},
    {4, 2, {66, 77}, {96, 147}, {7223, 8511}},   {4, 3, {41, 53}, {85, 103}, {6410, 9546}},
    {4, 4, {39, 69}, {87, 193}, {7111, 10162}},  {4, 5, {50, 96}, {103, 149}, {7907, 10076}},
    {5, 0, {86, 453}, {298, 845}, {6589, 9381}}, {5, 1, {86, 453}, {298, 845}, {6589, 9381}},
    {5, 2, {86, 453}, {298, 845}, {6589, 9381}}, {5, 3, {637, 1279}, {724, 1783}, {9626, 13847}},
    {5, 4, {86, 453}, {298, 845}, {6589, 9381}}, {5, 5, {71, 194}, {120, 400}, {6589, 9381}},
    {8, 0, {610, 1277}, {985, 2228}, {5055, 6805}}, {8, 1, {610, 1277}, {985, 2228}, {5055, 6805}},
    {8, 2, {610, 1277}, {985, 2228}, {5055, 6805}}, {8, 3, {489, 701}, {889, 1038}, {5184, 6313}},
    {8, 4, {519, 1113}, {1269, 2180}, {6424, 7628}}, {8, 5, {1075, 1355}, {1796, 2351}, {4377, 5554}},
    {2, 0, {69, 91}, {129, 230}, {2213, 3194}},  {2, 1, {78, 125}, {192, 236}, {2732, 4275}},
    {2, 2, {81, 148}, {196, 236}, {2979, 4318}}, {2, 3, {69, 123}, {103, 211}, {2610, 3171}},
    {2, 4, {71, 121}, {170, 254}, {2896, 3845}}, {2, 5, {110, 213}, {226, 486}, {2189, 3041}},
    {3, 5, {136, 356}, {447, 938}, {696, 2419}}, {0, 1, {218, 375}, {287, 398}, {93, 140}},
};

bool tightened(int family, int style) {
    for (const Tight& t : kTight)
        if (t.family == family && t.style == style) return true;
    return false;
}

void testMeasuredDrumsTight() {
    int failed = 0;
    for (const Tight& t : kTight) {
        const std::vector<float> x = renderHit(t.family, t.style);
        const std::vector<double> e = envelopeDb(x);
        const double t20 = lastAbove(e, 20.0), t40 = lastAbove(e, 40.0), t60 = lastAbove(e, 60.0), c = centroidHz(x, t40);
        char why[48] = "";
        auto bad = [&why](bool cond, const char* what) {
            if (!cond) std::snprintf(why + std::strlen(why), sizeof(why) - std::strlen(why), " %s", what);
            return cond;
        };
        const bool ok = bad(within(t20, t.t20, 0.0), "t20") & bad(within(t60, t.t60, 0.0), "t60") & bad(within(c, t.centroid, 0.0), "centroid");
        std::printf("  %-9s %-9s %s t20 %5.0f [%4.0f-%4.0f] t60 %5.0f [%4.0f-%4.0f] centroid %5.0f [%5.0f-%5.0f]%s\n", kFamilyNames[t.family],
                    kStyleNames[t.style], ok ? "ok  " : "FAIL", t20, double(t.t20.lo), double(t.t20.hi), t60, double(t.t60.lo), double(t.t60.hi), c,
                    double(t.centroid.lo), double(t.centroid.hi), why);
        if (!ok) ++failed;
    }
    CHECK(failed == 0);
}

// Point 4: the listening variants. Each one safe (attack, no NaN, no clipping, no allocation); the
// low / long one longer than the recipe and the dry / short one shorter (kick: inside the style's
// measured range, its note too); variant 0 is the recipe itself; the deep techno kick round and low.
void testMeasuredDrumsVariants() {
    namespace m = ps::measured;
    int failed = 0;
    auto check = [&failed](const char* name, int style, int which, const m::Recipe& r, double& t40out, double& noteOut) {
        const std::vector<float> x = renderRecipe(r);
        const int allocations = g_allocations.load();
        float peak = 0.0f;
        bool finite = true;
        for (float v : x) {
            finite = finite && std::isfinite(v);
            peak = std::max(peak, std::fabs(v));
        }
        const std::vector<double> e = envelopeDb(x);
        t40out = lastAbove(e, 40.0);
        noteOut = r.bodyLevel > 0.0f && r.startHz > 0.0f ? pitchHz(x, 60.0, std::max(120.0, std::min(lastAbove(e, 20.0), 300.0)), 250.0) : 0.0;
        const bool ok = finite && peak > 0.1f && peak < 1.0f && allocations == 0 && firstAbove(e, 6.0) <= 5.0;
        std::printf("  %-9s %-9s %+d %s t40 %5.0f note %5.1f peak %.2f\n", name, style >= 0 ? kStyleNames[style] : "-", which,
                    ok ? "ok  " : "FAIL", t40out, noteOut, double(peak));
        if (!ok) ++failed;
    };
    const int families[] = {m::Kick, m::Clap, m::Snare, m::ClosedHat};
    for (int f : families)
        for (int s = 0; s < m::kStyles; ++s) {
            double t40[3], note[3];
            for (int w = -1; w <= 1; ++w) check(kFamilyNames[f], s, w, m::variant(f, s, w), t40[w + 1], note[w + 1]);
            const m::Recipe a = m::variant(f, s, 0), b = m::recipe(f, s);
            const bool same = a.noiseLpHz == b.noiseLpHz && a.noiseFallMs == b.noiseFallMs && a.bodyFallMs == b.bodyFallMs && a.endHz == b.endHz;
            const bool order = t40[0] > t40[1] * 1.05 && t40[2] < t40[1] * 0.95;
            const bool pitch = f != m::Kick || (note[0] < note[2] && note[0] > 35.0 && note[2] < 65.0);
            if (!same || !order || !pitch) {
                std::printf("  %-9s %-9s FAIL variants: same %d longer/shorter %d note low<high %d\n", kFamilyNames[f], kStyleNames[s], same, order, pitch);
                ++failed;
            }
        }
    double t40 = 0.0, note = 0.0;
    check("deep kick", -1, 0, m::deepTechnoKick(), t40, note);
    if (!(note > 44.0 && note < 56.0 && t40 > 250.0 && t40 < 500.0 && m::deepTechnoKick().bodyDrive <= 0.25f)) {
        std::printf("  deep kick FAIL: note %.1f (44-56), t40 %.0f (250-500), drive %.2f (<= 0.25)\n", note, t40, double(m::deepTechnoKick().bodyDrive));
        ++failed;
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

// Every voice falls silent and goes inactive once its tails are done (no endless work), in every style.
void testMeasuredDrumsStop() {
    for (int st = 0; st < ps::measured::kStyles; ++st) {
        for (int f = 0; f < ps::measured::kFamilies; ++f) {
            ps::measured::Voice v;
            v.prepare(kRate);
            v.setRecipe(ps::measured::recipe(f, st));
            v.trigger(1.0f);
            std::vector<float> out(256, 0.0f);
            for (int i = 0; i < kRate * 8 / 256 && v.active(); ++i) v.render(out.data(), 256);
            if (v.active()) std::printf("  still active after 8 s: family %d style %d\n", f, st);
            CHECK(!v.active());
        }
    }
}


// « Batterie du matin » in songs: the measured kit is a bench-only extra (CommandType 211,
// SetMeasuredDrums; ps_live --measured-drums). A song with every pad, played through the engine.
// `measured`: the style to post (none when < -1; -1 posts "off"). `onlyLane`: that lane alone (-1: all
// but Snare2, the DaisySP synthetic snare, which already differed between two engines before the
// measured kit: state it does not initialise).
std::vector<float> kitSong(int kitVersion, float measured, int onlyLane = -1, bool countAllocations = false) {
    ps_rand_exchange(0);  // the shared noise state (the voices outside the kit) as on a fresh start
    ps::Engine engine(kRate);
    engine.post({ps::CommandType::SetKitVersion, static_cast<float>(kitVersion)});
    if (measured >= -1.0f) engine.post({static_cast<ps::CommandType>(211), measured});
    engine.post({ps::CommandType::SetTempo, 126.0f});
    engine.post({ps::CommandType::SetPadVoice, float(ps::DrumKit::KickHard), ps::DrumKit::Kick, 0});  // a studio kick kind: measured
    engine.post({ps::CommandType::SetPadVoice, float(ps::DrumKit::Bass808), ps::DrumKit::Kick2, 0});  // the 808 kind: never measured
    for (int lane = 0; lane < ps::DrumKit::kVoices; ++lane) {
        if (onlyLane >= 0 ? lane != onlyLane : lane == ps::DrumKit::Snare2) continue;
        for (int step = lane % 4; step < 16; step += 4) engine.post({ps::CommandType::SetDrumHit, step % 8 == 0 ? 0.9f : 0.6f, lane, step});
    }
    engine.post({ps::CommandType::SetTransport, 1.0f, 0});
    std::vector<float> warm = render(engine, kRate * 2, 256);  // a first pass of the pattern (warm-up: a first loop may allocate)
    std::vector<float> out(static_cast<size_t>(kRate) * 2 * kChannels, 0.0f);
    if (countAllocations) {
        g_allocations.store(0);
        g_countAllocations.store(true);
    }
    for (int pos = 0; pos < kRate * 2; pos += 256)
        engine.process(nullptr, 0, 0, out.data() + static_cast<size_t>(pos) * kChannels, std::min(256, kRate * 2 - pos), kChannels);
    g_countAllocations.store(false);
    out.insert(out.begin(), warm.begin(), warm.end());
    return out;
}

uint64_t fnv(const std::vector<float>& x) {
    uint64_t h = 1469598103934665603ull;
    for (float v : x) {
        uint32_t b;
        std::memcpy(&b, &v, 4);
        for (int k = 0; k < 4; ++k) h = (h ^ ((b >> (8 * k)) & 0xFF)) * 1099511628211ull;
    }
    return h;
}

// By default nothing changes: the same samples, bit for bit, as before the measured kit existed
// (checksums recorded at d898d275, kit v1 and v3), and "off" (-1) is the default.
void testMeasuredKitDefaultIdentical() {
    const uint64_t v1 = fnv(kitSong(1, -2.0f)), v3 = fnv(kitSong(3, -2.0f));
    std::printf("  default song checksums: v1 %016llx v3 %016llx\n", (unsigned long long)v1, (unsigned long long)v3);
    CHECK(v1 == 0x13771f7c1240426bull);
    CHECK(v3 == 0xa6b2386ef329bd87ull);
    CHECK(fnv(kitSong(3, -1.0f)) == v3);
    CHECK(fnv(kitSong(3, 0.0f)) != v3);
}

// The bench's command swaps the kit's voices for the measured ones (kick, snares, clap, hats, rim,
// toms, crash; a studio kick kind too) where measured::choice() has a version, and leaves the others
// (a family kept by the choice table, the 808 kind on pad 1, shaker, cowbell, conga, zap) as they were;
// finite, below full scale, no allocation while playing.
void testMeasuredKitCommand() {
    using K = ps::DrumKit;
    namespace m = ps::measured;
    struct Lane { int voice, family; };
    const Lane lanes[] = {{K::Kick, m::Kick},       {K::Snare, m::Snare},     {K::Snare2, m::Snare},  {K::Clap, m::Clap},
                          {K::ClosedHat, m::ClosedHat}, {K::OpenHat, m::OpenHat}, {K::LowTom, m::Tom}, {K::MidTom, m::Tom},
                          {K::HighTom, m::Tom},     {K::Rim, m::Rim},         {K::Cymbal, m::Crash}};
    const int kept[] = {K::Kick2, K::Cowbell, K::Shaker, K::Conga, K::Zap};
    for (int style = 0; style < m::kStyles; ++style) {
        int right = 0, swapped = 0;
        for (const Lane& l : lanes) {
            const bool changed = fnv(kitSong(3, float(style), l.voice)) != fnv(kitSong(3, -2.0f, l.voice));
            const bool expect = m::choice(l.family, style) != m::ChoiceKeep;
            // Snare2 (DaisySP's synthetic snare) already differs between two engines (see kitSong):
            // kept, its lane cannot be compared, so it only counts when swapped.
            right += changed == expect || (l.voice == K::Snare2 && !expect);
            swapped += changed;
        }
        int same = 0;
        for (int lane : kept) same += fnv(kitSong(3, float(style), lane)) == fnv(kitSong(3, -2.0f, lane));
        const auto all = kitSong(3, float(style), -1, true);
        const int allocations = g_allocations.load();
        float peak = 0.0f;
        bool finite = true;
        double sum = 0.0;
        for (float v : all) {
            finite = finite && std::isfinite(v);
            peak = std::max(peak, std::fabs(v));
            sum += double(v) * v;
        }
        std::printf("  measured kit %-9s swapped %d/11 as chosen %d/11 kept %d/5 peak %.2f rms %.1f dB alloc %d\n", kStyleNames[style], swapped,
                    right, same, double(peak), 10.0 * std::log10(sum / double(all.size()) + 1e-20), allocations);
        CHECK(right == 11 && same == 5);
        CHECK(finite && peak > 0.05f && peak <= 1.0f && allocations == 0);
    }
}

// The chosen versions (measured::chosen, the table of scripts/son/matin_choix.sh): every family and
// style that has one sounds inside its measured range (t20, t40 with the in-range test's 10 % margin,
// the kick's note with 8 %), safe (no NaN, below full scale, no allocation); ChoiceKeep only where the
// target does not count (CONCLUSION-BATTERIE.md: fewer than 20 hits, or the 808's single pack).
void testMeasuredChosenInRange() {
    namespace m = ps::measured;
    int failed = 0, kept = 0;
    for (const Target& t : kTargets) {
        const int c = m::choice(t.family, t.style);
        if (c == m::ChoiceKeep) {
            ++kept;
            continue;
        }
        const std::vector<float> x = renderRecipe(m::chosen(t.family, t.style));
        const int allocations = g_allocations.load();
        float peak = 0.0f;
        bool finite = true;
        for (float v : x) {
            finite = finite && std::isfinite(v);
            peak = std::max(peak, std::fabs(v));
        }
        const std::vector<double> e = envelopeDb(x);
        const double t20 = lastAbove(e, 20.0), t40 = lastAbove(e, 40.0);
        const bool pitched = t.steady.set();
        const double steady = pitched ? pitchHz(x, t.family == 7 ? 40.0 : 60.0, std::max(120.0, std::min(t20, 300.0)), 4.0 * t.steady.hi) : 0.0;
        const bool ok = finite && peak > 0.1f && peak < 1.0f && allocations == 0 && (!t.t20.set() || within(t20, t.t20, 0.1)) &&
                        within(t40, t.t40, 0.1) && (!pitched || within(steady, t.steady, 0.08));
        std::printf("  chosen %-9s %-9s v%+d %s t20 %4.0f [%4.0f-%4.0f] t40 %4.0f [%4.0f-%4.0f] note %5.1f peak %.2f\n", kFamilyNames[t.family],
                    kStyleNames[t.style], c, ok ? "ok  " : "FAIL", t20, double(t.t20.lo), double(t.t20.hi), t40, double(t.t40.lo), double(t.t40.hi),
                    steady, double(peak));
        if (!ok) ++failed;
    }
    std::printf("  chosen: %d kept as the app's own voice\n", kept);
    CHECK(failed == 0);
    // The families without a target that counts keep the app's voice (the 808, rims, house snare...).
    CHECK(m::choice(m::Bass808, m::Trap) == m::ChoiceKeep && m::choice(m::Rim, m::House) == m::ChoiceKeep);
    CHECK(m::choice(m::Kick, m::Techno) != m::ChoiceKeep && m::choice(m::Snare, m::House) == m::ChoiceKeep);
}

}  // namespace matin_tests

#define PS_MATIN_TESTS                                                      \
    {"matin_measured_drums_in_range", matin_tests::testMeasuredDrumsInRange}, \
    {"matin_measured_drums_tight", matin_tests::testMeasuredDrumsTight}, \
    {"matin_measured_drums_variants", matin_tests::testMeasuredDrumsVariants},      \
    {"matin_measured_drums_retrigger", matin_tests::testMeasuredDrumsRetrigger}, \
    {"matin_measured_drums_stop", matin_tests::testMeasuredDrumsStop},      \
    {"matin_measured_kit_default_identical", matin_tests::testMeasuredKitDefaultIdentical}, \
    {"matin_measured_kit_command", matin_tests::testMeasuredKitCommand}, \
    {"matin_measured_chosen_in_range", matin_tests::testMeasuredChosenInRange},
