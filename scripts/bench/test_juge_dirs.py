#!/usr/bin/env python3
"""Test of the judge's test folders (scripts/bench/verifie-chantier.py, METHODE-V4 astuce D): a proof that names a test
of project-model (the pure Kotlin module) is found and runs; before, it was « introuvable » (the tuto4 exception of
14 November). Seconds, no Gradle, a toy tree:

  python3 -I scripts/bench/test_juge_dirs.py
"""
import importlib.util
import os
import shutil
import sys
import tempfile
import unittest

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("verifie_chantier", os.path.join(HERE, "verifie-chantier.py"))
judge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(judge)

MODEL_TEST = """package com.pocketstudio.model

import org.junit.Test

class TutoStepsModelTest {
    @Test
    fun stepsOfEveryStyleAreNumberedInOrder() {
    }
}
"""
APP_TEST = """package com.pocketstudio.app

import org.junit.Test

class AppSideTest {
    @Test
    fun appSideStillFound() {
    }
}
"""


def write(root, rel, text):
    path = os.path.join(root, *rel.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


class JudgeTestDirs(unittest.TestCase):
    def setUp(self):
        self.root = tempfile.mkdtemp()
        write(self.root, "project-model/src/test/kotlin/com/pocketstudio/model/TutoStepsModelTest.kt", MODEL_TEST)
        write(self.root, "app/src/test/java/com/pocketstudio/app/AppSideTest.kt", APP_TEST)

    def tearDown(self):
        shutil.rmtree(self.root, ignore_errors=True)

    def test_project_model_tests_are_in_the_judge_s_folders(self):
        self.assertIn("project-model/src/test/kotlin", judge.TEST_DIRS)
        self.assertTrue("project-model/src/test/kotlin/a/BTest.kt".startswith(judge.TEST_DIRS))

    def test_a_proof_in_project_model_exists_and_runs(self):
        sources = judge.test_sources(self.root)
        self.assertTrue(judge.method_exists(sources, "TutoStepsModelTest", "stepsOfEveryStyleAreNumberedInOrder"))
        self.assertTrue(judge.test_runs(sources, "TutoStepsModelTest", "stepsOfEveryStyleAreNumberedInOrder"))

    def test_app_tests_are_still_read(self):
        sources = judge.test_sources(self.root)
        self.assertTrue(judge.method_exists(sources, "AppSideTest", "appSideStillFound"))

    def test_a_missing_method_is_still_missing(self):
        sources = judge.test_sources(self.root)
        self.assertFalse(judge.method_exists(sources, "TutoStepsModelTest", "noSuchTestAnywhereHere"))

    def test_the_real_project_model_tests_are_read(self):
        real = os.path.dirname(os.path.dirname(HERE))
        if not os.path.isdir(os.path.join(real, "project-model", "src", "test", "kotlin")):
            self.skipTest("no project-model tests in this checkout")
        sources = judge.test_sources(real)
        names = {os.path.splitext(n)[0] for _, _, fs in os.walk(os.path.join(real, "project-model", "src", "test", "kotlin"))
                 for n in fs if n.endswith(".kt")}
        self.assertTrue(names & set(sources), "no project-model test class read by the judge")


if __name__ == "__main__":
    unittest.main(verbosity=1)
