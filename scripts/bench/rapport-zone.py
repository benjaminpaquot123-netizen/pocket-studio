#!/usr/bin/env python3
"""PLAN-RESTE 7.1: the zone report of a team's branch, as fusion-equipes.sh and pre-vol.sh read it (from batch 2).

  rapport-zone.py <branch> <report path> [ajout:<replay id the branch adds to rejeux/> …]

Run from the repository's root. The report (docs/audits/ultra/<tour-N>-<zone>-rapport.md) is read in the branch only.
Accepted only when it has a line « tests: <filter> [<filter> …] », the team's targeted tests (Gradle --tests filters),
or « tests: aucun : <raison> ».
And its « liste des chemins » (docs/audits/AUDIT-ULTRA.md §12.6): at least one line
« chemin: <écran> | test: Classe#méthode », every « chemin: » line in that format, and each Classe#méthode a test of
app/src/test in the branch that runs (@Test, no @Ignore): the class owns the function, read as the judge reads it
(verifie-chantier.py: class_methods, still_runs).
And each replay the branch adds to docs/audits/ultra/rejeux/ has its last verdict in the report, the line
rejeu-defauts.sh prints (« <id> : oracle tenu »): the last such line is « oracle tenu ». No verdict is committed
anywhere else (rejeu-defauts.sh keeps no trace of a waiting replay), so a replay without one is refused too: a
replay in rejeux/ that is not held undoes every merge after it.
Prints « filtre: <filter> » per filter when accepted; otherwise one line per refusal, exit 1. Standard library only.
"""
import importlib.util
import os
import re
import subprocess
import sys

# (No __pycache__ left in scripts/bench/: the portal merges in that tree.)
sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("verifie_chantier", os.path.join(HERE, "verifie-chantier.py"))
judge = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(judge)

TESTS = re.compile(r"^\s*(?:[-*]\s*)?tests\s*:(.*)$", re.M)
NONE = re.compile(r"^aucun\s*[:—-]\s*\S", re.I)
PATH_LINE = re.compile(r"^\s*(?:[-*]\s*)?(?:\[[ xX]\]\s*)?`?chemin\s*:(.*)$", re.M)
PATH = re.compile(r"^\s*(\S[^|]*?)\s*\|\s*test\s*:\s*([A-Za-z_][A-Za-z0-9_]*)#([A-Za-z_][A-Za-z0-9_]*)\s*`?\s*$")
APP_TESTS = "app/src/test"


def is_app_test(branch, cls, method):
    """Classe#méthode is a function of class Classe in a file of app/src/test of the branch, with @Test, no @Ignore."""
    r = subprocess.run(["git", "grep", "-l", "-F", method, branch, "--", APP_TESTS], capture_output=True, text=True)
    for line in r.stdout.splitlines():
        text = judge.without_comments(show(branch, line.split(":", 1)[1]) or "")
        if f"{cls}#{method}" in judge.class_methods(text, False) and judge.still_runs(text, method):
            return True
    return False


def show(branch, path):
    r = subprocess.run(["git", "show", f"{branch}:{path}"], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else None


def replay_refusals(text, path, added):
    out = []
    for rid in added:
        said = re.findall(r"^\s*(?:[-*]\s*)?(?:rejeu\s*:\s*)?`?" + re.escape(rid) + r"\s*:\s*(oracle\b[^`\n]*)", text, re.M)
        if not said:
            out.append(f"replay {rid} added to rejeux/ without its verdict in {path} (the line « {rid} : oracle tenu » "
                       "of rejeu-defauts.sh, played with the fix)")
        elif said[-1].strip() != "oracle tenu":
            out.append(f"replay {rid} added to rejeux/: its last verdict in {path} is « {said[-1].strip()[:80]} », "
                       "not « oracle tenu » (a defect not fixed waits in rejeux-attente/)")
    return out


def check(branch, path, added=()):
    """(refusals, filters)."""
    text = show(branch, path)
    if text is None:
        return [f"no zone report {path} in {branch} (with its « tests: » line and its list of paths)"], []
    found = [m.group(1).strip().strip("`").strip() for m in TESTS.finditer(text)]
    found = [f for f in found if f]
    if not found:
        return [f"no line « tests: » in {path} (the team's targeted tests, or « tests: aucun : <raison> »)"], []
    refusals = replay_refusals(text, path, added)
    lines = [m.group(0).strip() for m in PATH_LINE.finditer(text)]
    if not lines:
        refusals.append(f"no list of paths in {path} (one line « chemin: <écran> | test: Classe#méthode » per place "
                        "that reads or shows the same data)")
    for line in lines:
        m = PATH.match(PATH_LINE.match(line).group(1))
        if not m:
            refusals.append(f"« {line[:80]} » is not « chemin: <écran> | test: Classe#méthode »")
        elif not is_app_test(branch, m.group(2), m.group(3)):
            refusals.append(f"chemin « {m.group(1)[:40]} »: {m.group(2)}#{m.group(3)} is not a @Test of {APP_TESTS} in {branch}")
    if refusals:
        return refusals, []
    filters = []
    for value in found:
        if re.match(r"^aucun\b", value, re.I) and not NONE.match(value):
            return [f"« tests: aucun » without its reason in {path} (« tests: aucun : <raison> »)"], []
        if not NONE.match(value):
            # Review of banc7 (grave 1): Gradle's --tests knows « Classe.méthode », not « Classe#méthode » (the form of a
            # path line and of the teams' sheet): translated, else no test is found and the merge is undone.
            filters += [f.replace("#", ".") for f in re.split(r"[\s,`]+", value) if f]
    return [], filters


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    refusals, filters = check(argv[1], argv[2], [a.split(":", 1)[1] for a in argv[3:] if a.startswith("ajout:")])
    for r in refusals:
        print(r)
    if refusals:
        return 1
    for f in filters:
        print("filtre: " + f)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
