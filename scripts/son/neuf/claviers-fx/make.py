"""Make new keys, effects and voice sounds tuned toward the family DNA.

For each (style, family) case, a few candidates are rendered (fixed musical
content per candidate), then CMA-ES tunes a pedalboard chain (filters,
shelves, drive, compressor, reverb, hand balance) so that the Essentia
measures of the result come as close as possible to the family median
(dna.distance). The best candidates are exported as MP3 192 kb/s, peak
-1 dBFS, and re-measured from the MP3 with both Essentia and librosa.

Usage (always python -I):
  PS_ESSENTIA_BASE=.../base.py python -I make.py --tsv essentia-familles.tsv \
      --salamander DIR --vcsl DIR --vocalset DIR --out DIR [--only style,...]
"""
import argparse
import io
import json
import multiprocessing as mp
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402

import dna  # noqa: E402
import instr  # noqa: E402
import styles  # noqa: E402

SR = instr.SR
CASES = {
    "pop": ["piano", "piano-electrique", "effet", "voix"],
    "lofi": ["piano", "piano-electrique", "effet", "voix"],
    "house": ["piano", "orgue", "effet", "voix"],
    "trap": ["piano", "effet", "voix"],
    "edm": ["piano", "effet", "voix"],
    "jazz-soul": ["piano", "piano-electrique", "orgue"],
    "rnb": ["piano", "piano-electrique", "voix"],
}
FAMILY = {"piano": "melodic_loop/keys", "piano-electrique": "melodic_loop/keys",
          "orgue": "melodic_loop/keys", "effet": "fx/fx", "voix": "vocal/vocal"}
KEEP = {"effet": 4}
N_CAND = {"effet": 6}
FX_KINDS = ["riser", "impact", "downlifter", "sweep", "riser", "sweep"]

A = None  # parsed args, set in main (inherited by forked workers)
_INST = {}


def inst(name):
    if name not in _INST:
        if name == "salamander":
            _INST[name] = instr.salamander(A.salamander)
        elif name.startswith("tx81z:"):
            _INST[name] = instr.tx81z(A.vcsl, name.split(":", 1)[1])
    return _INST[name]


# ------------------------------------------------------------------ dry content
def build(style, fam, seed):
    """Returns (stems, meta): stems is a list of (n, 2) float32 at 48 kHz."""
    rng = np.random.default_rng(1000 + seed)
    bpm = styles.TEMPO[style]
    sw = styles.SWING.get(style, 0.0)
    meta = {"bpm": bpm}
    if fam in ("piano", "piano-electrique"):
        lh, rh = styles.keys_phrase(style, "piano" if fam == "piano" else "ep", seed)
        if fam == "piano":
            voice, src = inst("salamander").note, "Salamander Grand Piano V3"
        elif seed % 2 == 0:
            voice = instr.fm_ep(bright=float(rng.uniform(0.5, 1.6)), tine=float(rng.uniform(0.2, 0.8)),
                                bark=float(rng.uniform(0.0, 0.6)), decay=float(rng.uniform(1.2, 3.0)))
            src = "FM synthesis (numpy)"
        else:
            patch = ["FM Piano", "Piano 1"][(seed // 2) % 2]
            voice, src = inst("tx81z:" + patch).note, "VCSL TX81Z " + patch
        stems = [instr.render(p, bpm, voice, humanize=0.008, seed=seed + k, swing=sw)
                 for k, p in enumerate((lh, rh))]
        n = max(len(s) for s in stems)
        stems = [np.pad(s, ((0, n - len(s)), (0, 0))) for s in stems]
        if style == "edm":
            stems = [styles.pump(s, bpm) for s in stems]
        meta["source"] = src
    elif fam == "orgue":
        reg = {"house": ["house", "pop"], "jazz-soul": ["jazz", "gospel", "soul"]}.get(style, ["jazz"])
        r = reg[seed % len(reg)]
        voice = instr.organ(r, perc=float(rng.uniform(0, 0.8)), click=float(rng.uniform(0.1, 0.5)), seed=seed)
        y = instr.render(styles.organ_phrase(style, seed), bpm, voice, tail_s=0.5,
                         humanize=0.005, seed=seed, swing=sw)
        y = instr.leslie(y, rate=float(rng.choice([0.8, 6.5])), depth=float(rng.uniform(0.15, 0.4)))
        stems = [y]
        meta["source"] = "additive drawbar organ (numpy), registration " + r
    elif fam == "effet":
        kind = FX_KINDS[seed % len(FX_KINDS)]
        spb = 60.0 / bpm
        s = int(rng.integers(0, 1 << 30))
        if kind == "riser":
            y = instr.fx_riser(spb * 4 * float(rng.choice([1, 2])), seed=s, q=float(rng.uniform(0.6, 2.5)),
                               f_lo=float(rng.uniform(150, 600)), f_hi=float(rng.uniform(4000, 12000)),
                               tone=float(rng.uniform(0.1, 0.6)), start_note=int(rng.integers(40, 60)))
        elif kind == "downlifter":
            y = instr.fx_downlifter(spb * 4 * float(rng.choice([1, 2])), seed=s, q=float(rng.uniform(0.6, 2.0)),
                                    f_hi=float(rng.uniform(3000, 10000)), f_lo=float(rng.uniform(100, 400)),
                                    tone=float(rng.uniform(0.1, 0.5)), start_note=int(rng.integers(60, 80)))
        elif kind == "impact":
            y = instr.fx_impact(float(rng.uniform(1.5, 3.5)), seed=s, f_start=float(rng.uniform(90, 200)),
                                f_end=float(rng.uniform(30, 50)), boom_decay=float(rng.uniform(0.4, 1.4)),
                                crack=float(rng.uniform(0.2, 0.9)), crack_decay=float(rng.uniform(0.03, 0.2)))
        else:
            y = instr.fx_sweep(spb * 4 * float(rng.choice([1, 2])), seed=s, q=float(rng.uniform(1.0, 3.0)),
                               f_lo=float(rng.uniform(200, 800)), f_hi=float(rng.uniform(5000, 14000)),
                               up_frac=float(rng.uniform(0.4, 0.8)))
        stems = [y]
        meta.update(kind=kind, source="noise and oscillator synthesis (numpy)")
    elif fam == "voix":
        files = instr.vocalset_files(A.vocalset)
        sub = [f for i, f in enumerate(files) if i % 7 == seed % 7] or files
        rng2 = np.random.default_rng(seed)
        pick = list(rng2.choice(sub, size=min(3, len(sub)), replace=False))
        y = instr.chops(pick, styles.chop_pattern(style, seed), bpm, seed=seed)
        if style in ("edm", "house"):
            y = styles.pump(y, bpm, 0.5)
        stems = [y]
        meta.update(source="VocalSet (sustained vowels)",
                    vocalset_files=[os.path.relpath(p, A.vocalset) for p in pick])
    return stems, meta


# ------------------------------------------------------------------ chain
def _lg(u, lo, hi):
    return float(lo * (hi / lo) ** u)


def _li(u, lo, hi):
    return float(lo + (hi - lo) * u)


NP = 12


def chain(x, stems):
    import pedalboard as pb
    x = np.clip(np.asarray(x, dtype=float), 0, 1)
    if len(stems) == 2:
        y = stems[0] * 10 ** (_li(x[11], -12, 6) / 20) + stems[1]
    else:
        y = stems[0]
    board = pb.Pedalboard([
        pb.HighpassFilter(_lg(x[0], 20, 600)),
        pb.LowpassFilter(_lg(x[1], 250, 18000)),
        pb.LowpassFilter(_lg(x[2], 250, 18000)),
        pb.LowShelfFilter(cutoff_frequency_hz=200, gain_db=_li(x[3], -12, 12)),
        pb.HighShelfFilter(cutoff_frequency_hz=3000, gain_db=_li(x[4], -24, 8)),
        pb.PeakFilter(cutoff_frequency_hz=_lg(x[10], 200, 5000), gain_db=_li(x[9], -10, 10), q=1.0),
    ])
    y = board(np.ascontiguousarray(y.T, dtype=np.float32), SR)
    m = x[6]
    if m > 0.02:
        d = pb.Distortion(drive_db=_li(x[5], 0, 24))(y, SR)
        y = (1 - m) * y + m * d * (np.abs(y).max() / (np.abs(d).max() + 1e-9))
    comp = pb.Pedalboard([
        pb.Compressor(threshold_db=_li(x[7], -40, 0), ratio=4, attack_ms=5, release_ms=120),
        pb.Reverb(room_size=_li(x[8], 0.1, 0.95), wet_level=_li(x[8], 0.0, 0.45),
                  dry_level=1.0 - _li(x[8], 0.0, 0.3), width=1.0),
    ])
    y = comp(np.ascontiguousarray(y, dtype=np.float32), SR).T
    return trim(y)


def trim(y, db=-60):
    a = np.abs(y).max(1)
    pk = a.max()
    if pk <= 0:
        return y
    k = np.flatnonzero(a > pk * 10 ** (db / 20))
    end = min(len(y), int(k[-1]) + int(0.05 * SR))
    y = y[:end].copy()
    f = min(len(y), int(0.03 * SR))
    y[-f:] *= np.linspace(1, 0, f)[:, None]
    return y


def to44(y):
    return dna.base().resample(np.asarray(y, dtype=np.float32), SR)


def score(y, fam, T):
    m = dna.measure(to44(y), fam)
    d, z = dna.distance(m, T[fam])
    return d, z, m


# ------------------------------------------------------------------ one candidate
def run_candidate(job):
    style, famname, seed, T = job
    import cma
    fam = FAMILY[famname]
    t0 = time.time()
    try:
        stems, meta = build(style, famname, seed)
    except Exception as e:  # a candidate that cannot be built is skipped
        return {"style": style, "famille": famname, "seed": seed, "error": repr(e)}
    best = (1e9, None)
    es = cma.CMAEvolutionStrategy(NP * [0.5], 0.3, {"bounds": [0, 1], "popsize": A.pop,
                                                    "maxiter": A.iters, "seed": seed + 1, "verbose": -9})
    while not es.stop():
        xs = es.ask()
        fs = []
        for x in xs:
            try:
                d = score(chain(x, stems), fam, T)[0]
            except Exception:
                d = 10.0
            fs.append(d)
            if d < best[0]:
                best = (d, list(map(float, x)))
        es.tell(xs, fs)
    y = chain(best[1], stems)
    return {"style": style, "famille": famname, "family": fam, "seed": seed, "meta": meta,
            "params": best[1], "opt_distance": best[0], "secs": round(time.time() - t0, 1),
            "audio": y.astype(np.float32)}


# ------------------------------------------------------------------ export
def encode(y, path):
    y = y / (np.abs(y).max() + 1e-12) * 10 ** (-1.3 / 20)
    import soundfile as sf
    for _ in range(3):
        buf = io.BytesIO()
        sf.write(buf, y, SR, format="WAV", subtype="FLOAT")
        mp3 = subprocess.run(["ffmpeg", "-v", "error", "-f", "wav", "-i", "pipe:0", "-c:a", "libmp3lame",
                              "-b:a", "192k", "-f", "mp3", "pipe:1"], input=buf.getvalue(),
                             capture_output=True, check=True).stdout
        dec = subprocess.run(["ffmpeg", "-v", "error", "-i", "pipe:0", "-f", "f32le", "-ac", "2", "pipe:1"],
                             input=mp3, capture_output=True, check=True).stdout
        pk = float(np.abs(np.frombuffer(dec, np.float32)).max())
        if pk <= 10 ** (-1.0 / 20):
            break
        y = y * 10 ** (-1.0 / 20) / pk * 0.98
    with open(path, "wb") as f:
        f.write(mp3)
    return mp3, 20 * np.log10(pk)


def final_measure(mp3, fam, T):
    """Measure the MP3 itself with both tools (decoded to WAV for soundfile)."""
    wav = subprocess.run(["ffmpeg", "-v", "error", "-i", "pipe:0", "-f", "wav", "pipe:1"],
                         input=mp3, capture_output=True, check=True).stdout
    typ = dna.MTYPE[fam.split("/")[0]]
    rec = dna.base().measure_bytes(wav, typ, fam)
    d, z = dna.distance(rec["ess"], T[fam])
    dl, zl = dna.distance(rec["lib"], T[fam], dna.LIB_MEASURES)
    return {"essentia_distance_sd": round(d, 3), "essentia_z": {k: round(v, 2) for k, v in z.items()},
            "librosa_distance_sd": round(dl, 3), "librosa_z": {k: round(v, 2) for k, v in zl.items()},
            "dur_s": round(rec["ess"].get("dur_s", 0), 2)}


def main():
    global A
    ap = argparse.ArgumentParser()
    ap.add_argument("--tsv", required=True)
    ap.add_argument("--salamander")
    ap.add_argument("--vcsl")
    ap.add_argument("--vocalset")
    ap.add_argument("--out", required=True)
    ap.add_argument("--only", default="")
    ap.add_argument("--families", default="")
    ap.add_argument("--cand", type=int, default=5)
    ap.add_argument("--keep", type=int, default=3)
    ap.add_argument("--pop", type=int, default=8)
    ap.add_argument("--iters", type=int, default=12)
    ap.add_argument("--procs", type=int, default=4)
    A = ap.parse_args()
    T = dna.load_targets(A.tsv)
    os.makedirs(A.out, exist_ok=True)
    only = set(filter(None, A.only.split(",")))
    fams = set(filter(None, A.families.split(",")))
    jobs = []
    for style, fl in CASES.items():
        if only and style not in only:
            continue
        for f in fl:
            if fams and f not in fams:
                continue
            for seed in range(N_CAND.get(f, A.cand)):
                jobs.append((style, f, seed, T))
    print("jobs", len(jobs), flush=True)
    results = {}
    with mp.get_context("fork").Pool(A.procs, maxtasksperchild=6) as pool:
        for r in pool.imap_unordered(run_candidate, jobs):
            key = (r["style"], r["famille"])
            results.setdefault(key, []).append(r)
            print(r["style"], r["famille"], r["seed"], r.get("opt_distance", r.get("error")),
                  r.get("secs"), flush=True)
    log = open(os.path.join(A.out, "results.jsonl"), "a")
    for (style, f), rs in sorted(results.items()):
        rs = sorted([r for r in rs if "audio" in r], key=lambda r: r["opt_distance"])
        keep = KEEP.get(f, A.keep)
        if f == "effet":  # at most two of a kind
            sel, cnt = [], {}
            for r in rs:
                k = r["meta"]["kind"]
                if cnt.get(k, 0) < 2:
                    sel.append(r)
                    cnt[k] = cnt.get(k, 0) + 1
            rs = sel
        for i, r in enumerate(rs[:keep]):
            name = "%s-%s-%02d.mp3" % (style, f, i + 1)
            mp3, pk = encode(r.pop("audio"), os.path.join(A.out, name))
            fm = final_measure(mp3, r["family"], T)
            rec = {k: v for k, v in r.items() if k != "audio"}
            rec.update(file=name, peak_dbfs=round(float(pk), 2), **fm)
            log.write(json.dumps(rec) + "\n")
            log.flush()
            print("kept", name, fm["essentia_distance_sd"], fm["librosa_distance_sd"], flush=True)


if __name__ == "__main__":
    main()
