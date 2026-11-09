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
        chantier(d)
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
    check(code == 0 and text.startswith("1 verts, 0 rouges, 0 en quarantaine, 1 sautés"), "green with a skip:\n" + text)
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
    root = os.path.join(d, "chantier")
    tests = os.path.join(root, "app", "src", "test", "java", "x")
    write(os.path.join(tests, "BenchFooTest.kt"), "class BenchFooTest {\n  fun guardsIt() {}\n}\n")
    write(os.path.join(tests, "AppProofTest.kt"),
          'class AppProofTest {\n  @KnownDefect("K1", "m")\n  @Test\n  fun provesIt() {}\n}\nclass Other {\n  fun elsewhere() {}\n}\n')
    write(os.path.join(root, "app", "src", "main", "assets", "recette.json"),
          json.dumps({"id": "r", "milestone": "m", "items": [{"id": "tel-01", "title": "t"}, {"id": "tel-02", "title": "t"}]}))
    ultra = os.path.join(root, "docs", "audits", "ultra")
    write(os.path.join(ultra, "base.tsv"), "cle\tetat\tvu\tnote\tcorrige_au\n")
    head = "tour\titem\tcle\tgravite\tverdict\tetat\tpreuve_appli\tgarde_banc\ttelephone\tcommit\tnote\n"
    good = (head + "t\tA\tk1\tm\tappli\touvert\tAppProofTest#provesIt\t-\trecette tel-01\t-\tn\n"
            "t\tB\tk2\tm\tbanc\touvert\t-\tBenchFooTest#guardsIt\trecette tel-02\t-\tn\n")
    write(os.path.join(ultra, "verdicts.tsv"), good)
    script = os.path.join(HERE, "verifie-chantier.py")
    out = run(script, "--root", root, "--sans-git")
    check("chantier : 0 écarts" in out, "a clean work: 0 gap\n" + out)
    bad = (head + "t\tA\tk1\tm\tappli\touvert\tBenchFooTest#guardsIt\t-\trecette tel-01\t-\tn\n"
           "t\tB\tk2\tm\tbanc\touvert\t-\t-\t-\t-\tn\n"
           "t\tC\tk3\tm\tappli\touvert\tOther#provesIt\t-\t-\t-\tn\n")
    write(os.path.join(ultra, "verdicts.tsv"), bad)
    r = subprocess.run([sys.executable, "-I", script, "--root", root, "--sans-git"], capture_output=True, text=True)
    out = r.stdout
    check(r.returncode == 1, "gaps fail the run")
    check("A : preuve d'appli BenchFooTest#guardsIt est un test du banc" in out, out)
    check("B : verdict « banc » sans garde Bench*" in out, out)
    check("C : preuve d'appli Other#provesIt introuvable" in out, "a method of another class of the same file is not this class's\n" + out)
    check("@KnownDefect K1 (AppProofTest#provesIt) sans ligne" in out, out)
    check("tel-02 de recette.json cité par aucune ligne" in out, out)


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
