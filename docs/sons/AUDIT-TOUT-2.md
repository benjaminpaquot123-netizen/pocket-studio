# Second independent audit of `scripts/sons/tout.py`

Audited: `scripts/sons/tout.py` at commit `b4d47baa` (branch `eq/outil-tout`), 2026-11-26.
The auditor did not write the tool. No real audio was used: every file is synthetic
and every name is invented.

Verdict: **to repair before use**. 40 defects (8 serious, 20 medium, 12 small).
Benjamin's order (2026-11-26): fix them all, and get the right answer rather than an
empty cell wherever the name or the sound allows it.

## Replays (run them before and after each fix)

    export AUDIT2_WORK=<a folder outside the repository>
    cd scripts/sons/audit2 && python3 -I b1_names.py     # one line per trap file

| script | what it replays | defects |
|---|---|---|
| `b1_names.py` | names -> type, subtype, role, style | 1-5, 9, 13, 14, 23-26 |
| `b2_tags.py` | names -> tempo, key, note | 6, 8, 10-12 |
| `b3_cut.py`, `b3b_falsecut.py`, `b3c_fuzz.py` | cut at the end | 15-18, 29-31 |
| `b4_tempo.py`, `b8_bars.py` | guessed tempo and bars | 7, 19, 20 |
| `b5_weird.py` | broken and odd files | 34-36 |
| `b6_misc.py` | writing the tables | 27, 32, 33, 39 |
| `b7_midi.py` | MIDI | 21, 22, 37 |
| `mutate.py <repo copy> <work dir> <out file> <seconds>` | 101 faults put in the tool, one at a time | tests |

`lib.py` holds the generators (kick, snare, hat, 808, drum_loop, chord_loop, write_wav,
write_aiff, write_midi) and `one(batch, idx, rel, x)`: one file = one pack.

## Tests

- `selftest_tout.py`: 338 checks, green. `selftest.py`: 140, green, but only on `mesure.py`.
- `audit_tout_repro.py`: 30 of 187 cases still bad (known, skipped on purpose).
- Mutation run (52 of 101 faults run in 85 s): **25 caught, 27 missed**.
  Caught: fences 5/5, measured tempo 4/4, level 4/5, one-shot measures 3/4, name tags 3/5,
  cut 2/5, type 1/5, style 1/5, writing 1/4, role 0/5, MIDI 0/4.
- Faults no test sees (a test to add for each):
  cut step threshold -60 -> -40, step 4x -> 1.5x motion, trailing trim -70 instead of -90;
  channels always 2; stereo width inverted; MIDI drum track any/all, swing default,
  last tempo, time signature ignored; roles ('mix' full, 808, clap snare, shaker hats,
  vocal role); styles (rnb, boom bap, house, trap; only "techno" is checked);
  bare number from 100 only, bpm word up to 180 only; loop from 0.2 s with a bpm word,
  drum loop despite a melodic word, drum one-shot max 3 s, melodic one-shot max 4 s;
  'nan' in cells, hidden `._` files measured, max length 60 s.
  Not run: 49, including the two open/closed hat swaps.

## Defects (test file -> obtained; expected)

### Serious

1. **Open / closed hat lost** when the word is in the folder or shortened.
   `Drums/Hats/Open/Hat 01.wav`, `Drums/Open Hats/Hat 01.wav`, `Drums/Hats/Closed/Hat 01.wav`,
   `Drums/Closed Hats/Hat 01.wav`, `Drums/Hats/Open/01.wav`, `Hat Op 01`, `Hat Cl 01`,
   `Hat Semi Open 01`, `Hat Half-Open 01`, `Hat Pedal 01`, `Opened Hat 01`, `Charley Ouvert 01`
   -> `hat`; expected `hat_open` / `hat_closed`. The `hat_measured` column exists and is not used.
2. **Drum thrown out by a "melodic" word** (substring match).
   `Synth Kick 01`, `Drums/Kicks/Synth Hard/Kick 01.wav`, `Synth Snare 01`, `Bass Drum 01`
   (role bass), `Kick Punchy Keyed`, `Kick Bassy 01`, `Snare Padded 01`, `Clap Leader`,
   `Hat Plucky 01` -> `oneshot_melodic`; expected `drum_oneshot`.
3. **A far folder with a loop word turns one-shots into loops.**
   `Loops & One Shots/Kicks/Kick 01.wav`, `Trap Beats/One Shots/Snare 01.wav`,
   `Drum Fills & Hits/Hits/Clap 01.wav`, `Beat Makers Pack/Hats/Hat 01.wav`,
   `Stems & Shots/Shots/Kick 01.wav`, `Drums/Fills/Solo Fill 01.wav` -> `drum_loop`;
   expected `drum_oneshot`.
4. **Loops without the word "loop".**
   `Tops/Top 128 01.wav` (7.5 s) -> `oneshot_melodic`, role drums, end_cut 1;
   `Hi Hats/Hats 128 01.wav` -> `drum_oneshot` hat; `Percussion/Perc 124 01.wav` ->
   `drum_oneshot` perc; `Breaks/Break 170 01.wav`, `Melodies/Melody 01 Cm.wav` (6 s),
   `Bass/Bassline 01 124 Gm.wav` -> `oneshot_melodic`; `Drums/Full Drums 01 120.wav` (8 s) ->
   `melodic_loop` untagged, role full. Expected: loops, with their tempo.
5. **Drum loop or melodic loop decided by substrings.**
   `Loops/Organic Drum Loop 120.wav`, `Sharp Hat Loop 140.wav`, `Monkey Drum Loop 100.wav`,
   `Padded Kick Loop 125.wav`, `Drum & Bass Loop 174.wav`, `Loops/Loop 01 120.wav` (drums
   inside), `Hat Tricks Vol 1/Loops/Loop 01 120 bpm.wav` -> `melodic_loop`;
   `Loops/What Loop 120 Am.wav`, `Custom Loop 01 120 Am.wav`, `Loops/Top Loop 01.wav` (a pad)
   -> `drum_loop`; role `drums` for `Upbeat Piano Loop 120 C.wav`, `Breakdown Synth Loop 128 Am.wav`.
   When the name does not say, the sound should decide.
6. **False tempo tag** from a gear number or a figure.
   `Juno 106 Chords Loop Am` -> 106; `SH-101 Bass Loop Am` -> 101; `CS-80 Pad Loop Dm` -> 80;
   `Rhodes 73 Loop Cm` -> 73; `MPC 60 Keys Loop Am` -> 60; `90's Piano Loop Cm` -> 90;
   `Route 66 Guitar Loop Am` -> 66; `Loop 100% Wet Am 120` -> 100; `Loop 85 Am 140` -> 85;
   `Bass Loop 64 Am 140` -> 64; `Bass Loop Am 96kHz24bit.wav` -> 96; `Bass Loop Am 96-kHz.wav` -> 96.
   Twelve "Juno 106" loops at a true 120: bars 3.5, accuracy 0/12. A tag far from what is
   heard (not equal, half or double) should be flagged, not trusted.
7. **Untagged drum loops: guessed tempo written as if sure.**
   `Drum Loops/Loop 01.wav`: 24 of 95 tempos wrong, 28 bar counts wrong (65 -> 130, 70 -> 140,
   80 -> 160, boom bap 150 -> 75, kick + snare 128 -> 64, 140 -> 70, dnb 172 -> 86, 174 -> 87,
   half-time 60 -> 120 ... 80 -> 160, triplet hats 80 -> 120, 100 -> 150, 120 -> 90, 140 -> 105,
   off-beat hats 90 -> 180, 55 -> 110, 205 -> 102.5, waltz 3 bars instead of 4, a ringing tail
   -> 2.25 bars). Expected: right, or marked as a guess.
8. **False key.**
   `Synth Loop 120 A.wav` -> A major, `Synth Loop 120 B.wav`, `Drum Loops/Top Loop 128 A.wav`,
   `Drum Loop 120 B.wav`, `Hat Loop 140 C.wav` (a variation letter; drums have no key);
   `Fm Synth Loop 120.wav` -> F minor; `Bb Trumpet Loop 120.wav` -> A# major;
   `Loop 120 C# m.wav` -> C# major (expected minor).

### Medium

9. **FX and vocal words steal sounds.** `Loops/Reversed Piano Loop 120 Am.wav`,
   `Noisey Bass Loop 140 F.wav`, `Texture Pad Loop 01.wav`, `Bass/Bass Sweep C2.wav`,
   `Synth/Atmosphere Pad C3.wav`, `Bass/Reese Drop 01.wav` -> `fx`; `FX/Reverse Crash 01.wav` ->
   `drum_oneshot` crash; `Loops/Guitar Phrase 120 Am.wav`, `Synth/Vox Lead C3.wav`,
   `Synth/Voice Pad C3.wav`, `Drums/Perc/Vocal Perc 01.wav`, `Loops/Chanting Monks Loop 90.wav`
   -> `vocal`.
10. **Tempo written in the name, not read.** `Drum Loops/Drum Loop 128.wav`, `Drum Loop 128 01`,
    `128 Drum Loop`, `Loop 01 - 140`, `DrumLoop_140`, `Loops/Bass Loop 120.wav`, `Piano Loop 120 C`,
    `Loop 120 G`, `Loop BPM 140`, `Loop bpm140`, `Loops/140/Loop 01.wav`, `Am/Loop 120.wav`,
    `Loop.120.Am.wav`, `BassLoop120Am`, `bass_loop_120_f#m`, `bass loop 120 am`,
    `BASS LOOP 120 AM`, `Bass Loop 120 CM`, `Loop 210 Am`, `Loop 58 Am`, `Loop 120 Asus4` -> empty.
    A bare number confirmed by the measured tempo (equal, half or double) can be trusted.
11. **Key alone, not read.** `Pad Loop F.wav`, `Pad Loop - F`, `[F]`, `Key F`, `in F`,
    `Piano Loop C 120`, `Blues in A 120`.
12. **False note on one-shots.** `Keys/Keys E Piano.wav`, `Keys/E Piano 01.wav`,
    `Bass/E Bass 01.wav` -> E; `Bass/A Bass 01.wav` -> A; `Synth/G Funk Lead 01.wav` -> G;
    `Synth/Plan B Lead.wav` -> B; `Synth/Pad Soft F.wav` -> F (may be true: check with the
    measured note).
13. **False style by substring.** `Gravel Snare 01`, `Brave Lead C3`, `Travel Kick 01` -> rave;
    `Horn Blast C3`, `Modern Bass C2`, `Soulful Snare` -> rnb; `Solo Fill 01` -> lofi;
    `Entrance FX 01` -> trance; `Warehouse Kick 01` -> house; `Strap Kick 01`, `Trapdoor Kick 01`,
    `Contrapunto Kick 01` -> trap; `Technology Lead C3` -> techno; `Electronic Drums/Kick 01` -> edm;
    `Jungle Percussion/Conga 01` -> dnb; `Garage Door Kick 01` -> garage; two styles -> the first
    of the list; Rock, Reggaeton, Drill, Disco, Funk, Hardstyle never recognised.
14. **Style lost when it is only in the pack folder's own name** (`Techno Essentials Vol 2/`
    given directly -> unlabelled; given through its parent -> techno).
15. **False cut on noisy sounds with a 4 to 8 ms fade** (hat, snare, noise: 5 ms -> 50-100 %
    cut, 6 ms -> 30-80 %, 8 ms -> 5-25 %, 10 ms -> 0-10 %, 12 ms and more -> 0; kick 5 ms -> 0 %).
    Root: `_short_fade` gives up on noise.
16. **Real cut missed when a quiet hiss follows** (hard cut + 50 ms at -80 dB -> end_cut 0 and
    end_fade_ms 50; + 200 ms hiss at -60 dB -> 0; 16-bit + 4-LSB dither -> 0).
17. **Real cut missed on quiet files** (file at -30 dB: hat, snare -> 0; at -40 dB: 808, lead
    too). The -45 dBFS threshold is absolute; it should follow the file's own level.
18. **end_cut left blank** for role `full` (`Kick Mastered 01`, `Kick Full 01`, `Snare Mix 01`,
    `Kick Song 01`) or a bpm in the name (`808 Long 140bpm C.wav`).
19. **`bars` of untagged melodic loops** shown from the hidden guess: `Loops/Pad Loop 01.wav`,
    `Loops/Pluck Loop 01.wav` (4 true bars) -> 2 or 8; 11 of 20 wrong.
20. **Untagged kit rows unfenced**: `Construction Kits/Kit 01/Pad.wav + Strings.wav` at 140 ->
    70, 2 bars; 174 -> 87.
21. **MIDI: the tempo of the name is never reported** (`MIDI/Chords 140 bpm Am.mid` -> none or
    the file's 120; `Trap MIDI/Hi-Hat Roll 140bpm.mid` -> none).
22. **MIDI bars counted from tick 0** (4 bars exported from bar 9 -> 12 bars, 1 note per bar;
    start on beat 2 -> 5; legato overlap -> 5 and poly_max 6).
23. **Role errors.** `808 Snare 01`, `808 Clap`, `TR-808 Hat Closed` -> role 808;
    `Drums/TR-808/Kick 01.wav`, `Kit 808/Kick 01.wav`, `Kick 808 Style` -> kick_808;
    `Cowbell 01` -> bell; `Kickdrum 01` -> drums.
24. **Perc height in the folder ignored**: `Drums/Perc/Mid/Perc 01.wav`,
    `Drums/Perc/High/Perc 01.wav`, `Conga High 01` -> perc.
25. **Long sounds taken for loops**: `808 Very Long C.wav` (11 s), `Cymbals/Crash Long.wav`
    (11 s), `Synth/Long Pad Cm.wav` (9 s), `Drone Dark.wav` (12 s) -> `melodic_loop` untagged;
    `Drums/808s/808 Long 140bpm C.wav` (2 s) -> `drum_loop`.
26. `Kits/Kit A/Bass C2.wav` -> a kit row and a kit_stem row for a single one-shot.
27. `--out` in a missing folder -> `FileNotFoundError` after everything has been measured.
28. Tables are anonymous (no file name): a wrong row cannot be traced back. To decide.

### Small

29. Files shorter than 10 ms are never "cut".
30. Finished kick + hiss or hum at -40 dBFS -> cut; 8-bit finished kick -> cut.
31. Reversed sounds -> cut (true by design; say it in the column doc).
32. `_fmt`: 9999.7 -> `1e+04`, 123.45 -> `123.5`, `-0`.
33. Tables of a previous run are kept when a type has no row any more.
34. Truncated WAV / AIFF / MIDI measured without a flag (AIFF: duration 0.002563 s).
35. A constant DC offset is measured as a normal sound.
36. A pack with only FLAC / MP3 / OGG -> 0 rows, said on the console only.
37. MIDI tempo change -> first tempo only; 3/4 without a time signature -> 3 bars.
38. Flats are shown as sharps (Bb -> A#).
39. The repository guard only works inside a git checkout.
40. The `end_cut` docstring says -30 dBFS, the code uses -45.

## What works (do not break)

No crash on odd files (a clear `error` cell); duration, level and LUFS exact; loud hard cuts
caught at 44.1, 48 and 96 kHz; fades of 12 ms and more never flagged; "24Bit 96k",
"96 kHz Am", "Perc Mid", "Hat Open / Closed", `OH` / `CH`, "140 bpm", dashes, accents;
formula guard in cells.

## Five lots (zones of `tout.py` that do not overlap; each lot adds its own test file)

| lot | theme | defects | zone it owns | tests |
|---|---|---|---|---|
| A | names -> type, subtype, role | 1-5, 9, 23-26 | `classify`, `drum_family`, `ROLES`, `EXTRA_RULES`, `*_RX`, `*_WORDS`, `kit_split`, `_measure_file` | `selftest_tout_a.py` |
| B | names -> tempo, key, note, style | 6, 8, 10-14, 38 | `parse_tags`, `_key_candidates`, tag regexes, `style_of`, `GENRES` | `selftest_tout_b.py` |
| C | guessed figures and fences | 7, 18 (fence part), 19, 20 | `tempo`, `bars_of`, `measure_drum_loop`, `measure_melodic_loop`, `fence_row`, `Accuracy`, `_run_kits` | `selftest_tout_c.py` |
| D | cut at the end | 15-17, 29-31, 40 | `_short_fade`, `_seamless`, `_end_one`, `end_cut` | `selftest_tout_d.py` |
| E | MIDI and writing the tables | 21, 22, 27, 28, 32-37, 39 | `parse_midi`, `measure_midi_bytes`, `_midi_row`, `_fmt`, `_walk`, `main`, `read_audio`, `mesure.py` | `selftest_tout_e.py` |

Nobody edits `selftest_tout.py` (shared): a lot that must change an old check asks the chief.
Each lot: replay first, commit "rouge: subject", then "vert: same subject"; old self-tests
stay green; the lot's replay script shows the expected value on every trap file above.
After the five lots: one fresh blind re-check with the replays and `mutate.py`.
