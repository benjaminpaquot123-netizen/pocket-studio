#include "Analysis.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <memory>

extern "C" {
#include "aubio_priv.h"
#include "cvec.h"
#include "fvec.h"
#include "spectral/phasevoc.h"
#include "tempo/tempo.h"
}

namespace ps {

namespace {

uint32_t le32(const unsigned char* p) { return p[0] | (p[1] << 8) | (p[2] << 16) | (uint32_t(p[3]) << 24); }
uint16_t le16(const unsigned char* p) { return static_cast<uint16_t>(p[0] | (p[1] << 8)); }

// Feeds hop-sized blocks to `fn`, padding the last one with zeros.
class Blocker {
public:
    Blocker(int hop, std::function<void(const float*)> fn) : buf_(static_cast<size_t>(hop)), fn_(std::move(fn)) {}
    void push(const float* x, int n) {
        for (int i = 0; i < n; ++i) {
            buf_[fill_++] = std::isfinite(x[i]) ? x[i] : 0.0f;
            if (fill_ == buf_.size()) { fn_(buf_.data()); fill_ = 0; }
        }
    }
    void flush() {
        if (fill_ == 0) return;
        std::fill(buf_.begin() + static_cast<std::ptrdiff_t>(fill_), buf_.end(), 0.0f);
        fn_(buf_.data());
        fill_ = 0;
    }

private:
    std::vector<float> buf_;
    size_t fill_ = 0;
    std::function<void(const float*)> fn_;
};

class TempoTracker {
public:
    explicit TempoTracker(int sampleRate)
        : rate_(sampleRate), lowCoef_(1.0f - std::exp(-2.0f * static_cast<float>(M_PI) * 150.0f / static_cast<float>(sampleRate))) {
        tempo_ = new_aubio_tempo("default", kWin, kHop, static_cast<uint_t>(sampleRate));
        in_ = new_fvec(kHop);
        out_ = new_fvec(2);
    }
    ~TempoTracker() {
        if (tempo_) del_aubio_tempo(tempo_);
        if (in_) del_fvec(in_);
        if (out_) del_fvec(out_);
    }
    bool ok() const { return tempo_ && in_ && out_; }
    void block(const float* x) {
        std::copy(x, x + kHop, in_->data);
        // Low-band energy per hop (one-pole low-pass near 150 Hz): where the kicks are.
        float e = 0.0f;
        for (int i = 0; i < kHop; ++i) {
            low_ += lowCoef_ * (x[i] - low_);
            e += low_ * low_;
        }
        lowEnergy_.push_back(e);
        aubio_tempo_do(tempo_, in_, out_);
        if (out_->data[0] != 0.0f) beats_.push_back(aubio_tempo_get_last_s(tempo_));
        coarseBpm_ = aubio_tempo_get_bpm(tempo_);
    }
    // aubio's own bpm is coarse (autocorrelation lag steps) and it may report only every second
    // or third beat: number the beats against that coarse period, then fit a line through
    // (beat number, time) for a precise period.
    TempoResult result() const {
        TempoResult r;
        if (beats_.size() < 4 || coarseBpm_ < 30.0f || coarseBpm_ > 300.0f) return r;
        const double coarse = 60.0 / coarseBpm_;
        std::vector<double> index{0.0};
        int agree = 0, gaps = 0;
        for (size_t i = 1; i < beats_.size(); ++i) {
            const double g = beats_[i] - beats_[i - 1];
            const double n = std::max(1.0, std::round(g / coarse));
            index.push_back(index.back() + n);
            ++gaps;
            if (std::fabs(g / n - coarse) < 0.05 * coarse) ++agree;
        }
        const double n = static_cast<double>(beats_.size());
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (size_t i = 0; i < beats_.size(); ++i) {
            sx += index[i]; sy += beats_[i]; sxx += index[i] * index[i]; sxy += index[i] * beats_[i];
        }
        const double den = n * sxx - sx * sx;
        if (den <= 0) return r;
        const double period = (n * sxy - sx * sy) / den;
        if (period <= 0.0 || std::fabs(period - coarse) > 0.1 * coarse) return r;
        double bpm = 60.0 / period;
        while (bpm < 70.0) bpm *= 2.0;
        while (bpm >= 180.0) bpm *= 0.5;
        r.bpm = static_cast<float>(std::round(bpm * 10.0) / 10.0);
        r.confidence = static_cast<float>(agree) / static_cast<float>(gaps);
        // Phase of the fitted grid, moved back to the first beat of the song.
        const double intercept = (sy - period * sx) / n;
        double phase = intercept - std::floor(intercept / period) * period;
        // Trackers may lock onto the off-beat (hats): keep the half of the grid with more bass.
        if (lowAt(phase, period) < lowAt(phase + period / 2, period)) phase = std::fmod(phase + period / 2, period);
        r.firstBeat = static_cast<float>(phase);
        return r;
    }
    static constexpr int kWin = 1024, kHop = 512;

private:
    // Low-band energy summed around every beat of the grid (phase, period) in seconds.
    double lowAt(double phase, double period) const {
        const double hopSeconds = static_cast<double>(kHop) / rate_;
        double sum = 0;
        for (double t = phase; t / hopSeconds < static_cast<double>(lowEnergy_.size()); t += period) {
            const auto h = static_cast<size_t>(t / hopSeconds);
            for (size_t k = h; k < std::min(h + 4, lowEnergy_.size()); ++k) sum += lowEnergy_[k];
        }
        return sum;
    }

    int rate_;
    float lowCoef_;
    float low_ = 0.0f;
    std::vector<float> lowEnergy_;
    aubio_tempo_t* tempo_ = nullptr;
    fvec_t* in_ = nullptr;
    fvec_t* out_ = nullptr;
    std::vector<float> beats_;
    float coarseBpm_ = 0.0f;
};

class Chroma {
public:
    explicit Chroma(int sampleRate) : rate_(sampleRate) {
        pv_ = new_aubio_pvoc(kWin, kHop);
        in_ = new_fvec(kHop);
        grain_ = new_cvec(kWin);
        // Bin -> pitch class, 55 Hz .. 2 kHz (bass to upper harmonics; drums mostly stay out).
        for (int b = 0; b <= kWin / 2; ++b) {
            const double f = static_cast<double>(b) * sampleRate / kWin;
            pc_[static_cast<size_t>(b)] = f < 55.0 || f > 2000.0 ? -1 : (static_cast<int>(std::lround(12.0 * std::log2(f / 440.0))) + 69) % 12;
        }
    }
    ~Chroma() {
        if (pv_) del_aubio_pvoc(pv_);
        if (in_) del_fvec(in_);
        if (grain_) del_cvec(grain_);
    }
    bool ok() const { return pv_ && in_ && grain_; }
    void block(const float* x) {
        std::copy(x, x + kHop, in_->data);
        aubio_pvoc_do(pv_, in_, grain_);
        for (int b = 0; b <= kWin / 2; ++b) {
            const int pc = pc_[static_cast<size_t>(b)];
            if (pc >= 0) chroma_[static_cast<size_t>(pc)] += grain_->norm[b];
        }
    }
    const std::array<double, 12>& chroma() const { return chroma_; }
    static constexpr int kWin = 8192, kHop = 4096;

private:
    int rate_;
    aubio_pvoc_t* pv_ = nullptr;
    fvec_t* in_ = nullptr;
    cvec_t* grain_ = nullptr;
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

bool readWavMono(const std::string& path, int& sampleRate, const std::function<void(const float*, int)>& sink) {
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
            std::vector<float> mono(kFrames);
            uint32_t left = size / static_cast<uint32_t>(bytesPerFrame);
            while (left > 0) {
                const int want = static_cast<int>(std::min<uint32_t>(left, kFrames));
                const size_t got = std::fread(raw.data(), static_cast<size_t>(bytesPerFrame), static_cast<size_t>(want), f);
                for (size_t i = 0; i < got; ++i) {
                    float sum = 0;
                    for (int ch = 0; ch < channels; ++ch) {
                        const unsigned char* p = raw.data() + i * static_cast<size_t>(bytesPerFrame) + static_cast<size_t>(ch * bits / 8);
                        float v;
                        if (format == 3) { const uint32_t u = le32(p); std::memcpy(&v, &u, 4); }
                        else if (bits == 16) v = static_cast<int16_t>(le16(p)) / 32768.0f;
                        else if (bits == 24) v = static_cast<float>(static_cast<int32_t>((p[0] << 8) | (p[1] << 16) | (uint32_t(p[2]) << 24)) >> 8) / 8388608.0f;
                        else v = static_cast<float>(static_cast<int32_t>(le32(p))) / 2147483648.0f;
                        sum += std::isfinite(v) ? v : 0.0f;
                    }
                    mono[i] = sum / static_cast<float>(channels);
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

TempoResult detectTempo(const float* mono, int64_t frames, int sampleRate) {
    if (!mono || frames <= 0 || sampleRate <= 0) return {};
    TempoTracker t(sampleRate);
    if (!t.ok()) return {};
    Blocker b(TempoTracker::kHop, [&](const float* x) { t.block(x); });
    for (int64_t pos = 0; pos < frames; pos += 65536) b.push(mono + pos, static_cast<int>(std::min<int64_t>(65536, frames - pos)));
    b.flush();
    return t.result();
}

TempoResult detectTempo(const std::string& wavPath) {
    int rate = 0;
    std::unique_ptr<TempoTracker> t;
    std::unique_ptr<Blocker> b;
    const bool ok = readWavMono(wavPath, rate, [&](const float* x, int n) {
        if (!t) {
            t = std::make_unique<TempoTracker>(rate);
            b = std::make_unique<Blocker>(TempoTracker::kHop, [&](const float* y) { t->block(y); });
        }
        if (t->ok()) b->push(x, n);
    });
    if (!ok || !t || !t->ok()) return {};
    b->flush();
    return t->result();
}

KeyResult detectKey(const float* mono, int64_t frames, int sampleRate) {
    if (!mono || frames <= 0 || sampleRate <= 0) return {};
    Chroma c(sampleRate);
    if (!c.ok()) return {};
    Blocker b(Chroma::kHop, [&](const float* x) { c.block(x); });
    for (int64_t pos = 0; pos < frames; pos += 65536) b.push(mono + pos, static_cast<int>(std::min<int64_t>(65536, frames - pos)));
    b.flush();
    return keyFromChroma(c.chroma());
}

KeyResult detectKey(const std::vector<std::string>& wavPaths) {
    std::array<double, 12> sum{};
    for (const auto& path : wavPaths) {
        int rate = 0;
        std::unique_ptr<Chroma> c;
        std::unique_ptr<Blocker> b;
        const bool ok = readWavMono(path, rate, [&](const float* x, int n) {
            if (!c) {
                c = std::make_unique<Chroma>(rate);
                b = std::make_unique<Blocker>(Chroma::kHop, [&](const float* y) { c->block(y); });
            }
            if (c->ok()) b->push(x, n);
        });
        if (!ok || !c || !c->ok()) continue;
        b->flush();
        for (int i = 0; i < 12; ++i) sum[static_cast<size_t>(i)] += c->chroma()[static_cast<size_t>(i)];
    }
    return keyFromChroma(sum);
}

}  // namespace ps
