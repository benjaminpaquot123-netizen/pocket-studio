"""Export the optimised bass sounds to MP3 (192 kb/s, peak -1 dBFS) + manifest.

usage: python -I export.py <out_dir> <mp3_dir> <manifest.json> [assets.json]

assets.json (optional) maps file name -> artifact asset url, filled after upload.
"""
import glob
import json
import os
import subprocess
import sys
import tempfile

import numpy as np
import soundfile as sf

SR = 44100
TOOL = {"808": "numpy sine + pedalboard (Distortion, LowpassFilter, Compressor), pycma",
        "sub": "Surge XT (surgepy), pycma", "basse-synthe": "Surge XT (surgepy), pycma",
        "reese": "Surge XT (surgepy), pycma",
        "basse-acoustique": "VSCO-2-CE Solo Contrabass Pizz + pedalboard chain, pycma"}
SOURCE = {"basse-acoustique": ("VSCO-2-CE (Versilian Studios), Solo Contrabass Pizz", "CC0-1.0")}


def peak_db(path):
    import librosa
    y, _ = librosa.load(path, sr=None, mono=False)
    return 20 * np.log10(np.max(np.abs(y)) + 1e-12)


def encode(y, dst):
    g = 1.0
    for _ in range(4):
        with tempfile.NamedTemporaryFile(suffix=".wav") as t:
            sf.write(t.name, (y * g).astype(np.float32), SR, subtype="FLOAT")
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", t.name, "-codec:a", "libmp3lame", "-b:a", "192k",
                            "-ar", str(SR), dst], check=True)
        pk = peak_db(dst)
        if -1.3 <= pk <= -1.0:
            return pk
        g *= 10 ** ((-1.1 - pk) / 20)
    return pk


def main():
    out, mp3, man = sys.argv[1:4]
    assets = json.load(open(sys.argv[4])) if len(sys.argv) > 4 else {}
    os.makedirs(mp3, exist_ok=True)
    rows = []
    for js in sorted(glob.glob(os.path.join(out, "*.json"))):
        for it in json.load(open(js)):
            fn = it["name"] + ".mp3"
            dst = os.path.join(mp3, fn)
            if not os.path.exists(dst):
                encode(np.load(os.path.join(out, it["name"] + ".npy")), dst)
            fam = it["family"]
            voice = it.get("voice", fam)
            src, lic = SOURCE.get(voice, ("synthesised (no sample)", "n/a"))
            rows.append({"file": fn, "style": it["style"], "family": fam, "tool": TOOL[voice],
                         "source": src, "licence": lic, "essentia_sd": it["essentia_sd"],
                         "librosa_agree": it["librosa_agree"], "dna_family": {
                             "808": "drum_oneshot/kick_808", "phrase": "melodic_loop/bass"}.get(fam, "oneshot_melodic/bass"),
                         "asset_url": assets.get(fn, "audio/" + fn)})
    json.dump({"zone": "basses", "artifact": "https://claude.ai/artifact/VrKirWrSn6i1YRCdJKVkio",
               "asset_url_note": "path of the MP3 published next to the artifact page", "score": "Essentia distance to family DNA, RMS of robust z (0 = family median)",
               "sounds": rows}, open(man, "w"), indent=1, ensure_ascii=False)
    print(len(rows), "sounds")


if __name__ == "__main__":
    main()
