#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 5): plays the replay tests of fixed defects, before every merge of a team's branch.
#   scripts/bench/rejeu-defauts.sh [<id> …]      (all of docs/audits/ultra/rejeux/ when none is given)
# Six at a time (one Gradle run, seats a-f), through the one Gradle queue. Prints one line per test:
# « <id> : oracle tenu » or « <id> : oracle non tenu … »; exits 1 when one is not held.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
dir="$root/docs/audits/ultra/rejeux"
if [ $# -gt 0 ]; then ids=("$@"); else mapfile -t ids < <(find "$dir" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort); fi
[ ${#ids[@]} -gt 0 ] || { echo "No replay test in $dir."; exit 0; }
work="$(mktemp -d)"
trap 'rm -rf "${work:?}"' EXIT
failed=0
# Banc fiable 1.2: the replays go through the self-test gate like any lot (no default bypass any more), once for all.
mkdir -p "$work/gate"
"$root/scripts/bench/selftest.sh" "$work/gate" > "$work/gate/selftest.log" 2>&1 || {
  echo "selftest rouge : rejeux non joués"; cat "$work/gate/selftest-resume.txt" 2>/dev/null || tail -5 "$work/gate/selftest.log"; exit 1; }
seats=(a b c d e f)
for ((start = 0; start < ${#ids[@]}; start += 6)); do
  run="$work/run-$start"
  mkdir -p "$run"
  for ((k = 0; k < 6 && start + k < ${#ids[@]}; k++)); do
    id="${ids[start + k]}"; s="${seats[k]}"
    mkdir -p "$run/$s"
    { cat "$dir/$id/start.txt"; echo "rejeu=$dir/$id"; grep -v '^source=' "$dir/$id/oracle.txt"; } > "$run/$s/start.txt"
    echo "$id" > "$run/$s/id.txt"
  done
  cp "$work/gate/selftest.ok" "$run/"
  "$root/scripts/human-tester.sh" "$run" > "$run/gradle.log" 2>&1 || true
  for s in "${seats[@]}"; do
    [ -f "$run/$s/id.txt" ] || continue
    id="$(cat "$run/$s/id.txt")"
    verdict="$(cat "$run/$s/rejeu.txt" 2>/dev/null || echo "pas de verdict (banc)")"
    echo "$id : $verdict"
    [ "$verdict" = "oracle tenu" ] || failed=1
  done
done
exit "$failed"
