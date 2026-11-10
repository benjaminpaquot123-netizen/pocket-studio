#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 7): the merge agent's script. Merges the teams' branches into the integration
# workshop ONE AT A TIME, in the order given (smallest and most central first), each followed by its checks:
#   scripts/bench/fusion-equipes.sh <tour-N> <zone> [<zone> …] [-- <gradle test filters…>]
# For each zone: the fresh review of its branch must exist (docs/audits/ultra/<tour-N>-<zone>-relecture.md, written
# BEFORE the merge: a review after the push came too late in rounds 1-3), committed in the branch, ending on the line
# « Verdict : 0 bloquant, 0 grave » with no open blocking or grave row (scripts/bench/relecture-verdict.py), then
# `git merge --no-ff eq/<tour-N>-<zone>`, then the bench's self-test on the merged code, the replay tests of fixed
# defects (scripts/bench/rejeu-defauts.sh, never none) and the team's targeted tests through the one Gradle queue. A red step stops everything: the merge is undone (git merge --abort / reset to the commit before)
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
  # shellcheck disable=SC2086
  why="$(printf '%s\n' "$text" | python3 -I scripts/bench/relecture-verdict.py $removed)" \
    || { echo "$zone: review refused: $why: not merged." >&2; exit 3; }
  # Only this script writes the portal's traces (D1): a branch never brings or changes one.
  touched="$(git diff --name-only "$base" "eq/$tour-$zone" -- "$traces_root")"
  [ -z "$touched" ] || { echo "$zone: the branch touches the portal's traces: $touched: not merged." >&2; exit 3; }
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
  if ! scripts/bench/selftest.sh "$lot" > "$lot/selftest.log" 2>&1 || ! env -u PS_REJEUX_DIR scripts/bench/rejeu-defauts.sh \
      || { [ ${#filters[@]} -gt 0 ] && ! scripts/bench/gradle-file.sh "${args[@]}"; }; then
    git reset --keep "$before"
    echo "$zone: red after the merge (self-test, replays or tests; self-test kept in $lot): undone." >&2; exit 5
  fi
  echo "$zone: merged ($(git rev-parse --short HEAD))"
done
