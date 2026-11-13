# Relecture du lot « padlive2 » (tour 6)
Relu : 4e516875984ab682d4144f7b9c172b5c73f6b43c

Périmètre : `git diff bdb777a9..ee4c2a92 -- app/` (PadLiveController, PadLiveRoute, PadLiveScreen, PartyMode et leurs tests). Lecture seule, pas de Gradle.
Thread audio : rien de touché côté moteur ; aucun problème d'allocation ou de verrou.

## P2 : Pad Live s'ouvre en silence

- **mineur** : `app/src/test/java/com/pocketstudio/app/padlive/PadLiveSilentOpenTest.kt:19-25`. Ce test se contente de chercher du texte dans les fichiers sources (`"welcome()"`, `"fun welcome("`). Il resterait vert si un autre chemin faisait sonner un pad à l'ouverture, par exemple une fonction renommée ou un `toggle(pad)` appelé ailleurs dans la route. Scénario : un futur lot rajoute un « coup d'accueil » sous un autre nom, le test reste vert et le siège entend de nouveau le Clap. Il vaudrait mieux un test de comportement : ouvrir la route avec `welcome = true`, puis vérifier que `toggleForTest` ne reçoit rien et que `welcomed()` est bien appelé.
- Le code lui-même est correct : `LaunchedEffect(welcome)` ne fait plus que `welcomed()`, et `welcome()` a disparu avec son job. Rien ne fuit.

## P3 : une deuxième prise s'ajoute à la suite

- **moyen** : `app/src/main/java/com/pocketstudio/app/padlive/PadLiveController.kt:257`. `vm.setTakeAppend(...)` écrit dans `_takeAppend`, et ce flux est partagé avec le choix « Nouvelle version / Ajouter à la suite » de la page Live (`LiveScreen.kt:827-828`, `AppRoot.kt:4386`). Pad Live écrase donc en silence le choix fait par l'utilisateur sur la page Live, dans les deux sens.
  - Scénario 1 : sur la page Live, l'utilisateur choisit « Nouvelle version ». Il ouvre Pad Live et fait une deuxième prise (la valeur passe à `true`). De retour sur la page Live, « Ajouter à la suite » est coché, et sa prise suivante s'ajoute au lieu de remplacer.
  - Scénario 2 : sur la page Live, il choisit « Ajouter à la suite » avec un morceau fait à la main (`liveTake` vide). Une prise dans Pad Live remet la valeur à `false`.
  - Correction possible : sauvegarder puis restaurer la valeur autour de la prise, ou passer le mode « à la suite » à `armPerformance` sans toucher au choix de la page.
- **moyen** : `app/src/main/java/com/pocketstudio/app/midi/AutoKnobsVm.kt:330`. Le bouton Record d'un contrôleur MIDI appelle `vm.togglePerformance()` directement, sans passer par `PadLiveController.record`. Or les contrôleurs servent bien dans Pad Live (lot B2). Le défaut n° 49 se reproduit donc par ce chemin.
  - Scénario : une première prise de 4 mesures est gardée dans Pad Live. L'utilisateur lance la deuxième prise avec le bouton Record de son contrôleur : elle remplace le morceau, qui retombe à « 1 mes. ».
  - Le morceau précédent reste en version et se récupère par ↶, donc il n'est pas perdu. Mais c'est exactement le symptôme que le lot devait corriger, et la règle « vérifier partout où elle peut se reproduire » n'est pas tenue.
- **moyen** : `PadLiveController.kt:257` combiné à `RhythmViewModel.kt:5107-5112`. En mode « à la suite », un morceau plein (128 parties, `songFullForTake`) bloque toute prise. Le message affiché, « choisis « Nouvelle version » pour enregistrer », renvoie vers un bouton qui n'existe pas dans Pad Live.
  - Scénario : après quelques longues prises riches en lancements (chaque lancement compte une partie), le ● de Pad Live ne fait plus rien, à part afficher ce message. L'utilisateur reste coincé sans savoir quoi faire.
  - Même famille : au-delà de `effectsEndBar` (`RhythmViewModel.kt:5113-5115`), une prise ajoutée à la suite perd ses effets, et l'avertissement correspondant parle lui aussi de choix absents de Pad Live.
- **mineur** : `PadLiveController.kt:257`. La règle « `liveTake` non vide » se trompe sur un projet « Live » dont le morceau a été construit à la main, ou importé, sans prise. La première prise de Pad Live remplace alors ce morceau. Il est gardé en version et récupérable par ↶, donc l'impact reste faible. L'annulation d'une prise, elle, est saine : `takeUndoTarget(before, …)` repart de `before`, donc `liveTake` revient à sa valeur d'avant. Les états Armed, Recording et Writing ne sont pas touchés, puisque le choix n'est fait qu'en Ready ou en Done (Done passe bien par `armPerformance`, branche `else` de `togglePerformance`).
- **mineur** : `app/src/test/java/com/pocketstudio/app/padlive/PadLiveTakeAfterTest.kt:62-80`. Le test vérifie seulement le drapeau `takeAppend`, pas qu'une deuxième prise réellement jouée (arm → enregistrement → fin) laisse le morceau à 4 + N mesures. Il ne vérifie pas non plus que `PadLiveRoute` branche bien `vm.padLive.record` sur le ● : un retour en arrière de `PadLiveRoute.kt:178` vers `actions.record` laisserait ce test vert. Le bench (`BenchAmeneesTest`, `tap("live_record")`) couvre en partie ce branchement, mais pas le cas d'une deuxième prise.

## P4 : le ▶ du haut montre ■ quand des boucles jouent

- **moyen, corrigé** (ce89f794, voir plus bas) : `app/src/main/java/com/pocketstudio/app/padlive/PadLiveScreen.kt:135-138`. `soundsNow` vaut vrai dès qu'un pad est à un état différent de 0, y compris un **coup** (pad SHOT). Le moteur le met à l'état Playing (2) tant qu'il sonne (`ClipLauncher::State`).
  - Effet 1 : transport arrêté, chaque frappe sur un coup fait passer brièvement le bouton du haut au ■ vert, ce qui donne un clignotement à chaque coup.
  - Effet 2 : un toucher sur ce bouton pendant qu'un coup résonne appelle `cutEverything` au lieu de lancer la musique.
  - Scénario « curieux » : il tape le Clap (ou une cymbale longue), puis tout de suite ▶ pour lancer un rythme. Le coup est coupé, rien ne démarre, et il doit retoucher le bouton.
  - Correction possible : ne compter que les boucles (`PadLive.modeOf(clip) != SHOT`, ou `oneShot == false`).
- **mineur** : `PadLiveScreen.kt:138`. Quand des boucles jouent toutes seules, le bouton du haut ne permet plus de démarrer le transport (métronome, scène) sans d'abord tout couper. C'est voulu, mais c'est un changement de comportement à garder en tête. Autre conséquence : `cutEverything` termine aussi un « Enregistrer ce que je joue » en cours (`padLive.played.endForCut()`). Le bouton ▶, qui avant ne touchait jamais à cet enregistrement, peut désormais l'arrêter.
- Tests : `PadLivePlayShowsTest` couvre bien les trois cas (boucles seules, pad en attente, transport en route) dans les deux écrans (barre et « Fête »), et `BenchAmeneesTest` est adapté proprement. Aucun cas n'essaie un coup isolé, ce qui explique que le défaut ci-dessus n'ait pas été vu.

## Reprise sur e053f20e (`git diff a95b0cb4..e053f20e -- app/`)

- Moyen P4 **corrigé** : `PadLiveScreen.kt:134-137`. `soundsNow` ne compte plus que les pads à l'état différent de 0 dont le clip n'est pas un coup (`oneShot == false`, ce qui couvre les boucles et les pads HOLD). Un coup qui résonne laisse le bouton sur ▶ « Jouer », et un toucher lance bien la musique (`togglePlay`). L'indexation `pads.at(pad)` est globale aux quatre pages, comme `states`, donc cohérente avec le reste de l'écran. « Fête » en profite aussi, puisque l'écran utilise la même propriété.
- Le test `PadLivePlayShowsTest#aHitRingingKeepsPlay` (pad 1, le Clap, joué en coup) prouve la correction : il échouait avec l'ancien code (bouton sur « Arrêter », toucher envoyé à « stop all »). Les cas existants restent valides, car les pads 0, 2 et 3 du jeu de départ sont des boucles.
- **mineur** : `PadLiveScreen.kt:137`. Un pad HOLD tenu compte comme « ça joue », donc le bouton passe à ■ tant que le doigt reste posé. C'est cohérent, puisque ça sonne vraiment, et sans gêne pratique : le doigt est occupé. Je le note seulement.
- Rien d'autre n'a changé dans `app/`. Les trois autres moyens (choix partagé avec la page Live, bouton Record du contrôleur MIDI, morceau plein) restent ouverts et sont notés dans `tour-6-padlive2-rapport.md`.

## Reprise sur 6af680f4 (`git diff ffbcb886..6af680f4 -- app/src/main/java/com/pocketstudio/app/live app/src/test/java/com/pocketstudio/app/padlive`, fichiers de la fusion 61e41ce0 ignorés)

- `LiveScreen.kt:436` et `LiveScreen.kt:684-698` (`LiveTopBar`) : la correction est juste. Le bouton ▶ de la page Live, et de « Plus › Briques », passe à ■ dès que ça joue (`ui.playing || padsOn`). Transport arrêté avec des boucles qui jouent seules, un toucher appelle `padsActions.stopAll`. Dans l'appli, c'est `rhythmVm::cutEverything` (`AppRoot.kt:2655`, `AppRoot.kt:2690`). Transport en route, le toucher garde `togglePlay`. Le bouton s'appuie sur des états frais, car `LiveRoute` interroge `pollLiveStates()` tant que la page est ouverte (`AppRoot.kt:4407`), quel que soit l'onglet affiché.
- `LiveScreen.kt:707-709` (`loopsOn`) : même règle que `PadLiveUi.soundsNow`. Un coup qui résonne ne compte pas, et un état à `null` donne `false`. Elle reste cohérente avec Pad Live.
- **mineur** : `loopsOn` (`LiveScreen.kt:707`) et `PadLiveUi.soundsNow` (`PadLiveScreen.kt:136`) recopient la même règle à deux endroits. Si l'une change sans l'autre (par exemple pour un futur mode de pad), les deux barres ne diront plus la même chose. Une fonction commune, par exemple dans `PadLive`, éviterait cet écart.
- `PadLivePlayShowsTest#theBricksPageSaysTheSame` : le test prouve la correction. Avant elle, le bouton disait « Jouer » et le toucher envoyait « play ». Aucun test ne joue en revanche le cas « coup qui résonne » sur la page Live : c'est mineur, puisque la règle est la même que celle déjà testée par `aHitRingingKeepsPlay`.
- `BenchAmeneesTest#padLiveLightsStopsAndRecordsAsOnAPhone` (adaptation de ee4c2a92, inchangée depuis) : je le juge **non affaibli** pour ce qu'il prouvait du ■ de la barre. Il vérifie toujours que le moteur reçoit `toggleClip -1`, que les pads se taisent et que le voyant s'éteint, et il ajoute que le transport reste arrêté, puis le cycle ▶/■. **mineur** : un sous-chemin n'est plus joué au banc, celui où le ■ du transport en route coupe aussi une boucle (`BenchPadLiveStopTest` passe par « Tout couper », pas par ce bouton). On pourrait le remettre en quelques gestes : pad, ▶, ■, puis vérifier.

Verdict : 0 bloquant, 0 grave

## Reprise sur 82cf7318 (`git diff 61769181..82cf7318 -- app/src/main project-model/src app/src/test`, fusion 82cf7318 ignorée : une ligne de `scripts/bench`)

P4, plein écran des pads (`a155a357` rouge, `86faca39` vert) :
- `LivePadsFullScreen.kt:196-199` : juste. Le bouton du haut reçoit `playing || ui.loopsOn()` et, transport arrêté avec des boucles seules, son toucher appelle `actions.stopAll` ; transport en route, il garde `togglePlay`. Même règle que `LiveTopBar` (`LiveScreen.kt:436`) et que `PadLiveUi.soundsNow`, donc les trois barres disent la même chose. `togglePlay` nul : le bouton reste caché, comme avant. L'horloge des pads (`active`, ligne 167) comptait déjà tout pad allumé, rien ne change de ce côté.
- `PadLivePlayShowsTest#thePadsFullScreenSaysTheSame` prouve la correction : sans elle, le bouton dit « Lecture » (l'assertion « Arrêter » échoue) et le toucher envoie « play ».
- **mineur** : aucun test du cas « coup qui résonne » ni « transport en route + boucles » sur ce plein écran ; la règle est la même que celle testée ailleurs (`aHitRingingKeepsPlay`), le risque est faible.

P3, longueur d'une boucle avec ses briques (`8cfec2b6` rouge, `41b7c2f5` vert) :
- `SongLength.kt:25-27` : juste pour le cas visé. L'indice de scène est cohérent : `withAudioInCell` (`LiveCells.kt:289`) pose `scene = scene` avec le même indice que `Project.scene` / `allScenes`, et `withTake` (`Takes.kt:73`) met `scene = take.scene` hors morceau. `loopSteps` à 0 ou négatif donne 0 brique (arrondi `(x + spb - 1) / spb` → 0), donc la scène seule compte ; pas de division par zéro (`spb` ≥ 1).
- Lecteurs de `SongLength.of` : seulement `LaunchChoice.kt:176` (carte de l'accueil) et `ProjectCatalog.kt:50` (`ProjectInfo.bars` / `seconds`, affichés par `ProjectsScreen.kt:318`). Le tri du catalogue se fait sur `modified`, pas sur la longueur. Le transport, l'export, le mode morceau (`partSteps` / `sceneSteps`), la sauvegarde et l'horloge du studio (`SessionScreen.kt:504`, qui n'utilise que `SongLength.seconds`) ne lisent pas ce calcul : aucun changement de comportement hors affichage. Avec un morceau, la branche `p.song` n'a pas bougé.
- `SongLengthBricksTest#aLoopLastsAsLongAsItsLongestBrick` prouve la correction (1 mes. avant, 6 après). `anotherScenesBrickOrASongClipDoesNotCount` passerait aussi sans la correction : c'est un garde-fou, pas une preuve, ce qui est normal.
- **mineur** : `SongLength.kt:26`. Un clip de scène sans scène (`scene == null`, `song == false` : vieux projet, fichier importé par `RhythmViewModel.kt:2030` ou `:5296`) joue « dans toutes les parties » (doc d'`AudioClip.scene`) mais n'est pas compté. Une boucle importée de 8 mesures sur une scène d'une mesure affiche encore « Boucle de 1 mes. ». Ce n'est pas une régression (jamais compté avant) ; `it.scene == null || it.scene == p.scene` le couvrirait.
- **mineur** : `SongLength.kt:26`. Une prise de scène posée par `withTake` porte `once = true` (jouée une fois par passage, pas en boucle) : elle allonge quand même « Boucle de N mes. », alors que la scène, elle, reboucle sur sa propre longueur. Cas rare hors Live (les briques du Live ont `once = false`), affichage seulement.
- **mineur** : la brique de remix de la scène (`remix.cell(scene)?.loopSteps`, comptée par `slotLoopSteps`, `Live.kt:212`) n'entre pas dans le calcul. Un projet remix dont la brique dure plus que la scène garde une longueur trop courte sur la carte. Pas une régression.
- Remarque : si l'utilisateur fait ensuite un morceau de cette scène, la partie dure `sceneSteps` (sans les briques). La carte passe alors de « Boucle de 6 mes. » à « 1 mes. · 1 partie ». C'est cohérent avec ce que joue le morceau, mais le saut peut surprendre ; hors périmètre du lot.

Bilan de cette reprise : 0 bloquant, 0 grave, 0 moyen, 4 mineurs.

Reprise sur 4e516875 : ce commit (le seul après 16db7a51) ne touche que des traces du banc sous `docs/audits/ultra/sieges/tour-6/padlive2/lotR/` (marqueur `.banc-en-cours` retiré, `incidents-lot.tsv` et `lot.json` ajoutés) ; ni code de l'appli ni tests, rien à relire de plus.

Verdict : 0 bloquant, 0 grave
