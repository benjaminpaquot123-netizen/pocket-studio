#!/usr/bin/env python3
"""Pick one original pack sound per line of the private listening page, lot "voix et effets".

  python3 -I scripts/sons/fin/page/choix_voixfx.py            # writes docs/sons/fin/page/originaux-voixfx.csv
  python3 -I scripts/sons/fin/page/choix_voixfx.py --rang 5   # also prints the 5 best candidates per line
  python3 -I scripts/sons/fin/page/choix_voixfx.py --selftest # fabricated tables only

Reads only CSV, never a sound. Tables and counter-measure are read the same way as
scripts/sons/fin/cibles_autres.py; targets come from docs/sons/fin/cibles-autres-definitives.tsv.

Per line (family x style, "tous" = every style):
  candidates = sounds of the family and style, without an error, whose counter-measure agrees
               (dur_s_ok, rms_dbfs_ok and peak_dbfs_ok all "yes");
  measures   = the "définitive" measures of the line, weight 1; when the line has only one,
               the two other level measures are added with weight 0.5 (column poids);
  distance   = weighted mean of |value - median| / max(p75 - p25, floor),
               floor 0.5 dB for dB measures, 5 % of the median for the duration;
  pick       = smallest distance; within TIE (0.1) of it, the pack of the smallest archive wins
               (archive size estimated by the sum of size_kb of its rows in every table).
A sound whose file could not be found or did not re-measure like its table goes in EXCLUS
(key -> reason) and the next one is taken. REMESURE holds the re-measure result of each pick.
"""
import argparse
import csv
import glob
import os
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import cibles_autres as ca  # noqa: E402

OUT = os.path.join(ca.ROOT, "docs/sons/fin/page/originaux-voixfx.csv")
CIBLES = ca.OUT
TIE, FLOOR_DB, FLOOR_DUR = 0.1, 0.5, 0.05
# slug, family label in the targets, table, style
LINES = [("voix_tous", "voix", "vocal", "tous"), ("voix_dnb", "voix", "vocal", "dnb"),
         ("effet_tous", "effet", "fx", "tous"), ("effet_dnb", "effet", "fx", "dnb"),
         ("effet_dubstep", "effet", "fx", "dubstep"), ("effet_techno", "effet", "fx", "techno"),
         ("effet_rave", "effet", "fx", "rave")]
# keys set aside after download (file not found, or re-measure off the table): key -> reason
EXCLUS = {}
# re-measure of the picked sound with scripts/sons/passe2/verif/mesure.py: key -> "oui" / "non"
REMESURE = {}
COLS = ["slug", "famille", "style", "statut", "mesures_definitives", "poids", "cle", "pack",
        "dur_s", "rms_dbfs", "peak_dbfs", "cible_dur_s", "cible_rms_dbfs", "cible_peak_dbfs",
        "distance", "candidats", "exclus", "remesure_ok"]


def targets(path):
    """(famille, style) -> {mesure: row} for the level measures."""
    out = {}
    with open(path, newline="") as fh:
        for r in csv.DictReader(fh, delimiter="\t"):
            if r["mesure"] in ca.LEVEL:
                out.setdefault((r["famille"], r["style"]), {})[r["mesure"]] = r
    return out


def archive_sizes(tables_dir):
    s = {}
    for p in glob.glob(os.path.join(tables_dir, "*.csv")):
        for r in ca.read(p):
            v = ca.fnum(r.get("size_kb"))
            if r.get("rar") and v:
                s[r["rar"]] = s.get(r["rar"], 0.0) + v
    return s


def weights(tg):
    defs = [m for m in ca.LEVEL if tg.get(m, {}).get("statut") == ca.DEF]
    if not defs:
        return defs, {}
    w = {m: 1.0 for m in defs}
    if len(defs) == 1:
        for m in ca.LEVEL:
            w.setdefault(m, 0.5)
    return defs, w


def distance(row, tg, w):
    num = den = 0.0
    for m, k in w.items():
        v = ca.fnum(row.get(m))
        med, p25, p75 = (ca.fnum(tg[m][c]) for c in ("mediane", "p25", "p75"))
        if v is None or med is None:
            return None
        floor = FLOOR_DUR * abs(med) if m == "dur_s" else FLOOR_DB
        num += k * abs(v - med) / max(p75 - p25, floor)
        den += k
    return num / den


def agrees(r, vidx):
    v = vidx.get(r.get("cle"), {})
    return all(v.get(m + "_ok") == "yes" for m in ca.LEVEL)


def ranked(rows, tg, w, vidx, sizes):
    """Candidates sorted by distance, then the tie rule applied to the head."""
    c = []
    for r in rows:
        if r.get("error") or not agrees(r, vidx):
            continue
        d = distance(r, tg, w)
        if d is not None:
            c.append((round(d, 6), r))
    c.sort(key=lambda x: (x[0], x[1]["cle"]))
    return c


def pick(cands, sizes, exclus):
    left = [(d, r) for d, r in cands if r["cle"] not in exclus]
    if not left:
        return None
    best = left[0][0]
    near = [(d, r) for d, r in left if d <= best + TIE + 1e-9]
    near.sort(key=lambda x: (sizes.get(x[1]["rar"], float("inf")), x[0], x[1]["cle"]))
    return near[0]


def build(tables_dir, verif_dir, cibles, exclus=None, remesure=None, rang=0):
    exclus = EXCLUS if exclus is None else exclus
    remesure = REMESURE if remesure is None else remesure
    tabs = {t: ca.read(os.path.join(tables_dir, t + ".csv")) for t in ("vocal", "fx")}
    vidx = ca.verif_index(verif_dir)
    tgs = targets(cibles)
    sizes = archive_sizes(tables_dir)
    out = []
    for slug, fam, table, style in LINES:
        tg = tgs.get((fam, style), {})
        defs, w = weights(tg)
        rows = [r for r in tabs[table] if style == "tous" or r.get("style") == style]
        cands = ranked(rows, tg, w, vidx, sizes) if defs else []
        got = pick(cands, sizes, exclus)
        o = dict.fromkeys(COLS, "")
        o.update(slug=slug, famille=fam, style=style, statut=ca.DEF if defs else ca.KEEP,
                 mesures_definitives=" ".join(defs),
                 poids=" ".join("%s=%g" % (m, w[m]) for m in ca.LEVEL if m in w),
                 candidats=len(cands),
                 exclus=" ".join(sorted(k for k in exclus if any(r["cle"] == k for _, r in cands))))
        for m in ca.LEVEL:
            o["cible_" + m] = tg.get(m, {}).get("mediane", "")
        if got:
            d, r = got
            o.update(cle=r["cle"], pack=ca.pack_of(r), dur_s=r["dur_s"], rms_dbfs=r["rms_dbfs"],
                     peak_dbfs=r["peak_dbfs"], distance="%.3f" % d, remesure_ok=remesure.get(r["cle"], ""))
        if rang:
            print("%s: %d candidats" % (slug, len(cands)))
            for d, r in [x for x in cands if x[1]["cle"] not in exclus][:rang]:
                print("  %.3f %s %s dur %s rms %s peak %s" % (d, r["cle"], ca.pack_of(r), r["dur_s"],
                                                            r["rms_dbfs"], r["peak_dbfs"]))
        out.append(o)
    return out


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

        def snd(cle, rar, style, dur, rms, peak, size="100", err=""):
            return {"rar": rar, "pack": "p1", "cle": cle, "style": style, "error": err, "dur_s": dur,
                    "rms_dbfs": rms, "peak_dbfs": peak, "size_kb": size}
        fx = [snd("a", "1", "dnb", "10", "-17", "-0.1", "900000"),   # exact, big archive
              snd("b", "2", "dnb", "10.2", "-17", "-0.1", "10"),     # 0.05 further, small archive
              snd("c", "2", "dnb", "10", "-17", "-0.1"),             # exact but counter-measure off
              snd("e", "2", "dnb", "10", "-17", "-0.1", err="bad"),  # error row
              snd("f", "3", "dnb", "4", "-30", "-6")]                # far
        voc = [snd("v1", "1", "dnb", "1", "-15", "-0.1"), snd("v2", "1", "dnb", "11", "-30", "-0.11"),
               snd("v3", "1", "dnb", "11", "-15", "-3")]
        wr(os.path.join(tdir, "fx.csv"), fx)
        wr(os.path.join(tdir, "vocal.csv"), voc)
        ver = [{"cle": k, "dur_s_ok": "yes", "rms_dbfs_ok": "yes", "peak_dbfs_ok": "no" if k == "c" else "yes"}
               for k in ("a", "b", "c", "e", "f", "v1", "v2", "v3")]
        wr(os.path.join(vdir, "rar1.csv"), ver)
        tg = []
        for fam, st, defs in (("effet", "dnb", ca.LEVEL), ("voix", "dnb", ["peak_dbfs"]),
                              ("effet", "tous", ca.LEVEL), ("voix", "tous", [])):
            for m, (p25, med, p75) in zip(ca.LEVEL, ((8, 10, 12), (-19, -17, -15), (-0.1, -0.1, -0.1))):
                tg.append({"famille": fam, "style": st, "mesure": m, "p25": p25, "mediane": med, "p75": p75,
                           "statut": ca.DEF if m in defs else ca.KEEP})
        cib = os.path.join(d, "c.tsv")
        wr(cib, tg, "\t")
        res = {r["slug"]: r for r in build(tdir, vdir, cib, exclus={}, remesure={"b": "oui"})}
        r = res["effet_dnb"]
        check("tie within 0.1 -> smallest archive", r["cle"], "b")
        check("distance written (0.2 / 4 / 3)", r["distance"], "0.017")
        check("counter-measure off and error rows dropped", r["candidats"], 3)
        check("three definitive measures, weight 1", r["poids"], "dur_s=1 rms_dbfs=1 peak_dbfs=1")
        check("pack written rNpN", r["pack"], "r2p1")
        check("remesure carried", r["remesure_ok"], "oui")
        v = res["voix_dnb"]
        check("one definitive: two others at half weight", v["poids"], "dur_s=0.5 rms_dbfs=0.5 peak_dbfs=1")
        check("peak floor 0.5 dB and halves break the tie", v["cle"], "v1")
        check("line without a definitive measure has no sound", res["voix_tous"]["cle"], "")
        check("line without a definitive measure: status", res["voix_tous"]["statut"], ca.KEEP)
        r2 = {r["slug"]: r for r in build(tdir, vdir, cib, exclus={"b": "x", "a": "y"}, remesure={})}["effet_dnb"]
        check("excluded keys skipped, next one taken", r2["cle"], "f")
        check("excluded keys listed", r2["exclus"], "a b")
        check("duration floor 5 % of the median", round(distance(snd("z", "1", "x", "10.5", "-17", "-0.1"),
              {m: {"mediane": "10", "p25": "10", "p75": "10"} for m in ["dur_s"]}, {"dur_s": 1.0}), 3), 1.0)
        check("no file or folder name column", any("file" in c or "nom" in c for c in COLS), False)
    print("%d failed" % len(fails))
    return 1 if fails else 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--selftest", action="store_true")
    ap.add_argument("--rang", type=int, default=0)
    ap.add_argument("--out", default=OUT)
    a = ap.parse_args(argv)
    if a.selftest:
        return selftest()
    rows = build(ca.TABLES, ca.VERIF, CIBLES, rang=a.rang)
    write(rows, a.out)
    print("ok: %d lignes, %d avec un son -> %s" % (len(rows), sum(bool(r["cle"]) for r in rows),
                                                   os.path.relpath(a.out, ca.ROOT)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
