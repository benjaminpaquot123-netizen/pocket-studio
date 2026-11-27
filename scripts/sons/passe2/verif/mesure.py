#!/usr/bin/env python3
"""Independent counter-measure of the sound tables (numpy only).

Written from the column DEFINITIONS, not from the code of scripts/sons/tout.py
or scripts/sons/mesure.py: nothing is imported from them. Own WAV / AIFF
reader, own decay, spectrum and pitch code. See TOLERANCES.md for how the
two tools are compared.

  python3 -I mesure.py FILE [--family kick|melodic]   # prints one json line
"""
import json
import math
import struct
import sys

import numpy as np

AUDIO_EXTS = (".wav", ".wave", ".aif", ".aiff", ".aifc")
MAX_SECONDS = 600.0


class BadAudio(Exception):
    pass


# ---------------------------------------------------------------- reader

def _pcm(raw, bits, big, ch):
    w = bits // 8
    n = len(raw) // (w * ch) * w * ch
    raw = raw[:n]
    if w == 1:
        a = np.frombuffer(raw, dtype=np.uint8).astype(np.float64)
        a = (a - 128.0) / 128.0 if not big else np.frombuffer(raw, dtype=np.int8).astype(np.float64) / 128.0
    elif w == 2:
        a = np.frombuffer(raw, dtype=">i2" if big else "<i2").astype(np.float64) / 32768.0
    elif w == 3:
        b = np.frombuffer(raw, dtype=np.uint8).reshape(-1, 3).astype(np.int32)
        if big:
            v = (b[:, 0] << 16) | (b[:, 1] << 8) | b[:, 2]
        else:
            v = (b[:, 2] << 16) | (b[:, 1] << 8) | b[:, 0]
        v = np.where(v >= 1 << 23, v - (1 << 24), v)
        a = v.astype(np.float64) / float(1 << 23)
    elif w == 4:
        a = np.frombuffer(raw, dtype=">i4" if big else "<i4").astype(np.float64) / 2147483648.0
    else:
        raise BadAudio("unsupported bit depth %d" % bits)
    return a.reshape(-1, ch)


def _float(raw, bits, big, ch):
    w = bits // 8
    n = len(raw) // (w * ch) * w * ch
    dt = {4: "f4", 8: "f8"}.get(w)
    if dt is None:
        raise BadAudio("unsupported float depth %d" % bits)
    a = np.frombuffer(raw[:n], dtype=(">" if big else "<") + dt).astype(np.float64)
    return a.reshape(-1, ch)


def _ext80(b):
    exp = ((b[0] & 0x7F) << 8) | b[1]
    mant = int.from_bytes(b[2:10], "big")
    if exp == 0 and mant == 0:
        return 0.0
    return mant * 2.0 ** (exp - 16383 - 63)


def _chunks(buf, start, big):
    i = start
    fmt = ">I" if big else "<I"
    while i + 8 <= len(buf):
        cid = buf[i:i + 4]
        size = struct.unpack(fmt, buf[i + 4:i + 8])[0]
        yield cid, i + 8, size
        i += 8 + size + (size & 1)


def _read_wav(buf):
    rf64 = buf[:4] == b"RF64"
    fmt = data = None
    big_size = None
    for cid, off, size in _chunks(buf, 12, False):
        if cid == b"ds64":
            big_size = struct.unpack("<Q", buf[off + 8:off + 16])[0]
        elif cid == b"fmt ":
            fmt = buf[off:off + size]
        elif cid == b"data":
            if rf64 and big_size is not None and size == 0xFFFFFFFF:
                size = big_size
            data = buf[off:off + size]  # a short file keeps what is there
            break
    if fmt is None or data is None:
        raise BadAudio("wav without fmt or data")
    tag, ch, sr = struct.unpack("<HHI", fmt[:8])
    bits = struct.unpack("<H", fmt[14:16])[0]
    if tag == 0xFFFE and len(fmt) >= 26:
        tag = struct.unpack("<H", fmt[24:26])[0]
    if ch < 1:
        raise BadAudio("no channel")
    if tag == 1:
        x = _pcm(data, bits, False, ch)
    elif tag == 3:
        x = _float(data, bits, False, ch)
    else:
        raise BadAudio("wav format tag %d" % tag)
    return sr, x


def _read_aiff(buf):
    kind = buf[8:12]
    comm = ssnd = None
    for cid, off, size in _chunks(buf, 12, True):
        if cid == b"COMM":
            comm = buf[off:off + size]
        elif cid == b"SSND":
            o = struct.unpack(">I", buf[off:off + 4])[0]
            ssnd = buf[off + 8 + o:off + size]
    if comm is None or ssnd is None:
        raise BadAudio("aiff without COMM or SSND")
    ch, frames, bits = struct.unpack(">hIh", comm[:8])
    sr = _ext80(comm[8:18])
    comp = comm[18:22] if kind == b"AIFC" and len(comm) >= 22 else b"NONE"
    if ch < 1:
        raise BadAudio("no channel")
    if comp in (b"NONE", b"twos"):
        x = _pcm(ssnd, bits, True, ch)
    elif comp == b"sowt":
        x = _pcm(ssnd, bits, False, ch)
    elif comp in (b"fl32", b"FL32"):
        x = _float(ssnd, 32, True, ch)
    elif comp in (b"fl64", b"FL64"):
        x = _float(ssnd, 64, True, ch)
    else:
        raise BadAudio("aifc compression %r" % comp)
    x = x[:frames]
    return int(round(sr)), x


def read_audio(path):
    """(sr, float64 [frames, ch])."""
    with open(path, "rb") as f:
        buf = f.read()
    if buf[:4] in (b"RIFF", b"RF64") and buf[8:12] == b"WAVE":
        sr, x = _read_wav(buf)
    elif buf[:4] == b"FORM" and buf[8:12] in (b"AIFF", b"AIFC"):
        sr, x = _read_aiff(buf)
    else:
        raise BadAudio("not wav / aiff")
    if sr <= 0 or len(x) == 0:
        raise BadAudio("empty")
    if len(x) / sr > MAX_SECONDS:
        raise BadAudio("too long")
    return sr, np.nan_to_num(x)


# ---------------------------------------------------------------- helpers

def _db_pow(p):
    return 10.0 * math.log10(p) if p > 0 else None


def mono(x):
    """Mean of channels, or the loudest channel when the mean loses half the power."""
    if x.shape[1] == 1:
        return x[:, 0]
    m = x.mean(axis=1)
    pch = (x ** 2).mean(axis=0)
    if pch.max() > 0 and (m ** 2).mean() < 0.5 * pch.max():
        return x[:, int(np.argmax(pch))]
    return m


def band_fft(sig, sr, lo, hi, analytic=False):
    """FFT band-pass with 10 % raised-cosine edges; optionally the analytic signal."""
    n = len(sig)
    nf = 1 << int(math.ceil(math.log2(max(n, 2))))
    spec = np.fft.fft(sig, nf)
    f = np.abs(np.fft.fftfreq(nf, 1.0 / sr))
    g = np.zeros(nf)
    e_lo, e_hi = 0.1 * lo, 0.1 * hi
    g[(f >= lo) & (f <= hi)] = 1.0
    m = (f >= lo - e_lo) & (f < lo)
    g[m] = 0.5 - 0.5 * np.cos(math.pi * (f[m] - (lo - e_lo)) / e_lo)
    m = (f > hi) & (f <= hi + e_hi)
    g[m] = 0.5 + 0.5 * np.cos(math.pi * (f[m] - hi) / e_hi)
    spec = spec * g
    if analytic:
        k = np.fft.fftfreq(nf)
        spec[k < 0] = 0.0
        spec[k > 0] *= 2.0
        return np.fft.ifft(spec)[:n]
    return np.real(np.fft.ifft(spec))[:n]


def rising_crossings(s):
    """Fractional indices of the upward zero crossings."""
    a, b = s[:-1], s[1:]
    i = np.flatnonzero((a < 0) & (b >= 0))
    return i + (-a[i]) / (b[i] - a[i])


# ---------------------------------------------------------------- measures

def common(sr, x):
    pk = float(np.abs(x).max())
    return {"dur_s": len(x) / sr,
            "peak_dbfs": 20.0 * math.log10(pk) if pk > 0 else None,
            "rms_dbfs": _db_pow(float((x ** 2).mean()))}


def _decay_points(power, onset):
    """Schroeder backward integral from the onset; {N: samples after onset}."""
    e = np.cumsum(power[onset:][::-1])[::-1]
    tot = e[0]
    out = {}
    for n in (10, 20, 40, 60):
        below = np.flatnonzero(e < tot * 10 ** (-n / 10.0))
        out[n] = int(below[0]) if len(below) else len(e)
    return out


def drum(sr, x):
    res = {}
    amp = np.abs(x).max(axis=1)
    peak = amp.max()
    if peak <= 0:
        return res
    onset = int(np.argmax(amp >= peak * 10 ** (-30 / 20.0)))
    power = (x ** 2).mean(axis=1)
    d = _decay_points(power, onset)
    ms = 1000.0 / sr
    res["onset_ms"] = onset * ms
    res["t20_ms"] = d[20] * ms
    res["t40_ms"] = d[40] * ms
    res["t60_ms"] = d[60] * ms
    # spectrum of the hit: 1 ms before the onset to the -60 dB point, channels summed
    a = max(0, onset - int(0.001 * sr))
    b = max(a + 16, onset + d[60])
    seg = x[a:b]
    spec = (np.abs(np.fft.rfft(seg, axis=0)) ** 2).sum(axis=1)
    f = np.fft.rfftfreq(len(seg), 1.0 / sr)
    band = (f >= 20) & (f <= 20000)
    tot = spec[band].sum()
    if tot > 0:
        res["centroid_hz"] = float((f[band] * spec[band]).sum() / tot)
        res["hf_db"] = _db_pow(float(spec[band & (f >= 10000)].sum() / tot))
    # first 10 ms against everything after
    k = int(0.010 * sr)
    head, body = power[onset:onset + k].sum(), power[onset + k:].sum()
    if head > 0 and body > 0:
        res["transient_body_db"] = 10 * math.log10(head / body)
    # tonal part of the tail (-10 dB to -40 dB points)
    m = mono(x)
    tail = m[onset + d[10]:onset + d[40]]
    if len(tail) >= int(0.020 * sr):
        nf = 1 << max(16, int(math.ceil(math.log2(len(tail) * 8))))
        p = np.abs(np.fft.rfft(tail * np.hanning(len(tail)), nf)) ** 2
        fr = np.fft.rfftfreq(nf, 1.0 / sr)
        sel = (fr >= 20) & (fr <= min(2000.0, 0.4 * sr))
        f_est = float(fr[sel][np.argmax(p[sel])])
        half = 0.06 * f_est + 2.0 * sr / len(tail)
        full = p[(fr >= 20) & (fr <= 20000)].sum()
        near = p[(fr >= f_est - half) & (fr <= f_est + half)].sum()
        if full > 0 and near > 0:
            res["tonal_db"] = 10 * math.log10(near / full)
        st = steady_pitch(m, sr, onset, d, f_est)
        if st:
            res["pitch_steady_hz"] = st
            at = attack_pitch(m, sr, onset, st)
            if at:
                res["pitch_attack_hz"] = at
    return res


def steady_pitch(m, sr, onset, d, f_est):
    """Median period of the upward crossings of the band 0.5-1.8 f_est between
    the -10 dB and -40 dB points (stretched to the -60 dB point for 6 periods)."""
    s = band_fft(m, sr, 0.5 * f_est, 1.8 * f_est)
    a, b = onset + d[10], onset + d[40]
    if (b - a) < 6 * sr / f_est:
        b = onset + d[60]
    z = rising_crossings(s[a:b])
    if len(z) < 4:
        return None
    per = np.diff(z)
    return float(sr / np.median(per))


def attack_pitch(m, sr, onset, f_steady):
    """Mean frequency 2 to 12 ms after the onset (analytic phase of the band
    0.5-8 f_steady), stretched to one period below 100 Hz."""
    z = band_fft(m, sr, 0.5 * f_steady, min(8 * f_steady, 0.45 * sr), analytic=True)
    a = onset + int(0.002 * sr)
    t = max(0.010, 1.0 / f_steady if f_steady < 100 else 0.010)
    b = a + int(round(t * sr))
    if b >= len(z):
        return None
    ph = np.unwrap(np.angle(z[a:b + 1]))
    f = (ph[-1] - ph[0]) / (2 * math.pi * (b - a) / sr)
    return float(f) if f > 0 else None


def melodic_f0(sr, x, lo=25.0, hi=2000.0):
    """Strongest spectral peak with a harmonic sum (fundamental, not h2), 30 ms to 1 s after the onset."""
    m = mono(x)
    amp = np.abs(m)
    if amp.max() <= 0:
        return None
    on = int(np.argmax(amp >= 0.1 * amp.max()))
    seg = m[on + int(0.03 * sr):on + int(1.0 * sr)]
    if len(seg) < 1024:
        seg = m[on:on + int(1.0 * sr)]
    if len(seg) < 256:
        return None
    n = 1 << int(math.ceil(math.log2(max(len(seg) * 4, 1 << 16))))
    s = np.abs(np.fft.rfft(seg * np.hanning(len(seg)), n))
    fr = np.fft.rfftfreq(n, 1.0 / sr)
    idx = np.flatnonzero((fr >= lo) & (fr <= hi))
    if not len(idx):
        return None
    top = s[idx].max()
    best, bi = -1.0, None
    for i in idx:
        if s[i] < 0.05 * top or s[i] < s[i - 1] or s[i] < s[i + 1]:
            continue
        sc = s[i] + 0.5 * s[min(2 * i, len(s) - 1)] + 0.33 * s[min(3 * i, len(s) - 1)]
        if sc > best:
            best, bi = sc, i
    if bi is None:
        return None
    a, b, c = np.log(s[bi - 1:bi + 2] + 1e-12)
    den = a - 2 * b + c
    p = 0.5 * (a - c) / den if den != 0 else 0.0
    return float((bi + p) * sr / n)


def measure(path, kind):
    """kind: 'drum_oneshot', 'oneshot_melodic' or anything else (common only)."""
    sr, x = read_audio(path)
    out = common(sr, x)
    if kind == "drum_oneshot":
        out.update(drum(sr, x))
    elif kind == "oneshot_melodic":
        out["f0_hz"] = melodic_f0(sr, x)
    return out


if __name__ == "__main__":
    kind = sys.argv[3] if len(sys.argv) > 3 and sys.argv[2] == "--family" else "drum_oneshot"
    print(json.dumps(measure(sys.argv[1], kind)))
