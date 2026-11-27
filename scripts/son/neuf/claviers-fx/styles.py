"""Musical content per style: tempo, key, chord progressions and the note
patterns of each family. A note is (start_beat, len_beats, midi, velocity).
Phrases are 4 bars at 110 bpm and above, 2 bars below (4/4).
"""
import numpy as np

TEMPO = {"pop": 100, "lofi": 80, "house": 124, "trap": 140, "edm": 128,
         "jazz-soul": 90, "rnb": 70}
SWING = {"lofi": 0.33, "jazz-soul": 0.4, "rnb": 0.2}

# Chords as (root offset from key root, intervals above that root).
MAJ, MIN, MAJ7, MIN7, DOM7 = (0, 4, 7), (0, 3, 7), (0, 4, 7, 11), (0, 3, 7, 10), (0, 4, 7, 10)
MAJ9, MIN9, MIN11 = (0, 4, 7, 11, 14), (0, 3, 7, 10, 14), (0, 3, 7, 10, 14, 17)
PROG = {
    "pop": [(0, MAJ), (7, MAJ), (9, MIN), (5, MAJ)],
    "lofi": [(5, MAJ7), (4, MIN7), (2, MIN9), (0, MAJ9)],
    "house": [(0, MIN7), (0, MIN7), (8, MAJ7), (10, DOM7)],
    "trap": [(0, MIN), (8, MAJ), (3, MAJ), (10, MAJ)],
    "edm": [(0, MIN), (8, MAJ), (3, MAJ), (10, MAJ)],
    "jazz-soul": [(2, MIN9), (7, (0, 4, 10, 14, 21)), (0, MAJ9), (9, (0, 4, 10, 13))],
    "rnb": [(5, MAJ9), (4, MIN11), (2, MIN11), (0, MAJ9)],
}
KEYS = {"pop": [60, 62, 65, 67], "lofi": [60, 62, 63, 65], "house": [57, 58, 60, 62],
        "trap": [57, 59, 60, 61], "edm": [57, 58, 60, 62], "jazz-soul": [58, 60, 63, 65],
        "rnb": [61, 63, 65, 58]}


def bars(style):
    return 4 if TEMPO[style] >= 110 else 2


def voicing(key, chord, low=52, high=74):
    """Close voicing of the chord inside [low, high]."""
    r, iv = chord
    notes = sorted(((key + r + i - low) % 12) + low for i in iv)
    out = []
    for n in notes:
        while out and n <= out[-1]:
            n += 12
        out.append(n)
    while out[-1] > high and out[0] - 12 >= low - 5:
        out = out[:-1] + [out[-1] - 12]
        out.sort()
    return out


def chords_per_bar(style):
    """One chord per bar (2-bar phrases take two chords per bar)."""
    p = PROG[style]
    nb = bars(style)
    if nb == 4:
        return [(b * 4.0, 4.0, c) for b, c in enumerate(p)]
    return [(i * 2.0, 2.0, c) for i, c in enumerate(p)]


def _rng(seed):
    return np.random.default_rng(seed)


# ------------------------------------------------------------------ keys
def keys_phrase(style, family, seed):
    """Returns (left_hand, right_hand) note lists."""
    rng = _rng(seed)
    key = int(rng.choice(KEYS[style]))
    lh, rh = [], []
    for t0, ln, ch in chords_per_bar(style):
        v = voicing(key, ch)
        root = key + ch[0]
        bass = (root - 36) % 12 + 36
        if style == "pop":
            if family == "piano":
                lh += [(t0, ln, bass, 80), (t0, ln, bass + 12, 64)]
                order = [0, 1, 2, 1] * 4
                for k in range(int(ln * 2)):
                    rh.append((t0 + k * 0.5, 0.5, v[order[k] % len(v)] + 12, 70 + (10 if k % 2 == 0 else 0)))
            else:
                lh.append((t0, ln, bass + 12, 70))
                for s in (0, 1.5, 2.5) if ln == 4 else (0, 1.0):
                    rh += [(t0 + s, 1.0, n, 72) for n in v]
        elif style == "lofi":
            lh.append((t0, ln, bass, 62))
            rh += [(t0 + 0.02 * i, ln * 0.9, n, 55 + 4 * i) for i, n in enumerate(v)]
            top = v[-1] + 12
            rh += [(t0 + 1.5, 0.5, top, 58), (t0 + 1.0 if ln == 2 else t0 + 3.0, 0.5, top - 3 if ch[1] in (MIN7, MIN9) else top - 1, 50)]
        elif style == "house":
            hits = [0, 0.75, 1.5, 2.5, 3.25] if ln == 4 else [0, 0.75, 1.5]
            for s in hits:
                rh += [(t0 + s, 0.25, n, 100) for n in v]
            lh.append((t0, 0.5, bass + 12, 85))
        elif style == "trap":
            scale = [0, 2, 3, 5, 7, 8, 11]
            lh += [(t0, ln, bass, 75), (t0, ln, bass + 12, 60)]
            rh += [(t0, ln, n, 55) for n in v]
            mel = rng.choice(scale, size=3)
            for k, s in enumerate((0.0, 1.5, 3.0) if ln == 4 else (0.0, 0.75, 1.5)):
                rh.append((t0 + s, 1.0, key + 12 + int(mel[k]), 82))
        elif style == "edm":
            lh += [(t0, ln, bass, 95), (t0, ln, bass + 12, 85)]
            for s in np.arange(0, ln, 1.0):
                rh += [(t0 + s, 0.9, n, 105) for n in v + [v[0] + 12]]
        elif style == "jazz-soul":
            vv = voicing(key, (ch[0], ch[1][1:]), 55, 76)  # rootless
            lh.append((t0, ln * 0.5, bass, 70))
            for s, d in ((0, 0.5), (0.75, 0.5)) if ln == 2 else ((0, 1.0), (1.5, 0.5), (2.75, 1.0)):
                rh += [(t0 + s, d, n, 68 + int(rng.integers(-6, 6))) for n in vv]
        elif style == "rnb":
            lh.append((t0, ln, bass, 66))
            rh += [(t0 + 0.06 * i, ln * 0.95, n, 58 + 3 * i) for i, n in enumerate(v)]
    return lh, rh


def organ_phrase(style, seed):
    rng = _rng(seed)
    key = int(rng.choice(KEYS[style]))
    out = []
    for t0, ln, ch in chords_per_bar(style):
        v = voicing(key, ch, 55, 79)
        if style == "house":
            hits = [0, 0.5, 1.25, 2.0, 2.75, 3.5] if ln == 4 else [0, 0.5, 1.25]
            for s in hits:
                out += [(t0 + s, 0.3, n, 100) for n in v]
        elif style == "jazz-soul":
            out += [(t0, ln * 0.6, n, 80) for n in voicing(key, (ch[0], ch[1][1:]), 55, 76)]
            out += [(t0 + ln * 0.75, ln * 0.2, n, 70) for n in voicing(key, (ch[0], ch[1][1:]), 55, 76)]
            out.append((t0, ln, (key + ch[0] - 36) % 12 + 36, 75))
        else:
            out += [(t0, ln, n, 80) for n in v]
    return out


# ------------------------------------------------------------------ voice
def chop_pattern(style, seed):
    rng = _rng(seed)
    key = int(rng.choice(KEYS[style]))
    major = style in ("pop",)
    scale = [0, 2, 4, 7, 9] if major else [0, 3, 5, 7, 10]
    pats = {
        "pop": [(0, .5), (.5, .5), (1.5, .5), (2, 1), (3, .5), (3.5, .5)],
        "lofi": [(0, 1.5), (2, .5), (2.5, 1.5)],
        "house": [(.5, .25), (1.5, .25), (1.75, .25), (2.5, .25), (3.5, .5)],
        "trap": [(0, .25), (.25, .25), (.75, .5), (1.5, .25), (2, .75), (3, .25), (3.5, .25)],
        "edm": [(0, .5), (.5, .25), (.75, .25), (1, .5), (1.5, .5), (2, 1), (3, .25), (3.25, .25), (3.5, .5)],
        "rnb": [(0, 1), (1.5, .5), (2, 2)],
    }
    out = []
    nb = bars(style)
    deg = list(rng.choice(scale, size=12))
    k = 0
    for b in range(nb if nb == 4 else 2):
        base = b * 4.0
        for s, l in pats.get(style, pats["pop"]):
            out.append((base + s, l, key + int(deg[k % len(deg)]), 100))
            k += 1
    return out


def pump(x, bpm, depth=0.7):
    """Sidechain-like pumping on each beat (EDM, house)."""
    sr = 48000
    t = np.arange(len(x)) / sr
    tb = (t % (60.0 / bpm))
    g = (1 - depth) + depth * (1 - np.exp(-tb / 0.07))
    return x * g[:, None].astype(np.float32)
