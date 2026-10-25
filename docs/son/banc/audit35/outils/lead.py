"""Audit 35: the lead track alone (ps_render --solo <META lead>), over the first Drop's 8 bars (or Refrain):
centroid, phone presence (1-4 kHz share after the HP300 model), brightness (4-10k vs 1-4k), mud (150-500 vs 1-4k), side/mid.
usage: lead.py <style.pscmd> <solo.wav> -> JSON"""
import json, sys
import numpy as np, soundfile as sf
from scipy import signal
SR = 48000
pscmd, wav = sys.argv[1], sys.argv[2]
bpm, secs = None, []
for line in open(pscmd, encoding="utf-8"):
    if line.startswith("SONG "):
        for kv in line.split()[1:]:
            if kv.startswith("bpm="): bpm = float(kv[4:])
    elif line.startswith("SECTION "):
        p = line.rstrip("\n").split(" ", 3); secs.append((int(p[2]), p[3]))
main = "Drop" if any(n.startswith("Drop") for _, n in secs) else "Refrain"
s0 = next(f for f, n in secs if n.startswith(main))
bar = int(round(4 * 60 / bpm * SR))
x, _ = sf.read(wav, dtype="float32", always_2d=True)
# The whole first Drop (its consecutive Drop scenes), only the bars where the lead plays (> -50 dBFS RMS).
end = next((f for f, n in secs if f > s0 and not n.startswith(main)), len(x))
bars = [x[a:a + bar] for a in range(s0, end - bar + 1, bar)]
playing = [b_ for b_ in bars if 10 * np.log10(np.mean(b_.astype(np.float64) ** 2) + 1e-20) > -50]
if not playing:
    print(json.dumps(dict(bars=0, of=len(bars)))); sys.exit()
seg = np.concatenate(playing).astype(np.float64)
m = seg.mean(axis=1)
f, p = signal.welch(m, SR, nperseg=8192)
def b(lo, hi): return p[(f >= lo) & (f < hi)].sum() + 1e-20
cent = float((f * p).sum() / p.sum())
hp = signal.sosfilt(signal.butter(4, 300, "highpass", fs=SR, output="sos"), m)
fh, ph = signal.welch(hp, SR, nperseg=8192)
pres = 10 * np.log10(ph[(fh >= 1000) & (fh < 4000)].sum() / ph.sum())
mid, side = (seg[:, 0] + seg[:, 1]) / 2, (seg[:, 0] - seg[:, 1]) / 2
print(json.dumps(dict(bars=len(playing), of=len(bars), centroid_hz=round(cent), pres=round(float(pres), 1), bright=round(float(10 * np.log10(b(4000, 10000) / b(1000, 4000))), 1),
                      mud=round(float(10 * np.log10(b(150, 500) / b(1000, 4000))), 1),
                      sm=round(float(10 * np.log10(np.sum(side ** 2) / (np.sum(mid ** 2) + 1e-20) + 1e-20)), 1),
                      level_db=round(float(10 * np.log10(np.mean(seg ** 2) + 1e-20)), 1))))
