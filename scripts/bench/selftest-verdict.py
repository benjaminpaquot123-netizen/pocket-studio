#!/usr/bin/env python3
"""Banc fiable 1.2: the self-test's verdict, test by test, from the JUnit XML results.
   selftest-verdict.py <results dir> <quarantine file> <summary out> [<expected classes file> [<failed Gradle runs file>
                       [<known skips file>]]]
Exit 0 when every test ran green, is a quarantined red (failing with the message its quarantine line names, « attendu :
<texte> »: audit « banc crédible » 2, P6) or a known skip; 1 otherwise: no test at all, an expected class
of which no test ran (no results: memory, stall, crash; or every test skipped: audit « banc crédible » A.4), a skip that
is not in the list of known skips (same format as the quarantine; no list = no skip allowed), or a Gradle run that
failed with no red test to show for it.
The summary: first line « N verts, R rouges, Q en quarantaine, S sautés », then one line per red, quarantined and
skipped test (with the skip's reason, so a skip is never silent), then one « vert : Class#method » line per green test:
the judge (verifie-chantier.py) reads a guard as green only when it is named there."""
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


def expected_messages(path):
    """Audit « banc crédible » 2, P6: {Class#method: the message its failure must hold} (cell « attendu : <texte> »;
    None when the line names none: such a line excuses nothing)."""
    out = {}
    for line in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            cells = [c.strip() for c in line.split("|")]
            wanted = [c.split(":", 1)[1].strip() for c in cells[1:] if c.lower().startswith("attendu") and ":" in c]
            out[cells[0]] = wanted[0] if wanted and wanted[0] else None
    return out


def optional_lines(path):
    p = pathlib.Path(path)
    return [l.strip() for l in p.read_text(encoding="utf-8").splitlines() if l.strip()] if p.is_file() else []


def verdict(results, quarantined):
    green, red, held, skipped = [], [], [], []
    for xml in sorted(pathlib.Path(results).rglob("TEST-*.xml")):
        for case in ET.parse(xml).getroot().iter("testcase"):
            name = f"{case.get('classname', '').rsplit('.', 1)[-1]}#{case.get('name')}"
            failure = case.find("failure")
            if failure is None:
                failure = case.find("error")
            skip = case.find("skipped")
            if failure is not None:
                full = (failure.get("message") or "") + "\n" + (failure.text or "")
                why = (failure.get("message") or failure.text or "").strip().splitlines()[:1]
                why = why[0][:200] if why else ""
                if name not in quarantined:
                    red.append((name, why))
                elif quarantined[name] is None:
                    red.append((name, "quarantaine sans message attendu (colonne « attendu : … ») : " + why))
                elif quarantined[name] not in full:
                    red.append((name, f"quarantaine : message inattendu (attendu « {quarantined[name]} ») : " + why))
                else:
                    held.append((name, why))
            elif skip is not None:
                skipped.append((name, (skip.get("message") or skip.text or "sans raison donnée").strip()[:200]))
            else:
                green.append(name)
    return green, red, held, skipped


def main(argv):
    results, qfile, out = argv[1:4]
    green, red, held, skipped = verdict(results, expected_messages(qfile))
    ran = {n.split("#")[0] for n, *_ in [(g,) for g in green] + red + held}
    only_skipped = {n.split("#")[0] for n, _ in skipped} - ran
    expected = optional_lines(argv[4]) if len(argv) > 4 else []
    known_skips = quarantine(argv[6]) if len(argv) > 6 and pathlib.Path(argv[6]).is_file() else set()
    # A known skip named without parameters (« Class#drive ») covers each parameterised case (« Class#drive[...] »).
    red += [(n, "saut hors de la liste des sauts connus : " + w) for n, w in skipped
            if n not in known_skips and n.split("[")[0] not in known_skips]
    red += [(f"{c}#*", "aucun test non sauté : la classe est sautée en entier") for c in expected if c in only_skipped]
    red += [(f"{c}#*", "aucun résultat : la classe n'a pas tourné") for c in expected if c not in ran and c not in only_skipped]
    failed_runs = optional_lines(argv[5]) if len(argv) > 5 else []
    # PLAN-RESTE 4.3: judged pass by pass (results/<pass>/): a pass whose Gradle failed with no red test of its own is
    # red, even if another pass has a quarantined red. (Results not split in passes: the whole folder is the pass.)
    for r in failed_runs:
        own = pathlib.Path(results) / r
        _, own_red, own_held, _ = verdict(own, expected_messages(qfile)) if own.is_dir() else ([], red, held, [])
        if not own_red and not own_held:
            red.append((f"gradle:{r}", "Gradle en échec sans test rouge (compilation, mémoire, blocage ?)"))
    lines = [f"{len(green)} verts, {len(red)} rouges, {len(held)} en quarantaine, {len(skipped)} sautés"]
    lines += [f"rouge : {n} : {w}" for n, w in red]
    lines += [f"quarantaine rouge : {n} : {w}" for n, w in held]
    lines += [f"sauté : {n} : {w}" for n, w in skipped]
    lines += [f"vert : {n}" for n in green]
    pathlib.Path(out).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(lines[0])
    return 0 if green and not red else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
