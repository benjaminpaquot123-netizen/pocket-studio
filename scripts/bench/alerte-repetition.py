#!/usr/bin/env python3
"""Repeated-failure alarm (« échec répété »): Benjamin asked for it on 2026-11-02 and again on 2026-11-14.

The rule lived only in the orchestrator's memory and got lost; this script holds it instead.

  alerte-repetition.py note <chantier> <lot> echec|ok "<defect ids, comma-separated>" [<source>]
  alerte-repetition.py etat

The registry is docs/audits/registre-echecs.tsv, one line per control verdict or portal refusal:
  date  chantier  lot  issue  defauts  source
A « chantier » groups the successive lots that fix the same thing (tuto → tuto2 → tuto3 → fable …).

Alarms (exit 2 when there is at least one):
  - a chantier has failed twice in a row since its last « ok »;
  - one defect id appears in two failing lines of the same chantier (it came back);
  - a control verdict.md under docs/audits/ultra/sieges/ is not in the registry (so nothing gets skipped).
Each alarm ends with the question for Benjamin: « Fable nécessaire ? ». Standard library only."""
import datetime
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
REG = os.path.join(ROOT, "docs", "audits", "registre-echecs.tsv")
SEATS = os.path.join(ROOT, "docs", "audits", "ultra", "sieges")
HEADER = "date\tchantier\tlot\tissue\tdefauts\tsource\n"


def read():
    rows = []
    if not os.path.exists(REG):
        return rows
    with open(REG, encoding="utf-8") as f:
        for n, line in enumerate(f):
            if n == 0 or not line.strip() or line.startswith("#"):
                continue
            cells = (line.rstrip("\n").split("\t") + [""] * 6)[:6]
            rows.append(dict(zip(["date", "chantier", "lot", "issue", "defauts", "source"], cells)))
    return rows


def alarms(rows):
    out = []
    chantiers = []
    for r in rows:
        if r["chantier"] not in chantiers:
            chantiers.append(r["chantier"])
    for c in chantiers:
        mine = [r for r in rows if r["chantier"] == c]
        streak = []
        for r in mine:
            streak = [] if r["issue"] == "ok" else streak + [r]
        if len(streak) >= 2:
            lots = ", ".join(r["lot"] for r in streak)
            out.append(f"ALERTE boucle : « {c} » a échoué {len(streak)} fois de suite ({lots}). Fable nécessaire ?")
        seen = {}
        for r in streak:
            for d in filter(None, (x.strip() for x in r["defauts"].split(","))):
                seen.setdefault(d, []).append(r["lot"])
        for d, lots in seen.items():
            if len(lots) >= 2:
                out.append(f"ALERTE défaut qui revient : « {d} » dans « {c} » ({', '.join(lots)}). Fable nécessaire ?")
    sources = {r["source"] for r in rows}
    for dirpath, _, files in os.walk(SEATS):
        if "verdict.md" in files and "controle" in dirpath.split(os.sep):
            rel = os.path.relpath(os.path.join(dirpath, "verdict.md"), ROOT)
            if rel not in sources:
                out.append(f"ALERTE verdict non enregistré : {rel} (le noter avec « note » avant toute décision)")
    return out


def main(argv):
    if len(argv) >= 5 and argv[1] == "note":
        chantier, lot, issue, defauts = argv[2:6]
        if issue not in ("echec", "ok"):
            sys.exit("issue : echec ou ok")
        source = argv[6] if len(argv) > 6 else ""
        new = not os.path.exists(REG)
        with open(REG, "a", encoding="utf-8") as f:
            if new:
                f.write(HEADER)
            f.write("\t".join([datetime.date.today().isoformat(), chantier, lot, issue, defauts, source]) + "\n")
    elif len(argv) != 2 or argv[1] != "etat":
        sys.exit(__doc__)
    found = alarms(read())
    for a in found:
        print(a)
    if not found:
        print("Aucune répétition d'échec.")
    return 2 if found else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
