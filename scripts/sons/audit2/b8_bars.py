import os, sys, shutil
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *
B = os.path.join(S, "pack", "b8"); shutil.rmtree(B, ignore_errors=True)
chk = Check("b8_bars")
prog = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
print("== untagged melodic loop: blank is honest; a shown value must be the truth (4 bars, true bpm)")
n = 0
for pl in (False, True):
    for bpm in (70, 80, 90, 100, 110, 125, 140, 150, 160, 174):
        n += 1
        rows, con, code = one("b8", n, "Loops/%s Loop 01.wav" % ("Pluck" if pl else "Pad"), chord_loop(bpm, 4, prog, pluck=pl), bits=24)
        tag = "%s %d" % ("pluck" if pl else "pad", bpm)
        if not chk.run(tag, rows, code):
            continue
        r = rows[0]
        print("  %s true %3d bpm 4 bars -> type=%s bpm_est=%r key_est=%r bars=%s" % (
            "pluck" if pl else "pad  ", bpm, r["type"], r["bpm_est"], r["key_est"], r["bars"]))
        chk.eq(tag + " type", r["type"], "melodic_loop")
        chk.eq(tag + " bars", r["bars"], "4", may_be_empty=True)
        chk.near(tag + " bpm_est", r["bpm_est"], bpm, may_be_empty=True)
        chk.eq(tag + " key_est", r["key_est"], "A minor", may_be_empty=True)
print("== untagged kit made of pads only: kit row bpm_est / key_est (not fenced)")
for bpm in (80, 100, 140, 174):
    n += 1
    root = os.path.join(B, "%03d" % n)
    kd = os.path.join(root, "Construction Kits", "Kit 01")
    write_wav(os.path.join(kd, "Pad.wav"), chord_loop(bpm, 4, prog, pluck=False), SR, bits=24)
    write_wav(os.path.join(kd, "Strings.wav"), chord_loop(bpm, 4, [[c[0] + 12] for c in prog], pluck=False), SR, bits=24)
    rows, con, code = run_pack(root, "b8")
    if chk.run("kit %d" % bpm, rows, code):
        chk.eq("kit %d types" % bpm, sorted(r["type"] for r in rows), ["kit", "kit_stem", "kit_stem"])
    for r in rows:
        tag = "kit %d %s %s" % (bpm, r["type"], r["role"])
        chk.near(tag + " bpm_est", r["bpm_est"], bpm, may_be_empty=True)
        chk.eq(tag + " key_est", r["key_est"], "A minor", may_be_empty=True)
        chk.eq(tag + " bars", r.get("bars"), "4", may_be_empty=True)
        print("  true %3d A minor -> type=%s role=%s bpm_est=%r key_est=%r bars=%s" % (bpm, r["type"], r["role"], r["bpm_est"], r["key_est"], r.get("bars")))
print("== untagged drum loop that is in fact melodic content in a 'Drum Loops' folder, and the reverse (bpm_est shown?)")
n += 1
rows, con, code = one("b8", n, "Loops/Top Loop 01.wav", chord_loop(80, 4, prog, pluck=False), bits=24)
if chk.run("Top Loop 01", rows, code):
    r = rows[0]; print("  pad 80 bpm named 'Top Loop 01' -> type=%s bpm_est=%r bars=%s" % (r["type"], r["bpm_est"], r["bars"]))
    chk.eq("Top Loop 01 type", r["type"], "melodic_loop")
    chk.near("Top Loop 01 bpm_est", r["bpm_est"], 80, may_be_empty=True)
    chk.eq("Top Loop 01 bars", r["bars"], "4", may_be_empty=True)
chk.clean_repo()
chk.done()
