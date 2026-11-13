#!/usr/bin/env python3
"""Tests of the bench lot « constate » (docs/audits/ultra/PLAN-REPARATIONS-CONSTATEES.md § 4): a repair is « done »
only when a tester saw it. Seconds, no Gradle, no network (toy repositories and folders):

  python3 -I scripts/bench/test_constate.py

1. corrections.tsv, the one register: four states (« pas pris », « tenté », « rejoué », « constaté »), an acceptance
   sentence and the seat of origin; the lines of the rounds before keep their text; the programs that read it go on.
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


if __name__ == "__main__":
    unittest.main(verbosity=1)
