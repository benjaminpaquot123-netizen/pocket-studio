#include "Mixer.h"
#include "Params.h"

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
    for (size_t ch = 0; ch < chops_.size(); ++ch) {
        chops_[ch].prepare(rate);
        chops_[ch].setParam(1, chopOn_[ch] ? 1.0f : 0.0f);  // depth: off lets the sound through
    }
    rate_ = rate;
    plate_->prepare(rate);
    reverb_.prepare(rate);
    reverb_.attachPlate(plate_.get());
    delay_.prepare(rate);
    for (int b = 0; b < kBuses; ++b) updateReturnEq(b);
    setMonoBass(monoBassHz_);
    groupDuck_.prepare(rate);
    updateGroupDuck();
    for (auto& r : busReturn_) r.configure(rate, 20.0f);
    perf_.prepare(rate);
    for (auto& iso : *isolators_) iso.prepare(rate);
    for (size_t ch = 0; ch < killWant_.size(); ++ch) {
        for (int b = 0; b < Isolator::kBands; ++b) (*isolators_)[ch].setKill(b, killWant_[ch][static_cast<size_t>(b)]);
    }
    for (auto& m : masterSend_) m.configure(rate, 15.0f);
    gateStep_ = 1.0f / std::max(1.0f, kGateMs * 0.001f * rate);
    for (auto& strip : gates_) {
        for (auto& g : strip) g.mix = g.on ? 1.0f : 0.0f;
    }
    gateCount_ = 0;
    // Stream closed: an effect still waiting for its swap goes in at once.
    for (int ch = 0; ch < kChannels; ++ch) finishSwaps(ch);
    stripLevel_.fill(0.0f);
    chain_->prepare(sampleRate);
    masterGain_.configure(rate, 15.0f);
    masterGain_.snap(masterGain_.target());
    masterFader_.configure(rate, 10.0f);
    masterFader_.snap(masterFader_.target());
    drumsOwn_.configure(rate, 5.0f);
    for (auto& own : trackOwn_) {
        own.configure(rate, 5.0f);
        own.snap(own.target());
    }
    updateTargets();
    for (auto& s : strips_) {
        s.gainL.snap(s.gainL.target());
        s.gainR.snap(s.gainR.target());
        for (auto& g : s.sendGain) g.snap(g.target());
    }
}

void Mixer::setVolume(int ch, float v) {
    if (ch == kMaster) {
        // The master's fader (the song curve's « Fondu d'entrée / de sortie »): after the master
        // chain, the strips' law (0.8 = unity) capped at unity, so the ceiling of the true-peak
        // limiter still holds.
        if (std::isfinite(v)) masterFader_.setTarget(std::min(1.0f, (std::clamp(v, 0.0f, 1.0f) / 0.8f) * (std::clamp(v, 0.0f, 1.0f) / 0.8f)));
        return;
    }
    if (ch < 0 || ch >= kChannels) return;
    if (!clampSetting(strips_[static_cast<size_t>(ch)].volume, v, 0.0f, 1.0f)) return;
    updateTargets();
}

void Mixer::setPan(int ch, float pan) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    if (!clampSetting(strips_[static_cast<size_t>(ch)].pan, pan, -1.0f, 1.0f)) return;
    updateTargets();
}

void Mixer::setMute(int ch, bool on) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    strips_[static_cast<size_t>(ch)].mute = on;
    updateTargets();
}

void Mixer::setLayerGain(int ch, float gain) {
    if (!isLayer(ch)) return;
    if (!clampSetting(strips_[static_cast<size_t>(ch)].linear, gain, 0.0f, 4.0f)) return;
    updateTargets();
}

void Mixer::setLayerThrough(int ch, bool through) {
    if (isLayer(ch)) through_[static_cast<size_t>(ch)] = through;
}

void Mixer::setLayerListen(int track, int mask) {
    if (track < 0 || track >= kLayerTracks) return;
    listen_[static_cast<size_t>(track)] = mask & 0xF;
    trackOwn_[static_cast<size_t>(track)].setTarget((mask & 1) != 0 ? 1.0f : 0.0f);
    updateTargets();
}

void Mixer::setSolo(int ch, bool on) {
    // A layer is heard alone through setLayerListen (its track's solo is the track's).
    if (ch < 0 || ch >= kChannels || ch == kMaster || isLayer(ch)) return;
    strips_[static_cast<size_t>(ch)].solo = on;
    updateTargets();
}

void Mixer::setInsertType(int ch, int slot, int type) {
    if (ch < 0 || ch >= kChannels || slot < 0 || slot >= kInserts) return;
    requestType(ch, slot, type);
}

void Mixer::clearInserts(int ch) {
    if (ch < 0 || ch >= kChannels) return;
    for (int slot = 0; slot < kInserts; ++slot) {
        requestType(ch, slot, InsertSlot::None);
        // An empty slot plays in every scene again.
        if (inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].type() == InsertSlot::None &&
            swaps_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].type < 0) {
            gates_[static_cast<size_t>(ch)][static_cast<size_t>(slot)] = Gate{};
        }
    }
}

void Mixer::finishSwaps(int ch) {
    for (int slot = 0; slot < kInserts; ++slot) {
        auto& sw = swaps_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
        if (sw.type >= 0) swapNow(ch, slot, sw.type, false);
        sw.fade = 1.0f;
    }
}

bool Mixer::slotHeard(int ch, int slot) const {
    if (ch == kMaster && !masterFxOn_) return false;
    if (inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].type() == InsertSlot::None) return false;
    const auto& g = gates_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    if (!g.on && g.mix <= 0.0f) return false;
    return stripLevel_[static_cast<size_t>(ch)] >= kSilentLevel;
}

void Mixer::requestType(int ch, int slot, int type) {
    if (type < 0 || type >= InsertSlot::kTypes) return;
    auto& sw = swaps_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    const int current = inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].type();
    if (sw.type >= 0) {
        // Still fading out: back to the effect in place (it fades in again), or another one waits.
        if (type == current) {
            sw.type = -1;
            sw.mask = 0;
            afterInsertChange(ch);
            return;
        }
        if (type != sw.type) sw.mask = 0;
        sw.type = type;
        sw.gateOn = true;
        return;
    }
    if (type == current) {
        afterInsertChange(ch);  // a slot still waiting for a long effect's unit tries again
        return;
    }
    if (!slotHeard(ch, slot)) {
        // Nothing of the old effect heard: in at once (from silence when the strip sounds).
        swapNow(ch, slot, type, stripLevel_[static_cast<size_t>(ch)] >= kSilentLevel && !(ch == kMaster && !masterFxOn_));
        return;
    }
    sw.type = type;
    sw.gateOn = true;
    sw.mask = 0;
}

void Mixer::swapNow(int ch, int slot, int type, bool fadeIn) {
    auto& s = inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    auto& sw = swaps_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    const bool on = sw.type >= 0 ? sw.gateOn : true;
    s.setType(type);
    // Another effect in the slot plays in every scene until a SetInsertOn says otherwise (the
    // gate of the one before must not keep it silent); put in while the strip sounds, it glides in.
    auto& g = gates_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    g.on = on;
    g.mix = fadeIn ? 0.0f : (on ? 1.0f : 0.0f);
    for (int p = 0; p < InsertSlot::kParams; ++p) {
        if (sw.mask & (1u << p)) s.setParam(p, sw.params[static_cast<size_t>(p)]);
    }
    sw.type = -1;
    sw.mask = 0;
    sw.fade = 1.0f;
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
    auto& sw = swaps_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    if (sw.type >= 0) {
        // For the effect waiting to go in, not the one fading out.
        if (param >= 0 && param < InsertSlot::kParams) {
            sw.params[static_cast<size_t>(param)] = v;
            sw.mask |= 1u << param;
        }
        return;
    }
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
    auto& sw = swaps_[static_cast<size_t>(ch)][static_cast<size_t>(slot)];
    if (sw.type >= 0) sw.gateOn = on;  // for the effect waiting to go in
    // Back from silence: no tail or LFO left from the scene it last played in.
    if (on && !g.on && g.mix <= 0.0f) inserts_[static_cast<size_t>(ch)][static_cast<size_t>(slot)].reset();
    g.on = on;
}

void Mixer::processInserts(int ch, float* l, float* r, int n) {
    auto& strip = inserts_[static_cast<size_t>(ch)];
    auto& gates = gates_[static_cast<size_t>(ch)];
    auto& swaps = swaps_[static_cast<size_t>(ch)];
    for (size_t k = 0; k < strip.size(); ++k) {
        auto& slot = strip[k];
        auto& g = gates[k];
        auto& sw = swaps[k];
        const bool swapping = sw.type >= 0 || sw.fade < 1.0f;
        if (!swapping && g.on && g.mix >= 1.0f) {
            slot.setKey(chunkKey_);
            slot.process(l, r, n);
            meterReduction(ch, k, slot);
            continue;
        }
        if (!g.on && g.mix <= 0.0f) {
            // Not in this scene: the sound goes through untouched (an effect waiting goes in now).
            if (sw.type >= 0) swapNow(ch, static_cast<int>(k), sw.type, true);
            sw.fade = 1.0f;
            continue;
        }
        // Gliding in or out (a scene's « Joue dans », an effect swapped): the dry sound and the
        // effect's, crossfaded. An effect fading out for another one is swapped on the frame its
        // share reaches 0, and the new one glides in from there (the same whatever the blocks).
        for (int from = 0; from < n;) {
            int to = n;
            bool ends = false;  // the old effect's share reaches 0 at `to`
            if (sw.type >= 0) {
                const int left = std::max(1, static_cast<int>(std::ceil(sw.fade / gateStep_ - 1e-3f)));
                if (from + left <= n) {
                    to = from + left;
                    ends = true;
                }
            }
            const int m = to - from;
            std::copy(l + from, l + to, dryL_.begin());
            std::copy(r + from, r + to, dryR_.begin());
            slot.setKey(chunkKey_ ? chunkKey_ + from : nullptr);
            slot.process(l + from, r + from, m);
            const float step = g.on ? gateStep_ : -gateStep_;
            const float fadeStep = sw.type >= 0 ? -gateStep_ : gateStep_;
            float mix = g.mix, fade = sw.fade;
            for (int i = 0; i < m; ++i) {
                mix = std::clamp(mix + step, 0.0f, 1.0f);
                fade = std::clamp(fade + fadeStep, 0.0f, 1.0f);
                const float wet = mix * fade;
                const auto j = static_cast<size_t>(i);
                l[from + i] = dryL_[j] + wet * (l[from + i] - dryL_[j]);
                r[from + i] = dryR_[j] + wet * (r[from + i] - dryR_[j]);
            }
            g.mix = mix;
            sw.fade = fade;
            if (ends) swapNow(ch, static_cast<int>(k), sw.type, true);
            from = to;
        }
        meterReduction(ch, k, slot);
    }
}

void Mixer::setSend(int ch, int bus, float level) {
    if (ch < 0 || ch >= kChannels || ch == kMaster || bus < 0 || bus >= kBuses) return;
    if (!clampSetting(strips_[static_cast<size_t>(ch)].send[static_cast<size_t>(bus)], level, 0.0f, 1.0f)) return;
    updateTargets();
}

void Mixer::setMacro(int ch, int macro, float v) {
    if (ch < 0 || ch >= kChannels || ch == kMaster) return;
    if (macro >= kKillMacro && macro < kKillMacro + Isolator::kBands) {
        const bool on = std::isfinite(v) && v > 0.5f;
        killWant_[static_cast<size_t>(ch)][static_cast<size_t>(macro - kKillMacro)] = on;
        if (!lineWaits()) (*isolators_)[static_cast<size_t>(ch)].setKill(macro - kKillMacro, on);
        return;
    }
    if (macro >= kThrowMacro && macro < kThrowMacro + kBuses) {
        throwEdge(ch, macro - kThrowMacro, std::isfinite(v) && v > 0.5f);
        return;
    }
    if (macro == kChopMacro) {
        // The strip's chopper (« Hachoir de voie »): sixteenths on and off, in time with the beat.
        const bool on = std::isfinite(v) && v > 0.5f;
        auto& was = chopOn_[static_cast<size_t>(ch)];
        if (on == was) return;
        // Switched on between two beats (a finger, any frame): it starts where the beat is, as it
        // does from a song curve on a beat (it only counts while it plays).
        if (on && !chopActive(ch)) chops_[static_cast<size_t>(ch)].syncTo(framesSinceBeat_);
        was = on;
        chops_[static_cast<size_t>(ch)].setParam(1, on ? 1.0f : 0.0f);
        return;
    }
    macros_[static_cast<size_t>(ch)].set(macro, v);
    if (macro == TrackMacros::Space) updateTargets();
}

void Mixer::setBusParam(int bus, int param, float v) {
    if (bus < 0 || bus >= kBuses) return;
    if (param == kBusLowCut || param == kBusHighCut) {
        auto& eq = returnEq_[static_cast<size_t>(bus)];
        if (!clampSetting(param == kBusLowCut ? eq.lowCut : eq.highCut, v, 0.0f, 1.0f)) return;
        updateReturnEq(bus);
        return;
    }
    if (bus == 0) reverb_.setParam(param, v);
    else delay_.setParam(param, v);
}

void Mixer::updateReturnEq(int bus) {
    auto& eq = returnEq_[static_cast<size_t>(bus)];
    const float low = 40.0f * std::pow(25.0f, eq.lowCut);
    const float high = 1000.0f * std::pow(20.0f, eq.highCut);
    eq.hpL.highPass(rate_, low);
    eq.hpR.highPass(rate_, low);
    eq.lpL.lowPass(rate_, std::min(high, 0.45f * rate_));
    eq.lpR.lowPass(rate_, std::min(high, 0.45f * rate_));
}

void Mixer::setMonoBass(float hz) {
    if (std::isnan(hz)) return;
    monoBassHz_ = std::clamp(hz, 0.0f, 400.0f);
    if (monoBassHz_ < 20.0f) {
        monoBassHz_ = 0.0f;
        for (auto& f : monoBass_) f.reset();
        return;
    }
    for (auto& f : monoBass_) f.highPass(rate_, monoBassHz_);
}

void Mixer::setGroupMember(int ch, bool on) {
    if (ch < 0 || ch >= kChannels || ch == kMaster || isPad(ch) || isLayer(ch)) return;  // pads go through the drums, layers through their track
    if (group_[static_cast<size_t>(ch)] == on) return;
    group_[static_cast<size_t>(ch)] = on;
    groupCount_ += on ? 1 : -1;
}

void Mixer::setGroupReturn(int bus, bool on) {
    if (bus < 0 || bus >= kBuses || groupReturn_[static_cast<size_t>(bus)] == on) return;
    groupReturn_[static_cast<size_t>(bus)] = on;
    groupCount_ += on ? 1 : -1;
}

void Mixer::setGroupDuck(int param, float v) {
    if (param < 0 || param >= static_cast<int>(groupParams_.size()) || !std::isfinite(v)) return;
    groupParams_[static_cast<size_t>(param)] = std::clamp(v, 0.0f, 1.0f);
    updateGroupDuck();
}

void Mixer::updateGroupDuck() {
    const float releaseMs = 50.0f * std::pow(12.0f, groupParams_[2]);  // 50..600 ms, the Pump's
    groupDuck_.set(groupParams_[0], groupParams_[1], releaseMs);
}

void Mixer::updateTargets() {
    // Solo: a track's strip, or a pad's (16..31). A soloed pad is heard through the drums' strip,
    // whose own sound (the pads without a strip of their own) then goes quiet, as do the other pads.
    bool anyTrackSolo = false, anyPadSolo = false;
    for (size_t ch = 0; ch < strips_.size(); ++ch) {
        if (!isLayer(static_cast<int>(ch))) (isPad(static_cast<int>(ch)) ? anyPadSolo : anyTrackSolo) |= strips_[ch].solo;
    }
    const bool anySolo = anyTrackSolo || anyPadSolo;
    const auto& drums = strips_[kDrums];
    const bool drumsAudible = !drums.mute && (!anySolo || drums.solo || anyPadSolo);
    drumsOwn_.setTarget(!anyPadSolo || drums.solo ? 1.0f : 0.0f);
    // A pad's dry sound goes through the drums' fader; its sends must follow that fader too.
    const float drumsFader = (drums.volume / 0.8f) * (drums.volume / 0.8f);
    for (size_t ch = 0; ch < strips_.size(); ++ch) {
        auto& s = strips_[ch];
        // A pad's strip goes through the drums' one: heard when the drums are (its reverb too).
        const bool pad = isPad(static_cast<int>(ch));
        const bool padAudible = anyPadSolo ? (s.solo || drums.solo) : (!anySolo || drums.solo);
        bool audible = !s.mute && (pad ? !drums.mute && padAudible : ch == kDrums ? drumsAudible : (!anySolo || s.solo));
        // A layer: heard when its track is (its mute, its solo) and the track's listening takes it.
        const bool layer = isLayer(static_cast<int>(ch));
        float parentFader = 1.0f;
        if (layer) {
            const auto& parent = strips_[static_cast<size_t>(layerParent(static_cast<int>(ch)))];
            const int k = (static_cast<int>(ch) - kFirstLayer) % kLayersPerTrack;
            const bool listened = ((listen_[static_cast<size_t>(layerTrack(static_cast<int>(ch)))] >> (k + 1)) & 1) != 0;
            audible = !s.mute && !parent.mute && (!anySolo || parent.solo) && listened;
            parentFader = (parent.volume / 0.8f) * (parent.volume / 0.8f);
        }
        // Fader: 0.8 = unity gain, 1.0 = +2 dB, quadratic below for a natural feel.
        const float gain = audible ? (s.volume / 0.8f) * (s.volume / 0.8f) * s.linear : 0.0f;
        // Equal-power pan; centre = -3 dB per side, compensated so centre stays at unity.
        const float angle = (s.pan + 1.0f) * 0.25f * 3.14159265f;
        s.gainL.setTarget(gain * std::cos(angle) * 1.41421356f);
        s.gainR.setTarget(gain * std::sin(angle) * 1.41421356f);
        // Sends follow the fader (post-fader); squared for a usable range. The Space macro adds
        // reverb, and some delay past its middle.
        const float space = macros_[ch].get(TrackMacros::Space);
        const float extra[kBuses] = {0.75f * space, 0.5f * std::max(0.0f, space - 0.5f)};
        for (size_t b = 0; b < s.send.size(); ++b) {
            // A throw: the send wide open for its beat.
            const float level = throws_[ch][b].on ? 1.0f : std::min(1.0f, s.send[b] + extra[b]);
            s.sendGain[b].setTarget(level * level * (pad ? drumsFader : layer ? parentFader : 1.0f));
        }
    }
}

void Mixer::storeMax(std::atomic<float>& a, float v) {
    float prev = a.load(std::memory_order_relaxed);
    while (v > prev && !a.compare_exchange_weak(prev, v, std::memory_order_acq_rel)) {
    }
}

void Mixer::storeMin(std::atomic<float>& a, float v) {
    float prev = a.load(std::memory_order_relaxed);
    while (v < prev && !a.compare_exchange_weak(prev, v, std::memory_order_acq_rel)) {
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
        for (int e = 0; e < settingCount_; ++e) {
            auto& ev = settingEvents_[static_cast<size_t>(e)];
            if (ev.offset < 0) continue;
            if (ev.offset <= offset) {
                applySetting(ev);
                ev.offset = -1;
            } else if (ev.offset < end) {
                end = ev.offset;
            }
        }
        // A chunk starts on every step: the live switches waiting for a grid line take effect on
        // its first frame, whatever the size of the blocks (the export sounds like the take).
        for (int e = 0; e < events_; ++e) {
            const int at = eventOffset_[static_cast<size_t>(e)];
            if (at > offset && at < end) end = at;
        }
        for (int e = 0; e < events_; ++e) {
            if (eventOffset_[static_cast<size_t>(e)] == offset) liveLine(eventStep_[static_cast<size_t>(e)], eventBar_[static_cast<size_t>(e)]);
        }
        const int n = end - offset;
        // Without a transport, a throw lasts a beat of frames.
        if (anyThrow_ && !clock_) {
            for (int ch = 0; ch < kChannels; ++ch) {
                for (int b = 0; b < kBuses; ++b) {
                    auto& t = throws_[static_cast<size_t>(ch)][static_cast<size_t>(b)];
                    if (!t.on || t.endStep >= 0) continue;
                    t.framesLeft -= n;
                    if (t.framesLeft <= 0.0) {
                        t.on = false;
                        updateTargets();
                        updateMasterSends();
                    }
                }
            }
        }
        updateMasterSends();
        // Hand this chunk's sidechain events to the pumps (every insert slot, master included).
        int beatAt = -1;
        for (int e = 0; e < events_; ++e) {
            const int at = eventOffset_[static_cast<size_t>(e)];
            if (at < offset || at >= offset + n) continue;
            for (auto& strip : inserts_) {
                for (auto& slot : strip) {
                    slot.sidechain(at - offset, eventKick_[static_cast<size_t>(e)], eventBeat_[static_cast<size_t>(e)], eventStep_[static_cast<size_t>(e)]);
                }
            }
            perf_.onStep(at - offset, eventStep_[static_cast<size_t>(e)], eventBar_[static_cast<size_t>(e)]);
            if (eventBeat_[static_cast<size_t>(e)]) {
                beatAt = at;
                for (size_t ch = 0; ch < chops_.size(); ++ch) {
                    if (chopActive(static_cast<int>(ch))) chops_[ch].beat(at - offset);
                }
            }
        }
        // Frames since the last beat at this chunk's end (a chopper switched on there starts in phase).
        framesSinceBeat_ = beatAt >= 0 ? offset + n - beatAt : framesSinceBeat_ + n;
        processChunk(channels, rights, direct, out + static_cast<size_t>(offset) * outChannels, n, outChannels, offset);
        offset = end;
    }
    // A switch past this call's frames (never scheduled so): at once rather than lost.
    for (int e = 0; e < gateCount_; ++e) {
        const auto& ev = gateEvents_[static_cast<size_t>(e)];
        if (ev.offset >= 0) applyGate(ev.ch, ev.slot, ev.on);
    }
    gateCount_ = 0;
    for (int e = 0; e < settingCount_; ++e) {
        const auto& ev = settingEvents_[static_cast<size_t>(e)];
        if (ev.offset >= 0) applySetting(ev);
    }
    settingCount_ = 0;
    events_ = 0;
}

void Mixer::scheduleSetting(int kind, int a, int b, float v, int offset) {
    const SettingEvent e{kind, a, b, v, std::max(0, offset)};
    // Full (never with 16 lanes a step): at once rather than lost.
    if (settingCount_ >= static_cast<int>(settingEvents_.size())) {
        applySetting(e);
        return;
    }
    settingEvents_[static_cast<size_t>(settingCount_++)] = e;
}

void Mixer::applySetting(const SettingEvent& e) {
    switch (e.kind) {
        case PerformanceSetting: setPerformance(e.a, e.v); break;
        case ReturnSetting: setBusReturn(e.a, e.v); break;
        case MacroSetting: setMacro(e.a, e.b, e.v); break;
        default: break;
    }
}

void Mixer::processChunk(const float* const* channels, const float* const* rights, const float* direct, float* out, int n,
                         int outChannels, int offset) {
    std::fill(mL_.begin(), mL_.begin() + n, 0.0f);
    std::fill(mR_.begin(), mR_.begin() + n, 0.0f);
    for (auto& b : busL_) std::fill(b.begin(), b.begin() + n, 0.0f);
    for (auto& b : busR_) std::fill(b.begin(), b.begin() + n, 0.0f);

    std::fill(padsL_.begin(), padsL_.begin() + n, 0.0f);
    std::fill(padsR_.begin(), padsR_.begin() + n, 0.0f);
    const bool grouped = groupCount_ > 0;
    if (grouped) {
        std::fill(grpL_.begin(), grpL_.begin() + n, 0.0f);
        std::fill(grpR_.begin(), grpR_.begin() + n, 0.0f);
    }
    // S5: the sidechain key for this chunk, before any strip (mono; a NaN is silence).
    chunkKey_ = nullptr;
    if (keyCh_ >= 0) {
        const float* kl = channels[keyCh_] ? channels[keyCh_] + offset : (keyInL_ ? keyInL_ + offset : nullptr);
        const float* kr = channels[keyCh_] ? (rights && rights[keyCh_] ? rights[keyCh_] + offset : kl) : (keyInR_ ? keyInR_ + offset : kl);
        if (kl) {
            for (int i = 0; i < n; ++i) {
                const float k = 0.5f * (kl[i] + kr[i]);
                keyBuf_[static_cast<size_t>(i)] = std::isfinite(k) ? k : 0.0f;
            }
            chunkKey_ = keyBuf_.data();
        }
    }

    fedPre_.fill(false);
    fedPost_.fill(false);
    std::array<float, kChannels> blockPeaks{};
    // The pads' strips first: the drums' strip then takes their sum along with the other pads.
    for (int k = 0; k < kChannels; ++k) {
        const int ch = (k + kFirstPad) % kChannels;
        const bool pad = isPad(ch);
        auto& s = strips_[static_cast<size_t>(ch)];
        if (ch == kMaster) continue;  // the master's slot
        if (!channels[ch]) {  // an unused track or pad
            stripLevel_[static_cast<size_t>(ch)] = 0.0f;
            finishSwaps(ch);  // not heard: an effect waiting goes in now
            continue;
        }
        const float* inL = channels[ch] + offset;
        const float* inR = rights && rights[ch] ? rights[ch] + offset : inL;
        // A stray NaN must not get into the effects' state (it would stay there for good).
        for (int i = 0; i < n; ++i) {
            chL_[static_cast<size_t>(i)] = std::isfinite(inL[i]) ? inL[i] : 0.0f;
            chR_[static_cast<size_t>(i)] = std::isfinite(inR[i]) ? inR[i] : 0.0f;
        }
        if (ch == kDrums) {
            const bool own = drumsOwn_.target() >= 1.0f && drumsOwn_.current() >= 1.0f;
            for (int i = 0; i < n; ++i) {
                // A soloed pad: the drums' own sound (pads without a strip) goes quiet.
                const float g = own ? 1.0f : drumsOwn_.next();
                chL_[static_cast<size_t>(i)] = chL_[static_cast<size_t>(i)] * g + padsL_[static_cast<size_t>(i)];
                chR_[static_cast<size_t>(i)] = chR_[static_cast<size_t>(i)] * g + padsR_[static_cast<size_t>(i)];
            }
        }
        // G1b: a track's layers join its own sound (the part of it heard: setLayerListen).
        const int lt = stripTrack(ch);
        if (lt >= 0) {
            auto& own = trackOwn_[static_cast<size_t>(lt)];
            if (own.target() < 1.0f || own.current() < 1.0f) {
                for (int i = 0; i < n; ++i) {
                    const float g = own.next();
                    chL_[static_cast<size_t>(i)] *= g;
                    chR_[static_cast<size_t>(i)] *= g;
                }
                if (own.target() >= 1.0f && own.current() > 0.99999f) own.snap(1.0f);
            }
            if (fedPre_[static_cast<size_t>(lt)]) {
                const auto& pl = (*preL_)[static_cast<size_t>(lt)];
                const auto& pr = (*preR_)[static_cast<size_t>(lt)];
                for (int i = 0; i < n; ++i) {
                    chL_[static_cast<size_t>(i)] += pl[static_cast<size_t>(i)];
                    chR_[static_cast<size_t>(i)] += pr[static_cast<size_t>(i)];
                }
            }
        }
        // What goes into the inserts and what comes out (an effect's tail): a swap fades if heard.
        float level = blockPeak(chL_.data(), chR_.data(), n);
        if (activeInserts_[static_cast<size_t>(ch)] > 0) {
            processInserts(ch, chL_.data(), chR_.data(), n);
            level = std::max(level, blockPeak(chL_.data(), chR_.data(), n));
        }
        // The layers that pass the track's inserts by (a clean sub under a driven bass).
        if (lt >= 0 && fedPost_[static_cast<size_t>(lt)]) {
            const auto& pl = (*postL_)[static_cast<size_t>(lt)];
            const auto& pr = (*postR_)[static_cast<size_t>(lt)];
            for (int i = 0; i < n; ++i) {
                chL_[static_cast<size_t>(i)] += pl[static_cast<size_t>(i)];
                chR_[static_cast<size_t>(i)] += pr[static_cast<size_t>(i)];
            }
            level = std::max(level, blockPeak(chL_.data(), chR_.data(), n));
        }
        stripLevel_[static_cast<size_t>(ch)] = level;
        macros_[static_cast<size_t>(ch)].process(chL_.data(), chR_.data(), n);
        if (chopActive(ch)) chops_[static_cast<size_t>(ch)].process(chL_.data(), chR_.data(), n);
        auto& iso = (*isolators_)[static_cast<size_t>(ch)];
        if (iso.active()) iso.process(chL_.data(), chR_.data(), n);
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
            iso.reset();
            blewUp_.store(true, std::memory_order_relaxed);
        }
        float peak = 0.0f;
        const bool toGroup = grouped && group_[static_cast<size_t>(ch)];
        float* toL = pad ? padsL_.data() : toGroup ? grpL_.data() : mL_.data();
        float* toR = pad ? padsR_.data() : toGroup ? grpR_.data() : mR_.data();
        if (isLayer(ch)) {
            // Into its track's strip (processed after every layer's: kFirstLayer > the tracks' strips
            // in the order above), before or after the track's inserts.
            const auto t = static_cast<size_t>(layerTrack(ch));
            const bool pre = through_[static_cast<size_t>(ch)];
            auto& fed = pre ? fedPre_[t] : fedPost_[t];
            toL = (pre ? *preL_ : *postL_)[t].data();
            toR = (pre ? *preR_ : *postR_)[t].data();
            if (!fed) {
                std::fill(toL, toL + n, 0.0f);
                std::fill(toR, toR + n, 0.0f);
                fed = true;
            }
        }
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
    // The whole mix into the buses (the build-up, a whole-mix throw).
    for (size_t b = 0; b < kBuses; ++b) {
        auto& g = masterSend_[b];
        if (g.target() <= 0.0f && g.current() <= 0.0f) continue;
        for (int i = 0; i < n; ++i) {
            // Back at rest on a frame of its own (not a block's): the same whatever the blocks.
            if (g.target() <= 0.0f && g.current() < 1e-5f) g.snap(0.0f);
            const float k = g.next();
            // The Musique bus is part of the whole mix.
            const float wl = grouped ? mL_[static_cast<size_t>(i)] + grpL_[static_cast<size_t>(i)] : mL_[static_cast<size_t>(i)];
            const float wr = grouped ? mR_[static_cast<size_t>(i)] + grpR_[static_cast<size_t>(i)] : mR_[static_cast<size_t>(i)];
            busL_[b][static_cast<size_t>(i)] += wl * k;
            busR_[b][static_cast<size_t>(i)] += wr * k;
        }
    }
    // Send returns, then the master inserts and the Live effects; the metronome / test tone after
    // them (a tape stop or the DJ filter must not take the click along), before the limiter.
    for (size_t b = 0; b < kBuses; ++b) {
        auto& ret = busReturn_[b];
        // Turned by the live grid, or equalised (S4): into its own buffer first.
        const bool plain = ret.target() == 1.0f && ret.current() == 1.0f && !returnEq_[b].active();
        // Turned by the live grid: the effect plays into its own buffer, then into the mix scaled
        // (S5: or into the Musique bus).
        const bool retGroup = grouped && groupReturn_[b];
        float* mixL = retGroup ? grpL_.data() : mL_.data();
        float* mixR = retGroup ? grpR_.data() : mR_.data();
        float* outL = plain ? mixL : retL_.data();
        float* outR = plain ? mixR : retR_.data();
        if (!plain) {
            std::fill(retL_.begin(), retL_.begin() + n, 0.0f);
            std::fill(retR_.begin(), retR_.begin() + n, 0.0f);
        }
        if (b == 0) reverb_.process(busL_[b].data(), busR_[b].data(), outL, outR, n);
        else delay_.process(busL_[b].data(), busR_[b].data(), outL, outR, n);
        if (plain) continue;
        auto& eq = returnEq_[b];
        if (eq.active()) {
            for (int i = 0; i < n; ++i) {
                float l = retL_[static_cast<size_t>(i)], r = retR_[static_cast<size_t>(i)];
                if (eq.lowCut > 0.0f) {
                    l = eq.hpL.process(l);
                    r = eq.hpR.process(r);
                }
                if (eq.highCut < 1.0f) {
                    l = eq.lpL.process(l);
                    r = eq.lpR.process(r);
                }
                retL_[static_cast<size_t>(i)] = l;
                retR_[static_cast<size_t>(i)] = r;
            }
        }
        for (int i = 0; i < n; ++i) {
            const float g = ret.next();
            mixL[i] += retL_[static_cast<size_t>(i)] * g;
            mixR[i] += retR_[static_cast<size_t>(i)] * g;
        }
        // Back at rest: the plain path again from the next block.
        if (std::fabs(ret.current() - ret.target()) < 1e-5f) ret.snap(ret.target());
    }
    // S5: the Musique bus, ducked once under the key, joins the mix.
    if (grouped) {
        if (groupDuck_.active()) groupDuck_.process(grpL_.data(), grpR_.data(), chunkKey_, n);
        for (int i = 0; i < n; ++i) {
            mL_[static_cast<size_t>(i)] += grpL_[static_cast<size_t>(i)];
            mR_[static_cast<size_t>(i)] += grpR_[static_cast<size_t>(i)];
        }
    }
    float masterLevel = blockPeak(mL_.data(), mR_.data(), n);
    if (activeInserts_[kMaster] > 0 && masterFxOn_) {
        processInserts(kMaster, mL_.data(), mR_.data(), n);
        masterLevel = std::max(masterLevel, blockPeak(mL_.data(), mR_.data(), n));
    } else {
        finishSwaps(kMaster);
    }
    stripLevel_[kMaster] = masterLevel;
    // The same guard as the strips', on the send effects and the master's own effects: one NaN
    // left there would keep the whole output silent for good. Checked before the Live effects,
    // which write what they hear into their memory with every NaN made 0 (the NaN's source would
    // then never be found nor reset), and once more after them, for their own state.
    auto finiteMix = [this, n]() {
        bool finite = true;
        for (int i = 0; i < n; ++i) finite &= std::isfinite(mL_[static_cast<size_t>(i)]) && std::isfinite(mR_[static_cast<size_t>(i)]);
        return finite;
    };
    if (!finiteMix()) {
        std::fill(mL_.begin(), mL_.begin() + n, 0.0f);
        std::fill(mR_.begin(), mR_.begin() + n, 0.0f);
        for (auto& slot : inserts_[kMaster]) slot.reset();
        reverb_.reset();
        delay_.reset();
        blewUp_.store(true, std::memory_order_relaxed);
    }
    // Always runs: the repeat and the tape stop need the last seconds already recorded.
    perf_.process(mL_.data(), mR_.data(), n);
    if (!finiteMix()) {
        std::fill(mL_.begin(), mL_.begin() + n, 0.0f);
        std::fill(mR_.begin(), mR_.begin() + n, 0.0f);
        perf_.recover();
        blewUp_.store(true, std::memory_order_relaxed);
    }

    // S4: the lows in mono (the side high-passed), before the master's gain and chain.
    if (monoBassHz_ > 0.0f) {
        for (int i = 0; i < n; ++i) {
            const float l = mL_[static_cast<size_t>(i)], r = mR_[static_cast<size_t>(i)];
            const float mid = 0.5f * (l + r);
            const float side = monoBass_[1].process(monoBass_[0].process(0.5f * (l - r)));
            mL_[static_cast<size_t>(i)] = mid + side;
            mR_[static_cast<size_t>(i)] = mid - side;
        }
    }
    // The master's gain, then the master chain (drive, clipper, true-peak limiter, meter), the
    // click / test tone added after the drive. Without the limiter (a pre-master export) the peaks
    // over 0 dBFS stay as they are, delayed the same.
    bool directBlewUp = false;
    // S7 (mix version 4, live): the master's fader here, before the chain, as in an export.
    const bool preFader = faderBeforeChain_ && !chainBypass_;
    const bool faderAtUnity = masterFader_.target() == 1.0f && masterFader_.current() == 1.0f;
    for (int i = 0; i < n; ++i) {
        float l = mL_[static_cast<size_t>(i)], r = mR_[static_cast<size_t>(i)];
        if (!std::isfinite(l)) l = 0.0f;
        if (!std::isfinite(r)) r = 0.0f;
        const float master = masterGain_.next() * (preFader && !faderAtUnity ? masterFader_.next() : 1.0f);
        outL_[static_cast<size_t>(i)] = l * master;
        outR_[static_cast<size_t>(i)] = r * master;
        if (direct) {
            float d = direct[offset + i];
            if (!std::isfinite(d)) {
                d = 0.0f;
                directBlewUp = true;
            }
            click_[static_cast<size_t>(i)] = d;
        }
    }
    MasterChain::Stats stats;
    if (chainBypass_) {
        for (int i = 0; i < n; ++i) {
            const float d = direct ? click_[static_cast<size_t>(i)] : 0.0f;
            outL_[static_cast<size_t>(i)] += d;
            outR_[static_cast<size_t>(i)] += d;
            stats.inPeak = std::max(stats.inPeak, std::max(std::fabs(outL_[static_cast<size_t>(i)]), std::fabs(outR_[static_cast<size_t>(i)])));
        }
    } else {
        stats = chain_->process(outL_.data(), outR_.data(), direct ? click_.data() : nullptr, n);
    }
    float masterPeak = 0.0f;
    for (int i = 0; i < n; ++i) {
        const float fader = faderAtUnity || preFader ? 1.0f : masterFader_.next();
        const float l = outL_[static_cast<size_t>(i)] * fader, r = outR_[static_cast<size_t>(i)] * fader;
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
    storeMax(masterInPeak_, stats.inPeak);
    if (directBlewUp) blewUp_.store(true, std::memory_order_relaxed);
    limiterGainOut_.store(stats.minGain, std::memory_order_relaxed);
    storeMin(limiterMin_, stats.minGain);
}

float Mixer::blockPeak(const float* l, const float* r, int n) {
    float peak = 0.0f;
    for (int i = 0; i < n; ++i) peak = std::max(peak, std::max(std::fabs(l[i]), std::fabs(r[i])));
    return peak;
}

// --- The Live page's switches --------------------------------------------------------------

void Mixer::setPerformance(int param, float v) {
    if (!std::isfinite(v)) return;
    const bool on = v > 0.5f;
    switch (param) {
        case PerformanceFx::kReverbReturn:
        case PerformanceFx::kEchoReturn: setBusReturn(param - PerformanceFx::kReverbReturn, 2.0f * std::clamp(v, 0.0f, 1.0f)); return;
        case PerformanceFx::ReverbFreeze:
            freezeWant_ = on;
            if (!lineWaits()) reverb_.setFreeze(on);
            return;
        case PerformanceFx::ThrowReverb:
        case PerformanceFx::ThrowEcho: throwEdge(kMaster, param - PerformanceFx::ThrowReverb, on); return;
        case PerformanceFx::Drop:
            // The buses' part of the drop, on the same bar line as the master's effects.
            if (on && !dropWant_) {
                if (clock_) dropPending_ = true;
                else liveReset();
            }
            dropWant_ = on;
            break;
        case PerformanceFx::ResetAll:
            if (on && !resetWant_) liveReset();
            resetWant_ = on;
            break;
        default: break;
    }
    perf_.setParam(param, v);
    if (param == PerformanceFx::BuildUp) updateMasterSends();
}

void Mixer::setClockRunning(bool on) {
    if (on == clock_) return;
    clock_ = on;
    perf_.setClock(on);
    // ▶: the integrated loudness and the true peak count from here.
    if (on) chain_->meter().reset();
    if (on) return;
    // Stopped: what waited for a line happens now; a throw counting steps ends.
    reverb_.setFreeze(freezeWant_);
    for (size_t ch = 0; ch < killWant_.size(); ++ch) {
        for (int b = 0; b < Isolator::kBands; ++b) (*isolators_)[ch].setKill(b, killWant_[ch][static_cast<size_t>(b)]);
    }
    for (int ch = 0; ch < kChannels; ++ch) {
        for (int b = 0; b < kBuses; ++b) {
            auto& t = throws_[static_cast<size_t>(ch)][static_cast<size_t>(b)];
            if (t.pending) startThrow(ch, b);
            else if (t.on && t.endStep >= 0) t.on = false;
        }
    }
    if (dropPending_) liveReset();
    updateTargets();
    updateMasterSends();
}

void Mixer::throwEdge(int ch, int bus, bool on) {
    if (ch < 0 || ch >= kChannels || bus < 0 || bus >= kBuses) return;
    auto& t = throws_[static_cast<size_t>(ch)][static_cast<size_t>(bus)];
    // A press: the next beat line (at once without the grid or a transport).
    if (on && !t.want) {
        if (lineWaits()) {
            t.pending = true;
            anyThrow_ = true;
        } else {
            startThrow(ch, bus);
        }
    }
    t.want = on;
}

void Mixer::startThrow(int ch, int bus) {
    auto& t = throws_[static_cast<size_t>(ch)][static_cast<size_t>(bus)];
    t.pending = false;
    t.on = true;
    // One beat: to the step a beat after this one (or a beat of frames with no transport).
    t.endStep = clock_ && lastStep_ >= 0 ? lastStep_ + 4 : -1;
    t.framesLeft = 4.0 * stepFrames_;
    anyThrow_ = true;
    updateTargets();
    updateMasterSends();
}

void Mixer::updateMasterSends() {
    for (int b = 0; b < kBuses; ++b) {
        const float t = throws_[kMaster][static_cast<size_t>(b)].on ? 1.0f : perf_.buildSend(b);
        masterSend_[static_cast<size_t>(b)].setTarget(t);
    }
}

void Mixer::liveLine(int step, int stepsPerBar) {
    if (step < 0) return;
    lastStep_ = step;
    const int grid = perf_.gridSteps();
    const bool onGrid = grid <= 0 || step % grid == 0;
    if (dropPending_ && stepsPerBar > 0 && step % stepsPerBar == 0) liveReset();
    if (onGrid && reverb_.frozen() != freezeWant_) reverb_.setFreeze(freezeWant_);
    if (onGrid) {
        for (size_t ch = 0; ch < killWant_.size(); ++ch) {
            auto& iso = (*isolators_)[ch];
            for (int b = 0; b < Isolator::kBands; ++b) {
                if (iso.kill(b) != killWant_[ch][static_cast<size_t>(b)]) iso.setKill(b, killWant_[ch][static_cast<size_t>(b)]);
            }
        }
    }
    if (!anyThrow_) return;
    bool any = false, changed = false;
    for (int ch = 0; ch < kChannels; ++ch) {
        for (int b = 0; b < kBuses; ++b) {
            auto& t = throws_[static_cast<size_t>(ch)][static_cast<size_t>(b)];
            if (t.on && t.endStep >= 0 && step >= t.endStep) {
                t.on = false;
                changed = true;
            }
            // Throws wait for a beat line.
            if (t.pending && step % 4 == 0) {
                t.pending = false;
                t.on = true;
                t.endStep = step + 4;
                t.framesLeft = 4.0 * stepFrames_;
                changed = true;
            }
            any |= t.on || t.pending;
        }
    }
    anyThrow_ = any;
    if (changed) {
        updateTargets();
        updateMasterSends();
    }
}

void Mixer::liveReset() {
    // « Tout remettre » (or the drop on its bar line): the freeze, the throws, the strips' kills and
    // the returns back at rest; the master's effects reset themselves (PerformanceFx).
    dropPending_ = false;
    freezeWant_ = false;
    reverb_.setFreeze(false);
    for (auto& strip : throws_) {
        for (auto& t : strip) t = Throw{t.want, false, false, -1, 0.0};
    }
    anyThrow_ = false;
    for (size_t ch = 0; ch < killWant_.size(); ++ch) {
        killWant_[ch].fill(false);
        for (int b = 0; b < Isolator::kBands; ++b) (*isolators_)[ch].setKill(b, false);
    }
    for (int b = 0; b < kBuses; ++b) setBusReturn(b, 1.0f);
    perf_.setParam(PerformanceFx::BuildUp, 0.0f);  // its sends close with the rest (the drop's bar line)
    updateTargets();
    updateMasterSends();
}

}  // namespace ps
