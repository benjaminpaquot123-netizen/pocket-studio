# Blind audit of `scripts/sons/tout.py`

Audited: `scripts/sons/tout.py` and `scripts/sons/selftest_tout.py` at commit
`73788069` (branch `eq/outil-tout`). The auditor did not write the tool.
No real audio was used: every number below comes from a synthetic signal or
MIDI file built by `scripts/sons/audit_tout_repro.py`.

    python3 -I scripts/sons/audit_tout_repro.py              # 50 s with --mutations, 10 s without
    python3 -I scripts/sons/audit_tout_repro.py --mutations

Each line of its output is `BAD` (defect still there) or `ok`, with the case
id used below (B1, I4, ...). Last run: 204 cases, 123 `BAD`, 81 `ok`.

## Verdict: fix first

Do not run the tool on the 11 packs yet. Four defects would make whole tables
wrong or unusable (B1 to B4). The measures themselves are mostly sound between
70 and 140 bpm and on clean one-shots; the weak parts are the reading of names
and folders, tempo above 150 bpm, and the shape of the CSV.

The self-test (177 checks, green) does test the core measures for real, but it
has no case for any of the four blocking defects.

## Blocking

### B1. The top folder is ignored as soon as there are two folder levels
`classify` drops the first folder (`inner = "/".join(folders[1:])`) when the
path has two folders or more. Packs are nearly always `Type/Subtype/file`.

| input (path, duration) | expected | got |
|---|---|---|
| `Vocals/Take 01.wav` 4 s | vocal | vocal |
| `Vocals/Dry/Take 01.wav` 4 s | vocal | oneshot_melodic |
| `Vocals/Adlibs/Hey 01.wav` 1 s | vocal | oneshot_melodic |
| `Vocal Loops/Dry/Hook 120 Am.wav` 8 s | vocal | melodic_loop |
| `Drum Loops/Tops/Top 128 01.wav` 7.5 s | drum_loop | oneshot_melodic |
| `Drum Loops/128/Full 01.wav` 7.5 s | drum_loop | oneshot_melodic |
| `Hi-Hats/Closed/HH 01.wav` 0.1 s | drum_oneshot | oneshot_melodic |

Fix idea: classify on every folder of the relative path; only skip a first
folder when it is the pack root shared by all files.

### B2. The output does not follow the column contract
Expected first columns, never missing:
`rar,kit_id,type,subtype,role,style,dur_s,sr,ch,size_kb,peak_dbfs,rms_dbfs,lufs_i,bpm_tag,key_tag,bpm_est,key_est,error`.
Got, in every table: `rar,kit_id,type,subtype,role,style,dur_s,sr,ch,peak_dbfs,rms_dbfs,lufs_i,error`.

- `size_kb`, `bpm_est` and `key_est` exist in no table (the estimates are
  called `bpm_measured` / `key_measured`, and only in some tables).
- `bpm_tag` and `key_tag` exist only in some tables, and after `error`.
- In `drum_loop`, `oneshot_melodic` and `midi`, the `# accuracy ...` lines come
  before the header row, so a plain CSV reader takes a comment as the header.

Fix idea: one `FIXED` list written first in every audio table, empty cell when
a value does not apply; accuracy lines only in `<prefix>_accuracy.csv`.

### B3. Every empty cell is written as a single quote
`mesure._safe_cell` tests `text[:1] in "=+-@"`; with an empty string this is
`"" in "=+-@"`, which is true, so `""` becomes `'`.

| input | expected | got |
|---|---|---|
| good drum loop, `error` cell | empty | `'` |
| file outside any kit, `kit_id` cell | empty | `'` |
| subtype `-` | `-` or empty | `'-` |

Consequence: "error is empty" filters keep nothing, and every good row looks
like a failed one. Fix idea: `text and text[0] in "=+-@"`.

### B4. Drum loops at 150 bpm and above are read at half tempo
Synthetic 4-bar loops (kick, snare, 8th-note hats), no tag given:

| input | expected | got |
|---|---|---|
| 70, 87, 100, 128, 140 bpm (four-on-the-floor and boom-bap) | exact | exact |
| 150 bpm | 150, 4 bars | 75, 2 bars |
| 160 bpm | 160, 4 bars | 80, 2 bars |
| 170 bpm | 170, 4 bars | 85, 2 bars |
| 174 bpm | 174, 4 bars | 87, 2 bars |
| 180 bpm | 180, 4 bars | 90, 2 bars |
| 70 bpm with 16th-note hats | 70, 2 bars | 140, 4 bars |

Both patterns fail the same way; only a loop with 16th hats is read right
above 150. The bar count is halved with it, and the accuracy table (I9) counts
all of these as plain disagreements. Drum and bass, jungle, footwork and fast
trap packs would be entirely wrong.
Fix idea: when a tag or a folder gives a tempo, pick the octave nearest to it;
without one, widen the prior (it is centred on 95 bpm) and use the snare
back-beat spacing to choose the octave; report half/double as its own class.

## Important

### I1. A loud kick with a 5 or 10 ms linear fade is called cut
`st.kick(t, 150, 50, 0.25)`, 250 ms, still loud at the end, linear fade:

| fade | expected | got |
|---|---|---|
| 5 ms linear | end_cut 0 | end_cut 1, no fade |
| 10 ms linear | end_cut 0 | end_cut 1, no fade |
| 20, 30, 50 ms linear | end_cut 0 | 0 (fade 17.5, 28.0, 44.5) |
| 5, 10 ms exponential | end_cut 0 | 0 (fade 5.5, 11.0) |
| 808 at 55 Hz, 10 to 50 ms linear | end_cut 0 | 0 |

The brief says 10 to 50 ms fades must not be cut. Fix idea: a fade is any
envelope that reaches under -60 dBFS at the last sample with no step; do not
require the level 5 ms before the end to be already low.

### I2. Other wrong `end_cut` / `end_fade_ms` verdicts

| input | expected | got |
|---|---|---|
| 50 Hz sine at -34 dBFS stopped dead | cut 1 | cut 0 |
| loud kick stopped dead, then 100 ms of digital silence | cut 1 | cut 0 |
| noise reverb tail decayed to about -51 dB, no fade | cut 0 | cut 1 |
| kick with 20 ms fade and a DC offset of 0.01 | cut 0 | cut 1 |
| seamless sustained loop (2 s of a held tone) | cut 0 | cut 1 |
| drum loop ending on the bar | no fade | fade 91.5 ms |
| 808 with 100 ms linear fade | fade about 100 | 67.5 |
| 808 with 200 ms linear fade | fade about 200 | 58.5 |

Every sustained loop of the packs will carry `end_cut = 1`, so the column only
means something for one-shots. Fix idea: remove DC first; make the thresholds
relative to the file peak; look for the step before trailing digital silence;
leave the cell empty for loop types.

### I3. Unreadable files all land in the `melodic_loop` table
`rows[row.get("type") or "melodic_loop"]`: a broken one-shot and a broken drum
loop gave 3 error rows typed `melodic_loop`. Expected: the type given by the
path, or a separate errors table. They would also count in that table's size.

### I4. BPM tags read from any bare number

| input | expected | got |
|---|---|---|
| `Loops/Bass Loop 100.wav` (an index) | none | 100 |
| `Drum Loops/Loop 66.wav` | none | 66 |
| `Loops/24Bit 96k/Bass Loop Am.wav` | none | 96 |
| `Melody Loops 140 BPM/Melody 74 Cm.wav` | 140 | 74 |

Fix idea: an explicit "N BPM" anywhere in the path wins; a bare number counts
only between 60 and 200 and when it is not the last token; ignore numbers next
to `k`, `bit`, `hz`.

### I5. Key and note tags read from stray capitals

| input | expected | got |
|---|---|---|
| `Loops/A Bass 125 Em.wav` key | E minor | A major |
| `Loops/D Bass Loop 125.wav` key | none | D major |
| `Loops 125 BPM/Bass G.wav` key | G major | none |
| `One Shots/E Piano C3.wav` note | C | E |
| `One Shots/B3 Organ C2.wav` note | C | B |
| `One Shots/Synth A 01.wav` note | none | A |
| `Kicks/Kick A 01.wav`, `Snares/Snare D 03.wav` note | none | A, D |

Fix idea: take the last candidate of the name, prefer one with a mode or an
octave, and never accept a lone letter followed by an index.

### I6. Closed and open hats, percussion heights

| input | expected | got |
|---|---|---|
| `Hats/Closed Hat 01.wav` | hat_closed | hat_closed |
| `Hats/HH Closed 01.wav`, `Hat Closed 01.wav`, `CHH_01.wav` | hat_closed | hat |
| `Hats/HiHat Open 02.wav`, `OHH 01.wav` | hat_open | hat |
| `Perc/Perc High 01.wav`, `Perc Low 01.wav` | perc_high, perc_low | perc |

Only the word order "Closed Hat" / "Open Hat" is recognised, and no perc
high/mid/low subtype exists at all. Fix idea: match `closed|open|chh|ohh` in
any order; add the three perc subtypes, from the name or from the centroid.

### I7. A stray word takes the type or the role

| input | expected | got |
|---|---|---|
| `One Shots/Kicks/Kick Impact 01.wav` | drum_oneshot | fx |
| `One Shots/Snares/Snare Reverse.wav` | drum_oneshot | fx |
| `Loops/Drums/Noise Hat Loop 128.wav`, `Drum Loops/Drop Beat 140.wav` | drum_loop | fx |
| `Kits/Kit 1/Kick 01.wav`, `Construction Kits/Kit 01/One Shots/Kick.wav` | drum_oneshot | kit_stem |
| `Vendor Construction Pack/Drum Loops/Loop 120.wav` | drum_loop | kit_stem |
| `Kit 01 120 Am/Stems/Bass.wav` | kit_stem | melodic_loop |
| `Atom Pluck C3.wav` role | not perc ("tom") | perc |
| `Chat Lead.wav` role | not hats ("hat") | hats |
| `808/808 Long F.wav` 6.5 s, `Cymbals/Crash 01.wav` 7 s | drum_oneshot | oneshot_melodic |
| `One Shots/Piano/Piano C3.wav` 9 s | oneshot_melodic | melodic_loop |
| `Loops/Percussion/Conga 120.wav` 4 s | drum_loop | drum_oneshot |

Fix idea: whole-word matches only; the folder decides before the file name;
"one shot" folders beat the duration limit; a drum word in the name beats an
fx word.

### I8. Key and tempo of melodic loops

| input | expected | got |
|---|---|---|
| A minor i-VII-VI-VII (Am G F G) | A minor | G major |
| A minor i-VI-III-VII (Am F C G) | A minor | C major |
| D dorian i-IV | D minor | D major |
| mono bassline, A minor pentatonic | A minor | G major |
| pad chords, no drums, 80 bpm, 4 bars | 80 | 120, 6 bars |
| same at 100 bpm | 100 | none |
| same at 140 and 174 bpm | 140, 174 | 70, 87 |
| 12/8 drum loop (8th triplets) at 120 bpm | 120, 2 bars | 180, 3 bars, swing 39.5 |

The melodic loop table also has no chord or progression column (only MIDI
has). Fix idea: weight the first and last chord and the bass note for the
tonic; for tempo, try the bar counts 1, 2, 4, 8, 16 against the file length
before the ACF; test a triplet grid next to the 16th grid.

### I9. The accuracy table hides the two commonest errors
- `_bpm_agree(140, 70)` is false: a half or double tempo is counted like any
  other miss, so B4 cannot be told from noise.
- A measured C major against a tag of A minor is `relative`, and
  `_key_agree` counts `relative` as agreement: the two wrong keys of I8 raise
  the score.

Fix idea: report four counts, exact / half-double (or relative) / other / no
measure, and compute `usable` on exact only.

### I10. MIDI

| input | expected | got |
|---|---|---|
| drum pattern on channel 1 (pitches 36, 38, 42) | drum_track 1, no key | drum_track 0, key F# minor, chords "io-io" |
| 8th-note triplets | no swing value | swing 66.67 |
| mono bassline | no chords | "i-v-i-v" |
| power chords A5 F5 C5 G5, tag A minor | A minor | C major, "I-VI-III-VII" |

Fix idea: treat a track as drums when most pitches are in 35 to 59 with very
short notes or when its name says so; require three pitch classes for a chord;
use the tag's tonic for the degrees when a tag exists.

### I11. Vocal wet/dry

| input | expected | got |
|---|---|---|
| dry phrases with gaps | dry | dry |
| room reverb, RT60 0.4 s, mix 0.3 | some | some (-44.9 dB) |
| hall reverb, RT60 2 s, mix 0.3 | wet | no verdict, no tail |
| hall reverb, RT60 2 s, mix 0.05 | dry or some | some (-36.9 dB) |
| delay 375 ms with feedback | wet | no verdict |
| dry voice with a noise floor 40 dB under it | dry | some (-39.9 dB) |

The wettest files get no verdict, because the level never drops 15 dB between
phrases. Fix idea: when no drop is found, call it wet if the envelope between
phrases stays within 15 dB; subtract the noise floor measured at the file end.

### I12. `rms_dbfs` adds the channels together
440 Hz sine at amplitude 0.5 (peak -6.02 dBFS):

| input | expected | got |
|---|---|---|
| mono | -9.03 | -9.03 |
| stereo, right channel at half level | -9.03 or lower | -8.06 |
| stereo, right channel inverted | -9.03 | -6.02 (equal to the peak) |
| 6 channels | -9.03 | -1.25 (above the peak) |

Stereo files read up to 3 dB hotter than mono ones, so the levels of a pack
cannot be compared. Fix idea: mean of the channel powers, not their sum.

### I13. One-shots

| input | expected | got |
|---|---|---|
| A minor chord one-shot (A3 C4 E4) | no note, or A | D1 |
| C8 (4186 Hz) | C8 | F6, 499 cents off |
| kick gliding 120 to 55 Hz (tau 0.08 s), note | A1 | A#1 |
| kick 200 to 50 Hz (tau 0.03 s), drop_start_hz | 200 | 163.6 |
| kick 300 to 45 Hz (tau 0.01 s), drop_start_hz | 300 | 177.5 |

Fix idea: leave the note empty when the harmonic ratio is low or several
pitches compete; read the kick note on the last third of the tail; shorten
the first pitch window for fast drops.

### I14. What the self-test does not notice
One measure is changed at a time and the self-test is run again.

Caught (the test is real): peak +0.2 dB, rms +1 dB, LUFS +0.5, f0 +3 %,
octave of the note, cents +15, attack / decay / release x1.5, sustain +3 dB,
loop bpm x2 and +1.5 %, bars +1, swing +2, melodic key and bpm, every MIDI
measure, vocal verdict forced, vocal pitch and range, every fx shape, hat
closed/open forced, kick drop_end and drop_t50, `end_cut()` forced to 0.

Missed (the self-test stays green):

| mutation | |
|---|---|
| `dur_s` x1.02 | missed |
| `end_cut` forced to 0, or to 1, in every row produced by `common` | missed |
| `end_fade_ms` x2 in every row | missed |
| vocal `tail_db` +10 dB | missed |
| kick `drop_start_hz` x1.3 | missed |
| drum `t40_ms` x1.5, `t60_ms` x1.5, `onset_ms` x3 | missed |

And it has no case at all for: a tempo of 150 bpm or more, a 5 to 10 ms fade,
a path with two folder levels, an empty `error` cell on a good row, the fixed
column list. Each blocking defect above would have needed one such case.

## Minor

- Bars with a trailing tail: 2 bars at 124 bpm plus 300 ms of silence gives
  2.25 bars; plus 1 s gives 2.5. Fix: round to the nearest power-of-two count
  when the excess is quiet.
- MIDI: one note hanging a 16th past bar 4 gives 5 bars; a truncated track is
  measured without an error; a tempo change keeps only the first tempo; 7th,
  sus and add9 chords are reduced to triads; no tempo event gives an empty
  `bpm_file` (120 is the standard default).
- WAV: a data chunk longer than the file is measured on what is there, with no
  flag (23989 frames of 48000); an odd-length chunk without its pad byte gives
  "no data chunk"; a 12-bit depth is accepted.
- Files longer than 600 s give the error "longer than 600 s" (fine, but it
  should be in the docstring of the error column).
- The cents value exists only when the name carries a note tag (`cents_gap`).

## Checked and found correct

- Levels: 1 kHz at amplitude 0.1 gives peak -20.0, rms -23.01, LUFS -23.0;
  full scale gives 0 / -3.01 / -3.0; K-weighting and gating behave; silence
  gives empty cells; a float file over 0 dBFS reads +6.02.
- Same numbers for WAV 8, 16, 24, 32-bit integer, 32 and 64-bit float,
  WAVE_FORMAT_EXTENSIBLE, AIFF 16 and 24, AIFC `sowt` and `NONE`; sample rates
  from 8 kHz to 384 kHz and 47999 Hz within 0.05 LU; mono, stereo, 6 channels
  (except I12).
- Broken files never crash and never give a number: 1 sample ("too short"),
  0 frames, zero-byte file, random bytes, cut header, NaN samples, ADPCM,
  RIFF that is not WAVE, MP3 renamed to .wav; bad MIDI (empty, random, header
  only, SMPTE time, giant variable-length value, 65535 tracks, RIFF MIDI).
- Public-repo safety: no file name, folder name, pack name, formula-like name
  (`=cmd|...`) or text-file content reaches any CSV or the console; `._` files
  and non-audio extensions are skipped; the tool refuses to write inside the
  repository.
- Drum-loop tempo from 70 to 140 bpm, exact, with the right bar count; swing
  at 50, 54, 58, 62, 66.7 and 70 % read within 0.5 point.
- Key: C major I-IV-V-I and I-vi-IV-V, A minor i-iv-v-i, F# minor i-iv-V-i,
  G major I-V-vi-IV, single chords, a file tuned 40 cents sharp.
- MIDI: "i-VI-III-VII", "I-V-vi-IV", 7th chords "vi-ii-V-I", inversions;
  note count, notes per bar, lowest and highest pitch, polyphony, velocity
  mean and spread; channel 10 as drums; 3/4 and 6/8 bars; running status;
  96 ticks per quarter; lead-in silence; swing 60 % reads 60.0.
- One-shot notes from C1 to C7, cents within 2; weak fundamental; noise gives
  no note; attack, decay, sustain coherent with the generated envelope.
- `end_cut`: abrupt stop of a loud kick gives 1; 1 to 2 ms fades give 1; 808
  with 10 to 200 ms fades gives 0; 808 stopped at a zero crossing or mid-cycle
  gives 1; one abrupt channel of a stereo file gives 1; same verdicts from
  22.05 to 96 kHz.
- FX: riser, downlifter, impact, flat noise, reverse cymbal, pitch sweep;
  length of 8 beats at 120 bpm.
- Drum one-shots: hat closed under 200 ms of decay and open above; 808 notes
  G1, F#1, E1; tom at 110 Hz gives A2; snare body at 200 Hz gives G3;
  `808/808 Bass 01 C.wav` and `Drums/808s/Deep Bass 808.wav` stay `kick_808`.
- Tags: "124 BPM", "120bpm", "87.5 BPM", F#m, Gbmin, Bbm, Db, Dm7, C#maj,
  "D# Minor", "Eb minor", "E flat minor", Cmin7, Abm; 2024, 808 and 303 are
  not tempos; "4 Bars" is ignored; kick attack and tail layers; MIDI by
  extension.
