#!/usr/bin/env bash
# « ecoute » (docs/audits/AUDIT.md § 3 and étage 4): a song rendered by the real engine (ps_render,
# the engine core as the app's export runs it), then measured for an engineer's report: loudness
# (LUFS-I, LRA), true peak, sample peak and clipped samples, DC, stereo (correlation, mono loss),
# bands and slope (ps_measure), a spectrogram (PNG, ffmpeg) and, with --against, the gap to a master
# sound (the null residual: peak and RMS of the difference, in dBFS, after lining the two up).
#
#   scripts/ecoute.sh [--out DIR] [--masters] [--against REF.wav] [--name NAME] <song.pscmd | song.wav> [-- ps_render options]
#
#   <song.pscmd>  an export recorded by the app (SongBenchExport, RealProjectsTest, export-matrix):
#                 rendered as recorded (« export »); with --masters also as the Master pro's
#                 « Spotify » (whole song at -14 LUFS) and « Club » (whole song at -9 LUFS) exports.
#   <song.wav>    a file already rendered: measured only.
#   --against     a reference render (e.g. the same .pscmd through an earlier engine): the residual of
#                 each render against it (--against-dir DIR: DIR/<name>-<kind>.wav for each kind).
#   --out         the work folder (default build/ecoute): <name>-<kind>.wav, .json, .png and report.md.
#                 Renders are big (a 3-minute song is ~70 MB in float): --keep-wav keeps them, else
#                 each WAV is deleted once measured (the reference ones are never touched).
# Environment: TOOLS (ps_render and ps_measure already built), CMAKE_EXTRA_ARGS.
# What this cannot say: how it sounds. The report lists what to listen to; the ear is Benjamin's.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/ecoute"
MASTERS=0
AGAINST=""
AGAINST_DIR=""
NAME=""
KEEP=0
IN=""
EXTRA=()
while [ $# -gt 0 ]; do
    case "$1" in
        --out) OUT="$2"; shift 2 ;;
        --masters) MASTERS=1; shift ;;
        --against) AGAINST="$2"; shift 2 ;;
        --against-dir) AGAINST_DIR="$2"; shift 2 ;;
        --name) NAME="$2"; shift 2 ;;
        --keep-wav) KEEP=1; shift ;;
        --) shift; EXTRA=("$@"); break ;;
        -*) echo "unknown option $1" >&2; exit 1 ;;
        *) IN="$1"; shift ;;
    esac
done
[ -n "$IN" ] && [ -f "$IN" ] || { echo "usage: scripts/ecoute.sh [options] <song.pscmd | song.wav>" >&2; exit 1; }
command -v ffmpeg > /dev/null || { echo "ffmpeg is needed (the spectrogram)" >&2; exit 1; }
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
[ -n "$NAME" ] || NAME="$(basename "${IN%.*}")"

if [ -z "${TOOLS:-}" ]; then
    TOOLS="$ROOT/build/ecoute/tools"
    cmake -S "$ROOT/engine/bench" -B "$TOOLS" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-} > "$OUT/cmake.log"
    cmake --build "$TOOLS" -j "$(nproc 2>/dev/null || echo 2)" --target ps_render ps_measure >> "$OUT/cmake.log"
fi

KINDS=()
case "$IN" in
    *.pscmd)
        KINDS=(export)
        [ "$MASTERS" = 1 ] && KINDS+=(spotify club)
        for kind in "${KINDS[@]}"; do
            args=()
            case "$kind" in
                spotify) args=(--whole-song --target -14) ;;
                club) args=(--whole-song --target -9) ;;
            esac
            echo "== $NAME: rendering « $kind »"
            "$TOOLS/ps_render" "${args[@]}" "${EXTRA[@]}" --report "$OUT/$NAME-$kind.render.json" "$IN" "$OUT/$NAME-$kind.wav"
        done
        ;;
    *.wav)
        KINDS=(file)
        cp "$IN" "$OUT/$NAME-file.wav"
        ;;
    *) echo "a .pscmd or a .wav, please" >&2; exit 1 ;;
esac

for kind in "${KINDS[@]}"; do
    wav="$OUT/$NAME-$kind.wav"
    echo "== $NAME « $kind »: measuring"
    pscmd=()
    [[ "$IN" == *.pscmd ]] && pscmd=("$IN")
    "$TOOLS/ps_measure" "$wav" "${pscmd[@]}" > "$OUT/$NAME-$kind.json"
    # The spectrogram: log frequency axis, 20 Hz to 24 kHz, the whole song.
    ffmpeg -nostdin -v error -y -i "$wav" -lavfi "showspectrumpic=s=1600x600:mode=combined:scale=log:fscale=log:legend=1:color=intensity" \
        "$OUT/$NAME-$kind.png"
    ref=""
    if [ -n "$AGAINST" ]; then ref="$AGAINST"; elif [ -n "$AGAINST_DIR" ] && [ -f "$AGAINST_DIR/$NAME-$kind.wav" ]; then ref="$AGAINST_DIR/$NAME-$kind.wav"; fi
    python3 -I "$ROOT/scripts/bench/ecoute_report.py" --entry "$OUT" "$NAME" "$kind" "$wav" ${ref:+"$ref"}
    [ "$KEEP" = 1 ] || rm -f "$wav"
done
python3 -I "$ROOT/scripts/bench/ecoute_report.py" --report "$OUT"
echo "Ecoute: $OUT/report.md"
