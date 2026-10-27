"""CI: lists the screenshots that differ from the committed ones (Roborazzi compare summary).

Never fails the build: a GitHub warning per run and a line per screen in the job summary.
Usage: screenshot-diff.py <results-summary.json>
"""
import json
import os
import sys


def main(path):
    if not os.path.exists(path):
        print(f"::warning::No screenshot summary ({path}): the comparison did not run.")
        return 0
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    summary = data.get("summary", {})
    odd = [r for r in data.get("results", []) if r.get("type") in ("changed", "added")]
    lines = [f"Captures : {summary.get('total', 0)} au total, {summary.get('changed', 0)} différentes, "
             f"{summary.get('added', 0)} sans référence."]
    for r in odd:
        lines.append(f"- {r['type']} : {os.path.basename(r.get('golden_file_path', '?'))}")
    print("\n".join(lines))
    if odd:
        print(f"::warning::{len(odd)} screenshot(s) differ from app/screenshots/: see the screenshots-diff artifact.")
    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as f:
            f.write("### Captures d'écran\n\n" + "\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
