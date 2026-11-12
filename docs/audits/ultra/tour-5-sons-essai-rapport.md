# Rapport du lot « sons-essai » (tour 5) : mêmes morceaux, sons modernes, derrière un interrupteur caché

Consigne (discussion principale, réponses de Benjamin 1A, 2A, 3A, 4A au plan `docs/sons/PLAN-SONS-MODERNES.md`) :
- l'essai du § 6 sur jump up, tech house et techno ;
- mêmes notes, seuls les sons changent, derrière un interrupteur invisible et coupé par défaut ;
- 0 €, aucun son dans le dépôt ;
- six extraits de 30 s envoyés à Benjamin.

## Branche

`ccr-09a30009-jy4aua`. Elle part de la dernière `ccr-80af1a1b-mn0wec` (2cfb8a3a, puis d12ad4a8 fusionnée) et contient aussi le lot « sons-modernes » (l'étude). Celui-ci n'était pas encore fusionné, et c'est la seule branche où je peux pousser : le repartir de zéro l'aurait effacé. Fusionner « sons-essai » apporte donc aussi « sons-modernes ».

## Ce qui est fait

- **Interrupteur caché**, `SoundTrial` (`project-model/…/SoundTrial.kt`) :
  - il est lu à chaque appel de `StarterWizard.style`, et ne s'allume que si la JVM est lancée avec `ps.sons.essai=1` (le banc, les tests ; `-Psons.essai=1` côté Gradle) ;
  - aucun écran, réglage ni fichier de l'appli ne l'allume ;
  - coupé, `style()` rend la recette elle-même (le même objet).
- **Interrupteur mis**, seuls jump up, tech house et techno changent :
  - **batterie** : leurs lignes prennent les packs d'essai `essai_<style>` à la place des packs de machines (TR-909…) et du kit synthétisé. En tech house, la percussion perd sa voix de kit imposée ; son shaker (deuxième percussion) garde la sienne. La techno garde son rumble studio accordé ;
  - **sons de la bibliothèque** : le jump up passe aux accords « future bass » (nappe) et à la supersaw (lead, au lieu du hoover de 1991) ;
  - **OTT** (compresseur trois bandes déjà dans le moteur) sur la nappe et le lead des trois styles ;
  - le reste de la recette ne change pas : lignes, notes, forme, tempo, effets d'origine.
  - Le « Pluck future » essayé sur la nappe techno a été retiré : il mesurait plus sombre que le pluck d'origine (voir les mesures).
- **Batterie d'essai fabriquée par nous** (`scripts/packs/trial_drums.py`, test `scripts/packs/test_trial_drums.py`) :
  - kicks en couches, accordés sur la tonique du style (techno Fa1 43,65 Hz, tech house Sol1 49 Hz), avec clic et saturation suréchantillonnée ×4 ;
  - grosse caisse claire de DnB, claps en salves avec pièce stéréo, charleys et rides métalliques étalés en stéréo, bois, rim et shaker ;
  - WAV 16 bits 48 kHz, déterministes, fabriqués hors dépôt.
  - Pourquoi pas les sources de l'étude : Signature Sounds exige un passage en caisse avec une adresse e-mail, Freesound une clé, et les téléchargements de Surge XT sur GitHub sont bloqués par le réseau du conteneur. La décision et la licence sont notées (`docs/decisions/tour-5-sons-essai.md`, `THIRD_PARTY.md`).
- **Le banc entend ces sons pour de vrai.** `BenchFlac` donne désormais à Robolectric les WAV 16 bits d'un pack avec leurs propres échantillons. Les FLAC restent une tonalité de remplacement, comme avant.

## Preuves

- **Rendu par défaut identique.**
  - `SoundTrialTest#offEveryStyleIsItsVeryRecipe` : coupé, chaque style est sa recette.
  - `SoundTrialTest#offEverySongIsTheSameToTheByte` : coupé, le morceau des 37 styles a l'empreinte SHA-256 prise sur la base 2cfb8a3a, avant tout changement.
  - Au banc, la hard techno (hors essai) jouée interrupteur mis a un rendu identique à l'octet à celui d'avant (57 600 058 octets comparés).
  - Ces tests du modèle se jouent par `:project-model:test --tests '*SoundTrialTest'` (6 tests verts). La suite du modèle reste verte (1 091 tests).
- **Rouge puis vert, à chaque changement** :
  - interrupteur, sons modernes et OTT : 2ee8f348 / 06efec42 ;
  - WAV de pack lus au banc : 599779ab / 94c546f5 ;
  - batterie d'essai : 04b4337d / c87c1432. Le test seul échoue sur ses propres vérifications sans le script ; avec lui, 1 225 vérifications vertes ; 13 versions cassées du script sont refusées ;
  - techno qui garde son pluck : 345818f3 / 81325551.
- **Moteur** : non modifié. `./scripts/host-tests.sh` : « ALL PASSED (0 failures) ». Aucune allocation, aucun verrou, aucun log ajouté dans le rappel audio.
- **Écoute en direct** (`scripts/live-bench.sh`, 150 s) : 4 styles sur 4 sans alerte, avant comme après.

### Charge du processeur

Mesurée par `scripts/cpu-bench.sh`, 3 passages, rappels de 96 trames, temps du fil audio en % du budget temps réel, machine x86 4 cœurs.

| Style | Avant : moy. / p99 / pire | Après : moy. / p99 / pire |
|---|---|---|
| jump up | 12,8 % / 19,4 % / 28,7 % | 12,8 % / 18,1 % / 27,2 % |
| tech house | 7,9 % / 11,7 % / 18,2 % | 7,5 % / 11,0 % / 16,0 % |
| techno (pluck d'origine) | 6,9 % / 10,0 % / 19,0 % | 6,4 % / 10,0 % / 16,9 % |
| hard techno (hors essai, audio identique) | 6,6 % / 10,7 % / 19,1 % | 7,2 % / 11,4 % / 19,4 % |

- **Aucune hausse au-delà du bruit de mesure.** La hard techno, dont l'audio est identique à l'octet, varie de +0,6 point : c'est ce bruit.
- Remplacer la batterie synthétisée par des échantillons n'a rien coûté. L'OTT de plus sur deux pistes non plus, de façon mesurable.
- Rien à signaler au-delà de +10 %.

### Timbre, voix seules

Premier Drop, écoute en direct, `docs/sons/outils/voix.py`, avant → après :

| Style, voix | Air > 10 kHz (dB) | 1-4 kHz au téléphone (dB) | Côté/centre (dB) |
|---|---|---|---|
| jump up, batterie | −13,4 → −13,3 | −3,9 → −4,9 | −17,5 → −13,7 |
| jump up, lead | −20,8 → −16,5 | −5,6 → −6,2 | −4,5 → −4,3 |
| jump up, nappe | −31,9 → −24,9 | −7,2 → −7,8 | −1,8 → −4,1 |
| tech house, batterie | −15,0 → −12,3 | −4,1 → −6,4 | −16,3 → −11,7 |
| tech house, lead (voix robot) | −58,7 → −53,3 | −15,0 → −17,3 | −3,0 → −3,0 |
| tech house, nappe (orgue) | −32,9 → −30,9 | −4,3 → −5,9 | 0,5 → 0,3 |
| techno, batterie | −19,5 → −15,7 | −3,4 → −4,8 | −18,2 → −14,7 |
| techno, lead | −39,2 → −36,3 | −4,0 → −6,2 | −2,9 → −3,1 |
| techno, nappe (pluck + OTT) | −38,9 → −37,4 | −8,9 → −10,7 | −1,1 → −1,0 |

Ce qu'on en tire :
- les batteries s'élargissent nettement (+3,5 à +4,6 dB de côté) ;
- elles gagnent de l'air en tech house et en techno ;
- là où seul l'OTT change (tech house et techno), leads et nappes gagnent de +1,5 à +5,4 dB d'air ;
- en jump up, la nappe et le lead changent aussi de son : +7,0 et +4,3 dB d'air ;
- **en contrepartie, la part 1-4 kHz recule sur les 9 voix, de 0,6 à 2,3 dB** : c'est la bande qu'un haut-parleur de téléphone fait entendre (le grave et l'aigu montent plus que le médium) ;
- le lead de la tech house reste de loin le plus sombre ;
- la nappe techno avec le « Pluck future » mesurait −40,2 dB d'air (plus sombre) : retirée.

### Extraits envoyés à Benjamin

- 6 WAV 16 bits, 30 s chacun, du début du premier Drop. La lecture commence au même endroit (« Écouter », `-Pbench.listen=drop`), même passage avant / après.
- « Avant » = les sons intégrés, sans pack téléchargé (comme un téléphone qui n'a pas les packs des styles). Sur un téléphone qui a ces packs, l'avant sonne autrement.
- Le plus fort a été baissé au niveau de l'autre : écart de 0,0 LU par style (jump up −9,0, tech house −9,1, techno −8,6 LUFS), crête vraie ≤ −1,2 dBTP.
- Pas de M4A : l'encodeur AAC de ffmpeg dépassait 0 dBTP sur ces extraits denses, un défaut connu (`docs/son/BANC.md`). Le WAV donne l'audio exact.
- Les extraits ne sont pas dans le dépôt.

## Essai n° 2 (après « Tu rajoutes juste des cymbales on dirait »)

Benjamin, à l'écoute de l'essai n° 1 : « Tu rajoutes juste des cymbales on dirait ». Le chef d'orchestre a décidé de ne pas fusionner l'essai n° 1 tel quel, et a fixé ce critère : pour chaque style, au moins 3 dB d'écart mesuré sur au moins deux bandes hors aigus, ou un changement de timbre net, à niveau égalisé.

### Ce que disait l'essai n° 1, bande par bande

Mesure : `docs/sons/outils/voix.py`, qui sait maintenant lire quatre bandes (sub 20-60 Hz, bas 60-250 Hz, médium 250 Hz-4 kHz, aigu 4-20 kHz), test compris. Premier Drop en écoute en direct, après − avant, l'« après » ramené au niveau du mix d'avant.

| Style | Mix : sub / bas / médium / aigu (dB) | Bus batterie : sub / bas / médium / aigu (dB) | Basse : médium (dB) |
|---|---|---|---|
| jump up | −0,2 / +1,3 / −0,6 / +1,1 | −0,4 / +1,9 / −0,8 / +0,8 | −0,2 |
| tech house | +0,5 / −0,5 / −0,4 / **+2,8** | +1,0 / +0,2 / +1,8 / **+3,1** | −0,5 |
| techno | +1,9 / −2,3 / −2,0 / **+4,2** | +2,5 / −1,3 / +0,5 / **+4,0** | −1,7 |

Confirmé : seuls les aigus de la batterie bougeaient. La basse ne changeait pas.

Le solo d'un pad de batterie n'est pas fiable quand ce pad joue un échantillon, d'où les chiffres incohérents d'un premier passage. On mesure donc le bus batterie entier (voie 0).

### Ce qui change dans l'essai n° 2

- **Kick** (`trial_drums.py`), en partant de ce qu'a trouvé le lot « kick » (`SpeakerKick`, branche ccr-aad91c20-ow1wdm, que je n'ai pas modifiée) : un kick s'entend sur un petit haut-parleur par les harmoniques de son grave (180 Hz-1,5 kHz) et par son clic (1,2-4,5 kHz). Ici, on les met dans le son lui-même :
  - sub accordé ;
  - « punch » de 60 à 100 ms ;
  - harmoniques par tanh suréchantillonné ×4 ;
  - clic à part.
  - Le passe-haut anti-grondement est déplacé avant l'écrêteur : placé après, il faisait perdre environ 2 dB à crête égale.
  - À crête égale, face au kick de l'essai n° 1 : 60-250 Hz +4,6 à +6,5 dB, 250 Hz-1,5 kHz +10,8 à +19,1 dB, clic +6,8 à +20,9 dB. Le facteur de crête tombe à 4,7-7,3 dB : le kick est nettement plus saturé.
- **Caisse claire et claps** : un corps à 185-250 Hz, du bruit à 1-3 kHz, une plaque de 0,3 à 0,5 s, décorrélée gauche/droite pour les claps. +4 à +12 dB entre 250 Hz et 2 kHz à crête égale.
- **Charleys, rides** : crête à −8 dBFS au lieu de −1, avec 3 dB de moins au-dessus de 10 kHz. **Percussions** : crête à −4 dBFS.
- **Basse** (sous l'interrupteur), dans cet ordre :
  - saturation en parallèle (force 0,95, mélange 85 %, volume constant) ;
  - +12 dB vers 800 Hz ;
  - égaliseur : graves +5 dB, aigus −9 dB au-dessus de 8 kHz.
  - Le pompage sur le kick existe déjà : le mixage de l'assistant fait baisser la musique sous le son du kick, par un bus. Il n'est pas doublé.
- **Leads et nappes** : élargisseur avec étalement, plaque et délai en ping-pong (lead : réverbe 0,25, délai 0,3 ; nappe : réverbe 0,4).
- **Voix de la tech house** : +9 dB vers 2,5 kHz.
- Pas de nouveau mouvement de filtre : les leads gardent les cycles de filtre de 8 mesures qu'ils avaient déjà.

### Mesures de l'essai n° 2

Après − avant, à niveau égalisé, premier Drop, écoute en direct.

| Style | Mix : sub / bas / médium / aigu | Bus batterie : sub / bas / médium / aigu | Basse : sub / bas / médium / aigu |
|---|---|---|---|
| jump up | −4,9 / +1,0 / +1,6 / −4,4 | −3,9 / **+3,1** / **+3,6** / −4,7 | −5,2 / −4,5 / **+3,5** / +2,1 |
| tech house | −1,8 / −0,5 / +1,9 / −1,1 | −3,0 / +0,8 / **+4,4** / −2,6 | −2,4 / −2,8 / **+3,8** / +24,1 |
| techno | +0,3 / −0,6 / +0,3 / −1,8 | +1,0 / **+3,0** / **+5,4** / −2,3 | −1,7 / −3,0 / −0,1 / +2,8 |

**Critère** (au moins 3 dB sur au moins deux bandes hors aigus, sur les pistes qu'on remarque) :
- **jump up** : batterie bas et médium, basse médium ;
- **tech house** : batterie médium, basse médium ;
- **techno** : batterie bas et médium. Sa basse ne bouge pas dans le médium (−0,1).

**Les aigus redescendent** : mix −1,1 à −4,4 dB, batterie −2,3 à −4,7 dB. Il n'y a plus « que des cymbales ».

**Basse au haut-parleur de téléphone** (écoute en direct, 300 / 200 Hz) :
- tech house −11,4 / −7,0 → **−4,8 / −3,6 dB** ;
- techno −6,3 / −4,4 → **−3,4 / −2,7 dB** ;
- jump up −1,5 / −0,9 → −0,3 / −0,1 dB.

**Contreparties mesurées** :
- **jump up** : le sub du mix baisse de 4,9 dB. Le médium monte, donc, à volume égal, le grave profond recule.
- **la basse** perd 1,7 à 5,2 dB de sub dans les trois styles.
- **tech house** : sa basse, un sinus presque pur, prend du grain au-dessus de 4 kHz (+24 dB, partis d'un niveau presque nul). Un écrêteur doux de l'appli placé après l'égaliseur en recrée une partie.

**Écoute en direct** : 0 alerte sur les 4 styles. Aucune saturation ni NaN dans les extraits.

**Processeur** (3 passages, 96 trames) :

| Style | Avant (moy. / p99) | Après l'essai n° 2 (moy. / p99) |
|---|---|---|
| jump up | 12,8 % / 19,4 % | 12,5 % / 18,0 % |
| tech house | 7,9 % / 11,7 % | 7,2 % / 10,5 % |
| techno | 6,9 % / 10,0 % | 6,4 % / 9,2 % |
| hard techno (témoin) | 6,6 % / 10,7 % | 6,6 % / 10,8 % |

Aucune hausse.

**Extraits envoyés à Benjamin** : 6 WAV 16 bits de 25 s, depuis le premier Drop, même passage avant / après. Écart de niveau perçu de 0,1 LU au plus, crête vraie ≤ −1,2 dBTP.

### Rouge puis vert (essai n° 2)

| Changement | Rouge | Vert |
|---|---|---|
| Quatre bandes dans l'outil | 56bf3f55 | fb505a5b |
| Basse, largeur, espace, voix de la tech house | 2ef1ceae | e3835d45 |
| Basse plus saturée et relevée à 700 Hz | 7f7201f4 | 922d5778 |
| Basse plus saturée et relevée à 800 Hz | bc6e8941 | 450d528f |
| Sub rendu, grésillement coupé | 77d58dde | 2f4cefdb |
| Batterie n° 2 | 3888b0e6 | b1d2245d |
| Kick avec son corps | db7b3ea8 | 7764266a |

Le rouge de chaque paire est un test seul, qui échoue sur ses vérifications (Kotlin : sur ses assertions ; Python : 805 puis 12 vérifications de la batterie échouent sur la version d'avant).

## Tests ciblés et chemins

tests: *SoundTrialPacksTest *BenchWavPackTest *HumanLucasBugsTest *SoundCategoriesTest

Les tests du modèle se jouent à part : `:project-model:test --tests '*SoundTrialTest'` (9 tests verts, dont les 3 de l'essai n° 2 sur la basse, la largeur et l'espace, la voix de la tech house). Les scripts Python : `python3 -I scripts/packs/test_trial_drums.py` (2 041 vérifications), `python3 -I docs/sons/outils/test_voix.py`.

- chemin: Assistant › essai sons modernes, chaque ligne d'essai prend son pack d'essai | test: SoundTrialPacksTest#withTheSwitchEachTrialLineTakesItsTrialPack
- chemin: Assistant › interrupteur coupé, les packs nommés par la recette restent devant | test: SoundTrialPacksTest#withoutTheSwitchTheRecipesPacksStayFirst
- chemin: Banc › un son de pack en WAV lu avec ses propres échantillons | test: BenchWavPackTest#aWavOfAPackIsReadWithItsOwnSamples
- chemin: Banc › les sons FLAC d'un pack téléchargé toujours lus comme avant | test: HumanLucasBugsTest#aDownloadedPacksSoundsAreRead
- chemin: Assistant › une ligne de batterie prend le son du pack avant le kit | test: SoundCategoriesTest#aLineWithoutAKitVoiceStillTakesThePackFirst

## Ce dont je ne suis pas sûr

- **Personne n'a écouté** : ni moi, ni l'agent qui a fabriqué la batterie. Tout est jugé sur les mesures. Les charleys synthétisés restent le risque principal d'un son « boîte à rythmes ».
- **Les sons de l'étude** (Signature Sounds, Freesound, Surge XT) n'ont pas pu être pris ici. Avec ton accord, ils peuvent l'être plus tard : un téléchargement fait par toi, ou une clé Freesound.
- **Niveaux** : les sons d'essai sont calés sur leur crête. Les volumes du mix n'ont pas été recalibrés : le banc ne montre ni alerte ni écart de sonie, mais l'équilibre batterie / reste est à juger à l'oreille.
- **Le lead de la tech house** (« voix robot ») reste sombre. De vraies voix découpées modernes demanderaient des enregistrements que nous n'avons pas.
- **Les tables d'ondes** (deuxième temps de l'essai 3) ne sont pas faites : on attend d'abord ton écoute.
- **Interrupteur mis sans les packs d'essai installés**, le banc jouerait un mélange (sons et OTT de l'essai, batterie du kit) : ni le rendu par défaut, ni l'essai. L'essai se joue donc toujours avec `-Psons.essai=1` et `-Pbench.packs=<packs d'essai>` ensemble.
- **La présence au téléphone recule** (1-4 kHz, voir plus haut) : à juger à l'oreille sur le haut-parleur.
