#!/usr/bin/env python3
"""Experiments of the timbre study (docs/sons/fin/RECHERCHE-TIMBRE.md). Prints numbers only.

    python3 -I scripts/son/timbre/etude.py <render_voice binary> [section ...]

Sections: cibles aveugle bruit modes copie contre note famille (all when none is named).
Sounds come from the engine's own voice (render_voice) or from formulas (timbre.rich).
Nothing is written to disk.
"""
import math
import multiprocessing
import os
import subprocess
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(REPO, "scripts", "sons"))
import mesure  # noqa: E402  the drum tables' own measures
import timbre as T  # noqa: E402

SR = T.SR
BIN = None
FAMILIES = ("kick", "808", "clap", "caisse_claire", "charley_ferme", "charley_ouvert", "rim", "tom", "crash")
STYLES = ("house", "techhouse", "techno", "trap", "dnb", "dubstep")
TABLE = os.path.join(REPO, "docs", "sons", "fin", "cibles-batterie-definitives.tsv")
TABLE_KEYS = ("t20_ms", "t60_ms", "centroid_hz", "hf_db", "transient_body_db")


def render(recipes, frames):
    """Plays recipes (19 numbers each) through the engine's voice. Returns [recipe, frames]."""
    text = "".join(" ".join("%.9g" % v for v in r) + "\n" for r in recipes)
    raw = subprocess.run([BIN, "render", str(frames)], input=text.encode(), stdout=subprocess.PIPE, check=True).stdout
    return np.frombuffer(raw, dtype="<f4").reshape(len(recipes), frames).astype(np.float64)


def chosen():
    out = {}
    for line in subprocess.run([BIN, "list"], stdout=subprocess.PIPE, check=True).stdout.decode().splitlines():
        v = line.split()
        out[(FAMILIES[int(v[0])], STYLES[int(v[1])])] = [float(s) for s in v[2:]]
    return out


def targets():
    """Definitive targets of the table: (family, style) -> measure -> (p10, p25, median, p75, p90)."""
    out = {}
    with open(TABLE, encoding="utf-8") as f:
        for line in f:
            c = line.rstrip("\n").split("\t")
            if line.startswith("#") or len(c) < 12 or not c[11].startswith("définitive"):
                continue
            if c[1] == "tous_styles" or c[0] == "boucle" or c[5] not in TABLE_KEYS:
                continue
            out.setdefault((c[0], c[1]), {})[c[5]] = tuple(float(v) for v in c[6:11])
    return out


def table_measures(x):
    m = mesure.measure(SR, x)
    return {k: m[k] for k in TABLE_KEYS}


def gap(v, t):
    return 2.0 * abs(v - t[2]) / (t[4] - t[0])


# --- 1. Our 54 sounds against the timbre targets the table already holds.

def section_cibles():
    print("\n== cibles: the app's chosen sounds against the definitive targets of the table")
    recs, tg = chosen(), targets()
    cells = sorted(tg)
    ys = render([recs[c] for c in cells], 4 * SR)
    tally = {k: [0, 0] for k in TABLE_KEYS}
    print("family\tstyle\tmeasure\tours\tp10\tmedian\tp90\tgap\tinside")
    for c, y in zip(cells, ys):
        m = table_measures(y)
        for k in TABLE_KEYS:
            if k not in tg[c]:
                continue
            t = tg[c][k]
            inside = t[0] <= m[k] <= t[4]
            tally[k][0] += inside
            tally[k][1] += 1
            print("%s\t%s\t%s\t%.4g\t%.4g\t%.4g\t%.4g\t%.2f\t%s" % (c[0], c[1], k, m[k], t[0], t[2], t[4], gap(m[k], t), "yes" if inside else "NO"))
    for k in TABLE_KEYS:
        print("inside p10-p90\t%s\t%d of %d" % (k, tally[k][0], tally[k][1]))
    ln = [tally[k] for k in ("t20_ms", "t60_ms")]
    tb = [tally[k] for k in ("centroid_hz", "hf_db", "transient_body_db")]
    print("lengths inside\t%d of %d" % (sum(a for a, _ in ln), sum(b for _, b in ln)))
    print("timbre inside\t%d of %d" % (sum(a for a, _ in tb), sum(b for _, b in tb)))


# --- 2. The rule of today looks at lengths only: how far can the colour move unseen?

def section_aveugle():
    print("\n== aveugle: same lengths for the rule, very different colours")
    recs, tg = chosen(), targets()
    for cell in (("charley_ferme", "techhouse"), ("clap", "techhouse"), ("kick", "techhouse")):
        base = recs[cell]
        t = tg[cell]
        grid = []
        for hp in (0.25, 0.5, 1.0, 2.0, 4.0):
            for lp in (0.25, 0.5, 1.0, 2.0, 4.0):
                for click in (0.0, 1.0, 3.0):
                    r = list(base)
                    r[8] = min(max(base[8] * hp, 20.0), 16000.0)   # noiseHpHz
                    r[9] = min(max(base[9] * lp, 200.0), 20000.0)  # noiseLpHz
                    r[16] = min(base[16] * click if base[16] > 0 else 0.1 * click, 1.0)  # clickLevel
                    r[13] = base[13] * (hp ** 0.5)                 # metalHz follows half way
                    grid.append(r)
        ys = render(grid, 2 * SR)
        ms = [table_measures(y) for y in ys]
        base_m = table_measures(render([base], 2 * SR)[0])

        def length_score(m):
            return sum(gap(m[k], t[k]) for k in ("t20_ms", "t60_ms")) / 2.0

        ref = length_score(base_m)
        same = [m for m in ms if length_score(m) <= max(ref, 0.5)]
        print("%s %s: chosen sound length score %.2f, centroid %.0f Hz, hf %.1f dB, transient/body %.1f dB" % (
            cell[0], cell[1], ref, base_m["centroid_hz"], base_m["hf_db"], base_m["transient_body_db"]))
        print("  %d of %d variants score as well or better on lengths (or under 0.5)" % (len(same), len(ms)))
        for k in ("centroid_hz", "hf_db", "transient_body_db"):
            vals = [m[k] for m in same]
            inside = sum(t[k][0] <= v <= t[k][4] for v in vals)
            print("  %s among them: %.4g to %.4g (target %.4g to %.4g); inside the target: %d of %d" % (
                k, min(vals), max(vals), t[k][0], t[k][4], inside, len(vals)))


# --- 3. Noise is dice: which distance survives a new throw?

def mss(x, y):
    """Multi-resolution STFT distance, bin by bin (mean absolute log-magnitude difference)."""
    tot = 0.0
    for n in (256, 1024, 4096):
        hop = n // 4
        w = np.hanning(n)
        fx = np.abs(np.fft.rfft(np.lib.stride_tricks.sliding_window_view(x, n)[::hop] * w, axis=1))
        fy = np.abs(np.fft.rfft(np.lib.stride_tricks.sliding_window_view(y, n)[::hop] * w, axis=1))
        tot += float(np.mean(np.abs(np.log(fx + 1e-3) - np.log(fy + 1e-3))))
    return tot / 3.0


def section_bruit():
    print("\n== bruit: the same sound with other dice, against a really different sound")
    n = 48000
    spec = {"sweep": (1.0, 280.0, 185.0, 6.0, 55.0), "modes": [(294.0, 0.45, 35.0), (396.0, 0.3, 26.0)],
            "noise": [(1200.0, 9000.0, 0.55, 70.0), (6000.0, 16000.0, 0.30, 32.0), (300.0, 1200.0, 0.18, 90.0)],
            "click": (0.5, 1.5, 3000.0)}
    bright = dict(spec, noise=[(1200.0 * 1.3, 9000.0 * 1.3, 0.55, 70.0), (6000.0 * 1.3, 16000.0, 0.30, 32.0), (300.0, 1200.0, 0.18, 90.0)])
    a = T.rich(spec, n, np.random.default_rng(1))
    b = T.rich(spec, n, np.random.default_rng(2))
    c = T.rich(bright, n, np.random.default_rng(3))
    ma, mb, mc = T.band_time_map(a), T.band_time_map(b), T.band_time_map(c)
    fa, fb, fc = T.fingerprint(a), T.fingerprint(b), T.fingerprint(c)
    print("distance\tsame sound, other dice\tnoise 30 % brighter\tratio")
    d1, d2 = mss(a, b), mss(a, c)
    print("stft bin by bin\t%.3f\t%.3f\t%.2f" % (d1, d2, d2 / d1))
    d1, d2 = T.map_distance(ma, mb), T.map_distance(ma, mc)
    print("band-time map (dB)\t%.3f\t%.3f\t%.2f" % (d1, d2, d2 / d1))
    live = [k for k in T.ENV_KEYS if fa[k] > T.FLOOR_DB + 5.0]
    d1 = sum(abs(fa[k] - fb[k]) for k in live) / len(live)
    d2 = sum(abs(fa[k] - fc[k]) for k in live) / len(live)
    print("nine octave bands, whole sound (dB)\t%.3f\t%.3f\t%.2f" % (d1, d2, d2 / d1))
    for k in ("centroid_hz", "centroid_att_hz", "centroid_tail_hz"):
        d1, d2 = abs(fb[k] / fa[k] - 1.0) * 100.0, abs(fc[k] / fa[k] - 1.0) * 100.0
        print("%s (%%)\t%.2f\t%.2f\t%.1f" % (k, d1, d2, d2 / max(d1, 1e-9)))
    # How much the dice alone move each measure: twelve throws of the same sound.
    throws = [T.fingerprint(T.rich(spec, n, np.random.default_rng(100 + i))) for i in range(12)]
    for k in ("centroid_hz", "hf_db", "t20_ms", "t20_high_ms", "env6_db", "env8_db"):
        vals = [f[k] for f in throws]
        print("twelve throws of the same sound, %s: %.4g to %.4g" % (k, min(vals), max(vals)))
    print("null test, same sound with other dice: %.2f dB (0 = nothing cancels)" % T.null_db(a, b))
    print("null test, a sound against itself 0.5 dB louder: %.1f dB" % T.null_db(a, a * 1.06))


# --- 4. Modes by ESPRIT.

def section_modes():
    print("\n== modes: ESPRIT on a sound whose modes are known")
    rng = np.random.default_rng(5)
    n = 4096
    t = np.arange(n) / SR
    truth = [(180.0 * r, a, tau) for r, a, tau in ((1.0, 1.0, 0.200), (1.59, 0.5, 0.120), (2.14, 0.35, 0.090),
                                                   (2.30, 0.3, 0.080), (2.65, 0.2, 0.060), (2.92, 0.15, 0.045))]
    clean = sum(a * np.sin(2 * math.pi * hz * t + 0.3 * i) * np.exp(-t / tau) for i, (hz, a, tau) in enumerate(truth))
    for noise_db in (-60.0, -40.0, -20.0):
        x = clean + rng.standard_normal(n) * float(np.sqrt(np.mean(clean ** 2))) * 10.0 ** (noise_db / 20.0)
        hz, decay, amp, _ = T.esprit(x, 12, SR)
        cents, taus = [], []
        for f0, _, tau in truth:
            i = int(np.argmin(np.abs(hz - f0)))
            cents.append(abs(1200.0 * math.log2(hz[i] / f0)))
            taus.append(abs(1.0 / decay[i] / tau - 1.0) * 100.0)
        print("noise %.0f dB under the sound: worst pitch error %.2f cents, worst decay error %.1f %%" % (noise_db, max(cents), max(taus)))

    print("\n== modes: can modes replay a noisy sound? (one invented snare)")
    ref = T.family("snare", 3, np.random.default_rng(11), n=24000)[0]
    on = T.onset_of(ref)
    seg = ref[on:on + n]
    other = T.family("snare", 3, np.random.default_rng(12), n=24000)[1]
    oseg = other[T.onset_of(other):][:n]
    print("pairs of exponentials\tnumbers kept\tsamples covered\tresidual dB\tnull against another snare dB")
    for k in (4, 8, 16, 32, 64, 128, 256, 512):
        _, _, _, model = T.esprit(seg, 2 * k, SR, m=1400)
        res = 10.0 * math.log10(float(((seg - model) ** 2).sum()) / float((seg ** 2).sum()))
        print("%d\t%d\t%d\t%.1f\t%.1f" % (k, 4 * k, n, res, T.null_db(oseg, model)))


# --- 5. Where a model turns into a copy.

def _stft(x, n=1024, hop=256):
    xp = np.concatenate([np.zeros(n), x, np.zeros(n)])
    return np.fft.rfft(np.lib.stride_tricks.sliding_window_view(xp, n)[::hop] * np.hanning(n), axis=1)


def _istft(spec, length, n=1024, hop=256):
    w = np.hanning(n)
    out = np.zeros(hop * (len(spec) - 1) + n)
    norm = np.zeros(len(out))
    for i, fr in enumerate(np.fft.irfft(spec, n, axis=1) * w):
        out[i * hop:i * hop + n] += fr
        norm[i * hop:i * hop + n] += w * w
    return (out / np.maximum(norm, 1e-9))[n:n + length]


def redraw(ref, bands, slices, rng):
    """Our own noise, shaped to follow ONE sound on a grid of `bands` x `slices` cells (both
    spaced like the ear: narrow at the bottom and at the start). No phase comes from the sound.
    Returns the redrawn sound and how many numbers were taken from the original."""
    power = np.abs(_stft(ref)) ** 2
    noise = _stft(rng.standard_normal(len(ref)))
    frames, bins = power.shape
    fe = np.unique(np.round(np.geomspace(1, bins, bands + 1)).astype(int)) if bands < bins else np.arange(bins + 1)
    te = np.unique(np.round(np.geomspace(4, frames, slices + 1)).astype(int)) if slices < frames else np.arange(frames + 1)
    fe[0], te[0] = 0, 0
    gain = np.zeros(power.shape)
    for a, b in zip(te[:-1], te[1:]):
        for c, d in zip(fe[:-1], fe[1:]):
            gain[a:b, c:d] = math.sqrt(float(power[a:b, c:d].sum()) / max(float((np.abs(noise[a:b, c:d]) ** 2).sum()), 1e-30))
    return _istft(noise * gain, len(ref)), len(fe) - 1, len(te) - 1


def section_copie():
    print("\n== copie: one sound redrawn with our own noise, finer and finer (invented snare family, 30 sounds)")
    refs = T.family("snare", 30, np.random.default_rng(31))
    with multiprocessing.Pool(4) as pool:
        prints = pool.map(T.fingerprint, refs)
        maps = pool.map(T.band_time_map, refs)
    prof = T.profile(prints)
    target = 7
    others = [i for i in range(len(refs)) if i != target]
    print("sound %d against the 29 others: nearest fingerprint %.2f, nearest band-time map %.2f dB, nearest stft %.3f" % (
        target, min(pair_distance(prints[target], prints[i], prof) for i in others),
        min(T.map_distance(maps[target], maps[i]) for i in others), min(mss(refs[target], refs[i]) for i in others)))
    own = sorted(min(pair_distance(p, q, prof) for j, q in enumerate(prints) if j != i) for i, p in enumerate(prints))
    own_map = sorted(min(T.map_distance(p, q) for j, q in enumerate(maps) if j != i) for i, p in enumerate(maps))
    print("the family, each sound against its nearest neighbour: fingerprint smallest %.2f, median %.2f; map smallest %.2f dB, median %.2f dB" % (
        own[0], own[len(own) // 2], own_map[0], own_map[len(own_map) // 2]))
    print("grid (bands x slices)\tnumbers taken\tshare of the samples\tfingerprint to the sound\tmap to the sound dB\tstft to the sound\tnull dB")
    used = int(np.sum(np.abs(refs[target]) > 1e-3 * np.max(np.abs(refs[target]))))

    def line(name, count, y):
        print("%s\t%d\t%.1f %%\t%.2f\t%.2f\t%.3f\t%.2f" % (
            name, count, 100.0 * count / used, pair_distance(T.fingerprint(y), prints[target], prof),
            T.map_distance(T.band_time_map(y), maps[target]), mss(y, refs[target]), T.null_db(refs[target], y)))

    for bands, slices in ((9, 6), (9, 12), (25, 12), (25, 24), (64, 48), (128, 96), (513, 100000)):
        y, nb, ns = redraw(refs[target], bands, slices, np.random.default_rng(5))
        line("%d x %d" % (nb, ns), nb * ns, y)
    y = refs[target] * 1.0
    y[1:] += 0.02 * y[:-1]
    line("the sound itself, a hair of filtering", used, y)

    print("\n== copie: a tonal sound (the body of one invented kick, under 3 kHz) rebuilt from a few modes")
    kicks = T.family("kick", 30, np.random.default_rng(33))

    def body(x):  # keep what lies under 3 kHz, at 6 kHz sampling
        return (np.fft.irfft(np.fft.rfft(x)[:len(x) // 16 + 1], len(x) // 8) / 8.0)[:4096]

    low = [body(k) for k in kicks]
    nulls = sorted(T.null_db(low[target], low[i]) for i in others)
    print("kick %d against the 29 others, null test: deepest %.1f dB, median %.1f dB" % (target, nulls[0], nulls[len(nulls) // 2]))
    print("modes\tnumbers taken\tnull against the kick dB\tnull against its nearest neighbour dB")
    near = min(others, key=lambda i: T.null_db(low[target], low[i]))
    for k in (1, 2, 3, 4, 6, 8, 16, 32):
        _, _, _, model = T.esprit(low[target], 2 * k, 6000.0, m=1400)
        print("%d\t%d\t%.1f\t%.1f" % (k, 4 * k, T.null_db(low[target], model), T.null_db(low[near], model)))


# --- 6. Every measure against a second road to the same number.

def _counter_one(x):
    return T.fingerprint(x), T.counter_fingerprint(x)


def section_contre():
    print("\n== contre: fingerprint against its counter-measure, sound by sound")
    recs = chosen()
    sounds = [y[:int(1.5 * SR) + T.onset_of(y)] for y in render(list(recs.values()), 4 * SR)]
    rng = np.random.default_rng(21)
    for kind in ("snare", "hat", "kick"):
        sounds += T.family(kind, 30, rng)
    with multiprocessing.Pool(4) as pool:
        pairs = pool.map(_counter_one, sounds)
    tol = {"centroid_hz": ("%", 5.0), "centroid_att_hz": ("%", 5.0), "centroid_tail_hz": ("%", 5.0),
           "t20_low_ms": ("%ms", 10.0), "t20_mid_ms": ("%ms", 10.0), "t20_high_ms": ("%ms", 10.0)}
    for k in T.ENV_KEYS:
        tol[k] = ("dB", 1.0)
    print("measure\tsounds\tagree\tshare\tmedian gap\tworst gap\tunit")
    for k, (unit, lim) in tol.items():
        gaps, both = [], 0
        for a, b in pairs:
            va, vb = a[k], b[k]
            if math.isnan(va) != math.isnan(vb):
                gaps.append(float("inf"))  # one road sees the measure, the other does not
                continue
            if math.isnan(va) or (unit == "dB" and va <= T.FLOOR_DB + 5.0 and vb <= T.FLOOR_DB + 5.0):
                continue
            both += 1
            if unit == "dB":
                gaps.append(abs(va - vb))
            elif unit == "%":
                gaps.append(abs(vb / va - 1.0) * 100.0)
            else:  # per cent, but never stricter than 1 ms
                gaps.append(0.0 if abs(va - vb) <= 1.0 else abs(vb / va - 1.0) * 100.0)
        ok = sum(g <= lim for g in gaps)
        fin = [g for g in gaps if math.isfinite(g)]
        print("%s\t%d\t%d\t%.1f %%\t%.2f\t%.2f\t%s (limit %.0f)" % (
            k, len(gaps), ok, 100.0 * ok / max(len(gaps), 1), float(np.median(fin)), max(fin), unit.replace("ms", ""), lim))


# --- 6b. The note of a kick, by two roads.

def _note_one(x):
    return T.note_hz(x), T.counter_note_hz(x)


def section_note():
    print("\n== note: the note of the low drums, two roads, against the note the recipe asks for")
    recs = chosen()
    cells = [(f, st) for f in ("kick", "808", "tom") for st in STYLES]
    sounds = list(render([recs[c] for c in cells], 4 * SR))
    asked = [recs[c][2] for c in cells]  # endHz: where the body's pitch lands
    rng = np.random.default_rng(41)
    invented = T.family("kick", 30, rng)
    hiss = [y + 10.0 ** (-40.0 / 20.0) * 0.8 * rng.standard_normal(len(y)) for y in invented]
    with multiprocessing.Pool(4) as pool:
        notes = pool.map(_note_one, sounds + invented + hiss)
    print("sound\tasked Hz\tnote Hz\tcounter Hz\tgap between the roads\ttail that is read")
    for c, want, (a, b) in zip(cells, asked, notes):
        y = sounds[cells.index(c)]
        low = T.band_signal(y, T._note_mask)
        on = T.onset_of(low)
        ms = (T.decay_index(low * low, on, T.NOTE_TO_DB) - T.decay_index(low * low, on, T.NOTE_FROM_DB)) * 1000.0 / SR
        print("%s %s\t%.1f\t%.2f\t%.2f\t%.2f %%\t%.0f ms = %.1f periods" % (
            c[0], c[1], want, a, b, abs(b / a - 1.0) * 100.0, ms, ms * want / 1000.0))
    for name, part in (("our 18 low drums", notes[:len(cells)]), ("30 invented kicks", notes[len(cells):len(cells) + 30]),
                       ("the same 30 under a hiss at -40 dB", notes[len(cells) + 30:])):
        seen = [(a, b) for a, b in part if not (math.isnan(a) or math.isnan(b))]
        lost = sum(math.isnan(a) != math.isnan(b) for a, b in part)
        none = sum(math.isnan(a) and math.isnan(b) for a, b in part)
        gaps = [abs(b / a - 1.0) * 100.0 for a, b in seen]
        ok = sum(g <= 3.0 for g in gaps)
        print("%s: a note on both roads for %d of %d, no note on both for %d, one road blind for %d; "
              "of the notes, %d agree within 3 %% (half a semitone), median gap %.2f %%, worst %.2f %%" % (
                  name, len(seen), len(part), none, lost, ok, float(np.median(gaps)), max(gaps)))
    off = [abs(a / want - 1.0) * 100.0 for want, (a, _) in zip(asked, notes) if not math.isnan(a)]
    print("our 18 low drums, note read against the note asked: median gap %.2f %%, worst %.2f %%" % (float(np.median(off)), max(off)))
    moved = [abs(h[0] / c[0] - 1.0) * 100.0 for c, h in zip(notes[len(cells):len(cells) + 30], notes[len(cells) + 30:])]
    print("30 invented kicks, the hiss moves the note by: median %.2f %%, worst %.2f %%" % (float(np.median(moved)), max(moved)))


# --- 7. A family with modes and band-by-band decays: what can today's voice reach?

VOICE_BOUNDS = (("lin", 0, 1), ("log", 30, 12000), ("log", 30, 12000), ("log", 1, 80), ("lin", 0, 300),
                ("log", 5, 1500), ("lin", 0, 1), ("lin", 0, 1), ("log", 20, 12000), ("log", 200, 20000),
                ("lin", 0, 300), ("log", 5, 3000), ("lin", 0, 1), ("log", 150, 1500), None, None,
                ("lin", 0, 1), ("log", 0.3, 10), ("log", 300, 12000))
N_MODES = 4
EXT_BOUNDS = ((("lin", 0, 1), ("log", 30, 12000), ("log", 30, 12000), ("log", 1, 80), ("log", 5, 600))
              + (("log", 60, 16000), ("lin", 0, 1), ("log", 5, 600)) * N_MODES
              + (("lin", -70, 0), ("log", 2, 600)) * 9
              + (("lin", 0, 1), ("log", 0.3, 10), ("log", 300, 12000)))


def to_unit(v, b):
    kind, lo, hi = b
    if kind == "log":
        return math.log(min(max(v, lo), hi) / lo) / math.log(hi / lo)
    return (min(max(v, lo), hi) - lo) / (hi - lo)


def from_unit(u, b):
    kind, lo, hi = b
    return lo * (hi / lo) ** u if kind == "log" else lo + (hi - lo) * u


class Extended:
    """Sketch of the richer voice, in numpy: today's swept body, four free modes, noise in
    nine octave bands that each have a level and a decay, a click. The noise is one fixed
    sequence, like the engine's."""

    def __init__(self, n):
        rng = np.random.default_rng(7)
        self.n = n
        self.t = np.arange(n) / SR
        spec = np.fft.rfft(rng.standard_normal(n))
        self.bands = []
        for i in range(9):
            w = np.fft.irfft(spec * T._smooth_band(n, T.OCT_EDGES[i], T.OCT_EDGES[i + 1]), n)
            self.bands.append(w / math.sqrt(float(np.mean(w * w))))
        self.click_spec = np.fft.rfft(rng.standard_normal(n))

    def render(self, u):
        p = [from_unit(v, b) for v, b in zip(u, EXT_BOUNDS)]
        t = self.t
        ms = t * 1000.0
        hz = p[2] + (p[1] - p[2]) * 0.5 ** (ms / p[3])
        y = p[0] * np.sin(2.0 * math.pi * np.cumsum(hz) / SR) * np.exp(-ms / p[4])
        at = 5
        for _ in range(N_MODES):
            y = y + p[at + 1] * np.sin(2.0 * math.pi * p[at] * t) * np.exp(-ms / p[at + 2])
            at += 3
        for i in range(9):
            if p[at] > -65.0:
                y = y + 10.0 ** (p[at] / 20.0) * self.bands[i] * np.exp(-ms / p[at + 1])
            at += 2
        if p[at] > 0.0:
            w = np.fft.irfft(self.click_spec * T._smooth_band(self.n, p[at + 2], 18000.0), self.n)
            y = y + p[at] * w / math.sqrt(float(np.mean(w * w))) * np.exp(-ms / (0.4 * p[at + 1]))
        peak = float(np.max(np.abs(y)))
        return y / peak * 0.8 if peak > 0 else y


_CTX = {}


def _score_sound(y):
    try:
        return T.score(T.fingerprint(y), _CTX["prof"], _CTX["keys"])
    except ValueError:
        return 10.0


def _score_ext(u):
    return _score_sound(_CTX["ext"].render(u))


def fit(kind, u0, prof, keys, iters, pop, seed, sigma, voice_template=None, frames=72000):
    """CMA-ES in the unit cube on the rule's own score. kind: 'voice' (engine) or 'ext'."""
    _CTX.update(prof=prof, keys=keys)
    if kind == "ext":
        _CTX["ext"] = Extended(frames)
    free = [i for i, b in enumerate(VOICE_BOUNDS) if b is not None]

    def recipe(u):
        r = list(voice_template)
        for v, i in zip(u, free):
            r[i] = from_unit(v, VOICE_BOUNDS[i])
        return r

    with multiprocessing.Pool(4) as pool:
        def loss(us):
            clipped = np.clip(us, 0.0, 1.0)
            pen = 10.0 * np.sum((us - clipped) ** 2, axis=1)
            if kind == "ext":
                s = pool.map(_score_ext, list(clipped))
            else:
                s = pool.map(_score_sound, list(render([recipe(u) for u in clipped], frames)))
            return np.asarray(s) + pen

        best, f = T.cma(loss, u0, sigma, iters, pop, np.random.default_rng(seed))
    best = np.clip(best, 0.0, 1.0)
    y = _CTX["ext"].render(best) if kind == "ext" else render([recipe(best)], frames)[0]
    return best, f, y


def report(name, y, prof, refs_prints, refs_maps):
    fp = T.fingerprint(y)
    d = T.distances(fp, prof)
    inside = sum(v <= 1.0 for v in d.values())
    worst = sorted(d.items(), key=lambda kv: -kv[1])[:3]
    near = min(pair_distance(fp, r, prof) for r in refs_prints)
    m = T.band_time_map(y)
    near_map = min(T.map_distance(m, r) for r in refs_maps)  # a finer measure the search never saw
    print("%s\tinside %d of %d\tscore %.2f\tnearest reference %.2f\tnearest map %.2f dB\tworst: %s" % (
        name, inside, len(d), sum(d.values()) / len(d), near, near_map, ", ".join("%s %.1f" % kv for kv in worst)))
    return fp


def pair_distance(a, b, prof):
    """Distance between two sounds, in the same unit as the score (half-widths of the family)."""
    tot, n = 0.0, 0
    for k, (_, p10, p90) in prof.items():
        if math.isnan(a[k]) or math.isnan(b[k]):
            continue
        tot += 2.0 * abs(a[k] - b[k]) / max(p90 - p10, 1e-9)
        n += 1
    return tot / max(n, 1)


def section_famille():
    print("\n== famille: an invented family (30 sounds, 3 invented packs), fitted by today's voice and by a richer one")
    recs = chosen()
    starts = {"snare": ("caisse_claire", "dnb"), "hat": ("charley_ferme", "techno"), "kick": ("kick", "techno")}
    for kind in ("snare", "hat", "kick"):
        refs = T.family(kind, 30, np.random.default_rng({"snare": 31, "hat": 32, "kick": 33}[kind]))
        with multiprocessing.Pool(4) as pool:
            prints = pool.map(T.fingerprint, refs)
            maps = pool.map(T.band_time_map, refs)
        prof = T.profile(prints)
        keys = tuple(prof.keys())
        print("\n-- %s: %d measures have a target" % (kind, len(keys)))
        own_map = sorted(min(T.map_distance(p, q) for j, q in enumerate(maps) if j != i) for i, p in enumerate(maps))
        print("references: nearest neighbour on the band-time map, smallest %.2f dB, median %.2f dB" % (own_map[0], own_map[len(own_map) // 2]))
        own = sorted(min(pair_distance(p, q, prof) for j, q in enumerate(prints) if j != i) for i, p in enumerate(prints))
        print("references: nearest neighbour inside the family, smallest %.2f, median %.2f" % (own[0], own[len(own) // 2]))
        inside = sorted(sum(v <= 1.0 for v in T.distances(p, prof).values()) for p in prints)
        print("references themselves: inside count, median %d of %d (smallest %d)" % (inside[len(inside) // 2], len(keys), inside[0]))

        base = recs[starts[kind]]
        free = [i for i, b in enumerate(VOICE_BOUNDS) if b is not None]
        u0 = np.array([to_unit(base[i], VOICE_BOUNDS[i]) for i in free])
        report("today, as chosen for %s %s" % starts[kind], render([base], 72000)[0], prof, prints, maps)
        _, _, y = fit("voice", u0, prof, T.LENGTH_KEYS, 60, 16, 1, 0.15, base)
        report("today's voice, lengths only", y, prof, prints, maps)
        best = None
        for seed in (1, 2, 3):
            u, f, y = fit("voice", u0, prof, keys, 150, 16, seed, 0.2, base)
            print("  run %d: score %.3f" % (seed, f))
            if best is None or f < best[1]:
                best = (u, f, y)
        report("today's voice, whole fingerprint", best[2], prof, prints, maps)
        among = min(T.null_db(refs[i], refs[j]) for i in range(len(refs)) for j in range(i))
        print("null test of the references among themselves: deepest %.2f dB" % among)
        print("null test of today's voice against each of the 30 references: deepest %.2f dB" % min(
            T.null_db(ref, best[2]) for ref in refs))
        fitted = [from_unit(v, VOICE_BOUNDS[i]) for v, i in zip(best[0], free)]

        # The richer voice starts from the family's medians (band levels, band decays), not from any one sound.
        r = dict(zip(free, fitted))
        e0 = [to_unit(r[0], EXT_BOUNDS[0]), to_unit(r[1], EXT_BOUNDS[1]), to_unit(r[2], EXT_BOUNDS[2]),
              to_unit(max(r[3], 1.0), EXT_BOUNDS[3]), to_unit(max(r[5], 5.0) / math.log(100.0), EXT_BOUNDS[4])]
        guess = [r[2] * m for m in (1.6, 2.3, 3.3, 4.8)] if r[0] > 0.05 else [3000.0, 5000.0, 7500.0, 11000.0]
        for hz in guess:
            e0 += [to_unit(hz, EXT_BOUNDS[5]), 0.1, to_unit(40.0, EXT_BOUNDS[7])]
        group = [prof.get("t20_%s_ms" % g, prof["t20_ms"])[0] / math.log(10.0) for g in ("low", "mid", "high")]
        taus = [group[0]] * 3 + [group[1]] * 3 + [group[2]] * 3
        amps = [math.sqrt(10.0 ** (prof[k][0] / 10.0) / tau) if k in prof else 0.0 for k, tau in zip(T.ENV_KEYS, taus)]
        for a, tau in zip(amps, taus):
            level = 20.0 * math.log10(a / max(amps)) if a > 0 else -70.0
            e0 += [to_unit(level, EXT_BOUNDS[17]), to_unit(tau, EXT_BOUNDS[18])]
        e0 += [to_unit(r[16], EXT_BOUNDS[-3]), to_unit(r[17], EXT_BOUNDS[-2]), to_unit(r[18], EXT_BOUNDS[-1])]
        _CTX.update(prof=prof, keys=keys, ext=Extended(72000))
        report("richer voice, straight from the medians (no search)", _CTX["ext"].render(e0), prof, prints, maps)
        best = None
        for seed in (1, 2):
            u, f, y = fit("ext", np.array(e0), prof, keys, 200, 20, seed, 0.1)
            print("  run %d: score %.3f" % (seed, f))
            if best is None or f < best[1]:
                best = (u, f, y)
        report("richer voice, after search", best[2], prof, prints, maps)
        worst_null = min(T.null_db(ref, best[2]) for ref in refs)
        print("null test of the richer voice against each of the 30 references: deepest %.2f dB" % worst_null)
        clone = refs[7] * 1.0
        clone[1:] += 0.02 * clone[:-1]  # a reference with a hair of filtering: what a copy looks like
        fp = T.fingerprint(clone)
        print("a disguised copy of reference 7: nearest reference %.3f, deepest null %.1f dB" % (
            min(pair_distance(fp, q, prof) for q in prints), min(T.null_db(ref, clone) for ref in refs)))


SECTIONS = {"cibles": section_cibles, "aveugle": section_aveugle, "bruit": section_bruit,
            "modes": section_modes, "copie": section_copie, "contre": section_contre, "note": section_note,
            "famille": section_famille}

if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    BIN = sys.argv[1]
    for name in (sys.argv[2:] or list(SECTIONS)):
        SECTIONS[name]()
