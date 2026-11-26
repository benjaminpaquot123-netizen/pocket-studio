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


TESTS = [test_lone_letter_root, test_kit_midi_key, test_drum_stem_no_key, test_bare_equal_first, test_bars_when_tempo_contradicted]


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
