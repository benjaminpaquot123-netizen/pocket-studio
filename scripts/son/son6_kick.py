#!/usr/bin/env python3
"""Tour 6, siège son: does each kick hit still lift the exported song on a phone? ps_measure --punch's « rise »
(engine/bench/ps_measure.cpp, « Le kick disparaît »), computed the same way (hits found in the kick rendered alone:
5 ms RMS envelope each millisecond, a rise of 12 dB over the lowest of the 30 ms before, over 5 % of the loudest, 80 ms
apart; power of the mono sum, the 40 ms from the hit against the 100 ms ending 20 ms before, summed over the part's
hits), at full range (to check it against ps_measure) and through the two phone models of son6.py.

  son6_kick.py OUT_DIR out.tsv [--styles a,b]

One row per style and part where the kick plays. Renders the kick alone again (OUT/tools/ps_render), deletes it.
"""
import json
import os
import subprocess
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import son6 as S  # noqa: E402

RATE = S.RATE


def onsets(kick):
    m = kick.mean(axis=1)
    hop, win = RATE // 1000, 5 * RATE // 1000
    c = np.concatenate([[0.0], np.cumsum(m * m)])
    starts = np.arange(0, len(m) - win + 1, hop)
    r = np.sqrt((c[starts + win] - c[starts]) / win)
    top = r.max() if len(r) else 0
    out, last = [], -1000
    for k in range(30, len(r)):
        if r[k] < 0.05 * top or k - last < 80:
            continue
        if r[k] > 4.0 * r[k - 30:k].min():
            out.append(k * hop)
            last = k
    return out


def rise(p, at):
    c = np.concatenate([[0.0], np.cumsum(p)])
    on, off_from, off_to = 40 * RATE // 1000, 120 * RATE // 1000, 20 * RATE // 1000
    h = sum((c[o + on] - c[o]) / on for o in at)
    v = sum((c[o - off_to] - c[o - off_from]) / (off_from - off_to) for o in at)
    return 10 * np.log10(h / v) if at and v > 0 and h > 0 else None


def main(argv):
    out_dir, tsv = argv[0], argv[1]
    styles = sorted(p[:-5] for p in os.listdir(os.path.join(out_dir, "json")) if p.endswith(".json"))
    if "--styles" in argv:
        wanted = argv[argv.index("--styles") + 1].split(",")
        styles = [s for s in styles if s in wanted]
    tmp = os.path.join(out_dir, "son6-tmp")
    os.makedirs(tmp, exist_ok=True)
    rows = []
    for s in styles:
        cmd = os.path.join(out_dir, "cmds", s + ".pscmd")
        kick_id = "16"
        with open(cmd, encoding="utf-8", errors="replace") as f:
            for line in f:
                if line.startswith("META kick "):
                    kick_id = line.split(" ", 2)[2].strip()
                    break
        kp = os.path.join(tmp, s + ".kick-only.wav")
        subprocess.run([os.path.join(out_dir, "tools", "ps_render"), "--no-limiter", "--solo", kick_id, cmd, kp], check=True, capture_output=True)
        kick = S.read_wav(kp)
        os.remove(kp)
        hits = onsets(kick)
        del kick
        x = S.read_wav(os.path.join(out_dir, "wav", s + ".wav"))
        mono = {}
        for name, pw in (("plein", None), ("tel", lambda f: S.phone_power(f)), ("tel400", S.phone400_power)):
            y = S.shape(x, pw) if pw else x
            m = y.mean(axis=1)
            mono[name] = m * m
        del x
        sec = S.sections_of(cmd)
        labels = []
        for i, (start, label) in enumerate(sec):
            if label not in labels:
                labels.append(label)
        for label in labels:
            ranges = [(st, sec[i + 1][0] if i + 1 < len(sec) else len(mono["plein"])) for i, (st, l) in enumerate(sec) if l == label]
            at = [o for o in hits for (a, b) in ranges if o - 120 * RATE // 1000 >= a and o + 40 * RATE // 1000 <= b]
            if not at:
                continue
            r = {"style": s, "partie": label, "coups": len(at)}
            for name, p in mono.items():
                r["coup_" + name] = rise(p, at)
            rows.append(r)
        print(s, len(hits), flush=True)
    with open(tsv, "w", encoding="utf-8") as f:
        f.write("style\tpartie\tcoups\tcoup_plein\tcoup_tel\tcoup_tel400\n")
        for r in rows:
            f.write("\t".join([r["style"], r["partie"], str(r["coups"])] + [S.fmt(r.get(k)) for k in ("coup_plein", "coup_tel", "coup_tel400")]) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
