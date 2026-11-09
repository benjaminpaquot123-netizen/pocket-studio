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
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DOCS = os.path.join(HERE, "..", "..", "docs", "audits", "ultra")
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


def measured(tour_dir, name, root=None, docs=None):
    """Banc fiable 2.5: what a program can know itself (hors-banc.tsv still wins when it gives a number):
    fichiers_non_commites (git status, submodule type changes aside), relecture_bloquants/graves (the round's
    *-relecture.md tables, rows not « corrigé »), corrections_sans_test (commits of the round's eq/<tour>-* branches
    touching app/src/main without app/src/test), lots_non_joues (sieges.tsv seats without mesures.json),
    sieges_joues and sieges_coupes_par_le_banc (mesures.json: no end, or an end that is the bench's).
    root (the repository) and docs (the folder of the *-relecture.md) default to this repository's (PLAN-RESTE 6.4:
    given by the test)."""
    out = {}
    root = root or os.path.abspath(os.path.join(HERE, "..", ".."))
    docs = docs or DOCS

    def git(*a):
        r = subprocess.run(["git", "-C", root, *a], capture_output=True, text=True)
        return r.stdout if r.returncode == 0 else None
    status = git("status", "--porcelain")
    if status is not None:
        # The audit's own outputs (this round's folder, the base) are written by the fusion just before: not « work left ».
        out["fichiers_non_commites"] = sum(1 for l in status.splitlines() if l.strip() and not l.startswith(" T")
                                           and "docs/audits/ultra/sieges/" not in l and not l.endswith("docs/audits/ultra/base.tsv"))
    # The bench's own reviews (*-banc*-relecture.md) are not the app's.
    reviews = [os.path.join(docs, f) for f in sorted(os.listdir(docs)) if f.startswith(name + "-") and f.endswith("-relecture.md")
               and "-banc" not in f] if os.path.isdir(docs) else []
    if reviews:
        rows = [l for f in reviews for l in read(f).splitlines() if l.startswith("|") and "corrigé" not in l.lower()]
        out["relecture_bloquants"] = sum(1 for l in rows if re.search(r"\|\s*bloquant\s*\|", l, re.I))
        out["relecture_graves"] = sum(1 for l in rows if re.search(r"\|\s*grave\s*\|", l, re.I))
    branches = git("for-each-ref", "--format=%(refname:short)", f"refs/heads/eq/{name}-*")
    if branches is not None and branches.strip():
        untested = 0
        for b in branches.split():
            created = (git("reflog", "show", "--format=%H", b) or "").split()
            if not created:
                untested = None  # no reflog (a fresh clone): unknown, never a silent 0
                break
            for c in (git("rev-list", "--no-merges", f"{created[-1]}..{b}") or "").split():
                files = (git("show", "--name-only", "--format=", c) or "").split()
                if any(f.startswith("app/src/main/") for f in files) and not any(f.startswith("app/src/test/") for f in files):
                    untested += 1
        if untested is not None:
            out["corrections_sans_test"] = untested
    planned = [l.split("\t")[:2] for l in read(os.path.join(tour_dir, "sieges.tsv")).splitlines()
               if l.strip() and not l.startswith(("lot\t", "#"))]
    played, cut = 0, 0
    for lot, seat in planned:
        m = os.path.join(tour_dir, f"lot{lot}", seat, "mesures.json")
        if not os.path.isfile(m):
            continue
        played += 1
        try:
            end = (json.loads(read(m)).get("mesures") or {}).get("fin")
        except ValueError:
            end = None
        # Cut by the bench: no end written, an end of the bench's own, or a freeze the bench saw (mesures « gels »).
        try:
            frozen = int(((json.loads(read(m)).get("mesures") or {}).get("gels")) or 0) > 0
        except (ValueError, TypeError):
            frozen = False
        if not end or frozen or re.match(r"(banc|délai|delai|timeout|temps|rien ne change)", str(end), re.I):
            cut += 1
    if planned and played:
        out["lots_non_joues"] = len({lot for lot, seat in planned} - {lot for lot, seat in planned if os.path.isfile(os.path.join(tour_dir, f"lot{lot}", seat, "mesures.json"))})
        out["sieges_joues"] = played
        out["sieges_coupes_par_le_banc"] = cut
    return out


def round_numbers(tour_dir, base, verdicts_path, total_promises):
    clusters, rejected, seats, promises, incidents = fuse_mod.fuse(tour_dir, base, verdicts_path)
    name = os.path.basename(os.path.normpath(tour_dir))
    defects = sum(len(c["where"]) for c in clusters)
    verdicts = [r for r in fuse_mod.load_tsv(verdicts_path) if r.get("tour") == name]
    verdict_of = {r.get("cle"): r for r in verdicts}
    # PLAN-RESTE 6.4 (META-AUDIT-2 §2): the round's scale (v1 = the tester's word, v2 = the consequence) and two graves
    # counts: declared by the testers (their own scale, no rule applied) / confirmed (kept grave by the fusion: redone
    # by the tester, and neither a « banc? » cluster nor a « banc » verdict). Graves compare only within one scale.
    scale = "+".join(sorted({sc for c in clusters for sc in c["scales"]})) or "?"
    declared_grave = sum(1 for c in clusters for g in c["declared"] if g <= 1)
    confirmed_grave = sum(1 for c in clusters if not c["bench_doubt"] and verdict_of.get(c["key"], {}).get("verdict") != "banc"
                          for g in c["gravities"] if g <= 1)
    serious = [c for c in clusters if c["gravity"] <= 1]
    # Banc fiable 1.6: an item fixed (etat « corrigé », set by the coordinator after the merge) or wanted no longer counts
    # in A, nor a « banc? » cluster (it waits for its verdict, counted in B); B is « ? » while a « banc? » card ≥ medium
    # has no verdict line (written by the bench team only).
    def settled(c):
        return verdict_of.get(c["key"], {}).get("etat", "") in ("voulu", "corrigé")
    appli = [c for c in serious if verdict_of.get(c["key"], {}).get("verdict", "?") != "banc"
             and not settled(c) and not (c.get("bench_doubt") and c["key"] not in verdict_of)]
    bench = [c for c in serious if verdict_of.get(c["key"], {}).get("verdict") == "banc"]
    # A proof « without the bench » is not a Bench* test (external audit: T4-19 cited one).
    bench_proven = all(verdict_of[c["key"]].get("garde_banc", "-") not in ("", "-") and
                       verdict_of[c["key"]].get("preuve_appli", "-") not in ("", "-") and
                       not verdict_of[c["key"]].get("preuve_appli", "").startswith("Bench") for c in bench)
    doubt_unjudged = [c for c in clusters if c.get("bench_doubt") and c["gravity"] <= 2 and c["key"] not in verdict_of]
    back = [c for c in clusters if c["state"] in ("revient", "banc contredit")]
    # « revient ? » candidates (not yet read by a reader, revient.tsv): C cannot be « oui » while one ≥ medium waits.
    maybe = [c for c in clusters if c["state"].endswith(" ?") and c["gravity"] <= 2]
    back_medium = [c for c in back if c["gravity"] <= 2]
    back_minor = [c for c in back if c["gravity"] > 2]
    # Banc fiable 2.3: one verdict per seat and promise (a seat citing it twice counts once; « contredite » counts « non »);
    # « checked » = seen by ≥ 2 distinct seats; the 120 of the catalogue are counted by their ids (P001-P120) only.
    by_promise = {}
    for row in promises:
        seat, pid, text, screen, kept_said = row[:5]
        verdict = "non" if (row[6:7] or [""])[0] == "contredite" else kept_said
        by_promise.setdefault(pid or text, {}).setdefault(seat, verdict)
    contradicted = sum(1 for row in promises if (row[6:7] or [""])[0] == "contredite")
    checked = {p: list(k.values()) for p, k in by_promise.items() if len(k) >= 2}
    kept = [p for p, k in checked.items() if all(x == "oui" for x in k)]
    seen_once = len([p for p in by_promise if re.fullmatch(r"P\d{3}", p)])
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
    for k, v in measured(tour_dir, name).items():
        # External audit: what the program measured wins over a hand-written number.
        if v is not None:
            hb[k] = str(v)

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
        ("C", "Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire", f"{len(back_medium)} ; {len(back_minor)} ; {len(maybe)}", "0 ; ≤ 2 ; 0",
         yes(None if maybe else not back_medium and len(back_minor) <= 2)),
        ("D", "Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois",
         f"{len(kept)}/{len(checked)} ; {seen_once}/{total_promises}" + (f" ; {contradicted} contredite(s)" if contradicted else ""), "≥ 90 % ; ≥ 80 %",
         # No promise with a catalogue id (round 5 used its own ids): not measurable, « ? », never a « non » that looks measured.
         yes(None if not seen_once else bool(checked) and len(kept) >= 0.9 * len(checked) and seen_once >= 0.8 * total_promises)),
        ("E", "Missions réussies (oracle mesures.json)", f"{succeeded}/{missions}", "≥ 90 %", yes(None if not missions else succeeded >= 0.9 * missions)),
        ("F", "Moyens nouveaux (dont vus par ≥ 2 sièges)", f"{len(new_medium)} ({sum(1 for c in new_medium if len(c['seats']) >= 2)})", "≤ 5 (0)",
         yes(len(new_medium) <= 5 and not any(len(c["seats"]) >= 2 for c in new_medium))),
        ("G", "Relecture neuve, corrections sans test, fichiers non commités", ", ".join(hb.get(k, "?") for k in ("relecture_bloquants", "relecture_graves", "corrections_sans_test", "fichiers_non_commites")),
         "0, 0, 0, 0", yes(all_zero("relecture_bloquants", "relecture_graves", "corrections_sans_test", "fichiers_non_commites"))),
        ("H", "Hors banc : CI build + émulateur, points rouges de la recette téléphone", f"{hb.get('ci_build', '?')}/{hb.get('ci_emulateur', '?')}, {hb.get('recette_points_rouges', '?')}",
         "vert/vert, 0", yes(None if ci_ok is None or num(hb, "recette_points_rouges") is None else ci_ok and num(hb, "recette_points_rouges") == 0)),
        ("I", "Couverture : cases vides, lots non joués, sièges joués / coupés par le banc",
         f"{hb.get('cases_vides', '?')}, {hb.get('lots_non_joues', '?')}, {hb.get('sieges_joues', '?')} / {hb.get('sieges_coupes_par_le_banc', '?')}",
         "0, 0, ≥ 16 / ≤ 3", yes(None if any(num(hb, k) is None for k in ("cases_vides", "lots_non_joues", "sieges_joues", "sieges_coupes_par_le_banc"))
                          else num(hb, "cases_vides") == 0 and num(hb, "lots_non_joues") == 0 and num(hb, "sieges_joues") >= 16
                          and num(hb, "sieges_coupes_par_le_banc") <= 3)),
    ]
    curve = {"tour": name, "sieges": len(seats), "fiches": defects, "fiches_par_siege": round(defects / max(1, len(seats)), 1),
             "echelle": scale, "graves_declares_par_siege": round(declared_grave / max(1, len(seats)), 2),
             "graves_confirmes_par_siege": round(confirmed_grave / max(1, len(seats)), 2), "grappes": len(clusters),
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
    # External audit: each round once, in round order (« tour-5 tour-1 » made tour-1 the last one; a round given twice
    # counted twice for the stop).
    tours = sorted({os.path.normpath(t): None for t in tours}, key=lambda t: (fuse_mod.tour_no(os.path.basename(t)) is None, fuse_mod.tour_no(os.path.basename(t)) or 0))
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
    lines += ["## Courbe de convergence", "",
              "`echelle` : v1 = le mot du testeur (bloquant, grave…), v2 = la conséquence (mission-impossible, contourne-en-N-gestes) ; "
              "`graves_declares` : ce que les testeurs ont écrit, à leur échelle ; `graves_confirmes` : ce que la fusion garde grave "
              "(revu par le testeur, ni « banc? » ni verdict « banc »). Les graves ne se comparent qu'entre tours de même échelle.", "",
              "| " + " | ".join(curves[0].keys()) + " |", "|" + "---|" * len(curves[0])]
    lines += ["| " + " | ".join(str(v) for v in c.values()) + " |" for c in curves]
    for a, b in zip(curves, curves[1:]):
        if a["echelle"] != b["echelle"]:
            lines.append(f"\nChangement d'échelle entre {a['tour']} et {b['tour']} ({a['echelle']} → {b['echelle']}) : "
                         "les graves de ces deux tours ne se comparent pas.")
    stop = len(met) >= 2 and met[-1] and met[-2]
    lines += ["", f"**Arrêt de l'audit possible : {'oui' if stop else 'non'}** (A à I remplies sur les deux derniers tours complets)."]
    text = "\n".join(lines) + "\n"
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    print(text, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
