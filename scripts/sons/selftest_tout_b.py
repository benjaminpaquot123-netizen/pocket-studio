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
    check_eq("10 a BPM word up to 250", tags("Loops/Loop 220 BPM Am.wav")["bpm"], 220.0)
    check_eq("10 '140-bpm' is read", tags("Loops/Loop 140-bpm.wav")["bpm"], 140.0)
    check_eq("10 a slow tempo beside a key", tags("Loops/Loop 85 Am.wav")["bpm"], 85.0)
    check_eq("6 'Bass-Loop-120-Am' keeps its tempo", tags("Loops/Bass-Loop-120-Am.wav")["bpm"], 120.0)
    check_eq("6 'TB-303 Loop Am' is a model", tags("Loops/TB-303 Loop Am.wav")["bpm"], None)
    check_eq("10 one-shot numbers stay out", tags("Synth/Bass 120 C.wav", "oneshot_melodic")["bpm"], None)


def test_key():
    """Defects 8, 11 and 38: no false key, a lone key letter checked by the sound, flats kept."""
    # 8: a variation letter, a drum loop letter, an instrument name are not keys
    for rel, typ in (("Loops/Synth Loop 120 A.wav", "melodic_loop"), ("Loops/Synth Loop 120 B.wav", "melodic_loop"),
                     ("Drum Loops/Top Loop 128 A.wav", "drum_loop"), ("Drum Loops/Drum Loop 120 B.wav", "drum_loop"),
                     ("Drum Loops/Hat Loop 140 C.wav", "drum_loop"), ("Loops/Fm Synth Loop 120.wav", "melodic_loop"),
                     ("Loops/Bb Trumpet Loop 120.wav", "melodic_loop"), ("Loops/Eb Clarinet Loop 120.wav", "melodic_loop")):
        check_eq("8 no sure key: %s" % rel, tags(rel, typ)["key"], None)
    for rel in ("Drum Loops/Top Loop 128 A.wav", "Drum Loops/Hat Loop 140 C.wav"):
        check_eq("8 drum loop letter stays empty after the sound: %s" % rel,
                 confirmed(rel, "drum_loop", key="A major" if " A" in rel else "C major")["key_tag"], None)
    check_eq("8 'C# m' is C# minor", tags("Loops/Loop 120 C# m.wav")["key"], (1, "minor"))
    check_eq("8 'C#-m' is C# minor", tags("Loops/Loop 120 C#-m.wav")["key"], (1, "minor"))
    check_eq("8 a tuned kick loop keeps its key", tags("Drum Loops/Kick Loop 125 Gm.wav", "drum_loop")["key"],
             (7, "minor"))
    check_eq("8 'Am Synth' is still A minor", tags("Loops/Am Synth Loop 120.wav")["key"], (9, "minor"))
    # 11: a lone key letter is read once the measured key has the same root
    for rel, meas in (("Loops/Pad Loop F.wav", "F major"), ("Loops/Pad Loop - F.wav", "F major"),
                      ("Loops/Pad Loop [F].wav", "F major"), ("Loops/Pad Loop Key F.wav", "F major"),
                      ("Loops/Pad Loop in F.wav", "F minor"), ("Loops/Piano Loop C 120.wav", "C major"),
                      ("Loops/Blues in A 120.wav", "A minor"), ("Loops/Synth Loop 120 A.wav", "A minor")):
        check_eq("11 lone key confirmed: %s" % rel, confirmed(rel, key=meas)["key_tag"], meas)
    for rel in ("Loops/Pad Loop F.wav", "Loops/Pad Loop Key F.wav", "Loops/Blues in A 120.wav"):
        check_eq("11 lone key not heard stays empty: %s" % rel, confirmed(rel, key="D major")["key_tag"], None)
    check_eq("11 a sure key is never replaced", confirmed("Loops/Loop 120 Am.wav", key="C major")["key_tag"],
             "A minor")
    # 38: a flat in the name is shown as a flat
    for rel, want in (("Loops/Bass Loop 120 Bb.wav", "Bb major"), ("Loops/Bass Loop 120 Bbm.wav", "Bb minor"),
                      ("Loops/Bass Loop 120 B\u266dm.wav", "Bb minor"), ("Loops/Bass Loop 120 Db.wav", "Db major"),
                      ("Loops/Loop 120 Dbmaj7.wav", "Db major"), ("Loops/Bass Loop 120 A#m.wav", "A# minor"),
                      ("Loops/Bass Loop 120 Eb minor.wav", "Eb minor")):
        check_eq("38 spelled as written: %s" % rel, confirmed(rel)["key_tag"], want)
    t = tags("Synth/Pluck Gb2.wav", "oneshot_melodic")
    row = {"note_tag": tout.NOTE_NAMES[t["note"]]}
    check_eq("38 one-shot flat note", tout.confirm_tags(row, t)["note_tag"], "Gb")


def note_row(rel, measured, typ="oneshot_melodic"):
    """One-shot row as measure_oneshot writes it, then checked against the measured note."""
    t = tags(rel, typ)
    row = {"note_tag": tout.NOTE_NAMES[t["note"]] if t["note"] is not None else None,
           "note_measured": measured}
    return tout.confirm_tags(row, t)["note_tag"]


def test_note():
    """Defect 12: a letter that starts an instrument name is not a note; a lone letter is checked."""
    for rel in ("Keys/Keys E Piano.wav", "Keys/E Piano 01.wav", "Bass/E Bass 01.wav", "Bass/A Bass 01.wav",
                "Synth/G Funk Lead 01.wav", "Synth/Plan B Lead.wav"):
        check_eq("12 no note: %s" % rel, tags(rel, "oneshot_melodic")["note"], None)
    for rel, want in (("Synth/Bb Bass.wav", 10), ("Synth/Bass A#.wav", 10), ("Synth/Pluck F#3.wav", 6),
                      ("Synth/Lead c3.wav", 0), ("Synth/E Piano C3.wav", 0), ("Synth/Bass C.wav", 0)):
        check_eq("12 note kept: %s" % rel, tags(rel, "oneshot_melodic")["note"], want)
    check_eq("12 lone note far from the sound is dropped", note_row("Synth/Pad Soft F.wav", "A3"), None)
    check_eq("12 lone note heard is kept", note_row("Synth/Pad Soft F.wav", "F3"), "F")
    check_eq("12 a note with an octave is never dropped", note_row("Synth/Pad C3 Wet.wav", "A3"), "C")
    check_eq("12 a drum letter far from the sound is dropped", note_row("Kicks/Kick F#.wav", "A3", "drum_oneshot"),
             None)


def test_style():
    """Defects 13 and 14: a style is a whole word; new styles; the pack folder's own name."""
    for rel in ("Drums/Snares/Gravel Snare 01.wav", "Synth/Brave Lead C3.wav", "Synth/Horn Blast C3.wav",
                "Synth/Modern Bass C2.wav", "Drums/Fills/Solo Fill 01.wav", "FX/Entrance FX 01.wav",
                "Drums/Kicks/Warehouse Kick 01.wav", "Drums/Kicks/Strap Kick 01.wav", "Synth/Technology Lead C3.wav",
                "Drums/Kicks/Popcorn Kick 01.wav", "Electronic Drums/Kick 01.wav", "Jungle Percussion/Conga 01.wav",
                "Drums/Kicks/Garage Door Kick 01.wav", "Drums/Snares/Soulful Snare 01.wav",
                "Drums/Kicks/Trapdoor Kick 01.wav", "Drums/Kicks/Contrapunto Kick 01.wav",
                "Drums/Kicks/Extra Punch Kick 01.wav", "Drums/Kicks/Travel Kick 01.wav",
                "Drums/Kicks/Ultra Phat Kick 01.wav", "Neo Raver Tools/Kick 01.wav", "Ketamine Kicks/Kick 01.wav"):
        check_eq("13 no style: %s" % rel, tout.style_of(rel), "unlabelled")
    for rel, want in (("Drums/Kicks/Pop Kick 01.wav", "pop"), ("Deep House/Kicks/Kick 01.wav", "house"),
                      ("Techno/House Kicks/Kick 01.wav", "house"), ("Hip-Hop & Trap/Kicks/Kick 01.wav", "trap"),
                      ("Lo-Fi House/Kicks/Kick 01.wav", "house"), ("Lo-Fi/Kicks/Kick 01.wav", "lofi"),
                      ("UK Garage/Kicks/Kick 01.wav", "garage"), ("Drum & Bass/Kicks/Kick 01.wav", "dnb"),
                      ("DnB/Kicks/Kick 01.wav", "dnb"), ("Jungle/Breaks/Break 01.wav", "dnb"),
                      ("RnB/Kicks/Kick 01.wav", "rnb"), ("Soul/Keys/Keys 01.wav", "rnb"),
                      ("Boom Bap/Kicks/Kick 01.wav", "hiphop"), ("HipHop/Kicks/Kick 01.wav", "hiphop"),
                      ("TrapKicks/Kick 01.wav", "trap"), ("Trap Beats/Snare 01.wav", "trap"),
                      ("Techno Kicks/Kick 01.wav", "techno"), ("Houses/Kick 01.wav", "house"),
                      ("Afrobeats/Kicks/Kick 01.wav", "afro"), ("Electro/Kicks/Kick 01.wav", "edm"),
                      ("Rock/Kicks/Kick 01.wav", "rock"), ("Reggaeton/Kicks/Kick 01.wav", "reggaeton"),
                      ("Dembow/Kicks/Kick 01.wav", "reggaeton"), ("Drill/Kicks/Kick 01.wav", "drill"),
                      ("Disco/Kicks/Kick 01.wav", "disco"), ("Funk/Kicks/Kick 01.wav", "funk"),
                      ("Hardstyle/Kicks/Kick 01.wav", "hardstyle"), ("Neo Rave/Kicks/Kick 01.wav", "rave"),
                      ("Liquid DnB/Kicks/Kick 01.wav", "dnb"), ("Afro House/Kicks/Kick 01.wav", "afro-house")):
        check_eq("13 style: %s" % rel, tout.style_of(rel), want)
    check_eq("13 classify uses the whole word", tout.classify("Drums/Kicks/Warehouse Kick 01.wav", 0.4)["style"],
             "unlabelled")
    # 14: the pack folder given directly still names the style
    check_eq("14 style from the pack folder", tout.style_of("Kicks/Kick 01.wav", "Techno Essentials Vol 2"),
             "techno")
    check_eq("14 the inner folders win over the pack", tout.style_of("House/Kick 01.wav", "Techno Essentials"),
             "house")
    check_eq("14 no style anywhere", tout.style_of("Kicks/Kick 01.wav", "Essentials Vol 2"), "unlabelled")


def main():
    for t in (test_tempo, test_key, test_note, test_style):
        t()
    print("%d checks, %d failed" % (COUNT[0], len(FAILS)))
    for f in FAILS:
        print("  FAIL", f)
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
