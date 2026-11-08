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
        check("3 siège(s), 3 grappe(s), 2 nouvelle(s)" in text, "the summary:\n" + text)
        with open(base, encoding="utf-8") as f:
            added = f.read()
        check("tab_Sons|klaxon|rien\tnouveau" in added and added.count("\n") == 4, "the new keys added to the base:\n" + added)
        # A second run: nothing new any more, and the two seats' same key (when on the same route) fuses.
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-2")
        check("0 nouvelle(s)" in text, "the keys are known now, at the next round:\n" + text)
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base, "--tour", "tour-1")
        check("2 nouvelle(s)" in text, "fused again, the same round still calls them new:\n" + text)
        with open(os.path.join(d, "b", "journal.txt"), "w", encoding="utf-8") as f:
            f.write(JOURNAL_B.replace("#1 ROUTE tab_Sons", "#1 ROUTE padlive"))
        text = run(os.path.join(HERE, "fuse-reports.py"), d, "--base", base)
        check("×2 (a#4, b#2) padlive · « klaxon » · rien" in text and "1 vue(s) par ≥ 2 sièges" in text, "the same key fused:\n" + text)
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
        seats_from_ids(d)
    with tempfile.TemporaryDirectory() as d:
        comebacks(d)
    print("ultra bench scripts: OK")


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
    check("[revient] grave ×1 (A/a#4)" in text and "1 qui revien(nen)t" in text, "confirmed by a reader:\n" + text)
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
    subprocess.run([tampon, "ecris", d], check=True)
    check(subprocess.run([tampon, "verifie", d]).returncode == 0, "a fresh stamp fits this code:\n" + open(os.path.join(d, "selftest.ok")).read())
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
    check("2 siège(s), 2 grappe(s)" in text, "the hesitation saying « cible » is no defect:\n" + text)
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

    # libre: ended by the tester on 3 screens or more; a crash fails it.
    free = JOURNAL_B.replace("#2 FIN : fin", "#2 > touche 2 2\n#3 ROUTE mixer/tab_Mix | MODE pro | EMPREINTE 3333 | project=p-2 no tuto\n#3 SON Son : rien ne joue.\n#3 > touche 3 3\n"
                             "#4 ROUTE padlive | MODE pro | EMPREINTE 4444 | project=p-2 no tuto\n#4 SON Son : rien ne joue.\n#4 FIN : fin")
    write(os.path.join(c, "journal.txt"), free)
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=reussie" in out and "3 écrans différents" in out, "free play ended by the tester:\n" + out)
    write(os.path.join(c, "journal.txt"), free.replace("#4 FIN : fin", "#4 CRASH java.lang.IllegalStateException: x\n#4 FIN : plantage"))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "libre")
    check("mission=non" in out, "a crash fails it:\n" + out)

    # defi: a challenge opened, no star kept: partial (lot F seat e), never « aucun défi joué ».
    write(os.path.join(c, "journal.txt"), JOURNAL_B.replace("#1 > touche 1 1", "#1 > touche 1 1\n#1 DOIGT padlive_challenge_go « Jouer le défi »"))
    write(os.path.join(c, "etat", "resume.json"), json.dumps({"projects": [{"name": "Live", "stars": []}], "shared": []}))
    out = run(os.path.join(HERE, "seat-metrics.py"), c, "--mission", "defi")
    check("mission=partielle" in out and "défi ouvert" in out, "a challenge opened:\n" + out)


if __name__ == "__main__":
    main()
