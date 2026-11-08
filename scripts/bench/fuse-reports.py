#!/usr/bin/env python3
"""Fuses the testers' reports of a lot or of a whole round (Audit ultra §3.4; method v2, META-AUDIT changes 4 and 9).

    fuse-reports.py <lot or round folder> [--base docs/audits/ultra/base.tsv] [--verdicts docs/audits/ultra/verdicts.tsv]
                    [--out <folder>/clusters.md] [--tour tour-N] [--maj-base]

Reads every report block (files rapport*.md / rapport*.txt under the folder; a seat is « X/s » when its report sits
in lotX/s, so a whole round fuses at once). Template v1 (« - ecran: | cible: ») and v2 (« - geste: N | ecran.vu: |
cible: ») are both read. Each defect gets a key:

    route | target | symptom family

- v2: route and target are the journal's own (#N ROUTE, #N DOIGT <tag>) at the gesture n° the tester cites, the
  same in every language; v1: for a gesture's symptom (rien, incompris, mauvais-ecran, bloque, plantage) the route and
  button of gesture « screen − 1 », otherwise the route at the screen n°; the target as the tester wrote it, normalised without
  losing non-Latin letters (NFKC + casefold: « 全部止める » stays itself, it was « ? » before).
- symptom families: « promesse » and « bloque » count as « rien », « coupe »/« texte-coupe »/« cache »/« hors-pouce » as
  « affichage » (one defect, five spellings in round 3).

States, from the base of known keys (base.tsv) and the verdicts (verdicts.tsv):
  [nouveau] never seen; [revient ?] the key was « corrigé » at an earlier round (a candidate: a reader confirms it in
  revient.tsv, then [revient]); [banc contredit ?] the key had a « banc » verdict and is seen
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


# Banc fiable 2.1/2.4: a navigation tag says where the finger went, not what failed (round 5: 148 « wizard_next », 39
# « guide_bar », every tab): never a key's target; the target the tester copied is kept instead.
NAV_TAGS = re.compile(r"^(tab_|wizard_next$|wizard_back$|guide_bar$|rien$|vue$|bouton$|retour$)")


# Buttons that do a different thing at each step (the tutorial's « Je le fais », the help labels, « Continuer »):
# the same tag at two rounds says nothing about the same defect.
GENERIC_BUTTONS = re.compile(r"^(tuto_(do|show|listen|need|next)|help_labels_button|launch_continue|remix_back|.*_back)$")


def tag_of(journal, n):
    """The tag the finger touched at gesture n (« #N DOIGT [désactivé] <tag> « label » »), unless a navigation one."""
    line = journal_line(journal, n, "DOIGT")
    if not line or line == "rien":
        return None
    words = [w for w in line.split() if w != "désactivé"]
    tag = words[0] if words and not words[0].startswith("«") else None
    return None if tag is None or NAV_TAGS.match(tag) else tag


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


def tour_no(t):
    """« tour-12 » -> 12; any other name (a scratch folder) -> None, read as « after every numbered round »."""
    m = re.fullmatch(r"tour-(\d+)", (t or "").strip())
    return int(m.group(1)) if m else None


def earlier(t, tour):
    """Round t came strictly before round « tour » (numeric order: tour-10 after tour-2)."""
    a, b = tour_no(t), tour_no(tour)
    return a is not None and (b is None or a < b)


# Symptoms of a gesture (the finger did something and got the wrong answer): only these take the button of the gesture
# before a v1 card's screen; a state seen on the screen (cut text, no name, no help, a promise) is the screen's own
# (review of session 2: 310 of 489 v1 cards got an unrelated button).
GESTURE_SYMPTOMS = {"rien", "incompris", "mauvais-ecran", "bloque", "plantage"}


def fuse(round_dir, base_path, verdicts_path=None, tour=None):
    rows = [r for r in load_tsv(base_path) if r.get("cle")]
    base = {r["cle"]: r.get("etat", "?") for r in rows}
    tour = tour or os.path.basename(os.path.normpath(round_dir))
    seen_in = {r["cle"]: sorted((t for t in r.get("vu", "").split(",") if t.strip()), key=lambda t: (tour_no(t) is None, tour_no(t) or 0))
               for r in rows}
    fixed_at = {r["cle"]: r.get("corrige_au", "").strip() for r in rows}
    # A « revient ? » candidate is confirmed (or not) by a reader in revient.tsv: tour, cle, oui|non (review of session 2:
    # 6 of 7 automatic « revient » were other defects under the same button).
    confirmed = {(r.get("tour"), r.get("cle")): r.get("revient", "").strip() for r in load_tsv(os.path.join(os.path.dirname(os.path.abspath(base_path)), "revient.tsv"))
                 if r.get("cle")}
    # Banc fiable 2.4: the same real button reported at an earlier round under another route or symptom (round 5:
    # « padlive_stop_all » rien in padlive at round 1, incompris in padlive_pads_side at round 5): a candidate, not fused.
    button_seen = {}
    for r in rows:
        parts = r["cle"].split("|")
        if len(parts) == 3 and re.fullmatch(r"[a-z][a-z0-9_]*_[a-z0-9_]+", parts[1]) and not NAV_TAGS.match(parts[1]) \
                and not GENERIC_BUTTONS.match(parts[1]):
            for t in seen_in.get(r["cle"], []):
                if earlier(t, tour):
                    button_seen.setdefault(parts[1], set()).add(t)
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
            # Banc fiable 2.3: a promise said kept (« oui ») while a defect of the same report cites it: contradicted.
            cited = {d.get("promesse", "").strip() for d in b["defects"]} - {"", "-"}
            for row in promises:
                if row[0] == seat and len(row) == 6:
                    row.append("contredite" if row[4] == "oui" and row[1] in cited else "")
            for i in b["incidents"]:
                rest = [v for k, v in i.items() if k not in ("geste", "attente")]
                incidents.append([seat, i.get("geste", ""), i.get("attente", ""), rest[0] if rest else ""])
            for d in b["defects"][:MAX_DEFECTS]:
                said = f"{d.get('attendu', '').strip()} / {d.get('obtenu', '').strip()}".strip(" /")
                screen = re.sub(r"\D", "", d.get("ecran.vu", d.get("ecran", ""))) or "?"
                if SOUND.search(said):
                    rejected.append(f"{seat}#{screen} (jugement sonore)")
                    continue
                raw = d.get("symptome", "?").strip().lower()
                symptom = FAMILY.get(raw, raw) if raw in SYMPTOMS else "?" + raw
                gesture = re.sub(r"\D", "", d.get("geste", "")) if "ecran.vu" in d else ""
                cited = gesture or screen  # what the tester cited (v2: the gesture; v1: the screen)
                if gesture:
                    tag, route = tag_of(journal, gesture), route_of(journal, gesture)
                elif screen.isdigit() and int(screen) > 1 and raw in GESTURE_SYMPTOMS:
                    # Template v1 (rounds 1-4), a gesture's symptom: the screen seen came from gesture screen - 1.
                    tag, route = tag_of(journal, str(int(screen) - 1)), route_of(journal, str(int(screen) - 1))
                else:
                    # v1, a state of the screen itself: its own route and the target as the tester copied it.
                    tag, route = None, route_of(journal, screen)
                key = f"{route}|{tag or norm(d.get('cible'))}|{symptom}"
                c = clusters.setdefault(key, {"key": key, "seats": set(), "where": [], "gravities": [], "blocks": False,
                                              "said": [], "bench": [], "tag": tag})
                c["seats"].add(seat)
                c["where"].append(f"{seat}#{cited}")
                c["gravities"].append(gravity_of(d))
                c["blocks"] = c["blocks"] or d.get("bloque", "").strip().lower() == "oui"
                c["bench"].append(d.get("banc?", "non").strip().lower() not in ("", "non"))
                if said and not c["said"]:
                    c["said"].append(said)
    for c in clusters.values():
        g = sorted(c["gravities"])
        c["gravity"] = g[(len(g) - 1) // 2]
        c["worst"] = g[0]
        before = [t for t in seen_in.get(c["key"], []) if earlier(t, tour)]
        # New for THIS round: unknown, or never seen at an earlier round (the base may hold it after --maj-base).
        c["new"] = c["key"] not in base or not before
        state = base.get(c["key"], "nouveau")
        fixed_before = state in ("corrigé", "banc") and (not fixed_at.get(c["key"]) or earlier(fixed_at[c["key"]], tour))
        if c["key"] in banc or (state == "banc" and fixed_before):
            state = "banc contredit ?"
        elif state == "corrigé" and fixed_before:
            state = "revient ?"
        elif state in ("corrigé", "banc"):
            state = "nouveau"  # fixed at this round or later: this round's own defect
        elif state in ("nouveau", "confirmé") and before:
            # Banc fiable 2.1: seen at an earlier round and never fixed: said so, not « nouveau » again.
            state = f"déjà vu ({before[0]})"
        elif state == "nouveau" and c["tag"] and button_seen.get(c["tag"]):
            state = f"même bouton ({sorted(button_seen[c['tag']], key=tour_no)[0]})"
        # A candidate confirmed by a reader (revient.tsv) is a comeback; refused, it is this round's own defect.
        if state.endswith(" ?"):
            said = confirmed.get((tour, c["key"]))
            state = state[:-2] if said == "oui" else ("nouveau" if said == "non" else state)
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
    maybe = sum(1 for c in clusters if c["state"].endswith(" ?"))
    again = sum(1 for c in clusters if c["state"].startswith(("déjà vu", "même bouton")))
    head = [
        f"# Grappes du tour « {os.path.basename(os.path.normpath(round_dir))} »",
        f"{len(seats)} siège(s), {len(clusters)} grappe(s), {new} nouvelle(s), {confirmed} vue(s) par ≥ 2 sièges (confirmées sans juge), "
        f"{back} qui revien(nen)t (confirmé par un lecteur), {maybe} candidat(s) « revient ? » à confirmer dans revient.tsv, {again} déjà vue(s) à un tour d'avant (même clé ou même bouton)." + (f" Écartés : {', '.join(rejected)}." if rejected else ""),
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


def update_base(base, clusters, tour):
    """Banc fiable 2.1: new keys are added with the round; a known key gets the round added to its « vu » column
    (it was ignored before: the base never learned that a defect came back unfixed). Comments and order are kept."""
    lines = read(base).splitlines() if os.path.isfile(base) else ["cle\tetat\tvu\tnote"]
    index = {}
    for i, line in enumerate(lines):
        parts = line.split("\t")
        if parts and not line.startswith("#") and parts[0] not in ("cle", ""):
            index[parts[0]] = i
    for c in clusters:
        i = index.get(c["key"])
        if i is None:
            lines.append(f"{c['key']}\tnouveau\t{tour}\t")
            index[c["key"]] = len(lines) - 1
            continue
        parts = (lines[i].split("\t") + ["", "", "", ""])[:max(4, len(lines[i].split("\t")))]
        seen = [t for t in parts[2].split(",") if t]
        if tour not in seen:
            parts[2] = ",".join(seen + [tour])
            lines[i] = "\t".join(parts)
    with open(base, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


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
    clusters, rejected, seats, promises, incidents = fuse(round_dir, base, verdicts, tour)
    text = render(round_dir, clusters, rejected, seats)
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    folder = os.path.dirname(os.path.abspath(out))
    with open(os.path.join(folder, "clusters-complet.md"), "w", encoding="utf-8") as f:
        f.write(render(round_dir, clusters, rejected, seats, limit=None))
    write_tsv(os.path.join(folder, "promesses.tsv"), ["siege", "id", "promesse", "ecran", "tenue", "pourquoi", "contredite"], promises)
    write_tsv(os.path.join(folder, "incidents-banc.tsv"), ["siege", "geste", "attente", "vu"], incidents)
    if "--maj-base" in argv:
        update_base(base, clusters, tour)
    print(text, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
