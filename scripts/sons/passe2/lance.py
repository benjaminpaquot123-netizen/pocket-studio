#!/usr/bin/env python3
"""Second (deep) pass over one RAR archive of bought sample packs.

Run it through lance.sh, which owns the work folder and always deletes it.

  lance.sh --rar 7 --url 'https://drive.google.com/file/d/.../view' --out ~/passe2-out
  lance.sh --rar 7 --url ... --out ... --plan      # list the packs and their styles, measure nothing

Steps: download (Google Drive link, outside the repository), `unrar t` (RARLAB
unrar; unar gave false errors in the first passes), `unrar x`, delete any
symbolic link the archive carried, find each pack folder, run tout.py ON THE
PACK FOLDER (never its parent: tout.py reads the pack folder's own name as the
last style hint), then merge the per-pack tables into anonymous CSVs with a
`rar` column, a `pack` column (p1, p2 ...) and the key `cle` = r<rar>-p<pack>-<file_id>.

A style is forced only on rows whose style came from the pack folder's name
(no folder or file name inside the pack names a style); see FORCES and
docs/sons/STYLES-PACKS.md.

Control (rar<N>_controle.csv and the exit code): for each pack, the rows that
stand for a file (every row except the `kit` summary rows) must equal the wav /
aiff / midi files tout.py found, and every row error is listed.
Exit 0 = all equal and 0 error; 3 = rows with an error (listed); 4 = count
mismatch or a pack tout.py could not measure; 2 = refused before measuring.
"""
import argparse
import csv
import os
import re
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SONS = os.path.dirname(HERE)
REPO = os.path.dirname(os.path.dirname(SONS))
TOUT = os.path.join(SONS, "tout.py")
sys.path.insert(0, SONS)  # our own scripts only; never the archive's folders (python -I)
import tout  # noqa: E402

# Pack folder name (regex, case-insensitive) -> style to force when tout.py's
# rule reads another style (or none) from that name. Source: docs/sons/STYLES-PACKS.md.
FORCES = [
    (r"blood\W*rave", "techno"),        # acid techno; 'Rave' in the name reads rave
    (r"consciousness", "techno"),      # melodic techno; no genre word unless the folder says so
    (r"cherry\W*blossom", "lofi"),     # chillhop; no genre word unless the folder says so
    (r"drift", "drift-phonk"),         # 'Drift Phonk' reads phonk, only 'Downtown Drift' reads drift-phonk
    (r"synthwave", "synthwave"),       # no synthwave style in tout.py: would stay unlabelled
]

MEDIA = tout.AUDIO_EXTS + tout.MIDI_EXTS
AUDIO_OR_ARCHIVE = re.compile(r"(?i)\.(wav|wave|aif|aiff|aifc|flac|mp3|ogg|m4a|mid|midi|rar|zip|7z|serumpack)$")
NESTED = re.compile(r"(?i)\.(zip|rar|7z|serumpack)$")
RAR_MAGIC = (b"Rar!\x1a\x07\x00", b"Rar!\x1a\x07\x01\x00")


class Refused(Exception):
    pass


def inside_repo(path):
    p = os.path.realpath(path)
    return p == REPO or p.startswith(REPO + os.sep)


def drive_url(url):
    """Direct download URL for a Google Drive share link; other URLs unchanged."""
    m = re.search(r"/file/d/([A-Za-z0-9_-]{10,})", url) or re.search(r"[?&]id=([A-Za-z0-9_-]{10,})", url)
    if m and "google." in url:
        return "https://drive.usercontent.google.com/download?id=%s&export=download&confirm=t" % m.group(1)
    return url


def download(url, dest):
    r = subprocess.run(["curl", "-L", "--fail", "-sS", "--retry", "3", "-o", dest, drive_url(url)],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise Refused("download failed (curl %d): %s" % (r.returncode, r.stderr.strip()[-200:]))
    check_rar(dest)


def check_rar(path):
    with open(path, "rb") as fh:
        head = fh.read(8)
    if not any(head.startswith(m) for m in RAR_MAGIC):
        raise Refused("not a RAR archive (Drive page instead of the file? link not shared, or quota): first bytes %r"
                      % head[:8])


def unrar(args):
    if not shutil.which("unrar"):
        raise Refused("RARLAB unrar not installed (apt-get install -y unrar); never unar")
    return subprocess.run(["unrar"] + args, capture_output=True, text=True)


def extract(archive, dest):
    t = unrar(["t", "-idq", archive])
    if t.returncode != 0:
        raise Refused("unrar t failed (rc %d): %s" % (t.returncode, (t.stdout + t.stderr).strip()[-300:]))
    x = unrar(["x", "-idq", "-o-", "-y", archive, dest + os.sep])
    if x.returncode != 0:
        raise Refused("unrar x failed (rc %d): %s" % (x.returncode, (x.stdout + x.stderr).strip()[-300:]))
    links = 0
    for root, dirs, files in os.walk(dest):
        for n in dirs + files:
            p = os.path.join(root, n)
            if os.path.islink(p):
                os.unlink(p)
                links += 1
    return links


def _visible(names):
    return [n for n in names if not n.startswith(".") and n != "__MACOSX"]


def has_media(folder):
    for root, dirs, files in os.walk(folder):
        dirs[:] = _visible(dirs)
        if any(f.lower().endswith(MEDIA) and not f.startswith(".") for f in files):
            return True
    return False


def find_packs(root):
    """Skip wrapper folders (one sub-folder, no sound of their own); then each sub-folder
    holding a sound is a pack, unless the folder itself holds loose sounds: then it is the pack."""
    top = root
    while True:
        names = _visible(sorted(os.listdir(top)))
        subs = [n for n in names if os.path.isdir(os.path.join(top, n))]
        loose = [n for n in names if os.path.isfile(os.path.join(top, n)) and n.lower().endswith(MEDIA)]
        if len(subs) == 1 and not loose:
            top = os.path.join(top, subs[0])
            continue
        break
    if loose:
        if top == root:
            raise Refused("sounds at the top of the archive, no pack folder: name it with --pack")
        return [top]
    packs = [os.path.join(top, s) for s in subs if has_media(os.path.join(top, s))]
    if not packs:
        raise Refused("no wav / aiff / midi file in the archive")
    return packs


def forced_style(name, cli):
    for pat, st in cli + FORCES:
        if re.search(pat, name, re.I):
            return st
    return None


def plan_of(packs, cli):
    plan = []
    for i, p in enumerate(packs, 1):
        audio, midi, skipped = tout._walk(p)
        name = os.path.basename(p)
        posed = tout.style_of("", pack=name)
        force = forced_style(name, cli)
        if force == posed:
            force = None
        nested = sum(1 for r, d, f in os.walk(p) for n in f if NESTED.search(n))
        plan.append({"pack": "p%d" % i, "path": p, "name": name, "audio": len(audio), "midi": len(midi),
                     "skipped": skipped, "posed": posed, "force": force, "nested": nested})
    return plan


def id_map(folder):
    """file_id -> relative path, for every file and every folder above a file (kit rows)."""
    out = {}
    audio, midi, _ = tout._walk(folder)
    for p in audio + midi:
        rel = os.path.relpath(p, folder).replace(os.sep, "/")
        parts = rel.split("/")
        for k in range(1, len(parts) + 1):
            r = "/".join(parts[:k])
            out.setdefault(tout._file_id(r), r)
    return out


def read_csv(path):
    with open(path, newline="", encoding="utf-8") as fh:
        rd = csv.DictReader(fh)
        return rd.fieldnames or [], list(rd)


def run_pack(pk, rar, tmp):
    prefix = os.path.join(tmp, pk["pack"])
    r = subprocess.run([sys.executable, "-I", TOUT, pk["path"], "--rar", str(rar), "--out", prefix],
                       capture_output=True, text=True, cwd=tmp)
    pk["tout_rc"] = r.returncode
    pk["tout_msg"] = (r.stdout + r.stderr).strip().splitlines()[-3:]
    tables = {}
    for f in sorted(os.listdir(tmp)):
        if f.startswith(pk["pack"] + "_") and f.endswith(".csv"):
            tables[f[len(pk["pack"]) + 1:-4]] = read_csv(os.path.join(tmp, f))
    return tables


def merge(plan, per_pack, rar):
    merged = {}
    for pk in plan:
        ids = id_map(pk["path"]) if pk["force"] else {}
        pk["rows"] = pk["kits"] = pk["errors"] = pk["forced_rows"] = pk["unresolved"] = 0
        pk["error_list"] = []
        for typ, (cols, rows) in per_pack[pk["pack"]].items():
            m = merged.setdefault(typ, {"cols": [], "rows": []})
            for c in ["rar", "pack"] + cols + ["cle"]:
                if c not in m["cols"]:
                    m["cols"].append(c)
            for row in rows:
                row["rar"] = str(rar)
                row["pack"] = pk["pack"]
                if typ != "accuracy":
                    row["cle"] = "r%d-%s-%s" % (rar, pk["pack"], row.get("file_id", ""))
                    if typ == "kit":
                        pk["kits"] += 1
                    else:
                        pk["rows"] += 1
                    if row.get("error"):
                        pk["errors"] += 1
                        pk["error_list"].append("%s %s: %s" % (row["cle"], typ, row["error"]))
                    if pk["force"] and row.get("style") in (pk["posed"], "unlabelled", ""):
                        rel = ids.get(row.get("file_id", ""))
                        if rel is None:
                            pk["unresolved"] += 1
                        elif tout.style_of(rel) == "unlabelled":
                            row["style"] = pk["force"]
                            pk["forced_rows"] += 1
                m["rows"].append(row)
    return merged


def write_out(out, rar, merged, plan):
    written = []
    for typ, m in sorted(merged.items()):
        path = os.path.join(out, "rar%d_%s.csv" % (rar, typ))
        with open(path, "w", newline="", encoding="utf-8") as fh:
            wr = csv.DictWriter(fh, fieldnames=m["cols"], extrasaction="ignore")
            wr.writeheader()
            for r in m["rows"]:
                wr.writerow(r)
        written.append(path)
    cols = ["rar", "pack", "files_audio", "files_midi", "rows", "kit_rows", "errors", "count_ok", "tout_rc",
            "style_posed", "style_forced", "rows_forced", "unresolved", "nested_archives", "skipped"]
    path = os.path.join(out, "rar%d_controle.csv" % rar)
    with open(path, "w", newline="", encoding="utf-8") as fh:
        wr = csv.writer(fh)
        wr.writerow(cols)
        for pk in plan:
            wr.writerow([rar, pk["pack"], pk["audio"], pk["midi"], pk["rows"], pk["kits"], pk["errors"],
                         "yes" if pk["count_ok"] else "NO", pk["tout_rc"], pk["posed"], pk["force"] or "",
                         pk["forced_rows"], pk["unresolved"], pk["nested"],
                         " ".join("%s:%d" % kv for kv in sorted(pk["skipped"].items()))])
    written.append(path)
    # pack names stay out of the tables; this list ties p1, p2 ... to the folders
    path = os.path.join(out, "rar%d_packs.txt" % rar)
    with open(path, "w", encoding="utf-8") as fh:
        for pk in plan:
            fh.write("%s\t%s\n" % (pk["pack"], pk["name"]))
    written.append(path)
    if any(pk["error_list"] for pk in plan):
        path = os.path.join(out, "rar%d_erreurs.txt" % rar)
        with open(path, "w", encoding="utf-8") as fh:
            for pk in plan:
                fh.write("".join(e + "\n" for e in pk["error_list"]))
        written.append(path)
    return written


def repo_audio():
    r = subprocess.run(["git", "-C", REPO, "status", "--porcelain", "--untracked-files=all", "--ignored"],
                       capture_output=True, text=True)
    return [ln[3:] for ln in r.stdout.splitlines() if AUDIO_OR_ARCHIVE.search(ln.strip().strip('"'))]


def print_plan(plan):
    for pk in plan:
        print("%s  %-55s audio %5d  midi %4d  style %-12s%s%s" % (
            pk["pack"], pk["name"][:55], pk["audio"], pk["midi"], pk["posed"],
            ("  -> forced " + pk["force"]) if pk["force"] else "",
            ("  nested archives %d" % pk["nested"]) if pk["nested"] else ""))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--rar", type=int, required=True, help="RAR number (1..99)")
    src = ap.add_mutually_exclusive_group(required=True)
    src.add_argument("--url", help="Google Drive share link (or any URL curl reads)")
    src.add_argument("--archive", help="RAR file already on disk (left in place)")
    ap.add_argument("--out", required=True, help="folder for the CSVs, outside the repository")
    ap.add_argument("--work", required=True, help="empty work folder outside the repository (lance.sh makes it)")
    ap.add_argument("--plan", action="store_true", help="list packs and styles, measure nothing")
    ap.add_argument("--pack", action="append", default=[], help="pack folder relative to the archive root (repeatable)")
    ap.add_argument("--force-style", action="append", default=[], metavar="REGEX=STYLE",
                    help="force STYLE on packs whose folder name matches REGEX (before the built-in table)")
    a = ap.parse_args(argv)
    created = []  # only what this run made is deleted
    before = set(repo_audio())
    try:
        if not 1 <= a.rar <= 99:
            raise Refused("--rar must be 1..99")
        for label, p in (("--out", a.out), ("--work", a.work)):
            if not os.path.isdir(p):
                raise Refused("%s: no such folder: %s" % (label, p))
            if inside_repo(p):
                raise Refused("%s is inside the repository: refused (no sound data in the repository)" % label)
        if os.listdir(a.work):
            raise Refused("--work must be empty")
        cli = []
        for f in a.force_style:
            if "=" not in f:
                raise Refused("--force-style wants REGEX=STYLE")
            pat, st = f.rsplit("=", 1)
            cli.append((pat, st.strip()))
        dl = os.path.join(a.work, "dl")
        x = os.path.join(a.work, "x")
        tmp = os.path.join(a.work, "csv")
        for d in (dl, x, tmp):
            os.mkdir(d)
            created.append(d)
        if a.url:
            archive = os.path.join(dl, "archive.rar")
            download(a.url, archive)
        else:
            archive = a.archive
            check_rar(archive)
        links = extract(archive, x)
        if a.url:
            os.remove(archive)  # the extraction is what we read; free the disk now
        if a.pack:
            packs = []
            for rel in a.pack:
                p = os.path.realpath(os.path.join(x, rel))
                if not p.startswith(os.path.realpath(x) + os.sep) or not os.path.isdir(p):
                    raise Refused("--pack %s: no such folder in the archive" % rel)
                packs.append(p)
        else:
            packs = find_packs(x)
        plan = plan_of(packs, cli)
        print("rar %d: %d pack(s), %d symbolic link(s) removed" % (a.rar, len(plan), links))
        print_plan(plan)
        if a.plan:
            return 0
        per_pack = {pk["pack"]: run_pack(pk, a.rar, tmp) for pk in plan}
        merged = merge(plan, per_pack, a.rar)
        for pk in plan:
            empty = pk["audio"] + pk["midi"] == 0
            pk["count_ok"] = pk["rows"] == pk["audio"] + pk["midi"] and (pk["tout_rc"] == 0 or (empty and pk["tout_rc"] == 1))
        written = write_out(a.out, a.rar, merged, plan)
    except Refused as e:
        print("refused: %s" % e, file=sys.stderr)
        return 2
    finally:
        for d in created:
            shutil.rmtree(d, ignore_errors=True)
    bad = [p for p in repo_audio() if p not in before]
    if bad:
        print("ALERT: sound or archive files in the repository: %s" % ", ".join(bad[:5]), file=sys.stderr)
        return 4
    files = sum(pk["audio"] + pk["midi"] for pk in plan)
    rows = sum(pk["rows"] for pk in plan)
    errors = sum(pk["errors"] for pk in plan)
    ok = all(pk["count_ok"] for pk in plan)
    for pk in plan:
        if not pk["count_ok"]:
            print("%s: files %d, rows %d, tout.py rc %d: %s" % (pk["pack"], pk["audio"] + pk["midi"], pk["rows"],
                                                               pk["tout_rc"], " / ".join(pk["tout_msg"])), file=sys.stderr)
    print("files read %d, rows %d, kit rows %d, errors %d, styles forced on %d rows, count %s" % (
        files, rows, sum(pk["kits"] for pk in plan), errors, sum(pk["forced_rows"] for pk in plan),
        "ok" if ok else "MISMATCH"))
    for p in written:
        print("wrote " + p)
    if not ok:
        return 4
    return 3 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
