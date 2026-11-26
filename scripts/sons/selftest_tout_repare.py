#!/usr/bin/env python3
"""Self-test of the repair of tout.py after audit 3 (the reunion's defects).

    python3 -I scripts/sons/selftest_tout_repare.py

Same frame as selftest_tout_reuni.py: the real command line on small synthetic
packs written to a temporary folder outside the repository. Exit code 1 when a
single check fails. No sample is needed.
"""
import contextlib
import csv
import glob
import io
import os
import shutil
import sys
import tempfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import tout  # noqa: E402
import selftest_tout as st  # noqa: E402  (generators only)
import selftest_tout_reuni as ru  # noqa: E402  (run() and generators only)

SR = st.SR
FAILS = []
COUNT = [0]
num = ru.num
run = ru.run
AM, FMAJ = ru.AM, ru.FMAJ


def check_eq(name, got, want):
    COUNT[0] += 1
    ok = got == want
    if not ok:
        FAILS.append(name)
    print("%s %-64s got %r  want %r" % ("ok  " if ok else "FAIL", name, got, want))


def check_true(name, ok, got=None):
    COUNT[0] += 1
    if not ok:
        FAILS.append(name)
    print("%s %-64s%s" % ("ok  " if ok else "FAIL", name, "" if ok or got is None else "  got %r" % (got,)))


def kit_files(folder, bpm=128.0, chords=FMAJ, midi=None):
    n = len(ru.drums(bpm))
    files = {folder + "Drums.wav": ru.drums(bpm), folder + "Pad.wav": ru.plucks(bpm, chords, 4)[:n],
             folder + "Bass.wav": ru.plucks(bpm, [[c[0] - 24] for c in chords], 4)[:n]}
    if midi:
        files[folder + midi] = st.midi_bytes([(b * 1920, 1920, p, 90, 0) for b, ch in enumerate(chords) for p in ch],
                                             480, bpm)
    return files


def test_kit_midi_key():
    """Defect 1: a kit with no key written gives its MIDI no guessed key."""
    files = kit_files("Kits/Kit 02/", midi="Kit 02.mid")
    rows, _, _, code, _ = run(files)
    check_eq("1 run ends well", code, 0)
    r = rows["Kits/Kit 02/Kit 02.mid"] or {}
    check_eq("1 kit midi without a written key: no key_tag", r.get("key_tag"), "")
    check_eq("1 kit midi without a written key: no scale_fit", r.get("scale_fit"), "")
    files = kit_files("Kits/Kit 01 - 128 F/", midi="Kit 01.mid")
    rows, _, _, code, _ = run(files)
    r = rows["Kits/Kit 01 - 128 F/Kit 01.mid"] or {}
    check_eq("1 kit midi with a key written on the kit: key_tag kept", r.get("key_tag"), "F major")


def test_drum_stem_no_key():
    """Defect 7: the drum stem of a kit written in a key carries no key; the pad keeps it."""
    for folder in ("Kits/Kit 01 - 128 F/", "Kits/Kit 04 - 128 bpm Fm/", "Kits/Kit 05 - 64 Am/"):
        rows, _, _, code, _ = run(kit_files(folder))
        r = rows[folder + "Drums.wav"] or {}
        check_eq("7 %s drums: no key_tag" % folder, r.get("key_tag"), "")
        check_eq("7 %s drums: no key cells" % folder, (r.get("key_est"), r.get("key_match")), ("", ""))
    r = rows["Kits/Kit 05 - 64 Am/Pad.wav"] or {}
    check_true("7 the pad of a kit keeps the written key", r.get("key_tag") not in ("", None), r.get("key_tag"))


def test_bare_equal_first():
    """Defect 6: two bare numbers both near the heard tempo: the equal one wins over half or double."""
    files = {"Loops/Loop 70 Am 140.wav": ru.plucks(140.0, AM, 4), "Loops/Loop 85 Am 140.wav": ru.plucks(140.0, AM, 4),
             "Loops/Loop 70 Am 140 B.wav": ru.plucks(70.0, AM, 2)}
    rows, _, _, code, _ = run(files)
    check_eq("6 run ends well", code, 0)
    check_eq("6 'Loop 70 Am 140' heard at 140: tempo 140", num((rows["Loops/Loop 70 Am 140.wav"] or {}).get("bpm_tag")), 140.0)
    check_eq("6 'Loop 85 Am 140' heard at 140: tempo 140", num((rows["Loops/Loop 85 Am 140.wav"] or {}).get("bpm_tag")), 140.0)
    check_eq("6 'Loop 70 Am 140' heard at 70: tempo 70",
             num((rows["Loops/Loop 70 Am 140 B.wav"] or {}).get("bpm_tag")), 70.0)


def test_bars_when_tempo_contradicted():
    """Defect 5: a written tempo the sound contradicts (neither equal, half nor double) gives no bars."""
    files = {"Drum Loops/Drum Loop 128 bpm.wav": ru.drums(100.0, 4),
             "Loops/Pad Loop 140bpm Am.wav": ru.plucks(100.0, AM, 1),
             "Drum Loops/Drum Loop 120 bpm.wav": ru.drums(120.0, 4),
             "Drum Loops/Half Loop 140 bpm.wav": ru.drums(70.0, 2),
             "Loops/Pad Loop 120bpm Am.wav": ru.plucks(120.0, AM, 1)}
    rows, _, _, code, _ = run(files)
    check_eq("5 run ends well", code, 0)
    check_eq("5 'Drum Loop 128 bpm' played at 100: no bars", (rows["Drum Loops/Drum Loop 128 bpm.wav"] or {}).get("bars"), "")
    check_eq("5 'Pad Loop 140bpm Am' played at 100: no bars", (rows["Loops/Pad Loop 140bpm Am.wav"] or {}).get("bars"), "")
    check_eq("5 'Drum Loop 120 bpm' played at 120: 4 bars", num((rows["Drum Loops/Drum Loop 120 bpm.wav"] or {}).get("bars")), 4.0)
    check_true("5 'Half Loop 140 bpm' heard at half: bars kept",
               (rows["Drum Loops/Half Loop 140 bpm.wav"] or {}).get("bars") not in ("", None))
    check_eq("5 'Pad Loop 120bpm Am' played at 120: 4 bars", num((rows["Loops/Pad Loop 120bpm Am.wav"] or {}).get("bars")), 4.0)


def test_lone_letter_root():
    """Defect 8: a lone letter confirmed by the sound only wrote the root: key_match 'root', not 'exact'."""
    rows, _, _, code, _ = run({"Loops/Pad Loop F.wav": ru.plucks(120.0, FMAJ, 1),
                               "Loops/Pad Loop F major.wav": ru.plucks(120.0, FMAJ, 1)})
    r = rows["Loops/Pad Loop F.wav"] or {}
    check_eq("8 lone F heard in F major: key_tag", r.get("key_tag"), "F major")
    check_eq("8 lone F heard in F major: key_match root", r.get("key_match"), "root")
    check_eq("8 'F major' written in full: key_match exact", (rows["Loops/Pad Loop F major.wav"] or {}).get("key_match"), "exact")
    check_true("8 the module text names the match 'root'", "root" in tout.__doc__.split("key_match (")[1].split(")")[0])


def test_oneshot_with_tempo():
    """Defect 2: a melodic one-shot with a tempo in its name stays a one-shot, its note kept."""
    held = ru.tone(130.81, 1.5)
    files = {"One Shots/Bass C2 120bpm.wav": held, "Bass One Shots/Bass C2 120bpm.wav": held,
             "Oneshots/Bass C2 120 bpm.wav": held, "Bass/Bass C2 120bpm.wav": held,
             "One Shots/Pluck C3 140bpm.wav": ru.tone(261.63, 1.5), "Stabs/Stab Am 128bpm.wav": ru.tone(220.0, 1.5),
             "Synth/Bass 120 C.wav": ru.tone(65.41, 2.0), "808/808 Long 140bpm C.wav": ru.b808(1.6)}
    rows, _, _, code, _ = run(files)
    check_eq("2 run ends well", code, 0)
    for rel in ("One Shots/Bass C2 120bpm.wav", "Bass One Shots/Bass C2 120bpm.wav", "Oneshots/Bass C2 120 bpm.wav",
                "Bass/Bass C2 120bpm.wav", "One Shots/Pluck C3 140bpm.wav", "Stabs/Stab Am 128bpm.wav",
                "Synth/Bass 120 C.wav"):
        r = rows[rel] or {}
        check_eq("2 '%s': a melodic one-shot, no tempo" % rel, (r.get("type"), r.get("bpm_tag")), ("oneshot_melodic", ""))
    check_eq("2 'Bass C2 120bpm': note C kept", (rows["One Shots/Bass C2 120bpm.wav"] or {}).get("note_tag"), "C")
    check_eq("2 '808 Long 140bpm C': still a drum hit", (rows["808/808 Long 140bpm C.wav"] or {}).get("type"), "drum_oneshot")


def test_pack_style_named():
    """Defect 3: the pack folder's name gives a style only when it really names one; said on the console; options."""
    loop = {"Loops/Loop 01 120 bpm.wav": ru.plucks(120.0, AM, 1)}
    for pack in ("Rock Solid Kicks", "Pop Up Drums", "Jungle Animals FX", "House of Loops Vol 3", "Soul Food Keys",
                 "Garage Band Loops", "Metal Percussion", "Acid Jazz", "Drum Hits 2", "archive_03",
                 "Trapped Souls Vol 1"):  # souls are not soul music (audit 2, b6 root4)
        rows, _, con, _, _ = run(loop, pack=pack)
        check_eq("3 pack '%s': no style" % pack, (rows["Loops/Loop 01 120 bpm.wav"] or {}).get("style"), "unlabelled")
        check_true("3 pack '%s': nothing said about a pack style" % pack, "style du pack" not in con, con)
    rows, _, con, _, _ = run(dict(loop, **{"House Loops/Loop 02 120 bpm.wav": ru.plucks(120.0, AM, 1)}),
                             pack="Drill Essentials Vol 1")
    check_eq("3 'Drill Essentials Vol 1': drill", (rows["Loops/Loop 01 120 bpm.wav"] or {}).get("style"), "drill")
    check_eq("3 'Drill Essentials Vol 1/House Loops': the inner folder wins",
             (rows["House Loops/Loop 02 120 bpm.wav"] or {}).get("style"), "house")
    check_true("3 console says the pack style and its row count", "style du pack : drill, posé sur 1 lignes" in con, con)
    rows, _, con, _, _ = run(loop, pack="Drill Essentials Vol 1", extra=["--no-pack-style"])
    check_eq("3 --no-pack-style: no style", (rows["Loops/Loop 01 120 bpm.wav"] or {}).get("style"), "unlabelled")
    check_true("3 --no-pack-style: nothing said", "style du pack" not in con, con)
    rows, _, con, _, _ = run(dict(loop, **{"House Loops/Loop 02 120 bpm.wav": ru.plucks(120.0, AM, 1)}),
                             pack="archive_03", extra=["--pack-style", "trap"])
    check_eq("3 --pack-style trap: given style", (rows["Loops/Loop 01 120 bpm.wav"] or {}).get("style"), "trap")
    check_eq("3 --pack-style trap: the inner folder still wins",
             (rows["House Loops/Loop 02 120 bpm.wav"] or {}).get("style"), "house")
    check_true("3 --pack-style: said on the console", "style du pack : trap, posé sur 1 lignes" in con, con)
    rows, _, con, _, _ = run(loop, pack="archive_03", extra=["--pack-style", "drift-phonk"])
    check_eq("3 --pack-style drift-phonk: a known style keeps its hyphen",
             (rows["Loops/Loop 01 120 bpm.wav"] or {}).get("style"), "drift-phonk")


def test_single_root_folder_is_the_pack():
    """Defect 4: one folder alone at the root is the pack itself: its name gives no role, its style is the pack's."""
    kick = st.kick(st.t_axis(0.4))
    rows, _, con, code, _ = run({"Hat Tricks Vol 1/Loops/Loop 01 120 bpm.wav": ru.plucks(120.0, AM, 1),
                                 "Hat Tricks Vol 1/Kicks/Kick 01.wav": kick}, pack="Downloads")
    check_eq("4 run ends well", code, 0)
    r = rows["Hat Tricks Vol 1/Loops/Loop 01 120 bpm.wav"] or {}
    check_true("4 'Hat Tricks Vol 1/Loops/Loop 01': no hats role", r.get("role") != "hats", r.get("role"))
    check_eq("4 'Hat Tricks Vol 1/Kicks/Kick 01': a kick", (rows["Hat Tricks Vol 1/Kicks/Kick 01.wav"] or {}).get("role"), "kick")
    rows, _, con, _, _ = run({"Drill Essentials Vol 1/Loops/Loop 01 120 bpm.wav": ru.plucks(120.0, AM, 1)}, pack="Downloads")
    check_eq("4 single 'Drill Essentials Vol 1' folder: its style", (rows["Drill Essentials Vol 1/Loops/Loop 01 120 bpm.wav"] or {}).get("style"), "drill")
    check_true("4 single pack folder: its style said on the console", "style du pack : drill, posé sur 1 lignes" in con, con)
    rows, _, _, _, _ = run({"Hats/Closed 01.wav": st.band_noise(int(0.2 * SR), 6000, 16000, 3)}, pack="Downloads")
    check_eq("4 a lone 'Hats' folder is a role, not a pack", (rows["Hats/Closed 01.wav"] or {}).get("role"), "hats")


def _ids(pack, files):
    """file_ids written for a pack, and the trace of each from the pack itself and from a twin pack."""
    tmp = tempfile.mkdtemp(prefix="tout_repare_")
    root = os.path.join(tmp, pack)
    for rel, x in files.items():
        p = os.path.join(root, *rel.split("/"))
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as fh:
            fh.write(st.wav_bytes(st.mono(np.asarray(x, dtype=float))))
    out = os.path.join(tmp, "t")
    with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
        tout.main([root, "--rar", "1", "--out", out])
    ids = []
    for f in sorted(glob.glob(out + "_*.csv")):
        if not f.endswith("_accuracy.csv"):
            with open(f, newline="", encoding="utf-8") as fh:
                ids += [r["file_id"] for r in csv.DictReader(fh)]
    return root, ids


def _trace(root, fid):
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        try:
            code = tout.main([root, "--rar", "1", "--trace", fid])
        except SystemExit as e:
            code = e.code
    return code, buf.getvalue().strip()


def test_file_id_has_the_pack():
    """Defect 11: two packs holding the same path give two ids; --trace finds each in its own pack only."""
    files = {"Loops/Loop 01 120 bpm.wav": ru.plucks(120.0, AM, 1)}
    ra, ia = _ids("Pack A", files)
    rb, ib = _ids("Pack B", files)
    check_true("11 same path in two packs: two ids", len(ia) == 1 and len(ib) == 1 and ia != ib, (ia, ib))
    if ia and ib:
        check_eq("11 --trace finds the file in its own pack", _trace(ra, ia[0]), (0, "Loops/Loop 01 120 bpm.wav"))
        check_eq("11 --trace does not find it in the twin pack", _trace(rb, ia[0])[0], 1)


def test_doc_empty_end_cut_not_clean():
    """Defect 18: the header says an empty end_cut of a one-shot with a tempo in its name is not a clean end."""
    doc = " ".join((tout.__doc__ or "").split())
    check_true("18 header: empty end_cut of a tempo-named one-shot is no verdict",
               "an empty end_cut there does not mean a clean end" in doc, None)


def test_kit_confirmed_tempo_reaches_all():
    """Holes a3-06, a3-13, a3-17: a kit's bare '128' heard at 125 (inside 3 %) becomes the tempo; the
    kit and a held pad stem (no tempo of its own) count their bars from 128; the kit's MIDI row is traceable."""
    kit = "Construction Kits/Kit 01 - 128/"
    d = ru.drums(125.0, 8)
    t = st.t_axis(len(d) / 48000.0)
    pad = sum(0.1 * st.harmonic(440.0 * 2 ** ((p - 69) / 12.0), t, 4, 0.5) for p in (57, 60, 64))
    mid = st.midi_bytes([(b * 1920, 1920, p, 90, 0) for b, ch in enumerate(AM) for p in ch], 480, 128.0)
    files = {kit + "Drums.wav": d, kit + "Pad.wav": pad, kit + "Chords.mid": mid}
    rows, kits, _, code, root = ru.run(files)
    check_eq("a3 kit run ends well", code, 0)
    check_eq("a3-17 kit: bars from the confirmed 128, not the heard 125",
             [(k.get("bpm_tag"), k.get("bars")) for k in kits], [("128", "8.25")])
    r = rows[kit + "Pad.wav"] or {}
    check_eq("a3-06 held pad stem re-measured with the kit's tempo", (r.get("bpm_tag"), r.get("bars")), ("128", "8.25"))
    r = rows[kit + "Chords.mid"] or {}
    check_eq("a3-13 kit MIDI row carries its file_id", r.get("type"), "midi")
    if r:
        check_eq("a3-13 --trace finds the kit MIDI file", _trace(root, r["file_id"]), (0, kit + "Chords.mid"))


def test_oneshot_note_accuracy_50_cents():
    """Hole a3-16: a one-shot named C3 that sounds 120 cents sharp is a miss in the accuracy table."""
    rows, _, _, code, root = ru.run({"Synth/Lead C3.wav": ru.tone(130.81 * 2 ** (1.2 / 12))})
    check_eq("a3-16 run ends well", code, 0)
    acc = []
    for f in glob.glob(os.path.join(os.path.dirname(root), "t_accuracy.csv")):
        with open(f, newline="", encoding="utf-8") as fh:
            acc = [(r["type"], r["measure"], r["tagged"], r["agree"]) for r in csv.DictReader(fh)]
    check_eq("a3-16 120 cents off is no agreement", acc, [("oneshot_melodic", "note", "1", "0")])


def test_flat_key_est_spelled_like_tag():
    """Defect 12: a key written with a flat ('Bbm') is shown as a flat in key_est too, not 'A# minor'."""
    bbm = [[n + 1 for n in c] for c in AM]
    rows, _, _, code, _ = ru.run({"Loops/Pad Loop 120 Bbm.wav": ru.plucks(120.0, bbm, 2),
                                  "Loops/Pad Loop 120 Am.wav": ru.plucks(120.0, AM, 2)})
    check_eq("12 run ends well", code, 0)
    r = rows["Loops/Pad Loop 120 Bbm.wav"] or {}
    check_eq("12 key_tag and key_est both written Bb", (r.get("key_tag"), r.get("key_est")), ("Bb minor", "Bb minor"))
    r = rows["Loops/Pad Loop 120 Am.wav"] or {}
    check_eq("12 a key without a flat is left as heard", (r.get("key_tag"), r.get("key_est")), ("A minor", "A minor"))


def _drum_hits(snare_on, hamp=0.5):
    """2 bars at 120: kick 4/4, white-noise snare on 2 and 4 (optional), 8th hats hamp under it."""
    t = np.arange(int(0.3 * SR)) / SR
    kick = 0.9 * np.sin(2 * np.pi * np.cumsum(50 + 110 * np.exp(-t / 0.03)) / SR) * np.exp(-t / 0.1)
    rng = np.random.default_rng(2)
    sn = 0.8 * (0.7 * rng.uniform(-1, 1, len(t)) * np.exp(-t / 0.05) + 0.3 * np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.04))
    h = np.diff(np.random.default_rng(3).uniform(-1, 1, int(0.05 * SR)), n=2, prepend=[0.0, 0.0])
    h = 0.6 * h / np.max(np.abs(h)) * np.exp(-np.arange(len(h)) / SR / 0.012)
    buf = np.zeros(4 * SR)

    def put(x, at):
        i = int(round(at * SR))
        n = min(len(x), len(buf) - i)
        buf[i:i + n] += x[:n]
    for b in range(2):
        for k in range(4):
            put(kick, 2.0 * b + 0.5 * k)
        for k in ((1, 3) if snare_on else ()):
            put(sn, 2.0 * b + 0.5 * k)
        for i in range(8):
            put(h * hamp, 2.0 * b + 0.25 * i)
    return np.clip(buf * 0.8, -1, 1)


def test_drum_grid_band_bleed():
    """A1/A2: a white snare spills into the hat band and hats spill into the snare band; each grid
    keeps its own drum (8th hats 6 dB under the snare stay on the hat grid, no snare means no snare grid)."""
    rows, _, _, code, _ = ru.run({"Loops/Drums/Drum Loop 120 BPM.wav": _drum_hits(True),
                                  "Loops/Drums/Hats Loop 120 BPM.wav": _drum_hits(False)})
    check_eq("A1 run ends well", code, 0)
    r = rows["Loops/Drums/Drum Loop 120 BPM.wav"] or {}
    check_eq("A1 8th hats under a snare", r.get("grid_hats"), "x.x.x.x.x.x.x.x.")
    check_eq("A1 snare on 2 and 4", r.get("grid_snare"), "....x.......x...")
    check_eq("A1 kick 4/4", r.get("grid_kick"), "x...x...x...x...")
    r = rows["Loops/Drums/Hats Loop 120 BPM.wav"] or {}
    check_true("A2 no snare, no snare hit", "x" not in (r.get("grid_snare") or ""), r.get("grid_snare"))
    check_eq("A2 hats still there", r.get("grid_hats"), "x.x.x.x.x.x.x.x.")


def _midi_no_ts(notes, tpq=480, bpm=120.0, ch=0):
    """Format 0, a tempo but no time signature; notes: (beat, beats, pitch, velocity)."""
    import struct

    def vlq(n):
        out = [n & 0x7F]
        n >>= 7
        while n:
            out.append(0x80 | (n & 0x7F))
            n >>= 7
        return bytes(reversed(out))
    ev = []
    for b, d, p, v in notes:
        ev.append((int(b * tpq), 1, bytes([0x90 | ch, p, v])))
        ev.append((int((b + d) * tpq), 0, bytes([0x80 | ch, p, 0])))
    ev.sort(key=lambda e: (e[0], e[1]))
    trk = b"\x00\xff\x51\x03" + struct.pack(">I", int(round(60e6 / bpm)))[1:]
    last = 0
    for tk, _, m in ev:
        trk += vlq(tk - last) + m
        last = tk
    trk += b"\x00\xff\x2f\x00"
    return b"MThd" + struct.pack(">IHHH", 6, 0, 1, tpq) + b"MTrk" + struct.pack(">I", len(trk)) + trk


def test_midi_waltz_by_accents():
    """A3: a MIDI waltz without a time signature, one note per beat, beat 1 louder and lower: the
    3-beat bar is heard, so bars is not counted in 4/4 (empty). A 4/4 part accented every 4 beats keeps 4 bars."""
    waltz = [(b * 3 + q, 0.9, 48 if q == 0 else 64, 110 if q == 0 else 70) for b in range(4) for q in range(3)]
    vel_only = [(b * 3 + q, 0.9, 60, 110 if q == 0 else 70) for b in range(4) for q in range(3)]
    four = [(b * 4 + q, 0.9, 48 if q == 0 else 64, 110 if q == 0 else 70) for b in range(4) for q in range(4)]
    rows, _, _, code, _ = ru.run({"MIDI/Waltz Piano C major.mid": _midi_no_ts(waltz),
                                  "MIDI/Waltz Lead C major.mid": _midi_no_ts(vel_only),
                                  "MIDI/Piano Four C major.mid": _midi_no_ts(four)})
    check_eq("A3 run ends well", code, 0)
    for f in ("MIDI/Waltz Piano C major.mid", "MIDI/Waltz Lead C major.mid"):
        r = rows[f] or {}
        check_true("A3 waltz bars not 3 (%s)" % f, r.get("bars") in ("", None, "4"), r.get("bars"))
    check_eq("A3 4/4 accented part keeps 4 bars", (rows["MIDI/Piano Four C major.mid"] or {}).get("bars"), "4")


def test_short_file_absolute_gate():
    """A4: a 0.3 s file at -90 dBFS is under the -70 LUFS absolute gate: no loudness, as for a long file."""
    x = 10 ** (-90 / 20) * np.sin(2 * np.pi * 1000 * np.arange(int(0.3 * SR)) / SR)
    check_eq("A4 short and under the gate", tout.lufs(x[:, None], SR), None)
    x = 10 ** (-90 / 20) * np.sin(2 * np.pi * 1000 * np.arange(int(2.0 * SR)) / SR)
    check_eq("A4 long and under the gate", tout.lufs(x[:, None], SR), None)
    y = 0.5 * np.sin(2 * np.pi * 1000 * np.arange(int(0.3 * SR)) / SR)
    v = tout.lufs(y[:, None], SR)
    check_true("A4 a loud short file keeps its loudness", v is not None and -12 < v < -3, v)


def test_digital_silence_is_an_error():
    """A5: a file of pure digital silence is an error row ('digital silence'), never a clean
    end_cut 0; an error row carries no measure at all."""
    rows, _, _, code, _ = ru.run({"Synth/Silent C3.wav": np.zeros(SR), "Loops/Silent Loop 120.wav": np.zeros(4 * SR),
                                  "Synth/DC C3.wav": np.full(SR, 0.3)})
    check_eq("A5 run ends well", code, 0)
    for f in ("Synth/Silent C3.wav", "Loops/Silent Loop 120.wav"):
        r = rows[f] or {}
        check_true("A5 silence is an error (%s)" % f, "silence" in (r.get("error") or ""), r.get("error"))
    for f, r in rows.items():
        r = r or {}
        if r.get("error"):
            filled = [c for c in ("end_cut", "end_fade_ms", "attack_ms", "loopable", "lufs_i", "peak_dbfs")
                      if r.get(c) not in ("", None)]
            check_eq("A5 error row has no measure (%s)" % f, filled, [])


def test_rf64_rifx_named():
    """A6: an RF64 or RIFX file is refused with its own reason, not as 'not a wav or aiff file'."""
    w = st.wav_bytes(st.mono(ru.tone(261.63)[:SR]))
    rows, _, _, code, _ = ru.run({"Synth/Big C3.wav": b"RF64" + w[4:], "Synth/Motorola C3.wav": b"RIFX" + w[4:],
                                  "Synth/Junk C3.wav": b"JUNK" + w[4:]})
    check_eq("A6 run ends well", code, 0)
    check_eq("A6 RF64", (rows["Synth/Big C3.wav"] or {}).get("error"), "RF64/RIFX/BW64 non pris en charge")
    check_eq("A6 RIFX", (rows["Synth/Motorola C3.wav"] or {}).get("error"), "RF64/RIFX/BW64 non pris en charge")
    check_eq("A6 other junk unchanged", (rows["Synth/Junk C3.wav"] or {}).get("error"), "not a wav or aiff file")


def test_accuracy_wilson_gate():
    """A7: usable only when the one-sided 95 % Wilson upper bound of the error is under 5 %."""
    def usable(n, bad):
        acc = tout.Accuracy()
        for i in range(n):
            acc.add("drum_loop", "bpm", i >= bad)
        return acc.rows()[0]["usable"]
    check_eq("A7 10 of 10 right: too few to trust", usable(10, 0), "no")
    check_eq("A7 51 of 51 right: bound 5.04 %, still closed", usable(51, 0), "no")
    check_eq("A7 52 of 52 right: bound 4.95 %, open", usable(52, 0), "yes")
    check_eq("A7 60 of 60 right: open", usable(60, 0), "yes")
    check_eq("A7 59 of 60 right: closed", usable(60, 1), "no")
    check_eq("A7 200 with 2 wrong: open", usable(200, 2), "yes")
    check_eq("A7 rule written in the header", "52 right out of 52" in (tout.__doc__ or ""), True)


def _legato(bpm, midis, amp=0.5):
    """Notes played back to back with no dip in level: one phase-continuous tone that changes pitch."""
    beat = int(round(60.0 / bpm * SR))
    f = np.repeat([440.0 * 2 ** ((m - 69) / 12.0) for m in midis], beat)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return amp * (np.sin(ph) + 0.3 * np.sin(2 * ph))


def test_review_oneshot_limits():
    """Fresh review: a zero tempo, a pack folder named 'One Shots', a legato bass line."""
    rows, _, _, code, _ = run({"Synth/Pad 00bpm.wav": _legato(120.0, [57] * 6)})
    check_eq("R1 run ends well", code, 0)
    check_true("R1 'Pad 00bpm': no division error", "Division" not in str((rows["Synth/Pad 00bpm.wav"] or {}).get("error")),
               (rows["Synth/Pad 00bpm.wav"] or {}).get("error"))
    rel = "Loops & One Shots Vol 1/Synths/Pad 120bpm Am.wav"
    rows, _, _, code, _ = run({rel: ru.plucks(120.0, AM, 4)}, pack="Downloads")
    check_eq("R2 pack folder 'Loops & One Shots Vol 1' is no one-shot folder", (rows[rel] or {}).get("type"), "melodic_loop")
    rel = "Synth/Bass 120bpm Am.wav"
    rows, _, _, code, _ = run({rel: _legato(120.0, [45, 48, 52, 55, 57, 55, 52, 48])})
    check_eq("R3 legato bass line, 8 notes, no dip: a loop", (rows[rel] or {}).get("type"), "melodic_loop")
    rows, _, _, code, _ = run({"Synth/Pad 120bpm C.wav": _legato(120.0, [48] * 8)})
    check_eq("R3 one held note named with a tempo: still a one-shot", (rows["Synth/Pad 120bpm C.wav"] or {}).get("type"), "oneshot_melodic")


def test_review2_limits():
    """Second fresh review: a lone 'One Shots' folder, BW64, a drum MIDI accented every 3 beats."""
    rel = "One Shots/Bass C2 120bpm.wav"
    rows, _, _, code, _ = run({rel: _legato(120.0, [36] * 6)}, pack="Downloads")
    check_eq("R4 lone 'One Shots' folder still a one-shot folder", (rows[rel] or {}).get("type"), "oneshot_melodic")
    w = st.wav_bytes(st.mono(ru.tone(261.63)[:SR]))
    rows, _, _, _, _ = run({"Synth/Broadcast C3.wav": b"BW64" + w[4:]})
    check_eq("R5 BW64 named", (rows["Synth/Broadcast C3.wav"] or {}).get("error"), "RF64/RIFX/BW64 non pris en charge")
    drums = [(b, 0.2, 42, 120 if b % 3 == 0 else 70) for b in range(16)]
    rows, _, _, _, _ = run({"MIDI/Drums/Groove 120 BPM.mid": _midi_no_ts(drums, ch=9)})
    check_eq("R6 4/4 drum MIDI with a 3-beat accent keeps 4 bars", (rows["MIDI/Drums/Groove 120 BPM.mid"] or {}).get("bars"), "4")


def test_mutation_survivors_2():
    """Mutations a3-00, a3-08 and a3-21: two bare numbers both equal to the heard tempo stay a doubt;
    a tonal drum stem in a kit with a lone key letter does not take that letter."""
    files = {"Loops/Keys Loop 118 Am 122.wav": ru.plucks(120.0, AM, 4)}
    files.update({"Kits/Kit 07 - 128 A/Drums.wav": ru.plucks(128.0, AM, 4),
                  "Kits/Kit 07 - 128 A/Pad.wav": ru.plucks(128.0, AM, 4)})
    rows, _, _, code, _ = run(files)
    check_eq("a3-00 run ends well", code, 0)
    check_eq("a3-00 118 and 122 both heard as 120: no tempo chosen",
             (rows["Loops/Keys Loop 118 Am 122.wav"] or {}).get("bpm_tag"), "")
    check_eq("a3-08 tonal drum stem of a kit 'A': no key", (rows["Kits/Kit 07 - 128 A/Drums.wav"] or {}).get("key_tag"), "")
    d = rows["Kits/Kit 07 - 128 A/Drums.wav"] or {}
    check_eq("a3-21 tonal drum stem: no key cells",
             tuple(d.get(c) or "" for c in ("key_est", "key_measured", "key_match", "key_strength")), ("",) * 4)
    check_true("a3-08 the pad of the same kit takes the confirmed letter",
               (rows["Kits/Kit 07 - 128 A/Pad.wav"] or {}).get("key_tag") not in ("", None),
               (rows["Kits/Kit 07 - 128 A/Pad.wav"] or {}).get("key_tag"))


def test_reversed_crash_role_fx():
    """b1 replay: a reversed crash is filed as fx, so its role is fx too, never hats."""
    n = int(SR * 2.0)
    x = (np.random.default_rng(9).standard_normal(n) * np.linspace(0.01, 0.4, n)).astype(np.float32)
    rows, _, _, code, _ = run({"FX/Reverse Crash 01.wav": x, "Drums/Crash 01.wav": x[::-1].copy()})
    r = rows["FX/Reverse Crash 01.wav"] or {}
    check_eq("b1 reversed crash: type fx", r.get("type"), "fx")
    check_eq("b1 reversed crash: role fx", r.get("role"), "fx")
    check_eq("b1 plain crash keeps the role hats", (rows["Drums/Crash 01.wav"] or {}).get("role"), "hats")


def test_short_bpm_name_not_a_loop():
    """Mutation 'bpm word makes a loop from 1.5 s -> 0.2 s': a tempo in the name alone makes no loop under 1.5 s,
    even when the sound lasts a full bar of that tempo (250 bpm: 0.96 s)."""
    for name, dur, want in (("Synth/Lead 250bpm.wav", 1.2, "oneshot_melodic"), ("Synth/Hat 174bpm.wav", 1.4, "drum_oneshot"),
                            ("Synth/Lead 250bpm.wav", 1.6, "melodic_loop")):
        check_eq("bpm-short '%s' %.1f s" % (name, dur), tout.classify(name, dur).get("type"), want)


def test_rar3_name_cases():
    """docs/sons/passe2/CAS-A-REJOUER.md, cases 1-6 and 9 (invented names, same shape as the real ones)."""
    pt = tout.parse_tags
    # case 1: a lone number between separators in a loop or MIDI name is its tempo
    for parts, typ in ((["MIDI", "Bass Loops", "Bass Synth Loop_125_Lantern_F.mid"], "midi"),
                       (["Loops", "Atmospheric Loops", "Atmospheric Loop_125_Meadow_D.wav"], "melodic_loop"),
                       (["Loops", "Drum Fills", "Drum Fill_125_Pebble.wav"], "drum_loop")):
        check_eq("c1 bpm from '%s'" % parts[-1], pt(parts, typ)["bpm"], 125.0)
    # the fence stays for names without that structure
    for name in ("Pad 120 Dark.wav", "Loop120Lantern.wav", "Synth Loop_250_Hot.wav", "Synth Loop 120 140 A.wav"):
        check_eq("c1 fence: no tempo from '%s'" % name, pt(["Loops", name], "melodic_loop")["bpm"], None)
    # case 2: a lone last letter beside a tempo is the tonic, mode left empty, A and B included
    for name, want in (("Synth Melody Loop_125_Orchid_A.mid", "A"), ("Bass Synth Loop_125_Copper_B.mid", "B"),
                       ("Synth Melody Loop_125_Willow_E.mid", "E"), ("Synth Melody Loop_125_Harbor_Gb.mid", "F#")):
        check_eq("c2 key from '%s'" % name, tout.key_name(pt(["MIDI", name], "midi")["key"]), want)
    check_eq("c2 strong key keeps its mode", tout.key_name(pt(["MIDI", "Synth Loop_125_Tundra_Am.mid"], "midi")["key"]),
             "A minor")
    check_eq("c2 drum loop letter is no key", pt(["Loops", "Drum Loop_125_Lantern_A.wav"], "drum_loop")["key"], None)
    check_eq("c2 root-only tag matches its root", tout.key_match((9, "minor"), (9, None)), "root")
    check_eq("c2 root-only tag, other root", tout.key_match((4, "minor"), (9, None)), "other")
    # case 3: a lone note a semitone off is still the note the name gives
    row = tout.confirm_tags({"note_tag": "C", "note_measured": "B1", "cents_gap": -100.0}, {"note": 0, "note_lone": True})
    check_eq("c3 'Shot_C' heard 100 cents flat keeps C", (row["note_tag"], row["cents_gap"]), ("C", -100.0))
    row = tout.confirm_tags({"note_tag": "F", "note_measured": "A3", "cents_gap": 400.0}, {"note": 5, "note_lone": True})
    check_eq("c3 'Pad Soft F' heard A3 still drops F", row["note_tag"], None)
    # case 4: a title word is no genre when the pack names one
    loop = ru.drums(125.0)
    rows, _, _, _, _ = run({"Loops/Drum Fills/Drum Fill_125_Soul.wav": loop}, extra=["--pack-style", "techno"])
    check_eq("c4 'Soul' title in a techno pack", (rows["Loops/Drum Fills/Drum Fill_125_Soul.wav"] or {}).get("style"), "techno")
    rows, _, _, _, _ = run({"Loops/Drum Fills/Drum Fill_125_Soul.wav": loop})
    check_eq("c4 witness: no pack style, the title still counts", (rows["Loops/Drum Fills/Drum Fill_125_Soul.wav"] or {}).get("style"), "rnb")
    # case 5: an atmospheric loop with tempo and key is a melodic loop (pad), a riser loop stays fx
    c = tout.classify("Loops/Atmospheric Loops/Atmospheric Loop_125_Meadow_D.wav", 7.68)
    check_eq("c5 atmospheric loop", (c["type"], c["role"]), ("melodic_loop", "pad"))
    check_eq("c5 witness: riser loop stays fx", tout.classify("Loops/Riser Loop_125_D.wav", 7.68)["type"], "fx")
    # case 6: an FX folder or an fx word beats a title word for the role
    for rel in ("One-Shots/FX/Sub Hits/Sub Boom_Gravelly.wav", "One-Shots/FX/Impacts/Impact FX_Break.wav"):
        c = tout.classify(rel, 2.0)
        check_eq("c6 role of '%s'" % rel, (c["type"], c["role"]), ("fx", "fx"))
    # case 9: no note on a clap or a snare, a kick keeps its note
    t = st.t_axis(0.25)
    rng = np.random.default_rng(3)
    snare = 0.5 * (rng.standard_normal(t.size) + np.sin(2 * np.pi * 190.0 * t)) * np.exp(-t / 0.08)
    for fam in ("snare", "clap"):
        check_eq("c9 %s has no note" % fam, tout.measure_drum_oneshot(SR, st.mono(snare), fam).get("note_measured"), None)
    check_true("c9 witness: a kick keeps its note", tout.measure_drum_oneshot(SR, st.mono(ru.b808()), "kick").get("note_measured"))


TESTS = [test_rar3_name_cases, test_short_bpm_name_not_a_loop, test_reversed_crash_role_fx, test_mutation_survivors_2, test_review2_limits, test_review_oneshot_limits, test_accuracy_wilson_gate, test_rf64_rifx_named, test_digital_silence_is_an_error, test_short_file_absolute_gate, test_midi_waltz_by_accents, test_drum_grid_band_bleed, test_flat_key_est_spelled_like_tag, test_kit_confirmed_tempo_reaches_all, test_oneshot_note_accuracy_50_cents, test_doc_empty_end_cut_not_clean, test_file_id_has_the_pack, test_single_root_folder_is_the_pack, test_pack_style_named, test_oneshot_with_tempo, test_lone_letter_root, test_kit_midi_key, test_drum_stem_no_key, test_bare_equal_first, test_bars_when_tempo_contradicted]


def main():
    with np.errstate(all="ignore"):
        for fn in TESTS:
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
