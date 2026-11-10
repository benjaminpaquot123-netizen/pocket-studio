# Lot « bloquants » : deuxième relecture neuve (correctif G1)
Relu : 37d6a977eb8d6ef957427109a870fe5446dd2502

Date : 2026-11-10 · relecteur neuf (n'a écrit ni le code ni la première relecture) · delta `26a0994..37d6a97` : `0793def` (rouge + gardes m3), `9bc590e` (vert, `PartHeader.kt`), `5ec1e6b` et `37d6a97` (rapport, DECISIONS).
Lus : la première relecture (annexe ci-dessous), la section « Après relecture » du rapport, le diff complet, `PartHeader.kt`, l'appel dans `AppRoot.kt` (`WithPartHeader` : `songPlaying = transport.playing && sessionUi.songIndex >= 0`), dans `RhythmViewModel.kt` : `selectScene`, `playSongPart`, `playSongFrom`, `stageSongPart`, `onSceneSwapped`, `endSongFollow`, `leaveSongForLive`, `watchSwaps`.
Le code n'a pas été modifié. Une sonde temporaire (`ZzRelecture2ProbeTest`, effacée, jamais commitée) a pris les captures et journaux ci-dessous ; la mutation a été annulée fichier par fichier (`git status` propre en dehors des lignes « T engine/third_party »).

## Ce qui a été lancé (file Gradle `scripts/bench/gradle-file.sh`, passages ciblés, tête 37d6a97)

| Passage | Résultat |
|---|---|
| `testDebugUnitTest` : `Tour5BloquantsTest`, `PlayStripLogicTest`, sonde | `Tour5BloquantsTest` 9 tests, 8 verts, `drive` sauté ; `PlayStripLogicTest` 6 verts (la sonde Pro a raté une touche, corrigée au passage suivant) |
| `verifyRoborazziDebug` : `Tour5BloquantsTest`, `TutoJourneyTest`, `PlayStripLogicTest`, `NavScreenshotTest`, sonde | **BUILD SUCCESSFUL** : `Tour5BloquantsTest` 8 verts + 1 sauté, `TutoJourneyTest` 74 verts, `PlayStripLogicTest` 6 verts, `NavScreenshotTest` 30 verts, `ScreenScanTest` 30 verts, sonde 3 verts + 1 sauté. Aucune image de référence ne change. (`ScreenshotTest` non relancé : hors des classes demandées ; sa `#recette` est l'image instable connue.) |
| Mutation : l'ancienne condition remise dans `PartHeader.kt` (`playing && waiting != null …`, c.-à-d. le `PartHeader.kt` de `26a0994` au commentaire près) ; `Tour5BloquantsTest.noWaiting*` | **2 rouges sur 2**, pour la bonne raison : Simple « Partie : A · Intro / C · Refrain arrive à la prochaine mesure… » avec `SessionUi(pending=2, songIndex=0)` ; Pro « Partie : E · Intro 2 / F · Intro 3 arrive à la prochaine mesure… » avec `SessionUi(pending=5, songIndex=0)`. Fichier restauré ensuite. |

## Constats

### Bloquant

Aucun.

### Grave

Aucun. **G1 de la première relecture : corrigé.** Preuves (journaux de la sonde, toutes les 0,5 s ; captures regardées par moi) :
- **Simple, morceau en lecture sans choix** (Lo-fi, « Écouter » du Guide, Clavier, 6 s, dont le passage A → C) : toujours « Le morceau joue : touche la grille et cette partie reste sous tes doigts, en boucle. », jamais la phrase, alors que `pending=2, songIndex=0` puis `songIndex=1`. Capture `s1` : « Partie : C · Refrain » et la phrase « Le morceau joue… » dessous.
- **Pro, ▶ du morceau sans choix** (Techno, Sons › Batterie, 6 s) : « Partie : E · Intro 2 / Le morceau joue… » avec `pending=5, songIndex=0`. Capture `p1` regardée (Pro, rien de coupé).
- **Choix d'une partie pendant le morceau** : `selectScene` pose `SessionUi(pending = index)`, donc `songIndex = -1`, et la phrase redevient possible. Sur le banc, la phrase apparaît et dit vrai pour chaque choix fait hors mode morceau : « Partie : L · Fill / K · Fill arrive à la prochaine mesure… » (`pending=10, songIndex=-1`), puis 0,5 à 1 s plus tard « Partie : K · Fill / Elle tourne en boucle… » (4 choix de suite, chaque fois la même chose). Capture `s2` regardée : ⟳ Partie : L · Fill, dessous « K · Fill arrive à la prochaine mesure… », rien de coupé. Voir aussi m2.
- **« Jouer depuis… » une partie du morceau pendant la lecture (Pro)** : reste en mode morceau (`queued=17`, `songIndex` ≥ 0) : pas de phrase, « Le morceau joue… » ; la partie 17 (K · Drop 2) arrive à la fin de la partie en cours (environ 3 s ici), puis l'en-tête la nomme. Rien de faux. Capture `p3` regardée. Voir m1.
- **Pro, choix par la puce de partie en haut pendant le morceau** : « ⟳ Partie : A · Intro / Elle tourne en boucle… » (capture `p4` regardée).
- **Transport arrêté** : `partHeader` exige `playing` ; après ■ : « En boucle (⟳ Boucler). ▶ joue cette partie seule. » (Simple, journal).
- **Le moteur qui ne bascule jamais** : voir M1 (inchangé).
- Lecture du code : en dehors de `selectScene`, les seuls `SessionUi(pending=…)` avec `songIndex < 0` sont `playSongPart` en lecture d'une partie seule (le morceau démarre de cette partie à la barre suivante : la phrase dit vrai) et `endSongFollow` / la ligne 3673 (`pending` = la partie montrée, donc `waiting == p.scene` : pas de phrase). Le critère `!songPlaying` est donc juste partout où je l'ai cherché.

### Moyen

**M1 (première relecture, inchangé). La phrase resterait si le moteur ne faisait jamais le changement.** `pending` n'est effacé que par `onSceneSwapped`, ■, un changement de projet ou la sortie du mode morceau. Ma sonde a essayé de figer le compteur de changements du faux moteur ; le changement a quand même eu lieu (le banc le fait voir à l'appli par un autre chemin), donc **pas reproduit, pas exclu**. Le rapport le garde dans ses doutes (§ 6) et dans l'essai téléphone n° 1. Pas grave : ■ l'efface, et il faudrait un moteur en panne.

### Mineur

- **m1. « Jouer depuis… » pendant le morceau ne dit plus rien jusqu'à la fin de la partie en cours.** Avant le correctif, la phrase disait « K · Drop 2 arrive à la prochaine mesure… » (bonne partie, mauvais moment) ; maintenant rien ne montre que le choix est pris (capture `p3` : « Intro 3 mes. 13/176 », « Le morceau joue… »). Pas une régression par rapport à la base `9d50148` (aucune phrase alors). Une phrase à elle (« ensuite : K · Drop 2 ») serait un autre lot.
- **m2. Le tout premier choix fait en plein morceau n'a pas montré la phrase sur le banc.** Simple (L choisi) comme Pro (A choisi, « mes. 1/8 » sur la capture `p4`), le changement était déjà fait quand l'en-tête a été lu. La phrase dans ce cas n'est prouvée que par la lecture du code (`selectScene` met `songIndex` à −1) ; les tests la prouvent pour un choix fait hors mode morceau (« + Nouvelle partie » de `simplePartsPickedAfterTheGuidesListenAreEdited`, mes 4 choix de la sonde). L'essai téléphone n° 1 du rapport couvre ce cas.
- **m3. Commentaire de `partHeader`** : « a part picked then leaves the song mode first » ne vaut pas en Pro sans « suivre le morceau » (`selectScene` ne fait que montrer la partie). Sans effet : rien n'attend alors, pas de phrase.
- **m4. Rapport** : la § 1 dit encore « Correction `1d3d4aa` : tant qu'une partie attend sa mesure (`sessionUi.pending`), la ligne… dit… » (la règle d'avant) ; la puce suivante porte bien l'avertissement en gras, et « Après relecture » donne la vraie règle. La § 7 dit « les 6 tests » de `Tour5BloquantsTest` (ils sont 9 maintenant). Rien de faux sur le fond.
- **m5. Gardes E a #117 (m3 de la première relecture) : corrigé.** `assertEquals(taken, shown)` où `taken` = notes écrites par la prise (≥ 3) et `shown` = le nombre lu dans « Grille des notes : N notes » (null si la ligne manque : rouge aussi). Verts dans les deux gardes. Reste vrai : c'est la description de la grille, pas une image.

## Paire rouge/vert `0793def` / `9bc590e`

Honnête. `0793def` ne touche que `Tour5BloquantsTest.kt` ; `9bc590e` que `PartHeader.kt` (la condition `!songPlaying` et deux commentaires). Ma mutation (ancienne condition sur la tête) fait rougir les deux tests avec exactement les messages cités par le rapport. Les tests regardent l'en-tête toutes les 0,5 s pendant 8 s (Simple) et 10 s (Pro), donc à travers au moins un changement de partie du morceau, et le test Simple exige en plus de voir « Le morceau joue… ». `simplePartsPickedAfterTheGuidesListenAreEdited` et `…OnTheSeatsPhone` restent verts : la phrase est toujours là après « + Nouvelle partie ».

## Rapport et DECISIONS

- « Après relecture » du rapport : exact (cause, critère, rouge/vert, m3, passages ciblés, « pas de nouvelle suite complète »). Ses captures regardées sont en Simple seulement, et il ne prétend rien de plus.
- DECISIONS (ligne A a #27) : désormais juste (« seulement pour une partie choisie hors du mode morceau », la cause G1 dite). La précision « un choix pendant le morceau quitte d'abord le mode morceau » vaut pour Simple et Pro en suivant le morceau ; voir m3 pour le seul cas où elle ne vaut pas, sans effet.
- Restes mineurs du texte : m4.

## Contrôles divers

- Thread audio : aucun fichier `engine/` ni `.cpp` touché par le delta.
- `verdicts.tsv`, banc (`scripts/bench`) : pas touchés.
- Aucune image commitée, aucun lien symbolique, aucun identifiant de modèle dans les commits du delta.
- Captures regardées par moi (sonde, non commitées) : `s1` Simple pendant le morceau sans choix ; `s2` Simple juste après un choix (« K · Fill arrive à la prochaine mesure… ») ; `p1` Pro pendant ▶ ; `p3` Pro après « Jouer depuis… » ; `p4` Pro après un choix par la puce de partie.

## Première relecture (ce51e8c)

Relu : ce51e8c905fcf0aa7a129d83535665ad703036b3

Date : 2026-11-10 · relecteur qui n'a pas écrit le code · base fx-int 9d50148 · 8 commits (`b9d4aed` … `ce51e8c`).
Lus : le rapport de l'auteur, `git log` et le diff complet, `tour-5-synthese.md` (lignes 8, 33, 50, 98, 198-200), `Spotlight.kt`, `PartHeader.kt`, `RhythmViewModel.kt` (cycle de `sessionUi.pending`), `RhythmScreen.kt`, `GuideNav.kt`, `TutoValidation.kt`, les 19 étapes `check: swing`, les 14 `bloquants.json`.
Le code n'a pas été modifié. Une sonde temporaire (`ZzRelectureProbeTest`, effacée, jamais commitée) a pris les captures ci-dessous dans le dossier de travail du relecteur ; les mutations ont été annulées fichier par fichier (`git status` propre en dehors des lignes « T engine/third_party »).

### Ce qui a été lancé (file Gradle `scripts/bench/gradle-file.sh`, passages ciblés)

| Passage | Résultat |
|---|---|
| Tête ce51e8c : `Tour5BloquantsTest`, `TutoJourneyTest` (74, tous les styles Pro et Facile), `GuideNavTest`, `GuideJourneyTest`, `GuideAuditTest`, `TutoBigFontTest`, `TutoScreenTest` | 103 verts, 0 rouge (`drive` sauté comme prévu) |
| Mutation 1 : les 3 lignes de `dd31ec4` retirées de `SpotNode.reveal` ; `TutoJourneyTest*techno*` | `theWholeCourse[techno Pro]` **rouge** : « step_cells coupé en haut, sous l'en-tête » ; les 9 autres verts |
| Mutation 2 : `PartHeader.kt` et `AppRoot.kt` remis à 9d50148 ; `simplePartsPickedAfterTheGuidesListenAreEdited` | **rouge** sur « + Nouvelle partie » : « l'en-tête nomme M : Partie : B · Couplet / Elle tourne en boucle… » (C et B passent : le rouge ne tient qu'à « + Nouvelle partie », comme le rapport le dit) |
| Mutation 3 : `techno.json` remis sur `rhythm.pattern` ; `theTutorialsSwingStepShowsTheSwingSetting` | **rouge** (« Ich mach's » ne montre pas le Swing) : le rouge actuel rougit bien sur l'ancien comportement ; ce test n'a pas changé entre `ffa273e` et la tête (vérifié par `git diff ffa273e ce51e8c`) |
| Sonde : en-tête pendant la lecture, avant tout choix (Simple et Pro) | voir constat G1 |

### Constats

#### Bloquant

Aucun.

#### Grave

**G1. La phrase « … arrive à la prochaine mesure… » s'affiche pendant toute lecture du morceau, nomme la partie suivante du morceau et ment sur le moment (Simple et Pro).**
- Cause : `partHeader(…, waiting = sessionUi.pending)`. Or `pending` ne vaut pas seulement « partie choisie au toucher » : en mode morceau, `stageSongPart()` y met **la prochaine partie du morceau** (`_sessionUi.value = ui.copy(pending = upcoming.scene…)`, `RhythmViewModel.kt` vers la ligne 3929) pendant toute la partie en cours.
- Preuve 1, Mode simple (Lo-fi, « Écouter » du Guide, Clavier, sans rien toucher) : pendant 6 s, en-tête « Partie : A · Intro » et dessous « C · Refrain arrive à la prochaine mesure… » (`SessionUi(pending=2, songIndex=0)`), capture `simple-before-pick.png` regardée. C arrive à la fin de l'Intro, pas à la mesure suivante.
- Preuve 2, Mode Pro (Techno, ▶ en haut, onglet Sons) : 8 s de « Partie : E · Intro 2 / F · Intro 3 arrive à la prochaine mesure… », puis « Partie : F · Intro 3 / A · Intro arrive à la prochaine mesure… » (`songIndex=1, pending=0`), capture `pro-song-playing.png` regardée.
- Conséquences : (a) un renseignement faux sur l'écran le plus vu pendant l'écoute, en Simple comme en Pro ; (b) il remplace la phrase des audits d'avant « Le morceau joue : touche la grille et cette partie reste sous tes doigts, en boucle. » (la sonde la voit revenir seulement quand la partie suivante est la même que l'actuelle) ; (c) couché, la ligne de l'en-tête ne s'affichait que « quand elle a quelque chose de plus à dire » : elle apparaît maintenant pendant toute lecture et prend de la hauteur à l'éditeur ; (d) en Pro sans « suivre le morceau », elle annonce une partie qui ne viendra jamais à l'écran.
- Le rapport (§ 1 « la ligne n'apparaît que pendant l'attente ») et DECISIONS (« jusqu'à cette barre ») décrivent donc mal le comportement. Aucun test ne regarde l'en-tête pendant une lecture sans choix ; le test de A a #27 passe même par hasard si la partie choisie est la suivante du morceau.
- Piste (non appliquée) : ne passer `waiting` que hors mode morceau (`sessionUi.songIndex < 0`), et ajouter une garde « ▶ du morceau, sans choix : la phrase n'apparaît pas, « Le morceau joue… » reste ».

#### Moyen

**M1. A a #27 : la phrase reste si le moteur ne bascule jamais.** Elle ne dépend que de `pending` et de `playing` : ■ l'efface (sonde : après un choix puis ■, note « En boucle (⟳ Boucler)… »), un changement de projet remet `SessionUi()` à zéro ; mais un échange jamais fait la laisse jusqu'au ■. Le rapport le signale lui-même (§ 6). À garder en tête pour l'essai téléphone.

**M2. Paire rouge/vert de A a #27 : honnête mais étroite.** Le rouge `b9d4aed` rougit à coup sûr et pour la bonne raison sur « + Nouvelle partie » seulement (mutation 2) ; pour C et B, il dépend du moment du toucher (dit dans le rapport). Rien dans la paire ne protège contre G1.

#### Mineur

- **m1. `SpotNode.reveal` (dd31ec4).** Pas de régression trouvée : les 74 parcours de tuto (Pro et Facile), Guide (Nav, Journey, Audit), tuto en police agrandie, et la sonde Swing sur tablette 8″ allemand, téléphone 360 dp à 130 % et tablette 11″ couchée : le réglage Swing entier au-dessus du panneau (trois captures regardées). La mutation prouve que le correctif est nécessaire. Reste non testé à part : cible plus haute que l'écran qui remonte depuis le bas (la 3ᵉ mise en vue ramène maintenant toujours son haut, ce qui va dans le sens de la règle « son haut d'abord » du commentaire), feuille du bas, ancre qui bouge pendant la mise en vue. Le `withFrameNanos` dans le `runCatching` est sans risque (annulation absorbée comme avant).
- **m2. Traductions.** EN « comes in at the next bar », NL « komt erin bij de volgende maat », DE « kommt im nächsten Takt », ZH, JA justes et cohérentes avec « Mesure suivante » de chaque catalogue. HI « अगले बार से शुरू होगा » suit le mot « बार » du catalogue, mais se lit facilement « la prochaine fois » ; KO sans particule après le nom (« %s 다음 마디부터… ») : correct à l'oral, un peu sec. Aucune n'est fausse. (Le sens est faux partout tant que G1 n'est pas réglé.)
- **m3. E a #117, gardes `7915041`.** Elles font bien les gestes du siège (premier morceau, « Créer et écouter », suppression pendant qu'il est ouvert, OK, corbeille, restauration, prise, Retour pendant la prise, Pad Live, réouverture) et regardent la grille comme le siège (Clavier, piste Mélodie, Notes, ligne « Grille des notes »). Faiblesse : la grille est vérifiée « pas 0 notes », pas « autant de notes que la prise » ; elle lit la description de la grille, pas une image. La conclusion est bien écrite « pas reproduit », jamais « corrigé » (rapport § 2 et § 6, DECISIONS, message de commit « garde: … not reproduced »).
- **m4. Repère `rhythm.swing`.** Juste pour les 19 tutos (`check: swing`, les 19 changés, rien d'autre) ; `TutoValidation.SPOTS`, `GuideNav.SPOTS` et le `when` de `GuideNav` à jour ; `AUTHORING.md` à jour. Les 5 étapes Swing sans `levels` (boom_bap, dubstep, jungle, liquid_dnb, lofi) servent aussi en Facile : la carte Swing est rendue en Simple (`GrooveCard(…, simple)`) et les parcours Facile de `TutoJourneyTest` passent.
- **m5. Historique.** `ffa273e` amendé une fois, dit dans le rapport ; le test de la corbeille a encore changé dans `7915041` (ajout de la vérification de la grille), normal pour un « garde: ».

### Contrôles divers

- Thread audio : aucun fichier `engine/` ni `.cpp` touché.
- `verdicts.tsv` : pas touché. Banc (`scripts/bench`) : pas touché.
- Aucune image (`.png`) commitée, aucun lien symbolique, aucun identifiant de modèle dans les commits.
- DECISIONS : honnête sur E a #117 et B c #23 ; la ligne A a #27 décrit mal quand la phrase apparaît (G1).
- Captures regardées par le relecteur : Clavier juste après « + Nouvelle partie » (« Partie : B · Couplet », dessous « M arrive à la prochaine mesure… », rien de coupé : correct dans ce cas) ; Clavier pendant « Écouter » sans choix (G1) ; Sons en Pro pendant ▶ (G1) ; écran Swing du tuto en DE tablette 8″, FR 360 dp 130 %, FR tablette 11″ couchée (correct).

Ancien verdict : 0 bloquant, 1 grave — fusion impossible

Verdict : 0 bloquant, 0 grave — fusion possible
