#!/usr/bin/env python3
"""Definitive drum targets (rule: docs/sons/fin/REGLE-BATTERIE.md).

Reads the passe2 tables and their counter-measures (read-only) and writes
docs/sons/fin/cibles-batterie-definitives.tsv. Packs are written as rN/pN only.

Run: python3 -I scripts/sons/fin/cibles_batterie.py
"""
import csv
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
FAMILY = {"kick": "kick", "kick_808": "808", "snare": "caisse_claire", "clap": "clap",
          "hat_closed": "charley_ferme", "hat_open": "charley_ouvert", "crash": "crash",
          "rim": "rim", "tom": "tom"}
FAMILIES = ["kick", "808", "caisse_claire", "clap", "charley_ferme", "charley_ouvert", "crash", "rim", "tom"]
# style -> (table styles, grouping label or "")
STYLES = [("house", ("house",), ""), ("techhouse", ("house", "techno"), "house+techno"),
          ("techno", ("techno",), ""), ("trap", ("trap", "drift-phonk"), "trap+drift-phonk"),
          ("dnb", ("dnb",), ""), ("dubstep", ("dubstep",), "")]
ONESHOT_MEASURES = ["dur_s", "peak_dbfs", "rms_dbfs", "t20_ms", "t60_ms", "centroid_hz",
                    "hf_db", "transient_body_db", "pitch_steady_hz"]
LOOP_MEASURES = ["dur_s", "peak_dbfs", "rms_dbfs"]
DB_MEASURES = {"peak_dbfs", "rms_dbfs", "hf_db", "transient_body_db"}
HEADER = ["famille", "style", "coups", "packs", "liste_packs", "mesure",
          "p10", "p25", "mediane", "p75", "p90", "statut"]


def quantile(xs, q):
    s = sorted(xs)
    if not s:
        return None
    pos = q * (len(s) - 1)
    lo = int(pos)
    hi = min(lo + 1, len(s) - 1)
    return s[lo] + (s[hi] - s[lo]) * (pos - lo)


def num(v):
    try:
        x = float(v)
    except (TypeError, ValueError):
        return None
    return x if x == x else None


def load(tables, verif, exclusions):
    excl = set()
    with open(exclusions) as f:
        for r in csv.DictReader(f, delimiter="\t"):
            excl.add(r["cle"])
    ok = {}
    for name in sorted(os.listdir(verif)):
        if name.startswith("rar") and name.endswith(".csv"):
            with open(os.path.join(verif, name)) as f:
                for r in csv.DictReader(f):
                    ok[r["cle"]] = r
    sounds = []
    for fname, kind in (("drum_oneshot.csv", "oneshot"), ("drum_loop.csv", "loop")):
        with open(os.path.join(tables, fname)) as f:
            for r in csv.DictReader(f):
                if r["cle"] in excl:
                    continue
                if kind == "oneshot":
                    fam = FAMILY.get(r["subtype"])
                    if fam is None:
                        continue
                else:
                    fam = "boucle"
                sounds.append({"cle": r["cle"], "famille": fam, "style": r["style"],
                               "pack": "r%s/%s" % (r["rar"], r["pack"]), "row": r,
                               "ver": ok.get(r["cle"], {})})
    return sounds


def target(sounds, measure, label):
    vals = [(s["pack"], num(s["row"].get(measure)), s["ver"].get(measure + "_ok", "")) for s in sounds]
    vals = [v for v in vals if v[1] is not None]
    xs = [v[1] for v in vals]
    packs = {}
    for p, _, _ in vals:
        packs[p] = packs.get(p, 0) + 1
    n = len(xs)
    q = [quantile(xs, k) for k in (0.10, 0.25, 0.50, 0.75, 0.90)]
    statut = None
    if n < 20:
        statut = "pas de cible : (1) %d sons (moins de 20)" % n
    elif sum(1 for c in packs.values() if c >= 5) < 2:
        statut = "pas de cible : (2) moins de 2 packs de 5 sons"
    elif max(packs.values()) > 0.90 * n:
        statut = "pas de cible : (3) un pack a %d %% des sons" % round(100.0 * max(packs.values()) / n)
    else:
        good = sum(1 for v in vals if v[2] == "yes")
        if good < 0.98 * n:
            statut = "pas de cible : (4) contre-mesure d'accord sur %d %% seulement" % int(100.0 * good / n)
        else:
            big = max(sorted(packs), key=lambda p: packs[p])
            rest = [v[1] for v in vals if v[0] != big]
            m2 = quantile(rest, 0.5)
            out = not (q[1] <= m2 <= q[3])
            if measure in DB_MEASURES:
                moved = abs(m2 - q[2]) >= 1.0
            else:
                moved = abs(m2 - q[2]) >= 0.20 * abs(q[2]) if q[2] else m2 != q[2]
            if out and moved:
                statut = "pas de cible : (5) sans le plus gros pack la médiane passe à %.4g" % m2
    if statut is None:
        statut = "définitive" + (" (regroupement %s)" % label if label else "")
    return n, packs, q, statut


def fmt(x):
    if x is None:
        return ""
    return ("%.4f" % x).rstrip("0").rstrip(".")


def rows_for(sounds):
    out = []
    plan = [(f, ONESHOT_MEASURES if f == "kick" else ONESHOT_MEASURES[:-1]) for f in FAMILIES]
    plan.append(("boucle", LOOP_MEASURES))
    for fam, measures in plan:
        styles = STYLES + [("tous_styles", None, "")]
        for style, srcs, label in styles:
            cell = [s for s in sounds if s["famille"] == fam and (srcs is None or s["style"] in srcs)]
            for m in measures:
                n, packs, q, statut = target(cell, m, label)
                if style == "tous_styles":
                    statut = statut.replace("définitive", "repère tous styles")
                out.append([fam, style, str(n), str(len(packs)), " ".join(sorted(packs)), m]
                           + [fmt(x) for x in q] + [statut])
    return out


def run(tables, verif, exclusions, out):
    rows = rows_for(load(tables, verif, exclusions))
    with open(out, "w") as f:
        f.write("# Cibles définitives de la batterie (règle : docs/sons/fin/REGLE-BATTERIE.md), "
                "écrites par scripts/sons/fin/cibles_batterie.py.\n")
        f.write("# Packs notés rN/pN ; « pas de cible » = on garde le son actuel, la première condition "
                "qui échoue est écrite ; « tous_styles » est un repère pour les originaux, pas une cible.\n")
        f.write("\t".join(HEADER) + "\n")
        for r in rows:
            f.write("\t".join(r) + "\n")
    return rows


def main():
    p = os.path.join
    rows = run(p(ROOT, "docs/sons/passe2/tout"), p(ROOT, "docs/sons/passe2/verif"),
               p(ROOT, "docs/sons/fin/exclusions-batterie.tsv"),
               p(ROOT, "docs/sons/fin/cibles-batterie-definitives.tsv"))
    d = sum(1 for r in rows if r[-1].startswith("définitive"))
    nc = sum(1 for r in rows if r[-1].startswith("pas de cible") and r[1] != "tous_styles")
    print("cibles définitives : %d ; pas de cible : %d" % (d, nc))
    return 0


if __name__ == "__main__":
    sys.exit(main())
