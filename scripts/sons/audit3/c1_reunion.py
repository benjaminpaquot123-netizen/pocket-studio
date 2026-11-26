"""Audit 3, own cases: the wiring added by the reunion of the 5 lots
(confirm_tags, style from the pack name, file_id of kits, defect 18).
Usage: AUDIT2_WORK=<folder outside the repo> python3 -I c1_reunion.py <path to scripts/sons/audit2>"""
import contextlib, io, os, shutil, sys
sys.path.insert(0, sys.argv[1])
from lib import *
import lib

COLS = ["type", "subtype", "role", "style", "bpm_tag", "bpm_est", "bpm_gap_pct", "key_tag", "key_est",
        "key_match", "note_tag", "bars", "end_cut", "scale_fit", "file_id"]
N = [0]


def pack(files, name=None, trace=False):
    """files: {rel: samples | ("midi", notes, bpm)}; name: the pack folder's own name."""
    N[0] += 1
    root = os.path.join(lib.S, "pack", "c1", "%03d" % N[0], *( [name] if name else []))
    shutil.rmtree(os.path.dirname(root) if name else root, ignore_errors=True)
    for rel, x in files.items():
        p = os.path.join(root, *rel.split("/"))
        if isinstance(x, tuple):
            write_midi(p, x[1], x[2])
        else:
            write_wav(p, x)
    rows, con, code = run_pack(root, "c1")
    print("-- %s%s  (code %s)" % ((name + " :: ") if name else "", " + ".join(files), code))
    for r in rows:
        print("     " + " | ".join("%s=%s" % (c, r.get(c)) for c in COLS if r.get(c, "") != "" or c == "type"))
        if trace and r.get("file_id"):
            buf = io.StringIO()
            with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
                try:
                    lib.tout.main([root, "--trace", r["file_id"]])
                except SystemExit:
                    pass
            print("        trace %s -> %s" % (r["file_id"], buf.getvalue().strip()[:110]))
    if not rows:
        print("     NO ROW " + con[:200])
    return rows


FMAJ = [[53, 57, 60], [58, 62, 65], [48, 52, 55], [53, 57, 60]]   # F Bb C F
FMIN = [[53, 56, 60], [58, 61, 65], [48, 51, 55], [53, 56, 60]]   # Fm Bbm Cm Fm
CMAJ = [[48, 52, 55], [53, 57, 60], [55, 59, 62], [48, 52, 55]]   # C F G C
AMAJ = [[57, 61, 64], [62, 66, 69], [52, 56, 59], [57, 61, 64]]   # A D E A

print("== A. bare tempo, the sound really at the written tempo")
pack({"Loops/Loop 85 Am 140.wav": chord_loop(140, 4)})
pack({"Loops/Loop 85 Am 140.wav": chord_loop(85, 4)})
pack({"Loops/Loop 70 Am 140.wav": chord_loop(140, 4)})
pack({"Loops/Loop 140 Am 02.wav": chord_loop(140, 4)})
pack({"Loops/Loop 58 Am.wav": chord_loop(58, 2)})
pack({"Loops/Loop 210 Am.wav": chord_loop(210, 4)})
pack({"Loops/Loop 128 Am.wav": chord_loop(125, 4)})
pack({"Loops/Synth Loop 174.wav": chord_loop(174, 4)})
for name, bpm, kind, hats in (("Drum Loop 128", 128, "four", "8th"), ("Drum Loop 90", 90, "boombap", "8th"),
                              ("Drum Loop 174", 174, "dnb", "8th"), ("Trap Loop 140", 70, "half", "16th"),
                              ("Trap Loop 70", 70, "half", "16th"), ("House Loop 124", 124, "four", "off"),
                              ("Drum Loop 100", 100, "four", "trip")):
    pack({"Drum Loops/%s.wav" % name: drum_loop(bpm, 2, kind, hats)})
print("== A2. written 'sure' tempo against a sound at another tempo (known gap iv)")
pack({"Drum Loops/Drum Loop 128 bpm.wav": drum_loop(100, 2)})
pack({"Loops/Pad Loop 140bpm Am.wav": chord_loop(100, 4)})

print("== B. lone key letter, the sound really in that key")
pack({"Loops/Pad Loop F.wav": chord_loop(120, 4, FMAJ)})
pack({"Loops/Pad Loop F.wav": chord_loop(120, 4, FMIN)})
pack({"Loops/Pad Loop [F].wav": chord_loop(120, 4, FMAJ)})
pack({"Loops/Piano Loop C 120.wav": chord_loop(120, 4, CMAJ)})
pack({"Loops/Piano Loop C.wav": chord_loop(120, 4)})            # Am F C G: C major or A minor
pack({"Loops/Synth Loop 120 A.wav": chord_loop(120, 4, AMAJ)})
pack({"Loops/Pad Loop Fm.wav": chord_loop(120, 4, FMAJ)})       # written minor, sounds major
pack({"Loops/Pad Loop Bb.wav": chord_loop(120, 4, [[58, 62, 65], [51, 55, 58], [53, 57, 60], [58, 62, 65]])})

print("== C. style from the pack folder's name")
L = chord_loop(140, 2)
pack({"Loops/Loop 01 140 bpm.wav": L}, "Drill Essentials Vol 1")
pack({"House Loops/Loop 01 140 bpm.wav": L}, "Drill Essentials Vol 1")
pack({"Loops/Loop 01 120 bpm.wav": chord_loop(120, 2), "Kicks/Kick 01.wav": kick()}, "Hat Tricks Vol 1")
for nm in ("House of Loops Vol 3", "Rock Solid Kicks", "Metal Percussion", "Pop Up Drums", "Jungle Animals FX",
           "Soul Food Keys", "Trap Soul Keys", "Garage Band Loops", "Future Bass Starter", "Ambient Textures",
           "Drum Hits 2", "Techno Tools", "archive_03", "LoFi Hip Hop Pack", "Hardstyle Kicks", "Acid Jazz"):
    r = pack({"Loops/Loop 01 120 bpm.wav": chord_loop(120, 2)}, nm)
print("   parent folder given instead of the pack (name inside the path)")
pack({"Hat Tricks Vol 1/Loops/Loop 01 120 bpm.wav": chord_loop(120, 2), "Drill Essentials Vol 1/Kicks/Kick 01.wav": kick()})

print("== D. kits")
D128, P128 = drum_loop(128, 4), chord_loop(128, 4, FMAJ)
B128 = chord_loop(128, 4, [[29], [34], [36], [29]], pluck=False)
MID = ("midi", [(i, 1, 53 + (i % 3) * 4, 100, 0) for i in range(16)], None)
pack({"Kits/Kit 01 - 128 F/Drums.wav": D128, "Kits/Kit 01 - 128 F/Pad.wav": P128, "Kits/Kit 01 - 128 F/Bass.wav": B128,
      "Kits/Kit 01 - 128 F/Kit 01.mid": MID}, trace=True)
pack({"Kits/Kit 02/Drums.wav": D128, "Kits/Kit 02/Pad.wav": P128, "Kits/Kit 02/Bass.wav": B128, "Kits/Kit 02/Kit 02.mid": MID})
pack({"Kits/Kit 03 - 100 F/Drums.wav": D128, "Kits/Kit 03 - 100 F/Pad.wav": P128, "Kits/Kit 03 - 100 F/Bass.wav": B128})
pack({"Kits/Kit 04 - 128 bpm Fm/Drums.wav": D128, "Kits/Kit 04 - 128 bpm Fm/Pad.wav": P128})
pack({"Kits/Kit 05 - 64 F/Drums.wav": D128, "Kits/Kit 05 - 64 F/Pad.wav": P128})

print("== E. defect 18: one-shots with a tempo in the name or a 'full' word")
B = b808(65.4, 1.6)
pack({"808/808 Long 140bpm C.wav": B})
pack({"808/808 Long 140bpm C.wav": B[: len(B) // 4]})
pack({"Drums/Kick Full.wav": kick()})
pack({"Drums/Kick Full.wav": kick()[: int(0.05 * SR)]})
T1 = fade(tone(130.8, 1.5, 0.4), 30)
pack({"One Shots/Bass C2 120bpm.wav": T1})
pack({"One Shots/Bass C2 120bpm.wav": tone(130.8, 1.5, 0.4)})
pack({"One Shots/Bass C2.wav": tone(130.8, 1.5, 0.4)})
pack({"One Shots/Full Bass C2.wav": tone(130.8, 1.5, 0.4)})

print("== F. odd ones")
pack({"Synth/Bass 120 C.wav": fade(tone(65.4, 2.0, 0.4), 30)})
pack({"Loops/Top Loop 01.wav": fade(tone(220, 6.0, 0.3), 30)})
pack({"Bass/Bass A#.wav": fade(tone(116.54, 1.5, 0.4), 30)})
pack({"Bass/Bass A#.wav": fade(tone(110.0, 1.5, 0.4), 30)})
pack({"Leads/Lead C.wav": fade(tone(277.18, 1.5, 0.4), 30)})   # written C, sounds C#
