"""4-bar acoustic loop per style from the chosen one-shots, bus tuned to the loop DNA.

  PS_BASE=.../base.py python -I boucle.py DNA.tsv PICKS_DIR OUTDIR STYLE [EVALS]

PICKS_DIR holds <style>-<family>-01.wav (peak -1 dBFS). Writes OUTDIR/<style>-boucle-01.wav
and prints one JSON line (score, z, params).
"""
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import acou  # noqa: E402

SR = acou.SR
# 16 steps per bar; strings: x = hit, o = soft hit (ghost), . = rest. One bar, repeated with a fill on bar 4.
P = {
    "rock": dict(bpm=112, swing=0.0,
                 kick="x.......x.x.....", snare="....x.......x...", hat="x.x.x.x.x.x.x.x.",
                 fill={"snare": "....x.......xoxx", "tom": "............x..."}, crash_on1=True),
    "pop": dict(bpm=100, swing=0.0,
                kick="x.....x...x.....", snare="....x.......x...", hat="x.x.x.x.x.x.x.x.",
                open="..............x.", fill={"snare": "....x.......x.xx"}, crash_on1=True),
    "funk": dict(bpm=100, swing=0.08,
                 kick="x..x......x..x..", snare="....x..o.o..x..o", hat="xxxxxxxxxxxxxxxx",
                 open=".......x........", fill={"snare": "....x..o.o..xoxx"}),
    "jazz-soul": dict(bpm=126, swing=0.33,
                      kick="x.........x.....", snare="......o.....x..o", ride="x...x.xx x...x.xx".replace(" ", ""),
                      hat="....x.......x...", fill={"snare": "......o...o.xoxx"}),
    "lo-fi": dict(bpm=82, swing=0.25,
                  kick="x......x..x.....", snare="....x.......x...", hat="x.x.x.x.x.x.x.x.",
                  open="......x.........", fill={"tom": "..........x.x..."}),
    "boom-bap": dict(bpm=90, swing=0.2,
                     kick="x.....x...x..x..", snare="....x.......x...", hat="x.x.x.x.x.x.x.x.",
                     open="..............x.", fill={"snare": "....x.......x..x"}),
}
FILES = {"kick": "kick", "snare": "caisse-claire", "hat": "charleston-ferme", "open": "charleston-ouvert",
         "ride": "ride", "crash": "crash", "tom": "tom"}
VOICES = ("kick", "snare", "hat", "open", "ride", "crash", "tom")
BUS = [("g_kick", -12, 6), ("g_snare", -12, 6), ("g_hat", -24, 0), ("g_open", -24, 0), ("g_ride", -24, 0),
       ("g_crash", -24, 0), ("g_tom", -18, 3), ("hpf", 20, 200), ("lpf", 3000, 20000), ("thr", -30, 0),
       ("ratio", 1, 8), ("drive", 0, 18), ("drive_mix", 0, 0.6)]


def load(picks, style):
    import soundfile as sf
    out = {}
    for v in VOICES:
        p = os.path.join(picks, "%s-%s-01.wav" % (style, FILES[v]))
        if os.path.exists(p):
            out[v] = sf.read(p, dtype="float32", always_2d=True)[0]
    return out


def sequence(style, hits, g):
    d = P[style]
    step = 60.0 / d["bpm"] / 4
    n = int(16 * 4 * step * SR)
    mix = np.zeros((n + 3 * SR, 2), np.float32)
    rng = np.random.default_rng(7)

    def put(v, t, amp):
        if v not in hits:
            return
        x = hits[v]
        i = max(0, int(t * SR))
        x = x[:len(mix) - i]
        mix[i:i + len(x)] += x * amp * 10 ** (g["g_" + v] / 20)

    for bar in range(4):
        last = bar == 3
        for v in ("kick", "snare", "hat", "open", "ride", "tom"):
            pat = d.get(v, "")
            if last and v in d.get("fill", {}):
                pat = d["fill"][v]
            if v == "hat" and "open" in d and pat:  # close the hat where it opens
                pat = "".join("." if d["open"][i] == "x" else c for i, c in enumerate(pat))
            for i, c in enumerate(pat):
                if c == ".":
                    continue
                t = (bar * 16 + i) * step + (d["swing"] * step if i % 2 else 0.0)
                amp = (0.35 if c == "o" else 1.0) * (0.85 + 0.15 * rng.random())
                if v in ("hat", "ride") and i % 4:
                    amp *= 0.75
                put(v, t + rng.normal(0, 0.002), amp)
        if bar == 0 and d.get("crash_on1"):
            put("crash", 0.0, 1.0)
    return mix[:n]


def bus(x, g):
    import pedalboard as pb
    x = x / max(np.abs(x).max(), 1e-9)
    y = pb.Pedalboard([pb.HighpassFilter(cutoff_frequency_hz=g["hpf"]),
                       pb.LowpassFilter(cutoff_frequency_hz=g["lpf"]),
                       pb.Compressor(threshold_db=g["thr"], ratio=g["ratio"], attack_ms=10, release_ms=120)])(
        x.T.copy(), SR)
    if g["drive_mix"] > 0.01:
        dd = pb.Pedalboard([pb.Distortion(drive_db=g["drive"])])(y.copy(), SR)
        dd *= np.abs(y).max() / max(np.abs(dd).max(), 1e-9)
        y = (1 - g["drive_mix"]) * y + g["drive_mix"] * dd
    y = y.T
    k = int(0.005 * SR)
    y[-k:] *= np.linspace(1, 0, k)[:, None]
    return (y * (10 ** (-1 / 20) / max(np.abs(y).max(), 1e-9))).astype(np.float32)


def render(style, hits, g):
    return bus(sequence(style, hits, g), g)


def main(argv):
    import cma
    dna_p, picks, outdir, style = argv[:4]
    evals = int(argv[4]) if len(argv) > 4 else 120
    target = acou.load_dna(dna_p)["drum_loop/drums"]
    hits = load(picks, style)
    best = [99.0, None, None]

    def dec(u):
        return {n: lo + (hi - lo) * float(np.clip(u[i], 0, 1)) for i, (n, lo, hi) in enumerate(BUS)}

    def f(u):
        g = dec(u)
        z = acou.zscores(acou.measure(render(style, hits, g), "drum_loop", "drums"), target, acou.LOOP_MEASURES)
        s = acou.score(z)
        if s < best[0]:
            best[:] = [s, g, z]
        return s

    es = cma.CMAEvolutionStrategy([0.5] * len(BUS), 0.25, {"bounds": [0, 1], "seed": 11, "maxfevals": evals,
                                                           "verbose": -9, "popsize": 10})
    while not es.stop():
        us = es.ask()
        es.tell(us, [f(u) for u in us])
    import soundfile as sf
    os.makedirs(outdir, exist_ok=True)
    name = "%s-boucle-01" % style
    sf.write(os.path.join(outdir, name + ".wav"), render(style, hits, best[1]), SR, subtype="FLOAT")
    print(json.dumps({"name": name, "style": style, "family": "boucle", "dna_family": "drum_loop/drums",
                      "bpm": P[style]["bpm"], "score": best[0], "z": best[2], "params": best[1]}), flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
