#!/usr/bin/env python3
"""Compare the sound tables of one RAR with mesure.py, file by file.

  python3 -I compare.py EXTRACTED_DIR --rar N --tables docs/sons/passe2/tout --out OUT_DIR

EXTRACTED_DIR: the RAR extracted (unrar x) in a new, empty folder outside the
repository. Packs are found by their file ids (sha1 of "folder/relative path",
first 10 hex digits): the folder whose ids match the pack's rows is the pack.
Writes OUT_DIR/rarN.csv (table key, type, per column table / mine / ok) and
OUT_DIR/rarN-resume.md. Never a file or folder name in either.
"""
import argparse
import collections
import csv
import hashlib
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesure  # noqa: E402

TYPES = ("drum_oneshot", "drum_loop", "melodic_loop", "oneshot_melodic", "vocal", "fx", "kit_stem", "kit", "midi")
MIDI_EXTS = (".mid", ".midi")
COLS = ("dur_s", "peak_dbfs", "rms_dbfs", "t20_ms", "t60_ms", "centroid_hz", "hf_db", "transient_body_db",
        "tonal_db", "pitch_steady_hz", "pitch_attack_hz", "f0_hz")
DRUM_COLS = COLS[3:11]
TOL = {"dur_s": ("abs", 0.01), "peak_dbfs": ("abs", 0.1), "rms_dbfs": ("abs", 0.2),
       "t20_ms": ("relms", 0.10), "t60_ms": ("relms", 0.10), "centroid_hz": ("rel", 0.10),
       "hf_db": ("abs", 1.5), "transient_body_db": ("abs", 1.0), "tonal_db": ("abs", 1.5),
       "pitch_steady_hz": ("cents", 100), "pitch_attack_hz": ("cents", 200), "f0_hz": ("cents", 50)}
NOISY = {"hat_closed", "hat_open", "crash", "ride", "cymbal", "shaker", "snare", "clap", "snap"}


def fnum(v):
    try:
        x = float(v)
        return x if math.isfinite(x) else None
    except (TypeError, ValueError):
        return None


def agree(col, mine, table):
    """'yes', 'no', 'one-empty' or '' (both empty)."""
    if mine is None and table is None:
        return ""
    if mine is None or table is None:
        return "one-empty"
    kind, tol = TOL[col]
    if kind == "abs":
        ok = abs(mine - table) <= tol
    elif kind == "rel":
        ok = abs(mine - table) <= tol * abs(table)
    elif kind == "relms":
        ok = abs(mine - table) <= max(5.0, tol * abs(table))
    else:
        ok = mine > 0 and table > 0 and abs(1200 * math.log2(mine / table)) <= tol
        if not ok and col == "f0_hz" and mine > 0 and table > 0:
            r = abs(1200 * math.log2(mine / table))
            if abs(r - 1200) <= tol or abs(r - 2400) <= tol:
                return "octave"
    return "yes" if ok else "no"


# ---------------------------------------------------------------- files and packs

def visible_walk(folder):
    """(audio, midi) paths, hidden files and folders skipped."""
    audio, midi = [], []
    for root, dirs, files in os.walk(folder, followlinks=False):
        dirs[:] = sorted(d for d in dirs if not d.startswith(".") and d != "__MACOSX")
        for n in sorted(files):
            if n.startswith("."):
                continue
            low = n.lower()
            p = os.path.join(root, n)
            if os.path.islink(p):
                continue
            if low.endswith(mesure.AUDIO_EXTS):
                audio.append(p)
            elif low.endswith(MIDI_EXTS):
                midi.append(p)
    return audio, midi


def fid(folder, rel):
    s = os.path.basename(os.path.normpath(folder)) + "/" + rel
    return hashlib.sha1(s.encode("utf-8", "surrogateescape")).hexdigest()[:10]


def ids_under(folder):
    """file_id -> path for every file, and every folder above a file (kit rows)."""
    out = {}
    audio, midi = visible_walk(folder)
    for p in audio + midi:
        rel = os.path.relpath(p, folder).replace(os.sep, "/")
        parts = rel.split("/")
        for k in range(1, len(parts) + 1):
            out.setdefault(fid(folder, "/".join(parts[:k])), os.path.join(folder, *parts[:k]))
    return out


def find_pack_dirs(root, rows_by_pack):
    """pack -> folder whose ids match most of the pack's rows (any depth).

    One walk: each file is hashed against every folder above it, so a pack
    folder is found wherever the extraction put it."""
    want = {pk: {r["file_id"] for r in rows} for pk, rows in rows_by_pack.items()}
    owner = {}
    for pk, w in want.items():
        for i in w:
            owner.setdefault(i, set()).add(pk)
    hits = collections.Counter()
    root = os.path.normpath(root)
    audio, midi = visible_walk(root)
    for p in audio + midi:
        rel = os.path.relpath(p, root).replace(os.sep, "/").split("/")
        for k in range(1, len(rel)):
            folder = os.path.join(root, *rel[:k])
            for j in range(k + 1, len(rel) + 1):  # the file and the folders between (kit rows)
                for pk in owner.get(fid(folder, "/".join(rel[k:j])), ()):
                    hits[(pk, folder)] += 1
    best = {}
    for (pk, folder), n in hits.items():
        if n > best.get(pk, (0, ""))[0]:
            best[pk] = (n, folder)
    return {pk: v[1] for pk, v in best.items()}


# ---------------------------------------------------------------- my own reading of the labels

DRUM_WORDS = [("kick_808", r"808"), ("kick", r"kick|kik|\bbd\b|bassdrum"), ("snare", r"snare|\bsd\b|snr"),
              ("clap", r"clap"), ("hat_open", r"open\s*-?_?\s*h|\boh\b|openhat"),
              ("hat_closed", r"hat|\bhh\b|\bch\b"), ("crash", r"crash"), ("ride", r"ride"),
              ("cymbal", r"cymbal|cym\b|china|splash"), ("rim", r"rim"), ("tom", r"\btoms?\b|tom[\s_-]?\d|floor\s*tom"),
              ("shaker", r"shak|tamb"), ("cowbell", r"cowbell|bell\b"), ("snap", r"snap|click\b"),
              ("perc", r"perc|conga|bongo|clave|wood|block|timb")]
STYLE_WORDS = [("afro-house", r"afro"), ("amapiano", r"amapiano|piano\s*log"), ("dnb", r"\bdnb\b|drum\s*[n&']+\s*bass|jungle"),
               ("drift-phonk", r"phonk|drift"), ("dubstep", r"dubstep|riddim"), ("techno", r"techno"),
               ("house", r"house"), ("rave", r"\brave"), ("lofi", r"lo-?fi"), ("hardstyle", r"hardstyle"),
               ("edm", r"\bedm\b|big\s*room"), ("trap", r"\btrap"), ("hiphop", r"hip\s*-?hop|boom\s*bap"),
               ("disco", r"disco"), ("funk", r"\bfunk"), ("ambient", r"ambient")]


def read_label(rel):
    """(type, subtype, style) from the path inside the pack, my own rules."""
    low = rel.lower().replace("_", " ")
    name = low.rsplit("/", 1)[-1]
    if low.endswith(MIDI_EXTS):
        typ = "midi"
    else:
        loop = bool(re.search(r"loop|\d{2,3}\s*bpm|\bbpm\b", low))
        if re.search(r"vocal|\bvox\b|voice|acapella|chant|shout|adlib", low):
            typ = "vocal"
        elif re.search(r"\bfx\b|sfx|riser|impact|sweep|uplifter|downlifter|transition|whoosh|noise", name):
            typ = "fx"
        elif re.search(r"kick|snare|clap|hat|\bhh\b|crash|ride|cymbal|rim|\btom|shaker|perc|cowbell|snap|808|drum|"
                       r"\btop|groove|fill|break", low):
            typ = "drum_loop" if loop else "drum_oneshot"
        else:
            typ = "melodic_loop" if loop else "oneshot_melodic"
    sub = ""
    if typ == "drum_oneshot":
        for where in (name, low):
            for s, pat in DRUM_WORDS:
                if re.search(pat, where):
                    sub = s
                    break
            if sub:
                break
    if sub == "kick_808" and re.search(r"kick|kik", name):
        sub = "kick"  # "808 kick" is a kick; a bare "808" is the bass
    style = ""
    for s, pat in STYLE_WORDS:
        if re.search(pat, low):
            style = s
            break
    return typ, sub, style


# ---------------------------------------------------------------- main

def load_rows(tables, rar):
    rows = []
    for typ in TYPES:
        p = os.path.join(tables, typ + ".csv")
        if not os.path.exists(p):
            continue
        with open(p, newline="", encoding="utf-8") as fh:
            for r in csv.DictReader(fh):
                if r.get("rar") == str(rar):
                    r["_table"] = typ
                    rows.append(r)
    return rows


def measure_row(path, typ):
    try:
        return mesure.measure(path, typ), ""
    except Exception as e:  # unreadable file: the table may say so too
        return {}, "%s" % type(e).__name__


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("folder")
    ap.add_argument("--rar", type=int, required=True)
    ap.add_argument("--tables", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args(argv)
    rows = load_rows(a.tables, a.rar)
    if not rows:
        print("no row for rar %d" % a.rar)
        return 2
    by_pack = collections.defaultdict(list)
    for r in rows:
        by_pack[r["pack"]].append(r)
    dirs = find_pack_dirs(a.folder, by_pack)
    os.makedirs(a.out, exist_ok=True)
    out_csv = os.path.join(a.out, "rar%d.csv" % a.rar)
    head = ["cle", "type", "subtype", "style", "error_table", "error_mine"]
    for c in COLS:
        head += [c + "_table", c + "_mine", c + "_ok"]
    stats = collections.defaultdict(lambda: collections.Counter())  # (type, col) -> outcome counts
    counts = []
    lab = collections.Counter()
    conf = collections.Counter()
    style_conf = collections.Counter()
    n808 = collections.Counter()
    with open(out_csv, "w", newline="", encoding="utf-8") as fh:
        wr = csv.writer(fh)
        wr.writerow(head)
        for pk in sorted(by_pack, key=lambda s: int(s[1:]) if s[1:].isdigit() else 0):
            prow = by_pack[pk]
            folder = dirs.get(pk)
            if folder is None:
                counts.append((pk, 0, 0, sum(r["_table"] not in ("kit", "midi") for r in prow),
                               sum(r["_table"] == "midi" for r in prow), "pack folder not found"))
                continue
            ids = ids_under(folder)
            audio, midi = visible_walk(folder)
            file_ids = {fid(folder, os.path.relpath(p, folder).replace(os.sep, "/")): p for p in audio + midi}
            row_ids = {r["file_id"] for r in prow if r["_table"] != "kit"}
            kit_ids = {r["file_id"] for r in prow if r["_table"] == "kit"}
            missing = len(set(file_ids) - row_ids)
            extra = len(row_ids - set(file_ids))
            kit_lost = len(kit_ids - set(ids))
            counts.append((pk, len(audio), len(midi), sum(r["_table"] not in ("kit", "midi") for r in prow),
                           sum(r["_table"] == "midi" for r in prow),
                           "files without row %d, rows without file %d, kit rows without folder %d" % (missing, extra, kit_lost)))
            for r in prow:
                typ = r["_table"]
                if typ == "kit":
                    continue
                path = file_ids.get(r["file_id"])
                if path is None:
                    continue
                rel = os.path.relpath(path, folder).replace(os.sep, "/")
                if "808" in rel:
                    n808[(pk, typ)] += 1
                mt, msub, mstyle = read_label(rel)
                tsub = r.get("subtype") or ""
                lab["type", mt == typ] += 1
                if typ == "drum_oneshot" and mt == "drum_oneshot":
                    conf[(tsub.split(":")[0], msub or "?")] += 1
                    lab["subtype", (msub or "?") == tsub.split(":")[0]] += 1
                if mstyle:
                    style_conf[(r.get("style") or "", mstyle)] += 1
                    lab["style", mstyle == r.get("style")] += 1
                if typ == "midi":
                    continue
                mine, err = measure_row(path, typ)
                line = [r["cle"], typ, tsub, r.get("style", ""), r.get("error", ""), err]
                fam = tsub.split(":")[0]
                for c in COLS:
                    if c not in r:
                        line += ["", "", ""]
                        continue
                    tv, mv = fnum(r.get(c)), mine.get(c)
                    if c in ("pitch_steady_hz", "pitch_attack_hz"):
                        td = fnum(r.get("tonal_db"))
                        if td is None or td < -10:
                            line += [r.get(c, ""), "" if mv is None else "%.6g" % mv, "nc"]
                            continue
                    ok = agree(c, mv, tv)
                    line += [r.get(c, ""), "" if mv is None else "%.6g" % mv, ok]
                    if ok:
                        key = typ + (":" + fam if typ == "drum_oneshot" and fam else "")
                        stats[(typ, c)][ok] += 1
                        stats[(key, c)][ok] += 1 if key != typ else 0
                wr.writerow(line)
    write_summary(a, stats, counts, lab, conf, style_conf, n808)
    print("wrote", out_csv)
    return 0


def pct(c):
    n = sum(c.values())
    return (100.0 * c["yes"] / n if n else float("nan")), n


def write_summary(a, stats, counts, lab, conf, style_conf, n808):
    L = ["# Contre-mesure RAR %d (outil `scripts/sons/passe2/verif/`)" % a.rar, "",
         "Tolérances : `scripts/sons/passe2/verif/TOLERANCES.md`. « oui » = dans la tolérance ; "
         "« vide d'un côté » = une seule des deux mesures existe ; « octave » = f0 à une ou deux octaves près.", "",
         "## Accord par type et colonne", "",
         "| type | colonne | comparés | accord % | non | vide d'un côté | octave |", "|---|---|---|---|---|---|---|"]
    for (typ, c) in sorted(stats):
        cnt = stats[(typ, c)]
        if not sum(cnt.values()):
            continue
        p, n = pct(cnt)
        flag = " ⚠" if p < 98 else ""
        L.append("| %s | %s | %d | %.1f%s | %d | %d | %d |" % (typ, c, n, p, flag, cnt["no"], cnt["one-empty"], cnt["octave"]))
    allc = collections.defaultdict(collections.Counter)
    for (typ, c), cnt in stats.items():
        if ":" not in typ:
            allc[c].update(cnt)
    L += ["", "## Accord par colonne, tous types", "", "| colonne | comparés | accord % |", "|---|---|---|"]
    for c in COLS:
        if c in allc:
            p, n = pct(allc[c])
            L.append("| %s | %d | %.1f |" % (c, n, p))
    L += ["", "## Fichiers de l'archive contre lignes", "",
          "| pack | audio | midi | lignes audio | lignes midi | écarts |", "|---|---|---|---|---|---|"]
    for pk, au, mi, ra, rm, note in counts:
        L.append("| %s | %d | %d | %d | %d | %s |" % (pk, au, mi, ra, rm, note))

    def rate(k):
        y, n = lab[k, True], lab[k, False]
        return "%d / %d (%.1f %%)" % (y, y + n, 100.0 * y / (y + n)) if y + n else "-"
    L += ["", "## Lecture indépendante des étiquettes (chemin du fichier)", "",
          "- type : %s" % rate("type"), "- sous-type des coups de batterie : %s" % rate("subtype"),
          "- style (quand le chemin en nomme un) : %s" % rate("style"), "",
          "Confusions de sous-type (table → ma lecture), les plus fréquentes :", "",
          "| table | ma lecture | n |", "|---|---|---|"]
    for (t, m), n in sorted(conf.items(), key=lambda kv: -kv[1]):
        if t != m:
            L.append("| %s | %s | %d |" % (t, m, n))
    pairs = [("kick", "kick_808"), ("kick_808", "kick"), ("snare", "clap"), ("clap", "snare"),
             ("hat_closed", "hat_open"), ("hat_open", "hat_closed")]
    L += ["", "Paires surveillées : " + ", ".join("%s→%s %d" % (t, m, conf[(t, m)]) for t, m in pairs), "",
          "Styles (table → ma lecture) en désaccord :", "", "| table | ma lecture | n |", "|---|---|---|"]
    for (t, m), n in sorted(style_conf.items(), key=lambda kv: -kv[1]):
        if t != m:
            L.append("| %s | %s | %d |" % (t, m, n))
    L += ["", "## Sons dont le chemin dit « 808 »", "", "| pack | type | n |", "|---|---|---|"]
    for (pk, typ), n in sorted(n808.items()):
        L.append("| %s | %s | %d |" % (pk, typ, n))
    with open(os.path.join(a.out, "rar%d-resume.md" % a.rar), "w", encoding="utf-8") as fh:
        fh.write("\n".join(L) + "\n")


if __name__ == "__main__":
    sys.exit(main())
