"""Tour 6, lot « stable »: scripts/bench/ci-rouge.sh on toy JUnit reports (no Gradle, no network, seconds).
Tour 9, lot « ci-outils »: ci-rouge.sh without gh (curl on the REST API) and ci-etat.sh with several runs of one commit,
both on GitHub answers recorded in scripts/bench/tests/ci-outils/ (no network, no token).

    python3 -I scripts/bench/test_stable.py
"""
import json
import os
import shutil
import subprocess
import tempfile
import unittest
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.join(HERE, "ci-rouge.sh")
STATE = os.path.join(HERE, "ci-etat.sh")
FIXTURES = os.path.join(HERE, "tests", "ci-outils")

NO_GH = "#!/usr/bin/env bash\n# A gh that is not signed in: every call fails.\nexit 4\n"
# Stand-in for curl on GitHub's REST API: the recorded answers of FIXTURES (copied into $PS_TOY_CI); every URL is noted.
FAKE_CURL = r"""#!/usr/bin/env bash
out=""; url=""
while [ $# -gt 0 ]; do
  case "$1" in -o) out="$2"; shift ;; -H|--max-time) shift ;; -*) ;; *) url="$1" ;; esac
  shift
done
echo "$url" >> "$PS_TOY_CI/urls"
body=""
case "$url" in
  *"/actions/runs?head_sha="*) body="$PS_TOY_CI/runs.json" ;;
  *"/actions/runs/"*"/jobs"*) id="${url#*/actions/runs/}"; body="$PS_TOY_CI/jobs-${id%%/*}.json" ;;
  *"/actions/runs/"*"/artifacts"*) id="${url#*/actions/runs/}"; body="$PS_TOY_CI/artifacts-${id%%/*}.json" ;;
  *"/actions/artifacts/"*"/zip") id="${url#*/actions/artifacts/}"; body="$PS_TOY_CI/art-${id%%/*}.zip" ;;
esac
[ -n "$body" ] && [ -f "$body" ] || exit 22
if [ -n "$out" ]; then cp "$body" "$out"; else cat "$body"; fi
"""


def suite(name, stamp, cases):
    """A JUnit XML report: [cases] are (method, seconds, failure message or None)."""
    body = []
    for method, secs, failure in cases:
        if failure is None:
            body.append(f'<testcase name="{method}" classname="{name}" time="{secs}"/>')
        else:
            body.append(
                f'<testcase name="{method}" classname="{name}" time="{secs}">'
                f'<failure message="{failure}" type="java.lang.AssertionError">{failure}\n\tat x.y(Z.kt:1)</failure></testcase>'
            )
    fails = sum(1 for c in cases if c[2] is not None)
    return (
        f'<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="{name}" tests="{len(cases)}" failures="{fails}" '
        f'errors="0" timestamp="{stamp}" time="1.0">{"".join(body)}</testsuite>\n'
    )


def run(*args):
    return subprocess.run(["bash", TOOL, *args], capture_output=True, text=True, timeout=60)


class CiRougeTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        d = os.path.join(self.tmp.name, "test-results", "testDebugUnitTest")
        os.makedirs(d)
        # Written in an order that is NOT the run's: the tool must order by the reports' timestamps.
        toys = [
            ("com.x.EClass", "2026-11-13T20:00:05.000Z", [("e", "0.1", None)]),
            ("com.x.AClass", "2026-11-13T20:00:01.000Z", [("a", "0.1", None)]),
            ("com.x.DClass", "2026-11-13T20:00:04.000Z", [("d1", "2.5", "ClosingSaves busy = 1"), ("d2", "0.2", None)]),
            ("com.x.BClass", "2026-11-13T20:00:02.000Z", [("b", "0.1", None)]),
            ("com.x.CClass", "2026-11-13T20:00:03.000Z", [("c", "0.1", None)]),
        ]
        for name, stamp, cases in toys:
            with open(os.path.join(d, f"TEST-{name}.xml"), "w") as f:
                f.write(suite(name, stamp, cases))

    def tearDown(self):
        self.tmp.cleanup()

    def test_each_red_test_with_its_rank_its_three_neighbours_and_its_duration(self):
        r = run("--dossier", self.tmp.name)
        self.assertEqual(r.returncode, 0, r.stderr)
        out = r.stdout
        self.assertIn("ROUGE com.x.DClass#d1 (2.5 s)", out)
        self.assertIn("message : ClosingSaves busy = 1", out)
        self.assertIn("rang    : classe 4/5", out)
        self.assertIn("avant   : com.x.AClass > com.x.BClass > com.x.CClass", out)
        self.assertNotIn("d2", out, "a green test of the same class is not listed")
        self.assertIn("1 test(s) rouge(s), 5 classes lues", out)

    def test_a_green_run_says_none(self):
        d = os.path.join(self.tmp.name, "test-results", "testDebugUnitTest")
        os.remove(os.path.join(d, "TEST-com.x.DClass.xml"))
        r = run("--dossier", self.tmp.name)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("0 test(s) rouge(s), 4 classes lues", r.stdout)
        self.assertNotIn("ROUGE", r.stdout)

    def test_a_missing_folder_or_no_report_is_refused(self):
        self.assertEqual(run("--dossier", os.path.join(self.tmp.name, "nope")).returncode, 2)
        empty = tempfile.mkdtemp(dir=self.tmp.name)
        r = run("--dossier", empty)
        self.assertEqual(r.returncode, 2)
        self.assertIn("no JUnit XML", r.stderr)

    def test_a_downloaded_artifact_is_removed_and_the_exit_code_kept(self):
        # Review M8: the run-id path downloads into a temporary folder removed by an EXIT trap; `exec python3` replaced the
        # shell, the trap never ran, every call left its folder behind.
        bin_dir = os.path.join(self.tmp.name, "bin")
        os.makedirs(bin_dir)
        seen = os.path.join(self.tmp.name, "gh-dir.txt")
        src = os.path.join(self.tmp.name, "test-results", "testDebugUnitTest")
        with open(os.path.join(bin_dir, "gh"), "w") as f:
            # A stand-in for `gh run download <run> -R <repo> -n unit-test-reports -D <dir>`: the toy reports into <dir>.
            f.write(
                "#!/usr/bin/env bash\n"
                'while [ $# -gt 0 ]; do [ "$1" = -D ] && dir="$2"; shift; done\n'
                f'echo "$dir" > "{seen}"\n'
                f'cp "{src}"/*.xml "$dir"/\n'
            )
        os.chmod(os.path.join(bin_dir, "gh"), 0o755)
        env = dict(os.environ, PATH=bin_dir + os.pathsep + os.environ.get("PATH", ""))
        r = subprocess.run(["bash", TOOL, "12345"], capture_output=True, text=True, timeout=60, env=env)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("ROUGE com.x.DClass#d1", r.stdout)
        with open(seen) as f:
            downloaded = f.read().strip()
        self.assertTrue(downloaded, "the stand-in gh was called with a folder")
        self.assertFalse(os.path.exists(downloaded), f"the downloaded artifact's folder is left behind: {downloaded}")
        # The exit code of the reading is kept: a folder without any report is refused (2), the folder removed all the same.
        with open(os.path.join(bin_dir, "gh"), "w") as f:
            f.write("#!/usr/bin/env bash\n" 'while [ $# -gt 0 ]; do [ "$1" = -D ] && dir="$2"; shift; done\n' f'echo "$dir" > "{seen}"\n')
        r = subprocess.run(["bash", TOOL, "12345"], capture_output=True, text=True, timeout=60, env=env)
        self.assertEqual(r.returncode, 2, r.stdout + r.stderr)
        with open(seen) as f:
            downloaded = f.read().strip()
        self.assertFalse(os.path.exists(downloaded), f"the empty artifact's folder is left behind: {downloaded}")

    def test_hooked_in_the_pre_flight_and_the_self_test(self):
        for script in ("pre-vol.sh", "selftest.sh"):
            with open(os.path.join(HERE, script)) as f:
                self.assertIn("scripts/bench/test_stable.py", f.read(), f"{script} must run this test")


def toy_github(tmp):
    """A PATH with a gh that is not signed in and a curl that answers from the recorded fixtures."""
    toy = os.path.join(tmp, "toy-github")
    os.makedirs(os.path.join(toy, "bin"))
    for f in os.listdir(FIXTURES):
        shutil.copy(os.path.join(FIXTURES, f), toy)
    shutil.copy(os.path.join(FIXTURES, "runs-two-events.json"), os.path.join(toy, "runs.json"))
    for name, body in (("gh", NO_GH), ("curl", FAKE_CURL)):
        with open(os.path.join(toy, "bin", name), "w") as f:
            f.write(body)
        os.chmod(os.path.join(toy, "bin", name), 0o755)
    env = dict(os.environ, PATH=os.path.join(toy, "bin") + os.pathsep + os.environ.get("PATH", ""), PS_TOY_CI=toy)
    return toy, env


class CiRougeSansGhTest(unittest.TestCase):
    """Défaut 2: ci-rouge.sh <run id> needed a signed-in gh; our sessions have none. It falls back to curl on the REST
    API: the run's artifacts listed, the live « unit-test-reports » downloaded as a zip and unzipped in a new folder."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.toy, self.env = toy_github(self.tmp.name)
        xml = suite("com.x.RedClass", "2026-11-25T10:00:02.000Z", [("r", "1.5", "expected 1 but was 2")])
        with zipfile.ZipFile(os.path.join(self.toy, "art-5552.zip"), "w") as z:
            z.writestr("shard-2/test-results/TEST-com.x.RedClass.xml", xml)
            z.writestr("shard-2/test-results/TEST-com.x.GreenClass.xml", suite("com.x.GreenClass", "2026-11-25T10:00:01.000Z", [("g", "0.1", None)]))

    def tearDown(self):
        self.tmp.cleanup()

    def call(self, run_id):
        return subprocess.run(["bash", TOOL, run_id], capture_output=True, text=True, timeout=60, env=self.env)

    def test_without_gh_the_artifact_is_read_through_the_rest_api(self):
        r = self.call("9100")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertIn("ROUGE com.x.RedClass#r (1.5 s)", r.stdout)
        self.assertIn("message : expected 1 but was 2", r.stdout)
        self.assertIn("lot     : 2", r.stdout)
        self.assertIn("avant   : com.x.GreenClass", r.stdout)
        urls = open(os.path.join(self.toy, "urls")).read()
        self.assertIn("/actions/runs/9100/artifacts", urls)
        self.assertIn("/actions/artifacts/5552/zip", urls, "the live artifact, not the expired one")
        self.assertNotIn("/actions/artifacts/5551/", urls)
        self.assertNotIn("/actions/artifacts/5550/", urls, "not another artifact of the run")

    def test_no_live_report_artifact_is_refused(self):
        shutil.copy(os.path.join(FIXTURES, "artifacts-empty.json"), os.path.join(self.toy, "artifacts-9100.json"))
        r = self.call("9100")
        self.assertEqual(r.returncode, 2, r.stdout + r.stderr)
        self.assertIn("unit-test-reports", r.stderr)

    def test_a_zip_reaching_out_of_its_folder_is_refused(self):
        with zipfile.ZipFile(os.path.join(self.toy, "art-5552.zip"), "w") as z:
            z.writestr("../../evil-TEST-x.xml", "x")
        r = self.call("9100")
        self.assertEqual(r.returncode, 2, r.stdout + r.stderr)
        self.assertFalse(any("evil" in f for f in os.listdir(self.tmp.name)), "nothing written out of the new folder")


class CiEtatPlusieursRunsTest(unittest.TestCase):
    """Défaut 1: two runs of one commit (a green « workflow_dispatch », a red « push ») were printed one under the other,
    the first header « CI null »: the chef read « CI success ». Each run now says id, event, branch and state, and one
    verdict says « rouge » when any run of the commit is red."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.toy, self.env = toy_github(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def state(self):
        r = subprocess.run(["bash", STATE, "HEAD"], capture_output=True, text=True, timeout=60, env=self.env)
        self.assertEqual(r.returncode, 0, r.stderr)
        return r.stdout

    def test_each_run_on_its_own_line_and_a_red_verdict(self):
        out = self.state()
        self.assertIn("run 9101\tworkflow_dispatch\teq/tour-9-paquet\tCI\tvert", out)
        self.assertIn("run 9100\tpush\teq/tour-9-paquet\tCI\trouge", out)
        self.assertIn("ci_verdict\trouge (1 run rouge sur 2 : 9100 push)", out)
        self.assertNotIn("null", out)
        self.assertIn("ci_build\trouge", out)
        self.assertIn("ci_emulateur\trouge", out)

    def test_a_later_green_run_does_not_hide_an_earlier_red_one(self):
        with open(os.path.join(self.toy, "runs.json")) as f:
            d = json.load(f)
        d["workflow_runs"].reverse()  # the red run read first: its red emulator must not be overwritten by the green
        with open(os.path.join(self.toy, "runs.json"), "w") as f:
            json.dump(d, f)
        out = self.state()
        self.assertIn("ci_verdict\trouge", out)
        self.assertIn("ci_build\trouge", out)
        self.assertIn("ci_emulateur\trouge", out)

    def test_only_green_runs_give_a_green_verdict(self):
        with open(os.path.join(self.toy, "runs.json")) as f:
            d = json.load(f)
        d["workflow_runs"] = d["workflow_runs"][:1]
        with open(os.path.join(self.toy, "runs.json"), "w") as f:
            json.dump(d, f)
        out = self.state()
        self.assertIn("ci_verdict\tvert (1 run)", out)
        self.assertIn("ci_build\tvert", out)

    def test_a_running_run_is_neither_green_nor_null(self):
        with open(os.path.join(self.toy, "runs.json")) as f:
            d = json.load(f)
        d["workflow_runs"][1].update(status="in_progress", conclusion=None)
        d["workflow_runs"] = d["workflow_runs"][1:]
        with open(os.path.join(self.toy, "runs.json"), "w") as f:
            json.dump(d, f)
        out = self.state()
        self.assertIn("run 9100\tpush\teq/tour-9-paquet\tCI\ten cours", out)
        self.assertIn("ci_verdict\ten cours", out)
        self.assertNotIn("null", out)


if __name__ == "__main__":
    unittest.main()
