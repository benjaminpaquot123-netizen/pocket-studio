#!/usr/bin/env python3
"""Fuses the testers' reports of a lot or of a whole round (Audit ultra §3.4; method v2, META-AUDIT changes 4 and 9).

    fuse-reports.py <lot or round folder> [--base docs/audits/ultra/base.tsv] [--verdicts docs/audits/ultra/verdicts.tsv]
                    [--out <folder>/clusters.md] [--tour tour-N] [--maj-base]

Reads every report block (files rapport*.md / rapport*.txt under the folder; a seat is « X/s » when its report sits
in lotX/s, so a whole round fuses at once). Template v1 (« - ecran: | cible: ») and v2 (« - geste: N | ecran.vu: |
cible: ») are both read. Each defect gets a key:

    route | target | symptom family

- v2: route and target are the journal's own (#N ROUTE, #N DOIGT <tag>) at the gesture n° the tester cites, the
  same in every language; v1: the route at the screen n°, the target as the tester wrote it, normalised without
  losing non-Latin letters (NFKC + casefold: « 全部止める » stays itself, it was « ? » before).
- symptom families: « promesse » and « bloque » count as « rien », « coupe »/« texte-coupe »/« cache »/« hors-pouce » as
  « affichage » (one defect, five spellings in round 3).

States, from the base of known keys (base.tsv) and the verdicts (verdicts.tsv):
  [nouveau] never seen; [revient] the key was « corrigé »; [banc contredit] the key had a « banc » verdict and is seen
  again; otherwise the base's own state. [banc?] marks a cluster every tester of which said « banc?: <not non> », or
  whose tag is one of the bench's known limits.
Gravity of a cluster: the median of its defects (the worst in brackets); a v2 defect « revu: non » counts « moyen » at most.
A defect that judges the sound (attendu/obtenu only) is set aside, not the whole report.

Writes: clusters.md (60 lines, what an agent reads), clusters-complet.md (every cluster), promesses.tsv (one line per
promise line of every report), incidents-banc.tsv. --maj-base adds the new keys to the base with the round's name.
Standard library only.
"""
import os
import re
import sys
import unicodedata

GRAVITY = {"bloquant": 0, "mission-impossible": 0, "grave": 1, "moyen": 2, "mineur": 3, "finition": 3}
NAMES = {0: "bloquant", 1: "grave", 2: "moyen", 3: "mineur", 9: "?"}
SYMPTOMS = {"rien", "mauvais-ecran", "plantage", "incompris", "coupe", "lent", "perdu", "bloque", "sans-nom", "sans-aide",
            "promesse", "texte-faux", "texte-coupe", "cache", "hors-pouce"}
FAMILY = {"promesse": "rien", "bloque": "rien", "coupe": "affichage", "texte-coupe": "affichage", "cache": "affichage",
          "hors-pouce": "affichage"}
MAX_DEFECTS = 12
MAX_LINES = 60
# The bench's known limits (brief v2): a cluster on these tags is « banc? » until a test without the bench says otherwise.
BENCH_LIMIT_TAGS = re.compile(r"^(export|share_|video_|name_clear|tuner|analyser|midi_|import_)")
# Sound judged by ear, in « attendu »/« obtenu » only (« ça sonne » = a sound starts, which the brief asks to say).
SOUND = re.compile(r"\b(sonne(nt|rait)? (mal|faux|creux|sourd|mou)|son (est|était) (mauvais|faible|beau|moche|sale|sourd|mou)|"
                   r"mixage (mauvais|raté)|qualité du son|trop fort|pas assez fort|grésille|sature|le kick (est|manque))", re.I)


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
    """The report blocks of a file: each starts at a « siege: » line. A defect is a « - ecran: » or « - geste: » line
    WITH a « cible » field, in the « defauts » section (a hesitation that only says the word « cible » is not one)."""
    out, cur, section = [], None, ""
    for raw in text.splitlines():
        line = raw.rstrip()
        if line.strip().startswith("```"):
            continue
        if re.match(r"^\s*siege\s*:", line):
            cur = {"head": fields(line), "lines": [], "defects": [], "promises": [], "incidents": []}
            out.append(cur)
            section = ""
            continue
        if cur is None:
            continue
        cur["lines"].append(line)
        s = line.strip()
        m = re.match(r"^([a-z.\-]+)\s*:", s)
        if m and m.group(1) in ("hesitations", "defauts", "incidents-banc", "surprise", "abandon", "mission"):
            section = m.group(1)
            continue
        if re.match(r"^-?\s*promesse\s*:", s) and section != "defauts":
            cur["promises"].append(fields(s))
            continue
        if section == "incidents-banc" and s.startswith("-"):
            cur["incidents"].append(fields(s))
            continue
        if re.match(r"^-\s*(ecran|geste)\s*:", s):
            f = fields(s)
            if "cible" in f and section != "hesitations":
                cur["defects"].append(f)
            continue
        if cur["defects"] and section == "defauts" and re.match(r"^(attendu|symptome|gravite|banc\?|revu|promesse)\s*:", s):
            cur["defects"][-1].update(fields(s))
    return out


def norm(text):
    """A target as a key: NFKC, case folded, accents off Latin letters only, no quotes or punctuation, one space."""
    t = unicodedata.normalize("NFKC", text or "").casefold()
    t = "".join(c for c in unicodedata.normalize("NFD", t) if not unicodedata.combining(c))
    t = unicodedata.normalize("NFC", t)
    t = re.sub(r"[«»\"'`’.,;:!?()\[\]、。：！？「」]", " ", t)
    return re.sub(r"\s+", " ", t).strip() or "?"


def journal_line(journal, n, kind):
    m = re.search(rf"^#{n} {kind} ([^\n]+)", journal, re.M)
    return m.group(1).strip() if m else None


def route_of(journal, n):
    line = journal_line(journal, n, "ROUTE")
    return line.split("|")[0].strip() if line else "?"


def tag_of(journal, n):
    """The tag the finger touched at gesture n (« #N DOIGT [désactivé] <tag> « label » »)."""
    line = journal_line(journal, n, "DOIGT")
    if not line or line == "rien":
        return None
    words = [w for w in line.split() if w != "désactivé"]
    return words[0] if words and not words[0].startswith("«") else None


def seat_of(report_path, round_dir, head):
    rel = os.path.relpath(os.path.dirname(report_path), round_dir).split(os.sep)
    lot = next((p[3:] for p in rel if re.fullmatch(r"lot[A-Z]", p)), None)
    seat = head.get("siege", "?").strip().lower().split("/")[-1].strip()
    return f"{lot}/{seat}" if lot else seat


def journal_for(report_path, round_dir, seat):
    here = os.path.join(os.path.dirname(report_path), "journal.txt")
    if os.path.isfile(here):
        return read(here)
    return read(os.path.join(round_dir, seat.split("/")[-1], "journal.txt"))


def load_tsv(path):
    rows = [line.split("\t") for line in read(path).splitlines() if line.strip() and not line.startswith("#")]
    if rows and rows[0] and rows[0][0] in ("cle", "tour"):
        head = rows.pop(0)
        return [dict(zip(head, r)) for r in rows]
    return [dict(zip(["cle", "etat", "vu", "note"], r)) for r in rows]


def reports(round_dir):
    files = []
    for root, _, names in os.walk(round_dir):
        for n in sorted(names):
            if re.match(r"^rapport.*\.(md|txt)$", n, re.I):
                files.append(os.path.join(root, n))
    return sorted(files)


def gravity_of(d):
    said = d.get("gravite", "").strip().lower()
    g = GRAVITY.get(said.split(" ")[0] if said else "", None)
    if g is None and said.startswith("contourne"):
        n = re.search(r"\d+", said)
        g = 1 if n and int(n.group()) > 3 else 2
    if g is None:
        g = 9
    if d.get("revu", "").strip().lower().startswith("non"):
        g = max(g, 2)
    return g


def fuse(round_dir, base_path, verdicts_path=None):
    base = {r["cle"]: r.get("etat", "?") for r in load_tsv(base_path) if r.get("cle")}
    banc = {r["cle"] for r in load_tsv(verdicts_path or "") if r.get("cle") and r.get("verdict", "").startswith("banc")}
    clusters, rejected, seats, promises, incidents = {}, [], [], [], []
    for f in reports(round_dir):
        for b in blocks(read(f)):
            seat = seat_of(f, round_dir, b["head"])
            journal = journal_for(f, round_dir, seat)
            seats.append(seat)
            for p in b["promises"]:
                head = p.get("promesse", "")
                if head.startswith("id="):
                    p.setdefault("id", head[3:].strip())
                    head = p.get("texte", "")
                promises.append([seat, p.get("id", ""), head, p.get("ecran", ""),
                                 p.get("tenue", "").split(" ")[0], p.get("pourquoi", "")])
            for i in b["incidents"]:
                rest = [v for k, v in i.items() if k not in ("geste", "attente")]
                incidents.append([seat, i.get("geste", ""), i.get("attente", ""), rest[0] if rest else ""])
            for d in b["defects"][:MAX_DEFECTS]:
                said = f"{d.get('attendu', '').strip()} / {d.get('obtenu', '').strip()}".strip(" /")
                screen = re.sub(r"\D", "", d.get("ecran.vu", d.get("ecran", ""))) or "?"
                if SOUND.search(said):
                    rejected.append(f"{seat}#{screen} (jugement sonore)")
                    continue
                symptom = d.get("symptome", "?").strip().lower()
                symptom = FAMILY.get(symptom, symptom) if symptom in SYMPTOMS else "?" + symptom
                gesture = re.sub(r"\D", "", d.get("geste", "")) if "ecran.vu" in d else ""
                tag = tag_of(journal, gesture) if gesture else None
                route = route_of(journal, gesture if gesture else screen)
                key = f"{route}|{tag or norm(d.get('cible'))}|{symptom}"
                c = clusters.setdefault(key, {"key": key, "seats": set(), "where": [], "gravities": [], "blocks": False,
                                              "said": [], "bench": [], "tag": tag})
                c["seats"].add(seat)
                c["where"].append(f"{seat}#{gesture or screen}")
                c["gravities"].append(gravity_of(d))
                c["blocks"] = c["blocks"] or d.get("bloque", "").strip().lower() == "oui"
                c["bench"].append(d.get("banc?", "non").strip().lower() not in ("", "non"))
                if said and not c["said"]:
                    c["said"].append(said)
    for c in clusters.values():
        g = sorted(c["gravities"])
        c["gravity"] = g[(len(g) - 1) // 2]
        c["worst"] = g[0]
        c["new"] = c["key"] not in base
        state = base.get(c["key"], "nouveau")
        if c["key"] in banc or state == "banc":
            state = "banc contredit"
        elif state == "corrigé":
            state = "revient"
        c["state"] = state
        c["bench_doubt"] = all(c["bench"]) or bool(c["tag"] and BENCH_LIMIT_TAGS.match(c["tag"]))
    ordered = sorted(clusters.values(), key=lambda c: (c["bench_doubt"], c["gravity"], -len(c["seats"]), not c["new"], c["key"]))
    return ordered, rejected, sorted(set(seats)), promises, incidents


def line_of(c):
    route, target, symptom = c["key"].split("|", 2)
    worst = f" (pire : {NAMES.get(c['worst'], '?')})" if c["worst"] < c["gravity"] else ""
    doubt = " [banc?]" if c["bench_doubt"] else ""
    said = f" — {c['said'][0]}" if c["said"] else ""
    return (f"- [{c['state']}]{doubt} {NAMES.get(c['gravity'], '?')}{worst}{' bloque' if c['blocks'] else ''} ×{len(c['seats'])} "
            f"({', '.join(c['where'][:4])}) {route} · « {target} » · {symptom}{said}")


def render(round_dir, clusters, rejected, seats, limit=MAX_LINES):
    new = sum(1 for c in clusters if c["new"])
    confirmed = sum(1 for c in clusters if len(c["seats"]) >= 2)
    back = sum(1 for c in clusters if c["state"] in ("revient", "banc contredit"))
    head = [
        f"# Grappes du tour « {os.path.basename(os.path.normpath(round_dir))} »",
        f"{len(seats)} siège(s), {len(clusters)} grappe(s), {new} nouvelle(s), {confirmed} vue(s) par ≥ 2 sièges (confirmées sans juge), "
        f"{back} qui revien(nen)t." + (f" Écartés : {', '.join(rejected)}." if rejected else ""),
        "",
    ]
    if limit is None:
        return "\n".join(head + [line_of(c) for c in clusters]) + "\n"
    room = limit - len(head) - 1
    lines = [line_of(c)[:220] for c in clusters[:room]]
    if len(clusters) > room:
        lines.append(f"- … et {len(clusters) - room} grappe(s) de plus : voir clusters-complet.md.")
    return "\n".join(head + lines) + "\n"


def write_tsv(path, head, rows):
    with open(path, "w", encoding="utf-8") as f:
        f.write("\t".join(head) + "\n")
        for r in rows:
            f.write("\t".join(str(x).replace("\t", " ") for x in r) + "\n")


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return 2
    round_dir = argv[0]
    docs = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "audits", "ultra")

    def opt(name, default):
        return argv[argv.index(name) + 1] if name in argv else default
    base = opt("--base", os.path.join(docs, "base.tsv"))
    verdicts = opt("--verdicts", os.path.join(docs, "verdicts.tsv"))
    out = opt("--out", os.path.join(round_dir, "clusters.md"))
    tour = opt("--tour", os.path.basename(os.path.normpath(round_dir)))
    clusters, rejected, seats, promises, incidents = fuse(round_dir, base, verdicts)
    text = render(round_dir, clusters, rejected, seats)
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    folder = os.path.dirname(os.path.abspath(out))
    with open(os.path.join(folder, "clusters-complet.md"), "w", encoding="utf-8") as f:
        f.write(render(round_dir, clusters, rejected, seats, limit=None))
    write_tsv(os.path.join(folder, "promesses.tsv"), ["siege", "id", "promesse", "ecran", "tenue", "pourquoi"], promises)
    write_tsv(os.path.join(folder, "incidents-banc.tsv"), ["siege", "geste", "attente", "vu"], incidents)
    if "--maj-base" in argv:
        exists = os.path.isfile(base)
        with open(base, "a", encoding="utf-8") as f:
            if not exists:
                f.write("cle\tetat\tvu\tnote\n")
            for c in clusters:
                if c["new"]:
                    f.write(f"{c['key']}\tnouveau\t{tour}\t\n")
    print(text, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
