#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 7): the merge agent's script. Merges the teams' branches into the integration
# workshop ONE AT A TIME, in the order given (smallest and most central first), each followed by its checks:
#   scripts/bench/fusion-equipes.sh <tour-N> <zone> [<zone> …] [-- <gradle test filters…>]
# For each zone: the fresh review of its branch must exist (docs/audits/ultra/<tour-N>-<zone>-relecture.md, written
# BEFORE the merge: a review after the push came too late in rounds 1-3), then `git merge --no-ff eq/<tour-N>-<zone>`,
# then the replay tests of fixed defects (scripts/bench/rejeu-defauts.sh) and the team's targeted tests through the
# one Gradle queue. A red step stops everything: the merge is undone (git merge --abort / reset to the commit before)
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
[ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T')" ] || { echo "The integration workshop has uncommitted changes: commit or set them aside first." >&2; exit 2; }
for zone in "${zones[@]}"; do
  review="docs/audits/ultra/$tour-$zone-relecture.md"
  text="$(git show "eq/$tour-$zone:$review" 2>/dev/null || cat "$review" 2>/dev/null)" || true
  [ -n "$text" ] || { echo "$zone: no fresh review ($review): not merged." >&2; exit 3; }
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
  printf '%s\n' "$text" | grep -Eq 'Verdict *: *0 bloquant, *0 grave' \
    || { echo "$zone: the review's verdict is not « 0 bloquant, 0 grave »: not merged." >&2; exit 3; }
  # PLAN-RESTE 7.1: a team branch never brings a symbolic link (a third-party checkout linked in, a secret linked out).
  links="$(git ls-tree -r "eq/$tour-$zone" | awk '$1 == "120000" {print $4}')"
  [ -z "$links" ] || { echo "$zone: symbolic link(s) in the branch: $links: not merged." >&2; exit 6; }
  before="$(git rev-parse HEAD)"
  if ! git merge --no-ff -m "Merge team $zone of $tour (audit ultra)" "eq/$tour-$zone"; then
    git merge --abort || true
    echo "$zone: merge conflict: undone. The team rebases on fx-int and comes back." >&2; exit 4
  fi
  args=(:app:testDebugUnitTest)
  for f in "${filters[@]}"; do args+=(--tests "$f"); done
  if ! scripts/bench/rejeu-defauts.sh || { [ ${#filters[@]} -gt 0 ] && ! scripts/bench/gradle-file.sh "${args[@]}"; }; then
    git reset --keep "$before"
    echo "$zone: red after the merge (replays or tests): undone." >&2; exit 5
  fi
  echo "$zone: merged ($(git rev-parse --short HEAD))"
done
