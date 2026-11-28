"""Kick character per page style (box over kick2.KICK2) and the DNA style
used as the target when per-style statistics exist."""
OFF = {"bits": (13, 16)}
K = {
    "trap": dict(OFF, decay_s=(0.15, 0.4), f_end=(40, 52), len_s=(0.4, 0.8), sat=(1.5, 4), punch=(0.1, 0.4), t_slow=(0.04, 0.12)),
    "drill": dict(OFF, decay_s=(0.15, 0.35), f_end=(42, 55), t_slow=(0.06, 0.15), len_s=(0.4, 0.7), sat=(1.5, 4)),
    "boom-bap": dict(bits=(9, 13), sat=(3, 8), lp=(3000, 8000), decay_s=(0.1, 0.3), punch=(0.2, 0.6), len_s=(0.3, 0.55)),
    "house": dict(OFF, decay_s=(0.15, 0.35), f_end=(48, 60), click=(0.1, 0.35), punch=(0.2, 0.5), len_s=(0.35, 0.6)),
    "techno": dict(bits=(10, 16), sat=(2, 8), decay_s=(0.08, 0.3), comp_db=(-24, -12), len_s=(0.3, 0.6)),
    "drum-and-bass": dict(OFF, decay_s=(0.06, 0.16), f_end=(40, 55), f_start=(300, 600), click=(0.1, 0.35), tick=(0.0, 0.25), lp=(3000, 9000), len_s=(0.25, 0.45)),
    "edm": dict(OFF, f_start=(250, 500), punch=(0.3, 0.6), sat=(2.5, 6), click=(0.1, 0.3), decay_s=(0.15, 0.35), len_s=(0.35, 0.6)),
    "reggaeton-afro": dict(OFF, decay_s=(0.12, 0.3), f_end=(50, 65), punch=(0.2, 0.5), len_s=(0.3, 0.5), sat=(1.5, 3)),
    "lo-fi": dict(bits=(8, 13), lp=(2500, 7000), sat=(1.5, 6), decay_s=(0.05, 0.16), hold_s=(0.0, 0.03), len_s=(0.25, 0.45)),
}
# page style -> DNA style label (per-style stats, when they exist)
DNA_STYLE = {"trap": "multi", "drill": "multi", "boom-bap": "lofi", "house": "house", "techno": "techno",
             "drum-and-bass": "dnb", "edm": "rave", "reggaeton-afro": "afro-house", "lo-fi": "lofi"}
