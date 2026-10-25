"""Audit 35: per-style extra measurements on one rendered song (WAV 48 kHz float) and its .pscmd.

For the first Drop (or Refrain), its first 8 bars (or the whole section if shorter):
  - lufs: K-weighted gated loudness (BS.1770) of the section;
  - tel300: loudness loss through the phone-speaker model (4th-order Butterworth high-pass at 300 Hz,
    as in the « basses qui tapent » bench): LUFS(full) - LUFS(HP300) -> the closer to 0, the more is heard;
  - pres: share of 1-4 kHz in what is left after the HP300 (dB);
  - lowphone: share of 300-800 Hz after HP300 (dB): what « taps » of the kick and bass on a phone;
  - sm: side/mid energy ratio over the section (dB);
  - corr_low (S7): L/R correlation of the section under 120 Hz (1.0 = the low end in mono);
  - corr_d12: waveform correlation (mid signal) between the first 8 bars of Drop 1 and of Drop 2 (1.0 = identical);
  - band_d12: largest band-energy difference (dB, 8 bands) between those two windows.
usage: phone.py <style.pscmd> <style.wav>  -> one JSON line on stdout
"""
import json, sys
import numpy as np
import soundfile as sf
from scipy import signal

SR = 48000


def sections(pscmd):
    bpm, secs, beats = None, [], 4
    for line in open(pscmd, encoding="utf-8"):
        if line.startswith("SONG "):
            for kv in line.split()[1:]:
                if "=" in kv:
                    k, v = kv.split("=", 1)
                    if k == "bpm": bpm = float(v)
                    if k in ("beats", "bpb", "beatsPerBar"): beats = int(float(v))
        elif line.startswith("SECTION "):
            p = line.rstrip("\n").split(" ", 3)
            secs.append((int(p[2]), p[3]))
    return bpm, beats, secs


def kweight(x):
    b1, a1 = [1.53512485958697, -2.69169618940638, 1.19839281085285], [1, -1.69065929318241, 0.73248077421585]
    b2, a2 = [1.0, -2.0, 1.0], [1, -1.99004745483398, 0.99007225036621]
    return signal.lfilter(b2, a2, signal.lfilter(b1, a1, x, axis=0), axis=0)


def lufs(x):
    y = kweight(x)
    blk, hop = int(0.4 * SR), int(0.1 * SR)
    ms = []
    for s in range(0, max(1, len(y) - blk + 1), hop):
        seg = y[s:s + blk]
        ms.append(np.sum(np.mean(seg ** 2, axis=0)))
    ms = np.array(ms)
    l = -0.691 + 10 * np.log10(ms + 1e-20)
    ms = ms[l > -70]
    if not len(ms): return -100.0
    rel = -0.691 + 10 * np.log10(np.mean(ms)) - 10
    ms2 = ms[(-0.691 + 10 * np.log10(ms)) > rel]
    return float(-0.691 + 10 * np.log10(np.mean(ms2)))


def band_db(x, lo, hi):
    m = x.mean(axis=1)
    f, p = signal.welch(m, SR, nperseg=8192)
    tot = p.sum()
    sel = p[(f >= lo) & (f < hi)].sum()
    return float(10 * np.log10(sel / tot + 1e-20))


def bands8(x):
    m = x.mean(axis=1)
    f, p = signal.welch(m, SR, nperseg=8192)
    edges = [20, 60, 120, 250, 500, 1000, 2000, 4000, 8000, 16000]
    return np.array([10 * np.log10(p[(f >= edges[i]) & (f < edges[i + 1])].sum() + 1e-20) for i in range(len(edges) - 1)])


def envelope(x, step):
    """Log energy per 16th step in 4 bands (<150, 150-1k, 1-5k, >5k): the arrangement's fingerprint."""
    m = x.mean(axis=1)
    out = []
    for lo, hi in ((20, 150), (150, 1000), (1000, 5000), (5000, 20000)):
        sos = signal.butter(4, [lo, hi], "bandpass", fs=SR, output="sos")
        y = signal.sosfilt(sos, m) ** 2
        n = len(y) // step
        out.append(10 * np.log10(y[:n * step].reshape(n, step).mean(axis=1) + 1e-12))
    return np.concatenate(out)


def main():
    pscmd, wav = sys.argv[1], sys.argv[2]
    bpm, beats, secs = sections(pscmd)
    x, sr = sf.read(wav, dtype="float32", always_2d=True)
    assert sr == SR, sr
    bar = int(round(beats * 60 / bpm * SR))
    names = [n for _, n in secs]
    main_name = "Drop" if any(n.startswith("Drop") for n in names) else ("Refrain" if any(n.startswith("Refrain") for n in names) else None)
    out = {"bpm": bpm}
    if main_name is None:
        print(json.dumps(out)); return
    starts = []
    for i, (f, n) in enumerate(secs):
        if n.startswith(main_name) and (i == 0 or not secs[i - 1][1].startswith(main_name)):
            starts.append(f)
    s0 = starts[0]
    nxt = [f for f, n in secs if f > s0 and not n.startswith(main_name)]
    end = min(nxt[0] if nxt else len(x), s0 + 8 * bar, len(x))
    seg = x[s0:end].astype(np.float64)
    sos = signal.butter(4, 300, "highpass", fs=SR, output="sos")
    hp = signal.sosfilt(sos, seg, axis=0)
    full, tel = lufs(seg), lufs(hp)
    mid, side = (seg[:, 0] + seg[:, 1]) / 2, (seg[:, 0] - seg[:, 1]) / 2
    out.update(main=main_name, drops=len(starts), drop1_s=round(s0 / SR, 1), lufs=round(full, 2), tel300=round(full - tel, 1),
               pres=round(band_db(hp, 1000, 4000), 1), lowphone=round(band_db(hp, 300, 800), 1),
               sm=round(float(10 * np.log10(np.sum(side ** 2) / (np.sum(mid ** 2) + 1e-20) + 1e-20)), 1))
    lp = signal.sosfilt(signal.butter(4, 120, "lowpass", fs=SR, output="sos"), seg, axis=0)
    out["corr_low"] = round(float(np.dot(lp[:, 0], lp[:, 1]) / (np.linalg.norm(lp[:, 0]) * np.linalg.norm(lp[:, 1]) + 1e-20)), 3)
    if len(starts) >= 2:
        n = min(end - s0, len(x) - starts[1])
        a = x[s0:s0 + n].mean(axis=1).astype(np.float64)
        b = x[starts[1]:starts[1] + n].mean(axis=1).astype(np.float64)
        out["corr_d12"] = round(float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b) + 1e-20)), 3)
        out["band_d12"] = round(float(np.max(np.abs(bands8(x[s0:s0 + n]) - bands8(x[starts[1]:starts[1] + n])))), 1)
        step = bar // 16
        ea, eb = envelope(x[s0:s0 + n], step), envelope(x[starts[1]:starts[1] + n], step)
        out["env_d12"] = round(float(np.corrcoef(ea, eb)[0, 1]), 3)
        out["lufs_d2"] = round(lufs(x[starts[1]:starts[1] + n].astype(np.float64)), 2)
    print(json.dumps(out))


main()
