#include "Mixer.h"

#include <algorithm>

namespace ps {

void Mixer::prepare(int sampleRate) {
    const auto rate = static_cast<float>(sampleRate);
    for (auto& s : strips_) {
        s.gainL.configure(rate, 15.0f);
        s.gainR.configure(rate, 15.0f);
        for (auto& g : s.sendGain) g.configure(rate, 15.0f);
    }
    for (auto& strip : inserts_) {
        for (auto& slot : strip) slot.prepare(rate);
    }
    reverb_.prepare(rate);
    delay_.prepare(rate);
    perf_.prepare(rate);
    release_ = std::exp(-1.0f / (0.12f * rate));  // 120 ms release
    updateTargets();
    for (auto& s : strips_) {
        s.gainL.snap(s.gainL.target());
        s.gainR.snap(s.gainR.target());
        for (auto& g : s.sendGain) g.snap(g.target());
    }
}

void Mixer::setVolume(int ch, float v) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    strips_[static_cast<size_t>(ch)].volume = std::clamp(v, 0.0f, 1.0f);
    updateTargets();
}

void Mixer::setPan(int ch, float pan) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    strips_[static_cast<size_t>(ch)].pan = std::clamp(pan, -1.0f, 1.0f);
    updateTargets();
}

void Mixer::setMute(int ch, bool on) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    strips_[static_cast<size_t>(ch)].mute = on;
    updateTargets();
}

void Mixer::setSolo(int ch, bool on) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    strips_[static_cast<size_t>(ch)].solo = on;
    updateTargets();
}

void Mixer::setInsertType(int ch, int slot, int type) {
    if (ch < 0 || ch >= kChannels || slot < 0 || slot >= kInserts) return;
    auto& strip = inserts_[static_cast<size_t>(ch)];
    strip[static_cast<size_t>(slot)].setType(type);
    activeInserts_[static_cast<size_t>(ch)] =
        static_cast<int>(std::count_if(strip.begin(), strip.end(), [](const InsertSlot& s) { return s.type() != InsertSlot::None; }));
}

void Mixer::setInsertParam(int ch, int slot, int param, float v) {
    if (ch < 0 || ch >= kChannels || slot < 0 || slot >= kInserts) return;
    inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].setParam(param, v);
}

void Mixer::setSend(int ch, int bus, float level) {
    if (ch < 0 || ch >= kChannels || ch == kMaster || bus < 0 || bus >= kBuses) return;
    strips_[static_cast<size_t>(ch)].send[static_cast<size_t>(bus)] = std::clamp(level, 0.0f, 1.0f);
    updateTargets();
}

void Mixer::setBusParam(int bus, int param, float v) {
    if (bus == 0) reverb_.setParam(param, v);
    else if (bus == 1) delay_.setParam(param, v);
}

void Mixer::updateTargets() {
    bool anySolo = false;
    for (const auto& s : strips_) anySolo |= s.solo;
    for (auto& s : strips_) {
        const bool audible = !s.mute && (!anySolo || s.solo);
        // Fader: 0.8 = unity gain, 1.0 = +2 dB, quadratic below for a natural feel.
        const float gain = audible ? (s.volume / 0.8f) * (s.volume / 0.8f) : 0.0f;
        // Equal-power pan; centre = -3 dB per side, compensated so centre stays at unity.
        const float angle = (s.pan + 1.0f) * 0.25f * 3.14159265f;
        s.gainL.setTarget(gain * std::cos(angle) * 1.41421356f);
        s.gainR.setTarget(gain * std::sin(angle) * 1.41421356f);
        // Sends follow the fader (post-fader); squared for a usable range.
        for (size_t b = 0; b < s.send.size(); ++b) s.sendGain[b].setTarget(s.send[b] * s.send[b]);
    }
}

void Mixer::storeMax(std::atomic<float>& a, float v) {
    float prev = a.load(std::memory_order_relaxed);
    while (v > prev && !a.compare_exchange_weak(prev, v, std::memory_order_acq_rel)) {
    }
}

void Mixer::process(const float* const* channels, const float* const* rights, const float* direct, float* out, int numFrames,
                    int outChannels) {
    for (int offset = 0; offset < numFrames; offset += kChunk) {
        const int n = std::min(kChunk, numFrames - offset);
        // Hand this chunk's sidechain events to the pumps (every insert slot, master included).
        for (int e = 0; e < events_; ++e) {
            const int at = eventOffset_[static_cast<size_t>(e)];
            if (at < offset || at >= offset + n) continue;
            for (auto& strip : inserts_) {
                for (auto& slot : strip) slot.sidechain(at - offset, eventKick_[static_cast<size_t>(e)], eventBeat_[static_cast<size_t>(e)]);
            }
            perf_.onStep(at - offset);
        }
        processChunk(channels, rights, direct, out + static_cast<size_t>(offset) * outChannels, n, outChannels, offset);
    }
    events_ = 0;
}

void Mixer::processChunk(const float* const* channels, const float* const* rights, const float* direct, float* out, int n,
                         int outChannels, int offset) {
    std::fill(mL_.begin(), mL_.begin() + n, 0.0f);
    std::fill(mR_.begin(), mR_.begin() + n, 0.0f);
    for (auto& b : busL_) std::fill(b.begin(), b.begin() + n, 0.0f);
    for (auto& b : busR_) std::fill(b.begin(), b.begin() + n, 0.0f);

    std::array<float, kChannels> blockPeaks{};
    for (int ch = 0; ch < kChannels; ++ch) {
        auto& s = strips_[static_cast<size_t>(ch)];
        if (ch == kMaster || !channels[ch]) continue;  // the master's slot, or an unused instrument track
        const float* inL = channels[ch] + offset;
        const float* inR = rights && rights[ch] ? rights[ch] + offset : inL;
        // A stray NaN must not get into the effects' state (it would stay there for good).
        for (int i = 0; i < n; ++i) {
            chL_[static_cast<size_t>(i)] = std::isfinite(inL[i]) ? inL[i] : 0.0f;
            chR_[static_cast<size_t>(i)] = std::isfinite(inR[i]) ? inR[i] : 0.0f;
        }
        if (activeInserts_[static_cast<size_t>(ch)] > 0) {
            for (auto& slot : inserts_[static_cast<size_t>(ch)]) slot.process(chL_.data(), chR_.data(), n);
        }
        float peak = 0.0f;
        for (int i = 0; i < n; ++i) {
            const float l = chL_[static_cast<size_t>(i)] * s.gainL.next();
            const float r = chR_[static_cast<size_t>(i)] * s.gainR.next();
            mL_[static_cast<size_t>(i)] += l;
            mR_[static_cast<size_t>(i)] += r;
            for (size_t b = 0; b < kBuses; ++b) {
                const float g = s.sendGain[b].next();
                busL_[b][static_cast<size_t>(i)] += l * g;
                busR_[b][static_cast<size_t>(i)] += r * g;
            }
            peak = std::max(peak, std::max(std::fabs(l), std::fabs(r)));
        }
        blockPeaks[static_cast<size_t>(ch)] = peak;
    }
    // Send returns, then the metronome / test tone, then the master inserts.
    reverb_.process(busL_[0].data(), busR_[0].data(), mL_.data(), mR_.data(), n);
    delay_.process(busL_[1].data(), busR_[1].data(), mL_.data(), mR_.data(), n);
    if (direct) {
        for (int i = 0; i < n; ++i) {
            mL_[static_cast<size_t>(i)] += direct[offset + i];
            mR_[static_cast<size_t>(i)] += direct[offset + i];
        }
    }
    if (activeInserts_[kMaster] > 0) {
        for (auto& slot : inserts_[kMaster]) slot.process(mL_.data(), mR_.data(), n);
    }
    // Always runs: the repeat and the tape stop need the last seconds already recorded.
    perf_.process(mL_.data(), mR_.data(), n);

    float masterPeak = 0.0f;
    float minGain = 1.0f;
    for (int i = 0; i < n; ++i) {
        float l = mL_[static_cast<size_t>(i)], r = mR_[static_cast<size_t>(i)];
        if (!std::isfinite(l)) l = 0.0f;
        if (!std::isfinite(r)) r = 0.0f;
        // Peak limiter: instant attack, smooth release, ceiling at -1 dBFS.
        const float peak = std::max(std::fabs(l), std::fabs(r));
        envelope_ = peak > envelope_ ? peak : envelope_ * release_ + peak * (1.0f - release_);
        const float gain = envelope_ > kCeiling ? kCeiling / envelope_ : 1.0f;
        minGain = std::min(minGain, gain);
        l *= gain;
        r *= gain;
        masterPeak = std::max(masterPeak, std::max(std::fabs(l), std::fabs(r)));
        if (outChannels == 1) {
            out[i] = 0.5f * (l + r);
        } else {
            out[i * outChannels] = l;
            out[i * outChannels + 1] = r;
            for (int c = 2; c < outChannels; ++c) out[i * outChannels + c] = 0.0f;
        }
    }
    for (int ch = 0; ch < kChannels; ++ch) storeMax(peaks_[static_cast<size_t>(ch)], blockPeaks[static_cast<size_t>(ch)]);
    storeMax(masterPeak_, masterPeak);
    limiterGainOut_.store(minGain, std::memory_order_relaxed);
}

}  // namespace ps
