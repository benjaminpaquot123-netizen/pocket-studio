#!/usr/bin/env bash
# Test of scripts/bench/fusion-touche-lot.sh: a small repository, an integration that moves, a lot. Seconds.
#   scripts/bench/test_fusion_touche_lot.sh
set -uo pipefail
src="$(cd "$(dirname "$0")/../.." && pwd)"
t="$(mktemp -d)"
trap 'rm -rf "${t:?}"' EXIT
cd "$t" || exit 2
git init -q -b main .
git config user.name essai
git config user.email essai@localhost
s="$src/scripts/bench/fusion-touche-lot.sh"
for f in a b c d; do echo "$f" > "$f.txt"; done
git add a.txt b.txt c.txt d.txt; git commit -q -m base
git checkout -q -b lot; echo lot >> a.txt; echo lot >> d.txt; git add a.txt d.txt; git commit -q -m "lot work"
reds=0
check() { # <name> <expected exit> <pattern> [<ref>]
  local out rc
  out="$(bash "$s" lot ${4:-main} 2>&1)"; rc=$?
  if [ "$rc" -eq "$2" ] && printf '%s\n' "$out" | grep -qE -- "$3"; then echo "ok    $1"
  else reds=$((reds + 1)); echo "ROUGE $1 (exit $rc)"; printf '%s\n' "$out" | sed 's/^/    /'; fi
}
git checkout -q main
check "rien de neuf sur main : NON" 0 '^NON : .*0 commit'
echo main >> b.txt; git add b.txt; git commit -q -m "other file"
check "main change un autre fichier : NON" 0 '^NON : .*1 commit'
echo main >> a.txt; git add a.txt; git commit -q -m "same file"
check "main change un fichier du lot : OUI" 1 '^OUI : 1 fichier'
check "  … et le nomme" 1 '^a\.txt$'
git mv d.txt e.txt; git commit -q -m rename
check "main déplace un fichier du lot : OUI" 1 '^d\.txt$'
git checkout -q lot; git merge -q --no-edit main >/dev/null 2>&1 || { git checkout -q --theirs . ; git add a.txt e.txt 2>/dev/null; git commit -q --no-edit; }
git checkout -q main
check "après la fusion de main dans le lot : NON" 0 '^NON'
out="$(bash "$s" 2>&1)"; [ $? -eq 2 ] || { reds=$((reds + 1)); echo "ROUGE sans lot : pas exit 2"; }
out="$(bash "$s" nulle-part main 2>&1)"; [ $? -eq 2 ] || { reds=$((reds + 1)); echo "ROUGE lot inconnu : pas exit 2"; }
echo
[ "$reds" -eq 0 ] && { echo "test_fusion_touche_lot : tout vert"; exit 0; }
echo "test_fusion_touche_lot : $reds rouge(s)"; exit 1
