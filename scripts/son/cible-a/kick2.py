"""Layered kick: no more hollow pure-sine kick.

Body: sine with a two-stage pitch envelope (a fast drop from the attack pitch to a
'knee' pitch, then a slow glide to the fundamental), hold + exponential decay,
run through a tanh saturator that never goes below a floor (it always adds the
2nd/3rd harmonics that fill the 100-300 Hz band, the 'chest' of the kick).
Punch: a short mid sine (110-300 Hz) that speaks only on the first tens of ms.
Transient: a filtered noise click and a short high 'tick'.
Bus: soft compressor, optional low-pass and bit reduction.
"""
import numpy as np
import pedalboard as pb

SR = 44100

# name -> (lo, hi, log); a style box overrides (lo, hi)
KICK2 = {
    "f_end": (40, 70, True),       # fundamental
    "f_knee": (70, 160, True),     # end of the fast drop
    "f_start": (180, 600, True),   # attack pitch
    "t_fast": (0.002, 0.012, True),
    "t_slow": (0.02, 0.15, True),
    "hold_s": (0.0, 0.06, False),
    "decay_s": (0.05, 0.6, True),
    "sat": (1.5, 8.0, True),       # tanh drive, floor 1.5 = always some harmonics
    "asym": (0.0, 0.3, False),     # even harmonics
    "punch": (0.0, 0.6, False),
    "punch_f": (110, 300, True),
    "punch_d": (0.015, 0.07, True),
    "click": (0.0, 0.4, False),
    "click_f": (1500, 7000, True),
    "tick": (0.0, 0.3, False),
    "comp_db": (-24, -6, False),
    "len_s": (0.3, 1.0, False),
    "lp": (2500, 16000, True),
    "bits": (8, 16, False),
}


def lerp(lo, hi, u, log=False):
    u = float(np.clip(u, 0.0, 1.0))
    if log:
        return float(np.exp(np.log(lo) + (np.log(hi) - np.log(lo)) * u))
    return lo + (hi - lo) * u


def params(box, u):
    p = {}
    for i, (k, (lo, hi, lg)) in enumerate(KICK2.items()):
        if k in box:
            lo, hi = box[k]
        p[k] = lerp(lo, hi, u[i], lg)
    return p


def kick(p, rng):
    n = int(p["len_s"] * SR)
    t = np.arange(n) / SR
    fe, fk, fs = p["f_end"], max(p["f_knee"], p["f_end"] + 1), max(p["f_start"], p["f_knee"] + 1)
    f = fe + (fk - fe) * np.exp(-t / p["t_slow"]) + (fs - fk) * np.exp(-t / p["t_fast"])
    ph = 2 * np.pi * np.cumsum(f) / SR
    att = np.clip(t / 0.0005, 0, 1)
    amp = att * np.exp(-np.maximum(t - p["hold_s"], 0) / p["decay_s"])
    body = np.sin(ph) * amp
    d = p["sat"]
    body = np.tanh(d * (body + p["asym"] * amp)) - np.tanh(d * p["asym"] * amp)
    body /= max(np.abs(body).max(), 1e-9)
    punch = np.sin(2 * np.pi * p["punch_f"] * t + 0.5) * att * np.exp(-t / p["punch_d"])
    nz = rng.standard_normal(n) * np.exp(-t / 0.003)
    nz = pb.Pedalboard([pb.HighpassFilter(800), pb.LowpassFilter(p["click_f"])])(nz.astype(np.float32)[None], SR)[0]
    tick = np.sin(2 * np.pi * 3200 * t) * np.exp(-t / 0.0012)
    x = body + p["punch"] * punch + p["click"] * nz / max(np.abs(nz).max(), 1e-9) + p["tick"] * tick
    m = int(0.01 * SR)
    x[-m:] *= np.linspace(1, 0, m)
    fx = [pb.HighpassFilter(28), pb.Compressor(threshold_db=p["comp_db"], ratio=3.0, attack_ms=8, release_ms=80),
          pb.LowpassFilter(p["lp"])]
    if p["bits"] < 13:
        fx.append(pb.Bitcrush(bit_depth=p["bits"]))
    y = pb.Pedalboard(fx)(x.astype(np.float32)[None], SR)[0]
    return y


def render(box, u, seed=0):
    p = params(box, u)
    y = kick(p, np.random.default_rng(seed)).astype(np.float64)
    pk = np.abs(y).max()
    return (y / pk).astype(np.float32), p
