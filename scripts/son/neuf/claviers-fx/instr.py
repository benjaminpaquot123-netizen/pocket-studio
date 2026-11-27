"""Sound sources for the keys, effects and voice zone (48 kHz, stereo float32).

- Sampler: plays recorded notes (Salamander piano, CC BY 3.0; VCSL TX81Z, CC0),
  pitch-shifted from the nearest recorded note.
- fm_ep: home-made two-operator FM electric piano (tine + bark).
- organ: additive drawbar organ with key click and percussion.
- fx_*: riser, downlifter, impact, sweep from noise and oscillators.
- chops: vowel slices of VocalSet (CC BY 4.0) re-pitched into a pattern.

A phrase is a list of notes (start_beat, len_beats, midi, velocity 1..127).
"""
import functools
import glob
import os
import re

import numpy as np
import soundfile as sf

SR = 48000
NOTE = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7,
        "G#": 8, "A": 9, "A#": 10, "B": 11}


def midi_of(name):
    m = re.match(r"^([A-G]#?)(-?\d)$", name)
    return 12 * (int(m.group(2)) + 1) + NOTE[m.group(1)]


def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def resample_ratio(y, ratio):
    """Play y 'ratio' times faster (pitch up by ratio). y: (n, ch)."""
    if abs(ratio - 1) < 1e-4:
        return y
    import librosa
    tgt = int(round(SR / ratio))
    return librosa.resample(y.T, orig_sr=SR, target_sr=tgt, res_type="soxr_hq").T


@functools.lru_cache(maxsize=4096)
def _load(path, max_s):
    y, sr = sf.read(path, dtype="float32", always_2d=True, frames=int(max_s * 48000 * 1.1))
    if y.shape[1] == 1:
        y = np.repeat(y, 2, 1)
    if sr != SR:
        import librosa
        y = librosa.resample(y.T, orig_sr=sr, target_sr=SR, res_type="soxr_hq").T
    return np.ascontiguousarray(y[:, :2])


class Sampler:
    """Map midi -> sorted velocity layers of files."""

    def __init__(self, files, parse, max_s=8.0, release_s=0.25):
        self.map = {}
        for f in files:
            p = parse(os.path.basename(f))
            if p:
                m, layer = p
                self.map.setdefault(m, {})[layer] = f
        self.keys = np.array(sorted(self.map))
        self.max_s = max_s
        self.release_s = release_s

    @functools.lru_cache(maxsize=8192)
    def note(self, m, vel, dur_s):
        k = int(self.keys[np.argmin(np.abs(self.keys - m))])
        layers = sorted(self.map[k])
        li = layers[min(len(layers) - 1, int((vel / 128.0) * len(layers)))]
        y = _load(self.map[k][li], self.max_s)
        y = resample_ratio(y, 2 ** ((m - k) / 12))
        n = min(len(y), int((dur_s + self.release_s * 4) * SR))
        y = y[:n].copy()
        off = int(dur_s * SR)
        if off < n:
            t = np.arange(n - off) / SR
            y[off:] *= np.exp(-t / self.release_s)[:, None]
        return y


def salamander(root):
    files = glob.glob(os.path.join(root, "samples", "*v*.flac"))

    def parse(b):
        r = re.match(r"^([A-G]#?)(\d)v(\d+)\.flac$", b)
        return (midi_of(r.group(1) + r.group(2)), int(r.group(3))) if r else None
    return Sampler(files, parse, max_s=10.0, release_s=0.22)


def tx81z(root, patch="FM Piano"):
    files = glob.glob(os.path.join(root, "Electrophones", "TX81Z", patch, "*.wav"))

    def parse(b):
        r = re.match(r"^[A-Za-z0-9 ]+_([A-G]#?)(-?\d)_vl(\d)\.wav$", b)
        return (midi_of(r.group(1) + r.group(2)), int(r.group(3))) if r else None
    return Sampler(files, parse, max_s=6.0, release_s=0.18)


def render(phrase, bpm, voice, tail_s=2.0, humanize=0.0, seed=0, swing=0.0):
    """voice(m, vel, dur_s) -> (n, 2). Returns the mixed phrase (n, 2)."""
    rng = np.random.default_rng(seed)
    spb = 60.0 / bpm
    end = max(s + l for s, l, _, _ in phrase)
    out = np.zeros((int((end * spb + tail_s) * SR) + SR, 2), np.float32)
    for s, l, m, v in phrase:
        if swing and abs((s * 2) % 2 - 1) < 1e-6:  # off-beat eighth
            s = s + swing * 0.5
        t0 = s * spb + (rng.normal(0, humanize) if humanize else 0.0)
        vel = int(np.clip(v + (rng.normal(0, 6) if humanize else 0), 1, 127))
        y = voice(int(m), vel, round(l * spb, 3))
        i = max(0, int(t0 * SR))
        n = min(len(y), len(out) - i)
        out[i:i + n] += y[:n]
    return out


# ------------------------------------------------------------------ FM e-piano
def fm_ep(bright=1.0, tine=0.5, bark=0.3, decay=2.2):
    @functools.lru_cache(maxsize=4096)
    def voice(m, vel, dur_s):
        f = hz(m)
        v = vel / 127.0
        n = int((dur_s + 1.2) * SR)
        t = np.arange(n) / SR
        dk = decay * (220.0 / f) ** 0.35
        amp = np.exp(-t / dk)
        off = int(dur_s * SR)
        if off < n:
            amp[off:] *= np.exp(-(t[off:] - t[off]) / 0.12)
        idx = bright * (0.4 + 1.6 * v) * np.exp(-t / 0.35) + 0.15 * bright
        car = np.sin(2 * np.pi * f * t + idx * np.sin(2 * np.pi * f * t))
        tn = tine * v * np.sin(2 * np.pi * f * 7.0 * t + 0.6 * np.sin(2 * np.pi * f * t)) * np.exp(-t / 0.045)
        y = (car + tn) * amp * (0.25 + 0.75 * v)
        if bark > 0:
            d = 1 + 4 * bark * v
            y = np.tanh(d * (y + 0.15 * bark)) / np.tanh(d) - np.tanh(d * 0.15 * bark) / np.tanh(d)
        a = np.minimum(1.0, t / 0.002)
        y = (y * a * 0.3).astype(np.float32)
        return np.stack([y, y], 1)
    return voice


# ------------------------------------------------------------------ organ
FOOT = [0.5, 1.5, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0]
REG = {"jazz": "888000000", "gospel": "888888888", "house": "008800080",
       "soul": "868000004", "pop": "807000000"}


def organ(reg="jazz", perc=0.6, click=0.3, seed=0):
    bars = [int(c) / 8.0 for c in REG.get(reg, reg)]
    rng = np.random.default_rng(seed)

    @functools.lru_cache(maxsize=4096)
    def voice(m, vel, dur_s):
        f = hz(m)
        n = int((dur_s + 0.15) * SR)
        t = np.arange(n) / SR
        y = np.zeros(n)
        for r, b in zip(FOOT, bars):
            if b > 0 and f * r < 16000:
                y += b * np.sin(2 * np.pi * f * r * t + rng.uniform(0, 6.28))
        if perc > 0:
            y += perc * 1.2 * np.sin(2 * np.pi * f * 3.0 * t) * np.exp(-t / 0.25)
        env = np.minimum(1, t / 0.004)
        off = int(dur_s * SR)
        if off < n:
            env[off:] *= np.exp(-(t[off:] - t[off]) / 0.02)
        y *= env
        if click > 0:
            k = int(0.006 * SR)
            y[:k] += click * rng.normal(0, 1, k) * np.exp(-np.arange(k) / (0.0015 * SR))
        y = (0.08 * y).astype(np.float32)
        return np.stack([y, y], 1)
    return voice


def leslie(x, rate=6.0, depth=0.35):
    """Rotary speaker: amplitude + stereo modulation, plus a light chorus."""
    import pedalboard as pb
    t = np.arange(len(x)) / SR
    l = 1 - depth * 0.5 * (1 + np.sin(2 * np.pi * rate * t))
    r = 1 - depth * 0.5 * (1 + np.sin(2 * np.pi * rate * t + 1.7))
    y = x * np.stack([l, r], 1).astype(np.float32)
    ch = pb.Pedalboard([pb.Chorus(rate_hz=rate * 0.9, depth=0.15, centre_delay_ms=3, feedback=0.0, mix=0.4)])
    return ch(y.T.copy(), SR).T


# ------------------------------------------------------------------ effects
def _stft_filter(x, centers, q, n_fft=2048, hop=256):
    """Time-varying band-pass of a mono signal: centers[i] per hop (Hz)."""
    import librosa
    S = librosa.stft(x, n_fft=n_fft, hop_length=hop)
    f = librosa.fft_frequencies(sr=SR, n_fft=n_fft)[:, None]
    c = np.interp(np.arange(S.shape[1]), np.linspace(0, S.shape[1] - 1, len(centers)), centers)[None, :]
    lf = np.log2(np.maximum(f, 1.0) / c)
    mask = np.exp(-0.5 * (lf * q) ** 2)
    return librosa.istft(S * mask, hop_length=hop, length=len(x))


def _saw(freq_t, rng, voices=5, detune=0.15):
    ph0 = rng.uniform(0, 1, voices)
    y = np.zeros(len(freq_t))
    for i in range(voices):
        d = 2 ** (detune * (i - (voices - 1) / 2) / 12 / max(1, (voices - 1) / 2))
        ph = (np.cumsum(freq_t * d) / SR + ph0[i]) % 1.0
        y += 2 * ph - 1
    return y / voices


def fx_riser(dur, seed=0, q=1.2, f_lo=300, f_hi=9000, tone=0.4, start_note=48, semis=24):
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    u = np.linspace(0, 1, n)
    noise = rng.normal(0, 1, n)
    cent = f_lo * (f_hi / f_lo) ** np.linspace(0, 1, n // 256 + 1)
    nz = _stft_filter(noise, cent, q)
    ft = hz(start_note) * 2 ** (semis * u ** 1.5 / 12)
    sw = _saw(ft, rng)
    y = (1 - tone) * nz / (np.abs(nz).max() + 1e-9) + tone * sw * 0.6
    y *= u ** 2.2
    y[-int(0.01 * SR):] *= np.linspace(1, 0, int(0.01 * SR))
    return np.stack([y, y], 1).astype(np.float32)


def fx_downlifter(dur, seed=0, q=1.0, f_hi=8000, f_lo=200, tone=0.3, start_note=72, semis=-24):
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    u = np.linspace(0, 1, n)
    noise = rng.normal(0, 1, n)
    cent = f_hi * (f_lo / f_hi) ** np.linspace(0, 1, n // 256 + 1) ** 0.6
    nz = _stft_filter(noise, cent, q)
    ft = hz(start_note) * 2 ** (semis * u ** 0.7 / 12)
    sw = np.sin(2 * np.pi * np.cumsum(ft) / SR)
    y = (1 - tone) * nz / (np.abs(nz).max() + 1e-9) + tone * sw
    y *= (1 - u) ** 1.8 * np.minimum(1, u / 0.005)
    return np.stack([y, y], 1).astype(np.float32)


def fx_impact(dur, seed=0, f_start=140, f_end=38, boom_decay=0.9, crack=0.5, crack_decay=0.08):
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f_end + (f_start - f_end) * np.exp(-t / 0.08)
    boom = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / boom_decay)
    nz = rng.normal(0, 1, n) * np.exp(-t / crack_decay)
    nz = _stft_filter(nz, np.array([2500.0, 900.0]), 0.6)
    y = np.tanh(1.5 * boom) + crack * nz / (np.abs(nz).max() + 1e-9)
    y *= np.minimum(1, t / 0.001)
    return np.stack([y, y], 1).astype(np.float32)


def fx_sweep(dur, seed=0, q=2.0, f_lo=400, f_hi=12000, up_frac=0.6):
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    u = np.linspace(0, 1, n // 256 + 1)
    shape = np.where(u < up_frac, u / up_frac, 1 - (u - up_frac) / (1 - up_frac))
    cent = f_lo * (f_hi / f_lo) ** shape
    nz = _stft_filter(rng.normal(0, 1, n), cent, q)
    a = np.interp(np.linspace(0, 1, n), u, 0.3 + 0.7 * shape)
    y = nz * a * np.minimum(1, np.linspace(0, 1, n) / 0.05)
    y[-int(0.05 * SR):] *= np.linspace(1, 0, int(0.05 * SR))
    return np.stack([y, y], 1).astype(np.float32)


# ------------------------------------------------------------------ vocal chops
def vocalset_files(root, singers=None, techniques=("straight", "vibrato", "breathy")):
    """Sustained vowels only (long tones and arpeggios), never lyrics."""
    out = []
    for f in glob.glob(os.path.join(root, "**", "*.wav"), recursive=True):
        b = os.path.basename(f).lower()
        if "excerpt" in f.lower() or "spoken" in f.lower():
            continue
        if not ("long_tones" in f.lower() or "arpeggios" in f.lower() or "scales" in f.lower()):
            continue
        if not any(t in b for t in techniques):
            continue
        if not re.search(r"_[aeiou]\.wav$", b):
            continue
        if singers and not any(b.startswith(s) for s in singers):
            continue
        out.append(f)
    return sorted(out)


@functools.lru_cache(maxsize=256)
def _vocal_slices(path, slice_s=0.35):
    """Stable voiced slices: (audio mono, f0) list."""
    import librosa
    y, sr = sf.read(path, dtype="float32", always_2d=True)
    y = y.mean(1)
    if sr != SR:
        y = librosa.resample(y, orig_sr=sr, target_sr=SR, res_type="soxr_hq")
    y = y[: int(20 * SR)]
    hop = 1024
    f0, vf, _ = librosa.pyin(y, fmin=70, fmax=1200, sr=SR, frame_length=4096, hop_length=hop)
    rms = librosa.feature.rms(y=y, frame_length=4096, hop_length=hop)[0]
    n = min(len(f0), len(rms))
    good = vf[:n] & np.isfinite(f0[:n]) & (rms[:n] > rms.max() * 0.25)
    w = int(slice_s * SR / hop)
    out = []
    i = 0
    while i + w < n:
        seg = f0[i:i + w]
        if good[i:i + w].all() and np.nanstd(12 * np.log2(seg / np.nanmedian(seg))) < 0.4:
            a = i * hop
            out.append((y[a:a + int(slice_s * SR)].copy(), float(np.nanmedian(seg))))
            i += w
        else:
            i += max(1, w // 4)
    return out


def chops(files, pattern, bpm, seed=0, formant_keep=True):
    """pattern: list of (start_beat, len_beats, midi, vel). Each hit takes a
    vowel slice, re-pitches it to midi, cuts it to length with short fades."""
    import pedalboard as pb
    rng = np.random.default_rng(seed)
    pool = []
    for f in files:
        pool += _vocal_slices(f)
    if not pool:
        raise RuntimeError("no vocal slices")
    spb = 60.0 / bpm
    end = max(s + l for s, l, _, _ in pattern)
    out = np.zeros(int((end * spb + 2.0) * SR), np.float32)
    # a small set of slices per phrase so it sounds like one singer's chops
    pick = [pool[i] for i in rng.choice(len(pool), size=min(4, len(pool)), replace=False)]
    for k, (s, l, m, v) in enumerate(pattern):
        y, f0 = pick[k % len(pick)] if rng.random() > 0.3 else pick[rng.integers(len(pick))]
        semis = 12 * np.log2(hz(m) / f0)
        semis = float(np.clip(semis, -12, 12))
        if abs(semis) > 0.05:
            y = pb.PitchShift(semitones=semis)(y[None, :].astype(np.float32), SR)[0]
        n = min(len(y), int(l * spb * SR))
        y = y[:n].copy()
        fi, fo = int(0.004 * SR), int(min(0.03, n / SR / 3) * SR)
        y[:fi] *= np.linspace(0, 1, fi)
        y[-fo:] *= np.linspace(1, 0, fo)
        i = int(s * spb * SR)
        out[i:i + n] += y * (v / 127.0)
    return np.stack([out, out], 1)
