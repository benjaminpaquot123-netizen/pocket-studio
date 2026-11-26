#!/usr/bin/env python3
"""Merge the deep-pass (passe 2) CSV tables of every RAR into one set of tables.

  python3 -I scripts/sons/passe2/reunit.py            # read the frozen heads (TETES), write docs/sons/passe2/tout/
  python3 -I scripts/sons/passe2/reunit.py --ref 7=eq/passe2-rar7   # override / add a head
  python3 -I scripts/sons/passe2/reunit.py --src DIR  # read DIR/rarN/*.csv instead of git

Reads only CSV (git show), never a sound. Refuses (exit 1):
  - a duplicate `cle` across all sound tables;
  - a non-empty `error` cell that is not listed in the admitted-errors file (cle,error,raison);
  - a RAR or pack whose sound rows differ from the control table (rows + kit_rows).
Writes one table per kind, controle.csv, accuracy.csv (precision table, not a sound table),
passe1_comptes.csv (first-pass file counts per RAR) and chiffres.md (automatic counts and medians).
"""
import argparse
import csv
import io
import os
import statistics
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
GENRES = ["drum_oneshot", "drum_loop", "melodic_loop", "oneshot_melodic", "vocal", "fx", "kit",
          "kit_stem", "midi"]
# Heads handed by the conductor (deep pass, one branch per RAR).
TETES = {
    1: "f08dac9d6", 2: "e902e0c70", 3: "f1b112281", 4: "3e25cea17", 5: "dcc0ebf24",
    6: "6f588cac8", 8: "9a804e452", 10: "d9f22701b", 12: "d05cf617a",
}
PASSE1_SKIP_TYPES = {"doc", "junk", "preset"}


def log(msg):
    print(msg, file=sys.stderr)


def git(*args):
    return subprocess.run(["git", "-C", ROOT] + list(args), check=True, capture_output=True,
                          text=True).stdout


def read_csv_text(text):
    lines = [l for l in text.splitlines() if l.strip() and not l.startswith("#")]
    if not lines:
        return [], []
    rd = csv.DictReader(io.StringIO("\n".join(lines) + "\n"))
    return list(rd.fieldnames or []), list(rd)


def load_rar_files(n, src=None, ref=None):
    """Return {basename: text} for the CSVs of RAR n."""
    out = {}
    if src:
        d = os.path.join(src, "rar%d" % n)
        if not os.path.isdir(d):
            return out
        for name in sorted(os.listdir(d)):
            if name.endswith(".csv"):
                with open(os.path.join(d, name), newline="") as f:
                    out[name] = f.read()
    else:
        base = "docs/sons/passe2/rar%d/" % n
        for path in git("ls-tree", "--name-only", ref, base).split():
            if path.endswith(".csv"):
                out[os.path.basename(path)] = git("show", "%s:%s" % (ref, path))
    return out


def load_admises(path):
    adm = set()
    if path and os.path.exists(path):
        with open(path, newline="") as f:
            for r in csv.DictReader(f):
                adm.add(((r.get("cle") or "").strip(), (r.get("error") or "").strip()))
    return adm


def passe1_counts():
    """First-pass file counts per RAR, from eq/mine-or-rarN-tout (else eq/mine-or-rarN)."""
    res = []
    for n in range(1, 13):
        ref = None
        for cand in ("origin/eq/mine-or-rar%d-tout" % n, "origin/eq/mine-or-rar%d" % n):
            try:
                git("rev-parse", "-q", "--verify", cand)
                ref = cand
                break
            except subprocess.CalledProcessError:
                continue
        if not ref:
            res.append({"rar": n, "source": "", "audio": "", "midi": "", "excluded": "", "total": ""})
            continue
        names = git("ls-tree", "--name-only", ref, "docs/sons/rar/").split()
        names = [os.path.basename(p) for p in names]
        tables = {}
        for suffix in ("", "-tout", "-midi"):
            name = "rar%d%s.csv" % (n, suffix)
            if name in names:
                tables[suffix] = read_csv_text(git("show", "%s:docs/sons/rar/%s" % (ref, name)))[1]
        audio = midi = excl = 0
        tout_has_midi = any((r.get("type") or "") == "midi" for r in tables.get("-tout", []))
        for suffix in ("", "-tout"):
            for r in tables.get(suffix, []):
                t = (r.get("type") or "").strip()
                if t in PASSE1_SKIP_TYPES:
                    excl += 1
                elif t == "midi":
                    midi += 1
                else:
                    audio += 1
        if not tout_has_midi:
            midi += len(tables.get("-midi", []))
        res.append({"rar": n, "source": ref.replace("origin/", ""), "audio": audio, "midi": midi,
                    "excluded": excl, "total": audio + midi})
    return res


def to_float(v):
    try:
        x = float(v)
    except (TypeError, ValueError):
        return None
    return x if x == x else None


def family(sub):
    f = (sub or "").split(":")[0]
    return "perc" if f.startswith("perc_") else f


def trusted(r, col):
    v = to_float(r.get(col))
    if v is None:
        return None
    end = to_float(r.get("end_level_db"))
    limits = {"t20_ms": -35, "t40_ms": -50, "t60_ms": -60}
    if col in limits and (end is None or end > limits[col]):
        return None
    if col in ("pitch_attack_hz", "pitch_steady_hz", "h2_db", "h3_db"):
        tonal = to_float(r.get("tonal_db"))
        if tonal is None or tonal < -10:
            return None
    return v


def fmt_med(vals):
    if not vals:
        return "-"
    vals = sorted(vals)
    med = statistics.median(vals)
    nd = 0 if abs(med) >= 100 else 1
    if len(vals) < 4:
        return ("%." + str(nd) + "f") % med
    q = statistics.quantiles(vals, n=4)
    f = "%." + str(nd) + "f"
    return (f + " [" + f + "–" + f + "]") % (med, q[0], q[2])


def write_table(path, header, rows):
    with open(path, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=header, extrasaction="ignore", lineterminator="\n")
        w.writeheader()
        for r in rows:
            w.writerow(r)


def chiffres(tables, ctrl, p1):
    L = ["# Chiffres de la deuxième passe (écrit par reunit.py, ne pas modifier à la main)", ""]
    rars = sorted({int(c["rar"]) for c in ctrl})
    L += ["## Par RAR et par pack", "",
          "| RAR | pack | style du pack | fichiers audio | fichiers MIDI | lignes | lignes de kit | sautés | archives imbriquées |",
          "|---|---|---|---|---|---|---|---|---|"]
    for c in sorted(ctrl, key=lambda c: (int(c["rar"]), c["pack"])):
        L.append("| %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (
            c["rar"], c["pack"], c.get("style_posed", ""), c["files_audio"], c["files_midi"], c["rows"],
            c["kit_rows"], c.get("skipped", "") or "-", c.get("nested_archives", "") or "0"))
    tot = {k: sum(int(c[k] or 0) for c in ctrl) for k in ("files_audio", "files_midi", "rows", "kit_rows")}
    L.append("| total | | | %d | %d | %d | %d | | |" % (tot["files_audio"], tot["files_midi"], tot["rows"],
                                                         tot["kit_rows"]))
    L += ["", "## Lignes par type et par RAR", "", "| type | " + " | ".join(str(r) for r in rars) + " | total |",
          "|---|" + "---|" * (len(rars) + 1)]
    for g in GENRES:
        rows = tables.get(g, [])
        per = [sum(1 for r in rows if int(r["rar"]) == n) for n in rars]
        L.append("| %s | %s | %d |" % (g, " | ".join(str(x) for x in per), len(rows)))
    styles = sorted({r.get("style") or "(aucun)" for g in GENRES for r in tables.get(g, [])})
    L += ["", "## Lignes par type et par style", "", "| style | " + " | ".join(GENRES) + " |",
          "|---|" + "---|" * len(GENRES)]
    for s in styles:
        L.append("| %s | %s |" % (s, " | ".join(
            str(sum(1 for r in tables.get(g, []) if (r.get("style") or "(aucun)") == s)) for g in GENRES)))
    drums = tables.get("drum_oneshot", [])
    fams = sorted({family(r.get("subtype")) for r in drums})
    L += ["", "## Coups de batterie : famille × style", "", "| famille | " + " | ".join(styles) + " | total |",
          "|---|" + "---|" * (len(styles) + 1)]
    for fa in fams:
        sub = [r for r in drums if family(r.get("subtype")) == fa]
        L.append("| %s | %s | %d |" % (fa, " | ".join(
            str(sum(1 for r in sub if (r.get("style") or "(aucun)") == s)) for s in styles), len(sub)))
    L += ["", "## Coups de batterie : médianes par famille (règles de confiance de ANALYSE-MINE-OR §1, sans retrait des valeurs aberrantes)",
          "", "| famille | n | t20 ms | t60 ms | hauteur attaque → stable Hz | tonal dB | centroïde Hz | hf dB | transitoire/corps dB |",
          "|---|---|---|---|---|---|---|---|---|"]
    for fa in fams:
        sub = [r for r in drums if family(r.get("subtype")) == fa]
        col = lambda c: [v for v in (trusted(r, c) for r in sub) if v is not None]
        L.append("| %s | %d | %s | %s | %s → %s | %s | %s | %s | %s |" % (
            fa, len(sub), fmt_med(col("t20_ms")), fmt_med(col("t60_ms")), fmt_med(col("pitch_attack_hz")),
            fmt_med(col("pitch_steady_hz")), fmt_med(col("tonal_db")), fmt_med(col("centroid_hz")),
            fmt_med(col("hf_db")), fmt_med(col("transient_body_db"))))
    L += ["", "## Coups de batterie par RAR", "", "| RAR | " + " | ".join(str(r) for r in rars) + " |",
          "|---|" + "---|" * len(rars),
          "| coups | " + " | ".join(str(sum(1 for r in drums if int(r["rar"]) == n)) for n in rars) + " |"]
    L += ["", "## Packs sans étiquette de style", ""]
    unl = [c for c in ctrl if (c.get("style_posed") or "") in ("", "unlabelled")]
    L += ["- RAR %s %s : %s lignes" % (c["rar"], c["pack"], c["rows"]) for c in unl] or ["- aucun"]
    if p1:
        p2tot = {n: sum(int(c["rows"] or 0) for c in ctrl if int(c["rar"]) == n) for n in rars}
        L += ["", "## Première passe contre deuxième passe (fichiers)", "",
              "| RAR | source 1re passe | audio | MIDI | exclus (doc, junk, preset) | total 1re | lignes 2e (rows) | écart |",
              "|---|---|---|---|---|---|---|---|"]
        for p in p1:
            n = int(p["rar"])
            two = p2tot.get(n)
            gap = "" if two is None or p["total"] == "" else str(two - int(p["total"]))
            L.append("| %d | %s | %s | %s | %s | %s | %s | %s |" % (
                n, p["source"], p["audio"], p["midi"], p["excluded"], p["total"],
                "-" if two is None else two, gap or "-"))
    return "\n".join(L) + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--ref", action="append", default=[], help="N=REF, overrides or adds a head")
    ap.add_argument("--src", help="read DIR/rarN/*.csv instead of git")
    ap.add_argument("--out", default=os.path.join(ROOT, "docs/sons/passe2/tout"))
    ap.add_argument("--admises", default=os.path.join(ROOT, "docs/sons/passe2/erreurs_admises.csv"))
    ap.add_argument("--no-passe1", action="store_true")
    ap.add_argument("--quiet", action="store_true")
    a = ap.parse_args(argv)

    heads = dict(TETES)
    for spec in a.ref:
        n, _, ref = spec.partition("=")
        heads[int(n)] = ref
    rars = list(range(1, 13)) if a.src else sorted(heads)

    tables, headers, ctrl, ctrl_h, acc, acc_h = {}, {}, [], None, [], None
    problems = []
    for n in rars:
        files = load_rar_files(n, src=a.src, ref=None if a.src else heads[n])
        if not files:
            continue
        for name, text in files.items():
            kind = name[len("rar%d_" % n):-4] if name.startswith("rar%d_" % n) else None
            h, rows = read_csv_text(text)
            if kind == "controle":
                ctrl_h = ctrl_h or h
                ctrl += rows
            elif kind == "accuracy":
                acc_h = acc_h or h
                acc += rows
            elif kind in GENRES:
                if kind in headers and headers[kind] != h:
                    problems.append("header of %s differs in RAR %d" % (name, n))
                headers.setdefault(kind, h)
                tables.setdefault(kind, []).extend(rows)
            else:
                problems.append("unknown table %s in RAR %d" % (name, n))
        if not a.quiet:
            log("RAR %d: %d table(s)" % (n, len(files)))

    # Refusal 1: duplicate key.
    seen = {}
    for g, rows in tables.items():
        for r in rows:
            k = r.get("cle", "")
            if k in seen:
                problems.append("duplicate cle %s (%s and %s)" % (k, seen[k], g))
            seen[k] = g
    # Refusal 2: unlisted error cell.
    adm = load_admises(a.admises)
    for g, rows in tables.items():
        for r in rows:
            e = (r.get("error") or "").strip()
            if e and (r.get("cle", ""), e) not in adm:
                problems.append("unlisted error in %s, cle %s: %s" % (g, r.get("cle"), e))
    # Refusal 3: totals against the control table, per pack and per RAR.
    count = {}
    for rows in tables.values():
        for r in rows:
            key = (str(r.get("rar")), r.get("pack"))
            count[key] = count.get(key, 0) + 1
    want_rar, got_rar = {}, {}
    for c in ctrl:
        key = (str(c["rar"]), c["pack"])
        want = int(c["rows"] or 0) + int(c["kit_rows"] or 0)
        got = count.get(key, 0)
        want_rar[key[0]] = want_rar.get(key[0], 0) + want
        if got != want:
            problems.append("row total of RAR %s %s: %d in the tables, %d in the control (rows + kit_rows)"
                            % (key[0], key[1], got, want))
    for key, v in count.items():
        got_rar[key[0]] = got_rar.get(key[0], 0) + v
    for rar in sorted(set(want_rar) | set(got_rar), key=int):
        if want_rar.get(rar, 0) != got_rar.get(rar, 0):
            problems.append("row total of RAR %s: %d in the tables, %d in the control"
                            % (rar, got_rar.get(rar, 0), want_rar.get(rar, 0)))

    if problems:
        for p in problems:
            print("REFUSED: " + p)
        return 1

    os.makedirs(a.out, exist_ok=True)
    keyf = lambda r: (int(r.get("rar") or 0), r.get("pack") or "", r.get("cle") or "")
    for g in GENRES:
        if g in tables:
            write_table(os.path.join(a.out, g + ".csv"), headers[g], sorted(tables[g], key=keyf))
    if ctrl_h:
        write_table(os.path.join(a.out, "controle.csv"), ctrl_h,
                    sorted(ctrl, key=lambda c: (int(c["rar"]), c["pack"])))
    if acc_h:
        write_table(os.path.join(a.out, "accuracy.csv"), acc_h, sorted(acc, key=keyf))
    p1 = None
    if not a.no_passe1:
        p1 = passe1_counts()
        write_table(os.path.join(a.out, "passe1_comptes.csv"),
                    ["rar", "source", "audio", "midi", "excluded", "total"], p1)
    with open(os.path.join(a.out, "chiffres.md"), "w") as f:
        f.write(chiffres(tables, ctrl, p1))
    total = sum(len(v) for v in tables.values())
    print("ok: %d RAR, %d rows (%s)" % (len({c["rar"] for c in ctrl}), total,
                                        ", ".join("%s %d" % (g, len(tables[g])) for g in GENRES if g in tables)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
