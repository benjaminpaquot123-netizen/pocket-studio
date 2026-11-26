#!/usr/bin/env python3
"""Reproduction script of the blind audit of scripts/sons/tout.py.

Report: docs/sons/AUDIT-TOUT.md. Every case builds a small synthetic signal
or MIDI file (numpy and struct only, generators of selftest_tout.py), runs
the tool on it and prints one line: verdict, case id, input, expected, got.
"BAD" = the defect is still there; "ok" = the tool gives the expected value.

    python3 -I scripts/sons/audit_tout_repro.py              # all cases
    python3 -I scripts/sons/audit_tout_repro.py --mutations  # + self-test mutations (slow)

No audio is read from or written to the repository: files go to a fresh
temporary directory that is removed at the end. The exit code is always 0
(this is a list of findings, not a gate).
"""
import contextlib
import csv
import glob
import io
import os
import shutil
import struct
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np  # noqa: E402

import mesure  # noqa: E402
import selftest_tout as st  # noqa: E402
import tout  # noqa: E402

SR = 48000
np.seterr(all="ignore")
COUNT = {"BAD": 0, "ok": 0}


def T(seconds):
    return np.arange(int(seconds * SR)) / SR


def col(x):
    return x[:, None] if x.ndim == 1 else x


def case(cid, what, expected, got, good):
    tag = "ok" if good else "BAD"
    COUNT[tag] += 1
    print("%-3s %-4s %-58s expected %-22s got %s" % (tag, cid, what, expected, got))


def r2(v):
    return round(float(v), 2) if isinstance(v, (float, np.floating)) else v


def fade(x, ms, kind="lin"):
    n = int(ms * SR / 1000)
    x = x.copy()
    if kind == "lin":
        ramp = np.linspace(1, 0, n)
    elif kind == "exp":
        ramp = np.exp(-5 * np.linspace(0, 1, n))
    else:
        ramp = np.cos(np.linspace(0, np.pi / 2, n)) ** 2
    x[-n:] *= ramp
    return x


def tri(root, minor):
    return [root, root + (3 if minor else 4), root + 7]


# ---------------------------------------------------------------- B1, I4-I7
def tags_and_types():
    print("\n== B1 top folder ignored with two folder levels (classify)")
    for rel, dur, want in [
        ("Vocals/Take 01.wav", 4.0, "vocal"),
        ("Vocals/Dry/Take 01.wav", 4.0, "vocal"),
        ("Vocals/Adlibs/Hey 01.wav", 1.0, "vocal"),
        ("Vocal Loops/Dry/Hook 120 Am.wav", 8.0, "vocal"),
        ("Drum Loops/Tops/Top 128 01.wav", 7.5, "drum_loop"),
        ("Drum Loops/128/Full 01.wav", 7.5, "drum_loop"),
        ("Hi-Hats/Closed/HH 01.wav", 0.1, "drum_oneshot"),
    ]:
        c = tout.classify(rel, dur)
        case("B1", "%s (%.1f s)" % (rel, dur), want, c["type"], c["type"] == want)

    print("\n== I4 bpm tags")
    for parts, typ, want in [
        (["Loops", "Bass Loop 100.wav"], "melodic_loop", None),
        (["Loops", "24Bit 96k", "Bass Loop Am.wav"], "melodic_loop", None),
        (["Melody Loops 140 BPM", "Melody 74 Cm.wav"], "melodic_loop", 140.0),
        (["Drum Loops", "Loop 66.wav"], "drum_loop", None),
        (["Loops", "Bass 87.5 BPM Em.wav"], "melodic_loop", 87.5),
        (["Loops", "Bass Loop 2024 Am.wav"], "melodic_loop", None),
    ]:
        got = tout.parse_tags(parts, typ).get("bpm")
        case("I4", "/".join(parts), want, got, got == want)

    print("\n== I5 key and note tags")
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
        (["One Shots", "Pluck C#3.wav"], "oneshot_melodic", "note", 1),
    ]:
        got = tout.parse_tags(parts, typ).get(field)
        case("I5", "/".join(parts) + " [" + field + "]", want, got, got == want)

    print("\n== I6 hats and percussion subtypes")
    for rel, dur, want in [
        ("Drum One Shots/Hats/Closed Hat 01.wav", 0.1, "hat_closed"),
        ("Drum One Shots/Hats/HH Closed 01.wav", 0.1, "hat_closed"),
        ("Drum One Shots/Hats/Hat Closed 01.wav", 0.1, "hat_closed"),
        ("Drum One Shots/Hats/HiHat Open 02.wav", 0.5, "hat_open"),
        ("Drum One Shots/Hats/OHH 01.wav", 0.5, "hat_open"),
        ("Drum One Shots/Hats/CHH_01.wav", 0.1, "hat_closed"),
        ("Drum One Shots/Perc/Perc High 01.wav", 0.2, "perc_high"),
        ("Drum One Shots/Perc/Perc Low 01.wav", 0.2, "perc_low"),
    ]:
        c = tout.classify(rel, dur)
        case("I6", rel, want, c["subtype"], c["subtype"] == want)

    print("\n== I7 type and role taken by a stray word")
    for rel, dur, field, want in [
        ("808/808 Bass 01 C.wav", 1.2, "subtype", "kick_808"),
        ("One Shots/Kicks/Kick Impact 01.wav", 0.4, "type", "drum_oneshot"),
        ("One Shots/Snares/Snare Reverse.wav", 0.5, "type", "drum_oneshot"),
        ("Loops/Drums/Noise Hat Loop 128.wav", 4.0, "type", "drum_loop"),
        ("Drum Loops/Drop Beat 140.wav", 8.0, "type", "drum_loop"),
        ("Kits/Kit 1/Kick 01.wav", 0.3, "type", "drum_oneshot"),
        ("Construction Kits/Kit 01/One Shots/Kick.wav", 0.3, "type", "drum_oneshot"),
        ("Vendor Construction Pack/Drum Loops/Loop 120.wav", 8.0, "type", "drum_loop"),
        ("Kit 01 120 Am/Stems/Bass.wav", 16.0, "type", "kit_stem"),
        ("One Shots/Synth/Atom Pluck C3.wav", 1.0, "role", "not perc"),
        ("One Shots/Synth/Chat Lead.wav", 1.0, "role", "not hats"),
        ("Drum One Shots/808/808 Long F.wav", 6.5, "type", "drum_oneshot"),
        ("Drum One Shots/Cymbals/Crash 01.wav", 7.0, "type", "drum_oneshot"),
        ("One Shots/Piano/Piano C3.wav", 9.0, "type", "oneshot_melodic"),
        ("Loops/Percussion/Conga 120.wav", 4.0, "type", "drum_loop"),
    ]:
        got = tout.classify(rel, dur)[field]
        good = (got not in want) if want.startswith("not ") else (got == want)
        case("I7", "%s (%.1f s) [%s]" % (rel, dur, field), want, got, good)


# ------------------------------------------------------------- B2, B3, I3
def output_contract(base):
    print("\n== B2 B3 I3 output files of a small synthetic pack (CLI)")
    pack = os.path.join(base, "SECRETVENDOR Pack Vol 3")

    def put(rel, data):
        path = os.path.join(pack, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)

    W = st.wav_bytes
    t = st.t_axis(1.0)
    tone = st.mono(0.5 * st.harmonic(261.63, t) * st.adsr(t, 0.005, 0.1, 0.6, 0.7, 0.2))
    put("One Shots/Keys/SECRETNAME Piano C3.wav", W(tone))
    put("One Shots/Keys/=cmd|' calc'!A1 C3.wav", W(tone))
    put("One Shots/Keys/broken SECRETBROKEN C3.wav", b"RIFF garbage not wav")
    put("One Shots/Keys/empty.wav", b"")
    put("Drum Loops/SECRETLOOP 124 BPM.wav",
        W(st.mono(st.drum_loop(124, 2, 0.5, [0, 4, 8, 12], [4, 12], range(0, 16, 2)))))
    put("Drum Loops/broken loop 124.wav", bytes(range(256)) * 12)
    put("Drums/Kicks/SECRETKICK 01.wav", W(st.mono(st.kick(st.t_axis(0.4)))))
    put("MIDI/SECRETMIDI Chords Am.mid",
        st.midi_bytes([(0, 400, 57, 100, 0), (0, 400, 60, 100, 0), (0, 400, 64, 100, 0)]))
    put("Vocals/SECRETVOX Dry 120 Am.wav", W(st.mono(0.3 * st.harmonic(220, st.t_axis(4.0)))))
    put("Docs/readme SECRETDOC.txt", b"hello")
    out = os.path.join(base, "out", "t")
    os.makedirs(os.path.dirname(out))
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        try:
            tout.main([pack, "--rar", "7", "--out", out])
        except SystemExit:
            pass
    fixed = ("rar,kit_id,type,subtype,role,style,dur_s,sr,ch,size_kb,peak_dbfs,rms_dbfs,"
             "lufs_i,bpm_tag,key_tag,bpm_est,key_est,error").split(",")
    text = buf.getvalue()
    for path in sorted(glob.glob(out + "*")):
        name = os.path.basename(path)
        with open(path, encoding="utf-8") as f:
            body = f.read()
        text += body
        lines = body.splitlines()
        rows_txt = [l for l in lines if not l.startswith("#")]
        if not rows_txt:
            continue
        cols = rows_txt[0].split(",")
        if "accuracy" in name:
            continue
        missing = [c for c in fixed if c not in cols]
        case("B2", name + ": fixed columns present", "none missing", "missing " + ",".join(missing), not missing)
        case("B2", name + ": fixed columns first, in order", "yes", "starts " + ",".join(cols[:13]),
             cols[:len(fixed)] == fixed)
        case("B2", name + ": first line is the header row", "header", lines[0][:40],
             not lines[0].startswith("#"))
        for r in csv.DictReader(io.StringIO("\n".join(rows_txt))):
            if r.get("peak_dbfs"):
                case("B3", name + ": error cell of a good row", "''", repr(r["error"]), r["error"] == "")
                case("B3", name + ": kit_id cell without kit", "''", repr(r["kit_id"]), r["kit_id"] == "")
                break
        if "melodic_loop" in name:
            bad = [r for r in csv.DictReader(io.StringIO("\n".join(rows_txt))) if r["error"] not in ("", "'")]
            case("I3", "unreadable one-shot/drum-loop files in the melodic_loop table", "0 rows",
                 "%d rows" % len(bad), not bad)
    for word in ("SECRET", "calc", "Piano", "readme", "Vol 3", "broken"):
        case("leak", "word %r in any CSV or stdout" % word, "absent",
             "present" if word in text else "absent", word not in text)


# ----------------------------------------------------------------- B4, I8
def tempo_and_key():
    print("\n== B4 drum-loop tempo (4 bars generated above 120 bpm, 2 below)")
    four, sn, h8, h16 = (0, 4, 8, 12), (4, 12), tuple(range(0, 16, 2)), tuple(range(16))
    for bpm in (70, 87, 100, 128, 140, 150, 160, 170, 174, 180):
        bars = 4 if bpm >= 120 else 2
        for name, (k, s, h) in (("four-on-floor", (four, sn, h8)), ("boom-bap", ((0, 10), sn, h8))):
            m = tout.measure_drum_loop(SR, st.mono(st.drum_loop(float(bpm), bars, 0.5, k, s, h)), {"bpm": None})
            good = abs(m["bpm_measured"] - bpm) <= 0.01 * bpm and m["bars"] == bars
            case("B4", "%d bpm %s, 8th hats" % (bpm, name), "%d bpm %d bars" % (bpm, bars),
                 "%.1f bpm %s bars" % (m["bpm_measured"], m["bars"]), good)
    m = tout.measure_drum_loop(SR, st.mono(st.drum_loop(70.0, 2, 0.5, four, sn, h16)), {"bpm": None})
    case("B4", "70 bpm four-on-floor, 16th hats", "70 bpm 2 bars",
         "%.1f bpm %s bars" % (m["bpm_measured"], m["bars"]), abs(m["bpm_measured"] - 70) < 1)

    print("\n== I8 swing, triplets, melodic-loop tempo, key")
    for sw in (0.5, 0.58, 0.667):
        m = tout.measure_drum_loop(SR, st.mono(st.drum_loop(120.0, 2, sw, four, sn, h16)), {"bpm": None})
        case("ok", "swing %.1f %% at 120 bpm" % (100 * sw), "%.1f" % (100 * sw), m["swing_pct"],
             abs(m["swing_pct"] - 100 * sw) < 1)
    step = 60 / 120 / 3
    n = int(2 * 4 * 0.5 * SR)
    x = np.zeros(n)
    hat = st.band_noise(int(.08 * SR), 8000, 16000, 3) * np.exp(-T(.08) / .012)
    for i in range(24):
        a = int(round(i * step * SR))
        e = min(n, a + len(hat))
        x[a:e] += 0.3 * hat[:e - a]
    kk = st.kick(T(.4))
    for b in range(8):
        a = int(round(b * 0.5 * SR))
        e = min(n, a + len(kk))
        x[a:e] += 0.8 * kk[:e - a]
    m = tout.measure_drum_loop(SR, st.mono(x), {"bpm": None})
    case("I8", "12/8 loop (8th triplets) at 120 bpm, 2 bars", "120 bpm 2 bars",
         "%.1f bpm %s bars swing %s" % (m["bpm_measured"], m["bars"], m["swing_pct"]),
         abs(m["bpm_measured"] - 120) < 2)
    am = [[57, 60, 64], [53, 57, 60], [50, 53, 57], [52, 56, 59]]
    for bpm in (80, 100, 128, 140, 174):
        m = tout.measure_melodic_loop(SR, st.mono(st.chord_loop(float(bpm), am)), {"bpm": None, "key": None})
        got = m["bpm_measured"]
        case("I8", "pad chord loop %d bpm, no drums, 4 bars" % bpm, "%d bpm 4 bars" % bpm,
             "%s bpm %s bars" % (r2(got), m["bars"]), got is not None and abs(got - bpm) < 0.02 * bpm)
    for name, chords, want in [
        ("C major I-IV-V-I", [tri(60, 0), tri(65, 0), tri(67, 0), tri(60, 0)], "C major"),
        ("A minor i-iv-v-i", [tri(57, 1), tri(62, 1), tri(64, 1), tri(57, 1)], "A minor"),
        ("A minor i-VII-VI-VII", [tri(57, 1), tri(55, 0), tri(53, 0), tri(55, 0)], "A minor"),
        ("A minor i-VI-III-VII", [tri(57, 1), tri(53, 0), tri(60, 0), tri(55, 0)], "A minor"),
        ("D dorian i-IV", [tri(62, 1), tri(67, 0)] * 2, "D minor"),
    ]:
        m = tout.measure_melodic_loop(SR, st.mono(st.chord_loop(120.0, chords)), {"bpm": 120.0, "key": None})
        case("I8", "key of " + name, want, m["key_measured"], m["key_measured"] == want)
    xs = []
    for p in [45, 45, 48, 50, 52, 55, 45, 43]:
        t = st.t_axis(0.5)
        xs.append(0.3 * st.harmonic(440 * 2 ** ((p - 69) / 12), t, 4, 0.5)
                  * np.minimum(t / .005, 1) * np.minimum((0.5 - t) / .02, 1))
    m = tout.measure_melodic_loop(SR, st.mono(np.concatenate(xs * 2)), {"bpm": 120.0, "key": (9, "minor")})
    case("I8", "mono bassline A minor pentatonic", "A minor", m["key_measured"], m["key_measured"] == "A minor")
    case("I8", "melodic_loop measure has chords / progression", "chords key",
         "absent" if "chords" not in m else "present", "chords" in m)

    print("\n== I9 accuracy rule")
    case("I9", "_bpm_agree(140 measured, tag 70)", "half/double class", tout._bpm_agree(140.0, 70.0), False)
    rel = tout.key_match((0, "major"), (9, "minor"))
    case("I9", "C major measured, tag A minor: counted in accuracy as", "disagree (own class)",
         "%s -> agree=%s" % (rel, tout._key_agree(rel)), not tout._key_agree(rel))


# ------------------------------------------------------------------ I1, I2
def end_of_file():
    print("\n== I1 I2 end_cut / end_fade_ms")

    def ec(cid, name, x, want):
        r = tout.end_cut(SR, col(x))
        case(cid, name, "cut=%d" % want, "cut=%s fade=%s" % (r["end_cut"], r2(r["end_fade_ms"])),
             r["end_cut"] == want)

    k = st.kick(T(0.25), 150, 50, 0.25)
    ec("ok", "kick 250 ms, abrupt stop", k, 1)
    ec("ok", "kick, 2 ms fade", fade(k, 2), 1)
    for ms in (5, 10, 20, 30, 50):
        ec("I1", "kick 250 ms still loud, linear fade %d ms" % ms, fade(k, ms), 0)
    for ms in (5, 10):
        ec("ok", "kick, exponential fade %d ms" % ms, fade(k, ms, "exp"), 0)
    s808 = 0.8 * np.sin(2 * np.pi * 55 * T(0.6))
    for ms in (10, 20, 50):
        ec("ok", "808 55 Hz, linear fade %d ms" % ms, fade(s808, ms), 0)
    ec("ok", "808 stopped at a zero crossing", 0.8 * np.sin(2 * np.pi * 50 * T(0.6)), 1)
    ec("I2", "quiet 808 (-34 dBFS) stopped at a zero crossing", 0.02 * np.sin(2 * np.pi * 50 * T(0.6)), 1)
    ec("I2", "abrupt stop + 100 ms digital silence", np.concatenate([k, np.zeros(int(.1 * SR))]), 1)
    ec("I2", "noise reverb tail down to -51 dB, no fade",
       st.band_noise(SR, 200, 8000, 2) * np.exp(-T(1.0) / 0.17), 0)
    ec("I2", "kick, 20 ms fade, DC offset 0.01", fade(k, 20) + 0.01, 0)
    ec("I2", "seamless sustained pad loop (2 s)", 0.3 * st.harmonic(220, T(2.0), 4, 0.5), 0)
    lp = st.drum_loop(120.0, 1, 0.5, (0, 4, 8, 12), (4, 12), tuple(range(16)))
    r = tout.end_cut(SR, col(lp))
    case("I2", "drum loop ending on the bar: fade length", "no fade", "fade=%s" % r2(r["end_fade_ms"]),
         r["end_fade_ms"] is None)
    for ms in (100, 200):
        r = tout.end_cut(SR, col(fade(s808, ms)))
        got = r["end_fade_ms"]
        case("I2", "808, linear fade %d ms: fade length" % ms, "%d +-25 %%" % ms, r2(got),
             got is not None and abs(got - ms) < 0.25 * ms)


# ----------------------------------------------------------- I12, I13, ok
def levels_and_oneshots():
    print("\n== levels (I12 rms of multi-channel files)")
    s = 0.5 * np.sin(2 * np.pi * 440 * T(1.0))
    c = tout.common(SR, col(0.1 * np.sin(2 * np.pi * 1000 * T(5))))
    case("ok", "1 kHz amp 0.1 mono", "-20.0/-23.01/-23.0",
         "%s/%s/%s" % (r2(c["peak_dbfs"]), r2(c["rms_dbfs"]), r2(c["lufs_i"])), abs(c["lufs_i"] + 23) < 0.1)
    for name, x in (("mono", col(s)), ("stereo L=s R=s/2", np.stack([s, s * 0.5], 1)),
                    ("stereo antiphase", np.stack([s, -s], 1)), ("6 channels", np.stack([s] * 6, 1))):
        c = tout.common(SR, x)
        case("I12", "rms_dbfs of 440 Hz amp 0.5, " + name, "<= -9.03",
             "%s (peak %s)" % (r2(c["rms_dbfs"]), r2(c["peak_dbfs"])), c["rms_dbfs"] <= -9.0)

    print("\n== I13 one-shots")
    for midi in (24, 45, 69, 93):
        f = 440 * 2 ** ((midi - 69) / 12)
        t = T(1.5)
        m = tout.measure_oneshot(SR, col(0.4 * st.harmonic(f, t, 6, 0.6) * st.adsr(t, 0.01, 0.2, 0.6, 1.0, 0.3)),
                                 {"note": midi % 12})
        case("ok", "harmonic note midi %d" % midi, tout.midi_name(midi), m.get("note_measured"),
             m.get("note_measured") == tout.midi_name(midi))
    t = T(1.5)
    for midi, name in ((96, "C7"), (108, "C8")):
        f = 440 * 2 ** ((midi - 69) / 12)
        m = tout.measure_oneshot(SR, col(0.4 * st.harmonic(f, t, 2, 0.5) * st.adsr(t, 0.01, 0.2, 0.6, 1.0, 0.3)),
                                 {"note": 0})
        case("I13", "high note %s" % name, name + " 0 cents",
             "%s gap %s" % (m.get("note_measured"), r2(m.get("cents_gap"))),
             m.get("note_measured") == name and abs(m.get("cents_gap") or 99) < 5)
    x = sum(0.2 * st.harmonic(440 * 2 ** ((p - 69) / 12), t, 4, 0.5) for p in (57, 60, 64))
    m = tout.measure_oneshot(SR, col(x * st.adsr(t, 0.01, 0.2, 0.6, 1.0, 0.3)), {"note": None})
    case("I13", "A minor chord one-shot (A3 C4 E4)", "no note, or A", m.get("note_measured"),
         m.get("note_measured") is None or str(m.get("note_measured")).startswith("A"))
    t2 = T(2.0)
    x = 0.4 * st.harmonic(220, t2, 4, 0.5) * np.minimum(t2 / 1.0, 1.0)
    m = tout.measure_oneshot(SR, col(x), {"note": None})
    case("I13", "pad: 1 s attack then held to the end", "sustain about 0 dB",
         "sustain %s attack %s" % (r2(m.get("sustain_db")), r2(m.get("attack_ms"))),
         m.get("sustain_db") is not None and m["sustain_db"] > -6)
    th = T(0.5)
    for fh, fl, tau, note in ((120, 55, 0.08, "A1"), (200, 50, 0.03, None), (300, 45, 0.01, None)):
        m = tout.measure_drum_oneshot(SR, col(st.kick(th, fh, fl, tau)), "kick")
        got = m.get("drop_start_hz")
        case("I13", "kick %d->%d Hz tau %.2f: drop_start_hz" % (fh, fl, tau), "%d +-10 %%" % fh, r2(got),
             got is not None and abs(got - fh) < 0.1 * fh)
        if note:
            case("I13", "kick %d->%d Hz tau %.2f: note" % (fh, fl, tau), note, m.get("note_measured"),
                 m.get("note_measured") == note)
    for tau, want in ((0.01, "closed"), (0.03, "closed"), (0.15, "open"), (0.4, "open")):
        tt = T(max(0.1, tau * 8))
        m = tout.measure_drum_oneshot(SR, col(st.band_noise(len(tt), 6000, 16000, 1) * np.exp(-tt / tau)), "hat")
        case("ok", "hat noise burst tau %.2f s" % tau, want, m.get("hat_measured"), m.get("hat_measured") == want)


# ------------------------------------------------------------ I10, I11, fx
def midi_vocal_fx():
    print("\n== I10 MIDI")
    Q = 480

    def mm(notes, key=None):
        return tout.measure_midi_bytes(st.midi_bytes(notes, Q, 120.0), {"key": key})

    def chords(ch):
        return [(i * 4 * Q, 4 * Q - 10, p, 100, 0) for i, c in enumerate(ch) for p in c]

    m = mm(chords([tri(57, 1), tri(53, 0), tri(48, 0), tri(55, 0)]), (9, "minor"))
    case("ok", "Am F C G triads, tag A minor", "i-VI-III-VII 4 bars", "%s %s bars" % (m["chords"], r2(m["bars"])),
         m["chords"] == "i-VI-III-VII")
    m = mm(chords([[45, 52], [41, 48], [48, 55], [43, 50]]), (9, "minor"))
    case("I10", "power chords A5 F5 C5 G5, tag A minor", "A minor, no major/minor guess",
         "%s %s" % (m["key_measured"], m["chords"]), m["key_measured"] == "A minor")
    m = mm([(i * Q, Q - 10, p, 100, 0) for i, p in enumerate([45, 45, 48, 50, 52, 55, 45, 43] * 2)], (9, "minor"))
    case("I10", "mono bassline: chords column", "empty", m.get("chords"), not m.get("chords"))
    drums = ([(i * Q // 2, 60, 42, 90, 0) for i in range(16)] + [(i * Q, 60, 36, 110, 0) for i in range(8)]
             + [(Q + i * 2 * Q, 60, 38, 110, 0) for i in range(4)])
    m = mm(drums)
    case("I10", "drum pattern on channel 1 (pitches 36/38/42)", "drum_track 1, no key",
         "drum_track %s key %s chords %s" % (m.get("drum_track"), m.get("key_measured"), m.get("chords")),
         m.get("drum_track") == 1)
    m = mm([(int(i * Q / 3), 60, 60, 100, 0) for i in range(24)])
    case("I10", "8th-note triplets: swing_pct", "empty (not a swing)", r2(m.get("swing_pct")),
         m.get("swing_pct") is None)
    m = mm([(int((i // 2) * Q / 2 + (0 if i % 2 == 0 else 0.6 * Q / 2)), 40, 60, 100, 0) for i in range(32)])
    case("ok", "16ths with 60 % swing", "60.0", r2(m.get("swing_pct")), abs(m["swing_pct"] - 60) < 0.5)
    m = mm([(0, 16 * Q + Q // 4, 60, 100, 0)])
    case("min", "one note overhanging bar 4 by a 16th", "4 bars", r2(m.get("bars")), m.get("bars") == 4)

    print("\n== I11 vocal wet/dry")

    def phrase(f, dur):
        t = T(dur)
        return 0.4 * st.harmonic(f, t, 8, 0.7) * np.minimum(t / .03, 1) * np.minimum((dur - t) / .02, 1)

    def voc(rt60=None, mix=0.3, gap=0.6):
        x = np.concatenate([np.concatenate([phrase(f, 0.7), np.zeros(int(gap * SR))]) for f in (220, 247, 262, 220)])
        if rt60:
            n = int(rt60 * 1.5 * SR)
            ir = np.random.default_rng(1).standard_normal(n) * np.exp(-6.9 * np.arange(n) / SR / rt60)
            ir /= np.sqrt(np.sum(ir ** 2))
            size = 1 << int(np.ceil(np.log2(len(x) + n)))
            x = x + mix * np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:len(x)]
        return x

    dry = voc()
    delayed = dry.copy()
    d = int(.375 * SR)
    for k in range(1, 5):
        delayed[k * d:] += 0.5 ** k * dry[:-k * d]
    noisy = dry + 0.004 * np.random.default_rng(2).standard_normal(len(dry))
    for cid, name, x, want in [
        ("ok", "dry phrases with 600 ms gaps", dry, "dry"),
        ("ok", "room reverb RT60 0.4 s, mix 0.3", voc(0.4), "some"),
        ("I11", "hall reverb RT60 2 s, mix 0.3", voc(2.0), "wet"),
        ("I11", "hall reverb RT60 2 s, mix 0.05", voc(2.0, 0.05), "dry"),
        ("I11", "delay 375 ms with feedback", delayed, "wet"),
        ("I11", "dry + noise floor 40 dB under the voice", noisy, "dry"),
    ]:
        m = tout.measure_vocal(SR, col(x), {})
        wet = next((v for k, v in m.items() if isinstance(v, str) and v in ("dry", "some", "wet")), None)
        tail = next((v for k, v in m.items() if "tail" in k and isinstance(v, (float, np.floating))), None)
        case(cid, name, want, "%s (tail %s dB)" % (wet, r2(tail)), wet == want)

    print("\n== fx (found correct)")
    t = T(4.0)
    n = st.band_noise(len(t), 200, 12000, 3)
    for name, x, want in (("riser", n * (t / 4) ** 2, "rising"), ("downlifter", n * (1 - t / 4) ** 2, "falling"),
                          ("impact", n * np.exp(-t / 0.4), "falling"), ("flat noise", n * 0.3, "flat")):
        m = tout.measure_fx(SR, col(x), {"bpm": 120.0})
        case("ok", "fx " + name, want, m.get("level_shape"), m.get("level_shape") == want)


# ------------------------------------------------------------- robustness
def formats(base):
    print("\n== file formats and broken files")

    def wav(x, sr=SR, bits=16, fmt=1):
        x = col(x)
        if fmt == 3:
            data = x.astype("<f4").tobytes()
        elif bits == 16:
            data = (np.clip(x, -1, 1) * 32767).astype("<i2").tobytes()
        elif bits == 24:
            v = (np.clip(x, -1, 1) * 8388607).astype("<i4").reshape(-1)
            data = b"".join(struct.pack("<i", int(a))[:3] for a in v)
        else:
            data = (np.clip(x, -1, 1) * 2147483647).astype("<i4").tobytes()
        ba = x.shape[1] * bits // 8
        head = struct.pack("<HHIIHH", fmt, x.shape[1], sr, sr * ba, ba, bits)
        body = b"WAVE" + b"fmt " + struct.pack("<I", len(head)) + head + b"data" + struct.pack("<I", len(data)) + data
        return b"RIFF" + struct.pack("<I", len(body)) + body

    def aiff(x, bits=16):
        x = col(x)
        data = (np.clip(x, -1, 1) * 32767).astype(">i2").tobytes()
        comm = struct.pack(">hIh", x.shape[1], x.shape[0], bits) + struct.pack(">HQ", 16398, SR << 48)
        ssnd = struct.pack(">II", 0, 0) + data
        body = b"AIFF" + b"COMM" + struct.pack(">I", len(comm)) + comm + b"SSND" + struct.pack(">I", len(ssnd)) + ssnd
        return b"FORM" + struct.pack(">I", len(body)) + body

    def rd(data, name="x.wav"):
        path = os.path.join(base, name)
        with open(path, "wb") as f:
            f.write(data)
        try:
            sr, x, _ = tout.read_audio(path)
            c = tout.common(sr, x)
            return "peak %s rms %s frames %d" % (r2(c.get("peak_dbfs")), r2(c.get("rms_dbfs")), x.shape[0])
        except mesure.BadAudio as e:
            return "error cell: %s" % e
        except Exception as e:  # a crash is the defect we look for
            return "CRASH %s: %s" % (type(e).__name__, e)

    s = 0.5 * np.sin(2 * np.pi * 440 * T(1.0))
    for name, data, fname in (("wav 16-bit", wav(s), "x.wav"), ("wav 24-bit", wav(s, bits=24), "x.wav"),
                              ("wav 32-bit int", wav(s, bits=32), "x.wav"),
                              ("wav 32-bit float", wav(s, bits=32, fmt=3), "x.wav"), ("aiff 16-bit", aiff(s), "x.aif")):
        got = rd(data, fname)
        case("ok", name + ", 440 Hz amp 0.5", "peak -6.02 rms -9.03", got, got.startswith("peak -6.02 rms -9.03"))
    full = wav(s)
    nan = np.full(1000, np.nan, dtype=np.float32)
    for name, data in (("1 sample", wav(np.array([0.5]))), ("0 frames", wav(np.zeros(0))), ("zero-byte file", b""),
                       ("garbage bytes", bytes(range(256)) * 20), ("header cut at 30 bytes", full[:30]),
                       ("float NaN samples", wav(nan, bits=32, fmt=3)),
                       ("ADPCM format tag", full[:20] + struct.pack("<H", 2) + full[22:])):
        got = rd(data)
        case("ok", name, "error cell", got, got.startswith("error cell"))
    got = rd(full[:len(full) // 2])
    case("min", "wav truncated to half (data chunk longer than file)", "error cell or flag", got,
         got.startswith("error cell"))
    odd = full[:36] + b"LIST" + struct.pack("<I", 5) + b"INFOx" + full[36:]
    got = rd(odd)
    case("min", "odd-length LIST chunk without pad byte", "peak -6.02 (tolerant)", got, got.startswith("peak -6.02"))


# ------------------------------------------------------------ I14 self-test
def mutations():
    print("\n== I14 does the self-test notice a wrong measure? (one mutation per run)")
    names = ("common", "measure_oneshot", "measure_drum_loop", "measure_vocal", "measure_drum_oneshot")
    orig = {n: getattr(tout, n) for n in names}

    def run():
        st.COUNT[0] = 0
        st.FAILS.clear()
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            try:
                st.main([])
            except SystemExit:
                pass
        return len(st.FAILS)

    def scale(key, f):
        def change(r):
            v = r.get(key)
            if isinstance(v, (int, float, np.floating, np.integer)) and not isinstance(v, bool):
                r[key] = f(v)
        return change

    def setk(key, value):
        return lambda r: r.__setitem__(key, value)

    case("ok", "self-test unmodified", "0 failed", "%d failed" % run(), True)
    for name, fn, change in [
        ("loop bpm x2 (control)", "measure_drum_loop", scale("bpm_measured", lambda v: v * 2)),
        ("lufs +0.5 LU (control)", "common", scale("lufs_i", lambda v: v + 0.5)),
        ("dur_s x1.02", "common", scale("dur_s", lambda v: v * 1.02)),
        ("end_cut always 0 in every row", "common", setk("end_cut", 0)),
        ("end_cut always 1 in every row", "common", setk("end_cut", 1)),
        ("end_fade_ms x2 in every row", "common", scale("end_fade_ms", lambda v: v * 2)),
        ("one-shot cents_gap +15", "measure_oneshot", scale("cents_gap", lambda v: v + 15)),
        ("vocal tail level +10 dB", "measure_vocal", scale("tail_db", lambda v: v + 10)),
        ("kick drop_start_hz x1.3", "measure_drum_oneshot", scale("drop_start_hz", lambda v: v * 1.3)),
        ("drum t40_ms x1.5", "measure_drum_oneshot", scale("t40_ms", lambda v: v * 1.5)),
        ("drum onset_ms x3", "measure_drum_oneshot", scale("onset_ms", lambda v: v * 3 + 1)),
        ("drum t60_ms x1.5", "measure_drum_oneshot", scale("t60_ms", lambda v: v * 1.5)),
    ]:
        def wrapped(*a, _fn=fn, _change=change, **k):
            r = orig[_fn](*a, **k)
            if isinstance(r, dict):
                r = dict(r)
                _change(r)
            return r
        setattr(tout, fn, wrapped)
        try:
            fails = run()
        finally:
            setattr(tout, fn, orig[fn])
        case("I14", "mutation: " + name, "caught", "caught" if fails else "MISSED", fails > 0)


def main(argv):
    base = tempfile.mkdtemp(prefix="audit_tout_")
    try:
        tags_and_types()
        output_contract(os.path.join(base, "cli"))
        tempo_and_key()
        end_of_file()
        levels_and_oneshots()
        midi_vocal_fx()
        os.makedirs(os.path.join(base, "fmt"))
        formats(os.path.join(base, "fmt"))
        if "--mutations" in argv:
            mutations()
    finally:
        shutil.rmtree(base, ignore_errors=True)
    print("\n%d cases: %d still wrong (BAD), %d as expected (ok)" % (COUNT["BAD"] + COUNT["ok"], COUNT["BAD"], COUNT["ok"]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
