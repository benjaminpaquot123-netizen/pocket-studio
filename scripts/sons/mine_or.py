#!/usr/bin/env python3
"""Pick the drum one-shots out of a sample-pack zip and summarise mesure.py results.

The zip and everything extracted from it are untrusted data: run with
`python3 -I`, paths as arguments, output folders outside the git repository.

    python3 -I scripts/sons/mine_or.py extract <pack.zip> <empty-out-dir> --manifest <file.tsv>
    python3 -I scripts/sons/mine_or.py stats <mesure.csv> [--exclude <file.tsv>]

extract  keeps only drum one-shots, classifies each by family / style /
         sub-type from its folder and file name, and writes it as
         <out>/<family>/<style>/<subtype>/<NNNN>.wav (anonymous name). The
         manifest (local only, never committed) maps NNNN to the zip entry.
         Entries that fail to decompress (bad CRC, truncated) are skipped
         and counted per family.
stats    prints aggregated markdown tables (count, median, quartiles) per
         family, per family x style, per family x sub-type. No file names.

Classification rules only; no numbers are stored here.
"""
import argparse
import collections
import csv
import math
import os
import re
import sys
import zipfile

# pack folder (after the vendor prefix) -> style label
STYLES = [
    ("Acid Techno", "techno"), ("Melodic Techno", "techno"),
    ("Deep House", "house"), ("Afro House", "afro-house"), ("Amapiano", "amapiano"),
    ("Mainstage DnB", "dnb"), ("Liquid DnB", "dnb"),
    ("Deathstep", "dubstep"), ("Riddim Riot", "dubstep"),
    ("Neo Rave", "rave"), ("Keta", "rave"),
    ("Chillhop", "lofi"), ("Downtown Drift", "drift-phonk"),
    ("Drum Hero 6", "multi"),
]

# never drum one-shots
SKIP_PATH = re.compile(r"(?i)loop|construction kit|melody kit|stem|midi|/fx/|whoosh|"
                       r"bass|synth shot|vocal|melod|pads?/|keys|piano|prereverse|riser|impact")
DRUM_PATH = re.compile(r"(?i)/one.?shots?/")

# (family, regex) checked in order, on the file name then on the folder
RULES = [
    ("hat_open", r"open\s*hats?|\boh\b"),
    ("hat_closed", r"closed\s*hats?|\bch\b"),
    ("hat", r"hi.?hats?|\bhats?\b"),
    ("ride", r"\brides?\b"),
    ("crash", r"\bcrash(es)?\b|\bsplash\b"),
    ("cymbal", r"\bcymbals?\b|\bchina\b"),
    ("clap", r"\bclaps?\b"),
    ("rim", r"\brims?\b|rimshot"),
    ("snare", r"\bsnares?\b"),
    ("tom", r"\btoms?\b"),
    ("shaker", r"\bshakers?\b|tambourine"),
    ("snap", r"\bsnaps?\b"),
    ("cowbell", r"cow\s*bells?"),
    ("kick_808", r"\b808"),
    ("kick", r"\bkicks?\b"),
    ("perc", r"\bperc(ussion)?s?\b|conga|bongo|djembe|darbuka|dunun|cajon|agogo|block"),
]
RULES = [(f, re.compile(r, re.I)) for f, r in RULES]

SUBTYPE_DIRS = re.compile(r"(?i)^(acoustic|foley|synth hard|synth soft|lofi|distorted|neuro|hard|soft|"
                          r"gabber|industrial|punchy|raw|tuned|darbuka|djembe|dunun|high|mid|low)")


def style_of(path):
    for key, st in STYLES:
        if key.lower() in path.lower():
            return st
    return None


def family_of(path):
    """(family, subtype) or None when the entry is not a drum one-shot."""
    if not path.lower().endswith((".wav", ".aif", ".aiff")):
        return None
    parts = path.split("/")
    inner = "/" + "/".join(parts[2:])  # below the pack folder: pack names say nothing
    if not DRUM_PATH.search(inner) or SKIP_PATH.search(inner):
        return None
    name = re.sub(r"(?i)\.(wav|aiff?)$", "", parts[-1])
    name = re.sub(r"[_\-]", " ", name)
    folder = parts[-2]
    # bonus kick layers (attack-only / tail-only) are not complete one-shots
    if re.search(r"(?i)\b(attack|tail)\b", name):
        return None
    fam = None
    for f, rx in RULES:
        if rx.search(name):
            fam = f
            break
    if fam is None:
        for f, rx in RULES:
            if rx.search(folder):
                fam = f
                break
    if fam is None:
        return None
    # "Open" alone inside a cymbal/hat folder is an open hat
    if fam in ("cymbal", "hat") and re.search(r"(?i)\bopen\b", name):
        fam = "hat_open"
    if fam == "kick" and re.search(r"(?i)808", folder):
        fam = "kick_808"
    sub = "-"
    for p in parts[-3:-1]:
        m = SUBTYPE_DIRS.match(re.sub(r"(?i)\s*(kicks?|snares?|hats?|toms?|claps?)\b", "", p).strip())
        if m and p.lower() not in ("one shots", "one-shots"):
            sub = re.sub(r"[^a-z0-9]+", "-", p.lower()).strip("-")
    if fam == "clap" and re.search(r"(?i)foley", folder):
        sub = "foley-claps"
    return fam, sub


def cmd_extract(a):
    out = os.path.abspath(a.out)
    os.makedirs(out, exist_ok=True)
    if os.listdir(out):
        sys.exit("output folder must be empty")
    kept = collections.Counter()
    bad = collections.Counter()
    unclassified = collections.Counter()
    n = 0
    with zipfile.ZipFile(a.zip) as z, open(a.manifest, "w", newline="") as man:
        w = csv.writer(man, delimiter="\t")
        for info in z.infolist():
            p = info.filename
            if info.is_dir():
                continue
            r = family_of(p)
            if r is None:
                if (p.lower().endswith(".wav") and DRUM_PATH.search(p)
                        and re.search(r"(?i)drum|kick|snare|clap|hat|perc|cymbal|tom", p)
                        and not SKIP_PATH.search("/" + "/".join(p.split("/")[2:]))):
                    unclassified[p.split("/")[-2]] += 1
                continue
            fam, sub = r
            st = style_of(p) or "unknown"
            n += 1
            rel = os.path.join(fam, st, sub, "%04d.wav" % n)
            try:
                data = z.read(info)
            except Exception as e:  # bad CRC, truncated entry, bad compression
                bad[fam] += 1
                w.writerow(["BAD", fam, st, sub, p, type(e).__name__])
                continue
            dst = os.path.join(out, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "wb") as fh:
                fh.write(data)
            kept[fam] += 1
            w.writerow(["OK", fam, st, sub, p, rel])
    print("kept", sum(kept.values()), dict(sorted(kept.items())))
    print("bad", sum(bad.values()), dict(sorted(bad.items())))
    print("unclassified drum-looking", sum(unclassified.values()), dict(unclassified))


NUM = ["dur_ms", "peak_dbfs", "onset_ms", "t20_ms", "t40_ms", "t60_ms", "end_level_db",
       "mono_loss_db", "pitch_attack_hz", "pitch_steady_hz", "tonal_db", "h2_db", "h3_db",
       "centroid_hz", "hf_db", "transient_body_db"]


def q(vals, p):
    v = sorted(vals)
    if not v:
        return float("nan")
    k = (len(v) - 1) * p
    lo = math.floor(k)
    hi = min(lo + 1, len(v) - 1)
    return v[lo] + (v[hi] - v[lo]) * (k - lo)


def fmt(x):
    if x != x:
        return ""
    a = abs(x)
    return "%.0f" % x if a >= 100 else ("%.1f" % x if a >= 1 else "%.2f" % x)


def load(path, exclude):
    rows = []
    with open(path, newline="") as fh:
        for r in csv.DictReader(fh):
            if r.get("error"):
                continue
            parts = r["folder"].split(os.sep)
            if len(parts) < 3:
                continue
            r["family"], r["style"], r["sub"] = parts[0], parts[1], parts[2]
            r["id"] = r["folder"] + "/" + r["file"]
            if r["id"] in exclude:
                continue
            rows.append(r)
    return rows


# a column is only trusted when the file allows it (see ANALYSE-MINE-OR.md)
def trusted(r, c):
    v = r.get(c)
    if v in ("", None):
        return False
    end = float(r["end_level_db"]) if r.get("end_level_db") else 0.0
    if c == "t20_ms" and end > -35:
        return False
    if c == "t40_ms" and end > -50:
        return False
    if c == "t60_ms" and end > -60:
        return False
    if c in ("pitch_attack_hz", "pitch_steady_hz", "h2_db", "h3_db"):
        if not r.get("tonal_db") or float(r["tonal_db"]) < -10:
            return False
    return True


HIGH = ("hat", "hat_open", "hat_closed", "ride", "crash", "cymbal", "shaker")


def sanity(r):
    """Reason why a file cannot belong to its family, or ''."""
    f, c, t = r["family"], float(r["centroid_hz"]), float(r["tonal_db"] or -99)
    if f in ("kick", "kick_808") and (c > 1500 or t < -10):
        return "kick that is not low and tonal"
    if f in HIGH and c < 2500:
        return "cymbal-type with a centroid under 2.5 kHz"
    if f in ("snare", "clap") and c < 500:
        return "snare/clap with a centroid under 500 Hz"
    if f == "snap" and c < 1000:
        return "snap with a centroid under 1 kHz"
    return ""


def robust_out(rows, cols=("centroid_hz", "t20_ms")):
    """ids more than 3 IQR (in log) away from their family x style median."""
    out = {}
    groups = collections.defaultdict(list)
    for r in rows:
        groups[(r["family"], r["style"])].append(r)
    for g, rs in groups.items():
        if len(rs) < 8:
            continue
        for c in cols:
            v = [math.log(float(r[c])) for r in rs if trusted(r, c) and float(r[c]) > 0]
            if len(v) < 8:
                continue
            m, iqr = q(v, .5), q(v, .75) - q(v, .25)
            for r in rs:
                if trusted(r, c) and float(r[c]) > 0 and abs(math.log(float(r[c])) - m) > 3 * max(iqr, 0.1):
                    out.setdefault(r["id"], "%s far from its group (%s)" % (c, "/".join(g)))
    return out


def table(rows, key, cols):
    groups = collections.defaultdict(list)
    for r in rows:
        groups[key(r)].append(r)
    print("| group | n | " + " | ".join(cols) + " |")
    print("|---|---|" + "---|" * len(cols))
    for g in sorted(groups):
        rs = groups[g]
        cells = []
        for c in cols:
            v = [float(r[c]) for r in rs if trusted(r, c)]
            cells.append("%s [%s–%s]" % (fmt(q(v, .5)), fmt(q(v, .25)), fmt(q(v, .75))) if v else "")
        print("| %s | %d | %s |" % (g, len(rs), " | ".join(cells)))
    print()


def cmd_flags(a):
    rows = load(a.csv, set())
    flags = {r["id"]: sanity(r) for r in rows if sanity(r)}
    for k, v in robust_out(rows).items():
        flags.setdefault(k, v)
    with open(a.out, "w") as fh:
        for k, v in sorted(flags.items()):
            fh.write("%s\t%s\n" % (k, v))
    c = collections.Counter((k.split("/")[0], v.split(" (")[0]) for k, v in flags.items())
    for k, v in sorted(c.items()):
        print(v, *k)
    print("flagged", len(flags), "of", len(rows))


def cmd_stats(a):
    exclude = set()
    if a.exclude:
        with open(a.exclude) as fh:
            exclude = {l.split("\t")[0].strip() for l in fh if l.strip()}
    rows = load(a.csv, exclude)
    cols = a.cols.split(",") if a.cols else NUM
    print("## by family\n")
    table(rows, lambda r: r["family"], cols)
    print("## by family x style\n")
    table(rows, lambda r: r["family"] + " / " + r["style"], cols)
    print("## by family x subtype\n")
    table([r for r in rows if r["sub"] != "-"], lambda r: r["family"] + " / " + r["sub"], cols)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    e = sp.add_parser("extract")
    e.add_argument("zip")
    e.add_argument("out")
    e.add_argument("--manifest", required=True)
    s = sp.add_parser("stats")
    s.add_argument("csv")
    s.add_argument("--exclude")
    s.add_argument("--cols")
    f = sp.add_parser("flags")
    f.add_argument("csv")
    f.add_argument("--out", required=True)
    a = ap.parse_args()
    {"extract": cmd_extract, "stats": cmd_stats, "flags": cmd_flags}[a.cmd](a)


if __name__ == "__main__":
    main()
