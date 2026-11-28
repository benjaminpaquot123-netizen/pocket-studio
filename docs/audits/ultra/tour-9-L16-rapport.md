# Tour 9 : lot L16 (rapport)

Base : `ccr-80af1a1b-mn0wec`. Branche : `eq/tour-9-L16`. Décisions : `docs/decisions/tour-9-L16.md`.

## Points et preuves (tous « tenté »)

| Défaut | Siège | Ce qui change | Rouge | Vert |
|---|---|---|---|---|
| F078 | t9-47 (es) | « il manque » / « dans le désordre » nomment le bouton Intro pour l'Outro ; Fin garde son nom | 60842d8f | 001a8c94 |
| F081 | t9-06 (ko) | coups barrés : « à enlever », plus « tu as tapé de plus » | 19ec667d | e5ae6b3d |
| F084 | t9-47 (es) | « Arrêter l'écoute » reprend le mot d'« Arrêter » (es, hi, pl) | 19ec667d | e5ae6b3d |
| F086 | t9-75 (nl) | Pourquoi ? des pas « en retard » (uk_garage, afrobeats) parle du réglage de l'étape, 14 langues | dd4d67fc | 9e8b0ff4 |
| F087 | t9-26 (tr) | chapitre = « aşama », jamais « bölüm » (la partie) | 19ec667d | e5ae6b3d |
| F089 | t9-40 (fr) | la ligne prête du partage ne répète plus « C'est prêt » | 77e1c53e | a2ccda05 |
| F093 | t9-68 (zh) | case en pointillé à appui long (Pro, coup doux ou roulement) dite « appui long » à TalkBack, 14 langues | e92ec6eb | bfce7c01 |
| F099 | t9-57 (tr) | les `Text("OK")` en dur passent par `tr` | 77e1c53e | a2ccda05 |

tests: *Tour9L16* *ShareReadyTextTest* *ShareNotSentTest* *Tour6TutoOrderRaceTest* *TutoPanel* *I18n*

## Chemins

chemin: partage d'un morceau, message « prêt » | test: Tour9L16ShareTextsTest#theReadyLineDoesNotRepeatTheTitle
chemin: partage d'un morceau, message « prêt » | test: ShareReadyTextTest#theFileIsSaidReadyAndWhereItsCopyIs
chemin: boutons OK des dialogues (6 écrans) | test: Tour9L16ShareTextsTest#noScreenWritesOkByHand
chemin: panneau du tuto, liste « il manque » | test: Tour9L16TutoOrderNamesTest#whatIsLeftNamesTheIntroButtonForTheOutro
chemin: panneau du tuto, parties dans le désordre | test: Tour9L16TutoOrderNamesTest#outOfOrderNamesTheIntroButtonForTheOutro
chemin: panneau du tuto, partie Fin | test: Tour9L16TutoOrderNamesTest#aRealEndPartKeepsItsName
chemin: grille du rythme, case en pointillé (TalkBack) | test: Tour9L16LongPressStateTest#aLongPressCellSaysLongPress

Hors appli (module `project-model`) : `Tour9L16TextsTest` (F081, F084, F087), `Tour9L16TutoOffsetWhyTest` (F086), `Tour9L16LongPressCellsTest` (F093).

## Captures

Aucune capture Roborazzi ne montre ces textes (la ligne de partage n'est pas dans `ScreenshotTest`, l'état « pointillé » n'est que de la sémantique) : rien à réenregistrer.

controle: reporté (mode boost, grande vérification de fin)
