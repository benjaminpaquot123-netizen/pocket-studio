# Tour 9, lot L16 : relecture neuve
Relu : e4a68848001b5528afa682f836bc17aa8243683d

Relecteur neuf (n'a pas écrit ce code). Lu : les commits propres du lot (19ec667d, e5ae6b3d, 77e1c53e, a2ccda05, 60842d8f, 001a8c94, dd4d67fc, 9e8b0ff4, e92ec6eb, bfce7c01, fe6c0edb), pas le travail des autres lots apporté par la fusion e4a68848.

## Contrôles généraux

- Paires rouge:/vert: : 5 paires, chacune avec son test nommé ; les commits « rouge: » ne touchent le code de l'appli que pour rendre le test compilable sans changer le comportement (extraction de `orderLine` à l'identique dans 60842d8f ; `longPressCells` = `emptySet()` et `dottedState` qui rend l'ancien texte dans e92ec6eb).
- Aucun `@Ignore`, `assume` ni test sauté dans les tests du lot.
- Thread audio : aucun fichier du moteur ni du callback touché. Sauvegarde : rien de touché (textes, tuto, sémantique TalkBack).
- DESIGN.md : aucun changement visuel (textes et `stateDescription` seulement) ; la ligne de partage devient plus courte. Pas de capture à refaire, d'accord avec le rapport.
- Tests existants qui pouvaient changer de couleur : `ShareReadyTextTest` et `ShareNotSentTest` suivent les nouvelles clés (a2ccda05) ; plus aucune trace des anciennes clés « C'est prêt : %s… » dans le code ni les tests ; `Tour6TutoCellsPanel100Test` (lit « en pointillé : à toucher », house Facile, kick : pas d'appui long) relancé vert.

## Constats par défaut

| Défaut | Gravité | Constat | Fichier:ligne |
|---|---|---|---|
| F078 | mineur | « Encore : Refrain, Intro. » quand l'Intro est déjà posée peut surprendre un débutant (il a déjà une Intro) ; c'est fidèle au texte du cours (« puis Intro pour l'Outro »), donc pas bloquant. Piste : « Intro (pour l'Outro) ». | app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt:962 |
| F078 | — | `Fin` est dans `Scene.SECTIONS` et garde son nom ; seul `Outro` devient « Intro ». Logique juste, testée des trois côtés. | TutoController.kt:962-967 |
| F093 | mineur | `aTapIsEnoughWhereTheWordsSayTouch` est déjà vert sur le rouge (stub vide) : c'est un garde-fou, pas une preuve ; la preuve est `theClapGhostIsALongPressInPro` (rouge vu). | project-model/src/test/kotlin/com/pocketstudio/model/tuto/Tour9L16LongPressCellsTest.kt:25 |
| F093 | mineur | `longPressCells` calcule ses cases depuis la ligne brute, `TutoPlay.hint` peut les déplacer (crash, partie) ; sans danger car `dottedState` ne dit « appui long » que pour une case à la fois pointillée et dans `longPress` (au pire « à toucher », comme avant). `longPress` n'est pas rangé par pad, mais vient du même pas que `cells`. | project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoPlay.kt:150 ; app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt:165 |
| F093 | mineur | `dottedState` appelé deux fois par case dans `StepGrid` (condition puis valeur) : coût négligeable, lecture d'état Compose correcte (`cells` et `longPress` sont des `mutableStateOf`, la grille se recompose). | app/src/main/java/com/pocketstudio/app/rhythm/RhythmScreen.kt:848 |
| F093 | — | `LaunchedEffect` d'AppRoot : clé `tutoUi?.progress?.level` ajoutée, donc changer de niveau recalcule `longPress` ; le collecteur de scène passe aussi par `showHint` : plus d'endroit qui pose `cells` sans `longPress`. `reset()` vide `longPress`. | app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt:2035 ; TutoController.kt:249, 694 |
| F081/F084/F087 | mineur | Le test F084 vérifie une inclusion de mot (« Arrêter » dans « Arrêter l'écoute ») : suffisant ici (es « Detener la escucha », hi « सुनना रोको », pl « Stop odsłuchu »), mais une langue à déclinaison pourrait le rougir à tort plus tard. Traductions plausibles ; ko « 히트 %d개를 지우세요 » dit bien d'enlever ; tr « aşama » partout pour chapitre, « bölüm » gardé pour la partie. | project-model/src/test/kotlin/com/pocketstudio/model/i18n/Tour9L16TextsTest.kt:35 |
| F086 | — | `why` ajouté à `snare_late` (uk_garage) et `conga_late` (afrobeats), 14 langues relues en échantillon (en, de, es, it, ja, ko, nl, hi, id, ar) : sens juste. | project-model/src/main/resources/tutos/uk_garage.json, afrobeats.json |
| F089 | — | Nouvelles clés sans « C'est prêt : », titre non répété dans les 14 langues (test balaie toutes les langues). | app/src/main/java/com/pocketstudio/app/export/ShareSongUi.kt |
| F099 | — | Les 7 `Text("OK")` passent par `tr("OK")` ; la clé « OK » existe dans les 14 langues ; le test balaie `src/main/java` et rougira si un `Text("OK")` revient. | app/src/test/java/com/pocketstudio/app/export/Tour9L16ShareTextsTest.kt:35 |

Aucun constat grave, aucun moyen.

## Relancé

- `bash scripts/bench/gradle-file.sh :project-model:test --tests '*Tour9L16*'` sur e4a68848 : BUILD SUCCESSFUL ; Tour9L16TextsTest 3/3, Tour9L16TutoOffsetWhyTest 1/1, Tour9L16LongPressCellsTest 3/3, 0 sauté.
- `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Tour9L16*' --tests '*ShareReadyTextTest' --tests '*ShareNotSentTest'` sur e4a68848 : BUILD SUCCESSFUL ; Tour9L16TutoOrderNamesTest 3/3, Tour9L16LongPressStateTest 1/1, Tour9L16ShareTextsTest 2/2, ShareReadyTextTest 1/1, ShareNotSentTest 2/2, 0 sauté.
- Voisins : `Tour6TutoCellsPanel100Test` (1 vert, 1 sauté d'avant le lot par son `assumeTrue`) et `Tour6TutoOrderRaceTest` 1/1 : verts.
- Rejeux rouges dans un worktree jetable (retiré ensuite) :

rouge constaté : 001a8c94 : rejoué sur 60842d8f : Tour9L16TutoOrderNamesTest 3 tests, 2 rouges (« Encore : Refrain, Outro. » names an Outro ; « Pas dans l'ordre : le morceau commence par « Outro ». » names an Outro), aRealEndPartKeepsItsName vert.
rouge constaté : bfce7c01 : rejoué sur e92ec6eb : Tour9L16LongPressCellsTest 3 tests, 2 rouges (theClapGhostIsALongPressInPro « got [] of [4, 12, 15] » ; theLongPressStateIsTranslated : clé absente en en, nl, es, pt-BR, de, it, pl, tr, id, hi…), aTapIsEnoughWhereTheWordsSayTouch vert (garde-fou).

## Verdict

Les cinq corrections font ce que disent le rapport et les décisions, les tests prouvent le changement (deux rouges rejoués), rien ne touche le son ni la sauvegarde. Restent cinq mineurs, sans action exigée.

Verdict : 0 bloquant, 0 grave — fusion possible
