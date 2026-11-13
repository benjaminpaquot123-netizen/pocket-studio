#!/usr/bin/env python3
"""Tests of the bench lot « constate » (docs/audits/ultra/PLAN-REPARATIONS-CONSTATEES.md § 4): a repair is « done »
only when a tester saw it. Seconds, no Gradle, no network (toy repositories and folders):

  python3 -I scripts/bench/test_constate.py

1. corrections.tsv, the one register: four states (« pas pris », « tenté », « rejoué », « constaté »), an acceptance
   sentence and the seat of origin; the lines of the rounds before keep their text; the programs that read it go on.
2. The portal (scripts/bench/fusion-equipes.sh, from batch 5): refuses a lot that gives itself « corrigé », « rejoué » or
   « constaté » (read against the base and the integration's head, never in the branch alone), and sets « rejoué »
   itself when the proof holds after the merge. The pre-flight says the same (« À VOIR » before batch 5).
"""
import importlib.util
import io
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stdout

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
REGISTER = os.path.join(ROOT, "docs", "audits", "ultra", "corrections.tsv")
# The integration's head this lot left from: the register's lines of rounds 1 to 5 as they were.
BEFORE = "2e29fdd759046551b7fb48b6dcd4612ebd9cd6d6"
HEADER = "tour\titem\tsiege\tecran\tetat\tcommit\ttest\tnote\tcible\tphrase_acceptation\tsiege_origine\tconstat"


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def read(path):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""


def git(repo, *args, check=True):
    return subprocess.run(["git", "-C", repo, "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false", *args],
                          check=check, capture_output=True, text=True)


def script(name, *args, cwd=None, env=None):
    r = subprocess.run([sys.executable, "-I", os.path.join(HERE, name), *args], cwd=cwd or ROOT, capture_output=True,
                       text=True, env=dict(os.environ, **(env or {})))
    return r.returncode, r.stdout + r.stderr


def load(name):
    """A script of scripts/bench/ as a module (its name has a dash), or None when it is not there."""
    path = os.path.join(HERE, name)
    if not os.path.isfile(path):
        return None
    spec = importlib.util.spec_from_file_location(name.replace("-", "_").replace(".py", ""), path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def row(*cells):
    return "\t".join(cells)


# A seat of control's report, the three forms of round 6 (controle/Ba, controle/Ba-3, controle/Ha-4) and the template's.
CONTROL_REPORT = """code testé : 10830ce2
joué par : sous-agent neuf

siege: a | profil: t6-Ba gaucher-espagnol-une-main | lang: es | device: phone | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
defauts-avant:
  - A1 : corrigé
  - A2 | corrigé (écrans 15-18 : Resto, Batería, Bajo, Voz touchés au pouce gauche, sans glisser)
  - A3 | autre : la phrase d'aide reste sous le flottant
  - A4 : encore là
  - T4 | corrigé (écran 98) : « Encore 2 notes en pointillé, plus haut ↑ »
  - A6 | pas atteint | ecran: 30 | le budget s'est fini avant
  - A7 | corrigé | ecran: 21 | la phrase touchée, « 1 trozo elegido »
hesitations:
  - ecran: 6 | cherchais: A9 | regarde: corrigé, écran 3
incidents-banc:
  - A5 : corrigé (écran 44), une ligne hors de la rubrique
abandon: jamais
"""


class Registry(unittest.TestCase):
    """Point 1: the register's four states, its two columns, its old lines; and who reads it."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="constate-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.reg = os.path.join(self.tmp, "corrections.tsv")
        self.old = row("tour-5", "T5-01", "A/a", "12", "corrigé", "abc1234", "FooTest", "une note ancienne", "")
        write(self.reg, "\n".join([
            HEADER,
            self.old,
            row("tour-6", "A2", "B/a", "15", "tenté", "1111111", "rejeu-a2", "", "", "en espagnol, main gauche : je touche « Voz »", "tour-6/lotB/a", ""),
            row("tour-6", "A2", "B/c", "40", "rejoué", "1111111", "rejeu-a2", "", "", "en espagnol, main gauche : je touche « Voz »", "tour-6/lotB/a", ""),
            row("tour-6", "A3", "B/a", "8", "tenté", "2222222", "-", "", "", "la phrase d'aide entière", "tour-6/lotB/a", ""),
            row("tour-6", "A4", "B/a", "9", "tenté", "-", "-", "", "", "", "tour-6/lotB/a", ""),
            row("tour-6", "A1", "B/a", "3", "tenté", "-", "-", "", "", "", "tour-6/lotB/a", ""),
            row("tour-6", "A6", "B/a", "30", "tenté", "-", "-", "", "", "", "tour-6/lotB/a", ""),
            row("tour-6", "A8", "B/a", "31", "pas pris", "-", "-", "", "", "", "tour-6/lotB/a", ""),
            row("tour-6", "A7", "B/a", "21", "tenté", "3333333", "-", "", "", "", "tour-6/lotB/a", ""),
            row("tour-6", "T4", "H/a", "98", "rejoué", "4444444", "rejeu-t4", "", "", "", "tour-6/lotH/a", ""),
            row("tour-6", "A5", "B/a", "44", "Tente", "5555555", "-", "", "", "", "tour-6/lotB/a", ""),
        ]) + "\n")
        self.seat = os.path.join(self.tmp, "docs", "audits", "ultra", "sieges", "tour-6", "controle", "Ba-3")
        write(os.path.join(self.seat, "rapport.md"), CONTROL_REPORT)

    def constate(self, item, tour="tour-6"):
        return script("reparations.py", "--constate", tour, item, self.seat, "--registre", self.reg, "--racine", self.tmp)

    def rows(self):
        lines = read(self.reg).splitlines()
        head = lines[0].split("\t")
        return [dict(zip(head, l.split("\t"))) for l in lines[1:] if l]

    # --- the real register -------------------------------------------------------------------------------------------

    def test_the_real_register_has_the_two_columns_and_the_register_of_the_chief(self):
        first = read(REGISTER).splitlines()[:1]
        self.assertEqual(first, [HEADER], "the register's header: the old columns, then « phrase_acceptation », "
                                          "« siege_origine » and « constat »")

    def test_the_lines_of_the_rounds_before_are_not_rewritten(self):
        if git(ROOT, "cat-file", "-e", BEFORE + "^{commit}", check=False).returncode != 0:
            self.skipTest("the history of the register is not here (shallow clone)")
        then = git(ROOT, "show", f"{BEFORE}:docs/audits/ultra/corrections.tsv").stdout.splitlines()
        now = read(REGISTER).splitlines()
        self.assertGreater(len(then), 300)
        self.assertEqual(now[1:len(then)], then[1:], "every line of rounds 1 to 5 stays, word for word, in its place")

    def test_every_state_of_the_real_register_is_known(self):
        mod = load("reparations.py")
        self.assertIsNotNone(mod, "scripts/bench/reparations.py is missing")
        if mod is None:
            return
        rows = mod.load(read(REGISTER))[1]
        unknown = sorted({r.get("etat", "") for r in rows} - set(mod.STATES) - set(mod.OLD_STATES))
        self.assertEqual(unknown, [], "a state of the register that no program knows")
        late = [r for r in rows if mod.round_no(r.get("tour")) >= 6 and mod.state(r.get("etat")) in mod.OLD_REPAIR]
        self.assertEqual(late, [], "from round 6 on, no line says « corrigé »: only « constaté » is said fixed")

    # --- the four states -----------------------------------------------------------------------------------------------

    def test_the_four_states_and_who_gives_them(self):
        mod = load("reparations.py")
        self.assertIsNotNone(mod, "scripts/bench/reparations.py is missing")
        if mod is None:
            return
        self.assertEqual(tuple(mod.STATES), ("pas pris", "tenté", "rejoué", "constaté"))
        self.assertEqual(mod.GIVEN_BY, {"pas pris": "le trieur", "tenté": "l'équipe", "rejoué": "le portail",
                                        "constaté": "le chef, d'après un siège de contrôle"})
        # The state is read as a person writes it: case, accents (composed or not), spaces.
        for said, meant in (("Tenté", "tenté"), ("tente", "tenté"), ("  PAS  PRIS ", "pas pris"), ("constaté", "constaté"),
                            ("Corrige", "corrigé"), ("rejoue", "rejoué"), ("corrigé ✓", "corrigé ✓")):
            self.assertEqual(mod.state(said), meant, said)

    def test_old_and_new_lines_read_together(self):
        mod = load("reparations.py")
        self.assertIsNotNone(mod, "scripts/bench/reparations.py is missing")
        if mod is None:
            return
        # (An old line has 9 cells under a header of 12: the missing ones are empty, never shifted.)
        header, rows = mod.load(read(self.reg))
        self.assertEqual(header, HEADER.split("\t"))
        self.assertEqual(rows[0]["etat"], "corrigé")
        self.assertEqual(rows[0]["phrase_acceptation"], "")
        self.assertEqual(rows[1]["siege_origine"], "tour-6/lotB/a")
        self.assertEqual(rows[1]["phrase_acceptation"], "en espagnol, main gauche : je touche « Voz »")

    # --- « constaté »: the chief, from a control seat's « corrigé » line with its screen ------------------------------

    def test_a_control_seat_s_corrigé_with_its_screen_makes_the_defect_constaté(self):
        code, out = self.constate("A2")
        self.assertEqual(code, 0, out)
        got = [r for r in self.rows() if r["item"] == "A2"]
        self.assertEqual([r["etat"] for r in got], ["constaté", "constaté"], "every citation of the defect")
        self.assertEqual({r["constat"] for r in got}, {"docs/audits/ultra/sieges/tour-6/controle/Ba-3#15"}, out)
        self.assertIn("A2", out)
        for item, screen in (("T4", "98"), ("A7", "21")):
            code, out = self.constate(item)
            self.assertEqual(code, 0, out)
            self.assertEqual({r["constat"] for r in self.rows() if r["item"] == item},
                             {f"docs/audits/ultra/sieges/tour-6/controle/Ba-3#{screen}"}, out)

    def test_anything_but_corrigé_with_a_screen_is_refused_and_changes_nothing(self):
        before = read(self.reg)
        for item, word in (("A3", "autre"), ("A4", "encore là"), ("A6", "pas atteint"), ("A1", "screen"),
                           ("A9", "no line"), ("A8", "pas pris"), ("A5", "no line")):
            code, out = self.constate(item)
            self.assertEqual(code, 1, f"{item}: {out}")
            self.assertIn(word, out, item)
            self.assertEqual(read(self.reg), before, f"{item}: the register is not touched")

    def test_a_seat_without_a_report_or_a_round_without_the_defect_is_refused(self):
        code, out = self.constate("A2", tour="tour-7")
        self.assertEqual(code, 1, out)
        shutil.rmtree(self.seat)
        code, out = self.constate("A2")
        self.assertEqual(code, 1, out)
        self.assertIn("rapport.md", out)

    def test_the_other_lines_are_kept_byte_for_byte(self):
        before = read(self.reg).splitlines()
        self.assertEqual(self.constate("A2")[0], 0)
        after = read(self.reg).splitlines()
        self.assertEqual(len(before), len(after))
        changed = [i for i, (a, b) in enumerate(zip(before, after)) if a != b]
        self.assertEqual(changed, [2, 3], "only the two lines of A2")
        self.assertEqual(after[1], self.old)
        self.assertEqual(after[2], before[2].replace("\ttenté\t", "\tconstaté\t") + "docs/audits/ultra/sieges/tour-6/controle/Ba-3#15",
                         "only the state and the « constat » cell change, in their columns")

    # --- the programs that read it -------------------------------------------------------------------------------------

    def test_applique_corrections_reads_the_new_states(self):
        mod = load("applique-corrections.py")
        base = os.path.join(self.tmp, "base.tsv")
        write(base, "cle\tetat\tvu\tnote\tcorrige_au\nk-a2\tdéjà vu\ttour-6\t\t\nk-a4\tdéjà vu\ttour-6\t\t\n"
                    "k-t4\tdéjà vu\ttour-6\t\t\nk-old\tdéjà vu\ttour-5\t\t\nk-a8\tdéjà vu\ttour-6\t\t\nk-a5\tdéjà vu\ttour-6\t\t\n")
        where = {"B/a#15": "k-a2", "B/c#40": "k-a2", "B/a#9": "k-a4", "H/a#98": "k-t4", "A/a#12": "k-old", "B/a#31": "k-a8",
                 "B/a#8": "k-a3", "B/a#3": "k-a1", "B/a#30": "k-a6", "B/a#21": "k-a7", "B/a#44": "k-a5"}
        clusters = [{"key": k, "where": [w for w, kk in where.items() if kk == k]} for k in sorted(set(where.values()))]
        mod.fuse_mod.fuse = lambda *a, **k: (clusters, [], [], [], [])
        self.constate("A2")
        out = io.StringIO()
        with redirect_stdout(out):
            mod.main(["--corrections", self.reg, "--base", base, "--sieges", self.tmp])
        states = {l.split("\t")[0]: l.split("\t")[1] for l in read(base).splitlines()[1:]}
        # « constaté » and « rejoué » are a repair the bench must see come back (« [revient] »); « tenté » and
        # « pas pris » are not one: the key keeps its state.
        self.assertEqual(states["k-a2"], "corrigé", out.getvalue())
        self.assertEqual(states["k-t4"], "corrigé", out.getvalue())
        self.assertEqual(states["k-old"], "corrigé")
        self.assertEqual(states["k-a4"], "déjà vu")
        self.assertEqual(states["k-a8"], "déjà vu")
        self.assertEqual(states["k-a5"], "déjà vu", "« Tente » is « tenté », as a person writes it")
        self.assertIn("0 sans clé", out.getvalue())

    def test_fuse_reports_reads_the_register_s_rows(self):
        fuse = load("fuse-reports.py")
        rows = fuse.load_tsv(self.reg)
        self.assertEqual(rows[0]["etat"], "corrigé")
        self.assertEqual(rows[1]["phrase_acceptation"], "en espagnol, main gauche : je touche « Voz »")


PORTAL = os.path.join(HERE, "fusion-equipes.sh")
PREVOL = os.path.join(HERE, "pre-vol.sh")
TOY_REGISTER = "docs/audits/ultra/corrections.tsv"
BRANCH = "eq/tour-9-z"
PHRASE = "en espagnol, main gauche, téléphone à 100 %, Mode simple : je touche « Voz » au pouce et la piste voix s'ouvre"


class ToyRegister(unittest.TestCase):
    """A toy repository: the integration « int » with its register, and the lot's branch."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="constate-lot-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.repo = os.path.join(self.tmp, "depot")
        os.makedirs(self.repo)
        git(self.repo, "init", "-q", "-b", "int")
        self.lines = [
            HEADER,
            row("tour-5", "T5-01", "A/a", "12", "corrigé", "abc1234", "FooTest", "ancienne", ""),
            row("tour-6", "A2", "B/a", "15", "pas pris", "-", "-", "", "", PHRASE, "tour-6/lotB/a", ""),
            row("tour-6", "A3", "B/a", "8", "pas pris", "-", "-", "", "", "", "tour-6/lotB/a", ""),
        ]
        self.put(self.lines)
        self.base = self.commit("base")
        git(self.repo, "checkout", "-q", "-b", BRANCH)

    def put(self, lines):
        write(os.path.join(self.repo, TOY_REGISTER), "\n".join(lines) + "\n")

    def commit(self, message):
        git(self.repo, "add", "-A")
        git(self.repo, "commit", "-q", "--allow-empty", "-m", message)
        return git(self.repo, "rev-parse", "HEAD").stdout.strip()

    def lot(self, *changes, add=()):
        """The lot's commit: lines of the register replaced ({index: new line}) or added."""
        lines = list(self.lines)
        for i, text in changes:
            lines[i] = text
        self.put(lines + list(add))
        return self.commit("the lot writes the register")

    def rule(self, *more, cwd=None):
        return script("reparations.py", "--lot", BRANCH, f"base:{self.base}",
                      f"tete:{git(self.repo, 'rev-parse', 'int').stdout.strip()}", *more, cwd=cwd or self.repo)

    def refused(self, *words):
        code, out = self.rule()
        self.assertEqual(code, 1, out)
        for w in words:
            self.assertIn(w, out)
        return out

    def accepted(self):
        code, out = self.rule()
        self.assertEqual(code, 0, out)
        self.assertEqual(out.strip(), "", "nothing said of an honest lot")


class LotGivesItselfAState(ToyRegister):
    """§ 3, rule 5: « tenté » is the team's; « rejoué » the portal's; « constaté » the chief's after a control seat;
    « corrigé » is given by nobody from round 6 on."""

    def test_a_lot_that_turns_its_defect_to_tenté_enters(self):
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "rejeu-a2", "", "", PHRASE, "tour-6/lotB/a", "")))
        self.accepted()

    def test_a_lot_that_writes_corrigé_rejoué_or_constaté_is_refused(self):
        for said in ("corrigé", "rejoué", "constaté", "Corrige", "CORRIGÉ", "corrigé ✓", "constaté", " rejoue ", "fait"):
            with self.subTest(said=said):
                git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
                self.lot((2, row("tour-6", "A2", "B/a", "15", said, "7777777", "rejeu-a2", "", "", PHRASE, "tour-6/lotB/a", "")))
                out = self.refused("tour-6 A2", "« tenté »")
                self.assertIn("B/a#15", out)

    def test_a_new_line_of_an_old_round_cannot_say_corrigé_either(self):
        self.lot(add=[row("tour-5", "T5-09", "C/b", "4", "corrigé", "abc1234", "BarTest", "", "")])
        self.refused("tour-5 T5-09", "corrigé")

    def test_a_lot_does_not_write_the_sorter_s_sentence_nor_the_seat_of_origin(self):
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "-", "", "", "je touche Voz", "tour-6/lotB/a", "")))
        self.refused("acceptance sentence", "sorter")
        git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
        self.lot((3, row("tour-6", "A3", "B/a", "8", "tenté", "7777777", "-", "", "", "une phrase à moi", "tour-6/lotB/a", "")))
        self.refused("acceptance sentence")
        git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "-", "", "", PHRASE, "tour-6/lotF/f", "")))
        self.refused("seat of origin")
        git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "-", "", "", PHRASE, "tour-6/lotB/a", "controle/Ba#3")))
        self.refused("constat")

    def test_the_header_is_not_the_lot_s(self):
        lines = list(self.lines)
        lines[0] = HEADER.replace("\tconstat", "")
        self.put(lines)
        self.commit("a lot drops a column")
        self.refused("header")

    def test_a_line_the_integration_already_holds_is_not_the_lot_s(self):
        # The chief wrote « constaté » on the integration; the lot merged the integration in: nothing of its own.
        done = row("tour-6", "A3", "B/a", "8", "constaté", "7777777", "-", "", "", "", "tour-6/lotB/a",
                   "docs/audits/ultra/sieges/tour-6/controle/Ba#8")
        git(self.repo, "checkout", "-q", "int")
        lines = list(self.lines)
        lines[3] = done
        self.put(lines)
        self.commit("the chief: A3 constaté")
        git(self.repo, "checkout", "-q", BRANCH)
        git(self.repo, "merge", "-q", "--no-edit", "int")
        self.accepted()

    def test_the_lines_are_read_at_the_base_and_the_head_not_in_the_branch(self):
        # A branch whose own history says « constaté » (written, then left) is still the lot's line.
        self.lot((3, row("tour-6", "A3", "B/a", "8", "constaté", "7777777", "-", "", "", "", "tour-6/lotB/a",
                         "docs/audits/ultra/sieges/tour-6/controle/Ba#8")))
        self.commit("again")
        self.refused("tour-6 A3", "constaté")

    def test_without_its_options_the_rule_says_so(self):
        code, out = script("reparations.py", "--lot", BRANCH, cwd=self.repo)
        self.assertEqual(code, 2, out)

    def test_a_lot_that_does_not_touch_the_register_is_not_a_word(self):
        write(os.path.join(self.repo, "app.txt"), "code\n")
        self.commit("code only")
        self.accepted()


class PortalSetsRejoue(ToyRegister):
    """§ 3, rule 5: « rejoué » is the portal's, when the proof of rule 2 holds on the merged head."""

    def setUp(self):
        super().setUp()
        self.rejeux = os.path.join(self.tmp, "rejeux.txt")
        write(self.rejeux, "r1 : oracle tenu\nr7 : oracle non tenu (geste 3)\nr7 : oracle tenu\nr8 : oracle tenu\nr8 : oracle non tenu (écran 4)\n")

    def promote(self, *filters):
        git(self.repo, "checkout", "-q", "int")
        git(self.repo, "merge", "-q", "--no-ff", "--no-edit", BRANCH)
        merged = git(self.repo, "rev-parse", "HEAD").stdout.strip()
        code, out = script("reparations.py", "--rejoue", BRANCH, f"base:{self.base}", f"tete:{self.base}",
                           f"rejeux:{self.rejeux}", f"fusion:{merged}", *filters, cwd=self.repo)
        self.assertEqual(code, 0, out)
        header = read(os.path.join(self.repo, TOY_REGISTER)).splitlines()[0].split("\t")
        return {(r[1], r[3]): dict(zip(header, r)) for r in (l.split("\t") for l in read(os.path.join(self.repo, TOY_REGISTER)).splitlines()[1:])}, out, merged

    def test_a_tenté_whose_replay_holds_after_the_merge_becomes_rejoué(self):
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "r7", "", "", PHRASE, "tour-6/lotB/a", "")))
        rows, out, merged = self.promote()
        self.assertEqual(rows[("A2", "15")]["etat"], "rejoué", out)
        self.assertIn(merged[:8], rows[("A2", "15")]["note"], "the note names the merge the proof held on")
        self.assertIn("r7", rows[("A2", "15")]["note"])
        self.assertIn("rejoué", out)
        self.assertEqual(rows[("T5-01", "12")]["etat"], "corrigé")

    def test_a_replay_whose_last_verdict_is_not_held_leaves_tenté(self):
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "r8", "", "", PHRASE, "tour-6/lotB/a", "")),
                 (3, row("tour-6", "A3", "B/a", "8", "tenté", "7777777", "r9", "", "", "", "tour-6/lotB/a", "")))
        rows, out, _ = self.promote()
        self.assertEqual(rows[("A2", "15")]["etat"], "tenté", out)
        self.assertEqual(rows[("A3", "8")]["etat"], "tenté", "a replay the portal did not play")

    def test_a_test_proof_holds_when_the_portal_ran_it_green(self):
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "FooTest#works", "", "", PHRASE, "tour-6/lotB/a", "")),
                 (3, row("tour-6", "A3", "B/a", "8", "tenté", "7777777", "BarTest#cut", "", "", "", "tour-6/lotB/a", "")))
        rows, out, _ = self.promote("filtre:FooTest.works", "filtre:BazTest")
        self.assertEqual(rows[("A2", "15")]["etat"], "rejoué", out)
        self.assertEqual(rows[("A3", "8")]["etat"], "tenté", "a test the portal did not run")
        git(self.repo, "reset", "-q", "--hard", self.base)
        git(self.repo, "checkout", "-q", BRANCH)
        rows, out, _ = self.promote("filtre:BarTest")
        self.assertEqual(rows[("A3", "8")]["etat"], "rejoué", "the whole class ran")

    def test_only_the_lot_s_own_tenté_lines_are_promoted(self):
        # A « tenté » the integration held before (another lot's), and a « pas pris »: untouched.
        git(self.repo, "checkout", "-q", "int")
        lines = list(self.lines)
        lines[3] = row("tour-6", "A3", "B/a", "8", "tenté", "7777777", "r1", "", "", "", "tour-6/lotB/a", "")
        self.put(lines)
        self.base = self.commit("another lot's tenté")
        git(self.repo, "checkout", "-q", "-B", BRANCH, "int")
        self.lines = lines
        self.lot((2, row("tour-6", "A2", "B/a", "15", "pas pris", "-", "r1", "x", "", PHRASE, "tour-6/lotB/a", "")))
        rows, out, _ = self.promote()
        self.assertEqual(rows[("A3", "8")]["etat"], "tenté", out)
        self.assertEqual(rows[("A2", "15")]["etat"], "pas pris", out)

    def test_nothing_else_of_the_file_moves(self):
        self.lot((2, row("tour-6", "A2", "B/a", "15", "tenté", "7777777", "r7", "", "", PHRASE, "tour-6/lotB/a", "")))
        git(self.repo, "checkout", "-q", "int")
        git(self.repo, "merge", "-q", "--no-ff", "--no-edit", BRANCH)
        before = read(os.path.join(self.repo, TOY_REGISTER)).splitlines()
        git(self.repo, "checkout", "-q", BRANCH)
        git(self.repo, "checkout", "-q", "int")
        script("reparations.py", "--rejoue", BRANCH, f"base:{self.base}", f"tete:{self.base}", f"rejeux:{self.rejeux}",
               "fusion:abcdef12", cwd=self.repo)
        after = read(os.path.join(self.repo, TOY_REGISTER)).splitlines()
        self.assertEqual([i for i, (a, b) in enumerate(zip(before, after)) if a != b], [2])
        self.assertEqual(after[2].split("\t")[:4] + after[2].split("\t")[5:7], before[2].split("\t")[:4] + before[2].split("\t")[5:7])


def rules_of(text):
    return [l for l in text.splitlines() if re.search(r"not merged|undone", l)]


class PortalAndPreflight(unittest.TestCase):
    """The rule's place in the portal and its mirror in the pre-flight."""

    def test_the_portal_refuses_from_batch_5_before_the_merge(self):
        portal = read(PORTAL)
        lines = [l for l in rules_of(portal) if "reparations.py --lot" in l]
        self.assertEqual(len(lines), 1, "one refusal line calls reparations.py --lot")
        self.assertIn("from_paquet 5", lines[0])
        self.assertIn("exit 11", lines[0])
        self.assertLess(portal.index("reparations.py --lot"), portal.index('git merge --no-ff -m "Merge team'))
        self.assertRegex(lines[0], r'base:\$base" "tete:\$\(git rev-parse HEAD\)"')

    def test_the_portal_sets_rejoue_after_the_merge_and_takes_it_back_with_it(self):
        portal = read(PORTAL)
        self.assertTrue("reparations.py --rejoue" in portal, "the portal sets « rejoué »")
        at = portal.index("reparations.py --rejoue")
        self.assertGreater(at, portal.index("trap undo_merge ERR"), "after the checks, inside the traces' step")
        self.assertLess(at, portal.index('git commit -q -m "Portal traces of the merge'))
        block = portal[portal.rindex("\n  if ", 0, at):at]
        self.assertIn("from_paquet 5", block)
        self.assertIn("computed+=(docs/audits/ultra/corrections.tsv)", block, "taken back by clean_merge on a failure")
        self.assertIn('rejeux:$lot/rejeux.txt', portal[at:at + 300])
        self.assertIn('tete:$before', portal[at:at + 300])

    def test_the_preflight_mirrors_the_rule(self):
        prevol = read(PREVOL)
        self.assertTrue("reparations.py --lot" in prevol, "the pre-flight calls the rule")
        at = prevol.index("reparations.py --lot")
        self.assertIn("rule_from 5", prevol[at:at + 900])

    def test_the_preflight_knows_the_portal_s_rules(self):
        portal = read(PORTAL)
        rules = rules_of(portal)
        import hashlib
        fingerprint = hashlib.sha256(("\n".join(l.lstrip(" ") for l in rules) + "\n").encode()).hexdigest()[:10]
        m = re.search(r'^KNOWN_RULES="(\d+) ([0-9a-f]+)"', read(PREVOL), re.M)
        self.assertIsNotNone(m)
        self.assertEqual((int(m.group(1)), m.group(2)), (len(rules), fingerprint), "KNOWN_RULES of pre-vol.sh")


# ---------------------------------------------------------------------------------------------------------------------
# 3. Control seats: the bench plays docs/audits/ultra/sieges/<tour>/controle/<id>/<a-f>/ as it plays lot<X>/<a-f>/
# ---------------------------------------------------------------------------------------------------------------------

GRADLE_STUB = """#!/usr/bin/env bash
# The bench's stand-in: each seat of -Phuman.dir plays one gesture and ends, as HumanTester leaves it.
for a in "$@"; do case "$a" in -Phuman.dir=*) dir="${a#-Phuman.dir=}" ;; esac; done
for s in "$dir"/[a-f]; do
  [ -f "$s/start.txt" ] || continue
  printf '#0 SIEGE lang=es\\n#1 ROUTE remix | MODE simple | EMPREINTE aaaa | project=p-1 no tuto\\n#1 SON Son : rien ne joue.\\n#1 > touche 10 10\\n#2 ROUTE remix | MODE simple | EMPREINTE bbbb | project=p-1 no tuto\\n#2 FIN : fin du test\\n' > "$s/journal.txt"
  echo "fin du test" > "$s/fin.txt"
done
"""


class ControlSeats(unittest.TestCase):
    """Point 3 (plan § 4.3): the bench accepts a control folder wherever it accepts lot<X>/<a-f> (B a of round 6 had to
    be played in a lot of its own, then copied; three control seats had no mesures.json and guessed their goals)."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="constate-siege-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.depot = os.path.join(self.tmp, "depot")
        os.makedirs(os.path.join(self.depot, "scripts", "bench"))
        for f in ("human-tester.sh", "human-act.py"):
            shutil.copy(os.path.join(ROOT, "scripts", f), os.path.join(self.depot, "scripts", f))
        for f in ("sortie-de-siege.py", "seat-metrics.py"):
            shutil.copy(os.path.join(HERE, f), os.path.join(self.depot, "scripts", "bench", f))
        write(os.path.join(self.depot, "scripts", "bench", "tampon.sh"), "#!/usr/bin/env bash\nexit 0\n")
        write(os.path.join(self.depot, "scripts", "bench", "gradle-file.sh"), GRADLE_STUB)
        for f in ("tampon.sh", "gradle-file.sh"):
            os.chmod(os.path.join(self.depot, "scripts", "bench", f), 0o755)
        git(self.depot, "init", "-q")
        git(self.depot, "add", "-A")
        git(self.depot, "commit", "-q", "-m", "the code a control seat plays")
        self.sha = git(self.depot, "rev-parse", "HEAD").stdout.strip()
        self.round = os.path.join(self.depot, "docs", "audits", "ultra", "sieges", "tour-6")
        self.control = os.path.join(self.round, "controle", "Ba")
        write(os.path.join(self.control, "a", "start.txt"), "lang=es\ndevice=phone\nfont=100\nmode=simple\nmain=gauche\n"
                                                            "gestes.max=110\nmission=remix-voix\ntimeout.minutes=30\n")

    def bench(self, folder):
        r = subprocess.run(["bash", os.path.join(self.depot, "scripts", "human-tester.sh"), folder], cwd=self.depot,
                           capture_output=True, text=True, env=dict(os.environ, PS_JOUE_PAR="sous-agent neuf"))
        return r.returncode, r.stdout + r.stderr

    def test_the_bench_plays_a_control_folder_and_writes_its_head_and_its_measures(self):
        code, out = self.bench(self.control)
        self.assertEqual(code, 0, out)
        seat = os.path.join(self.control, "a")
        head = read(os.path.join(seat, "rapport.md")).splitlines()[:2]
        self.assertEqual(head, [f"code testé : {self.sha}", "joué par : sous-agent neuf"], out)
        self.assertTrue(os.path.isfile(os.path.join(seat, "mesures.json")), "the bench writes mesures.json itself:\n" + out)
        self.assertIn('"mission": "remix-voix"', read(os.path.join(seat, "mesures.json")))

    def test_every_seat_of_a_wave_lot_gets_its_measures_too(self):
        lot = os.path.join(self.round, "lotB")
        write(os.path.join(lot, "c", "start.txt"), "lang=fr\nmission=simple\n")
        code, out = self.bench(lot)
        self.assertEqual(code, 0, out)
        self.assertTrue(os.path.isfile(os.path.join(lot, "c", "mesures.json")), out)

    def test_a_flat_control_folder_is_told_where_its_seat_goes(self):
        flat = os.path.join(self.round, "controle", "Ha")
        write(os.path.join(flat, "start.txt"), "lang=fr\nmission=tuto-facile\n")
        code, out = self.bench(flat)
        self.assertEqual(code, 2, out)
        self.assertIn(os.path.join(flat, "a", "start.txt"), out)

    def test_the_tester_s_finger_accepts_a_control_seat(self):
        seat = os.path.join(self.control, "a")
        act = os.path.join(self.depot, "scripts", "human-act.py")
        r = subprocess.run([sys.executable, act, seat, "regarde"], capture_output=True, text=True)
        out = r.stdout + r.stderr
        self.assertNotIn("Siège refusé", out, "a control seat is a seat")
        self.assertIn("Le banc ne tourne pas", out, "it goes as far as the bench's own check")
        for wrong in (os.path.join(self.round, "controle", "Ba"), os.path.join(self.round, "controle", "Ba", "g"),
                      os.path.join(self.round, "ailleurs", "Ba", "a"), os.path.join(self.round, "controle", "..", "a")):
            os.makedirs(wrong, exist_ok=True)
            write(os.path.join(wrong, "start.txt"), "lang=fr\n")
            r = subprocess.run([sys.executable, act, wrong, "regarde"], capture_output=True, text=True)
            self.assertIn("Siège refusé", r.stdout + r.stderr, wrong)
        spec = importlib.util.spec_from_file_location("human_act", act)
        mod = importlib.util.module_from_spec(spec)
        try:
            spec.loader.exec_module(mod)
        except ImportError as e:  # (Pillow missing here: the label is read from its regular expression)
            self.skipTest(f"human-act.py needs {e}")
        self.assertEqual(mod.seat_label(seat), "controle/Ba/a")
        self.assertEqual(mod.seat_label(os.path.join(self.round, "lotB")), None)

    def test_the_exit_check_names_a_control_seat_by_its_folder(self):
        sortie = load("sortie-de-siege.py")
        self.assertEqual(sortie.seat_name(self.control, "a"), "controle/Ba/a")
        self.assertEqual(sortie.seat_name(os.path.join(self.round, "lotB"), "c"), "lotB/c")
        # A seat started, its report gone: the refusal names the control seat.
        code, out = script("sortie-de-siege.py", "avant", self.control, "--depot", self.depot)
        self.assertEqual(code, 0, out)
        os.remove(os.path.join(self.control, "a", "rapport.md"))
        code, out = script("sortie-de-siege.py", "verifie", self.control)
        self.assertEqual(code, 1, out)
        self.assertIn("controle/Ba/a : pas de rapport.md", out)


SEAT_REPORT = """siege: a | profil: t6-Ba gaucher | lang: es | device: phone | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
defauts-avant:
  - A7 | corrigé | ecran: 21 | la phrase touchée
  - geste: 3 | ecran.vu: 4 | cible: « Cortar » | geste.type: touche
hesitations:
  - ecran: 6 | cherchais: la voix | regarde: rien
defauts:
  - geste: 5 | ecran.vu: 6 | cible: « Voz » | geste.type: touche
    attendu: la voix | obtenu: rien
    symptome: hors-pouce
    gravite: finition | revu: oui: ecran 6 | banc?: non | promesse: -
"""


class ControlSeatsInTheRound(unittest.TestCase):
    """fuse-reports.py and tableau-de-bord.py read a round that holds control seats, nested or flat (round 6)."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="constate-tour-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.round = os.path.join(self.tmp, "tour-6")
        for folder in ("lotB/a", "controle/Ba/a", "controle/Ha"):
            write(os.path.join(self.round, folder, "rapport.md"), SEAT_REPORT)
            write(os.path.join(self.round, folder, "journal.txt"),
                  "#5 ROUTE remix | MODE simple | EMPREINTE a | project=p no tuto\n#5 > touche 1 1\n#5 DOIGT remix_voice « Voz »\n"
                  "#6 ROUTE remix | MODE simple | EMPREINTE b | project=p no tuto\n#6 BANC : état hérité remis\n")
        write(os.path.join(self.tmp, "base.tsv"), "cle\tetat\tvu\tnote\tcorrige_au\n")

    def test_a_control_seat_is_named_by_its_folder_and_its_section_is_no_defect(self):
        fuse = load("fuse-reports.py")
        clusters, rejected, seats, promises, incidents = fuse.fuse(self.round, os.path.join(self.tmp, "base.tsv"), None, "tour-6")
        where = sorted(w for c in clusters for w in c["where"])
        self.assertEqual(where, ["B/a#5", "controle/Ba/a#5", "controle/Ha#5"], "one defect a seat, three seats apart")
        code, out = script("fuse-reports.py", self.round, "--base", os.path.join(self.tmp, "base.tsv"), "--tour", "tour-6")
        self.assertEqual(code, 0, out)
        bench = read(os.path.join(self.round, "incidents-banc.tsv"))
        self.assertIn("controle/Ba/a\t6", bench)
        self.assertIn("controle/Ha\t6", bench)

    def test_the_round_s_missions_leave_the_control_seats_out(self):
        board = load("tableau-de-bord.py")
        for folder, result in (("lotB/a", "reussie"), ("controle/Ba/a", "non"), ("controle/Ha", "non")):
            write(os.path.join(self.round, folder, "mesures.json"), '{"mission": "remix-voix", "resultat": "%s", "mesures": {"fin": "fin"}}' % result)
        name, rows, curve = board.round_numbers(self.round, os.path.join(self.tmp, "base.tsv"), os.path.join(self.tmp, "v.tsv"), 120,
                                                os.path.join(self.tmp, "caches.tsv"))
        e = next(r for r in rows if r[0] == "E")
        self.assertEqual(e[2], "1/1", "a control seat replays a lot's branch: it is not one of the round's missions")


class TutoLockAcceptsTheBenchLayout(unittest.TestCase):
    """The lock of « portail4 » (rapport-zone.py --tuto): a control seat played by the bench now lives in
    controle/<id>/a/, its rapport.md starting « code testé : <sha> »; « controle: <id> » names it by its folder."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="constate-tuto-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.repo = os.path.join(self.tmp, "depot")
        os.makedirs(self.repo)
        git(self.repo, "init", "-q", "-b", "int")
        self.panel = os.path.join(self.repo, "app/src/main/java/p/tuto/TutoPanel.kt")
        write(self.panel, "v1\n")
        git(self.repo, "add", "-A")
        git(self.repo, "commit", "-q", "-m", "base")
        git(self.repo, "checkout", "-q", "-b", "eq/tour-9-z")
        write(self.panel, "v2\n")
        git(self.repo, "commit", "-q", "-am", "the tutorial, changed")
        self.sha = git(self.repo, "rev-parse", "HEAD").stdout.strip()

    def seat(self, rel, sha=None):
        write(os.path.join(self.repo, "docs/audits/ultra/sieges/tour-9/controle", rel, "rapport.md"),
              f"code testé : {sha or self.sha}\njoué par : sous-agent neuf\nsiege: a | profil: t9\n"
              "chapitre: 1 Un chapitre | ecran: 3 | compris du premier coup | vu\n")
        git(self.repo, "add", "-A")
        git(self.repo, "commit", "-q", "-m", "control seat")

    def rule(self, seat_id):
        write(os.path.join(self.repo, "docs/audits/ultra/tour-9-z-rapport.md"),
              f"# Rapport\n\ntests: aucun : jouet\n\ncontrole: {seat_id} | code testé: {self.sha[:10]} | bloqués: 0 | graves neufs: 0\n")
        git(self.repo, "add", "-A")
        git(self.repo, "commit", "-q", "-m", "report")
        base = git(self.repo, "merge-base", "int", "eq/tour-9-z").stdout.strip()
        return script("rapport-zone.py", "--tuto", "eq/tour-9-z", "docs/audits/ultra/tour-9-z-rapport.md", f"base:{base}",
                      f"tete:{git(self.repo, 'rev-parse', 'int').stdout.strip()}", "tour:tour-9", cwd=self.repo)

    def test_the_folder_of_a_seat_the_bench_played_is_its_id(self):
        self.seat("Ba/a")
        code, out = self.rule("Ba")
        self.assertEqual(code, 0, out)
        self.assertIn("Ba@", out)
        code, out = self.rule("Ba/a")
        self.assertEqual(code, 0, out)

    def test_the_seat_s_report_is_still_read(self):
        self.seat("Ba/a", sha="0" * 40)
        code, out = self.rule("Ba")
        self.assertEqual(code, 1, out)
        self.assertIn("names another commit", out)

    def test_two_seats_in_one_control_folder_are_not_guessed(self):
        self.seat("Ba/a")
        self.seat("Ba/b")
        code, out = self.rule("Ba")
        self.assertEqual(code, 1, out)
        self.assertIn("Ba/a", out)


class ReportTemplate(unittest.TestCase):
    """The tester's template gets the section « defauts-avant » with the four answers of rule 3."""

    def test_the_template_has_the_section_and_its_four_answers(self):
        text = read(os.path.join(ROOT, "docs", "audits", "GABARIT-RAPPORT-TESTEUR.md"))
        block = text.split("```")[1] if text.count("```") >= 2 else ""
        self.assertIn("defauts-avant:", block)
        line = next((l for l in block.splitlines() if l.strip().startswith("- <id")), "")
        for answer in ("corrigé", "autre", "encore là", "pas atteint"):
            self.assertIn(answer, line)
        self.assertIn("ecran:", line)
        mod = load("reparations.py")
        filled = ("defauts-avant:\n" + line.replace("<id du défaut visé>", "A7").replace("<corrigé|autre|encore là|pas atteint>", "corrigé")
                  .replace("<n°>", "21"))
        self.assertEqual(mod.before_defects(filled).get("A7", (None,))[:2], ("corrigé", "21"), "the chief's command reads the template's line")


# ---------------------------------------------------------------------------------------------------------------------
# 4. The dashboard: one line « réparations » with the four figures per round
# ---------------------------------------------------------------------------------------------------------------------

class DashboardLine(unittest.TestCase):
    """Plan § 3, rule 5: a point gives the four figures; « réparés sur N » counts « constaté » among the defects taken,
    the « pas pris » apart. One defect (round, item) counts once, at its best state."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="constate-tdb-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.reg = os.path.join(self.tmp, "corrections.tsv")
        write(self.reg, "\n".join([
            HEADER,
            row("tour-5", "T5-01", "A/a", "12", "corrigé", "abc", "-", "", ""),
            row("tour-5", "T5-02", "A/b", "13", "banc", "abc", "-", "", ""),
            row("tour-5", "T5-02", "A/c", "14", "banc", "abc", "-", "", ""),
            row("tour-6", "A1", "B/a", "3", "constaté", "1", "r1", "", "", "", "", "controle/Ba#3"),
            row("tour-6", "A1", "B/c", "9", "tenté", "1", "r1", "", ""),
            row("tour-6", "A2", "B/a", "15", "rejoué", "1", "r2", "", ""),
            row("tour-6", "A3", "B/a", "8", "tenté", "1", "-", "", ""),
            row("tour-6", "A5", "B/a", "44", "Tente", "1", "-", "", ""),
            row("tour-6", "A4", "B/a", "9", "pas pris", "-", "-", "", ""),
            # (A defect with a line of another state too, « banc » here: counted once, at its repair state.)
            row("tour-6", "A3", "B/d", "10", "banc", "1", "-", "", ""),
        ]) + "\n")
        for t in ("tour-5", "tour-6"):
            os.makedirs(os.path.join(self.tmp, t))
        write(os.path.join(self.tmp, "base.tsv"), "cle\tetat\tvu\tnote\tcorrige_au\n")

    def test_the_four_figures_of_a_round(self):
        mod = load("reparations.py")
        self.assertTrue(hasattr(mod, "counts"), "reparations.counts(rows, tour) gives the four figures")
        if not hasattr(mod, "counts"):
            return
        rows = mod.load(read(self.reg))[1]
        self.assertEqual(mod.counts(rows, "tour-6"), {"constaté": 1, "rejoué": 1, "tenté": 2, "pas pris": 1, "avant": {}})
        self.assertEqual(mod.counts(rows, "tour-5"), {"constaté": 0, "rejoué": 0, "tenté": 0, "pas pris": 0,
                                                       "avant": {"corrigé": 1, "banc": 1}})
        self.assertEqual(mod.counts(rows, "tour-7"), {"constaté": 0, "rejoué": 0, "tenté": 0, "pas pris": 0, "avant": {}})

    def test_the_dashboard_prints_one_line_per_round(self):
        code, out = script("tableau-de-bord.py", os.path.join(self.tmp, "tour-5"), os.path.join(self.tmp, "tour-6"),
                           "--base", os.path.join(self.tmp, "base.tsv"), "--verdicts", os.path.join(self.tmp, "v.tsv"),
                           "--caches", os.path.join(self.tmp, "c.tsv"), "--corrections", self.reg)
        self.assertEqual(code, 0, out)
        lines = [l for l in out.splitlines() if l.startswith("Réparations")]
        self.assertEqual(lines, [
            "Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris "
            "(seul « constaté » se dit corrigé ; les pas pris à part) ; états d'avant la règle : corrigé 1, banc 1",
            "Réparations (registre corrections.tsv) : constaté 1, rejoué 1, tenté 2, pas pris 1 — réparés : 1 sur 4 pris "
            "(seul « constaté » se dit corrigé ; les pas pris à part)"], out)
        tour6 = out.split("## tour-6")[1]
        self.assertLess(tour6.index("Réparations"), tour6.index("Toutes remplies"), "under the round's own table")
        self.assertFalse(any(l.startswith("| R") or "réparations" in l.lower() for l in out.splitlines() if l.startswith("|")),
                         "not a row of the stop criterion: its A to J stay as they are")

    def test_the_real_register_is_the_default(self):
        board = read(os.path.join(HERE, "tableau-de-bord.py"))
        self.assertTrue('opt("--corrections", os.path.join(docs, "corrections.tsv"))' in board, "the register's default path")


# ---------------------------------------------------------------------------------------------------------------------
# 5. The cards (docs/audits/ultra/GABARIT-FICHES-V3.md, printed by scripts/bench/fiche.sh)
# ---------------------------------------------------------------------------------------------------------------------

SEAT_KEYS = ["NOM=Lancelot", "TOUR=tour-6", "LOT=B", "SIEGE=a", "SHA=0123abcd", "BRANCHE=ccr-jouet",
             "DOSSIER_DU_LOT=docs/audits/ultra/sieges/tour-6/controle/Ba", "BRIEF=briefs/Ba.txt",
             "CONSIGNES_TESTEUR_MD_DU_TOUR=tour-6/consignes-testeur.md", "GESTES=110", "MINUTES=30", "LISTE=A2, A3"]
FIX_KEYS = ["NOM=Perceval", "TOUR=tour-6", "ZONE=remix2", "IDS=A2, A3", "LOTS_CORRECTION_MD_SECTION_DU_LOT=lots-correction.md §2",
            "VERDICTS_TSV=verdicts.tsv", "LA_LECTURE=lecture-recurrents.md", "LISTE=RemixScreen.kt"]


def card(*args):
    r = subprocess.run(["bash", os.path.join(HERE, "fiche.sh"), *args], capture_output=True, text=True)
    return r.returncode, r.stdout, r.stderr


class Cards(unittest.TestCase):
    """Plan § 4.5: the acceptance sentence in the sorter's card, the proof in the conditions of origin in the
    corrector's, the control seat's card."""

    def test_the_control_seat_s_card_asks_for_the_seat_of_origin_and_its_answers(self):
        code, out, err = card("1", "CHOIX=contrôle", "ZONE=remix2", *SEAT_KEYS)
        self.assertEqual(code, 1, out + err)
        self.assertIn("SIEGE_D_ORIGINE", err, "a control seat names the seat it replays")
        code, out, err = card("1", "CHOIX=contrôle", "ZONE=remix2", "SIEGE_D_ORIGINE=tour-6/lotB/a", *SEAT_KEYS)
        self.assertEqual(code, 0, err)
        self.assertIn("contrôle du lot remix2, siège d'origine tour-6/lotB/a", out)
        self.assertIn("docs/audits/ultra/sieges/tour-6/controle/Ba/a/", out, "the control seat's own folder, played by the bench")
        self.assertIn("`start.txt` du siège d'origine", out)
        self.assertIn("defauts-avant:", out)
        for answer in ("« corrigé »", "« autre »", "« encore là »", "« pas atteint »"):
            self.assertIn(answer, out)
        self.assertIn("« - <id> | corrigé | ecran: <n> | … »", out, "the line the chief's command reads, left to the tester")
        self.assertIn("phrase d'acceptation", out)

    def test_a_wave_seat_s_card_asks_nothing_more(self):
        code, out, err = card("1", "CHOIX=vague", *SEAT_KEYS)
        self.assertEqual(code, 0, err)
        code, keys, err = card("--variables", "1")
        self.assertIn("SIEGE_D_ORIGINE\t<siège d'origine>\tselon CHOIX", keys)

    def test_the_corrector_repairs_the_sentence_and_proves_it_in_the_conditions_of_origin(self):
        code, out, err = card("2", *FIX_KEYS)
        self.assertEqual(code, 0, err)
        for words in ("phrase d'acceptation", "siège d'origine", "conditions d'origine", "`start.txt`", "« tenté »",
                      "colonne `test`", "« rejoué »", "« constaté »"):
            self.assertIn(words, out)

    def test_the_sorter_writes_the_sentence_and_the_seat_of_origin(self):
        code, out, err = card("--brouillon", "4b")
        self.assertEqual(code, 0, err)
        for words in ("phrase d'acceptation", "`phrase_acceptation`", "`siege_origine`", "« pas pris »",
                      "docs/audits/ultra/corrections.tsv"):
            self.assertIn(words, out)
        code, out, err = card("--brouillon", "4a")
        self.assertNotIn("phrase d'acceptation", out, "the reader gives facts, not the sentence")


if __name__ == "__main__":
    unittest.main(verbosity=1)
