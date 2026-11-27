"""New electronic drum one-shots pulled towards the family DNA by CMA-ES.

  python make.py opt    --base BASE_PY --dna FAMILLES_TSV --out DIR [--runs 6 --evals 300]
  python make.py export --base BASE_PY --dna FAMILLES_TSV --out DIR

opt: for each style x family, several CMA-ES runs (different starts) on the
synth parameters inside the style box; objective = RMS of the clipped
robust z-scores of the Essentia measures against the family median.
export: keeps the 3 best runs (a 4th when it scores <= 1.0), renders them,
writes MP3 192 kb/s with a -1 dBFS peak, then re-measures the decoded MP3
(peak brought back to the packs' -0.1 dBFS: level is a gain, not a timbre)
and writes results.json with the final Essentia score and librosa check.
"""
import argparse
import json
import multiprocessing as mp
import os
import subprocess
import zlib
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import dna as dnamod  # noqa: E402
import styles  # noqa: E402
import synth  # noqa: E402

PACK_PEAK = 10 ** (-0.1 / 20)
OUT_PEAK = 10 ** (-1.0 / 20)
J = None


def init(base, tsv):
    global J
    J = dnamod.Judge(base, tsv)


def objective(u, fam, box, seed):
    y, _ = synth.render(fam, box, u, seed)
    m = J.measure(y * PACK_PEAK, "drum_oneshot", "drum_oneshot/" + fam)
    return J.score(J.zs(m, "drum_oneshot/" + fam), clip=5.0)


def run(job):
    import cma
    style, fam, k, evals = job
    box = styles.STYLES[style]["box"][fam]
    n = len(synth.TABLES[fam])
    rng = np.random.default_rng(1000 * k + zlib.crc32((style + fam).encode()) % 997)
    x0 = rng.uniform(0.2, 0.8, n)
    seed = int(rng.integers(1 << 30))
    es = cma.CMAEvolutionStrategy(x0, 0.3, {"bounds": [0, 1], "maxfevals": evals, "verbose": -9,
                                            "seed": int(rng.integers(1 << 30))})
    best = (99.0, x0)
    while not es.stop():
        xs = es.ask()
        fs = [objective(x, fam, box, seed) for x in xs]
        es.tell(xs, fs)
        i = int(np.argmin(fs))
        if fs[i] < best[0]:
            best = (fs[i], np.array(xs[i]))
    return {"style": style, "fam": fam, "run": k, "seed": seed, "score": best[0], "u": best[1].tolist()}


def to_mp3(y, path):
    y = np.asarray(y, dtype=np.float32) / max(np.abs(y).max(), 1e-9) * OUT_PEAK
    st = np.repeat(y[:, None], 2, 1)
    p = subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "f32le", "-ar", "44100", "-ac", "2",
                        "-i", "-", "-codec:a", "libmp3lame", "-b:a", "192k", path],
                       input=st.tobytes(), capture_output=True)
    if p.returncode:
        raise RuntimeError(p.stderr.decode())


def from_mp3(path):
    p = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", path, "-f", "f32le", "-ac", "1", "-ar", "44100", "-"],
                       capture_output=True, check=True)
    return np.frombuffer(p.stdout, dtype=np.float32)


def final_score(path, typ, fam):
    y = from_mp3(path)
    y = y / max(np.abs(y).max(), 1e-9) * PACK_PEAK
    m = J.measure(y, typ, fam)
    z = J.zs(m, fam)
    ok, n = J.librosa_check(y, typ, fam, m)
    inr, ni = J.in_range(m, fam)
    return {"essentia_sd": round(J.score(z), 2), "in_p10_p90": "%d/%d" % (inr, ni),
            "librosa_agree": "%d/%d" % (ok, n),
            "worst": sorted(((k, round(v, 1)) for k, v in z.items()), key=lambda t: -abs(t[1]))[:3]}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["opt", "export"])
    ap.add_argument("--base", required=True)
    ap.add_argument("--dna", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--runs", type=int, default=6)
    ap.add_argument("--evals", type=int, default=300)
    ap.add_argument("--styles", default=",".join(styles.STYLES))
    ap.add_argument("--fams", default=",".join(synth.TABLES))
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    rj = os.path.join(a.out, "runs.json")
    if a.cmd == "opt":
        runs = json.load(open(rj)) if os.path.exists(rj) else []
        done = {(r["style"], r["fam"], r["run"]) for r in runs}
        jobs = [(s, f, k, a.evals) for s in a.styles.split(",") for f in a.fams.split(",")
                for k in range(a.runs) if (s, f, k) not in done]
        with mp.Pool(os.cpu_count(), init, (a.base, a.dna)) as pool:
            for r in pool.imap_unordered(run, jobs):
                runs.append(r)
                print(r["style"], r["fam"], r["run"], round(r["score"], 2), flush=True)
                json.dump(runs, open(rj, "w"))
        return
    init(a.base, a.dna)
    runs = json.load(open(rj))
    res = []
    for s in styles.STYLES:
        for f in synth.TABLES:
            rs = sorted((r for r in runs if r["style"] == s and r["fam"] == f), key=lambda r: r["score"])
            keep = rs[:3] + [r for r in rs[3:4] if r["score"] <= 1.0]
            for i, r in enumerate(keep, 1):
                y, p = synth.render(f, styles.STYLES[s]["box"][f], np.array(r["u"]), r["seed"])
                name = "%s-%s-%02d.mp3" % (s, styles.FAM_FR[f], i)
                path = os.path.join(a.out, name)
                to_mp3(y, path)
                sc = final_score(path, "drum_oneshot", "drum_oneshot/" + f)
                res.append(dict(file=name, style=s, family=f, opt_score=round(r["score"], 2),
                                params={k: round(v, 4) for k, v in p.items()}, seed=r["seed"], **sc))
                print(name, sc["essentia_sd"], sc["in_p10_p90"], sc["librosa_agree"], flush=True)
    json.dump(res, open(os.path.join(a.out, "results.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
