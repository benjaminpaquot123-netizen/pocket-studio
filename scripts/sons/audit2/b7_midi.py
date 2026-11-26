import os, sys, struct, shutil
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *
from lib import _vlq

B = os.path.join(S, "pack", "b7")
shutil.rmtree(B, ignore_errors=True)
N = [0]
COLS = ["type", "role", "style", "bpm_tag", "key_tag", "bpm_est", "bpm_file", "bars", "notes", "notes_per_bar",
        "pitch_min", "pitch_max", "poly_max", "swing_pct", "scale_fit", "drum_track", "error"]

def run(label, rel, data=None, notes=None, cols=COLS, **kw):
    N[0] += 1
    root = os.path.join(B, "%03d" % N[0])
    p = os.path.join(root, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    if data is not None:
        open(p, "wb").write(data)
    else:
        write_midi(p, notes, **kw)
    rows, con, code = run_pack(root, "b7")
    print("%-52s %s" % (label, rel))
    if not rows:
        print("      NO ROW | console:", con.replace("\n", " | "), "| code", code)
    for r in rows:
        print("      " + " ".join("%s=%s" % (c, r[c]) for c in cols if c in r and r[c] != ""))

def chords(start=0.0, bars=4, ch=0):
    out = []
    prog = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
    for b in range(bars):
        for p in prog[b % 4]:
            out.append((start + 4 * b, 4, p, 90, ch))
    return out

def drums(bars=2, ch=9, start=0.0):
    out = []
    for b in range(bars):
        o = start + 4 * b
        for k in (0, 2): out.append((o + k, 0.25, 36, 110, ch))
        for s in (1, 3): out.append((o + s, 0.25, 38, 100, ch))
        for h in range(8): out.append((o + h * 0.5, 0.2, 42, 80, ch))
    return out

def trk(events, ppq=480):
    """events: (beat, bytes) -> MTrk chunk"""
    ev = sorted(((int(round(b * ppq)), d) for b, d in events), key=lambda e: e[0])
    out, last = b"", 0
    for t, d in ev:
        out += _vlq(t - last) + d
        last = t
    out += b"\x00\xff\x2f\x00"
    return b"MTrk" + struct.pack(">I", len(out)) + out

def smf(tracks, fmt=1, div=480):
    return b"MThd" + struct.pack(">IHHH", 6, fmt, len(tracks), div) + b"".join(tracks)

def tempo_ev(bpm): return b"\xff\x51\x03" + struct.pack(">I", int(60e6 / bpm))[1:]
def ts_ev(n, dpow): return b"\xff\x58\x04" + bytes([n, dpow, 24, 8])
def note_evs(notes):
    out = []
    for s, l, p, v, c in notes:
        out.append((s, bytes([0x90 | c, p, v]))); out.append((s + l - 0.001, bytes([0x80 | c, p, 0])))
    return out

print("== tempo: name versus file")
run("no tempo in file, '140 bpm' in the name", "MIDI/Chords 140 bpm Am.mid", notes=chords())
run("file tempo 120 (DAW default), name says 140 bpm", "MIDI/Chords 140 bpm Am.mid", notes=chords(), bpm=120)
run("file tempo 140, no tag in the name", "MIDI/Chords 01.mid", notes=chords(), bpm=140)
run("name 'Chords Am 120' (bare number + key)", "MIDI/Chords Am 120.mid", notes=chords())
run("name 'Chords 120' (bare number only)", "MIDI/Chords 120.mid", notes=chords())
run("tempo 93.5 in file", "MIDI/Chords 02.mid", notes=chords(), bpm=93.5)
run("tempo change at bar 3 (90 then 180)", "MIDI/Chords 03.mid",
    data=smf([trk([(0, tempo_ev(90)), (8, tempo_ev(180))] + note_evs(chords()))], fmt=0))

print("== bars")
run("4 bars, start at bar 1 (reference)", "MIDI/Chords 04.mid", notes=chords(), bpm=120)
run("4 bars exported from bar 9 of the song", "MIDI/Chords 05.mid", notes=chords(start=32.0), bpm=120)
run("4 bars, pattern starts on beat 2 (pickup 1 beat late)", "MIDI/Chords 06.mid", notes=chords(start=1.0), bpm=120)
run("drums 2 bars (last hit before the bar line)", "MIDI/Drums/Beat 01.mid", notes=drums(2), bpm=120)
run("drums 2 bars, last bar only has a kick on beat 1", "MIDI/Drums/Beat 02.mid",
    notes=drums(1) + [(4, 0.25, 36, 110, 9)], bpm=120)
run("3/4: 4 bars of 3 beats (12 beats)", "MIDI/Waltz 01.mid",
    data=smf([trk([(0, tempo_ev(120)), (0, ts_ev(3, 2))] + note_evs(
        [(3 * b, 3, p, 90, 0) for b in range(4) for p in (57, 60, 64)]))], fmt=0))
run("same 12 beats, no time signature in the file", "MIDI/Waltz 02.mid",
    notes=[(3 * b, 3, p, 90, 0) for b in range(4) for p in (57, 60, 64)], bpm=120)
run("6/8: 4 bars (12 beats)", "MIDI/Six 01.mid",
    data=smf([trk([(0, tempo_ev(120)), (0, ts_ev(6, 3))] + note_evs(
        [(3 * b, 3, p, 90, 0) for b in range(4) for p in (57, 60, 64)]))], fmt=0))
run("legato: each chord overlaps the next by a 16th", "MIDI/Chords 07.mid",
    notes=[(s, l + 0.25, p, v, c) for s, l, p, v, c in chords()], bpm=120)

print("== format 1, several tracks")
run("fmt 1: tempo track + chords track", "MIDI/Chords 08.mid",
    data=smf([trk([(0, tempo_ev(128))]), trk(note_evs(chords()))]))
run("fmt 1: drums (ch 10) + bass (ch 1) in one file", "MIDI/Full Beat 01.mid",
    data=smf([trk([(0, tempo_ev(128))]), trk(note_evs(drums(4))), trk(note_evs([(4 * b, 2, 33, 100, 0) for b in range(4)]))]))
run("drums written on channel 1 (very common)", "MIDI/Drums/Beat 03.mid", notes=drums(2, ch=0), bpm=120)
run("drums ch 1, folder 'Drum MIDI', file 'Kick Pattern'", "Drum MIDI/Kick Pattern 01.mid",
    notes=[(b, 0.25, 36, 110, 0) for b in range(8)], bpm=120)
run("hat pattern in swing 58 % on ch 10", "MIDI/Drums/Hats 01.mid",
    notes=[(h * 0.5 + (0.04 if h % 2 else 0), 0.1, 42, 80, 9) for h in range(16)] +
          [(q * 0.5 + 0.29, 0.1, 42, 60, 9) for q in range(16)], bpm=120)
run("straight 8ths only (no 16th) -> swing?", "MIDI/Drums/Hats 02.mid",
    notes=[(h * 0.5, 0.1, 42, 80, 9) for h in range(16)], bpm=120)

print("== ppq and resolution")
run("ppq 96", "MIDI/Chords 09.mid", notes=chords(), bpm=120, ppq=96)
run("ppq 960", "MIDI/Chords 10.mid", notes=chords(), bpm=120, ppq=960)
run("ppq 24 (old drum machine)", "MIDI/Drums/Beat 04.mid", notes=drums(2), bpm=120, ppq=24)

print("== weird or broken")
good = smf([trk([(0, tempo_ev(120))] + note_evs(chords()))], fmt=0)
run("garbage text named .mid", "MIDI/Junk.mid", data=b"this is not midi at all" * 10)
run("empty file", "MIDI/Empty.mid", data=b"")
run("header only, no track", "MIDI/Head.mid", data=good[:14])
run("truncated in the middle of the track", "MIDI/Trunc.mid", data=good[:len(good) // 2])
run("no notes (only tempo)", "MIDI/NoNotes.mid", data=smf([trk([(0, tempo_ev(120))])], fmt=0))
run("SMPTE time division", "MIDI/Smpte.mid", data=good[:12] + struct.pack(">H", 0xE728) + good[14:])
run("RIFF-wrapped midi (.mid holding RMID)", "MIDI/Rmid.mid", data=b"RIFF" + struct.pack("<I", len(good) + 12) + b"RMIDdata" + struct.pack("<I", len(good)) + good)
run("uppercase extension .MID", "MIDI/Chords 11.MID", data=good)
run(".midi extension", "MIDI/Chords 12.midi", data=good)
run("notes never released (stuck notes)", "MIDI/Stuck.mid",
    data=smf([trk([(0, tempo_ev(120))] + [(4 * b, bytes([0x90, 57 + b, 90])) for b in range(4)])], fmt=0))
run("tempo 0 microseconds", "MIDI/Tempo0.mid", data=smf([trk([(0, b"\xff\x51\x03\x00\x00\x00")] + note_evs(chords()))], fmt=0))
run("one single note of 1 tick", "MIDI/One.mid", notes=[(0, 0.002, 60, 100, 0)], bpm=120)
run("pitch bend + CC + program change + sysex mixed in", "MIDI/Ctl.mid",
    data=smf([trk([(0, tempo_ev(120)), (0, b"\xc0\x05"), (0, b"\xb0\x07\x64"), (0.5, b"\xe0\x00\x40"),
                   (0.6, b"\xf0\x03\x7e\x7f\xf7"), (0.7, b"\xd0\x40")] + note_evs(chords()))], fmt=0))
run("running status, note-on velocity 0 as note-off", "MIDI/Run.mid",
    data=smf([b"MTrk" + struct.pack(">I", 4 + 3 + 3 * 8 + 1 + 4 - 1 + 1) + b"\x00\xff\x51\x03\x07\xa1\x20" +
              b"\x00\x90\x3c\x64" + b"\x83\x60\x3c\x00" + b"\x00\x3e\x64" + b"\x83\x60\x3e\x00" +
              b"\x00\x40\x64" + b"\x83\x60\x40\x00" + b"\x00\x41\x64" + b"\x83\x60\x41\x00" + b"\x00\xff\x2f\x00"], fmt=0))

print("== midi names: role / style / key")
run("'Am' in the name, notes in A minor", "MIDI/Piano Chords Am.mid", notes=chords(), bpm=120)
run("'C' alone in the name", "MIDI/Piano Chords C.mid", notes=chords(), bpm=120)
run("melody folder, bass line", "Melodies MIDI/Bassline 01 Gm 124.mid", notes=[(b, 0.5, 43, 100, 0) for b in range(16)], bpm=124)
run("'Trap Hi-Hat Roll 140bpm' on ch 10", "Trap MIDI/Hi-Hat Roll 140bpm.mid", notes=[(h * 0.25, 0.1, 42, 80, 9) for h in range(32)])
