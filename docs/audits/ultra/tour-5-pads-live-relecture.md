# Relecture du lot « pads-live » (étape 3, lot 4 ; restes des relectures « fuite » et « partage »)
Relu : 761a103ca061dda6f9128d202050ec8c2b038fcc

Relecteur neuf, seul, sans sous-agent. Plage : `git diff 594be3d..761a103` (`git merge-base fx-int HEAD` = 594be3d). Rien n'a été corrigé dans le code : seules des mutations temporaires, chacune remise fichier par fichier (`git checkout -- <chemin>`), et une sonde de banc temporaire (`ZzReviewProbeTest.kt`, supprimée ; copie dans le scratchpad). Arbre propre à la fin (seules les lignes « T engine/third_party/… » et « T separation/third_party/… » restent, normales).

## Constats

| Gravité | Quoi | Preuve (commande, résultat vu) | Correction proposée |
|---|---|---|---|
| moyen | **Règle (c) du juge trop crédule.** Un « vert: » est compté comme paire dès que `@KnownDefect` disparaît, dans ce commit, d'un test dont le nom de méthode figure dans le message. Rien ne vérifie que la garde a existé comme rouge : (1) une garde ajoutée juste avant dans un commit quelconque (« chore: tidy ») puis retirée par un « vert: » passe ; (2) seul le nom de méthode est comparé, pas la classe : retirer la garde de `OtherProofTest#sameName` avec un message qui nomme `UnrelatedTest#sameName` passe ; (3) un commit de fusion « vert: » qui apporte le retrait passe aussi (diff contre le premier parent). | Dépôt adverse construit avec `judge_root` de `test_ultra_bench.py` (`<scratchpad>/relecture-padslive/judge_adv.py`, lancé par `python3 -I`) : les cas 1, 2 et 3 donnent « ACCEPTED as a pair » ; le témoin (vert sans retrait) est bien signalé ; « chantier : 1 écarts ». | Exiger que l'annotation retirée ait été ajoutée par un commit « rouge: » (ou qu'elle existe avant `--since`, ou que son id figure dans `verdicts.tsv` en `garde_id`) ; comparer `Classe#méthode` (la classe du fichier modifié) ; ignorer les commits de fusion (`--no-merges`) ou les signaler. Ajouter ces trois cas à `known_defect_lifted`. |
| mineur | **`idleClock` périmé après la fin du morceau.** `CMD_TOGGLE_CLIP` et `CMD_LAUNCH_CLIP_ROW` lisent `transportSince < 0` sans faire `song.advance()` d'abord (seul `CMD_SET_TRANSPORT` le fait). Si la fin du morceau (arrêt du transport) est passée sans qu'aucune lecture ne l'ait jouée, le faux moteur croit le transport en marche. | Sonde (R4) : morceau joué, `armSongEnd(0)` à 100 ms (arrêt à 2000 ms), boucle touchée à 2300 ms. Vrai moteur : joue à 2301 ms (horloge remise à 0). Banc sans lecture entre deux : **4001 ms** (attend une mesure). Banc avec une lecture (`transportStep()`) avant le toucher : tout de suite, conforme. Masqué sur un siège (le tic lit la position), aucun test n'en dépend aujourd'hui. | `song.advance()` en tête des deux branches (comme pour `CMD_SET_TRANSPORT`), ou `idleClock` calculé depuis `transportNow()`. |
| mineur | **Les gardes des sièges ne regardent pas l'instant des lignes.** `BenchTour3Test#padLiveRowsStartAndStopAndStopAllCutsThem` et `BenchTour4Test#liveStopsReachTheirLaunchLineOnTheBench` vérifient la commande reçue et l'état final, pas quand il arrive. | Mutation A2 (lancement au pas suivant, quantum ignoré) et B (rangée tout de suite, sans mesure) : ces deux gardes restent vertes ; `BenchPadLiveTest` les attrape (3 rouges). | Acceptable tant que `BenchPadLiveTest` reste dans la liste à rejouer ; le dire dans la KDoc des deux gardes. |
| mineur | **`afterATestWithTheRule` ne laisse pas finir les sauvegardes de fermeture** avant `leaks()` (qui compte « ClosingSaves busy » et « TakeVault.app busy »), alors que `afterAnOrdinaryTest` les attend. Un test du banc dont l'écran se ferme dans son `@After` peut donc devenir rouge par course. | Lecture de `BenchPristine.kt` ; non reproduit (suite complète verte chez l'auteur, ciblée verte ici). Même risque qu'avait déjà la règle `leavesNothing` retirée de `Tour5AppProofTest`. | Appeler `closingSaves()` au début de `afterATestWithTheRule`. |
| mineur | **Mineur 3 sans test dédié** ; la pompe de la boucle principale (`idleFor(10 ms)` jusqu'à 15 s) peut faire tourner du code de l'appli après le test (minuteries décalées jusqu'à 15 s d'horloge factice) et `runCatching` avale une exception qui en sortirait. Borné (15 s d'horloge murale), ne boucle pas sans fin sauf tâche qui se reposte à délai nul. | Lecture ; aucune sortie « closing save(s) still running » dans les exécutions ciblées. | Écrire la ligne sur la sortie quand une exception est avalée ; garder la borne. Acceptable en l'état. |
| mineur | **Journal** : la ligne cochée (d) porte « preuve: script scripts/bench/verifie-chantier.py », forme réservée à un chiffre calculé par un script ; la ligne « A.16 (G2) plus de crochet » s'appuie sur la paire de la ligne d'écran A.14 (0ac27a1 / 948d8ad), pas sur une paire des crochets. L'auteur le dit dans son rapport. | `git diff 594be3d..HEAD -- docs/audits/ultra/banc-fiable-journal.md`. | Écrire « preuve: suppression 518bdab (sans test) » et « preuve: mutation L2 (rapport) » plutôt que d'emprunter une forme que le juge accepte. |
| observation | Changement de tempo **pendant** un pas, transport en marche : le vrai séquenceur applique le tempo au pas suivant et l'horloge des pads se recale à chaque temps ; le banc applique le tempo tout de suite et ne recale qu'au pas 0. | Sonde R1b : vrai 2876 ms, banc 2901 ms (25 ms). Écart hérité de `SongClock`, aucun test n'en dépend. | Noter dans la KDoc de `BenchClips` (« recalée au pas 0 seulement »). |
| observation | Le tic de `HumanTester` écrit `engine.tempo` et `engine.beatsPerBar` directement, sans `clips.advance()` : si le tempo du projet changeait sans commande `SetTempo`, l'intervalle écoulé serait recompté au nouveau tempo. | Lecture (`HumanTester.kt`, boucle du tic). Non reproduit : l'appli envoie `SetTempo` à chaque changement. | Passer par `clips.advance()` (ou par `post(CMD_SET_TEMPO…)`) dans le tic. |
| observation | Lumières recopiées par le banc à chaque tic depuis l'état du faux moteur (`lightPads`) : acceptable. C'est ce que ferait `pollLiveStates` (qui s'arrête sans moteur natif), lu au même endroit (`clipStatesNow`) ; la DECISIONS correspondante le dit, avec son retour arrière. | Lecture. | — |
| observation | `Tour5AppProofTest#aScreenComposedAndClosedLeavesComposesDispatcherFree` lit des champs privés de `AndroidUiDispatcher` (une montée de version de Compose le cassera bruyamment) ; `effectRan` n'est jamais vérifié. | Lecture. | Rien d'urgent. |

Aucun bloquant, aucun grave.

## Module `engine` et `app/src/main`

- `git diff 594be3d..761a103 --stat -- engine/` : seulement `CommandSink.kt` (+2) et `NativeEngine.kt` (+5 −2). Aucun fichier de `engine/src/main/cpp` touché : pas d'allocation, de verrou ni d'appel JNI ajoutés au rappel audio.
- `clipStatesNow` : sans substitut, appelle `clipStates` natif comme avant ; avec un substitut, lui demande l'état (défaut de l'interface : tout éteint, comme avant). Sur téléphone (`sink == null`), aucun changement de comportement.
- `git diff --stat 594be3d..761a103 -- app/src/main app/screenshots` : vide.

## Fidélité au vrai moteur

Sonde C++ écrite par le relecteur (`<scratchpad>/relecture-padslive/probe.cpp`), liée à `libps_core.a` reconstruit depuis cet atelier (`cmake --build … --target ps_core`, cœur inchangé depuis 8e03727), blocs de 1 ms. Côté banc, sonde Robolectric temporaire avec les mêmes commandes par `NativeEngine` (le faux moteur posé en `sink`). Scénarios absents du rapport de l'auteur.

| Comportement du faux moteur | Vrai moteur (fichier, fonction) | Scénario | Vrai | Banc | Conforme ? |
|---|---|---|---|---|---|
| Horloge des pads au tempo, attente en temps | `ClipLauncher::render`, `setBeatFrames` (`Engine.cpp` l. 1612) | 120 BPM arrêté ; boucle 4 temps à 0 ; boucle 4 temps touchée à 300 ms ; tempo 60 à 1000 ms | joue à 3001 ms | 3001 ms | oui |
| Idem, transport en marche | `Sequencer` (tempo au pas suivant) + `ClipLauncher::align` à chaque temps (`Engine::onStep`) | ▶ à 0, boucle touchée à 300 ms, tempo 60 à 1100 ms (milieu d'un pas) | 2876 ms | 2901 ms | non (25 ms ; hérité, rien n'en dépend) |
| Retouche d'un coup exactement sur la ligne | `ClipLauncher::toggle` (`ceil(beat/grid − 1e-9)`) | coup d'un temps (5 s) touché à 0, retouché pile au temps k ; 120, 150, 100, 125, 96 BPM ; k = 1, 2, 3, 4, 7 | les 25 cas : joue tout de suite (aucun état 1 vu après le bloc) | les 25 cas : 2 tout de suite et 1 ms après | oui |
| « Tout couper » pendant qu'une ligne de lancement est armée | `ToggleClip(−1)` → `ClipLauncher::stopAll` ; `Engine::launchSlots` | ▶ à 0 ; boucle touchée à 100 ms ; arrêt de batterie armé (q16) à 626 ms ; « Tout couper » à 1000 ms | pad 0 éteint à 1001 ms ; lancement (0, −1, 16) à 2001 ms | pad 0 = 0 à 1001 ms ; lancements `[[0, -1, 16]]` | oui |
| `idleClock` après la fin du morceau | `ArmSceneSwap` kind 2 → `stopTransport` ; `toggle(…, !sequencer_.running())` | morceau ▶, fin armée à 100 ms (2000 ms), boucle touchée à 2300 ms | joue à 2301 ms | sans lecture entre deux : 4001 ms ; avec une lecture : tout de suite | non sans lecture (mineur ci-dessus) ; oui avec |

Relu ligne à ligne en plus : `BenchClips.toggle`, `launchRow`, `stopAll`, `install`, `setParam`, `align`, fin d'un coup (`ceil(frames/speed)` = trames jusqu'à `pos ≥ frames`) contre `ClipLauncher.cpp` ; `armLaunch`, `cancelLaunch`, `launchDue`, `launchSlots`, `slotStates`, `drainLaunches`, `slotsStopped`, `start`, `arm` (keepLaunches) contre `Engine.cpp` (`ArmLaunch` l. 590, `launchSlots` l. 1263, `stopTransport`, `SetTransport`, `ArmSceneSwap` l. 393). Conformes en sens, sauf : recalage de l'horloge à chaque temps (seulement au pas 0 au banc) et saut d'horloge du vrai moteur si la mesure change en cours de lecture (`align`, cycle `beatsPerBar*16`), non joués ; sans effet sur les tests actuels. Un armement pendant le décompte : `stepsNow()` rend −1, donc lancement au pas 0 comme le vrai moteur (vérifié en lecture).

## Mutations

| Mutation (remise ensuite) | Test attendu rouge | Résultat |
|---|---|---|
| A1 : `launchDue` ne déclenche jamais (faux moteur) | `BenchTour4Test#liveStopsReachTheirLaunchLineOnTheBench`, `BenchPadLiveTest#aLaunchArmedWaitsForItsLineAndIsReported` | rouges (« the drums' ■ reached its line expected:<-1> but was:<4> » ; « stopped on step 16 ») ; aussi `aLaunchArmedWhileStoppedComesOnTheOne`, `leavingTheSong…` |
| A2 : lancement au pas suivant, quantum ignoré | gardes des sièges | `BenchTour4Test` **vert** ; `BenchPadLiveTest` rouge (2 tests) |
| B : rangée lancée tout de suite (sans mesure) | `BenchTour3Test#padLiveRows…` | `BenchTour3Test` **vert** ; `BenchPadLiveTest#aRowWaitsForTheBarLine…` rouge |
| 4a : coup retouché joué tout de suite (correctif de 844dbcd défait) | `BenchPadLiveTest#aHitRetouchedWaitsForTheGrid` | rouge (« touched again while it sounds … expected:<1> but was:<2> ») |
| 4c : `ScreenText.kt` remis à 594be3d (ligne « sans grille ») | `BenchPlaceAndPadLiveTest#padLivesScreenTextNoLongerSaysThePadsStartWithoutTheGrid` | rouge |
| 4d : `afterTest` ne juge plus un test portant la règle (ancien comportement) | `BenchPristineAfterTest#aBenchTestThatLeavesSomethingIsRedItselfAndPutsItBack` | rouge (« is red itself: null ») ; `…LooseType…` rouge aussi |
| 4e : règle reconnue par le type déclaré du champ (ancien mineur 4) | `BenchPristineAfterLooseTypeTest#aRuleDeclaredAsATestRuleStillAnswersForItsLeaks` | rouge ; `BenchPristineAfterTest` vert (attendu) |
| 4f : `idle(100)` retiré de `clear()` | `Tour5AppProofTest#aScreenComposedAndClosedLeavesComposesDispatcherFree` | rouge (« queued=1 trampoline=true frame=true ») |
| (c) : `elif not any(names…)` remis en `else:` dans `verifie-chantier.py` | `test_ultra_bench.py::known_defect_lifted` | rouge (« FAIL: a green that takes @KnownDefect off the test it names is a pair ») |

Toutes les paires rouge/vert du lot sont de vraies gardes. Mutations A1, 4a, 4c, 4d, 4f dans un même Gradle (fichiers et tests disjoints), A2, B, 4e dans un second.

## Données et historique

- A.14 retirée (`ScreenText`, en-tête de `verdicts.tsv`, `hors-banc.tsv` `pads_du_banc`, AUDIT-ULTRA § 12.3) : textes exacts, et ils gardent l'exigence d'une preuve hors banc (pas plus fort que la preuve).
- `T5-PADRETRIG` reste « appli » « ouvert », garde du banc ajoutée ; citation tel-17 identique à `retours-benjamin.md` l. 44 ; aucun fait d'appareil ajouté.
- Six lignes ajoutées à `docs/DECISIONS.md`, chacune avec raison et retour arrière ; vraies (la règle (c) est fidèle à la demande de la relecture « partage », mais plus large que nécessaire : moyen ci-dessus).
- Les 19 identifiants cités (103f232, 844dbcd, f59af32, 6c2f776, bc8bb91, 0ac27a1, 948d8ad, fb50699, ec4ade5, 451b3f3, a3feb26, f95bb55, 4f99710, 518bdab, 711e1b0, 0d049be, 2e6e828, 4437ccc, 594be3d) existent et sont ancêtres de la tête (`git merge-base --is-ancestor`) ; `git show 711e1b0:scripts/bench/mutation-l1.sh` répond.
- Hygiène : `git ls-tree -r HEAD | grep -c '^120000'` = 0 ; aucun identifiant de modèle dans les lignes ajoutées ni dans les messages ; ni morceau ni projet de Benjamin.

## Exécutions

- `python3 -I scripts/bench/test_ultra_bench.py` : « ultra bench scripts: OK ».
- `python3 -I scripts/bench/verifie-chantier.py --resume …/lot-pads-live-final/selftest-resume.txt` : « chantier : 22 écarts », les mêmes que le rapport de l'auteur (tous antérieurs au lot).
- Un Gradle ciblé (`scripts/bench/gradle-file.sh :app:testDebugUnitTest`) sur BenchPadLiveTest (9), BenchSongClockTest (15), BenchEngineTest (10), BenchTour3Test, BenchTour4Test, BenchAmeneesTest, BenchPlaceAndPadLiveTest (les `drive` sautés), BenchPristineAfterTest (2), BenchPristineAfterLooseTypeTest (1), BenchPristineRuleTest (4), OrdinaryTestsLeaveNothingTest (2), Tour3AppProofTest (3), Tour4AppProofTest (10), Tour5AppProofTest (6), Tour5AppProof2Test (4), Tour3Zone1Test (4), ChallengeControllerTest (8) et la sonde : BUILD SUCCESSFUL, 0 échec.
- Suite complète non relancée : aucun constat ne l'exigeait.

## En trois lignes

Le faux moteur suit ClipLauncher et les lignes de lancement : quatre scénarios nouveaux sur cinq tombent pile, les deux écarts trouvés (tempo en plein pas, fin de morceau sans lecture) ne touchent aucun test.
Toutes les paires rouge/vert attrapent leur mutation ; les gardes des sièges vérifient les commandes mais pas l'instant des lignes (c'est `BenchPadLiveTest` qui le fait).
Un moyen : la règle (c) du juge accepte un « vert: » sans vraie preuve rouge (garde ajoutée juste avant, autre classe, fusion) ; à resserrer.

Verdict : 0 bloquant, 0 grave — fusion possible
