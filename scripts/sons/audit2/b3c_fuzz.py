"""Batch 3c: fuzz of end_cut on noisy sounds (hats, snares, claps) with an anti-click fade, direct call of tout.end_cut."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *

def q24(x): return np.round(x * 8388607) / 8388608.0
def mk(kind, sr, seed, cutpos):
    rng = np.random.default_rng(seed)
    n = int(1.0 * sr); t = np.arange(n) / sr
    w = rng.standard_normal(n)
    if kind == "hat":      # bright noise, slow decay (open hat)
        x = np.diff(np.diff(w, prepend=0), prepend=0); x = x / np.max(np.abs(x)) * np.exp(-t / 0.3) * 0.7
    elif kind == "snare":  # noise + 190 Hz body
        x = (0.6 * w / 4 + 0.5 * np.sin(2 * np.pi * 190 * t)) * np.exp(-t / 0.15); x = x / np.max(np.abs(x)) * 0.8
    elif kind == "white":
        x = w / 4 * np.exp(-t / 0.3) * 0.7
    elif kind == "kick":
        ph = 2 * np.pi * (50 * t + (160 - 50) * 0.03 * (1 - np.exp(-t / 0.03)))
        x = np.sin(ph) * np.exp(-t / 0.2) * 0.9
    return x[:int(cutpos * sr)]

print("share of files read as CUT (20 seeds each). Linear fade applied at the cut point.")
print("%-6s %-6s " % ("sound", "sr") + " ".join("%6s" % ("%gms" % m) for m in (0, 2, 4, 5, 6, 8, 10, 12, 15, 20, 30, 50)))
for kind in ("hat", "snare", "white", "kick"):
    for sr in (44100, 48000, 96000):
        line = []
        for ms in (0, 2, 4, 5, 6, 8, 10, 12, 15, 20, 30, 50):
            cuts = 0
            for seed in range(20):
                x = mk(kind, sr, seed, 0.15 + 0.01 * (seed % 7))
                if ms:
                    x = fade(x, ms, sr)
                cuts += tout.end_cut(sr, q24(x))["end_cut"]
            line.append("%5d%%" % (cuts * 5))
        print("%-6s %-6d " % (kind, sr) + " ".join(line))
