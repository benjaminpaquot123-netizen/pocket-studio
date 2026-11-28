"""Phase 1: generate raw candidates with Stable Audio Open Small (resumable).

usage: python -I gen.py CODE_DIR MODEL_DIR RAW_DIR [family ...]
Writes RAW_DIR/<style>-<family>-<i>.npy (float16 stereo, 44.1 kHz) and RAW_DIR/meta.jsonl.
"""
import json
import os
import sys

import numpy as np

CODE, MODEL, RAW = sys.argv[1:4]
sys.path.insert(0, CODE)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_pack as gp  # noqa: E402
import prompts as P  # noqa: E402

fams = sys.argv[4:] or list(P.FAMILIES)
os.makedirs(RAW, exist_ok=True)
model = gp.Model(MODEL)
meta = open(os.path.join(RAW, "meta.jsonl"), "a")
for fam in fams:
    f = P.FAMILIES[fam]
    for style in P.BPM:
        for i in range(f["n"]):
            name = f"{style}-{fam}-{i:02d}"
            path = os.path.join(RAW, name + ".npy")
            if os.path.exists(path):
                continue
            p = P.prompt(fam, style, i)
            sec = P.gen_seconds(fam, style)
            seed = 2000 + i + (0 if style == "dubstep" else 500)
            a = model.generate(p, sec, seed)
            np.save(path, a.astype(np.float16) if np.abs(a).max() < 60000 else a.astype(np.float32))
            meta.write(json.dumps(dict(name=name, fam=fam, style=style, i=i, prompt=p, seconds=sec, seed=seed,
                                       raw_peak=float(np.abs(a).max()))) + "\n")
            meta.flush()
            print(name, round(sec, 2), p, flush=True)
