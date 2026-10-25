#!/usr/bin/env bash
# The emulator's tests (CI job « Emulator smoke test », nightly « Endurance sur émulateur »;
# docs/TESTS-REELS.md): the app's androidTest on a running emulator or phone (adb), then their
# own lines of the log (RealUse, LivePlaybackTest: the stream, each round, each listening) in
# app/build/realuse-logcat.txt, and on a failure the failing assertion and the app's errors.
#
#   scripts/emulator-tests.sh [--endurance MINUTES (2)] [--class a.b.C,a.b.D] [GRADLE ARGS...]
#
# --class: only these test classes (the nightly: EnduranceTest and JourneyTest).
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MINUTES=2
CLASSES=""
EXTRA=()
while [ $# -gt 0 ]; do
    case "$1" in
        --endurance) MINUTES="$2"; shift 2 ;;
        --class) CLASSES="$2"; shift 2 ;;
        *) EXTRA+=("$1"); shift ;;
    esac
done
[[ "$MINUTES" =~ ^[0-9]+$ ]] || { echo "--endurance takes whole minutes, not $MINUTES" >&2; exit 2; }
cd "$ROOT"
mkdir -p app/build
# A big log buffer, and the tests' own lines streamed to the file as they come: a run cut by a
# timeout still leaves them.
adb logcat -G 16M 2> /dev/null || true
adb logcat -c 2> /dev/null || true
adb logcat -v time -s RealUse:I LivePlaybackTest:I > app/build/realuse-logcat.txt 2> /dev/null &
LOGCAT=$!
trap 'kill "$LOGCAT" 2> /dev/null || true' EXIT
# A test stuck past 15 minutes (the endurance: its minutes and 15 more) fails, the runner's own
# limit, rather than holding the job.
TIMEOUT_MS=$(( (MINUTES > 0 ? MINUTES : 0) * 60000 + 900000 ))
./gradlew -PemulatorAbi :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.enduranceMinutes="$MINUTES" \
    -Pandroid.testInstrumentationRunnerArguments.timeout_msec="$TIMEOUT_MS" \
    ${CLASSES:+-Pandroid.testInstrumentationRunnerArguments.class="$CLASSES"} ${EXTRA[@]+"${EXTRA[@]}"}
STATUS=$?
sleep 1
grep -E 'stream:|round |memory:|listen|M4A:|WAV:|shared:|count-in|take:' app/build/realuse-logcat.txt | cut -c1-400 | tail -80 || true
if [ "$STATUS" != 0 ]; then
    find app/build/outputs/androidTest-results -name '*.xml' -exec grep -A25 '<failure' {} \; 2> /dev/null
    adb logcat -d -t 300 '*:E' 2> /dev/null | tail -150
fi
exit "$STATUS"
