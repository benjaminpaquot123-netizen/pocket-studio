#!/usr/bin/env python3
"""Pick one original pack sound per "claviers" line for the private listening page (reads CSV only).

  python3 -I scripts/sons/fin/page/choix_claviers.py             # writes docs/sons/fin/page/originaux-claviers.csv
  python3 -I scripts/sons/fin/page/choix_claviers.py --plan      # prints rar, pack, file_id, key of the picks
  python3 -I scripts/sons/fin/page/choix_claviers.py --selftest  # fabricated tables only

For each line (family, style; "tous" = every style):
  candidates = sounds of the passe-2 table oneshot_melodic with this role (and style), no error,
               whose counter-measure (docs/sons/passe2/verif/rar*.csv) says "yes" on dur_s, rms_dbfs and
               peak_dbfs, minus the keys REMESURE marks "non" (re-measured file did not match);
  targets    = the "définitive" measures of the line in docs/sons/fin/cibles-autres-definitives.tsv;
  distance   = mean over these measures of |value - median| / max(p75 - p25, floor),
               floor = 0.5 dB, or 5 % of the median for the duration.
The pick is the smallest distance; among candidates within TIE of the best, the pack of the smallest
archive wins (fewer bytes to download), then the distance, then the key.
Never a file or folder name, a link or a pack title in the output: packs are written rNpN.
"""
import argparse
import csv
import os
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import cibles_autres as ca  # noqa: E402

OUT = os.path.join(ca.ROOT, "docs/sons/fin/page/originaux-claviers.csv")
TSV = ca.OUT
TIE, FLOOR_DB, FLOOR_DUR = 0.1, 0.5, 0.05
# archive sizes in MB (download cost only; measured once, 2026-11-27)
RAR_MB = {1: 600, 2: 711, 3: 259, 4: 418, 5: 331, 6: 558, 7: 2538, 8: 1092, 9: 1781, 10: 1351,
          11: 2369, 12: 573}
LINES = [("chords_tous", "chords", "tous"), ("keys_tous", "keys", "tous"), ("keys_lofi", "keys", "lofi"),
         ("lead_tous", "lead", "tous"), ("lead_dubstep", "lead", "dubstep"),
         ("synth_tous", "synth", "tous"), ("synth_house", "synth", "house"),
         ("synth_techno", "synth", "techno")]
# key -> "oui"/"non": the extracted file re-measured with scripts/sons/passe2/verif/mesure.py matches the
# table (dur 0.01 s, peak 0.1 dB, rms 0.2 dB). "non" removes the key from the candidates.
REMESURE = {}
COLS = ["slug", "famille", "style", "statut", "cle", "pack", "dur_s", "rms_dbfs", "peak_dbfs",
        "cible_dur_s", "cible_rms_dbfs", "cible_peak_dbfs", "distance", "remesure_ok"]


def targets(tsv_rows, fam, style):
    t = {}
    for r in tsv_rows:
        if r["famille"] == fam and r["style"] == style and r["statut"] == ca.DEF and r["mesure"] in ca.LEVEL:
            t[r["mesure"]] = (float(r["mediane"]), float(r["p25"]), float(r["p75"]))
    return t


def distance(row, t):
    ds = []
    for m, (med, p25, p75) in sorted(t.items()):
        v = ca.fnum(row.get(m))
        if v is None:
            return None
        floor = FLOOR_DUR * abs(med) if m == "dur_s" else FLOOR_DB
        ds.append(abs(v - med) / max(p75 - p25, floor))
    return sum(ds) / len(ds) if ds else None


def agreed(row, vidx):
    v = vidx.get(row.get("cle"), {})
    return all(v.get(c + "_ok") == "yes" for c in ca.LEVEL)


def ranked(rows, role, style, t, vidx, remesure):
    cand = []
    for r in rows:
        if r.get("role") != role or r.get("error") or (style != "tous" and r.get("style") != style):
            continue
        if not agreed(r, vidx) or remesure.get(r.get("cle")) == "non":
            continue
        d = distance(r, t)
        if d is not None:
            cand.append((d, r))
    if not cand:
        return []
    best = min(d for d, _ in cand)
    size = lambda r: RAR_MB.get(int(r["rar"]), 10 ** 6)  # noqa: E731
    near = sorted([c for c in cand if c[0] <= best + TIE], key=lambda c: (size(c[1]), c[0], c[1]["cle"]))
    far = sorted([c for c in cand if c[0] > best + TIE], key=lambda c: (c[0], c[1]["cle"]))
    return near + far


def build(tables_dir, verif_dir, tsv_path, remesure):
    rows = ca.read(os.path.join(tables_dir, "oneshot_melodic.csv"))
    vidx = ca.verif_index(verif_dir)
    tsv = list(csv.DictReader(open(tsv_path, newline=""), delimiter="\t"))
    out, plan = [], []
    for slug, role, style in LINES:
        fam = "coup mélodique : %s" % role
        t = targets(tsv, fam, style)
        rk = ranked(rows, role, style, t, vidx, remesure) if t else []
        base = {"slug": slug, "famille": fam, "style": style, "statut": ca.DEF if t else ca.KEEP}
        for m in ca.LEVEL:
            base["cible_" + m] = "%.4g" % t[m][0] if m in t else ""
        if not rk:
            out.append(dict(base, cle="", pack="", dur_s="", rms_dbfs="", peak_dbfs="", distance="",
                            remesure_ok=""))
            continue
        d, r = rk[0]
        out.append(dict(base, cle=r["cle"], pack=ca.pack_of(r), dur_s=r["dur_s"], rms_dbfs=r["rms_dbfs"],
                        peak_dbfs=r["peak_dbfs"], distance="%.3f" % d,
                        remesure_ok=remesure.get(r["cle"], "")))
        plan.append((slug, r["rar"], r["pack"], r["file_id"], r["cle"]))
    return out, plan


def write(rows, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=COLS, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)


def selftest():
    fails = []

    def check(name, got, want):
        if got != want:
            fails.append("%s: got %r want %r" % (name, got, want))

    with tempfile.TemporaryDirectory() as d:
        tdir, vdir = os.path.join(d, "t"), os.path.join(d, "v")
        os.makedirs(tdir)
        os.makedirs(vdir)

        def sh(cle, rar, style, dur, rms, peak, role="keys", err=""):
            return {"rar": str(rar), "pack": "p1", "cle": cle, "file_id": "f" + cle, "role": role,
                    "style": style, "error": err, "dur_s": str(dur), "rms_dbfs": str(rms), "peak_dbfs": str(peak)}

        rows = [sh("near_big", 7, "lofi", 10, -20, -0.1),      # distance 0 but in the biggest archive
                sh("near_small", 3, "lofi", 10, -20.6, -0.1),  # distance 0.05: within TIE, smaller archive
                sh("far", 3, "trap", 4, -12, -3),
                sh("bad_cm", 3, "lofi", 10, -20, -0.1),        # counter-measure disagrees
                sh("err", 3, "lofi", 10, -20, -0.1, err="x"),
                sh("lead1", 5, "dubstep", 2, -16, -0.1, role="lead")]
        with open(os.path.join(tdir, "oneshot_melodic.csv"), "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=list(rows[0]))
            w.writeheader()
            w.writerows(rows)
        with open(os.path.join(vdir, "rar1.csv"), "w", newline="") as fh:
            cols = ["cle"] + [c + "_ok" for c in ca.LEVEL]
            w = csv.DictWriter(fh, fieldnames=cols)
            w.writeheader()
            for r in rows:
                w.writerow({"cle": r["cle"], "dur_s_ok": "yes", "rms_dbfs_ok": "no" if r["cle"] == "bad_cm" else "yes",
                            "peak_dbfs_ok": "yes"})
        tsv = os.path.join(d, "c.tsv")
        with open(tsv, "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=["famille", "style", "mesure", "p25", "mediane", "p75", "statut"],
                               delimiter="\t")
            w.writeheader()
            for st in ("tous", "lofi"):
                w.writerow({"famille": "coup mélodique : keys", "style": st, "mesure": "dur_s", "p25": "8",
                            "mediane": "10", "p75": "12", "statut": ca.DEF})
                w.writerow({"famille": "coup mélodique : keys", "style": st, "mesure": "rms_dbfs", "p25": "-22",
                            "mediane": "-20", "p75": "-18", "statut": ca.DEF})
            w.writerow({"famille": "coup mélodique : keys", "style": "lofi", "mesure": "peak_dbfs", "p25": "-1",
                        "mediane": "-0.1", "p75": "-0.1", "statut": ca.KEEP})
        out, _ = build(tdir, vdir, tsv, {})
        got = {r["slug"]: r for r in out}
        check("tie goes to the smaller archive", got["keys_lofi"]["cle"], "near_small")
        check("distance within tie", got["keys_lofi"]["distance"], "0.075")
        check("pack written rNpN", got["keys_lofi"]["pack"], "r3p1")
        check("non-definitive measure ignored", got["keys_lofi"]["cible_peak_dbfs"], "")
        check("line without targets has no pick", got["lead_dubstep"]["cle"], "")
        check("line without targets status", got["lead_dubstep"]["statut"], ca.KEEP)
        out, _ = build(tdir, vdir, tsv, {"near_small": "non"})
        check("remesure non -> next one", {r["slug"]: r for r in out}["keys_lofi"]["cle"], "near_big")
        rk = ranked(rows, "keys", "tous", {"dur_s": (10, 8, 12)}, ca.verif_index(vdir), {})
        check("counter-measure and error removed", sorted(r["cle"] for _, r in rk), ["far", "near_big", "near_small"])
        check("floor 5 % of the median for duration", distance({"dur_s": "11"}, {"dur_s": (10, 10, 10)}), 2.0)
        check("floor 0.5 dB", distance({"rms_dbfs": "-19"}, {"rms_dbfs": (-20, -20, -20)}), 2.0)
    for f in fails:
        print("FAIL", f)
    print("selftest: %d failure(s)" % len(fails))
    return 1 if fails else 0


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--tables", default=ca.TABLES)
    ap.add_argument("--verif", default=ca.VERIF)
    ap.add_argument("--targets", default=TSV)
    ap.add_argument("--out", default=OUT)
    ap.add_argument("--plan", action="store_true")
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args(argv)
    if a.selftest:
        return selftest()
    out, plan = build(a.tables, a.verif, a.targets, REMESURE)
    if a.plan:
        for p in plan:
            print(",".join(p))
        return 0
    write(out, a.out)
    print("wrote %s (%d lines, %d with a sound)" % (a.out, len(out), sum(bool(r["cle"]) for r in out)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
