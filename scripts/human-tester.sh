#!/usr/bin/env bash
# The « testeur humain » bench (owner, 2026-10-28): the whole app on a phone-sized Robolectric
# screen, driven by a tester who only sees the screen (HumanTester.kt). One seat a folder:
#   <folder>/a/start.txt … <folder>/f/start.txt (simple=true|false, timeout.minutes=20). Only the
#   seats with a start.txt are started (E and F: the Remix's testers); up to 4 run side by side.
# The tester plays with scripts/human-act.py <folder>/<seat> <gesture>.
set -euo pipefail
dir="$(cd "${1:?usage: scripts/human-tester.sh <folder>}" && pwd)"
cd "$(dirname "$0")/.."
seats=()
for seat in a b c d e f; do
  if [ -f "$dir/$seat/start.txt" ]; then seats+=(--tests "com.pocketstudio.app.human.HumanTester${seat^^}"); fi
done
if [ ${#seats[@]} -eq 0 ]; then
  echo "No seat: put a start.txt in $dir/<a-f>/." >&2
  exit 2
fi
# shellcheck disable=SC2086
./gradlew ${GRADLE_ARGS:-} :app:testDebugUnitTest "${seats[@]}" -Phuman.dir="$dir" --rerun
