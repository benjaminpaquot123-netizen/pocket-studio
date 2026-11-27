"""Fingerprint of a note instrument (bass, 808, lead, pad, keys, pluck), each number twice.

Bench tool, not used by the app. See docs/sons/fin/TIMBRE-INSTRUMENTS-OUTIL.md.
Every measure has a counter-measure computed by another path (time domain against frequency
domain, or another estimator). A measure is "sure" only when both paths agree within the
tolerance written in TOL; otherwise it is reported as "décroche" and must not be trusted.

Input: float stereo array (frames, 2) at 48 kHz, plus the note-off time in seconds.
"""
import math

import numpy as np

SR = 48000
N_HARM = 16

# Tolerances between measure and counter-measure: (relative, absolute floor).
TOL = {
    "note_hz": (0.005, 0.0),
    "pente_db_oct": (0.0, 0.5),
    "pair_impair_db": (0.0, 0.5),
    "inharm_b": (0.10, 2e-5),
    "brillance_attaque": (0.05, 0.0),
    "brillance_tenue": (0.05, 0.0),
    "chute_filtre_s": (0.15, 0.010),
    "largeur_sm_db": (0.0, 1.0),
    "desaccord_cents": (0.15, 1.0),
    "vibrato_hz": (0.05, 0.2),
    "vibrato_cents": (0.10, 1.0),
    "attaque_s": (0.10, 0.003),
    "chute_s": (0.10, 0.010),
    "relache_s": (0.10, 0.010),
    "souffle_hnr_db": (0.0, 2.0),
    "evolution_pct": (0.15, 2.0),
    "evolution_periode_s": (0.10, 0.1),
}


ENV_KEYS = ("attaque_s", "chute_s", "relache_s")


def agree(key, a, b, period=0.0):
    """True when two paths agree for this measure (both None also counts: nothing to measure).

    For the envelope times the floor is at least one period of the note: an envelope is not
    resolved finer than one cycle of the wave it rides on (32 ms on a low C, 4 ms on a C5)."""
    if a is None or b is None:
        return a is None and b is None
    rel, floor = TOL[key]
    if key in ENV_KEYS:
        floor = max(floor, period)
    return abs(a - b) <= max(floor, rel * max(abs(a), abs(b)))


# ---------------------------------------------------------------- helpers

def _db(p):
    return 10.0 * math.log10(max(p, 1e-30))


def _boxcar(z, length, times=2):
    """Moving average of `length` samples applied `times` times (zeros at multiples of SR/length)."""
    k = np.ones(length) / length
    for _ in range(times):
        z = np.convolve(z, k, mode="same")
    return z


def _hetero(x, f, length):
    """Complex envelope of x around frequency f (time-domain demodulation, no FFT)."""
    t = np.arange(len(x)) / SR
    return _boxcar(x * np.exp(-2j * math.pi * f * t), length)


def _env_rms(x, win):
    """Sliding RMS envelope (path A)."""
    k = np.ones(win) / win
    return np.sqrt(np.maximum(np.convolve(x * x, k, mode="same"), 0.0))


def _env_analytic(x, f0):
    """Analytic-signal magnitude smoothed over one period (path B, no squaring)."""
    n = len(x)
    spec = np.fft.fft(x)
    h = np.zeros(n)
    h[0] = 1.0
    h[1:(n + 1) // 2] = 2.0
    if n % 2 == 0:
        h[n // 2] = 1.0
    mag = np.abs(np.fft.ifft(spec * h))
    return _boxcar(mag, max(int(0.004 * SR), int(round(SR / f0))), 1)


# ---------------------------------------------------------------- note (two paths)

def note_time(x):
    """f0 by the cumulative-mean-normalised difference (YIN) on the waveform."""
    seg = x[: min(len(x), int(0.25 * SR))]
    max_lag = int(SR / 25)
    min_lag = int(SR / 2000)
    n = len(seg) - max_lag
    if n < max_lag:
        return None
    spec = np.fft.rfft(seg, 2 * len(seg))
    ac = np.fft.irfft(spec * np.conj(spec))[: max_lag + 1]
    e = np.concatenate(([0.0], np.cumsum(seg * seg)))
    taus = np.arange(max_lag + 1)
    # d(tau) over a fixed window of n samples, computed exactly with sums.
    w0 = e[n] - e[0]
    wt = e[taus + n] - e[taus]
    ac_n = np.array([float(np.dot(seg[:n], seg[t:t + n])) for t in range(max_lag + 1)])
    d = w0 + wt - 2 * ac_n
    cm = np.ones_like(d)
    cs = np.cumsum(d[1:])
    cm[1:] = d[1:] * np.arange(1, len(d)) / np.maximum(cs, 1e-30)
    del ac
    for t in range(min_lag, max_lag - 1):
        if cm[t] < 0.12 and cm[t] <= cm[t + 1]:
            while t + 1 < max_lag and cm[t + 1] < cm[t]:
                t += 1
            a, b, c = cm[t - 1], cm[t], cm[t + 1]
            den = a - 2 * b + c
            off = 0.5 * (a - c) / den if den > 0 else 0.0
            return SR / (t + off)
    return None


def note_freq(x):
    """f0 by harmonic sum on the spectrum, refined by a least-squares fit of the peaks."""
    seg = x[: min(len(x), int(1.0 * SR))] * np.hanning(min(len(x), int(1.0 * SR)))
    nfft = 1 << int(math.ceil(math.log2(len(seg) * 4)))
    mag = np.abs(np.fft.rfft(seg, nfft))
    hz = SR / nfft
    logm = np.log(mag + mag.max() * 1e-5)
    best, best_f = -1e30, None
    for f in 25.0 * 2 ** (np.arange(0, 1200 * math.log2(2000 / 25), 5) / 1200):
        ks = np.arange(1, 9)
        idx = np.round(ks * f / hz).astype(int)
        idx = idx[idx < len(logm)]
        s = logm[idx].mean()
        if s > best:
            best, best_f = s, f
    num = den = 0.0
    for k in range(1, 11):
        c = int(round(k * best_f / hz))
        lo, hi = max(1, int(c - 0.25 * best_f / hz)), int(c + 0.25 * best_f / hz) + 1
        if hi >= len(mag) - 1:
            break
        p = lo + int(np.argmax(mag[lo:hi]))
        a, b, cc = np.log(mag[p - 1] + 1e-30), np.log(mag[p] + 1e-30), np.log(mag[p + 1] + 1e-30)
        d = a - 2 * b + cc
        off = 0.5 * (a - cc) / d if d < 0 else 0.0
        w = mag[p] ** 2
        num += w * k * (p + off) * hz
        den += w * k * k
    return num / den if den > 0 else None


# ---------------------------------------------------------------- harmonics

def amps_freq(x, f0):
    """Power of harmonics 1..16 from the long spectrum (energy within ±f0/2 of k·f0)."""
    seg = x * np.hanning(len(x))
    nfft = 1 << int(math.ceil(math.log2(len(seg) * 2)))
    p = np.abs(np.fft.rfft(seg, nfft)) ** 2
    hz = SR / nfft
    out = []
    for k in range(1, N_HARM + 1):
        lo, hi = int((k - 0.5) * f0 / hz), int((k + 0.5) * f0 / hz)
        out.append(float(p[lo:hi].sum()) if hi < len(p) else 0.0)
    out = np.array(out)
    return out / max(out.max(), 1e-30)


def amps_time(x, f0):
    """Power of harmonics 1..16 by demodulation in the time domain (no FFT)."""
    length = max(2, int(round(SR / f0)))
    w = np.hanning(len(x))
    out = []
    for k in range(1, N_HARM + 1):
        if k * f0 > SR / 2 - f0:
            out.append(0.0)
            continue
        z = _hetero(x, k * f0, length)
        out.append(float(np.mean(np.abs(z * w) ** 2)))
    out = np.array(out)
    return out / max(out.max(), 1e-30)


def slope_from_amps(p):
    """Richness: slope in dB per octave of the harmonic envelope (k=1..16, 60 dB floor)."""
    db = 10 * np.log10(np.maximum(p, 1e-30))
    db = np.maximum(db, db.max() - 60)
    env = np.array([db[max(0, i - 1): i + 2].max() for i in range(len(db))])
    k = np.log2(np.arange(1, len(p) + 1))
    return float(np.polyfit(k, env, 1)[0])


def even_odd_from_amps(p):
    """Even against odd harmonics (2..16 against 3..15), in dB, floored at -60 dB."""
    even = p[1::2].sum()
    odd = p[2::2].sum()
    return max(-60.0, _db(even) - _db(odd))


def inharm_freq(x, f0, strong):
    """B in f_k = k f0 sqrt(1 + B k^2), from the spectral peak frequencies."""
    seg = x * np.hanning(len(x))
    nfft = 1 << int(math.ceil(math.log2(len(seg) * 4)))
    mag = np.abs(np.fft.rfft(seg, nfft))
    hz = SR / nfft
    ks, fs = [], []
    for k in strong:
        lo, hi = int((k - 0.3) * f0 / hz), int((k + 0.3) * f0 / hz)
        if hi >= len(mag) - 1:
            break
        p = lo + int(np.argmax(mag[lo:hi]))
        a, b, c = np.log(mag[p - 1] + 1e-30), np.log(mag[p] + 1e-30), np.log(mag[p + 1] + 1e-30)
        d = a - 2 * b + c
        off = 0.5 * (a - c) / d if d < 0 else 0.0
        ks.append(k)
        fs.append((p + off) * hz)
    return _fit_b(ks, fs)


def inharm_time(x, f0, strong):
    """B from the phase slope of each demodulated harmonic (time domain)."""
    length = max(2, int(round(SR / f0)))
    ks, fs = [], []
    cut = int(0.1 * len(x))
    for k in strong:
        z = _hetero(x, k * f0, length)[cut:-cut]
        ph = np.unwrap(np.angle(z))
        t = np.arange(len(ph)) / SR
        w = np.abs(z)
        slope = np.polyfit(t, ph, 1, w=w)[0]
        ks.append(k)
        fs.append(k * f0 + slope / (2 * math.pi))
    return _fit_b(ks, fs)


def _fit_b(ks, fs):
    if len(ks) < 4:
        return None
    k = np.array(ks, float)
    y = (np.array(fs) / k) ** 2
    slope, icpt = np.polyfit(k * k, y, 1)
    return float(slope / icpt)


# ---------------------------------------------------------------- detune and width

def _win_moment(n, f0):
    """Spectral second moment (Hz^2) of the Hann window alone, within ±f0/2."""
    nfft = 1 << int(math.ceil(math.log2(n * 2)))
    p = np.abs(np.fft.fft(np.hanning(n), nfft)) ** 2
    f = np.fft.fftfreq(nfft, 1 / SR)
    m = np.abs(f) < f0 / 2
    return float((f[m] ** 2 * p[m]).sum() / p[m].sum())


def spread_freq(x, f0, strong):
    """Pitch spread (detune, chorus) in cents: RMS width of each harmonic line in the spectrum."""
    seg = x * np.hanning(len(x))
    nfft = 1 << int(math.ceil(math.log2(len(seg) * 2)))
    p = np.abs(np.fft.rfft(seg, nfft)) ** 2
    hz = SR / nfft
    w2 = _win_moment(len(x), f0)
    vals = []
    for k in strong:
        lo, hi = int((k - 0.5) * f0 / hz), int((k + 0.5) * f0 / hz)
        f = np.arange(lo, hi) * hz
        pk = p[lo:hi]
        mean = (f * pk).sum() / pk.sum()
        var = ((f - mean) ** 2 * pk).sum() / pk.sum() - w2
        vals.append(1731.2 * math.sqrt(max(var, 0.0)) / (k * f0))
    return float(np.median(vals)) if vals else None


def spread_time(x, f0, strong):
    """Same quantity by the Gabor bandwidth of each demodulated harmonic (time derivative)."""
    length = max(2, int(round(SR / f0)))
    w = np.hanning(len(x))
    dw = np.gradient(w) * SR
    w2 = float((dw * dw).sum() / (w * w).sum()) / (4 * math.pi ** 2)
    vals = []
    for k in strong:
        z = _hetero(x, k * f0, length) * w
        dz = np.gradient(z) * SR
        e = (np.abs(z) ** 2).sum()
        fm = float(np.imag((np.conj(z) * dz).sum()) / (2 * math.pi * e))
        var = float((np.abs(dz) ** 2).sum() / (4 * math.pi ** 2 * e)) - fm * fm - w2
        vals.append(1731.2 * math.sqrt(max(var, 0.0)) / (k * f0))
    return float(np.median(vals)) if vals else None


def width_time(st):
    """Stereo width: side against mid energy (dB), on the whole waveform."""
    m, s = st[:, 0] + st[:, 1], st[:, 0] - st[:, 1]
    return max(-60.0, _db(float((s * s).sum())) - _db(float((m * m).sum())))


def width_harm(st, f0):
    """Counter: side against mid on the harmonic lines only (demodulated), noise left out."""
    length = max(2, int(round(SR / f0)))
    ps = pm = 0.0
    for k in range(1, N_HARM + 1):
        if k * f0 > SR / 2 - f0:
            break
        zl = _hetero(st[:, 0], k * f0, length)
        zr = _hetero(st[:, 1], k * f0, length)
        ps += float(np.mean(np.abs(zl - zr) ** 2))
        pm += float(np.mean(np.abs(zl + zr) ** 2))
    return max(-60.0, _db(ps) - _db(pm))


# ---------------------------------------------------------------- vibrato

def _track_autocorr(x, f0):
    """f0 track in cents every 5 ms by local autocorrelation peak (path A)."""
    per = SR / f0
    win = int(3 * per) + 2
    hop = SR // 200
    lo, hi = int(per * 0.85), int(per * 1.15) + 2
    out = []
    for at in range(0, len(x) - win - hi, hop):
        a = x[at: at + win]
        r = np.array([np.dot(a, x[at + t: at + t + win]) for t in range(lo - 1, hi + 2)])
        i = 1 + int(np.argmax(r[1:-1]))
        den = r[i - 1] - 2 * r[i] + r[i + 1]
        off = 0.5 * (r[i - 1] - r[i + 1]) / den if den < 0 else 0.0
        out.append(1200 * math.log2((lo - 1 + i + off) / per))
    return np.array(out), 200.0


def _track_hetero(x, f0, k):
    """f0 track in cents from the phase of harmonic k demodulated in time (path B)."""
    length = max(2, int(round(SR / f0)))
    z = _hetero(x, k * f0, length)
    dph = np.angle(z[1:] * np.conj(z[:-1]))
    inst = k * f0 + dph * SR / (2 * math.pi)
    sm = _boxcar(inst, SR // 100, 2)[::SR // 200]
    return 1200 * np.log2(np.maximum(sm, 1.0) / (k * f0)), 200.0


def _detrend(tr):
    t = np.arange(len(tr))
    return tr - np.polyval(np.polyfit(t, tr, 2), t)


def vibrato_freq(x, f0):
    """Vibrato rate (FFT peak of the autocorrelation track, 2..12 Hz) and depth (cents)."""
    tr, fs = _track_autocorr(x, f0)
    tr = _detrend(tr[5:-5])
    depth = math.sqrt(2) * float(np.std(tr))
    if depth < 2.0:
        return None, None
    n = 1 << int(math.ceil(math.log2(len(tr) * 8)))
    sp = np.abs(np.fft.rfft(tr * np.hanning(len(tr)), n))
    f = np.fft.rfftfreq(n, 1 / fs)
    m = (f > 2) & (f < 12)
    i = int(np.argmax(sp * m))
    return float(f[i]), depth


def beats_not_vibrato(x, f0, strong):
    """True when the pitch wobble differs from one harmonic to the next: beats of detuned voices.

    A vibrato moves every harmonic by the same number of cents at the same moment; the beats of
    two detuned voices do not (their rate grows with the harmonic number). Correlation of the
    cents tracks of the two strongest harmonics under 0.6, or depths more than 1.5x apart."""
    ks = [k for k in strong if k <= 6][:6]
    if len(ks) < 2:
        return False
    tr = [_detrend(_track_hetero(x, f0, k)[0][10:-10]) for k in ks[:2]]
    sd = [float(np.std(t)) for t in tr]
    if min(sd) < 1e-9:
        return False
    corr = float(np.corrcoef(tr[0], tr[1])[0, 1])
    return corr < 0.6 or max(sd) / min(sd) > 1.5


def vibrato_time(x, f0, k):
    """Same from the demodulated phase track, rate counted by mean crossings."""
    tr, fs = _track_hetero(x, f0, k)
    tr = _detrend(tr[10:-10])
    depth = math.sqrt(2) * float(np.std(tr))
    if depth < 2.0:
        return None, None
    s = np.signbit(tr)
    idx = np.nonzero(s[1:] != s[:-1])[0]
    if len(idx) < 3:
        return None, depth
    rate = (len(idx) - 1) / 2 / ((idx[-1] - idx[0]) / fs)
    return float(rate), depth


# ---------------------------------------------------------------- envelope

def _times_from_env(env, t_off):
    """Attack 10-90 %, decay to the midway dB level, release to -40 dB after note-off (crossings)."""
    off = int(t_off * SR)
    pk = int(np.argmax(env[:off]))
    a10 = int(np.argmax(env[:pk + 1] >= 0.1 * env[pk]))
    # The attack ends where the rise slows to a tenth of its steepest pace (over 20 ms), not at
    # the loudest point of the hold: detuned voices beat, and a later beat can be louder.
    win = int(0.020 * SR)
    if pk - a10 > win:
        rise = env[a10 + win:pk + 1] - env[a10:pk + 1 - win]
        steep = int(np.argmax(rise))
        slow = np.nonzero(rise[steep:] < 0.1 * rise[steep])[0]
        if len(slow):
            pk = int(np.argmax(env[:a10 + steep + slow[0] + win + 1]))
    top = env[pk]
    a10 = int(np.argmax(env[:pk + 1] >= 0.1 * top))
    a90 = int(np.argmax(env[:pk + 1] >= 0.9 * top))
    attack = (a90 - a10) / SR
    lvl_off = float(np.mean(env[off - SR // 50: off]))
    mid = math.sqrt(top * lvl_off)
    decay = None
    if top / max(lvl_off, 1e-12) > 1.41:
        j = pk + int(np.argmax(env[pk:off] <= mid))
        decay = (j - pk) / SR
    tail = env[off:]
    below = np.nonzero(tail <= lvl_off * 0.01)[0]
    release = below[0] / SR if len(below) else None
    if lvl_off < 0.01 * top:
        release = None  # the note had already died out (pluck): no release to measure
    return attack, decay, release


def env_path_a(x, t_off, f0):
    win = max(int(0.005 * SR), int(2 * SR / f0))
    return _times_from_env(_env_rms(x, win), t_off)


def env_path_b(x, t_off, f0):
    """Hilbert envelope; release from the Schroeder backward integral (slope fit -5..-25 dB)."""
    env = _env_analytic(x, f0)
    attack, decay, rel = _times_from_env(env, t_off)
    off = int(t_off * SR)
    if rel is None and float(np.mean(env[off - SR // 50: off])) < 0.01 * float(env[:off].max()):
        return attack, decay, None
    tail = x[off:] ** 2
    sch = np.cumsum(tail[::-1])[::-1]
    sch_db = 10 * np.log10(np.maximum(sch / max(sch[0], 1e-30), 1e-30))
    m = (sch_db <= -5) & (sch_db >= -25)
    if m.sum() < 10:
        return attack, decay, None
    t = np.arange(len(tail))[m] / SR
    slope = np.polyfit(t, sch_db[m], 1)[0]
    return attack, decay, (-40.0 / slope if slope < 0 else None)


# ---------------------------------------------------------------- brightness and filter envelope

def bright_track_freq(x, f0, hop_s=0.005):
    """RMS frequency (harmonic units) per 1024-sample frame from the STFT."""
    n, hop = 1024, int(hop_s * SR)
    w = np.hanning(n)
    f = np.fft.rfftfreq(n, 1 / SR)
    out = []
    for at in range(0, len(x) - n, hop):
        p = np.abs(np.fft.rfft(x[at: at + n] * w)) ** 2
        out.append(math.sqrt((f * f * p).sum() / max(p.sum(), 1e-30)) / f0)
    return np.array(out), n / 2 / SR


def bright_track_time(x, f0, hop_s=0.005):
    """Same quantity in the time domain: RMS of the derivative over RMS of the signal (Hann frames)."""
    n, hop = 1024, int(hop_s * SR)
    w = np.hanning(n)
    # 5-point derivative, accurate to about 8 kHz at 48 kHz.
    d = np.zeros_like(x)
    d[2:-2] = (-x[4:] + 8 * x[3:-1] - 8 * x[1:-3] + x[:-4]) * SR / 12.0
    out = []
    for at in range(0, len(x) - n, hop):
        a = (x[at: at + n] * w) ** 2
        b = (d[at: at + n] * w) ** 2
        out.append(math.sqrt(b.sum() / max(a.sum(), 1e-30)) / (2 * math.pi) / f0)
    return np.array(out), n / 2 / SR


def filter_env(track, centre, onset_s, t_off):
    """Brightness at the attack (0-60 ms), held (last 300 ms before note-off), drop time."""
    t = np.arange(len(track)) * 0.005 + centre
    att = track[(t >= onset_s) & (t < onset_s + 0.06)]
    held = track[(t >= t_off - 0.3) & (t < t_off - 0.02)]
    if len(att) == 0 or len(held) == 0:
        return None, None, None
    a, h = float(att.mean()), float(np.median(held))
    first = track[(t >= onset_s) & (t < onset_s + 0.15)]
    top = float(first.max())
    drop = None
    if top / max(h, 1e-9) > 1.3:
        i0 = int(np.argmax((t >= onset_s) & (track >= top)))
        mid = (top + h) / 2
        rest = np.nonzero((track[i0:] <= mid) & (t[i0:] < t_off))[0]
        if len(rest):
            drop = float(t[i0 + rest[0]] - onset_s)
    return a, h, drop


def evolution(track, centre, t0, t1):
    """Slow evolution of a pad between t0 and t1: spread (%) and period (s) of the brightness."""
    t = np.arange(len(track)) * 0.005 + centre
    seg = track[(t >= t0) & (t < t1)]
    if len(seg) < 200:
        return None, None
    seg = _boxcar(seg, 10, 1)[10:-10]
    med = float(np.median(seg))
    spread = 100 * float(np.percentile(seg, 95) - np.percentile(seg, 5)) / med
    if spread < 3:
        return spread, None
    d = seg - seg.mean()
    ac = np.correlate(d, d, "full")[len(d) - 1:]
    lo, hi = int(0.4 / 0.005), min(len(ac) - 1, int(6.0 / 0.005))
    if hi <= lo + 2:
        return spread, None
    i = lo + int(np.argmax(ac[lo:hi]))
    if ac[i] < 0.3 * ac[0]:
        return spread, None
    return spread, i * 0.005


# ---------------------------------------------------------------- noise

def hnr_freq(x, f0, spread_c):
    """Harmonic-to-noise ratio (dB) from the spectrum: energy near k·f0 against the rest."""
    seg = x * np.hanning(len(x))
    nfft = 1 << int(math.ceil(math.log2(len(seg) * 2)))
    p = np.abs(np.fft.rfft(seg, nfft)) ** 2
    f = np.fft.rfftfreq(nfft, 1 / SR)
    m = (f > 0.5 * f0) & (f < 12000)
    k = np.round(f / f0)
    half = np.maximum(4 * SR / len(x), 3 * k * f0 * (2 ** (max(spread_c, 1.0) / 1200) - 1))
    harm = m & (np.abs(f - k * f0) <= half) & (k >= 1)
    noise = m & ~harm
    # The noise between the lines is extended over the line bands (same density).
    dens = p[noise].sum() / max(noise.sum(), 1)
    nh = p[harm].sum() - dens * harm.sum()
    return min(40.0, _db(max(nh, 1e-30)) - _db(dens * m.sum()))


def hnr_time(x, f0):
    """Boersma: normalised autocorrelation r at one period, HNR = 10 log10(r / (1 - r))."""
    seg = x * np.hanning(len(x))
    n = len(seg)
    nfft = 1 << int(math.ceil(math.log2(2 * n)))
    ac = np.fft.irfft(np.abs(np.fft.rfft(seg, nfft)) ** 2)
    aw = np.fft.irfft(np.abs(np.fft.rfft(np.hanning(n), nfft)) ** 2)
    per = SR / f0
    lo, hi = int(per * 0.95), int(per * 1.05) + 2
    r = ac[lo:hi] / ac[0] / (aw[lo:hi] / aw[0])
    rmax = float(min(r.max(), 0.99999))
    if rmax <= 0:
        return 0.0
    return min(40.0, 10 * math.log10(rmax / (1 - rmax)))


# ---------------------------------------------------------------- whole fingerprint

def fingerprint(st, t_off, pad=False):
    """All measures, each as (measure, counter, agree). Returns a dict and a status line."""
    st = np.asarray(st, float)
    mono = st.mean(axis=1)
    out = {}
    env = _env_rms(mono, int(0.005 * SR))
    off = int(t_off * SR)
    top = env[:off].max()
    if top < 1e-5:
        return {}, "silence"
    onset = int(np.argmax(env >= 0.1 * top)) / SR
    a90 = int(np.argmax(env[:off] >= 0.9 * top)) / SR
    s0 = min(max(onset + 0.25, a90 + 0.05), t_off - 0.45)
    steady = mono[int(s0 * SR): int((t_off - 0.05) * SR)]
    sst = st[int(s0 * SR): int((t_off - 0.05) * SR)]

    fa, fb = note_time(steady), note_freq(steady)
    out["note_hz"] = (fa, fb, agree("note_hz", fa, fb))
    status = "une note"
    if not out["note_hz"][2] or fa is None:
        status = "pas de note sûre (plusieurs notes, bruit ou percussion)"
    f0 = fb if fb is not None else (fa or 100.0)
    pa, pb = amps_freq(steady, f0), amps_time(steady, f0)
    harm_frac = None
    if status == "une note":
        seg = steady * np.hanning(len(steady))
        nfft = 1 << int(math.ceil(math.log2(len(seg) * 2)))
        p = np.abs(np.fft.rfft(seg, nfft)) ** 2
        f = np.fft.rfftfreq(nfft, 1 / SR)
        k = np.round(f / f0)
        m = (f > 0.5 * f0) & (f < 8.5 * f0)
        harm_frac = float(p[m & (np.abs(f - k * f0) < f0 / 4)].sum() / max(p[m].sum(), 1e-30))
        if harm_frac < 0.6:
            status = "pas de note sûre (plusieurs notes, bruit ou percussion)"
    note_ok = status == "une note"
    strong = [k for k in range(1, 13) if pa[k - 1] > 1e-5 and k * f0 < 10000]

    if note_ok:
        out["pente_db_oct"] = _pair("pente_db_oct", slope_from_amps(pa), slope_from_amps(pb))
        out["pair_impair_db"] = _pair("pair_impair_db", even_odd_from_amps(pa), even_odd_from_amps(pb))
        kv = strong[int(np.argmax([pa[k - 1] for k in strong]))] if strong else 1
        va = vibrato_freq(steady, f0)
        vb = vibrato_time(steady, f0, kv)
        by_level = sorted(strong, key=lambda k: -pa[k - 1])
        if (va[1] or vb[1]) and beats_not_vibrato(steady, f0, by_level):
            for key in ("vibrato_hz", "vibrato_cents"):
                out[key] = (None, None, True, "hors mesure (battements du désaccord, pas un vibrato)")
            vib = 0.0
        else:
            out["vibrato_hz"] = _pair("vibrato_hz", va[0], vb[0])
            out["vibrato_cents"] = _pair("vibrato_cents", va[1], vb[1])
            vib = max(va[1] or 0.0, vb[1] or 0.0)
        low = [k for k in strong if 2 <= k <= 8] or strong[:1]
        sa, sb = spread_freq(steady, f0, low), spread_time(steady, f0, low)
        out["desaccord_cents"] = _pair("desaccord_cents", sa, sb)
        if vib >= 3.0:
            out["desaccord_cents"] += ("contient le vibrato",)
        if vib < 3.0 and max(sa or 0, sb or 0) < 3.0:
            out["inharm_b"] = _pair("inharm_b", inharm_freq(steady, f0, strong), inharm_time(steady, f0, strong))
        else:
            out["inharm_b"] = (None, None, True, "hors mesure (désaccord ou vibrato)")
        sp = max(sa or 0.0, sb or 0.0)
        out["souffle_hnr_db"] = _pair("souffle_hnr_db", hnr_freq(steady, f0, sp), hnr_time(steady, f0))
        if sp >= 3.0 or vib >= 3.0:
            out["souffle_hnr_db"] += ("désaccord ou vibrato compté comme souffle par le contre-chemin",)
        out["largeur_sm_db"] = _pair("largeur_sm_db", width_time(sst), width_harm(sst, f0))
    else:
        out["largeur_sm_db"] = (width_time(sst), None, False, "contre-mesure hors outil (accord)")

    ref = f0 if note_ok else 100.0
    ta = env_path_a(mono, t_off, ref)
    tb = env_path_b(mono, t_off, ref)
    for i, key in enumerate(ENV_KEYS):
        out[key] = (ta[i], tb[i], agree(key, ta[i], tb[i], 1.0 / ref))
    tra, ca = bright_track_freq(mono, ref)
    trb, cb = bright_track_time(mono, ref)
    fa_ = filter_env(tra, ca, onset, t_off)
    fb_ = filter_env(trb, cb, onset, t_off)
    for i, key in enumerate(("brillance_attaque", "brillance_tenue", "chute_filtre_s")):
        out[key] = _pair(key, fa_[i], fb_[i])
    if not note_ok:
        for key in ("brillance_attaque", "brillance_tenue"):
            out[key] += ("unité : centaines de Hz (pas de note)",)
    if pad:
        t0, t1 = 1.0, min(t_off - 0.05, 8.0)
        ea = evolution(tra, ca, t0, t1)
        eb = evolution(trb, cb, t0, t1)
        out["evolution_pct"] = _pair("evolution_pct", ea[0], eb[0])
        out["evolution_periode_s"] = _pair("evolution_periode_s", ea[1], eb[1])
    out["_harm_frac"] = harm_frac
    return out, status


def _pair(key, a, b):
    return (a, b, agree(key, a, b))


def note_name(hz):
    if not hz:
        return "?"
    m = 69 + 12 * math.log2(hz / 440.0)
    names = ["do", "do#", "ré", "ré#", "mi", "fa", "fa#", "sol", "sol#", "la", "la#", "si"]
    r = int(round(m))
    return f"{names[r % 12]}{r // 12 - 2} ({100 * (m - r):+.0f} c)"  # French octaves: la3 = 440 Hz


def fmt(v):
    if v is None:
        return "-"
    if abs(v) < 0.01 and v != 0:
        return f"{v:.2e}"
    return f"{v:.3g}"
