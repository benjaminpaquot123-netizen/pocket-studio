"""Style character: each style narrows the synth ranges (its "box") and gives
the tempo of its 4-bar loop. The optimiser then pulls every sound towards the
family DNA inside that box, so a trap kick stays a long boomy kick and a drum
& bass kick stays short and tight while both get closer to the kick median."""

OFF = {"bits": (14, 16)}  # no bit reduction
LOFI = {"bits": (7, 12)}

STYLES = {
    "trap": {"label": "Trap", "bpm": 140, "box": {
        "kick": dict(OFF, decay_s=(0.3, 0.9), f_end=(38, 52), len_s=(0.7, 1.2), drive=(0, 8)),
        "snare": dict(OFF, n_hp=(1200, 3000), noise_decay=(0.06, 0.2), verb=(0, 0.15)),
        "clap": dict(OFF, bursts=(3, 5), verb=(0.05, 0.25)),
        "hat_closed": dict(OFF, decay_s=(0.015, 0.04)),
        "hat_open": dict(OFF, decay_s=(0.12, 0.3)),
        "perc": dict(OFF, f=(700, 2200), noise=(0.0, 0.4)),
    }},
    "drill": {"label": "Drill", "bpm": 142, "box": {
        "kick": dict(OFF, decay_s=(0.25, 0.7), f_end=(40, 55), sweep_s=(0.02, 0.08), len_s=(0.6, 1.1)),
        "snare": dict(OFF, f1=(180, 260), noise_decay=(0.05, 0.16), n_hp=(800, 2500)),
        "clap": dict(OFF, bursts=(2, 4), tail_decay=(0.05, 0.15)),
        "hat_closed": dict(OFF, decay_s=(0.015, 0.035), base=(380, 560)),
        "hat_open": dict(OFF, decay_s=(0.12, 0.25)),
        "perc": dict(OFF, f=(500, 1800), index=(1, 6)),
    }},
    "boom-bap": {"label": "Hip-hop boom bap", "bpm": 90, "box": {
        "kick": dict(bits=(9, 16), decay_s=(0.12, 0.35), f_end=(45, 62), drive=(4, 14), lp=(3000, 8000), len_s=(0.35, 0.6)),
        "snare": dict(bits=(9, 16), tone=(0.3, 0.7), f1=(160, 220), noise_decay=(0.08, 0.25), n_lp=(5000, 9000)),
        "clap": dict(bits=(9, 16), lp=(5000, 9000), verb=(0, 0.2)),
        "hat_closed": dict(bits=(9, 16), lp=(9000, 14000), metal=(0.2, 0.6)),
        "hat_open": dict(bits=(9, 16), lp=(9000, 14000), decay_s=(0.15, 0.4)),
        "perc": dict(bits=(9, 16), f=(300, 1200), lp=(4000, 9000)),
    }},
    "house": {"label": "House", "bpm": 124, "box": {
        "kick": dict(OFF, decay_s=(0.18, 0.45), f_end=(45, 60), click=(0.1, 0.5), len_s=(0.35, 0.7)),
        "snare": dict(OFF, tone=(0.15, 0.5), verb=(0.05, 0.3)),
        "clap": dict(OFF, bursts=(3, 5), verb=(0.1, 0.4), tail_decay=(0.1, 0.35)),
        "hat_closed": dict(OFF, metal=(0.4, 1.0)),
        "hat_open": dict(OFF, decay_s=(0.15, 0.4), metal=(0.4, 1.0)),
        "perc": dict(OFF, f=(400, 1600), noise=(0.0, 0.3)),
    }},
    "techno": {"label": "Techno", "bpm": 130, "box": {
        "kick": dict(OFF, decay_s=(0.2, 0.6), drive=(6, 18), f_end=(42, 58), len_s=(0.4, 0.8)),
        "snare": dict(OFF, drive=(3, 15), noise_decay=(0.05, 0.25)),
        "clap": dict(OFF, drive=(2, 10), verb=(0.05, 0.4)),
        "hat_closed": dict(OFF, metal=(0.5, 1.0), drive=(0, 8)),
        "hat_open": dict(OFF, metal=(0.5, 1.0), decay_s=(0.15, 0.5)),
        "perc": dict(OFF, ratio=(1.2, 3.5), index=(1.5, 6), f=(250, 1500)),
    }},
    "drum-and-bass": {"label": "Drum & bass", "bpm": 174, "box": {
        "kick": dict(OFF, decay_s=(0.08, 0.25), f_end=(50, 72), click=(0.15, 0.6), len_s=(0.3, 0.5)),
        "snare": dict(OFF, f1=(200, 280), n_hp=(1000, 3000), noise_decay=(0.05, 0.2), bp_g=(2, 9)),
        "clap": dict(OFF, bursts=(2, 4), hp=(600, 1500)),
        "hat_closed": dict(OFF, decay_s=(0.015, 0.05)),
        "hat_open": dict(OFF, decay_s=(0.12, 0.35)),
        "perc": dict(OFF, f=(600, 2200)),
    }},
    "edm": {"label": "EDM / future", "bpm": 128, "box": {
        "kick": dict(OFF, click=(0.25, 0.6), drive=(4, 16), decay_s=(0.15, 0.4), len_s=(0.35, 0.7)),
        "snare": dict(OFF, verb=(0.1, 0.35), bp_g=(2, 9)),
        "clap": dict(OFF, bursts=(4, 5), verb=(0.15, 0.4)),
        "hat_closed": dict(OFF, lp=(15000, 19000)),
        "hat_open": dict(OFF, lp=(15000, 19000), decay_s=(0.15, 0.45)),
        "perc": dict(OFF, f=(500, 2000), noise=(0.1, 0.5)),
    }},
    "reggaeton-afro": {"label": "Reggaeton / afro", "bpm": 98, "box": {
        "kick": dict(OFF, decay_s=(0.15, 0.4), f_end=(45, 62), len_s=(0.35, 0.7)),
        "snare": dict(OFF, tone=(0.3, 0.7), noise_decay=(0.04, 0.15), f1=(200, 280)),
        "clap": dict(OFF, bursts=(2, 4), tail_decay=(0.05, 0.2)),
        "hat_closed": dict(OFF, metal=(0.2, 0.7)),
        "hat_open": dict(OFF, decay_s=(0.12, 0.3)),
        "perc": dict(OFF, f=(150, 700), index=(0, 2.5), noise=(0, 0.3), drop=(0.3, 1.5)),
    }},
    "lo-fi": {"label": "Lo-fi", "bpm": 82, "box": {
        "kick": dict(LOFI, lp=(2500, 6000), drive=(3, 12), decay_s=(0.12, 0.35), len_s=(0.35, 0.6)),
        "snare": dict(LOFI, n_lp=(5000, 8000), verb=(0.05, 0.3)),
        "clap": dict(LOFI, lp=(5000, 8000), verb=(0.05, 0.3)),
        "hat_closed": dict(LOFI, lp=(11000, 13000), metal=(0.2, 0.5)),
        "hat_open": dict(LOFI, lp=(11000, 13000), decay_s=(0.12, 0.35)),
        "perc": dict(LOFI, f=(200, 1000), lp=(3000, 7000)),
    }},
}

FAM_FR = {"kick": "kick", "snare": "snare", "clap": "clap", "hat_closed": "charleston-ferme",
          "hat_open": "charleston-ouvert", "perc": "percussion"}
FAM_LABEL = {"kick": "Kick", "snare": "Snare", "clap": "Clap", "hat_closed": "Charleston fermé",
             "hat_open": "Charleston ouvert", "perc": "Percussion électronique", "loop": "Boucle 4 mesures"}
