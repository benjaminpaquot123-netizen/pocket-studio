#!/usr/bin/env bash
# Runs the real separation and transcription models on Linux (needs ~200 MB of downloads, cached
# under build/). Basic Pitch always runs (its model is in the app assets); the Demucs part is
# skipped (exit 0) when its model release does not exist yet.
#
# Given-file mode (lot C, the real-song check): `separation-tests.sh --file song.mp3 --out DIR`
# decodes the song with ffmpeg (stereo 44.1 kHz float) and separates it with the app's own code
# into DIR/<stem>.wav; nothing else runs. DIR must be outside the repository (copyrighted songs and
# their stems are never committed): scripts/voice-notes-check.py then reads DIR/vocals.wav.
set -euo pipefail
GIVEN=""
OUT=""
while [ $# -gt 0 ]; do
    case "$1" in
        --file) GIVEN="$2"; shift 2 ;;
        --out) OUT="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="$ROOT/build/separation-cache"
ORT_VERSION=1.31.0
MODEL_URL=https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/modele-htdemucs-v4-onnx-1/htdemucs.onnx
MODEL_SHA256=ba2224192729d6992c64a8cc440f1b9977b84519247b3f6c459c57cd35f9597d
mkdir -p "$CACHE"
if [ ! -d "$CACHE/onnxruntime-linux-x64-$ORT_VERSION" ]; then
    curl -fsSL "https://github.com/microsoft/onnxruntime/releases/download/v$ORT_VERSION/onnxruntime-linux-x64-$ORT_VERSION.tgz" | tar xz -C "$CACHE"
fi
cmake -S "$ROOT/separation/host-tests" -B "$ROOT/build/separation-tests" -DCMAKE_BUILD_TYPE=Release -DORT_DIR="$CACHE/onnxruntime-linux-x64-$ORT_VERSION" >/dev/null
cmake --build "$ROOT/build/separation-tests" -j >/dev/null
if [ -z "$GIVEN" ]; then
    "$ROOT/build/separation-tests/pitch_test" "$ROOT/app/src/main/assets/basic-pitch/nmp.onnx"
fi

MODEL="${SEPARATION_MODEL:-$CACHE/htdemucs.onnx}"
if [ ! -f "$MODEL" ]; then
    if ! curl -fsSL -o "$MODEL.part" "$MODEL_URL"; then
        echo "Model release not published yet: separation test skipped."
        rm -f "$MODEL.part"; exit 0
    fi
    mv "$MODEL.part" "$MODEL"
fi
echo "$MODEL_SHA256  $MODEL" | sha256sum -c -
if [ -n "$GIVEN" ]; then
    [ -f "$GIVEN" ] || { echo "no such song: $GIVEN" >&2; exit 2; }
    [ -n "$OUT" ] || { echo "--out DIR is required with --file" >&2; exit 2; }
    case "$(realpath -m "$OUT")/" in "$ROOT"/*) echo "--out must be outside the repository" >&2; exit 2 ;; esac
    mkdir -p "$OUT"
    ffmpeg -nostdin -loglevel error -y -i "$GIVEN" -f f32le -acodec pcm_f32le -ac 2 -ar 44100 "$OUT/song.raw"
    "$ROOT/build/separation-tests/separate_file" "$MODEL" "$OUT/song.raw" 2 44100 "$OUT"
    rm -f -- "$OUT/song.raw"
    exit 0
fi
"$ROOT/build/separation-tests/separation_test" "$MODEL"

# Stable Audio Open Small (858 MB): only when a directory with its files is given.
if [ -n "${STABLE_AUDIO_DIR:-}" ]; then
    "$ROOT/build/separation-tests/generate_test" "$STABLE_AUDIO_DIR"
fi
