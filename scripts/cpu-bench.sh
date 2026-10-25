#!/usr/bin/env bash
# « Petit téléphone » (docs/TESTS-REELS.md): every style's song played live as on the phone
# (ps_live, the live bench's recordings), the audio thread's CPU time measured at each callback,
# with 96- and 192-frame callbacks, RUNS times each (the fastest of each block is kept: the replay
# is the same at each run, another process only ever slows a block down). The report models a
# phone SCALE times slower: blocks over their real-time budget, glitches heard with a buffer of
# two callbacks, a verdict per style. Report: OUT/report.md (and cpu.json).
#
#   scripts/cpu-bench.sh [--styles a,b,c] [--seconds S (60)] [--runs R (2)] [--scale X (3)]
#                        [--cmds DIR (record them)] [--out DIR] [--strict] [--machine "text"]
#
# --cmds: the .pslive files of an earlier live bench (build/live-bench/cmds, or a CI artifact on
# another machine, an ARM one); otherwise the live bench records and checks them first.
# --strict: fails when a style is « à risque ». Environment: JOBS (1: runs never share a core),
# GRADLE_ARGS, CMAKE_EXTRA_ARGS (as scripts/live-bench.sh).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/cpu-bench"
STYLES=""
SECONDS_LIVE=60
RUNS=2
SCALE=3
CMDS=""
STRICT=""
MACHINE="$(uname -m), $(nproc 2>/dev/null || echo '?') cœurs, $(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2 | sed 's/^ *//' || true)"
JOBS="${JOBS:-1}"
while [ $# -gt 0 ]; do
    case "$1" in
        --styles) STYLES="$2"; shift 2 ;;
        --seconds) SECONDS_LIVE="$2"; shift 2 ;;
        --runs) RUNS="$2"; shift 2 ;;
        --scale) SCALE="$2"; shift 2 ;;
        --cmds) CMDS="$(cd "$2" && pwd)"; shift 2 ;;
        --out) OUT="$(mkdir -p "$2" && cd "$2" && pwd)"; shift 2 ;;
        --strict) STRICT="--strict"; shift ;;
        --machine) MACHINE="$2"; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 1 ;;
    esac
done
mkdir -p "$OUT"
TOOLS="$OUT/tools"
LIVE_STATUS=0
if [ -z "$CMDS" ]; then
    echo "== Live bench first (records the songs, checks them)"
    JOBS="${LIVE_JOBS:-$(nproc 2>/dev/null || echo 2)}" "$ROOT/scripts/live-bench.sh" --out "$OUT/live" --seconds "$SECONDS_LIVE" \
        ${STYLES:+--styles "$STYLES"} > "$OUT/live-bench.log" 2>&1 || LIVE_STATUS=$?
    tail -5 "$OUT/live-bench.log"
    CMDS="$OUT/live/cmds"
    TOOLS="$OUT/live/tools"
else
    echo "== Building ps_live"
    cmake -S "$ROOT/engine/bench" -B "$TOOLS" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-} > "$OUT/cmake.log"
    cmake --build "$TOOLS" -j "$(nproc 2>/dev/null || echo 2)" --target ps_live >> "$OUT/cmake.log"
fi
ls "$CMDS"/*.pslive > /dev/null 2>&1 || { echo "no .pslive in $CMDS" >&2; exit 1; }

echo "== Measuring the audio thread ($RUNS runs, 96 and 192 frames, $SECONDS_LIVE s each, $JOBS at a time)"
rm -rf "$OUT/cpu" && mkdir -p "$OUT/cpu"
export OUT TOOLS CMDS SECONDS_LIVE
for f in "$CMDS"/*.pslive; do
    s="$(basename "$f" .pslive)"
    if [ -z "$STYLES" ] || [[ ",$STYLES," == *",$s,"* ]]; then
        for r in $(seq 1 "$RUNS"); do for b in 96 192; do echo "$s $b $r"; done; done
    fi
done | xargs -P "$JOBS" -L 1 sh -c '"$TOOLS/ps_live" --quiet --burst "$1" --seconds "$SECONDS_LIVE" --cpu-out "$OUT/cpu/$0-$1-$2.f32" "$CMDS/$0.pslive" > "$OUT/cpu/$0-$1-$2.txt"; st=$?; [ $st = 0 ] || [ $st = 4 ] || { echo "FAILED: $0 ($st)" >&2; exit 255; }'

echo "== Report"
python3 -I "$ROOT/scripts/bench/cpu_report.py" "$OUT/cpu" --out "$OUT/report.md" --json "$OUT/cpu.json" --scale "$SCALE" --machine "$MACHINE" $STRICT
if [ "$LIVE_STATUS" != 0 ]; then
    echo "The live bench flagged something (see $OUT/live/report.md)" >&2
    exit "$LIVE_STATUS"
fi
