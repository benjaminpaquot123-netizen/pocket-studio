#!/usr/bin/env python3
"""Tests of the bench's base (« socle du banc », docs/PLAN-SESSIONS-PARALLELES.md, discussion A).

Seconds, no Gradle:  python3 -I scripts/bench/test_socle.py
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile
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


def git(repo, *args, check=True):
    return subprocess.run(["git", "-C", repo, "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false", *args],
                          check=check, capture_output=True, text=True)


JOURNAL = "docs/audits/ultra/banc-fiable-journal.md"


class SharedFilesMergeByUnion(unittest.TestCase):
    """Point 2: two lots that each add lines at the end of DECISIONS.md merge without a conflict."""

    def test_a_file_edited_in_place_is_never_merged_by_union(self):
        # Review of the lot (moyen 1): the journal's checked lines get their « preuve: » later, in place; union would keep
        # the old line and the new one, and the judge would read the old one « sans preuve », with no conflict said.
        attributes = read(".gitattributes")
        for f in (JOURNAL, "docs/audits/ultra/verdicts.tsv", "docs/audits/ultra/PLAN-RESTE.md"):
            self.assertNotRegex(attributes, re.escape(f) + r"\s+merge=union", f"{f} is edited in place")

    def test_two_lots_append_to_decisions(self):
        with tempfile.TemporaryDirectory() as repo:
            git(repo, "init", "-q", "-b", "fx-int")
            shutil.copy(os.path.join(ROOT, ".gitattributes"), os.path.join(repo, ".gitattributes"))
            for f in ("docs/DECISIONS.md",):
                os.makedirs(os.path.join(repo, os.path.dirname(f)), exist_ok=True)
                with open(os.path.join(repo, f), "w", encoding="utf-8") as out:
                    out.write("# titre\n- [x] 1.1 une ligne\n")
            git(repo, "add", "-A"); git(repo, "commit", "-q", "-m", "base")
            for zone in ("z1", "z2"):
                git(repo, "checkout", "-q", "-b", zone, "fx-int")
                for f in ("docs/DECISIONS.md",):
                    with open(os.path.join(repo, f), "a", encoding="utf-8") as out:
                        out.write(f"- [x] 9.9 la ligne de {zone}\n")
                git(repo, "commit", "-q", "-am", zone)
            git(repo, "checkout", "-q", "fx-int")
            git(repo, "merge", "-q", "--no-ff", "-m", "z1", "z1")
            merged = git(repo, "merge", "-q", "--no-ff", "-m", "z2", "z2", check=False)
            self.assertEqual(merged.returncode, 0, "conflict: " + merged.stdout + merged.stderr)
            for f in ("docs/DECISIONS.md",):
                with open(os.path.join(repo, f), encoding="utf-8") as got:
                    text = got.read()
                self.assertIn("la ligne de z1", text)
                self.assertIn("la ligne de z2", text)


class DecisionsIndex(unittest.TestCase):
    """Point 2: the portal lists a lot's decisions file in docs/DECISIONS.md, once, after the merge."""

    def run_index(self, root, *args):
        return subprocess.run(["bash", os.path.join(ROOT, "scripts", "bench", "decisions-index.sh"), *args],
                              cwd=root, capture_output=True, text=True)

    def test_listed_once_with_its_count(self):
        with tempfile.TemporaryDirectory() as root:
            os.makedirs(os.path.join(root, "docs", "decisions"))
            with open(os.path.join(root, "docs", "DECISIONS.md"), "w", encoding="utf-8") as f:
                f.write("# Décisions\n- 2026-11-11 · une décision\n")
            with open(os.path.join(root, "docs", "decisions", "tour-9-z.md"), "w", encoding="utf-8") as f:
                f.write("# Décisions du lot\n- 2026-11-11 · un choix\n- 2026-11-11 · un autre\n")
            for _ in range(2):
                r = self.run_index(root, "tour-9", "z")
                self.assertEqual(r.returncode, 0, r.stderr)
            with open(os.path.join(root, "docs", "DECISIONS.md"), encoding="utf-8") as f:
                text = f.read()
            self.assertEqual(text.count("docs/decisions/tour-9-z.md"), 1, text)
            self.assertIn("2 décision(s)", text)
            self.assertIn("lot « z » (tour-9)", text)

    def test_nothing_without_a_decisions_file(self):
        with tempfile.TemporaryDirectory() as root:
            os.makedirs(os.path.join(root, "docs"))
            with open(os.path.join(root, "docs", "DECISIONS.md"), "w", encoding="utf-8") as f:
                f.write("# Décisions\n")
            r = self.run_index(root, "tour-9", "sans")
            self.assertEqual(r.returncode, 0, r.stderr)
            with open(os.path.join(root, "docs", "DECISIONS.md"), encoding="utf-8") as f:
                self.assertEqual(f.read(), "# Décisions\n")


PORTAL = "scripts/bench/fusion-equipes.sh"
PAQUET = "scripts/bench/portail-paquet.txt"


def rules(text):
    """The portal's refusals: the lines that say « not merged » or « undone »."""
    return {line.strip() for line in text.splitlines() if re.search(r"not merged|undone", line)}


class PortalRulesFrozenPerBatch(unittest.TestCase):
    """Point 5: a rule added to the portal while a batch is open only applies from the next batch."""

    def paquet(self, text):
        m = re.search(r"^paquet:\s*(\d+)\s*$", text, re.M)
        self.assertIsNotNone(m, f"{PAQUET} has no « paquet: N » line")
        return int(m.group(1))

    def test_the_portal_reads_the_batch(self):
        portal = read(*PORTAL.split("/"))
        self.assertIn(PAQUET, portal)
        self.assertRegex(portal, r"from_paquet\(\)")

    def test_the_portal_starts_without_a_batch_file(self):
        # Under « set -euo pipefail » a failing read of the batch ended the portal silently (code 2, no word).
        with tempfile.TemporaryDirectory() as repo:
            os.makedirs(os.path.join(repo, "scripts", "bench"))
            shutil.copy(os.path.join(ROOT, *PORTAL.split("/")), os.path.join(repo, *PORTAL.split("/")))
            git(repo, "init", "-q", "-b", "fx-int")
            git(repo, "add", "-A"); git(repo, "commit", "-q", "-m", "base")
            r = subprocess.run(["bash", PORTAL, "tour-9"], cwd=repo, capture_output=True, text=True)
            self.assertEqual(r.returncode, 0, r.stdout + r.stderr)

    def test_a_rule_added_during_the_batch_waits_for_the_next(self):
        current = self.paquet(read(*PAQUET.split("/")))
        opened = git(ROOT, "log", "-1", "--format=%H", "--", PAQUET).stdout.strip()
        if not opened:
            self.skipTest("no history of the batch file here (shallow clone)")
        then = git(ROOT, "show", f"{opened}:{PORTAL}", check=False).stdout
        for line in sorted(rules(read(*PORTAL.split("/"))) - rules(then)):
            m = re.search(r"from_paquet (\d+)", line)
            self.assertTrue(m and int(m.group(1)) > current,
                            f"rule added during batch {current} without « from_paquet {current + 1} »: {line[:140]}")


REJEUX = os.path.join(ROOT, "docs", "audits", "ultra", "rejeux")
TOUCHES = ("touche", "appui-long", "double-touche")
TAG = r"[A-Za-z_][A-Za-z0-9_:\-]*\*?"


class ReplaysAimByTag(unittest.TestCase):
    """Point 3: a replay touches a button by its test tag (« touche <étiquette> »), never by its pixels: a change of
    layout no longer breaks it (t4-26 was re-aimed twice on 2026-11-11 for that)."""

    def test_every_touch_of_a_replay_names_a_tag(self):
        ids = sorted(d for d in os.listdir(REJEUX) if not d.startswith("_") and os.path.isdir(os.path.join(REJEUX, d)))
        self.assertTrue(ids, "no replay in docs/audits/ultra/rejeux/")
        for rid in ids:
            for name in sorted(os.listdir(os.path.join(REJEUX, rid))):
                if not re.fullmatch(r"cmd-\d+\.txt", name):
                    continue
                with open(os.path.join(REJEUX, rid, name), encoding="utf-8") as f:
                    words = f.read().split()
                if words and words[0].lower() in TOUCHES:
                    self.assertTrue(len(words) == 2 and re.fullmatch(TAG, words[1]),
                                    f"{rid}/{name}: « {' '.join(words)} » aims by pixels, not by a tag")
                if words and words[0].lower() == "glisse" and len(words) == 4 and not re.fullmatch(r"-?\d+", words[1]):
                    self.assertTrue(re.fullmatch(TAG, words[1]) and all(re.fullmatch(r"-?\d+", w) for w in words[2:]),
                                    f"{rid}/{name}: « {' '.join(words)} » is a swipe by tag written wrong")

    def test_the_tester_resolves_a_tag(self):
        tester = read("app", "src", "test", "java", "com", "pocketstudio", "app", "human", "HumanTester.kt")
        self.assertIn("étiquette introuvable", tester, "HumanTester does not resolve « touche <étiquette> »")


class ReplayRunKeepsItsProof(unittest.TestCase):
    """Tour 5 (rejeux6): rejeu-defauts.sh keeps the journal of every run, and a « tenu » needs the last tagged touch to be in it."""

    def run_replay(self, journal, last_cmd="touche record_stop"):
        tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, tmp, True)
        os.makedirs(os.path.join(tmp, "scripts", "bench"))
        shutil.copy(os.path.join(ROOT, "scripts", "bench", "rejeu-defauts.sh"), os.path.join(tmp, "scripts", "bench"))
        with open(os.path.join(tmp, "scripts", "bench", "selftest.sh"), "w") as f:
            f.write("#!/usr/bin/env bash\nexit 0\n")
        with open(os.path.join(tmp, "scripts", "human-tester.sh"), "w") as f:
            f.write("#!/usr/bin/env bash\nfor s in \"$1\"/[a-f]; do cp \"$FAKE_JOURNAL\" \"$s/journal.txt\"; echo 'oracle tenu' > \"$s/rejeu.txt\"; done\n")
        for n in ("scripts/bench/selftest.sh", "scripts/human-tester.sh"):
            os.chmod(os.path.join(tmp, n), 0o755)
        rej = os.path.join(tmp, "rejeux", "t9-x")
        os.makedirs(rej)
        for name, text in (("start.txt", "lang=fr\n"), ("oracle.txt", "rejeu.n=2\nrejeu.attendu=x\n"), ("cmd-1.txt", "touche a_b"), ("cmd-2.txt", last_cmd)):
            with open(os.path.join(rej, name), "w") as f:
                f.write(text)
        jf = os.path.join(tmp, "journal.fake")
        with open(jf, "w") as f:
            f.write(journal)
        r = subprocess.run([os.path.join(tmp, "scripts", "bench", "rejeu-defauts.sh"), "t9-x"], capture_output=True, text=True,
                           env=dict(os.environ, PS_REJEUX_DIR=os.path.join(tmp, "rejeux"), FAKE_JOURNAL=jf))
        return r, os.path.join(tmp, "rejeux", "_journaux", "t9-x")

    def test_tenu_with_the_tag_touched_is_kept_with_its_journal(self):
        r, kept = self.run_replay("#1 ETIQUETTE a_b : 1 2\n#2 ETIQUETTE record_stop : 3 4\n")
        self.assertIn("t9-x : oracle tenu", r.stdout)
        self.assertEqual(r.returncode, 0)
        self.assertTrue(os.path.isfile(os.path.join(kept, "journal.txt")), "the journal of a held replay is not kept")

    def test_tenu_without_the_tag_touched_is_not_tenu(self):
        r, kept = self.run_replay("#1 ETIQUETTE a_b : 1 2\n#2 > touche 3 4\n")
        self.assertIn("oracle non tenu", r.stdout)
        self.assertEqual(r.returncode, 1)
        self.assertTrue(os.path.isfile(os.path.join(kept, "journal.txt")))

    def test_a_swipe_by_tag_needs_its_proof_too(self):
        r, _ = self.run_replay("#1 ETIQUETTE a_b : 1 2\n", last_cmd="glisse share_go 0 -500")
        self.assertIn("oracle non tenu", r.stdout)
        self.assertEqual(r.returncode, 1)

    def test_tenu_with_the_touch_refused_is_not_tenu(self):
        r, _ = self.run_replay("#2 ETIQUETTE record_stop : 3 4\n#2 REFUS hors de portée du pouce (d'une main, la gauche)\n")
        self.assertIn("refusée", r.stdout)
        self.assertEqual(r.returncode, 1)


class NewReplaysAimByTag(unittest.TestCase):
    """Tour 5 (rejeux6): rejeu-nouveau.sh no longer copies a seat's pixels: its touches become tags read from the seat's journal."""

    JOURNAL = ("#1 > touche 270 560\n#1 DOIGT welcome_guided « Guide »\n#2 > glisse 1 2 3 4\n#2 DOIGT rien\n"
               "#3 > touche 5 6\n#3 DOIGT project_p-896025b4 « Techno »\n#4 > touche 7 8\n#4 DOIGT vue « Drive »\n"
               "#5 > touche 9 9\n#5 REFUS hors de portée du pouce (d'une main, la gauche)\n")

    def seat(self, cmds):
        tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, tmp, True)
        seat = os.path.join(tmp, "seat")
        os.makedirs(seat)
        with open(os.path.join(seat, "journal.txt"), "w", encoding="utf-8") as f:
            f.write(self.JOURNAL)
        with open(os.path.join(seat, "start.txt"), "w") as f:
            f.write("lang=fr\n")
        for i, c in enumerate(cmds, 1):
            with open(os.path.join(seat, f"cmd-{i}.txt"), "w") as f:
                f.write(c)
        return tmp, seat

    def run_new(self, tmp, seat, options=""):
        rej = os.path.join(tmp, "rejeux")
        shutil.copytree(os.path.join(ROOT, "scripts"), os.path.join(tmp, "scripts"), ignore=shutil.ignore_patterns("__pycache__"))
        return subprocess.run([os.path.join(tmp, "scripts", "bench", "rejeu-nouveau.sh"), seat, "5", "t9-x", "attendu=x"],
                              capture_output=True, text=True, env=dict(os.environ, PS_REJEUX_DIR=rej, PS_REJEU_OPTIONS=options)), rej

    def test_touches_become_tags_and_scrolls_wait(self):
        tmp, seat = self.seat(["touche 270 560", "glisse 1 2 3 4", "touche 5 6", "touche 7 8", "touche 9 9"])
        r, rej = self.run_new(tmp, seat, "--cible 4=vue_Drive --cible 5=mode_card_simple")
        self.assertEqual(r.returncode, 0, r.stderr)
        got = [open(os.path.join(rej, "t9-x", f"cmd-{i}.txt")).read() for i in range(1, 6)]
        self.assertEqual(got, ["touche welcome_guided", "attends 1", "touche project_p-*", "touche vue_Drive", "touche mode_card_simple"])

    def test_a_command_can_stand_for_an_untagged_button(self):
        tmp, seat = self.seat(["touche 270 560", "glisse 1 2 3 4", "touche 5 6", "touche 7 8", "touche 9 9"])
        r, rej = self.run_new(tmp, seat, "--cible 4=vue_Drive --commande 5=retour --glisse 2=share_go:0:-500")
        self.assertEqual(r.returncode, 0, r.stderr)
        got = [open(os.path.join(rej, "t9-x", f"cmd-{i}.txt")).read() for i in (2, 5)]
        self.assertEqual(got, ["glisse share_go 0 -500", "retour"])

    def test_a_touch_with_no_tag_under_the_finger_stops_the_copy(self):
        tmp, seat = self.seat(["touche 270 560", "glisse 1 2 3 4", "touche 5 6", "touche 7 8", "touche 9 9"])
        r, rej = self.run_new(tmp, seat)
        self.assertEqual(r.returncode, 2)
        self.assertIn("give --cible 4", r.stderr)
        self.assertFalse(os.path.exists(os.path.join(rej, "t9-x")), "a half-made replay was left behind")

    def test_the_format_allows_a_swipe_by_tag_and_a_prefix(self):
        tester = read("app", "src", "test", "java", "com", "pocketstudio", "app", "human", "HumanTester.kt")
        self.assertIn("swipeByTag", tester)
        self.assertIn('tag.endsWith("*")', tester)
        self.assertIn('startsWith("vue_")', tester)
        self.assertIn("reachableNear", tester, "a tag touch is refused where the thumb could reach the control's edge")
        self.assertIn("nearestReachable", tester)


class StableScreenshots(unittest.TestCase):
    """Point 1: the five « unstable » images are made stable for good (docs/audits/ultra/tour-5-socle-rapport.md)."""

    def test_the_recette_image_reads_a_frozen_list(self):
        shot = read("app", "src", "test", "java", "com", "pocketstudio", "app", "ScreenshotTest.kt")
        self.assertNotIn('File("src/main/assets/recette.json")', shot, "the image follows the milestone's list again")
        self.assertNotIn('File("src/main/assets/changelog.txt")', shot)

    def test_the_guide_is_waited_for_before_the_captures_that_show_it(self):
        for name in ("PadHoldAndTrackDeleteTest.kt", "RemixSongScreenshotTest.kt"):
            text = read("app", "src", "test", "java", "com", "pocketstudio", "app", "nav", name)
            for m in re.finditer(r"capture(?:Screen)?RoboImage\(\s*\"screenshots/(nav/rhythm_scrolled_under_part|remix_stem_edit_menu)", text):
                self.assertIn("h.awaitGuide()", text[:m.start()].rsplit("@Test", 1)[-1], f"{m.group(1)}: the Guide not waited for")


if __name__ == "__main__":
    unittest.main(verbosity=2)
