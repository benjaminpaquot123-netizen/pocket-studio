"""Audit 4: replay the _accuracy.csv table from the rows themselves and check the gate.

Run: AUDIT2_WORK=/dir/outside/repo python3 -I scripts/sons/audit4/accuracy.py
"""
import csv, glob, os, shutil, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "audit2"))
from lib import *  # noqa: F401,F403

NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]


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
        want_usable = "yes" if nn >= 10 and 100.0 * (nn - ag) / nn < 5 else "no"
        same = same and a.get("usable") == want_usable
        bad += not same
        print("  %s %-24s table tagged=%s agree=%s usable=%s | replay tagged=%d agree=%d usable=%s" % (
            "PASS" if same else "FAIL", "%s/%s" % k, a.get("tagged"), a.get("agree"), a.get("usable"), nn, ag, want_usable))
    un = [r for r in t.get("melodic_loop", []) if not r.get("key_tag")]
    print("  untagged melodic loops: key_est=%s bpm_est=%s" % ([r.get("key_est") for r in un], [r.get("bpm_est") for r in un]))
print("FAIL=%d" % bad)
sys.exit(min(bad, 100))
