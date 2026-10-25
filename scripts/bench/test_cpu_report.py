#!/usr/bin/env python3
"""Checks cpu_report.py (« Petit téléphone ») on made-up callback times: a light engine passes, a
heavy one or a recurring spike is « à risque », a spike in one run only is noise (the fastest run
of each block is kept). The CI runs it: `python3 -I scripts/bench/test_cpu_report.py`."""
import array
import json
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))


def times(burst, seconds, share, spikes=(), spike_share=0.0):
    budget = burst / 48000
    n = int(seconds * 48000 / burst)
    a = array.array("f", [budget * share] * n)
    for i in spikes:
        if i < n:
            a[i] = budget * spike_share
    return a


def write(d, style, burst, run, a):
    if sys.byteorder != "little":
        a = array.array("f", a)
        a.byteswap()
    with open(os.path.join(d, f"{style}-{burst}-{run}.f32"), "wb") as f:
        f.write(a.tobytes())


def main():
    failures = []
    with tempfile.TemporaryDirectory() as d:
        for burst in (96, 192):
            per_second = 48000 // burst
            # 10 % of the budget, steady: ok even three times slower.
            for run in (1, 2):
                write(d, "light", burst, run, times(burst, 10, 0.10))
            # 45 % steady: a phone three times slower is over budget.
            for run in (1, 2):
                write(d, "heavy", burst, run, times(burst, 10, 0.45))
            # A block at 150 % every second (a part staged): three times slower, heard each time.
            for run in (1, 2):
                write(d, "spiky", burst, run, times(burst, 10, 0.10, spikes=range(0, 10 * per_second, per_second), spike_share=1.5))
            # The same spikes in one run only: another process took the core, not the engine.
            write(d, "noisy", burst, 1, times(burst, 10, 0.10, spikes=range(0, 10 * per_second, per_second), spike_share=1.5))
            write(d, "noisy", burst, 2, times(burst, 10, 0.10))
        # Heavy only with the small callbacks: 30 % at 96 frames (×3 = 90 %), but at 96 frames a
        # spike to 80 % every second (×3 = 240 %: the margin of two bursts is gone).
        write(d, "edge", 96, 1, times(96, 10, 0.30, spikes=range(0, 5000, 500), spike_share=0.8))
        write(d, "edge", 192, 1, times(192, 10, 0.20))
        out, js = os.path.join(d, "r.md"), os.path.join(d, "r.json")
        p = subprocess.run([sys.executable, "-I", os.path.join(HERE, "cpu_report.py"), d, "--out", out, "--json", js, "--strict"],
                           capture_output=True, text=True)
        if p.returncode != 1:
            failures.append(f"--strict with styles at risk should exit 1, got {p.returncode}: {p.stderr}")
        v = {s: r["verdict"] for s, r in json.load(open(js))["styles"].items()}
        want = {"light": "ok", "heavy": "à risque", "spiky": "à risque", "noisy": "ok", "edge": "limite"}
        for style, verdict in want.items():
            if v.get(style) != verdict:
                failures.append(f"{style}: {v.get(style)} instead of {verdict}")
        md = open(out).read()
        if "2 à risque (heavy, spiky)" not in md:
            failures.append("the report's first line should name the styles at risk:\n" + md.splitlines()[2])
    if failures:
        print("cpu_report self-test FAILED:\n  " + "\n  ".join(failures))
        sys.exit(1)
    print("cpu_report self-test: light ok, heavy and spiky at risk, a noisy run ignored, edge only at 96 frames")


if __name__ == "__main__":
    main()
