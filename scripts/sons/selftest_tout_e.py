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


def test_34_35_truncated_and_dc():
    """34: truncated WAV / AIFF / MIDI flagged; 35: a constant DC offset is no sound."""
    tmp = tempfile.mkdtemp()
    try:
        def path(name, data):
            p = os.path.join(tmp, name)
            write(p, data)
            return p
        k = kick()
        good = wav_bytes(k)
        check_eq("34 whole wav reads", _raises(lambda: tout.read_audio(path("ok.wav", good))), None)
        e = _raises(lambda: tout.read_audio(path("t.wav", good[:44 + 9600])))
        check_true("34 wav cut in the data (0.1 s of 0.5 s) flagged truncated: %r" % e, bool(e) and "truncated" in e)
        e = _raises(lambda: tout.read_audio(path("t2.wav", good[:-100])))
        check_eq("34 wav missing 100 bytes of 48000 still reads", e, None)
        streamed = good[:40] + struct.pack("<I", 0xFFFFFFFF) + good[44:]
        check_eq("34 streamed wav (data size 0xFFFFFFFF) still reads",
                 _raises(lambda: tout.read_audio(path("s.wav", streamed))), None)
        a = aiff_bytes(k)
        check_eq("34 whole aiff reads", _raises(lambda: tout.read_audio(path("ok.aif", a))), None)
        e = _raises(lambda: tout.read_audio(path("t.aif", a[:300])))
        check_true("34 aiff cut at 300 bytes flagged truncated: %r" % e, bool(e) and "truncated" in e)
        e = None
        try:
            mesure.read_audio(path("t3.wav", good[:44 + 9600]))
        except mesure.BadAudio as ex:
            e = str(ex)
        check_true("34 mesure.py flags the truncated wav too: %r" % e, bool(e) and "truncated" in e)
        mid = smf(chords())
        e = _raises(lambda: tout.measure_midi_bytes(mid[:len(mid) // 2], {}))
        check_true("34 Trunc.mid cut in the middle of the track flagged: %r" % e, bool(e) and "truncated" in e)
        check_eq("34 whole midi reads", _raises(lambda: tout.measure_midi_bytes(mid, {})), None)
        no_eot = smf(chords(), eot=False)
        check_eq("34 midi without end-of-track but whole track still reads",
                 _raises(lambda: tout.measure_midi_bytes(no_eot, {})), None)
        e = _raises(lambda: tout.read_audio(path("dc.wav", wav_bytes(np.full(SR, 0.5)))))
        check_true("35 constant DC 0.5 for 1 s flagged: %r" % e, bool(e) and "dc" in e.lower())
        e = _raises(lambda: tout.read_audio(path("kdc.wav", wav_bytes(k + 0.05))))
        check_eq("35 kick with a small DC offset still reads", e, None)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def test_27_33_36_main_output():
    tmp = tempfile.mkdtemp(prefix="tout_e_main_")
    try:
        pack = os.path.join(tmp, "pack")
        write(os.path.join(pack, "Kicks", "Kick 01.wav"), wav_bytes(kick()))
        # 27: --out in a folder that does not exist is refused before measuring
        code, out = run_main([pack, "--rar", "1", "--out", os.path.join(tmp, "nope", "t")])
        check_eq("27 missing out folder refused", code, ("exit", 2))
        check_true("27 message names the folder", "folder" in out and "nope" in out)
        # 33: a table of a previous run is removed when the type has no row any more
        outp = os.path.join(tmp, "out", "t")
        os.makedirs(os.path.dirname(outp))
        stale = outp + "_midi.csv"
        open(stale, "w").write("type\nmidi\n")
        code, out = run_main([pack, "--rar", "1", "--out", outp])
        check_eq("33 run ok", code, 0)
        check_true("33 stale midi table removed", not os.path.exists(stale))
        check_true("33 drum table written", os.path.exists(outp + "_drum_oneshot.csv"))
        # 36: a pack of formats it cannot read says so and fails
        only = os.path.join(tmp, "flac")
        write(os.path.join(only, "Loops", "Loop 01.flac"), b"fLaC" + b"\0" * 100)
        write(os.path.join(only, "Loops", "Loop 02.mp3"), b"ID3" + b"\0" * 100)
        code, out = run_main([only, "--rar", "1", "--out", os.path.join(tmp, "out", "f")])
        check_eq("36 no measurable file -> code 1", code, 1)
        check_true("36 says no wav/aiff/midi", "no wav" in out.lower())
        check_true("36 names the skipped formats", ".flac 1" in out and ".mp3 1" in out)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def test_32_write_cells():
    f = tout._fmt
    check_eq("32 9999.7 kept", f(9999.7), "9999.7")
    check_eq("32 123.45 kept", f(123.45), "123.45")
    check_eq("32 -0.0 -> 0", f(-0.0), "0")
    check_eq("32 1.234 four digits", f(1.234), "1.234")
    check_eq("32 0.5", f(0.5), "0.5")
    check_eq("32 120.0", f(120.0), "120")
    check_eq("32 12345.67", f(12345.67), "12345.7")
    check_eq("32 nan empty", f(float("nan")), "")
    check_eq("32 inf empty", f(float("inf")), "")
    check_eq("write '+1' quoted", f("+1"), "'+1")
    check_eq("write '-A1' quoted", f("-A1"), "'-A1")
    tmp = tempfile.mkdtemp(prefix="tout_e_w_")
    try:
        write(os.path.join(tmp, "Kicks", "._Kick 01.wav"), wav_bytes(kick()))
        write(os.path.join(tmp, "Kicks", "Kick 01.wav"), wav_bytes(kick()))
        audio, midi, skipped = tout._walk(tmp)
        check_eq("write hidden '._' file skipped", [os.path.basename(a) for a in audio], ["Kick 01.wav"])
        p = os.path.join(tmp, "Loops", "Pad 61s.wav")
        write(p, wav_bytes(0.1 * np.sin(np.arange(61 * 8000) * 0.05), sr=8000))
        check_eq("write a 61 s file is read", _raises(lambda: tout.read_audio(p)), None)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def test_39_guard_without_git():
    tmp = tempfile.mkdtemp(prefix="tout_e_g_")
    try:
        repo = os.path.join(tmp, "repo")
        os.makedirs(os.path.join(repo, "scripts", "sons"))
        os.makedirs(os.path.join(repo, "docs", "sons"))
        open(os.path.join(repo, "CLAUDE.md"), "w").write("x")
        for f in ("tout.py", "mesure.py", "mine_or.py"):
            shutil.copy(os.path.join(HERE, f), os.path.join(repo, "scripts", "sons", f))
        pack = os.path.join(tmp, "pack")
        write(os.path.join(pack, "Kicks", "Kick 01.wav"), wav_bytes(kick()))
        for name, out in (("tout", os.path.join(repo, "docs", "sons", "t")),):
            r = subprocess.run([sys.executable, "-I", os.path.join(repo, "scripts", "sons", name + ".py"),
                                pack, "--rar", "1", "--out", out], capture_output=True, text=True, timeout=120)
            check_eq("39 %s refuses inside a copy without .git" % name, r.returncode, 2)
            check_true("39 %s wrote nothing" % name, not glob.glob(out + "_*.csv"))
        r = subprocess.run([sys.executable, "-I", os.path.join(repo, "scripts", "sons", "mesure.py"),
                            pack, "--out", os.path.join(repo, "docs", "sons", "m.csv")],
                           capture_output=True, text=True, timeout=120)
        check_eq("39 mesure refuses inside a copy without .git", r.returncode, 2)
        out = os.path.join(tmp, "elsewhere", "t")
        os.makedirs(os.path.dirname(out))
        r = subprocess.run([sys.executable, "-I", os.path.join(repo, "scripts", "sons", "tout.py"),
                            pack, "--rar", "1", "--out", out], capture_output=True, text=True, timeout=120)
        check_eq("39 outside the copy still works", r.returncode, 0)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


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
