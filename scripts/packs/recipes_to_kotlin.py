#!/usr/bin/env python3
"""Turns the researched genre recipes (JSON, see the brief) into the assistant's styles (Kotlin).

Usage: recipes_to_kotlin.py <out .kt> <recipes.json>...    (or --names: the sounds, presets and effects it accepts)
  python3 scripts/packs/recipes_to_kotlin.py project-model/src/main/kotlin/com/pocketstudio/model/StyleRecipes.kt scripts/packs/recipes/*.json
Every value is checked (patterns, scales, sounds, packs) so that a typo fails here, not in the app.
A test (StyleRecipesUpToDateTest) fails when StyleRecipes.kt is not what this script makes.

Recipe syntax (one JSON object per style; one character per sixteenth, 16 = a bar, 32 = two bars):
  drums[]: {"role": kick|snare|clap|hat|open_hat|perc|808|tom|ride|perc2|rumble, "pattern": "X...x...o...2...", "tip_fr": "...",
            "source": <pack the line's sound comes from, default the style's own>,
            "voice": <built-in kit voice when no pack is there, default the pad's own: see VOICES>,
            "build": <the line in the second section (Groove/Couplet), where it builds up: hats that come in>,
            "off": true (offered switched off: an alternative, such as the techno rumble next to its bass line)}
    roles: tom (lane 8, kit toms), ride (lane 12, the kit cymbal: no pack has a ride), perc2 (lane 13, a second
    percussion: shaker by default), rumble (lane 3: the kick again, played wherever the kick plays, meant for the
    "rumble" fx preset: reverb, distortion, low-pass, pump).
    pattern: 'X' strong, 'x' medium, 'o' soft, '.' nothing, '2' '3' '4' a medium roll of that many hits in the step.
  bass/lead "pattern" (and acid "pattern"): one token per step on the bar's chord: 'R' root, '2'..'7' scale
    steps, 'O' octave up (lower case 'r' 'o' softer), '=' the previous pitch again (a common tone held across
    the chord change), '-' holds the note, '.' silence, '/' holds the note and slides into the next one (808
    slide: the notes overlap a little and a gliding sound glides there). 16, 32 or 64 steps (1, 2 or 4 bars).
  bass: {"pattern", "variation" (bars 5-8, 64 steps; default: the line again, varied, resting before the turn),
    "switch" (a library sound the second Drop switches the bass to, on a « Basse 2 » track), "octave" (the
    root's octave, 1 = sub, 2 = usual), "sound", "glide": true (the bass sound glides: synth portamento, pack
    sample slide), "sub": true (a clean sub layer: a « Sub » track plays the bass's notes an octave down with
    « Sub pur » wherever the bass plays: the jump up growl over its sine)};
    lead: "glide" too (a riff with '/': the rave hoover's dive)
  chords: {"type": pad|stab|keys|none, "rhythm": "X-------x-------" ('X'/'x'/'o' hit strong/softer/soft, '-' held,
    '.' rest; 16, 32 or 64 steps), "voicing" (triad, seventh, ninth, sus2, sus4, power, octave, single, rootless7,
    rootless9), "voice_lead": true (each chord inverted to move as little as possible from the one before),
    "arp": {"pattern": [indices into the chord's notes over two octaves], "gate": 0.25..1, "accent": every n
    notes, "rate": 24|48} (the chords arpeggiated on that fixed pattern: synthwave, trance), "octave", "sound"};
  lead: {"type": riff|hook|chops|arp|none ("hook": a singable line judged by every melody rule, "riff": a short
    loop that may leap and repeat, "chops": vocal chops), "pattern" (the cell: 1, 2 or 4 bars, developed into
    an 8-bar phrase by the app), "answer" (the cell's answer, bars 3-4; default: written by the app), "on_key":
    true (the tokens are degrees of the key, not of each bar's chord: the riff stays put), "arp": {pattern, gate,
    accent, rate} for an arpeggio lead, "arp_rate", "octave", "sound"}
  sounds: a preset of PRESETS, "pack:<pack>:<kind>" (falls back to a preset without the pack: FALLBACK),
    "pack:<pack>:<kind>|<preset>" (the pack's sound first when it is on the phone, else that preset),
    "gm:<General MIDI name>" (a built-in stand-in), "kit:<voice>" (a kit voice played across the keys,
    e.g. "kit:cowbell" for the 808 cowbell melody; falls back to "Cloche").
  "defaults": {"bass": bool, "chords": bool, "lead": bool}: the parts switched on in the assistant.
  "default_mood": dark|happy|future|dreamy: the mood proposed first (default dark).
  "signature": bass|chords|lead: the part that makes the style; it plays in every section of the song.
  "form": song|rap|club|rave: Intro/Couplet/Pré-refrain/Refrain with a Pont, the rap song (its Refrain first, its
    Couplet without chords, the kick and the 808 out 2 bars before each Refrain), or Intro/Groove/Montée/Drop with a
    Break (default from the family: club for House, Techno and Bass music, rap for Hip-hop, else song); rave: the
    303 alone in the Intro, two Breaks, an outro. Techno and bass music last 5 to 6 minutes, the rest about 3.
  "fake_drop": true (club music): a fake drop before the first Drop (riddim): the Drop's beat without its bass,
    a beat of silence, then the real one.
  "rewind": true (club music): the record pulled back (a tape stop on the master) before the second Drop (jungle).
  "vocal": answer|stab|shout|crush: a « Voix » track sung by the built-in voice (« Voix robot »): chops that
    answer the hook in its rests (house, garage, jersey), a stab every 4 bars (trap), an MC's shout in each
    Drop's second half (drum & bass), a crushed hit every 2 bars (phonk).
  "build": true (club music): the Groove becomes a Montée without kick nor bass where the snare (or the clap)
    rolls faster and faster, before each Drop (Intro, Montée, Drop, Break, Montée, Drop, Intro).
  "drop": [layers] (club music): held back until the Drop, e.g. ["bass", "open_hat"]; "break": [layers]: what the
    Break plays (default: the chords and the lead; never the kick). Layers: bass, chords, lead or a drum role.
  "riser": true: a « Montée » track whose noise rises over the 4 bars before each Drop (all the Montée) and hits on
    the Drop's first beat (a song curve opens and shuts its filter); in the song form, the impact alone on the Refrain.
  "counter": {"sound": <preset>, "rhythm": "x..." (default: the chords' rhythm when they are stabs, else 3-3-2 stabs)}:
    a counter-melody track that answers the chords an octave above them in the Drop (or the Refrain), from the
    second half of the first one (the second Drop is fuller).
  "sweeps": [{"on": bass|chords|lead|acid|master, "intro"|"groove"|"drop"|"break": [start, end], "resonance": 0..1}]:
    a low-pass filter on that layer whose cutoff (0..1: 40 Hz to 18 kHz) moves along the song (each section from its
    start to its end value; "groove" is the Montée of the build form; a section left out plays it open; the outro
    runs the intro backwards).
  "bass_tip" / "pad_tip" / "lead_tip": what that part does in this style (French), shown on its step.
  "fx": [{"on": bass|chords|lead|<drum role>|acid|drums|master, "preset": <FX_PRESETS id>, "insert": <EFFECTS name>,
          "params": [up to 4 values 0..1 of the insert, the others its defaults], "reverb": 0..1, "delay": 0..1}]:
          effects put on that layer when the song is made (e.g. {"on": "chords", "insert": "VINYL"},
          {"on": "bass", "preset": "pump"}, {"on": "snare", "reverb": 0.4}); on the master: inserts only (a preset's
          inserts, or one), in its free slots.
  "acid": {"use": true, "pattern": 16 or 32 tokens (a two-bar riff), "accents"/"slides": one 'x' or '.' per step,
          "sound": <a 303 sound of SoundLibrary.ACID>, "knobs": {<knob>: 0..1} set on top of it,
          "moves": {<knob>: {"intro"|"groove"|"drop"|"break": [start, end]}}: the knob turned along the song (an
          automation curve: each section from its start value to its end value; the outro runs the intro backwards)}.
    knobs: cutoff, resonance, env_mod, decay, accent, tuning, waveform, drive, level (moves: not tuning, waveform, level).
  "references": every landmark track, the most recent / best known first (all are shown).
"""
import json
import os
import re
import sys

MODEL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "project-model", "src", "main", "kotlin", "com", "pocketstudio", "model")


def _source(name):
    with open(os.path.join(MODEL, name), encoding="utf-8") as f:
        return f.read()


def _between(text, start, end):
    i = text.index(start)
    return text[i:text.index(end, i)]


def code_lists():
    """The names the app knows, read from its code: a sound, preset or effect added there is accepted here."""
    library = _source("SoundLibrary.kt")
    synth = _between(library, "val SYNTH:", "val ACID:")
    sounds = set(re.findall(r'NamedSound\("([^"]+)"', synth))
    global SOUND_FAMILY
    SOUND_FAMILY = dict(re.findall(r'NamedSound\("([^"]+)", "([^"]+)"', synth))
    acid = set(re.findall(r'NamedSound\("([^"]+)"', _between(library, "val ACID:", "\n    )\n")))
    presets = set(re.findall(r'FxPreset\("([a-z0-9_]+)"', _between(_source("FxPresets.kt"), "val ALL:", "fun byId")))
    enum = _between(_source("Effects.kt"), "enum class EffectType", "\n    ;\n")
    effects = set(re.findall(r"^    ([A-Z][A-Z0-9_]*)\(", enum, re.M)) - {"NONE"}
    return sounds, acid, presets, effects


SOUNDS_IN_CODE, ACID_SOUNDS, PRESETS_IN_CODE, EFFECTS_IN_CODE = code_lists()

NAMES = {
    "boom_bap": ("Boom bap", "Hip-hop"), "trap": ("Trap", "Hip-hop"), "drill": ("Drill", "Hip-hop"),
    "lofi": ("Lo-fi hip-hop", "Hip-hop"), "phonk": ("Phonk", "Hip-hop"), "grime": ("Grime", "Hip-hop"),
    "jersey_club": ("Jersey club", "Hip-hop"),
    "house": ("House", "House"), "deep_house": ("Deep house", "House"), "tech_house": ("Tech house", "House"),
    "uk_garage": ("UK Garage", "House"), "speed_garage": ("Speed garage", "House"), "acid_house": ("Acid house", "House"),
    "afro_house": ("Afro house", "House"), "disco": ("Disco / funk", "House"), "amapiano": ("Amapiano", "House"),
    "techno": ("Techno", "Techno"), "retro_techno": ("Rétro techno", "Techno"), "detroit": ("Techno de Detroit", "Techno"),
    "industrial_techno": ("Techno industrielle", "Techno"), "hard_techno": ("Hard techno", "Techno"),
    "acid_techno": ("Acid techno", "Techno"),
    "gabber": ("Gabber", "Techno"), "acidcore": ("Acidcore", "Techno"), "minimal": ("Minimal", "Techno"), "trance": ("Trance", "Techno"),
    "dnb": ("Drum & bass", "Bass music"), "liquid_dnb": ("Liquid DnB", "Bass music"), "neurofunk": ("Neurofunk", "Bass music"),
    "jungle": ("Jungle", "Bass music"), "jump_up": ("Jump up DnB", "Bass music"), "dubstep": ("Dubstep", "Bass music"),
    "riddim": ("Riddim", "Bass music"),
    "reggaeton": ("Reggaeton", "Autres"), "afrobeats": ("Afrobeats", "Autres"), "synthwave": ("Synthwave", "Autres"),
    "pop": ("Pop", "Autres"),
}
ORDER = list(NAMES)

ROLES = {"kick": "Kick", "snare": "Snare", "clap": "Clap", "hat": "Hat", "open_hat": "OpenHat", "perc": "Perc", "808": "Bass808",
         "tom": "Tom", "ride": "Ride", "perc2": "Perc2", "rumble": "Rumble"}
SCALES = {
    "minor": "Minor", "major": "Major", "dorian": "Dorian", "phrygian": "Phrygian", "minor_pentatonic": "MinorPentatonic",
    "major_pentatonic": "MajorPentatonic", "blues": "Blues", "harmonic_minor": "HarmonicMinor",
}
MOODS = {"dark": "Dark", "happy": "Happy", "future": "Future", "dreamy": "Dreamy"}
VOICINGS = {"triad": "Triad", "seventh": "Seventh", "ninth": "Ninth", "sus2": "Sus2", "sus4": "Sus4", "power": "Power", "octave": "Octave", "single": "Single",
            "rootless7": "Rootless7", "rootless9": "Rootless9"}
LEAD_KINDS = {"riff": "Riff", "hook": "Hook", "chops": "Chops", "arp": "Arp"}
ROOTS = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}
# Synth and Plaits sounds of SoundLibrary.kt (every one, the effects "Sirène" and "Souffle" too).
PRESETS = SOUNDS_IN_CODE
PACKS = set(NAMES) | {"tr808", "tr909", "tr707", "tr606", "cr78", "linndrum", "sp1200", "mpc60", "juno", "jupiter", "dx7",
                      "minimoog", "tb303", "modern_808", "hyperpop", "future_bass", "vocal_chops", "fx"}
KINDS = {"kick", "snare", "clap", "hat", "open_hat", "perc", "808", "bass", "stab", "tom", "pad", "lead", "pluck", "keys", "vocal", "fx"}
# Synth fallback when the pack sound is not on the phone.
FALLBACK = {
    "bass": {"808": "Basse sub", "bass": "Basse ronde"},
    "chords": {"stab": "Hoover", "pad": "Nappe chaude", "keys": "Piano électrique", "pluck": "Pluck", "lead": "Lead"},
    "lead": {"stab": "Hoover", "lead": "Lead", "pluck": "Pluck", "keys": "Piano FM", "vocal": "Voix robot", "pad": "Nappe"},
}
# General MIDI instruments need a downloaded SoundFont: the closest built-in sound stands in.
GM_FALLBACK = {
    "Acoustic Grand Piano": "Piano FM", "Acoustic Guitar (nylon)": "Pluck", "Electric Bass (finger)": "Basse pincée",
    "Electric Guitar (muted)": "Pluck", "String Ensemble 1": "Cordes", "Vibraphone": "Marimba",
}
DEFAULT_PRESET = {"bass": "Basse ronde", "chords": "Nappe chaude", "lead": "Lead"}
PART = {"bass": "Bass", "chords": "Pad", "lead": "Lead"}
DRUM_CHARS = set("Xxo.234")
LINE_CHARS = set("R23457Oro.-6/=")
CHORD_CHARS = set("Xxo-.")
# Built-in kit voices (DrumKit order), for a line's "voice" and a "kit:" sound.
VOICES = {"kick": 0, "kick2": 1, "snare": 2, "snare2": 3, "clap": 4, "hat": 5, "open_hat": 6, "tom_low": 7,
          "tom_mid": 8, "tom_high": 9, "rim": 10, "cowbell": 11, "cymbal": 12, "shaker": 13, "conga": 14, "zap": 15}
# Kit voices that can play a melody (StarterWizard.KIT_MELODIC) and their synth stand-in.
KIT_MELODIC = {"cowbell": "Cloche"}
FX_PRESETS = PRESETS_IN_CODE  # FxPresets.ALL ids
EFFECTS = EFFECTS_IN_CODE  # EffectType names (NONE apart)
FX_TARGETS = {"bass": "bass", "chords": "pad", "lead": "lead", "acid": "acid", "drums": "drums", "master": "master"}
FORMS = {"song": "Song", "rap": "Rap", "club": "Club", "rave": "Rave", "build": "Build"}
VOCALS = {"answer": "Answer", "stab": "Stab", "shout": "Shout", "crush": "Crush"}
CLUB_FAMILIES = {"House", "Techno", "Bass music"}
# The 303's knobs (AcidSound.values order) and those a song curve can turn (AutoTarget.ACID_PARAMS).
ACID_KNOBS = {"cutoff": 0, "resonance": 1, "env_mod": 2, "decay": 3, "accent": 4, "tuning": 5, "waveform": 6, "drive": 7, "level": 8}
ACID_MOVES = {"cutoff", "resonance", "env_mod", "decay", "accent", "drive"}
SECTIONS = {"intro": 0, "groove": 1, "drop": 2, "break": 3}


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def check(cond, msg):
    if not cond:
        raise SystemExit("recipe error: " + msg)


def num(v):
    """[v] as a number, or None (a typo such as "0.5x" gets a recipe error, not a raw ValueError)."""
    if isinstance(v, bool):
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def unit_pair(ends):
    """[start, end], both 0..1."""
    return isinstance(ends, list) and len(ends) == 2 and all(num(v) is not None and 0 <= num(v) <= 1 for v in ends)


def sound_of(gid, part, spec):
    """(synth preset, "pack:kind" / "kit:<voice>" or None)."""
    spec = (spec or "").strip()
    if spec.startswith("kit:"):
        voice = spec[4:]
        check(voice in KIT_MELODIC, f"{gid} {part}: kit voice {voice} cannot play a melody ({sorted(KIT_MELODIC)})")
        return KIT_MELODIC[voice], f"kit:{VOICES[voice]}"
    if spec.startswith("pack:"):
        spec, _, preset = spec.partition("|")
        _, pack, kind = spec.split(":")
        check(pack in PACKS, f"{gid} {part}: unknown pack {pack}")
        check(kind in KINDS, f"{gid} {part}: unknown kind {kind}")
        check(not preset or preset in PRESETS, f"{gid} {part}: unknown fallback sound {preset}")
        # A bass falls back on a bass (a pad sound would not hold the low end); chords and leads may
        # borrow from another family (a hoover as stabs).
        check(not preset or part != "bass" or SOUND_FAMILY.get(preset) == "Basses", f"{gid} bass: fallback {preset} is not a bass")
        return preset or FALLBACK[part].get(kind, DEFAULT_PRESET[part]), f"{pack}:{kind}"
    if spec.startswith("gm:"):
        check(spec[3:] in GM_FALLBACK, f"{gid} {part}: no stand-in for {spec}")
        return GM_FALLBACK[spec[3:]], None
    if spec in PRESETS:
        return spec, None
    check(not spec, f"{gid} {part}: unknown sound {spec}")
    return DEFAULT_PRESET[part], None


def line(pattern, chars, gid, what, lengths=(16, 32, 64)):
    check(len(pattern) in lengths, f"{gid} {what}: {len(pattern)} steps")
    check(set(pattern) <= chars, f"{gid} {what}: bad characters in {pattern}")
    return pattern


def arp_spec(spec, gid, what):
    """An "arp" object as an ArpSpec(...) literal, or None."""
    if not spec:
        return None
    pat = spec.get("pattern")
    check(isinstance(pat, list) and 1 <= len(pat) <= 16 and all(isinstance(v, int) and 0 <= v < 32 for v in pat), f"{gid} {what} arp: pattern = up to 16 indices")
    gate = num(spec.get("gate", 0.6))
    check(gate is not None and 0.25 <= gate <= 1, f"{gid} {what} arp: gate 0.25..1")
    accent = spec.get("accent", 4)
    check(isinstance(accent, int) and 0 <= accent <= 16, f"{gid} {what} arp: accent = every n notes (0: none)")
    rate = spec.get("rate", 24)
    check(rate in (12, 24, 48), f"{gid} {what} arp: rate 12, 24 or 48")
    return f"ArpSpec(listOf({', '.join(map(str, pat))}), gate = {float(gate)}f, accentEvery = {accent}, rate = {rate})"


def style(r):
    gid = r["id"]
    check(gid in NAMES, f"unknown id {gid}")
    name, family = NAMES[gid]
    lines, kit = [], {}
    for d in r["drums"]:
        role = ROLES.get(d["role"])
        check(role is not None, f"{gid}: role {d['role']}")
        pat = line(d["pattern"], DRUM_CHARS, gid, d["role"], lengths=(16, 32))
        check(any(c not in "." for c in pat), f"{gid} {d['role']}: no hit")
        voice = d.get("voice")
        check(voice is None or voice in VOICES, f"{gid} {d['role']}: unknown voice {voice} ({sorted(VOICES)})")
        extra = f", voice = {VOICES[voice]}" if voice is not None else ""
        if d.get("build"):
            line(d["build"], DRUM_CHARS, gid, d["role"] + " build", lengths=(16, 32))
            extra += f", build = {kstr(d['build'])}"
        if d.get("off"):
            extra += ", off = true"
        lines.append(f'                Line(Role.{role}, {kstr(pat)}, {kstr(d["tip_fr"])}{extra}),')
        src = d.get("source", gid)
        check(src in PACKS, f"{gid} {d['role']}: unknown source {src}")
        if src != gid:
            kit[role] = src
    check(lines and r["drums"][0]["role"] in ("kick", "808"), f"{gid}: the kick first")
    roles_used = [d["role"] for d in r["drums"]]
    check(len(set(roles_used)) == len(roles_used), f"{gid}: one line per role")
    check("rumble" not in roles_used or "808" not in roles_used, f"{gid}: the rumble and the 808 are both the low end")
    progs = []
    for mood, p in r["progressions"].items():
        check(mood in MOODS, f"{gid}: mood {mood}")
        check(p["scale"] in SCALES, f"{gid}: scale {p['scale']}")
        check(len(p["degrees"]) == 4 and all(0 <= x <= 6 for x in p["degrees"]), f"{gid}: degrees {p['degrees']}")
        progs.append(f'Mood.{MOODS[mood]} to (Scale.{SCALES[p["scale"]]} to listOf({", ".join(map(str, p["degrees"]))}))')
    c, b, l = r["chords"], r["bass"], r["lead"]
    pad, pad_pack = sound_of(gid, "chords", c.get("sound"))
    bass, bass_pack = sound_of(gid, "bass", b.get("sound"))
    lead, lead_pack = sound_of(gid, "lead", l.get("sound"))
    rhythm = line(c.get("rhythm") or "X---------------", CHORD_CHARS, gid, "chords")
    check(c.get("voicing", "triad") in VOICINGS, f"{gid}: voicing {c.get('voicing')}")
    chord_arp = arp_spec(c.get("arp"), gid, "chords")
    bass_pat = line(b["pattern"], LINE_CHARS, gid, "bass")
    bass_var = line(b["variation"], LINE_CHARS, gid, "bass variation", lengths=(64,)) if b.get("variation") else ""
    bass_switch = (b.get("switch") or "").strip()
    check(not bass_switch or bass_switch in PRESETS and SOUND_FAMILY.get(bass_switch) == "Basses", f"{gid} bass: switch {bass_switch} is not a bass of the library")
    lead_type = l.get("type", "none")
    check(lead_type in ("none",) + tuple(LEAD_KINDS), f"{gid} lead: type {lead_type} (riff, hook, chops, arp, none)")
    lead_pat = line(l["pattern"], LINE_CHARS, gid, "lead") if lead_type in ("riff", "hook", "chops") else ""
    check(lead_type == "none" or lead_type == "arp" or lead_pat, f"{gid} lead: a {lead_type} needs a pattern")
    lead_answer = line(l["answer"], LINE_CHARS, gid, "lead answer", lengths=(16, 32)) if l.get("answer") else ""
    lead_arp = arp_spec(l.get("arp"), gid, "lead") if lead_type == "arp" else None
    check(lead_type != "arp" or lead_arp, f"{gid} lead: an arp needs its \"arp\" pattern")
    lead_on_key = bool(l.get("on_key", False))
    parts = [PART[k] for k, on in r.get("defaults", {}).items() if on and k in PART]
    if c.get("type") == "none" and "Pad" in parts:
        parts.remove("Pad")
    if l.get("type") == "none" and "Lead" in parts:
        parts.remove("Lead")
    packs = {PART[k]: v for k, v in (("bass", bass_pack), ("chords", pad_pack), ("lead", lead_pack)) if v}
    acid = r.get("acid") or {}
    root = ROOTS.get(r["root"])
    check(root is not None, f"{gid}: root {r['root']}")
    mood0 = r.get("default_mood", "dark")
    check(mood0 in MOODS, f"{gid}: default_mood {mood0} ({sorted(MOODS)})")
    check(mood0 in r["progressions"], f"{gid}: default_mood {mood0} has no progression")
    glide = bool(b.get("glide", False))
    if glide and "/" not in bass_pat:
        print(f"note: {gid}: the bass glides but its pattern has no '/' (nothing slides)", file=sys.stderr)
    sub = bool(b.get("sub", False))
    ctr = r.get("counter") or {}
    counter = ctr.get("sound")
    check(counter is None or counter in PRESETS, f"{gid} counter: unknown sound {counter}")
    counter_rhythm = line(ctr["rhythm"], CHORD_CHARS, gid, "counter") if ctr.get("rhythm") else ""
    check(not sub or "Sub pur" in PRESETS, f"{gid} bass: no « Sub pur » sound for the sub layer")
    check(not sub or int(b.get("octave", 2)) >= 2, f"{gid} bass: a sub layer an octave under a bass in octave 1 is inaudible")
    lead_glide = bool(l.get("glide", False))
    check(not lead_glide or "/" in lead_pat, f"{gid}: the lead glides but its riff has no '/' (nothing slides)")
    sig = r.get("signature")
    check(sig is None or sig in PART, f"{gid}: signature {sig} ({sorted(PART)})")
    tips = {}
    for key, part in (("bass_tip", "Bass"), ("pad_tip", "Pad"), ("lead_tip", "Lead")):
        if r.get(key):
            tips[part] = r[key]
    form = r.get("form")
    check(form is None or form in ("song", "rap", "club", "rave"), f"{gid}: form {form} (song, rap, club, rave)")
    club = (form or ("club" if family in CLUB_FAMILIES else "song")) == "club"
    if r.get("build"):
        check(club, f"{gid}: a Montée (build) needs the club form")
        form = "build"

    def layers(key):
        keys = r.get(key)
        if keys is None:
            return None
        check(club or form == "build", f"{gid}: '{key}' needs the club form")
        check(isinstance(keys, list) and keys, f"{gid}: '{key}' is a list of layers")
        roles = [d["role"] for d in r["drums"]]
        lines, parts = [], []
        for k in keys:
            if k in PART:
                parts.append(PART[k])
            else:
                check(k in roles, f"{gid} {key}: {k} is neither a part ({sorted(PART)}) nor a drum line ({roles})")
                check(key != "break" or k != "kick", f"{gid} break: never the kick")
                lines.append(ROLES[k])
        return f"Layers(setOf({', '.join('Role.' + x for x in lines)}), setOf({', '.join('Part.' + x for x in parts)}))"
    drop, brk = layers("drop"), layers("break")
    riser = bool(r.get("riser", False))
    # A song (Intro, Couplet, Refrain) gets the impact alone, on the Refrain's first beat.
    sweeps = []
    for sw in r.get("sweeps", []):
        on = sw.get("on", "")
        target = {"bass": "bass", "chords": "pad", "lead": "lead", "acid": "acid", "master": "master"}.get(on)
        check(target, f"{gid} sweep: unknown target {on} (bass, chords, lead, acid, master)")
        check(on != "acid" or (r.get("acid") or {}).get("use"), f"{gid} sweep: no acid line")
        pairs = []
        for sec, ends in sw.items():
            if sec in ("on", "resonance"):
                continue
            check(sec in SECTIONS and unit_pair(ends), f"{gid} sweep {on} {sec}: [start, end] 0..1")
            pairs.append(f"{SECTIONS[sec]} to ({float(ends[0])}f to {float(ends[1])}f)")
        check(pairs, f"{gid} sweep {on}: no section")
        res = num(sw.get("resonance", 0.25))
        check(res is not None and 0 <= res <= 1, f"{gid} sweep {on}: resonance 0..1")
        sweeps.append(f"Sweep({kstr(target)}, mapOf({', '.join(pairs)})" + (f", resonance = {res}f" if res != 0.25 else "") + ")")
    check(len(sweeps) == len({sw.get("on") for sw in r.get("sweeps", [])}), f"{gid}: one sweep per layer")
    check(drop is None or form != "build", f"{gid}: the Montée already holds the kick and the bass back for the Drop")
    fake_drop = bool(r.get("fake_drop", False))
    rewind = bool(r.get("rewind", False))
    check(not (fake_drop or rewind) or club or form == "build", f"{gid}: a fake drop or a rewind needs the club form")
    vocal = r.get("vocal")
    check(vocal is None or vocal in VOCALS, f"{gid}: vocal {vocal} ({sorted(VOCALS)})")
    fxs = []
    for fx in r.get("fx", []):
        on = fx.get("on", "")
        target = FX_TARGETS.get(on) or (ROLES.get(on) and on)
        check(target, f"{gid} fx: unknown target {on}")
        check(on not in ROLES or any(d["role"] == on for d in r["drums"]), f"{gid} fx: no {on} line")
        preset, insert = fx.get("preset", ""), fx.get("insert", "")
        check(not preset or preset in FX_PRESETS, f"{gid} fx: unknown preset {preset} ({sorted(FX_PRESETS)})")
        check(not insert or insert in EFFECTS, f"{gid} fx: unknown insert {insert} ({sorted(EFFECTS)})")
        params = fx.get("params", [])
        check(not params or (insert and len(params) <= 4 and all(num(v) is not None and 0 <= num(v) <= 1 for v in params)), f"{gid} fx: params 0..1 of an insert")
        check(on != "master" or not (fx.get("reverb") or fx.get("delay")), f"{gid} fx: no sends on the master")
        rev, dly = float(fx.get("reverb", 0)), float(fx.get("delay", 0))
        check(0 <= rev <= 1 and 0 <= dly <= 1, f"{gid} fx: sends 0..1")
        check(preset or insert or rev or dly, f"{gid} fx on {on}: nothing to do")
        args = [kstr(target)] + ([f"preset = {kstr(preset)}"] if preset else []) + ([f"insert = {kstr(insert)}"] if insert else []) + \
            ([f"reverb = {rev}f"] if rev else []) + ([f"delay = {dly}f"] if dly else []) + \
            ([f"params = listOf({', '.join(f'{float(v)}f' for v in params)})"] if params else [])
        fxs.append(f"StyleFx({', '.join(args)})")
    out = [
        "        Style(",
        f"            {kstr(gid)}, {kstr(name)}, {kstr(family)}, {float(r['bpm'])},",
        f"            {kstr(r['description_fr'])},",
        "            listOf(",
        *lines,
        "            ),",
        f"            swing = {float(r.get('swing', 0.0))},",
        "            melody = Melody(",
        f"                root = {root}, mood = Mood.{MOODS.get(mood0, 'Dark')}, bassPattern = {kstr(bass_pat)},",
        f"                bass = {kstr(bass)}, pad = {kstr(pad)}, lead = {kstr(lead)},",
        f"                progressions = mapOf({', '.join(progs)}),",
        f"                chordRhythm = {kstr(rhythm)}, voicing = Voicing.{VOICINGS[c.get('voicing', 'triad')]}, chordOctave = {int(c.get('octave', 4))},",
        f"                bassOctave = {int(b.get('octave', 2))}, leadPattern = {kstr(lead_pat)}, arpRate = {int(l.get('arp_rate') or 24)}, leadOctave = {int(l.get('octave', 5))},",
        f"                packSounds = mapOf({', '.join(f'Part.{k} to {kstr(v)}' for k, v in packs.items())}),",
        f"                parts = setOf({', '.join('Part.' + p for p in parts)}),",
    ]
    if acid.get("use"):
        # One bar or a two-bar riff (the acid line then lasts two bars).
        pat = acid["pattern"]
        check(len(pat) in (16, 32) and set(pat) <= LINE_CHARS, f"{gid} acid: 16 or 32 of {sorted(LINE_CHARS)} expected, got {pat}")
        for what in ("accents", "slides"):
            marks = acid.get(what, "")
            check(marks == "" or (len(marks) == len(pat) and set(marks) <= set("x.")), f"{gid} acid {what}: {len(pat)} of 'x' or '.', got {marks}")
        args = [kstr(pat), kstr(acid.get("accents", "")), kstr(acid.get("slides", ""))]
        sound = acid.get("sound", "")
        check(not sound or sound in ACID_SOUNDS, f"{gid} acid: unknown sound {sound} ({sorted(ACID_SOUNDS)})")
        if sound:
            args.append(f"sound = {kstr(sound)}")
        knobs = acid.get("knobs", {})
        for k, v in knobs.items():
            check(k in ACID_KNOBS and num(v) is not None and 0 <= num(v) <= 1, f"{gid} acid knob {k}: one of {sorted(ACID_KNOBS)}, 0..1")
        if knobs:
            args.append(f"knobs = mapOf({', '.join(f'{ACID_KNOBS[k]} to {float(v)}f' for k, v in knobs.items())})")
        moves = []
        for k, sections in acid.get("moves", {}).items():
            check(k in ACID_MOVES, f"{gid} acid move {k}: one of {sorted(ACID_MOVES)}")
            pairs = []
            for sec, ends in sections.items():
                check(sec in SECTIONS and unit_pair(ends), f"{gid} acid move {k} {sec}: [start, end] 0..1")
                pairs.append(f"{SECTIONS[sec]} to ({float(ends[0])}f to {float(ends[1])}f)")
            moves.append(f"AcidMove({ACID_KNOBS[k]}, mapOf({', '.join(pairs)}))")
        if moves:
            args.append(f"moves = listOf({', '.join(moves)})")
        out.append(f"                acid = AcidRecipe({', '.join(args)}),")
    if sig:
        out.append(f"                signature = Part.{PART[sig]},")
    if tips:
        out.append(f"                tips = mapOf({', '.join(f'Part.{k} to {kstr(v)}' for k, v in tips.items())}),")
    if glide:
        out.append("                bassGlide = true,")
    if lead_glide:
        out.append("                leadGlide = true,")
    if sub:
        out.append("                subBass = true,")
    if counter:
        out.append(f"                counter = {kstr(counter)},")
        if counter_rhythm:
            out.append(f"                counterRhythm = {kstr(counter_rhythm)},")
    if lead_type in LEAD_KINDS and lead_type != "hook":
        out.append(f"                leadKind = LeadKind.{LEAD_KINDS[lead_type]},")
    if lead_on_key:
        out.append("                leadOnKey = true,")
    if lead_answer:
        out.append(f"                leadAnswer = {kstr(lead_answer)},")
    if lead_arp:
        out.append(f"                leadArp = {lead_arp},")
    if bass_var:
        out.append(f"                bassVariation = {kstr(bass_var)},")
    if bass_switch:
        out.append(f"                bassSwitch = {kstr(bass_switch)},")
    if chord_arp:
        out.append(f"                chordArp = {chord_arp},")
    if c.get("voice_lead"):
        out.append("                chordVoiceLead = true,")
    out += [
        "            ),",
        f"            references = listOf({', '.join(kstr(x) for x in r['references'])}),",
        f"            kit = mapOf({', '.join(f'Role.{k} to {kstr(v)}' for k, v in kit.items())}),",
    ]
    if form:
        out.append(f"            form = SongForm.{FORMS[form]},")
    if fxs:
        out.append(f"            effects = listOf({', '.join(fxs)}),")
    if drop:
        out.append(f"            drop = {drop},")
    if brk:
        out.append(f"            breakWith = {brk},")
    if riser:
        out.append("            riser = true,")
    if sweeps:
        out.append(f"            sweeps = listOf({', '.join(sweeps)}),")
    if fake_drop:
        out.append("            fakeDrop = true,")
    if rewind:
        out.append("            rewind = true,")
    if vocal:
        out.append(f"            vocal = Vocal.{VOCALS[vocal]},")
    out.append("        ),")
    return "\n".join(l[4:] if l.startswith("    ") else l for l in out)


def main(out, *paths):
    recipes = {}
    for p in paths:
        for r in json.load(open(p)):
            recipes[r["id"]] = r
    body = [style(recipes[g]) for g in ORDER if g in recipes]
    missing = [g for g in ORDER if g not in recipes]
    text = "\n".join([
        "package com.pocketstudio.model",
        "",
        "import com.pocketstudio.model.StarterWizard.AcidMove",
        "import com.pocketstudio.model.StarterWizard.AcidRecipe",
        "import com.pocketstudio.model.StarterWizard.ArpSpec",
        "import com.pocketstudio.model.StarterWizard.Layers",
        "import com.pocketstudio.model.StarterWizard.LeadKind",
        "import com.pocketstudio.model.StarterWizard.Line",
        "import com.pocketstudio.model.StarterWizard.Melody",
        "import com.pocketstudio.model.StarterWizard.Mood",
        "import com.pocketstudio.model.StarterWizard.Part",
        "import com.pocketstudio.model.StarterWizard.Role",
        "import com.pocketstudio.model.StarterWizard.SongForm",
        "import com.pocketstudio.model.StarterWizard.Style",
        "import com.pocketstudio.model.StarterWizard.StyleFx",
        "import com.pocketstudio.model.StarterWizard.Sweep",
        "import com.pocketstudio.model.StarterWizard.Vocal",
        "",
        "/**",
        " * The assistant's styles, as producers of each genre make them (researched: landmark tracks,",
        " * tempo, drum programming, harmony, bass lines, signature sounds). Generated by",
        " * scripts/packs/recipes_to_kotlin.py from the recipes; edit the recipes, not this file.",
        " */",
        "internal val STYLE_RECIPES: List<Style> = listOf(",
        *body,
        ")",
        "",
    ])
    open(out, "w").write(text)
    print(f"{len(body)} styles written" + (f", missing: {', '.join(missing)}" if missing else ""))


if __name__ == "__main__":
    if sys.argv[1:] == ["--names"]:
        # What the converter accepts, for StyleRecipesUpToDateTest to compare with the app's own lists.
        print(json.dumps({"sounds": sorted(PRESETS), "acid_sounds": sorted(ACID_SOUNDS), "fx_presets": sorted(FX_PRESETS),
                          "effects": sorted(EFFECTS)}))
    else:
        main(*sys.argv[1:])
