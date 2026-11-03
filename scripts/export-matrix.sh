#!/usr/bin/env bash
# The export matrix (lot E, « Master pro », PLAN-AUDITS-FINAUX §1 bis), in pairs: format x rate x bits x
# target x ceiling on 5 styles, every pair of values in at least one case (scripts/bench/pairwise.py, the
# in-repo generator). Each case is the style's song as the app exports it (SongBenchExport records the
# engine commands, ps_render replays them through the engine core with the case's options), encoded as
# the app does (FLAC: the app's FlacEncoder; M4A: FDK AAC, the family of Android's encoder, with the
# app's M4aGuard loop), tagged by the app's Metadata (ExportPairsTest), then checked with ffmpeg:
#   - LUFS within ±0.5 of the target (the whole song for Spotify -14, Apple -16, club -9, a free -11;
#     the loud part for « comme le style », which aims at the Drop), on the file as encoded;
#   - true peak under the ceiling after the encoding (M4A decoded, FLAC included);
#   - ffprobe reads the file: codec, rate, bits, length, title and artist;
#   - stems: the sum of the separate tracks (each strip with its sends, or each strip dry plus the
#     reverb's and the echo's returns) equals the mix without the master under -60 dB.
#
#   scripts/export-matrix.sh [--out DIR] [--no-record] [--jobs N] [--styles a,b,...] [--cases c01,c02]
#
# The rendered audio stays in the work folder (build/matrix by default): never commit it.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/matrix"
RECORD=1
JOBS="${JOBS:-3}"
STYLES="techno,house,dnb,trap,lofi"
ONLY=""
while [ $# -gt 0 ]; do
    case "$1" in
        --out) OUT="$(mkdir -p "$2" && cd "$2" && pwd)"; shift 2 ;;
        --no-record) RECORD=0; shift ;;
        --jobs) JOBS="$2"; shift 2 ;;
        --styles) STYLES="$2"; shift 2 ;;
        --cases) ONLY="$2"; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 1 ;;
    esac
done
command -v ffmpeg > /dev/null && command -v ffprobe > /dev/null || { echo "ffmpeg and ffprobe are needed" >&2; exit 1; }
TOOLS="$OUT/tools"
mkdir -p "$OUT"
GRADLE_LOCK="${GRADLE_LOCK:-}"
gradle() {
    if [ -n "$GRADLE_LOCK" ]; then (cd "$ROOT" && flock "$GRADLE_LOCK" ./gradlew ${GRADLE_ARGS:---max-workers=2} "$@")
    else (cd "$ROOT" && ./gradlew ${GRADLE_ARGS:---max-workers=2} "$@"); fi
}

echo "== Building ps_render"
cmake -S "$ROOT/engine/bench" -B "$TOOLS" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-} > "$OUT/cmake.log"
cmake --build "$TOOLS" -j "$JOBS" --target ps_render >> "$OUT/cmake.log"

if [ "$RECORD" = 1 ]; then
    echo "== Recording the songs' exports (app test SongBenchExport: $STYLES)"
    rm -rf "$OUT/cmds"
    gradle :app:testDebugUnitTest --no-configuration-cache --tests 'com.pocketstudio.app.bench.SongBenchExport' --rerun \
        -Pbench.out="$OUT/cmds" -Pbench.render="$TOOLS/ps_render" -Pbench.styles="$STYLES" > "$OUT/gradle-record.log" 2>&1 \
        || { tail -40 "$OUT/gradle-record.log"; exit 1; }
fi

echo "== Cases (pairwise)"
python3 -I "$ROOT/scripts/bench/pairwise.py" --check > "$OUT/cases.tsv"
CASES="$OUT/cases"
rm -rf "$CASES" "$OUT/stems"
mkdir -p "$CASES" "$OUT/stems"
export OUT TOOLS CASES

# One case: the render (and the M4A's encoding loop) into $CASES/<case>.<wav|m4a>, the report beside it.
render_case() {
    IFS=$'\t' read -r id style format rate bits target ceiling <<< "$1"
    cmd="$OUT/cmds/$style.pscmd"
    [ -f "$cmd" ] || { echo "FAILED: $id: no recording for $style" >&2; return 1; }
    args=()
    case "$target" in
        spotify) args+=(--whole-song --target -14) ;;
        apple) args+=(--whole-song --target -16) ;;
        club) args+=(--whole-song --target -9) ;;
        free) args+=(--whole-song --target -11) ;;
        style) ;;  # the song's own « Volume visé » (the Drop)
        none) args+=(--no-limiter) ;;
    esac
    # Every other 16-bit case with the shaped dither.
    [ "$bits" = 16 ] && [ $(( ${id#c} % 2 )) = 0 ] && args+=(--shape)
    if [ "$format" = m4a ]; then
        # As the app: the limiter 1 dB under the ceiling, the drive found at the ceiling, encoded from 16
        # bits; decoded over the ceiling, rendered again lower (3 renders), then encoded lower.
        lim="$(python3 -I -c "import sys; print(float(sys.argv[1]) - 1.0)" "$ceiling")"
        gain=1
        for render in 1 2 3; do
            "$TOOLS/ps_render" "${args[@]}" --ceiling "$lim" --drive-ceiling "$ceiling" --rate "$rate" --bits 24 \
                --report "$CASES/$id.json" "$cmd" "$CASES/$id.src.wav" || { echo "FAILED: $id (render)" >&2; return 1; }
            ffmpeg -nostats -hide_banner -loglevel error -y -i "$CASES/$id.src.wav" -c:a pcm_s16le "$CASES/$id.16.wav"
            fdkaac -S -b 256000 -o "$CASES/$id.m4a" "$CASES/$id.16.wav" 2> /dev/null
            tp="$(ffmpeg -nostats -hide_banner -i "$CASES/$id.m4a" -af ebur128=peak=true -f null - 2>&1 | awk '/^ *Peak:/ {p=$2} END {print p}')"
            lim="$(python3 -I -c "import sys; t,c,l=map(float,sys.argv[1:4]); print(round(l-(t-c+0.2),2) if t>c else 'ok')" "$tp" "$ceiling" "$lim")"
            [ "$lim" = ok ] && break
        done
        if [ "$lim" != ok ]; then
            for again in 1 2 3; do
                gain="$(python3 -I -c "import sys; t,c,g=map(float,sys.argv[1:4]); print(g*10**((c-0.3-t)/20) if t>c else 0)" "$tp" "$ceiling" "$gain")"
                [ "$gain" = 0 ] && break
                ffmpeg -nostats -hide_banner -loglevel error -y -i "$CASES/$id.src.wav" -af "volume=$gain" -c:a pcm_s16le "$CASES/$id.16.wav"
                fdkaac -S -b 256000 -o "$CASES/$id.m4a" "$CASES/$id.16.wav" 2> /dev/null
                tp="$(ffmpeg -nostats -hide_banner -i "$CASES/$id.m4a" -af ebur128=peak=true -f null - 2>&1 | awk '/^ *Peak:/ {p=$2} END {print p}')"
            done
        fi
        rm -f "$CASES/$id.src.wav" "$CASES/$id.16.wav"
    else
        [ "$target" = none ] || args+=(--ceiling "$ceiling")
        "$TOOLS/ps_render" "${args[@]}" --rate "$rate" --bits "$bits" --report "$CASES/$id.json" "$cmd" "$CASES/$id.wav" \
            || { echo "FAILED: $id (render)" >&2; return 1; }
    fi
    echo "  $id done ($style $format $rate/$bits $target $ceiling)"
}
export -f render_case

echo "== Rendering ($JOBS at a time)"
tail -n +2 "$OUT/cases.tsv" | while IFS= read -r line; do
    id="${line%%$'\t'*}"
    if [ -z "$ONLY" ] || [[ ",$ONLY," == *",$id,"* ]]; then printf '%s\0' "$line"; fi
done | xargs -0 -P "$JOBS" -I{} bash -c 'render_case "$1"' _ {}

echo "== Stems (sum against the mix without the master)"
# The first 40 s of each song (the RENDER line shortened): each strip alone with its sends, or (every
# other style) each strip dry plus the reverb's and the echo's returns alone.
stem_style() {
    style="$1"; mode="$2"
    dir="$OUT/stems/$style"
    mkdir -p "$dir"
    awk '/^RENDER /{ $3 = ($3 > 1920000 ? 1920000 : $3) } {print}' "$OUT/cmds/$style.pscmd" > "$OUT/cmds/$style.short.pscmd"
    cmd="$OUT/cmds/$style.short.pscmd"
    "$TOOLS/ps_render" --no-limiter --no-master-fx --bits 32 "$cmd" "$dir/mix.wav"
    for ch in 0 1 2 3 5 6 7 8 9 10 11 12 13 14 15 32 33 34 35; do
        if [ "$mode" = sends ]; then
            "$TOOLS/ps_render" --no-limiter --no-master-fx --bits 24 --solo "$ch" "$cmd" "$dir/stem-$ch.wav"
        else
            "$TOOLS/ps_render" --no-limiter --no-master-fx --bits 24 --stem-mode 1 --solo "$ch" "$cmd" "$dir/stem-$ch.wav"
        fi
    done
    if [ "$mode" = dry ]; then
        "$TOOLS/ps_render" --no-limiter --no-master-fx --bits 24 --stem-mode 2 "$cmd" "$dir/stem-reverb.wav"
        "$TOOLS/ps_render" --no-limiter --no-master-fx --bits 24 --stem-mode 3 "$cmd" "$dir/stem-echo.wav"
    fi
    echo "$mode" > "$dir/mode.txt"
    echo "  stems of $style ($mode) done"
}
export -f stem_style
k=0
for s in ${STYLES//,/ }; do
    k=$((k + 1))
    [ -f "$OUT/cmds/$s.pscmd" ] || continue
    printf '%s\0' "$s $([ $((k % 2)) = 1 ] && echo sends || echo dry)"
done | xargs -0 -P "$JOBS" -I{} bash -c 'stem_style $1' _ {}

echo "== Encoding and tagging as the app (ExportPairsTest: FlacEncoder, Metadata)"
gradle :app:testDebugUnitTest --no-configuration-cache --tests 'com.pocketstudio.app.export.ExportPairsTest' --rerun \
    -Pbench.out="$OUT" > "$OUT/gradle-encode.log" 2>&1 || { tail -40 "$OUT/gradle-encode.log"; exit 1; }

echo "== Checking with ffmpeg"
python3 -I "$ROOT/scripts/bench/matrix_check.py" "$OUT"
