"""(v3: final zone-A targets) Kicks per style: CMA-ES over kick2 inside each style box, towards the DNA
(the style's own stats when given, else the kick family), plus an anti-hollow
term (a kick whose 100-300 Hz share falls under the family median is pushed
back up: Benjamin found the thin sine kicks hollow)."""
import argparse, csv, json, multiprocessing as mp, os, sys, zlib
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import tgt  # noqa: E402
import kick2  # noqa: E402
import kstyles  # noqa: E402

FAM = "kick"
ARGS = None


def init(tsv, desc_dir):
    tgt.load(tsv, desc_dir)


def objective(u, style, seed):
    y, _ = kick2.render(kstyles.K[style], u, seed)
    return tgt.score(tgt.measure_st(y), FAM, tgt.CASE_K[style], "kick")[0]


def run(job):
    import cma
    style, k, evals = job
    n = len(kick2.KICK2)
    rng = np.random.default_rng(1000 * k + 7 + zlib.crc32(style.encode()) % 997)
    x0 = rng.uniform(0.15, 0.85, n)
    seed = int(rng.integers(1 << 30))
    es = cma.CMAEvolutionStrategy(x0, 0.3, {"bounds": [0, 1], "maxfevals": evals, "verbose": -9,
                                            "seed": int(rng.integers(1 << 30))})
    best = (99.0, x0)
    while not es.stop():
        xs = es.ask()
        fs = [objective(x, style, seed) for x in xs]
        es.tell(xs, fs)
        i = int(np.argmin(fs))
        if fs[i] < best[0]:
            best = (fs[i], np.array(xs[i]))
    return {"style": style, "run": k, "seed": seed, "score": best[0], "u": best[1].tolist()}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["opt", "export"])
    ap.add_argument("--targets", required=True)
    ap.add_argument("--desc", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--runs", type=int, default=10)
    ap.add_argument("--evals", type=int, default=300)
    ap.add_argument("--keep", type=int, default=8)
    ap.add_argument("--styles", default=",".join(kstyles.K))
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    rj = os.path.join(a.out, "kick-runs.json")
    if a.cmd == "opt":
        runs = json.load(open(rj)) if os.path.exists(rj) else []
        done = {(r["style"], r["run"]) for r in runs}
        jobs = [(s, k, a.evals) for s in a.styles.split(",") for k in range(a.runs) if (s, k) not in done]
        with mp.Pool(os.cpu_count(), init, (a.targets, a.desc)) as pool:
            for r in pool.imap_unordered(run, jobs):
                runs.append(r)
                print(r["style"], r["run"], round(r["score"], 2), flush=True)
                json.dump(runs, open(rj, "w"))
        return
    init(a.targets, a.desc)
    runs = json.load(open(rj))
    res = []
    for s in a.styles.split(","):
        rs = sorted((r for r in runs if r["style"] == s), key=lambda r: r["score"])[:a.keep]
        for i, r in enumerate(rs, 1):
            y, p = kick2.render(kstyles.K[s], np.array(r["u"]), r["seed"])
            name = "%s-kick-%02d.mp3" % (s, i)
            path = os.path.join(a.out, name)
            tgt.to_mp3_st(y, path)
            sc = tgt.report(path, FAM, tgt.CASE_K[s], "kick")
            res.append(dict(file=name, style=s, family="kick", tool="numpy+pedalboard (kick2), CMA-ES",
                            source="synthesised, no sample", license="CC0 (own work)", opt_score=round(r["score"], 2),
                            params={k: round(v, 4) for k, v in p.items()}, seed=r["seed"], **sc))
            print(name, sc["score"], sc["in_p25_p75"], sc["in_p10_p90"], sc["ranks"].get("flatness_db"), sc["worst"], flush=True)
    json.dump(res, open(os.path.join(a.out, "kicks.json"), "w"), indent=1, default=float)


if __name__ == "__main__":
    main()
