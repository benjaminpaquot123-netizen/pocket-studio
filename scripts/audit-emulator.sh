#!/usr/bin/env bash
# The Audit's checks of the RELEASE build (R8) on a running emulator (docs/audits/AUDIT.md §1 point 8,
# §3 « Contrôles sur l'émulateur de la CI », étage 0): install the APK given, then
#   1. cold start, measured N times (am start -W, TotalTime), against the 1.5 s goal;
#   2. screen by screen: a screenshot after each step of a short path driven by key events;
#   3. monkey: random gestures with a fixed seed (rerun with the same seed to reproduce);
#   4. Android chaos: killed in the background, rotation, dark mode, font at 200 %, split screen;
#      then (audit stage 3) a call, battery saver, microphone revoked, storage full, killed while playing;
#   5. frames (dumpsys gfxinfo) and memory (dumpsys meminfo) after the run;
#   6. any crash or ANR of the app in the log fails the run.
# Everything lands in OUT (default app/build/audit-emulator/), uploaded by the workflow.
#
#   scripts/audit-emulator.sh APK [--seed N (4242)] [--events N (5000)] [--starts N (10)] [--out DIR]
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="${1:-}"
[ -n "$APK" ] && [ -f "$APK" ] || { echo "usage: $0 APK [--seed N] [--events N] [--starts N] [--out DIR]" >&2; exit 2; }
shift
SEED=4242
EVENTS=5000
STARTS=10
OUT="$ROOT/app/build/audit-emulator"
while [ $# -gt 0 ]; do
    case "$1" in
        --seed) SEED="$2"; shift 2 ;;
        --events) EVENTS="$2"; shift 2 ;;
        --starts) STARTS="$2"; shift 2 ;;
        --out) OUT="$2"; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 2 ;;
    esac
done
for n in "$SEED" "$EVENTS" "$STARTS"; do [[ "$n" =~ ^[0-9]+$ ]] || { echo "not a whole number: $n" >&2; exit 2; }; done
PKG=com.pocketstudio
ACT="$PKG/com.pocketstudio.app.MainActivity"
mkdir -p "$OUT/screens"
FAIL=0
note() { echo "$*" | tee -a "$OUT/summary.txt"; }
shot() { adb exec-out screencap -p > "$OUT/screens/$1.png" 2> /dev/null || true; }
settle() { adb shell input keyevent KEYCODE_WAKEUP > /dev/null 2>&1 || true; sleep "${1:-2}"; }

: > "$OUT/summary.txt"
adb logcat -G 16M 2> /dev/null || true
adb logcat -c 2> /dev/null || true
adb logcat -v time > "$OUT/logcat.txt" 2> /dev/null &
LOGCAT=$!
trap 'kill "$LOGCAT" 2> /dev/null || true' EXIT

note "APK: $(basename "$APK") ($(stat -c %s "$APK") bytes)"
adb uninstall "$PKG" > /dev/null 2>&1 || true
adb install -r -g "$APK" > "$OUT/install.txt" 2>&1 || { cat "$OUT/install.txt"; note "FAIL: install"; exit 1; }
# The first-run notification prompt would sit over every screen.
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS > /dev/null 2>&1 || true

# 1. Cold start.
TIMES=()
for i in $(seq 1 "$STARTS"); do
    adb shell am force-stop "$PKG"
    sleep 1
    t=$(adb shell am start -W -n "$ACT" 2> /dev/null | tr -d '\r' | awk -F': ' '/TotalTime/ {print $2}')
    TIMES+=("${t:-0}")
    sleep 3
done
printf '%s\n' "${TIMES[@]}" > "$OUT/cold-start-ms.txt"
MEDIAN=$(printf '%s\n' "${TIMES[@]}" | sort -n | awk '{a[NR]=$1} END {print a[int((NR+1)/2)]}')
note "cold start: median ${MEDIAN} ms over $STARTS (goal < 1500 ms on the standard phone; the emulator is only a trend)"
shot 01-home

# 2. Screen by screen: the home screen, then each tab reached by DPAD focus and ENTER.
for i in 1 2 3 4 5 6; do
    adb shell input keyevent KEYCODE_TAB > /dev/null 2>&1
    adb shell input keyevent KEYCODE_ENTER > /dev/null 2>&1
    settle 2
    shot "02-step-$i"
    adb shell input keyevent KEYCODE_BACK > /dev/null 2>&1
    settle 1
done

# 3. Monkey (no system keys: no leaving the app by the power or home key).
adb shell monkey -p "$PKG" -s "$SEED" --throttle 50 --pct-syskeys 0 --pct-appswitch 2 \
    --ignore-security-exceptions -v "$EVENTS" > "$OUT/monkey.txt" 2>&1
if grep -qE 'CRASH|ANR in|NOT RESPONDING' "$OUT/monkey.txt"; then note "FAIL: monkey (seed $SEED) found a crash or an ANR"; FAIL=1
else note "monkey: $EVENTS events, seed $SEED, no crash"; fi
adb shell am start -n "$ACT" > /dev/null 2>&1; settle 3
shot 03-after-monkey

# 4. Chaos.
adb shell input keyevent KEYCODE_HOME; settle 1
adb shell am kill "$PKG"; settle 1
adb shell am start -n "$ACT" > /dev/null 2>&1; settle 3
shot 04-killed-in-background
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1; settle 3; shot 05-landscape
adb shell settings put system user_rotation 0; settle 2
adb shell cmd uimode night yes > /dev/null 2>&1; settle 3; shot 06-dark
adb shell cmd uimode night no > /dev/null 2>&1
adb shell settings put system font_scale 2.0; settle 3; shot 07-font-200
adb shell settings put system font_scale 1.0; settle 2
adb shell am trim-memory "$PKG" RUNNING_CRITICAL > /dev/null 2>&1; settle 2; shot 08-memory-critical
# Split screen (API 35: the task goes to the primary split).
TASK=$(adb shell am stack list 2> /dev/null | tr -d '\r' | awk -v p="$PKG" '$0 ~ p {for (i=1;i<=NF;i++) if ($i ~ /^taskId=/) {sub("taskId=","",$i); sub(":","",$i); print $i; exit}}')
if [ -n "$TASK" ]; then adb shell cmd activity set-task-windowing-mode "$TASK" 6 > /dev/null 2>&1 || true; settle 3; shot 09-split; fi
adb shell am start -n "$ACT" > /dev/null 2>&1; settle 2

# 4b. Audit stage 3 (docs/lots/AUDIT-3.md), what Robolectric cannot do: each best effort, any crash
# is caught by step 6. Play (space bar) before each, so they hit the app while the music runs.
play() { adb shell input keyevent KEYCODE_SPACE > /dev/null 2>&1; settle 1; }
# A phone call while playing (the emulator's console): the song stops, the app survives.
play; adb emu gsm call 5551234 > /dev/null 2>&1; settle 3; shot 10-call; adb emu gsm cancel 5551234 > /dev/null 2>&1; settle 2
# Battery saver on and off while playing.
play; adb shell cmd power set-mode 1 > /dev/null 2>&1 || adb shell settings put global low_power 1; settle 3; shot 11-battery-saver
adb shell cmd power set-mode 0 > /dev/null 2>&1 || adb shell settings put global low_power 0; settle 2
# The microphone permission taken away (Android kills the app), then the app opened again.
adb shell pm grant "$PKG" android.permission.RECORD_AUDIO > /dev/null 2>&1
adb shell am start -n "$ACT" > /dev/null 2>&1; settle 2
adb shell pm revoke "$PKG" android.permission.RECORD_AUDIO > /dev/null 2>&1; settle 2
adb shell am start -n "$ACT" > /dev/null 2>&1; settle 3; shot 12-mic-revoked
# The phone nearly full: a file fills /data to ~30 MB free, the app plays, edits and goes to the
# background (its save meets the full disk), then the room comes back and it opens again.
FREE_KB=$(adb shell df /data 2> /dev/null | tr -d '\r' | awk 'NR==2 {print $4}')
if [[ "$FREE_KB" =~ ^[0-9]+$ ]] && [ "$FREE_KB" -gt 60000 ]; then
    adb shell dd if=/dev/zero of=/data/local/tmp/audit-fill bs=1048576 count=$(( (FREE_KB - 30000) / 1024 )) > /dev/null 2>&1
    adb shell am start -n "$ACT" > /dev/null 2>&1; play; play; settle 2; shot 13-storage-full
    adb shell input keyevent KEYCODE_HOME; settle 3
    adb shell rm -f /data/local/tmp/audit-fill
    adb shell am start -n "$ACT" > /dev/null 2>&1; settle 3; shot 14-storage-back
    note "storage full: played and saved with ~30 MB free"
fi
# The app killed by the system while playing in the background, then opened from the launcher.
play; adb shell input keyevent KEYCODE_HOME; settle 2; adb shell am kill "$PKG"; settle 1
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1; settle 3; shot 15-killed-while-playing
note "stage 3 chaos: call, battery saver, microphone revoked, storage full, killed while playing (see screens 10 to 15)"

# 5. Frames and memory.
adb shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo.txt" 2>&1
adb shell dumpsys meminfo "$PKG" > "$OUT/meminfo.txt" 2>&1
note "frames: $(grep -E 'Total frames rendered|Janky frames' "$OUT/gfxinfo.txt" | head -2 | tr -s ' ' | tr '\n' ';')"
note "memory: $(grep -E 'TOTAL PSS|TOTAL:' "$OUT/meminfo.txt" | head -1 | tr -s ' ')"

# 6. Crashes and ANRs anywhere in the log.
sleep 1
if grep -E "FATAL EXCEPTION|ANR in $PKG|Fatal signal" "$OUT/logcat.txt" > "$OUT/crashes.txt"; then
    note "FAIL: $(wc -l < "$OUT/crashes.txt") crash/ANR line(s), see crashes.txt"; FAIL=1
    grep -A30 -E "FATAL EXCEPTION|Fatal signal" "$OUT/logcat.txt" | head -80
else note "log: no crash, no ANR"; fi
cat "$OUT/summary.txt"
exit "$FAIL"
