# Lot « banc9 » (tour 8) : restes banc8 et banc8b, doigt sur cible couverte, attente de « Mes projets »

Base : 2af35df0 · branche `eq/tour-8-banc9` · commits : rouge ebab732b, vert d769c17c.
Fiche : `docs/audits/ultra/tour-8-fiches/fiche-B-banc9.md`. Aucun fichier de `app/src/main` touché, aucune phrase de l'appli.

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
| h | `TutoAnchor.built(…, skipped)` : chaque pas non fait s'écrit `#0 AMENEE pas non fait : <id>`. | garde (aucun pas sauté sur les profils testés) |
| i | `ScreenText.coverAt` : un groupe dessiné après la cible, hors de sa chaîne et qui ne laisse pas passer le toucher, la couvre. Le doigt échoue alors : ligne `COUVERT « x » couvre « y »`, et le refus dit qui couvre. | `BenchCoverTest` : carte dessus, rouge puis vert ; témoins « carte à côté » et « anneau du tuto » (qui laisse passer) |
| j | `reachTag` attend tant que `projects_loading` est affiché (40 × 0,5 s). | garde |

Tour5BloquantsTest l. 192-193 et 215-216 : aucune attente faite à la main n'y était. Il n'y avait rien à retirer, donc rien n'a changé.

Tests « garde » : ils passaient déjà avant la correction. Ils protègent le comportement, mais ne prouvent pas un rouge. C'est le cas de c, d, h, j et du témoin « anneau » de i.

## Phrases d'acceptation
- « en ko, deux mains, phone360 à 130 %, Mode Simple : l'amenée `tuto:techno:9` m'arrive au chapitre 9 avec 0 glissé inutile dans le journal. » : `tutoAnchorAtChapterNineSwipesForNothing`.
- « sur un écran où un élément dessiné couvre le bouton, le doigt du banc échoue et le journal dit qui couvre qui. » : `BenchCoverTest.aCardDrawnOverTheButtonCoversIt`.

## Vérifications
- `python3 scripts/bench/test_ultra_bench.py` : OK.
- Gradle local en panne de dépendances (kotlin-stdlib, kotlin-tooling-core, jspecify… introuvables ; 8 essais, `--refresh-dependencies` compris). Rien n'a donc tourné ici, le Kotlin non plus : 0 test de banc local, et le selftest est rouge à « la classe n'a pas tourné » (42 classes). C'est la CI qui juge : job « Build, unit tests, screenshots » et « Captures : 0 différentes ».
- Rejeux (`rejeu-defauts.sh`) non joués ici pour la même raison.
