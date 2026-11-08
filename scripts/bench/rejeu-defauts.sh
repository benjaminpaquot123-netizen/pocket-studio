#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 5): plays the replay tests of fixed defects, before every merge of a team's branch.
#   scripts/bench/rejeu-defauts.sh [<id> …]      (all of docs/audits/ultra/rejeux/ when none is given)
# Six at a time (one Gradle run, seats a-f), through the one Gradle queue. Prints one line per test:
# « <id> : oracle tenu » or « <id> : oracle non tenu … »; exits 1 when one is not held.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
dir="${PS_REJEUX_DIR:-$root/docs/audits/ultra/rejeux}"
if [ $# -gt 0 ]; then ids=("$@"); else mapfile -t ids < <(find "$dir" -mindepth 1 -maxdepth 1 -type d ! -name '_*' -printf '%f\n' | sort); fi
[ ${#ids[@]} -gt 0 ] || { echo "No replay test in $dir."; exit 0; }
work="$(mktemp -d)"
trap 'rm -rf "${work:?}"' EXIT
failed=0
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
  # Banc fiable 1.2 and the external audit: each batch of replays is a lot, gated like any lot on ITS seats (a stamp
  # names its lot: one copied from another folder is refused).
  "$root/scripts/bench/selftest.sh" "$run" > "$run/selftest.log" 2>&1 || {
    echo "selftest rouge : rejeux non joués"; cat "$run/selftest-resume.txt" 2>/dev/null || tail -5 "$run/selftest.log"; exit 1; }
  "$root/scripts/human-tester.sh" "$run" > "$run/gradle.log" 2>&1 || true
  for s in "${seats[@]}"; do
    [ -f "$run/$s/id.txt" ] || continue
    id="$(cat "$run/$s/id.txt")"
    verdict="$(cat "$run/$s/rejeu.txt" 2>/dev/null || echo "pas de verdict (banc)")"
    echo "$id : $verdict"
    if [ "$verdict" != "oracle tenu" ]; then
      failed=1
      # Banc fiable 1.5: a red replay is kept (its journal, screens as text, verdict) and its end shown.
      # (Not for the waiting replays of rejeux-attente/: their « non tenu » is the expected one.)
      [ "$(basename "$dir")" != "rejeux-attente" ] || continue
      keep="$dir/_echecs/$(date +%F)/$id"
      mkdir -p "$keep"
      cp "$run/$s/"*.txt "$keep/" 2>/dev/null || true
      # No journal: the seat never started; the bench's own log says why.
      [ -f "$run/$s/journal.txt" ] || tail -40 "$run/gradle.log" > "$keep/gradle-fin.log" 2>/dev/null || true
      echo "  gardé : ${keep#"$root"/}"
      tail -10 "$run/$s/journal.txt" 2>/dev/null | sed 's/^/  | /'
    fi
  done
done
exit "$failed"
