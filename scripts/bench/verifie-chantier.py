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
       (tampon.sh code) and, when the lot's junit/ is there, its results give it (tampon.sh resultats). Else
       « résumé refusé » and no guard is green;
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
    the prefix, or a test the red names or adds that the green's message names). A green of another test closes
    nothing. A green (not a merge) that takes @KnownDefect off a test (kept) whose « Class#method » its message names
    is a pair on its own, when that guard was added by a « rouge: » commit or before the discipline began: that
    guard, failing with the defect's message in the parent, was the red. A checked line of banc-fiable-journal.md (« - [x] 4.2 … », ids 4.x to 7.x, R.x, A.x) carries
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
from datetime import datetime

TEST_ROOT = os.path.join("app", "src", "test", "java")
PROOF = re.compile(r"([A-Z][A-Za-z0-9_]*)[#.]([a-z][A-Za-z0-9_]*)")
ENGINE_PROOF = re.compile(r"(engine/host-tests/[A-Za-z0-9_./-]+)#([A-Za-z_][A-Za-z0-9_]*)")
KNOWN_DEFECT = re.compile(r'@KnownDefect\(\s*"([^"]+)"[^)]*\)\s*(?:@[A-Za-z]+(?:\([^)]*\))?\s*)*fun\s+`?([A-Za-z0-9_]+)')


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


def method_exists(sources, cls, method):
    text = sources.get(cls)
    return text is not None and re.search(r"\bfun\s+`?" + re.escape(method) + r"\b", text) is not None


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
    if os.path.isdir(os.path.join(lot, "junit")):
        r = subprocess.run(["bash", os.path.join(HERE, "tampon.sh"), "resultats", lot], capture_output=True, text=True)
        if r.returncode != 0:
            return "ses résultats JUnit ne donnent pas ce résumé vert, ou le tampon ne les scelle pas"
    return None


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
            elif not method_exists(sources, cls, m):
                gaps.append(f"{item} : preuve d'appli {cls}#{m} introuvable")
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
ADDED_TEST = re.compile(r"^\+\s*(?:fun|def|static void)\s+`?([A-Za-z0-9_]{12,})", re.M)
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


# The rouge/vert discipline began with the first « rouge: » commit (8f36fa3, 2026-11-08 18:29 UTC): a guard added
# before it could not come from a red, it counts as there before (Tour5AppProofTest's, 4168889).
DISCIPLINE_SINCE = "2026-11-08T18:29:57+00:00"
CLASS = re.compile(r"^\s*(?:(?:private|internal|public|abstract|open|data)\s+)*class\s+([A-Za-z0-9_]+)", re.M)
FUN = re.compile(r"\bfun\s+`?([A-Za-z0-9_]+)")


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


def guard_from_red(root, sha, path, test):
    """True when the @KnownDefect on test (« Class#method ») that commit sha lifts came from a « rouge: » commit, or
    was added before the rouge/vert discipline began: the newest commit before sha that put it there (its parent
    without it) is a red, or older than DISCIPLINE_SINCE. Any other origin (a « chore: », a guard never seen
    added) is no red evidence (review of the « pads-live » lot, moyen)."""
    try:
        log = _git(root, "log", "--no-merges", "--format=%H%x1f%s%x1f%cI", f"{sha}^", "--", path).splitlines()
    except (subprocess.SubprocessError, OSError):
        return False
    for line in log:
        h, subject, date = line.split("\x1f", 2)
        now, before = _show(root, h, path), _show(root, h + "^", path)
        if now is not None and test in class_methods(now, True) and (before is None or test not in class_methods(before, True)):
            return subject.lower().startswith("rouge:") or datetime.fromisoformat(date) < datetime.fromisoformat(DISCIPLINE_SINCE)
    return False


def known_defects_lifted(root, sha):
    """The tests (« Class#method ») a commit takes @KnownDefect off, the test kept (review of the « partage » lot, (c)):
    their guard was the red (KnownDefectRule: the test failed with the defect's message until this commit), provided
    the guard came from a red ([guard_from_red]). A test deleted is not one; a merge lifts nothing (review of the
    « pads-live » lot, moyen: the lift must be a commit of its own)."""
    try:
        parents = _git(root, "rev-list", "--parents", "-n", "1", sha).split()[1:]
        if len(parents) != 1:
            return set()
        files = _git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", sha + "^", sha).split()
    except (subprocess.SubprocessError, OSError):
        return set()
    lifted = set()
    for f in files:
        if not (f.startswith(TEST_ROOT.replace(os.sep, "/") + "/") and f.endswith(".kt")):
            continue
        before, after = _show(root, sha + "^", f), _show(root, sha, f)
        if before is None or after is None:
            continue
        gone = (class_methods(before, True) - class_methods(after, True)) & class_methods(after, False)
        lifted |= {t for t in gone if guard_from_red(root, sha, f, t)}
    return lifted


def names(message, test):
    """The message names « Class#method » (or « Class.method »), the class too: a method name alone matches any
    class that has one (review of the « pads-live » lot, moyen)."""
    cls, _, method = test.partition("#")
    return re.search(r"(?<![A-Za-z0-9_])" + re.escape(cls) + r"[#.]" + re.escape(method) + r"(?![A-Za-z0-9_])", message) is not None


def red_green_pairs(root, since="2026-11-07"):
    """(b) Every bench repair is a « rouge: » commit (the guard, red) followed by a « vert: » commit (the fix) of the
    same test. A red whose test gets no green is a gap; so is a green that closes no red of its test, unless it takes
    @KnownDefect off a test whose class and method it names, a guard that came from a red (that guard was the red:
    [known_defects_lifted])."""
    try:
        out = _git(root, "log", f"--since={since}", "--reverse", "--format=%x1e%h%x1f%s%x1f%B")
    except (subprocess.SubprocessError, OSError) as e:
        return [f"git log impossible : {e}"]
    gaps, open_reds = [], []
    for record in out.split("\x1e")[1:]:
        sha, subject, message = record.split("\x1f", 2)
        low = subject.lower()
        if low.startswith("rouge:"):
            open_reds.append((sha, subject, message, red_names(root, sha, message)))
        elif low.startswith("vert:"):
            green = (sha, subject, message, set())
            red = next((r for r in open_reds if same_test(r, green)), None)
            if red is not None:
                open_reds.remove(red)
            elif not any(names(message, t) for t in known_defects_lifted(root, sha)):
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
    a = ap.parse_args()
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
