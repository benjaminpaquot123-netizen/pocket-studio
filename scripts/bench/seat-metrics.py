#!/usr/bin/env python3
"""A seat's measures and its mission's verdict (Audit ultra §3.3 and §3.4), read from what the bench
left in the seat's folder: journal.txt, cmd-N.txt, sound-N.txt, images.txt, start.txt and etat/
(resume.json, the projects' files, the prefs, the shared files). « Réussie » is read in the state,
never said by the tester.

    seat-metrics.py <seat folder> [--mission NAME]     writes <seat folder>/mesures.json, prints it

The mission comes from start.txt (mission=) unless given. Standard library only.
"""
import glob
import html
import json
import os
import re
import sys

# The repository (scripts/bench/ is two levels down): the tutorials' courses, for the tuto oracles.
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TUTOS = os.path.join(REPO, "project-model", "src", "main", "resources", "tutos")

SILENT = "Son : rien ne joue."
WAITS = ("attends", "fin")


def read(path, default=""):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return default


def start_options(seat):
    out = {}
    for line in read(os.path.join(seat, "start.txt")).splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


class Journal:
    """The journal's lines by turn: routes, gestures, fingers, backs, helps, shares, stalls, the end."""

    LINE = re.compile(r"^#(\d+) (.*)$")

    def __init__(self, text):
        self.routes = {}      # turn -> route
        self.modes = {}       # turn -> simple|pro
        self.cmds = {}        # turn -> gesture
        self.fingers = {}     # turn -> what was under the finger
        self.backs = set()
        self.helps = []
        self.shares = []      # (turn, file)
        self.stalls = 0
        self.refused = 0
        self.layout = 0
        self.crash = False
        self.end = None
        self.first_screen = None
        # PLAN-RESTE 5.9 (control 10, a round's measure): the engine missing on the bench (a call into the native library
        # Robolectric has not, or the bench's own « moteur absent »), anywhere in the journal, stacks included.
        self.native = sum(1 for raw in text.splitlines() if "UnsatisfiedLinkError" in raw or "moteur absent" in raw)
        for raw in text.splitlines():
            m = self.LINE.match(raw)
            if not m:
                continue
            n, rest = int(m.group(1)), m.group(2)
            if rest.startswith("ROUTE "):
                parts = [p.strip() for p in rest[len("ROUTE "):].split("|")]
                self.routes[n] = parts[0]
                for p in parts[1:]:
                    if p.startswith("MODE "):
                        self.modes[n] = p[len("MODE "):]
            elif rest.startswith("> "):
                self.cmds[n] = rest[2:].strip()
            elif rest.startswith("DOIGT "):
                self.fingers[n] = rest[len("DOIGT "):]
            elif rest == "RETOUR":
                self.backs.add(n)
            elif rest.startswith("AIDE "):
                self.helps.append((n, rest[len("AIDE "):]))
            elif rest.startswith("PARTAGE "):
                self.shares.append((n, rest[len("PARTAGE "):]))
            elif rest.startswith("GEL "):
                self.stalls += 1
            elif rest.startswith("REFUS "):
                self.refused += 1
            elif rest.startswith("LAYOUT "):
                self.layout += 1
            elif rest.startswith("CRASH "):
                self.crash = True
            elif rest.startswith("FIN : "):
                self.end = rest[len("FIN : "):]
            elif rest.startswith("PREMIER ECRAN "):
                self.first_screen = first_screen(rest[len("PREMIER ECRAN "):])


def first_screen(text):
    """PLAN-RESTE 4.6: « reel_s=6.4 simule_s=1.5 jvm_s=48.2 travail=oui vide=non attente_s=4.9 » (HumanTester.begin): the
    first screen's real seconds since the app's launch, the app's own clock, the seconds since the JVM started (the bench's
    start-up, what a tester waited before screen 1), the bench's workers busy, the screen blank, the bench's wait for it.
    A blank first screen while the bench was at rest is not the bench's: « à trancher » (recette tel-20)."""
    f = dict(p.split("=", 1) for p in text.split() if "=" in p)
    def num(k):
        try:
            return float(f[k].replace(",", "."))
        except (KeyError, ValueError):
            return None
    busy, blank = f.get("travail") == "oui", f.get("vide") == "oui"
    return {"reel": num("reel_s"), "simule": num("simule_s"), "jvm": num("jvm_s"), "travail_de_fond": busy, "vide": blank,
            "attente_banc": num("attente_s"), "verdict": "à trancher" if blank and not busy else "-"}


def counted(cmd):
    return not cmd.startswith(WAITS)


def gestures_before(j, turn):
    """The gestures played before screen [turn] (the gestures 1..turn-1, « attends » not counted)."""
    return sum(1 for n, c in j.cmds.items() if n < turn and counted(c))


def sounds(seat, n):
    s = read(os.path.join(seat, f"sound-{n}.txt")).strip()
    first = s.splitlines()[0] if s else SILENT
    return first != SILENT and not first.startswith("⚠") or bool(re.search(r"joue une fois|joue en boucle|jouent en boucle", s))


def measures(seat):
    j = Journal(read(os.path.join(seat, "journal.txt")))
    screens = sorted(j.routes)
    gestures = sum(1 for c in j.cmds.values() if counted(c))
    images = len([l for l in read(os.path.join(seat, "images.txt")).split() if l.strip()])
    first_sound = next((gestures_before(j, n) for n in screens if sounds(seat, n)), None)
    first_share = next((gestures_before(j, n + 1) for n, _ in j.shares), None)
    empty = sum(1 for f in j.fingers.values() if f.startswith("rien"))
    disabled = sum(1 for f in j.fingers.values() if f.startswith("désactivé"))
    immediate = sum(1 for n in j.backs if n - 1 in j.routes and j.routes.get(n) != j.routes.get(n - 1))
    # Screens revisited: back on a route left earlier (not the one just before).
    revisited, seen, last = 0, set(), None
    for n in screens:
        r = j.routes[n]
        if r != last and r in seen:
            revisited += 1
        seen.add(r)
        last = r
    # A swipe not followed by a touch (the next gesture, waits aside), or the last gesture.
    order = [n for n in sorted(j.cmds) if counted(j.cmds[n])]
    scroll_only = 0
    for i, n in enumerate(order):
        if j.cmds[n].startswith("glisse"):
            nxt = j.cmds[order[i + 1]] if i + 1 < len(order) else ""
            if not nxt.startswith(("touche", "appui-long", "ecris")):
                scroll_only += 1
    abandon = j.end if j.end and (j.end.startswith("abandon") or j.end.startswith("rien ne change") or j.end == "plantage") else None
    return j, {
        "gestes": gestures,
        "images": images,
        "gestes_premier_son": first_sound,
        "gestes_premier_partage": first_share,
        "touchers_a_vide": empty,
        "touchers_desactives": disabled,
        "retours": len(j.backs),
        "retours_immediats": immediate,
        "ecrans": len(screens),
        "ecrans_revisites": revisited,
        "defilements_sans_toucher": scroll_only,
        "appels_aide": len(j.helps),
        "abandon": abandon,
        "gels": j.stalls,
        "moteur_absent": j.native,
        "refus_pouce": j.refused,
        "constats_layout": j.layout,
        "plantage": j.crash,
        "fin": j.end,
        "premier_ecran_s": j.first_screen,
    }


# --- The missions' oracles (§3.3): each reads the saved state, says reussie|partielle|non and why ---------

def _projects(state):
    return state.get("projects", [])


def song(state, j, m):
    full = [p for p in _projects(state) if p.get("parts", 0) >= 3 and p.get("seconds", 0) >= 120]
    shared = state.get("shared", [])
    if full and shared:
        return "reussie", f"« {full[0]['name']} » : {full[0]['parts']} parties, {full[0]['seconds']:.0f} s ; partagé : {shared[0]}"
    some = any(p.get("parts", 0) >= 1 for p in _projects(state))
    if full or shared or some:
        return "partielle", f"morceau ≥ 3 parties et ≥ 2 min : {'oui' if full else 'non'} ; fichier partagé : {'oui' if shared else 'non'}"
    return "non", "aucune partie dans aucun projet"


def remix(state, j, m):
    separated = state.get("stem_sets", 0) > 0 or any(p.get("remix_set") for p in _projects(state))
    voice = any(p.get("voice_pad") for p in _projects(state))
    if separated and voice:
        return "reussie", "chanson séparée et un pad « Voix »"
    if separated or voice:
        return "partielle", f"séparée : {'oui' if separated else 'non'} ; pad « Voix » : {'oui' if voice else 'non'}"
    return "non", "aucune chanson séparée"


def padlive(state, j, m):
    if m["gestes_premier_son"] is None:
        return "non", "aucun son"
    if m["gestes_premier_son"] <= 1:
        return "reussie", f"premier son en {m['gestes_premier_son']} geste(s)"
    return "partielle", f"premier son en {m['gestes_premier_son']} gestes (visé : 1)"


def challenge(state, j, m):
    stars = [s for p in _projects(state) for s in p.get("stars", [])]
    if any(s >= 1 for s in stars):
        return "reussie", f"étoiles : {max(stars)}"
    if stars:
        return "partielle", "défi joué, 0 étoile"
    # Round 1: a challenge opened (its screen or one of its buttons touched) but none ended with stars.
    if any("challenge" in r for r in j.routes.values()) or any("padlive_challenge" in f for f in j.fingers.values()):
        return "partielle", "défi ouvert, aucune étoile gardée"
    return "non", "aucun défi joué"


def _tuto_progress(seat):
    """Each project's tutorial (etat/prefs/tuto.xml, PrefsTutoStore): style, level, steps done."""
    xml = read(os.path.join(seat, "etat", "prefs", "tuto.xml"))
    out = []
    for raw in re.findall(r"<string name=\"[^\"]*\">(.*?)</string>", xml, re.S):
        try:
            p = json.loads(html.unescape(raw))
        except ValueError:
            continue
        if isinstance(p, dict) and p.get("style"):
            p.setdefault("level", "Facile")  # the store leaves the default level out
            out.append(p)
    return out


def _at_level(item, code):
    levels = item.get("levels")
    return not levels or code in levels


def _chapters_done(style, level, done):
    """(chapters of [style]'s course at [level] whose steps are all in [done], chapters at that level)."""
    try:
        with open(os.path.join(TUTOS, f"{style}.json"), encoding="utf-8") as f:
            course = json.load(f)
    except (OSError, ValueError):
        return None, None
    code = "p" if level == "Pro" else "f"
    chapters = [c for c in course.get("chapters", []) if _at_level(c, code)]
    full = 0
    for c in chapters:
        steps = [s["id"] for s in c.get("steps", []) if _at_level(s, code)]
        if steps and all(s in done for s in steps):
            full += 1
    return full, len(chapters)


TUTO_CHAPTERS = 3  # « 5 chapitres au plus » a seat: 3 whole chapters is the mission done (AUDIT-ULTRA §2)


def tuto(level):
    """Mission tuto-facile / tuto-pro: a course followed at [level], whole chapters done (read in the prefs)."""
    def oracle(state, j, m):
        progress = _tuto_progress(state.get("_seat", ""))
        if not progress:
            return "non", "aucun tuto commencé"
        best = None
        for p in progress:
            done = set(p.get("done", []))
            full, total = _chapters_done(p["style"], p["level"], done)
            key = (p.get("level") == level, full or 0, len(done))
            if best is None or key > best[0]:
                best = (key, p, full, total, len(done))
        (right, _, _), p, full, total, steps = best
        what = f"« {p['style']} » en {p['level']} : {steps} étape(s)" + (f", {full} chapitre(s) entier(s) sur {total}" if total else " (cours introuvable)")
        if not right:
            return "partielle", what + f" (niveau demandé : {level})"
        if full is not None and full >= min(TUTO_CHAPTERS, total or TUTO_CHAPTERS):
            return "reussie", what
        return ("partielle", what) if steps else ("non", what)
    return oracle


def _synth_sounds(state):
    """Each synth track of each project: (project, track, sound key, notes, edited or None when not known)."""
    out = []
    facts = [p for p in _projects(state) if "synth_tracks" in p]
    if facts:
        for p in facts:
            for t in p["synth_tracks"]:
                out.append((p.get("name"), t.get("name"), t.get("sound"), t.get("notes", 0), t.get("edited")))
        return out
    # States of round 1 (before SeatState wrote synth_tracks): read the projects' own files.
    for f in sorted(glob.glob(os.path.join(state.get("_seat", ""), "etat", "projects", "*", "project.json"))):
        try:
            with open(f, encoding="utf-8") as fh:
                d = json.load(fh)
        except (OSError, ValueError):
            continue
        tracks = []
        if d.get("synthEngine", 0) == 0 and d.get("synth"):
            tracks.append((d.get("synthTrackName") or "Synthé", d["synth"].get("values"), (d.get("synthNotes") or {}).get("notes", [])))
        for t in d.get("tracks") or []:
            if (t.get("engine") or 0) == 0 and t.get("synth"):
                tracks.append((t.get("name"), t["synth"].get("values"), (t.get("notes") or {}).get("notes", [])))
        for name, values, notes in tracks:
            key = ",".join(f"{v:.4f}" for v in values or [])
            out.append((d.get("name"), name, key, len(notes or []), None))
    return out


def synth(state, j, m):
    """Mission synthe: a sound made at the Synth, on two tracks with notes (AUDIT-ULTRA §3.3, missions musicales)."""
    sounds = _synth_sounds(state)
    playing = [s for s in sounds if s[3] > 0 and s[4] is not False]
    groups = {}
    for s in playing:
        groups.setdefault((s[0], s[2]), []).append(s)
    two = max(groups.values(), key=len, default=[])
    if len(two) >= 2:
        unknown = any(s[4] is None for s in two)
        return "reussie", f"« {two[0][0]} » : le même son sur {', '.join(str(s[1]) for s in two)}" + (" (son retouché : non vérifié)" if unknown else "")
    if playing or any(s[4] for s in sounds):
        return "partielle", f"un son sur une seule piste avec des notes ({len(playing)} piste(s) de synthé jouée(s))"
    return "non", "aucune piste de synthé avec des notes"


def _saved_changes(state):
    """What a seat left on the phone that a fresh project has not (the seat starts with no project at all): a part, a
    track, a take, a Pad Live star, notes on a synth (the main Synthé is not in « tracks »), a retouched synth sound,
    a Live pad recorded (live.clips are filled by the tester's own actions, unlike the Pad Live set), a remix set, a
    separated song. One line each."""
    out = []
    for p in _projects(state):
        name = p.get("name", "?")
        if p.get("audio_clips", 0):
            out.append(f"« {name} » : {p['audio_clips']} prise(s)")
        if p.get("parts", 0):
            out.append(f"« {name} » : {p['parts']} partie(s)")
        if p.get("tracks", 0):
            out.append(f"« {name} » : {p['tracks']} piste(s)")
        if p.get("stars"):
            out.append(f"« {name} » : {len(p['stars'])} défi(s) avec étoiles")
        if any(t.get("edited") for t in p.get("synth_tracks", [])):
            out.append(f"« {name} » : un son de synthé retouché")
        if sum(t.get("notes", 0) for t in p.get("synth_tracks", [])):
            out.append(f"« {name} » : {sum(t.get('notes', 0) for t in p['synth_tracks'])} note(s) au synthé")
        if p.get("live_pads", 0):
            out.append(f"« {name} » : {p['live_pads']} pad(s) Live enregistré(s)")
        if p.get("remix_set"):
            out.append(f"« {name} » : un remix")
    if state.get("stem_sets", 0):
        out.append(f"{state['stem_sets']} chanson(s) séparée(s)")
    return out


def free(state, j, m):
    """Mission libre: explore at will. PLAN-RESTE 6.4 (hardened; META-AUDIT-2 §E: round 5's A c was « réussie » with
    24 screens and no sound at all, E a with its take lost): besides an end by the tester on ≥ 3 screens without a
    crash, a stall or an abandon, « réussie » needs a sound heard (a sound-N.txt said something played) AND a trace
    on the phone: a saved change (_saved_changes, read in etat/resume.json) or a shared file. One of the two missing:
    « partielle », saying which; both missing: « non ». A seat whose state was not saved cannot prove a change: said so."""
    if m["plantage"]:
        return "non", "plantage"
    if m["abandon"]:
        return "non", m["abandon"]
    places = len(set(j.routes.values()))
    if m["gels"]:
        return "partielle", f"{m['gels']} gel(s) ; {places} écrans différents"
    if j.end != "fin" or places < 3:
        return "partielle", f"fin : {j.end or 'aucune'} ; {places} écrans différents"
    changes = _saved_changes(state)
    shared = list(state.get("shared") or []) or [f for _, f in j.shares]
    missing = []
    if m["gestes_premier_son"] is None:
        missing.append("aucun son entendu")
    if not changes and not shared:
        missing.append("aucun changement gardé ni fichier partagé" + (" (état du siège absent)" if "projects" not in state else ""))
    done = f"fini par le testeur, {places} écrans différents"
    if len(missing) == 2:
        return "non", f"{done}, mais {' ; '.join(missing)}"
    if missing:
        return "partielle", f"{done}, mais {missing[0]}"
    trace = changes[0] if changes else f"partagé : {shared[0]}"
    return "reussie", f"{done}, premier son en {m['gestes_premier_son']} geste(s) ; {trace}"


def voice(state, j, m):
    clips = sum(p.get("audio_clips", 0) for p in _projects(state))
    return ("reussie", f"{clips} prise(s) sur une piste audio") if clips else ("non", "aucune prise")


def exports(state, j, m):
    files = sorted(set(state.get("shared", [])))
    if len(files) >= 2:
        return "reussie", "fichiers : " + ", ".join(files) + " (cibles non mesurées ici : ffprobe à brancher)"
    if files:
        return "partielle", "un seul fichier : " + files[0]
    return "non", "aucun fichier"


def simple_only(state, j, m):
    modes = set(j.modes.values())
    if "pro" in modes or not state.get("prefs", {}).get("simpleMode", False):
        return "non", "passé en Pro"
    return "reussie", "toujours en Mode simple"


def resume_later(state, j, m):
    return "non", "oracle à écrire : le banc n'avance pas encore l'horloge de 3 jours"


MISSIONS = {
    # name: (what the tester is asked, its oracle)
    "premier-morceau": ("Premier morceau en Mode simple, jusqu'au fichier partagé (≤ 40 gestes)", song),
    "remix-voix": ("Remixer une chanson et découper la voix", remix),
    "padlive": ("Jouer au Pad Live (premier son en 1 toucher)", padlive),
    "defi": ("Refaire un défi du Pad Live", challenge),
    "voix": ("Enregistrer sa voix", voice),
    "exports": ("Exporter pour Spotify et pour le club", exports),
    "reprendre": ("Reprendre un projet 3 jours après", resume_later),
    "simple": ("Tout faire en Mode simple sans passer en Pro", simple_only),
    # Round 1 (lot banc): the missions of the round that had no oracle.
    "tuto-facile": ("Suivre un tuto en Facile (5 chapitres au plus)", tuto("Facile")),
    "tuto-pro": ("Suivre un tuto en Pro (5 chapitres au plus)", tuto("Pro")),
    "synthe": ("Faire un son de A à Z au Synthé et le poser sur deux pistes", synth),
    "libre": ("Explorer librement (explorateur)", free),
}
ALIASES = {"remix": "remix-voix", "song": "premier-morceau", "pad-live": "padlive", "synth": "synthe", "tuto": "tuto-facile"}


def verdict(seat, mission, j, m):
    state = {}
    raw = read(os.path.join(seat, "etat", "resume.json"))
    if raw:
        try:
            state = json.loads(raw)
        except ValueError:
            state = {}
    state["_seat"] = seat
    name = ALIASES.get(mission, mission)
    if name not in MISSIONS:
        return name, "non", f"mission inconnue ({', '.join(MISSIONS)})"
    result, why = MISSIONS[name][1](state, j, m)
    return name, result, why


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return 2
    seat = argv[0]
    mission = None
    if "--mission" in argv:
        mission = argv[argv.index("--mission") + 1]
    opts = start_options(seat)
    mission = mission or opts.get("mission")
    j, m = measures(seat)
    out = {"siege": os.path.basename(os.path.normpath(seat)), "options": opts, "mesures": m}
    if mission:
        name, result, why = verdict(seat, mission, j, m)
        out.update({"mission": name, "resultat": result, "pourquoi": why})
    else:
        out.update({"mission": None, "resultat": "non", "pourquoi": "pas de mission (mission= dans start.txt)"})
    with open(os.path.join(seat, "mesures.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
    print(json.dumps(out, ensure_ascii=False, indent=2))
    print(f"mission={out['resultat']}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
