#!/usr/bin/env python3
"""Runs unittest tests of a bench script and writes a JUnit XML (classname « <module>.<Class> », name the method).

  junit.py <repo root> <out.xml> <module> <Class.method> [<Class.method> …]

The module is loaded from <repo root>/scripts/bench/<module>.py, run from <repo root> (as `python3 -I scripts/bench/...`).
"""
import importlib.util
import os
import sys
import unittest
from xml.sax.saxutils import quoteattr


def main(root, out, module, names):
    os.chdir(root)
    spec = importlib.util.spec_from_file_location(module, os.path.join(root, "scripts", "bench", module + ".py"))
    mod = importlib.util.module_from_spec(spec)
    sys.modules[module] = mod
    spec.loader.exec_module(mod)
    cases = []
    for name in names:
        cls, method = name.split(".")
        result = unittest.TestResult()
        getattr(mod, cls)(method).run(result)
        bad = result.failures + result.errors
        body = ""
        if bad:
            body = f"<failure message={quoteattr(bad[0][1].strip().splitlines()[-1][:300])}>{quoteattr(bad[0][1])[1:-1]}</failure>"
        elif result.skipped:
            body = "<skipped/>"
        cases.append(f'  <testcase classname="{module}.{cls}" name="{method}">{body}</testcase>')
        print(f"{module}.{cls}.{method}: {'FAIL' if bad else 'skip' if result.skipped else 'ok'}")
    with open(out, "w", encoding="utf-8") as f:
        f.write(f'<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="{module}" tests="{len(cases)}">\n' + "\n".join(cases) + "\n</testsuite>\n")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:])
