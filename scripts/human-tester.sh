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
  # Banc fiable 1.2 (and its review): no bypass any more; a red test in quarantine (scripts/bench/selftest-quarantaine.txt)
  # already lets the self-test pass, with a stamp naming it.
  echo "PS_SKIP_SELFTEST is refused: run scripts/bench/selftest.sh $dir (a known red goes in scripts/bench/selftest-quarantaine.txt)." >&2
  exit 3
elif ! scripts/bench/tampon.sh verifie "$dir"; then
  # The stamp's own reason is printed above (tampon.sh verifie). Round 6, lot banc7 (point 7): it covers the start.txt present
  # when the self-test ran: put the other seats' start.txt aside BEFORE selftest.sh.
  echo "No green self-test for this code and these seats (start.txt): see the reason above, then run scripts/bench/selftest.sh $dir." >&2
  exit 3
fi
# Round 6, lot banc7 (point 8, E9): the bench says it runs for this lot (its PID), so a seat's first « regarde » without it
# is told at once to start this script instead of waiting 15 min. Removed when the bench ends, before the exit check.
echo "$$" > "$dir/.banc-en-cours"
# PLAN-RESTE 4.2: files written outside the seats' folders during the lot are listed after it (incidents-lot.tsv).
python3 -I scripts/bench/sortie-de-siege.py avant "$dir"
trap 'rm -f "$dir/.banc-en-cours"; python3 -I scripts/bench/sortie-de-siege.py apres "$dir" || true' EXIT
# Round 6, lot banc7 (point 6): the Gradle queue kills a run silent for PS_STALL_MIN minutes (20 by default), but a seat waits
# up to its timeout.minutes (30 from fabrique-sieges) for its tester's next gesture, printing nothing: the queue waits the
# longest seat timeout of the lot plus 10 min (the seat's own watchdog ends a silent seat first, and says why).
longest=20
for s in "$dir"/[a-f]/start.txt; do
  [ -f "$s" ] || continue
  t="$(sed -n 's/^timeout\.minutes=\([0-9][0-9]*\)$/\1/p' "$s" | tail -1)"
  [ -n "$t" ] && [ "$t" -gt "$longest" ] && longest="$t"
done
export PS_STALL_MIN="${PS_STALL_MIN:-$((longest + 10))}"
# shellcheck disable=SC2086
PS_GRADLE_LOG="$dir/bench.log" scripts/bench/gradle-file.sh ${GRADLE_ARGS:-} :app:testDebugUnitTest "${seats[@]}" -Phuman.dir="$dir" --rerun
