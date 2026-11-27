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


TESTS = [test_key_breakdown]


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
