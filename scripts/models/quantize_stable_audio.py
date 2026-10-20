"""Shrinks Stable Audio Open Small for phones: int8 weights where it does not change the sound.

- cond (T5 text encoder): every MatMul/Gemm to int8 (no measurable change).
- dit (diffusion transformer): only the weight MatMuls inside the transformer layers, per channel.
  Embeddings, input/output projections and the attention score products stay fp32: quantizing
  them changed the spectrum more than a different random seed does.
- dec (autoencoder decoder): unchanged.
"""
import sys

import onnx
from onnxruntime.quantization import QuantType, quantize_dynamic

src, out = sys.argv[1], sys.argv[2]
quantize_dynamic(f"{src}/cond.onnx", f"{out}/cond_q8.onnx", weight_type=QuantType.QInt8, op_types_to_quantize=["MatMul", "Gemm"])
model = onnx.load(f"{src}/dit.onnx", load_external_data=False)
weights = {i.name for i in model.graph.initializer}
layers = [n.name for n in model.graph.node
          if n.op_type in ("MatMul", "Gemm") and "/transformer/layers." in n.name and any(i in weights for i in n.input)]
quantize_dynamic(f"{src}/dit.onnx", f"{out}/dit_l8.onnx", weight_type=QuantType.QInt8, nodes_to_quantize=layers, per_channel=True)
