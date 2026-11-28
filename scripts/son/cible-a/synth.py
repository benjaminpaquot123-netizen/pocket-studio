"""Small parametric electronic drum synth (numpy) + pedalboard effect chain.

Every model takes a vector u in [0, 1]^n (what CMA-ES searches) plus a
style "box" that narrows each parameter's range, and returns a mono float32
signal at SR. Nothing here is copied from a commercial sound: these are the
textbook analogue-drum recipes (pitch-swept sine kick, tone + noise snare,
multi-burst clap, six-square metallic hat, FM/membrane percussion).
"""
import numpy as np
import pedalboard as pb

SR = 44100


def lerp(lo, hi, u, log=False):
    u = float(np.clip(u, 0.0, 1.0))
    if log:
        return float(np.exp(np.log(lo) + (np.log(hi) - np.log(lo)) * u))
    return lo + (hi - lo) * u


def env_exp(n, attack_s, decay_s, curve=1.0):
    t = np.arange(n) / SR
    a = np.clip(t / max(attack_s, 1e-4), 0, 1)
    d = np.exp(-np.maximum(t - attack_s, 0) / max(decay_s, 1e-4)) ** curve
    return (a * d).astype(np.float64)


def noise(n, rng):
    return rng.standard_normal(n)




def chain(x, hp=None, lp=None, drive=0.0, bits=None, comp=None, verb=None, bp=None):
    """pedalboard chain; all args optional."""
    fx = []
    if hp:
        fx.append(pb.HighpassFilter(cutoff_frequency_hz=hp))
    if bp:
        fx.append(pb.PeakFilter(cutoff_frequency_hz=bp[0], gain_db=bp[1], q=bp[2]))
    if lp:
        fx.append(pb.LowpassFilter(cutoff_frequency_hz=lp))
    if drive > 0.05:
        fx.append(pb.Distortion(drive_db=drive))
    if bits:
        fx.append(pb.Bitcrush(bit_depth=bits))
    if comp:
        fx.append(pb.Compressor(threshold_db=comp[0], ratio=comp[1], attack_ms=comp[2], release_ms=60))
    if verb:
        fx.append(pb.Reverb(room_size=verb[0], wet_level=verb[1], dry_level=1.0, damping=0.6, width=0.0))
    if not fx:
        return x.astype(np.float32)
    return pb.Pedalboard(fx)(x.astype(np.float32)[None, :], SR)[0]


# Parameter tables: name -> (lo, hi, log). A style box overrides (lo, hi).
KICK = {
    "f_end": (38, 75, True), "f_start": (120, 420, True), "sweep_s": (0.008, 0.08, True),
    "decay_s": (0.06, 0.9, True), "curve": (0.6, 2.0, False), "click": (0.0, 0.6, False),
    "click_lp": (2000, 12000, True), "drive": (0.0, 18.0, False), "len_s": (0.3, 1.2, False),
    "lp": (2500, 16000, True), "bits": (6, 16, False),
}
SNARE = {
    "f1": (150, 280, True), "f2": (250, 480, True), "tone_decay": (0.02, 0.15, True),
    "tone": (0.1, 0.8, False), "noise_decay": (0.04, 0.45, True), "n_hp": (300, 3000, True),
    "n_lp": (5000, 16000, True), "bp_f": (1500, 7000, True), "bp_g": (-3, 9, False),
    "drive": (0.0, 15.0, False), "verb": (0.0, 0.35, False), "len_s": (0.25, 1.0, False),
    "bits": (6, 16, False),
}
CLAP = {
    "bursts": (2, 5, False), "gap_ms": (6, 16, False), "burst_decay": (0.003, 0.012, True),
    "tail_decay": (0.05, 0.35, True), "bp_f": (900, 3500, True), "bp_g": (0, 12, False),
    "hp": (300, 1500, True), "lp": (5000, 16000, True), "verb": (0.0, 0.4, False),
    "drive": (0.0, 10.0, False), "len_s": (0.25, 0.8, False), "bits": (6, 16, False),
}
HAT = {
    "base": (300, 560, True), "metal": (0.2, 1.0, False), "decay_s": (0.015, 0.12, True),
    "hp": (5000, 10000, True), "lp": (11000, 19000, True), "bp_g": (-4, 8, False),
    "drive": (0.0, 8.0, False), "len_s": (0.1, 0.5, False), "curve": (0.7, 2.0, False),
    "bits": (6, 16, False), "tail": (0.05, 0.6, True), "spike_s": (0.001, 0.008, True),
}
OHAT = dict(HAT, decay_s=(0.12, 0.7, True), len_s=(0.3, 1.2, False))
PERC = {
    "f": (150, 2200, True), "ratio": (0.5, 3.5, False), "index": (0.0, 6.0, False),
    "drop": (0.0, 1.5, False), "decay_s": (0.02, 0.4, True), "noise": (0.0, 0.7, False),
    "n_bp": (800, 9000, True), "hp": (40, 800, True), "lp": (3000, 16000, True),
    "drive": (0.0, 12.0, False), "len_s": (0.15, 0.6, False), "bits": (6, 16, False),
}
TABLES = {"kick": KICK, "snare": SNARE, "clap": CLAP, "hat_closed": HAT, "hat_open": OHAT, "perc": PERC}


def params(table, box, u):
    names = list(table)
    p = {}
    for i, k in enumerate(names):
        lo, hi, lg = table[k]
        if k in box:
            lo, hi = box[k]
        p[k] = lerp(lo, hi, u[i], lg)
    return p


def kick(p, rng):
    n = int(p["len_s"] * SR)
    t = np.arange(n) / SR
    f = p["f_end"] + (p["f_start"] - p["f_end"]) * np.exp(-t / p["sweep_s"])
    ph = 2 * np.pi * np.cumsum(f) / SR
    body = np.sin(ph) * env_exp(n, 0.0008, p["decay_s"], p["curve"])
    cl = chain(noise(n, rng) * env_exp(n, 0.0002, 0.004), lp=p["click_lp"]).astype(np.float64)
    x = body + p["click"] * cl
    x[-int(0.01 * SR):] *= np.linspace(1, 0, int(0.01 * SR))
    bits = p["bits"] if p["bits"] < 13 else None
    return chain(x, hp=25, lp=p["lp"], drive=p["drive"], bits=bits)


def snare(p, rng):
    n = int(p["len_s"] * SR)
    t = np.arange(n) / SR
    tone = (np.sin(2 * np.pi * p["f1"] * t) + 0.6 * np.sin(2 * np.pi * p["f2"] * t)) * env_exp(n, 0.0005, p["tone_decay"])
    nz = chain(noise(n, rng), hp=p["n_hp"], lp=p["n_lp"]).astype(np.float64) * env_exp(n, 0.0008, p["noise_decay"])
    x = p["tone"] * tone + (1 - p["tone"]) * nz * 2.0
    verb = (0.35, p["verb"]) if p["verb"] > 0.03 else None
    bits = p["bits"] if p["bits"] < 13 else None
    y = chain(x, bp=(p["bp_f"], p["bp_g"], 0.9), drive=p["drive"], bits=bits, verb=verb)
    y[-int(0.01 * SR):] *= np.linspace(1, 0, int(0.01 * SR))
    return y


def clap(p, rng):
    n = int(p["len_s"] * SR)
    e = np.zeros(n)
    nb = int(round(p["bursts"]))
    t = np.arange(n) / SR
    for b in range(nb):
        t0 = b * p["gap_ms"] / 1000 * (1 + 0.15 * rng.uniform(-1, 1))
        m = t >= t0
        e[m] += np.exp(-(t[m] - t0) / p["burst_decay"]) * (0.8 + 0.2 * rng.uniform())
    t_last = (nb - 1) * p["gap_ms"] / 1000
    m = t >= t_last
    e[m] += 0.5 * np.exp(-(t[m] - t_last) / p["tail_decay"])
    x = noise(n, rng) * e
    verb = (0.4, p["verb"]) if p["verb"] > 0.03 else None
    bits = p["bits"] if p["bits"] < 13 else None
    y = chain(x, hp=p["hp"], lp=p["lp"], bp=(p["bp_f"], p["bp_g"], 1.2), drive=p["drive"], bits=bits, verb=verb)
    y[-int(0.01 * SR):] *= np.linspace(1, 0, int(0.01 * SR))
    return y


RATIOS = np.array([1.0, 1.4471, 1.6170, 1.9265, 2.5028, 2.6637])  # classic six-square hat


def hat(p, rng):
    n = int(p["len_s"] * SR)
    t = np.arange(n) / SR
    sq = sum(np.sign(np.sin(2 * np.pi * p["base"] * r * t + rng.uniform(0, 6.28))) for r in RATIOS) / 6
    x = p["metal"] * sq + (1 - p["metal"]) * noise(n, rng) * 0.5
    # stick transient on top of a quieter ringing body (reference hats peak hard, then ring under 40%)
    x = x * (env_exp(n, 0.0003, p["spike_s"]) * (1 - p["tail"]) + p["tail"] * env_exp(n, 0.0003, p["decay_s"], p["curve"]))
    bits = p["bits"] if p["bits"] < 13 else None
    y = chain(x, hp=p["hp"], lp=p["lp"], bp=(10000, p["bp_g"], 1.0), drive=p["drive"], bits=bits)
    y[-int(0.005 * SR):] *= np.linspace(1, 0, int(0.005 * SR))
    return y


def perc(p, rng):
    n = int(p["len_s"] * SR)
    t = np.arange(n) / SR
    f = p["f"] * (1 + p["drop"] * np.exp(-t / 0.015))
    mod = p["index"] * np.exp(-t / (p["decay_s"] * 0.5)) * np.sin(2 * np.pi * np.cumsum(f * p["ratio"]) / SR)
    car = np.sin(2 * np.pi * np.cumsum(f) / SR + mod)
    nz = chain(noise(n, rng), bp=(p["n_bp"], 12, 2.0), hp=p["n_bp"] * 0.5).astype(np.float64)
    x = (car * (1 - p["noise"]) + nz * p["noise"] * 0.6) * env_exp(n, 0.0004, p["decay_s"])
    bits = p["bits"] if p["bits"] < 13 else None
    y = chain(x, hp=p["hp"], lp=p["lp"], drive=p["drive"], bits=bits)
    y[-int(0.005 * SR):] *= np.linspace(1, 0, int(0.005 * SR))
    return y


MODELS = {"kick": kick, "snare": snare, "clap": clap, "hat_closed": hat, "hat_open": hat, "perc": perc}


def render(fam, box, u, seed=0):
    p = params(TABLES[fam], box, u)
    y = MODELS[fam](p, np.random.default_rng(seed)).astype(np.float64)
    pk = np.abs(y).max()
    return (y / pk).astype(np.float32) if pk > 0 else y.astype(np.float32), p
