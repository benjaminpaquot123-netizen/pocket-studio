#!/usr/bin/env python3
"""Checks report.py's hard limits on two tiny measures: one within them, one outside (the CI runs
it: `python3 -I scripts/bench/test_report.py`). Standard library only."""
import json
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))


def measure(style, **over):
    m = {
        "style": style, "seconds": 120.0, "lufs_i": -8.0, "lra": 4.0, "true_peak": -1.2, "true_peak_bs1770": -1.3, "sample_peak": -1.4,
        "plr": 6.8, "clipped": 0, "non_finite": 0, "dc": 0.0002, "end_dbfs": -60.0, "main": "Drop", "main_lufs": -7.8, "main_true_peak": -1.2,
        "main_crest": 7.5, "mono_loss_lu": -0.1, "side_mid": -20.0, "corr": 0.98, "side_mid_below_120": -30.0, "corr_below_120": 0.999,
        "bands": {"sub": -2.0, "low": -6.0, "low_mid": -12.0, "mid": -12.0, "high_mid": -18.0, "air": -20.0}, "slope_db_oct": -4.8,
        "drop_minus_groove": 0.5, "drop_minus_break": 2.0, "sections": [], "labels": {},
    }
    m.update(over)
    return m


def run(measures, renders=None):
    with tempfile.TemporaryDirectory() as d:
        os.mkdir(os.path.join(d, "json"))
        for m in measures:
            with open(os.path.join(d, "json", m["style"] + ".json"), "w") as f:
                json.dump(m, f)
        args = [sys.executable, "-I", os.path.join(HERE, "report.py"), os.path.join(d, "json"), "--out", os.path.join(d, "r.md"),
                "--json", os.path.join(d, "b.json")]
        if renders:
            os.mkdir(os.path.join(d, "render"))
            for style, r in renders.items():
                with open(os.path.join(d, "render", style + ".json"), "w") as f:
                    json.dump(r, f)
            args += ["--renders", os.path.join(d, "render")]
        p = subprocess.run(args, capture_output=True, text=True)
        return p.returncode, p.stderr.strip(), open(os.path.join(d, "r.md")).read() if os.path.exists(os.path.join(d, "r.md")) else ""


def main():
    failures = 0

    def check(name, cond):
        nonlocal failures
        print(("  ok   " if cond else "  FAIL ") + name)
        failures += not cond

    code, err, md = run([measure("house"), measure("techno", lufs_i=-7.3)])
    check("within the limits: exit 0", code == 0 and err == "")
    check("the report names the criteria", "True peak ≤ −1,0 dBTP (bloquant) | 2 sur 2" in md)

    code, err, _ = run([measure("house", true_peak=-0.6)])
    check("true peak over -1 dBTP fails", code == 1 and "True peak" in err and "house" in err)
    code, err, _ = run([measure("house", dc=0.004)])
    check("DC over 1e-3 fails", code == 1 and "Continu" in err)
    code, err, _ = run([measure("house", non_finite=3)])
    check("a NaN fails", code == 1 and "NaN" in err)
    code, err, _ = run([measure("house", lufs_i=-10.0)])
    check("2 LU under the target fails", code == 1 and "LUFS" in err and "house" in err)
    # Under the target but the chain already took 6 LU off: capped, not broken.
    code, err, md = run([measure("riddim", lufs_i=-9.2)], {"riddim": {"lufs": -9.2, "drive_db": 12.0, "music_lufs": -15.0, "true_peak_db": -1.2}})
    check("capped by the chain's loss: exit 0, listed as capped", code == 0 and "plafonné" in md)
    code, err, _ = run([measure("riddim", lufs_i=-9.2)], {"riddim": {"lufs": -9.2, "drive_db": 8.0, "music_lufs": -15.0, "true_peak_db": -1.2}})
    check("under the target with room left fails", code == 1 and "LUFS" in err)
    code, _, md = run([measure("house", lufs_i=-7.2)], {"house": {"lufs": -7.2, "drive_db": 3.0, "music_lufs": -9.0, "true_peak_db": -1.2}})
    check("the drive and the loss are in the table", "| +3,0 | 1,2 |" in md)

    print("ALL PASSED" if not failures else f"{failures} FAILURE(S)")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
