#!/usr/bin/env python3
"""Self-test of the name tags and styles in tout.py (audit 2, lot B: defects 6, 8, 10-14, 38).

    python3 -I scripts/sons/selftest_tout_b.py

Trap names come from scripts/sons/audit2/b2_tags.py and b1_names.py. Exit code 1 when a
single check fails. No sample is needed.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tout  # noqa: E402

FAILS = []
COUNT = [0]


def check_eq(name, got, want):
    COUNT[0] += 1
    ok = got == want
    if not ok:
        FAILS.append(name)
    print("%s %-60s got %r  want %r" % ("ok  " if ok else "FAIL", name, got, want))


def tags(rel, typ="melodic_loop"):
    return tout.parse_tags(rel.split("/")[1:], typ)


def confirmed(rel, typ="melodic_loop", bpm=120.0, key="A minor"):
    """Row as measure_* writes it, then the tags confirmed by the measured sound."""
    t = tags(rel, typ)
    row = {"bpm_tag": t["bpm"], "key_tag": tout.key_name(t["key"]),
           "bpm_measured": bpm, "key_measured": key}
    tout.confirm_tags(row, t)
    return row


def test_tempo():
    """Defects 6 and 10: gear numbers are not tempos; tempos written in the name are read."""
    # 6: a gear number, a figure, a sample rate or two numbers is never a sure tempo
    for rel in ("Loops/Juno 106 Chords Loop Am.wav", "Loops/SH-101 Bass Loop Am.wav",
                "Loops/CS-80 Pad Loop Dm.wav", "Loops/Rhodes 73 Loop Cm.wav",
                "Loops/MPC 60 Keys Loop Am.wav", "Loops/90's Piano Loop Cm.wav",
                "Loops/Route 66 Guitar Loop Am.wav", "Loops/Loop 85 Am 140.wav",
                "Loops/Bass Loop 64 Am 140.wav", "Loops/Bass Loop Am 96kHz24bit.wav",
                "Loops/Bass Loop Am 96-kHz.wav", "Loops/Bass Loop Am 96KHZ 24BIT.wav",
                "Loops/Bass Loop Am 96khz_24bit.wav"):
        check_eq("6 no sure bpm: %s" % rel, tags(rel)["bpm"], None)
    check_eq("6 percent is not a tempo", tags("Loops/Loop 100% Wet Am 120.wav")["bpm"], 120.0)
    # 6: a gear number is not confirmed either; two numbers, neither heard -> empty
    for rel in ("Loops/Juno 106 Chords Loop Am.wav", "Loops/Loop 85 Am 140.wav",
                "Loops/Bass Loop 64 Am 140.wav"):
        check_eq("6 empty after the sound: %s" % rel, confirmed(rel)["bpm_tag"], None)
    # 10: a 'BPM' word before the number, a folder that is a number, glued names
    for rel, want in (("Loops/Loop BPM 140.wav", 140.0), ("Loops/Loop bpm140.wav", 140.0),
                      ("Loops/140/Loop 01.wav", 140.0), ("Loops/140 Am/Loop 01.wav", 140.0),
                      ("Loops/Am/Loop 120.wav", 120.0), ("Loops/Loop.120.Am.wav", 120.0),
                      ("Loops/BassLoop120Am.wav", 120.0), ("Loops/bass_loop_120_f#m.wav", 120.0),
                      ("Loops/bass loop 120 am.wav", 120.0), ("Loops/BASS LOOP 120 AM.wav", 120.0),
                      ("Loops/Trap Loop 01 [140] [Am].wav", 140.0)):
        check_eq("10 bpm read: %s" % rel, tags(rel)["bpm"], want)
    check_eq("10 drum loop BPM word before", tags("Drum Loops/Drum Loop BPM 140.wav", "drum_loop")["bpm"], 140.0)
    # 10: a bare number alone counts once the measured tempo confirms it (equal, half, double)
    for rel, typ in (("Loops/Bass Loop 120.wav", "melodic_loop"), ("Loops/Bass Loop 120 01.wav", "melodic_loop"),
                     ("Loops/Piano Loop 120 C.wav", "melodic_loop"), ("Loops/Loop 120 G.wav", "melodic_loop"),
                     ("Loops/Bass Loop 120 CM.wav", "melodic_loop"), ("Loops/Loop 120 Asus4.wav", "melodic_loop"),
                     ("Drum Loops/Drum Loop 120.wav", "drum_loop"), ("Drum Loops/Drum Loop 120 01.wav", "drum_loop"),
                     ("Drum Loops/120 Drum Loop.wav", "drum_loop"), ("Drum Loops/Loop 01 - 120.wav", "drum_loop"),
                     ("Drum Loops/DrumLoop_120.wav", "drum_loop")):
        check_eq("10 bare bpm alone is not sure: %s" % rel, tags(rel, typ)["bpm"], None)
        check_eq("10 bare bpm confirmed at 120: %s" % rel, confirmed(rel, typ)["bpm_tag"], 120.0)
    check_eq("10 bare bpm confirmed at half", confirmed("Loops/Bass Loop 120.wav", bpm=60.0)["bpm_tag"], 120.0)
    check_eq("10 bare bpm confirmed at double", confirmed("Loops/Bass Loop 120.wav", bpm=240.0)["bpm_tag"], 120.0)
    for rel in ("Loops/Loop 210 Am.wav", "Loops/Loop 58 Am.wav", "Drum Loops/Drum Loop 128.wav"):
        check_eq("10 bare bpm far from the sound stays empty: %s" % rel,
                 confirmed(rel, "drum_loop" if "Drum" in rel else "melodic_loop")["bpm_tag"], None)
    check_eq("10 a sure tag is never replaced", confirmed("Loops/Loop 140 BPM Am.wav")["bpm_tag"], 140.0)
    check_eq("10 one-shot numbers stay out", tags("Synth/Bass 120 C.wav", "oneshot_melodic")["bpm"], None)


def main():
    for t in (test_tempo,):
        t()
    print("%d checks, %d failed" % (COUNT[0], len(FAILS)))
    for f in FAILS:
        print("  FAIL", f)
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
