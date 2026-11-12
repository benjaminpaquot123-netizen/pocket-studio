#!/usr/bin/env python3
"""Synthesizes the app's own party sounds (air horns, sirens, lasers, risers, drops, whistle).

Everything here is computed from scratch (oscillators, noise, filters, echo): no recording, no
sample of anyone else, so the sounds are ours (see THIRD_PARTY.md). The Kenney voices (CC0) are
copied from the published pack « Bruitages : corps, voix et pas » with --voices.

Usage: synth_party.py <output dir> [--voices <dir with the unzipped bruitages_corps pack>] [--wav]
Writes <id>.ogg (Vorbis, mono, 48 kHz) for each sound of project-model's PartySounds.
"""
import subprocess
import sys
from pathlib import Path

import numpy as np

RATE = 48000
rng = np.random.default_rng(303)


def t_of(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def phase(freq):
    """Running phase (radians) of a frequency curve, one value per sample."""
    return 2 * np.pi * np.cumsum(freq) / RATE


def saw(ph):
    return 2 * ((ph / (2 * np.pi)) % 1.0) - 1


def square(ph, width=0.5):
    return np.where((ph / (2 * np.pi)) % 1.0 < width, 1.0, -1.0)


def onepole_lp(x, cutoff):
    """One-pole low-pass; cutoff may be a curve."""
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = np.exp(-2 * np.pi * cutoff / RATE)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a[i]) * x[i] + a[i] * acc
        y[i] = acc
    return y


def svf_bp(x, freq, q=4.0):
    """State-variable band-pass (Chamberlin), freq may be a curve."""
    freq = np.broadcast_to(np.asarray(freq, dtype=float), x.shape)
    low = band = 0.0
    y = np.empty_like(x)
    damp = 1.0 / q
    for i in range(len(x)):
        f = 2 * np.sin(np.pi * min(freq[i], RATE / 6) / RATE)
        low += f * band
        high = x[i] - low - damp * band
        band += f * high
        y[i] = band
    return y


def env_adsr(n, a, d, s, r, hold=None):
    """Attack, decay, sustain level, release, in seconds; the note holds until n - release."""
    e = np.zeros(n)
    A, D, R = int(a * RATE), int(d * RATE), int(r * RATE)
    H = n - R if hold is None else int(hold * RATE)
    i = 0
    for k in range(min(A, n)):
        e[k] = k / max(A, 1)
    i = A
    for k in range(i, min(i + D, H)):
        e[k] = 1 - (1 - s) * (k - i) / max(D, 1)
    for k in range(min(i + D, H), H):
        e[k] = s
    last = e[H - 1] if H > 0 else s
    for k in range(H, n):
        e[k] = last * (1 - (k - H) / max(R, 1))
    return np.clip(e, 0, 1)


def echo(x, seconds, feedback, mix, tail=1.0):
    d = int(seconds * RATE)
    y = np.concatenate([x, np.zeros(int(tail * RATE))])
    out = y.copy()
    for k in range(d, len(out)):
        out[k] += feedback * out[k - d]
    return y + mix * (out - y)


def soft(x, drive=1.5):
    return np.tanh(drive * x) / np.tanh(drive)


def finish(x, peak_db=-1.0, fade=0.01):
    x = x - np.mean(x)
    f = int(fade * RATE)
    x[-f:] *= np.linspace(1, 0, f)
    x[: int(0.001 * RATE)] *= np.linspace(0, 1, int(0.001 * RATE))
    peak = np.max(np.abs(x)) or 1.0
    return x / peak * 10 ** (peak_db / 20)


# --- Air horns -----------------------------------------------------------------------------------

def horn_blast(seconds, base=466.0, bend=0.06, voices=(1.0, 1.007, 0.994, 1.189, 1.195)):
    """One blast of an air horn: a stack of detuned saws (a minor third on top), a pitch that
    settles from above, a nasal band, a bit of crunch."""
    t = t_of(seconds)
    settle = 1 + bend * np.exp(-t / 0.05)
    wobble = 1 + 0.004 * np.sin(2 * np.pi * 6.0 * t)
    x = sum(saw(phase(base * v * settle * wobble) + rng.uniform(0, 6.28)) for v in voices) / len(voices)
    x = 0.6 * svf_bp(x, 1400, q=1.2) + 0.5 * onepole_lp(x, 3500)
    x = soft(2.2 * x)
    return x * env_adsr(len(t), 0.008, 0.05, 0.85, 0.06)


def rest(seconds):
    return np.zeros(int(seconds * RATE))


def air_horn_dj():
    # « Baaa, ba-ba-baaa »: the DJ's call.
    x = np.concatenate([horn_blast(0.55), rest(0.07), horn_blast(0.13), rest(0.05), horn_blast(0.13), rest(0.05), horn_blast(0.8)])
    return finish(echo(x, 0.18, 0.25, 0.35, tail=0.6))


def air_horn_long():
    return finish(echo(horn_blast(1.4, base=440.0), 0.21, 0.2, 0.3, tail=0.5))


def air_horn_dancehall():
    # Three fast high blasts climbing, then a long one: the sound-system's.
    x = np.concatenate([
        horn_blast(0.12, base=523.0), rest(0.04), horn_blast(0.12, base=523.0), rest(0.04),
        horn_blast(0.12, base=587.0), rest(0.04), horn_blast(0.9, base=622.0, bend=0.1),
    ])
    return finish(echo(x, 0.15, 0.3, 0.35, tail=0.7))


# --- Sirens --------------------------------------------------------------------------------------

def siren_dub():
    # The dub siren: a square whose pitch is swept by a fast saw LFO, the LFO rate rising, echoes.
    t = t_of(2.4)
    rate = 3.0 + 4.0 * t / t[-1]
    lfo = (np.cumsum(rate) / RATE) % 1.0
    f = 380 + 520 * lfo + 180 * t / t[-1]
    x = 0.6 * square(phase(f), 0.5) + 0.4 * np.sin(phase(f))
    x = onepole_lp(x, 2600) * env_adsr(len(t), 0.02, 0.1, 1.0, 0.15)
    return finish(echo(soft(x, 1.3), 0.3, 0.45, 0.45, tail=1.4))


def siren_wail():
    # The long rise and fall of an alarm, twice.
    t = t_of(2.6)
    f = 650 + 650 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 1.3))
    x = np.sin(phase(f)) + 0.25 * np.sin(2 * phase(f)) + 0.1 * square(phase(f))
    return finish(soft(x * env_adsr(len(t), 0.05, 0.1, 1.0, 0.2), 1.2))


def siren_yelp():
    # Fast « yelp » of a police car.
    t = t_of(1.8)
    f = 800 + 600 * np.abs(2 * ((t * 7.5) % 1.0) - 1)
    x = np.sin(phase(f)) + 0.3 * square(phase(f), 0.3)
    return finish(onepole_lp(x, 4000) * env_adsr(len(t), 0.01, 0.1, 1.0, 0.1))


# --- Lasers and zaps -----------------------------------------------------------------------------

def pew(seconds=0.28, top=3200.0, bottom=180.0):
    t = t_of(seconds)
    f = bottom + (top - bottom) * np.exp(-t / (seconds / 4.5))
    mod = np.sin(phase(f * 1.5)) * 1.5
    x = np.sin(phase(f) + mod) + 0.3 * square(phase(f), 0.25)
    return x * env_adsr(len(t), 0.002, seconds * 0.6, 0.3, seconds * 0.3)


def laser():
    return finish(echo(pew(), 0.12, 0.35, 0.4, tail=0.5))


def laser_triple():
    x = np.concatenate([pew(0.18, 3800, 300), rest(0.06), pew(0.18, 3400, 260), rest(0.06), pew(0.3, 4200, 150)])
    return finish(echo(x, 0.11, 0.3, 0.3, tail=0.4))


def zap():
    # An electric zap: a fast buzzing drop, noisy.
    t = t_of(0.45)
    f = 90 + 1800 * np.exp(-t / 0.06)
    buzz = square(phase(f), 0.2) * (0.6 + 0.4 * square(phase(np.full_like(t, 37.0))))
    noise = rng.standard_normal(len(t)) * np.exp(-t / 0.04)
    x = 0.8 * buzz + 0.5 * svf_bp(noise, 3000, 2)
    return finish(soft(x * env_adsr(len(t), 0.001, 0.2, 0.4, 0.2), 2.0))


# --- Risers, drops, impact -----------------------------------------------------------------------

def riser(seconds=4.0):
    # White noise through a band rising four octaves, a saw climbing an octave and a half, louder and louder.
    t = t_of(seconds)
    u = t / t[-1]
    noise = rng.standard_normal(len(t))
    band = svf_bp(noise, 300 * 2 ** (4.5 * u), q=3.0)
    f = 110 * 2 ** (2.5 * u ** 1.4)
    tone = sum(saw(phase(f * d)) for d in (1.0, 1.01, 0.99)) / 3
    tone = onepole_lp(tone, 400 + 6000 * u)
    x = (0.9 * band + 0.45 * tone) * (0.08 + 0.92 * u ** 2)
    # Faster and faster pulses at the end (the « snare roll » feel).
    pulse = 0.65 + 0.35 * np.sign(np.sin(2 * np.pi * np.cumsum(2 + 14 * u ** 2) / RATE))
    return finish(x * np.where(u > 0.5, pulse, 1.0), fade=0.004)


def riser_short():
    return riser(2.0)


def drop():
    # The downlifter: a falling tone and noise, then the sub boom.
    t = t_of(1.6)
    u = t / t[-1]
    f = 40 + 600 * np.exp(-t / 0.35)
    tone = np.sin(phase(f)) + 0.3 * saw(phase(f))
    noise = svf_bp(rng.standard_normal(len(t)), 5000 * np.exp(-t / 0.4) + 200, 2.0) * np.exp(-t / 0.5)
    x = 0.9 * tone * env_adsr(len(t), 0.005, 0.3, 0.8, 0.6) + 0.5 * noise
    return finish(soft(x, 1.6))


def impact():
    # A big hit: a sub kick (pitch falling), a noise crash, a long dark tail.
    t = t_of(2.5)
    f = 38 + 160 * np.exp(-t / 0.05)
    boom = np.sin(phase(f)) * np.exp(-t / 0.7)
    crash = svf_bp(rng.standard_normal(len(t)), 2500, 0.8) * np.exp(-t / 0.25)
    tail = onepole_lp(rng.standard_normal(len(t)), 600) * np.exp(-t / 0.9) * 0.6
    return finish(soft(1.1 * boom + 0.45 * crash + tail, 1.8))


def whistle():
    # The party whistle: a bright tone trilled fast by the pea inside, three blows.
    def blow(seconds):
        t = t_of(seconds)
        trill = 1 + 0.035 * np.sin(2 * np.pi * 28 * t)
        f = 2900 * trill
        x = np.sin(phase(f)) + 0.15 * rng.standard_normal(len(t))
        return onepole_lp(x, 6000) * env_adsr(len(t), 0.01, 0.02, 0.9, 0.04)
    x = np.concatenate([blow(0.18), rest(0.08), blow(0.18), rest(0.08), blow(0.6)])
    return finish(x)


SYNTHS = {
    "air_horn": air_horn_dj,
    "air_horn_long": air_horn_long,
    "air_horn_dancehall": air_horn_dancehall,
    "siren_dub": siren_dub,
    "siren_wail": siren_wail,
    "siren_yelp": siren_yelp,
    "laser": laser,
    "laser_triple": laser_triple,
    "zap": zap,
    "riser": riser,
    "riser_short": riser_short,
    "drop": drop,
    "impact": impact,
    "whistle": whistle,
}

# Kenney Voiceover Pack (CC0) as published in « Bruitages : corps, voix et pas » (words heard).
VOICES = {
    "voice_go_go_go": "vocal_annonce_52.flac",  # « Go go go go! »
    "voice_go": "vocal_annonce_40.flac",  # « Go! »
    "voice_ready": "vocal_annonce_47.flac",  # « Ready »
    "voice_level_up": "vocal_annonce_43.flac",  # « Level up! »
    "voice_power_up": "vocal_annonce_16.flac",  # « Power up! »
    "voice_fire": "vocal_annonce_51.flac",  # « Fire in the hole! »
    "voice_you_win": "vocal_annonce_60.flac",  # « You win! »
    "voice_hurry_up": "vocal_annonce_41.flac",  # « Hurry up! »
}


def write_ogg(x, path, wav=False):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2").tobytes()
    fmt = ["-c:a", "pcm_s16le"] if wav else ["-c:a", "libvorbis", "-q:a", "4"]
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-", *fmt, str(path)],
                   input=pcm, check=True)


def main(argv):
    out = Path(argv[1])
    out.mkdir(parents=True, exist_ok=True)
    wav = "--wav" in argv
    ext = ".wav" if wav else ".ogg"
    for name, make in SYNTHS.items():
        write_ogg(make(), out / f"{name}{ext}", wav)
        print("synth", name)
    if "--voices" in argv:
        src = Path(argv[argv.index("--voices") + 1])
        for name, file in VOICES.items():
            raw = subprocess.run(["ffmpeg", "-v", "error", "-i", str(src / file), "-ac", "1", "-ar", str(RATE), "-f", "f32le", "-"],
                                 capture_output=True, check=True).stdout
            x = np.frombuffer(raw, dtype=np.float32).astype(float)
            # Starts on the word (no silence before it), peak at -1 dBFS like the synthesized ones.
            start = int(np.argmax(np.abs(x) > 0.01 * np.max(np.abs(x))))
            write_ogg(finish(x[max(0, start - 48):].copy()), out / f"{name}{ext}", wav)
            print("voice", name)


if __name__ == "__main__":
    main(sys.argv)
