#!/usr/bin/env python3
"""The test classes the bench's self-test expects and runs (audit « banc crédible » A.3), read from the sources.

  python3 -I scripts/bench/selftest-attendus.py [--root DIR]
  python3 -I scripts/bench/selftest-attendus.py [--root DIR] --sauts-connus scripts/bench/selftest-sauts-connus.txt

One « <pass> <fully qualified class> » line per class, sorted:
  banc       every Bench*Test and Tour*AppProof<digits>Test of app/src/test/…/human (Tour5AppProof2Test too);
  renommage  every Tour*IdleTest of the same folder (a Gradle run of their own, see selftest.sh);
  defauts    every class that carries a @KnownDefect anywhere else under app/src/test/java (found by reading the
             sources: a guard of a known defect that the self-test does not run guards nothing).

--sauts-connus FILE (review of lot 1): the list of known skips is bounded. Only « <Class>#drive » of a class of the
unit tests that inherits HumanTester is accepted (a seat's entry point, skipped outside seat mode); every other entry
is printed « saut connu refusé : … » and the exit code is 1: a real test is never skipped in silence.
Standard library only.
"""
import argparse
import os
import re

TESTS = os.path.join("app", "src", "test", "java")
BANC = re.compile(r"^(Bench[A-Za-z0-9]*Test|Tour[A-Za-z0-9]*AppProof[0-9]*Test)$")
RENAME = re.compile(r"^Tour[A-Za-z0-9]*IdleTest$")
CLASS = re.compile(r"^(?:internal |private |open |abstract |data )*class\s+([A-Za-z0-9_]+)", re.M)
KNOWN = re.compile(r"^\s*@KnownDefect\(", re.M)


def classes(text):
    """[(name, its own source text)]: the file cut at each top-level class declaration."""
    decl = list(CLASS.finditer(text))
    return [(m.group(1), text[m.start():decl[i + 1].start() if i + 1 < len(decl) else len(text)]) for i, m in enumerate(decl)]


def expected(root):
    out = set()
    for d, _, files in os.walk(os.path.join(root, TESTS)):
        human = os.path.basename(d) == "human"
        for name in files:
            if not name.endswith(".kt"):
                continue
            text = open(os.path.join(d, name), encoding="utf-8").read()
            package = re.search(r"^package\s+([A-Za-z0-9_.]+)", text, re.M)
            prefix = package.group(1) + "." if package else ""
            for cls, body in classes(text):
                if human and BANC.match(cls):
                    out.add(("banc", prefix + cls))
                elif human and RENAME.match(cls):
                    out.add(("renommage", prefix + cls))
                elif KNOWN.search(body):
                    out.add(("defauts", prefix + cls))
    return sorted(out)


def seats(root):
    """The names of the test classes that inherit HumanTester (« class Name(…) : HumanTester(… »)."""
    out = set()
    for d, _, files in os.walk(os.path.join(root, TESTS)):
        for name in files:
            if name.endswith(".kt"):
                text = open(os.path.join(d, name), encoding="utf-8").read()
                out |= {m.group(1) for m in re.finditer(CLASS.pattern + r"\b[^{]*?:\s*HumanTester\s*\(", text, re.M)}
    return out


def refused_skips(root, path):
    """The entries of the list of known skips that are not « <Class>#drive » of a class inheriting HumanTester."""
    allowed = {cls + "#drive" for cls in seats(root)}
    out = []
    for line in open(path, encoding="utf-8").read().splitlines():
        entry = line.split("|")[0].strip()
        if entry and not entry.startswith("#") and entry not in allowed:
            out.append(entry)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    ap.add_argument("--sauts-connus", metavar="FILE", help="check the list of known skips, print the entries refused")
    a = ap.parse_args()
    if a.sauts_connus:
        bad = refused_skips(a.root, a.sauts_connus)
        for entry in bad:
            print(f"saut connu refusé : {entry} (seul « <Classe>#drive » d'une classe qui hérite de HumanTester est accepté)")
        raise SystemExit(1 if bad else 0)
    for run, cls in expected(a.root):
        print(run, cls)


if __name__ == "__main__":
    main()
