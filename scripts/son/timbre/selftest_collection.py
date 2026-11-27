#!/usr/bin/env python3
"""Self-test of the collection tool (collection.py): every measure and its independent
counter-measure on sounds whose answer is known, then the agreement on invented drum families,
then the "copie" trial (does any guard catch one sound redrawn with our own noise?).

    python3 -I scripts/son/timbre/selftest_collection.py
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import collection as C  # noqa: E402
import timbre as T  # noqa: E402

SR = T.SR
FAILED = []


def check(name, got, want, tol):
    ok = got == got and abs(got - want) <= tol
    print("%s\t%s\tgot %.4g, expected %.4g (tolerance %.3g)" % ("ok  " if ok else "FAIL", name, got, want, tol))
    if not ok:
        FAILED.append(name)


def truth(name, cond):
    print("%s\t%s" % ("ok  " if cond else "FAIL", name))
    if not cond:
        FAILED.append(name)


def main():
    rng = np.random.default_rng(0)
    t = np.arange(SR) / SR

    # A decaying 1 kHz sine after 30 ms of silence: onset 30 ms, t20/t40/t60 = 1, 2, 3 x tau ln 10.
    tau = 0.050
    x = np.concatenate([np.zeros(int(0.030 * SR)), np.sin(2 * math.pi * 1000.0 * t) * np.exp(-t / tau)])
    m, c = C.measure(x), C.counter(x)
    for k, mult in (("t20_ms", 1), ("t40_ms", 2), ("t60_ms", 3)):
        want = 1000.0 * mult * tau * math.log(10.0)
        check("measure: %s of exp(-t/tau)" % k, m[k], want, 1.0)
        check("counter: %s of exp(-t/tau)" % k, c[k], want, 1.0)
    check("counter onset after 30 ms of silence (ms)", 1000.0 * C.counter_onset(x) / SR, 30.0, 0.5)
    check("counter: centroid of a 1 kHz sine", c["centroid_hz"], 1000.0, 10.0)
    check("counter: t20 of the mid band", c["t20_mid_ms"], 1000.0 * tau * math.log(10.0), 2.0)

    # The counter's decay is not Schroeder's: a forward sum over 1 ms blocks, read between blocks.
    p = np.zeros(SR // 10)
    p[:480] = 1.0  # all the energy in the first 10 ms, flat: -20 dB is reached at 9.9 ms
    check("counter decay of a flat 10 ms block, -20 dB (ms)", C.counter_decay_ms(p, 0, 20.0), 9.9, 0.15)

    # White noise: share above 10 kHz by both roads.
    w = rng.standard_normal(SR) * np.exp(-t / 0.080)
    want = 10.0 * math.log10(10000.0 / 19980.0)
    check("measure: white noise hf share", C.measure(w)["hf_db"], want, 0.5)
    check("counter: white noise hf share", C.counter(w)["hf_db"], want, 0.5)

    # Attack against body: 10 ms at level 1 then 90 ms at level 0.1 (a +-1 square at Nyquist,
    # so the energies are exact; a random noise would move the answer by its own draw).
    b = np.where(np.arange(int(0.100 * SR)) % 2 == 0, 1.0, -1.0)
    b[int(0.010 * SR):] *= 0.1
    want = 10.0 * math.log10(10.0 / (90.0 * 0.01))
    check("measure: attack against body", C.measure(b)["transient_body_db"], want, 0.4)
    check("counter: attack against body", C.counter(b)["transient_body_db"], want, 0.4)

    # Resampling keeps the pitch: 1 kHz at 44.1 kHz read at 48 kHz.
    y = np.sin(2 * math.pi * 1000.0 * np.arange(44100) / 44100.0) * np.exp(-np.arange(44100) / 44100.0 / 0.05)
    check("resampled 44.1 kHz sine: centroid", C.measure(C.to_48k(y, 44100))["centroid_hz"], 1000.0, 10.0)
    truth("resampled length", abs(len(C.to_48k(y, 44100)) - 48000) <= 1)

    # Independence: the counter's code never calls the measure's onset, decay, spectrum or band
    # functions; it shares only the definitions (band masks, the 30 dB onset level).
    import inspect
    src = "".join(inspect.getsource(f) for f in (C.counter, C.counter_onset, C.counter_decay_ms,
                                                 C._kernel_centroid, C.fir_band))
    for name in ("fingerprint(", "onset_of(", "decay_index(", "_spectrum(", "band_signal(", "_centroid("):
        truth("independence: the counter never calls T.%s)" % name, "T." + name not in src)

    # Agreement rule.
    truth("agree: t20 within 5 ms", C.agree("t20_ms", 30.0, 34.0))
    truth("agree: t20 10 % but more than 5 ms is not", not C.agree("t20_ms", 100.0, 112.0))
    truth("agree: centroid within 10 %", C.agree("centroid_hz", 1000.0, 1090.0))
    truth("agree: hf 1.5 dB", C.agree("hf_db", -20.0, -21.4) and not C.agree("hf_db", -20.0, -21.6))
    truth("agree: one road empty is a disagreement", not C.agree("t20_low_ms", float("nan"), 20.0))
    truth("agree: both bands empty agree", C.agree("env0_db", T.FLOOR_DB, T.FLOOR_DB + 2.0))

    # Map threshold: the 5th centile of the nearest-neighbour distances, never the minimum.
    d = list(range(100))
    check("map threshold = 5th centile", C.map_threshold(d), 4.95, 1e-9)

    # Copy-guard calibration (the code that will run on the collection, cmd_carte) on invented
    # families written as measurement lines: its threshold is the 5th centile computed by hand,
    # a near copy of a sound falls under it, and at most 5 % of the family's sounds do.
    import tempfile
    lines, fam_maps = [], {}
    for fam, kind, seed in (("caisse_claire", "snare", 31), ("charley_ferme", "hat", 32), ("kick", "kick", 33)):
        refs = T.family(kind, 30, np.random.default_rng(seed))
        fam_maps[fam] = (refs, [T.band_time_map(r) for r in refs])
        for i, mp in enumerate(fam_maps[fam][1]):
            lines.append({"cle": "f%d" % i, "famille": fam, "style": "house", "pack": "r0/p%d" % (i % 3), "map": mp.tolist()})
    with tempfile.TemporaryDirectory() as tmp:
        res = {r[0]: r for r in C.cmd_carte(lines, os.path.join(tmp, "carte.tsv"))}
        header = open(os.path.join(tmp, "carte.tsv")).read().splitlines()[1]
    truth("carte: header has seuil_p5 and rapport_p95", "seuil_p5" in header and "rapport_p95" in header)
    truth("carte: a family under 10 sounds gets no threshold", res["clap"][4] == "")
    for fam, (refs, mp) in fam_maps.items():
        nn = [min(T.map_distance(mp[i], mp[j]) for j in range(30) if j != i) for i in range(30)]
        thr = T.quantile(nn, 0.05)
        check("carte %s: seuil_p5 = 5th centile by hand" % fam, float(res[fam][4]), thr, 0.006)
        truth("carte %s: at most 5 %% of the family under its own threshold" % fam, sum(d < thr for d in nn) <= 2)
        near = refs[7] * 0.999
        near[1:] += 0.01 * near[:-1]
        truth("carte %s: a near copy falls under the threshold" % fam,
              T.map_distance(T.band_time_map(near), mp[7]) < float(res[fam][4]))
        ratios = C.neighbour_ratios(mp)
        check("carte %s: rapport_p95 = 95th centile by hand" % fam, float(res[fam][7]), T.quantile(ratios, 0.95), 0.006)
        truth("carte %s: a near copy stands out from its neighbours" % fam,
              C.nearest_is(T.band_time_map(near), mp, 7, C.ratio_threshold(ratios)))

    # Independent counter: on invented drums (30 snares, hats, kicks), every measure agrees >= 98 %.
    sounds = []
    for kind, seed in (("snare", 31), ("hat", 32), ("kick", 33)):
        sounds += T.family(kind, 30, np.random.default_rng(seed))
    pairs = [(C.measure(s), C.counter(s)) for s in sounds]
    for k in C.ALL_KEYS:
        n, ok = C.agreement([a[k] for a, _ in pairs], [b[k] for _, b in pairs], k)
        check("agreement on 90 invented drums: %s (%d counted)" % (k, n), 100.0 * ok / max(n, 1), 100.0, 2.0)

    # The copie trial: one invented snare redrawn with our own noise on finer and finer grids.
    rows = C.copie_trial()
    for grid, caught in rows:
        print("copie\t%s\t%s" % (grid, "  ".join("%s %s" % (k, "attrapée" if v else "passe") for k, v in caught.items())))
    truth("copie: the sound itself (a hair of filtering) is caught by the 25x9 map", rows[-1][1]["carte 25x9"])
    by = dict(rows)
    truth("copie: the neighbour guard catches the 22 x 24 redraw that every distance guard lets pass",
          by["22 x 24"]["voisin 25x9"] and not any(v for k, v in by["22 x 24"].items() if k != "voisin 25x9"))
    truth("copie: nothing catches the coarse 9 x 6 redraw (it is no copy)", not any(by["9 x 6"].values()))

    ident_test()

    print("\n%s" % ("selftest: all green" if not FAILED else "selftest: %d FAILED" % len(FAILED)))
    return 1 if FAILED else 0


def ident_test():
    """ident.py on invented drums in three layouts: named by key, archive tree, renamed."""
    import tempfile
    import wave
    import ident as I
    rng = np.random.default_rng(41)
    base = tempfile.mkdtemp(prefix="ident-")
    src = os.path.join(base, "src")
    os.mkdir(src)

    def wav(path, x):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with wave.open(path, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(48000)
            w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())

    sounds = []
    for k in range(12):
        n = int(48000 * (0.15 + 0.05 * k))
        t = np.arange(n) / 48000
        tone = np.sin(2 * np.pi * (50 + 15 * k) * t) if k % 2 else rng.standard_normal(n) * 0.5
        sounds.append(0.9 * tone * np.exp(-t / (0.02 + 0.015 * k)))
    rows = []
    for k, x in enumerate(sounds):
        p = os.path.join(src, "s%d.wav" % k)
        wav(p, x)
        m = M_measure(p)
        r = {c: "%.6g" % m[c] if m.get(c) is not None else "" for c in I.COLS}
        r.update(cle="K%02d" % k, file_id="")
        rows.append(r)
    link = os.path.join(base, "link")
    wav(os.path.join(link, "K00.wav"), sounds[0])                     # named by key
    wav(os.path.join(link, "K01.wav"), sounds[5])                     # wrong name: the measures say K05
    tree = os.path.join(link, "x", "Pack A")
    wav(os.path.join(tree, "Kicks", "a.wav"), sounds[2])              # archive tree
    rows[2]["file_id"] = I.CMP.fid(tree, "Kicks/a.wav")
    wav(os.path.join(link, "y", "random name.wav"), sounds[3])        # renamed
    wav(os.path.join(link, "y", "copy.wav"), sounds[3])               # the same sound twice
    wav(os.path.join(link, "y", "z.wav"), rng.standard_normal(4800) * 0.01)  # no row at all
    got = {os.path.relpath(r["path"], link): r for r in I.identify(link, rows)}
    want = {"K00.wav": ("K00", "cle", "haute"), "K01.wav": ("K05", "mesures", "haute"),
            "x/Pack A/Kicks/a.wav": ("K02", "hachage", "haute")}
    for rel, w in want.items():
        g = got[rel]
        truth("ident: %s -> %s by %s (%s)" % (rel, w[0], w[1], w[2]), (g["cle"], g["methode"], g["confiance"]) == w)
    pair = [got["y/random name.wav"], got["y/copy.wav"]]
    truth("ident: the renamed sound is found by its measures, its double is left out",
          sorted(str(r["cle"]) for r in pair) == ["K03", "None"] and all(r["methode"] == "mesures" for r in pair))
    truth("ident: a sound with no row is left out", got["y/z.wav"]["cle"] is None)
    import shutil
    shutil.rmtree(base)


def M_measure(p):
    import ident as I
    return I.M.measure(p, "drum_oneshot")


if __name__ == "__main__":
    sys.exit(main())
