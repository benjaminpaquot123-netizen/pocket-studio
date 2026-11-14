# Audit ultra, tour 3 : relecture neuve

Diff relu : `ff9b9c3..7b7d74e` (six zones, banc, intégration). Relecteur qui n'a pas écrit le code.

## Résultat
Aucun Bloquant ni Grave. 2 Moyens corrigés, chacun avec un test rouge avant ; le geste de la découpe tranché.

| Où | Gravité | Scénario | Suite |
|---|---|---|---|
| `remix/CutWave.kt` (toucher la courbe) | Moyen (choix de design) | Placer la tête orange pour « Couper ici » cochait aussi le bout touché : on envoyait aux pads des bouts jamais choisis (le test `CutJourneyTest` devait déjà filtrer les bouts cochés « par accident »). | Corrigé : toucher le **numéro** d'un bout (bande du haut, 40 dp) le coche ou le décoche sans bouger la tête ; toucher la courbe ne fait que placer la tête. Numéro rempli de sa couleur quand le bout est choisi. Aide réécrite (15 langues). Tests `CutZoomGestureTest` (3, dont `placingThePlayHeadTicksNothing`, rouge avant). |
| `rhythm/RhythmViewModel.kt` `ExportUi.madeHash` | Moyen | Une étoile gagnée au Défi (ou une autre page de Pad Live) rendait l'export « changé depuis cet export » : plus de « Partager » ni de rapport, alors que le son n'avait pas bougé. | Corrigé : l'empreinte ignore étoiles, page de Pad Live, carte MIDI, versions gardées. Test `Round3Zone2Test.aChallengesStarLeavesTheExportFresh` (rouge avant). |

## Zones sensibles vérifiées
- **Fil audio** : aucun fichier C++ touché ce tour. `stop()`/`stopLive`, prises écoutées seules (`hearTake` → `toggleAlone` existant), export d'une partie du remix (moteur privé d'export fermé dans `finally`), lecture depuis une partie : tout sur le fil principal ou d'export, rien dans le callback. `BenchEngine`/`HumanTester` sont dans `src/test`.
- **Sauvegarde** : `ProjectOpeningViewModel` sur IO : les attentes (prise, sauvegarde de fermeture) restent bornées à 10 s comme avant, en temps réel ; `prepared` est `@Volatile`, `take(id)` vérifie l'id ; une annulation pendant la lecture jette le résultat avant `prepared = …` (pas de fuite). « J'apprends » rouvre un projet existant (aucune écriture de projet, seulement la progression du tuto). La suppression du premier projet intact passe toujours par `flushNow` puis `untouched` (projet + journal Live) : inchangée. ✕ des champs : même chemin qu'un effacement au clavier. « Renommer » une partie renomme sa brique (un pas d'annulation).
- **Plantages** : `RhythmStrip` suppose un défi non vide (déjà supposé par `PadChallenge.score`) ; regex coréennes, contractions, `QuotedNames` : motifs échappés, pas d'exception possible ; formats pluriels gardés par `I18nCatalogTest` (placeholders des formes `|few`, `|many`, `|two`) ; plus de défilement imbriqué dans le bloc kit du tuto.
- **Fuites** : `OfflineExport` fermé dans `finally` pour chacun des 4 fichiers du remix ; `HoldAlone` relâché au départ de l'écran ; aucun écouteur nouveau sans `DisposableEffect`.

## Tests relus (« rouges avant »)
`TutoKitUnderFingerTest` (vrai : panneau remplacé avant), `Tour3Zone1Test` (commandes moteur réelles), `Zone4Round3Test`, `TalkBackNamesTest`, `CountWordsTest` (lecture des sources) : vérifient bien le défaut. `Round3Zone2Test` : surtout des fonctions nouvelles (rouges car absentes) ; `theRemixGivesOneFilePerStem` ne vérifie que la liste des fichiers, pas que chaque rendu n'a qu'une partie (reste).

## Restes (Mineurs, dans `docs/REVIEW.md`)
1. Rendu « une partie du remix seule » (`describe`, `REMIX_STEM_BASE`) sans test de rendu.
2. `madeHash` calcule le hash de tout le projet à chaque modification, en composition (`AppRoot.kt`, carte Exporter) : à mettre en cache si un gros projet rame.
3. `ProjectOpeningViewModel.opened` écrit sur IO, lu dans `onCleared` sans `@Volatile` (sans effet : `take` vérifie l'id).
4. `trCount` en français : « 0 mesures » (le français dit « 0 mesure »).
5. `PackSearch.fold` retire aussi les signes voyelles hindi et le dakuten japonais (ド = ト) : recherche plus large, cohérente des deux côtés.
6. `Contractions` contracte aussi un nom tapé qui commence par un article (« de O Rappa » → « do Rappa »).
7. Bloc kit du tuto invisible quand le panneau est replié (progression du téléchargement cachée).
8. `tutoPadNames` reconstruit une regex à chaque recomposition du panneau.
