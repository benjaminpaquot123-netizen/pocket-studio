# Passe 2 — cas de lecture des noms à rejouer (RAR 3)

Pour le correcteur de `scripts/sons/tout.py`, qui n'a pas les vrais fichiers.
Source : passe 2 sur le RAR 3 (1 pack, style techno, 370 lignes, 0 erreur).
Tous les chemins ci-dessous sont **inventés** : même forme que les vrais noms
(séparateurs, ordre des morceaux, casse, place du tempo et de la note, dossiers
de même sorte), mots du titre remplacés. Seuls les mots génériques utiles au
défaut sont gardés (Soul, Sub Boom, Break, Clap, Kick, Fill, Bass, Loop, MIDI).

Forme des vrais noms : `<Marque>-<X>_<Catégorie en mots>_125_<Titre>_<Note>.ext`
(tempo puis titre puis note, la note est souvent une lettre seule).
MIDI réels : format 0, tpq 96, mesure 4/4, **aucun événement de tempo**.

Chaque chemin listé a été **vérifié** : le pack inventé ci-dessous, passé dans
`tout.py`, sort la valeur fautive indiquée (« aujourd'hui »).

## Rejouer

```sh
F=$(mktemp -d)
python3 -I gen_cas.py "$F"            # le script de la fin de ce fichier
python3 -I scripts/sons/tout.py "$F/Nebulon - Reverie - Melodic Techno" --rar 3 --out "$F/out/f"
# puis lire $F/out/f_<type>.csv ; une ligne : python3 -I scripts/sons/tout.py ... --trace <file_id>
# file_id = sha1("Nebulon - Reverie - Melodic Techno/" + chemin relatif)[:10]
```

Préfixe de tous les noms ci-dessous : `Nebulon-R_` (omis pour la lecture).

## Cas 1 — `bpm_tag` vide alors que le nom porte `_125_`

**Corrigé par 013555d9** (rejoué le 2026-11-26) : `bpm_tag=125` sur les 6 lignes (forme sûre : un nombre 60-200 entre deux `_` dans un nom de boucle ou de MIDI).

Vraies lignes : **55** (47 MIDI à lettre seule, 2 boucles mélodiques à note
tenue, 2 boucles d'ambiance à lettre seule, 4 breaks de batterie).
Cause : un nombre nu ne vaut tempo qu'à côté d'une tonalité forte (dièse/bémol
ou mode) ; sinon il attend la confirmation par le tempo mesuré, qui n'existe
pas pour un MIDI sans tempo ni pour un son sans attaques.

| chemin inventé | aujourd'hui | attendu |
|---|---|---|
| `MIDI/Bass Loops/Bass Synth Loop_125_Lantern_F.mid` | midi `bpm_tag` vide | 125 |
| `MIDI/Bass Loops/Bass Synth Loop_125_Copper_B.mid` | midi `bpm_tag` vide | 125 |
| `MIDI/Melody Loops/Synth Melody Loop_125_Orchid_A.mid` | midi `bpm_tag` vide | 125 |
| `MIDI/Melody Loops/Synth Melody Loop_125_Harbor_G.mid` | midi `bpm_tag` vide | 125 |
| `Loops/Melody Loops/Synth Melody Loop_125_Granite_F.wav` (note tenue) | `bpm_tag` vide, `bpm_est` vide | 125 |
| `Loops/Atmospheric Loops/Atmospheric Loop_125_Meadow_D.wav` | `bpm_tag` vide | 125 |

Témoin (correct aujourd'hui) : `MIDI/Melody Loops/Synth Melody Loop_125_Tundra_Am.mid`
→ `bpm_tag=125`, et `Atmospheric Loop_125_Glacier_Dm.wav` → 125. La seule
différence est la tonalité forte.
Non reproduit : les 4 breaks réels (`Loops/Drum Fills/Drum Fill_125_<Titre>.wav`,
`bpm_est` vide) ; mes breaks synthétiques ont un tempo mesuré, donc 125 sort.

## Cas 2 — lettre seule en fin de nom

**Corrigé par 013555d9** (rejoué le 2026-11-26) : `key_tag` = la tonique seule (A, B, E, G, F), sans mode inventé ; une tonalité forte (`Am`, `Dm`) garde son mode.

Vraies lignes : **20** avec `key_tag` vide (14 MIDI en `_A`, 2 MIDI en `_B`,
4 boucles mélodiques en `_A`) ; **64** lettres C–G forcées en « major »
(31 mélodiques, 31 MIDI, 2 fx) ; **12** boucles mélodiques en A/B dont le mode
vient de la mesure (5 A major, 5 A minor, 1 B major, 1 B minor) : une même
lettre lue tantôt majeure, tantôt mineure.

| chemin inventé | contenu | aujourd'hui | attendu |
|---|---|---|---|
| `MIDI/Melody Loops/Synth Melody Loop_125_Orchid_A.mid` | la mineur | `key_tag` vide | A (mode inconnu ou lu des notes) |
| `MIDI/Bass Loops/Bass Synth Loop_125_Copper_B.mid` | ligne de basse sur B | `key_tag` vide | B |
| `MIDI/Melody Loops/Synth Melody Loop_125_Willow_E.mid` | mi mineur | `E major` | E (pas « major » inventé) |
| `MIDI/Melody Loops/Synth Melody Loop_125_Harbor_G.mid` | sol mineur | `G major` | G |
| `MIDI/Bass Loops/Bass Synth Loop_125_Lantern_F.mid` | F répété | `F major` | F |
| `Loops/Melody Loops/Synth Melody Loop_125_Ember_E.wav` | mi mineur | `E major` | E |
| `Loops/Melody Loops/Synth Melody Loop_125_Velvet_A.wav` | la mineur | `A minor` | même règle que pour E/G |
| `Loops/Melody Loops/Synth Melody Loop_125_Sapphire_A.wav` | la majeur | `A major` | même règle que pour E/G |
| `Loops/Bass Loops/Bass Synth Loop_125_Quarry_B.wav` | si mineur | `B minor` | même règle que pour E/G |

Règle à unifier : A/B attendent la mesure, C–G sont forcés en majeur ; une
lettre seule devrait être traitée pareil quelle que soit la lettre.

## Cas 3 — `note_tag` vide alors que le nom finit par la note

**Corrigé par 013555d9** (rejoué le 2026-11-26) : `note_tag=C` gardé (écart jusqu'à 150 cents) ; l'écart reste dans `cents_gap`. La justesse de `note_measured` (B1 à −60 cents) n'est pas reprise ici.

Vraies lignes : **4** one-shots mélodiques en `_C` sur 20 (notes mesurées
2 × B1, 1 × C2, 1 × C3). Règle en cause : `tout.py` l. 518–520 (note seule
sans octave effacée si la note mesurée diffère).

| chemin inventé | son | aujourd'hui | attendu |
|---|---|---|---|
| `One-Shots/Bass Shots/Bass Synth Shot_Orchid_C.wav` | C2 désaccordé de −100 cents | `note_tag` vide, `note_measured=B1` | C (le nom), écart dans `cents_gap` |
| `One-Shots/Bass Shots/Bass Synth Shot_Willow_C.wav` | C2 à −60 cents | `note_tag` vide, B1 | C |
| `One-Shots/Synth Shots/Synth Shot_Willow_C.wav` | C3 à −60 cents | `note_tag` vide, B2 | C |

Témoins (corrects) : même son à 0, ±30, −45, +45 cents → `note_tag=C`.
Non reproduit : les 2 vraies lignes mesurées C2 et C3 avec `note_tag` vide
(un son pur en C ne les reproduit pas ; à regarder avec `--trace` sur la
note mesurée **avant** l'arrondi de la colonne).

## Cas 4 — « Soul » donne `rnb` dans un pack techno

**Corrigé par 013555d9** (rejoué le 2026-11-26) : `style=techno` (le style du pack l'emporte sur un mot du titre).

Vraie ligne : **1** (un break `Drum Fill_125_Soul.wav`).

| chemin inventé | aujourd'hui | attendu |
|---|---|---|
| `Loops/Drum Fills/Drum Fill_125_Soul.wav` | `style=rnb` | techno (style du pack) |
| `Loops/Drum Loops/Drum Loop_125_Soul_Full.wav` | `style=rnb` | techno |

Témoin : `Drum Fill_125_Lantern.wav`, même son → techno. Un mot de titre ne
doit pas battre le style du pack.

## Cas 5 — boucle d'ambiance rangée en `fx`

**Corrigé par 013555d9** (rejoué le 2026-11-26) : `melodic_loop`, rôle `pad`, `bpm_tag=125` sur les 3 lignes.

Vraies lignes : **15** (tout le dossier `Loops/Atmospheric Loops`, tempo +
note + 4 mesures dans le nom), type `fx`, rôle `fx`.

| chemin inventé | aujourd'hui | attendu |
|---|---|---|
| `Loops/Atmospheric Loops/Atmospheric Loop_125_Meadow_D.wav` | type `fx`, `key_tag=D major`, `bpm_tag` vide | `melodic_loop` (rôle pad), 125 |
| `Loops/Atmospheric Loops/Atmospheric Loop_125_Glacier_Dm.wav` | type `fx`, `bpm_tag=125` | `melodic_loop` |
| `Loops/Atmospheric Loops/Atmospheric Loop_125_Harbor_C.wav` | type `fx`, `C major` | `melodic_loop` |

Son : accord tenu 7,68 s (4 mesures à 125), montée lente, un peu de bruit,
aucune attaque.

## Cas 6 — rôle pris dans un mot du titre

**Corrigé par 013555d9** (rejoué le 2026-11-26) : `role=fx` sur les 3 lignes.

Vraies lignes : **5** `Sub Boom_<Titre>` (dossier FX des sub)
en rôle `bass` ; **1** `Impact FX_Break` (dossier `One-Shots/FX/Impacts`)
en rôle `drums` (« break » est dans les mots de batterie).

| chemin inventé | aujourd'hui | attendu |
|---|---|---|
| `One-Shots/FX/Sub Hits/Sub Boom_Gravelly.wav` | `role=bass` | fx |
| `One-Shots/FX/Sub Hits/Sub Boom_Hollow.wav` | `role=bass` | fx |
| `One-Shots/FX/Impacts/Impact FX_Break.wav` | `role=drums` | fx |

Témoins (corrects aujourd'hui, à garder) : `Impact FX_Breaker.wav`,
`Impact FX_Hatchet.wav`, `Impact FX_Lantern.wav`, `One-Shots/FX/Risers/Riser FX_Kickback.wav`
→ `fx`. Le dossier FX devrait primer sur le titre.

## Cas 7 — boucle d'un seul instrument : trois grilles identiques, tempo moitié

**Non corrigé** : défaut de mesure (séparation des grilles, tempo moitié), pas de lecture du nom ; ni simple ni sûr (voir `docs/sons/REPARATION-OUTIL2.md`).

Vraies lignes : grilles kick = snare = hats sur **33** boucles `..._Clap`
(17) et `..._Kick` (16), plus 1 break et 2 roulements ; `bpm_est` = 62,5
pour 125 sur **18** boucles, toutes `_Clap` (plus 2 `_Top`).

| chemin inventé | son | aujourd'hui | attendu |
|---|---|---|---|
| `Loops/Drum Loops/Drum Loop_125_Lantern_Clap.wav` | clap sur les pas 4 et 12, 4 mesures | `grid_kick=grid_snare=grid_hats=....x.......x...`, `bpm_est=62.5` | kick et hats vides, `bpm_est=125` |
| `Loops/Drum Loops/Drum Loop_125_Copper_Clap.wav` | clap pas 4, 12, 15 | trois grilles `....x.......x..x`, 62.5 | idem |
| `Loops/Drum Fills/Drum Fill_125_Pebble.wav` | roulement de caisse claire, 16 doubles-croches, 1 mesure | `grid_kick=.xxxxxxxxxxxxxxx`, snare et hats pleines | kick vide |
| `Loops/Drum Fills/Drum Fill_125_Lantern.wav` | kick 0, 8 + caisse 4, 12, 14 (pas de charleston) | `grid_hats` = `grid_snare` | hats vide |

Partiel : mes boucles `_Kick` (kick 4 temps) sortent `bpm_est=125` mais une
grille snare `x.x.x.x.x.x.x.x.` sans caisse claire (`Drum Loop_125_Lantern_Kick.wav`,
`Drum Loop_125_Copper_Kick.wav`) ; les vraies `_Kick` ont les trois grilles
égales. Même défaut de fond : une grille par bande sans seuil relatif.

## Cas 8 — MIDI : 3 ou 1 mesure pour une boucle de 4

**Non corrigé** : comptage des mesures MIDI, pas de lecture du nom ; ni simple ni sûr (voir `docs/sons/REPARATION-OUTIL2.md`).

Vraies lignes : 58 × 4 mesures, **25** × 3, **2** × 1 (une seule note
0→384, alors que l'audio jumeau dure 7,68 s = 4 mesures).
Cause : nombre de mesures = fin de la dernière note arrondie, au lieu de la
mesure entamée suivante en puissance de 2 / de l'audio jumeau.

| chemin inventé | notes (début, fin, hauteur), tpq 96 | aujourd'hui | attendu |
|---|---|---|---|
| `MIDI/Bass Loops/Bass Synth Loop_125_Anvil_Cm.mid` | 5 notes C2 (36) : (0,96) (192,288) (384,480) (576,672) (768,864) ; dernière fin 864 (vrais exemples : 960 ou 1200) | `bars=3` | 4 |
| `MIDI/Melody Loops/Synth Melody Loop_125_Beacon_Fm.mid` | 8 notes, départ 72+144·i, fin 144+144·i, hauteurs 65,68,72,63 ; dernière fin 1152 | `bars=3` | 4 |
| `MIDI/Melody Loops/Synth Melody Loop_125_Granite_Fm.mid` | 1 note F2 (41) de 0 à 384 | `bars=1` | 4 (si l'audio jumeau fait 4 mesures) ou au moins pas 1 seul |

Témoin : 16 noires (i·96, i·96+72) → dernière fin 1512 → `bars=4`.

## Cas 9 — one-shot de batterie clap / snare avec `note_measured`

**Corrigé par 013555d9** (rejoué le 2026-11-26) : plus de `note_measured` sur un clap ou une caisse claire ; le kick garde sa note.

Vraies lignes : **12** (2 claps : E6, F#6 ; 10 snares : A#2 à A3). Pour info,
10 kicks et 9 percussions ont aussi une note (sans doute légitime pour un kick).

| chemin inventé | son | aujourd'hui | attendu |
|---|---|---|---|
| `One-Shots/Drum Shots/Claps/Clap_Lantern.wav` | 3 rafales de bruit à 10 ms + 1300 Hz amorti, 0,25 s | `note_measured=E6` | vide |
| `One-Shots/Drum Shots/Snares/Snare_Copper.wav` | bruit + 190 Hz, décroissance 80 ms, 0,25 s | `note_measured=F#3` | vide |
| `One-Shots/Drum Shots/Snares/Snare_Harbor.wav` | idem, 0,4 s | `F#3` | vide |

Témoin : `One-Shots/Drum Shots/Kicks/Kick_Lantern.wav` → G#1 (à garder).

## Cas 10 — boucle mélodique à tempo moitié

**Non corrigé** : choix du tempo moitié/double à la mesure, pas de lecture du nom ; ni simple ni sûr (voir `docs/sons/REPARATION-OUTIL2.md`).

Vraies lignes : **2** boucles mélodiques à `bpm_est=62.5` pour `_125_`
(et 1 à 83,25, 1 à 93,75). Sur le synthétique, une ligne en croches
(7,68 s) donne 62,5 : `Synth Melody Loop_125_Velvet_A.wav`,
`_Sapphire_A.wav`, `_Ember_E.wav`, `Loops/Bass Loops/Bass Synth Loop_125_Quarry_B.wav`.
Attendu : 125 (le tag dit 125 et la durée vaut 4 mesures à 125).
Les boucles réelles sont plus riches que mes croches : ce cas est
surtout un garde-fou de non-régression.

## Script de génération (`gen_cas.py`)

À poser hors du dépôt, puis `python3 -I gen_cas.py <dossier>`. Il faut numpy.

```python
"""Synthetic stand-ins for the RAR 3 name cases."""
import os, struct, sys, wave
import numpy as np
SR = 48000
BEAT = 60 / 125
rng = np.random.default_rng(7)

def wav(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = np.clip(x, -1, 1)
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767 * 0.7).astype("<i2").tobytes())

def hz(m): return 440 * 2 ** ((m - 69) / 12)

def env(n, a=0.002, d=0.25):
    t = np.arange(n) / SR
    return np.minimum(t / a, 1) * np.exp(-t / d)

def tone(m, dur, d=0.25, cents=0.0):
    n = int(dur * SR); t = np.arange(n) / SR; f = hz(m) * 2 ** (cents / 1200)
    return (np.sin(2*np.pi*f*t) + 0.3*np.sin(4*np.pi*f*t)) * env(n, d=d)

def melodic_loop(notes, bars=4):
    """8th-note pluck line at 125 BPM, bars*4 beats long (7.68 s for 4 bars)."""
    x = np.zeros(int(bars * 4 * BEAT * SR))
    step = BEAT / 2
    for i in range(bars * 8):
        m = notes[i % len(notes)]; s = int(i * step * SR)
        y = tone(m, step, d=0.12); x[s:s+len(y)] += y[:len(x)-s]
    return x * 0.5

def pad(notes, bars=4):
    """Sustained chord, slow swell, no attacks (an atmospheric loop)."""
    n = int(bars * 4 * BEAT * SR); t = np.arange(n) / SR
    x = sum(np.sin(2*np.pi*hz(m)*t) for m in notes) / len(notes)
    x += 0.05 * rng.standard_normal(n)
    return x * (0.5 - 0.4*np.cos(2*np.pi*t/(n/SR)))

def hit(kind, n=None):
    n = n or int(0.25 * SR); t = np.arange(n) / SR
    if kind == "kick":
        f = 50 + 120*np.exp(-t/0.03); ph = 2*np.pi*np.cumsum(f)/SR
        return np.sin(ph) * np.exp(-t/0.18)
    if kind == "clap":
        e = sum(np.exp(-np.maximum(t - k*0.01, 0)/0.012) * (t >= k*0.01) for k in range(3))
        return rng.standard_normal(n) * e * 0.6 + 0.4*np.sin(2*np.pi*1300*t)*np.exp(-t/0.05)
    if kind == "snare":
        return (rng.standard_normal(n)*0.6 + np.sin(2*np.pi*190*t)) * np.exp(-t/0.08)
    if kind == "hat":
        y = rng.standard_normal(n); y = np.diff(y, prepend=0); return y*np.exp(-t/0.03)*0.5

def drum_loop(pattern, bars=4):
    """pattern: list of (kind, 16th steps in one bar)."""
    x = np.zeros(int(bars*4*BEAT*SR))
    for b in range(bars):
        for kind, steps in pattern:
            for s in steps:
                p = int((b*16 + s) * BEAT/4 * SR); y = hit(kind); x[p:p+len(y)] += y[:len(x)-p]
    return x * 0.5

def midi(path, notes, tpq=96):
    """SMF format 0, tpq 96, 4/4 time signature, NO tempo event (like the real files).
    notes: (start_tick, end_tick, pitch)."""
    ev = []
    for s, e, p in notes:
        ev += [(s, 1, bytes([0x90, p, 100])), (e, 0, bytes([0x80, p, 0]))]
    ev.sort(key=lambda z: (z[0], z[1]))
    def vlq(v):
        out = [v & 127]; v >>= 7
        while v: out.insert(0, (v & 127) | 128); v >>= 7
        return bytes(out)
    body = b"\x00\xff\x58\x04\x04\x02\x24\x08"; last = 0
    for tk, _, b in ev:
        body += vlq(tk - last) + b; last = tk
    body += b"\x00\xff\x2f\x00"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"MThd" + struct.pack(">IHHH", 6, 0, 1, tpq) + b"MTrk" + struct.pack(">I", len(body)) + body)

def boom(dur=2.0):
    n = int(dur*SR); t = np.arange(n)/SR
    f = 35 + 80*np.exp(-t/0.15); return np.sin(2*np.pi*np.cumsum(f)/SR) * np.exp(-t/0.6)

def impact(dur=2.5):
    n = int(dur*SR); t = np.arange(n)/SR
    return (rng.standard_normal(n)*0.5*np.exp(-t/0.4) + boom(dur)*0.8)

def riser(dur=4.0):
    n = int(dur*SR); t = np.arange(n)/SR
    return rng.standard_normal(n) * (t/dur)**2 * 0.6

PK = os.path.join(sys.argv[1], "Nebulon - Reverie - Melodic Techno")
P = "Nebulon-R_"
def W(rel, x): wav(os.path.join(PK, rel), x)
def M(rel, notes): midi(os.path.join(PK, rel), notes)
BAR = 384
four_bar_bass = [(i*96, i*96+72, 41) for i in range(16)]           # F2 quarters, ends 1512
four_bar_mel_minor = [(i*96, i*96+80, [57,60,64,62][i%4]) for i in range(16)]
four_bar_mel_major = [(i*96, i*96+80, [57,61,64,66][i%4]) for i in range(16)]
e_minor = [(i*96, i*96+80, [52,55,59,57][i%4]) for i in range(16)]
# case 1 / 2 (MIDI, lone letter)
M("MIDI/Bass Loops/"+P+"Bass Synth Loop_125_Lantern_F.mid", four_bar_bass)
M("MIDI/Bass Loops/"+P+"Bass Synth Loop_125_Copper_B.mid", [(s,e,p-6) for s,e,p in four_bar_bass])
M("MIDI/Melody Loops/"+P+"Synth Melody Loop_125_Harbor_G.mid", [(s,e,p-2) for s,e,p in four_bar_mel_minor])
M("MIDI/Melody Loops/"+P+"Synth Melody Loop_125_Orchid_A.mid", four_bar_mel_minor)
M("MIDI/Melody Loops/"+P+"Synth Melody Loop_125_Willow_E.mid", e_minor)
M("MIDI/Melody Loops/"+P+"Synth Melody Loop_125_Tundra_Am.mid", four_bar_mel_minor)  # control
# case 2 audio
W("Loops/Melody Loops/"+P+"Synth Melody Loop_125_Velvet_A.wav", melodic_loop([57,60,64,69,64,60]))
W("Loops/Melody Loops/"+P+"Synth Melody Loop_125_Sapphire_A.wav", melodic_loop([57,61,64,69,64,61]))
W("Loops/Melody Loops/"+P+"Synth Melody Loop_125_Ember_E.wav", melodic_loop([52,55,59,64,59,55]))
W("Loops/Bass Loops/"+P+"Bass Synth Loop_125_Quarry_B.wav", melodic_loop([35,38,42,47,42,38]))
# case 1 audio: sustained note, no attacks
n = int(16*BEAT*SR); t = np.arange(n)/SR
W("Loops/Melody Loops/"+P+"Synth Melody Loop_125_Granite_F.wav", 0.5*np.sin(2*np.pi*hz(53)*t)*np.minimum(t/0.01,1))
W("Loops/Melody Loops/"+P+"Synth Melody Loop_125_Marble_A.wav", 0.5*np.sin(2*np.pi*hz(69)*t)*np.minimum(t/0.01,1))
# case 1 / 5 atmospheric
W("Loops/Atmospheric Loops/"+P+"Atmospheric Loop_125_Meadow_D.wav", pad([50,57,62,65]))
W("Loops/Atmospheric Loops/"+P+"Atmospheric Loop_125_Glacier_Dm.wav", pad([50,57,62,65]))
W("Loops/Atmospheric Loops/"+P+"Atmospheric Loop_125_Harbor_C.wav", pad([48,55,60,64]))
# case 1 / 4 fills (1 bar)
roll = [("snare", list(range(0,16,1)))]
W("Loops/Drum Fills/"+P+"Drum Fill_125_Pebble.wav", drum_loop(roll, bars=1))
W("Loops/Drum Fills/"+P+"Drum Fill_125_Soul.wav", drum_loop([("kick",[0,8]),("snare",[4,12,14])], bars=1))
W("Loops/Drum Fills/"+P+"Drum Fill_125_Lantern.wav", drum_loop([("kick",[0,8]),("snare",[4,12,14])], bars=1))
W("Loops/Drum Loops/"+P+"Drum Loop_125_Soul_Full.wav", drum_loop([("kick",[0,4,8,12]),("clap",[4,12]),("hat",[2,6,10,14])]))
# case 6 FX
W("One-Shots/FX/Sub Hits/"+P+"Sub Boom_Gravelly.wav", boom())
W("One-Shots/FX/Sub Hits/"+P+"Sub Boom_Hollow.wav", boom())
W("One-Shots/FX/Impacts/"+P+"Impact FX_Break.wav", impact())
W("One-Shots/FX/Impacts/"+P+"Impact FX_Breaker.wav", impact())
W("One-Shots/FX/Impacts/"+P+"Impact FX_Hatchet.wav", impact())
W("One-Shots/FX/Impacts/"+P+"Impact FX_Lantern.wav", impact())
W("One-Shots/FX/Risers/"+P+"Riser FX_Kickback.wav", riser())
# case 7 single-instrument loops
W("Loops/Drum Loops/"+P+"Drum Loop_125_Lantern_Clap.wav", drum_loop([("clap",[4,12])]))
W("Loops/Drum Loops/"+P+"Drum Loop_125_Copper_Clap.wav", drum_loop([("clap",[4,12,15])]))
W("Loops/Drum Loops/"+P+"Drum Loop_125_Lantern_Kick.wav", drum_loop([("kick",[0,4,8,12])]))
W("Loops/Drum Loops/"+P+"Drum Loop_125_Copper_Kick.wav", drum_loop([("kick",[0,4,8,10,12])]))
# case 8 MIDI ends early (tpq 96, a 4-bar loop = 1536 ticks)
M("MIDI/Bass Loops/"+P+"Bass Synth Loop_125_Anvil_Cm.mid", [(i*192, i*192+96, 36) for i in range(5)])  # last note ends at 864
M("MIDI/Melody Loops/"+P+"Synth Melody Loop_125_Beacon_Fm.mid", [(72+i*144, 144+i*144, [65,68,72,63][i%4]) for i in range(8)])  # last note ends at 1152
M("MIDI/Melody Loops/"+P+"Synth Melody Loop_125_Granite_Fm.mid", [(0, 384, 41)])  # one whole note, the audio loop is 4 bars
# case 3 one-shots with note letter
for cents, title in [(0,"Lantern"),(-30,"Harbor"),(-45,"Copper"),(-60,"Willow"),(-100,"Orchid"),(30,"Meadow"),(45,"Tundra")]:
    W("One-Shots/Bass Shots/"+P+"Bass Synth Shot_%s_C.wav" % title, tone(36, 1.0, d=0.4, cents=cents))
    W("One-Shots/Synth Shots/"+P+"Synth Shot_%s_C.wav" % title, tone(48, 1.0, d=0.4, cents=cents))
# case 9 drum one-shots
W("One-Shots/Drum Shots/Claps/"+P+"Clap_Lantern.wav", hit("clap"))
W("One-Shots/Drum Shots/Snares/"+P+"Snare_Copper.wav", hit("snare"))
W("One-Shots/Drum Shots/Snares/"+P+"Snare_Harbor.wav", hit("snare", int(0.4*SR)))
W("One-Shots/Drum Shots/Kicks/"+P+"Kick_Lantern.wav", hit("kick"))
```
