#!/usr/bin/env python3
"""Banc fiable 2.2: the fixes of rounds 1-4 written into the base of known keys, so that a fixed defect seen again is
« [revient] » (and a « banc » one « [banc contredit] »).
    applique-corrections.py [--corrections docs/audits/ultra/corrections.tsv] [--base docs/audits/ultra/base.tsv] [--sieges docs/audits/ultra/sieges] [--dry]
PLAN-RESTE 6.1: an optional « cible » column names the card when a screen holds several (a piece of the key's target).
corrections.tsv: tour, item, siege (« X/s »), ecran, etat (corrigé | banc | voulu), commit, test, note — one line per
tester's citation. Each citation is resolved to its key by the fusion itself (same key as fuse-reports.py computes for
that seat and screen); the key's state in base.tsv becomes the citation's, its « corrige_au » column the round of the
citation (a defect « revient ? » only at a LATER round), the note says the item and the commit (once: running it again
changes nothing). A citation that resolves to no key, or to several keys (two cards on one screen; review of session 2:
42 of 345), is listed, never guessed. Priority when two citations meet: corrigé > banc > voulu. Standard library only."""
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
    sieges = opt("--sieges", os.path.join(DOCS, "sieges"))
    rows = fuse_mod.load_tsv(corrections)
    keys_of = {}
    for tour in sorted({r["tour"] for r in rows}):
        clusters = fuse_mod.fuse(os.path.join(sieges, tour), base, None, tour)[0]
        found = {}
        for c in clusters:
            for w in c["where"]:
                found.setdefault(w, set()).add(c["key"])
        keys_of[tour] = found
    rank = {"corrigé": 3, "banc": 2, "voulu": 1}
    states, missing, ambiguous = {}, [], []
    for r in rows:
        keys = keys_of.get(r["tour"], {}).get(f"{r['siege']}#{r['ecran']}", set())
        if not keys:
            missing.append(f"{r['tour']} {r['siege']}#{r['ecran']} ({r['item']})")
            continue
        if len(keys) > 1:
            # PLAN-RESTE 6.1: the citation names its card (column « cible », a piece of the key's target, or the whole
            # key); the key(s) whose target or whole text holds it, case folded, are kept.
            cible = (r.get("cible") or "").strip().casefold()
            if cible:
                narrowed = {k for k in keys if cible in k.casefold() or cible in k.split("|")[1].casefold()}
                if len(narrowed) == 1:
                    keys = narrowed
            if len(keys) > 1:
                ambiguous.append(f"{r['tour']} {r['siege']}#{r['ecran']} ({r['item']}) : {' / '.join(sorted(keys))}")
                continue
        key = next(iter(keys))
        old = states.get(key)
        if old is None or rank.get(r["etat"], 0) > rank.get(old[0], 0):
            states[key] = (r["etat"], f"{r['tour']} {r['item']} {r.get('commit', '-')}".strip(), r["tour"])
    lines = fuse_mod.read(base).splitlines()
    done = 0
    for i, line in enumerate(lines):
        parts = line.split("\t")
        if parts and parts[0] == "cle":
            lines[i] = "cle\tetat\tvu\tnote\tcorrige_au"
        elif parts and parts[0] in states:
            parts = (parts + ["", "", "", "", ""])[:5]
            parts[1], note, parts[4] = states[parts[0]]
            if note not in parts[3]:
                parts[3] = (parts[3] + " ; " if parts[3] else "") + note
            lines[i] = "\t".join(parts)
            done += 1
    if "--dry" not in argv:
        with open(base, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
    print(f"{len(rows)} citations, {len(states)} clés, {done} lignes de base.tsv changées, {len(missing)} sans clé, {len(ambiguous)} ambiguës")
    for m in missing:
        print("  sans clé :", m)
    for m in ambiguous:
        print("  ambiguë (plusieurs fiches sur cet écran : ajouter la cible dans corrections.tsv) :", m)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
