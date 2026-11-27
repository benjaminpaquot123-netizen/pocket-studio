#!/usr/bin/env python3
"""Timbre fingerprint on the real collection (docs/sons/fin/RECHERCHE-TIMBRE.md, step 0).

measure(x): the fingerprint of timbre.py plus t40_ms.
counter(x): the same 20 numbers by roads that share nothing with the measure: its own onset
(0.5 ms RMS blocks, not the first loud sample), its own decay (energy left after 1 ms blocks,
summed forward and read between blocks, not Schroeder per sample), time-domain filters for
the bands, a |frequency| kernel for the centroids, a hard 10-20 kHz filter for hf.

Numpy only. Run with `python3 -I`. Nothing measured on one bought sound is ever written to the
repository: the per-sound files go to a folder outside it, only family statistics come back.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import timbre as T  # noqa: E402

SR = T.SR
ALL_KEYS = T.KEYS + ("t40_ms",)
TIME_KEYS = ("t20_ms", "t40_ms", "t60_ms", "t20_low_ms", "t20_mid_ms", "t20_high_ms")
CENTROID_KEYS = ("centroid_hz", "centroid_att_hz", "centroid_tail_hz")


# --- Measure.

def measure(x):
    x = np.asarray(x, dtype=np.float64)
    out = T.fingerprint(x)
    onset = T.onset_of(x)
    out["t40_ms"] = 1000.0 * (T.decay_index(x * x, onset, 40) - onset) / SR
    return out


# --- Counter-measure.

ONSET_BLOCK = SR // 2000  # 0.5 ms
DECAY_BLOCK = SR // 1000  # 1 ms


def fftconv(a, b):
    n = len(a) + len(b) - 1
    m = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(a, m) * np.fft.rfft(b, m), m)[:n]


def fir_band(x, key, mask):
    """Band of x by the truncated linear-phase filter of timbre.py, delay removed."""
    h = T._fir(key, mask)
    d = (len(h) - 1) // 2
    return fftconv(x, h)[d:d + len(x)]


def counter_onset(x):
    """Start of the first 0.5 ms block whose RMS is within 30 dB of the loudest block."""
    x = np.asarray(x, dtype=np.float64)
    nb = len(x) // ONSET_BLOCK
    if nb == 0:
        raise ValueError("sound too short")
    e = (x[:nb * ONSET_BLOCK] ** 2).reshape(nb, ONSET_BLOCK).mean(axis=1)
    if e.max() <= 0:
        raise ValueError("silent sound")
    return int(np.argmax(e >= e.max() * 10.0 ** (T.ONSET_DB / 10.0))) * ONSET_BLOCK


def counter_decay_ms(power, onset, level_db):
    """Time after `onset` when the energy still to come falls `level_db` under the whole:
    1 ms blocks summed forward, read between two block edges (in dB, or in energy when the
    energy left reaches zero). If it never falls that far, the time to the end."""
    p = np.asarray(power[onset:], dtype=np.float64)
    nb = -(-len(p) // DECAY_BLOCK)
    blocks = np.add.reduceat(p, np.arange(0, nb * DECAY_BLOCK, DECAY_BLOCK)[:nb])
    total = float(blocks.sum())
    if total <= 0:
        return float("nan")
    left = np.maximum(total - np.concatenate([[0.0], np.cumsum(blocks)]), 0.0)
    goal = total * 10.0 ** (-level_db / 10.0)
    below = np.nonzero(left < goal)[0]
    if not len(below):
        return 1000.0 * len(p) / SR
    k = int(below[0])
    a, b = left[k - 1], left[k]
    if a > 0 and b > 0:
        frac = (math.log10(a) - math.log10(goal)) / (math.log10(a) - math.log10(b))
    else:
        frac = (a - goal) / (a - b)
    ms_per_block = 1000.0 * DECAY_BLOCK / SR
    return min((k - 1 + frac) * ms_per_block, 1000.0 * len(p) / SR)


def _hard_hf(f):
    return ((f >= 10000.0) & (f <= 20000.0)).astype(float)


def _kernel_centroid(s, taper):
    if len(s) < 16:
        return float("nan")
    y = fftconv(T._taper(s, taper), T._LOWPASS_20K)
    d = (len(T._ABS_OMEGA) - 1) // 2
    ky = fftconv(y, T._ABS_OMEGA)[d:d + len(y)]
    yy = float((y * y).sum())
    return float((y * ky).sum() / yy * SR / (2.0 * math.pi)) if yy > 0 else float("nan")


def counter(x):
    x = np.asarray(x, dtype=np.float64)
    out = {k: float("nan") for k in ALL_KEYS}
    onset = counter_onset(x)
    power = x * x
    for lvl in (20, 40, 60):
        out["t%d_ms" % lvl] = counter_decay_ms(power, onset, lvl)
    cut = onset + int(round(0.010 * SR))
    e_t, e_b = float(power[onset:cut].sum()), float(power[cut:].sum())
    if e_t > 0 and e_b > 0:
        out["transient_body_db"] = 10.0 * math.log10(e_t / e_b)

    i60 = onset + int(round(out["t60_ms"] * SR / 1000.0))
    a = max(0, onset - int(0.001 * SR))
    seg = x[a:max(i60, a + 16)]
    pad = np.concatenate([np.zeros(4096), seg, np.zeros(4096)])
    e = [float((fir_band(pad, ("env", i), lambda f, i=i: T._env_mask(f, i)) ** 2).sum())
         for i in range(len(T.OCT_EDGES) - 1)]
    tot = sum(e)
    if tot > 0:
        for i, v in enumerate(e):
            out[T.ENV_KEYS[i]] = max(10.0 * math.log10(max(v / tot, 1e-30)), T.FLOOR_DB)
        hf = float((fir_band(pad, "hardhf", _hard_hf) ** 2).sum())
        out["hf_db"] = max(10.0 * math.log10(max(hf / tot, 1e-30)), -80.0)

    whole = float(power[onset:].sum())
    for g, name in enumerate(("low", "mid", "high")):
        p = fir_band(x, ("group", g), lambda f, g=g: T._group_mask(f, g)) ** 2
        if p[onset:].sum() > whole * 10.0 ** (-30.0 / 10.0):
            out["t20_%s_ms" % name] = counter_decay_ms(p, onset, 20)

    out["centroid_hz"] = _kernel_centroid(seg, None)
    out["centroid_att_hz"] = _kernel_centroid(x[a:onset + int(0.020 * SR)], "out")
    tail = x[onset + int(0.040 * SR):i60]
    if len(tail) >= int(0.010 * SR):
        out["centroid_tail_hz"] = _kernel_centroid(tail, "both")
    return out


# --- Agreement between the two roads.

def agree(k, a, b):
    if math.isnan(a) and math.isnan(b):
        return True
    if math.isnan(a) or math.isnan(b):
        return False
    if k in TIME_KEYS:
        return abs(a - b) <= 5.0 or abs(a - b) <= 0.10 * abs(a)
    if k in CENTROID_KEYS:
        return abs(a - b) <= 0.10 * abs(a)
    if k == "hf_db":
        return abs(a - b) <= 1.5
    if k in T.ENV_KEYS and a <= T.FLOOR_DB + 5.0 and b <= T.FLOOR_DB + 5.0:
        return True
    return abs(a - b) <= 1.0  # transient_body_db and the octave bands


def counted(k, a, b):
    """A pair counts unless both roads find nothing (no value, or both bands empty)."""
    if math.isnan(a) and math.isnan(b):
        return False
    if k in T.ENV_KEYS and a <= T.FLOOR_DB + 5.0 and b <= T.FLOOR_DB + 5.0:
        return False
    return True


def agreement(av, bv, k):
    n = ok = 0
    for a, b in zip(av, bv):
        if counted(k, a, b):
            n += 1
            ok += agree(k, a, b)
    return n, ok


# --- Reading.

def to_48k(y, sr):
    y = np.asarray(y, dtype=np.float64)
    if sr == SR:
        return y.copy()
    pad = 4096
    yp = np.concatenate([np.zeros(pad), y, np.zeros(pad)])
    n_out = int(round(len(yp) * SR / sr))
    z = np.fft.irfft(np.fft.rfft(yp), n_out) * (n_out / len(yp))
    a = int(round(pad * SR / sr))
    return z[a:a + int(round(len(y) * SR / sr))]


# --- Copy guard.

def map_threshold(nearest):
    """A copy is closer to its sound than 95 % of the family's sounds are to their nearest
    neighbour: the 5th centile, never the minimum (one twin pair would set it to zero)."""
    return T.quantile(list(nearest), 0.05)


def neighbour_ratios(maps):
    """For each sound: distance to its second nearest neighbour over the distance to its
    nearest (1 = two equal neighbours, 2 = twice as close to one of them)."""
    out = []
    for i in range(len(maps)):
        d = sorted(T.map_distance(maps[i], maps[j]) for j in range(len(maps)) if j != i)
        out.append(d[1] / d[0] if d[0] > 0 else float("inf"))
    return out


def ratio_threshold(ratios):
    """The 'one neighbour stands out' guard: 95 % of the family's own sounds are not that much
    closer to one sound than to the next (the 95th centile of the neighbour ratios)."""
    return T.quantile(list(ratios), 0.95)


def nearest_is(m, maps, target, ratio_thr):
    """True when the map m is nearest to maps[target] and that much nearer than to any other:
    a redraw too coarse to fall under the distance threshold can still give itself away."""
    d = [T.map_distance(m, q) for q in maps]
    order = sorted(range(len(d)), key=d.__getitem__)
    return order[0] == target and d[order[0]] > 0 and d[order[1]] / d[order[0]] > ratio_thr


def fine_map(x, bands, slices, n=1024, hop=256):
    """Like timbre.band_time_map with a finer grid: `bands` log bands from 20 Hz to 20 kHz
    over `slices` geometric slices of the first second, dB under the whole, clamped."""
    x = np.asarray(x, dtype=np.float64)
    x = x[T.onset_of(x):][:SR]
    xp = np.concatenate([x, np.zeros(n)])
    fr = np.lib.stride_tricks.sliding_window_view(xp, n)[::hop] * np.hanning(n)
    power = np.abs(np.fft.rfft(fr, axis=1)) ** 2
    f = np.fft.rfftfreq(n, 1.0 / SR)
    fe = np.searchsorted(f, np.geomspace(20.0, 20000.0, bands + 1))
    te = np.unique(np.round(np.geomspace(1, len(power), slices + 1)).astype(int))
    te[0] = 0
    total = float(power.sum())
    cells = np.zeros((len(te) - 1, bands))
    for i, (ta, tb) in enumerate(zip(te[:-1], te[1:])):
        col = power[ta:tb].sum(axis=0)
        for j, (fa, fb) in enumerate(zip(fe[:-1], fe[1:])):
            cells[i, j] = col[fa:max(fb, fa + 1)].sum()
    return np.maximum(10.0 * np.log10(np.maximum(cells / total, 1e-30)), -70.0)


DETECTORS = {
    "carte 25x9": T.band_time_map,
    "carte 64x24": lambda x: fine_map(x, 64, 24),
    "carte 128x48": lambda x: fine_map(x, 128, 48),
}


def _redraw(ref, bands, slices, rng):
    sys.path.insert(0, HERE)
    import etude  # the study's own redraw, untouched
    return etude.redraw(ref, bands, slices, rng)


COPIE_KINDS = (("snare", 31), ("kick", 33), ("hat", 32))


def copie_trial(kind="snare", family_seed=31, target=7):
    """One invented sound of `kind` (snare, kick, hat) redrawn with our own noise on finer and finer grids; which guard
    catches it? A guard catches when the redraw is closer to the sound than the guard's
    threshold (5th centile of the family's nearest-neighbour distances); the null test
    catches when the redraw cancels the sound deeper than any neighbour does (p5 too)."""
    refs = T.family(kind, 30, np.random.default_rng(family_seed))
    maps = {k: [f(r) for r in refs] for k, f in DETECTORS.items()}
    thr = {k: map_threshold(min(T.map_distance(m[i], m[j]) for j in range(len(m)) if j != i)
                            for i in range(len(m))) for k, m in maps.items()}
    null_thr = map_threshold(min(T.null_db(refs[i], refs[j]) for j in range(len(refs)) if j != i)
                             for i in range(len(refs)))
    ref = refs[target]
    m25 = maps["carte 25x9"]
    ratio_thr = ratio_threshold(neighbour_ratios(m25))

    def caught(y):
        out = {k: T.map_distance(DETECTORS[k](y), maps[k][target]) < thr[k] for k in DETECTORS}
        out["voisin 25x9"] = nearest_is(T.band_time_map(y), m25, target, ratio_thr)
        out["nul"] = T.null_db(ref, y) < null_thr
        return out

    rows = []
    for bands, slices in ((9, 6), (9, 12), (25, 12), (25, 24), (64, 48), (128, 96), (513, 100000)):
        y, nb, ns = _redraw(ref, bands, slices, np.random.default_rng(5))
        rows.append(("%d x %d" % (nb, ns), caught(y)))
    y = ref * 1.0
    y[1:] += 0.02 * y[:-1]
    rows.append(("le son lui-même", caught(y)))
    return rows


# --- The collection: one archive at a time, per-sound lines outside the repository.

REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
VERIF = os.path.join(REPO, "scripts", "sons", "passe2", "verif")
TABLES = os.path.join(REPO, "docs", "sons", "passe2", "tout")
EXCLUSIONS = os.path.join(REPO, "docs", "sons", "fin", "exclusions-batterie.tsv")
FAMILY = {"kick": "kick", "kick_808": "808", "snare": "caisse_claire", "clap": "clap",
          "hat_closed": "charley_ferme", "hat_open": "charley_ouvert", "crash": "crash",
          "rim": "rim", "tom": "tom"}
FAMILIES = ("kick", "808", "caisse_claire", "clap", "charley_ferme", "charley_ouvert", "crash", "rim", "tom")
STYLES = (("house", ("house",), ""), ("techhouse", ("house", "techno"), "house+techno"),
          ("techno", ("techno",), ""), ("trap", ("trap", "drift-phonk"), "trap+drift-phonk"),
          ("dnb", ("dnb",), ""), ("dubstep", ("dubstep",), ""))
TABLE_STYLES = {s for _, srcs, _ in STYLES for s in srcs}
MAX_S = 6.0  # seconds kept after the onset (a crash's t60 stays well inside)


def scope_rows(rar):
    import csv
    excl = set()
    with open(EXCLUSIONS) as f:
        for r in csv.DictReader(f, delimiter="\t"):
            excl.add(r["cle"])
    rows = []
    with open(os.path.join(TABLES, "drum_oneshot.csv"), newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if ((rar is None or r["rar"] == str(rar)) and r["subtype"] in FAMILY and r["style"] in TABLE_STYLES
                    and r["cle"] not in excl):
                rows.append(r)
    return rows


def _one(job):
    cle, path = job
    from ident import M  # passe2/verif/mesure.py, never its namesake in scripts/sons
    try:
        sr, x = M.read_audio(path)
        y = to_48k(M.mono(x), sr)
        y = y[:T.onset_of(y) + int(MAX_S * SR)]
        m, c = measure(y), counter(y)
        mp = np.round(T.band_time_map(y), 1).tolist()
        return {"cle": cle, "m": m, "c": c, "map": mp}
    except Exception as e:  # a file that cannot be read is counted, not hidden
        return {"cle": cle, "error": "%s: %s" % (type(e).__name__, e)}


def flat_jobs(root, rows):
    """The WeTransfer layout: every sound at <root>/<cle>.<ext>, whatever its archive."""
    by_key = {}
    for name in os.listdir(root):
        stem, ext = os.path.splitext(name)
        if ext.lower() in (".wav", ".aif", ".aiff", ".flac") and not os.path.islink(os.path.join(root, name)):
            by_key[stem] = os.path.join(root, name)
    jobs, missing = [], []
    for r in rows:
        p = by_key.get(r["cle"])
        (jobs.append((r["cle"], p)) if p else missing.append(r["cle"]))
    return jobs, missing


def cmd_mesure(root, rar, out):
    import json
    import multiprocessing
    from ident import CMP
    how = {}
    if rar == "auto":
        import ident
        rows = scope_rows(None)
        keep = {r["cle"] for r in rows}
        found = {d["cle"]: d for d in ident.identify(root) if d["cle"] in keep}
        how = {k: (d["methode"], d["confiance"]) for k, d in found.items()}
        jobs = [(k, d["path"]) for k, d in sorted(found.items())]
        rows = [r for r in rows if r["cle"] in found]
        missing = []
    elif rar == "plat":
        rows = scope_rows(None)
        jobs, missing = flat_jobs(root, rows)
    else:
        rows = scope_rows(rar)
        by_pack = {}
        for r in rows:
            by_pack.setdefault(r["pack"], []).append(r)
        dirs = CMP.find_pack_dirs(root, by_pack)
        jobs, missing = [], []
        for pk, rs in sorted(by_pack.items()):
            ids = CMP.ids_under(dirs[pk]) if pk in dirs else {}
            for r in rs:
                p = ids.get(r["file_id"])
                (jobs.append((r["cle"], p)) if p else missing.append(r["cle"]))
    with multiprocessing.Pool(4) as pool:
        res = pool.map(_one, jobs)
    meta = {r["cle"]: r for r in rows}
    with open(out, "a") as f:
        for d in res:
            r = meta[d["cle"]]
            d.update(famille=FAMILY[r["subtype"]], style=r["style"], pack="r%s/%s" % (r["rar"], r["pack"]))
            if d["cle"] in how:
                d["methode"], d["confiance"] = how[d["cle"]]
            f.write(json.dumps(d) + "\n")
        for cle in missing:
            r = meta[cle]
            f.write(json.dumps({"cle": cle, "error": "not found", "famille": FAMILY[r["subtype"]],
                                "style": r["style"], "pack": "r%s/%s" % (r["rar"], r["pack"])}) + "\n")
    err = sum(1 for d in res if "error" in d)
    print("rar%s: %d in scope, %d measured, %d unreadable, %d not found" % (
        rar, len(rows), len(res) - err, err, len(missing)))


def load_lines(paths):
    import json
    seen, out = set(), []
    for p in paths:
        with open(p) as f:
            for line in f:
                d = json.loads(line)
                if d["cle"] not in seen:
                    seen.add(d["cle"])
                    out.append(d)
    return out


def _db_key(k):
    return k in ("hf_db", "transient_body_db") or k in T.ENV_KEYS


# Measures kept in the table but left out of the rule. t20_low: its filter has a hard edge at
# 20 Hz, so a sound with a rumble or an offset near 20 Hz gets a length that depends on how the
# filter is built (a 15 Hz rumble under a snare: 178 ms by the FFT mask, 142 ms by the FIR).
# The two roads then disagree on 2 % of the sounds, and only a new measure could fix it.
OUT_OF_RULE = {"t20_low_ms": "hors règle : la tenue des graves dépend du filtre à 20 Hz"}


def target(cell, k, label):
    """The drum rule (docs/sons/fin/REGLE-BATTERIE.md), on one measure of one case."""
    vals = [(d["pack"], d["m"][k], agree(k, d["m"][k], d["c"][k])) for d in cell
            if not math.isnan(d["m"][k])]
    xs = [v[1] for v in vals]
    packs = {}
    for p, _, _ in vals:
        packs[p] = packs.get(p, 0) + 1
    n = len(xs)
    q = [T.quantile(xs, a) for a in (0.10, 0.25, 0.50, 0.75, 0.90)] if xs else [None] * 5
    acc = 100.0 * sum(v[2] for v in vals) / n if n else float("nan")
    statut = OUT_OF_RULE.get(k)
    if statut:
        pass
    elif n < 20:
        statut = "pas de cible : (1) %d sons (moins de 20)" % n
    elif sum(1 for c in packs.values() if c >= 5) < 2:
        statut = "pas de cible : (2) moins de 2 packs de 5 sons"
    elif max(packs.values()) > 0.90 * n:
        statut = "pas de cible : (3) un pack a %d %% des sons" % round(100.0 * max(packs.values()) / n)
    elif acc < 98.0:
        statut = "pas de cible : (4) contre-mesure d'accord sur %d %% seulement" % int(acc)
    else:
        big = max(sorted(packs), key=lambda p: packs[p])
        m2 = T.quantile([v[1] for v in vals if v[0] != big], 0.5)
        out = not (q[1] <= m2 <= q[3])
        moved = abs(m2 - q[2]) >= 1.0 if _db_key(k) else abs(m2 - q[2]) >= 0.20 * abs(q[2])
        if out and moved:
            statut = "pas de cible : (5) sans le plus gros pack la médiane passe à %.4g" % m2
    if statut is None:
        statut = "définitive" + (" (regroupement %s)" % label if label else "")
    return n, packs, q, acc, statut


def fmt(x):
    if x is None or (isinstance(x, float) and math.isnan(x)):
        return ""
    return ("%.4f" % x).rstrip("0").rstrip(".")


def cells(lines):
    good = [d for d in lines if "error" not in d]
    for fam in FAMILIES:
        for style, srcs, label in STYLES:
            yield fam, style, label, [d for d in good if d["famille"] == fam and d["style"] in srcs]


def cmd_table(lines, out):
    head = ["famille", "style", "mesure", "coups", "packs", "p10", "p25", "mediane", "p75", "p90",
            "accord_contre", "statut"]
    rows = []
    for fam, style, label, cell in cells(lines):
        for k in ALL_KEYS:
            n, packs, q, acc, statut = target(cell, k, label)
            rows.append([fam, style, k, str(n), str(len(packs))] + [fmt(x) for x in q] + [fmt(acc), statut])
    with open(out, "w") as f:
        f.write("# Empreinte du timbre sur la collection, par famille et par style (règle : "
                "docs/sons/fin/REGLE-BATTERIE.md), écrite par scripts/son/timbre/collection.py.\n")
        f.write("# Statistiques de famille seulement ; accord_contre = %% des sons où la contre-mesure "
                "indépendante donne le même nombre ; « pas de cible » = la première condition qui échoue.\n")
        f.write("\t".join(head) + "\n")
        for r in rows:
            f.write("\t".join(r) + "\n")
    return rows


def cmd_carte(lines, out):
    """Copy-guard calibration: per family (all styles), each sound's nearest neighbour on the
    25x9 map; the threshold is the 5th centile."""
    good = [d for d in lines if "error" not in d]
    res = []
    for fam in FAMILIES:
        fs = [d for d in good if d["famille"] == fam]
        if len(fs) < 10:
            res.append([fam, str(len(fs)), str(len({d["pack"] for d in fs})), "", "", "", "", ""])
            continue
        mp = [np.array(d["map"]) for d in fs]
        nn = [min(T.map_distance(mp[i], mp[j]) for j in range(len(mp)) if j != i) for i in range(len(mp))]
        res.append([fam, str(len(fs)), str(len({d["pack"] for d in fs})), fmt(min(nn)),
                    fmt(map_threshold(nn)), fmt(T.quantile(nn, 0.5)), fmt(T.quantile(nn, 0.95)),
                    fmt(ratio_threshold(neighbour_ratios(mp)))])
    with open(out, "w") as f:
        f.write("# Seuil anti-copie : distance de chaque son à son plus proche voisin sur la carte "
                "25 bandes x 9 tranches (dB), par famille, tous styles ; seuil = 5e centile ; "
                "rapport_p95 = 95e centile de (2e voisin / 1er voisin).\n")
        f.write("famille\tsons\tpacks\tmin\tseuil_p5\tmediane\tp95\trapport_p95\n")
        for r in res:
            f.write("\t".join(r) + "\n")
    return res


# --- Step 1: the rule replayed on the app's sounds (nouvelle) and their candidates (A / B / deep).

TIE = 0.05
LENGTH_TABLE = os.path.join(REPO, "docs", "sons", "fin", "cibles-batterie-definitives.tsv")


def read_targets(path, keys=None, length_table=False):
    """(family, style) -> measure -> (p10, p25, median, p75, p90), definitive rows only."""
    out = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            c = line.rstrip("\n").split("\t")
            if line.startswith("#") or c[0] == "famille":
                continue
            if length_table:
                fam, style, k, q, st = c[0], c[1], c[5], c[6:11], c[11]
            else:
                fam, style, k, q, st = c[0], c[1], c[2], c[5:10], c[11]
            if not st.startswith("définitive") or (keys and k not in keys):
                continue
            out.setdefault((fam, style), {})[k] = tuple(float(v) for v in q)
    return out


def score(m, tg, at):
    """Mean gap 2|v - T| / (p90 - p10) over the case's definitive measures; T = quantile index at."""
    gaps = [2.0 * abs(m[k] - t[at]) / (t[4] - t[0]) for k, t in tg.items() if t[4] > t[0] and not math.isnan(m[k])]
    return sum(gaps) / len(gaps) if gaps else float("nan")


def winner(cands, tg, at):
    sc = {v: score(m, tg, at) for v, m in cands.items()}
    best = min(sc, key=lambda v: sc[v])
    if "nouvelle" in sc and sc["nouvelle"] <= sc[best] + TIE:
        best = "nouvelle"
    return best, sc


def app_sounds(binary):
    """The app's 54 chosen sounds, played by the engine's voice (render_voice list / render), 4 s."""
    import subprocess
    keys, recs = [], []
    for line in subprocess.run([binary, "list"], stdout=subprocess.PIPE, check=True).stdout.decode().splitlines():
        v = line.split()
        keys.append((FAMILY_ORDER[int(v[0])], STYLE_ORDER[int(v[1])]))
        recs.append(" ".join(v[2:]))
    raw = subprocess.run([binary, "render", str(4 * SR)], input=("\n".join(recs) + "\n").encode(),
                         stdout=subprocess.PIPE, check=True).stdout
    ys = np.frombuffer(raw, dtype="<f4").reshape(len(recs), 4 * SR).astype(np.float64)
    return dict(zip(keys, ys))


FAMILY_ORDER = ("kick", "808", "clap", "caisse_claire", "charley_ferme", "charley_ouvert", "rim", "tom", "crash")
STYLE_ORDER = ("house", "techhouse", "techno", "trap", "dnb", "dubstep")


def cmd_etape1(table, raw_dir, binary, out):
    sys.path.insert(0, os.path.join(REPO, "scripts", "sons"))
    import mesure as MS
    app = app_sounds(binary)
    tg = read_targets(table)
    lt = read_targets(LENGTH_TABLE, keys=("t20_ms", "t60_ms"), length_table=True)
    sounds = {}
    for name in sorted(os.listdir(raw_dir)):
        fam_style, ver = name[:-4].rsplit("_", 1)
        fam, style = fam_style.rsplit("_", 1)
        y = np.fromfile(os.path.join(raw_dir, name), dtype="<f4").astype(np.float64)
        sounds.setdefault((fam, style), {})[ver] = y
    lines, inside_len, n_len, sure, n_case = [], 0, 0, 0, 0
    lines.append("famille\tstyle\tmesures_cible\tdans_fourchette\tlongueurs\tgagnant\tverdict\tscores")
    for fam in FAMILIES:
        for style, _, _ in STYLES:
            vers = sounds.get((fam, style), {})
            if (fam, style) not in app:
                continue
            y = app[(fam, style)]
            tm = MS.measure(SR, y)
            lens = lt.get((fam, style), {})
            li = sum(1 for k, t in lens.items() if t[0] <= tm[k] <= t[4])
            inside_len += li
            n_len += len(lens)
            t = tg.get((fam, style), {})
            if not t:
                lines.append("%s\t%s\t0\t\t%d/%d\t\tpas de cible\t" % (fam, style, li, len(lens)))
                continue
            cands = {v: measure(x) for v, x in vers.items()}
            m = measure(y)
            ins = sum(1 for k, q in t.items() if q[0] <= m[k] <= q[4])
            ws = [winner(cands, t, at) for at in (1, 2, 3)]
            names = {w for w, _ in ws}
            ok = len(names) == 1
            n_case += 1
            sure += ok
            win = ws[1][0] if ok else "nouvelle"
            sc = " ".join("%s=%.2f" % (v, s) for v, s in sorted(ws[1][1].items()))
            lines.append("%s\t%s\t%d\t%d\t%d/%d\t%s\t%s\t%s" % (
                fam, style, len(t), ins, li, len(lens), win, "sûr" if ok else "serré (%s)" % "/".join(w for w, _ in ws), sc))
    with open(out, "w") as f:
        f.write("\n".join(lines) + "\n")
    print("cases with a definitive timbre target: %d; sûr: %d (%.0f %%)" % (n_case, sure, 100.0 * sure / max(n_case, 1)))
    print("lengths inside: %d of %d" % (inside_len, n_len))


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("mesure")
    a.add_argument("root")
    a.add_argument("--rar", required=True, help="archive number, 'plat' for <root>/<cle>.wav, 'auto' to find each sound (ident.py)")
    a.add_argument("--out", required=True)
    b = sub.add_parser("table")
    b.add_argument("lines", nargs="+")
    b.add_argument("--out", required=True)
    b.add_argument("--carte", required=True)
    c = sub.add_parser("copie")
    e = sub.add_parser("etape1")
    e.add_argument("table")
    e.add_argument("raw")
    e.add_argument("render_voice")
    e.add_argument("--out", required=True)
    args = ap.parse_args(argv)
    if args.cmd == "mesure":
        cmd_mesure(args.root, args.rar, args.out)
    elif args.cmd == "table":
        lines = load_lines(args.lines)
        cmd_table(lines, args.out)
        cmd_carte(lines, args.carte)
    elif args.cmd == "etape1":
        cmd_etape1(args.table, args.raw, args.render_voice, args.out)
    elif args.cmd == "copie":
        for kind, seed in COPIE_KINDS:
            for grid, caught in copie_trial(kind, seed):
                print("%s\t%s\t%s" % (kind, grid, "\t".join("%s=%s" % (k, "attrapée" if v else "passe") for k, v in caught.items())))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
