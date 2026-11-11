#!/usr/bin/env python3
"""PLAN-RESTE 7.1: the zone report of a team's branch, as fusion-equipes.sh and pre-vol.sh read it (from batch 2).

  rapport-zone.py <branch> <report path>

Run from the repository's root. The report (docs/audits/ultra/<tour-N>-<zone>-rapport.md) is read in the branch only.
Accepted only when it has a line « tests: <filter> [<filter> …] », the team's targeted tests (Gradle --tests filters),
or « tests: aucun : <raison> ».
Prints « filtre: <filter> » per filter when accepted; otherwise one line per refusal, exit 1. Standard library only.
"""
import re
import subprocess
import sys

TESTS = re.compile(r"^\s*(?:[-*]\s*)?tests\s*:(.*)$", re.M)
NONE = re.compile(r"^aucun\s*[:—-]\s*\S", re.I)


def show(branch, path):
    r = subprocess.run(["git", "show", f"{branch}:{path}"], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else None


def check(branch, path):
    """(refusals, filters)."""
    text = show(branch, path)
    if text is None:
        return [f"no zone report {path} in {branch} (with its « tests: » line and its list of paths)"], []
    found = [m.group(1).strip().strip("`").strip() for m in TESTS.finditer(text)]
    found = [f for f in found if f]
    if not found:
        return [f"no line « tests: » in {path} (the team's targeted tests, or « tests: aucun : <raison> »)"], []
    filters = []
    for value in found:
        if re.match(r"^aucun\b", value, re.I) and not NONE.match(value):
            return [f"« tests: aucun » without its reason in {path} (« tests: aucun : <raison> »)"], []
        if not NONE.match(value):
            filters += [f for f in re.split(r"[\s,`]+", value) if f]
    return [], filters


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    refusals, filters = check(argv[1], argv[2])
    for r in refusals:
        print(r)
    if refusals:
        return 1
    for f in filters:
        print("filtre: " + f)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
