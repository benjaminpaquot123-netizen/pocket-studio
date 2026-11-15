#!/usr/bin/env bash
# Test of scripts/bench/pret.sh (METHODE-V4 astuce C): a small repository made here, one lot branch per case, what the
# script must say of each. Seconds, no Gradle (the lots' tests are Python; the self-test, replays and pre-flight are
# stand-ins that write down how they were called).
#   scripts/bench/test_pret.sh
set -uo pipefail
src="$(cd "$(dirname "$0")/../.." && pwd)"
t="$(mktemp -d)"
trap 'rm -rf "${t:?}"' EXIT
mkdir "$t/repo" && cd "$t/repo" || exit 2
git init -q -b fx-int .
git config user.name essai
git config user.email essai@localhost
mkdir -p scripts/bench docs/audits/ultra/rejeux/r1 docs/audits/ultra/rejeux/r2 app/src/main/java/a/tuto lib
cp "$src/scripts/bench/pret.sh" scripts/bench/
printf 'touche Lecture\n' > docs/audits/ultra/rejeux/r1/cmd-1.txt
printf 'touche Arrêt\n' > docs/audits/ultra/rejeux/r2/cmd-1.txt
printf 'fun label() = "Lecture"\n' > app/src/main/java/a/Screen.kt
printf 'def add(a, b):\n    return a - b\n' > lib/calc.py
echo "# titre" > README.md
git add scripts docs app lib README.md
git commit -q -m base

calls="$t/calls"
export PS_PRET_SELFTEST="$t/stub-selftest" PS_PRET_REJEU="$t/stub-rejeu" PS_PRET_PREVOL="$t/stub-prevol"
for s in selftest rejeu prevol; do printf '#!/bin/sh\necho "%s $*" >> "%s"\nexit 0\n' "$s" "$calls" > "$t/stub-$s"; chmod +x "$t/stub-$s"; done

reds=0
check() { # <name> <expected exit> <pattern that must be in the output> [<pattern that must not>]
  local out rc
  : > "$calls"
  out="$(bash scripts/bench/pret.sh fx-int ${LOT_ARGS:-} 2>&1)"; rc=$?
  if [ "$rc" -ne "$2" ] || ! printf '%s\n' "$out" | grep -qE -- "$3" || { [ -n "${4:-}" ] && printf '%s\n' "$out" | grep -qE -- "$4"; }; then
    reds=$((reds + 1)); echo "ROUGE $1 (exit $rc, attendu $2)"; printf '%s\n' "$out" | sed 's/^/    /'
  else
    echo "ok    $1"
  fi
}
lot() { git checkout -q -B "lot-$1" fx-int; }
commit() { git add "$@"; git commit -q -m "lot work"; }
test_file() { printf 'import sys\nsys.path.insert(0, "lib")\nfrom calc import add\nassert add(2, 2) == %s\n' "$1" > test_calc.py; }
fix() { printf 'def add(a, b):\n    return a + b\n' > lib/calc.py; }

# 1. A real red: the test fails on the parent, the fix makes it green.
lot red
test_file 4; fix; commit test_calc.py lib/calc.py
check "rouge prouvé, tests verts, niveau 2 : PRET" 0 '^PRET \(niveau 2\)' 'NON'
grep -q '^selftest ' "$calls" || { reds=$((reds + 1)); echo "ROUGE le self-test n'a pas été lancé au niveau 2"; }

# 2. The test already passes without the fix: no red proved.
lot nored
test_file 0; commit test_calc.py
check "test déjà vert sur le parent : PAS PRET" 1 '^PAS PRET : rouge non prouvé'

# 3. The test is red on HEAD too.
lot stillred
test_file 4; commit test_calc.py
check "test rouge sur la tête : PAS PRET" 1 'tests du lot pas verts'

# 4. Texts only: level 1, no self-test, no test needed.
lot texts
echo "notes" > docs/notes.md; commit docs/notes.md
check "textes seuls : PRET niveau 1" 0 '^PRET \(niveau 1\)'
! grep -q '^selftest ' "$calls" || { reds=$((reds + 1)); echo "ROUGE le self-test a tourné au niveau 1"; }

# 5. The tuto's mechanics: level 3, and with no test the red cannot be proved.
lot tuto
printf 'fun step() = 2\n' > app/src/main/java/a/tuto/Step.kt; commit app/src/main/java/a/tuto/Step.kt
check "mécanique du tuto sans test : niveau 3, PAS PRET" 1 'niveau 3 .*tuto' 'PRET \(niveau'
check "  … la raison est l'absence de test" 1 '^PAS PRET : aucun test ajouté'

# 6. A regex in src/main: level 3.
lot regex
printf 'val r = Regex("a+")\n' > app/src/main/java/a/Re.kt; test_file 4; fix
commit app/src/main/java/a/Re.kt test_calc.py lib/calc.py
check "regex : niveau 3" 0 '^PRET \(niveau 3\)'

# 7. Replays touched: the one whose tag the lot changes (r1 « Lecture »), not the other.
lot label
test_file 4; fix; printf 'fun label() = "Jouer"\n' > app/src/main/java/a/Screen.kt
commit app/src/main/java/a/Screen.kt test_calc.py lib/calc.py
check "libellé changé : rejeu touché joué" 0 'rejeux touchés tenus \(1 : r1\)'
grep -qx 'rejeu r1' "$calls" || { reds=$((reds + 1)); echo "ROUGE le rejeu r1 n'a pas été joué seul ($(cat "$calls"))"; }
printf '#!/bin/sh\necho "  r1 : oracle non tenu (geste 1)"\nexit 1\n' > "$t/stub-rejeu"
check "rejeu non tenu : PAS PRET" 1 '^PAS PRET : rejeu non tenu'
printf '#!/bin/sh\necho "rejeu $*" >> "%s"\nexit 0\n' "$calls" > "$t/stub-rejeu"

# 8. A symbolic link in the lot.
lot link
test_file 4; fix; ln -s README.md lien; commit test_calc.py lib/calc.py lien
check "lien symbolique : PAS PRET" 1 'lien symbolique'

# 9. A conflict with the base when it has moved.
git checkout -q fx-int
printf 'def add(a, b):\n    return b + a  # base\n' > lib/calc.py; commit lib/calc.py
git checkout -q lot-red
check "conflit avec la base : PAS PRET" 1 'conflit avec fx-int'
git checkout -q fx-int; git reset -q --hard HEAD~1

# 10. --lot: the pre-flight is asked, before review, for that round and zone; its refusal is the reason.
git checkout -q lot-red
LOT_ARGS="--lot tour-9:zone" check "--lot : pré-vol consulté" 0 'portail à blanc \(pré-vol\) : prêt'
grep -qx 'prevol --avant-relecture tour-9 zone' "$calls" || { reds=$((reds + 1)); echo "ROUGE pré-vol mal appelé ($(cat "$calls"))"; }
printf '#!/bin/sh\necho "  RECALÉ  pas de relecture"\nexit 1\n' > "$t/stub-prevol"
LOT_ARGS="--lot tour-9:zone" check "--lot : refus du pré-vol" 1 '^PAS PRET : portail à blanc \(pré-vol\) : pas de relecture'

# 11. Nothing written: the tree, the index and the worktrees as before.
[ -z "$(git status --porcelain)" ] || { reds=$((reds + 1)); echo "ROUGE pret.sh a laissé des fichiers : $(git status --porcelain | head -3)"; }
[ "$(git worktree list | wc -l)" -eq 1 ] || { reds=$((reds + 1)); echo "ROUGE un worktree est resté"; }

# 12. Bad usage.
out="$(bash scripts/bench/pret.sh 2>&1)"; [ $? -eq 2 ] || { reds=$((reds + 1)); echo "ROUGE sans base : pas exit 2"; }
out="$(bash scripts/bench/pret.sh nulle-part 2>&1)"; [ $? -eq 2 ] || { reds=$((reds + 1)); echo "ROUGE base inconnue : pas exit 2"; }

echo
[ "$reds" -eq 0 ] && { echo "test_pret : tout vert"; exit 0; }
echo "test_pret : $reds rouge(s)"; exit 1
