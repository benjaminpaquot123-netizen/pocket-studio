"""Pick the best candidates, export MP3 192 kb/s (peak <= -1 dBFS), note them on the decoded MP3.

  PS_BASE=.../base.py python -I finalise.py pick DNA.tsv CANDS_DIR PICKS_DIR [KEEP]
  PS_BASE=.../base.py python -I finalise.py mp3  DNA.tsv PICKS_DIR MP3_DIR LOOPS.jsonl OUT.json

The note is the Essentia distance to the family DNA in standard deviations (0 = the median),
measured on the decoded MP3; librosa gives a counter-measure (centroid, RMS, duration).
"""
import glob
import json
import os
import subprocess
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import acou  # noqa: E402

FAM_DNA = {"kick": "drum_oneshot/kick", "caisse-claire": "drum_oneshot/snare", "tom": "drum_oneshot/tom",
           "charleston-ferme": "drum_oneshot/hat_closed", "charleston-ouvert": "drum_oneshot/hat_open",
           "crash": "drum_oneshot/crash", "ride": "drum_oneshot/ride", "boucle": "drum_loop/drums"}
SOURCE = "Virtuosity Drums (sfzinstruments/virtuosity_drums, enregistré chez Virtuosity Musical Instruments, Boston), CC0 1.0"


def pick(dna_p, cands, picks, keep=4):
    import shutil
    rows = [json.loads(l) for l in open(os.path.join(cands, "cands.jsonl"))]
    os.makedirs(picks, exist_ok=True)
    groups = {}
    for r in rows:
        if r["params"] is not None:
            groups.setdefault((r["style"], r["family"]), []).append(r)
    meta = []
    for (st, fam), rs in sorted(groups.items()):
        rs.sort(key=lambda r: r["score"])
        for i, r in enumerate(rs[:keep]):
            name = "%s-%s-%02d" % (st, fam, i + 1)
            shutil.copy(os.path.join(cands, r["name"] + ".wav"), os.path.join(picks, name + ".wav"))
            meta.append(dict(r, final=name))
    with open(os.path.join(picks, "picks.jsonl"), "w") as f:
        for m in meta:
            f.write(json.dumps(m) + "\n")
    print(len(meta), "picks")


def decode_mp3(p):
    out = subprocess.run(["ffmpeg", "-v", "error", "-i", p, "-f", "f32le", "-ac", "2", "-ar", "44100", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(out, np.float32).reshape(-1, 2)


def to_mp3(wav, mp3):
    import soundfile as sf
    x = sf.read(wav, dtype="float32", always_2d=True)[0]
    gain = 1.0
    for _ in range(6):
        tmp = mp3 + ".wav"
        sf.write(tmp, x * gain, acou.SR, subtype="FLOAT")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", tmp, "-c:a", "libmp3lame", "-b:a", "192k",
                        "-ar", "44100", mp3], check=True)
        os.remove(tmp)
        y = decode_mp3(mp3)
        pk = 20 * np.log10(max(np.abs(y).max(), 1e-9))
        if pk <= -1.0:
            return y, pk
        gain *= 10 ** ((-1.05 - pk) / 20)
    return y, pk


def librosa_check(y):
    import librosa
    m = y.mean(1)
    return {"centroid_hz": float(np.mean(librosa.feature.spectral_centroid(y=m, sr=acou.SR))),
            "rms_db": float(20 * np.log10(max(np.sqrt(np.mean(m ** 2)), 1e-9))),
            "dur_s": round(len(m) / acou.SR, 3)}


def mp3(dna_p, picks, outdir, loops_p, out_json):
    dna = acou.load_dna(dna_p)
    os.makedirs(outdir, exist_ok=True)
    meta = [json.loads(l) for l in open(os.path.join(picks, "picks.jsonl"))]
    if os.path.exists(loops_p):
        for l in open(loops_p):
            r = json.loads(l)
            meta.append(dict(r, final=r["name"]))
    items = []
    for r in meta:
        name, fam = r["final"], r["family"]
        wav = os.path.join(picks, name + ".wav")
        if not os.path.exists(wav):
            wav = os.path.join(os.path.dirname(loops_p), name + ".wav")
        y, pk = to_mp3(wav, os.path.join(outdir, name + ".mp3"))
        loop = fam == "boucle"
        z = acou.zscores(acou.measure(y, "drum_loop" if loop else "drum_oneshot", "drums" if loop else ""),
                         dna[FAM_DNA[fam]], acou.LOOP_MEASURES if loop else acou.MEASURES)
        worst = sorted(z.items(), key=lambda kv: -abs(kv[1]))[:3]
        items.append({
            "fichier": name + ".mp3", "style": r["style"], "famille": fam, "famille_adn": FAM_DNA[fam],
            "outil": "pycma (CMA-ES) -> pedalboard (mix 6 micros, filtres, EQ, compresseur, saturation"
                     + (", bitcrush" if r.get("params", {}).get("crush", 16) < 15.9 else "") + ")"
                     + (" ; séquence 4 mesures %d BPM" % r["bpm"] if loop else ""),
            "source": SOURCE if not loop else SOURCE + " (via les coups de ce style)",
            "prise_source": r.get("source_key"),
            "note_essentia": round(acou.score(z), 2),
            "ecarts_principaux": {k: round(v, 2) for k, v in worst},
            "note_clap": None,
            "librosa": librosa_check(y),
            "pic_dbfs": round(float(pk), 2),
            "url_asset": None,
        })
        print("%-36s %.2f" % (name, items[-1]["note_essentia"]), flush=True)
    json.dump({"zone": "batterie-acou", "note": "note_essentia = distance RMS à la médiane de la famille "
               "(écarts-types, 0 = parfait), mesurée sur le MP3 décodé ; CLAP non disponible.",
               "sons": items}, open(out_json, "w"), ensure_ascii=False, indent=1)


if __name__ == "__main__":
    a = sys.argv[1:]
    if a[0] == "pick":
        pick(a[1], a[2], a[3], int(a[4]) if len(a) > 4 else 4)
    else:
        mp3(*a[1:6])
