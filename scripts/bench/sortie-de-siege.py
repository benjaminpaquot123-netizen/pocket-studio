#!/usr/bin/env python3
"""Seat exit detection (PLAN-RESTE 4.2): a tester (an AI agent) may write files outside its own seat folder.

  sortie-de-siege.py avant <lot> [--depot DIR]   # before the lot: lists the lot's files (<lot>/.avant-lot.txt) and
                                                 # the repository's changed files (<lot>/.avant-depot.txt)
  sortie-de-siege.py apres <lot> [--depot DIR]   # after: new files outside <lot>/<a-f>/ and outside the white list,
                                                 # and files of the repository new or changed outside the lot

Audit « banc crédible » 2, P7: the white list holds at the lot's root only (a pattern without « / » never matches a
file of a sub-folder: g/rapport.md is foreign), and the repository (--depot, this script's own by default) is watched:
« dépôt : <path> » for a file new or changed there, outside the lot. A seat writing in another seat's folder stays
invisible while the seats share one user (the seat folders are theirs).

Method v3 (docs/audits/AUDIT-ULTRA.md §13.1, §13.4, §13.9): the bench itself writes « code testé : <sha> » and
« joué par : <who> » as the first lines of each seat's rapport.md.

  sortie-de-siege.py avant <lot>     # also records, for each seat (a start.txt), the commit of the repository played
                                     # (HEAD of --depot) at THIS moment, the start, in <lot>/code-teste.tsv, and writes
                                     # the two lines in <seat>/rapport.md (created if absent)
  sortie-de-siege.py apres <lot>     # also writes the two lines again where the tester's own file lost them (his text
                                     # is kept, under them), then checks as `verifie`: exit 1 on a refused report
  sortie-de-siege.py entete <lot>    # only that writing (a report rewritten after the bench ended)
  sortie-de-siege.py verifie <lot>   # reads only: refuses (exit 1) a seat whose report is missing, has no « code testé »
                                     # in its heading, names another commit than the one recorded at the start, or
                                     # that the bench never recorded

« joué par » is the seat's start.txt key « joue.par= », else the environment's PS_JOUE_PAR, else « sous-agent neuf ».
A line the tester wrote himself is never rewritten: a « code testé » that names another commit is refused, not fixed.

Each foreign file is a line of <lot>/incidents-lot.tsv (at the lot's level: never pinned on a seat, nobody can tell
which one wrote it) and <lot>/lot.json gets « fichiers_hors_lot ». fuse-reports.py adds these lines to the round's
incidents-banc.tsv. Standard library only.
"""
import fnmatch
import hashlib
import json
import os
import re
import subprocess
import sys
import time

SEATS = set("abcdef")
WHITE = ["selftest-*.txt", "selftest*.log", "selftest.ok", "bench.log", "*.tsv", "*.md", "lot.json",
         ".avant-lot.txt", ".avant-depot.txt", "junit/*", "briefs/*"]


def files(lot):
    out = set()
    for root, _, names in os.walk(lot):
        for n in names:
            rel = os.path.relpath(os.path.join(root, n), lot).replace(os.sep, "/")
            out.add(rel)
    return out


def foreign(rel):
    top = rel.split("/", 1)[0]
    if "/" in rel and top in SEATS:
        return False
    return not any(fnmatch.fnmatch(rel, w) and ("/" in w or "/" not in rel) for w in WHITE)


def repo_state(depot, lot):
    """{path: sha256 of its content} of the repository's files that git reports new or changed, outside the lot."""
    try:
        out = subprocess.run(["git", "-C", depot, "status", "--porcelain", "--untracked-files=all", "-z"],
                             capture_output=True, timeout=300, check=True).stdout.decode("utf-8", "replace")
    except (subprocess.SubprocessError, OSError):
        return {}
    lot_rel = os.path.relpath(os.path.realpath(lot), os.path.realpath(depot)).replace(os.sep, "/") + "/"
    state = {}
    for entry in out.split("\0"):
        path = entry[3:]
        if len(entry) < 4 or (path + "/").startswith(lot_rel) or path.startswith(lot_rel):
            continue
        full = os.path.join(depot, path)
        if os.path.isfile(full):
            with open(full, "rb") as f:
                state[path] = hashlib.sha256(f.read()).hexdigest()
        else:
            state[path] = "-"
    return state


RECORD = "code-teste.tsv"
CODE_LINE = re.compile(r"^code testé\s*:\s*(\S*)\s*$")
WHO_LINE = re.compile(r"^joué par\s*:\s*(.*\S)?\s*$")
DEFAULT_WHO = "sous-agent neuf"


def seat_dirs(lot):
    """The lot's seats: its folders a to f that hold a start.txt."""
    return [s for s in sorted(SEATS) if os.path.isfile(os.path.join(lot, s, "start.txt"))]


def seat_name(lot, seat):
    return f"{os.path.basename(os.path.normpath(os.path.abspath(lot)))}/{seat}"


def played_by(lot, seat):
    """Who plays the seat: its start.txt (« joue.par= »), else PS_JOUE_PAR, else a fresh sub-agent."""
    try:
        with open(os.path.join(lot, seat, "start.txt"), encoding="utf-8") as f:
            for line in f:
                k, _, v = line.partition("=")
                if k.strip() == "joue.par" and v.strip():
                    return v.strip()
    except OSError:
        pass
    return os.environ.get("PS_JOUE_PAR", "").strip() or DEFAULT_WHO


def heading(text):
    """(« code testé » value or None, « joué par » value or None) of a report's heading: its lines before the first
    « siege: » block, the first eight at most."""
    code = who = None
    for line in text.splitlines()[:8]:
        if line.startswith("siege:"):
            break
        m = CODE_LINE.match(line)
        if m and code is None:
            code = m.group(1)
        m = WHO_LINE.match(line)
        if m and who is None:
            who = m.group(1) or ""
    return code, who


def record_start(lot, depot):
    """The start of the bench: the commit played and who plays, for each seat, kept in <lot>/code-teste.tsv."""
    try:
        sha = subprocess.run(["git", "-C", depot, "rev-parse", "--verify", "-q", "HEAD^{commit}"], capture_output=True,
                             text=True, timeout=60).stdout.strip()
    except (subprocess.SubprocessError, OSError):
        sha = ""
    if not re.fullmatch(r"[0-9a-f]{40,64}", sha):
        return  # not a repository: nothing the bench can vouch for (`verifie` then refuses the lot's reports)
    day = time.strftime("%Y-%m-%d %H:%M")
    with open(os.path.join(lot, RECORD), "w", encoding="utf-8") as f:
        f.write("siege\tcode\tjoue_par\tdepart\n")
        for seat in seat_dirs(lot):
            f.write(f"{seat}\t{sha}\t{played_by(lot, seat)}\t{day}\n")


def recorded(lot):
    """{seat: (commit, who)} as the bench recorded it at its start."""
    out = {}
    try:
        with open(os.path.join(lot, RECORD), encoding="utf-8") as f:
            for line in f:
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 3 and parts[0] in SEATS and re.fullmatch(r"[0-9a-f]{40,64}", parts[1]):
                    out[parts[0]] = (parts[1], parts[2])
    except OSError:
        pass
    return out


def write_headings(lot):
    """The two lines on top of each recorded seat's rapport.md, where they are missing; nothing else is touched."""
    for seat, (sha, who) in recorded(lot).items():
        path = os.path.join(lot, seat, "rapport.md")
        if not os.path.isdir(os.path.dirname(path)):
            continue
        text = ""
        if os.path.isfile(path):
            with open(path, encoding="utf-8") as f:
                text = f.read()
        code, by = heading(text)
        add = ([f"code testé : {sha}"] if code is None else []) + ([f"joué par : {who}"] if by is None else [])
        if add:
            with open(path, "w", encoding="utf-8") as f:
                f.write("\n".join(add) + "\n" + text)


def refused_reports(lot):
    """One line per seat whose report the bench cannot vouch for: « lotX/s : why »."""
    start = recorded(lot)
    out = []
    for seat in sorted(set(seat_dirs(lot)) | set(start)):
        name, path = seat_name(lot, seat), os.path.join(lot, seat, "rapport.md")
        if seat not in start:
            out.append(f"{name} : le banc n'a noté aucun commit au départ de ce siège ({RECORD}) : « code testé » ne se vérifie pas")
            continue
        sha = start[seat][0]
        if not os.path.isfile(path):
            out.append(f"{name} : pas de rapport.md (code testé attendu : {sha[:8]})")
            continue
        with open(path, encoding="utf-8") as f:
            code, _ = heading(f.read())
        if not code:
            out.append(f"{name} : pas de ligne « code testé : <sha> » en tête du rapport (le banc a noté {sha[:8]} au départ : "
                       f"scripts/bench/sortie-de-siege.py entete <lot> la remet)")
        elif not re.fullmatch(r"[0-9a-f]{7,64}", code) or not sha.startswith(code):
            out.append(f"{name} : le rapport dit « code testé : {code[:12]} », le banc est parti du commit {sha[:8]} : "
                       f"ce siège n'a pas jugé le code qu'il nomme")
    return out


def main(argv):
    depot = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    if len(argv) == 4 and argv[2] == "--depot":
        depot, argv = argv[3], argv[:2]
    if len(argv) != 2 or argv[0] not in ("avant", "apres", "entete", "verifie"):
        print(__doc__)
        return 2
    step, lot = argv
    if step in ("entete", "verifie"):
        if step == "entete":
            write_headings(lot)
        bad = refused_reports(lot)
        for line in bad:
            print(f"rapport refusé : {line}")
        if not bad:
            print("rapports : « code testé » en tête de chaque siège, celui du départ ("
                  + ", ".join(sorted({sha[:8] for sha, _ in recorded(lot).values()})) + ")")
        return 1 if bad else 0
    listing = os.path.join(lot, ".avant-lot.txt")
    repo_listing = os.path.join(lot, ".avant-depot.txt")
    if step == "avant":
        record_start(lot, depot)
        write_headings(lot)
        with open(listing, "w", encoding="utf-8") as f:
            f.write("\n".join(sorted(files(lot))) + "\n")
        with open(repo_listing, "w", encoding="utf-8") as f:
            json.dump(repo_state(depot, lot), f, ensure_ascii=False)
        return 0
    before = set(open(listing, encoding="utf-8").read().split()) if os.path.isfile(listing) else set()
    new = sorted(r for r in files(lot) - before if foreign(r))
    repo_before = json.load(open(repo_listing, encoding="utf-8")) if os.path.isfile(repo_listing) else {}
    new = sorted([f"dépôt : {p}" for p, h in repo_state(depot, lot).items() if repo_before.get(p) != h] + new)
    meta_path = os.path.join(lot, "lot.json")
    meta = json.load(open(meta_path, encoding="utf-8")) if os.path.isfile(meta_path) else {}
    meta["fichiers_hors_lot"] = new
    # Method v3: the two heading lines put back where a tester's file lost them, then the reports checked. A lot the
    # bench recorded nothing for (an old one) is left as it was.
    bad = []
    if os.path.isfile(os.path.join(lot, RECORD)):
        write_headings(lot)
        bad = refused_reports(lot)
        meta["rapports_refuses"] = bad
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    tsv = os.path.join(lot, "incidents-lot.tsv")
    with open(tsv, "w", encoding="utf-8") as f:
        f.write("siege\tgeste\tattente\tvu\n")
        for r in new:
            f.write(f"lot\t-\t-\tfichier écrit hors des sièges : {r}\n")
    print(f"sortie de siège : {len(new)} fichier(s) hors des sièges" + (" : " + ", ".join(new) if new else ""))
    for line in bad:
        print(f"rapport refusé : {line}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
