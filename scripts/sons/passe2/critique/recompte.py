#!/usr/bin/env python3
"""Independent recount of the deep-pass (passe 2) per-archive tables.

Written for the blind review; shares no code with reunit.py or lance.py.

  python3 -I scripts/sons/passe2/critique/recompte.py TABLES_DIR [TOUT_DIR]

TABLES_DIR holds rarN/rarN_<kind>.csv (extracted from the frozen per-archive heads
with `git show`). TOUT_DIR (optional) is docs/sons/passe2/tout/: each merged table is
compared row by row (by `cle`) with the concatenation of the per-archive tables.
Reads CSV only, never a sound.
"""
import csv
import glob
import math
import os
import sys
from collections import Counter, defaultdict

KINDS = ["drum_oneshot", "drum_loop", "melodic_loop", "oneshot_melodic", "vocal", "fx",
         "kit", "kit_stem", "midi"]


def num(v):
    try:
        x = float(v)
    except (TypeError, ValueError):
        return None
    return x


def load(tables):
    rows = defaultdict(list)  # kind -> rows
    for path in sorted(glob.glob(os.path.join(tables, "rar*", "rar*_*.csv"))):
        base = os.path.basename(path)[:-4]
        kind = base.split("_", 1)[1]
        if kind not in KINDS:
            continue
        with open(path, newline="") as f:
            for r in csv.DictReader(f):
                r["_src"] = base
                rows[kind].append(r)
    return rows


def controls(tables):
    out = []
    for path in sorted(glob.glob(os.path.join(tables, "rar*", "rar*_controle.csv"))):
        with open(path, newline="") as f:
            out.extend(csv.DictReader(f))
    return out


def absurd(kind, r):
    """Return a list of reasons why this row looks wrong."""
    bad = []
    for k, v in r.items():
        if isinstance(v, str) and v.strip().lower() in ("nan", "inf", "-inf"):
            bad.append(f"{k}=NaN/inf")
    d = num(r.get("dur_s"))
    if kind not in ("kit",) and (d is None or d <= 0):
        bad.append(f"dur_s={r.get('dur_s')!r}")
    for col in ("bpm_tag", "bpm_est", "bpm_file"):
        b = num(r.get(col))
        if b is not None and not (40 <= b <= 300):
            bad.append(f"{col}={b}")
    for col in ("pitch_min", "pitch_max"):
        p = num(r.get(col))
        if p is not None and not (0 <= p <= 127):
            bad.append(f"{col}={p}")
    for col in ("f0_hz", "pitch_steady_hz", "pitch_attack_hz"):
        p = num(r.get(col))
        if p is not None and not (20 <= p <= 20000):
            bad.append(f"{col}={p}")
    pk = num(r.get("peak_dbfs"))
    if pk is not None and pk > 0.5:
        bad.append(f"peak_dbfs={pk}")
    rms = num(r.get("rms_dbfs"))
    if pk is not None and rms is not None and rms > pk + 0.01:
        bad.append(f"rms>{'peak'}")
    if (r.get("error") or "").strip():
        bad.append("error=" + r["error"][:40])
    return bad


def main():
    tables = sys.argv[1]
    tout = sys.argv[2] if len(sys.argv) > 2 else None
    rows = load(tables)
    total = sum(len(v) for v in rows.values())
    print(f"total rows (all kinds, kit rows included): {total}")
    print(f"total rows without kit rows: {total - len(rows['kit'])}")
    print("\nby kind:")
    for k in KINDS:
        print(f"  {k:16s} {len(rows[k])}")
    print("\nby kind x rar:")
    for k in KINDS:
        c = Counter(int(r["rar"]) for r in rows[k])
        print(f"  {k:16s} " + " ".join(f"{n}:{c.get(n, 0)}" for n in range(1, 13)))
    print("\nby style x kind:")
    styles = sorted({r["style"] or "(empty)" for k in KINDS for r in rows[k]})
    for s in styles:
        print(f"  {s:12s} " + " ".join(
            f"{k}={sum(1 for r in rows[k] if (r['style'] or '(empty)') == s)}" for k in KINDS))
    # duplicates
    keys = Counter(r["cle"] for k in KINDS for r in rows[k])
    dups = [k for k, n in keys.items() if n > 1]
    print(f"\nduplicate cle across all tables: {len(dups)} {dups[:5]}")
    fid = defaultdict(set)
    for k in KINDS:
        for r in rows[k]:
            if r.get("file_id"):
                fid[r["file_id"]].add(int(r["rar"]))
    cross = {f: s for f, s in fid.items() if len(s) > 1}
    print(f"file_id present in more than one RAR: {len(cross)}")
    fidc = Counter(r["file_id"] for k in KINDS for r in rows[k] if r.get("file_id"))
    print(f"file_id repeated (any place): {sum(1 for n in fidc.values() if n > 1)}")
    # absurd cells
    print("\nabsurd cells:")
    reasons = Counter()
    examples = defaultdict(list)
    for k in KINDS:
        for r in rows[k]:
            for b in absurd(k, r):
                key = (k, b.split("=")[0])
                reasons[key] += 1
                if len(examples[key]) < 3:
                    examples[key].append((r["cle"], b))
    for key, n in reasons.most_common():
        print(f"  {key[0]:16s} {key[1]:16s} {n}  e.g. {examples[key]}")
    # drum families
    fam = Counter()
    for r in rows["drum_oneshot"]:
        f = (r["subtype"] or "").split(":")[0]
        if f.startswith("perc"):
            f = "perc"
        fam[f] += 1
    print("\ndrum families:", dict(fam.most_common()))
    # controls
    ctl = controls(tables)
    sa = sum(int(c["files_audio"]) for c in ctl)
    sm = sum(int(c["files_midi"]) for c in ctl)
    sr = sum(int(c["rows"]) for c in ctl)
    sk = sum(int(c["kit_rows"]) for c in ctl)
    print(f"\ncontrol sums: audio={sa} midi={sm} rows={sr} kit_rows={sk} nested={sum(int(c['nested_archives']) for c in ctl)}")
    per_pack = Counter((r["rar"], r["pack"]) for k in KINDS for r in rows[k])
    for c in ctl:
        want = int(c["rows"]) + int(c["kit_rows"])
        got = per_pack[(c["rar"], c["pack"])]
        if want != got:
            print(f"  MISMATCH rar{c['rar']} {c['pack']}: control {want} tables {got}")
    # styled rows vs control
    for c in ctl:
        if c["style_posed"] in ("", "unlabelled") and not c["style_forced"]:
            continue
        sty = c["style_forced"] or c["style_posed"]
        n = sum(1 for k in KINDS for r in rows[k]
                if r["rar"] == c["rar"] and r["pack"] == c["pack"] and r["style"] == sty)
        print(f"  rar{c['rar']} {c['pack']} style {sty}: rows with that style {n}, "
              f"control rows_pack_styled {c['rows_pack_styled']} rows_forced {c['rows_forced']}")
    if tout:
        print("\nmerged tables vs per-archive tables:")
        for k in KINDS:
            p = os.path.join(tout, k + ".csv")
            with open(p, newline="") as f:
                merged = {r["cle"]: r for r in csv.DictReader(f)}
            mine = {r["cle"]: r for r in rows[k]}
            diff = 0
            for cle, r in mine.items():
                m = merged.get(cle)
                if m is None:
                    diff += 1
                    continue
                for col, v in m.items():
                    if col in r and r[col] != v:
                        diff += 1
                        break
            print(f"  {k:16s} merged={len(merged)} mine={len(mine)} missing_or_diff={diff} "
                  f"extra={len(set(merged) - set(mine))}")


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------------------
# Drum medians with the trust rules of ANALYSE-MINE-OR section 1, re-implemented
# from the prose (not from reunit.py), with the number of rows actually kept.
TRUST_END = {"t20_ms": -35.0, "t40_ms": -50.0, "t60_ms": -60.0}
PITCH_COLS = ("pitch_attack_hz", "pitch_steady_hz", "h2_db", "h3_db")


def kept(r, col):
    v = num(r.get(col))
    if v is None or v != v:
        return None
    if col in TRUST_END:
        end = num(r.get("end_level_db"))
        if end is None or end > TRUST_END[col]:
            return None
    if col in PITCH_COLS:
        t = num(r.get("tonal_db"))
        if t is None or t < -10.0:
            return None
    return v


def drum_medians(tout):
    import statistics
    path = os.path.join(tout, "drum_oneshot.csv")
    with open(path, newline="") as f:
        rows = list(csv.DictReader(f))
    fams = defaultdict(list)
    for r in rows:
        fam = (r["subtype"] or "").split(":")[0]
        fams["perc" if fam.startswith("perc") else fam].append(r)
    cols = ["t20_ms", "t60_ms", "pitch_attack_hz", "pitch_steady_hz", "tonal_db",
            "centroid_hz", "hf_db", "transient_body_db"]
    print("\ndrum medians with trust rules (median / rows kept):")
    for fam, rs in sorted(fams.items(), key=lambda x: -len(x[1])):
        cells = []
        for c in cols:
            xs = [v for v in (kept(r, c) for r in rs) if v is not None]
            cells.append(f"{c}={statistics.median(xs):.4g}/{len(xs)}" if xs else f"{c}=-/0")
        print(f"  {fam:10s} n={len(rs):4d} " + " ".join(cells))


if __name__ == "__main__" and len(sys.argv) > 2:
    drum_medians(sys.argv[2])
