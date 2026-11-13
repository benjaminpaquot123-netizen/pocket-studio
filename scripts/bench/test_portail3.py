#!/usr/bin/env python3
"""Tests of the bench lot « portail3 », part B (docs/audits/AUDIT-ULTRA.md §13.9): the end of a batch
(paquet-verifie.sh), the instruction cards (fiche.sh), and what the bench writes itself in a seat's report.

Seconds, no Gradle, no network (toy repositories, a fake gh):  python3 -I scripts/bench/test_portail3.py
"""
import json
import os
import shutil
import stat
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def git(repo, *args):
    return subprocess.run(["git", "-C", repo, "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false", *args],
                          check=True, capture_output=True, text=True).stdout


def script(name, *args, env=None, cwd=None):
    """(exit code, stdout, stderr) of a bench script; a script that is not there yet answers 127 and says nothing, so
    a test goes red on its assertion, never on a crash."""
    path = os.path.join(HERE, name)
    if not os.path.isfile(path):
        return 127, "", f"{name} : absent"
    cmd = ["bash", path] if name.endswith(".sh") else [sys.executable, "-I", path]
    full = dict(os.environ)
    full.update(env or {})
    r = subprocess.run(cmd + list(args), capture_output=True, text=True, env=full, cwd=cwd, timeout=120)
    return r.returncode, r.stdout, r.stderr


# ---------------------------------------------------------------------------------------------------------------------
# 1. paquet-verifie.sh
# ---------------------------------------------------------------------------------------------------------------------

EN_COURS = """# En cours

## Temps
Correction.

## Ce qui n'est pas vert
- une ligne du chef, à garder

## Lots ouverts
- aucun
"""

SMOKE = "Emulator smoke test"
BUILD = "Build, unit tests, screenshots"
FAKE_GH = r'''#!/usr/bin/env python3
"""A gh that answers from a folder: gh api <path>."""
import os, re, sys
d = os.environ["FAKE_GH_DIR"]
with open(os.path.join(d, "appels.txt"), "a") as f:
    f.write(" ".join(sys.argv[1:]) + "\n")
if os.path.isfile(os.path.join(d, "pas-connecte")):
    sys.stderr.write("gh: To get started with GitHub CLI, please run: gh auth login\n")
    sys.exit(4)
if len(sys.argv) < 3 or sys.argv[1] != "api":
    sys.exit(2)
path = sys.argv[2]
if "actions/runs?head_sha=" in path:
    name = "runs-" + re.search(r"head_sha=([0-9a-f]+)", path).group(1) + ".json"
elif re.search(r"actions/runs/(\d+)/jobs", path):
    name = "jobs-" + re.search(r"actions/runs/(\d+)/jobs", path).group(1) + ".json"
elif re.search(r"actions/jobs/(\d+)/logs", path):
    name = "log-" + re.search(r"actions/jobs/(\d+)/logs", path).group(1) + ".txt"
else:
    sys.exit(3)
p = os.path.join(d, name)
if not os.path.isfile(p):
    sys.stderr.write("HTTP 404\n")
    sys.exit(1)
sys.stdout.write(open(p, encoding="utf-8").read())
'''


def captures_log(changed=0, added=0, total=310):
    return ("2026-11-13T10:00:00.0000000Z ##[group]Run python3 -I scripts/screenshot-diff.py\n"
            f"2026-11-13T10:00:01.0000000Z Captures : {total} au total, {changed} différentes, {added} sans référence.\n")


def job(name, status="completed", conclusion="success", **more):
    return dict({"id": sum(ord(c) * (i + 1) for i, c in enumerate(name)), "name": name, "status": status, "conclusion": conclusion}, **more)


def green_jobs():
    return [job("Quick checks (seconds)"), job("Engine tests (Linux)"), job("Separation tests (Linux)"),
            job(BUILD, log=captures_log()), job(SMOKE), job("Publish APK", conclusion="skipped")]


class Batch(unittest.TestCase):
    """§13.6 and §13.9: the next batch opens only on a CI green to the end (emulator included), a judge that did not
    rise and reference screenshots up to date; the verdict is written in EN-COURS.md."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="portail3-")
        self.repo = os.path.join(self.tmp, "depot")
        os.makedirs(self.repo)
        git(self.repo, "init", "-q", "-b", "integration")
        git(self.repo, "remote", "add", "origin", "https://github.com/jouet/depot.git")
        write(self.path("scripts/bench/portail-paquet.txt"), "# Paquet en cours.\npaquet: 2\n")
        write(self.path("docs/audits/ultra/EN-COURS.md"), EN_COURS)
        git(self.repo, "add", ".")
        git(self.repo, "commit", "-q", "-m", "base")
        self.trace("2026-11-12-tour-6-a", "a", 8)
        self.ledger([("1", "aaaaaaa", "400", "8", "2026-11-12")])
        self.ci = {"runs": [{"id": 77, "name": "CI", "run_number": 412, "status": "completed", "conclusion": "success",
                             "jobs": green_jobs()}]}

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def path(self, rel):
        return os.path.join(self.repo, rel)

    def trace(self, folder, zone, count, message=None):
        """A portal's trace commit, as fusion-equipes.sh leaves it."""
        write(self.path(f"docs/audits/ultra/portail/{folder}/selftest-chantier.txt"),
              f"un écart nommé\nchantier : {count} écarts\n")
        git(self.repo, "add", "docs/audits/ultra/portail")
        git(self.repo, "commit", "-q", "-m", message or f"Portal traces of the merge of {zone} (tour-6)")

    def ledger(self, rows):
        write(self.path("scripts/bench/portail-paquets.tsv"),
              "# paquet\ttête\tCI\tjuge\tdate\n" + "".join("\t".join(r) + "\n" for r in rows))

    def head(self):
        return git(self.repo, "rev-parse", "HEAD").strip()

    def run_it(self, *args, env=None):
        ci = os.path.join(self.tmp, "ci.json")
        write(ci, json.dumps(self.ci))
        full = {"PS_PAQUET_RACINE": self.repo, "PS_PAQUET_CI_JSON": ci, "PS_PAQUET_DATE": "2026-11-13"}
        full.update(env or {})
        code, out, err = script("paquet-verifie.sh", "--tete", "HEAD", *args, env=full)
        return code, out.strip(), err

    def set_job(self, name, **fields):
        for j in self.ci["runs"][0]["jobs"]:
            if j["name"] == name:
                j.update(fields)

    def packet(self):
        return read(self.path("scripts/bench/portail-paquet.txt"))

    # -- the one line and the exit code -------------------------------------------------------------------------------

    def test_all_green_says_so_in_one_line_and_exits_0(self):
        code, out, err = self.run_it()
        self.assertEqual(out, "paquet 2 : CI 412 verte ; juge 8 (avant 8) ; captures à jour", err)
        self.assertEqual(code, 0, err)

    def test_a_red_emulator_smoke_test_is_named_and_not_green(self):
        self.set_job(SMOKE, conclusion="failure")
        code, out, err = self.run_it()
        self.assertIn(f"CI 412 rouge ({SMOKE})", out, err)
        self.assertNotEqual(code, 0, "a red emulator smoke test must not exit 0")

    def test_every_red_job_is_named(self):
        self.set_job(SMOKE, conclusion="timed_out")
        self.set_job("Engine tests (Linux)", conclusion="failure")
        code, out, _ = self.run_it()
        self.assertIn("rouge (Engine tests (Linux), Emulator smoke test)", out)
        self.assertNotEqual(code, 0)

    def test_a_job_still_running_is_not_green(self):
        self.set_job(SMOKE, status="in_progress", conclusion=None)
        code, out, _ = self.run_it()
        self.assertIn("CI 412 en cours", out)
        self.assertNotEqual(code, 0, "a job in progress must not exit 0")

    def test_a_run_not_finished_is_not_green_even_with_green_jobs(self):
        self.ci["runs"][0].update(status="in_progress", conclusion=None)
        code, out, _ = self.run_it()
        self.assertIn("CI 412 en cours", out)
        self.assertNotEqual(code, 0)

    def test_an_unknown_state_is_not_green(self):
        self.set_job("Engine tests (Linux)", status="completed", conclusion=None)
        code, out, _ = self.run_it()
        self.assertNotIn("verte", out)
        self.assertNotEqual(code, 0, "a job with no known conclusion must not exit 0")
        self.set_job("Engine tests (Linux)", status="un état que personne ne connaît", conclusion="success")
        code, out, _ = self.run_it()
        self.assertNotIn("verte", out)
        self.assertNotEqual(code, 0, "a job in a state nobody knows must not exit 0")

    def test_no_emulator_job_at_all_is_not_green(self):
        self.ci["runs"][0]["jobs"] = [j for j in green_jobs() if j["name"] != SMOKE]
        code, out, err = self.run_it()
        self.assertNotIn("verte", out)
        self.assertIn(SMOKE, out + err)
        self.assertNotEqual(code, 0, "a CI without its emulator smoke test must not exit 0")

    def test_a_skipped_emulator_job_is_not_green(self):
        self.set_job(SMOKE, conclusion="skipped")
        code, out, _ = self.run_it()
        self.assertNotIn("verte", out)
        self.assertNotEqual(code, 0)

    def test_no_ci_run_for_the_head_is_not_green(self):
        self.ci = {"runs": []}
        code, out, _ = self.run_it()
        self.assertIn("CI absente", out)
        self.assertNotEqual(code, 0)

    def test_the_latest_ci_run_of_the_head_is_the_one_read(self):
        old = {"id": 70, "name": "CI", "run_number": 409, "status": "completed", "conclusion": "failure",
               "jobs": [job(SMOKE, conclusion="failure"), job(BUILD, log=captures_log())]}
        other = {"id": 90, "name": "Nightly real use", "run_number": 999, "status": "completed", "conclusion": "failure", "jobs": []}
        self.ci["runs"] = [old, other] + self.ci["runs"]
        code, out, err = self.run_it()
        self.assertIn("CI 412 verte", out, err)
        self.assertEqual(code, 0, err)

    # -- the judge ----------------------------------------------------------------------------------------------------

    def test_a_judge_that_rose_is_not_green(self):
        self.trace("2026-11-13-tour-6-b", "b", 12)
        code, out, _ = self.run_it()
        self.assertIn("juge 12 (avant 8)", out)
        self.assertNotEqual(code, 0, "a judge that rose must not exit 0")

    def test_a_judge_that_went_down_is_green(self):
        self.trace("2026-11-13-tour-6-b", "b", 5)
        code, out, err = self.run_it()
        self.assertIn("juge 5 (avant 8)", out)
        self.assertEqual(code, 0, err)

    def test_the_latest_trace_is_the_latest_in_git_history_not_by_folder_name(self):
        # The folder « 2026-11-10-… » sorts before the first one but is committed after it: it is the latest trace.
        self.trace("2026-11-10-tour-5-z", "z", 30)
        code, out, _ = self.run_it()
        self.assertIn("juge 30 (avant 8)", out)
        self.assertNotEqual(code, 0)

    def test_a_chantier_file_outside_a_portal_trace_commit_is_not_the_judge(self):
        self.trace("2026-11-13-tour-6-b", "b", 99, message="notes of a team")
        code, out, err = self.run_it()
        self.assertIn("juge 8 (avant 8)", out, err)
        self.assertEqual(code, 0, err)

    def test_no_trace_at_all_is_not_green(self):
        shutil.rmtree(self.path("docs/audits/ultra/portail"))
        git(self.repo, "add", "-A", "docs/audits/ultra")
        git(self.repo, "commit", "-q", "-m", "no trace any more")
        git(self.repo, "checkout", "-q", "--orphan", "vide")
        git(self.repo, "commit", "-q", "-m", "a history with no portal trace")
        code, out, _ = self.run_it()
        self.assertIn("juge non lu", out)
        self.assertNotEqual(code, 0)

    def test_without_a_ledger_line_the_comparison_is_skipped_and_said(self):
        os.remove(self.path("scripts/bench/portail-paquets.tsv"))
        code, out, err = self.run_it()
        self.assertIn("juge 8 (avant ?)", out, err)
        self.assertIn("comparaison sautée", err)
        self.assertEqual(code, 0, err)

    def test_the_count_before_is_the_previous_batch_s_not_this_batch_s(self):
        self.ledger([("1", "aaaaaaa", "400", "8", "2026-11-12"), ("2", "bbbbbbb", "411", "3", "2026-11-13")])
        code, out, err = self.run_it()
        self.assertIn("juge 8 (avant 8)", out, err)

    # -- the reference screenshots ------------------------------------------------------------------------------------

    def test_stale_screenshots_are_counted_and_not_green(self):
        self.set_job(BUILD, log=captures_log(changed=3, added=1))
        code, out, _ = self.run_it()
        self.assertIn("captures 4 périmées", out)
        self.assertNotEqual(code, 0, "stale reference screenshots must not exit 0")

    def test_screenshots_that_cannot_be_read_are_not_green(self):
        self.set_job(BUILD, log="2026-11-13T10:00:00Z nothing about the screenshots here\n")
        code, out, _ = self.run_it()
        self.assertIn("captures non lues", out)
        self.assertNotEqual(code, 0, "screenshots not read must not exit 0")
        self.set_job(BUILD, log=captures_log(total=0))
        code, out, _ = self.run_it()
        self.assertIn("captures non lues", out, "a comparison of 0 screenshot compared nothing")
        self.assertNotEqual(code, 0)

    # -- GitHub, through the one door ---------------------------------------------------------------------------------

    def fake_gh(self, signed_in=True):
        d = os.path.join(self.tmp, "gh")
        os.makedirs(d, exist_ok=True)
        gh = os.path.join(d, "gh")
        write(gh, FAKE_GH)
        os.chmod(gh, os.stat(gh).st_mode | stat.S_IXUSR)
        run = self.ci["runs"][0]
        jobs = []
        for j in run["jobs"]:
            j = dict(j)
            log = j.pop("log", None)
            if log is not None:
                write(os.path.join(d, f"log-{j['id']}.txt"), log)
            jobs.append(j)
        write(os.path.join(d, f"runs-{self.head()}.json"),
              json.dumps({"workflow_runs": [{k: v for k, v in run.items() if k != "jobs"}]}))
        write(os.path.join(d, f"jobs-{run['id']}.json"), json.dumps({"jobs": jobs}))
        if not signed_in:
            write(os.path.join(d, "pas-connecte"), "")
        return {"PS_PAQUET_GH": gh, "FAKE_GH_DIR": d, "PS_PAQUET_CI_JSON": ""}

    def test_the_ci_is_read_from_gh_job_by_job(self):
        env = self.fake_gh()
        code, out, err = self.run_it(env=env)
        self.assertEqual(out, "paquet 2 : CI 412 verte ; juge 8 (avant 8) ; captures à jour", err)
        self.assertEqual(code, 0, err)
        calls = read(os.path.join(env["FAKE_GH_DIR"], "appels.txt"))
        self.assertIn(f"repos/jouet/depot/actions/runs?head_sha={self.head()}", calls)
        self.assertIn("actions/runs/77/jobs", calls)

    def test_a_red_job_read_from_gh_is_red(self):
        self.set_job(SMOKE, conclusion="failure")
        code, out, _ = self.run_it(env=self.fake_gh())
        self.assertIn(f"CI 412 rouge ({SMOKE})", out)
        self.assertNotEqual(code, 0)

    def test_gh_not_signed_in_is_ci_not_read_and_not_green(self):
        code, out, _ = self.run_it(env=self.fake_gh(signed_in=False))
        self.assertIn("CI non lue", out)
        self.assertIn("captures non lues", out)
        self.assertNotEqual(code, 0, "a CI that was not read must not exit 0")

    def test_no_gh_at_all_is_ci_not_read(self):
        code, out, _ = self.run_it(env={"PS_PAQUET_GH": os.path.join(self.tmp, "pas-de-gh"), "PS_PAQUET_CI_JSON": ""})
        self.assertIn("CI non lue", out)
        self.assertNotEqual(code, 0)

    def test_a_job_log_gh_cannot_give_is_screenshots_not_read(self):
        env = self.fake_gh()
        for n in os.listdir(env["FAKE_GH_DIR"]):
            if n.startswith("log-"):
                os.remove(os.path.join(env["FAKE_GH_DIR"], n))
        code, out, _ = self.run_it(env=env)
        self.assertIn("CI 412 verte ; juge 8 (avant 8) ; captures non lues", out)
        self.assertNotEqual(code, 0)

    # -- the verdict in EN-COURS.md -----------------------------------------------------------------------------------

    def test_the_verdict_is_written_under_its_heading_in_a_replaceable_block(self):
        self.set_job(SMOKE, conclusion="failure")
        _, out, err = self.run_it()
        text = read(self.path("docs/audits/ultra/EN-COURS.md"))
        lines = text.splitlines()
        self.assertIn("<!-- paquet-verifie:debut -->", lines, err)
        start, end = lines.index("<!-- paquet-verifie:debut -->"), lines.index("<!-- paquet-verifie:fin -->")
        self.assertEqual(lines[start - 1], "## Ce qui n'est pas vert", "the block sits right under its heading")
        block = "\n".join(lines[start:end])
        self.assertIn("2026-11-13", block)
        self.assertIn(out, block)
        self.assertIn(self.head()[:8], block)
        self.assertIn("- une ligne du chef, à garder", lines[end + 1:], "the chief's own lines are kept")
        self.assertIn("## Lots ouverts", lines)

    def test_the_block_is_replaced_not_piled_up(self):
        self.set_job(SMOKE, conclusion="failure")
        self.run_it()
        first = read(self.path("docs/audits/ultra/EN-COURS.md"))
        self.run_it()
        self.assertEqual(read(self.path("docs/audits/ultra/EN-COURS.md")), first, "twice the same verdict: the same file")
        self.set_job(SMOKE, conclusion="success")
        _, out, _ = self.run_it()
        text = read(self.path("docs/audits/ultra/EN-COURS.md"))
        self.assertEqual(text.count("<!-- paquet-verifie:debut -->"), 1)
        self.assertEqual(text.count("<!-- paquet-verifie:fin -->"), 1)
        self.assertIn(out, text)
        self.assertNotIn("rouge", text)

    def test_the_heading_is_created_when_absent(self):
        write(self.path("docs/audits/ultra/EN-COURS.md"), "# En cours\n\n## Lots ouverts\n- aucun\n")
        self.run_it()
        text = read(self.path("docs/audits/ultra/EN-COURS.md"))
        self.assertIn("## Ce qui n'est pas vert\n<!-- paquet-verifie:debut -->", text)
        self.assertIn("## Lots ouverts\n- aucun\n", text)

    def test_it_never_commits(self):
        before = self.head()
        self.run_it("--ouvre")
        self.assertEqual(self.head(), before)
        changed = git(self.repo, "status", "--porcelain")
        self.assertIn("docs/audits/ultra/EN-COURS.md", changed, "the verdict is left in the working tree, for the chief to commit")

    # -- opening the next batch ---------------------------------------------------------------------------------------

    def test_ouvre_on_green_opens_the_next_batch_and_keeps_the_ledger(self):
        code, out, err = self.run_it("--ouvre")
        self.assertEqual(code, 0, err)
        self.assertEqual(self.packet(), "# Paquet en cours.\npaquet: 3\n")
        last = read(self.path("scripts/bench/portail-paquets.tsv")).splitlines()[-1].split("\t")
        self.assertEqual(last, ["2", self.head(), "412", "8", "2026-11-13"])
        self.assertIn("paquet 3", read(self.path("docs/audits/ultra/EN-COURS.md")))

    def test_ouvre_refuses_when_not_green_and_touches_nothing(self):
        ledger = read(self.path("scripts/bench/portail-paquets.tsv"))
        for breakage in ("smoke", "judge", "screenshots", "running"):
            with self.subTest(breakage):
                self.setUp_ci()
                if breakage == "smoke":
                    self.set_job(SMOKE, conclusion="failure")
                elif breakage == "judge":
                    self.ledger([("1", "aaaaaaa", "400", "2", "2026-11-12")])
                    ledger = read(self.path("scripts/bench/portail-paquets.tsv"))
                elif breakage == "screenshots":
                    self.set_job(BUILD, log=captures_log(changed=2))
                else:
                    self.set_job(BUILD, status="queued", conclusion=None)
                code, out, err = self.run_it("--ouvre")
                self.assertNotEqual(code, 0)
                self.assertIn("REFUSÉ", err)
                self.assertEqual(self.packet(), "# Paquet en cours.\npaquet: 2\n", "the batch number must not move")
                self.assertEqual(read(self.path("scripts/bench/portail-paquets.tsv")), ledger, "the ledger must not move")

    def setUp_ci(self):
        self.ci = {"runs": [{"id": 77, "name": "CI", "run_number": 412, "status": "completed", "conclusion": "success",
                             "jobs": green_jobs()}]}

    def test_without_ouvre_a_green_batch_opens_nothing(self):
        ledger = read(self.path("scripts/bench/portail-paquets.tsv"))
        code, _, err = self.run_it()
        self.assertEqual(code, 0, err)
        self.assertEqual(self.packet(), "# Paquet en cours.\npaquet: 2\n")
        self.assertEqual(read(self.path("scripts/bench/portail-paquets.tsv")), ledger)

    def test_the_real_ledger_and_script_are_where_the_chief_looks(self):
        self.assertTrue(os.path.isfile(os.path.join(HERE, "paquet-verifie.sh")), "scripts/bench/paquet-verifie.sh is missing")
        self.assertTrue(os.path.isfile(os.path.join(HERE, "portail-paquets.tsv")), "scripts/bench/portail-paquets.tsv is missing")


# ---------------------------------------------------------------------------------------------------------------------
# 2. fiche.sh
# ---------------------------------------------------------------------------------------------------------------------

AIDE = os.path.join(ROOT, "docs", "AIDE-FUSIONS-TESTS.md")
SEAT_KEYS = ["NOM=Lancelot", "TOUR=tour-7", "LOT=B", "SIEGE=c", "SHA=0123abcd", "BRANCHE=ccr-jouet", "DOSSIER_DU_LOT=/tmp/lotB",
             "BRIEF=briefs/Bc.txt", "CONSIGNES_TESTEUR_MD_DU_TOUR=tour-7/consignes-testeur.md", "GESTES=300", "MINUTES=120",
             "LISTE=t6-12, t6-14"]
FIX_KEYS = ["NOM=Perceval", "TOUR=tour-7", "ZONE=pads", "IDS=t6-12", "LOTS_CORRECTION_MD_SECTION_DU_LOT=lots-correction.md §2",
            "VERDICTS_TSV=verdicts.tsv", "LA_LECTURE=lecture-recurrents.md", "LISTE=PadGrid.kt"]
TOY_GABARIT = """# Fiches jouet

## 1. Un siège

> Chef → <NOM>. Une phrase que le script ne peut pas connaître : <valeur neuve>.
> Réponds « <NOM> fini : <branche> <sha> ».

Un paragraphe hors fiche, jamais imprimé.

## 2. Un lot

> Lot <zone> pour <NOM>.

## 3. Un lot du banc

> - Ligne en plus : `from_paquet <numéro du paquet suivant>`.
"""


def card(*args, env=None):
    return script("fiche.sh", *args, env=env)


def variables(n, env=None):
    """{key: (placeholder, what is said of it)} from fiche.sh --variables <n>."""
    code, out, err = card("--variables", str(n), env=env)
    rows = [l.split("\t") for l in out.splitlines() if "\t" in l]
    return code, {r[0]: (r[1], r[2] if len(r) > 2 else "") for r in rows}, err


class Cards(unittest.TestCase):
    """§13.4: an instruction is copied from the template, only its variables change; fiche.sh prints it filled."""

    def test_a_seat_card_is_printed_with_its_variables_filled(self):
        code, out, err = card("1", "CHOIX=pilote", *SEAT_KEYS)
        self.assertEqual(code, 0, err)
        self.assertIn("Chef d'orchestre → Lancelot, rôle TESTEUR. Siège tour-7 Bc (pilote).", out)
        self.assertIn("`git rev-parse HEAD` rend bien `0123abcd`", out)
        self.assertIn("`git fetch origin ccr-jouet`", out)
        self.assertIn("« code testé : 0123abcd »", out, "the commit to judge is written in the report's heading line too")
        self.assertIn("Budget : 300 gestes, délai 120 min", out)
        self.assertFalse([l for l in out.splitlines() if l.startswith(">")], "printed ready to paste, without the quote marks")

    def test_the_recipient_s_own_branch_and_commit_stay_his_to_fill(self):
        code, out, err = card("1", "CHOIX=vague", *SEAT_KEYS)
        self.assertEqual(code, 0, err)
        reply = [l for l in out.splitlines() if "fini siège" in l]
        self.assertEqual(len(reply), 1, out)
        self.assertIn("« Lancelot fini siège Bc : <branche> <sha>, code testé 0123abcd,", reply[0])

    def test_a_placeholder_left_unfilled_fails_and_is_listed(self):
        keys = [k for k in SEAT_KEYS if not k.startswith(("SHA=", "GESTES="))]
        code, out, err = card("1", "CHOIX=vague", *keys)
        self.assertNotEqual(code, 0, "a card with a variable left empty must fail")
        self.assertEqual(out, "", "nothing to paste when a variable is missing")
        self.assertIn("SHA", err)
        self.assertIn("<sha>", err)
        self.assertIn("GESTES", err)
        self.assertNotIn("MINUTES", err, "only what is missing is listed")

    def test_an_empty_value_is_a_missing_one(self):
        code, _, err = card("1", "CHOIX=vague", *[k if not k.startswith("SHA=") else "SHA=" for k in SEAT_KEYS])
        self.assertNotEqual(code, 0)
        self.assertIn("SHA", err)

    def test_brouillon_prints_with_the_holes(self):
        code, out, err = card("--brouillon", "1", "NOM=Lancelot")
        self.assertEqual(code, 0, err)
        self.assertIn("→ Lancelot, rôle TESTEUR", out)
        self.assertIn("<sha>", out)
        self.assertIn("<vague | pilote | contrôle du lot <zone>>", out)

    def test_a_choice_needs_its_key_and_one_of_its_values(self):
        code, out, err = card("1", *SEAT_KEYS)
        self.assertNotEqual(code, 0, "the choice « vague | pilote | contrôle » left open must fail")
        self.assertIn("CHOIX", err)
        code, out, err = card("1", "CHOIX=essai", *SEAT_KEYS)
        self.assertNotEqual(code, 0, "a value that is none of the alternatives must fail")
        self.assertIn("pilote", err)
        self.assertEqual(out, "")

    def test_an_alternative_with_its_own_variable_asks_for_it(self):
        code, out, err = card("1", "CHOIX=contrôle", *SEAT_KEYS)
        self.assertNotEqual(code, 0, "« contrôle du lot <zone> » needs ZONE")
        self.assertIn("ZONE", err)
        code, out, err = card("1", "CHOIX=controle", "ZONE=pads", *SEAT_KEYS)
        self.assertEqual(code, 0, err)
        self.assertIn("Siège tour-7 Bc (contrôle du lot pads).", out)
        code, out, err = card("1", "CHOIX=vague", *SEAT_KEYS)
        self.assertEqual(code, 0, "« vague » needs no ZONE: " + err)
        self.assertIn("Siège tour-7 Bc (vague).", out)

    def test_variables_lists_the_keys_of_a_card(self):
        code, keys, err = variables(1)
        self.assertEqual(code, 0, err)
        for k in [k.split("=")[0] for k in SEAT_KEYS] + ["CHOIX", "ZONE"]:
            self.assertIn(k, keys, f"--variables 1 must list {k}")
        self.assertEqual(keys["SIEGE"][0], "<siège>")
        self.assertIn("vague", keys["CHOIX"][0])

    def test_an_unknown_key_is_refused(self):
        code, out, err = card("1", "CHOIX=vague", "SHAA=1", *SEAT_KEYS)
        self.assertNotEqual(code, 0, "a key that is no variable of the card is a typo")
        self.assertIn("SHAA", err)
        self.assertEqual(out, "")

    def test_an_unknown_card_is_refused(self):
        code, out, _ = card("8")
        self.assertNotEqual(code, 0)
        self.assertEqual(out, "")
        self.assertNotEqual(card()[0], 0)

    def test_a_correction_card_keeps_the_templates_the_team_fills_itself(self):
        code, out, err = card("2", *FIX_KEYS)
        self.assertEqual(code, 0, err)
        self.assertIn("rôle CORRECTEUR. Lot tour-7 pads : défauts t6-12.", out)
        self.assertIn("`docs/decisions/tour-7-pads.md`", out)
        self.assertIn("`scripts/bench/pre-vol.sh --avant-relecture tour-7 pads`", out)
        self.assertIn("« rouge: <sujet> »", out)
        self.assertIn("« Demande : <fichier> : <pourquoi> »", out)
        self.assertIn("`scripts/bench/rejeu-nouveau.sh <dossier du siège> <n> <id> 'attendu=…'`", out)
        self.assertIn("« Perceval fini lot pads : <branche> <sha>,", out)

    def test_the_bench_card_is_the_correction_card_plus_its_own_lines(self):
        _, fix, _ = card("2", *FIX_KEYS)
        code, out, err = card("3", "NUMERO_DU_PAQUET_SUIVANT=7", *FIX_KEYS)
        self.assertEqual(code, 0, err)
        self.assertTrue(fix.strip() and out.startswith(fix.rstrip("\n")), "fiche 3 starts with fiche 2, word for word")
        extra = out[len(fix.rstrip("\n")):]
        self.assertIn("`from_paquet 7`", extra)
        self.assertIn("jamais `app/src/main`", extra)

    def test_the_next_batch_number_is_read_from_the_portal_when_not_given(self):
        current = int(read(os.path.join(HERE, "portail-paquet.txt")).split("paquet:")[1].split()[0])
        code, out, err = card("3", *FIX_KEYS)
        self.assertEqual(code, 0, err)
        self.assertIn(f"`from_paquet {current + 1}`", out)

    def test_the_reviewer_s_card_is_the_one_of_the_help_plus_its_own_lines(self):
        code, out, err = card("6", "TOUR_N=tour-7", "ZONE=pads")
        self.assertEqual(code, 0, err)
        help_text = read(AIDE).split("## La fiche du relecteur\n", 1)[1].split("\n## ", 1)[0].strip().splitlines()
        self.assertGreaterEqual(len(help_text), 5)
        for line in help_text:
            self.assertIn(line.replace("<tour-N>", "tour-7").replace("<zone>", "pads"), out)
        self.assertIn("`docs/audits/ultra/tour-7-pads-relecture.md`", out)
        self.assertIn("« Relu : <sha du commit que tu as lu> »", out, "what the reviewer fills stays a template")
        self.assertIn("Est GRAVE, quel que soit ton premier mot", out)
        self.assertLess(out.index("Dernière ligne, seule"), out.index("Est GRAVE"), "the help's card first, then the extra lines")

    def test_the_refusal_card_takes_the_portal_s_line_though_it_sits_in_quotes(self):
        keys = ["NOM=Karadoc", "ZONE=pads", "CODE=4", "CE_QUE_LA_LIGNE_DEMANDE=ajoute le rejeu"]
        code, out, err = card("7", *keys)
        self.assertNotEqual(code, 0, "the portal's exact line is the chief's to give")
        self.assertIn("LIGNE", err)
        code, out, err = card("7", "LIGNE=not merged: no replay", *keys)
        self.assertEqual(code, 0, err)
        self.assertIn("le portail refuse pads (sortie 4). Sa ligne exacte : « not merged: no replay ».", out)
        self.assertIn("« Karadoc fini pads v<n> : <sha>, cause : … »", out)

    def test_reading_and_sorting_are_two_cards_of_number_4(self):
        code, out, err = card("--brouillon", "4")
        self.assertEqual(code, 0, err)
        self.assertIn("rôle LECTEUR", out)
        self.assertIn("rôle TRIEUR", out)
        code, out, err = card("--brouillon", "4b")
        self.assertEqual(code, 0, err)
        self.assertNotIn("rôle LECTEUR", out)
        self.assertIn("rôle TRIEUR", out)

    def test_every_card_of_the_real_template_can_be_listed_and_filled(self):
        for n in range(8):
            with self.subTest(fiche=n):
                code, keys, err = variables(n)
                self.assertEqual(code, 0, err)
                given = []
                for k, (placeholder, said) in keys.items():
                    if k == "CHOIX":
                        given.append("CHOIX=" + placeholder.strip("<>").split(" | ")[0])
                    elif said.startswith("à remplir"):
                        given.append(f"{k}=x{k.lower()}x")
                code, out, err = card(str(n), *given)
                self.assertEqual(code, 0, f"fiche {n} with every « à remplir » key given: {err}")
                self.assertGreater(len(out.splitlines()), 0)
                for k, (placeholder, said) in keys.items():
                    if said.startswith("à remplir") and k != "CHOIX":
                        self.assertIn(f"x{k.lower()}x", out, f"fiche {n}: {k} given but not printed")
                if n == 0:
                    self.assertIn("Tu fais partie de l'équipe", out)

    def test_the_text_is_read_from_the_template_at_run_time(self):
        toy = os.path.join(tempfile.mkdtemp(prefix="portail3-fiche-"), "gabarit.md")
        write(toy, TOY_GABARIT)
        try:
            code, out, err = card("1", "NOM=Bohort", "VALEUR_NEUVE=42", env={"PS_FICHE_GABARIT": toy})
            self.assertEqual(code, 0, err)
            self.assertEqual(out, "Chef → Bohort. Une phrase que le script ne peut pas connaître : 42.\n"
                                  "Réponds « Bohort fini : <branche> <sha> ».\n")
            code, out, err = card("3", "NOM=Bohort", "ZONE=z", "NUMERO_DU_PAQUET_SUIVANT=9", env={"PS_FICHE_GABARIT": toy})
            self.assertEqual(out, "Lot z pour Bohort.\n- Ligne en plus : `from_paquet 9`.\n", err)
            code, out, err = card("5", env={"PS_FICHE_GABARIT": toy})
            self.assertNotEqual(code, 0, "a card the template does not hold is refused")
        finally:
            shutil.rmtree(os.path.dirname(toy), ignore_errors=True)
        path = os.path.join(HERE, "fiche.sh")
        source = read(path) if os.path.isfile(path) else ""
        self.assertTrue(source, "scripts/bench/fiche.sh is missing")
        for sentence in ("Tu ne suis pas la tête", "Aucune question à Benjamin", "Tu fais partie de l'équipe", "rôle TESTEUR"):
            self.assertNotIn(sentence, source, "the cards' text lives in the template, never in the script")


# ---------------------------------------------------------------------------------------------------------------------
# 3. « code testé » and « joué par » written by the bench; no etat/ of a seat in git
# ---------------------------------------------------------------------------------------------------------------------

TESTER_REPORT = ("siege: a | profil: t9-Aa curieux | lang: fr | device: phone360 | font: 100 | mode: simple\n"
                 "mission: simple | buts: son, partie, jamais-pro\n"
                 "defauts:\n  - geste: 4 | ecran.vu: 5 | cible: « Jouer » | geste.type: touche\n")


class SeatReport(unittest.TestCase):
    """§13.1 and §13.4: a seat's report starts with the commit it judged and who played it. The bench writes both
    when it STARTS, and its exit check refuses a report that says another commit, or none."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="portail3-siege-")
        self.depot = os.path.join(self.tmp, "depot")
        self.lot = os.path.join(self.depot, "docs", "audits", "ultra", "sieges", "tour-9", "lotA")
        os.makedirs(self.depot)
        git(self.depot, "init", "-q")
        write(os.path.join(self.depot, "app.txt"), "v1\n")
        git(self.depot, "add", "app.txt")
        git(self.depot, "commit", "-q", "-m", "the code a seat plays")
        self.start_sha = git(self.depot, "rev-parse", "HEAD").strip()
        write(os.path.join(self.lot, "a", "start.txt"), "lang=fr\nmission=simple\n")
        self.report = os.path.join(self.lot, "a", "rapport.md")

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def step(self, name, env=None):
        clean = {"PS_JOUE_PAR": ""}
        clean.update(env or {})
        code, out, err = script("sortie-de-siege.py", name, self.lot, "--depot", self.depot, env=clean)
        return code, out + err

    def head_lines(self):
        return read(self.report).splitlines()[:2] if os.path.isfile(self.report) else []

    def new_commit(self):
        write(os.path.join(self.depot, "app.txt"), "v2\n")
        git(self.depot, "commit", "-q", "-am", "a correction merged while the seat plays")
        return git(self.depot, "rev-parse", "HEAD").strip()

    def test_the_bench_writes_both_lines_when_it_starts(self):
        code, said = self.step("avant")
        self.assertEqual(code, 0, said)
        self.assertEqual(self.head_lines(), [f"code testé : {self.start_sha}", "joué par : sous-agent neuf"], said)

    def test_only_the_seats_of_the_lot_get_a_report(self):
        os.makedirs(os.path.join(self.lot, "b"))
        os.makedirs(os.path.join(self.lot, "briefs"))
        self.step("avant")
        self.assertFalse(os.path.exists(os.path.join(self.lot, "b", "rapport.md")), "no start.txt, no seat")
        self.assertFalse(os.path.exists(os.path.join(self.lot, "briefs", "rapport.md")))

    def test_who_played_comes_from_the_seat_or_the_environment(self):
        self.step("avant", env={"PS_JOUE_PAR": "Benjamin, sur son téléphone"})
        self.assertEqual(self.head_lines()[1:], ["joué par : Benjamin, sur son téléphone"])
        os.remove(self.report)
        write(os.path.join(self.lot, "a", "start.txt"), "lang=fr\njoue.par=le chef, à la main\n")
        self.step("avant", env={"PS_JOUE_PAR": "quelqu'un d'autre"})
        self.assertEqual(self.head_lines()[1:], ["joué par : le chef, à la main"], "the seat's own start.txt says it best")

    def test_the_commit_is_the_one_at_the_start_not_at_the_end(self):
        self.step("avant")
        later = self.new_commit()
        code, said = self.step("apres")
        self.assertEqual(self.head_lines()[:1], [f"code testé : {self.start_sha}"], "the head moved while the seat played: " + said)
        self.assertNotIn(later, read(self.report))
        self.assertEqual(self.step("verifie")[0], 0, "the report names the commit of the start: accepted")

    def test_what_the_tester_wrote_is_kept(self):
        self.step("avant")
        write(self.report, TESTER_REPORT)  # the tester wrote his whole file over the bench's two lines
        code, said = self.step("apres")
        text = read(self.report)
        self.assertEqual(text.splitlines()[:2], [f"code testé : {self.start_sha}", "joué par : sous-agent neuf"], said)
        self.assertTrue(text.endswith(TESTER_REPORT), "the tester's report, byte for byte, under the two lines")
        self.assertEqual(text.count("code testé :"), 1)
        self.step("apres")
        self.step("entete")
        self.assertEqual(read(self.report), text, "written once, whatever the number of runs")

    def test_a_tester_s_own_lines_are_not_rewritten(self):
        self.step("avant")
        write(self.report, f"code testé : {self.start_sha[:10]}\njoué par : sous-agent neuf, relancé une fois\n" + TESTER_REPORT)
        before = read(self.report)
        self.step("apres")
        self.assertEqual(read(self.report), before)
        self.assertEqual(self.step("verifie")[0], 0, "a short form of the same commit is the same commit")

    def test_a_report_that_names_another_commit_is_refused(self):
        self.step("avant")
        later = self.new_commit()
        write(self.report, f"code testé : {later}\njoué par : sous-agent neuf\n" + TESTER_REPORT)
        code, said = self.step("verifie")
        self.assertNotEqual(code, 0, "the seat says it judged a commit the bench did not start on")
        self.assertIn("lotA/a", said)
        self.assertIn(self.start_sha[:8], said)
        self.assertIn(later[:8], said)
        code, said = self.step("apres")
        self.assertNotEqual(code, 0, "the exit check itself refuses it")
        self.assertEqual(read(self.report).splitlines()[0], f"code testé : {later}", "refused, never silently rewritten")
        with open(os.path.join(self.lot, "lot.json"), encoding="utf-8") as f:
            self.assertEqual([r.split(" : ")[0] for r in json.load(f)["rapports_refuses"]], ["lotA/a"])

    def test_a_report_without_its_commit_is_refused(self):
        self.step("avant")
        self.step("apres")
        write(self.report, TESTER_REPORT)  # rewritten after the bench ended
        code, said = self.step("verifie")
        self.assertNotEqual(code, 0, "« code testé » is missing")
        self.assertIn("code testé", said)
        self.assertEqual(read(self.report), TESTER_REPORT, "the check reads, it does not write")
        write(self.report, "joué par : sous-agent neuf\ncode testé :\n" + TESTER_REPORT)
        self.assertNotEqual(self.step("verifie")[0], 0, "an empty « code testé » names no commit")
        write(self.report, f"code testé : {self.start_sha[:4]}\n" + TESTER_REPORT)
        self.assertNotEqual(self.step("verifie")[0], 0, "four characters name no commit")
        write(self.report, TESTER_REPORT + f"\ncode testé : {self.start_sha}\n")
        self.assertNotEqual(self.step("verifie")[0], 0, "the line is the report's heading, not a line lost under the cards")

    def test_a_missing_report_is_refused(self):
        self.step("avant")
        if os.path.isfile(self.report):
            os.remove(self.report)
        code, said = self.step("verifie")
        self.assertNotEqual(code, 0)
        self.assertIn("lotA/a", said)

    def test_a_report_the_bench_never_started_is_refused(self):
        write(self.report, f"code testé : {self.start_sha}\njoué par : sous-agent neuf\n" + TESTER_REPORT)
        code, said = self.step("verifie")
        self.assertNotEqual(code, 0, "nothing recorded at the start: the bench cannot vouch for that commit")
        self.assertIn("lotA/a", said)

    def test_a_good_lot_passes_and_says_so(self):
        self.step("avant")
        with open(self.report, "a", encoding="utf-8") as f:
            f.write(TESTER_REPORT)
        code, said = self.step("apres")
        self.assertEqual(code, 0, said)
        code, said = self.step("verifie")
        self.assertEqual(code, 0, said)
        self.assertIn(self.start_sha[:8], said)

    def test_the_lot_s_own_files_are_not_foreign_files(self):
        self.step("avant")
        code, said = self.step("apres")
        self.assertIn("0 fichier(s) hors des sièges", said, "what the bench writes at the start is no foreign file")

    def test_the_bench_calls_the_record_before_the_seats_play(self):
        tester = read(os.path.join(HERE, "..", "human-tester.sh"))
        self.assertTrue(0 < tester.find("sortie-de-siege.py avant") < tester.find(":app:testDebugUnitTest"),
                        "human-tester.sh records the commit before it starts the seats")
        self.assertTrue("sortie-de-siege.py apres" in tester)

    def test_the_tester_is_told_to_keep_the_two_lines(self):
        tour = os.path.join(self.tmp, "tour-9")
        write(os.path.join(tour, "sieges-proposes.tsv"), "A\ta\tfr\tphone360\t100\tsimple\t-\tsimple\t110\tcurieux\tlibre: un son joue\n")
        code, out, err = script("fabrique-sieges.py", tour)
        self.assertEqual(code, 0, err)
        brief = read(os.path.join(tour, "briefs", "Aa.txt"))
        self.assertTrue("code testé" in brief and "joué par" in brief, "the brief says the report already starts with the bench's two lines")

    def test_the_end_of_a_seat_reminds_the_two_lines(self):
        seat = os.path.join(self.lot, "a")
        self.step("avant")
        write(os.path.join(seat, "turn"), "1\n")
        write(os.path.join(seat, "screen-1.txt"), "Jouer @ 10,10\n")
        act = os.path.join(HERE, "..", "human-act.py")
        r = subprocess.run([sys.executable, "-I", act, seat, "fin"], capture_output=True, text=True, timeout=60)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertTrue(f"code testé : {self.start_sha}" in r.stdout and "rapport.md" in r.stdout,
                        "« fin » says where the report is and the two lines to keep:\n" + r.stdout)


class SeatStateOutOfGit(unittest.TestCase):
    """§13.4: a seat folder does not keep etat/ in git (286 MB to sort after round 6)."""

    def ignored(self, *paths):
        tmp = tempfile.mkdtemp(prefix="portail3-ignore-")
        try:
            git(tmp, "init", "-q")
            shutil.copy(os.path.join(ROOT, ".gitignore"), os.path.join(tmp, ".gitignore"))
            out = {}
            for p in paths:
                write(os.path.join(tmp, p), "x")
                r = subprocess.run(["git", "-C", tmp, "check-ignore", "-q", p], capture_output=True)
                out[p] = r.returncode == 0
            return out
        finally:
            shutil.rmtree(tmp, ignore_errors=True)

    def test_a_seat_s_state_folder_is_ignored(self):
        seats = "docs/audits/ultra/sieges/"
        got = self.ignored(seats + "tour-7/lotA/a/etat/resume.json", seats + "tour-7/lotB/f/etat/projects/p1/project.json",
                           seats + "tour-7/controle/Bb/etat/prefs/prefs.xml", seats + "fumee/lotA/a/etat/partage/morceau.zip")
        self.assertEqual([p for p, yes in got.items() if not yes], [], "these files of a seat's etat/ must be ignored")

    def test_nothing_else_is_hidden(self):
        seats = "docs/audits/ultra/sieges/"
        got = self.ignored(seats + "tour-7/lotA/a/rapport.md", seats + "tour-7/lotA/a/journal.txt", seats + "tour-7/lotA/a/etat.txt",
                           seats + "tour-7/etat-des-lieux.md", seats + "tour-7/lotA/a/screen-1.txt", "docs/audits/ultra/etat/note.md",
                           "docs/etat/note.md", "app/src/main/java/etat/Etat.kt", "etat/x.txt")
        self.assertEqual([p for p, yes in got.items() if yes], [], "only a seat's etat/ folder is ignored")


class ReviewOfTheLot(unittest.TestCase):
    """Fresh review of the lot « portail3 »: G1 (the named pairs are frozen: a later lot cannot add a line that hides a
    gap of the judge), G2 (the rule « a grave fix adds its replay » reads the gravity where the branch cannot rewrite
    it: at its base and at the integration's head too) and M1 (a block of code showing the format is not a line)."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.repo = os.path.join(self.tmp, "repo")
        os.makedirs(self.repo)
        git(self.repo, "init", "-q", "-b", "int")
        self.verdicts = os.path.join(self.repo, "docs", "audits", "ultra", "verdicts.tsv")
        self.triage = os.path.join(self.repo, "docs", "audits", "ultra", "sieges", "tour-9", "verdicts.tsv")
        self.report = os.path.join(self.repo, "docs", "audits", "ultra", "tour-9-z-rapport.md")
        write(self.verdicts, "item\tgravite\tetat\nV1\tgrave\touvert\nV2\tmoyen\touvert\n")
        write(self.triage, "n°\tverdict\tgravité\nG1\tréel\tgrave\nM1\tréel\tmoyen\n")
        self.commit("base")
        git(self.repo, "checkout", "-q", "-b", "eq/tour-9-z")

    def commit(self, message):
        git(self.repo, "add", "-A")
        git(self.repo, "-c", "user.name=t", "-c", "user.email=t@example.org", "commit", "-q", "-m", message)

    def corrections(self, report, message="the lot"):
        write(self.report, report)
        self.commit(message)
        r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "rapport-zone.py"), "--corrections", "eq/tour-9-z",
                            "docs/audits/ultra/tour-9-z-rapport.md", "base:int", "tour:tour-9", "tete:int"],
                           cwd=self.repo, capture_output=True, text=True)
        return r.returncode, r.stdout + r.stderr

    def test_witness_a_grave_row_turned_fixed_needs_its_line(self):
        write(self.verdicts, "item\tgravite\tetat\nV1\tgrave\tcorrigé\nV2\tmoyen\touvert\n")
        code, out = self.corrections("tests: aucun : toy\n")
        self.assertEqual(code, 1, out)
        self.assertIn("V1", out)

    def test_the_branch_cannot_lower_the_gravity_of_the_row_it_fixes(self):
        write(self.verdicts, "item\tgravite\tetat\nV1\tmoyen\tcorrigé\nV2\tmoyen\touvert\n")
        code, out = self.corrections("tests: aucun : toy\n")
        self.assertEqual(code, 1, out)
        self.assertIn("V1", out)

    def test_the_branch_cannot_take_the_grave_row_away(self):
        write(self.verdicts, "item\tgravite\tetat\nV2\tmoyen\touvert\n")
        code, out = self.corrections("tests: aucun : toy\n")
        self.assertEqual(code, 1, out)
        self.assertIn("V1", out)

    def test_fixed_is_read_with_words_after_it(self):
        write(self.verdicts, "item\tgravite\tetat\nV1\tgrave\tcorrigé (lot z)\nV2\tmoyen\touvert\n")
        code, out = self.corrections("tests: aucun : toy\n")
        self.assertEqual(code, 1, out)
        self.assertIn("V1", out)

    def test_the_branch_cannot_lower_a_grave_of_the_triage(self):
        for row in ("G1\tréel\tmoyen", "G1\tvoulu\tgrave"):
            write(self.triage, "n°\tverdict\tgravité\n" + row + "\nM1\tréel\tmoyen\n")
            code, out = self.corrections("tests: aucun : toy\ncorrige: G1 | gravité: moyen\n", message="lot " + row)
            self.assertEqual(code, 1, row + "\n" + out)
            self.assertIn("G1", out)

    def test_a_medium_row_fixed_needs_nothing(self):
        write(self.verdicts, "item\tgravite\tetat\nV1\tgrave\touvert\nV2\tmoyen\tcorrigé\n")
        code, out = self.corrections("tests: aucun : toy\n")
        self.assertEqual(code, 0, out)

    def test_a_block_of_code_showing_the_format_is_not_a_line(self):
        code, out = self.corrections("tests: aucun : toy\n\nLe format :\n```\ncorrige: <id du défaut> | gravité: bloquant|grave|moyen|mineur | rejeu: <id du rejeu>\n"
                                     "ne corrige pas: <id> : <raison>\n```\n")
        self.assertEqual(code, 0, out)

    def test_this_lot_s_own_report_passes(self):
        root = os.path.dirname(os.path.dirname(HERE))
        report = "docs/audits/ultra/tour-6-portail3-rapport.md"
        if not os.path.isfile(os.path.join(root, report)):
            self.skipTest("the lot's report is not here")
        write(self.report, read(os.path.join(root, report)))
        self.commit("the real report")
        r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "rapport-zone.py"), "--corrections", "eq/tour-9-z",
                            "docs/audits/ultra/tour-9-z-rapport.md", "base:int", "tour:tour-9", "tete:int"],
                           cwd=self.repo, capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stdout)

    def test_the_named_pairs_are_frozen(self):
        """The judge reads juge-paires-nommees.tsv only as this lot wrote it (its sha256 is in the judge): a line added
        later is a gap of its own and pairs nothing."""
        import hashlib
        import importlib.util
        spec = importlib.util.spec_from_file_location("vc", os.path.join(HERE, "verifie-chantier.py"))
        judge = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(judge)
        root = os.path.dirname(os.path.dirname(HERE))
        raw = open(os.path.join(root, "scripts", "bench", "juge-paires-nommees.tsv"), "rb").read()
        self.assertEqual(getattr(judge, "NAMED_SHA256", None), hashlib.sha256(raw).hexdigest(),
                         "the judge names the sha256 of the file of named pairs")
        toy = os.path.join(self.tmp, "toy")
        path = os.path.join(toy, "scripts", "bench", "juge-paires-nommees.tsv")
        write(path, raw.decode("utf-8"))
        rows, gaps = judge.named_lines(toy)
        self.assertEqual((len(rows), gaps), (31, []))
        write(path, raw.decode("utf-8") + "\t".join(["a" * 40, "b" * 40, "AnyTest", "une ligne de plus"]) + "\n")
        rows, gaps = judge.named_lines(toy)
        self.assertEqual(rows, [], "a changed file pairs nothing")
        self.assertEqual(len(gaps), 1, gaps)
        self.assertIn("changé", gaps[0])


class Hooks(unittest.TestCase):
    """This file is run where the bench's other script tests are: the self-test and pre-vol.sh --ci."""

    def test_the_preflight_and_the_selftest_run_this_file(self):
        self.assertTrue("scripts/bench/test_portail3.py" in read(os.path.join(HERE, "pre-vol.sh")), "pre-vol --ci must run this test")
        self.assertTrue("scripts/bench/test_portail3.py" in read(os.path.join(HERE, "selftest.sh")), "the self-test must run this test")


if __name__ == "__main__":
    unittest.main()
