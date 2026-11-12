#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 5): turns a tester's seat into a replay test of the defect it found, BEFORE the fix.
#   scripts/bench/rejeu-nouveau.sh <seat folder> <n> <id> 'attendu=<text the screen must show after gesture n>' ['absent=<text it must not>']
# Writes docs/audits/ultra/rejeux/<id>/ : the seat's start.txt (its options: language, device, font, mode, fresh…),
# its gestures cmd-1..n and oracle.txt. scripts/bench/rejeu-defauts.sh plays every such folder; the fix is done when
# its replay says « oracle tenu », and it must say « oracle non tenu » before the fix (red before, green after).
# <id> names the round and the item: t4-26-clavier, t5-03-…
# Banc fiable 1.5: a defect not fixed yet waits in docs/audits/ultra/rejeux-attente/ (PS_REJEUX_DIR=…): rejeu-defauts.sh
# plays rejeux/ before every merge and must find it held there; the team moves it to rejeux/ with its fix.
set -euo pipefail
src="$(cd "${1:?usage: rejeu-nouveau.sh <seat folder> <n> <id> attendu=… [absent=…]}" && pwd)"
n="${2:?n}"; id="${3:?id}"; shift 3
root="$(cd "$(dirname "$0")/../.." && pwd)"
out="${PS_REJEUX_DIR:-$root/docs/audits/ultra/rejeux}/$id"
[ -e "$out" ] && { echo "$out exists already." >&2; exit 2; }
[ -f "$src/start.txt" ] || { echo "No start.txt in $src." >&2; exit 2; }
# External audit: the oracle is checked before anything is written (a bad option left a half-made folder behind).
for kv in "$@"; do case "$kv" in attendu=*|absent=*) ;; *) echo "unknown: $kv (attendu=… or absent=…)" >&2; exit 2 ;; esac; done
[ $# -gt 0 ] || { echo "An oracle is needed: attendu=… or absent=…" >&2; exit 2; }
mkdir -p "$out"
trap '[ -f "$out/oracle.txt" ] || rm -rf "${out:?}"' EXIT
grep -v -E '^(rejeu|rejeu\.[a-z]+|timeout\.minutes)=' "$src/start.txt" > "$out/start.txt" || true
# Tour 5 (rejeux6): a seat with a journal is rewritten by test tag (« touche <étiquette> »), never copied as pixels: a touch whose
# finger saw no tag stops here and is given with PS_REJEU_OPTIONS="--cible N=TAG --saute N --glisse N=TAG:DX:DY".
if [ -f "$src/journal.txt" ] && grep -q '^#[0-9]* DOIGT ' "$src/journal.txt"; then
  # shellcheck disable=SC2086
  python3 -I "$root/scripts/bench/rejeu-etiquettes.py" "$src" "$n" "$out" ${PS_REJEU_OPTIONS:-} \
    || { echo "Gestures with no tag under the finger: see above." >&2; rm -rf "${out:?}"; exit 2; }
else
for i in $(seq 1 "$n"); do
  if [ -f "$src/cmd-$i.txt" ]; then
    cp "$src/cmd-$i.txt" "$out/"
  else
    # Banc fiable 1.5: a seat kept in the repository has its journal only (« #i > touche 308 457 »): the gesture from there.
    cmd="$(sed -n "s/^#$i > //p" "$src/journal.txt" 2>/dev/null | head -1)"
    [ -n "$cmd" ] || { echo "No cmd-$i.txt and no « #$i > … » line in $src/journal.txt." >&2; rm -rf "${out:?}"; exit 2; }
    printf '%s' "$cmd" > "$out/cmd-$i.txt"
  fi
done
fi
{
  echo "source=${src#"$root"/}"
  echo "rejeu.n=$n"
  for kv in "$@"; do
    case "$kv" in attendu=*) echo "rejeu.attendu=${kv#attendu=}" ;; absent=*) echo "rejeu.absent=${kv#absent=}" ;; *) echo "unknown: $kv" >&2; exit 2 ;; esac
  done
} > "$out/oracle.txt"
grep -q '^rejeu\.\(attendu\|absent\)=' "$out/oracle.txt" || { echo "An oracle is needed: attendu=… or absent=…" >&2; rm -rf "${out:?}"; exit 2; }
echo "Replay test written: $out"
