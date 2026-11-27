"""Batch 3: end_cut / end_fade_ms on one-shots. Expected: cut = the sound is stopped abruptly while audible."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *

def db(a): return 10 ** (a / 20.0)

def sounds(sr):
    return {
        "kick": kick(1.2, taua=0.12, sr=sr),                 # body until ~0.5 s
        "808_40Hz": b808(40.0, 2.0, 0.8, sr=sr),
        "lead_440": tone(440.0, 1.0, 0.4, sr=sr),
        "hat_noise": 0.5 * hp(noise(0.5, 1.0, 5, sr)) * np.exp(-T(0.5, sr) / 0.3),
        "snare": snare(0.6, sr=sr),
    }

def endings(x, sr, name):
    n = len(x)
    cutpos = {"kick": 0.15, "808_40Hz": 0.5, "lead_440": 0.5, "hat_noise": 0.2, "snare": 0.08}[name]
    c = x[:int(cutpos * sr)]
    out = []
    out.append(("hard cut (loud)", c, 1))
    for ms in (1, 2, 3, 5, 10, 30):
        out.append(("cut + %d ms fade" % ms, fade(c, ms, sr), 1 if ms <= 2 else (None if ms == 3 else 0)))
    out.append(("cut + 50 ms zeros", np.concatenate([c, np.zeros(int(0.05 * sr))]), 1))
    out.append(("cut + 50 ms dither -80 dB", np.concatenate([c, noise(0.05, db(-80), 7, sr)]), 1))
    out.append(("cut + 50 ms hiss -66 dB", np.concatenate([c, noise(0.05, db(-66), 7, sr)]), 1))
    out.append(("cut + 200 ms hiss -60 dB", np.concatenate([c, noise(0.2, db(-60), 7, sr)]), 1))
    out.append(("whole file at -30 dB, hard cut", c * db(-30), 1))
    out.append(("whole file at -40 dB, hard cut", c * db(-40), 1))
    out.append(("hard cut + clipped", np.clip(c * 3, -0.99, 0.99), 1))
    return out

chk = Check("b3_cut")
rows_out = []
i = 0
for sr in (48000, 44100, 96000):
    for name, x in sounds(sr).items():
        for label, y, want in endings(x, sr, name):
            if sr != 48000 and not label.startswith(("hard cut (loud)", "cut + 5 ms", "cut + 10 ms", "cut + 50 ms zeros")):
                continue
            i += 1
            rel = "Drums/Kicks/Kick 01.wav" if name in ("kick", "hat_noise", "snare") else "Synth/Lead C3.wav"
            rows, con, code = one("b3", i, rel, y, sr=sr, bits=24)
            tag = "%s sr=%d %s" % (name, sr, label)
            if not chk.run(tag, rows, code):
                continue
            r = rows[0]
            got = r.get("end_cut")
            if want is not None:
                chk.eq(tag + " end_cut", got, want)
            flag = "" if want is None or str(want) == got else "  <<< expected %s" % want
            print("%-9s sr=%-5d %-32s end_cut=%s fade=%s%s" % (name, sr, label, got, r.get("end_fade_ms"), flag))
chk.clean_repo()
chk.done()
