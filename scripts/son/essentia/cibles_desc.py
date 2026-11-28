"""Target descriptors for the sound teams (same code that built docs/sons/fin/cibles/*.tsv).

Usage:
    <venv-with-essentia>/bin/python -I scripts/son/essentia/cibles_desc.py out.jsonl a.wav b.mp3 ...

Input: any wav/mp3/flac (decoded by base.py, resampled to 44.1 kHz, first 20 s).
Output: one JSON line per file: {"path", dur_s, peak_db, attack_ms (-20 dB -> peak),
decay20_s / decay40_s (peak -> -20/-40 dB), rms_db, crest_db, lufs, width_db (side/mid),
sub120 / low300 / high4k (energy shares), centroid_hz, rolloff_hz, flatness_db,
lowflux (40-300 Hz flux), low_beat (std/mean of the 40-300 Hz envelope), n_peaks
(spectral peaks above -40 dB, median of the 12 loudest frames), dissonance}.
Compare each value with the p10..p90 columns of the zone tsv for the same family and style.
"""
import sys, os, json
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import base  # noqa: E402
SR = base.SR
CAP = 20.0
def env_db(y, win=0.005):
    n = max(1, int(win * SR))
    k = len(y) // n
    if k < 2:
        return np.array([-120.0, -120.0]), n
    e = np.sqrt((y[:k * n].reshape(k, n) ** 2).mean(1) + 1e-20)
    return 20 * np.log10(e + 1e-12), n

def measure(st):
    import essentia.standard as es
    st = np.ascontiguousarray(st[:int(CAP * SR), :2] if st.shape[1] >= 2 else np.repeat(st[:int(CAP * SR), :1], 2, 1)).astype(np.float32)
    L, R = st[:, 0], st[:, 1]
    y = 0.5 * (L + R)
    o = {"dur_s": len(y) / SR}
    pk = float(np.abs(st).max()) + 1e-12
    o["peak_db"] = 20 * np.log10(pk)
    e, n = env_db(y)
    ip = int(np.argmax(e)); emax = e[ip]
    a10 = np.where(e[:ip + 1] >= emax - 20)[0]
    o["attack_ms"] = (ip - a10[0]) * n / SR * 1000 if len(a10) else 0.0
    after = e[ip:]
    d20 = np.where(after < emax - 20)[0]
    o["decay20_s"] = d20[0] * n / SR if len(d20) else (len(after) * n / SR)
    d40 = np.where(after < emax - 40)[0]
    o["decay40_s"] = d40[0] * n / SR if len(d40) else (len(after) * n / SR)
    act = e > emax - 40
    rms_act = np.sqrt(np.mean(y[:len(act) * n].reshape(-1, n)[act] ** 2)) if act.any() else 1e-9
    o["rms_db"] = 20 * np.log10(rms_act + 1e-12)
    o["crest_db"] = 20 * np.log10(pk / (rms_act + 1e-12))
    # loudness
    try:
        lu = es.LoudnessEBUR128(sampleRate=SR, startAtZero=True)(st)
        o["lufs"] = float(lu[2])
    except Exception:
        o["lufs"] = None
    # width
    M, S_ = 0.5 * (L + R), 0.5 * (L - R)
    o["width_db"] = 10 * np.log10((np.sum(S_ ** 2) + 1e-12) / (np.sum(M ** 2) + 1e-12))
    # spectra
    F, H = 4096, 1024
    w = np.hanning(F).astype(np.float32)
    if len(y) < F:
        y = np.pad(y, (0, F - len(y)))
    fr = np.lib.stride_tricks.sliding_window_view(y, F)[::H] * w
    P = np.abs(np.fft.rfft(fr, axis=1)) ** 2
    fe = P.sum(1)
    keep = fe > fe.max() * 1e-4
    P = P[keep] if keep.any() else P
    f = np.fft.rfftfreq(F, 1 / SR)
    tot = P.sum() + 1e-20
    o["sub120"] = float(P[:, f < 120].sum() / tot)
    o["low300"] = float(P[:, f < 300].sum() / tot)
    o["high4k"] = float(P[:, f >= 4000].sum() / tot)
    cen = (P * f).sum(1) / (P.sum(1) + 1e-20)
    o["centroid_hz"] = float(np.median(cen))
    cs = np.cumsum(P, 1); ro = (cs < 0.85 * cs[:, -1:]).sum(1)
    o["rolloff_hz"] = float(np.median(f[np.minimum(ro, len(f) - 1)]))
    Pm = P[:, 1:] + 1e-20
    fl = np.exp(np.mean(np.log(Pm), 1)) / np.mean(Pm, 1)
    o["flatness_db"] = float(np.median(10 * np.log10(fl + 1e-20)))
    # low band (40-300) flux and beating
    b = (f >= 40) & (f <= 300)
    lb = np.sqrt(P[:, b])
    if len(lb) > 2:
        fx = np.sqrt((np.diff(lb, axis=0).clip(0) ** 2).sum(1)) / (lb[1:].sum(1) + 1e-12)
        o["lowflux"] = float(np.median(fx))
        le = lb.sum(1)
        mid = le[len(le) // 8: len(le) - len(le) // 8] if len(le) > 16 else le
        o["low_beat"] = float(np.std(mid) / (np.mean(mid) + 1e-12))
    else:
        o["lowflux"] = o["low_beat"] = None
    # harmonic peaks and dissonance on the loudest frames
    sp = es.SpectralPeaks(sampleRate=SR, maxPeaks=60, magnitudeThreshold=1e-5, minFrequency=20, maxFrequency=8000, orderBy="frequency")
    dis = es.Dissonance()
    idx = np.argsort(P.sum(1))[-12:]
    D, NP = [], []
    for i in idx:
        mag = np.sqrt(P[i]).astype(np.float32)
        mag = mag / (mag.max() + 1e-12)
        fq, mg = sp(mag)
        sel = mg > 0.01  # -40 dB
        NP.append(int(sel.sum()))
        if len(fq) > 1:
            D.append(dis(fq, mg))
    o["n_peaks"] = float(np.median(NP)) if NP else None
    o["dissonance"] = float(np.median(D)) if D else None
    return o

def from_bytes(data):
    x, sr = base.decode(data)
    x = x[:int(CAP * sr)]
    st = base.resample(x, sr)
    if st.ndim == 1:
        st = st[:, None]
    return measure(st)

def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    with open(sys.argv[1], "w") as f:
        for p in sys.argv[2:]:
            try:
                with open(p, "rb") as fh:
                    r = from_bytes(fh.read())
            except Exception as e:
                r = {"error": type(e).__name__}
            r["path"] = p
            f.write(json.dumps(r, default=float) + "\n")


if __name__ == "__main__":
    main()
