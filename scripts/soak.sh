#!/usr/bin/env bash
# « Endurance » on Linux (docs/TESTS-REELS.md): a long song (the assistant's song of STYLE, its
# parts played again after it until it lasts SONG seconds) looped for MINUTES, recorded as the app
# plays it (LiveSongRecorder: each part staged after the engine's swap, round and round) and played
# by the real engine as the phone's stream drives it (ps_live). Checked: no flag over the whole
# run (fade, silence, click, NaN, live gain out of range, a heap call in the audio callback, a loop
# 1 dB away from the second one), at least 3 whole loops, every callback; then the drift alert's
# own test on that recording. Report: OUT/soak.md (and the live bench's files under OUT).
#
#   scripts/soak.sh [--minutes M (30)] [--song S (360)] [--style jump_up] [--out DIR]
#
# Environment: as scripts/live-bench.sh (GRADLE_ARGS, CMAKE_EXTRA_ARGS, JOBS).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/soak"
MINUTES=30
SONG=360
STYLE=jump_up
while [ $# -gt 0 ]; do
    case "$1" in
        --minutes) MINUTES="$2"; shift 2 ;;
        --song) SONG="$2"; shift 2 ;;
        --style) STYLE="$2"; shift 2 ;;
        --out) OUT="$(mkdir -p "$2" && cd "$2" && pwd)"; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 1 ;;
    esac
done
mkdir -p "$OUT"
SECONDS_SOAK=$((MINUTES * 60))
echo "== Soak: $STYLE, a song of $SONG s looped for $MINUTES min"
LIVE_STATUS=0
"$ROOT/scripts/live-bench.sh" --styles "$STYLE" --loop "$SONG" --seconds "$SECONDS_SOAK" --out "$OUT" > "$OUT/live-bench.log" 2>&1 || LIVE_STATUS=$?
tail -4 "$OUT/live-bench.log"
[ -f "$OUT/log/$STYLE.txt" ] || { echo "the soak did not play (see $OUT/live-bench.log)" >&2; exit 1; }
python3 -I "$ROOT/scripts/bench/soak_check.py" "$OUT/tools/ps_live" "$OUT" "$STYLE" "$SECONDS_SOAK" --out "$OUT/soak.md"
# soak_check fails on any flag; the live bench's own report agrees (exit 1 when flagged).
exit "$LIVE_STATUS"
