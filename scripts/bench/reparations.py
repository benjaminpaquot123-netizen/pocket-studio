#!/usr/bin/env python3
"""The register of the repairs, docs/audits/ultra/corrections.tsv (docs/audits/ultra/PLAN-REPARATIONS-CONSTATEES.md):
a repair is « done » only when a tester saw it.

One line per tester's citation of a defect (round, item, seat « X/s », screen), its state, and since round 6:
  phrase_acceptation  what the tester must see after the repair, with its conditions, written by the sorter (rule 1);
  siege_origine       the seat that saw the defect (« tour-6/lotB/a »), whose start.txt gives the conditions of the
                      proof (language, device, font, mode, options; rule 2);
  constat             for « constaté »: the control seat's folder and the screen of its « corrigé » line (« …#21 »).
The lines of rounds 1 to 5 keep their text (9 cells, states « corrigé », « banc », « voulu »): a missing cell is empty.

The four states of a repair (rule 5), and who gives each:
  pas pris   the sorter                  the defect is in no lot
  tenté      the team                    a commit, without the proof in the conditions of origin
  rejoué     the portal                  the proof of rule 2 held on the merged head (scripts/bench/fusion-equipes.sh)
  constaté   the chief, from a seat      the « corrigé » line of a control seat, with its screen number
Only « constaté » is said « corrigé » to Benjamin.

  reparations.py --constate <tour-N> <item> <control seat folder> [--registre <corrections.tsv>] [--racine <repo>]

The chief's command: reads the section « defauts-avant: » of <folder>/rapport.md (one line per defect aimed at:
« - <id> | corrigé | ecran: <n> | … », and the forms of round 6, « - A7 | corrigé (écran 21 : …) », « - T4 | corrigé
(écran 98) : … »); the defect's line must say « corrigé » and name its screen. Then every line of that round and item in
« tenté » or « rejoué » becomes « constaté », its « constat » the folder (from the repository's root) and the screen.
Refused (exit 1, nothing written): « autre », « encore là », « pas atteint », no screen, no line, a defect « pas pris ».
Standard library only.
"""
import os
import re
import sys
import unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
REGISTER = "docs/audits/ultra/corrections.tsv"
COLUMNS = ["tour", "item", "siege", "ecran", "etat", "commit", "test", "note", "cible",
           "phrase_acceptation", "siege_origine", "constat"]
STATES = ("pas pris", "tenté", "rejoué", "constaté")
GIVEN_BY = {"pas pris": "le trieur", "tenté": "l'équipe", "rejoué": "le portail",
            "constaté": "le chef, d'après un siège de contrôle"}
# Rounds 1 to 5: « corrigé » (a lot's own word, before the rule), « banc », « voulu ».
OLD_STATES = ("corrigé", "banc", "voulu")
OLD_REPAIR = ("corrigé",)



def plain(text):
    """Lower case, no accents, single spaces: how two writings of one word are compared."""
    t = unicodedata.normalize("NFKD", text or "")
    t = "".join(c for c in t if not unicodedata.combining(c))
    return re.sub(r"\s+", " ", t.replace(" ", " ")).strip().lower()


def state(text):
    """The state as written, read as one of the known ones when it is one (any case, accents or not, spaces)."""
    for known in STATES + OLD_STATES:
        if plain(text) == plain(known):
            return known
    return unicodedata.normalize("NFC", re.sub(r"\s+", " ", (text or "").replace(" ", " "))).strip()


def round_no(tour):
    m = re.fullmatch(r"tour-(\d+)", (tour or "").strip())
    return int(m.group(1)) if m else -1


def load(text):
    """(header, [rows as {column: cell}]); « # » lines are comments; a short line's missing cells are empty."""
    header, rows = None, []
    for line in (text or "").split("\n"):
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.rstrip("\r").split("\t")
        if header is None:
            header = cells
            continue
        rows.append(dict(zip(header, cells + [""] * (len(header) - len(cells)))))
    return header or list(COLUMNS), rows


def read(path):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return None


AVANT_HEAD = re.compile(r"^\s*d[ée]fauts[- ]avant\s*:", re.I)
SECTION = re.compile(r"^[A-Za-z][\w.\-?]*\s*:")
AVANT_LINE = re.compile(r"^\s*[-*]\s*`?([^\s|:`]+)`?\s*[|:]\s*(corrig[ée]e?|autre|encore\s+l[àa]|pas\s+atteint)(?![\w])(.*)$", re.I)
SCREEN = re.compile(r"\b[ée]crans?\s*:?\s*(\d+)", re.I)


def before_defects(text):
    """{id: (answer, screen or None, the rest of the line)} of a seat report's section « defauts-avant: »."""
    out, inside = {}, False
    for raw in (text or "").splitlines():
        if AVANT_HEAD.match(raw):
            inside = True
            continue
        if inside and SECTION.match(raw):
            inside = False
        if not inside:
            continue
        m = AVANT_LINE.match(raw)
        if m:
            word = plain(m.group(2))
            answer = ("corrigé" if word.startswith("corrig") else "autre" if word == "autre"
                      else "encore là" if word.startswith("encore") else "pas atteint")
            screen = SCREEN.search(m.group(3))
            out.setdefault(m.group(1), (answer, screen.group(1) if screen else None, m.group(3).strip(" |:")))
    return out


def rewrite(path, changes):
    """Rewrites the lines of the register whose index (0 = the file's first line) is in changes {index: {column: value}};
    every other line is kept byte for byte."""
    with open(path, encoding="utf-8", newline="") as f:
        lines = f.read().split("\n")
    header = None
    for i, line in enumerate(lines):
        if not line.strip() or line.startswith("#"):
            continue
        if header is None:
            header = line.rstrip("\r").split("\t")
            continue
        if i in changes:
            cells = line.rstrip("\r").split("\t")
            cells += [""] * (len(header) - len(cells))
            for k, v in changes[i].items():
                cells[header.index(k)] = v
            lines[i] = "\t".join(cells) + ("\r" if line.endswith("\r") else "")
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write("\n".join(lines))


def indexed_rows(text):
    """[(index of the line in the file, row)]."""
    header, out = None, []
    for i, line in enumerate((text or "").split("\n")):
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.rstrip("\r").split("\t")
        if header is None:
            header = cells
            continue
        out.append((i, dict(zip(header, cells + [""] * (len(header) - len(cells))))))
    return header or [], out


def constate(tour, item, seat, register, root):
    """(exit code, lines to print)."""
    text = read(register)
    if text is None:
        return 2, [f"no register {register}"]
    header, rows = indexed_rows(text)
    missing = [c for c in ("etat", "constat") if c not in header]
    if missing:
        return 2, [f"{register}: no column {', '.join(missing)} (header: {' '.join(header)})"]
    # (The register first: a defect no lot took is never constaté, whatever a seat says of it.)
    mine = [(i, r) for i, r in rows if r.get("tour") == tour and r.get("item") == item]
    if not mine:
        return 1, [f"{item}: no line of {tour} for this defect in {register}"]
    takeable = [(i, r) for i, r in mine if state(r.get("etat")) in ("tenté", "rejoué")]
    if not takeable:
        states = ", ".join(sorted({state(r.get("etat")) for _, r in mine}))
        return 1, [f"{item}: its line(s) say « {states} »: only a repair « tenté » or « rejoué » becomes « constaté » "
                   "(a defect « pas pris » was in no lot)"]
    report = os.path.join(seat, "rapport.md")
    said = read(report)
    if said is None:
        return 1, [f"{item}: no report {report} (the control seat's rapport.md, with its section « defauts-avant: »)"]
    answers = before_defects(said)
    if item not in answers:
        return 1, [f"{item}: no line for this defect in the section « defauts-avant: » of {report} (no line: nothing to "
                   "constate; the seat writes « - <id> | corrigé | ecran: <n> | … »)"]
    answer, screen, rest = answers[item]
    if answer != "corrigé":
        return 1, [f"{item}: the control seat says « {answer} » ({rest[:80]}): not constaté (only « corrigé » is)"]
    if not screen:
        return 1, [f"{item}: the control seat says « corrigé » without its screen number: a « corrigé » is read again on "
                   "its screen (« - <id> | corrigé | ecran: <n> | … »)"]
    where = os.path.relpath(os.path.abspath(seat), os.path.abspath(root)).replace(os.sep, "/").rstrip("/")
    rewrite(register, {i: {"etat": "constaté", "constat": f"{where}#{screen}"} for i, _ in takeable})
    return 0, [f"constaté : {tour} {item} ({r.get('siege')}#{r.get('ecran')}) ← {where}#{screen}" for _, r in takeable]


def main(argv):
    def opt(name, default):
        if name in argv:
            i = argv.index(name)
            value = argv[i + 1] if i + 1 < len(argv) else default
            del argv[i:i + 2]
            return value
        return default
    argv = list(argv)
    register = opt("--registre", os.path.join(ROOT, REGISTER))
    root = opt("--racine", ROOT)
    if len(argv) == 4 and argv[0] == "--constate":
        code, lines = constate(argv[1], argv[2], argv[3], register, root)
        for line in lines:
            print(line)
        return code
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
