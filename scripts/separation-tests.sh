#!/usr/bin/env bash
# Runs the real separation and transcription models on Linux (needs ~200 MB of downloads, cached
# under build/). Basic Pitch always runs (its model is in the app assets); the Demucs part is
# skipped (exit 0) when its model release does not exist yet.
set -euo pipefail
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
"$ROOT/build/separation-tests/pitch_test" "$ROOT/app/src/main/assets/basic-pitch/nmp.onnx"

MODEL="${SEPARATION_MODEL:-$CACHE/htdemucs.onnx}"
if [ ! -f "$MODEL" ]; then
    if ! curl -fsSL -o "$MODEL.part" "$MODEL_URL"; then
        echo "Model release not published yet: separation test skipped."
        rm -f "$MODEL.part"; exit 0
    fi
    mv "$MODEL.part" "$MODEL"
fi
echo "$MODEL_SHA256  $MODEL" | sha256sum -c -
"$ROOT/build/separation-tests/separation_test" "$MODEL"
