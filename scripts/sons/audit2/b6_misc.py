import os, sys, time, struct, shutil
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *
import lib

B = os.path.join(S, "pack", "b6")
shutil.rmtree(B, ignore_errors=True)

print("== 1. _fmt direct")
for v in (9999.7, 9999.4, 10000.2, 0.00001234, 123.45, 133.33, 0.5, -0.0, 1e-7, 44100.0, float("nan"), np.float32(0.1), True):
    print("  %r -> %r" % (v, tout._fmt(v)))
for t in ("-", "-A1", "=1+1", "+x", "@x", "A# major", "i-VI-III-VII", ".-.-I"):
    print("  %r -> %r" % (t, tout._fmt(t)))

def table(root, tag, cols, types=None):
    rows, con, code = run_pack(root, tag)
    print("  console:", con.replace("\n", " | "), "| code", code)
    for r in rows:
        if types and r["type"] not in types:
            continue
        print("   ", " ".join("%s=%s" % (c, r.get(c, "?")) for c in cols if c in r))
    acc = os.path.join(S, "out", "run_" + tag + "_accuracy.csv")
    if os.path.exists(acc):
        for line in open(acc, encoding="utf-8").read().strip().split("\n"):
            print("    acc:", line)
    return rows

print("== 2. --out in a missing folder (when does it fail?)")
root = os.path.join(B, "outmiss")
for i in range(6):
    write_wav(os.path.join(root, "Drums", "Kicks", "Kick %02d.wav" % i), kick(), SR, bits=24)
t0 = time.time()
try:
    code = tout.main([root, "--rar", "1", "--out", os.path.join(S, "out", "no_such_dir", "x")])
    print("  returned", code)
except SystemExit as e:
    print("  SystemExit", e.code, "after %.2f s" % (time.time() - t0))
except Exception as e:
    print("  CRASH %s: %s after %.2f s (all files measured first)" % (type(e).__name__, str(e)[:90], time.time() - t0))

print("== 3. stale tables when the same prefix is reused")
r1 = os.path.join(B, "stale1"); r2 = os.path.join(B, "stale2")
write_wav(os.path.join(r1, "Vocals", "Vocal 01.wav"), vox(), SR, bits=24)
write_wav(os.path.join(r2, "Drums", "Kicks", "Kick 01.wav"), kick(), SR, bits=24)
out = os.path.join(S, "out", "stale")
import glob
for f in glob.glob(out + "_*.csv"): os.remove(f)
import io, contextlib
with contextlib.redirect_stdout(io.StringIO()):
    tout.main([r1, "--rar", "1", "--out", out]); tout.main([r2, "--rar", "2", "--out", out])
print("  files after run 2:", sorted(os.path.basename(f) for f in glob.glob(out + "_*.csv")))

print("== 4. non-UTF-8 names")
root = os.path.join(B, "badname")
os.makedirs(os.path.join(root, "Drums", "Kicks"), exist_ok=True)
write_wav(os.path.join(root, "Drums", "Kicks", "tmp.wav"), kick(), SR, bits=24)
os.rename(os.path.join(root, "Drums", "Kicks", "tmp.wav").encode(), os.path.join(root, "Drums", "Kicks").encode() + b"/Kick \xe9t\xe9 01.wav")
with open(os.path.join(root).encode() + b"/notes.t\xe9t", "wb") as fh: fh.write(b"x")
table(root, "badname", ["type", "subtype", "role", "end_cut", "error"])

print("== 5. style only in the pack root folder name")
root = os.path.join(B, "Techno Essentials Vol 2")
write_wav(os.path.join(root, "Kicks", "Kick 01.wav"), kick(), SR, bits=24)
table(root, "root1", ["type", "subtype", "role", "style"])
print("  (same pack passed by its parent folder)")
root = os.path.join(B, "parent")
write_wav(os.path.join(root, "Techno Essentials Vol 2", "Kicks", "Kick 01.wav"), kick(), SR, bits=24)
table(root, "root2", ["type", "subtype", "role", "style"])
root = os.path.join(B, "Hat Tricks Vol 1")   # drum word only in the root: must NOT leak
write_wav(os.path.join(root, "Loops", "Loop 01 120 bpm.wav"), drum_loop(120), SR, bits=24)
table(root, "root3", ["type", "subtype", "role", "style", "bpm_tag"])

print("== 6. whole pack: the accuracy fence on melodic loops")
root = os.path.join(B, "acc1")
keys = [("Am", [57, 60, 64]), ("Cm", [60, 63, 67]), ("Dm", [62, 65, 69]), ("Em", [64, 67, 71]), ("Fm", [65, 68, 72])]
i = 0
for bpm in (100, 120, 128, 140, 90, 110):
    for kn, tri in keys[:2]:
        i += 1
        ch = [tri, [tri[0] + 5, tri[1] + 5, tri[2] + 5], [tri[0] + 7, tri[1] + 7, tri[2] + 7], tri]
        write_wav(os.path.join(root, "Loops", "Synth Loop %02d %d bpm %s.wav" % (i, bpm, kn)),
                  chord_loop(bpm, 4, ch, pluck=True), SR, bits=24)
# untagged, hard: a drumless pad at 80 bpm in A minor
write_wav(os.path.join(root, "Loops", "Pad Loop 01.wav"),
          chord_loop(80, 4, [[57, 60, 64], [53, 57, 60], [55, 59, 62], [52, 55, 59]], pluck=False), SR, bits=24)
write_wav(os.path.join(root, "Loops", "Pad Loop 02.wav"),
          chord_loop(140, 4, [[57, 60, 64], [53, 57, 60], [55, 59, 62], [52, 55, 59]], pluck=False), SR, bits=24)
table(root, "acc1", ["type", "role", "dur_s", "bpm_tag", "key_tag", "bpm_est", "key_est", "bpm_gap_pct", "bars", "key_match"])

print("== 7. whole pack: false tags (synth model numbers) feed the accuracy table")
root = os.path.join(B, "acc2")
for j in range(12):
    write_wav(os.path.join(root, "Loops", "Juno 106 Chords Loop %02d Am.wav" % j),
              chord_loop(120, 4, None, pluck=True), SR, bits=24)
write_wav(os.path.join(root, "Loops", "Chords Loop 99.wav"), chord_loop(120, 4, None, pluck=True), SR, bits=24)
table(root, "acc2", ["type", "role", "bpm_tag", "key_tag", "bpm_est", "key_est", "bpm_gap_pct", "bars"])

print("== 8. kits: untagged kit, bpm_est/key_est of the kit row")
root = os.path.join(B, "kit1")
kd = os.path.join(root, "Construction Kits", "Kit 01")
write_wav(os.path.join(kd, "Drums.wav"), drum_loop(100, 4), SR, bits=24)
write_wav(os.path.join(kd, "Pad.wav"), chord_loop(100, 4, None, pluck=False), SR, bits=24)
write_wav(os.path.join(kd, "Bass.wav"), chord_loop(100, 4, [[33], [29], [31], [28]], pluck=True), SR, bits=24)
table(root, "kit1", ["type", "role", "bpm_tag", "key_tag", "bpm_est", "key_est", "bars", "n_stems", "roles"])
root = os.path.join(B, "kit2")
kd = os.path.join(root, "Construction Kits", "Kit 02 - 100 bpm Am")
write_wav(os.path.join(kd, "Pad.wav"), chord_loop(80, 4, None, pluck=False), SR, bits=24)
write_wav(os.path.join(kd, "Pad 2.wav"), chord_loop(80, 4, None, pluck=False), SR, bits=24)
table(root, "kit2", ["type", "role", "bpm_tag", "key_tag", "bpm_est", "key_est", "bpm_gap_pct", "bars", "n_stems"])
