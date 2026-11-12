#!/usr/bin/env python3
"""Tour 6, siège son: one row per style (TSV) from the bench (OUT/json/*.json: ps_measure), son6.py's TSV and
son6_kick.py's TSV, against the genre targets of docs/audits/ultra/sieges/tour-6/son/cibles-genres.md.

  tableau_son6.py OUT_DIR out.tsv

Target: (low, high, kind, read on): kind M = measured by a source, E = estimate; read on « I » (whole song, integrated)
or « drop » (sources measuring the loud part: store extracts). « ecart_cible »: LU under the target's low end (0 inside).
"""
import csv
import json
import os
import sys

T = {
    # Hip-hop
    "boom_bap": (-12, -10, "E", "I"), "trap": (-10.3, -6.7, "M", "drop"), "drill": (-10, -8, "E", "I"), "lofi": (-14, -12, "E", "I"),
    "phonk": (-9, -7, "E", "I"), "grime": (-10, -8, "E", "I"), "jersey_club": (-10, -8, "E", "I"),
    # House
    "house": (-9.0, -7.1, "M", "I"), "deep_house": (-10.9, -9.3, "M", "drop"), "tech_house": (-10.0, -7.9, "M", "drop"),
    "uk_garage": (-9.5, -8, "E", "I"), "speed_garage": (-9.5, -8, "E", "I"), "acid_house": (-10, -9, "E", "I"),
    "afro_house": (-10.9, -9.4, "M", "drop"), "disco": (-10.8, -8.5, "M", "drop"), "amapiano": (-10.5, -9, "E", "I"),
    # Techno
    "techno": (-9.9, -7.6, "M", "drop"), "retro_techno": (-10.5, -9, "E", "I"), "detroit": (-10.5, -9, "E", "I"),
    "industrial_techno": (-8.5, -7, "E", "I"), "hard_techno": (-8.0, -6.4, "M", "I"), "acid_techno": (-8, -7, "E", "I"),
    "gabber": (-7, -5, "E", "I"), "acidcore": (-8, -7, "E", "I"), "minimal": (-11.4, -9.4, "M", "drop"), "trance": (-8.1, -6.7, "M", "drop"),
    # Bass music
    "dnb": (-7.9, -5.2, "M", "drop"), "liquid_dnb": (-9.5, -8, "E", "I"), "neurofunk": (-7, -5.5, "E", "I"), "jungle": (-10, -8, "E", "I"),
    "jump_up": (-7, -5.5, "E", "I"), "dubstep": (-6.5, -4, "M", "drop"), "riddim": (-6, -4, "E", "I"),
    # Autres
    "reggaeton": (-11.0, -8.2, "M", "I"), "afrobeats": (-10.8, -8.4, "M", "I"), "synthwave": (-11, -9, "E", "I"), "pop": (-11.2, -8.1, "M", "I"),
}

KEEP = ["lufs_i", "main_lufs", "true_peak", "lra", "clipped", "corr", "corr_below_120", "mono_loss_lu", "side_mid", "slope_db_oct",
        "b_sub", "b_basse", "b_bas_medium", "b_medium", "b_presence", "b_air", "perte_tel", "perte_tel400",
        "tel_basse", "tel_bas_medium", "tel_medium", "tel_presence", "drop_minus_groove", "drop_groove_tel", "silences", "sauts",
        "kick_garde_tel", "kick_contre_mix", "kick_contre_mix_tel", "kick_contre_mix_tel400", "bass_phone_300", "bass_garde_tel", "bass_contre_mix_tel",
        "bass_brillance_db", "bass_platitude_db", "bass_mouvement", "bass_centroide_hz", "bass_largeur_db",
        "lead_contre_mix_tel", "lead_brillance_db", "lead_platitude_db", "lead_mouvement", "lead_centroide_hz", "lead_centroide_ecart_oct", "lead_largeur_db"]


def f(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def main(argv):
    out_dir, tsv = argv
    son = {r["style"]: r for r in csv.DictReader(open(os.path.join(out_dir, "son6.tsv")), delimiter="\t")}
    kick = {}
    for r in csv.DictReader(open(os.path.join(out_dir, "kick.tsv")), delimiter="\t"):
        lab = r["partie"]
        pri = 0 if lab.startswith("Drop") else 1 if lab.startswith("Refrain") else 2 if lab.startswith(("Groove", "Couplet")) else 9
        if r["style"] not in kick or pri < kick[r["style"]][0]:
            kick[r["style"]] = (pri, r)
    head = ["style", "cible_lufs", "cible_sorte", "lu_sous_cible"] + KEEP + ["kick_partie", "coup_kick_plein", "coup_kick_tel", "coup_kick_tel400", "part_sous_250_pct"]
    with open(tsv, "w", encoding="utf-8") as out:
        out.write("\t".join(head) + "\n")
        for s in sorted(son):
            r = dict(son[s])
            lo, hi, kind, on = T[s]
            v = f(r["main_lufs"] if on == "drop" else r["lufs_i"])
            gap = max(0.0, lo - v) if v is not None else None
            k = kick.get(s, (9, {}))[1]
            share = 10 ** (f(r["b_sub"]) / 10) + 10 ** (f(r["b_basse"]) / 10)
            row = [s, f"{lo:g} à {hi:g} ({'Drop' if on == 'drop' else 'morceau'})", "mesurée" if kind == "M" else "estimée",
                   f"{gap:.1f}" if gap is not None else ""] + [r.get(x, "") for x in KEEP] + \
                  [k.get("partie", ""), k.get("coup_plein", ""), k.get("coup_tel", ""), k.get("coup_tel400", ""), f"{100 * share:.0f}"]
            out.write("\t".join(row) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
