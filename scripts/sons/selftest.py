#!/usr/bin/env python3
"""Self-test of mesure.py on synthetic signals whose truth is known.

    python3 -I scripts/sons/selftest.py

Every measured value must sit within its tolerance of the value the signal
was built with. Exit code 1 when a single check fails. No sample is needed:
all signals are computed here.
"""
import csv
import math
import os
import struct
import sys
import tempfile

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mesure  # noqa: E402

FAILS = []
COUNT = [0]
LN10 = math.log(10.0)


def check(name, got, want, tol, relative=False):
    """|got - want| <= tol (or tol * |want| when relative)."""
    COUNT[0] += 1
    limit = tol * abs(want) if relative else tol
    ok = math.isfinite(got) and abs(got - want) <= limit
    if not ok:
        FAILS.append(name)
    print("%s %-46s got %10.4f  want %10.4f  +-%.4g" % ("ok  " if ok else "FAIL", name, got, want, limit))


def check_below(name, got, ceiling):
    COUNT[0] += 1
    ok = math.isfinite(got) and got < ceiling
    if not ok:
        FAILS.append(name)
    print("%s %-46s got %10.4f  want < %g" % ("ok  " if ok else "FAIL", name, got, ceiling))


def check_true(name, ok):
    COUNT[0] += 1
    if not ok:
        FAILS.append(name)
    print("%s %s" % ("ok  " if ok else "FAIL", name))


def time(sr, seconds=1.0):
    return np.arange(int(seconds * sr)) / sr


def band_noise(sr, n, lo, hi, seed):
    """Noise with a flat power spectrum between lo and hi Hz, nothing else."""
    rng = np.random.default_rng(seed)
    f = np.fft.rfftfreq(n, 1.0 / sr)
    spec = np.where((f >= lo) & (f <= hi), np.exp(2j * np.pi * rng.random(len(f))), 0.0)
    x = np.fft.irfft(spec, n)
    return x / np.max(np.abs(x))


def signals(sr):
    t = time(sr)

    # Pure sine: no harmonic at all.
    for f0, tau in ((100.0, 0.1), (50.0, 0.03), (200.0, 0.05)):
        r = mesure.measure(sr, np.exp(-t / tau) * np.sin(2 * np.pi * f0 * t))
        tag = "%d sine %g Hz tau %g" % (sr, f0, tau)
        check(tag + " steady", r["pitch_steady_hz"], f0, 0.01, relative=True)
        check(tag + " attack", r["pitch_attack_hz"], f0, 0.05, relative=True)
        check_below(tag + " h2", r["h2_db"], -80.0)
        check_below(tag + " h3", r["h3_db"], -80.0)
        check(tag + " tonal", r["tonal_db"], 0.0, 0.5)
        if f0 * tau >= 5.0:  # the formula holds for many periods per tau
            check(tag + " t20", r["t20_ms"], 1000 * tau * LN10, 0.02, relative=True)

    # Known harmonics: h2 at -20 dB, h3 at -30 dB (and a louder pair).
    for f0, tau, a2, a3 in ((60.0, 0.1, -20.0, -30.0), (45.0, 0.4, -20.0, -30.0),
                            (55.0, 0.2, -6.0, -8.0)):
        g2, g3 = 10 ** (a2 / 20), 10 ** (a3 / 20)
        x = np.exp(-t / tau) * (np.sin(2 * np.pi * f0 * t)
                                + g2 * np.sin(2 * np.pi * 2 * f0 * t + 1.0)
                                + g3 * np.sin(2 * np.pi * 3 * f0 * t + 2.0))
        r = mesure.measure(sr, x)
        tag = "%d harmonics %g Hz %g/%g" % (sr, f0, a2, a3)
        check(tag + " steady", r["pitch_steady_hz"], f0, 0.01, relative=True)
        check(tag + " h2", r["h2_db"], a2, 0.3)
        check(tag + " h3", r["h3_db"], a3, 0.3)

    # Exponential decay with a known tau: t = tau*ln(10)*level/20.
    for tau in (0.02, 0.1):
        x = np.exp(-t / tau) * np.sin(2 * np.pi * 1000.0 * t)
        r = mesure.measure(sr, x)
        tag = "%d decay tau %g" % (sr, tau)
        for level in (20, 40, 60):
            check(tag + " t%d" % level, r["t%d_ms" % level], 1000 * tau * LN10 * level / 20, 0.02, relative=True)
        check(tag + " transient/body", r["transient_body_db"],
              10 * math.log10(math.exp(2 * mesure.TRANSIENT_S / tau) - 1.0), 0.2)
        check(tag + " onset", r["onset_ms"], 0.0, 0.1)

    # Kick: pitch glides 150 -> 50 Hz (tau 20 ms), h2 -20 dB, h3 -30 dB.
    freq = 50.0 + 100.0 * np.exp(-t / 0.020)
    ph = 2 * np.pi * np.cumsum(freq) / sr
    kick = np.sin(ph) + 0.1 * np.sin(2 * ph) + 10 ** -1.5 * np.sin(3 * ph)
    a0, a1 = mesure.ATTACK_WIN
    attack_truth = 50.0 + 100.0 * 0.020 * (math.exp(-a0 / 0.020) - math.exp(-a1 / 0.020)) / (a1 - a0)
    hold = np.where(t < 0.12, 1.0, np.exp(-(t - 0.12) / 0.025))
    for name, env, tol_h in (("glide", np.exp(-t / 0.1), 0.3), ("glide+hold", hold, 1.0)):
        r = mesure.measure(sr, env * kick)
        tag = "%d %s" % (sr, name)
        check(tag + " steady", r["pitch_steady_hz"], 50.0, 0.02, relative=True)
        check(tag + " attack", r["pitch_attack_hz"], attack_truth, 0.05, relative=True)
        check(tag + " h2", r["h2_db"], -20.0, tol_h)
        check(tag + " h3", r["h3_db"], -30.0, tol_h)

    # Noise with a known centroid: flat 2-6 kHz -> 4 kHz, nothing above 10 kHz;
    # flat 5-15 kHz -> 10 kHz, half the energy above 10 kHz (-3.01 dB).
    n = len(t)
    r = mesure.measure(sr, np.exp(-t / 0.05) * band_noise(sr, n, 2000.0, 6000.0, 1))
    check("%d noise 2-6 kHz centroid" % sr, r["centroid_hz"], 4000.0, 0.02, relative=True)
    check_below("%d noise 2-6 kHz hf" % sr, r["hf_db"], -40.0)
    check_below("%d noise 2-6 kHz tonal" % sr, r["tonal_db"], -8.0)
    check("%d noise 2-6 kHz t20" % sr, r["t20_ms"], 1000 * 0.05 * LN10, 0.05, relative=True)
    r = mesure.measure(sr, np.exp(-t / 0.05) * band_noise(sr, n, 5000.0, 15000.0, 2))
    check("%d noise 5-15 kHz centroid" % sr, r["centroid_hz"], 10000.0, 0.02, relative=True)
    check("%d noise 5-15 kHz hf" % sr, r["hf_db"], -3.01, 0.3)

    # Clap: three short bursts then a tail. Truth from the envelope itself;
    # a first-crossing envelope method stops in the gap after burst one.
    env = np.zeros(n)
    for start in (0.0, 0.010, 0.020):
        m = t >= start
        env[m] = np.maximum(env[m], np.exp(-(t[m] - start) / 0.002))
    m = t >= 0.030
    env[m] = np.maximum(env[m], 0.8 * np.exp(-(t[m] - 0.030) / 0.030))
    r = mesure.measure(sr, env * np.sin(2 * np.pi * 3000.0 * t))
    edc = np.cumsum((env ** 2)[::-1])[::-1]
    for level in (20, 40):
        truth = 1000.0 * np.argmax(edc < edc[0] * 10 ** (-level / 10)) / sr
        check("%d clap t%d" % (sr, level), r["t%d_ms" % level], truth, 0.02, relative=True)
    check_true("%d clap t20 is after the last burst" % sr, r["t20_ms"] > 30.0)

    # Leading silence: times start at the onset, not at the file start.
    x = np.concatenate([np.zeros(int(0.05 * sr)), np.exp(-t / 0.05) * np.sin(2 * np.pi * 1000.0 * t)])
    r = mesure.measure(sr, x)
    check("%d silence then sound onset" % sr, r["onset_ms"], 50.0, 0.5)
    check("%d silence then sound t20" % sr, r["t20_ms"], 1000 * 0.05 * LN10, 0.02, relative=True)


def stereo():
    """44.1 kHz stereo: same numbers as mono, also when L = -R."""
    sr = 44100
    t = time(sr)
    s = np.exp(-t / 0.1) * (np.sin(2 * np.pi * 60.0 * t) + 0.1 * np.sin(2 * np.pi * 120.0 * t + 1.0))
    for name, right in (("L, 0.5 L", 0.5 * s), ("L, -L", -s), ("L, silence", 0.0 * s)):
        r = mesure.measure(sr, np.stack([s, right], axis=1))
        tag = "44100 stereo (%s)" % name
        check(tag + " t20", r["t20_ms"], 1000 * 0.1 * LN10, 0.02, relative=True)
        check(tag + " steady", r["pitch_steady_hz"], 60.0, 0.01, relative=True)
        check(tag + " h2", r["h2_db"], -20.0, 0.3)
    # Two channels with unrelated content: the centroid weighs both.
    n = len(t)
    left = np.exp(-t / 0.05) * band_noise(sr, n, 1000.0, 3000.0, 3)
    right = np.exp(-t / 0.05) * band_noise(sr, n, 5000.0, 7000.0, 4)
    right *= math.sqrt(np.sum(left ** 2) / np.sum(right ** 2))
    r = mesure.measure(sr, np.stack([left, right], axis=1))
    check("44100 stereo two bands centroid", r["centroid_hz"], 4000.0, 0.02, relative=True)


def wav_bytes(sr, x, kind):
    ch = x.shape[1]
    if kind == "f32":
        data, tag, bits = x.astype("<f4").tobytes(), 3, 32
    elif kind == "i24":
        v = np.round(x * (2 ** 23 - 1)).astype("<i4").reshape(-1)
        data, tag, bits = v.view(np.uint8).reshape(-1, 4)[:, :3].tobytes(), 1, 24
    else:
        data, tag, bits = np.round(x * 32767).astype("<i2").tobytes(), 1, 16
    align = ch * bits // 8
    fmt = struct.pack("<HHIIHH", tag, ch, sr, sr * align, align, bits)
    body = b"WAVE" + b"fmt " + struct.pack("<I", len(fmt)) + fmt + b"data" + struct.pack("<I", len(data)) + data
    return b"RIFF" + struct.pack("<I", len(body)) + body


def aiff_bytes(sr, x):
    ch = x.shape[1]
    data = np.round(x * 32767).astype(">i2").tobytes()
    exp = 16383 + int(math.floor(math.log2(sr)))
    mant = int(round(sr * 2.0 ** (63 - (exp - 16383))))
    comm = struct.pack(">hIh", ch, x.shape[0], 16) + struct.pack(">HQ", exp, mant)
    body = (b"AIFF" + b"COMM" + struct.pack(">I", len(comm)) + comm
            + b"SSND" + struct.pack(">I", len(data) + 8) + struct.pack(">II", 0, 0) + data)
    return b"FORM" + struct.pack(">I", len(body)) + body


def files():
    """File readers and the command line, on files written here."""
    sr = 44100
    t = time(sr, 0.5)
    s = np.exp(-t / 0.05) * np.sin(2 * np.pi * 1000.0 * t)
    x = np.stack([s, 0.5 * s], axis=1)
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "in")
        os.mkdir(src)
        made = {"a16.wav": wav_bytes(sr, x, "i16"), "b24.wav": wav_bytes(sr, x, "i24"),
                "c32f.wav": wav_bytes(sr, x, "f32"), "d16.aif": aiff_bytes(sr, x),
                "=bad.wav": b"RIFF\x04\x00\x00\x00WAVEjunk", "notes.txt": b"not audio"}
        for name, blob in made.items():
            with open(os.path.join(src, name), "wb") as fh:
                fh.write(blob)
        for name in ("a16.wav", "b24.wav", "c32f.wav", "d16.aif"):
            rate, y = mesure.read_audio(os.path.join(src, name))
            check_true("read %s rate and shape" % name, rate == sr and y.shape == x.shape)
            check("read %s samples" % name, float(np.max(np.abs(y - x))), 0.0, 1e-4)
        out = os.path.join(tmp, "out.csv")
        code = mesure.main([src, "--out", out])
        with open(out, newline="", encoding="utf-8") as fh:
            rows = list(csv.DictReader(fh))
        check_true("cli exit code and one row per audio file", code == 0 and len(rows) == 5)
        good = [r for r in rows if not r["error"]]
        check_true("cli four readable files, one refused", len(good) == 4)
        check_true("cli formula-like file name is neutralised",
                   any(r["file"] == "'=bad.wav" and r["error"] for r in rows))
        for r in good:
            check("cli %s t20" % r["file"], float(r["t20_ms"]), 1000 * 0.05 * LN10, 0.02, relative=True)
            check("cli %s channels" % r["file"], float(r["channels"]), 2, 0)
        inside = os.path.join(os.path.dirname(os.path.abspath(__file__)), "leak.csv")
        try:
            mesure.main([src, "--out", inside])
            refused = False
        except SystemExit:
            refused = True
        check_true("cli refuses to write inside the repository", refused and not os.path.exists(inside))


def main():
    with np.errstate(all="ignore"):
        for sr in (48000, 44100):
            signals(sr)
        stereo()
        files()
    print("%d checks, %d failed" % (COUNT[0], len(FAILS)))
    for name in FAILS:
        print("FAILED: " + name)
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
