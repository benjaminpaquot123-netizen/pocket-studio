#!/usr/bin/env python3
"""Turns the researched genre recipes (JSON, see the brief) into the assistant's styles (Kotlin).

Usage: recipes_to_kotlin.py <out .kt> <recipes.json>...
  python3 scripts/packs/recipes_to_kotlin.py project-model/src/main/kotlin/com/pocketstudio/model/StyleRecipes.kt scripts/packs/recipes/*.json
Every value is checked (patterns, scales, sounds, packs) so that a typo fails here, not in the app.
A test (StyleRecipesUpToDateTest) fails when StyleRecipes.kt is not what this script makes.

Recipe syntax (one JSON object per style; one character per sixteenth, 16 = a bar, 32 = two bars):
  drums[]: {"role": kick|snare|clap|hat|open_hat|perc|808, "pattern": "X...x...o...2...", "tip_fr": "...",
            "source": <pack the line's sound comes from, default the style's own>,
            "voice": <built-in kit voice when no pack is there, default the pad's own: see VOICES>}
    pattern: 'X' strong, 'x' medium, 'o' soft, '.' nothing, '2' '3' '4' a medium roll of that many hits in the step.
  bass/lead "pattern" (and acid "pattern"): one token per step on the bar's chord: 'R' root, '2'..'7' scale
    steps, 'O' octave up (lower case 'r' 'o' softer), '-' holds the note, '.' silence, '/' holds the note and
    slides into the next one (808 slide: the notes overlap a little and a gliding sound glides there).
  bass: {"pattern", "octave" (the root's octave, 1 = sub, 2 = usual), "sound", "glide": true (the bass sound
    glides: synth portamento, pack sample slide)}
  chords: {"type": pad|stab|keys|none, "rhythm": "X-------x-------" ('X'/'x' hit, '-' held, '.' rest),
    "voicing", "octave", "sound"}; lead: {"type": riff|arp|none, "pattern", "arp_rate", "octave", "sound"}
  sounds: a preset of PRESETS, "pack:<pack>:<kind>" (falls back to a preset without the pack),
    "gm:<General MIDI name>" (a built-in stand-in), "kit:<voice>" (a kit voice played across the keys,
    e.g. "kit:cowbell" for the 808 cowbell melody; falls back to "Cloche").
  "defaults": {"bass": bool, "chords": bool, "lead": bool}: the parts switched on in the assistant.
  "default_mood": dark|happy|future|dreamy: the mood proposed first (default dark).
  "signature": bass|chords|lead: the part that makes the style; it plays in every section of the song.
  "form": song|club: Intro/Couplet/Refrain, or Intro/Groove/Drop with a Break (default from the family:
    club for House, Techno and Bass music).
  "bass_tip" / "pad_tip" / "lead_tip": what that part does in this style (French), shown on its step.
  "fx": [{"on": bass|chords|lead|<drum role>|acid|drums, "preset": <FX_PRESETS id>, "insert": <EFFECTS name>,
          "reverb": 0..1, "delay": 0..1}]: effects put on that layer when the song is made
          (e.g. {"on": "chords", "insert": "VINYL"}, {"on": "bass", "preset": "pump"}, {"on": "snare", "reverb": 0.4}).
  "references": every landmark track, the most recent / best known first (all are shown).
"""
import json
import sys

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
    "gabber": ("Gabber", "Techno"), "minimal": ("Minimal", "Techno"), "trance": ("Trance", "Techno"),
    "dnb": ("Drum & bass", "Bass music"), "liquid_dnb": ("Liquid DnB", "Bass music"), "neurofunk": ("Neurofunk", "Bass music"),
    "jungle": ("Jungle", "Bass music"), "dubstep": ("Dubstep", "Bass music"), "riddim": ("Riddim", "Bass music"),
    "reggaeton": ("Reggaeton", "Autres"), "afrobeats": ("Afrobeats", "Autres"), "synthwave": ("Synthwave", "Autres"),
    "pop": ("Pop", "Autres"),
}
ORDER = list(NAMES)

ROLES = {"kick": "Kick", "snare": "Snare", "clap": "Clap", "hat": "Hat", "open_hat": "OpenHat", "perc": "Perc", "808": "Bass808"}
SCALES = {
    "minor": "Minor", "major": "Major", "dorian": "Dorian", "phrygian": "Phrygian", "minor_pentatonic": "MinorPentatonic",
    "major_pentatonic": "MajorPentatonic", "blues": "Blues", "harmonic_minor": "HarmonicMinor",
}
MOODS = {"dark": "Dark", "happy": "Happy", "future": "Future", "dreamy": "Dreamy"}
VOICINGS = {"triad": "Triad", "seventh": "Seventh", "ninth": "Ninth", "sus2": "Sus2", "sus4": "Sus4", "power": "Power", "octave": "Octave", "single": "Single"}
ROOTS = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}
PRESETS = {
    "Basse ronde", "Basse sub", "Basse reese", "Reese moderne", "Basse analogique", "Basse FM", "Basse pincée",
    "Lead", "Lead carré", "Lead acide", "Supersaw", "Lead trance", "Hoover", "Lead filtré", "Lead chiptune", "Voix robot",
    "Nappe", "Nappe chaude", "Accords future bass", "Cordes", "Nappe d'accords", "Machine à cordes", "Essaim",
    "Pluck", "Pluck future", "Arpège", "Cloche", "Piano FM", "Marimba", "Orgue", "Piano électrique",
    "Basse wobble", "Basse qui parle", "Log drum",
}
PACKS = set(NAMES) | {"tr808", "tr909", "tr707", "tr606", "cr78", "linndrum", "sp1200", "mpc60", "juno", "jupiter", "dx7",
                      "minimoog", "tb303", "modern_808", "hyperpop", "future_bass", "vocal_chops", "fx"}
KINDS = {"kick", "snare", "clap", "hat", "open_hat", "perc", "808", "bass", "stab", "tom", "pad", "lead", "pluck", "keys", "vocal", "fx"}
# Synth fallback when the pack sound is not on the phone.
FALLBACK = {
    "bass": {"808": "Basse sub", "bass": "Basse analogique"},
    "chords": {"stab": "Hoover", "pad": "Nappe chaude", "keys": "Piano électrique", "pluck": "Pluck", "lead": "Lead"},
    "lead": {"stab": "Hoover", "lead": "Lead", "pluck": "Pluck", "keys": "Piano FM", "vocal": "Voix robot", "pad": "Nappe"},
}
# General MIDI instruments need a downloaded SoundFont: the closest built-in sound stands in.
GM_FALLBACK = {
    "Acoustic Grand Piano": "Piano FM", "Acoustic Guitar (nylon)": "Pluck", "Electric Bass (finger)": "Basse pincée",
    "Electric Guitar (muted)": "Pluck", "String Ensemble 1": "Cordes", "Vibraphone": "Marimba",
}
DEFAULT_PRESET = {"bass": "Basse analogique", "chords": "Nappe chaude", "lead": "Lead"}
PART = {"bass": "Bass", "chords": "Pad", "lead": "Lead"}
DRUM_CHARS = set("Xxo.234")
LINE_CHARS = set("R23457Oro.-6/")
# Built-in kit voices (DrumKit order), for a line's "voice" and a "kit:" sound.
VOICES = {"kick": 0, "kick2": 1, "snare": 2, "snare2": 3, "clap": 4, "hat": 5, "open_hat": 6, "tom_low": 7,
          "tom_mid": 8, "tom_high": 9, "rim": 10, "cowbell": 11, "cymbal": 12, "shaker": 13, "conga": 14, "zap": 15}
# Kit voices that can play a melody (StarterWizard.KIT_MELODIC) and their synth stand-in.
KIT_MELODIC = {"cowbell": "Cloche"}
FX_PRESETS = {"underwater", "cathedral", "small_room", "far_away", "space", "dub_echo", "lofi_tape", "warm_vinyl", "radio",
              "phone", "bright", "distortion", "crunch", "robot", "8bit", "dream", "jet", "wobble", "chopper", "pump", "punch"}
EFFECTS = {"EQ", "COMPRESSOR", "SATURATION", "CHORUS", "FILTER", "PUMP", "FLANGER", "PHASER", "GATE", "TREMOLO", "WIDENER",
           "BITCRUSHER", "PARAM_EQ", "ROBOT", "WAH", "VINYL", "RADIO", "CHOPPER"}
FX_TARGETS = {"bass": "bass", "chords": "pad", "lead": "lead", "acid": "acid", "drums": "drums"}
FORMS = {"song": "Song", "club": "Club"}


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def check(cond, msg):
    if not cond:
        raise SystemExit("recipe error: " + msg)


def sound_of(gid, part, spec):
    """(synth preset, "pack:kind" / "kit:<voice>" or None)."""
    spec = (spec or "").strip()
    if spec.startswith("kit:"):
        voice = spec[4:]
        check(voice in KIT_MELODIC, f"{gid} {part}: kit voice {voice} cannot play a melody ({sorted(KIT_MELODIC)})")
        return KIT_MELODIC[voice], f"kit:{VOICES[voice]}"
    if spec.startswith("pack:"):
        _, pack, kind = spec.split(":")
        check(pack in PACKS, f"{gid} {part}: unknown pack {pack}")
        check(kind in KINDS, f"{gid} {part}: unknown kind {kind}")
        return FALLBACK[part].get(kind, DEFAULT_PRESET[part]), f"{pack}:{kind}"
    if spec.startswith("gm:"):
        check(spec[3:] in GM_FALLBACK, f"{gid} {part}: no stand-in for {spec}")
        return GM_FALLBACK[spec[3:]], None
    if spec in PRESETS:
        return spec, None
    check(not spec, f"{gid} {part}: unknown sound {spec}")
    return DEFAULT_PRESET[part], None


def line(pattern, chars, gid, what):
    check(len(pattern) in (16, 32), f"{gid} {what}: {len(pattern)} steps")
    check(set(pattern) <= chars, f"{gid} {what}: bad characters in {pattern}")
    return pattern


def style(r):
    gid = r["id"]
    check(gid in NAMES, f"unknown id {gid}")
    name, family = NAMES[gid]
    lines, kit = [], {}
    for d in r["drums"]:
        role = ROLES.get(d["role"])
        check(role is not None, f"{gid}: role {d['role']}")
        pat = line(d["pattern"], DRUM_CHARS, gid, d["role"])
        check(any(c not in "." for c in pat), f"{gid} {d['role']}: no hit")
        voice = d.get("voice")
        check(voice is None or voice in VOICES, f"{gid} {d['role']}: unknown voice {voice} ({sorted(VOICES)})")
        extra = f", voice = {VOICES[voice]}" if voice is not None else ""
        lines.append(f'                Line(Role.{role}, {kstr(pat)}, {kstr(d["tip_fr"])}{extra}),')
        src = d.get("source", gid)
        check(src in PACKS, f"{gid} {d['role']}: unknown source {src}")
        if src != gid:
            kit[role] = src
    check(lines and r["drums"][0]["role"] in ("kick", "808"), f"{gid}: the kick first")
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
    rhythm = line(c.get("rhythm") or "X---------------", set("Xx-."), gid, "chords")
    check(c.get("voicing", "triad") in VOICINGS, f"{gid}: voicing {c.get('voicing')}")
    bass_pat = line(b["pattern"], LINE_CHARS, gid, "bass")
    lead_pat = line(l["pattern"], LINE_CHARS, gid, "lead") if l.get("type") == "riff" and l.get("pattern") else ""
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
    sig = r.get("signature")
    check(sig is None or sig in PART, f"{gid}: signature {sig} ({sorted(PART)})")
    tips = {}
    for key, part in (("bass_tip", "Bass"), ("pad_tip", "Pad"), ("lead_tip", "Lead")):
        if r.get(key):
            tips[part] = r[key]
    form = r.get("form")
    check(form is None or form in FORMS, f"{gid}: form {form} ({sorted(FORMS)})")
    fxs = []
    for fx in r.get("fx", []):
        on = fx.get("on", "")
        target = FX_TARGETS.get(on) or (ROLES.get(on) and on)
        check(target, f"{gid} fx: unknown target {on}")
        check(on not in ROLES or any(d["role"] == on for d in r["drums"]), f"{gid} fx: no {on} line")
        preset, insert = fx.get("preset", ""), fx.get("insert", "")
        check(not preset or preset in FX_PRESETS, f"{gid} fx: unknown preset {preset}")
        check(not insert or insert in EFFECTS, f"{gid} fx: unknown insert {insert}")
        rev, dly = float(fx.get("reverb", 0)), float(fx.get("delay", 0))
        check(0 <= rev <= 1 and 0 <= dly <= 1, f"{gid} fx: sends 0..1")
        check(preset or insert or rev or dly, f"{gid} fx on {on}: nothing to do")
        args = [kstr(target)] + ([f"preset = {kstr(preset)}"] if preset else []) + ([f"insert = {kstr(insert)}"] if insert else []) + \
            ([f"reverb = {rev}f"] if rev else []) + ([f"delay = {dly}f"] if dly else [])
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
        # The acid track plays one bar: a second one would be dropped without a word.
        pat = acid["pattern"]
        check(len(pat) == 16 and set(pat) <= LINE_CHARS, f"{gid} acid: one bar of {sorted(LINE_CHARS)} expected, got {pat}")
        for what in ("accents", "slides"):
            marks = acid.get(what, "")
            check(marks == "" or (len(marks) == 16 and set(marks) <= set("x.")), f"{gid} acid {what}: 16 of 'x' or '.', got {marks}")
        out.append(f"                acid = AcidRecipe({kstr(pat)}, {kstr(acid.get('accents', ''))}, {kstr(acid.get('slides', ''))}),")
    if sig:
        out.append(f"                signature = Part.{PART[sig]},")
    if tips:
        out.append(f"                tips = mapOf({', '.join(f'Part.{k} to {kstr(v)}' for k, v in tips.items())}),")
    if glide:
        out.append("                bassGlide = true,")
    out += [
        "            ),",
        f"            references = listOf({', '.join(kstr(x) for x in r['references'])}),",
        f"            kit = mapOf({', '.join(f'Role.{k} to {kstr(v)}' for k, v in kit.items())}),",
    ]
    if form:
        out.append(f"            form = SongForm.{FORMS[form]},")
    if fxs:
        out.append(f"            effects = listOf({', '.join(fxs)}),")
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
        "import com.pocketstudio.model.StarterWizard.AcidRecipe",
        "import com.pocketstudio.model.StarterWizard.Line",
        "import com.pocketstudio.model.StarterWizard.Melody",
        "import com.pocketstudio.model.StarterWizard.Mood",
        "import com.pocketstudio.model.StarterWizard.Part",
        "import com.pocketstudio.model.StarterWizard.Role",
        "import com.pocketstudio.model.StarterWizard.SongForm",
        "import com.pocketstudio.model.StarterWizard.Style",
        "import com.pocketstudio.model.StarterWizard.StyleFx",
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
    main(*sys.argv[1:])
