#!/usr/bin/env python3
"""« Endurance » on Linux (docs/TESTS-REELS.md): reads the soak's ps_live log and checks what a
long play must keep, then checks that ps_live's DRIFT alert works on that very recording.

    python3 -I scripts/bench/soak_check.py <ps_live> <soak dir> <style> <seconds> --out report.md

The soak (scripts/soak.sh) plays a long song looped for <seconds>, its parts staged by the app
each time round. From <soak dir>/log/<style>.txt it requires: the song looped at least 3 whole
times (DRIFT compares each loop with the second one), no flag (FADE, SILENCE, CLICK, NAN, GAIN,
ALLOC: no heap call in tens of thousands of callbacks, DRIFT: no loop 1 dB away from the second),
every callback counted. Then the self-test: the same recording with the drums' channel muted
from the third time round must be flagged DRIFT (a check that never fires checks nothing).
Standard library only; exit 1 on a failure.
"""
import argparse
import os
import re
import subprocess
import sys


def parts_per_loop(pslive):
    with open(pslive) as f:
        for line in f:
            m = re.match(r"SONG bpm=\S+ parts=(\d+)", line)
            if m:
                return int(m.group(1))
    sys.exit(f"{pslive}: no SONG line")


def drifted_copy(pslive, out, from_swap):
    """The recording with the drums' channel (0) muted from swap [from_swap] on: right after that
    swap, and again at the end of each later part's staging (the app's own commands for the next
    part come after its swap and would set the channel back)."""
    n = 0
    on = False
    with open(pslive) as f, open(out, "w") as g:
        for line in f:
            m = re.match(r"WAIT swaps (\d+)", line)
            if m and on:
                g.write(MUTE_DRUMS)
                n += 1
            g.write(line)
            if m and int(m.group(1)) >= from_swap and not on:
                on = True
                g.write(MUTE_DRUMS)
                n += 1
    return n


# SetChannelMute (Engine.h: CommandType 22), value 1, channel 0 (the drums).
MUTE_DRUMS = "C 22 1.0 0 0\n"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("ps_live")
    ap.add_argument("dir")
    ap.add_argument("style")
    ap.add_argument("seconds", type=float)
    ap.add_argument("--out", required=True)
    ap.add_argument("--no-selftest", action="store_true")
    args = ap.parse_args()
    failures = []
    log = open(os.path.join(args.dir, "log", args.style + ".txt")).read().splitlines()
    flags = [l[5:] for l in log if l.startswith("FLAG ")]
    loops_line = next((l for l in log if l.startswith("LOOPS ")), "LOOPS 0:")
    loops = int(loops_line.split()[1].rstrip(":"))
    alloc = next((l for l in log if l.startswith("ALLOC")), "")
    cpu = next((l for l in log if l.startswith("CPU ")), "")
    blocks = int(re.search(r"blocks=(\d+)", cpu).group(1)) if cpu else 0
    want_blocks = int(args.seconds * 48000 / 96)
    if flags:
        failures.append(f"{len(flags)} flag(s): {flags[:5]}")
    if loops < 3:
        failures.append(f"the song looped {loops} whole time(s): at least 3 are needed to compare them")
    # The heap calls must have been counted (a sanitizer build of ps_live cannot: not a soak).
    if not alloc.startswith("ALLOC news="):
        failures.append(f"the audio callback's heap calls were not counted: {alloc or 'no ALLOC line'}")
    elif not alloc.startswith("ALLOC news=0 deletes=0"):
        failures.append(alloc)
    if blocks < want_blocks:
        failures.append(f"{blocks} callbacks for {want_blocks} expected")
    loop_rows = re.findall(r" (\d+)-(\d+)s (-?[0-9.]+)dB/([+-][0-9.]+)dB", loops_line)

    selftest = "non lancé"
    if not args.no_selftest:
        pslive = os.path.join(args.dir, "cmds", args.style + ".pslive")
        n = parts_per_loop(pslive)
        bad = os.path.join(args.dir, "drift-selftest.pslive")
        changed = drifted_copy(pslive, bad, 2 * n)
        # Three whole loops and a bit: the third one, drums off, against the second.
        if loop_rows:
            loop_s = int(loop_rows[0][1]) - int(loop_rows[0][0]) + 1
        else:
            loop_s = args.seconds / 3
        seconds = min(args.seconds, 3 * loop_s + 20)
        r = subprocess.run([args.ps_live, "--quiet", "--seconds", str(int(seconds)), bad], capture_output=True, text=True)
        drift = [l for l in r.stdout.splitlines() if l.startswith("FLAG DRIFT")]
        if changed == 0 or not drift:
            failures.append(f"self-test: the drums muted from loop 3 ({changed} commands) were not flagged DRIFT: {r.stdout.splitlines()[-3:]}")
            selftest = "ÉCHEC"
        else:
            selftest = f"ok ({drift[0][5:]})"

    md = [f"# Endurance sur Linux : {args.style}, {args.seconds / 60:.0f} min", ""]
    md.append(("OK" if not failures else "**ÉCHEC**") + f" : {loops} tours entiers du morceau, {blocks} rappels audio, {len(flags)} alerte(s).")
    md.append("")
    md.append(f"- Allocations dans le rappel audio : {alloc or 'non mesuré'}")
    md.append(f"- Temps du fil audio : {cpu or 'non mesuré'}")
    md.append(f"- Contrôle de la dérive (batterie muette dès le 3e tour, doit être signalée) : {selftest}")
    if loop_rows:
        md += ["", "| tour | secondes | niveau (dBFS) | gain live (dB) |", "|---|---|---|---|"]
        for i, (a, b, level, drive) in enumerate(loop_rows):
            md.append(f"| {i + 1} | {a}–{b} | {level} | {drive} |")
    for f in failures:
        md.append(f"- **{f}**")
    with open(args.out, "w") as f:
        f.write("\n".join(md) + "\n")
    print("\n".join(md))
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
