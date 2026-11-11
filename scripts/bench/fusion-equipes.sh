#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 7): the merge agent's script. Merges the teams' branches into the integration
# workshop ONE AT A TIME, in the order given (smallest and most central first), each followed by its checks:
#   scripts/bench/fusion-equipes.sh <tour-N> <zone> [<zone> …] [-- <gradle test filters…>]
# For each zone: the fresh review of its branch must exist (docs/audits/ultra/<tour-N>-<zone>-relecture.md, written
# BEFORE the merge: a review after the push came too late in rounds 1-3), committed in the branch, ending on the line
# « Verdict : 0 bloquant, 0 grave » with no open blocking or grave row (scripts/bench/relecture-verdict.py), then
# `git merge --no-ff eq/<tour-N>-<zone>`, then the bench's self-test on the merged code, the replay tests of fixed
# defects (scripts/bench/rejeu-defauts.sh, never none) and the team's targeted tests through the one Gradle queue; then
# the portal's traces of the merge, committed (docs/audits/ultra/portail/<date>-<tour>-<zone>/). A red step stops everything: the merge is undone (git merge --abort / reset to the commit before)
# and the zone is named. Never pushes.
set -euo pipefail
int="$(cd "$(dirname "$0")/../.." && pwd)"
tour="${1:?tour-N}"; shift
zones=(); filters=()
while [ $# -gt 0 ]; do
  if [ "$1" = "--" ]; then shift; filters=("$@"); break; fi
  zones+=("$1"); shift
done
cd "$int"
traces_root="docs/audits/ultra/portail"
# Socle du banc, point 5: the rules are frozen per batch of merges. The batch in progress is in portail-paquet.txt; a
# rule added while batch N is open is written « from_paquet N+1 … » ON ITS refusal line, and only applies from the next
# batch (scripts/bench/test_socle.py checks it). Only the main discussion moves the number, when it opens a batch.
# (No batch file: batch 1. « || true »: a failing substitution would end this script silently under set -e.)
paquet="$(sed -n 's/^paquet: *\([0-9][0-9]*\).*/\1/p' scripts/bench/portail-paquet.txt 2>/dev/null | head -1 || true)"
from_paquet() { [ "${paquet:-1}" -ge "$1" ]; }
# The replay tests of a commit: the folders of docs/audits/ultra/rejeux/ (« _… » are kept failures, not tests).
replays() { git ls-tree --name-only "$1:docs/audits/ultra/rejeux" 2>/dev/null | grep -v '^_' | sort || true; }
[ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T')" ] || { echo "The integration workshop has uncommitted changes: commit or set them aside first." >&2; exit 2; }
for zone in "${zones[@]}"; do
  review="docs/audits/ultra/$tour-$zone-relecture.md"
  # Audit « banc crédible » 2, P4: the review is read in the branch only (a file left in the workshop, never committed,
  # was accepted and then missing from the history).
  text="$(git show "eq/$tour-$zone:$review" 2>/dev/null)" || true
  [ -n "$text" ] || { echo "$zone: no fresh review ($review) committed in eq/$tour-$zone: not merged." >&2; exit 3; }
  # PLAN-RESTE 4.0: the review names the commit it read (« relu : <sha> ») and nothing but the review itself changed
  # after it; its verdict says 0 blocking and 0 grave.
  read_sha="$(printf '%s\n' "$text" | sed -n 's/^.*[Rr]elu *: *\([0-9a-f]\{7,40\}\).*$/\1/p' | head -1)"
  [ -n "$read_sha" ] && git cat-file -e "$read_sha^{commit}" 2>/dev/null \
    || { echo "$zone: the review does not name the commit it read (« relu : <sha> »): not merged." >&2; exit 3; }
  # (A file changed after the review only because the team merged fx-int in, i.e. now equal to fx-int's, is not the team's.)
  after=""
  for f in $(git diff --name-only "$read_sha" "eq/$tour-$zone" | grep -vxF "$review" || true); do
    git diff --quiet HEAD "eq/$tour-$zone" -- "$f" || after="$after $f"
  done
  [ -z "$after" ] || { echo "$zone: changed after the review ($read_sha): $after: not merged." >&2; exit 3; }
  # Audit « banc crédible » 2, P1: the verdict is the review's LAST line, no table row « bloquant »/« grave » is left
  # without « corrigé », and each replay the branch takes out of rejeux/ is named with its reason (relecture-verdict.py).
  base="$(git merge-base HEAD "eq/$tour-$zone")"
  removed="$(comm -23 <(replays "$base") <(replays "eq/$tour-$zone"))"
  # Review of the « portail » lot (M5): a replay kept but whose files the branch changes (its oracle, its gestures)
  # needs « rejeu changé : <id> : <raison> » too.
  changed="$(comm -12 <(replays "$base") <(replays "eq/$tour-$zone") | while read -r id; do
    git diff --quiet "$base" "eq/$tour-$zone" -- "docs/audits/ultra/rejeux/$id" || echo "changé:$id"; done)"
  # Second review of the « portail » lot (G2): red evidence (rouges-constates/<sha>/) the branch brings or changes is
  # named in its review (« rouge constaté : <sha> : rejoué sur <parent> : … »; third review, M9: the parent passed).
  evidence="$(git diff --name-only "$base" "eq/$tour-$zone" -- docs/audits/ultra/rouges-constates \
    | sed -n 's#^docs/audits/ultra/rouges-constates/\([^/]*\)/.*#\1#p' | sort -u | while read -r sha; do
      echo "constat:$sha:$(git rev-parse -q --verify "$sha^1" 2>/dev/null || true)"; done)"
  # shellcheck disable=SC2086
  why="$(printf '%s\n' "$text" | python3 -I scripts/bench/relecture-verdict.py $removed $changed $evidence)" \
    || { echo "$zone: review refused: $why: not merged." >&2; exit 3; }
  # Only this script writes the portal's traces (D1): a branch never brings or changes one.
  touched="$(git diff --name-only "$base" "eq/$tour-$zone" -- "$traces_root")"
  [ -z "$touched" ] || { echo "$zone: the branch touches the portal's traces: $touched: not merged." >&2; exit 3; }
  # Socle du banc, point 2: a lot writes its decisions in docs/decisions/<tour-N>-<zone>.md only (never in
  # docs/DECISIONS.md, which the portal alone extends, nor in another lot's file).
  decisions="$(git diff --name-only "$base" "eq/$tour-$zone" -- docs/DECISIONS.md docs/decisions | grep -vxF "docs/decisions/$tour-$zone.md" | tr '\n' ' ' || true)"
  if from_paquet 2 && [ -n "$decisions" ]; then echo "$zone: decisions written outside docs/decisions/$tour-$zone.md (docs/DECISIONS.md or another lot's file): $decisions: not merged." >&2; exit 3; fi
  # Socle du banc (review, moyen 3): only the main discussion moves the batch, when it opens one.
  moved_batch="$(git diff --name-only "$base" "eq/$tour-$zone" -- scripts/bench/portail-paquet.txt)"
  if from_paquet 2 && [ -n "$moved_batch" ]; then echo "$zone: the branch moves the batch (scripts/bench/portail-paquet.txt): not merged." >&2; exit 3; fi
  # PLAN-RESTE 7.1: a team branch never brings a symbolic link (a third-party checkout linked in, a secret linked out).
  links="$(git ls-tree -r "eq/$tour-$zone" | awk '$1 == "120000" {print $4}')"
  [ -z "$links" ] || { echo "$zone: symbolic link(s) in the branch: $links: not merged." >&2; exit 6; }
  before="$(git rev-parse HEAD)"
  if ! git merge --no-ff -m "Merge team $zone of $tour (audit ultra)" "eq/$tour-$zone"; then
    git merge --abort || true
    echo "$zone: merge conflict: undone. The team rebases on fx-int and comes back." >&2; exit 4
  fi
  # Audit « banc crédible » 2, P1: a merge without any replay test is refused (moving the one replay out of rejeux/
  # took the self-test away with it).
  if [ -z "$(replays HEAD)" ]; then
    git reset --keep "$before"
    echo "$zone: no replay test left in docs/audits/ultra/rejeux/ after the merge: undone." >&2; exit 5
  fi
  args=(:app:testDebugUnitTest)
  for f in "${filters[@]}"; do args+=(--tests "$f"); done
  # The self-test (every Bench*, Tour*AppProof*, Tour*IdleTest and @KnownDefect class, the bench's scripts, the judge)
  # always runs on the merged code, on a lot of its own; then the repository's replays (never PS_REJEUX_DIR's), then
  # the team's filters.
  lot="$(mktemp -d)"
  green=1
  scripts/bench/selftest.sh "$lot" > "$lot/selftest.log" 2>&1 || green=0
  if [ "$green" -eq 1 ]; then
    env -u PS_REJEUX_DIR scripts/bench/rejeu-defauts.sh > "$lot/rejeux.txt" 2>&1 || green=0
    cat "$lot/rejeux.txt"
  fi
  if [ "$green" -eq 1 ] && [ ${#filters[@]} -gt 0 ]; then scripts/bench/gradle-file.sh "${args[@]}" || green=0; fi
  if [ "$green" -eq 0 ]; then
    git reset --keep "$before"
    echo "$zone: red after the merge (self-test, replays or tests; kept in $lot): undone." >&2; exit 5
  fi
  # Audit « banc crédible » 2, D1: the portal's traces of this merge (the self-test's summary, stamp and judge, and
  # fusion.txt) are committed right after it, so every figure quoted later can be checked; verifie-chantier.py counts
  # « fusion sans trace du portail » for a merge without them.
  merged="$(git rev-parse HEAD)"
  # Review of the « portail » lot (M2): any step that fails from here to the traces' commit undoes the merge too, and
  # leaves no half-written traces.
  trace=""
  undo_merge() {
    [ -z "$trace" ] || rm -rf "${trace:?}"
    # From HEAD, not the index: a failed traces' commit after « git add » has the index's line staged already.
    git checkout -q HEAD -- docs/DECISIONS.md 2>/dev/null || true
    git reset -q --keep "$before"
    echo "$zone: a step after the checks failed (traces of the portal): undone (kept in $lot)." >&2
    exit 5
  }
  trap undo_merge ERR
  trace="$traces_root/$(date -u +%F)-$tour-$zone"
  n=2
  while [ -e "$trace" ]; do trace="$traces_root/$(date -u +%F)-$tour-$zone-$n"; n=$((n + 1)); done
  mkdir -p "$trace"
  cp "$lot/selftest-resume.txt" "$lot/selftest.ok" "$lot/selftest-chantier.txt" "$trace/"
  { echo "fusion: $merged"; echo "branche: eq/$tour-$zone"; echo "tete: $(git rev-parse "eq/$tour-$zone")"
    echo "relecture: $review"; echo "relu: $read_sha"; [ ${#filters[@]} -eq 0 ] || echo "filtres: ${filters[*]}"
    echo "rejeux :"; sed 's/^/  /' "$lot/rejeux.txt"; } > "$trace/fusion.txt"
  # The judge again, with the traces there (the self-test's ran before them and counted this merge « sans trace »);
  # it reads the lot's summary, sealed with its JUnit results (the trace keeps no XML).
  python3 -I scripts/bench/verifie-chantier.py --resume "$lot/selftest-resume.txt" > "$trace/selftest-chantier.txt" 2>&1 || true
  # Socle du banc, point 2: the lot's decisions file, listed once at the end of docs/DECISIONS.md.
  scripts/bench/decisions-index.sh "$tour" "$zone"
  index=(); [ ! -f docs/DECISIONS.md ] || index=(docs/DECISIONS.md)
  git add -- "$trace" "${index[@]}"
  git commit -q -m "Portal traces of the merge of $zone ($tour)" -- "$trace" "${index[@]}"
  trap - ERR
  rm -rf "${lot:?}"
  echo "$zone: merged ($(git rev-parse --short "$merged")), traces in $trace"
done
