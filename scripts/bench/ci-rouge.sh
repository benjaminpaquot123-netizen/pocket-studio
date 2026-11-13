#!/usr/bin/env bash
# Tour 6, lot « stable »: the red unit tests of a CI run, each with its place in the run.
#   scripts/bench/ci-rouge.sh <run id>            (downloads the run's « unit-test-reports » artifact with gh)
#   scripts/bench/ci-rouge.sh --dossier <dir>     (an artifact already downloaded and unzipped: its JUnit XML files)
# For each red test: its message (first line), the rank of its class in the run (the classes ordered by the timestamps
# of their JUnit XML, i.e. the order they ran in the one test process), the three classes run just before it (a red
# test is often another class's leftover: BenchPristine names what was left), and its duration.
# Exit 0 when the reports were read (red tests or not), 2 on a missing artifact or folder.
set -euo pipefail
dir=""
case "${1:-}" in
  --dossier)
    dir="${2:-}"
    [ -n "$dir" ] && [ -d "$dir" ] || { echo "ci-rouge: no folder « ${2:-} »" >&2; exit 2; }
    ;;
  ""|-h|--help)
    sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
  *)
    run="$1"
    root="$(cd "$(dirname "$0")/../.." && pwd)"
    repo="$(git -C "$root" remote get-url origin | sed -E 's#.*github.com[:/]##; s#\.git$##')"
    dir="$(mktemp -d)"
    trap 'rm -rf "$dir"' EXIT
    if ! gh run download "$run" -R "$repo" -n unit-test-reports -D "$dir" >/dev/null 2>&1; then
      echo "ci-rouge: no « unit-test-reports » artifact for run $run (green run, gh not signed in, or expired): try --dossier" >&2
      exit 2
    fi
    ;;
esac
exec python3 -I - "$dir" <<'PY'
import os, sys, xml.etree.ElementTree as ET

root = sys.argv[1]
suites = []
for d, _, files in os.walk(root):
    for f in files:
        if f.startswith("TEST-") and f.endswith(".xml"):
            try:
                s = ET.parse(os.path.join(d, f)).getroot()
            except ET.ParseError as e:
                print(f"? {f}: unreadable ({e})")
                continue
            suites.append(s)
if not suites:
    print(f"ci-rouge: no JUnit XML (TEST-*.xml) under {root}", file=sys.stderr)
    sys.exit(2)
# One process runs the classes one after the other: the timestamp of each class's report is its start.
suites.sort(key=lambda s: (s.get("timestamp", ""), s.get("name", "")))
names = [s.get("name", "?") for s in suites]
red = 0
for i, s in enumerate(suites):
    for c in s.findall("testcase"):
        bad = c.find("failure")
        if bad is None:
            bad = c.find("error")
        if bad is None:
            continue
        red += 1
        msg = (bad.get("message") or (bad.text or "").strip()).splitlines()
        before = names[max(0, i - 3):i]
        print(f"ROUGE {s.get('name')}#{c.get('name')} ({c.get('time', '?')} s)")
        print(f"  message : {(msg[0] if msg else '?')[:400]}")
        print(f"  rang    : classe {i + 1}/{len(suites)}, partie à {s.get('timestamp', '?')}")
        print(f"  avant   : {' > '.join(before) if before else '(aucune : première classe)'}")
print(f"{red} test(s) rouge(s), {len(suites)} classes lues")
PY
