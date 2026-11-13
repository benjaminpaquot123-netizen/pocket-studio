#!/usr/bin/env python3
"""The register of the repairs, docs/audits/ultra/corrections.tsv (docs/audits/ultra/PLAN-REPARATIONS-CONSTATEES.md):
a repair is « done » only when a tester saw it.

One line per tester's citation of a defect (round, item, seat « X/s », screen), its state, and since round 6:
  phrase_acceptation  what the tester must see after the repair, with its conditions, written by the sorter (rule 1);
  siege_origine       the seat that saw the defect (« tour-6/lotB/a »), whose start.txt gives the conditions of the
                      proof (language, device, font, mode, options; rule 2);
  constat             for « constaté »: the control seat's folder and the screen of its « corrigé » line (« …#21 »).
The lines of rounds 1 to 5 keep their text (9 cells, states « corrigé », « banc », « voulu »): a missing cell is empty.
A line is known by its round, item, seat, screen and card (« cible »): KEY.

One reader for every program (load(), review of « constate », M2): lines are cut on « \\n » only, and a register that
holds any other line separator or control character (U+2028, U+2029, \\v, \\f, \\x1c-\\x1e, \\x85, \\r, the C0 controls
but the tab) is unreadable (RegisterError), never read another way by another program.

The four states of a repair (rule 5), and who gives each:
  pas pris   the sorter                  the defect is in no lot (the sorter writes each retained defect's line)
  tenté      the team                    its own line, from « pas pris » (or « tenté »): a commit, its proof
  rejoué     the portal                  the proof of rule 2 held on the merged head (scripts/bench/fusion-equipes.sh)
  constaté   the chief, from a seat      the « corrigé » line of a control seat, with its screen number
Only « constaté » is said « corrigé » to Benjamin.

  reparations.py --constate <tour-N> <item> <control seat folder> [--registre <corrections.tsv>] [--racine <repo>]

The chief's command. The folder is a control seat of the same round, under <repo>/docs/audits/ultra/sieges/<tour-N>/
controle/. Its rapport.md says on top « code testé : <sha> », a commit of <repo> that holds the repair: the commit each
line names (its « commit » cell) is that commit or one of its ancestors. Its section « defauts-avant: » has the
defect's line (« - <id> | corrigé | ecran: <n> | … », and the forms of round 6, « - A7 | corrigé (écran 21 : …) »,
« - <id> | corrigé (écran 98) : … »), saying « corrigé » and its screen (« corrigé ? non … » is no « corrigé »). Then
every line of that round and item in « tenté » or « rejoué » becomes « constaté », its « constat » the folder (from the
repository's root) and the screen. Refused (exit 1, nothing written): « autre », « encore là », « pas atteint », no
screen, no line, a defect « pas pris », a folder elsewhere, a seat that played a code without the repair.

  reparations.py --lot <branch> base:<commit the branch left> tete:<the integration's head>

The portal's rule (scripts/bench/fusion-equipes.sh, from batch 8; pre-vol.sh says it before), the branch against the
base and the integration's head, line by line (KEY), never in the branch alone:
- every line of the branch is read with the register's own columns (COLUMNS), never its branch's header; a line with
  more cells than its branch's header (a branch cut before the columns) or than COLUMNS is refused;
- a line the branch brings (in neither the base nor the head) must be a line of the base or the head (same KEY) that the
  lot turns from « pas pris » or « tenté » to « tenté », changing only « etat », « commit », « test » and « note »: the
  lot adds no line (not even « pas pris », the sorter's), writes no « corrigé », « rejoué », « constaté » or unknown
  word, changes no other cell (the sorter's sentence and seat of origin, the chief's « constat »), lowers nothing (a
  line « rejoué » or « constaté » at the head stays so);
- a line of the base the branch no longer has, while the head still has it, is removed by the lot: refused;
- the header is not the lot's (the register's own COLUMNS excepted: the lot « constate » brought it);
- a register that is unreadable (see the reader) in the branch is refused.
Prints one line per refusal, exit 1; nothing when all is well.

  reparations.py --rejoue <branch> base:<commit> tete:<the head before the merge> rejeux:<file> fusion:<merged commit>
                 [filtre:<Gradle filter the portal ran green> …] [--registre <corrections.tsv>]

The portal's own writing, after a green merge: each line the lot brought (as above) that says « tenté » becomes
« rejoué » in the working file when its proof, the « test » cell, held on the merged head: a replay id whose last line in
<file> (rejeu-defauts.sh's « <id> : oracle tenu ») is « oracle tenu »; or « Classe#méthode », a @Test (no @Ignore) of
app/src/test at the merged commit (read as rapport-zone.py reads a path's test), and one of the filters the portal ran
green covers it: « Classe », « pkg.Classe », « *Classe », « Classe.méthode », « pkg.Classe.méthode » (a wildcard
elsewhere proves nothing). Its note gets « rejoué au portail <sha> : <proof> ». Every other line is kept byte for byte.
Not seen: that the proof was played in the conditions of the seat of origin (start.txt), nor that it was red before the
repair (rule 2): the lot's report and its fresh review say so.
Standard library only.
"""
import importlib.util
import os
import re
import subprocess
import sys
import unicodedata

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
REGISTER = "docs/audits/ultra/corrections.tsv"
COLUMNS = ["tour", "item", "siege", "ecran", "etat", "commit", "test", "note", "cible",
           "phrase_acceptation", "siege_origine", "constat"]
KEY = ("tour", "item", "siege", "ecran", "cible")
STATES = ("pas pris", "tenté", "rejoué", "constaté")
GIVEN_BY = {"pas pris": "le trieur", "tenté": "l'équipe", "rejoué": "le portail",
            "constaté": "le chef, d'après un siège de contrôle"}
# Rounds 1 to 5: « corrigé » (a lot's own word, before the rule), « banc », « voulu ».
OLD_STATES = ("corrigé", "banc", "voulu")
OLD_REPAIR = ("corrigé",)
# Review of « constate », M4: a lot moves its own line from « pas pris » (or « tenté ») to « tenté », nothing else.
LOT_STATES = ("tenté",)
LOT_CELLS = ("etat", "commit", "test", "note")
# Review of « constate », M2: every character that some reader (str.splitlines) takes for an end of line, and the
# other control characters but the tab and the newline.
FORBIDDEN = re.compile("[\u2028\u2029\x85\x00-\x08\x0b-\x1f\x7f]")


class RegisterError(ValueError):
    pass


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


def unreadable(text):
    """The characters that make the register unreadable: « line N: U+XXXX », or []."""
    out = []
    for n, line in enumerate((text or "").split("\n"), 1):
        for m in FORBIDDEN.finditer(line):
            out.append(f"line {n}: U+{ord(m.group()):04X}")
    return out


def indexed_rows(text, strict=True):
    """(header, [(index of the line in the file, row)]): the one reader. « # » lines are comments; a short line's
    missing cells are empty. strict: a forbidden character raises RegisterError."""
    bad = unreadable(text)
    if bad and strict:
        raise RegisterError(f"unreadable register: a line separator or control character ({', '.join(bad[:3])}): "
                            "the register's lines end with « \\n » only")
    header, out = None, []
    for i, line in enumerate((text or "").split("\n")):
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.split("\t")
        if header is None:
            header = cells
            continue
        out.append((i, dict(zip(header, cells + [""] * (len(header) - len(cells))))))
    return header or list(COLUMNS), out


def load(text, strict=True):
    """(header, [rows as {column: cell}]) (indexed_rows without the indexes)."""
    header, rows = indexed_rows(text, strict)
    return header, [r for _, r in rows]


def read(path):
    try:
        with open(path, encoding="utf-8", newline="") as f:
            return f.read()
    except OSError:
        return None


AVANT_HEAD = re.compile(r"^\s*d[ée]fauts[- ]avant\s*:", re.I)
SECTION = re.compile(r"^[A-Za-z][\w.\-?]*\s*:")
# (Review of « constate », m1: the answer is followed by the end, « ( », « | » or « : », and no « non », « pas » or
# « ? » after it: « corrigé ? non, toujours là » is no « corrigé ».)
AVANT_LINE = re.compile(r"^\s*[-*]\s*`?([^\s|:`]+)`?\s*[|:]\s*(corrig[ée]e?|autre|encore\s+l[àa]|pas\s+atteint)"
                        r"(?=\s*(?:$|[(|:]))(.*)$", re.I)
DENIED = re.compile(r"^\s*[(|:]?\s*(?:non\b|pas\b|\?)", re.I)
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
        if m and not DENIED.match(m.group(3)):
            word = plain(m.group(2))
            answer = ("corrigé" if word.startswith("corrig") else "autre" if word == "autre"
                      else "encore là" if word.startswith("encore") else "pas atteint")
            screen = SCREEN.search(m.group(3))
            out.setdefault(m.group(1), (answer, screen.group(1) if screen else None, m.group(3).strip(" |:")))
    return out


def rewrite(path, changes):
    """Rewrites the lines of the register whose index (0 = the file's first line) is in changes {index: {column: value}};
    every other line is kept byte for byte (the register is read strictly first: no « \\r » to keep)."""
    text = read(path)
    header, _ = indexed_rows(text)
    lines = text.split("\n")
    for i, values in changes.items():
        cells = lines[i].split("\t")
        cells += [""] * (len(header) - len(cells))
        for k, v in values.items():
            cells[header.index(k)] = v
        lines[i] = "\t".join(cells)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write("\n".join(lines))


def git_root(root, *args):
    r = subprocess.run(["git", "-C", root, *args], capture_output=True, text=True)
    return r.returncode, r.stdout.strip()


def seat_head(text):
    """« code testé » of a seat report's head: its lines before « siege: », the first eight at most."""
    for line in (text or "").splitlines()[:8]:
        if line.startswith("siege:"):
            break
        m = re.match(r"^code testé\s*:\s*(\S*)\s*$", line)
        if m:
            return m.group(1)
    return None


def constate(tour, item, seat, register, root):
    """(exit code, lines to print)."""
    text = read(register)
    if text is None:
        return 2, [f"no register {register}"]
    try:
        header, rows = indexed_rows(text)
    except RegisterError as e:
        return 2, [f"{register}: {e}"]
    missing = [c for c in ("etat", "constat", "commit") if c not in header]
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
    # Review of « constate », M5: a control seat of this round, under the repository's sieges/<tour>/controle/.
    controls = os.path.realpath(os.path.join(root, "docs", "audits", "ultra", "sieges", tour, "controle"))
    folder = os.path.realpath(seat)
    if os.path.commonpath([folder, controls]) != controls or folder == controls:
        return 1, [f"{item}: {seat} is not a control seat of {tour} (a folder under {os.path.relpath(controls, root)}/)"]
    report = os.path.join(seat, "rapport.md")
    said = read(report)
    if said is None:
        return 1, [f"{item}: no report {report} (the control seat's rapport.md, with its section « defauts-avant: »)"]
    answers = before_defects(said)
    if item not in answers:
        return 1, [f"{item}: no line for this defect in the section « defauts-avant: » of {report} (no line, or no "
                   "readable answer: nothing to constate; the seat writes « - <id> | corrigé | ecran: <n> | … »)"]
    answer, screen, rest = answers[item]
    if answer != "corrigé":
        return 1, [f"{item}: the control seat says « {answer} » ({rest[:80]}): not constaté (only « corrigé » is)"]
    if not screen:
        return 1, [f"{item}: the control seat says « corrigé » without its screen number: a « corrigé » is read again on "
                   "its screen (« - <id> | corrigé | ecran: <n> | … »)"]
    # M5: the seat played a commit that holds the repair: each line's commit is that commit or one of its ancestors.
    played = seat_head(said)
    code, full = git_root(root, "rev-parse", "-q", "--verify", f"{played}^{{commit}}") if played else (1, "")
    if not played or not re.fullmatch(r"[0-9a-f]{7,64}", played) or code != 0 or not full.startswith(played):
        return 1, [f"{item}: no « code testé : <sha> » of a commit of this repository on top of {report} "
                   f"(« {played or ''} »): the seat's code is not known"]
    for _, r in takeable:
        fix = (r.get("commit") or "").strip()
        code, fixed = git_root(root, "rev-parse", "-q", "--verify", f"{fix}^{{commit}}") if re.fullmatch(r"[0-9a-f]{7,64}", fix) else (1, "")
        if code != 0:
            return 1, [f"{item}: the line {r.get('siege')}#{r.get('ecran')} names no commit of this repository (« {fix} »): "
                       "the repair a seat saw is a commit"]
        if subprocess.run(["git", "-C", root, "merge-base", "--is-ancestor", fixed, full]).returncode != 0:
            return 1, [f"{item}: the seat played {full[:8]}, which does not hold the repair {fixed[:8]} of the line "
                       f"{r.get('siege')}#{r.get('ecran')}: not constaté"]
    where = os.path.relpath(os.path.abspath(seat), os.path.abspath(root)).replace(os.sep, "/").rstrip("/")
    rewrite(register, {i: {"etat": "constaté", "constat": f"{where}#{screen}"} for i, _ in takeable})
    return 0, [f"constaté : {tour} {item} ({r.get('siege')}#{r.get('ecran')}) ← {where}#{screen}, code {full[:8]}"
               for _, r in takeable]


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


def show(rev, path):
    # (Bytes, decoded here: text mode would turn a « \r » into a « \n » and hide it from the reader.)
    r = subprocess.run(["git", "show", f"{rev}:{path}"], capture_output=True)
    return r.stdout.decode("utf-8", "replace") if r.returncode == 0 else None


def key(r):
    return tuple((r.get(k) or "").strip() for k in KEY)


def brought(branch, base, head):
    """The register of the branch against the base and the head. Returns a dict: header, lines [(line, row)] the
    branch brings (in neither the base nor the head; read with COLUMNS), header_known, base / head {KEY: row},
    branch_keys, unreadable (the branch's register's forbidden characters). None when the branch has no register."""
    text = show(branch, REGISTER)
    if text is None:
        return None
    known_lines, sides = set(), {}
    for name, rev in (("base", base), ("head", head)):
        other = show(rev, REGISTER)
        sides[name] = {}
        if other is None:
            continue
        known_lines |= set(other.split("\n"))
        for r in load(other, strict=False)[1]:
            sides[name][key(r)] = r
    out = {"header": None, "lines": [], "header_known": True, "base": sides["base"], "head": sides["head"],
           "branch_keys": set(), "unreadable": unreadable(text)}
    for line in text.split("\n"):
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.split("\t")
        if out["header"] is None:
            out["header"] = cells
            # (The register's own header, COLUMNS, is no invention of a lot: the lot « constate » brought it.)
            out["header_known"] = line in known_lines or not known_lines or cells == COLUMNS
            continue
        # Review of « constate », G1: a line is read with the register's own columns, never with its branch's header
        # (a branch cut before the three columns has 9; the cells after them were invisible). Its cells beyond its
        # branch's header, or beyond the register's columns, are a refusal (« long »).
        row = dict(zip(COLUMNS, cells + [""] * (len(COLUMNS) - len(cells))))
        out["branch_keys"].add(key(row))
        if line not in known_lines:
            row["_long"] = len(cells) if len(cells) > min(len(out["header"]), len(COLUMNS)) else 0
            row["_header"] = len(out["header"])
            out["lines"].append((line, row))
    return out


def lot_refusals(branch, base, head):
    seen = brought(branch, base, head)
    if seen is None:
        return []
    out = []
    if seen["unreadable"]:
        out.append(f"{REGISTER}: a line separator or control character ({', '.join(seen['unreadable'][:3])}): the register's "
                   "lines end with « \\n » only (another reader would cut them elsewhere)")
    if not seen["header_known"]:
        out.append(f"{REGISTER}: the lot changes the register's header (« {' '.join(seen['header'])} »): its columns are not "
                   "the lot's")
    rank = {s: i for i, s in enumerate(STATES)}
    for line, r in seen["lines"]:
        name = f"« {r.get('tour')} {r.get('item')} {r.get('siege')}#{r.get('ecran')} »"
        if r["_long"]:
            out.append(f"{REGISTER} line {name}: {r['_long']} cells, its branch's header has {r['_header']} and the register "
                       f"{len(COLUMNS)} ({' '.join(COLUMNS)}): merge the integration (its header has the register's columns), "
                       "and write no cell beyond them")
            continue
        said = state(r.get("etat"))
        if said not in LOT_STATES:
            who = {"rejoué": "the portal's", "constaté": "the chief's, after a control seat", "corrigé": "given by nobody since round 6",
                   "pas pris": "the sorter's"}
            out.append(f"{REGISTER} line {name} says « {said} »: a lot turns its own line to « tenté », never "
                       f"« {said} » ({who.get(said, 'a state the register does not know')}; « rejoué » is the portal's, "
                       "« constaté » the chief's after a control seat, « pas pris » the sorter's)")
        priors = [p for p in (seen["head"].get(key(r)), seen["base"].get(key(r))) if p is not None]
        if not priors:
            out.append(f"{REGISTER} line {name}: the lot adds a line; the sorter writes the register's lines (« pas pris », "
                       "with the acceptance sentence and the seat of origin), a lot only turns its own to « tenté »")
            continue
        at_head = seen["head"].get(key(r))
        if at_head is not None and rank.get(state(at_head.get("etat")), -1) > rank["tenté"]:
            out.append(f"{REGISTER} line {name}: the lot lowers « {state(at_head.get('etat'))} » (at the integration's head) "
                       f"to « {said} »")
            continue
        if said in LOT_STATES and not any(state(p.get("etat")) in ("pas pris", "tenté") for p in priors):
            out.append(f"{REGISTER} line {name}: « {said} » from « {state(priors[0].get('etat'))} »: a lot turns « pas pris » "
                       "(or its own « tenté ») to « tenté », nothing else")
        what = {"phrase_acceptation": "acceptance sentence (the sorter's)", "siege_origine": "seat of origin (the sorter's)",
                "constat": "« constat » cell (the chief's)"}
        changed = [c for c in COLUMNS if c not in LOT_CELLS
                   and all((r.get(c) or "").strip() != (p.get(c) or "").strip() for p in priors)]
        for c in changed:
            out.append(f"{REGISTER} line {name}: the lot changes its {what.get(c, '« ' + c + ' »')} "
                       f"(« {(priors[0].get(c) or '')[:50]} » → « {(r.get(c) or '')[:50]} »): a lot changes only "
                       f"{', '.join(LOT_CELLS)}; the sorter writes the sentence and the seat of origin, the lot repairs "
                       "it as written")
    # A line of the base the branch no longer has, the head still having it: removed by the lot.
    for k, r in seen["base"].items():
        if k not in seen["branch_keys"] and k in seen["head"]:
            out.append(f"{REGISTER} line « {r.get('tour')} {r.get('item')} {r.get('siege')}#{r.get('ecran')} »: the lot "
                       "removes it (a line of the register is never taken out by a lot)")
    return out


def last_verdicts(text):
    out = {}
    for line in (text or "").splitlines():
        m = re.match(r"^\s*(\S+)\s+:\s+(oracle\b.*\S)\s*$", line)
        if m:
            out[m.group(1)] = m.group(2)
    return out


def covers(f, cls, method):
    """The Gradle filter f runs the test cls.method: « Classe », « pkg.Classe », « *Classe », or the same with
    « .méthode »; any other wildcard proves nothing."""
    f = f.strip()
    if f.startswith("*"):
        f = f[1:]
        if f.startswith("."):
            f = f[1:]
    if not f:
        return False
    # (A wildcard left in f never equals a class or a method name below: it proves nothing.)
    parts = f.split(".")
    return parts[-1] == cls or (len(parts) >= 2 and parts[-2] == cls and parts[-1] == method)


_zone = None


def is_app_test(rev, cls, method):
    """Classe#méthode is a @Test (no @Ignore) of app/src/test at rev, as rapport-zone.py reads a path's test."""
    global _zone
    if _zone is None:
        spec = importlib.util.spec_from_file_location("rapport_zone", os.path.join(HERE, "rapport-zone.py"))
        _zone = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(_zone)
    return _zone.is_app_test(rev, cls, method)


def held(proof, verdicts, filters, rev):
    proof = (proof or "").strip().strip("`")
    if not proof or proof == "-":
        return False
    if verdicts.get(proof) == "oracle tenu":
        return True
    m = re.fullmatch(r"([A-Za-z_][A-Za-z0-9_]*)#([A-Za-z_][A-Za-z0-9_]*)", proof)
    return bool(m) and any(covers(f, m.group(1), m.group(2)) for f in filters) and is_app_test(rev, m.group(1), m.group(2))


def rejoue(branch, base, head, rejeux, merged, filters, register):
    """(exit code, lines to print)."""
    seen = brought(branch, base, head)
    text = read(register)
    if seen is None or text is None:
        return 0, []
    try:
        _, rows = indexed_rows(text)
    except RegisterError as e:
        return 2, [f"{register}: {e}"]
    verdicts = last_verdicts(read(rejeux) or "")
    mine = {line for line, r in seen["lines"] if state(r.get("etat")) == "tenté" and not r["_long"]}
    changes, said = {}, []
    raw = text.split("\n")
    for i, r in rows:
        if raw[i] not in mine or not held(r.get("test"), verdicts, filters, merged):
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
