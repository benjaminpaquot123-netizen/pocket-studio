"""One 4-bar loop per style, built only from that style's new hits (the best
take of each family, re-rendered from its parameters), then scored against
the drum_loop/drums DNA.

  python loops.py --base BASE_PY --dna FAMILLES_TSV --out DIR   (after make.py export)
"""
import argparse
import json
import os
import sys

import numpy as np
import pedalboard as pb

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import make  # noqa: E402
import styles  # noqa: E402
import synth  # noqa: E402

SR = synth.SR
# 16 steps per bar; each lane: list of (step, velocity). A pattern string per
# bar uses "x" = full hit, "o" = soft hit, "." = rest.


def lane(*bars):
    out = []
    for b, s in enumerate(bars):
        for i, c in enumerate(s):
            if c in "xo":
                out.append((16 * b + i, 1.0 if c == "x" else 0.55))
    return out


def rep(s, n=4):
    return [s] * n


PATTERNS = {
    "trap": {"swing": 0.0, "lanes": {
        "kick": lane("x......x..x.....", "x.........x..x..", "x......x..x.....", "x.....x...x...x."),
        "snare": lane(*rep("........x.......")),
        "clap": lane(*rep("........x.......")),
        "hat_closed": lane("x.x.x.x.x.x.x.x.", "x.x.x.x.x.xxx.x.", "x.x.x.x.x.x.x.x.", "x.x.x.x.xxxxxxxx"),
        "hat_open": lane("", "", "", "..............x."),
        "perc": lane("...o............", "", "...o............", "......o.....o..."),
    }},
    "drill": {"swing": 0.0, "lanes": {
        "kick": lane("x..........x....", "x.....x.........", "x..........x....", "x.....x....x...."),
        "snare": lane("........x.......", "........x.....o.", "........x.......", "........x..o..x."),
        "clap": lane(*rep("........x.......")),
        "hat_closed": lane(*rep("x..x..x.x..x..x.")),
        "hat_open": lane("", "", "", "...........x...."),
        "perc": lane("..o.......o.....", "", "..o.......o.....", ""),
    }},
    "boom-bap": {"swing": 0.22, "lanes": {
        "kick": lane("x.........x.....", "x......x..x.....", "x.........x.....", "x......x..x..x.."),
        "snare": lane(*rep("....x.......x...")),
        "clap": lane(),
        "hat_closed": lane(*rep("x.o.x.o.x.o.x.o.")),
        "hat_open": lane("", "..............o.", "", "..............o."),
        "perc": lane("", "", "", ".............o.o"),
    }},
    "house": {"swing": 0.08, "lanes": {
        "kick": lane(*rep("x...x...x...x...")),
        "snare": lane(),
        "clap": lane(*rep("....x.......x...")),
        "hat_closed": lane(*rep("oooooooooooooooo")),
        "hat_open": lane(*rep("..x...x...x...x.")),
        "perc": lane(".....o.....o....", "...o.....o......", ".....o.....o....", "...o.....o...o.."),
    }},
    "techno": {"swing": 0.0, "lanes": {
        "kick": lane(*rep("x...x...x...x...")),
        "snare": lane(),
        "clap": lane(*rep("....o.......o...")),
        "hat_closed": lane(*rep("x.o.x.o.x.o.x.o.")),
        "hat_open": lane(*rep("..x...x...x...x.")),
        "perc": lane(*rep("...x..x....x..x.")),
    }},
    "drum-and-bass": {"swing": 0.0, "lanes": {
        "kick": lane("x.........x.....", "x.x.......x.....", "x.........x.....", "x.x.......x..x.."),
        "snare": lane("....x.......x...", "....x..o....x...", "....x.......x...", "....x..o....x.oo"),
        "clap": lane(),
        "hat_closed": lane(*rep("x.x.x.x.x.x.x.x.")),
        "hat_open": lane("", "", "", "..............x."),
        "perc": lane("......o.......o.", "", "......o.......o.", ""),
    }},
    "edm": {"swing": 0.0, "lanes": {
        "kick": lane(*rep("x...x...x...x...")),
        "snare": lane("", "", "", "x.x.x.x.xxxxxxxx"),
        "clap": lane(*rep("....x.......x...")),
        "hat_closed": lane(*rep("o.o.o.o.o.o.o.o.")),
        "hat_open": lane(*rep("..x...x...x...x.")),
        "perc": lane("", "...o.......o....", "", "...o.......o...."),
    }},
    "reggaeton-afro": {"swing": 0.05, "lanes": {
        "kick": lane(*rep("x...x...x...x...")),
        "snare": lane(*rep("...x..x....x..x.")),
        "clap": lane("", "", "", "...x..x....x..x."),
        "hat_closed": lane(*rep("x.o.x.o.x.o.x.o.")),
        "hat_open": lane("", "..............x.", "", "..............x."),
        "perc": lane(*rep("..o..x.o..o..x..")),
    }},
    "lo-fi": {"swing": 0.3, "lanes": {
        "kick": lane("x........x......", "x.....x...x.....", "x........x......", "x.....x......x.."),
        "snare": lane(*rep("....x.......x...")),
        "clap": lane(),
        "hat_closed": lane(*rep("x.o.x.o.x.o.x.oo")),
        "hat_open": lane("", "", "", "..............o."),
        "perc": lane("", ".........o......", "", ".........o......"),
    }},
}
GAIN = {"kick": 1.0, "snare": 0.75, "clap": 0.55, "hat_closed": 0.32, "hat_open": 0.3, "perc": 0.35}


def build(style, hits):
    pat = PATTERNS[style]
    step = 60.0 / styles.STYLES[style]["bpm"] / 4
    n = int(round(64 * step * SR))
    mix = np.zeros(n + SR * 2)
    rng = np.random.default_rng(7)
    for fam, ev in pat["lanes"].items():
        h = hits[fam]
        for s, v in ev:
            t = s * step + (pat["swing"] * step if s % 2 else 0.0) + rng.normal(0, 0.002 if pat["swing"] else 0)
            i = max(0, int(t * SR))
            g = GAIN[fam] * v * (1 + rng.normal(0, 0.04))
            if fam == "hat_closed":  # choke: an open hat is cut by the next closed hat
                pass
            mix[i:i + len(h)] += g * h[:len(mix) - i]
    mix = mix[:n].astype(np.float32)
    bus = pb.Pedalboard([pb.Compressor(threshold_db=-14, ratio=3, attack_ms=8, release_ms=90)])
    return bus(mix[None, :], SR)[0]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True)
    ap.add_argument("--dna", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    make.init(a.base, a.dna)
    res = json.load(open(os.path.join(a.out, "results.json")))
    res = [r for r in res if r["family"] != "loop"]
    for s in styles.STYLES:
        hits = {}
        for f in synth.TABLES:
            r = next(r for r in res if r["style"] == s and r["family"] == f and r["file"].endswith("-01.mp3"))
            hits[f] = make.from_mp3(os.path.join(a.out, r["file"])).astype(np.float64)
        y = build(s, hits)
        name = "%s-boucle-01.mp3" % s
        path = os.path.join(a.out, name)
        make.to_mp3(y, path)
        sc = make.final_score(path, "drum_loop", "drum_loop/drums")
        yy = make.from_mp3(path)
        bpm = make.J.measure(yy, "drum_loop", "drum_loop/drums").get("bpm")
        res.append(dict(file=name, style=s, family="loop", bpm=styles.STYLES[s]["bpm"],
                        bpm_measured=round(bpm, 1) if bpm else None, **sc))
        print(name, sc["essentia_sd"], sc["in_p10_p90"], bpm, flush=True)
    json.dump(res, open(os.path.join(a.out, "results.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
