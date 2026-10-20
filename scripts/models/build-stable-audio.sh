#!/usr/bin/env bash
# Rebuilds the on-device generation model: Stable Audio Open Small (Stability AI Community
# License), as exported to ONNX by atauser001 (fp32, pinned revision, checked), then shrunk by
# quantize_stable_audio.py. Pinned versions make the output byte-identical, so the app can pin
# its SHA-256.
set -euo pipefail
OUT="${1:?output dir}"
REPO=https://huggingface.co/atauser001/stable-audio-open-small-onnx/resolve/8f9544df5933b9ed4a7e8ed7fa6f77b3dd48ddce
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
for f in cond.onnx dit.onnx dec.onnx tokenizer.json LICENSE NOTICE; do
    curl -fsSL --retry 3 -o "$WORK/$f" "$REPO/$f"
done
(cd "$WORK" && sha256sum -c - <<'SUMS'
847822809975d0c36a665594fad0312c4c225147910657820e9936b09014629c  cond.onnx
8e9c8b14baac4a26159d5ecf3d365ef5a097f321541f9548d62d136354a23d8b  dit.onnx
96029859b4d563f3aff266e97e715190e8d53278c6e27ab94095535a0da4c911  dec.onnx
d2acde0d8d71dd30a711834b07781b9c89feaac33fd332f60507699282740066  tokenizer.json
d6f6b1a4dce5c852bd6d7d9482d002baf0ccdb71e662250b73be9eec8764ee8d  LICENSE
SUMS
)
python -m venv "$WORK/venv"
. "$WORK/venv/bin/activate"
pip install -q -r "$HERE/requirements-stable-audio.txt"
mkdir -p "$OUT"
python "$HERE/quantize_stable_audio.py" "$WORK" "$OUT"
cp "$WORK/dec.onnx" "$WORK/tokenizer.json" "$WORK/LICENSE" "$OUT/"
# The licence asks derivative works to say what changed.
{ cat "$WORK/NOTICE"; printf '\n\nFurther changes by Pocket Studio\n================================\n'
  printf 'cond_q8.onnx: every MatMul/Gemm weight quantized to int8 (ONNX Runtime 1.31 dynamic quantization).\n'
  printf 'dit_l8.onnx: the weight MatMuls of the transformer layers quantized to int8 per channel; the rest unchanged.\n'
  printf 'dec.onnx and tokenizer.json: unchanged.\n'; } > "$OUT/NOTICE"
(cd "$OUT" && sha256sum -c - <<'SUMS'
31952d152e570649ef9e49882ea8a41d25a2746f2f6184c607e871b3ae636c82  cond_q8.onnx
ee73eb76a354a3bb0676bd185956bb78d169f8535a0e23188212f0cc002bcf08  dit_l8.onnx
96029859b4d563f3aff266e97e715190e8d53278c6e27ab94095535a0da4c911  dec.onnx
d2acde0d8d71dd30a711834b07781b9c89feaac33fd332f60507699282740066  tokenizer.json
d6f6b1a4dce5c852bd6d7d9482d002baf0ccdb71e662250b73be9eec8764ee8d  LICENSE
556b1b69ff89db713abcd7aebac1c5fd0e81d24161a6d4dfb4960b17939251f0  NOTICE
SUMS
)
