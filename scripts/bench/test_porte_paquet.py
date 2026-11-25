#!/usr/bin/env python3
"""Tests of the batch gate (lot « porte-paquet », tour 9): scripts/bench/porte-paquet.sh and the PS_PAQUET_ETAPE steps of
scripts/bench/fusion-equipes.sh.

The gate took 46 to 75 min per lot, whatever its size: the bench's self-test and the replays ran again for each zone.
In batch mode (up to three zones) the checks of every zone run first, then every merge, then ONE self-test, the
replays and the union of the zones' filters; then the portal's traces of each zone, which point at the common
self-test. Red: everything back to the starting commit, then the zones one by one as before, to name the culprit.
Before anything runs, a pre-check of a few seconds refuses with a clear message (disk, time limit, a filter matching
no test class, a missing branch, a pre-flight not « PRÊT »). Without batch mode the portal does not change.

Seconds, no Gradle, on toy repositories:  python3 -I scripts/bench/test_porte_paquet.py
"""
import importlib.util
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
GATE = "scripts/bench/porte-paquet.sh"
PORTAL = "scripts/bench/fusion-equipes.sh"
# The portal as it was before this lot (integration 9f0c5eb8): without batch mode the new one must behave the same.
PORTAL_BEFORE = "9f0c5eb8"

ENV = dict(os.environ, GIT_AUTHOR_NAME="t", GIT_AUTHOR_EMAIL="t@example.org", GIT_COMMITTER_NAME="t",
           GIT_COMMITTER_EMAIL="t@example.org", GIT_AUTHOR_DATE="2026-11-24T10:00:00Z",
           GIT_COMMITTER_DATE="2026-11-24T10:00:00Z", GIT_CONFIG_GLOBAL="/dev/null", GIT_CONFIG_NOSYSTEM="1")
for k in ("PS_PAQUET_ETAPE", "PS_PAQUET_LOT", "PS_PAQUET_LIMITE_MIN", "PS_PAQUET_DISQUE_MIN_GO", "PS_JUGE_BOOST",
          "PS_REJEUX_DIR", "PS_REJEUX_SELFTEST"):
    ENV.pop(k, None)

SELFTEST = """#!/usr/bin/env bash
# The toy's self-test: says it ran; red when the code holds rouge.txt (a culprit zone brings it).
echo "selftest" >> "$PS_TOY_CALLS"
[ ! -f rouge.txt ] || exit 1
printf '1 verts, 0 rouges, 0 en quarantaine, 0 sautés\\n' > "$1/selftest-resume.txt"
echo "a stamp" > "$1/selftest.ok"
echo "chantier : 0 écarts" > "$1/selftest-chantier.txt"
"""

REJEUX = """#!/usr/bin/env bash
# The toy's replays: a batch gated by the self-test, whose files it leaves in $PS_REJEUX_SELFTEST.
echo "rejeux" >> "$PS_TOY_CALLS"
lot="$(mktemp -d)"
scripts/bench/selftest.sh "$lot" || { echo "selftest rouge : rejeux non joués"; exit 1; }
[ -z "${PS_REJEUX_SELFTEST:-}" ] || cp "$lot"/selftest* "$PS_REJEUX_SELFTEST"/
echo "t1-clavier : oracle tenu"
"""

JUDGE_MAIN = """

if __name__ == "__main__":
    # The toy's judge: the real « fusion sans trace du portail » count, as the portal reads it after each trace.
    gaps = merge_trace_gaps(".")
    for g in gaps:
        print(g)
    print(f'chantier : {len(gaps)} écarts')
"""

PREVOL = """#!/usr/bin/env bash
# The toy's pre-flight: « PRÊT » unless asked to refuse.
echo "prevol $*" >> "$PS_TOY_CALLS"
if [ -n "${PS_TOY_PREVOL_RED:-}" ]; then echo "  RECALÉ  la zone b n'a pas de relecture"; echo "PRÉ-VOL : RECALÉ (1 à corriger, 0 à voir)."; exit 1; fi
echo "PRÉ-VOL : PRÊT (0 à voir)."
"""


def write(path, text, mode=None):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    if mode:
        os.chmod(path, mode)


def git(root, *args, check=True):
    r = subprocess.run(["git", "-C", root, "-c", "commit.gpgsign=false", *args], capture_output=True, text=True, env=ENV)
    if check:
        assert r.returncode == 0, " ".join(args) + "\n" + r.stdout + r.stderr
    return r.stdout


def toy(d, name, portal_text=None, zones=("a", "b", "c"), culprit=None, paquet=10):
    """A toy integration workshop: the real portal (or portal_text), the real gate, stand-ins for the self-test, the
    replays, Gradle, the stamp, the pre-flight and the judge; one test class app/src/test/…/FooTest.kt; one team branch
    per zone, reviewed (« relu : <sha> », « Verdict : 0 bloquant, 0 grave »)."""
    root = os.path.join(d, name)
    bench = os.path.join(root, "scripts", "bench")
    with open(os.path.join(HERE, "fusion-equipes.sh"), encoding="utf-8") as f:
        write(os.path.join(bench, "fusion-equipes.sh"), portal_text if portal_text is not None else f.read(), 0o755)
    if os.path.isfile(os.path.join(HERE, "porte-paquet.sh")):
        shutil.copy(os.path.join(HERE, "porte-paquet.sh"), os.path.join(bench, "porte-paquet.sh"))
    write(os.path.join(bench, "portail-paquet.txt"), f"paquet: {paquet}\n")
    for extra in ("relecture-verdict.py", "decisions-index.sh", "rapport-zone.py", "reparations.py", "json-merge-ok.py"):
        shutil.copy(os.path.join(HERE, extra), os.path.join(bench, extra))
    write(os.path.join(bench, "selftest.sh"), SELFTEST, 0o755)
    write(os.path.join(bench, "rejeu-defauts.sh"), REJEUX, 0o755)
    write(os.path.join(bench, "gradle-file.sh"), '#!/usr/bin/env bash\necho "gradle $*" >> "$PS_TOY_CALLS"\n', 0o755)
    write(os.path.join(bench, "tampon.sh"), "#!/usr/bin/env bash\ncase \"$1\" in verifie) exit 1 ;; esac\n", 0o755)
    write(os.path.join(bench, "pre-vol.sh"), PREVOL, 0o755)
    with open(os.path.join(HERE, "verifie-chantier.py"), encoding="utf-8") as f:
        write(os.path.join(bench, "verifie-chantier.py"), f.read().split('\nif __name__ == "__main__":', 1)[0] + JUDGE_MAIN)
    write(os.path.join(root, "docs", "audits", "ultra", "rejeux", "t1-clavier", "start.txt"), "lang=fr\n")
    write(os.path.join(root, "app", "src", "test", "java", "com", "toy", "FooTest.kt"), "package com.toy\n\nimport org.junit.Test\n\nclass FooTest {\n    @Test\n    fun works() {}\n}\n")
    git(root, "init", "-q", "-b", "fx-int")
    git(root, "add", "-A")
    git(root, "commit", "-q", "-m", "toy")
    for z in zones:
        git(root, "checkout", "-q", "-b", f"eq/tour-9-{z}", "fx-int")
        write(os.path.join(root, "zones", f"{z}.txt"), f"{z}\n")
        # The zone report the portal reads from batch 2: its targeted tests and its list of paths.
        write(os.path.join(root, "docs", "audits", "ultra", f"tour-9-{z}-rapport.md"),
              "# Rapport\n\ntests: FooTest\n\n- chemin: Accueil | test: FooTest#works\n")
        if z == culprit:
            write(os.path.join(root, "rouge.txt"), "x\n")
        git(root, "add", "-A")
        git(root, "commit", "-q", "-m", f"team {z}")
        sha = git(root, "rev-parse", "HEAD").strip()
        write(os.path.join(root, "docs", "audits", "ultra", f"tour-9-{z}-relecture.md"),
              f"Relu : {sha}\n\nVerdict : 0 bloquant, 0 grave\n")
        git(root, "add", "-A")
        git(root, "commit", "-q", "-m", f"review {z}")
    git(root, "checkout", "-q", "fx-int")
    return root


def run(root, script, *args, **env):
    calls = os.path.join(root, ".git", "toy-calls.txt")
    open(calls, "w").close()
    e = dict(ENV, PS_TOY_CALLS=calls, PS_PAQUET_DISQUE_MIN_GO="0")
    e.update(env)
    r = subprocess.run(["bash", os.path.join(root, *script.split("/")), *args], cwd=root, capture_output=True, text=True, env=e)
    with open(calls, encoding="utf-8") as f:
        said = f.read()
    return r.returncode, r.stdout + r.stderr, said


def subjects(root):
    return git(root, "log", "--format=%s", "fx-int").splitlines()


def judge(root):
    """The real judge's « fusion sans trace du portail » on the toy."""
    spec = importlib.util.spec_from_file_location("verifie_chantier_real", os.path.join(HERE, "verifie-chantier.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod.merge_trace_gaps(root)


class Batch(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp()

    def tearDown(self):
        shutil.rmtree(self.d, ignore_errors=True)

    def test_the_gate_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(ROOT, *GATE.split("/"))), GATE + " is missing")

    def test_one_self_test_for_three_zones(self):
        root = toy(self.d, "vert")
        start = git(root, "rev-parse", "HEAD").strip()
        code, out, said = run(root, GATE, "tour-9", "a", "b", "c", "--", "*FooTest")
        self.assertEqual(code, 0, out)
        self.assertEqual(said.count("selftest"), 1, "one self-test for the batch:\n" + said)
        self.assertEqual(said.count("rejeux"), 1, said)
        self.assertEqual(len(re.findall(r"^gradle ", said, re.M)), 1, said)
        self.assertIn("--tests *FooTest", said)
        s = subjects(root)
        for z in "abc":
            self.assertIn(f"Merge team {z} of tour-9 (audit ultra)", s)
            self.assertIn(f"Portal traces of the merge of {z} (tour-9)", s)
        # The merges first, then the traces: the self-test ran on the three zones together.
        merges = git(root, "log", "--merges", "--format=%H", f"{start}..fx-int").split()
        self.assertEqual(len(merges), 3)
        traces = git(root, "ls-tree", "-r", "--name-only", "HEAD", "docs/audits/ultra/portail").split()
        folders = sorted({os.path.dirname(t) for t in traces})
        self.assertEqual(len(folders), 3, traces)
        for f in folders:
            names = sorted(os.path.basename(t) for t in traces if os.path.dirname(t) == f)
            self.assertEqual(names, ["fusion.txt", "selftest-chantier.txt", "selftest-resume.txt", "selftest.ok"])
            fusion = git(root, "show", f"HEAD:{f}/fusion.txt")
            self.assertRegex(fusion, r"(?m)^fusion: [0-9a-f]{40}$")
            self.assertRegex(fusion, r"(?m)^paquet: a b c$")
            self.assertRegex(fusion, r"(?m)^selftest commun: ")
        self.assertEqual(judge(root), [], "the judge counts no « fusion sans trace du portail » for a batch")
        self.assertEqual(git(root, "status", "--porcelain", "--untracked-files=no").strip(), "")

    def test_red_batch_falls_back_zone_by_zone(self):
        root = toy(self.d, "rouge", culprit="b")
        start = git(root, "rev-parse", "HEAD").strip()
        code, out, said = run(root, GATE, "tour-9", "a", "b", "c")
        self.assertNotEqual(code, 0, out)
        self.assertIn("b: red after the merge", out, "the fallback names the culprit:\n" + out)
        s = subjects(root)
        self.assertIn("Merge team a of tour-9 (audit ultra)", s)
        self.assertIn("Portal traces of the merge of a (tour-9)", s)
        self.assertNotIn("Merge team b of tour-9 (audit ultra)", s)
        self.assertNotIn("Merge team c of tour-9 (audit ultra)", s)
        # The batch's merges were undone before the fallback: a's merge is a fresh one on the start.
        self.assertEqual(git(root, "rev-parse", "HEAD~1^1").strip(), start)
        self.assertEqual(judge(root), [])

    def test_the_judge_never_refuses_a_batch_for_its_own_next_traces(self):
        # A previous portal trace at 0 gaps (zone p through the portal alone), then a batch of three: when a's trace is
        # written, b's and c's merges have none yet; that is owed by the next steps, not a judge gone up (no boost).
        root = toy(self.d, "juge", zones=("p", "a", "b", "c"))
        code, out, said = run(root, "scripts/bench/fusion-equipes.sh", "tour-9", "p")
        self.assertEqual(code, 0, out)
        self.assertIn("p: judge: 0 écarts", out)
        code, out, said = run(root, GATE, "tour-9", "a", "b", "c")
        self.assertEqual(code, 0, out)
        self.assertEqual(said.count("selftest"), 1, "no fallback zone by zone:\n" + out)
        self.assertIn("a: judge: 2 « fusion sans trace » of this batch's next zones, traced next: not counted", out)
        self.assertIn("a: judge: 0 écarts", out)
        self.assertIn("c: judge: 0 écarts", out)
        self.assertEqual(judge(root), [])

    def test_the_traces_step_refuses_a_merge_the_batch_did_not_make(self):
        # A merge made by hand and a folder made by hand: no portal trace for it.
        root = toy(self.d, "faux", zones=("a",))
        git(root, "merge", "-q", "--no-ff", "-m", "Merge team a of tour-9 (audit ultra)", "eq/tour-9-a")
        merged = git(root, "rev-parse", "HEAD").strip()
        lot = os.path.join(self.d, "lot-faux")
        for name, text in (("fusions", f"a {merged}\n"), ("filtres-a", ""), ("selftest.ok", "x\n"),
                           ("selftest-resume.txt", "x\n"), ("selftest-chantier.txt", "x\n"), ("rejeux.txt", "x\n")):
            write(os.path.join(lot, name), text)
        code, out, said = run(root, "scripts/bench/fusion-equipes.sh", "tour-9", "a", PS_PAQUET_ETAPE="traces", PS_PAQUET_LOT=lot)
        self.assertEqual(code, 3, out)
        self.assertIn("is not this batch's tested merge", out)
        self.assertNotIn("Portal traces of the merge of a (tour-9)", subjects(root))

    def test_batch_mode_from_paquet_10(self):
        root = toy(self.d, "neuf", paquet=9)
        code, out, said = run(root, GATE, "tour-9", "a", "b")
        self.assertNotEqual(code, 0, out)
        self.assertIn("from_paquet 10", out)
        self.assertNotIn("selftest", said)
        self.assertNotIn("Merge team a of tour-9 (audit ultra)", subjects(root))

    def test_more_than_three_zones_refused(self):
        root = toy(self.d, "quatre", zones=("a", "b", "c", "e"))
        code, out, said = run(root, GATE, "tour-9", "a", "b", "c", "e")
        self.assertNotEqual(code, 0)
        self.assertIn("3 zones au plus", out)
        self.assertNotIn("selftest", said)


class PreCheck(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp()
        self.root = toy(self.d, "pre")
        self.start = git(self.root, "rev-parse", "HEAD").strip()

    def tearDown(self):
        shutil.rmtree(self.d, ignore_errors=True)

    def refused(self, needle, *args, **env):
        code, out, said = run(self.root, GATE, *args, **env)
        self.assertNotEqual(code, 0, out)
        self.assertIn("REFUSÉ", out)
        self.assertIn(needle, out)
        self.assertNotIn("selftest", said, "nothing long started")
        self.assertNotIn("gradle", said)
        self.assertEqual(git(self.root, "rev-parse", "HEAD").strip(), self.start, "nothing merged")
        return out

    def test_disk(self):
        self.refused("disque libre", "tour-9", "a", PS_PAQUET_DISQUE_MIN_GO="100000")

    def test_time_limit_argument_and_variable(self):
        self.refused("90 min", "--limite", "60", "tour-9", "a")
        self.refused("90 min", "tour-9", "a", PS_PAQUET_LIMITE_MIN="45")
        code, out, _ = run(self.root, GATE, "--limite", "120", "tour-9", "a")
        self.assertEqual(code, 0, out)

    def test_filter_without_class(self):
        out = self.refused("*NoSuchTest", "tour-9", "a", "--", "*FooTest", "*NoSuchTest")
        self.assertNotIn("« *FooTest »", out)

    def test_filter_forms_that_match(self):
        code, out, _ = run(self.root, GATE, "tour-9", "a", "--", "com.toy.FooTest", "FooTest", "*FooTest.someMethod", "com.toy.*")
        self.assertEqual(code, 0, out)

    def test_missing_branch_created_from_origin_else_refused(self):
        self.refused("eq/tour-9-z", "tour-9", "a", "z")
        origin = os.path.join(self.d, "origin.git")
        git(self.root, "clone", "-q", "--bare", self.root, origin)
        git(self.root, "remote", "add", "origin", origin)
        git(self.root, "branch", "-D", "eq/tour-9-b")
        code, out, _ = run(self.root, GATE, "tour-9", "a", "b")
        self.assertEqual(code, 0, out)
        self.assertIn("Merge team b of tour-9 (audit ultra)", subjects(self.root))

    def test_preflight_not_ready(self):
        out = self.refused("PRÉ-VOL", "tour-9", "a", PS_TOY_PREVOL_RED="1")
        self.assertIn("RECALÉ  la zone b", out)


def normalized(text, root):
    text = text.replace(root, "<toy>")
    text = re.sub(r"/tmp/[A-Za-z0-9._/-]+", "<tmp>", text)
    return re.sub(r"\b[0-9a-f]{7,40}\b", "<sha>", text)


class PortalUnchangedWithoutBatch(unittest.TestCase):
    """Without PS_PAQUET_ETAPE the portal says and does what it did before this lot, byte for byte once the shas and
    the temporary folders are put aside."""

    def test_same_output_same_history(self):
        before = subprocess.run(["git", "-C", ROOT, "show", f"{PORTAL_BEFORE}:{PORTAL}"], capture_output=True, text=True)
        if before.returncode != 0:
            self.skipTest("the portal before this lot is not in this clone")
        with tempfile.TemporaryDirectory() as d:
            for culprit in (None, "b"):
                results = []
                for name, text in (("old", before.stdout), ("new", None)):
                    root = toy(d, f"{name}-{culprit}", portal_text=text, culprit=culprit)
                    code, out, said = run(root, PORTAL, "tour-9", "a", "b", "--", "*FooTest")
                    tree = git(root, "ls-tree", "-r", "--name-only", "HEAD")
                    results.append((code, normalized(out, root), said, subjects(root), tree))
                self.assertEqual(results[0], results[1], f"culprit {culprit}")


if __name__ == "__main__":
    unittest.main(verbosity=1)
