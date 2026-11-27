import os, sys, time, struct, shutil
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *
import lib

B = os.path.join(S, "pack", "b6")
shutil.rmtree(B, ignore_errors=True)

chk = Check("b6_misc")
FMT = {9999.7: "9999.7", 9999.4: "9999.4", 10000.2: "10000.2", 0.00001234: "1.234e-05", 123.45: "123.45",
       133.33: "133.33", 0.5: "0.5", -0.0: "0", 1e-7: "1e-07", 44100.0: "44100.0",
       "-": "-", "-A1": "'-A1", "=1+1": "'=1+1", "+x": "'+x", "@x": "'@x", "A# major": "A# major",
       "i-VI-III-VII": "i-VI-III-VII", ".-.-I": ".-.-I"}
for v, want in FMT.items():
    chk.eq("_fmt %r" % (v,), tout._fmt(v), want)
chk.eq("_fmt nan", tout._fmt(float("nan")), "")
chk.eq("_fmt float32 0.1", tout._fmt(np.float32(0.1)), "0.1")
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
    chk.run(tag + " run", rows, code)
    return rows

print("== 2. --out in a missing folder (when does it fail?)")
root = os.path.join(B, "outmiss")
for i in range(6):
    write_wav(os.path.join(root, "Drums", "Kicks", "Kick %02d.wav" % i), kick(), SR, bits=24)
t0 = time.time()
try:
    code = tout.main([root, "--rar", "1", "--out", os.path.join(S, "out", "no_such_dir", "x")])
    print("  returned", code)
    chk.err("missing --out exit", "returned %s, no SystemExit" % code)
except SystemExit as e:
    print("  SystemExit", e.code, "after %.2f s" % (time.time() - t0))
    chk.eq("missing --out exit", e.code, 2)
    chk.ok("missing --out fails before measuring", time.time() - t0 < 1.0, "%.2f s" % (time.time() - t0))
except Exception as e:
    print("  CRASH %s: %s after %.2f s (all files measured first)" % (type(e).__name__, str(e)[:90], time.time() - t0))
    chk.err("missing --out exit", "CRASH %s (defect 27)" % type(e).__name__)

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
files = sorted(os.path.basename(f) for f in glob.glob(out + "_*.csv"))
print("  files after run 2:", files)
chk.eq("stale tables removed", files, ['stale_accuracy.csv', 'stale_drum_oneshot.csv'])

print("== 4. non-UTF-8 names")
root = os.path.join(B, "badname")
os.makedirs(os.path.join(root, "Drums", "Kicks"), exist_ok=True)
write_wav(os.path.join(root, "Drums", "Kicks", "tmp.wav"), kick(), SR, bits=24)
os.rename(os.path.join(root, "Drums", "Kicks", "tmp.wav").encode(), os.path.join(root, "Drums", "Kicks").encode() + b"/Kick \xe9t\xe9 01.wav")
with open(os.path.join(root).encode() + b"/notes.t\xe9t", "wb") as fh: fh.write(b"x")
r = table(root, "badname", ["type", "subtype", "role", "end_cut", "error"])
chk.eq("badname row", [(x["type"], x["subtype"], x["error"]) for x in r], [("drum_oneshot", "kick", "")])

print("== 5. style only in the pack root folder name")
root = os.path.join(B, "Techno Essentials Vol 2")
write_wav(os.path.join(root, "Kicks", "Kick 01.wav"), kick(), SR, bits=24)
r = table(root, "root1", ["type", "subtype", "role", "style"])
chk.eq("root1 style", r and r[0]["style"], "techno")
print("  (same pack passed by its parent folder)")
root = os.path.join(B, "parent")
write_wav(os.path.join(root, "Techno Essentials Vol 2", "Kicks", "Kick 01.wav"), kick(), SR, bits=24)
r = table(root, "root2", ["type", "subtype", "role", "style"])
chk.eq("root2 style", r and r[0]["style"], "techno")
root = os.path.join(B, "Hat Tricks Vol 1")   # drum word only in the root: must NOT leak
write_wav(os.path.join(root, "Loops", "Loop 01 120 bpm.wav"), drum_loop(120), SR, bits=24)
r = table(root, "root3", ["type", "subtype", "role", "style", "bpm_tag"])
if r:
    chk.eq("root3 type", r[0]["type"], "drum_loop")
    chk.eq("root3 style (no drum word leak)", r[0]["style"], "unlabelled")
    chk.eq("root3 bpm_tag", r[0]["bpm_tag"], "120")
root = os.path.join(B, "Trapped Souls Vol 1")   # "Souls" is not soul/rnb: style too greedy
write_wav(os.path.join(root, "Kicks", "Kick 01.wav"), kick(), SR, bits=24)
r = table(root, "root4", ["type", "subtype", "role", "style"])
if r:
    chk.ok("root4 style not from 'Souls'", r[0]["style"] != "rnb", r[0]["style"])

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
r = table(root, "acc1", ["type", "role", "dur_s", "bpm_tag", "key_tag", "bpm_est", "key_est", "bpm_gap_pct", "bars", "key_match"])
KEYN = {"Am": "A minor", "Cm": "C minor"}
chk.eq("acc1 rows", len(r), 14)
for x in r:
    tag = "acc1 %s %s" % (x["bpm_tag"] or "untagged", x["key_tag"] or x["dur_s"])
    chk.eq(tag + " type", x["type"], "melodic_loop")
    if not x["bpm_tag"]:   # untagged pads: blank is honest, a value must be the truth
        chk.near(tag + " bpm_est", x["bpm_est"], 80 if x["dur_s"] == "12" else 140, may_be_empty=True)
        chk.eq(tag + " bars", x["bars"], "4", may_be_empty=True)
        continue
    bpm = float(x["bpm_tag"])
    est = float(x["bpm_est"] or "nan")
    # an octave (half or double) is counted as such by the accuracy table
    chk.ok(tag + " bpm_est", any(abs(est - bpm * f) <= 0.02 * bpm * f for f in (1, 0.5, 2)), "est %s" % x["bpm_est"])
    chk.eq(tag + " bars", x["bars"], "4")
    chk.ok(tag + " key_tag", x["key_tag"] in KEYN.values(), x["key_tag"])
    chk.ok(tag + " key_est or flagged", x["key_est"] == x["key_tag"] and x["key_match"] == "exact"
           or x["key_est"] != x["key_tag"] and x["key_match"] == "other", "%s / %s" % (x["key_est"], x["key_match"]))
acc = open(os.path.join(S, "out", "run_acc1_accuracy.csv"), encoding="utf-8").read()
chk.ok("acc1 accuracy bpm tagged 12", ",melodic_loop,bpm,12," in acc, acc)
chk.ok("acc1 accuracy key tagged 12", ",melodic_loop,key,12," in acc, acc)

print("== 7. whole pack: false tags (synth model numbers) feed the accuracy table")
root = os.path.join(B, "acc2")
for j in range(12):
    write_wav(os.path.join(root, "Loops", "Juno 106 Chords Loop %02d Am.wav" % j),
              chord_loop(120, 4, None, pluck=True), SR, bits=24)
write_wav(os.path.join(root, "Loops", "Chords Loop 99.wav"), chord_loop(120, 4, None, pluck=True), SR, bits=24)
r = table(root, "acc2", ["type", "role", "bpm_tag", "key_tag", "bpm_est", "key_est", "bpm_gap_pct", "bars"])
chk.eq("acc2 rows", len(r), 13)
chk.eq("acc2 no bpm from 106", sorted(set(x["bpm_tag"] for x in r)), [""])
chk.eq("acc2 key_tag A minor", sum(x["key_tag"] == "A minor" for x in r), 12)
for k, x in enumerate(r):
    # untagged loop: key_est stays blank while the key measure is not usable (12/12 is not enough)
    chk.eq("acc2 %d key_est" % k, x["key_est"], "A minor", may_be_empty=not x["key_tag"])
    chk.eq("acc2 %d bars" % k, x["bars"], "4", may_be_empty=True)
acc = open(os.path.join(S, "out", "run_acc2_accuracy.csv"), encoding="utf-8").read()
# gate: one-sided 95 % Wilson upper bound of the error rate under 5 % (audit 4, A7)
chk.ok("acc2 accuracy key 12/12 not yet usable", "1,melodic_loop,key,12,12,0,0,0,no" in acc, acc)
chk.ok("Wilson gate 60/60 open", tout._wilson_upper(0, 60) < 0.05, tout._wilson_upper(0, 60))
chk.ok("Wilson gate 59/60 closed", tout._wilson_upper(1, 60) >= 0.05, tout._wilson_upper(1, 60))
chk.ok("Wilson gate 12/12 closed", tout._wilson_upper(0, 12) >= 0.05, tout._wilson_upper(0, 12))

print("== 8. kits: untagged kit, bpm_est/key_est of the kit row")
root = os.path.join(B, "kit1")
kd = os.path.join(root, "Construction Kits", "Kit 01")
write_wav(os.path.join(kd, "Drums.wav"), drum_loop(100, 4), SR, bits=24)
write_wav(os.path.join(kd, "Pad.wav"), chord_loop(100, 4, None, pluck=False), SR, bits=24)
write_wav(os.path.join(kd, "Bass.wav"), chord_loop(100, 4, [[33], [29], [31], [28]], pluck=True), SR, bits=24)
r = table(root, "kit1", ["type", "role", "bpm_tag", "key_tag", "bpm_est", "key_est", "bars", "n_stems", "roles"])
k = [x for x in r if x["type"] == "kit"]
if chk.ok("kit1 one kit row", len(k) == 1, str(len(k))):
    chk.near("kit1 kit bpm_est", k[0]["bpm_est"], 100, may_be_empty=True)
    chk.eq("kit1 kit bars", k[0]["bars"], "4", may_be_empty=True)
    chk.eq("kit1 kit n_stems", k[0]["n_stems"], "3")
    chk.eq("kit1 kit roles", k[0]["roles"], "bass+drums+pad")
root = os.path.join(B, "kit2")
kd = os.path.join(root, "Construction Kits", "Kit 02 - 100 bpm Am")
write_wav(os.path.join(kd, "Pad.wav"), chord_loop(80, 4, None, pluck=False), SR, bits=24)
write_wav(os.path.join(kd, "Pad 2.wav"), chord_loop(80, 4, None, pluck=False), SR, bits=24)
r = table(root, "kit2", ["type", "role", "bpm_tag", "key_tag", "bpm_est", "key_est", "bpm_gap_pct", "bars", "n_stems"])
chk.eq("kit2 types", sorted(x["type"] for x in r), ["kit", "kit_stem", "kit_stem"])
for k, x in enumerate(r):
    tag = "kit2 %s%s" % (x["type"], "" if x["type"] == "kit" else " %d" % k)
    chk.near(tag + " bpm_est", x["bpm_est"], 80, may_be_empty=True)
    chk.eq(tag + " key", x["key_tag"], "A minor")
    # 4 true bars at 80 bpm; counted from the written 100 they become 5 (absurd bars)
    chk.eq(tag + " bars", x["bars"], "4", may_be_empty=True)

chk.clean_repo()
chk.done()
