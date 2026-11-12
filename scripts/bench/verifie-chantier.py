#!/usr/bin/env python3
"""Checks the « banc fiable » work by program, not by eye (PLAN-RESTE 4.0, audit « banc crédible » A.10).

  python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume.txt [--root DIR] [--sans-git]

(a) verdicts.tsv, four criteria:
    1. an « appli » line has a proof: a test without the bench that exists (never a Bench* test), or a dated ❌
       trial on a device: an annotation « RECETTE tel-NN ❌ <device> … <date> » of its telephone or note cell that is
       one of the owner's trials of retours-benjamin.md (without that file: any « tel-NN … ❌ … date » segment, and
       the judge says the notes are absent). Every « tel-NN ✅/❌ » written on a line must be one of those trials;
    2. a « banc » line has a Bench* guard AND a proof without the bench (a test that exists outside Bench*):
       the bench judging itself is not a proof;
    3. the summary of the latest self-test is mandatory (« résumé absent » without it) and a guard is green
       only when the summary names it « vert : Class#method »; the summary is read only when it is sealed for
       the code judged (audit « banc crédible » 2, P3): it names « code testé : <arbre> », the stamp beside it
       (selftest.ok) seals it (« resume: <sha256> », « code: <arbre> »), that tree is the code here
       (tampon.sh code) and its lot's junit/ gives it (tampon.sh resultats), or it is a portal trace committed by
       « Portal traces of the merge … » (review of the « portail » lot, G1). Else « résumé refusé » and no guard is
       green;
    4. a « corrigé » line is in base.tsv with its round and its commit cell names a commit that exists.
    Also: every proof names a test that exists: a Kotlin « Class#method », or an engine host test
    « engine/host-tests/<file>#<function> » defined in that file and registered in a PS_*_TESTS list (A.12);
    every @KnownDefect(id) in the tests has a line « appli » citing its test, one of them with the id in its
    « garde_id » column; a line's garde_id is carried by one of its proofs (a @KnownDefect, or an engine test
    registered as « known_defect_<id> », which the C++ runner prints as « KNOWN DEFECT still there »).
    --preuves ITEM prints the proofs read on that line and stops.
    Audit « banc crédible » 2, D1: a lot of seats from round 6 on keeps its self-test's summary, stamp and judge
    (« lot sans trace du portail »); with git, a « Merge team … » made under the traces rule has its traces under
    docs/audits/ultra/portail/ (« fusion sans trace du portail »).
(b) git log: a « vert: » commit closes the oldest open « rouge: » commit OF THE SAME TEST (same subject after
    the prefix, or a test the red names or adds that the green's message names, or the red's topic: a camelCase word
    of its subject before « ( » that is a class or the start of a function of a test file the red adds or changes,
    named in the green's subject before « ( »; a green closes at most one red per topic). A green of another test
    closes nothing. A green (not a merge) that takes @KnownDefect off a test (kept, still @Test, never @Ignore) whose
    « Class#method » its message names, and that touches code outside the tests (or says « sans code : <raison> »),
    is a pair on its own, when that guard was put there by a « rouge: » commit naming the guarded test, or in an
    ancestor of the discipline's first red (8f36fa3): that guard, failing with the defect's message in the parent,
    was the red. The guard's origin is followed through renamed classes, reformatted annotations and moved files.
    Commits are read from 8f36fa3 on by ancestry, never by date. A green whose red was an existing test seen failing in
    a run is a pair by its evidence kept under docs/audits/ultra/rouges-constates/<green sha>/ (rouge.txt and the
    JUnit XML: [red_seen]). A checked line of banc-fiable-journal.md (« - [x] 4.2 … », ids 4.x to 7.x, R.x, A.x) carries
    « preuve: rouge <sha> / vert <sha> » naming such a pair, or « preuve: script <path> » (a figure computed
    by a script that exists).
(c) recette.json <-> verdicts: every « recette tel-NN » cited by an open undecided line exists in the
    checklist, and every checklist item is cited by some line.
Prints one line per gap, then « chantier : N écarts ». Exit 1 when N > 0. Standard library only.
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
from functools import lru_cache

TEST_ROOT = os.path.join("app", "src", "test", "java")
PROOF = re.compile(r"([A-Z][A-Za-z0-9_]*)[#.]([a-z][A-Za-z0-9_]*)")
ENGINE_PROOF = re.compile(r"(engine/host-tests/[A-Za-z0-9_./-]+)#([A-Za-z_][A-Za-z0-9_]*)")
# A function's name: an identifier, or (lot « juge6 ») the whole name of a Kotlin test in backticks,
# « fun `vert - a swallowed touch is touched again`() » being that one test, never its first word.
NAME = r"`?((?<=`)[^`\n]+(?=`)|[A-Za-z0-9_]+)"
# Positional or named id (« @KnownDefect("X", …) », « @KnownDefect(id = "X", …) »: review of « banc-fin », case E).
KNOWN_DEFECT = re.compile(r'@KnownDefect\(\s*(?:id\s*=\s*)?"([^"]+)"[^)]*\)\s*(?:@[A-Za-z]+(?:\([^)]*\))?\s*)*fun\s+' + NAME)


def read_tsv(path):
    rows, header = [], None
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            cells = line.split("\t")
            if header is None:
                header = cells
                continue
            rows.append(dict(zip(header, cells + [""] * (len(header) - len(cells)))))
    return rows


def test_sources(root):
    """{ClassName: its own source text}: a file's text cut at each top-level class declaration, so a method is
    attributed to the class it belongs to (unit tests and instrumented tests)."""
    out = {}
    for sub in (TEST_ROOT, os.path.join("app", "src", "androidTest", "java")):
        for d, _, files in os.walk(os.path.join(root, sub)):
            for name in files:
                if not name.endswith(".kt"):
                    continue
                text = open(os.path.join(d, name), encoding="utf-8").read()
                decl = list(re.finditer(r"^(?:internal |private |open |abstract |data )*class\s+([A-Za-z0-9_]+)", text, re.M))
                for i, m in enumerate(decl):
                    end = decl[i + 1].start() if i + 1 < len(decl) else len(text)
                    out[m.group(1)] = out.get(m.group(1), "") + text[m.start():end]
    return out


DECL = re.compile(r"\bclass\s+([A-Za-z0-9_]+)(?:\s*<[^>{]*>)?\s*(?:constructor\s*)?")
PARENT = re.compile(r"\s*:\s*([A-Za-z0-9_.]+)\s*\(")


def _after_parens(text, i):
    """The index after the balanced « ( … ) » that starts at text[i] (nested calls in default values), or i."""
    if i >= len(text) or text[i] != "(":
        return i
    depth = 0
    for j in range(i, len(text)):
        depth += {"(": 1, ")": -1}.get(text[j], 0)
        if depth == 0:
            return j + 1
    return i


def seat_class(sources, cls, depth=0):
    """Lot « verdicts »: the class inherits HumanTester (itself or through a base class): a seat of the bench, whatever
    its name. Read from the class's declaration, « class Name(…) : Parent(… », its constructor's parentheses balanced
    (review of « verdicts »: default values calling functions hid the parent)."""
    if depth > 6:
        return False
    text = sources.get(cls, "")
    d = next((d for d in DECL.finditer(text) if d.group(1) == cls), None)
    m = d and PARENT.match(text, _after_parens(text, d.end()))
    if not m:
        return False
    parent = m.group(1).rsplit(".", 1)[-1]
    return parent == "HumanTester" or seat_class(sources, parent, depth + 1)


def method_exists(sources, cls, method):
    """A function of that name, its whole name (review of « juge6 », G1: « vert - x » is not « vert - x y »)."""
    text = sources.get(cls)
    return text is not None and any(m.group(1) == method for m in FUN.finditer(text))


def without_comments(text, keep_lines=False):
    """The Kotlin text with its comments blanked (strings kept): « // fun x » is no function. keep_lines: a block
    comment leaves its line breaks, each line of the text stays at its number."""
    out, i, n = [], 0, len(text)
    while i < n:
        if text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
            continue
        if text.startswith("/*", i):
            j = text.find("*/", i + 2)
            k = n if j < 0 else j + 2
            out.append("\n" * text.count("\n", i, k) if keep_lines else " ")
            i = k
            continue
        if text.startswith('"""', i):
            j = text.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append(text[i:j])
            i = j
            continue
        if text[i] in "\"'":
            j = i + 1
            while j < n and text[j] != text[i] and text[j] != "\n":
                j += 2 if text[j] == "\\" else 1
            out.append(text[i:j + 1])
            i = j + 1
            continue
        out.append(text[i])
        i += 1
    return "".join(out)


def test_runs(sources, cls, method):
    """Audit « banc crédible » 2, P5: the proof is a test that runs: a function of the class (comments aside) with
    @Test and without @Ignore/@Disabled ([still_runs])."""
    text = sources.get(cls)
    return text is not None and still_runs(without_comments(text), method)


def proofs(cell):
    """Class#method references in a cell (« - » or free text without one gives none)."""
    return [(c, m) for c, m in PROOF.findall(ENGINE_PROOF.sub(" ", cell or "")) if c[0].isupper()]


def engine_proofs(cell):
    """« engine/host-tests/<file>#<function> » references in a cell."""
    return ENGINE_PROOF.findall(cell or "")


def engine_test(root, path, fn):
    """(defined, registered name or None) of an engine host test: « static void fn( » in the file, and
    « {"name", ns::fn} » in one of its PS_*_TESTS lists (what main() runs)."""
    if ".." in path.split("/"):
        return False, None
    try:
        text = open(os.path.join(root, path), encoding="utf-8").read()
    except OSError:
        return False, None
    defined = re.search(r"\bvoid\s+" + re.escape(fn) + r"\s*\(", text) is not None
    listed = "".join(m.group(0) for m in re.finditer(r"^#define\s+PS_[A-Z0-9_]+_TESTS\b(?:.*\\\n)*.*$", text, re.M))
    m = re.search(r'\{\s*"([A-Za-z0-9_]+)"\s*,\s*(?:[A-Za-z0-9_]+::)*' + re.escape(fn) + r"\s*\}", listed)
    return defined, (m.group(1) if m else None)


def engine_guard_name(ident):
    return "known_defect_" + re.sub(r"[^a-z0-9]+", "_", ident.lower())


def green_in_resume(resume_text, cls, method):
    """Green only when the summary names it « vert : Class#method » (selftest-verdict.py): a guard that is red,
    quarantined, skipped or that did not run at all is not green. No summary: not judged here (see « résumé absent »)."""
    if resume_text is None:
        return True
    return re.search(r"^vert : " + re.escape(cls) + "#" + re.escape(method) + r"\s*$", resume_text, re.M) is not None


HERE = os.path.dirname(os.path.abspath(__file__))


def code_here(root):
    """The tree id of root's working files but docs/ (tampon.sh code), or None."""
    try:
        r = subprocess.run(["bash", os.path.join(HERE, "tampon.sh"), "code", root], capture_output=True, text=True, timeout=300)
    except (subprocess.SubprocessError, OSError):
        return None
    out = r.stdout.strip()
    return out if r.returncode == 0 and re.fullmatch(r"[0-9a-f]{40,64}", out) else None


def unsealed(root, resume_path):
    """Why the summary may not be read for the code of root (audit « banc crédible » 2, P3), or None when it may."""
    with open(resume_path, "rb") as f:
        raw = f.read()
    named = re.findall(r"^code testé : (\S+)\s*$", raw.decode("utf-8", "replace"), re.M)
    if not named:
        return "il ne nomme pas le code testé (« code testé : <arbre> », écrit par selftest.sh)"
    lot = os.path.dirname(os.path.abspath(resume_path))
    stamp_path = os.path.join(lot, "selftest.ok")
    if not os.path.isfile(stamp_path):
        return "pas de tampon à côté (selftest.ok)"
    stamp = open(stamp_path, encoding="utf-8", errors="replace").read()
    if not re.search(r"^resume: " + hashlib.sha256(raw).hexdigest() + r"$", stamp, re.M):
        return "le tampon ne scelle pas ce résumé (changé après le selftest ?)"
    if not re.search(r"^code: " + re.escape(named[-1]) + r"$", stamp, re.M):
        return "le tampon ne scelle pas le code qu'il nomme"
    here = code_here(root)
    if here is None:
        return "le code d'ici n'a pas pu être calculé (tampon.sh code)"
    if here != named[-1]:
        return f"résumé d'un autre code (testé : {named[-1][:12]}, ici : {here[:12]})"
    # Review of the « portail » lot (G1): « resume: » and « code: » anyone can compute; the stamp is worth something only
    # with the JUnit results it seals (replayed by tampon.sh, the classes expected read from root's sources), or as a
    # portal trace committed by fusion-equipes.sh.
    if os.path.isdir(os.path.join(lot, "junit")):
        r = subprocess.run(["bash", os.path.join(HERE, "tampon.sh"), "resultats", lot, os.path.abspath(root),
                            os.path.abspath(resume_path)],
                           capture_output=True, text=True)
        if r.returncode != 0:
            return "ses résultats JUnit ne donnent pas ce résumé vert, ou le tampon ne les scelle pas"
    elif not committed_trace(root, resume_path):
        return ("ni résultats JUnit à côté (junit/), ni trace du portail commitée par « Portal traces of the merge … » : "
                "un tampon sans eux s'écrit à la main")
    return None


def committed_trace(root, resume_path):
    """The summary and its stamp are a portal trace: under docs/audits/ultra/portail/, as committed, the last commit
    that touched each of them being a « Portal traces of the merge … » (fusion-equipes.sh; a team branch never touches that folder)."""
    rel = os.path.relpath(os.path.realpath(resume_path), os.path.realpath(root)).replace(os.sep, "/")
    if not rel.startswith(PORTAL_TRACES.replace(os.sep, "/") + "/"):
        return False
    for f in (rel, os.path.dirname(rel) + "/selftest.ok"):
        if subprocess.run(["git", "-C", root, "ls-files", "--error-unmatch", f], capture_output=True).returncode != 0:
            return False
        if subprocess.run(["git", "-C", root, "diff", "--quiet", "HEAD", "--", f], capture_output=True).returncode != 0:
            return False
        try:
            last = _git(root, "log", "-1", "--format=%s", "--", f).strip()
        except (subprocess.SubprocessError, OSError):
            return False
        if not last.startswith("Portal traces of the merge "):
            return False
    return True


def dated_failed_trial(v, trials=None):
    """A dated ❌ trial on a device. With the owner's notes ([trials], see [owner_trials]): a ❌ annotation of the line
    that is one of their trials, whole ([tied_marks]). Without them: one « ; » segment of the telephone or note cell with
    tel-NN, ❌ and a date (nothing to tie it to: the judge says the notes are absent)."""
    if trials is not None:
        return "❌" in tied_marks(v, trials)
    for cell in (v.get("telephone", ""), v.get("note", "")):
        for part in cell.split(";"):
            if re.search(r"\btel-\d+", part) and "❌" in part and re.search(r"\b20\d\d-\d\d-\d\d\b", part):
                return True
    return False


TRIAL_BULLET = re.compile(r"^- (20\d\d-\d\d-\d\d)\b(.*)$")
TRIAL_ALONE = re.compile(r"recette (tel-\d+) = (✅|❌)")
TRIAL_IN_LIST = re.compile(r"^\s+- (tel-\d+) (✅|❌)")
TRIAL_KEPT = re.compile(r"^(.*?)\b(tel-\d+)\b.*\bRésultat retenu : (✅|❌)")


def _device(text):
    m = re.search(r"téléphone|tablette", text)
    return m.group(0) if m else None


def owner_trials(root):
    """The owner's trials of the checklist written in retours-benjamin.md, as (tel-NN, mark, device, date): a
    « tel-NN ✅/❌ » sub-bullet of a dated bullet « <device>, recette n/m », a dated bullet « recette tel-NN = ✅/❌ »,
    or a dated bullet « <device>, tel-NN … Résultat retenu : ✅/❌ »."""
    path = notes_path(root)
    if not os.path.isfile(path):
        return []
    trials, date, device = [], None, None
    for line in open(path, encoding="utf-8").read().splitlines():
        m = TRIAL_BULLET.match(line)
        if m:
            date, device = None, None
            alone = TRIAL_ALONE.search(m.group(2))
            if alone:
                dev = _device(m.group(2)[:alone.start()])
                if dev:
                    trials.append((alone.group(1), alone.group(2), dev, m.group(1)))
            else:
                head = re.search(r"^(.*?)\brecette \d+/\d+", m.group(2))
                kept = TRIAL_KEPT.search(m.group(2))
                if head and _device(head.group(1)):
                    date, device = m.group(1), _device(head.group(1))
                elif kept and _device(kept.group(1)):
                    trials.append((kept.group(2), kept.group(3), _device(kept.group(1)), m.group(1)))
            continue
        m = TRIAL_IN_LIST.match(line)
        if m and date:
            trials.append((m.group(1), m.group(2), device, date))
        elif not line.startswith(" "):
            date, device = None, None
    return trials


def records_trial(v, tel, mark, device, date):
    """« RECETTE tel-NN <mark> <device> … <date> » in one « ; » segment of the telephone or note cell."""
    wanted = re.compile(r"RECETTE\s+" + tel + r"\s+" + mark + r"\s+" + device + r"\b[^;]*\b" + date + r"\b")
    return any(wanted.search(part) for cell in (v.get("telephone", ""), v.get("note", "")) for part in cell.split(";"))


def trial_gaps(root, verdicts):
    gaps = []
    for tel, mark, device, date in owner_trials(root):
        missing = "essai de retours-benjamin.md absent de verdicts.tsv"
        citing = [v for v in verdicts if re.search(r"\b" + tel + r"\b", v.get("telephone", ""))]
        if not citing:
            gaps.append(f"{tel} {mark} {device} {date} : {missing} (aucune ligne ne cite {tel})")
        gaps += [f"{v.get('item', '?')} : {missing} (attendu « RECETTE {tel} {mark} {device} {date} »)"
                 for v in citing if not records_trial(v, tel, mark, device, date)]
    return gaps


CLAIM = re.compile(r"(?:\brecette\s+)?\b(tel-\d+)\b[\s:=]*(✅|❌)", re.I)


def trial_claims(v):
    """Every « tel-NN ✅/❌ » written in a line's telephone or note cell, whatever is around it (the word RECETTE or not,
    upper or lower case, first of its « ; » segment or behind another): (number, mark, its text up to the next one or
    the end of the segment, the same text from the word « recette » when it carries one)."""
    out = []
    for cell in (v.get("telephone", ""), v.get("note", "")):
        for part in cell.split(";"):
            found = list(CLAIM.finditer(part))
            for i, m in enumerate(found):
                end = found[i + 1].start() if i + 1 < len(found) else len(part)
                out.append((m.group(1).lower(), m.group(2), part[m.start(1):end], part[m.start():end].strip()))
    return out


def is_owner_trial(claim, trials):
    """The claim is one of the owner's trials, whole: « RECETTE tel-NN <mark> <device> … <date> » ([records_trial])."""
    tel, mark, _, text = claim
    return any(records_trial({"note": text}, *t) for t in trials if t[0] == tel and t[1] == mark)


def tied_marks(v, trials):
    """The marks (✅, ❌) of the line's annotations that are trials of the owner's notes."""
    return {c[1] for c in trial_claims(v) if is_owner_trial(c, trials)}


def notes_path(root):
    return os.path.join(root, "docs", "audits", "ultra", "retours-benjamin.md")


def annotation_gaps(root, verdicts):
    """The reverse of [trial_gaps]: each « tel-NN ✅/❌ » of a line's telephone or note cell ([trial_claims]) is one of the
    owner's trials, whole (the word RECETTE, number, mark, device, date). Without the owner's notes nothing can be
    compared: that is one gap, as long as the verdicts carry such annotations."""
    claims = [(v, c) for v in verdicts for c in trial_claims(v)]
    if not os.path.isfile(notes_path(root)):
        return [f"retours-benjamin.md absent : les annotations d'essai de verdicts.tsv ne peuvent pas être vérifiées "
                f"({len(claims)} annotation(s) d'essai, aucune rattachée à un essai du propriétaire)"] if claims else []
    trials = owner_trials(root)
    return [f"{v.get('item', '?')} : annotation « RECETTE » sans essai correspondant dans retours-benjamin.md (« {c[3]} »)"
            for v, c in claims if not is_owner_trial(c, trials)]


PAD_KEY = re.compile(r"padlive|(?<![a-z])pad_")


def dated_trial(v, trials=None):
    """A dated trial on a device, whatever its mark: with the owner's notes, an annotation of the line that is one of
    their trials ([tied_marks]); without them, one « ; » segment with tel-NN, ✅ or ❌, and a date."""
    if trials is not None:
        return bool(tied_marks(v, trials))
    for cell in (v.get("telephone", ""), v.get("note", "")):
        for part in cell.split(";"):
            if re.search(r"\btel-\d+", part) and re.search("✅|❌", part) and re.search(r"\b20\d\d-\d\d-\d\d\b", part):
                return True
    return False


def commit_exists(root, sha):
    try:
        return subprocess.run(["git", "-C", root, "cat-file", "-e", sha + "^{commit}"], capture_output=True, timeout=60).returncode == 0
    except (subprocess.SubprocessError, OSError):
        return False


TRACES = ("selftest-resume.txt", "selftest.ok", "selftest-chantier.txt")
PORTAL_TRACES = os.path.join("docs", "audits", "ultra", "portail")
# The first round whose lots keep their portal traces (audit « banc crédible » 2, D1: rounds 1 to 5 were run before).
TRACED_ROUND = 6


def lot_trace_gaps(root):
    """D1: a lot of seats (docs/audits/ultra/sieges/tour-N/lot*/, N ≥ TRACED_ROUND) keeps its self-test's summary, stamp
    and judge beside its seats."""
    gaps, seats = [], os.path.join(root, "docs", "audits", "ultra", "sieges")
    for tour in sorted(os.listdir(seats)) if os.path.isdir(seats) else []:
        m = re.fullmatch(r"tour-(\d+)", tour)
        if not m or int(m.group(1)) < TRACED_ROUND:
            continue
        for lot in sorted(os.listdir(os.path.join(seats, tour))):
            where = os.path.join(seats, tour, lot)
            if lot.startswith("lot") and os.path.isdir(where):
                missing = [t for t in TRACES if not os.path.isfile(os.path.join(where, t))]
                if missing:
                    gaps.append(f"docs/audits/ultra/sieges/{tour}/{lot} : lot sans trace du portail ({', '.join(missing)})")
    return gaps


def merge_trace_gaps(root):
    """D1: every « Merge team … » made by a fusion-equipes.sh that writes the portal's traces (its first parent's script
    names docs/audits/ultra/portail) has them: a fusion.txt naming it under docs/audits/ultra/portail/, with the
    self-test's summary, stamp and judge beside it."""
    traced = set()
    base = os.path.join(root, PORTAL_TRACES)
    for d in sorted(os.listdir(base)) if os.path.isdir(base) else []:
        fusion = os.path.join(base, d, "fusion.txt")
        if os.path.isfile(fusion) and all(os.path.isfile(os.path.join(base, d, t)) for t in TRACES):
            traced |= set(re.findall(r"^fusion: ([0-9a-f]{40})\s*$", open(fusion, encoding="utf-8").read(), re.M))
    try:
        merges = _git(root, "log", "--merges", "--format=%H%x1f%h%x1f%s").splitlines()
    except (subprocess.SubprocessError, OSError) as e:
        return [f"git log impossible : {e}"]
    gaps = []
    for line in merges:
        full, short, subject = line.split("\x1f", 2)
        if not subject.startswith("Merge team ") or full in traced:
            continue
        script = _show(root, full + "^1", "scripts/bench/fusion-equipes.sh") or ""
        if PORTAL_TRACES.replace(os.sep, "/") in script:
            gaps.append(f"{short} « {subject[:70]} » : fusion sans trace du portail")
    return gaps


def check(root, resume_path=None, with_git=True):
    gaps = []
    ultra = os.path.join(root, "docs", "audits", "ultra")
    verdicts = read_tsv(os.path.join(ultra, "verdicts.tsv"))
    sources = test_sources(root)
    resume = None
    if not resume_path:
        gaps.append("résumé absent : --resume <selftest-resume.txt> est obligatoire (le dernier selftest.sh l'écrit dans son lot)")
    elif not os.path.isfile(resume_path):
        gaps.append(f"résumé absent : {resume_path} introuvable")
    else:
        why = unsealed(root, resume_path)
        if why:
            gaps.append(f"résumé refusé : {why} ({resume_path}) : aucune garde n'y est verte")
            resume = ""
        else:
            resume = open(resume_path, encoding="utf-8").read()
    base = {r.get("cle"): r for r in read_tsv(os.path.join(ultra, "base.tsv"))}
    # The owner's trials, when his notes are there: only an annotation tied to one of them is a proof.
    trials = owner_trials(root) if os.path.isfile(notes_path(root)) else None

    for v in verdicts:
        item = v.get("item", "?")
        verdict, etat = v.get("verdict", ""), v.get("etat", "")
        without_bench, carried = 0, set()
        gid = (v.get("garde_id") or "").strip().strip("-")
        for cls, m in proofs(v.get("preuve_appli")):
            if cls.startswith("Bench"):
                gaps.append(f"{item} : preuve d'appli {cls}#{m} est un test du banc")
            elif seat_class(sources, cls):
                gaps.append(f"{item} : preuve d'appli {cls}#{m} est un test du banc (hérite de HumanTester)")
            elif not method_exists(sources, cls, m):
                gaps.append(f"{item} : preuve d'appli {cls}#{m} introuvable")
            elif not test_runs(sources, cls, m):
                gaps.append(f"{item} : preuve d'appli {cls}#{m} n'est pas un test qui tourne (@Test absent, @Ignore, ou en commentaire)")
            else:
                without_bench += 1
                carried |= {kid for kid, method in KNOWN_DEFECT.findall(sources.get(cls, "")) if method == m}
        for path, fn in engine_proofs(v.get("preuve_appli")):
            defined, name = engine_test(root, path, fn)
            if not defined:
                gaps.append(f"{item} : preuve d'appli {path}#{fn} introuvable")
            elif name is None:
                gaps.append(f"{item} : preuve d'appli {path}#{fn} non enregistrée dans une liste PS_*_TESTS")
            else:
                without_bench += 1
                carried |= {g for g in (gid,) if engine_guard_name(g) == name}
        if gid and gid not in carried:
            gaps.append(f"{item} : garde_id {gid} porté par aucune de ses preuves (@KnownDefect(\"{gid}\") ou test moteur {engine_guard_name(gid)})")
        guards = proofs(v.get("garde_banc"))
        for cls, m in guards:
            if not method_exists(sources, cls, m):
                gaps.append(f"{item} : garde {cls}#{m} introuvable")
            elif not green_in_resume(resume, cls, m):
                gaps.append(f"{item} : garde {cls}#{m} pas verte dans le dernier selftest")
        if verdict == "appli" and not without_bench and not dated_failed_trial(v, trials):
            gaps.append(f"{item} : verdict « appli » sans preuve (ni test sans banc existant, ni essai ❌ daté)")
        if verdict == "appli" and etat == "ouvert" and without_bench and not carried and not dated_failed_trial(v, trials):
            gaps.append(f"{item} : verdict « appli » dont la preuve n'est ni @KnownDefect ni un essai ❌ daté "
                        f"(un test vert ne prouve pas un défaut ouvert)")
        if verdict in ("appli", "banc") and PAD_KEY.search(v.get("cle", "")) and not without_bench and not dated_trial(v, trials):
            gaps.append(f"{item} : verdict pad-timing sans preuve hors banc (les pads du banc sont une copie de ClipLauncher (A.16), "
                        f"pas le moteur : il faut un test hors Bench*, un test moteur ou un essai daté sur un appareil)")
        if verdict == "banc":
            if not any(c.startswith("Bench") for c, _ in guards):
                gaps.append(f"{item} : verdict « banc » sans garde Bench*")
            if not without_bench:
                gaps.append(f"{item} : verdict « banc » sans preuve sans banc (aucun test hors Bench* existant dans preuve_appli)")
        if etat == "corrigé":
            row = base.get(v.get("cle"))
            if row is None:
                gaps.append(f"{item} : « corrigé » absent de base.tsv ({v.get('cle')})")
            elif not (row.get("corrige_au") or "").strip() and row.get("etat") != "corrigé":
                gaps.append(f"{item} : « corrigé » sans tour dans base.tsv")
            shas = re.findall(r"\b[0-9a-f]{7,40}\b", v.get("commit", ""))
            if not shas:
                gaps.append(f"{item} : « corrigé » sans commit")
            elif with_git:
                gaps += [f"{item} : « corrigé », commit {c} introuvable" for c in shas if not commit_exists(root, c)]

    appli = [v for v in verdicts if v.get("verdict") == "appli"]
    for cls, text in sorted(sources.items()):
        for kid, method in KNOWN_DEFECT.findall(text):
            citing = [v for v in appli if f"{cls}#{method}" in v.get("preuve_appli", "")]
            if not citing:
                gaps.append(f"@KnownDefect {kid} ({cls}#{method}) sans ligne « appli » qui le cite")
            elif not any((v.get("garde_id") or "").strip() == kid for v in citing):
                gaps.append(f"@KnownDefect {kid} ({cls}#{method}) : aucune ligne qui le cite ne porte garde_id {kid} "
                            f"({', '.join(v.get('item', '?') for v in citing)})")

    gaps += lot_trace_gaps(root)
    gaps += trial_gaps(root, verdicts)
    gaps += annotation_gaps(root, verdicts)

    try:
        checklist = json.load(open(os.path.join(root, "app", "src", "main", "assets", "recette.json"), encoding="utf-8"))
        ids = {i["id"] for i in checklist.get("items", [])}
    except (OSError, ValueError):
        ids = None
        gaps.append("recette.json illisible")
    if ids is not None:
        refs = {}
        for v in verdicts:
            for tel in re.findall(r"tel-\d+", v.get("telephone", "")):
                refs.setdefault(tel, []).append(v)
        for tel, rows in sorted(refs.items()):
            open_rows = [r for r in rows if r.get("verdict") == "non-tranché"]
            if open_rows and tel not in ids:
                gaps.append(f"{tel} cité par {', '.join(r['item'] for r in open_rows)} (non tranché) mais absent de recette.json")
        for tel in sorted(ids - set(refs)):
            gaps.append(f"{tel} de recette.json cité par aucune ligne de verdicts.tsv")
    return gaps


TEST_TOKEN = re.compile(r"[A-Za-z0-9_./]+(?:#|::)([A-Za-z0-9_]+)")
ADDED_TEST = re.compile(r"^\+\s*(?:fun|def|static void)\s+`?((?<=`)[^`\n]{12,}(?=`)|[A-Za-z0-9_]{12,})", re.M)
CHECKED = re.compile(r"^- \[x\] ((?:[4-7]\.\d+|[RA]\.\d+))\b(.*)$", re.M)
PAIR = re.compile(r"preuve\s*:\s*rouge\s+([0-9a-f]{7,40})\s*/\s*vert\s+([0-9a-f]{7,40})")
SCRIPT = re.compile(r"preuve\s*:\s*script\s+`?([A-Za-z0-9_./-]+)")


def _git(root, *args):
    return subprocess.run(["git", "-C", root, *args], capture_output=True, text=True, timeout=120, check=True).stdout


def _rest(subject):
    return subject.partition(":")[2].strip().lower()


def red_names(root, sha, message):
    """The tests a « rouge: » commit is about: the « Class#method » and « file::case » it names, and the test
    functions it adds (public ones, 12 letters or more: a helper called « tap » would match any sentence)."""
    names = {m.group(0) for m in TEST_TOKEN.finditer(message)} | {m.group(1) for m in TEST_TOKEN.finditer(message) if len(m.group(1)) >= 12}
    try:
        names |= set(ADDED_TEST.findall(_git(root, "show", sha, "--format=", "-U0")))
    except (subprocess.SubprocessError, OSError):
        pass
    return names


# Lot « banc6b »: a commit's topic, « rouge: SimpleNoDecibelTest (…) », « vert: SimpleNoDecibelTest and oneWordPerThing
# (…) »: the camelCase words (12 letters or more) of its subject before the first parenthesis.
# Review of « banc6b » (grave): a word of a « Class#method », « Class.method » or « file::case » is one test, never a
# topic (the names pair it): « vert: FooTest#other » closed « rouge: FooTest#one » by its class.
TOPIC = re.compile(r"(?<![A-Za-z0-9_#.:])([A-Za-z_][A-Za-z0-9_]*[a-z][A-Z][A-Za-z0-9_]*)(?![A-Za-z0-9_#:]|\.\w)")
TEST_FILE = re.compile(r"(?:^|/)src/(?:test|androidTest)/|(?:^|/)test_[^/]*\.py$|(?:^|/)host-tests/")


def topics(subject):
    head = subject.partition(":")[2].split("(", 1)[0]
    return {t for t in TOPIC.findall(head) if len(t) >= 12}


def red_topics(root, sha, subject):
    """The topics of a red that are in a test file it adds or changes (a class, or the start of a function): a topic
    found in no test file is only a word."""
    wanted = topics(subject)
    if not wanted:
        return set()
    try:
        files = [f for f in _git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", "--root", sha).split() if TEST_FILE.search(f)]
    except (subprocess.SubprocessError, OSError):
        return set()
    found = set()
    for f in files:
        text = _show(root, sha, f) or ""
        found |= {t for t in wanted if re.search(r"\b(?:class|object|fun|def)\s+`?" + re.escape(t), text)}
    return found


def same_test(red, green):
    """red, green: (sha, subject, message, names). Same subject after the prefix, or the green names a test of the red."""
    if _rest(red[1]) == _rest(green[1]):
        return True
    return any(re.search(r"(?<![A-Za-z0-9_])" + re.escape(n) + r"(?![A-Za-z0-9_])", green[2]) for n in red[3])


def _commit(root, sha):
    """(short sha, subject, message, names) of a commit, or None."""
    try:
        out = _git(root, "show", "-s", "--format=%h%x1f%s%x1f%B", sha + "^{commit}")
    except (subprocess.SubprocessError, OSError):
        return None
    h, subject, message = out.split("\x1f", 2)
    return (h, subject, message, red_names(root, h, message) if subject.lower().startswith("rouge:") else set())


# The rouge/vert discipline began with the first « rouge: » commit (8f36fa3, 2026-11-08 18:29 UTC): a guard added in
# one of its ancestors could not come from a red, it counts as there before (Tour5AppProofTest's, 4168889). An ancestry,
# not a date: GIT_COMMITTER_DATE sets a date at will (review of « banc-fin », case C). --debut-discipline in a toy.
DISCIPLINE_START = "8f36fa3"
# inner, sealed, enum classes and named objects too: a guard in « inner class Hidden » is Hidden's (review of
# « banc-fin », m2 / case F2).
CLASS = re.compile(r"^\s*(?:(?:private|internal|public|abstract|open|data|inner|sealed|enum|final|annotation|value)\s+)*"
                   r"(?:class|object)\s+([A-Za-z0-9_]+)", re.M)
TEST_DIRS = ("app/src/test/java", "app/src/androidTest/java")
FUN = re.compile(r"\bfun\s+" + NAME)


def _open_braces(text):
    """For each position, the position of the innermost « { » still open there (or -1): strings, characters and
    comments skipped. Returned as a sorted list of (position, innermost open brace) changes."""
    stack, changes, i, n = [], [(0, -1)], 0, len(text)
    while i < n:
        c = text[i]
        if text.startswith("//", i):
            i = text.find("\n", i)
            i = n if i < 0 else i
            continue
        if text.startswith("/*", i):
            i = text.find("*/", i + 2)
            i = n if i < 0 else i + 2
            continue
        if text.startswith('"""', i):
            i = text.find('"""', i + 3)
            i = n if i < 0 else i + 3
            continue
        if c in "\"'":
            j = i + 1
            while j < n and text[j] != c and text[j] != "\n":
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if c == "{":
            stack.append(i)
            changes.append((i + 1, i))
        elif c == "}" and stack:
            stack.pop()
            changes.append((i, stack[-1] if stack else -1))
        i += 1
    return changes


def class_methods(text, marked):
    """« Class#method » of the functions of a Kotlin file (marked: only those carrying @KnownDefect): the class is
    the one whose body is the innermost « { » open at the function (a nested helper class does not take its place)."""
    starts = [(m.start(), m.group(1)) for m in CLASS.finditer(text)]
    changes = _open_braces(text)

    def owner(pos):
        brace = next((b for at, b in reversed(changes) if at <= pos), -1)
        return next((name for start, name in reversed(starts) if start < brace), "") if brace >= 0 else ""
    if marked:
        return {f"{owner(m.start(2))}#{m.group(2)}" for m in KNOWN_DEFECT.finditer(text)}
    return {f"{owner(m.start(1))}#{m.group(1)}" for m in FUN.finditer(text)}


def _show(root, rev, path):
    try:
        return _git(root, "show", f"{rev}:{path}")
    except (subprocess.SubprocessError, OSError):
        return None


def guarded(root, rev, method):
    """The « Class#method » of rev's test files (any file) that carry @KnownDefect on a function named method."""
    try:
        listed = _git(root, "grep", "-l", "-F", "KnownDefect", rev, "--", *TEST_DIRS).splitlines()
    except (subprocess.SubprocessError, OSError):
        return set()
    found = set()
    for line in listed:
        text = _show(root, rev, line.split(":", 1)[1]) or ""
        found |= {t for t in class_methods(text, True) if t.endswith("#" + method)}
    return found


@lru_cache(maxsize=None)
def _guard_commits(root):
    """The commits (not merges, newest first) whose diff adds or takes off a « KnownDefect » line in the tests."""
    try:
        return tuple(_git(root, "log", "--no-merges", "--format=%H", "-G", "KnownDefect", "--", *TEST_DIRS).split())
    except (subprocess.SubprocessError, OSError):
        return ()


def _ancestor(root, a, b):
    return subprocess.run(["git", "-C", root, "merge-base", "--is-ancestor", a, b], capture_output=True).returncode == 0


def guard_origin(root, sha, test):
    """(commit, « Class#method ») that put the guard on test (« Class#method ») lifted by sha: the newest commit before
    sha where a test file guards it and no test file of its parent does. Followed by class AND method (review of the
    « portail » lot, M3: the same method name in another class is another test): an annotation reformatted or a file
    moved keeps the same « Class#method » in the parent, so that commit is no origin; a class renamed is followed to
    its old name (the parent guards that method under a class this commit no longer guards it under)."""
    target, method = test, test.partition("#")[2]
    for h in _guard_commits(root):
        if h.startswith(sha) or not _ancestor(root, h, sha + "^"):
            continue
        now = guarded(root, h, method)
        if target not in now:
            continue
        before = guarded(root, h + "^", method)
        if target in before:
            continue
        renamed = sorted(before - now)
        if renamed:
            target = renamed[0]
            continue
        return h, target
    return None


def guard_from_red(root, sha, test):
    """True when the @KnownDefect on test (« Class#method ») that commit sha lifts came from a « rouge: » commit that
    names the guarded test, or from an ancestor of the discipline's start ([guard_origin]). Any other origin (a
    « chore: », the red of another test, a guard never seen added) is no red evidence (reviews of « pads-live » and
    « banc-fin »)."""
    found = guard_origin(root, sha, test)
    if found is None:
        return False
    origin, there = found
    if _ancestor(root, origin, DISCIPLINE_START):
        return True
    red = _commit(root, origin)
    return red is not None and red[1].lower().startswith("rouge:") and names(red[2], there)


# A function with its annotations and modifiers, its name whole ([NAME]).
ANNOTATED_FUN = re.compile(r"((?:@[A-Za-z.]+(?:\([^)]*\))?\s*)*)(?:(?:public|internal|suspend|open|override)\s+)*"
                           r"(?<![A-Za-z0-9_])fun\s+" + NAME)


def still_runs(text, method):
    """The function keeps @Test and no @Ignore/@Disabled (review of « banc-fin », case F), the function of exactly
    that name (review of « juge6 », G1: not a longer-named sibling, « <name> twice », whatever comes first)."""
    m = next((m for m in ANNOTATED_FUN.finditer(text) if m.group(2) == method), None)
    return m is not None and re.search(r"@(?:org\.junit\.)?Test\b", m.group(1)) is not None \
        and re.search(r"@(?:org\.junit\.)?(?:Ignore|Disabled)\b", m.group(1)) is None


def known_defects_lifted(root, sha):
    """The tests (« Class#method ») a commit takes @KnownDefect off, the test kept (review of the « partage » lot, (c)):
    their guard was the red (KnownDefectRule: the test failed with the defect's message until this commit), provided
    the guard came from a red ([guard_from_red]). A test deleted is not one; a merge lifts nothing (review of the
    « pads-live » lot, moyen: the lift must be a commit of its own). Review of « banc-fin » (B, F): the test still runs
    (@Test, no @Ignore), and the commit touches code outside the tests and docs/, or says why not on a line
    « sans code : <raison> »."""
    try:
        parents = _git(root, "rev-list", "--parents", "-n", "1", sha).split()[1:]
        if len(parents) != 1:
            return set()
        files = _git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", sha + "^", sha).split()
        message = _git(root, "show", "-s", "--format=%B", sha)
    except (subprocess.SubprocessError, OSError):
        return set()
    code = [f for f in files if not f.startswith(TEST_DIRS) and not f.startswith("docs/")]
    if not code and not re.search(r"^sans code\s*:\s*\S", message, re.M | re.I):
        return set()
    lifted = set()
    for f in files:
        if not (f.startswith(TEST_ROOT.replace(os.sep, "/") + "/") and f.endswith(".kt")):
            continue
        before, after = _show(root, sha + "^", f), _show(root, sha, f)
        if before is None or after is None:
            continue
        gone = (class_methods(before, True) - class_methods(after, True)) & class_methods(after, False)
        lifted |= {t for t in gone if still_runs(after, t.partition("#")[2]) and guard_from_red(root, sha, t)}
    return lifted


RED_SEEN = os.path.join("docs", "audits", "ultra", "rouges-constates")


def test_in(root, rev, cls, method):
    """« Class#method » is a test of a test file of rev (the class that owns it, [class_methods]) that runs: @Test, no
    @Ignore/@Disabled ([still_runs]; third review of the « portail » lot, M9: a helper never fails in a run).
    Lot « juge7 »: or a test of the bench's own scripts ([py_test_in])."""
    try:
        listed = _git(root, "grep", "-l", "-F", method, rev, "--", *TEST_DIRS).splitlines()
    except (subprocess.SubprocessError, OSError):
        listed = []  # git grep exits 1 when nothing matches
    for l in listed:
        text = without_comments(_show(root, rev, l.split(":", 1)[1]) or "")
        if f"{cls}#{method}" in class_methods(text, False) and still_runs(text, method):
            return True
    try:
        scripts = _git(root, "ls-tree", "-r", "--name-only", rev, "--", "scripts/bench").splitlines()
    except (subprocess.SubprocessError, OSError):
        return False
    return any(PY_TEST_FILE.match(f) and py_test_in(_show(root, rev, f) or "", cls, method) for f in scripts)


# Lot « juge7 »: the folder of the bench's code that lives under the tests (HumanTester and its helpers).
BENCH_HELPERS = "app/src/test/java/com/pocketstudio/app/human/"
# Lot « juge7 »: the bench's own tests, unittest classes of scripts/bench/test_*.py (run with python3 -I).
PY_TEST_FILE = re.compile(r"^scripts/bench/test_[^/]*\.py$")


def py_test_in(text, cls, method):
    """« def method » is a test written right in « class cls(…) » of a bench script's tests: its name starts with
    « test » (unittest runs nothing else), no skip decorator above it. A def of another class, a nested def or a
    module-level def is not it."""
    in_cls = False
    lines = text.split("\n")
    for i, line in enumerate(lines):
        if re.match(r"class\s+" + re.escape(cls) + r"\s*[(:]", line):
            in_cls = True
            continue
        if line.strip() and not line[0].isspace() and not line.lstrip().startswith("#"):
            in_cls = False
        m = re.match(r"    def\s+([A-Za-z0-9_]+)\s*\(", line) if in_cls else None
        if m and m.group(1) == method:
            if not method.startswith("test"):
                return False
            j = i - 1
            while j >= 0 and lines[j].startswith("    @"):
                if re.match(r"    @(?:unittest\.)?(?:skip|expectedFailure)", lines[j]):
                    return False
                j -= 1
            return True
    return False


def red_seen(root, sha, message):
    """Review of the « portail » lot: a test that existed went red in a run, and this green fixes the app. Evidence in
    docs/audits/ultra/rouges-constates/<the green's full sha>/: rouge.txt (« parent: <the green's parent> »,
    « test: Classe#méthode ») and a JUnit XML of that run where that test fails; the green names the test and does not
    touch the test's file (nor any file of the tests). True only when all of it holds."""
    import xml.etree.ElementTree as ET
    try:
        full = _git(root, "rev-parse", sha + "^{commit}").strip()
        parents = _git(root, "rev-list", "--parents", "-n", "1", full).split()[1:]
        files = _git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", full + "^", full).split()
    except (subprocess.SubprocessError, OSError):
        return False
    folder = os.path.join(root, RED_SEEN, full)
    try:
        facts = open(os.path.join(folder, "rouge.txt"), encoding="utf-8").read()
    except OSError:
        return False
    parent = re.search(r"^parent:\s*([0-9a-f]{40})\s*$", facts, re.M)
    test = re.search(r"^test:\s*(\S.*?)\s*$", facts, re.M)
    if len(parents) != 1 or not parent or parent.group(1) != parents[0] or not test or not names(message, test.group(1)):
        return False
    # Lot « juge7 »: a bench script's tests are tests too ([py_test_in]).
    if any(f.startswith(TEST_DIRS) or PY_TEST_FILE.match(f) for f in files):
        return False
    cls, _, method = test.group(1).partition("#")
    # Second review of the « portail » lot (G2): the test exists, in the green's parent and in the green.
    for rev in (parents[0], full):
        if not test_in(root, rev, cls, method.split("[")[0]):
            return False
    for name in os.listdir(folder):
        if not name.endswith(".xml"):
            continue
        try:
            cases = ET.parse(os.path.join(folder, name)).getroot().iter("testcase")
        except (ET.ParseError, OSError):
            continue
        for case in cases:
            if case.get("classname", "").rsplit(".", 1)[-1] == cls and case.get("name") == method \
                    and (case.find("failure") is not None or case.find("error") is not None):
                return True
    return False


REPLAYED = os.path.join("docs", "audits", "ultra", "paires-rejouees")
# Review of the « verdicts » lot (grave): the replays' XML is the team's own word (the portal's fresh review never
# replays it, unlike rouges-constates/), so the rule closes only greens of the history written before it: ancestors of
# this commit (the lot's base). A green made later pairs by its words, or by red evidence the review replays.
# Lot « juge6 »: moved to that lot's base (db66a3ce, fx-int after the merges of testapk, oreille and mixwake): those
# lots wrote some reds and greens in other words (no test named in a green), history that is fixed now; their
# pairs are replayed (paires-rejouees/) or their greens' reds made by a mutation (rouges-mutes/) by the lot « juge6 »,
# which wrote none of that code. The first end was 92258b19 (lot « verdicts »).
# Lot « juge7 »: moved to that lot's base (1585bb46, fx-int after the lot rejeux6, whose pair d56efddf / 37e2c8ed and
# greens a95d9667, 2e2c5f3d are proven by replay or mutation; the first ends were 92258b19, then db66a3ce).
REPLAYS_UNTIL = "1585bb4672edd5e9f824f890966c1659398d79fc"


def _case_result(path, cls, method):
    """« fails », « passes » or None: the testcase Class#method of a JUnit XML ([cls] its simple name)."""
    import xml.etree.ElementTree as ET
    try:
        cases = ET.parse(path).getroot().iter("testcase")
    except (ET.ParseError, OSError):
        return None
    for case in cases:
        if case.get("classname", "").rsplit(".", 1)[-1] == cls and case.get("name") == method:
            if case.find("failure") is not None or case.find("error") is not None:
                return "fails"
            return None if case.find("skipped") is not None else "passes"
    return None


def replayed_red(root, sha, open_reds):
    """Lot « verdicts »: the open red a green closes by a replay, or None. Evidence in docs/audits/ultra/paires-rejouees/
    <the green's full sha>/: rejeu.txt (« rouge: <the red's full sha> », « test: Classe#méthode »), rouge.xml (the test
    run on the red: it fails) and vert.xml (run on the green: it passes). The red is still open, an ancestor of the
    green, and adds or names the test ([red_names]); the test runs in both ([test_in]). History is never rewritten: a
    red and its green written in other words (2026-11-08/09) pair by what they did, replayed."""
    try:
        full = _git(root, "rev-parse", sha + "^{commit}").strip()
    except (subprocess.SubprocessError, OSError):
        return None
    if not commit_exists(root, REPLAYS_UNTIL) or not _ancestor(root, full, REPLAYS_UNTIL):
        return None
    folder = os.path.join(root, REPLAYED, full)
    try:
        facts = open(os.path.join(folder, "rejeu.txt"), encoding="utf-8").read()
    except OSError:
        return None
    red = re.search(r"^rouge:\s*([0-9a-f]{40})\s*$", facts, re.M)
    test = re.search(r"^test:\s*" + TEST_NAME + r"\s*$", facts, re.M)
    if not red or not test:
        return None
    cls, method = test.group(1), test.group(2)
    # Lot « juge6 »: « test au vert: Classe#nom », the test under the name the green gave it, by a rename alone.
    renamed = re.search(r"^test au vert:\s*" + TEST_NAME + r"\s*$", facts, re.M)
    green_method = method
    if renamed:
        if renamed.group(1) != cls or not renamed_only(root, full, cls, method, renamed.group(2)):
            return None
        green_method = renamed.group(2)
    match = None
    for r in open_reds:
        try:
            if _git(root, "rev-parse", r[0] + "^{commit}").strip() == red.group(1):
                match = r
        except (subprocess.SubprocessError, OSError):
            continue
    if match is None or not _ancestor(root, red.group(1), full):
        return None
    if f"{cls}#{method}" not in match[3] and method not in match[3]:
        return None
    if not (test_in(root, red.group(1), cls, method) and test_in(root, full, cls, green_method)):
        return None
    if _case_result(os.path.join(folder, "rouge.xml"), cls, method) != "fails":
        return None
    if _case_result(os.path.join(folder, "vert.xml"), cls, green_method) != "passes":
        return None
    return match


# « Classe#méthode » in an evidence file: the method an identifier or a test's whole name in backticks (lot « juge6 »).
TEST_NAME = r"([A-Za-z0-9_]+)#([^#\s`](?:[^#\n`]*[^#\s`])?)"


def renamed_only(root, sha, cls, old, new):
    """Lot « juge6 »: commit sha renames the test cls#old to cls#new and changes nothing else of its file: a test file
    of its diff, old put back in place of new, is that file in its parent (the green's « rouge - » to « vert - »)."""
    if old == new:
        return False
    try:
        files = _git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", sha + "^", sha).split("\n")
    except (subprocess.SubprocessError, OSError):
        return False
    for f in files:
        if not f.startswith(TEST_DIRS):
            continue
        before, after = _show(root, sha + "^", f), _show(root, sha, f)
        if before is None or after is None or f"{cls}#{new}" not in class_methods(after, False):
            continue
        back = FUN.sub(lambda m: m.group(0)[:m.start(1) - m.start()] + old if m.group(1) == new else m.group(0), after)
        if back == before:
            return True
    return False


MUTATED = os.path.join("docs", "audits", "ultra", "rouges-mutes")
HUNK = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")


def _hunk_lines(diff):
    """The lines of a unified diff's hunks, as (file, sign, line number in the old file, in the new file, text): a
    line is read by its hunk's counts, never by its first characters alone (« --- » may be a line taken off)."""
    current, old, new, left_old, left_new = None, 0, 0, 0, 0
    for line in diff.splitlines():
        if left_old > 0 or left_new > 0:
            sign, text = line[:1], line[1:]
            if sign == "\\":
                continue
            yield current, sign, old, new, text
            if sign in (" ", ""):
                old, new, left_old, left_new = old + 1, new + 1, left_old - 1, left_new - 1
            elif sign == "-":
                old, left_old = old + 1, left_old - 1
            elif sign == "+":
                new, left_new = new + 1, left_new - 1
            else:
                left_old = left_new = 0
            continue
        h = HUNK.match(line)
        if h:
            old, new = int(h.group(1)), int(h.group(3))
            left_old = 1 if h.group(2) is None else int(h.group(2))
            left_new = 1 if h.group(4) is None else int(h.group(4))
        elif line.startswith("+++ "):
            current = line[4:].strip()
            current = current[2:] if current.startswith("b/") else current


def _added_at(root, sha, path):
    """The line numbers, in commit sha's path, of the lines sha added to it."""
    try:
        diff = _git(root, "show", "--format=", "-U0", sha, "--", path)
    except (subprocess.SubprocessError, OSError):
        return set()
    return {new for _, sign, _, new, _ in _hunk_lines(diff) if sign == "+"}


def _patch_applies(root, sha, patch):
    """The patch applies to commit sha's tree (checked in a scratch index, nothing written)."""
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        env = dict(os.environ, GIT_INDEX_FILE=os.path.join(tmp, "index"))
        for args in (["read-tree", sha], ["apply", "--cached", "--check", patch]):
            if subprocess.run(["git", "-C", root, *args], env=env, capture_output=True, timeout=120).returncode != 0:
                return False
    return True


def mutated_red(root, sha):
    """Lot « juge6 » (the testapk lot's greens without a red): a green that brought its test with its fix has the red
    it lacks when its own fix, partly taken back, makes that test fail (the fiche's « retire la ligne corrigée, le test
    doit rougir »). Evidence in docs/audits/ultra/rouges-mutes/<the green's full sha>/: rejeu.txt (« test:
    Classe#méthode », a test the green adds: in it, not in its parent), mutation.patch (applies to the green; touches
    only code files the green changed, never a test; it only takes lines off, each a line of code, neither blank nor a
    comment, that the green added at that very place: review of « juge6 », M1), rouge.xml (the test run on the green with the patch: it fails) and
    vert.xml (on the green: it passes). Only greens of the history before [REPLAYS_UNTIL], as the replays: the XML is
    the team's word, the patch is there for the review to replay."""
    try:
        full = _git(root, "rev-parse", sha + "^{commit}").strip()
        parents = _git(root, "rev-list", "--parents", "-n", "1", full).split()[1:]
        files = _git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", full + "^", full).split("\n")
    except (subprocess.SubprocessError, OSError):
        return False
    if len(parents) != 1 or not commit_exists(root, REPLAYS_UNTIL) or not _ancestor(root, full, REPLAYS_UNTIL):
        return False
    folder = os.path.join(root, MUTATED, full)
    try:
        facts = open(os.path.join(folder, "rejeu.txt"), encoding="utf-8").read()
        patch_text = open(os.path.join(folder, "mutation.patch"), encoding="utf-8").read()
    except OSError:
        return False
    test = re.search(r"^test:\s*" + TEST_NAME + r"\s*$", facts, re.M)
    if not test:
        return False
    cls, method = test.group(1), test.group(2)
    if not test_in(root, full, cls, method) or test_in(root, parents[0], cls, method):
        return False
    code = {f for f in files if f and not TEST_FILE.search(f) and not f.startswith("docs/")}
    # Lot « juge7 »: the bench's code that lives under the tests (SeatOptions.kt, BenchWindows.kt): a Kotlin file of the
    # bench's folder with no test in the green (no @Test, nor @org.junit.Test: review of « juge7 », G1) is a helper,
    # never run as a test; a line taken off it is a line of code. Only the bench's folder (review M1): an app test's
    # helper stays a test file.
    code |= {f for f in files if f.startswith(BENCH_HELPERS) and f.endswith(".kt")
             and not re.search(r"@(?:org\.junit\.)?Test\b", without_comments(_show(root, full, f) or "@Test"))}
    # Review of « juge6 », M1: the mutation only takes lines off (nothing put in their place: no require(false),
    # no « !! » on null, no throw), each a line of code (not blank, not a comment) the green added at that very
    # place: matched by its number in the green's file, never by its text alone.
    removed, texts, added_at = 0, {}, {}
    for path, sign, old, _, text in _hunk_lines(patch_text):
        if sign == "+" or path not in code:
            return False
        if sign != "-":
            continue
        if path not in texts:
            whole = _show(root, full, path)
            if whole is None:
                return False
            texts[path] = (whole.split("\n"), without_comments(whole, keep_lines=True).split("\n"))
            added_at[path] = _added_at(root, full, path)
        lines, bare = texts[path]
        if old not in added_at[path] or old > len(lines) or lines[old - 1] != text or not bare[old - 1].strip():
            return False
        removed += 1
    if not removed:
        return False
    if not _patch_applies(root, full, os.path.join(folder, "mutation.patch")):
        return False
    return (_case_result(os.path.join(folder, "rouge.xml"), cls, method) == "fails"
            and _case_result(os.path.join(folder, "vert.xml"), cls, method) == "passes")


def names(message, test):
    """The message names « Class#method » (or « Class.method »), the class too: a method name alone matches any
    class that has one (review of the « pads-live » lot, moyen)."""
    cls, _, method = test.partition("#")
    return re.search(r"(?<![A-Za-z0-9_])" + re.escape(cls) + r"[#.]" + re.escape(method) + r"(?![A-Za-z0-9_])", message) is not None


def red_green_pairs(root):
    """(b) Every bench repair is a « rouge: » commit (the guard, red) followed by a « vert: » commit (the fix) of the
    same test. A red whose test gets no green is a gap; so is a green that closes no red of its test, unless it takes
    @KnownDefect off a test whose class and method it names, a guard that came from a red (that guard was the red:
    [known_defects_lifted])."""
    # The commits from the discipline's start on, by ancestry and parents first: a date (--since) let one antidated
    # commit stop the walk and hide every green older than it (review of « banc-fin », case C, found by this lot's toy).
    span = ["HEAD"]
    if commit_exists(root, DISCIPLINE_START + "^"):
        span = [f"{DISCIPLINE_START}^..HEAD"]
    try:
        out = _git(root, "log", "--topo-order", "--reverse", "--format=%x1e%h%x1f%s%x1f%B", *span)
    except (subprocess.SubprocessError, OSError) as e:
        return [f"git log impossible : {e}"]
    gaps, open_reds = [], []
    for record in out.split("\x1e")[1:]:
        sha, subject, message = record.split("\x1f", 2)
        low = subject.lower()
        if low.startswith("rouge:"):
            open_reds.append((sha, subject, message, red_names(root, sha, message), red_topics(root, sha, subject)))
        elif low.startswith("vert:"):
            green = (sha, subject, message, set())
            red = next((r for r in open_reds if same_test(r, green)), None)
            closed = [red] if red is not None else []
            # Lot « banc6b »: one red per topic the green names (« vert: A and B (…) » closes A's and B's oldest).
            for t in sorted(topics(subject) - {t for r in closed for t in r[4]}):
                more = next((r for r in open_reds if t in r[4] and r not in closed), None)
                if more is not None:
                    closed.append(more)
            if not closed:
                replayed = replayed_red(root, sha, open_reds)
                if replayed is not None:
                    closed.append(replayed)
            for r in closed:
                open_reds.remove(r)
            if not closed and not any(names(message, t) for t in known_defects_lifted(root, sha)) and not red_seen(root, sha, message) \
                    and not mutated_red(root, sha):
                gaps.append(f"{sha} « {subject[:70]} » : vert sans rouge du même test avant lui")
    gaps += [f"{r[0]} « {r[1][:70]} » : rouge sans vert du même test après lui" for r in open_reds]
    return gaps


def checked_lines(root):
    """(b) A checked line of the journal names its proof: a rouge/vert pair of the same test, or a script."""
    path = os.path.join(root, "docs", "audits", "ultra", "banc-fiable-journal.md")
    if not os.path.isfile(path):
        return []
    gaps = []
    for m in CHECKED.finditer(open(path, encoding="utf-8").read()):
        ident, text = m.group(1), m.group(2)
        where = f"journal, ligne cochée {ident}"
        pairs, scripts = PAIR.findall(text), SCRIPT.findall(text)
        if not pairs and not scripts:
            gaps.append(f"{where} : sans preuve (ni « preuve: rouge <sha> / vert <sha> », ni « preuve: script <chemin> »)")
        for s in scripts:
            if not os.path.isfile(os.path.join(root, s)):
                gaps.append(f"{where} : script {s} introuvable")
        for r, g in pairs:
            red, green = _commit(root, r), _commit(root, g)
            if red is None or green is None:
                gaps.append(f"{where} : paire rouge {r} / vert {g} : commit introuvable")
            elif not red[1].lower().startswith("rouge:") or not green[1].lower().startswith("vert:"):
                gaps.append(f"{where} : paire rouge {r} / vert {g} : le premier n'est pas un commit « rouge: » ou le second pas un « vert: »")
            elif subprocess.run(["git", "-C", root, "merge-base", "--is-ancestor", r, g], capture_output=True).returncode != 0:
                gaps.append(f"{where} : paire rouge {r} / vert {g} : le rouge ne précède pas le vert")
            elif not same_test(red, green):
                gaps.append(f"{where} : paire rouge {r} / vert {g} : pas le même test")
    return gaps


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    ap.add_argument("--resume", default=None, help="selftest-resume.txt of the latest selftest")
    ap.add_argument("--sans-git", action="store_true", help="skip what reads git: the rouge/vert pairs, the journal's checked lines, the commits' existence")
    ap.add_argument("--preuves", metavar="ITEM", help="print the proofs read on that line of verdicts.tsv, and stop")
    ap.add_argument("--debut-discipline", metavar="SHA", default=DISCIPLINE_START,
                    help="the first « rouge: » commit (a guard added in one of its ancestors was there before); a toy's own")
    ap.add_argument("--fin-rejeux", metavar="SHA", default=REPLAYS_UNTIL,
                    help="the last commit whose greens a replay (paires-rejouees/) may close; a toy's own")
    a = ap.parse_args()
    globals()["DISCIPLINE_START"] = a.debut_discipline
    globals()["REPLAYS_UNTIL"] = a.fin_rejeux
    if a.preuves:
        sources = test_sources(a.root)
        for v in read_tsv(os.path.join(a.root, "docs", "audits", "ultra", "verdicts.tsv")):
            if v.get("item") == a.preuves:
                found = [f"{c}#{m}" for c, m in proofs(v.get("preuve_appli")) if method_exists(sources, c, m)]
                for path, fn in engine_proofs(v.get("preuve_appli")):
                    defined, name = engine_test(a.root, path, fn)
                    if defined and name:
                        found.append(f"{path}#{fn} ({name})")
                print(f"{a.preuves} : {len(found)} preuve(s) : {', '.join(found)}")
        return
    gaps = check(a.root, a.resume, with_git=not a.sans_git)
    if not a.sans_git:
        gaps += red_green_pairs(a.root) + checked_lines(a.root) + merge_trace_gaps(a.root)
    for g in gaps:
        print(g)
    print(f"chantier : {len(gaps)} écarts")
    sys.exit(1 if gaps else 0)


if __name__ == "__main__":
    main()
