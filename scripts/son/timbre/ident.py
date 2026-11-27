#!/usr/bin/env python3
"""Find which table row each downloaded sound is, whatever the folder or the arrival order.

Three roads, tried in order, each one checked by the measures of passe2 (mesure.py, the very
code that filled docs/sons/passe2/tout/drum_oneshot.csv):
  cle      the file is named <cle>.<ext> (the flat layout);
  hachage  some folder above the file gives the row's file_id (compare.fid, the archive layout);
  mesures  the nearest row on dur_s, peak, rms, t20, t60 (robust spread, as reference.py).
Each sound gets a method and a confidence: haute, moyenne, or faible (faible = not used).
The search runs over every drum row, so a sound outside the scope finds its own row and is
then left out instead of being taken for an in-scope neighbour.

Run with `python3 -I`. The result holds file paths and keys only, and goes outside the repo.
"""
import csv
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
VERIF = os.path.join(REPO, "scripts", "sons", "passe2", "verif")


def _verif_modules():
    """compare and mesure of passe2/verif, even when scripts/sons/mesure.py is already loaded."""
    other = sys.modules.get("mesure")
    if other is not None and os.path.dirname(os.path.abspath(other.__file__)) != VERIF:
        sys.modules.pop("mesure")
        sys.modules.pop("compare", None)
    else:
        other = None
    sys.path.insert(0, VERIF)
    import compare
    import mesure
    if other is not None:
        sys.modules["mesure"] = other
    return compare, mesure


CMP, M = _verif_modules()

TABLE = os.path.join(REPO, "docs", "sons", "passe2", "tout", "drum_oneshot.csv")
COLS = ("dur_s", "peak_dbfs", "rms_dbfs", "t20_ms", "t60_ms")


def all_rows():
    with open(TABLE, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def vec(row):
    return {c: CMP.fnum(row.get(c)) for c in COLS}


def checks(mine, row):
    """How many of the five columns agree within compare.TOL, out of how many present."""
    ok = n = 0
    for c in COLS:
        a = CMP.agree(c, mine.get(c), CMP.fnum(row.get(c)))
        if a in ("yes", "no"):
            n += 1
            ok += a == "yes"
    return ok, n


def level(ok, n):
    if n >= 4 and ok == n:
        return "haute"
    if n >= 3 and ok >= n - 1:
        return "moyenne"
    return "faible"


class Nearest:
    def __init__(self, rows):
        self.rows = rows
        self.v = [vec(r) for r in rows]
        self.spread = {}
        for c in COLS:
            xs = sorted(x[c] for x in self.v if x[c] is not None)
            m = xs[len(xs) // 2]
            mad = sorted(abs(x - m) for x in xs)[len(xs) // 2]
            self.spread[c] = max(1.4826 * mad, 1e-3)

    def dist(self, a, b):
        s = n = 0
        for c in COLS:
            if a.get(c) is not None and b[c] is not None:
                s += ((a[c] - b[c]) / self.spread[c]) ** 2
                n += 1
        return math.sqrt(s / n) if n >= 3 else float("inf")

    def best(self, mine):
        d = sorted((self.dist(mine, v), i) for i, v in enumerate(self.v))
        return d[0][1], d[0][0], d[1][0]


def mine_of(path):
    try:
        return {c: M.measure(path, "drum_oneshot").get(c) for c in COLS}, ""
    except Exception as e:  # unreadable: counted, never guessed
        return None, type(e).__name__


def by_hash(path, root, ids):
    """file_id from every folder between root's parent and the file."""
    path = os.path.normpath(path)
    top = os.path.dirname(os.path.normpath(root))
    d = os.path.dirname(path)
    while True:
        i = ids.get(CMP.fid(d, os.path.relpath(path, d).replace(os.sep, "/")))
        if i is not None:
            return i
        if d == top or os.path.dirname(d) == d:
            return None
        d = os.path.dirname(d)


def identify(root, rows=None, measure_of=mine_of):
    rows = rows if rows is not None else all_rows()
    by_cle = {r["cle"]: i for i, r in enumerate(rows)}
    by_id = {r["file_id"]: i for i, r in enumerate(rows) if r.get("file_id")}
    near = Nearest(rows)
    audio, _ = CMP.visible_walk(root)
    out = []
    for p in audio:
        mine, err = measure_of(p)
        rec = {"path": p, "cle": None, "methode": "", "confiance": "faible", "detail": err}
        if mine is None:
            out.append(rec)
            continue
        stem = os.path.splitext(os.path.basename(p))[0]
        for meth, i in (("cle", by_cle.get(stem)), ("hachage", by_hash(p, root, by_id))):
            if i is None:
                continue
            ok, n = checks(mine, rows[i])
            if level(ok, n) != "faible":
                rec.update(cle=rows[i]["cle"], methode=meth, confiance=level(ok, n), detail="%d/%d" % (ok, n))
                break
        if rec["cle"] is None:
            i, d1, d2 = near.best(mine)
            ok, n = checks(mine, rows[i])
            ratio = d2 / d1 if d1 > 0 else float("inf")
            conf = ("haute" if ok == n and n >= 4 and ratio >= 3 else
                    "moyenne" if ok >= n - 1 and n >= 3 and ratio >= 1.5 else "faible")
            rec.update(cle=rows[i]["cle"] if conf != "faible" else None, methode="mesures", confiance=conf,
                       detail="%d/%d d1=%.3g d2/d1=%.3g" % (ok, n, d1, min(ratio, 1e9)))
        out.append(rec)
    # A key claimed twice keeps its best claim; the others drop to faible.
    rank = {"haute": 2, "moyenne": 1, "faible": 0}
    order = {"cle": 2, "hachage": 1, "mesures": 0}
    best = {}
    for k, r in enumerate(out):
        if r["cle"]:
            key = (rank[r["confiance"]], order[r["methode"]])
            if r["cle"] not in best or key > best[r["cle"]][0]:
                best[r["cle"]] = (key, k)
    for k, r in enumerate(out):
        if r["cle"] and best[r["cle"]][1] != k:
            r.update(cle=None, confiance="faible", detail=r["detail"] + " doublon")
    return out


def main(argv):
    import json
    root, dst = argv[0], argv[1]
    res = identify(root)
    with open(dst, "a") as f:
        for r in res:
            f.write(json.dumps(r) + "\n")
    c = {}
    for r in res:
        c[(r["methode"], r["confiance"])] = c.get((r["methode"], r["confiance"]), 0) + 1
    print("%d sons: %s" % (len(res), ", ".join("%s/%s=%d" % (k[0] or "illisible", k[1], v) for k, v in sorted(c.items()))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
