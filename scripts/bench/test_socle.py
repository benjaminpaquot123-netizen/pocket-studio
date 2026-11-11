#!/usr/bin/env python3
"""Tests of the bench's base (« socle du banc », docs/PLAN-SESSIONS-PARALLELES.md, discussion A).

Seconds, no Gradle:  python3 -I scripts/bench/test_socle.py
"""
import os
import re
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def read(*parts):
    with open(os.path.join(ROOT, *parts), encoding="utf-8") as f:
        return f.read()


def jobs_of(workflow):
    """The jobs of a GitHub workflow, read without PyYAML (not on every runner under python3 -I): {name: text}."""
    body = workflow.split("\njobs:\n", 1)[1]
    jobs, name, lines = {}, None, []
    for line in body.splitlines():
        m = re.match(r"^  ([A-Za-z0-9_-]+):\s*$", line)
        if m:
            if name:
                jobs[name] = "\n".join(lines)
            name, lines = m.group(1), []
        elif name:
            lines.append(line)
    if name:
        jobs[name] = "\n".join(lines)
    return jobs


def needs_of(job):
    m = re.search(r"^    needs:\s*(.+)$", job, re.M)
    if not m:
        return []
    return [n.strip() for n in m.group(1).strip("[] ").split(",") if n.strip()]


class QuickChecksFirst(unittest.TestCase):
    """Point 4: the checks of a few seconds run first on GitHub; nothing long starts before they are green."""

    def setUp(self):
        self.jobs = jobs_of(read(".github", "workflows", "ci.yml"))

    def test_a_quick_job_needs_nothing(self):
        self.assertIn("quick", self.jobs, "ci.yml has no « quick » job")
        self.assertEqual(needs_of(self.jobs["quick"]), [], "the quick job must start at once")

    def test_the_quick_job_runs_the_bench_tests_and_the_preflight(self):
        quick = self.jobs.get("quick", "")
        self.assertIn("scripts/bench/pre-vol.sh --ci --rapide", quick)
        self.assertNotIn("gradlew", quick, "no Gradle in the quick job")

    def test_every_other_job_waits_for_it(self):
        # Directly or through another job: the long jobs never start on a red quick check.
        def waits(name, seen=()):
            needs = needs_of(self.jobs[name])
            return "quick" in needs or any(waits(n, seen + (name,)) for n in needs if n not in seen and n in self.jobs)
        for name in self.jobs:
            if name != "quick":
                self.assertTrue(waits(name), f"job « {name} » does not wait for « quick »")

    def test_the_preflight_lists_its_quick_mode(self):
        pre_vol = read("scripts", "bench", "pre-vol.sh")
        self.assertIn("--rapide", pre_vol)
        self.assertIn("scripts/bench/test_socle.py", pre_vol, "pre-vol --ci must run this test too")


if __name__ == "__main__":
    unittest.main(verbosity=2)
