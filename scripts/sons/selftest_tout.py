#!/usr/bin/env python3
"""Self-test of tout.py on synthetic signals whose truth is known.

    python3 -I scripts/sons/selftest_tout.py          run every check
    python3 -I scripts/sons/selftest_tout.py --app    print how BenStudio's own renders go through tout.py

Every measured value must sit within its tolerance of the value the signal
was built with. Exit code 1 when a single check fails. No sample is needed:
all signals and MIDI files are computed here.
"""
import csv
import math
import os
import struct
import sys
import tempfile

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tout  # noqa: E402

FAILS = []
COUNT = [0]
SR = 48000

APP_DOC = """\
App comparison mode (nothing is rendered by this script)
--------------------------------------------------------
Goal: BenStudio's own sounds measured by the very same tool, so each pack
table has an "app" counterpart and the gap, style by style, is the work list.

1. Render the songs as the phone plays them, one wav per style, whole mix and
   basses alone:
       scripts/live-bench.sh --styles techno,house --out build/live-bench
   (ps_live writes <out>/log/<style>.csv; add `--wav <file>` to a ps_live call
   to keep the audio, and `--solo bass` for the basses alone).
2. Lay the renders out like a pack, outside the repository; the folder words
   are what tout.py classifies on, the bpm and key go in the name:
       <dir>/Drum Loops/<style>/<style> drums 124 BPM.wav
       <dir>/Loops/<style>/<style> bass 124 BPM Am.wav
       <dir>/Construction Kits/<style> 124 BPM Am/<role>.wav   (one stem per role)
       <dir>/One Shots/<style>/<role> C3.wav                    (one note per preset)
   Cut drum loops and stems to whole bars (the bar count is read from the length).
3. Measure:
       python3 -I scripts/sons/tout.py <dir> --rar app --out <prefix>
   The rar column then reads "app": the app rows add up with the pack rows
   (same columns, same accuracy header), and a summary compares them per
   type x style.
"""


def check(name, got, want, tol, relative=False):
    """|got - want| <= tol (or tol * |want| when relative)."""
    COUNT[0] += 1
    limit = tol * abs(want) if relative else tol
    ok = got is not None and isinstance(got, (int, float)) and math.isfinite(got) and abs(got - want) <= limit
    if not ok:
        FAILS.append(name)
    shown = got if isinstance(got, (int, float)) and got is not None else float("nan")
    print("%s %-50s got %10.4f  want %10.4f  +-%.4g" % ("ok  " if ok else "FAIL", name, shown, want, limit))


def check_eq(name, got, want):
    COUNT[0] += 1
    ok = got == want
    if not ok:
        FAILS.append(name)
    print("%s %-50s got %r  want %r" % ("ok  " if ok else "FAIL", name, got, want))


def check_true(name, ok):
    COUNT[0] += 1
    if not ok:
        FAILS.append(name)
    print("%s %s" % ("ok  " if ok else "FAIL", name))


def t_axis(seconds, sr=SR):
    return np.arange(int(round(seconds * sr))) / sr


def band_noise(n, lo, hi, seed, sr=SR):
    rng = np.random.default_rng(seed)
    f = np.fft.rfftfreq(n, 1.0 / sr)
    spec = np.where((f >= lo) & (f <= hi), np.exp(2j * np.pi * rng.random(len(f))), 0.0)
    x = np.fft.irfft(spec, n)
    return x / (np.max(np.abs(x)) + 1e-12)


def harmonic(f0, t, n_harm=6, decay=0.7):
    """A tone with harmonics k*f0 at amplitude decay**(k-1)."""
    x = np.zeros_like(t)
    for k in range(1, n_harm + 1):
        if k * f0 < 0.45 * SR:
            x += decay ** (k - 1) * np.sin(2 * np.pi * k * f0 * t)
    return x


def adsr(t, a, d, s, hold_end, r):
    env = np.zeros_like(t)
    env = np.where(t < a, t / a, env)
    env = np.where((t >= a) & (t < a + d), 1.0 - (1.0 - s) * (t - a) / d, env)
    env = np.where((t >= a + d) & (t < hold_end), s, env)
    env = np.where((t >= hold_end) & (t < hold_end + r), s * (1.0 - (t - hold_end) / r), env)
    return env


def mono(x):
    return x.reshape(-1, 1)


def wav_bytes(x, sr=SR):
    """32-bit float wav of a [frames, channels] array."""
    x = np.asarray(x, dtype="<f4")
    if x.ndim == 1:
        x = x.reshape(-1, 1)
    ch = x.shape[1]
    data = x.tobytes()
    fmt = struct.pack("<HHIIHH", 3, ch, sr, sr * ch * 4, ch * 4, 32)
    body = b"WAVE" + b"fmt " + struct.pack("<I", len(fmt)) + fmt + b"data" + struct.pack("<I", len(data)) + data
    return b"RIFF" + struct.pack("<I", len(body)) + body


def vlq(n):
    out = [n & 0x7F]
    n >>= 7
    while n:
        out.append(0x80 | (n & 0x7F))
        n >>= 7
    return bytes(reversed(out))


def midi_bytes(notes, tpq=480, bpm=120.0, tracks_split=False):
    """notes: (start_tick, dur_tick, pitch, velocity, channel). Format 1 with a tempo track."""
    tempo = struct.pack(">I", int(round(60e6 / bpm)))[1:]
    t0 = vlq(0) + b"\xFF\x51\x03" + tempo + vlq(0) + b"\xFF\x58\x04\x04\x02\x18\x08" + vlq(0) + b"\xFF\x2F\x00"
    ev = []
    for s, d, p, v, ch in notes:
        ev.append((s, 1, bytes([0x90 | ch, p, v])))
        ev.append((s + d, 0, bytes([0x80 | ch, p, 0])))
    ev.sort(key=lambda e: (e[0], e[1]))
    trk, last, prev_status = b"", 0, None
    for tick, _, msg in ev:
        status = msg[0]
        body = msg[1:] if status == prev_status else msg  # running status
        trk += vlq(tick - last) + body
        last, prev_status = tick, status
    trk += vlq(0) + b"\xFF\x2F\x00"
    out = b"MThd" + struct.pack(">IHHH", 6, 1, 2, tpq)
    out += b"MTrk" + struct.pack(">I", len(t0)) + t0
    out += b"MTrk" + struct.pack(">I", len(trk)) + trk
    return out


# ------------------------------------------------------------------ tests

def test_tags():
    tg = tout.parse_tags(["Bass Loop 124 BPM Am.wav"], "loop")
    check_eq("tag bpm from '124 BPM'", tg["bpm"], 124.0)
    check_eq("tag key 'Am' = A minor", tg["key"], (9, "minor"))
    tg = tout.parse_tags(["Lead_128_F#min_03.wav"], "loop")
    check_eq("tag bare bpm next to a key", tg["bpm"], 128.0)
    check_eq("tag key F#min", tg["key"], (6, "minor"))
    tg = tout.parse_tags(["Kit B Version.wav"], "loop")
    check_eq("tag lone letter without bpm is not a key", tg["key"], None)
    tg = tout.parse_tags(["Pluck Shot C#3.wav"], "oneshot_melodic")
    check_eq("tag note C#3", tg["note"], 1)
    tg = tout.parse_tags(["Bb Bass.wav"], "oneshot_melodic")
    check_eq("tag note Bb", tg["note"], 10)
    tg = tout.parse_tags(["Snare 64.wav"], "drum_oneshot")
    check_eq("tag no bpm on a one-shot number", tg["bpm"], None)
    # blind review cases
    for name, typ, field, want in [
            ("Loop_A_Minor_90.wav", "melodic_loop", "key", (9, "minor")),
            ("Lead_128_AMIN.wav", "melodic_loop", "key", (9, "minor")),
            ("C sharp minor 120.wav", "melodic_loop", "key", (1, "minor")),
            ("Cm7 Chord Loop 90.wav", "melodic_loop", "key", (0, "minor")),
            ("Gmaj7 Keys 100.wav", "melodic_loop", "key", (7, "major")),
            ("Strings 128 Dbmin.wav", "melodic_loop", "key", (1, "minor")),
            ("FM_Bass_120.wav", "melodic_loop", "key", None),
            ("Kit B 120.wav", "drum_loop", "key", None),
            ("Loop 120 ab.wav", "melodic_loop", "key", None),
            ("Loop_073.wav", "drum_loop", "bpm", None),
            ("90s Keys Loop.wav", "melodic_loop", "bpm", None),
            ("808 D Sharp.wav", "drum_oneshot", "note", 3),
            ("Kit B Kick.wav", "drum_oneshot", "note", None),
            ("Bass_ab_C3.wav", "oneshot_melodic", "note", 0)]:
        check_eq("tag %s %s" % (field, name), tout.parse_tags([name], typ)[field], want)


def test_classify():
    c = tout.classify("Pack/Drum Loops/Techno/Top Loop 130 BPM.wav", 7.4)
    check_eq("class drum loop", (c["type"], c["style"]), ("drum_loop", "techno"))
    c = tout.classify("Pack/Loops/Synth Loop 124 BPM Am.wav", 7.7)
    check_eq("class melodic loop", c["type"], "melodic_loop")
    c = tout.classify("Pack/One Shots/Kicks/Kick 01.wav", 0.5)
    check_eq("class drum one-shot", (c["type"], c["subtype"]), ("drum_oneshot", "kick"))
    c = tout.classify("Pack/One Shots/808s/808 Long C.wav", 2.0)
    check_eq("class 808 kept as drum one-shot", (c["type"], c["subtype"]), ("drum_oneshot", "kick_808"))
    c = tout.classify("Pack/One Shots/Bass/Bass Shot C.wav", 1.0)
    check_eq("class bass one-shot kept as melodic", (c["type"], c["role"]), ("oneshot_melodic", "bass"))
    c = tout.classify("Pack/One Shots/Kicks/Kick 03 Attack.wav", 0.1)
    check_eq("class attack layer kept apart", c["subtype"], "kick:attack")
    c = tout.classify("Pack/Vocals/Vox Chop 01.wav", 1.0)
    check_eq("class vocal", c["type"], "vocal")
    c = tout.classify("Pack/FX/Riser 03.wav", 4.0)
    check_eq("class fx", c["type"], "fx")
    c = tout.classify("Pack/MIDI/Chords 124 Am.mid", None)
    check_eq("class midi", c["type"], "midi")
    c = tout.classify("Pack/Construction Kits/Kit 01 124 BPM Am/Stems/Bass.wav", 8.0)
    check_eq("class kit stem", (c["type"], c["role"]), ("kit_stem", "bass"))


def test_common():
    t = t_axis(2.0)
    x = 0.1 * np.sin(2 * np.pi * 1000 * t)
    c = tout.common(SR, mono(x))
    check("lufs 1 kHz sine at amplitude 0.1 (BS.1770: -23.01)", c["lufs_i"], -23.01, 0.1)
    check("peak dbfs", c["peak_dbfs"], -20.0, 0.01)
    check("rms dbfs", c["rms_dbfs"], -23.01, 0.01)
    c = tout.common(SR, np.stack([x, x], axis=1))
    check("lufs stereo sums the channels (+3 dB)", c["lufs_i"], -20.0, 0.1)
    c = tout.common(44100, mono(0.1 * np.sin(2 * np.pi * 1000 * t_axis(2.0, 44100))))
    check("lufs at 44.1 kHz", c["lufs_i"], -23.01, 0.1)


def test_notes():
    for f, name in ((41.2034, "E1"), (110.0, "A2"), (440.0, "A4"), (1046.502, "C6")):
        t = t_axis(1.0)
        x = 0.5 * harmonic(f, t) * adsr(t, 0.005, 0.1, 0.6, 0.8, 0.15)
        m = tout.measure_oneshot(SR, mono(x), {"note": None})
        check("note %s f0" % name, m["f0_hz"], f, 0.005, relative=True)
        check_eq("note %s name" % name, m["note_measured"], name)
    t = t_axis(1.0)
    f = 440.0 * 2 ** (25 / 1200.0)
    x = 0.5 * harmonic(f, t) * adsr(t, 0.005, 0.1, 0.6, 0.8, 0.15)
    m = tout.measure_oneshot(SR, mono(x), {"note": 9})
    check("note cents gap against tag A (+25 c)", m["cents_gap"], 25.0, 3.0)
    m = tout.measure_oneshot(SR, mono(x), {"note": 8})
    check("note cents gap against tag G# (+125 c)", m["cents_gap"], 125.0, 3.0)
    # missing fundamental: harmonics 2..6 of 110 Hz still read as A2
    x = sum(0.3 * np.sin(2 * np.pi * k * 110 * t) for k in range(2, 7)) * adsr(t, 0.005, 0.1, 0.6, 0.8, 0.15)
    m = tout.measure_oneshot(SR, mono(x), {"note": None})
    check_eq("note missing fundamental A2", m["note_measured"], "A2")


def test_adsr():
    t = t_axis(1.4)
    a, d, s, hold, r = 0.020, 0.100, 0.5, 0.8, 0.300
    x = 0.5 * harmonic(220.0, t) * adsr(t, a, d, s, hold, r)
    m = tout.measure_oneshot(SR, mono(x), {"note": 9})
    check("adsr attack 10-90 % (0.8 A)", m["attack_ms"], 0.8 * a * 1000, 0.15, relative=True)
    check("adsr decay to 90 % of the way (0.9 D)", m["decay_ms"], 0.9 * d * 1000, 0.15, relative=True)
    check("adsr sustain level dB", m["sustain_db"], 20 * math.log10(s), 0.5)
    check("adsr release 90-10 % of sustain (0.8 R)", m["release_ms"], 0.8 * r * 1000, 0.15, relative=True)
    check_eq("adsr held note is loopable", m["loopable"], 1)
    check("harmonic ratio of a harmonic tone", m["harmonic_ratio"], 1.0, 0.05)
    check("stereo width of a mono file", m["stereo_width"], 0.0, 0.01)
    pluck = 0.5 * harmonic(220.0, t) * np.exp(-t / 0.15) * np.minimum(t / 0.002, 1)
    m = tout.measure_oneshot(SR, mono(pluck), {"note": 9})
    check_eq("pluck is not loopable", m["loopable"], 0)
    check_true("pluck has no sustain (< -30 dB)", m["sustain_db"] is not None and m["sustain_db"] < -30)
    # brightness falling: harmonics fade faster than the fundamental
    bright = sum(np.exp(-t * k * 4.0) * np.sin(2 * np.pi * k * 220 * t) / k for k in range(1, 12))
    m = tout.measure_oneshot(SR, mono(0.4 * bright), {"note": 9})
    check_true("centroid start > mid > end on a darkening tone",
               m["centroid_start_hz"] > m["centroid_mid_hz"] > m["centroid_end_hz"])
    n = len(t)
    st = np.stack([band_noise(n, 200, 8000, 1), band_noise(n, 200, 8000, 2)], axis=1) * 0.3
    m = tout.measure_oneshot(SR, st, {"note": None})
    check("stereo width of uncorrelated L/R noise", m["stereo_width"], 0.5, 0.05)
    check_true("harmonic ratio of noise is low (< 0.4)", m["harmonic_ratio"] is None or m["harmonic_ratio"] < 0.4)


def kick(t, f_hi=120.0, f_lo=50.0, tau=0.03):
    f = f_lo + (f_hi - f_lo) * np.exp(-t / tau)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t / 0.12)


def drum_loop(bpm, bars, swing, kick_steps, snare_steps, hat_steps, seed=7):
    step = 60.0 / bpm / 4
    n = int(round(bars * 16 * step * SR))
    x = np.zeros(n)
    hat = band_noise(int(0.08 * SR), 8000, 16000, seed) * np.exp(-np.arange(int(0.08 * SR)) / SR / 0.012)
    sn_n = int(0.25 * SR)
    ts = np.arange(sn_n) / SR
    snare = (0.7 * band_noise(sn_n, 1000, 5000, seed + 1) + 0.5 * np.sin(2 * np.pi * 200 * ts)) * np.exp(-ts / 0.06)
    kn = int(0.4 * SR)
    kk = kick(np.arange(kn) / SR)

    def put(sig, s, gain):
        when = (s // 2) * 2 * step + (s % 2) * 2 * step * swing
        i = int(round(when * SR))
        e = min(n, i + len(sig))
        x[i:e] += gain * sig[:e - i]

    for b in range(bars):
        for s in kick_steps:
            put(kk, b * 16 + s, 0.8)
        for s in snare_steps:
            put(snare, b * 16 + s, 0.5)
        for s in hat_steps:
            put(hat, b * 16 + s, 0.25 if s % 2 else 0.3)
    return x


def test_drum_loop():
    ksteps, ssteps, hsteps = (0, 4, 8, 10, 12), (4, 12), tuple(range(16))
    for bpm, bars, swing in ((124.0, 2, 0.62), (140.0, 4, 0.5), (90.0, 2, 0.58)):
        x = drum_loop(bpm, bars, swing, ksteps, ssteps, hsteps)
        m = tout.measure_drum_loop(SR, mono(x), {"bpm": None})
        tag = "%g bpm %d bars" % (bpm, bars)
        check("drum loop %s: bpm" % tag, m["bpm_measured"], bpm, 0.005, relative=True)
        check("drum loop %s: bars" % tag, m["bars"], bars, 0)
        check_eq("drum loop %s: kick grid" % tag, m["grid_kick"], "x...x...x.x.x...")
        check_eq("drum loop %s: snare grid" % tag, m["grid_snare"], "....x.......x...")
        check_eq("drum loop %s: hats grid" % tag, m["grid_hats"], "x" * 16)
        check("drum loop %s: swing %%" % tag, m["swing_pct"], swing * 100, 2.0)
    x = drum_loop(124.0, 2, 0.5, ksteps, ssteps, hsteps)
    m = tout.measure_drum_loop(SR, mono(x), {"bpm": 124.0})
    check("drum loop bpm gap against tag", m["bpm_gap_pct"], 0.0, 0.5)
    check("drum loop density (onsets per beat)", m["density_per_beat"], 4.0, 0.5)
    # half-time trap: hats on 8ths, kick and snare sparse, 140 bpm
    x = drum_loop(140.0, 2, 0.5, (0, 7, 10), (8,), (0, 2, 4, 6, 8, 10, 12, 14))
    m = tout.measure_drum_loop(SR, mono(x), {"bpm": None})
    check("half-time drum loop bpm (140)", m["bpm_measured"], 140.0, 0.005, relative=True)
    check_eq("half-time snare on step 9 only", m["grid_snare"], "........x.......")


def chord_loop(bpm, chords, bars_per_chord=1, n_harm=5):
    """chords: list of lists of midi pitches, one per bar; a root bass an octave down."""
    bar = 240.0 / bpm
    out = []
    for ch in chords:
        t = t_axis(bar * bars_per_chord)
        env = np.minimum(t / 0.01, 1.0) * np.minimum((bar * bars_per_chord - t) / 0.02, 1.0)
        x = np.zeros_like(t)
        for p in ch:
            x += harmonic(440.0 * 2 ** ((p - 69) / 12.0), t, n_harm, 0.6)
        x += 1.2 * harmonic(440.0 * 2 ** ((ch[0] - 12 - 69) / 12.0), t, 3, 0.5)
        out.append(0.1 * x * env)
    return np.concatenate(out)


def test_key():
    am = [[57, 60, 64], [53, 57, 60], [50, 53, 57], [52, 56, 59]]   # i VI iv V in A minor
    x = chord_loop(120.0, am * 2)
    m = tout.measure_melodic_loop(SR, mono(x), {"bpm": 120.0, "key": (9, "minor")})
    check_eq("chord loop A minor: key", m["key_measured"], "A minor")
    check_eq("chord loop A minor: key matches tag", m["key_match"], "exact")
    check("chord loop A minor: bpm", m["bpm_measured"], 120.0, 0.01, relative=True)
    check("chord loop A minor: bars", m["bars"], 8, 0)
    eb = [[63, 67, 70], [68, 72, 75], [70, 74, 77], [63, 67, 70]]   # I IV V I in Eb major
    x = chord_loop(100.0, eb * 2)
    m = tout.measure_melodic_loop(SR, mono(x), {"bpm": None, "key": (0, "minor")})
    check_eq("chord loop Eb major: key", m["key_measured"], "D# major")
    check_eq("chord loop Eb major: tag C minor = relative", m["key_match"], "relative")
    check_true("band energies sum to the whole (shares in dB)",
               abs(sum(10 ** (m[k] / 10) for k in ("low_db", "mid_db", "high_db")) - 1.0) < 0.02)


def test_midi():
    tpq = 480
    bar = 4 * tpq
    prog = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]   # Am F C G
    notes = []
    for b, ch in enumerate(prog * 2):
        for p in ch:
            notes.append((b * bar, bar, p, 90 + (p % 3) * 5, 0))
    m = tout.measure_midi_bytes(midi_bytes(notes, tpq, 124.0), {"bpm": None, "key": (9, "minor")})
    check("midi bars", m["bars"], 8, 0)
    check("midi notes", m["notes"], 24, 0)
    check("midi notes per bar", m["notes_per_bar"], 3.0, 0.001)
    check("midi pitch min", m["pitch_min"], 48, 0)
    check("midi pitch max", m["pitch_max"], 64, 0)
    check("midi poly max", m["poly_max"], 3, 0)
    check("midi bpm from tempo meta", m["bpm_file"], 124.0, 0.01)
    vels = [n[3] for n in notes]
    check("midi velocity mean", m["vel_mean"], float(np.mean(vels)), 0.01)
    check("midi velocity sd", m["vel_sd"], float(np.std(vels)), 0.01)
    check_eq("midi key (Am F C G, tagged Am)", m["key_measured"], "A minor")
    check_eq("midi chords per bar vs tagged A minor", m["chords"], "i-VI-III-VII-i-VI-III-VII")
    check("midi chord changes per bar", m["chord_changes_per_bar"], 7 / 8.0, 0.01)
    check("midi chord length in beats", m["chord_len_beats"], 4.0, 0.01)
    # swung 16th hats on channel 10 + a swung bass line
    notes = []
    for b in range(4):
        for s in range(16):
            tick = b * bar + (s // 2) * (tpq // 2) + (s % 2) * int(round(tpq // 2 * 0.6))
            notes.append((tick, 40, 42, 80, 9))
    m = tout.measure_midi_bytes(midi_bytes(notes, tpq, 128.0), {"bpm": None, "key": None})
    check("midi swing of 16ths (60 %)", m["swing_pct"], 60.0, 0.5)
    check_eq("midi drum track flagged", m["drum_track"], 1)
    check("midi drum bars", m["bars"], 4, 0)
    notes = [(i * tpq // 2, tpq // 2, p, 100, 0) for i, p in enumerate([57, 59, 60, 62, 64, 65, 67, 69] * 2)]
    m = tout.measure_midi_bytes(midi_bytes(notes, tpq, 120.0), {"bpm": 120.0, "key": None})
    check("midi straight 8ths: scale fit of A minor scale", m["scale_fit"], 1.0, 0.001)
    check("midi straight 8ths: poly max 1", m["poly_max"], 1, 0)
    check_true("midi garbage is refused cleanly",
               _raises(lambda: tout.measure_midi_bytes(b"MThd\x00\x00\x00\x06garbage", {})))


def _raises(fn):
    try:
        fn()
    except tout.BadAudio:
        return True
    return False


def test_vocal():
    parts = []
    for i, f in enumerate((220.0, 330.0, 220.0, 247.0)):
        t = t_axis(0.4)
        vib = f * (1 + 0.01 * np.sin(2 * np.pi * 5 * t))
        ph = 2 * np.pi * np.cumsum(vib) / SR
        tone = sum(0.6 ** (k - 1) * np.sin(k * ph) for k in range(1, 6))
        env = np.minimum(t / 0.01, 1) * np.minimum((0.4 - t) / 0.01, 1)
        parts.append(0.3 * tone * env)
        parts.append(np.zeros(int(0.5 * SR)))
    dry = np.concatenate(parts)
    m = tout.measure_vocal(SR, mono(dry), {})
    check_eq("vocal dry is dry", m["wet"], "dry")
    check("vocal median pitch (A3 220 Hz)", m["pitch_median_hz"], 220.0, 0.02, relative=True)
    check("vocal range in semitones (A3..E4)", m["range_st"], 7.0, 1.0)
    n = int(1.5 * SR)
    ir = band_noise(n, 100, 8000, 9) * np.exp(-np.arange(n) / SR * 6.9 / 1.5)
    wet_tail = np.fft.irfft(np.fft.rfft(dry, len(dry) + n) * np.fft.rfft(ir, len(dry) + n), len(dry) + n)
    wet = np.concatenate([dry, np.zeros(n)]) + 0.5 * wet_tail / np.max(np.abs(wet_tail)) * np.max(np.abs(dry))
    m = tout.measure_vocal(SR, mono(wet), {})
    check_eq("vocal with 1.5 s reverb is wet", m["wet"], "wet")


def test_fx():
    dur = 4.0
    n = int(dur * SR)
    t = np.arange(n) / SR
    noise = np.random.default_rng(3).standard_normal(n)
    chunks = []
    seg = n // 32
    for i in range(32):
        cut = 500.0 * (12000.0 / 500.0) ** (i / 31.0)
        c = noise[i * seg:(i + 1) * seg]
        f = np.fft.rfftfreq(len(c), 1.0 / SR)
        chunks.append(np.fft.irfft(np.fft.rfft(c) * (f < cut), len(c)))
    riser = np.concatenate(chunks) * 10 ** ((-40 + 40 * t[:seg * 32] / dur) / 20)
    m = tout.measure_fx(SR, mono(0.3 * riser / np.max(np.abs(riser))), {"bpm": 120.0})
    check_eq("riser level shape", m["level_shape"], "rising")
    check_eq("riser brightness shape", m["bright_shape"], "rising")
    check("riser length in beats at 120 bpm", m["length_beats"], 8.0, 0.1)
    check_true("riser peak late (> 0.8)", m["peak_pos"] > 0.8)
    impact = noise * np.exp(-t / 0.4)
    m = tout.measure_fx(SR, mono(0.5 * impact / np.max(np.abs(impact))), {"bpm": None})
    check_eq("impact level shape", m["level_shape"], "falling")
    check_eq("impact length in beats without bpm is empty", m["length_beats"], None)
    m = tout.measure_fx(SR, mono(0.2 * noise), {"bpm": None})
    check_eq("steady noise level shape", m["level_shape"], "flat")
    check_eq("steady noise brightness shape", m["bright_shape"], "flat")


def test_drum_oneshots():
    n = int(1.5 * SR)
    t = np.arange(n) / SR
    closed = band_noise(n, 7000, 16000, 4) * np.exp(-t / 0.015)
    opened = band_noise(n, 7000, 16000, 5) * np.exp(-t / 0.25)
    m = tout.measure_drum_oneshot(SR, mono(0.5 * closed), "hat")
    check_eq("closed hat measured closed", m["hat_measured"], "closed")
    m = tout.measure_drum_oneshot(SR, mono(0.5 * opened), "hat")
    check_eq("open hat measured open", m["hat_measured"], "open")
    tau = 0.03
    k = kick(t, 200.0, 50.0, tau)
    m = tout.measure_drum_oneshot(SR, mono(0.9 * k), "kick")
    check("kick drop end pitch", m["drop_end_hz"], 50.0, 0.04, relative=True)
    check("kick drop t50 (tau ln2)", m["drop_t50_ms"], 1000 * tau * math.log(2), 0.25, relative=True)
    check("kick drop t90 (tau ln10)", m["drop_t90_ms"], 1000 * tau * math.log(10), 0.25, relative=True)
    check_eq("kick drop shape exponential", m["drop_shape"], "exp")
    check_eq("kick steady note G1 (49 Hz) or G#1", m["note_measured"] in ("G1", "G#1"), True)
    f = np.where(t < 0.06, 200.0 - 150.0 * t / 0.06, 50.0)
    lin = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.2)
    m = tout.measure_drum_oneshot(SR, mono(0.9 * lin), "kick")
    check_eq("kick linear drop shape", m["drop_shape"], "lin")
    check("kick linear drop t90 (0.9 x 60 ms)", m["drop_t90_ms"], 54.0, 0.25, relative=True)


def _fade_out(x, ms, sr=SR):
    """Linear fade over the last ms milliseconds."""
    n = int(round(ms * sr / 1000.0))
    y = x.copy()
    y[-n:] *= np.linspace(1.0, 0.0, n)
    return y


def test_end_cut():
    """end_cut flags a click or an abrupt stop, never a trimmed-and-faded tail."""
    fn = getattr(tout, "end_cut", None)
    check_true("end_cut function exists", callable(fn))
    if not callable(fn):
        return
    t = t_axis(0.25)
    k = _fade_out(0.9 * kick(t, 120.0, 50.0, 0.15), 20.0)
    m = fn(SR, mono(k))
    check_eq("short kick with a 20 ms fade: not cut", m["end_cut"], 0)
    check_true("short kick fade length 8-25 ms", m["end_fade_ms"] is not None and 8 <= m["end_fade_ms"] <= 25)
    rng = np.random.default_rng(3)
    nz = rng.standard_normal(SR // 2)
    nz *= 10 ** (-10 / 20) / np.sqrt(np.mean(nz ** 2))
    m = fn(SR, mono(nz))
    check_eq("abrupt stop at -10 dBFS: cut", m["end_cut"], 1)
    check_true("abrupt stop: no fade", m["end_fade_ms"] is None)
    t = t_axis(3.43)
    sus = 0.7 * np.sin(2 * math.pi * 49.0 * t)
    m = fn(SR, mono(_fade_out(sus, 20.0)))
    check_eq("sustained 808 sine with a 20 ms fade: not cut", m["end_cut"], 0)
    check_true("808 fade length 10-25 ms", m["end_fade_ms"] is not None and 10 <= m["end_fade_ms"] <= 25)
    n_mid = int(round((3.0 + 0.3 / 49.0) * SR))  # stops at 0.3 of a cycle, near the crest
    m = fn(SR, mono(sus[:n_mid]))
    check_eq("sustained 808 cut mid-cycle: cut", m["end_cut"], 1)
    t = t_axis(1.0)
    ring = 0.01 * np.sin(2 * math.pi * 440.0 * t)
    m = fn(SR, mono(_fade_out(ring, 30.0)))
    check_eq("rings to the end at -40 dB, 30 ms fade: not cut", m["end_cut"], 0)
    check_true("ring fade length 15-35 ms", m["end_fade_ms"] is not None and 15 <= m["end_fade_ms"] <= 35)
    m = fn(SR, np.zeros((SR // 2, 2)))
    check_eq("pure silence: not cut", m["end_cut"], 0)
    check_true("pure silence: no fade", m["end_fade_ms"] is None)
    st = np.stack([_fade_out(sus, 20.0), sus], axis=1)
    m = fn(SR, st[:n_mid])
    check_eq("stereo: one channel cut is a cut", m["end_cut"], 1)
    td = t_axis(0.5)
    decayed = 0.9 * kick(td, 120.0, 50.0, 0.03) * np.exp(-td / 0.02)
    m = fn(SR, mono(decayed))
    check_eq("kick fully decayed before the end: not cut", m["end_cut"], 0)
    m = tout.measure_drum_oneshot(SR, mono(k), "kick")
    check_true("drum one-shot keeps end_level_db as information", "end_level_db" in m)
    for typ, cols in tout.TYPE_COLUMNS.items():
        if typ in ("midi", "kit"):
            continue
        check_true("%s table has end_cut and end_fade_ms" % typ, "end_cut" in cols and "end_fade_ms" in cols)
    check_true("no column doc calls an unfaded tail 'cut'",
               "cut short" not in tout.mesure.__doc__ and "cut tail" not in (tout.__doc__ or ""))


def test_blocking():
    """Blind audit B1-B4 and I3 (docs/sons/AUDIT-TOUT.md)."""
    for rel, dur, want in [
            ("Vocals/Dry/Take 01.wav", 4.0, "vocal"),
            ("Vocals/Adlibs/Hey 01.wav", 1.0, "vocal"),
            ("Vocal Loops/Dry/Hook 120 Am.wav", 8.0, "vocal"),
            ("Drum Loops/Tops/Top 128 01.wav", 7.5, "drum_loop"),
            ("Drum Loops/128/Full 01.wav", 7.5, "drum_loop"),
            ("Hi-Hats/Closed/HH 01.wav", 0.1, "drum_oneshot")]:
        check_eq("two folder levels: %s" % rel, tout.classify(rel, dur)["type"], want)
    check_eq("empty cell stays empty", tout.mesure._safe_cell(""), "")
    check_eq("formula cell is quoted", tout.mesure._safe_cell("=1+1"), "'=1+1")
    four, sn, h8, h16 = (0, 4, 8, 12), (4, 12), tuple(range(0, 16, 2)), tuple(range(16))
    for bpm in (150.0, 160.0, 174.0):
        for name, k in (("four-on-floor", four), ("boom-bap", (0, 10))):
            m = tout.measure_drum_loop(SR, mono(drum_loop(bpm, 4, 0.5, k, sn, h8)), {"bpm": None})
            check("fast %s %g bpm: bpm" % (name, bpm), m["bpm_measured"], bpm, 0.01, relative=True)
            check("fast %s %g bpm: bars" % (name, bpm), m["bars"], 4, 0)
    m = tout.measure_drum_loop(SR, mono(drum_loop(70.0, 2, 0.5, four, sn, h16)), {"bpm": None})
    check("70 bpm with 16th hats: bpm", m["bpm_measured"], 70.0, 0.01, relative=True)
    check("70 bpm with 16th hats: bars", m["bars"], 2, 0)


def test_tags_types():
    """Blind audit I4-I7: tags and types taken by a stray word."""
    for parts, typ, want in [
            (["Loops", "Bass Loop 100.wav"], "melodic_loop", None),
            (["Loops", "24Bit 96k", "Bass Loop Am.wav"], "melodic_loop", None),
            (["Melody Loops 140 BPM", "Melody 74 Cm.wav"], "melodic_loop", 140.0),
            (["Drum Loops", "Loop 66.wav"], "drum_loop", None),
            (["Loops", "Bass 87.5 BPM Em.wav"], "melodic_loop", 87.5),
            (["Loops", "Bass 124 Am.wav"], "melodic_loop", 124.0)]:
        check_eq("bpm tag %s" % "/".join(parts), tout.parse_tags(parts, typ)["bpm"], want)
    for parts, typ, field, want in [
            (["Loops", "A Bass 125 Em.wav"], "melodic_loop", "key", (4, "minor")),
            (["Loops", "D Bass Loop 125.wav"], "melodic_loop", "key", None),
            (["Loops 125 BPM", "Bass G.wav"], "melodic_loop", "key", (7, "major")),
            (["Loops", "Bass 125 Eb minor.wav"], "melodic_loop", "key", (3, "minor")),
            (["One Shots", "E Piano C3.wav"], "oneshot_melodic", "note", 0),
            (["One Shots", "B3 Organ C2.wav"], "oneshot_melodic", "note", 0),
            (["One Shots", "Synth A 01.wav"], "oneshot_melodic", "note", None),
            (["Kicks", "Kick A 01.wav"], "drum_oneshot", "note", None),
            (["Snares", "Snare D 03.wav"], "drum_oneshot", "note", None),
            (["One Shots", "Pluck C#3.wav"], "oneshot_melodic", "note", 1)]:
        check_eq("%s tag %s" % (field, "/".join(parts)), tout.parse_tags(parts, typ)[field], want)
    for rel, dur, field, want in [
            ("Drum One Shots/Hats/HH Closed 01.wav", 0.1, "subtype", "hat_closed"),
            ("Drum One Shots/Hats/Hat Closed 01.wav", 0.1, "subtype", "hat_closed"),
            ("Drum One Shots/Hats/HiHat Open 02.wav", 0.5, "subtype", "hat_open"),
            ("Drum One Shots/Hats/OHH 01.wav", 0.5, "subtype", "hat_open"),
            ("Drum One Shots/Hats/CHH_01.wav", 0.1, "subtype", "hat_closed"),
            ("Drum One Shots/Perc/Perc High 01.wav", 0.2, "subtype", "perc_high"),
            ("Drum One Shots/Perc/Perc Low 01.wav", 0.2, "subtype", "perc_low"),
            ("One Shots/Kicks/Kick Impact 01.wav", 0.4, "type", "drum_oneshot"),
            ("One Shots/Snares/Snare Reverse.wav", 0.5, "type", "drum_oneshot"),
            ("Loops/Drums/Noise Hat Loop 128.wav", 4.0, "type", "drum_loop"),
            ("Drum Loops/Drop Beat 140.wav", 8.0, "type", "drum_loop"),
            ("FX/Risers/Riser 01.wav", 4.0, "type", "fx"),
            ("Kits/Kit 1/Kick 01.wav", 0.3, "type", "drum_oneshot"),
            ("Construction Kits/Kit 01/One Shots/Kick.wav", 0.3, "type", "drum_oneshot"),
            ("Vendor Construction Pack/Drum Loops/Loop 120.wav", 8.0, "type", "drum_loop"),
            ("Kit 01 120 Am/Stems/Bass.wav", 16.0, "type", "kit_stem"),
            ("One Shots/Synth/Atom Pluck C3.wav", 1.0, "role", "pluck"),
            ("One Shots/Synth/Chat Lead.wav", 1.0, "role", "lead"),
            ("Drum One Shots/808/808 Long F.wav", 6.5, "type", "drum_oneshot"),
            ("Drum One Shots/Cymbals/Crash 01.wav", 7.0, "type", "drum_oneshot")]:
        check_eq("%s of %s" % (field, rel), tout.classify(rel, dur)[field], want)


def test_end_cut_audit():
    """Blind audit I1/I2: short fades, quiet stops, digital silence, DC, noise tails, loops."""
    def lin(x, ms):
        n = int(ms * SR / 1000)
        x = x.copy()
        x[-n:] *= np.linspace(1, 0, n)
        return x
    k = kick(t_axis(0.25), 150.0, 50.0, 0.25)
    for ms in (5, 10):
        check_eq("audit: loud kick, linear fade %d ms: not cut" % ms, tout.end_cut(SR, mono(lin(k, ms)))["end_cut"], 0)
    check_eq("audit: loud kick, 2 ms fade: cut", tout.end_cut(SR, mono(lin(k, 2)))["end_cut"], 1)
    q = 0.02 * np.sin(2 * math.pi * 50 * t_axis(0.6))
    check_eq("audit: quiet 808 stopped at a zero crossing: cut", tout.end_cut(SR, mono(q))["end_cut"], 1)
    ds = np.concatenate([k, np.zeros(SR // 10)])
    check_eq("audit: abrupt stop + digital silence: cut", tout.end_cut(SR, mono(ds))["end_cut"], 1)
    tail = band_noise(SR, 200, 8000, 2) * np.exp(-t_axis(1.0) / 0.17)
    check_eq("audit: noise reverb tail to -51 dB: not cut", tout.end_cut(SR, mono(tail))["end_cut"], 0)
    check_eq("audit: faded kick with DC offset: not cut", tout.end_cut(SR, mono(lin(k, 20) + 0.01))["end_cut"], 0)
    pad = 0.3 * harmonic(220, t_axis(2.0), 4, 0.5)
    check_eq("audit: seamless 2 s pad loop: not cut", tout.end_cut(SR, mono(pad))["end_cut"], 0)
    s808 = 0.8 * np.sin(2 * math.pi * 50 * t_axis(0.6))
    check_eq("audit: 808 at a zero crossing: still cut", tout.end_cut(SR, mono(s808))["end_cut"], 1)


def test_measure_values():
    """Blind audit I14: each column the audit mutated is pinned to a known value."""
    c = tout.common(SR, mono(0.1 * np.sin(2 * np.pi * 1000 * t_axis(2.0))))
    check("common dur_s of a 2 s file", c["dur_s"], 2.0, 0.001)
    k = kick(t_axis(0.25), 150.0, 50.0, 0.25)
    check_eq("common end_cut of an abrupt kick", tout.common(SR, mono(k))["end_cut"], 1)
    sus = 0.7 * np.sin(2 * math.pi * 49.0 * t_axis(1.0))
    c = tout.common(SR, mono(_fade_out(sus, 20.0)))
    check_eq("common end_cut of a faded 808", c["end_cut"], 0)
    check_true("common end_fade_ms of a 20 ms fade (10-25)",
               c["end_fade_ms"] is not None and 10 <= c["end_fade_ms"] <= 25)
    t = t_axis(4.0)
    m = tout.measure_drum_oneshot(SR, mono(0.9 * kick(t, 200.0, 50.0, 0.03)), "kick")
    check("kick drop_start_hz (first window of a 200 Hz start)", m["drop_start_hz"], 163.6, 0.1, relative=True)
    check("kick t40_ms (0.12 s decay: 552.6)", m["t40_ms"], 552.6, 0.05, relative=True)
    check("kick t60_ms (0.12 s decay: 828.9)", m["t60_ms"], 828.9, 0.05, relative=True)
    check_true("kick onset_ms of an instant attack under 0.5", m["onset_ms"] is not None and m["onset_ms"] < 0.5)
    parts = []
    for f in (220.0, 330.0, 220.0, 247.0):
        tt = t_axis(0.4)
        ph = 2 * np.pi * np.cumsum(f * (1 + 0.01 * np.sin(2 * np.pi * 5 * tt))) / SR
        env = np.minimum(tt / 0.01, 1) * np.minimum((0.4 - tt) / 0.01, 1)
        parts.append(0.3 * sum(0.6 ** (h - 1) * np.sin(h * ph) for h in range(1, 6)) * env)
        parts.append(np.zeros(int(0.5 * SR)))
    dry = np.concatenate(parts)
    n = int(1.5 * SR)
    ir = band_noise(n, 100, 8000, 9) * np.exp(-np.arange(n) / SR * 6.9 / 1.5)
    tail = np.fft.irfft(np.fft.rfft(dry, len(dry) + n) * np.fft.rfft(ir, len(dry) + n), len(dry) + n)
    wet = np.concatenate([dry, np.zeros(n)]) + 0.5 * tail / np.max(np.abs(tail)) * np.max(np.abs(dry))
    check("vocal wet tail_db (1.5 s reverb: about -21)", tout.measure_vocal(SR, mono(wet), {})["tail_db"], -21.2, 4.0)


def test_accuracy_rms():
    """Blind audit I9 (near misses have their own class) and I12 (rms per channel)."""
    check_true("relative key is not an agreement", not tout._key_agree("relative"))
    check_eq("bpm 140 vs tag 70 class", tout._bpm_class(140.0, 70.0), "half_double")
    check_eq("bpm 69 vs tag 140 class", tout._bpm_class(69.0, 140.0), "half_double")
    check_eq("bpm 120.5 vs tag 120 class", tout._bpm_class(120.5, 120.0), True)
    check_eq("bpm 100 vs tag 120 class", tout._bpm_class(100.0, 120.0), False)
    acc = tout.Accuracy()
    for a in (True, "half_double", False, "relative", None):
        acc.add("drum_loop", "bpm", a)
    r = acc.rows()[0]
    check_eq("accuracy counts", (r["tagged"], r["agree"], r["half_double"], r["relative"]), (4, 1, 1, 1))
    t = np.arange(48000) / 48000.0
    s = 0.5 * np.sin(2 * np.pi * 440 * t)
    mono = tout.common(48000, s[:, None])["rms_dbfs"]
    check_true("rms mono -9.03", abs(mono + 9.03) < 0.05)
    for label, x in [("L=s R=s/2", np.stack([s, s / 2], 1)), ("antiphase", np.stack([s, -s], 1)),
                     ("6 channels", np.stack([s] * 6, 1))]:
        check_true("rms %s <= mono" % label, tout.common(48000, x)["rms_dbfs"] <= mono + 0.01)


def _fence():
    return getattr(tout, "fence_row", lambda row, usable=(): row)


def test_recheck_tags_cells():
    """Blind re-check 2-4 (docs/sons/REVERIF-TOUT.md)."""
    for parts in (["Loops", "Bass Loop Am 24Bit 96k.wav"], ["Loops", "Bass Loop 96kHz Am.wav"],
                  ["Loops", "Bass 96 kHz Am.wav"], ["Loops", "Pad 44 khz.wav"],
                  ["Loops", "Keys 192k Em.wav"], ["Loops", "Lead 24 bit Am.wav"],
                  ["Loops", "Lead 24bits.wav"], ["Loops", "Pad 48 Hz Hum.wav"],
                  ["Loops 24Bit", "Bass Em.wav"]):
        check_eq("bpm tag not a sample rate or bit depth: %s" % "/".join(parts),
                 tout.parse_tags(parts, "melodic_loop")["bpm"], None)
    check_eq("bpm tag kept next to a key: Bass 124 Am",
             tout.parse_tags(["Loops", "Bass 124 Am.wav"], "melodic_loop")["bpm"], 124.0)
    for rel in ("Drum One Shots/Perc/Perc Mid 01.wav", "Drum One Shots/Perc/Perc Middle 02.wav",
                "Drum One Shots/Perc/Mid Perc 03.wav"):
        check_eq("subtype perc_mid: %s" % rel, tout.classify(rel, 0.2)["subtype"], "perc_mid")
    check_eq("lone dash cell written as dash", tout._fmt("-"), "-")
    check_eq("dash formula start still quoted", tout._fmt("-A1"), "'-A1")
    check_eq("equals formula still quoted", tout._fmt("=1+1"), "'=1+1")


def test_recheck_short_noise():
    """Blind re-check 5: a very short noise burst that fades out is not cut."""
    cuts = 0
    for length in (0.1, 0.15, 0.2, 0.3):
        for tau in (0.01, 0.02):
            for seed in range(20):
                r = np.random.default_rng(seed)
                n = int(length * SR)
                t = np.arange(n) / SR
                x = 0.5 * r.standard_normal(n) * np.exp(-t / tau)
                cuts += tout.end_cut(SR, x[:, None])["end_cut"]
    check_eq("short fading noise bursts flagged cut (of 160)", cuts, 0)
    n = int(0.3 * SR)
    t = np.arange(n) / SR
    x = 0.5 * np.random.default_rng(1).standard_normal(n) * np.exp(-t / 0.02) + 0.05
    check_eq("fading noise on a DC offset is not cut", tout.end_cut(SR, x[:, None])["end_cut"], 0)
    x = 0.5 * np.random.default_rng(2).standard_normal(n) * np.exp(-t / 0.2)
    check_eq("noise stopped loud is still cut", tout.end_cut(SR, x[:, None])["end_cut"], 1)


def test_recheck_end_scope():
    """Re-check 1 (docs/sons/REVERIF-TOUT.md): end_cut on loops is a coin toss; one-shots only."""
    fence = _fence()
    four, sn = (0, 4, 8, 12), (4, 12)
    for bpm in (90.0, 120.0, 140.0, 174.0):
        for hats in (tuple(range(16)), (2, 6, 10, 14)):
            x = mono(drum_loop(bpm, 2, 0.5, four, sn, hats))
            row = {"type": "drum_loop", "role": "drums"}
            row.update(tout.common(SR, x))
            row = fence(dict(row))
            check_true("drum loop %g bpm: end_cut and end_fade_ms empty" % bpm,
                       row.get("end_cut") is None and row.get("end_fade_ms") is None)
    for typ in ("melodic_loop", "kit_stem", "fx", "vocal"):
        row = fence({"type": typ, "role": "bass", "end_cut": 1, "end_fade_ms": 61.0})
        check_true("%s: end columns empty" % typ, row.get("end_cut") is None and row.get("end_fade_ms") is None)
    for typ, extra in (("oneshot_melodic", {"role": "full"}), ("oneshot_melodic", {"bpm_tag": 120.0}),
                       ("oneshot_melodic", {"_loop": 1}), ("drum_oneshot", {"bpm_tag": 140.0})):
        row = {"type": typ, "role": "kick", "end_cut": 1, "end_fade_ms": 10.0}
        row.update(extra)
        row = fence(row)
        check_true("%s with %s: end columns empty" % (typ, list(extra)[0]),
                   row.get("end_cut") is None and row.get("end_fade_ms") is None)
    row = fence({"type": "drum_oneshot", "role": "kick", "end_cut": 1, "end_fade_ms": None})
    check_eq("drum one-shot keeps end_cut", row.get("end_cut"), 1)
    row = fence({"type": "oneshot_melodic", "role": "bass", "end_cut": 0, "end_fade_ms": None})
    check_eq("melodic one-shot keeps end_cut", row.get("end_cut"), 0)


def test_cli():
    with tempfile.TemporaryDirectory() as tmp:
        pack = os.path.join(tmp, "Secret Vendor Pack")
        files = {}
        t = t_axis(1.0)
        for i, (f, tag) in enumerate(((110.0, "A2"), (130.81, "C3"), (146.83, "D3"))):
            x = 0.4 * harmonic(f, t) * adsr(t, 0.005, 0.1, 0.6, 0.8, 0.15)
            files["One Shots/Synth/Zorglub Pluck %s.wav" % tag] = mono(x)
        files["One Shots/Kicks/Zorglub Kick.wav"] = mono(0.9 * kick(t_axis(0.6)))
        files["Drum Loops/Techno/Zorglub Top 124 BPM.wav"] = mono(
            drum_loop(124.0, 2, 0.5, (0, 4, 8, 12), (4, 12), tuple(range(0, 16, 2))))
        am = [[57, 60, 64], [53, 57, 60], [50, 53, 57], [52, 56, 59]]
        bass = chord_loop(124.0, [[c[0] - 12] for c in am])
        keys = chord_loop(124.0, am)
        drums = drum_loop(124.0, 4, 0.5, (0, 4, 8, 12), (4, 12), tuple(range(16)))
        kit = "Construction Kits/Zorglub Kit 124 BPM Am"
        files[kit + "/Zorglub Bass.wav"] = mono(bass)
        files[kit + "/Zorglub Keys.wav"] = mono(keys)
        files[kit + "/Zorglub Drums.wav"] = mono(drums[:len(keys)])
        files["Vocals/Zorglub Vox.wav"] = mono(0.3 * harmonic(220.0, t_axis(2.0)) * (t_axis(2.0) < 1.0))
        files["Drum Loops/Techno/Zorglub broken 124.wav"] = b"RIFF garbage"
        files["One Shots/Synth/Zorglub Broken C3.wav"] = b""
        files["FX/Zorglub Riser.wav"] = mono(np.random.default_rng(1).standard_normal(SR * 2) * np.linspace(0, 0.3, SR * 2))
        for rel, x in files.items():
            p = os.path.join(pack, rel)
            os.makedirs(os.path.dirname(p), exist_ok=True)
            with open(p, "wb") as fh:
                fh.write(x if isinstance(x, bytes) else wav_bytes(x))
        notes = [(b * 1920, 1920, p, 90, 0) for b, ch in enumerate(am) for p in ch]
        with open(os.path.join(pack, kit, "Zorglub Chords.mid"), "wb") as fh:
            fh.write(midi_bytes(notes, 480, 124.0))
        with open(os.path.join(pack, "One Shots", "Zorglub Preset.fxp"), "wb") as fh:
            fh.write(b"preset")
        out_dir = os.path.join(tmp, "out")
        os.makedirs(out_dir)
        prefix = os.path.join(out_dir, "rar7")
        code = tout.main([pack, "--rar", "7", "--out", prefix])
        check_eq("cli exit code", code, 0)
        made = sorted(os.listdir(out_dir))
        for typ in ("oneshot_melodic", "drum_oneshot", "drum_loop", "kit", "kit_stem", "midi", "vocal", "fx"):
            check_true("cli wrote %s table" % typ, "rar7_%s.csv" % typ in made)
        blob = ""
        for name in made:
            with open(os.path.join(out_dir, name), encoding="utf-8") as fh:
                blob += fh.read()
        check_true("cli tables hold no file or folder name", "Zorglub" not in blob and "Secret" not in blob
                   and "Vendor" not in blob)
        fixed = ("rar,kit_id,type,subtype,role,style,dur_s,sr,ch,size_kb,peak_dbfs,rms_dbfs,lufs_i,"
                 "bpm_tag,key_tag,bpm_est,key_est,error").split(",")
        check_eq("cli fixed column list", tout.COMMON, fixed)
        for name in made:
            with open(os.path.join(out_dir, name), encoding="utf-8") as fh:
                first = fh.readline().rstrip("\r\n").split(",")
            if not name.endswith("_accuracy.csv"):
                check_eq("cli %s starts with the fixed columns on line 1" % name, first[:len(fixed)], fixed)
        check_true("cli accuracy only in its own table", "# accuracy" not in blob)

        def rows(typ):
            with open(os.path.join(out_dir, "rar7_%s.csv" % typ), encoding="utf-8") as fh:
                return list(csv.DictReader(line for line in fh if not line.startswith("#")))

        mel = rows("oneshot_melodic")
        check_true("cli empty cells stay empty", all(r["kit_id"] == "" for r in mel)
                   and sum(r["error"] == "" for r in mel) == 3)
        check_true("cli size_kb filled", all(float(r["size_kb"]) > 0 for r in mel if not r["error"]))
        check_true("cli unreadable files keep their type",
                   any(r["error"] for r in mel) and any(r["error"] for r in rows("drum_loop"))
                   and "rar7_melodic_loop.csv" not in made)
        mel = [r for r in mel if not r["error"]]
        check_eq("cli three melodic one-shots", len(mel), 3)
        check_true("cli melodic notes all agree with tags", all(abs(float(r["cents_gap"])) < 10 for r in mel))
        check_true("cli common columns first", list(mel[0].keys())[:len(tout.COMMON)] == tout.COMMON)
        check_true("cli rar column", all(r["rar"] == "7" for r in mel))
        stems = rows("kit_stem")
        kits = rows("kit")
        mids = rows("midi")
        check_eq("cli one kit row", len(kits), 1)
        kid = kits[0]["kit_id"]
        check_true("cli kit id shared by stems and midi",
                   len(stems) == 3 and all(r["kit_id"] == kid for r in stems) and mids and mids[0]["kit_id"] == kid)
        check_eq("cli kit roles", kits[0]["roles"], "bass+drums+keys")
        check_eq("cli kit key measured", kits[0]["key_est"], "A minor")
        check("cli kit bpm measured", float(kits[0]["bpm_est"]), 124.0, 0.01, relative=True)
        lv = [10 ** (float(r["level_rel_kit_db"]) / 10) for r in stems]
        check_true("cli stem levels relative to the kit sum are below 0 dB", all(v < 1.0 for v in lv))
        dl = [r for r in rows("drum_loop") if not r["error"]]
        check_eq("cli drum loop style from folder", dl[0]["style"], "techno")
        check_eq("cli midi chords against kit key", mids[0]["chords"], "i-VI-iv-V")
        check_true("cli end_cut filled on one-shot rows",
                   all(r.get("end_cut") in ("0", "1") for r in mel + rows("drum_oneshot")))
        check_true("cli end_cut and end_fade_ms empty on loops, stems, fx and vocals (re-check 1)",
                   all(r.get("end_cut") == "" and r.get("end_fade_ms") == ""
                       for r in stems + dl + rows("fx") + rows("vocal")))
        inside = os.path.join(os.path.dirname(os.path.abspath(__file__)), "leak")
        try:
            tout.main([pack, "--rar", "7", "--out", inside])
            wrote = any(n.startswith("leak_") for n in os.listdir(os.path.dirname(inside)))
        except SystemExit:
            wrote = False
        check_true("cli refuses to write inside the repository", not wrote)


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if "--app" in argv:
        print(APP_DOC)
        return 0
    with np.errstate(all="ignore"):
        for fn in (test_tags, test_classify, test_common, test_notes, test_adsr, test_drum_loop, test_key,
                   test_midi, test_vocal, test_fx, test_drum_oneshots, test_end_cut, test_end_cut_audit, test_measure_values, test_blocking, test_tags_types, test_accuracy_rms, test_recheck_end_scope, test_recheck_tags_cells, test_recheck_short_noise, test_cli):
            try:
                fn()
            except Exception as e:  # a crash is a failed check, the rest still runs
                COUNT[0] += 1
                FAILS.append("%s crashed: %s: %s" % (fn.__name__, type(e).__name__, e))
                print("FAIL %s crashed: %s: %s" % (fn.__name__, type(e).__name__, e))
    print("%d checks, %d failed" % (COUNT[0], len(FAILS)))
    for name in FAILS:
        print("FAILED: " + name)
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
