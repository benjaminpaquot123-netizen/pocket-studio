#!/usr/bin/env python3
"""Second blind critique of the non-drum sound targets.

Recomputes every row of docs/sons/fin/cibles-autres-definitives.tsv straight
from the per-sound tables (docs/sons/passe2/tout/*.csv) and the independent
re-measure (docs/sons/passe2/verif/*.csv). Written without reading or importing
cibles_autres.py / critique_autres.py. Standard library only.

Usage:
  critique2_autres.py            # recompute, print summary, write gap file
  critique2_autres.py --selftest # check helpers on known inputs
"""
import csv
import glob
import os
import random
import statistics
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
P2 = os.path.join(ROOT, "docs", "sons", "passe2")
TSV = os.path.join(ROOT, "docs", "sons", "fin", "cibles-autres-definitives.tsv")
OUT = os.path.join(ROOT, "docs", "sons", "fin", "critique2-autres-ecarts.tsv")

MIN_SONS, MIN_PACK_SIZE, MIN_PACKS, MAX_PART, MIN_ACCORD = 20, 5, 2, 90, 98
TEMPO_TOL = 2.0  # percent, pack medians that agree


def quantile(xs, q):
    """Linear interpolation between order statistics (type 7)."""
    s = sorted(xs)
    if not s:
        return None
    h = (len(s) - 1) * q
    lo = int(h)
    hi = min(lo + 1, len(s) - 1)
    return s[lo] + (h - lo) * (s[hi] - s[lo])


def fnum(v):
    try:
        x = float(v)
    except (TypeError, ValueError):
        return None
    return x if x == x else None


def read(name):
    with open(os.path.join(P2, "tout", name + ".csv"), newline="") as fh:
        return list(csv.DictReader(fh))


def load_verif():
    ok = {}
    for f in glob.glob(os.path.join(P2, "verif", "*.csv")):
        with open(f, newline="") as fh:
            for r in csv.DictReader(fh):
                ok[r["cle"]] = r
    return ok


def family_rows():
    """Yield (famille, row, measure->value)."""
    out = []
    for r in read("oneshot_melodic"):
        fam = "basse (coups)" if r["role"] == "bass" else "coup mélodique : " + r["role"]
        out.append((fam, r))
    for name, fam in (("melodic_loop", "boucle mélodique"), ("drum_loop", "boucle de batterie"),
                      ("vocal", "voix"), ("fx", "effet"), ("midi", "midi")):
        for r in read(name):
            out.append((fam, r))
    return out


def measures(fam):
    if fam == "midi":
        return ["notes_per_bar", "etendue_demitons", "bars"]
    base = ["dur_s", "rms_dbfs", "peak_dbfs"]
    if fam == "voix":
        base += ["range_st", "voiced_pct"]
    return base


def value(r, m):
    if m == "etendue_demitons":
        a, b = fnum(r.get("pitch_min")), fnum(r.get("pitch_max"))
        return None if a is None or b is None else b - a
    return fnum(r.get(m))


def pack(r):
    return "r%s%s" % (r["rar"], r["pack"])


def stats_line(items, m, verif):
    """items: list of rows; returns dict of recomputed figures for measure m."""
    vals = [(r, value(r, m)) for r in items]
    vals = [(r, v) for r, v in vals if v is not None]
    n = len(vals)
    counts = {}
    for r, _ in vals:
        counts[pack(r)] = counts.get(pack(r), 0) + 1
    p5 = sorted(p for p, c in counts.items() if c >= MIN_PACK_SIZE)
    big = max(counts.values()) if counts else 0
    part = round(100.0 * big / n) if n else 0
    xs = [v for _, v in vals]
    acc = None
    okcol = m + "_ok"
    if any(okcol in v for v in list(verif.values())[:1]):
        good = sum(1 for r, _ in vals if verif.get(r["cle"], {}).get(okcol) == "yes")
        acc = (good, n)
    # median without the biggest pack
    bigp = max(counts, key=lambda p: counts[p]) if counts else None
    rest = [v for r, v in vals if pack(r) != bigp]
    d = dict(n=n, packs=len(counts), packs5=len(p5), part=part, bigpack=bigp,
             liste=" ".join(sorted(counts)), counts=counts,
             q=[quantile(xs, q) for q in (0.1, 0.25, 0.5, 0.75, 0.9)],
             acc=acc, med_wo=statistics.median(rest) if rest else None)
    accord_ok = acc is not None and n and 100.0 * acc[0] / acc[1] >= MIN_ACCORD
    d["statut"] = ("définitive" if n >= MIN_SONS and len(p5) >= MIN_PACKS and part <= MAX_PART
                   and accord_ok else "pas de cible, on garde le son actuel")
    return d


def tempo_line(items, fold_tol):
    """Manufacturer tempo: tag median per pack over confirmed loops."""
    tagged = [r for r in items if fnum(r["bpm_tag"]) is not None]
    both = [r for r in tagged if fnum(r.get("bpm_gap_folded_pct")) is not None]
    conf = [r for r in both if abs(fnum(r["bpm_gap_folded_pct"])) <= fold_tol]
    per = {}
    for r in conf:
        per.setdefault(pack(r), []).append(fnum(r["bpm_tag"]))
    votes = {p: (statistics.median(v), len(v)) for p, v in per.items() if len(v) >= MIN_PACK_SIZE}
    meds = sorted(v[0] for v in votes.values())
    agree = any(abs(a - b) / min(a, b) * 100 <= TEMPO_TOL
                for i, a in enumerate(meds) for b in meds[i + 1:])
    xs = [fnum(r["bpm_tag"]) for r in tagged]
    return dict(n=len(tagged), acc=(len(conf), len(both)), votes=votes, agree=agree,
                q=[quantile(xs, q) for q in (0.1, 0.25, 0.5, 0.75, 0.9)])


def important(m, med, mw):
    """Shift that changes the target: >= 1 dB for levels, >= 20 % for durations."""
    if mw is None:
        return False
    med = float(med)
    if m == "dur_s":
        return abs(mw - med) >= 0.2 * max(abs(med), 1e-9)
    return abs(mw - med) >= 1.0


def close(a, b):
    a, b = fnum(a), fnum(b)
    if a is None or b is None:
        return a is None and b is None
    return abs(a - b) <= max(0.0015 * abs(b), 0.0011)


def main():
    verif = load_verif()
    rows = family_rows()
    groups = {}
    for fam, r in rows:
        groups.setdefault((fam, "tous"), []).append(r)
        groups.setdefault((fam, r["style"]), []).append(r)
    with open(TSV, newline="") as fh:
        tsv = list(csv.DictReader(fh, delimiter="\t"))
    gaps, solid, defin = [], [], 0
    seen = set()
    for t in tsv:
        key = (t["famille"], t["style"])
        items = groups.get(key, [])
        m = t["mesure"]
        seen.add((key, m))
        if m == "tempo du fabricant":
            d = tempo_line(items, 2.0)
            if str(d["n"]) != t["sons"]:
                gaps.append((key, m, "sons", t["sons"], d["n"]))
            acc = "%d/%d" % d["acc"]
            if acc != t["accord"]:
                gaps.append((key, m, "accord", t["accord"], acc))
            for name, qv in zip(("p10", "p25", "mediane", "p75", "p90"), d["q"]):
                if not close(t[name], qv):
                    gaps.append((key, m, name, t[name], qv))
            v = ", ".join("%s %g (%d)" % (p, vv[0], vv[1]) for p, vv in sorted(d["votes"].items()))
            if "votes " + v not in t["raison"]:
                gaps.append((key, m, "votes", t["raison"].split("votes ")[-1], v))
            sure = t["statut"] == "tempo du fabricant sûr"
            if sure != d["agree"] and not (sure is False and d["agree"]):
                gaps.append((key, m, "statut", t["statut"], d["agree"]))
            if sure and not d["agree"]:
                gaps.append((key, m, "statut_sans_regle", t["statut"], d["agree"]))
            if (not sure) and d["agree"]:
                gaps.append((key, m, "refus_plus_strict_que_regle", t["statut"], "2 packs a 2 %"))
            continue
        d = stats_line(items, m, verif)
        exp = dict(sons=d["n"], packs=d["packs"], packs_5=d["packs5"], part_pack_max=d["part"],
                   liste_packs=d["liste"])
        for k, v in exp.items():
            if str(v) != t[k]:
                gaps.append((key, m, k, t[k], v))
        for name, qv in zip(("p10", "p25", "mediane", "p75", "p90"), d["q"]):
            if not close(t[name], qv):
                gaps.append((key, m, name, t[name], qv))
        acc = "" if d["acc"] is None else "%d/%d" % d["acc"]
        if acc != t["accord"]:
            gaps.append((key, m, "accord", t["accord"], acc))
        if d["statut"] != t["statut"]:
            gaps.append((key, m, "statut", t["statut"], d["statut"]))
        if t["statut"] == "définitive":
            defin += 1
            p25, p75 = fnum(t["p25"]), fnum(t["p75"])
            mw = d["med_wo"]
            inside = mw is not None and p25 - 1e-9 <= mw <= p75 + 1e-9
            solid.append((key, m, d["bigpack"], d["part"], t["mediane"], mw, t["p25"], t["p75"], inside))
    # lines that would exist but are missing from the tsv
    for (fam, sty), items in groups.items():
        for m in measures(fam) + (["tempo du fabricant"] if fam == "boucle de batterie" else []):
            if ((fam, sty), m) not in seen:
                gaps.append(((fam, sty), m, "ligne_absente", "", len(items)))
    with open(OUT, "w", newline="") as fh:
        w = csv.writer(fh, delimiter="\t", lineterminator="\n")
        w.writerow(["famille", "style", "mesure", "champ", "tsv", "recalcul"])
        for key, m, k, a, b in gaps:
            w.writerow([key[0], key[1], m, k, a, b])
        w.writerow([])
        w.writerow(["famille", "style", "mesure", "plus_gros_pack", "part", "mediane", "mediane_sans_lui",
                    "p25", "p75", "dans_p25_p75", "ecart_important"])
        for key, m, bp, part, med, mw, p25, p75, ins in solid:
            w.writerow([key[0], key[1], m, bp, part, med, "%.4g" % mw if mw is not None else "",
                        p25, p75, "oui" if ins else "NON",
                        "oui" if (not ins and important(m, med, mw)) else ""])
    print("lignes tsv:", len(tsv), "définitives:", defin, "écarts:", len(gaps))
    for g in gaps:
        print("ECART", g)
    n_out = n_imp = 0
    for s in solid:
        if not s[-1]:
            n_out += 1
            imp = important(s[1], s[4], s[5])
            n_imp += imp
            print("UN SEUL PACK", "IMPORTANT" if imp else "negligeable", s)
    print("hors p25-p75:", n_out, "dont importants:", n_imp)


def b1():
    """Replay of point 5: 40 of the 622 moved voices, fixed seed."""
    path = os.path.join(ROOT, "docs", "sons", "passe2", "fin", "reetiquetage", "n7_vocal.csv")
    with open(path, newline="") as fh:
        moved = {"r%s-%s-%s" % (r["rar"], r["pack"], r["file_id"]): r for r in csv.DictReader(fh)}
    random.seed(20261127)
    keys = random.sample(sorted(moved), 40)
    fams = {}
    for fam, r in family_rows():
        fams.setdefault(r["cle"], []).append(fam)
    vp = [float(moved[k]["voiced_pct"]) for k in keys]
    rs = [float(moved[k]["range_st"]) for k in keys]
    print("moved:", len(moved), "sample:", " ".join(keys))
    print("families of sample:", sorted({f for k in keys for f in fams.get(k, ["absent"])}))
    print("voiced_pct median %.1f min %.1f ; range_st median %.2f" % (
        statistics.median(vp), min(vp), statistics.median(rs)))


def selftest():
    assert quantile([1, 2, 3, 4], 0.5) == 2.5
    assert quantile([1, 2, 3, 4, 5], 0.25) == 2
    assert abs(quantile([0, 10], 0.1) - 1.0) < 1e-12
    assert close("0.8571", 0.85714) and not close("1.0", 1.1)
    rows = [dict(rar="1", pack="p1", cle="a%d" % i, dur_s="1") for i in range(18)]
    rows += [dict(rar="2", pack="p1", cle="b%d" % i, dur_s="3") for i in range(4)]
    v = {r["cle"]: {"dur_s_ok": "yes"} for r in rows}
    d = stats_line(rows, "dur_s", v)
    assert d["n"] == 22 and d["packs5"] == 1 and d["part"] == 82
    assert d["statut"] != "définitive" and d["med_wo"] == 3
    rows += [dict(rar="2", pack="p1", cle="c", dur_s="3")]
    v["c"] = {"dur_s_ok": "no"}
    d = stats_line(rows, "dur_s", v)
    assert d["packs5"] == 2 and d["acc"] == (22, 23) and d["statut"] != "définitive"
    v["c"] = {"dur_s_ok": "yes"}
    assert stats_line(rows, "dur_s", v)["statut"] == "définitive"
    lo = [dict(rar="1", pack="p1", bpm_tag="140", bpm_gap_folded_pct="0") for _ in range(5)]
    lo += [dict(rar="2", pack="p1", bpm_tag="142", bpm_gap_folded_pct="1") for _ in range(5)]
    assert tempo_line(lo, 2.0)["agree"]
    lo[5:] = [dict(rar="2", pack="p1", bpm_tag="150", bpm_gap_folded_pct="1") for _ in range(5)]
    assert not tempo_line(lo, 2.0)["agree"]
    assert important("dur_s", "1.0", 1.3) and not important("dur_s", "1.0", 1.1)
    assert important("rms_dbfs", "-10", -11.5) and not important("peak_dbfs", "-0.1", 0.0)
    random.seed(1)
    print("selftest ok")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    elif "--b1" in sys.argv:
        b1()
    else:
        main()
