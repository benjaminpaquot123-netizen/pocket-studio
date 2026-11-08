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
# Audit method v2 (META-AUDIT changes 1 and 8): no lot without a green self-test of the bench on this very code
# (scripts/bench/selftest.sh <lot folder>), and Gradle only through the one queue (scripts/bench/gradle-file.sh).
stamp="$dir/selftest.ok"
if [ "${PS_SKIP_SELFTEST:-}" != "" ]; then
  # Banc fiable 1.2: a bypass names a test of the quarantine list (scripts/bench/selftest-quarantaine.txt), else refused.
  if ! grep -v '^#' scripts/bench/selftest-quarantaine.txt | cut -d'|' -f1 | sed 's/ *$//' | grep -qxF "$PS_SKIP_SELFTEST"; then
    echo "PS_SKIP_SELFTEST='$PS_SKIP_SELFTEST' is not a quarantined test (scripts/bench/selftest-quarantaine.txt): refused." >&2
    exit 3
  fi
  echo "selftest skipped: $PS_SKIP_SELFTEST (quarantined)" | tee -a "$dir/selftest-skipped.txt" >&2
elif ! scripts/bench/tampon.sh verifie "$dir"; then
  echo "No green self-test for this code: run scripts/bench/selftest.sh $dir first (or PS_SKIP_SELFTEST=<a quarantined test>)." >&2
  exit 3
fi
# shellcheck disable=SC2086
PS_GRADLE_LOG="$dir/bench.log" scripts/bench/gradle-file.sh ${GRADLE_ARGS:-} :app:testDebugUnitTest "${seats[@]}" -Phuman.dir="$dir" --rerun
