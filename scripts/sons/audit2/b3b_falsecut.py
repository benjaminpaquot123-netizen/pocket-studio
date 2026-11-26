"""Batch 3b: false cuts. Sounds that end naturally: is end_cut wrongly 1?"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *

def db(a): return 10 ** (a / 20.0)
i = 0
def case(label, rel, x, want, sr=SR, bits=24):
    global i
    i += 1
    rows, con, code = one("b3b", i, rel, x, sr=sr, bits=bits)
    r = rows[0]
    got = r.get("end_cut")
    flag = "" if want is None or str(want) == got else "  <<< expected %s" % want
    if flag:
        fault("%s: end_cut %s, expected %s" % (label, got, want))
    print("%-58s %-14s cut=%-2s fade=%-5s%s" % (label, r.get("type")[:14], got, r.get("end_fade_ms"), flag))

K = "Drums/Kicks/Kick 01.wav"; B = "Drums/808s/808 01 C.wav"; L = "Synth/Lead C3.wav"; H = "Drums/Hats/Hat Closed 01.wav"

# 1. natural exponential tails stopped (no fade) at a given level under full scale
for end in (-36, -40, -44, -46, -50, -55, -59, -61, -70):
    tau = 0.3
    d = -end / 8.686 * tau  # time for the envelope to reach `end` dB
    t = T(d)
    case("808 40 Hz, natural tail ends at %d dBFS (no fade)" % end, B, np.sin(2*np.pi*40*t)*np.exp(-t/tau)*0.999, 0 if end <= -50 else None)
for end in (-36, -44, -46, -50, -55, -59, -61):
    tau = 0.25
    d = -end / 8.686 * tau
    t = T(d)
    case("lead 440 Hz, natural tail ends at %d dBFS" % end, L, np.sin(2*np.pi*440*t)*np.exp(-t/tau)*0.999, 0 if end <= -50 else None)
for end in (-36, -44, -46, -50, -55, -61):
    tau = 0.05
    d = -end / 8.686 * tau
    case("hat noise, natural tail ends at %d dBFS" % end, H, hp(noise(d, 1.0, 9))/np.max(np.abs(hp(noise(d,1.0,9))))*np.exp(-T(d)/tau)*0.999, 0 if end <= -50 else None)

# 2. finished kick + constant hiss (vinyl / tape) until the end of the file
full = kick(0.8, taua=0.06)
for h in (-70, -60, -50, -46, -44, -40, -34):
    case("finished kick + hiss at %d dBFS to the end" % h, K, full + noise(0.8, db(h), 11), 0)
for h in (-60, -50, -40):
    case("finished kick + 50 Hz hum at %d dBFS to the end" % h, K, full + sine(50, 0.8, db(h)), 0)

# 3. DC offsets
case("finished kick + DC offset -50 dBFS", K, full + db(-50), 0)
case("finished kick + DC offset -30 dBFS", K, full + db(-30), 0)
case("finished kick then 300 ms of DC -60 dBFS only at the end", K, np.concatenate([full, np.full(14400, db(-60))]), 0)

# 4. 16-bit files with dither after the end of the sound
rng = np.random.default_rng(3)
def dith(n, lsb): return (rng.random(n) - rng.random(n)) * lsb / 32768.0
case("finished kick + 100 ms TPDF dither 1 LSB (16 bit)", K, np.concatenate([full, np.zeros(4800)]) + dith(len(full)+4800, 1), 0, bits=16)
c = kick(1.2, taua=0.12)[:7200]
case("CUT kick + 100 ms silence, TPDF dither 1 LSB (16 bit)", K, np.concatenate([c, np.zeros(4800)]) + dith(7200+4800, 1), 1, bits=16)
case("CUT kick + 100 ms silence, dither 2 LSB (16 bit)", K, np.concatenate([c, np.zeros(4800)]) + dith(7200+4800, 2), 1, bits=16)
case("CUT kick + 100 ms silence, dither 4 LSB (noise shaping)", K, np.concatenate([c, np.zeros(4800)]) + dith(7200+4800, 4), 1, bits=16)
case("CUT kick + 100 ms pure digital silence (16 bit)", K, np.concatenate([c, np.zeros(4800)]), 1, bits=16)
case("CUT kick + 2 s pure digital silence", K, np.concatenate([c, np.zeros(96000)]), 1, bits=16)

# 5. very short files
case("click 3 ms cut hard", H, hp(noise(0.003, 0.8, 4)), 1)
case("hat 8 ms cut hard (loud)", H, 0.6*hp(noise(0.008, 1.0, 4))/3, 1)
case("hat 12 ms cut hard (loud)", H, 0.6*hp(noise(0.012, 1.0, 4))/3, 1)

# 6. stereo traps
st = np.stack([full, np.zeros_like(full)], axis=1)
case("stereo: finished kick left, right silent", K, st, 0)
cc = kick(1.2, taua=0.12)[:7200]
st2 = np.stack([np.concatenate([cc, np.zeros(len(full)-7200)]), full*0.9], axis=1)
case("stereo: left cut early then silent, right finishes", K, st2, 0)
st3 = np.stack([fade(cc, 30), cc * 0.05], axis=1)
case("stereo: left faded, right quiet (-26 dB) but cut", K, st3, 1)

# 7. reversed sounds (end is loud by design)
rv = (0.5 * hp(noise(1.5, 1.0, 5)) / 3 * np.exp(-T(1.5) / 0.4))[::-1].copy()
case("reverse cymbal in Drums/Cymbals (ends loud by design)", "Drums/Cymbals/Reverse Cymbal 01.wav", rv, None)
case("reverse cymbal named 'Rev Crash 01'", "Drums/Cymbals/Rev Crash 01.wav", rv, None)

# 8. sustained organ note with 30 ms release, pad with slow fade, tremolo tail
org = tone(220, 1.0, 0.5)
case("organ note, 30 ms release", L, fade(org, 30), 0)
case("organ note, 8 ms release", L, fade(org, 8), 0)
case("organ note, 4 ms release", L, fade(org, 4), None)
case("organ note, exp fade 300 ms", L, org * np.exp(-np.maximum(0, T(1.0) - 0.7) / 0.03), 0)
t = T(2.0)
trem = np.sin(2*np.pi*220*t) * np.exp(-t/0.25) * (0.6 + 0.4*np.sin(2*np.pi*7*t))
case("tremolo lead with natural decay to -70", L, trem, 0)
# 9. cosine (S-curve) and exponential fades
case("lead cut + 20 ms cosine fade", L, org[:24000] * np.concatenate([np.ones(24000-960), 0.5*(1+np.cos(np.linspace(0, np.pi, 960)))]), 0)
case("lead cut + 20 ms exp fade (-60 dB)", L, org[:24000] * np.concatenate([np.ones(24000-960), 10**(-3*np.linspace(0,1,960))]), 0)
# 10. bass one-shot ending right at a zero crossing, full level
z = sine(100.0, 0.5, 0.8)
case("bass 100 Hz stopped on a zero crossing, full level", "Bass/Bass C2.wav", z, 1)
done()
