#!/usr/bin/env python3
"""PLAN-RESTE 7.1: the zone report of a team's branch, as fusion-equipes.sh and pre-vol.sh read it (from batch 2).

  rapport-zone.py <branch> <report path> [ajout:<replay id the branch adds to rejeux/> …]

Run from the repository's root. The report (docs/audits/ultra/<tour-N>-<zone>-rapport.md) is read in the branch only.
Accepted only when it has a line « tests: <filter> [<filter> …] », the team's targeted tests (Gradle --tests filters),
or « tests: aucun : <raison> ».
And its « liste des chemins » (docs/audits/AUDIT-ULTRA.md §12.6): at least one line
« chemin: <écran> | test: Classe#méthode », every « chemin: » line in that format, and each Classe#méthode a test of
app/src/test in the branch that runs (@Test, no @Ignore): the class owns the function, read as the judge reads it
(verifie-chantier.py: class_methods, still_runs).
And each replay the branch adds to docs/audits/ultra/rejeux/ has its last verdict in the report, the line
rejeu-defauts.sh prints (« <id> : oracle tenu »): the last such line is « oracle tenu ». No verdict is committed
anywhere else (rejeu-defauts.sh keeps no trace of a waiting replay), so a replay without one is refused too: a
replay in rejeux/ that is not held undoes every merge after it.
Prints « filtre: <filter> » per filter when accepted; otherwise one line per refusal, exit 1. Standard library only.

AUDIT-ULTRA §13.9 (lot « portail3 », from batch 4): a lot that says it fixes a blocking or grave defect adds its replay.

  rapport-zone.py --corrections <branch> <report path> base:<commit the branch left> tete:<the integration's head>
                                 tour:<tour-N> [ajout:<id> …]

The report says each defect the lot fixes in one line (a list bullet and backquotes are allowed):

  corrige: <id du défaut> | gravité: bloquant|grave|moyen|mineur | rejeu: <id du rejeu>
  corrige: <id du défaut> | gravité: bloquant|grave | rejeu impossible : <raison>

For « bloquant » and « grave », « rejeu: <id> » is a folder the branch ADDS to docs/audits/ultra/rejeux/ (one of the
« ajout: » ids; its line « <id> : oracle tenu » is asked by the rule above), or « rejeu impossible : » gives a real
reason (three words at least). For « moyen » and « mineur » the third part is free to leave out.
A lot does not get round the rule by leaving the line out, where the repository knows the defect's gravity:
- a row of docs/audits/ultra/verdicts.tsv whose « gravite » is bloquant or grave and whose « etat » the branch turns
  to « corrigé » (scripts/bench/marque-corrige.sh) needs its « corrige: <item> » line;
- a « réel » row of the round's triage (docs/audits/ultra/sieges/<tour-N>/verdicts.tsv, columns « n° » and « gravité
  retenue »; read in the branch, or at the integration's head when the branch left before the triage was there)
  that is bloquant or grave and that the report, or the subject of a commit of the branch, names needs its
  « corrige: <n°> » line, or the line « ne corrige pas: <n°> : <raison> » (three words at least) when the lot only
  cites it;
- a « corrige: » line cannot give such a defect a lower gravity than its table.
Not seen (docs/decisions/tour-6-portail3.md): a defect no table knows, or that neither the report nor a commit
names; whether the replay named is the defect's own; whether a reason is a good one (the fresh review reads them).
Prints one line per refusal, exit 1; nothing when all is well.

AUDIT-ULTRA §13.5, last point (lot « portail4 », from batch 5): no lot of the tutorial enters before a fresh beginner
replayed it on the lot's branch.

  rapport-zone.py --tuto <branch> <report path> base:<commit the branch left> tete:<the integration's head> tour:<tour-N>

The lot touches the tutorial zone when a file it changes (changed since the base, and not the integration head's
version: a file that differs only because the branch merged the integration in is not the lot's) is application code
or a resource of the tutorial:
  <module>/src/main/…/tuto/…   <module>/src/main/…/tutos/…   <module>/src/main/…/i18n/<lang>/tuto*.json
or is one of the four files the tutorial shares with the rest of the application (AUDIT-ULTRA §13.3: PianoRoll.kt,
RhythmScreen.kt, PlainWords.kt, TrackSheet.kt, under src/main) while the zone report says « tuto » (the word, any case,
outside fenced blocks). Once a lot touches the zone, the shared files it changes are tutorial code whatever its report
says: the seat plays after their last change too. File names are read with « -z » (git quotes « tuto_café.json »).
Not the zone: the tutorial's tests (src/test), the reference screenshots, the docs, the bench.
Then the zone report carries, per control seat, the line (a list bullet, a number, a table row, bold and backquotes
are allowed: a « controle: » line is read or refused, never passed over):

  controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0

- at least one such line; EVERY « controle: » line well-formed, with 0 and 0;
- <sha> (7 to 40 hex letters) is a commit of the branch (an ancestor of its head), not older than the lot's last change
  of the tutorial: every commit of the lot (tete..branch, merges left out) that changes one of the zone's files above
  is <sha> or one of its ancestors, and those files are the same at <sha> and at the branch's head (a merge changed
  none of them after the seat played);
- <id> is a seat whose report is in the branch, docs/audits/ultra/sieges/<tour-N>/controle/<id>/rapport.md (<id>: one
  folder, « Ha », or a lot and its seat, « lotK/a »), whose head (the lines before « siege: », the first eight at most,
  as scripts/bench/sortie-de-siege.py writes and reads them) says « code testé : <the same commit> », and none of
  whose chapter lines says « bloqué » (« chapitre: <n titre> | ecran: <n> | bloqué | … » or
  « - chapitre <n> « titre » : bloqué (…) », the two forms of tour 6).
Not seen (docs/decisions/tour-6-portail4.md): whether the seat was a fresh beginner, played the whole tutorial, in the
same language and profile as the seat before; a grave the seat's report describes without the line counting it; a
lot that changes only shared files for the tutorial without its report saying « tuto »; a seat's report written by
hand in the branch (code-teste.tsv is not read).
Prints one line per refusal, exit 1. Accepted: « tuto: … » (the zone's files and the seats) when the lot touches the
zone, nothing at all when it does not.
"""
import importlib.util
import os
import re
import subprocess
import sys

# (No __pycache__ left in scripts/bench/: the portal merges in that tree.)
sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("verifie_chantier", os.path.join(HERE, "verifie-chantier.py"))
judge = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(judge)

TESTS = re.compile(r"^\s*(?:[-*]\s*)?tests\s*:(.*)$", re.M)
NONE = re.compile(r"^aucun\s*[:—-]\s*\S", re.I)
PATH_LINE = re.compile(r"^\s*(?:[-*]\s*)?(?:\[[ xX]\]\s*)?`?chemin\s*:(.*)$", re.M)
PATH = re.compile(r"^\s*(\S[^|]*?)\s*\|\s*test\s*:\s*([A-Za-z_][A-Za-z0-9_]*)#([A-Za-z_][A-Za-z0-9_]*)\s*`?\s*$")
APP_TESTS = "app/src/test"


def is_app_test(branch, cls, method):
    """Classe#méthode is a function of class Classe in a file of app/src/test of the branch, with @Test, no @Ignore."""
    # (Review of « portail4 », G2: « -z », git quotes a name that is not plain ASCII otherwise.)
    r = subprocess.run(["git", "grep", "-z", "-l", "-F", method, branch, "--", APP_TESTS], capture_output=True, text=True)
    for line in filter(None, r.stdout.split("\0")):
        text = judge.without_comments(show(branch, line.split(":", 1)[1]) or "")
        if f"{cls}#{method}" in judge.class_methods(text, False) and judge.still_runs(text, method):
            return True
    return False


def show(branch, path):
    r = subprocess.run(["git", "show", f"{branch}:{path}"], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else None


def replay_refusals(text, path, added):
    out = []
    for rid in added:
        said = re.findall(r"^\s*(?:[-*]\s*)?(?:rejeu\s*:\s*)?`?" + re.escape(rid) + r"\s*:\s*(oracle\b[^`\n]*)", text, re.M)
        if not said:
            out.append(f"replay {rid} added to rejeux/ without its verdict in {path} (the line « {rid} : oracle tenu » "
                       "of rejeu-defauts.sh, played with the fix)")
        elif said[-1].strip() != "oracle tenu":
            out.append(f"replay {rid} added to rejeux/: its last verdict in {path} is « {said[-1].strip()[:80]} », "
                       "not « oracle tenu » (a defect not fixed waits in rejeux-attente/)")
    return out


def check(branch, path, added=()):
    """(refusals, filters)."""
    text = show(branch, path)
    if text is None:
        return [f"no zone report {path} in {branch} (with its « tests: » line and its list of paths)"], []
    found = [m.group(1).strip().strip("`").strip() for m in TESTS.finditer(text)]
    found = [f for f in found if f]
    if not found:
        return [f"no line « tests: » in {path} (the team's targeted tests, or « tests: aucun : <raison> »)"], []
    refusals = replay_refusals(text, path, added)
    lines = [m.group(0).strip() for m in PATH_LINE.finditer(text)]
    if not lines:
        refusals.append(f"no list of paths in {path} (one line « chemin: <écran> | test: Classe#méthode » per place "
                        "that reads or shows the same data)")
    for line in lines:
        m = PATH.match(PATH_LINE.match(line).group(1))
        if not m:
            refusals.append(f"« {line[:80]} » is not « chemin: <écran> | test: Classe#méthode »")
        elif not is_app_test(branch, m.group(2), m.group(3)):
            refusals.append(f"chemin « {m.group(1)[:40]} »: {m.group(2)}#{m.group(3)} is not a @Test of {APP_TESTS} in {branch}")
    if refusals:
        return refusals, []
    filters = []
    for value in found:
        if re.match(r"^aucun\b", value, re.I) and not NONE.match(value):
            return [f"« tests: aucun » without its reason in {path} (« tests: aucun : <raison> »)"], []
        if not NONE.match(value):
            # Review of banc7 (grave 1): Gradle's --tests knows « Classe.méthode », not « Classe#méthode » (the form of a
            # path line and of the teams' sheet): translated, else no test is found and the merge is undone.
            filters += [f.replace("#", ".") for f in re.split(r"[\s,`]+", value) if f]
    return [], filters


FIX_LINE = re.compile(r"^\s*(?:[-*]\s*)?`?corrige\s*:(.*)$", re.M | re.I)
NOT_FIX_LINE = re.compile(r"^\s*(?:[-*]\s*)?`?ne\s+corrige\s+pas\s*:\s*([^\s:|`]+)\s*(?::(.*))?$", re.M | re.I)
GRAVITY = re.compile(r"^gravit[ée]\s*:\s*(bloquant|grave|moyen|mineur)$", re.I)
NO_REPLAY = re.compile(r"^rejeu\s+impossible\s*(?::(.*))?$", re.I)
REPLAY = re.compile(r"^rejeu\s*:\s*(\S+)$", re.I)
SERIOUS = ("bloquant", "grave")
VERDICTS = "docs/audits/ultra/verdicts.tsv"
FIX_SHAPE = "« corrige: <id> | gravité: bloquant|grave|moyen|mineur | rejeu: <id du rejeu> » (or « … | rejeu impossible : <raison> »)"


def real_reason(text):
    """A reason is a few words (three at least), not « non » or nothing."""
    return len(re.findall(r"\w{2,}", text or "")) >= 3


def table(text):
    """The rows of a tab-separated table with its header line (« # » lines are comments): [{column: cell}]."""
    header, rows = None, []
    for line in (text or "").split("\n"):
        if not line.strip() or line.startswith("#"):
            continue
        cells = [c.strip() for c in line.split("\t")]
        if header is None:
            header = cells
        else:
            rows.append(dict(zip(header, cells + [""] * (len(header) - len(cells)))))
    return rows


def triage_graves(branch, head, tour, base=None):
    """{n°: gravity} of the rows of the round's triage that are « réel » and bloquant or grave, and the table's path.
    Review of « portail3 » (G2): read in the branch, at its base and at the integration's head; a row is grave when ANY
    of them says so (a branch that rewrites the triage lowers nothing)."""
    path = f"docs/audits/ultra/sieges/{tour}/verdicts.tsv"
    out = {}
    for rev in (branch, base, head):
        for row in table(show(rev, path)) if rev else []:
            gravity = next((v.lower() for k, v in row.items() if k.lower().startswith("gravit")), "")
            if row.get("n°") and row.get("verdict", "").lower() == "réel" and gravity in SERIOUS:
                out.setdefault(row["n°"], gravity)
    return out, path


def without_code_blocks(text):
    """Review of « portail3 » (M1): a fenced block (``` … ``` or ~~~ … ~~~) shows a format, it states nothing."""
    out, inside = [], None
    for line in text.split("\n"):
        fence = line.strip()[:3]
        if fence in ("```", "~~~") and inside in (None, fence):
            inside = None if inside else fence
            continue
        if not inside:
            out.append(line)
    return "\n".join(out)


def named(defect, text):
    """The id as a whole word (« T4 » is not named by « T4-26 » or « T41 »)."""
    return re.search(r"(?<![A-Za-z0-9_-])" + re.escape(defect) + r"(?![A-Za-z0-9_]|-[A-Za-z0-9])", text) is not None


def corrections(branch, path, base, tour, added=(), head=None):
    """The refusals of « a blocking or grave fix adds its replay »."""
    text = without_code_blocks(show(branch, path) or "")
    refusals, fixed, cited = [], {}, set()
    for m in NOT_FIX_LINE.finditer(text):
        cited.add(m.group(1))
        if not real_reason(m.group(2)):
            refusals.append(f"« ne corrige pas: {m.group(1)} » without a real reason (a few words: « ne corrige pas: <id> : <raison> »)")
    known, triage = triage_graves(branch, head, tour, base) if tour else ({}, "")
    sources = {d: triage for d in known}
    now = {r.get("item"): r for r in table(show(branch, VERDICTS))}
    # Review of « portail3 » (G2): the gravity of a row is the strongest of the branch, its base and the integration's head.
    grave_rows = {}
    for rev in (branch, base, head):
        for row in table(show(rev, VERDICTS)) if rev else []:
            if row.get("item") and row.get("gravite", "").lower() in SERIOUS:
                grave_rows.setdefault(row["item"], row["gravite"].lower())
    for item, gravity in grave_rows.items():
        known.setdefault(item, gravity)
        sources.setdefault(item, VERDICTS)
    for m in FIX_LINE.finditer(text):
        parts = [p.strip() for p in m.group(1).strip().strip("`").split("|")]
        gravity = GRAVITY.match(parts[1]) if len(parts) in (2, 3) else None
        rest = parts[2] if len(parts) == 3 else ""
        if not gravity or not re.fullmatch(r"\S+", parts[0]) or (rest and not (NO_REPLAY.match(rest) or REPLAY.match(rest))):
            refusals.append(f"« {m.group(0).strip()[:80]} » is not {FIX_SHAPE}")
            continue
        defect, said = parts[0], gravity.group(1).lower()
        fixed[defect] = said
        if defect in known and said not in SERIOUS:
            refusals.append(f"corrige « {defect} »: says {said}, {sources[defect]} says {known[defect]}")
            continue
        if said not in SERIOUS:
            continue
        none, replay = NO_REPLAY.match(rest), REPLAY.match(rest)
        if none:
            if not real_reason(none.group(1)):
                refusals.append(f"corrige « {defect} » ({said}): « rejeu impossible » without a real reason (a few words: why no "
                                "replay can show this defect)")
        elif not replay:
            refusals.append(f"corrige « {defect} » ({said}): no replay named (« rejeu: <id> » of a replay added to rejeux/ with "
                            "the fix, or « rejeu impossible : <raison> »)")
        elif replay.group(1).strip("`") not in added:
            refusals.append(f"corrige « {defect} » ({said}): replay {replay.group(1).strip('`')} is not a folder the branch adds "
                            "to docs/audits/ultra/rejeux/ (the replay comes with the fix, often from rejeux-attente/; or "
                            "« rejeu impossible : <raison> »)")
    # A grave row of the bench's verdicts the branch turns « corrigé » (the word, whatever follows it) or takes away.
    if base:
        before = {r.get("item"): r for r in table(show(base, VERDICTS))}
        for item, gravity in sorted(grave_rows.items()):
            was = before.get(item, {}).get("etat", "").strip().lower().startswith("corrigé") if item in before else None
            row = now.get(item)
            if item in fixed or was:
                continue
            if row is None and item in before:
                refusals.append(f"{item} ({gravity}) taken out of {VERDICTS} without its line "
                                f"« corrige: {item} | gravité: {gravity} | rejeu: <id> » in {path}")
            elif row is not None and row.get("etat", "").strip().lower().startswith("corrigé"):
                refusals.append(f"{item} ({gravity}) turned « corrigé » in {VERDICTS} without its line "
                                f"« corrige: {item} | gravité: {gravity} | rejeu: <id> » in {path}")
    # A grave of the round's triage the report or a commit of the branch names.
    prose = "\n".join(l for l in text.split("\n") if not FIX_LINE.match(l) and not NOT_FIX_LINE.match(l))
    subjects = []
    if base:
        r = subprocess.run(["git", "log", "--no-merges", "--format=%s", f"{base}..{branch}"], capture_output=True, text=True)
        subjects = r.stdout.splitlines() if r.returncode == 0 else []
    for defect in sorted(d for d in known if sources[d] == triage and d not in fixed and d not in cited):
        where = "the zone report" if named(defect, prose) else next((f"the commit « {s[:70]} »" for s in subjects if named(defect, s)), None)
        if where:
            refusals.append(f"{defect} ({known[defect]} in {triage}) is named in {where} without its line "
                            f"« corrige: {defect} | gravité: {known[defect]} | rejeu: <id> » or « ne corrige pas: {defect} : <raison> » in {path}")
    return refusals


ZONE = re.compile(r"(?:^|/)src/main/(?:.*/)?(?:tutos?/.+|i18n/[^/]+/tuto[^/]*\.json)$")
# (AUDIT-ULTRA §13.3: the four files the tutorial shares with the rest of the application.)
ZONE_IF_SAID = re.compile(r"(?:^|/)src/main/(?:.*/)?(?:PianoRoll|RhythmScreen|PlainWords|TrackSheet)\.kt$")
SAID = re.compile(r"\btuto", re.I)
# (A list bullet, a number, a check box, a quote, a table row, bold, backquotes: the line is read all the same.)
SEAT_LINE = re.compile(r"^[ \t>#|*_`+\-0-9.)\[\]xX]*contr[oô]le[*_`]*\s*:(.*)$", re.M | re.I)
SEAT_ID = re.compile(r"^[A-Za-z0-9][A-Za-z0-9_.-]*(?:/[A-Za-z0-9][A-Za-z0-9_.-]*)?$")
SEAT_CODE = re.compile(r"^code\s+test[ée]\s*:\s*([0-9a-f]{7,40})$", re.I)
SEAT_BLOCKED = re.compile(r"^bloqu[ée]s\s*:\s*(\d+)$", re.I)
SEAT_GRAVES = re.compile(r"^graves\s+neufs\s*:\s*(\d+)$", re.I)
SEAT_SHAPE = "« controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0 »"
SEAT_HEAD = re.compile(r"^code testé\s*:\s*(\S*)\s*$")
# (« chapitre: 13 Die Effekte | ecran: 193 | bloqué | … » and « - chapitre 5 « ベース » : bloqué (…) », both of tour 6.)
CHAPTER = re.compile(r"^\s*(?:[-*]\s*)?(chapitre\b[^\n]*?)\s*[|:]\s*(bloqu[ée][^|\n]*)", re.M | re.I)


def git_out(*args):
    r = subprocess.run(["git", *args], capture_output=True, text=True)
    return r.returncode, r.stdout


def names(*args):
    """The file names a git command lists, read with « -z » (git quotes a name that is not plain ASCII otherwise)."""
    code, out = git_out(*args)
    return code, [f for f in out.split("\0") if f]


def tutorial_files(branch, text, base, head):
    """The files of the tutorial zone the lot changes: changed since the base, and not the integration head's version.
    Review of « portail4 » (G1): once the lot is a lot of the tutorial, the four shared files of §13.3 it changes are
    tutorial code too, whatever its report says."""
    code, changed = names("diff", "-z", "--no-renames", "--name-only", base, branch)
    said = SAID.search(text or "") is not None
    files, shared = [], []
    for f in changed if code == 0 else []:
        if not (ZONE.search(f) or ZONE_IF_SAID.search(f)):
            continue
        if head and subprocess.run(["git", "diff", "--quiet", head, branch, "--", f]).returncode == 0:
            continue
        (files if ZONE.search(f) or said else shared).append(f)
    return files + shared if files else []


def seat_head(text):
    """« code testé » of a seat report's head, as sortie-de-siege.py reads it: before « siege: », eight lines at most."""
    for line in text.splitlines()[:8]:
        if line.startswith("siege:"):
            break
        m = SEAT_HEAD.match(line)
        if m:
            return m.group(1)
    return None


def tutorial(branch, path, base, head, tour):
    """(refusals, what to say when accepted) of « a lot of the tutorial comes with its control seat »."""
    raw = show(branch, path)
    text = without_code_blocks(raw or "")
    files = tutorial_files(branch, text, base, head)
    if not files:
        return [], ""
    zone = f"{len(files)} file(s) of the tutorial zone ({', '.join(files[:4])}{', …' if len(files) > 4 else ''})"
    if raw is None:
        return [f"no zone report {path} in {branch}, and the lot changes {zone}: its control seat's line "
                f"{SEAT_SHAPE} is missing"], ""
    lines = [m for m in SEAT_LINE.finditer(text)]
    if not lines:
        return [f"no « controle: » line in {path}, and the lot changes {zone}: one line per control seat that replayed "
                f"the tutorial on this branch, {SEAT_SHAPE}"], ""
    # The lot's own commits that change the zone's files (the integration's, merged in, are not the lot's).
    own = []
    if head:
        code, out = git_out("rev-list", "--no-merges", "--full-history", f"{head}..{branch}", "--", *files)
        own = out.split() if code == 0 else []
    refusals, seats = [], []
    for m in lines:
        parts = [p.strip(" \t*_`") for p in m.group(1).strip(" \t*_`|").split("|")]
        ok = len(parts) == 4 and SEAT_ID.match(parts[0]) and ".." not in parts[0]
        sha, blocked, graves = (SEAT_CODE.match(parts[1]), SEAT_BLOCKED.match(parts[2]), SEAT_GRAVES.match(parts[3])) if ok else (None,) * 3
        if not (ok and sha and blocked and graves):
            refusals.append(f"« {m.group(0).strip()[:90]} » is not {SEAT_SHAPE}")
            continue
        seat, sha = parts[0], sha.group(1).lower()
        if int(blocked.group(1)) or int(graves.group(1)):
            refusals.append(f"control seat « {seat} »: bloqués: {int(blocked.group(1))}, graves neufs: {int(graves.group(1))} "
                            "(a lot of the tutorial enters with 0 chapter blocked and 0 new grave)")
            continue
        code, full = git_out("rev-parse", "-q", "--verify", sha + "^{commit}")
        full = full.strip()
        if code != 0 or not full:
            refusals.append(f"control seat « {seat} »: code testé {sha} is not a commit here")
            continue
        if subprocess.run(["git", "merge-base", "--is-ancestor", full, branch]).returncode != 0:
            refusals.append(f"control seat « {seat} »: code testé {sha[:10]} is not an ancestor of {branch} "
                            "(the seat replays the lot's branch)")
            continue
        late = [c for c in own if subprocess.run(["git", "merge-base", "--is-ancestor", c, full]).returncode != 0]
        if late:
            refusals.append(f"control seat « {seat} »: code testé {sha[:10]} was played before the lot's last change of the "
                            f"tutorial ({', '.join(c[:8] for c in late[:3])}): the seat replays after the last fix")
            continue
        code, moved = names("diff", "-z", "--no-renames", "--name-only", full, branch, "--", *files)
        if code != 0 or moved:
            refusals.append(f"control seat « {seat} »: {', '.join(moved[:4])} at the head of {branch} are not the files "
                            f"the seat played at {sha[:10]} (a merge changed them since): the seat replays the head")
            continue
        seat_path = f"docs/audits/ultra/sieges/{tour}/controle/{seat}/rapport.md"
        seat_text = show(branch, seat_path)
        if seat_text is None and "/" not in seat:
            # Lot « constate »: the bench plays a control seat in controle/<id>/<a-f>/ (scripts/human-tester.sh
            # controle/<id>); « <id> » names that folder when it holds one seat's report, never a guess among several.
            nested = [p for p in (f"docs/audits/ultra/sieges/{tour}/controle/{seat}/{s}/rapport.md" for s in "abcdef")
                      if show(branch, p) is not None]
            if len(nested) > 1:
                refusals.append(f"control seat « {seat} »: {len(nested)} seats in its folder "
                                f"({', '.join(p.split('/controle/')[1][:-len('/rapport.md')] for p in nested)}): name one, « {seat}/a »")
                continue
            if nested:
                seat_path, seat_text = nested[0], show(branch, nested[0])
        if seat_text is None:
            refusals.append(f"control seat « {seat} »: no report {seat_path} in {branch} (the seat's folder comes with the lot)")
            continue
        said = seat_head(seat_text)
        if not said:
            refusals.append(f"control seat « {seat} »: no « code testé : <sha> » on top of {seat_path} (the bench writes it at "
                            "the seat's start: scripts/bench/sortie-de-siege.py)")
            continue
        if not re.fullmatch(r"[0-9a-f]{7,64}", said) or not full.startswith(said):
            refusals.append(f"control seat « {seat} »: {seat_path} names another commit (« code testé : {said[:12]} ») than the "
                            f"line of {path} ({sha[:10]})")
            continue
        stuck = CHAPTER.search(seat_text)
        if stuck:
            refusals.append(f"control seat « {seat} »: its own report says « {stuck.group(1).strip()[:50]} … "
                            f"{stuck.group(2).strip()[:40]} » while the line says bloqués: 0")
            continue
        seats.append(f"{seat}@{full[:8]}")
    return refusals, f"tuto: {zone}; control seat(s): {', '.join(seats)}"


def main(argv):
    if len(argv) >= 4 and argv[1] == "--tuto":
        opts = dict(a.split(":", 1) for a in argv[4:] if ":" in a)
        if not opts.get("base") or not opts.get("tete") or not opts.get("tour"):
            print("rapport-zone.py --tuto: base:<commit>, tete:<commit> and tour:<tour-N> are needed")
            return 2
        refusals, said = tutorial(argv[2], argv[3], opts["base"], opts["tete"], opts["tour"])
        for r in refusals:
            print(r)
        if not refusals and said:
            print(said)
        return 1 if refusals else 0
    if len(argv) >= 4 and argv[1] == "--corrections":
        opts = dict(a.split(":", 1) for a in argv[4:] if ":" in a and not a.startswith("ajout:"))
        refusals = corrections(argv[2], argv[3], opts.get("base"), opts.get("tour"),
                               [a.split(":", 1)[1] for a in argv[4:] if a.startswith("ajout:")], opts.get("tete"))
        for r in refusals:
            print(r)
        return 1 if refusals else 0
    if len(argv) < 3:
        print(__doc__)
        return 2
    refusals, filters = check(argv[1], argv[2], [a.split(":", 1)[1] for a in argv[3:] if a.startswith("ajout:")])
    for r in refusals:
        print(r)
    if refusals:
        return 1
    for f in filters:
        print("filtre: " + f)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
