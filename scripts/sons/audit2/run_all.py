"""Play every replay b1 to b8 of the second tout.py audit, one summary line each.

usage: python3 -I scripts/sons/audit2/run_all.py [work_dir]
Each replay prints 'FAUX|ERREUR <replay> | <label> | ...' per problem and ends with
'<replay>: ok N faux N vide N erreur N' (lib.Check). Exit code 1 when a replay has a
fault not listed below, an erreur, no verdict line, a listed fault that no longer
shows (repaired: take it off the list), or when the repository is not clean after
the runs. Packs and tables go under work_dir (default: AUDIT2_WORK or a temp folder),
never in the repository.
"""
import os
import re
import subprocess
import sys
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
REPLAYS = ["b1_names", "b2_tags", "b3_cut", "b3b_falsecut", "b3c_fuzz",
           "b4_tempo", "b5_weird", "b6_misc", "b7_midi", "b8_bars"]

# Known tool defects a replay catches: expected red until the repair (no false green).
# Labels exactly as the replay prints them; docs/sons/REJEUX-OUTIL2.md explains each one.
EXPECTED_RED = {}

# Most blanks allowed today (honest "don't know"). A replay above its ceiling fails: a tool that
# stops estimating must not pass green by leaving every cell blank. Lower it when the tool improves.
# b6 21 = 4 untagged acc1 pads (bpm and bars fenced) + 1 untagged acc2 key_est (key not usable at
# 12/12) + 13 acc2 bars (no written tempo: "106" is a
# synth model) + 3 kit2 bars (written 100 bpm against a heard 80: blank, no longer 5 bars).
VIDE_MAX = {"b4_tempo": 92, "b6_misc": 21, "b8_bars": 98}


def play(name, work):
    env = dict(os.environ, AUDIT2_WORK=os.path.join(work, name))
    t = time.time()
    p = subprocess.run([sys.executable, "-I", os.path.join(HERE, name + ".py")], cwd=REPO, env=env,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")
    with open(os.path.join(work, name + ".log"), "w", encoding="utf-8") as fh:
        fh.write(p.stdout)
    return name, p.returncode, p.stdout, time.time() - t


def judge(name, code, out):
    """-> (failed, text)."""
    fails, n_lines = {}, 0
    for line in out.splitlines():
        m = re.match(r"(FAUX|ERREUR) %s \| (.*?) \| (.*)$" % re.escape(name), line)
        if m:
            n_lines += 1
            if m.group(2) not in fails or m.group(1) == "ERREUR":   # an erreur is never hidden
                fails[m.group(2)] = (m.group(1), m.group(3))
    m = re.search(r"^%s: (ok \d+ faux (\d+) vide (\d+) erreur (\d+))$" % re.escape(name), out, re.M)
    if not m:
        return True, "pas de verdict (code %s) : %s" % (code, out.strip().splitlines()[-1:] or "")
    counts = m.group(1)
    if int(m.group(2)) + int(m.group(4)) != n_lines:
        return True, "%s ; %d lignes FAUX/ERREUR lues" % (counts, n_lines)
    if int(m.group(3)) > VIDE_MAX.get(name, 0):
        return True, "%s ; trop de vides (plafond %d)" % (counts, VIDE_MAX.get(name, 0))
    want = set(EXPECTED_RED.get(name, []))
    new = sorted(k for k in fails if k not in want or fails[k][0] == "ERREUR")
    fixed = sorted(want - set(fails))
    if code != 0 and not fails:
        return True, "%s ; sortie %s sans faute nommée" % (counts, code)
    if new or fixed:
        txt = [counts]
        txt += ["NOUVEAU %s : %s %s" % (fails[k][0], k, fails[k][1]) for k in new]
        txt += ["réparé : retirer de la liste « %s »" % k for k in fixed]
        return True, " ; ".join(txt)
    if fails:
        return False, "%s ; rouge attendu (%d)" % (counts, len(fails))
    return False, counts


def main(argv):
    work = argv[0] if argv else (os.environ.get("AUDIT2_WORK") or os.path.join(tempfile.gettempdir(), "audit2_tout"))
    os.makedirs(work, exist_ok=True)
    bad = 0
    with ThreadPoolExecutor(4) as ex:
        for name, code, out, dt in ex.map(lambda n: play(n, work), REPLAYS):
            failed, txt = judge(name, code, out)
            bad += failed
            print("%-5s %-13s %4.0f s  %s" % ("ÉCHEC" if failed else "ok", name, dt, txt))
    st = subprocess.run(["git", "status", "--porcelain"], cwd=REPO, stdout=subprocess.PIPE, text=True).stdout
    if st.strip() or os.path.exists(os.path.join(REPO, "scripts", "sons", "pack")):
        bad += 1
        print("ÉCHEC dépôt modifié par les rejeux :\n" + st)
    print("rejeux : %d en échec sur %d (journaux dans %s)" % (bad, len(REPLAYS), work))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
