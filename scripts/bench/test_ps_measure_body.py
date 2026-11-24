#!/usr/bin/env python3
"""Checks ps_measure's « body » measures (L4) on two synthetic songs, before they read a real one:
a hollow Drop (a kick and a bass alone, silence between the hits, no treble, a Break that falls
15 LU at once) and a full one (the same plus a pad, a shaker and sixteenth hats, a Break 4 LU under).
The hollow song must read: few tracks together, treble seldom busy, a deep floor between the hits,
one brutal transition; the full one the opposite. Standard library only.

    python3 -I scripts/bench/test_ps_measure_body.py [--tools <dir with ps_measure>]
(without --tools it builds ps_measure in build/bench-test-tools)."""
import array
import json
import math
import os
import random
import subprocess
import sys
import tempfile
import wave

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
RATE = 48000
BPM = 120
BEAT = RATE * 60 // BPM          # 24000 frames
BAR = 4 * BEAT                   # 2 s
# Groove (4 bars), Drop (4 bars), Break (4 bars): 24 s.
SECTIONS = [("Groove", 0), ("Drop", 4 * BAR), ("Break", 8 * BAR)]
TOTAL = 12 * BAR


def write_wav(path, x):
    """Mono float list -> 16-bit stereo WAV (clipped at ±1)."""
    pcm = array.array("h")
    for v in x:
        s = int(max(-1.0, min(1.0, v)) * 32767)
        pcm.append(s)
        pcm.append(s)
    with wave.open(path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())


def kick(n):
    x = [0.0] * n
    for start in range(0, n, BEAT):
        for i in range(min(int(0.12 * RATE), n - start)):
            t = i / RATE
            x[start + i] += 0.8 * math.sin(2 * math.pi * (55 + 90 * math.exp(-t * 40)) * t) * math.exp(-t * 30)
    return x


def bass(n):
    # An eighth-note bass (60 ms notes): short, silence between them.
    x = [0.0] * n
    for start in range(BEAT // 2, n, BEAT):
        for i in range(min(int(0.06 * RATE), n - start)):
            t = i / RATE
            x[start + i] += 0.35 * math.sin(2 * math.pi * 82.4 * t) * math.exp(-t * 25)
    return x


def pad(n):
    # A held chord (three saws, gently filtered by summing harmonics): the floor between the hits.
    x = [0.0] * n
    for f in (220.0, 277.2, 329.6):
        for h in range(1, 9):
            a = 0.05 / h
            w = 2 * math.pi * f * h / RATE
            for i in range(n):
                x[i] += a * math.sin(w * i)
    return x


def hats(n, rng):
    # Sixteenth hats: noise bursts high-passed by a first difference, 40 ms decay.
    x = [0.0] * n
    step = BEAT // 4
    for start in range(0, n, step):
        prev = 0.0
        for i in range(min(int(0.04 * RATE), n - start)):
            v = rng.uniform(-1, 1)
            x[start + i] += 0.25 * (v - prev) * math.exp(-i / RATE * 60)
            prev = v
    return x


def shaker(n, rng):
    # A steady shaker: soft high-passed noise all along.
    x = [0.0] * n
    prev = 0.0
    for i in range(n):
        v = rng.uniform(-1, 1)
        x[i] = 0.05 * (v - prev)
        prev = v
    return x


def song(stems, gains):
    """The song: each stem over the whole length, scaled per section (gains: label -> linear)."""
    out = [0.0] * TOTAL
    bounds = [s for _, s in SECTIONS] + [TOTAL]
    for k, (label, start) in enumerate(SECTIONS):
        g = gains[label]
        for x in stems:
            for i in range(start, bounds[k + 1]):
                out[i] += g * x[i]
    return out


def pscmd(path):
    with open(path, "w") as f:
        f.write(f"PSCMD 1\nRATE {RATE}\nMETA style synth\nSONG frames={TOTAL} bpm={BPM}\n")
        for k, (label, start) in enumerate(SECTIONS):
            f.write(f"SECTION {k} {start} {label}\n")


def tools(argv):
    if "--tools" in argv:
        return argv[argv.index("--tools") + 1]
    out = os.path.join(ROOT, "build", "bench-test-tools")
    subprocess.run(["cmake", "-S", os.path.join(ROOT, "engine", "bench"), "-B", out, "-DCMAKE_BUILD_TYPE=Release"],
                   check=True, stdout=subprocess.DEVNULL)
    subprocess.run(["cmake", "--build", out, "-j", str(os.cpu_count() or 2), "--target", "ps_measure"], check=True, stdout=subprocess.DEVNULL)
    return out


def measure(exe, d, name, stems, gains):
    wav = os.path.join(d, name + ".wav")
    write_wav(wav, song(stems, gains))
    args = [exe, wav, os.path.join(d, "song.pscmd")]
    for k, x in enumerate(stems):
        stem = os.path.join(d, f"{name}.track{k}.wav")
        write_wav(stem, song([x], gains))
        args += ["--track", f"{k}:{stem}"]
    r = subprocess.run(args, check=True, capture_output=True, text=True)
    return json.loads(r.stdout)


def main():
    exe = os.path.join(tools(sys.argv), "ps_measure")
    rng = random.Random(7)
    k, b = kick(TOTAL), bass(TOTAL)
    p, h, s = pad(TOTAL), hats(TOTAL, rng), shaker(TOTAL, rng)
    failures = []

    def check(name, cond):
        print(("ok    " if cond else "FAIL  ") + name)
        if not cond:
            failures.append(name)

    with tempfile.TemporaryDirectory() as d:
        pscmd(os.path.join(d, "song.pscmd"))
        hollow = measure(exe, d, "hollow", [k, b], {"Groove": 0.5, "Drop": 1.0, "Break": 0.18})
        full = measure(exe, d, "full", [k, b, p, h, s], {"Groove": 0.5, "Drop": 1.0, "Break": 0.63})
    for name, m in (("hollow", hollow), ("full", full)):
        print(name, {key: m.get(key) for key in ("drop_tracks", "hf_occupancy", "floor_between_hits", "brutal_transitions")})
        print(name, "transitions", m.get("transitions"))
    # Tracks sounding together at the Drop: 2 against 5.
    check("hollow: 2 tracks together at the Drop", hollow.get("drop_tracks") == 2)
    check("full: 5 tracks together at the Drop", full.get("drop_tracks") == 5)
    # The treble's busy share: the hollow Drop has none, the full one nearly always.
    check("hollow: treble busy < 10 % of the Drop", hollow.get("hf_occupancy") is not None and hollow["hf_occupancy"] < 0.10)
    check("full: treble busy > 80 % of the Drop", full.get("hf_occupancy") is not None and full["hf_occupancy"] > 0.80)
    # The floor between the hits: silence (hollow) against a held pad (full).
    check("hollow: floor between hits < -30 dB", hollow.get("floor_between_hits") is not None and hollow["floor_between_hits"] < -30)
    check("full: floor between hits > -15 dB", full.get("floor_between_hits") is not None and full["floor_between_hits"] > -15)
    # Transitions: Groove -> Drop (+6 LU, into a Drop: allowed), Drop -> Break (-15 against -4 LU).
    def jump(m, to):
        for t in m.get("transitions") or []:
            if t["to"] == to:
                return t
        return None
    hb, fb = jump(hollow, "Break"), jump(full, "Break")
    check("hollow: Drop -> Break falls ~15 LU", hb is not None and -17 < hb["jump_lu"] < -13)
    check("full: Drop -> Break falls ~4 LU", fb is not None and -6 < fb["jump_lu"] < -2)
    check("hollow: the Break is a brutal transition", hb is not None and hb["brutal"])
    check("full: the Break is not brutal", fb is not None and not fb["brutal"])
    hd = jump(hollow, "Drop")
    check("Groove -> Drop: +6 LU into a Drop, not brutal", hd is not None and 5 < hd["jump_lu"] < 7 and not hd["brutal"])
    check("hollow: 1 brutal transition, full: 0", hollow.get("brutal_transitions") == 1 and full.get("brutal_transitions") == 0)
    if failures:
        print(f"{len(failures)} check(s) failed")
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
