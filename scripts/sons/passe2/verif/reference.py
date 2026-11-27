#!/usr/bin/env python3
"""Listening references: for each pack and drum family, the 2 sounds closest
to the style median; plus 2 drum loops per pack at the style's label tempo.

  python3 -I reference.py EXTRACTED_DIR --rar N --tables docs/sons/passe2/tout --out OUT_DIR_OUTSIDE_REPO

The style median covers every row of that style and family, all RARs
together (from the tables). Distance: mean of |value - median| / robust
spread over dur_s, peak_dbfs, rms_dbfs, t20_ms, t60_ms (+ pitch_steady_hz in
semitones for kicks). MP3 192 kb/s, level unchanged, neutral names
ref/<style>/<family>_rarN_pN_<rank>.mp3, and OUT_DIR/refs.csv. Nothing here
goes into the repository.
"""
import argparse
import collections
import csv
import math
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import compare  # noqa: E402

FAMILY = {"kick": "kick", "kick_808": "808", "snare": "snare", "clap": "clap", "hat_closed": "hat_ferme",
          "hat_open": "hat_ouvert", "crash": "crash", "rim": "rim", "tom": "tom", "shaker": "shaker",
          "cowbell": "cowbell", "conga": "conga", "perc": "percussion", "perc_low": "percussion",
          "perc_mid": "percussion", "perc_high": "percussion"}
DIST_COLS = ("dur_s", "peak_dbfs", "rms_dbfs", "t20_ms", "t60_ms")


def family_of(row):
    return FAMILY.get((row.get("subtype") or "").split(":")[0])


def feats(row, fam):
    v = {c: compare.fnum(row.get(c)) for c in DIST_COLS}
    if fam == "kick":
        p = compare.fnum(row.get("pitch_steady_hz"))
        v["pitch_st"] = 12 * math.log2(p) if p and p > 0 else None
    return v


def median(xs):
    xs = sorted(xs)
    n = len(xs)
    return None if not n else (xs[n // 2] if n % 2 else 0.5 * (xs[n // 2 - 1] + xs[n // 2]))


def stats(values):
    """median and robust spread (1.4826 * MAD, floor at a small value)."""
    xs = [x for x in values if x is not None]
    m = median(xs)
    if m is None:
        return None
    mad = median([abs(x - m) for x in xs]) or 0.0
    return m, max(1.4826 * mad, 1e-3)


def all_rows(tables, typ):
    p = os.path.join(tables, typ + ".csv")
    if not os.path.exists(p):
        return []
    with open(p, newline="", encoding="utf-8") as fh:
        return list(csv.DictReader(fh))


def mp3(src, dst, seconds=None):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    cmd = ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-i", src]
    if seconds:
        cmd += ["-t", "%.3f" % seconds]
    cmd += ["-map_metadata", "-1", "-codec:a", "libmp3lame", "-b:a", "192k", dst]
    return subprocess.run(cmd, check=False).returncode == 0


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("folder")
    ap.add_argument("--rar", type=int, required=True)
    ap.add_argument("--tables", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args(argv)
    out = os.path.abspath(a.out)
    repo = os.path.abspath(os.path.join(HERE, "..", "..", "..", ".."))
    if out == repo or out.startswith(repo + os.sep):
        print("refusing: --out must be outside the repository")
        return 2
    shots = all_rows(a.tables, "drum_oneshot")
    loops = all_rows(a.tables, "drum_loop")
    # style x family medians, all RARs
    groups = collections.defaultdict(list)
    for r in shots:
        fam = family_of(r)
        if fam and not r.get("error"):
            groups[(r.get("style", ""), fam)].append(feats(r, fam))
    med = {}
    for key, fl in groups.items():
        med[key] = {c: stats([f.get(c) for f in fl]) for c in fl[0]}
    tempo = {}
    by_style = collections.defaultdict(list)
    for r in loops:
        b = compare.fnum(r.get("bpm_tag"))
        if b:
            by_style[r.get("style", "")].append(b)
    for s, bs in by_style.items():
        tempo[s] = collections.Counter(bs).most_common(1)[0][0]
    # this RAR, its files
    mine = [r for r in shots + loops if r.get("rar") == str(a.rar)]
    for r in shots:
        r["_table"] = "drum_oneshot"
    for r in loops:
        r["_table"] = "drum_loop"
    by_pack = collections.defaultdict(list)
    for r in compare.load_rows(a.tables, a.rar):
        by_pack[r["pack"]].append(r)
    dirs = compare.find_pack_dirs(a.folder, by_pack)
    files = {}
    for pk, folder in dirs.items():
        au, mi = compare.visible_walk(folder)
        for p in au:
            files[(pk, compare.fid(folder, os.path.relpath(p, folder).replace(os.sep, "/")))] = p
    picks = []
    cand = collections.defaultdict(list)
    for r in mine:
        p = files.get((r["pack"], r["file_id"]))
        if p is None or r.get("error"):
            continue
        style = r.get("style", "")
        if r["_table"] == "drum_oneshot":
            fam = family_of(r)
            m = med.get((style, fam)) if fam else None
            if not m:
                continue
            f = feats(r, fam)
            parts = [abs(f[c] - m[c][0]) / m[c][1] for c in m if m[c] and f.get(c) is not None]
            if not parts:
                continue
            cand[(r["pack"], style, fam)].append((sum(parts) / len(parts), r, p))
        else:
            b = compare.fnum(r.get("bpm_tag"))
            t = tempo.get(style)
            if b and t:
                cand[(r["pack"], style, "boucle")].append((abs(b - t), r, p))
    for (pk, style, fam), lst in sorted(cand.items()):
        lst.sort(key=lambda z: z[0])
        for rank, (d, r, p) in enumerate(lst[:2], 1):
            name = "ref/%s/%s_rar%d_%s_%d.mp3" % (style or "sans-style", fam, a.rar, pk, rank)
            sec = None
            if fam == "boucle":
                sec = 8 * 4 * 60.0 / compare.fnum(r["bpm_tag"])
            if mp3(p, os.path.join(out, name), sec):
                picks.append((name, r["cle"], fam, style, "%.3f" % d,
                              ";".join("%s=%s" % (c, r.get(c, "")) for c in DIST_COLS + ("pitch_steady_hz", "bpm_tag")
                                       if r.get(c))))
    new = not os.path.exists(os.path.join(out, "refs.csv"))
    with open(os.path.join(out, "refs.csv"), "a", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        if new:
            w.writerow(["fichier", "cle", "famille", "style", "distance", "mesures"])
        w.writerows(picks)
    print("rar %d: %d references in %s" % (a.rar, len(picks), out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
