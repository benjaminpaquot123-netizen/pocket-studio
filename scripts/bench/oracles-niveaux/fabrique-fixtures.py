import json, os, struct, wave, io, zipfile, shutil
R = "scripts/bench/oracles-niveaux"
FICHE = {"pas": 16, "rythme": {"kick": [0, 4, 8, 12], "snare": [4, 12], "hat": [0, 2, 4, 6, 8, 10, 12, 14]},
         "basse": [{"pas": 0, "note": 36, "longueur": 2}, {"pas": 6, "note": 36, "longueur": 2},
                   {"pas": 8, "note": 43, "longueur": 2}, {"pas": 12, "note": 41, "longueur": 4}],
         "accords": [{"pas": 0, "notes": [60, 64, 67]}, {"pas": 8, "notes": [57, 60, 64]}],
         "dit": "Tempo 100. Grosse caisse sur les temps 1, 2, 3, 4 ; caisse claire sur 2 et 4 ; charleston sur chaque croche. "
                "Basse : do grave au pas 1, encore do au pas 7, sol au pas 9, fa au pas 13. Accords : do majeur au pas 1, la mineur au pas 9."}

def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    mode = "wb" if isinstance(data, bytes) else "w"
    with open(path, mode, **({} if mode == "wb" else {"encoding": "utf-8"})) as f:
        f.write(data)

def wav(sec=0.05):
    b = io.BytesIO()
    with wave.open(b, "wb") as x:
        x.setnchannels(1); x.setsampwidth(2); x.setframerate(48000)
        x.writeframes(b"\0\0" * int(48000 * sec))
    return b.getvalue()

def lanes(pats):
    out = [{"hits": {}} for _ in range(16)]
    for i, p in enumerate(pats):
        out[i]["hits"] = {str(s): {"velocity": 1.0} for s in p}
    return {"steps": 16, "lanes": out}

def notes(ns):
    return {"steps": 16, "notes": [{"start": s * 24, "length": l * 24, "pitch": p, "velocity": 0.8} for s, p, l in ns]}

def acid(on):
    return {"length": 16, "steps": [{"gate": on and i % 4 == 0} for i in range(16)]}

def project(name, **kw):
    d = {"name": name, "drums": lanes([]), "acid": acid(False), "synthEngine": 0, "synthTrackName": "Synthé",
         "synthNotes": notes([]), "tracks": [], "scenes": [], "song": [], "audioTracks": [], "style": None, "guide": None}
    d.update(kw)
    return d

def seat(mission, case, proj, facts=None, shared=(), extra=None):
    d = os.path.join(R, mission, case)
    shutil.rmtree(d, ignore_errors=True)
    w(f"{d}/start.txt", f"lang=fr\ndevice=phone360\nfont=100\ngestes.max=200\nmission={mission}\nniveau={ {'mon-morceau':2,'boucle-imposee':3}.get(mission,4)}\n")
    w(f"{d}/journal.txt", "#1 ROUTE home | MODE pro\n#1 > touche Nouveau\n#2 ROUTE rhythm | MODE pro\n#2 FIN : fini\n")
    f = {"id": "p1", "name": proj["name"], "open": True, "parts": len(proj["song"]), "seconds": 0, "tracks": len(proj["tracks"])}
    f.update(facts or {})
    w(f"{d}/etat/resume.json", json.dumps({"projects": [f], "open": "p1", "stem_sets": 0, "shared": [n for n, _ in shared],
                                           "prefs": {"simpleMode": False}}, ensure_ascii=False, indent=1))
    w(f"{d}/etat/projects/p1/project.json", json.dumps(proj, ensure_ascii=False, indent=1))
    for n, b in shared:
        w(f"{d}/etat/partage/{n}", b)
    for n, b in (extra or {}).items():
        w(f"{d}/{n}", b)

w(f"{R}/boucle-imposee/fiche.json", json.dumps(FICHE, ensure_ascii=False, indent=1) + "\n")
# mon-morceau
song = dict(style="lofi", guide={"start": "song", "startMarks": {"sound": 1, "notes": 2, "arrange": 3}})
seat("mon-morceau", "tient", project("Nuit", **song), {"changed": {"sound": True, "notes": True, "arrange": True}})
seat("mon-morceau", "ne-tient-pas", project("Nuit", **song), {"changed": {"sound": False, "notes": False, "arrange": False}})
# boucle-imposee: the loop in a saved part; the failing one has the rhythm only (bass a step off, no chords)
P = FICHE["rythme"]
good = {"drums": lanes([P["kick"], [], P["snare"], P["hat"]]), "acid": acid(False),
        "synthNotes": notes([(b["pas"], b["note"] + 12, b["longueur"]) for b in FICHE["basse"]]),
        "trackNotes": {"1": notes([(c["pas"], n, 8) for c in FICHE["accords"] for n in c["notes"]])}}
bad = dict(good, drums=lanes([[0, 8], [4]]), synthNotes=notes([(b["pas"] + 1, b["note"], 2) for b in FICHE["basse"]]), trackNotes={})
tr = [{"id": 1, "name": "Piano", "engine": 0, "notes": notes([])}]
seat("boucle-imposee", "tient", project("Copie", tracks=tr, scenes=[good]))
seat("boucle-imposee", "ne-tient-pas", project("Copie", tracks=tr, scenes=[bad]))
# projet-vide: 6 instruments, 8 parts, a file shared; the failing one: 3 instruments, 2 parts, nothing shared
tr6 = [{"id": i, "name": n, "engine": 0, "notes": notes([(0, 60 + i, 4)])} for i, n in ((1, "Piano"), (2, "Cordes"), (3, "Lead"))]
part = {"drums": lanes([[0, 8]]), "acid": acid(True), "synthNotes": notes([(0, 48, 4)]),
        "trackNotes": {str(i): notes([(0, 60 + i, 4)]) for i in (1, 2, 3)}}
seat("projet-vide", "tient", project("Vide", tracks=tr6, scenes=[part] * 8, song=[{"scene": i} for i in range(8)],
                                     drums=lanes([[0, 8]]), acid=acid(True), synthNotes=notes([(0, 48, 4)])),
     shared=[("Vide.wav", wav())])
seat("projet-vide", "ne-tient-pas", project("Vide", tracks=tr6[:1], scenes=[part] * 2, drums=lanes([[0, 8]])))
# pistes-separees: one WAV per instrument (3), same length, in a .zip; the failing one: 2 WAVs of different lengths
proj3 = project("Trois", tracks=tr6[:1], drums=lanes([[0, 8]]), synthNotes=notes([(0, 48, 4)]))
z = io.BytesIO()
with zipfile.ZipFile(z, "w") as x:
    for n in ("batterie", "synthe", "piano"):
        x.writestr(f"Trois-{n}.wav", wav())
seat("pistes-separees", "tient", proj3, shared=[("Trois-pistes.zip", z.getvalue())])
seat("pistes-separees", "ne-tient-pas", proj3, shared=[("Trois.flac", b"fLaC")])
