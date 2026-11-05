#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 5): turns a tester's seat into a replay test of the defect it found, BEFORE the fix.
#   scripts/bench/rejeu-nouveau.sh <seat folder> <n> <id> 'attendu=<text the screen must show after gesture n>' ['absent=<text it must not>']
# Writes docs/audits/ultra/rejeux/<id>/ : the seat's start.txt (its options: language, device, font, mode, fresh…),
# its gestures cmd-1..n and oracle.txt. scripts/bench/rejeu-defauts.sh plays every such folder; the fix is done when
# its replay says « oracle tenu », and it must say « oracle non tenu » before the fix (red before, green after).
# <id> names the round and the item: t4-26-clavier, t5-03-…
set -euo pipefail
src="$(cd "${1:?usage: rejeu-nouveau.sh <seat folder> <n> <id> attendu=… [absent=…]}" && pwd)"
n="${2:?n}"; id="${3:?id}"; shift 3
root="$(cd "$(dirname "$0")/../.." && pwd)"
out="$root/docs/audits/ultra/rejeux/$id"
[ -e "$out" ] && { echo "$out exists already." >&2; exit 2; }
[ -f "$src/start.txt" ] || { echo "No start.txt in $src." >&2; exit 2; }
mkdir -p "$out"
grep -v -E '^(rejeu|rejeu\.[a-z]+|timeout\.minutes)=' "$src/start.txt" > "$out/start.txt" || true
for i in $(seq 1 "$n"); do
  [ -f "$src/cmd-$i.txt" ] || { echo "No cmd-$i.txt in $src." >&2; rm -rf "${out:?}"; exit 2; }
  cp "$src/cmd-$i.txt" "$out/"
done
{
  echo "source=${src#"$root"/}"
  echo "rejeu.n=$n"
  for kv in "$@"; do
    case "$kv" in attendu=*) echo "rejeu.attendu=${kv#attendu=}" ;; absent=*) echo "rejeu.absent=${kv#absent=}" ;; *) echo "unknown: $kv" >&2; exit 2 ;; esac
  done
} > "$out/oracle.txt"
grep -q '^rejeu\.\(attendu\|absent\)=' "$out/oracle.txt" || { echo "An oracle is needed: attendu=… or absent=…" >&2; rm -rf "${out:?}"; exit 2; }
echo "Replay test written: $out"
