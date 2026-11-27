#!/usr/bin/env python3
"""Pick one original pack sound per line of the private listening page, lot "basses".

  python3 -I scripts/sons/fin/page/choix_basses.py             # writes docs/sons/fin/page/originaux-basses.csv
  python3 -I scripts/sons/fin/page/choix_basses.py --selftest  # fabricated tables only
  python3 -I scripts/sons/fin/page/choix_basses.py --besoin    # file ids to fetch, per archive (stdout)
  python3 -I scripts/sons/fin/page/choix_basses.py --trouve LISTE --rar N
        # LISTE = `unrar lb` of archive N (outside the repo): prints "cle<TAB>path inside the archive"
        # for every candidate of that archive, never written to the repository.

Reads only CSV (the tables cibles_autres.py reads, and its output TSV), never a sound.
Candidates of a line = sounds of its family and style ("tous" = all styles), minus those the blind
counter-measure does not confirm on dur_s, rms_dbfs AND peak_dbfs (all three "yes"), minus those whose
remeasure on the extracted file failed (REMESURE below, "non").
Measures aimed at: the "définitive" measures of the line in cibles-autres-definitives.tsv; for an
"exemple" line (no target), dur_s, rms_dbfs and peak_dbfs.
Distance = mean over these measures of |value - median| / max(p75 - p25, floor),
floor = 0.5 dB, or 5 % of the median for dur_s.
Pick = smallest distance; within 0.1 of it, the pack of the smallest archive (less to download).
"""
import argparse
import csv
import hashlib
import os
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "scripts", "sons", "fin"))
import cibles_autres as ca  # noqa: E402

TARGETS = os.path.join(ROOT, "docs/sons/fin/cibles-autres-definitives.tsv")
OUT = os.path.join(ROOT, "docs/sons/fin/page/originaux-basses.csv")
DEF, EX = "définitive", "exemple"
LEVEL = ["dur_s", "rms_dbfs", "peak_dbfs"]
FLOOR_DB, FLOOR_DUR, TIE = 0.5, 0.05, 0.1
# archive sizes in MB (HTTP Content-Range, 2026-11-27), for the tie-break only
ARCHIVE_MB = {1: 600, 2: 711, 3: 259, 4: 418, 5: 331, 6: 558, 7: 2538, 8: 1092, 9: 1781,
              10: 1351, 11: 2369, 12: 573}
# (slug, family, style, status)
LINES = [("basse_tous", "basse (coups)", "tous", DEF), ("basse_dnb", "basse (coups)", "dnb", DEF),
         ("basse_dubstep", "basse (coups)", "dubstep", DEF), ("basse_house", "basse (coups)", "house", DEF),
         ("basse_techno", "basse (coups)", "techno", DEF), ("basse_trap", "basse (coups)", "trap", DEF),
         ("pad_tous", "coup mélodique : pad", "tous", DEF),
         ("coupdivers_tous", "coup mélodique : -", "tous", DEF)]
LINES += [("%s_tous" % r, "coup mélodique : %s" % r, "tous", EX)
          for r in ("arp", "strings", "bell", "pluck", "guitar", "brass")]
# remeasure of the extracted file against the table (mesure.py + compare.agree): cle -> "oui" / "non"
REMESURE = {
    "r10-p1-806926203a": "oui",
    "r10-p2-4742cddabd": "oui",
    "r11-p1-e14187e0f9": "oui",
    "r11-p1-f3cff6d1d1": "oui",
    "r12-p2-3ef91a1e90": "oui",
    "r2-p1-0fa4f9ba8c": "oui",
    "r5-p1-420bfc4ac0": "oui",
    "r8-p1-4e1c7e62a9": "oui",
    "r8-p1-b20ffa9fea": "oui",
    "r9-p1-2c772fbaa0": "oui",
    "r9-p2-362f4ff4f2": "oui",
    "r9-p2-7fcc5be6ac": "oui",
    "r9-p2-b361b88c37": "oui",
    "r9-p2-fc61b5ead8": "oui",
}
COLS = ["slug", "famille", "style", "statut", "cle", "pack", "dur_s", "rms_dbfs", "peak_dbfs",
        "mediane_dur_s", "mediane_rms_dbfs", "mediane_peak_dbfs", "mesures", "distance", "candidats",
        "remesure_ok"]


def targets(path):
    """(famille, style, mesure) -> (p25, median, p75, statut)."""
    out = {}
    with open(path, newline="") as fh:
        for r in csv.DictReader(fh, delimiter="\t"):
            out[(r["famille"], r["style"], r["mesure"])] = (
                ca.fnum(r["p25"]), ca.fnum(r["mediane"]), ca.fnum(r["p75"]), r["statut"])
    return out


def aims(tg, fam, style, status):
    ms = [m for m in LEVEL if tg.get((fam, style, m), (0, 0, 0, ""))[3] == ca.DEF] if status == DEF else LEVEL
    return [(m,) + tg[(fam, style, m)][:3] for m in ms if (fam, style, m) in tg]


def confirmed(row, vidx):
    v = vidx.get(row.get("cle"), {})
    return all(v.get(m + "_ok") == "yes" for m in LEVEL)


def distance(row, aim):
    parts = []
    for m, p25, med, p75 in aim:
        x = ca.fnum(row.get(m))
        if x is None or med is None:
            return None
        floor = FLOOR_DUR * abs(med) if m == "dur_s" else FLOOR_DB
        parts.append(abs(x - med) / max(p75 - p25, floor))
    return sum(parts) / len(parts) if parts else None


def ranked(cands):
    """cands = [(distance, row)] -> rows in pick order (the rule applied again on what is left)."""
    left = sorted(cands, key=lambda c: (c[0], c[1]["cle"]))
    out = []
    while left:
        best = left[0][0]
        near = [c for c in left if c[0] <= best + TIE + 1e-12]
        pick = min(near, key=lambda c: (ARCHIVE_MB.get(int(c[1]["rar"]), 10 ** 9), c[0], c[1]["cle"]))
        out.append(pick)
        left.remove(pick)
    return out


def build(tables_dir, verif_dir, targets_path, remesure):
    t = {}
    for name in ("oneshot_melodic",):
        t[name] = ca.read(os.path.join(tables_dir, name + ".csv"))
    fams = {f: rows for f, rows, _ in ca.families(t)}
    vidx = ca.verif_index(verif_dir)
    tg = targets(targets_path)
    out, lists = [], {}
    for slug, fam, style, status in LINES:
        rows = [r for r in fams.get(fam, []) if not r.get("error") and (style == "tous" or r.get("style") == style)]
        aim = aims(tg, fam, style, status)
        cands = []
        for r in rows:
            if not confirmed(r, vidx) or remesure.get(r["cle"]) == "non":
                continue
            d = distance(r, aim)
            if d is not None:
                cands.append((d, r))
        order = ranked(cands) if aim else []
        lists[slug] = [r for _, r in order]
        med = {m: md for m, _, md, _ in aim}
        line = dict.fromkeys(COLS, "")
        line.update(slug=slug, famille=fam, style=style, statut=status, mesures=" ".join(m for m, *_ in aim),
                    candidats=len(order))
        for m in LEVEL:
            line["mediane_" + m] = "" if m not in med else "%.4g" % med[m]
        if order:
            d, r = order[0]
            line.update(cle=r["cle"], pack=ca.pack_of(r), dur_s=r["dur_s"], rms_dbfs=r["rms_dbfs"],
                        peak_dbfs=r["peak_dbfs"], distance="%.3f" % d, remesure_ok=remesure.get(r["cle"], ""))
        out.append(line)
    return out, lists


def write(rows, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=COLS, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)


def fid(folder_name, rel):
    return hashlib.sha1((folder_name + "/" + rel).encode("utf-8", "surrogateescape")).hexdigest()[:10]


def find(listing, wanted):
    """listing = archive paths; wanted = {(pack, file_id): cle}. Each file is hashed against every folder
    above it (as compare.find_pack_dirs); a pack is pinned to the folder that matches most of its ids."""
    hits = {}
    for path in listing:
        parts = path.replace("\\", "/").strip("/").split("/")
        for k in range(1, len(parts)):
            i = fid(parts[k - 1], "/".join(parts[k:]))
            for (pk, f), cle in wanted.items():
                if f == i:
                    hits.setdefault((pk, "/".join(parts[:k])), {})[cle] = path
    best = {}
    for (pk, folder), m in hits.items():
        if len(m) > len(best.get(pk, {})):
            best[pk] = m
    return {cle: p for m in best.values() for cle, p in m.items()}


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
        om = []
        # bass dnb: sizes rar 7 (big) and 3 (small). b0 exact (rar 7), b1 at 0.05 (rar 3) -> b1 wins the tie;
        # b2 exact but counter-measure "no" -> out; b3 exact, error row -> out.
        spec = [("b0", "7", "1.0", "-10", "-0.1", ""), ("b1", "3", "1.0", "-10.05", "-0.1", ""),
                ("b2", "1", "1.0", "-10", "-0.1", ""), ("b3", "1", "1.0", "-10", "-0.1", "bad"),
                ("b4", "9", "3.0", "-14", "-3", "")]
        for cle, rar, dur, rms, pk, err in spec:
            om.append({"rar": rar, "pack": "p1", "cle": cle, "role": "bass", "style": "dnb", "error": err,
                       "dur_s": dur, "rms_dbfs": rms, "peak_dbfs": pk, "file_id": "f" + cle})
        # arp (example line): x0 far, x1 close, x2 closest but remeasure failed -> x1
        for cle, dur in (("x0", "9"), ("x1", "2.1"), ("x2", "2.0")):
            om.append({"rar": "5", "pack": "p2", "cle": cle, "role": "arp", "style": "trap", "error": "",
                       "dur_s": dur, "rms_dbfs": "-20", "peak_dbfs": "-1", "file_id": "f" + cle})
        with open(os.path.join(tdir, "oneshot_melodic.csv"), "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=list(om[0]))
            w.writeheader()
            w.writerows(om)
        with open(os.path.join(vdir, "rar1.csv"), "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=["cle", "dur_s_ok", "rms_dbfs_ok", "peak_dbfs_ok"])
            w.writeheader()
            for r in om:
                w.writerow({"cle": r["cle"], "dur_s_ok": "yes", "rms_dbfs_ok": "no" if r["cle"] == "b2" else "yes",
                            "peak_dbfs_ok": "yes"})
        tp = os.path.join(d, "t.tsv")
        with open(tp, "w", newline="") as fh:
            w = csv.writer(fh, delimiter="\t")
            w.writerow(["famille", "style", "mesure", "p25", "mediane", "p75", "statut"])
            w.writerow(["basse (coups)", "dnb", "dur_s", "0.9", "1", "1.1", "pas de cible, on garde le son actuel"])
            w.writerow(["basse (coups)", "dnb", "rms_dbfs", "-11", "-10", "-9", ca.DEF])
            w.writerow(["basse (coups)", "dnb", "peak_dbfs", "-0.1", "-0.1", "-0.1", ca.DEF])
            for m, a, b, c in (("dur_s", 1, 2, 3), ("rms_dbfs", -22, -20, -18), ("peak_dbfs", -1, -1, -1)):
                w.writerow(["coup mélodique : arp", "tous", m, a, b, c, "pas de cible, on garde le son actuel"])
        rows, lists = build(tdir, vdir, tp, {"x2": "non"})
        g = {r["slug"]: r for r in rows}
        b = g["basse_dnb"]
        check("dnb aims only the définitive measures", b["mesures"], "rms_dbfs peak_dbfs")
        check("dnb duration median not written", b["mediane_dur_s"], "")
        check("dnb tie within 0.1 -> smallest archive", b["cle"], "b1")
        check("dnb pack written rNpN", b["pack"], "r3p1")
        check("dnb distance (0.05/2 + 0/0.5)/2", b["distance"], "0.013")
        check("dnb disagreeing counter-measure and error left out", [r["cle"] for r in lists["basse_dnb"]],
              ["b1", "b0", "b4"])
        a = g["arp_tous"]
        check("example aims the three level measures", a["mesures"], "dur_s rms_dbfs peak_dbfs")
        check("failed remeasure skipped -> next", a["cle"], "x1")
        check("floor 0.5 dB when p75 = p25 (peak)", round(distance({"dur_s": "1", "rms_dbfs": "-10", "peak_dbfs": "-0.6"},
              [("peak_dbfs", -0.1, -0.1, -0.1)]), 3), 1.0)
        check("floor 5 % of the median for dur_s", round(distance({"dur_s": "2.1"}, [("dur_s", 2, 2, 2)]), 3), 1.0)
        check("line without target or sound stays empty", g["basse_house"]["cle"], "")
        lst = ["W/Pk/Bass/a.wav", "W/Pk/Bass/b.wav", "W/Other/c.wav"]
        want = {("p1", fid("Pk", "Bass/a.wav")): "k1", ("p1", fid("Other", "c.wav")): "k3"}
        check("find pins a pack to its best folder", find(lst, want), {"k1": "W/Pk/Bass/a.wav"})
        check("no real file name in columns", any("name" in c or "path" in c for c in COLS), False)
    print("%d failed" % len(fails))
    return 1 if fails else 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--selftest", action="store_true")
    ap.add_argument("--besoin", action="store_true")
    ap.add_argument("--trouve", metavar="LISTE")
    ap.add_argument("--rar", type=int)
    ap.add_argument("--tables", default=ca.TABLES)
    ap.add_argument("--verif", default=ca.VERIF)
    ap.add_argument("--out", default=OUT)
    a = ap.parse_args(argv)
    if a.selftest:
        return selftest()
    rows, lists = build(a.tables, a.verif, TARGETS, REMESURE)
    if a.besoin:
        for r in rows:
            print(r["slug"], r["pack"], r["cle"], r["distance"], "remesure=" + (r["remesure_ok"] or "?"))
        return 0
    if a.trouve:
        with open(a.trouve, encoding="utf-8", errors="surrogateescape") as fh:
            listing = [ln.rstrip("\n") for ln in fh if ln.strip()]
        want = {(r["pack"], r["file_id"]): r["cle"] for rs in lists.values() for r in rs[:5]
                if int(r["rar"]) == a.rar}
        for cle, p in sorted(find(listing, want).items()):
            print("%s\t%s" % (cle, p))
        return 0
    write(rows, a.out)
    print("ok: %d lines, %d with a sound -> %s" % (len(rows), sum(bool(r["cle"]) for r in rows),
                                                   os.path.relpath(a.out, ROOT)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
