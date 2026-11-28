"""Encode cible.py outputs to MP3 (192 kb/s, peak -1 dBFS) and write index.json.

usage: python -I export.py <out_dir> <dest_dir>
"""
import glob
import json
import os
import subprocess
import sys

import numpy as np

SR = 44100


def main():
    src, dest = sys.argv[1:3]
    os.makedirs(dest, exist_ok=True)
    index = []
    for jf in sorted(glob.glob(os.path.join(src, "*.json"))):
        for it in json.load(open(jf)):
            y = np.load(os.path.join(src, it["name"] + ".npy")).astype(np.float32)
            if y.ndim == 1:
                y = np.stack([y, y], 1)
            y *= 10 ** (-1 / 20) / max(float(np.abs(y).max()), 1e-9)
            mp3 = it["name"] + ".mp3"
            subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "2",
                            "-i", "-", "-b:a", "192k", os.path.join(dest, mp3)],
                           input=y.tobytes(), check=True)
            it = {k: v for k, v in it.items() if k != "x"} | {"file": mp3, "params": it["x"]}
            index.append(it)
    index.sort(key=lambda i: (i["style"], i["family"], i["distance"]))
    with open(os.path.join(dest, "index.json"), "w") as f:
        json.dump({"zone": "B basses", "targets": "docs/sons/fin/cibles/zone-b-basses.tsv",
                   "distance": "RMS of robust z (p25-p50-p75) over zone B descriptors, lower = closer",
                   "sounds": index}, f, indent=1)
    print(len(index), "sounds")


if __name__ == "__main__":
    main()
