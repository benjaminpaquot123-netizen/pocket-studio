#!/usr/bin/env python3
"""Self-test of mesure.py on synthetic signals with known answers.

The same signals also go through scripts/sons/tout.py (read only, functions
called as they are) and each miss of either tool is printed. Exit code 1 when
mesure.py misses a truth; tout.py misses are reported, not fatal.

  python3 -I scripts/sons/passe2/verif/selftest.py
"""
import math
import os
import struct
import sys
import tempfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesure  # noqa: E402

SR = 48000
RNG = np.random.default_rng(20261127)


def t(n_s):
    return np.arange(int(n_s * SR)) / SR


def cents(a, b):
    return abs(1200 * math.log2(a / b))


# ---------------------------------------------------------------- writers (for the reader test)

def wav_bytes(x, bits, flt=False):
    ch = x.shape[1]
    if flt:
        data = x.astype("<f4" if bits == 32 else "<f8").tobytes()
        tag = 3
    elif bits == 16:
        data = np.round(x * 32767).astype("<i2").tobytes()
        tag = 1
    elif bits == 24:
        v = np.round(x * (2 ** 23 - 1)).astype(np.int32).reshape(-1)
        b = np.stack([v & 255, (v >> 8) & 255, (v >> 16) & 255], axis=1).astype(np.uint8)
        data = b.tobytes()
        tag = 1
    else:
        raise ValueError
    fmt = struct.pack("<HHIIHH", tag, ch, SR, SR * ch * bits // 8, ch * bits // 8, bits)
    body = b"WAVE" + b"fmt " + struct.pack("<I", len(fmt)) + fmt + b"data" + struct.pack("<I", len(data)) + data
    return b"RIFF" + struct.pack("<I", len(body)) + body


def ext80(v):
    e = int(math.floor(math.log2(v)))
    mant = int(v / 2.0 ** (e - 63))
    return struct.pack(">H", e + 16383) + mant.to_bytes(8, "big")


def aiff_bytes(x, little=False):
    ch = x.shape[1]
    data = np.round(x * 32767).astype("<i2" if little else ">i2").tobytes()
    kind = b"AIFC" if little else b"AIFF"
    comm = struct.pack(">hIh", ch, len(x), 16) + ext80(float(SR))
    if little:
        comm += b"sowt" + b"\x00"
    if len(comm) & 1:
        comm += b"\x00"
    ssnd = struct.pack(">II", 0, 0) + data
    body = kind + b"COMM" + struct.pack(">I", len(comm)) + comm + b"SSND" + struct.pack(">I", len(ssnd)) + ssnd
    return b"FORM" + struct.pack(">I", len(body)) + body


# ---------------------------------------------------------------- signals

def sig_sine55():
    x = 0.5 * np.sin(2 * math.pi * 55 * t(1.0))
    return x[:, None], {"dur_s": 1.0, "peak_dbfs": 20 * math.log10(0.5), "rms_dbfs": 10 * math.log10(0.125),
                        "f0_hz": 55.0}


def sig_decay_noise():
    tau = 0.1
    tt = t(3.0)
    x = RNG.standard_normal(len(tt)) * np.exp(-tt / tau)
    x /= np.abs(x).max()
    k = tau * math.log(10) / 20  # energy exp(-2t/tau): -N dB at N * tau ln10 / 20
    return x[:, None], {"t20_ms": 20 * k * 1000, "t60_ms": 60 * k * 1000}


def sig_decay_sine():
    tt = t(2.0)
    x = np.sin(2 * math.pi * 55 * tt) * np.exp(-tt / 0.15)
    return np.stack([x, x], axis=1), {"pitch_steady_hz": 55.0, "tonal_db": 0.0}


def sig_band_noise(lo, hi):
    n = int(0.5 * SR)
    spec = np.fft.rfft(RNG.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / SR)
    spec[(f < lo) | (f > hi)] = 0
    x = np.fft.irfft(spec, n)
    x /= np.abs(x).max()
    hf = None
    if hi > 10000:
        hf = 10 * math.log10((hi - max(lo, 10000)) / (hi - lo))
    out = {"centroid_hz": (lo + hi) / 2}
    if hf is not None:
        out["hf_db"] = hf
    return x[:, None], out


def sig_click_body():
    k = int(0.010 * SR)
    tt = t(0.5)
    x = 0.1 * np.sin(2 * math.pi * 100 * tt)
    x[:k] = np.cos(2 * math.pi * 1000 * tt[:k])
    head = (x[:k] ** 2).sum()
    body = (x[k:] ** 2).sum()
    return x[:, None], {"transient_body_db": 10 * math.log10(head / body)}


def sig_kick_drop():
    tt = t(1.5)
    f = 50 + 100 * np.exp(-tt / 0.01)
    ph = 2 * math.pi * np.cumsum(f) / SR
    x = np.sin(ph) * np.exp(-tt / 0.2)
    a, b = 0.002, 0.020  # one period of 50 Hz from 2 ms
    mean_f = 50 + 100 * 0.01 * (math.exp(-a / 0.01) - math.exp(-b / 0.01)) / (b - a)
    return x[:, None], {"pitch_steady_hz": 50.0, "pitch_attack_hz": mean_f}


TOL = {"dur_s": ("abs", 0.01), "peak_dbfs": ("abs", 0.1), "rms_dbfs": ("abs", 0.2),
       "t20_ms": ("relms", 0.10), "t60_ms": ("relms", 0.10), "centroid_hz": ("rel", 0.10),
       "hf_db": ("abs", 1.5), "transient_body_db": ("abs", 1.0), "tonal_db": ("abs", 1.5),
       "pitch_steady_hz": ("cents", 100), "pitch_attack_hz": ("cents", 200), "f0_hz": ("cents", 50)}


def agree(col, a, b):
    if a is None or b is None or not math.isfinite(a) or not math.isfinite(b):
        return False
    kind, tol = TOL[col]
    if kind == "abs":
        return abs(a - b) <= tol
    if kind == "rel":
        return abs(a - b) <= tol * abs(b)
    if kind == "relms":
        return abs(a - b) <= max(5.0, tol * abs(b))
    return a > 0 and b > 0 and cents(a, b) <= tol


def tout_measures(x, kind):
    """The same signal through tout.py's own functions (read only)."""
    # tout.py imports its own 'mesure' module: hide ours while it loads
    mine = sys.modules.pop("mesure", None)
    saved = list(sys.path)
    try:
        sys.path.insert(0, os.path.abspath(os.path.join(HERE, "..", "..")))
        import tout  # noqa: E402
    except Exception as e:  # tout.py missing or broken: say it, do not hide it
        return {"_error": "import tout: %s" % e}
    finally:
        sys.path[:] = saved
        if mine is not None:
            sys.modules["mesure"] = mine
    out = dict(tout.common(SR, x))
    if kind == "oneshot_melodic":
        out.update(tout.measure_oneshot(SR, x, {}))
    else:
        out.update(tout.measure_drum_oneshot(SR, x, "kick"))
    return out


def main():
    cases = [("sine 55 Hz", sig_sine55, "oneshot_melodic"),
             ("decaying noise tau 0.1 s", sig_decay_noise, "drum_oneshot"),
             ("decaying sine 55 Hz stereo", sig_decay_sine, "drum_oneshot"),
             ("band noise 2-4 kHz", lambda: sig_band_noise(2000, 4000), "drum_oneshot"),
             ("band noise 8-12 kHz", lambda: sig_band_noise(8000, 12000), "drum_oneshot"),
             ("click + body", sig_click_body, "drum_oneshot"),
             ("kick 150 -> 50 Hz", sig_kick_drop, "drum_oneshot")]
    fails = 0
    tout_miss = 0
    print("signal | column | truth | mesure.py | tout.py")
    for name, fn, kind in cases:
        x, truth = fn()
        mine = mesure.common(SR, x)
        if kind == "drum_oneshot":
            mine.update(mesure.drum(SR, x))
        else:
            mine["f0_hz"] = mesure.melodic_f0(SR, x)
        theirs = tout_measures(x, kind)
        for col, v in truth.items():
            ok_m = agree(col, mine.get(col), v)
            tv = theirs.get(col)
            ok_t = agree(col, tv if isinstance(tv, (int, float)) else None, v)
            fails += not ok_m
            tout_miss += not ok_t
            print("%s | %s | %.4g | %s %s | %s %s" % (
                name, col, v, "%.4g" % mine[col] if mine.get(col) is not None else "-", "ok" if ok_m else "MISS",
                "%.4g" % tv if isinstance(tv, (int, float)) else "-", "ok" if ok_t else "MISS"))
    # reader: the same samples written as 16 / 24 / float32 wav, aiff, aifc sowt
    x = 0.7 * np.stack([np.sin(2 * math.pi * 440 * t(0.1)), np.cos(2 * math.pi * 330 * t(0.1))], axis=1)
    with tempfile.TemporaryDirectory() as d:
        for label, data, q in (("wav16", wav_bytes(x, 16), 2e-4), ("wav24", wav_bytes(x, 24), 1e-6),
                               ("wavf32", wav_bytes(x, 32, True), 1e-7), ("aiff16", aiff_bytes(x), 2e-4),
                               ("aifc-sowt16", aiff_bytes(x, True), 2e-4)):
            p = os.path.join(d, "s." + ("aif" if label.startswith("aif") else "wav"))
            with open(p, "wb") as fh:
                fh.write(data)
            sr, y = mesure.read_audio(p)
            ok = sr == SR and y.shape == x.shape and float(np.abs(y - x).max()) <= q
            fails += not ok
            print("reader | %s | sr %d shape %s | %s" % (label, sr, y.shape, "ok" if ok else "MISS"))
    print("mesure.py misses: %d ; tout.py misses: %d" % (fails, tout_miss))
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
