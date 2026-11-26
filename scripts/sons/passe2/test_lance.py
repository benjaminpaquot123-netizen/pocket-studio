#!/usr/bin/env python3
"""End-to-end test of the deep-pass launcher on a fake RAR5 archive of synthetic sounds.

  python3 -I scripts/sons/passe2/test_lance.py

Needs RARLAB unrar (apt-get install -y unrar). The archive is written by
fakerar.py (stored entries) and checked by `unrar t`, so the real unrar path
is exercised: download (file:// URL through curl), test, extract, packs,
tout.py per pack, merged CSVs, forced style, control, cleanup, repository guard.
"""
import csv
import math
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import wave

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
sys.path.insert(0, HERE)
import fakerar  # noqa: E402
import lance  # noqa: E402

SR = 44100
FAILS = []


def check(cond, msg):
    print(("ok   " if cond else "FAIL ") + msg)
    if not cond:
        FAILS.append(msg)


def wav(path, kind, secs):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    n = int(SR * secs)
    out = bytearray()
    for i in range(n):
        t = i / SR
        if kind == "kick":
            v = math.sin(2 * math.pi * (50 + 100 * math.exp(-t * 30)) * t) * math.exp(-t * 8)
        elif kind == "loop":  # 120 BPM clicks over a 110 Hz tone
            v = 0.3 * math.sin(2 * math.pi * 110 * t) + (0.6 if (t % 0.5) < 0.01 else 0.0)
        else:
            v = 0.4 * math.sin(2 * math.pi * 440 * t)
        out += struct.pack("<h", int(max(-1, min(1, v)) * 20000))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(bytes(out))


def midi(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    trk = b"\x00\x90\x3c\x64\x83\x60\x80\x3c\x00\x00\xff\x2f\x00"
    with open(path, "wb") as fh:
        fh.write(b"MThd" + struct.pack(">IHHH", 6, 0, 1, 480) + b"MTrk" + struct.pack(">I", len(trk)) + trk)


def build(src):
    a = os.path.join(src, "Bundle", "Ghosthack - Blood Rave")
    wav(os.path.join(a, "One Shots", "Kicks", "BR Kick 01.wav"), "kick", 0.4)
    wav(os.path.join(a, "One Shots", "Kicks", "BR Kick 02.wav"), "kick", 0.5)
    wav(os.path.join(a, "Loops", "Bass Loops", "BR Bass Loop 01 120 BPM Am.wav"), "loop", 4.0)
    wav(os.path.join(a, "Techno Stabs", "BR Stab 01 C.wav"), "tone", 0.6)  # inner folder names a style
    midi(os.path.join(a, "MIDI", "BR Melody 01 120 BPM Am.mid"))
    k = os.path.join(a, "Construction Kits", "Kit 01 120 BPM Am")
    wav(os.path.join(k, "BR Kit 01 Drums 120 BPM Am.wav"), "loop", 4.0)
    wav(os.path.join(k, "BR Kit 01 Bass 120 BPM Am.wav"), "loop", 4.0)
    b = os.path.join(src, "Bundle", "Ghosthack - Riddim Riot")
    wav(os.path.join(b, "Loops", "RR Drum Loop 01 120 BPM.wav"), "loop", 4.0)
    with open(os.path.join(b, "Preset 01.fxp"), "wb") as fh:
        fh.write(b"CcnK" + b"\0" * 60)
    os.makedirs(os.path.join(src, "Bundle", "Docs"))
    with open(os.path.join(src, "Bundle", "Docs", "readme.pdf"), "wb") as fh:
        fh.write(b"%PDF-1.4\n")
    return 8  # wav + midi files in the two packs (p1: 6 wav + 1 mid, p2: 1 wav)


def rows(path):
    with open(path, newline="", encoding="utf-8") as fh:
        return list(csv.DictReader(fh))


def run(args, env=None):
    return subprocess.run(["bash", os.path.join(HERE, "lance.sh")] + args, capture_output=True, text=True,
                          env=env)


def main():
    if not shutil.which("unrar"):
        print("FAIL unrar missing (apt-get install -y unrar)")
        return 1
    base = tempfile.mkdtemp(prefix="passe2-test-")
    try:
        src, out, work = (os.path.join(base, d) for d in ("src", "out", "work"))
        for d in (src, out, work):
            os.mkdir(d)
        nfiles = build(src)
        rar = os.path.join(base, "fake.rar")
        fakerar.main(["fakerar", rar, src])
        check(subprocess.run(["unrar", "t", "-idq", rar]).returncode == 0, "fake archive passes unrar t")

        env = dict(os.environ, PASSE2_WORK=work)
        r = run(["--rar", "7", "--url", "file://" + rar, "--out", out, "--plan"], env)
        check(r.returncode == 0, "--plan exits 0 (%s)" % r.stderr.strip()[-200:])
        check("p1" in r.stdout and "Blood Rave" in r.stdout and "forced techno" in r.stdout, "--plan lists p1 forced techno")
        check("Riddim Riot" in r.stdout and "dubstep" in r.stdout, "--plan lists p2 dubstep")
        check("Docs" not in r.stdout, "a folder without sounds is no pack")
        check(os.listdir(out) == [], "--plan writes nothing")
        check(os.listdir(work) == [], "--plan leaves no work folder")

        r = run(["--rar", "7", "--url", "file://" + rar, "--out", out], env)
        print(r.stdout.strip())
        check(r.returncode == 0, "full run exits 0 (rc %d, %s)" % (r.returncode, r.stderr.strip()[-300:]))
        check(os.listdir(work) == [], "download and extraction deleted")
        ctl = rows(os.path.join(out, "rar7_controle.csv"))
        check([c["pack"] for c in ctl] == ["p1", "p2"], "control lists both packs")
        check(all(c["count_ok"] == "yes" and c["errors"] == "0" for c in ctl), "control: rows = files, 0 error")
        check(sum(int(c["files_audio"]) + int(c["files_midi"]) for c in ctl) == nfiles, "files read = %d" % nfiles)
        check(ctl[1]["skipped"] == ".fxp:1", "preset counted as skipped")
        data = []
        for f in sorted(os.listdir(out)):
            if f.endswith(".csv") and f not in ("rar7_controle.csv", "rar7_accuracy.csv"):
                data += [(f, r_) for r_ in rows(os.path.join(out, f))]
        check(sum(1 for f, r_ in data if r_["type"] != "kit") == nfiles, "rows standing for a file = files")
        check(any(r_["type"] == "kit" for f, r_ in data), "kit summary row present")
        check(all(r_["rar"] == "7" for f, r_ in data), "rar column = 7")
        keys = [r_["cle"] for f, r_ in data]
        check(len(keys) == len(set(keys)) and all(k.startswith("r7-p") for k in keys), "cle unique, r7-pN-file_id")
        p1 = [r_ for f, r_ in data if r_["pack"] == "p1"]
        p2 = [r_ for f, r_ in data if r_["pack"] == "p2"]
        check(p1 and all(r_["style"] == "techno" for r_ in p1), "Blood Rave rows forced to techno, inner Techno kept")
        check(p2 and all(r_["style"] == "dubstep" for r_ in p2), "Riddim Riot rows dubstep")
        text = "".join(open(os.path.join(out, f), encoding="utf-8").read() for f in os.listdir(out) if f.endswith(".csv"))
        check("Kick" not in text and "Blood" not in text and "BPM" not in text, "CSVs carry no file or pack name")
        check(open(os.path.join(out, "rar7_packs.txt")).read().startswith("p1\tGhosthack - Blood Rave"), "packs.txt maps p1")

        # --archive keeps the given file, a damaged archive is refused, outputs in the repo are refused
        r = run(["--rar", "7", "--archive", rar, "--out", out, "--plan"], env)
        check(r.returncode == 0 and os.path.isfile(rar), "--archive works and leaves the archive")
        bad = os.path.join(base, "bad.rar")
        blob = bytearray(open(rar, "rb").read())
        blob[len(blob) // 2] ^= 0xFF
        open(bad, "wb").write(bytes(blob))
        r = run(["--rar", "7", "--archive", bad, "--out", out], env)
        check(r.returncode == 2 and "unrar t failed" in r.stderr, "damaged archive refused by unrar t")
        check(os.listdir(work) == [], "work folder deleted after a refusal")
        notrar = os.path.join(base, "page.rar")
        open(notrar, "w").write("<html>quota</html>")
        r = run(["--rar", "7", "--url", "file://" + notrar, "--out", out], env)
        check(r.returncode == 2 and "not a RAR archive" in r.stderr, "an HTML page instead of the archive is refused")
        r = run(["--rar", "7", "--archive", rar, "--out", os.path.join(REPO, "docs")], env)
        check(r.returncode == 2 and "inside the repository" in r.stderr, "--out inside the repository refused")
        r = run(["--rar", "7", "--archive", rar, "--out", out], dict(os.environ, PASSE2_WORK=os.path.join(REPO, "docs")))
        check(r.returncode == 2 and "inside the repository" in r.stderr, "work folder inside the repository refused")
        check(not [p for p in os.listdir(os.path.join(REPO, "docs")) if p.startswith("passe2.")], "no work folder left in docs/")

        # Drive links become a direct download; a loose sound at the archive top is refused
        u = lance.drive_url("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOp/view?usp=sharing")
        check(u.endswith("id=1AbCdEfGhIjKlMnOp&export=download&confirm=t"), "Drive share link -> direct URL")
        check(lance.drive_url("https://drive.google.com/open?id=1AbCdEfGhIjKlMnOp").count("1AbCdEfGhIjKlMnOp") == 1,
              "Drive open?id link -> direct URL")
        loose = os.path.join(base, "loose")
        wav(os.path.join(loose, "kick.wav"), "kick", 0.2)
        try:
            lance.find_packs(loose)
            check(False, "loose sound at the top refused")
        except lance.Refused:
            check(True, "loose sound at the top refused")
    finally:
        shutil.rmtree(base, ignore_errors=True)
    print("%d failure(s)" % len(FAILS))
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
