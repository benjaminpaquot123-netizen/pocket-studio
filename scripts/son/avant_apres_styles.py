#!/usr/bin/env python3
"""Helper of scripts/son/avant_apres_styles.sh: the morning's before/after MP3s, one per style.

  avant_apres_styles.py candidates ROOT BEFORE_REV AFTER_REV [--max N]
      The styles whose sound may change between the two commits, one per line (id<TAB>why):
      the styles whose recipe (StyleRecipes.kt, generated from scripts/packs/recipes) differs or is new,
      then, when the lot also changes code that every song goes through (engine, model, app), varied
      styles (one per genre) up to N in all (default 8). --max caps the whole list (the engine lot).
  avant_apres_styles.py assemble BEFORE_AUDIO BEFORE_CSV AFTER_AUDIO AFTER_CSV OUT.mp3 [--seconds 40] [--gap 1]
      One MP3: SECONDS of the song's main part before, GAP of silence, the same passage after. The
      passage starts at a part boundary of the live playback (ps_live --csv « swaps ») and is the
      loudest such window: the Drop or Refrain, not the intro. Never normalised: each half keeps the
      level the app plays it at. When the two passages are the same samples, nothing is written and the
      JSON says « identique ». Prints one JSON line.
  avant_apres_styles.py measure OUT.mp3 [--seconds 40] [--gap 1]
      Checks an MP3 as written: total length, the silence in the middle, neither half muted nor clipped;
      prints one JSON line (levels of each half in LUFS and dBFS) and exits 1 when a check fails.

Audio is read through ffmpeg (any format it decodes: the bench's float WAVs, FLACs, MP3s).
"""
import csv
import json
import math
import re
import subprocess
import sys

import numpy as np

RATE = 48000
RECIPES = "project-model/src/main/kotlin/com/pocketstudio/model/StyleRecipes.kt"
# Code every song goes through: a change there may change any style's sound.
CODE_PREFIXES = ("engine/src/main/cpp/", "project-model/src/main/kotlin/", "app/src/main/java/")
CODE_IGNORED = (RECIPES, "project-model/src/main/kotlin/com/pocketstudio/model/tuto/")


def git(root, *args):
    return subprocess.run(["git", "-C", root, *args], check=True, capture_output=True, text=True).stdout


def style_blocks(text):
    """id -> (genre, block text) in the file's order."""
    out = {}
    starts = [m.start() for m in re.finditer(r"^    Style\(\n", text, re.M)]
    for i, s in enumerate(starts):
        block = text[s:starts[i + 1] if i + 1 < len(starts) else len(text)]
        lits = re.findall(r'"((?:[^"\\]|\\.)*)"', block)
        if len(lits) >= 3:
            out[lits[0]] = (lits[2], block)
    return out


def candidates(root, before, after, cap):
    a = style_blocks(git(root, "show", f"{before}:{RECIPES}"))
    b = style_blocks(git(root, "show", f"{after}:{RECIPES}"))
    rows = [(s, "recette") for s in b if s not in a or a[s][1] != b[s][1]]
    changed = [p for p in git(root, "diff", "--name-only", before, after).splitlines()
               if p.startswith(CODE_PREFIXES) and not p.startswith(CODE_IGNORED)]
    if changed:
        # Varied styles: one per genre first, then a second of each genre, until 8 in all.
        for per_genre in (1, 2):
            count = {}
            for s, _ in rows:
                count[b[s][0]] = count.get(b[s][0], 0) + 1
            for s, (genre, _) in b.items():
                if len(rows) >= 8:
                    break
                if count.get(genre, 0) < per_genre and all(s != r[0] for r in rows):
                    rows.append((s, "code"))
                    count[genre] = count.get(genre, 0) + 1
    if cap:
        rows = rows[:cap]
    return rows


def read_audio(path):
    """Stereo float32 (frames, 2) at 48 kHz, as ffmpeg decodes the file."""
    raw = subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-i", path, "-f", "f32le", "-ac", "2", "-ar", str(RATE), "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype="<f4").reshape(-1, 2)


def read_csv(path):
    return [r for r in csv.DictReader(open(path, encoding="utf-8"))]


def boundaries(rows):
    """Seconds (float) where the playback is at a part start: ▶ itself, then each scene swap."""
    out = []
    prev_running, prev_swaps = 0, 0
    for r in rows:
        t, running, swaps = int(r["second"]), int(r["running"]), int(r["swaps"])
        if running and not prev_running:
            out.append(float(t))
        elif running and swaps > prev_swaps:
            out.append(float(t))
        prev_running, prev_swaps = running, swaps
    return out


def window_level(rows, start, seconds):
    vals = [float(r["rms_db"]) for r in rows if start <= int(r["second"]) < start + seconds]
    return sum(vals) / len(vals) if vals else -200.0


def choose_start(rows, frames, seconds):
    """The part boundary (seconds) whose next SECONDS are the loudest, half a second early so the
    downbeat is in; the first boundary when the song is too short for any later one."""
    last = frames / RATE - seconds
    cands = [b for b in boundaries(rows) if b - 0.5 <= last] or [0.5]
    best = max(cands, key=lambda b: window_level(rows, int(b), seconds))
    return max(0.0, best - 0.5)


def db(x):
    return 20.0 * math.log10(max(x, 1e-12))


def rms(x):
    return float(np.sqrt(np.mean(np.square(x, dtype=np.float64)))) if x.size else 0.0


def edge_fade(x, ms=10):
    n = min(len(x) // 2, int(RATE * ms / 1000))
    if n > 0:
        ramp = np.linspace(0.0, 1.0, n, dtype=np.float32)[:, None]
        x = x.copy()
        x[:n] *= ramp
        x[-n:] *= ramp[::-1]
    return x


def flags_in(log, start, seconds):
    """ps_live's alerts (« FLAG KIND at N s: … ») whose second falls in the passage heard."""
    if not log:
        return []
    out = []
    for line in open(log, encoding="utf-8", errors="replace"):
        m = re.match(r"FLAG (\S+) at (\d+) s: (.*)", line.strip())
        if m and start - 1 <= int(m.group(2)) <= start + seconds:
            out.append(f"{m.group(1)} {m.group(2)} s")
        elif line.startswith("FLAG SILENCE: nothing heard"):
            out.append("SILENCE tout le morceau")
    return out


def assemble(argv):
    import argparse
    ap = argparse.ArgumentParser(prog="assemble")
    ap.add_argument("before_audio")
    ap.add_argument("before_csv")
    ap.add_argument("after_audio")
    ap.add_argument("after_csv")
    ap.add_argument("out")
    ap.add_argument("--seconds", type=float, default=40.0)
    ap.add_argument("--gap", type=float, default=1.0)
    ap.add_argument("--log-before", help="ps_live's output: its FLAG lines inside the passage are reported")
    ap.add_argument("--log-after")
    o = ap.parse_args(argv)
    a, b = read_audio(o.before_audio), read_audio(o.after_audio)
    ra, rb = read_csv(o.before_csv), read_csv(o.after_csv)
    n = int(o.seconds * RATE)
    sa = choose_start(ra, len(a), o.seconds)
    # The same passage after: the before's start when the after has a part boundary there too
    # (same arrangement), else the after's own loudest part.
    near = [x for x in boundaries(rb) if abs((x - 0.5) - sa) <= 1.0]
    sb = sa if near and sa * RATE + n <= len(b) else choose_start(rb, len(b), o.seconds)
    wa = a[int(sa * RATE):int(sa * RATE) + n]
    wb = b[int(sb * RATE):int(sb * RATE) + n]
    res = {"start_before": round(sa, 2), "start_after": round(sb, 2),
           "flags_before": flags_in(o.log_before, sa, o.seconds), "flags_after": flags_in(o.log_after, sb, o.seconds)}
    if len(wa) < n or len(wb) < n:
        res.update(status="trop court", frames_before=len(wa), frames_after=len(wb))
        print(json.dumps(res))
        return 2
    if sa == sb and np.array_equal(wa, wb):
        res["status"] = "identique"
        print(json.dumps(res))
        return 0
    res.update(rms_before_db=round(db(rms(wa)), 2), rms_after_db=round(db(rms(wb)), 2),
               peak_before=round(float(np.max(np.abs(wa))), 4), peak_after=round(float(np.max(np.abs(wb))), 4))
    gap = np.zeros((int(o.gap * RATE), 2), dtype=np.float32)
    pcm = np.concatenate([edge_fade(wa), gap, edge_fade(wb)]).astype("<f4")
    subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-f", "f32le", "-ar", str(RATE), "-ac", "2", "-i", "-",
                    "-c:a", "libmp3lame", "-b:a", "192k", o.out], input=pcm.tobytes(), check=True)
    res["status"] = "ok"
    print(json.dumps(res))
    return 0


def lufs(path, start, seconds):
    """Integrated loudness (LUFS) of [start, start + seconds) by ffmpeg's ebur128."""
    err = subprocess.run(["ffmpeg", "-nostdin", "-hide_banner", "-ss", f"{start}", "-t", f"{seconds}", "-i", path,
                          "-af", "ebur128", "-f", "null", "-"], capture_output=True, text=True).stderr
    m = re.findall(r"I:\s+(-?[\d.]+|-inf) LUFS", err)
    return float(m[-1]) if m and m[-1] != "-inf" else -200.0


def clipped_runs(x, level=0.999, run=3):
    """Runs of RUN or more consecutive samples (per channel) at |x| >= LEVEL: a flat top."""
    count = 0
    for ch in range(x.shape[1]):
        hot = np.abs(x[:, ch]) >= level
        if not hot.any():
            continue
        edges = np.diff(np.concatenate([[0], hot.astype(np.int8), [0]]))
        lengths = np.flatnonzero(edges == -1) - np.flatnonzero(edges == 1)
        count += int(np.sum(lengths >= run))
    return count


def measure(argv):
    import argparse
    ap = argparse.ArgumentParser(prog="measure")
    ap.add_argument("mp3")
    ap.add_argument("--seconds", type=float, default=40.0)
    ap.add_argument("--gap", type=float, default=1.0)
    o = ap.parse_args(argv)
    x = read_audio(o.mp3)
    total = len(x) / RATE
    want = 2 * o.seconds + o.gap
    h1 = x[:int(o.seconds * RATE)]
    # The silence's middle: the MP3 coder smears a few ms around its edges.
    mid = x[int((o.seconds + 0.1) * RATE):int((o.seconds + o.gap - 0.1) * RATE)]
    h2 = x[int((o.seconds + o.gap) * RATE):int((2 * o.seconds + o.gap) * RATE)]
    r = {
        "file": o.mp3, "duration_s": round(total, 3),
        "rms_before_db": round(db(rms(h1)), 2), "rms_after_db": round(db(rms(h2)), 2),
        "gap_rms_db": round(db(rms(mid)), 2),
        "peak_before": round(float(np.max(np.abs(h1))) if h1.size else 0.0, 4),
        "peak_after": round(float(np.max(np.abs(h2))) if h2.size else 0.0, 4),
        "clipped_before": clipped_runs(h1), "clipped_after": clipped_runs(h2),
        "lufs_before": lufs(o.mp3, 0, o.seconds), "lufs_after": lufs(o.mp3, o.seconds + o.gap, o.seconds),
    }
    fails = []
    if abs(total - want) > 0.1:
        fails.append(f"durée {total:.2f} s au lieu de {want:.2f} s")
    if r["gap_rms_db"] > -60.0:
        fails.append("pas de silence au milieu")
    for half in ("before", "after"):
        if r[f"rms_{half}_db"] < -40.0:
            fails.append(f"moitié {half} muette")
        if r[f"clipped_{half}"] > 0:
            fails.append(f"moitié {half} écrêtée")
    r["ok"] = not fails
    r["fails"] = fails
    print(json.dumps(r, ensure_ascii=False))
    return 0 if not fails else 1


def main(argv):
    if not argv:
        print(__doc__, file=sys.stderr)
        return 2
    cmd, rest = argv[0], argv[1:]
    if cmd == "candidates":
        cap = 0
        if "--max" in rest:
            i = rest.index("--max")
            cap = int(rest[i + 1])
            rest = rest[:i] + rest[i + 2:]
        for s, why in candidates(*rest, cap):
            print(f"{s}\t{why}")
        return 0
    if cmd == "assemble":
        return assemble(rest)
    if cmd == "measure":
        return measure(rest)
    print(f"unknown command {cmd}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
