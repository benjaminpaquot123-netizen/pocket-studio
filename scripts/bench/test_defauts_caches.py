#!/usr/bin/env python3
"""Checks defauts-caches.py (audit « banc crédible » 2, MT1: the defects the bench hid) on files made here, then on the
repository's own defauts-caches.tsv (`python3 -I scripts/bench/test_defauts_caches.py`). Standard library only.
To be run by the selftest with test_ultra_bench.py once the « portail » lot is merged (selftest.sh is that lot's)."""
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "defauts-caches.py")
FAILED = []


def check(ok, what):
    if not ok:
        FAILED.append(what)
        print("ÉCHEC :", what)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def run(*args):
    r = subprocess.run([sys.executable, "-I", SCRIPT, *args], capture_output=True, text=True)
    return r.returncode, r.stdout + r.stderr


HEAD = "id\tdate\tsource\tdefaut\tverdicts\tvu\tcause\tpreuve\trejeu\treparable\tplan\n"
RETOURS = """- 2026-11-08, téléphone :
  - tel-03 ❌ (.zip absent).
  - tel-04 ✅ WAV.
  - tel-05 ❌ un pad se joue.
  - tel-12 ✅ « ■ Tout » coupe le son, mais les mesures continuent. À compter comme défaut partiel.
- 2026-11-08, tablette, **recette tel-14 = ❌ « figé »**.
"""
PLAN = "| R.8 | quelque chose | preuve | 1 h |\n"
TEST = "class BenchFooTest {\n    @Test\n    fun showsIt() {}\n}\n"


def hidden(d):
    """A trial ❌ without its row, values outside their lists, a repairable hidden one without its plan line, a rejeu
    naming no test: each a gap; the rate counts what the bench can see, a partial one ½, a rejeu as seen today."""
    root = os.path.join(d, "repo")
    write(os.path.join(root, "app", "src", "test", "java", "x", "BenchFooTest.kt"), TEST)
    retours = os.path.join(d, "retours.md")
    plan = os.path.join(d, "PLAN-RESTE.md")
    write(retours, RETOURS)
    write(plan, PLAN)
    tsv = os.path.join(d, "caches.tsv")
    write(tsv, "# notes\n" + HEAD
          + "B1\td\ttel-03\tzip\t-\toui\t-\tsiège\t-\t-\t-\n"
          + "B2\td\ttel-05\tpad\t-\tpartiel\tecran-texte\tp\t-\toui\tR.8\n"
          + "B3\td\ttel-14\tcurseur\t-\tnon\tfaux-moteur\tp\tBenchFooTest#showsIt\toui\t-\n"
          + "B4\td\tremarque\tgoût\t-\tnon\tjugement\tp\t-\tnon\t-\n")
    code, out = run("--tsv", tsv, "--retours", retours, "--plan", plan, "--root", root)
    check(code == 1 and "tel-12 : essai ❌ de retours-benjamin.md sans ligne" in out, "a partial defect without its row:\n" + out)
    check("tel-03" not in out.split("défauts vus")[0] and "tel-14 :" not in out, "trials with their row are no gap:\n" + out)
    check("montrés aujourd'hui : 2.5/3 (83 %, à l'essai 50 %)" in out, "3 the bench can see; 1 + ½ at the trial, + 1 by a rejeu today:\n" + out)
    with open(tsv, "a", encoding="utf-8") as f:
        f.write("B5\td\ttel-12\ttransport\t-\tpeut-être\t-\t-\t-\t-\t-\n"
                "B6\td\tremarque\tlien\t-\tnon\tflou\tp\t-\toui\tR.8\n"
                "B7\td\tremarque\tdéfilement\t-\tnon\tecran-texte\tp\t-\toui\tR.99\n"
                "B8\td\tremarque\tjoue\t-\tnon\tfaux-moteur\tp\tBenchFooTest#gone\toui\t-\n"
                "B9\td\tremarque\tcompteur\t-\tnon\ttemps\tp\t-\t?\t-\n")
    code, out = run("--tsv", tsv, "--retours", retours, "--plan", plan, "--root", root)
    check("tel-12 : essai" not in out, "tel-12 has its row now:\n" + out)
    check("B5 : vu « peut-être »" in out, "vu outside its list:\n" + out)
    check("B6 : cause « flou » hors liste" in out, "a cause outside its list:\n" + out)
    check("B7 : caché, réparable, sans ligne de plan dans PLAN-RESTE.md (« R.99 »)" in out, "a plan line that does not exist:\n" + out)
    check("B8 : rejeu BenchFooTest#gone introuvable" in out, "a rejeu naming no test:\n" + out)
    check("B9 : réparable « ? »" in out, "repairable outside its list:\n" + out)
    check("B2 :" not in out and "B3 :" not in out, "a plan line that exists, a rejeu that exists: no gap:\n" + out)
    code, out = run("--tsv", os.path.join(d, "absent.tsv"), "--retours", retours, "--plan", plan, "--root", root)
    check(code == 1 and "fichier absent" in out and "défauts cachés : ?" in out, "no file: a gap, never a rate:\n" + out)


def repository():
    """The repository's own file: every ❌ of Benjamin's notes has its row, 0 gap, and the rate is written."""
    code, out = run()
    check(code == 0 and "défauts cachés : 0 écart(s)" in out, "the repository's defauts-caches.tsv:\n" + out)
    check("seuil 80 %" in out, "the threshold is written:\n" + out)


def main():
    with tempfile.TemporaryDirectory() as d:
        hidden(d)
    repository()
    if FAILED:
        print(f"{len(FAILED)} échec(s)")
        sys.exit(1)
    print("test_defauts_caches : vert")


if __name__ == "__main__":
    main()
