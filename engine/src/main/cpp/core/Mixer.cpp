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
    pool_->prepare(rate);
    for (auto& strip : inserts_) {
        for (auto& slot : strip) {
            slot.prepare(rate);
            slot.attachPool(pool_.get());
            slot.takeUnit();  // a long type chosen before the stream opened
        }
    }
    for (auto& m : macros_) m.prepare(rate);
    reverb_.prepare(rate);
    delay_.prepare(rate);
    perf_.prepare(rate);
    gateStep_ = 1.0f / std::max(1.0f, kGateMs * 0.001f * rate);
    for (auto& strip : gates_) {
        for (auto& g : strip) g.mix = g.on ? 1.0f : 0.0f;
    }
    gateCount_ = 0;
    release_ = std::exp(-1.0f / (0.12f * rate));  // 120 ms release
    masterGain_.configure(rate, 15.0f);
    masterGain_.snap(masterGain_.target());
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
    auto& s = strip[static_cast<size_t>(slot)];
    const int before = s.type();
    s.setType(type);
    // Another effect in the slot plays in every scene until a SetInsertOn says otherwise (the
    // gate of the one before must not keep it silent).
    if (s.type() != before) gates_[static_cast<size_t>(ch)][static_cast<size_t>(slot)] = Gate{};
    afterInsertChange(ch);
}

void Mixer::clearInserts(int ch) {
    if (ch < 0 || ch >= kChannels) return;
    for (auto& slot : inserts_[static_cast<size_t>(ch)]) slot.setType(InsertSlot::None);
    for (auto& g : gates_[static_cast<size_t>(ch)]) g = Gate{};
    afterInsertChange(ch);
}

void Mixer::afterInsertChange(int ch) {
    auto& strip = inserts_[static_cast<size_t>(ch)];
    // A unit given back may be waiting for: the first slot still without one gets it.
    for (auto& s : inserts_) {
        for (auto& other : s) {
            if (other.waitingForUnit()) other.takeUnit();
        }
    }
    activeInserts_[static_cast<size_t>(ch)] =
        static_cast<int>(std::count_if(strip.begin(), strip.end(), [](const InsertSlot& s) { return s.type() != InsertSlot::None; }));
}

void Mixer::setInsertParam(int ch, int slot, int param, float v) {
    if (ch < 0 || ch >= kChannels || slot < 0 || slot >= kInserts) return;
    inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].setParam(param, v);
}

void Mixer::setInsertOn(int ch, int slot, bool on) {
    if (ch < 0 || ch >= kChannels || slot < 0 || slot >= kInserts) return;
    applyGate(ch, slot, on);
}

void Mixer::scheduleInsertOn(int ch, int slot, bool on, int offset) {
    if (ch < 0 || ch >= kChannels || slot < 0 || slot >= kInserts) return;
    if (offset <= 0 || gateCount_ >= static_cast<int>(gateEvents_.size())) {
        applyGate(ch, slot, on);
        return;
    }
    gateEvents_[static_cast<size_t>(gateCount_++)] = {ch, slot, offset, on};
}

void Mixer::applyGate(int ch, int slot, bool on) {
    auto& g = gates_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    // Back from silence: no tail or LFO left from the scene it last played in.
    if (on && !g.on && g.mix <= 0.0f) inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].reset();
    g.on = on;
}

void Mixer::processInserts(int ch, float* l, float* r, int n) {
    auto& strip = inserts_[static_cast<size_t>(ch)];
    auto& gates = gates_[static_cast<size_t>(ch)];
    for (size_t k = 0; k < strip.size(); ++k) {
        auto& slot = strip[k];
        auto& g = gates[k];
        if (g.on && g.mix >= 1.0f) {
            slot.process(l, r, n);
            continue;
        }
        if (!g.on && g.mix <= 0.0f) continue;  // not in this scene: the sound goes through untouched
        // Gliding in or out: the dry sound and the effect's, crossfaded.
        std::copy(l, l + n, dryL_.begin());
        std::copy(r, r + n, dryR_.begin());
        slot.process(l, r, n);
        const float step = g.on ? gateStep_ : -gateStep_;
        float m = g.mix;
        for (int i = 0; i < n; ++i) {
            m = std::clamp(m + step, 0.0f, 1.0f);
            const auto j = static_cast<size_t>(i);
            l[i] = dryL_[j] + m * (l[i] - dryL_[j]);
            r[i] = dryR_[j] + m * (r[i] - dryR_[j]);
        }
        g.mix = m;
    }
}

void Mixer::setSend(int ch, int bus, float level) {
    if (ch < 0 || ch >= kChannels || ch == kMaster || bus < 0 || bus >= kBuses) return;
    strips_[static_cast<size_t>(ch)].send[static_cast<size_t>(bus)] = std::clamp(level, 0.0f, 1.0f);
    updateTargets();
}

void Mixer::setMacro(int ch, int macro, float v) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    macros_[static_cast<size_t>(ch)].set(macro, v);
    if (macro == TrackMacros::Space) updateTargets();
}

void Mixer::setBusParam(int bus, int param, float v) {
    if (bus == 0) reverb_.setParam(param, v);
    else if (bus == 1) delay_.setParam(param, v);
}

void Mixer::updateTargets() {
    bool anySolo = false;
    for (size_t ch = 0; ch < kFirstPad; ++ch) anySolo |= strips_[ch].solo;
    const auto& drums = strips_[kDrums];
    const bool drumsAudible = !drums.mute && (!anySolo || drums.solo);
    // A pad's dry sound goes through the drums' fader; its sends must follow that fader too.
    const float drumsFader = (drums.volume / 0.8f) * (drums.volume / 0.8f);
    for (size_t ch = 0; ch < strips_.size(); ++ch) {
        auto& s = strips_[ch];
        // A pad's strip goes through the drums' one: heard when the drums are (its reverb too).
        const bool pad = ch >= static_cast<size_t>(kFirstPad);
        const bool audible = !s.mute && (pad ? drumsAudible : (!anySolo || s.solo));
        // Fader: 0.8 = unity gain, 1.0 = +2 dB, quadratic below for a natural feel.
        const float gain = audible ? (s.volume / 0.8f) * (s.volume / 0.8f) : 0.0f;
        // Equal-power pan; centre = -3 dB per side, compensated so centre stays at unity.
        const float angle = (s.pan + 1.0f) * 0.25f * 3.14159265f;
        s.gainL.setTarget(gain * std::cos(angle) * 1.41421356f);
        s.gainR.setTarget(gain * std::sin(angle) * 1.41421356f);
        // Sends follow the fader (post-fader); squared for a usable range. The Space macro adds
        // reverb, and some delay past its middle.
        const float space = macros_[ch].get(TrackMacros::Space);
        const float extra[kBuses] = {0.75f * space, 0.5f * std::max(0.0f, space - 0.5f)};
        for (size_t b = 0; b < s.send.size(); ++b) {
            const float level = std::min(1.0f, s.send[b] + extra[b]);
            s.sendGain[b].setTarget(level * level * (pad ? drumsFader : 1.0f));
        }
    }
}

void Mixer::storeMax(std::atomic<float>& a, float v) {
    float prev = a.load(std::memory_order_relaxed);
    while (v > prev && !a.compare_exchange_weak(prev, v, std::memory_order_acq_rel)) {
    }
}

void Mixer::process(const float* const* channels, const float* const* rights, const float* direct, float* out, int numFrames,
                    int outChannels) {
    for (int offset = 0; offset < numFrames;) {
        int end = std::min(offset + kChunk, numFrames);
        // « Joue dans » switches due now take effect; a later one in this chunk ends it there.
        for (int e = 0; e < gateCount_; ++e) {
            auto& ev = gateEvents_[static_cast<size_t>(e)];
            if (ev.offset < 0) continue;  // done
            if (ev.offset <= offset) {
                applyGate(ev.ch, ev.slot, ev.on);
                ev.offset = -1;
            } else if (ev.offset < end) {
                end = ev.offset;
            }
        }
        const int n = end - offset;
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
        offset = end;
    }
    // A switch past this call's frames (never scheduled so): at once rather than lost.
    for (int e = 0; e < gateCount_; ++e) {
        const auto& ev = gateEvents_[static_cast<size_t>(e)];
        if (ev.offset >= 0) applyGate(ev.ch, ev.slot, ev.on);
    }
    gateCount_ = 0;
    events_ = 0;
}

void Mixer::processChunk(const float* const* channels, const float* const* rights, const float* direct, float* out, int n,
                         int outChannels, int offset) {
    std::fill(mL_.begin(), mL_.begin() + n, 0.0f);
    std::fill(mR_.begin(), mR_.begin() + n, 0.0f);
    for (auto& b : busL_) std::fill(b.begin(), b.begin() + n, 0.0f);
    for (auto& b : busR_) std::fill(b.begin(), b.begin() + n, 0.0f);

    std::fill(padsL_.begin(), padsL_.begin() + n, 0.0f);
    std::fill(padsR_.begin(), padsR_.begin() + n, 0.0f);

    std::array<float, kChannels> blockPeaks{};
    // The pads' strips first: the drums' strip then takes their sum along with the other pads.
    for (int k = 0; k < kChannels; ++k) {
        const int ch = (k + kFirstPad) % kChannels;
        const bool pad = ch >= kFirstPad;
        auto& s = strips_[static_cast<size_t>(ch)];
        if (ch == kMaster || !channels[ch]) continue;  // the master's slot, or an unused track or pad
        const float* inL = channels[ch] + offset;
        const float* inR = rights && rights[ch] ? rights[ch] + offset : inL;
        // A stray NaN must not get into the effects' state (it would stay there for good).
        for (int i = 0; i < n; ++i) {
            chL_[static_cast<size_t>(i)] = std::isfinite(inL[i]) ? inL[i] : 0.0f;
            chR_[static_cast<size_t>(i)] = std::isfinite(inR[i]) ? inR[i] : 0.0f;
        }
        if (ch == kDrums) {
            for (int i = 0; i < n; ++i) {
                chL_[static_cast<size_t>(i)] += padsL_[static_cast<size_t>(i)];
                chR_[static_cast<size_t>(i)] += padsR_[static_cast<size_t>(i)];
            }
        }
        if (activeInserts_[static_cast<size_t>(ch)] > 0) processInserts(ch, chL_.data(), chR_.data(), n);
        macros_[static_cast<size_t>(ch)].process(chL_.data(), chR_.data(), n);
        // An effect that blew up (extreme settings on a very loud sound): this strip goes quiet
        // for one block and its effects start again from silence, instead of a NaN reaching the
        // buses and the master, where it would stay for good.
        bool finite = true;
        for (int i = 0; i < n; ++i) finite &= std::isfinite(chL_[static_cast<size_t>(i)]) && std::isfinite(chR_[static_cast<size_t>(i)]);
        if (!finite) {
            std::fill(chL_.begin(), chL_.begin() + n, 0.0f);
            std::fill(chR_.begin(), chR_.begin() + n, 0.0f);
            for (auto& slot : inserts_[static_cast<size_t>(ch)]) slot.reset();
            macros_[static_cast<size_t>(ch)].reset();
            blewUp_.store(true, std::memory_order_relaxed);
        }
        float peak = 0.0f;
        float* toL = pad ? padsL_.data() : mL_.data();
        float* toR = pad ? padsR_.data() : mR_.data();
        for (int i = 0; i < n; ++i) {
            const float l = chL_[static_cast<size_t>(i)] * s.gainL.next();
            const float r = chR_[static_cast<size_t>(i)] * s.gainR.next();
            toL[i] += l;
            toR[i] += r;
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
    if (activeInserts_[kMaster] > 0 && masterFxOn_) processInserts(kMaster, mL_.data(), mR_.data(), n);
    // Always runs: the repeat and the tape stop need the last seconds already recorded.
    perf_.process(mL_.data(), mR_.data(), n);
    // The same guard as the strips', on the master's own effects: one NaN left there would keep
    // the whole output silent for good.
    bool masterFinite = true;
    for (int i = 0; i < n; ++i) masterFinite &= std::isfinite(mL_[static_cast<size_t>(i)]) && std::isfinite(mR_[static_cast<size_t>(i)]);
    if (!masterFinite) {
        std::fill(mL_.begin(), mL_.begin() + n, 0.0f);
        std::fill(mR_.begin(), mR_.begin() + n, 0.0f);
        for (auto& slot : inserts_[kMaster]) slot.reset();
        perf_.recover();
        reverb_.reset();
        delay_.reset();
        blewUp_.store(true, std::memory_order_relaxed);
    }

    float masterPeak = 0.0f;
    float minGain = 1.0f;
    for (int i = 0; i < n; ++i) {
        float l = mL_[static_cast<size_t>(i)], r = mR_[static_cast<size_t>(i)];
        if (!std::isfinite(l)) l = 0.0f;
        if (!std::isfinite(r)) r = 0.0f;
        // The master's gain, then the peak limiter: instant attack, smooth release, ceiling at
        // -1 dBFS. Without the limiter (a pre-master export) the peaks above 0 dBFS stay as they are.
        const float master = masterGain_.next();
        l *= master;
        r *= master;
        const float peak = std::max(std::fabs(l), std::fabs(r));
        envelope_ = peak > envelope_ ? peak : envelope_ * release_ + peak * (1.0f - release_);
        const float gain = limiterOn_ && envelope_ > kCeiling ? kCeiling / envelope_ : 1.0f;
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
