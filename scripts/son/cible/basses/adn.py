"""Family "DNA" targets and distance for the new bass sounds.

Measures a rendered sound with the exact Essentia recipe of the sound base
(essentia_base.py, copied from scripts/son/essentia/base.py on
eq/son-fin-essentia-01) and scores it against the percentiles of its family
in essentia-familles.tsv.

Distance, in standard deviations (0 = on the family median):
  for each feature, z = (x - p50) / s, where s is the half-spread on the side
  of x: (p50 - p25) / 0.6745 below the median, (p75 - p50) / 0.6745 above,
  with a small floor per feature so a near-constant family feature does not
  explode; Hz and durations are taken in log. The sound's distance is the RMS
  of the z over the features both sides have.
"""
import csv
import io
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import essentia_base as eb  # noqa: E402

SR = eb.SR

FEATURES = ["centroid_hz", "rolloff_hz", "flatness_db", "zcr", "rms_db", "lufs",
            "low_share_db", "mid_share_db", "high_share_db", "log_attack",
            "effective_dur_s", "mfcc1", "mfcc2", "mfcc3", "mfcc4", "mfcc5", "f0_hz"]
LOG = {"centroid_hz", "rolloff_hz", "effective_dur_s", "f0_hz"}
FLOOR = {"centroid_hz": 0.05, "rolloff_hz": 0.05, "effective_dur_s": 0.05, "f0_hz": 0.03,
         "flatness_db": 1.0, "zcr": 0.0005, "rms_db": 0.5, "lufs": 0.5,
         "low_share_db": 0.5, "mid_share_db": 1.0, "high_share_db": 2.0,
         "log_attack": 0.05, "mfcc1": 5.0, "mfcc2": 5.0, "mfcc3": 3.0, "mfcc4": 3.0, "mfcc5": 3.0}

# Our families -> the measured family of the sound base and the type used to
# measure it (type decides the frame mode, exactly as for the reference stock).
FAMILY = {
    "808": ("oneshot_melodic/808", "oneshot_melodic"),
    "basse-synthe": ("oneshot_melodic/bass", "oneshot_melodic"),
    "sub": ("oneshot_melodic/bass", "oneshot_melodic"),
    "reese": ("oneshot_melodic/bass", "oneshot_melodic"),
    "basse-acoustique": ("oneshot_melodic/bass", "oneshot_melodic"),
    "phrase": ("melodic_loop/bass", "melodic_loop"),
}

_orig_pitched = eb.pitched


def _pitched(typ, fam):  # f0 also on 808 hits (the family table has it)
    return fam.endswith("kick_808") or _orig_pitched(typ, fam)


eb.pitched = _pitched


def load_targets(path):
    t = {}
    with open(path) as f:
        r = csv.DictReader(f, delimiter="\t")
        for row in r:
            def num(k):
                try:
                    return float(row[k])
                except (TypeError, ValueError):
                    return None
            t.setdefault(row["famille"], {})[row["mesure"]] = {k: num(k) for k in ("p10", "p25", "p50", "p75", "p90")}
    return t


def _tr(k, v):
    if k in LOG:
        return math.log(max(v, 1e-6))
    return v


def zscores(m, fam_targets):
    z = {}
    for k in FEATURES:
        p = fam_targets.get(k)
        if p is None or m.get(k) is None or p["p50"] is None:
            continue
        x, med = _tr(k, m[k]), _tr(k, p["p50"])
        lo = (med - _tr(k, p["p25"])) / 0.6745
        hi = (_tr(k, p["p75"]) - med) / 0.6745
        s = max(lo if x < med else hi, FLOOR[k])
        z[k] = (x - med) / s
    return z


def distance(m, fam_targets, skip=()):
    z = {k: v for k, v in zscores(m, fam_targets).items() if k not in skip}
    if not z:
        return float("inf"), z
    return math.sqrt(sum(v * v for v in z.values()) / len(z)), z


def measure(stereo, family):
    """stereo: float32 array (n, 2) at 44.1 kHz."""
    fam, typ = FAMILY[family]
    st = np.ascontiguousarray(np.asarray(stereo, dtype=np.float32))
    if st.ndim == 1:
        st = np.stack([st, st], 1)
    m = eb.ess_measure(st, typ, fam)
    m.pop("_f0", None)
    return m


def counter_measure(stereo, family):
    """librosa counter-measure on the same audio (encoded as WAV bytes)."""
    import soundfile as sf
    fam, typ = FAMILY[family]
    buf = io.BytesIO()
    sf.write(buf, np.asarray(stereo, dtype=np.float32), SR, format="WAV", subtype="FLOAT")
    m = eb.lib_measure(buf.getvalue(), typ, fam)
    m.pop("_f0", None)
    return m


def agreement(ess, lib):
    keys = ("centroid_hz", "rolloff_hz", "flatness_db", "zcr", "rms_db")
    ok, n = 0, 0
    for k in keys:
        if ess.get(k) is None or lib.get(k) is None:
            continue
        n += 1
        ok += bool(eb.agree(k, ess[k], lib[k]))
    return ok, n
