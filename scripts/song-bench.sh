#!/usr/bin/env bash
# The song bench (docs/plan/son.md § 2): the assistant's songs exported by the app's real code,
# replayed through the engine core on Linux, measured, and summed up in a Markdown report.
#
#   scripts/song-bench.sh [--styles a,b,c] [--out DIR] [--ffmpeg] [--before FILE.json] [--no-record] [--jobs N]
#
#   --styles   only these styles (ids of scripts/packs/recipes); default: all of them
#   --out      work folder (default build/bench): cmds/ (.pscmd + samples), wav/, json/, render/ (ps_render's
#              reports: the loudness target's drive), report.md, bench.json
#   --ffmpeg   cross-checks LUFS-I and true peak with ffmpeg's ebur128 (fails beyond ±0.1)
#   --before   a previous bench.json: the report shows the change against it
#   --no-record  keeps the .pscmd files already in the work folder (engine-only change)
#   --title    the report's title line
# Environment: GRADLE_ARGS (default --max-workers=1), CMAKE_EXTRA_ARGS (e.g. ccache), JOBS.
# The rendered audio stays in the work folder: never commit it.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/bench"
STYLES=""
FFMPEG=0
BEFORE=""
RECORD=1
TITLE=""
JOBS="${JOBS:-$(nproc 2>/dev/null || echo 2)}"
while [ $# -gt 0 ]; do
    case "$1" in
        --styles) STYLES="$2"; shift 2 ;;
        --out) OUT="$(mkdir -p "$2" && cd "$2" && pwd)"; shift 2 ;;
        --ffmpeg) FFMPEG=1; shift ;;
        --before) BEFORE="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"; shift 2 ;;
        --no-record) RECORD=0; shift ;;
        --jobs) JOBS="$2"; shift 2 ;;
        --title) TITLE="$2"; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 1 ;;
    esac
done
TOOLS="$OUT/tools"
mkdir -p "$OUT"

echo "== Building ps_render and ps_measure"
cmake -S "$ROOT/engine/bench" -B "$TOOLS" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-} > "$OUT/cmake.log"
cmake --build "$TOOLS" -j "$JOBS" --target ps_render ps_measure >> "$OUT/cmake.log"

if [ "$RECORD" = 1 ]; then
    echo "== Recording the exports (app test SongBenchExport)"
    rm -rf "$OUT/cmds"
    (cd "$ROOT" && ./gradlew ${GRADLE_ARGS:---max-workers=1} :app:testDebugUnitTest \
        --tests 'com.pocketstudio.app.bench.SongBenchExport' --rerun \
        -Pbench.out="$OUT/cmds" -Pbench.render="$TOOLS/ps_render" ${STYLES:+-Pbench.styles="$STYLES"}) > "$OUT/gradle.log" 2>&1 \
        || { tail -60 "$OUT/gradle.log"; exit 1; }
fi
ls "$OUT"/cmds/*.pscmd > /dev/null 2>&1 || { echo "no .pscmd recorded in $OUT/cmds" >&2; exit 1; }

echo "== Rendering and measuring ($JOBS at a time)"
rm -rf "$OUT/wav" "$OUT/json" "$OUT/render"
mkdir -p "$OUT/wav" "$OUT/json" "$OUT/render"
list() {
    for f in "$OUT"/cmds/*.pscmd; do
        s="$(basename "$f" .pscmd)"
        if [ -z "$STYLES" ] || [[ ",$STYLES," == *",$s,"* ]]; then echo "$s"; fi
    done
}
export OUT TOOLS
list | xargs -P "$JOBS" -I{} sh -c '
    "$TOOLS/ps_render" --report "$OUT/render/{}.json" "$OUT/cmds/{}.pscmd" "$OUT/wav/{}.wav" && "$TOOLS/ps_measure" "$OUT/wav/{}.wav" "$OUT/cmds/{}.pscmd" > "$OUT/json/{}.json" \
        || { echo "FAILED: {}" >&2; exit 255; }'
(cd "$OUT/wav" && sha256sum *.wav > "$OUT/sha256.txt")

if [ "$FFMPEG" = 1 ]; then
    echo "== Cross-check with ffmpeg ebur128"
    : > "$OUT/ffmpeg.txt"
    for s in $(list); do
        summary="$(ffmpeg -nostats -hide_banner -i "$OUT/wav/$s.wav" -af ebur128=peak=true -f null - 2>&1 | sed -n '/Summary:/,$p')"
        i="$(echo "$summary" | awk '/^ *I:/ {print $2}')"
        tp="$(echo "$summary" | awk '/^ *Peak:/ {print $2}')"
        echo "$s $i $tp" >> "$OUT/ffmpeg.txt"
    done
fi

echo "== Report"
python3 -I "$ROOT/scripts/bench/report.py" "$OUT/json" --out "$OUT/report.md" --json "$OUT/bench.json" \
    --hashes "$OUT/sha256.txt" --renders "$OUT/render" ${BEFORE:+--before "$BEFORE"} ${TITLE:+--title "$TITLE"} \
    $([ "$FFMPEG" = 1 ] && echo --ffmpeg "$OUT/ffmpeg.txt")
echo "Report: $OUT/report.md"
