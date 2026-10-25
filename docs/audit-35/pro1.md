# Testeur Pro 1, vague 3 : producteur techno mélodique, 124-132 BPM, morceau de 5-6 min (candidat de la PR #47)

Base : `/home/user/ps-audit`, branche `audit` (0aa9bc7 : phases 34 à 39 + lot 1). Lecture du code, des captures (`nav/`, `tablet/`, `tablet8/`, `scale130/`), et **écoute comme le téléphone** : `scripts/live-bench.sh` (37 styles × 90 s, puis 150 s sur les 9 styles dont le Drop arrive tard, dont toute ma famille), `scripts/song-bench.sh --premaster --m4a`, la piste lead rendue seule (`ps_render --solo`), et les extraits de 30 s `scratchpad/audit-35/techno-avant.m4a`, `acid_techno-avant.m4a`, `house-avant.m4a` (avec leurs versions « téléphone »). Chiffres : `docs/audit-35/mesures.md`. Chemins relatifs à `app/src/main/java/com/pocketstudio/app/` sauf mention ; `project-model/...` = `project-model/src/main/kotlin/com/pocketstudio/model/`. Avis formé avant de relire `docs/audit-34/pro1.md`.

Gravité : **grave** (on entend « amateur » ou on ne peut pas finir), **moyen**, **mineur**.

## Parcours réel (gestes comptés, téléphone)

1. **Démarrer** : Nouveau morceau › Techno › Techno (3 gestes). « Ton morceau est prêt ! » : 5 min 20, 132 BPM (`SongShape.kt`, forme `CLUB_LONG`), Intro 32, Groove 24, Montée 8, Drop 32, Break 16, Montée 8, Drop 32, Outro. C'est enfin une forme de techno (vague 2 : 2 min 40).
2. **Écouter** : 1 geste ; le premier Drop arrive à **118 s** en direct (`ps_live`, 150 s). L'Intro démarre à −15,5 dBFS court terme et monte seule à −9,7 en 20 s (gain live, voir pro2 1.1).
3. **Variation du 2e Drop, Fill, Break, Montée, Fin** : **2 gestes** chacun depuis la fiche de partie (`GestureBudgetTest.aVariationIsTwoGestures`, `aFillIsTwoGestures`…). Vague 2 : 9 à 16. Réglé.
4. **Montée de filtre du lead sur 16 mesures** : appui sur le potard › Automatiser = **2 gestes** (`automatingAKnobIsTwoGestures`), 1 point par temps, noms des parties sur l'axe, Lisser, Ligne, Zone (capture `automate_lane.png`). Vague 2 : 11 gestes, et impossible sur un potard du synthé. Réglé.
5. **Piano roll** : toucher = choisir, appui long = effacer (capture `piano_roll.png`), presse-papiers gardé par l'appli pour toutes les pistes et parties (`synth/PianoRoll.kt:87-88`), copier/coller une mesure en 3 gestes. Réglé.
6. **Batterie** : peindre en glissant, roulements ×2 à ×8 montants en 2 gestes (`aRollIsTwoGesturesUpToEightAndRising`), longueur par pad (« Ce pad boucle sur 16 pas »), volume et pan du pad. Réglé.
7. **Geler une piste** : 2 gestes (`freezingATrackIsTwoGestures`). Nouveau.
8. **Exporter** : ⤴ › Partager (2) ; stems : la batterie en un fichier (pareil).

Bilan : le **métier** est là, au nombre de gestes d'un FL Studio Mobile. Ce qui me fait dire « démo amateur » est maintenant **dans le son et dans les notes** que l'assistant écrit.

## Points durs

### 1. Le son de ma famille (Techno, House)

**1.1 [grave] Le lead techno est un « vieux synthé » sombre : centre à 296 Hz, brillance −19,9 dB, boue +12,6 dB.**
- Preuve (lead seul, premier Drop, mesures où il joue, `mesures.md`) : techno centroïde **296 Hz**, présence au téléphone −8,9 dB, brillance (4–10 k / 1–4 k) −19,9, boue (150–500 / 1–4 k) **+12,6** ; tech house 525 Hz, présence **−18,9**, boue +17,4 ; house 563 Hz, −8,2, −18,1, +7,1 ; Detroit 584 Hz, −20,4 de brillance. Cibles (`docs/hits/leads-pros.md` § 8) : brillance −8 à −14, boue ≤ 0, présence ≥ −7. Seuls trance (−12,3) et hard techno (−12,8) ont une brillance de lead dans la cible. Ce que l'extrait `techno-avant.m4a` doit faire entendre à Benjamin (le Drop à 7,3 s) : un blip filtré terne derrière les charleys ; la version téléphone perd 4,5 LU, et la présence 1–4 kHz du Drop techno n'est que −6,0 dB.
- Où : la recette techno joue son « Lead filtré » à l'octave 3 (`project-model/.../StyleRecipes.kt`, techno : `leadOctave = 3`, `lead = "Lead filtré"`) ; sub carré 0,2 par défaut sur toutes les voix (`SynthPatch.kt:106`, `DEFAULTS[5]`) ; doublure à l'octave −1 à 30 % sur les leads des familles Techno, House, Bass music, Autres (`WideMix.kt:46,59`) ; unisson à phases figées (`engine/src/main/cpp/core/Synth.cpp:304`, `Reset(0.137f * (k + 1))`) ; chorus qui somme en mono (`Effects.cpp:335`). C'est exactement le diagnostic de `leads-pros.md` § 3, causes 1, 2, 6 et 12 : rien n'a encore été changé (chantier sur branche, attendu après cet audit).
- Fix : le lot A de `leads-pros.md` (Kotlin seul) : `SUB = 0` et pas d'octave −1 hors basses, SVF à la place du ladder dès qu'il y a de l'unisson, coupures ouvertes, releases 150–400 ms, vibrato 5 Hz ±8 cents ; en techno, lead à l'octave 4 avec la résonance qui respire (modèle Energy Flash, `docs/hits/techno.md` § 1).

**1.2 [grave] Le Drop 2 est le Drop 1 : même scène, même sonie.**
- Preuve (`mesures.md` B) : empreinte d'énergie Drop 2 / Drop 1 sur 8 mesures : techno **0,97**, acid techno 0,99, industrielle 1,00, rétro 0,99, trance 0,97, minimal 0,99, house 0,98, acid house 1,00 ; Δ sonie Drop 2 − Drop 1 : **+0,0 à +0,2 LU**. Seuls hard techno (0,65), Detroit (0,90), tech house (0,91) bougent un peu.
- Où : `project-model/.../SongShape.kt:328-331` (`val drop2 by lazy { sceneOf(...) }` : une seule scène « Chorus2 » rejouée à chaque passage) ; le « mute de 8 mesures » techno (`:332-335`) est le même aux deux Drops.
- Pourquoi : en techno, le 2e Drop est celui qu'on attend (le filtre grand ouvert, une couche de plus, le lead doublé) ; les 32 mesures du Drop 2 rejouent celles du Drop 1 (forme I32 V24 L8 D32 B16 L8 D32 O24, 176 mesures) : c'est la boucle d'un débutant.
- Fix : une scène par passage de Drop (Drop 2 : lead à l'octave ou la 2e couche, ride en croches, basse filtrée plus ouverte ; Drop 3 : tout), et un critère au banc « empreinte Drop 2 / Drop 1 ≤ 0,9 ».

**1.3 [moyen] Le Drop ne monte presque pas au-dessus du Groove en techno et en house.**
- Preuve (`mesures.md` A) : Drop − Groove techno +1,8, Detroit **+0,7**, acid techno +0,9, rétro +1,2, house **+0,6**, acid house +0,5, tech house +1,0 (cible ≥ +2) ; avant master, la médiane est +3,1 : la chaîne en retire 1,3. Drop − Break : trance **+1,6**, acid techno +3,0, minimal +2,9, house +3,9 (cible ≥ +4) ; house, deep house et tech house ont reculé sous +4 depuis S5b.
- Où : la recette techno ne fait arriver au Drop que le charley ouvert et la ride (`StyleRecipes.kt`, techno : `drop = Layers(setOf(Role.OpenHat, Role.Ride), setOf())`) ; le lead ne joue que **16 mesures sur 31** du premier Drop en techno, 9 sur 16 en house, 8 sur 15 en tech house (lead seul, `mesures.md`).
- Fix : un Drop techno = le lead (dès la mesure 1) + le filtre de basse grand ouvert + un crash ; Groove sans ride ; basse +1,5 dB au Drop (prévu en S2, non tenu).

**1.4 [moyen] Étroit, surtout l'acid et la house : côté/centre du Drop −24 à −36 dB.**
- Preuve : acidcore **−35,8**, acid techno **−30,1**, acid house −24,3, house −23,3, rétro −20,5, Detroit −20,2 (cible −15 à −9) ; techno −14,1 et trance −10,9 sont dans la cible.
- Où : la 303 est mono et sans effet de largeur ; aucune piste mélodique n'est pannée (`WizardMix.kt:391-400`, la batterie seule).
- Fix : délai ping-pong 3/16 sur la 303 (c'est le son acid), pluck/nappe en doublure ±0,2, chorus stéréo.

**1.5 [moyen] Au téléphone, la house perd 4,8 LU, la deep house 6,1, l'afro house et l'amapiano 7,1.**
- Preuve : `mesures.md` B ; `house-avant-telephone.m4a` : −4,7 LU contre la version normale. La présence 1–4 kHz de la house −7,8 dB, disco −9,6.
- Fix : basses du chantier (harmoniques 150–800 Hz), lead plus brillant (1.1), un clap/caisse avec une couche 1–3 kHz.

**1.6 [mineur] La forme techno est juste, mais l'écoute « Ton morceau est prêt » joue 2 minutes d'intro** (Drop à 118 s). Voir pro3 n° 1 : « ▶ Le Drop ».

### 2. Les notes et les choix du générateur

**2.1 [moyen] Les riffs passent les 20 règles mais « tombent juste » partout.** `docs/son/MELODIE.md` : notes de l'accord sur les temps forts **1,00** pour tous mes styles sauf la trance (0,94) (hits : 0,55–0,85) ; plage de vélocité **0,46** identique dans 13 styles (même profil 1,0 / 0,82 / 0,68, `MelodyBuilder`) ; techno : progression phrygienne `0,0,0,1` (`StyleRecipes.kt`, techno `Mood.Dark`) = un accord pendant 3 mesures, juste pour le genre, mais le riff `R-..O-..R-......r-..o-..R-..3-..` est rejoué ×8 sur 16 mesures (« Motif ×16 » = 8).
- Fix : appogiatures et 9es tenues (M9 ≤ 0,85 pour les hooks), profil de vélocité par style (techno : accent toutes les 3 doubles croches, polymétrie de 7 pas de `hits/techno.md` § 1), et une note qui change toutes les 8 mesures.

**2.2 [moyen] La nappe et le pluck jouent dans tous les Drops.** `project-model/.../StarterWizard.kt:1548` (`val scenes = mutableListOf(intro, couplet, lines to parts)` : le Drop prend toutes les parties). En techno, le Drop vit de soustraction : le pluck `dub_echo` occupe le bas-médium où le lead est déjà trop chargé (boue du lead seul +12,6 dB).
- Fix : en famille Techno, le pluck sort au Drop 1A et revient au Drop 2 ; sinon passe-haut 300 Hz au Drop.

**2.3 [mineur] Batterie : bien plus vivante.** `DrumHumanizer` (graine fixe), roulements montants, crash au 1, kit v3, kicks accordés (S3) : réglé depuis la vague 2. Reste : une seule boucle de charleys par Drop (empreinte 0,97).

### 3. Les outils

**3.1 [moyen] Une piste = un son.** Pas de couches (`project-model/.../Tracks.kt:17-38`) ; `Track.layers` est sur la branche `g1b`, hors PR #47. Le lead techno pro = scie + carré une octave au-dessus + bruit d'attaque (`leads-pros.md` § 4.8). Fix : fusionner G1b avant le chantier leads.

**3.2 [moyen] Sidechain : une clé pour tout le morceau ; compresseur sans entrée externe ni genou ; 2 envois ; pré-délai en ms.** `Project.kt:545`, `Effects.kt:77-89`, `Effects.kt:449`. Pour une nappe pompée par le clap ou un rumble pompé à part : impossible. Fix : « Écoute : voie » sur compresseur et Pompe, 4 bus, pré-délai au tempo.

**3.3 [mineur] Pendant la lecture, l'éditeur suit le morceau par défaut** (`rhythm/RhythmViewModel.kt:2004`, `_followSong = true`) ; l'option « rester sur ma partie » existe maintenant dans Parties › Réglages du morceau (capture `nav/structure_parts.png`, tablette) : mieux qu'en vague 2, mais le défaut reste celui qui fait sauter l'éditeur sous les doigts.

**3.4 [mineur] Stems de batterie en un seul fichier** (`rhythm/StemExports.kt:37`) : pareil.

### 4. L'interface, téléphone et tablette

**4.1 [moyen] Piano roll coupé sur téléphone, une seule mesure en paysage tablette.** `piano_roll.png` (360-400 dp) : « Note 1/16 1/… », « Basse en fantôm… », « Zoom − » coupés à droite, rien n'indique qu'on peut faire défiler ; `tablet/land_piano_roll.png` et `tablet8/land_piano_roll.png` : barre d'outils sur 3 rangées (≈ 30 % de la hauteur), une mesure visible, couloir de force hors écran.
- Fix : barre d'outils sur une ligne repliable, boucle 4 mesures visible par défaut en paysage.

**4.2 [moyen] La ligne du temps en paysage téléphone montre 3 pistes** (`nav/land_structure.png` : barre du haut, bandeau, barre d'outils de l'espace, en-têtes Parties/Mesures). Sur une techno à 9 pistes, il faut défiler 3 écrans. Fix : en paysage, pistes de 48 dp et barre d'outils dans le rail.

**4.3 [moyen] Sur la tablette 11", la ligne du temps tronque les noms de partie à 3 lettres alors qu'il reste 700 dp** (`tablet/nav_structure.png` : « A · Int », « B · Re », « C · Dr ») et la Galaxy Tab A11 n'a aucune capture des espaces (pro3 n° 8).

**4.4 [mineur] Synthé en paysage tablette** : 80 dp vides entre les pistes et « Clavier / Notes », sons prêts à jouer hors écran (`tablet/land_nav_sons_synth.png`) ; sur la A11, le clavier recouvre « Réglages du son » (`tablet8/land_synth.png`).

### 5. Régressions depuis la vague 2

- Aucune perte de fonction.
- Mesuré : depuis S5b, Drop − Break de la house 4,26 → 3,94, tech house 4,14 → 3,89, deep house 3,96 → 3,64 ; la techno passe au plafond de la chaîne (partie forte −8,32 → −8,57 LUFS) ; Detroit Drop − Groove 0,94 → 0,70. Petits écarts, mais dans le mauvais sens et sans commit de son annoncé : à expliquer avant les chantiers, sinon l'« avant » et l'« après » ne se comparent pas.

## Fonctions manquantes, classées

1. Le timbre : lot A puis lot C de `leads-pros.md` (recettes, puis vraie supersaw et filtre qui suit la note).
2. Une scène par passage de Drop, et un Drop qui ajoute une couche (lead dès la mesure 1, filtre ouvert, crash).
3. Couches par piste (`Track.layers`) avec écoute avec / sans.
4. Largeur : ping-pong sur la 303, pans par rôle, chorus stéréo.
5. Sidechain « Écoute : voie », genou, 4 envois, pré-délai au tempo.
6. Piano roll et ligne du temps adaptés au paysage (téléphone et tablette).
7. Stems de batterie par pad.

## Comparé à la vague 2 (mon Top 5 du 23/10)

| Top 5 vague 2 | Vague 3 | Pourquoi |
|---|---|---|
| 1. Clips par piste : Variation, Fill, Break, Montée, Fin en 1-2 gestes | **réglé** | Fiche de partie, 2 gestes chacun (`GestureBudgetTest`). Mais l'assistant, lui, rejoue toujours la même scène au Drop 2 (1.2). |
| 2. Automation des potards du synthé / Plaits, ¼ de mesure, noms de parties | **réglé** | `AutoTarget.SYNTH` / `PLAITS` (`project-model/.../Automation.kt:27-35,70-76`), 1 point par temps, axe « partie A · Intro », 2 gestes. |
| 3. Un seul transport | **réglé** | Plus de carte de transport dans Rythme ; ▶ en haut seulement. |
| 4. Sonie honnête (gain plafonné, perte en LU, Drop − Break) | **mieux** | Cible sur le Drop, perte ≤ 3 LU, Drop − Break +4,0 ; mais le direct démarre à 0 dB de gain et joue le Drop 1,4 LU sous l'export (pro2 1.1). |
| 5. Piano roll et batterie pro, stems par pad, gel | **mieux** | Toucher = choisir, presse-papiers commun, glissé, roulements ×8, longueur par pad, gel : réglés. Stems de batterie : pareil. |

**Nouveau depuis la vague 2** : formes techno de 5-6 min, Montée qui se vide puis remonte et Drop au-dessus de sa fin (+2,3 LU), kits v3, 808, sidechain au son du kick, réverbe plate, automation des potards, gel de piste, 0 alerte en écoute directe.

## Top 5 maintenant

1. **Le timbre des leads** (lot A de `leads-pros.md`) : en techno, lead à 296 Hz de centre, boue +12,6 dB ; c'est le « vieux synthé » de Benjamin.
2. **Des Drops qui ne se répètent pas** : une scène par passage (`SongShape.kt:328-331`), le lead dès la mesure 1 du Drop, la nappe qui sort.
3. **Le Drop au-dessus du Groove** (+1,8 → ≥ +2, house +0,6) : le Drop ajoute une vraie couche, le Groove en retire une.
4. **Couches par piste** (G1b) puis largeur (ping-pong 303, pans, chorus stéréo).
5. **Paysage** : piano roll et ligne du temps utilisables en paysage sur téléphone et sur la Galaxy Tab A11, avec ses captures.
