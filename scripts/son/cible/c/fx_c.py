"""Zone C effects, synthesised from noise and oscillators (no samples, no
patches), then shaped by pycma toward the fx/fx ADN like the synth families.

Four kinds, two variants each per style (8 effects per style):
  montant (riser, 4 or 8 bars: Benjamin found the first ones too quick),
  descendant (downlifter, 2 or 4 bars), impact (boom + a real kick body:
  "manque de kick"), balayage (filter sweep up and down, 2 or 4 bars; the
  previous one was kept).
pycma tunes 4 synthesis parameters and 6 pedalboard ones per effect.
"""
import json
import math
import os
import zlib

import numpy as np

import synthes as S  # noqa: E402
import tgt_c  # noqa: E402

SR = S.SR
KINDS = ["montant", "descendant", "impact", "balayage"]
ADN_FAM = "fx/fx"


def _bandpass_sweep(x, centers, q, n_fft=2048, hop=256):
    """Time-varying log-Gaussian band-pass (STFT mask); centers in Hz per hop."""
    win = np.hanning(n_fft)
    pad = np.concatenate([np.zeros(n_fft // 2), x, np.zeros(n_fft)])
    nfr = 1 + (len(pad) - n_fft) // hop
    idx = np.arange(n_fft)[None, :] + hop * np.arange(nfr)[:, None]
    spec = np.fft.rfft(pad[idx] * win, axis=1)
    f = np.fft.rfftfreq(n_fft, 1 / SR)[None, :]
    c = np.interp(np.arange(nfr), np.linspace(0, nfr - 1, len(centers)), centers)[:, None]
    spec *= np.exp(-0.5 * (np.log2(np.maximum(f, 1.0) / c) * q) ** 2)
    fr = np.fft.irfft(spec, n_fft, axis=1) * win
    out = np.zeros(len(pad))
    norm = np.zeros(len(pad))
    for i in range(nfr):
        out[i * hop:i * hop + n_fft] += fr[i]
        norm[i * hop:i * hop + n_fft] += win ** 2
    out = out / np.maximum(norm, 1e-6)
    return out[n_fft // 2:n_fft // 2 + len(x)]


def _saw(freq_t, rng, voices=5, detune=0.15):
    ph0 = rng.uniform(0, 1, voices)
    y = np.zeros(len(freq_t))
    for i in range(voices):
        d = 2 ** (detune * (i - (voices - 1) / 2) / 12 / max(1, (voices - 1) / 2))
        ph = (np.cumsum(freq_t * d) / SR + ph0[i]) % 1.0
        y += 2 * ph - 1
    return y / voices


def _hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def _norm(v):
    return v / (np.abs(v).max() + 1e-9)


def synth(kind, dur, u, seed, tonic):
    """Mono effect; u in [0, 1]^4 are the tunable synthesis parameters."""
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = t / dur
    if kind == "montant":
        q = 0.6 + 2.0 * u[0]
        f_hi = 3000 * (14000 / 3000) ** u[1]
        tone = 0.15 + 0.6 * u[2]
        curve = 1.2 + 2.0 * u[3]  # slow start, the build lasts the whole riser
        nz = _bandpass_sweep(rng.normal(0, 1, n), 200 * (f_hi / 200) ** np.linspace(0, 1, n // 256 + 1) ** 1.5, q)
        sw = _saw(_hz(tonic - 12) * 2 ** (24 * s ** 1.6 / 12), rng)
        y = (1 - tone) * _norm(nz) + tone * 0.7 * sw
        y *= s ** curve
        y[-int(0.006 * SR):] *= np.linspace(1, 0, int(0.006 * SR))
    elif kind == "descendant":
        q = 0.6 + 1.6 * u[0]
        f_hi = 3000 * (12000 / 3000) ** u[1]
        tone = 0.1 + 0.6 * u[2]
        decay = 0.8 + 2.4 * u[3]
        nz = _bandpass_sweep(rng.normal(0, 1, n), f_hi * (120 / f_hi) ** np.linspace(0, 1, n // 256 + 1) ** 0.6, q)
        sw = np.sin(2 * np.pi * np.cumsum(_hz(tonic + 12) * 2 ** (-24 * s ** 0.7 / 12)) / SR)
        y = (1 - tone) * _norm(nz) + tone * sw
        y *= (1 - s) ** decay * np.minimum(1, t / 0.004)
    elif kind == "impact":
        boom_decay = 0.4 + 1.6 * u[0]
        crack = 0.1 + 0.6 * u[1]
        kick = 0.6 + 0.4 * u[2]
        f0 = 38 + 22 * u[3]
        fb = f0 + (130 - f0) * np.exp(-t / 0.08)
        boom = np.sin(2 * np.pi * np.cumsum(fb) / SR) * np.exp(-t / boom_decay)
        fk = 48 + 190 * np.exp(-t / 0.035)  # kick body: fast pitch drop, punchy
        kb = np.sin(2 * np.pi * np.cumsum(fk) / SR) * np.exp(-t / 0.28)
        click = rng.normal(0, 1, n) * np.exp(-t / 0.004)
        nz = _bandpass_sweep(rng.normal(0, 1, n) * np.exp(-t / 0.09), np.array([2800.0, 700.0]), 0.6)
        y = np.tanh(1.4 * boom) * 0.8 + kick * np.tanh(2.2 * kb) + 0.25 * kick * _norm(click) + crack * _norm(nz)
        y *= np.minimum(1, t / 0.0008)
    else:  # balayage
        q = 1.0 + 2.5 * u[0]
        f_hi = 4000 * (15000 / 4000) ** u[1]
        up = 0.35 + 0.4 * u[2]
        floor = 0.15 + 0.5 * u[3]
        k = np.linspace(0, 1, n // 256 + 1)
        shape = np.where(k < up, k / up, 1 - (k - up) / (1 - up))
        nz = _bandpass_sweep(rng.normal(0, 1, n), 300 * (f_hi / 300) ** shape, q)
        y = _norm(nz) * np.interp(s, k, floor + (1 - floor) * shape) * np.minimum(1, s / 0.04)
        y[-int(0.05 * SR):] *= np.linspace(1, 0, int(0.05 * SR))
    return y


def chain(y, xp, kind):
    """pedalboard one-shot chain (no loop wrap); xp in [-1, 1]^6."""
    import pedalboard as pb
    u = (np.clip(xp, -1, 1) + 1) / 2
    width = 0.7 if kind != "impact" else 0.3
    st = np.stack([y, np.roll(y, int(0.0004 * SR))], 1).astype(np.float32)  # small Haas width
    st = (1 - width) * np.stack([y, y], 1) + width * st
    board = pb.Pedalboard([
        pb.HighpassFilter(cutoff_frequency_hz=float(20 * (200 / 20) ** u[0])),
        pb.LowShelfFilter(cutoff_frequency_hz=200, gain_db=float(-10 + 18 * u[1])),
        pb.PeakFilter(cutoff_frequency_hz=1500, gain_db=float(-8 + 14 * u[2]), q=1.0),
        pb.HighShelfFilter(cutoff_frequency_hz=5000, gain_db=float(-18 + 22 * u[3])),
        pb.LowpassFilter(cutoff_frequency_hz=float(2000 * (20000 / 2000) ** u[4])),
        pb.Reverb(room_size=0.7, damping=0.5, wet_level=float(0.35 * u[5]), dry_level=1.0, width=1.0),
    ])
    tail = int((1.2 if kind != "montant" else 0.05) * SR)
    x = np.concatenate([st, np.zeros((tail, 2), np.float32)]).astype(np.float32)
    out = board(x.T.copy(), SR).T
    if kind == "montant":  # a riser stops dead on the downbeat
        out = out[:len(st)]
        out[-int(0.006 * SR):] *= np.linspace(1, 0, int(0.006 * SR))[:, None]
    else:
        a = np.abs(out).max(1)
        k = np.flatnonzero(a > a.max() * 10 ** (-60 / 20))
        out = out[:max(len(st) // 2, int(k[-1]) + int(0.05 * SR))].copy()
        f = int(0.03 * SR)
        out[-f:] *= np.linspace(1, 0, f)[:, None]
    return np.ascontiguousarray(out).astype(np.float32)


def duration(kind, style, variant):
    spb = 60.0 / S.music.STYLES[style]["bpm"]
    if kind == "montant":
        return spb * 4 * (4 if variant == 0 else 8)
    if kind == "impact":
        return 2.0 + variant * 1.2
    return spb * 4 * (2 if variant == 0 else 4)


def evaluate(kind, style, variant, x, target):
    import base
    seed = zlib.crc32(f"{style}|{kind}|{variant}".encode()) % 100000
    y = synth(kind, duration(kind, style, variant), (np.clip(x[:4], -1, 1) + 1) / 2, seed,
              S.music.STYLES[style]["tonic"])
    y = S.master(chain(y, x[4:], kind), tgt_c.master_lufs(target))
    if y is None:
        return 99.0, None
    return tgt_c.score(tgt_c.measure(y), target), y


def job(a):
    import cma
    kind, style, variant, budget = a
    target = tgt_c.target("effet", style)
    es_ = cma.CMAEvolutionStrategy(np.zeros(10), 0.35, {
        "bounds": [-1, 1], "popsize": 8, "maxfevals": budget, "verbose": -9,
        "seed": zlib.crc32(f"{style}|{kind}|{variant}".encode()) % 100000 + 1})
    sc0, _ = evaluate(kind, style, variant, np.zeros(10), target)
    best = (sc0, np.zeros(10))
    while not es_.stop():
        xs = es_.ask()
        fs = []
        for x in xs:
            try:
                sc, _ = evaluate(kind, style, variant, np.asarray(x), target)
            except Exception:
                sc = 99.0
            fs.append(sc)
            if sc < best[0]:
                best = (sc, np.asarray(x))
        es_.tell(xs, fs)
    return dict(kind=kind, style=style, variant=variant, family="effet", score0=sc0, score=best[0],
                x=[float(v) for v in best[1]])


def export_one(r, out):
    import base
    target = tgt_c.target("effet", r["style"])
    _, y = evaluate(r["kind"], r["style"], r["variant"], np.asarray(r["x"]), target)
    name = f"{r['style']}-effet-{r['kind']}-{r['variant'] + 1:02d}.mp3"
    path = os.path.join(out, "mp3", name)
    gain = 1.0
    for _ in range(4):
        dec = S.encode_mp3(y * gain, path)
        pk = 20 * math.log10(max(np.abs(dec).max(), 1e-9))
        if pk <= -0.95:
            break
        gain *= 10 ** ((-1.0 - pk - 0.05) / 20)
    m = tgt_c.measure(dec.copy())
    return dict(r, fichier=name, score_mp3=tgt_c.score(m, target), peak_db=pk, cible=target["_cell"],
                z={k: round(v, 2) for k, v in tgt_c.zscores(m, target).items()}, ess=m)


def _job_export(a):
    return export_one(*a)


def run(out, styles, budget):
    S.FAM_ADN["effet"] = ADN_FAM
    S.FAMILY_TYPE["effet"] = "fx"
    path = os.path.join(out, "fx.jsonl")
    done = {}
    if os.path.exists(path):
        for l in open(path):
            r = json.loads(l)
            done[(r["kind"], r["style"], r["variant"])] = r
    jobs = [(k, st, v, budget) for st in styles for k in KINDS for v in (0, 1) if (k, st, v) not in done]
    with S._pool() as pool, open(path, "a") as f:
        for r in pool.imap_unordered(job, jobs):
            f.write(json.dumps(r) + "\n")
            f.flush()
            done[(r["kind"], r["style"], r["variant"])] = r
            print("fx", r["style"], r["kind"], r["variant"], round(r["score0"], 2), "->", round(r["score"], 2),
                  flush=True)
    os.makedirs(os.path.join(out, "mp3"), exist_ok=True)
    ep = os.path.join(out, "export.jsonl")
    have = {json.loads(l)["fichier"] for l in open(ep)} if os.path.exists(ep) else set()
    todo = [(r, out) for r in done.values() if r["style"] in styles and
            f"{r['style']}-effet-{r['kind']}-{r['variant'] + 1:02d}.mp3" not in have]
    with S._pool() as pool, open(ep, "a") as f:
        for r in pool.imap_unordered(_job_export, todo):
            f.write(json.dumps(r, default=float) + "\n")
            print("export", r["fichier"], round(r["score_mp3"], 2), flush=True)
