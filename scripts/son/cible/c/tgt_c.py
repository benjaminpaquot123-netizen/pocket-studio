"""Zone C targets: per (family, style) percentiles from
docs/sons/fin/cibles/zone-c-synthes.tsv, measured with the same code that built
the table (scripts/son/essentia/cibles_desc.py).

Each descriptor is aimed at a percentile of the stock (p50 by default, or the
"lean" percentile taken from what Benjamin kept, see zone-c-synthes.md), with
sd = IQR / 1.349 (floored). Score = weighted RMS of the clipped z-scores.
"""
import csv
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", "..", ".."))
sys.path.insert(0, os.path.join(REPO, "scripts", "son", "essentia"))
TSV = os.path.join(REPO, "docs", "sons", "fin", "cibles", "zone-c-synthes.tsv")

# page style -> stock style (zone-c-synthes.md); missing cells fall back to "*"
STOCK_STYLE = {"dnb": "dnb", "techno": "techno", "house": "house", "lo-fi": "lofi",
               "trap": "trap", "drill": "trap", "boom-bap": "hiphop"}
# our family -> stock family
FAM_TGT = {"stab": "stab", "pluck": "pluck", "nappe": "nappe", "accords": "accords",
           "boucle": "boucle-lead", "arpege": "boucle-synth", "ambiance": "fx-nappe",
           "effet": "fx-effet"}
DESC = ["attack_ms", "decay20_s", "centroid_hz", "high4k", "low300", "dissonance",
        "n_peaks", "width_db", "flatness_db"]
LOG = {"attack_ms", "decay20_s", "centroid_hz"}
FLOOR = {"attack_ms": 0.25, "decay20_s": 0.15, "centroid_hz": 0.12, "high4k": 0.006,
         "low300": 0.04, "dissonance": 0.012, "n_peaks": 3.0, "width_db": 1.5,
         "flatness_db": 2.0, "low_beat": 0.05, "lufs": 0.5}
# What Benjamin kept, as target percentiles (zone-c-synthes.md, "gardé" vs "non").
LEAN = {
    "stab": {"decay20_s": 0.85, "low300": 0.2},
    "pluck": {"centroid_hz": 0.85, "high4k": 0.85, "n_peaks": 0.75},
    "accords": {"decay20_s": 0.5, "low300": 0.4},
    "boucle": {"low300": 0.35, "dissonance": 0.75, "n_peaks": 0.7},
    "arpege": {"attack_ms": 0.2, "decay20_s": 0.6},
    "nappe": {"attack_ms": 0.3, "decay20_s": 0.7, "dissonance": 0.5},
    "ambiance": {},
    "effet": {"low300": 0.3, "low_beat": 0.75, "lufs": 0.85, "decay20_s": 0.65},
}
EXTRA = {"effet": ["low_beat"]}
# Loops: the loudest note sits anywhere in the bar, so attack is a weak cue.
WEIGHT = {"attack_ms": 0.5}
QS = [0.10, 0.25, 0.50, 0.75, 0.90]

_ROWS = None


def _rows():
    global _ROWS
    if _ROWS is None:
        _ROWS = {}
        with open(TSV) as f:
            for r in csv.DictReader(f, delimiter="\t"):
                _ROWS[(r["famille"], r["style"], r["descripteur"])] = r
    return _ROWS


def _q(r, q, log):
    ps = [float(r[k]) for k in ("p10", "p25", "p50", "p75", "p90")]
    if log:
        ps = [math.log(max(p, 1e-3)) for p in ps]
    return float(np.interp(q, QS, ps)), ps


def cell(family, style):
    fam = FAM_TGT[family]
    st = STOCK_STYLE.get(style, "*")
    rows = _rows()
    if (fam, st, "centroid_hz") not in rows:
        st = "*"
    return fam, st


def target(family, style):
    fam, st = cell(family, style)
    rows = _rows()
    t = {"_cell": f"{fam}/{st}", "_n": int(rows[(fam, st, "centroid_hz")]["sons"])}
    for d in DESC + EXTRA.get(family, []) + ["lufs"]:
        r = rows[(fam, st, d)]
        log = d in LOG
        c, ps = _q(r, LEAN.get(family, {}).get(d, 0.5), log)
        sd = max((ps[3] - ps[1]) / 1.349, FLOOR.get(d, 1e-6))
        t[d] = (c, sd, WEIGHT.get(d, 1.0))
    return t


def measure(y):
    """y: float stereo (n, 2) at 44.1 kHz."""
    import cibles_desc
    st = np.asarray(y, dtype=np.float32)
    if st.ndim == 1:
        st = st[:, None]
    return cibles_desc.measure(st)


def zscores(meas, t):
    z = {}
    for d, v in t.items():
        if d.startswith("_") or d == "lufs":
            continue
        x = meas.get(d)
        if x is None or not np.isfinite(x):
            z[d] = 10.0
            continue
        if d in LOG:
            x = math.log(max(x, 1e-3))
        z[d] = (x - v[0]) / v[1]
    return z


def score(meas, t):
    z = zscores(meas, t)
    num = sum(t[d][2] * min(abs(v), 10.0) ** 2 for d, v in z.items())
    den = sum(t[d][2] for d in z)
    return float(math.sqrt(num / den))


def master_lufs(t):
    return t["lufs"][0] - 0.4
