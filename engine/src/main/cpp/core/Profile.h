#pragma once

// Where the audio callback's time goes, brick by brick (tour 9, B1: which brick costs the most on a
// small tablet). A measuring build only: without PS_PROFILE (the app, the tests, the benches) the
// macro below is empty and nothing of this file is compiled, so neither the sound nor the audio
// thread changes. With it (engine/bench, -DPS_PROFILE=ON, one engine on one thread), each scope
// gives its time to its brick and takes it from the brick around it: the bricks add up to the
// whole callback. scripts/cpu-bench.sh --bricks builds the table.
#ifdef PS_PROFILE

#include <chrono>
#include <cstdint>

namespace ps::profile {

enum Brick : int {
    Rest = 0,  // the engine around the bricks: sequencer, notes, buffers, the load meter
    Drums, PadSampler, Acid, Synth, SoundFont, Plaits, SampleInstrument, MultiSampler, Bass808,
    AudioClips, Mixer, ReverbBus, DelayBus, LiveFx, MasterChain,
    FirstInsert,  // + InsertSlot::Type (30 of them)
    kBricks = FirstInsert + 30
};

inline const char* name(int brick) {
    static const char* const kNames[kBricks] = {
        "engine-rest", "drums", "pad-sampler", "acid-303", "synth", "soundfont", "plaits", "sample-instrument",
        "multi-sampler", "bass-808", "audio-clips", "mixer-strips", "reverb-bus", "delay-bus", "live-fx", "master-chain",
        "fx-none", "fx-eq", "fx-compressor", "fx-saturation", "fx-chorus", "fx-filter", "fx-pump", "fx-flanger", "fx-phaser",
        "fx-gate", "fx-tremolo", "fx-widener", "fx-crusher", "fx-param-eq", "fx-robot", "fx-wah", "fx-vinyl", "fx-radio",
        "fx-chopper", "fx-distortion", "fx-echo", "fx-ping-pong", "fx-reverb", "fx-auto-pan", "fx-ott", "fx-freq-shift",
        "fx-tape-stop", "fx-wobble", "fx-clipper", "fx-bass-exciter"};
    return brick >= 0 && brick < kBricks ? kNames[brick] : "?";
}

struct State {
    int64_t ns[kBricks] = {};
    int64_t last = 0;
    int current = -1;  // -1: outside the callback, time is not counted
};
inline State g_state;

inline int64_t now() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::steady_clock::now().time_since_epoch()).count();
}

class Scope {
public:
    explicit Scope(int brick) : before_(g_state.current) {
        const int64_t t = now();
        if (before_ >= 0) g_state.ns[before_] += t - g_state.last;
        g_state.last = t;
        g_state.current = brick;
    }
    ~Scope() {
        const int64_t t = now();
        g_state.ns[g_state.current] += t - g_state.last;
        g_state.last = t;
        g_state.current = before_;
    }
    Scope(const Scope&) = delete;
    Scope& operator=(const Scope&) = delete;

private:
    int before_;
};

}  // namespace ps::profile

#define PS_PROFILE_JOIN2(a, b) a##b
#define PS_PROFILE_JOIN(a, b) PS_PROFILE_JOIN2(a, b)
#define PS_PROFILE_SCOPE(brick) ::ps::profile::Scope PS_PROFILE_JOIN(psProfileScope, __LINE__)(::ps::profile::brick)
#define PS_PROFILE_SCOPE_AT(index) ::ps::profile::Scope PS_PROFILE_JOIN(psProfileScope, __LINE__)(index)

#else

#define PS_PROFILE_SCOPE(brick)
#define PS_PROFILE_SCOPE_AT(index)

#endif
