"""Family "DNA" targets and the distance of a sound to them.

The targets are the per-family percentiles of essentia-familles.tsv
(branch eq/son-fin-essentia-01). The measures are taken with the very same
function as the base (scripts/son/essentia/base.py, ess_measure), so the
2048 / 512 / 44.1 kHz settings match. base.py is loaded from the path given
in the PS_ESSENTIA_BASE environment variable.

Distance: for each timbre measure, a robust z-score against the family,
asymmetric (the p25..p50 and p50..p75 half-ranges each stand for 0.674 sd),
capped at 4; the score is the mean |z| (0 = on the family median).
Level, length, tempo and key are left out: the producer sets them.
"""
import csv
import importlib.util
import io
import os

import numpy as np

MEASURES = ["centroid_hz", "rolloff_hz", "flatness_db", "zcr", "low_share_db",
            "mid_share_db", "high_share_db", "log_attack", "effective_dur_s",
            "mfcc1", "mfcc2", "mfcc3", "mfcc4", "mfcc5"]
# Counter-measure: the five spectrum and level measures librosa also takes.
LIB_MEASURES = ["centroid_hz", "rolloff_hz", "flatness_db", "zcr"]
ZCAP = 4.0
SR = 44100

_base = None


def base():
    global _base
    if _base is None:
        path = os.environ.get("PS_ESSENTIA_BASE")
        if not path or not os.path.exists(path):
            raise SystemExit("PS_ESSENTIA_BASE must point to scripts/son/essentia/base.py")
        spec = importlib.util.spec_from_file_location("ps_essentia_base", path)
        _base = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(_base)
    return _base


def load_targets(tsv):
    t = {}
    with open(tsv, newline="") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            try:
                t.setdefault(r["famille"], {})[r["mesure"]] = {
                    k: float(r[k]) for k in ("p10", "p25", "p50", "p75", "p90")}
            except (ValueError, KeyError):
                pass
    return t


def zscore(x, p):
    if x is None or not np.isfinite(x):
        return ZCAP
    if x >= p["p50"]:
        s = (p["p75"] - p["p50"]) / 0.674
    else:
        s = (p["p50"] - p["p25"]) / 0.674
    s = max(s, 1e-9, 0.02 * abs(p["p50"]))
    return float(np.clip((x - p["p50"]) / s, -ZCAP, ZCAP))


def distance(meas, fam_t, keys=MEASURES):
    z = {k: zscore(meas.get(k), fam_t[k]) for k in keys if k in fam_t}
    return float(np.mean([abs(v) for v in z.values()])), z


# Measurement type: same branches of ess_measure as the family's own type,
# without the (slow, unused) tempo and key extraction of loops.
MTYPE = {"melodic_loop": "phrase", "kit_stem": "phrase", "fx": "fx", "vocal": "vocal"}


def measure(stereo_44k, fam):
    """stereo_44k: float32 array (n, 2) at 44.1 kHz."""
    typ = MTYPE.get(fam.split("/")[0], fam.split("/")[0])
    return base().ess_measure(np.asarray(stereo_44k, dtype=np.float32), typ, fam)


def lib_measure_wav(wav_bytes, fam):
    typ = MTYPE.get(fam.split("/")[0], fam.split("/")[0])
    return base().lib_measure(wav_bytes, typ, fam)
