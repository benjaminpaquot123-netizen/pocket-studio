#!/usr/bin/env python3
"""Audit « banc crédible » 2, P1: the fresh review's verdict, as fusion-equipes.sh reads it.

  relecture-verdict.py [<replay id taken out of rejeux/> …] < review.md

Accepted only when:
  - its LAST non-empty line is « Verdict : 0 bloquant, 0 grave » (« — fusion possible » may follow, nothing else): a
    phrase found anywhere let « Verdict : 0 bloquant, 0 grave (avant correction du constat 1) » under a table row
    « | 1 | bloquant | … » through (forge2);
  - no table row has a cell « bloquant » or « grave » unless the row says « corrigé » (not « non corrigé » / « pas
    corrigé »);
  - each replay taken out of docs/audits/ultra/rejeux/ by the branch is named on a line « rejeu retiré : <id> : <raison> ».
Prints one line per refusal; exit 1 when there is one. Standard library only."""
import re
import sys

LAST = re.compile(r"^Verdict\s*:\s*0 bloquant,\s*0 grave(?:\s*[—-]+\s*fusion possible)?\s*\.?\s*$")
FIXED = re.compile(r"(?<!non )(?<!pas )\bcorrigée?s?\b", re.I)


def refusals(text, removed):
    out = []
    lines = [l.rstrip() for l in text.splitlines() if l.strip()]
    if not lines or not LAST.match(lines[-1].strip()):
        out.append("the review's last line is not « Verdict : 0 bloquant, 0 grave »"
                   + (f" (it is « {lines[-1].strip()[:80]} »)" if lines else " (empty review)"))
    for line in lines:
        s = line.strip()
        if not s.startswith("|"):
            continue
        cells = [c.strip().strip("*").strip().lower() for c in s.strip("|").split("|")]
        severe = [c for c in cells if c in ("bloquant", "grave")]
        if severe and not FIXED.search(s):
            out.append(f"a {severe[0]} row of the review is not « corrigé »: {s[:100]}")
    for rid in removed:
        if not re.search(r"^rejeu retiré\s*:\s*" + re.escape(rid) + r"\s*:\s*\S", text, re.M):
            out.append(f"replay {rid} taken out of rejeux/ without « rejeu retiré : {rid} : <raison> » in the review")
    return out


def main(argv):
    found = refusals(sys.stdin.read(), argv[1:])
    for r in found:
        print(r)
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
