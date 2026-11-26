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


TESTS = [test_kit_midi_key]


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
