#!/usr/bin/env bash
# Rebuilds the separation model exactly: HT-Demucs v4 (Meta, MIT) exported to ONNX with the
# spectrogram moved outside the network by demucs.onnx (S. Sevagh, MIT). Pinned versions make
# the output byte-identical, so the app can pin its SHA-256.
set -euo pipefail
OUT="${1:?output dir}"
DEMUCS_ONNX_COMMIT=81fa192e6fcc88e35e887f6e6ccce91227f4e6f5
EXPECTED_SHA256=ba2224192729d6992c64a8cc440f1b9977b84519247b3f6c459c57cd35f9597d
WEIGHTS_SHA256=$(cat "$(dirname "$0")/weights.sha256")
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
git clone -q https://github.com/sevagh/demucs.onnx "$WORK/demucs.onnx"
git -C "$WORK/demucs.onnx" checkout -q "$DEMUCS_ONNX_COMMIT"
python -m venv "$WORK/venv"
. "$WORK/venv/bin/activate"
pip install -q --extra-index-url https://download.pytorch.org/whl/cpu -r "$HERE/requirements-demucs.txt"
pip install -q --no-deps -e "$WORK/demucs.onnx/demucs-for-onnx"
# Official weights, checked before use.
mkdir -p ~/.cache/torch/hub/checkpoints
curl -fsSL --retry 3 -o ~/.cache/torch/hub/checkpoints/955717e8-8726e21a.th https://dl.fbaipublicfiles.com/demucs/hybrid_transformer/955717e8-8726e21a.th
echo "$WEIGHTS_SHA256  $HOME/.cache/torch/hub/checkpoints/955717e8-8726e21a.th" | sha256sum -c -
(cd "$WORK/demucs.onnx" && python scripts/convert-pth-to-onnx.py "$WORK/out")
mkdir -p "$OUT"
cp "$WORK/out/htdemucs.onnx" "$OUT/htdemucs.onnx"
cp "$WORK/demucs.onnx/LICENSE" "$OUT/LICENSE-demucs.onnx.txt"
cp "$WORK/demucs.onnx/demucs-for-onnx/LICENSE" "$OUT/LICENSE-demucs.txt"
echo "$EXPECTED_SHA256  $OUT/htdemucs.onnx" | sha256sum -c -
