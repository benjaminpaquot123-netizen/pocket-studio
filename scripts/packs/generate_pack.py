"""Generates one sound pack with Stable Audio Open Small (fp32 ONNX), keeps the good takes.

usage: generate_pack.py MODEL_DIR styles.json STYLE_ID OUT_DIR [--scale F]

For each kind of sound of the style (kick, snare, ...), seeds 1, 2, ... are generated from the
kind's prompt with the style's words; each take is trimmed, faded, normalized and checked (not
silent, energy where that kind of sound has it, a real hit for drums). The first good takes
(the kind's count, times --scale) are written as 16-bit FLAC <kind>_<n>.flac, plus pack.json
(names and kinds, read by the app).
"""
import json
import sys
from pathlib import Path

import numpy as np
import onnxruntime as ort
import soundfile as sf
from tokenizers import Tokenizer

RATE = 44100
STEPS = 8


class Model:
    def __init__(self, d):
        opts = ort.SessionOptions()
        opts.intra_op_num_threads = 4
        p = ['CPUExecutionProvider']
        # The app's int8 files when present (twice as fast, same sound to the ear), else fp32.
        q = Path(d, 'dit_l8.onnx').exists()
        self.cond = ort.InferenceSession(f"{d}/{'cond_q8' if q else 'cond'}.onnx", opts, providers=p)
        self.dit = ort.InferenceSession(f"{d}/{'dit_l8' if q else 'dit'}.onnx", opts, providers=p)
        self.dec = ort.InferenceSession(f'{d}/dec.onnx', opts, providers=p)
        self.tok = Tokenizer.from_file(f'{d}/tokenizer.json')

    def generate(self, prompt, seconds, seed):
        ids = self.tok.encode(prompt).ids[:64]
        n = len(ids)
        ids = ids + [0] * (64 - n)
        mask = [1] * n + [0] * (64 - n)
        cross, glob = self.cond.run(None, {
            'input_ids': np.array([ids], np.int64), 'attention_mask': np.array([mask], np.int64),
            'seconds_total': np.array([seconds], np.float32)})
        rng = np.random.default_rng(seed)
        u = np.linspace(1, 0, STEPS + 1)
        ts = 1 / (1 + np.exp(-(8.2 * u - 2.0)))
        ts[0], ts[-1] = 1.0, 0.0
        x = rng.standard_normal((1, 64, 256)).astype(np.float32)
        for i in range(STEPS):
            v, = self.dit.run(None, {'x': x, 't': np.array([ts[i]], np.float32), 'cross': cross, 'global': glob})
            den = x - ts[i] * v
            x = ((1 - ts[i + 1]) * den + ts[i + 1] * rng.standard_normal(x.shape)).astype(np.float32)
        lat = int(np.ceil(seconds * RATE / 2048))
        audio, = self.dec.run(None, {'z': x[:, :, :lat]})
        return audio[0, :, :int(seconds * RATE)].T.astype(np.float64)


def band_share(mono, lo, hi):
    spec = np.abs(np.fft.rfft(mono * np.hanning(len(mono)))) ** 2
    f = np.fft.rfftfreq(len(mono), 1 / RATE)
    total = spec.sum() + 1e-12
    return spec[(f >= lo) & (f < hi)].sum() / total


def clean(audio, one_shot):
    """Trimmed, faded, normalized to -1 dBFS; None when the take is unusable."""
    if not np.all(np.isfinite(audio)):
        return None
    mono = audio.mean(axis=1)
    peak = np.abs(mono).max()
    if peak < 1e-3:
        return None
    env = np.abs(mono) / peak
    loud = np.nonzero(env > 0.05)[0]
    start = max(0, loud[0] - int(0.002 * RATE)) if one_shot else 0
    # The end: last point above -50 dB, a short fade after it.
    quiet = np.nonzero(env > 0.003)[0]
    end = min(len(mono), quiet[-1] + int(0.02 * RATE))
    out = audio[start:end].copy()
    if len(out) < int(0.05 * RATE):
        return None
    fade_in = min(int(0.001 * RATE), len(out))
    out[:fade_in] *= np.linspace(0, 1, fade_in)[:, None]
    fade = min(int(0.03 * RATE), len(out) // 4)
    out[-fade:] *= np.linspace(1, 0, fade)[:, None]
    out *= 0.89 / np.abs(out).max()
    return out


def accept(kind, audio):
    mono = audio.mean(axis=1)
    rms = np.sqrt((mono ** 2).mean())
    rule = KIND_RULES.get(kind, {})
    if rms < 0.01:
        return False
    if 'low' in rule and band_share(mono, 20, 200) < rule['low']:
        return False
    if 'max_low' in rule and band_share(mono, 20, 200) > rule['max_low']:  # a snare is not a kick
        return False
    if 'high' in rule and band_share(mono, 4000, 20000) < rule['high']:
        return False
    if 'max_seconds' in rule and len(mono) / RATE > rule['max_seconds']:
        return False
    if rule.get('transient') and not is_hit(mono):
        return False
    return True


def is_hit(mono):
    """A drum hit: loudest near the start, then dying away (not a pad, a loop or noise)."""
    env = np.abs(mono)
    if np.argmax(env) > int(0.04 * RATE):
        return False
    q = max(1, len(mono) // 4)
    first = np.sqrt((mono[:q] ** 2).mean())
    last = np.sqrt((mono[-q:] ** 2).mean())
    return last < 0.5 * first


KIND_RULES = {
    'kick': {'low': 0.45, 'max_seconds': 1.6, 'transient': True},
    '808': {'low': 0.5},
    'snare': {'max_seconds': 1.2, 'transient': True, 'max_low': 0.6},
    'clap': {'max_seconds': 1.2, 'transient': True, 'max_low': 0.5},
    'hat': {'high': 0.35, 'max_seconds': 0.8, 'transient': True},
    'open_hat': {'high': 0.3, 'max_seconds': 1.6},
    'perc': {'max_seconds': 1.2, 'transient': True},
    'bass': {'low': 0.3},
    'fx': {},
    'loop': {},
}


def main():
    model_dir, styles_path, style_id, out_dir = sys.argv[1:5]
    scale = float(sys.argv[sys.argv.index('--scale') + 1]) if '--scale' in sys.argv else 1.0
    catalog = json.loads(Path(styles_path).read_text())
    style = next(s for s in catalog['styles'] if s['id'] == style_id)
    model = Model(model_dir)
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    sounds = []
    for k in style['kinds']:
        kind = catalog['kinds'][k]
        count = max(1, round(kind['count'] * scale))
        one_shot = k not in ('loop', 'fx')
        prompt = kind['prompt'].replace('{words}', style['words'])
        kept, seed = 0, 0
        while kept < count and seed < count * 5:
            seed += 1
            take = clean(model.generate(prompt, kind['seconds'], seed), one_shot)
            if take is None or not accept(k, take):
                continue
            kept += 1
            name = f'{k}_{kept:02d}.flac'
            sf.write(out / name, take.astype(np.float32), RATE, subtype='PCM_16')
            sounds.append({'file': name, 'kind': k, 'name': f"{kind['label']} {kept}"})
            print(f'{style_id}: {name} ({len(take) / RATE:.2f} s, seed {seed})', flush=True)
        if kept < count:
            print(f'{style_id}: only {kept}/{count} {k}', flush=True)
    (out / 'pack.json').write_text(json.dumps({
        'id': style_id, 'name': style['name'], 'family': style['family'], 'bpm': style['bpm'], 'sounds': sounds,
    }, ensure_ascii=False, indent=1))


if __name__ == '__main__':
    main()
