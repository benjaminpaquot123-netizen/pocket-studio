"""AI trial: can Stable Audio Open Small beat our synthesis on the 3 weak spots
(drum-and-bass kicks, techno sub, drum-and-bass sub)?

usage: python -I ia_essai.py CODE_DIR MODEL_DIR TARGET_DIR OUT_DIR [N_PER_FAMILY]

CODE_DIR holds generate_pack.py (scripts/packs), cibles_desc.py + base.py
(eq/son-fin-essentia-01 @ faa70cb3a), tgt.py (eq/son-cible-a) and zb.py (eq/son-cible-b).
Each candidate: generated, trimmed/faded/normalised (generate_pack.clean), then cut to a few
lengths ("raw"), and the same with a mono low-pass ("lp", subs only). Scored with the exact
production distances: tgt.score for kicks (kick/dnb), zb.distance with cible.py's sub keys and
preferences for subs. Final scores are re-measured on the exported MP3.
"""
import json
import os
import subprocess
import sys

import numpy as np

CODE, MODEL, TG, OUT = sys.argv[1:5]
N = int(sys.argv[5]) if len(sys.argv) > 5 else 48
sys.path.insert(0, CODE)
import generate_pack as gp  # noqa: E402
import tgt  # noqa: E402
import zb  # noqa: E402
from scipy.signal import butter, sosfilt  # noqa: E402

SR = 44100
tgt.load(os.path.join(TG, "zone-a-kicks-boucles.tsv"), CODE)
TB = zb.load(os.path.join(TG, "zone-b-basses.tsv"))
PREF_SUB = {"decay20_s": "p75", "lufs": "p75", "n_peaks": "p75"}  # cible.py PREF["sub"]


def sub_target(style):
    tg, used = zb.target(TB, "sub", style)
    tg = {k: (dict(v, p50=v[PREF_SUB[k]]) if k in PREF_SUB else v) for k, v in tg.items()}
    ks = list(zb.ONESHOT)
    if tg.get("n_peaks", {}).get("p50", 0) >= 58:
        ks = [k for k in ks if k != "n_peaks"]
    return tg, ks


NOTES = ["C1", "D1", "E1", "F1", "G1", "A1", "A#0", "B0"]
KICK_P = [
    "drum and bass kick drum one shot, punchy, tight, 174 bpm",
    "dnb kick, short and deep, clean sub thump, one shot",
    "jungle kick drum single hit, dry, 170 bpm",
    "neurofunk kick one shot, hard punchy transient, short tail",
    "liquid drum and bass kick drum, round and soft, single hit",
    "drum and bass kick, deep low thump with a small click, one shot, 172 bpm",
    "rollers dnb kick drum, tight low end, short decay, single shot",
    "electronic kick drum one shot, very short, deep, for drum and bass",
    "acoustic-style breakbeat kick, dry single hit, 174 bpm",
    "halftime dnb kick, heavy sub punch, one shot",
    "minimal drum and bass kick drum, clean sine thump, very short",
    "drum and bass kick with noisy beater attack, short low tail, one shot",
]
SUB_P = {
    "techno": [
        "techno sub bass, single sustained note {n}, pure sine, clean, mono",
        "deep techno sub bass one shot {n}, short, rounded, 130 bpm",
        "minimal techno sub bass hit {n}, clean low sine, short decay",
        "warehouse techno low sub bass note {n}, tight and deep",
        "techno bassline sub tone {n}, sine wave, 128 bpm, one note",
        "dark techno rumble sub bass note {n}, clean, mono",
        "hypnotic techno sub bass stab {n}, short sine pluck, 132 bpm",
        "industrial techno sub bass one shot {n}, deep and clean",
        "techno sub kick-bass tone {n}, short, sine",
        "dub techno sub bass note {n}, warm, soft attack",
        "techno sub bass hit {n}, very low, no harmonics",
        "peak time techno sub bass note {n}, punchy, mono",
    ],
    "drum-and-bass": [
        "drum and bass sub bass, sustained note {n}, clean sine, 174 bpm",
        "dnb sub bass one shot {n}, deep, rounded, mono",
        "liquid drum and bass sub bass note {n}, warm sine, long",
        "jungle sub bass note {n}, deep 808-like sine, 170 bpm",
        "neurofunk sub layer, pure sine note {n}, mono",
        "rollers dnb sub bass note {n}, smooth and steady",
        "drum and bass sub bass hit {n}, short attack, clean low end",
        "deep dnb sub bass tone {n}, no distortion, sine",
        "drum and bass bassline sub note {n}, 174 bpm, single held note",
        "halftime dnb sub bass {n}, heavy and clean",
        "dnb sub bass with slight saturation {n}, single note",
        "minimal drum and bass sub bass note {n}, pure low tone",
    ],
}
FAMILIES = {
    "dnb-kick": dict(kind="kick", style="drum-and-bass", seconds=1.0,
                     prompts=KICK_P, cuts=[None, 0.25, 0.34, 0.45]),
    "techno-sub": dict(kind="sub", style="techno", seconds=2.5,
                       prompts=SUB_P["techno"], cuts=[None, 0.5, 1.0, 1.6, 2.2]),
    "dnb-sub": dict(kind="sub", style="drum-and-bass", seconds=2.5,
                    prompts=SUB_P["drum-and-bass"], cuts=[None, 1.0, 1.6, 2.2]),
}
LP = butter(8, 160, "lowpass", fs=SR, output="sos")


def cut(a, L):
    if L is not None and len(a) > int(L * SR):
        a = a[:int(L * SR)].copy()
    f = min(int(0.01 * SR), len(a) // 4)
    a[-f:] *= np.linspace(1, 0, f)[:, None]
    return a


def lowpass(a):
    m = sosfilt(LP, a.mean(1))
    return np.stack([m, m], 1)


def score_arr(fam, a):
    a = a / max(np.abs(a).max(), 1e-9) * tgt.OUT_PEAK
    if fam["kind"] == "kick":
        m = tgt.measure_st(a)
        s, z = tgt.score(m, "kick", tgt.CASE_K[fam["style"]], "kick")
    else:
        tg, ks = sub_target(fam["style"])
        m = zb.measure(a)
        s, z = zb.distance(m, tg, ks)
    return float(s) if np.isfinite(s) else 99.0, z, m


def score_mp3(fam, path):
    with open(path, "rb") as fh:
        m = tgt.CD.from_bytes(fh.read())
    m["high4k_db"] = 10 * np.log10(max(m["high4k"], 1e-12))
    if fam["kind"] == "kick":
        s, z = tgt.score(m, "kick", tgt.CASE_K[fam["style"]], "kick")
    else:
        tg, ks = sub_target(fam["style"])
        s, z = zb.distance(m, tg, ks)
    return float(s), z, m


def to_mp3(a, path):
    a = (a / max(np.abs(a).max(), 1e-9) * tgt.OUT_PEAK).astype(np.float32)
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "2", "-i", "-",
                    "-codec:a", "libmp3lame", "-b:a", "192k", path], input=np.ascontiguousarray(a).tobytes(),
                   check=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    model = gp.Model(MODEL)
    allres = {}
    for name, fam in FAMILIES.items():
        res = []
        P = fam["prompts"]
        for i in range(N):
            p = P[i % len(P)].replace("{n}", NOTES[i % len(NOTES)])
            seed = 1000 + i
            raw = model.generate(p, fam["seconds"], seed)
            c = gp.clean(raw, True)
            if c is None:
                continue
            best = {}
            for L in fam["cuts"]:
                for proc in (["raw", "lp"] if fam["kind"] == "sub" else ["raw"]):
                    a = cut(c.copy(), L)
                    if proc == "lp":
                        a = lowpass(a)
                    s, z, _ = score_arr(fam, a)
                    if proc not in best or s < best[proc][0]:
                        best[proc] = (s, L, a, z)
            for proc, (s, L, a, z) in best.items():
                res.append(dict(i=i, prompt=p, seed=seed, proc=proc, cut_s=L, score=round(s, 3), a=a))
            print(name, i, {k: round(v[0], 2) for k, v in best.items()}, flush=True)
        allres[name] = res
    index = {"model": "Stable Audio Open Small (Stability AI), ONNX export atauser001 rev 8f9544d, fp32, 8 steps",
             "license": "Stability AI Community License (outputs usable; attribution 'Powered by Stability AI')",
             "targets": "eq/son-fin-essentia-01 @ faa70cb3a (zone-a-kicks-boucles.tsv, zone-b-basses.tsv)",
             "families": {}}
    for name, res in allres.items():
        fam = FAMILIES[name]
        fi = {"kind": fam["kind"], "style": fam["style"], "candidates": len({r["i"] for r in res}),
              "all_scores_raw": sorted(r["score"] for r in res if r["proc"] == "raw"), "kept": []}
        for proc in ("raw", "lp"):
            pool = sorted((r for r in res if r["proc"] == proc), key=lambda r: r["score"])[:8]
            for k, r in enumerate(pool, 1):
                fn = f"{name}-{proc}-{k:02d}.mp3"
                to_mp3(r["a"], os.path.join(OUT, fn))
                s, z, m = score_mp3(fam, os.path.join(OUT, fn))
                fi["kept"].append(dict(file=fn, proc=proc, prompt=r["prompt"], seed=r["seed"], cut_s=r["cut_s"],
                                       score_array=r["score"], score_mp3=round(s, 3),
                                       worst=sorted(((a, round(float(b), 1)) for a, b in z.items()),
                                                    key=lambda u: -abs(u[1]))[:4],
                                       measured={k2: round(float(v), 4) for k2, v in m.items()
                                                 if isinstance(v, (int, float, np.floating))}))
        index["families"][name] = fi
    with open(os.path.join(OUT, "index.json"), "w") as f:
        json.dump(index, f, indent=1, default=float)
    for name, fi in index["families"].items():
        for proc in ("raw", "lp"):
            sc = [k["score_mp3"] for k in fi["kept"] if k["proc"] == proc]
            if sc:
                print(name, proc, "best", min(sc), "median8", float(np.median(sc)))


if __name__ == "__main__":
    main()
