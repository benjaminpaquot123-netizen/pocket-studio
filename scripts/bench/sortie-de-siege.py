#!/usr/bin/env python3
"""Seat exit detection (PLAN-RESTE 4.2): a tester (an AI agent) may write files outside its own seat folder.

  sortie-de-siege.py avant <lot>   # before the lot: lists the lot's files (<lot>/.avant-lot.txt)
  sortie-de-siege.py apres <lot>   # after: new files outside <lot>/<a-f>/ and outside the white list

Each foreign file is a line of <lot>/incidents-lot.tsv (at the lot's level: never pinned on a seat, nobody can tell
which one wrote it) and <lot>/lot.json gets « fichiers_hors_lot ». fuse-reports.py adds these lines to the round's
incidents-banc.tsv. Standard library only.
"""
import fnmatch
import json
import os
import sys

SEATS = set("abcdef")
WHITE = ["selftest-*.txt", "selftest*.log", "selftest.ok", "bench.log", "*.tsv", "*.md", "lot.json",
         ".avant-lot.txt", "junit/*", "briefs/*"]


def files(lot):
    out = set()
    for root, _, names in os.walk(lot):
        for n in names:
            rel = os.path.relpath(os.path.join(root, n), lot).replace(os.sep, "/")
            out.add(rel)
    return out


def foreign(rel):
    top = rel.split("/", 1)[0]
    if "/" in rel and top in SEATS:
        return False
    return not any(fnmatch.fnmatch(rel, w) for w in WHITE)


def main(argv):
    if len(argv) != 2 or argv[0] not in ("avant", "apres"):
        print(__doc__)
        return 2
    step, lot = argv
    listing = os.path.join(lot, ".avant-lot.txt")
    if step == "avant":
        with open(listing, "w", encoding="utf-8") as f:
            f.write("\n".join(sorted(files(lot))) + "\n")
        return 0
    before = set(open(listing, encoding="utf-8").read().split()) if os.path.isfile(listing) else set()
    new = sorted(r for r in files(lot) - before if foreign(r))
    meta_path = os.path.join(lot, "lot.json")
    meta = json.load(open(meta_path, encoding="utf-8")) if os.path.isfile(meta_path) else {}
    meta["fichiers_hors_lot"] = new
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    tsv = os.path.join(lot, "incidents-lot.tsv")
    with open(tsv, "w", encoding="utf-8") as f:
        f.write("siege\tgeste\tattente\tvu\n")
        for r in new:
            f.write(f"lot\t-\t-\tfichier écrit hors des sièges : {r}\n")
    print(f"sortie de siège : {len(new)} fichier(s) hors des sièges" + (" : " + ", ".join(new) if new else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
