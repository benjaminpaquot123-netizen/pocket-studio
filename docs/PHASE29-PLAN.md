# Phase 29: the remaining points (REVIEW.md phases 1–28, PROGRESS next steps, STYLE-FIXES)

Base: `/home/user/ps-p22` = main after phase 28 (c51e22f). Every "open" item below was checked in the current
code (file:line). Ratings: **Impact** for Benjamin (live on phone + Tab A11, acid/techno/DnB/dubstep) H/M/L;
**Risk** = data loss / crash / wrong sound / wrong export; **Size** S/M/L.

## 0. Triage of REVIEW.md "Restant" (checked against the code)

### Still open, worth doing now (→ packages A–D)
| # | Item (REVIEW phase) | Evidence | Impact | Risk | Size |
|---|---|---|---|---|---|
| 1 | Master meter sits after the limiter; instant-attack limiter pumps ~8 dB on default kit + acid (16, DECISIONS:151) | `Mixer.cpp:481-492` (peak taken after `gain`), `kCeiling` `Mixer.h:34`, no level test | H | wrong sound + export | M |
| 2 | Click when an effect is added/removed/changed while playing (16) | `Mixer.cpp:77-87` `setInsertType` swaps at once; the gate fade (`:136-158`) only covers « Joue dans » | M | wrong sound | S–M |
| 3 | Metronome goes through master inserts, Live FX (tape stop, DJ filter) and pumps the limiter (16) | `Mixer.cpp:442-447` adds `direct` before `processInserts(kMaster)` and `perf_` | L–M | wrong sound | S |
| 4 | Long export in background has no foreground service (16) | `AndroidManifest.xml:51-59`: only Playback + Separation services; `RhythmViewModel.kt:5687 startExport` in VM scope | M | lost export | M |
| 5 | Update install confirmation lost when the app is in background (0) | no notification in `app/.../update/*` | L–M | none (retry) | S |
| 6 | Tempo change while playing: synced pad clicks and its loop goes silent until the next hit; a replaced sound is cut dead (2, 16) | `Sampler.cpp:39-44` `install` zeroes `voices_`/`tails_` | H (live) | wrong sound | M |
| 7 | Remix following the tempo jumps position when the stretched copy lands; stems start without fade; click when stopped/removed (5, 16) | `StemPlayer.cpp:171-178` keeps `pos_` in old frames, `:238-241` start at gain 1, install(null) instant | M | wrong sound | M |
| 8 | Arpeggio forgets held keys at Stop (8) | `InstrumentTrack.h:59-67` `allNotesOff` sets `arp.count = 0` | L–M | — | S |
| 9 | No pitch bend on General MIDI instruments (8) | `InstrumentTrack.h:118` Bend → synth/plaits/sampled only | L–M | — | S |
| 10 | Song mode: a pad/note recorded at a part change lands in the old scene; recording while « Rester sur ma scène » writes the shown scene timed on the playing one (16, 21) | `RhythmViewModel.kt:4955-4962` `recordPad` edits `p.drums` (shown scene); `recordTick` `:4919-4927` | M–H | wrong take | M |
| 11 | Song mode: editing a track muted in the playing part makes it audible until the next part (7, 16) | `RhythmViewModel.kt:4064-4067` `editNotes` → `pushNotes` raw clip (`:3913`); same for `pushAcid`/`pushLane`; only `pushPart` (`:1992`) uses `partView` | M | wrong sound | S–M |
| 12 | Each fader/pan move re-sends the whole mixer (16) | `RhythmViewModel.kt:4579` `editChannel` → `pushMixer(project)` | L–M (Live) | CPU burst | S |
| 13 | « Enregistrer le morceau » (resample) builds its offline engine on the main thread (25) | `RhythmViewModel.kt:4191` `OfflineExport()` + `captureInto` on Main | L | jank | S |
| 14 | MIDI export without arpeggios/strums nor rolls (16, 28) | `MidiFile.kt:108` raw `notes` (no `performance.expand`), `:138` ignores `hit.repeats` | L–M | wrong export | S |
| 15 | Style lines limited to 16/32 steps (STYLE-FIXES F4) | `scripts/packs/recipes_to_kotlin.py:206` | M | — | S |
| 16 | Missing presets: Basse riddim, Wobble lente, Basse dred, Sirène dub (F10) | absent from `SoundLibrary.kt`/`SynthPatch.kt` | M | — | S |
| 17 | Two bass layers (sub + mid) for bass music (F9) | no `sub` key in converter | M | — | M |
| 18 | Parallel chord shapes (m7/m9 « chord memory ») (F11) | `Voicing` has diatonic Seventh/Ninth only (`recipes_to_kotlin.py:117`) | L–M | — | S–M |
| 19 | Keyboard can write above C8 (28 music) | REVIEW 28; piano roll caps at C8, keyboard octave 7 not capped | L | — | S |
| 20 | Acid line style found only by project name (28 music) | REVIEW 28 (`WizardAcid.generateFor` guesses from the name) | L | wrong feel | S |

### Already fixed or obsolete (dropped)
- STYLE-FIXES part 1 (all recipe edits: riddim wobble, neuro Q/A, dnb glide…) applied (`recipes/bass.json`); F1 `pack|Preset`
  (`recipes_to_kotlin.py:187`), F2 single voicing, F3 Rumble, F5 Tom/Ride/Perc2 (`StarterWizard.kt:52-67`), F6 build/drop/break
  (`recipes_to_kotlin.py:35-37,298`), F7 sweeps, F8 lead glide (`:267`) are done.
- Mic take with the screen off: done in phase 17 (`PlaybackService.kt:151` microphone type) → only a phone test (section C).
- Remix « un seul morceau épinglé »: fixed in phase 20 (`StemStore.inUse` reads every project, `StemStore.kt:106`).
- Acid playhead = drum step (phase 1 #12): now `live.absoluteStep` (`AppRoot.kt:955`).
- Phase 0/1 items on stream start/stop thread, rotation cutting sound, `nativePost` ignored, unwatched queue: superseded
  by PlaybackService, command waits/resends (phases 12, 27, 28).

## 1. Work packages (parallel worktrees, minimal overlap)

Shared-file rules: `engine/host-tests/engine_tests.cpp` is one file: A adds its tests right after the limiter tests
(~line 1460/1607), B after the Sampler tests (~line 1850) and the stem tests (~line 2840), never at the end of the file.
`RhythmViewModel.kt` (5 974 lines): each package edits only its named regions. `docs/DECISIONS.md`, `REVIEW.md`,
`changelog.txt`, `recette.json`: integrator only (fixers put their lines in the PR description).

### Package A: master level, effect clicks, export in background (engine Mixer + Mixer screen + Android shell)
Files: `engine/src/main/cpp/core/Mixer.{h,cpp}`, `engine/src/main/cpp/android/JniBridge.cpp` (meters array only,
`:635-645`), `engine/.../NativeEngine.kt` (meter count), `app/.../mixer/MixerScreen.kt`, `project-model/.../Mixer.kt`
(new-project master default only), new `app/.../export/ExportService.kt`, `AndroidManifest.xml`, `app/.../update/*`,
`RhythmViewModel.kt` export region only (`:5681-5830`).
- [ ] A1 Limiter: short look-ahead (≤ 1 ms = 48 frames, preallocated delay line; decide in DECISIONS with retour = 0),
      smooth attack over the look-ahead, release 120 ms kept; still −1 dBFS ceiling. No allocation, no lock.
- [ ] A2 Meter before the limiter: publish the pre-limiter master peak (new slot in the meters array, JNI layout test);
      MixerScreen master meter shows it with a red zone above −1 dBFS; existing « Le limiteur protège (−x dB) » kept.
- [ ] A3 Headroom without changing old projects: host test renders default kit + acid (+ synth) 4 bars at defaults and
      records gain reduction; new projects (blank, demos, wizard via one helper in `Mixer.kt`) start at a master trim
      that keeps GR ≤ 3 dB (expected ≈ −6 dB). Old projects untouched; Mixer shows a one-tap hint « Le limiteur
      travaille fort : baisser le volume général de 6 dB ? » when GR > 6 dB for 2 s (one undo step).
- [ ] A4 Effect switch without click: `setInsertType` while sounding → pending type per slot (fixed array), ramp the slot
      gate out (~5 ms), swap + reset, ramp in; type None ramps out then frees the unit. Same for `clearInserts`.
- [ ] A5 Metronome (and test tone) added after master inserts and Live FX, before the limiter.
- [ ] A6 Export foreground service (`dataSync` or `mediaProcessing`, progress notification, Annuler), started by
      `startExport`, stopped on end/cancel/error; Android 14+ type declared; falls back gracefully if refused.
- [ ] A7 Update: if the install confirmation cannot show (app in background), post a notification « Mise à jour prête :
      toucher pour installer » with the confirmation intent.
- [ ] (optional, if time) phase 28 m3: preview/assistant engines without `LongFxPool` (lazy), −18 MB per extra engine.
Tests: host — limiter holds ceiling with look-ahead on a 0 dBFS+12 dB burst, no overshoot, GR value; meter pre-limiter >
ceiling when driven; type switch mid-sine max sample step < threshold (fails today); metronome unaffected by tape stop;
zero-allocation test extended (pending type switch). JVM — `MixerMetersTest` with the new slot, hint shown/hidden, one undo
step, new project master default, old project unchanged on load; Robolectric service start/stop on export (capture).
Roborazzi: Mixer phone + tablet8 + scale130.

### Package B: tempo changes and sound continuity (engine voices + Remix/pad stretch glue)
Files: `engine/src/main/cpp/core/Sampler.{h,cpp}`, `StemPlayer.{h,cpp}`, `InstrumentTrack.h`, `SoundFontPlayer.{h,cpp}`,
`app/.../rhythm/PadSamples.kt`, `RhythmViewModel.kt` remix stretch region (`:3480-3600`) and `setTempo`/`tapTempo`
(`:5402-5415`) only.
- [ ] B1 `Sampler::install` on a sounding slot: the old voice becomes a tail with a 5 ms fade (no hard cut); the old
      sample is retired only when its tail ends (per-slot pending retire, no allocation). A looping pad (fitted or not)
      restarts on the new sample at the same phase of its region (frac of start..end), so a tempo change keeps the loop.
- [ ] B2 `StemPlayer::install` while playing: map `pos_` and `startFrame_` by newFrames/oldFrames (same musical
      position), 10 ms crossfade old → new (old bundle retired after the fade); `start` fades in (~5 ms);
      `install(nullptr)`/stop of a playing bundle fades out before letting it go.
- [ ] B3 Kotlin glue: Remix « suit le tempo » sends the new copy without a stop/restart; old copy closed off the main
      thread (`old?.close()` today on Main: the freeze of phase 16); pads keep `lastFitted` until the new copy is in.
- [ ] B4 Arpeggio keeps the keys still held across Stop/▶ (only `sounding` released at Stop); keys released while
      stopped leave the list.
- [ ] B5 Pitch bend (and mod wheel if TinySoundFont exposes it) on GM tracks: `tsf_channel_set_pitchwheel`, range ±2.
- [ ] (optional) 32 automation lanes instead of 16 (`Engine.h:186,323`, `Automation.kt:265`, memory +0.34 MB, JSON
      compat, message text): only if B1–B5 are done; Benjamin's rich Live sets hit the limit.
Tests: host — fitted loop pad: install a 0.8× copy mid-loop, no sample jump > threshold and sound continues (fails today);
replaced one-shot fades instead of cut; old sample freed after the tail (ASan, retire count); stems: install stretched
copy at frame N → position N×ratio, continuity; start fade-in; stop no click; zero-allocation test with installs during
playback; arp held across stop; GM bend changes pitch (mini SF2 already in tests). JVM — tempo drag with followTempo:
one stretch, no restart command.

### Package C: song mode and recording (RhythmViewModel song/record regions)
Files: `RhythmViewModel.kt` regions `:1590-2210` (scenes, song, push), `:3905-3920` (`pushNotes`), `:4060-4100`
(`editNotes`), `:4183-4210` (`resampleSong`), `:4570-4610` (mixer push), `:4860-4965` (record), `:5100-5190` (acid
push), `:5422-5470` (undo/redo); `project-model/.../MidiFile.kt`; app tests.
- [ ] C1 Record into the scene the engine plays at the hit's (latency-corrected) position: keep the hit's scene from
      `sceneHeard()` and the swap counter; a hit just before a part change stays in the old part's scene, just after goes
      to the new one; « Rester sur ma scène »: notes and hits go to the scene heard (said in the REC line).
- [ ] C2 Edits during song playback respect the playing part: notes/drums/acid of a track muted in that part are pushed
      empty (one helper used by `editNotes`, `clearTrackNotes`, `pushLane`, acid edits); edits to the shown scene that
      is not playing keep waiting (existing `editsWait`).
- [ ] C3 Fader/pan/send moves send only that strip (`editChannel` → one-strip push; full `pushMixer` stays for sync).
- [ ] C4 `resampleSong`: build `OfflineExport` and `captureInto` off Main as `startExport` does since phase 25.
- [ ] C5 Undo during song playback keeps the part queued « ensuite » (phase 21; first write a failing test, drop if it
      already passes).
- [ ] C6 MIDI export: notes via `performance.expand` (arpeggio, strum) and rolls as `repeats` hits; overlapping same
      pitch: end the first just before the next start.
Tests: ViewModel tests with `playingForTest` + command capture (exists since phase 27): hit at part boundary goes to the
right scene (fails today); muted-in-part track edit sends no notes; one-strip push count; undo keeps queued. Model:
`MidiFileTest` arpeggio/roll/overlap round-trip.

### Package D: style features left in STYLE-FIXES (converter + model, no engine)
Files: `scripts/packs/recipes_to_kotlin.py`, `scripts/packs/recipes/*.json`, `project-model/.../StyleRecipes.kt`
(regenerated only), `StarterWizard.kt`, `SoundLibrary.kt`, `SynthPatch.kt` (presets only), `StyleMusic.kt`,
`WizardAcid.kt`, `Project.kt` (one optional `style` field), `i18n/{en,nl}/model.json`, `app/.../synth/` keyboard octave
cap, project-model tests. Never touches `Mixer.kt` (A owns master defaults).
- [ ] D1 F4: 16/32/64-step drum lines in the converter; recipes that benefit: jungle (4-bar Amen), dnb, gabber,
      hard techno, retro techno, tech house, house (fill at bar 4). Update `StarterWizardTest` length assertion.
- [ ] D2 F10: presets « Basse riddim » (LFO2 sync 1/8T, more resonance), « Wobble lente » (1/4), « Basse dred »
      (slow filter attack), « Sirène dub » (pitch LFO); riddim uses Basse riddim, dubstep keeps Basse wobble.
- [ ] D3 F9: `bass.sub: true` → a « Sous-basse » track (Basse sub, octave −1, same notes, LP) + HP on the main bass,
      both Part.Bass in the scene plan (≤ 8 tracks); dnb, neurofunk, dubstep, riddim, jungle, speed garage.
- [ ] D4 F11: fixed-shape voicings (m7, m9) not snapped by the scale lock; detroit, deep house, minimal dub stabs.
- [ ] D5 Project remembers its style id (`Project.style`, optional, old files null) so Acide › Générer keeps the feel
      after a rename; keyboard octave capped so no note above C8.
- [ ] D6 Texts EN (British) + NL (informal) for every new name/tip; `recette.json` lines handed to the integrator.
Tests: converter run + `StyleRecipesUpToDateTest`, `MusicTheoryTest` probe (ranges, ≤ 3 min 30, every line heard),
`TechnoStylesTest` (hat/open-hat overlap at 64 steps), new tests for sub layer (same notes, octave −1, Part.Bass),
voicing shapes, style id round-trip and old JSON, i18n key test.

## 2. Left for later (one line why)
- Non-4/4 in piano roll and acid (`Notes.kt:27` multiples of 16, `Acid.kt:95` pages of 16): Benjamin's genres are 4/4;
  touches model, engine loops, two editors, MIDI; M–L. Next « métrique » phase.
- F12 triplet grid (engine `Hit` offsets, commands, UI): L; dubstep/trap nicety. Live « Répéter 1/16T » covers live use.
- F13 sampled breaks: needs a CC0 breaks pack, licence check, pack pipeline, slicer hookup: L.
- F14 synth parameter automation (cutoff/LFO per bar): L, engine + model + UI.
- F15 kick tuned to the key, F16 longer club songs (decision only): low gain now.
- Project opened on the main thread (`RhythmViewModel.kt:353` `initial = loadOrCreate()`, up to 3 s after a long
  take): touches the save path; under the 5 s ANR; do in a data-focused pass.
- Phase 28 m4 (launch priority behind a resync), borrowed reverb cleared in the callback (0,15 Mo memset), chorus/
  « Mouvement » LFO not restarted, Plaits/DaisySP shared RNG: measured harmless.
- Resampling at every load with `SRC_SINC_BEST` (`Sample.cpp:26`, phase 2 m7): slower project open only; cache later.
- DrumKit pan jump on retrigger (`DrumKit.cpp:298`), Chopper ignores swing, metronome-only nits: inaudible or rare.
- ● on the synth grows the scene to 4 bars (`RhythmViewModel.kt:4874-4882`): already announced on screen.
- Song timeline without the Remix column, `automationBefore` ignoring play/from, take of moved scenes, 1024-bar JSON
  size, 16 Live lanes (unless B optional), Live journal 2.7 Mo: rare in practice, each M.
- Live pads without transport keep playing on unplug; zip pack entry count; StemStore reading all projects: rare.
- UI/test nits (touch targets < 48 dp left, duplicated test tags, `HeldKeysTest` copy, « Nommer la scène » FR prefill,
  scroll-to-top in Générer/Remix, Colour/Color, dd/MM dates, 130 % « Genereren » wrap): polish batch later.
- Stems 0,77 Mo/s in memory, 32-bit Remix .zip size, cloud backup > 25 Mo: platform limits, documented.
- Housekeeping (no code): GitHub issues #8–#15 (crashes of 0.3.40–0.3.45, fixed in phases 3–5) and #5 (recette phase
  1) are still open: close them with a short French comment.

## 3. Pour Benjamin (une seule demande groupée, à l'écoute sur le téléphone)
1. Niveau : ouvre un projet neuf et un ancien projet techno avec l'acide. Ça pompe encore ? C'est assez fort ?
   Et le nouveau vumètre du Mixeur (zone rouge) : clair ?
2. Tempo en jouant : un pad calé en boucle + le Remix qui suit le tempo, bouge le tempo. Plus de clic, plus de trou ?
3. Effets : ajoute ou enlève un effet pendant la lecture. Plus de clic ?
4. Micro écran éteint (Magic 8 Pro) : une prise voix, écran éteint, 30 secondes. Le son est là ? Tu vois une pastille
   ou la Capsule magique ?
5. Export long : exporte tout le morceau puis va sur une autre appli. Il finit quand même ?
6. Sons à écouter : Log drum, Basse qui parle, Basse wobble, et les nouvelles (Basse riddim, Wobble lente, Basse dred,
   Sirène dub), la sous-basse en drum & bass et dubstep, les stabs gabber en octave, les basses des packs (house, tech
   house, garage, neurofunk, techno), le tom 909 de la deep house, la batterie saturée en techno, la nappe techno.
7. Styles forts (hard techno, neurofunk, riddim, acidcore) : le Drop sature ou ça va ?
8. Si tu as un clavier MIDI : le pitch bend sur un instrument General MIDI marche ?
9. Tablette Tab A11 : un tour rapide du Mixeur et du Live, rien de coupé ?
