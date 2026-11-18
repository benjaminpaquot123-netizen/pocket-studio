# Lot « banc9 » (tour 8) : restes banc8 et banc8b, doigt sur cible couverte, attente de « Mes projets »

Base : 2af35df0, puis fusion de l'intégration (bc14c85b) · branche `eq/tour-8-banc9` · commits : rouge ebab732b, vert d769c17c ; relecture n° 1 : rouge 6b90be79, vert 6a02f596.
Fiche : `docs/audits/ultra/tour-8-fiches/fiche-B-banc9.md`. Aucun fichier de `app/src/main` touché, aucune phrase de l'appli.

tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.human.Bench*' --tests '*Tour5Bloquants*' --tests '*Coquille2Seat*'
chemin: banc, cible à moitié dessinée hors conteneur défilable | test: BenchShortSeatTest#aHalfDrawnTargetIsAimedAtInItsDrawnPart
chemin: banc, amenée tuto:techno:9 en ko, phone360, 130 %, Simple | test: BenchAmeneesTest#tutoAnchorAtChapterNineSwipesForNothing
chemin: banc, rejeu sans borne joué jusqu'au bout | test: BenchShortSeatTest#aReplayWithNoBoundIsFaithfulWhenTheOriginIsPlayedOut
chemin: banc, bouton sans étiquette parti du doigt | test: BenchShortSeatTest#anUntaggedButtonThatMovedGivesTheSeatBack
chemin: banc, rejeu dans: sur un conteneur à plusieurs boutons | test: BenchShortSeatTest#aContainerTagWithManyButtonsIsNotReaimed
chemin: Remix, remix_pick autour de remix_pick_thumb | test: BenchShortSeatTest#aTouchOnATagDrawnInsideTheTargetIsOnTheTarget
chemin: banc, écran qui recolle après un écart | test: BenchShortSeatTest#aScreenThatSticksAgainHasNoDiffNow
chemin: banc, écrans d'audit réels en ressources | test: BenchShortSeatTest#theAuditScreensReadAreTestResources
chemin: banc, carte dessinée sur le bouton | test: BenchCoverTest#aCardDrawnOverTheButtonCoversIt
chemin: banc, carte à côté du bouton | test: BenchCoverTest#aCardBesideTheButtonCoversNothing
chemin: banc, anneau du tuto sur le bouton | test: BenchCoverTest#aRingDrawnOverTheButtonLetsTheTouchThrough
chemin: banc, doigt sur un bouton couvert | test: BenchCoverTest#theFingerFailsOnACoveredButtonAndSaysWhoCoversWhom
chemin: banc, barre du bas sous un corps plein écran | test: BenchCoverTest#aBottomBarButtonUnderAFullScreenBodyIsNotCovered

## Les dix points

| Point | Ce qui change | Test (rouge avant, vert après) |
|---|---|---|
| a | `reachTag` vise le centre de la partie dessinée. Aucun glissé quand aucun parent ne défile (plus de `repeat(8)` pour rien). | `BenchShortSeatTest.aHalfDrawnTargetIsAimedAtInItsDrawnPart` |
| b | `performScrollTo` seulement si la partie dessinée est trop petite pour être touchée et qu'un parent défile. | même test, et `BenchAmeneesTest.tutoAnchorAtChapterNineSwipesForNothing` |
| c | `drawnAt` est retiré. Le nouveau `aim()` écrit « BANC forme de … illisible » dans le journal quand il échoue. | garde, par lecture du code (le chemin d'échec dépend de Compose) |
| d | `human-act.py` l. 132 : seulement « fin jouée ou siège fini » (`n == turn(seat)` était toujours vrai). | `test_ultra_bench.py: image_refused_while_the_seat_goes_on` (garde : la condition morte ne changeait rien) |
| e | amenée `tuto:techno:9` en ko, phone360, 130 %, Mode Simple. | `BenchAmeneesTest.tutoAnchorAtChapterNineSwipesForNothing` : chapitre 9, 0 ligne VISEE de glissé |
| f | `handBackNote` avec `replayN = 0` ; rendre la main quand un bouton sans étiquette a quitté le doigt (`goneWords`, ligne « REJEU bouton « w » plus sous le doigt ») ; `reaimTag` refuse `dans:` sur un conteneur à plusieurs boutons ; un toucher sur une étiquette dessinée dans la cible compte (`remix_pick` / `remix_pick_thumb`) ; `ReplayDiffs` remplace `lastDiff`, qui n'était jamais remis à zéro. | `aReplayWithNoBoundIsFaithfulWhenTheOriginIsPlayedOut`, `anUntaggedButtonThatMovedGivesTheSeatBack`, `aContainerTagWithManyButtonsIsNotReaimed`, `aTouchOnATagDrawnInsideTheTargetIsOnTheTarget`, `aScreenThatSticksAgainHasNoDiffNow` |
| g | deux écrans d'audit réels copiés sous `app/src/test/resources/bench/` (tour-6 lotB a écran 2, tour-7 lotB c écran 2), identiques aux originaux. | `BenchShortSeatTest.theAuditScreensReadAreTestResources` |
| h | `TutoAnchor.built(…, skipped)` : chaque pas dont le besoin n'a pas pu être rempli s'écrit `#0 AMENEE pas non fait : <id>` (un pas d'écoute ou de lecture n'est pas un manque). | `tutoAnchorAtChapterNineSwipesForNothing` (aucun pas non fait) |
| i | `ScreenText.coverAt` : un calque (groupe de parcours, ou conteneur M3 comme `Card` / `Surface`) dessiné après la cible, hors de sa chaîne, qui ne laisse pas passer le toucher et ne couvre pas presque toute la fenêtre, la couvre. `ScreenText.finger` : le doigt refuse alors le geste (lignes `COUVERT` et `REFUS doigt couvert : « x » couvre « y »`, pas de `DOIGT`, rien n'est touché). | `BenchCoverTest` : carte dessus et doigt refusé, rouge puis vert ; témoins « carte à côté », « anneau du tuto », « barre du bas sous un corps plein écran » |
| j | `reachTag` attend tant que `projects_loading` est affiché (40 × 0,5 s). | garde |

Tour5BloquantsTest l. 192-193 et 215-216 : aucune attente faite à la main n'y était. Il n'y avait rien à retirer, donc rien n'a changé.

Tests « garde » : ils passaient déjà avant la correction. Ils protègent le comportement, mais ne prouvent pas un rouge. C'est le cas de c, d, h, j et du témoin « anneau » de i.

## Phrases d'acceptation
- « en ko, deux mains, phone360 à 130 %, Mode Simple : l'amenée `tuto:techno:9` m'arrive au chapitre 9 avec 0 glissé inutile dans le journal. » : `tutoAnchorAtChapterNineSwipesForNothing`.
- « sur un écran où un élément dessiné couvre le bouton, le doigt du banc échoue et le journal dit qui couvre qui. » : `BenchCoverTest.aCardDrawnOverTheButtonCoversIt`.

## Relecture n° 1 (grave corrigé)
- Grave : le doigt écrivait `COUVERT` mais touchait quand même. Corrigé : `ScreenText.finger` refuse, `HumanTester` ne joue pas le geste et le dit au siège. Test : `theFingerFailsOnACoveredButtonAndSaysWhoCoversWhom`, rouge à 6b90be79 (ligne `#7 DOIGT target « Valider »`), vert à 6a02f596.
- Vu en local au passage : `coverAt` ne voyait pas la carte (M3 pose `IsContainer`, pas `IsTraversalGroup`) ; `TutoAnchor` signalait à tort les pas d'écoute ; le test de visée hors fenêtre visait une bande de 40 px (trop fine, donc refusée à raison). Les trois sont corrigés dans 6a02f596.

## Vérifications (locales, sur 6a02f596)
- `:app:testDebugUnitTest --tests 'com.pocketstudio.app.human.Bench*' --tests '*Tour5Bloquants*' --tests '*Coquille2Seat*'` : BUILD SUCCESSFUL, 247 tests, 0 échec, 46 ignorés.
- Avant correction (sur bc14c85b) : 245 tests, 3 échecs (`aCardDrawnOverTheButtonCoversIt`, `aHalfDrawnTargetIsAimedAtInItsDrawnPart`, `tutoAnchorAtChapterNineSwipesForNothing`).
- `python3 scripts/bench/test_ultra_bench.py` : OK.
- `scripts/bench/selftest.sh <dossier temporaire>` : vert, 235 verts, 0 rouge, 0 en quarantaine, 44 sautés.
- `scripts/bench/rejeu-defauts.sh` (tous) : 6 sur 6 « oracle tenu » (t4-26-prise-clavier, t6-da15-ecoute-passee, t6-ec17-un-bout-courbe, t6-ga14-phrases-batterie, t6-ha19-cases-panneau, t7-g1-guide-son).
