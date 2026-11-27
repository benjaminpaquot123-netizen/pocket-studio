#!/usr/bin/env python3
"""Adds table-method measures to the candidates' TSV (docs/sons/fin/REGLE-BATTERIE.md, b).

The reference tables measure decay with a Schroeder curve (scripts/sons/mesure.py); the engine-side
measurer (matin_choix_mesure.cpp) uses a 5 ms RMS envelope. To compare candidates with the
definitive targets, each raw render (<famille>_<style>_<version>.f32, mono float32, 48 kHz) is
measured with mesure.measure and the columns t20_ms, t60_ms and pitch_steady_hz are appended.
Usage: matin_choix_tables.py MESURES.tsv RAW_DIR > MESURES_TABLES.tsv
"""
import csv
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "sons"))
import mesure  # noqa: E402

COLS = ["t20_ms", "t60_ms", "pitch_steady_hz"]


def main(mesures, raw):
    with open(mesures, encoding="utf8") as f:
        rows = list(csv.DictReader(f, delimiter="\t"))
    w = csv.DictWriter(sys.stdout, list(rows[0].keys()) + COLS, delimiter="\t", lineterminator="\n")
    w.writeheader()
    for r in rows:
        x = np.fromfile(os.path.join(raw, "%s_%s_%s.f32" % (r["famille"], r["style"], r["version"])), dtype="<f4")
        m = mesure.measure(48000, x)
        for c in COLS:
            v = m.get(c, float("nan"))
            r[c] = "" if not math.isfinite(v) else "%.2f" % v
        w.writerow(r)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
