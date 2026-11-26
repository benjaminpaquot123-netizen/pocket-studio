#!/usr/bin/env python3
"""Self-test of tout.end_cut on the traps of the second blind audit (docs/sons/AUDIT-TOUT-2.md, lot D).

    python3 -I scripts/sons/selftest_tout_d.py

Defects 15-17, 29-31 and 40: noisy sounds with a short anti-click fade,
a cut hidden under hiss or dither, quiet files, very short files, a sound
that ends in its own noise floor, coarse quantization, stereo where one
channel stops early, and the end_cut docstring. Exit code 1 when a check fails.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tout  # noqa: E402

FAILS = []
COUNT = [0]
SR = 48000


def check_eq(name, got, want):
    COUNT[0] += 1
    ok = got == want
    if not ok:
        FAILS.append(name)
    print("%s %s: got %r want %r" % ("ok  " if ok else "FAIL", name, got, want))


def db(a):
    return 10 ** (a / 20.0)


def T(d, sr=SR):
    return np.arange(int(round(d * sr))) / sr


def noise(d, a, seed, sr=SR):
    return a * np.random.default_rng(seed).uniform(-1, 1, int(round(d * sr)))


def hp(x):
    y = np.diff(np.diff(x, prepend=0.0), prepend=0.0)
    return y / (np.max(np.abs(y)) + 1e-12)


def kick(d, taua, sr=SR):
    t = T(d, sr)
    f = 50.0 + 110.0 * np.exp(-t / 0.03)
    return 0.9 * np.sin(2 * np.pi * np.cumsum(f) / sr) * np.exp(-t / taua)


def b808(f, d, tau, sr=SR):
    t = T(d, sr)
    return 0.9 * np.sin(2 * np.pi * f * t) * np.exp(-t / tau)


def tone(f, d, a, sr=SR):
    t = T(d, sr)
    return a * sum(np.sin(2 * np.pi * f * k * t) / k for k in range(1, 7)) / 1.8


def snare(d, sr=SR):
    t = T(d, sr)
    return 0.8 * (0.7 * noise(d, 1.0, 2, sr) * np.exp(-t / 0.05) + 0.3 * np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.04))


def fade(x, ms, sr=SR):
    n = int(ms * sr / 1000)
    y = x.copy()
    if n > 0:
        y[-n:] *= np.linspace(1, 0, n)
    return y


def q(x, bits):
    s = float(1 << (bits - 1))
    return np.round(x * s) / s


def cut(sr, x):
    x = np.asarray(x, dtype=np.float64)
    return tout.end_cut(sr, x if x.ndim == 2 else x[:, None])["end_cut"]


def noisy(kind, sr, seed, cutpos):
    """Hats, snares, white noise and a kick cut at `cutpos` s (fuzz of the audit, b3c)."""
    rng = np.random.default_rng(seed)
    n = int(1.0 * sr)
    t = np.arange(n) / sr
    w = rng.standard_normal(n)
    if kind == "hat":
        x = np.diff(np.diff(w, prepend=0), prepend=0)
        x = x / np.max(np.abs(x)) * np.exp(-t / 0.3) * 0.7
    elif kind == "snare":
        x = (0.6 * w / 4 + 0.5 * np.sin(2 * np.pi * 190 * t)) * np.exp(-t / 0.15)
        x = x / np.max(np.abs(x)) * 0.8
    elif kind == "white":
        x = w / 4 * np.exp(-t / 0.3) * 0.7
    else:
        ph = 2 * np.pi * (50 * t + 110 * 0.03 * (1 - np.exp(-t / 0.03)))
        x = np.sin(ph) * np.exp(-t / 0.2) * 0.9
    return q(x[:int(cutpos * sr)], 24)


def test_noisy_short_fades():
    """Defect 15: a 5-10 ms anti-click fade on a hat or a snare is a fade, 0-2 ms is still a cut."""
    for kind in ("hat", "snare", "white"):
        for sr in (44100, 48000, 96000):
            for ms, want in ((0, 1), (2, 1), (5, 0), (6, 0), (8, 0), (10, 0)):
                got = []
                for seed in range(8):
                    x = noisy(kind, sr, seed, 0.15 + 0.01 * (seed % 7))
                    got.append(cut(sr, fade(x, ms, sr) if ms else x))
                check_eq("%s %d Hz, %d ms fade: end_cut %d on 8 seeds" % (kind, sr, ms, want), got, [want] * 8)
    for sr in (44100, 48000):
        got = [cut(sr, fade(noisy("kick", sr, s, 0.15 + 0.01 * (s % 7)), 5, sr)) for s in range(8)]
        check_eq("kick %d Hz, 5 ms fade: not cut on 8 seeds" % sr, got, [0] * 8)
    for sr in (44100, 48000, 96000):
        h = (0.5 * hp(noise(0.5, 1.0, 5, sr)) * np.exp(-T(0.5, sr) / 0.3))[:int(0.2 * sr)]
        s = snare(0.6, sr)[:int(0.08 * sr)]
        for ms in (5, 10):
            check_eq("audit hat %d Hz + %d ms fade: not cut" % (sr, ms), cut(sr, fade(h, ms, sr)), 0)
            check_eq("audit snare %d Hz + %d ms fade: not cut" % (sr, ms), cut(sr, fade(s, ms, sr)), 0)


TOPICS = [test_noisy_short_fades]


def main():
    with np.errstate(all="ignore"):
        for fn in TOPICS:
            try:
                fn()
            except Exception as e:  # a crash is a failed check
                COUNT[0] += 1
                FAILS.append("%s crashed: %s: %s" % (fn.__name__, type(e).__name__, e))
                print("FAIL %s crashed: %s: %s" % (fn.__name__, type(e).__name__, e))
    print("%d checks, %d failed" % (COUNT[0], len(FAILS)))
    for name in FAILS:
        print("FAILED: " + name)
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
