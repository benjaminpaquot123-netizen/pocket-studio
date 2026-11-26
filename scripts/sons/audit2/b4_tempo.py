"""Batch 4: tempo guessed on untagged drum loops (bpm_est, bars, swing are shown with no fence)."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *
i = 0
bad = tot = 0
def case(label, bpm, x, beats_per_bar=4, rel="Drum Loops/Loop 01.wav"):
    global i, bad, tot
    i += 1
    rows, con, code = one("b4", i, rel, x)
    r = rows[0]
    est = r.get("bpm_est") or ""
    bars_true = len(x) / SR * bpm / 60.0 / beats_per_bar
    ok = not off(est, bpm, 0.02)  # an empty cell is a fenced guess, not a wrong tempo (audit 3, defect 14)
    okb = not off(r.get("bars"), bars_true, 0.26 / bars_true)
    tot += 1; bad += (not ok)
    if not (ok and okb):
        fault("%s %g bpm: bpm_est=%s bars=%s" % (label, bpm, est, r.get("bars")))
    print("%-44s true %-5g bars %-4.3g | %-9s est=%-7s bars=%-5s swing=%-5s k=%s s=%s h=%s%s%s" % (
        label, bpm, bars_true, r.get("type")[:9], est, r.get("bars"), r.get("swing_pct"),
        r.get("grid_kick"), r.get("grid_snare"), r.get("grid_hats"),
        "" if ok else "  <<< TEMPO", "" if okb else "  <<< BARS"))

for bpm in (60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 128, 130, 135, 140, 145, 150, 155, 160, 165, 170, 174, 180, 190, 200):
    case("four on the floor, 8th hats, 2 bars", bpm, drum_loop(bpm, 2, "four", "8th"))
for bpm in (70, 80, 85, 90, 95, 100, 110, 120, 130, 140, 150, 160):
    case("boom bap, 8th hats, 2 bars", bpm, drum_loop(bpm, 2, "boombap", "8th"))
for bpm in (60, 70, 75, 80, 120, 130, 140, 150, 160):
    case("half-time (trap), 16th hats, 2 bars", bpm, drum_loop(bpm, 2, "half", "16th"))
for bpm in (160, 165, 170, 172, 174, 176, 180):
    case("drum and bass, 8th hats, 2 bars", bpm, drum_loop(bpm, 2, "dnb", "8th"))
for bpm in (90, 120, 150, 180):
    case("waltz 3/4, 8th hats, 4 bars", bpm, drum_loop(bpm, 4, "waltz", "8th"), 3)
for bpm in (80, 100, 120, 140):
    case("four, triplet hats (shuffle), 2 bars", bpm, drum_loop(bpm, 2, "four", "trip"))
for bpm in (90, 120, 126, 140):
    case("four, off-beat hats (house), 2 bars", bpm, drum_loop(bpm, 2, "four", "off"))
for bpm in (90, 100, 120, 128, 140):
    case("kick and snare only, no hats, 2 bars", bpm, drum_loop(bpm, 2, "boombap", "none"))
for bpm in (85, 90, 95, 120):
    case("boom bap swing 60 %, 16th hats", bpm, drum_loop(bpm, 2, "boombap", "16th", swing=0.6))
for bpm in (90, 120, 140):
    case("1 bar only, four, 8th hats", bpm, drum_loop(bpm, 1, "four", "8th"))
    case("8 bars, four, 8th hats", bpm, drum_loop(bpm, 8, "four", "8th"))
    case("2 bars + 1 s reverb tail after the loop", bpm, drum_loop(bpm, 2, "four", "8th", tail=True))
for bpm in (55, 58, 205, 210, 220):
    case("outside 60-200: four, 8th hats", bpm, drum_loop(bpm, 2, "four", "8th"))
for bpm in (87.5, 92.3, 123.45, 133.33):
    case("fractional tempo: four, 8th hats, 4 bars", bpm, drum_loop(bpm, 4, "four", "8th"))
print("TEMPO wrong (written and off by more than 2 %%): %d of %d" % (bad, tot))
done()
