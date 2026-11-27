"""Self-test of the last bench repairs (docs/sons/REPARATION-FIN.md), fabricated data only.

Run: python3 -I scripts/sons/selftest_tout_fin.py
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "audit4"))
import tout  # noqa: E402,F401

FAILS = []
COUNT = [0]


def check_eq(name, got, want):
    COUNT[0] += 1
    ok = got == want
    if not ok:
        FAILS.append(name)
    print("%s %-64s got %r  want %r" % ("ok  " if ok else "FAIL", name, got, want))


def test_key_breakdown():
    """Point 3: key rows split into exact / same tonic / relative / other, by type and RAR; same tonic is wrong."""
    import accuracy
    rows = [{"type": "melodic_loop", "rar": "3", "key_tag": "A minor", "key_est": "A minor"},
            {"type": "melodic_loop", "rar": "3", "key_tag": "A minor", "key_est": "A major"},
            {"type": "melodic_loop", "rar": "3", "key_tag": "A minor", "key_est": "C major"},
            {"type": "melodic_loop", "rar": "5", "key_tag": "F", "key_est": "F major"},
            {"type": "melodic_loop", "rar": "5", "key_tag": "Db major", "key_est": "C# major"},
            {"type": "melodic_loop", "rar": "5", "key_tag": "Eb minor", "key_est": "G minor"},
            {"type": "melodic_loop", "rar": "5", "key_tag": "", "key_est": "G minor"},
            {"type": "kit", "rar": "3", "key_tag": "E minor", "key_est": "G major"}]
    d = accuracy.key_breakdown(rows)
    check_eq("3 rar 3 loops", d.get(("melodic_loop", "3")), {"exact": 1, "same_tonic": 1, "relative": 1, "other": 0})
    check_eq("3 rar 5 loops: root-only tag is same tonic, Db=C#", d.get(("melodic_loop", "5")),
             {"exact": 1, "same_tonic": 1, "relative": 0, "other": 1})
    check_eq("3 all loops", d.get(("melodic_loop", "all")), {"exact": 2, "same_tonic": 2, "relative": 1, "other": 1})
    check_eq("3 kit kept apart", d.get(("kit", "3")), {"exact": 0, "same_tonic": 0, "relative": 1, "other": 0})


def test_drum_note_label():
    """Point 4 (m2): a trailing variant letter on a drum hit is not a note; a real note label is kept in the table."""
    for name in ("Snare A", "Kick 01 B", "Clap C", "Perc_D", "Kick Hard E"):
        check_eq("4 variant letter, no note: " + name, tout.parse_tags([name + ".wav"], "drum_oneshot")["note"], None)
    for name, pc in (("808 C", 0), ("Kick C1", 0), ("Tom F#", 6), ("808 Bass G", 7)):
        check_eq("4 note kept: " + name, tout.parse_tags([name + ".wav"], "drum_oneshot")["note"], pc)
    check_eq("4 melodic one-shot lone letter still a note", tout.parse_tags(["Pluck C.wav"], "oneshot_melodic")["note"], 0)
    check_eq("4 note_tag column in the drum table", "note_tag" in tout.TYPE_COLUMNS["drum_oneshot"], True)
    import numpy as np
    import selftest_tout as st
    import selftest_tout_reuni as ru
    t = np.arange(int(0.5 * st.SR)) / st.SR
    rows, _, _, _, _ = ru.run({"Drums/Kick C1.wav": st.kick(t), "Drums/Kick Hard E.wav": st.kick(t)})
    r1, r2 = rows["Drums/Kick C1.wav"] or {}, rows["Drums/Kick Hard E.wav"] or {}
    check_eq("4 table type of a kick", r1.get("type"), "drum_oneshot")
    check_eq("4 table keeps the label C of 'Kick C1'", r1.get("note_tag"), "C")
    check_eq("4 table: no label for 'Kick Hard E'", r2.get("note_tag") or None, None)


def test_tempo_fold():
    """Point 7 (M6): a tempo heard at half or double (62.5 for 125) is folded apart, not counted as wrong."""
    check_eq("7 fold 62.5 onto 125", tout.fold_bpm(62.5, 125.0), 125.0)
    check_eq("7 fold 250 onto 125", tout.fold_bpm(250.0, 125.0), 125.0)
    check_eq("7 no fold for 100 vs 125", tout.fold_bpm(100.0, 125.0), 100.0)
    check_eq("7 no fold without a tag", tout.fold_bpm(62.5, None), 62.5)
    acc = tout.Accuracy()
    for m in (125.0, 124.0, 62.5, 250.0, 100.0):
        acc.add("drum_loop", "bpm", tout._bpm_class(m, 125.0))
    r = acc.rows()[0]
    check_eq("7 accuracy agree / half_double", (r["agree"], r["half_double"]), (2, 2))
    check_eq("7 raw error_pct still counts the octave as wrong", r["error_pct"], 60.0)
    check_eq("7 error after fold counts only the true miss", r.get("error_fold_pct"), 20.0)
    check_eq("7 usable stays on the raw rate", r["usable"], "no")
    import accuracy
    rows = [{"type": "drum_loop", "rar": "3", "bpm_tag": "125", "bpm_measured": "62.5"},
            {"type": "drum_loop", "rar": "3", "bpm_tag": "125", "bpm_measured": "125.4"},
            {"type": "drum_loop", "rar": "3", "bpm_tag": "125", "bpm_measured": "100"},
            {"type": "melodic_loop", "rar": "5", "bpm_tag": "90", "bpm_est": "180"},
            {"type": "melodic_loop", "rar": "5", "bpm_tag": "", "bpm_est": "180"}]
    d = accuracy.tempo_breakdown(rows)
    check_eq("7 tempo breakdown rar 3 drums", d.get(("drum_loop", "3")), {"exact": 1, "half_double": 1, "other": 1})
    check_eq("7 tempo breakdown rar 5 melodic", d.get(("melodic_loop", "5")), {"exact": 0, "half_double": 1, "other": 0})


TESTS = [test_key_breakdown, test_drum_note_label, test_tempo_fold]


def main():
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
