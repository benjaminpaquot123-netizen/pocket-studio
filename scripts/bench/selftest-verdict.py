#!/usr/bin/env python3
"""Banc fiable 1.2: the self-test's verdict, test by test, from the JUnit XML results.
   selftest-verdict.py <results dir> <quarantine file> <summary out>
Exit 0 when every test ran green or is a quarantined red; 1 otherwise (and when no test ran at all).
The summary: first line « N verts, R rouges, Q en quarantaine, S sautés », then one line per red, quarantined and
skipped test (with the skip's reason, so a skip is never silent)."""
import pathlib
import sys
import xml.etree.ElementTree as ET


def quarantine(path):
    names = set()
    for line in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            names.add(line.split("|")[0].strip())
    return names


def verdict(results, quarantined):
    green, red, held, skipped = [], [], [], []
    for xml in sorted(pathlib.Path(results).glob("TEST-*.xml")):
        for case in ET.parse(xml).getroot().iter("testcase"):
            name = f"{case.get('classname', '').rsplit('.', 1)[-1]}#{case.get('name')}"
            failure = case.find("failure")
            if failure is None:
                failure = case.find("error")
            skip = case.find("skipped")
            if failure is not None:
                why = (failure.get("message") or failure.text or "").strip().splitlines()[:1]
                (held if name in quarantined else red).append((name, why[0][:200] if why else ""))
            elif skip is not None:
                skipped.append((name, (skip.get("message") or skip.text or "sans raison donnée").strip()[:200]))
            else:
                green.append(name)
    return green, red, held, skipped


def main(argv):
    results, qfile, out = argv[1:4]
    green, red, held, skipped = verdict(results, quarantine(qfile))
    lines = [f"{len(green)} verts, {len(red)} rouges, {len(held)} en quarantaine, {len(skipped)} sautés"]
    lines += [f"rouge : {n} : {w}" for n, w in red]
    lines += [f"quarantaine rouge : {n} : {w}" for n, w in held]
    lines += [f"sauté : {n} : {w}" for n, w in skipped]
    pathlib.Path(out).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(lines[0])
    return 0 if green and not red else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
