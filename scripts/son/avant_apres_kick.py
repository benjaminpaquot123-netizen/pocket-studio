#!/usr/bin/env python3
"""Tour 6, siège son, after the « kick » lot: one row per style, before → after, from two measured folders.

  avant_apres_kick.py BEFORE_DIR AFTER_DIR out.tsv

BEFORE_DIR: the bench run of the version before (json/, kick.tsv, son6.tsv); AFTER_DIR: scripts/son/haut_parleur.sh's
folder (the speaker renders: json/, kick.tsv, son6.tsv, export.tsv, time.tsv). The kick's lift is read on the song's
main part (Drop, else Refrain, else Groove or Couplet), as in tableau_son6.py.
"""
import csv
import json
import os
import sys


def main_parts(path):
    m = {}
    for r in csv.DictReader(open(path, encoding="utf-8"), delimiter="\t"):
        lab = r["partie"]
        pri = 0 if lab.startswith("Drop") else 1 if lab.startswith("Refrain") else 2 if lab.startswith(("Groove", "Couplet")) else 9
        if r["style"] not in m or pri < m[r["style"]][0]:
            m[r["style"]] = (pri, r)
    return {s: v[1] for s, v in m.items()}


def f(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def d(a, b):
    return "" if a is None or b is None else f"{b - a:+.2f}"


def main(argv):
    before, after, out = argv
    ka, kb = main_parts(os.path.join(before, "kick.tsv")), main_parts(os.path.join(after, "kick.tsv"))
    sa = {r["style"]: r for r in csv.DictReader(open(os.path.join(before, "son6.tsv"), encoding="utf-8"), delimiter="\t")}
    sb_path = os.path.join(after, "son6.tsv")
    sb = {r["style"]: r for r in csv.DictReader(open(sb_path, encoding="utf-8"), delimiter="\t")} if os.path.isfile(sb_path) else {}
    export = dict(l.rstrip("\n").split("\t") for l in open(os.path.join(after, "export.tsv"), encoding="utf-8"))
    times = {l.split("\t")[0]: (float(l.split("\t")[1]), float(l.split("\t")[2])) for l in open(os.path.join(after, "time.tsv"), encoding="utf-8")}
    head = ["style", "partie", "coups", "kick_plein_avant", "kick_plein_apres", "kick_tel_avant", "kick_tel_apres", "kick_pire_avant", "kick_pire_apres",
            "export_sans_haut_parleur", "lufs_i_delta", "drop_lufs_delta", "true_peak_apres", "ecretes_apres", "bas_medium_delta_db", "medium_delta_db",
            "basse_contre_mix_tel_delta", "lead_contre_mix_tel_delta", "kick_contre_mix_tel_delta", "temps_rendu_ratio"]
    with open(out, "w", encoding="utf-8") as o:
        o.write("\t".join(head) + "\n")
        for s in sorted(ka):
            a, b = ka[s], kb[s]
            ja = json.load(open(os.path.join(before, "json", s + ".json")))
            jb = json.load(open(os.path.join(after, "json", s + ".json")))
            ra, rb = sa.get(s, {}), sb.get(s, {})
            t = times.get(s)
            row = [s, a["partie"], b["coups"], a["coup_plein"], b["coup_plein"], a["coup_tel"], b["coup_tel"], a["coup_tel400"], b["coup_tel400"],
                   export.get(s, ""), d(ja["lufs_i"], jb["lufs_i"]), d(ja["main_lufs"], jb["main_lufs"]), f"{jb['true_peak']:.2f}", str(jb.get("clipped", "")),
                   d(ja["bands"]["low_mid"], jb["bands"]["low_mid"]), d(ja["bands"]["mid"], jb["bands"]["mid"]),
                   d(f(ra.get("bass_contre_mix_tel")), f(rb.get("bass_contre_mix_tel"))), d(f(ra.get("lead_contre_mix_tel")), f(rb.get("lead_contre_mix_tel"))),
                   d(f(ra.get("kick_contre_mix_tel")), f(rb.get("kick_contre_mix_tel"))), f"{t[0] / t[1]:.3f}" if t else ""]
            o.write("\t".join(row) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
