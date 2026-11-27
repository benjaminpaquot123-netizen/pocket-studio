#!/usr/bin/env python3
"""Chooses, per family and style, the measured drum version closest to the target.

Reads the targets (docs/sons/matin/cibles-batterie.tsv) and the candidates' measures (the TSV
printed by matin_choix_mesure). Rule, fixed before any number was computed:
  - reliable measures: t20 and t40 (ms), plus the note (semitones) for the kick;
  - gap of a measure: d = 2 |v - T| / (p90 - p10), T the target; score = the mean gap;
  - the winner has the lowest score; within 0.05 of "nouvelle", "nouvelle" wins (a tie);
  - margin: the winner with T at p25, at the median and at p75 (all measures moved together);
    the same winner three times = "sur", otherwise "serre" and "nouvelle" is kept;
  - a target counts only with >= 20 hits and >= 2 packs ("?": packs not written in the repo,
    counted but flagged); otherwise "actuel": the app's own voice stays.
Definitive targets (docs/sons/fin/cibles-batterie-definitives.tsv, with a "statut" column; rule
docs/sons/fin/REGLE-BATTERIE.md, b), the rule is adapted as follows:
  - only "definitive" rows count (the five conditions are already checked there); measures t20_ms
    and t60_ms, plus pitch_steady_hz for the kick, read from the candidates' table-method columns
    (matin_choix_tables.py: Schroeder decay, like the reference tables);
  - a family and style without any definitive measure is "actuel" (the app's own voice stays);
  - gap, score, tie and margin are unchanged.
Prints the choice table and the C++ table literal (MeasuredDrums.cpp must hold it).
"""
import csv, sys

FAMILIES = ["kick", "808", "clap", "caisse_claire", "charley_ferme", "charley_ouvert", "rim", "tom", "crash"]
STYLES = ["house", "techhouse", "techno", "trap", "dnb", "dubstep"]
CODE = {"A": -1, "nouvelle": 0, "B": 1, "deep": 2, "actuel": 9}
TIE = 0.05


def rows(path):
    with open(path, encoding="utf8") as f:
        return list(csv.DictReader((l for l in f if not l.startswith("#")), delimiter="\t"))


def main(cibles, mesures):
    tgt = {}
    rs = rows(cibles)
    definitive = bool(rs) and "statut" in rs[0]
    for r in rs:
        if definitive:
            keep = ("t20_ms", "t60_ms") + (("pitch_steady_hz",) if r["famille"] == "kick" else ())
            if not r["statut"].startswith("d\u00e9finitive") or r["mesure"] not in keep:
                continue
        tgt.setdefault((r["famille"], r["style"]), {})[r["mesure"]] = r
    cand = {}
    for r in rows(mesures):
        cand.setdefault((r["famille"], r["style"]), {})[r["version"]] = r
    table, out = {}, []
    for f in FAMILIES:
        for s in STYLES:
            t, c = tgt.get((f, s)), cand.get((f, s))
            if not t or not c:
                table[f, s] = "actuel"
                out.append(f"{f}\t{s}\tactuel\tpas de cible{' definitive' if definitive else ''} ni de mesure, on garde le son actuel")
                continue
            any_t = next(iter(t.values()))
            coups, packs = int(any_t["coups"]), any_t["packs"]
            if not definitive and (coups < 20 or (packs != "?" and int(packs) < 2)):
                table[f, s] = "actuel"
                out.append(f"{f}\t{s}\tactuel\tpas de cible ({coups} coups, {packs} pack(s)), on garde le son actuel")
                continue
            meas = [m for m in ("t20", "t40", "note", "t20_ms", "t60_ms", "pitch_steady_hz") if m in t]

            def score(v, col):
                return sum(2 * abs(float(c[v][m]) - float(t[m][col])) / (float(t[m]["p90"]) - float(t[m]["p10"]))
                           for m in meas) / len(meas)

            def winner(col):
                sc = {v: score(v, col) for v in c}
                best = min(sc, key=sc.get)
                if "nouvelle" in sc and sc["nouvelle"] - sc[best] < TIE:
                    best = "nouvelle"
                return best, sc

            w, sc = winner("mediane")
            margin = {winner(col)[0] for col in ("p25", "mediane", "p75")}
            sure = len(margin) == 1
            pick = w if sure else "nouvelle"
            table[f, s] = pick
            detail = " ".join(f"{v}={sc[v]:.2f}" for v in sorted(sc))
            vals = " ".join(f"{m}={float(c[pick][m]):g}(cible {float(t[m]['mediane']):g} [{float(t[m]['p10']):g}-{float(t[m]['p90']):g}])" for m in meas)
            flag = " packs?" if packs == "?" else ""
            out.append(f"{f}\t{s}\t{pick}\t{'sur' if sure else 'serre (' + '/'.join(sorted(margin)) + ')'}{flag}\t{detail}\t{vals}")
    print("famille\tstyle\tretenu\tmarge\tscores (ecart normalise moyen)\tmesures du retenu")
    print("\n".join(out))
    lit = "static constexpr int8_t kChoice[kFamilies][kStyles] = {\n" + "".join(
        "    {" + ", ".join(str(CODE[table[f, s]]) for s in STYLES) + "},  // " + f + "\n" for f in FAMILIES) + "};"
    print("\n" + lit)
    return lit


if __name__ == "__main__":
    lit = main(sys.argv[1], sys.argv[2])
    if len(sys.argv) > 3:
        with open(sys.argv[3], encoding="utf8") as f:
            if lit not in f.read():
                sys.exit("MeasuredDrums.cpp does not hold this choice table: copy it there")
        print("table du moteur : identique")
