# Special effects and performance FX: research for Pocket Studio

Date: 2026-10-21. Read-only inventory of `/home/user/ps-p22` (branch with phase 27, Live mode) plus a web scan
of comparable products. Cost scale: **S** about a day (under ~150 lines of DSP plus UI), **M** 2 to 4 days,
**L** more than a week. "RT-safe" means it fits the audio-thread rules: no allocation, no lock, state
prepared up front, timing from the sequencer's `onStep(offset)`.

## 1. What we already have

**Insert effects (27 types, 4 params each, max 4 inserts per channel)**, `EffectType` in
`project-model/.../Effects.kt`, engine `InsertSlot` in `engine/src/main/cpp/core/Effects.h`:
EQ 3-band, parametric EQ, compressor, saturation, distortion (overdrive/fuzz/hard clip/foldback),
bitcrusher, vinyl, radio ("Talkie-walkie"), chorus, flanger, phaser, static filter (LP/BP/HP),
tremolo (free Hz, not synced), auto-pan (tempo-synced), wah (envelope), robot (ring mod), frequency
shifter, widener, noise gate, pump (sidechain on kick or every beat), chopper (8 one-beat patterns,
1/16 or 1/32, synced), OTT, echo and ping-pong (note values incl. dotted), insert reverb (Freeverb),
tape stop (automatable trigger). Long effects borrow units from a pool.

**Send buses:** reverb (Freeverb) and tempo delay, with per-channel sends and Live "return" sliders.

**Macros:** 4 per track (`TrackMacros`): Colour (dark LP .. neutral .. thin HP), Dirt, Motion
(chorus), Space (adds to sends). Track XY pad = Colour x a macro (`QuickFx.XyPad`), 4 macro knobs.

**FX presets:** 28 "ambiances" (`FxPresets.kt`) in Ambiance / Couleur / Saleté / Mouvement / Rythme
groups, with "Pour : Basse" suggestions by sound kind. Named after sounds (Sous l'eau, Dub profond,
Pompe EDM, Lead qui crie, Grondement...).

**Live page (`app/.../live/`, docs/LIVE.md):** clip grid per track and scene, quantised launch
(1/2/4 bars), ● record performance into the song (gestures become per-beat automation, 16 lanes).
Effects panel: target chips "Tout le mix" or one track.
- Whole mix (`PerformanceFx` on the master): DJ filter XY pad (LP left / HP right, Y = resonance,
  springs back), hold buttons Répéter (fixed 1/8), Freinage (tape stop, fixed 0.7 s), Hachoir
  (fixed: first half of every 1/16), reverb and echo return sliders.
- One track: XY Colour x macro, 4 macro knobs (56 dp), held "Hachoir de voie", 2 send sliders,
  "Effets..." opens the effects box.
- 303 panel: XY cutoff x resonance (no spring, double tap = saved value), 4 big sliders.

**Visuals available:** `analysis/Spectrum.kt` (40 log bands), sample waveform view, level meters.

**Observed limits in the code (not bugs, but shape the ideas below):**
1. Beat repeat starts *the instant* the button is pressed and loops the slice that *ends* there
   (`Effects.cpp` ~l.505): it is not aligned to the grid, so a roll pressed slightly late loops
   an off-beat fragment. Pioneer's Roll and Logic's Repeater lock to note divisions.
2. Live repeat division is fixed at 1/8 (`LiveFx.kt` l.73); the engine supports 1/4..1/32.
3. Tape stop is fixed 0.7 s with no restart ramp; no backspin, no reverse.
4. No tempo-synced filter LFO anywhere: the "Ondulation" (wobble) preset is a left-right tremolo,
   and the insert tremolo runs in Hz. The dubstep "wob" and DnB reese movement are missing.
5. Nothing makes a build-up by itself (no noise riser, no rising repeat, no one-knob "montée").
6. No freeze (reverb or echo), no pitch effect, no isolator kills, no reset/panic button.

## 2. What the market does (short scan)

| Product | Performance-FX ideas worth noting | Source |
|---|---|---|
| Pioneer DJM-900NXS2 / A9 | 14 Beat FX on an X-Pad, tempo-synced: Delay, Echo, Ping Pong, Spiral, Reverb, Trans, Filter, Flanger, Phaser, Pitch, Slip Roll, Roll, Vinyl Brake, Helix; A9 adds Mobius (endless rising/falling tone), Triplet Filter, Triplet Roll. FX Frequency picks low/mid/high band. | [1], [2] |
| Pioneer Sound Color FX | One knob per channel, **off at 12 o'clock**, two flavours left/right: Space, Dub Echo, Sweep, Noise, Crush, Filter. | [1], [3] |
| rekordbox Pad FX / Release FX | Pad FX: effect on *only while the pad is held*. Release FX: leave an FX with vinyl brake, echo or back spin. | [4], [5] |
| Pioneer Smart CFX (DDJ-FLX4) | One knob that drives a preset *combination* of effects for build-ups and drops (8 combos in rekordbox). | [6] |
| Traktor Macro FX | Bundles fired by one button: Event Horizon (filter + echo, last 2-4 beats of a phrase), Laser Slicer (beatmasher + flanger), Zzzurp, Strrretch (loop sampler for build-ups). Mixer FX: one knob per channel like Sound Color FX. | [7], [8] |
| Logic Remix FX (Logic Remote / Logic iPad) | XY pads: Filter, Wobble (rate/depth), Orbit, Repeater (rate/mix), Reverb, Delay. Buttons: Gater, Downsampler, Reverse, Scratch, Tape Stop, each with a note division. **FX Lock** keeps an XY value after lifting, **Reset** clears all. | [9] |
| BandLab Looper (Android) | Filter pad (cutoff/resonance), Gater pad (depth/rate), Stutter held on division buttons, Tape Stop. | [10] |
| Remixlive | XY FX page per master or channel; six Beat Repeat pads 1/16, 1/8, 1/4, 1/2, 1 beat, 2 beats. | [11] |
| Koala Sampler / Koala FX | 16 "DJ mix FX" incl. Stutter, Reverse, Cutter, Gate, Dub, Comb, Bitcrusher, Pitch-shift, Talkbox; stutter/reverse snap to tempo; Koala FX crossfades saved scenes on an 8-way XY. | [12], [13] |
| Maschine Perform FX | One perform FX per group, played from a touch strip: Filter, Flanger, Burst Echo, Reso Echo, Ring, Stutter, Tremolo, Scratcher. | [14], [15] |
| iMaschine 2 | Twin XY effect pads on an otherwise simple mixer. | [15] |
| OP-Z punch-in FX | Hold a key: Duck, Filter sweep, Loop 1/2, Stereo, Pitch, Echo, Ramp up/down (fills), Random. Low octave = this track, high octave = its group (drums or synths). Recorded to a performance track, editable. | [16] |
| Elektron Octatrack / Syntakt | Crossfader morphs between two scenes (16 stored), e.g. different master FX per scene. FILL trigs as held variations; Syntakt FX track with its own sequencer. | [17], [18] |
| Ableton Live | Beat Repeat: Interval, Offset, Gate, Chance, **Pitch Decay** (each repeat lower), Mix/Insert/Gate modes. Auto Filter with synced LFO and envelope follower. Ableton Note: note repeat incl. triplets. | [19], [20] |
| FL Studio Mobile | Tape Stop with Start/Stop time in beats (Sync), Warp curve, triggered by automating Play. | [21] |
| Gross Beat (FL Studio) | 2-bar buffer; 36 time + 36 volume envelope slots (half-speed, scratch, gates), switched like presets. | [22] |
| Sugar Bytes Turnado / Effectrix | Turnado: 8 knobs, *turn up = effect starts, further = more, back to 0 = off*; freeze reverb, granulizer, vowel filter. Effectrix: step grid of effects. | [23] |
| iZotope Stutter Edit 2 | One key fires a timed "gesture" (stutter + sweeping FX) to build tension. | [24] |
| Cableguys ShaperBox | Draw an LFO curve per effect (volume = trance gate/pump, filter = wobble, time = tape stop/reverse). | [25] |
| Korg Kaoss Pad KP3+ | 150 XY programs: filter, LFO, delay, reverb, grain shifter, looper, vocoder. | [26] |
| Novation Launchpad app | Performance FX: Stutter, Gater, Autofilter (with LFO), Delay. | [27] |
| djay Pro | Instant FX on pads (Echo Out, Chop, Loop), FX applied per stem with Neural Mix. | [28] |
| NI Thrill / Rise & Hit | XY morph from dark to screaming for tension; dedicated riser + impact instrument. | [29] |

## 3a. Effects we lack

| # | Effect | Who does it | Why for Benjamin | Engine cost / RT | Where |
|---|---|---|---|---|---|
| E1 | **Wobble filter** (LP/BP filter + tempo-synced LFO 1 bar..1/32, triplets, sine/saw/square, depth) | Logic Wobble [9], Ableton Auto Filter [19], ShaperBox [25] | The dubstep wob, DnB reese motion, acid "breathing". Our "Ondulation" is only volume. | S: `SweepFilter` + phase reset on beat like `AutoPan::beat()`. RT-safe. | Insert type + Live XY (rate x depth) |
| E2 | **Grid-locked roll** with divisions 1/4..1/32 + triplets, optional pitch decay | Pioneer Roll/Triplet Roll [1][2], Ableton Pitch Decay [19], Remixlive [11] | DnB/dubstep fills before the drop; must be on beat even with clumsy fingers. | S: start the loop at the next step boundary from `onStep`; pitch decay = read speed < 1 per repeat (ring buffer exists). | Live (whole mix + track) |
| E3 | **Echo out / echo freeze** (input fades, feedback held near 100 %, tail dies over N beats) | rekordbox Release FX [5], djay Echo Out [28], Pioneer Echo [1] | Clean exit before a break; classic dub techno gesture. | S: momentary input mute + feedback override on the send delay. | Live hold + "release" choice |
| E4 | **Reverb freeze / throw** | Turnado freeze reverb [23], Pioneer Space [3] | Benjamin "s'éclate avec la réverbe en Play": a frozen pad of sound under a drop is the next step. | S: Freeverb freeze mode (combs feedback 1, damping 0, input 0). | Live button on the reverb return |
| E5 | **Noise riser / downlifter** (white noise through a rising band-pass/HP over 1, 2, 4, 8 bars) | Pioneer Noise [3], Rise & Hit [29] | Every techno/EDM build. Today Benjamin must find a sample. | S: DaisySP `WhiteNoise` + `Svf`, ramp driven by beat count. | Live "Montée" + insert/generator |
| E6 | **Backspin, reverse, vinyl brake length** (1/4 beat .. 2 bars, restart ramp) | Logic Reverse/Scratch/Tape Stop [9], rekordbox [5], FL Mobile Tape Stop [21] | DJ transitions, dubstep rewinds ("pull up!"). | S-M: read the existing 4 s ring backwards / variable speed. | Live hold buttons |
| E7 | **Half-time** (playback at half speed, resync every beat or bar) | Gross Beat [22], ShaperBox time [25] | The half-time drop is a DnB and dubstep signature. | M: ring-buffer read at 0.5x with crossfaded resync; 1 bar at 70 BPM fits in 4 s. | Live hold |
| E8 | **Isolator kills** (cut lows / mids / highs, 3 buttons) | DJM FX Frequency [1], Koala isolator [12] | "Bass out, bass in" on the drop: the most effective techno gesture. | S: Linkwitz-Riley 3-band split, smoothed gains. | Live (whole mix + track) |
| E9 | **Pitch shifter / pitch riser-dive** (+/-12 st, glide) | Pioneer Pitch [1], Koala Pitch-shift [12], OP-Z Pitch [16] | Risers that climb, dives on the drop, octave-down vocals. | M: DaisySP `PitchShifter` (fixed delay lines, RT-safe), grainy but fine for FX. | Insert + Live hold |
| E10 | **Mobius / Shepard rise** (endless rising tone) | DJM-A9 Mobius [2] | Hypnotic tension in techno. | M: two-oscillator Shepard sweep ring-mod or added tone. | Live, later |
| E11 | **Comb / resonator** (tuned metallic ring) | Koala Comb [12], Ableton Corpus [19] | Metallic techno percussion, dubstep growl colour. | S: short feedback delay tuned to a note. | Insert |
| E12 | **Granular freeze / stutter grain** | Kaoss grain shifter [26], Turnado granulizer [23] | Glitchy textures, IDM breaks. | M: grains from the ring buffer, fixed voice count. | Live XY, later |
| E13 | **Step FX lane** (16 steps, each step turns an effect on: repeat, filter, reverse, gate) | Effectrix [23], Gross Beat slots [22] | Programmed glitch fills, very "FL". | M-L: per-step flags read in `onStep`; UI is the cost. | Effects box, later |
| E14 | **Shimmer / Spiral / Helix** (delay or reverb whose feedback climbs in pitch) | Pioneer Spiral, Helix [1] | Big break ambiences. | M (needs E9). | Later |

## 3b. Live page UX ideas

| # | Idea | Who | Why for Benjamin | Cost | Notes |
|---|---|---|---|---|---|
| L1 | **Quantised FX**: every hold FX starts on the next 1/16 (or beat) and, optionally, releases on the beat | Pioneer Beat FX, Logic note divisions [1][9], Koala snap [12] | Fingers on glass are late; snapping makes him sound tight. | S | Pending flags consumed in `PerformanceFx::onStep`. |
| L2 | **Roll pad row**: 1/4, 1/8, 1/16, 1/32, 1/8T, 1/16T as six held pads (slide between them) | Remixlive [11], BandLab [10], Triplet Roll [2] | Snare rolls for DnB, triplet rolls for dubstep. | S (UI) | Division already in engine. |
| L3 | **One-knob "Montée" (build-up) + "Drop" button**: slider 0..1 raises HP filter, reverb/echo sends, noise riser, roll speed (1/4 to 1/32) and a little pitch; "Drop" resets all on the next bar | Smart CFX [6], Traktor Macro FX [7], Stutter Edit [24], Thrill [29] | One gesture for the most common EDM/techno moment. | M | Mostly mapping existing params + E5. Recorded as one automation lane. |
| L4 | **Turnado-style knobs**: 0 = off, turning up starts the effect and increases it | Turnado [23], Sound Color FX [3] | No separate on/off; one finger, one effect. | S (UI) | Fits ProSlider; Live track macros already behave close to this. |
| L5 | **FX Lock + "Tout remettre" (panic)**: lock keeps XY value after lifting; panic resets all live FX, macros and returns to saved values with a 10 ms fade | Logic FX Lock / Reset [9] | Safe live: one button to get out of chaos. | S | Must also be recordable into a take (writes neutral values). |
| L6 | **Release FX choice**: when a held FX is let go, end with Echo out, Brake or Backspin | rekordbox Release FX [5] | Turns a clumsy release into a transition. | S-M | Needs E3/E6. |
| L7 | **XY pad modes by genre**: Filtre DJ, Acide (cutoff/reso), Wobble (rate/depth), Roll (division/mix), Espace (reverb size/echo feedback), Bitcrush (bits/rate) | Logic XY effects [9], Kaoss Pad [26], Remixlive [11] | One big pad, many sounds; presets named by style. | S | X/Y axis labels must stay in the corners (defect 5 fix). |
| L8 | **Scene crossfader A/B**: store two FX snapshots, one big slider morphs between them | Octatrack [17], Koala FX scenes [13] | Long smooth transitions with one thumb (techno). | M | Interpolate in engine to avoid command floods; record as a lane. |
| L9 | **Scope per gesture**: this track / drums / everything except drums / whole mix | OP-Z track vs group [16], djay per stem [28] | "Filter everything but the kick" is the core techno move. | M-L | Needs a drums vs rest submix on the master path. Start with "Tout sauf la batterie". |
| L10 | **Dub throws**: tap a track's "Lancer réverbe/écho" to send one beat into the bus | Burst Echo [14], Sound Color Dub Echo [3] | Dub techno and DnB vocal throws. | S | Momentary send spike, quantised to the beat. |
| L11 | **Duck / Fill hold**: hold to mute the kick (or swap in a fill pattern) until release | OP-Z Duck/Fill [16], Elektron FILL [18] | Classic drop tension: kick out for a bar. | S (duck), M (fill) | Duck = track mute quantised; fill later. |
| L12 | **Beat ring and glow**: active FX glows, a ring counts down to the quantised start/stop; haptic tick per beat while held | Pioneer OLED feedback [2], DESIGN.md rules | He sees and feels the timing without reading. | S | Use transportStep already exposed. |
| L13 | **Multi-touch XY** (second finger = a second effect, e.g. filter + roll) | Kaoss Pad [26] | Two-handed play on the Tab A11. | M | Tablet layout only at first. |

## 3c. Rest of the app (effects box, presets, visuals)

| # | Idea | Who | Why | Cost |
|---|---|---|---|---|
| D1 | **Live mini-display per effect** in the effects box: filter/EQ curve, gate pattern, echo taps, wobble LFO line, moving with the sound | ShaperBox curves [25], Pioneer OLED [2] | Understands what a knob does without words. | M (UI), S per effect |
| D2 | **Spectrum behind XY pads and EQ** (reuse `Spectrum.kt`) | Kaoss / Logic visual pads | Sees the filter "close" the sound. | S-M |
| D3 | **Audition presets by holding** (finger down = hear, lift = back) and favourites star | Koala simplicity, Remixlive FX dropdown [11] | Fast browsing, no undo needed. | S |
| D4 | **Style tags on presets** (Acid, Techno, DnB, Dubstep, Lo-fi) next to "Pour : Basse"; add genre presets: "Wob dubstep 1/4", "Reese DnB", "Acid qui respire", "Techno dub throw", "Riser 8 mesures" | Pioneer/Traktor named combos [7] | Starts from his genres. | S |
| D5 | **Activity LED per slot** (lights when the slot changes the sound) and bypass glow | Pioneer FX on-indicators [2] | Shows which effect is acting. | S |
| D6 | **A/B compare** in the effects box | Common in DAWs | Decide by ear, quickly. | S |
| D7 | **"Surprends-moi"**: random but musical preset or XY position, within safe ranges | OP-Z Random [16] | Fun discovery for a beginner. | S |
| D8 | **Effect family colours** consistent between chips, slots, Live buttons and automation lanes | DESIGN.md (FL Mobile clear) | Recognise an effect by colour. | S |

## 4. Top 15 (impact for Benjamin vs cost)

| Rank | Item | Impact | Cost | Where |
|---|---|---|---|---|
| 1 | L1 + E2: quantised hold FX and grid-locked roll (fixes the off-beat repeat) | High | S | Live |
| 2 | L2: roll pad row 1/4..1/32 + triplets | High | S | Live |
| 3 | E1: wobble filter (insert + Live XY mode) | High (dubstep/DnB) | S | Effects box + Live |
| 4 | L5: FX Lock + "Tout remettre" panic | High (safety) | S | Live |
| 5 | E8: isolator kills (bass/mid/high) | High (techno drop) | S | Live |
| 6 | E3 + L6: echo out and release FX choice | High | S-M | Live |
| 7 | E5: noise riser / downlifter | High | S | Live + generator |
| 8 | L3: one-knob "Montée" + "Drop" on the bar | Very high | M (after 1, 5, 7) | Live |
| 9 | L7: XY pad modes by genre (filter, acid, wobble, roll, space, crush) | High | S | Live |
| 10 | E4 + L10: reverb freeze and dub throws | High (his favourite) | S | Live |
| 11 | E6: backspin, reverse, brake length + restart | Medium-high | S-M | Live |
| 12 | E7: half-time hold | Medium-high (DnB/dubstep) | M | Live |
| 13 | L12 + D5: beat ring, glow, haptic tick, activity LEDs | Medium-high | S | Live + box |
| 14 | D1 + D2: mini-displays and spectrum behind pads | Medium | M | Effects box + Live |
| 15 | D3 + D4: hold-to-audition, favourites, genre tags and genre presets | Medium | S | Effects box |

Next after the top 15: L8 scene crossfader, L9 "everything except drums", E9 pitch shifter,
E13 step FX lane, L11 duck/fill.

Suggested grouping into one phase: **Step A** (1, 2, 4, 5, 9, 13: engine timing + Live UI),
**Step B** (3, 6, 7, 10, 11: new DSP), **Step C** (8 build-up macro on top of A and B, then 12),
**Step D** (14, 15: effects box polish). Every new Live control must go through the ViewModel so
● records it (kinds 6/7 lanes already exist; new on/off targets need `LIVE_ON_OFF` entries).

## 5. Do not do (for now)

- **Vocoder / talkbox**: needs a carrier and voice input; L cost, little use in his genres.
- **Convolution reverb with impulse libraries**: CPU and APK size; Freeverb + freeze covers live use.
- **Modular FX grid (Bitwig Grid style) or plugin hosting (AUv3/VST)**: against "facile à prendre en main".
- **Effectrix-size step grids (32 x 14) on a phone**: unreadable; if step FX come, one 16-step row per effect.
- **Per-stem live FX on the Remix track (djay Neural Mix)**: separation is offline; not real-time here.
- **Jog-wheel scratch physics**: a held "Scratch"/"Backspin" button gives 90 % of the fun.
- **Random "chance" on FX by default** (Ableton Chance): confusing for a beginner; keep it out or under "Avancé".
- **Any timing from Kotlin timers or animations**: quantisation and ramps live in the engine (`onStep`).
- **More than 4 parameters per effect or deep menus**: keep 4 params and the "Avancé" pattern.
- **Paid or locked FX packs** (Remixlive model): everything included.

## 6. Engine notes (fit with the audio-thread rules)

- `PerformanceFx` already owns a 4 s stereo ring, the step size and `onStep(offset)`: grid-locked
  roll, backspin, reverse, half-time and brake length are new read-head modes on that ring, no new memory.
- Quantised start/stop: store "pending on/off" flags set by commands, apply them at the next step
  boundary inside `process()` (same pattern as `Chopper::beat`).
- Wobble: phase reset on beat like `AutoPan`; LFO computed per sample, smoothed cutoff (no zipper).
- Isolator: 2 crossovers (LR4) per channel on master and per-track insert; gain ramps ~5 ms.
- Riser: DaisySP `WhiteNoise` + `Svf` (MIT, already vendored); length in beats from step frames.
- Pitch: DaisySP `pitchshifter.h` (fixed buffers, template, RT-safe); Signalsmith Stretch stays offline.
- Freeze: Freeverb's classic freeze mode (feedback 1, damping 0, input muted) on the reverb bus.
- Panic: one command that sets all live params to neutral with the existing 10 ms fades.

## Sources

1. Pioneer DJM-900NXS2 spec sheets (Beat FX, Sound Colour FX, FX Frequency): https://www.musicmatter.co.uk/pioneer-djm-900nxs2 , https://www.novelty.fr/produits/sonorisation/mixage/dj/djm900-nexus-2/
2. Pioneer DJM-A9 (Mobius, Triplet Filter, Triplet Roll): https://www.idjnow.com/pioneer-dj-djm-a9-mixer.html , https://ror.ie/products/pioneer-dj-djm-a9.html
3. Sound Color FX behaviour: https://forum.image-line.com/viewtopic.php?p=2050538 , https://www.mixgraph.io/glossary/color-fx , https://virtualdj.com/manuals/hardware/pioneer/djm850/layout/colorfx.html
4. rekordbox 7.2.8 pad editor guide (Pad FX on while held): https://cdn.rekordbox.com/files/20251202174704/rekordbox7.2.8_pad_editor_operation_guide_EN.pdf
5. rekordbox Release FX (vinyl brake, echo, back spin): https://www.pioneerdj.com/ro/news/2015/rekordbox-dj/
6. Smart CFX: https://wearecrossfader.co.uk/blog/pioneer-dj-ddj-flx4-review/
7. Traktor Macro FX descriptions: https://forum.djtechtools.com/t/describing-the-basic-effects-that-form-pio-colorfx-tp3-mixerfx-and-tp2-macrofx/78178 , https://forum.djtechtools.com/t/how-to-use-the-traktor-macro-efx-effects/70879
8. Traktor Pro 3 Mixer FX: https://www.native-instruments.com/en/products/traktor/dj-software/traktor-dj-2/compare/
9. Apple, Logic Remote Remix FX: https://support.apple.com/guide/logicremote-logicpro-iphone/chsf6725a0be/ios
10. BandLab Looper help: https://help.bandlab.com/hc/en-us/articles/115004496573-Using-the-Looper
11. Remixlive reviews: https://www.digitaldjtips.com/reviews/remixlive-1-2/ , https://www.gearnews.com/mixvibes-introduces-remixlive-windows
12. Koala Sampler App Store listing: https://apps.apple.com/us/app/koala-sampler-beat-maker/id1449584007
13. Koala FX: https://synthanatomy.com/2020/08/koala-fx-is-a-new-creative-touch-multi-fx-processor-for-ios.html
14. Maschine+ manual, Performance effects: https://www.native-instruments.com/ni-tech-manuals/maschine-plus-manual/en/using-performance-effects
15. iMaschine 2 review: https://www.soundonsound.com/reviews/native-instruments-imaschine-2
16. OP-Z punch-in effects guide: https://teenage.engineering/guides/op-z/punch-in-effects
17. Octatrack crossfader scenes: https://sonicstate.com/news/2010/11/01/-more-octatrack-features/ , https://www.elektronauts.com/t/your-favorite-x-fader-scene-tricks/961
18. Syntakt FX track and FILL use: https://www.perfectcircuit.com/m2e/elektron-syntakt.html , https://musictech.com/reviews/hardware-instruments/elektron-syntakt-review/
19. Ableton Live audio effect reference: https://www.ableton.com/en/manual/live-audio-effect-reference/
20. Ableton Note: https://www.soundonsound.com/reviews/ableton-note
21. FL Studio Mobile Tape Stop: https://www.image-line.com/fl-studio-learning/fl-studio-mobile-online-manual/html/plugins/FL%20Studio%20Mobile_Module_Tape_Stop.htm
22. Gross Beat manual: https://www.image-line.com/fl-studio-learning-content/fl-studio-online-manual/html/plugins/Gross%20Beat.htm
23. Turnado / Effectrix: https://www.soundonsound.com/reviews/sugar-bytes-plug-ins , https://www.pluginboutique.com/articles/190-Sugar-Bytes-Turnado-Review-at-Resident-Advisor
24. Stutter Edit 2: https://www.izotope.com/products/stutter-edit , https://www.synthtopia.com/content/2020/06/24/stutter-edit-2-now-available-heres-whats-new/
25. ShaperBox 3 manual: https://www.cableguys.com/downloads/Cableguys-ShaperBox-3-Manual.pdf?v=3.0
26. Korg KP3+: https://www.korg.com/uk/products/dj/kaoss_pad_kp3_plus/page_2.php
27. Novation Launchpad Performance FX: https://www.sonicstate.com/news/2015/03/12/novation-launchpad-for-ipad-update/
28. djay effects and Neural Mix: https://help.algoriddim.com/user-manual/djay-ios/dj-tools/effects/overview , https://community.algoriddim.com/t/expose-harmonics-bass-neural-mix-instant-fx/42896
29. NI Thrill / Rise & Hit: https://ra.co/reviews/21217 , https://www.soundonsound.com/node/4917447

Caveats: several lists (Traktor Macro FX, Sound Color FX details, Koala performance behaviour)
come from community or retailer pages, not manuals; Zenbeats, Korg Gadget and Groovepad gave no
documented performance-FX design worth copying (generic filter/flanger/reverb/delay lists).
