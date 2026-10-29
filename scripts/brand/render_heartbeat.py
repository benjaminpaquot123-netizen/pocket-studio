#!/usr/bin/env python3
"""BenStudio's opening sound: a double heartbeat (« boum-boum »), produced like a studio sound.

Synthesised from scratch (nothing sampled), so it is reproducible bit for bit:
  - a warm sub (a sine falling from about 105 Hz to 52 Hz, lightly saturated so that its upper
    harmonics carry it on a phone's small speaker);
  - a felt body (a low thump: band-passed noise and a short falling sine around 150 Hz);
  - a soft attack click (a few ms of 2-6 kHz) so that the beat is heard on a phone;
  - the second beat softer and shorter, as a real heart's « dub »;
  - a short dark room (a small algorithmic reverb, on everything but the sub);
  - a very quiet A add9 chord opening under the name, and a faint glint when the name shines.
Mastered to about -14 LUFS (ITU-R BS.1770-4), true peak at most -1 dBTP (4x oversampled), no clip.

Timing: sample 0 of the file plays at SOUND_START ms of the opening's timeline (BrandIntro.kt's
IntroTimeline), so each beat starts exactly at its frame: (BEAT_START[k] - SOUND_START) * 48.
The beats' first samples are written as WAV cue points (« lub », « dub »), which the app's tests
read back. Before the first beat the file is digital silence.

Writes app/src/main/assets/intro/heartbeat.wav (48 kHz, mono, 16 bit).
Usage: python3 scripts/brand/render_heartbeat.py [--out FILE] [--report]
Needs numpy and scipy.
"""
import os
import struct
import sys

import numpy as np
from scipy import signal

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "intro", "heartbeat.wav")

SR = 48000
# --- Keep in step with IntroTimeline (BrandIntro.kt) and render_brand.py ---------------------------
SOUND_START = 1000          # ms on the timeline where the file's sample 0 plays
BEAT_START = (1100, 1340)   # ms: « lub », « dub »
NAME_START = 1750           # ms: the name comes in, the chord opens
NAME_SHINE = 2350           # ms: the light runs over the name, the glint
LENGTH_MS = 3200            # the file's length (it ends, faded, well before the opening's fade)
TARGET_LUFS = -14.0
CEILING_DBTP = -1.0

RNG = np.random.default_rng(20261029)
SUB, MALLET = 0.55, 2.5  # the sub's level, and the mids' (what a phone's speaker plays)


def frames(ms_on_timeline):
    """The file's frame for a time on the opening's timeline (exact: 48 frames per ms)."""
    return (ms_on_timeline - SOUND_START) * SR // 1000


def env_ad(n, attack, decay):
    """Raised-cosine attack then exponential decay, n samples from the onset (starts at 0)."""
    t = np.arange(n) / SR
    a = np.where(t < attack, 0.5 - 0.5 * np.cos(np.pi * np.clip(t / attack, 0, 1)), 1.0)
    return a * np.exp(-np.maximum(t - attack, 0) / decay)


def bandpass(x, lo, hi, order=2):
    sos = signal.butter(order, [lo, hi], btype="bandpass", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def lowpass(x, f, order=2):
    return signal.sosfilt(signal.butter(order, f, btype="lowpass", fs=SR, output="sos"), x)


def highpass(x, f, order=2):
    return signal.sosfilt(signal.butter(order, f, btype="highpass", fs=SR, output="sos"), x)


def beat(n_total, onset, gain, sub_from, sub_to, sub_decay, body_decay, click):
    """One heartbeat starting exactly at sample [onset]: (dry sub, everything else)."""
    n = n_total - onset
    t = np.arange(n) / SR
    # Sub: a sine falling fast to its note, phase 0 at the onset (no step).
    f = sub_to + (sub_from - sub_to) * np.exp(-t / 0.028)
    ph = 2 * np.pi * np.cumsum(f) / SR - 2 * np.pi * f[0] / SR
    e = env_ad(n, 0.007, sub_decay)
    sub = np.tanh(2.4 * np.sin(ph)) / np.tanh(2.4) * e
    harm = 0.30 * np.sin(2 * ph) * env_ad(n, 0.002, sub_decay * 0.55)
    # Body: a felt knock (falling sine near 150 Hz) and a puff of low noise.
    fk = 120 + 55 * np.exp(-t / 0.02)
    knock = np.sin(2 * np.pi * np.cumsum(fk) / SR - 2 * np.pi * fk[0] / SR) * env_ad(n, 0.0015, body_decay)
    puff = bandpass(RNG.standard_normal(n), 90, 380) * env_ad(n, 0.001, body_decay * 0.8)
    puff /= max(1e-9, np.max(np.abs(puff)))
    # Felt mallet: the part a phone's small speaker plays (a muffled tom near 330 Hz, a puff of mids).
    fm = 300 + 90 * np.exp(-t / 0.012)
    mallet = np.sin(2 * np.pi * np.cumsum(fm) / SR - 2 * np.pi * fm[0] / SR) * env_ad(n, 0.001, body_decay * 0.55)
    felt = bandpass(RNG.standard_normal(n), 320, 1600) * env_ad(n, 0.0008, body_decay * 0.4)
    felt /= max(1e-9, np.max(np.abs(felt)))
    # Click: a few ms of soft high noise and a 2.6 kHz blip, so a phone's speaker says « tok ».
    hiss = bandpass(RNG.standard_normal(n), 1800, 6000) * env_ad(n, 0.0004, 0.004)
    hiss /= max(1e-9, np.max(np.abs(hiss)))
    blip = np.sin(2 * np.pi * 2600 * t) * env_ad(n, 0.0005, 0.006)
    out_sub = np.zeros(n_total)
    out_rest = np.zeros(n_total)
    out_sub[onset:] = gain * SUB * sub
    out_rest[onset:] = gain * (harm + 0.62 * knock + 0.36 * puff + MALLET * (0.5 * mallet + 0.3 * felt) + click * (0.36 * hiss + 0.2 * blip))
    return out_sub, out_rest


def chord(n_total, onset):
    """A add9, open and very quiet (A2 E3 C#4 B4, a breath of E5), slow attack, long decay."""
    n = n_total - onset
    t = np.arange(n) / SR
    out = np.zeros(n)
    for hz, amp in ((110.0, 0.9), (164.81, 0.7), (277.18, 0.5), (493.88, 0.32), (659.26, 0.12)):
        for cents in (-4, 3):
            f = hz * 2 ** (cents / 1200)
            out += amp * (np.sin(2 * np.pi * f * t + RNG.uniform(0, 2 * np.pi)) + 0.12 * np.sin(4 * np.pi * f * t))
    out *= env_ad(n, 0.45, 1.1)
    out = lowpass(out, 2200)
    res = np.zeros(n_total)
    res[onset:] = out / np.max(np.abs(out))
    return res


def glint(n_total, onset):
    """A faint bell (E6 with an inharmonic partial) when the light runs over the name."""
    n = n_total - onset
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * 1318.5 * t) + 0.35 * np.sin(2 * np.pi * 1318.5 * 2.76 * t) * np.exp(-t / 0.08)
    res = np.zeros(n_total)
    res[onset:] = tone * env_ad(n, 0.006, 0.42)
    return res


def room(x):
    """A short, dark room: early reflections then a decaying noise tail (RT60 about 0.7 s)."""
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    ir = RNG.standard_normal(n) * np.exp(-t / 0.1)
    ir = highpass(lowpass(ir, 4500), 220)
    ir[: int(0.014 * SR)] = 0  # pre-delay
    for ms, g in ((17, 0.5), (23, -0.35), (31, 0.28), (43, -0.2)):
        ir[int(ms * SR / 1000)] += g * np.max(np.abs(ir))
    ir /= np.sqrt(np.sum(ir ** 2))
    return signal.fftconvolve(x, ir)[: len(x)]


# --- Loudness (ITU-R BS.1770-4) and true peak -------------------------------------------------------
def lufs(x):
    b1, a1 = [1.53512485958697, -2.69169618940638, 1.19839281085285], [1.0, -1.69065929318241, 0.73248077421585]
    b2, a2 = [1.0, -2.0, 1.0], [1.0, -1.99004745483398, 0.99007225036621]
    y = signal.lfilter(b2, a2, signal.lfilter(b1, a1, x))
    block, hop = int(0.4 * SR), int(0.1 * SR)
    ms = np.array([np.mean(y[i:i + block] ** 2) for i in range(0, len(y) - block + 1, hop)])
    ld = -0.691 + 10 * np.log10(np.maximum(ms, 1e-20))
    gated = ms[ld > -70]
    rel = -0.691 + 10 * np.log10(np.mean(gated)) - 10
    gated = ms[(ld > -70) & (ld > rel)]
    return -0.691 + 10 * np.log10(np.mean(gated))


def true_peak_db(x):
    return 20 * np.log10(np.max(np.abs(signal.resample_poly(x, 4, 1))) + 1e-20)


def limit(x, ceiling_db):
    """A transparent look-ahead limiter (gain only, 1.5 ms look-ahead, 25 ms release) on the
    4x oversampled peak: nothing is clipped. Before the first beat the file is silent, so the
    look-ahead never moves an onset."""
    ceiling = 10 ** (ceiling_db / 20)
    over = np.abs(signal.resample_poly(x, 4, 1)).reshape(-1, 4).max(axis=1)[: len(x)]
    need = np.minimum(1.0, ceiling / np.maximum(over, 1e-12))
    look = int(0.0015 * SR)
    need = np.array([need[i:i + look + 1].min() for i in range(len(need))])
    g = np.empty_like(need)
    rel = np.exp(-1 / (0.025 * SR))
    cur = 1.0
    for i, v in enumerate(need):
        cur = v if v < cur else v + (cur - v) * rel
        g[i] = cur
    return x * g


def render():
    n = LENGTH_MS * SR // 1000
    lub, dub = (frames(b) for b in BEAT_START)
    s1, r1 = beat(n, lub, 1.0, 105, 52, 0.32, 0.06, 1.0)
    s2, r2 = beat(n, dub, 0.7, 98, 56, 0.22, 0.04, 0.6)
    pad = 0.085 * chord(n, frames(NAME_START))
    bell = 0.022 * glint(n, frames(NAME_SHINE))
    rest = r1 + r2
    wet = room(0.9 * rest + 1.4 * pad + 2.0 * bell)
    mix = s1 + s2 + rest + pad + bell + 0.16 * wet
    mix = highpass(mix, 22)  # no DC, nothing under the sub
    # The end: a cosine fade to digital silence over the last 0.6 s.
    tail = int(0.6 * SR)
    mix[-tail:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, tail))
    mix[:lub] = 0.0
    for _ in range(8):  # converges on the target: about 3 dB of limiting on the beats
        mix *= 10 ** ((TARGET_LUFS - lufs(mix)) / 20)
        mix = limit(mix, CEILING_DBTP - 0.15)
    mix[:lub] = 0.0
    return mix, (lub, dub)


def write_wav(path, x, cues):
    pcm = np.round(np.clip(x, -1, 1) * 32767).astype("<i2").tobytes()
    fmt = struct.pack("<HHIIHH", 1, 1, SR, SR * 2, 2, 16)
    cue = struct.pack("<I", len(cues)) + b"".join(
        struct.pack("<II4sIII", i + 1, pos, b"data", 0, 0, pos) for i, pos in enumerate(cues))
    labels = b""
    for i, name in enumerate((b"lub", b"dub")[: len(cues)]):
        body = struct.pack("<I", i + 1) + name + b"\0"
        body += b"\0" * (len(body) % 2)
        labels += b"labl" + struct.pack("<I", len(body)) + body
    adtl = b"adtl" + labels
    chunks = (b"fmt " + struct.pack("<I", len(fmt)) + fmt
              + b"cue " + struct.pack("<I", len(cue)) + cue
              + b"LIST" + struct.pack("<I", len(adtl)) + adtl
              + b"data" + struct.pack("<I", len(pcm)) + pcm)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"RIFF" + struct.pack("<I", 4 + len(chunks)) + b"WAVE" + chunks)


def main():
    out = sys.argv[sys.argv.index("--out") + 1] if "--out" in sys.argv else OUT
    mix, cues = render()
    q = np.round(np.clip(mix, -1, 1) * 32767) / 32767
    write_wav(out, mix, cues)
    print(f"wrote {out}: {len(mix) / SR:.2f} s, {lufs(q):.2f} LUFS, true peak {true_peak_db(q):.2f} dBTP, "
          f"sample peak {20 * np.log10(np.max(np.abs(q))):.2f} dBFS, beats at frames {cues}")
    if "--report" in sys.argv:
        for name, a in (("lub", cues[0]), ("dub", cues[1])):
            w = q[a:a + SR // 50]
            print(f"  {name}: peak {20 * np.log10(np.max(np.abs(w))):.1f} dBFS in its first 20 ms")
        spec = np.abs(np.fft.rfft(q)) ** 2
        fr = np.fft.rfftfreq(len(q), 1 / SR)
        tot = spec.sum()
        for lo, hi in ((20, 80), (80, 300), (300, 1500), (1500, 8000), (8000, 24000)):
            print(f"  {lo}-{hi} Hz: {100 * spec[(fr >= lo) & (fr < hi)].sum() / tot:.1f} % of the energy")


if __name__ == "__main__":
    main()
