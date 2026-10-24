#!/usr/bin/env bash
# The song bench (docs/plan/son.md § 2): the assistant's songs exported by the app's real code,
# replayed through the engine core on Linux, measured, and summed up in a Markdown report.
#
#   scripts/song-bench.sh [--styles a,b,c] [--out DIR] [--ffmpeg] [--before FILE.json] [--no-record] [--jobs N]
#                         [--premaster] [--m4a] [--keep-wav] [--kit3]
#
#   --styles   only these styles (ids of scripts/packs/recipes); default: all of them
#   --out      work folder (default build/bench): cmds/ (.pscmd + samples), wav/, json/, render/ (ps_render's
#              reports: the loudness target's drive), report.md, bench.json
#   --ffmpeg   cross-checks LUFS-I and true peak with ffmpeg's ebur128 (fails beyond ±0.1), and encodes each
#              song as the app's M4A, decoded again under -1 dBTP (scripts/bench/m4a_check.py, m4a.txt)
#   --before   a previous bench.json: the report shows the change against it
#   --no-record  keeps the .pscmd files already in the work folder (engine-only change)
#   --title    the report's title line
#   --premaster  also renders each song before the master chain (ps_render --no-limiter) and measures it:
#              the arrangement's own section contrast, next to the exported one (json-pre/)
#   --m4a      also renders each song as the app's M4A does (ceiling -2 dBTP), encodes it with ffmpeg's AAC
#              at 256 kbit/s, decodes it and measures its true peak (json-m4a/). ffmpeg's « fast » coder: its
#              default « twoloop » leaves isolated glitches up to +6 dB over the source on dense material
#              (an encoder fault: the phone encodes with MediaCodec)
#   --keep-wav keeps the WAVs (by default each is deleted once measured: 37 long songs take about 4 GB;
#              --ffmpeg keeps them for its cross-check)
#   --kit3     renders the recordings with kit v3 (the line C 100 3.0 appended to a copy of each .pscmd)
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
PREMASTER=0
M4A=0
KEEP=0
KIT3=0
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
        --premaster) PREMASTER=1; shift ;;
        --m4a) M4A=1; shift ;;
        --keep-wav) KEEP=1; shift ;;
        --kit3) KIT3=1; shift ;;
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
rm -rf "$OUT/wav" "$OUT/json" "$OUT/render" "$OUT/json-pre" "$OUT/json-m4a" "$OUT/hash"
mkdir -p "$OUT/wav" "$OUT/json" "$OUT/render" "$OUT/hash"
[ "$PREMASTER" = 1 ] && mkdir -p "$OUT/json-pre"
[ "$M4A" = 1 ] && mkdir -p "$OUT/json-m4a"
[ "$FFMPEG" = 1 ] && KEEP=1
list() {
    for f in "$OUT"/cmds/*.pscmd; do
        s="$(basename "$f" .pscmd)"
        if [ -z "$STYLES" ] || [[ ",$STYLES," == *",$s,"* ]]; then echo "$s"; fi
    done
}
# --kit3: copies of the recordings with the kit switched to v3 (the samples are shared: same folder).
if [ "$KIT3" = 1 ]; then
    for s in $(list); do
        # The kit version line after the recording's own (before its RENDER line, at once).
        awk '/^RENDER /{print "AT -1"; print "C 100 3.0 0 0"} {print}' "$OUT/cmds/$s.pscmd" > "$OUT/cmds/$s.kit3"
    done
fi
export OUT TOOLS PREMASTER M4A KEEP KIT3
# A recording that names its lead's mixer channel (META lead, S4) also renders the lead alone: the
# report reads how much its brightness moves along the song (ps_measure --lead).
list | xargs -P "$JOBS" -I{} sh -c '
    cmd="$OUT/cmds/{}.pscmd"
    [ "$KIT3" = 1 ] && cmd="$OUT/cmds/{}.kit3"
    lead="$(sed -n "s/^META lead //p" "$cmd" | head -1)"
    leadArgs=""
    if [ -n "$lead" ]; then
        "$TOOLS/ps_render" --solo "$lead" "$cmd" "$OUT/wav/{}.lead.wav" || { echo "FAILED: {} (lead)" >&2; exit 255; }
        leadArgs="--lead $OUT/wav/{}.lead.wav"
    fi
    "$TOOLS/ps_render" --report "$OUT/render/{}.json" "$cmd" "$OUT/wav/{}.wav" && "$TOOLS/ps_measure" "$OUT/wav/{}.wav" "$cmd" $leadArgs > "$OUT/json/{}.json" \
        || { echo "FAILED: {}" >&2; exit 255; }
    rm -f "$OUT/wav/{}.lead.wav"
    (cd "$OUT/wav" && sha256sum "{}.wav" > "$OUT/hash/{}.txt")
    [ "$KEEP" = 1 ] || rm -f "$OUT/wav/{}.wav"
    if [ "$PREMASTER" = 1 ]; then
        "$TOOLS/ps_render" --no-limiter "$cmd" "$OUT/wav/{}.pre.wav" && "$TOOLS/ps_measure" "$OUT/wav/{}.pre.wav" "$cmd" > "$OUT/json-pre/{}.json" \
            || { echo "FAILED: {} (pre-master)" >&2; exit 255; }
        rm -f "$OUT/wav/{}.pre.wav"
    fi
    if [ "$M4A" = 1 ]; then
        "$TOOLS/ps_render" --ceiling -2 "$cmd" "$OUT/wav/{}.m4a.wav" \
            && ffmpeg -nostats -hide_banner -loglevel error -y -i "$OUT/wav/{}.m4a.wav" -c:a aac -aac_coder fast -b:a 256k "$OUT/wav/{}.m4a" \
            && ffmpeg -nostats -hide_banner -loglevel error -y -i "$OUT/wav/{}.m4a" -c:a pcm_f32le "$OUT/wav/{}.dec.wav" \
            && "$TOOLS/ps_measure" "$OUT/wav/{}.dec.wav" > "$OUT/json-m4a/{}.first.json" \
            || { echo "FAILED: {} (m4a)" >&2; exit 255; }
        # As the app does (AacEncoder.encodeChecked): over -1 dBTP decoded, encoded again lower (4 encodes at most).
        cp "$OUT/json-m4a/{}.first.json" "$OUT/json-m4a/{}.json"
        for again in 1 2 3; do
            gain="$(python3 -I -c "import json,sys; t=json.load(open(sys.argv[1]))[\"true_peak\"]; g=float(sys.argv[2]); a=int(sys.argv[3]); print(g * 10 ** ((-1.5 - 0.5 * (a - 1) - t) / 20) if t > -1.0 else 0)" "$OUT/json-m4a/{}.json" "${gain:-1}" "$again")"
            [ "$gain" = 0 ] && break
            ffmpeg -nostats -hide_banner -loglevel error -y -i "$OUT/wav/{}.m4a.wav" -af "volume=$gain" -c:a aac -aac_coder fast -b:a 256k "$OUT/wav/{}.m4a" \
                && ffmpeg -nostats -hide_banner -loglevel error -y -i "$OUT/wav/{}.m4a" -c:a pcm_f32le "$OUT/wav/{}.dec.wav" \
                && "$TOOLS/ps_measure" "$OUT/wav/{}.dec.wav" > "$OUT/json-m4a/{}.json" \
                || { echo "FAILED: {} (m4a again)" >&2; exit 255; }
        done
        rm -f "$OUT/wav/{}.m4a.wav" "$OUT/wav/{}.m4a" "$OUT/wav/{}.dec.wav"
    fi'
cat $(ls "$OUT"/hash/*.txt | sort) > "$OUT/sha256.txt"

if [ "$FFMPEG" = 1 ]; then
    echo "== Cross-check with ffmpeg ebur128"
    : > "$OUT/ffmpeg.txt"
    for s in $(list); do
        summary="$(ffmpeg -nostats -hide_banner -i "$OUT/wav/$s.wav" -af ebur128=peak=true -f null - 2>&1 | sed -n '/Summary:/,$p')"
        i="$(echo "$summary" | awk '/^ *I:/ {print $2}')"
        tp="$(echo "$summary" | awk '/^ *Peak:/ {print $2}')"
        echo "$s $i $tp" >> "$OUT/ffmpeg.txt"
    done
    # Phase 38: each song as the app's M4A (source at -2 dBTP, AAC 256 kbit/s; when its decoded true
    # peak goes over -1 dBTP, rendered again with the limiter lower, or encoded lower): fails when one stays over.
    echo "== M4A decoded again (ffmpeg's AAC)"
    python3 -I "$ROOT/scripts/bench/m4a_check.py" "$OUT/m4a.txt" --render "$TOOLS/ps_render" --cmds "$OUT/cmds" \
        $(for s in $(list); do echo "$OUT/wav/$s.wav"; done)
fi

echo "== Report"
python3 -I "$ROOT/scripts/bench/report.py" "$OUT/json" --out "$OUT/report.md" --json "$OUT/bench.json" \
    --hashes "$OUT/sha256.txt" --renders "$OUT/render" ${BEFORE:+--before "$BEFORE"} ${TITLE:+--title "$TITLE"} \
    $([ "$PREMASTER" = 1 ] && echo --premaster "$OUT/json-pre") $([ "$M4A" = 1 ] && echo --m4a "$OUT/json-m4a") \
    $([ "$FFMPEG" = 1 ] && echo --ffmpeg "$OUT/ffmpeg.txt")
echo "Report: $OUT/report.md"
