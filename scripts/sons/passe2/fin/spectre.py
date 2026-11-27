#!/usr/bin/env python3
"""Blind check of centroid_hz and transient_body_db on one archive's drum hits,
written from the definitions in the docstring of scripts/sons/mesure.py (own code,
not copied from it).

  python3 -I spectre.py EXTRACTED_DIR TABLES_DIR RAR [N] [SEED]

Same file matching as critique/remesure.py (file_id = sha1("pack/relative path")).
Checks (1) the drum hits of the 40-sound sample drawn exactly as remesure.py draws
it with the same N and SEED, and (2) every drum hit of the archive.
Prints only table keys and numbers, never a file or folder name.

Tolerances (written before the run):
  centroid_hz        |gap| <= 10 % of the table value
  transient_body_db  |gap| <= 1.0 dB
"""
import csv
import os
import random
import sys

import numpy as np
import soundfile as sf

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "critique"))
import remesure  # noqa: E402  (index_files, TYPES_AUDIO, fnum)


def mine(x, sr):
    """centroid (Hz) and transient/body (dB) per the mesure.py definitions."""
    pw = (x ** 2).mean(axis=1)                       # power, mean over channels
    amp = np.abs(x).max(axis=1)
    peak = amp.max()
    on = int(np.flatnonzero(amp >= peak * 10 ** (-30 / 20))[0])   # -30 dB onset
    rest = np.cumsum(pw[::-1])[::-1]                 # energy still to come
    hit = np.flatnonzero(rest[on:] < rest[on] * 1e-6)              # -60 dB point
    end = on + int(hit[0]) if len(hit) else len(pw)
    k = on + int(round(0.010 * sr))
    tb = 10 * np.log10(pw[on:k].sum() / pw[k:].sum()) if pw[k:].sum() > 0 else None
    a = max(0, on - int(0.001 * sr))
    seg = x[a:max(end, a + 16)]
    P = np.zeros(len(seg) // 2 + 1)
    for c in range(seg.shape[1]):                    # channel-summed power spectrum, no taper
        P += np.abs(np.fft.rfft(seg[:, c])) ** 2
    f = np.arange(len(P)) * sr / len(seg)
    b = (f >= 20) & (f <= 20000)
    cen = float((f[b] * P[b]).sum() / P[b].sum()) if P[b].sum() > 0 else None
    return cen, tb


def main():
    root, tables, rar = sys.argv[1], sys.argv[2], sys.argv[3]
    n_total = int(sys.argv[4]) if len(sys.argv) > 4 else 40
    seed = int(sys.argv[5]) if len(sys.argv) > 5 else 20261126
    files = remesure.index_files(root)
    rng = random.Random(seed)
    sample = []
    for typ, k in remesure.TYPES_AUDIO.items():      # same draw as remesure.py
        rows = [r for r in csv.DictReader(open(os.path.join(tables, "rar%s_%s.csv" % (rar, typ)), newline=""))
                if not r["error"]]
        rng.shuffle(rows)
        sample += [(typ, r) for r in rows[:k]]
    sample = [r for t, r in sample[:n_total] if t == "drum_oneshot"]
    every = [r for r in csv.DictReader(open(os.path.join(tables, "rar%s_drum_oneshot.csv" % rar), newline=""))
             if not r["error"]]
    for label, rows in (("sample", sample), ("all_drum_hits", every)):
        ok_c = ok_t = n_c = n_t = miss = 0
        gaps_c, gaps_t = [], []
        for r in rows:
            p = files.get(r["file_id"])
            if not p:
                miss += 1
                continue
            x, sr = sf.read(p, always_2d=True, dtype="float64")
            c, t = mine(x, sr)
            rc, rt = remesure.fnum(r["centroid_hz"]), remesure.fnum(r["transient_body_db"])
            if c is not None and rc:
                n_c += 1
                g = abs(c - rc) / rc
                gaps_c.append(g)
                ok_c += g <= 0.10
            if t is not None and rt is not None:
                n_t += 1
                gaps_t.append(abs(t - rt))
                ok_t += abs(t - rt) <= 1.0
            if label == "sample":
                print("%s | centroid %s/%s | transient_body %s/%s" % (
                    r["cle"], "%.0f" % c if c else "-", r["centroid_hz"], "%.2f" % t if t is not None else "-",
                    r["transient_body_db"]))
        print("%s: unmatched %d ; centroid_hz %d / %d within 10 %% (max gap %.1f %%) ; "
              "transient_body_db %d / %d within 1 dB (max gap %.2f dB)" % (
                  label, miss, ok_c, n_c, 100 * max(gaps_c or [0]), ok_t, n_t, max(gaps_t or [0])))


if __name__ == "__main__":
    main()
