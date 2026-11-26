#!/usr/bin/env python3
"""Self-test of the repair of tout.py after audit 3 (the reunion's defects).

    python3 -I scripts/sons/selftest_tout_repare.py

Same frame as selftest_tout_reuni.py: the real command line on small synthetic
packs written to a temporary folder outside the repository. Exit code 1 when a
single check fails. No sample is needed.
"""
import contextlib
import io
import os
import sys

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
                 "Garage Band Loops", "Metal Percussion", "Acid Jazz", "Drum Hits 2", "archive_03"):
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


TESTS = [test_single_root_folder_is_the_pack, test_pack_style_named, test_oneshot_with_tempo, test_lone_letter_root, test_kit_midi_key, test_drum_stem_no_key, test_bare_equal_first, test_bars_when_tempo_contradicted]


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
