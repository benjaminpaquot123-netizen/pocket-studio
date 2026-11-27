#!/usr/bin/env python3
"""Third road for the band lengths, by a proven library: Essentia (AGPL-3.0, `pip install essentia`).

Frames -> Windowing (Hann) -> Spectrum -> FrequencyBands (20-200, 200-2000, 2000-20000 Hz), one
energy per band every millisecond, then the same reading as the other roads (energy still to come,
20 dB under the band's whole). It shares no filter and no mask with timbre.py nor with
collection.counter. It is a witness, not the rule's counter-measure: its answer moves with the
frame length where a band only holds the skirt of the sound (docs/sons/fin/TIMBRE-ETAPE01.md).

  tiers_essentia.py selftest
  tiers_essentia.py compare <folder with <cle>.<ext>> <lines.jsonl> ...   (family statistics only)

Run with `python3 -I`. Nothing measured on one bought sound is written anywhere.
"""
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import collection as C  # noqa: E402

SR = C.SR
HOP = SR // 1000
FRAMES = (256, 1024, 4096)
NAMES = ("low", "mid", "high")
EDGES_HZ = [20.0, 200.0, 2000.0, 20000.0]


def band_lengths(y, onset, frame):
    """{band: t20 in ms, or nan when the band holds less than -30 dB of the sound}."""
    import essentia
    essentia.log.infoActive = essentia.log.warningActive = False
    import essentia.standard as es
    window = es.Windowing(type="hann", size=frame, zeroPadding=0, normalized=False)
    spectrum = es.Spectrum(size=frame)
    bands = es.FrequencyBands(frequencyBands=EDGES_HZ, sampleRate=SR)
    y32 = np.ascontiguousarray(y, dtype=np.float32)
    e = np.asarray([bands(spectrum(window(f))) for f in
                    es.FrameGenerator(y32, frameSize=frame, hopSize=HOP, startFromZero=False)],
                   dtype=np.float64)[onset // HOP:]
    whole = float(e.sum())
    out = {}
    for g, name in enumerate(NAMES):
        out[name] = float("nan")
        if whole > 0 and e[:, g].sum() > whole * 10.0 ** (-C.BAND_EMPTY_DB / 10.0):
            # one energy per HOP samples: repeat it so counter_decay_ms reads 1 ms blocks
            out[name] = C.counter_decay_ms(np.repeat(e[:, g], C.DECAY_BLOCK), 0, 20)
    return out


def _one(job):
    cle, path = job
    from ident import M
    sr, x = M.read_audio(path)
    y = C.to_48k(M.mono(x), sr)
    y = y[:C.T.onset_of(y) + int(C.MAX_S * SR)]
    onset = C.counter_onset(y)
    return cle, {frame: band_lengths(y, onset, frame) for frame in FRAMES}


def cmd_compare(root, paths):
    import multiprocessing
    lines = [d for d in C.load_lines(paths)]
    ext = {os.path.splitext(n)[0]: n for n in os.listdir(root)}
    jobs = [(d["cle"], os.path.join(root, ext[d["cle"]])) for d in lines if d["cle"] in ext]
    with multiprocessing.Pool(4) as pool:
        ess = dict(pool.map(_one, jobs, chunksize=8))
    print("famille\tbande\ttrame\tmesure~essentia\tcontre~essentia\tmesure~contre")
    for fam in ("TOUT",) + C.FAMILIES:
        cell = [d for d in lines if d["cle"] in ess and (fam == "TOUT" or d["famille"] == fam)]
        for name in NAMES:
            k = "t20_%s_ms" % name
            for frame in FRAMES:
                e = [ess[d["cle"]][frame][name] for d in cell]
                m, c = [d["m"][k] for d in cell], [d["c"][k] for d in cell]
                cols = ["%d/%d" % C.agreement(a, b, k)[::-1] for a, b in ((m, e), (c, e), (m, c))]
                if cols[0] != "0/0" or cols[2] != "0/0":
                    print("%s\t%s\t%d\t%s" % (fam, name, frame, "\t".join(cols)))
    return 0


def cmd_selftest():
    t = np.arange(int(0.8 * SR)) / SR
    x = np.concatenate([np.zeros(480), np.sin(2 * np.pi * 80 * t) * np.exp(-t / 0.060)
                        + 0.5 * np.sin(2 * np.pi * 6000 * t) * np.exp(-t / 0.015)])
    want = {"low": 0.060 * math.log(10) * 1000.0, "high": 0.015 * math.log(10) * 1000.0}
    bad = 0
    for frame in (256, 1024):
        got = band_lengths(x, C.counter_onset(x), frame)
        for name, w in want.items():
            ok = C.agree("t20_%s_ms" % name, w, got[name])
            bad += not ok
            print("%s  two decaying sines, frame %d, %s: %.1f ms (want %.1f)" % (
                "ok  " if ok else "FAIL", frame, name, got[name], w))
    return 1 if bad else 0


if __name__ == "__main__":
    if len(sys.argv) >= 2 and sys.argv[1] == "selftest":
        sys.exit(cmd_selftest())
    if len(sys.argv) >= 4 and sys.argv[1] == "compare":
        sys.exit(cmd_compare(sys.argv[2], sys.argv[3:]))
    sys.exit(__doc__)
