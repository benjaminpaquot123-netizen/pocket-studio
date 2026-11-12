#pragma once

#include "Effects.h"

namespace ps {

// « Le kick au haut-parleur » (tour 5, lot kick; docs/decisions/tour-5-kick.md). Benjamin, on his
// phone's own speaker: « ça tape mais pas assez, surtout le kick »; through Bluetooth it is fine. A
// phone's speaker plays almost nothing under 200-300 Hz, where a kick puts its weight (45-65 Hz).
// The ear hears a low note that is not there when its harmonics are (the missing fundamental: the
// « virtual bass » of small loudspeakers, Waves' MaxxBass), and hears a kick on a small speaker by
// its click (1-4 kHz). So, ONLY while the app says the music goes to the phone's speaker
// (Mixer::setSpeakerOutput, from Android's AudioManager), on the kick's own strip (pad 1's; the
// drums' strip when that pad has none: then the whole kit's low band and click band, the kit's 808
// included), after its effects and before its fader (so its volume, mute and solo still rule; the
// basses' own strips are not touched). Never while a take of the output is armed (Engine):
// - harmonics of its low band (mono, a 4th-order Butterworth low-pass at kLowHz) divided by their
//   own envelope into a wave of steady size, shaped by tanh (odd) and tanh² (even), band-passed
//   (kHarmLowHz..kHarmTopHz), multiplied by the envelope again (they hit and fade with the kick)
//   and added to both sides;
// - its click: its own kClickLowHz..kClickTopHz band, raised (x 1 + kClick).
// The master's chain (drive, limiter) then keeps the song at its loudness and under its ceiling.
// Tried and left out: a 100 Hz high-pass of the whole mix (the band the speaker cannot play) took
// 1-2 dB off the kick through the phone model on the live bench, the harmonics on the master (keyed
// by the kick's steps) excited the bass too and lifted the kick less.
// Off (headphones, Bluetooth, an export): process() returns at once, the strip untouched sample for
// sample. On / off glides over 20 ms. Audio thread: no allocation, no lock. The shapes are smooth and
// the low band is under 150 Hz: nothing folds back at 48 kHz.
class SpeakerKick {
public:
    static constexpr float kLowHz = 150.0f;
    static constexpr float kHarmLowHz = 180.0f, kHarmTopHz = 1500.0f;
    static constexpr float kClickLowHz = 1200.0f, kClickTopHz = 4500.0f;
    // Tuned on the live bench (docs/audits/ultra/tour-5-kick-rapport.md): the drive into tanh, the
    // harmonics against the kick's low envelope, the click band's rise.
    static constexpr float kDrive = 8.0f, kHarmonics = 4.0f, kClick = 1.0f;

    void prepare(float rate);
    void reset();
    // `instant`: off at once, no glide (a take of the output starts: nothing of it may be in there).
    void setOn(bool on, bool instant = false);
    bool on() const { return on_; }
    // The kick's strip, in place (before its fader).
    void process(float* l, float* r, int n);

private:
    void clear();
    bool on_ = false;
    float level_ = 0.0f, glide_ = 0.001f;  // on / off, gliding
    Biquad lowA_, lowB_, hpA_, hpB_, top_, clickHp_, clickLp_;
    float env_ = 0.0f, envUp_ = 0.0f, envDown_ = 0.0f;
};

}  // namespace ps
