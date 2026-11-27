#!/usr/bin/env python3
"""Self-test of the timbre tools: every measure is checked on a sound whose answer is known.

    python3 -I scripts/son/timbre/selftest.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import timbre as T  # noqa: E402

SR = T.SR
FAILED = []


def check(name, got, want, tol):
    ok = abs(got - want) <= tol
    print("%s\t%s\tgot %.4g, expected %.4g (tolerance %.3g)" % ("ok  " if ok else "FAIL", name, got, want, tol))
    if not ok:
        FAILED.append(name)


def main():
    rng = np.random.default_rng(0)
    n = SR
    t = np.arange(n) / SR

    # A decaying 1 kHz sine: centroid 1000 Hz, t20 = tau * ln(10).
    tau = 0.050
    x = np.sin(2 * math.pi * 1000.0 * t) * np.exp(-t / tau)
    for name, f in (("measure", T.fingerprint(x)), ("counter", T.counter_fingerprint(x))):
        check("%s: centroid of a 1 kHz sine" % name, f["centroid_hz"], 1000.0, 10.0)
        check("%s: octave band 800-1600 holds it all" % name, f["env4_db"], 0.0, 0.1)
        check("%s: t20 of the mid band" % name, f["t20_mid_ms"], 1000.0 * tau * math.log(10.0), 2.0)
    f = T.fingerprint(x)
    check("t20 of exp(-t/tau)", f["t20_ms"], 1000.0 * tau * math.log(10.0), 0.5)
    check("t60 of exp(-t/tau)", f["t60_ms"], 1000.0 * tau * math.log(1000.0), 1.0)

    # White noise with a known decay: each band holds its share of the 20 Hz - 20 kHz width.
    w = rng.standard_normal(n) * np.exp(-t / 0.080)
    f, c = T.fingerprint(w), T.counter_fingerprint(w)
    grid = np.linspace(20.0, 20000.0, 400001)
    for i in (5, 6, 7, 8):
        # The share of a band is the area under its squared mask, written here a second time.
        lo, hi = T.OCT_EDGES[i], T.OCT_EDGES[i + 1]
        up = np.clip(np.log2(grid / lo) / 0.5 + 0.5, 0.0, 1.0)
        down = np.clip(np.log2(hi / grid) / 0.5 + 0.5, 0.0, 1.0) if i < 8 else np.ones(len(grid))
        share = 10.0 * math.log10(float(np.mean((np.sin(0.5 * math.pi * up) * np.sin(0.5 * math.pi * down)) ** 2)))
        check("measure: white noise, octave band %d" % i, f[T.ENV_KEYS[i]], share, 0.7)
        check("counter: white noise, octave band %d" % i, c[T.ENV_KEYS[i]], share, 0.7)
    check("white noise: centroid", f["centroid_hz"], 10010.0, 300.0)
    check("white noise: hf share", f["hf_db"], 10.0 * math.log10(10000.0 / 19980.0), 0.5)
    check("white noise: t20 of the high band", f["t20_high_ms"], 80.0 * math.log(10.0), 8.0)

    # Bright attack, dark tail: the two centroids must tell them apart.
    y = np.sin(2 * math.pi * 200.0 * t) * np.exp(-t / 0.150) + 2.0 * np.sin(2 * math.pi * 6000.0 * t) * np.exp(-t / 0.004)
    f, c = T.fingerprint(y), T.counter_fingerprint(y)
    check("tail centroid of a 200 Hz body", f["centroid_tail_hz"], 200.0, 10.0)
    check("counter: tail centroid", c["centroid_tail_hz"], 200.0, 10.0)
    check("attack centroid is far above the tail", float(f["centroid_att_hz"] > 3000.0), 1.0, 0.0)
    check("counter: attack centroid within 5 %", c["centroid_att_hz"] / f["centroid_att_hz"], 1.0, 0.05)

    # A sound that starts at full level on its first sample: the band filter must not smear
    # it around the buffer (the first version of the measure did, and read 1000 ms here).
    z = np.cos(2 * math.pi * 100.0 * t) * np.exp(-t / 0.080) + np.cos(2 * math.pi * 1000.0 * t) * np.exp(-t / 0.020)
    for name, f in (("measure", T.fingerprint(z)), ("counter", T.counter_fingerprint(z))):
        check("%s: abrupt start, t20 of the low band" % name, f["t20_low_ms"], 80.0 * math.log(10.0), 3.0)
        check("%s: abrupt start, t20 of the mid band" % name, f["t20_mid_ms"], 20.0 * math.log(10.0), 1.5)

    # A body that sits on the 200 Hz edge (it sweeps from 280 to 185 Hz): the two roads must
    # still agree. With hard edges they did not (one sound in two disagreed on the mid band).
    hz = 185.0 + 95.0 * 0.5 ** (t * 1000.0 / 6.0)
    e = np.sin(2 * math.pi * np.cumsum(hz) / SR) * np.exp(-t / 0.055)
    f, c = T.fingerprint(e), T.counter_fingerprint(e)
    for k in ("t20_low_ms", "t20_mid_ms"):
        check("body on the 200 Hz edge: %s, counter / measure" % k, c[k] / f[k], 1.0, 0.03)
    for k in ("env1_db", "env2_db"):
        check("body on the 200 Hz edge: %s, counter - measure" % k, c[k] - f[k], 0.0, 0.3)

    # Null test: a sound against itself cancels, against another throw of the dice it does not.
    a = T.rich({"noise": [(2000.0, 9000.0, 1.0, 60.0)]}, n, np.random.default_rng(1))
    b = T.rich({"noise": [(2000.0, 9000.0, 1.0, 60.0)]}, n, np.random.default_rng(2))
    check("null test, a sound against itself delayed by 1 ms", float(T.null_db(a, np.roll(a, 48)) < -40.0), 1.0, 0.0)
    check("null test, same recipe with other dice", T.null_db(a, b), 0.0, 0.5)
    check("band-time map, same recipe with other dice (dB)", float(T.map_distance(T.band_time_map(a), T.band_time_map(b)) < 3.0), 1.0, 0.0)

    # The note of a kick whose answer is known: it drops from 220 Hz to 55 Hz in a few
    # milliseconds, and a click and some hiss must not move the reading.
    hz = 55.0 + 165.0 * 0.5 ** (t * 1000.0 / 8.0)
    k = np.sin(2 * math.pi * np.cumsum(hz) / SR) * np.exp(-t / 0.090)
    k += 0.3 * rng.standard_normal(n) * np.exp(-t / 0.002) + 0.003 * rng.standard_normal(n)
    check("note of a kick that lands on 55 Hz", T.note_hz(k), 55.0, 0.5)
    check("counter: note of the same kick", T.counter_note_hz(k), 55.0, 0.5)
    check("the note that counts, same kick", T.note(k), 55.0, 0.5)
    # Tails of hardly more than one period: one road alone may still read something (and be wrong),
    # the two roads together never give a note.
    for hz0, tau in ((45.0, 0.008), (45.0, 0.012), (45.0, 0.016), (60.0, 0.010), (40.0, 0.015)):
        short = np.sin(2 * math.pi * hz0 * t) * np.exp(-t / tau)
        check("no note for %.0f Hz that dies in %.0f ms" % (hz0, 1000.0 * tau), float(math.isnan(T.note(short))), 1.0, 0.0)

    # ESPRIT finds two known modes.
    m = 2048
    tt = np.arange(m) / SR
    z = np.sin(2 * math.pi * 440.0 * tt) * np.exp(-tt / 0.030) + 0.5 * np.sin(2 * math.pi * 1234.0 * tt) * np.exp(-tt / 0.010)
    hz, decay, _, _ = T.esprit(z, 4, SR)
    i = int(np.argmin(np.abs(hz - 440.0)))
    j = int(np.argmin(np.abs(hz - 1234.0)))
    check("esprit: first mode, Hz", hz[i], 440.0, 0.05)
    check("esprit: first mode, decay time ms", 1000.0 / decay[i], 30.0, 0.1)
    check("esprit: second mode, Hz", hz[j], 1234.0, 0.05)
    check("esprit: second mode, decay time ms", 1000.0 / decay[j], 10.0, 0.1)

    # CMA-ES finds the bottom of a tilted bowl.
    goal = np.linspace(0.2, 0.8, 8)
    best, val = T.cma(lambda xs: np.sum(((xs - goal) * np.arange(1, 9)) ** 2, axis=1), np.full(8, 0.5), 0.2, 120, 12,
                      np.random.default_rng(3))
    check("cma: distance to the known optimum", float(np.max(np.abs(best - goal))), 0.0, 1e-3)

    print("self-test: %s" % ("FAILED: " + ", ".join(FAILED) if FAILED else "all green"))
    return 1 if FAILED else 0


if __name__ == "__main__":
    sys.exit(main())
