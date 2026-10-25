"""Audit 35 listening extracts (the « avant » of the sound chantiers), after abtools/abfinal.py:
30 s from 4 bars before the first Drop (trap: Refrain), read from the song's SECTION lines; 10 ms fade-in,
1 s fade-out; AAC 192k (ffmpeg, -aac_coder fast); decoded true peak <= -1 dBTP (lower by overshoot + 0.3 dB
and re-encode). Plus a phone-speaker version: the same segment through a 4th-order Butterworth high-pass at
300 Hz (docs/hits « basses qui tapent » model), at the SAME gain (what is lost is heard as lost), same TP check.

usage: extracts.py <dest> <cmds dir with .pscmd> <wav dir> style...
"""
import json, os, re, subprocess, sys
import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 48000
DEST, CMDS, WAVS, STYLES = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]


def sections(pscmd):
    bpm, secs = None, []
    for line in open(pscmd, encoding="utf-8"):
        if line.startswith("SONG "):
            for kv in line.split()[1:]:
                if kv.startswith("bpm="): bpm = float(kv[4:])
        elif line.startswith("SECTION "):
            p = line.rstrip("\n").split(" ", 3)
            secs.append((int(p[2]), p[3]))
    return bpm, secs


def ebur(path):
    out = subprocess.run(["ffmpeg", "-nostats", "-hide_banner", "-i", path, "-af", "ebur128=peak=true", "-f", "null", "-"],
                         capture_output=True, text=True).stderr
    summ = out[out.rfind("Summary:"):]
    i = float(re.search(r"I:\s+(-?[\d.]+|-inf) LUFS", summ).group(1))
    tp = float(re.search(r"True peak:\s+Peak:\s+(-?[\d.]+|-inf) dBFS", summ).group(1))
    return i, tp


def encode(seg, name, gain):
    src = f"{DEST}/.{name}.wav"
    for _ in range(8):
        wavfile.write(src, SR, (seg * 10 ** (gain / 20)).astype(np.float32))
        m4a = f"{DEST}/{name}.m4a"
        subprocess.run(["ffmpeg", "-nostats", "-hide_banner", "-loglevel", "error", "-y", "-i", src, "-c:a", "aac",
                        "-aac_coder", "fast", "-b:a", "192k", m4a], check=True)
        dec = f"{DEST}/.{name}.dec.wav"
        subprocess.run(["ffmpeg", "-nostats", "-hide_banner", "-loglevel", "error", "-y", "-i", m4a, "-c:a", "pcm_f32le", dec], check=True)
        i, tp = ebur(dec)
        os.remove(dec)
        if tp <= -1.0:
            os.remove(src)
            return dict(file=m4a, gain_db=round(gain, 2), lufs_i=i, true_peak=tp)
        gain -= (tp + 1.0) + 0.3
    raise SystemExit(f"{name}: still {tp} dBTP")


res = {}
sos = signal.butter(4, 300, "highpass", fs=SR, output="sos")
for st in STYLES:
    bpm, secs = sections(f"{CMDS}/{st}.pscmd")
    want = "Refrain" if not any(n.startswith("Drop") for _, n in secs) else "Drop"
    t0 = next(f for f, n in secs if n.startswith(want))
    bar = int(round(4 * 60 / bpm * SR))
    start = max(0, t0 - 4 * bar)
    rate, wav = wavfile.read(f"{WAVS}/{st}.wav")
    assert rate == SR
    seg = wav[start:start + 30 * SR].astype(np.float64)
    env = np.ones(len(seg)); fi, fo = int(0.01 * SR), SR
    env[:fi] = np.arange(fi) / fi
    env[-fo:] = np.minimum(env[-fo:], (fo - 1 - np.arange(fo)) / (fo - 1))
    seg = seg * env[:, None]
    full = encode(seg, f"{st}-avant", 0.0)
    phone = encode(signal.sosfilt(sos, seg, axis=0), f"{st}-avant-telephone", full["gain_db"])
    res[st] = dict(bpm=bpm, target=want, start_s=round(start / SR, 2), target_in_extract_s=round((t0 - start) / SR, 2),
                   full=full, phone=phone, phone_minus_full_lu=round(phone["lufs_i"] - full["lufs_i"], 1))
    print(st, json.dumps(res[st]))
json.dump(res, open(f"{DEST}/extraits.json", "w"), ensure_ascii=False, indent=1)
