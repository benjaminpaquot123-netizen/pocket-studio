#!/usr/bin/env python3
"""Checks the « banc fiable » work by program, not by eye (PLAN-RESTE 4.0, audit « banc crédible » A.10).

  python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume.txt [--root DIR] [--sans-git]

(a) verdicts.tsv, four criteria:
    1. an « appli » line has a proof: a test without the bench that exists (never a Bench* test), or a dated ❌
       trial on a device (« tel-NN … ❌ … 2026-11-08 » in one « ; » segment of its telephone or note cell);
    2. a « banc » line has a Bench* guard AND a proof without the bench (a test that exists outside Bench*):
       the bench judging itself is not a proof;
    3. the summary of the latest self-test is mandatory (« résumé absent » without it) and a guard is green
       only when the summary names it « vert : Class#method »;
    4. a « corrigé » line is in base.tsv with its round and its commit cell names a commit that exists.
    Also: every proof names a test that exists: a Kotlin « Class#method », or an engine host test
    « engine/host-tests/<file>#<function> » defined in that file and registered in a PS_*_TESTS list (A.12);
    every @KnownDefect(id) in the tests has a line « appli » citing its test, one of them with the id in its
    « garde_id » column; a line's garde_id is carried by one of its proofs (a @KnownDefect, or an engine test
    registered as « known_defect_<id> », which the C++ runner prints as « KNOWN DEFECT still there »).
    --preuves ITEM prints the proofs read on that line and stops.
(b) git log: a « vert: » commit closes the oldest open « rouge: » commit OF THE SAME TEST (same subject after
    the prefix, or a test the red names or adds that the green's message names). A green of another test closes
    nothing. A checked line of banc-fiable-journal.md (« - [x] 4.2 … », ids 4.x to 7.x, R.x, A.x) carries
    « preuve: rouge <sha> / vert <sha> » naming such a pair, or « preuve: script <path> » (a figure computed
    by a script that exists).
(c) recette.json <-> verdicts: every « recette tel-NN » cited by an open undecided line exists in the
    checklist, and every checklist item is cited by some line.
Prints one line per gap, then « chantier : N écarts ». Exit 1 when N > 0. Standard library only.
"""
import argparse
import json
import os
import re
import subprocess
import sys

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


def dated_failed_trial(v):
    """A dated ❌ trial on a device: one « ; » segment of the telephone or note cell with tel-NN, ❌ and a date."""
    for cell in (v.get("telephone", ""), v.get("note", "")):
        for part in cell.split(";"):
            if re.search(r"\btel-\d+", part) and "❌" in part and re.search(r"\b20\d\d-\d\d-\d\d\b", part):
                return True
    return False


def commit_exists(root, sha):
    try:
        return subprocess.run(["git", "-C", root, "cat-file", "-e", sha + "^{commit}"], capture_output=True, timeout=60).returncode == 0
    except (subprocess.SubprocessError, OSError):
        return False


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
        resume = open(resume_path, encoding="utf-8").read()
    base = {r.get("cle"): r for r in read_tsv(os.path.join(ultra, "base.tsv"))}

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
        if verdict == "appli" and not without_bench and not dated_failed_trial(v):
            gaps.append(f"{item} : verdict « appli » sans preuve (ni test sans banc existant, ni essai ❌ daté)")
        if verdict == "appli" and etat == "ouvert" and without_bench and not carried and not dated_failed_trial(v):
            gaps.append(f"{item} : verdict « appli » dont la preuve n'est ni @KnownDefect ni un essai ❌ daté "
                        f"(un test vert ne prouve pas un défaut ouvert)")
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


def red_green_pairs(root, since="2026-11-07"):
    """(b) Every bench repair is a « rouge: » commit (the guard, red) followed by a « vert: » commit (the fix) of the
    same test. A red whose test gets no green is a gap; so is a green that closes no red of its test."""
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
            else:
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
        gaps += red_green_pairs(a.root) + checked_lines(a.root)
    for g in gaps:
        print(g)
    print(f"chantier : {len(gaps)} écarts")
    sys.exit(1 if gaps else 0)


if __name__ == "__main__":
    main()
