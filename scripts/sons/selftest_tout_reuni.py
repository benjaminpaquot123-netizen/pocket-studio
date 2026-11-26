#!/usr/bin/env python3
"""Self-test of the wiring between the five repair lots of tout.py (audit 2, reunion).

    python3 -I scripts/sons/selftest_tout_reuni.py

Every check runs the real command line on a small synthetic pack written to a
temporary folder (outside the repository) and reads the tables back: a fix that
exists as a function but is not called by the tool fails here. Exit code 1 when
a single check fails. No sample is needed.
"""
import contextlib
import csv
import glob
import importlib.util
import io
import math
import os
import sys
import tempfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import tout  # noqa: E402
import selftest_tout as st  # noqa: E402  (generators only)

SR = st.SR
FAILS = []
COUNT = [0]


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


def num(cell):
    return float(cell) if cell not in ("", None) else None


def run(files, pack="Pack", extra=()):
    """files: {rel: mono signal or bytes}. Runs tout.main on the pack folder given
    directly; returns ({rel: row}, kit rows, console text, exit code, pack folder)."""
    tmp = tempfile.mkdtemp(prefix="tout_reuni_")
    root = os.path.join(tmp, pack)
    for rel, x in files.items():
        p = os.path.join(root, *rel.split("/"))
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as fh:
            fh.write(x if isinstance(x, bytes) else st.wav_bytes(st.mono(np.asarray(x, dtype=float))))
    out = os.path.join(tmp, "t")
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        try:
            code = tout.main([root, "--rar", "1", "--out", out] + list(extra))
        except SystemExit as e:
            code = "exit %s" % e.code
    rows = []
    for f in sorted(glob.glob(out + "_*.csv")):
        if not f.endswith("_accuracy.csv"):
            with open(f, newline="", encoding="utf-8") as fh:
                rows += list(csv.DictReader(fh))
    by_id = {r.get("file_id"): r for r in rows}
    by_rel = {rel: by_id.get(tout._file_id(rel)) for rel in files}
    return by_rel, [r for r in rows if r["type"] == "kit"], buf.getvalue(), code, root


AM = [[57, 60, 64], [53, 57, 60], [50, 53, 57], [52, 56, 59]]   # i VI iv V in A minor
FMAJ = [[53, 57, 60], [58, 62, 65], [60, 64, 67], [53, 57, 60]]  # I IV V I in F major
FOUR = ((0, 4, 8, 12), (4, 12), tuple(range(0, 16, 2)))


def drums(bpm, bars=4):
    return st.drum_loop(bpm, bars, 0.5, *FOUR)


def plucks(bpm, chords, bars=1):
    """Chords struck on every beat: a melodic loop with a tempo that can be heard."""
    beat = 60.0 / bpm
    t = st.t_axis(beat)
    out = []
    for ch in chords * bars:
        hit = sum(st.harmonic(440.0 * 2 ** ((p - 69) / 12.0), t, 5, 0.6) for p in ch)
        hit = hit + 1.2 * st.harmonic(440.0 * 2 ** ((ch[0] - 81) / 12.0), t, 3, 0.5)
        hit = 0.1 * hit * np.minimum(t / 0.003, 1.0) * np.exp(-t / 0.12)
        out += [hit] * 4
    return np.concatenate(out)


def tone(f, dur=1.0):
    t = st.t_axis(dur)
    return 0.4 * st.harmonic(f, t) * st.adsr(t, 0.005, 0.1, 0.6, dur - 0.3, 0.25)


def b808(dur=2.0, cut=False):
    t = st.t_axis(dur)
    x = 0.8 * np.sin(2 * np.pi * 65.4 * t) * np.exp(-t / (2.0 if cut else 0.3))
    return x


def test_confirm_wired():
    """Wiring 1 (lot B, defects 6, 10, 11, 12, 38): the measured sound confirms what the name suggests."""
    files = {
        "Drum Loops/Drum Loop 128.wav": drums(128.0),
        "Drum Loops/Drum Loop 99.wav": drums(128.0),
        "Loops/Bass Loop 120.wav": plucks(120.0, AM, 2),
        "Loops/Juno 106 Chords Loop Am.wav": plucks(120.0, AM, 2),
        "Loops/Pad Loop F.wav": plucks(120.0, FMAJ, 2),
        "Loops/Pad Loop D.wav": plucks(120.0, FMAJ, 2),
        "Loops/Bass Loop 120 Bbm.wav": plucks(120.0, [[c + 1 for c in ch] for ch in AM], 2),
        "Synth/Pad Soft F.wav": tone(220.0),
        "Synth/Pad Soft A.wav": tone(220.0),
        "Synth/Pluck Bb2.wav": tone(116.54),
        "Synth/Pad Soft Bb.wav": tone(233.08),
        "Synth/Pad Soft Eb.wav": tone(220.0),
    }
    rows, _, con, code, _ = run(files)
    check_eq("1 run ends well", code, 0)
    r = rows["Drum Loops/Drum Loop 128.wav"] or {}
    check_eq("1 drum loop: bare 128 heard at 128 is the tempo", num(r.get("bpm_tag")), 128.0)
    check_true("1 drum loop: the gap to the confirmed tempo is written",
               num(r.get("bpm_gap_pct")) is not None and abs(num(r.get("bpm_gap_pct"))) < 3.0, r.get("bpm_gap_pct"))
    check_eq("1 drum loop: bars from the confirmed tempo", num(r.get("bars")), 4.0)
    check_eq("1 drum loop: bpm_est is the confirmed tempo", num(r.get("bpm_est")), 128.0)
    r = rows["Drum Loops/Drum Loop 99.wav"] or {}
    check_eq("1 drum loop: bare 99 heard at 128 stays out", r.get("bpm_tag"), "")
    r = rows["Loops/Bass Loop 120.wav"] or {}
    check_eq("1 melodic loop: bare 120 heard at 120 is the tempo", num(r.get("bpm_tag")), 120.0)
    check_eq("1 melodic loop: bars from the confirmed tempo", num(r.get("bars")), 8.0)
    r = rows["Loops/Juno 106 Chords Loop Am.wav"] or {}
    check_eq("1 gear number 106 heard at 120 stays out", r.get("bpm_tag"), "")
    r = rows["Loops/Pad Loop F.wav"] or {}
    check_eq("1 lone key F heard in F is the key", r.get("key_tag"), "F major")
    check_eq("1 lone key F: the match is written", r.get("key_match"), "exact")
    r = rows["Loops/Pad Loop D.wav"] or {}
    check_eq("1 lone key D heard in F stays out", r.get("key_tag"), "")
    r = rows["Loops/Bass Loop 120 Bbm.wav"] or {}
    check_eq("1 a flat written in the name is shown as a flat", r.get("key_tag"), "Bb minor")
    r = rows["Synth/Pad Soft F.wav"] or {}
    check_eq("1 lone note F that sounds A is dropped", r.get("note_tag"), "")
    check_eq("1 lone note dropped: no gap to a note that is not there", r.get("cents_gap"), "")
    r = rows["Synth/Pad Soft A.wav"] or {}
    check_eq("1 lone note A that sounds A is kept", r.get("note_tag"), "A")
    r = rows["Synth/Pluck Bb2.wav"] or {}
    check_eq("1 one-shot: a flat note is shown as a flat", r.get("note_tag"), "Bb")
    r = rows["Synth/Pad Soft Bb.wav"] or {}
    check_eq("1 lone flat Bb that sounds Bb is kept, as a flat", r.get("note_tag"), "Bb")
    check_true("1 lone flat kept: its gap is written", num(r.get("cents_gap")) is not None, r.get("cents_gap"))
    r = rows["Synth/Pad Soft Eb.wav"] or {}
    check_eq("1 lone flat Eb that sounds A is dropped", r.get("note_tag"), "")


def test_confirm_wired_kits():
    """Wiring 1 in construction kits: the kit folder's bare number, heard in the summed stems."""
    kit = "Construction Kits/Kit 01 - 128/"
    off = "Construction Kits/Kit 02 - 99/"
    n = len(drums(128.0))
    files = {
        kit + "Drums.wav": drums(128.0), kit + "Keys.wav": plucks(128.0, AM)[:n],
        off + "Drums.wav": drums(128.0), off + "Keys.wav": plucks(128.0, AM)[:n],
    }
    rows, kits, _, code, _ = run(files)
    check_eq("1k run ends well", code, 0)
    by = {r["file_id"]: r for r in kits}
    k1 = by.get(tout._file_id(kit.rstrip("/"))) or {}
    k2 = by.get(tout._file_id(off.rstrip("/"))) or {}
    check_eq("1k kit: bare 128 heard at 128 is the tempo", num(k1.get("bpm_tag")), 128.0)
    check_eq("1k kit: bars from the confirmed tempo", num(k1.get("bars")), 4.0)
    check_eq("1k kit: bare 99 heard at 128 stays out", k2.get("bpm_tag"), "")
    r = rows[kit + "Drums.wav"] or {}
    check_eq("1k stem: the kit's confirmed tempo", num(r.get("bpm_tag")), 128.0)
    check_eq("1k stem: bars from the confirmed tempo", num(r.get("bars")), 4.0)
    r = rows[off + "Drums.wav"] or {}
    check_eq("1k stem: bare 99 heard at 128 stays out", r.get("bpm_tag"), "")


def test_pack_style():
    """Wiring 2 (defect 14): a style written only in the pack folder's own name."""
    kit = "Construction Kits/Kit 01 128 BPM/"
    n = len(drums(128.0))
    files = {"Kicks/Kick 01.wav": 0.9 * st.kick(st.t_axis(0.5)), "Loops/Bass Loop 120 Am.wav": plucks(120.0, AM, 2),
             "House Loops/Bass Loop 124 Am.wav": plucks(124.0, AM, 2),
             kit + "Drums.wav": drums(128.0), kit + "Keys.wav": plucks(128.0, AM)[:n],
             "MIDI/Chords 140 bpm Am.mid": st.midi_bytes([(b * 1920, 1920, p, 90, 0) for b, ch in enumerate(AM) for p in ch],
                                                           480, 140.0)}
    rows, kits, _, code, _ = run(files, pack="Techno Essentials Vol 2")
    check_eq("2 run ends well", code, 0)
    for rel in ("Kicks/Kick 01.wav", "Loops/Bass Loop 120 Am.wav", kit + "Drums.wav", "MIDI/Chords 140 bpm Am.mid"):
        check_eq("2 pack name gives the style: %s" % rel, (rows[rel] or {}).get("style"), "techno")
    check_eq("2 pack name gives the style: kit row", [k.get("style") for k in kits], ["techno"])
    check_eq("2 a folder inside the pack beats the pack name",
             (rows["House Loops/Bass Loop 124 Am.wav"] or {}).get("style"), "house")
    rows, _, _, _, _ = run({"Kicks/Kick 01.wav": 0.9 * st.kick(st.t_axis(0.5))}, pack="Pack")
    check_eq("2 no style anywhere", (rows["Kicks/Kick 01.wav"] or {}).get("style"), "unlabelled")


def test_kit_rows_traceable():
    """Wiring 3 (defect 28): kit and stem rows carry a file_id that --trace finds again."""
    kit = "Construction Kits/Kit 01 128 BPM/"
    n = len(drums(128.0))
    files = {kit + "Drums.wav": drums(128.0), kit + "Keys.wav": plucks(128.0, AM)[:n]}
    rows, kits, _, code, root = run(files)
    check_eq("3 run ends well", code, 0)
    for rel in files:
        check_true("3 stem row has a file_id: %s" % rel, rows[rel] is not None and rows[rel]["type"] == "kit_stem")
    check_eq("3 kit row has the id of its folder", [k.get("file_id") for k in kits], [tout._file_id(kit.rstrip("/"))])
    for rel in list(files) + [kit.rstrip("/")]:
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            code = tout.main([root, "--rar", "1", "--trace", tout._file_id(rel)])
        check_eq("3 --trace finds %s" % rel, (code, buf.getvalue().strip()), (0, rel))


def test_mutations_run_every_selftest():
    """Wiring 4: audit2/mutate.py plays every selftest_tout*.py, or the lots' mutants pass unseen."""
    spec = importlib.util.spec_from_file_location("audit2_mutate", os.path.join(HERE, "audit2", "mutate.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)  # importing must not start a run
    got = mod.tests_for("tout.py", HERE)
    want = ["selftest_tout.py"] + ["selftest_tout_%s.py" % s for s in ("a", "b", "c", "d", "e", "repare", "reuni")]
    check_eq("4 every self-test of tout.py is played", sorted(got), sorted(want))
    check_eq("4 the base self-test first (fast kill)", got[0], "selftest_tout.py")
    check_eq("4 mesure.py also plays its own self-test", sorted(mod.tests_for("mesure.py", HERE)),
             sorted(want + ["selftest.py"]))


def test_end_of_tempo_named_hits():
    """Wiring 5 (defect 18, chef's rule): a one-shot whose name carries a tempo ('808 Long 140bpm C')
    is still one hit. A measured cut is written; 'no cut' stays empty (a long 808 may be meant to run on)."""
    files = {
        "Drums/808s/808 Long 140bpm C.wav": b808(2.0),
        "Drums/808s/808 Cut 140bpm C.wav": b808(2.0, cut=True),
        "Drums/808s/808 Long C.wav": b808(2.0),
        "Drums/Kicks/Kick Full 01.wav": 0.9 * st.kick(st.t_axis(0.5)),
        "Drums/Kicks/Kick Full 02.wav": (0.9 * st.kick(st.t_axis(0.5)))[:SR // 20],
    }
    rows, _, _, code, _ = run(files)
    check_eq("5 run ends well", code, 0)
    r = rows["Drums/808s/808 Long 140bpm C.wav"] or {}
    check_eq("5 808 with a tempo in its name is a one-shot", r.get("type"), "drum_oneshot")
    check_eq("5 808 with a tempo, clean end: end_cut empty", r.get("end_cut"), "")
    check_eq("5 a one-shot shows no tempo tag", r.get("bpm_tag"), "")
    check_eq("5 a one-shot shows no tempo estimate", r.get("bpm_est"), "")
    r = rows["Drums/808s/808 Cut 140bpm C.wav"] or {}
    check_eq("5 808 with a tempo, cut end: end_cut 1", r.get("end_cut"), "1")
    check_eq("5 808 without a tempo, clean end: end_cut 0", (rows["Drums/808s/808 Long C.wav"] or {}).get("end_cut"), "0")
    r = rows["Drums/Kicks/Kick Full 01.wav"] or {}
    check_eq("5 'Kick Full' is one hit, role full", (r.get("type"), r.get("role")), ("drum_oneshot", "full"))
    check_eq("5 'Kick Full', clean end: end_cut 0", r.get("end_cut"), "0")
    check_eq("5 'Kick Full', cut end: end_cut 1", (rows["Drums/Kicks/Kick Full 02.wav"] or {}).get("end_cut"), "1")


def test_doc_matches_fence():
    """Wiring 6: the module's text on end_cut says what fence_row does."""
    doc = " ".join(tout.__doc__.split())
    check_true("6 the text no longer says full mixes are always empty", "full mixes and any one-shot with a bpm" not in doc)
    check_true("6 the text names the drum hit called full", "drum hit named" in doc)
    check_true("6 the text names the tempo in a one-shot's name", "tempo in its name" in doc)


def test_leftovers():
    """Wiring 7: what lot A left to check on the merged head."""
    files = {
        "Synth/Bass 120 C.wav": plucks(120.0, [[48, 52, 55]], 1),
        "Synth/Bass 97 C.wav": plucks(120.0, [[48, 52, 55]], 1),
        "Loops/Top Loop 01.wav": st.chord_loop(80.0, AM),
        "Loops/Chanting Monks Loop 90.wav": plucks(90.0, AM, 1),
    }
    rows, _, _, code, _ = run(files)
    check_eq("7 run ends well", code, 0)
    r = rows["Synth/Bass 120 C.wav"] or {}
    check_eq("7 'Bass 120 C' (2 s): a loop", r.get("type"), "melodic_loop")
    check_eq("7 'Bass 120 C': tempo 120 once heard", num(r.get("bpm_tag")), 120.0)
    check_eq("7 'Bass 97 C': 97 not heard, no tempo", (rows["Synth/Bass 97 C.wav"] or {}).get("bpm_tag"), "")
    r = rows["Loops/Top Loop 01.wav"] or {}
    check_eq("7 pad named 'Top Loop 01': a melodic loop", r.get("type"), "melodic_loop")
    check_eq("7 pad named 'Top Loop 01': no guessed tempo, no guessed bars", (r.get("bpm_est"), r.get("bars")), ("", ""))
    r = rows["Loops/Chanting Monks Loop 90.wav"] or {}
    check_eq("7 'Chanting Monks Loop 90': a melodic loop sung by voices",
             (r.get("type"), r.get("role")), ("melodic_loop", "vocal"))


def _decay(f, end_db, phase_end, dur, sr=48000):
    """A sine that decays steadily from 0 dB and stops at end_db, at the phase phase_end (turns)."""
    n = int(dur * sr)
    t = (np.arange(n) - (n - 1)) / sr
    env = 10 ** (end_db / 20) * np.exp(t * (end_db / 8.686) / dur)
    return (env * np.sin(2 * np.pi * (f * t + phase_end)))[:, None]


def test_mutation_survivors():
    """One check per limit the first full mutation run left unguarded."""
    sr = 48000
    # end cut: the four limits of the two rules
    check_eq("8 stop on a crest at -50 dB of the peak: a step, cut",
             tout.end_cut(sr, _decay(200, -50, 0.25, 0.083))["end_cut"], 1)
    check_eq("8 stop at 2.7 times the wave's own motion: no step, not cut",
             tout.end_cut(sr, _decay(1620, -48, math.asin(0.4) / (2 * math.pi), 0.080))["end_cut"], 0)
    check_eq("8 stop on a zero crossing, still at -35 dB 5 ms before: cut",
             tout.end_cut(sr, _decay(1000, -34, 0.0, 0.085))["end_cut"], 1)
    # accuracy: usable needs 10 tagged files and under 5 % of error
    def usable(n, bad):
        acc = tout.Accuracy()
        for i in range(n):
            acc.add("drum_loop", "bpm", i >= bad)
        return acc.rows()[0]["usable"]
    check_eq("8 accuracy: 9 files that all agree are not enough", usable(9, 0), "no")
    check_eq("8 accuracy: 10 files that all agree are usable", usable(10, 0), "yes")
    check_eq("8 accuracy: 1 wrong in 10 (10 %) is not usable", usable(10, 1), "no")
    check_eq("8 accuracy: 1 wrong in 20 (5 %) is not usable", usable(20, 1), "no")
    check_eq("8 accuracy: 1 wrong in 21 (4.8 %) is usable", usable(21, 1), "yes")
    # bars and tempo gap
    check_eq("8 bars: 7 s at 120 is 3.5 bars, not 4", tout.bars_of(7.0, 120), 3.5)
    check_eq("8 bars: 4.5 s at 120 is 2.25 bars", tout.bars_of(4.5, 120), 2.25)
    check_eq("8 bars: 8.1 s at 120 is 4 bars", tout.bars_of(8.1, 120), 4.0)
    check_eq("8 tempo gap: 126 heard for 120 written is +5 %", tout.bpm_gap(126.0, 120.0), 5.0)
    check_eq("8 tempo gap: 114 heard for 120 written is -5 %", tout.bpm_gap(114.0, 120.0), -5.0)
    # channels and stereo width
    tone = 0.5 * np.sin(2 * np.pi * 220 * np.arange(sr) / sr) * np.exp(-np.arange(sr) / (0.2 * sr))
    check_eq("8 a mono file has 1 channel", tout.common(sr, tone[:, None])["ch"], 1)
    check_eq("8 a stereo file has 2 channels", tout.common(sr, np.stack([tone, tone], 1))["ch"], 2)
    w = tout.measure_oneshot(sr, np.stack([tone, tone], 1), {}).get("stereo_width")
    check_true("8 same sound left and right: width 0", w is not None and w < 0.01, w)
    w = tout.measure_oneshot(sr, np.stack([tone, -tone], 1), {}).get("stereo_width")
    check_true("8 left and right opposed: width 1", w is not None and w > 0.99, w)
    # swing: straight kicks on the beats have no off-beat hit to measure
    n = 4 * sr
    kick = np.zeros(n)
    k = np.arange(int(0.2 * sr)) / sr
    hit = np.sin(2 * np.pi * 60 * k) * np.exp(-k / 0.04)
    for b in range(8):
        kick[b * sr // 2:b * sr // 2 + hit.size] += 0.8 * hit
    r = tout.measure_drum_loop(sr, kick[:, None], {"bpm": 120.0})
    check_eq("8 four kicks to the bar: swing reads 50 (straight)", r.get("swing_pct"), 50.0)


def main():
    with np.errstate(all="ignore"):
        for fn in (test_confirm_wired, test_confirm_wired_kits, test_pack_style, test_kit_rows_traceable,
                   test_mutations_run_every_selftest, test_end_of_tempo_named_hits, test_doc_matches_fence,
                   test_leftovers, test_mutation_survivors):
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
