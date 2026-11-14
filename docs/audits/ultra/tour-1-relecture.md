# Tour 1 : corrections de la relecture neuve

Relecture neuve du tour 1 : aucun Grave, 6 Moyens, 6 Mineurs ; plus 3 restes de l'intégration.
Chaque point : un test qui échouait avant la correction (vérifié en remettant l'ancien code), vert après.

| Point | Correction | Test | Commit |
|---|---|---|---|
| M1 plusieurs bouts sans pad libre | seul le pad touché est remplacé, les autres bouts vont sur les pads libérés, le reste est dit | `StemBoardRouterTest.everyPadUsedNothingIsCoveredWithoutAsking` | bbba50b |
| M2 « Découper ce que je veux » | la chanson est marquée posée sans partie de 90 mesures : la boucle reste une boucle | `RemixKeepsTheSongTest.aSongTakenToCutLeavesTheLoopALoop` | bbba50b |
| M5 « + Copie » du morceau | `firstEmptyScene` écarte les scènes du morceau (et des prises) ; copie et « Dupliquer » s'en servent | `NewSongPartSceneTest`, `SceneLabelTest.anEmptySceneInTheSongIsNotFree` | bbba50b |
| M3 Pad Live rempli tout seul | écrit dans toutes les étapes d'annulation (`editEverywhere`) ; un pack touché pendant le remplissage passe | `PadLiveSaveUndoTest.aPageFilledOnItsOwnSurvivesUndoAndGivesWayToTheUsersPack`, `HistoryRebaseTest` | 7f7b832 |
| M4 « Montre-moi » et Kick 2 | vide aussi les autres pads du rôle dans la boucle ; commentaire à jour | `TutoCourseTest.showMeClearsTheRolesOtherPads` | f4d7184 |
| M6 Streaming sur tout le morceau | dit dans le changelog (ancien projet réexporté : 1 à 2 dB plus fort, voulu) | relu (5 lignes, `LogicTest`) | 0cc6cd2 |
| m son gardé à moitié appliqué | refusé en entier si ses effets dépassent la limite | `KeptSoundTest.aKeptSoundWhoseEffectsAreRefusedLeavesTheTrackAsItIs` | bbba50b |
| m « moteur pas prêt : réessaie » | « Le moteur audio n'a pas démarré : ferme l'appli puis rouvre-la. » (3 endroits, 15 langues) | `RecordZone4Test.withoutTheEngineEnregistrerSaysWhy` | bbba50b |
| m nom de fichier coupé (take 40) | coupe sur des lettres entières (`takeWhole`) | `WavExporterTest.theCutKeepsLettersWhole` | 297f6c0 |
| m champ de nom (IME, take max) | une composition n'est plus avalée ; coupe sur des lettres entières | `NameFieldTest.theLimitKeepsLettersWhole` (la composition : non testable ici) | 297f6c0 |
| m dossier avec seulement `project.json.tmp` | relu comme une copie (illisible : mis de côté), plus écrasé | `AtomicSaveTest.aFirstSaveCutBeforeItsRenameIsStillTheProject` | 297f6c0 |
| m « C'est fait » du Guide sans changement | non fait : course lecture/écriture pas reproduite à peu de frais | — | — |
| Intégration « Acid / 303 » coupé | espace insécable dans la clé et les 14 traductions | `I18nCatalogTest.theWelcomeLineKeepsAcid303Whole` | 0cc6cd2 |
| Intégration « coup… » des pads groupés | le mot a la largeur du pad, le coin à toucher ne bouge pas | `PadWordWholeTest` | 0cc6cd2 |
| Intégration « Rétablir » introuvable | sans barre du pouce, il garde sa place dans la barre du haut (Simple et Pro) | `TopBarTest.redoWithNowhereElseKeepsItsPlace` | 0cc6cd2 |

Tests lancés : `:project-model:test` (1057, vert) ; les 10 classes ciblées de l'appli (50 tests, vert ; 9 nouveaux tests rouges sur l'ancien code) ; `ScreenScanTest`, `TutoJourneyTest`, `TutoTest`, `remix.*`, `padlive.*` (968 tests, vert) ; `:app:verifyRoborazziDebug` (7855 tests).
Captures refaites et regardées : accueil (5, « Acid 303 » sur une ligne), Pad Live (12, « coup · 1 » entier), recette (changelog).
Suite complète après les captures : 7855 tests, 2 rouges, verts seuls : `RemixSongScreenshotTest.structureEdits` (23 pixels d'anticrénelage sur la pastille « Ligne du temps », écran pas touché) et `ForegroundSoundTest.aFirstLaunchAsksForTheSound` (journal lu trop tôt sous charge). Non réenregistrés.
