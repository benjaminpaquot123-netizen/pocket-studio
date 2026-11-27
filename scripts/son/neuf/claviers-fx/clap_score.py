"""Optional CLAP check: cosine similarity between each MP3 and a text prompt
describing its style and family (LAION-CLAP, music checkpoint). There is no
CLAP fingerprint of the families, so this only says "sounds like the words".

Usage: python -I clap_score.py --ckpt music_audioset_epoch_15_esc_90.14.pt --out DIR
Adds "clap_text_sim" to DIR/results.jsonl (rewritten in place).
"""
import argparse
import json
import os

STYLE = {"pop": "pop", "lofi": "lo-fi hip hop", "house": "house", "trap": "trap",
         "edm": "EDM future bass", "jazz-soul": "jazz soul", "rnb": "R&B"}
FAM = {"piano": "piano chords loop", "piano-electrique": "electric piano loop",
       "orgue": "organ loop", "effet": "sound effect", "voix": "vocal chops"}
KIND = {"riser": "riser sound effect", "impact": "impact hit sound effect",
        "downlifter": "downlifter sound effect", "sweep": "white noise sweep"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ckpt", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    import numpy as np
    import torch
    import laion_clap
    _load = torch.load
    torch.load = lambda *x, **k: _load(*x, **{**k, "weights_only": False})
    m = laion_clap.CLAP_Module(enable_fusion=False, amodel="HTSAT-base")
    m.load_ckpt(a.ckpt)
    path = os.path.join(a.out, "results.jsonl")
    recs = [json.loads(l) for l in open(path)]
    texts = []
    for r in recs:
        f = KIND.get(r.get("meta", {}).get("kind"), FAM[r["famille"]])
        texts.append("a %s %s" % (STYLE[r["style"]], f))
    files = [os.path.join(a.out, r["file"]) for r in recs]
    ae = []
    for i in range(0, len(files), 8):
        ae.append(m.get_audio_embedding_from_filelist(x=files[i:i + 8], use_tensor=False))
    ae = np.concatenate(ae)
    te = m.get_text_embedding(texts, use_tensor=False)
    ae /= np.linalg.norm(ae, axis=1, keepdims=True)
    te /= np.linalg.norm(te, axis=1, keepdims=True)
    for r, x, t, p in zip(recs, ae, te, texts):
        r["clap_text_sim"] = round(float(x @ t), 3)
        r["clap_prompt"] = p
    with open(path, "w") as f:
        for r in recs:
            f.write(json.dumps(r) + "\n")


if __name__ == "__main__":
    main()
