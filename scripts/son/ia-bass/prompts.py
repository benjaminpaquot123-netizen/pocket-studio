"""Families, prompts and measurement targets for the AI dubstep / drum-and-bass sound set.

Each family: tgt = (zone tsv, stock family, keys kind, preferences), kind = loop | oneshot,
bars = loop lengths in bars (loops) or seconds (one-shots), pitched = must carry a bass pitch.
Targets come from eq/son-fin-essentia-01 @ faa70cb3a (zone-b-basses.tsv, zone-c-synthes.tsv).
"""
BPM = {"dubstep": 140, "dnb": 174}
STYLE_WORDS = {"dubstep": ["dubstep", "140 bpm"], "dnb": ["drum and bass", "174 bpm"]}
STOCK_STYLE = {"dubstep": "dubstep", "dnb": "dnb"}
NOTES = ["F1", "E1", "G1", "D1", "A1", "F#1", "C2", "D#1"]

# preferences = which percentile to aim at instead of p50 (same idea as cible.py PREF)
PREF_REESE = {"width_db": "p90", "sub120": "p90", "low_beat": "p25"}  # what Benjamin kept
PREF_SUB = {"decay20_s": "p75", "lufs": "p75", "n_peaks": "p75"}

FAMILIES = {
    "wobble": dict(label="Wobbles", kind="loop", bars=[2, 4], gen_bars=4, n=40, keep=16, pitched=True,
                   tgt=("zone-b-basses", "phrase-de-basse", "loop", {}),
                   prompts=[
                       "slow wobble bass, {s}, half-time LFO, heavy and fat, {n}",
                       "slow dubstep wobble bass, quarter note LFO, deep and dirty, {n}",
                       "fast wobble bass, {s}, sixteenth note LFO, aggressive, {n}",
                       "fast wobble bassline, {s}, eighth note wub wub, distorted, {n}",
                       "triplet wobble bass, {s}, triplet LFO rhythm, gritty, {n}",
                       "wobble bass with triplet rhythm, {s}, rolling and fat, {n}",
                       "talking bass, {s}, vowel formant wobble, yapping, {n}",
                       "talking wobble bass, {s}, wah wah speaking bass, filthy, {n}",
                       "wobble bass with changing LFO speed, {s}, wub wub, {n}",
                       "classic wobble bass loop, {s}, low-pass filter LFO, saw bass, {n}",
                   ]),
    "growl": dict(label="Growls", kind="loop", bars=[1, 2], gen_bars=2, n=24, keep=12, pitched=True,
                  tgt=("zone-b-basses", "basse-synthe", "loop", {}),
                  prompts=[
                      "growl bass, {s}, FM growl, aggressive, {n}",
                      "dirty growl bass one note, {s}, monster growl, {n}",
                      "metallic growl bass, {s}, comb filter, heavy, {n}",
                      "deep growl bass sweep, {s}, distorted, {n}",
                      "neuro growl bass, {s}, resampled, gritty, {n}",
                      "angry growling bass, {s}, formant growl, {n}",
                  ]),
    "yoi": dict(label="Yoi / vowel bass", kind="loop", bars=[1, 2], gen_bars=2, n=24, keep=12, pitched=True,
                tgt=("zone-b-basses", "basse-synthe", "loop", {}),
                prompts=[
                    "yoi bass, {s}, vowel formant bass saying yoi, {n}",
                    "vowel bass, {s}, formant filter a-e-i-o-u, fat, {n}",
                    "yoy bass stab, {s}, talking vowel, distorted, {n}",
                    "vocal formant bass, {s}, oi oi vowel sweep, {n}",
                    "wah vowel bass, {s}, formant growl, heavy, {n}",
                    "yoi yoi bass riff, {s}, vowel bass, {n}",
                ]),
    "reese": dict(label="Reese / neuro reese", kind="oneshot", bars=[2.0, 3.0, 4.5], gen_s=5.0, n=24, keep=12,
                  pitched=True, tgt=("zone-b-basses", "basse-reese", "loop", PREF_REESE),
                  prompts=[
                      "reese bass, {s}, detuned saw bass, wide and dark, sustained note {n}",
                      "neuro reese bass, {s}, phasing detuned saws, distorted, {n}",
                      "dark reese bass, {s}, slow phasing, deep, long note {n}",
                      "neurofunk reese, {s}, gritty and moving, {n}",
                      "reese bass with chorus, {s}, wide stereo, sustained {n}",
                      "evil reese bass, {s}, rumbling detuned bass, {n}",
                  ]),
    "neuro": dict(label="Basses neuro / sales", kind="loop", bars=[1, 2], gen_bars=2, n=24, keep=12, pitched=True,
                  tgt=("zone-b-basses", "basse-synthe", "loop", {}),
                  prompts=[
                      "neuro bass, {s}, dirty distorted bass, {n}",
                      "filthy bass, {s}, heavy distortion, fat low end, {n}",
                      "distorted saw bass, {s}, overdriven, gritty, {n}",
                      "neurofunk bass shot, {s}, resampled, crunchy, {n}",
                      "bitcrushed bass, {s}, dirty, heavy, {n}",
                      "fat dirty bass, {s}, saturated square bass, {n}",
                  ]),
    "screech": dict(label="Screech / riddim", kind="loop", bars=[1, 2], gen_bars=2, n=24, keep=12, pitched=True,
                    tgt=("zone-b-basses", "phrase-de-basse", "loop", {}),
                    prompts=[
                        "screech bass, {s}, high pitched screaming bass, {n}",
                        "riddim bass, {s}, repetitive square bass, minimal, {n}",
                        "riddim dubstep bass loop, {s}, bouncy and dirty, {n}",
                        "screaming lead bass, {s}, distorted, metallic, {n}",
                        "tearout screech bass, {s}, aggressive, {n}",
                        "riddim wub bass, {s}, triplet bounce, {n}",
                    ]),
    "subdrop": dict(label="Sub drops", kind="oneshot", bars=[2.0, 3.0, 4.0], gen_s=4.0, n=16, keep=10, pitched=True,
                    tgt=("zone-b-basses", "sub-basse", "oneshot", PREF_SUB),
                    prompts=[
                        "sub drop, {s}, deep sine pitch falling down, long",
                        "808 sub drop, {s}, pitch dive, deep and clean",
                        "deep sub boom drop, {s}, falling low frequency",
                        "sub bass drop impact, {s}, descending sine",
                    ]),
    "impact": dict(label="Impacts", kind="oneshot", bars=[2.0, 3.0, 4.0], gen_s=4.0, n=16, keep=10, pitched=False,
                   tgt=("zone-c-synthes", "fx-effet", "oneshot", {}),
                   prompts=[
                       "impact hit, {s}, huge boom with reverb tail",
                       "cinematic bass impact, {s}, explosion, heavy",
                       "drop impact fx, {s}, deep boom and crash",
                       "distorted impact hit, {s}, heavy low end",
                   ]),
    "riser": dict(label="Risers / FX", kind="oneshot", bars=[3.0, 4.5, 6.0], gen_s=7.0, n=28, keep=14, pitched=False,
                  tgt=("zone-c-synthes", "fx-effet", "oneshot", {}),
                  prompts=[
                      "riser fx, {s}, white noise sweep up, building tension",
                      "pitch riser, {s}, screaming synth rising, intense",
                      "chaotic fx, {s}, glitchy sweeps all over the place",
                      "laser zap fx, {s}, wild pitch bends",
                      "downlifter sweep fx, {s}, falling noise",
                      "glitch stutter fx, {s}, random bleeps and zaps",
                      "alarm siren riser, {s}, build up",
                  ]),
    "stab": dict(label="Stabs gras", kind="oneshot", bars=[1.0, 1.5, 2.2], gen_s=2.5, n=24, keep=12, pitched=True,
                 tgt=("zone-b-basses", "basse-synthe", "oneshot", {}),
                 prompts=[
                     "fat bass stab, {s}, short distorted hit, {n}",
                     "heavy bass shot, {s}, punchy, dirty, {n}",
                     "dubstep bass hit, {s}, gritty one shot, {n}",
                     "big distorted stab, {s}, short, fat, {n}",
                     "neuro bass stab, {s}, sharp attack, {n}",
                     "chord stab bass, {s}, massive, {n}",
                 ]),
    "foley": dict(label="Foley bass", kind="loop", bars=[1, 2], gen_bars=2, n=16, keep=10, pitched=True,
                  tgt=("zone-b-basses", "basse-synthe", "loop", {}),
                  prompts=[
                      "foley bass, {s}, metallic clanks resampled into bass, {n}",
                      "organic bass, {s}, recorded objects mangled into a growl, {n}",
                      "junk metal bass, {s}, foley percussion bass hits, {n}",
                      "creature bass, {s}, foley sounds pitched down, gritty, {n}",
                  ]),
}


def prompt(fam, style, i):
    f = FAMILIES[fam]
    p = f["prompts"][i % len(f["prompts"])]
    s = ", ".join(STYLE_WORDS[style])
    return p.replace("{s}", s).replace("{n}", NOTES[(i // len(f["prompts"]) + i) % len(NOTES)])


def gen_seconds(fam, style):
    f = FAMILIES[fam]
    if f["kind"] == "loop":
        return min(11.5, f["gen_bars"] * 4 * 60.0 / BPM[style])
    return f["gen_s"]
