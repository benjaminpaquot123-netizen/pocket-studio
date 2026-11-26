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


def test_floor_after_the_end():
    """Defects 16 and 30: a cut hidden under hiss or dither is still a cut; a sound
    that ends in its own floor (hiss, hum, coarse quantization, a natural tail) is not."""
    for sr in (44100, 48000, 96000):
        rng = np.random.default_rng(7)
        src = {"kick": kick(1.2, 0.12, sr)[:int(0.15 * sr)],
               "hat": (0.5 * hp(noise(0.5, 1.0, 5, sr)) * np.exp(-T(0.5, sr) / 0.3))[:int(0.2 * sr)],
               "snare": snare(0.6, sr)[:int(0.08 * sr)],
               "lead": tone(440.0, 1.0, 0.4, sr)[:int(0.5 * sr)]}
        for name, c in src.items():
            for label, d, lvl in (("50 ms dither -80 dB", 0.05, -80), ("50 ms hiss -66 dB", 0.05, -66),
                                  ("200 ms hiss -60 dB", 0.2, -60)):
                y = np.concatenate([c, db(lvl) * rng.uniform(-1, 1, int(d * sr))])
                check_eq("%s %d Hz, hard cut + %s: cut" % (name, sr, label), cut(sr, y), 1)
    full = kick(0.8, 0.06)
    for h in (-70, -60, -50, -40, -34):
        check_eq("finished kick + hiss %d dB: not cut" % h, cut(SR, full + noise(0.8, db(h), 11)), 0)
    for h in (-60, -50, -40):
        check_eq("finished kick + 50 Hz hum %d dB: not cut" % h,
                 cut(SR, full + db(h) * np.sin(2 * np.pi * 50 * T(0.8))), 0)
    for end in (-50, -55, -61):
        d = -end / 8.686 * 0.3
        t = T(d)
        check_eq("808 natural tail to %d dB: not cut" % end, cut(SR, np.sin(2 * np.pi * 40 * t) * np.exp(-t / 0.3) * 0.999), 0)
        d = -end / 8.686 * 0.25
        t = T(d)
        check_eq("lead natural tail to %d dB: not cut" % end, cut(SR, np.sin(2 * np.pi * 440 * t) * np.exp(-t / 0.25) * 0.999), 0)
    for bits in (8, 10, 12):
        check_eq("finished kick quantized to %d bit: not cut" % bits, cut(SR, q(kick(0.5, 0.06), bits)), 0)
    check_eq("cut kick quantized to 8 bit: cut", cut(SR, q(kick(1.2, 0.12)[:7200], 8)), 1)
    rng = np.random.default_rng(3)
    c = kick(1.2, 0.12)[:7200]
    for lsb in (1, 2, 4):
        y = np.concatenate([c, np.zeros(4800)]) + (rng.random(12000) - rng.random(12000)) * lsb / 32768.0
        check_eq("cut kick + 100 ms dither %d LSB (16 bit): cut" % lsb, cut(SR, q(y, 16)), 1)
    y = np.concatenate([full, np.zeros(4800)]) + (rng.random(len(full) + 4800) - rng.random(len(full) + 4800)) / 32768.0
    check_eq("finished kick + 1 LSB dither (16 bit): not cut", cut(SR, q(y, 16)), 0)
    st = np.stack([np.concatenate([c, np.zeros(len(full) - 7200)]), full * 0.9], axis=1)
    check_eq("stereo: left cut early then silent, right finishes: not cut", cut(SR, st), 0)
    st = np.stack([full * 0.9, np.concatenate([c, np.zeros(len(full) - 7200)])], axis=1)
    check_eq("stereo: right cut early then silent, left finishes: not cut", cut(SR, st), 0)
    st = np.stack([fade(c, 30), c * 0.05], axis=1)
    check_eq("stereo: left faded, right quiet but cut: cut", cut(SR, st), 1)
    st = np.stack([c, c * 0.5], axis=1)
    check_eq("stereo: both channels cut together: cut", cut(SR, st), 1)


TOPICS = [test_noisy_short_fades, test_floor_after_the_end]


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
