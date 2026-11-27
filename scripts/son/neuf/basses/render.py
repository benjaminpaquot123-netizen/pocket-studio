"""Render bass sounds: Surge XT (surgepy) patches, numpy 808s, VSCO pizz chains.

Every voice is described by a vector x in [0, 1]^n (what the CMA-ES loop
tunes) and fixed "identity" bounds per voice type, so the optimiser moves a
sound towards the family DNA without turning a sub into a lead.
"""
import glob
import os

import numpy as np

SR = 44100
BLOCK = 32


# ---------------------------------------------------------------- Surge XT

def _lerp(lo, hi, x):
    return lo + (hi - lo) * float(np.clip(x, 0.0, 1.0))


def _set(s, p, v):
    s.setParamVal(p, float(np.clip(v, s.getParamMin(p), s.getParamMax(p))))


# Each Surge voice type: list of (name, lo, hi) the optimiser explores.
SURGE_SPACE = {
    "sub": [("cutoff", -10, 25), ("reso", 0.0, 0.3), ("drive", -6, 10), ("ws", 0, 1),
            ("a", -8, -5), ("d", -3, 0.5), ("s", 0.3, 1.0), ("r", -4, -1), ("o2", 0.0, 0.35)],
    "basse-synthe": [("shape", -1, 1), ("cutoff", -20, 40), ("reso", 0.0, 0.7), ("envmod", 0, 60),
                     ("fd", -5, 0), ("fs", 0.0, 0.6), ("drive", -6, 18), ("ws", 0, 1),
                     ("a", -8, -5), ("d", -3, 0.5), ("s", 0.2, 1.0), ("r", -5, -1), ("sub", 0.0, 0.6)],
    "reese": [("detune", 0.1, 0.6), ("voices", 2, 6), ("cutoff", -10, 45), ("reso", 0.0, 0.5),
              ("envmod", 0, 30), ("drive", 0, 20), ("ws", 0, 1), ("o2det", 0.0, 0.5),
              ("a", -8, -4), ("s", 0.5, 1.0), ("r", -4, -1), ("lfo", 0.0, 1.0)],
}
WS_TYPES = {"sub": [1, 6, 7, 41], "basse-synthe": [1, 3, 24, 25, 41, 40], "reese": [1, 24, 26, 41, 15]}
FILTERS = {"sub": 2, "basse-synthe": 12, "reese": 2}


def surge_params(kind, x):
    sp = SURGE_SPACE[kind]
    return {n: _lerp(lo, hi, x[i]) for i, (n, lo, hi) in enumerate(sp)}


def _build_surge(kind, x, variant=0):
    import surgepy
    s = surgepy.createSurge(SR)
    pt = s.getPatch()
    sc = pt["scene"][0]
    v = surge_params(kind, x)
    o0, o1 = sc["osc"][0], sc["osc"][1]
    f = sc["filterunit"][0]
    amp, fenv = sc["adsr"][0], sc["adsr"][1]
    _set(s, sc["polymode"], 1)  # mono
    if kind == "sub":
        _set(s, o0["type"], 1)  # sine
        _set(s, sc["mute_o2"], 0)
        _set(s, o1["type"], 0)  # classic, a touch of harmonics
        _set(s, sc["level_o2"], v["o2"])
        _set(s, o1["p"][0], -1.0)
    elif kind == "basse-synthe":
        _set(s, o0["type"], 0)
        _set(s, o0["p"][0], v["shape"])
        _set(s, o0["p"][3], v["sub"])  # sub mix
        _set(s, f["envmod"], v["envmod"])
        _set(s, fenv["a"], -8)
        _set(s, fenv["d"], v["fd"])
        _set(s, fenv["s"], v["fs"])
        _set(s, fenv["r"], -3)
    elif kind == "reese":
        _set(s, o0["type"], 0)
        _set(s, o0["p"][0], 0.0)
        _set(s, o0["p"][6], round(v["voices"]))
        _set(s, o0["p"][5], v["detune"])
        _set(s, sc["mute_o2"], 0)
        _set(s, o1["type"], 0)
        _set(s, o1["p"][0], 0.0)
        _set(s, o1["p"][6], 2)
        _set(s, o1["p"][5], v["o2det"])
        _set(s, f["envmod"], v["envmod"])
        _set(s, fenv["d"], -1.5)
        _set(s, fenv["s"], 0.4)
        _set(s, amp["d"], -1)
        lfo = sc["lfo"][0]
        _set(s, lfo["rate"], -2 + 3 * v["lfo"])
        try:
            s.setModDepth01(f["cutoff"], s.getModSource(0) if False else _lfo_src(s), 0.08)
        except Exception:
            pass
    _set(s, f["type"], FILTERS[kind])
    _set(s, f["cutoff"], v["cutoff"])
    _set(s, f["resonance"], v["reso"])
    ws = WS_TYPES[kind]
    _set(s, sc["wsunit"]["type"], ws[min(int(v["ws"] * len(ws)), len(ws) - 1)])
    _set(s, sc["wsunit"]["drive"], v["drive"])
    _set(s, amp["a"], v["a"])
    if "d" in v:
        _set(s, amp["d"], v["d"])
    _set(s, amp["s"], v["s"])
    _set(s, amp["r"], v["r"])
    return s


def _lfo_src(s):
    import surgepy
    return surgepy.constants.ms_lfo1


def render_surge(kind, x, events, total_s):
    """events: list of (start_s, dur_s, midi_note, velocity)."""
    s = _build_surge(kind, x)
    nblk = int(np.ceil(total_s * SR / BLOCK))
    buf = s.createMultiBlock(nblk)
    marks = []
    for (t0, d, n, vel) in events:
        marks.append((int(t0 * SR / BLOCK), 1, n, vel))
        marks.append((int((t0 + d) * SR / BLOCK), 0, n, vel))
    marks.sort(key=lambda m: (m[0], m[1]))
    pos = 0
    for (b, on, n, vel) in marks:
        b = min(b, nblk)
        if b > pos:
            s.processMultiBlock(buf, pos, b - pos)
            pos = b
        if on:
            s.playNote(0, int(n), int(vel), 0)
        else:
            s.releaseNote(0, int(n), 0)
    if pos < nblk:
        s.processMultiBlock(buf, pos, nblk - pos)
    return np.ascontiguousarray(buf.T[: int(total_s * SR)], dtype=np.float32)


# ---------------------------------------------------------------- 808 (numpy + pedalboard)

SPACE_808 = [("drop", 0.0, 24.0), ("drop_t", 0.01, 0.12), ("decay", 0.4, 3.0), ("click", 0.0, 0.5),
             ("drive", 0.0, 24.0), ("lp", 200.0, 4000.0), ("tone2", 0.0, 0.4), ("glide", 0.0, 1.0)]


def _p808(x):
    return {n: _lerp(lo, hi, x[i]) for i, (n, lo, hi) in enumerate(SPACE_808)}


def render_808(x, events, total_s, slide=False):
    import pedalboard as pb
    v = _p808(x)
    n = int(total_s * SR)
    out = np.zeros(n, np.float32)
    prev_f = None
    for (t0, d, note, vel) in events:
        f = 440.0 * 2 ** ((note - 69) / 12.0)
        i0 = int(t0 * SR)
        m = min(n - i0, int((d + v["decay"]) * SR))
        t = np.arange(m) / SR
        freq = f * 2 ** (v["drop"] / 12.0 * np.exp(-t / v["drop_t"]))
        if slide and prev_f is not None and v["glide"] > 0.3:  # drill-style slide from the last note
            g = np.exp(-t / (0.04 + 0.08 * v["glide"]))
            freq = freq * (1 - g) + prev_f * g
        ph = 2 * np.pi * np.cumsum(freq) / SR
        env = np.exp(-t / v["decay"]) * np.clip(t / 0.002, 0, 1)
        rel = np.clip((d + 0.05 - t) / 0.05, 0, 1) if d < v["decay"] else 1.0
        y = (np.sin(ph) + v["tone2"] * np.sin(2 * ph)) * env * rel
        y += v["click"] * np.exp(-t / 0.004) * np.sin(2 * np.pi * 1800 * t)
        out[i0:i0 + m] += (vel / 127.0) * y[: m].astype(np.float32)
        prev_f = f
    board = pb.Pedalboard([pb.Distortion(drive_db=v["drive"]), pb.LowpassFilter(cutoff_frequency_hz=v["lp"]),
                           pb.Compressor(threshold_db=-12, ratio=3)])
    y = board(out[None, :], SR)[0]
    return np.stack([y, y], 1).astype(np.float32)


# ---------------------------------------------------------------- acoustic (VSCO-2-CE pizz + pedalboard)

SPACE_AC = [("hp", 20.0, 60.0), ("low_gain", -3.0, 9.0), ("hi_gain", -12.0, 12.0), ("drive", 0.0, 18.0),
            ("comp", -30.0, -6.0), ("ratio", 1.5, 8.0), ("lp", 1500.0, 12000.0), ("room", 0.0, 0.25)]

_PIZZ = {}


def _pizz_bank(root):
    if root in _PIZZ:
        return _PIZZ[root]
    import soundfile as sf
    names = "C C# D D# E F F# G G# A A# B".split()
    bank = {}
    for p in sorted(glob.glob(os.path.join(root, "*.wav"))):
        tok = os.path.basename(p).split("_")[2]  # e.g. E0, A#1
        nm, octv = tok[:-1], int(tok[-1])
        if nm not in names:
            continue
        midi = 12 * (octv + 1) + names.index(nm) + 12  # VSCO naming is one octave low
        y, sr = sf.read(p, dtype="float32", always_2d=True)
        if sr != SR:
            import librosa
            y = librosa.resample(y.T, orig_sr=sr, target_sr=SR).T
        bank.setdefault(midi, []).append(np.ascontiguousarray(y))
    _PIZZ[root] = bank
    return bank


def render_acoustic(x, events, total_s, root, rr=0):
    import librosa
    import pedalboard as pb
    v = {nm: _lerp(lo, hi, x[i]) for i, (nm, lo, hi) in enumerate(SPACE_AC)}
    bank = _pizz_bank(root)
    keys = np.array(sorted(bank))
    n = int(total_s * SR)
    out = np.zeros((n, 2), np.float32)
    for (t0, d, note, vel) in events:
        k = int(keys[np.argmin(np.abs(keys - note))])
        smp = bank[k][rr % len(bank[k])]
        if k != note:
            smp = np.stack([librosa.effects.pitch_shift(smp[:, c], sr=SR, n_steps=note - k) for c in range(smp.shape[1])], 1)
        if smp.shape[1] == 1:
            smp = np.repeat(smp, 2, 1)
        i0 = int(t0 * SR)
        m = min(n - i0, len(smp), int((d + 0.12) * SR))
        fade = np.clip((d + 0.12 - np.arange(m) / SR) / 0.12, 0, 1)[:, None]
        out[i0:i0 + m] += (vel / 127.0) * smp[:m] * fade
    board = pb.Pedalboard([pb.HighpassFilter(v["hp"]), pb.LowShelfFilter(120, v["low_gain"]),
                           pb.HighShelfFilter(3000, v["hi_gain"]), pb.Distortion(v["drive"]),
                           pb.Compressor(threshold_db=v["comp"], ratio=v["ratio"]),
                           pb.LowpassFilter(v["lp"]), pb.Reverb(room_size=0.2, wet_level=v["room"], dry_level=1.0)])
    return board(out.T.copy(), SR).T.astype(np.float32)


# ---------------------------------------------------------------- dispatch

def dims(kind):
    if kind in SURGE_SPACE:
        return len(SURGE_SPACE[kind])
    return {"808": len(SPACE_808), "basse-acoustique": len(SPACE_AC)}[kind]


def render(kind, x, events, total_s, pizz_root=None, slide=False):
    if kind == "808":
        y = render_808(x, events, total_s, slide=slide)
    elif kind == "basse-acoustique":
        y = render_acoustic(x, events, total_s, pizz_root)
    else:
        y = render_surge(kind, x, events, total_s)
    y = np.nan_to_num(y)
    pk = float(np.max(np.abs(y))) or 1.0
    return (y / pk * 0.891).astype(np.float32)  # -1 dBFS peak
