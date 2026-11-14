#!/usr/bin/env bash
# Audit method v2 (docs/audits/ultra/META-AUDIT.md, change 1): the bench's self-tests, BEFORE every lot.
#   scripts/bench/selftest.sh <lot folder>
# A red check forbids the lot: scripts/human-tester.sh refuses to start without the stamp this writes
# (<lot folder>/selftest.ok, holding the commit and the tree it passed on).
# Checks (analysis Q2 §5):
#   machine   ≥ 2 GB of disk, ≥ PS_MEM_MIN_GB of memory, one Gradle (gradle-file.sh does it);
#   bench     every Bench*Test, Tour*AppProof<digits>Test and Tour*IdleTest of app/src/test/…/human (engine render
#             length, play head, note takes, rename dialogs, Tout couper / ■, share, microphone, pads, amenées…), and
#             every class that carries a @KnownDefect anywhere else in the unit tests (audit « banc crédible » A.3:
#             ChallengeControllerTest today); the list is scripts/bench/selftest-attendus.py's;
#   scripts   scripts/bench/test_ultra_bench.py (fuser, metrics, dashboard).
# Banc fiable 1.2: the verdict is read test by test from the JUnit results, not from Gradle's exit code. A red test
# forbids the lot unless it is in scripts/bench/selftest-quarantaine.txt (named in the stamp); every skipped test is
# listed with its reason in <lot>/selftest-resume.txt. Never green on 0 test run. Audit « banc crédible » A.4: a class
# skipped as a whole is red, and so is a skip that is not in scripts/bench/selftest-sauts-connus.txt. That list is
# bounded: only the seat entry point `drive` of a class inheriting HumanTester may be there (selftest-attendus.py).
set -euo pipefail
lot="$(cd "${1:?usage: scripts/bench/selftest.sh <lot folder>}" && pwd)"
cd "$(dirname "$0")/../.."
fail() { echo "SELFTEST ROUGE : $*" >&2; rm -f "$lot/selftest.ok"; exit 1; }
# External audit: an old stamp never outlives a new run (whatever stops it); the code is fingerprinted BEFORE the tests
# and again after: a change made while they ran is never stamped as tested.
rm -f "$lot/selftest.ok"
scripts/bench/tampon.sh empreinte > "$lot/selftest-empreinte.txt"
# Audit « banc crédible » 2, P3: the code tested, named in the summary (the judge reads a summary of THIS code only).
scripts/bench/tampon.sh code > "$lot/selftest-code.txt"

free_gb=$(df -BG --output=avail / | tail -1 | tr -dc 0-9)
[ "$free_gb" -ge 2 ] || fail "disque : ${free_gb} Go libres (< 2)"
# PLAN-RESTE 5.12 (control 14): the machine, checked and written in the summary. Memory ≥ 4 GB available; at most one
# Gradle daemon; orphans = Gradle test workers whose parent is init (PPID 1) only: a live lot's 4 forks are not orphans.
mem_gb=$(awk '/MemAvailable/ {printf "%d", $2 / 1048576}' /proc/meminfo)
[ "$mem_gb" -ge 4 ] || fail "mémoire : ${mem_gb} Go disponibles (< 4)"
daemons=$(pgrep -fc 'GradleDaemon' || true)
[ "${daemons:-0}" -le 1 ] || fail "démons Gradle : $daemons (1 au plus)"
orphans=$(ps -eo ppid=,args= | awk '$1 == 1 && /GradleWorkerMain/' | wc -l)
[ "$orphans" -eq 0 ] || fail "workers Gradle orphelins : $orphans (pkill -f GradleWorkerMain)"
machine="machine : ${free_gb} Go disque, ${mem_gb} Go mémoire, ${daemons:-0} démon(s), 0 orphelin"

python3 -I scripts/bench/test_ultra_bench.py >"$lot/selftest-scripts.log" 2>&1 || fail "scripts du banc (voir $lot/selftest-scripts.log)"
[ ! -f scripts/bench/test_portail3.py ] || python3 -I scripts/bench/test_portail3.py >>"$lot/selftest-scripts.log" 2>&1 || fail "scripts du banc, portail3 (voir $lot/selftest-scripts.log)"
[ ! -f scripts/bench/test_stable.py ] || python3 -I scripts/bench/test_stable.py >>"$lot/selftest-scripts.log" 2>&1 || fail "scripts du banc, ci-rouge (voir $lot/selftest-scripts.log)"
[ ! -f scripts/bench/test_portail4.py ] || python3 -I scripts/bench/test_portail4.py >>"$lot/selftest-scripts.log" 2>&1 || fail "scripts du banc, portail4 (voir $lot/selftest-scripts.log)"
[ ! -f scripts/bench/test_constate.py ] || python3 -I scripts/bench/test_constate.py >>"$lot/selftest-scripts.log" 2>&1 || fail "scripts du banc, constate (voir $lot/selftest-scripts.log)"

junit="$lot/junit"
rm -rf "$junit"; mkdir -p "$junit"
# Banc fiable 1.2: the rename tests run in a Gradle run (a JVM) of their own: alone they pass 8 times out of 8, among
# the Bench* tests they went red 3 times out of 5 (state left by an earlier test; culprit still to find, journal 1.2).
# Review of session 1: a run that produced nothing must not read as green: every test class of the patterns is expected
# (read from the sources), and a Gradle that failed with no red test in its results is red.
# PLAN-RESTE 4.5: Tour3NameFieldIdleTest (the name fields alone) runs with the rename tests: every Tour*IdleTest; and
# Tour5AppProof2Test (its name ends in AppProof2Test) was neither expected nor run until banc45.
# Audit « banc crédible » A.3: the list is read by selftest-attendus.py (« pass class »): a @KnownDefect guard outside
# human/ (ChallengeControllerTest) is expected and run too, in a pass of its own (« defauts »).
python3 -I scripts/bench/selftest-attendus.py > "$lot/selftest-passes.txt"
awk '{n = split($2, p, "."); print p[n]}' "$lot/selftest-passes.txt" | sort -u > "$lot/selftest-attendus.txt"
# Review of lot 1: the list of known skips is bounded (a seat's `drive` only): any other entry forbids the lot.
python3 -I scripts/bench/selftest-attendus.py --sauts-connus scripts/bench/selftest-sauts-connus.txt > "$lot/selftest-sauts-refuses.txt" \
  || fail "liste des sauts connus hors borne : $(tr '\n' ' ' < "$lot/selftest-sauts-refuses.txt")"
gradle_failed=0
run_tests() {
  # PLAN-RESTE 4.3: each pass writes its XML straight into $junit/<pass>/ (never the shared build/test-results that
  # another self-test may wipe between its Gradle run and its copy).
  local name="$1"; shift
  mkdir -p "$junit/$name"
  PS_GRADLE_LOG="$lot/selftest-gradle-$name.log" scripts/bench/gradle-file.sh :app:testDebugUnitTest "$@" \
      "-Ptest.results.dir=$junit/$name" --rerun --continue >/dev/null \
    || echo "$name" >> "$lot/selftest-gradle-echecs.txt"
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
run_tests banc "${profile_arg[@]}" --tests 'com.pocketstudio.app.human.Bench*' --tests 'com.pocketstudio.app.human.Tour*AppProof*Test'
run_tests renommage --tests 'com.pocketstudio.app.human.Tour*IdleTest'
defect_args=()
while read -r pass cls; do
  [ "$pass" = defauts ] && defect_args+=(--tests "$cls")
done < "$lot/selftest-passes.txt"
if [ ${#defect_args[@]} -gt 0 ]; then
  run_tests defauts "${defect_args[@]}"
fi

verdict_ok=0
python3 -I scripts/bench/selftest-verdict.py "$junit" scripts/bench/selftest-quarantaine.txt "$lot/selftest-resume.txt" \
    "$lot/selftest-attendus.txt" "$lot/selftest-gradle-echecs.txt" scripts/bench/selftest-sauts-connus.txt || verdict_ok=1
echo "$machine" >> "$lot/selftest-resume.txt"
echo "code testé : $(cat "$lot/selftest-code.txt")" >> "$lot/selftest-resume.txt"
# Audit « banc crédible » A.10: the judge of the work reads THIS run's summary (a guard is green only when named
# there). Its gaps are the memory's (verdicts.tsv, the journal, the pairs), not the bench's: written in
# <lot>/selftest-chantier.txt and counted here, they do not forbid the lot, and a red
# self-test shows them too (P3: unstamped, its summary is refused and no guard is green in it).
judge() {
  python3 -I scripts/bench/verifie-chantier.py --resume "$lot/selftest-resume.txt" > "$lot/selftest-chantier.txt" 2>&1 || true
  echo "Juge du chantier : $(tail -1 "$lot/selftest-chantier.txt") ($lot/selftest-chantier.txt)"
}
[ "$verdict_ok" -eq 0 ] || { python3 -I scripts/bench/verifie-chantier.py --resume "$lot/selftest-resume.txt" > "$lot/selftest-chantier.txt" 2>&1 || true
  fail "tests du banc (voir $lot/selftest-resume.txt et $lot/selftest-gradle-*.log)"; }

mapfile -t held < <(sed -n 's/^quarantaine rouge : \([^ ]*\) :.*/\1/p' "$lot/selftest-resume.txt")
[ "$(scripts/bench/tampon.sh empreinte)" = "$(cat "$lot/selftest-empreinte.txt")" ] || fail "le code a changé pendant le selftest : relancer"
scripts/bench/tampon.sh ecris "$lot" "$lot/selftest-empreinte.txt" "${held[@]}" || fail "tampon refusé"
# The judge after the stamp: the summary it reads is sealed for this code.
judge
echo "Selftest vert : $lot/selftest.ok ($(head -1 "$lot/selftest-resume.txt"))"
