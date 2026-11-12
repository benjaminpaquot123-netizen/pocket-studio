#!/usr/bin/env python3
"""Synthesizes the drum packs of the modern-sounds trial (docs/sons/PLAN-SONS-MODERNES.md § 6).

usage: python3 -I trial_drums.py OUT_DIR [--report]

Writes three packs, OUT_DIR/essai_jump_up, essai_tech_house and essai_techno: 16-bit 48 kHz WAV
files (kicks and snares mono, everything else stereo) named <kind>_<nn>.wav, a pack.json in the
app's PackManifest format (the best sound of each kind first: the assistant takes the first) and a
LICENSE.txt. Every sound is computed here from oscillators, noise and filters with fixed seeds: no
recording and no sample of anyone else, so the sounds are ours, and two runs write the same bytes.

The techniques are today's rather than a drum machine's. Trial n° 2 (Benjamin on n° 1: « tu
rajoutes juste des cymbales on dirait »): the difference goes where it is heard, the kick and the
snare or clap, and the metal steps back.
- kicks in four layers: a tuned sub (a sine falling from 170-250 Hz onto its fundamental, reached
  exactly: the tech house and techno kicks sit on the key), a short punch on a harmonic between 90
  and 200 Hz, the body's harmonics between 180 Hz and 1.5 kHz (tanh and tanh² at 4x the rate, so
  nothing folds back: what a phone's speaker plays, the ear rebuilding the fundamental from them)
  and a click of its own in 1.2-4.5 kHz over 2-6 ms;
- snares and claps in layers: a tonal body at 180-250 Hz, a noise band at 1-3 kHz (the wires, the
  hands), a little top, saturated, then a plate (t60 0.3-0.5 s) whose left and right tails are
  partly independent (claps: wide, yet summing to mono cleanly) and a soft clipper (denser at the
  same peak): each stays within 40 dB of its peak for 0.3-0.55 s;
- hats and rides from six detuned square oscillators and their ring products plus noise, each
  side mixing the shared bank with its own slightly detuned one, a shelf taking 3 dB off above
  10 kHz;
- percussion by modal synthesis (wood, rim, membrane), grain shakers, a two-operator FM clank.
Peaks by kind (PEAKS): kicks, snares and claps at -1 dBFS, percussion at -4, hats, open hats and
rides at -8 (the metal discreet). No DC, a short fade at the end.

--report prints, and writes to OUT_DIR/mesures.json, each sound's duration, peak, RMS, crest
factor, energy above 10 kHz, spectral centroid, side against centre, DC, its energy in the four
bands (sub 20-60 Hz, low 60-250 Hz, mid 250 Hz-4 kHz, high 4-20 kHz), in 250 Hz-1.5 kHz (a kick's
harmonics), 1.2-4.5 kHz (its click) and 250 Hz-2 kHz (a snare's or clap's body), each against
20 Hz-20 kHz, the energy above 10 kHz against 4-10 kHz, the tail (the last sample within 40 dB of
the peak), and each kick's fundamental measured over 60-200 ms (tech house in G: 49.0 Hz, techno in
F: 43.65 Hz). Checked by test_trial_drums.py.
"""
import json
import sys
import wave
import zlib
from pathlib import Path

import numpy as np
from scipy import signal

RATE = 48000
OS = 4  # oversampling of the saturation stages and of the square banks
# Peak of each kind (dBFS): the metal far under the kick and the snare (trial n° 2).
PEAKS = {'kick': -1.0, 'snare': -1.0, 'clap': -1.0, 'perc': -4.0, 'hat': -8.0, 'open_hat': -8.0, 'ride': -8.0}
LICENSE = ('Sons originaux de Pocket Studio, fabriqués par synthèse maison (scripts/packs/trial_drums.py) : '
           'aucun enregistrement ni échantillon tiers.\n')

# Notes the tuned sounds sit on (Hz).
F1, G1 = 43.654, 48.999
F3, F4, F5, C6 = 174.614, 349.228, 698.456, 1046.502
D4, D5, G5, D6 = 293.665, 587.330, 783.991, 1174.659


# --- Building blocks -----------------------------------------------------------------------------

def rng_for(name):
    """A generator seeded by the sound's own name: adding a sound never changes the others."""
    return np.random.default_rng(zlib.crc32(name.encode()))


def t_of(seconds):
    return np.arange(int(round(seconds * RATE))) / RATE


def filt(x, btype, freq, order=2):
    """Causal Butterworth filter along time; freq is a cutoff or a (low, high) band."""
    return signal.sosfilt(signal.butter(order, freq, btype=btype, fs=RATE, output='sos'), x, axis=0)


def peak_eq(x, freq, gain_db, q=0.8):
    """RBJ peaking equalizer."""
    a_ = 10 ** (gain_db / 40)
    w = 2 * np.pi * freq / RATE
    cw, alpha = np.cos(w), np.sin(w) / (2 * q)
    b = np.array([1 + alpha * a_, -2 * cw, 1 - alpha * a_])
    a = np.array([1 + alpha / a_, -2 * cw, 1 - alpha / a_])
    return signal.lfilter(b / a[0], a / a[0], x, axis=0)


def high_shelf(x, freq, gain_db):
    """RBJ high shelf (slope 1), half its gain at `freq`: at 8 kHz and -3 dB, -2.3 dB at 10 kHz and
    -2.7 dB at 12 kHz."""
    a_ = 10 ** (gain_db / 40)
    w = 2 * np.pi * freq / RATE
    cw, alpha = np.cos(w), np.sin(w) / np.sqrt(2)
    r = 2 * np.sqrt(a_) * alpha
    b = np.array([a_ * ((a_ + 1) + (a_ - 1) * cw + r), -2 * a_ * ((a_ - 1) + (a_ + 1) * cw),
                  a_ * ((a_ + 1) + (a_ - 1) * cw - r)])
    a = np.array([(a_ + 1) - (a_ - 1) * cw + r, 2 * ((a_ - 1) - (a_ + 1) * cw), (a_ + 1) - (a_ - 1) * cw - r])
    return signal.lfilter(b / a[0], a / a[0], x, axis=0)


def tame_top(x):
    """The metal's top: 3 dB off above 10 kHz (trial n° 2: less fizz, the « cymbals » step back)."""
    return high_shelf(x, 8000.0, -3.0)


def norm(x):
    return x / (np.max(np.abs(x)) or 1.0)


def saturate(x, drive, bias=0.0):
    """Soft clip (tanh) computed at 4x the rate, so its harmonics do not fold back under Nyquist.
    x peaks near 1. `bias` shifts the curve a little for even harmonics (warmth); the curve still
    passes through 0, so silence stays silent."""
    up = signal.resample_poly(x, OS, 1, axis=0)
    y = (np.tanh(drive * up + bias) - np.tanh(bias)) / np.tanh(drive)
    return signal.resample_poly(y, 1, OS, axis=0)


def harmonics(x, drive, even, band=(180.0, 1500.0)):
    """A low sound's harmonics, as a phone's speaker plays them (« virtual bass », the ear rebuilding
    the fundamental from them): x through tanh (odd harmonics) plus `even` of tanh² (even ones) at 4x
    the rate, so nothing folds back; only the band kept (4th-order Butterworth each side). x peaks
    near 1: the harmonics follow its level, strong while it is loud, thinning as it fades."""
    up = drive * signal.resample_poly(x, OS, 1, axis=0)
    y = signal.resample_poly(np.tanh(up) + even * np.tanh(up) ** 2, 1, OS, axis=0)
    return filt(y, 'bandpass', band, 4)


def exp_env(t, tau, start=0.0, attack=0.0002):
    """Exponential decay from `start` (silent before), after a short linear attack."""
    d = t - start
    e = np.exp(-np.maximum(d, 0.0) / tau) * (d >= 0)
    return e * np.clip(d / attack, 0.0, 1.0) if attack > 0 else e


def hold_decay(t, hold, length, curve):
    """Full level for `hold` seconds, then (1 - u)^curve down to exactly zero at `length`."""
    u = np.clip((t - hold) / (length - hold), 0.0, 1.0)
    return (1.0 - u) ** curve


def osc(t, freq, phase=0.0):
    """Sine following a frequency curve (or a constant)."""
    f = np.broadcast_to(np.asarray(freq, dtype=float), t.shape)
    return np.sin(2 * np.pi * np.cumsum(f) / RATE + phase)


def resonate(x, freqs, taus, amps):
    """Modal synthesis: two-pole resonators, each mode ringing at its frequency for its time."""
    y = np.zeros_like(x)
    for f, tau, a in zip(freqs, taus, amps):
        r, w = np.exp(-1.0 / (tau * RATE)), 2 * np.pi * f / RATE
        y += a * signal.lfilter([np.sin(w)], [1.0, -2 * r * np.cos(w), r * r], x)
    return y


def place(x, pan):
    """Mono to stereo at `pan` (-1 left, +1 right), constant power."""
    a = (pan + 1) * np.pi / 4
    return np.stack([x * np.cos(a), x * np.sin(a)], axis=1) * np.sqrt(2)


def room(x, rng, t60, mix, predelay=0.006, damp=6000.0, channels=2, spread=0.6):
    """A short room: x convolved with exponentially decaying noise whose highs die first. Each
    output channel's tail mixes a shared noise with its own (`spread` of it its own): wide, and no
    comb filter when the sides are summed (no delayed copies)."""
    n = int(t60 * RATE)
    tt = np.arange(n) / RATE
    shared = rng.standard_normal(n)
    wet = []
    for c in range(channels):
        noise = np.sqrt(1 - spread) * shared + np.sqrt(spread) * rng.standard_normal(n)
        ir = (filt(noise, 'lowpass', damp, 1) * 10 ** (-3 * tt / t60)
              + 0.5 * filt(noise, 'highpass', damp, 1) * 10 ** (-3 * tt / (0.4 * t60)))
        ir *= np.clip(tt / 0.004, 0.0, 1.0)  # the diffuse tail builds up over a few ms
        ir = np.concatenate([np.zeros(int(predelay * RATE)), ir / np.sqrt(np.sum(ir ** 2))])
        wet.append(signal.fftconvolve(x[:, min(c, x.shape[1] - 1)], ir)[: len(x)])
    return x + mix * np.stack(wet, axis=1)


def finish(x, fade=0.006):
    """Columns (samples, channels), no DC, a 0.2 ms fade in, a raised-cosine fade out, a peak of 1
    (build() sets each kind's peak, PEAKS)."""
    x = np.array(x, dtype=float)
    if x.ndim == 1:
        x = x[:, None]
    x = norm(x)
    x -= x.mean(axis=0)
    k = int(0.0002 * RATE)
    x[:k] *= np.linspace(0.0, 1.0, k, endpoint=False)[:, None]
    f = int(fade * RATE)
    x[-f:] *= (0.5 + 0.5 * np.cos(np.linspace(0.0, np.pi, f)))[:, None]
    # The fades moved the mean a little: take it out along a slow bump, ends kept at zero.
    bump = np.hanning(len(x))[:, None]
    x -= x.sum(axis=0) / bump.sum() * bump
    return x / np.max(np.abs(x))


# --- Kicks ---------------------------------------------------------------------------------------

def kick(name, f0, top, glide, length, hold, curve, drive, bias=0.1, h=3, punch=0.5, punch_ms=22.0, harm=1.5,
         hdrive=10.0, even=0.4, click=1.5, click_ms=5.0, knock=1.0, knock_hz=2200.0, clip=2.5, lp=7000.0, grit=0.0):
    """Layered kick, made to be heard on a phone's speaker as well as on a big one:
    - sub: a sine falling from `top` onto f0, reached exactly at `glide` seconds and held (tuned);
    - punch: a sine on the h-th harmonic of f0 (90-200 Hz, so in tune too), dropping onto it from
      half an octave above in a few ms, gone in about 3 x punch_ms (30-80 ms);
    - glue: sub and punch through a soft tanh at 4x (`drive`, `bias` for a little warmth);
    - harmonics: `harm` of sub and punch through tanh and tanh² (`hdrive`, `even`), kept at
      180 Hz-1.5 kHz (harmonics());
    - click: a layer of its own: noise and an impulse band-passed to 1.2-4.5 kHz and a knock, a
      sine diving onto knock_hz, held for 40 % of click_ms then gone at click_ms (2-6 ms).
    The body (all but the click) is kept hot by a gentler envelope before the saturation than after
    it; `grit` boosts the low mids before the saturation (dirtier). Click and body then go through a
    soft clipper (`clip`, tanh at 4x): the click's spike no longer sets the peak alone, so the body
    keeps its weight at the same peak. The sub-sonic rumble and the DC go last."""
    rng = rng_for(name)
    t = t_of(length)
    sweep = np.clip(1.0 - t / glide, 0.0, 1.0) ** 3
    pre = hold_decay(t, hold, length, 0.5 * curve)
    pun = osc(t, h * f0 * (1 + 0.41 * np.exp(-t / 0.004))) * exp_env(t, punch_ms / 1000, attack=0.0005)
    low = osc(t, f0 + (top - f0) * sweep) * pre + punch * pun
    if grit:
        low = peak_eq(low, 400.0, grit, 0.7)
    low = norm(low)
    body = filt(saturate(low, drive, bias) + harm * harmonics(low, hdrive, even), 'lowpass', lp, 2) * pre
    click_env = hold_decay(t, 0.4 * click_ms / 1000, click_ms / 1000, 2.0) * np.clip(t / 0.0001, 0.0, 1.0)
    burst = rng.standard_normal(len(t)) * click_env
    burst[0] += 4.0
    knk = osc(t, knock_hz * (1 + np.exp(-t / 0.0015))) * click_env
    tick = norm(filt(norm(filt(burst, 'bandpass', (1200, 4500), 2)) + knock * knk, 'lowpass', 6000.0, 2))
    return finish(filt(saturate(norm(body + click * tick), clip), 'highpass', 0.45 * f0, 2))


# --- Snares and claps ----------------------------------------------------------------------------

def snare(name, f, length, body=0.9, body_tau=0.08, noise=1.3, noise_tau=0.05, clap=0.6, top=0.3, crack=0.5,
          drive=2.2, plate=0.5, t60=0.4, damp=4500.0, clip=3.0, hp=100.0):
    """Layered snare: a tuned shell (three membrane modes, pitch dropping 30 % onto f, 180-250 Hz,
    the first ringing body_tau), the wires' noise in 1-3 kHz (a fast and a slower decay), a
    three-burst clap in the same band, `top` of the wires' brighter 3-9 kHz rattle, a stick crack;
    saturated at 4x for weight (`drive`), then a plate: a darkened tail of t60 s, `plate` of it
    (mono, like the file); last a soft clipper on the whole (`clip`, tanh at 4x: denser at the same
    peak)."""
    rng = rng_for(name)
    t = t_of(length)
    fall = 1 + 0.3 * np.exp(-t / 0.008)
    shell = (osc(t, f * fall) * exp_env(t, body_tau, attack=0.0005)
             + 0.7 * osc(t, 1.59 * f * fall, 1.0) * exp_env(t, 0.6 * body_tau, attack=0.0005)
             + 0.45 * osc(t, 2.14 * f * fall, 2.0) * exp_env(t, 0.4 * body_tau, attack=0.0005))
    wire_env = 0.6 * exp_env(t, noise_tau, attack=0.0005) + 0.4 * exp_env(t, 2.5 * noise_tau, attack=0.0005)
    wire = filt(rng.standard_normal(len(t)), 'bandpass', (1000, 3000), 2) * wire_env
    hand = filt(rng.standard_normal(len(t)), 'bandpass', (1000, 2600), 2)
    hand_env = sum(exp_env(t, 0.004, s, attack=0.0004) for s in (0.0, 0.006)) + exp_env(t, 0.035, 0.012, attack=0.0004)
    rattle = filt(rng.standard_normal(len(t)), 'bandpass', (3000, 9000), 2) * exp_env(t, 0.6 * noise_tau, attack=0.0005)
    tip = filt(rng.standard_normal(len(t)), 'bandpass', (2000, 6000), 2) * exp_env(t, 0.0015, attack=0.0001)
    x = (body * norm(shell) + noise * norm(wire) + clap * norm(hand) * hand_env + top * norm(rattle)
         + crack * norm(tip))
    x = room(saturate(norm(x), drive)[:, None], rng, t60, plate, damp=damp, channels=1)
    return finish(filt(saturate(norm(x), clip), 'highpass', hp, 2), fade=0.25 * length)


def clap(name, length, offsets=(0.0, 0.010, 0.020, 0.030), band=(1000, 2500), burst=0.005, tail=0.08, high=0.2,
         body=0.35, body_hz=210.0, width=0.35, drive=3.0, plate=0.6, t60=0.4, damp=5000.0, predelay=0.008, clip=3.0,
         lp=9000.0, hp=120.0):
    """Hand clap in layers: 3-4 noise bursts 8-15 ms apart in the hands' band (1-3 kHz), each its own
    hand (its own noise, the band shifted a little), the last one ringing for `tail`; `high` of a
    brighter layer that dies sooner; the palms' body, two modes from body_hz (180-250 Hz) rung by
    every burst, in the centre; a soft saturation (`drive`); a plate tail of t60 s, `plate` of it;
    then a soft clipper on the whole (`clip`, tanh at 4x: denser at the same peak) and a low-pass
    at `lp` (the clipper's fizz). Each side mixes shared noise with its own (`width`), and the
    plate's tails are partly their own: a real stereo clap that sums to mono without comb
    filtering."""
    rng = rng_for(name)
    t = t_of(length)
    n = len(t)
    x = np.zeros((n, 2))
    hits = np.zeros(n)
    for k, start in enumerate(offsets):
        last = k == len(offsets) - 1
        env = exp_env(t, tail if last else burst, start, attack=0.0005)
        env_top = exp_env(t, 0.6 * tail if last else burst, start, attack=0.0003)
        noise = (1 - width) * rng.standard_normal(n)[:, None] + width * rng.standard_normal((n, 2))
        shift = rng.uniform(0.9, 1.1)
        x += ((0.8 + 0.2 * last) * filt(noise, 'bandpass', (band[0] * shift, band[1] * shift), 2) * env[:, None]
              + high * filt(noise, 'bandpass', (3500, 9000), 2) * env_top[:, None])
        hits += exp_env(t, burst, start, attack=0.0005)
    thump = resonate(rng.standard_normal(n) * hits, [body_hz, 1.48 * body_hz], [0.045, 0.025], [1.0, 0.5])
    x = room(saturate(norm(norm(x) + body * norm(thump)[:, None]), drive), rng, t60, plate, predelay, damp)
    return finish(filt(filt(saturate(norm(x), clip), 'highpass', hp, 2), 'lowpass', lp, 2), fade=0.25 * length)


# --- Hats and rides ------------------------------------------------------------------------------

SIX = np.array([205.3, 304.4, 369.6, 522.7, 540.0, 800.0])  # the classic six-square cymbal ratios


def metal(n, scale, rng, detune, ring):
    """Six detuned squares at 4x the rate, mixed with their pairwise ring products (denser and more
    inharmonic), decimated (little aliasing), scaled to unit RMS."""
    tt = np.arange(n * OS) / (RATE * OS)
    freqs = SIX * scale * (1 + detune * rng.uniform(-1, 1, 6))
    sq = np.where((freqs[:, None] * tt[None, :] + rng.uniform(0, 1, 6)[:, None]) % 1.0 < 0.5, 1.0, -1.0)
    x = (1 - ring) * sq.mean(axis=0) + ring * (sq[0] * sq[1] + sq[2] * sq[3] + sq[4] * sq[5]) / 3
    x = signal.resample_poly(x, 1, OS)
    return x / np.std(x)


def cymbal(name, length, tau, scale=1.6, hp=5200.0, ring=0.5, noise=0.5, width=0.3, detune=0.01, attack=0.0003,
           snap=0.0, grit=0.0):
    """Closed or open hat: the six-square bank plus white noise, enveloped (a sharp attack, an
    exponential decay `tau`, `snap` of it a fast first drop, closed by a fade over its last third),
    high-passed steeply (lower than in trial n° 1: more of the 5-10 kHz « tss », less fizz), the
    fizz above 14 kHz tamed (a phone does not play it), 3 dB off above 10 kHz (tame_top: the
    « cymbals » of trial n° 1 step back). `grit` drives it into a tanh first (dirtier). Each side mixes the shared bank and noise with
    its own (`width`), detuned a little more: a slight width that sums to mono."""
    rng = rng_for(name)
    t = t_of(length)
    n = len(t)
    shared = metal(n, scale, rng, detune, ring) + noise * rng.standard_normal(n)
    sides = [metal(n, scale, rng, 3 * detune, ring) + noise * rng.standard_normal(n) for _ in range(2)]
    x = np.stack([(1 - width) * shared + width * s for s in sides], axis=1)
    if grit:
        x = np.tanh(grit * x / np.std(x))
    env = (1 - snap) * exp_env(t, tau, attack=attack) + snap * exp_env(t, 0.01, attack=attack)
    x = filt(filt(x * env[:, None], 'highpass', hp, 4), 'lowpass', 14000.0, 2)
    return finish(tame_top(x), fade=0.35 * length)


BELL = np.array([1.0, 1.183, 1.506, 1.741, 2.065, 2.412, 2.873, 3.367])  # inharmonic ride partials


def ride(name, length, ping=2900.0, bell=0.35, ping_tau=0.35, wash_tau=0.28, scale=2.4, width=0.4, wash_hp=3500.0,
         damp=0.0):
    """Ride: a stick tick, a bell of eight inharmonic partials from `ping` up, each doubled a hair
    higher (they beat: the shimmer; the double is tuned a little differently on each side), a long
    wash of two square banks and noise above wash_hp; high-passed, the fizz above 16 kHz tamed, 3 dB
    off above 10 kHz (tame_top), a long fade. `damp` darkens it (a low-pass, in kHz)."""
    rng = rng_for(name)
    t = t_of(length)
    n = len(t)
    taus = ping_tau * np.linspace(1.0, 0.35, len(BELL))
    amps = 1.0 / (1.0 + 0.35 * np.arange(len(BELL)))
    phases = rng.uniform(0, 2 * np.pi, (2, len(BELL)))
    beat = rng.uniform(0.001, 0.003, len(BELL))
    pings = [a * np.sin(2 * np.pi * ping * r * t + p) * exp_env(t, tau, attack=0.0005)
             for r, a, p, tau in zip(BELL, amps, phases[0], taus)]
    shared = metal(n, scale, rng, 0.01, 0.6) + 0.6 * metal(n, 1.37 * scale, rng, 0.01, 0.8) + 0.5 * rng.standard_normal(n)
    chans = []
    for _ in range(2):
        tune = 1 + beat + rng.uniform(-0.0005, 0.0005, len(BELL))
        ping_x = sum(pings) + 0.6 * sum(a * np.sin(2 * np.pi * ping * r * k * t + p) * exp_env(t, tau, attack=0.0005)
                                        for r, k, a, p, tau in zip(BELL, tune, amps, phases[1], taus))
        own = metal(n, scale, rng, 0.03, 0.6) + 0.5 * rng.standard_normal(n)
        wash = filt(((1 - width) * shared + width * own) * exp_env(t, wash_tau, attack=0.001), 'highpass', wash_hp, 4)
        chans.append(bell * 2.5 * ping_x / np.sum(amps) + wash)
    x = np.stack(chans, axis=1)
    tick = filt(rng.standard_normal(n), 'bandpass', (3000, 9000), 2) * exp_env(t, 0.002, attack=0.0001)
    x = filt(filt(x + 1.5 * tick[:, None], 'highpass', 500.0, 2), 'lowpass', damp * 1000 if damp else 16000.0, 2)
    return finish(tame_top(x), fade=0.3 * length)


# --- Percussion ----------------------------------------------------------------------------------

def strike(rng, t, soft_ms, lp):
    """A stick or hand: an impulse and a short burst of noise, low-passed (softer when lower)."""
    exc = rng.standard_normal(len(t)) * exp_env(t, soft_ms / 1000, attack=0.0001)
    exc[0] += 3.0
    return filt(exc, 'lowpass', lp, 2)


def wood(name, f, length, pan=0.15, hardness=0.5, room_mix=0.15, t60=0.25):
    """Wooden « toc », tuned: a free bar's first three modes (1 : 2.76 : 5.40) struck by a short
    soft click, set a little off centre in a small room."""
    rng = rng_for(name)
    t = t_of(length)
    exc = strike(rng, t, 0.4, 2500 + 9000 * hardness)
    ring = 0.045 * np.sqrt(G5 / f)  # a smaller block rings shorter
    x = resonate(exc, [f, 2.76 * f, 5.40 * f], [ring, ring / 3, ring / 7], [1.0, 0.45, 0.2])
    x = norm(x) + 0.25 * hardness * norm(filt(exc, 'highpass', 3000, 2))
    return finish(room(place(norm(x), pan), rng, t60, room_mix, damp=7000), fade=0.35 * length)


def rim(name, f, length, pan=-0.1, crack=0.6, room_mix=0.18, t60=0.3):
    """Rim shot click, tuned: a sharp stick crack, a short wooden body at f, a metallic ring above."""
    rng = rng_for(name)
    t = t_of(length)
    exc = strike(rng, t, 0.3, 12000)
    body = resonate(exc, [f, 1.47 * f, 2.41 * f, 3.73 * f], [0.025, 0.012, 0.03, 0.008], [1.0, 0.4, 0.35, 0.2])
    tip = filt(rng.standard_normal(len(t)), 'bandpass', (2000, 8000), 2) * exp_env(t, 0.0012, attack=0.0001)
    x = saturate(norm(norm(body) + crack * norm(tip)), 1.5)
    return finish(room(place(x, pan), rng, t60, room_mix, damp=8000), fade=0.35 * length)


def conga(name, f, length, pan=0.25, slap=0.4, room_mix=0.12, t60=0.3):
    """Conga or bongo, tuned: a membrane (fundamental falling 10 % onto f, two higher modes) and a
    slap of band-passed noise."""
    rng = rng_for(name)
    t = t_of(length)
    fall = 1 + 0.1 * np.exp(-t / 0.015)
    skin = (osc(t, f * fall) * exp_env(t, 0.09, attack=0.0005)
            + 0.3 * osc(t, 1.59 * f * fall, 1.0) * exp_env(t, 0.04, attack=0.0005)
            + 0.15 * osc(t, 2.14 * f * fall, 2.0) * exp_env(t, 0.025, attack=0.0005))
    hit = filt(rng.standard_normal(len(t)), 'bandpass', (1000, 4500), 2) * exp_env(t, 0.004, attack=0.0002)
    x = saturate(norm(norm(skin) + slap * norm(hit)), 1.3)
    return finish(room(place(x, pan), rng, t60, room_mix), fade=0.35 * length)


def shaker(name, length, density=3000.0, band=(5500, 12000), width=0.7, rise=0.012, tau=0.035, room_mix=0.1):
    """Shaker: beads as random clicks, denser at the peak of the shake, band-passed high, over a
    little hiss, the loudest beads rounded off by a soft saturation; each side mostly its own
    beads: wide."""
    rng = rng_for(name)
    t = t_of(length)
    n = len(t)
    shape = np.where(t < rise, (t / rise) ** 2, np.exp(-(t - rise) / tau))
    p = np.clip(density * shape / RATE, 0.0, 1.0)

    def beads():
        hits = (rng.random(n) < p) * rng.uniform(0.3, 1.0, n) * rng.choice([-1.0, 1.0], n)
        return filt(hits, 'bandpass', band, 2) + 0.08 * filt(rng.standard_normal(n), 'bandpass', band, 2) * shape

    shared = beads()
    x = np.stack([(1 - width) * shared + width * beads() for _ in range(2)], axis=1) * np.sqrt(shape)[:, None]
    x = saturate(norm(x), 2.5)
    return finish(room(x, rng, 0.2, room_mix, damp=9000), fade=0.3 * length)


def blip(name, top, f, length, pan=-0.2, room_mix=0.15):
    """Short laser tick (drum and bass's little fx percussion): a sine diving from `top` onto f in a
    few ms, over a noise click."""
    rng = rng_for(name)
    t = t_of(length)
    tone = osc(t, f + (top - f) * np.exp(-t / 0.004)) * exp_env(t, 0.018, attack=0.0003)
    tip = filt(rng.standard_normal(len(t)), 'bandpass', (4000, 10000), 2) * exp_env(t, 0.001, attack=0.0001)
    x = saturate(norm(norm(tone) + 0.4 * norm(tip)), 1.8)
    return finish(room(place(x, pan), rng, 0.25, room_mix, damp=8000), fade=0.35 * length)


def tom(name, f, length, pan=0.2, drive=2.0, room_mix=0.15):
    """Short synth tom, tuned: a sine falling an octave onto f, saturated, over a noise attack."""
    rng = rng_for(name)
    t = t_of(length)
    tone = osc(t, f * (1 + np.exp(-t / 0.012))) * hold_decay(t, 0.01, length, 2.5)
    hit = filt(rng.standard_normal(len(t)), 'bandpass', (1500, 6000), 2) * exp_env(t, 0.003, attack=0.0001)
    x = filt(saturate(norm(norm(tone) + 0.3 * norm(hit)), drive), 'highpass', 0.5 * f, 2)
    return finish(room(place(x, pan), rng, 0.35, room_mix), fade=0.35 * length)


def clank(name, f, length, pan=-0.25, index=3.0, room_mix=0.2):
    """Metallic hit, tuned: two-operator FM at an inharmonic ratio (1 : 1.41) whose brightness
    falls fast; the right side a hair higher than the left (a little width, no hollow in mono
    before the sound has died)."""
    rng = rng_for(name)
    t = t_of(length)
    chans = []
    for k in (1.0, 1.0015):
        mod = index * np.sin(2 * np.pi * 1.41 * f * k * t) * exp_env(t, 0.02, attack=0.0002)
        chans.append(np.sin(2 * np.pi * f * k * t + mod) * exp_env(t, 0.06, attack=0.0003))
    x = np.stack(chans, axis=1) * np.array([np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)]) * np.sqrt(2)
    tip = filt(rng.standard_normal(len(t)), 'bandpass', (3000, 9000), 2) * exp_env(t, 0.001, attack=0.0001)
    x = x + 0.3 * tip[:, None]
    return finish(room(filt(x, 'highpass', 300.0, 2), rng, 0.4, room_mix), fade=0.35 * length)


# --- The packs -----------------------------------------------------------------------------------
# Per pack: (kind, the word that sets the variants apart, maker). The first of each kind is the
# one the assistant plays.

PACKS = [
    {'id': 'essai_jump_up', 'name': 'Essai jump up', 'family': 'Bass music', 'bpm': 174.0, 'sounds': [
        ('kick', 'percutant', lambda n: kick(n, 57.0, 250, 0.035, 0.25, 0.015, 2.2, 2.8, 0.12, h=2, punch=0.6,
                                             punch_ms=18.0, knock_hz=2400.0)),
        ('kick', 'rond', lambda n: kick(n, 55.0, 220, 0.045, 0.28, 0.03, 1.8, 2.0, h=2, punch_ms=25.0, harm=1.3,
                                        click=1.3, click_ms=6.0, knock_hz=1600.0, clip=2.0)),
        ('kick', 'sec', lambda n: kick(n, 60.0, 250, 0.03, 0.21, 0.01, 2.8, 3.2, h=2, punch=0.6, punch_ms=12.0,
                                       click=2.6, click_ms=5.5, knock_hz=3000.0)),
        ('kick', 'distordu', lambda n: kick(n, 58.0, 240, 0.04, 0.26, 0.02, 2.0, 4.5, 0.2, h=2, harm=1.6, hdrive=12.0,
                                            even=0.6, click=1.9, click_ms=6.0, grit=4.0)),
        ('snare', 'énorme', lambda n: snare(n, 200.0, 0.45, clap=0.8, t60=0.42)),
        ('snare', 'claquante', lambda n: snare(n, 215.0, 0.38, body=0.75, body_tau=0.06, noise=1.4, noise_tau=0.04,
                                               clap=0.9, crack=0.8, drive=2.6, plate=0.4, t60=0.32)),
        ('snare', 'grasse', lambda n: snare(n, 185.0, 0.45, body=0.75, body_tau=0.09, noise=1.2, noise_tau=0.065,
                                            clap=0.5, drive=3.2, plate=0.45)),
        ('snare', 'brillante', lambda n: snare(n, 230.0, 0.5, clap=0.8, top=0.5, plate=0.55, t60=0.45, damp=6000.0)),
        ('clap', 'clac', lambda n: clap(n, 0.4, (0.0, 0.009, 0.019), band=(1100, 2600), tail=0.09, high=0.3,
                                        body_hz=230.0, plate=0.5, t60=0.35, clip=4.0)),
        ('clap', 'large', lambda n: clap(n, 0.55, (0.0, 0.010, 0.021, 0.033), tail=0.12, body=0.25, body_hz=220.0,
                                         drive=4.0, plate=0.7, t60=0.45, clip=4.0)),
        ('clap', 'sec', lambda n: clap(n, 0.35, (0.0, 0.008, 0.016), band=(1200, 3000), tail=0.045, high=0.3,
                                       body_hz=240.0, plate=0.4, t60=0.3)),
        ('clap', 'épais', lambda n: clap(n, 0.45, (0.0, 0.012, 0.024, 0.038), band=(900, 2000), tail=0.12, body=0.4,
                                         body_hz=190.0, drive=4.0, clip=4.5)),
        ('hat', 'net', lambda n: cymbal(n, 0.055, 0.012, scale=1.7, hp=5600.0)),
        ('hat', 'tic', lambda n: cymbal(n, 0.04, 0.007, scale=2.0, hp=6000.0)),
        ('hat', 'rond', lambda n: cymbal(n, 0.08, 0.02, scale=1.4, hp=4800.0)),
        ('hat', 'granuleux', lambda n: cymbal(n, 0.07, 0.016, ring=0.7, noise=1.0, width=0.4)),
        ('perc', 'rim', lambda n: rim(n, F5, 0.14)),
        ('perc', 'shaker', lambda n: shaker(n, 0.14)),
        ('perc', 'bongo', lambda n: conga(n, F4, 0.25, pan=0.3, slap=0.5)),
        ('perc', 'laser', lambda n: blip(n, 3500.0, F5, 0.1)),
    ]},
    {'id': 'essai_tech_house', 'name': 'Essai tech house', 'family': 'House', 'bpm': 126.0, 'sounds': [
        ('kick', 'claquant', lambda n: kick(n, G1, 210, 0.045, 0.34, 0.025, 2.4, 2.0, punch=0.6, punch_ms=20.0,
                                            knock_hz=2600.0)),
        ('kick', 'court', lambda n: kick(n, G1, 220, 0.04, 0.30, 0.015, 2.8, 2.4, punch=0.6, punch_ms=15.0, click=1.9,
                                         knock_hz=3000.0)),
        ('kick', 'rond', lambda n: kick(n, G1, 180, 0.05, 0.38, 0.04, 2.0, 1.6, punch=0.45, punch_ms=26.0, harm=1.3,
                                        click=1.3, click_ms=6.0, knock_hz=1600.0, clip=2.0)),
        ('kick', 'gras', lambda n: kick(n, G1, 200, 0.045, 0.36, 0.03, 2.2, 3.2, 0.18, harm=1.6, hdrive=12.0, even=0.6,
                                        grit=3.0)),
        ('clap', 'claquant', lambda n: clap(n, 0.4, (0.0, 0.010, 0.020, 0.030), tail=0.06, high=0.3, body_hz=220.0,
                                            plate=0.5, t60=0.35)),
        ('clap', 'classique', lambda n: clap(n, 0.45, (0.0, 0.011, 0.022, 0.032), band=(950, 2300), tail=0.1,
                                             body_hz=205.0, clip=4.0)),
        ('clap', 'large', lambda n: clap(n, 0.55, (0.0, 0.012, 0.025, 0.037), width=0.4, body_hz=200.0, t60=0.45)),
        ('clap', 'claquement de doigts', lambda n: clap(n, 0.35, (0.0, 0.008, 0.016), band=(1300, 3000), tail=0.035,
                                                        high=0.3, body=0.25, body_hz=250.0, t60=0.35, hp=180.0)),
        ('hat', 'net', lambda n: cymbal(n, 0.05, 0.011, scale=1.8, hp=5600.0)),
        ('hat', 'doux', lambda n: cymbal(n, 0.075, 0.018, scale=1.4, hp=4800.0)),
        ('hat', 'tic', lambda n: cymbal(n, 0.04, 0.007, scale=2.0, hp=6000.0)),
        ('hat', 'shuffle', lambda n: cymbal(n, 0.065, 0.015, ring=0.6, noise=1.0, width=0.4)),
        ('open_hat', 'court', lambda n: cymbal(n, 0.24, 0.06, scale=1.6, snap=0.3, width=0.4)),
        ('open_hat', 'moyen', lambda n: cymbal(n, 0.32, 0.09, scale=1.6, snap=0.25, width=0.4)),
        ('open_hat', 'long', lambda n: cymbal(n, 0.4, 0.12, scale=1.5, hp=4800.0, snap=0.2, width=0.45)),
        ('perc', 'toc', lambda n: wood(n, G5, 0.2)),
        ('perc', 'shaker', lambda n: shaker(n, 0.15)),
        ('perc', 'conga', lambda n: conga(n, D4, 0.3)),
        ('perc', 'rim', lambda n: rim(n, D5, 0.13, pan=-0.2)),
        ('perc', 'toc aigu', lambda n: wood(n, D6, 0.15, pan=-0.25, hardness=0.7)),
        ('ride', 'scintillante', lambda n: ride(n, 1.2)),
        ('ride', 'cloche', lambda n: ride(n, 1.0, ping=2400.0, bell=0.8, ping_tau=0.4, wash_tau=0.24)),
        ('ride', 'douce', lambda n: ride(n, 1.4, ping=3200.0, bell=0.25, wash_tau=0.38, damp=12.0)),
    ]},
    {'id': 'essai_techno', 'name': 'Essai techno', 'family': 'Techno', 'bpm': 132.0, 'sounds': [
        ('kick', 'profond', lambda n: kick(n, F1, 190, 0.055, 0.56, 0.07, 3.0, 3.0, 0.12, punch_ms=25.0, click=1.6,
                                           click_ms=6.0, knock_hz=1800.0, lp=6000.0)),
        ('kick', 'saturé', lambda n: kick(n, F1, 220, 0.045, 0.52, 0.06, 2.8, 5.0, 0.2, harm=1.6, hdrive=12.0,
                                          even=0.6, click=1.6, click_ms=6.0, knock_hz=2000.0, lp=6000.0, grit=4.0)),
        ('kick', 'long', lambda n: kick(n, F1, 170, 0.06, 0.6, 0.09, 2.6, 1.8, punch=0.45, punch_ms=26.0, click=1.6,
                                        click_ms=6.0, knock_hz=1500.0, lp=6000.0)),
        ('kick', 'dur', lambda n: kick(n, F1, 240, 0.04, 0.46, 0.04, 3.2, 3.8, 0.15, punch=0.6, punch_ms=18.0,
                                       knock_hz=2600.0)),
        ('clap', 'sombre', lambda n: clap(n, 0.45, (0.0, 0.010, 0.021, 0.031), band=(850, 2100), tail=0.11, high=0.15,
                                          body_hz=190.0, t60=0.42, damp=4000.0, clip=4.0)),
        ('clap', 'industriel', lambda n: clap(n, 0.42, (0.0, 0.010, 0.020), band=(1000, 2600), tail=0.12, body=0.3,
                                              body_hz=215.0, drive=5.0, plate=0.5, t60=0.38, clip=5.0)),
        ('clap', 'large', lambda n: clap(n, 0.55, (0.0, 0.011, 0.023, 0.034), band=(800, 2000), tail=0.1, high=0.15,
                                         body_hz=195.0, width=0.4, t60=0.45, damp=4500.0, clip=3.5)),
        ('clap', 'hall', lambda n: clap(n, 0.6, (0.0, 0.012, 0.024, 0.036), tail=0.1, body_hz=205.0, plate=0.8,
                                        t60=0.5, damp=4500.0, clip=3.5)),
        ('hat', 'métallique', lambda n: cymbal(n, 0.06, 0.013, scale=1.5, ring=0.75)),
        ('hat', 'tic', lambda n: cymbal(n, 0.04, 0.007, scale=2.0, hp=6000.0)),
        ('hat', 'sale', lambda n: cymbal(n, 0.07, 0.015, ring=0.7, grit=2.5)),
        ('hat', 'doux', lambda n: cymbal(n, 0.085, 0.02, scale=1.4, hp=4800.0)),
        ('open_hat', 'moderne', lambda n: cymbal(n, 0.32, 0.09, scale=1.5, ring=0.6, snap=0.25, width=0.4)),
        ('open_hat', 'court', lambda n: cymbal(n, 0.22, 0.055, scale=1.5, ring=0.6, snap=0.3)),
        ('open_hat', 'long', lambda n: cymbal(n, 0.4, 0.13, scale=1.4, ring=0.6, hp=4800.0, snap=0.2, width=0.45)),
        ('perc', 'toc', lambda n: rim(n, F5, 0.14, pan=0.2)),
        ('perc', 'tom', lambda n: tom(n, F3, 0.25)),
        ('perc', 'métal', lambda n: clank(n, C6, 0.24)),
        ('perc', 'shaker', lambda n: shaker(n, 0.15, density=2500.0)),
        ('ride', 'scintillante', lambda n: ride(n, 1.3, ping=3100.0)),
        ('ride', 'cloche', lambda n: ride(n, 1.0, ping=2600.0, bell=0.8, ping_tau=0.4, wash_tau=0.24)),
        ('ride', 'sombre', lambda n: ride(n, 1.5, ping=2700.0, bell=0.3, wash_tau=0.42, damp=9.0)),
    ]},
]

LABELS = {'kick': 'Kick', 'snare': 'Caisse claire', 'clap': 'Clap', 'hat': 'Charley fermé',
          'open_hat': 'Charley ouvert', 'perc': 'Percussion', 'ride': 'Ride'}
MONO = {'kick', 'snare'}


def write_wav(path, x):
    pcm = np.round(np.clip(x, -1.0, 1.0) * 32767).astype('<i2')
    with wave.open(str(path), 'wb') as w:
        w.setnchannels(x.shape[1])
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())


def read_wav(path):
    with wave.open(str(path), 'rb') as w:
        ch, rate = w.getnchannels(), w.getframerate()
        x = np.frombuffer(w.readframes(w.getnframes()), dtype='<i2').astype(float) / 32768.0
    return x.reshape(-1, ch), rate


# --- Measures ------------------------------------------------------------------------------------

def fundamental(mono, rate, start=0.06, end=0.2):
    """A kick's fundamental over start-end s: the strongest peak between 25 and 120 Hz of the
    Hann-windowed segment's spectrum (zero-padded to 2^18 points, peak interpolated on a parabola
    through the log magnitudes). None when the segment is too short."""
    seg = mono[int(start * rate):int(end * rate)]
    if len(seg) < int(0.05 * rate):
        return None
    n = 1 << 18
    mag = np.abs(np.fft.rfft((seg - seg.mean()) * np.hanning(len(seg)), n)) + 1e-30
    freqs = np.fft.rfftfreq(n, 1.0 / rate)
    inside = np.flatnonzero((freqs > 25) & (freqs < 120))
    i = inside[np.argmax(mag[inside])]
    a, b, c = np.log(mag[i - 1:i + 2])
    return (i + 0.5 * (a - c) / (a - 2 * b + c)) * rate / n


# Energy bands of the report (Hz): name -> (low, high), each read against 20 Hz-20 kHz.
BANDS = {'sub_db': (20, 60), 'low_db': (60, 250), 'mid_db': (250, 4000), 'high_db': (4000, 20000),
         'e250_1500_db': (250, 1500), 'e1200_4500_db': (1200, 4500), 'e250_2000_db': (250, 2000)}


def tail_time(x, rate, below_db=40.0):
    """Seconds from the start to the last sample within below_db of the peak (any channel)."""
    a = np.max(np.abs(x), axis=1)
    return float(np.flatnonzero(a >= np.max(a) * 10 ** (-below_db / 20))[-1] + 1) / rate


def measure(x, rate, kind=''):
    """Duration, peak, RMS, crest factor, energy above 10 kHz (10-20 kHz against 20 Hz-20 kHz, as
    docs/sons/outils/voix.py), power-weighted centroid, side ((L-R)/2) against centre ((L+R)/2),
    the largest DC of a channel, the energy in each of BANDS against 20 Hz-20 kHz, the energy above
    10 kHz against 4-10 kHz, the tail (tail_time()), and a kick's fundamental."""
    mono = x.mean(axis=1)
    power = np.abs(np.fft.rfft(mono, 1 << int(np.ceil(np.log2(max(len(mono), 2)))))) ** 2
    freqs = np.linspace(0, rate / 2, len(power))

    def band(lo, hi):
        return float(power[(freqs >= lo) & (freqs < hi)].sum()) + 1e-30

    def db(v):
        return round(10 * float(np.log10(v)), 2)

    peak = float(np.max(np.abs(x)))
    rms = float(np.sqrt(np.mean(x ** 2)))
    out = {
        'duration_s': round(len(x) / rate, 3),
        'channels': x.shape[1],
        'peak_dbfs': round(20 * np.log10(peak), 2),
        'rms_dbfs': round(20 * np.log10(rms), 2),
        'crest_db': round(20 * np.log10(peak / rms), 2),
        'air_10k_plus_db': db(band(10000, 20000) / band(20, 20000)),
        'centroid_hz': round(float((power * freqs).sum() / power.sum())),
        'side_centre_db': (db((float(np.sum(((x[:, 0] - x[:, 1]) / 2) ** 2)) + 1e-30)
                              / (float(np.sum(((x[:, 0] + x[:, 1]) / 2) ** 2)) + 1e-30)) if x.shape[1] == 2 else None),
        'dc': round(float(np.max(np.abs(x.mean(axis=0)))), 6),
    }
    out.update({key: db(band(lo, hi) / band(20, 20000)) for key, (lo, hi) in BANDS.items()})
    out['top_10k_vs_4_10k_db'] = db(band(10000, 20000) / band(4000, 10000))
    out['tail_s'] = round(tail_time(x, rate), 3)
    if kind == 'kick':
        f0 = fundamental(mono, rate)
        out['f0_hz'] = round(f0, 2) if f0 else None
    return out


def print_report(report):
    cols = ('duration_s', 'peak_dbfs', 'rms_dbfs', 'crest_db', 'air_10k_plus_db', 'centroid_hz', 'side_centre_db',
            'dc', 'e250_1500_db', 'e1200_4500_db', 'e250_2000_db', 'top_10k_vs_4_10k_db', 'tail_s', 'f0_hz')
    heads = ('s', 'peak', 'rms', 'crest', '>10k', 'centroid', 'side', 'dc', '.25-1.5k', '1.2-4.5k', '.25-2k',
             '>10k/4-10', 'tail', 'f0')
    for pack, sounds in report.items():
        print(f'\n{pack}')
        print(f"{'file':<16}" + ''.join(f'{h:>10}' for h in heads))
        for file, m in sounds.items():
            cells = []
            for c in cols:
                v = m.get(c)
                cells.append('-' if v is None else f'{v:.4f}' if c == 'dc' else f'{v}')
            print(f'{file:<16}' + ''.join(f'{c:>10}' for c in cells))


# --- Main ----------------------------------------------------------------------------------------

def build(out):
    for pack in PACKS:
        d = out / pack['id']
        d.mkdir(parents=True, exist_ok=True)
        for old in d.glob('*.wav'):
            old.unlink()
        sounds, count = [], {}
        for kind, word, make in pack['sounds']:
            count[kind] = count.get(kind, 0) + 1
            file = f'{kind}_{count[kind]:02d}.wav'
            x = make(f"{pack['id']}/{file}")
            x = x / np.max(np.abs(x)) * 10 ** (PEAKS[kind] / 20)
            if not np.all(np.isfinite(x)) or x.shape[1] != (1 if kind in MONO else 2):
                raise RuntimeError(f"{pack['id']}/{file}: not finite or wrong channel count")
            write_wav(d / file, x)
            sounds.append({'file': file, 'kind': kind, 'name': f'{LABELS[kind]} essai {count[kind]}',
                           'tags': ['essai', word]})
        manifest = {'id': pack['id'], 'name': pack['name'], 'family': pack['family'], 'bpm': pack['bpm'],
                    'version': 2, 'sounds': sounds}
        (d / 'pack.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
        (d / 'LICENSE.txt').write_text(LICENSE, encoding='utf-8')
        print(f"{pack['id']}: {len(sounds)} sounds", flush=True)


def main(argv):
    args = [a for a in argv[1:] if not a.startswith('--')]
    if len(args) != 1 or any(a not in ('--report',) for a in argv[1:] if a.startswith('--')):
        print(__doc__, file=sys.stderr)
        return 2
    out = Path(args[0])
    build(out)
    if '--report' in argv:
        report = {}
        for pack in PACKS:
            d = out / pack['id']
            manifest = json.loads((d / 'pack.json').read_text(encoding='utf-8'))
            report[pack['id']] = {s['file']: measure(*read_wav(d / s['file']), s['kind']) for s in manifest['sounds']}
        (out / 'mesures.json').write_text(json.dumps(report, indent=1) + '\n', encoding='utf-8')
        print_report(report)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
