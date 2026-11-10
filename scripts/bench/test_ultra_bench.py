#!/usr/bin/env python3
"""Checks seat-metrics.py and fuse-reports.py (Audit ultra, banc 2) on a tiny round made here: two
seats' journals, sounds and saved state, two reports (`python3 -I scripts/bench/test_ultra_bench.py`).
Standard library only."""
import json
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))

JOURNAL_A = """#0 SIEGE lang=fr device=phone font=100 mode=simple
#0 AMENEE padlive
#1 ROUTE padlive | MODE simple | EMPREINTE aaaa | project=p-1 no tuto
#1 SON Son : rien ne joue.
#1 > touche 10 10
#1 DOIGT rien
#2 ROUTE padlive | MODE simple | EMPREINTE aaaa | project=p-1 no tuto
#2 SON Son : rien ne joue.
#2 > touche 100 600
#2 DOIGT padlive_pad_4 « Klaxon »
#3 ROUTE padlive | MODE simple | EMPREINTE bbbb | project=p-1 no tuto
#3 SON Son : rien ne joue.
#3 > glisse 10 900 10 300
#4 ROUTE padlive | MODE simple | EMPREINTE cccc | project=p-1 no tuto
#4 SON Son : rien ne joue.
#4 > touche 500 40
#4 DOIGT help_padlive « ? »
#4 AIDE help_padlive
#5 ROUTE dialogue:help_page | MODE simple | EMPREINTE dddd | project=p-1 no tuto
#5 SON Son : rien ne joue.
#5 > retour
#5 RETOUR
#5 GEL 812 ms
    at com.pocketstudio.app.Foo.bar(Foo.kt:1)
#6 ROUTE padlive | MODE simple | EMPREINTE cccc | project=p-1 no tuto
#6 SON Son : rien ne joue.
#6 > attends 2
#7 ROUTE padlive | MODE simple | EMPREINTE cccc | project=p-1 no tuto
#7 SON Son : rien ne joue.
#7 > touche 300 300
#7 DOIGT désactivé padlive_rec « REC »
#7 PARTAGE perf.m4a
#8 ROUTE padlive | MODE simple | EMPREINTE eeee | project=p-1 no tuto
#8 SON Son : rien ne joue.
#8 FIN : abandon : budget (6 gestes, 6 faits, 0 images)
"""

JOURNAL_B = """#0 SIEGE lang=de device=tablet8 font=130 mode=pro
#1 ROUTE tab_Sons | MODE pro | EMPREINTE 1111 | project=p-2 no tuto
#1 SON Son : rien ne joue.
#1 > touche 1 1
#2 ROUTE tab_Sons | MODE pro | EMPREINTE 2222 | project=p-2 no tuto
#2 SON Son : rien ne joue.
#2 FIN : fin
"""

REPORT_A = """```
siege: a | profil: enfant de 8 ans | lang: fr | device: phone | font: 100 | mode: simple
mission: padlive | buts atteints: 1 sur 2
hesitations:
  - ecran: 3 | cherchais: le bouton pour jouer | regarde: en haut
surprise: aucune
abandon: jamais
defauts:
  - ecran: 4 | cible: « Klaxon » | geste: touche
    attendu: un son | obtenu: rien
    symptome: rien
    gravite: grave | bloque: non
  - ecran: 1 | cible: REC | geste: touche
    attendu: enregistrer | obtenu: rien
    symptome: incompris
    gravite: mineur | bloque: non
```
"""

REPORT_B = """siege: b | profil: DJ amateur | lang: de | device: tablet8 | font: 130 | mode: pro
mission: padlive | buts atteints: 0 sur 2
defauts:
  - ecran: 2 | cible: Klaxon | geste: touche
    attendu: un son | obtenu: rien
    symptome: rien
    gravite: bloquant | bloque: oui
"""

REPORT_SOUND = """siege: c | profil: ingé son | lang: fr | device: phone | font: 100 | mode: pro
defauts:
  - ecran: 1 | cible: Master | geste: touche
    attendu: propre | obtenu: le son est mauvais
    symptome: rien
    gravite: grave | bloque: non
"""


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def make_round(d):
    a = os.path.join(d, "a")
    write(os.path.join(a, "start.txt"), "mission=padlive\ngestes.max=6\n")
    write(os.path.join(a, "journal.txt"), JOURNAL_A)
    cmds = {1: "touche 10 10", 2: "touche 100 600", 3: "glisse 10 900 10 300", 4: "touche 500 40", 5: "retour", 6: "attends 2", 7: "touche 300 300", 8: "touche 1 1"}
    for n, c in cmds.items():
        write(os.path.join(a, f"cmd-{n}.txt"), c)
    for n in range(1, 9):
        write(os.path.join(a, f"sound-{n}.txt"), "Son : rien ne joue." if n != 3 else "Son : rien ne joue.\nLe pad 5 (« Klaxon ») joue une fois.")
    write(os.path.join(a, "etat", "resume.json"), json.dumps({
        "projects": [{"id": "p-1", "name": "Pad Live", "parts": 0, "seconds": 0, "stars": [2], "voice_pad": False, "audio_clips": 0}],
        "stem_sets": 0, "shared": ["perf.m4a"], "prefs": {"simpleMode": True},
    }))
    write(os.path.join(a, "rapport.md"), REPORT_A)
    b = os.path.join(d, "b")
    write(os.path.join(b, "start.txt"), "mission=premier-morceau\n")
    write(os.path.join(b, "journal.txt"), JOURNAL_B)
    write(os.path.join(b, "cmd-1.txt"), "touche 1 1")
    write(os.path.join(b, "cmd-2.txt"), "fin")
    write(os.path.join(b, "etat", "resume.json"), json.dumps({
        "projects": [{"id": "p-2", "name": "Mon son", "parts": 4, "seconds": 150.0}], "stem_sets": 0, "shared": [], "prefs": {"simpleMode": False},
    }))
    write(os.path.join(b, "rapport-b.txt"), REPORT_B)
    write(os.path.join(d, "rapport-c.md"), REPORT_SOUND)


def run(*args):
    r = subprocess.run([sys.executable, "-I", *args], capture_output=True, text=True)
    assert r.returncode == 0, r.stdout + r.stderr
    return r.stdout


def check(cond, what):
    if not cond:
        print("FAIL:", what)
        sys.exit(1)


def main():
    with tempfile.TemporaryDirectory() as d:
        make_round(d)
        # The seat's measures and its mission, read in its state.
        run(os.path.join(HERE, "seat-metrics.py"), os.path.join(d, "a"))
        with open(os.path.join(d, "a", "mesures.json"), encoding="utf-8") as f:
            a = json.load(f)
        m = a["mesures"]
        check(m["gestes"] == 6, f"6 gestures, the wait aside: {m['gestes']}")
        check(m["gestes_premier_son"] == 2, f"first sound after 2 gestures (the pad, screen 3): {m['gestes_premier_son']}")
        check(m["gestes_premier_partage"] == 6, f"shared at the 6th gesture: {m['gestes_premier_partage']}")
        check(m["touchers_a_vide"] == 1 and m["touchers_desactives"] == 1, f"one empty touch, one disabled: {m}")
        check(m["retours"] == 1 and m["retours_immediats"] == 1, f"one back, right after arriving: {m}")
        check(m["ecrans_revisites"] == 1, f"padlive revisited once after the help: {m['ecrans_revisites']}")
        check(m["defilements_sans_toucher"] == 0, "the swipe was followed by a touch")
        check(m["appels_aide"] == 1 and m["gels"] == 1, f"one help, one stall: {m}")
        check(m["moteur_absent"] == 0, f"no native call missing in a's journal: {m}")
        check(m["abandon"].startswith("abandon : budget"), f"abandoned on the budget: {m['abandon']}")
        check(a["mission"] == "padlive" and a["resultat"] == "partielle", f"padlive: first sound in 2 gestures, not 1: {a['resultat']} {a['pourquoi']}")
        out = run(os.path.join(HERE, "seat-metrics.py"), os.path.join(d, "a"), "--mission", "defi")
        check("mission=reussie" in out, "two stars: the challenge succeeded")
        out = run(os.path.join(HERE, "seat-metrics.py"), os.path.join(d, "b"))
        check("mission=partielle" in out, "a long song, never shared: partial")
        out = run(os.path.join(HERE, "seat-metrics.py"), os.path.join(d, "b"), "--mission", "simple")
        check("mission=non" in out, "in Pro: the « Mode simple » mission failed")

        # The fusion: « Klaxon »/« rien » seen by a (screen 4: padlive) and by b (screen 2: tab_Sons) are two
        # routes; a's screen-1 REC is another key. The sound report is rejected.
        base = os.path.join(d, "base.tsv")
        with open(base, "w", encoding="utf-8") as f:
            f.write("cle\tetat\tvu\tnote\npadlive|rec|incompris\tvoulu\ttour-0\t\n")
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--maj-base", "--tour", "tour-1")
        lines = text.splitlines()
        check(len(lines) <= 60, "60 lines at most")
        check("c#1 (jugement sonore)" in text, "the sound judgment set aside, that defect only:\n" + text)
        check(any("[nouveau] bloquant bloque ×1 (b#2) tab_Sons · « klaxon » · rien" in l for l in lines), "b's blocking key first:\n" + text)
        check(any("[nouveau] grave ×1 (a#4) padlive · « klaxon » · rien" in l for l in lines), "a's key:\n" + text)
        check(any("[voulu]" in l and "« rec »" in l for l in lines), "a known key keeps its state:\n" + text)
        check(lines[1].startswith("3 siège(s), 3 grappe(s) : 2 nouvelle(s), ") and ", 1 autre(s) état(s) de la base (voulu : 1) ;" in lines[1], "the summary:\n" + text)
        with open(base, encoding="utf-8") as f:
            added = f.read()
        check("tab_Sons|klaxon|rien\tnouveau" in added and added.count("\n") == 4, "the new keys added to the base:\n" + added)
        # A second run: nothing new any more, and the two seats' same key (when on the same route) fuses.
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-2")
        check(" : 0 nouvelle(s), " in text, "the keys are known now, at the next round:\n" + text)
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-1")
        check(" : 2 nouvelle(s), " in text, "fused again, the same round still calls them new:\n" + text)
        with open(os.path.join(d, "b", "journal.txt"), "w", encoding="utf-8") as f:
            f.write(JOURNAL_B.replace("#1 ROUTE tab_Sons", "#1 ROUTE padlive"))
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base)
        check("×2 (a#4, b#2) padlive · « klaxon » · rien" in text and "; 1 vue(s) par ≥ 2 sièges" in text, "the same key fused:\n" + text)
        # Banc fiable 2.1: --maj-base adds the round to a known key; the next round sees it « déjà vu ».
        run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--maj-base", "--tour", "tour-7")
        with open(base, encoding="utf-8") as f:
            text_base = f.read()
        check("padlive|klaxon|rien\tnouveau\ttour-0," in text_base or "padlive|klaxon|rien\tnouveau\t" in text_base, "the key is in the base:\n" + text_base)
        check("tour-7" in [l for l in text_base.splitlines() if l.startswith("padlive|klaxon|rien")][0], "its round added:\n" + text_base)
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-8")
        check("[déjà vu (" in text and "padlive · « klaxon »" in text, "seen at an earlier round, never fixed: « déjà vu »:\n" + text)
        missions_of_round_1(d)
    with tempfile.TemporaryDirectory() as d:
        fusion_v2(d)
    with tempfile.TemporaryDirectory() as d:
        bench_guards(d)
    with tempfile.TemporaryDirectory() as d:
        stamp_needs_green_summary(d)
    with tempfile.TemporaryDirectory() as d:
        chantier(d)
        judge_criteria(d)
        open_app_proof(d)
        known_defect_lifted(d)
        known_defect_lifted_needs_a_red(d)
        owner_trials(d)
        pad_timing_verdicts(d)
        engine_proof(d)
        expected_classes(d)
        skipped_class(d)
        portal(d)
        fusion_forged(d)
        known_skips_bounded(d)
        mark_fixed(d)
        seat_exit(d)
        native_missing(d)
        corrections_by_target(d)
    with tempfile.TemporaryDirectory() as d:
        seats_from_ids(d)
    with tempfile.TemporaryDirectory() as d:
        comebacks(d)
    with tempfile.TemporaryDirectory() as d:
        header_counts(d)
    with tempfile.TemporaryDirectory() as d:
        element_field(d)
    with tempfile.TemporaryDirectory() as d:
        curve_scales(d)
    with tempfile.TemporaryDirectory() as d:
        same_button(d)
    with tempfile.TemporaryDirectory() as d:
        measured_numbers(d)
    print("ultra bench scripts: OK")


REPORT_BUTTONS = """siege: a | profil: ado | lang: fr | device: phone | font: 100 | mode: simple
defauts:
  - geste: 2 | ecran.vu: 3 | cible: Klaxon | geste.type: touche
    attendu: un son | obtenu: rien
    symptome: rien
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: ? | geste.type: touche
    attendu: l'aide | obtenu: rien
    symptome: rien
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: REC | geste.type: touche
    attendu: enregistrer | obtenu: rien
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
"""


def same_button(d):
    """Banc fiable 2.4 / PLAN-RESTE 6.4: « même bouton (tour-N) » when the same real button was reported at an earlier
    round under another route or symptom (N = the earliest such round), a candidate shown without fusion; a button
    known only at this round or a later one stays « nouveau »; a navigation tag or a generic button (tuto_do, *_back)
    never makes one."""
    write(os.path.join(d, "lotA", "a", "journal.txt"), JOURNAL_A)
    write(os.path.join(d, "lotA", "a", "rapport.md"), REPORT_BUTTONS)
    base = os.path.join(d, "base.tsv")
    write(base, "cle\tetat\tvu\tnote\n"
                "mixer|padlive_pad_4|perdu\tnouveau\ttour-3,tour-2\t\n"   # the button, elsewhere, at rounds 2 and 3
                "settings|help_padlive|rien\tnouveau\ttour-5\t\n"         # this round only
                "live|padlive_rec|lent\tnouveau\ttour-6\t\n")             # a later round
    text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-5")
    check("[même bouton (tour-2)] mineur ×1 (A/a#2) padlive · « padlive_pad_4 » · rien" in text, "the earliest earlier round:\n" + text)
    check("[nouveau] mineur ×1 (A/a#4) padlive · « help_padlive » · rien" in text, "known at this round only: new:\n" + text)
    check("[nouveau] mineur ×1 (A/a#7) padlive · « padlive_rec » · incompris" in text, "known at a later round only: new:\n" + text)
    check(", 1 même bouton (tour-2 : 1), " in text, "counted in the header:\n" + text)
    # A generic button (the tutorial's « Je le fais » does a different thing at each step) and a navigation tag.
    write(os.path.join(d, "lotA", "a", "journal.txt"), JOURNAL_A.replace("#2 DOIGT padlive_pad_4 « Klaxon »", "#2 DOIGT tuto_do « Je le fais »")
          .replace("#4 DOIGT help_padlive « ? »", "#4 DOIGT tab_Sons « Sons »"))
    write(base, "cle\tetat\tvu\tnote\nmixer|tuto_do|perdu\tnouveau\ttour-1\t\nmixer|tab_Sons|perdu\tnouveau\ttour-1\t\n")
    text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-5")
    check("[nouveau] mineur ×1 (A/a#2) padlive · « tuto_do » · rien" in text, "a generic button is no « même bouton »:\n" + text)
    check("[nouveau] mineur ×1 (A/a#4) padlive · « ? » · rien" in text, "a navigation tag is never the target:\n" + text)
    check(", 0 même bouton, " in text, "none counted:\n" + text)


def measured_numbers(d):
    """PLAN-RESTE 6.4: tableau-de-bord.py's measured(), on a repository, a docs folder and a round made here: the
    uncommitted files, the round's reviews (the bench's own aside, « corrigé » rows aside), the fix commits of the
    round's eq/ branches without a test, the lots not played, the seats played and cut by the bench; a folder of
    reviews that does not exist gives no number (never a 0)."""
    sys.path.insert(0, HERE)
    tb = __import__("tableau-de-bord")
    repo = os.path.join(d, "repo")
    os.makedirs(repo)

    def git(*a):
        subprocess.run(["git", "-C", repo, "-c", "user.name=t", "-c", "user.email=t@t", *a], check=True, capture_output=True)
    git("init", "-q", "-b", "main")
    write(os.path.join(repo, "README"), "x")
    git("add", "README")
    git("commit", "-q", "-m", "start")
    git("checkout", "-q", "-b", "eq/tour-9-zone1")
    write(os.path.join(repo, "app", "src", "main", "A.kt"), "a")
    git("add", "app")
    git("commit", "-q", "-m", "a fix without a test")
    write(os.path.join(repo, "app", "src", "main", "B.kt"), "b")
    write(os.path.join(repo, "app", "src", "test", "BTest.kt"), "t")
    git("add", "app")
    git("commit", "-q", "-m", "a fix with its test")
    write(os.path.join(repo, "note.txt"), "left behind")
    docs = os.path.join(d, "docs")
    write(os.path.join(docs, "tour-9-zone1-relecture.md"),
          "| # | Gravité | Quoi | État |\n|---|---|---|---|\n| 1 | grave | x | corrigé |\n| 2 | grave | y | ouvert |\n| 3 | bloquant | z | ouvert |\n")
    write(os.path.join(docs, "tour-9-banc-relecture.md"), "| 4 | grave | w | ouvert |\n")
    tour = os.path.join(d, "tour-9")
    write(os.path.join(tour, "sieges.tsv"), "lot\tsiege\tlang\nA\ta\tfr\nA\tb\tfr\nA\tc\tfr\nB\ta\tfr\n")
    write(os.path.join(tour, "lotA", "a", "mesures.json"), json.dumps({"mesures": {"fin": "fin", "gels": 0}}))
    write(os.path.join(tour, "lotA", "b", "mesures.json"), json.dumps({"mesures": {"fin": "banc : rien ne change", "gels": 0}}))
    write(os.path.join(tour, "lotA", "c", "mesures.json"), json.dumps({"mesures": {"fin": "fin", "gels": 2}}))
    m = tb.measured(tour, "tour-9", root=repo, docs=docs)
    check(m.get("fichiers_non_commites") == 1, f"one file left behind: {m}")
    check(m.get("relecture_graves") == 1 and m.get("relecture_bloquants") == 1, f"the review's open rows, the bench's review aside: {m}")
    check(m.get("corrections_sans_test") == 1, f"one fix commit without a test on eq/tour-9-*: {m}")
    check(m.get("lots_non_joues") == 1 and m.get("sieges_joues") == 3 and m.get("sieges_coupes_par_le_banc") == 2,
          f"lot B not played; 3 seats played, 2 cut (the bench's end, a freeze): {m}")
    m = tb.measured(tour, "tour-9", root=repo, docs=os.path.join(d, "nowhere"))
    check("relecture_graves" not in m and "relecture_bloquants" not in m, f"no reviews folder: unknown, not 0: {m}")


REPORT_V2_GRAVES = """siege: a | profil: ado | lang: fr | device: phone | font: 100 | mode: simple
defauts:
  - geste: 2 | ecran.vu: 3 | cible: Klaxon | geste.type: touche
    attendu: un son | obtenu: rien
    symptome: rien
    gravite: mission-impossible | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: ? | geste.type: touche
    attendu: l'aide | obtenu: rien
    symptome: rien
    gravite: contourne-en-5-gestes | revu: oui: ecran 6 | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: REC | geste.type: touche
    attendu: tout de suite | obtenu: 40 s
    symptome: lent
    gravite: contourne-en-5-gestes | revu: oui: ecran 8 | banc?: temps | promesse: -
"""


def curve_scales(d):
    """PLAN-RESTE 6.4 (META-AUDIT-2 §2): the convergence curve says each round's scale (v1 = the tester's word,
    v2 = the consequence) and gives two graves columns: declared by the testers / confirmed (kept grave by the fusion:
    redone by the tester, not the bench's doubt nor a « banc » verdict); a change of scale between two rounds is
    written, since their graves do not compare."""
    write(os.path.join(d, "tour-4", "lotA", "a", "journal.txt"), JOURNAL_A)
    write(os.path.join(d, "tour-4", "lotA", "a", "rapport.md"), REPORT_A)
    write(os.path.join(d, "tour-5", "lotA", "a", "journal.txt"), JOURNAL_A)
    write(os.path.join(d, "tour-5", "lotA", "a", "rapport.md"), REPORT_V2_GRAVES)
    base = os.path.join(d, "base.tsv")
    write(base, "cle\tetat\tvu\tnote\n")
    verdicts = os.path.join(d, "verdicts.tsv")
    write(verdicts, "tour\titem\tcle\tverdict\tpreuve_appli\tgarde_banc\ttelephone\tcommit\n")
    board = run(os.path.join(HERE, "tableau-de-bord.py"), os.path.join(d, "tour-4"), os.path.join(d, "tour-5"), "--base", base, "--verdicts", verdicts)
    check("| tour | sieges | fiches | fiches_par_siege | echelle | graves_declares_par_siege | graves_confirmes_par_siege | grappes |" in board,
          "the curve's columns:\n" + board)
    check("| tour-4 | 1 | 2 | 2.0 | v1 | 1.0 | 1.0 |" in board, "v1: one grave by the tester's word, kept:\n" + board)
    check("| tour-5 | 1 | 3 | 3.0 | v2 | 3.0 | 1.0 |" in board,
          "v2: three graves by the consequence, one kept (a mission-impossible not redone, a « banc? » card):\n" + board)
    check("Changement d'échelle entre tour-4 et tour-5 (v1 → v2)" in board, "the change of scale is written:\n" + board)


JOURNAL_DIALOG = """#0 SIEGE lang=fr device=phone font=100 mode=simple
#1 ROUTE dialogue:part_header_menu | MODE simple | EMPREINTE aaaa | project=p-1 no tuto
#1 SON Son : rien ne joue.
#1 > touche 10 10
#1 DOIGT bouton « Partie C »
#2 ROUTE dialogue:part_header_menu | MODE simple | EMPREINTE bbbb | project=p-1 no tuto
#2 SON Son : rien ne joue.
#2 FIN : fin
"""


def element_field(d):
    """PLAN-RESTE 6.4 (rest of banc fiable 2.4): a card's « element: » (the testTag under the finger, written by newer
    benches) is the key's target when the journal gives no button (a dialogue, a navigation tag), instead of the free
    text, so a French and a Hindi seat fuse; a navigation tag as element is ignored; the element is a button for
    « même bouton »."""
    card = ("siege: {s} | profil: p | lang: {l} | device: phone | font: 100 | mode: simple\n"
            "defauts:\n"
            "  - geste: 1 | ecran.vu: 2 | cible: {c}{e} | geste.type: touche\n"
            "    attendu: la partie C | obtenu: reste sur A\n"
            "    symptome: rien\n"
            "    gravite: contourne-en-5-gestes | revu: oui: ecran 2 | banc?: non | promesse: -\n")
    for seat, lang, cible, element in (("a", "fr", "Partie C", " | element: part_pick_2"), ("b", "hi", "पार्ट: C", " | element: part_pick_2"),
                                       ("c", "fr", "Partie C", ""), ("d", "fr", "Partie C", " | element: tab_Sons")):
        write(os.path.join(d, "lotA", seat, "journal.txt"), JOURNAL_DIALOG)
        write(os.path.join(d, "lotA", seat, "rapport.md"), card.format(s=seat, l=lang, c=cible, e=element))
    base = os.path.join(d, "base.tsv")
    write(base, "cle\tetat\tvu\tnote\nparts|part_pick_2|perdu\tnouveau\ttour-2\t\n")
    text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-6")
    check("[même bouton (tour-2)] grave ×2 (A/a#1, A/b#1) dialogue:part_header_menu · « part_pick_2 » · rien" in text,
          "the element is the target (two languages, one key) and a button for « même bouton »:\n" + text)
    check("[nouveau] grave ×2 (A/c#1, A/d#1) dialogue:part_header_menu · « partie c » · rien" in text,
          "no element, or a navigation tag as element: the free text stays the target:\n" + text)
    check(text.splitlines()[1].startswith("4 siège(s), 2 grappe(s) : "), "two clusters:\n" + text)


REPORT_STATES = """siege: a | profil: ado | lang: fr | device: phone | font: 100 | mode: simple
defauts:
  - geste: 2 | ecran.vu: 3 | cible: Klaxon | geste.type: touche
    attendu: un son | obtenu: rien
    symptome: rien
    gravite: contourne-en-5-gestes | revu: oui: ecran 6 | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: ? | geste.type: touche
    attendu: l'aide du Pad Live | obtenu: rien
    symptome: rien
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: ? | geste.type: touche
    attendu: « Aide » | obtenu: « ? »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: REC | geste.type: touche
    attendu: enregistrer | obtenu: rien
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: REC | geste.type: touche
    attendu: tout de suite | obtenu: 40 s
    symptome: lent
    gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: Klaxon | geste.type: touche
    attendu: propre | obtenu: le son est mauvais
    symptome: rien
    gravite: finition | revu: non | banc?: non | promesse: -
"""


def header_counts(d):
    """PLAN-RESTE 6.4: the fusion's header counts apart, as a partition of the clusters, the new ones, those seen
    before (by round), the same button (by round), the comebacks, the bench's doubts and the base's other states, and
    the cards set aside (sound judgments); « nouvelle » no longer includes a « même bouton » cluster."""
    write(os.path.join(d, "lotA", "a", "journal.txt"), JOURNAL_A)
    write(os.path.join(d, "lotA", "a", "rapport.md"), REPORT_STATES)
    base = os.path.join(d, "base.tsv")
    write(base, "cle\tetat\tvu\tnote\n"
                "padlive|padlive_pad_4|rien\tnouveau\ttour-1\t\n"
                "settings|help_padlive|perdu\tnouveau\ttour-2\t\n"
                "padlive|help_padlive|texte-faux\tvoulu\ttour-3\t\n")  # the button's earliest round stays tour-2
    text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-5")
    head = text.splitlines()[1]
    check(head.startswith("1 siège(s), 5 grappe(s) : 1 nouvelle(s), 1 déjà vue(s) (tour-1 : 1), 1 même bouton (tour-2 : 1), "),
          "new, seen before and same button counted apart, by round:\n" + head)
    check(", 0 qui revien(nen)t (confirmé par un lecteur), 0 candidat(s) « revient ? »" in head, "comebacks and candidates:\n" + head)
    check(", 1 [banc?] (" in head and ", 1 autre(s) état(s) de la base (voulu : 1) ;" in head, "the bench's doubts and the base's states:\n" + head)
    check("; 1 fiche(s) rejetée(s) (jugement sonore) ;" in head and "Écartés : A/a#2 (jugement sonore)." in head, "the rejected cards:\n" + head)
    check("[même bouton (tour-2)]" in text and "[déjà vu (tour-1)]" in text and "[banc?]" in text, "the states themselves:\n" + text)


def comebacks(d):
    """Review of session 2: « revient ? » only at a round after the fix, confirmed by a reader in revient.tsv; a v1 card
    whose symptom is a state of the screen keeps the screen's route and its own target (not the previous gesture's button)."""
    tour = os.path.join(d, "tour-4", "lotA", "a")
    os.makedirs(tour)
    write(os.path.join(tour, "journal.txt"), JOURNAL_A)
    write(os.path.join(tour, "rapport.md"), REPORT_A.replace("symptome: incompris", "symptome: texte-coupe"))
    rnd = os.path.dirname(os.path.dirname(tour))
    base = os.path.join(d, "base.tsv")
    write(base, "cle\tetat\tvu\tnote\tcorrige_au\npadlive|klaxon|rien\tcorrigé\ttour-2\tn° 1\ttour-3\n")
    text = run(os.path.join(HERE, "fuse-reports.py"), rnd, "--base", base, "--tour", "tour-4")
    check("[revient ?] grave ×1 (A/a#4) padlive · « klaxon » · rien" in text, "fixed at round 3, seen at round 4: a candidate:\n" + text)
    check("padlive · « rec » · affichage" in text, "a screen's state keeps the screen and its target, not the previous button:\n" + text)
    text = run(os.path.join(HERE, "fuse-reports.py"), rnd, "--base", base, "--tour", "tour-3")
    check("[nouveau] grave ×1 (A/a#4) padlive · « klaxon »" in text, "fixed at round 3, seen at round 3: that round's own defect:\n" + text)
    write(os.path.join(d, "revient.tsv"), "tour\tcle\trevient\ntour-4\tpadlive|klaxon|rien\toui\n")
    text = run(os.path.join(HERE, "fuse-reports.py"), rnd, "--base", base, "--tour", "tour-4")
    check("[revient] grave ×1 (A/a#4)" in text and ", 1 qui revien(nen)t (confirmé par un lecteur), " in text, "confirmed by a reader:\n" + text)
    write(os.path.join(d, "revient.tsv"), "tour\tcle\trevient\ntour-4\tpadlive|klaxon|rien\tnon\n")
    text = run(os.path.join(HERE, "fuse-reports.py"), rnd, "--base", base, "--tour", "tour-4")
    check("[nouveau] grave ×1 (A/a#4)" in text, "refused by a reader: another defect:\n" + text)


def seats_from_ids(d):
    """Banc fiable 2.3: a seat's promises from catalogue ids and free texts; an id twice is refused."""
    tour = os.path.join(d, "tour-9")
    os.makedirs(tour)
    write(os.path.join(tour, "sieges-proposes.tsv"),
          "A\ta\tfr\tphone\t100\tsimple\t-\tpadlive\t40\tEnfant\tP006 ; libre: le pad 2 sonne\n")
    out = run(os.path.join(HERE, "fabrique-sieges.py"), tour)
    check("1 sièges" in out, out)
    brief = open(os.path.join(tour, "briefs", "Aa.txt"), encoding="utf-8").read()
    check("id=P006 | « Pad Live · NOUVEAU" in brief and "id=t9-Aa-1 | le pad 2 sonne" in brief, "ids resolved:\n" + brief)
    check("P006 ; t9-Aa-1" in open(os.path.join(tour, "sieges.tsv"), encoding="utf-8").read(), "sieges.tsv keeps the ids")
    write(os.path.join(tour, "sieges-proposes.tsv"), "A\ta\tfr\tphone\t100\tsimple\t-\tpadlive\t40\tEnfant\tP006 ; P6\n")
    r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "fabrique-sieges.py"), tour], capture_output=True, text=True)
    check(r.returncode != 0 and "twice" in (r.stdout + r.stderr), "the same id twice is refused")
    # PLAN-RESTE 6.4: more than 5 promises is refused, naming the seat (it used to truncate to 5 without a word).
    write(os.path.join(tour, "sieges-proposes.tsv"), "A\ta\tfr\tphone\t100\tsimple\t-\tpadlive\t40\tEnfant\tP001 ; P002 ; P003 ; P004 ; P005 ; libre: le pad 2 sonne\n")
    r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "fabrique-sieges.py"), tour], capture_output=True, text=True)
    check(r.returncode != 0 and "Aa: 6 promises, 5 at most" in (r.stdout + r.stderr), "6 promises are refused, the seat named:\n" + r.stdout + r.stderr)
    write(os.path.join(tour, "sieges-proposes.tsv"), "A\ta\tfr\tphone\t100\tsimple\t-\tpadlive\t40\tEnfant\tP001 ; P002 ; P003 ; P004 ; P005\n")
    out = run(os.path.join(HERE, "fabrique-sieges.py"), tour)
    check("1 sièges" in out and open(os.path.join(tour, "briefs", "Aa.txt"), encoding="utf-8").read().count("  - id=") == 5, "5 promises pass whole")


def bench_guards(d):
    """Banc fiable 1.2 and 1.3: the self-test's verdict test by test, and every bench answer naming its seat."""
    act = os.path.join(HERE, "..", "human-act.py")
    seat = os.path.join(d, "lotE", "c")
    os.makedirs(seat)
    write(os.path.join(seat, "start.txt"), "lang=fr\n")
    write(os.path.join(seat, "turn"), "1")
    write(os.path.join(seat, "screen-1.txt"), "Écran 1 : Que veux-tu faire ?")
    out = run(act, seat, "regarde")
    check(out.startswith("Siège lotE/c · écran 1\n"), "the answer names its seat first:\n" + out)
    stray = os.path.join(d, "helper", "c")
    os.makedirs(stray)
    write(os.path.join(stray, "start.txt"), "lang=fr\n")
    r = subprocess.run([sys.executable, "-I", act, stray, "regarde"], capture_output=True, text=True)
    check(r.returncode == 2 and "Siège refusé" in r.stdout, "a folder outside …/lot<X>/<a-f>/ is refused:\n" + r.stdout)

    # The stamp written by selftest.sh is the one human-tester.sh recognises (clean tree: the empty diff line kept).
    tampon = os.path.join(HERE, "tampon.sh")
    fp = os.path.join(d, "fp.txt")
    write(fp, subprocess.run([tampon, "empreinte"], capture_output=True, text=True, check=True).stdout)
    green_lot(d)
    subprocess.run([tampon, "ecris", d, fp], check=True)
    check(subprocess.run([tampon, "verifie", d]).returncode == 0, "a fresh stamp fits this code:\n" + open(os.path.join(d, "selftest.ok")).read())
    other = os.path.join(d, "autre-lot")
    os.makedirs(other)
    import shutil
    shutil.copy(os.path.join(d, "selftest.ok"), other)
    check(subprocess.run([tampon, "verifie", other]).returncode != 0, "a stamp copied to another lot is refused (external audit)")
    os.makedirs(os.path.join(d, "z"))
    write(os.path.join(d, "z", "start.txt"), "lang=de\n")
    check(subprocess.run([tampon, "verifie", d]).returncode != 0, "a lot whose seats changed is refused")
    os.remove(os.path.join(d, "z", "start.txt"))
    with open(os.path.join(d, "selftest.ok"), "w") as f:
        f.write("0000000000000000000000000000000000000000\n\n")
    check(subprocess.run([tampon, "verifie", d]).returncode != 0, "a stamp of another commit does not")

    verdict = os.path.join(HERE, "selftest-verdict.py")
    res = os.path.join(d, "results")
    os.makedirs(res)
    quarantine = os.path.join(d, "q.txt")
    write(quarantine, "# none\n")
    summary = os.path.join(d, "resume.txt")

    def results(cases):
        write(os.path.join(res, "TEST-x.xml"), "<testsuite>" + cases + "</testsuite>")
        r = subprocess.run([sys.executable, "-I", verdict, res, quarantine, summary], capture_output=True, text=True)
        with open(summary, encoding="utf-8") as f:
            return r.returncode, f.read()

    green = '<testcase classname="a.BenchOneTest" name="plays"/>'
    red = '<testcase classname="a.Tour3RenameIdleTest" name="renames"><failure message="timeout"/></testcase>'
    skipped = '<testcase classname="a.BenchTwoTest" name="mic"><skipped message="no microphone on Robolectric"/></testcase>'
    code, text = results(green + skipped)
    check("sauté : BenchTwoTest#mic : no microphone on Robolectric" in text, "the skip's reason is listed:\n" + text)
    code, text = results(green + red)
    check(code == 1 and "rouge : Tour3RenameIdleTest#renames : timeout" in text, "one red forbids the lot:\n" + text)
    write(quarantine, "Tour3RenameIdleTest#renames | order-dependent | 2026-11-08 | banc\n")
    code, text = results(green + red)
    check(code == 0 and "quarantaine rouge : Tour3RenameIdleTest#renames" in text, "a quarantined red lets it pass, named:\n" + text)
    code, text = results("")
    check(code == 1, "no test run at all is never green")
    # Review of session 1: a class expected but without results (its run never ran) is red; so is a failed Gradle run
    # with no red test to show.
    expected = os.path.join(d, "attendus.txt")
    write(expected, "BenchOneTest\nTour3RenameIdleTest\n")
    write(quarantine, "# none\n")
    write(os.path.join(res, "TEST-x.xml"), "<testsuite>" + green + "</testsuite>")
    r = subprocess.run([sys.executable, "-I", verdict, res, quarantine, summary, expected], capture_output=True, text=True)
    text = open(summary, encoding="utf-8").read()
    check(r.returncode == 1 and "rouge : Tour3RenameIdleTest#* : aucun résultat" in text, "an expected class that never ran:\n" + text)
    write(expected, "BenchOneTest\n")
    failed = os.path.join(d, "echecs.txt")
    write(failed, "renommage\n")
    r = subprocess.run([sys.executable, "-I", verdict, res, quarantine, summary, expected, failed], capture_output=True, text=True)
    text = open(summary, encoding="utf-8").read()
    check(r.returncode == 1 and "gradle:renommage" in text, "a failed Gradle run without a red test:\n" + text)
    # PLAN-RESTE 4.3: judged pass by pass: the « renommage » pass failed in Gradle with no red of its own while the
    # « banc » pass has a quarantined red: still red (the quarantine of one pass never covers another).
    os.remove(os.path.join(res, "TEST-x.xml"))
    write(os.path.join(res, "banc", "TEST-b.xml"), "<testsuite>" + green + red + "</testsuite>")
    os.makedirs(os.path.join(res, "renommage"))
    write(quarantine, "Tour3RenameIdleTest#renames | order-dependent | 2026-11-08 | banc\n")
    r = subprocess.run([sys.executable, "-I", verdict, res, quarantine, summary, expected, failed], capture_output=True, text=True)
    text = open(summary, encoding="utf-8").read()
    check(r.returncode == 1 and "gradle:renommage" in text, "a failed pass is red even with a quarantined red elsewhere:\n" + text)


GREEN_CASE = '<testcase classname="a.BenchOneTest" name="plays"/>'


def green_lot(lot, cases=GREEN_CASE):
    """A lot whose self-test ran: its JUnit results (one pass) and the summary selftest-verdict.py makes of them, with
    the machine's line selftest.sh adds."""
    write(os.path.join(lot, "junit", "banc", "TEST-x.xml"), "<testsuite>" + cases + "</testsuite>")
    resume = os.path.join(lot, "selftest-resume.txt")
    subprocess.run([sys.executable, "-I", os.path.join(HERE, "selftest-verdict.py"), os.path.join(lot, "junit"),
                    os.path.join(HERE, "selftest-quarantaine.txt"), resume], capture_output=True, text=True)
    with open(resume, "a", encoding="utf-8") as f:
        f.write("machine : 50 Go disque, 8 Go mémoire, 0 démon(s), 0 orphelin\n")
    return resume


def stamp_needs_green_summary(d):
    """Audit « banc crédible » 2, P2: `tampon.sh ecris` refuses a lot without a green self-test summary (no summary, a
    red one, one that its JUnit results do not give), and the stamp seals that summary and those results: `verifie`
    recomputes both (and the verdict of the results) and refuses a lot where either changed."""
    tampon = os.path.join(HERE, "tampon.sh")
    lot = os.path.join(d, "tampon-p2", "lotT")
    write(os.path.join(lot, "a", "start.txt"), "lang=fr\n")
    fp = os.path.join(d, "tampon-p2", "fp.txt")
    write(fp, subprocess.run([tampon, "empreinte"], capture_output=True, text=True, check=True).stdout)
    stamp = os.path.join(lot, "selftest.ok")

    def ecris():
        if os.path.exists(stamp):
            os.remove(stamp)
        r = subprocess.run([tampon, "ecris", lot, fp], capture_output=True, text=True)
        return r.returncode == 0 and os.path.isfile(stamp)

    def verifie():
        return subprocess.run([tampon, "verifie", lot], capture_output=True, text=True).returncode == 0

    check(not ecris(), "no self-test summary: no stamp (the audit's forge: ecris without any test)")
    resume = green_lot(lot, GREEN_CASE + '<testcase classname="a.BenchTwoTest" name="mic"><failure message="no"/></testcase>')
    check(not ecris(), "a red summary: no stamp:\n" + open(resume, encoding="utf-8").read())
    resume = green_lot(lot)
    with open(resume, "a", encoding="utf-8") as f:
        f.write("vert : BenchAddedByHandTest#guards\n")
    check(not ecris(), "a summary its JUnit results do not give (a line added by hand): no stamp")
    resume = green_lot(lot)
    check(ecris() and verifie(), "a green summary of its results: stamped, and the stamp fits:\n" + open(stamp, encoding="utf-8").read())
    with open(resume, "a", encoding="utf-8") as f:
        f.write("vert : BenchAddedByHandTest#guards\n")
    check(not verifie(), "a summary changed after the stamp is refused")
    resume = green_lot(lot)
    check(ecris() and verifie(), "stamped again")
    write(os.path.join(lot, "junit", "banc", "TEST-y.xml"), "<testsuite>" + GREEN_CASE.replace("plays", "other") + "</testsuite>")
    check(not verifie(), "results changed after the stamp are refused")


REPORT_V2 = """siege: a | profil: ado-ko | lang: ko | device: phone | font: 100 | mode: simple
mission: padlive | buts: son,partie,partage
promesse: id=padlive_hint | texte: 한 번 터치로 소리가 나요 | ecran: 2 | tenue: non | pourquoi: rien ne sonne
hesitations:
  - ecran: 1 | cherchais: la cible du jeu | regarde: en haut
incidents-banc:
  - geste: 5 | attente: 70 | vu: le banc n'a pas rendu d'écran
defauts:
  - geste: 2 | ecran.vu: 3 | cible: 경적 | geste.type: touche
    attendu: un son | obtenu: rien
    symptome: promesse
    gravite: grave | revu: non | banc?: non | promesse: padlive_hint
  - geste: 1 | ecran.vu: 2 | cible: 全部止める | geste.type: touche
    attendu: tout s'arrête | obtenu: rien
    symptome: rien
    gravite: grave | revu: oui: ecran 6 | banc?: non | promesse: -
"""

REPORT_V2_FR = """siege: b | profil: parent | lang: fr | device: phone | font: 100 | mode: simple
defauts:
  - geste: 2 | ecran.vu: 3 | cible: Klaxon | geste.type: touche
    attendu: un son | obtenu: rien
    symptome: rien
    gravite: grave | revu: oui: ecran 4 | banc?: non | promesse: -
"""


def fusion_v2(d):
    """Template v2: the key is the journal's tag at the gesture cited, the same for a Korean and a French seat; non-Latin
    targets are kept; a hesitation that says « cible » is no defect; promises and bench incidents are listed; a key
    « corrigé » in the base comes back as [revient], a « banc » verdict seen again as [banc contredit]."""
    for seat, report in (("a", REPORT_V2), ("b", REPORT_V2_FR)):
        write(os.path.join(d, "lotA", seat, "journal.txt"), JOURNAL_A)
        write(os.path.join(d, "lotA", seat, "rapport.md"), report)
    base = os.path.join(d, "base.tsv")
    write(base, "cle\tetat\tvu\tnote\npadlive|padlive_pad_4|rien\tcorrigé\ttour-3\t\n")
    verdicts = os.path.join(d, "verdicts.tsv")
    write(verdicts, "tour\titem\tcle\tverdict\tpreuve_appli\tgarde_banc\ttelephone\tcommit\n"
                    "tour-3\t2\tpadlive|全部止める|rien\tbanc\tX\tY\t-\tabc\n")
    text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--verdicts", verdicts)
    check("[revient ?] grave ×2 (A/a#2, A/b#2) padlive · « padlive_pad_4 » · rien" in text, "a Korean and a French seat, one key, back (a candidate):\n" + text)
    check("« 全部止める »" in text and "[banc contredit ?]" in text, "non-Latin target kept, bench verdict contradicted (a candidate):\n" + text)
    check(text.splitlines()[1].startswith("2 siège(s), 2 grappe(s) : "), "the hesitation saying « cible » is no defect:\n" + text)
    promises = open(os.path.join(d, "promesses.tsv"), encoding="utf-8").read()
    check("A/a\tpadlive_hint\t" in promises and "\tnon\t" in promises, "the promise listed:\n" + promises)
    incidents = open(os.path.join(d, "incidents-banc.tsv"), encoding="utf-8").read()
    check("A/a\t5\t70" in incidents, "the bench incident listed:\n" + incidents)
    check(os.path.isfile(os.path.join(d, "clusters-complet.md")), "every cluster kept in clusters-complet.md")
    # The dashboard: computed from the same files; a missing number never meets a condition.
    write(os.path.join(d, "hors-banc.tsv"), "ci_build\tvert\nci_emulateur\trouge\nrecette_points_rouges\t0\n")
    board = run(os.path.join(HERE, "tableau-de-bord.py"), d, "--base", base, "--verdicts", verdicts)
    check("| A | Items ≥ grave côté appli, non tranchés ou non faits | 2 | 0 | non |" in board, "A counts the two graves without a verdict of this round:\n" + board)
    check("| H |" in board and "vert/rouge, 0" in board and "| G |" in board and "?, ?, ?, " in board, "H read, G unknown but the uncommitted files (measured):\n" + board)
    check("Arrêt de l'audit possible : non" in board, "no stop on one round:\n" + board)
    # Banc fiable 1.6: an item « corrigé » in verdicts.tsv no longer counts in A; a « banc? » card ≥ medium without its
    # verdict line makes B « ? ».
    name = os.path.basename(os.path.normpath(d))
    with open(verdicts, "a", encoding="utf-8") as f:
        f.write(f"{name}\tx\tpadlive|padlive_pad_4|rien\tappli\tT\t-\t-\tdef\tcorrigé\n")
    lines = open(verdicts, encoding="utf-8").read().splitlines()
    lines[0] += "\tetat"
    lines[1] += "\touvert"
    write(verdicts, "\n".join(lines) + "\n")
    board = run(os.path.join(HERE, "tableau-de-bord.py"), d, "--base", base, "--verdicts", verdicts)
    check("| A | Items ≥ grave côté appli, non tranchés ou non faits | 1 | 0 | non |" in board, "the fixed item left A:\n" + board)
    check("; 0 | ≤ 2, prouvés ; 0 |" in board, "no « banc? » card yet:\n" + board)
    write(os.path.join(d, "lotA", "c", "journal.txt"), JOURNAL_A)
    write(os.path.join(d, "lotA", "c", "rapport.md"), REPORT_V2_FR.replace("siege: b", "siege: c").replace("Klaxon", "Pad Live").replace("geste: 2", "geste: 4").replace("banc?: non", "banc?: son"))
    board = run(os.path.join(HERE, "tableau-de-bord.py"), d, "--base", base, "--verdicts", verdicts)
    check("; 1 | ≤ 2, prouvés ; 0 | ? |" in board, "a « banc? » card without verdict: B unknown:\n" + board)
    # Judged « appli » by the bench team, that « banc? » cluster counts in A again (review of session 1).
    with open(verdicts, "a", encoding="utf-8") as f:
        f.write(f"{name}\ty\tpadlive|help_padlive|rien\tappli\tT\t-\t-\t-\touvert\n")
    board = run(os.path.join(HERE, "tableau-de-bord.py"), d, "--base", base, "--verdicts", verdicts)
    check("| A | Items ≥ grave côté appli, non tranchés ou non faits | 2 | 0 | non |" in board, "the judged « banc? » counts in A:\n" + board)
    check("; 0 | ≤ 2, prouvés ; 0 |" in board, "and no longer waits for a verdict in B:\n" + board)


def chantier(d):
    """verifie-chantier.py (PLAN-RESTE 4.0): a « banc » line without a Bench* guard, a Bench* test given as
    the app's proof, a missing method, a @KnownDefect nobody cites and a checklist item nobody cites are gaps."""
    root = judge_root(d, "chantier")
    ultra = os.path.join(root, "docs", "audits", "ultra")
    resume = os.path.join(root, "selftest-resume.txt")
    script = os.path.join(HERE, "verifie-chantier.py")
    out = run(script, "--root", root, "--sans-git", "--resume", resume)
    check("chantier : 0 écarts" in out, "a clean work: 0 gap\n" + out)
    bad = (JUDGE_HEAD + "t\tA\tk1\tm\tappli\touvert\tBenchFooTest#guardsIt\t-\trecette tel-01\t-\tn\t-\n"
           "t\tB\tk2\tm\tbanc\touvert\t-\t-\t-\t-\tn\t-\n"
           "t\tC\tk3\tm\tappli\touvert\tOther#provesIt\t-\t-\t-\tn\t-\n")
    write(os.path.join(ultra, "verdicts.tsv"), bad)
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", resume], capture_output=True, text=True)
    out = r.stdout
    check(r.returncode == 1, "gaps fail the run")
    check("A : preuve d'appli BenchFooTest#guardsIt est un test du banc" in out, out)
    check("B : verdict « banc » sans garde Bench*" in out, out)
    check("C : preuve d'appli Other#provesIt introuvable" in out, "a method of another class of the same file is not this class's\n" + out)
    check("@KnownDefect K1 (AppProofTest#provesIt) sans ligne" in out, out)
    check("tel-02 de recette.json cité par aucune ligne" in out, out)


def judge_criteria(d):
    """Audit « banc crédible » A.10 (M1), five cases: an « appli » line without a proof, a « banc » line without a
    proof without the bench, no summary of the self-test, a crossed rouge/vert pair, a « corrigé » without a commit."""
    script = os.path.join(HERE, "verifie-chantier.py")

    def judge(root, *args):
        r = subprocess.run([sys.executable, "-I", script, "--root", root, *args], capture_output=True, text=True)
        return r.returncode, r.stdout + r.stderr

    def lines(*rows):
        return JUDGE_GOOD + "".join("\t".join(r) + "\n" for r in rows)

    # 1. « appli » needs a test without the bench that exists, or a dated ❌ trial on a device.
    root = judge_root(d, "juge-appli", lines(
        ("t", "C", "k3", "m", "appli", "ouvert", "-", "-", "recette tel-01", "-", "read in the code", "-"),
        ("t", "D", "k4", "m", "appli", "ouvert", "-", "-", "recette tel-01", "-", "RECETTE tel-01 ❌ téléphone 2026-11-08 : nothing plays", "-"),
        ("t", "E", "k5", "m", "appli", "ouvert", "-", "-", "recette tel-01", "-", "RECETTE tel-01 ✅ téléphone 2026-11-08 : it plays", "-"),
        ("t", "F", "k6", "m", "appli", "ouvert", "-", "-", "recette tel-01", "-", "RECETTE tel-01 ❌ téléphone : no date", "-"),
        ("t", "H", "k8", "m", "appli", "ouvert", "-", "-", "recette tel-01", "-", "❌ téléphone 2026-11-08 : no trial number in this segment", "-")))
    resume = os.path.join(root, "selftest-resume.txt")
    code, out = judge(root, "--sans-git", "--resume", resume)
    for item in ("C", "E", "F", "H"):
        check(f"{item} : verdict « appli » sans preuve (ni test sans banc existant, ni essai ❌ daté)" in out, f"appli line {item} has no proof:\n" + out)
    check("D : verdict « appli »" not in out and "chantier : 5 écarts" in out and code == 1, "a dated ❌ trial is a proof:\n" + out)
    check(NO_NOTES + " (3 annotation(s) d'essai" in out, "trial annotations without the owner's notes to check them against: said\n" + out)

    # 2. « banc » needs a proof without the bench too (a Bench* guard alone is the bench judging itself).
    root = judge_root(d, "juge-banc", lines(
        ("t", "G", "k7", "m", "banc", "ouvert", "-", "BenchFooTest#guardsIt", "recette tel-02", "-", "n", "-")))
    code, out = judge(root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt"))
    check("G : verdict « banc » sans preuve sans banc" in out and "chantier : 1 écarts" in out, "a banc line without a proof without the bench:\n" + out)

    # 3. The summary of the latest self-test is mandatory.
    root = judge_root(d, "juge-resume")
    code, out = judge(root, "--sans-git")
    check(code == 1 and "résumé absent : --resume <selftest-resume.txt> est obligatoire" in out, "no --resume is a gap:\n" + out)
    code, out = judge(root, "--sans-git", "--resume", os.path.join(root, "nowhere.txt"))
    check(code == 1 and "résumé absent : " in out and "nowhere.txt introuvable" in out, "a summary that does not exist is a gap:\n" + out)

    # 4. A crossed pair: the green of another test never closes a red; a checked line of the journal names its pair.
    root = judge_root(d, "juge-paires")
    resume = os.path.join(root, "selftest-resume.txt")

    def git(*a):
        return subprocess.run(["git", "-C", root, "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false", *a],
                              capture_output=True, text=True, check=True).stdout.strip()

    def commit(subject):
        git("commit", "-q", "--allow-empty", "-m", subject)
        return git("rev-parse", "--short", "HEAD")

    git("init", "-q")
    red_a = commit("rouge: BenchFooTest#guardsIt the counter stops")
    red_b = commit("rouge: test_x.py::other_case a second repair")
    green_b = commit("vert: test_x.py::other_case a second repair")
    green_c = commit("vert: BenchBarTest#neverRed a repair without its red")
    code, out = judge(root, "--resume", resume)
    check(f"{red_a} « rouge: BenchFooTest#guardsIt the counter stops » : rouge sans vert du même test après lui" in out, "the red left open is the one whose test has no green:\n" + out)
    check(f"{green_c} « vert: BenchBarTest#neverRed a repair without its red » : vert sans rouge du même test avant lui" in out, "a green of another test closes nothing:\n" + out)
    check(red_b not in out and green_b not in out and "chantier : 2 écarts" in out, "the pair of the same test is fine:\n" + out)
    green_a = commit("vert: the bench's counter reads the engine (BenchFooTest#guardsIt)")
    journal = os.path.join(root, "docs", "audits", "ultra", "banc-fiable-journal.md")
    write(journal, f"# Journal\n- [x] 1.2 an old line, before the rule\n- [ ] 4.4 not done\n"
                   f"- [x] 4.2 done, preuve: rouge {red_a} / vert {green_a}\n- [x] 4.3 done, no pair\n"
                   f"- [x] 6.1 done, preuve: rouge {red_a} / vert {green_b}\n- [x] 6.4 done, preuve: script scripts/nowhere.py\n"
                   f"- [x] A.4 done, preuve: rouge {green_b} / vert {red_b}\n")
    code, out = judge(root, "--resume", resume)
    check(f"{red_a} «" not in out, "a green naming the red's test closes it:\n" + out)
    check("journal, ligne cochée 4.3 : sans preuve" in out, "a checked line without its pair:\n" + out)
    check("journal, ligne cochée 6.1 : paire" in out and "pas le même test" in out, "a checked line citing a crossed pair:\n" + out)
    check("journal, ligne cochée 6.4 : script scripts/nowhere.py introuvable" in out, "a checked line citing a script that does not exist:\n" + out)
    check("journal, ligne cochée A.4 : paire" in out, "a pair cited backwards:\n" + out)
    check("ligne cochée 4.2" not in out and "ligne cochée 1.2" not in out and "4.4" not in out and "chantier : 5 écarts" in out,
          "the line with its pair, the old line and the open line are no gaps:\n" + out)

    # 5. « corrigé » needs a commit that exists.
    head = git("rev-parse", "--short", "HEAD")
    with open(os.path.join(root, "docs", "audits", "ultra", "base.tsv"), "a", encoding="utf-8") as f:
        f.write("k8\tcorrigé\tt\tn\ttour-5\nk9\tcorrigé\tt\tn\ttour-5\nk10\tcorrigé\tt\tn\ttour-5\n")
    os.remove(journal)
    write(os.path.join(root, "docs", "audits", "ultra", "verdicts.tsv"), lines(
        ("t", "H", "k8", "m", "banc", "corrigé", "Other#elsewhere", "BenchFooTest#guardsIt", "-", "-", "n", "-"),
        ("t", "I", "k9", "m", "banc", "corrigé", "Other#elsewhere", "BenchFooTest#guardsIt", "-", "0123abc", "n", "-"),
        ("t", "J", "k10", "m", "banc", "corrigé", "Other#elsewhere", "BenchFooTest#guardsIt", "-", head, "n", "-")))
    code, out = judge(root, "--resume", resume)
    check("H : « corrigé » sans commit" in out, "a « corrigé » whose commit is « - »:\n" + out)
    check("I : « corrigé », commit 0123abc introuvable" in out, "a « corrigé » whose commit does not exist:\n" + out)
    check("J : « corrigé »" not in out and "chantier : 3 écarts" in out, "a « corrigé » with its commit (the third gap is the green without its red):\n" + out)


def known_defect_lifted(d):
    """Review of the « partage » lot, (c): a « vert: » commit that takes @KnownDefect off a test that carried it in its
    parent, and names that test, is a rouge/vert pair: the red was the guard (KnownDefectRule: the test failed with the
    defect's message). Nothing else is: the annotation taken off another test, kept, or the test deleted with it."""
    root = judge_root(d, "juge-knowndefect")
    resume = os.path.join(root, "selftest-resume.txt")
    proofs = os.path.join(root, "app", "src", "test", "java", "x", "ShareProofTest.kt")

    def git(*a, env=None):
        return subprocess.run(["git", "-C", root, "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false", *a],
                              capture_output=True, text=True, check=True, env=env).stdout.strip()

    def commit(subject, text=None, env=None):
        if text is not None:
            write(proofs, text)
        git("add", "-A")
        git("commit", "-q", "--allow-empty", "-m", subject, env=env)
        return git("rev-parse", "--short", "HEAD")

    def guard(name, carried):
        mark = f'  @KnownDefect("{name.upper()}", "still there")\n' if carried else ""
        return f"{mark}  @Test\n  fun {name}() {{}}\n"

    def share(*tests):
        return "class ShareProofTest {\n" + "".join(tests) + "}\n"

    git("init", "-q")
    # The guards were there before the rouge/vert discipline (as Tour5AppProofTest's, 4168889).
    commit("start", share(guard("keepsTheLength", True), guard("listsTheZip", True), guard("warnsNothingAfter", True)),
           env=old_commit_env())
    lifted = commit("vert: share follows the length chosen (ShareProofTest#keepsTheLength)",
                    share(guard("keepsTheLength", False), guard("listsTheZip", True), guard("warnsNothingAfter", True)))
    other = commit("vert: another repair (ShareProofTest#somethingElseEntirely)",
                   share(guard("keepsTheLength", False), guard("listsTheZip", False), guard("warnsNothingAfter", True)))
    kept = commit("vert: the proof of K1 (AppProofTest#provesIt)")
    deleted = commit("vert: no warning any more (ShareProofTest#warnsNothingAfter)",
                     share(guard("keepsTheLength", False), guard("listsTheZip", False)))
    out = subprocess.run([sys.executable, "-I", os.path.join(HERE, "verifie-chantier.py"), "--root", root, "--resume", resume],
                         capture_output=True, text=True).stdout
    check(lifted not in out, "a green that takes @KnownDefect off the test it names is a pair:\n" + out)
    for sha, what in ((other, "the annotation taken off another test than the one named"), (kept, "the annotation kept"),
                      (deleted, "the test deleted with its annotation")):
        check(f"{sha} « vert:" in out and "vert sans rouge du même test avant lui" in out, f"{what} is no red evidence:\n" + out)
    check("chantier : 3 écarts" in out, "three greens without red, nothing else:\n" + out)


def old_commit_env():
    """A commit dated before the rouge/vert discipline began (first « rouge: », 2026-11-08 18:29 UTC)."""
    env = dict(os.environ)
    env["GIT_AUTHOR_DATE"] = env["GIT_COMMITTER_DATE"] = "2026-11-08T12:00:00+00:00"
    return env


def known_defect_lifted_needs_a_red(d):
    """Review of the « pads-live » lot (moyen): a « vert: » that takes @KnownDefect off a test is a pair only when that
    guard came from a « rouge: » commit (or was there before the discipline began), when its message names the test's
    own class and method, and when it is not a merge. The three ways the reviewer broke it are gaps."""
    root = judge_root(d, "juge-knowndefect-rouge")
    resume = os.path.join(root, "selftest-resume.txt")
    tests = os.path.join(root, "app", "src", "test", "java", "x")
    share_f, other_f = os.path.join(tests, "ShareProofTest.kt"), os.path.join(tests, "OtherProofTest.kt")

    def git(*a, env=None):
        return subprocess.run(["git", "-C", root, "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false", *a],
                              capture_output=True, text=True, check=True, env=env).stdout.strip()

    def commit(subject, files=(), env=None):
        for path, text in files:
            write(path, text)
        git("add", "-A")
        git("commit", "-q", "--allow-empty", "-m", subject, env=env)
        return git("rev-parse", "--short", "HEAD")

    def guard(name, carried):
        mark = f'  @KnownDefect("{name.upper()}", "still there")\n' if carried else ""
        return f"{mark}  @Test\n  fun {name}() {{}}\n"

    def cls(name, *tests):
        return f"class {name} {{\n" + "".join(tests) + "}\n"

    def others(same, own, merged):
        return (cls("OtherProofTest", guard("sameName", same), guard("ownName", own), guard("mergedOne", merged))
                + cls("UnrelatedTest", guard("sameName", False)))

    def shares(plain, zip_):
        return cls("ShareProofTest", guard("plainTest", plain), guard("listsTheZipInMyFiles", zip_))

    git("init", "-q")
    commit("start", [(share_f, shares(False, False)), (other_f, others(True, True, True))], env=old_commit_env())
    # 1. A guard added by a commit that is no « rouge: », then lifted by a green that names it: no red evidence.
    commit("chore: tidy", [(share_f, shares(True, False))])
    chore = commit("vert: plain test now passes (ShareProofTest#plainTest)", [(share_f, shares(False, False))])
    # 2. Lifted from OtherProofTest#sameName, the message names UnrelatedTest#sameName: another test.
    other_cls = commit("vert: fix (UnrelatedTest#sameName)", [(other_f, others(False, True, True))])
    # Control: an old guard lifted by a green naming its own class and method is a pair.
    own = commit("vert: fix (OtherProofTest#ownName)", [(other_f, others(False, False, True))])
    # 3. The lift brought by a merge commit called « vert: ».
    main_branch = git("rev-parse", "--abbrev-ref", "HEAD")
    git("checkout", "-q", "-b", "side")
    commit("wip lift", [(other_f, others(False, False, False))])
    git("checkout", "-q", main_branch)
    commit("elsewhere", [(os.path.join(tests, "Elsewhere.kt"), "class Elsewhere\n")])
    git("merge", "-q", "--no-ff", "side", "-m", "vert: merged fix (OtherProofTest#mergedOne)")
    merge = git("rev-parse", "--short", "HEAD")
    # Control: a guard added by a « rouge: » (closed by its own green), lifted later by a green naming it: a pair.
    commit("rouge: the zip in Mes fichiers (ShareProofTest#listsTheZipInMyFiles)", [(share_f, shares(False, True))])
    commit("vert: the zip's guard holds the defect (ShareProofTest#listsTheZipInMyFiles)")
    from_red = commit("vert: the zip is listed (ShareProofTest#listsTheZipInMyFiles)", [(share_f, shares(False, False))])
    out = subprocess.run([sys.executable, "-I", os.path.join(HERE, "verifie-chantier.py"), "--root", root, "--resume", resume],
                         capture_output=True, text=True).stdout
    for sha, what in ((chore, "a guard added outside a « rouge: »"), (other_cls, "a guard of another class with the same method name"),
                      (merge, "a lift brought by a merge")):
        check(f"{sha} « vert:" in out, f"{what} is no red evidence:\n" + out)
    for sha, what in ((own, "an old guard lifted by a green naming its class and method"), (from_red, "a guard from a « rouge: »")):
        check(sha not in out, f"{what} is a pair:\n" + out)
    check("chantier : 3 écarts" in out, "the three adversarial greens, nothing else:\n" + out)


def open_app_proof(d):
    """Audit « banc crédible » A.2 (M3): an open « appli » line whose only proofs are green tests proves no defect.
    Its proof is a @KnownDefect guard (or an engine known_defect test) or a dated ❌ trial on a device."""
    script = os.path.join(HERE, "verifie-chantier.py")
    root = judge_root(d, "juge-appli-vert", JUDGE_GOOD
                      + "t\tC\tk3\tm\tappli\touvert\tOther#elsewhere\t-\trecette tel-01\t-\tRECETTE tel-01 ❌ téléphone : no date\t-\n"
                      + "t\tD\tk4\tm\tappli\touvert\tOther#elsewhere\t-\trecette tel-01\t-\tRECETTE tel-01 ❌ téléphone 2026-11-08 : nothing plays\t-\n"
                      + "t\tE\tk5\tm\tappli\tvoulu\tOther#elsewhere\t-\t-\t-\tn\t-\n")
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt")],
                       capture_output=True, text=True)
    out = r.stdout + r.stderr
    check("C : verdict « appli » dont la preuve n'est ni @KnownDefect ni un essai ❌ daté" in out,
          "an open appli line proved by a green test only is a gap:\n" + out)
    check("A : verdict « appli » dont" not in out and "D : verdict « appli » dont" not in out and "E : verdict « appli » dont" not in out
          and "chantier : 2 écarts" in out and r.returncode == 1 and NO_NOTES + " (2 annotation(s) d'essai" in out,
          "a @KnownDefect guard, a dated ❌ trial and a line that is not open are fine (the second gap: no notes of the owner):\n" + out)


def owner_trials(d):
    """Audit « banc crédible » A.6 (M5): each trial of retours-benjamin.md (« tel-NN ✅/❌ » under a dated « recette »
    bullet, or « recette tel-NN = ✅/❌ ») is written « RECETTE tel-NN <mark> <device> <date> » on every line citing it."""
    script = os.path.join(HERE, "verifie-chantier.py")
    root = judge_root(d, "juge-essais", JUDGE_GOOD
                      + "t\tC\tk3\tm\tnon-tranché\touvert\t-\t-\trecette tel-01\t-\tRECETTE tel-01 ✅ téléphone 2026-11-08 : it plays\t-\n"
                      + "t\tD\tk4\tm\tnon-tranché\touvert\t-\t-\trecette tel-02\t-\tRECETTE tel-02 ❌ tablette 2026-11-08 (video)\t-\n"
                      + "t\tE\tk5\tm\tnon-tranché\touvert\t-\t-\trecette tel-02\t-\tRECETTE tel-02 ✅ tablette 2026-11-08 : the mark is wrong\t-\n"
                      + "t\tF\tk6\tm\tnon-tranché\touvert\t-\t-\trecette tel-01\t-\tRECETTE tel-01 ✅ tablette 2026-11-08 : the device is wrong\t-\n"
                      + "t\tG\tk7\tm\tnon-tranché\touvert\t-\t-\trecette tel-01\t-\tRECETTE tel-01 ✅ téléphone : no date\t-\n"
                      + "t\tH\tk8\tm\tnon-tranché\touvert\t-\t-\trecette tel-01\t-\tRECETTE tel-01 ✅ téléphone ; seen 2026-11-08, in another segment\t-\n"
                      + "t\tN\tk14\tm\tnon-tranché\touvert\t-\t-\trecette tel-08\t-\tRECETTE tel-08 ❌ tablette paysage 2026-11-08 (video) : it lit up\t-\n"
                      + "t\tO\tk15\tm\tnon-tranché\touvert\t-\t-\trecette tel-08, tel-10, tel-12\t-\tn\t-\n"
                      + "t\tI\tk9\tm\tnon-tranché\touvert\t-\t-\t-\t-\tRECETTE tel-99 ❌ lune 2031-01-01 : invented\t-\n"
                      + "t\tJ\tk10\tm\tnon-tranché\touvert\t-\t-\t-\t-\tn ; RECETTE tel-01 ❌ téléphone 2026-11-08 : against the notes\t-\n"
                      + "t\tK\tk11\tm\tnon-tranché\touvert\t-\t-\tRECETTE tel-02 ❌ téléphone 2026-11-08\t-\tn\t-\n"
                      + "t\tL\tk12\tm\tnon-tranché\touvert\t-\t-\t-\t-\tRECETTE tel-07 ❌ téléphone 2026-11-07 : another day\t-\n"
                      + "t\tM\tk13\tm\tnon-tranché\touvert\t-\t-\t-\t-\tRECETTE tel-07 ❌ téléphone 2026-11-08 : as the notes say\t-\n"
                      # Review of the fix of lot 2 (its « moyen »): three invented proofs the reverse rule did not see, and that closed
                      # « appli sans preuve »: without the word RECETTE, in lower case, behind a true annotation of the same segment.
                      + "t\tP\tk16\tm\tappli\touvert\t-\t-\t-\t-\tn ; essai tel-99 ❌ lune 2031-01-01 : invented, without the word\t-\n"
                      + "t\tQ\tk17\tm\tappli\touvert\t-\t-\t-\t-\trecette tel-99 ❌ lune 2031-01-01 : invented, in lower case\t-\n"
                      + "t\tR\tk18\tm\tappli\touvert\t-\t-\t-\t-\tRECETTE tel-07 ❌ téléphone 2026-11-08 : as the notes say\t-\n"
                      + "t\tS\tk19\tm\tappli\touvert\t-\t-\t-\t-\tRECETTE tel-01 ✅ téléphone 2026-11-08 : it plays, RECETTE tel-99 ❌ lune 2031-01-01 : invented, behind a true one\t-\n"
                      + "t\tT\tk20\tm\tappli\touvert\t-\t-\t-\t-\ttel-99 seen ❌ on the moon 2031-01-01 : shaped like a trial, no annotation\t-\n")
    write(os.path.join(root, "app", "src", "main", "assets", "recette.json"),
          json.dumps({"id": "r", "milestone": "m", "items": [{"id": f"tel-{n:02d}", "title": "t"} for n in (1, 2, 8, 10, 12)]}))
    notes = os.path.join(root, "docs", "audits", "ultra", "retours-benjamin.md")
    write(notes, "# Retours\n\n- 2026-11-06, APK 1, tel-01 ✅ a remark that is no trial of the checklist.\n"
                 "- 2026-11-08, APK 2, **téléphone (Honor), recette 2/3** (captures) :\n"
                 "  - tel-01 ✅ it plays.\n  - tel-07 ❌ nothing.\n  - tel-09 not done.\n"
                 "- 2026-11-08, APK 2, tablette, **recette tel-02 = ❌ « frozen »** (video) : details.\n"
                 "- 2026-11-08, APK 2, **recette 3/3** on no device said :\n  - tel-03 ❌ nothing.\n"
                 "- 2026-11-08, APK 2, **recette tel-04 = ❌** on no device said.\n"
                 "- 2026-11-08, APK 2, **tablette, recette 1/3** :\n  - tel-09 not done.\n"
                 "A remark between two lists.\n  - tel-05 ❌ under no dated bullet.\n"
                 "- 2026-11-08, APK 2, **tablette, recette 1/3** :\n"
                 "- 2026-11-09, APK 3, tablette, a remark that is no round of the checklist :\n  - tel-06 ❌ no trial.\n"
                 "- 2026-11-08, APK 2, tablette, **tel-08 (video, read frame by frame)** : details. Résultat retenu : ❌ « it lit up ». To confirm.\n"
                 "- 2026-11-08, APK 2, **tel-10 (video)** on no device said. Résultat retenu : ❌ « nothing ».\n"
                 "- 2026-11-08, APK 2, tablette, tel-12 : lost (❌ said of something else), no result kept.\n")
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt")],
                       capture_output=True, text=True)
    out = r.stdout + r.stderr
    missing = "essai de retours-benjamin.md absent de verdicts.tsv"
    check(f"A : {missing} (attendu « RECETTE tel-01 ✅ téléphone 2026-11-08 »)" in out, "a line citing tel-01 without its trial:\n" + out)
    check(f"B : {missing} (attendu « RECETTE tel-02 ❌ tablette 2026-11-08 »)" in out, "a line citing tel-02 without its trial:\n" + out)
    check(f"E : {missing} (attendu « RECETTE tel-02 ❌ tablette 2026-11-08 »)" in out, "a trial written with the wrong mark:\n" + out)
    check(f"tel-07 ❌ téléphone 2026-11-08 : {missing} (aucune ligne ne cite tel-07)" in out, "a trial no line cites:\n" + out)
    check(f"F : {missing} (attendu « RECETTE tel-01 ✅ téléphone 2026-11-08 »)" in out, "a trial written with the wrong device:\n" + out)
    check(f"G : {missing} (attendu « RECETTE tel-01 ✅ téléphone 2026-11-08 »)" in out, "a trial written without its date:\n" + out)
    check(f"H : {missing} (attendu « RECETTE tel-01 ✅ téléphone 2026-11-08 »)" in out, "a trial whose date is in another segment:\n" + out)
    check(f"K : {missing} (attendu « RECETTE tel-02 ❌ tablette 2026-11-08 »)" in out, "a trial written with the wrong device, in the telephone cell:\n" + out)
    check(f"C : {missing}" not in out and f"D : {missing}" not in out and "tel-09" not in out and "2026-11-06" not in out
          and not any(t in out for t in ("tel-03", "tel-04", "tel-05", "tel-06"))
          and "chantier : 24 écarts" in out and r.returncode == 1, "the lines carrying their trial, and what is no trial, are fine:\n" + out)
    # The reverse (review of lot 2): an annotation « RECETTE tel-NN … » of verdicts.tsv is one of the notes' trials, whole.
    unknown = "annotation « RECETTE » sans essai correspondant dans retours-benjamin.md"
    check(f"I : {unknown} (« RECETTE tel-99 ❌ lune 2031-01-01 : invented »)" in out, "an invented trial:\n" + out)
    check(f"J : {unknown} (« RECETTE tel-01 ❌ téléphone 2026-11-08 : against the notes »)" in out, "a mark against the notes:\n" + out)
    check(f"K : {unknown} (« RECETTE tel-02 ❌ téléphone 2026-11-08 »)" in out, "another device than the notes', in the telephone cell:\n" + out)
    check(f"L : {unknown}" in out, "another date than the notes':\n" + out)
    check(f"E : {unknown}" in out and f"F : {unknown}" in out and f"G : {unknown}" in out and f"H : {unknown}" in out,
          "the annotations with a wrong mark, a wrong device, no date:\n" + out)
    # A result kept in the notes outside a round (« tablette, tel-NN … Résultat retenu : ❌ ») is a trial too.
    check(f"O : {missing} (attendu « RECETTE tel-08 ❌ tablette 2026-11-08 »)" in out and f"N : {missing}" not in out,
          "a result kept in the notes is a trial of its device:\n" + out)
    check("tel-10" not in out and "tel-12" not in out, "no device, or no result kept: no trial:\n" + out)
    check(not any(f"{item} : {unknown}" in out for item in "ABCDMNRT"), "the annotations that are trials of the notes are fine:\n" + out)
    # An invented proof is seen whatever its spelling, and proves nothing: the line stays « appli sans preuve ».
    no_proof = "verdict « appli » sans preuve (ni test sans banc existant, ni essai ❌ daté)"
    check(f"P : {unknown} (« tel-99 ❌ lune 2031-01-01 : invented, without the word »)" in out, "an invented trial without the word RECETTE:\n" + out)
    check(f"Q : {unknown} (« recette tel-99 ❌ lune 2031-01-01 : invented, in lower case »)" in out, "an invented trial in lower case:\n" + out)
    check(f"S : {unknown} (« RECETTE tel-99 ❌ lune 2031-01-01 : invented, behind a true one »)" in out
          and f"S : {unknown} (« RECETTE tel-01" not in out, "an invented trial behind a true annotation, in the same segment:\n" + out)
    check(all(f"{item} : {no_proof}" in out for item in "PQST"), "an invented trial, or a ✅ one, is no proof of a defect:\n" + out)
    check(f"R : {no_proof}" not in out, "a ❌ trial of the notes is a proof:\n" + out)
    # Notes that hold no trial at all: every annotation is unknown (the rule is not mute).
    write(notes, "# Retours\n\nNothing tried yet.\n")
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt")],
                       capture_output=True, text=True)
    check(all(f"{item} : {unknown}" in r.stdout for item in "CDMNR") and f"R : {no_proof}" in r.stdout,
          "notes without a trial: no annotation is one of theirs, none is a proof:\n" + r.stdout)
    # No notes at all: the judge says so (it was silent), as long as the verdicts carry trial annotations.
    os.remove(notes)
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt")],
                       capture_output=True, text=True)
    check(r.returncode == 1 and NO_NOTES + " (17 annotation(s) d'essai" in r.stdout, "no notes of the owner: said\n" + r.stdout)
    root = judge_root(d, "juge-sans-notes")
    out = run(script, "--root", root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt"))
    check("chantier : 0 écarts" in out, "no notes of the owner and no trial annotation: nothing to compare\n" + out)


def pad_timing_verdicts(d):
    """Audit « banc crédible » A.14 (F3, G2): the bench's pads start at once, without the grid, so a verdict on a
    pad of Pad Live needs a proof off the bench: a test outside Bench*, an engine test or a dated trial on a device."""
    script = os.path.join(HERE, "verifie-chantier.py")
    root = judge_root(d, "juge-pads", JUDGE_GOOD
                      + "t\tC\tpadlive_pads|padlive_pad_N|rien\tm\tappli\tvoulu\t-\t-\t-\t-\tread in the code\t-\n"
                      + "t\tD\tpadlive|padlive_stop_all|rien\tm\tappli\tvoulu\t-\t-\t-\t-\tRECETTE tel-01 ✅ téléphone 2026-11-08 : it stops\t-\n"
                      + "t\tE\tpadlive_pads|padlive_pad_N|lent\tm\tappli\tvoulu\tOther#elsewhere\t-\t-\t-\tn\t-\n"
                      + "t\tF\tpadlive|padlive_fx|rien\tm\tnon-tranché\touvert\t-\t-\t-\t-\tn\t-\n"
                      + "t\tG\tlaunch|launch_padlive|rien\tm\tappli\tvoulu\t-\t-\t-\t-\tRECETTE tel-01 ✅ téléphone : no date\t-\n"
                      + "t\tH\trhythm|pad_3|rien\tm\tappli\tvoulu\t-\t-\t-\t-\tread in the code\t-\n"
                      + "t\tI\tnotes|notepad_x|rien\tm\tappli\tvoulu\t-\t-\t-\t-\tread in the code\t-\n"
                      + "t\tJ\tpadlive|padlive_fx|rien\tm\tbanc\tvoulu\t-\t-\t-\t-\tread in the code\t-\n"
                      + "t\tK\tpadlive|padlive_fx|lent\tm\tappli\tvoulu\t-\t-\t-\t-\ttel-01 téléphone 2026-11-08 : no mark\t-\n"
                      + "t\tL\tpadlive|padlive_fx|perdu\tm\tappli\tvoulu\t-\t-\trecette tel-01\t-\t✅ téléphone 2026-11-08 : no trial number in this segment\t-\n")
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt")],
                       capture_output=True, text=True)
    out = r.stdout + r.stderr
    gap = "verdict pad-timing sans preuve hors banc"
    check(f"H : {gap}" in out and f"I : {gap}" not in out, "a « pad_ » key is a pad, « notepad_ » is not:\n" + out)
    check(f"J : {gap}" in out, "a « banc » verdict on a pad needs its proof off the bench too:\n" + out)
    check(f"K : {gap}" in out and f"L : {gap}" in out, "a trial without its mark, or without its number, is no dated trial:\n" + out)
    check(f"C : {gap}" in out and f"G : {gap}" in out, "a pad verdict read in the code, or with an undated trial, is a gap:\n" + out)
    check(f"D : {gap}" not in out and f"E : {gap}" not in out and f"F : {gap}" not in out and f"A : {gap}" not in out and f"B : {gap}" not in out,
          "a dated trial, a test outside Bench*, an undecided line and a line that is not about pads are fine:\n" + out)


def engine_proof(d):
    """Audit « banc crédible » A.12 (G5): an engine host test is a proof the judge reads (« path#function », the
    function defined and registered in a PS_*_TESTS list); a @KnownDefect id is tied to its line by « garde_id »."""
    script = os.path.join(HERE, "verifie-chantier.py")
    cell = "engine/host-tests/pads_tests.inc#knownDefectWaits (known_defect_k2, to invert once fixed)"
    inc = ("namespace pads {\nstatic void knownDefectWaits() {\n}\nstatic void other() {\n}\n}\n"
           '#define PS_PADS_TESTS \\\n    {"known_defect_k2", pads::knownDefectWaits}, \\\n    {"pads_other", pads::other},\n')

    def judge(root):
        r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", os.path.join(root, "selftest-resume.txt")],
                           capture_output=True, text=True)
        return r.stdout + r.stderr

    def lines(*rows):
        return JUDGE_GOOD + "".join("\t".join(r) + "\n" for r in rows)

    root = judge_root(d, "juge-moteur", lines(("t", "C", "k3", "m", "appli", "ouvert", cell, "-", "-", "-", "n", "K2")))
    path = os.path.join(root, "engine", "host-tests", "pads_tests.inc")
    write(path, inc)
    out = judge(root)
    check("chantier : 0 écarts" in out, "an engine host test, defined and registered, is a proof:\n" + out)
    write(path, inc.replace("knownDefectWaits", "padWaits"))
    out = judge(root)
    check("C : preuve d'appli engine/host-tests/pads_tests.inc#knownDefectWaits introuvable" in out
          and "C : verdict « appli » sans preuve" in out, "the C++ function renamed is a gap:\n" + out)
    write(path, inc.replace('{"known_defect_k2", pads::knownDefectWaits}, ', ""))
    out = judge(root)
    check("C : preuve d'appli engine/host-tests/pads_tests.inc#knownDefectWaits non enregistrée dans une liste PS_*_TESTS" in out,
          "a function no list runs is a gap:\n" + out)
    write(path, inc.replace("known_defect_k2", "pads_waits"))
    out = judge(root)
    check("C : garde_id K2 porté par aucune de ses preuves" in out and "C : verdict « appli » dont la preuve n'est ni @KnownDefect" in out
          and "chantier : 2 écarts" in out, "an engine guard no longer named known_defect_<id> is a gap, and proves no open defect:\n" + out)
    write(path, inc)

    # An id no line carries; a line's id no proof carries.
    kt = os.path.join(root, "app", "src", "test", "java", "x", "AppProofTest.kt")
    with open(kt, encoding="utf-8") as f:
        source = f.read()
    write(kt, source.replace('@KnownDefect("K1"', '@KnownDefect("K9"'))
    out = judge(root)
    check("@KnownDefect K9 (AppProofTest#provesIt) : aucune ligne qui le cite ne porte garde_id K9" in out, "an unknown id in @KnownDefect is a gap:\n" + out)
    check("A : garde_id K1 porté par aucune de ses preuves" in out and "chantier : 2 écarts" in out, "a line's id that no proof carries is a gap:\n" + out)

    # The real cell of T5-PADRETRIG reads as one proof, and the C++ runner says a known defect is one.
    repo = os.path.dirname(os.path.dirname(HERE))
    out = run(script, "--root", repo, "--preuves", "T5-PADRETRIG")
    check(out.strip() == "T5-PADRETRIG : 1 preuve(s) : engine/host-tests/padlive_tests.inc#knownDefectPadRetriggerWaitsForTheGrid (known_defect_t5_padretrig)",
          "the engine proof of T5-PADRETRIG is read:\n" + out)
    with open(os.path.join(repo, "engine", "host-tests", "engine_tests.cpp"), encoding="utf-8") as f:
        runner = f.read()
    check('"known_defect_"' in runner and "KNOWN DEFECT still there" in runner, "the C++ runner knows the known_defect_ prefix")


def expected_classes(d):
    """Audit « banc crédible » A.3 (G4): the self-test expects and runs every class that carries a @KnownDefect,
    whatever its name's digits and wherever it lives (selftest-attendus.py: « pass fqcn » lines)."""
    script = os.path.join(HERE, "selftest-attendus.py")
    root = os.path.join(d, "attendus")
    java = os.path.join(root, "app", "src", "test", "java", "com", "x", "app")
    write(os.path.join(java, "human", "BenchFooTest.kt"), "package com.x.app.human\nclass BenchFooTest {\n}\n")
    write(os.path.join(java, "human", "Tour5AppProofTest.kt"), "package com.x.app.human\nclass Tour5AppProofTest {\n}\n"
          "/** The @KnownDefect(id) guard. */\nannotation class KnownDefect(val id: String, val message: String)\nclass KnownDefectRule\n")
    write(os.path.join(java, "human", "Tour5AppProof2Test.kt"), "package com.x.app.human\nclass Tour5AppProof2Test {\n}\n")
    write(os.path.join(java, "human", "Tour3NameFieldIdleTest.kt"), "package com.x.app.human\nclass Tour3NameFieldIdleTest {\n}\n")
    write(os.path.join(java, "human", "HumanTesterA.kt"), "package com.x.app.human\nclass HumanTesterA {\n}\n")
    write(os.path.join(java, "pads", "ChallengeTest.kt"), 'package com.x.app.pads\nclass Helper {\n}\nclass ChallengeTest {\n'
          '    @KnownDefect("K1", "m")\n    @Test\n    fun provesIt() {}\n}\n')
    write(os.path.join(java, "pads", "PlainTest.kt"), "package com.x.app.pads\n// @KnownDefect(\"K2\", \"m\") in a comment\nclass PlainTest {\n}\n")
    out = run(script, "--root", root).splitlines()
    check(out == ["banc com.x.app.human.BenchFooTest", "banc com.x.app.human.Tour5AppProof2Test", "banc com.x.app.human.Tour5AppProofTest",
                  "defauts com.x.app.pads.ChallengeTest", "renommage com.x.app.human.Tour3NameFieldIdleTest"],
          "the classes the self-test expects, by pass:\n" + "\n".join(out))
    repo = os.path.dirname(os.path.dirname(HERE))
    out = run(script, "--root", repo)
    check("banc com.pocketstudio.app.human.Tour5AppProof2Test\n" in out and "defauts com.pocketstudio.app.padlive.ChallengeControllerTest\n" in out,
          "the real list holds Tour5AppProof2Test and ChallengeControllerTest:\n" + out)
    with open(os.path.join(HERE, "selftest.sh"), encoding="utf-8") as f:
        portal = f.read()
    check("selftest-attendus.py" in portal and "run_tests defauts" in portal, "selftest.sh takes its list there and runs the « defauts » pass")
    # A @KnownDefect made green by hand: its rule fails the test, the verdict is red; a class of the list that did
    # not run is red too.
    results = os.path.join(d, "attendus-junit")
    write(os.path.join(results, "defauts", "TEST-c.xml"), '<testsuite><testcase classname="com.x.app.pads.ChallengeTest" name="provesIt">'
          '<failure message="K1 no longer fails: fixed?"/></testcase><testcase classname="com.x.app.pads.ChallengeTest" name="other"/></testsuite>')
    empty = os.path.join(d, "attendus-q.txt")
    write(empty, "")
    expected = os.path.join(d, "attendus.txt")
    write(expected, "ChallengeTest\nTour5AppProof2Test\n")
    summary = os.path.join(d, "attendus-resume.txt")
    r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "selftest-verdict.py"), results, empty, summary, expected], capture_output=True, text=True)
    with open(summary, encoding="utf-8") as f:
        text = f.read()
    check(r.returncode == 1 and "rouge : ChallengeTest#provesIt : K1 no longer fails" in text and "rouge : Tour5AppProof2Test#* : aucun résultat" in text,
          "a known defect that no longer fails and an expected class that did not run are red:\n" + text)


def skipped_class(d):
    """Audit « banc crédible » A.4 (G3): a class skipped as a whole is red, a skip outside the list of known skips is
    red, the summary names every green test, and the judge never reads a skipped guard as green."""
    verdict = os.path.join(HERE, "selftest-verdict.py")
    res = os.path.join(d, "verdict-skip")
    quarantine = os.path.join(d, "skip-q.txt")
    write(quarantine, "# none\n")
    summary = os.path.join(d, "skip-resume.txt")
    expected = os.path.join(d, "skip-attendus.txt")
    none_failed = os.path.join(d, "skip-echecs.txt")
    known = os.path.join(d, "skip-connus.txt")

    def judge(cases, *more):
        write(os.path.join(res, "banc", "TEST-x.xml"), "<testsuite>" + cases + "</testsuite>")
        r = subprocess.run([sys.executable, "-I", verdict, res, quarantine, summary, expected, none_failed, *more], capture_output=True, text=True)
        with open(summary, encoding="utf-8") as f:
            return r.returncode, f.read()

    seats = '<skipped message="not while the bench runs seats"/>'
    whole = (f'<testcase classname="a.BenchRecetteTest" name="one">{seats}</testcase>'
             f'<testcase classname="a.BenchRecetteTest" name="two">{seats}</testcase>'
             '<testcase classname="a.Tour5AppProofTest" name="proves"/>')
    write(expected, "BenchRecetteTest\nTour5AppProofTest\n")
    write(known, "BenchRecetteTest#one | seats | 2026-11-09 | test\nBenchRecetteTest#two | seats | 2026-11-09 | test\n")
    code, text = judge(whole, known)
    check(code == 1 and "rouge : BenchRecetteTest#* : aucun test non sauté" in text, "a class skipped as a whole is red, even with known skips:\n" + text)
    check("vert : Tour5AppProofTest#proves" in text, "every green test is named in the summary:\n" + text)
    part = ('<testcase classname="a.BenchRecetteTest" name="one"/>'
            f'<testcase classname="a.BenchRecetteTest" name="two">{seats}</testcase>'
            '<testcase classname="a.Tour5AppProofTest" name="proves"/>')
    code, text = judge(part)
    check(code == 1 and "rouge : BenchRecetteTest#two : saut hors de la liste des sauts connus" in text, "a skip outside the known list is red:\n" + text)
    code, text = judge(part, known)
    check(code == 0 and text.startswith("2 verts, 0 rouges, 0 en quarantaine, 1 sautés") and "sauté : BenchRecetteTest#two : not while" in text,
          "a known skip is listed and lets it pass:\n" + text)

    # A known skip named without its parameters covers every parameterised case of that method (a seat's `drive`,
    # skipped outside seat mode, has one case per profile), and no other method.
    params = ('<testcase classname="a.BenchRecetteTest" name="one"/>'
              f'<testcase classname="a.BenchRecetteTest" name="drive[lang=fr device=phone]">{seats}</testcase>'
              f'<testcase classname="a.BenchRecetteTest" name="drive[lang=de device=tablet8]">{seats}</testcase>'
              '<testcase classname="a.Tour5AppProofTest" name="proves"/>')
    write(known, "BenchRecetteTest#drive | only in seat mode | 2026-11-09 | test\n")
    code, text = judge(params, known)
    check(code == 0 and text.startswith("2 verts, 0 rouges, 0 en quarantaine, 2 sautés"), "a known skip covers its parameterised cases:\n" + text)
    write(known, "BenchRecetteTest#driveOther | only in seat mode | 2026-11-09 | test\n")
    code, text = judge(params, known)
    check(code == 1 and "rouge : BenchRecetteTest#drive[lang=fr device=phone] : saut hors" in text, "a known skip covers no other method:\n" + text)
    # A red self-test still shows the judge of the work: selftest.sh runs it before it stops on a red verdict.
    with open(os.path.join(HERE, "selftest.sh"), encoding="utf-8") as f:
        sh = f.read()
    check(0 < sh.find("verifie-chantier.py --resume") < sh.find('fail "tests du banc'), "selftest.sh runs the judge before it fails on red tests")

    # The judge: a guard listed « sauté » is not green; neither is one the summary does not name at all.
    root = judge_root(d, "chantier-skip")
    script = os.path.join(HERE, "verifie-chantier.py")
    resume = os.path.join(d, "chantier-skip-resume.txt")
    write(resume, "1 verts, 0 rouges, 0 en quarantaine, 1 sautés\nsauté : BenchFooTest#guardsIt : not while the bench runs seats\nvert : AppProofTest#provesIt\n")
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", resume], capture_output=True, text=True)
    check("B : garde BenchFooTest#guardsIt pas verte dans le dernier selftest" in r.stdout, "a skipped guard is not green:\n" + r.stdout)
    write(resume, "1 verts, 0 rouges, 0 en quarantaine, 0 sautés\nvert : AppProofTest#provesIt\n")
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", resume], capture_output=True, text=True)
    check("B : garde BenchFooTest#guardsIt pas verte dans le dernier selftest" in r.stdout, "a guard that did not run is not green:\n" + r.stdout)
    write(resume, "2 verts, 0 rouges, 0 en quarantaine, 0 sautés\nvert : AppProofTest#provesIt\nvert : BenchFooTest#guardsIt\n")
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git", "--resume", resume], capture_output=True, text=True)
    check("pas verte" not in r.stdout, "a guard named green is green:\n" + r.stdout)


PORTAL_GRADLE = """#!/usr/bin/env bash
# The portal's Gradle on the toy: writes the pass's fixture results where the portal asked for them.
set -euo pipefail
dir=""
for a in "$@"; do case "$a" in -Ptest.results.dir=*) dir="${a#-Ptest.results.dir=}" ;; esac; done
name="$(basename "$dir")"
echo "$name $*" >> "$PS_TOY_CALLS"
if [ -d "$PS_TOY_FIXTURES/$name" ]; then cp "$PS_TOY_FIXTURES/$name"/*.xml "$dir"/; fi
[ "${PS_TOY_GRADLE_FAILS:-}" != "$name" ]
"""


def git_toy(root, *args):
    r = subprocess.run(["git", "-C", root, "-c", "user.name=t", "-c", "user.email=t@example.org", "-c", "commit.gpgsign=false", *args],
                       capture_output=True, text=True)
    assert r.returncode == 0, r.stdout + r.stderr
    return r.stdout


def portal(d):
    """Review of lot 1 « Le juge » (finding « moyen » 1): selftest.sh's wiring, by its behaviour. The real portal runs
    on a toy repository (its own copies of the verdict, the list of expected classes, the stamp and the judge; a stand-in
    for Gradle that gives each pass its fixture JUnit results): the classes it expects are the toy's (read from the
    toy's sources, not a list written in the portal), a red verdict forbids the stamp, and a class of the list that
    did not run is red."""
    root = os.path.join(d, "portail")
    bench = os.path.join(root, "scripts", "bench")
    for name in ("selftest.sh", "tampon.sh", "selftest-attendus.py", "selftest-verdict.py", "verifie-chantier.py"):
        with open(os.path.join(HERE, name), encoding="utf-8") as f:
            write(os.path.join(bench, name), f.read())
    write(os.path.join(bench, "test_ultra_bench.py"), "import os, sys\nsys.exit(1 if os.environ.get('PS_TOY_SCRIPTS_FAIL') else 0)\n")
    write(os.path.join(bench, "gradle-file.sh"), PORTAL_GRADLE)
    write(os.path.join(bench, "selftest-quarantaine.txt"), "# none\n")
    write(os.path.join(bench, "selftest-sauts-connus.txt"), "BenchFooTest#drive | seat entry point | 2026-11-09 | test\n")
    for name in ("selftest.sh", "tampon.sh", "gradle-file.sh"):
        os.chmod(os.path.join(bench, name), 0o755)
    java = os.path.join(root, "app", "src", "test", "java", "com", "x", "app")
    write(os.path.join(java, "human", "HumanTester.kt"), "package com.x.app.human\nabstract class HumanTester(val name: String) {\n  fun drive() {}\n}\n")
    write(os.path.join(java, "human", "BenchFooTest.kt"), 'package com.x.app.human\nclass BenchFooTest : HumanTester("foo") {\n  fun guardsIt() {}\n}\n')
    write(os.path.join(java, "pads", "ChallengeTest.kt"), 'package com.x.app.pads\nclass ChallengeTest {\n'
          '    @KnownDefect("K1", "m")\n    @Test\n    fun provesIt() {}\n}\n')
    write(os.path.join(root, "app", "src", "main", "assets", "recette.json"), json.dumps({"id": "r", "milestone": "m", "items": []}))
    ultra = os.path.join(root, "docs", "audits", "ultra")
    write(os.path.join(ultra, "base.tsv"), "cle\tetat\tvu\tnote\tcorrige_au\n")
    write(os.path.join(ultra, "verdicts.tsv"), JUDGE_HEAD + "t\tA\tk1\tm\tappli\touvert\tChallengeTest#provesIt\t-\t-\t-\tn\tK1\n")
    git_toy(root, "init", "-q")
    git_toy(root, "add", "app", "docs", "scripts")
    git_toy(root, "commit", "-q", "-m", "toy")
    # The machine's checks answered by stand-ins (disk, Gradle daemons, orphan workers): the test is about the wiring.
    tools = os.path.join(d, "portail-outils")
    write(os.path.join(tools, "df"), "#!/usr/bin/env bash\necho Avail; echo 50G\n")
    write(os.path.join(tools, "pgrep"), "#!/usr/bin/env bash\necho 0\n")
    write(os.path.join(tools, "ps"), "#!/usr/bin/env bash\ntrue\n")
    for name in ("df", "pgrep", "ps"):
        os.chmod(os.path.join(tools, name), 0o755)
    calls = os.path.join(d, "portail-appels.txt")
    foo = '<testcase classname="com.x.app.human.BenchFooTest" name="guardsIt"/>'
    defect = '<testcase classname="com.x.app.pads.ChallengeTest" name="provesIt"/>'
    count = [0]

    def selftest(banc, defauts, **env):
        """(exit code, stderr + stdout, the summary, the lot) of the portal on a new lot with these results per pass."""
        count[0] += 1
        fixtures = os.path.join(d, f"portail-resultats-{count[0]}")
        for name, cases in (("banc", banc), ("defauts", defauts)):
            if cases is not None:
                write(os.path.join(fixtures, name, "TEST-x.xml"), "<testsuite>" + cases + "</testsuite>")
        os.makedirs(fixtures, exist_ok=True)
        lot = os.path.join(d, f"portail-lot-{count[0]}")
        os.makedirs(lot)
        write(os.path.join(lot, "selftest.ok"), "an older stamp\n")
        write(calls, "")
        e = dict(os.environ, PATH=tools + os.pathsep + os.environ["PATH"], PS_TOY_FIXTURES=fixtures, PS_TOY_CALLS=calls, **env)
        r = subprocess.run(["bash", os.path.join(bench, "selftest.sh"), lot], capture_output=True, text=True, env=e)
        summary = os.path.join(lot, "selftest-resume.txt")
        text = open(summary, encoding="utf-8").read() if os.path.isfile(summary) else ""
        return r.returncode, r.stderr + r.stdout, text, lot

    def stamped(lot):
        return os.path.isfile(os.path.join(lot, "selftest.ok"))

    # Everything green: stamped, and the stamp fits; Gradle was asked for the toy's own defect class.
    code, out, text, lot = selftest(foo, defect)
    check(code == 0 and text.startswith("2 verts, 0 rouges, 0 en quarantaine, 0 sautés") and "vert : ChallengeTest#provesIt" in text,
          "the toy's two classes green: the portal is green\n" + out + text)
    check(stamped(lot) and subprocess.run(["bash", os.path.join(bench, "tampon.sh"), "verifie", lot]).returncode == 0,
          "a green portal writes a stamp that fits its lot")
    with open(calls, encoding="utf-8") as f:
        asked = f.read()
    check("defauts " in asked and "--tests com.x.app.pads.ChallengeTest" in asked, "the « defauts » pass runs the classes read from the sources:\n" + asked)
    with open(os.path.join(lot, "selftest-attendus.txt"), encoding="utf-8") as f:
        check(f.read().split() == ["BenchFooTest", "ChallengeTest"], "the expected classes are the toy's, read from its sources")
    with open(os.path.join(lot, "selftest-chantier.txt"), encoding="utf-8") as f:
        check("résumé absent" not in f.read(), "the judge is given this run's summary")
    # A red test (a @KnownDefect made green: its rule fails it): no stamp, the older one gone, exit 1.
    fixed = '<testcase classname="com.x.app.pads.ChallengeTest" name="provesIt"><failure message="K1 no longer fails: fixed?"/></testcase>'
    code, out, text, lot = selftest(foo, fixed)
    check(code != 0 and not stamped(lot) and "SELFTEST ROUGE" in out and "rouge : ChallengeTest#provesIt : K1 no longer fails" in text,
          "a red verdict forbids the lot:\n" + out + text)
    # An expected class that did not run at all (its pass gave no result): red.
    code, out, text, lot = selftest(foo, None)
    check(code != 0 and not stamped(lot) and "rouge : ChallengeTest#* : aucun résultat" in text, "an expected class without a result is red:\n" + out + text)
    # A skip outside the list of known skips is red; a known one is listed and passes.
    skip = '<skipped message="only in seat mode"/>'
    code, out, text, lot = selftest(foo + f'<testcase classname="com.x.app.human.BenchFooTest" name="other">{skip}</testcase>', defect)
    check(code != 0 and not stamped(lot) and "rouge : BenchFooTest#other : saut hors de la liste" in text, "an unknown skip is red:\n" + out + text)
    code, out, text, lot = selftest(foo + f'<testcase classname="com.x.app.human.BenchFooTest" name="drive">{skip}</testcase>', defect)
    check(code == 0 and stamped(lot) and "sauté : BenchFooTest#drive : only in seat mode" in text, "a known skip passes, listed:\n" + out + text)
    # A Gradle run that failed with no red test to show for it, and red bench scripts: red.
    code, out, text, lot = selftest(foo, defect, PS_TOY_GRADLE_FAILS="banc")
    check(code != 0 and not stamped(lot), "a failed Gradle run with no red test is red:\n" + out + text)
    code, out, text, lot = selftest(foo, defect, PS_TOY_SCRIPTS_FAIL="1")
    check(code != 0 and not stamped(lot) and "scripts du banc" in out, "red bench scripts forbid the lot:\n" + out)
    # A list of known skips that holds anything but a seat's entry point: red (see known_skips_bounded).
    write(os.path.join(bench, "selftest-sauts-connus.txt"), "BenchFooTest#drive | seat | 2026-11-09 | test\nBenchFooTest#guardsIt | flaky | 2026-11-09 | test\n")
    git_toy(root, "commit", "-q", "-m", "a real test in the known skips", "scripts/bench/selftest-sauts-connus.txt")
    code, out, text, lot = selftest(foo, defect)
    check(code != 0 and not stamped(lot) and "sauts connus" in out and "BenchFooTest#guardsIt" in out, "a real test in the list of known skips forbids the lot:\n" + out)


FUSION_SELFTEST = """#!/usr/bin/env bash
# The fusion's self-test on the toy: says it ran (and on which lot), red on demand.
echo "selftest $1" >> "$PS_TOY_CALLS"
[ -z "${PS_TOY_SELFTEST_RED:-}" ] || exit 1
printf '1 verts, 0 rouges, 0 en quarantaine, 0 sautés\\nvert : BenchFooTest#guardsIt\\n' > "$1/selftest-resume.txt"
echo "a stamp" > "$1/selftest.ok"
echo "chantier : 0 écarts" > "$1/selftest-chantier.txt"
"""

FUSION_REJEUX = """#!/usr/bin/env bash
# The fusion's replays on the toy: says which folder it would play.
echo "rejeux ${PS_REJEUX_DIR:-docs/audits/ultra/rejeux} $(ls docs/audits/ultra/rejeux 2>/dev/null | tr '\\n' ' ')" >> "$PS_TOY_CALLS"
"""


def fusion_toy(d, name):
    """A toy integration workshop for fusion-equipes.sh: the real script, stand-ins for the self-test, the replays and
    Gradle (each one writes its call), one replay test in docs/audits/ultra/rejeux/."""
    root = os.path.join(d, name)
    bench = os.path.join(root, "scripts", "bench")
    with open(os.path.join(HERE, "fusion-equipes.sh"), encoding="utf-8") as f:
        write(os.path.join(bench, "fusion-equipes.sh"), f.read())
    for extra in ("relecture-verdict.py",):
        if os.path.isfile(os.path.join(HERE, extra)):
            with open(os.path.join(HERE, extra), encoding="utf-8") as f:
                write(os.path.join(bench, extra), f.read())
    write(os.path.join(bench, "selftest.sh"), FUSION_SELFTEST)
    write(os.path.join(bench, "rejeu-defauts.sh"), FUSION_REJEUX)
    write(os.path.join(bench, "gradle-file.sh"), '#!/usr/bin/env bash\necho "gradle $*" >> "$PS_TOY_CALLS"\n')
    write(os.path.join(bench, "tampon.sh"), "#!/usr/bin/env bash\ncase \"$1\" in verifie) exit 1 ;; esac\n")
    for n in ("fusion-equipes.sh", "selftest.sh", "rejeu-defauts.sh", "gradle-file.sh", "tampon.sh"):
        os.chmod(os.path.join(bench, n), 0o755)
    write(os.path.join(root, "docs", "audits", "ultra", "rejeux", "t1-clavier", "start.txt"), "lang=fr\n")
    write(os.path.join(root, "docs", "audits", "ultra", "rejeux", "t2-partie", "start.txt"), "lang=fr\n")
    git_toy(root, "init", "-q", "-b", "fx-int")
    git_toy(root, "add", "scripts", "docs")
    git_toy(root, "commit", "-q", "-m", "toy")
    return root


def fusion_forged(d):
    """Audit « banc crédible » 2, P1 (bloquant) and P4: fusion-equipes.sh reads the review's verdict on its LAST line
    only (« Verdict : 0 bloquant, 0 grave », nothing else on it), refuses a table row « bloquant »/« grave » that does
    not say « corrigé », reads the review in the branch only (a file left in the workshop is not a review), always runs
    the self-test after the merge (not only through the replays) and the replays of the repository's own rejeux/
    (PS_REJEUX_DIR ignored), and refuses an empty rejeux/ or a replay taken out without « rejeu retiré : <id> :
    <raison> » in the review. The forges of the audit (forge2, forge3) are refused; an honest branch is merged."""
    root = fusion_toy(d, "fusion-forge")
    script = os.path.join(root, "scripts", "bench", "fusion-equipes.sh")
    calls = os.path.join(d, "fusion-appels.txt")
    empty = os.path.join(d, "fusion-rejeux-vides")
    os.makedirs(empty)

    def branch(zone, change, review, untracked=False):
        """eq/tour-9-<zone> from fx-int: change(root) committed, then its review (naming that commit)."""
        git_toy(root, "checkout", "-q", "-b", f"eq/tour-9-{zone}", "fx-int")
        change(root)
        git_toy(root, "add", "-A")
        git_toy(root, "commit", "-q", "--allow-empty", "-m", f"team {zone}")
        sha = git_toy(root, "rev-parse", "HEAD").strip()
        path = os.path.join(root, "docs", "audits", "ultra", f"tour-9-{zone}-relecture.md")
        if not untracked:
            write(path, f"# Relecture\nRelu : {sha}\n\n" + review)
            git_toy(root, "add", path)
            git_toy(root, "commit", "-q", "-m", "review")
        git_toy(root, "checkout", "-q", "fx-int")
        if untracked:
            write(path, f"Relu : {sha}\n\n" + review)
        return path

    def merge(zone, **env):
        write(calls, "")
        before = git_toy(root, "rev-parse", "HEAD").strip()
        e = dict(os.environ, PS_TOY_CALLS=calls, **env)
        r = subprocess.run(["bash", script, "tour-9", zone], capture_output=True, text=True, env=e)
        with open(calls, encoding="utf-8") as f:
            asked = f.read()
        after = git_toy(root, "rev-parse", "HEAD").strip()
        return r.returncode, r.stdout + r.stderr, asked, before != after

    def touch(name):
        return lambda r: write(os.path.join(r, "scripts", "bench", name), "x\n")

    def move_replay(rid, to="rejeux-attente"):
        def go(r):
            git_toy(r, "mv", f"docs/audits/ultra/rejeux/{rid}", f"docs/audits/ultra/{to}")
        return go

    good = "| 1 | moyen | a thing | à reprendre |\n| 2 | grave | another | **corrigé** (abc1234) |\n\nVerdict : 0 bloquant, 0 grave — fusion possible\n"
    # forge2: a review that says « 1 bloquant » in its table and the verdict phrase anyway; the only replay moved out.
    def forge2(r):
        write(os.path.join(r, "scripts", "bench", "tampon.sh"), "#!/usr/bin/env bash\nexit 0\n")
        move_replay("t1-clavier")(r)
        move_replay("t2-partie")(r)
    branch("forge2", forge2, "| 1 | bloquant | le tampon ne vérifie plus rien |\n\nVerdict : 0 bloquant, 0 grave (avant correction du constat 1)\n")
    code, out, asked, moved = merge("forge2", PS_REJEUX_DIR=empty)
    check(code != 0 and not moved and "not merged" in out, "forge2 (a review with « 1 bloquant », no replay left) is refused:\n" + out)
    # A table row « grave » not said « corrigé », even under a clean last line.
    branch("grave", touch("a.txt"), "| 1 | grave | still open | à faire |\n| 2 | grave | non corrigé |\n\nVerdict : 0 bloquant, 0 grave\n")
    code, out, asked, moved = merge("grave")
    check(code != 0 and not moved and "grave" in out, "a review whose table holds an open grave is refused:\n" + out)
    # The verdict phrase somewhere, not on the last line.
    branch("milieu", touch("b.txt"), "Verdict : 0 bloquant, 0 grave\n\nAjout : un point ouvert.\n")
    code, out, asked, moved = merge("milieu")
    check(code != 0 and not moved, "a verdict that is not the review's last line is refused:\n" + out)
    # forge3 (P4): the review left in the workshop, never committed in the branch.
    left = branch("forge3", touch("c.txt"), "Verdict : 0 bloquant, 0 grave\n", untracked=True)
    code, out, asked, moved = merge("forge3")
    check(code != 0 and not moved and "no fresh review" in out, "a review that is not in the branch is refused (P4):\n" + out)
    os.remove(left)
    # A replay taken out of rejeux/ without a reason in the review.
    branch("retire", move_replay("t2-partie"), good)
    code, out, asked, moved = merge("retire")
    check(code != 0 and not moved and "t2-partie" in out, "a replay taken out without « rejeu retiré » is refused:\n" + out)
    # Every replay taken out, each with its reason: rejeux/ empty is still refused (a merge without any replay).
    branch("vide", forge2, good.replace("\nVerdict", "rejeu retiré : t1-clavier : non tranché\nrejeu retiré : t2-partie : non tranché\n\nVerdict"))
    code, out, asked, moved = merge("vide")
    check(code != 0 and not moved and "rejeu" in out, "an empty rejeux/ is refused:\n" + out)
    # A red self-test after the merge: undone.
    branch("rouge", touch("d.txt"), good)
    code, out, asked, moved = merge("rouge", PS_TOY_SELFTEST_RED="1")
    check(code != 0 and not moved and "selftest " in asked, "a red self-test after the merge undoes it:\n" + out + asked)
    # Honest: one replay taken out with its reason, an open « moyen » and a fixed grave: merged, the self-test run
    # and the repository's own replays played (PS_REJEUX_DIR ignored).
    branch("honnete", move_replay("t2-partie"), good.replace("\nVerdict", "rejeu retiré : t2-partie : non tranché, retour en attente\n\nVerdict"))
    code, out, asked, moved = merge("honnete", PS_REJEUX_DIR=empty)
    check(code == 0 and moved and "honnete: merged" in out, "an honest branch is merged:\n" + out)
    check("selftest " in asked and "rejeux docs/audits/ultra/rejeux t1-clavier" in asked, "the self-test and the repository's replays ran:\n" + asked)


def mark_fixed(d):
    """Review of lot 1 « Le juge » (finding « moyen » 2): marque-corrige.sh runs the judge WITH the summary it was
    given (4th argument or PS_SELFTEST_RESUME), on a toy repository; without one the judge says « résumé absent »."""
    root = judge_root(d, "marque")
    bench = os.path.join(root, "scripts", "bench")
    for name in ("marque-corrige.sh", "verifie-chantier.py"):
        with open(os.path.join(HERE, name), encoding="utf-8") as f:
            write(os.path.join(bench, name), f.read())
    git_toy(root, "init", "-q")
    git_toy(root, "add", "app", "docs", "scripts")
    git_toy(root, "commit", "-q", "-m", "toy")
    sha = git_toy(root, "rev-parse", "--short", "HEAD").strip()
    script = os.path.join(bench, "marque-corrige.sh")
    ultra = os.path.join(root, "docs", "audits", "ultra")
    env = {k: v for k, v in os.environ.items() if k != "PS_SELFTEST_RESUME"}

    def mark(*more, **extra):
        write(os.path.join(ultra, "verdicts.tsv"), JUDGE_GOOD)
        write(os.path.join(ultra, "base.tsv"), "cle\tetat\tvu\tnote\tcorrige_au\n")
        r = subprocess.run(["bash", script, "B", sha, "tour-9", *more], capture_output=True, text=True, env=dict(env, **extra))
        check(r.returncode == 0, "marque-corrige.sh ran:\n" + r.stdout + r.stderr)
        return r.stdout

    # The guard of line B is green only in a summary that names it: the judge's answer shows which summary it read.
    green = os.path.join(d, "marque-vert.txt")
    write(green, JUDGE_RESUME)
    other = os.path.join(d, "marque-autre.txt")
    write(other, "1 verts, 0 rouges, 0 en quarantaine, 0 sautés\nvert : AppProofTest#provesIt\n")
    out = mark(green)
    check(f"B: corrigé ({sha}, tour-9) ; clé k2" in out, "the line is marked:\n" + out)
    check("résumé absent" not in out and "pas verte" not in out, "the judge read the summary given as 4th argument:\n" + out)
    with open(os.path.join(ultra, "verdicts.tsv"), encoding="utf-8") as f:
        check(f"\tbanc\tcorrigé\tOther#elsewhere\tBenchFooTest#guardsIt\trecette tel-02\t{sha}\t" in f.read(), "verdicts.tsv: corrigé, with its commit")
    with open(os.path.join(ultra, "base.tsv"), encoding="utf-8") as f:
        check(f"k2\tcorrigé\tt\tB ({sha})\ttour-9\n" in f.read(), "base.tsv: the key, corrigé, with its round")
    out = mark(other)
    check("B : garde BenchFooTest#guardsIt pas verte dans le dernier selftest" in out and "résumé absent" not in out,
          "the judge read THAT summary (the guard is not green in it):\n" + out)
    out = mark(PS_SELFTEST_RESUME=other)
    check("B : garde BenchFooTest#guardsIt pas verte dans le dernier selftest" in out and "résumé absent" not in out,
          "the summary named by PS_SELFTEST_RESUME is read too:\n" + out)
    out = mark()
    check("résumé absent" in out, "without a summary the judge says so:\n" + out)


def known_skips_bounded(d):
    """Review of lot 1 « Le juge » (finding « moyen » 3): the list of known skips is bounded. Only « <Class>#drive » of a
    class that inherits HumanTester (a seat's entry point, skipped outside seat mode) is accepted: a real test added
    there would be skipped in silence. selftest-attendus.py --sauts-connus says which entries it refuses (exit 1), and
    selftest.sh is red on them."""
    script = os.path.join(HERE, "selftest-attendus.py")
    root = os.path.join(d, "sauts")
    java = os.path.join(root, "app", "src", "test", "java", "com", "x", "app")
    write(os.path.join(java, "human", "HumanTester.kt"), "package com.x.app.human\nabstract class HumanTester(val name: String) {\n  fun drive() {}\n}\n")
    write(os.path.join(java, "human", "BenchFooTest.kt"), 'package com.x.app.human\nclass BenchFooTest(private val a: String) : HumanTester("foo") {\n  fun guardsIt() {}\n}\n')
    write(os.path.join(java, "human", "BenchAloneTest.kt"), "package com.x.app.human\nclass BenchAloneTest {\n  fun drive() {}\n}\n")
    known = os.path.join(d, "sauts-connus.txt")

    def refused(text, tree=root):
        write(known, text)
        r = subprocess.run([sys.executable, "-I", script, "--root", tree, "--sauts-connus", known], capture_output=True, text=True)
        return r.returncode, r.stdout

    code, out = refused("# a comment\nBenchFooTest#drive | seat entry point | 2026-11-09 | test\n")
    check(code == 0 and out == "", f"a seat's entry point is accepted: {code}\n" + out)
    code, out = refused("BenchFooTest#drive | seat | 2026-11-09 | test\nBenchFooTest#guardsIt | flaky today | 2026-11-09 | test\n")
    check(code == 1 and out.splitlines() == ["saut connu refusé : BenchFooTest#guardsIt (seul « <Classe>#drive » d'une classe qui hérite de HumanTester est accepté)"],
          f"a real test is refused: {code}\n" + out)
    code, out = refused("BenchAloneTest#drive | not a seat | 2026-11-09 | test\n")
    check(code == 1 and "saut connu refusé : BenchAloneTest#drive" in out, f"« drive » of a class that is not a HumanTester is refused: {code}\n" + out)
    code, out = refused("BenchGoneTest#drive | no such class | 2026-11-09 | test\n")
    check(code == 1 and "saut connu refusé : BenchGoneTest#drive" in out, f"a class that does not exist is refused: {code}\n" + out)
    code, out = refused("BenchFooTest#drive[lang=fr] | one case | 2026-11-09 | test\n")
    check(code == 1 and "saut connu refusé : BenchFooTest#drive[lang=fr]" in out, f"only the bare name is accepted: {code}\n" + out)
    # The real list fits the real sources.
    repo = os.path.dirname(os.path.dirname(HERE))
    with open(os.path.join(HERE, "selftest-sauts-connus.txt"), encoding="utf-8") as f:
        real = f.read()
    code, out = refused(real, repo)
    check(code == 0 and "#drive |" in real, f"the real list holds seats' entry points only: {code}\n" + out)
    code, out = refused(real + "BenchSelfTest#reachTagTouchesAVisibleElementInPlace | skipped for now | 2026-11-09 | test\n", repo)
    check(code == 1 and "saut connu refusé : BenchSelfTest#reachTagTouchesAVisibleElementInPlace" in out, f"a real test added to the real list is refused: {code}\n" + out)


NO_NOTES = "retours-benjamin.md absent : les annotations d'essai de verdicts.tsv ne peuvent pas être vérifiées"
JUDGE_HEAD = "tour\titem\tcle\tgravite\tverdict\tetat\tpreuve_appli\tgarde_banc\ttelephone\tcommit\tnote\tgarde_id\n"
JUDGE_GOOD = (JUDGE_HEAD + "t\tA\tk1\tm\tappli\touvert\tAppProofTest#provesIt\t-\trecette tel-01\t-\tn\tK1\n"
              "t\tB\tk2\tm\tbanc\touvert\tOther#elsewhere\tBenchFooTest#guardsIt\trecette tel-02\t-\tn\t-\n")
JUDGE_RESUME = "3 verts, 0 rouges, 0 en quarantaine, 0 sautés\nvert : AppProofTest#provesIt\nvert : BenchFooTest#guardsIt\n"


def judge_root(d, name, verdicts=JUDGE_GOOD):
    """A tiny work for verifie-chantier.py: one app proof (a @KnownDefect), one bench guard, a test without the bench,
    a checklist of two items, an empty base and a summary of the self-test where both ran green."""
    root = os.path.join(d, name)
    tests = os.path.join(root, "app", "src", "test", "java", "x")
    write(os.path.join(tests, "BenchFooTest.kt"), "class BenchFooTest {\n  fun guardsIt() {}\n}\n")
    write(os.path.join(tests, "AppProofTest.kt"),
          'class AppProofTest {\n  @KnownDefect("K1", "m")\n  @Test\n  fun provesIt() {}\n}\nclass Other {\n  fun elsewhere() {}\n}\n')
    write(os.path.join(root, "app", "src", "main", "assets", "recette.json"),
          json.dumps({"id": "r", "milestone": "m", "items": [{"id": "tel-01", "title": "t"}, {"id": "tel-02", "title": "t"}]}))
    ultra = os.path.join(root, "docs", "audits", "ultra")
    write(os.path.join(ultra, "base.tsv"), "cle\tetat\tvu\tnote\tcorrige_au\n")
    write(os.path.join(ultra, "verdicts.tsv"), verdicts)
    write(os.path.join(root, "selftest-resume.txt"), JUDGE_RESUME)
    return root


def native_missing(d):
    """PLAN-RESTE 5.9: an UnsatisfiedLinkError or « moteur absent » in a seat's journal is counted."""
    seat = os.path.join(d, "native", "a")
    write(os.path.join(seat, "journal.txt"), JOURNAL_A.replace("    at com.pocketstudio.app.Foo.bar(Foo.kt:1)",
          "    java.lang.UnsatisfiedLinkError: no pocketstudio\n#5 SON (banc : moteur absent)"))
    run(os.path.join(HERE, "seat-metrics.py"), seat)
    with open(os.path.join(seat, "mesures.json"), encoding="utf-8") as f:
        m = json.load(f)["mesures"]
    check(m["moteur_absent"] == 2, f"two lines: {m['moteur_absent']}")


def seat_exit(d):
    """sortie-de-siege.py (PLAN-RESTE 4.2): a file written beside the seats during the lot is counted at the lot's
    level; a seat's own files and the white list (selftest.ok, *.tsv…) are not."""
    lot = os.path.join(d, "lotX")
    write(os.path.join(lot, "a", "start.txt"), "x")
    script = os.path.join(HERE, "sortie-de-siege.py")
    run(script, "avant", lot)
    write(os.path.join(lot, "a", "journal.txt"), "x")
    write(os.path.join(lot, "c-out.txt"), "x")
    write(os.path.join(lot, "selftest.ok"), "x")
    write(os.path.join(lot, "notes", "idee.txt"), "x")
    out = run(script, "apres", lot)
    check("2 fichier(s) hors des sièges" in out, out)
    with open(os.path.join(lot, "lot.json"), encoding="utf-8") as f:
        check(json.load(f)["fichiers_hors_lot"] == ["c-out.txt", "notes/idee.txt"], "the two foreign files")
    with open(os.path.join(lot, "incidents-lot.tsv"), encoding="utf-8") as f:
        text = f.read()
    check("lot\t-\t-\tfichier écrit hors des sièges : c-out.txt" in text, text)
    # Review of banc45: both reach the round's incidents-banc.tsv: the lot's foreign files, and a state a seat inherited
    # and the bench put back (« #0 BANC : … » in its journal); the journal's other « BANC » lines stay the journal's.
    write(os.path.join(lot, "a", "journal.txt"), "#0 BANC : état hérité remis (laissé par le test d'avant, HumanTesterB#drive, ou par "
          "un test sans la règle passé entre les deux) : I18n.lang = EN\n#0 SIEGE lang=fr\n#3 BANC repos non venu en 20 s (1 fois)\n")
    run(os.path.join(HERE, "fuse-reports.py"), lot, "--base", os.path.join(lot, "base.tsv"), "--verdicts", os.path.join(lot, "verdicts.tsv"))
    with open(os.path.join(lot, "incidents-banc.tsv"), encoding="utf-8") as f:
        incidents = f.read()
    check("a\t0\t-\tétat hérité remis (laissé par le test d'avant, HumanTesterB#drive" in incidents and "I18n.lang = EN" in incidents,
          "the inherited state put back, listed for its seat:\n" + incidents)
    check(". (lot)\t-\t-\tfichier écrit hors des sièges : c-out.txt" in incidents, "the lot's foreign file listed:\n" + incidents)
    check("repos non venu" not in incidents and incidents.count("\n") == 4, "nothing else:\n" + incidents)


def corrections_by_target(d):
    """applique-corrections.py (PLAN-RESTE 6.1): two cards on one screen; a citation with a « cible » resolves to its
    card, one without stays ambiguous."""
    rnd = os.path.join(d, "corr", "tour-9")
    make_round(rnd)
    two = REPORT_A.replace("  - ecran: 1 | cible: REC | geste: touche", "  - ecran: 4 | cible: REC | geste: touche")
    write(os.path.join(rnd, "a", "rapport-a.txt"), two)
    base = os.path.join(d, "corr", "base.tsv")
    run(os.path.join(HERE, "fuse-reports.py"), rnd, "--base", base, "--maj-base", "--tour", "tour-9")
    corr = os.path.join(d, "corr", "corrections.tsv")
    write(corr, "tour\titem\tsiege\tecran\tetat\tcommit\ttest\tnote\tcible\n"
          "tour-9\tX-1\ta\t4\tcorrigé\tabc1234\tT\tn\trec\n"
          "tour-9\tX-2\ta\t4\tbanc\tabc1234\tT\tn\t\n")
    out = run(os.path.join(HERE, "applique-corrections.py"), "--corrections", corr, "--base", base, "--sieges", os.path.join(d, "corr"))
    check("1 ambiguës" in out and "X-2" in out, "the citation without a target is the only ambiguous one:\n" + out)
    with open(base, encoding="utf-8") as f:
        text = f.read()
    check("padlive|rec|incompris\tcorrigé\ttour-9\t" in text.replace("\n", "\n") or "|rec|incompris\tcorrigé" in text,
          "the REC card is the one marked corrected:\n" + text)


def missions_of_round_1(d):
    """Round 1 (lot banc): the oracles of tuto-facile, tuto-pro, synthe, libre and a challenge opened without stars."""
    c = os.path.join(d, "c")
    write(os.path.join(c, "start.txt"), "mission=tuto-pro\n")
    write(os.path.join(c, "journal.txt"), JOURNAL_B)
    # jump_up's first three chapters done in Pro (bases, kick, snare), the store's JSON escaped as Android writes it.
    done = ["ref", "tempo", "key", "ready", "kick_hear", "kick_do", "kick_sound", "snare_do", "clap_do"]
    prefs = json.dumps({"style": "jump_up", "level": "Pro", "done": done, "current": "hat_do"}).replace('"', "&quot;")
    write(os.path.join(c, "etat", "prefs", "tuto.xml"), f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    <string name=\"p-3\">{prefs}</string>\n</map>\n")
    write(os.path.join(c, "etat", "resume.json"), json.dumps({"projects": [], "shared": [], "prefs": {"simpleMode": False}}))
    out = run(os.path.join(HERE, "seat-metrics.py"), c)
    check("mission=reussie" in out and "3 chapitre(s) entier(s)" in out, "three whole chapters in Pro:\n" + out)
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "tuto-facile")
    check("mission=partielle" in out and "niveau demandé : Facile" in out, "the course done in Pro is not the Facile one:\n" + out)
    # Two steps only, Facile (the store leaves the default level out): partial.
    prefs = json.dumps({"style": "jump_up", "done": ["ref", "tempo"], "current": "key"}).replace('"', "&quot;")
    write(os.path.join(c, "etat", "prefs", "tuto.xml"), f"<map><string name=\"p-3\">{prefs}</string></map>")
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "tuto-facile")
    check("mission=partielle" in out and "en Facile : 2 étape(s), 0 chapitre(s)" in out, "two steps of the first chapter:\n" + out)

    # synthe, from the projects' files (round 1's states): the same sound on the Synth and on « Basse », both with notes.
    sound = {"values": [0.5, 0.1, 0.3]}
    project = {"name": "Mon projet 1", "synthEngine": 0, "synth": sound, "synthNotes": {"notes": [{"start": 0}]},
               "tracks": [{"name": "Basse", "engine": 0, "synth": sound, "notes": {"notes": [{"start": 0}, {"start": 8}]}}]}
    write(os.path.join(c, "etat", "projects", "p-3", "project.json"), json.dumps(project))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "synthe")
    check("mission=reussie" in out and "Synthé, Basse" in out, "one sound on two tracks:\n" + out)
    project["tracks"][0]["synth"] = {"values": [0.0, 0.0, 0.3]}
    write(os.path.join(c, "etat", "projects", "p-3", "project.json"), json.dumps(project))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "synthe")
    check("mission=partielle" in out, "two different sounds: partial:\n" + out)
    # SeatState's facts (round 2 on): an untouched sound does not count.
    write(os.path.join(c, "etat", "resume.json"), json.dumps({"projects": [{"name": "P", "synth_tracks": [
        {"name": "Synthé", "sound": "x", "notes": 3, "edited": False}, {"name": "Basse", "sound": "x", "notes": 2, "edited": False}]}], "shared": []}))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "synthe")
    check("mission=non" in out, "the default sound on two tracks is not a sound made:\n" + out)

    # libre (PLAN-RESTE 6.4, hardened): ended by the tester on 3 screens or more is not enough; « réussie » needs a
    # sound heard AND a trace on the phone (a saved change or a shared file). Round 5: A c « réussie » without a sound.
    free = JOURNAL_B.replace("#2 FIN : fin", "#2 > touche 2 2\n#3 ROUTE mixer/tab_Mix | MODE pro | EMPREINTE 3333 | project=p-2 no tuto\n#3 SON Son : rien ne joue.\n#3 > touche 3 3\n"
                             "#4 ROUTE padlive | MODE pro | EMPREINTE 4444 | project=p-2 no tuto\n#4 SON Son : rien ne joue.\n#4 FIN : fin")
    write(os.path.join(c, "journal.txt"), free)
    untouched = json.dumps({"projects": [{"name": "P", "parts": 0, "tracks": 0, "audio_clips": 0, "stars": [], "synth_tracks": [
        {"name": "Synthé", "sound": "x", "notes": 0, "edited": False}]}], "stem_sets": 0, "shared": []})
    write(os.path.join(c, "etat", "resume.json"), untouched)
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=non" in out and "aucun son entendu" in out and "aucun changement gardé ni fichier partagé" in out,
          "3 screens, no sound, nothing left on the phone: not a success:\n" + out)
    write(os.path.join(c, "sound-3.txt"), "Son : la musique du projet joue en boucle.")
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=partielle" in out and "aucun changement gardé ni fichier partagé" in out and "aucun son" not in out,
          "a sound heard but nothing left on the phone: partial:\n" + out)
    os.remove(os.path.join(c, "etat", "resume.json"))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=partielle" in out and "état du siège absent" in out, "no state saved: a change cannot be proven, said so:\n" + out)
    write(os.path.join(c, "etat", "resume.json"), untouched.replace('"audio_clips": 0', '"audio_clips": 1'))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=reussie" in out and "3 écrans différents" in out and "premier son en 2 geste(s)" in out and "1 prise(s)" in out,
          "a sound heard and a take kept: free play succeeded:\n" + out)
    write(os.path.join(c, "etat", "resume.json"), untouched.replace('"shared": []', '"shared": ["perf.m4a"]'))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=reussie" in out and "partagé : perf.m4a" in out, "a sound heard and a file shared: succeeded too:\n" + out)
    # Review of fusion64: a piano-roll composer writes notes on the main Synthé (not in « tracks », its sound untouched):
    # a trace; so are Live pads recorded.
    write(os.path.join(c, "etat", "resume.json"), untouched.replace('"notes": 0, "edited": false', '"notes": 12, "edited": false'))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=reussie" in out and "12 note(s) au synthé" in out, "notes written on the main Synthé: a change kept:\n" + out)
    write(os.path.join(c, "etat", "resume.json"), untouched.replace('"synth_tracks":', '"live_pads": 2, "synth_tracks":'))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=reussie" in out and "2 pad(s) Live enregistré(s)" in out, "Live pads recorded: a change kept:\n" + out)
    os.remove(os.path.join(c, "sound-3.txt"))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=partielle" in out and "aucun son entendu" in out, "a file shared but no sound heard: partial:\n" + out)
    write(os.path.join(c, "journal.txt"), free.replace("#4 FIN : fin", "#4 CRASH java.lang.IllegalStateException: x\n#4 FIN : plantage"))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=non" in out, "a crash fails it:\n" + out)
    write(os.path.join(c, "journal.txt"), free)

    # defi: a challenge opened, no star kept: partial (lot F seat e), never « aucun défi joué ».
    write(os.path.join(c, "journal.txt"), JOURNAL_B.replace("#1 > touche 1 1", "#1 > touche 1 1\n#1 DOIGT padlive_challenge_go « Jouer le défi »"))
    write(os.path.join(c, "etat", "resume.json"), json.dumps({"projects": [{"name": "Live", "stars": []}], "shared": []}))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "defi")
    check("mission=partielle" in out and "défi ouvert" in out, "a challenge opened:\n" + out)


if __name__ == "__main__":
    main()
