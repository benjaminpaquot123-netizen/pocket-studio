#!/usr/bin/env python3
"""The dashboard of the audit ultra, by program, never by hand (method v2, META-AUDIT change 10; analysis Q7 §6-7).

    tableau-de-bord.py <round folder> [<round folder> …] [--out <last round>/tableau-de-bord.md]
                       [--base docs/audits/ultra/base.tsv] [--verdicts docs/audits/ultra/verdicts.tsv] [--promesses-total 120]

A round folder is docs/audits/ultra/sieges/tour-N (lotX/s/rapport.md, journal.txt, mesures.json when seat-metrics ran)
plus, written by the coordinator at the end of the round, hors-banc.tsv (key<TAB>value: relecture_bloquants,
relecture_graves, corrections_sans_test, tests_rejeu_graves, fichiers_non_commites, ci_build, ci_emulateur,
recette_points_rouges, cases_vides, lots_non_joues, sieges_coupes_par_le_banc). A value missing is « ? »: the line is
then NOT met (the criterion is never reached by lack of a number).

Writes one table per round (the stop criterion's 9 conditions A-I, each « oui », « non » or « ? ») and the convergence
curve across the rounds given. The stop criterion (docs/audits/AUDIT-ULTRA.md §9): A-I « oui » on each of the last two
complete rounds. Standard library only.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
fuse_mod = __import__("fuse-reports")


def read(path):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""


def kv(path):
    out = {}
    for line in read(path).splitlines():
        if "\t" in line and not line.startswith("#"):
            k, v = line.split("\t", 1)
            out[k.strip()] = v.strip()
    return out


def num(d, k):
    v = d.get(k, "?")
    return int(v) if re.fullmatch(r"-?\d+", v) else None


def round_numbers(tour_dir, base, verdicts_path, total_promises):
    clusters, rejected, seats, promises, incidents = fuse_mod.fuse(tour_dir, base, verdicts_path)
    name = os.path.basename(os.path.normpath(tour_dir))
    defects = sum(len(c["where"]) for c in clusters)
    declared_grave = sum(1 for c in clusters for g in c["gravities"] if g <= 1)
    verdicts = [r for r in fuse_mod.load_tsv(verdicts_path) if r.get("tour") == name]
    verdict_of = {r.get("cle"): r for r in verdicts}
    serious = [c for c in clusters if c["gravity"] <= 1]
    # Banc fiable 1.6: an item fixed (etat « corrigé », set by the coordinator after the merge) or wanted no longer counts
    # in A, nor a « banc? » cluster (it waits for its verdict, counted in B); B is « ? » while a « banc? » card ≥ medium
    # has no verdict line (written by the bench team only).
    def settled(c):
        return verdict_of.get(c["key"], {}).get("etat", "") in ("voulu", "corrigé")
    appli = [c for c in serious if verdict_of.get(c["key"], {}).get("verdict", "?") != "banc"
             and not settled(c) and not c.get("bench_doubt")]
    bench = [c for c in serious if verdict_of.get(c["key"], {}).get("verdict") == "banc"]
    bench_proven = all(verdict_of[c["key"]].get("garde_banc", "-") not in ("", "-") and
                       verdict_of[c["key"]].get("preuve_appli", "-") not in ("", "-") for c in bench)
    doubt_unjudged = [c for c in clusters if c.get("bench_doubt") and c["gravity"] <= 2 and c["key"] not in verdict_of]
    back = [c for c in clusters if c["state"] in ("revient", "banc contredit")]
    back_medium = [c for c in back if c["gravity"] <= 2]
    back_minor = [c for c in back if c["gravity"] > 2]
    by_promise = {}
    for seat, pid, text, screen, kept, why in promises:
        by_promise.setdefault(pid or text, []).append(kept)
    checked = {p: k for p, k in by_promise.items() if len(k) >= 2}
    kept = [p for p, k in checked.items() if all(x == "oui" for x in k)]
    seen_once = len(by_promise)
    missions, succeeded = 0, 0
    for root, _, files in os.walk(tour_dir):
        if "mesures.json" in files:
            try:
                m = json.loads(read(os.path.join(root, "mesures.json")))
            except ValueError:
                continue
            if m.get("mission"):
                missions += 1
                succeeded += m.get("resultat") == "reussie"
    new_medium = [c for c in clusters if c["new"] and c["gravity"] == 2]
    hb = kv(os.path.join(tour_dir, "hors-banc.tsv"))

    def yes(cond):
        return "?" if cond is None else ("oui" if cond else "non")

    def all_zero(*keys):
        vals = [num(hb, k) for k in keys]
        return None if any(v is None for v in vals) else all(v == 0 for v in vals)
    ci_ok = None if not hb.get("ci_build") or not hb.get("ci_emulateur") else (hb["ci_build"] == "vert" and hb["ci_emulateur"] == "vert")
    rows = [
        ("A", "Items ≥ grave côté appli, non tranchés ou non faits", len(appli), "0", yes(len(appli) == 0)),
        ("B", "Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict",
         f"{len(bench)}{'' if bench_proven else ' (sans preuve)'} ; {len(doubt_unjudged)}", "≤ 2, prouvés ; 0",
         yes(None if doubt_unjudged else len(bench) <= 2 and bench_proven)),
        ("C", "Revient / banc contredit (≥ moyen ; mineurs)", f"{len(back_medium)} ; {len(back_minor)}", "0 ; ≤ 2", yes(not back_medium and len(back_minor) <= 2)),
        ("D", "Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois",
         f"{len(kept)}/{len(checked)} ; {seen_once}/{total_promises}", "≥ 90 % ; ≥ 80 %",
         yes(bool(checked) and len(kept) >= 0.9 * len(checked) and seen_once >= 0.8 * total_promises)),
        ("E", "Missions réussies (oracle mesures.json)", f"{succeeded}/{missions}", "≥ 90 %", yes(None if not missions else succeeded >= 0.9 * missions)),
        ("F", "Moyens nouveaux (dont vus par ≥ 2 sièges)", f"{len(new_medium)} ({sum(1 for c in new_medium if len(c['seats']) >= 2)})", "≤ 5 (0)",
         yes(len(new_medium) <= 5 and not any(len(c["seats"]) >= 2 for c in new_medium))),
        ("G", "Relecture neuve, corrections sans test, fichiers non commités", ", ".join(hb.get(k, "?") for k in ("relecture_bloquants", "relecture_graves", "corrections_sans_test", "fichiers_non_commites")),
         "0, 0, 0, 0", yes(all_zero("relecture_bloquants", "relecture_graves", "corrections_sans_test", "fichiers_non_commites"))),
        ("H", "Hors banc : CI build + émulateur, points rouges de la recette téléphone", f"{hb.get('ci_build', '?')}/{hb.get('ci_emulateur', '?')}, {hb.get('recette_points_rouges', '?')}",
         "vert/vert, 0", yes(None if ci_ok is None or num(hb, "recette_points_rouges") is None else ci_ok and num(hb, "recette_points_rouges") == 0)),
        ("I", "Couverture : cases vides, lots non joués, sièges coupés par le banc", ", ".join(hb.get(k, "?") for k in ("cases_vides", "lots_non_joues", "sieges_coupes_par_le_banc")),
         "0, 0, ≤ 3", yes(None if any(num(hb, k) is None for k in ("cases_vides", "lots_non_joues", "sieges_coupes_par_le_banc"))
                          else num(hb, "cases_vides") == 0 and num(hb, "lots_non_joues") == 0 and num(hb, "sieges_coupes_par_le_banc") <= 3)),
    ]
    curve = {"tour": name, "sieges": len(seats), "fiches": defects, "fiches_par_siege": round(defects / max(1, len(seats)), 1),
             "graves_declares_par_siege": round(declared_grave / max(1, len(seats)), 2), "grappes": len(clusters),
             "revient": len(back), "incidents_banc": len(incidents), "promesses_tenues": f"{len(kept)}/{len(checked)}"}
    return name, rows, curve


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return 2
    docs = os.path.join(HERE, "..", "..", "docs", "audits", "ultra")

    def opt(n, d):
        return argv[argv.index(n) + 1] if n in argv else d
    base = opt("--base", os.path.join(docs, "base.tsv"))
    verdicts = opt("--verdicts", os.path.join(docs, "verdicts.tsv"))
    total = int(opt("--promesses-total", "120"))
    flags = {"--out", "--base", "--verdicts", "--promesses-total"}
    tours = [a for i, a in enumerate(argv) if not a.startswith("--") and (i == 0 or argv[i - 1] not in flags)]
    out = opt("--out", os.path.join(tours[-1], "tableau-de-bord.md"))
    lines = ["# Tableau de bord de l'audit ultra", "", "Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.", ""]
    met = []
    curves = []
    for t in tours:
        name, rows, curve = round_numbers(t, base, verdicts, total)
        curves.append(curve)
        met.append(all(r[4] == "oui" for r in rows))
        lines += [f"## {name}", "", "| | Condition | Mesure | Seuil | Remplie |", "|---|---|---|---|---|"]
        lines += [f"| {r[0]} | {r[1]} | {r[2]} | {r[3]} | {r[4]} |" for r in rows]
        lines += ["", f"Toutes remplies : {'oui' if met[-1] else 'non'}", ""]
    lines += ["## Courbe de convergence", "", "| " + " | ".join(curves[0].keys()) + " |", "|" + "---|" * len(curves[0])]
    lines += ["| " + " | ".join(str(v) for v in c.values()) + " |" for c in curves]
    stop = len(met) >= 2 and met[-1] and met[-2]
    lines += ["", f"**Arrêt de l'audit possible : {'oui' if stop else 'non'}** (A à I remplies sur les deux derniers tours complets)."]
    text = "\n".join(lines) + "\n"
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    print(text, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
