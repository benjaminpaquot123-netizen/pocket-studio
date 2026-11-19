"""CI: lists the screenshots that differ from the committed ones (Roborazzi compare summary).

Never fails the build: a GitHub warning per run and a line per screen in the job summary.
Usage: screenshot-diff.py <results-summary.json> [<results-summary.json> ...]
Several summaries (the unit tests run on several machines, one summary each) are read as one: their counts are
added. A summary that is missing means a part of the suite did not report: the line « Captures : … » is then NOT
printed (scripts/bench/paquet-verifie.sh reads its absence as « captures non lues », which is not green).
"""
import json
import os
import sys


def short_name(path):
    """The golden's path from app/screenshots/ on: 156 goldens share a basename, so the basename alone is ambiguous."""
    norm = path.replace("\\", "/")
    marker = "app/screenshots/"
    i = norm.rfind(marker)
    return norm[i + len(marker):] if i >= 0 else norm


def main(paths):
    missing = [p for p in paths if not os.path.exists(p)]
    if missing:
        print(f"::warning::No screenshot summary ({', '.join(missing)}): the comparison did not run.")
        return 0
    total = changed = added = 0
    odd = []
    for path in paths:
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        summary = data.get("summary", {})
        total += summary.get("total", 0)
        changed += summary.get("changed", 0)
        added += summary.get("added", 0)
        odd += [r for r in data.get("results", []) if r.get("type") in ("changed", "added")]
    lines = [f"Captures : {total} au total, {changed} différentes, {added} sans référence."]
    for r in sorted(odd, key=lambda r: (r["type"], short_name(r.get("golden_file_path", "?")))):
        lines.append(f"- {r['type']} : {short_name(r.get('golden_file_path', '?'))}")
    print("\n".join(lines))
    if odd:
        print(f"::warning::{len(odd)} screenshot(s) differ from app/screenshots/: see the screenshots-diff artifact.")
    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as f:
            f.write("### Captures d'écran\n\n" + "\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    sys.exit(main(sys.argv[1:]))
