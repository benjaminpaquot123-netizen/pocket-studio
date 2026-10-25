"""Audit 35: what the phone plays (ps_live logs), per style, against the export's sections.
usage: live.py <live log dir> <bench cmds dir> -> JSON per style on stdout
  ramp_s: seconds from the start until the live loudness gain reaches 90 % of its 90-s maximum;
  st_start / st_ramp: short-term level (dBFS) at second 4 and when the ramp ends (the Intro rising by the gain alone);
  limiter_min_db: the deepest limiter gain reading of the 90 s; drop_s: when the first Drop/Refrain starts
  (from the export's sections); st_drop: the short-term level 4 s into it (if inside the 90 s);
  st_before_drop: the short-term level 1 s before it.
"""
import csv, glob, json, math, os, sys

LOG, CMDS = sys.argv[1], sys.argv[2]
out = {}
for path in sorted(glob.glob(os.path.join(LOG, "*.csv"))):
    st = os.path.basename(path)[:-4]
    rows = list(csv.DictReader(open(path)))
    drive = [float(r["drive_db"]) for r in rows]
    mx = max(drive)
    ramp = next((int(r["second"]) for r in rows if float(r["drive_db"]) >= 0.9 * mx), None) if mx > 0.5 else 0
    short = {int(r["second"]): float(r["short_db"]) for r in rows}
    lim = min(float(r["limiter"]) for r in rows if r["running"] == "1")
    res = dict(drive_max=round(mx, 1), ramp_s=ramp, st_start=short.get(4), st_ramp=short.get(ramp) if ramp else None,
               limiter_min_db=round(20 * math.log10(max(lim, 1e-6)), 1))
    pscmd = os.path.join(CMDS, st + ".pscmd")
    if os.path.exists(pscmd):
        bpm, secs = None, []
        for line in open(pscmd, encoding="utf-8"):
            if line.startswith("SONG "):
                for kv in line.split()[1:]:
                    if kv.startswith("bpm="): bpm = float(kv[4:])
            elif line.startswith("SECTION "):
                p = line.rstrip("\n").split(" ", 3); secs.append((int(p[2]), p[3]))
        main = "Drop" if any(n.startswith("Drop") for _, n in secs) else "Refrain"
        f0 = next((f for f, n in secs if n.startswith(main)), None)
        if f0 is not None:
            step = f0 / (48000 * 60 / bpm / 4)
            r0 = next((r for r in rows if int(r["song_step"]) >= step), None)
            if r0:
                s = int(r0["second"])
                res.update(drop_s=s, st_before_drop=short.get(s - 1), st_drop=short.get(s + 4))
            else:
                res.update(drop_s=round(step * 60 / bpm / 4 + 1, 0), st_drop=None)
    out[st] = res
print(json.dumps(out, indent=0))
