# Relecture neuve : Tour 5, banc 4.5 (détecteur de fuite, `reachTag` sur place, cinq gardes « banc »)
Relu : 3c9114f5d746e6a95b28d745ae63a8f2e6765086

Branche `eq/tour-5-banc45`, 7 commits sur `c877908` (rouge `d503254`, vert `87b6693`). Relecteur neuf, qui n'a rien écrit de
cette branche. Lu : `BenchPristine.kt`, les diffs de `HumanTester.kt`, `BenchEngine.kt`, `BenchWindows.kt`, les cinq gardes,
`Tour3RenameIdleTest.kt`, `selftest.sh`, la quarantaine, les journaux de sièges cités, `Engine.cpp`/`AudioTracks.h`/
`RhythmViewModel.songEnded`, le plan (`PLAN-RESTE.md` 4.5, 4.7, R.6). Deux passages Gradle (`gradle-file.sh`), expériences
remises par `git checkout` (arbre propre, aucun code commité par le relecteur).

## Vérifications faites

- **Aucun code principal modifié** : `git diff --stat c877908..HEAD` ne touche que `app/src/test/…/human/`, `docs/`,
  `scripts/bench/`.
- **Le détecteur détecte vraiment** (passage 1, test jetable `ZzLeakProbeTest` non commité, trois méthodes ordonnées) : `a` laisse
  `NativeEngine.sink` et `LaunchRule.askAtStartForTest = false` ; `b` est rouge par la règle, sans exécuter son corps :
  « inherited state not pristine before ZzLeakProbeTest#b_mustBeRedByTheRule (left by the test before: ZzLeakProbeTest#a_leaks):
  LaunchRule.askAtStartForTest = false; NativeEngine.sink = BenchEngine@… » ; `c` est vert (état remis, pas de cascade).
  La remise ne cache rien : `leaks()` est lu avant `restore()`, puis l'échec est lancé ; `KnownDefectRule` ne l'inverse pas
  (message différent → « failed for another reason »).
- **Couverture de la règle** : les 20 fichiers `Bench*Test`/`Tour*Test` de `human/` la portent (héritée de `HumanTester` ou
  posée). Cibles de réflexion existantes : `_instance` (javap sur `lifecycle-viewmodel` 2.11.0, la version du `toml`),
  `DoubleTouchGuard.downAt/armedUntil/dropping`, `ProjectOpening.prepared`, `ClosingSaves.busy`, `TakeVault.busy`.
- **Les gardes neuves sont vertes sur la tête** (passage 1) : `BenchSelfTest#reachTagTouchesAVisibleElementInPlace`,
  `BenchTour4Test#theClearButtonKeepsTheFieldsFocus`, `BenchSongClockTest#aMicTakeEndsWithTheSongsEnd`,
  `BenchEngineTest#anExportSizeIsSaidToBeTheBenchs`.
- **Rouge sur l'ancien banc** (passage 2, `if (shownWhole(tag)) return first` retiré et l'ancien swap de type 2 remis, puis
  `git checkout`) : `BenchSelfTest#reachTagTouchesAVisibleElementInPlace` rouge « the bench's clock moved 24545 ms to reach « help_labels_button », already shown » (le rapport disait 24 545 ms : exact) ; `BenchSongClockTest#aMicTakeEndsWithTheSongsEnd` rouge « the take is over … expected:<2.0> but was:<0.0> » (la prise repassait à « attend le 1 »). Les deux gardes mesurent bien ce qu'elles disent. Logique de R.6 confirmée aussi : pour un élément hors de la bande 20-80 %, l'ancien code fait toujours `performScrollTo` + « attends 0.5 » (≥ 500 ms d'horloge), donc `spent < 300` est faux ; le chemin « glisse » reste pour un élément rogné (`boundsInWindow` < 90 % de sa taille) ou hors écran, et `BenchRecetteTest` y gagne (sa rustine touchait un élément partiellement coupé dès qu'il était dans la hauteur de l'écran).
- **T5-Dc-4 / Ac-61 / Db-16, journaux lus** : A c #50 champ touché, #51 ✕, #52 « ecris » → « 15/24 » (ça marche) ; #60 ✕ **sans**
  toucher le champ, #61 « ecris » → `sound-62.txt` : « Rien ne s'écrit : aucun champ n'attend le texte (touche d'abord le
  champ) », #62 champ touché, #63 « ecris » OK. D b #16-#19 et D c #4-#7 : exactement la même séquence. Le défaut rapporté (« le
  champ perd le curseur ») est faux : il ne l'a jamais eu. « banc » justifié pour le symptôme rapporté (voir la réserve 5).
- **T5-Ab-27, moteur natif** : `Engine::onStep` → `stopTransport` → `finishRecording()` → `state = 2` avec `written` jusque-là ;
  appli : `songEnded()` → `stopAudioRecording()` garde la prise. Le correctif du banc (`freezeTakes(upTo = due·15·RATE/tempo)`)
  est dans le même repère que `recorded()` (images depuis la fin du compte à rebours) : cohérent avec le téléphone.
- **selftest.sh** : motifs vérifiés contre les classes de `human/` (`Tour3NameFieldIdleTest`, `Tour5AppProof2Test` désormais
  attendus et lancés) ; `test_ultra_bench.py` vert ici aussi. `BenchEngine` : attente par tranches de 50 ms, `cancelAllRenders()`
  en tête de `end()` (renderMs remis) ; `BenchShareTest` annule ses rendus de 30 s dans le test lui-même.

## Constats

| Gravité | Constat | Suite |
|---|---|---|
| moyen | **La règle arme aussi les sièges du banc réel.** `HumanTester.drive()` la porte (classes `HumanTesterA..F`, 4 forks pour 6 sièges : deux forks enchaînent deux sièges). Une fuite entre deux sièges d'un même fork rend le second rouge **avant** son `#0 SIEGE` : journal vide, siège perdu pour le lot (avant : siège joué sur un état hérité, donc défauts faux ; aucun des deux n'est bon). `end()` ne remet pas tout ce que `restore()` vérifie (`Intro.playing`, `TutoHints.active`, `TutoTestHooks.kitOffer/packs`, `I18n.pseudo`, `SpotChecks.log`, `ProjectOpening.prepared`, `Retours.active`) : un siège long (intro, tuto, retours) peut en laisser un. Changement de comportement du banc absent du rapport et de DECISIONS. | Une ligne DECISIONS et un choix : en mode siège (`ps.human.dir` posé), la règle remet l'état et écrit « #0 BANC état hérité remis : … » dans le journal au lieu d'échouer ; ou `end()` appelle `BenchPristine.restore()` (une seule liste). Vérifier que `fuse-reports.py`/`sortie-de-siege.py` lisent un siège sans journal. |
| moyen | **`lastTest` ne connaît que les tests porteurs de la règle.** En CI complète (`ci.yml:136`, `:app:testDebugUnitTest` sans filtre, tous les tests de l'appli dans les mêmes forks), une fuite d'un test ordinaire sera imputée au dernier `Bench*`/`Tour*` passé, et c'est la **victime** qui est rouge, pas le coupable. | Dire dans le message « (ou un test sans la règle passé entre les deux) », ou attribuer par un `RunListener` global qui voit tous les tests ; noter dans `docs/REVIEW.md`. |
| moyen | **`verifie-chantier.py` : 5 écarts sur la tête** (les cinq verdicts « banc » sans `garde_banc`), alors que la fin de séance 4 exige 0 écart. Le rapport le sait et liste les noms, mais `verdicts.tsv` n'est pas touché. | Reporter les cinq noms dans la colonne `garde_banc` de `verdicts.tsv` dans le commit de fusion (trois minutes), puis `verifie-chantier.py` à 0. |
| mineur | **4.5 non clos** : critère « 10 selftests verts sans quarantaine » non atteint (rouge au passage 4 entre deux verts), quarantaine maintenue. C'est honnête et bien documenté ; le diagnostic ajouté (`loading=false`, 1 projet, `welcome_later` taille 388×72 mais bornes 0, 3 images forcées, recomposeurs, `mainClock`) est ce qu'il faut pour finir. | Ne pas cocher 4.5 dans `PLAN-RESTE.md` (il n'est pas touché : bien). Piste à tester en premier : `Recomposer.runningRecomposers` et `WindowRecomposerPolicy` (statique) après le `scenario.close()` du test d'avant. |
| mineur | **T5-Dc-4/Ac-61/Db-16 : le rapport ferme un peu vite l'ergonomie.** Les trois testeurs attendaient la même chose : « à l'ouverture de la fenêtre, je peux taper directement » (A c #61, D c #4, D b #16). Les dialogues de nom n'ont pas de `FocusRequester` (seule `RenameProjectBar` en a un) : sur un téléphone aussi, la fenêtre « Renommer » s'ouvre sans curseur ni clavier. Ce n'est pas le défaut rapporté, et « banc » reste juste pour lui ; mais la question « focus et clavier à l'ouverture des dialogues de nom ? » n'est notée nulle part. | Design délégué : décider (une ligne DECISIONS), ou une ligne « à trancher » dans `verdicts.tsv`. Si on donne le focus à l'ouverture, `theClearButtonKeepsTheFieldsFocus` (premier chemin : « aucun champ n'a le curseur ») devra changer. |
| mineur | **T5-Cc-41b : la garde teste une chaîne, pas l'écran.** `anExportSizeIsSaidToBeTheBenchs` appelle `HumanTester.shareNote()` (fonction statique) : vert quoi que fasse le banc à l'écran. Le second chemin (« Mes exports » ouvert ou export en cours → `EXPORTS_NOT_A_PHONES` dans la ligne de son) n'a aucune garde. Reste de 4.7 (profil, `seat-metrics.py`) non fait et dit. | Quand 4.7 est repris : un test `Bench*` qui ouvre « Mes exports » et lit la ligne de son. |
| mineur | **Lectures par réflexion silencieuses.** `BenchPristine.staticField()` rend `null` si le champ manque : `_instance == null` et `prepared == null` passeraient sans bruit après une montée de version qui renomme le champ ; `ClosingSaves busy`/`TakeVault busy` retombent à 0. À l'inverse `HumanTester.end()` lit `_instance` sans garde : un renommage planterait tout le banc. `TakeVault.busy` est public : la réflexion est inutile. | Une seule lecture stricte (échec explicite « champ introuvable ») partagée par la règle et `end()` ; `TakeVault.app.busy.value` en direct. |
| mineur | `BenchWindows.drawnBounds` translate par `drawn.left/top` alors que `drawnPosition` utilise `shift` (dessiné − position réelle) : identiques tant que Robolectric pose chaque fenêtre en (0, 0). | Utiliser `shift` dans les deux. |
| mineur | **Pas de preuve rouge pour `theClearButtonKeepsTheFieldsFocus`** (« vert d'emblée : le banc avait raison »). Acceptable pour une garde de verdict (le plan n'exige que « verte dans le dernier selftest »), mais la garde dépend du libellé du banc (`"touche d'abord le champ" in blind`). | Rien à faire ; si le libellé change, la garde le dira. |
| observation | Passage A : `BenchAmeneesTest#aTapDuringTheExampleIsNotCounted` rouge une fois (« YourTurn(c, 0) attendu »), jamais revu dans les passages 1 à 5 : un intermittent possible, hors de ce lot. | À surveiller dans les prochains `selftest-resume.txt`. |
| RAS | Détecteur, couverture, gardes vertes, rouges sur l'ancien banc, journaux, moteur natif, `selftest.sh` : voir « Vérifications faites ». | — |

Verdict : 0 bloquant, 0 grave — fusion possible
