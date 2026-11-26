#!/usr/bin/env python3
"""End-to-end test of the deep-pass launcher on a fake RAR5 archive of synthetic sounds.

  python3 -I scripts/sons/passe2/test_lance.py

Needs RARLAB unrar (apt-get install -y unrar). The archive is written by
fakerar.py (stored entries) and checked by `unrar t`, so the real unrar path
is exercised: download (file:// URL through curl), test, extract, packs,
tout.py per pack, merged CSVs, forced style, control, cleanup, repository guard.
"""
import contextlib
import csv
import io
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
    wav(os.path.join(a, "House Stabs", "BR Stab 01 C.wav"), "tone", 0.6)  # inner folder names a style: it wins
    midi(os.path.join(a, "MIDI", "BR Melody 01 120 BPM Am.mid"))
    k = os.path.join(a, "Construction Kits", "Kit 01 120 BPM Am")
    wav(os.path.join(k, "BR Kit 01 Drums 120 BPM Am.wav"), "loop", 4.0)
    wav(os.path.join(k, "BR Kit 01 Bass 120 BPM Am.wav"), "loop", 4.0)
    c = os.path.join(src, "Bundle", "Ghosthack - Cherry Blossom")  # forced lofi, no inner style folder
    wav(os.path.join(c, "One Shots", "CB Kick 01.wav"), "kick", 0.3)
    wav(os.path.join(c, "Loops", "CB Keys Loop 01 90 BPM C.wav"), "loop", 2.0)
    midi(os.path.join(c, "MIDI", "CB Chords 01 90 BPM C.mid"))
    b = os.path.join(src, "Bundle", "Ghosthack - Riddim Riot")
    wav(os.path.join(b, "Loops", "RR Drum Loop 01 120 BPM.wav"), "loop", 4.0)
    with open(os.path.join(b, "Preset 01.fxp"), "wb") as fh:
        fh.write(b"CcnK" + b"\0" * 60)
    os.makedirs(os.path.join(src, "Bundle", "Docs"))
    with open(os.path.join(src, "Bundle", "Docs", "readme.pdf"), "wb") as fh:
        fh.write(b"%PDF-1.4\n")
    return 11  # wav + midi files in the three packs (p1: 6 wav + 1 mid, p2: 2 wav + 1 mid, p3: 1 wav)


def pack_forms(base):
    """Point a: one pack alone at the archive root is one pack, named after its folder;
    a wrapper folder holding packs gives each pack; mixed layouts too."""
    names = lambda ps: [os.path.basename(p) for p in ps]
    one = os.path.join(base, "form-one")
    pk = os.path.join(one, "Ghosthack - Blood Rave")
    wav(os.path.join(pk, "Loops", "BR Loop 01 120 BPM.wav"), "loop", 1.0)
    midi(os.path.join(pk, "MIDI", "BR Melody 01.mid"))
    wav(os.path.join(pk, "One-Shots", "Kicks", "BR Kick 01.wav"), "kick", 0.2)
    check(names(lance.find_packs(one)) == ["Ghosthack - Blood Rave"], "single pack at the root = 1 pack, its own name")
    gen = os.path.join(base, "form-samples")
    pk = os.path.join(gen, "Ghosthack - Riddim Riot", "Samples")
    wav(os.path.join(pk, "Drum Loops", "RR Loop 01.wav"), "loop", 1.0)
    midi(os.path.join(pk, "MIDI", "RR Melody 01.mid"))
    check(names(lance.find_packs(gen)) == ["Ghosthack - Riddim Riot"], "pack/Samples/<categories> = 1 pack, pack name kept")
    wrap = os.path.join(base, "form-wrap", "Bundle")
    for n in ("Pack A", "Pack B"):
        wav(os.path.join(wrap, n, "Loops", "L 01.wav"), "loop", 1.0)
        wav(os.path.join(wrap, n, "One Shots", "K 01.wav"), "kick", 0.2)
    os.makedirs(os.path.join(wrap, "Docs"))
    check(names(lance.find_packs(os.path.dirname(wrap))) == ["Pack A", "Pack B"], "wrapper of two packs = 2 packs")
    mix = os.path.join(base, "form-mix")
    pk = os.path.join(mix, "Ghosthack - Riddim Riot")
    wav(os.path.join(pk, "Loops", "RR Loop 01.wav"), "loop", 1.0)
    midi(os.path.join(pk, "02. MIDI", "RR Melody 01.mid"))
    wav(os.path.join(pk, "Bonus Content", "RR Extra 01.wav"), "tone", 0.3)
    wav(os.path.join(pk, "RR Kit 01 140 BPM", "RR Kit 01 Drums.wav"), "loop", 1.0)
    check(names(lance.find_packs(mix)) == ["Ghosthack - Riddim Riot"], "pack with category and other folders = 1 pack")
    mix2 = os.path.join(base, "form-mix2")
    wav(os.path.join(mix2, "Pack A", "Loops", "L 01.wav"), "loop", 1.0)
    wav(os.path.join(mix2, "Pack B", "B 01.wav"), "tone", 0.3)
    check(names(lance.find_packs(mix2)) == ["Pack A", "Pack B"], "two folders at the root, one without categories = 2 packs")
    return one


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
        check("Riddim Riot" in r.stdout and "dubstep" in r.stdout, "--plan lists p3 dubstep")
        check("Cherry Blossom" in r.stdout and "forced lofi" in r.stdout, "--plan lists p2 forced lofi")
        check("Docs" not in r.stdout, "a folder without sounds is no pack")
        check(os.listdir(out) == [], "--plan writes nothing")
        check(os.listdir(work) == [], "--plan leaves no work folder")

        r = run(["--rar", "7", "--url", "file://" + rar, "--out", out], env)
        print(r.stdout.strip())
        check(r.returncode == 0, "full run exits 0 (rc %d, %s)" % (r.returncode, r.stderr.strip()[-300:]))
        check(os.listdir(work) == [], "download and extraction deleted")
        ctl = rows(os.path.join(out, "rar7_controle.csv"))
        check([c["pack"] for c in ctl] == ["p1", "p2", "p3"], "control lists the three packs")
        check(all(c["count_ok"] == "yes" and c["errors"] == "0" for c in ctl), "control: rows = files, 0 error")
        check(sum(int(c["files_audio"]) + int(c["files_midi"]) for c in ctl) == nfiles, "files read = %d" % nfiles)
        check(ctl[2]["skipped"] == ".fxp:1", "preset counted as skipped")
        check(all(c["pack_line_ok"] == "yes" for c in ctl) and ctl[0]["rows_pack_styled"] == ctl[0]["rows_forced"],
              "control: tout.py's pack-style count matches the tables")
        check(all(c["unresolved"] == "0" for c in ctl), "forced packs: every row found again (unresolved 0)")
        check(ctl[1]["style_forced"] == "lofi" and ctl[1]["rows_forced"] == ctl[1]["rows"] != "0",
              "Cherry Blossom: rows forced = rows of the pack (%s / %s)" % (ctl[1]["rows_forced"], ctl[1]["rows"]))
        check(int(ctl[0]["rows_forced"]) == int(ctl[0]["rows"]) - 1 + int(ctl[0]["kit_rows"]),
              "Blood Rave: every row forced but the House Stabs one (%s)" % ctl[0]["rows_forced"])
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
        p3 = [r_ for f, r_ in data if r_["pack"] == "p3"]
        check(sorted(r_["style"] for r_ in p1) == ["house"] + ["techno"] * (len(p1) - 1),
              "Blood Rave rows forced to techno, inner House Stabs folder wins")
        check(p2 and all(r_["style"] == "lofi" for r_ in p2), "Cherry Blossom rows forced to lofi")
        check(p3 and all(r_["style"] == "dubstep" for r_ in p3), "Riddim Riot rows dubstep")
        text = "".join(open(os.path.join(out, f), encoding="utf-8").read() for f in os.listdir(out) if f.endswith(".csv"))
        check("Kick" not in text and "Blood" not in text and "Cherry" not in text and "BPM" not in text, "CSVs carry no file or pack name")
        check(open(os.path.join(out, "rar7_packs.txt")).read().startswith("p1\tGhosthack - Blood Rave"), "packs.txt maps p1")

        # point c: tout.py's pack-style line is shown for every pack
        check("p1: style du pack : rave, posé sur" in r.stdout, "pack-style line shown for p1")
        check("p3: style du pack : dubstep, posé sur" in r.stdout, "pack-style line shown for p3")
        check("p2: style du pack : aucun" in r.stdout, "p2 (no style in its name) said too")
        # ... and a changed tout.py phrase fails the run instead of counting 0 forced rows
        fake = os.path.join(base, "tout_other_words.py")
        with open(fake, "w") as fh:
            fh.write("import contextlib, io, sys\nsys.path.insert(0, %r)\nimport tout\nb = io.StringIO()\n"
                     "with contextlib.redirect_stdout(b):\n    rc = tout.main()\n"
                     "print(b.getvalue().replace('style du pack', 'pack style'), end='')\nsys.exit(rc)\n"
                     % os.path.dirname(HERE))
        w2 = tempfile.mkdtemp(dir=work)
        old, lance.TOUT = lance.TOUT, fake
        err = io.StringIO()
        try:
            with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
                rc = lance.main(["--rar", "7", "--archive", rar, "--out", out, "--work", w2])
        finally:
            lance.TOUT = old
            shutil.rmtree(w2, ignore_errors=True)
        check(rc == 4 and "style du pack" in err.getvalue(),
              "missing pack-style line fails the run (rc %d, %s)" % (rc, err.getvalue().strip()[-200:]))

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

        # point a, end to end: an archive of one pack folder plans one pack under its own name
        one = pack_forms(base)
        rar1 = os.path.join(base, "one.rar")
        fakerar.main(["fakerar", rar1, one])
        r = run(["--rar", "7", "--archive", rar1, "--out", out, "--plan"], env)
        check(r.returncode == 0 and "1 pack(s)" in r.stdout and "Ghosthack - Blood Rave" in r.stdout
              and "rave" in r.stdout, "--plan on a one-pack RAR: 1 pack, named, style read (%s)" % r.stdout.strip()[-200:])

        # point d: without RARLAB unrar the launcher refuses before anything else (no download, no skipped test)
        nobin = os.path.join(base, "bin-without-unrar")
        os.mkdir(nobin)
        for tool in ("python3", "curl", "git", "mktemp", "rm", "dirname", "bash"):
            with open(os.path.join(nobin, tool), "w") as fh:
                # the system binaries, not a shell's own wrapper of the same name
                fh.write('#!/bin/sh\nexec %s "$@"\n' % (shutil.which(tool, path="/usr/bin:/bin") or shutil.which(tool)))
            os.chmod(os.path.join(nobin, tool), 0o755)
        r = subprocess.run([shutil.which("bash"), os.path.join(HERE, "lance.sh"), "--rar", "7", "--url",
                            "file://" + os.path.join(base, "nowhere.rar"), "--out", out],
                           capture_output=True, text=True, env=dict(env, PATH=nobin))
        check(r.returncode == 2 and "unrar not installed" in r.stderr and "download" not in r.stderr,
              "no unrar: refused first, clear message (rc %d, %s)" % (r.returncode, r.stderr.strip()[-200:]))
        check(os.listdir(work) == [], "no unrar: work folder deleted")

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
