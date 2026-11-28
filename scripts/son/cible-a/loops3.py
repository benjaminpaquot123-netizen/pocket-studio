"""Drum loops v3 (final zone-A targets, boucle-drums; stereo hats and room, bus shelf): several 4-bar grooves per style, built from the new layered
kicks plus the snares, claps and closed hats Benjamin kept ('garder') on the
first page (re-rendered from their parameters; no sample anywhere). The open hat
is the kept closed hat with a long ring; it is choked by the next closed hat.

Each variant = a groove template + generated variations (ghost notes, hat rolls,
a fill on bar 4) + a kit draw; CMA-ES then sets the mix (lane gains, room,
bus drive) towards the drum_loop DNA of the style.
"""
import argparse, json, multiprocessing as mp, os, sys, zlib
import numpy as np
import pedalboard as pb
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import tgt  # noqa: E402
import kick2  # noqa: E402
import kstyles  # noqa: E402
import synth  # noqa: E402

SR = 44100
FAM = "boucle-drums"
BPM = {"trap": 140, "drill": 142, "boom-bap": 90, "house": 124, "techno": 130, "drum-and-bass": 174,
       "edm": 128, "reggaeton-afro": 98, "lo-fi": 82}
SWING = {"trap": 0.0, "drill": 0.0, "boom-bap": 0.2, "house": 0.1, "techno": 0.0, "drum-and-bass": 0.03,
         "edm": 0.0, "reggaeton-afro": 0.06, "lo-fi": 0.28}

# Templates: one bar of 16 steps per lane (x = accent, o = normal, g = ghost, . = rest).
# K kick, S snare, C clap, H closed hat, O open hat, R hat roll (32nds) marker in H.
T = {
    "trap": [
        dict(K="x......x..x.....", S="........x.......", C="........x.......", H="o.o.o.o.o.o.o.o.", O="..............o."),
        dict(K="x.........x..x..", S="........x.......", C="........x.......", H="ooooooooooooRRoo", O=""),
        dict(K="x......x.x......", S="........x.......", C="", H="o.o.o.oRo.o.o.oR", O=""),
        dict(K="x.x.......x.....", S="........x.....g.", C="........x.......", H="ooo.ooo.ooo.oRRR", O=""),
    ],
    "drill": [
        dict(K="x.........x.....", S="......x.......x.", C="......x.......x.", H="o..o..o.o..o..o.", O=""),
        dict(K="x.....x...x.....", S="......x...g...x.", C="", H="o..o..o.o..o.Ro.", O=""),
        dict(K="x..........x..x.", S="......x.......x.", C="......x.......x.", H="o..o..o.oR.o..o.", O="........o......."),
    ],
    "boom-bap": [
        dict(K="x.........x.....", S="....x.......x...", C="", H="o.o.o.o.o.o.o.o.", O=""),
        dict(K="x......x..x.....", S="....x.......x..g", C="", H="o.o.o.o.o.o.o.o.", O="..............o."),
        dict(K="x.......x.x.....", S="....x..g....x...", C="....x.......x...", H="oooooooooooooooo", O=""),
        dict(K="x..x......x..x..", S="....x.......x...", C="", H="o.o.o.o.o.o.o.o.", O="......o........."),
    ],
    "house": [
        dict(K="x...x...x...x...", S="", C="....x.......x...", H="g.o.g.o.g.o.g.o.", O="..o...o...o...o."),
        dict(K="x...x...x...x...", S="............g...", C="....x.......x...", H="gogogogogogogogo", O="..o...o...o...o."),
        dict(K="x...x...x...x..o", S="", C="....x.......x...", H="..o...o...o...o.", O="..o...o...o...o."),
    ],
    "techno": [
        dict(K="x...x...x...x...", S="", C="....o.......o...", H="g.o.g.o.g.o.g.o.", O="..o...o...o...o."),
        dict(K="x...x...x...x...", S="", C="", H="gogogogogogogogo", O="..o...o...o...o."),
        dict(K="x...x...x...x.o.", S="", C="....o.......o...", H="o.o.o.o.o.o.o.o.", O="..o...o...o...o."),
    ],
    "drum-and-bass": [
        dict(K="x.........x.....", S="....x.......x...", C="", H="o.o.o.o.o.o.o.o.", O=""),
        dict(K="x.........x..x..", S="....x..g....x...", C="", H="o.o.o.o.o.o.o.o.", O="..............o."),
        dict(K="x.x.......x.....", S="....x.......x.g.", C="....x.......x...", H="oooooooooooooooo", O=""),
    ],
    "edm": [
        dict(K="x...x...x...x...", S="....x.......x...", C="....x.......x...", H="..o...o...o...o.", O="..o...o...o...o."),
        dict(K="x...x...x...x...", S="", C="....x.......x...", H="g.o.g.o.g.o.g.o.", O="..o...o...o...o."),
        dict(K="x...x...x...x...", S="....x.......x...", C="", H="gogogogogogogogo", O="..o...o...o...o."),
    ],
    "reggaeton-afro": [
        dict(K="x...x...x...x...", S="...x..x....x..x.", C="", H="o.o.o.o.o.o.o.o.", O=""),
        dict(K="x...x...x...x...", S="...x..x....x..x.", C="...x..x....x..x.", H="o.goo.goo.goo.go", O=""),
        dict(K="x..x..x...x.....", S="....x.......x...", C="", H="o.o.o.o.o.o.o.o.", O="......o.......o."),
    ],
    "lo-fi": [
        dict(K="x........x......", S="....x.......x...", C="", H="o.o.o.o.o.o.o.o.", O=""),
        dict(K="x.....x...x.....", S="....x.......x..g", C="", H="o.o.o.o.o.o.o.oo", O=""),
        dict(K="x........x..x...", S="....x..g....x...", C="", H="o.o.o.o.o.o.o.o.", O="..............o."),
    ],
}
VEL = {"x": 1.0, "o": 0.78, "g": 0.35}
FILLS = ["....x.x.x.x.xxxx", "........x.x.x.gx", "..........g.x.xx", "............xgxx"]


def kept_kit(manifest, choices):
    """{fam: [(style, params, seed)]} for hits Benjamin kept, else empty."""
    out = {"snare": [], "clap": [], "hat_closed": []}
    for r in manifest:
        key = "batterie-elec__" + r["file"][:-4]
        if r["family"] in out and choices.get(key) == "garder":
            out[r["family"]].append((r["style"], r["params"], r["seed"]))
    return out


def render_hit(fam, p, seed, open_hat=False):
    p = dict(p)
    model = synth.MODELS[fam]
    if open_hat:
        p.update(decay_s=min(p["decay_s"] * 6, 0.45), tail=max(p["tail"], 0.45), len_s=0.7)
    y = model(p, np.random.default_rng(seed)).astype(np.float64)
    return y / max(np.abs(y).max(), 1e-9)


def events(tpl, rng, variant):
    """Expand a one-bar template into 4 bars of (lane, step32, vel)."""
    ev = []
    for b in range(4):
        for lane, s in tpl.items():
            for i, c in enumerate(s):
                if c == ".":
                    continue
                if c == "R":
                    for k in range(2):
                        ev.append(("H", 64 * b + 4 * i + 2 * k, 0.55 + 0.1 * k))
                    continue
                v = VEL[c]
                if lane == "H" and rng.random() < 0.08 * (variant % 3):
                    continue
                ev.append((lane, 64 * b + 4 * i, v))
        if b == 1 and variant % 2 and tpl.get("K"):  # kick push in bar 2
            ev.append(("K", 64 * b + 4 * 14, 0.7))
    # bar 4 fill on the snare lane (or the clap when the style has no snare)
    fl = FILLS[variant % len(FILLS)]
    lane = "S" if tpl.get("S") else "C"
    ev = [e for e in ev if not (e[0] == lane and e[1] >= 64 * 3 + 32)]
    for i, c in enumerate(fl):
        if c != "." and i >= 8:
            ev.append((lane, 64 * 3 + 4 * i, VEL.get(c, 0.6) * 0.9))
    return ev


def build(style, kit, ev, mix, rng):
    step32 = 60.0 / BPM[style] / 8
    n = int(round(128 * 2 * step32 * SR))
    lanes = {k: np.zeros(n + 2 * SR) for k in "KSCHO"}
    sw = SWING[style]
    ohat_end = []
    for lane, s, v in sorted(ev, key=lambda e: e[1]):
        t = s * step32
        if (s // 4) % 2 == 1 and s % 4 == 0:
            t += sw * 2 * step32
        t += rng.normal(0, 0.0025) if sw else 0.0
        i = max(0, int(t * SR))
        h = kit[lane]
        g = v * (1 + rng.normal(0, 0.05))
        if lane == "H" and ohat_end and ohat_end[-1][0] > i:  # choke the open hat
            j0, j1 = i, ohat_end[-1][0]
            fade = np.linspace(1, 0, min(256, j1 - j0))
            seg = lanes["O"][j0:j0 + len(fade)]
            lanes["O"][j0:j0 + len(fade)] = seg * fade
            lanes["O"][j0 + len(fade):j1] = 0
        lanes[lane][i:i + len(h)] += g * h
        if lane == "O":
            ohat_end.append((i + len(h), i))
    gains = {"K": 1.0, "S": mix["s"], "C": mix["c"], "H": mix["h"], "O": mix["o"]}
    mixd = sum(gains[k] * lanes[k] for k in lanes)
    side = mix["pan"] * (gains["H"] * lanes["H"] - gains["O"] * lanes["O"])  # hats left, open hat right
    room = pb.Pedalboard([pb.HighpassFilter(250), pb.Reverb(room_size=0.3, wet_level=1.0, dry_level=0.0, damping=0.6, width=1.0)])
    sd = ((lanes["S"] * mix["s"] + lanes["C"] * mix["c"] + 0.5 * lanes["H"] * mix["h"]) * mix["room"]).astype(np.float32)
    sd = np.stack([sd, np.roll(sd, 331)])
    send = room(sd, SR)
    y = np.stack([mixd + side, mixd - side]) + send
    y = y[:, :n].astype(np.float32)
    bus = pb.Pedalboard([pb.HighpassFilter(mix["hp"]), pb.HighShelfFilter(cutoff_frequency_hz=5000, gain_db=mix["hs"]),
                         pb.Compressor(threshold_db=mix["thr"], ratio=3.0, attack_ms=10, release_ms=120),
                         pb.Gain(mix["drive"]), pb.Clipping(threshold_db=-1.0)])
    return bus(y / max(np.abs(y).max(), 1e-9), SR).T


MIXR = {"s": (0.35, 1.0), "c": (0.25, 0.9), "h": (0.1, 0.6), "o": (0.08, 0.45), "room": (0.0, 0.35),
        "thr": (-30, -8), "drive": (0, 12), "pan": (0.0, 0.7), "hs": (0.0, 9.0), "hp": (20, 70)}


def mix_of(u):
    return {k: lo + (hi - lo) * float(np.clip(x, 0, 1)) for (k, (lo, hi)), x in zip(MIXR.items(), u)}


J = None


def init(tsv, desc):
    tgt.load(tsv, desc)


def make_variant(job):
    import cma
    style, v, kick_u, kick_seed, kitdef, evals = job
    rng0 = np.random.default_rng(zlib.crc32(("%s%d" % (style, v)).encode()))
    tpl = T[style][v % len(T[style])]
    ev = events(tpl, rng0, v)
    ky, _ = kick2.render(kstyles.K[style], np.array(kick_u), kick_seed)
    kit = {"K": ky.astype(np.float64)}
    for lane, fam, op in (("S", "snare", False), ("C", "clap", False), ("H", "hat_closed", False), ("O", "hat_closed", True)):
        st, p, sd = kitdef[lane]
        kit[lane] = render_hit(fam, p, sd, op)
    def f(u):
        y = build(style, kit, ev, mix_of(u), np.random.default_rng(5))
        return tgt.score(tgt.measure_st(y), FAM, tgt.CASE_L[style], "loop")[0]
    es = cma.CMAEvolutionStrategy([0.5] * len(MIXR), 0.3, {"bounds": [0, 1], "maxfevals": evals, "verbose": -9, "seed": v + 1})
    best = (99.0, None)
    while not es.stop():
        xs = es.ask()
        fs = [f(x) for x in xs]
        es.tell(xs, fs)
        i = int(np.argmin(fs))
        if fs[i] < best[0]:
            best = (fs[i], list(map(float, xs[i])))
    return dict(style=style, variant=v, score=best[0], mix=best[1], kit={k: (a[0], a[2]) for k, a in kitdef.items()},
                template=v % len(T[style]))


NEAR = {"trap": ["drill", "edm"], "drill": ["drill"], "boom-bap": ["boom-bap"], "house": ["edm", "techno"],
        "techno": ["techno", "edm"], "drum-and-bass": ["drum-and-bass"], "edm": ["edm"],
        "reggaeton-afro": ["boom-bap", "edm"], "lo-fi": ["boom-bap"]}


def pick(pool, style, k):
    near = [h for h in pool if h[0] in NEAR[style]] or pool
    return near[k % len(near)]


def load_choices(d):
    out = {}
    for fn in os.listdir(d):
        try:
            j = json.load(open(os.path.join(d, fn)))
        except ValueError:
            continue
        j = j.get("data", j)
        out[fn[:-5]] = j.get("choix")
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["opt", "export"])
    ap.add_argument("--targets", required=True)
    ap.add_argument("--desc", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--kit", required=True)
    ap.add_argument("--variants", type=int, default=8)
    ap.add_argument("--evals", type=int, default=60)
    ap.add_argument("--styles", default=",".join(BPM))
    a = ap.parse_args()
    kr = json.load(open(os.path.join(a.out, "kick-runs.json")))
    kit = json.load(open(a.kit))
    rj = os.path.join(a.out, "loop-runs.json")
    if a.cmd == "opt":
        runs = json.load(open(rj)) if os.path.exists(rj) else []
        done = {(r["style"], r["variant"]) for r in runs}
        jobs = []
        for s in a.styles.split(","):
            ks = sorted((r for r in kr if r["style"] == s), key=lambda r: r["score"])[:4]
            for v in range(a.variants):
                if (s, v) in done:
                    continue
                kd = {"S": pick(kit["snare"], s, v), "C": pick(kit["clap"], s, v // 2),
                      "H": pick(kit["hat_closed"], s, v), "O": pick(kit["hat_closed"], s, v + 1)}
                k = ks[v % len(ks)]
                jobs.append((s, v, k["u"], k["seed"], kd, a.evals))
        with mp.Pool(os.cpu_count(), init, (a.targets, a.desc)) as pool:
            for r in pool.imap_unordered(make_variant, jobs):
                r["kick_run"] = None
                runs.append(r)
                print(r["style"], r["variant"], round(r["score"], 2), flush=True)
                json.dump(runs, open(rj, "w"))
        return
    init(a.targets, a.desc)
    runs = json.load(open(rj))
    res = []
    for s in a.styles.split(","):
        ks = sorted((r for r in kr if r["style"] == s), key=lambda r: r["score"])[:4]
        rs = sorted((r for r in runs if r["style"] == s), key=lambda r: r["variant"])
        for r in rs:
            v = r["variant"]
            rng0 = np.random.default_rng(zlib.crc32(("%s%d" % (s, v)).encode()))
            ev = events(T[s][v % len(T[s])], rng0, v)
            k = ks[v % len(ks)]
            ky, _ = kick2.render(kstyles.K[s], np.array(k["u"]), k["seed"])
            kd = {"S": pick(kit["snare"], s, v), "C": pick(kit["clap"], s, v // 2),
                  "H": pick(kit["hat_closed"], s, v), "O": pick(kit["hat_closed"], s, v + 1)}
            kt = {"K": ky.astype(np.float64)}
            for lane, fam, op in (("S", "snare", False), ("C", "clap", False), ("H", "hat_closed", False), ("O", "hat_closed", True)):
                kt[lane] = render_hit(fam, kd[lane][1], kd[lane][2], op)
            y = build(s, kt, ev, mix_of(r["mix"]), np.random.default_rng(5))
            name = "%s-boucle-%02d.mp3" % (s, v + 1)
            path = os.path.join(a.out, name)
            tgt.to_mp3_st(y, path)
            sc = tgt.report(path, FAM, tgt.CASE_L[s], "loop")
            res.append(dict(file=name, style=s, family="boucle", bpm=BPM[s], bars=4, template=r["template"],
                            kick="%s-kick (kick2 run %d)" % (s, k["run"]),
                            kit_from_kept={kk: "%s %s" % (vv[0], kk) for kk, vv in kd.items()},
                            tool="numpy+pedalboard, CMA-ES mix", source="synthesised, no sample", license="CC0 (own work)",
                            opt_score=round(r["score"], 2), mix={kk: round(vv, 3) for kk, vv in mix_of(r["mix"]).items()}, **sc))
            print(name, sc["score"], sc["in_p25_p75"], sc["in_p10_p90"], sc["ranks"], sc["worst"], flush=True)
    json.dump(res, open(os.path.join(a.out, "loops.json"), "w"), indent=1, default=float)


if __name__ == "__main__":
    main()
