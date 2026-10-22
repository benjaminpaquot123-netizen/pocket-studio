# STYLE-FIXES: work plan to bring the 21 researched styles closer to the real genres

Inputs: research `famille-techno`, `famille-dnb-dubstep`, `famille-techhouse` (.md + .json drafts), current recipes
`/home/user/ps-p22/scripts/packs/recipes/*.json`, converter `scripts/packs/recipes_to_kotlin.py`, `StarterWizard.kt`.
Out of scope (parallel fixer "P"): converter accepting every existing preset/insert/sound (fat_bass, dub_deep, edm_pump,
wide_alive, OTT, DISTORTION, ECHO, REVERB, TAPE_STOP, Sirène, Souffle…), 303 knob automation (kind 5), 2-bar acid,
Acidcore, filter movement on acid_techno / acid_house. Items marked (P) below need P's converter change merged first.

Two fixers:
- **Fixer A (recipes)**: part 1 only, then re-applies the part-2 features to recipes once Fixer B lands them.
- **Fixer B (assistant engine)**: part 2, in the order of part 3. Branch from P's branch (same converter, same
  StyleRecipes.kt); Fixer A rebases on B before regenerating. Never hand-edit StyleRecipes.kt.

---------------------------------------------------------------------------------------------------------------
## 1. Pure recipe edits (current syntax)

The three draft JSONs already contain almost everything below, verified to pass the converter. Fastest path:
copy each style's object from the draft into its repo file, drop the `notes`/`gaps` keys, then apply the
extra lines marked "+". Coordination: `acid_techno` (techno.json) and `acid_house` (house.json) are also edited by P:
take P's version and apply only the fields listed here.
Text work: 34 new/changed French texts in the drafts (tips, bass/pad/lead tips) have no entry yet in
`project-model/src/main/resources/i18n/{en,nl}/model.json`: add EN (British) + NL (informal) for each, drop unused keys.
Tests: converter, then `./gradlew -q --max-workers=2 :project-model:test` (StyleRecipesUpToDateTest, TechnoStylesTest,
HipHopRecipesTest, catalogs).

Per style, highest musical impact first. Grids: 1 char = 1 sixteenth.

### Bass music (bass.json)
- **riddim** (6.5/10, biggest win): `bass.sound` "Basse FM" → "Basse wobble" (a static FM note never goes "wub");
  `bass.pattern` `R-------....O-..`; new line `{"role":"clap","pattern":"........X......."}` + fx clap reverb 0.25;
  ref + "Infekt – Orgalorg (2016)". + (P) bass fx `{"preset":"fat_bass","insert":"FLANGER"}` instead of SATURATION+FLANGER.
- **neurofunk** (7/10): question/answer — bass `R-.R--.R-.......R-.R--.R-...R-O-`, lead (Voix robot, oct 3)
  `..........O-.5-..........O-.....`; fx bass SATURATION + PUMP, lead FLANGER; ref + "Phace & Misanthrop – Desert Orgy (2009)".
  + (P) bass `{"preset":"fat_bass","insert":"PUMP"}` (one entry: a preset replaces the strip, so preset first, insert after).
- **dubstep** (7/10): kick 2 bars `X.........x.....X.....x...x.....`; hat `x.x.x.x...x.x.xo` (silent on 3);
  bass `R-------..R---O-R-----------2-..` (phrygian b2); `signature: "chords"` (pad from the intro);
  lead riff "Cloche" oct 5 `R.......3.....2.................` + fx lead preset dub_echo, `lead_tip`;
  refs: Skrillex – Scary Monsters and Nice Sprites (2010) first.
- **dnb** (7.5/10): snare `....X..o.o..X.....o.X..o....X.2.` (ghosts in both bars); bass
  `R-------..R-----R-------..R--/O-`, `glide: true` (reese slides to the octave); `signature: "chords"`;
  fx + snare reverb 0.15.
- **jungle** (6.5/10): bass 2-bar dub line `R-------..R---5-R-----3-..5---7-`; fx + drums preset punch,
  + bass insert PUMP. Amen grid is already right.
- **liquid_dnb** (8/10): new line `{"role":"perc","voice":"shaker","pattern":"..o...o...o..o....o...o...o...oo"}`;
  bass `R-------..R---5-R-------.5..R-3-`; fx + bass PUMP. + (P) chords `{"preset":"wide_alive","reverb":0.35}`.

### Techno family (techno.json, techno_hard.json)
- **hard_techno** (7.5/10): bass = rumble: `{"pattern":".rRr.rRr.rRr.rRr","octave":1,"sound":"Basse sub"}`,
  fx bass `{"preset":"pump","insert":"SATURATION"}` (+ (P) better: `{"preset":"fat_bass","insert":"PUMP"}`),
  chords reverb 0.2; refs + Marco Leckbert – Schranz Will Never Die (2024), Remon Verhoeve – I Wanna Feel (2024).
- **gabber** (7/10): new line `{"role":"snare","source":"tr909","pattern":"............................x234"}`
  (accelerating 909 roll), hat `o..oo..oo..oo..o`; fx snare reverb 0.3, chords reverb 0.25; replace the
  untraceable "Sperminator – No Women Allowed" by Holy Noise – James Brown Is Still Alive (1991) and
  Technohead – I Wanna Be A Hippy (1995). Stopgap until feature F2: chords `voicing` "triad" → "octave"
  (the default-on `pack:gabber:stab` is already a chord sample; a triad stacks 3 chords). Check by ear.
- **industrial_techno** (7.5/10): kick `X..oX...X..oX...` (pendulum ghosts); swing 0.10 → 0.05;
  ref "Regis – Speak To Me (1994)" (not found) → "Surgeon – Force + Form (1999)".
- **retro_techno** (7.5/10): fx lead `{"insert":"CHORUS","reverb":0.2}`, chords `{"insert":"CHORUS"}`
  (the chorus is half of the hoover), perc (zap) delay 0.2, snare reverb 0.3.
- **detroit** (8/10): bpm 130 → 126; swing 0.10 → 0.15; fx + drums insert SATURATION (MPC grain), perc reverb 0.15.
- **techno** (8/10): swing 0.05; hat `ox.oxx.oox.oxx.o`; clap `....X.......X..o`; fx + perc reverb 0.15,
  lead delay 0.25. + optional drums insert SATURATION ("drum buss"), check by ear.
- **minimal** (8.5/10): swing 0.10 → 0.15; clap `....o.......o..o`. + (P) chords preset dub_echo → dub_deep.
- **acid_techno** (7.5/10): bpm 136 → 138 (rest is P's).

### House family (house.json)
- **tech_house** (7/10, inaudible bass today): bass `{"sound":"Basse ronde","octave":2,"glide":true,
  "pattern":"..Rr..Rr..Rr..RO..Rr..Rr..Rr.5/O"}` (G1 Plaits at 49 Hz could not be heard on a phone nor glide);
  hat `ox.xox.xox.xox.x`; clap `....X.......X.......X.......Xo..` (ghost right after the last clap);
  fx + kick preset punch.
- **speed_garage** (6.5/10): bpm 134 → 130; bass `R-----.R--/O-.R-R-----.R--/5-.O-` (was continuous mush);
  fx + bass preset pump; refs: Double 99 – RipGroove first, Kelly G mix → CJ Bolland – Sugar Is Sweeter
  (Armand's Drum & Bass Mix) (1996).
- **uk_garage** (7/10): snare becomes rims `{"voice":"rim","pattern":"....x..o....x.......x..o...ox.o."}`
  (today snare = clap, duplicate hit); perc → `{"voice":"shaker","pattern":"ooxoooxoooxoooxo"}`;
  kick `X.........X.....X.........X....o`; bass `R--....R..R--.O-` (hits with the kick, then answers);
  open_hat `..x...x.......x.`.
- **afro_house** (7.5/10): swing 0.15 → 0.20; new line open_hat `..x...x...x...x.`; dark progression
  `[0,6,5,3]` (i–VII–VI–iv); bass `..R-.R....R-.R-...R-.R....R-.5-.`.
- **house** (8/10): kick `X...X...X...X...X...X...X...X.x.` (soft push at the end of bar 2);
  perc `{"source":"tr707","voice":"shaker","pattern":"..x...x...x...x."}` (707 tambourine); fx + chords reverb 0.15.
- **deep_house** (8/10): kick and clap `"source":"tr909"`. Optional (by ear): perc tom 909 rolling
  `{"voice":"tom_mid","pattern":"..x.....x.x....."}` instead of congas.
- **acid_house** (8/10): bpm 122 → 120; kick and clap `"source":"tr707"` (all of Acid Tracks is a 707).

---------------------------------------------------------------------------------------------------------------
## 2. Converter / model features (Fixer B)

**F1. Pack sounds with a chosen fallback — why `pack:<style>:bass` gives "Basse analogique"** (S)
- Cause: packs are downloaded by the user, so on most phones the style's pack is not installed.
  `recipes_to_kotlin.py` `sound_of()` then writes the hard-coded `FALLBACK["bass"]["bass"] = "Basse analogique"`
  (also `DEFAULT_PRESET`) into `Melody.bass`, and `StarterWizard.soundsFor()` (`preferredSynth`, ~line 255) picks it
  when `chosenPack` is null. "Basse analogique" is a Plaits preset: no portamento, weak at octave 1.
  Second cause: recipes naming a synth (dnb, neurofunk, dubstep, riddim, techno, garage…) never prefer their own
  pack basses (styles.json gives these packs a `bass` kind): `soundsFor` only puts a pack first when
  `packSounds[part]` is set, so these basses only appear lower in the list.
- Fix (converter only): accept `"pack:<pack>:<kind>|<Preset>"`, split on `|`, validate the preset, emit it as
  `bass/pad/lead`; change the bass fallback to "Basse ronde" (synth, glides). No Kotlin change needed.
- Then (Fixer A, check by ear, revert a style if its pack bass is worse than its synth):
  `pack:neurofunk:bass|Basse qui parle`, `pack:techno:bass|Basse analogique`, `pack:tech_house:bass|Basse ronde`,
  `pack:uk_garage:bass|Basse ronde`, and `pack:juno:bass|Basse ronde` at octave 2 for house. Keep pure synth for dubstep/riddim/speed garage (the wobble/talk is the synth LFO a one-shot
  loses) and dnb reese. Benefits: house, tech house, garage, neurofunk, techno; hip-hop (boom_bap, lofi, afrobeats).

**F2. Pack chord samples played as one note** (S) — `StarterWizard.build()`/`startingNotes()`: when the Pad's
  PartSound is a pack `stab`/`keys`/`pad` sample (the generator prompts "chord, note C"), use `Voicing.Single`.
  Test in StarterWizardTest. Benefits: gabber (default ON today: 3 chords stacked), acid_techno, deep house later.

**F3. Rumble** (S preset + recipe, M if a proper role) — new `FxPreset("rumble")` in `FxPresets.kt`:
  REVERB (size 0.8, mix 1.0 = tail only) → DISTORTION (0.5, overdrive) → FILTER LP (cutoff 0.22 ≈ 150 Hz) → PUMP
  (0.9, kick trigger); add to converter `FX_PRESETS`, i18n name/description. Recipe use (works with current roles):
  a line `{"role":"808","voice":"kick2","pattern":<kick pattern>}` + fx `{"on":"808","preset":"rumble"}`; the 808 lane
  is already treated as bass by `scenePlan` (out of Intro and Break) and a line `voice` keeps the kit sound first
  even with packs. Drawback: the wizard step is titled "Basse 808" → later a `Role.Rumble` (with F5).
  Benefits: techno, industrial_techno, hard_techno (then hard techno's bass part can default off).

**F4. 4-bar drum lines (64 steps)** (S) — converter `line()` accepts 64 for drums (DrumPattern.MAX_STEPS = 64,
  `loopBars` already 4 with parts); update the 16/32 assertion in `StarterWizardTest` (line 23); check
  TechnoStylesTest hat/open-hat overlap. Rolls then come once per 4 bars instead of every 1–2.
  Benefits: jungle (full 4-bar Amen), gabber, retro_techno, hard_techno, dnb, tech_house, house.

**F5. Ride, Tom, Perc2 lines** (M) — new `StarterWizard.Role` entries on free pads (Tom lane 8, Ride lane 12,
  Perc2 lane 14; Crash comes with F6), titles/explanations + EN/NL, converter `ROLES`, `CLUB_INTRO_LINES` /
  `CHORUS_LINES` (ride = Drop layer), `SoundPacks.KIND_LABELS`; packs: add `ride`/`crash` kinds to tr909 in
  `scripts/packs/styles.json` + regenerate (kit cymbal voice 12 is a long crash, not a ride). Check WizardScreen
  with 7–8 drum steps. Benefits: techno, detroit, hard techno (ride), industrial (toms), tech house (ride + shaker),
  afro house (3–4 percs), UK garage (rim + tambourine), deep house (tom), liquid/dnb (ride tops).

**F6. Real Drop, Build and per-style Break** (L) — today Groove already has the bass and the Drop only adds a pad
  or lead (`scenePlan`), so nothing "drops". Add recipe fields: `"drop": ["bass", …]` (held back until the Drop),
  `"break": ["bass","perc","hat","lead"]` (tech house keeps bass+percs; deep/afro = drop minus kick),
  `"build": true` (Groove becomes "Montée": no kick nor bass, pad + hats, generated accelerating snare/clap roll in the
  last bar, riser from pack `fx` or "Souffle"). Transition scenes: "Drop (entrée)" with crash on step 0, kick cut in
  the last bar before it (scenes go up to 24). Files: `StarterWizard.kt` (Style, SongForm labels/roles, ScenePlan,
  scenePlan, sectionsOf, inScenes), converter, i18n, StarterWizardTest/TracksTest, app guide pages naming sections.
  Bass form: Intro 16, Montée 8, Drop 32, Break 8, Montée 8, Drop 32, Outro 16 (= 3:26 at 140, ok under 3:30).
  Benefits: all club styles; most for dnb, dubstep, riddim, neurofunk, jungle, tech house, techno.

**F7. Generic filter sweep from a recipe** (S–M, reuse P's mechanism) — `"sweep": [{"on":"chords|lead|bass|acid",
  "from":0.25,"to":0.8,"bars":16}]` → FILTER insert + SONG automation (AutoTarget INSERT_PARAM, kind 3, exists).
  Files: converter, `StarterWizard.withEffects` (or P's helper), Automation.kt. Benefits: techno (stab/lead),
  tech house (build), retro_techno (FZ-1 sweep), trance, hard techno breaks, speed garage, deep house intro.

**F8. Lead glide** (S) — converter `lead.glide`, `Melody.leadGlide`; in `build()` the `glides` test covers Part.Lead
  with '/' in the lead pattern. Benefits: retro_techno (hoover bends), gabber riff, hard techno, speed garage.

**F9. Two bass layers** (M) — converter `bass.sub: true` → `build()` adds a "Sous-basse" track (Basse sub, octave −1,
  same notes, FILTER LP) and a FILTER HP on the main bass; both map to Part.Bass in `partOfTrack` so the scene plan
  is unchanged (Tracks.MAX = 8). Benefits: dnb, neurofunk, dubstep, riddim, jungle, speed garage.

**F10. Missing synth presets** (S) — `SoundLibrary.kt` + converter `PRESETS` + i18n: "Basse riddim" (wobble with
  `LFO2_SYNC = SYNC_EIGHTH_TRIPLET`, more resonance), "Wobble lente" (SYNC_QUARTER, Skream), "Basse dred"
  (slow `F_ATTACK`, high FILTER_ENV: opens "backwards", speed garage), "Sirène dub" (VIBRATO + slow LFO1: a pitch
  siren, unlike today's "Sirène" filter sweep). Benefits: riddim (also stops sharing "Basse wobble" with dubstep),
  dubstep, speed garage, jungle, gabber/retro.

**F11. Parallel chords (chord memory)** (S–M) — `Voicing` with fixed semitone shapes (m7 `0,3,7,10`, m9 `0,3,7,10,14`)
  in `StyleMusic.kt` (`Harmony.voiced`), converter `VOICINGS`; check the scale lock does not snap them.
  Benefits: detroit, deep house, minimal/techno dub stabs.

**F12. Triplets** (L, engine) — `Hit.offset` in ticks or a roll spanning several steps: `Project.kt` Hit, ProjectJson,
  `engine/src/main/cpp/core/Engine.h/.cpp` (lane array like `repeats_`, lock-free command), host-tests; converter
  token. Benefits: dubstep kick triplets, riddim percs, afro house, trap hats.

**F13. Sampled breaks** (L) — a CC0 breaks pack (licence in THIRD_PARTY.md; not the Amen recording itself),
  pack kind `break`, Slicer slices on pads, recipe line `source: "breaks"` + slice/pitch per hit. Files:
  scripts/packs, SoundPacks.kt, StarterWizard, app Slicer hookup. Benefits: jungle (most), dnb/liquid tops,
  speed garage, UK garage.

**F14. Synth parameter automation** (L) — AutoTarget kind 6 on instrument tracks (cutoff, LFO2 rate/sync), applied
  lock-free in the audio thread; recipes vary wobble speed per bar and evolve the neuro growl.
  Benefits: neurofunk, dubstep, riddim.

**F15. Kick pitch and length per recipe** (M) — tuned kick on the key (pack kick root detection + kit voice tune).
  Benefits: gabber, hard techno. **F16.** Longer club songs (MAX_SONG_SECONDS per family, 32-bar DJ intro): a
  decision for DECISIONS.md, not code-heavy (S).

---------------------------------------------------------------------------------------------------------------
## 3. Priority (impact on "sounds like the real genre")

1. **Part 1, all 21 styles** (Fixer A, S–M): riddim wobble, neuro Q/A, dubstep groove, tech house audible bass,
   garage rims, gabber roll; immediate, no code.
2. **F6 Drop/Build/Break per style** (L): the bass-music drop is the missing core experience (dnb, dubstep,
   riddim, neuro, jungle) and tech house breaks keep the groove.
3. **F3 Rumble preset** (S): the Berlin/industrial/hard techno low end in one preset + one line.
4. **F1 + F2 + F8 quick model fixes** (S each): real pack basses and a sane fallback, no stacked stab chords
   (gabber default), hoover bends.
5. **F5 Ride / Tom / Perc2** (M): ride 909 in techno drops, shaker+ride in tech house, 3–4 percs in afro house.
6. **F4 64-step lines** (S): fills at phrase ends, the 4-bar Amen.
7. **F10 presets** (S): Basse riddim (triplet wub), Wobble lente, Basse dred, Sirène dub.
8. **F9 two bass layers** (M): sub + mid for all bass music.
9. **F7 filter sweeps** (S–M): movement in techno, tech house builds, rave sweeps.
10. **F13 breaks** (L) for jungle, then F12 triplets, F11 parallel chords, F14 synth automation, F15, F16.

After each feature: Fixer A updates the recipes that benefit (listed per feature), regenerates StyleRecipes.kt,
adds EN/NL texts, and adds the styles to `app/src/main/assets/recette.json` for Benjamin's phone check
(by-ear items: gabber octave stabs, pack basses, deep house tom, techno drum saturation).
