# Leads, contre-chants, stabs et nappes « pros » : diagnostic mesuré et plan (2026-10-24)

Point de départ : Benjamin, sur la mélodie DnB : « très très cheap… on dirait un vieux synthé amateur ».
Les notes et les phrases ont été reprises (S1). Ce rapport traite **le timbre** : les sons que jouent le
lead, le contre-chant, les stabs et la nappe des morceaux de l'assistant. On a mesuré ce qui fait « vieux
synthé amateur » et dit comment le corriger, avec des cibles chiffrées.

Branche lue : `restes` (`/home/user/ps-p35`, f53a4a5). Aucun fichier suivi n'a été modifié. Le code du son
(Synth.cpp, WideMix.kt, SoundLibrary.kt, StyleRecipes.kt) n'a pas bougé depuis a36bfec (06:27). Les
enregistrements utilisés (10:59) jouent donc bien le son actuel.

---------------------------------------------------------------------------------------------------------------
## 0. En bref

**Le coupable n'est pas un seul réglage.** Six défauts s'additionnent, et chacun se mesure :

1. **Le son est sombre et boueux, encore plus sur un téléphone.** Le lead Supersaw de la DnB a son centre de
   gravité spectral à **584 Hz** (une scie de lead pro se situe entre 1,5 et 3 kHz). Il a **20 dB de moins**
   entre 4 et 10 kHz qu'entre 1 et 4 kHz (cible −8 à −14 dB), et **6,5 dB de plus** entre 150 et 500 Hz
   qu'entre 1 et 4 kHz. La nappe de la DnB est pire : centre à 432 Hz, et une fois passée par le modèle de
   haut-parleur de téléphone, la bande 1–4 kHz ne pèse plus que **−21,8 dB** du total (une scie de référence :
   −3,3 dB). Trois causes :
   - **un sub carré à 20 % une octave sous chaque note**, hérité du réglage par défaut du synthé, sur tous les
     leads, nappes, plucks, pianos et cloches ;
   - **la « doublure à l'octave −1 » de WideMix**, à 30 %, sur tous les leads de club ;
   - **des coupures de filtre très basses** : 216 Hz pour le Pluck, 600 Hz pour la Nappe, 843 Hz pour le
     Piano électrique, le tout dans un filtre ladder à 24 dB par octave.
2. **Le filtre ne suit pas la note, et la vélocité ne change pas le timbre.** Sans suivi de clavier, le
   Pluck a un centroïde égal à 0,97 × f0 à Do3 et 0,45 × f0 à Do5 : c'est son sub qui domine. À Do7, on
   n'entend plus rien (0,01 × f0). Le rapport de centroïde entre vélocité 1,0 et 0,4 vaut **1,00 sur les
   29 patchs** testés (cible ≥ 1,15). Seul le volume change, de 4,7 dB.
3. **L'unisson « supersaw » est un chœur périodique et figé.** Ses 7 voix sont espacées de façon linéaire
   (±8, ±16, ±24 cents). Elles repartent **toujours des mêmes phases** à chaque note : deux attaques
   successives sont corrélées à **0,94** (référence JP-8000, phases libres : 0,12). Elles battent à un
   rythme fixe de 2,3 Hz, avec 8,7 dB de profondeur. Elles n'ont ni dérive, ni réglage centre/côtés, ni
   passe-haut sur f0. Ce sont exactement les deux pièges que décrivent ceux qui ont étudié la supersaw :
   « oscillateurs libres/aléatoires, le plus important » et « un désaccord linéaire donne des battements
   constants ».
4. **Le filtre ladder salit les sons denses.** Les 7 scies passées par le ladder (même ouvert) laissent
   **−32 à −35 dB** d'énergie non harmonique : de l'intermodulation et du repliement de sa saturation
   interne. Le suréchantillonnage ×2 des oscillateurs n'y change rien. Avec le filtre SVF du même synthé,
   on tombe à **−67 dB**. Une scie seule est propre : −76 dB avec suréchantillonnage.
5. **Aucune attaque, aucune queue, aucune vie dans la note.** Le Supersaw a 0,6 dB d'attaque (rien), une
   release de 46 ms (le Hoover 13 ms, le Pluck 8 ms) : les notes sont coupées net. Les vibratos sont lents
   et larges : Hoover ±20 cents à 0,5 Hz, Lead carré ±20 cents à 1 Hz, Piano électrique ±10 cents à 1,8 Hz.
   C'est le « chevrotement de cassette » des vieux synthés bon marché. Un vibrato pro tourne entre 5 et
   6 Hz, sur 5 à 15 cents, et démarre en retard.
6. **Le cycle de filtre de 8 mesures est à l'envers.** Il est fermé au début de chaque phrase, quand le hook
   entre. Le Pluck liquid a son centroïde à 424 Hz sur la mesure 1 de la phrase et à 840 Hz sur la mesure 5.
   Sur le lead DnB : 426 Hz puis 903 Hz.

Il y a aussi des **mauvais sons choisis**. La DnB n'a pas de voix « pro » pour ses rôles : le piano
électrique est un triangle avec une scie à l'octave, et la cloche est un triangle de 70 ms. Or **le moteur
contient déjà de vraies voix DX7** : les banques 6 opérateurs de Plaits, sous licence MIT, avec E.PIANO 1,
FENDER 1, WINTRHODES, BELL C, VIBE 1, STRINGS, M1 PADS, CS 80, PPG, etc. Elles sont mal branchées :
« Piano FM » joue en fait le patch DX « **BASS 1** » (modèle 2, harmonics 0,2).

**Les 5 changements prioritaires** :
1. **Recettes (Kotlin seul, effort S)** : plus de sub ni d'octave −1 sur les leads, nappes, plucks et
   claviers ; le filtre SVF à la place du ladder dès qu'il y a de l'unisson ; des coupures ouvertes
   (Supersaw 10 kHz, nappe 3 kHz) ; des releases de 150 à 400 ms ; des vibratos à 5 Hz et ±8 cents. Mesuré :
   présence téléphone 1–4 kHz de la nappe de −21,6 à **−3,8 dB**, non-harmoniques du Supersaw de −32 à
   **−67 dB**, Pluck de −36,8 à **−10,3 dB**.
2. **Les vraies voix FM déjà dans le moteur** : le piano liquid sur DX « E.PIANO 1 » ou « WINTRHODES »
   (Plaits modèle 3), le contre-chant sur « VIBE 1 » ou « BELL C », les cordes et nappes sur la banque 3 ;
   « Piano FM » corrigé ; et la vélocité qui agit sur le volume des voix FM.
3. **Moteur : suivi de clavier du filtre et vélocité vers la coupure**, plus une enveloppe de hauteur et un
   clic de bruit pour les plucks et les stabs. Cible : centroïde/f0 de Do3 à Do7 dans un facteur 2,
   rapport de vélocité ≥ 1,15, attaque d'un pluck ≥ 12 dB.
4. **Moteur : une vraie supersaw**, avec les rapports JP-8000, des phases libres (aléatoires mais
   déterministes à l'export), une dérive de ±2 à 4 cents par voix, un mélange centre/côtés et un
   passe-haut sur f0. Plus un chorus stéréo en entrée : l'actuel somme d'abord en mono. Cible : corrélation
   d'une attaque à l'autre ≤ 0,5, aucun battement périodique.
5. **Le cycle de 8 mesures ouvert en début de phrase** (phase décalée, ±0,5 octave au plus sur un lead),
   et **des couches** (Track.layers, en cours) : pluck + nappe, scie + carré une octave au-dessus + clic,
   piano FM + nappe d'air.

---------------------------------------------------------------------------------------------------------------
## 1. Méthode

- **Morceaux réels.** On a utilisé les enregistrements du banc d'écoute en direct
  (`scratchpad/lb/cmds/<style>.pslive`, `LiveSongRecorder`, sons intégrés, aucun pack). Chacun a été rejoué
  par `ps_live`, recompilé depuis `ps-p35` (`scratchpad/leads/tools`), avec **une seule voie du mixeur en
  solo**, la chaîne master et le limiteur coupés (`C 23 1 <voie>`, `C 67 0 2`, `C 67 0 1`, ajoutés juste
  avant le départ du transport). Les inserts et les envois de la voie restent : chorus, ping-pong, réverbe
  plate. Durées : 160 s pour les 4 styles DnB (Intro, Montée, Drop 1, début du Break), 140 s pour les
  autres. Chaque rendu a été mesuré puis effacé.
- **Rôle de chaque piste.** Il a été retrouvé à partir de ses réglages, comparés à `SoundLibrary.kt` et aux
  signatures de WideMix (`scratchpad/leads/py/roles.py`, `roles.json`).
- **Sonde synthé isolée** (`scratchpad/leads/probe/probe.cpp`, `pprobe.cpp` pour Plaits). Elle joue le patch
  exact de chaque piste dans le vrai `ps::Synth` (même chose pour `PlaitsSynth`) : notes Do3/Do5/Do7,
  vélocités 0,4 et 1,0, la même note quatre fois, une note tenue 4 s, et Do6/Do7 avec et sans
  suréchantillonnage.
- **Références « pro »** en numpy (`scratchpad/leads/py/refs.py`) :
  - supersaw JP-8000 : 7 scies aux rapports de Szabo, phases libres, dérive, mélange centre/côtés,
    passe-haut sur f0 ;
  - pluck : 2 scies à ±5 cents et une scie à l'octave, SVF qui suit la note, enveloppe, vélocité, clic de
    4 ms ;
  - piano FM : modulateur « tine » 14:1 qui décroît en 20 ms, corps 1:1, trémolo à 4,5 Hz.
- **Modèle de haut-parleur de téléphone** : passe-haut du 4e ordre à 300 Hz, comme le banc
  « basses qui tapent ». « Présence » = part de la bande 1–4 kHz dans l'énergie qui reste.
- **Métriques par piste** (`stemstats.py`), calculées sur les mesures où la piste joue :
  - centroïde par mesure (et selon la position dans la phrase de 8 mesures) ;
  - « mouvement 8 mesures » : écart-type de la forme du spectre sur 18 bandes, en dB ;
  - présence au téléphone ;
  - « brillance » : 4–10 kHz contre 1–4 kHz ;
  - « boue » : 150–500 Hz contre 1–4 kHz ;
  - côté/centre ;
  - attaque : crête des 30 premières ms contre la médiane de 60 à 200 ms ;
  - ressemblance d'une mesure avec celle 8 mesures plus loin.
- **Non-harmoniques et repliement** (`alias.py`) : énergie hors d'une fenêtre de ±3,5 % autour des
  multiples de f0/2 sous 20 kHz, sur une note tenue de 0,7 s. On mesure le son lui-même : enveloppe tenue,
  LFO et cycle coupés.
- Place disque : chaque rendu fait environ 60 Mo et a été effacé tout de suite (il restait 9 Go de libres).

Limites : il n'y a pas de piste de référence commerciale. Les cibles viennent des voix de référence
synthétisées et des recettes publiées (§ 4). La présence au téléphone n'est qu'un modèle : un vrai
haut-parleur a aussi une bosse de résonance vers 1–3 kHz.

---------------------------------------------------------------------------------------------------------------
## 2. Ce que jouent les morceaux DnB (réglages réels)

| style | rôle | son | source | filtre | ampli | effets (voie) | envois rév./écho |
|---|---|---|---|---|---|---|---|
| dnb | **lead** (riff, oct. 4) | Supersaw | 7 scies ±8/16/24 c (linéaire), sub carré 0,1, octave −1 0,3 | ladder 24 dB **4,6 kHz**, env. +0,5 oct, cycle 8 mesures ±1,2 oct (fermé au début) | A 2 ms, S 0,9, **R 46 ms** | écrêteur, chorus, ping-pong | 0,15 / 0,15 |
| dnb | nappe (oct. 4, 7e) | Nappe | 2 scies à 24 c, **sub carré 0,2** | ladder **600 Hz**, LFO libre ±0,9 oct à 0,17 Hz | A 254 ms, R 595 ms | filtre (montée) | 0,3 / 0 |
| jump_up | lead = 2e basse (oct. 3) | Basse wobble | scie + carré 16 c, **sub 0,7**, octave −1 0,3 | ladder **248 Hz**, balayage 1/8 ±2,7 oct, cycle ±1,2 oct | R 4 ms | distorsion, OTT, chorus, ping-pong | 0,15 / 0,15 |
| jump_up | nappe | Nappe | comme la DnB | | | élargisseur, filtre | 0,35 / 0 |
| liquid_dnb | **nappe = accords** (oct. 3, sans fondamentale 9) | Piano électrique | **triangle** + scie une octave au-dessus (30 %), **sub 0,2** | ladder **843 Hz**, env. +2 oct en 13 ms | D 108 ms, S 0,2, R 20 ms, vibrato ±10 c à 1,8 Hz | auto-pan, chorus, filtre | 0,35 / 0 |
| liquid_dnb | lead (oct. 5) | Pluck | carré + scie 9 c, **sub 0,2**, octave −1 0,3 | ladder **216 Hz** + env. 3,5 oct / 46 ms, rés. 0,5 | D 140 ms, S 0, **R 8 ms** | chorus, ping-pong | 0,15 / 0,3 |
| liquid_dnb | contre-chant | Cloche | 2 triangles (octave), sub 0,2, octave −1 0,3 | 4,6 kHz | D 71 ms, S 0 | chorus, ping-pong | 0,15 / 0,2 |
| neurofunk | lead = réponse (oct. 3) | Basse growl | 3 scies ±12 c + carré, octave −1 0,3 | ladder **348 Hz**, balayage 1/16 | R 3 ms | flanger, ping-pong | 0,15 / 0,15 |

(Sur le téléphone de Benjamin, si le pack « Piano de concert » est installé, la nappe liquid devient ce
piano à queue : un vrai piano, mais pas un Rhodes.)

### Mesures sur les pistes seules (morceau réel, Drop compris)

| piste | centroïde | présence tél. 1–4 kHz | brillance 4–10 k / 1–4 k | boue 150–500 / 1–4 k | côté/centre | attaque | centroïde mesure 1 → 5 de la phrase |
|---|---|---|---|---|---|---|---|
| dnb lead Supersaw | **584 Hz** | −6,9 dB | **−19,8 dB** | **+6,5 dB** | −7,1 | **0,6 dB** | 426 → 903 Hz |
| dnb nappe | **432 Hz** | **−21,8 dB** | **−37,0 dB** | **+20,8 dB** | −6,2 | — | 378 → 486 Hz |
| jump_up lead (wobble) | **177 Hz** | **−17,9 dB** | −23,6 dB | +22,0 dB | −13,1 | 9,0 dB | — |
| jump_up basse growl | 620 Hz | −5,8 dB | −16,6 dB | +3,6 dB | −3,5 | 10,3 dB | — |
| liquid lead Pluck | 637 Hz | −8,3 dB | −22,1 dB | +5,5 dB | −9,3 | 15,8 dB | **424 → 840 Hz** |
| liquid nappe Piano électrique | **417 Hz** | **−14,7 dB** | −25,8 dB | **+15,9 dB** | −6,2 | 6,5 dB | 410 → 464 Hz |
| liquid contre-chant Cloche | 663 Hz | −5,4 dB | −27,0 dB | −0,1 dB | −9,6 | 16,7 dB | — |
| neurofunk lead (growl) | **324 Hz** | **−23,5 dB** | **−38,5 dB** | +25,5 dB | −6,5 | 11,2 dB | 213 → 478 Hz |
| mix dnb (sans master) | 465 Hz | −8,3 dB | −1,6 dB | +9,1 dB | −13,9 | | |
| mix liquid | 422 Hz | −7,6 dB | −0,3 dB | +9,2 dB | −16,2 | | |

Pour lire ce tableau :
- **Les leads et nappes sont plus sombres que le mix entier.** Seuls la batterie et les cymbales donnent de
  la brillance (brillance du mix ≈ 0 dB, des leads −20 à −38 dB). Sur un téléphone, la mélodie paraît « en
  carton » derrière les charleys.
- **En neurofunk et en jump up, la « réponse » au lead est une basse en octave 3 filtrée sous 350 Hz.** Au
  téléphone, il en reste 1–4 kHz à −18 à −23 dB : le jeu question/réponse est inaudible.

### Mesures sur le son seul (sonde, patch exact de l'assistant)

| son (styles) | rapport de vélocité (centroïde 1,0/0,4) | centroïde/f0 Do3 / Do5 / Do7 | même note rejouée (corrélation) | attaque | battement sur note tenue |
|---|---|---|---|---|---|
| Supersaw (dnb, hard techno) | 1,00 | 1,65 / 1,16 / 0,76 | **0,94** | **0,4 dB** | 8,7 dB à **2,3 Hz** (périodique) |
| Nappe (dnb, dubstep, jump up, phonk) | 1,00 | 1,87 / 0,96 / 0,53 | 0,42 | — | 7,6 dB à 3,7 Hz (2 scies à 24 c) |
| Pluck (liquid, pop, reggaeton, techno…) | 1,00 | 0,64 / **0,45** / **0,01** | 0,25 | 31 dB | — |
| Piano électrique (liquid, lo-fi, boom bap, deep house…) | 1,00 | 1,30 / 0,99 / 0,55 | 0,66 | 17,8 dB | — |
| Cloche (liquid, trap, minimal, dubstep…) | 1,00 | 1,21 / 1,21 / 0,89 | 0,21 | 29 dB | — |
| Hoover (jungle, retro, hard, gabber) | 1,00 | 1,22 / 1,02 / 0,52 | 0,00 | 6,9 dB | 10,4 dB à 0,7 Hz (vibrato) |
| Basse growl (neuro, lead) | 1,00 | 1,18 / 0,62 / 0,03 | 0,26 | 4,7 dB | 13,9 dB à 2,7 Hz |
| Lead trance | 1,00 | 1,39 / 1,05 / 0,55 | 0,71 | 9,4 dB | 6,5 dB |
| **Réf. pluck pro** | 1,11 | 1,38 / 1,89 / 1,69 | 0,03 | 15,6 dB | — |
| **Réf. supersaw JP** | — | constant (passe-haut) | **0,12** | 10,6 dB | apériodique |
| **Réf. piano FM** | **1,36** | 1,15 / 1,15 / 1,15 | — | 6,0 dB | trémolo 4,5 Hz voulu |

### Non-harmoniques et repliement (note tenue, son ouvert)

| cas | Do6 sans / avec suréch. | Do7 sans / avec |
|---|---|---|
| scie seule, ladder | −60 / **−77 dB** | −58 / −76 dB |
| carré seul | −66 / −78 | −65 / −77 |
| scie + drive 0,5 | −59 / −71 | −56 / −72 |
| **7 scies (Supersaw), ladder** | **−35 / −35** | **−35 / −35** |
| 7 scies, **SVF** | −71 / **−90** | −54 / −73 |
| 2 scies 9 c, ladder | −68 / −64 | −58 / −58 |
| Supersaw de l'assistant (dnb), tel quel | −32 | |
| … même patch en SVF à 10 kHz, sans sub ni octave −1 | **−67** | |

Ce qu'on en tire :
- **Les oscillateurs vont bien** : suréchantillonnés, ils restent sous −60 dB.
- **C'est le ladder qui salit.** Celui de DaisySP (Huovilainen) sature en interne (`fast_tanh`). Il est
  suréchantillonné ×4 par interpolation linéaire, sans vrai filtre de décimation. Sur une pile de 7 scies,
  il fabrique des sons de différence (34 Hz, 988 Hz…) et du repliement à −35 dB. C'est le « grain sale de
  synthé bon marché » qu'on entend sur le Supersaw et le Hoover.

### Les voix FM déjà présentes (Plaits, MIT) : mesurées

| patch (Plaits) | vélocité | centroïde/f0 Do3/Do5/Do7 | remarque |
|---|---|---|---|
| « Piano FM » actuel = modèle 2, harmonics 0,2 → **DX « BASS 1 »** | 1,26 | 1,34 / 1,08 / 0,70 | un patch de basse, pas un piano |
| DX **E.PIANO 1** (modèle 3, harmonics 0,015) | 1,08 | 2,38 / 1,63 / 1,51 | le Rhodes FM des années 80 : suit le clavier |
| DX **WINTRHODES** (modèle 3, harmonics 0,077) | 1,01 | 3,82 / 2,70 / 1,21 | plus brillant, bien pour du liquid |
| DX **VIBE 1** (modèle 3, harmonics 0,58) | 1,04 | 1,01 constant | vibraphone propre (−85 dB de non-harmoniques) |
| DX **BELL C** (modèle 3, harmonics 0,64) | 1,34 | 3,8 / 2,5 / 2,2 | cloche FM qui suit la vélocité |
| DX **M1 PADS** (modèle 4, harmonics 0,37) | 1,30 | 6,3 / 3,8 / 2,3 | nappe brillante |
| DX STRINGS 2 (modèle 4, harmonics 0,79) | — | 3,3 / 1,5 / 1,9 | attaque lente |

Défaut à corriger : `PlaitsSynth` envoie la vélocité seulement dans « accent ». Pour les modèles déjà
enveloppés (FM 6 opérateurs), le volume ne change donc que de 0,2 dB entre vélocité 0,4 et 1,0 : un piano
insensible au toucher. Il faut multiplier la sortie par la courbe de vélocité, comme le synthé
(0,3 + 0,7 v).

Banques présentes (`third_party/mutable/plaits/resources.cc`) :
- **modèle 2** : SOLID BASS, Mooger Low, ANALOG 4/A/6, CS 80, PPG Vol.1/2, Fairlight, RESONANCES… ;
- **modèle 3** : E.PIANO 1, FENDER 1, WINTRHODES, RS-EP, Mark III, CLAV, PIANO 5, VIBE 1, MARIMBA,
  GLOKENSPL, BELL C, BELLS, TUB BELLS… ;
- **modèle 4** : Hammond 1, E.ORGAN 3, ICE PAD 2, M1 PADS, Etherial, VANGELIS 1, STRINGS 2/3/7, FULL
  STRIN, SYN-ORCH, BRASS 1, BRASS 6…

Index d'un patch : harmonics = (i + 0,5) / 32 / 1,02.

---------------------------------------------------------------------------------------------------------------
## 3. Les causes du « vieux synthé amateur », une par une

| # | défaut | mesure actuelle | cible | d'où il vient |
|---|---|---|---|---|
| 1 | Sub carré sous les leads et nappes | Pluck à Do5 : centroïde 0,45 × f0 ; nappe dnb : boue +20,8 dB | sub = 0 hors basses ; boue ≤ 0 dB (lead), ≤ +6 dB (nappe) | `SynthPatch.DEFAULTS[SUB] = 0,2`, jamais remis à 0 dans les presets non-basses |
| 2 | Octave −1 de WideMix sur tous les leads de club | +3 à +6 dB de 150–500 Hz | 0 sur plucks et claviers, ≤ 0,15 passe-haut 150 Hz ailleurs | `WideMix.lead()` : `OCTAVE_DOWN 0.3` si famille Techno, House, Bass music, Autres |
| 3 | Coupures basses, ladder 24 dB | brillance −20 à −38 dB | −8 à −14 dB (lead), −12 à −20 dB (nappe) | presets : 216 Hz, 600 Hz, 843 Hz… |
| 4 | Pas de suivi de clavier | Pluck Do7 = 0,01 × f0 | centroïde/f0 de Do3 à Do7 dans un facteur 2 | `Synth.cpp` : `cutoffHz` fixe |
| 5 | Vélocité qui ne change que le volume | rapport 1,00 (29 patchs) | ≥ 1,15 (plucks, claviers, leads) | `gain = amp * (0.3 + 0.7 v)` uniquement |
| 6 | Unisson linéaire, phases figées | corrélation 0,94, battement 2,3 Hz | ≤ 0,5, battements apériodiques | `onNote` : `Reset(0.137 k)` ; positions `k/2+1` linéaires |
| 7 | Ladder sur les piles denses | −32 à −35 dB de non-harmoniques | ≤ −60 dB | DaisySP `LadderFilter` (saturation interne, suréch. linéaire) |
| 8 | Releases de 3 à 46 ms | notes coupées net | lead 150–400 ms, pluck 200–300 ms, nappe ≥ 600 ms | presets (`A_RELEASE` 0,12–0,45) |
| 9 | Vibratos lents et larges | ±20 c à 0,5–1 Hz | 5–6 Hz, 5–15 c, départ retardé de 200–400 ms (ou aucun) | `LFO1_RATE` 0,4–0,6, un seul LFO pour toutes les voix, sans délai |
| 10 | Cycle de 8 mesures fermé au début de la phrase | ×0,5 sur la mesure 1 | mesure 1 ≥ 0,9 × médiane ; ±0,5 oct sur un lead | `cycle = -cos(2π phase)` à partir du début du morceau |
| 11 | Aucune attaque | Supersaw 0,6 dB | lead ≥ 3 dB, pluck ≥ 12 dB et brillance d'attaque ≥ 1,5 | pas d'enveloppe de hauteur, pas de clic, phases fixes |
| 12 | Chorus qui somme en mono | l'unisson large repasse par un chorus d'entrée mono | chorus stéréo en entrée, côté conservé à ±1 dB | `StereoChorus::process` : `chorus_.Process(0.5f * (l + r))` |
| 13 | Mauvaise voix pour le rôle | piano liquid = triangle, cloche = triangle de 70 ms, « Piano FM » = BASS 1 | une voix FM, échantillonnée ou en couches selon le rôle (§ 4) | `SoundLibrary`, `StyleRecipes` |
| 14 | Piano FM insensible au toucher | 0,2 dB entre v = 0,4 et 1,0 | ≥ 4 dB | `PlaitsSynth` : vélocité → accent seulement |

Ce qui va bien et ne doit pas bouger :
- les oscillateurs suréchantillonnés ;
- le drive suréchantillonné ×4 ;
- la largeur stéréo de l'unisson (côté/centre −6 à −9 dB sur les leads, dans la cible S4) ;
- l'écho ping-pong ;
- la réverbe plate (Clouds) ;
- le mouvement d'ensemble sur 8 mesures (forme du spectre : 5,6 dB sur le lead DnB, il existe bel et bien,
  mais dans le mauvais sens).

---------------------------------------------------------------------------------------------------------------
## 4. Recettes pros par famille, dans les termes de notre moteur

Notation : « actuel = paramètre existant » ; **[nouveau]** = ce que le moteur doit ajouter (§ 6).
Coupures en Hz. Pour le paramètre CUTOFF : f = 20 × 900^v (0,5 → 600 Hz, 0,7 → 2,3 kHz, 0,8 → 4,6 kHz,
0,93 → 11 kHz). Temps d'enveloppe : 0,001 × 5000^v s (0,6 → 166 ms, 0,65 → 254 ms, 0,7 → 388 ms). Vibrato
en cents = VIBRATO × 200. LFO1 en Hz = 0,05 × 400^v (0,77 → 5 Hz).

### 4.1 Supersaw (riff DnB façon Pendulum, trance, hard techno)
- Source : 7 scies. Écarts relatifs du JP-8000 (Szabo) : −0,110, −0,063, −0,020, 0, +0,020, +0,062,
  +0,107, multipliés par le réglage de désaccord **[nouveau : courbe JP]**. Pour un lead : extérieures à
  **±25–35 cents**, médianes à 57 % de cette valeur, intérieures à 18 % (pas ±8/16/24 linéaires).
- **Phases libres et aléatoires à chaque note [nouveau]**, tirées d'un générateur déterministe (xorshift
  qui repart avec le transport, comme le bruit) pour que l'export reste identique octet pour octet.
  **Dérive** lente de ±2–4 cents par voix **[nouveau]**.
- Mélange centre/côtés **[nouveau]** : à mi-course, centre ≈ 0,72 et côtés ≈ 0,5 (courbes de Szabo :
  centre = −0,554 m + 0,998 ; côtés = −0,738 m² + 1,284 m + 0,044). Passe-haut 12 dB sur f0 **[nouveau]** :
  la supersaw ne descend pas sous sa note.
- SUB 0, OCTAVE_DOWN 0. L'octave basse, si on la veut, va dans une couche à part passée au passe-haut à
  150 Hz.
- Filtre : **SVF passe-bas** (FILTER_TYPE 0,3) à 8–11 kHz (CUTOFF 0,90–0,93), suivi de clavier 50 %
  **[nouveau]**, enveloppe +1 octave en 300 ms, vélocité → +1 octave **[nouveau]**.
- Ampli : A 2–5 ms, D 400 ms, S 0,8, **R 250–350 ms** (A_RELEASE 0,63–0,67).
- Effets : chorus stéréo léger ou aucun (l'unisson est déjà large) ; ping-pong en 3/16 à 15–20 % de
  retour, filtré 300 Hz–6 kHz ; plate de 1,8 s envoyée à 20–25 % ; pompe de −3 à −5 dB sur le kick ; EQ
  coupe-bas 150 Hz, +2 dB vers 3 kHz, +1,5 dB en plateau à 10 kHz ; un peu de saturation de bande avant la
  réverbe.
- Couches (Track.layers) : 1) la supersaw ; 2) un carré ou une pulse une octave au-dessus à −12 dB (le
  « mordant ») ; 3) un clic de bruit de 3–5 ms passé au passe-haut à 2 kHz, à −18 dB.
- Cibles : présence téléphone ≥ −5 dB, brillance −8 à −12 dB, boue ≤ 0 dB, corrélation d'une attaque à
  l'autre ≤ 0,5, non-harmoniques ≤ −60 dB.

### 4.2 Hoover (jungle, jump up, retro et hard techno)
- Origine : « What the » de l'Alpha Juno. 3 oscillateurs **pulse-scie avec PWM [nouveau : PWM]**, deux
  désaccordés de ±40 cents en sens opposés, le troisième à −12 demi-tons (recette MusicRadar). Notre
  Hoover garde l'unisson 7 × ±41 c et le carré à −1 octave, mais **sans PWM ni chorus de Juno**.
- Le geste : la note plonge. Enveloppe de hauteur **[nouveau]** de quelques demi-tons sur 100–250 ms, et
  le glissé de fin (GLIDE déjà là, `leadGlide`).
- Filtre SVF à 3 kHz, résonance faible, drive 0,3. **Chorus de Juno** (2 lignes à 1,5–5 ms, triangle à
  0,5 et 0,8 Hz, sortie droite inversée) **[nouveau]**. Vibrato à 0 : la largeur vient du chorus, pas d'un
  LFO à 0,5 Hz.
- R 200–400 ms. Plate courte et écho en 1/8 pointé.

### 4.3 Reese en lead ou en stab (DnB, jump up, neuro)
- 2 scies à ±10–27 cents (la reese de Saunderson) et 4–8 voix d'unisson. Passe-bas 24 dB à 0,8–1,5 kHz
  qui suit le clavier, avec enveloppe. **Distorsion avant le filtre [nouveau : drive pré-filtre]**, puis
  OTT, puis EQ.
- **Pour un stab qu'on entend au téléphone** : la note en octave 4–5, pas 3 ; couche médium passe-haut à
  300 Hz ; présence 1–4 kHz ≥ −8 dB ; durée 100–250 ms, R 80 ms.

### 4.4 Stab wobble ou growl (jump up, la « 2e basse » qui répond)
- Garder le balayage synchronisé (1/8, 1/4 triplet) et le formant. Mais **la réponse doit vivre entre 300
  et 3 000 Hz** : octave 3 + 12, ou une couche une octave au-dessus passée au passe-haut à 300 Hz.
  Coupure de base ≥ 600 Hz (pas 248), balayage ±1,5 octave (pas ±2,7).
- Distorsion avant le filtre, OTT après, puis un passe-haut à 150 Hz (le sub garde le bas). Le jump up
  moderne (Kanine, Dimension) empile « 2 womps + lead + sub épais » : c'est le travail de Track.layers.

### 4.5 Sirène (jungle, dub, jump up)
- Sinus ou carré dont la hauteur suit un LFO lent puis rapide (0,5 → 8 Hz) **[nouveau : LFO → hauteur
  avec rampe, ou automation de LFO1_RATE]**. Écho en 1/4 pointé, retour 60 %, ressort ou plate courte.
  Le préset « Sirène » actuel balaie le filtre, pas la hauteur.

### 4.6 Piano électrique liquid (et lo-fi, deep house, garage, amapiano)
- **Voix** : DX « E.PIANO 1 » ou « WINTRHODES » (Plaits modèle 3, harmonics 0,015 / 0,077), déjà dans le
  moteur. Ou l'échantillon CC0 « TX81Z FM Piano » de VCSL (§ 7). Le triangle actuel est à abandonner.
- **Vélocité 0,45–0,9 qui change le timbre** : une note forte « aboie » (barks). Le modèle FM le fait
  (1,08 en centroïde) si on ajoute la vélocité au volume.
- **Trémolo/auto-pan stéréo à 4–5 Hz, 20–30 %** (AUTO_PAN déjà là, synchronisé au tempo : en croches, soit 5,8 Hz à 174 BPM),
  chorus doux, plate de 2,5 s à 30–35 %, coupe-bas à 150 Hz sous la basse. **Pas de vibrato.**
- Couche (Track.layers) : une « nappe d'air » (bruit filtré 6–10 kHz, ou nappe à 5 voix à 2–3 kHz) à
  −18 dB derrière le piano. Conseil des forums liquid : un Rhodes derrière la nappe.
- Cibles : présence téléphone ≥ −10 dB (actuel −14,7), boue ≤ +6 dB (actuel +15,9), rapport de vélocité
  ≥ 1,15, au moins 4 dB de volume entre v = 0,45 et 0,9.

### 4.7 Nappe (toutes familles)
- Couche A : 5 scies, extérieures à ±12–18 c, phases libres, SVF passe-bas à 2–3 kHz (CUTOFF 0,7–0,75)
  qui suit le clavier à 30 %, A 300–600 ms, R 1–1,5 s.
- Couche B : de l'air (bruit filtré 6–10 kHz, 2e oscillateur à l'octave) à −20 dB.
- SUB 0. Cycle de 8 mesures à ±0,4 octave, ouvert sur la mesure 1. Chorus stéréo, plate longue. La pompe
  existe déjà.
- Liquid et pop : DX « M1 PADS », « ICE PAD 2 », « Etherial » ; cordes : l'instrument VSCO « Cordes »
  installé, ou DX « STRINGS 2 ».
- Cibles : présence ≥ −12 dB (actuel −21,8), brillance −12 à −20 dB (actuel −37), mouvement 8 mesures
  ≥ 2,5 dB (actuel 1,7–2,8).

### 4.8 Pluck (liquid, trance, pop, reggaeton, techno)
- 2 scies à ±5–7 c et une scie ou un carré à l'octave à −8 dB, phases libres. SUB 0, OCTAVE_DOWN 0.
- SVF passe-bas **qui suit le clavier à 100 % [nouveau]** (coupure de base ≈ 1,5 × f0), enveloppe
  +3,5 octaves avec un declin de 120–250 ms, **vélocité → +1 octave [nouveau]**.
- **Clic** de bruit de 3–5 ms passé au passe-haut à 2 kHz **[nouveau]** ; **enveloppe de hauteur** de +12
  demi-tons sur 5–10 ms pour le « tick » **[nouveau]**.
- Ampli : D 300–600 ms (A_DECAY 0,67–0,75), S 0, R 250 ms.
- Écho ping-pong 3/16 à 25 %, plate à 25 %. Couche « pluck + nappe » : la même note en nappe douce à
  −14 dB (Track.layers).
- Cibles : centroïde/f0 de Do3 à Do7 entre 1,2 et 2,5 (actuel 0,64 → 0,01), attaque ≥ 12 dB, brillance
  d'attaque ≥ 1,5.

### 4.9 Cloche, mailloche, contre-chant (liquid, trap, dubstep, minimal, detroit)
- DX « VIBE 1 » (propre, −85 dB), « BELL C » (sensible à la vélocité, 1,34), « TUB BELLS » ; Plaits
  modal (« Cloche (Plaits) ») pour le sombre ; marimba et glockenspiel VCSL (pack « Percussions
  mélodiques », CC0, déjà publié).
- Réverbe longue (plate 2,5 s à 30 %) et écho en 1/4 pointé : une cloche sèche de 70 ms sonne comme un
  jouet.

### 4.10 Stabs FM et formant métalliques (neurofunk)
- FM 2 opérateurs à rapport non entier (1:1,41, 1:2,76, 1:3,5) avec un indice qui décroît en 50–150 ms,
  puis formant (VOWEL qui bouge), flanger ou filtre en peigne, distorsion, OTT, passe-haut à 200 Hz.
- Dans le moteur aujourd'hui : Plaits modèle 10 (« FM 2 op. »), 1 (distorsion de phase), 11 (formants),
  5 (« terrain »). Demain : FM par voix dans le synthé **[nouveau, DaisySP `Fm2`, MIT]**.
- Octave 4–5, durée 80–200 ms, 2 à 4 notes en réponse aux silences de la basse. Le lead « Basse growl »
  en octave 3, filtré sous 350 Hz, n'est pas une réponse audible (présence −23,5 dB).

### 4.11 Ailleurs (bref)
- **Orgue house et garage (M1)** : DX « E.ORGAN 3 » ou orgue à tirettes ; actuellement 2 carrés + sub 0,3
  avec R 2 ms. Mettre R 80–150 ms, sub 0, chorus et réverbe courte.
- **Synthwave** : brass Jupiter (scies, enveloppe filtre en 200 ms, chorus II), lead carré avec un vibrato
  retardé à 5 Hz, arpèges avec écho.
- **Trap, drill** : cloches DX ou Plaits modal ; piano Salamander installé ; cordes VSCO.
- **Afro, amapiano** : log drum (Plaits, bien) ; marimba et kalimba VCSL ; piano Splendid.
- **Techno** : stabs d'accords avec une longue réverbe et un écho, cycle de filtre ; le « Lead filtré »
  (Plaits) est correct.
- **Trance** : supersaw JP + pluck. Dans la recette trance, le Supersaw de la nappe n'est même pas
  suréchantillonné : WideMix.pad saute le suréchantillonnage dès que l'unisson dépasse 3 copies (pour le
  CPU). Avec un filtre SVF, la nappe restera propre quand même (−54 à −71 dB sans suréchantillonnage).

---------------------------------------------------------------------------------------------------------------
## 5. Tableau par style

« Lead », « nappe » et « contre-chant » viennent des recettes. Le diagnostic vient des réglages
(`scratchpad/leads/summary.md`), de la sonde et, quand la piste a été rendue, des mesures sur la piste
seule (annexe A).

| style | lead | contre-chant | nappe | défaut principal | remplacement proposé |
|---|---|---|---|---|---|
| **dnb** | Supersaw oct. 4 | — | Nappe oct. 4 | lead sombre (584 Hz) et ladder sale (−32 dB), unisson figé, R 46 ms ; nappe à 600 Hz + sub | Supersaw JP (§ 4.1) + carré oct. +1 + clic ; nappe § 4.7 |
| **jump_up** | Basse wobble oct. 3 | — | Nappe | la réponse vit sous 300 Hz (177 Hz, −17,9 dB) | stab wobble ou reese oct. 4 avec couche médium (§ 4.3, 4.4) ; hoover en Montée |
| **liquid_dnb** | Pluck oct. 5 | Cloche | Piano électrique | piano triangle + sub (417 Hz, −14,7 dB), pluck sans suivi de clavier, cloche de 70 ms | DX WINTRHODES ou E.PIANO 1 + trémolo ; pluck § 4.8 ; DX VIBE 1 |
| **neurofunk** | Basse growl oct. 3 | — | Essaim | réponse sous 350 Hz (−23,5 dB) | stab FM/formant oct. 4–5 (§ 4.10) |
| jungle | Hoover oct. 3 | — | Nappe chaude | hoover sans PWM ni chorus, vibrato 0,5 Hz, R 13 ms ; nappe à 304 Hz | hoover § 4.2 ; nappe § 4.7 ; sirène § 4.5 |
| dubstep | Cloche oct. 5 | — | Nappe | cloche triangle sèche | DX BELL C / TUB BELLS + écho dub |
| riddim | Lead oct. 5 | — | Nappe | Lead : vibrato 30 c à 1,35 Hz | vibrato 5 Hz 8 c retardé |
| trance | Lead trance | Pluck | Supersaw | nappe supersaw non suréchantillonnée, unisson linéaire | supersaw JP ; pluck § 4.8 |
| hard_techno | Supersaw oct. 4 | — | Hoover | comme dnb | § 4.1, 4.2 |
| retro_techno | Hoover (+ stab de pack) | — | Hoover | vibrato lent, R 13 ms | § 4.2 |
| gabber | Lead carré | — | Hoover (+ stab de pack) | vibrato 20 c à 1 Hz | vibrato retardé 5 Hz, distorsion avant filtre |
| techno | Lead filtré (Plaits) | — | Pluck oct. 3 | pluck à 216 Hz, sans suivi de clavier | pluck § 4.8 ou stab d'accords |
| minimal | Cloche | Cloche | Pluck | cloche sèche, pluck sombre | DX VIBE 1, pluck § 4.8 |
| detroit | Cloche | Piano électrique | Machine à cordes | piano triangle | DX E.PIANO 1, cordes DX/VSCO |
| industrial | Cloche oct. 3 | — | Essaim | cloche triangle bitcrushée | Plaits modal + bitcrush |
| acid_techno, acidcore | Lead acide | — | Hoover / stab de pack | (le 303 est bien) | hoover § 4.2 |
| house | Arpège oct. 4 | — | Piano FM | « Piano FM » = DX BASS 1, arpège à 427 Hz | DX E.PIANO 1 ; arpège : suivi de clavier |
| deep_house | Voix robot / chops | — | Piano électrique | piano triangle | DX E.PIANO 1 + trémolo, ou piano Splendid |
| tech_house, speed_garage | chops | — | Orgue | orgue à R 2 ms + sub 0,3 | DX E.ORGAN 3, R 100 ms |
| uk_garage | chops | — | Piano électrique | piano triangle | DX E.PIANO 1 / WINTRHODES |
| acid_house | Pluck | — | Machine à cordes | pluck sombre | § 4.8 |
| afro_house | Marimba (Plaits) | — | Nappe chaude | nappe à 304 Hz + sub | marimba VCSL ; nappe § 4.7 |
| disco | Cordes | — | Pluck | cordes 2 scies à 1,2 kHz, vibrato à 0,7 Hz | instrument VSCO Cordes / DX STRINGS |
| amapiano | Piano FM | — | Piano électrique | « Piano FM » = DX BASS 1 | DX E.PIANO 1, log drum gardé |
| boom_bap | Piano électrique | — | Piano électrique (cordes VSCO si installées) | piano triangle | DX E.PIANO 1 + VINYL ; Salamander |
| lofi | Piano électrique | — | Piano électrique (Salamander si installé) | piano triangle | DX E.PIANO 1 / WINTRHODES + VINYL |
| trap | Cloche oct. 5 | Nappe | Cordes | cloche triangle de 70 ms | DX BELLS / TUB BELLS, cordes VSCO |
| drill | Piano électrique | — | Cordes | piano triangle | Salamander / DX PIANO 5, cordes VSCO |
| phonk | cloche de kit | — | Nappe | nappe à 600 Hz + sub | nappe § 4.7 |
| grime | Lead chiptune (Plaits) | — | Lead carré | (chiptune volontaire) | — |
| jersey_club | chops | — | Nappe chaude | nappe sombre | § 4.7 |
| reggaeton | Pluck oct. 5 | — | Orgue | pluck à 216 Hz | § 4.8 |
| afrobeats | Marimba | Pluck | Piano électrique | piano triangle, pluck sombre | DX E.PIANO 1, marimba VCSL |
| synthwave | Lead carré oct. 5 | — | Arpège | vibrato 20 c à 1 Hz, arpège sombre | brass/lead Jupiter (§ 4.11) |
| pop | Pluck oct. 5 | — | Piano FM | Piano FM = DX BASS 1, pluck sombre | piano Salamander / DX E.PIANO 1, pluck § 4.8 |

---------------------------------------------------------------------------------------------------------------
## 6. Ce qui manque au moteur

Par ordre d'effet sur l'oreille. Chaque ajout garde le son par défaut identique (paramètre en fin de liste,
valeur au repos = comportement d'aujourd'hui), suit les règles du fil audio (rien d'alloué, pas de verrou)
et reste déterministe (aléatoire à graine, repartant avec le transport).

1. **Suivi de clavier du filtre** (KeyTrack 0..1, 0,5 = la coupure double par octave à 50 %), **vélocité
   → coupure** (0..+2 octaves) et **vélocité → enveloppe**. Effort S. Synth.cpp, à l'endroit où on
   calcule `fc`.
2. **Unisson façon JP-8000** :
   - rapports de Szabo (−0,110…+0,107) à la place des positions linéaires ;
   - phases aléatoires à chaque nouvelle note (xorshift par voix, repartant avec le transport) ;
   - dérive lente par voix (bruit filtré à 0,1–0,5 Hz, ±3 c) ;
   - mélange centre/côtés ;
   - passe-haut 12 dB à f0.
   Effort M.
3. **Filtre** : SVF par défaut sous l'unisson, ou ladder vraiment suréchantillonné (demi-bande `hiir` ×2,
   déjà dans le dépôt, au lieu de l'interpolation linéaire de DaisySP), et un drive avant le filtre
   (DRIVE est après). Cible ≤ −60 dB de non-harmoniques sur 7 scies. Effort S–M.
4. **Enveloppe de hauteur** (±24 demi-tons, déclin 5–300 ms) et **couche de clic/bruit** avec sa propre
   enveloppe de 2–10 ms et son passe-haut. Effort S.
5. **Vibrato par voix**, avec délai et montée progressive (Lfo1Delay), à 4–7 Hz ; LFO1 à phase libre par
   voix. Effort S.
6. **Chorus stéréo en entrée**, de type Juno : 2 lignes BBD à 1,5–5 ms, triangle à 0,5 et 0,8 Hz, côté
   inversé, sans réinjection. Il remplace `StereoChorus`, qui somme en mono. Effort S.
7. **Cycle de 8 mesures déphasable** : CycleOffset, avec « ouvert » au début de la phrase. Ou la courbe
   par une ligne d'automation de morceau (déjà possible). Effort S.
8. **PWM, pulse-scie et sync d'oscillateur** pour le hoover et les brass ; DaisySP `VariableSawOscillator`
   et `VariableShapeOscillator` (MIT). Effort M.
9. **FM 2 opérateurs par voix** (DaisySP `Fm2`, MIT) pour les stabs neuro et les cloches ; en attendant,
   Plaits 10, 2, 3, 4. Effort M.
10. **Tables d'ondes** : Plaits modèle 13 existe déjà ; tables AKWF (CC-BY 3.0) en option. Effort M–L.
11. **Couches (Track.layers, en cours)** : jusqu'à 4 sources par piste, chacune avec son niveau, sa
    transposition, son passe-haut et son panoramique. Elles portent la plupart des recettes ci-dessus. À
    prévoir dès maintenant : le passe-haut par couche (pour qu'une couche octave ou pluck ne remplisse pas
    le bas) et la vélocité qui commande le niveau de chaque couche (une couche de bark ou de clic qui n'entre
    que sur les notes fortes).
12. **Plaits** : vélocité → volume pour les modèles déjà enveloppés (FM 6 opérateurs), et un sélecteur de
    patch nommé (index de banque) dans le modèle, pour que « Piano FM » soit un piano. Effort S.

---------------------------------------------------------------------------------------------------------------
## 7. Échantillons libres (CC0, MIT, CC-BY seulement)

| source | licence | ce qu'elle apporte | remarque |
|---|---|---|---|
| **VCSL**, `Electrophones/TX81Z` (dossiers « FM Piano », « Piano 1 », « Clavisynth ») | **CC0-1.0** | un piano électrique FM échantillonné (Yamaha TX81Z), à faire en instrument multizone | VCSL est déjà dans THIRD_PARTY (percussions) ; à vérifier : couches de vélocité, boucles |
| VCSL, idiophones (marimba, glockenspiel, kalimba, vibraphone si présent) | CC0-1.0 | cloches et mailloches de contre-chant | pack « Percussions mélodiques » déjà publié |
| **VSCO 2 CE** (cordes, cuivres, bois, harpe) | CC0-1.0 | nappes de cordes, pizz., cuivres | déjà des instruments (phase 38) : à brancher sur trap, drill, disco, liquid |
| Salamander Grand, Splendid | CC-BY (déjà dans THIRD_PARTY) | piano à queue | déjà en instrument (lofi, pop, liquid) |
| **AKWF** (Adventure Kid Waveforms, ~4 400 cycles uniques) | **CC-BY 3.0** (citer « Kristoffer Ekstrand / Adventure Kid ») | tables d'ondes pour une future table d'ondes ou un futur sampler de cycles | des messages de forum parlent de « domaine public » : à vérifier sur la page officielle ; garder CC-BY par prudence |
| Freesound, filtre « Creative Commons 0 » | CC0 (vérifier chaque son) | stabs, hoovers, sirènes, rhodes isolés | faire le tri à la main, noter l'auteur et l'URL dans le pack.json |
| Banques DX7 de Plaits (`resources.cc`) | MIT (déjà dans le moteur) | 96 patches DX7 : EP, cloches, cordes, cuivres, nappes, PPG, CS-80 | **la meilleure source immédiate : rien à télécharger** |

Exclus :
- **jRhodes3** (échantillons en CC BY-NC) ;
- le Rhodes de Pianobook (licence floue) ;
- celui de « zvon » (redistribution interdite) ;
- tables et presets de Surge et Vital (GPL ou propriétaires) ;
- Dexed (GPL).

---------------------------------------------------------------------------------------------------------------
## 8. Plan priorisé, avec tests et cibles

### Lot A · Recettes seules (Kotlin, effort S, aucune règle du fil audio en jeu)

Contenu :
- `SoundLibrary` : SUB = 0 dans tous les sons « Leads », « Nappes », « Pluck », « Claviers ».
  Releases : Supersaw 0,65, Hoover 0,62, Lead 0,6, Pluck 0,62, Arpège 0,55.
- Vibratos : Lead, Lead carré, Hoover, Cordes, Piano électrique à LFO1 0,77 (5 Hz) et VIBRATO ≤ 0,05,
  ou 0 pour le piano.
- Coupures : Nappe 0,72, Nappe chaude 0,68, Piano électrique 0,7, Pluck 0,5, Arpège 0,55. FILTER_TYPE
  SVF dès que UNISON > 0 ou que deux scies jouent à plus de 15 c.
- `WideMix.lead` : OCTAVE_DOWN 0 pour les plucks, claviers et cloches ; 0,15 ailleurs ; CYCLE_DEPTH 0,25
  sur les leads (±0,5 octave).
- Le Supersaw de la nappe trance suréchantillonné.
- `StyleRecipes` (via `recipes_to_kotlin.py`) :
  - liquid : nappe = DX WINTRHODES (Plaits 3, h 0,077) + AUTO_PAN en croches ; contre-chant = DX VIBE 1 ;
  - « Piano FM » = Plaits 3, h 0,015 (E.PIANO 1) ;
  - trap, dubstep : cloche = DX BELL C / TUB BELLS ;
  - jump up, neurofunk : réponse en octave 4 ;
  - envoi de réverbe des leads à 0,2–0,25.

Tests Kotlin :
- aucun son non-basse avec SUB > 0 ;
- FILTER_TYPE ≠ ladder quand UNISON > 0 ;
- A_RELEASE ≥ 0,6 (≈ 166 ms) pour les leads ;
- LFO1 ≥ 4 Hz dès que VIBRATO > 0 ;
- « Piano FM » est un patch de la banque EP ;
- la nappe liquid est en Plaits modèle 3.

Banc (sonde + pistes seules), cibles du lot A :

| métrique | cible |
|---|---|
| présence téléphone 1–4 kHz, lead | ≥ −7 dB |
| présence téléphone 1–4 kHz, nappe | ≥ −12 dB |
| brillance, lead | ≥ −16 dB |
| boue, lead | ≤ +2 dB |
| boue, nappe | ≤ +8 dB |
| non-harmoniques du Supersaw à Do6 | ≤ −60 dB (mesuré : −67 dB avec la recette) |

A/B n° 1.

### Lot B · Moteur, le toucher et l'attaque (C++, effort M)

> **Fait (son 8, 2026-10-27)** : `docs/son/banc/apres-son8.md`. Tests hôtes `engine/host-tests/leads2_tests.inc`.

Contenu : suivi de clavier, vélocité → coupure et enveloppe, enveloppe de hauteur, couche de clic, vibrato
par voix retardé, cycle déphasable, vélocité → volume pour les voix FM de Plaits.

Host-tests (`engine/host-tests`) :
- `keytrack_keeps_brightness_across_keys` : centroïde/f0 à Do3, Do5, Do7 dans un facteur 2 avec
  KeyTrack 1 ;
- `velocity_opens_the_filter` : rapport de centroïde ≥ 1,15 entre 1,0 et 0,4 ;
- `pitch_env_starts_high` : f0 mesurée à 3 ms > f0 × 1,5, puis à ±5 c de la note après 50 ms ;
- `click_layer_is_short` : énergie au-dessus de 2 kHz dans les 5 premières ms ≥ +12 dB par rapport à
  50–100 ms ;
- `vibrato_delay` : écart de hauteur < 2 c pendant les 150 premières ms ;
- tous les nouveaux paramètres au repos donnent un rendu identique, échantillon pour échantillon (le test
  `plain()` actuel étendu) ;
- aucune allocation dans `render` (motif existant).

Cibles :
- Pluck : centroïde/f0 de Do3 à Do7 entre 1,2 et 2,5 ;
- attaque d'un pluck ≥ 12 dB, brillance d'attaque ≥ 1,5 ;
- attaque d'un lead ≥ 3 dB.

A/B n° 2.

### Lot C · Moteur, l'unisson pro et un filtre propre (C++, effort M)

> **Fait (son 8)** : supersaw JP-8000, ladder linéaire (au lieu du suréchantillonnage hiir), drive avant le
> filtre, chorus à entrée stéréo (5ᵉ réglage du chorus). Voir `apres-son8.md`.

Contenu : supersaw JP (rapports, phases aléatoires à graine, dérive, mélange, passe-haut sur f0), ladder
suréchantillonné ×2 avec hiir (ou SVF par défaut), drive avant le filtre, chorus stéréo de type Juno.

Host-tests :
- `unison_phases_differ_each_note` : corrélation des 200 premières ms de deux attaques ≤ 0,5, mais deux
  exports identiques octet pour octet ;
- `unison_jp_ratios` : pics FFT aux écarts attendus à ±1 c ;
- `unison_no_periodic_beat` : pic de l'autocorrélation de l'enveloppe entre 0,3 et 5 s ≤ 0,5 ;
- `ladder_dense_stack_clean` : 7 scies ouvertes, non-harmoniques ≤ −60 dB à Do6 ;
- `chorus_keeps_input_side` : le côté d'une entrée large est conservé à ±1 dB ;
- `drift_bounded` : dérive ≤ ±5 c sur 60 s.

A/B n° 3.

### Lot D · Couches et nouvelles voix (Track.layers + FM, effort L)

> **Fait en partie (son 8)** : couches de l'assistant (supersaw + carré à l'octave, pluck + nappe, piano FM +
> nappe d'air), PWM, paire FM 2 opérateurs dans le synthé. Pas fait : la couche médium sous un stab reese ou
> wobble (la réponse jump up est un hoover en octave 4 depuis le lot A), la paire FM dans les recettes (le
> stab neuro reste celui de Plaits), le piano VCSL. Voir `apres-son8.md` et `docs/REVIEW.md`.

Contenu :
- recettes à couches : supersaw + carré oct. +1 + clic ; pluck + nappe ; piano FM + air ; stab reese ou
  wobble + couche médium ;
- PWM et pulse-scie (hoover) ;
- FM 2 opérateurs par voix (stabs neuro, cloches) ;
- piano VCSL TX81Z en multizone (CC0) en option.

Banc :
- mouvement de la nappe sur 8 mesures ≥ 2,5 dB ;
- centroïde du lead en mesure 1 de la phrase ≥ 0,9 × sa médiane ;
- présence du lead au téléphone ≥ −5 dB ;
- côté/centre dans la fourchette S4 (−15 à −9 dB sur le mix) ;
- corrélation sous 120 Hz ≥ 0,95 (pas de couche qui déborde dans le grave).

A/B n° 4.

### Cibles chiffrées, en un seul tableau (bloquantes au banc une fois atteintes)

| métrique | actuel (pire cas) | cible |
|---|---|---|
| présence téléphone 1–4 kHz, lead | −23,5 dB (neuro) / −6,9 dB (dnb) | ≥ −7 dB (lot A), ≥ −5 dB (lot D) |
| idem, nappe | −21,8 dB | ≥ −12 dB |
| brillance 4–10 k / 1–4 k, lead | −38,5 dB | −8 à −14 dB |
| boue 150–500 / 1–4 k, lead | +25,5 dB | ≤ 0 dB |
| mouvement spectral sur 8 mesures (forme sur 18 bandes), nappe | 1,7 dB | ≥ 2,5 dB |
| centroïde mesure 1 / médiane de la phrase, lead | 0,73 | ≥ 0,9 |
| écart de l'unisson | ±24 c linéaire | extérieur ±25–35 c, rapports JP (0,18 / 0,57 / 1) |
| corrélation d'une attaque à l'autre (unisson) | 0,94 | ≤ 0,5 |
| non-harmoniques et repliement, note tenue Do6 | −32 dB | ≤ −60 dB |
| rapport de vélocité (centroïde) | 1,00 | ≥ 1,15 |
| volume par vélocité (FM) | 0,2 dB | ≥ 4 dB |
| centroïde/f0 Do3 → Do7 | ×64 d'écart (pluck) | facteur ≤ 2 |
| release d'un lead | 3 à 46 ms | 150–400 ms |
| vibrato | 0,5–1,8 Hz, ±10–30 c | 4–7 Hz, ≤ ±15 c, retardé |

À ajouter à `ps_measure --lead` : présence, brillance, boue et centroïde selon la position dans la phrase,
lus sur la piste lead rendue seule (`--solo`, déjà là), et la même chose pour la nappe (`META pad` à
ajouter dans l'enregistrement).

---------------------------------------------------------------------------------------------------------------
## 9. Plan d'écoute A/B

- **Extraits** : 4 × 30 s, pris au même endroit à chaque passe : début du Drop 1, 4 mesures de Montée
  comprises.
  - **dnb** (riff Supersaw + nappe) ;
  - **liquid_dnb** (piano + pluck + cloche) ;
  - **jump_up** (réponse wobble ; la nappe au Break) ;
  - **neurofunk** (réponse).
  - En plus, un extrait « hors DnB » au choix : trance ou lo-fi.
- **Variantes** : A = aujourd'hui, B = lot A, C = lot A + B, D = lot A + B + C (puis D + couches). La sonie
  est égalisée à ±0,5 LU sur l'extrait (sans quoi « plus fort » gagne toujours), avec le même master.
- **Ordre aveugle** : chaque paire est présentée dans un ordre tiré au sort (noté dans le fichier, pas dans
  le nom).
- **Écoute** :
  1. haut-parleur du téléphone, à mi-volume (le cas de Benjamin) ;
  2. écouteurs.
- **Question** (une seule, réponse en un geste) : « Lequel sonne le plus comme un vrai morceau : 1 ou 2 ? »,
  puis « Qu'est-ce qui cloche encore ? » en ses mots.
- **Critère** : on garde un lot s'il gagne au moins 3 extraits sur 4 au téléphone et n'en perd aucun aux
  écouteurs. Sinon, on revient sur la cause listée au § 3 qui a le plus changé.
- **Contrôle mesuré en même temps** : le tableau des cibles du § 8 pour chaque variante, joint au
  message. Côté/centre et corrélation sous 120 Hz doivent rester dans la plage S4 (pas de gain « par la
  largeur »).
- Les morceaux sont faits avec les mêmes graines (`MelodyBuilder.seed`) : seul le timbre change.

---------------------------------------------------------------------------------------------------------------
## 10. Sources

- Supersaw : A. Szabo, « How to Emulate the Super Saw » (KTH, 2010), cité par :
  - [KVR : Roland Supersaw, rapports .893…1.110, « free running/random », « linear detuning → constant beat
    rates »](https://www.kvraudio.com/forum/viewtopic.php?p=4644620) ;
  - [KVR (fil 2)](https://www.kvraudio.com/forum/viewtopic.php?p=8572590) ;
  - [Synthtopia, JP6K](https://synthtopia.com/?p=31937) ;
  - [KVR, JP-4c Reaktor](https://www.kvraudio.com/forum/viewtopic.php?p=5429065).
- Hoover et reese :
  - [MusicRadar, classic 90s hoover (pulse-scie PWM, ±40 c, −12 st)](https://musicradar.com/how-to/how-to-create-a-classic-90s-hoover-sound) ;
  - [Attack, Liquid Hoover Bass](https://www.attackmagazine.com/technique/synth-secrets/liquid-hoover-bass/) ;
  - [MusicRadar, Reese](https://musicradar.com/tuition/tech/how-to-make-a-reese-bass-sound-258701) ;
  - [NI, Reese](https://blog.native-instruments.com/reese-bass/) ;
  - [Avid, Reese](https://www.avid.com/resource-center/how-to-make-a-reese-bass) ;
  - [SoundBridge, Reese](https://www.soundbridge.io/design-a-reese-bass-from-scratch).
- DnB et neuro :
  - [Splice, DnB sounds in Serum](https://splice.com/blog/drum-and-bass-sounds-in-serum) ;
  - [Loopmasters, jump up bass (couche médium)](https://www.loopmasters.com/articles/4480-How-To-Get-the-Perfect-Jump-Up-Bass-A-D-B-Bass-Tutorial) ;
  - [Sonic State, Noisia « Kings of the Midrange »](https://sonicstate.com/news/2026/07/20/noisia-kings-of-the-midrange-/) ;
  - [NI, neurofunk](https://blog.native-instruments.com/watch-how-to-make-neurofunk-drum-and-bass) ;
  - [Computer Music, neuro dans Serum](https://www.pressreader.com/australia/computer-music/20160615/283201557570027).
- Liquid :
  - [DnB Forum, instruments liquid (Rhodes derrière la nappe, release longue, envois variés)](https://dnbforum.com/threads/liquid-dnb-instruments.146932/latest) ;
  - [Rewind Samples, RVS Atmos](https://rekkerd.org/rewind-samples-releases-rvs-atmos-dnb-sample-pack/).
- Piano FM :
  - [Muzines, DX7 (algorithme 5, modulateur 14:1)](https://www.muzines.co.uk/articles/yamaha-dx7/4733) ;
  - [Muzines, Practically FM](https://www.muzines.co.uk/articles/practically-fm/3945) ;
  - [Attack, FM electric piano](https://www.attackmagazine.com/technique/tutorials/fm-electric-piano/) ;
  - [Yamaha, FM 101](https://yamahasynth.com/learn/synth-programming/fm-101-part-five-expanding-your-sound/) ;
  - [DecentSampler, FM](https://decentsampler-developers-guide.readthedocs.io/en/1.21.1/topic-how-to-use-fm-synthesis.html).
- Pluck :
  - [Avid, trance plucks](https://www.avid.com/resource-center/how-to-make-trance-plucks) ;
  - [Cymatics, plucks dans Serum](https://cymatics.fm/blogs/production/creating-plucks-xfer-serum) ;
  - [Violet Recording, pluck](https://violetrecording.com/how-to-make-a-pluck-sound/).
- Dérive analogique :
  - [Sinevibes Dense (dérive par oscillateur, accord aléatoire par note)](https://www.sinevibes.com/korgdense/) ;
  - [Logic ES2 « Analog »](https://support.apple.com/ar-kw/guide/logicpro/lgsia1296c4/mac) ;
  - [KVR, Analog Drift](https://www.kvraudio.com/forum/viewtopic.php?p=7408514) ;
  - [Gearspace, VCO instability](https://gearspace.com/threads/vco-instability-emulation-tips.710793/).
- Licences :
  - [VCSL (CC0, Electrophones/TX81Z)](https://github.com/sgossner/VCSL) ;
  - jRhodes3, CC BY-NC (exclu) : [KVR](https://www.kvraudio.com/forum/viewtopic.php?p=8522315), [Zynthian](https://os.zynthian.org/packages/Soundfonts/jRhodes3d/info.yml) ;
  - AKWF, CC-BY 3.0 : [rekkerd](https://rekkerd.org/?p=25058), [MSX forum](https://www.msx.org/forum/msx-talk/software/big-collection-free-single-cycle-waveforms-useful-scc).
- Projet :
  - `docs/hits/bass.md` (familles DnB et dubstep) ;
  - `docs/PLAN-SON-PRO.md` (S4) ;
  - `scratchpad/basses-qui-tapent.md` (méthode) ;
  - `engine/src/main/cpp/core/Synth.cpp` ;
  - `project-model/.../WideMix.kt`, `SoundLibrary.kt`, `SynthPatch.kt`, `StyleRecipes.kt` ;
  - `engine/third_party/mutable/plaits/resources.cc` (banques DX7) ;
  - `third_party/DaisySP/Source/Filters/ladder.cpp`.

Scripts et données (dans `scratchpad/leads/`) :
- `tools/` (ps_live, ps_render recompilés) ;
- `py/stem.sh`, `py/job.sh`, `py/stemstats.py`, `py/roles.py`, `py/summary.py`, `py/probestats.py`,
  `py/alias.py`, `py/pmeas.py`, `py/variants.py`, `py/refs.py`, `py/refmeas.py`, `py/presence.py`,
  `py/readstems.py` ;
- `probe/probe.cpp`, `probe/pprobe.cpp` ;
- `roles.json`, `probe.json`, `stems.jsonl`, `summary.md`.

---------------------------------------------------------------------------------------------------------------
## Annexe A · Pistes seules de tous les styles (lead ou nappe, morceau réel)

(généré depuis `stems.jsonl` ; centroïde en Hz, autres colonnes en dB)

| style | rôle | son | mesures | centroïde | présence tél. | brillance | boue | côté/centre | attaque | mouvement 8 mes. | centroïde mes. 1 / médiane |
|---|---|---|---|---|---|---|---|---|---|---|---|
| deep_house | pad | Piano électrique | 47 | 379 | -13,5 | -28,2 | +14,9 | -7,3 | +9,9 | +1,6 | 0,96 |
| dnb | bass | ?synth | 75 | 146 | -20,5 | -29,8 | +25,6 | -15,9 | +6,2 | +1,6 | 1,00 |
| dnb | lead/counter | Supersaw | 59 | 584 | -6,9 | -19,8 | +6,5 | -7,1 | +0,6 | +5,6 | 0,73 |
| dnb | mix | mix | 114 | 465 | -8,3 | -1,6 | +9,1 | -13,9 | +8,6 | +1,1 | 1,03 |
| dnb | pad | Nappe | 91 | 432 | -21,8 | -37,0 | +20,8 | -6,2 | +6,2 | +2,8 | 0,88 |
| dubstep | lead/counter | Cloche | 9 | 799 | -6,4 | -20,1 | +2,2 | -6,8 | +11,6 | — | 0,82 |
| house | lead/counter | Arpège | 16 | 573 | -9,9 | -20,9 | +8,3 | -8,7 | +14,4 | +4,9 | 0,74 |
| jump_up | bass | Basse growl | 57 | 620 | -5,8 | -16,6 | +3,6 | -3,5 | +10,3 | +1,0 | 1,12 |
| jump_up | lead/counter | Basse wobble | 29 | 177 | -17,9 | -23,6 | +22,0 | -13,1 | +9,0 | — | — |
| jump_up | mix | mix | 114 | 838 | -6,1 | -3,1 | +4,6 | -10,5 | +13,0 | +0,8 | 1,29 |
| jump_up | pad | Nappe | 83 | 432 | -21,6 | -37,0 | +20,7 | -2,9 | +6,0 | +2,5 | 0,92 |
| liquid_dnb | bass | ?synth | 75 | 114 | -17,3 | -22,8 | +26,0 | -18,6 | +9,9 | +2,4 | 1,16 |
| liquid_dnb | lead/counter | Pluck | 75 | 637 | -8,3 | -22,1 | +5,5 | -9,2 | +15,8 | +5,7 | 0,67 |
| liquid_dnb | lead/counter | Cloche | 15 | 663 | -5,4 | -27,0 | -0,1 | -9,6 | +16,7 | — | 1,05 |
| liquid_dnb | mix | mix | 114 | 422 | -7,6 | -0,3 | +9,2 | -16,2 | +8,3 | +1,3 | 1,16 |
| liquid_dnb | pad | Piano électrique | 91 | 417 | -14,7 | -25,8 | +15,9 | -6,2 | +6,5 | +1,7 | 0,98 |
| lofi | lead/counter | Piano électrique | 37 | 540 | -12,3 | -31,1 | +11,9 | -8,9 | +7,2 | +7,5 | 0,79 |
| neurofunk | bass | Basse qui parle | 57 | 168 | -20,0 | -20,2 | +24,6 | -15,8 | +10,6 | +2,3 | 1,10 |
| neurofunk | lead/counter | Basse growl | 58 | 324 | -23,5 | -38,5 | +25,5 | -6,5 | +11,2 | +5,4 | 0,66 |
| neurofunk | mix | mix | 114 | 559 | -7,0 | -0,2 | +8,1 | -15,7 | +8,6 | +1,3 | 1,07 |
| pop | lead/counter | Pluck | 52 | 591 | -11,9 | -19,0 | +9,9 | -9,8 | +19,1 | +5,4 | 0,69 |
| retro_techno | lead/counter | Hoover | 36 | 332 | -5,9 | -29,0 | +5,6 | -10,4 | +3,6 | +4,4 | 0,87 |
| synthwave | lead/counter | Lead carré | 47 | 979 | -3,6 | -16,9 | -4,7 | -9,3 | +2,7 | +6,5 | 0,76 |
| tech_house | pad | Orgue | 34 | 938 | -4,5 | -18,4 | -0,5 | -7,4 | — | +1,5 | 0,93 |
| techno | pad | Pluck | 12 | 352 | -12,5 | -36,2 | +15,2 | -6,0 | +18,0 | +2,1 | 0,97 |
| trap | lead/counter | Cloche | 63 | 1247 | -2,1 | -17,7 | -11,1 | -5,9 | +11,4 | +5,0 | 1,18 |

Notes :
- Ces pistes sont jouées seules : solo, master coupé, inserts et envois gardés.
- « mesures » = nombre de mesures où la piste joue à moins de 24 dB de sa mesure la plus forte.
- Les leads trance, hard techno, jungle et minimal n'entrent pas dans les 110 premières secondes : pas de
  mesure de piste seule pour eux, le diagnostic s'appuie sur la sonde (§ 2) et les réglages (`summary.md`).
- Le seul lead déjà « présent » au téléphone est la cloche trap (centroïde 1,25 kHz, −2,1 dB). La seule
  nappe qui l'est, l'orgue tech house (−4,5 dB), a son attaque à 2 ms et sa release à 2 ms : elle claque.
  Le lead carré synthwave (979 Hz, −3,6 dB) ne garde que son vibrato lent (±20 c à 1 Hz) comme défaut.
