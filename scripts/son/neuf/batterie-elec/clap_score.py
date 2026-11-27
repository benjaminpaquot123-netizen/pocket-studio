"""CLAP check (LAION-CLAP, music checkpoint): cosine between each MP3 and the
text "an electronic <family> drum sample", plus its rank among all family
prompts (1 = CLAP hears the intended family). No pack audio is needed.

  python clap_score.py OUT_DIR   (updates OUT_DIR/results.json in place)
"""
import json
import os
import sys

import librosa
import numpy as np
import laion_clap

PROMPTS = {"kick": "an electronic kick drum one-shot", "snare": "an electronic snare drum one-shot",
           "clap": "an electronic hand clap one-shot", "hat_closed": "a closed hi-hat one-shot",
           "hat_open": "an open hi-hat one-shot", "perc": "an electronic percussion hit",
           "loop": "an electronic drum loop"}


def main(out):
    res = json.load(open(os.path.join(out, "results.json")))
    m = laion_clap.CLAP_Module(enable_fusion=False, amodel="HTSAT-base")
    m.load_ckpt(os.environ.get("CLAP_CKPT"))
    keys = list(PROMPTS)
    t = m.get_text_embedding([PROMPTS[k] for k in keys], use_tensor=False)
    t /= np.linalg.norm(t, axis=1, keepdims=True)
    for r in res:
        y, _ = librosa.load(os.path.join(out, r["file"]), sr=48000, mono=True)
        a = m.get_audio_embedding_from_data(x=y[None, :].astype(np.float32), use_tensor=False)[0]
        a /= np.linalg.norm(a)
        sims = t @ a
        fam = r["family"]
        cands = keys if fam == "loop" else keys[:-1]
        order = sorted(cands, key=lambda k: -sims[keys.index(k)])
        r["clap_cos"] = round(float(sims[keys.index(fam)]), 3)
        r["clap_rank"] = order.index(fam) + 1
        print(r["file"], r["clap_cos"], r["clap_rank"], flush=True)
    json.dump(res, open(os.path.join(out, "results.json"), "w"), indent=1)


if __name__ == "__main__":
    main(sys.argv[1])
