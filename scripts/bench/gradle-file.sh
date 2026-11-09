#!/usr/bin/env bash
# Audit method v2 (docs/audits/ultra/META-AUDIT.md, change 8): the ONE way to run Gradle during an audit round.
#   scripts/bench/gradle-file.sh <gradle args…>
# - One Gradle at a time on the machine: a lock (PS_GRADLE_LOCK) queues the others (two daemons = the OOMs of
#   round 3 lot F and round 4 lot A, six seats lost).
# - Before running: every other Gradle daemon AND the Kotlin compile daemon stopped (`./gradlew --stop` leaves the
#   Kotlin one alive: it was the second big JVM of the 14:03 OOM), then a memory budget: at least PS_MEM_MIN_GB
#   available, else it waits up to 10 min, then refuses (exit 75).
# - While running: a run whose output has not moved for PS_STALL_MIN minutes (20, CLAUDE.md) is killed (exit 124),
#   its last lines kept in PS_GRADLE_LOG.
set -euo pipefail
cd "$(dirname "$0")/../.."
lock="${PS_GRADLE_LOCK:-/tmp/pocket-studio-gradle.lock}"
mem_min="${PS_MEM_MIN_GB:-6}"
stall_min="${PS_STALL_MIN:-20}"
log="${PS_GRADLE_LOG:-/tmp/pocket-studio-gradle.log}"

exec 9>"$lock"
if ! flock -n 9; then
  echo "gradle-file: another Gradle runs, waiting in line ($lock)…" >&2
  flock 9
fi

stop_daemons() {
  PS_GRADLE_FILE=1 ./gradlew --stop >/dev/null 2>&1 || true
  # The Kotlin compile daemon and any orphan test worker (not stopped by --stop).
  pkill -f 'KotlinCompileDaemon' 2>/dev/null || true
  pkill -f 'GradleWorkerMain' 2>/dev/null || true
  sleep 2
}

avail_gb() { awk '/MemAvailable/ {printf "%d", $2 / 1048576}' /proc/meminfo; }

stop_daemons
waited=0
while [ "$(avail_gb)" -lt "$mem_min" ]; do
  if [ "$waited" -ge 600 ]; then
    echo "gradle-file: only $(avail_gb) GB of memory available (< $mem_min): refused." >&2
    exit 75
  fi
  sleep 15
  waited=$((waited + 15))
done

: >"$log"
PS_GRADLE_FILE=1 ./gradlew "$@" >"$log" 2>&1 &
pid=$!
status=0
while kill -0 "$pid" 2>/dev/null; do
  sleep 30
  idle=$(( ($(date +%s) - $(stat -c %Y "$log")) / 60 ))
  if [ "$idle" -ge "$stall_min" ]; then
    echo "gradle-file: no output for $idle min: Gradle killed (frozen)." >&2
    pkill -P "$pid" 2>/dev/null || true
    kill "$pid" 2>/dev/null || true
    stop_daemons
    tail -40 "$log" >&2
    exit 124
  fi
done
wait "$pid" || status=$?
tail -25 "$log"
stop_daemons
exit "$status"
