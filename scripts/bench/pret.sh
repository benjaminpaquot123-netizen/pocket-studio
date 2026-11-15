#!/usr/bin/env bash
# METHODE-V4 §4.2 point 3 and §4.5 step 3 (astuce C): « is this lot ready? », said before asking for anything, from
# the lot's diff alone. Writes nothing in the working tree, the index or the branches: the parent is checked out in a
# throw-away worktree (removed on exit), every log goes to a temporary folder.
#
#   scripts/bench/pret.sh <base> [--lot <tour-N>:<zone>]
#
# <base> is the integration commit the lot left from (its merge base with HEAD is used). In order:
#   1. the level of the lot, from its diff: 1 texts only; 2 a .kt of src/main (or any other code); 3 a regex, the shell
#      (AppRoot, MainActivity), the save, the engine or the tuto's mechanics;
#   2. the RED proved: the tests the lot adds or changes are played on the parent (the base, with only the lot's test
#      files on top); they must fail (a test that compiles and passes there proves nothing);
#   3. the lot's tests on HEAD: green;
#   4. the bench's self-test (scripts/bench/selftest.sh) from level 2 on;
#   5. the replays the lot touches (scripts/bench/rejeu-defauts.sh): its own folders under rejeux/, those whose gesture
#      tags the diff changes in src/main; all of them at level 3;
#   6. the portal, dry: with --lot, scripts/bench/pre-vol.sh --avant-relecture <tour-N> <zone> (reads only); always, a
#      merge in memory with <base> (git merge-tree) and no symbolic link in the lot.
# Output: one line per step, then « PRET » (exit 0) or « PAS PRET : <reason> » (exit 1). Exit 2: bad usage.
#
# Overrides (the script's own test, scripts/bench/test_pret.sh): PS_PRET_GRADLE (the Gradle runner, default
# scripts/bench/gradle-file.sh of the tree tested), PS_PRET_SELFTEST, PS_PRET_REJEU, PS_PRET_PREVOL (commands).
set -uo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root" || exit 2
# Nothing written: no __pycache__ beside the lot's Python (« python3 -B » below too: -I ignores this variable; a stale
# cache also hid a fix of the same size written in the same second).
export PYTHONDONTWRITEBYTECODE=1

base_arg=""; lot=""
while [ $# -gt 0 ]; do
  case "$1" in
    --lot) lot="${2:-}"; shift 2 || { echo "usage: pret.sh <base> [--lot <tour-N>:<zone>]" >&2; exit 2; } ;;
    -h|--help) sed -n '2,24p' "$0"; exit 0 ;;
    *) [ -z "$base_arg" ] || { echo "pret.sh: one base only" >&2; exit 2; }; base_arg="$1"; shift ;;
  esac
done
[ -n "$base_arg" ] || { echo "usage: pret.sh <base> [--lot <tour-N>:<zone>]" >&2; exit 2; }
git rev-parse -q --verify "$base_arg^{commit}" >/dev/null || { echo "pret.sh: $base_arg is not a commit" >&2; exit 2; }
case "$lot" in ''|*:*) ;; *) echo "pret.sh: --lot takes <tour-N>:<zone>" >&2; exit 2 ;; esac
base="$(git merge-base "$base_arg" HEAD)" || { echo "pret.sh: $base_arg and HEAD have no common ancestor" >&2; exit 2; }
head="$(git rev-parse HEAD)"

work="$(mktemp -d)"
wt=""
cleanup() {
  [ -z "$wt" ] || git worktree remove --force "$wt" >/dev/null 2>&1 || true
  git worktree prune >/dev/null 2>&1 || true
  rm -rf "${work:?}"
}
trap cleanup EXIT

reasons=()
fail() { reasons+=("$1"); printf '  NON  %s\n' "$1"; }
ok() { printf '  ok   %s\n' "$1"; }
note() { printf '  --   %s\n' "$1"; }

files="$(git diff --name-only "$base" HEAD)"
[ -n "$files" ] || { echo "PAS PRET : le lot ne change rien par rapport à $(git rev-parse --short "$base")"; exit 1; }
[ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T')" ] || note "des changements ne sont pas commités : seul HEAD est jugé"

# --- 1. Level ---------------------------------------------------------------------------------------------------------
is_test() { printf '%s\n' "$1" | grep -qE '(^|/)src/(test|androidTest)/|(^|/)test_[^/]*\.(py|sh)$|^engine/host-tests/'; }
is_text() { printf '%s\n' "$1" | grep -qE '^docs/|\.md$|\.txt$|/res/values[^/]*/strings\.xml$|^app/src/main/assets/.*\.json$'; }
level=1; why="textes seuls"
while IFS= read -r f; do
  [ -n "$f" ] || continue
  is_test "$f" && continue
  if printf '%s\n' "$f" | grep -qE '^engine/|/(AppRoot|MainActivity)\.kt$|/tuto/[^/]*\.kt$|(Storage|Save|Autosave|Serializer|ProjectIo)[^/]*\.kt$'; then
    level=3; why="$f"; break
  fi
  if ! is_text "$f" && [ "$level" -lt 2 ]; then level=2; why="$f"; fi
done <<< "$files"
if [ "$level" -lt 3 ]; then
  # A regex added or removed in the application's code.
  rx="$(git diff -U0 "$base" HEAD -- '*/src/main/*.kt' | grep -E '^[-+][^-+]' | grep -E 'Regex\(|\.toRegex\(|\.matches\(' | head -1)"
  [ -z "$rx" ] || { level=3; why="une regex : $(printf '%s' "$rx" | cut -c1-80)"; }
fi
ok "niveau $level ($why)"

# --- The lot's tests ----------------------------------------------------------------------------------------------------
tests="$(while IFS= read -r f; do [ -n "$f" ] && is_test "$f" && git cat-file -e "HEAD:$f" 2>/dev/null && echo "$f"; done <<< "$files")"

# Gradle task of a test file's module: pure JVM modules (no « com.android » plugin) have « test ».
gradle_task() { # <dir> <file>
  local mod="${2%%/src/*}"
  if grep -qE 'com\.android|android\.(application|library)|libs\.plugins\.android' "$1/$mod/build.gradle.kts" 2>/dev/null; then
    echo ":${mod//\//:}:testDebugUnitTest"
  else
    echo ":${mod//\//:}:test"
  fi
}

# Plays the lot's tests in <dir>. Prints « vert », « rouge » or « rouge-compil » (the tests do not compile there: still a
# red, the fix brings what they call) or « panne » (nothing could run). Logs in $work/<tag>-*.
play() { # <dir> <tag>
  local dir="$1" tag="$2" f res="vert" n=0 task cls log rc
  declare -A tasks=()
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    case "$f" in
      *.py) n=$((n + 1)); (cd "$dir" && python3 -I -B "$f") > "$work/$tag-$n.log" 2>&1 || res="rouge" ;;
      *.sh) n=$((n + 1)); (cd "$dir" && bash "$f") > "$work/$tag-$n.log" 2>&1 || res="rouge" ;;
      engine/host-tests/*) tasks[host]=1 ;;
      */src/test/*.kt)
        cls="$(basename "$f" .kt)"
        task="$(gradle_task "$dir" "$f")"
        tasks[$task]="${tasks[$task]:-} --tests *.$cls" ;;
      *) ;;
    esac
  done <<< "$tests"
  for task in "${!tasks[@]}"; do
    n=$((n + 1)); log="$work/$tag-$n.log"
    if [ "$task" = host ]; then
      (cd "$dir" && ./scripts/host-tests.sh) > "$log" 2>&1 || res="rouge"
      continue
    fi
    rc=0
    # shellcheck disable=SC2086 # the « --tests » filters are words on purpose
    (cd "$dir" && ${PS_PRET_GRADLE:-$dir/scripts/bench/gradle-file.sh} "$task" ${tasks[$task]}) > "$log" 2>&1 || rc=$?
    [ "$rc" -eq 0 ] && continue
    if grep -qE '^e: |Compilation error|compileDebugUnitTestKotlin.*FAILED|compileTestKotlin.*FAILED' "$log"; then
      [ "$res" = rouge ] || res="rouge-compil"
    elif grep -qE 'tests completed, [0-9]+ failed|There were failing tests|FAILED$' "$log"; then
      res="rouge"
    else
      echo "panne"; return
    fi
  done
  [ "$n" -gt 0 ] || { echo "aucun"; return; }
  echo "$res"
}

if [ -z "$tests" ]; then
  if [ "$level" -eq 1 ]; then
    note "aucun test dans le lot (niveau 1 : pas de rouge à prouver)"
  else
    fail "aucun test ajouté ni changé : le rouge ne peut pas être prouvé (niveau $level)"
  fi
else
  # --- 2. The red, on the parent --------------------------------------------------------------------------------------
  wt="$work/parent"
  if git worktree add -q --detach "$wt" "$base" >/dev/null 2>&1; then
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      mkdir -p "$wt/$(dirname "$f")"
      git show "HEAD:$f" > "$wt/$f"
    done <<< "$tests"
    r="$(play "$wt" parent)"
    case "$r" in
      rouge) ok "rouge prouvé : les tests du lot échouent sur le parent ($(git rev-parse --short "$base"))" ;;
      rouge-compil) ok "rouge prouvé : les tests du lot ne compilent pas sans le correctif" ;;
      vert) fail "rouge non prouvé : les tests du lot passent déjà sur le parent ($(git rev-parse --short "$base")) sans le correctif" ;;
      *) fail "rouge non prouvé : les tests n'ont pas pu tourner sur le parent (journaux : $work/parent-*)" ;;
    esac
  else
    fail "rouge non prouvé : le parent n'a pas pu être extrait"
  fi
  # --- 3. The lot's tests on HEAD -------------------------------------------------------------------------------------
  r="$(play "$root" tete)"
  case "$r" in
    vert) ok "tests du lot verts sur $(git rev-parse --short "$head")" ;;
    *) fail "tests du lot pas verts sur $(git rev-parse --short "$head") ($r)$(f="$(ls "$work"/tete-*.log 2>/dev/null | head -1)"; [ -z "$f" ] || printf ' : %s' "$(grep -m1 -E 'FAIL|Error|failed|AssertionError' "$f" | cut -c1-100)")" ;;
  esac
fi

# --- 4. The bench's self-test ---------------------------------------------------------------------------------------
if [ "$level" -ge 2 ]; then
  mkdir -p "$work/lot-selftest"
  if ${PS_PRET_SELFTEST:-scripts/bench/selftest.sh} "$work/lot-selftest" > "$work/selftest.log" 2>&1; then
    ok "auto-test du banc vert"
  else
    fail "auto-test du banc rouge ($(tail -1 "$work/selftest.log" | cut -c1-100))"
  fi
else
  note "auto-test du banc non lancé (niveau 1)"
fi

# --- 5. The replays touched -----------------------------------------------------------------------------------------
rdir="docs/audits/ultra/rejeux"
touched=()
if [ -d "$rdir" ]; then
  removed="$(git diff -U0 "$base" HEAD -- '*/src/main/*' | grep -E '^-[^-]' || true)"
  for d in "$rdir"/*/; do
    [ -d "$d" ] || continue
    id="$(basename "$d")"
    case "$id" in _*) continue ;; esac
    hit=""
    [ "$level" -lt 3 ] || hit=1
    printf '%s\n' "$files" | grep -qF "$rdir/$id/" && hit=1
    if [ -z "$hit" ] && [ -n "$removed" ]; then
      for c in "$d"cmd-*.txt; do
        [ -f "$c" ] || continue
        read -r _verb arg _rest < "$c" || true
        case "$arg" in ''|[0-9]*|-[0-9]*) continue ;; esac
        printf '%s\n' "$removed" | grep -qF -- "\"$arg\"" && { hit=1; break; }
      done
    fi
    [ -z "$hit" ] || touched+=("$id")
  done
fi
if [ ${#touched[@]} -eq 0 ]; then
  note "aucun rejeu touché"
elif ${PS_PRET_REJEU:-scripts/bench/rejeu-defauts.sh} "${touched[@]}" > "$work/rejeux.log" 2>&1; then
  ok "rejeux touchés tenus (${#touched[@]} : ${touched[*]:0:5}$([ ${#touched[@]} -le 5 ] || echo …))"
else
  fail "rejeu non tenu : $(grep -m1 'non tenu' "$work/rejeux.log" | cut -c1-100)"
fi

# --- 6. The portal, dry ---------------------------------------------------------------------------------------------
links="$(git diff --diff-filter=AMT --raw "$base" HEAD | awk '$2 == "120000" {print $NF}' | head -3)"
[ -z "$links" ] || fail "lien symbolique dans le lot : $(printf '%s ' $links)"
mt="$(git merge-tree --write-tree --name-only "$base_arg" HEAD 2>/dev/null)"; rc=$?
if [ "$rc" -eq 0 ]; then
  ok "fusion à blanc avec $base_arg sans conflit"
elif [ "$rc" -eq 1 ]; then
  fail "conflit avec $base_arg : $(printf '%s\n' "$mt" | sed -n '2,/^$/p' | sed '/^$/d' | head -4 | tr '\n' ' ')"
else
  note "fusion à blanc impossible avec ce git (2.38 au moins)"
fi
if [ -n "$lot" ]; then
  if ${PS_PRET_PREVOL:-scripts/bench/pre-vol.sh} --avant-relecture "${lot%%:*}" "${lot#*:}" > "$work/prevol.log" 2>&1; then
    ok "portail à blanc (pré-vol) : prêt"
  else
    fail "portail à blanc (pré-vol) : $(grep -m1 'RECALÉ' "$work/prevol.log" | sed 's/^ *RECALÉ *//' | cut -c1-100)"
  fi
fi

echo
if [ ${#reasons[@]} -eq 0 ]; then
  echo "PRET (niveau $level)"
  exit 0
fi
echo "PAS PRET : ${reasons[0]}$([ ${#reasons[@]} -le 1 ] || echo " (+$((${#reasons[@]} - 1)) autre(s))")"
exit 1
