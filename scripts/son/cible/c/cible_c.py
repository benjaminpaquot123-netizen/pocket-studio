"""Zone C (synths): stab, pluck, nappe, accords, boucle (melodic loop) and
effects (montant / descendant / impact / balayage) for every big style of the
"Nouveaux sons" page, tuned toward the measured family ADN.

Synth families reuse the synthes chain (scripts/son/neuf/synthes/synthes.py:
Surge XT factory patch -> pedalboard -> loudness -> limiter, pycma on 4 Surge
and 6 pedalboard parameters, score = RMS of Essentia z-scores against the ADN
median). What changes here:
  - styles and families come from music_c.py;
  - candidates start from the patches Benjamin kept ("garder") in seeds.json
    and skip those he rejected ("bof"/"non");
  - 10 candidates per (style, family), the best 8 exported;
  - effects are synthesised in fx_c.py (no patch), tuned by pycma the same way.

  python -I cible_c.py screen OUT   # rank Surge patches per style/family
  python -I cible_c.py tune OUT     # CMA on the candidates
  python -I cible_c.py fx OUT       # effects: build + CMA
  python -I cible_c.py export OUT   # MP3 192k, peak -1 dBFS, final scores
Audio stays in OUT (outside the repository).
"""
import argparse
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(HERE)), "neuf", "synthes"))

import music_c  # noqa: E402
import synthes as S  # noqa: E402

S.music = music_c
import tgt_c  # noqa: E402

S.FAM_CATEGORIES = {
    "stab": ["Polysynths", "Plucks", "Keys", "Chords"], "pluck": ["Plucks", "Keys"],
    "nappe": ["Pads", "Polysynths"], "accords": ["Polysynths", "Keys", "Pads", "Chords"],
    "boucle": ["Leads", "Sequences"], "arpege": ["Plucks", "Polysynths", "Sequences"],
    "ambiance": ["Pads", "FX"],
}
S.FAM_ADN = {k: tgt_c.FAM_TGT[k] for k in S.FAM_CATEGORIES}
S.FAMILY_TYPE = {k: "c" for k in S.FAM_CATEGORIES}
S.N_CAND, S.N_KEEP = 9, 8
S.SCREEN_TOP = 16


def _init():
    import surgepy  # noqa: F401
    _W = S._W
    _W["surge"] = None
    _W["patch"] = None
    _W["adn"] = None


def evaluate(patch, style, family, variant, x):
    t = tgt_c.target(family, style)
    ntw = len(S.SURGE_TWEAKS)
    raw = S.render(patch, style, family, variant, x[:ntw])
    y = S.master(S.fx(raw, x[ntw:]), tgt_c.master_lufs(t))
    if y is None:
        return 99.0, None, None
    m = tgt_c.measure(y)
    return tgt_c.score(m, t), m, y


def job_export(a):
    r, name, out = a
    t = tgt_c.target(r["family"], r["style"])
    x = np.asarray(r["x"])
    ntw = len(S.SURGE_TWEAKS)
    raw = S.render(r["patch"], r["style"], r["family"], r["variant"], x[:ntw])
    y = S.master(S.fx(raw, x[ntw:]), tgt_c.master_lufs(t))
    path = os.path.join(out, "mp3", name)
    gain = 1.0
    for _ in range(4):
        dec = S.encode_mp3(y * gain, path)
        pk = 20 * math.log10(max(np.abs(dec).max(), 1e-9))
        if pk <= -0.95:
            break
        gain *= 10 ** ((-1.0 - pk - 0.05) / 20)
    m = tgt_c.measure(dec.copy())
    z = tgt_c.zscores(m, t)
    return dict(r, fichier=name, score_mp3=tgt_c.score(m, t), peak_db=pk, cible=t["_cell"],
                z={k: round(v, 2) for k, v in z.items()}, ess=m)


S._init, S.evaluate, S.job_export = _init, evaluate, job_export

SEEDS = json.load(open(os.path.join(HERE, "seeds.json")))
for _k in ("garder", "eviter"):  # arpeggios were rated together with the loops
    SEEDS[_k].setdefault("arpege", list(SEEDS[_k].get("boucle", [])))


def _patch(name):
    return os.path.join(S.PATCHES, name + ".fxp")


def pick(out, styles, families):
    """Kept patches first (if they render at all), then the best screen scores;
    a patch is used at most three times per family across styles."""
    rows = [json.loads(l) for l in open(os.path.join(out, "screen.jsonl"))]
    picks = {}
    for fam in families:
        keep = [_patch(p) for p in SEEDS["garder"].get(fam, [])]
        avoid = {_patch(p) for p in SEEDS["eviter"].get(fam, [])}
        used = {}
        for st in styles:
            r = sorted([x for x in rows if x["family"] == fam and x["style"] == st and x["score"] < 50
                        and x["patch"] not in avoid], key=lambda x: x["score"])
            ranked = [x["patch"] for x in r]
            # kept patches compete only if the screen did not reject them for this style
            order = [p for p in keep if p in ranked[:60]] + [p for p in ranked if p not in keep]
            chosen = []
            for p in order:
                if used.get(p, 0) >= 3:
                    continue
                chosen.append(p)
                used[p] = used.get(p, 0) + 1
                if len(chosen) == S.N_CAND:
                    break
            # late styles: once the screened patches are used up, reuse the best ones
            for p in order:
                if len(chosen) >= S.N_CAND:
                    break
                if p not in chosen:
                    chosen.append(p)
                    used[p] = used.get(p, 0) + 1
            picks[(st, fam)] = chosen
    return picks


S.pick = pick


def cmd_screen(out, styles, families):
    # kept patches are screened on every style, whatever their house score
    S.cmd_screen(out, styles, families)  # reference style (house) for every patch
    S.cmd_screen(out, styles, families)  # then each patch's top on the other styles
    path = os.path.join(out, "screen.jsonl")
    done = {(json.loads(l)["patch"], json.loads(l)["style"], json.loads(l)["family"]) for l in open(path)}
    jobs = [(_patch(p), st, fam, 0) for fam in families for p in SEEDS["garder"].get(fam, [])
            for st in styles if (_patch(p), st, fam) not in done and os.path.exists(_patch(p))]
    with S._pool() as pool, open(path, "a") as f:
        for r in pool.imap_unordered(S.job_screen, jobs, chunksize=2):
            f.write(json.dumps(r) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["screen", "tune", "fx", "export"])
    ap.add_argument("out")
    ap.add_argument("--styles", default=",".join(music_c.STYLE_ORDER))
    ap.add_argument("--families", default=",".join(music_c.FAMILIES))
    ap.add_argument("--budget", type=int, default=48)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    styles, fams = a.styles.split(","), a.families.split(",")
    if a.cmd == "screen":
        cmd_screen(a.out, styles, fams)
    elif a.cmd == "tune":
        S.cmd_tune(a.out, styles, fams, a.budget)
    elif a.cmd == "fx":
        import fx_c
        fx_c.run(a.out, styles, a.budget)
    else:
        S.cmd_export(a.out)


if __name__ == "__main__":
    main()
