"""Self-made FX for the "fx" family (tonal riser, sub drops, tape stop), numpy/scipy only, seeded.
The recipes are in each function's docstring. Usage: python3 fx_synth.py OUT_DIR  ->  OUT_DIR/<role>_synth<n>.flac
(16-bit FLAC, 48 kHz, peak -1 dBFS). Freesound picks for the same family: freesound/fx.json."""
import os, sys
import numpy as np, soundfile as sf
from scipy.signal import butter, sosfilt
SR = 48000
BAR = 4 * 60 / 124


def saw(phase):
    return 2 * (phase % 1.0) - 1


def svf_lp(x, fc):
    """Time-varying one-pole x2 low-pass (fc array in Hz)."""
    a = np.exp(-2 * np.pi * np.asarray(fc) / SR)
    y = np.empty_like(x); z1 = z2 = 0.0
    for i in range(len(x)):
        z1 = (1 - a[i]) * x[i] + a[i] * z1
        z2 = (1 - a[i]) * z1 + a[i] * z2
        y[i] = z2
    return y


def supersaw_riser(bars=2):
    """Tonal riser: 7 detuned saws per side (A minor triad A2-E3-A3), pitch +12 st (exponential) over the
    length, low-pass 400 Hz -> 14 kHz, level -30 dB -> 0 dB, plus a high-passed noise layer at -14 dB.
    Ends on the downbeat with a 20 ms release."""
    n = int(bars * BAR * SR); t = np.arange(n) / SR; u = t / t[-1]
    bend = 2 ** (u ** 2 * 12 / 12)
    out = np.zeros((n, 2))
    for ch in range(2):
        s = np.zeros(n)
        for f0 in (110.0, 164.81, 220.0):
            for k in range(7):
                det = (k - 3) * 0.11 + (0.035 if ch else -0.035)
                f = f0 * 2 ** (det / 12) * bend
                s += saw(np.cumsum(f) / SR + rng.random())
        nz = sosfilt(butter(2, 3000, 'highpass', fs=SR, output='sos'), rng.standard_normal(n))
        s = s / 21 + 0.2 * nz
        out[:, ch] = svf_lp(s, 400 * (14000 / 400) ** (u ** 1.5))
    out *= (10 ** (-30 / 20) * (1 / 10 ** (-30 / 20)) ** (u ** 1.3))[:, None]
    r = int(0.02 * SR); out[-r:] *= np.linspace(1, 0, r)[:, None]
    return out


def sub_drop(f_hi, f_lo, glide, length, click):
    """Sub drop: sine gliding f_hi -> f_lo (exponential, `glide` s), soft tanh drive x2.5 for 2nd harmonic,
    amplitude exp decay to -40 dB at `length`; optional 3 ms noise click at -12 dB."""
    n = int(length * SR); t = np.arange(n) / SR
    f = f_lo + (f_hi - f_lo) * np.exp(-t / (glide / 4))
    s = np.tanh(2.5 * np.sin(2 * np.pi * np.cumsum(f) / SR)) / np.tanh(2.5)
    s *= np.exp(np.log(10 ** (-40 / 20)) * t / length)
    a = int(0.004 * SR); s[:a] *= np.linspace(0, 1, a)
    if click:
        c = int(0.003 * SR); s[:c] += 0.25 * rng.standard_normal(c) * np.linspace(1, 0, c)
    return s[:, None]


def tape_stop():
    """Tape stop: 0.5 s of a filtered saw chord (C minor 7, C3), then the playback speed falls from 1 to 0
    over 1.0 s (quadratic), read by variable-rate interpolation; level fades with speed."""
    src_len = 3 * SR; t = np.arange(src_len) / SR
    s = sum(saw(f * t + rng.random()) for f in (130.81, 155.56, 196.0, 233.08)) / 4
    s = sosfilt(butter(2, 2500, 'lowpass', fs=SR, output='sos'), s)
    hold, stop = int(0.5 * SR), int(1.0 * SR)
    speed = np.concatenate([np.ones(hold), (1 - np.linspace(0, 1, stop)) ** 2])
    pos = np.cumsum(speed)
    y = np.interp(pos, np.arange(src_len), s) * np.concatenate([np.ones(hold), np.sqrt(speed[hold:])])
    return y[:, None]


def make(seed=124):
    """name -> float array (n, channels), peak 0.89."""
    global rng
    rng = np.random.default_rng(seed)
    made = {'riser_tonal_synth1': supersaw_riser(2),
            'sub_drop_synth1': sub_drop(90, 32, 1.2, 2.0, True),
            'sub_drop_synth2': sub_drop(160, 28, 2.0, 2.8, False),
            'tape_stop_synth1': tape_stop()}
    return {k: y * 0.89 / np.abs(y).max() for k, y in made.items()}


def main(out):
    os.makedirs(out, exist_ok=True)
    for name, y in make().items():
        sf.write(os.path.join(out, name + '.flac'), y.astype(np.float32), SR, subtype='PCM_16')
        print(name, y.shape[1], 'ch', round(len(y) / SR, 2), 's')


rng = np.random.default_rng(124)
if __name__ == '__main__':
    main(sys.argv[1])
