"""Tour 6, lot « stable »: scripts/bench/ci-rouge.sh on toy JUnit reports (no Gradle, no network, seconds).

    python3 -I scripts/bench/test_stable.py
"""
import os
import subprocess
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.join(HERE, "ci-rouge.sh")


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

    def test_hooked_in_the_pre_flight_and_the_self_test(self):
        for script in ("pre-vol.sh", "selftest.sh"):
            with open(os.path.join(HERE, script)) as f:
                self.assertIn("scripts/bench/test_stable.py", f.read(), f"{script} must run this test")


if __name__ == "__main__":
    unittest.main()
