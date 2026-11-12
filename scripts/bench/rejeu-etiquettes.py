#!/usr/bin/env python3
"""Rewrites a seat's gestures as a replay's gestures that aim by test tag (« touche <étiquette> »), never by pixels.

  scripts/bench/rejeu-etiquettes.py <seat folder> <n> <out folder> [--cible N=TAG] [--glisse N=TAG:DX:DY] [--saute N] [--commande N=CMD]

Each « touche / appui-long / double-touche X Y » of gestures 1..n takes the tag the seat's journal saw under the finger
(its « #i DOIGT <tag> « words » » line). Random ids are cut to a prefix (« project_p-896025b4 » -> « project_p-* », the first
node of that family, « stem_<40 hex>_drums » -> « stem_* »). A « glisse » (a scroll: the replay's tags are brought into view
by the tester) becomes « attends 1 » unless --glisse keeps it as « glisse TAG DX DY » (a swipe from the middle of a tag).
A touch whose finger saw no tag (« rien », « dans: », a refused touch, an Android view) must be given with --cible N=TAG
(« vue_<texte> » aims an Android view such as the bench's share menu). --saute N: gesture N becomes « attends 1 ». --commande N=retour: gesture N is that command (a button with no tag, such as a dialog's « OK »,
is closed with « retour »).
Writes cmd-1..n.txt in <out folder>. Exits 2, listing the gestures to give, when one is missing.
"""
import re
import sys

TOUCHES = ("touche", "appui-long", "double-touche")
TAG = re.compile(r"[A-Za-z_][A-Za-z0-9_:\-]*\*?")


def finger_tags(journal_text):
    """{gesture: (command, what was under the finger)} read from a seat's journal."""
    cmds, under = {}, {}
    for line in journal_text.splitlines():
        m = re.match(r"#(\d+) (.*)", line)
        if not m:
            continue
        i, text = int(m.group(1)), m.group(2)
        if text.startswith("> "):
            cmds[i] = text[2:].strip()
        elif text.startswith("DOIGT ") and i in cmds and i not in under:
            under[i] = text[6:]
        elif text.startswith("REFUS") and i in cmds and i not in under:
            under[i] = "REFUS"
    return cmds, under


def prefix_of_random_id(tag):
    """A tag with a random id (a project's « p-<8 hex> », a stem set's 24+ hex) cut to the prefix before it, plus « * »."""
    m = re.search(r"_(p-)[0-9a-f]{8}", tag)
    if m:
        return tag[:m.end(1)] + "*"
    m = re.search(r"_[0-9a-f]{20,}", tag)
    if m:
        return tag[:m.start() + 1] + "*"
    return tag


def tag_under(finger):
    m = re.match(r"(?:désactivé )?(\S+)(?: «.*)?$", finger or "")
    if not m or finger.startswith(("rien", "REFUS", "dans:", "vue ")) or m.group(1).startswith("#"):
        return None
    return prefix_of_random_id(m.group(1))


def convert(cmds, under, n, cible=None, glisse=None, saute=(), commande=None):
    cible, glisse, commande = cible or {}, glisse or {}, commande or {}
    out, missing = {}, []
    for i in range(1, n + 1):
        cmd = cmds.get(i)
        if cmd is None:
            missing.append((i, "no gesture in the journal"))
            continue
        verb = cmd.split()[0].lower()
        if i in commande:
            out[i] = commande[i]
        elif i in saute:
            out[i] = "attends 1"
        elif verb in TOUCHES:
            tag = cible.get(i) or tag_under(under.get(i))
            if tag is None or not TAG.fullmatch(tag.replace("vue_", "v", 1) if tag.startswith("vue_") else tag):
                missing.append((i, f"{cmd} (under the finger: {under.get(i, '?')})"))
            else:
                out[i] = f"{verb} {tag}"
        elif verb == "glisse":
            out[i] = "glisse %s %s %s" % tuple(glisse[i]) if i in glisse else "attends 1"
        else:
            out[i] = cmd
    return out, missing


def main(argv):
    if len(argv) < 4:
        print(__doc__)
        return 2
    seat, n, dest = argv[1], int(argv[2]), argv[3]
    cible, glisse, saute, commande = {}, {}, set(), {}
    rest = argv[4:]
    while rest:
        flag, val = rest[0], rest[1]
        rest = rest[2:]
        if flag == "--cible":
            k, v = val.split("=", 1)
            cible[int(k)] = v
        elif flag == "--glisse":
            k, v = val.split("=", 1)
            glisse[int(k)] = v.split(":")
        elif flag == "--commande":
            k, v = val.split("=", 1)
            commande[int(k)] = v
        elif flag == "--saute":
            saute.add(int(val))
        else:
            print(f"unknown option {flag}", file=sys.stderr)
            return 2
    with open(seat + "/journal.txt", encoding="utf-8") as f:
        cmds, under = finger_tags(f.read())
    out, missing = convert(cmds, under, n, cible, glisse, saute, commande)
    if missing:
        for i, why in missing:
            print(f"gesture {i}: {why}: give --cible {i}=<tag> (or --saute {i})", file=sys.stderr)
        return 2
    for i, c in out.items():
        with open(f"{dest}/cmd-{i}.txt", "w", encoding="utf-8") as f:
            f.write(c)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
