#!/usr/bin/env python3
"""Timbre fingerprint on the real collection (docs/sons/fin/RECHERCHE-TIMBRE.md, step 0).

measure(x): the fingerprint of timbre.py plus t40_ms.
counter(x): the same 20 numbers by roads that share nothing with the measure: its own onset
(0.5 ms RMS blocks, not the first loud sample), its own decay (energy left after 1 ms blocks,
summed forward and read between blocks, not Schroeder per sample), time-domain filters for
the bands, a |frequency| kernel for the centroids, a hard 10-20 kHz filter for hf.

Numpy only. Run with `python3 -I`. Nothing measured on one bought sound is ever written to the
repository: the per-sound files go to a folder outside it, only family statistics come back.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import timbre as T  # noqa: E402

SR = T.SR
ALL_KEYS = T.KEYS + ("t40_ms",)
TIME_KEYS = ("t20_ms", "t40_ms", "t60_ms", "t20_low_ms", "t20_mid_ms", "t20_high_ms")
CENTROID_KEYS = ("centroid_hz", "centroid_att_hz", "centroid_tail_hz")


# --- Measure.

def measure(x):
    x = np.asarray(x, dtype=np.float64)
    out = T.fingerprint(x)
    onset = T.onset_of(x)
    out["t40_ms"] = 1000.0 * (T.decay_index(x * x, onset, 40) - onset) / SR
    return out


# --- Counter-measure.

ONSET_BLOCK = SR // 2000  # 0.5 ms
DECAY_BLOCK = SR // 1000  # 1 ms


def fftconv(a, b):
    n = len(a) + len(b) - 1
    m = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(a, m) * np.fft.rfft(b, m), m)[:n]


def fir_band(x, key, mask):
    """Band of x by the truncated linear-phase filter of timbre.py, delay removed."""
    h = T._fir(key, mask)
    d = (len(h) - 1) // 2
    return fftconv(x, h)[d:d + len(x)]


def counter_onset(x):
    """Start of the first 0.5 ms block whose RMS is within 30 dB of the loudest block."""
    x = np.asarray(x, dtype=np.float64)
    nb = len(x) // ONSET_BLOCK
    if nb == 0:
        raise ValueError("sound too short")
    e = (x[:nb * ONSET_BLOCK] ** 2).reshape(nb, ONSET_BLOCK).mean(axis=1)
    if e.max() <= 0:
        raise ValueError("silent sound")
    return int(np.argmax(e >= e.max() * 10.0 ** (T.ONSET_DB / 10.0))) * ONSET_BLOCK


def counter_decay_ms(power, onset, level_db):
    """Time after `onset` when the energy still to come falls `level_db` under the whole:
    1 ms blocks summed forward, read between two block edges (in dB, or in energy when the
    energy left reaches zero). If it never falls that far, the time to the end."""
    p = np.asarray(power[onset:], dtype=np.float64)
    nb = -(-len(p) // DECAY_BLOCK)
    blocks = np.add.reduceat(p, np.arange(0, nb * DECAY_BLOCK, DECAY_BLOCK)[:nb])
    total = float(blocks.sum())
    if total <= 0:
        return float("nan")
    left = np.maximum(total - np.concatenate([[0.0], np.cumsum(blocks)]), 0.0)
    goal = total * 10.0 ** (-level_db / 10.0)
    below = np.nonzero(left < goal)[0]
    if not len(below):
        return 1000.0 * len(p) / SR
    k = int(below[0])
    a, b = left[k - 1], left[k]
    if a > 0 and b > 0:
        frac = (math.log10(a) - math.log10(goal)) / (math.log10(a) - math.log10(b))
    else:
        frac = (a - goal) / (a - b)
    ms_per_block = 1000.0 * DECAY_BLOCK / SR
    return min((k - 1 + frac) * ms_per_block, 1000.0 * len(p) / SR)


def _hard_hf(f):
    return ((f >= 10000.0) & (f <= 20000.0)).astype(float)


def _kernel_centroid(s, taper):
    if len(s) < 16:
        return float("nan")
    y = fftconv(T._taper(s, taper), T._LOWPASS_20K)
    d = (len(T._ABS_OMEGA) - 1) // 2
    ky = fftconv(y, T._ABS_OMEGA)[d:d + len(y)]
    yy = float((y * y).sum())
    return float((y * ky).sum() / yy * SR / (2.0 * math.pi)) if yy > 0 else float("nan")


def counter(x):
    x = np.asarray(x, dtype=np.float64)
    out = {k: float("nan") for k in ALL_KEYS}
    onset = counter_onset(x)
    power = x * x
    for lvl in (20, 40, 60):
        out["t%d_ms" % lvl] = counter_decay_ms(power, onset, lvl)
    cut = onset + int(round(0.010 * SR))
    e_t, e_b = float(power[onset:cut].sum()), float(power[cut:].sum())
    if e_t > 0 and e_b > 0:
        out["transient_body_db"] = 10.0 * math.log10(e_t / e_b)

    i60 = onset + int(round(out["t60_ms"] * SR / 1000.0))
    a = max(0, onset - int(0.001 * SR))
    seg = x[a:max(i60, a + 16)]
    pad = np.concatenate([np.zeros(4096), seg, np.zeros(4096)])
    e = [float((fir_band(pad, ("env", i), lambda f, i=i: T._env_mask(f, i)) ** 2).sum())
         for i in range(len(T.OCT_EDGES) - 1)]
    tot = sum(e)
    if tot > 0:
        for i, v in enumerate(e):
            out[T.ENV_KEYS[i]] = max(10.0 * math.log10(max(v / tot, 1e-30)), T.FLOOR_DB)
        hf = float((fir_band(pad, "hardhf", _hard_hf) ** 2).sum())
        out["hf_db"] = max(10.0 * math.log10(max(hf / tot, 1e-30)), -80.0)

    whole = float(power[onset:].sum())
    for g, name in enumerate(("low", "mid", "high")):
        p = fir_band(x, ("group", g), lambda f, g=g: T._group_mask(f, g)) ** 2
        if p[onset:].sum() > whole * 10.0 ** (-30.0 / 10.0):
            out["t20_%s_ms" % name] = counter_decay_ms(p, onset, 20)

    out["centroid_hz"] = _kernel_centroid(seg, None)
    out["centroid_att_hz"] = _kernel_centroid(x[a:onset + int(0.020 * SR)], "out")
    tail = x[onset + int(0.040 * SR):i60]
    if len(tail) >= int(0.010 * SR):
        out["centroid_tail_hz"] = _kernel_centroid(tail, "both")
    return out


# --- Agreement between the two roads.

def agree(k, a, b):
    if math.isnan(a) and math.isnan(b):
        return True
    if math.isnan(a) or math.isnan(b):
        return False
    if k in TIME_KEYS:
        return abs(a - b) <= 5.0 or abs(a - b) <= 0.10 * abs(a)
    if k in CENTROID_KEYS:
        return abs(a - b) <= 0.10 * abs(a)
    if k == "hf_db":
        return abs(a - b) <= 1.5
    if k in T.ENV_KEYS and a <= T.FLOOR_DB + 5.0 and b <= T.FLOOR_DB + 5.0:
        return True
    return abs(a - b) <= 1.0  # transient_body_db and the octave bands


def counted(k, a, b):
    """A pair counts unless both roads find nothing (no value, or both bands empty)."""
    if math.isnan(a) and math.isnan(b):
        return False
    if k in T.ENV_KEYS and a <= T.FLOOR_DB + 5.0 and b <= T.FLOOR_DB + 5.0:
        return False
    return True


def agreement(av, bv, k):
    n = ok = 0
    for a, b in zip(av, bv):
        if counted(k, a, b):
            n += 1
            ok += agree(k, a, b)
    return n, ok


# --- Reading.

def to_48k(y, sr):
    y = np.asarray(y, dtype=np.float64)
    if sr == SR:
        return y.copy()
    pad = 4096
    yp = np.concatenate([np.zeros(pad), y, np.zeros(pad)])
    n_out = int(round(len(yp) * SR / sr))
    z = np.fft.irfft(np.fft.rfft(yp), n_out) * (n_out / len(yp))
    a = int(round(pad * SR / sr))
    return z[a:a + int(round(len(y) * SR / sr))]


# --- Copy guard.

def map_threshold(nearest):
    """A copy is closer to its sound than 95 % of the family's sounds are to their nearest
    neighbour: the 5th centile, never the minimum (one twin pair would set it to zero)."""
    return T.quantile(list(nearest), 0.05)


def fine_map(x, bands, slices, n=1024, hop=256):
    """Like timbre.band_time_map with a finer grid: `bands` log bands from 20 Hz to 20 kHz
    over `slices` geometric slices of the first second, dB under the whole, clamped."""
    x = np.asarray(x, dtype=np.float64)
    x = x[T.onset_of(x):][:SR]
    xp = np.concatenate([x, np.zeros(n)])
    fr = np.lib.stride_tricks.sliding_window_view(xp, n)[::hop] * np.hanning(n)
    power = np.abs(np.fft.rfft(fr, axis=1)) ** 2
    f = np.fft.rfftfreq(n, 1.0 / SR)
    fe = np.searchsorted(f, np.geomspace(20.0, 20000.0, bands + 1))
    te = np.unique(np.round(np.geomspace(1, len(power), slices + 1)).astype(int))
    te[0] = 0
    total = float(power.sum())
    cells = np.zeros((len(te) - 1, bands))
    for i, (ta, tb) in enumerate(zip(te[:-1], te[1:])):
        col = power[ta:tb].sum(axis=0)
        for j, (fa, fb) in enumerate(zip(fe[:-1], fe[1:])):
            cells[i, j] = col[fa:max(fb, fa + 1)].sum()
    return np.maximum(10.0 * np.log10(np.maximum(cells / total, 1e-30)), -70.0)


DETECTORS = {
    "carte 25x9": T.band_time_map,
    "carte 64x24": lambda x: fine_map(x, 64, 24),
    "carte 128x48": lambda x: fine_map(x, 128, 48),
}


def _redraw(ref, bands, slices, rng):
    sys.path.insert(0, HERE)
    import etude  # the study's own redraw, untouched
    return etude.redraw(ref, bands, slices, rng)


def copie_trial(family_seed=31, target=7):
    """One invented snare redrawn with our own noise on finer and finer grids; which guard
    catches it? A guard catches when the redraw is closer to the sound than the guard's
    threshold (5th centile of the family's nearest-neighbour distances); the null test
    catches when the redraw cancels the sound deeper than any neighbour does (p5 too)."""
    refs = T.family("snare", 30, np.random.default_rng(family_seed))
    maps = {k: [f(r) for r in refs] for k, f in DETECTORS.items()}
    thr = {k: map_threshold(min(T.map_distance(m[i], m[j]) for j in range(len(m)) if j != i)
                            for i in range(len(m))) for k, m in maps.items()}
    null_thr = map_threshold(min(T.null_db(refs[i], refs[j]) for j in range(len(refs)) if j != i)
                             for i in range(len(refs)))
    ref = refs[target]

    def caught(y):
        out = {k: T.map_distance(DETECTORS[k](y), maps[k][target]) < thr[k] for k in DETECTORS}
        out["nul"] = T.null_db(ref, y) < null_thr
        return out

    rows = []
    for bands, slices in ((9, 6), (9, 12), (25, 12), (25, 24), (64, 48), (128, 96), (513, 100000)):
        y, nb, ns = _redraw(ref, bands, slices, np.random.default_rng(5))
        rows.append(("%d x %d" % (nb, ns), caught(y)))
    y = ref * 1.0
    y[1:] += 0.02 * y[:-1]
    rows.append(("le son lui-même", caught(y)))
    return rows
