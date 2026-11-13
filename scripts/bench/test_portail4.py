#!/usr/bin/env python3
"""Tests of the bench lot « portail4 » (docs/audits/AUDIT-ULTRA.md §13.5, last point): no lot of the tutorial enters
before a fresh beginner replayed it ON THE LOT'S BRANCH. The portal refuses (exit 10, from batch 7) a lot that touches
the tutorial zone when its zone report does not carry, per control seat, the line

  controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0

read by scripts/bench/rapport-zone.py --tuto. Seconds, no Gradle, no network (toy repositories):

  python3 -I scripts/bench/test_portail4.py
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
REPORT = "docs/audits/ultra/tour-9-z-rapport.md"
BRANCH = "eq/tour-9-z"

PANEL = "app/src/main/java/p/tuto/TutoPanel.kt"
COURSE = "project-model/src/main/kotlin/m/tuto/TutoCourse.kt"
SCRIPT = "project-model/src/main/resources/tutos/house.json"
WORDS = "project-model/src/main/resources/i18n/fr/tuto_house.json"
ROLL = "app/src/main/java/p/synth/PianoRoll.kt"
RHYTHM = "app/src/main/java/p/rhythm/RhythmScreen.kt"
PLAIN = "app/src/main/java/p/words/PlainWords.kt"
SHEET = "app/src/main/java/p/track/TrackSheet.kt"
# AUDIT-ULTRA §13.3: the four files the tutorial shares with the rest of the application.
SHARED = (ROLL, RHYTHM, PLAIN, SHEET)
ACCENT = "project-model/src/main/resources/i18n/fr/tuto_café.json"
SPACE = "project-model/src/main/resources/tutos/deep house.json"
ZONE = (PANEL, COURSE, SCRIPT, WORDS)
# What is NOT the tutorial zone: its tests, the other texts, the reference screenshots, the bench, the docs.
OUTSIDE = ("app/src/test/java/p/tuto/TutoPanelTest.kt", "project-model/src/test/kotlin/m/tuto/TutoCourseTest.kt",
           "project-model/src/main/resources/i18n/fr/app.json", "app/screenshots/tuto_chapters.txt",
           "scripts/bench/outil.sh", "docs/tuto/notes.md", "app/src/main/java/p/synth/Keys.kt")


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def git(repo, *args, check=True):
    return subprocess.run(["git", "-C", repo, "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false", *args],
                          check=check, capture_output=True, text=True).stdout.strip()


class Toy(unittest.TestCase):
    """A toy repository: an integration branch « int » holding the tutorial's files, and the lot's branch."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="portail4-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.repo = os.path.join(self.tmp, "depot")
        os.makedirs(self.repo)
        git(self.repo, "init", "-q", "-b", "int")
        for path in ZONE + OUTSIDE + SHARED + (ACCENT, SPACE):
            write(self.path(path), "ligne 1\nligne 2\nligne 3\nligne 4\nligne 5\n")
        self.commit("base")
        git(self.repo, "checkout", "-q", "-b", BRANCH)

    def path(self, rel):
        return os.path.join(self.repo, rel)

    def commit(self, message):
        git(self.repo, "add", "-A")
        git(self.repo, "commit", "-q", "--allow-empty", "-m", message)
        return git(self.repo, "rev-parse", "HEAD")

    def change(self, path, message=None, line=0):
        """One commit of the current branch that changes a line of this file; its sha."""
        lines = read(self.path(path)).split("\n")
        lines[line] = lines[line] + " changée"
        write(self.path(path), "\n".join(lines))
        return self.commit(message or f"change {os.path.basename(path)}")

    def seat(self, sid, sha, head=None, chapters=("compris du premier coup", "hésitation")):
        """A control seat's report, as the bench starts it (« code testé » and « joué par » on top)."""
        text = (f"code testé : {sha}\njoué par : sous-agent neuf\n" if head is None else head)
        text += "siege: a | profil: t9-controle | lang: fr | device: phone | font: 100 | mode: simple\nmission: tuto-facile\n"
        text += "".join(f"chapitre: {i + 1} Un chapitre | ecran: {i + 3} | {state} | vu\n" for i, state in enumerate(chapters))
        write(self.path(f"docs/audits/ultra/sieges/tour-9/controle/{sid}/rapport.md"), text)
        return self.commit(f"control seat {sid}")

    def report(self, *lines):
        write(self.path(REPORT), "\n".join(("# Rapport", "", "tests: aucun : jouet") + lines) + "\n")
        return self.commit("zone report")

    def line(self, sid, sha, blocked=0, graves=0):
        return f"controle: {sid} | code testé: {sha} | bloqués: {blocked} | graves neufs: {graves}"

    def run_rule(self, *more):
        base = git(self.repo, "merge-base", "int", BRANCH)
        r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "rapport-zone.py"), "--tuto", BRANCH, REPORT,
                            f"base:{base}", f"tete:{git(self.repo, 'rev-parse', 'int')}", "tour:tour-9", *more],
                           cwd=self.repo, capture_output=True, text=True)
        return r.returncode, r.stdout + r.stderr

    def refused(self, *words):
        code, out = self.run_rule()
        self.assertEqual(code, 1, out)
        for word in words:
            self.assertIn(word, out)
        return out

    def accepted(self):
        code, out = self.run_rule()
        self.assertEqual(code, 0, out)
        return out

    def good_lot(self, path=PANEL):
        """A tutorial lot as it should come: the fix, the seat that played it, its line."""
        sha = self.change(path)
        self.seat("Ha", sha)
        self.report(self.line("Ha", sha[:10]))
        return sha


class Zone(Toy):
    """Which lots the rule looks at: those that change the application's tutorial (code and resources of src/main)."""

    def test_each_file_of_the_zone_asks_for_its_control_seat(self):
        for path in ZONE:
            git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
            self.change(path)
            self.report()
            out = self.refused("controle:", path)
            self.assertIn("no « controle: » line", out, path)

    def test_a_lot_outside_the_zone_is_left_alone_and_nothing_is_said(self):
        for path in OUTSIDE:
            self.change(path)
        self.report("Le lot ne touche pas au tuto.")
        self.assertEqual(self.accepted(), "")

    def test_a_lot_without_any_report_outside_the_zone_is_left_alone(self):
        self.change(OUTSIDE[0])
        self.assertEqual(self.accepted(), "")

    def test_the_piano_roll_counts_when_the_report_says_tuto(self):
        self.change(ROLL)
        self.report("La flèche du Tuto suit la note.")
        self.refused("no « controle: » line", ROLL)

    def test_the_piano_roll_alone_does_not_count(self):
        self.change(ROLL)
        self.report("Les notes gardent leur longueur ; rien d'institutionnel.")
        self.assertEqual(self.accepted(), "")

    def test_tuto_in_a_block_of_code_is_not_said(self):
        self.change(ROLL)
        self.report("Format :", "```", "controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0 (lots du tuto)", "```")
        self.assertEqual(self.accepted(), "")

    def test_a_file_that_differs_only_because_the_branch_merged_the_integration_is_not_the_lot_s(self):
        self.change(OUTSIDE[0])
        git(self.repo, "checkout", "-q", "int")
        self.change(PANEL, "another lot's tutorial change, already in the integration")
        git(self.repo, "checkout", "-q", BRANCH)
        git(self.repo, "merge", "-q", "--no-ff", "-m", "Merge the integration", "int")
        self.report()
        self.assertEqual(self.accepted(), "")

    def test_a_change_the_integration_already_holds_is_not_the_lot_s(self):
        # The same change entered the integration by another way after the branch left (the branch has not merged it
        # in): the file is in the branch's diff since its base, but the lot brings nothing the integration has not.
        self.change(PANEL)
        same = read(self.path(PANEL))
        git(self.repo, "checkout", "-q", "int")
        write(self.path(PANEL), same)
        self.commit("the same change of the tutorial, already in the integration")
        git(self.repo, "checkout", "-q", BRANCH)
        self.report()
        self.assertEqual(self.accepted(), "")

    def test_a_deleted_file_of_the_zone_counts(self):
        os.remove(self.path(SCRIPT))
        self.commit("a chapter taken away")
        self.report()
        self.refused("no « controle: » line", SCRIPT)

    def test_a_zone_lot_without_its_report_is_refused(self):
        self.change(PANEL)
        self.refused("no zone report")


class Line(Toy):
    """The line of the zone report: one per control seat, every one well-formed, 0 blocked and 0 new grave."""

    def test_a_good_lot_passes_and_says_its_seat(self):
        sha = self.good_lot()
        out = self.accepted()
        self.assertRegex(out, r"^tuto: ")
        self.assertIn("Ha", out)
        self.assertIn(sha[:7], out)

    def test_a_bullet_and_backquotes_are_allowed(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        self.report("- `" + self.line("Ha", sha) + "`")
        self.accepted()

    def test_a_blocked_chapter_is_refused_and_named(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        self.report(self.line("Ha", sha, blocked=2))
        self.refused("Ha", "bloqués: 2")

    def test_a_new_grave_is_refused_and_named(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        self.report(self.line("Ha", sha, graves=1))
        self.refused("Ha", "graves neufs: 1")

    def test_every_line_counts_not_only_the_first(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        self.seat("Da", sha)
        self.report(self.line("Ha", sha), self.line("Da", sha, graves=3))
        out = self.refused("Da", "graves neufs: 3")
        self.assertNotIn("« Ha »", out)

    def test_a_malformed_line_is_refused(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        for bad in (f"controle: Ha, joué sur {sha}, rien de bloqué",
                    f"controle: Ha | code testé: {sha} | bloqués: 0",
                    f"controle: Ha | code testé: {sha} | bloqués: aucun | graves neufs: 0",
                    f"controle: Ha | code testé: {sha[:6]} | bloqués: 0 | graves neufs: 0",
                    f"controle: ../../x | code testé: {sha} | bloqués: 0 | graves neufs: 0",
                    f"contrôle: | code testé: {sha} | bloqués: 0 | graves neufs: 0"):
            self.report(self.line("Ha", sha), bad)
            self.refused("is not « controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0 »")

    def test_a_line_in_a_block_of_code_is_not_a_line(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        self.report("```", self.line("Ha", sha), "```")
        self.refused("no « controle: » line")


class Commit(Toy):
    """« code testé »: a commit of the lot's branch, not older than the lot's last change of the tutorial."""

    def test_an_unknown_commit_is_refused(self):
        self.change(PANEL)
        ghost = "abcdef0123"
        self.seat("Ha", ghost)
        self.report(self.line("Ha", ghost))
        self.refused("Ha", ghost, "is not a commit")

    def test_a_commit_that_is_not_in_the_branch_is_refused(self):
        self.change(PANEL)
        git(self.repo, "checkout", "-q", "-b", "ailleurs")
        other = self.change(PANEL, "played elsewhere", line=2)
        git(self.repo, "checkout", "-q", BRANCH)
        self.seat("Ha", other)
        self.report(self.line("Ha", other))
        self.refused("Ha", other[:7], "is not an ancestor of")

    def test_a_seat_that_played_before_the_last_tutorial_change_does_not_count(self):
        first = self.change(PANEL)
        self.seat("Ha", first)
        last = self.change(COURSE, "the last fix of the tutorial")
        self.report(self.line("Ha", first))
        self.refused("Ha", first[:7], last[:7], "before the lot's last change of the tutorial")

    def test_commits_outside_the_zone_after_the_seat_change_nothing(self):
        sha = self.good_lot()
        self.change(OUTSIDE[0], "a test of the tutorial changed after the seat")
        self.change(OUTSIDE[4])
        out = self.accepted()
        self.assertIn(sha[:7], out)

    def test_a_seat_that_played_a_later_commit_counts(self):
        self.change(PANEL)
        later = self.change(OUTSIDE[4], "a bench commit after the fix")
        self.seat("Ha", later)
        self.report(self.line("Ha", later))
        self.accepted()

    def test_a_merge_that_changes_the_tutorial_after_the_seat_is_refused(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        git(self.repo, "checkout", "-q", "int")
        self.change(PANEL, "the integration changes the same file", line=4)
        git(self.repo, "checkout", "-q", BRANCH)
        git(self.repo, "merge", "-q", "--no-ff", "-m", "Merge the integration", "int")
        self.report(self.line("Ha", sha))
        self.refused("Ha", sha[:7], PANEL, "not the files the seat played")

    def test_the_piano_roll_of_a_tutorial_lot_is_tutorial_code_too(self):
        first = self.change(PANEL)
        self.seat("Ha", first)
        last = self.change(ROLL, "the arrow of the tuto in the piano roll")
        self.report("Le tuto montre la bonne note.", self.line("Ha", first))
        self.refused("Ha", last[:7], "before the lot's last change of the tutorial")


class Seat(Toy):
    """The line cannot be invented: the seat's report is in the branch and its head names the same commit."""

    def test_a_seat_without_its_report_in_the_branch_is_refused(self):
        sha = self.change(PANEL)
        self.report(self.line("Ha", sha))
        self.refused("Ha", "docs/audits/ultra/sieges/tour-9/controle/Ha/rapport.md", "no report")

    def test_a_seat_report_that_names_another_commit_is_refused(self):
        first = self.change(OUTSIDE[4])
        sha = self.change(PANEL)
        self.seat("Ha", first)
        self.report(self.line("Ha", sha))
        self.refused("Ha", first[:7], sha[:7], "names another commit")

    def test_a_seat_report_without_its_commit_on_top_is_refused(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha, head="")
        self.report(self.line("Ha", sha))
        self.refused("Ha", "no « code testé : <sha> » on top")

    def test_code_teste_lower_in_the_seat_report_is_not_its_head(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        path = self.path("docs/audits/ultra/sieges/tour-9/controle/Ha/rapport.md")
        lines = read(path).split("\n")
        write(path, "\n".join(lines[2:] + lines[:1]))
        self.commit("the commit named at the bottom")
        self.report(self.line("Ha", sha))
        self.refused("Ha", "no « code testé : <sha> » on top")

    def test_a_seat_of_another_round_is_not_this_round_s(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        git(self.repo, "mv", "docs/audits/ultra/sieges/tour-9", "docs/audits/ultra/sieges/tour-8")
        self.commit("the seat filed under another round")
        self.report(self.line("Ha", sha))
        self.refused("Ha", "no report")

    def test_a_seat_in_a_lot_folder_of_the_control_is_named_with_its_folder(self):
        sha = self.change(PANEL)
        self.seat("lotK/a", sha)
        self.report(self.line("lotK/a", sha))
        self.accepted()

    def test_a_seat_whose_own_report_says_a_chapter_blocked_is_refused(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha, chapters=("compris du premier coup", "bloqué : aucune note ne se pose"))
        self.report(self.line("Ha", sha))
        self.refused("Ha", "chapitre: 2", "bloqué")


class ReviewOfTheLot(Toy):
    """Fresh review of the lot (first pass, on e8503cbe). G1: the tutorial zone is §13.3's, with RhythmScreen.kt,
    PlainWords.kt and TrackSheet.kt beside PianoRoll.kt. G2: git quotes a file name that is not plain ASCII; the names
    are read unquoted. M3: « bloqué » is read in the real seats' format too. M4: three controls had no test. Minor:
    « ~~~ » blocks, and a « controle: » line written as a numbered item, a table row or in bold."""

    def run_plain(self, *args):
        r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "rapport-zone.py"), *args], cwd=self.repo, capture_output=True, text=True)
        return r.returncode, r.stdout + r.stderr

    # --- G1 ---
    def test_each_shared_file_counts_when_the_report_says_tuto(self):
        for path in SHARED:
            git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
            self.change(path)
            self.report("Le tuto ne perd plus sa flèche.")
            self.refused("no « controle: » line", path)

    def test_a_shared_file_alone_without_the_word_is_left_alone(self):
        for path in SHARED:
            self.change(path)
        self.report("Les écrans gardent leur titre.")
        self.assertEqual(self.accepted(), "")

    def test_a_later_fix_in_a_shared_file_is_a_change_of_the_tutorial_whatever_the_report_says(self):
        for words in ((), ("Le tuto montre la bonne case.",)):
            git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
            first = self.change(PANEL)
            self.seat("Ha", first)
            last = self.change(RHYTHM, "the tutorial fixed again, in the rhythm screen")
            self.report(self.line("Ha", first), *words)
            self.refused("Ha", first[:7], last[:7], "before the lot's last change of the tutorial")

    def test_each_shared_file_changed_after_the_seat_refuses_a_tutorial_lot(self):
        for path in SHARED:
            git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
            first = self.change(COURSE)
            self.seat("Ha", first)
            last = self.change(path)
            self.report(self.line("Ha", first))
            self.refused("Ha", last[:7], "before the lot's last change of the tutorial")

    def test_a_shared_file_a_merge_changes_after_the_seat_refuses_a_tutorial_lot(self):
        self.change(PANEL)
        sha = self.change(SHEET)
        self.seat("Ha", sha)
        git(self.repo, "checkout", "-q", "int")
        self.change(SHEET, "the integration changes the same shared file", line=4)
        git(self.repo, "checkout", "-q", BRANCH)
        git(self.repo, "merge", "-q", "--no-ff", "-m", "Merge the integration", "int")
        self.report(self.line("Ha", sha))
        self.refused("Ha", SHEET, "not the files the seat played")

    def test_a_seat_that_played_after_the_shared_file_s_fix_counts(self):
        self.change(PANEL)
        last = self.change(RHYTHM)
        self.seat("Ha", last)
        self.report(self.line("Ha", last))
        self.assertIn(RHYTHM, self.accepted())

    # --- G2 ---
    def test_a_file_name_with_an_accent_is_in_the_zone(self):
        self.change(ACCENT)
        self.report()
        self.refused("no « controle: » line", ACCENT)

    def test_a_new_file_with_an_accent_is_in_the_zone(self):
        write(self.path("project-model/src/main/resources/i18n/fr/tuto_éveil.json"), "{}\n")
        self.commit("a new chapter's words")
        self.report()
        self.refused("no « controle: » line", "tuto_éveil.json")

    def test_a_file_name_with_a_space_is_in_the_zone(self):
        self.change(SPACE)
        self.report()
        self.refused("no « controle: » line", SPACE)

    def test_a_seat_that_played_before_an_accented_file_changed_does_not_count(self):
        first = self.change(PANEL)
        self.seat("Ha", first)
        last = self.change(ACCENT)
        self.report(self.line("Ha", first))
        self.refused("Ha", last[:7], "before the lot's last change of the tutorial")

    def test_an_accented_or_spaced_file_a_merge_changes_after_the_seat_is_named(self):
        for path in (ACCENT, SPACE):
            git(self.repo, "checkout", "-q", "int")
            git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
            sha = self.change(path)
            self.seat("Ha", sha)
            git(self.repo, "checkout", "-q", "int")
            self.change(path, "the integration changes the same file", line=4)
            git(self.repo, "checkout", "-q", BRANCH)
            git(self.repo, "merge", "-q", "--no-ff", "-m", "Merge the integration", "int")
            self.report(self.line("Ha", sha))
            self.refused("Ha", path, "not the files the seat played")

    def test_a_good_lot_on_an_accented_file_passes(self):
        sha = self.good_lot(ACCENT)
        self.assertIn(ACCENT, self.accepted())
        self.assertIn(sha[:7], self.accepted())

    def test_the_plain_mode_finds_a_test_in_a_file_with_an_accent(self):
        write(self.path("app/src/test/java/t/CaféTest.kt"), "package t\n\nimport org.junit.Test\n\nclass CafeTest {\n    @Test\n    fun works() {}\n}\n")
        self.commit("a test in a file with an accent")
        self.report("- chemin: Accueil | test: CafeTest#works")
        code, out = self.run_plain(BRANCH, REPORT)
        self.assertEqual(code, 0, out)

    # --- M3 ---
    def seat_says(self, *lines):
        sha = self.change(PANEL)
        self.seat("Da", sha)
        path = self.path("docs/audits/ultra/sieges/tour-9/controle/Da/rapport.md")
        write(path, read(path) + "chapitres:\n" + "".join(l + "\n" for l in lines))
        self.commit("the seat's chapters, as the seats of tour 6 wrote them")
        self.report(self.line("Da", sha))

    def test_blocked_is_read_in_the_real_seats_format(self):
        self.seat_says("  - chapitre 4 « ハイハット » : hésitation (nom latin)", "  - chapitre 5 « ベース » : bloqué (note restante introuvable, 14 gestes de détour)")
        self.refused("Da", "chapitre 5", "bloqué")

    def test_a_chapter_that_only_mentions_the_word_is_not_blocked(self):
        self.seat_says("  - chapitre 4 « ハイハット » : hésitation (j'ai cru le pad bloqué)", "  - chapitre 5 « ベース » : compris du premier coup")
        self.accepted()

    # --- M4 ---
    def test_a_tutorial_file_renamed_out_of_the_zone_counts(self):
        os.makedirs(self.path("project-model/src/main/resources/autres"))
        git(self.repo, "mv", SCRIPT, "project-model/src/main/resources/autres/house.json")
        self.commit("a chapter moved out of tutos/")
        self.report()
        self.refused("no « controle: » line", SCRIPT)

    def test_two_dots_in_the_seat_s_name_are_refused(self):
        sha = self.change(PANEL)
        self.seat("lot..K", sha)
        self.report(self.line("lot..K", sha))
        self.refused("is not « controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0 »")

    def test_without_its_three_options_the_rule_answers_2_never_0(self):
        self.good_lot()
        base, head = git(self.repo, "merge-base", "int", BRANCH), git(self.repo, "rev-parse", "int")
        options = (f"base:{base}", f"tete:{head}", "tour:tour-9")
        code, out = self.run_plain("--tuto", BRANCH, REPORT, *options)
        self.assertEqual(code, 0, out)
        for missing in options:
            code, out = self.run_plain("--tuto", BRANCH, REPORT, *(o for o in options if o != missing))
            self.assertEqual(code, 2, f"without {missing.split(':')[0]}: {out}")
            self.assertIn("are needed", out)

    # --- minor ---
    def test_a_tilde_block_shows_a_format_too(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        self.report("~~~", self.line("Ha", sha), "~~~")
        self.refused("no « controle: » line")

    def test_tuto_in_a_tilde_block_is_not_said(self):
        self.change(ROLL)
        self.report("~~~", "controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0 (lots du tuto)", "~~~")
        self.assertEqual(self.accepted(), "")

    def shapes(self, line):
        return ("1. " + line, "2) " + line, "| " + line + " |", "**" + line + "**", line.replace("controle:", "**controle:**"),
                "> " + line, "+ " + line, "- [x] " + line, "*" + line + "*")

    def test_a_line_as_a_numbered_item_a_table_row_or_in_bold_is_read(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        for line in self.shapes(self.line("Ha", sha)):
            self.report(line)
            self.assertIn("Ha@", self.accepted(), line)

    def test_a_bad_line_in_any_shape_is_never_ignored_next_to_a_good_one(self):
        sha = self.change(PANEL)
        self.seat("Ha", sha)
        self.seat("Da", sha)
        for line in self.shapes(self.line("Da", sha, blocked=1)):
            self.report(self.line("Ha", sha), line)
            code, out = self.run_rule()
            self.assertEqual(code, 1, line + "\n" + out)
            self.assertIn("Da", out, line)


class Portal(unittest.TestCase):
    """The rule is in the portal with its batch guard, and the pre-flight repeats it."""

    def test_the_portal_refuses_with_exit_10_from_batch_7(self):
        portal = read(os.path.join(HERE, "fusion-equipes.sh"))
        lines = [l for l in portal.splitlines() if "rapport-zone.py --tuto" in l]
        self.assertEqual(len(lines), 1, "one line of the portal calls rapport-zone.py --tuto")
        self.assertIn("from_paquet 7", lines[0])
        self.assertIn("exit 10", lines[0])
        self.assertIn("not merged", lines[0])
        self.assertLess(portal.index("rapport-zone.py --tuto"), portal.index("if ! git merge --no-ff -m"), "refused BEFORE the merge")

    def test_the_preflight_repeats_it_from_batch_7(self):
        pre_vol = read(os.path.join(HERE, "pre-vol.sh"))
        self.assertIn("rapport-zone.py --tuto", pre_vol)
        self.assertRegex(pre_vol, r"rule_from 7 ")

    def test_the_preflight_knows_the_portal_s_rules(self):
        pre_vol = read(os.path.join(HERE, "pre-vol.sh"))
        known = re.search(r'^KNOWN_RULES="(\d+) ([0-9a-f]{10})"', pre_vol, re.M)
        self.assertIsNotNone(known)
        r = subprocess.run(["bash", "-c", "grep -E 'not merged|undone' \"$0\" | grep -c .; grep -E 'not merged|undone' \"$0\" | sed 's/^ *//' | sha256sum | cut -c1-10",
                            os.path.join(HERE, "fusion-equipes.sh")], capture_output=True, text=True, check=True)
        self.assertEqual(" ".join(r.stdout.split()), f"{known.group(1)} {known.group(2)}",
                         "pre-vol.sh's KNOWN_RULES is not the portal's rules: update the pre-flight with the portal")
        self.assertEqual(known.group(1), "17", "the tutorial rule is the portal's 17th refusal")


def real(rev):
    r = subprocess.run(["git", "-C", ROOT, "rev-parse", "-q", "--verify", rev + "^{commit}"], capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else None


class RealLots(unittest.TestCase):
    """Read only, on the real merges of tour 6 (skipped where the history is not there): the rule sees the tutorial
    lots of the night of the 12th, which entered without a control seat, and leaves the others alone."""

    def rule(self, merge, zone):
        if not real(merge) or not real(merge + "^2"):
            self.skipTest(f"the merge {merge} is not in this history")
        base = subprocess.run(["git", "-C", ROOT, "merge-base", merge + "^1", merge + "^2"], capture_output=True, text=True).stdout.strip()
        r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "rapport-zone.py"), "--tuto", merge + "^2",
                            f"docs/audits/ultra/tour-6-{zone}-rapport.md", f"base:{base}", f"tete:{real(merge + '^1')}", "tour:tour-6"],
                           cwd=ROOT, capture_output=True, text=True)
        return r.returncode, r.stdout + r.stderr

    def test_the_tutorial_lots_of_tour_6_would_have_been_asked_for_their_seat(self):
        for merge, zone in (("35645c88", "tutofleches"), ("0b8ba680", "tutoconsignes"), ("1980dff6", "tutogrille"), ("ea294587", "tutogrille"),
                            # (Review of the lot, G1: « emulateur » repaired PlainWords.kt, « le panneau du tuto » in its report.)
                            ("2c146a42", "reparations"), ("911c6195", "emulateur")):
            code, out = self.rule(merge, zone)
            self.assertEqual(code, 1, f"{zone}: {out}")
            self.assertIn("no « controle: » line", out, zone)

    def test_the_bench_lots_of_tour_6_are_left_alone(self):
        for merge, zone in (("42f608f6", "portail3"), ("c40b834e", "banc7"), ("d4958947", "guidecoquille"), ("9b106bd5", "cablage")):
            code, out = self.rule(merge, zone)
            self.assertEqual((code, out), (0, ""), zone)


class Hooks(unittest.TestCase):
    """This file is run where the bench's other script tests are: the self-test and pre-vol.sh --ci."""

    def test_the_preflight_and_the_selftest_run_this_file(self):
        self.assertTrue("scripts/bench/test_portail4.py" in read(os.path.join(HERE, "pre-vol.sh")), "pre-vol --ci must run this test")
        self.assertTrue("scripts/bench/test_portail4.py" in read(os.path.join(HERE, "selftest.sh")), "the self-test must run this test")


if __name__ == "__main__":
    unittest.main()
