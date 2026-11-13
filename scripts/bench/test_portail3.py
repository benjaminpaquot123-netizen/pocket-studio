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


class Hooks(unittest.TestCase):
    """This file is run where the bench's other script tests are: the self-test and pre-vol.sh --ci."""

    def test_the_preflight_and_the_selftest_run_this_file(self):
        self.assertIn("scripts/bench/test_portail3.py", read(os.path.join(HERE, "pre-vol.sh")), "pre-vol --ci must run this test")
        self.assertIn("scripts/bench/test_portail3.py", read(os.path.join(HERE, "selftest.sh")), "the self-test must run this test")


if __name__ == "__main__":
    unittest.main()
