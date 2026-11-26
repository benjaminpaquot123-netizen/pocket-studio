#!/usr/bin/env python3
"""Blind re-measurement of a random sample of one archive, with code written
independently of scripts/sons/tout.py (numpy/scipy only).

  python3 -I remesure.py EXTRACTED_DIR TABLES_DIR RAR [N] [SEED]

EXTRACTED_DIR holds the pack folders (one level down). Rows are matched to
files through the table's file_id (sha1 of "pack/relative path", first 10 hex
digits: the id scheme is documented, the measurements are not shared).
Prints only table ids and numbers, never a file or folder name.

Tolerances (written before the run):
  dur_s        |gap| <= 0.01 s
  peak_dbfs    |gap| <= 0.5 dB   (sample peak, all channels)
  rms_dbfs     |gap| <= 0.5 dB   (whole file, all channels pooled)
  f0 / pitch   |gap| <= 50 cents (steady part of drum hits, whole note of melodic one-shots)
  bpm_est      |gap| <= 2 %      ; half/double counted apart
  key_est      same tonic and mode ; relative major/minor counted apart
"""
import csv
import hashlib
import math
import os
import random
import sys

import numpy as np
import soundfile as sf
from scipy import signal

AUDIO_EXT = (".wav", ".aif", ".aiff", ".flac", ".mp3", ".ogg")
TYPES_AUDIO = {"drum_oneshot": 12, "drum_loop": 8, "melodic_loop": 8, "oneshot_melodic": 7, "fx": 5}
NOTES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
KS_MAJ = np.array([6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88])
KS_MIN = np.array([6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17])


def index_files(root):
    out = {}
    for pack in sorted(os.listdir(root)):
        pdir = os.path.join(root, pack)
        if not os.path.isdir(pdir):
            continue
        for dp, _, fs in os.walk(pdir):
            for f in fs:
                rel = os.path.relpath(os.path.join(dp, f), pdir).replace(os.sep, "/")
                fid = hashlib.sha1((pack + "/" + rel).encode("utf-8", "surrogateescape")).hexdigest()[:10]
                out[fid] = os.path.join(dp, f)
    return out


def db(x):
    return 20 * math.log10(x) if x > 0 else float("-inf")


def f0_fft(x, sr, lo=20.0, hi=2000.0):
    """Strongest spectral peak with harmonic-sum check (picks the fundamental, not h2)."""
    if len(x) < 2048:
        x = np.pad(x, (0, 2048 - len(x)))
    n = 1 << int(math.ceil(math.log2(max(len(x), 1 << 15))))
    w = np.hanning(len(x))
    s = np.abs(np.fft.rfft(x * w, n))
    fr = np.fft.rfftfreq(n, 1 / sr)
    band = (fr >= lo) & (fr <= hi)
    best, bf = -1.0, None
    for i in np.flatnonzero(band):
        if s[i] < s.max() * 0.05:
            continue
        sc = s[i] + 0.5 * s[min(2 * i, len(s) - 1)] + 0.33 * s[min(3 * i, len(s) - 1)]
        if sc > best:
            best, bf = sc, i
    if bf is None:
        return None
    # parabolic interpolation
    if 0 < bf < len(s) - 1:
        a, b, c = np.log(s[bf - 1:bf + 2] + 1e-12)
        p = 0.5 * (a - c) / (a - 2 * b + c) if (a - 2 * b + c) != 0 else 0
        return (bf + p) * sr / n
    return fr[bf]


def bpm_est(m, sr):
    hop = 512
    f, t, z = signal.stft(m, sr, nperseg=2048, noverlap=2048 - hop)
    mag = np.abs(z)
    flux = np.maximum(np.diff(np.log1p(mag * 100), axis=1), 0).sum(axis=0)
    flux = flux - flux.mean()
    ac = np.correlate(flux, flux, "full")[len(flux) - 1:]
    fps = sr / hop
    best, bb = -1, None
    for bpm in np.arange(70, 180.01, 0.25):
        lag = 60 * fps / bpm
        sc = sum(np.interp(k * lag, np.arange(len(ac)), ac) for k in (1, 2, 4) if k * lag < len(ac))
        if sc > best:
            best, bb = sc, bpm
    return bb


def key_est(m, sr):
    n = 1 << 14
    f, t, z = signal.stft(m, sr, nperseg=n, noverlap=n // 2)
    mag = np.abs(z).mean(axis=1)
    chroma = np.zeros(12)
    for fr, a in zip(f, mag):
        if 55 <= fr <= 2000:
            chroma[int(round(12 * math.log2(fr / 440.0) + 69)) % 12] += a
    best, bk = -2, None
    for k in range(12):
        for mode, prof in (("maj", KS_MAJ), ("min", KS_MIN)):
            r = np.corrcoef(chroma, np.roll(prof, k))[0, 1]
            if r > best:
                best, bk = r, (k, mode)
    return bk


def parse_key(s):
    """'A minor', 'D major', 'Am', 'D' (no mode: None mode) -> (pitch class, mode)."""
    if not s:
        return None
    s = s.strip().replace("\u266f", "#").replace("\u266d", "b")
    parts = s.split()
    root, mode = parts[0], None
    if len(parts) > 1:
        mode = "min" if parts[1].lower().startswith("min") else "maj"
    elif root.endswith("m") and len(root) > 1:
        root, mode = root[:-1], "min"
    flats = {"Db": "C#", "Eb": "D#", "Gb": "F#", "Ab": "G#", "Bb": "A#", "Cb": "B", "Fb": "E"}
    root = flats.get(root, root)
    return (NOTES.index(root), mode) if root in NOTES else None


def cents(a, b):
    return abs(1200 * math.log2(a / b))


def fnum(v):
    try:
        x = float(v)
        return x if math.isfinite(x) else None
    except (TypeError, ValueError):
        return None


def main():
    root, tables, rar = sys.argv[1], sys.argv[2], sys.argv[3]
    n_total = int(sys.argv[4]) if len(sys.argv) > 4 else 40
    seed = int(sys.argv[5]) if len(sys.argv) > 5 else 20261126
    files = index_files(root)
    rng = random.Random(seed)
    sample = []
    for typ, k in TYPES_AUDIO.items():
        p = os.path.join(tables, "rar%s_%s.csv" % (rar, typ))
        rows = [r for r in csv.DictReader(open(p, newline="")) if not r["error"]]
        rng.shuffle(rows)
        sample += [(typ, r) for r in rows[:k]]
    sample = sample[:n_total]
    tally = {}

    def check(name, ok):
        if ok is None:
            return "  -  "
        a = tally.setdefault(name, [0, 0])
        a[1] += 1
        a[0] += bool(ok)
        return " ok  " if ok else " NO  "

    print("cle | type/subtype | dur | peak | rms | pitch | bpm | key")
    unmatched = 0
    for typ, r in sample:
        path = files.get(r["file_id"])
        if not path:
            unmatched += 1
            print(r["cle"], "UNMATCHED")
            continue
        x, sr = sf.read(path, always_2d=True, dtype="float64")
        m = x.mean(axis=1)
        dur = len(x) / sr
        pk = db(np.abs(x).max())
        rms = db(math.sqrt(np.mean(x ** 2)))
        line = [r["cle"], "%s/%s" % (typ, r["subtype"])]
        line.append(check("dur_s", abs(dur - fnum(r["dur_s"])) <= 0.01) + "%.3f/%s" % (dur, r["dur_s"]))
        tp = fnum(r["peak_dbfs"])
        line.append(check("peak_dbfs", tp is not None and abs(pk - tp) <= 0.5) + "%.2f/%s" % (pk, r["peak_dbfs"]))
        tr = fnum(r["rms_dbfs"])
        line.append(check("rms_dbfs", tr is not None and abs(rms - tr) <= 0.5) + "%.2f/%s" % (rms, r["rms_dbfs"]))
        pitch = "-"
        if typ == "drum_oneshot":
            ref = fnum(r["pitch_steady_hz"])
            tonal = fnum(r["tonal_db"])
            if ref and tonal is not None and tonal >= -10:
                on = int(np.argmax(np.abs(m) > 0.1 * np.abs(m).max()))
                seg = m[on + int(0.05 * sr): on + int(0.4 * sr)]
                mine = f0_fft(seg, sr, 20, 1000) if len(seg) > 256 else None
                pitch = check("pitch_steady_hz", mine is not None and cents(mine, ref) <= 50) + \
                    "%s/%.1f" % ("%.1f" % mine if mine else "none", ref)
        elif typ == "oneshot_melodic":
            ref = fnum(r["f0_hz"])
            if ref:
                on = int(np.argmax(np.abs(m) > 0.1 * np.abs(m).max()))
                seg = m[on + int(0.03 * sr): on + int(1.0 * sr)]
                mine = f0_fft(seg, sr, 25, 2000)
                pitch = check("f0_hz", mine is not None and cents(mine, ref) <= 50) + \
                    "%s/%.1f" % ("%.1f" % mine if mine else "none", ref)
        line.append(pitch)
        bpmc = "-"
        if typ in ("drum_loop", "melodic_loop") and fnum(r["bpm_est"]):
            mine = bpm_est(m, sr)
            ref = fnum(r["bpm_est"])
            g = abs(mine - ref) / ref
            hd = min(abs(mine * 2 - ref), abs(mine / 2 - ref)) / ref <= 0.02
            ok = g <= 0.02
            if not ok and hd:
                tally.setdefault("bpm_half_double", [0, 0])[0] += 1
            bpmc = check("bpm_est", ok) + "%.1f/%.1f" % (mine, ref)
            tag = fnum(r["bpm_tag"])
            if tag:
                check("bpm_mine_vs_tag", abs(mine - tag) / tag <= 0.02)
        line.append(bpmc)
        keyc = "-"
        if typ == "melodic_loop" and r["key_est"]:
            mine = key_est(m, sr)
            ref = parse_key(r["key_est"])
            if ref:
                ok = mine == ref
                rel = (mine[1] != ref[1]) and ((mine[0] - ref[0]) % 12 in (3, 9))
                if not ok and rel:
                    tally.setdefault("key_relative", [0, 0])[0] += 1
                keyc = check("key_est", ok) + "%s%s/%s" % (NOTES[mine[0]], "m" if mine[1] == "min" else "", r["key_est"])
                tag = parse_key(r["key_tag"])
                if tag:
                    check("key_mine_vs_tag", mine == tag if tag[1] else mine[0] == tag[0])
        line.append(keyc)
        print(" | ".join(line))
    print("\nunmatched rows:", unmatched, "of", len(sample))
    print("agreement per measure (ok / checked):")
    for k, (a, b) in sorted(tally.items()):
        print("  %-18s %3d / %3d  %s" % (k, a, b, ("%.0f %%" % (100 * a / b)) if b else ""))


if __name__ == "__main__":
    main()
