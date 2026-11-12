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
- leads et nappes gagnent de l'air avec l'OTT, surtout la nappe du jump up (+7 dB) ;
- en contrepartie, la part 1-4 kHz recule de 1 à 2 dB sur plusieurs voix (le grave et l'aigu montent plus) ;
- le lead de la tech house reste de loin le plus sombre ;
- la nappe techno avec le « Pluck future » mesurait −40,2 dB d'air (plus sombre) : retirée.

### Extraits envoyés à Benjamin

- 6 WAV 16 bits, 30 s chacun, du début du premier Drop. La lecture commence au même endroit (« Écouter », `-Pbench.listen=drop`), même passage avant / après.
- Le plus fort a été baissé au niveau de l'autre : écart de 0,0 LU par style (jump up −9,0, tech house −9,1, techno −8,6 LUFS), crête vraie ≤ −1,2 dBTP.
- Pas de M4A : l'encodeur AAC de ffmpeg dépassait 0 dBTP sur ces extraits denses, un défaut connu (`docs/son/BANC.md`). Le WAV donne l'audio exact.
- Les extraits ne sont pas dans le dépôt.

## Tests ciblés et chemins

tests: *SoundTrialPacksTest *BenchWavPackTest *HumanLucasBugsTest *SoundCategoriesTest

Les tests du modèle se jouent à part : `:project-model:test --tests '*SoundTrialTest'`.

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
