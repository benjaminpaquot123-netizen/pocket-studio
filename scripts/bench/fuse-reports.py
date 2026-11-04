#!/usr/bin/env python3
"""Fuses the testers' reports of a round (Audit ultra §3.4, « fusion des doublons, avant qu'un agent lise
quoi que ce soit »). Reads every GABARIT-RAPPORT-TESTEUR block of the round (files rapport*.md or
rapport*.txt in the round's folder and its seats' folders), keys each defect by
route (from the seat's journal at the screen's n°) + target + symptom, groups the same keys, marks
the ones absent from the base of known keys « nouveau », and writes clusters.md (60 lines at most:
the only thing an agent reads).

    fuse-reports.py <round folder> [--base docs/audits/ultra/base.tsv] [--out <round>/clusters.md] [--maj-base]

--maj-base adds the new keys to the base (state « nouveau », the round's name). A report that judges
the sound is rejected (the bench has no engine). Standard library only.
"""
import os
import re
import sys
import unicodedata

GRAVITY = {"bloquant": 0, "grave": 1, "moyen": 2, "mineur": 3}
SYMPTOMS = {"rien", "mauvais-ecran", "plantage", "incompris", "coupe", "lent", "perdu", "bloque", "sans-nom", "sans-aide"}
MAX_DEFECTS = 12
MAX_LINES = 60
# Sound judged by ear: rejected (§3.2, « aucun testeur IA ne juge le son »).
SOUND = re.compile(r"\b(sonne(nt|rait)? (mal|faux|bien|creux|fort)|son (est|était) (mauvais|faible|beau|moche|sale|fort)|"
                   r"mixage (mauvais|raté)|trop fort|pas assez fort|ça grésille|ça sature|le kick (est|manque))", re.I)


def read(path, default=""):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return default


def fields(line):
    """« a: x | b: y » -> {a: x, b: y} (the first « : » of each part)."""
    out = {}
    for part in line.split("|"):
        if ":" in part:
            k, v = part.split(":", 1)
            out[k.strip().lstrip("- ").strip().lower()] = v.strip()
    return out


def blocks(text):
    """The report blocks of a file: each starts at a « siege: » line."""
    out, cur = [], None
    for raw in text.splitlines():
        line = raw.rstrip()
        if line.strip().startswith("```"):
            continue
        if re.match(r"^\s*siege\s*:", line):
            cur = {"head": fields(line), "lines": [], "defects": []}
            out.append(cur)
            continue
        if cur is None:
            continue
        cur["lines"].append(line)
        s = line.strip()
        if re.match(r"^-\s*ecran\s*:", s) and "cible" in s:
            cur["defects"].append(fields(s))
        elif cur["defects"] and re.match(r"^(attendu|symptome|gravite)\s*:", s):
            cur["defects"][-1].update(fields(s))
    return out


def norm(text):
    """A target as a key: lower case, no accents, no quotes or punctuation, one space."""
    t = unicodedata.normalize("NFKD", text or "").encode("ascii", "ignore").decode().lower()
    t = re.sub(r"[«»\"'`’.,;:!?()\[\]]", " ", t)
    return re.sub(r"\s+", " ", t).strip() or "?"


def route_at(round_dir, seat, screen):
    j = read(os.path.join(round_dir, seat, "journal.txt"))
    m = re.search(rf"^#{screen} ROUTE ([^|\n]+)", j, re.M)
    return m.group(1).strip() if m else "?"


def load_base(path):
    base = {}
    for line in read(path).splitlines():
        if not line.strip() or line.startswith(("#", "cle\t")):
            continue
        parts = line.split("\t")
        base[parts[0]] = parts[1] if len(parts) > 1 else "?"
    return base


def reports(round_dir):
    files = []
    for root, _, names in os.walk(round_dir):
        for n in sorted(names):
            if re.match(r"^rapport.*\.(md|txt)$", n, re.I):
                files.append(os.path.join(root, n))
    return sorted(files)


def fuse(round_dir, base_path):
    base = load_base(base_path)
    clusters = {}
    rejected = []
    seats = []
    for f in reports(round_dir):
        for b in blocks(read(f)):
            seat = b["head"].get("siege", "?").strip().lower()
            if SOUND.search("\n".join(b["lines"])):
                rejected.append(f"{seat} (jugement sonore)")
                continue
            seats.append(seat)
            for d in b["defects"][:MAX_DEFECTS]:
                screen = re.sub(r"\D", "", d.get("ecran", "")) or "?"
                symptom = d.get("symptome", "?").strip().lower()
                if symptom not in SYMPTOMS:
                    symptom = "?" + symptom
                route = route_at(round_dir, seat, screen)
                key = f"{route}|{norm(d.get('cible'))}|{symptom}"
                c = clusters.setdefault(key, {"key": key, "seats": set(), "where": [], "gravity": 9, "blocks": False, "said": []})
                c["seats"].add(seat)
                c["where"].append(f"{seat}#{screen}")
                c["gravity"] = min(c["gravity"], GRAVITY.get(d.get("gravite", "").strip().lower(), 9))
                c["blocks"] = c["blocks"] or d.get("bloque", "").strip().lower() == "oui"
                said = f"{d.get('attendu', '').strip()} / {d.get('obtenu', '').strip()}".strip(" /")
                if said and len(c["said"]) < 1:
                    c["said"].append(said)
    for c in clusters.values():
        c["state"] = base.get(c["key"], "nouveau")
        c["new"] = c["key"] not in base
    return sorted(clusters.values(), key=lambda c: (c["gravity"], -len(c["seats"]), not c["new"], c["key"])), rejected, sorted(set(seats))


def render(round_dir, clusters, rejected, seats):
    names = {v: k for k, v in GRAVITY.items()}
    new = sum(1 for c in clusters if c["new"])
    confirmed = sum(1 for c in clusters if len(c["seats"]) >= 2)
    head = [
        f"# Grappes du tour « {os.path.basename(os.path.normpath(round_dir))} »",
        f"{len(seats)} siège(s), {len(clusters)} grappe(s), {new} nouvelle(s), {confirmed} vue(s) par ≥ 2 sièges (confirmées sans juge)."
        + (f" Rejetés : {', '.join(rejected)}." if rejected else ""),
        "",
    ]
    room = MAX_LINES - len(head) - 1
    lines = []
    for c in clusters[:room]:
        route, target, symptom = c["key"].split("|", 2)
        tag = "nouveau" if c["new"] else c["state"]
        g = names.get(c["gravity"], "?")
        said = f" — {c['said'][0]}" if c["said"] else ""
        line = f"- [{tag}] {g}{' bloque' if c['blocks'] else ''} ×{len(c['seats'])} ({', '.join(c['where'][:4])}) {route} · « {target} » · {symptom}{said}"
        lines.append(line[:220])
    if len(clusters) > room:
        lines.append(f"- … et {len(clusters) - room} grappe(s) de plus (les moins graves).")
    return "\n".join(head + lines) + "\n"


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return 2
    round_dir = argv[0]
    here = os.path.dirname(os.path.abspath(__file__))
    base = os.path.join(here, "..", "..", "docs", "audits", "ultra", "base.tsv")
    out = os.path.join(round_dir, "clusters.md")
    if "--base" in argv:
        base = argv[argv.index("--base") + 1]
    if "--out" in argv:
        out = argv[argv.index("--out") + 1]
    clusters, rejected, seats = fuse(round_dir, base)
    text = render(round_dir, clusters, rejected, seats)
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    if "--maj-base" in argv:
        name = os.path.basename(os.path.normpath(round_dir))
        exists = os.path.isfile(base)
        with open(base, "a", encoding="utf-8") as f:
            if not exists:
                f.write("cle\tetat\tvu\tnote\n")
            for c in clusters:
                if c["new"]:
                    f.write(f"{c['key']}\tnouveau\t{name}\t\n")
    print(text, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
