#!/usr/bin/env bash
# Audit method v2 (docs/audits/ultra/META-AUDIT.md, change 1): the bench's self-tests, BEFORE every lot.
#   scripts/bench/selftest.sh <lot folder>
# A red check forbids the lot: scripts/human-tester.sh refuses to start without the stamp this writes
# (<lot folder>/selftest.ok, holding the commit and the tree it passed on).
# Checks (analysis Q2 §5):
#   machine   ≥ 2 GB of disk, ≥ PS_MEM_MIN_GB of memory, one Gradle (gradle-file.sh does it);
#   bench     every Bench*Test and Tour*AppProofTest/Tour*RenameIdleTest of app/src/test/…/human (engine render length,
#             play head, note takes, rename dialogs, Tout couper / ■, share, microphone, pads, amenées…);
#   scripts   scripts/bench/test_ultra_bench.py (fuser, metrics, dashboard).
# Banc fiable 1.2: the verdict is read test by test from the JUnit results, not from Gradle's exit code. A red test
# forbids the lot unless it is in scripts/bench/selftest-quarantaine.txt (named in the stamp); every skipped test is
# listed with its reason in <lot>/selftest-resume.txt. Never green on 0 test run.
set -euo pipefail
lot="$(cd "${1:?usage: scripts/bench/selftest.sh <lot folder>}" && pwd)"
cd "$(dirname "$0")/../.."
fail() { echo "SELFTEST ROUGE : $*" >&2; rm -f "$lot/selftest.ok"; exit 1; }
# External audit: an old stamp never outlives a new run (whatever stops it); the code is fingerprinted BEFORE the tests
# and again after: a change made while they ran is never stamped as tested.
rm -f "$lot/selftest.ok"
scripts/bench/tampon.sh empreinte > "$lot/selftest-empreinte.txt"

free_gb=$(df -BG --output=avail / | tail -1 | tr -dc 0-9)
[ "$free_gb" -ge 2 ] || fail "disque : ${free_gb} Go libres (< 2)"

python3 -I scripts/bench/test_ultra_bench.py >"$lot/selftest-scripts.log" 2>&1 || fail "scripts du banc (voir $lot/selftest-scripts.log)"

results=app/build/test-results/testDebugUnitTest
junit="$lot/junit"
rm -rf "$junit"; mkdir -p "$junit"
# Banc fiable 1.2: the rename tests run in a Gradle run (a JVM) of their own: alone they pass 8 times out of 8, among
# the Bench* tests they went red 3 times out of 5 (state left by an earlier test; culprit still to find, journal 1.2).
# Review of session 1: a run that produced nothing must not read as green: every test class of the patterns is expected
# (read from the sources), and a Gradle that failed with no red test in its results is red.
grep -rhoE '^class (Bench[A-Za-z0-9]*Test|Tour[A-Za-z0-9]*AppProofTest|Tour[A-Za-z0-9]*RenameIdleTest)\b' app/src/test/java/com/pocketstudio/app/human \
  | awk '{print $2}' | sort -u > "$lot/selftest-attendus.txt"
gradle_failed=0
run_tests() {
  local name="$1"; shift
  rm -rf "$results"
  PS_GRADLE_LOG="$lot/selftest-gradle-$name.log" scripts/bench/gradle-file.sh :app:testDebugUnitTest "$@" --rerun --continue >/dev/null \
    || echo "$name" >> "$lot/selftest-gradle-echecs.txt"
  cp "$results"/TEST-*.xml "$junit/" 2>/dev/null || true
}
rm -f "$lot/selftest-gradle-echecs.txt"
# Banc fiable 3.2 (control 13): the amenées are checked on the profiles of THIS lot's seats (their start.txt, the
# bench's own keys), not only on round 1's ten.
profiles=()
for st in "$lot"/*/start.txt; do
  [ -f "$st" ] || continue
  line="$({ grep -E '^(lang|device|font|mode|main|lit|theme)=' "$st" || true; } | tr '\n' ' ' | sed 's/ *$//')"
  [ -n "$line" ] && profiles+=("$line")
done
profile_arg=()
if [ ${#profiles[@]} -gt 0 ]; then
  printf '%s\n' "${profiles[@]}" | sort -u > "$lot/selftest-profils.txt"
  profile_arg=("-Pbench.profiles=$lot/selftest-profils.txt")
fi
run_tests banc "${profile_arg[@]}" --tests 'com.pocketstudio.app.human.Bench*' --tests 'com.pocketstudio.app.human.Tour*AppProofTest'
run_tests renommage --tests 'com.pocketstudio.app.human.Tour*RenameIdleTest'

python3 -I scripts/bench/selftest-verdict.py "$junit" scripts/bench/selftest-quarantaine.txt "$lot/selftest-resume.txt" \
    "$lot/selftest-attendus.txt" "$lot/selftest-gradle-echecs.txt" \
  || fail "tests du banc (voir $lot/selftest-resume.txt et $lot/selftest-gradle-*.log)"

mapfile -t held < <(sed -n 's/^quarantaine rouge : \([^ ]*\) :.*/\1/p' "$lot/selftest-resume.txt")
[ "$(scripts/bench/tampon.sh empreinte)" = "$(cat "$lot/selftest-empreinte.txt")" ] || fail "le code a changé pendant le selftest : relancer"
scripts/bench/tampon.sh ecris "$lot" "$lot/selftest-empreinte.txt" "${held[@]}"
echo "Selftest vert : $lot/selftest.ok ($(head -1 "$lot/selftest-resume.txt"))"
