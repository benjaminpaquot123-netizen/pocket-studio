#!/usr/bin/env python3
"""Pick, for each loop line of the private listening page, the pack sound closest to the final targets.

  python3 -I scripts/sons/fin/page/choix_boucles.py            # writes docs/sons/fin/page/originaux-boucles.csv
  python3 -I scripts/sons/fin/page/choix_boucles.py --list 5   # also prints the 5 best candidates per line
  python3 -I scripts/sons/fin/page/choix_boucles.py --selftest # fabricated tables only

Reads only CSV (the passe-2 tables and the blind counter-measure, through cibles_autres.py, and
docs/sons/fin/cibles-autres-definitives.tsv), never a sound. Never writes a file or folder name.

Candidates of a line = the sounds of its family and style ("tous" = every style), without an error,
whose counter-measure agrees on dur_s, rms_dbfs and peak_dbfs. For dnb, dubstep and house, only the
loops whose maker tempo (bpm_tag) is the style's sure tempo within 2 %.
Targets = the medians of the line's "définitive" level measures. Distance = mean over these measures of
|value - median| / max(p75 - p25, floor); floor = 0.5 dB, or 5 % of the median for dur_s.
Pick = smallest distance; within TIE of the best, the sound whose archive is smallest (less to download).
A sound listed in NOT_FOUND (missing from its archive, or re-measured differently) is skipped: the next
one is taken. REMEASURED holds the keys whose extracted file was re-measured equal to the table.
"""
import argparse
import csv
import os
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import cibles_autres as ca  # noqa: E402

ROOT = ca.ROOT
TARGETS = os.path.join(ROOT, "docs/sons/fin/cibles-autres-definitives.tsv")
OUT = os.path.join(ROOT, "docs/sons/fin/page/originaux-boucles.csv")
LEVEL = ["dur_s", "rms_dbfs", "peak_dbfs"]
FLOOR_DB, FLOOR_DUR, TIE, TEMPO_TOL = 0.5, 0.05, 0.1, 0.02
SURE_TEMPO = {"dnb": 174.0, "dubstep": 140.0, "house": 125.0}
# archive sizes in MB (download size of each RAR), for the tie-break only
ARCHIVE_MB = {"1": 600, "2": 711, "3": 259, "4": 418, "5": 331, "6": 558, "7": 2538, "8": 1092,
              "9": 1781, "10": 1351, "11": 2369, "12": 573}
LINES = [("bouclemelodique_tous", "boucle mélodique", "melodic_loop", "tous"),
         ("bouclemelodique_dnb", "boucle mélodique", "melodic_loop", "dnb"),
         ("bouclemelodique_dubstep", "boucle mélodique", "melodic_loop", "dubstep"),
         ("bouclemelodique_techno", "boucle mélodique", "melodic_loop", "techno"),
         ("bouclebatterie_tous", "boucle de batterie", "drum_loop", "tous"),
         ("bouclebatterie_dnb", "boucle de batterie", "drum_loop", "dnb"),
         ("bouclebatterie_dubstep", "boucle de batterie", "drum_loop", "dubstep"),
         ("bouclebatterie_house", "boucle de batterie", "drum_loop", "house"),
         ("bouclebatterie_techno", "boucle de batterie", "drum_loop", "techno")]
NOT_FOUND = set()  # keys skipped after extraction (none so far)
REMEASURED = {  # keys re-measured equal to the table after extraction
    "r5-p1-07ddad181f", "r3-p1-4f400f88a0", "r5-p1-25d2443f13", "r5-p1-079dfe0c92",
    "r10-p1-0c67caebad", "r9-p3-a90b72fc2a", "r1-p1-7515055228", "r1-p1-fe88e65085",
    "r12-p2-4e836e41f3",
}
COLS = ["slug", "famille", "style", "statut", "cle", "pack", "tempo_fabricant", "dur_s", "rms_dbfs",
        "peak_dbfs", "cible_dur_s", "cible_rms_dbfs", "cible_peak_dbfs", "distance", "remesure_ok"]


def read_targets(path):
    """(famille, style) -> {measure: (median, p25, p75)} for the 'définitive' level measures."""
    out = {}
    with open(path, newline="") as fh:
        for r in csv.DictReader(fh, delimiter="\t"):
            if r["statut"] == ca.DEF and r["mesure"] in LEVEL:
                out.setdefault((r["famille"], r["style"]), {})[r["mesure"]] = tuple(
                    float(r[c]) for c in ("mediane", "p25", "p75"))
    return out


def agreed(r, vidx):
    v = vidx.get(r.get("cle"), {})
    return all(v.get(c + "_ok") == "yes" for c in LEVEL)


def candidates(rows, style, vidx):
    out = []
    for r in rows:
        if r.get("error") or not agreed(r, vidx):
            continue
        if style != "tous" and r.get("style") != style:
            continue
        if style in SURE_TEMPO:
            t = ca.fnum(r.get("bpm_tag"))
            if t is None or abs(t - SURE_TEMPO[style]) > TEMPO_TOL * SURE_TEMPO[style]:
                continue
        if any(ca.fnum(r.get(c)) is None for c in LEVEL):
            continue
        out.append(r)
    return out


def distance(r, tgt):
    ds = []
    for m, (med, p25, p75) in sorted(tgt.items()):
        floor = FLOOR_DUR * abs(med) if m == "dur_s" else FLOOR_DB
        ds.append(abs(ca.fnum(r[m]) - med) / max(p75 - p25, floor))
    return sum(ds) / len(ds)


def ranked(cands, tgt):
    """Best first: distance order, but within TIE of the best the smallest archive comes first."""
    scored = sorted(((distance(r, tgt), r) for r in cands if r["cle"] not in NOT_FOUND),
                    key=lambda x: (x[0], x[1]["cle"]))
    if not scored:
        return []
    best = scored[0][0]
    near = [x for x in scored if x[0] <= best + TIE + 1e-12]
    near.sort(key=lambda x: (ARCHIVE_MB.get(x[1]["rar"], 10 ** 6), x[0], x[1]["cle"]))
    return near + scored[len(near):]


def build(tables_dir, verif_dir, targets_path):
    t = {k: ca.read(os.path.join(tables_dir, k + ".csv")) for k in ("melodic_loop", "drum_loop")}
    vidx = ca.verif_index(verif_dir)
    targets = read_targets(targets_path)
    out, lists = [], {}
    for slug, fam, table, style in LINES:
        tgt = targets.get((fam, style), {})
        rank = ranked(candidates(t[table], style, vidx), tgt) if tgt else []
        lists[slug] = rank
        row = dict.fromkeys(COLS, "")
        row.update(slug=slug, famille=fam, style=style, statut=ca.DEF if tgt else "pas de cible définitive")
        for m in LEVEL:
            if m in tgt:
                row["cible_" + m] = "%.4g" % tgt[m][0]
        if rank:
            d, r = rank[0]
            row.update(cle=r["cle"], pack=ca.pack_of(r), tempo_fabricant=r.get("bpm_tag") or "",
                       distance="%.3f" % d, remesure_ok="oui" if r["cle"] in REMEASURED else "à faire")
            for m in LEVEL:
                row[m] = "%.4g" % ca.fnum(r[m])
        out.append(row)
    return out, lists


def write(rows, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=COLS, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)


def selftest():
    fails = []

    def check(name, got, want):
        ok = got == want
        print("%s %-55s got %r want %r" % ("ok  " if ok else "FAIL", name, got, want))
        if not ok:
            fails.append(name)

    with tempfile.TemporaryDirectory() as d:
        tdir, vdir = os.path.join(d, "t"), os.path.join(d, "v")
        os.makedirs(tdir)
        os.makedirs(vdir)

        def wr(path, rows, delim=","):
            with open(path, "w", newline="") as fh:
                w = csv.DictWriter(fh, fieldnames=list(rows[0]), delimiter=delim)
                w.writeheader()
                w.writerows(rows)

        def loop(cle, rar, style, dur, rms, peak, bpm="", err=""):
            return {"rar": rar, "pack": "p1", "cle": cle, "style": style, "error": err, "bpm_tag": bpm,
                    "dur_s": dur, "rms_dbfs": rms, "peak_dbfs": peak}

        drum = [loop("a", "9", "dnb", "5.5", "-15", "-0.1", "174"),      # exact, big archive
                loop("b", "3", "dnb", "5.5", "-15.2", "-0.1", "174"),    # 0.04 away, small archive -> wins tie
                loop("c", "3", "dnb", "5.5", "-15", "-0.1", "160"),      # wrong maker tempo
                loop("e", "3", "dnb", "5.5", "-15", "-0.1", "174", "bad"),  # error row
                loop("f", "3", "dnb", "5.5", "-15", "-0.1", "174"),      # counter-measure disagrees
                loop("g", "1", "dnb", "5.5", "-25", "-0.1", "174"),      # far away
                loop("h", "9", "dnb", "5.5", "-15", "-0.1", "177"),      # 1.7 % off: kept
                loop("t1", "3", "techno", "6.4", "-14", "-0.1", "150"),
                loop("t2", "3", "techno", "6.4", "-14", "-3", "128")]
        wr(os.path.join(tdir, "drum_loop.csv"), drum)
        wr(os.path.join(tdir, "melodic_loop.csv"), [loop("m1", "2", "dnb", "11", "-13", "-0.1", "174")])
        ver = [{"cle": r["cle"], "dur_s_ok": "yes", "rms_dbfs_ok": "no" if r["cle"] == "f" else "yes",
                "peak_dbfs_ok": "yes"} for r in drum]
        wr(os.path.join(vdir, "rar1.csv"), ver)
        tgt = [{"famille": "boucle de batterie", "style": "dnb", "mesure": "dur_s", "mediane": "5.5", "p25": "5.5",
                "p75": "5.5", "statut": ca.DEF},
               {"famille": "boucle de batterie", "style": "dnb", "mesure": "rms_dbfs", "mediane": "-15", "p25": "-20",
                "p75": "-10", "statut": ca.DEF},
               {"famille": "boucle de batterie", "style": "dnb", "mesure": "tempo du fabricant", "mediane": "174",
                "p25": "174", "p75": "174", "statut": ca.TEMPO_SURE},
               {"famille": "boucle de batterie", "style": "techno", "mesure": "peak_dbfs", "mediane": "-0.1",
                "p25": "-0.9", "p75": "-0.1", "statut": ca.DEF},
               {"famille": "boucle de batterie", "style": "techno", "mesure": "rms_dbfs", "mediane": "-14",
                "p25": "-17", "p75": "-9", "statut": ca.KEEP}]
        tp = os.path.join(d, "tg.tsv")
        wr(tp, tgt, "\t")
        rows, lists = build(tdir, vdir, tp)
        get = {r["slug"]: r for r in rows}
        dn = get["bouclebatterie_dnb"]
        keys = [r["cle"] for _, r in lists["bouclebatterie_dnb"]]
        check("wrong tempo, error, disagreement dropped", sorted(keys), ["a", "b", "g", "h"])
        check("smallest archive wins inside the tie", dn["cle"], "b")
        check("distance written", dn["distance"], "0.010")
        check("pack written rNpN", dn["pack"], "r3p1")
        check("maker tempo written", dn["tempo_fabricant"], "174")
        check("non-definitive target left empty", dn["cible_peak_dbfs"], "")
        check("definitive target written", dn["cible_rms_dbfs"], "-15")
        check("far sound last", keys[-1], "g")
        check("not re-measured yet", dn["remesure_ok"], "à faire")
        tc = get["bouclebatterie_techno"]
        check("techno: only the definitive peak counts", tc["cle"], "t1")
        check("techno keeps the maker tempo", tc["tempo_fabricant"], "150")
        check("line without definitive target has no sound", get["bouclemelodique_dnb"]["cle"], "")
        check("all nine lines written", len(rows), 9)
        NOT_FOUND.add("b")
        try:
            rows2, _ = build(tdir, vdir, tp)
        finally:
            NOT_FOUND.discard("b")
        check("not found -> next one", {r["slug"]: r for r in rows2}["bouclebatterie_dnb"]["cle"], "a")
        REMEASURED.add("b")
        try:
            rows3, _ = build(tdir, vdir, tp)
        finally:
            REMEASURED.discard("b")
        check("re-measured -> oui", {r["slug"]: r for r in rows3}["bouclebatterie_dnb"]["remesure_ok"], "oui")
        check("no file name column", any(c in COLS for c in ("file_id", "path", "nom")), False)
    print("%d failed" % len(fails))
    return 1 if fails else 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--selftest", action="store_true")
    ap.add_argument("--list", type=int, default=0)
    ap.add_argument("--tables", default=ca.TABLES)
    ap.add_argument("--verif", default=ca.VERIF)
    ap.add_argument("--targets", default=TARGETS)
    ap.add_argument("--out", default=OUT)
    a = ap.parse_args(argv)
    if a.selftest:
        return selftest()
    rows, lists = build(a.tables, a.verif, a.targets)
    write(rows, a.out)
    for slug, rank in lists.items():
        for d, r in rank[:a.list]:
            print("%-26s %-20s %-6s %6s %.3f" % (slug, r["cle"], ca.pack_of(r), r.get("bpm_tag"), d))
    print("ok: %d lines, %d with a sound -> %s" % (len(rows), sum(bool(r["cle"]) for r in rows),
                                                  os.path.relpath(a.out, ROOT)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
