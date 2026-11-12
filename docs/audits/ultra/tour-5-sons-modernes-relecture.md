# Tour 5, lot « sons-modernes » : relecture neuve de l'étude des sons (2026-11-12)
Relu : a0713bae5220d7f76c363fcafa9a7e822ce3baf6

## Ce qui a été relu
- **Premier passage** sur cacaea09, l'étude seule : `docs/sons/PLAN-SONS-MODERNES.md` et `docs/decisions/tour-5-sons-modernes.md`. Ce relevé avait été commité en 034561af.
- **Second passage** sur 6776fa68, seulement l'écart `034561af..6776fa68` : l'étude, les décisions, et le nouveau dossier `docs/sons/outils/` (`voix.py`, `test_voix.py`, `mesures-2026-11-12.md`).
- **Troisième passage** sur 9d3eab94, seulement l'écart `efe68ad1..9d3eab94` : l'étude, les décisions et `test_voix.py`.
- Je suis un relecteur neuf : je n'ai écrit ni l'étude ni les corrections, et je n'ai modifié aucun de leurs fichiers.

**Contrôles faits au troisième passage :**
- `PYTHONDONTWRITEBYTECODE=1 python3 -I docs/sons/outils/test_voix.py` : OK, avec des scies à 65,4, 261,6 et 523,3 Hz, chaque colonne publiée vérifiée.
- J'ai rejoué les sept versions cassées du deuxième passage, dans une copie hors du dépôt : toutes sont refusées, y compris les quatre qui passaient. J'en ai essayé une huitième, « grave-médium » calculé sur 150-600 Hz : refusée aussi.
- Remarque, non comptée comme constat : une version qui compte la présence jusqu'à 5 kHz au lieu de 4 kHz passe encore. L'écart sur la scie grave est de 0,28 dB, juste sous la tolérance de 0,3 dB. La phrase du § 2.2 reste vraie : elle ne parle que des sept versions cassées qu'elle énumère.
- Dates des références relues dans `StyleRecipes.kt` :
  - House : l. 251, de 1984 à 1992 ;
  - Tech house : l. 320, de 2001 à 2022, dont trois après 2014 ;
  - Jump up : l. 984, de 2007 à 2023, dont six à partir de 2017.
- L'écart ne touche que trois fichiers de documentation (`docs/sons/` et `docs/decisions/`), sans lien symbolique.

**Contrôles faits au second passage :**
- `PYTHONDONTWRITEBYTECODE=1 python3 -I docs/sons/outils/test_voix.py` : OK.
- Sept versions cassées de `voix.py`, essayées dans une copie hors du dépôt :
  - trois sont refusées : l'air pris à partir de 6 kHz, le seuil de silence retiré, le côté mal calculé ;
  - quatre passent le test : voir N1.
- Les chiffres du tableau du § 2.2 correspondent à `mesures-2026-11-12.md`.
- Le commit 6776fa68 ne touche que `docs/sons/` et `docs/decisions/tour-5-sons-modernes.md`. Tous les fichiers sont en mode 100644 : aucun lien symbolique, aucun son, aucun fichier de l'appli, du moteur, du banc ni des tests.

## Bloquant

Aucun.

## Grave

### G1. L'appli a déjà un OTT : l'étude disait le contraire et en faisait sa première brique moteur — **corrigé**
- **Passages relevés sur cacaea09** :
  - En bref, point 1 : « sans tables d'ondes ni compression multibande » ;
  - § 2.1, point 3 : « On ne l'a pas » ;
  - § 5 : « un OTT mesuré à 1 dB près de l'original : le gain « son 2020 » le plus fort pour le moins de travail » et l'Avis « 1. un OTT (code Faust MIT…) » ;
  - essai 3 : « l'OTT en effet » ;
  - décisions, lignes 4 et 6.
- **Ce que dit le code** : il y a un `Ott` à trois bandes (`Effects.h:608-612`), l'effet « OTT multibande », le master de la Bass music (`WizardMix.kt:195`) et les réglages « Growl » et « Basse grasse ».
- **Ce que j'ai vérifié sur 6776fa68** :
  - En bref 1, § 2.1 point 4, § 5 et les décisions (lignes 5 et 8) décrivent l'OTT existant ;
  - ses masters sont bien ceux qu'écrit l'étude, vérifié au grep de `StyleRecipes.kt` : hard techno l. 669, gabber 728, acidcore 757, trance 818, jump up 987, riddim 1051, plus la Bass music par `WizardMix` ;
  - l'assistant ne le pose sur aucune nappe ni aucun lead : aucun `StyleFx("pad" | "lead")` n'a d'OTT ni de réglage qui en contienne ;
  - `co.xfer_ott` est devenu « facultatif » ;
  - l'essai 3 A commence par les recettes seules, et l'effort est revu à la baisse.

### G2. « Les packs ont été demandés rétro » : phrase plus forte que la preuve — **corrigé**
- **Passages relevés sur cacaea09** : En bref 1, § 2.1 point 2, essai 2 (« on retire « 90s », « TR-909 », « Juno-106 » des demandes »), décisions ligne 4.
- **Ce que j'ai vérifié sur 6776fa68** :
  - le rétro est maintenant attribué à la recette House, qui prend les packs TR-909, TR-707 et Juno (`StyleRecipes.kt:246`, `:252`), et au Hoover du jump up ;
  - « early 90s » est limité au pack Rétro techno ;
  - les packs de l'essai sont cités avec leurs vrais mots, conformes à `styles.json` : « drum and bass », « tech house tight », « techno Berlin » ;
  - l'essai 2 ne prétend plus retirer des mots d'époque ;
  - la tech house remplace la house dans l'essai, et la question 3 devient une question de goût sur la House.
- Il reste une petite erreur de dates : voir n1.

### G3. Le branchement de l'essai 1 était faux pour la house et le jump up — **corrigé**
- **Passages relevés sur cacaea09** : « Le « pack du style » passe avant le kit synthétisé » et « Les recettes ne changent pas tant que tu n'as pas dit « mieux » ».
- **Ce que j'ai vérifié sur 6776fa68** :
  - le § 1, « Quel échantillon gagne », décrit l'ordre réel de `soundCategories` : pack de machine nommé par la recette, puis pack du style, puis kit, et le pack de `packSounds` en premier pour la basse ;
  - le § 6 branche l'essai par un interrupteur qui fait nommer aux recettes les packs d'essai ; au banc, seuls les packs d'essai sont installés ;
  - « Les notes et la structure ne changent pas » remplace la phrase fausse ;
  - le jump up est dit prendre le pack « dnb ».
- Une précision manque, style par style : voir n3.

## Moyen

- **M1. Mauvais témoin pour le processeur** — **corrigé**.
  - Les témoins sont maintenant le jump up (60 %, pire 74 %) et la hard techno (décrochages), au § 1, au § 6 et dans les décisions (ligne 12).
  - La règle est devenue « rien de plus en jump up ni en hard techno ».
- **M2. Les mesures « avant » ne se refaisaient pas avec l'outil nommé** — **corrigé**.
  - L'outil `voix.py` est commité avec son test.
  - Les commandes exactes et les chiffres « avant » sont dans `mesures-2026-11-12.md` ; leurs options `--no-bass` et `--no-kick` existent bien dans `song-bench.sh`.
  - L'« air » à 10 kHz est distingué de celui du banc (6 kHz).
  - Le test ne couvre pas encore toutes les colonnes publiées : nouveau constat N1.
- **M3. « Trop sombre » n'était jugé que face à une scie brute** — **corrigé**.
  - La limite est écrite au § 2.2.
  - La parade est dans l'essai : des sons modernes faits avec Surge XT, mesurés avec le même outil.
- **M4. Les questions ne suivaient pas CLAUDE.md** — **corrigé**.
  - Chaque option a sa conséquence en une ligne.
  - La question 2 reste posée à Benjamin, selon la consigne du lot, que je ne peux pas lire ici. La question 3 relève maintenant du goût, et la 4 touche à l'APK, qui ne se fait que sur sa demande.
- **M5. Le « 5 bits » de la table d'ondes de Plaits** — **corrigé**.
  - Le § 5 le limite à la sortie secondaire, conforme à `wavetable_engine.cc` et `PlaitsSynth.cpp:158-160`.
  - Le modèle table d'ondes de Plaits entre dans l'essai 3 A, à 0 €.

## Mineur (premier passage)

- **m1.** Basse en échantillon : « seulement en house et en techno » — **corrigé** (« en premier dans 7 styles »).
- **m2.** « la ride et le crash raccourcis » — **corrigé** : seule la ride est raccourcie.
- **m3.** « Reese moderne » déjà dans la bibliothèque — **corrigé**.
  - Le § 5 cite aussi « Accords future bass » et « Pluck future ».
  - Aucune recette ne s'en sert, vérifié au grep.
- **m4.** Signature Sounds : 34 packs sur 36 — **corrigé** (« 34 packs annoncent du CC0, dont 18 avec la ligne »).
- **m5.** Estimations pas présentées comme telles — **corrigé** : efforts, tailles et prix sont étiquetés.
- **m6.** « les échantillons allègent le processeur » — **corrigé** : « devrait alléger (estimation, à mesurer) ».
- **m7.** Dossier `docs/sons/` à côté de `docs/son/` — **corrigé**.
  - Le dossier reste en place, mais la raison est notée dans les décisions (ligne 3) : nom imposé par la consigne du lot.
- **m8.** Le sous-style « 90s » de WaivOps — **corrigé** (« à éviter »).
- **m9.** Tables d'ondes préparées hors du fil audio — **corrigé** (§ 5, point 2, et essai 3 B).
- **m10.** La bonne clause de Splice — **corrigé** : 2.3.1.4 d, avec l'exception « fondu dans une œuvre ».
  - La liste des œuvres de 2.3.1.3 est ouverte (« such as but not limited to »), donc « un jeu ou une appli » se défend.

## Constats du second passage (sur 6776fa68), revus sur 9d3eab94

### Moyen

- **N1. Le test de l'outil ne couvrait pas deux des colonnes publiées** — **corrigé**.
  - Sur 9d3eab94, le test vérifie « boue » et « présence » sur trois scies. Une bande vide, comme 150-500 Hz pour la scie à 523,3 Hz, doit lire moins de −40 dB.
  - Les sept versions cassées sont refusées, et le § 2.2 le dit. La ligne 4 des décisions est alignée.
  - Passages relevés sur 6776fa68 :
    - § 2.2 : « Cet outil est vérifié par `test_voix.py` […]. Une version cassée de l'outil est refusée. » ;
    - colonnes « Grave-médium 150–500 Hz face à 1–4 kHz » et « présence 1–4 kHz » ;
    - essai, « Voix seules » : « grave-médium face au médium-aigu ; présence au téléphone ».
  - Sur 6776fa68, le test ne vérifiait que l'air, le centroïde, la brillance, le côté et le silence. Ces quatre versions cassées passaient quand même :
    - `boue` calculée sur 100-500 Hz au lieu de 150-500 Hz ;
    - `presence` rapportée à 20 Hz-20 kHz au lieu de 300 Hz-20 kHz ;
    - la bande totale commencée à 200 Hz au lieu de 20 Hz (la scie de 261,6 Hz n'a rien en dessous) ;
    - la fenêtre de Hann retirée.
  - La phrase était donc plus forte que la preuve pour les colonnes « grave-médium » et « présence », qui portent la lecture « boueuse » de la nappe techno et la basse « peu audible » de la tech house.
  - Correction proposée :
    - ajouter au test les valeurs exactes de `boue_150_500_vs_1_4k_db` et `presence_tel_1_4k_db` pour la scie (somme des 1/n² dans chaque bande) ;
    - ajouter une scie grave, par exemple à 65,4 Hz, pour tenir le bas de la bande totale ;
    - ou bien limiter la phrase du § 2.2 aux colonnes vérifiées.

### Mineur

- **n1. Les dates des références étaient inexactes** — **corrigé** : House 1984-1992 (En bref, § 1, question 3, décisions), jump up 2007-2023, tech house 2001-2022. Ce que j'avais relevé dans `StyleRecipes.kt` :
  - House « de 1986 à 1992 » (En bref 1, § 1, question 3, décisions lignes 5 et 6) : la liste va de 1984 (Jesse Saunders, « On and On ») à 1992 ;
  - Jump up « références de 2017 à 2023 » : la liste commence en 2007 (DJ Hazard & Distorted Minds, « Mr Happy ») ;
  - Tech house « de 2014 à 2022 » : la liste commence en 2001 (Green Velvet, « La La Land »).
  - Correction : 1984-1992, 2007-2023 et 2001-2022, ou « références surtout de … ».
- **n2. Le kick de la tech house est accordé lui aussi** — **corrigé** : tableau du § 1 (« familles House et Techno ») et essai 1.
  - Passage : « Attention aux kicks : le kick studio de la techno est accordé à la tonalité ».
  - La tech house est de la famille « House », donc dans `TUNED_FAMILIES` (`StudioDrums.kt:27`, `:96`).
  - Correction : « de la techno et de la tech house ».
- **n3. L'interrupteur d'essai, style par style** — **corrigé** : § 6, une ligne par style (jump up : « dnb » ; tech house : un pack donné, sinon « tech_house » ; techno : le TR-909 du charley ouvert, « techno » et « techno:bass »).
  - Passage : « des packs d'essai à la place de leurs packs de machines (TR-909, Juno…) ».
  - Jump up : le pack à remplacer est « dnb », un pack de style partagé avec la drum & bass, pas un pack de machine.
  - Tech house : la recette n'a pas de `kit` ; il faut en ajouter un, ou donner au pack d'essai l'identifiant « tech_house ».
  - Techno : le TR-909 pour le charley ouvert, et le pack « techno ».
  - Correction : le dire en une ligne par style.
- **n4. « la voix la plus sombre » (tableau du § 2.2, lead de la tech house à −57 dB)** — **corrigé** (« le lead le plus sombre »).
  - La phrase était contredite par la ligne suivante : la basse de la tech house est à −60 dB.
  - Correction : « le lead le plus sombre ».
- **n5. L'essai 3 A changeait le rôle d'une partie** — **corrigé** : « des voix découpées modernes (ou une voix robot plus claire) […] : la partie garde son rôle de voix ».
  - Passage : « pour la tech house, un vrai lead à la place de la « Voix robot » ».
  - La recette joue des voix découpées (`LeadKind.Chops`, `vocal_chops:vocal`). Mettre un lead change le rôle de la partie, pas seulement son son, ce qui contredit « mêmes notes, sons remplacés ».
  - Correction : « des voix découpées modernes (pack d'essai) à la place de la « Voix robot » ».
- **n6. « la production d'aujourd'hui la met sur presque tout »** (§ 2.1, point 4), sans source — **corrigé** (« très courante sur ces sons dans la production d'aujourd'hui »).
  - Correction : « très courante sur les basses, leads et nappes ».

## Rapport de zone (quatrième passage, sur a0713bae)

**Ce qui a été relu.** Seulement `docs/audits/ultra/tour-5-sons-modernes-rapport.md`. Le reste de l'écart `31dad71c..a0713bae` est la fusion de la base (aa005116, trois commits « Tour 6 »).
- Dans la zone (`docs/sons/`, les décisions du lot, cette relecture), seul le rapport bouge. Rien ne bouge dans `app/`, `engine/`, `project-model/` ni `scripts/`, et l'écart n'ajoute aucun lien symbolique.
- `scripts/bench/rapport-zone.py HEAD docs/audits/ultra/tour-5-sons-modernes-rapport.md` accepte le rapport : code 0, aucun message.

**La ligne « tests: aucun : … » est honnête.** Le lot ne touche ni l'appli, ni le moteur, ni le banc, et l'outil Python a son propre test.

**Les quatre chemins.** Chacun nomme un `@Test` sans `@Ignore`, présent dans `app/src/test` :
- `SoundCategoriesTest#aLineWithoutAKitVoiceStillTakesThePackFirst` : le chemin est fidèle au test (le kick de l'afro house prend le pack du style avant le kit) ;
- `SoundCategoriesTest#aLineThatAsksForAKitVoiceKeepsItWithThePackDownloaded` : fidèle aussi ;
- les deux autres sont dans les constats ci-dessous.

### Moyen

- **R1. Le chemin 3 ne décrit pas ce que fait son test.**
  - Passage : « chemin: Assistant › la machine nommée par la ligne passe en premier | test: WizardLineFxTest#theStylesPacksAreOfferedAtTheLineTheMachineOfThatLineFirst ». Le rapport le range sous « le choix d'un son par rôle, que l'interrupteur d'essai changera ».
  - Ce test ne vérifie que l'ordre des packs **à télécharger** proposés sur une ligne (`missingStylePacks`, `WizardScreen.kt:502-507`). Il ne vérifie pas que, packs installés, le son de la machine passe avant le pack du style et le kit. C'est pourtant l'ordre de `soundCategories` sur lequel l'essai compte (G3).
  - Le test qui couvre cet ordre existe : `ShelfBrowserTest#theAssistantKeepsTheStylesMachineAndItsKitVoiceFirst` (`@Test` sans `@Ignore`). Il installe le pack de la machine et vérifie qu'il sort en premier dans « Pour ce style », devant le kit.
  - Correction proposée : nommer ce test-là pour ce chemin, ou renommer le chemin en « Assistant › packs à télécharger d'une ligne : sa machine d'abord ».

### Mineur

- **r1. Chemin 4 : « Bibliothèque › les packs du style proposés d'abord » (`LibraryChoicesTest#theStylesPacksAreOfferedFirstAndDownloadedInOneTouch`).**
  - Le test vérifie que l'offre existe et qu'une seule touche télécharge tous les packs du style. Il ne vérifie pas qu'elle vient « d'abord » : il ne contrôle aucune position.
  - Correction : « les packs du style proposés et téléchargés d'une touche ».
- **r2. Rapport, « Preuves » : « Toutes les remarques ont été corrigées ».**
  - Tous les constats l'ont été. Mais la remarque du troisième passage, non comptée comme constat (la présence comptée jusqu'à 5 kHz passe encore le test), est toujours vraie.
  - Correction : « Tous les constats ont été corrigés ».

Verdict : 0 bloquant, 0 grave — fusion possible
