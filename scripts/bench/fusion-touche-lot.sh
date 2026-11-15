#!/usr/bin/env bash
# METHODE-V4 §4.2 point 3 (« ne plus courir après la tête »): does merging the integration into a lot touch the lot?
#   scripts/bench/fusion-touche-lot.sh <lot-branch> [<integration ref>]      (default ref: origin/main)
# The lot's base is its merge base with the integration. The files the integration changed since that base are
# compared with the files the lot changed since it. « NON »: the lot's review and seat stay valid without merging the
# integration in (exit 0). « OUI » and the files both changed, one per line (exit 1). Exit 2: bad usage. Reads only.
set -uo pipefail
lot="${1:-}"; ref="${2:-origin/main}"
[ -n "$lot" ] || { echo "usage: scripts/bench/fusion-touche-lot.sh <lot-branch> [<integration ref>]" >&2; exit 2; }
for r in "$lot" "$ref"; do
  git rev-parse -q --verify "$r^{commit}" >/dev/null || { echo "fusion-touche-lot: $r is not a commit" >&2; exit 2; }
done
base="$(git merge-base "$lot" "$ref")" || { echo "fusion-touche-lot: $lot and $ref have no common ancestor" >&2; exit 2; }
# Renames count on both sides (old and new name): a file the lot changes, moved by the integration, is touched.
names() { git diff --name-status -M "$base" "$1" | awk -F'\t' '{for (i = 2; i <= NF; i++) print $i}' | sort -u; }
both="$(comm -12 <(names "$lot") <(names "$ref"))"
if [ -z "$both" ]; then
  echo "NON : la fusion de $ref ($(git rev-list --count "$base..$ref") commit(s) depuis $(git rev-parse --short "$base")) ne touche aucun fichier du lot"
  exit 0
fi
echo "OUI : $(printf '%s\n' "$both" | grep -c .) fichier(s) du lot changé(s) aussi par $ref depuis $(git rev-parse --short "$base")"
printf '%s\n' "$both"
exit 1
