"""Bass variants per style, optimised towards each style's measured DNA.

usage: python -I cible.py <targets.tsv> <out_dir> [n_variants] [style ...]

targets.tsv: columns famille, [style], mesure, p10..p90 (Essentia recipe of
essentia_base.py). With a style column, a (famille, style) row wins over the
family-wide row (style "" or missing) when it exists.

Families (Benjamin's zone B, "for every style but adapted"): sub, reese, 808
held notes and one 2-bar bass phrase per variant. Each variant is an
independent CMA-ES run from a style prior (seeded from what Benjamin kept on
the previous page) with its own seed and root; loss = Essentia distance to the
style target; librosa counter-measure recorded. Phrases are optimised on the
phrase render itself towards melodic_loop/bass of the style.
Output: <out_dir>/<style>-<family>-NN.npy + <out_dir>/<style>.json
"""
import csv
import json
import math
import os
import sys
from multiprocessing import Pool

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import adn  # noqa: E402
import render as R  # noqa: E402
import zb  # noqa: E402

# page style -> names the sound base may use for it
ALIAS = {"lo-fi": ["lofi", "lo-fi"], "reggaeton-afro": ["reggaeton", "afro-house", "afrobeats", "amapiano"],
         "boom-bap": ["boom-bap", "hiphop", "hip-hop"], "dnb": ["dnb", "drum-and-bass"]}

# Priors in [0,1]^n per voice. Sub/reese start from what Benjamin kept
# (techno sub, dnb/edm reese); 808 from the trap/drill ones.
SUB = {"_": [0.4, 0.1, 0.3, 0.2, 0.2, 0.5, 0.9, 0.5, 0.1],
       "trap": [0.35, 0.05, 0.35, 0.2, 0.2, 0.6, 0.95, 0.6, 0.05],
       "drill": [0.4, 0.1, 0.4, 0.3, 0.2, 0.5, 0.9, 0.6, 0.15],
       "lo-fi": [0.25, 0.1, 0.2, 0.0, 0.3, 0.4, 0.6, 0.4, 0.3],
       "boom-bap": [0.3, 0.1, 0.3, 0.1, 0.3, 0.4, 0.7, 0.4, 0.25],
       "pop": [0.35, 0.1, 0.25, 0.1, 0.25, 0.45, 0.8, 0.45, 0.2],
       "house": [0.45, 0.15, 0.35, 0.3, 0.2, 0.35, 0.7, 0.35, 0.2],
       "dnb": [0.4, 0.1, 0.35, 0.25, 0.2, 0.55, 0.95, 0.55, 0.1]}
REESE = {"_": [0.5, 0.6, 0.4, 0.3, 0.3, 0.5, 0.4, 0.3, 0.2, 0.9, 0.5, 0.4],
         "edm": [0.4, 0.5, 0.6, 0.2, 0.4, 0.7, 0.6, 0.4, 0.2, 0.9, 0.4, 0.6],
         "techno": [0.35, 0.5, 0.35, 0.4, 0.4, 0.6, 0.6, 0.3, 0.15, 0.8, 0.4, 0.3],
         "lo-fi": [0.3, 0.4, 0.15, 0.1, 0.1, 0.2, 0.2, 0.2, 0.3, 0.8, 0.5, 0.2],
         "pop": [0.3, 0.4, 0.3, 0.15, 0.2, 0.3, 0.2, 0.2, 0.2, 0.85, 0.5, 0.3],
         "boom-bap": [0.3, 0.4, 0.2, 0.1, 0.15, 0.35, 0.3, 0.2, 0.25, 0.8, 0.5, 0.2],
         "trap": [0.4, 0.5, 0.3, 0.2, 0.3, 0.6, 0.5, 0.3, 0.15, 0.9, 0.5, 0.4],
         "drill": [0.45, 0.5, 0.35, 0.25, 0.35, 0.65, 0.5, 0.35, 0.15, 0.9, 0.4, 0.5]}
B808 = {"_": [0.5, 0.4, 0.5, 0.2, 0.35, 0.3, 0.1, 0.0],
        "drill": [0.4, 0.3, 0.45, 0.15, 0.5, 0.35, 0.15, 0.9],
        "reggaeton-afro": [0.35, 0.3, 0.25, 0.3, 0.3, 0.3, 0.1, 0.0],
        "boom-bap": [0.3, 0.3, 0.3, 0.25, 0.2, 0.25, 0.15, 0.0],
        "lo-fi": [0.2, 0.3, 0.3, 0.1, 0.1, 0.15, 0.2, 0.0],
        "techno": [0.45, 0.2, 0.2, 0.4, 0.5, 0.4, 0.1, 0.0],
        "house": [0.4, 0.25, 0.25, 0.3, 0.3, 0.35, 0.15, 0.0],
        "edm": [0.55, 0.3, 0.35, 0.35, 0.55, 0.45, 0.15, 0.0],
        "dnb": [0.5, 0.25, 0.3, 0.3, 0.45, 0.4, 0.1, 0.3],
        "pop": [0.35, 0.3, 0.35, 0.2, 0.25, 0.3, 0.15, 0.0]}
PRIOR = {"sub": SUB, "reese": REESE, "808": B808}
for _k, _v in {"basse-synthe": {"_": [0.5, 0.25, 0.85, 0.7, 0.3, 0.1, 0.5, 0.5, 0.1, 0.4, 0.3, 0.4, 0.2]}}.items():
    PRIOR[_k] = _v  # techno phrase (kept) voice

# style: bpm, roots, phrase voice, phrase (beat, len_beats, semitone, vel) over 8 beats
STYLES = {
    "trap": (140, [30, 29, 31, 28, 32, 27], "808",
             [(0, 1.5, 0, 120), (1.75, 0.5, 0, 100), (2.5, 1.5, 0, 115), (4, 2, 3, 120), (6.5, 1, -2, 110)]),
    "drill": (142, [27, 28, 26, 29, 30, 25], "808",
              [(0, 0.75, 0, 120), (1, 0.5, 3, 110), (1.5, 1.25, 0, 115), (3, 0.5, 7, 110), (4, 0.75, 0, 120),
               (5.5, 1, 5, 115), (7, 0.75, -2, 110)]),
    "boom-bap": (90, [31, 33, 29, 34, 30, 32], "sub",
                 [(0, 0.9, 0, 115), (1.5, 0.4, 0, 95), (2, 0.9, 3, 110), (3.5, 0.4, 5, 95), (4, 1.4, 7, 115),
                  (5.75, 0.25, 5, 90), (6, 0.9, 3, 110), (7.25, 0.5, -2, 100)]),
    "house": (124, [33, 31, 36, 34, 32, 35], "basse-synthe",
              [(b + 0.5, 0.35, s, 110) for b, s in zip(range(8), [0, 0, 12, 0, 3, 3, 15, 5])]),
    "techno": (130, [33, 29, 31, 30, 32, 34], "basse-synthe",
               [(i * 0.25 + 0.25, 0.18, (0, 0, 12, 0)[i % 4], 90 + 20 * (i % 4 == 1)) for i in range(32) if i % 4 != 0]),
    "dnb": (174, [29, 28, 31, 30, 27, 32], "reese",
            [(0, 2.5, 0, 115), (3, 1, 3, 110), (4, 2.5, -2, 115), (7, 1, 5, 110)]),
    "edm": (128, [33, 34, 31, 32, 35, 30], "reese",
            [(b * 0.5, 0.3, (0, 12)[b % 2] + (0, 0, 0, 0, -4, -4, 3, 3)[b // 2], 110) for b in range(16) if b % 2 == 1]),
    "pop": (110, [33, 36, 31, 34, 35, 32], "sub",
            [(b * 0.5, 0.4, s, 100 + 10 * (b % 2 == 0)) for b, s in
             zip(range(16), [0, 0, 0, 7, 5, 5, 5, 4, -3, -3, -3, 2, -5, -5, 2, 4])]),
    "lo-fi": (82, [33, 31, 29, 32, 30, 34], "sub",
              [(0, 1.5, 0, 105), (1.75, 0.5, 7, 85), (2.5, 1.0, 10, 95), (4, 1.5, 5, 105), (5.75, 0.5, 4, 85),
               (6.5, 1.25, 2, 95)]),
    "reggaeton-afro": (95, [33, 31, 34, 32, 30, 35], "808",
                       [(0, 0.6, 0, 120), (0.75, 0.25, 0, 100), (1.5, 0.4, 0, 110), (2, 0.6, 0, 120),
                        (2.75, 0.25, 0, 100), (3.5, 0.4, 3, 110), (4, 0.6, -2, 120), (4.75, 0.25, -2, 100),
                        (5.5, 0.4, -2, 110), (6, 0.6, -4, 120), (6.75, 0.25, -4, 100), (7.5, 0.4, -4, 110)]),
}
FAMILIES = ["sub", "reese", "808", "phrase"]
HOLD = {"808": (1.3, 2.0), "sub": (1.6, 2.2), "reese": (1.8, 2.4), "basse-synthe": (0.6, 1.0)}
ITER = int(os.environ.get("CIBLE_ITER", "30"))

T = None  # targets


PREF = {"reese": {"width_db": "p90", "sub120": "p90", "low_beat": "p25"},
        "sub": {"decay20_s": "p75", "lufs": "p75", "n_peaks": "p75"},
        "phrase": {"low_beat": "p25", "centroid_hz": "p75", "lowflux": "p25"}}


def init(path):
    global T
    T = zb.load(path)


def tgt(family, style):
    tg, used = zb.target(T, family, style)
    pref = PREF.get(family, {})
    return {k: (dict(v, p50=v[pref[k]]) if k in pref else v) for k, v in tg.items()}, used


def prior(voice, style):
    d = PRIOR[voice]
    return np.array(d.get(style) or d["_"], float)


ALT = {"house": "sub", "techno": "sub", "dnb": "basse-synthe", "edm": "basse-synthe", "trap": "sub",
       "drill": "sub", "reggaeton-afro": "sub", "pop": "basse-synthe", "lo-fi": "basse-synthe",
       "boom-bap": "basse-synthe"}


def voice_of(family, style, k):
    if family != "phrase":
        return family
    return ALT[style] if k % 2 else STYLES[style][2]


def events(family, style, root, k=0):
    bpm, _, _, pat = STYLES[style]
    voice = voice_of(family, style, k)
    if family == "phrase":
        beat = 60.0 / bpm
        return voice, [(b * beat, max(0.05, L * beat), root + s, v) for (b, L, s, v) in pat], 8 * beat + 0.5
    h, tot = HOLD[family]
    return family, [(0.0, h, root, 112)], tot


def render(family, style, root, x, k=0):
    voice, ev, tot = events(family, style, root, k)
    return R.render(voice, x, ev, tot, slide=(style == "drill"))


REESE_FREE = {"centroid_hz", "rolloff_hz", "high4k_db", "n_peaks", "attack_ms", "decay20_s"}


def keys(family, style=None):
    if family == "reese":  # kept reeses: wide and heavy; length and brightness free
        ks = [k for k in zb.ONESHOT if k not in REESE_FREE]
    else:
        ks = list(zb.LOOP if family == "phrase" else zb.ONESHOT)
    tg, used = zb.target(T, family, style) if style else ({}, None)
    if tg.get("n_peaks", {}).get("p50", 0) >= 58:  # capped at 60: no information
        ks = [k for k in ks if k != "n_peaks"]
    if style == "edm" and used == "*":  # the generic row is mostly dubstep
        ks = [k for k in ks if k not in ("n_peaks", "high4k_db")]
    return ks


def loss(family, style, y):
    tg, _ = tgt(family, style)
    d, _ = zb.distance(zb.measure(y), tg, keys(family, style))
    return d if np.isfinite(d) else 99.0


def run(job):
    import cma
    style, family, k, root = job
    voice = voice_of(family, style, k)
    rng = np.random.default_rng(7919 * k + root)
    x0 = np.clip(prior(voice, style) + rng.normal(0, 0.12 if k else 0.0, R.dims(voice)), 0.02, 0.98)
    es = cma.CMAEvolutionStrategy(x0.tolist(), 0.2, {"bounds": [0, 1], "popsize": 10, "maxiter": ITER,
                                                      "seed": 1000 * k + root + 1, "verbose": -9})
    d0 = loss(family, style, render(family, style, root, x0, k))
    best = (d0, x0)
    while not es.stop():
        xs = es.ask()
        fs = []
        for x in xs:
            try:
                f = loss(family, style, render(family, style, root, np.array(x), k))
            except Exception:
                f = 99.0
            fs.append(f)
            if f < best[0]:
                best = (f, np.array(x))
        es.tell(xs, fs)
    return style, family, k, root, float(best[0]), best[1].tolist(), float(d0)


def score(family, style, y):
    m = zb.measure(y)
    tg, used = tgt(family, style)
    d, z = zb.distance(m, tg, keys(family, style))
    mf = "phrase" if family == "phrase" else family
    try:
        ok, n = adn.agreement(adn.measure(y, mf), adn.counter_measure(y, mf))
    except Exception:
        ok, n = 0, 0
    return {"distance": round(d, 3), "target": zb.FAM[family] + "/" + (used if used != "*" else "tous"),
            "z": {a: round(b, 2) for a, b in z.items()}, "librosa_agree": f"{ok}/{n}",
            "measures": {k: round(float(v), 4) for k, v in m.items()}}


def main():
    tsv, out = sys.argv[1:3]
    n = int(sys.argv[3]) if len(sys.argv) > 3 else 8
    styles = sys.argv[4:] or list(STYLES)
    fams = os.environ.get("CIBLE_FAMS", ",".join(FAMILIES)).split(",")
    os.makedirs(out, exist_ok=True)
    init(tsv)
    jobs = [(st, f, k, STYLES[st][1][k % 6] + 12 * (k >= 6 and f != "808" and f != "phrase") * 0)
            for st in styles for f in fams for k in range(n)]
    jobs.sort(key=lambda j: j[1] != "phrase")
    with Pool(int(os.environ.get("CIBLE_PROCS", "4")), initializer=init, initargs=(tsv,)) as pool:
        res = pool.map(run, jobs, chunksize=1)
    for st in styles:
        items = []
        for (s_, f, k, root, d, x, d0) in sorted(res, key=lambda r: (r[1], r[4])):
            if s_ != st:
                continue
            y = render(f, st, root, np.array(x), k)
            name = f"{st}-{f}-{k + 1:02d}"
            np.save(os.path.join(out, name + ".npy"), y)
            items.append({"name": name, "style": st, "family": f, "voice": voice_of(f, st, k),
                          "root_midi": root, "bpm": STYLES[st][0] if f == "phrase" else None, "x": x,
                          "start_distance": round(d0, 3), **score(f, st, y)})
        with open(os.path.join(out, st + ".json"), "w") as fh:
            json.dump(items, fh, indent=1, default=float)
        print(st, len(items), "best", {f: min([i["distance"] for i in items if i["family"] == f] or [None])
                                         for f in fams}, flush=True)


if __name__ == "__main__":
    main()
