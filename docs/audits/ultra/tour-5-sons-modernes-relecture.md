# Tour 5, lot « sons-modernes » : relecture neuve de l'étude des sons (2026-11-12)
Relu : cacaea09a8fcd16cfc874f579a692534818f8cc8

Objet : `docs/sons/PLAN-SONS-MODERNES.md` et `docs/decisions/tour-5-sons-modernes.md` (commit cacaea09). Relecteur neuf, qui n'a pas écrit l'étude. Rien n'a été modifié dans ces deux fichiers.

## Bloquant

Aucun.

## Grave

### G1. L'appli a déjà un OTT : l'étude dit le contraire et en fait sa première brique moteur
- Passages :
  - En bref, point 1 : « sans tables d'ondes ni compression multibande, qui font le son d'aujourd'hui » ;
  - § 2.1, point 3 : « **la compression multibande « OTT »**, utilisée sur presque toutes les basses, leads et nappes d'aujourd'hui. […] On ne l'a pas ; » et « le travail du son en couches : distorsion, puis OTT, puis réenregistrement » ;
  - § 5, tableau : « **un OTT mesuré à 1 dB près de l'original** : le gain « son 2020 » le plus fort pour le moins de travail » ; § 5, Avis : « 1. un **OTT** (code Faust MIT, sans allocation dans le fil audio, testé sur Linux) ; » ;
  - Essai 3 : « l'OTT en effet (sur la basse, le lead et le bus musique) ; » ;
  - Décisions, ligne 4 : « synthés archétypes 1983-2000 sans tables d'ondes ni OTT » ; ligne 6 : « Briques retenues pour le moteur : Faust `co.xfer_ott` (MIT) ».
- Ce que dit le code :
  - le moteur a une classe `Ott` (`engine/src/main/cpp/core/Effects.h:608-612` : trois bandes coupées à 120 Hz et 2,5 kHz, compression vers le haut et vers le bas ; `OttFx = 24`, l. 757) ;
  - l'appli l'offre sous le nom « OTT multibande » (`project-model/…/Effects.kt:302`) et le glossaire l'explique (`SoundGlossary.kt:128`) ;
  - l'assistant le pose déjà : sur le master du jump up et de la hard techno (`StyleRecipes.kt:987`, `:669`), sur le master de toute la famille Bass music (`WizardMix.kt:195-196`) ;
  - il est dans les réglages d'effets « Growl », « Basse grasse », « Wob dubstep », « Pompe EDM » (`FxPresets.kt:92-116`). La basse growl du jump up passe déjà par « distorsion, puis OTT ».
  - Ce qui manque vraiment : l'OTT n'est jamais posé sur les nappes et les leads de house et de techno.
- Correction proposée :
  - cause 3 : « l'appli a déjà un OTT, mais il ne sert qu'au master de la bass music et de la hard techno et aux basses « growl » et « grasse », jamais aux nappes et leads de house et de techno » ;
  - essai 3, première étape : poser l'OTT existant sur les nappes, les leads et le bus de house et de techno (recette seulement, sans code moteur), puis mesurer ;
  - `co.xfer_ott` devient une comparaison facultative avec notre `Ott`, seulement si une mesure le montre meilleur. Ce n'est plus « le gain le plus fort pour le moins de travail » ;
  - effort de l'essai 3 à revoir à la baisse ; lignes 4 et 6 des décisions à aligner ;
  - « utilisée sur presque toutes les basses, leads et nappes » : sans source, à adoucir (« très courante »).

### G2. « Les packs ont été demandés rétro » : phrase plus forte que la preuve, et mauvaise cible pour l'essai 2
- Passages :
  - En bref, point 1 : « Les packs téléchargeables ont été générés en demandant littéralement « rave des années 90, TR-909, Juno-106 ». » ;
  - § 2.1, point 2 : « **Les packs ont été demandés « rétro ».** Les mots donnés au générateur Stable Audio contiennent « early 90s Belgian and Dutch rave », « Roland TR-909 », « Juno-106 », « DX7 » » ;
  - Essai 2 : « on retire « 90s », « TR-909 », « Juno-106 » des demandes » ;
  - Décisions, ligne 4.
- Ce que dit `scripts/packs/styles.json` (54 packs) :
  - « early 90s » n'apparaît que dans le pack « Rétro techno » ;
  - « Roland TR-909 », « Juno-106 » et « DX7 » sont les mots des packs **qui portent ces noms** (famille « Machines »). Ces packs sont faits exprès pour imiter ces machines ;
  - les packs des styles de l'essai n'ont aucun de ces mots : house « classic Chicago house », techno « techno Berlin », hard techno « hard techno industrial distorted », drum & bass « drum and bass ». Le jump up n'a pas de pack à lui.
- Le vrai chemin du « rétro » par les packs, ce sont les recettes :
  - la house prend son kick, son clap et ses charleys dans le pack TR-909, sa percussion dans le TR-707 et sa basse dans le Juno-106 (`StyleRecipes.kt:246`, `:252`) ;
  - la techno prend son charley ouvert dans le TR-909 (`:542`) ;
  - surtout, la recette House vise exprès « La house classique, née à Chicago », avec des références de 1984 à 1992 (`StyleRecipes.kt:229-249`).
- Correction proposée :
  - réécrire En bref 1 et § 2.1 point 2 : « certains styles prennent leurs sons dans les packs de machines anciennes (la house : batterie TR-909, percussion TR-707, basse Juno-106) ; seul le pack Rétro techno est demandé « années 90 » ; les packs Techno, Hard techno et Drum & bass n'ont pas ces mots. La recette House vise exprès la house de Chicago des années 80-90 » ;
  - essai 2 : régénérer le pack House avec des mots d'aujourd'hui et changer ce que la recette House va chercher. Il ne faut pas retirer « TR-909 » des demandes : le pack TR-909 perdrait sa raison d'être. Pour techno, hard techno et drum & bass, dire que seule la qualité du modèle est en cause, pas les mots ;
  - question 3 : signaler que la House de l'appli est classique par choix, et proposer la tech house, ou une recette House moderne, comme style d'essai.

### G3. Le branchement de l'essai 1 est faux pour la house et le jump up
- Passages :
  - Essai 1 : « **Branchement** : par le mécanisme qui existe déjà. Le « pack du style » passe avant le kit synthétisé, et chaque rôle (kick, clap, charley…) prend le meilleur son du pack. » ;
  - § 6 : « Les recettes ne changent pas tant que tu n'as pas dit « mieux ». »
- Ce que dit le code :
  - `soundCategories` (`app/…/library/SoundLibraryViewModel.kt:217-235`) range d'abord le pack de machine nommé par la recette (`style.kit[role]`), puis le pack du style, puis le kit (`first + own + kit + …`) ;
  - le son par défaut est le premier de la première catégorie (`defaultRow`, l. 1173-1180) ;
  - pour les parties, le pack de `packSounds` passe en premier (`StarterWizard.soundsFor`, l. 502-546) : la basse de la house vient de `juno:bass` ;
  - conséquence : un pack d'essai « house » ne serait **pas** choisi pour le kick, le clap, les charleys, la percussion ni la basse tant que les packs TR-909, TR-707 et Juno-106 sont sur le téléphone ou installés au banc ;
  - le jump up n'a pas de pack à lui : sa recette envoie le kick, la caisse, les charleys et les percussions vers le pack « dnb » (`kit = mapOf(Role.Kick to "dnb", …)`).
- Correction proposée :
  - dire que l'essai change, derrière un interrupteur d'essai, le choix des packs dans les recettes (House : `kit` et `packSounds` ; Jump up : `kit`). Autre voie : le banc n'installe que les packs d'essai, en nommant leurs identifiants ;
  - remplacer « Les recettes ne changent pas » par « les notes et la structure ne changent pas ; pour l'essai, seul change le pack où la recette prend ses sons ».

## Moyen

- **M1. Mauvais témoin pour le processeur.**
  - Passages : « **Hard techno** (témoin pour le processeur) » et « rien de plus en hard techno. Sur le téléphone Honor, elle fait déjà 44 % en moyenne et 67 % au pire moment ».
  - Le même rapport (`tour-5-hardtechno-rapport.md`, l. 5) donne le jump up plus lourd : 60 % en moyenne et 74 % au pire. Le jump up est un style de l'essai, et l'essai 3 y ajoute de l'OTT et des tables d'ondes. Le rapport dit aussi que ce « pire bloc » est lissé sur 0,5 s et plafonné.
  - Correction : prendre le jump up comme témoin de charge (la hard techno reste le témoin des décrochages), avec une règle chiffrée pour lui, et préciser ce que mesure le « pire bloc ».
- **M2. Les mesures « avant » ne se refont pas avec l'outil nommé.**
  - Passages : § 2.2, « Air = la part du son au-dessus de 10 kHz » et « Rendus effacés après mesure » ; « Ce qu'on mesure », « Banc d'export (`scripts/song-bench.sh`) : […] air au-dessus de 10 kHz ; grave-médium face au médium-aigu ; […] chaque voix rendue seule ».
  - Dans le banc, « air » couvre 6 à 20 kHz (`engine/bench/ps_measure.cpp:297`), aucune bande ne va de 150 à 500 Hz ni de 1 à 4 kHz, et `song-bench.sh` ne rend seuls que le lead, la basse, le kick et la batterie, jamais la nappe. Le calcul utilisé n'est pas commité.
  - Correction : commiter le petit outil de mesure avec un test, ou ajouter ces bandes à `ps_measure` avec un test, et garder le JSON « avant ». Sinon, refaire les chiffres avec les bandes du banc. Règle de CLAUDE.md : l'outil de mesure se vérifie avant de mesurer.
- **M3. « Trop sombre » n'est jugé que face à une scie brute.**
  - Passages : § 2.1 point 4, « Les nappes et les leads de house et de techno sont filtrés sombres » ; tableau, « sombre et boueuse : 15 à 20 dB plus sombre qu'une scie brute » ; essai 3, « ouvrir les nappes et leads trop sombres ».
  - Une scie sans filtre n'est pas une cible : une nappe de techno d'aujourd'hui peut être sombre elle aussi.
  - Correction : présenter cette cause comme une piste à confirmer à l'oreille, ou la mesurer sur des morceaux de référence récents (`docs/hits/`).
- **M4. Les questions ne suivent pas CLAUDE.md.**
  - Règle du 2026-10-24 : ne demander à Benjamin que ce qu'il est seul à pouvoir faire.
  - Les questions 2 (taille de l'appli) et 3 (styles de l'essai) sont des choix à prendre soi-même, avec l'option conseillée notée dans les décisions. La 1 (de l'argent) et la 4 (son écoute, l'APK) peuvent rester.
  - Format : la question 3 n'a de conséquence pour aucune option, et 1A n'en a pas d'écrite.
  - Correction : décider 2A et 3A dans `docs/decisions/tour-5-sons-modernes.md`, garder 1 et 4, et donner à chaque option une conséquence en une ligne.
- **M5. La raison donnée pour écarter la table d'ondes de Plaits est fausse.**
  - Passage : « Il imite volontairement un convertisseur 5 bits : c'est un son lo-fi, pas un Serum. »
  - Le 5 bits ne touche que la sortie AUX (`wavetable_engine.cc` : `*aux++ = …(int)(mix * 32.0f) / 32.0f`). L'appli joue la sortie principale `frames[i].out` (`PlaitsSynth.cpp:158-160`), qui lit les tables avec interpolation. Le côté lo-fi ne vient que de la moitié non interpolée du bouton HARMONICS.
  - Correction : corriger la phrase, et essayer à 0 € la table d'ondes de Plaits (modèle 13) et le « Terrain d'onde » (modèle 5) avant d'écrire un oscillateur.

## Mineur

- **m1.** § 1, tableau, ligne Basse : « seulement en house (Juno) et en techno ».
  - Avec les packs, la basse en échantillon passe aussi en premier en boom bap, lo-fi, tech house, UK garage et neurofunk (`packSounds`, `StyleRecipes.kt`).
  - Correction : « dans 7 styles, dont house et techno ».
- **m2.** § 1, tableau : « la ride et le crash sont la cymbale du kit raccourcie ».
  - Seule la ride est raccourcie (`kitShaped`, `Role.Ride`). Le crash est la cymbale du kit telle quelle.
- **m3.** Essai 3 : « nouveaux sons de bibliothèque : basse « reese » moderne ».
  - Une « Reese moderne » existe déjà (`SoundLibrary.kt:73`).
  - Correction : « retoucher la Reese moderne existante ».
- **m4.** § 3.1, Signature Sounds : « « License: CC0 (Free to use, no restrictions) » sur 34 des 36 packs visibles ».
  - À ma lecture (2026-11-12), la ligne « License: » ne figure que sur 18 packs. Sur 16 autres, « CC0 » n'est que dans la description. L'un des packs est payant (Signature Soundbank, 9,99 £). La plupart des packs visibles sont des bruitages et des prises de son en extérieur.
  - Correction : « 34 packs annoncent du CC0, dont 18 avec une ligne de licence » ; liste des styles couverts à vérifier.
- **m5.** Estimations pas toujours présentées comme telles :
  - efforts « environ 2 à 3 jours d'agent », « environ 1 jour », « environ 1 à 2 semaines » ;
  - tailles « environ 2 à 5 Mo par style », « environ +5 à 10 Mo », « moins de 1 Mo » ;
  - En bref : « environ 500 à 1 500 $ » ; question 1B : « Jusqu'à environ 1 500 $ » ; question 2B : « +5 à 10 Mo ».
  - Correction : ajouter « (estimation) » partout, comme dans le tableau du § 4.
- **m6.** Décisions, ligne 5 : « les échantillons allègent le processeur au lieu de le charger ».
  - Rien ne le mesure, et le plan lui-même dit « allège plutôt ».
  - Correction : « devraient alléger (à mesurer) ».
- **m7.** Un dossier neuf `docs/sons/` à côté de l'existant `docs/son/` (MELODIE.md, banc/).
  - Correction : ranger l'étude dans `docs/son/`.
- **m8.** WaivOps EDM-TECH (zenodo 17584890) : les boucles viennent « mainly from drum machines », avec un sous-style « 90s ».
  - Correction : préciser qu'il faut filtrer sur le sous-style « modern ». La citation « may contain minor … audio defects » vient de la page EDM-HSE, pas des quatre.
- **m9.** Fil audio : les tables d'ondes (AKWF) se préparent hors du rappel audio, comme `AcidTrack::prepare` construit les siennes.
  - Correction : l'écrire à côté de l'essai 3, comme c'est fait pour le kick transposé.
- **m10.** § 3.2, Splice « interdit pour une appli ».
  - La conclusion tient : 2.3.1.4 (d), « redistribute Sounds in new sample packs », et (a), les sons isolés comme matière pour d'autres échantillons. Mais 2.3.1.3 autorise les sons dans un jeu vidéo, s'ils sont fondus dans une œuvre.
  - Correction : citer la bonne clause pour éviter un contresens.

## Vérifié sans constat

- **Code :**
  - `DrumKit.h` : « 16 synthesized drum voices (808/909 flavour), no samples » ;
  - `soundsFor` : le pack du style passe avant le son du synthé ;
  - le kick d'un pack garde son pad, et le kick studio n'est posé que sur la voix du kit (`withStudioVoices`, l. 822-825) ;
  - l'assistant ne prend jamais de SoundFont GM : `InstrumentChoice` n'est créé que dans `InstrumentPanel` ;
  - synthé : scie, carré et triangle sur les oscillateurs, sinus seulement sur le sub et la FM, pas de table d'ondes ;
  - `MidiFile.importInto` : canal 10 sur les pads, le reste dans le piano roll ;
  - le modèle Plaits 13 « Table d'ondes » n'est utilisé par aucun son de la bibliothèque ;
  - 37 styles ; kick de la techno vers 44 Hz ; « Piano FM » = E.PIANO 1.
- **Chiffres :**
  - APK 47,8 Mo : c'est bien la dernière Release (v0.22.219, 47 844 446 octets) ;
  - hard techno 44 % et 67 % ;
  - mélodies à 91-100 % de notes de l'accord (`MELODIE.md`).
- **Licences lues sur le web le 2026-11-12 :**
  - Stability AI : seuil de 1 M$, inscription obligatoire pour un usage commercial, « You own any outputs », « Powered by Stability AI », mise à jour du 5 juillet 2024 ;
  - 99Sounds : citation exacte ; Splice 2.3.1.4 (d) : citation exacte ;
  - free-midi-chords : MIT, plus de 13 000 fichiers, 195 suites par tonalité ;
  - Groove MIDI Dataset : CC BY 4.0, 13,6 h ;
  - Vital EULA : les deux citations sont exactes ;
  - FAQ Surge XT : citation exacte ;
  - `co.xfer_ott` : `declare xfer_ott license "MIT"` (auteur David Braun, `compressors.lib`, l. 1898-1899), « 1 dB at most » ;
  - AKWF : CC0 ; WaivOps : CC BY 4.0 ; Freesound : « You could even sell the sound ».
- **Commit cacaea09 :** deux fichiers ajoutés, tous deux de la documentation, en mode 100644. Aucun lien symbolique, aucun son, aucun fichier de l'appli, du moteur, du banc ni des tests.
- **CLAUDE.md :** pas d'APK sans demande, « sans allocation dans le fil audio » écrit pour l'OTT, « En bref » en 5 points.

Verdict : 0 bloquant, 3 grave
