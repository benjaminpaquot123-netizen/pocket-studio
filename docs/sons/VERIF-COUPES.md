# Blind check: are the "cut" kicks and 808s really cut?

Date: 2026-11-26. Pack: the drum one-shot zip measured in `docs/sons/rar/zip1-drums.csv`
(100 kicks, 50 808s, all 24-bit, peak-normalised). Aggregates only, no file names.

## Claim checked
"20 of 100 kicks have a cut tail and 30 of 50 808s are cut before -60 dB."

## The tool criterion
`scripts/sons/mesure.py`: `end_level_db` = mean power of the last 10 ms against
the peak power. The CSV flags a "cut tail" when `end_level_db > -60`.
That only says "the sound is still present when the file ends". It never looks
at how the file ends (click, step, fade).

Re-measured with an independent reader: same flags, 20/100 kicks and 30/50 808s.

## Method (own scripts, every flagged file + every unflagged file)
- level of the last 5 ms and last 50 ms (dBFS);
- last sample (dBFS) and the end step: last sample against the RMS of the
  sample-to-sample differences of the last 50 ms (a click is a step far above
  the natural motion of the waveform);
- period-by-period peak envelope over the last 250-400 ms: natural decay slope
  fitted 200-60 ms before the end, fade start = last peak within 1.5 dB of
  that trend, level just before the fade;
- decay slope extrapolated to -60 dB under the peak;
- high-frequency splatter (energy above 1 kHz) in the last 30 ms against the
  30 ms before.

## Results

| | kicks flagged (20) | 808s flagged (30) | unflagged (80 kicks, 20 808s) |
|---|---|---|---|
| last sample, worst file | -98 dBFS | -103 dBFS | -124 / -138 dBFS |
| end step vs natural motion | -45 to -82 dB (no step) | -55 to -95 dB (no step) | no step |
| level 50 ms before the end | -9 to -14 dBFS | -3 to -40 dBFS (median about -7) | mostly silent |
| level 5 ms before the end | -19 to -47 dBFS | -26 to -61 dBFS | silent |
| fade at the very end | yes, median 18 ms (10-90 %: 12-53 ms) | yes, median 24 ms (range 12-29 ms) | n/a |
| level just before the fade | median -9 dBFS | median -3 dBFS | n/a |
| body before the fade | still decaying slowly (6 of 20 flat) | flat sustain in 28 of 30 (under 10 dB/s) | decayed |
| extra time to reach -60 dB | 1 to 3 s for the slow ones | many seconds, often never (flat) | 0 |
| HF splatter at the end | none above normal (median -6 dB vs before) | none (median +0.7 dB) | none |

Kick files last about 0.46 s (one beat at 130 BPM); 808 files 3.43 s
(8 beats at 140 BPM). Every flagged file ends with a short, smooth fade to
digital silence. Not one file in the 150 ends on a step or a click.

## Verdict
- Really cut (audible click or abrupt step): **0 kicks, 0 808s**.
- Short but faded on purpose (no click, fine): **20 kicks, 30 808s**. The
  kicks are trimmed to one beat with a 10-50 ms fade. The 808s are
  "sustain" 808s (flat level, made to be held and stopped by note length or a
  release), with a 15-30 ms fade at the end of the file.
- Tool too strict: the whole "cut" count is a criterion artefact. "-60 dB not
  reached" means "long sound, trimmed with a fade", not "defect". Benjamin is
  right.

Only practical point for the app: a one-shot player that plays a sustain 808
to its end will stop it with the file's ~20 ms fade (a quick end, not a click).
Note length plus an amp release, as any sampler does, covers it.

The 16 GB pack figure (84/415 "cut" kicks) uses the same criterion; it was not
re-measured here but should be read the same way until checked.

## Recommended fix to `mesure.py`
Replace the "cut tail" flag (`end_level_db > -60`) by an end-discontinuity test,
and keep `end_level_db` as information only ("still sounding at end"):
1. `end_step_db` = 20 log10(|last sample|, worst channel) dBFS;
2. `end_step_rel_db` = that minus 20 log10(RMS of the sample differences over the
   last 50 ms);
3. `end_fade_ms` = time from the last envelope peak within 1.5 dB of the natural
   decay trend to the end.
Flag "cut (click)" only when `end_step_db > -60 dBFS` **and**
`end_step_rel_db > 0 dB` (the final jump to silence is bigger than the
waveform's own motion), or when the level 5 ms before the end is above
-30 dBFS with `end_fade_ms < 2`. Report "trimmed with fade" separately
(`end_level_db > -60` and `end_fade_ms >= 2`), as information, not as a defect.
On this pack that gives 0 cut, 50 trimmed with fade.
