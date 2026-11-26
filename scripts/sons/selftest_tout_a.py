#!/usr/bin/env python3
"""Self-test of lot A of the second audit (docs/sons/AUDIT-TOUT-2.md):
names -> type, subtype and role, and the sound deciding when the name is silent.
Run: python3 -I scripts/sons/selftest_tout_a.py
"""
import os
import sys
import tempfile
import wave

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tout  # noqa: E402

FAILS = 0
COUNT = 0
SR = 48000


def check_eq(name, got, want):
    global FAILS, COUNT
    COUNT += 1
    if got != want:
        FAILS += 1
        print("FAIL %s: got %r, want %r" % (name, got, want))


def check_true(name, ok):
    check_eq(name, bool(ok), True)


def cl(rel, dur):
    return tout.classify(rel, dur)


def expect(rel, dur, **want):
    got = cl(rel, dur)
    for k, v in want.items():
        check_eq("%s [%s]" % (rel, k), got.get(k), v)


# ---------------------------------------------------------------- synthetic sounds

def _t(d):
    return np.arange(int(round(d * SR))) / SR


def _noise(d, seed):
    return np.random.default_rng(seed).uniform(-1, 1, int(round(d * SR)))


def _hp(x):
    y = np.diff(np.diff(x, prepend=0.0), prepend=0.0)
    return y / (np.max(np.abs(y)) + 1e-12)


def hat(d, tau, seed=3):
    return 0.6 * _hp(_noise(d, seed)) * np.exp(-_t(d) / tau)


def kick(d=0.4):
    t = _t(d)
    f = 50.0 + 110.0 * np.exp(-t / 0.03)
    return 0.9 * np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.10)


def snare(d=0.3, seed=2):
    t = _t(d)
    return 0.8 * (0.7 * _noise(d, seed) * np.exp(-t / 0.05) + 0.3 * np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.04))


def place(buf, x, at):
    i = int(round(at * SR))
    n = min(len(x), len(buf) - i)
    if n > 0:
        buf[i:i + n] += x[:n]


def drum_loop(bpm, bars=2):
    beat = 60.0 / bpm
    buf = np.zeros(int(round(bars * 4 * beat * SR)))
    for b in range(bars * 4):
        place(buf, kick(), b * beat)
        if b % 2 == 1:
            place(buf, snare(), b * beat)
        for h in (0.0, 0.5):
            place(buf, hat(0.05, 0.012, seed=b + 7), (b + h) * beat)
    return np.clip(buf * 0.8, -1, 1)


def chord_loop(bpm, bars=4):
    beat = 60.0 / bpm
    buf = np.zeros(int(round(bars * 4 * beat * SR)))
    chords = [(57, 60, 64), (53, 57, 60), (48, 52, 55), (55, 59, 62)]
    for b in range(bars):
        d = 4 * beat
        t = _t(d)
        seg = np.zeros(len(t))
        for m in chords[b % 4]:
            f = 440.0 * 2 ** ((m - 69) / 12.0)
            seg += sum(np.sin(2 * np.pi * f * k * t) / k for k in range(1, 5)) * np.exp(-t / 1.5)
        place(buf, 0.15 * seg, b * d)
    return np.clip(buf, -1, 1)


def write_wav(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2").tobytes()
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm)


def measured(tmp, rel, x):
    p = os.path.join(tmp, rel)
    write_wav(p, x)
    return tout._measure_file(p, rel, rel.split("/"), tout.Accuracy(), "t")


# ================================================================ 1 + 24: hat open / closed, perc height

for rel in ["Drums/Hats/Open/Hat 01.wav", "Drums/Open Hats/Hat 01.wav", "Drums/Hats/Open/01.wav",
            "Drums/Hats/Open/Open 01.wav", "Drums/Hats/Hat Op 01.wav", "Drums/Hats/Hat Semi Open 01.wav",
            "Drums/Hats/Hat Half-Open 01.wav", "Drums/Hats/Opened Hat 01.wav",
            "Drums/Hats/Charley Ouvert 01.wav", "Drums/Hats/Hat Open 01.wav", "Drums/Hats/OH 01.wav",
            "Drums/Hats/Hi-Hat Open 01.wav", "Drums/Hats/Hat - Open - 01.wav"]:
    expect(rel, 0.6, type="drum_oneshot", subtype="hat_open")
for rel in ["Drums/Hats/Closed/Hat 01.wav", "Drums/Closed Hats/Hat 01.wav", "Drums/Hats/Closed/01.wav",
            "Drums/Hats/Hat Cl 01.wav", "Drums/Hats/Hat Pedal 01.wav", "Drums/Hats/Hat Closed 01.wav",
            "Drums/Hats/CH 01.wav", "Drums/Hats/HiHat_Closed_01.wav"]:
    expect(rel, 0.08, type="drum_oneshot", subtype="hat_closed")
# the name wins over the folder
expect("Drums/Hats/Closed/Hat Open 01.wav", 0.6, subtype="hat_open")
expect("Drums/Hats/Hat 01.wav", 0.6, type="drum_oneshot", subtype="hat")
# words that are not open / closed
expect("Drums/Hats/Hat Clean 01.wav", 0.08, subtype="hat")
expect("Drums/Hats/Hat Opera 01.wav", 0.08, subtype="hat")
for rel, want in [("Drums/Perc/Mid/Perc 01.wav", "perc_mid"), ("Drums/Perc/High/Perc 01.wav", "perc_high"),
                  ("Drums/Perc/Low/Perc 01.wav", "perc_low"), ("Drums/Perc/Conga High 01.wav", "perc_high"),
                  ("Drums/Perc/Bongo Low 01.wav", "perc_low"), ("Drums/Perc/Perc Mid 01.wav", "perc_mid"),
                  ("Drums/Perc/Perc 01.wav", "perc")]:
    expect(rel, 0.3, type="drum_oneshot", subtype=want)
check_eq("drum_family folder open", tout.drum_family("Hat 01", ["Drums", "Hats", "Open"]), "hat_open")

with tempfile.TemporaryDirectory() as tmp:
    r = measured(tmp, "Drums/Hats/Hat 01.wav", hat(0.6, 0.15))
    check_eq("sound decides open hat", r.get("subtype"), "hat_open")
    r = measured(tmp, "Drums/Hats/Hat 02.wav", hat(0.08, 0.012))
    check_eq("sound decides closed hat", r.get("subtype"), "hat_closed")
    r = measured(tmp, "Drums/Hats/Hat Closed 03.wav", hat(0.6, 0.15))
    check_eq("the name wins over the measure", r.get("subtype"), "hat_closed")

# ================================================================ 2 + 3: melodic substrings, far loop folders

for rel, d in [("Drums/Kicks/Synth Kick 01.wav", 0.4), ("Drums/Kicks/Synth Hard/Kick 01.wav", 0.4),
               ("Drums/Snares/Synth Snare 01.wav", 0.3), ("Drums/Kicks/Bass Drum 01.wav", 0.4),
               ("Drums/Kicks/Kick Punchy Keyed.wav", 0.4), ("Drums/Kicks/Kick Bassy 01.wav", 0.4),
               ("Drums/Snares/Snare Padded 01.wav", 0.3), ("Drums/Claps/Clap Leader.wav", 0.3),
               ("Drums/Hats/Hat Plucky 01.wav", 0.08), ("Drums/Kicks/Kickdrum 01.wav", 0.4)]:
    expect(rel, d, type="drum_oneshot")
expect("Drums/Kicks/Bass Drum 01.wav", 0.4, subtype="kick", role="kick")
expect("Drums/Kicks/Kickdrum 01.wav", 0.4, subtype="kick", role="kick")
# a melodic name in a drum folder stays melodic
expect("Drums/Snares/Piano Stab 01.wav", 0.5, type="oneshot_melodic")
expect("Drums/Kicks/Synth/Lead C3.wav", 0.5, type="oneshot_melodic")
for rel, d in [("Loops & One Shots/Kicks/Kick 01.wav", 0.4), ("Trap Beats/One Shots/Snare 01.wav", 0.3),
               ("Drum Fills & Hits/Hits/Clap 01.wav", 0.3), ("Beat Makers Pack/Hats/Hat 01.wav", 0.08),
               ("Stems & Shots/Shots/Kick 01.wav", 0.4), ("Drums/Fills/Solo Fill 01.wav", 0.4)]:
    expect(rel, d, type="drum_oneshot")
expect("Drums/Fills/Fill 01 120.wav", 4.0, type="drum_loop")
expect("Drum Loops/128/Full 01.wav", 7.5, type="drum_loop")
expect("Kick Loops/Kick 01.wav", 3.84, type="drum_loop")
expect("Loops & One Shots/Loops/Drum Loop 01.wav", 4.0, type="drum_loop")

# ================================================================ 4 + 5: loops without 'loop', drum or melodic by words then sound

for rel, d, want in [("Tops/Top 128 01.wav", 7.5, "drum_loop"), ("Hi Hats/Hats 128 01.wav", 3.75, "drum_loop"),
                     ("Percussion/Perc 124 01.wav", 1.94, "drum_loop"), ("Breaks/Break 170 01.wav", 2.8, "drum_loop"),
                     ("Melodies/Melody 01 Cm.wav", 6.0, "melodic_loop"),
                     ("Bass/Bassline 01 124 Gm.wav", 3.87, "melodic_loop"),
                     ("Drums/Full Drums 01 120.wav", 8.0, "drum_loop")]:
    expect(rel, d, type=want)
expect("Drums/Kicks/Kick 150.wav", 0.4, type="drum_oneshot")
expect("Drums/Snares/Snare 128 01.wav", 0.3, type="drum_oneshot")
expect("808s/808 Long 140bpm C.wav", 3.0, type="drum_oneshot")
expect("808s/808 Long 140 C.wav", 3.0, type="drum_oneshot")
for rel in ["Loops/Organic Drum Loop 120.wav", "Loops/Sharp Hat Loop 140.wav", "Loops/Monkey Drum Loop 100.wav",
            "Loops/Padded Kick Loop 125.wav", "Loops/Drum & Bass Loop 174.wav", "Loops/Drum n Bass Loop 174.wav"]:
    expect(rel, 4.0, type="drum_loop")
for rel in ["Loops/Piano Loop 120 C.wav", "Loops/Bass Loop 140 F.wav", "Loops/Arp Loop 128 Am.wav",
            "Loops/Basslines Loop 124.wav"]:
    expect(rel, 4.0, type="melodic_loop")
for rel in ["Loops/What Loop 120 Am.wav", "Loops/Custom Loop 01 120 Am.wav", "Loops/Loop 01 120.wav",
            "Loops/Top Loop 01.wav"]:
    expect(rel, 4.0, subtype="by_sound")
expect("Drum Loops/Loop 01.wav", 4.0, type="drum_loop", subtype="-")
expect("Loops/Upbeat Piano Loop 120 C.wav", 4.0, role="keys", type="melodic_loop")
expect("Loops/Breakdown Synth Loop 128 Am.wav", 4.0, role="synth", type="melodic_loop")
expect("Loops/Superclap Loop 120.wav", 4.0, type="drum_loop", subtype="-")
expect("Loops/Breakbeat Loop 170.wav", 4.0, role="drums", type="drum_loop")

with tempfile.TemporaryDirectory() as tmp:
    for rel, x, want in [("Loops/Loop 01 120.wav", drum_loop(120, 2), "drum_loop"),
                         ("Hat Tricks Vol 1/Loops/Loop 01 120 bpm.wav", drum_loop(120, 2), "drum_loop"),
                         ("Loops/What Loop 120 Am.wav", chord_loop(120, 2), "melodic_loop"),
                         ("Loops/Custom Loop 01 120 Am.wav", chord_loop(120, 2), "melodic_loop"),
                         ("Loops/Top Loop 01.wav", chord_loop(120, 2), "melodic_loop"),
                         ("Tops/Top 128 01.wav", drum_loop(128, 4), "drum_loop")]:
        r = measured(tmp, rel, x)
        check_eq("sound decides %s" % rel, (r.get("type"), r.get("subtype")), (want, "by_sound"))
        check_true("measured as a loop %s" % rel, "bpm_measured" in r and not r.get("error"))
    r = measured(tmp, "Percussion/Perc 124 01.wav", drum_loop(124, 1))
    check_eq("Perc 124 01 is a drum loop", r.get("type"), "drum_loop")
    check_true("Perc 124 01 tempo heard", abs((r.get("bpm_measured") or 0) - 124) < 1)
expect("Drum Loops/Techno/Loop 01.wav", 4.0, type="drum_loop", subtype="-")
expect("Piano Loops/Deep/Loop 01.wav", 4.0, type="melodic_loop", subtype="-")

# ================================================================ 9: fx and vocal words do not steal instruments
for rel, d in [("Loops/Reversed Piano Loop 120 Am.wav", 8.0), ("Loops/Noisey Bass Loop 140 F.wav", 6.9),
               ("Loops/Texture Pad Loop 01.wav", 8.0), ("Bass/Bass Sweep C2.wav", 2.0),
               ("Synth/Atmosphere Pad C3.wav", 3.0), ("Bass/Reese Drop 01.wav", 2.0),
               ("Loops/Guitar Phrase 120 Am.wav", 8.0), ("Synth/Vox Lead C3.wav", 3.0),
               ("Synth/Voice Pad C3.wav", 3.0), ("Loops/Chanting Monks Loop 90.wav", 8.0)]:
    r = cl(rel, d)
    check_true("not fx / vocal: %s (%s)" % (rel, r["type"]), r["type"] not in ("fx", "vocal"))
expect("Bass/Reese Drop 01.wav", 2.0, role="bass")
expect("Synth/Vox Lead C3.wav", 3.0, role="lead")
expect("FX/Reverse Crash 01.wav", 2.0, type="fx")
expect("FX/Reversed Cymbal 01.wav", 2.0, type="fx")
expect("Drums/Perc/Vocal Perc 01.wav", 0.3, type="drum_oneshot")
# real fx and real voices stay
expect("FX/Risers/Riser 01.wav", 4.0, type="fx")
expect("FX/Piano Riser 01.wav", 4.0, type="fx")
expect("FX/White Noise Sweep 01.wav", 4.0, type="fx")
expect("Drums/Kicks/Kick Impact 01.wav", 0.5, type="drum_oneshot")
expect("Drums/Snares/Snare Noise 01.wav", 0.3, type="drum_oneshot")
expect("Vocals/Dry/Take 01.wav", 3.0, type="vocal")
expect("Vocals/Lead Vocal 01.wav", 3.0, type="vocal")
expect("Loops/Vocal Phrase 120 Am.wav", 8.0, type="vocal")
expect("Loops/Vox Loop 01 120.wav", 8.0, type="vocal")
expect("Vocals/Phrases/Phrase 01.wav", 2.0, type="vocal")
expect("Chants/Chant 01.wav", 2.0, type="vocal")
expect("Synth/Vocoder Chord Am.wav", 3.0, type="oneshot_melodic")


print("%d checks, %d failed" % (COUNT, FAILS))
sys.exit(1 if FAILS else 0)
