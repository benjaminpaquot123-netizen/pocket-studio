#!/usr/bin/env bash
# Replays a seat of the « testeur humain » bench (Audit ultra §4: « reproduit = le défaut existe, zéro agent »):
#   scripts/human-replay.sh <seat folder> <n>
# A fresh seat with the same start.txt plays the seat's cmd-1..n (rejeu=, rejeu.n=); after each gesture its
# screen (screen-N.txt, as a fingerprint: clock times and percentages aside) is compared with the seat's.
# Prints « reproduit » or « divergé au pas k ». The new seat is <seat folder>/../rejeu-<seat>-<n>-<time>/<seat>/.
set -euo pipefail
src="$(cd "${1:?usage: scripts/human-replay.sh <seat folder> <n>}" && pwd)"
n="${2:?usage: scripts/human-replay.sh <seat folder> <n>}"
seat="$(basename "$src")"
case "$seat" in a|b|c|d|e|f) ;; *) echo "A seat is a folder named a to f (here: $seat)." >&2; exit 2 ;; esac
[ -f "$src/screen-1.txt" ] || { echo "No screen-1.txt in $src: the seat was not played with the bench 2." >&2; exit 2; }
out="$(dirname "$src")/rejeu-$seat-$n-$(date +%H%M%S)"
mkdir -p "$out/$seat"
grep -v -E '^(rejeu|rejeu\.n)=' "$src/start.txt" > "$out/$seat/start.txt" || true
printf 'rejeu=%s\nrejeu.n=%s\n' "$src" "$n" >> "$out/$seat/start.txt"
"$(dirname "$0")/human-tester.sh" "$out" > "$out/gradle.log" 2>&1 || true
if [ -f "$out/$seat/rejeu.txt" ]; then
  cat "$out/$seat/rejeu.txt"; echo
else
  echo "pas de verdict (voir $out/gradle.log)"
  exit 1
fi
