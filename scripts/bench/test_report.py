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


def run(measures, renders=None, cmds=None):
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
        if cmds:
            os.mkdir(os.path.join(d, "cmds"))
            for style, text in cmds.items():
                with open(os.path.join(d, "cmds", style + ".pscmd"), "w") as f:
                    f.write(text)
            args += ["--cmds", os.path.join(d, "cmds")]
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
    check("2 LU under the target fails", code == 1 and "Sonie" in err and "house" in err)
    # Under the target but the chain already took 6 LU off: capped, not broken.
    code, err, md = run([measure("riddim", lufs_i=-9.2)], {"riddim": {"lufs": -9.2, "drive_db": 12.0, "music_lufs": -15.0, "true_peak_db": -1.2}})
    check("capped by the chain's loss: exit 0, listed as capped", code == 0 and "plafonné" in md)
    code, err, _ = run([measure("riddim", lufs_i=-9.2)], {"riddim": {"lufs": -9.2, "drive_db": 8.0, "music_lufs": -15.0, "true_peak_db": -1.2}})
    check("under the target with room left fails", code == 1 and "Sonie" in err)
    code, _, md = run([measure("house", lufs_i=-7.2)], {"house": {"lufs": -7.2, "drive_db": 3.0, "music_lufs": -9.0, "true_peak_db": -1.2}})
    check("the drive and the loss are in the table", "| +3,0 | 1,2 |" in md)

    # S5: the Drop mode (mix version 2): the Drop's loudness against the target, a 3 LU cap.
    drop = {"lufs": -9.0, "drive_db": 4.0, "music_lufs": -13.0, "true_peak_db": -1.2, "mode": 1, "target_lufs": -8.0, "loss_db": 1.5}
    code, err, md = run([measure("house", lufs_i=-9.0, main_lufs=-8.6, loud_lufs=-8.1)], {"house": drop})
    check("Drop mode: the loudest part at the target passes (the song under it)", code == 0 and "(Drop)" in md)
    code, err, _ = run([measure("house", lufs_i=-9.0, main_lufs=-9.9, loud_lufs=-9.6)], {"house": drop})
    check("Drop mode: the Drop 1.6 LU under with room left fails", code == 1 and "Sonie" in err)
    code, err, md = run([measure("house", lufs_i=-9.0, main_lufs=-9.9, loud_lufs=-9.6)], {"house": {**drop, "loss_db": 2.95}})
    check("Drop mode: capped at 3 LU passes, listed", code == 0 and "plafonné" in md)
    code, err, _ = run([measure("house", lufs_i=-9.0, main_lufs=-8.0)], {"house": {**drop, "loss_db": 3.6}})
    check("Drop mode: a loss over 3 LU fails", code == 1 and "Perte" in err)
    code, err, _ = run([measure("house", m4a_true_peak=-0.7)])
    check("a decoded M4A over -1 dBTP fails", code == 1 and "M4A" in err)

    # S5b: each Drop lands at least 1 LU over the end of its Montée (blocking); the Intro's distance is listed only.
    code, err, md = run([measure("techno", drop_minus_montee_end=[1.6, 1.2], montee_end_lufs=[-10.0, -9.7], drop_start_lufs=[-8.4, -8.5])])
    check("Drops over their Montée's end: exit 0, in the table", code == 0 and "+1,6 / +1,2" in md and "−10,0 / −9,7" in md)
    code, err, md = run([measure("techno", drop_minus_montee_end=[1.6, 0.4])])
    check("a Drop only 0.4 LU over its Montée's end fails", code == 1 and "fin de sa Montée" in err and "techno" in err and "+0,4" in md)
    code, err, md = run([measure("jump_up", drop_minus_montee_end=[-2.5])])
    check("a Montée louder than its Drop fails", code == 1 and "jump_up" in err)
    code, err, md = run([measure("trap", intro_minus_main=-10.2)])
    check("an Intro 10 LU under the Refrain is listed, not blocking", code == 0 and "trap (−10,2)" in md)
    code, err, md = run([measure("pop", intro_minus_main=-5.0)])
    check("an Intro 5 LU under passes", code == 0 and "Intro à moins de 8 LU sous le Drop ou le Refrain (exporté) | 1 sur 1" in md)

    # S5b: a Montée rising less than +2 LU over its last 4 bars is listed, not blocking.
    code, err, md = run([measure("house", montee_rise=[0.8, 2.6])])
    check("a Montée rising only +0.8 LU is listed, not blocking", code == 0 and "house (+0,8 / +2,6)" in md and "au moins +2 LU" in md)

    # G0: the variation between Drops, read from the recordings' META line: listed, not blocking.
    code, err, md = run([measure("jump_up"), measure("house")], cmds={
        "jump_up": "PSCMD 1\nMETA style jump_up\nMETA drop_variation 0.08\nRENDER 1\n", "house": "META drop_variation 0.5\n"})
    check("Drops too alike are listed, not blocking", code == 0 and "jump_up (0,08)" in md and "Variation entre Drops" in md)
    code, err, md = run([measure("house")])
    check("without recordings, no variation line", code == 0 and "Variation entre Drops ≥" not in md)

    print("ALL PASSED" if not failures else f"{failures} FAILURE(S)")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
