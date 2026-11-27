"""Re-measure every MP3 listed in DIR/results.jsonl with the current base.py
(PS_ESSENTIA_BASE) and the given family targets; rewrites results.jsonl.

Usage: python -I rescore.py --tsv essentia-familles.tsv --out DIR
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dna  # noqa: E402
from make import FAMILY, final_measure  # noqa: E402


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tsv", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    T = dna.load_targets(a.tsv)
    path = os.path.join(a.out, "results.jsonl")
    recs = [json.loads(l) for l in open(path)]
    for r in recs:
        with open(os.path.join(a.out, r["file"]), "rb") as f:
            r.update(final_measure(f.read(), FAMILY[r["famille"]], T))
    with open(path, "w") as f:
        for r in recs:
            f.write(json.dumps(r) + "\n")


if __name__ == "__main__":
    main()
