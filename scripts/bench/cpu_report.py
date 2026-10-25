#!/usr/bin/env python3
"""« Petit téléphone » (docs/TESTS-REELS.md): the audio thread's time per callback, from ps_live's
--cpu-out files, against its real-time budget, on this machine and on a phone SCALE times slower.

    python3 -I scripts/bench/cpu_report.py <dir of <style>-<burst>-<run>.f32> --out report.md
        [--json cpu.json] [--scale 3] [--buffer-bursts 2] [--strict]

Each file holds one float32 per callback (seconds of this thread's CPU time). The replay is the
same at each run, so the runs of a style are merged block by block, keeping the fastest: another
process taking the core only ever makes a block slower. Then, per style and callback size: mean,
99th and 99.9th percentiles and worst block (% of the budget, burst / 48 kHz), and on the slower
phone the blocks over budget and the glitches heard with a buffer of BUFFER bursts (the late
blocks eat the buffer's margin; once it is gone the next late one is heard).

Verdict: « à risque » when the slower phone glitches (or its 99.9th percentile is over budget)
even with 192-frame callbacks; « limite » when only 96-frame callbacks do; « ok » otherwise.
--strict: exit 1 when a style is « à risque ». Standard library only.
"""
import argparse
import array
import glob
import json
import os
import re
import sys

RATE = 48000
NAME = re.compile(r"^(?P<style>.+)-(?P<burst>\d+)-(?P<run>\d+)\.f32$")


def read(path):
    a = array.array("f")
    with open(path, "rb") as f:
        a.frombytes(f.read())
    if sys.byteorder != "little":
        a.byteswap()
    return a


def merge(runs):
    """The fastest time of each block over the runs (the shortest run sets the length)."""
    n = min(len(r) for r in runs)
    return [min(r[i] for r in runs) for i in range(n)]


def percentile(sorted_times, q):
    if not sorted_times:
        return 0.0
    k = min(len(sorted_times) - 1, int(q * (len(sorted_times) - 1)))
    return sorted_times[k]


def model(times, burst, scale, buffer_bursts):
    """Stats in % of the budget, and the slower phone's late blocks and glitches."""
    budget = burst / RATE
    margin = max(0, buffer_bursts - 1) * budget
    over = glitches = 0
    backlog = 0.0
    worst, worst_at = 0.0, 0.0
    for i, t in enumerate(times):
        if t > worst:
            worst, worst_at = t, i * burst / RATE
        slow = t * scale
        if slow > budget:
            over += 1
        backlog = max(0.0, backlog + slow - budget)
        if backlog > margin:
            glitches += 1
            backlog = 0.0
    s = sorted(times)
    pct = 100.0 / budget
    mean = sum(times) / len(times) if times else 0.0
    return {
        "burst": burst, "blocks": len(times), "mean": mean * pct, "p99": percentile(s, 0.99) * pct,
        "p999": percentile(s, 0.999) * pct, "max": worst * pct, "max_at": worst_at,
        "scale": scale, "buffer": buffer_bursts, "over": over, "glitches": glitches,
        "p999_scaled": percentile(s, 0.999) * pct * scale,
    }


def risky(m):
    return m is not None and (m["glitches"] > 0 or m["p999_scaled"] > 100.0)


def verdict(by_burst):
    small, big = by_burst.get(96), by_burst.get(192)
    if risky(big) or (big is None and risky(small)):
        return "à risque"
    if risky(small):
        return "limite"
    return "ok"


def fr(x, digits=1):
    return f"{x:.{digits}f}".replace(".", ",")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--out", required=True)
    ap.add_argument("--json")
    ap.add_argument("--scale", type=float, default=3.0)
    ap.add_argument("--buffer-bursts", type=int, default=2)
    ap.add_argument("--title", default="Petit téléphone : le fil audio contre son budget")
    ap.add_argument("--machine", default="")
    ap.add_argument("--strict", action="store_true")
    args = ap.parse_args()

    files = {}
    for path in glob.glob(os.path.join(args.dir, "*.f32")):
        m = NAME.match(os.path.basename(path))
        if m:
            files.setdefault((m["style"], int(m["burst"])), []).append(path)
    styles = sorted({s for s, _ in files})
    results = {}
    for style in styles:
        for burst in sorted({b for s, b in files if s == style}):
            runs = [read(p) for p in sorted(files[(style, burst)])]
            runs = [r for r in runs if len(r)]
            if not runs:
                continue
            m = model(merge(runs), burst, args.scale, args.buffer_bursts)
            m["runs"] = len(runs)
            results.setdefault(style, {})[burst] = m

    lines = [f"# {args.title}", ""]
    if args.machine:
        lines += [f"Machine : {args.machine}.", ""]
    lines += [
        f"Temps de calcul du fil audio à chaque rappel (temps CPU du fil, le plus rapide de chaque bloc sur plusieurs passages), "
        f"en % du budget temps réel (96 trames = 2 ms, 192 trames = 4 ms à 48 kHz). Téléphone {fr(args.scale)} fois plus lent : "
        f"blocs hors budget et craquements entendus avec un tampon de {args.buffer_bursts} rappels.",
        "",
        "Verdict : « à risque » = craque même avec des rappels de 192 trames ; « limite » = seulement à 96 trames.",
        "",
        "| style | verdict | 96 : moy. | p99 | p99,9 | pire | craquements ×{0} | 192 : moy. | p99 | p99,9 | pire | craquements ×{0} |".format(fr(args.scale)),
        "|---|---|---|---|---|---|---|---|---|---|---|---|",
    ]
    risk = []
    summary = {}
    for style in styles:
        by = results.get(style, {})
        v = verdict(by)
        if v == "à risque":
            risk.append(style)
        summary[style] = {"verdict": v, "bursts": {str(b): m for b, m in by.items()}}
        cells = []
        for b in (96, 192):
            m = by.get(b)
            if m is None:
                cells += ["–"] * 5
            else:
                cells += [fr(m["mean"]) + " %", fr(m["p99"]) + " %", fr(m["p999"]) + " %",
                          f"{fr(m['max'])} % ({fr(m['max_at'])} s)", f"{m['glitches']} ({m['over']} blocs)"]
        name = f"**{style}**" if v != "ok" else style
        lines.append(f"| {name} | {v} | " + " | ".join(cells) + " |")
    ok = sum(1 for s in summary.values() if s["verdict"] == "ok")
    lines[2:2] = [f"{ok}/{len(styles)} styles ok, {len(risk)} à risque" + (f" ({', '.join(risk)})" if risk else "") + ".", ""]
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w") as f:
        f.write("\n".join(lines) + "\n")
    if args.json:
        with open(args.json, "w") as f:
            json.dump({"scale": args.scale, "buffer_bursts": args.buffer_bursts, "styles": summary}, f, indent=1, ensure_ascii=False)
    print("\n".join(lines))
    if not styles:
        print("cpu_report: no times found", file=sys.stderr)
        sys.exit(2)
    if args.strict and risk:
        sys.exit(1)


if __name__ == "__main__":
    main()
