"""Musical templates: one 4-bar phrase per (style, family).

Every phrase is a list of notes (start_beat, midi, velocity, length_beats).
A phrase loops: the renderer plays it twice and keeps the second pass, so
reverb and delay tails wrap around and the file loops without a click.
"""
import random

# name, tempo, swing (0.5 = straight), tonic midi, scale, progression.
# Chords are (root degree, quality) in the scale; qualities add sevenths.
MAJ = [0, 2, 4, 5, 7, 9, 11]
MIN = [0, 2, 3, 5, 7, 8, 10]
STYLES = {
    "trap":       dict(bpm=140, swing=0.5, tonic=57, scale=MIN, prog=[(0, "m"), (5, "M"), (2, "M"), (6, "M")]),
    "house":      dict(bpm=124, swing=0.54, tonic=53, scale=MIN, prog=[(0, "m7"), (6, "M"), (5, "M7"), (6, "M")]),
    "techno":     dict(bpm=128, swing=0.5, tonic=55, scale=MIN, prog=[(0, "m"), (0, "m"), (5, "M"), (0, "m")]),
    "edm":        dict(bpm=128, swing=0.5, tonic=50, scale=MIN, prog=[(0, "m"), (5, "M"), (2, "M"), (6, "M")]),
    "pop":        dict(bpm=110, swing=0.5, tonic=60, scale=MAJ, prog=[(0, "M"), (4, "M"), (5, "m"), (3, "M")]),
    "lofi":       dict(bpm=82, swing=0.6, tonic=62, scale=MAJ, prog=[(1, "m7"), (4, "7"), (0, "M7"), (5, "m7")]),
    "synthwave":  dict(bpm=100, swing=0.5, tonic=57, scale=MIN, prog=[(0, "m"), (5, "M"), (6, "M"), (4, "m")]),
    "dnb":        dict(bpm=174, swing=0.5, tonic=52, scale=MIN, prog=[(0, "m7"), (5, "M7"), (2, "M"), (6, "M")]),
}
STYLE_ORDER = list(STYLES)
FAMILIES = ["lead", "pluck", "pad", "arp", "stab", "accords"]
QUAL = {"M": [0, 4, 7], "m": [0, 3, 7], "M7": [0, 4, 7, 11], "m7": [0, 3, 7, 10], "7": [0, 4, 7, 10]}
BARS = 4


def chord_notes(st, bar, octave=0):
    deg, q = st["prog"][bar % len(st["prog"])]
    root = st["tonic"] + st["scale"][deg] + 12 * octave
    while root > st["tonic"] + 7 + 12 * octave:  # keep voicings close to the tonic
        root -= 12
    return [root + i for i in QUAL[q]]


def scale_notes(st, lo, hi):
    out = []
    for o in range(-3, 4):
        for d in st["scale"]:
            n = st["tonic"] + 12 * o + d
            if lo <= n <= hi:
                out.append(n)
    return out


def swung(beat, swing):
    """Delay every off 8th (16th grid: odd 16ths) according to swing."""
    six = round(beat * 4, 6)
    if abs(six - round(six)) < 1e-6 and int(round(six)) % 2 == 1:
        return beat + (swing - 0.5) * 0.5
    return beat


# Rhythms on a 16th grid over one bar (16 steps): onsets, per style and role.
LEAD_RHYTHM = {
    "trap": [0, 3, 6, 8, 10, 14], "house": [0, 3, 6, 10, 12], "techno": [0, 3, 6, 9, 12],
    "edm": [0, 2, 4, 6, 8, 11, 14], "pop": [0, 2, 4, 7, 10, 12], "lofi": [0, 4, 6, 10, 14],
    "synthwave": [0, 4, 6, 8, 12], "dnb": [0, 3, 6, 10, 12, 14],
}
STAB_RHYTHM = {
    "trap": [0, 6, 10], "house": [2, 6, 10, 14], "techno": [0, 3, 6, 10], "edm": [2, 6, 10, 14],
    "pop": [0, 3, 6, 10, 12], "lofi": [0, 6, 10], "synthwave": [0, 6, 8, 14], "dnb": [0, 6, 10, 13],
}
CHORD_RHYTHM = {
    "trap": [0, 8], "house": [0, 3, 7, 10, 14], "techno": [0, 6, 12], "edm": [0, 4, 8, 12],
    "pop": [0, 4, 8, 12], "lofi": [0, 10], "synthwave": [0, 2, 4, 6, 8, 10, 12, 14], "dnb": [0, 6, 12],
}
PLUCK_RHYTHM = {
    "trap": [0, 3, 6, 8, 11, 14], "house": [0, 2, 3, 6, 8, 10, 11, 14], "techno": [0, 3, 6, 8, 11, 14],
    "edm": [0, 2, 4, 6, 8, 10, 12, 14], "pop": [0, 2, 4, 6, 8, 10, 12, 14], "lofi": [0, 3, 6, 10, 12],
    "synthwave": [0, 2, 4, 6, 8, 10, 12, 14], "dnb": [0, 2, 3, 6, 8, 10, 11, 14],
}


def phrase(style, family, variant=0):
    """Notes for one 4-bar loop. variant shifts the melody seed and voicing."""
    st = STYLES[style]
    rng = random.Random(f"{style}-{family}-{variant}")
    sw = st["swing"]
    notes = []
    if family == "lead":
        pool = scale_notes(st, st["tonic"] + 7, st["tonic"] + 26)
        cur = rng.choice(pool[2:6])
        rhythm = LEAD_RHYTHM[style]
        for bar in range(BARS):
            ch = set(n % 12 for n in chord_notes(st, bar))
            onsets = rhythm if bar != 3 else rhythm[: max(2, len(rhythm) - 2)]
            for k, step in enumerate(onsets):
                nxt = onsets[k + 1] if k + 1 < len(onsets) else 16 + (2 if bar == 3 else 0)
                length = max(1, min(nxt - step, 4)) / 4.0 * 0.9
                if k == 0 or step % 4 == 0:  # strong step: nearest chord tone
                    cands = [n for n in pool if n % 12 in ch]
                else:
                    i = pool.index(cur) if cur in pool else 3
                    cands = [pool[max(0, min(len(pool) - 1, i + d))] for d in (-2, -1, 1, 2)]
                cur = min(cands, key=lambda n: abs(n - cur) + rng.random() * 3)
                notes.append((swung(bar * 4 + step / 4, sw), cur, rng.randint(88, 112), length))
    elif family == "pad":
        for bar in range(BARS):
            for n in chord_notes(st, bar, 0) + [chord_notes(st, bar, -1)[0]]:
                notes.append((bar * 4.0, n, 90, 3.97))
    elif family == "arp":
        for bar in range(BARS):
            ch = chord_notes(st, bar, 0)
            seq = ch + [ch[0] + 12, ch[1] + 12] if variant % 2 == 0 else ch + [n + 12 for n in ch[::-1]]
            div = 4 if st["bpm"] < 150 else 2  # 16ths, 8ths in drum & bass
            for i in range(4 * div):
                n = seq[i % len(seq)] if variant % 3 != 2 else seq[::-1][i % len(seq)]
                notes.append((swung(bar * 4 + i / div, sw), n, 100 - 14 * (i % 2), 0.8 / div))
    elif family == "pluck":
        pool = scale_notes(st, st["tonic"] + 5, st["tonic"] + 22)
        for bar in range(BARS):
            ch = chord_notes(st, bar, 1)
            tones = sorted([n for n in pool if n % 12 in set(c % 12 for c in ch)])
            for k, step in enumerate(PLUCK_RHYTHM[style]):
                n = tones[(k * (2 + variant % 2) + bar) % len(tones)]
                notes.append((swung(bar * 4 + step / 4, sw), n, rng.randint(85, 110), 0.22))
    elif family in ("stab", "accords"):
        rhythm = (STAB_RHYTHM if family == "stab" else CHORD_RHYTHM)[style]
        length = 0.2 if family == "stab" else None
        for bar in range(BARS):
            ch = chord_notes(st, bar, 0 if family == "stab" else 0)
            if family == "accords" and len(ch) == 3:
                ch = ch + [ch[0] + 12]
            for k, step in enumerate(rhythm):
                nxt = rhythm[k + 1] if k + 1 < len(rhythm) else 16
                ln = length if length else (nxt - step) / 4.0 * 0.92
                for n in ch:
                    notes.append((swung(bar * 4 + step / 4, sw), n, rng.randint(90, 108), ln))
    return notes
