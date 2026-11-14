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
- when the journal gives no button (a dialogue, a navigation tag under the finger) and the card has an « element: »
  field (the testTag under the finger, written by newer benches), that element is the target instead of the free
  text (PLAN-RESTE 6.4: « dialogue:<tag>|<element>|<symptôme> »); a navigation tag as element is ignored.
- symptom families: « promesse » and « bloque » count as « rien », « coupe »/« texte-coupe »/« cache »/« hors-pouce » as
  « affichage » (one defect, five spellings in round 3).

States, from the base of known keys (base.tsv) and the verdicts (verdicts.tsv):
  [nouveau] never seen; [revient ?] the key, or the button of a « même bouton » (PLAN-RESTE 6.2), was « corrigé » at
  an earlier round (a candidate: a reader confirms it in
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
# PLAN-RESTE 4.7 (T5-Cc-41b, « 1 KB », 4 min for 4 tracks): an export's size and the time it took are the bench's (the
# phone's AAC encoder is a shadow that copies, the engine a stand-in; recette tel-04: 42 Mo for 2 min 26 on the phone).
# A card on an export that is about them is « banc », never the app's; what the file holds (its length in bars) is not.
# Review of « banc6a » (important): an export is said by the button's tag or the card's target, never by « fichier »
# alone; a size is a number with its unit or the size of the file/export; a time is « a pris / met … N s », never
# « en N s » alone. An export of a few KB or « vide » is no figure of the bench (its shadow codec writes 768 000
# bytes for 4 s): a real empty file, the app's, not even a doubt of its tag.
EXPORT_WORDS = re.compile(r"export|partag|\bm4a\b|\bwav\b|\bmp3\b|\bflac\b|\.zip", re.I)
EXPORT_SIZE = re.compile(r"\b\d+(?:[,.]\d+)?\s*(?:ko|kb|mo|mb|go|gb|octets?|bytes?)\b|"
                         r"\b(?:taille|poids)\s+(?:du|de\s+l'|de\s+l’|de\s+ce)\s*(?:fichier|export|son|morceau)|\bpèse\b", re.I)
EXPORT_TIME = re.compile(r"\b(?:a pris|prend|met|a mis|mis)\b[^.;]*\b\d+(?:[,.]\d+)?\s*(?:min|minutes?|s|sec|secondes?)\b|"
                         r"\btrop long\b|\blent(?:e|ement)?\b|\btemps de l'export\b", re.I)
EXPORT_AMOUNT = re.compile(r"(\d+(?:[ \u00a0\u202f]\d{3})*(?:[,.]\d+)?)\s*(ko|kb|mo|mb|go|gb|octets?|bytes?)\b", re.I)
EMPTY = re.compile(r"\bvides?\b|\b0\s*(?:octets?|bytes?|ko|kb)\b", re.I)
TINY_BYTES = 64 * 1024
EXPORT_FIGURE_MARK = "[banc : taille ou durée d'un export, non représentatives]"


def on_export(d, tag):
    return bool(tag and re.match(r"^(export|share_|my_export)", tag)) or bool(EXPORT_WORDS.search(d.get("cible", "")))


def export_figure(d, tag):
    """PLAN-RESTE 4.7: the card is about an export's size or the time it took (the bench's), not what the file holds."""
    text = " ".join(d.get(k, "") for k in ("attendu", "obtenu"))
    slow = d.get("symptome", "").strip().lower() == "lent"
    return on_export(d, tag) and not empty_export(d, tag) and bool(EXPORT_SIZE.search(text) or EXPORT_TIME.search(text) or slow)


def empty_export(d, tag):
    """An export of a few KB or « vide »: the app's (review of « banc6a »)."""
    said = d.get("obtenu", "")
    if not on_export(d, tag):
        return False
    if EMPTY.search(said):
        return True
    for number, unit in EXPORT_AMOUNT.findall(said):
        value = float(re.sub(r"[ \u00a0\u202f]", "", number).replace(",", "."))
        scale = {"k": 1024, "m": 1024 ** 2, "g": 1024 ** 3}.get(unit[0].lower(), 1)
        if value * scale < TINY_BYTES:
            return True
    return False


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
        # (Lot « constate »: « defauts-avant », a control seat's answers on the defects aimed at, holds no defect card.)
        if m and m.group(1) in ("hesitations", "defauts", "defauts-avant", "incidents-banc", "surprise", "abandon", "mission"):
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
            if "cible" in f and section not in ("hesitations", "defauts-avant"):
                cur["defects"].append(f)
            continue
        if cur["defects"] and section == "defauts" and re.match(r"^(attendu|symptome|gravite|banc\?|revu|promesse|element)\s*:", s):
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


def element_of(d):
    """The card's « element: » (a testTag) as a key's target: an identifier, never a navigation tag; else None."""
    e = d.get("element", "").strip()
    return e if re.fullmatch(r"[A-Za-z][\w.\-#:]*", e) and not NAV_TAGS.match(e) else None


def tag_of(journal, n):
    """The tag the finger touched at gesture n (« #N DOIGT [désactivé] <tag> « label » »), unless a navigation one."""
    line = journal_line(journal, n, "DOIGT")
    if not line or line == "rien":
        return None
    words = [w for w in line.split() if w != "désactivé"]
    tag = words[0] if words and not words[0].startswith("«") else None
    return None if tag is None or NAV_TAGS.match(tag) else tag


def control_of(rel):
    """Lot « constate »: a control seat's folder from the round's root (« controle/Ba/a », « controle/Ha »), or None."""
    return "/".join(rel[rel.index("controle"):]) if "controle" in rel and rel[-1] != "controle" else None


def seat_of(report_path, round_dir, head):
    rel = os.path.relpath(os.path.dirname(report_path), round_dir).split(os.sep)
    # (A control seat is named by its folder: its « siege: a » would mix it with another control's a, or a lot's.)
    if control_of(rel):
        return control_of(rel)
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


def declared_gravity(d):
    """The gravity as the tester wrote it: a v1 word (bloquant, grave, moyen, mineur) or a v2 consequence
    (mission-impossible = bloquant, contourne-en-N-gestes = grave if N > 3 else moyen, finition = mineur); 9 unknown."""
    said = d.get("gravite", "").strip().lower()
    g = GRAVITY.get(said.split(" ")[0] if said else "", None)
    if g is None and said.startswith("contourne"):
        n = re.search(r"\d+", said)
        g = 1 if n and int(n.group()) > 3 else 2
    return 9 if g is None else g


def gravity_of(d):
    """The gravity the fusion keeps: the declared one, « moyen » at most when the tester did not redo it (revu: non)."""
    g = declared_gravity(d)
    if d.get("revu", "").strip().lower().startswith("non"):
        g = max(g, 2)
    return g


def scale_of(d):
    """The card's template: v2 (a gesture n° and « ecran.vu », the consequence scale) or v1 (the tester's word)."""
    return "v2" if "ecran.vu" in d else "v1"


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
    # External audit: a « banc » verdict contradicts only at a LATER round (one given at this round is this round's).
    banc = {r["cle"] for r in load_tsv(verdicts_path or "") if r.get("cle") and r.get("verdict", "").startswith("banc")
            and earlier(r.get("tour", ""), tour)}
    seen_in = {r["cle"]: sorted((t for t in r.get("vu", "").split(",") if t.strip()), key=lambda t: (tour_no(t) is None, tour_no(t) or 0))
               for r in rows}
    fixed_at = {r["cle"]: r.get("corrige_au", "").strip() for r in rows}
    # A « revient ? » candidate is confirmed (or not) by a reader in revient.tsv: tour, cle, oui|non (review of session 2:
    # 6 of 7 automatic « revient » were other defects under the same button).
    confirmed = {(r.get("tour"), r.get("cle")): r.get("revient", "").strip() for r in load_tsv(os.path.join(os.path.dirname(os.path.abspath(base_path)), "revient.tsv"))
                 if r.get("cle")}
    # Banc fiable 2.4: the same real button reported at an earlier round under another route or symptom (round 5:
    # « padlive_stop_all » rien in padlive at round 1, incompris in padlive_pads_side at round 5): a candidate, not fused.
    button_seen, button_fixed = {}, set()
    for r in rows:
        parts = r["cle"].split("|")
        if len(parts) == 3 and re.fullmatch(r"[a-z][a-z0-9_]*_[a-z0-9_]+", parts[1]) and not NAV_TAGS.match(parts[1]) \
                and not GENERIC_BUTTONS.match(parts[1]):
            for t in seen_in.get(r["cle"], []):
                if earlier(t, tour):
                    button_seen.setdefault(parts[1], set()).add(t)
            # PLAN-RESTE 6.2: a button fixed at an earlier round (A d #21 of round 5, T1-22: the key changed, the
            # button did not): its « même bouton » is a « revient ? » candidate, for a reader to confirm or refuse.
            if r.get("etat") == "corrigé" and fixed_at.get(r["cle"]) and earlier(fixed_at[r["cle"]], tour):
                button_fixed.add(parts[1])
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
                gesture = re.sub(r"\D", "", d.get("geste", "")) if "ecran.vu" in d else ""
                cited = gesture or screen  # what the tester cited (v2: the gesture; v1: the screen)
                if SOUND.search(said):
                    rejected.append(f"{seat}#{cited} (jugement sonore)")
                    continue
                raw = d.get("symptome", "?").strip().lower()
                symptom = FAMILY.get(raw, raw) if raw in SYMPTOMS else "?" + raw
                if gesture:
                    tag, route = tag_of(journal, gesture), route_of(journal, gesture)
                elif screen.isdigit() and int(screen) > 1 and raw in GESTURE_SYMPTOMS:
                    # Template v1 (rounds 1-4), a gesture's symptom: the screen seen came from gesture screen - 1.
                    tag, route = tag_of(journal, str(int(screen) - 1)), route_of(journal, str(int(screen) - 1))
                else:
                    # v1, a state of the screen itself: its own route and the target as the tester copied it.
                    tag, route = None, route_of(journal, screen)
                # PLAN-RESTE 6.4: no button in the journal (a dialogue, a navigation tag): the card's element, if any.
                tag = tag or element_of(d)
                key = f"{route}|{tag or norm(d.get('cible'))}|{symptom}"
                c = clusters.setdefault(key, {"key": key, "seats": set(), "where": [], "gravities": [], "blocks": False,
                                              "said": [], "bench": [], "tag": tag, "declared": [], "scales": [], "export_figure": [], "export_empty": []})
                c["seats"].add(seat)
                c["where"].append(f"{seat}#{cited}")
                c["gravities"].append(gravity_of(d))
                c["declared"].append(declared_gravity(d))  # the tester's own scale, for the dashboard's curve
                c["scales"].append(scale_of(d))
                c["blocks"] = c["blocks"] or d.get("bloque", "").strip().lower() == "oui"
                c["bench"].append(d.get("banc?", "non").strip().lower() not in ("", "non"))
                c["export_figure"].append(export_figure(d, tag))
                c["export_empty"].append(empty_export(d, tag))
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
        same_button = state if state.startswith("même bouton") else None
        if same_button and c["tag"] in button_fixed:
            state = "revient ?"
        # A candidate confirmed by a reader (revient.tsv) is a comeback; refused, it is this round's own defect (or,
        # from a « même bouton », that same button again).
        if state.endswith(" ?"):
            said = confirmed.get((tour, c["key"]))
            state = state[:-2] if said == "oui" else ((same_button or "nouveau") if said == "non" else state)
        c["state"] = state
        # PLAN-RESTE 4.7: every card of it about an export's size or time taken: the bench's, not a doubt.
        c["bench_export"] = bool(c["export_figure"]) and all(c["export_figure"])
        # Review of « banc6a »: an empty export is the app's, the tag's doubt aside (the testers' own « banc? » kept).
        tag_doubt = bool(c["tag"] and BENCH_LIMIT_TAGS.match(c["tag"])) and not any(c["export_empty"])
        c["bench_doubt"] = c["bench_export"] or all(c["bench"]) or tag_doubt
    ordered = sorted(clusters.values(), key=lambda c: (c["bench_doubt"], c["gravity"], -len(c["seats"]), not c["new"], c["key"]))
    return ordered, rejected, sorted(set(seats)), promises, incidents


def line_of(c):
    route, target, symptom = c["key"].split("|", 2)
    worst = f" (pire : {NAMES.get(c['worst'], '?')})" if c["worst"] < c["gravity"] else ""
    doubt = f" {EXPORT_FIGURE_MARK}" if c.get("bench_export") else " [banc?]" if c["bench_doubt"] else ""
    said = f" — {c['said'][0]}" if c["said"] else ""
    return (f"- [{c['state']}]{doubt} {NAMES.get(c['gravity'], '?')}{worst}{' bloque' if c['blocks'] else ''} ×{len(c['seats'])} "
            f"({', '.join(c['where'][:4])}) {route} · « {target} » · {symptom}{said}")


def bucket_of(c):
    """PLAN-RESTE 6.4: the header's buckets, a partition of the clusters. A comeback (confirmed or a candidate) keeps
    its bucket whatever the bench's doubt (a reader must read it anyway); then the bench's doubts; then the states."""
    state = c["state"]
    if state in ("revient", "banc contredit"):
        return "revient"
    if state.endswith(" ?"):
        return "candidat"
    if c["bench_doubt"]:
        return "banc"
    if state == "nouveau":
        return "nouveau"
    if state.startswith(("déjà vu", "même bouton")):
        return state.split(" (")[0]
    return "autre"


def by_round(clusters, bucket):
    """« déjà vu (tour-1) » ×6, « (tour-3) » ×12 -> « tour-1 : 6, tour-3 : 12 » (round order)."""
    counts = {}
    for c in clusters:
        if bucket_of(c) == bucket:
            t = c["state"].split(" (", 1)[1].rstrip(")")
            counts[t] = counts.get(t, 0) + 1
    return ", ".join(f"{t} : {n}" for t, n in sorted(counts.items(), key=lambda kv: (tour_no(kv[0]) is None, tour_no(kv[0]) or 0, kv[0])))


def render(round_dir, clusters, rejected, seats, limit=MAX_LINES):
    buckets = {}
    for c in clusters:
        buckets[bucket_of(c)] = buckets.get(bucket_of(c), 0) + 1
    confirmed = sum(1 for c in clusters if len(c["seats"]) >= 2)
    others = {}
    for c in clusters:
        if bucket_of(c) == "autre":
            others[c["state"]] = others.get(c["state"], 0) + 1
    # Banc fiable 2.1 / PLAN-RESTE 6.4: each count apart (they used to overlap: a « même bouton » cluster was both
    # « nouvelle » and « déjà vue ») so that the header's numbers add up to the clusters.
    counts = (f"{buckets.get('nouveau', 0)} nouvelle(s), "
              f"{buckets.get('déjà vu', 0)} déjà vue(s)" + (f" ({by_round(clusters, 'déjà vu')})" if buckets.get("déjà vu") else "") + ", "
              f"{buckets.get('même bouton', 0)} même bouton" + (f" ({by_round(clusters, 'même bouton')})" if buckets.get("même bouton") else "") + ", "
              f"{buckets.get('revient', 0)} qui revien(nen)t (confirmé par un lecteur), "
              f"{buckets.get('candidat', 0)} candidat(s) « revient ? » à confirmer dans revient.tsv, "
              f"{buckets.get('banc', 0)} [banc?] (limites connues du banc, à trancher)"
              + (f", {buckets['autre']} autre(s) état(s) de la base ({', '.join(f'{k} : {v}' for k, v in sorted(others.items()))})" if buckets.get("autre") else ""))
    head = [
        f"# Grappes du tour « {os.path.basename(os.path.normpath(round_dir))} »",
        f"{len(seats)} siège(s), {len(clusters)} grappe(s) : {counts} ; {len(rejected)} fiche(s) rejetée(s) (jugement sonore) ; "
        f"{confirmed} vue(s) par ≥ 2 sièges (confirmées sans juge)." + (f" Écartés : {', '.join(rejected)}." if rejected else ""),
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
    # PLAN-RESTE 4.2: files written outside the seats (sortie-de-siege.py), at the lot's level.
    for root, _, names in os.walk(round_dir):
        if "incidents-lot.tsv" in names:
            lot = os.path.relpath(root, round_dir)
            for line in read(os.path.join(root, "incidents-lot.tsv")).splitlines()[1:]:
                cells = line.split("\t")
                if len(cells) >= 4:
                    incidents.append([f"{lot} (lot)", cells[1], cells[2], cells[3]])
    # Review of banc45: a state a seat inherited from the seat before it in its fork, put back by the bench before the
    # seat started (BenchPristineRule: « #0 BANC : état hérité remis … » in the seat's journal), is the bench's incident.
    for root, _, names in sorted(os.walk(round_dir)):
        if "journal.txt" in names:
            rel = os.path.relpath(root, round_dir).split(os.sep)
            lot = next((p[3:] for p in rel if re.fullmatch(r"lot[A-Z]", p)), None)
            name = control_of(rel) or (f"{lot}/{rel[-1]}" if lot else rel[-1])
            for m in re.finditer(r"^#(\d+) BANC : ([^\n]+)", read(os.path.join(root, "journal.txt")), re.M):
                incidents.append([name, m.group(1), "-", m.group(2)])
    write_tsv(os.path.join(folder, "incidents-banc.tsv"), ["siege", "geste", "attente", "vu"], incidents)
    if "--maj-base" in argv:
        update_base(base, clusters, tour)
    print(text, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
