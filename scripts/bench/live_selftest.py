#!/usr/bin/env python3
"""ps_live's own check (scripts/live-bench.sh runs it first): a sound that plays on is not flagged,
an isolated discontinuity is a CLICK, a mix turned off while the transport runs is SILENCE, a heap
call inside the audio callback is ALLOC (ps_live's own probe; not counted in a sanitizer build).

    python3 -I scripts/bench/live_selftest.py <ps_live> <work dir>
"""
import math
import os
import struct
import subprocess
import sys


def sample(path, cut):
    """0.3 s of a 100 Hz sine at 0.4; `cut`: its phase jumps half a turn at 0.15 s (a click)."""
    n = int(48000 * 0.3)
    x = [0.4 * math.sin(2 * math.pi * 100 * i / 48000 + (math.pi if cut and i >= 7320 else 0.0)) for i in range(n)]
    with open(path, "wb") as f:
        f.write(struct.pack("<%df" % n, *x))


def run(ps_live, work, name, lines, seconds, extra_args=(), stdout=None):
    path = os.path.join(work, name + ".pslive")
    with open(path, "w") as f:
        f.write("PSCMD 1\n" + "\n".join(lines) + "\n")
    out = subprocess.run([ps_live, "--quiet", "--seconds", str(seconds), *extra_args, path], capture_output=True, text=True)
    if out.returncode not in (0, 4):
        sys.exit(f"ps_live failed on {name}: {out.stderr}")
    if stdout is not None:
        stdout.extend(out.stdout.splitlines())
    return [l[5:] for l in out.stdout.splitlines() if l.startswith("FLAG ")]


def main():
    ps_live, work = sys.argv[1], sys.argv[2]
    os.makedirs(os.path.join(work, "samples"), exist_ok=True)
    sample(os.path.join(work, "samples", "clean.f32"), False)
    sample(os.path.join(work, "samples", "cut.f32"), True)
    # 120 BPM, a 4-bar pattern; commands as the app numbers them (NativeEngine: 5 transport, 6
    # tempo, 9 pattern steps, 10 drum hit, 20 channel volume).
    # Pad 5: the clean sine on every other step; pad 6: the cut one, on the steps given.
    def song(cut_hits=(), extra=()):
        return ["C 6 120.0 0 0", "C 9 0.0 64 0", "SAMPLE 1 1 48000 samples/clean.f32", "PAD 5 1",
                "SAMPLE 2 1 48000 samples/cut.f32", "PAD 6 2"] + \
               [f"C 10 1.0 5 {h}" for h in range(0, 64, 2)] + [f"C 10 1.0 6 {h}" for h in cut_hits] + ["C 5 1.0 0 0"] + list(extra)
    failures = []
    clean = run(ps_live, work, "clean", song(), 12)
    if clean:
        failures.append(f"a sound playing on was flagged: {clean}")
    # Once in the 8 s pattern, the click alone (its pad sounds over the steady one).
    click = run(ps_live, work, "click", song([21]), 7)
    if not any(f.startswith("CLICK") for f in click):
        failures.append(f"an isolated discontinuity was not flagged: {click}")
    # The steady sound passed with no heap call in its callbacks; one heap call is seen.
    clean_out = []
    run(ps_live, work, "clean-alloc", song(), 3, stdout=clean_out)
    if any(l.startswith("ALLOC news=") for l in clean_out):
        probe = run(ps_live, work, "alloc", song(), 3, ["--alloc-probe"])
        if not any(f.startswith("ALLOC") for f in probe):
            failures.append(f"a heap call in the audio callback was not flagged: {probe}")
    silence = run(ps_live, work, "silence", song(extra=["WAIT ms 6000", "C 20 0.0 0 0"]), 12)
    if not any(f.startswith("SILENCE") for f in silence):
        failures.append(f"the mix turned off while playing was not flagged: {silence}")
    if failures:
        print("ps_live self-test FAILED:\n  " + "\n  ".join(failures))
        sys.exit(1)
    print("ps_live self-test: a steady sound passes, a click, a silence and a heap call in the callback are flagged")


if __name__ == "__main__":
    main()
