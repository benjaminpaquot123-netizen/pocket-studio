#!/usr/bin/env python3
"""Seat exit detection (PLAN-RESTE 4.2): a tester (an AI agent) may write files outside its own seat folder.

  sortie-de-siege.py avant <lot> [--depot DIR]   # before the lot: lists the lot's files (<lot>/.avant-lot.txt) and
                                                 # the repository's changed files (<lot>/.avant-depot.txt)
  sortie-de-siege.py apres <lot> [--depot DIR]   # after: new files outside <lot>/<a-f>/ and outside the white list,
                                                 # and files of the repository new or changed outside the lot

Audit « banc crédible » 2, P7: the white list holds at the lot's root only (a pattern without « / » never matches a
file of a sub-folder: g/rapport.md is foreign), and the repository (--depot, this script's own by default) is watched:
« dépôt : <path> » for a file new or changed there, outside the lot. A seat writing in another seat's folder stays
invisible while the seats share one user (the seat folders are theirs).

Each foreign file is a line of <lot>/incidents-lot.tsv (at the lot's level: never pinned on a seat, nobody can tell
which one wrote it) and <lot>/lot.json gets « fichiers_hors_lot ». fuse-reports.py adds these lines to the round's
incidents-banc.tsv. Standard library only.
"""
import fnmatch
import hashlib
import json
import os
import subprocess
import sys

SEATS = set("abcdef")
WHITE = ["selftest-*.txt", "selftest*.log", "selftest.ok", "bench.log", "*.tsv", "*.md", "lot.json",
         ".avant-lot.txt", ".avant-depot.txt", "junit/*", "briefs/*"]


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
    return not any(fnmatch.fnmatch(rel, w) and ("/" in w or "/" not in rel) for w in WHITE)


def repo_state(depot, lot):
    """{path: sha256 of its content} of the repository's files that git reports new or changed, outside the lot."""
    try:
        out = subprocess.run(["git", "-C", depot, "status", "--porcelain", "--untracked-files=all", "-z"],
                             capture_output=True, timeout=300, check=True).stdout.decode("utf-8", "replace")
    except (subprocess.SubprocessError, OSError):
        return {}
    lot_rel = os.path.relpath(os.path.realpath(lot), os.path.realpath(depot)).replace(os.sep, "/") + "/"
    state = {}
    for entry in out.split("\0"):
        path = entry[3:]
        if len(entry) < 4 or (path + "/").startswith(lot_rel) or path.startswith(lot_rel):
            continue
        full = os.path.join(depot, path)
        if os.path.isfile(full):
            with open(full, "rb") as f:
                state[path] = hashlib.sha256(f.read()).hexdigest()
        else:
            state[path] = "-"
    return state


def main(argv):
    depot = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    if len(argv) == 4 and argv[2] == "--depot":
        depot, argv = argv[3], argv[:2]
    if len(argv) != 2 or argv[0] not in ("avant", "apres"):
        print(__doc__)
        return 2
    step, lot = argv
    listing = os.path.join(lot, ".avant-lot.txt")
    repo_listing = os.path.join(lot, ".avant-depot.txt")
    if step == "avant":
        with open(listing, "w", encoding="utf-8") as f:
            f.write("\n".join(sorted(files(lot))) + "\n")
        with open(repo_listing, "w", encoding="utf-8") as f:
            json.dump(repo_state(depot, lot), f, ensure_ascii=False)
        return 0
    before = set(open(listing, encoding="utf-8").read().split()) if os.path.isfile(listing) else set()
    new = sorted(r for r in files(lot) - before if foreign(r))
    repo_before = json.load(open(repo_listing, encoding="utf-8")) if os.path.isfile(repo_listing) else {}
    new = sorted([f"dépôt : {p}" for p, h in repo_state(depot, lot).items() if repo_before.get(p) != h] + new)
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
