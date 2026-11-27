"""Audit 4: replay the _accuracy.csv table from the rows themselves and check the gate.

Run: AUDIT2_WORK=/dir/outside/repo python3 -I scripts/sons/audit4/accuracy.py
     python3 -I scripts/sons/audit4/accuracy.py --tables <dir of merged *.csv>
The second form prints the key breakdown (exact / same tonic / relative / other)
by type and by RAR. Only 'exact' counts as right: a same tonic with the wrong
mode is a different key (passe 2 blind critique, G1).
"""
import csv, glob, os, shutil, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "audit2"))
from lib import *  # noqa: F401,F403

NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]


_PC = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
KEY_CLASSES = ("exact", "same_tonic", "relative", "other")


def parse_key(cell):
    """'F# minor', 'Db major', 'F' -> (pc, mode or None); None when the cell holds no key."""
    parts = (cell or "").strip().split()
    if not parts or parts[0][:1].upper() not in _PC or len(parts) > 2:
        return None
    root = parts[0]
    pc = _PC[root[0].upper()] + sum(1 for c in root[1:] if c in "#\u266f") - sum(1 for c in root[1:] if c in "b\u266d")
    if len(root) > 1 and any(c not in "#b\u266f\u266d" for c in root[1:]):
        return None
    mode = parts[1].lower() if len(parts) == 2 else None
    if mode not in (None, "major", "minor"):
        return None
    return pc % 12, mode


def key_class(est, tag):
    """One of KEY_CLASSES, or None when either cell has no key."""
    e, t = parse_key(est), parse_key(tag)
    if e is None or t is None or e[1] is None:
        return None
    if e == t:
        return "exact"
    if e[0] == t[0]:
        return "same_tonic"  # a root-only tag heard on its root lands here too: never a recovery
    if t[1] is not None and e == (((t[0] + 3) % 12, "major") if t[1] == "minor" else ((t[0] - 3) % 12, "minor")):
        return "relative"
    return "other"


def key_breakdown(rows):
    """{(type, rar): {class: n}} plus (type, 'all'); rows need key_est and key_tag (type and rar optional)."""
    d = {}
    for r in rows:
        c = key_class(r.get("key_est"), r.get("key_tag"))
        if c is None:
            continue
        typ = r.get("type") or "?"
        for g in ((typ, r.get("rar") or "?"), (typ, "all")):
            d.setdefault(g, dict.fromkeys(KEY_CLASSES, 0))[c] += 1
    return d


TEMPO_CLASSES = ("exact", "half_double", "other")


def _num(cell):
    try:
        v = float(cell)
    except (TypeError, ValueError):
        return None
    return v if v > 0 else None


def tempo_breakdown(rows):
    """{(type, rar): {exact, half_double, other}} plus (type, 'all'); tempo heard (bpm_measured, else bpm_est)
    against the written one. half_double is the octave slip (62.5 for 125), folded apart (critique M6)."""
    d = {}
    for r in rows:
        t = _num(r.get("bpm_tag"))
        m = _num(r.get("bpm_measured")) or _num(r.get("bpm_est"))
        if t is None or m is None:
            continue
        c = tout._bpm_class(m, t)
        c = "exact" if c is True else ("half_double" if c == "half_double" else "other")
        typ = r.get("type") or "?"
        for g in ((typ, r.get("rar") or "?"), (typ, "all")):
            d.setdefault(g, dict.fromkeys(TEMPO_CLASSES, 0))[c] += 1
    return d


def print_tempo(d):
    print("%-14s %-4s %6s %6s %11s %6s %7s %12s" % ("type", "rar", "n", "exact", "half_double", "other", "exact%",
                                                    "after_fold%"))
    for (typ, rar) in sorted(d, key=lambda g: (g[0], g[1] == "all", g[1].zfill(3))):
        c = d[(typ, rar)]
        n = sum(c.values())
        print("%-14s %-4s %6d %6d %11d %6d %6.1f%% %11.1f%%" % (
            typ, rar, n, c["exact"], c["half_double"], c["other"], 100.0 * c["exact"] / n if n else 0.0,
            100.0 * (c["exact"] + c["half_double"]) / n if n else 0.0))


def print_breakdown(d):
    print("%-14s %-4s %6s %6s %10s %8s %6s %7s" % ("type", "rar", "n", "exact", "same_tonic", "relative", "other", "exact%"))
    for (typ, rar) in sorted(d, key=lambda g: (g[0], g[1] == "all", g[1].zfill(3))):
        c = d[(typ, rar)]
        n = sum(c.values())
        print("%-14s %-4s %6d %6d %10d %8d %6d %6.1f%%" % (typ, rar, n, c["exact"], c["same_tonic"], c["relative"],
                                                       c["other"], 100.0 * c["exact"] / n if n else 0.0))


def tables_rows(folder, need=("key_est", "key_tag")):
    rows = []
    for f in sorted(glob.glob(os.path.join(folder, "*.csv"))):
        with open(f, newline="", encoding="utf-8") as fh:
            lines = [ln for ln in fh if not ln.startswith("#")]
        rd = csv.DictReader(lines)
        if rd.fieldnames and all(c in rd.fieldnames for c in need):
            rows += list(rd)
    return rows


def build(root, n_tagged, wrong_names=0):
    shutil.rmtree(root, ignore_errors=True)
    for i in range(n_tagged):
        r = i % 12
        bpm = 90 + 5 * (i % 6)
        ch = [[48 + r, 52 + r, 55 + r], [53 + r, 57 + r, 60 + r], [55 + r, 59 + r, 62 + r], [48 + r, 52 + r, 55 + r]]  # I IV V I
        name = NAMES[(r + 2) % 12] if i < wrong_names else NAMES[r]
        write_wav(os.path.join(root, "Loops", "Melodic", "Keys %02d %d BPM %s major.wav" % (i, bpm, name)),
                  chord_loop(bpm, 4, chords=ch))
        write_wav(os.path.join(root, "Loops", "Drums", "Drum Loop %02d %d BPM.wav" % (i, bpm)),
                  drum_loop(bpm, 2, "four"))
    for i in range(3):
        write_wav(os.path.join(root, "Loops", "Melodic", "Keys untagged %02d.wav" % i),
                  chord_loop(100, 4, chords=[[50, 54, 57], [55, 59, 62], [57, 61, 64], [50, 54, 57]]))


def tables(out):
    t = {}
    for f in glob.glob(out + "_*.csv"):
        with open(f, newline="", encoding="utf-8") as fh:
            t[f[len(out) + 1:-4]] = list(csv.DictReader(fh))
    return t


def replay(t):
    """Recompute (type, measure) -> (n, agree) from the row cells."""
    d = {}
    for typ in ("melodic_loop", "drum_loop"):
        for r in t.get(typ, []):
            if r.get("bpm_tag") and r.get("bpm_gap_pct") not in (None, ""):  # no bpm_measured column
                n, a = d.get((typ, "bpm"), (0, 0))
                d[(typ, "bpm")] = (n + 1, a + (abs(float(r["bpm_gap_pct"])) <= 2.0))
            if typ == "melodic_loop" and r.get("key_match"):
                n, a = d.get((typ, "key"), (0, 0))
                d[(typ, "key")] = (n + 1, a + (r["key_match"] == "exact"))
    return d


def main():
    bad = 0
    for label, n, wrong in (("12 tagged, all right", 12, 0), ("12 tagged, 1 wrong key name", 12, 1),
                            ("9 tagged, all right", 9, 0)):
        root = os.path.join(S, "a4acc", "p%d_%d" % (n, wrong))
        build(root, n, wrong)
        rows, con, code = run_pack(root, "acc_%d_%d" % (n, wrong))
        out = os.path.join(S, "out", "run_acc_%d_%d" % (n, wrong))
        t = tables(out)
        acc = {(r["type"], r["measure"]): r for r in t.get("accuracy", [])}
        rep = replay(t)
        print("== %s (code %s)" % (label, code))
        for k in sorted(set(acc) | set(rep)):
            a = acc.get(k, {})
            nn, ag = rep.get(k, (0, 0))
            same = a and int(a["tagged"]) == nn and int(a["agree"]) == ag
            want_usable = "yes" if nn and tout._wilson_upper(nn - ag, nn) < 0.05 else "no"  # the tool's Wilson gate (audit 4, A7)
            same = same and a.get("usable") == want_usable
            bad += not same
            print("  %s %-24s table tagged=%s agree=%s usable=%s | replay tagged=%d agree=%d usable=%s" % (
                "PASS" if same else "FAIL", "%s/%s" % k, a.get("tagged"), a.get("agree"), a.get("usable"), nn, ag, want_usable))
        un = [r for r in t.get("melodic_loop", []) if not r.get("key_tag")]
        print("  untagged melodic loops: key_est=%s bpm_est=%s" % ([r.get("key_est") for r in un], [r.get("bpm_est") for r in un]))
    print("FAIL=%d" % bad)
    return min(bad, 100)


if __name__ == "__main__":
    if sys.argv[1:2] == ["--tables"]:
        print_breakdown(key_breakdown(tables_rows(sys.argv[2])))
        print()
        print_tempo(tempo_breakdown(tables_rows(sys.argv[2], ("bpm_tag",))))
        sys.exit(0)
    sys.exit(main())
