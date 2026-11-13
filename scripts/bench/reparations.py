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

  reparations.py --lot <branch> base:<commit the branch left> tete:<the integration's head>

The portal's rule (scripts/bench/fusion-equipes.sh, from batch 5; pre-vol.sh says it before): the lot's own lines of
the register are those of the branch that are neither at the base nor at the integration's head (a line the chief wrote
on the integration and the lot merged in is not the lot's; nothing is read in the branch alone but the lines it brings).
Each of them says « tenté » or « pas pris » (any case, accents or not); never « corrigé » (given by nobody from round 6
on, and a new line of an older round is new all the same), « rejoué » (the portal's), « constaté » (the chief's), nor a
word the register does not know. Its acceptance sentence and its seat of origin are the sorter's: the same as a line of
that round and item at the base or the head, or empty when none has one. Its « constat » cell is empty (the chief's).
The header is not the lot's to change. Prints one line per refusal, exit 1; nothing when all is well.
Not seen: a line the lot takes out (a « constaté » removed); whether the commit and the proof it names are the repair's.

  reparations.py --rejoue <branch> base:<commit> tete:<the head before the merge> rejeux:<file> fusion:<merged commit>
                 [filtre:<Gradle filter the portal ran green> …] [--registre <corrections.tsv>]

The portal's own writing, after a green merge: each line the lot brought (as above) that says « tenté » becomes
« rejoué » in the working file when its proof, the « test » cell, held on the merged head: a replay id whose last line in
<file> (rejeu-defauts.sh's « <id> : oracle tenu ») is « oracle tenu », or « Classe#méthode » of a test the portal ran
green (« Classe.méthode » or the whole « Classe » among its filters). Its note gets « rejoué au portail <sha> : <proof> ».
Every other line is kept byte for byte. Not seen: that the proof was played in the conditions of the seat of origin
(start.txt), nor that it was red before the repair (rule 2): the lot's report and its fresh review say so.
Standard library only.
"""
import os
import re
import subprocess
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


def counts(rows, tour):
    """The four figures of a round (plan, rule 5): each defect (round, item) once, at its best state (constaté >
    rejoué > tenté > pas pris); « avant »: the states of the lines from before the rule (corrigé, banc, voulu), by defect."""
    rank = {s: i for i, s in enumerate(STATES)}
    best, before = {}, {}
    for r in rows:
        if r.get("tour") != tour:
            continue
        said = state(r.get("etat"))
        if said in rank:
            if rank[said] > rank.get(best.get(r.get("item")), -1):
                best[r.get("item")] = said
        elif said:
            before.setdefault(r.get("item"), said)
    out = {s: sum(1 for v in best.values() if v == s) for s in STATES}
    out["avant"] = {}
    for item, said in before.items():
        if item not in best:
            out["avant"][said] = out["avant"].get(said, 0) + 1
    return out


def line(rows, tour):
    """The dashboard's line « réparations » of a round."""
    n = counts(rows, tour)
    taken = n["tenté"] + n["rejoué"] + n["constaté"]
    text = (f"Réparations (registre corrections.tsv) : constaté {n['constaté']}, rejoué {n['rejoué']}, tenté {n['tenté']}, "
            f"pas pris {n['pas pris']} — réparés : {n['constaté']} sur {taken} pris (seul « constaté » se dit corrigé ; "
            "les pas pris à part)")
    if n["avant"]:
        text += " ; états d'avant la règle : " + ", ".join(f"{k} {v}" for k, v in n["avant"].items())
    return text


LOT_STATES = ("tenté", "pas pris")


def show(rev, path):
    r = subprocess.run(["git", "show", f"{rev}:{path}"], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else None


def brought(branch, base, head):
    """(the branch's header, [(line, row)] the lines of the register the branch has that neither the base nor the head
    has, known: {(tour, item): [rows at the base or the head]}, header line known or not)."""
    text = show(branch, REGISTER)
    if text is None:
        return None, [], {}, True
    known_lines, known = set(), {}
    for rev in (base, head):
        other = show(rev, REGISTER)
        if other is None:
            continue
        known_lines |= {l.rstrip("\r") for l in other.split("\n")}
        for r in load(other)[1]:
            known.setdefault((r.get("tour"), r.get("item")), []).append(r)
    header, out, header_known = None, [], True
    for line in text.split("\n"):
        if not line.strip() or line.startswith("#"):
            continue
        line = line.rstrip("\r")
        cells = line.split("\t")
        if header is None:
            header = cells
            # (The register's own header, COLUMNS, is no invention of a lot: the lot « constate » brought it.)
            header_known = line in known_lines or not known_lines or cells == COLUMNS
            continue
        if line not in known_lines:
            # Review of « constate », G1: a line is read with the register's own columns, never with its branch's header
            # (a branch cut before the three columns has 9; the cells after them were invisible). Its cells beyond
            # its branch's header, or beyond the register's columns, are a refusal (« long »).
            row = dict(zip(COLUMNS, cells + [""] * (len(COLUMNS) - len(cells))))
            row["_long"] = len(cells) if len(cells) > min(len(header), len(COLUMNS)) else 0
            row["_header"] = len(header)
            out.append((line, row))
    return header, out, known, header_known


def lot_refusals(branch, base, head):
    header, lines, known, header_known = brought(branch, base, head)
    if header is None:
        return []
    out = []
    if not header_known:
        out.append(f"{REGISTER}: the lot changes the register's header (« {' '.join(header)} »): its columns are not the lot's")
    for line, r in lines:
        name = f"« {r.get('tour')} {r.get('item')} {r.get('siege')}#{r.get('ecran')} »"
        if r["_long"]:
            out.append(f"{REGISTER} line {name}: {r['_long']} cells, its branch's header has {r['_header']} and the register "
                       f"{len(COLUMNS)} ({' '.join(COLUMNS)}): merge the integration (its header has the register's columns), "
                       "and write no cell beyond them")
            continue
        said = state(r.get("etat"))
        if said not in LOT_STATES:
            who = {"rejoué": "the portal's", "constaté": "the chief's, after a control seat", "corrigé": "given by nobody since round 6"}
            out.append(f"{REGISTER} line {name} says « {said} »: a lot writes « tenté » (or « pas pris »), never "
                       f"« {said} » ({who.get(said, 'a state the register does not know')}; « rejoué » is the portal's, "
                       "« constaté » the chief's after a control seat)")
        others = known.get((r.get("tour"), r.get("item")), [])
        for column, what in (("phrase_acceptation", "acceptance sentence"), ("siege_origine", "seat of origin")):
            mine = (r.get(column) or "").strip()
            theirs = {(o.get(column) or "").strip() for o in others} - {""}
            if (mine and mine not in theirs) or (theirs and not mine):
                out.append(f"{REGISTER} line {name}: its {what} is not the sorter's (« {mine[:60]} »"
                           + (f", the sorter wrote « {sorted(theirs)[0][:60]} »" if theirs else ", the sorter wrote none")
                           + "): the sorter writes it, the lot repairs it as written")
        if (r.get("constat") or "").strip():
            out.append(f"{REGISTER} line {name}: its « constat » cell is the chief's (« {r['constat'][:60]} »)")
    return out


def last_verdicts(text):
    out = {}
    for line in (text or "").splitlines():
        m = re.match(r"^\s*(\S+)\s+:\s+(oracle\b.*\S)\s*$", line)
        if m:
            out[m.group(1)] = m.group(2)
    return out


def held(proof, verdicts, filters):
    proof = (proof or "").strip().strip("`")
    if not proof or proof == "-":
        return False
    if verdicts.get(proof) == "oracle tenu":
        return True
    m = re.fullmatch(r"([A-Za-z_][A-Za-z0-9_]*)[#.]([A-Za-z_][A-Za-z0-9_]*)", proof)
    return bool(m) and (f"{m.group(1)}.{m.group(2)}" in filters or m.group(1) in filters)


def rejoue(branch, base, head, rejeux, merged, filters, register):
    """(exit code, lines to print)."""
    header, lines, _, _ = brought(branch, base, head)
    text = read(register)
    if header is None or text is None:
        return 0, []
    verdicts = last_verdicts(read(rejeux) or "")
    mine = {line: r for line, r in lines if state(r.get("etat")) == "tenté"}
    changes, said = {}, []
    file_header, rows = indexed_rows(text)
    raw = text.split("\n")
    for i, r in rows:
        line = raw[i].rstrip("\r")
        if line not in mine or not held(r.get("test"), verdicts, filters):
            continue
        proof = r.get("test").strip().strip("`")
        note = (r.get("note") or "").strip()
        changes[i] = {"etat": "rejoué", "note": (note + " ; " if note else "") + f"rejoué au portail {merged[:8]} : {proof}"}
        said.append(f"rejoué : {r.get('tour')} {r.get('item')} ({r.get('siege')}#{r.get('ecran')}) : {proof} tenu sur {merged[:8]}")
    if changes:
        rewrite(register, changes)
    return 0, said


def main(argv):
    def opt(name, default):
        if name in argv:
            i = argv.index(name)
            value = argv[i + 1] if i + 1 < len(argv) else default
            del argv[i:i + 2]
            return value
        return default
    argv = list(argv)
    # (--rejoue writes the register of the repository it runs in, the portal's: relative to the working directory.)
    explicit = "--registre" in argv
    register = opt("--registre", os.path.join(ROOT, REGISTER))
    root = opt("--racine", ROOT)
    if len(argv) == 4 and argv[0] == "--constate":
        code, lines = constate(argv[1], argv[2], argv[3], register, root)
        for line in lines:
            print(line)
        return code
    if len(argv) >= 2 and argv[0] in ("--lot", "--rejoue"):
        opts = {}
        filters = []
        for a in argv[2:]:
            k, _, v = a.partition(":")
            if k == "filtre":
                filters.append(v)
            elif v:
                opts[k] = v
        need = ("base", "tete") + (("rejeux", "fusion") if argv[0] == "--rejoue" else ())
        if any(not opts.get(k) for k in need):
            print(f"reparations.py {argv[0]}: {', '.join(k + ':<…>' for k in need)} are needed")
            return 2
        if argv[0] == "--lot":
            refusals = lot_refusals(argv[1], opts["base"], opts["tete"])
            for line in refusals:
                print(line)
            return 1 if refusals else 0
        if not explicit:
            register = REGISTER
        code, lines = rejoue(argv[1], opts["base"], opts["tete"], opts["rejeux"], opts["fusion"], filters, register)
        for line in lines:
            print(line)
        return code
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
