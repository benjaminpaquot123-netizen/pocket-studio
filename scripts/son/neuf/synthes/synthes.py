"""New synth sounds (leads, plucks, pads, arps, stabs, chords) tuned toward the
measured family ADN (docs/sons/fin/essentia-familles.tsv).

Chain: Surge XT factory patch (surgepy, built with scripts/son/neuf/synthes/
surgepy-settempo.patch so tempo-synced parameters follow the style tempo)
playing a phrase from music.py -> pedalboard (high-pass, shelves, bell,
low-pass, reverb) -> loudness to the family median -> limiter -> peak -1 dBFS.
pycma tunes 4 Surge parameters and 6 pedalboard parameters to bring the
Essentia measures (scripts/son/essentia/base.py, same settings as the ADN)
close to the family median. Score = RMS of z over the spectral, envelope and
loudness measures, z = (x - p50) / (IQR / 1.349): 0 is the median sound.

Run with the venv that holds surgepy, cma, pedalboard, essentia, librosa:
  python -I synthes.py screen OUTDIR [--styles a,b]  # rank patches per style/family
  python -I synthes.py tune OUTDIR [--styles a,b]    # CMA on the best candidates
  python -I synthes.py export OUTDIR                 # MP3 192k + final scores
Audio stays in OUTDIR (outside the repository).
"""
import argparse
import csv
import glob
import json
import math
import multiprocessing as mp
import os
import subprocess
import sys
import tempfile
import zlib

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(REPO, "scripts", "son", "essentia"))
SURGEPY = os.environ.get("SURGEPY_DIR", "/home/user/surge-synthesizer/surge/buildpy/src/surge-python")
PATCHES = os.environ.get("SURGE_PATCHES", "/home/user/surge-synthesizer/surge/resources/data/patches_factory")
sys.path.insert(0, SURGEPY)

import music  # noqa: E402

SR = 44100
ADN_TSV = os.path.join(REPO, "docs", "sons", "fin", "essentia-familles.tsv")
# Our family -> measured ADN family (type/subtype in the stock).
FAM_ADN = {
    "lead": "melodic_loop/lead", "pluck": "melodic_loop/pluck", "pad": "melodic_loop/pad",
    "arp": "kit_stem/arp", "accords": "kit_stem/chords", "stab": "melodic_loop/synth",
}
FAM_CATEGORIES = {
    "lead": ["Leads"], "pluck": ["Plucks"], "pad": ["Pads"],
    "arp": ["Plucks", "Polysynths", "Keys"], "accords": ["Polysynths", "Keys", "Pads"],
    "stab": ["Polysynths", "Plucks", "Keys"],
}
FEATURES = ["centroid_hz", "rolloff_hz", "flatness_db", "zcr", "low_share_db", "mid_share_db",
            "high_share_db", "mfcc1", "mfcc2", "mfcc3", "mfcc4", "mfcc5", "log_attack", "lufs"]
LOG_FEATURES = {"centroid_hz", "rolloff_hz"}  # z on log scale: spread is multiplicative
SIGMA_FLOOR = {"zcr": 0.005, "log_attack": 0.05, "lufs": 0.5}

FAMILY_TYPE = {k: v.split("/")[0] for k, v in FAM_ADN.items()}
N_CAND, N_KEEP = 5, 3
SCREEN_TOP = 24


def load_adn(path=ADN_TSV):
    rows = {}
    with open(path) as f:
        for r in csv.DictReader(f, delimiter="\t"):
            rows.setdefault(r["famille"], {})[r["mesure"]] = r
    adn = {}
    for fam, src in FAM_ADN.items():
        t = {}
        for m in FEATURES:
            r = rows[src][m]
            p25, p50, p75 = float(r["p25"]), float(r["p50"]), float(r["p75"])
            if m in LOG_FEATURES:
                p25, p50, p75 = math.log(p25), math.log(p50), math.log(p75)
            t[m] = (p50, max((p75 - p25) / 1.349, SIGMA_FLOOR.get(m, 1e-6)))
        t["_n"] = int(rows[src][FEATURES[0]]["n_sons"])
        adn[fam] = t
    return adn


def zscores(meas, target):
    z = {}
    for m in FEATURES:
        v = meas.get(m)
        if v is None or not np.isfinite(v):
            z[m] = 10.0
            continue
        if m in LOG_FEATURES:
            v = math.log(max(v, 1.0))
        p50, sd = target[m]
        z[m] = (v - p50) / sd
    return z


def score(meas, target):
    z = zscores(meas, target)
    return float(math.sqrt(sum(min(abs(v), 10.0) ** 2 for v in z.values()) / len(z)))


# ------------------------------------------------------------------ worker state
_W = {}


def _init():
    import surgepy  # noqa: F401
    import base  # noqa: F401
    _W["surge"] = None
    _W["patch"] = None
    _W["adn"] = load_adn()


def _surge():
    import surgepy
    if _W.get("surge") is None:
        _W["surge"] = surgepy.createSurge(SR)
    return _W["surge"]


SURGE_TWEAKS = [  # (control group, entry index, param name, delta range)
    ("cg_FILTER", 0, "Filter 1 Cutoff", 18.0),
    ("cg_FILTER", 0, "Filter 1 Resonance", 0.25),
    ("cg_ENV", 0, "Amp EG Attack", 2.0),
    ("cg_ENV", 0, "Amp EG Release", 2.0),
]


def _load_patch(path):
    import surgepy
    s = _surge()
    if _W.get("patch") != path:
        s.loadPatch(path)
        params = []
        for cg, entry, name, rng in SURGE_TWEAKS:
            g = s.getControlGroup(getattr(surgepy.constants, cg))
            for e in g.getEntries():
                if e.getScene() in (1, 2) and e.getEntry() == entry:
                    for p in e.getParams():
                        if str(p.getName()) == name:
                            params.append((p, s.getParamVal(p), rng, s.getParamMin(p), s.getParamMax(p)))
        _W["params"] = params
        _W["patch"] = path
    return s


def render(patch, style, family, variant, xs):
    """Stereo float32 [n, 2], one loop (second pass of two)."""
    st = music.STYLES[style]
    s = _load_patch(patch)
    for p, v0, rng, lo, hi in _W["params"]:
        i = [t[2] for t in SURGE_TWEAKS].index(str(p.getName()))
        s.setParamVal(p, float(np.clip(v0 + xs[i] * rng, lo, hi)))
    s.setTempo(float(st["bpm"]))
    s.allNotesOff()
    bs = s.getBlockSize()
    spb = 60.0 / st["bpm"] * SR
    loop_len = int(round(music.BARS * 4 * spb))
    nb = int(math.ceil(loop_len * 2 / bs)) + 1
    nfl = int(SR / bs)  # one second of silence clears the previous tails
    s.processMultiBlock(s.createMultiBlock(nfl), 0, nfl)
    events = []
    for b0, n, vel, ln in music.phrase(style, family, variant):
        for rep in (0, 1):
            t0 = int(round(b0 * spb)) + rep * loop_len
            events.append((t0, 1, n, vel))
            events.append((t0 + max(bs, int(round(ln * spb))), 0, n, 0))
    events.sort(key=lambda e: (e[0], e[1]))
    buf = s.createMultiBlock(nb)
    blk, ei = 0, 0
    while blk < nb:
        t = blk * bs
        while ei < len(events) and events[ei][0] <= t:
            _, on, n, vel = events[ei]
            if on:
                s.playNote(0, n, vel, 0)
            else:
                s.releaseNote(0, n, 0)
            ei += 1
        nxt = nb if ei >= len(events) else min(nb, max(blk + 1, events[ei][0] // bs))
        s.processMultiBlock(buf, blk, nxt - blk)
        blk = nxt
    s.allNotesOff()
    out = buf[:, loop_len:2 * loop_len].T.astype(np.float32)
    return np.ascontiguousarray(out)


def fx(x, xp):
    """pedalboard chain; xp in [-1, 1]^6."""
    import pedalboard as pb
    u = (np.clip(xp, -1, 1) + 1) / 2
    hp = 20 * (300 / 20) ** u[0]
    lp = 2500 * (20000 / 2500) ** u[4]
    board = pb.Pedalboard([
        pb.HighpassFilter(cutoff_frequency_hz=float(hp)),
        pb.LowShelfFilter(cutoff_frequency_hz=250, gain_db=float(-12 + 18 * u[1])),
        pb.PeakFilter(cutoff_frequency_hz=1500, gain_db=float(-8 + 14 * u[2]), q=1.0),
        pb.HighShelfFilter(cutoff_frequency_hz=5000, gain_db=float(-15 + 21 * u[3])),
        pb.LowpassFilter(cutoff_frequency_hz=float(lp)),
        pb.Reverb(room_size=0.55, damping=0.5, wet_level=float(0.3 * u[5]), dry_level=1.0, width=1.0),
    ])
    # loop-aware: process two copies, keep the second (tails wrap around)
    y = board(np.concatenate([x, x]).T, SR).T
    return np.ascontiguousarray(y[len(x):]).astype(np.float32)


def lufs(x):
    import essentia.standard as es
    _, _, integ, _ = es.LoudnessEBUR128(sampleRate=SR)(x)
    return float(integ)


def master(x, target_lufs):
    import pedalboard as pb
    if not np.isfinite(x).all() or np.abs(x).max() < 1e-5:
        return None
    l = lufs(x)
    if not np.isfinite(l) or l < -70:
        return None
    x = x * 10 ** ((target_lufs - l) / 20)
    lim = pb.Pedalboard([pb.Limiter(threshold_db=-1.5, release_ms=80)])
    y = lim(np.concatenate([x, x]).T, SR).T[len(x):]
    # peak ceiling -1 dBFS: never raise the level above the family loudness
    y = y * min(1.0, 10 ** (-1.05 / 20) / max(np.abs(y).max(), 1e-9))
    return np.ascontiguousarray(y).astype(np.float32)


def evaluate(patch, style, family, variant, x):
    import base
    target = _W["adn"][family]
    ntw = len(SURGE_TWEAKS)
    raw = render(patch, style, family, variant, x[:ntw])
    y = fx(raw, x[ntw:])
    y = master(y, target["lufs"][0] - 0.4)
    if y is None:
        return 99.0, None, None
    m = base.ess_measure(y, FAMILY_TYPE[family], family)
    return score(m, target), m, y


NDIM = len(SURGE_TWEAKS) + 6


# ------------------------------------------------------------------ jobs
def job_screen(a):
    patch, style, family, variant = a
    try:
        sc, m, _ = evaluate(patch, style, family, variant, np.zeros(NDIM))
    except Exception as e:  # a broken patch is skipped, not fatal
        return dict(patch=patch, style=style, family=family, score=99.0, err=str(e)[:200])
    return dict(patch=patch, style=style, family=family, score=sc)


def job_tune(a):
    import cma
    patch, style, family, variant, budget = a
    es_ = cma.CMAEvolutionStrategy(np.zeros(NDIM), 0.35, {
        "bounds": [-1, 1], "popsize": 8, "maxfevals": budget, "verbose": -9,
        "seed": zlib.crc32(f"{patch}|{style}|{family}".encode()) % 100000 + 1})
    best = (99.0, np.zeros(NDIM))
    sc0, _, _ = evaluate(patch, style, family, variant, np.zeros(NDIM))
    best = (sc0, np.zeros(NDIM))
    while not es_.stop():
        xs = es_.ask()
        fs = []
        for x in xs:
            try:
                sc, _, _ = evaluate(patch, style, family, variant, np.asarray(x))
            except Exception:
                sc = 99.0
            fs.append(sc)
            if sc < best[0]:
                best = (sc, np.asarray(x))
        es_.tell(xs, fs)
    return dict(patch=patch, style=style, family=family, variant=variant,
                score0=sc0, score=best[0], x=[float(v) for v in best[1]], evals=es_.result.evaluations)


def candidates(family):
    out = []
    for c in FAM_CATEGORIES[family]:
        out += sorted(glob.glob(os.path.join(PATCHES, c, "*.fxp")))
    return out


def _pool():
    return mp.get_context("fork").Pool(int(os.environ.get("PS_JOBS", "4")), initializer=_init)


def cmd_screen(out, styles, families):
    path = os.path.join(out, "screen.jsonl")
    # every patch on the reference style first, then only its SCREEN_TOP best on the others
    ref = "house"
    jobs = [(p, ref, fam, 0) for fam in families for p in candidates(fam)]
    if os.path.exists(path):
        rows = [json.loads(l) for l in open(path)]
        for fam in families:
            r = sorted([x for x in rows if x["family"] == fam and x["style"] == ref], key=lambda x: x["score"])
            top = [x["patch"] for x in r[:SCREEN_TOP]]
            jobs += [(p, st, fam, 0) for st in styles if st != ref for p in top]
    done = set()
    if os.path.exists(path):
        for l in open(path):
            r = json.loads(l)
            done.add((r["patch"], r["style"], r["family"]))
    jobs = [j for j in jobs if (j[0], j[1], j[2]) not in done]
    with _pool() as pool, open(path, "a") as f:
        for i, r in enumerate(pool.imap_unordered(job_screen, jobs, chunksize=4)):
            f.write(json.dumps(r) + "\n")
            if i % 200 == 0:
                f.flush()
                print("screen", i, len(jobs), flush=True)


def pick(out, styles, families):
    """N_CAND patches per (style, family): best screen score, a patch used at
    most twice per family across styles so each style sounds different."""
    rows = [json.loads(l) for l in open(os.path.join(out, "screen.jsonl"))]
    picks = {}
    for fam in families:
        used = {}
        for st in styles:
            r = sorted([x for x in rows if x["family"] == fam and x["style"] == st and x["score"] < 50],
                       key=lambda x: x["score"])
            chosen = []
            for x in r:
                if used.get(x["patch"], 0) >= 2:
                    continue
                chosen.append(x["patch"])
                used[x["patch"]] = used.get(x["patch"], 0) + 1
                if len(chosen) == N_CAND:
                    break
            picks[(st, fam)] = chosen
    return picks


def cmd_tune(out, styles, families, budget):
    picks = pick(out, styles, families)
    path = os.path.join(out, "tune.jsonl")
    done = set()
    if os.path.exists(path):
        for l in open(path):
            r = json.loads(l)
            done.add((r["patch"], r["style"], r["family"]))
    jobs = []
    for (st, fam), ps in picks.items():
        for k, p in enumerate(ps):
            if (p, st, fam) not in done:
                jobs.append((p, st, fam, k, budget))
    with _pool() as pool, open(path, "a") as f:
        for i, r in enumerate(pool.imap_unordered(job_tune, jobs)):
            f.write(json.dumps(r) + "\n")
            f.flush()
            print("tune", i + 1, len(jobs), r["style"], r["family"], round(r["score0"], 2), "->",
                  round(r["score"], 2), flush=True)


def encode_mp3(y, path):
    import soundfile as sf
    with tempfile.NamedTemporaryFile(suffix=".wav") as t:
        sf.write(t.name, y, SR, subtype="FLOAT")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", t.name, "-codec:a", "libmp3lame",
                        "-b:a", "192k", "-map_metadata", "-1", path], check=True)
    dec = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "f32le", "-ac", "2", "-ar", str(SR), "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(dec, dtype=np.float32).reshape(-1, 2)


_LIB_SNIPPET = r"""
import json, sys
sys.path.insert(0, sys.argv[1])
import base
m = base.lib_measure(open(sys.argv[2], "rb").read(), sys.argv[3], sys.argv[4])
print(json.dumps({k: m.get(k) for k in ("centroid_hz", "rolloff_hz", "lufs")}, default=float))
"""


def lib_check(path, family):
    """librosa cross-check in a child process: its numba beat tracker can
    segfault, which would otherwise hang the whole worker pool."""
    import subprocess
    try:
        p = subprocess.run([sys.executable, "-I", "-c", _LIB_SNIPPET, os.path.join(REPO, "scripts", "son", "essentia"), path,
                            FAMILY_TYPE[family], family],
                           capture_output=True, text=True, timeout=120)
        if p.returncode == 0:
            return json.loads(p.stdout.strip().splitlines()[-1])
        return {"err": "librosa exit %d" % p.returncode}
    except Exception as e:  # timeout or bad output
        return {"err": str(e)[:120]}


def job_export(a):
    import base
    r, name, out = a
    target = _W["adn"][r["family"]]
    x = np.asarray(r["x"])
    ntw = len(SURGE_TWEAKS)
    raw = render(r["patch"], r["style"], r["family"], r["variant"], x[:ntw])
    y = master(fx(raw, x[ntw:]), target["lufs"][0] - 0.4)
    path = os.path.join(out, "mp3", name)
    gain = 1.0
    for _ in range(4):
        dec = encode_mp3(y * gain, path)
        pk = 20 * math.log10(max(np.abs(dec).max(), 1e-9))
        if pk <= -0.95:
            break
        gain *= 10 ** ((-1.0 - pk - 0.05) / 20)
    m = base.ess_measure(dec.copy(), FAMILY_TYPE[r["family"]], r["family"])
    z = zscores(m, target)
    lib = lib_check(path, r["family"])
    return dict(r, fichier=name, score_mp3=score(m, target), peak_db=pk,
                z={k: round(v, 2) for k, v in z.items()}, ess=m,
                lib={k: lib.get(k) for k in ("centroid_hz", "rolloff_hz", "lufs", "err") if k in lib})


def cmd_export(out):
    rows = {}
    for l in open(os.path.join(out, "tune.jsonl")):
        r = json.loads(l)
        rows[(r["patch"], r["style"], r["family"])] = r  # last run wins
    groups = {}
    for r in rows.values():
        groups.setdefault((r["style"], r["family"]), []).append(r)
    jobs = []
    for (st, fam), rs in groups.items():
        rs.sort(key=lambda r: r["score"])
        for k, r in enumerate(rs[:N_KEEP]):
            jobs.append((r, f"{st}-{fam}-{k + 1:02d}.mp3", out))
    os.makedirs(os.path.join(out, "mp3"), exist_ok=True)
    ep = os.path.join(out, "export.jsonl")
    done = set()
    if os.path.exists(ep):  # resume: keep finished files
        done = {json.loads(l)["fichier"] for l in open(ep) if l.strip()}
    jobs = [j for j in jobs if j[1] not in done]
    with _pool() as pool, open(ep, "a") as f:
        for r in pool.imap_unordered(job_export, jobs):
            f.write(json.dumps(r, default=float) + "\n")
            print("export", r["fichier"], round(r["score_mp3"], 2), flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["screen", "tune", "export"])
    ap.add_argument("out")
    ap.add_argument("--styles", default=",".join(music.STYLE_ORDER))
    ap.add_argument("--families", default=",".join(music.FAMILIES))
    ap.add_argument("--budget", type=int, default=72)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    styles, fams = a.styles.split(","), a.families.split(",")
    if a.cmd == "screen":
        cmd_screen(a.out, styles, fams)
    elif a.cmd == "tune":
        cmd_tune(a.out, styles, fams, a.budget)
    else:
        cmd_export(a.out)


if __name__ == "__main__":
    main()
