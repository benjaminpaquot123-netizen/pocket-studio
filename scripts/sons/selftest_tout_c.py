#!/usr/bin/env python3
"""Self-test of tout.py, lot C of audit 2 (defects 7, 19, 20 and the end fence of 18).

    python3 -I scripts/sons/selftest_tout_c.py

The traps are the audit's own untagged loops (scripts/sons/audit2/lib.py):
a tempo, a bar count or a key read from them is either right or left blank,
never shown wrong. Exit code 1 when a single check fails.
"""
import os
import sys
import tempfile

_WORK = tempfile.mkdtemp(prefix="selftest_tout_c_")
os.environ["AUDIT2_WORK"] = _WORK
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "audit2"))
import lib  # noqa: E402
from lib import SR, chord_loop, drum_loop, one, run_pack, write_wav  # noqa: E402

tout = lib.tout
FAILS = []
COUNT = [0]


def ok(name, cond, detail=""):
    COUNT[0] += 1
    if not cond:
        FAILS.append(name)
    print("%s %s%s" % ("ok  " if cond else "FAIL", name, ("  (" + detail + ")") if detail else ""))


def _f(v):
    return None if v in (None, "") else float(v)


def right_or_blank(name, got, want, rel):
    g = _f(got)
    ok(name, g is None or abs(g - want) <= rel * want, "got %r want %g or blank" % (got, want))


N = [0]


def drum(label, bpm, x, beats_per_bar=4):
    """defect 7 / 20: an untagged drum loop shows the right tempo and bar count, or blanks."""
    N[0] += 1
    r = one("c4", N[0], "Drum Loops/Loop 01.wav", x)[0][0]
    right_or_blank("tempo %s %g" % (label, bpm), r.get("bpm_est"), bpm, 0.02)
    bars = len(x) / SR * bpm / 60.0 / beats_per_bar
    b = _f(r.get("bars"))
    ok("bars %s %g" % (label, bpm), b is None or abs(b - bars) < 0.26, "got %r want %.3g or blank" % (r.get("bars"), bars))
    return r


# defect 7: a half or double tempo was shown as sure
for bpm in (65, 70, 80):
    drum("four", bpm, drum_loop(bpm, 2, "four", "8th"))
for bpm in (55, 58):
    drum("outside the range", bpm, drum_loop(bpm, 2, "four", "8th"))
drum("half-time", 60, drum_loop(60, 2, "half", "16th"))
drum("triplet hats", 80, drum_loop(80, 2, "four", "trip"))
drum("drum and bass", 174, drum_loop(174, 2, "dnb", "8th"))
# defect 20: a waltz counted in 4, a reverb tail counted as a bar fraction
w = drum("waltz 3/4", 90, drum_loop(90, 4, "waltz", "8th"), 3)
ok("waltz bars not counted in 4", _f(w.get("bars")) != 3.0, "got %r" % w.get("bars"))
t = drum("reverb tail", 120, drum_loop(120, 2, "four", "8th", tail=True))
ok("tail: 2 whole bars", _f(t.get("bars")) in (None, 2.0), "got %r" % t.get("bars"))
# a clear loop is still measured (the fence does not blank everything)
r = drum("four", 120, drum_loop(120, 2, "four", "8th"))
ok("four 120 measured", _f(r.get("bpm_est")) == 120.0, "got %r" % r.get("bpm_est"))

# tempo_detail: 55 bpm (below the range) is never sure
bpm, info = tout.tempo_detail(drum_loop(55, 2, "four", "8th"), SR, 2 * 4 * 60.0 / 55)
ok("tempo_detail 55 not sure", info["sure"] is False, "bpm %r" % bpm)
bpm, info = tout.tempo_detail(drum_loop(120, 2, "four", "8th"), SR, 2 * 4 * 60.0 / 120)
ok("tempo_detail 120 sure", info["sure"] is True and abs(bpm - 120) < 0.5, "bpm %r sure %r" % (bpm, info["sure"]))

# defect 19: an untagged melodic loop keeps no bar count read from a blanked tempo
prog = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
for pl in (False, True):
    for bpm in (80, 140):
        N[0] += 1
        r = one("c8", N[0], "Loops/%s Loop 01.wav" % ("Pluck" if pl else "Pad"), chord_loop(bpm, 4, prog, pluck=pl), bits=24)[0][0]
        ok("melodic bars %s %d" % ("pluck" if pl else "pad", bpm), r.get("bars") in ("", "4", "4.0"), "got %r" % r.get("bars"))
        right_or_blank("melodic tempo %s %d" % ("pluck" if pl else "pad", bpm), r.get("bpm_est"), bpm, 0.02)

# defect 19: an untagged kit made of pads shows no unproven tempo, key or bars
for bpm in (100, 174):
    N[0] += 1
    root = os.path.join(_WORK, "pack", "c8k", "%03d" % N[0])
    kd = os.path.join(root, "Construction Kits", "Kit 01")
    write_wav(os.path.join(kd, "Pad.wav"), chord_loop(bpm, 4, prog, pluck=False), SR, bits=24)
    write_wav(os.path.join(kd, "Strings.wav"), chord_loop(bpm, 4, [[c[0] + 12] for c in prog], pluck=False), SR, bits=24)
    rows = run_pack(root, "c8k")[0]
    kit = [x for x in rows if x.get("type") == "kit"]
    ok("pad kit %d has a row" % bpm, len(kit) == 1, "rows %d" % len(kit))
    for k in kit:
        right_or_blank("pad kit tempo %d" % bpm, k.get("bpm_est"), bpm, 0.02)
        ok("pad kit key %d blank" % bpm, k.get("key_est") in ("", None), "got %r" % k.get("key_est"))
        ok("pad kit bars %d blank or 4" % bpm, k.get("bars") in ("", None, "4", "4.0"), "got %r" % k.get("bars"))

# defect 18 (fence part): a 'full' drum one-shot keeps its measured end
row = {"type": "drum_oneshot", "role": "full", "bpm_tag": None, "end_cut": "clean", "end_fade_ms": 37}
tout.fence_row(row, set())
ok("full one-shot keeps end_cut", row.get("end_cut") == "clean", "got %r" % row.get("end_cut"))
ok("full one-shot fade rounded", row.get("end_fade_ms") == 40, "got %r" % row.get("end_fade_ms"))
# ...but not when a bpm in the name says it is a loop slice
row = {"type": "drum_oneshot", "role": "full", "bpm_tag": 140.0, "end_cut": "clean", "end_fade_ms": 37}
tout.fence_row(row, set())
ok("full one-shot with a bpm tag: end blank", row.get("end_cut") is None, "got %r" % row.get("end_cut"))

print("%d checks, %d failed" % (COUNT[0], len(FAILS)))
sys.exit(1 if FAILS else 0)
