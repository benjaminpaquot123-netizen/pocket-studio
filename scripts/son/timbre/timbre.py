"""Timbre measures and small tools for the timbre study (docs/sons/fin/RECHERCHE-TIMBRE.md).

Bench only, numpy only. Nothing here reads a bought sample: the study runs on the
engine's own renders (render_voice.cpp) and on sounds synthesized below.

Every measure of the "fingerprint" has a counter-measure that takes another road
(time domain against FFT), so the two can be compared sound by sound, the way the
drum rule already asks for its lengths.
"""
import math

import numpy as np

SR = 48000
F_LO, F_HI = 20.0, 20000.0
ONSET_DB = -30.0
FLOOR_DB = -50.0  # a band this far under the whole sound is "empty": its value is clamped
OCT_EDGES = (20.0, 100.0, 200.0, 400.0, 800.0, 1600.0, 3200.0, 6400.0, 12800.0, 20000.0)
BARK_EDGES = (20, 100, 200, 300, 400, 510, 630, 770, 920, 1080, 1270, 1480, 1720, 2000, 2320,
              2700, 3150, 3700, 4400, 5300, 6400, 7700, 9500, 12000, 15500, 20000)
GROUPS = ((20.0, 200.0), (200.0, 2000.0), (2000.0, 20000.0))  # low, mid, high
MAP_MS = (0, 5, 10, 20, 40, 80, 160, 320, 640, 1000)

ENV_KEYS = tuple("env%d_db" % i for i in range(len(OCT_EDGES) - 1))
KEYS = ("t20_ms", "t60_ms", "centroid_hz", "hf_db", "transient_body_db", "centroid_att_hz",
        "centroid_tail_hz", "t20_low_ms", "t20_mid_ms", "t20_high_ms") + ENV_KEYS
LENGTH_KEYS = ("t20_ms", "t60_ms")


def _db(v):
    return 10.0 * math.log10(v) if v > 0 else float("-inf")


def onset_of(x):
    peak = float(np.max(np.abs(x)))
    if peak <= 0.0:
        raise ValueError("silent sound")
    return int(np.argmax(np.abs(x) >= peak * 10.0 ** (ONSET_DB / 20.0)))


def decay_index(power, onset, level_db):
    """First sample where the energy still to come is `level_db` under the whole (Schroeder)."""
    edc = np.cumsum(power[::-1])[::-1]
    below = np.nonzero(edc[onset:] < edc[onset] * 10.0 ** (-level_db / 10.0))[0]
    return onset + int(below[0]) if len(below) else len(power)


def _spectrum(seg):
    spec = np.abs(np.fft.rfft(seg)) ** 2
    return np.fft.rfftfreq(len(seg), 1.0 / SR), spec


def _taper(seg, taper):
    """"both": a Hann window. "out": only a fade-out, so the very first milliseconds (the
    attack itself) keep their full weight. None: nothing."""
    if taper == "both":
        return seg * np.hanning(len(seg))
    if taper == "out":
        return seg * np.hanning(2 * len(seg))[len(seg):]
    return seg


def _centroid(seg, taper):
    if len(seg) < 16:
        return float("nan")
    f, spec = _spectrum(_taper(seg, taper))
    band = (f >= F_LO) & (f <= F_HI)
    tot = float(spec[band].sum())
    return float((f[band] * spec[band]).sum() / tot) if tot > 0 else float("nan")


def soft_mask(f, lo, hi, width_oct, lo_soft=True, hi_soft=True):
    """Amplitude of a band whose edges are crossfades `width_oct` wide, centred on the edge on
    a log-frequency axis (sine going up, cosine going down: two neighbours add up to the whole
    power). A hard edge makes the measure depend on how the filter is built as soon as a drum
    body sits on it (and bodies do sit around 200 Hz); a soft edge does not."""
    lf = np.log2(np.maximum(f, 1e-3))
    m = np.ones(len(f))
    if lo is not None:
        m *= np.sin(0.5 * math.pi * np.clip((lf - math.log2(lo)) / width_oct + 0.5, 0.0, 1.0)) if lo_soft \
            else (f >= lo)
    if hi is not None:
        m *= np.sin(0.5 * math.pi * np.clip((math.log2(hi) - lf) / width_oct + 0.5, 0.0, 1.0)) if hi_soft \
            else (f < hi)
    return m


GROUP_WIDTH_OCT = 1.0  # crossfade of the low / mid / high groups, around 200 Hz and 2 kHz
ENV_WIDTH_OCT = 0.5    # crossfade between two octave bands


def _env_mask(f, i):
    """Octave band i. The outer edges (20 Hz, 20 kHz) stay hard: they are the edges of the whole."""
    last = len(OCT_EDGES) - 2
    return soft_mask(f, OCT_EDGES[i], OCT_EDGES[i + 1], ENV_WIDTH_OCT, lo_soft=i > 0, hi_soft=i < last)


def _group_mask(f, g):
    """Low: everything under 200 Hz down to 20. Mid: 200 Hz to 2 kHz. High: above 2 kHz, up to 20 kHz."""
    lo, hi = GROUPS[g]
    return soft_mask(f, lo, hi, GROUP_WIDTH_OCT, lo_soft=g > 0, hi_soft=g < len(GROUPS) - 1)


def band_signal(x, mask, pad=16384):
    """Band of x through `mask(f)`, in the frequency domain. The FFT is circular and a filter
    rings on both sides: without silence around the sound, the ringing that belongs before
    the first sample comes back at the end of the buffer and the band never "ends"."""
    xp = np.concatenate([np.zeros(pad), x, np.zeros(pad)])
    spec = np.fft.rfft(xp)
    spec *= mask(np.fft.rfftfreq(len(xp), 1.0 / SR))
    return np.fft.irfft(spec, len(xp))[pad:pad + len(x)]


def fingerprint(x):
    """The timbre fingerprint of one sound: 19 numbers, level-free."""
    x = np.asarray(x, dtype=np.float64)
    out = {k: float("nan") for k in KEYS}
    onset = onset_of(x)
    power = x * x
    i20, i60 = decay_index(power, onset, 20), decay_index(power, onset, 60)
    out["t20_ms"] = 1000.0 * (i20 - onset) / SR
    out["t60_ms"] = 1000.0 * (i60 - onset) / SR
    cut = onset + int(round(0.010 * SR))
    e_t, e_b = float(power[onset:cut].sum()), float(power[cut:].sum())
    if e_t > 0 and e_b > 0:
        out["transient_body_db"] = _db(e_t / e_b)

    a = max(0, onset - int(0.001 * SR))
    seg = x[a:max(i60, a + 16)]
    f, spec = _spectrum(seg)
    band = (f >= F_LO) & (f <= F_HI)
    tot = float(spec[band].sum())
    out["centroid_hz"] = float((f[band] * spec[band]).sum() / tot)
    out["hf_db"] = max(_db(float(spec[band & (f >= 10000.0)].sum()) / tot), -80.0)
    for i in range(len(OCT_EDGES) - 1):
        out[ENV_KEYS[i]] = max(_db(float((spec * _env_mask(f, i) ** 2).sum()) / tot), FLOOR_DB)

    out["centroid_att_hz"] = _centroid(x[a:onset + int(0.020 * SR)], "out")
    tail = x[onset + int(0.040 * SR):i60]
    if len(tail) >= int(0.010 * SR):
        out["centroid_tail_hz"] = _centroid(tail, "both")

    whole = float(power[onset:].sum())
    for g, name in enumerate(("low", "mid", "high")):
        p = band_signal(x, lambda f, g=g: _group_mask(f, g)) ** 2
        if p[onset:].sum() > whole * 10.0 ** (-30.0 / 10.0):  # an empty band has no length
            out["t20_%s_ms" % name] = 1000.0 * (decay_index(p, onset, 20) - onset) / SR
    return out


# --- Counter-measures: the same quantities by another road.

_FIR_CACHE = {}


def _fir(key, mask, taps=8191):
    """A linear-phase filter with the response `mask`, cut to `taps` samples (Blackman)."""
    if key not in _FIR_CACHE:
        n = 1 << 17
        h = np.fft.irfft(mask(np.fft.rfftfreq(n, 1.0 / SR)), n)
        half = (taps - 1) // 2
        _FIR_CACHE[key] = np.concatenate([h[-half:], h[:half + 1]]) * np.blackman(taps)
    return _FIR_CACHE[key]


def fir_band_signal(x, key, mask):
    """Band of x by convolution in the time domain, delay removed."""
    h = _fir(key, mask)
    d = (len(h) - 1) // 2
    return np.convolve(x, h)[d:d + len(x)]


def _abs_omega(taps):
    """Fourier series of |w| on (-pi, pi): pi/2 at 0, -2 / (pi n^2) at odd n, 0 at even n."""
    n = np.arange(taps) - (taps - 1) // 2
    h = np.zeros(taps)
    odd = (n % 2) != 0
    h[odd] = -2.0 / (math.pi * n[odd].astype(float) ** 2)
    h[(taps - 1) // 2] = math.pi / 2.0
    return h


_ABS_OMEGA = _abs_omega(4095)
_LOWPASS_20K = (lambda n: 2.0 * 20300.0 / SR * np.sinc(2.0 * 20300.0 / SR * n) * np.blackman(len(n)))(
    np.arange(1023) - 511.0)


def counter_fingerprint(x):
    """env*, t20_low/mid/high by time-domain filters; the three centroids in the time domain
    too (a kernel whose response is |frequency|). Unlike the measure, the centroids keep what
    lies under 20 Hz: a disagreement there points at an offset or a rumble in the sound."""
    x = np.asarray(x, dtype=np.float64)
    out = {}
    onset = onset_of(x)
    power = x * x
    i60 = decay_index(power, onset, 60)
    a = max(0, onset - int(0.001 * SR))
    seg = x[a:max(i60, a + 16)]
    pad = np.concatenate([np.zeros(4096), seg, np.zeros(4096)])  # room for the filters to ring
    e = [float((fir_band_signal(pad, ("env", i), lambda f, i=i: _env_mask(f, i)) ** 2).sum())
         for i in range(len(OCT_EDGES) - 1)]
    tot = sum(e)
    for i, v in enumerate(e):
        out[ENV_KEYS[i]] = max(_db(v / tot), FLOOR_DB)

    whole = float(power[onset:].sum())
    for g, name in enumerate(("low", "mid", "high")):
        p = fir_band_signal(x, ("group", g), lambda f, g=g: _group_mask(f, g)) ** 2
        out["t20_%s_ms" % name] = float("nan")
        if p[onset:].sum() > whole * 10.0 ** (-30.0 / 10.0):
            out["t20_%s_ms" % name] = 1000.0 * (decay_index(p, onset, 20) - onset) / SR

    def inst(s, taper):
        # Time domain, no spectrum estimate: sum(y * (k * y)) / sum(y * y), where the kernel k
        # has the frequency response |w|. Its error is under 3 Hz, and only next to 0 Hz.
        if len(s) < 16:
            return float("nan")
        y = np.convolve(_taper(s, taper), _LOWPASS_20K)
        d = (len(_ABS_OMEGA) - 1) // 2
        ky = np.convolve(y, _ABS_OMEGA)[d:d + len(y)]
        return float((y * ky).sum() / (y * y).sum() * SR / (2.0 * math.pi))

    out["centroid_hz"] = inst(seg, None)
    out["centroid_att_hz"] = inst(x[a:onset + int(0.020 * SR)], "out")
    tail = x[onset + int(0.040 * SR):i60]
    out["centroid_tail_hz"] = inst(tail, "both") if len(tail) >= int(0.010 * SR) else float("nan")
    return out


# --- Distances.

def band_time_map(x):
    """Energy in 25 Bark bands over 9 time slices that double in length, in dB under the
    whole sound, clamped. It compares envelopes, never the exact wave: two noises drawn
    from different dice with the same colour and the same decay give the same map."""
    x = np.asarray(x, dtype=np.float64)
    onset = onset_of(x)
    total = float((x[onset:] ** 2).sum())
    rows = []
    for i in range(len(MAP_MS) - 1):
        a, b = onset + MAP_MS[i] * SR // 1000, onset + MAP_MS[i + 1] * SR // 1000
        seg = x[a:b]
        if len(seg) < 16:
            rows.append(np.full(len(BARK_EDGES) - 1, FLOOR_DB - 10.0))
            continue
        f, spec = _spectrum(np.pad(seg, (0, max(0, 2048 - len(seg)))))
        idx = np.searchsorted(f, BARK_EDGES)
        e = np.add.reduceat(spec, idx[:-1])
        e[idx[:-1] >= idx[1:]] = 0.0
        # Parseval: sum(|X|^2) over the half spectrum is about N/2 times the energy.
        e = e / (0.5 * len(f) * 2.0) / total
        rows.append(np.maximum(10.0 * np.log10(np.maximum(e, 1e-30)), FLOOR_DB - 10.0))
    return np.array(rows)


def map_distance(a, b):
    """Mean absolute difference of two band-time maps, in dB."""
    return float(np.mean(np.abs(a - b)))


def null_db(x, y, max_shift_ms=5.0):
    """The null test: how much of x disappears when the best-aligned, best-scaled y is
    subtracted, in dB (0 = nothing cancels, -20 = 99 % of the energy cancels)."""
    n = max(len(x), len(y))
    fx, fy = np.fft.rfft(x, 2 * n), np.fft.rfft(y, 2 * n)
    cc = np.fft.irfft(fx * np.conj(fy))
    s = int(max_shift_ms * SR / 1000)
    peak = float(np.max(np.abs(np.concatenate([cc[:s + 1], cc[-s:]]))))
    rho2 = peak * peak / (float((x * x).sum()) * float((y * y).sum()))
    return _db(max(1.0 - rho2, 1e-12))


# --- Family profile and score (the drum rule's own formula, extended to the fingerprint).

def quantile(values, q):
    v = sorted(values)
    pos = q * (len(v) - 1)
    lo = int(math.floor(pos))
    hi = min(lo + 1, len(v) - 1)
    return v[lo] + (v[hi] - v[lo]) * (pos - lo)


def profile(prints, keys=KEYS):
    """Median, p10 and p90 of each measure over a family of fingerprints."""
    prof = {}
    for k in keys:
        vals = [p[k] for p in prints if not math.isnan(p[k])]
        if len(vals) < max(5, len(prints) // 2):
            continue  # a measure most sounds do not have is not a target
        prof[k] = (quantile(vals, 0.5), quantile(vals, 0.1), quantile(vals, 0.9))
    return prof


def distances(print_, prof, keys=None):
    """Per measure: 2 |v - median| / (p90 - p10). 1 = on the edge of the family's middle 80 %."""
    out = {}
    for k in (keys or prof.keys()):
        if k not in prof:
            continue
        med, p10, p90 = prof[k]
        v = print_[k]
        out[k] = 4.0 if math.isnan(v) else 2.0 * abs(v - med) / max(p90 - p10, 1e-9)
    return out


def score(print_, prof, keys=None):
    d = distances(print_, prof, keys)
    return sum(d.values()) / len(d)


# --- A hidden, richer synthesizer: the stand-in for "reference sounds" in this study.
# Several modes, coloured noise in overlapping bands that each fade at their own speed,
# a click. Entirely made of formulas written here.

def _smooth_band(n, lo, hi, width_oct=0.5):
    f = np.fft.rfftfreq(n, 1.0 / SR)
    lf = np.log2(np.maximum(f, 1.0))
    up = np.clip((lf - math.log2(lo)) / width_oct + 0.5, 0.0, 1.0)
    down = np.clip((math.log2(hi) - lf) / width_oct + 0.5, 0.0, 1.0)
    return np.sin(0.5 * math.pi * up) * np.sin(0.5 * math.pi * down)


def rich(spec, n, rng):
    """spec: dict with 'sweep' (level, start, end, half_ms, tau_ms), 'modes' [(hz, level, tau_ms)],
    'noise' [(lo, hi, level, tau_ms)], 'click' (level, ms, lo)."""
    t = np.arange(n) / SR
    y = np.zeros(n)
    if "sweep" in spec:
        level, start, end, half, tau = spec["sweep"]
        hz = end + (start - end) * 0.5 ** (t * 1000.0 / half)
        y += level * np.sin(2.0 * math.pi * np.cumsum(hz) / SR) * np.exp(-t * 1000.0 / tau)
    for hz, level, tau in spec.get("modes", ()):
        y += level * np.sin(2.0 * math.pi * hz * t + rng.uniform(0, 0.3)) * np.exp(-t * 1000.0 / tau)
    for lo, hi, level, tau in spec.get("noise", ()):
        w = np.fft.irfft(np.fft.rfft(rng.standard_normal(n)) * _smooth_band(n, lo, hi), n)
        y += level * w / math.sqrt(float(np.mean(w * w))) * np.exp(-t * 1000.0 / tau)
    if "click" in spec:
        level, ms, lo = spec["click"]
        w = np.fft.irfft(np.fft.rfft(rng.standard_normal(n)) * _smooth_band(n, lo, 18000.0), n)
        y += level * w / math.sqrt(float(np.mean(w * w))) * np.exp(-t * 1000.0 / (0.4 * ms))
    return y / float(np.max(np.abs(y))) * 0.8


def family(kind, count, rng, n=72000):
    """`count` sounds of one invented family, drawn around a centre, in three invented
    "packs" that each lean their own way (brighter, longer...), like real collections do."""
    sounds = []
    for i in range(count):
        pack = i % 3
        bright = 2.0 ** (rng.normal(0.0, 0.18) + (-0.15, 0.0, 0.2)[pack])
        long_ = 2.0 ** (rng.normal(0.0, 0.22) + (0.1, -0.15, 0.05)[pack])
        j = lambda s=0.12: 2.0 ** rng.normal(0.0, s)  # noqa: E731
        if kind == "snare":
            f0 = 185.0 * j()
            spec = {
                "sweep": (1.0 * j(0.3), 1.5 * f0, f0, 6.0 * j(), 55.0 * long_ * j()),
                "modes": [(f0 * r * j(0.03), a * j(0.3), 45.0 * long_ / r)
                          for r, a in ((1.59, 0.45), (2.14, 0.3), (2.30, 0.22), (2.92, 0.15))],
                "noise": [(1200.0 * bright, 9000.0 * bright, 0.55 * j(0.3), 70.0 * long_ * j()),
                          (6000.0 * bright, 16000.0, 0.30 * j(0.3), 32.0 * long_ * j()),
                          (300.0, 1200.0, 0.18 * j(0.3), 90.0 * long_ * j())],
                "click": (0.5 * j(0.3), 1.5 * j(), 3000.0),
            }
        elif kind == "hat":
            base = 420.0 * j(0.08)
            spec = {
                "modes": [(base * r * 8.0 * bright, 0.12 * j(0.3), 28.0 * long_)
                          for r in (1.0, 1.4826, 1.8007, 2.5462, 2.6303, 3.8968)
                          if base * r * 8.0 * bright < 19000.0],
                "noise": [(6500.0 * bright, 17000.0, 0.8 * j(0.2), 26.0 * long_ * j()),
                          (3000.0 * bright, 6500.0 * bright, 0.25 * j(0.3), 14.0 * long_ * j())],
                "click": (0.3 * j(0.3), 1.0, 5000.0),
            }
        elif kind == "kick":
            f0 = 50.0 * j(0.1)
            spec = {
                "sweep": (1.0, 4.5 * f0 * j(), f0, 11.0 * j(), 130.0 * long_ * j()),
                "modes": [(2.0 * f0, 0.10 * j(0.4), 80.0 * long_), (140.0 * j(), 0.25 * j(0.4), 28.0)],
                "noise": [(200.0, 900.0 * bright, 0.06 * j(0.4), 35.0),
                          (2000.0 * bright, 7000.0 * bright, 0.03 * j(0.4), 6.0)],
                "click": (0.12 * j(0.4), 2.0, 2500.0),
            }
        else:
            raise ValueError(kind)
        sounds.append(rich(spec, n, rng))
    return sounds


# --- Modal analysis (ESPRIT): frequencies and dampings of decaying sinusoids.

def esprit(x, k, sr, m=None):
    """k complex exponentials in x (2 per real mode). Returns (hz, decay per second, complex amplitude)."""
    n = len(x)
    m = m or n // 3
    h = np.lib.stride_tricks.sliding_window_view(x, m)
    _, _, vh = np.linalg.svd(h, full_matrices=False)
    vs = vh[:k].conj().T
    phi = np.linalg.lstsq(vs[:-1], vs[1:], rcond=None)[0]
    z = np.linalg.eigvals(phi)
    basis = z[None, :] ** np.arange(n)[:, None]
    amp = np.linalg.lstsq(basis, x.astype(complex), rcond=None)[0]
    return np.angle(z) * sr / (2.0 * math.pi), -np.log(np.abs(z)) * sr, amp, np.real(basis @ amp)


# --- The note of a low drum (kick, 808, tom), by two roads.

NOTE_BAND = (20.0, 400.0)
NOTE_FROM_DB, NOTE_TO_DB = 10.0, 30.0  # the part of the tail that is read
NOTE_PERIODS = 2.0  # under two whole periods in that part, there is no note to read


def _note_mask(f):
    return soft_mask(f, NOTE_BAND[0], NOTE_BAND[1], 0.5)


def _note_window(low):
    """The tail of the low band, from 10 dB to 30 dB under its whole energy (Schroeder): the pitch
    drop of the attack is over by then. Returns the samples under a Hann window, or None when
    the tail is shorter than 20 ms."""
    power = low * low
    if float(power.sum()) <= 0.0:
        return None
    on = onset_of(low)
    a, b = decay_index(power, on, NOTE_FROM_DB), decay_index(power, on, NOTE_TO_DB)
    if b - a < SR // 50:
        return None
    return low[a:b] * np.hanning(b - a)


def note_hz(x):
    """Road one, in time: each period between two upward zero crossings gives a frequency; the
    note is their mean, every period weighted by its energy."""
    seg = _note_window(band_signal(x, _note_mask))
    if seg is None:
        return float("nan")
    up = np.nonzero((seg[:-1] < 0.0) & (seg[1:] >= 0.0))[0]
    if len(up) < 2:  # not one whole period in the tail
        return float("nan")
    at = up + seg[up] / (seg[up] - seg[up + 1])
    energy = np.cumsum(seg * seg)
    w = energy[up[1:]] - energy[up[:-1]]
    hz = float((SR / np.diff(at) * w).sum() / w.sum())
    return hz if hz * len(seg) / SR >= NOTE_PERIODS else float("nan")


def counter_note_hz(x):
    """Road two, in frequency: the band comes from a filter run in the time domain, and the note
    is the centre of gravity of the strongest lobe of the tail's spectrum."""
    seg = _note_window(fir_band_signal(x, "note", _note_mask))
    if seg is None:
        return float("nan")
    n = 1 << 18
    spec = np.abs(np.fft.rfft(seg, n)) ** 2
    f = np.fft.rfftfreq(n, 1.0 / SR)
    band = (f >= NOTE_BAND[0]) & (f <= 500.0)
    peak = float(f[band][int(np.argmax(spec[band]))])
    lobe = (f >= 0.55 * peak) & (f <= 1.45 * peak)
    hz = float((f[lobe] * spec[lobe]).sum() / spec[lobe].sum())
    return hz if hz * len(seg) / SR >= NOTE_PERIODS else float("nan")


NOTE_AGREE = 0.03  # half a semitone


def note(x):
    """The note that counts: both roads see one and they agree within half a semitone. Otherwise
    there is no note (NaN). One road alone can be wrong without knowing it: on a tail of hardly more
    than one period, what is left to read is mostly the ringing of the band filter."""
    a, b = note_hz(x), counter_note_hz(x)
    if math.isnan(a) or math.isnan(b) or abs(b / a - 1.0) > NOTE_AGREE:
        return float("nan")
    return 0.5 * (a + b)


# --- CMA-ES (Hansen's standard update), batch evaluation.

def cma(batch_loss, x0, sigma, iters, pop, rng):
    n = len(x0)
    mu = pop // 2
    w = math.log(mu + 0.5) - np.log(np.arange(1, mu + 1))
    w /= w.sum()
    mueff = 1.0 / float((w * w).sum())
    cc = (4 + mueff / n) / (n + 4 + 2 * mueff / n)
    cs = (mueff + 2) / (n + mueff + 5)
    c1 = 2 / ((n + 1.3) ** 2 + mueff)
    cmu = min(1 - c1, 2 * (mueff - 2 + 1 / mueff) / ((n + 2) ** 2 + mueff))
    damps = 1 + 2 * max(0.0, math.sqrt((mueff - 1) / (n + 1)) - 1) + cs
    chi = math.sqrt(n) * (1 - 1 / (4 * n) + 1 / (21 * n * n))
    mean = np.array(x0, dtype=float)
    pc, ps_ = np.zeros(n), np.zeros(n)
    b, d, c = np.eye(n), np.ones(n), np.eye(n)
    best_x, best_f = mean.copy(), float("inf")
    for g in range(iters):
        z = rng.standard_normal((pop, n))
        y = (z * d) @ b.T
        xs = mean + sigma * y
        fs = np.asarray(batch_loss(xs))
        order = np.argsort(fs)
        if fs[order[0]] < best_f:
            best_f, best_x = float(fs[order[0]]), xs[order[0]].copy()
        ysel = y[order[:mu]]
        ymean = w @ ysel
        mean = mean + sigma * ymean
        ps_ = (1 - cs) * ps_ + math.sqrt(cs * (2 - cs) * mueff) * (b @ ((b.T @ ymean) / d))
        hsig = float(np.linalg.norm(ps_)) / math.sqrt(1 - (1 - cs) ** (2 * (g + 1))) / chi < 1.4 + 2 / (n + 1)
        pc = (1 - cc) * pc + (math.sqrt(cc * (2 - cc) * mueff) * ymean if hsig else 0.0)
        c = ((1 - c1 - cmu) * c + c1 * (np.outer(pc, pc) + (0.0 if hsig else cc * (2 - cc)) * c)
             + cmu * (ysel.T * w) @ ysel)
        sigma *= math.exp((cs / damps) * (float(np.linalg.norm(ps_)) / chi - 1))
        c = (c + c.T) / 2
        d2, b = np.linalg.eigh(c)
        d = np.sqrt(np.maximum(d2, 1e-20))
    return best_x, best_f
