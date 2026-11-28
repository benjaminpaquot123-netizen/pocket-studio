#!/usr/bin/env python3
"""Banc fiable 2.3: the seats of a round, from <round>/sieges-proposes.tsv, written as start.txt and v2 briefs.
    fabrique-sieges.py <round folder> [--tour tour-N]
sieges-proposes.tsv: lot, siege, lang, device, font, mode, options (« a=b;c=d » or -), mission, gestes, profil, promesse,
niveau (lot « niveaux », PLAN-NIVEAUX §3: 0 curieux … 4 pro, or X for a seat of no level; a seat without one is refused,
but a « libre » seat, which counts for no level and may not claim one).
The promise field lists catalogue ids and free promises: « P093 ; P094 ; libre: Mes exports liste le .zip » (5 at
most). An id is taken from docs/audits/ultra/promesses-catalogue.tsv (its text, as the screen writes it); a free one
gets the id « tN-<lot><seat>-k ». The same id twice in one seat is refused. Writes <round>/lot<X>/<s>/start.txt,
<round>/briefs/<X><s>.txt and <round>/sieges.tsv (with the resolved ids). Standard library only (round 5's fabrique.py,
kept in the scratchpad until now, made general)."""
import json
import os
import re
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
CATALOGUE = os.path.join(HERE, "..", "..", "docs", "audits", "ultra", "promesses-catalogue.tsv")
GOALS = {
    "premier-morceau": "son (un son joue), partie (une 2e partie existe), partage (un fichier est partagé)",
    "voix": "prise (une prise de voix existe), ecoute (elle s'entend dans le morceau), garde (elle est encore là après Retour)",
    # PLAN-RESTE 6.4: what seat-metrics.py's « libre » oracle reads (a sound heard, a change kept or a file shared).
    "libre": "son (un son a joué), trace (un changement gardé sur le téléphone : partie, piste, prise, étoile, son retouché, remix) ou partage (un fichier partagé)",
    "padlive": "son (un pad sonne au 1er toucher), stop (tout s'arrête quand tu le demandes), prise (une performance est enregistrée)",
    "tuto-facile": "chap1 (1er chapitre fini), chap3 (3e chapitre fini), bravo (« Bravo » du tuto)",
    "tuto-pro": "chap1 (1er chapitre fini), chap3 (3e chapitre fini), bravo (« Bravo » du tuto)",
    "simple": "son (un son joue), partie (une 2e partie), jamais-pro (jamais passé en Pro)",
    "defi": "ouvert (un défi est ouvert), tente (une tentative faite), compris (tu sais quel pad taper et quand)",
    "remix-voix": "separe (la chanson est séparée), decoupe (la voix est découpée), pose (un bout sur un pad ou une piste)",
    "exports": "spotify (un fichier « streaming »), club (un fichier « club »), retrouve (retrouvés dans Mes exports)",
    "synthe": "son (un son de synthé choisi), regle (un réglage changé et entendu), garde (le son gardé sous un nom)",
    # Lot « niveaux » (PLAN-NIVEAUX §2): what seat-metrics.py's level oracles read in the saved project.
    "mon-morceau": "son (un son du morceau de l'assistant changé), melodie (ses notes changées), structure (ses parties changées)",
    "boucle-imposee": "rythme (la batterie de la fiche), basse (la ligne de basse de la fiche), accords (les accords de la fiche)",
    "projet-vide": "pistes (6 pistes qui jouent), parties (8 parties remplies), export (un fichier exporté)",
    "pistes-separees": "fichiers (un fichier audio par piste qui joue), format (.wav ou .flac), duree (tous de la même durée)",
}
LEVELS = ("0", "1", "2", "3", "4", "X")
# The fixture folder of the level oracles: the loop of « boucle-imposee » (its brief says it in words, the seat gets its sheet).
LOOP = os.path.join(HERE, "oracles-niveaux", "boucle-imposee", "fiche.json")

# Round 6, lot banc7 (point 6): the gestures a mission needs at least. A whole tutorial (15 chapters, « Bravo ») took the
# seats of round 6 about 16 gestures a chapter: 120 ran out at chapter 7/15 (D a, H a). Refused below, never cut in silence.
MIN_GESTES = {"tuto-facile": 250, "tuto-pro": 250}

def catalogue(path=CATALOGUE):
    out = {}
    for line in open(path, encoding="utf-8"):
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 4 and re.fullmatch(r"P\d{3}", parts[0]):
            out[parts[0]] = parts[3]
    return out


def promises(field, cat, tour, lot, seat):
    """« P093 ; libre: … » -> [(id, text)], refusing an unknown or repeated id."""
    out, free = [], 0
    for p in [p.strip() for p in field.split(";") if p.strip()]:
        m = re.fullmatch(r"P0*(\d{1,3})", p)
        if m:
            pid = f"P{int(m.group(1)):03d}"
            if pid not in cat:
                raise SystemExit(f"{lot}{seat}: promise {pid} is not in the catalogue")
            text = cat[pid]
        else:
            free += 1
            pid, text = f"t{tour}-{lot}{seat}-{free}", re.sub(r"^libre\s*:\s*", "", p)
        if any(pid == q for q, _ in out):
            raise SystemExit(f"{lot}{seat}: promise {pid} twice")
        out.append((pid, text))
    # PLAN-RESTE 6.4: a brief holds 5 promises at most (the report template's limit); refused, never cut in silence.
    if len(out) > 5:
        raise SystemExit(f"{lot}{seat}: {len(out)} promises, 5 at most (cut the seat's promise field in sieges-proposes.tsv)")
    return out


def main(argv):
    if not argv:
        print(__doc__)
        return 2
    t = argv[0]
    tour = re.sub(r"\D", "", argv[argv.index("--tour") + 1] if "--tour" in argv else os.path.basename(os.path.normpath(t))) or "x"
    cat = catalogue()
    rows = [l.rstrip("\n").split("\t") for l in open(os.path.join(t, "sieges-proposes.tsv"), encoding="utf-8")
            if l.strip() and not l.startswith(("#", "lot\t"))]
    os.makedirs(os.path.join(t, "briefs"), exist_ok=True)
    out = []
    for r in rows:
        lot, seat, lang, device, font, mode, options, mission, gestes, profil, promesse, niveau = [x.strip() for x in (r + [""] * 12)[:12]]
        # Lot « niveaux »: « reprendre » has no oracle yet (the bench cannot move the clock 3 days): refused, never a « non ».
        if mission == "reprendre":
            raise SystemExit(f"{lot}{seat}: mission reprendre: oracle à écrire (horloge), hors du tour 8")
        if mission == "libre" and niveau not in ("", "X"):
            raise SystemExit(f"{lot}{seat}: mission libre with niveau {niveau}: libre counts for no level (X or empty)")
        if mission != "libre" and niveau not in LEVELS:
            raise SystemExit(f"{lot}{seat}: niveau « {niveau} »: 0 to 4 or X (12th column of sieges-proposes.tsv, PLAN-NIVEAUX §3)")
        d = os.path.join(t, f"lot{lot}", seat)
        os.makedirs(d, exist_ok=True)
        start = [f"lang={lang}", f"device={device}", f"font={font}"]
        if mode and mode != "-":
            start.append(f"mode={mode}")
        if options and options != "-":
            start += [o for o in options.split(";") if o]
        need = MIN_GESTES.get(mission, 0)
        if not gestes.isdigit() or int(gestes) < need:
            raise SystemExit(f"{lot}{seat}: mission {mission} with {gestes} gestures, {need} at least (a whole tutorial: sieges-proposes.tsv)")
        start += [f"gestes.max={gestes}", f"mission={mission}", "timeout.minutes=30"] + ([f"niveau={niveau}"] if niveau else [])
        loop = ""
        if mission == "boucle-imposee":
            shutil.copyfile(LOOP, os.path.join(d, "boucle.json"))
            with open(LOOP, encoding="utf-8") as f:
                loop = json.load(f).get("dit", "")
        with open(os.path.join(d, "start.txt"), "w", encoding="utf-8") as f:
            f.write("\n".join(start) + "\n")
        proms = promises(promesse, cat, tour, lot, seat)
        brief = [
            f"First read and follow {t}/consignes-testeur.md (method v2) and write your report with EXACTLY its template.",
            f"SEAT={d} (seat {seat}, lot {lot}; lang {lang}, device {device}, font {font}, mode {mode}, options {options}, mission {mission}, budget {gestes} gestures).",
            f"Profile (id t{tour}-{lot}{seat}): {profil}. Act as this person (habits, patience, vocabulary).",
            f"Mission {mission}; its 3 goals (ids, the bench checks them): {GOALS.get(mission, 'but1, but2, but3')}.",
        ] + ([f"The loop to rebuild (say it to yourself as a musician would read it): {loop}"] if loop else []) + [
            "Promises to check, one « promesse: » line each in your report, with its id (one line per id, never twice):",
        ] + [f"  - id={i} | {p}" for i, p in proms] + [
            f"Your report {d}/rapport.md already starts with two lines the bench wrote when it started (« code testé : … », « joué par : … »): keep them as its first two lines, write your block under them.",
            "Report in French (fields may quote the screen in its language). Never a defect card because the bench gave no screen back: an « incidents-banc: » line.",
        ]
        with open(os.path.join(t, "briefs", f"{lot}{seat}.txt"), "w", encoding="utf-8") as f:
            f.write("\n".join(brief) + "\n")
        out.append("\t".join([lot, seat, lang, device, font, mode, options, mission, gestes, profil, " ; ".join(i for i, _ in proms), niveau]))
    with open(os.path.join(t, "sieges.tsv"), "w", encoding="utf-8") as f:
        f.write("lot\tsiege\tlang\tdevice\tfont\tmode\toptions\tmission\tgestes\tprofil\tpromesses\tniveau\n" + "\n".join(out) + "\n")
    print(len(out), "sièges")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
