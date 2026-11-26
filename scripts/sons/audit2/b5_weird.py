"""Batch 5: weird files, crashes, cell formats, skipped formats."""
import os, sys, struct, shutil, subprocess
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *
ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "pack", "b5")
shutil.rmtree(ROOT, ignore_errors=True)
i = 0
COLS = ["type", "subtype", "dur_s", "sr", "ch", "size_kb", "peak_dbfs", "rms_dbfs", "lufs_i", "end_cut", "error"]
def raw(label, rel, data):
    global i
    i += 1
    root = os.path.join(ROOT, "%03d" % i)
    p = os.path.join(root, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "wb") as f:
        f.write(data)
    rows, con, code = run_pack(root, "b5_%03d" % i)
    print("--- " + label + "   [" + rel + "]")
    if not rows:
        print("    NO ROW. code=%s console=%s" % (code, con.strip().replace("\n", " | ")[:300]))
    for r in rows:
        print("    code=%s " % code + " ".join("%s=%s" % (c, r.get(c)) for c in COLS if r.get(c) not in (None, "")))

def wavbytes(x, sr=SR, bits=16, fl=False, fmt=None, ch=None, block=None, datasize=None, riffsize=None, pre=b"", post=b"", ext=False, riff=b"RIFF", hdrbits=None):
    if x.ndim == 1: x = x[:, None]
    nch = x.shape[1] if ch is None else ch
    data = pcm_bytes(x, bits) if not fl else x.astype("<f4").tobytes()
    tag = (3 if fl else 1) if fmt is None else fmt
    ba = nch * bits // 8 if block is None else block
    if ext:
        fmtc = struct.pack("<HHIIHHHHIH", 0xFFFE, nch, sr, sr * ba, ba, bits, 22, bits, 0, 3 if fl else 1) + b"\x00\x00\x00\x00\x10\x00\x80\x00\x00\xAA\x00\x38\x9B\x71"
    else:
        fmtc = struct.pack("<HHIIHH", tag, nch, sr, sr * ba, ba, bits if hdrbits is None else hdrbits)
    body = b"WAVE" + pre + b"fmt " + struct.pack("<I", len(fmtc)) + fmtc + b"data" + struct.pack("<I", len(data) if datasize is None else datasize) + data + post
    return riff + struct.pack("<I", len(body) if riffsize is None else riffsize) + body

k = kick(0.5, taua=0.06)
K = "Drums/Kicks/Kick 01.wav"
raw("control: 16-bit mono kick", K, wavbytes(k))
raw("8-bit unsigned", K, wavbytes(k, bits=8))
raw("24-bit", K, wavbytes(k, bits=24))
raw("32-bit int", K, wavbytes(k, bits=32))
raw("32-bit float", K, wavbytes(k, bits=32, fl=True))
raw("64-bit float (fmt 3, 64 bits)", K, (lambda x: b"RIFF" + struct.pack("<I", 36 + x.nbytes) + b"WAVE" + b"fmt " + struct.pack("<IHHIIHH", 16, 3, 1, SR, SR * 8, 8, 64) + b"data" + struct.pack("<I", x.nbytes) + x.tobytes())(k.astype("<f8")))
raw("WAVE_FORMAT_EXTENSIBLE 24-bit (very common in packs)", K, wavbytes(k, bits=24, ext=True))
raw("float file with values above 1.0 (peak +6 dB)", K, wavbytes(k * 2.2, bits=32, fl=True))
raw("float file containing NaN samples", K, wavbytes(np.where(np.arange(len(k)) % 1000 == 5, np.nan, k), bits=32, fl=True))
raw("float file containing +inf", K, wavbytes(np.where(np.arange(len(k)) == 50, np.inf, k), bits=32, fl=True))
raw("data size field = 0 (streamed WAV), audio present", K, wavbytes(k, datasize=0))
raw("data size field = 0xFFFFFFFF", K, wavbytes(k, datasize=0xFFFFFFFF))
raw("RIFF size wrong (0)", K, wavbytes(k, riffsize=0))
raw("block align = 0", K, wavbytes(k, block=0))
raw("channels = 0", K, wavbytes(k, ch=0))
raw("sample rate = 0", K, wavbytes(k, sr=0))
raw("sample rate = 8000", K, wavbytes(kick(0.5, taua=0.06, sr=8000), sr=8000))
raw("sample rate = 192000", K, wavbytes(kick(0.5, taua=0.06, sr=192000), sr=192000, bits=24))
raw("sample rate = 22050", K, wavbytes(kick(0.5, taua=0.06, sr=22050), sr=22050))
raw("6 channels (5.1)", K, wavbytes(np.stack([k] * 6, axis=1)))
raw("JUNK + bext chunks before fmt", K, wavbytes(k, pre=b"JUNK" + struct.pack("<I", 28) + b"\0" * 28 + b"bext" + struct.pack("<I", 10) + b"\0" * 10))
raw("odd-sized LIST chunk before fmt, padded (legal)", K, wavbytes(k, pre=b"LIST" + struct.pack("<I", 5) + b"INFOx" + b"\0"))
raw("acid + smpl chunks after data", K, wavbytes(k, post=b"acid" + struct.pack("<I", 24) + struct.pack("<IHHfIHHf", 1, 60, 0x8000, 0.0, 4, 4, 4, 120.0) ))
raw("empty file (0 byte)", K, b"")
raw("44-byte header only, 0 frame", K, wavbytes(np.zeros(0)))
raw("one single sample", K, wavbytes(np.array([0.5])))
raw("10 samples", K, wavbytes(np.full(10, 0.5)))
raw("pure digital silence 1 s", K, wavbytes(np.zeros(SR)))
raw("constant DC 0.5 for 1 s", K, wavbytes(np.full(SR, 0.5)))
raw("text file named .wav", K, b"this is not audio\n" * 50)
raw("MP3 data named .wav (ID3 header)", K, b"ID3\x03\x00\x00\x00\x00\x0f\x76" + b"\xff\xfb\x90\x00" * 400)
raw("RF64 header", K, wavbytes(k, riff=b"RF64"))
raw("RIFX (big endian)", K, wavbytes(k, riff=b"RIFX"))
raw("ADPCM (format 2)", K, wavbytes(k, fmt=2, hdrbits=4))
raw("mu-law (format 7)", K, wavbytes(k, fmt=7, bits=8))
raw("truncated in the middle of the data (declared 0.5 s, holds 0.1 s)", K, wavbytes(k)[:44 + 9600])
raw("truncated inside the header (20 bytes)", K, wavbytes(k)[:20])
raw("12-bit declared", K, wavbytes(k, hdrbits=12))
raw("upper-case extension .WAV", "Drums/Kicks/Kick 01.WAV", wavbytes(k))
raw("extension .wave", "Drums/Kicks/Kick 01.wave", wavbytes(k))
raw("macOS resource fork ._Kick 01.wav", "Drums/Kicks/._Kick 01.wav", b"\x00\x05\x16\x07\x00\x02\x00\x00Mac OS X        " + b"\0" * 200)
raw("hidden .DS_Store + flac + mp3 + ogg only", "Drums/Kicks/Kick 01.flac", b"fLaC" + b"\0" * 500)
raw("name with leading =  (formula)", "Drums/Kicks/=cmd 01.wav", wavbytes(k))
raw("name with comma, quote and newline-like chars", 'Drums/Kicks/Kick "big", 01;.wav', wavbytes(k))
raw("emoji and CJK in the name", "Drums/Kicks/Kick 🔥 太鼓 01.wav", wavbytes(k))
raw("very long name (240 chars)", "Drums/Kicks/Kick " + "x" * 220 + ".wav", wavbytes(k))
# AIFF
import io
def aiff(x, sr=SR, bits=16):
    p = os.path.join(ROOT, "tmp.aif"); write_aiff(p, x, sr, bits); d = open(p, "rb").read(); os.remove(p); return d
raw("AIFF 16-bit", "Drums/Kicks/Kick 01.aif", aiff(k))
raw("AIFF 24-bit", "Drums/Kicks/Kick 01.aiff", aiff(k, bits=24))
a = aiff(k)
raw("AIFF-C 'sowt' (little endian, written by macOS)", "Drums/Kicks/Kick 01.aifc", a.replace(b"AIFF", b"AIFC", 1))
raw("AIFF truncated", "Drums/Kicks/Kick 01.aif", a[:300])
# value formats
big = np.clip(noise(1.0, 0.9, 3), -1, 1)
raw("white noise 1 s (centroid near 10 kHz: 1e+04 ?)", "Drums/Hats/Hat 01.wav", wavbytes(big))
raw("tone at 9998 Hz", "Synth/Lead C3.wav", wavbytes(sine(9998.0, 1.0, 0.5)))
raw("very quiet file (-80 dBFS)", K, wavbytes(k * 1e-4, bits=24))
raw("11.5 MB file -> size_kb", "Synth/Pad C3.wav", wavbytes(tone(130.81, 5.0, 0.3), sr=96000, bits=32))
chk = Check("b5")
chk.clean_repo()
chk.done()
