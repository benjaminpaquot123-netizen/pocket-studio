#!/usr/bin/env python3
"""Self-test for cibles_batterie.py on a fake table (no real sound data).

Run: python3 -I scripts/sons/fin/selftest_cibles.py
"""
import csv
import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cibles_batterie as cb  # noqa: E402

ONE = ["rar", "pack", "type", "subtype", "style", "dur_s", "peak_dbfs", "rms_dbfs",
       "t20_ms", "t60_ms", "pitch_steady_hz", "centroid_hz", "hf_db", "transient_body_db", "cle"]
VER = ["cle"] + [m + "_ok" for m in cb.ONESHOT_MEASURES]


def row(rar, pack, sub, style, i, t20=100.0, ok=True):
    cle = "r%d-%s-%04x" % (rar, pack, i)
    vals = {"rar": rar, "pack": pack, "type": "drum_oneshot", "subtype": sub, "style": style,
            "dur_s": 0.5, "peak_dbfs": -1.0, "rms_dbfs": -12.0, "t20_ms": t20 + i % 7,
            "t60_ms": 300 + i % 5, "pitch_steady_hz": 55, "centroid_hz": 2000,
            "hf_db": -30, "transient_body_db": -10, "cle": cle}
    ver = {"cle": cle}
    for m in cb.ONESHOT_MEASURES:
        ver[m + "_ok"] = "yes" if ok else "no"
    return vals, ver


def build(d):
    rows, vers = [], []
    i = 0

    def add(rar, pack, sub, style, n, **kw):
        nonlocal i
        for _ in range(n):
            a, b = row(rar, pack, sub, style, i, **kw)
            rows.append(a)
            vers.append((rar, b))
            i += 1
    add(1, "p1", "kick", "techno", 12)          # techno kick: 2 packs -> definitive
    add(2, "p1", "kick", "techno", 12)
    add(3, "p1", "kick", "house", 25)           # house kick: one pack -> fails (2)
    add(4, "p1", "snare", "dnb", 15)            # dnb snare: only 19 -> fails (1)
    add(4, "p2", "snare", "dnb", 4)
    add(5, "p1", "clap", "dubstep", 46)         # dubstep clap: 46+5 -> pack > 90 % fails (3)
    add(5, "p2", "clap", "dubstep", 5)
    add(6, "p1", "hat_closed", "dnb", 10)       # dnb hat: 1 of 21 not ok -> fails (4)
    add(6, "p2", "hat_closed", "dnb", 10)
    add(6, "p2", "hat_closed", "dnb", 1, ok=False)
    add(7, "p1", "tom", "dubstep", 18, t20=100.0)   # dubstep tom: big pack far away -> fails (5)
    add(7, "p2", "tom", "dubstep", 5, t20=400.0)
    add(8, "p1", "hat_open", "drift-phonk", 10)  # trap via drift-phonk + trap
    add(8, "p2", "hat_open", "trap", 10)
    add(9, "p1", "kick", "dnb", 20)             # excluded key brings dnb kick under 20
    add(9, "p2", "kick", "dnb", 5)
    os.makedirs(os.path.join(d, "t"))
    os.makedirs(os.path.join(d, "v"))
    with open(os.path.join(d, "t", "drum_oneshot.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, ONE)
        w.writeheader()
        w.writerows(rows)
    with open(os.path.join(d, "t", "drum_loop.csv"), "w", newline="") as f:
        csv.DictWriter(f, ONE).writeheader()
    for rar in range(1, 10):
        with open(os.path.join(d, "v", "rar%d.csv" % rar), "w", newline="") as f:
            w = csv.DictWriter(f, VER)
            w.writeheader()
            w.writerows(b for r, b in vers if r == rar)
    excl = [r["cle"] for r in rows if r["rar"] == 9][:6]
    with open(os.path.join(d, "excl.tsv"), "w") as f:
        f.write("cle\tfamille_table\tsource\traison\n")
        for c in excl:
            f.write("%s\tkick\ttest\ttest\n" % c)
    return d


def main():
    with tempfile.TemporaryDirectory() as d:
        build(d)
        out = os.path.join(d, "out.tsv")
        cb.run(os.path.join(d, "t"), os.path.join(d, "v"), os.path.join(d, "excl.tsv"), out)
        got = {}
        with open(out) as f:
            for r in csv.DictReader((l for l in f if not l.startswith("#")), delimiter="\t"):
                got[(r["famille"], r["style"], r["mesure"])] = r
    fails = []

    def check(key, prefix, what):
        st = got.get(key, {}).get("statut", "<absent>")
        if not st.startswith(prefix):
            fails.append("%s: %s expected %r, got %r" % (what, key, prefix, st))
    check(("kick", "techno", "t20_ms"), "définitive", "two packs")
    check(("kick", "house", "t20_ms"), "pas de cible : (2)", "one pack")
    check(("caisse_claire", "dnb", "t20_ms"), "pas de cible : (1)", "under 20")
    check(("clap", "dubstep", "t20_ms"), "pas de cible : (3)", "pack over 90 %")
    check(("charley_ferme", "dnb", "t20_ms"), "pas de cible : (4)", "verif disagreement")
    check(("tom", "dubstep", "t20_ms"), "pas de cible : (5)", "leave-largest-out")
    check(("tom", "dubstep", "rms_dbfs"), "définitive", "same values in both packs")
    check(("charley_ouvert", "trap", "t20_ms"), "définitive (regroupement trap+drift-phonk)", "trap grouping")
    check(("kick", "techhouse", "t20_ms"), "définitive (regroupement house+techno)", "techhouse grouping")
    check(("kick", "dnb", "t20_ms"), "pas de cible : (1)", "exclusions applied")
    check(("caisse_claire", "house", "t20_ms"), "pas de cible : (1)", "empty cell present")
    if ("kick", "techno", "t40_ms") in got:
        fails.append("t40 must not be a target")
    if ("caisse_claire", "techno", "pitch_steady_hz") in got:
        fails.append("note is for the kick only")
    k = got.get(("kick", "techno", "t20_ms"), {})
    if k and (k.get("coups") != "24" or k.get("packs") != "2" or k.get("liste_packs") != "r1/p1 r2/p1"):
        fails.append("kick techno counts wrong: %r" % ({x: k.get(x) for x in ("coups", "packs", "liste_packs")},))
    if k and abs(float(k["mediane"]) - 102.5) > 1e-6:
        fails.append("kick techno median %s != 102.5" % k["mediane"])
    if fails:
        print("ÉCHEC")
        for x in fails:
            print(" -", x)
        return 1
    print("selftest cibles : ok (%d cases)" % 15)
    return 0


if __name__ == "__main__":
    sys.exit(main())
