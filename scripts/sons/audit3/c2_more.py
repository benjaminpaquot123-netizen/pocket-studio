"""Audit 3, own cases 2: --trace on kit rows, one-shots with a tempo in the name,
bare-number drum loops over many patterns.
Usage: AUDIT2_WORK=<folder outside the repo> python3 -I c2_more.py <path to scripts/sons/audit2>"""
import contextlib, io, os, shutil, sys
sys.path.insert(0, sys.argv[1])
from lib import *
import lib

N = [0]


def pack(files, show=True, allcols=False):
    N[0] += 1
    root = os.path.join(lib.S, "pack", "c2", "%03d" % N[0])
    shutil.rmtree(root, ignore_errors=True)
    for rel, x in files.items():
        p = os.path.join(root, *rel.split("/"))
        write_midi(p, x[1], x[2]) if isinstance(x, tuple) else write_wav(p, x)
    rows, con, code = run_pack(root, "c2")
    if show:
        print("-- %s (code %s)" % (" + ".join(files), code))
        for r in rows:
            print("     " + " | ".join("%s=%s" % (k, v) for k, v in r.items() if v != "" and (allcols or k in (
                "type", "subtype", "role", "bpm_tag", "bpm_est", "key_tag", "note_tag", "bars", "end_cut", "file_id"))))
    return rows, root


def trace(root, fid):
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        try:
            lib.tout.main([root, "--rar", "1", "--trace", fid])
        except SystemExit:
            pass
    return buf.getvalue().strip().replace("\n", " / ")[:140]


print("== trace on kit, stem and kit midi rows")
D, P = drum_loop(128, 4), chord_loop(128, 4)
MID = ("midi", [(i, 1, 57 + (i % 3) * 4, 100, 0) for i in range(16)], None)
rows, root = pack({"Kits/Kit 01 - 128 Am/Drums.wav": D, "Kits/Kit 01 - 128 Am/Pad.wav": P, "Kits/Kit 01 - 128 Am/Kit 01.mid": MID,
                   "Kits/Kit 02 - 128 Am/Drums.wav": D, "Kits/Kit 02 - 128 Am/Pad.wav": P}, show=False)
for r in rows:
    print("   %-9s %s -> %s" % (r["type"], r.get("file_id"), trace(root, r.get("file_id") or "none")))
print("   unknown id -> %s" % trace(root, "0000000000"))

print("== one-shots with a tempo in the name")
T1 = fade(tone(130.8, 1.5, 0.4), 30)
for rel in ("One Shots/Bass C2 120bpm.wav", "Bass One Shots/Bass C2 120bpm.wav", "Oneshots/Bass C2 120 bpm.wav",
            "One Shots/Bass C2 120.wav", "Bass/Bass C2 120bpm.wav", "One Shots/Pluck C3 140bpm.wav", "Stabs/Stab Am 128bpm.wav"):
    pack({rel: T1})
pack({"One Shots/Bass C2.wav": T1}, allcols=True)

print("== drum loops named with a bare number that IS their tempo: kept or lost?")
kept = lost = wrong = 0
for kind in ("four", "boombap", "half", "dnb"):
    for hats in ("8th", "16th", "off"):
        line = []
        for bpm in (70, 85, 90, 100, 110, 120, 128, 140, 150, 160, 174):
            rows, _ = pack({"Drum Loops/Drum Loop %d.wav" % bpm: drum_loop(bpm, 2, kind, hats)}, show=False)
            t = rows[0].get("bpm_tag", "") if rows else "?"
            ok = t not in ("", "?") and abs(float(t) - bpm) < 0.01
            kept += ok
            lost += t == ""
            wrong += (not ok and t != "")
            line.append("%d:%s" % (bpm, "ok" if ok else (t or "LOST")))
        print("   %-8s %-5s %s" % (kind, hats, " ".join(line)))
print("   kept %d, lost %d, wrong %d" % (kept, lost, wrong))
