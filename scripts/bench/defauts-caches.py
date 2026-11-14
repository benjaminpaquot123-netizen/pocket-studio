#!/usr/bin/env python3
"""The defects a person saw on the device and the bench did not (audit « banc crédible » 2, MT1; PLAN-RESTE § 5, criterion 9).

    python3 -I scripts/bench/defauts-caches.py [--tsv docs/audits/ultra/defauts-caches.tsv]
        [--retours docs/audits/ultra/retours-benjamin.md] [--plan docs/audits/ultra/PLAN-RESTE.md] [--root DIR]

The exit criteria counted only one way the bench is wrong (a false defect); this counts the other one: a defect of the
app that Benjamin saw on his phone and the bench hid. One row per defect (defauts-caches.tsv). Gaps (one line each,
exit 1 when there is one):
  - a trial « tel-NN ❌ » (or a « défaut partiel ») of retours-benjamin.md with no row whose source is tel-NN;
  - a value outside its list (vu: oui / partiel / non; cause, for partiel or non: ecran-texte, faux-moteur, temps,
    fichier, toucher, son, jugement, hors-mission; reparable: oui / non);
  - a hidden one (partiel, non) that can be repaired, not shown today by a rejeu, whose plan line (A.20, R.8 …) is not
    a row of PLAN-RESTE.md: a « non » with a cause one can repair opens a line of the plan;
  - a rejeu « Class#method » that names no test of app/src/test.
The rate: among the defects the bench can see (seen at the trial, or repairable), how many it shows today (seen at the
trial, or a rejeu that shows it now); a partial one at the trial counts ½. Threshold: RATE_MIN. Standard library only.
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ULTRA = os.path.join("docs", "audits", "ultra")
RATE_MIN = 0.8
SEEN = ("oui", "partiel", "non")
CAUSES = ("ecran-texte", "faux-moteur", "temps", "fichier", "toucher", "son", "jugement", "hors-mission")
COLUMNS = ("id", "date", "source", "defaut", "verdicts", "vu", "cause", "preuve", "rejeu", "reparable", "plan")


def read(path):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return None


def rows_of(text):
    lines = [l for l in (text or "").splitlines() if l.strip() and not l.startswith("#")]
    if not lines:
        return []
    head = lines[0].split("\t")
    return [dict(zip(head, (c.strip() for c in l.split("\t")))) for l in lines[1:]]


def failed_trials(retours):
    """The trials of the owner's notes that found a defect: « tel-NN ❌ », « tel-NN = ❌ », or a line that says
    « défaut partiel » (its first tel-NN)."""
    out = set()
    for line in (retours or "").splitlines():
        out.update(m.group(1) for m in re.finditer(r"\b(tel-\d{2})\b[^\n]{0,8}?❌", line))
        if "défaut partiel" in line:
            m = re.search(r"\b(tel-\d{2})\b", line)
            if m:
                out.add(m.group(1))
    return out


def plan_ids(plan):
    return set(re.findall(r"^\|\s*([A-Z]\.\d+)\s*\|", plan or "", re.M))


def test_exists(root, ref):
    cls, _, method = ref.partition("#")
    if not cls or not method:
        return False
    for d, _, files in os.walk(os.path.join(root, "app", "src", "test", "java")):
        if cls + ".kt" in files:
            text = read(os.path.join(d, cls + ".kt")) or ""
            if re.search(r"\bfun\s+`?" + re.escape(method) + r"\b", text):
                return True
    return False


def summary(tsv_path, retours_path=None, plan_path=None, root=ROOT):
    """(gaps, numbers): numbers = {total, seen, partial, hidden, visible, shown_today, rate, rate_trial}."""
    gaps = []
    text = read(tsv_path)
    if text is None:
        return [f"fichier absent : {tsv_path}"], None
    rows = rows_of(text)
    head = [l for l in text.splitlines() if l.strip() and not l.startswith("#")][:1]
    if not head or tuple(head[0].split("\t")) != COLUMNS:
        gaps.append("en-tête : " + "\t".join(COLUMNS) + " attendu")
    retours = read(retours_path) if retours_path else None
    if retours is None:
        gaps.append(f"notes de Benjamin absentes : {retours_path}")
    sources = {r.get("source", "") for r in rows}
    for t in sorted(failed_trials(retours)):
        if t not in sources:
            gaps.append(f"{t} : essai ❌ de retours-benjamin.md sans ligne")
    ids = plan_ids(read(plan_path) if plan_path else "")
    visible, today, trial = 0, 0.0, 0.0
    for r in rows:
        rid, vu, cause, rejeu, rep, plan = (r.get(k, "") for k in ("id", "vu", "cause", "rejeu", "reparable", "plan"))
        if vu not in SEEN:
            gaps.append(f"{rid} : vu « {vu} » (oui, partiel ou non)")
            continue
        if vu != "oui" and cause not in CAUSES:
            gaps.append(f"{rid} : cause « {cause} » hors liste")
        if vu != "oui" and rep not in ("oui", "non"):
            gaps.append(f"{rid} : réparable « {rep} » (oui ou non)")
        shown = [x.strip() for x in rejeu.split(",") if x.strip() and x.strip() != "-"]
        for ref in shown:
            if not test_exists(root, ref):
                gaps.append(f"{rid} : rejeu {ref} introuvable")
        if vu != "oui" and rep == "oui" and not shown:
            pid = plan.split(" ")[0] if plan else ""
            if pid not in ids:
                gaps.append(f"{rid} : caché, réparable, sans ligne de plan dans PLAN-RESTE.md (« {plan or '-'} »)")
        if vu == "oui" or rep == "oui":
            visible += 1
            trial += 1 if vu == "oui" else 0.5 if vu == "partiel" else 0
            today += 1 if vu == "oui" or shown else 0.5 if vu == "partiel" else 0
    numbers = {
        "total": len(rows), "seen": sum(r.get("vu") == "oui" for r in rows), "partial": sum(r.get("vu") == "partiel" for r in rows),
        "hidden": sum(r.get("vu") == "non" for r in rows), "visible": visible, "shown_today": today,
        "rate_trial": trial / visible if visible else None, "rate": today / visible if visible else None,
    }
    return gaps, numbers


def line(numbers):
    if numbers is None:
        return "défauts cachés : ?"
    pct = "?" if numbers["rate"] is None else f"{round(100 * numbers['rate'])} %"
    trial = "?" if numbers["rate_trial"] is None else f"{round(100 * numbers['rate_trial'])} %"
    return (f"défauts vus sur l'appareil : {numbers['total']} (à l'essai : {numbers['seen']} vus, {numbers['partial']} en partie, "
            f"{numbers['hidden']} cachés) ; visibles au banc : {numbers['visible']} ; montrés aujourd'hui : "
            f"{numbers['shown_today']:g}/{numbers['visible']} ({pct}, à l'essai {trial}) ; seuil {round(100 * RATE_MIN)} %")


def main(argv):
    def opt(n, d):
        return argv[argv.index(n) + 1] if n in argv else d
    if "-h" in argv or "--help" in argv:
        print(__doc__)
        return 2
    root = opt("--root", ROOT)
    tsv = opt("--tsv", os.path.join(root, ULTRA, "defauts-caches.tsv"))
    retours = opt("--retours", os.path.join(root, ULTRA, "retours-benjamin.md"))
    plan = opt("--plan", os.path.join(root, ULTRA, "PLAN-RESTE.md"))
    gaps, numbers = summary(tsv, retours, plan, root)
    for g in gaps:
        print(g)
    print(line(numbers))
    met = numbers is not None and numbers["rate"] is not None and numbers["rate"] >= RATE_MIN
    print(f"défauts cachés : {len(gaps)} écart(s), critère 9 {'rempli' if met and not gaps else 'non rempli'}")
    return 1 if gaps else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
