"""Self-test of lot E of the second audit of tout.py (docs/sons/AUDIT-TOUT-2.md):
MIDI and writing the tables (defects 21, 22, 27, 28, 32-37, 39).

usage: python3 -I scripts/sons/selftest_tout_e.py
Every check is built from a trap file of the audit replays (audit2/b5, b6, b7).
"""
import csv
import glob
import io
import contextlib
import math
import os
import shutil
import struct
import subprocess
import sys
import tempfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import tout  # noqa: E402
import mesure  # noqa: E402

SR = 48000
COUNT = [0]
FAILS = []


def check_eq(name, got, want):
    COUNT[0] += 1
    ok = got == want
    if not ok:
        FAILS.append(name)
    print("%s %-60s got %r  want %r" % ("ok  " if ok else "FAIL", name, got, want))


def check_true(name, ok):
    COUNT[0] += 1
    if not ok:
        FAILS.append(name)
    print("%s %s" % ("ok  " if ok else "FAIL", name))


def _raises(fn):
    try:
        fn()
    except tout.BadAudio as e:
        return str(e)
    return None


# ------------------------------------------------------------ builders

def vlq(n):
    out = [n & 0x7F]
    n >>= 7
    while n:
        out.append(0x80 | (n & 0x7F))
        n >>= 7
    return bytes(reversed(out))


def tempo_ev(bpm):
    return b"\xFF\x51\x03" + struct.pack(">I", int(round(60e6 / bpm)))[1:]


def ts_ev(num, den_pow):
    return b"\xFF\x58\x04" + bytes([num, den_pow, 24, 8])


def smf(notes, tpq=480, tempos=((0, 120.0),), ts=(4, 2), eot=True, off90=False, program=False):
    """notes: (start_beat, len_beats, pitch, vel, ch). tempos: (beat, bpm). ts None = no event.
    off90: note-offs written as note-on velocity 0. program: a program change first."""
    ev = []
    if program:
        ev.append((0, 0, bytes([0xC0, 5])))
    for beat, bpm in tempos:
        ev.append((int(round(beat * tpq)), 0, tempo_ev(bpm)))
    if ts is not None:
        ev.append((0, 0, ts_ev(*ts)))
    for s, ln, p, v, ch in notes:
        ev.append((int(round(s * tpq)), 2, bytes([0x90 | ch, p, v])))
        ev.append((int(round((s + ln) * tpq)), 1, bytes([(0x90 if off90 else 0x80) | ch, p, 0])))
    ev.sort(key=lambda e: (e[0], e[1]))
    trk, last = b"", 0
    for tick, _, msg in ev:
        trk += vlq(tick - last) + msg
        last = tick
    if eot:
        trk += vlq(0) + b"\xFF\x2F\x00"
    return b"MThd" + struct.pack(">IHHH", 6, 0, 1, tpq) + b"MTrk" + struct.pack(">I", len(trk)) + trk


def chords(start=0.0, bars=4):
    am = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
    return [(start + 4 * b, 4, p, 90, 0) for b in range(bars) for p in am[b % 4]]


def wav_bytes(x, sr=SR, bits=16):
    x = np.asarray(x, dtype=float).reshape(len(x), -1)
    ch = x.shape[1]
    if bits == 16:
        data = (np.clip(x, -1, 1) * 32767).astype("<i2").tobytes()
    else:
        raise ValueError(bits)
    fmt = struct.pack("<HHIIHH", 1, ch, sr, sr * ch * 2, ch * 2, 16)
    body = b"WAVE" + b"fmt " + struct.pack("<I", 16) + fmt + b"data" + struct.pack("<I", len(data)) + data
    return b"RIFF" + struct.pack("<I", len(body)) + body


def _ext80(v):
    exp = int(math.floor(math.log2(v)))
    mant = int(v / 2 ** exp * 2 ** 63)
    return struct.pack(">HQ", exp + 16383, mant)


def aiff_bytes(x, sr=SR):
    x = np.asarray(x, dtype=float)
    data = (np.clip(x, -1, 1) * 32767).astype(">i2").tobytes()
    comm = struct.pack(">hIh", 1, len(x), 16) + _ext80(sr)
    ssnd = struct.pack(">II", 0, 0) + data
    body = b"AIFF" + b"COMM" + struct.pack(">I", len(comm)) + comm + b"SSND" + struct.pack(">I", len(ssnd)) + ssnd
    return b"FORM" + struct.pack(">I", len(body)) + body


def kick(n=SR // 2):
    t = np.arange(n) / SR
    return 0.9 * np.sin(2 * np.pi * (50 + 100 * np.exp(-t * 30)) * t) * np.exp(-t * 8)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(data)


def run_main(args):
    buf = io.StringIO()
    code = None
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        try:
            code = tout.main(args)
        except SystemExit as e:
            code = ("exit", e.code)
    return code, buf.getvalue()


def read_table(path):
    with open(path, encoding="utf-8") as fh:
        return list(csv.DictReader(fh))


def midi_row(tmp, rel, data):
    p = os.path.join(tmp, "pack_" + str(COUNT[0]), rel)
    write(p, data)
    acc = tout.Accuracy()
    parts = rel.split("/")
    return tout._midi_row(p, rel, tout.parse_tags(parts, "midi"), "1", acc)


# ------------------------------------------------------------ tests

def test_21_37a_midi_tempo(tmp=None):
    """21: the tempo of the name is reported; 37a: a tempo change gives no single file tempo."""
    tmp = tempfile.mkdtemp()
    try:
        r = midi_row(tmp, "MIDI/Chords 140 bpm Am.mid", smf(chords(), tempos=()))
        check_eq("21 name 140, no tempo event: bpm_tag", r.get("bpm_tag"), 140.0)
        check_eq("21 name 140, no tempo event: bpm_est", r.get("bpm_est"), 140.0)
        r = midi_row(tmp, "MIDI/Chords 140 bpm Am.mid", smf(chords(), tempos=((0, 120.0),)))
        check_eq("21 name 140, file 120: bpm_tag", r.get("bpm_tag"), 140.0)
        check_eq("21 name 140, file 120: bpm_est follows the name", r.get("bpm_est"), 140.0)
        check_eq("21 name 140, file 120: bpm_file stays the file's", round(r.get("bpm_file") or 0, 3), 120.0)
        hats = [(i * 0.25, 0.2, 42, 100, 9) for i in range(32)]
        r = midi_row(tmp, "Trap MIDI/Hi-Hat Roll 140bpm.mid", smf(hats, tempos=()))
        check_eq("21 Hi-Hat Roll 140bpm: bpm_tag", r.get("bpm_tag"), 140.0)
        r = midi_row(tmp, "MIDI/Chords Am.mid", smf(chords(), tempos=((0, 90.0), (8, 180.0))))
        check_eq("37a tempo change 90->180 at beat 8: bpm_file empty", r.get("bpm_file"), None)
        check_eq("37a tempo change: bpm_est empty", r.get("bpm_est"), None)
        r = midi_row(tmp, "MIDI/Chords Am.mid", smf(chords(), tempos=((0, 90.0), (64, 180.0))))
        check_eq("37a tempo event after the last note: bpm_file kept", round(r.get("bpm_file") or 0, 3), 90.0)
        r = midi_row(tmp, "MIDI/Chords Am.mid", smf(chords(), tempos=((0, 124.0), (4, 124.0))))
        check_eq("37a same tempo repeated: bpm_file kept", round(r.get("bpm_file") or 0, 3), 124.0)
        # end to end: the name tempo reaches the midi table
        pack = os.path.join(tmp, "pack_cli")
        write(os.path.join(pack, "MIDI", "Chords 140 bpm Am.mid"), smf(chords(), tempos=((0, 120.0),)))
        out = os.path.join(tmp, "out", "t")
        os.makedirs(os.path.dirname(out))
        code, _ = run_main([pack, "--rar", "1", "--out", out])
        rows = read_table(out + "_midi.csv")
        check_eq("21 cli: midi table bpm_tag", rows[0]["bpm_tag"], "140")
        check_eq("21 cli: midi table bpm_est", rows[0]["bpm_est"], "140")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def test_22_37b_midi_bars():
    """22: bars counted from the first note, legato is not a new bar or more voices;
    37b: 3/4 without a time signature leaves the bar cells empty."""
    am = {"bpm": None, "key": (9, "minor")}
    m = tout.measure_midi_bytes(smf(chords()), am)
    check_eq("22 reference 4 bars: bars", m["bars"], 4)
    check_eq("22 reference: swing 50 when nothing is off the 8ths", m["swing_pct"], 50.0)
    m = tout.measure_midi_bytes(smf(chords(start=32.0)), am)
    check_eq("22 Chords 05 exported from bar 9: bars", m["bars"], 4)
    check_eq("22 Chords 05: notes per bar", m["notes_per_bar"], 3.0)
    check_eq("22 Chords 05: chords from its first bar", m["chords"], "i-VI-III-VII")
    check_eq("22 Chords 05: chord length 4 beats", m["chord_len_beats"], 4.0)
    m = tout.measure_midi_bytes(smf(chords(start=1.0)), am)
    check_eq("22 Chords 06 starting on beat 2: bars", m["bars"], 4)
    check_eq("22 Chords 06: chords", m["chords"], "i-VI-III-VII")
    legato = [(s, ln + 0.25, p, v, c) for s, ln, p, v, c in chords()]
    m = tout.measure_midi_bytes(smf(legato), am)
    check_eq("22 Chords 07 legato a 16th: bars", m["bars"], 4)
    check_eq("22 Chords 07 legato a 16th: poly_max", m["poly_max"], 3)
    rel = [(s, ln + 0.05, p, v, c) for s, ln, p, v, c in chords()]
    m = tout.measure_midi_bytes(smf(rel), am)
    check_eq("22 release a 64th past the bar line: bars", m["bars"], 4)
    beat = []
    for k in (0, 2):
        beat.append((k, 0.25, 36, 110, 9))
    for k in (1, 3):
        beat.append((k, 0.25, 38, 100, 9))
    beat += [(h * 0.5, 0.2, 42, 80, 9) for h in range(8)] + [(4, 0.25, 36, 110, 9)]
    m = tout.measure_midi_bytes(smf(beat), {})
    check_eq("22 Beat 02: 2nd bar holds only a kick on beat 1: bars", m["bars"], 2)
    check_eq("22 Beat 02: drum track", m["drum_track"], 1)
    mixed = beat + [(0, 2, 33, 100, 0)]
    m = tout.measure_midi_bytes(smf(mixed), {})
    check_eq("drums plus a bass line is not a drum track", m["drum_track"], 0)
    waltz = [(3 * b, 3, p, 90, 0) for b in range(4) for p in (57, 60, 64)]
    m = tout.measure_midi_bytes(smf(waltz, ts=(3, 2)), {})
    check_eq("37b Waltz 01 with 3/4 signature: bars", m["bars"], 4)
    m = tout.measure_midi_bytes(smf(waltz, ts=None), {})
    check_eq("37b Waltz 02 no signature: bars left empty", m["bars"], None)
    check_eq("37b Waltz 02 no signature: notes per bar left empty", m["notes_per_bar"], None)
    m = tout.measure_midi_bytes(smf(chords(), ts=None), am)
    check_eq("37b 4/4 chords without a signature: bars", m["bars"], 4)
    m = tout.measure_midi_bytes(smf(chords(), program=True, off90=True), am)
    check_eq("program change and note-on 0 offs: notes", m["notes"], 12)
    check_eq("program change and note-on 0 offs: poly_max", m["poly_max"], 3)
    check_eq("program change and note-on 0 offs: bars", m["bars"], 4)


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    with np.errstate(all="ignore"):
        for fn in tests:
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
