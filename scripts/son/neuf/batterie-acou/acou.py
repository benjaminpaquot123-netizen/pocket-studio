"""New acoustic drum one-shots shaped toward a family "DNA" (Essentia stats).

Source: Virtuosity Drums (CC0), multi-mic recordings. A hit is a mix of the
six mic positions (kick mic, snare mic, overheads, room, mid, lo-fi), tuned,
then a pedalboard chain (filters, EQ, compressor, parallel drive, optional
bit crush) and a length/fade. pycma tunes the mix and the chain so the Essentia
measures (same code as scripts/son/essentia/base.py) land on the family median.

Distance: for each measure, z = (x - p50) / s where s = (p90 - p50) / 1.2816
above the median and (p50 - p10) / 1.2816 below (asymmetric families stay
fair); score = RMS of z over the measures both sides have. 0 = the median.

Run with the scratchpad venv, always with -I. Audio never enters git.
"""
import csv
import glob
import math
import os
import sys

import numpy as np

SR = 44100
MICS = ("kickmic", "snaremic", "oh", "room", "mid", "lofi")
MEASURES = ("dur_s", "rms_db", "lufs", "centroid_hz", "rolloff_hz", "flatness_db", "zcr",
            "low_share_db", "mid_share_db", "high_share_db", "log_attack", "effective_dur_s",
            "mfcc1", "mfcc2", "mfcc3", "mfcc4", "mfcc5")
LOOP_MEASURES = tuple(m for m in MEASURES if m != "dur_s")

_BASE = None


def base():
    """The Essentia measurement module of the DNA (base.py, path in PS_BASE)."""
    global _BASE
    if _BASE is None:
        import importlib.util
        p = os.environ["PS_BASE"]
        spec = importlib.util.spec_from_file_location("ps_base", p)
        _BASE = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(_BASE)
    return _BASE


# ------------------------------------------------------------------ DNA
def load_dna(path):
    dna = {}
    with open(path, newline="") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            try:
                q = tuple(float(r[k]) for k in ("p10", "p50", "p90"))
            except (ValueError, KeyError):
                continue
            dna.setdefault(r["famille"], {})[r["mesure"]] = q
    return dna


def zscores(meas, target, keys=MEASURES):
    z = {}
    for k in keys:
        if k not in target or meas.get(k) is None:
            continue
        p10, p50, p90 = target[k]
        x = float(meas[k])
        s = (p90 - p50) if x >= p50 else (p50 - p10)
        s = max(abs(s) / 1.2816, 1e-9)
        z[k] = (x - p50) / s
    return z


def score(z):
    return math.sqrt(sum(v * v for v in z.values()) / len(z)) if z else 99.0


def measure(stereo, typ="drum_oneshot", fam=""):
    return base().ess_measure(np.asarray(stereo, dtype=np.float32), typ, fam)


# ------------------------------------------------------------------ sources
class Bank:
    """Virtuosity Drums sample index: key = '<inst>_<art>_vl<v>[_rr<r>]'."""

    def __init__(self, root, cache):
        self.root, self.cache = root, cache
        os.makedirs(cache, exist_ok=True)
        self.keys = {}
        for p in glob.glob(os.path.join(root, "oh", "*", "*.flac")):
            inst = os.path.basename(os.path.dirname(p))
            key = os.path.basename(p)[3:-5]
            self.keys[key] = inst

    def find(self, inst, art, vmin=1):
        out = []
        for k, i in self.keys.items():
            if i != inst:
                continue
            rest = k[len(inst) + 1:]
            a, _, tail = rest.partition("_vl")
            if a != art:
                continue
            v = int(tail.split("_")[0])
            if v >= vmin:
                out.append((v, k))
        return [k for _, k in sorted(out)]

    def mics(self, key):
        """(6, n, 2) float32 at 44.1 kHz, cached as .npy."""
        c = os.path.join(self.cache, key + ".npy")
        if os.path.exists(c):
            return np.load(c)
        import soundfile as sf
        inst = self.keys[key]
        arrs = []
        for m in MICS:
            x, sr = sf.read(os.path.join(self.root, m, inst, "%s_%s.flac" % (m, key)),
                            dtype="float32", always_2d=True)
            if x.shape[1] == 1:
                x = np.repeat(x, 2, 1)
            arrs.append(base().resample(x[:, :2], sr))
        n = max(len(a) for a in arrs)
        out = np.zeros((len(MICS), n, 2), np.float32)
        for i, a in enumerate(arrs):
            out[i, :len(a)] = a
        np.save(c, out)
        return out


# ------------------------------------------------------------------ chain
# Parameter space: name -> (low, high, kind). kind "log" maps on a log scale.
PARAMS = [
    ("g_kickmic", -40, 6, "lin"), ("g_snaremic", -40, 6, "lin"), ("g_oh", -40, 6, "lin"),
    ("g_room", -40, 6, "lin"), ("g_mid", -40, 6, "lin"), ("g_lofi", -60, 0, "lin"),
    ("tune", -3, 3, "lin"),
    ("hpf", 20, 400, "log"), ("lowshelf", -12, 12, "lin"),
    ("pk_f", 120, 8000, "log"), ("pk_g", -12, 12, "lin"),
    ("highshelf", -12, 12, "lin"), ("lpf", 2500, 20000, "log"),
    ("thr", -40, 0, "lin"), ("ratio", 1, 10, "lin"), ("att_ms", 0.3, 40, "log"),
    ("drive", 0, 30, "lin"), ("drive_mix", 0, 1, "lin"),
    ("crush", 16, 16, "lin"), ("lofi_sr", 44100, 44100, "log"),
    ("length", 0.2, 1.0, "lin"), ("fade", 0.5, 4, "log"), ("decay_ms", 3, 3000, "log"),
]
NAMES = [p[0] for p in PARAMS]


def bounds_for(overrides):
    b = {p[0]: (p[1], p[2], p[3]) for p in PARAMS}
    for k, v in overrides.items():
        b[k] = (v[0], v[1], b[k][2])
    return b


def decode(u, b):
    """u in [0, 1]^n -> parameter dict."""
    out = {}
    for i, n in enumerate(NAMES):
        lo, hi, kind = b[n]
        t = float(np.clip(u[i], 0, 1))
        out[n] = lo * (hi / lo) ** t if kind == "log" and lo > 0 and hi > lo else lo + (hi - lo) * t
    return out


def render(mics, p, peak_db=-1.0):
    import pedalboard as pb
    g = np.array([10 ** (p["g_" + m] / 20) for m in MICS], np.float32)
    x = np.tensordot(g, mics, axes=(0, 0))  # (n, 2)
    a = np.abs(x).max(1)  # trim the lead-in: start 1 ms before -30 dB of the peak
    on = int(np.argmax(a > a.max() * 0.0316)) if a.max() > 0 else 0
    x = x[max(0, on - int(0.001 * SR)):]
    if abs(p["tune"]) > 0.01:  # tape-style pitch: resample (length changes too)
        r = 2 ** (p["tune"] / 12)
        n = int(len(x) / r)
        t = np.arange(n) * r
        x = np.stack([np.interp(t, np.arange(len(x)), x[:, c]) for c in range(2)], 1).astype(np.float32)
    n = min(len(x), int(p["length"] * SR))
    x = x[:n].copy()
    fl = max(1, n - int(0.003 * SR))
    env = np.ones(n, np.float32)
    env[:fl] = (1 - np.arange(fl) / fl) ** (p["fade"] - 1) if p["fade"] > 1.001 else 1.0
    env[fl:] = np.linspace(env[fl - 1] if fl > 0 else 1, 0, n - fl)
    env *= np.exp(-np.arange(n) / (p["decay_ms"] * 0.001 * SR)).astype(np.float32) ** 0.5
    x *= env[:, None]
    m = np.abs(x).max()
    if m <= 0:
        return x
    x /= m
    board = pb.Pedalboard([
        pb.HighpassFilter(cutoff_frequency_hz=p["hpf"]),
        pb.LowShelfFilter(cutoff_frequency_hz=120, gain_db=p["lowshelf"]),
        pb.PeakFilter(cutoff_frequency_hz=p["pk_f"], gain_db=p["pk_g"], q=1.0),
        pb.HighShelfFilter(cutoff_frequency_hz=6000, gain_db=p["highshelf"]),
        pb.LowpassFilter(cutoff_frequency_hz=p["lpf"]),
        pb.Compressor(threshold_db=p["thr"], ratio=p["ratio"], attack_ms=p["att_ms"], release_ms=80),
    ])
    y = board(x.T.copy(), SR)
    if p["drive_mix"] > 0.01 and p["drive"] > 0.1:
        d = pb.Pedalboard([pb.Distortion(drive_db=p["drive"])])(y.copy(), SR)
        d *= np.abs(y).max() / max(np.abs(d).max(), 1e-9)
        y = (1 - p["drive_mix"]) * y + p["drive_mix"] * d
    if p["crush"] < 15.9 or p["lofi_sr"] < 44000:
        fx = []
        if p["lofi_sr"] < 44000:
            fx.append(pb.Resample(target_sample_rate=p["lofi_sr"]))
        if p["crush"] < 15.9:
            fx.append(pb.Bitcrush(bit_depth=p["crush"]))
        y = pb.Pedalboard(fx)(y, SR)
    y = y.T
    y = y[:n]
    k = max(1, int(0.002 * SR))
    y[-k:] *= np.linspace(1, 0, k)[:, None]
    m = np.abs(y).max()
    if not np.isfinite(m) or m <= 0:
        return np.zeros_like(y)
    return (y * (10 ** (peak_db / 20) / m)).astype(np.float32)


def penalty(p):
    """Keep it a real drum: soft cost on extreme EQ moves."""
    return 0.002 * (p["lowshelf"] ** 2 + p["pk_g"] ** 2 + p["highshelf"] ** 2) / 3


def optimise(mics, target, b, seed, evals=300, sigma=0.3):
    import cma
    best = [99.0, None, None]

    def f(u):
        p = decode(u, b)
        y = render(mics, p)
        if not np.any(y):
            return 99.0
        try:
            z = zscores(measure(y), target)
        except Exception:
            return 99.0
        s = score(z)
        if s < best[0]:
            best[:] = [s, p, z]
        return s + penalty(p)

    es = cma.CMAEvolutionStrategy([0.5] * len(NAMES), sigma,
                                  {"bounds": [0, 1], "seed": seed, "maxfevals": evals,
                                   "verbose": -9, "popsize": 12})
    while not es.stop():
        us = es.ask()
        es.tell(us, [f(u) for u in us])
    return best
