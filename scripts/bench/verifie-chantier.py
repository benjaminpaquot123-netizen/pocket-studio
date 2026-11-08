#!/usr/bin/env python3
"""Checks the « banc fiable » work by program, not by eye (PLAN-RESTE 4.0).

  python3 -I scripts/bench/verifie-chantier.py [--root DIR] [--resume selftest-resume.txt]

(a) verdicts.tsv: every proof names a Class#method that exists in the tests; a proof of the app is
    never a Bench* test; a « banc » line has a Bench* guard, green in the latest selftest summary when
    one is given; every @KnownDefect(id) in the tests has a line « appli » citing its test, and every
    such cited test exists; every « corrigé » line is in base.tsv with its round.
(c) recette.json <-> verdicts: every « recette tel-NN » cited by an open undecided line exists in the
    checklist, and every checklist item is cited by some line.
Prints one line per gap, then « chantier : N écarts ». Exit 1 when N > 0. Standard library only.
"""
import argparse
import json
import os
import re
import sys

TEST_ROOT = os.path.join("app", "src", "test", "java")
PROOF = re.compile(r"([A-Z][A-Za-z0-9_]*)[#.]([a-z][A-Za-z0-9_]*)")


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
    return [(c, m) for c, m in PROOF.findall(cell or "") if c[0].isupper()]


def green_in_resume(resume_text, cls, method):
    if resume_text is None:
        return True
    bad = re.search(r"(rouge|échec|failed)[^\n]*\b" + re.escape(cls) + r"\b[^\n]*" + re.escape(method), resume_text)
    return bad is None


def check(root, resume_path=None):
    gaps = []
    ultra = os.path.join(root, "docs", "audits", "ultra")
    verdicts = read_tsv(os.path.join(ultra, "verdicts.tsv"))
    sources = test_sources(root)
    resume = open(resume_path, encoding="utf-8").read() if resume_path else None
    base = {r.get("cle"): r for r in read_tsv(os.path.join(ultra, "base.tsv"))}

    for v in verdicts:
        item = v.get("item", "?")
        verdict, etat = v.get("verdict", ""), v.get("etat", "")
        for cls, m in proofs(v.get("preuve_appli")):
            if cls.startswith("Bench"):
                gaps.append(f"{item} : preuve d'appli {cls}#{m} est un test du banc")
            elif not method_exists(sources, cls, m):
                gaps.append(f"{item} : preuve d'appli {cls}#{m} introuvable")
        guards = proofs(v.get("garde_banc"))
        for cls, m in guards:
            if not method_exists(sources, cls, m):
                gaps.append(f"{item} : garde {cls}#{m} introuvable")
            elif not green_in_resume(resume, cls, m):
                gaps.append(f"{item} : garde {cls}#{m} pas verte dans le dernier selftest")
        if verdict == "banc" and not any(c.startswith("Bench") for c, _ in guards):
            gaps.append(f"{item} : verdict « banc » sans garde Bench*")
        if etat == "corrigé":
            row = base.get(v.get("cle"))
            if row is None:
                gaps.append(f"{item} : « corrigé » absent de base.tsv ({v.get('cle')})")
            elif not (row.get("corrige_au") or "").strip() and row.get("etat") != "corrigé":
                gaps.append(f"{item} : « corrigé » sans tour dans base.tsv")

    cited = " ".join(v.get("preuve_appli", "") for v in verdicts if v.get("verdict") == "appli")
    for cls, text in sorted(sources.items()):
        for kid, method in re.findall(r'@KnownDefect\(\s*"([^"]+)"[^)]*\)\s*(?:@[A-Za-z]+(?:\([^)]*\))?\s*)*fun\s+`?([A-Za-z0-9_]+)', text):
            if f"{cls}#{method}" not in cited:
                gaps.append(f"@KnownDefect {kid} ({cls}#{method}) sans ligne « appli » qui le cite")

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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    ap.add_argument("--resume", default=None, help="selftest-resume.txt of the latest selftest")
    a = ap.parse_args()
    gaps = check(a.root, a.resume)
    for g in gaps:
        print(g)
    print(f"chantier : {len(gaps)} écarts")
    sys.exit(1 if gaps else 0)


if __name__ == "__main__":
    main()
