#!/usr/bin/env python3
"""Banc fiable 2.2: the fixes of rounds 1-4 written into the base of known keys, so that a fixed defect seen again is
« [revient] » (and a « banc » one « [banc contredit] »).
    applique-corrections.py [--corrections docs/audits/ultra/corrections.tsv] [--base docs/audits/ultra/base.tsv] [--dry]
corrections.tsv: tour, item, siege (« X/s »), ecran, etat (corrigé | banc | voulu), commit, test, note — one line per
tester's citation. Each citation is resolved to its key by the fusion itself (same key as fuse-reports.py computes for
that seat and screen); the key's state in base.tsv becomes the citation's (the note says the item and the commit). A
citation that resolves to no key is listed, never guessed. Standard library only."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
fuse_mod = __import__("fuse-reports")
DOCS = os.path.join(HERE, "..", "..", "docs", "audits", "ultra")


def main(argv):
    def opt(n, d):
        return argv[argv.index(n) + 1] if n in argv else d
    corrections = opt("--corrections", os.path.join(DOCS, "corrections.tsv"))
    base = opt("--base", os.path.join(DOCS, "base.tsv"))
    rows = fuse_mod.load_tsv(corrections)
    keys_of = {}
    for tour in sorted({r["tour"] for r in rows}):
        clusters = fuse_mod.fuse(os.path.join(DOCS, "sieges", tour), base, None, tour)[0]
        keys_of[tour] = {w: c["key"] for c in clusters for w in c["where"]}
    states, missing = {}, []
    for r in rows:
        key = keys_of.get(r["tour"], {}).get(f"{r['siege']}#{r['ecran']}")
        if key is None:
            missing.append(f"{r['tour']} {r['siege']}#{r['ecran']} ({r['item']})")
            continue
        # A key fixed by one citation and called « banc » by another keeps « corrigé » (the app was changed).
        if states.get(key, ("", ""))[0] != "corrigé":
            states[key] = (r["etat"], f"{r['tour']} {r['item']} {r.get('commit', '-')}".strip())
    lines = fuse_mod.read(base).splitlines()
    done = 0
    for i, line in enumerate(lines):
        parts = line.split("\t")
        if parts and parts[0] in states:
            parts = (parts + ["", "", "", ""])[:max(4, len(parts))]
            parts[1], note = states[parts[0]]
            parts[3] = (parts[3] + " ; " if parts[3] else "") + note
            lines[i] = "\t".join(parts)
            done += 1
    if "--dry" not in argv:
        with open(base, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
    print(f"{len(rows)} citations, {len(states)} clés, {done} lignes de base.tsv changées, {len(missing)} sans clé")
    for m in missing:
        print("  sans clé :", m)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
