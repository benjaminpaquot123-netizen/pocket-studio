#!/usr/bin/env bash
# The « testeur humain » bench (owner, 2026-10-28): the whole app on a phone-sized Robolectric
# screen, driven by a tester who only sees the screen (HumanTester.kt). One seat a folder:
#   <folder>/a/start.txt … <folder>/f/start.txt. Only the seats with a start.txt are started; up to 4
#   run side by side. The bench 2's options (Audit ultra §3.1, SeatOptions.kt, docs/lots/ULTRA-P0-A.md):
#   lang= device= font=100|130|200 mode=simple|pro theme=studio|night|daltonien fresh=reel gestes.max=
#   lit=non main=une|gauche amenee=premier-morceau|remix|padlive mission= timeout.minutes=20.
# The tester plays with scripts/human-act.py <folder>/<seat> <gesture>; scripts/human-replay.sh replays
# a seat, scripts/bench/seat-metrics.py measures it, scripts/bench/fuse-reports.py fuses a round's reports.
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
