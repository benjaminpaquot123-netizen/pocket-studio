# Plan « son » : du morceau de démo au vrai morceau

Date : 2026-10-23. Base : `/home/user/ps-p22`, main `75dd1ba` (après la phase 32 et l'audit 33).
Travail en lecture seule. Le dépôt n'a pas été modifié et gradle n'a pas été lancé. Les tests moteur sur Linux passent (`./scripts/host-tests.sh`, construit dans le scratchpad : ALL PASSED).
Toutes les mesures citées ont été faites pour ce plan avec le cœur C++ actuel (`libps_core.a`), sauf mention contraire.
Outils jetables : `scratchpad/son/`, avec `probe.cpp` (chaînes, aliasing, limiteur, kick), `acid.cpp`, `sat.cpp`, `tools/measure.py` (prototype du mesureur) et `tools/alias.py`. Les rendus sont dans `scratchpad/son/wav/`, les résultats dans `scratchpad/son/out/drops.jsonl`.

Phrase de Benjamin : « niveau son il faut encore améliorer car on est à peine à la moitié du niveau d'un vrai morceau ».

## Résumé

1. Les volumes et le liant ont été réglés en phase 29 (audit « corps ») : chaque son est calibré, le mix de départ est réglé par rôle, avec bus batterie, master, fills toutes les 4 mesures, Drop 2 et contre-chant. Il reste quatre écarts mesurables avec un vrai morceau, sans compter les sources elles-mêmes :
   - le niveau : −10,7 LUFS médian sur les Drops, contre −8 à −6 en club ;
   - le haut du spectre : −27 à −33 dB d'« air » avec le kit, contre −16 à −20 ;
   - la largeur stéréo : côté/centre de −14 à −30 dB, corrélation de 0,93 à 1,0, contre −10 à −15 dB ;
   - le true peak, au-dessus de −1 dBTP (jusqu'à +0,7 dBTP mesuré).
2. La cause n° 1 à l'oreille, ce sont les sources de batterie. Le kit intégré est synthétique : le kick est un sinus à 47 Hz qui traîne 1,5 s avec un clic faible, et les charleys sont faibles. Les packs, eux, sont des one-shots générés par IA (Stable Audio Open Small en 8 pas). Aucun échantillon de vrai kit de production n'est utilisé.
3. Toutes les pistes d'instruments sont rendues en mono : synthé et son unisson à 7 voix, Plaits, SF2, échantillons stéréo des packs (repliés en mono). C'est le défaut « amateur » le plus net après la batterie.
4. La chaîne master est primitive. Le limiteur travaille en crête échantillon, avec une anticipation de 1 ms et une relâche fixe de 120 ms, sans clipper ni true peak. On ne peut donc pas atteindre −8 LUFS proprement.
5. Le compresseur ajoute +3,6 dB fixes même sans compresser. La Saturation par défaut (master et bus batterie) cache un passe-bas : −1,6 dB à 10 kHz, −3,1 dB à 18 kHz.
6. Le plan : d'abord un banc de mesure qui joue sur Linux les vrais morceaux de l'assistant ; ensuite 9 étapes, des corrections rapides (gain staging, master) aux grands chantiers (batterie échantillonnée, stéréo, production, auto mix/master).

---

## 0. Ce qui existe déjà (à ne pas refaire)

- Calibration des sons : `SoundLibrary.SYNTH_GAIN_DB` / `PLAITS_GAIN_DB` (paramètre `Gain` ±30 dB dans `Synth.h`), plus `WizardMix.KIT_LUFS`, `ROLE_LUFS`, `PANS` et l'étagère 8 kHz sur les charleys.
- Liant automatique `WizardMix.withGlue` : bus batterie Comp + Sat + EQ air ; master OTT (bass music) + Comp + Sat ; pompe sur la basse en House/Techno. Plus `masterDb = −1`.
- Arrangement :
  - fill de fin de phrase de 4 mesures (`WizardMix.phrase`) ;
  - souffle montant et impact (`RISER_TRACK`, bruit blanc filtré) ;
  - Drop 2 plus plein (`dropTwoLane`) ;
  - contre-chant ;
  - balayages de filtre dans le Break ;
  - courbes de la 303.
- Export : WAV 24 bits avec dither TPDF (`WavWriter.cpp:69`), 32 bits float « sans limiteur », FLAC, stems, LUFS intégré avec portillon (`app/.../export/Loudness.kt`).
- Briques intégrées : DaisySP (MIT), Open303 (MIT, suréchantillonné ×4 en interne), Plaits (MIT, 24 modèles dont FM 6 op. compatible DX7), Freeverb (domaine public), libsamplerate, Signalsmith Stretch + Linear (MIT), TinySoundFont (MIT) + banques GM, VCSL (CC0).

---

## 1. Diagnostic : pourquoi ça sonne « à moitié »

### 1.1 Mesures faites pour ce plan

**A. Drops réels, moteur actuel.** Ce sont les scènes de l'audit p29 (`scratchpad/p29corps/cmds-final`), rejouées avec le moteur d'aujourd'hui sur 8 boucles et mesurées par `son/tools/measure.py`. Les valeurs LUFS et TP sont recoupées avec `ffmpeg -af ebur128=peak=true`.

| Drop | LUFS-I | True peak | PLR | Côté/centre | Corrélation | Air > 6 kHz (dB rel.) | Pente 0,1–10 kHz |
|---|---|---|---|---|---|---|---|
| techno, kit | −11,0 | −0,86 dBTP | 10,2 | −15,8 dB | 0,95 | **−28,3** | −6,1 dB/oct |
| techno, packs | −10,0 | −0,17 | 9,9 | −15,5 | 0,95 | −17,0 | −4,6 |
| house, kit | −10,6 | −0,57 | 10,0 | **−29,5** | 1,00 | **−27,2** | −6,1 |
| house, packs | −10,7 | **0,03** | 10,8 | −26,9 | 1,00 | −17,1 | −4,6 |
| tech house, packs | −13,5 | **+0,71** | 14,2 | −14,2 | 0,93 | −12,1 | −4,5 |
| trap, kit | −11,6 | −0,79 | 10,8 | −23,4 | 0,99 | **−33,0** | −5,5 |
| trap, packs | −8,1 | −0,65 | 7,5 | −26,7 | 1,00 | −22,8 | −5,4 |
| dnb, kit | −11,2 | −0,65 | 10,5 | −26,1 | 1,00 | −20,2 | −5,2 |
| pop, packs | −10,6 | −0,55 | 10,1 | −20,0 | 0,98 | −21,0 | −5,7 |

Sur les 37 styles de l'audit p29 (`p29corps/stats/cmp-final.txt`), les médianes sont :

| | LUFS | Facteur de crête | Air | Largeur | Bas-médium |
|---|---|---|---|---|---|
| Kit | −11,1 | 10,4 dB | −25,1 dB | −24,9 dB | −11,8 dB (corrigé) |
| Packs | −11,2 | 11,6 dB | −17,2 dB | −21,3 dB | −11,3 dB |

Cibles d'un master de club : −8 à −6 LUFS, PLR 7 à 9, air −16 à −20, largeur −10 à −15 dB.

**B. Chaînes par défaut de l'assistant.** Sinus à −40 dBFS, gain en dB par rapport à l'entrée (`probe.cpp`) :

| Chaîne | 50 Hz | 1 kHz | 5 kHz | 10 kHz | 16 kHz | 18 kHz |
|---|---|---|---|---|---|---|
| Master (Comp 0,6/0,2 + Sat 0,2/0,85) | +3,73 | +3,70 | +3,21 | +2,10 | +0,90 | +0,62 |
| Compresseur seul | +3,61 | +3,61 | +3,61 | +3,61 | +3,61 | +3,61 |
| Saturation seule | +0,12 | +0,10 | −0,40 | −1,50 | −2,71 | −2,99 |
| Bus batterie (Comp + Sat + EQ +2 dB à 8 kHz) | +1,15 | +1,13 | +0,85 | +1,04 | +0,30 | +0,04 |

Le compresseur du master donne +3,7 dB à tout signal sous le seuil (−40 à −16 dBFS), puis −2 dB à −1 dBFS. Ce makeup statique pousse le limiteur.

**C. Repliement (aliasing).** Énergie non harmonique par rapport au total, et pire raie repliée :

| Source | Mesure |
|---|---|
| Synthé, scie Do6 (1 047 Hz), filtre ouvert | −52,9 dB (ladder) / −42,5 dB (SVF) ; pire raie −49 dBc (SVF) |
| Synthé, scie Do7 (2 093 Hz) | −49,4 / −39,3 dB ; pire raie −43 dBc |
| Synthé, scie Sol7 (3 136 Hz) | −47,0 / −36,5 dB ; pire raie −40,5 dBc |
| Distorsion (overdrive / écrêtage dur), sinus 2,5 kHz | pire raie −34 / −30 dBc |
| Saturation drive 0,8, sinus 2,75 kHz | **−21 dBc** (drive 0,3 : rien) |
| OTT fort | −72 dBc (correct) |
| 303 (Open303, ×4 interne) avec drive 0,8 | −98 dBc : propre |

**D. Kick intégré** (`DrumKit` voix 0) :
- Durée audible 1,5 s (plafond `kMaxSeconds`), soit plus de 3 temps à 128 BPM : il recouvre le kick suivant et la basse.
- Sur 300 ms, l'énergie est à 47 Hz : < 60 Hz −1,6 dB, 120–250 Hz −12,9 dB, 1–4 kHz −35,8 dB.
- Même sur les 10 premières millisecondes, le clic est faible : 1–4 kHz à −21,7 dB.
- C'est un sinus de sub, presque inaudible sur un téléphone, sans « punch » à 100–200 Hz ni attaque à 2–5 kHz.

**E. Limiteur.** Plafond −1,01 dBFS en crête échantillon, mais **true peak jusqu'à +0,71 dBTP** (tech house packs, confirmé par ffmpeg). Un encodage AAC (`AacEncoder.kt`, 256 kbps) de ce fichier écrêtera au décodage.

### 1.2 Causes classées par impact audible

| # | Cause | Preuves (fichiers) | Ce qu'on entend | Impact |
|---|---|---|---|---|
| **1** | **Sources de batterie pas au niveau d'un vrai disque.** Kit intégré synthétique : kick sinus 47 Hz × 3,2 puis 1,5 s de queue, clic en carré naïf ; charleys DaisySP `HiHat` à −45 LUFS solo ; niveaux internes écartés de 40 dB (`kLevels`). Packs de styles et de machines (« TR-808 », « TR-909 ») générés par Stable Audio Open Small en 8 pas, en 16 bits, normalisés en crête à −1 dBFS. Aucune couche (layering), aucune vélocité multi-échantillon, aucun round-robin. | `DrumKit.cpp:21-36,78`, `DrumKit.h`, `scripts/packs/generate_pack.py:25-26,182`, `styles.json` (`kinds.*.prompt`) | Kick mou ou bourdonnant, caisse claire et clap « plastique », aigus soufflés ou flous. C'est la signature d'une démo. | Très fort |
| **2** | **Master et loudness.** Limiteur en crête échantillon (1 ms d'anticipation, relâche fixe de 120 ms, moyenne glissante) : il module le gain dans la période d'une basse à 50 Hz, donc distord dès qu'on le pousse. Ni clipper, ni true peak, ni multibande, ni cible par genre. Drops à −10,7 LUFS médian. | `Mixer.h:34-39`, `Mixer.cpp:45,649-683`, `WizardMix.MASTER_DB` | Morceau plus faible et plus « petit » qu'une référence au même volume. Si on pousse le gain, le grave se déforme. Le TP > −1 dBTP fait écrêter l'AAC. | Très fort |
| **3** | **Pistes d'instruments en mono.** `Synth::render(float* mono)` : l'unisson à 7 voix est sommé en mono. `SampleInstrument` replie les échantillons stéréo (`0.5*(L+R)`). Plaits et 303 sont mono, TinySoundFont tourne en `TSF_MONO`. Seuls les pans de batterie, l'élargisseur sur les nappes tenues et la réverbe ouvrent l'image. | `Synth.cpp:233-317`, `SampleInstrument.cpp:282-285`, `SoundFontPlayer.cpp:24`, `InstrumentTrack.h:208-212` | Supersaw et nappes « petits », tout au centre ; aucune profondeur gauche/droite. | Fort |
| **4** | **Équilibre tonal.** Il manque d'aigus : charleys faibles, et passe-bas à un pôle (`Saturator.toneCoef_` ≈ 12,4 kHz à 0,85) actif à 100 % de mix sur le master et le bus batterie. Le grave est brouillé : queue de kick de 1,5 s, aucun passe-haut sur les pistes non graves, retour réverbe non filtré, bas mono non garanti. | `Effects.cpp:196-212`, `WizardMix.withGlue`, `Effects.cpp:1054-1100` | Son terne et « fermé » face à une référence ; grave boueux sur les gros systèmes. | Fort |
| **5** | **Espace et profondeur.** Une seule réverbe d'envoi (Freeverb 1997 : métallique, entrée mono, sans modulation, sans EQ de retour, pré-délai non calé au tempo) et un seul écho. Pas de plate ni de room. Peu d'envois dans les recettes. | `Effects.cpp:1004-1105`, `Mixer.cpp:551-571` | Sec et plat, ou bien voile métallique ; pas de « premier plan / arrière-plan ». | Moyen-fort |
| **6** | **Dynamique et pompe.** Compresseur à détecteur crête, makeup fixe (+3,6 dB master, +4,2 dB bus), genou fixe, pas de GR visible, pas de mix parallèle ni de passe-haut de détection. La « Pompe » est une enveloppe fixe déclenchée par la voie kick du pad 1, pas par l'audio. La basse et le sub pompent deux fois. | `Effects.cpp:148-180,404-425`, `Engine.cpp:661`, `StarterWizard.kt:782`, `WizardMix.kt` | Kick et basse qui se marchent dessus, pompe absente si le kick vient d'ailleurs, limiteur trop sollicité. | Moyen-fort |
| **7** | **Production et arrangement.** 3 vélocités fixes (X/x/o = 1/0,6/0,35), même son à chaque coup, aucun micro-timing. Sections de 1 à 4 mesures répétées, avec un fill toutes les 4 mesures seulement. Ni crash, ni impact échantillonné, ni reverse, ni downlifter ; peu d'automation (filtres, souffle). Drop ≈ Groove (+0 à +3 LU). | `StarterWizard.kt:487-490`, `WizardMix.phrase`, `riserLane` | Mécanique, répétitif ; les transitions ne « décollent » pas. | Moyen-fort |
| **8** | **Synthés et instruments.** Oscillateurs PolyBLEP sans suréchantillonnage (repliement de −37 à −49 dB en 7e octave), filtres DaisySP de base. Distorsion et Saturation forte non suréchantillonnées (−21 à −34 dBc). GM via TinySoundFont : interpolation linéaire, mono, sans modulateurs ni chorus, banques années 90. | `Synth.cpp`, `ExtraEffects.cpp:53-81`, `tsf.h:1294` | Leads aigus « durs », grain numérique sur les sons saturés, pianos et cordes « General MIDI ». | Moyen |
| 9 | Gain staging caché : le niveau réel est « cuit » dans `Gain` et `kLevels` ; les charleys sont relevés de +9 dB par EQ, ce qui remonte le bruit. | `WizardMix.kt:64-101` | Indirect : mix difficile à corriger. | Faible-moyen |
| 10 | Export : pas de contrôle TP avant AAC, 48 kHz seulement, pas de normalisation de cible. Le reste est correct (24 bits TPDF, LUFS gated). | `AacEncoder.kt:19`, `WavWriter.cpp` | Écrêtage possible du M4A partagé. | Faible |

Point rassurant : la 303 (Open303 ×4) ne replie pas, et le dither et le LUFS de l'export sont justes.

---

## 2. Mesurer d'abord : le « banc de mesure »

But : rendre sur Linux, sans téléphone, **les vrais morceaux complets** de l'assistant, avec les courbes, les scènes, les packs et le master, puis en sortir des chiffres comparables à des cibles par genre. Ces chiffres doivent tourner en CI et servir à prouver chaque étape de la feuille de route.

### 2.1 Choix d'architecture

Trois options ont été pesées :

1. **(Retenue) Enregistrer le flux de commandes réel de l'appli et le rejouer en C++.**
   - Le code qui décrit le projet au moteur hors ligne est celui de l'export : `RhythmViewModel.describe()` (l.5968), `syncEngine()` (l.514) et `pushPart`, branchés sur `NativeEngine.captureInto`.
   - Toutes les commandes passent par `NativeEngine.post()` / `exportPost`, `setPadSample`, `captureAt`, `setStems`, et par `NativeSample.create(FloatArray, channels, rate)`.
   - On ajoute une couture `NativeEngine.sink: CommandSink?` (null en production). Avec un `RecordingSink`, chaque commande est écrite dans un fichier texte `.pscmd`, et chaque `NativeSample` est vidé en `.f32`.
   - Un test Robolectric `SongBenchExport` (app/src/test, désactivé par défaut, activé par `-Pbench`) construit pour chaque style le projet de l'assistant avec une graine fixe, puis appelle `describe(song = true)`.
   - Fidélité totale : c'est le même code que l'export, scènes mises en attente à la demi-croche près comprises.
   - À moyen terme, sortir `describe/syncEngine/pushPart` de `RhythmViewModel` (6 000 lignes) dans un `ProjectSync` pur Kotlin. Le test n'aura plus besoin de la VM.
2. Réécrire la traduction projet → commandes à part (comme `probe/Render.kt` des audits p28/p29). C'est rapide, mais c'est une copie qui dérive. Elle n'est utile qu'en dépannage (les scripts existent dans `scratchpad/p29corps`).
3. Compiler la JNI pour Linux et faire tourner l'appli sous Robolectric avec la vraie `.so`. Fidélité maximale, mais il faut bouchonner Oboe et c'est lourd. À écarter.

**Côté C++** (nouveau dossier `engine/bench/`, qui réutilise la bibliothèque `ps_core` de `engine/host-tests/CMakeLists.txt`) :
- **`ps_render`** lit le `.pscmd`. Il poste les commandes dans `OfflineRenderer::post`, avec `scheduleFrom(frame)` aux `AT <frame>`, comme le fait la JNI d'export. Il rend le morceau entier puis la queue automatique (`setAutoTail`) en WAV float 32. Variantes :
  - `--no-limiter` (pré-master) ;
  - `--solo <canal>` (stems, même logique que `stemExports`).
- **`ps_measure`** lit le WAV et la liste des sections (frames de début de chaque partie, écrites dans le `.pscmd`) et sort un JSON de métriques (§ 2.3) :
  - la sonie avec **libebur128** (MIT, v1.2.6) en sous-module ;
  - le spectre avec **Signalsmith Linear** (MIT, déjà vendoré) ;
  - le reste en C++ simple.
- **`scripts/song-bench.sh`** :
  - lance le test d'export (gradle, en CI) puis `ps_render` et `ps_measure` sur les 36 styles × {kit, packs} ;
  - compare aux seuils (`engine/bench/targets/*.json`) ;
  - écrit `build/bench/report.md` (tableau, flèches vers la référence précédente) ;
  - publie WAV et rapport en artefacts CI.
- Les packs nécessaires sont téléchargés depuis les Releases `sons-packs-*` (déjà publiées) et mis en cache CI.
- Déterminisme : le moteur rend à l'identique quelle que soit la taille des blocs (garanti par les tests existants). Une empreinte SHA du WAV par style, gardée dans `engine/bench/golden.json`, détecte tout changement de son non voulu.

### 2.2 Format `.pscmd` (texte, une ligne par appel)

```
PSCMD 1
RATE 48000
SONG frames=<n> tail=<max> fade=<frames> limited=1 masterFx=1
SECTION <index> <nom> <frameDébut>          # pour les mesures par section
SAMPLE <id> <canaux> <fréquence> samples/<id>.f32
SF2 <id> <chemin> ; STEMS <id> <chemins…>
AT -1                                         # captureAt(-1) : immédiat
C <type> <valeur> <a> <b>                     # NativeEngine.post
PAD <pad> <idSample|-1>   TRACKSAMPLE <piste> <idSample>
AT <frame>                                    # captureAt(frame) : commandes différées
```

### 2.3 Métriques et seuils d'acceptation

Mesurées sur le morceau entier (I, LRA, TP) et par section (S = 3 s, bandes et stéréo sur la section la plus forte, Drop ou Refrain).

| Métrique | Définition | Seuil |
|---|---|---|
| LUFS intégré | BS.1770-4 / EBU R128 (libebur128) | Cible du genre (§ 2.4) ±1 LU ; ±0,5 après « Masteriser » |
| True peak | libebur128 (suréchantillonnage ×4 à 48 kHz) | **≤ −1,0 dBTP** partout (aujourd'hui jusqu'à +0,7) |
| PLR | TP − LUFS-I | Club ≥ 6 et ≤ 10 dB ; streaming 9 à 14 dB |
| Facteur de crête (section) | crête / RMS | ≥ 6 dB (jamais écrasé) |
| LRA | EBU R128 (morceau entier) | 3 à 9 LU en club, 4 à 12 en chanson |
| Contraste des sections | LUFS court terme moyen par section | Drop − Groove ≥ +2 LU ; Drop − Break ≥ +4 LU ; Montée croissante ≥ +3 LU sur ses 4 dernières mesures |
| Bandes | énergie relative (sub < 60, grave 60–250, bas-médium 250–500, médium 0,5–2 k, haut-médium 2–6 k, air > 6 k) | ±3 dB de la courbe du genre (§ 2.5) |
| Courbe 1/3 d'octave | écart moyen absolu 40 Hz–16 kHz à la courbe du genre | ≤ 2,5 dB moyen, ≤ 6 dB sur toute bande |
| Pente spectrale | régression de la densité 0,1–10 kHz | ≈ −4,5 ±0,7 dB/oct (aujourd'hui −5,2 à −6,1 en kit) |
| Largeur | énergie côté / centre (Drop) | −15 à −9 dB |
| Corrélation L/R | moyenne sur la section | 0,3 à 0,85 (jamais < 0) |
| Bas mono | corrélation < 120 Hz ; côté/centre < 120 Hz | ≥ 0,95 ; ≤ −20 dB |
| Compatibilité mono | LUFS(mono) − LUFS(stéréo) | ≥ −1,5 LU |
| Kick / basse | énergie < 100 Hz de la basse dans les 50 ms après chaque kick (stems) | ≥ 6 dB sous le reste du temps (sidechain) |
| Équilibre des parties (stems) | LUFS de chaque partie − mix | basse −2 à −6, accords −6 à −10, lead −5 à −9, charleys −14 à −20 ; aucune partie cochée < mix −14 dB |
| Écrêtage / NaN | échantillons ≥ 0,999 (rendu limité) ; non finis | 0 ; 0 |
| DC | moyenne absolue | < 1e-3 |
| Fin | 500 ms finales | < −60 dBFS |

### 2.4 Cibles de sonie par genre (préréglage « Club / sortie »)

Pour tous les genres, un préréglage « Streaming » vise −14 ±1 LUFS-I et ≤ −1 dBTP.

| Famille / styles | LUFS-I | PLR |
|---|---|---|
| Techno, hard techno, acid techno, industrielle, acidcore | −7 ±1 | 6–8 |
| Gabber | −6 ±1 | 5–7 |
| House, tech house, afro house, UK garage, speed garage, disco, amapiano | −8 ±1 | 7–9 |
| Deep house | −9 ±1 | 8–10 |
| Drum & bass, jungle, neurofunk, dubstep, riddim | −7 ±1 | 6–8 |
| Liquid DnB, trance, synthwave, rétro techno, Detroit, minimal | −8 ±1 | 7–9 |
| Trap, drill, phonk, grime, jersey club, reggaeton, afrobeats, pop | −8,5 ±1 | 7–9 |
| Boom bap | −10 ±1,5 | 9–11 |
| Lo-fi hip-hop | −12 ±1,5 | 10–13 |

### 2.5 Courbes de référence par genre

- Point de départ : les ordres de grandeur d'un master de club déjà utilisés dans CORPS.md. Sub et grave à −4/−6 dB chacun, bas-médium −10, médium −10/−12, haut-médium −14/−16, air −16/−20, largeur −10/−15 dB.
- Affinage par références :
  - `scripts/bench/reference_curves.py` (numpy/scipy) mesure 5 à 10 morceaux de référence par famille (les « références » des recettes) ;
  - les fichiers restent sur la machine de l'agent, en usage personnel, et ne sont jamais commités ;
  - **seules les courbes dérivées** (1/3 d'octave relatif, LUFS, PLR, côté/centre, moyenne et écart-type) entrent dans `engine/bench/targets/<famille>.json` ;
  - autre source possible : des titres CC-BY/CC0 (Free Music Archive, Jamendo).
- Option diagnostic : **Matchering 2.0.6** (GPL-3, Python, hors ligne) appliqué à un rendu et à une référence. La courbe d'EQ et le gain qu'il calcule disent ce qui manque. Il ne sert qu'à l'analyse : il n'entre pas dans l'appli.

### 2.6 Tests synthétiques (dans `engine_tests`, rapides, sans projet)

- **Repliement** (méthode de `son/tools/alias.py` : FFT Blackman-Harris, énergie hors harmoniques ±6 Hz, fréquences sans rapport entier avec 48 kHz). Seuils :
  - scie et carré du synthé (ladder et SVF, unisson) de Do6 à Sol7 : ≤ −60 dB ;
  - Distorsion (4 types) et Saturation drive 0,8 sur 2,75 kHz : ≤ −50 dBc ;
  - échantillon transposé de +12/+24 : ≤ −50 dB ;
  - chaîne master (clipper + limiteur) sur un sinus de 7 kHz à +6 dB : ≤ −70 dB ;
  - la 303 reste à ≤ −80 dB.
- **Distorsion basse fréquence du limiteur** : sinus 50 Hz poussé de 6 dB, DHT < 1 % (vise ≤ 0,3 %).
- **Clics** :
  - scénarios sur sources sinus : legato, vol de voix, changement de scène, échange d'insert, mute/solo, saut d'automation, « Joue dans » ;
  - détecteur : dérivée seconde passe-haut 8 kHz, pic > 30 dB au-dessus de la médiane locale de 5 ms ;
  - attendu : 0 clic. Une partie de ces tests existe déjà.
- **CPU** : temps de rendu par module (synthé 8 voix stéréo, chaîne master, réverbes) en facteur temps réel sur l'hôte, ramené au téléphone par un coefficient mesuré une fois. Budget :
  - chaîne master ≤ 5 % d'un cœur performant à 48 kHz ;
  - un insert ≤ 1 % ;
  - avec de la marge pour la tablette Tab A11 (entrée de gamme).

### 2.7 Outils open source du banc (hors ligne)

| Outil | Licence (vérifiée) | Version / date | Rôle |
|---|---|---|---|
| libebur128 | MIT (COPYING, © 2011 Jan Kokemüller) | v1.2.6, dernier commit 2021-02-14 (stable) | LUFS M/S/I, LRA, true peak : dans `ps_measure`, plus tard dans l'appli (§ 4, étape 8) |
| Signalsmith Linear | MIT | 0.6.4 (déjà vendoré) | FFT des bandes et du 1/3 d'octave |
| pyloudnorm | MIT | v0.2.0 (commit 2026-01-04) | Contre-vérification en Python |
| FFmpeg `ebur128=peak=true` | LGPL/GPL (outil externe) | présent sur la machine de CI | Contre-vérification LUFS/TP (déjà concordante à 0,1 près ici) |
| numpy / scipy / matplotlib | BSD / PSF | déjà utilisés par `scripts/packs` | Courbes de référence, graphiques du rapport |
| Matchering | GPL-3.0 | 2.0.6 (commit 2026-10-07) | Diagnostic « ce qui manque face à une référence » |

### 2.8 CI

Nouveau job « Song bench » : il se déclenche sur chaque PR qui touche `engine/`, `project-model/` ou `scripts/packs/recipes`. Il échoue si :
- un seuil dur saute (TP > −1, NaN, écrêtage, clic) ;
- ou si une métrique de son régresse de plus de 1 dB/LU face à `main`.

Le rapport (tableau + 3 graphiques : courbe spectrale, LUFS court terme sur le morceau, côté/centre) est joint à la PR, comme les captures d'écran.

---

## 3. Briques open source pour monter en qualité

Licences et versions vérifiées le 2026-10-23. Méthode : fichiers LICENSE et étiquettes git (clones superficiels dans `scratchpad/son/repos`), plus le site de l'auteur pour HIIR et 99Sounds. Le coût CPU est une **estimation** à confirmer par le banc (§ 2.6).

### 3.1 Traitement (DSP)

| Brique | Licence | Dernière version / date | Déjà intégré ? | Usage proposé | Android / ARM | CPU | Verdict |
|---|---|---|---|---|---|---|---|
| **Airwindows** | MIT (© 2018 Chris Johnson) | pas d'étiquette ; commit 2026-10-20 ; 529 effets | Non (prévu dans THIRD_PARTY) | ToTape9 (bande), Console9/ConsoleX (sommation), ClipOnly3/ADClip9 (écrêteur avant limiteur), Galactic3/kPlate140/240/kCathedral5 (réverbes), Pressure6, BussColors4, NotJustAnotherDither | C++ par échantillon, sans allocation : direct | faible à moyen | **Oui** (clipper, bande, plate) |
| **x42 dpl.lv2** (peaklim de Fons Adriaensen) | GPL-3.0-or-later (en-tête `src/peaklim.cc`) | v0.7.3, commit 2026-09-08 | Non | Limiteur master avec anticipation, relâche auto, **true peak** (`set_truepeak`), maintien prolongé sur grave fort (pas de distorsion de forme d'onde) | C++ simple, alloue seulement dans `init` | très faible | **Oui, 1er choix limiteur** |
| **Signalsmith basics** | MIT | 1.2.1, commit 2026-09-30 | Non (Stretch/Linear du même auteur le sont) | Limiter, Dynamics (compresseur/expandeur avec anticipation), Crunch (distorsion ×2 + ADAA), Reverb (FDN), Chorus, Analyser | header-only | faible | **Oui** (alternative MIT au limiteur, Crunch) |
| **Signalsmith dsp** | MIT | v1.7.3, commit 2026-10-13 | Non | `rates.h` (suréchantillonneur ×2 FIR), enveloppes (peak-hold, box filter), délais | header-only | faible | **Oui** (outillage) |
| **HIIR** (Laurent de Soras) | WTFPL v2 (`license.txt`) | v1.50 (2024-08-09) | Non | Suréchantillonnage polyphase IIR ×2/×4 : Distorsion, Saturation, clipper, 303 si besoin | **NEON ARM64** supporté | très faible | **Oui** |
| **Mutable Instruments** (eurorack) | MIT (projets STM32F) | commit 2026-10-12 | **Oui** (Plaits + stmlib vendorés) | Réverbes Clouds/Elements/Beads (`*/dsp/fx/reverb.h`, type Griesinger/Dattorro) ; `plaits/dsp/fx/ensemble.h` (chœur stéréo) | conçu pour Cortex-M4 | **très faible** | **Oui, gain rapide** |
| **DaisySP-LGPL** | LGPL-2.1 | commit 2023-12-21 | Sous-module prévu (dossier vide) | `ReverbSc` (réverbe Csound de Costello, 8 lignes modulées), `MoogLadder`, `Compressor` | C++ simple | faible | Oui (réverbe « Hall » bon marché) |
| **Surge XT** (sst-filters, sst-waveshapers, sst-effects, sst-basic-blocks) | GPL-3.0 (quelques en-têtes de basic-blocks en MIT) | Surge XT `release_xt_1.3.4` ; sst-* commits 2026-10 (waveshapers 2026-02) | Non | Filtres (ladder, OB-Xd, diode, K35, vintage), waveshapers ADAA, effets (Reverb 2, Nimbus…) | SIMD SSE via SIMDE : tourne sur ARM64 | faible (SIMD par 4 voix) | Oui pour les filtres du synthé |
| **chowdsp_utils** | par module : core/math/simd BSD-3 ; compressor, eq, filters, reverb, **waveshapers (ADAA)** GPL-3 | v2.4.0 (2025-12-09) | Non | Waveshapers ADAA pour Saturation/Distorsion, EQ, filtres | C++17 | faible | Possible (doublon avec HIIR + Signalsmith) |
| **ChowTape** (AnalogTapeModel) | GPL-3.0 | v2.11.4, dernier commit 2023-11-05 | Non | Bande magnétique réaliste (hystérésis) | lourd (suréchantillonnage + solveur) | **élevé** | Non (ToTape9 suffit) |
| **LSP** (lsp-dsp-lib, lsp-dsp-units ; plugins) | lib et units LGPL-3.0 ; plugins GPL-3.0 | plugins 1.2.35 ; dsp-lib 0.5.8 ; dsp-units 1.0.38 (commits 2026-08) | Non | Compresseur multibande, limiteur avec suréchantillonnage, EQ, mesures LUFS/TP | NEON/AArch64 optimisé ; système de build propre | moyen | Plus tard (multibande du « Masteriser ») |
| **Dragonfly Reverb** | GPL-3.0 | 3.2.10 (commit 2026-05-21) | Non | Hall/Room/Plate de qualité (freeverb3) | C++ (DPF) | moyen à élevé (Hall) | Option « Grande salle » à l'export |
| **Cloud Seed** | MIT | v1.0.1 (commit 2024-05-26, version 2) | Non | Réverbe algorithmique longue et modulée | C++ | moyen à élevé | Option ambient |
| **Odin 2** | GPL-3.0 (sauf certains éléments) | v2.4.1 (2025-05-30) | Non | Filtres analogiques variés | JUCE, extraction à faire | faible | 2e choix après sst-filters |
| **Vital** | GPL-3.0 | dernier commit 2022-04-20 (figé ; fork Vitalium dans DISTRHO-Ports) | Non | Wavetable | moteur très lié à son cadre | moyen | Non (s'en inspirer) |
| **Dexed / MSFA** | Dexed GPL-3.0 ; MSFA Apache-2.0 | Dexed v1.0.1 (commit 2026-10-14) ; MSFA dernier commit 2017-09-12 | Plaits joue déjà les patchs DX7 (FM 6 op.) | Fidélité DX7 complète | C++ | faible | Faible priorité |
| **sfizz** | BSD-2-Clause | 1.2.3 (dernier commit 2025-03-17) | Non | Lecteur SFZ complet (couches de vélocité, round-robin, interpolation sinc) | non officiel sur Android ; fils et dépendances (abseil) | moyen | Plus tard ; d'abord un `SampleInstrument` multi-zones |
| **Geonkick** | GPL-3.0 | v3.9.0 (2026-10-06) | Non | Synthé de percussion (kicks à enveloppes de hauteur et d'amplitude, couches) ; préréglages à rendre **hors ligne** en one-shots | C, rendu hors ligne | nul sur le téléphone | **Oui** pour fabriquer des kicks |
| **Matchering** | GPL-3.0 | 2.0.6 | Non | Banc (diagnostic) ; principe repris pour « Masteriser » (sonie + EQ vers une cible + limiteur) | Python, hors ligne | — | Banc seulement |
| **libebur128** | MIT | v1.2.6 | Non (LUFS en Kotlin aujourd'hui) | Banc, puis mesures en direct hors du fil audio | C | faible | **Oui** |

### 3.2 Sources d'échantillons de qualité, redistribuables

| Source | Licence (vérifiée) | Version | Contenu utile | Verdict |
|---|---|---|---|---|
| **Sonic Pi `etc/samples`** | CC0 (README : Freesound CC0 + dons d'Arovane CC0) | v5.0.0 | 15 kicks (bd_808, bd_haus, bd_tek, bd_klub…), 21 charleys, caisses claires, sons « elec_ », Arovane | **Oui** (petit, propre, déjà curé) |
| **Freesound** (filtre de licence CC0) | CC0, à vérifier fichier par fichier (API, champ `license`) | en ligne | ex. 89 résultats CC0 pour « 909 kick » ; 808, claps, FX (crash, reverse, impacts, risers) | **Oui** via un script qui garde l'auteur, l'URL et la licence de chaque son |
| **Virtuosity Drums** (Versilian / Karoryfer) | CC0 | v0.925 (commit 2026-04-21) | batterie acoustique multi-vélocités (boom bap, lo-fi, breaks) | **Oui** |
| **VSCO-2 Community Edition** | CC0 | 1.1.0 | cordes, cuivres, bois, piano droit, percussions d'orchestre | **Oui** (nappes et cordes réelles) |
| **VCSL** | CC0 | déjà intégré | percussions | déjà là |
| **Karoryfer Meatbass** | CC0 | v1.001 | basse électrique (disco, funk, house) | **Oui** |
| **Splendid Grand Piano** (échantillons Akai) | domaine public | — | piano à queue | **Oui** (piano par défaut) |
| **Salamander Grand Piano V3** | CC-BY 3.0 (attribution) | — | piano à queue de meilleure qualité, 16 couches | Oui, avec crédit dans THIRD_PARTY |
| jRhodes3d | échantillons CC BY-**NC** | — | piano électrique Rhodes | Usage personnel seulement : à noter dans DECISIONS ou à éviter |
| Kits Hydrogen | variable : HardElectro1 GPL-2+, Audiophob CC0, autres GPL ou CC-BY-SA | — | quelques kits électro | Au cas par cas |
| Kits DrumGizmo | non indiquée sur le wiki | — | acoustiques multi-micros, très lourds | Non (licence à confirmer, taille) |
| **99Sounds** | « You do not have the right to sell or redistribute (even for free) the audio files » | — | — | **Exclu** des packs |
| SAO Small (actuel) | Stability AI Community License | — | packs générés | À reléguer en « Sons générés » ; plus utilisé par défaut pour la batterie |

### 3.3 À écarter (pour l'instant)

- ChowTape (CPU).
- Vital (extraction trop lourde).
- Convolution avec réponses impulsionnelles (CPU et taille ; Freeverb remplacé suffit).
- Hébergement de plugins.
- DrumGizmo (taille, licence non claire).
- 99Sounds, BPB, MusicRadar et autres packs « royalty-free sans redistribution ».

---

## 4. Feuille de route du son (10 étapes, par priorité)

Règle commune : chaque étape passe par le banc (§ 2), ajoute ses tests dans `engine/host-tests/engine_tests.cpp`, met à jour THIRD_PARTY.md et DECISIONS.md, et n'alloue ni ne verrouille dans le callback (tout se prépare dans `prepare()`, réglages par `clampSetting`, mesures par atomiques ou `SpscFloatRing`).
Un nouveau son par défaut ne doit pas changer les anciens projets : version de kit ou de chaîne dans le projet, ancien comportement gardé pour les projets existants.

### Étape 0 : banc de mesure (prérequis), effort M
- **Quoi** : couture `NativeEngine.sink` + `RecordingSink` ; test `SongBenchExport` (Robolectric, graine fixe, 36 styles × {kit, packs}) ; `engine/bench/ps_render.cpp` et `ps_measure.cpp` ; `scripts/song-bench.sh` ; job CI ; premier rapport de référence.
- **Bibliothèques** : libebur128 (MIT, sous-module), Signalsmith Linear (déjà là), FFmpeg et pyloudnorm pour contre-vérifier.
- **Fichiers** :
  - `engine/src/main/java/com/pocketstudio/engine/NativeEngine.kt` et `NativeSample.kt` ;
  - `app/src/main/java/com/pocketstudio/app/rhythm/RhythmViewModel.kt` (`describe` l.5968, `syncEngine` l.514, rendus `internal`) ;
  - nouveaux `app/src/test/.../bench/SongBenchExport.kt`, `engine/bench/*`, `scripts/song-bench.sh`, `.github/workflows/*`.
- **Gain audible** : aucun directement. Il rend chaque étape suivante prouvable.
- **Risque fil audio** : nul (hors ligne ; la couture n'est active qu'en test).
- **Critère** :
  - 2 rendus successifs ont la même empreinte ;
  - LUFS-I et TP à ±0,1 de `ffmpeg ebur128` ;
  - 72 morceaux rendus sans NaN ;
  - rapport de base commité (ce sont les chiffres « avant »).

### Étape 1 : gain staging, compresseur et saturation honnêtes, effort S
- **Quoi** :
  - Compresseur : paramètre **Makeup** (dB, 0 par défaut) + mode **Auto** (compense la réduction moyenne mesurée, lente, et rien quand rien ne comprime) ; détecteur RMS/crête ; passe-haut de détection 90 Hz ; **Mix** (parallèle) ; GR exposée par atomique et affichée.
  - Saturation : le passe-bas ne s'applique plus à 100 % (tonalité neutre = pas de filtre) ; courbe à antiderivée (ADAA) ou ×2 HIIR quand le drive dépasse 0,5.
  - Kit intégré « v2 » : `kLevels` ramenés près des cibles de rôle (charleys +15 à +20 dB, kick −6 dB). Les +9 dB d'étagère et les faders extrêmes de `WizardMix` disparaissent.
  - `WizardMix.withGlue` recalé : il ne compte plus sur le makeup fantôme.
- **Bibliothèques** : HIIR 1.50 (WTFPL) ou Signalsmith dsp `rates.h` (MIT).
- **Fichiers** : `engine/src/main/cpp/core/Effects.h/.cpp` (Compressor, Saturator, InsertSlot), `Mixer.h` (GR par slot), `DrumKit.cpp`, `project-model/.../Effects.kt` (paramètres ajoutés en fin de liste), `WizardMix.kt`, `app/.../mixer/MixerScreen.kt` (aiguille GR).
- **Gain audible** : moyen. Plus d'air (le passe-bas caché disparaît), limiteur moins sollicité, charleys propres.
- **Effort** : S.
- **Risque fil audio** : faible (DSP pur, atomiques).
- **Critère** :
  - chaînes par défaut sous le seuil : 0 ±0,2 dB et plates à ±0,3 dB de 20 Hz à 18 kHz (aujourd'hui +3,7 dB et −3,1 dB à 18 kHz) ;
  - Saturation drive 0,8 : repliement ≤ −50 dBc (aujourd'hui −21) ;
  - banc : air médian en kit ≥ −22 dB (aujourd'hui −25) ; réduction médiane du limiteur sur les Drops −3 dB.

### Étape 2 : chaîne master de mastering, true peak et cibles par genre, effort M
- **Quoi** : nouveau `MasterChain` qui remplace `Mixer::limit` :
  - EQ de pente et M/S léger ;
  - compresseur de bus lent (2:1, attaque 30 ms, relâche auto) ;
  - **écrêteur doux ×4** (Airwindows ClipOnly3/ADClip, avec HIIR) pour les 1 à 2 dB de crête du kick ;
  - **limiteur true peak** x42 peaklim (anticipation ≈ 1,2 ms, relâche auto, maintien sur grave fort) ;
  - plafond **−1,0 dBTP**.
  - Mesure **LUFS M/S/I + TP en direct** : le fil audio pousse la sortie dans un `SpscFloatRing`, un fil de mesure calcule avec libebur128, l'UI lit des atomiques.
  - Préréglages « Club », « Streaming −14 » et « Démo » : la cible de sonie vient du genre (§ 2.4).
- **Bibliothèques** : x42 dpl.lv2 v0.7.3 (GPL-3+) ou Signalsmith basics Limiter (MIT) + détection TP avec HIIR ; Airwindows (MIT) ; libebur128 (MIT).
- **Fichiers** : `engine/src/main/cpp/core/Mixer.h/.cpp`, nouveaux `MasterChain.h/.cpp` et `LoudnessMeter.h/.cpp`, `engine/third_party/{x42-dpl,hiir,airwindows}`, CMake (hôte + appareil), `Engine.cpp` (commandes), `NativeEngine.kt`, `project-model` (réglages du master), `MixerScreen.kt` (LUFS, TP, cible), `WizardMix.kt` (préréglage selon le style).
- **Gain audible** : fort. On entend le même volume que les références, sans grave déformé, et le M4A n'écrête plus.
- **Effort** : M.
- **Risque fil audio** : moyen. Le tampon d'anticipation et le suréchantillonnage sont préalloués, x42 alloue seulement dans `init`, `prepare` reste hors callback. CPU : la chaîne doit rester ≤ 5 % d'un cœur.
- **Critère** :
  - banc : **TP ≤ −1,0 dBTP sur 100 % des morceaux** (aujourd'hui jusqu'à +0,7) ;
  - LUFS-I à ±1 LU de la cible du genre avec « Club » ; PLR ≥ 6 ;
  - sinus 50 Hz à 6 dB de réduction : DHT < 0,3 % ;
  - chaîne master sur sinus 7 kHz à +6 dB : repliement ≤ −70 dB.

### Étape 3 : batterie de qualité studio (kits échantillonnés par genre, kick refait, couches), effort L
- **Quoi** :
  - **Packs « Kits »** : nouveau `scripts/packs/kits.json` + `kit_pack.py` sur le modèle de `library_pack.py`.
    - Sources : Sonic Pi CC0, Freesound CC0 sélectionné par l'API (licence vérifiée fichier par fichier, auteur et URL gardés), Virtuosity Drums CC0 (boom bap, lo-fi, breaks), VCSL, kicks rendus hors ligne depuis Geonkick (GPL-3).
    - Traitement : tri, coupe, fondu, **normalisation en sonie par rôle** (cibles `ROLE_LUFS`, pas la crête à −1 dBFS), FLAC 24 bits à 48 kHz.
    - Contenu : 2 à 4 variantes (round-robin) et 2 couches de vélocité par voix.
  - **Moteur** :
    - `Sampler` avec round-robin et couches de vélocité par pad (tirage par le xorshift du moteur, déterministe) ; vélocité → gain et brillance ;
    - kit intégré v2 : kick refait (enveloppe de hauteur en deux temps ≈ 250 → 55 Hz, durée liée au tempo, 300 à 450 ms ; couche de clic 2–5 kHz ; écrêtage doux) ; charleys et cymbales remplacés par de vrais échantillons CC0 intégrés à l'APK (quelques centaines de Ko).
  - **Couches** : l'assistant pose « kick sub + kick attaque » et « clap + caisse » sur deux pads du même groupe d'étouffement, selon le genre.
  - Les packs SAO deviennent une famille « Sons générés » ; ils ne sont plus choisis par défaut pour la batterie.
- **Fichiers** : `DrumKit.h/.cpp`, `Sampler.h/.cpp`, `scripts/packs/{kits.json,kit_pack.py,build_index.py,rank_sounds.py}`, `project-model/.../{PackPicks.kt,StarterWizard.kt,WizardMix.kt (KIT_LUFS remesurés),SoundPacks.kt}`, THIRD_PARTY.md (une ligne par source, auteurs dans `pack.json`).
- **Gain audible** : **le plus fort de tout le plan**.
- **Effort** : L.
- **Risque fil audio** : faible à moyen (plus de voix, tout préalloué ; tirage sans `rand()`).
- **Critère** :
  - banc, kick : 40–80 Hz dominant, 2–5 kHz ≥ −25 dB rel. dans les 20 premières ms, −40 dB atteint en ≤ 500 ms à 128 BPM ;
  - air du Drop entre −20 et −16 dB en kit (aujourd'hui −25 à −33) ;
  - pente spectrale −4,5 ±0,7 dB/oct ;
  - écoute en aveugle (Benjamin + 2 sous-agents sur rendus) : v2 préférée ≥ 80 %.

### Étape 4 : stéréo et espace, effort M
- **Quoi** :
  - **Instruments en stéréo** :
    - `Synth::render(L, R)` avec copies d'unisson réparties (paramètre Largeur, copies impaires à gauche, paires à droite, oscillateur 1 au centre) ;
    - `SampleInstrument` garde la stéréo ;
    - Plaits en stéréo par `out`/`aux` ou ensemble ;
    - TinySoundFont en `TSF_STEREO_INTERLEAVED`.
    - Le mixeur sait déjà traiter une voie stéréo (`rights[ch]`).
  - **Ensemble** Plaits (MIT, déjà vendoré) sur nappes et cordes.
  - **Réverbes** :
    - « Plate » = Mutable Clouds/Elements (MIT, quasi gratuite en CPU) ;
    - « Salle » = DaisySP-LGPL `ReverbSc` ou Airwindows kPlate240/Galactic3 ;
    - Freeverb gardée pour les anciens projets.
    - Retour avec **passe-haut 250 Hz et passe-bas 9 kHz** par défaut, pré-délai calé au tempo (1/64–1/32), pompe possible sur le retour.
  - **Bas mono** sur le master (< 120 Hz).
  - Envois par rôle dans `WizardMix` (caisse claire et clap 10–15 %, lead 15 %, nappes 25 %, percus 10 %).
- **Fichiers** : `Synth.h/.cpp`, `InstrumentTrack.h`, `SampleInstrument.h/.cpp`, `PlaitsSynth.h/.cpp`, `SoundFontPlayer.cpp`, `Engine.cpp` (tampons stéréo par piste), `Mixer.cpp`, `Effects.h/.cpp` (types de réverbe, filtres de retour), `engine/third_party/mutable` (ajout de `clouds/dsp/fx`, README), `project-model/.../{Effects.kt,WizardMix.kt}`.
- **Gain audible** : fort. Large, profond, « produit ».
- **Effort** : M.
- **Risque fil audio** : moyen. Tampons supplémentaires préalloués ; CPU du synthé ×1,3 à ×2 en unisson stéréo, à mesurer.
- **Critère** :
  - banc Drop : côté/centre entre −15 et −9 dB (aujourd'hui −14 à −30) ; corrélation 0,3–0,85 ;
  - < 120 Hz : corrélation ≥ 0,95 ;
  - perte mono ≥ −1,5 LU ;
  - retour réverbe < 200 Hz ≤ −20 dB.

### Étape 5 : vrai sidechain et bus « Musique », effort M
- **Quoi** :
  - La Pompe devient un **sidechain audio** : clé = voie du kick (pad, piste ou clip choisi), suiveur d'enveloppe à attaque et relâche en ms, 4 formes de courbe, anticipation de 2 ms.
  - Le mixeur traite les voies clés en premier dans chaque bloc.
  - Mode « chaque temps » gardé.
  - Bus de groupe **Musique** (tout sauf la batterie) : une seule pompe pour basse + sub + nappes, au lieu de deux pompes non coordonnées.
  - Pompe possible sur les retours réverbe et écho.
- **Fichiers** : `Effects.h/.cpp` (Ducker → SidechainDucker), `Mixer.h/.cpp` (ordre des voies, bus de groupe, clé), `Engine.cpp:661` (ne plus dépendre de la voie du pad 1), `project-model/.../{Effects.kt (source de la clé),WizardMix.kt,StarterWizard.kt:782}`.
- **Gain audible** : moyen à fort. Kick net, groove qui respire, plus de double pompe.
- **Effort** : M.
- **Risque fil audio** : moyen (ordre de traitement, aucune allocation).
- **Critère** :
  - test : un kick sur n'importe quel pad, piste ou échantillon fait baisser la basse de 4 à 8 dB en ≤ 10 ms ;
  - banc (stems) : énergie < 100 Hz de la basse dans les 50 ms après le kick ≥ 6 dB sous la moyenne ;
  - aucun double ducking.

### Étape 6 : production dans l'assistant, effort M
- **Quoi** (`StarterWizard`, `WizardMix`, recettes) :
  - **Vélocités humaines** : graine fixe, ±5 à 8 % sur charleys et percus, accents sur le temps et le contretemps, notes fantômes de caisse claire en hip-hop et DnB.
  - **Micro-timing** : 0 sur le kick, ±2 à 5 ms sur charleys et percus, plus en hip-hop et lo-fi.
  - **Fills à 8 et 16 mesures** : roulement 1/16 puis 1/32, toms, kick coupé, plus gros que celui des 4 mesures.
  - **Crash ou impact échantillonné** au 1er temps de chaque Drop et bloc de 16 ; **reverse cymbal** avant les Drops ; **downlifter** à l'entrée des Breaks.
  - **Montée sur 8 mesures** : souffle + passe-haut sur le bus Musique + envoi de réverbe croissant + roulement accéléré.
  - Courbe d'énergie cible par forme (Intro −6, Groove −2, Drop 0, Break −6 LU) vérifiée par le banc.
  - Variantes A/B de charley et de percu toutes les 8 mesures.
  - Lancer de réverbe (`kThrowMacro`) sur le dernier temps avant un Drop.
  - Recettes : champs `fills`, `fx_hits`, `humanize` dans `scripts/packs/recipes/*.json` + `recipes_to_kotlin.py`.
- **Fichiers** : `project-model/.../{StarterWizard.kt,WizardMix.kt,StyleRecipes.kt (généré),Automation.kt,NoteEdits.kt (humanize existant)}`, recettes, pack « FX de transition » (Freesound CC0).
- **Gain audible** : fort. Le morceau vit et les transitions décollent.
- **Effort** : M.
- **Risque fil audio** : nul (Kotlin ; le séquenceur est déjà à l'échantillon près).
- **Critère** :
  - banc : Drop − Groove ≥ +2 LU, Drop − Break ≥ +4 LU, montée ≥ +3 LU sur ses 4 dernières mesures ;
  - ≥ 6 mesures de batterie distinctes par morceau ;
  - écart-type des vélocités de charley ≥ 0,05 ;
  - jamais deux blocs de 8 mesures identiques sur toutes les pistes.

### Étape 7 : meilleures sources de synthé et de basse, effort M à L
- **Quoi** :
  - Anti-repliement : synthé (oscillateurs PolyBLEP → filtre et étage non linéaire ×2 avec HIIR, ou oscillateurs à table limitée en bande) ; Distorsion ×4 ; Bitcrusher inchangé (repli voulu).
  - Filtres du synthé : modèles sst-filters (GPL-3) au choix (ladder, OB-Xd 12/24, diode).
  - Moteur « 808 / Sub » dédié : sinus + saturation ADAA + glide + enveloppe de hauteur ; accordé, stéréo à 0 sous 120 Hz.
  - **Instruments multi-échantillons** :
    - `SampleInstrument` multi-zones (plages de notes et de vélocité, boucles) avec un analyseur de sous-ensemble SFZ en Kotlin ;
    - sources : Splendid Grand Piano (domaine public), Salamander (CC-BY), Meatbass (CC0), VSCO-2-CE (CC0) ;
    - sfizz 1.2.3 (BSD-2) seulement si le sous-ensemble ne suffit pas.
  - L'assistant préfère ces sons aux GM de TinySoundFont.
- **Fichiers** : `Synth.h/.cpp`, `ExtraEffects.cpp` (Distortion), `Effects.cpp` (Saturator), `SampleInstrument.h/.cpp`, `engine/third_party/{hiir,sst-filters}`, `scripts/packs` (packs instruments), `project-model/.../{SoundLibrary.kt,SynthPatch.kt,StarterWizard.kt}`.
- **Gain audible** : moyen à fort sur les styles mélodiques (pop, house, lo-fi, trance).
- **Effort** : M à L.
- **Risque fil audio** : moyen (CPU des filtres et du suréchantillonnage ; voix préallouées).
- **Critère** :
  - repliement scie Do6–Sol7 ≤ −60 dB (aujourd'hui −37 à −53) ;
  - Distorsion ≤ −50 dBc (aujourd'hui −30/−34) ;
  - piano échantillonné : 8 couches de vélocité au moins ;
  - CPU synthé 8 voix stéréo ≤ 3 % d'un cœur (projeté) ;
  - banc : parties mélodiques à leur place (§ 2.3).

### Étape 8 : boutons « Équilibrer mon mix » et « Masteriser », effort M
- **Quoi** :
  - Le code de mesure du banc est porté dans le cœur (`core/LoudnessMeter`, `core/Analysis`) et partagé entre le banc et l'appli.
  - **Équilibrer** :
    - rendu hors ligne de 8 mesures du Drop, piste par piste ;
    - LUFS et bandes de chaque stem ;
    - trims vers les cibles de rôle, passe-haut par rôle (nappes 150 Hz, lead 200 Hz, charleys 300 Hz), bas mono, sidechain posé, envois par défaut ;
    - une seule étape d'annulation, et une carte de conseils en mots simples (« ta basse cache ton kick à 60 Hz »).
  - **Masteriser** :
    - rendu du morceau pré-master ;
    - EQ vers la courbe du genre (±3 dB au plus), compresseur de bus, écrêteur, limiteur TP ;
    - gain cherché en 2 ou 3 passes hors ligne pour la cible ;
    - rapport « −8,1 LUFS, crête −1,0 dBTP : comme en club ».
  - Principe de Matchering (sonie + EQ vers une cible + limiteur), sans Python.
- **Fichiers** : nouveaux `project-model/.../AutoMix.kt` (logique pure, testée), `core/LoudnessMeter.*`, `Analysis.cpp`, `RhythmViewModel.kt` (tâches hors ligne), `MixerScreen.kt`, `WizardMix.kt` (l'assistant s'en sert à la création).
- **Gain audible** : fort pour tout projet modifié après l'assistant.
- **Effort** : M.
- **Risque fil audio** : faible (moteur hors ligne).
- **Critère** :
  - banc : après Équilibrer, chaque partie est à ±1,5 LU de sa cible ;
  - après Masteriser : LUFS-I à ±0,5 de la cible, TP ≤ −1,0, PLR ≥ 6, bandes à ±3 dB de la courbe ;
  - durée sur le téléphone ≤ 20 s pour un morceau de 3 min.

### Étape 9 : export propre, effort S
- **Quoi** :
  - contrôle TP avant AAC (plafond −1 dBTP, ou −1,5 pour le M4A) ;
  - rapport de fin (LUFS-I, TP, LRA, PLR) ;
  - option 44,1 kHz (libsamplerate SINC_BEST, déjà là) ;
  - 16 bits avec dither TPDF ou à mise en forme du bruit (Airwindows NotJustAnotherDither, MIT) ;
  - normalisation optionnelle à la cible.
- **Fichiers** : `WavWriter.cpp`, `OfflineRenderer.cpp`, `app/.../export/{AacEncoder.kt,Loudness.kt,WavExporter.kt}`, `MixerScreen.kt` (carte d'export).
- **Gain audible** : faible, mais plus aucun écrêtage du fichier partagé.
- **Effort** : S.
- **Risque fil audio** : nul.
- **Critère** :
  - M4A décodé : crête ≤ 0 dBFS, 0 échantillon écrêté ;
  - rapport égal à ffmpeg à ±0,1 près.

### Ordre et dépendances

```
0 banc ─┬─ 1 gain staging ── 2 master ──┬── 8 auto mix / master ── 9 export
        ├─ 3 batterie ───────────────────┤
        ├─ 4 stéréo et espace ───────────┤
        ├─ 5 sidechain ──────────────────┘
        ├─ 6 production (en parallèle de 3 et 4)
        └─ 7 synthés (après 4 : stéréo du synthé)
```

- Suggestion de phases :
  - phase A = 0 + 1 + 2 (mesurable, gros gain rapide) ;
  - phase B = 3 ;
  - phase C = 4 + 5 ;
  - phase D = 6 ;
  - phase E = 7 + 8 + 9.
- Gain attendu, à confirmer au banc :
  - après A : le volume et le haut du spectre des références ;
  - après B et C : 70 à 80 % de l'impression d'un vrai morceau ;
  - après D et E : morceaux « présentables ».

### Ce que Benjamin aura à faire (une seule demande groupée)

- Écouter les A/B avant/après de chaque phase sur son téléphone et au casque. Les fichiers sont joints à la PR, avec les mêmes morceaux rendus par le banc.
- Optionnel : donner 2 ou 3 titres de référence par style qu'il aime. Ils restent chez l'agent : seules les courbes mesurées entrent dans le dépôt.
- Aucun compte ni licence payante n'est nécessaire. Une clé API Freesound gratuite serait utile pour le script des kits ; sinon, sélection manuelle.

---

## Sources

Licences et versions de code : dépôts git clonés le 2026-10-23 (fichiers LICENSE / COPYING / en-têtes et étiquettes), y compris airwindows/airwindows, x42/dpl.lv2, Signalsmith-Audio/basics et dsp, pichenettes/eurorack, electro-smith/DaisySP-LGPL, surge-synthesizer/*, Chowdhury-DSP/chowdsp_utils, jatinchowdhury18/AnalogTapeModel, lsp-plugins/*, michaelwillis/dragonfly-reverb, ValdemarOrn/CloudSeed, TheWaveWarden/odin2, mtytel/vital, asb2m10/dexed, google/music-synthesizer-for-android, sfztools/sfizz, sergree/matchering, jiixyj/libebur128, csteinmetz1/pyloudnorm, sonic-pi-net/sonic-pi (`etc/samples/README.md`), sfzinstruments/{virtuosity_drums,SalamanderGrandPiano,SplendidGrandPiano,karoryfer.meatbass,jlearman.jRhodes3d}, sgossner/VSCO-2-CE, gitlab.com/quamplex/geonkick.

Pages web :
- [HIIR, Laurent de Soras](https://ldesoras.fr/prod.html) (v1.50 ; archive `hiir-1.50.zip` : licence WTFPL v2, NEON ARM64)
- [Licence 99Sounds](https://99sounds.org/license/) (redistribution interdite)
- [Freesound, recherche « 909 kick » filtrée CC0](https://freesound.org/search/?q=909+kick&f=license%3A%22Creative+Commons+0%22) (89 résultats)
- [Debian hydrogen-drumkits copyright](https://metadata.ftp-master.debian.org/changelogs/main/h/hydrogen-drumkits/stable_copyright)
- [VSCO-2 Community Edition](https://versilian-studios.com/vsco-community/)
- [Salamander Grand Piano (sfzinstruments)](https://github.com/sfzinstruments/SalamanderGrandPiano)
- [Wiki des kits DrumGizmo](https://drumgizmo.org/wiki/doku.php?id=kits)
