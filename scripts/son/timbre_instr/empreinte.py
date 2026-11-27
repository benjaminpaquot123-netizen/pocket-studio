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

    For decay and release the floor is at least one period of the note (they last hundreds of ms).
    The attack keeps its fixed tolerance whatever the note: low notes get no extra slack."""
    rel, floor = TOL[key]
    if key == "attaque_s" and period > 0 and (a is None) != (b is None):
        # None = "shorter than one period": an interval [0, period]. The other path agrees when
        # it lies within the same fixed tolerance of that interval (the tolerance does not grow).
        v = a if b is None else b
        return v - period <= max(floor, rel * v)
    if key == "chute_s" and {a, b} == {None, 0.0}:
        # A decay shorter than the smoothing also gets smoothed under the 3 dB gate: below the
        # resolution, "too short" (0.0) and "none" (None) are the same reading.
        return True
    if key == "chute_s" and period > 0 and (a == 0.0) != (b == 0.0) and None not in (a, b):
        # 0.0 = "shorter than the smoothing" (at most path A's window, max(5 ms, two periods)): the other
        # path agrees when it lies within the same tolerance of that interval.
        v = a if b == 0.0 else b
        return v - max(0.005, 2 * period) <= max(floor, rel * v)
    if key == "relache_s" and period > 0 and (a == 0.0) != (b == 0.0) and None not in (a, b):
        # 0.0 = "over before the smoothing lets go" (at most four periods + 5 ms after note-off).
        v = a if b == 0.0 else b
        return v - (4 * period + 0.005) <= max(floor, rel * v)
    if a is None or b is None:
        return a is None and b is None
    if key in ENV_KEYS and key != "attaque_s":
        floor = max(floor, period)
    return abs(a - b) <= max(floor, rel * max(abs(a), abs(b)))


# ---------------------------------------------------------------- helpers

def _db(p):
    return 10.0 * math.log10(max(p, 1e-30))


def _boxcar(z, length, times=2):
    """Moving average of `length` samples applied `times` times (zeros at multiples of SR/length)."""
    n = len(z)
    hi = np.minimum(np.arange(n) + (length - 1) // 2 + 1, n)
    lo = np.maximum(np.arange(n) + (length - 1) // 2 - (length - 1), 0)
    for _ in range(times):
        c = np.concatenate(([0], np.cumsum(z)))
        z = (c[hi] - c[lo]) / length  # same as np.convolve(z, ones(length)/length, "same"), O(n)
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
    """Analytic-signal magnitude smoothed over two periods (path B, no squaring): nulls at every
    multiple of f0 / 2, so a sub-oscillator one octave down does not ripple the envelope."""
    n = len(x)
    spec = np.fft.fft(x)
    h = np.zeros(n)
    h[0] = 1.0
    h[1:(n + 1) // 2] = 2.0
    if n % 2 == 0:
        h[n // 2] = 1.0
    mag = np.abs(np.fft.ifft(spec * h))
    return _boxcar(mag, max(int(0.004 * SR), int(round(2 * SR / f0))), 1)


# ---------------------------------------------------------------- note (two paths)

def note_time(x):
    """f0 by the cumulative-mean-normalised difference (YIN) on the waveform. One second: longer
    than the beat of detuned voices, so the period found is their average, not one of them."""
    seg = x[: min(len(x), int(1.0 * SR))]
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
            # Octave check: a weak sub-oscillator makes the true period twice the heard one.
            # Correlation at half the period r = (P_even - P_odd) / P; r > 0.6 means the odd
            # harmonics carry less than a quarter of the even ones (same rule as note_freq).
            h = t // 2
            if h - 2 > min_lag:
                lo, hi = int(0.95 * h), int(1.05 * h) + 2
                j = lo + int(np.argmin(d[lo:hi]))
                if 1.0 - d[j] / max(w0 + wt[j], 1e-30) > 0.6:
                    t, cm = j, d
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
    # Octave check (same rule as note_time): odd harmonics under a quarter of the even ones
    # = a weak sub-oscillator, the heard note is one octave up.
    # Energy within +-f0/4 of each multiple, as the time path's correlation sees it: a detuned
    # main oscillator does not sit exactly on twice the sub's harmonics and two close voices
    # share their power between peaks, so a fixed band or a single peak misses part of it.
    def band(fr):
        lo, hi = max(1, int((fr - 0.25 * best_f) / hz)), int((fr + 0.25 * best_f) / hz) + 1
        return float((mag[lo:hi] ** 2).sum()) if hi < len(mag) else 0.0
    p_odd = sum(band(k * best_f) for k in (1, 3, 5, 7))
    p_even = sum(band(k * best_f) for k in (2, 4, 6, 8))
    if p_odd < 0.25 * p_even:
        best_f *= 2
    num = den = 0.0
    for k in range(1, 11):
        c = int(round(k * best_f / hz))
        lo, hi = max(1, int(c - 0.25 * best_f / hz)), int(c + 0.25 * best_f / hz) + 1
        if hi >= len(mag) - 1:
            break
        p = lo + int(np.argmax(mag[lo:hi]))
        # Power centroid over +-f0/8 around the peak: the exact peak for one voice (symmetric
        # window), the power-weighted centre for detuned voices, which is the pitch heard.
        w8 = max(2, int(0.125 * best_f / hz))
        q0, q1 = max(1, p - w8), min(len(mag) - 1, p + w8 + 1)
        pw = mag[q0:q1] ** 2
        w = float(pw.sum())
        centre = float((np.arange(q0, q1) * pw).sum()) / max(w, 1e-30)
        num += w * k * centre * hz
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

def _times_from_env(env, t_off, skip=0):
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
    # Decay = time constant of an exponential to a sustain, s + a * exp(-t / tau), fitted by least
    # squares from just after the attack (skip = the envelope's own smoothing, so the rounded top
    # does not count) to note-off. A fit averages out the ripple of beating voices, does not
    # depend on the exact top value (which the smoothing lowers differently on each path) and its
    # truth is the tau of the envelope itself. No decay when the top is under 3 dB above the
    # sustain, when the fit finds no falling exponential, or when tau sits on the grid's edge.
    decay = None
    lvl_s = float(np.mean(env[max(pk, off - SR // 5): off]))
    if top / max(lvl_s, 1e-12) > 1.41:
        i0 = min(pk + skip, off - 1)
        if (env[i0] - lvl_s) / (top - lvl_s) < math.exp(-1):
            # More than one tau already went by inside the smoothing: the decay is real but
            # shorter than this path can resolve. 0.0 = "shorter than the smoothing" (see agree).
            decay = 0.0
        else:
            decay = _decay_fit(env, i0, off)
    tail = env[off:]
    below = np.nonzero(tail <= lvl_off * 0.01)[0]
    release = below[0] / SR if len(below) else None
    if lvl_off < 0.01 * top:
        release = None  # the note had already died out (pluck): no release to measure
    return attack, decay, release


def _decay_fit(env, i0, off):
    """tau of env ~ s + a * exp(-(t - t0) / tau) over [i0, off), a > 0; grid on tau, s and a by
    linear least squares. None when nothing falls or tau is at an edge of the grid."""
    dec = max(1, (off - i0) // 2000)
    e = env[i0:off:dec]
    if len(e) < 20:
        return None
    t = np.arange(len(e)) * dec / SR
    taus = np.geomspace(0.003, 10.0, 300)
    flat = float(((e - e.mean()) ** 2).sum())
    best = (math.inf, None, 0.0)
    for k, tau in enumerate(taus):
        g = np.exp(-t / tau)
        A = np.stack((np.ones_like(t), g), 1)
        coef, *_ = np.linalg.lstsq(A, e, rcond=None)
        if coef[0] < 0:
            # The sustain cannot be negative: a long, almost straight fall would otherwise trade a
            # negative sustain against tau freely. Refit with s = 0 (a plain exponential).
            coef = np.array([0.0, float(g @ e) / float(g @ g)])
        r = float(((A @ coef - e) ** 2).sum())
        if r < best[0]:
            best = (r, k, float(coef[1]))
    r, k, a = best
    if k is None or a <= 0 or k in (0, len(taus) - 1):
        return None
    if r > 0.5 * flat:
        return None  # the exponential explains under half of the motion: a wave, not a decay
    # A tau longer than the stretch it is fitted on is not measurable (the fall looks straight
    # and any long tau fits): report it as that stretch, "at least as slow as the held note".
    return float(min(taus[k], (off - i0) / SR))


def _rise_peak(env, t_off):
    """Index where the attack ends (same rule as _times_from_env)."""
    off = int(t_off * SR)
    pk = int(np.argmax(env[:off]))
    a10 = int(np.argmax(env[:pk + 1] >= 0.1 * env[pk]))
    win = int(0.020 * SR)
    if pk - a10 > win:
        rise = env[a10 + win:pk + 1] - env[a10:pk + 1 - win]
        steep = int(np.argmax(rise))
        slow = np.nonzero(rise[steep:] < 0.1 * rise[steep])[0]
        if len(slow):
            pk = int(np.argmax(env[:a10 + steep + slow[0] + win + 1]))
    return pk


RAMP_10_90 = 0.8 * math.sqrt(12.0)  # 10-90 % time of a straight ramp, in standard deviations


def attack_moments(env, t_off, kernel_var):
    """Attack time with the smoothing taken out (works on low notes).

    The rise of an envelope is a positive pulse in its derivative; smoothing the envelope by a
    kernel convolves that pulse, so their variances add. The kernel's own variance (known) is
    removed, whatever the note's period. The width is given as the 10-90 % time of a straight
    ramp of the same spread."""
    pk = _rise_peak(env, t_off)
    top = float(env[pk])
    if top <= 0:
        return None
    i0 = int(np.argmax(env[:pk + 1] > 1e-3 * top))
    d = np.diff(env[: pk + 1 + int(0.2 * SR)])
    steep = i0 + int(np.argmax(d[i0:pk + 1]))
    stop = np.nonzero(d[steep:] <= 0.0)[0]
    i1 = steep + (int(stop[0]) if len(stop) else len(d) - steep)
    seg = np.maximum(d[i0:i1], 0.0)
    if seg.sum() <= 0:
        return None
    t = np.arange(len(seg)) / SR
    mean = float((t * seg).sum() / seg.sum())
    var = float(((t - mean) ** 2 * seg).sum() / seg.sum()) - kernel_var
    return RAMP_10_90 * math.sqrt(max(var, 0.0))


PAD = int(0.1 * SR)  # silence put before the note so the smoothing does not cut the rise


def _box_var(length, times):
    return times * (length * length - 1) / 12.0 / (SR * SR)


def attack_gauss(x, t_off, f0):
    """Counter for the attack: each harmonic cut out in the frequency domain by a Gaussian band
    (FFT, analytic), magnitudes summed; a Gaussian kernel has a known variance and, unlike a box,
    lets nothing of the neighbour harmonics through while the note switches on."""
    xp = np.concatenate((np.zeros(PAD), x, np.zeros(PAD)))
    n = 1 << int(math.ceil(math.log2(len(xp))))
    spec = np.fft.fft(xp, n)
    f = np.fft.fftfreq(n, 1 / SR)
    sf = f0 / GAUSS_BANDS
    env = np.zeros(n)
    for k in _attack_partials(f0):
        env += np.abs(np.fft.ifft(spec * np.exp(-0.5 * ((f - k * f0) / sf) ** 2))) ** 2
    env = np.sqrt(env)
    st = 1.0 / (2 * math.pi * sf)
    return attack_fit(env[: len(xp)], t_off + PAD / SR, Kernel(sigma=st))


def _erf(u):
    """Abramowitz-Stegun 7.1.26 (error below 1.5e-7), numpy has no erf."""
    s = np.sign(u)
    a = np.abs(u)
    t = 1.0 / (1.0 + 0.3275911 * a)
    y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t
               + 0.254829592) * t * np.exp(-a * a)
    return s * y


def _ramp_gauss(u, sigma):
    """Integral of a unit step smoothed by a Gaussian of std sigma, evaluated at u."""
    z = u / (sigma * math.sqrt(2.0))
    return u * 0.5 * (1.0 + _erf(z)) + sigma / math.sqrt(2 * math.pi) * np.exp(-z * z)


class Kernel:
    """Smoothing kernel of an envelope path, known exactly: a Gaussian (std sigma, closed form) or
    the sampled kernel of the boxes (its smoothed ramp tabulated once, then interpolated)."""

    def __init__(self, sigma=None, taps=None):
        self.sigma = sigma
        if taps is not None:
            taps = np.asarray(taps, float) / float(np.sum(taps))
            half = len(taps) // 2
            u = np.arange(-2 * half - 1, 2 * half + 2) / SR
            ramp = np.convolve(np.maximum(u, 0.0), taps[::-1], mode="same")
            self.u, self.table, self.half = u, ramp, (half + 1) / SR

    @property
    def reach(self):
        return 4 * self.sigma if self.sigma is not None else self.half

    def ramp(self, u):
        """A unit-slope ramp starting at 0, smoothed by the kernel, evaluated at u."""
        if self.sigma is not None:
            return _ramp_gauss(u, self.sigma)
        return np.where(u >= self.half, u, np.interp(u, self.u, self.table, left=0.0))


def box_kernel(length, times):
    """Taps of `times` moving averages of `length` samples (what _boxcar applies)."""
    k = np.ones(1)
    for _ in range(times):
        k = np.convolve(k, np.ones(length) / length)
    return k


def _fit_rows(e, t, t0s, T, kern):
    """Residual of the best ramp + drift model for each start in t0s (ramp length T).

    Model: g * ramp(t0, T) + h * drift, both smoothed by the Gaussian kernel; drift is a straight
    line that starts where the ramp ends (the slow decay, or detuned voices still beating up).
    The drift slope is kept under a quarter of the ramp slope, otherwise it is dropped (h = 0):
    a slow attack must not be read as a short ramp plus a rising line."""
    u = t[None, :] - t0s[:, None]
    m = (kern.ramp(u) - kern.ramp(u - T)) / T
    d = kern.ramp(u - T)
    mm, dd, md = (m * m).sum(1), (d * d).sum(1), (m * d).sum(1)
    me, de = m @ e, d @ e
    det = np.maximum(mm * dd - md * md, 1e-30)
    g = (me * dd - de * md) / det
    h = (de * mm - me * md) / det
    bad = (np.abs(h) > 0.25 * np.abs(g) / T) | (dd <= 1e-30)
    g0 = me / np.maximum(mm, 1e-30)
    g = np.where(bad, g0, g)
    h = np.where(bad, 0.0, h)
    return ((e[None, :] - g[:, None] * m - h[:, None] * d) ** 2).sum(1)


def attack_fit(env, t_off, kern):
    """Attack by fitting a straight ramp (start t0, length T) smoothed by the known Gaussian kernel
    to the rise of the envelope, least squares (no moments, no derivative). A slow straight drift
    after the ramp is part of the model, so a decay or a beat right after the attack does not bend
    the reading. Coarse grid on T, then a fine grid around the best (steps of 0.25 %)."""
    pk = _rise_peak(env, t_off)
    top = float(env[pk])
    if top <= 0:
        return None
    i0 = int(np.argmax(env[:pk + 1] > 0.02 * top))
    lo = max(0, i0 - int(kern.reach * SR))
    # 2 kHz is plenty (the envelope is smoothed by sigma >= a few ms); long pad rises are
    # thinned further so the grid stays bounded (at most ~1500 points, 0.1 % of the span).
    dec = max(24, (pk - lo) // 1500)
    e = env[lo:pk + 1:dec]
    t = (lo + np.arange(len(e)) * dec) / SR

    def best_for(T):
        t0s = np.arange(t[0] - T, t[-1], max(0.00025, T / 80, (t[-1] - t[0]) / 1500))
        rows = max(1, 1_000_000 // len(e))  # bounded memory on long pad rises
        out = math.inf
        for c in range(0, len(t0s), rows):
            out = min(out, float(_fit_rows(e, t, t0s[c:c + rows], T, kern).min()))
        return out

    coarse = np.geomspace(0.0003, 0.6, 160)
    res = [best_for(T) for T in coarse]
    j = int(np.argmin(res))
    fine = np.geomspace(coarse[max(j - 1, 0)], coarse[min(j + 1, len(coarse) - 1)], 41)
    resf = [best_for(T) for T in fine]
    return 0.8 * float(fine[int(np.argmin(resf))])


ATT_FMAX = 600.0  # Hz: partials used for the attack (see _attack_partials)


def _attack_partials(f0):
    """Partials summed for the attack: up to 12, but only those under ATT_FMAX (at least two).
    Detuned voices beat at a rate proportional to the partial's frequency in Hz (12 cents at
    600 Hz: 4 Hz); above that the beat turns within the 30 ms of a rise and bends it."""
    return [k for k in range(1, 13) if k <= 2 or k * f0 <= ATT_FMAX]


GAUSS_BANDS = 5.0  # Gaussian band standard deviation = f0 / 5 (neighbour at 5 sigma)


def resolved_attack(att, f0):
    """An attack shorter than one period of the note has no envelope to measure (the first cycle
    is the whole rise and its shape depends on the phases): reported as None, "below one period",
    never as a number. The tolerance itself does not depend on the period."""
    if att is None or att < 1.0 / f0:
        return None
    return att


REL_FMAX = 8000.0  # Hz: partials used for the release
REL_KMAX = 40


def _release_partials(x, t_off, f0):
    """Partials used for the release: up to REL_KMAX under REL_FMAX, and only those whose level
    at the end of the hold is within 40 dB of the strongest (per-partial rms over 100 ms)."""
    off = int(t_off * SR)
    seg = x[max(0, off - SR // 10): off]
    t = np.arange(len(seg)) / SR
    ks = [k for k in range(1, REL_KMAX + 1) if k == 1 or k * f0 <= REL_FMAX]
    lv = np.array([abs(np.mean(seg * np.exp(-2j * math.pi * k * f0 * t))) for k in ks])
    return [k for k, v in zip(ks, lv) if v >= 0.01 * lv.max()]


REL_FS = 400  # Hz: rate of the demodulated partials for the release fit (beats stay below 200 Hz)


def _order(z, cap=6):
    """Number of damped components in z: singular values of its Hankel matrix over 1e-3 of the
    largest (a pure exponential gives one, two detuned voices two)."""
    n = len(z)
    L = n // 3
    Y = np.lib.stride_tricks.sliding_window_view(z, L + 1)
    S = np.linalg.svd(Y, compute_uv=False)
    return Y, int(min(cap, max(1, np.sum(S > 1e-3 * S[0]))))


def _poles_pencil(z):
    """Matrix pencil (Hua-Sarkar): poles from the shift between the two halves of the signal
    subspace."""
    Y, m = _order(z)
    _, _, vh = np.linalg.svd(Y, full_matrices=False)
    v = vh[:m].conj().T
    return np.linalg.eigvals(np.linalg.pinv(v[:-1]) @ v[1:]), m


def _poles_lp(z):
    """Forward linear prediction (Prony), same order: poles = roots of the predictor (forward
    only: the backward pass would mirror decaying poles into growing ones)."""
    _, m = _order(z)
    zf = np.lib.stride_tricks.sliding_window_view(z, m + 1)
    a, *_ = np.linalg.lstsq(zf[:, :m][:, ::-1], zf[:, m], rcond=None)
    return np.roots(np.concatenate(([1.0], -a))), m


def _partial_tau(z, poles_fn):
    """tau of one demodulated partial after note-off: z = sum of damped exponentials, poles
    from poles_fn; decay rate = energy-weighted mean of the decaying components."""
    poles, m = poles_fn(z)
    # A pole losing more than half its level per sample (tau under 5 ms at REL_FS) is faster
    # than the decimated path resolves: it fits the smoothing kernel or the cut, not the voice.
    poles = poles[np.isfinite(poles) & (np.abs(poles) > math.exp(-0.5))]
    if len(poles) == 0:
        return None
    n = np.arange(len(z))
    V = poles[None, :] ** n[:, None]
    c, *_ = np.linalg.lstsq(V, z, rcond=None)
    e = np.abs(c) ** 2 * np.minimum(np.abs(V) ** 2, 1e6).sum(0)
    keep = np.argsort(-e)[:m]
    poles, e = poles[keep], e[keep]
    rate = -np.log(np.abs(poles)) * REL_FS
    if np.sum(e[rate <= 0]) > 0.1 * np.sum(e):
        return None  # a growing or steady part carries the energy: no release here
    rate, e = rate[rate > 0], e[rate > 0]
    return float(np.sum(e) / np.sum(e * rate))


def _release_from_partials(zs, skip, tail_s, poles_fn):
    """Release = time to -40 dB = tau * ln(100), tau the median over partials. Each partial is a
    sum of damped exponentials (one per voice, all with the voices' tau): fitting the poles
    takes the beats in, where a fit of the envelope rose or dipped with the beat phase at
    note-off (the -40 dB crossing moved from 0.39 to 0.82 s for one sound). None when the
    release outlasts the recorded tail."""
    dec = SR // REL_FS
    # Already under -40 dB when the smoothing lets go (typical partial): the release is shorter
    # than the path can see. Reported as 0.0, "below the resolution", never as a number (a fit
    # there reads the kernel's own tail).
    gone = [abs(z[min(skip, len(z) - 1)]) < 0.01 * np.abs(z[:max(1, skip // 8)]).max() for z in zs]
    if zs and np.mean(gone) > 0.5:
        return 0.0
    taus = []
    for z in zs:
        z = z[skip::dec]
        lv = np.abs(z)
        # fit until -60 dB of the level at the start, at most 1.5 s
        low = np.nonzero(lv < 1e-3 * lv[:max(1, REL_FS // 50)].max())[0]
        end = min(len(z), int(1.5 * REL_FS), low[0] if len(low) else len(z))
        if end < 12:
            continue
        tau = _partial_tau(z[:end], poles_fn)
        if tau is not None:
            taus.append(tau)
    if not taus:
        return None
    rel = float(np.median(taus)) * math.log(100.0)
    return rel if rel <= tail_s else None


def _died_out(env, t_off):
    off = int(t_off * SR)
    return float(np.mean(env[off - SR // 50: off])) < 0.01 * float(env[:off].max())


def env_path_a(x, t_off, f0, harmonic=True):
    """Sliding RMS for decay; release from the demodulated partials (median of per-partial fits);
    attack from the demodulated harmonics (no squaring)."""
    win = max(int(0.005 * SR), int(2 * SR / f0))
    rms = _env_rms(x, win)
    _, decay, release = _times_from_env(rms, t_off, win)
    length = max(2, int(round(2 * SR / f0)))
    off = int(t_off * SR)
    if harmonic and not _died_out(rms, t_off):
        pre = 4 * length
        xs = x[off - pre:]
        zs = [_hetero(xs, k * f0, length)[pre:] for k in _release_partials(x, t_off, f0)]
        release = _release_from_partials(zs, 2 * length + int(0.005 * SR), (len(x) - off) / SR,
                                         _poles_pencil)
    # Boxes of TWO periods: nulls at every multiple of f0 / 2, so a sub-oscillator one octave
    # down (its odd harmonics sit half-way between ours) does not leak in and ripple the envelope.
    xp = np.concatenate((np.zeros(PAD), x[: int(t_off * SR)]))
    env = np.zeros(len(xp))
    for k in _attack_partials(f0):
        env += np.abs(_hetero(xp, k * f0, length)) ** 2
    env = np.sqrt(env)
    # Same model as path B (ramp smoothed by the known kernel, least-squares fit), its own
    # envelope: the two boxes of _hetero, a triangle, taken exactly (not as a Gaussian).
    att = attack_fit(env, t_off + PAD / SR, Kernel(taps=box_kernel(length, 2)))
    return resolved_attack(att, f0), decay, release


def env_path_b(x, t_off, f0, harmonic=True):
    """Hilbert envelope for decay; release from each partial cut out by a Gaussian band (FFT),
    median of per-partial fits; without a note, from the Schroeder backward integral."""
    env = _env_analytic(x, f0)
    _, decay, rel = _times_from_env(env, t_off, max(int(0.004 * SR), int(round(2 * SR / f0))))
    attack = resolved_attack(attack_gauss(x, t_off, f0), f0)
    off = int(t_off * SR)
    if rel is None and _died_out(env, t_off):
        return attack, decay, None
    if harmonic:
        pre = int(round(8 * SR / f0))
        xs = x[off - pre:]
        n = 1 << int(math.ceil(math.log2(len(xs) + PAD)))
        spec = np.fft.fft(xs, n)
        f = np.fft.fftfreq(n, 1 / SR)
        sf = f0 / GAUSS_BANDS
        zs = [np.fft.ifft(spec * np.exp(-0.5 * ((f - k * f0) / sf) ** 2))[pre: len(xs)]
              for k in _release_partials(x, t_off, f0)]
        skip = int(4 / (2 * math.pi * sf) * SR) + int(0.005 * SR)
        return attack, decay, _release_from_partials(zs, skip, (len(x) - off) / SR, _poles_lp)
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

def bright_frame(f0):
    """Frame length: 1024 samples, or more so that a frame holds at least 2.5 periods (basses:
    a shorter Hann frame leaks the fundamental towards 0 Hz and lowers the brightness)."""
    return max(1024, 2 * int(1.25 * SR / f0))


def bright_track_freq(x, f0, hop_s=0.005, fmax=None):
    """RMS frequency (harmonic units) per frame (bright_frame) from the STFT, up to fmax."""
    n, hop = bright_frame(f0), int(hop_s * SR)
    w = np.hanning(n)
    f = np.fft.rfftfreq(n, 1 / SR)
    keep = f < (fmax or SR)
    f = f[keep]
    out = []
    for at in range(0, len(x) - n, hop):
        p = np.abs(np.fft.rfft(x[at: at + n] * w))[keep] ** 2
        out.append(math.sqrt((f * f * p).sum() / max(p.sum(), 1e-30)) / f0)
    return np.array(out), n / 2 / SR


def bright_track_time(x, f0, hop_s=0.005):
    """Same quantity in the time domain: RMS of the derivative over RMS of the signal (Hann frames)."""
    n, hop = bright_frame(f0), int(hop_s * SR)
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


def bright_track_harm(x, f0, hop_s=0.005):
    """Independent counter: each harmonic k demodulated in the time domain (heterodyne, no FFT,
    no derivative), RMS harmonic number sqrt(sum k^2 |a_k|^2 / sum |a_k|^2) per Hann frame
    (bright_frame). Only the harmonics count (noise between them does not), up to 16 kHz."""
    n, hop = bright_frame(f0), int(hop_s * SR)
    w2 = np.hanning(n) ** 2
    length = max(2, int(round(SR / f0)))
    p0 = np.zeros(len(x))
    p2 = np.zeros(len(x))
    for k in range(1, 401):
        if k * f0 >= 16000:
            break
        pk = np.abs(_hetero(x, k * f0, length)) ** 2
        p0 += pk
        p2 += k * k * pk
    out = []
    for at in range(0, len(x) - n, hop):
        a = float((p0[at: at + n] * w2).sum())
        b = float((p2[at: at + n] * w2).sum())
        out.append(math.sqrt(b / max(a, 1e-30)))
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
        # Breath (HNR) is left out of the fingerprint: on our synth its two paths agreed on 4/11
        # sounds only (detune, unison and chorus read as noise by one path, not the other).
        out["largeur_sm_db"] = _pair("largeur_sm_db", width_time(sst), width_harm(sst, f0))
    else:
        out["largeur_sm_db"] = (width_time(sst), None, False, "contre-mesure hors outil (accord)")

    ref = f0 if note_ok else 100.0
    ta = env_path_a(mono, t_off, ref, note_ok)
    tb = env_path_b(mono, t_off, ref, note_ok)
    for i, key in enumerate(ENV_KEYS):
        out[key] = (ta[i], tb[i], agree(key, ta[i], tb[i], 1.0 / ref))
    tra, ca = bright_track_freq(mono, ref, fmax=16000 if note_ok else None)
    trb, cb = bright_track_harm(mono, ref) if note_ok else bright_track_time(mono, ref)
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
