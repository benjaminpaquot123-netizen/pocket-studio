"""Trial of the note-instrument fingerprint on our own synth sounds, then the CMA demo.

  python3 essai.py instruments RENDER_BIN   fingerprints ~13 of our synth sounds (engine on host)
  python3 essai.py cma                      the study's CMA aims at an instrument fingerprint

Read-only sources: SynthPatch.kt and SoundLibrary.kt (SYNTH_V2 patches, without the later
ProVoices touch-ups) and scripts/son/timbre/timbre.py (its cma()). Nothing is written to the repo.
"""
import math
import os
import re
import subprocess
import sys
from multiprocessing import Pool

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))  # works under python3 -I
import empreinte as E  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
MODEL = os.path.join(ROOT, "project-model", "src", "main", "kotlin", "com", "pocketstudio", "model")

# name -> (midi note, hold s, total s, is pad)
CHOSEN = {
    "Basse ronde": (36, 1.6, 4.6, False), "Basse sub": (36, 1.6, 4.6, False),
    "Reese moderne": (36, 1.6, 4.6, False), "Sub pur": (33, 1.6, 4.6, False),
    "Lead": (64, 1.6, 4.6, False), "Lead carré": (64, 1.6, 4.6, False),
    "Supersaw": (64, 1.6, 4.6, False), "Hoover": (60, 1.6, 4.6, False),
    "Nappe": (57, 8.5, 12.0, True), "Cordes": (57, 8.5, 12.0, True),
    "Pluck": (60, 1.6, 4.6, False), "Orgue": (60, 1.6, 4.6, False),
    "Piano électrique": (60, 1.6, 4.6, False),
}
PRESET_INDEX = {"Basse ronde": 0, "Lead": 1, "Nappe": 2, "Pluck": 3}


def load_patches():
    src = open(os.path.join(MODEL, "SynthPatch.kt"), encoding="utf-8").read()
    idx = {m.group(1): int(m.group(2)) for m in re.finditer(r"const val ([A-Z0-9_]+) = (\d+)\b", src)}
    body = src[src.index("val DEFAULTS"):]
    body = body[body.index("listOf(") + 7:]
    body = body[: body.index("\n        )")]
    body = re.sub(r"//[^\n]*", "", body)
    named = {m.group(1): m.group(2) for m in re.finditer(r"const val ([A-Z0-9_]+) = ([0-9.]+)f", src)}
    body = re.sub(r"\b([A-Z][A-Z0-9_]+)\b", lambda m: named.get(m.group(1), m.group(1)) + "f", body)
    defaults = [float(v) for v in re.findall(r"(-?\d+(?:\.\d+)?)f", body)]
    presets = []
    for m in re.finditer(r'"[^"]+" to SynthPatch\(DEFAULTS\)((?:\s*\.with\([A-Z0-9_]+, [0-9.]+f\))+)', src):
        p = list(defaults)
        for k, v in re.findall(r"\.with\(([A-Z0-9_]+), ([0-9.]+)f\)", m.group(1)):
            p[idx[k]] = float(v)
        presets.append(p)
    lib = open(os.path.join(MODEL, "SoundLibrary.kt"), encoding="utf-8").read()
    v2 = lib[lib.index("val SYNTH_V2"):]
    v2 = v2[: v2.index(").map {")]
    out = {}
    for name in CHOSEN:
        if name in PRESET_INDEX:
            out[name] = presets[PRESET_INDEX[name]]
            continue
        m = re.search(r'NamedSound\("' + re.escape(name) + r'", "[^"]+", synth\(([^)]*)\)\)', v2)
        p = list(defaults)
        for k, v in re.findall(r"([A-Z0-9_]+) to ([0-9.]+)f", m.group(1)):
            p[idx[k]] = float(v)
        out[name] = p
    if len(defaults) != 59:
        raise SystemExit(f"DEFAULTS: {len(defaults)} values, 59 expected")
    return out


def render(binary, midi, hold, total, params):
    line = f"{hold} {total} 1 {midi} " + " ".join(f"{v:.6f}" for v in params) + "\n"
    raw = subprocess.run([binary], input=line.encode(), capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).astype(float)


KEYS = ["note_hz", "pente_db_oct", "pair_impair_db", "inharm_b", "desaccord_cents", "vibrato_hz",
        "vibrato_cents", "largeur_sm_db", "souffle_hnr_db", "attaque_s", "chute_s", "relache_s",
        "brillance_attaque", "brillance_tenue", "chute_filtre_s", "evolution_pct", "evolution_periode_s"]


def instruments(binary):
    patches = load_patches()
    tally = {k: [0, 0] for k in KEYS}
    for name, (midi, hold, total, pad) in CHOSEN.items():
        st = render(binary, midi, hold, total, patches[name])
        fp, status = E.fingerprint(st, hold, pad=pad)
        print(f"\n{name} (midi {midi}) : {status}")
        if "note_hz" in fp and fp["note_hz"][1]:
            print(f"  note entendue : {E.note_name(fp['note_hz'][1])}")
        for k in KEYS:
            if k not in fp:
                continue
            v = fp[k]
            note = f"  [{v[3]}]" if len(v) > 3 else ""
            flag = "ok " if v[2] else "DÉCROCHE"
            if v[0] is None and v[1] is None:
                flag = "rien"
            else:
                tally[k][1] += 1
                tally[k][0] += bool(v[2])
            print(f"  {flag:<9}{k:<22}{E.fmt(v[0]):>10}{E.fmt(v[1]):>10}{note}")
    print("\nAccord mesure / contre-mesure, par mesure (sons où il y avait quelque chose à mesurer) :")
    for k in KEYS:
        ok, n = tally[k]
        print(f"  {k:<22}{ok}/{n}")


# ---------------------------------------------------------------- CMA demo

LO = np.array([0.6, 0.0, 0.003, 0.03, 0.0])
HI = np.array([2.5, 1.0, 0.080, 0.30, 15.0])
NAMES = ["pente p", "gain des pairs", "attaque (s)", "relâchement tau (s)", "désaccord (cents)"]
CMA_KEYS = ["pente_db_oct", "pair_impair_db", "attaque_s", "relache_s", "desaccord_cents"]
HOLD, TOTAL, F0 = 2.5, 4.1, 110.0  # hold > 2 beat periods of the detune


def toy(params, seed):
    """A fake instrument: additive, slope k^-p, even harmonics scaled, linear attack, two voices."""
    p, ev, att, tau, det = params
    rng = np.random.default_rng(seed)
    t = np.arange(int(TOTAL * SR)) / SR
    x = np.zeros_like(t)
    for cents in (-det, det):
        for k in range(1, 41):
            a = k ** -p * (ev if k % 2 == 0 else 1.0)
            x += a * np.sin(2 * math.pi * k * F0 * 2 ** (cents / 1200) * t + rng.uniform(0, 2 * math.pi))
    env = np.where(t < att, t / att, 1.0)
    env = np.where(t < HOLD, env, np.exp(-(t - HOLD) / tau))
    return 0.1 * x * env


SR = E.SR


def light_fp(x, path="a"):
    """Only the five measures the demo aims at; path "b" takes attack and release from the
    counter-measure (envelope path B) to check the optimum on a second chain."""
    steady = x[int(0.25 * SR): int((HOLD - 0.05) * SR)]
    pa = E.amps_freq(steady, F0)
    strong = [k for k in range(2, 9) if pa[k - 1] > 1e-5]
    att, _, rel = (E.env_path_a if path == "a" else E.env_path_b)(x, HOLD, F0)
    att = 0.0 if att is None else att  # below one period: counted as 0
    rel = TOTAL - HOLD if rel is None else rel  # release longer than the tail: count the tail
    return np.array([E.slope_from_amps(pa), E.even_odd_from_amps(pa), att, rel,
                     E.spread_freq(steady, F0, strong) if strong else 0.0])


def dist(fp, target):
    tol = np.array([max(E.TOL[k][1], E.TOL[k][0] * abs(t)) for k, t in zip(CMA_KEYS, target)])
    return float(np.sqrt(np.mean(((fp - target) / tol) ** 2)))


TRAIN_SEEDS = (11, 21, 31)       # phase draws averaged inside the cost
TARGET_SEEDS = (3, 4, 5)         # phase draws averaged for the target fingerprint
UNSEEN_SEEDS = (101, 102, 103, 104, 105)  # never seen by the search
_TARGET = None


def _params(u):
    return LO + np.clip(u, 0, 1) * (HI - LO)


def _loss(u):
    params = _params(u)
    d = np.mean([dist(light_fp(toy(params, sd)), _TARGET) for sd in TRAIN_SEEDS])
    return float(d) + 10 * float(np.sum(np.abs(u - np.clip(u, 0, 1))))


def _init(target):
    global _TARGET
    _TARGET = target


def _unseen(args):
    params, sd, path, target = args
    return dist(light_fp(toy(params, sd), path), target)


def cma_demo():
    sys.path.insert(0, os.path.join(ROOT, "scripts", "son", "timbre"))
    import timbre as T  # read-only use of the study's CMA

    hidden = np.array([1.3, 0.35, 0.025, 0.12, 7.0])
    with Pool(4) as pool:
        tgt = np.mean(pool.map(light_fp, [toy(hidden, sd) for sd in TARGET_SEEDS]), axis=0)
    rng = np.random.default_rng(1)
    x0 = rng.uniform(0.1, 0.9, 5)
    with Pool(4, initializer=_init, initargs=(tgt,)) as pool:
        start = pool.map(_loss, [x0])[0]
        best_u, best_f = T.cma(lambda xs: pool.map(_loss, list(xs)), x0, 0.25, 30, 10, rng)
    found = _params(best_u)
    print("Démo CMA : la recherche de l'étude (timbre.cma) vise l'empreinte d'un instrument factice")
    print(f"  coût moyenné sur {len(TRAIN_SEEDS)} rendus à phases différentes ; cible = moyenne de "
          f"{len(TARGET_SEEDS)} rendus du son caché")
    print(f"  distance au départ          {start:.2f}  (1 = une tolérance)")
    print(f"  distance trouvée (optimum)  {best_f:.2f}")
    ok = True
    with Pool(4) as pool:
        for path, label in (("a", "mesure"), ("b", "contre-mesure (attaque, relâchement chemin B)")):
            tgt_p = tgt if path == "a" else np.mean(
                pool.map(_unseen_fp, [(hidden, sd, "b") for sd in TARGET_SEEDS]), axis=0)
            d_found = pool.map(_unseen, [(found, sd, path, tgt_p) for sd in UNSEEN_SEEDS])
            d_hid = pool.map(_unseen, [(hidden, sd, path, tgt_p) for sd in UNSEEN_SEEDS])
            mf, mh = float(np.median(d_found)), float(np.median(d_hid))
            good = float(np.max(d_found)) <= 1.0 and mf <= mh + 0.5
            ok &= good
            print(f"  graines jamais vues ({len(UNSEEN_SEEDS)}), {label} :")
            print(f"    trouvé : médiane {mf:.2f}, pire {max(d_found):.2f} ; "
                  f"son caché lui-même (plancher) : médiane {mh:.2f}  -> {'tient' if good else 'NE TIENT PAS'}")
    for n, h, f in zip(NAMES, hidden, found):
        print(f"  {n:<22} caché {h:.3g}   trouvé {f:.3g}")
    print("  critère : sur chaque graine jamais vue, distance <= 1 tolérance, et médiane <= plancher + 0.5")
    print("CMA VERT" if ok else "CMA ROUGE")
    return 0 if ok else 1


def _unseen_fp(args):
    params, sd, path = args
    return light_fp(toy(params, sd), path)


if __name__ == "__main__":
    if sys.argv[1] == "instruments":
        instruments(sys.argv[2])
    else:
        sys.exit(cma_demo())
