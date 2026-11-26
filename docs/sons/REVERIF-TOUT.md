# Blind re-check of scripts/sons/tout.py (eq/outil-tout at d28313c5)

Verdict: ROUGE (fixable by fences): the claims hold, but three columns we rely on are misleading in cases the 14 archives will contain.

## Numbers (re-run here)
- selftest_tout.py: 275 checks, 0 failed. selftest.py: 140 checks, 0 failed.
- audit_tout_repro.py --mutations: 200 cases, 30 BAD. All 30 map to skipped items: I8 x10, I9 x1 (_bpm_agree 140/70), I2 fade length x3, I13 x5, I10 x4, I11 x4, minor x3.

## Spot checks (synthetic files, python -I, outside the repo)
| claim | result |
|---|---|
| B1 two-level folders keep the top folder (Vocals/Dry, Drums/Kicks, Drum Loops/140, FX/Risers) | ok |
| B2 column order: COMMON + TYPE_COLUMNS, end_cut/end_fade_ms last | ok (drum_loop, drum_oneshot) |
| B3 empty cells: no "None"/"nan" | ok; but "-" is written as `'-` (formula guard) in subtype/role |
| B4 150/160/174 BPM tags not halved | ok (bpm_tag = bpm_est = 150/160/174) |
| kick with 7 ms fade: not cut | ok (end_cut 0, fade 6-12 ms) |
| kick stopped hard: cut | ok |
| Closed Hats / Hat Closed / Hat Open / Open Hat | ok |
| Perc High / Low / Hi Perc | ok; **Perc Mid -> perc** (no perc_mid subtype) |
| "24Bit 96k" folder, "44.1k 24bit", "48 kHz" not a BPM | ok |
| "Bass Loop Am 24Bit 96k.wav", "Bass Loop 96kHz Am.wav", "Bass 96 kHz Am.wav" | **bpm_tag 96** (bare number beside a key in the same name) |

## New findings
1. end_cut on loops is a coin toss. A drum loop whose last hit still rings at the bar line reads end_cut 1: same pattern at 90/120/140/174 BPM gives 0,0,0,1 (hats on every 16th) and 0,1,1,1 (off-beat hats); end_fade_ms picks up the last hit's decay (58-61 ms). _seamless only covers sustained tonal loops.
2. bpm_tag reads "96k"/"96 kHz" as 96 BPM when the same name carries a key (I4 only fixed it in a separate folder).
3. Full-band noise bursts decaying to silence (tau about 10 ms, short file) read end_cut 1 in 10/20 seeds: the DC removal on the last 100 ms of a trimmed file leaves a constant -57 dB step. High-passed noise (150 Hz, 1 kHz, 6 kHz): 0/20. Low risk on real samples.
4. Perc Mid has no subtype (falls back to perc); "-" cells are written `'-`.

## Skipped items: misleading or incomplete?
| item | judgement |
|---|---|
| I8 tempo of drumless pads, 12/8, modal keys | MISLEADING on bpm_est/key_est of untagged melodic loops (120 for 80, 70 for 140, G major for A minor). Fenced only if the deep pass honours accuracy `usable`; otherwise use bpm_est/key_est only for tagged files or drum loops. |
| I9 _bpm_agree(140,70) | incomplete: counted in half_double, not agree. |
| I2 fade lengths | MISLEADING on end_fade_ms (67 for 100 ms, 49.5 for 200 ms, 61 on loops); keep it as "has a fade", never as a length. |
| I13 kick note, drop_start_hz, C8, chord one-shot | MISLEADING on kick pitch: note one semitone high (A#1 for A1), drop_start_hz 164/177 for 200/300. Read the kick note +-1 semitone and drop_start_hz as a lower bound. C8 and chord: rare. |
| I10 MIDI | MISLEADING on midi key/chords/swing (power chords, drum channel, triplets). MIDI is not a relied column: read notes/bars only. |
| I11 vocal dry/wet | MISLEADING on wet (2 s hall = None, noise floor = some); leave the column out. |
| minor (overhanging note, truncated wav, odd LIST chunk) | incomplete; the truncated wav gives numbers without a flag (rare). |

## Recommendation
Running tout.py on the 14 archives is safe. Before reading its tables, fence (in tout.py or when reading):
- end_cut and end_fade_ms: read on one-shots only (blank for drum_loop, melodic_loop, kit_stem, fx, vocal); end_fade_ms as yes/no.
- bpm_tag: ignore a bare number followed by k, kHz or bit.
- bpm_est/key_est of untagged melodic loops: only when accuracy says usable.
- kick note +-1 semitone, drop_start_hz a lower bound; wet column and MIDI key/chords left out.
