#!/usr/bin/env python3
"""Turns the researched genre recipes (JSON, see the brief) into the assistant's styles (Kotlin).

Usage: recipes_to_kotlin.py <out .kt> <recipes.json>...
Every value is checked (patterns, scales, sounds, packs) so that a typo fails here, not in the app.
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
DRUM_CHARS = set("Xxo.")
LINE_CHARS = set("R23457Oro.-6")


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def check(cond, msg):
    if not cond:
        raise SystemExit("recipe error: " + msg)


def sound_of(gid, part, spec):
    """(synth preset, "pack:kind" or None)."""
    spec = (spec or "").strip()
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
        lines.append(f'                Line(Role.{role}, {kstr(pat)}, {kstr(d["tip_fr"])}),')
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
    out += [
        "            ),",
        f"            references = listOf({', '.join(kstr(x) for x in r['references'][:4])}),",
        f"            kit = mapOf({', '.join(f'Role.{k} to {kstr(v)}' for k, v in kit.items())}),",
        "        ),",
    ]
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
        "import com.pocketstudio.model.StarterWizard.Style",
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
