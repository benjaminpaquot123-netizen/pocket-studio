"""Family DNA (Essentia medians from essentia-familles.tsv) and the distance of
a new sound to it, in robust standard deviations (0 = the family median).

The measure is the very function that built the DNA (scripts/son/essentia/
base.py: ess_measure), so a new sound and a pack sound are measured the same
way (mono, 44.1 kHz, same frames, same gate).
"""
import csv
import importlib.util
import io
import math
import os

import numpy as np
import soundfile as sf

MEASURES = ["rms_db", "lufs", "centroid_hz", "rolloff_hz", "flatness_db", "zcr",
            "low_share_db", "mid_share_db", "high_share_db", "log_attack",
            "effective_dur_s", "mfcc1", "mfcc2", "mfcc3", "mfcc4", "mfcc5"]
# librosa counter-measure: same definitions, independent framing (base.py tolerances)
LIB_TOL = {"centroid_hz": (0.05, 25), "rolloff_hz": (0.05, 25), "zcr": (0.05, 0.002),
           "flatness_db": (None, 1.0), "rms_db": (None, 0.5)}


def load_base(path):
    spec = importlib.util.spec_from_file_location("essentia_base", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def load_dna(tsv):
    dna = {}
    with open(tsv, newline="") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            try:
                q = [float(r[k]) for k in ("p10", "p25", "p50", "p75", "p90")]
            except (ValueError, KeyError):
                continue
            sd = (q[3] - q[1]) / 1.349
            if sd <= 1e-9:
                sd = (q[4] - q[0]) / 2.563
            if sd <= 1e-9:
                sd = max(abs(q[2]) * 0.05, 1e-6)
            dna.setdefault(r["famille"], {})[r["mesure"]] = {"med": q[2], "sd": sd, "p10": q[0], "p90": q[4]}
    return dna


class Judge:
    def __init__(self, base_py, tsv):
        self.base = load_base(base_py)
        self.dna = load_dna(tsv)

    def measure(self, y, typ, fam):
        st = np.repeat(np.asarray(y, dtype=np.float32)[:, None], 2, 1)
        return self.base.ess_measure(st, typ, fam)

    def zs(self, m, fam):
        d = self.dna[fam]
        return {k: (m[k] - d[k]["med"]) / d[k]["sd"] for k in MEASURES
                if k in d and m.get(k) is not None}

    def score(self, z, clip=None):
        v = np.array(list(z.values()))
        if clip:
            v = np.clip(v, -clip, clip)
        return float(math.sqrt(np.mean(v ** 2))) if len(v) else 99.0

    def in_range(self, m, fam):
        d = self.dna[fam]
        ks = [k for k in MEASURES if k in d and m.get(k) is not None]
        return sum(d[k]["p10"] <= m[k] <= d[k]["p90"] for k in ks), len(ks)

    def librosa_check(self, y, typ, fam, ess):
        buf = io.BytesIO()
        sf.write(buf, np.asarray(y, dtype=np.float32), 44100, format="WAV", subtype="FLOAT")
        lib = self.base.lib_measure(buf.getvalue(), typ, fam)
        ok = n = 0
        for k, (rel, ab) in LIB_TOL.items():
            a, b = ess.get(k), lib.get(k)
            if a is None or b is None:
                continue
            n += 1
            tol = ab if rel is None else max(rel * abs(a), ab)
            if k == "rolloff_hz":
                tol = max(tol, lib.get("rolloff_res_hz", 0), ess.get("rolloff_res_hz", 0))
            ok += abs(a - b) <= tol
        return ok, n
