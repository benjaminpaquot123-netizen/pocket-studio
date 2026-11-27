#!/usr/bin/env python3
"""Final targets for the non-drum-hit families (reads only CSV, never a sound).

  python3 -I scripts/sons/fin/cibles_autres.py            # writes docs/sons/fin/cibles-autres-definitives.tsv
  python3 -I scripts/sons/fin/cibles_autres.py --selftest # fabricated tables only

Inputs: the passe-2 tables (docs/sons/passe2/tout/*.csv, written by scripts/sons/passe2/fin/tables.py)
and the blind counter-measure (docs/sons/passe2/verif/rar*.csv, joined by `cle`).

A figure is "définitive" only when ALL hold:
  - at least MIN_N sounds and MIN_PACKS distinct packs (rar + pack);
  - its column was counter-measured on at least MIN_N of these same sounds with >= ACCORD agreement
    (the counter-measure's own tolerance, docs/sons/passe2/verif/TOLERANCES.md).
The drum-loop tempo is read on the name tag (bpm_tag), and only from loops whose audio estimate,
folded to half / double, agrees with the tag (bpm_gap_folded_pct <= FOLD_TOL): two independent paths,
the share of loops in agreement must be >= TEMPO_ACCORD.
Never a target on: the estimated key, the raw tempo estimate, the note of a melodic sound
(f0_hz: 81 % agreement only), nor any column the counter-measure did not check (MIDI, voice pitch,
envelopes): those rows are written with status "pas de cible" and the reason.
"""
import argparse
import csv
import glob
import os
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
TABLES = os.path.join(ROOT, "docs/sons/passe2/tout")
VERIF = os.path.join(ROOT, "docs/sons/passe2/verif")
OUT = os.path.join(ROOT, "docs/sons/fin/cibles-autres-definitives.tsv")
MIN_N, MIN_PACKS, ACCORD, TEMPO_ACCORD, FOLD_TOL = 20, 2, 0.98, 0.90, 2.0
COLS = ["famille", "style", "sons", "packs", "liste_packs", "mesure",
        "p10", "p25", "mediane", "p75", "p90", "statut"]
LEVEL = ["dur_s", "rms_dbfs", "peak_dbfs"]  # counter-measured on every audio file
# family name -> (table, row filter, measures). A measure is a column or (label, function(row)).
NOT_CHECKED = "pas de cible (colonne non contre-mesurée)"


def fnum(v):
    try:
        x = float(v)
    except (TypeError, ValueError):
        return None
    return x if x == x and abs(x) != float("inf") else None


def pct(xs, q):
    xs = sorted(xs)
    if not xs:
        return None
    i = (len(xs) - 1) * q
    lo = int(i)
    hi = min(lo + 1, len(xs) - 1)
    return xs[lo] + (xs[hi] - xs[lo]) * (i - lo)


def read(path):
    if not os.path.exists(path):
        return []
    with open(path, newline="") as fh:
        return list(csv.DictReader(fh))


def verif_index(verif_dir):
    idx = {}
    for p in sorted(glob.glob(os.path.join(verif_dir, "rar*.csv"))):
        for r in read(p):
            if r.get("cle"):
                idx[r["cle"]] = r
    return idx


def midi_range(r):
    a, b = fnum(r.get("pitch_min")), fnum(r.get("pitch_max"))
    return None if a is None or b is None else b - a


def families(t):
    om = t.get("oneshot_melodic", [])
    fam = [("basse (coups)", [r for r in om if r.get("role") == "bass"], LEVEL)]
    for role in sorted({r.get("role") or "-" for r in om} - {"bass"}):
        fam.append(("coup mélodique : %s" % role, [r for r in om if (r.get("role") or "-") == role], LEVEL))
    fam.append(("boucle mélodique", t.get("melodic_loop", []), LEVEL))
    fam.append(("boucle de batterie", t.get("drum_loop", []), ["tempo"] + LEVEL))
    fam.append(("voix", t.get("vocal", []), LEVEL + ["range_st", "voiced_pct"]))
    fam.append(("effet", t.get("fx", []), LEVEL))
    fam.append(("midi", t.get("midi", []),
                [("notes_per_bar", None), ("etendue_demitons", midi_range), ("bars", None)]))
    return fam


def accord(rows, col, vidx):
    seen = ok = 0
    for r in rows:
        v = vidx.get(r.get("cle"), {}).get(col + "_ok", "")
        if v in ("yes", "no"):
            seen += 1
            ok += v == "yes"
    return seen, (ok / seen if seen else 0.0)


def line(fam, style, rows, label, vals, statut):
    packs = sorted({"r%s%s" % (r["rar"], r["pack"]) for r in rows})
    q = [pct(vals, x) for x in (0.10, 0.25, 0.50, 0.75, 0.90)]
    return dict(zip(COLS, [fam, style, len(vals), len(packs), " ".join(packs), label]
                    + ["" if v is None else "%.4g" % v for v in q] + [statut]))


def target(fam, style, rows, meas, vidx):
    rows = [r for r in rows if not r.get("error")]
    if meas == "tempo":
        tagged = [r for r in rows if fnum(r.get("bpm_tag"))]
        good = [r for r in tagged if fnum(r.get("bpm_gap_folded_pct")) is not None
                and fnum(r["bpm_gap_folded_pct"]) <= FOLD_TOL]
        share = len(good) / len(tagged) if tagged else 0.0
        vals = [fnum(r["bpm_tag"]) for r in good]
        out = line(fam, style, good, "tempo étiqueté (accord replié %d %%)" % round(100 * share), vals, "")
        ok = share >= TEMPO_ACCORD and style != "tous"
        reason = ("" if ok else " (tempos de styles différents mélangés)" if style == "tous"
                  else " (étiquette et estimation repliée d'accord < %d %%)" % round(100 * TEMPO_ACCORD))
    else:
        label, fn = (meas, None) if isinstance(meas, str) else meas
        fn = fn or (lambda r, c=label: fnum(r.get(c)))
        kept = [(r, fn(r)) for r in rows]
        kept = [(r, v) for r, v in kept if v is not None]
        seen, share = accord([r for r, _ in kept], label, vidx)
        out = line(fam, style, [r for r, _ in kept], label, [v for _, v in kept], "")
        ok = seen >= MIN_N and share >= ACCORD
        reason = "" if ok else (" (contre-mesure %d %% sur %d)" % (round(100 * share), seen) if seen
                                else " (colonne non contre-mesurée)")
    if out["sons"] >= MIN_N and out["packs"] >= MIN_PACKS and ok:
        out["statut"] = "définitive"
    else:
        small = out["sons"] < MIN_N or out["packs"] < MIN_PACKS
        out["statut"] = "pas de cible" + (" (n %d, %d packs)" % (out["sons"], out["packs"]) if small else "") + reason
    return out


def build(tables_dir, verif_dir):
    t = {os.path.basename(p)[:-4]: read(p) for p in glob.glob(os.path.join(tables_dir, "*.csv"))}
    vidx = verif_index(verif_dir)
    out = []
    for fam, rows, measures in families(t):
        styles = ["tous"] + sorted({r.get("style") or "unlabelled" for r in rows} - {"unlabelled"})
        for st in styles:
            sub = rows if st == "tous" else [r for r in rows if r.get("style") == st]
            for m in measures:
                out.append(target(fam, st, sub, m, vidx))
    return out


def write(rows, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=COLS, delimiter="\t", lineterminator="\n")
        w.writeheader()
        w.writerows(rows)


def selftest():
    fails = []

    def check(name, got, want):
        ok = got == want
        print("%s %-60s got %r want %r" % ("ok  " if ok else "FAIL", name, got, want))
        if not ok:
            fails.append(name)

    with tempfile.TemporaryDirectory() as d:
        tdir, vdir = os.path.join(d, "t"), os.path.join(d, "v")
        os.makedirs(tdir)
        os.makedirs(vdir)

        def wr(path, rows):
            with open(path, "w", newline="") as fh:
                w = csv.DictWriter(fh, fieldnames=list(rows[0]))
                w.writeheader()
                w.writerows(rows)

        fx = [{"rar": str(1 + i % 2), "pack": "p1", "cle": "fx%d" % i, "style": "edm", "error": "",
               "dur_s": str(i + 1), "rms_dbfs": "-20", "peak_dbfs": "-1"} for i in range(25)]
        fx.append({"rar": "1", "pack": "p1", "cle": "fxbad", "style": "edm", "error": "bad",
                   "dur_s": "999", "rms_dbfs": "", "peak_dbfs": ""})
        wr(os.path.join(tdir, "fx.csv"), fx)
        voc = [{"rar": "3", "pack": "p1", "cle": "v%d" % i, "style": "edm", "error": "", "dur_s": "2",
                "rms_dbfs": "-20", "peak_dbfs": "-3", "range_st": "5", "voiced_pct": "50"} for i in range(30)]
        wr(os.path.join(tdir, "vocal.csv"), voc)
        loops = [{"rar": str(1 + i % 2), "pack": "p1", "cle": "d%d" % i, "style": "dnb", "error": "",
                  "bpm_tag": "174", "bpm_gap_folded_pct": "0.5" if i < 23 else "30", "dur_s": "4",
                  "rms_dbfs": "-12", "peak_dbfs": "-1"} for i in range(25)]
        loops += [{"rar": str(1 + i % 2), "pack": "p1", "cle": "h%d" % i, "style": "house", "error": "",
                   "bpm_tag": "125", "bpm_gap_folded_pct": "0.5" if i < 15 else "30", "dur_s": "4",
                   "rms_dbfs": "-12", "peak_dbfs": "-1"} for i in range(25)]
        # durcie: one pack of 5+ loops in agreement = no sure tempo; two packs of 5 within 2 % = sure;
        # two packs that disagree (125 / 150) = no single tempo.
        loops += [{"rar": "5", "pack": "p1", "cle": "t%d" % i, "style": "techno", "error": "",
                   "bpm_tag": "150", "bpm_gap_folded_pct": "0.5", "dur_s": "4",
                   "rms_dbfs": "-12", "peak_dbfs": "-1"} for i in range(25)]
        loops += [{"rar": "1" if i < 5 else "8", "pack": "p1", "cle": "s%d" % i, "style": "dubstep", "error": "",
                   "bpm_tag": "140" if i < 5 else "142", "bpm_gap_folded_pct": "0.5", "dur_s": "4",
                   "rms_dbfs": "-12", "peak_dbfs": "-1"} for i in range(10)]
        loops += [{"rar": "3" if i < 6 else "5", "pack": "p1", "cle": "w%d" % i, "style": "rave", "error": "",
                   "bpm_tag": "125" if i < 6 else "150", "bpm_gap_folded_pct": "0.5", "dur_s": "4",
                   "rms_dbfs": "-12", "peak_dbfs": "-1"} for i in range(12)]
        wr(os.path.join(tdir, "drum_loop.csv"), loops)
        # durcie: 77 sounds + 1 of a second pack; 93 % of one pack (93 + 7)
        om = [{"rar": "7" if i < 77 else "9", "pack": "p3" if i < 77 else "p2", "cle": "k%d" % i,
               "role": "keys", "style": "trap", "error": "", "dur_s": "2", "rms_dbfs": "-20",
               "peak_dbfs": "-1"} for i in range(78)]
        om += [{"rar": "8" if i < 93 else "9", "pack": "p1" if i < 93 else "p3", "cle": "l%d" % i,
                "role": "lead", "style": "dubstep", "error": "", "dur_s": "2", "rms_dbfs": "-20",
                "peak_dbfs": "-1"} for i in range(100)]
        om += [{"rar": "8" if i < 60 else "9", "pack": "p1" if i < 60 else "p3", "cle": "y%d" % i,
                "role": "synth", "style": "dubstep", "error": "", "dur_s": "2", "rms_dbfs": "-20",
                "peak_dbfs": "-1"} for i in range(80)]
        wr(os.path.join(tdir, "oneshot_melodic.csv"), om)
        ver = [{"cle": "fx%d" % i, "dur_s_ok": "yes", "rms_dbfs_ok": "yes" if i else "no", "peak_dbfs_ok": "yes"}
               for i in range(25)]
        ver += [{"cle": "v%d" % i, "dur_s_ok": "yes", "rms_dbfs_ok": "yes", "peak_dbfs_ok": "yes"} for i in range(30)]
        ver += [{"cle": r["cle"], "dur_s_ok": "yes", "rms_dbfs_ok": "yes", "peak_dbfs_ok": "yes"} for r in om]
        wr(os.path.join(vdir, "rar1.csv"), ver)
        res = build(tdir, vdir)
        get = {(r["famille"], r["style"], r["mesure"].split(" (")[0]): r for r in res}
        r = get[("effet", "tous", "dur_s")]
        check("fx dur definitive", r["statut"], "définitive")
        check("fx dur n skips the error row", r["sons"], 25)
        check("fx dur median", r["mediane"], "13")
        check("fx dur p10", r["p10"], "3.4")
        check("fx packs", r["liste_packs"], "r1p1 r2p1")
        check("fx rms 96 % agreement refused", get[("effet", "tous", "rms_dbfs")]["statut"].startswith("pas de cible"), True)
        check("voice one pack refused", get[("voix", "tous", "dur_s")]["statut"] == "définitive", False)
        check("voice range never checked", "non contre-mesurée" in get[("voix", "tous", "range_st")].get("raison", ""), True)
        tempo = {(r["famille"], r["style"]): r for r in res if r["mesure"].startswith("tempo")}
        sure = "tempo du fabricant sûr"
        check("dnb tempo 2 packs of 5+ agreeing = sure", tempo[("boucle de batterie", "dnb")]["statut"], sure)
        check("dnb tempo n = every tagged loop", tempo[("boucle de batterie", "dnb")]["sons"], 25)
        check("dnb tempo agreement 23/25 written", "23/25" in tempo[("boucle de batterie", "dnb")].get("accord", ""), True)
        check("house tempo 60 % refused", tempo[("boucle de batterie", "house")]["statut"] == sure, False)
        check("tempo of all styles mixed refused", tempo[("boucle de batterie", "tous")]["statut"] == sure, False)
        check("tempo one pack in agreement not sure", tempo[("boucle de batterie", "techno")]["statut"] == sure, False)
        check("tempo 2 packs of 5 within 2 % sure", tempo[("boucle de batterie", "dubstep")]["statut"], sure)
        check("tempo 125 / 150 by pack not sure", tempo[("boucle de batterie", "rave")]["statut"] == sure, False)
        check("tempo 125 / 150 written pack by pack", all(x in tempo[("boucle de batterie", "rave")].get("raison", "")
                                                           for x in ("r3p1 125", "r5p1 150")), True)
        check("tempo never 'définitive'", any(r["statut"] == "définitive" for r in tempo.values()), False)
        k = get[("coup mélodique : keys", "trap", "dur_s")]
        check("77 + 1 of a second pack not definitive", k["statut"] == "définitive", False)
        check("77 + 1: packs_5 = 1", k.get("packs_5"), 1)
        l = get[("coup mélodique : lead", "dubstep", "dur_s")]
        check("93 % of one pack not definitive", l["statut"] == "définitive", False)
        check("93 %: part_pack_max", l.get("part_pack_max"), "93")
        check("60 + 20 definitive", get[("coup mélodique : synth", "dubstep", "dur_s")]["statut"], "définitive")
        check("refused line keeps the current sound", l["statut"], "pas de cible, on garde le son actuel")
        check("refused line has a reason", "90 %" in l.get("raison", ""), True)
        check("no verif = no target on loop levels", get[("boucle de batterie", "dnb", "dur_s")]["statut"].startswith("pas de cible"), True)
        check("no key / raw tempo / f0 column", any(m in r["mesure"] for r in res for m in ("key_est", "bpm_est", "f0_hz")), False)
    print("%d failed" % len(fails))
    return 1 if fails else 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--selftest", action="store_true")
    ap.add_argument("--tables", default=TABLES)
    ap.add_argument("--verif", default=VERIF)
    ap.add_argument("--out", default=OUT)
    a = ap.parse_args(argv)
    if a.selftest:
        return selftest()
    rows = build(a.tables, a.verif)
    write(rows, a.out)
    n = sum(r["statut"] == "définitive" for r in rows)
    print("ok: %d lines, %d définitives -> %s" % (len(rows), n, os.path.relpath(a.out, ROOT)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
