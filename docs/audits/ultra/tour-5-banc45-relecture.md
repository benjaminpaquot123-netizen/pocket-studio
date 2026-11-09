# Relecture neuve : Tour 5, banc 4.5 (détecteur de fuite, `reachTag` sur place, cinq gardes « banc »)
Relu : b5c315642d7b8808bc75bbed6f779fe5817e1a34

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

## Relecture du delta (`6b35430..b5c3156` : `30f2fcf`, `a3d57ae`, `b5c3156`)

Même relecteur neuf, qui n'a rien écrit de ce delta. Le tableau ci-dessus est celui de la première relecture (tête `3c9114f`) ;
ses deux premiers « moyen » sont traités par ce delta, le troisième (`verifie-chantier.py`, 5 écarts) ne l'est pas (voir plus bas).
Trois passages Gradle (`gradle-file.sh`), mutations remises par `git checkout`, test-sonde supprimé, arbre propre.

**1. Le mode siège ne vaut que pour un vrai siège.** `HumanTester.seatJournalFor` reprend exactement les trois conditions de
`drive()` (méthode `drive`, `ps.human.dir` non vide, `start.txt` du siège présent) ; les classes `Bench*` qui héritent de
`HumanTester` ont des noms de siège (« selftest », « tour4 »…) qu'un lot n'a pas ; les classes sans `HumanTester` gardent la règle
nue (`{ null }`). Preuve de bout en bout (passage 3, fuite injectée dans le constructeur de `HumanTester`, dossier de lot jetable
avec `a/start.txt` et `cmd-1.txt` = « fin », `-Phuman.dir`) :
- `HumanTesterA#drive` vert, son journal (qui avait déjà une ligne, gardée) reçoit « #0 BANC : état hérité remis (laissé par le
  test d'avant, aucun dans cette JVM, ou par un test sans la règle passé entre les deux) : I18n.lang = EN; TutoTestHooks.kitOffer
  = false », puis « #0 SIEGE lang=fr … », l'écran 1, « #1 FIN : fin » ; `fuse-reports.py` sur ce dossier range la ligne dans
  `incidents-banc.tsv` au nom du siège `a` ;
- `HumanTesterB#drive` (pas de `start.txt`) rouge, `BenchSelfTest#reachTagTouchesAVisibleElementInPlace` et `BenchSelfTest#drive`
  rouges, tous trois avec le message à deux suspects, alors même que `-Phuman.dir` est posé.
Il ne cache donc pas de fuite dans un test qui n'est pas un siège ; en selftest et en CI (`ps.human.dir` absent) il ne s'applique jamais.

**2. `end()` appelle `restore()`.** Ce qui s'ajoute à l'ancienne fin : `I18n.pseudo`, `kitOffer`/`packs`, `Retours.active`,
`Intro.playing`, `TutoHints.active`, `SpotChecks.log`, `ProjectOpening.prepared`, la suppression de `projects/` et
`CurrentProject`. Rien n'est remis trop tôt : `end()` est le `@After` de la classe mère (après ceux des classes filles), personne
ne l'appelle en cours de test, et `drive()` sauve l'état du siège (`SeatState.save`) dans son `finally`, avant. Passage 1 (tête
propre) : `BenchPristineRuleTest` 4/4, `BenchSelfTest`, `Tour3AppProofTest` 3/3 verts. Passage 2 : `BenchTour4Test`, `BenchTour3Test`,
`BenchRecetteTest`, `BenchSeatOptionsTest`, `BenchPlayThenTabsTest` verts (20 tests enchaînés dans un fork, chacun contrôlé par la
règle après le `end()` du précédent). Le rapport ne montre pas de selftest complet après ce delta : à faire avant la fusion (le lot suivant le lance de toute façon).

**3. Mutations** (passage 2 : les quatre tests de `BenchPristineRuleTest` rejoués sous chaque mutation de la règle) :

| Mutation | Test rouge |
|---|---|
| ligne de journal retirée | `aLeakBeforeASeat…` (`FileNotFoundException` sur `a/journal.txt`) |
| `restore()` retiré en mode siège | `aLeakBeforeASeat…` (« expected:<FR> but was:<EN> ») |
| mode siège pour toute méthode (plus de test sur `drive`) | `onTheBenchOnlyASeatsDriveIsSpared` (« another test than drive is red: null ») |
| mode siège sans `start.txt` | `onTheBenchOnlyASeatsDriveIsSpared` (« a seat without its start file is red: null ») |
| mode siège pour toute règle (jamais rouge) | `aLeakBeforeABenchTest…` et `onTheBenchOnlyASeatsDriveIsSpared` |
| `restore()` retiré en mode rouge | `aLeakBeforeABenchTest…` (« expected:<FR> but was:<EN> ») |
| journal écrasé (`writeText`) au lieu de complété | **aucun : la mutation survit** |
| sans mutation | 4 verts |

`fuse-reports.py` : la lecture des journaux retirée → `test_ultra_bench.py` rouge (« the inherited state put back, listed for its
seat ») ; motif élargi à toutes les lignes « BANC » → rouge (« nothing else ») ; nom du siège remplacé → rouge. Tête : « ultra bench
scripts: OK ». La fusion du vrai tour 5 (sortie dans un dossier jetable) passe et ne change rien à ses incidents.

**4. Restes (aucun bloquant, aucun grave).**

| Gravité | Constat | Suite |
|---|---|---|
| moyen (reste de la 1re relecture) | `verifie-chantier.py` : toujours 5 écarts (les cinq verdicts « banc » sans `garde_banc` dans `verdicts.tsv`) ; ce delta n'y touche pas. | Reporter les cinq noms de garde dans `verdicts.tsv` avec la fusion, avant de clore la séance. |
| mineur | L'ajout au journal (ne pas écraser) n'est gardé par aucun test : la mutation `writeText` survit. Le vrai siège du passage 3 montre que la tête complète bien un journal existant. | Partir d'un journal non vide dans `aLeakBeforeASeat…`. |
| mineur | `BenchPristineRuleTest` laisse `lastTest` sur ses descriptions simulées (« HumanTesterB#drive », « BenchPristineRuleTest#drive ») : une fuite trouvée par le test suivant du fork nommerait un siège qui n'a pas tourné. | Remettre `lastTest` en fin de test, ou le dire dans le message. |
| mineur | `fuse-reports.py` parcourt tous les `journal.txt` du tour, donc aussi ceux des dossiers `rejeu-*` (`tour-1/lotA/rejeu-a-23-…/a`) : leur ligne serait rangée au nom du siège d'origine (« A/a »). Le libellé « lot/siège » n'est pas couvert par le test (lot seul testé). | Sauter les dossiers `rejeu-*` ou les nommer ; un cas `lotA/a` dans `seat_exit`. |
| mineur | `end()` remet `_instance` par `restore()` (dans un `runCatching`) : le dernier accès non silencieux a disparu, un champ renommé par une montée de version passerait sans bruit (réserve déjà notée). | Un test qui vérifie que les champs lus par réflexion existent. |
| observation | Avec `end()` qui remet toute la liste, le détecteur ne voit plus ce qu'un test `HumanTester` salit dans ces statiques ; il garde les classes sans `HumanTester`, le travail tardif, les workers et les compteurs. C'est le but (l'état de départ du test suivant), pas un masquage. | — |

Verdict : 0 bloquant, 0 grave — fusion possible
