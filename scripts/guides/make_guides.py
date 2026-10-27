#!/usr/bin/env python3
"""Writes the Guide's parcours (docs/PLAN-GUIDE.md §3.5 and §4.1): one file of cards per style of the assistant.

Usage: make_guides.py [<out dir>]    (default: the project-model's resources)
  python3 -I scripts/guides/make_guides.py
writes project-model/src/main/resources/guides/<style>.json (37 styles and generic.json, for a song made by hand)
and the English and Dutch wordings of every card in project-model/src/main/resources/i18n/{en,nl}/guide.json.
A test (GuideParcoursUpToDateTest) fails when the committed files are not what this script makes.

Every card is written from the style's recipe (scripts/packs/recipes/*.json, the same that make StyleRecipes.kt):
its drum lines, parts, sounds, the effects it puts (« fx »), its sweeps, riser, vocal, sub and second bass, its form,
and from the app's ready-made effects (FxPresets.kt: names and descriptions, already translated in the app's catalogs).
The order is the pro one (docs/guide/recherche.md §2): 0 the frame, 1 sounds and patterns (kick, snare, hats, bass,
chords, lead, voice/FX), 2 layers, 3 the effect chain per role, 4 the arrangement, 5 the movement, 6 levels and master,
7 export. Stages 1, 4, 6 and 7 make the Mode simple's subset.

Card fields (see GuideParcours.kt): id, stage, target (a role: kick, snare, hats, perc, drums, bass, sub, bass2, acid,
pad, lead, counter, vocal, riser, master, sends, song), kind (decide | review | check), simple, title, why,
simpleTitle, simpleWhy, when / howMuch / howOften (the track sheet's columns), applicable and done (detectors, all must
hold, "!" negates: see GuideAnalyzer), proposals and elsewhere (id, label, why, value, action), lexicon (a word of the
« Les mots de la musique » glossary). Every text is French, the key of its translation (the app's tr()).
"""
import importlib.util
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "project-model", "src", "main", "resources")
FORMAT = 1
PARCOURS_VERSION = 2


def _load_converter():
    spec = importlib.util.spec_from_file_location("recipes_to_kotlin", os.path.join(ROOT, "scripts", "packs", "recipes_to_kotlin.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


R2K = _load_converter()
NAMES = R2K.NAMES
ORDER = R2K.ORDER
CLUB_FAMILIES = R2K.CLUB_FAMILIES


def _catalog(lang):
    out = {}
    folder = os.path.join(RES, "i18n", lang)
    for name in sorted(os.listdir(folder)):
        if name.endswith(".json") and name != "guide.json":
            with open(os.path.join(folder, name), encoding="utf-8") as f:
                out.update(json.load(f))
    return out


CATALOG = {"en": _catalog("en"), "nl": _catalog("nl")}
LANGS = ("fr", "en", "nl")


def _presets():
    """The ready-made effects (FxPresets.kt): id -> (name, description), French."""
    import re
    with open(os.path.join(ROOT, "project-model", "src", "main", "kotlin", "com", "pocketstudio", "model", "FxPresets.kt"), encoding="utf-8") as f:
        text = f.read()
    return {m[0]: (m[1], m[2]) for m in re.findall(r'FxPreset\("([a-z0-9_]+)", "([^"]+)", "([^"]+)"', text)}


PRESETS = _presets()


class T:
    """A text in the three languages; {name} placeholders are filled per language by [fill]."""

    def __init__(self, fr, en, nl):
        self.v = {"fr": fr, "en": en, "nl": nl}

    def fill(self, **kw):
        out = T("", "", "")
        for lang in LANGS:
            vals = {k: (v.v[lang] if isinstance(v, T) else v) for k, v in kw.items()}
            out.v[lang] = self.v[lang].format(**vals)
        out.v["nl"] = nl_articles(out.v["nl"])
        return out


# Dutch: « refrein » is a het-word, « Drop » a de-word. The Dutch texts are written for « de Drop »;
# once {peak} is the chorus their article and determiners agree: het refrein, dit refrein, elk refrein.
NL_NEUTER = {"de": "het", "De": "Het", "deze": "dit", "Deze": "Dit", "elke": "elk", "Elke": "Elk"}
NL_REFREIN = re.compile(r"\b(de|De|deze|Deze|elke|Elke)((?: (?:\d+e|eerste|tweede|derde|laatste|hele|volle))?) ([Rr]efrein)\b")


def nl_articles(text):
    return NL_REFREIN.sub(lambda m: NL_NEUTER[m.group(1)] + m.group(2) + " " + m.group(3), text)


def known(fr):
    """A French text the app's catalogs already translate (a style's tip, a preset's name): the same words everywhere."""
    en, nl = CATALOG["en"].get(fr), CATALOG["nl"].get(fr)
    if not en or not nl:
        raise SystemExit(f"no translation in the app's catalogs for: {fr}")
    return T(fr, en, nl)


def num(value, lang):
    s = f"{value:g}"
    if lang in ("fr", "nl"):
        s = s.replace(".", ",")
    return s.replace("-", "−") if lang == "fr" else s


def lufs(value):
    return T(num(value, "fr"), num(value, "en"), num(value, "nl"))


# --- The texts, in the three languages -------------------------------------------------------------

K = T("Garder", "Keep", "Houden")
K_WHY = T("Ce qui est là sonne déjà juste : on n'y touche pas.", "What is there already sounds right: leave it.", "Wat er staat klinkt al goed: laat het zo.")
LATER_EAR = T("À l'oreille, plus tard", "By ear, later", "Op het gehoor, later")
LATER_EAR_WHY = T("Tu le régleras toi-même en écoutant.", "You will set it yourself while listening.", "Je stelt het zelf in terwijl je luistert.")


def keep(label=K, why=K_WHY, value=None):
    return {"id": "keep", "label": label, "why": why, "value": value, "action": {"type": "keep"}}


def prop(pid, label, why, action, value=None, simple_label=None, simple_why=None):
    """An option; [simple_label] / [simple_why]: its everyday words in the Mode simple (its [value] is not shown there then)."""
    return {"id": pid, "label": label, "why": why, "value": value, "action": action, "simpleLabel": simple_label, "simpleWhy": simple_why}


def open_spot(pid, label, why, spot, target="", simple_label=None, simple_why=None):
    a = {"type": "open", "spot": spot}
    if target:
        a["target"] = target
    return prop(pid, label, why, a, simple_label=simple_label, simple_why=simple_why)


def card(cid, stage, title, why, target="song", kind="decide", simple=False, simple_title=None, simple_why=None,
         applicable=(), done=(), proposals=(), elsewhere=(), lexicon="", when=None, how_much=None, how_often=None):
    return {
        "id": cid, "stage": stage, "target": target, "kind": kind, "simple": simple,
        "title": title, "why": why, "simpleTitle": simple_title, "simpleWhy": simple_why,
        "when": when, "howMuch": how_much, "howOften": how_often,
        "applicable": list(applicable), "done": list(done),
        "proposals": list(proposals), "elsewhere": list(elsewhere), "lexicon": lexicon,
    }


class Style:
    """What the cards need to know of a style's recipe."""

    def __init__(self, r):
        self.id = r["id"]
        self.name, self.family = NAMES[self.id]
        self.recipe = r
        self.bpm = int(r["bpm"]) if float(r["bpm"]).is_integer() else r["bpm"]
        self.drums = [d["role"] for d in r["drums"] if not d.get("off")]
        self.tips = {d["role"]: d.get("tip_fr", "") for d in r["drums"]}
        form = r.get("form") or ("club" if self.family in CLUB_FAMILIES else ("rap" if self.family == "Hip-hop" else "song"))
        self.club = form in ("club", "rave")
        self.build = bool(r.get("build"))
        self.chords = (r.get("chords") or {}).get("type", "none")
        self.lead = (r.get("lead") or {}).get("type", "none")
        lead_sound = (r.get("lead") or {}).get("sound", "")
        self.lead_is_bass = getattr(R2K, "SOUND_FAMILY", {}).get(lead_sound) == "Basses"
        bass = r.get("bass") or {}
        self.bass = bool(bass)
        self.sub = bool(bass.get("sub"))
        self.switch = bass.get("switch", "")
        sound = bass.get("sound", "")
        self.bass808 = "808" in sound or "808" in self.drums
        self.acid = bool((r.get("acid") or {}).get("use"))
        self.vocal = r.get("vocal")
        self.riser = bool(r.get("riser"))
        self.counter = bool(r.get("counter"))
        self.signature = r.get("signature")
        self.fx = r.get("fx", [])
        self.sweeps = [w["on"] for w in r.get("sweeps", [])]
        self.refs = r.get("references", [])
        self.bass_tip = r.get("bass_tip", "")
        self.pad_tip = r.get("pad_tip", "")
        self.lead_tip = r.get("lead_tip", "")
        self.lufs = LUFS_V2.get(self.id, -9.0)
        self.title = known(self.name) if self.id != "generic" else T("Ton morceau", "Your song", "Je nummer")

    def preset(self, on):
        for f in self.fx:
            if f["on"] == on and f.get("preset"):
                return f["preset"]
        return None

    def has_drum(self, *roles):
        return any(r in self.drums for r in roles)


# The loudness of each style's Drop (LoudnessTargets.BY_STYLE_V2: the assistant's songs since S5).
LUFS_V2 = {}
for s in ("techno", "hard_techno", "acid_techno", "industrial_techno", "acidcore"):
    LUFS_V2[s] = -7.0
LUFS_V2["gabber"] = -6.0
for s in ("house", "tech_house", "acid_house", "afro_house", "uk_garage", "speed_garage", "disco", "amapiano"):
    LUFS_V2[s] = -8.0
LUFS_V2["deep_house"] = -9.0
for s in ("dnb", "jump_up", "jungle", "neurofunk", "dubstep", "riddim"):
    LUFS_V2[s] = -7.0
for s in ("liquid_dnb", "trance", "synthwave", "retro_techno", "detroit", "minimal"):
    LUFS_V2[s] = -8.0
for s in ("trap", "drill", "phonk", "grime", "jersey_club", "reggaeton", "afrobeats", "pop"):
    LUFS_V2[s] = -8.5
LUFS_V2["boom_bap"] = -10.0
LUFS_V2["lofi"] = -12.0
for s in ("techno", "acid_techno", "industrial_techno", "acidcore"):
    LUFS_V2[s] = -7.5
for s in ("dnb", "jump_up", "jungle", "neurofunk", "dubstep", "riddim"):
    LUFS_V2[s] = -7.5
LUFS_V2["gabber"] = -7.0
for s in ("deep_house", "afro_house", "amapiano", "disco"):
    LUFS_V2[s] = -9.0
LUFS_V2["hard_techno"] = -6.0
LUFS_V2["phonk"] = -7.0
for s in ("trap", "drill"):
    LUFS_V2[s] = -8.5
for s in ("pop", "afrobeats", "reggaeton"):
    LUFS_V2[s] = -9.0

PEAK = {
    True: T("Drop", "Drop", "Drop"),
    False: T("refrain", "chorus", "refrein"),
}
PEAK_CAP = {True: T("Drop", "Drop", "Drop"), False: T("Refrain", "Chorus", "Refrein")}
# The values of a low cut on the PARAM_EQ's 4th knob (expMap 20..500 Hz): 120, 150, 200, 250, 400 Hz.
LOW_CUT = {120: 0.5566, 150: 0.626, 200: 0.7153, 250: 0.7847, 400: 0.9307}


def lowcut_insert(hz):
    return {"type": "insert", "effect": "PARAM_EQ", "params": [0.5, 0.5, 0.4, LOW_CUT[hz]]}


# --- Stage 0: the frame ----------------------------------------------------------------------------

def stage0(s):
    peak = PEAK[s.club]
    return [
        card(
            "frame.style", 0,
            T("Le style, le tempo et la tonalité", "The style, the tempo and the key", "De stijl, het tempo en de toonsoort"),
            T("{style} à {bpm} BPM : le style fixe les sons, la forme et la vitesse. Toutes les notes suivent la même gamme, rien ne sonne faux.",
              "{style} at {bpm} BPM: the style sets the sounds, the form and the speed. Every note follows the same scale, nothing sounds wrong.",
              "{style} op {bpm} BPM: de stijl bepaalt de klanken, de vorm en de snelheid. Alle noten volgen dezelfde toonladder, niets klinkt vals.").fill(style=s.title, bpm=str(s.bpm)),
            done=["style", "key"], lexicon="tempo",
            proposals=[keep(T("Garder {bpm} BPM", "Keep {bpm} BPM", "{bpm} BPM houden").fill(bpm=str(s.bpm))),
                       open_spot("tempo", T("Changer le tempo", "Change the tempo", "Het tempo wijzigen"),
                                 T("Deux ou trois BPM suffisent à changer l'énergie.", "Two or three BPM are enough to change the energy.", "Twee of drie BPM volstaan om de energie te veranderen."), "tempo"),
                       open_spot("key", T("Changer de tonalité", "Change the key", "Van toonsoort wisselen"),
                                 T("Toutes les pistes suivent, rien ne sonne faux.", "Every track follows, nothing sounds wrong.", "Alle sporen volgen, niets klinkt vals."), "key")],
        ),
        card(
            "frame.form", 0,
            T("La durée et la grille de 8 mesures", "The length and the 8-bar grid", "De lengte en het raster van 8 maten"),
            T("Tout change par 8 mesures (4, 8, 16, 32) : l'oreille sent venir chaque nouvelle partie. Sortir de la grille doit être voulu.",
              "Everything changes every 8 bars (4, 8, 16, 32): the ear feels each new part coming. Leaving the grid must be on purpose.",
              "Alles verandert per 8 maten (4, 8, 16, 32): het oor voelt elk nieuw deel aankomen. Van het raster afwijken moet bewust zijn."),
            done=["form"], lexicon="bar",
            proposals=[keep(), open_spot("parts", T("Voir les parties", "See the parts", "De delen bekijken"),
                                         T("Rallonger ou raccourcir une partie, toujours par 4 ou 8 mesures.", "Lengthen or shorten a part, always by 4 or 8 bars.", "Een deel langer of korter maken, altijd per 4 of 8 maten."), "song.parts")],
        ),
        card(
            "frame.destination", 0,
            T("Pour où : téléphone, streaming ou club ?", "Where for: phone, streaming or club?", "Waarvoor: telefoon, streaming of club?"),
            T("Le volume visé change : vers −14 LUFS en streaming, −8 en club ; ce style vise {lufs} LUFS au {peak}. Le master s'en approche sans écraser le son.",
              "The target loudness changes: about -14 LUFS for streaming, -8 in a club; this style aims at {lufs} LUFS in the {peak}. The master gets close to it without squashing the sound.",
              "Het beoogde volume verandert: rond -14 LUFS voor streaming, -8 in een club; deze stijl mikt op {lufs} LUFS in de {peak}. De master komt er dicht bij zonder de klank te pletten.").fill(lufs=lufs(s.lufs), peak=peak),
            done=["target"], lexicon="loudness",
            proposals=[prop("auto", T("Le style (auto)", "The style (auto)", "De stijl (auto)"),
                            T("La cible du style, mesurée sur son {peak}.", "The style's target, measured on its {peak}.", "Het doel van de stijl, gemeten op zijn {peak}.").fill(peak=peak),
                            {"type": "target", "name": "AUTO"}),
                       prop("streaming", T("Streaming et téléphone", "Streaming and phone", "Streaming en telefoon"),
                            T("−14 LUFS : là où les plateformes ramènent tout.", "-14 LUFS: where the platforms bring everything.", "-14 LUFS: waar de platformen alles naartoe brengen."),
                            {"type": "target", "name": "STREAMING"}, T("−14 LUFS", "-14 LUFS", "-14 LUFS")),
                       prop("club", T("Club", "Club", "Club"),
                            T("−8 LUFS : fort, pour les grosses enceintes.", "-8 LUFS: loud, for big speakers.", "-8 LUFS: luid, voor grote boxen."),
                            {"type": "target", "name": "CLUB"}, T("−8 LUFS", "-8 LUFS", "-8 LUFS"))],
        ),
    ]


# --- Stage 1: the sounds and the patterns, in the pro order ----------------------------------------

SOUND_KEEP = T("Garder ce son", "Keep this sound", "Deze klank houden")
SOUND_KEEP_WHY = T("Il tient son rôle : on passe au suivant.", "It does its job: on to the next one.", "Hij doet zijn werk: door naar de volgende.")
OTHER_SOUND = T("Autre idée de son", "Another sound idea", "Ander klankidee")
OTHER_SOUND_WHY = T("Un autre son du même rôle, écouté avec le reste.", "Another sound for the same role, heard with the rest.", "Een andere klank voor dezelfde rol, samen met de rest beluisterd.")
FAVORITE = T("Garder ce son dans mes favoris", "Keep this sound in my favourites", "Deze klank bij mijn favorieten bewaren")
FAVORITE_WHY = T("Pour le retrouver dans un autre morceau.", "To find it again in another song.", "Om hem in een ander nummer terug te vinden.")
WHERE = T("Où il joue", "Where it plays", "Waar hij speelt")
WHERE_WHY = T("Les parties où la piste joue : une case par partie.", "The parts the track plays in: one box per part.", "De delen waarin het spoor speelt: één vakje per deel.")


def sound_card(cid, target, title, why, simple_title, simple_why, lexicon, spot="sounds.track"):
    return card(
        cid, 1, title, why, target=target, simple=True, simple_title=simple_title, simple_why=simple_why,
        applicable=[f"has:{target}"], done=[f"has:{target}"], lexicon=lexicon,
        proposals=[keep(SOUND_KEEP, SOUND_KEEP_WHY), open_spot("other", OTHER_SOUND, OTHER_SOUND_WHY, spot, target),
                   open_spot("where", WHERE, WHERE_WHY, "song.parts", target)],
        elsewhere=[open_spot("favorite", FAVORITE, FAVORITE_WHY, "sounds.favorite", target)],
        when=T("Au début, piste par piste : le kick d'abord.", "At the start, track by track: the kick first.", "In het begin, spoor per spoor: de kick eerst."),
    )


def tip(s, role, fallback):
    t = s.tips.get(role, "")
    return known(t) if t else fallback


def stage1(s):
    out = [sound_card(
        "sound.kick", "kick",
        T("Le kick : son, motif, où il joue", "The kick: sound, pattern, where it plays", "De kick: klank, patroon, waar hij speelt"),
        tip(s, "kick", T("La fondation : le battement que tout le monde suit.", "The foundation: the beat everyone follows.", "Het fundament: de slag die iedereen volgt.")),
        T("Le kick (le « boum »)", "The kick (the “boom”)", "De kick (de ‘boem’)"),
        T("Le battement grave que tout le monde suit en tapant du pied.", "The low beat everyone taps their foot to.", "De lage slag waarop iedereen met de voet meetikt."),
        "kick", "rhythm.kit")]
    if s.has_drum("snare", "clap"):
        role = "snare" if s.has_drum("snare") else "clap"
        out.append(sound_card(
            "sound.snare", "snare",
            T("La caisse claire et le clap", "The snare and the clap", "De snaredrum en de clap"),
            tip(s, role, T("Elle répond au kick sur 2 et 4 : c'est elle qui donne l'élan.", "It answers the kick on 2 and 4: it gives the drive.", "Hij antwoordt de kick op 2 en 4: hij geeft de vaart.")),
            T("Le « tchak » qui répond au kick", "The “tchak” answering the kick", "De ‘tsjak’ die de kick antwoordt"),
            T("Sur les temps 2 et 4 : c'est lui qui fait avancer.", "On beats 2 and 4: it moves things forward.", "Op tel 2 en 4: hij duwt alles vooruit."),
            "snare", "rhythm.kit"))
    if s.has_drum("hat", "open_hat", "ride", "perc", "perc2", "tom"):
        role = next(r for r in ("hat", "open_hat", "ride", "perc", "perc2", "tom") if s.has_drum(r))
        out.append(sound_card(
            "sound.hats", "hats",
            T("Les charleys et les percussions", "The hats and the percussion", "De hi-hats en de percussie"),
            tip(s, role, T("Le « tic-tic » qui donne la vitesse et le groove.", "The “tick-tick” that gives speed and groove.", "Het ‘tik-tik’ dat snelheid en groove geeft.")),
            T("Les petits « tss » qui donnent la vitesse", "The little “tss” that give the speed", "De kleine ‘tss’ die de snelheid geven"),
            T("Ils remplissent entre les coups ; plus doux une fois sur deux, ils respirent.", "They fill between the hits; softer every other one, they breathe.", "Ze vullen tussen de slagen; om de andere zachter, dan ademen ze."),
            "hat", "rhythm.kit"))
    if s.has_drum("rumble"):
        out.append(sound_card(
            "sound.rumble", "rumble",
            T("Le grondement sous le kick", "The rumble under the kick", "Het gedreun onder de kick"),
            tip(s, "rumble", T("Un grondement grave qui roule entre les coups.", "A low rumble rolling between the hits.", "Een laag gedreun dat tussen de slagen rolt.")),
            T("Le grondement", "The rumble", "Het gedreun"),
            T("Un grave qui roule entre les kicks : la techno qui gronde.", "A low end rolling between the kicks: rumbling techno.", "Een laag dat tussen de kicks rolt: techno die gromt."),
            "kick", "rhythm.kit"))
    if s.bass:
        bass_title = T("La 808 : la basse qui suit le kick", "The 808: the bass that follows the kick", "De 808: de bas die de kick volgt") if s.bass808 else \
            T("La basse : son et motif", "The bass: sound and pattern", "De bas: klank en patroon")
        out.append(sound_card(
            "sound.bass", "bass", bass_title,
            known(s.bass_tip) if s.bass_tip else T("Elle joue la note de chaque accord dans le grave : le lien entre la batterie et l'harmonie.",
                                                   "It plays each chord's note in the low end: the link between the drums and the harmony.",
                                                   "Hij speelt de grondnoot van elk akkoord in het laag: de brug tussen drums en harmonie."),
            T("La basse", "The bass", "De bas"),
            T("Le son grave qui fait bouger le corps, sur les notes des accords.", "The low sound that moves the body, on the chords' notes.", "Het lage geluid dat het lijf laat bewegen, op de noten van de akkoorden."),
            "808" if s.bass808 else "bass"))
    if s.acid:
        out.append(sound_card(
            "sound.acid", "acid",
            T("La 303 : la ligne acide", "The 303: the acid line", "De 303: de acid-lijn"),
            T("La basse qui couine quand son filtre s'ouvre : la signature du style, elle bouge tout le morceau.",
              "The bass that squeals as its filter opens: the style's signature, it moves the whole song.",
              "De bas die piept als zijn filter opengaat: het handelsmerk van de stijl, hij beweegt het hele nummer."),
            T("La basse acide", "The acid bass", "De acid-bas"),
            T("Elle « parle » quand son filtre s'ouvre.", "It “talks” as its filter opens.", "Hij ‘praat’ als zijn filter opengaat."),
            "acid", "acid"))
    if s.chords != "none":
        title = {
            "pad": T("La nappe : les accords tenus", "The pad: the held chords", "De pad: de aangehouden akkoorden"),
            "keys": T("Le clavier : les accords", "The keys: the chords", "De toetsen: de akkoorden"),
            "stab": T("Les stabs : les accords en petits coups", "The stabs: the chords in short hits", "De stabs: de akkoorden in korte slagen"),
        }.get(s.chords, T("Les accords", "The chords", "De akkoorden"))
        out.append(sound_card(
            "sound.pad", "pad", title,
            known(s.pad_tip) if s.pad_tip else T("Les accords posent l'ambiance sous le reste.", "The chords set the mood under the rest.", "De akkoorden zetten de sfeer onder de rest."),
            T("Les accords", "The chords", "De akkoorden"),
            T("Ils posent l'ambiance, comme un tapis sous le morceau.", "They set the mood, like a carpet under the song.", "Ze zetten de sfeer, als een tapijt onder het nummer."),
            "pad"))
    if s.lead != "none":
        out.append(sound_card(
            "sound.lead", "lead",
            T("La 2e basse : la réponse", "The 2nd bass: the answer", "De 2e bas: het antwoord") if s.lead_is_bass else
            T("Le lead : le motif qu'on retient", "The lead: the motif you remember", "De lead: het motief dat je onthoudt"),
            known(s.lead_tip) if s.lead_tip else T("Un son plus aigu qui joue le bout qu'on retient.", "A higher sound playing the bit you remember.", "Een hogere klank die het stukje speelt dat je onthoudt."),
            T("La mélodie", "The melody", "De melodie"),
            T("Le bout qu'on retient et qu'on chante.", "The bit you remember and sing.", "Het stukje dat je onthoudt en meezingt."),
            "lead"))
    if s.counter:
        out.append(sound_card(
            "sound.counter", "counter",
            T("Le contre-chant : la réponse", "The counter-melody: the answer", "De tegenmelodie: het antwoord"),
            T("Une deuxième ligne qui répond au lead dans ses silences, au 2e {peak} surtout : le morceau grandit.",
              "A second line answering the lead in its rests, in the second {peak} above all: the song grows.",
              "Een tweede lijn die de lead in zijn stiltes antwoordt, vooral in de tweede {peak}: het nummer groeit.").fill(peak=PEAK[s.club]),
            T("La réponse", "The answer", "Het antwoord"),
            T("Une petite mélodie qui répond à la première.", "A little melody answering the first one.", "Een melodietje dat de eerste antwoordt."),
            "lead"))
    if s.vocal:
        why = {
            "shout": T("Un cri de MC au début de la 2e moitié du Drop. Tu peux y poser ta voix (Sons › Voix).",
                       "An MC's shout at the start of the Drop's second half. You can put your own voice there (Sounds › Voice).",
                       "Een MC-kreet aan het begin van de tweede helft van de Drop. Je kunt er je eigen stem op zetten (Klanken › Stem)."),
            "answer": T("Des bouts de voix qui répondent à la mélodie dans ses silences. Tu peux y poser ta voix (Sons › Voix).",
                        "Vocal chops answering the melody in its rests. You can put your own voice there (Sounds › Voice).",
                        "Stemflarden die de melodie in haar stiltes antwoorden. Je kunt er je eigen stem op zetten (Klanken › Stem)."),
            "stab": T("Un petit coup de voix toutes les 4 mesures. Tu peux y poser ta voix (Sons › Voix).",
                      "A short vocal stab every 4 bars. You can put your own voice there (Sounds › Voice).",
                      "Een korte stemstoot om de 4 maten. Je kunt er je eigen stem op zetten (Klanken › Stem)."),
            "crush": T("Une voix écrasée toutes les 2 mesures, à la Memphis. Tu peux y poser ta voix (Sons › Voix).",
                       "A crushed voice every 2 bars, Memphis style. You can put your own voice there (Sounds › Voice).",
                       "Een vermorzelde stem om de 2 maten, Memphis-stijl. Je kunt er je eigen stem op zetten (Klanken › Stem)."),
        }[s.vocal]
        c = sound_card(
            "sound.vocal", "vocal",
            T("La voix", "The voice", "De stem"), why,
            T("La voix", "The voice", "De stem"),
            T("Une voix robot en attendant la tienne.", "A robot voice until yours comes.", "Een robotstem tot de jouwe komt."),
            "", "record.voice")
        c["proposals"][2] = (open_spot("mine", T("Poser ma voix", "Record my voice", "Mijn stem opnemen"),
                                        T("Ta voix à la place du robot, sur les mêmes mesures.", "Your voice instead of the robot, on the same bars.", "Jouw stem in plaats van de robot, op dezelfde maten."), "record.voice", "vocal"))
        out.append(c)
    if s.riser:
        out.append(sound_card(
            "sound.riser", "riser",
            T("Le souffle qui monte", "The rising noise", "De opstijgende ruis"),
            T("Un souffle qui monte pendant la Montée et frappe au 1 du Drop : l'oreille sait que ça arrive.",
              "A noise rising through the build-up and hitting on the Drop's first beat: the ear knows it is coming.",
              "Een ruis die in de opbouw stijgt en op de eerste tel van de Drop inslaat: het oor weet dat het eraan komt.")
            if s.club else
            T("Un souffle qui frappe au début du refrain : l'oreille sait que ça arrive.",
              "A noise hitting at the start of the chorus: the ear knows it is coming.",
              "Een ruis die aan het begin van het refrein inslaat: het oor weet dat het eraan komt."),
            T("Le souffle avant le grand moment", "The whoosh before the big moment", "De ruis voor het grote moment"),
            T("Il annonce le grand moment.", "It announces the big moment.", "Het kondigt het grote moment aan."),
            "buildup"))
    return out


# --- Stage 2: layers, sparingly ---------------------------------------------------------------------

def stage2(s):
    out = []
    if s.sub and s.bass:
        out.append(card(
            "layers.bass", 2,
            T("La basse en deux couches : le sub et le médium", "The bass in two layers: the sub and the mid", "De bas in twee lagen: de sub en het midden"),
            T("Sous 120 Hz une seule source, propre et au centre (le sub) ; au-dessus, la couche qui grogne et bouge.",
              "Under 120 Hz one source only, clean and centred (the sub); above it, the layer that growls and moves.",
              "Onder 120 Hz maar één bron, zuiver en in het midden (de sub); daarboven de laag die gromt en beweegt."),
            target="bass", applicable=["has:bass"], done=["layered:bass"], lexicon="bass",
            proposals=[keep(), open_spot("layers", T("Voir les couches", "See the layers", "De lagen bekijken"),
                                         T("Chaque couche s'écoute seule, avec les autres, ou sans.", "Each layer can be heard alone, with the others, or without.", "Elke laag is apart te horen, met de andere, of zonder."), "sounds.layers", "bass")],
        ))
    elif s.bass808 and s.has_drum("kick"):
        out.append(card(
            "layers.808", 2,
            T("Un kick court par-dessus la 808", "A short kick on top of the 808", "Een korte kick bovenop de 808"),
            T("Sur un téléphone la 808 disparaît : le claquant du kick la fait entendre.",
              "On a phone the 808 disappears: the kick's click makes it heard.",
              "Op een telefoon verdwijnt de 808: de klik van de kick maakt hem hoorbaar."),
            target="bass", applicable=["has:bass", "has:kick"], done=["layered:808"], lexicon="808",
            proposals=[keep(), open_spot("kit", T("Choisir le kick", "Choose the kick", "De kick kiezen"),
                                         T("Un kick court, qui claque, sans grave à lui.", "A short, clicking kick, without low end of its own.", "Een korte kick die klikt, zonder eigen laag."), "rhythm.kit", "kick")],
        ))
    elif s.has_drum("snare") and s.has_drum("clap"):
        out.append(card(
            "layers.snare", 2,
            T("La caisse claire doublée d'un clap", "The snare doubled by a clap", "De snaredrum verdubbeld met een clap"),
            T("Ensemble, un « clac » plus épais et plus large. Jamais deux sons pour le même rôle dans la même bande.",
              "Together, a thicker and wider “clack”. Never two sounds for the same role in the same band.",
              "Samen een dikkere en bredere ‘klak’. Nooit twee klanken voor dezelfde rol in dezelfde band."),
            target="snare", applicable=["has:snare"], done=["layered:snare"], lexicon="clap",
            proposals=[keep(), open_spot("kit", T("Voir le clap", "See the clap", "De clap bekijken"),
                                         T("Le clap pile sur la caisse claire, un peu plus bas.", "The clap right on the snare, a little lower.", "De clap precies op de snare, iets zachter."), "rhythm.kit", "snare")],
        ))
    return out


# --- Stage 3: the effect chain of each role ---------------------------------------------------------

# The styles whose bass stays round and clean: no « grain » offered.
ROUND_BASS = {"lofi", "boom_bap", "liquid_dnb"}
WHEN_FX = T("Une fois le son et le motif posés.", "Once the sound and the pattern are set.", "Zodra de klank en het patroon staan.")
ONCE = T("Une chaîne pour tout le morceau.", "One chain for the whole song.", "Eén keten voor het hele nummer.")
LESS = T("Moins", "Less", "Minder")
MORE = T("Plus", "More", "Meer")


def fx_card(cid, target, title, why, proposals, how_much, lexicon, elsewhere=(), applicable=None, done=None, kind="review"):
    return card(cid, 3, title, why, target=target, kind=kind, applicable=applicable or [f"has:{target}"], done=done or [f"changed:{target}"],
                proposals=proposals, elsewhere=elsewhere, lexicon=lexicon, when=WHEN_FX, how_much=how_much, how_often=ONCE)


def stage3(s):
    peak = PEAK[s.club]
    out = [card(
        "fx.review", 3,
        T("Voilà ce que j'ai mis", "Here is what I put in", "Dit heb ik erop gezet"),
        T("J'ai posé une chaîne d'effets sur chaque piste, sans rien dire. Les voici une par une, chacune avec son pourquoi : écoute sans puis avec, à volume égal, puis garde ou retire.",
          "I put an effect chain on every track without saying a word. Here they are one by one, each with its why: hear it without then with, at equal loudness, then keep or remove.",
          "Ik heb op elk spoor een effectketen gezet, zonder iets te zeggen. Hier zijn ze een voor een, elk met zijn waarom: luister zonder en dan met, even luid, en hou of verwijder."),
        kind="review", applicable=["fx:any"], lexicon="mix",
        proposals=[keep(T("Tout garder", "Keep everything", "Alles houden"),
                        T("Ce que tu n'as pas retiré reste tel quel.", "What you did not remove stays as it is.", "Wat je niet verwijderde, blijft zoals het is.")),
                   open_spot("all", T("Voir tout ce qui joue", "See everything that plays", "Alles zien wat speelt"),
                             T("La liste complète des effets, envois et courbes.", "The full list of effects, sends and curves.", "De volledige lijst van effecten, sends en curves."), "mixer.whatplays")],
    )]
    out.append(fx_card(
        "fx.drums", "drums",
        T("La batterie : du punch et de la colle", "The drums: punch and glue", "De drums: punch en lijm"),
        T("Le kick et la caisse claire plus serrés (compresseur, un peu de saturation), puis toute la batterie collée par un compresseur léger.",
          "The kick and the snare tighter (compressor, a little saturation), then the whole kit glued by a light compressor.",
          "De kick en de snare strakker (compressor, wat saturatie), daarna het hele drumstel gelijmd door een lichte compressor."),
        [keep(), prop("softer", T("Plus doux", "Softer", "Zachter"), T("La colle travaille moins : plus de dynamique.", "The glue works less: more dynamics.", "De lijm werkt minder: meer dynamiek."),
                      {"type": "nudge", "effect": "COMPRESSOR", "param": 0, "delta": 0.1}, T("1 dB de réduction", "1 dB of reduction", "1 dB reductie")),
         prop("tighter", T("Plus serré", "Tighter", "Strakker"), T("La batterie plus dense, elle claque plus.", "Denser drums, they hit harder.", "Dichtere drums, ze slaan harder."),
              {"type": "nudge", "effect": "COMPRESSOR", "param": 0, "delta": -0.1}, T("3 dB de réduction", "3 dB of reduction", "3 dB reductie"))],
        T("1 à 3 dB de réduction ; saturation à peine audible.", "1 to 3 dB of reduction; saturation barely audible.", "1 tot 3 dB reductie; saturatie nauwelijks hoorbaar."),
        "compressor", applicable=["has:kick", "fx:drums"],
        elsewhere=[open_spot("favorite", T("Garder ce kit dans mes favoris", "Keep this kit in my favourites", "Deze kit bij mijn favorieten bewaren"), FAVORITE_WHY, "sounds.favorite", "drums")],
    ))
    if s.bass:
        preset = s.preset("bass")
        if preset and preset in PRESETS:
            name, desc = PRESETS[preset]
            title = T("La basse : {p}", "The bass: {p}", "De bas: {p}").fill(p=known(name))
            why = known(desc)
        else:
            # No ready-made chain: say nothing of effects the assistant may not have put.
            title = T("La basse : ses effets", "The bass: its effects", "De bas: zijn effecten")
            why = T("Écoute la basse avec et sans ses effets, au même volume : garde ce qui la rend plus présente.",
                    "Hear the bass with and without its effects, at the same loudness: keep what makes it more present.",
                    "Luister naar de bas met en zonder zijn effecten, even luid: hou wat hem aanweziger maakt.")
        dirty = preset in ("growl", "fat_bass", "wob_dubstep", "reese_dnb")  # a distortion insert to turn
        if dirty:
            props = [keep(), prop("cleaner", T("Moins sale", "Less dirty", "Minder vuil"), T("Plus de rondeur, moins de grain.", "Rounder, less grit.", "Ronder, minder korrel."),
                                  {"type": "nudge", "effect": "DISTORTION", "param": 0, "delta": -0.15}),
                     prop("dirtier", T("Plus sale", "Dirtier", "Vuiler"), T("Plus de grain : elle perce sur un téléphone.", "More grit: it cuts through on a phone.", "Meer korrel: hij komt door op een telefoon."),
                          {"type": "nudge", "effect": "DISTORTION", "param": 0, "delta": 0.1, "params": [0.3, 0.5, 0.3, 0.6]})]
        elif s.id in ROUND_BASS:
            props = [keep(), prop("later", LATER_EAR, LATER_EAR_WHY, {"type": "later"})]
        else:
            props = [keep(), prop("grain", T("Un peu de grain", "A little grit", "Een beetje korrel"),
                                  T("Une saturation légère : on l'entend aussi sur un téléphone.", "A light saturation: heard on a phone too.", "Een lichte saturatie: ook op een telefoon te horen."),
                                  {"type": "insert", "effect": "SATURATION", "params": [0.25, 1.0, 1.0, 0.7, 1.0]}, T("Drive 25 %", "Drive 25%", "Drive 25%"))]
        elsewhere = [open_spot("favorite", FAVORITE, FAVORITE_WHY, "sounds.favorite", "bass")]
        if s.switch:
            elsewhere.insert(0, prop("copy_bass2", T("Copier cette chaîne sur la Basse 2", "Copy this chain to Bass 2", "Deze keten naar Bas 2 kopiëren"),
                                     T("Les deux basses gardent la même place dans le mix.", "Both basses keep the same place in the mix.", "Beide bassen houden dezelfde plek in de mix."),
                                     {"type": "copyChain", "target": "bass", "to": "bass2"}))
        out.append(fx_card("fx.bass", "bass", title, why, props,
                           T("Point de départ, puis écoute : la basse au même volume avec et sans.", "A starting point, then listen: the bass at the same loudness with and without.", "Een vertrekpunt, dan luisteren: de bas even luid met en zonder."),
                           "saturation", elsewhere=elsewhere, applicable=["has:bass"]))
    if s.sub:
        out.append(fx_card(
            "fx.sub", "sub",
            T("Le sub : propre, au centre, un peu saturé", "The sub: clean, centred, a little saturated", "De sub: zuiver, in het midden, licht verzadigd"),
            T("Aucun effet ne colore encore le sub : une saturation légère entre 60 et 250 Hz le rend plus présent, sans le salir.",
              "No effect colours the sub yet: a light saturation between 60 and 250 Hz brings it forward without dirtying it.",
              "Nog geen effect kleurt de sub: een lichte saturatie tussen 60 en 250 Hz brengt hem naar voren, zonder hem vuil te maken."),
            [prop("light", T("Saturation légère", "Light saturation", "Lichte saturatie"), T("Juste assez pour qu'un petit haut-parleur le devine.", "Just enough for a small speaker to hint at it.", "Net genoeg zodat een kleine luidspreker hem laat vermoeden."),
                  {"type": "insert", "effect": "SATURATION", "params": [0.2, 1.0, 1.0, 0.7, 1.0]}, T("Drive 20 %", "Drive 20%", "Drive 20%")),
             keep(T("Le laisser tel quel", "Leave it as it is", "Laten zoals hij is"), T("Un grave rond, senti plus qu'entendu.", "A round low end, felt more than heard.", "Een rond laag, meer gevoeld dan gehoord."))],
            T("Saturation 15 à 25 %, mono strict.", "Saturation 15 to 25%, strictly mono.", "Saturatie 15 tot 25%, strikt mono."),
            "saturation", applicable=["has:sub"], done=["colour:sub", "by_hand|changed:sub"], kind="decide"))
    if s.bass and s.has_drum("kick"):
        out.append(fx_card(
            "fx.sidechain", "bass",
            T("La basse s'écarte quand le kick frappe", "The bass ducks when the kick hits", "De bas wijkt als de kick slaat"),
            T("Sinon les deux se battent dans le grave, surtout sur un téléphone. Toute la musique baisse un instant à chaque kick, le grave un peu plus.",
              "Otherwise both fight in the low end, on a phone above all. All the music dips an instant on each kick, the lows a little more.",
              "Anders vechten ze in het laag, vooral op een telefoon. Alle muziek zakt even bij elke kick, het laag iets meer."),
            [keep(T("Garder", "Keep", "Houden"), T("Si la musique s'écarte déjà sous le kick, c'est réglé : écoute-la.", "If the music already ducks under the kick, it is set: listen to it.", "Als de muziek al wijkt onder de kick, is het geregeld: luister ernaar.")),
             prop("light", T("Léger", "Light", "Licht"), T("On le sent, on ne l'entend pas.", "Felt, not heard.", "Je voelt het, je hoort het niet."), {"type": "duck", "value": 0.35}),
             prop("pump", T("Qui pompe", "Pumping", "Pompend"), T("On l'entend respirer : l'effet « house ».", "You hear it breathe: the “house” effect.", "Je hoort het ademen: het ‘house’-effect."), {"type": "duck", "value": 0.8})],
            T("Ratio 3:1 à 6:1, attaque 0 à 10 ms, relâche 60 à 120 ms.", "Ratio 3:1 to 6:1, attack 0 to 10 ms, release 60 to 120 ms.", "Ratio 3:1 tot 6:1, attack 0 tot 10 ms, release 60 tot 120 ms."),
            "sidechain", applicable=["has:bass", "has:kick"], done=["sidechain|changed:sidechain"]))
    if s.acid:
        out.append(fx_card(
            "fx.acid", "acid",
            T("La 303 : grain et écho", "The 303: grit and echo", "De 303: korrel en echo"),
            T("Un peu de saturation pour qu'elle morde, un écho discret pour qu'elle tourne.", "A little saturation so it bites, a discreet echo so it spins.", "Wat saturatie zodat hij bijt, een discrete echo zodat hij draait."),
            [keep(), prop("echo", T("Plus d'écho", "More echo", "Meer echo"), T("Elle tourne dans l'espace.", "It spins in space.", "Hij draait in de ruimte."), {"type": "send", "bus": 1, "delta": 0.1}),
             prop("dry", T("Plus sèche", "Drier", "Droger"), T("Elle reste devant, nette.", "It stays in front, clean.", "Hij blijft vooraan, helder."), {"type": "send", "bus": 1, "delta": -0.1})],
            T("Envoi écho −18 dB environ.", "Echo send about -18 dB.", "Echo-send rond -18 dB."), "acid", applicable=["has:acid"]))
    if s.chords != "none":
        out.append(fx_card(
            "fx.pad", "pad",
            T("Les accords : de la place et de l'espace", "The chords: room and space", "De akkoorden: ruimte en diepte"),
            T("Un coupe-bas vers 150 à 250 Hz laisse le grave à la basse ; la réverbe les met au fond, derrière le reste.",
              "A low cut around 150 to 250 Hz leaves the low end to the bass; the reverb puts them at the back, behind the rest.",
              "Een low-cut rond 150 tot 250 Hz laat het laag aan de bas; de reverb zet ze achteraan, achter de rest."),
            [keep(), prop("further", T("Plus loin", "Further", "Verder"), T("Plus de réverbe : ils reculent.", "More reverb: they move back.", "Meer reverb: ze schuiven naar achter."), {"type": "send", "bus": 0, "delta": 0.1}),
             prop("closer", T("Plus près", "Closer", "Dichterbij"), T("Moins de réverbe : ils avancent.", "Less reverb: they come forward.", "Minder reverb: ze komen naar voren."), {"type": "send", "bus": 0, "delta": -0.1})],
            T("Envoi réverbe −6 à −12 dB.", "Reverb send -6 to -12 dB.", "Reverb-send -6 tot -12 dB."), "reverb", applicable=["has:pad"],
            elsewhere=[open_spot("favorite", FAVORITE, FAVORITE_WHY, "sounds.favorite", "pad")]))
    if s.lead != "none":
        el = [open_spot("favorite", FAVORITE, FAVORITE_WHY, "sounds.favorite", "lead")]
        if s.counter:
            el.insert(0, prop("copy_counter", T("La même chaîne pour le contre-chant", "The same chain for the counter-melody", "Dezelfde keten voor de tegenmelodie"),
                              T("Les deux lignes sonnent dans le même espace.", "Both lines sound in the same space.", "Beide lijnen klinken in dezelfde ruimte."),
                              {"type": "copyChain", "target": "lead", "to": "counter"}))
        lp = s.preset("lead")
        if lp and lp in PRESETS:
            lname, ldesc = PRESETS[lp]
            ltitle = (T("La 2e basse : {p}", "The 2nd bass: {p}", "De 2e bas: {p}") if s.lead_is_bass else T("Le lead : {p}", "The lead: {p}", "De lead: {p}")).fill(p=known(lname))
            lwhy = known(ldesc)
        else:
            ltitle = T("Le lead : écho et réverbe", "The lead: echo and reverb", "De lead: echo en reverb")
            lwhy = T("Un écho à la croche et une réverbe moyenne : il chante devant sans prendre toute la place.",
                     "An eighth-note echo and a medium reverb: it sings in front without taking all the room.",
                     "Een echo op de achtste en een middelgrote reverb: hij zingt vooraan zonder alle ruimte te nemen.")
        out.append(fx_card(
            "fx.lead", "lead", ltitle, lwhy,
            ([keep(), prop("cleaner", T("Moins de grain", "Less grit", "Minder korrel"), T("Plus ronde, elle laisse la place à la première basse.", "Rounder, it leaves room for the first bass.", "Ronder, hij laat ruimte voor de eerste bas."),
                           {"type": "nudge", "effect": "DISTORTION", "param": 0, "delta": -0.15}),
              prop("dry", T("Plus sèche", "Drier", "Droger"), T("Moins d'écho : elle répond net, sans traîner.", "Less echo: it answers cleanly, without dragging.", "Minder echo: hij antwoordt helder, zonder te slepen."), {"type": "send", "bus": 1, "delta": -0.1})]
             if s.lead_is_bass else
             [keep(), prop("echo", T("Plus d'écho", "More echo", "Meer echo"), T("Chaque note rebondit plus longtemps.", "Each note bounces longer.", "Elke noot kaatst langer."), {"type": "send", "bus": 1, "delta": 0.1}),
              prop("dry", T("Plus sec", "Drier", "Droger"), T("Il reste net et proche.", "It stays clean and close.", "Hij blijft helder en dichtbij."), {"type": "send", "bus": 1, "delta": -0.1})]),
            T("Envoi écho −9 dB, réverbe −12 dB.", "Echo send -9 dB, reverb -12 dB.", "Echo-send -9 dB, reverb -12 dB."), "echo", applicable=["has:lead"], elsewhere=el))
    if s.vocal:
        out.append(fx_card(
            "fx.vocal", "vocal",
            T("La voix : coupe-bas, compresseur, réverbe", "The voice: low cut, compressor, reverb", "De stem: low-cut, compressor, reverb"),
            T("La voix n'a aucun effet : un coupe-bas et un compresseur la gardent claire, un petit pré-délai de réverbe garde chaque mot net.",
              "The voice has no effect: a low cut and a compressor keep it clear, a short reverb pre-delay keeps every word clean.",
              "De stem heeft geen effect: een low-cut en een compressor houden hem helder, een korte reverb-predelay houdt elk woord scherp."),
            [prop("clear", T("Voix claire", "Clear voice", "Heldere stem"), T("Coupe-bas à 120 Hz puis un compresseur doux.", "A low cut at 120 Hz, then a gentle compressor.", "Low-cut op 120 Hz, dan een zachte compressor."),
                  {"type": "chain", "inserts": [lowcut_insert(120), {"type": "insert", "effect": "COMPRESSOR", "params": [0.55, 0.3, 0.35, 0.35, 0.0, 1.0, 1.0, 0.3, 1.0]}]},
                  T("120 Hz · 3:1", "120 Hz · 3:1", "120 Hz · 3:1")),
             prop("echo", T("Voix d'écho", "Echo voice", "Echostem"), T("Elle se répète dans l'espace, comme un cri dans une salle.", "It repeats in space, like a shout in a hall.", "Hij herhaalt in de ruimte, als een kreet in een zaal."),
                  {"type": "send", "bus": 1, "value": 0.35}),
             keep(T("La laisser brute", "Leave it raw", "Rauw laten"), T("Brute, elle reste en avant.", "Raw, it stays upfront.", "Rauw blijft hij vooraan."))],
            T("Coupe-bas 100 à 150 Hz ; pré-délai 20 à 40 ms.", "Low cut 100 to 150 Hz; pre-delay 20 to 40 ms.", "Low-cut 100 tot 150 Hz; predelay 20 tot 40 ms."),
            "reverb", applicable=["has:vocal"], done=["colour:vocal", "by_hand|changed:vocal"], kind="decide"))
    out.append(card(
        "fx.sends", 3,
        T("Une réverbe et un écho pour tout le morceau", "One reverb and one echo for the whole song", "Eén reverb en één echo voor het hele nummer"),
        T("Toutes les pistes partagent les mêmes : le morceau sonne dans une seule salle. Leur retour est coupé sous 200 Hz et au-dessus de 10 kHz, le grave reste net.",
          "Every track shares the same ones: the song sounds in a single room. Their return is cut under 200 Hz and above 10 kHz, the low end stays clean.",
          "Alle sporen delen dezelfde: het nummer klinkt in één zaal. Hun return is afgesneden onder 200 Hz en boven 10 kHz, het laag blijft helder."),
        target="sends", kind="review", done=["changed:sends"], lexicon="reverb", when=WHEN_FX, how_often=T("Créés une fois, partagés par toutes les pistes.", "Made once, shared by every track.", "Eén keer gemaakt, door alle sporen gedeeld."),
        proposals=[keep(), prop("clean", T("Nettoyer les retours", "Clean the returns", "De returns opschonen"),
                                T("Coupe-bas 200 Hz et coupe-haut 10 kHz sur la réverbe et l'écho.", "Low cut 200 Hz and high cut 10 kHz on the reverb and the echo.", "Low-cut 200 Hz en high-cut 10 kHz op de reverb en de echo."),
                                {"type": "returns"}, T("200 Hz – 10 kHz", "200 Hz – 10 kHz", "200 Hz – 10 kHz"))],
    ))
    out.append(card(
        "fx.lowcut", 3,
        T("Un coupe-bas partout, sauf le kick et la basse", "A low cut everywhere, except the kick and the bass", "Overal een low-cut, behalve op de kick en de bas"),
        T("Couper plutôt que monter : chaque piste laisse le grave au kick et à la basse. C'est l'effet n° 1 des pros.",
          "Cut rather than boost: every track leaves the low end to the kick and the bass. The pros' number one effect.",
          "Liever wegsnijden dan oppompen: elk spoor laat het laag aan de kick en de bas. Het nummer-één-effect van de pro's."),
        kind="decide", done=["lowcut", "by_hand"], lexicon="filter", when=WHEN_FX, how_much=T("Accords 150–250 Hz, lead 200–300 Hz, voix 100–150 Hz.", "Chords 150–250 Hz, lead 200–300 Hz, voice 100–150 Hz.", "Akkoorden 150–250 Hz, lead 200–300 Hz, stem 100–150 Hz."), how_often=ONCE,
        proposals=[keep(T("Garder ce que j'ai mis", "Keep what I put", "Houden wat ik erop zette"), T("Chaque piste a déjà le sien : écoute-les, avec et sans.", "Every track already has its own: listen to them, with and without.", "Elk spoor heeft er al een: luister ernaar, met en zonder.")),
                   prop("all", T("Sur les pistes qui n'en ont pas", "On the tracks without one", "Op de sporen zonder"),
                        T("Chacune à la hauteur de son rôle.", "Each at the height of its role.", "Elk op de hoogte van zijn rol."), {"type": "lowCut"})]
    ))
    return out


# --- Stage 4: the arrangement -----------------------------------------------------------------------

def stage4(s):
    peak, cap = PEAK[s.club], PEAK_CAP[s.club]
    out = [card(
        "arr.subtract", 4,
        T("Le {cap} complet, puis on enlève", "The full {cap}, then take away", "De volle {cap}, dan weghalen").fill(cap=cap),
        (T("L'intro, la montée et le break sont le {peak} avec des pistes en moins : c'est ce qui donne envie de la suite.",
           "The intro, the build-up and the break are the {peak} with tracks taken out: that is what makes you want more.",
           "De intro, de opbouw en de break zijn de {peak} met sporen eruit: dat maakt dat je meer wilt.") if s.club else
         T("L'intro, le couplet et le pont sont le {peak} avec des pistes en moins : c'est ce qui donne envie de la suite.",
           "The intro, the verse and the bridge are the {peak} with tracks taken out: that is what makes you want more.",
           "De intro, het couplet en de brug zijn de {peak} met sporen eruit: dat maakt dat je meer wilt.")).fill(peak=peak),
        simple=True, simple_title=T("On enlève pour faire monter l'envie", "Take things out to build the craving", "Haal dingen weg om het verlangen op te bouwen"),
        simple_why=T("Le début joue moins de pistes que le grand moment.", "The start plays fewer tracks than the big moment.", "Het begin speelt minder sporen dan het grote moment."),
        done=["subtractive"], lexicon="drop" if s.club else "chorus",
        proposals=[prop("lighten", (T("Alléger l'intro et la montée", "Lighten the intro and the build-up", "De intro en de opbouw lichter maken") if s.club else
                                    T("Alléger l'intro et le couplet", "Lighten the intro and the verse", "De intro en het couplet lichter maken")),
                        T("Les basses et la mélodie n'y jouent plus : elles arrivent au {peak}.", "The basses and the melody no longer play there: they come in at the {peak}.",
                          "De bassen en de melodie spelen er niet meer: ze komen pas in de {peak}.").fill(peak=peak),
                        {"type": "subtract", "name": "lighten"}),
                   prop("break", T("Un Break tiré du {cap}", "A Break made from the {cap}", "Een break uit de {cap}").fill(cap=cap),
                        T("8 mesures du {peak} sans le kick ni les basses, juste après lui : on respire avant la suite.",
                          "8 bars of the {peak} without the kick or the basses, right after it: a breather before what comes next.",
                          "8 maten van de {peak} zonder kick en bassen, er meteen na: even ademen voor wat volgt.").fill(peak=peak),
                        {"type": "subtract", "name": "break"}, T("8 mesures", "8 bars", "8 maten")),
                   open_spot("parts", T("Voir ce que joue chaque partie", "See what each part plays", "Zien wat elk deel speelt"),
                             T("Une ligne par piste, une case par partie.", "One line per track, one box per part.", "Eén lijn per spoor, één vakje per deel."), "song.parts")],
    ), card(
        "arr.differ", 4,
        T("Chaque {peak} a sa surprise", "Every {peak} has its surprise", "Elke {peak} heeft zijn verrassing").fill(peak=peak),
        T("Le 2e {peak} = le 1er + un élément : une couche, une basse qui change, un contre-chant, une piste qui attend la 2e moitié.",
          "The 2nd {peak} = the 1st + one element: a layer, a bass that changes, a counter-melody, a track waiting for the second half.",
          "De 2e {peak} = de 1e + één element: een laag, een bas die verandert, een tegenmelodie, een spoor dat op de tweede helft wacht.").fill(peak=peak),
        simple=True, simple_title=T("Le 2e {peak} un peu différent", "The 2nd {peak} a little different", "De 2e {peak} een beetje anders").fill(peak=peak),
        simple_why=T("Sinon on a l'impression d'entendre deux fois la même chose.", "Otherwise it feels like hearing the same thing twice.", "Anders lijkt het alsof je twee keer hetzelfde hoort."),
        applicable=["peaks:2"], done=["peaks_differ"], lexicon="drop" if s.club else "chorus",
        proposals=[prop("lead_waits",
                        T("Au 1er, la 2e basse attend la 2e moitié", "In the 1st, the 2nd bass waits for the second half", "In de 1e wacht de 2e bas op de tweede helft") if s.lead_is_bass else
                        T("Au 1er, le lead attend la 2e moitié", "In the 1st, the lead waits for the second half", "In de 1e wacht de lead op de tweede helft"),
                        (T("Le 1er {peak} commence plus nu, la 2e basse répond ensuite.", "The 1st {peak} starts barer, the 2nd bass answers after.", "De 1e {peak} begint kaler, de 2e bas antwoordt erna.") if s.lead_is_bass else
                         T("Le 1er {peak} commence plus nu, le lead arrive ensuite.", "The 1st {peak} starts barer, the lead comes in after.", "De 1e {peak} begint kaler, de lead komt erna.")).fill(peak=peak),
                        {"type": "mute", "target": "lead", "section": "peak", "occurrence": 0, "half": "first"}),
                   prop("pad_waits", T("Au 1er, les accords attendent la 2e moitié", "In the 1st, the chords wait for the second half", "In de 1e wachten de akkoorden op de tweede helft"),
                        T("Le grave et la batterie seuls d'abord : le {peak} frappe plus.", "The low end and the drums alone first: the {peak} hits harder.", "Eerst alleen het laag en de drums: de {peak} slaat harder.").fill(peak=peak),
                        {"type": "mute", "target": "pad", "section": "peak", "occurrence": 0, "half": "first"}),
                   open_spot("parts", T("Je le fais moi-même", "I'll do it myself", "Ik doe het zelf"),
                             T("Une couche, une variation, un contre-chant au 2e.", "A layer, a variation, a counter-melody in the 2nd.", "Een laag, een variatie, een tegenmelodie in de 2e."), "song.parts",
                             simple_why=T("Un son en plus, une variation, une petite réponse au 2e.", "One more sound, a variation, a little answer in the 2nd.", "Een klank erbij, een variatie, een klein antwoord in de 2e."))],
    )]
    if s.bass:
        out.append(card(
            "arr.bass", 4,
            T("La basse change toutes les 8 à 16 mesures", "The bass changes every 8 to 16 bars", "De bas verandert om de 8 tot 16 maten"),
            T("Une même boucle de 4 mesures répétée tout le {peak} lasse : une variation, une octave, deux temps de silence en fin de 16.",
              "The same 4-bar loop repeated through the whole {peak} gets tiring: a variation, an octave, two beats of silence at the end of 16.",
              "Dezelfde lus van 4 maten de hele {peak} lang herhaald gaat vervelen: een variatie, een octaaf, twee tellen stilte aan het eind van 16.").fill(peak=peak),
            target="bass", simple=True, simple_title=T("La basse ne tourne pas toujours pareil", "The bass does not always loop the same", "De bas draait niet altijd hetzelfde"),
            simple_why=T("Un petit changement de temps en temps garde l'oreille éveillée.", "A small change now and then keeps the ear awake.", "Af en toe een kleine verandering houdt het oor wakker."),
            applicable=["has:bass", "peaks:1"], done=["bass_varies"], lexicon="bass",
            proposals=[open_spot("notes", T("Varier la fin de la boucle", "Vary the loop's ending", "Het einde van de lus variëren"),
                                 T("Les mesures 5 à 8 autrement, un silence avant le retour.", "Bars 5 to 8 another way, a rest before the return.", "Maat 5 tot 8 anders, een rust voor de terugkeer."), "notes.track", "bass"),
                       prop("later", LATER_EAR, LATER_EAR_WHY, {"type": "later"})],
            elsewhere=[open_spot("reuse", T("Réutiliser ce motif ailleurs", "Reuse this pattern elsewhere", "Dit patroon elders hergebruiken"),
                                 T("Le même motif, varié, dans une autre partie.", "The same pattern, varied, in another part.", "Hetzelfde patroon, gevarieerd, in een ander deel."), "song.parts", "bass")],
        ))
    if s.chords != "none" and s.club and s.signature != "chords":
        out.append(card(
            "arr.pad", 4,
            T("Les accords laissent de la place au Drop", "The chords leave room in the Drop", "De akkoorden laten ruimte in de Drop"),
            T("Dans le Drop la basse mène : sans les accords au début, le Drop frappe plus fort et ils reviennent comme une récompense.",
              "In the Drop the bass leads: without the chords at its start, the Drop hits harder and they come back as a reward.",
              "In de Drop leidt de bas: zonder de akkoorden aan het begin slaat de Drop harder en komen ze terug als beloning."),
            target="pad", simple=True, simple_title=T("Un peu de vide dans le Drop", "A little emptiness in the Drop", "Een beetje leegte in de Drop"),
            simple_why=T("Moins de pistes au début du Drop, il cogne plus.", "Fewer tracks at the Drop's start: it hits harder.", "Minder sporen aan het begin van de Drop: hij slaat harder."),
            applicable=["has:pad", "peaks:1"], done=["pad_room"], lexicon="pad",
            proposals=[prop("wait", T("Les accords attendent la 2e moitié du 1er Drop", "The chords wait for the 1st Drop's second half", "De akkoorden wachten op de tweede helft van de 1e Drop"),
                            T("La batterie et la basse seules d'abord.", "The drums and the bass alone first.", "Eerst alleen de drums en de bas."),
                            {"type": "mute", "target": "pad", "section": "peak", "occurrence": 0, "half": "first"}),
                       keep(T("Les garder partout", "Keep them everywhere", "Overal houden"), T("C'est voulu : ils portent le morceau.", "On purpose: they carry the song.", "Bewust: ze dragen het nummer."))],
        ))
    out.append(card(
        "arr.cut", 4,
        T("Une coupure juste avant le {peak}", "A cut just before the {peak}", "Een onderbreking net voor de {peak}").fill(peak=peak),
        T("Une mesure presque vide, ou deux temps de silence, juste avant : le retour du kick et de la basse claque.",
          "One nearly empty bar, or two beats of silence, just before: the kick and the bass come back with a bang.",
          "Eén bijna lege maat, of twee tellen stilte, net ervoor: de kick en de bas komen knallend terug."),
        simple=True, simple_title=T("Un petit silence avant le grand moment", "A short silence before the big moment", "Een korte stilte voor het grote moment"),
        simple_why=T("Le retour paraît deux fois plus fort.", "The return feels twice as loud.", "De terugkeer voelt twee keer zo luid."),
        applicable=["peaks:1"], done=["cut"], lexicon="buildup" if s.club else "chorus",
        proposals=[open_spot("parts", T("Couper la dernière mesure", "Cut the last bar", "De laatste maat wegsnijden"),
                             T("La partie d'avant finit presque vide.", "The part before ends nearly empty.", "Het deel ervoor eindigt bijna leeg."), "song.parts"),
                   keep(T("Enchaîner sans coupure", "Go straight on", "Zonder onderbreking doorgaan"), T("Le style le veut parfois : tout d'un bloc.", "The style sometimes wants it: all in one go.", "De stijl wil het soms: alles in één stuk."))],
    ))
    out.append(card(
        "arr.yours", 4,
        T("Qu'est-ce qui rend ce {peak} à toi ?", "What makes this {peak} yours?", "Wat maakt deze {peak} van jou?").fill(peak=peak),
        T("Un détail à toi par partie : un son, un cri, un silence, un fill. Le Guide propose, c'est toi qui tranches.",
          "One detail of yours per part: a sound, a shout, a silence, a fill. The Guide suggests, you decide.",
          "Eén detail van jou per deel: een klank, een kreet, een stilte, een fill. De Gids stelt voor, jij beslist."),
        kind="review", simple=True, simple_title=T("Ta touche à toi", "Your own touch", "Jouw eigen toets"),
        simple_why=T("Un détail qui ne vient que de toi.", "A detail that comes from you alone.", "Een detail dat alleen van jou komt."),
        lexicon="fill",
        proposals=[open_spot("fill", T("Un fill à moi", "A fill of mine", "Een eigen fill"), T("Un roulement que tu tapes toi-même.", "A roll you tap yourself.", "Een roffel die je zelf tikt."), "rhythm.pattern"),
                   open_spot("voice", T("Ma voix ou un bruit à moi", "My voice or a sound of mine", "Mijn stem of een eigen geluid"), T("Enregistré, posé sur une mesure.", "Recorded, set on a bar.", "Opgenomen, op een maat gezet."), "record.voice"),
                   open_spot("favorite", T("Un son de mes favoris", "A sound from my favourites", "Een klank uit mijn favorieten"), T("Celui que tu as gardé d'un autre morceau.", "The one you kept from another song.", "Die je uit een ander nummer bewaarde."), "sounds.favorite")],
    ))
    return out


# --- Stage 5: the movement --------------------------------------------------------------------------

def stage5(s):
    out = []
    if s.club:
        out.append(card(
            "move.build", 5,
            T("Le filtre s'ouvre dans la Montée", "The filter opens in the build-up", "Het filter gaat open in de opbouw"),
            T("Un filtre qui s'ouvre sur 4 à 8 mesures, avec un peu de résonance : la tension monte jusqu'au Drop.",
              "A filter opening over 4 to 8 bars, with a little resonance: the tension rises up to the Drop.",
              "Een filter dat over 4 tot 8 maten opengaat, met wat resonantie: de spanning stijgt tot aan de Drop."),
            applicable=["build"], done=["build_moves"], lexicon="filter", when=T("Dans la Montée.", "In the build-up.", "In de opbouw."),
            how_much=T("De fermé à ouvert sur 8 mesures, résonance 20 à 30 %.", "From closed to open over 8 bars, resonance 20 to 30%.", "Van dicht naar open over 8 maten, resonantie 20 tot 30%."),
            proposals=[prop("sweep", T("Ouvrir le filtre pour moi", "Open the filter for me", "Open het filter voor mij"),
                            T("Sombre au début de chaque Montée, grand ouvert à sa fin, au repos quand le Drop tombe.",
                              "Dark at the start of each build-up, wide open at its end, at rest when the Drop lands.",
                              "Donker aan het begin van elke opbouw, wijd open aan het eind, in rust als de Drop valt."),
                            {"type": "curve", "name": "sweep"}),
                       open_spot("curve", T("Dessiner la courbe", "Draw the curve", "De curve tekenen"), T("Du fermé à l'ouvert, la dernière mesure tout en haut.", "From closed to open, the last bar at the top.", "Van dicht naar open, de laatste maat helemaal boven."), "song.automation"),
                       prop("later", LATER_EAR, LATER_EAR_WHY, {"type": "later"})],
        ))
    out.append(card(
        "move.throw", 5,
        T("La réverbe monte en fin de phrase", "The reverb rises at the end of a phrase", "De reverb stijgt aan het eind van een frase"),
        T("Un envoi de réverbe ou d'écho qui monte sur la dernière mesure de chaque phrase, puis retombe : l'espace respire.",
          "A reverb or echo send rising on the last bar of each phrase, then falling back: the space breathes.",
          "Een reverb- of echo-send die op de laatste maat van elke frase stijgt en dan terugvalt: de ruimte ademt."),
        done=["send_moves", "by_hand|changed:lanes"], lexicon="reverb", when=T("En fin de phrase, toutes les 8 mesures.", "At the end of a phrase, every 8 bars.", "Aan het eind van een frase, om de 8 maten."),
        proposals=[prop("throw", T("Lancer la réverbe pour moi", "Throw the reverb for me", "Gooi de reverb voor mij"),
                        T("Sur la mélodie, la dernière mesure de chaque phrase de 8 : la note s'envole dans la salle.",
                          "On the melody, the last bar of each 8-bar phrase: the note flies off into the room.",
                          "Op de melodie, de laatste maat van elke frase van 8: de noot vliegt de zaal in."),
                        {"type": "curve", "name": "throw", "target": "lead"}),
                   keep(T("Garder ce que j'ai mis", "Keep what I put", "Houden wat ik erop zette"), T("Si la courbe est là, elle monte déjà en fin de phrase : écoute-la.", "If the curve is there, it already rises at each phrase's end: listen to it.", "Als de curve er is, stijgt hij al aan het eind van elke frase: luister ernaar.")),
                   open_spot("curve", T("Dessiner la courbe", "Draw the curve", "De curve tekenen"), T("Sur le lead ou la voix, la dernière mesure de chaque phrase.", "On the lead or the voice, the last bar of each phrase.", "Op de lead of de stem, de laatste maat van elke frase."), "song.automation", "lead")],
    ))
    out.append(card(
        "move.sections", 5,
        T("Au moins une courbe par partie", "At least one curve per part", "Minstens één curve per deel"),
        T("Une partie où rien ne bouge devient du papier peint, une nappe immobile s'oublie : 10 à 15 % de filtre sur quelques mesures suffisent. Deux courbes par piste au plus.",
          "A part where nothing moves turns into wallpaper, a still pad gets forgotten: 10 to 15% of filter over a few bars is enough. Two curves per track at most.",
          "Een deel waar niets beweegt wordt behang, een stilstaande pad raakt vergeten: 10 tot 15% filter over een paar maten volstaat. Hoogstens twee curves per spoor."),
        done=["sections_move"], lexicon="automation",
        proposals=[prop("wave", T("Faire respirer ces parties", "Make these parts breathe", "Laat deze delen ademen"),
                        T("Sur les 4 dernières mesures de chacune, le filtre se ferme un peu puis se rouvre : la suite arrive.",
                          "Over the last 4 bars of each, the filter closes a little then opens again: what follows is coming.",
                          "Over de laatste 4 maten van elk sluit het filter een beetje en gaat dan weer open: het vervolg komt eraan."),
                        {"type": "curve", "name": "wave"}, T("15 % de filtre", "15% of filter", "15% filter")),
                   open_spot("curve", T("Voir les parties immobiles", "See the still parts", "De stilstaande delen bekijken"), T("Une courbe douce sur une piste de chacune.", "A gentle curve on one track of each.", "Een zachte curve op één spoor van elk."), "song.automation"),
                   prop("later", LATER_EAR, LATER_EAR_WHY, {"type": "later"})],
    ))
    if s.has_drum("kick", "snare", "hat"):
        out.append(card(
            "move.fills", 5,
            T("Un fill avant chaque {peak}", "A fill before every {peak}", "Een fill voor elke {peak}").fill(peak=PEAK[s.club]),
            T("Un roulement de batterie sur la dernière mesure avant le {peak} : l'oreille sent que ça arrive, et le retour frappe.",
              "A drum roll on the last bar before the {peak}: the ear feels it coming, and the return hits.",
              "Een drumroffel op de laatste maat voor de {peak}: het oor voelt het aankomen, en de terugkeer slaat in.").fill(peak=PEAK[s.club]),
            applicable=["peaks:1", "has:kick"], done=["fills|cut"], lexicon="fill",
            when=T("La mesure juste avant le grand moment.", "The bar just before the big moment.", "De maat net voor het grote moment."),
            proposals=[prop("fills", T("Poser les fills pour moi", "Put the fills in for me", "Zet de fills voor mij"),
                            T("Le roulement du style, sur la seconde moitié de la mesure ; le reste de la batterie continue.",
                              "The style's roll, on the bar's second half; the rest of the drums carries on.",
                              "De roffel van de stijl, op de tweede helft van de maat; de rest van de drums speelt door."),
                            {"type": "fill"}),
                       open_spot("pattern", T("Taper mon propre fill", "Tap my own fill", "Mijn eigen fill tikken"),
                                 T("Un roulement à toi, sur la dernière mesure.", "A roll of your own, on the last bar.", "Een eigen roffel, op de laatste maat."), "rhythm.pattern"),
                       keep(T("Sans fill", "No fill", "Zonder fill"), T("Une coupure nette fait aussi l'affaire.", "A clean cut does the job too.", "Een strakke onderbreking werkt ook."))],
        ))
    return out


# --- Stage 6: levels and master ---------------------------------------------------------------------

def stage6(s):
    peak = PEAK[s.club]
    brk = T("Break", "Break", "Break") if s.club else T("pont", "bridge", "brug")
    ref = s.refs[0] if s.refs else ""
    out = [card(
        "mix.balance", 6,
        T("La balance : kick, basse, batterie, lead, le reste", "The balance: kick, bass, drums, lead, the rest", "De balans: kick, bas, drums, lead, de rest"),
        T("On règle dans cet ordre, avec les faders seuls, effets compris : le kick en référence, chaque piste à sa place autour de lui.",
          "Set them in this order, with the faders only, effects included: the kick as the reference, each track in its place around it.",
          "Stel ze in deze volgorde in, alleen met de faders, effecten inbegrepen: de kick als referentie, elk spoor op zijn plek eromheen."),
        simple=True, simple_title=T("Le volume de chaque piste", "Each track's volume", "Het volume van elk spoor"),
        simple_why=T("Le kick et la basse d'abord, le reste autour.", "The kick and the bass first, the rest around them.", "Eerst de kick en de bas, de rest eromheen."),
        done=["balanced|faders"], lexicon="mix",
        proposals=[prop("balance", T("Placer chaque piste pour moi", "Place each track for me", "Plaats elk spoor voor mij"),
                        T("J'écoute chaque piste seule au {peak}, le kick en référence, et je remets à sa place celle qui s'en écarte de plus de 3 dB.",
                          "I listen to each track alone in the {peak}, the kick as the reference, and put back in its place any that strays more than 3 dB.",
                          "Ik beluister elk spoor apart in de {peak}, met de kick als referentie, en zet elk spoor dat meer dan 3 dB afwijkt terug op zijn plek.").fill(peak=peak),
                        {"type": "balance"}, T("± 3 dB", "± 3 dB", "± 3 dB"),
                        simple_why=T("J'écoute chaque piste au {peak}, le kick en référence, et je remets à sa place celle qui est trop forte ou trop faible.",
                                     "I listen to each track in the {peak}, the kick as the reference, and put back in its place any that is too loud or too soft.",
                                     "Ik beluister elk spoor in de {peak}, met de kick als referentie, en zet elk spoor dat te luid of te zacht is terug op zijn plek.").fill(peak=peak)),
                   open_spot("mixer", T("Régler les volumes", "Set the volumes", "De volumes instellen"), T("Le kick en référence, chaque piste à sa place.", "The kick as the reference, each track in its place.", "De kick als referentie, elk spoor op zijn plek."), "mixer.strip"),
                   keep(T("Ça sonne déjà juste", "It already sounds right", "Het klinkt al goed"), T("Le mix de départ te convient.", "The starting mix suits you.", "De startmix bevalt je."))],
    ), card(
        "mix.pans", 6,
        T("La place de chaque piste dans la stéréo", "Each track's place in the stereo", "De plek van elk spoor in de stereo"),
        T("Le kick, la basse et la voix au centre ; les charleys, les percussions, les accords et les leads un peu sur les côtés.",
          "The kick, the bass and the voice in the centre; the hats, the percussion, the chords and the leads a little to the sides.",
          "De kick, de bas en de stem in het midden; de hi-hats, de percussie, de akkoorden en de leads een beetje opzij."),
        simple=True, simple_title=T("Gauche, droite, centre", "Left, right, centre", "Links, rechts, midden"),
        simple_why=T("Chaque son à sa place : tout s'entend mieux.", "Every sound in its place: everything is heard better.", "Elke klank op zijn plek: alles is beter te horen."),
        done=["pans", "by_hand|changed:pans"], lexicon="pan",
        proposals=[keep(T("Garder ces places", "Keep these places", "Deze plekken houden"), T("Si les pistes sont déjà placées, écoute-les au casque : c'est réglé.", "If the tracks are placed already, listen on headphones: it is set.", "Als de sporen al geplaatst zijn, luister met een koptelefoon: het is geregeld.")),
                   prop("place", T("Placer pour moi", "Place them for me", "Plaats ze voor mij"), T("Le grave au centre, le reste un peu écarté.", "The low end in the centre, the rest spread a little.", "Het laag in het midden, de rest een beetje uit elkaar."), {"type": "pans"})],
    ), card(
        "mix.master", 6,
        T("Un master honnête", "An honest master", "Een eerlijke master"),
        T("La cible du style ({lufs} LUFS au {peak}) et une chaîne qui travaille peu : 3 dB au plus. Plus fort, ça écrase et ça fatigue.",
          "The style's target ({lufs} LUFS in the {peak}) and a chain that works little: 3 dB at most. Louder squashes and tires.",
          "Het doel van de stijl ({lufs} LUFS in de {peak}) en een keten die weinig werkt: hoogstens 3 dB. Luider plet en vermoeit.").fill(lufs=lufs(s.lufs), peak=peak),
        simple=True, simple_title=T("Le volume final", "The final loudness", "Het eindvolume"),
        simple_why=T("Fort comme les autres morceaux du style, sans écraser.", "As loud as the style's other songs, without squashing.", "Even luid als de andere nummers van de stijl, zonder te pletten."),
        target="master", kind="review", done=["master", "by_hand|changed:master"], lexicon="master",
        proposals=[keep(), open_spot("master", T("Voir le master", "See the master", "De master bekijken"), T("La cible, la marge et ce que fait la chaîne.", "The target, the headroom and what the chain does.", "Het doel, de headroom en wat de keten doet."), "mixer.master",
                                               simple_label=T("Voir le volume final", "See the final loudness", "Het eindvolume bekijken"),
                                               simple_why=T("Le volume visé et ce qui l'ajuste.", "The loudness aimed at and what adjusts it.", "Het beoogde volume en wat het bijstelt."))],
    ), card(
        "mix.contrast", 6,
        T("Le {peak} plus fort que le {brk}", "The {peak} louder than the {brk}", "De {peak} luider dan de {brk}").fill(peak=peak, brk=brk),
        T("Au moins 4 dB d'écart entre le {brk} et le {peak} : sinon le {peak} ne tombe pas.", "At least 4 dB between the {brk} and the {peak}: otherwise the {peak} does not land.",
          "Minstens 4 dB verschil tussen de {brk} en de {peak}: anders landt de {peak} niet.").fill(peak=peak, brk=brk),
        simple=True, simple_title=T("Le {peak} plus fort que le {brk}", "The {peak} louder than the {brk}", "De {peak} luider dan de {brk}").fill(peak=peak, brk=brk),
        simple_why=T("Le {brk} un peu plus calme, pour que le {peak} frappe quand il revient.", "The {brk} a little calmer, so the {peak} hits when it comes back.",
                     "De {brk} iets rustiger, zodat de {peak} harder aankomt.").fill(peak=peak, brk=brk),
        applicable=["measured", "break"], done=["contrast"], lexicon="break",
        proposals=[open_spot("parts", T("Alléger le {brk}", "Lighten the {brk}", "De {brk} lichter maken").fill(brk=brk), T("Une piste de moins, ou son volume un peu plus bas.", "One track fewer, or its volume a little lower.", "Eén spoor minder, of het volume iets lager."), "song.parts"),
                   prop("later", LATER_EAR, LATER_EAR_WHY, {"type": "later"})],
    ), card(
        "mix.mono", 6,
        T("En mono, rien ne disparaît", "In mono, nothing disappears", "In mono verdwijnt niets"),
        T("Une enceinte de soirée, un téléphone, une radio replient souvent la stéréo en un seul son : un son trop large ou à l'envers s'y efface.",
          "A party speaker, a phone, a radio often fold the stereo into a single sound: a sound too wide or out of phase fades away there.",
          "Een feestbox, een telefoon, een radio vouwen de stereo vaak samen tot één klank: een te brede of gespiegelde klank verdwijnt daar."),
        kind="check", simple=True, simple_title=T("Écouter en mono", "Listen in mono", "In mono luisteren"),
        simple_why=T("Comme sur une seule enceinte : tout doit rester là.", "As on a single speaker: everything must stay there.", "Zoals op één box: alles moet er blijven."),
        done=["mono_ok"], lexicon="mix",
        proposals=[prop("listen", T("Écouter en mono", "Listen in mono", "In mono luisteren"),
                        T("Le {peak} en stéréo, puis replié en mono : ce qui s'efface s'entend.", "The {peak} in stereo, then folded to mono: what fades away is heard.",
                          "De {peak} in stereo, dan samengevouwen tot mono: wat verdwijnt, hoor je.").fill(peak=peak),
                        {"type": "listen", "name": "mono"},
                        simple_why=T("Le {peak} normal, puis comme sur une seule enceinte : ce qui s'efface s'entend.", "The {peak} as usual, then as on a single speaker: what fades away is heard.",
                                     "De {peak} gewoon, dan zoals op één box: wat verdwijnt, hoor je.").fill(peak=peak)),
                   prop("narrow", T("Resserrer la stéréo", "Narrow the stereo", "De stereo smaller maken"),
                        T("Les graves en mono jusqu'à 150 Hz, aucun élargisseur au-delà de 100 %.", "The lows in mono up to 150 Hz, no widener past 100%.", "Het laag in mono tot 150 Hz, geen verbreder boven 100%."),
                        {"type": "mono"}, T("150 Hz", "150 Hz", "150 Hz"),
                        simple_label=T("Resserrer le son", "Narrow the sound", "De klank smaller maken"),
                        simple_why=T("Les graves au milieu, rien de trop large : tout reste là sur une seule enceinte.", "The lows in the middle, nothing too wide: everything stays there on a single speaker.",
                                     "Het laag in het midden, niets te breed: alles blijft er op één box.")),
                   keep(T("C'est vérifié", "Checked", "Gecontroleerd"), T("Rien ne disparaît en mono.", "Nothing vanishes in mono.", "Niets verdwijnt in mono."))],
    ), card(
        "mix.phone", 6,
        T("Sur le haut-parleur d'un téléphone", "On a phone's speaker", "Op de luidspreker van een telefoon"),
        T("Un téléphone ne joue rien sous 300 Hz : le kick et la basse doivent encore s'y deviner, par leurs harmoniques.",
          "A phone plays nothing under 300 Hz: the kick and the bass must still be guessed there, through their harmonics.",
          "Een telefoon speelt niets onder 300 Hz: de kick en de bas moeten er nog te raden zijn, via hun boventonen."),
        kind="check", simple=True, simple_title=T("Écouter comme sur un téléphone", "Listen as on a phone", "Luisteren zoals op een telefoon"),
        simple_why=T("La plupart des gens l'écouteront là : la basse doit s'y entendre.", "Most people will hear it there: the bass must come through.", "De meeste mensen horen het daar: de bas moet erdoor komen."),
        done=["phone_ok"], lexicon="808" if s.bass808 else "bass",
        proposals=[prop("listen", T("Écouter comme un téléphone", "Listen as a phone", "Luisteren als een telefoon"),
                        T("Le {peak} tel quel, puis sans rien sous 300 Hz, au même volume.", "The {peak} as it is, then with nothing under 300 Hz, at the same loudness.",
                          "De {peak} zoals hij is, dan zonder iets onder 300 Hz, even luid.").fill(peak=peak),
                        {"type": "listen", "name": "phone"},
                        simple_why=T("Le {peak} tel quel, puis comme sur un petit haut-parleur, au même volume.", "The {peak} as it is, then as on a small speaker, at the same loudness.",
                                     "De {peak} zoals hij is, dan zoals op een kleine luidspreker, even luid.").fill(peak=peak)),
                   prop("audible", T("Une basse plus audible", "A more audible bass", "Een beter hoorbare bas"),
                        T("« Basse audible » sur la basse, un peu plus fort : ses harmoniques passent sur un petit haut-parleur.",
                          "“Audible bass” on the bass, a little stronger: its harmonics come through a small speaker.",
                          "‘Hoorbare bas’ op de bas, iets sterker: zijn boventonen komen door een kleine luidspreker."),
                        {"type": "nudge", "target": "bass", "effect": "BASS_EXCITER", "param": 0, "delta": 0.15, "params": [0.4]}, T("+15 %", "+15%", "+15%")),
                   keep(T("C'est vérifié", "Checked", "Gecontroleerd"), T("La basse se devine sur un téléphone.", "The bass can be guessed on a phone.", "De bas is te raden op een telefoon."))],
    ), card(
        "mix.reference", 6,
        T("À côté d'un morceau que tu aimes", "Next to a song you love", "Naast een nummer waar je van houdt"),
        (T("Compare avec {ref}, ou un autre morceau du style, au même volume : plus fort paraît toujours mieux, alors on égalise d'abord.",
           "Compare with {ref}, or another song of the style, at the same loudness: louder always seems better, so we level first.",
           "Vergelijk met {ref}, of een ander nummer van de stijl, even luid: luider lijkt altijd beter, dus eerst gelijk zetten.").fill(ref=ref) if ref else
         T("Compare avec un morceau du style, au même volume : plus fort paraît toujours mieux, alors on égalise d'abord.",
           "Compare with a song of the style, at the same loudness: louder always seems better, so we level first.",
           "Vergelijk even luid met een nummer van de stijl: luider lijkt altijd beter, dus eerst gelijk zetten.")),
        kind="check", simple=True, simple_title=T("À côté d'un morceau que tu aimes", "Next to a song you love", "Naast een nummer waar je van houdt"),
        simple_why=T("Au même volume, pour comparer le son, pas la force.", "At the same loudness, to compare the sound, not the level.", "Even luid, om de klank te vergelijken, niet de sterkte."),
        lexicon="mix",
        proposals=[prop("listen", T("Comparer à un morceau", "Compare with a song", "Vergelijken met een nummer"),
                        T("Choisis un fichier audio : ton {peak}, puis son passage le plus fort, au même volume.", "Pick an audio file: your {peak}, then its loudest passage, at the same loudness.",
                          "Kies een audiobestand: jouw {peak}, dan zijn luidste stuk, even luid.").fill(peak=peak),
                        {"type": "listen", "name": "reference"}),
                   keep(T("C'est vérifié", "Checked", "Gecontroleerd"), T("Mon morceau tient la comparaison.", "My song holds up.", "Mijn nummer doorstaat de vergelijking.")),
                   prop("later", T("Plus tard", "Later", "Later"), T("À la fin, avant d'exporter.", "At the end, before exporting.", "Aan het eind, voor het exporteren."), {"type": "later"})],
    )]
    return out


# --- Stage 7: export and share ----------------------------------------------------------------------

def stage7(s):
    return [card(
        "share.ready", 7,
        T("Prêt à partager ?", "Ready to share?", "Klaar om te delen?"),
        T("Les dernières vérifications (rien de coupé par erreur, pas de solo oublié, pas de saturation), puis un M4A pour l'écouter partout ou une vidéo pour le montrer.",
          "The last checks (nothing muted by mistake, no solo left on, no clipping), then an M4A to listen to it anywhere or a video to show it.",
          "De laatste controles (niets per ongeluk gedempt, geen solo vergeten, geen oversturing), dan een M4A om het overal te beluisteren of een video om het te laten zien."),
        kind="check", simple=True, simple_title=T("Partager le morceau", "Share the song", "Het nummer delen"),
        simple_why=T("Un dernier coup d'œil, puis en fichier ou en vidéo.", "A last look, then as a file or a video.", "Een laatste blik, dan als bestand of als video."),
        lexicon="master",
        proposals=[open_spot("check", T("Lancer les vérifications", "Run the checks", "De controles uitvoeren"), T("Six points, chacun avec sa solution.", "Six points, each with its fix.", "Zes punten, elk met zijn oplossing."), "share.check"),
                   open_spot("export", T("Exporter le M4A", "Export the M4A", "De M4A exporteren"), T("Le morceau entier, au volume du style.", "The whole song, at the style's loudness.", "Het hele nummer, op het volume van de stijl."), "share.export",
                             simple_label=T("Faire le fichier audio", "Make the audio file", "Het audiobestand maken")),
                   open_spot("video", T("Faire la vidéo", "Make the video", "De video maken"), T("Les pistes qui s'allument en rythme.", "The tracks lighting up in time.", "De sporen die in de maat oplichten."), "share.video")],
        elsewhere=[keep(T("C'est partagé", "Shared", "Gedeeld"), T("Le morceau est parti : bravo.", "The song is out: well done.", "Het nummer is weg: goed gedaan."))],
    )]


def cards_for(s):
    cards = stage0(s) + stage1(s) + stage2(s) + stage3(s) + stage4(s) + stage5(s) + stage6(s) + stage7(s)
    ids = [c["id"] for c in cards]
    assert len(ids) == len(set(ids)), f"{s.id}: duplicate card ids"
    return cards


# The generic parcours: a song made by hand, every role offered (each card applies only to the tracks there).
GENERIC = {
    "id": "generic", "bpm": 120, "drums": [{"role": r} for r in ("kick", "snare", "clap", "hat", "perc")],
    "chords": {"type": "pad"}, "lead": {"type": "hook"}, "bass": {"sound": "Basse ronde", "sub": False},
    "acid": {"use": True}, "vocal": None, "riser": False, "counter": False, "fx": [], "references": [],
}


# --- Why each effect is on a track (the sheet's « pourquoi »): by role, "*" for every role ------------

WHYS = {
    "*": {
        "EQ": T("Enlève ce qui gêne, ajoute un peu de couleur.", "Takes out what gets in the way, adds a little colour.", "Haalt weg wat stoort, voegt wat kleur toe."),
        "COMPRESSOR": T("Rapproche les passages forts et faibles : plus serré, plus présent.", "Brings loud and soft passages closer: tighter, more present.", "Brengt luide en zachte passages dichter bij elkaar: strakker, aanweziger."),
        "SATURATION": T("Un peu de grain : on l'entend mieux, même sur un téléphone.", "A little grit: heard better, even on a phone.", "Wat korrel: beter hoorbaar, zelfs op een telefoon."),
        "CHORUS": T("Épaissit et élargit le son.", "Thickens and widens the sound.", "Maakt de klank dikker en breder."),
        "FILTER": T("Rend le son plus sourd ou plus brillant ; une courbe le fait bouger.", "Makes the sound duller or brighter; a curve moves it.", "Maakt de klank doffer of helderder; een curve laat hem bewegen."),
        "PUMP": T("Le son se baisse à chaque kick puis remonte.", "The sound dips on each kick, then comes back up.", "De klank zakt bij elke kick en komt weer op."),
        "FLANGER": T("Un balayage qui tourne, très années 70.", "A sweeping whoosh, very seventies.", "Een draaiende zwiep, heel jaren zeventig."),
        "PHASER": T("Un balayage doux qui tourne.", "A soft, turning sweep.", "Een zachte, draaiende zwiep."),
        "GATE": T("Coupe le son sous un seuil : net entre les notes.", "Cuts the sound under a threshold: clean between the notes.", "Snijdt de klank onder een drempel weg: strak tussen de noten."),
        "TREMOLO": T("Le volume qui bat en rythme.", "The volume beating in time.", "Het volume dat in de maat klopt."),
        "WIDENER": T("Élargit le son dans la stéréo, le grave reste au centre.", "Widens the sound in the stereo, the low end stays centred.", "Maakt de klank breder in de stereo, het laag blijft in het midden."),
        "BITCRUSHER": T("Un son numérique, granuleux.", "A grainy, digital sound.", "Een korrelige, digitale klank."),
        "PARAM_EQ": T("Une bande précise : creuser, monter, ou couper le bas.", "One precise band: cut, boost, or remove the lows.", "Eén precieze band: uitdiepen, versterken of het laag wegsnijden."),
        "ROBOT": T("Une voix de robot, métallique.", "A metallic robot voice.", "Een metalen robotstem."),
        "WAH": T("Le « wah » qui parle.", "The talking “wah”.", "De pratende ‘wah’."),
        "VINYL": T("Le grain d'un vieux disque.", "An old record's grain.", "De korrel van een oude plaat."),
        "RADIO": T("Fin et nasillard, comme une vieille radio.", "Thin and nasal, like an old radio.", "Dun en nasaal, als een oude radio."),
        "CHOPPER": T("Le son coupé en tranches, en rythme.", "The sound sliced in time.", "De klank in de maat in plakjes gesneden."),
        "DISTORTION": T("Le grain qui grogne et qui mord.", "The grit that growls and bites.", "De korrel die gromt en bijt."),
        "ECHO": T("Chaque note se répète en rythme.", "Each note repeats in time.", "Elke noot herhaalt zich in de maat."),
        "PING_PONG": T("Chaque note rebondit à gauche et à droite.", "Each note bounces left and right.", "Elke noot kaatst links en rechts."),
        "REVERB": T("L'espace autour du son.", "The space around the sound.", "De ruimte rond de klank."),
        "AUTO_PAN": T("Le son bouge de gauche à droite.", "The sound moves from left to right.", "De klank beweegt van links naar rechts."),
        "OTT": T("Compresse en trois bandes : dense, gros, présent.", "Compresses in three bands: dense, big, present.", "Comprimeert in drie banden: dicht, groot, aanwezig."),
        "FREQ_SHIFT": T("Décale toutes les fréquences : un son étrange, métallique.", "Shifts every frequency: a strange, metallic sound.", "Verschuift alle frequenties: een vreemde, metalen klank."),
        "TAPE_STOP": T("Le disque qui ralentit jusqu'à l'arrêt.", "The record slowing down to a stop.", "De plaat die vertraagt tot stilstand."),
        "WOBBLE": T("Un filtre qui s'ouvre et se ferme en rythme : le « wob ».", "A filter opening and closing in time: the “wob”.", "Een filter dat in de maat open- en dichtgaat: de ‘wob’."),
        "BASS_EXCITER": T("Ajoute les harmoniques du grave : la basse s'entend aussi sur un téléphone, qui ne joue pas sous 150 Hz.",
                          "Adds the low end's harmonics: the bass is heard on a phone too, which plays nothing under 150 Hz.",
                          "Voegt de boventonen van het laag toe: de bas is ook op een telefoon te horen, die niets onder 150 Hz speelt."),
        "CLIPPER": "Arrondit les crêtes qui dépassent le niveau du son : plus dense, sans pomper. Parfait sur une basse, avant l'OTT.",
    },
    "bass": {
        "FILTER": T("Un passe-haut vers 80 Hz : le sub garde le tout grave.", "A high-pass around 80 Hz: the sub keeps the very low end.", "Een high-pass rond 80 Hz: de sub houdt het diepste laag."),
        "PARAM_EQ": T("+3 dB vers 120 Hz : le corps de la basse.", "+3 dB around 120 Hz: the bass's body.", "+3 dB rond 120 Hz: het lijf van de bas."),
    },
    "pad": {
        "FILTER": T("Le filtre qui s'ouvre dans la Montée.", "The filter that opens in the build-up.", "Het filter dat in de opbouw opengaat."),
    },
    "riser": {
        "FILTER": T("Le souffle s'ouvre jusqu'au grand moment.", "The noise opens up to the big moment.", "De ruis gaat open tot aan het grote moment."),
    },
    "drums": {
        "COMPRESSOR": T("Colle la batterie : un seul instrument plutôt que des coups séparés.", "Glues the drums: one instrument rather than separate hits.", "Lijmt de drums: één instrument in plaats van losse slagen."),
    },
    "kick": {
        "COMPRESSOR": T("Le punch : plus serré, il claque.", "The punch: tighter, it hits.", "De punch: strakker, hij slaat."),
    },
    "master": {
        "OTT": T("Un OTT léger : le morceau plus dense.", "A light OTT: a denser song.", "Een lichte OTT: een dichter nummer."),
        "COMPRESSOR": T("Une colle douce sur tout le morceau, 2 dB au plus.", "A gentle glue over the whole song, 2 dB at most.", "Een zachte lijm over het hele nummer, hoogstens 2 dB."),
    },
}


# --- The alerts' cards: a mistake to fix, before every other card -------------------------------------

REFUSE = T("C'est voulu", "It's on purpose", "Het is bewust")
REFUSE_WHY = T("Je ne te le redemanderai pas.", "I won't ask again.", "Ik vraag het niet meer.")


def refuse():
    return prop("refuse", REFUSE, REFUSE_WHY, {"type": "refuse"})


ALERTS = [
    card("alert.curves.too_long", 6,
         T("Des courbes dépassent la fin du morceau", "Some curves run past the end of the song", "Sommige curves lopen voorbij het einde van het nummer"),
         T("Le morceau a été raccourci et ses courbes n'ont pas suivi : un changement prévu pour le grand moment tombe ailleurs.",
           "The song was shortened and its curves did not follow: a change meant for the big moment lands elsewhere.",
           "Het nummer is ingekort en de curves zijn niet gevolgd: een verandering voor het grote moment valt ergens anders."),
         lexicon="automation",
         proposals=[open_spot("curves", T("Voir les courbes", "See the curves", "De curves bekijken"), T("Chaque courbe partie par partie.", "Each curve part by part.", "Elke curve deel per deel."), "song.automation"), refuse()]),
    card("alert.low.off_center", 6,
         T("Le grave n'est pas au centre", "The low end is not centred", "Het laag staat niet in het midden"),
         T("Un kick ou une basse sur un côté s'entend mal et disparaît sur un téléphone.", "A kick or a bass on one side sounds weak and vanishes on a phone.", "Een kick of bas aan één kant klinkt zwak en verdwijnt op een telefoon."),
         lexicon="mix",
         proposals=[prop("centre", T("Remettre au centre", "Put it back in the centre", "Terug in het midden zetten"), T("Le grave au milieu, comme partout.", "The low end in the middle, as everywhere.", "Het laag in het midden, zoals overal."), {"type": "pan", "value": 0.0}), refuse()]),
    card("alert.mix.solo", 6,
         T("Un solo est resté allumé", "A solo was left on", "Er staat nog een solo aan"),
         T("Les autres pistes ne s'entendent plus, ni à l'export.", "The other tracks can't be heard, nor in the export.", "De andere sporen zijn niet meer te horen, ook niet bij de export."),
         lexicon="mix",
         proposals=[prop("unsolo", T("Couper le solo", "Turn the solo off", "De solo uitzetten"), T("Toutes les pistes s'entendent à nouveau.", "Every track is heard again.", "Alle sporen zijn weer te horen."), {"type": "unsolo"}),
                    keep(T("Je l'écoute exprès", "I'm listening on purpose", "Ik luister er bewust naar"), T("Pense à le couper avant de partager.", "Remember to turn it off before sharing.", "Vergeet hem niet uit te zetten voor het delen."))]),
    card("alert.loudness.peak", 6,
         T("Le morceau sature", "The song clips", "Het nummer oversturt"),
         T("Des crêtes dépassent la limite : ça craque sur certains lecteurs.", "Some peaks go over the limit: it crackles on some players.", "Sommige pieken gaan over de grens: het kraakt op sommige spelers."),
         lexicon="master",
         proposals=[open_spot("master", T("Baisser le master", "Lower the master", "De master verlagen"), T("Un ou deux dB suffisent.", "One or two dB are enough.", "Een of twee dB volstaan."), "mixer.master"), refuse()]),
]


# --- The texts the code shows (GuideAnalyzer, GuideStages, GuideSheets): their translations live here too ---

CODE_TEXTS = [
    T("Le cadre", "The frame", "Het kader"), T("Les sons et les motifs", "Sounds and patterns", "Klanken en patronen"),
    T("Les couches", "The layers", "De lagen"), T("Les effets de chaque piste", "Each track's effects", "De effecten van elk spoor"),
    T("L'arrangement", "The arrangement", "Het arrangement"), T("Le mouvement", "The movement", "De beweging"),
    T("Les niveaux et le master", "Levels and master", "Niveaus en master"), T("Exporter et partager", "Export and share", "Exporteren en delen"),
    T("le cadre", "the frame", "het kader"), T("les sons", "the sounds", "de klanken"), T("les couches", "the layers", "de lagen"),
    T("les effets", "the effects", "de effecten"), T("l'arrangement", "the arrangement", "het arrangement"),
    T("le mouvement", "the movement", "de beweging"), T("les niveaux", "the levels", "de niveaus"), T("l'export", "the export", "de export"),
    T("%1$s · %2$s · %3$d pistes · tout est fait", "%1$s · %2$s · %3$d tracks · all done", "%1$s · %2$s · %3$d sporen · alles klaar"),
    T("%1$s · %2$s · %3$d pistes · à corriger : %4$s", "%1$s · %2$s · %3$d tracks · to fix: %4$s", "%1$s · %2$s · %3$d sporen · te verbeteren: %4$s"),
    T("%1$s · %2$s · %3$d pistes · étape %4$d/%5$d : %6$s", "%1$s · %2$s · %3$d tracks · step %4$d/%5$d: %6$s", "%1$s · %2$s · %3$d sporen · stap %4$d/%5$d: %6$s"),
    T("Tout est fait : ton morceau est prêt à partager.", "All done: your song is ready to share.", "Alles klaar: je nummer is klaar om te delen."),
    T("Te revoilà ! Tu en étais à : %s.", "Welcome back! You were at: %s.", "Welkom terug! Je was bij: %s."),
    T("Tu en étais à : %s.", "You were at: %s.", "Je was bij: %s."),
    T("On commence par : %s.", "Let's start with: %s.", "We beginnen met: %s."),
    T("Fait : rien encore, tout est devant toi.", "Done: nothing yet, it's all ahead of you.", "Gedaan: nog niets, alles ligt voor je."),
    T("Fait : %s.", "Done: %s.", "Gedaan: %s."),
    T("Reste : rien, bravo.", "Left: nothing, well done.", "Over: niets, goed gedaan."),
    T("Reste : %s. On reprend ?", "Left: %s. Shall we go on?", "Over: %s. Gaan we verder?"),
    T("refrain", "chorus", "refrein"), T("refrains", "choruses", "refreinen"), T("Drops", "Drops", "Drops"),
    T("le refrain", "the chorus", "het refrein"), T("le Drop", "the Drop", "de Drop"),
    T("Des courbes durent %1$d mesures pour un morceau de %2$d : elles sont sans doute décalées.",
      "Some curves last %1$d bars for a song of %2$d: they are probably shifted.", "Sommige curves duren %1$d maten voor een nummer van %2$d: ze zijn waarschijnlijk verschoven."),
    T("Pas au centre : %s. Le grave doit rester au milieu.", "Not centred: %s. The low end must stay in the middle.", "Niet in het midden: %s. Het laag moet in het midden blijven."),
    T("Un solo est resté allumé : %s. Les autres pistes ne s'entendent plus.", "A solo was left on: %s. The other tracks can't be heard.", "Er staat nog een solo aan: %s. De andere sporen zijn niet te horen."),
    T("Le morceau sature : crête à %s dBTP.", "The song clips: peak at %s dBTP.", "Het nummer oversturt: piek op %s dBTP."),
    T("Pistes vides : %s.", "Empty tracks: %s.", "Lege sporen: %s."),
    T("Sans aucun effet : %s.", "Without any effect: %s.", "Zonder enig effect: %s."),
    T("Sans coupe-bas : %s.", "Without a low cut: %s.", "Zonder low-cut: %s."),
    T("Sans réverbe ni écho : %s.", "Without reverb or echo: %s.", "Zonder reverb of echo: %s."),
    T("%d pistes, toutes au centre : rien n'est placé dans la stéréo.", "%d tracks, all in the centre: nothing is placed in the stereo.", "%d sporen, allemaal in het midden: niets staat in de stereo."),
    T("Tous les faders sont à 0 dB : la balance n'est pas encore faite.", "Every fader is at 0 dB: the balance is not done yet.", "Alle faders staan op 0 dB: de balans is nog niet gemaakt."),
    T("Plus de deux courbes sur : %s. Le mix bouge peut-être trop.", "More than two curves on: %s. The mix may move too much.", "Meer dan twee curves op: %s. De mix beweegt misschien te veel."),
    T("Master à %1$s dB, cible %2$s LUFS.", "Master at %1$s dB, target %2$s LUFS.", "Master op %1$s dB, doel %2$s LUFS."),
    T("Master à %s dB, sans cible de volume.", "Master at %s dB, no loudness target.", "Master op %s dB, zonder volumedoel."),
    T("%1$s − Break : %2$s dB (4 au moins).", "%1$s − Break: %2$s dB (4 at least).", "%1$s − Break: %2$s dB (minstens 4)."),
    T("Les %1$d %2$s jouent les mêmes notes : seuls un son ou un volume changent.", "The %1$d %2$s play the same notes: only a sound or a volume changes.", "De %1$d %2$s spelen dezelfde noten: alleen een klank of een volume verandert."),
    T("Les %1$d %2$s sont identiques.", "The %1$d %2$s are identical.", "De %1$d %2$s zijn identiek."),
    T("%1$d %2$s sur %3$d sont identiques.", "%1$d %2$s out of %3$d are identical.", "%1$d %2$s van de %3$d zijn identiek."),
    T("La basse rejoue la même boucle de %1$d mesures %2$d fois de suite dans %3$s.",
      "The bass plays the same %1$d-bar loop %2$d times in a row in %3$s.", "De bas speelt dezelfde lus van %1$d maten %2$d keer na elkaar in %3$s."),
    T("Les accords jouent pendant tout %1$s (%2$d sur %2$d).", "The chords play throughout %1$s (%2$d out of %2$d).", "De akkoorden spelen door tijdens %1$s (%2$d van de %2$d)."),
    T("Rien ne bouge dans : %s.", "Nothing moves in: %s.", "Niets beweegt in: %s."),
    T("Les accords dans %1$s : %2$s.", "The chords in %1$s: %2$s.", "De akkoorden in %1$s: %2$s."),
    T("%s : la ligne passe les 20 règles de mélodie.", "%s: the line passes the 20 melody rules.", "%s: de lijn haalt de 20 melodieregels."),
    T("%1$s : %2$d règle(s) de mélodie sur 20 à revoir (%3$s).", "%1$s: %2$d of the 20 melody rules to check (%3$s).", "%1$s: %2$d van de 20 melodieregels na te kijken (%3$s)."),
    T("Volume", "Volume", "Volume"), T("Pan", "Pan", "Pan"), T("Effet", "Effect", "Effect"), T("Son", "Sound", "Klank"),
    T("Rien à changer ici : la piste n'a pas cet effet ou il est déjà réglé.", "Nothing to change here: the track does not have this effect, or it is already set.", "Hier valt niets te veranderen: het spoor heeft dit effect niet, of het staat al goed."),
    T("Ce réglage a changé depuis : le ramener effacerait ce que tu as fait après. Change-le à la main, ou annule d'abord le reste.",
      "This setting changed since: bringing it back would erase what you did after. Change it by hand, or undo the rest first.",
      "Deze instelling is sindsdien veranderd: terugzetten zou wissen wat je erna deed. Pas het met de hand aan, of maak eerst de rest ongedaan."),
    T("Cette piste n'est plus là : rien à ramener.", "This track is gone: nothing to bring back.", "Dit spoor is er niet meer: niets terug te zetten."),
    T("Trop de courbes dans le morceau pour ramener celle-ci.", "Too many curves in the song to bring this one back.", "Te veel curves in het nummer om deze terug te zetten."),
    # G4
    T("Il faut d'abord mesurer le morceau.", "The song must be measured first.", "Het nummer moet eerst gemeten worden."),
    T("Plus de place pour une courbe : le morceau en a déjà 16.", "No room for another curve: the song already has 16.", "Geen plaats meer voor een curve: het nummer heeft er al 16."),
    T("Retirer", "Remove", "Verwijderen"), T("Le remettre", "Put it back", "Terugzetten"),
    T("Pas à leur place : %s.", "Out of place: %s.", "Niet op hun plek: %s."),
    T("En mono, le morceau perd %s dB : des sons s'effacent.", "In mono, the song loses %s dB: some sounds fade away.", "In mono verliest het nummer %s dB: sommige klanken vervagen."),
    T("En mono, le morceau ne perd que %s dB : rien ne disparaît.", "In mono, the song only loses %s dB: nothing disappears.", "In mono verliest het nummer maar %s dB: niets verdwijnt."),
    T("Sur un haut-parleur de téléphone, la basse disparaît (%s dB).", "On a phone's speaker, the bass disappears (%s dB).", "Op de luidspreker van een telefoon verdwijnt de bas (%s dB)."),
    T("Sur un haut-parleur de téléphone, la basse se devine encore (%s dB).", "On a phone's speaker, the bass can still be guessed (%s dB).", "Op de luidspreker van een telefoon is de bas nog te raden (%s dB)."),
]



# --- The common cards (G5): the start of every song, then the Remix -----------------------------------
# guides/common.json, merged into every parcours by GuideLibrary.forProject: « Pour commencer » (stage -1)
# takes over « Ton morceau est prêt », the Missions « Mon premier morceau » and the « Prochaine étape » of a
# song made by hand; the Remix closes stage 7. Each card applies only when its detectors say (« start:first »
# a first song, « start:any » a song the assistant just made, « by_hand » a song made by hand).

START = -1


def play(pid, label, why, name):
    return prop(pid, label, why, {"type": "play", "name": name})


def later_prop():
    return prop("later", T("Plus tard", "Later", "Later"), T("Je te le reproposerai à la fin.", "I'll offer it again at the end.", "Ik stel het aan het eind opnieuw voor."), {"type": "later"})


COMMON = [
    card("start.style", START, T("Choisis un style", "Choose a style", "Kies een stijl"),
         T("L'appli écrit tout le morceau pour toi : rythme, basse, accords et mélodie. Ensuite, tu le fais tien.",
           "The app writes the whole song for you: beat, bass, chords and melody. Then you make it yours.",
           "De app schrijft het hele nummer voor je: ritme, bas, akkoorden en melodie. Daarna maak je het van jou."),
         simple=True, applicable=["start:first", "!style"], done=["song2"], lexicon="part",
         proposals=[open_spot("style", T("Choisir un style", "Choose a style", "Een stijl kiezen"),
                              T("Un style, une ambiance : le morceau est prêt en une minute.", "A style, a mood: the song is ready in a minute.", "Een stijl, een sfeer: het nummer is in een minuut klaar."), "new.style"),
                    keep(T("Je pars de zéro", "I'll start from scratch", "Ik begin vanaf nul"),
                         T("Tu poses toi-même le rythme, la basse et la mélodie.", "You lay down the beat, the bass and the melody yourself.", "Je zet zelf het ritme, de bas en de melodie neer."))]),
    card("start.ready", START, T("Ton morceau est prêt !", "Your song is ready!", "Je nummer is klaar!"),
         T("Écoute-le, puis fais-le tien : un son, la structure, et partage-le.",
           "Listen to it, then make it yours: a sound, the structure, and share it.",
           "Luister ernaar en maak het van jou: een geluid, de structuur, en deel het."),
         simple=True, applicable=["start:any", "style"], lexicon="drop",
         proposals=[play("drop", T("Écouter", "Listen", "Luisteren"),
                         T("Juste avant le premier grand moment : le meilleur passage, tout de suite.", "Just before the first big moment: the best part, right away.", "Vlak voor het eerste grote moment: meteen het beste stuk."), "drop"),
                    play("start", T("Depuis le début", "From the start", "Vanaf het begin"),
                         T("Tout le morceau, de la première mesure.", "The whole song, from the first bar.", "Het hele nummer, vanaf de eerste maat."), "start"),
                    open_spot("guide", T("Le guide de ton morceau", "Your song's guide", "De gids van je nummer"),
                              T("Ses parties, ce que chacune ajoute, et quoi essayer.", "Its parts, what each one adds, and what to try.", "Zijn delen, wat elk toevoegt, en wat je kunt proberen."), "song.guide")],
         elsewhere=[open_spot("sound", T("Changer un son", "Change a sound", "Een geluid wisselen"), T("Le son de la mélodie, en un toucher.", "The melody's sound, in one tap.", "De klank van de melodie, met één tik."), "sounds.main"),
                    open_spot("structure", T("Modifier le morceau", "Edit the song", "Nummer aanpassen"), T("Ses parties, dans l'ordre.", "Its parts, in order.", "Zijn delen, op volgorde."), "song.parts"),
                    open_spot("share", T("Partager", "Share", "Delen"), T("Les vérifications, puis le fichier.", "The checks, then the file.", "De controles, dan het bestand."), "share.check")]),
    card("start.sound", START, T("Change un son", "Change a sound", "Verander een geluid"),
         T("Fais-le tien : un autre son pour la mélodie ou pour la batterie.", "Make it yours: another sound for the melody or the drums.", "Maak het van jou: een andere klank voor de melodie of de drums."),
         simple=True, applicable=["start:first", "style"], done=["start_changed:sound"], lexicon="lead",
         proposals=[open_spot("melody", T("Le son de la mélodie", "The melody's sound", "De klank van de melodie"), T("Touche-en un : tu l'entends tout de suite.", "Tap one: you hear it at once.", "Tik er een aan: je hoort het meteen."), "sounds.main"),
                    open_spot("kit", T("Une autre batterie", "Another drum kit", "Een ander drumstel"), T("Les mêmes coups, un autre son.", "The same hits, another sound.", "Dezelfde slagen, een andere klank."), "rhythm.kit")]),
    card("start.melody", START, T("Enregistre une mélodie", "Record a melody", "Neem een melodie op"),
         T("Touche « ● Enregistrer ma mélodie » et joue quelques notes : le clavier reste dans la gamme, aucune fausse note.",
           "Tap “● Record my melody” and play a few notes: the keyboard stays in the scale, no wrong note.",
           "Tik op ‘● Mijn melodie opnemen’ en speel een paar noten: het klavier blijft in de toonladder, geen valse noot."),
         simple=True, applicable=["start:first", "style"], done=["start_changed:notes"], lexicon="scale",
         proposals=[open_spot("keys", T("Ouvrir le clavier", "Open the keyboard", "Het klavier openen"), T("Le bouton d'enregistrement s'allume.", "The record button lights up.", "De opnameknop licht op."), "record.melody"),
                    open_spot("notes", T("Voir les notes", "See the notes", "De noten bekijken"), T("Celles de la mélodie, à toucher une par une.", "The melody's, to tap one by one.", "Die van de melodie, één voor één aan te tikken."), "notes.track", target="lead")]),
    card("start.arrange", START, T("Arrange les parties", "Arrange the parts", "Schik de delen"),
         T("Appui long sur une partie : ajoute un Fill, un Break ou une Montée.", "Long press on a part: add a Fill, a Break or a Build-up.", "Lang drukken op een deel: voeg een Fill, een Break of een Opbouw toe."),
         simple=True, applicable=["start:first", "style"], done=["start_changed:arrange"], lexicon="fill",
         proposals=[open_spot("parts", T("Voir les parties", "See the parts", "De delen bekijken"), T("Le morceau, partie par partie.", "The song, part by part.", "Het nummer, deel per deel."), "song.parts"),
                    later_prop()]),
    card("start.beat", START, T("Pose un rythme", "Lay down a beat", "Zet een ritme neer"),
         T("Touche les cases de la grille : le kick sur chaque temps, pour commencer.", "Tap the grid's cells: the kick on every beat, to start with.", "Tik op de vakjes van het raster: de kick op elke tel, om te beginnen."),
         simple=True, applicable=["by_hand"], done=["beat"], lexicon="kick",
         proposals=[open_spot("grid", T("Aller à la grille", "Go to the grid", "Naar het raster"), T("Seize cases par mesure.", "Sixteen cells a bar.", "Zestien vakjes per maat."), "rhythm.pattern"),
                    open_spot("kit", T("Choisir une batterie", "Choose a drum kit", "Een drumstel kiezen"), T("Le son des coups.", "The hits' sound.", "De klank van de slagen."), "rhythm.kit")]),
    card("start.bassline", START, T("Ajoute une basse", "Add a bass", "Voeg een bas toe"),
         T("Une ligne grave : génère une basse acide, ou joue des notes graves au clavier d'une piste.", "A low line: generate an acid bass, or play low notes on a track's keyboard.", "Een lage lijn: genereer een acid-bas, of speel lage noten op het klavier van een spoor."),
         simple=True, applicable=["by_hand"], done=["bassline"], lexicon="bass",
         proposals=[open_spot("acid", T("Générer une basse acide", "Generate an acid bass", "Een acid-bas genereren"), T("Une ligne 303, en un toucher.", "A 303 line, in one tap.", "Een 303-lijn, met één tik."), "acid"),
                    open_spot("notes", T("Jouer la basse au clavier", "Play the bass on the keyboard", "De bas op het klavier spelen"), T("Les notes du bas du clavier.", "The keyboard's low notes.", "De lage noten van het klavier."), "notes.track", target="bass")]),
    card("start.tune", START, T("Ajoute une mélodie", "Add a melody", "Voeg een melodie toe"),
         T("Joue quelques notes au clavier d'une piste : il reste dans la gamme.", "Play a few notes on a track's keyboard: it stays in the scale.", "Speel een paar noten op het klavier van een spoor: het blijft in de toonladder."),
         simple=True, applicable=["by_hand"], done=["tune"], lexicon="lead",
         proposals=[open_spot("keys", T("Ouvrir le clavier", "Open the keyboard", "Het klavier openen"), T("Le bouton d'enregistrement s'allume.", "The record button lights up.", "De opnameknop licht op."), "record.melody"),
                    open_spot("notes", T("Écrire les notes", "Write the notes", "De noten schrijven"), T("Une note par toucher, sur la grille.", "One note per tap, on the grid.", "Eén noot per tik, op het raster."), "notes.track", target="lead")]),
    card("start.parts", START, T("Fais une deuxième partie", "Make a second part", "Maak een tweede deel"),
         T("Duplique ta partie et change-la : un couplet, un refrain. Donne-leur un nom pour t'y retrouver.",
           "Duplicate your part and change it: a verse, a chorus. Give them a name to find your way.",
           "Dupliceer je deel en verander het: een couplet, een refrein. Geef ze een naam om je weg te vinden."),
         simple=True, applicable=["by_hand"], done=["parts2"], lexicon="part",
         proposals=[open_spot("parts", T("Voir les parties", "See the parts", "De delen bekijken"), T("Dupliquer, renommer.", "Duplicate, rename.", "Dupliceren, hernoemen."), "song.parts"), later_prop()]),
    card("start.song", START, T("Enchaîne tes parties", "Chain your parts", "Zet je delen achter elkaar"),
         T("Au moins trois parties dans le morceau, avec un début et une fin.", "At least three parts in the song, with a start and an end.", "Minstens drie delen in het nummer, met een begin en een einde."),
         simple=True, applicable=["by_hand"], done=["song3"], lexicon="chorus",
         proposals=[open_spot("parts", T("Voir les parties", "See the parts", "De delen bekijken"), T("Ajoute-les dans l'ordre.", "Add them in order.", "Voeg ze op volgorde toe."), "song.parts"), later_prop()]),
    card("share.remix", 7, T("Remixer un morceau", "Remix a song", "Een nummer remixen"),
         T("L'appli sépare une chanson en batterie, basse, voix et le reste : rejoue-la à ta façon.",
           "The app splits a song into drums, bass, vocals and the rest: play it again your way.",
           "De app splitst een nummer in drums, bas, zang en de rest: speel het op jouw manier."),
         kind="check", simple=True, applicable=["start:any"], lexicon="",
         proposals=[open_spot("remix", T("Remixer une chanson", "Remix a song", "Een nummer remixen"), T("Choisis un fichier de ton téléphone.", "Pick a file from your phone.", "Kies een bestand van je telefoon."), "remix"),
                    dict(keep(T("Pas maintenant", "Not now", "Niet nu"), T("Il t'attend dans Sons › Remix, quand tu veux.", "It waits for you in Sounds › Remix, whenever you like.", "Het wacht op je in Klanken › Remix, wanneer je wilt.")),
                         simpleWhy=T("Il t'attend dans ⋮ › Remixer une chanson, quand tu veux.", "It waits for you in ⋮ › Remix a song, whenever you like.", "Het wacht op je in ⋮ › Een nummer remixen, wanneer je wilt."))]),
]

# The stage of « Pour commencer » (GuideStages.START_TITLE, START_SHORT), shown by the app.
CODE_TEXTS_G5 = [T("Pour commencer", "Getting started", "Om te beginnen"), T("le départ", "the start", "het begin"),
                 T("Les volumes", "The volumes", "De volumes")]  # GuideStages.SIMPLE_TITLES

# --- Writing ----------------------------------------------------------------------------------------

class Texts:
    def __init__(self):
        self.en, self.nl = {}, {}

    def put(self, t):
        if t is None:
            return None
        fr = t.v["fr"]
        for lang, table in (("en", self.en), ("nl", self.nl)):
            value = t.v[lang]
            # A text the app already translates keeps its words (the same French means the same thing).
            if CATALOG[lang].get(fr) is not None:
                continue
            if table.get(fr, value) != value:
                raise SystemExit(f"{fr!r} has two {lang} wordings: {table[fr]!r} and {value!r}")
            table[fr] = value
        return fr


def render(c, texts):
    def text(t):
        return texts.put(t)

    def proposal(p):
        out = {"id": p["id"], "label": text(p["label"])}
        if p.get("why") is not None:
            out["why"] = text(p["why"])
        if p.get("value") is not None:
            out["value"] = text(p["value"])
        for key in ("simpleLabel", "simpleWhy"):
            if p.get(key) is not None:
                out[key] = text(p[key])
        out["action"] = p["action"]
        return out

    out = {"id": c["id"], "stage": c["stage"], "target": c["target"], "kind": c["kind"]}
    if c["simple"]:
        out["simple"] = True
    out["title"] = text(c["title"])
    out["why"] = text(c["why"])
    for key in ("simpleTitle", "simpleWhy", "when", "howMuch", "howOften"):
        if c.get(key) is not None:
            out[key] = text(c[key])
    for key in ("applicable", "done"):
        if c[key]:
            out[key] = c[key]
    out["proposals"] = [proposal(p) for p in c["proposals"]]
    if c["elsewhere"]:
        out["elsewhere"] = [proposal(p) for p in c["elsewhere"]]
    if c["lexicon"]:
        out["lexicon"] = c["lexicon"]
    return out


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1, sort_keys=False)
        f.write("\n")


def main(out_dir=RES):
    recipes = {}
    for name in sorted(os.listdir(os.path.join(ROOT, "scripts", "packs", "recipes"))):
        if name.endswith(".json"):
            with open(os.path.join(ROOT, "scripts", "packs", "recipes", name), encoding="utf-8") as f:
                for r in json.load(f):
                    recipes[r["id"]] = r
    missing = [i for i in ORDER if i not in recipes]
    if missing:
        raise SystemExit(f"no recipe for {missing}")
    texts = Texts()
    global WHYS_RENDERED
    WHYS_RENDERED = {role: {fx: texts.put(known(t) if isinstance(t, str) else t) for fx, t in table.items()} for role, table in WHYS.items()}
    counts = {}
    for sid in ORDER + ["generic"]:
        if sid == "generic":
            NAMES.setdefault("generic", ("Mon morceau", "Autres"))
            s = Style(GENERIC)
        else:
            s = Style(recipes[sid])
        cards = [render(c, texts) for c in cards_for(s)]
        counts[sid] = len(cards)
        write_json(os.path.join(out_dir, "guides", f"{sid}.json"), {
            "format": FORMAT, "style": sid, "version": PARCOURS_VERSION, "cards": cards, "whys": WHYS_RENDERED,
        })
    write_json(os.path.join(out_dir, "guides", "alerts.json"), {
        "format": FORMAT, "style": "alerts", "version": PARCOURS_VERSION, "cards": [render(c, texts) for c in ALERTS],
    })
    write_json(os.path.join(out_dir, "guides", "common.json"), {
        "format": FORMAT, "style": "common", "version": PARCOURS_VERSION, "cards": [render(c, texts) for c in COMMON],
    })
    for t in CODE_TEXTS + CODE_TEXTS_G5:
        texts.put(t)
    for lang, table in (("en", texts.en), ("nl", texts.nl)):
        write_json(os.path.join(out_dir, "i18n", lang, "guide.json"), dict(sorted(table.items())))
    print(f"{len(counts)} parcours written: " + ", ".join(f"{k} {v}" for k, v in counts.items()))


if __name__ == "__main__":
    main(*sys.argv[1:])
