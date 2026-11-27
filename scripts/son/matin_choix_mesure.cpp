// « Batterie du matin », the choice of versions (docs/sons/matin/CONCLUSION-BATTERIE.md): renders
// every candidate hit of MeasuredDrums.h offline (one hit, 256-frame blocks, like the callback) and
// prints its measures as TSV on stdout, for scripts/son/matin_choix.py.
// Candidates: for kick, clap, snare and closed hat in each style, the recipe (« nouvelle »), variant
// A (-1: low / long) and variant B (+1: dry / short); the deep techno kick; the other families' recipe.
// Measures (the same as engine/host-tests/matin_batterie_tests.inc): t20 / t40 / t60 (ms, last
// millisecond of a 5 ms RMS above peak - N dB), the kick's steady note (Hz), the peak (dBFS), the
// RMS until -40 dB (dBFS), the spectral centroid until -40 dB (Hz, an indication only).
// Usage: matin_choix_mesure > mesures.tsv

#include <algorithm>
#include <cmath>
#include <complex>
#include <cstdio>
#include <vector>

#include "MeasuredDrums.h"

namespace {

constexpr int kRate = 48000;
namespace m = ps::measured;
const char* const kFamilies[] = {"kick", "808", "clap", "caisse_claire", "charley_ferme", "charley_ouvert", "rim", "tom", "crash"};
const char* const kStyles[] = {"house", "techhouse", "techno", "trap", "dnb", "dubstep"};

std::vector<float> renderRecipe(const m::Recipe& r) {
    m::Voice v;
    v.prepare(kRate);
    v.setRecipe(r);
    std::vector<float> out(static_cast<size_t>(3 * kRate), 0.0f);
    v.trigger(1.0f);
    for (size_t i = 0; i < out.size(); i += 256) v.render(out.data() + i, static_cast<int>(std::min<size_t>(256, out.size() - i)));
    return out;
}

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

double lastAbove(const std::vector<double>& e, double below) {
    const double peak = *std::max_element(e.begin(), e.end());
    for (size_t i = e.size(); i-- > 0;)
        if (e[i] >= peak - below) return double(i);
    return 0.0;
}

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

void row(const char* family, const char* style, const char* version, const m::Recipe& r, bool kick) {
    const std::vector<float> x = renderRecipe(r);
    const std::vector<double> e = envelopeDb(x);
    const double t20 = lastAbove(e, 20.0), t40 = lastAbove(e, 40.0), t60 = lastAbove(e, 60.0);
    double peak = 0.0, sum = 0.0;
    const size_t n = std::max<size_t>(1, std::min(x.size(), size_t(t40 * kRate / 1000)));
    for (size_t i = 0; i < x.size(); ++i) peak = std::max(peak, double(std::fabs(x[i])));
    for (size_t i = 0; i < n; ++i) sum += double(x[i]) * x[i];
    const double note = kick ? pitchHz(x, 60.0, std::max(120.0, std::min(t20, 300.0)), 260.0) : 0.0;
    std::printf("%s\t%s\t%s\t%.0f\t%.0f\t%.0f\t%.1f\t%.2f\t%.2f\t%.0f\n", family, style, version, t20, t40, t60, note,
                20.0 * std::log10(peak + 1e-20), 10.0 * std::log10(sum / double(n) + 1e-20), centroidHz(x, t40));
}

}  // namespace

int main() {
    std::printf("famille\tstyle\tversion\tt20\tt40\tt60\tnote\tcrete_db\trms_db\tcentroide\n");
    const char* const names[] = {"A", "nouvelle", "B"};
    for (int f = 0; f < m::kFamilies; ++f)
        for (int s = 0; s < m::kStyles; ++s) {
            const bool choosable = f == m::Kick || f == m::Clap || f == m::Snare || f == m::ClosedHat;
            for (int w = -1; w <= 1; ++w)
                if (choosable || w == 0) row(kFamilies[f], kStyles[s], names[w + 1], m::variant(f, s, w), f == m::Kick);
            if (f == m::Kick && s == m::Techno) row(kFamilies[f], kStyles[s], "deep", m::deepTechnoKick(), true);
        }
    return 0;
}
