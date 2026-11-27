"""Mutation testing of scripts/sons self-tests, on scratch copies only.

usage: python3 -I mutate.py <pristine_repo_copy> <work_dir> <out_file> <budget_seconds>
One fault at a time is put in a copy of tout.py (or mesure.py), every self-test
of the tool runs (selftest_tout.py, then each selftest_tout_*.py, plus selftest.py
for mesure.py), and the mutant is 'killed' when a test fails.
Importing this file starts nothing: the run is behind __main__.
"""
import os
import re
import shutil
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor

REPO = WORK = OUT = None
SONS = os.path.join("scripts", "sons")

M = []  # (family, name, file, old, new)
DEADLINE = float("inf")


def tests_for(f, sons_dir):
    """The self-tests a mutant of file f must survive: the main one first, then every lot's."""
    lots = sorted(n for n in os.listdir(sons_dir)
                  if n.startswith("selftest_tout_") and n.endswith(".py"))
    return ["selftest_tout.py"] + lots + (["selftest.py"] if f == "mesure.py" else [])


def m(fam, name, old, new, f="tout.py"):
    M.append((fam, name, f, old, new))


# ---- names -> type
m("type", "drum one-shot max 6 s -> 3 s", "LONG_FAMILIES else 6.0)", "LONG_FAMILIES else 3.0)")
m("type", "melodic one-shot max 8 s -> 4 s", "dur < 8.0) or ONESHOT_DIR", "dur < 4.0) or ONESHOT_DIR")
m("type", "bpm word makes a loop from 1.5 s -> 0.2 s", "dur >= 1.5)", "dur >= 0.2)")
m("type", "fx word wins even on a drum name", "if FX_RX.search(text) and not drum_named:", "if FX_RX.search(text):")
m("type", "drum loop even when a melodic word is there", '"drum_loop" if drummy and not melodic else', '"drum_loop" if drummy else')
m("type", "kick in an 808 folder no longer kick_808", 'if fam == "kick" and re.search(r"808", inner):', 'if False and re.search(r"808", inner):')
m("type", "long 808 no longer allowed 10 s", 'LONG_FAMILIES = ("kick_808", "crash", "cymbal", "ride")', 'LONG_FAMILIES = ("crash", "cymbal", "ride")')
m("type", "'fill' no longer a loop word", r'|\bbeats?\b|\bfills?\b")', r'|\bbeats?\b")')
m("type", "'beat' no longer a loop word", r'|\bgrooves?\b|\bbeats?\b|', r'|\bgrooves?\b|')
m("type", "'phrase' no longer a vocal word", r'|\bspoken|\bphrase")', r'|\bspoken")')
m("type", "'spoken' no longer a vocal word", r'|\bchant|\bspoken|', r'|\bchant|')
m("type", "melodic word no longer blocks a drum one-shot", r're.search(r"(?i)bass|synth|lead|pad|key|piano|chord|pluck", name + "/" +', r're.search(r"(?i)zzzzzz", name + "/" +')
m("type", "short drum in a kit: 6 s -> 1 s", "dur is not None and dur < 6.0", "dur is not None and dur < 1.0")
m("type", "'sweep' no longer an fx word", "sweep|whoosh", "whoosh")
m("type", "'reverse' no longer an fx word", "noise|reverse|", "noise|")
m("type", "'tom' no longer a drum word (loops)", r'\bride\b|cymbal|tom|fill")', r'\bride\b|cymbal|fill")')
m("type", "'arp' no longer a melodic word (loops)", "pluck|arp|melod", "pluck|melod")
# ---- names -> role / family / style
m("role", "vocal type loses its vocal role", 'out["type"] = "vocal"\n        out["role"] = "vocal"', 'out["type"] = "vocal"')
m("role", "clap no longer in the snare role", r'("snare", r"snare|clap|\bsd\b")', r'("snare", r"snare|\bsd\b")')
m("role", "shaker no longer in the hats role", r'|\bride\b|shaker|\b[co]?hh\b|\bcrash")', r'|\bride\b|\b[co]?hh\b|\bcrash")')
m("role", "808 role never given", '("808", r"808"),', '("808", r"909"),')
m("role", "'mix' no longer the full role", r'("full", r"\bfull\b|\bmix\b|', r'("full", r"\bfull\b|')
m("role", "'melody' no longer the lead role", '("lead", r"lead|melody|hook', '("lead", r"lead|hook')
m("role", "'piano' no longer the keys role", r'("keys", r"\bkeys\b|piano|', r'("keys", r"\bkeys\b|')
m("role", "kick role never given", r'("kick", r"\bkicks?\b|\bbd\b")', r'("kick", r"\bbd\b")')
m("role", "'top' no longer the drums role", r'("drums", r"drum|\btop\b|\btops\b|', r'("drums", r"drum|')
m("role", "open hat read as closed hat", r'("hat_open", r"\bo ?hh\b|', r'("hat_closed", r"\bo ?hh\b|')
m("role", "closed hat read as open hat", r'("hat_closed", r"\bc ?hh\b|', r'("hat_open", r"\bc ?hh\b|')
m("role", "perc mid read as perc low", r'("perc_mid", r"\bperc', r'("perc_low", r"\bperc')
m("role", "perc high read as perc low", r'("perc_high", r"\bperc', r'("perc_low", r"\bperc')
m("style", "techno never found", '("techno", r"techno")', '("techno", r"tekkno")')
m("style", "house never found", '("house", r"house")', '("house", r"haus")')
m("style", "trap never found", '("trap", r"trap")', '("trap", r"trapp")')
m("style", "boom bap no longer hiphop", r'("hiphop", r"hip.?hop|boom.?bap")', r'("hiphop", r"hip.?hop")')
m("style", "'rnb' no longer rnb", r'("rnb", r"r ?n ?b|r&b|soul")', r'("rnb", r"r&b|soul")')
m("style", "lofi never found", r'("lofi", r"lo.?fi|chillhop")', r'("lofi", r"chillhop")')
m("style", "dnb never found", r'("dnb", r"dnb|drum ?(and|&|n) ?bass|jungle")', r'("dnb", r"jungle")')
m("style", "plural 'souls' names soul music again", r'|garage ?band|\bsouls\b")', r'|garage ?band")')
m("style", "--pack-style drops the hyphen of drift-phonk", 'r"[^a-z0-9-]", "", args.pack_style', 'r"[^a-z0-9]", "", args.pack_style')
m("type", "reversed crash filed as fx keeps the role hats", '                out["role"] = "fx"  # a riser', '                pass  # a riser')
# ---- names -> tempo / key
m("tags", "bpm word accepted up to 180 only", "if 40 <= v <= 250:", "if 40 <= v <= 180:")
m("tags", "bare number accepted from 100 only", "if 60 <= v <= 200:", "if 100 <= v <= 200:")
m("tags", "flat goes up instead of down", "        pc -= 1", "        pc += 1")
m("tags", "key without mode = minor", 'mode or "major")', 'mode or "minor")')
m("tags", "'96k', '96 kHz', '24 bit' read as tempo again", "(?i:bars?|x|k|khz|hz|bits?)", "(?i:bars?|x)")
m("tags", "'140-bpm' no longer read", r"\s*-?\s*bpm\b", r"\s*bpm\b")
m("tags", "relative key: +3 -> +4", "(tag[0] + 3) % 12", "(tag[0] + 4) % 12")
# ---- level / duration
m("level", "duration 2 % too long", '"dur_s": x.shape[0] / sr,', '"dur_s": x.shape[0] / sr * 1.02,')
m("level", "peak: 20 log -> 10 log", '"peak_dbfs": 20 * math.log10(pk)', '"peak_dbfs": 10 * math.log10(pk)')
m("level", "rms 3 dB too high", '"rms_dbfs": _db(float(np.mean(p))),', '"rms_dbfs": _db(2 * float(np.mean(p))),')
m("level", "lufs 3 dB too low", "return -0.691 + 10 * math.log10(np.mean(z2))", "return -3.691 + 10 * math.log10(np.mean(z2))")
m("level", "channels always 2", '"ch": x.shape[1],', '"ch": 2,')
m("level", "16-bit decoded at half level", "/ 32768.0", "/ 65536.0", "mesure.py")
m("level", "24-bit decoded at half level", "/ float(1 << 23)", "/ float(1 << 24)", "mesure.py")
# ---- cut at the end
m("cut", "trailing silence trimmed under -70 instead of -90", "np.abs(s) > 10 ** (-90 / 20))[0]", "np.abs(s) > 10 ** (-70 / 20))[0]")
m("cut", "fade needs a 40 dB drop instead of 20", "if drop >= 20.0 and rises <= 3.0:", "if drop >= 40.0 and rises <= 3.0:")
m("cut", "step threshold -60 -> -40", "step = last > 10 ** (-60 / 20) and", "step = last > 10 ** (-40 / 20) and")
m("cut", "step: 4x motion -> 1.5x", "last > 4.0 * motion", "last > 1.5 * motion")
m("cut", "short fade counts from 8 ms instead of 4", "if sf is not None and sf >= 4.0:", "if sf is not None and sf >= 8.0:")
m("cut", "fade counts from 12 ms instead of 4", "faded = fade_ms is not None and fade_ms >= 4.0", "faded = fade_ms is not None and fade_ms >= 12.0")
m("cut", "loud threshold -45 -> -30 (the docstring value)", "loud = lvl > 10 ** (-45 / 20) and not faded", "loud = lvl > 10 ** (-30 / 20) and not faded")
m("cut", "loud threshold -45 -> -60", "loud = lvl > 10 ** (-45 / 20) and not faded", "loud = lvl > 10 ** (-60 / 20) and not faded")
m("cut", "step rule removed", "cut = (step and not faded) or loud", "cut = loud")
m("cut", "loud rule removed", "cut = (step and not faded) or loud", "cut = (step and not faded)")
m("cut", "seamless-loop pardon removed", "if cut and _seamless(s, sr, x0):", "if False and _seamless(s, sr, x0):")
# ---- fences
m("fence", "end_cut shown on 'full' role", 'if typ not in END_TYPES or row.get("role") == "full" or named or row.get("_loop"):', 'if typ not in END_TYPES or named or row.get("_loop"):')
m("fence", "end_cut shown on one-shots with a bpm in the name", 'or named or row.get("_loop"):', 'or row.get("_loop"):')
m("fence", "tempo-named one-shot that ends cut loses its end_cut", "if hit_cut:", "if False:")
m("fence", "name tempo forgotten once bpm_tag is emptied", 'row.get("bpm_tag") is not None or row.get("_name_bpm") is not None)', 'row.get("bpm_tag") is not None)')
m("fence", "end_cut shown on fx and vocals", 'END_TYPES = ("oneshot_melodic", "drum_oneshot")', 'END_TYPES = ("oneshot_melodic", "drum_oneshot", "fx", "vocal")')
m("fence", "end_cut shown on drum loops", 'END_TYPES = ("oneshot_melodic", "drum_oneshot")', 'END_TYPES = ("oneshot_melodic", "drum_oneshot", "drum_loop")')
m("fence", "fade length no longer rounded to tens", 'row["end_fade_ms"] = max(10, int(round(float(row["end_fade_ms"]) / 10.0)) * 10)', 'pass')
m("fence", "vocal wet/dry shown again", 'row["wet"] = None  # the dry', 'pass  # the dry')
m("fence", "untagged loop tempo shown again", '            row["bpm_est"] = None', '            pass')
m("fence", "untagged loop key shown again", '            row["key_est"] = None', '            pass')
m("fence", "midi chords shown again", 'for k in ("key_measured", "key_est", "key_match", "chords", "chord_changes_per_bar",', 'for k in ("key_measured", "key_est", "key_match",')
m("fence", "accuracy usable on the raw error rate (no Wilson bound)", '"yes" if n and _wilson_upper(n - a, n) < 0.05', '"yes" if n and (n - a) / n < 0.05')
m("fence", "Wilson bound under 50 % error", '"yes" if n and _wilson_upper(n - a, n) < 0.05', '"yes" if n and _wilson_upper(n - a, n) < 0.5')
m("fence", "tempo agreement tolerance 2 % -> 20 %", "return abs(m - t) / t <= 0.02", "return abs(m - t) / t <= 0.2")
# ---- tempo / bars
m("tempo", "tempo search 60-200 -> 80-160", "bpms = np.arange(60.0, 200.01, 0.25)", "bpms = np.arange(80.0, 160.01, 0.25)")
m("tempo", "tempo read on the first 4 s only", "sig = sig[: int(40 * sr)]", "sig = sig[: int(4 * sr)]")
m("tempo", "half-tempo weight 0.3 -> 0.9", "(0.5, 0.3))", "(0.5, 0.9))")
m("tempo", "bars: 240 -> 120 (double)", "b = dur * bpm / 240.0", "b = dur * bpm / 120.0")
m("tempo", "bars rounded whatever the remainder", "abs(b - round(b)) < 0.1 else nb", "abs(b - round(b)) < 0.6 else nb")
m("tempo", "bpm gap sign flipped", "return 100.0 * (meas - tag) / tag", "return 100.0 * (tag - meas) / tag")
m("tempo", "swing default 50 -> 66 on drum loops", "- bias if len(us) >= 4 else 0.5", "- bias if len(us) >= 4 else 0.66")
m("tempo", "density per beat doubled", 'out["density_per_beat"] = len(on_t) / (dur / (4 * step))', 'out["density_per_beat"] = len(on_t) / (dur / (8 * step))')
# ---- pitch / one-shot measures
m("pitch", "tuning 440 -> 432", "math.log2(f / 440.0)", "math.log2(f / 432.0)")
m("oneshot", "closed hat up to 2 s", 'out["hat_measured"] = "closed" if t40 < 200 else "open"', 'out["hat_measured"] = "closed" if t40 < 2000 else "open"')
m("oneshot", "attack ten times too short", 'out["attack_ms"] = max(0.0, 1000 * (tb - ta))', 'out["attack_ms"] = max(0.0, 100 * (tb - ta))')
m("oneshot", "stereo width inverted", 'out["stereo_width"] = ps / (pm + ps)', 'out["stereo_width"] = pm / (pm + ps)')
m("oneshot", "peak position halved", 'out["peak_pos"] = float(tt[int(np.argmax(e))] / dur)', 'out["peak_pos"] = float(tt[int(np.argmax(e))] / (2 * dur))')
# ---- midi
m("midi", "time signature ignored (always 4/4)", "bar = int(tpq * 4 * ts[0] / ts[1])", "bar = int(tpq * 4)")
m("midi", "swing default 50 -> 0", "if len(us) >= 4 else 50.0", "if len(us) >= 4 else 0.0")
m("midi", "drum track if ANY note on channel 10", "1 if all(n[4] == 9 for n in notes)", "1 if any(n[4] == 9 for n in notes)")
m("midi", "tempo = last tempo event", "60e6 / tempos[0][1] if tempos and tempos[0][1] > 0", "60e6 / tempos[-1][1] if tempos and tempos[0][1] > 0")
m("midi", "program change read with 2 data bytes", "nd = 1 if hi in (0xC0, 0xD0) else 2", "nd = 2")
m("midi", "note-on velocity 0 no longer a note-off", "elif hi == 0x80 or (hi == 0x90 and data[1] == 0):", "elif hi == 0x80:")
m("midi", "bar count without the small tolerance", "- tpq // 8) / bar)))", ") / bar)))")
m("midi", "polyphony counts legato as overlap", "key=lambda e: (e[0], e[1]))", "key=lambda e: (e[0], -e[1]))")
m("midi", "notes per bar off by one bar", 'out["notes_per_bar"] = len(notes) / nbars', 'out["notes_per_bar"] = len(notes) / (nbars + 1)')
# ---- writing / walking
m("write", "numbers written with 2 digits", '"%.4g" % v if abs(v) < 1e4', '"%.2g" % v if abs(v) < 1e4')
m("write", "'nan' written in cells", '"" if not math.isfinite(v) else "%.4g"', '"nan" if not math.isfinite(v) else "%.4g"')
m("write", "hidden '._' files measured", 'if name.startswith("._") or name.startswith("."):', 'if False:')
m("write", "max length 600 s -> 60 s", "MAX_SECONDS = 600.0", "MAX_SECONDS = 60.0")
m("write", "formula guard: + and - no longer quoted", 'text[:1] in "=+-@"', 'text[:1] in "=@"', "mesure.py")


def run_one(args):
    k, (fam, name, f, old, new) = args
    if time.time() > DEADLINE:
        return (k, fam, name, "NOTRUN", "")
    src = open(os.path.join(REPO, SONS, f), encoding="utf-8").read()
    n = src.count(old)
    if n != 1:
        return (k, fam, name, "SKIPPED (text found %d times)" % n, "")
    w = os.path.join(WORK, "m%03d" % k)
    shutil.rmtree(w, ignore_errors=True)
    shutil.copytree(REPO, w, symlinks=True)
    with open(os.path.join(w, SONS, f), "w", encoding="utf-8") as fh:
        fh.write(src.replace(old, new))
    verdict, why = "survived", ""
    for test in tests_for(f, os.path.join(w, SONS)):
        try:
            p = subprocess.run([sys.executable, "-I", test], cwd=os.path.join(w, SONS),
                               capture_output=True, text=True, timeout=300)
            text = p.stdout + p.stderr
            mm = re.search(r"(\d+) checks, (\d+) failed", text)
            bad = [ln for ln in text.splitlines() if ln.startswith("FAIL")]
            if p.returncode != 0 or (mm and int(mm.group(2)) > 0) or not mm:
                verdict = "killed"
                why = "%s: %s failed; first: %s" % (test, mm.group(2) if mm else "crash",
                                                    (bad[0][:110] if bad else text.strip().splitlines()[-1][:110]))
                break
        except subprocess.TimeoutExpired:
            verdict, why = "killed", "%s: timeout" % test
            break
    shutil.rmtree(w, ignore_errors=True)
    return (k, fam, name, verdict, why)


def main():
    global REPO, WORK, OUT, DEADLINE
    REPO, WORK, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
    DEADLINE = time.time() + float(sys.argv[4])
    os.makedirs(WORK, exist_ok=True)
    # a self-test that is red before any fault would "kill" every mutant
    for test in tests_for("mesure.py", os.path.join(REPO, SONS)):  # the widest list
        p = subprocess.run([sys.executable, "-I", test], cwd=os.path.join(REPO, SONS),
                           capture_output=True, text=True, timeout=300)  # the mutants' own limit
        mm = re.search(r"(\d+) checks, (\d+) failed", p.stdout + p.stderr)
        if p.returncode != 0 or not mm or int(mm.group(2)) > 0:
            sys.exit("baseline red, nothing measured: %s on the pristine copy (%s)"
                     % (test, mm.group(0) if mm else "crash"))
    # round robin over the families, so every family is sampled early
    byfam = {}
    for i, mm in enumerate(M):
        byfam.setdefault(mm[0], []).append((i, mm))
    order = []
    while any(byfam.values()):
        for fam in list(byfam):
            if byfam[fam]:
                order.append(byfam[fam].pop(0))
    open(OUT, "w").close()
    with ThreadPoolExecutor(max_workers=4) as ex:
        for k, fam, name, verdict, why in ex.map(run_one, order):
            with open(OUT, "a", encoding="utf-8") as fh:
                fh.write("%-8s|%-9s|%-55s|%s\n" % (verdict.split()[0], fam, name, why if verdict == "killed" else verdict if verdict.startswith("SKIP") else ""))


if __name__ == "__main__":
    main()
