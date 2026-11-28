"""Zone B bass descriptors (approximation of the stock recipe behind
docs/sons/fin/cibles/zone-b-basses.tsv) and robust distance to a target row.

Input: float stereo (n, 2) at 44.1 kHz. Descriptors:
dur_s, attack_ms (onset -> peak of 10 ms RMS envelope), decay20_s (peak -> last
frame within 20 dB of it), centroid_hz, rolloff_hz (85 %), flatness_db,
sub120 / low300 / high4k (energy share), lufs, rms_db, crest_db, n_peaks
(Essentia SpectralPeaks, max 60), dissonance, lowflux, low_beat
(std/mean of the <300 Hz envelope while sounding), width_db (side/mid energy).
"""
import csv

import numpy as np

SR = 44100
FR, HOP = 2048, 512
# descriptors used for the loss (one-shots / loops); dur_s and bpm are set by
# the render, attack/decay are meaningless on a whole loop
ONESHOT = ["attack_ms", "decay20_s", "centroid_hz", "rolloff_hz", "flatness_db", "sub120", "low300",
           "high4k_db", "lufs", "crest_db", "n_peaks", "dissonance", "lowflux", "low_beat", "width_db"]
LOOP = ["centroid_hz", "rolloff_hz", "flatness_db", "sub120", "low300", "high4k_db", "lufs", "crest_db",
        "n_peaks", "dissonance", "lowflux", "low_beat", "width_db"]
LOG = {"attack_ms", "decay20_s", "centroid_hz", "rolloff_hz", "dur_s"}
FLOOR = {"attack_ms": 0.25, "decay20_s": 0.2, "centroid_hz": 0.12, "rolloff_hz": 0.12, "flatness_db": 4.0,
         "sub120": 0.03, "low300": 0.02, "high4k": 0.01, "high4k_db": 3.0, "lufs": 1.0, "crest_db": 1.0, "n_peaks": 1.0,
         "dissonance": 0.02, "lowflux": 0.005, "low_beat": 0.05, "width_db": 3.0}
CLIP = {"high4k_db": (-80.0, 0.0), "width_db": (-100.0, 10.0), "flatness_db": (-120.0, 0.0)}
FAM = {"sub": "sub-basse", "reese": "basse-reese", "808": "808", "phrase": "phrase-de-basse",
       "basse-synthe": "basse-synthe"}
STYLE = {"drill": "trap", "boom-bap": "hiphop", "lo-fi": "lofi", "reggaeton-afro": "reggaeton"}


def load(path):
    t = {}
    with open(path) as f:
        for r in csv.DictReader(f, delimiter="\t"):
            t.setdefault((r["famille"], r["style"]), {})[r["descripteur"]] = {
                k: (-100.0 if r[k] == "mono" else float(r[k])) for k in ("p10", "p25", "p50", "p75", "p90")}
    return t


def target(T, family, style):
    f = FAM[family]
    s = STYLE.get(style, style)
    if (f, s) in T:
        return T[(f, s)], s
    return T[(f, "*")], "*"


def _frames(x):
    n = 1 + max(0, (len(x) - FR) // HOP)
    idx = np.arange(FR)[None, :] + HOP * np.arange(n)[:, None]
    return np.pad(x, (0, FR))[idx]


def measure(st):
    import cibles_desc
    st = np.asarray(st, np.float32)
    if st.ndim == 1:
        st = np.stack([st, st], 1)
    o = cibles_desc.measure(st)
    o["high4k_db"] = 10 * np.log10(max(o["high4k"], 1e-12))
    return o


def measure_approx(st):
    import essentia.standard as es
    st = np.asarray(st, np.float32)
    if st.ndim == 1:
        st = np.stack([st, st], 1)
    m = st.mean(1)
    s = 0.5 * (st[:, 0] - st[:, 1])
    out = {"dur_s": len(m) / SR}
    w = int(0.01 * SR)
    env = np.sqrt(np.convolve(m.astype(np.float64) ** 2, np.ones(w) / w, "same") + 1e-20)
    pk = int(np.argmax(env))
    thr_on = env[pk] * 10 ** (-40 / 20)
    on = int(np.argmax(env > thr_on))
    out["attack_ms"] = 1000.0 * max(pk - on, 0) / SR
    above = np.where(env >= env[pk] * 0.1)[0]
    out["decay20_s"] = (above[-1] - pk) / SR if len(above) else 0.0
    F = _frames(m) * np.hanning(FR)[None, :].astype(np.float32)
    X = np.abs(np.fft.rfft(F, axis=1))
    P = X ** 2
    fr = np.fft.rfftfreq(FR, 1 / SR)
    e = P.sum(1)
    act = e > e.max() * 1e-6
    Pa = P[act]
    tot = Pa.sum() + 1e-20
    out["sub120"] = float(Pa[:, fr < 120].sum() / tot)
    out["low300"] = float(Pa[:, fr < 300].sum() / tot)
    out["high4k"] = float(Pa[:, fr >= 4000].sum() / tot)
    Xa = X[act]
    cen = (Xa * fr).sum(1) / (Xa.sum(1) + 1e-20)
    out["centroid_hz"] = float(np.median(cen))
    c = np.cumsum(Pa, 1)
    out["rolloff_hz"] = float(np.median(fr[np.argmax(c >= 0.85 * c[:, -1:], 1)]))
    fl = np.exp(np.mean(np.log(Pa + 1e-20), 1)) / (Pa.mean(1) + 1e-20)
    out["flatness_db"] = float(np.median(10 * np.log10(np.maximum(fl, 1e-12))))
    rms = float(np.sqrt(np.mean(m.astype(np.float64) ** 2)))
    out["rms_db"] = 20 * np.log10(rms + 1e-12)
    out["crest_db"] = 20 * np.log10(np.abs(m).max() + 1e-12) - out["rms_db"]
    try:
        out["lufs"] = float(es.LoudnessEBUR128(sampleRate=SR)(st)[2])
    except Exception:
        out["lufs"] = out["rms_db"] - 3.0
    spk = es.SpectralPeaks(sampleRate=SR, maxPeaks=60, magnitudeThreshold=1e-3, minFrequency=20,
                           maxFrequency=SR / 2 - 1, orderBy="frequency")
    dis = es.Dissonance()
    npk, dd = [], []
    for row in Xa[:: max(1, len(Xa) // 60)].astype(np.float32):
        f_, a_ = spk(row / (row.max() + 1e-20))
        npk.append(len(f_))
        if len(f_) > 1:
            dd.append(dis(f_, a_))
    out["n_peaks"] = float(np.median(npk)) if npk else 0.0
    out["dissonance"] = float(np.median(dd)) if dd else 0.0
    low = np.sqrt(Pa[:, fr < 300].sum(1))
    lown = low / (low.max() + 1e-20)
    out["lowflux"] = float(np.median(np.abs(np.diff(lown)))) if len(lown) > 1 else 0.0
    snd = lown > 0.1
    out["low_beat"] = float(lown[snd].std() / (lown[snd].mean() + 1e-20)) if snd.sum() > 2 else 0.0
    es_, em_ = float(np.sum(s.astype(np.float64) ** 2)), float(np.sum(m.astype(np.float64) ** 2))
    out["width_db"] = 10 * np.log10(max(es_, 1e-17 * em_ + 1e-30) / (em_ + 1e-30))
    return out


def _v(k, v):
    if k in CLIP:
        v = min(max(v, CLIP[k][0]), CLIP[k][1])
    return np.log(max(v, 1e-3)) if k in LOG else v


def distance(m, tg, keys):
    z = {}
    for k in keys:
        if k not in tg or m.get(k) is None:
            continue
        p = tg[k]
        p25, p50, p75 = (_v(k, p[q]) for q in ("p25", "p50", "p75"))
        fl = FLOOR[k] if k not in LOG else FLOOR[k] * 4
        sc = max((p75 - p25) / 1.349, fl)
        z[k] = (_v(k, m[k]) - p50) / sc
    if not z:
        return float("inf"), z
    return float(np.sqrt(np.mean(np.square(np.clip(list(z.values()), -6, 6))))), z
