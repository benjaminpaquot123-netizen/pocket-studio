"""Make candidate acoustic one-shots for every style x family (pycma -> DNA).

  PS_BASE=.../base.py python -I fabrique.py DNA.tsv SAMPLES CACHE OUTDIR [N_CAND] [EVALS]

Writes OUTDIR/<style>-<family>-c<i>.wav (peak -1 dBFS) and OUTDIR/cands.jsonl.
"""
import json
import os
import random
import sys
from multiprocessing import Pool

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import acou  # noqa: E402

# family -> (DNA family, [(inst, art)], bounds overrides)
CYM = {"hpf": (150, 4000), "highshelf": (-6, 18), "att_ms": (0.3, 100)}
FAMILIES = {
    "kick": ("drum_oneshot/kick", [("kick", "snoff"), ("kick", "snon")], {"length": (0.25, 0.8)}),
    "caisse-claire": ("drum_oneshot/snare", [("snare", "center"), ("snare", "offcenter"), ("snare", "rimshot")],
                      {"length": (0.15, 1.0)}),
    "tom": ("drum_oneshot/tom", [("htom", "center"), ("ltom", "center"), ("ltom", "offcenter")],
            {"length": (0.4, 1.6)}),
    "charleston-ferme": ("drum_oneshot/hat_closed", [("hh", "closed")], dict(CYM, length=(0.08, 0.6))),
    "charleston-ouvert": ("drum_oneshot/hat_open", [("hh", "open"), ("hh", "half")], dict(CYM, length=(0.15, 1.3))),
    "crash": ("drum_oneshot/crash", [("crash", "crash"), ("crash", "sizzle")], dict(CYM, length=(1.0, 3.2))),
    "ride": ("drum_oneshot/ride", [("ride", "ride"), ("ride", "bell")], dict(CYM, length=(0.35, 1.0))),
}

# style -> bounds overrides (character), extra sources per family
STYLES = {
    "rock": {"b": {"g_room": (-20, 6), "g_lofi": (-60, -30), "drive": (0, 15), "ratio": (2, 8), "tune": (-2, 1)},
             "src": {"caisse-claire": [("snare", "rimshot"), ("snare", "center")]}},
    "pop": {"b": {"g_room": (-40, -6), "g_lofi": (-60, -30), "tune": (-1, 2), "lpf": (8000, 20000)}, "src": {}},
    "funk": {"b": {"g_room": (-40, -12), "g_lofi": (-60, -30), "tune": (0, 3), "drive": (0, 8)},
             "src": {"caisse-claire": [("snare", "rimshot"), ("snare", "center")]}},
    "jazz-soul": {"b": {"g_room": (-12, 6), "g_oh": (-6, 6), "g_lofi": (-60, -30), "ratio": (1, 2.5),
                        "drive": (0, 3), "tune": (-1, 2)},
                  "src": {"caisse-claire": [("snare", "center"), ("snare", "crossstick"), ("snare", "offcenter")],
                          "ride": [("ride", "ride"), ("flatride", "crash")],
                          "crash": [("crash", "sizzle"), ("crash", "crash")]}},
    "lo-fi": {"b": {"lpf": (2500, 9000), "crush": (8, 13), "lofi_sr": (11025, 32000), "g_lofi": (-20, 0),
                    "drive": (0, 20)}, "cym_b": {"lpf": (6000, 13000)}, "src": {}},
    "boom-bap": {"b": {"drive": (5, 25), "ratio": (3, 10), "lpf": (5000, 16000), "crush": (12, 16),
                       "tune": (-2, 0), "lofi_sr": (22050, 44100)}, "cym_b": {"lpf": (9000, 18000)}, "src": {}},
}


def candidates(bank, style, fam, n):
    srcs = STYLES[style]["src"].get(fam) or FAMILIES[fam][1]
    pool = []
    for inst, art in srcs:
        ks = bank.find(inst, art)
        pool.append(ks[len(ks) // 2:] or ks)  # upper half of the velocities
    rnd = random.Random("%s/%s" % (style, fam))
    out, i = [], 0
    while len(out) < n and any(pool):
        p = pool[i % len(pool)]
        if p:
            k = rnd.choice(p)
            p.remove(k)
            out.append(k)
        i += 1
    return out


def job(a):
    dna_p, samples, cache, outdir, style, fam, ci, key, evals = a
    dna = acou.load_dna(dna_p)
    bank = acou.Bank(samples, cache)
    dfam, _, fb = FAMILIES[fam]
    ov = dict(fb)
    ov.update(STYLES[style]["b"])
    if fam in ("charleston-ferme", "charleston-ouvert", "crash", "ride"):
        ov.update(STYLES[style].get("cym_b", {}))
    b = acou.bounds_for(ov)
    s, p, z = acou.optimise(bank.mics(key), dna[dfam], b, seed=1000 + ci, evals=evals)
    name = "%s-%s-c%d" % (style, fam, ci)
    if p is not None:
        import soundfile as sf
        sf.write(os.path.join(outdir, name + ".wav"), acou.render(bank.mics(key), p), acou.SR, subtype="FLOAT")
    return {"name": name, "style": style, "family": fam, "dna_family": dfam, "source_key": key,
            "score": s, "z": z, "params": p}


def main(argv):
    dna_p, samples, cache, outdir = argv[:4]
    n = int(argv[4]) if len(argv) > 4 else 5
    evals = int(argv[5]) if len(argv) > 5 else 450
    only = argv[6].split(",") if len(argv) > 6 else None
    os.makedirs(outdir, exist_ok=True)
    bank = acou.Bank(samples, cache)
    jobs = []
    for style in STYLES:
        if only and style not in only:
            continue
        for fam in FAMILIES:
            for ci, key in enumerate(candidates(bank, style, fam, n)):
                bank.mics(key)  # fill the cache before forking
                jobs.append((dna_p, samples, cache, outdir, style, fam, ci, key, evals))
    with Pool(4) as pool, open(os.path.join(outdir, "cands.jsonl"), "a") as f:
        for r in pool.imap_unordered(job, jobs):
            f.write(json.dumps(r) + "\n")
            f.flush()
            print("%-40s %.2f" % (r["name"], r["score"]), flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
