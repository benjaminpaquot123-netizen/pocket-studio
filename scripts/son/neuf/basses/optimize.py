"""CMA-ES search of new bass sounds towards each family's measured DNA.

usage: python -I optimize.py <familles.tsv> <pizz_dir> <out_dir> [style ...]

For each style, for each family the style uses, three independent CMA-ES runs
(different seeds, roots and identity priors) each give one held note. The
loss is the Essentia distance to the family DNA (adn.distance); librosa is the
counter-measure, recorded for the best candidate. Then one 2-bar phrase per
style is rendered with each of the style's patches and the one closest to the
bass-loop DNA is kept. Results: <out_dir>/<name>.npy (float32 stereo 44.1 kHz)
and <out_dir>/<style>.json.
"""
import json
import os
import sys
from multiprocessing import Pool

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import adn  # noqa: E402
import render as R  # noqa: E402

# Identity priors (x0 in [0,1]^n, per voice type) refined per style; CMA moves
# from there with sigma 0.2 inside [0, 1].
P = {
    "808": {"trap": [0.5, 0.4, 0.5, 0.2, 0.35, 0.3, 0.1, 0.0], "drill": [0.4, 0.3, 0.45, 0.15, 0.5, 0.35, 0.15, 0.9],
            "reggaeton": [0.35, 0.3, 0.25, 0.3, 0.3, 0.3, 0.1, 0.0]},
    "sub": {"_": [0.4, 0.1, 0.3, 0.2, 0.2, 0.5, 0.9, 0.5, 0.1], "drill": [0.4, 0.1, 0.4, 0.3, 0.2, 0.5, 0.9, 0.6, 0.15],
            "lofi": [0.25, 0.1, 0.2, 0.0, 0.3, 0.4, 0.6, 0.4, 0.3]},
    "basse-synthe": {"house": [0.9, 0.35, 0.3, 0.5, 0.4, 0.2, 0.3, 0.1, 0.1, 0.3, 0.5, 0.4, 0.4],
                     "techno": [-0.0 + 0.5, 0.25, 0.85, 0.7, 0.3, 0.1, 0.5, 0.5, 0.1, 0.4, 0.3, 0.4, 0.2],
                     "edm": [0.5, 0.6, 0.3, 0.4, 0.5, 0.4, 0.7, 0.8, 0.1, 0.6, 0.8, 0.4, 0.3],
                     "pop": [0.7, 0.4, 0.2, 0.3, 0.5, 0.3, 0.2, 0.0, 0.2, 0.5, 0.6, 0.5, 0.4],
                     "reggaeton": [0.3, 0.35, 0.4, 0.6, 0.15, 0.0, 0.3, 0.2, 0.05, 0.2, 0.0, 0.3, 0.3]},
    "reese": {"dnb": [0.5, 0.6, 0.4, 0.3, 0.3, 0.5, 0.4, 0.3, 0.2, 0.9, 0.5, 0.4],
              "edm": [0.4, 0.5, 0.6, 0.2, 0.4, 0.7, 0.6, 0.4, 0.2, 0.9, 0.4, 0.6]},
    "basse-acoustique": {"pop": [0.5, 0.5, 0.6, 0.2, 0.5, 0.4, 0.5, 0.1], "lofi": [0.5, 0.6, 0.3, 0.3, 0.6, 0.3, 0.2, 0.4]},
}

# style: tempo, roots (one per run), families, phrase (beat, len_beats, semitone, vel) over 8 beats
STYLES = {
    "trap": (140, [30, 29, 31], ["808", "sub"],
             [(0, 1.5, 0, 120), (1.75, 0.5, 0, 100), (2.5, 1.5, 0, 115), (4, 2, 3, 120), (6.5, 1, -2, 110)]),
    "drill": (142, [27, 28, 26], ["808", "sub"],
              [(0, 0.75, 0, 120), (1, 0.5, 3, 110), (1.5, 1.25, 0, 115), (3, 0.5, 7, 110), (4, 0.75, 0, 120),
               (5.5, 1, 5, 115), (7, 0.75, -2, 110)]),
    "house": (124, [33, 31, 36], ["basse-synthe", "sub"],
              [(b + 0.5, 0.35, s, 110) for b, s in zip(range(8), [0, 0, 12, 0, 3, 3, 15, 5])]),
    "techno": (130, [33, 29, 31], ["basse-synthe", "sub"],
               [(i * 0.25 + 0.25, 0.18, (0, 0, 12, 0)[i % 4], 90 + 20 * (i % 4 == 1)) for i in range(32) if i % 4 != 0]),
    "dnb": (174, [29, 28, 31], ["reese", "sub"],
            [(0, 2.5, 0, 115), (3, 1, 3, 110), (4, 2.5, -2, 115), (7, 1, 5, 110)]),
    "edm": (128, [33, 34, 31], ["basse-synthe", "reese"],
            [(b * 0.5 + 0.5 * (b % 2 == 0) * 0 + 0.0, 0.3, (0, 12)[b % 2] + (0, 0, 0, 0, -4, -4, 3, 3)[b // 2], 110)
             for b in range(16) if b % 2 == 1]),
    "pop": (110, [33, 36, 31], ["basse-acoustique", "basse-synthe"],
            [(b * 0.5, 0.4, s, 100 + 10 * (b % 2 == 0)) for b, s in
             zip(range(16), [0, 0, 0, 7, 5, 5, 5, 4, -3, -3, -3, 2, -5, -5, 2, 4])]),
    "lofi": (82, [33, 31, 29], ["basse-acoustique", "sub"],
             [(0, 1.5, 0, 105), (1.75, 0.5, 7, 85), (2.5, 1.0, 10, 95), (4, 1.5, 5, 105), (5.75, 0.5, 4, 85), (6.5, 1.25, 2, 95)]),
    "reggaeton": (95, [33, 31, 34], ["808", "basse-synthe"],
                  [(0, 0.6, 0, 120), (0.75, 0.25, 0, 100), (1.5, 0.4, 0, 110), (2, 0.6, 0, 120), (2.75, 0.25, 0, 100),
                   (3.5, 0.4, 3, 110), (4, 0.6, -2, 120), (4.75, 0.25, -2, 100), (5.5, 0.4, -2, 110), (6, 0.6, -4, 120),
                   (6.75, 0.25, -4, 100), (7.5, 0.4, -4, 110)]),
}
# held-note length (s) and render length (s) per family
HOLD = {"808": (1.3, 2.0), "sub": (0.6, 1.0), "basse-synthe": (0.45, 0.9), "reese": (0.7, 1.0),
        "basse-acoustique": (0.6, 1.0)}

TARGETS = None
PIZZ = None


def prior(kind, style):
    d = P[kind]
    return np.array(d.get(style) or d.get("_") or next(iter(d.values())), float)


def held(kind, style, root, x):
    h, tot = HOLD[kind]
    return R.render(kind, x, [(0.0, h, root, 112)], tot, pizz_root=PIZZ, slide=False)


def loss(kind, y):
    m = adn.measure(y, kind)
    d, _ = adn.distance(m, TARGETS[adn.FAMILY[kind][0]])
    return d if np.isfinite(d) else 99.0


def run(job):
    import cma
    style, kind, k, root = job
    x0 = np.clip(prior(kind, style) + np.random.default_rng(k).normal(0, 0.08, R.dims(kind)), 0.02, 0.98)
    opts = {"bounds": [0, 1], "popsize": 10, "maxiter": 14, "seed": 1000 * k + root, "verbose": -9}
    es = cma.CMAEvolutionStrategy(x0.tolist(), 0.2, opts)
    best = (loss(kind, held(kind, style, root, x0)), x0)
    while not es.stop():
        xs = es.ask()
        fs = []
        for x in xs:
            try:
                f = loss(kind, held(kind, style, root, np.array(x)))
            except Exception:
                f = 99.0
            fs.append(f)
            if f < best[0]:
                best = (f, np.array(x))
        es.tell(xs, fs)
    return style, kind, k, root, float(best[0]), best[1].tolist(), float(loss(kind, held(kind, style, root, x0)))


def init(tsv, pizz):
    global TARGETS, PIZZ
    TARGETS = adn.load_targets(tsv)
    PIZZ = pizz


def score(kind, y):
    m = adn.measure(y, kind)
    d, z = adn.distance(m, TARGETS[adn.FAMILY[kind][0]])
    lib = adn.counter_measure(y, kind)
    ok, n = adn.agreement(m, lib)
    return {"essentia_sd": round(d, 3), "z": {a: round(b, 2) for a, b in z.items()},
            "librosa_agree": f"{ok}/{n}", "f0_hz": m.get("f0_hz"), "centroid_hz": m.get("centroid_hz")}


def phrase_events(style, root):
    bpm, _, _, pat = STYLES[style]
    beat = 60.0 / bpm
    return [(b * beat, max(0.05, L * beat), root + s, v) for (b, L, s, v) in pat], 8 * beat + 0.5


def main():
    tsv, pizz, out = sys.argv[1:4]
    styles = sys.argv[4:] or list(STYLES)
    os.makedirs(out, exist_ok=True)
    init(tsv, pizz)
    jobs = [(st, kind, k, STYLES[st][1][k]) for st in styles for kind in STYLES[st][2] for k in range(3)]
    jobs.sort(key=lambda j: j[1] != "basse-acoustique")  # slow ones first
    with Pool(4, initializer=init, initargs=(tsv, pizz)) as pool:
        res = pool.map(run, jobs, chunksize=1)
    for st in styles:
        items = []
        for (s_, kind, k, root, d, x, d0) in res:
            if s_ != st:
                continue
            y = held(kind, st, root, np.array(x))
            name = f"{st}-{kind}-{k + 1:02d}"
            np.save(os.path.join(out, name + ".npy"), y)
            items.append({"name": name, "style": st, "family": kind, "kind": "note", "root_midi": root,
                          "x": x, "start_sd": round(d0, 3), **score(kind, y)})
        # phrase: the style's first family, each of its 3 patches, keep the closest to the loop DNA
        fam = STYLES[st][2][0]
        best = None
        for it in [i for i in items if i["family"] == fam]:
            ev, tot = phrase_events(st, it["root_midi"])
            y = R.render(fam, np.array(it["x"]), ev, tot, pizz_root=PIZZ, slide=(st == "drill"))
            sc = score("phrase", y)
            if best is None or sc["essentia_sd"] < best[1]["essentia_sd"]:
                best = (y, sc, it)
        name = f"{st}-phrase-01"
        np.save(os.path.join(out, name + ".npy"), best[0])
        items.append({"name": name, "style": st, "family": "phrase", "kind": "phrase", "voice": fam,
                      "from": best[2]["name"], "bpm": STYLES[st][0], **best[1]})
        with open(os.path.join(out, st + ".json"), "w") as f:
            json.dump(items, f, indent=1)
        print(st, [(i["name"], i["essentia_sd"], i["librosa_agree"]) for i in items], flush=True)


if __name__ == "__main__":
    main()
