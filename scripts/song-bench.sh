#!/usr/bin/env bash
# The song bench (docs/plan/son.md § 2): the assistant's songs exported by the app's real code,
# replayed through the engine core on Linux, measured, and summed up in a Markdown report.
#
#   scripts/song-bench.sh [--styles a,b,c] [--out DIR] [--ffmpeg] [--before FILE.json] [--no-record] [--jobs N]
#                         [--premaster] [--m4a] [--keep-wav] [--kit3] [--no-bass] [--guide accept|first]
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
#   --m4a      also renders each song as the app's M4A does (ceiling -2 dBTP standing for the WAV's -1, rendered
#              again lower while the decoded file is over -1 dBTP), encodes it in AAC at 256 kbit/s with FDK AAC
#              (fdkaac, Android's encoder family; S6) or else ffmpeg's « fast » coder, decodes it and measures it
#              (json-m4a/). ffmpeg's default « twoloop » leaves isolated glitches up to +6 dB on dense material
#   --keep-wav keeps the WAVs (by default each is deleted once measured: 37 long songs take about 4 GB;
#              --ffmpeg keeps them for its cross-check)
#   --kit3     renders the recordings with kit v3 (the line C 100 3.0 appended to a copy of each .pscmd)
#   --guide    records each song once followed to the end of its Guide's parcours (G6, GuideFollow): « accept »
#              takes each card's first option that changes the song, « first » each card's first option
#   --no-bass  skips the bass pass. By default each song that names its basses (META bass) also renders
#              them alone before the master chain (ps_render --no-limiter --solo), the whole song before it,
#              the kick alone (META kick) and, when it has a bass exciter (« Basse audible »), the song before
#              the master without it: ps_measure --bass reads what a phone keeps of the bass
#              (docs/hits/basses-qui-tapent.md § 8, B1-B12)
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
BASS=1
GUIDE=""
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
        --no-bass) BASS=0; shift ;;
        --guide) GUIDE="$2"; shift 2 ;;
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
        -Pbench.out="$OUT/cmds" -Pbench.render="$TOOLS/ps_render" ${STYLES:+-Pbench.styles="$STYLES"} ${GUIDE:+-Pbench.guide="$GUIDE"}) > "$OUT/gradle.log" 2>&1 \
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
export OUT TOOLS PREMASTER M4A KEEP KIT3 BASS
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
    # The bass pass: the basses alone and the song before the master (with and without the exciter), the kick alone.
    bass="$(sed -n "s/^META bass //p" "$cmd" | head -1)"
    kick="$(sed -n "s/^META kick //p" "$cmd" | head -1)"
    bassArgs=""
    if [ "$BASS" = 1 ] && [ -n "$bass" ]; then
        "$TOOLS/ps_render" --no-limiter --solo "$bass" "$cmd" "$OUT/wav/{}.bass.wav" \
            && "$TOOLS/ps_render" --no-limiter "$cmd" "$OUT/wav/{}.pre-b.wav" || { echo "FAILED: {} (bass)" >&2; exit 255; }
        bassArgs="--bass $OUT/wav/{}.bass.wav --pre $OUT/wav/{}.pre-b.wav"
        if [ -n "$kick" ]; then
            "$TOOLS/ps_render" --no-limiter --solo "$kick" "$cmd" "$OUT/wav/{}.kick.wav" || { echo "FAILED: {} (kick)" >&2; exit 255; }
            bassArgs="$bassArgs --kick $OUT/wav/{}.kick.wav"
        fi
        # SetInsertType (33) of the bass exciter (InsertSlot::BassExciterFx, 29): the song without it too.
        if grep -qE "^C 33 [^ ]+ [0-9]+ 29$" "$cmd"; then
            "$TOOLS/ps_render" --no-limiter --no-exciter "$cmd" "$OUT/wav/{}.dry.wav" || { echo "FAILED: {} (dry)" >&2; exit 255; }
            bassArgs="$bassArgs --dry $OUT/wav/{}.dry.wav"
        fi
    fi
    "$TOOLS/ps_render" --report "$OUT/render/{}.json" "$cmd" "$OUT/wav/{}.wav" && "$TOOLS/ps_measure" "$OUT/wav/{}.wav" "$cmd" $leadArgs $bassArgs > "$OUT/json/{}.json" \
        || { echo "FAILED: {}" >&2; exit 255; }
    rm -f "$OUT/wav/{}.lead.wav" "$OUT/wav/{}.bass.wav" "$OUT/wav/{}.pre-b.wav" "$OUT/wav/{}.kick.wav" "$OUT/wav/{}.dry.wav"
    (cd "$OUT/wav" && sha256sum "{}.wav" > "$OUT/hash/{}.txt")
    [ "$KEEP" = 1 ] || rm -f "$OUT/wav/{}.wav"
    if [ "$PREMASTER" = 1 ]; then
        "$TOOLS/ps_render" --no-limiter "$cmd" "$OUT/wav/{}.pre.wav" && "$TOOLS/ps_measure" "$OUT/wav/{}.pre.wav" "$cmd" > "$OUT/json-pre/{}.json" \
            || { echo "FAILED: {} (pre-master)" >&2; exit 255; }
        rm -f "$OUT/wav/{}.pre.wav"
    fi
    if [ "$M4A" = 1 ]; then
        # S6: as the app (RhythmViewModel.renderMix, M4aGuard): the song rendered with its limiter at -2 dBTP,
        # standing for its WAV (--drive-ceiling -1: the codec headroom off the peaks, not off the loudness),
        # encoded, decoded; over -1 dBTP, rendered again with the limiter lower by the excess + 0.2 dB
        # (3 renders at most), then encoded lower (the last resort). The encoder: FDK AAC (fdkaac, the
        # family of the MediaCodec encoder of Android) when installed, else the « fast » coder of ffmpeg (which
        # overshoots 2 to 4 dB more than the phone: the M4A comes out 2 LU too low).
        enc() {
            if command -v fdkaac > /dev/null; then
                ffmpeg -nostats -hide_banner -loglevel error -y -i "$1" -af "volume=$3" -c:a pcm_s16le "$OUT/wav/{}.m4a16.wav" \
                    && fdkaac -S -b 256000 -o "$2" "$OUT/wav/{}.m4a16.wav" && rm -f "$OUT/wav/{}.m4a16.wav"
            else
                ffmpeg -nostats -hide_banner -loglevel error -y -i "$1" -af "volume=$3" -c:a aac -aac_coder fast -b:a 256k "$2"
            fi
        }
        ceiling=-2
        for render in 1 2 3; do
            "$TOOLS/ps_render" --ceiling "$ceiling" --drive-ceiling -1 "$cmd" "$OUT/wav/{}.m4a.wav" \
                && enc "$OUT/wav/{}.m4a.wav" "$OUT/wav/{}.m4a" 1 \
                && ffmpeg -nostats -hide_banner -loglevel error -y -i "$OUT/wav/{}.m4a" -c:a pcm_f32le "$OUT/wav/{}.dec.wav" \
                && "$TOOLS/ps_measure" "$OUT/wav/{}.dec.wav" > "$OUT/json-m4a/{}.json" \
                || { echo "FAILED: {} (m4a)" >&2; exit 255; }
            [ "$render" = 1 ] && cp "$OUT/json-m4a/{}.json" "$OUT/json-m4a/{}.first.json"
            ceiling="$(python3 -I -c "import json,sys; t=json.load(open(sys.argv[1]))[\"true_peak\"]; c=float(sys.argv[2]); print(round(c - (t + 1.0 + 0.2), 2) if t > -1.0 else \"ok\")" "$OUT/json-m4a/{}.json" "$ceiling")"
            [ "$ceiling" = ok ] && break
        done
        # Still over: this mix encoded lower (3 encodes at most).
        if [ "$ceiling" != ok ]; then
            for again in 1 2 3; do
                gain="$(python3 -I -c "import json,sys; t=json.load(open(sys.argv[1]))[\"true_peak\"]; g=float(sys.argv[2]); print(g * 10 ** ((-1.3 - t) / 20) if t > -1.0 else 0)" "$OUT/json-m4a/{}.json" "${gain:-1}")"
                [ "$gain" = 0 ] && break
                enc "$OUT/wav/{}.m4a.wav" "$OUT/wav/{}.m4a" "$gain" \
                    && ffmpeg -nostats -hide_banner -loglevel error -y -i "$OUT/wav/{}.m4a" -c:a pcm_f32le "$OUT/wav/{}.dec.wav" \
                    && "$TOOLS/ps_measure" "$OUT/wav/{}.dec.wav" > "$OUT/json-m4a/{}.json" \
                    || { echo "FAILED: {} (m4a again)" >&2; exit 255; }
            done
        fi
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
    --hashes "$OUT/sha256.txt" --renders "$OUT/render" --cmds "$OUT/cmds" ${BEFORE:+--before "$BEFORE"} ${TITLE:+--title "$TITLE"} \
    $([ "$PREMASTER" = 1 ] && echo --premaster "$OUT/json-pre") $([ "$M4A" = 1 ] && echo --m4a "$OUT/json-m4a") \
    $([ "$FFMPEG" = 1 ] && echo --ffmpeg "$OUT/ffmpeg.txt")
echo "Report: $OUT/report.md"
