#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 5): plays the replay tests of fixed defects, before every merge of a team's branch.
#   scripts/bench/rejeu-defauts.sh [<id> …]      (all of docs/audits/ultra/rejeux/ when none is given)
# Six at a time (one Gradle run, seats a-f), through the one Gradle queue. Prints one line per test:
# « <id> : oracle tenu » or « <id> : oracle non tenu … »; exits 1 when one is not held.
# PS_REJEUX_SELFTEST=<folder>: the first batch's self-test is copied there (fusion-equipes.sh, PLAN-RESTE 7.7).
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
    # PLAN-RESTE 7.5: the seat the replay was made from (oracle.txt's « source=<folder> (…) »), for a verdict that says
    # what differs from it (the last different screen, a button no longer under the finger).
    src="$(sed -n 's/^source=//p' "$dir/$id/oracle.txt" | head -1 | sed 's/ (.*//')"
    case "$src" in /*) ;; ?*) src="$root/$src" ;; esac
    { cat "$dir/$id/start.txt"; echo "rejeu=$dir/$id"; if [ -n "$src" ] && [ -d "$src" ]; then echo "rejeu.origine=$src"; fi
      grep -v '^source=' "$dir/$id/oracle.txt"; } > "$run/$s/start.txt"
    echo "$id" > "$run/$s/id.txt"
  done
  # Banc fiable 1.2 and the external audit: each batch of replays is a lot, gated like any lot on ITS seats (a stamp
  # names its lot: one copied from another folder is refused).
  "$root/scripts/bench/selftest.sh" "$run" > "$run/selftest.log" 2>&1 || {
    echo "selftest rouge : rejeux non joués"; cat "$run/selftest-resume.txt" 2>/dev/null || tail -5 "$run/selftest.log"
    # Dry run 7.6: the bench's scripts' own log goes with the lot (removed on exit): its failures are shown here.
    if [ -f "$run/selftest-scripts.log" ]; then grep -E '^(FAIL|Traceback|AssertionError)' "$run/selftest-scripts.log" | head -5 || true; fi
    exit 1; }
  # PLAN-RESTE 7.7: the first batch's self-test (summary, stamp, judge, JUnit results: all a sealed summary needs) is
  # left in $PS_REJEUX_SELFTEST: the portal keeps it as its own instead of running the Bench* tests a second time.
  if [ "$start" -eq 0 ] && [ -n "${PS_REJEUX_SELFTEST:-}" ]; then
    cp -r "$run"/selftest* "$run/junit" "${PS_REJEUX_SELFTEST:?}/"
  fi
  "$root/scripts/human-tester.sh" "$run" > "$run/gradle.log" 2>&1 || true
  for s in "${seats[@]}"; do
    [ -f "$run/$s/id.txt" ] || continue
    id="$(cat "$run/$s/id.txt")"
    verdict="$(cat "$run/$s/rejeu.txt" 2>/dev/null || echo "pas de verdict (banc)")"
    # Tour 5 (rejeux6): the journal of every run is kept, « tenu » or not (the zone report and the next tour read it), and a « tenu »
    # needs its proof: the last control the replay aims at by a tag was really touched (« #n ETIQUETTE <tag> » in the journal, and not
    # refused: « #n REFUS »). A touch that landed nowhere cannot pass as « tenu ».
    keepj="$dir/_journaux/$id"
    rm -rf "${keepj:?}"; mkdir -p "$keepj"
    cp "$run/$s/journal.txt" "$run/$s/rejeu.txt" "$run/$s/start.txt" "$keepj/" 2>/dev/null || true
    if [ "$verdict" = "oracle tenu" ]; then
      last=""; lasttag=""
      for f in $(ls "$dir/$id"/cmd-*.txt 2>/dev/null | sed 's/.*cmd-\([0-9]*\)\.txt/\1 &/' | sort -n | cut -d' ' -f2); do
        read -r verb arg rest < "$f" || true
        case "$verb" in touche|appui-long|double-touche|glisse) ;; *) continue ;; esac
        case "$arg" in ''|[0-9]*|-[0-9]*) continue ;; esac
        last="$(basename "$f" .txt)"; last="${last#cmd-}"; lasttag="$arg"
      done
      if [ -n "$lasttag" ]; then
        if ! grep -qF "#$last ETIQUETTE $lasttag :" "$run/$s/journal.txt" 2>/dev/null; then
          verdict="oracle non tenu : l'étiquette « $lasttag » (pas $last) n'a pas été touchée (journal sans « ETIQUETTE »)"
        elif grep -q "^#$last REFUS" "$run/$s/journal.txt"; then
          verdict="oracle non tenu : l'étiquette « $lasttag » (pas $last) a été refusée (hors de portée du pouce)"
        fi
      fi
    fi
    echo "$verdict" > "$keepj/verdict.txt"
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
