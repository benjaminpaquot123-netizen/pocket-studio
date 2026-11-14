#include "Analysis.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <memory>

#include "MagnitudeStft.h"

namespace ps {

namespace {

uint32_t le32(const unsigned char* p) { return p[0] | (p[1] << 8) | (p[2] << 16) | (uint32_t(p[3]) << 24); }
uint16_t le16(const unsigned char* p) { return static_cast<uint16_t>(p[0] | (p[1] << 8)); }

// Beat tracking, written for Pocket Studio from the published method of D. P. W. Ellis, "Beat
// Tracking by Dynamic Programming", J. New Music Research 36(1), 2007:
//  1. onset strength envelope: log-compressed spectral flux every ~5 ms, high-passed and
//     normalised;
//  2. global tempo: autocorrelation of the envelope weighted by a log-normal tempo prior, each lag
//     helped by its half (the off-beat) so a regularly subdivided beat wins over the bar;
//  3. beats: dynamic programming that maximises onset strength at the beats minus a penalty for
//     beat intervals that stray from the tempo (log-ratio squared), then backtracking.
// The beat times then go through the line fit and off-beat check below. Off the audio thread.
class TempoTracker {
public:
    explicit TempoTracker(int sampleRate)
        : rate_(sampleRate), win_(windowFor(sampleRate)), hop_(win_ / 4), lowCoef_(1.0f - std::exp(-2.0f * static_cast<float>(M_PI) * 150.0f / static_cast<float>(sampleRate))),
          stft_(win_, hop_), prevLog_(static_cast<size_t>(win_ / 2 + 1), 0.0f) {
        topBin_ = std::min(win_ / 2, static_cast<int>(8000.0 * win_ / sampleRate));
    }
    void push(const float* x, int n) {
        for (int i = 0; i < n; ++i) {
            // Low-band energy per hop (one-pole low-pass near 150 Hz): where the kicks are.
            const float v = std::isfinite(x[i]) ? x[i] : 0.0f;
            low_ += lowCoef_ * (v - low_);
            lowSum_ += low_ * low_;
            if (++lowFill_ == hop_) {
                lowEnergy_.push_back(lowSum_);
                lowSum_ = 0.0f;
                lowFill_ = 0;
            }
        }
        stft_.push(x, n, [this](const float* mag, int) { frame(mag); });
    }
    void flush() {
        stft_.flush([this](const float* mag, int) { frame(mag); });
        if (lowFill_ > 0) lowEnergy_.push_back(lowSum_);
        lowFill_ = 0;
        track();
    }
    // The tracker's beats are numbered against its coarse period (they may skip a beat where the
    // music stops), then a line through (beat number, time) gives a precise period.
    TempoResult result() const {
        TempoResult r;
        if (beats_.size() < 4 || coarseBpm_ < 30.0f || coarseBpm_ > 300.0f) return r;
        const double coarse = 60.0 / coarseBpm_;
        // Runs of beats whose gaps are whole numbers of the coarse period (a tracker slip, a fill or
        // a break starts a new run). One line per run, sharing the slope: a slip shifts the phase
        // of the next run without tilting the period.
        struct Run { double sx = 0, sy = 0, sxx = 0, sxy = 0; int n = 0; };
        std::vector<Run> runs(1);
        auto add = [&](double x, double y) {
            Run& u = runs.back();
            u.sx += x; u.sy += y; u.sxx += x * x; u.sxy += x * y; ++u.n;
        };
        double x = 0;
        add(0, beats_[0]);
        int agree = 0, gaps = 0;
        for (size_t i = 1; i < beats_.size(); ++i) {
            const double g = beats_[i] - beats_[i - 1];
            const double k = std::max(1.0, std::round(g / coarse));
            ++gaps;
            if (std::fabs(g / k - coarse) < 0.05 * coarse) {
                ++agree;
                x += k;
            } else {
                runs.emplace_back();
                x = 0;
            }
            add(x, beats_[i]);
        }
        double num = 0, den = 0;
        const Run* longest = &runs[0];
        for (const Run& u : runs) {
            if (u.n < 2) continue;
            num += u.sxy - u.sx * u.sy / u.n;
            den += u.sxx - u.sx * u.sx / u.n;
            if (u.n > longest->n) longest = &u;
        }
        if (den <= 0 || longest->n < 4) return r;
        const double period = num / den;
        if (period <= 0.0 || std::fabs(period - coarse) > 0.1 * coarse) return r;
        double bpm = 60.0 / period;
        while (bpm < 70.0) bpm *= 2.0;
        while (bpm >= 180.0) bpm *= 0.5;
        r.bpm = static_cast<float>(std::round(bpm * 10.0) / 10.0);
        r.confidence = static_cast<float>(agree) / static_cast<float>(gaps) * strength_;
        // Phase of the longest run's grid, moved back to the first beat of the song.
        const double intercept = (longest->sy - period * longest->sx) / longest->n;
        double phase = intercept - std::floor(intercept / period) * period;
        // Trackers may lock onto the off-beat (hats): move to the other half of the grid when it
        // clearly has more bass attacks (kicks).
        if (lowAt(phase + period / 2, period) > kOffBeatMargin * lowAt(phase, period)) phase = std::fmod(phase + period / 2, period);
        r.firstBeat = static_cast<float>(phase);
        return r;
    }
    // 1024-sample frames every 256 at 44.1/48 kHz (about 5 ms); shorter at low rates.
    static int windowFor(int rate) {
        int w = 1024;
        while (w > 256 && static_cast<double>(rate) < 40000.0 * w / 1024) w /= 2;
        return w;
    }

private:
    // 1. Onset strength: positive change of log magnitude (up to 8 kHz), summed over the bins.
    void frame(const float* mag) {
        double flux = 0, level = 0;
        for (int k = 1; k <= topBin_; ++k) {
            const float l = std::log1p(kCompression * mag[k]);
            const float d = l - prevLog_[static_cast<size_t>(k)];
            if (d > 0) flux += d;
            prevLog_[static_cast<size_t>(k)] = l;
            level += mag[k];
        }
        envelope_.push_back(static_cast<float>(flux));
        peakLevel_ = std::max(peakLevel_, level);
    }

    double hopSeconds() const { return static_cast<double>(hop_) / rate_; }

    void track() {
        const int n = static_cast<int>(envelope_.size());
        // Silence (or a few hops of it): no tempo.
        if (n < 64 || peakLevel_ < 1e-3) return;
        // High-pass: remove the local mean (about 1 s), keep the rises, normalise.
        const int half = std::max(1, static_cast<int>(0.5 / hopSeconds()));
        std::vector<double> prefix(static_cast<size_t>(n) + 1, 0.0);
        for (int t = 0; t < n; ++t) prefix[static_cast<size_t>(t) + 1] = prefix[static_cast<size_t>(t)] + envelope_[static_cast<size_t>(t)];
        std::vector<float> env(static_cast<size_t>(n));
        double sum2 = 0;
        for (int t = 0; t < n; ++t) {
            const int a = std::max(0, t - half), b = std::min(n, t + half + 1);
            const double mean = (prefix[static_cast<size_t>(b)] - prefix[static_cast<size_t>(a)]) / (b - a);
            const double v = std::max(0.0, envelope_[static_cast<size_t>(t)] - mean);
            env[static_cast<size_t>(t)] = static_cast<float>(v);
            sum2 += v * v;
        }
        const double rms = std::sqrt(sum2 / n);
        if (rms <= 1e-9) return;
        for (float& v : env) v = static_cast<float>(v / rms);

        // 2. Tempo: weighted autocorrelation over 40..240 BPM.
        const double period = estimatePeriod(env);
        if (period <= 0) return;
        coarseBpm_ = static_cast<float>(60.0 / (period * hopSeconds()));

        // 3. Dynamic programming over the envelope smoothed at the beat's scale.
        std::vector<float> local(static_cast<size_t>(n), 0.0f);
        {
            const double sigma = std::max(1.0, period / 32.0);
            const int w = static_cast<int>(std::ceil(3 * sigma));
            std::vector<float> g(static_cast<size_t>(2 * w + 1));
            for (int i = -w; i <= w; ++i) g[static_cast<size_t>(i + w)] = static_cast<float>(std::exp(-0.5 * i * i / (sigma * sigma)));
            for (int t = 0; t < n; ++t) {
                double s = 0;
                for (int i = -w; i <= w; ++i) {
                    const int u = t + i;
                    if (u >= 0 && u < n) s += g[static_cast<size_t>(i + w)] * env[static_cast<size_t>(u)];
                }
                local[static_cast<size_t>(t)] = static_cast<float>(s);
            }
        }
        std::vector<float> score(static_cast<size_t>(n));
        std::vector<int> back(static_cast<size_t>(n), -1);
        const int lo = std::max(1, static_cast<int>(std::round(period / 2))), hi = static_cast<int>(std::round(2 * period));
        std::vector<float> penalty(static_cast<size_t>(hi + 1), 0.0f);
        for (int d = lo; d <= hi; ++d) {
            const double l = std::log(d / period);
            penalty[static_cast<size_t>(d)] = static_cast<float>(-kTightness * l * l);
        }
        for (int t = 0; t < n; ++t) {
            float best = 0.0f;
            int arg = -1;
            for (int d = lo; d <= hi && t - d >= 0; ++d) {
                const float c = score[static_cast<size_t>(t - d)] + penalty[static_cast<size_t>(d)];
                if (arg < 0 || c > best) {
                    best = c;
                    arg = t - d;
                }
            }
            // A beat may also start a fresh chain (song start), never worse than nothing before it.
            if (arg >= 0 && best < 0) arg = -1, best = 0;
            score[static_cast<size_t>(t)] = local[static_cast<size_t>(t)] + best;
            back[static_cast<size_t>(t)] = arg;
        }
        // Last beat: the best-scoring frame in the final period that sits on an onset.
        int last = -1;
        float bestEnd = 0;
        for (int t = std::max(0, n - static_cast<int>(std::ceil(period)) - 1); t < n; ++t) {
            if (last < 0 || score[static_cast<size_t>(t)] > bestEnd) {
                bestEnd = score[static_cast<size_t>(t)];
                last = t;
            }
        }
        std::vector<int> frames;
        for (int t = last; t >= 0; t = back[static_cast<size_t>(t)]) frames.push_back(t);
        std::reverse(frames.begin(), frames.end());
        // Drop the beats of silent edges (intro, outro): weak local strength.
        std::vector<float> strengths;
        for (int f : frames) strengths.push_back(local[static_cast<size_t>(f)]);
        if (strengths.empty()) return;
        std::vector<float> sorted = strengths;
        std::sort(sorted.begin(), sorted.end());
        const float median = sorted[sorted.size() / 2];
        size_t first = 0, end = frames.size();
        while (first < end && strengths[first] < 0.2f * median) ++first;
        while (end > first && strengths[end - 1] < 0.2f * median) --end;
        int onBeat = 0;
        for (size_t i = first; i < end; ++i) {
            if (strengths[i] >= 0.5f * median) ++onBeat;
            // Frame k ends at (k + 1) * hop; an attack peaks the flux about half a window earlier.
            beats_.push_back(static_cast<float>((frames[i] + 1) * hopSeconds() - kAttackLag * win_ / rate_));
        }
        strength_ = end > first ? static_cast<float>(onBeat) / static_cast<float>(end - first) : 0.0f;
    }

    // Beat period in hops (fractional), 0 when nothing periodic.
    double estimatePeriod(const std::vector<float>& env) const {
        const int n = static_cast<int>(env.size());
        const double hop = hopSeconds();
        const int minLag = std::max(2, static_cast<int>(std::floor(60.0 / 240.0 / hop)));
        const int maxLag = static_cast<int>(std::ceil(60.0 / 40.0 / hop));
        const int top = std::min(n - 1, 2 * maxLag + 2);
        if (top <= minLag + 2) return 0;
        std::vector<double> ac(static_cast<size_t>(top + 1), 0.0);
        for (int l = 0; l <= top; ++l) {
            double s = 0;
            for (int t = l; t < n; ++t) s += static_cast<double>(env[static_cast<size_t>(t)]) * env[static_cast<size_t>(t - l)];
            ac[static_cast<size_t>(l)] = s / (n - l);
        }
        auto acAt = [&](double lag) {  // linear interpolation
            const int i = static_cast<int>(lag);
            if (i < 0 || i + 1 > top) return 0.0;
            const double f = lag - i;
            return ac[static_cast<size_t>(i)] * (1 - f) + ac[static_cast<size_t>(i + 1)] * f;
        };
        std::vector<double> weighted(static_cast<size_t>(maxLag + 1), 0.0);
        int best = -1;
        for (int l = minLag; l <= maxLag; ++l) {
            const double bpm = 60.0 / (l * hop);
            const double octaves = std::log2(bpm / kPriorBpm);
            const double prior = std::exp(-0.5 * octaves * octaves / (kPriorWidth * kPriorWidth));
            // Comb: the beat's own lag plus its half (the off-beat eighths): a beat that is
            // regularly subdivided wins over its double, which the bar's repetition would favour.
            const double comb = acAt(l) + acAt(0.5 * l);
            weighted[static_cast<size_t>(l)] = prior * comb;
            if (best < 0 || weighted[static_cast<size_t>(l)] > weighted[static_cast<size_t>(best)]) best = l;
        }
        if (best < 0 || weighted[static_cast<size_t>(best)] <= 0) return 0;
        // Parabolic interpolation of the peak.
        double lag = best;
        if (best > minLag && best < maxLag) {
            const double a = weighted[static_cast<size_t>(best - 1)], b = weighted[static_cast<size_t>(best)], c = weighted[static_cast<size_t>(best + 1)];
            const double den = a - 2 * b + c;
            if (den < 0) lag += 0.5 * (a - c) / den;
        }
        return lag;
    }

    // Low-band attacks (energy rises) summed around every beat of the grid (phase, period) in
    // seconds: kicks, rather than a held bass line.
    double lowAt(double phase, double period) const {
        const double hs = hopSeconds();
        double sum = 0;
        for (double t = phase; t / hs < static_cast<double>(lowEnergy_.size()); t += period) {
            const auto h = static_cast<size_t>(t / hs);
            for (size_t k = std::max<size_t>(h, 2) - 2; k < std::min(h + 6, lowEnergy_.size()); ++k) sum += k ? std::max(0.0f, lowEnergy_[k] - lowEnergy_[k - 1]) : 0.0f;
        }
        return sum;
    }

    static constexpr float kCompression = 100.0f;
    static constexpr double kTightness = 400.0;   // Ellis's alpha: how strictly beats keep the tempo
    // Log-normal tempo prior (centre, width in octaves). Ellis centres it on 120; 130 keeps
    // 165..175 drum and bass at its own tempo rather than half (tuned on the test corpus).
    static constexpr double kPriorBpm = 130.0, kPriorWidth = 0.8;
    static constexpr double kAttackLag = 0.5;    // in windows
    static constexpr double kOffBeatMargin = 1.2;

    int rate_;
    int win_, hop_;
    float lowCoef_;
    float low_ = 0.0f, lowSum_ = 0.0f;
    int lowFill_ = 0;
    std::vector<float> lowEnergy_;
    MagnitudeStft stft_;
    std::vector<float> prevLog_;
    int topBin_ = 0;
    std::vector<float> envelope_;
    double peakLevel_ = 0;
    std::vector<float> beats_;
    float coarseBpm_ = 0.0f;
    float strength_ = 0.0f;
};

// Pitch-class energy (chromagram) over 8192-sample Hann frames, half overlapping.
class Chroma {
public:
    explicit Chroma(int sampleRate) : stft_(kWin, kHop) {
        // Bin -> pitch class, 55 Hz .. 2 kHz (bass to upper harmonics; drums mostly stay out).
        for (int b = 0; b <= kWin / 2; ++b) {
            const double f = static_cast<double>(b) * sampleRate / kWin;
            pc_[static_cast<size_t>(b)] = f < 55.0 || f > 2000.0 ? -1 : (static_cast<int>(std::lround(12.0 * std::log2(f / 440.0))) + 69) % 12;
        }
    }
    void push(const float* x, int n) { stft_.push(x, n, [this](const float* mag, int bins) { frame(mag, bins); }); }
    void flush() { stft_.flush([this](const float* mag, int bins) { frame(mag, bins); }); }
    const std::array<double, 12>& chroma() const { return chroma_; }
    static constexpr int kWin = 8192, kHop = 4096;

private:
    void frame(const float* mag, int bins) {
        for (int b = 0; b < bins; ++b) {
            const int pc = pc_[static_cast<size_t>(b)];
            if (pc >= 0) chroma_[static_cast<size_t>(pc)] += mag[b];
        }
    }

    MagnitudeStft stft_;
    std::array<int, kWin / 2 + 1> pc_{};
    std::array<double, 12> chroma_{};
};

double correlate(const std::array<double, 12>& chroma, const double* profile, int tonic) {
    double mx = 0, my = 0;
    for (int i = 0; i < 12; ++i) { mx += chroma[static_cast<size_t>(i)]; my += profile[i]; }
    mx /= 12; my /= 12;
    double sxy = 0, sxx = 0, syy = 0;
    for (int i = 0; i < 12; ++i) {
        const double x = chroma[static_cast<size_t>((i + tonic) % 12)] - mx, y = profile[i] - my;
        sxy += x * y; sxx += x * x; syy += y * y;
    }
    return sxx > 0 && syy > 0 ? sxy / std::sqrt(sxx * syy) : 0.0;
}

KeyResult keyFromChroma(const std::array<double, 12>& chroma) {
    static constexpr double kMajor[12] = {6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88};
    static constexpr double kMinor[12] = {6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17};
    KeyResult r;
    double total = 0;
    for (double c : chroma) total += c;
    if (total <= 1e-6) return r;
    double best = -2, second = -2;
    for (int t = 0; t < 12; ++t) {
        for (int m = 0; m < 2; ++m) {
            const double c = correlate(chroma, m ? kMinor : kMajor, t);
            if (c > best) {
                second = best;
                best = c;
                r.tonic = t;
                r.minor = m == 1;
            } else if (c > second) {
                second = c;
            }
        }
    }
    r.confidence = static_cast<float>(std::clamp(best - second, 0.0, 1.0));
    return r;
}

}  // namespace

namespace {

bool readWav(const std::string& path, int& sampleRate, bool stereo, const std::function<void(const float*, int)>& sink) {
    std::FILE* f = std::fopen(path.c_str(), "rb");
    if (!f) return false;
    unsigned char h[12];
    if (std::fread(h, 1, 12, f) != 12 || std::memcmp(h, "RIFF", 4) != 0 || std::memcmp(h + 8, "WAVE", 4) != 0) {
        std::fclose(f);
        return false;
    }
    int format = 0, channels = 0, bits = 0;
    sampleRate = 0;
    while (true) {
        unsigned char c[8];
        if (std::fread(c, 1, 8, f) != 8) { std::fclose(f); return false; }
        const uint32_t size = le32(c + 4);
        if (std::memcmp(c, "fmt ", 4) == 0) {
            unsigned char fmt[40] = {};
            const size_t n = std::min<size_t>(size, sizeof(fmt));
            if (size < 16 || std::fread(fmt, 1, n, f) != n) { std::fclose(f); return false; }
            if (size > n) std::fseek(f, static_cast<long>(size - n), SEEK_CUR);
            format = le16(fmt);
            channels = le16(fmt + 2);
            sampleRate = static_cast<int>(le32(fmt + 4));
            bits = le16(fmt + 14);
            if (format == 0xFFFE && size >= 26) format = le16(fmt + 24);  // WAVE_FORMAT_EXTENSIBLE
        } else if (std::memcmp(c, "data", 4) == 0) {
            const bool known = (format == 1 && (bits == 16 || bits == 24 || bits == 32)) || (format == 3 && bits == 32);
            if (!known || channels < 1 || channels > 32 || sampleRate < 8000 || sampleRate > 384000) { std::fclose(f); return false; }
            const int bytesPerFrame = channels * bits / 8;
            constexpr int kFrames = 4096;
            std::vector<unsigned char> raw(static_cast<size_t>(kFrames * bytesPerFrame));
            std::vector<float> mono(kFrames * 2);
            std::vector<float> frame(static_cast<size_t>(channels));
            uint32_t left = size / static_cast<uint32_t>(bytesPerFrame);
            while (left > 0) {
                const int want = static_cast<int>(std::min<uint32_t>(left, kFrames));
                const size_t got = std::fread(raw.data(), static_cast<size_t>(bytesPerFrame), static_cast<size_t>(want), f);
                for (size_t i = 0; i < got; ++i) {
                    for (int ch = 0; ch < channels; ++ch) {
                        const unsigned char* p = raw.data() + i * static_cast<size_t>(bytesPerFrame) + static_cast<size_t>(ch * bits / 8);
                        float v;
                        if (format == 3) { const uint32_t u = le32(p); std::memcpy(&v, &u, 4); }
                        else if (bits == 16) v = static_cast<int16_t>(le16(p)) / 32768.0f;
                        else if (bits == 24) v = static_cast<float>(static_cast<int32_t>((p[0] << 8) | (p[1] << 16) | (uint32_t(p[2]) << 24)) >> 8) / 8388608.0f;
                        else v = static_cast<float>(static_cast<int32_t>(le32(p))) / 2147483648.0f;
                        frame[static_cast<size_t>(ch)] = std::isfinite(v) ? v : 0.0f;
                    }
                    if (!stereo) {
                        float sum = 0;
                        for (float v : frame) sum += v;
                        mono[i] = sum / static_cast<float>(channels);
                    } else if (channels == 1) {
                        mono[i * 2] = mono[i * 2 + 1] = frame[0];
                    } else {
                        float l = 0, r = 0;
                        for (int ch = 0; ch < channels; ++ch) (ch % 2 == 0 ? l : r) += frame[static_cast<size_t>(ch)];
                        const float k = 2.0f / static_cast<float>(channels);
                        mono[i * 2] = l * k;
                        mono[i * 2 + 1] = r * k;
                    }
                }
                if (got > 0) sink(mono.data(), static_cast<int>(got));
                if (got < static_cast<size_t>(want)) break;  // truncated file: keep what was read
                left -= static_cast<uint32_t>(want);
            }
            std::fclose(f);
            return true;
        } else {
            std::fseek(f, static_cast<long>(size + (size & 1)), SEEK_CUR);
        }
    }
}

}  // namespace

bool readWavMono(const std::string& path, int& sampleRate, const std::function<void(const float*, int)>& sink) {
    return readWav(path, sampleRate, false, sink);
}

bool readWavStereo(const std::string& path, int& sampleRate, const std::function<void(const float*, int)>& sink) {
    return readWav(path, sampleRate, true, sink);
}

TempoResult detectTempo(const float* mono, int64_t frames, int sampleRate) {
    if (!mono || frames <= 0 || sampleRate <= 0) return {};
    TempoTracker t(sampleRate);
    for (int64_t pos = 0; pos < frames; pos += 65536) t.push(mono + pos, static_cast<int>(std::min<int64_t>(65536, frames - pos)));
    t.flush();
    return t.result();
}

TempoResult detectTempo(const std::string& wavPath) {
    int rate = 0;
    std::unique_ptr<TempoTracker> t;
    const bool ok = readWavMono(wavPath, rate, [&](const float* x, int n) {
        if (!t) t = std::make_unique<TempoTracker>(rate);
        t->push(x, n);
    });
    if (!ok || !t) return {};
    t->flush();
    return t->result();
}

KeyResult detectKey(const float* mono, int64_t frames, int sampleRate) {
    if (!mono || frames <= 0 || sampleRate <= 0) return {};
    Chroma c(sampleRate);
    for (int64_t pos = 0; pos < frames; pos += 65536) c.push(mono + pos, static_cast<int>(std::min<int64_t>(65536, frames - pos)));
    c.flush();
    return keyFromChroma(c.chroma());
}

KeyResult detectKey(const std::vector<std::string>& wavPaths) {
    std::array<double, 12> sum{};
    for (const auto& path : wavPaths) {
        int rate = 0;
        std::unique_ptr<Chroma> c;
        const bool ok = readWavMono(path, rate, [&](const float* x, int n) {
            if (!c) c = std::make_unique<Chroma>(rate);
            c->push(x, n);
        });
        if (!ok || !c) continue;
        c->flush();
        for (int i = 0; i < 12; ++i) sum[static_cast<size_t>(i)] += c->chroma()[static_cast<size_t>(i)];
    }
    return keyFromChroma(sum);
}

}  // namespace ps
