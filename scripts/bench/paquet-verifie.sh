#!/usr/bin/env bash
# Method v3 (docs/audits/AUDIT-ULTRA.md §13.6, §13.9): the end of a batch (« paquet ») of merges, checked by program.
#   scripts/bench/paquet-verifie.sh [--ouvre] [--tete <commit>]
# Reads, for the pushed head (--tete; by default the branch's upstream, else HEAD):
#   the CI      the latest run of the « CI » workflow for that commit, to the end, job by job. « Emulator smoke test »
#               and « Build, unit tests, screenshots » must be there and green; a job in progress, skipped where it is
#               required, or in a state nobody knows is NOT green; a red job is named;
#   the judge   the line « chantier : N écarts » of the latest portal trace (the selftest-chantier.txt of the latest
#               « Portal traces of the merge … » commit in the head's history, never the last folder by name), compared
#               with the count at the end of the previous batch (scripts/bench/portail-paquets.tsv);
#   the screenshots   the line « Captures : T au total, C différentes, A sans référence. » that scripts/screenshot-diff.py
#               prints in the build job's log; not found = « captures non lues », which is not green.
# Writes the verdict in docs/audits/ultra/EN-COURS.md (a dated block between « <!-- paquet-verifie:debut --> » and
# « <!-- paquet-verifie:fin --> », right under « Ce qui n'est pas vert », replaced at each run; never commits, never
# pushes) and prints ONE line:
#   paquet <n> : CI <numéro> verte | rouge (<travail>) | en cours ; juge N (avant M) ; captures à jour | P périmées | non lues
# Exit 0 only when everything is green. --ouvre (the chief's command), and only when everything is green: writes
# « paquet: <n+1> » in scripts/bench/portail-paquet.txt and adds this batch's line to the ledger; otherwise it refuses,
# says why on stderr and leaves both files as they are.
# Tests (scripts/bench/test_portail3.py) replace GitHub: PS_PAQUET_CI_JSON=<file> (the JSON ci() returns), or
# PS_PAQUET_GH=<a fake gh>; PS_PAQUET_RACINE=<a toy repository>, PS_PAQUET_DATE=<day>.
set -euo pipefail
root="${PS_PAQUET_RACINE:-$(cd "$(dirname "$0")/../.." && pwd)}"
exec python3 -I - "$root" "$@" <<'PY'
import datetime
import json
import os
import re
import subprocess
import sys

REQUIRED = ["Build, unit tests, screenshots", "Emulator smoke test"]
BUILD = REQUIRED[0]
WORKFLOW = "CI"
START, END = "<!-- paquet-verifie:debut -->", "<!-- paquet-verifie:fin -->"
HEADING = "## Ce qui n'est pas vert"


class Unread(Exception):
    pass


def git(root, *args):
    r = subprocess.run(["git", "-C", root, "-c", "core.quotepath=off", *args], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else ""


def ci(root, sha):
    """The ONLY door to GitHub: {"runs": [{id, name, run_number, status, conclusion, jobs: [{id, name, status,
    conclusion, log?}]}]} for the CI runs of this commit. Raises Unread when GitHub cannot be read (gh absent, not
    signed in, an answer that is not JSON)."""
    canned = os.environ.get("PS_PAQUET_CI_JSON")
    if canned:
        with open(canned, encoding="utf-8") as f:
            return json.load(f)
    gh = os.environ.get("PS_PAQUET_GH") or "gh"

    def api(path):
        try:
            r = subprocess.run([gh, "api", path], capture_output=True, text=True, timeout=180)
        except (OSError, subprocess.SubprocessError) as e:
            raise Unread(str(e))
        if r.returncode != 0:
            raise Unread((r.stderr.strip().splitlines() or ["gh a échoué"])[0][:160])
        return r.stdout

    def api_json(path):
        try:
            return json.loads(api(path))
        except ValueError:
            raise Unread("réponse illisible")

    repo = re.sub(r"\.git$", "", re.sub(r".*github\.com[:/]", "", git(root, "remote", "get-url", "origin").strip()))
    if not repo:
        raise Unread("pas de dépôt GitHub (remote origin)")
    runs = []
    for run in api_json(f"repos/{repo}/actions/runs?head_sha={sha}&per_page=100").get("workflow_runs", []):
        if run.get("name") != WORKFLOW:
            continue
        run = {k: run.get(k) for k in ("id", "name", "run_number", "status", "conclusion")}
        run["jobs"] = []
        for job in api_json(f"repos/{repo}/actions/runs/{run['id']}/jobs?per_page=100").get("jobs", []):
            job = {k: job.get(k) for k in ("id", "name", "status", "conclusion")}
            if job["name"] == BUILD and job["status"] == "completed":
                try:
                    job["log"] = api(f"repos/{repo}/actions/jobs/{job['id']}/logs")
                except Unread:
                    pass  # the screenshots are then « non lues »
            run["jobs"].append(job)
        runs.append(run)
    return {"runs": runs}


def state(thing):
    """vert | rouge | en cours | sauté | inconnu, for a run or a job. Only « vert » is green."""
    status, conclusion = thing.get("status"), thing.get("conclusion")
    if status == "completed":
        if conclusion == "success":
            return "vert"
        if conclusion in ("skipped", "neutral"):
            return "sauté"
        return "rouge" if conclusion else "inconnu"
    if status in ("queued", "in_progress", "waiting", "pending", "requested"):
        return "en cours"
    return "inconnu"


def read_ci(root, sha):
    """(the words of the one line, green?, the run or None, detail lines)."""
    try:
        runs = [r for r in ci(root, sha).get("runs", []) if r.get("name") == WORKFLOW]
    except (Unread, OSError, ValueError) as e:
        return "CI non lue", False, None, [f"CI : non lue ({e})"]
    if not runs:
        return "CI absente", False, None, [f"CI : aucun passage de « {WORKFLOW} » pour {sha[:8]}"]
    run = max(runs, key=lambda r: (r.get("run_number") or 0, r.get("id") or 0))
    n = run.get("run_number")
    jobs = {j.get("name"): state(j) for j in run.get("jobs", [])}
    for name in REQUIRED:
        jobs.setdefault(name, "absent")
    red = [name for name, s in jobs.items() if s == "rouge"]
    running = [name for name, s in jobs.items() if s == "en cours"]
    odd = [name for name, s in jobs.items() if s in ("inconnu", "absent") or (s == "sauté" and name in REQUIRED)]
    detail = [f"CI {n} : {name} : {s}" for name, s in jobs.items() if s != "vert"]
    if red:
        return f"CI {n} rouge ({', '.join(red)})", False, run, detail
    if running or state(run) == "en cours":
        return f"CI {n} en cours", False, run, detail or [f"CI {n} : le passage n'est pas fini"]
    if odd:
        return f"CI {n} inconnue ({', '.join(f'{name} : {jobs[name]}' for name in odd)})", False, run, detail
    if state(run) != "vert":
        return f"CI {n} inconnue (passage : {state(run)})", False, run, detail + [f"CI {n} : passage {state(run)}"]
    return f"CI {n} verte", True, run, detail


def read_judge(root, head):
    """(N or None, where it was read or why not)."""
    commit = git(root, "log", "-1", "--format=%H", "--grep=^Portal traces of the merge", head).strip()
    if not commit:
        return None, "aucune trace du portail dans l'histoire de la tête"
    paths = [p for p in git(root, "show", "--name-only", "--format=", commit).splitlines()
             if re.fullmatch(r"docs/audits/ultra/portail/[^/]+/selftest-chantier\.txt", p)]
    if not paths:
        return None, f"la dernière trace du portail ({commit[:8]}) n'a pas de selftest-chantier.txt"
    counts = re.findall(r"^chantier : (\d+) écarts?\b", git(root, "show", f"{commit}:{paths[0]}"), re.M)
    if not counts:
        return None, f"pas de ligne « chantier : N écarts » dans {paths[0]}"
    return int(counts[-1]), paths[0]


def ledger_rows(path):
    rows = []
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            for line in f:
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 4 and parts[0].isdigit() and parts[3].isdigit():
                    rows.append(parts)
    return rows


def read_screenshots(run):
    """(the words, green?)."""
    if run is None:
        return "captures non lues", False
    log = next((j.get("log") or "" for j in run.get("jobs", []) if j.get("name") == BUILD), "")
    found = re.findall(r"Captures : (\d+) au total, (\d+) différentes, (\d+) sans référence", log)
    if not found or int(found[-1][0]) == 0:
        return "captures non lues", False
    stale = int(found[-1][1]) + int(found[-1][2])
    return ("captures à jour", True) if stale == 0 else (f"captures {stale} périmées", False)


def write_verdict(path, block):
    lines = open(path, encoding="utf-8").read().splitlines() if os.path.isfile(path) else []
    if START in lines and END in lines and lines.index(START) < lines.index(END):
        a, b = lines.index(START), lines.index(END)
        lines[a:b + 1] = block
    else:
        at = next((i for i, l in enumerate(lines) if re.match(r"^#+\s+Ce qui n'est pas vert\s*$", l, re.I)), None)
        if at is None:
            lines += ([""] if lines and lines[-1].strip() else []) + [HEADING]
            at = len(lines) - 1
        lines[at + 1:at + 1] = block
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


def main(argv):
    root, args = argv[0], argv[1:]
    open_next, head_arg = False, None
    while args:
        a = args.pop(0)
        if a == "--ouvre":
            open_next = True
        elif a == "--tete" and args:
            head_arg = args.pop(0)
        else:
            sys.stderr.write("usage: scripts/bench/paquet-verifie.sh [--ouvre] [--tete <commit>]\n")
            return 2
    packet_file = os.path.join(root, "scripts", "bench", "portail-paquet.txt")
    ledger_file = os.path.join(root, "scripts", "bench", "portail-paquets.tsv")
    try:
        packet_text = open(packet_file, encoding="utf-8").read()
        n = int(re.search(r"^paquet:[ \t]*(\d+)[ \t]*$", packet_text, re.M).group(1))
    except (OSError, AttributeError):
        sys.stderr.write(f"paquet-verifie : pas de ligne « paquet: N » dans {packet_file}\n")
        return 2
    head = git(root, "rev-parse", "--verify", "-q", (head_arg or "@{u}") + "^{commit}").strip()
    if not head and head_arg is None:
        head = git(root, "rev-parse", "--verify", "-q", "HEAD^{commit}").strip()
        sys.stderr.write("paquet-verifie : cette branche n'a pas d'amont : HEAD pris pour la tête poussée (--tete <commit> pour la nommer)\n")
    if not head:
        sys.stderr.write(f"paquet-verifie : commit inconnu : {head_arg or 'HEAD'}\n")
        return 2
    day = os.environ.get("PS_PAQUET_DATE") or datetime.date.today().isoformat()

    ci_words, ci_green, run, detail = read_ci(root, head)
    count, where = read_judge(root, head)
    before = [r for r in ledger_rows(ledger_file) if int(r[0]) < n]
    previous = int(before[-1][3]) if before else None
    if count is None:
        judge_words, judge_green = "juge non lu", False
        detail.append(f"juge : non lu ({where})")
    elif previous is None:
        judge_words, judge_green = f"juge {count} (avant ?)", True
        detail.append(f"juge : pas de paquet d'avant dans scripts/bench/portail-paquets.tsv : comparaison sautée ({where})")
    else:
        judge_words, judge_green = f"juge {count} (avant {previous})", count <= previous
        if not judge_green:
            detail.append(f"juge : monté de {previous} à {count} ({where})")
    shots_words, shots_green = read_screenshots(run)
    if not shots_green:
        detail.append(f"{shots_words} (travail « {BUILD} »)")
    green = ci_green and judge_green and shots_green
    line = f"paquet {n} : {ci_words} ; {judge_words} ; {shots_words}"

    opened = open_next and green
    if opened:
        after = "ouvert : paquet %d" % (n + 1)
    elif green:
        after = "peut s'ouvrir (scripts/bench/paquet-verifie.sh --ouvre)"
    else:
        after = "fermé tant que ce n'est pas vert"
    block = [START, f"- {day} · {line}", f"  - tête : {head[:8]} · paquet suivant : {after}"]
    block += [f"  - {d}" for d in detail] + [END]
    write_verdict(os.path.join(root, "docs", "audits", "ultra", "EN-COURS.md"), block)

    for d in detail:
        sys.stderr.write(d + "\n")
    if open_next and not green:
        sys.stderr.write(f"REFUSÉ : le paquet {n + 1} ne s'ouvre pas ({packet_file} inchangé) : {line}\n")
    if opened:
        with open(packet_file, "w", encoding="utf-8") as f:
            f.write(re.sub(r"^paquet:[ \t]*\d+[ \t]*$", f"paquet: {n + 1}", packet_text, count=1, flags=re.M))
        new = not os.path.isfile(ledger_file)
        with open(ledger_file, "a", encoding="utf-8") as f:
            if new:
                f.write("# paquet\ttête\tCI\tjuge\tdate\n")
            f.write("\t".join([str(n), head, str(run.get("run_number")), str(count), day]) + "\n")
        sys.stderr.write(f"paquet {n + 1} ouvert ({packet_file}) ; le registre garde le paquet {n}\n")
    print(line)
    return 0 if green else 1


sys.exit(main(sys.argv[1:]))
PY
