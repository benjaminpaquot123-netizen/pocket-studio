#!/usr/bin/env python3
"""Measure drum one-shots (wav/aif) and write one CSV row per file.

Usage (samples are untrusted data, always isolated mode, paths as arguments):

    python3 -I scripts/sons/mesure.py <samples-folder> --out <file.csv>

The CSV holds numbers derived from the samples: the tool refuses to write it
inside this git repository (public repo, bought samples).

Needs numpy only. Self-test: python3 -I scripts/sons/selftest.py

Method (one method per column, same for every file)
---------------------------------------------------
No resampling: every window is defined in seconds and every band in Hz, the
analysis runs at the file's own rate. Bands stop at 20 kHz so 44.1, 48 and
96 kHz files are comparable.

Stereo: nothing is measured on (L+R)/2 unless it is safe. Envelope, decay,
centroid and band energies use the power summed over channels (no phase
cancellation). Pitch and harmonics need a waveform: the mid signal, or the
loudest channel when the mid loses more than 3 dB (`mono_loss_db`).

onset_ms       first sample whose absolute value reaches -30 dB under the
               absolute peak of the file. All times below start there.
t20/t40/t60_ms Schroeder decay: time after the onset at which the energy
               still to come falls 20/40/60 dB under the total energy after
               the onset (backward-integrated power, ISO 3382 style). The
               curve only goes down: no smoothing window to choose, no first
               dip between two clap bursts, no ripple on a 50 Hz sine. For
               an amplitude decay exp(-t/tau), t20 = tau*ln(10).
end_level_db   mean power of the last 10 ms under the peak power. Above
               about -60 the sound still plays (or is noisy) when the file
               ends, usually trimmed with a fade: t60 (and maybe t40) is then
               empty or not to be trusted. Information only, not a cut
               (tout.py end_cut tells a real cut).
pitch_steady_hz mean frequency of the fundamental over the steady window:
               phase advance of the band-passed analytic signal (0.5 to 1.8
               times the strongest spectral peak of the tail, searched from
               20 Hz to 2 kHz, wide soft edges so the filter does not ring).
               Steady window: it ends at the -30 dB point of the decay curve
               (-40 then -60 until it holds 6 periods) and starts as early
               as the pitch stays within 5 % of the tail pitch, so the glide
               of a kick is left out and a held body is kept.
pitch_attack_hz same phase-advance measure, band 0.5 to 8 times the steady
               pitch, from 2 ms to 12 ms after the onset: the mean frequency
               of that window, not the very first instant of a glide. Under
               100 Hz the window is lengthened to one period of the measured
               pitch. Expect about 5 % of error (the sound has just started).
tonal_db       share of the tail energy (-10 to -40 dB of the decay curve,
               20 Hz to 20 kHz) that sits in the fundamental. Near 0: a
               note. Under about -10: noise, the pitch and harmonic columns
               mean nothing.
h2_db, h3_db   harmonics 2 and 3 against the fundamental, on the steady
               window, by synchronous demodulation: the signal is multiplied
               by exp(-j*k*phase), where phase is a degree-5 polynomial
               fitted to the phase of the fundamental, and averaged with a
               Blackman-Harris window. It follows a pitch that still moves
               (no fixed FFT bin). It sees harmonics locked to the
               fundamental (distortion), not inharmonic partials. Empty when
               the steady window is shorter than 6 periods.
centroid_hz    power-weighted spectral centroid, sum(f*P)/sum(P), 20 Hz to
               20 kHz, from 1 ms before the onset to the -60 dB point, no
               taper (a Hann window there would erase the attack).
hf_db          energy from 10 to 20 kHz against 20 Hz to 20 kHz, same span.
transient_body_db energy of the first 10 ms after the onset against the
               energy of everything after.
"""
import argparse
import csv
import math
import os
import struct
import sys

import numpy as np

MAX_BYTES = 256 * 1024 * 1024
MAX_SECONDS = 60.0
EXTS = (".wav", ".wave", ".aif", ".aiff", ".aifc")

ONSET_DB = -30.0
ATTACK_WIN = (0.002, 0.012)
TRANSIENT_S = 0.010
MIN_PERIODS = 6.0
STEADY_TOL = 0.05
PHASE_DEGREE = 5
F_LO, F_HI = 20.0, 20000.0

COLUMNS = [
    "folder", "file", "sr", "channels", "dur_ms", "peak_dbfs", "onset_ms",
    "t20_ms", "t40_ms", "t60_ms", "end_level_db", "mono_loss_db",
    "pitch_attack_hz", "pitch_steady_hz", "tonal_db", "h2_db", "h3_db",
    "centroid_hz", "hf_db", "transient_body_db", "error",
]


class BadAudio(Exception):
    pass


# ---------------------------------------------------------------- reading

def _pcm(raw, width, channels, big, floating, unsigned8=False):
    frame = width * channels
    n = len(raw) // frame
    if n <= 0:
        raise BadAudio("no audio frames")
    raw = raw[: n * frame]
    order = ">" if big else "<"
    if floating:
        if width not in (4, 8):
            raise BadAudio("unsupported float width")
        x = np.frombuffer(raw, dtype=order + "f%d" % width).astype(np.float64)
    elif width == 1:
        if unsigned8:
            x = (np.frombuffer(raw, dtype=np.uint8).astype(np.float64) - 128.0) / 128.0
        else:
            x = np.frombuffer(raw, dtype=np.int8).astype(np.float64) / 128.0
    elif width == 2:
        x = np.frombuffer(raw, dtype=order + "i2").astype(np.float64) / 32768.0
    elif width == 3:
        b = np.frombuffer(raw, dtype=np.uint8).reshape(-1, 3).astype(np.int32)
        if big:
            v = (b[:, 0] << 16) | (b[:, 1] << 8) | b[:, 2]
        else:
            v = (b[:, 2] << 16) | (b[:, 1] << 8) | b[:, 0]
        v = np.where(v >= 1 << 23, v - (1 << 24), v)
        x = v.astype(np.float64) / float(1 << 23)
    elif width == 4:
        x = np.frombuffer(raw, dtype=order + "i4").astype(np.float64) / float(1 << 31)
    else:
        raise BadAudio("unsupported sample width")
    if not np.all(np.isfinite(x)):
        raise BadAudio("non-finite samples")
    return x.reshape(n, channels)


def _chunks(buf, start, big):
    fmt = ">I" if big else "<I"
    pos = start
    while pos + 8 <= len(buf):
        cid = buf[pos:pos + 4]
        size = struct.unpack(fmt, buf[pos + 4:pos + 8])[0]
        yield cid, pos + 8, min(size, len(buf) - pos - 8)
        pos += 8 + size + (size & 1)


def check_whole(buf):
    """Raise BadAudio when the sound chunk (WAV data / AIFF SSND) declares more
    bytes than the file holds: a cut download measured as a short sound
    (audit 2, defect 34). Streamed files (size 0 or 0xFFFFFFFF) and a few
    missing bytes (odd padding, sloppy writers) pass."""
    big = buf[:4] == b"FORM"
    want = b"SSND" if big else b"data"
    pos = 12
    while pos + 8 <= len(buf):
        size = struct.unpack(">I" if big else "<I", buf[pos + 4:pos + 8])[0]
        if buf[pos:pos + 4] == want:
            if size in (0, 0xFFFFFFFF):
                return
            missing = size - (len(buf) - pos - 8)
            if missing > max(256, size // 100):
                raise BadAudio("truncated %s (%d of %d bytes)" % ("aiff" if big else "wav",
                                                                   size - missing, size))
            return
        pos += 8 + size + (size & 1)


def _check(sr, channels):
    if not 1 <= channels <= 8:
        raise BadAudio("unsupported channel count")
    if not 8000 <= sr <= 384000:
        raise BadAudio("unsupported sample rate")


def _read_wav(buf):
    fmt = None
    for cid, off, size in _chunks(buf, 12, big=False):
        if cid == b"fmt " and size >= 16:
            tag, channels, sr, _, align, bits = struct.unpack("<HHIIHH", buf[off:off + 16])
            if tag == 0xFFFE and size >= 26:
                tag = struct.unpack("<H", buf[off + 24:off + 26])[0]
            fmt = (tag, channels, sr, align, bits)
        elif cid == b"data":
            if fmt is None:
                raise BadAudio("data before fmt")
            tag, channels, sr, align, bits = fmt
            _check(sr, channels)
            if tag not in (1, 3):
                raise BadAudio("compressed wav (format %d)" % tag)
            width = align // channels if align else (bits + 7) // 8
            if size == 0 and struct.unpack("<I", buf[off - 4:off])[0] == 0:
                size = len(buf) - off  # streamed writer never filled the size in
            x = _pcm(buf[off:off + size], width, channels, big=False,
                     floating=(tag == 3), unsigned8=True)
            return sr, x
    raise BadAudio("no data chunk")


def _ext80(b):
    exp, mant = struct.unpack(">HQ", b)
    sign = -1.0 if exp & 0x8000 else 1.0
    exp &= 0x7FFF
    if exp == 0 and mant == 0:
        return 0.0
    if exp == 0x7FFF:
        raise BadAudio("bad sample rate")
    return sign * mant * 2.0 ** (exp - 16383 - 63)


def _read_aiff(buf):
    comm = None
    for cid, off, size in _chunks(buf, 12, big=True):
        if cid == b"COMM" and size >= 18:
            channels, _, bits = struct.unpack(">hIh", buf[off:off + 8])
            sr = int(round(_ext80(buf[off + 8:off + 18])))
            comp = buf[off + 18:off + 22] if size >= 22 else b"NONE"
            comm = (channels, bits, sr, comp)
        elif cid == b"SSND" and size >= 8:
            if comm is None:
                raise BadAudio("SSND before COMM")
            channels, bits, sr, comp = comm
            _check(sr, channels)
            skip = struct.unpack(">I", buf[off:off + 4])[0]
            raw = buf[off + 8 + skip:off + size]
            if comp in (b"NONE", b"twos"):
                return sr, _pcm(raw, (bits + 7) // 8, channels, True, False)
            if comp == b"sowt":
                return sr, _pcm(raw, (bits + 7) // 8, channels, False, False)
            if comp in (b"fl32", b"FL32"):
                return sr, _pcm(raw, 4, channels, True, True)
            if comp in (b"fl64", b"FL64"):
                return sr, _pcm(raw, 8, channels, True, True)
            raise BadAudio("compressed aiff")
    raise BadAudio("no SSND chunk")


def read_audio(path):
    """Return (sample_rate, float64 array [frames, channels])."""
    if os.path.islink(path) or not os.path.isfile(path):
        raise BadAudio("not a regular file")
    if os.path.getsize(path) > MAX_BYTES:
        raise BadAudio("file too large")
    with open(path, "rb") as f:
        buf = f.read(MAX_BYTES + 1)
    if len(buf) < 12:
        raise BadAudio("too short")
    if buf[:4] == b"RIFF" and buf[8:12] == b"WAVE":
        sr, x = _read_wav(buf)
    elif buf[:4] == b"FORM" and buf[8:12] in (b"AIFF", b"AIFC"):
        sr, x = _read_aiff(buf)
    else:
        raise BadAudio("not a wav or aiff file")
    check_whole(buf)
    if x.shape[0] > MAX_SECONDS * sr:
        raise BadAudio("longer than %d s, not a one-shot" % MAX_SECONDS)
    return sr, x


# -------------------------------------------------------------- measuring

def _db10(v):
    return 10.0 * math.log10(v) if v > 0 else float("-inf")


def _analytic_band(sig, sr, lo, hi):
    """Analytic signal of `sig` limited to lo..hi Hz.

    Wide raised-cosine edges (0.2*lo..lo and hi..1.45*hi): a narrow filter
    would ring longer than a short drum and be measured in its place.
    """
    n = len(sig)
    spec = np.fft.fft(sig)
    f = np.fft.fftfreq(n, 1.0 / sr)
    gain = np.zeros(n)
    e_lo, e_hi = 0.8 * lo, 0.45 * hi
    pos = f > 0
    gain[pos & (f >= lo) & (f <= hi)] = 1.0
    m = pos & (f > lo - e_lo) & (f < lo)
    gain[m] = 0.5 - 0.5 * np.cos(np.pi * (f[m] - (lo - e_lo)) / e_lo)
    m = pos & (f > hi) & (f < hi + e_hi)
    gain[m] = 0.5 + 0.5 * np.cos(np.pi * (f[m] - hi) / e_hi)
    return np.fft.ifft(2.0 * spec * gain)


def _mean_freq(z, sr, a, b):
    """Mean frequency of analytic signal z between samples a and b."""
    if b - a < 2:
        return float("nan")
    ph = np.unwrap(np.angle(z[a:b + 1]))
    return float((ph[-1] - ph[0]) * sr / (2.0 * math.pi * (b - a)))


def _blackman_harris(n):
    k = 2.0 * np.pi * np.arange(n) / max(n - 1, 1)
    return 0.35875 - 0.48829 * np.cos(k) + 0.14128 * np.cos(2 * k) - 0.01168 * np.cos(3 * k)


def measure(sr, x):
    """Measure one sound. x: float array [frames, channels]. Returns a dict."""
    x = np.asarray(x, dtype=np.float64)
    if x.ndim == 1:
        x = x[:, None]
    n, ch = x.shape
    out = {"sr": sr, "channels": ch, "dur_ms": 1000.0 * n / sr}
    nan = float("nan")
    for k in COLUMNS[5:-1]:
        out[k] = nan

    absmax = np.max(np.abs(x), axis=1)
    peak = float(absmax.max()) if n else 0.0
    if peak <= 0.0:
        raise BadAudio("silent file")
    out["peak_dbfs"] = 20.0 * math.log10(peak)

    power = np.mean(x * x, axis=1)
    onset = int(np.argmax(absmax >= peak * 10.0 ** (ONSET_DB / 20.0)))
    out["onset_ms"] = 1000.0 * onset / sr

    # Schroeder decay curve from the onset.
    edc = np.cumsum(power[::-1])[::-1]
    total = float(edc[onset])
    idx = {}
    for level in (10, 20, 30, 40, 60):
        below = np.nonzero(edc[onset:] < total * 10.0 ** (-level / 10.0))[0]
        idx[level] = onset + int(below[0]) if len(below) else n
    for level in (20, 40, 60):
        out["t%d_ms" % level] = 1000.0 * (idx[level] - onset) / sr
    last = max(1, int(0.010 * sr))
    out["end_level_db"] = _db10(float(np.mean(power[-last:])) / (peak * peak))

    # Waveform used for pitch and harmonics.
    mid = np.mean(x, axis=1)
    out["mono_loss_db"] = _db10(float(np.mean(mid * mid)) / float(np.mean(power)))
    if out["mono_loss_db"] < -3.0:
        mid = x[:, int(np.argmax(np.sum(x * x, axis=0)))]

    # Transient against body.
    cut = onset + int(round(TRANSIENT_S * sr))
    e_t, e_b = float(np.sum(power[onset:cut])), float(np.sum(power[cut:]))
    if e_t > 0 and e_b > 0:
        out["transient_body_db"] = _db10(e_t / e_b)

    # Centroid and high band: channel-summed power spectrum, no taper.
    a = max(0, onset - int(0.001 * sr))
    seg = x[a:max(idx[60], a + 16)]
    spec = np.sum(np.abs(np.fft.rfft(seg, axis=0)) ** 2, axis=1)
    f = np.fft.rfftfreq(len(seg), 1.0 / sr)
    band = (f >= F_LO) & (f <= F_HI)
    tot = float(spec[band].sum())
    if tot > 0:
        out["centroid_hz"] = float((f[band] * spec[band]).sum() / tot)
        out["hf_db"] = _db10(float(spec[band & (f >= 10000.0)].sum()) / tot)

    # Steady pitch: strongest peak of the tail, then phase advance.
    t0, t1 = idx[10], idx[40]
    if t1 - t0 < int(0.020 * sr):
        return out
    tail = mid[t0:t1]
    nfft = 1 << max(16, int(math.ceil(math.log2(len(tail) * 8))))
    ps = np.abs(np.fft.rfft(tail * np.hanning(len(tail)), nfft)) ** 2
    pf = np.fft.rfftfreq(nfft, 1.0 / sr)
    sel = (pf >= F_LO) & (pf <= min(2000.0, 0.4 * sr))
    f_est = float(pf[sel][int(np.argmax(ps[sel]))])
    all_band = (pf >= F_LO) & (pf <= F_HI)
    half = 0.06 * f_est + 2.0 * sr / len(tail)
    fund = (pf >= f_est - half) & (pf <= f_est + half) & all_band
    if ps[all_band].sum() > 0:
        out["tonal_db"] = _db10(float(ps[fund].sum()) / float(ps[all_band].sum()))

    body = mid[onset:]
    period = sr / f_est
    z = _analytic_band(body, sr, 0.5 * f_est, 1.8 * f_est)
    ph = np.unwrap(np.angle(z))
    r1 = min(idx[30] - onset, len(body) - 1)
    r0 = max(min(t0 - onset, r1 - int(3 * period)), 0)
    f_ref = (ph[r1] - ph[r0]) * sr / (2.0 * math.pi * max(r1 - r0, 1))
    if not (f_ref > 0):
        return out

    # Steady window: walk back from the tail while the pitch stays within 5 %.
    span = max(int(period), 1)
    first = min(span, r0)
    start = r0
    if r0 - span > first:
        pos = np.arange(first, r0 - span + 1)
        track = (ph[pos + span] - ph[pos - span]) * sr / (4.0 * math.pi * span)
        off = np.nonzero(np.abs(track / f_ref - 1.0) > STEADY_TOL)[0]
        start = int(pos[off[-1]]) + 1 if len(off) else first
    end = None
    for level in (30, 40, 60):
        e = min(idx[level] - onset, len(body) - 1)
        if (e - start) / sr >= MIN_PERIODS / f_ref:
            end = e
            break
    stop = end if end is not None else r1
    if stop - start < 2:
        return out
    f_steady = (ph[stop] - ph[start]) * sr / (2.0 * math.pi * (stop - start))
    out["pitch_steady_hz"] = float(f_steady)

    # Attack pitch. A window shorter than one period cannot tell a pitch, so
    # it grows to one period of what it measures when that is under 100 Hz.
    za = _analytic_band(body, sr, 0.5 * f_steady, min(8.0 * f_steady, 0.45 * sr))
    a0, a1 = int(ATTACK_WIN[0] * sr), int(ATTACK_WIN[1] * sr)
    f_att = _mean_freq(za, sr, a0, a1) if a1 < len(body) else nan
    for _ in range(4):
        if not f_att > 0:
            break
        need = a0 + int(round(sr / f_att))
        if need <= a1 or need >= len(body):
            break
        f_att = _mean_freq(za, sr, a0, need)
    if f_att > 0:
        out["pitch_attack_hz"] = f_att

    # Harmonics by synchronous demodulation on the steady window. The phase
    # is a smooth polynomial fitted to the fundamental's phase, so it follows
    # a slow glide and ignores the ripple that the harmonics put on it.
    if end is None:
        return out
    seg = body[start:end]
    w = _blackman_harris(len(seg))
    u = np.linspace(-1.0, 1.0, len(seg))
    wfit = w * np.abs(z[start:end])
    fit = np.polynomial.polynomial.polyfit(u, ph[start:end], PHASE_DEGREE, w=wfit)
    phs = np.polynomial.polynomial.polyval(u, fit)
    c1 = abs(np.sum(w * seg * np.exp(-1j * phs)))
    for k in (2, 3):
        if k * f_steady * 1.1 < 0.5 * sr and c1 > 0:
            ck = abs(np.sum(w * seg * np.exp(-1j * k * phs)))
            out["h%d_db" % k] = 20.0 * math.log10(max(ck / c1, 1e-10))
    return out


def measure_file(path):
    sr, x = read_audio(path)
    return measure(sr, x)


# -------------------------------------------------------------------- cli

def _safe_cell(text):
    """Printable text that a spreadsheet will not run as a formula."""
    text = "".join(c if c.isprintable() else "?" for c in text)
    return "'" + text if text[:1] and text[:1] in "=+-@" else text


def _fmt(v):
    if isinstance(v, float):
        return "" if not math.isfinite(v) else "%.4g" % v if abs(v) < 1e4 else "%.1f" % v
    return v


def _inside_repo(path):
    repo = os.path.realpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
    # a zip export or a copy has no .git: the project files still mark the
    # repository (audit 2, defect 39)
    if not any(os.path.exists(os.path.join(repo, m)) for m in (".git", "CLAUDE.md", os.path.join("docs", "sons"))):
        return False
    path = os.path.realpath(path)
    return path == repo or path.startswith(repo + os.sep)


def main(argv=None):
    ap = argparse.ArgumentParser(description="Measure drum one-shots into a CSV.")
    ap.add_argument("folder", help="folder of wav/aif files (searched recursively)")
    ap.add_argument("--out", required=True, help="CSV to write, outside the git repository")
    args = ap.parse_args(argv)
    if not os.path.isdir(args.folder):
        ap.error("not a folder: %s" % args.folder)
    if _inside_repo(args.out):
        ap.error("refusing to write measurements of samples inside the repository")

    paths = []
    for root, dirs, files in os.walk(args.folder, followlinks=False):
        dirs.sort()
        for name in sorted(files):
            if name.lower().endswith(EXTS) and not name.startswith("._"):
                paths.append(os.path.join(root, name))

    failed = 0
    with open(args.out, "w", newline="", encoding="utf-8") as fh:
        wr = csv.DictWriter(fh, fieldnames=COLUMNS)
        wr.writeheader()
        for p in paths:
            row = {"folder": _safe_cell(os.path.relpath(os.path.dirname(p), args.folder)),
                   "file": _safe_cell(os.path.basename(p)), "error": ""}
            try:
                with np.errstate(all="ignore"):
                    row.update(measure_file(p))
            except BadAudio as e:
                row["error"] = str(e)
                failed += 1
            except Exception as e:  # a hostile file must not stop the batch
                row["error"] = "internal: %s" % type(e).__name__
                failed += 1
            wr.writerow({k: _fmt(row.get(k, "")) for k in COLUMNS})
    print("%d files, %d unreadable -> %s" % (len(paths), failed, args.out))
    return 0 if paths else 1


if __name__ == "__main__":
    sys.exit(main())
