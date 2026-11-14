# Tour 6 — lot « stable » : rapport

Base : `c384b6939` (tête d'intégration poussée). Aucun fichier de `app/src/main` touché : tout est dans le code de test, le banc et un outil.

## Verdict sur « ci3 »
**ci3 n'est pas en cause.** Chaque job GitHub a sa propre machine (`runs-on: ubuntu-latest` par job) ; les 643 classes tournent dans **un seul processus** (`app/build.gradle.kts` : `maxParallelForks` seulement avec `-Phuman.dir`, pas de `forkEvery`), en 38 à 42 min comme avant ci3 (43 min dans le job unique, run 43402800379). Les trois rouges sont trois courses entre un test et un travail de l'appli en temps réel ; la machine plus ou moins rapide décide qui gagne. Les rapports JUnit (artefact « unit-test-reports » des trois runs) le montrent :

| Run | Rouge | Message complet (rapport XML) | Cause |
|---|---|---|---|
| 43495469474 (constate) | `BenchShareTest#annulerStopsAndClosesAtOnce` puis 15 × `BenchSongClockTest` | « leaves … ClosingSaves busy = 1 », puis « inherited state not pristine … ClosingSaves busy = 1 » ; aucun fil de coroutine occupé ; les 15 tests en 0,32 s | 1 |
| 43498223553 (intégration) | `BenchNetworkEndTest#theEndWaitsForTheAppsNetworkWork` | « after 9800 ms … DefaultDispatcher-worker-1 RUNNABLE: VerifiedDownload.fetch:39 < PackStore.fetchIndex:74 » | 2 |
| 43498458467 (exports2) | `GestureFuzzTest#aRandomFingerNeverCrashesTheApp` | « seed 11: too few gestures made (133) », en 2,1 s (ici 299 en 5,6 s) | 3 |

1. **Sauvegarde de fermeture qui attend la boucle principale.** La règle Compose ferme l'écran *après* le `@After` ; la sauvegarde de fermeture (`ClosingSaves`) attend le verrou de sauvegarde, tenu par une sauvegarde automatique qui reprend sur le fil principal. Le détecteur (`BenchPristine.afterATestWithTheRule`) jugeait sans faire tourner la boucle ; et `restore()` ne remettait pas cette sauvegarde : d'où la cascade de 15 rouges (« une fuite, un rouge » n'était pas tenu). Preuve que ce n'était pas un calcul en cours : le contrôle « coroutine workers » (2 s d'attente) ne nommait aucun fil.
2. **Fin du banc qui échantillonne.** `HumanTester.appNetworkWorkEnded` attendait « tant que tous les fils occupés sont sur le réseau » et sortait au premier échantillon (toutes les 20 ms) hors réseau. Quand la retenue de 10 s lâche, le travail du catalogue passe à ses replis (copie sur disque, puis index épinglé, de nouveau sur le réseau) : échantillonné entre les deux, il passait pour « autre travail ». Ce n'étaient pas deux minuteries voisines (plafond 30 s, retenue 10 s) : la fin sortait 200 ms avant la fin du travail, au moment du passage d'un appel à l'autre.
3. **Le doigt au hasard ne regardait pas l'écran d'ouverture.** Après un « Retour » qui ferme l'appli, le test la rouvre ; l'ouverture attend la sauvegarde de l'écran fermé (« Ouverture du projet… », rien à toucher). La boucle tirait ses gestes à son propre rythme : sur un disque lent, une partie tombait sur cet écran (« nothing to touch »). Mesuré ici avec un disque lent simulé : 108 gestes tirés sur l'écran d'ouverture.

Hypothèse du chef (« du travail de l'appli laissé vivant par une classe et hérité par la suivante ») : vraie pour 1 (la sauvegarde héritée par BenchSongClockTest) ; pour 2, c'est la classe elle-même qui teste ce cas ; pour 3, la course est dans le test (son propre écran rouvert), pas un héritage.

## Paires rouge → vert
- `rouge/vert: the detector lets a closing save waiting for the main thread end before it judges` — `BenchPristineAfterTest` (deux tests neufs : la sauvegarde qui attend la boucle n'est pas une fuite ; `restore()` la laisse finir). Rouge vu : « ClosingSaves busy = 1 », et la cascade sur le test suivant.
- `rouge/vert: the bench's end waits for a network job through its steps off the network` — `BenchNetworkEndTest#theEndWaitsThroughAStepOffTheNetworkBetweenTwoCalls` (la copie du catalogue est un tube nommé écrit 3 s après la retenue). Rouge vu : « after 9804 ms … PackStore.fetchIndex:71 », la même signature que la CI (9 800 ms).
- `rouge/vert: the random finger waits for the project to open, however slow the disk` — `GestureFuzzTest#aSlowDiskDoesNotEatTheGestures` (graine 11, chaque sauvegarde de fermeture 8 s) et une nouvelle vérification dans tous les doigts : aucun geste tiré pendant « Ouverture du projet… ». Rouge vu : 108 gestes. Vert : la graine 11 sur disque lent rejoue exactement la même partie que sur disque rapide (300/300, 21 fenêtres de texte, 1 réouverture).
- `rouge/vert: an ordinary test that leaves the app's work running is red itself` — `OrdinaryTestsLeaveNothingTest#c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself` (demande b du chef).

Hors paire : `WeekInTheLifeTest` (trouvé par la suite mélangée, graine 1 : bloqué 20 min dans `runBlocking { vm.flushNow() }` sur le fil principal, aucun fil de l'appli occupé : une sauvegarde automatique tenait le verrou en attendant ce fil). Pas de rouge à la demande (dépend du moment de la sauvegarde automatique) : la preuve est la pile (jstack) relevée pendant le blocage. Corrigé dans ce seul fichier ; **24 autres** `runBlocking { …flushNow() }` sur le fil principal dans `app/src/test` portent le même risque (liste : `grep -rn "runBlocking {[^}]*flushNow() }" app/src/test`) — à traiter par un lot qui les possède, avec une aide commune.

## Demandes du chef
- (a) Fin du banc sur un état : le travail vu sur le réseau est attendu jusqu'à sa fin (même fil, même coroutine), plafond 30 s, loin des 10 s de retenue + 3 s de pas hors réseau. Je n'attends pas *tout* travail de l'appli dans `end()` : la revue de ci2 a voulu que le travail hors réseau reste nommé par le détecteur (un calcul laissé tourner doit être rouge, pas attendu).
- (b) Contrôle pour toutes les classes : `StrictTestApplication.afterTest` est l'écouteur unique de tous les tests Robolectric (déclaré une fois dans `robolectric.properties`) ; après un test ordinaire, l'état est remis puis le travail de l'appli encore sur les fils de coroutines a 10 s pour finir, sinon le test est rouge à son nom. Les tests JVM purs (sans Robolectric) n'y passent pas.
- (c) Plus de paire de minuteries voisines dans ces tests : la retenue (10 s) et le pas hors réseau (3 s) sont loin du plafond (30 s) ; le doigt attend l'écran d'ouverture sur son état (plafond 60 s, au-delà de l'attente de 10 s de l'appli).
- (d) `scripts/bench/ci-rouge.sh <run>` ou `--dossier <dir>` : message, rang de la classe, trois classes d'avant, durée ; testé par `scripts/bench/test_stable.py` (branché dans `pre-vol.sh` et `selftest.sh`).
- (e) Proposition, non faite : un processus par famille (banc `human`, tuto, fuzz `audit3`, le reste) avec `forkEvery` ou quatre tâches `Test`. Gain : un héritage ne traverse plus les familles, un rouge se lit dans sa famille, et deux familles peuvent tourner côte à côte (4 cœurs sur le runner) : la suite passerait peut-être de 40 à 25 min. Coût : chaque processus recharge Robolectric (≈ 30 à 60 s et 1 à 2 Go chacun ; à 2 Go de tas, deux côte à côte tiennent sur 16 Go) ; et on perd une partie de ce qui a trouvé ces fuites (des voisins différents). Je recommande plutôt de garder un processus et de passer la suite mélangée (`-Pshuffle`) une nuit sur deux.
- (f) Ordre mélangé : `-Pshuffle=<graine>` (ce lot) fait tourner toute la suite dans un seul processus, dans un ordre tiré de la graine (`ShuffledOrderTest`, une suite JUnit de toutes les classes ; la propriété limite le run à elle). Les filtres `--tests` dans un ordre différent n'auraient rien changé : Gradle suit l'ordre de ses fichiers, pas celui des filtres.

## Suite : le rouge de la CI 43531021739 (branche `eq/tour-6-stable-suite`)
Seul rouge sur 8 980 tests : `LibraryRound2Zone1Test#aSwipeAlongTheFamiliesScrollsWithoutChoosingOne`, « UncaughtExceptionsBeforeTest » ; l'exception : `UnsatisfiedLinkError NativeEngine.exportCreate` < `OfflineExport.<init>:17` < `RhythmViewModel$estimateLiveDrive$1$job$2:11150`.

- **Coupable : `WeekInTheLifeTest`** (rang 241, quatre classes avant). Il crée trois `RhythmViewModel` sans jamais les fermer ; le dernier naît avec le moteur du banc (`BenchEngine`) et lance l'estimation du gain du direct. Le `@After` lâche le moteur ; l'estimation, posée sur la boucle principale (`Dispatchers.Main`), repart dans une classe suivante, quand la règle Compose fait tourner la boucle, et tombe sur la bibliothèque native absente. Le premier test qui ouvre un `runTest` (la règle Compose de LibraryRound2Zone1Test) hérite de l'exception.
- **Pourquoi le contrôle ne le nommait pas** : `BenchPristine.appThreads()` ne regarde que les fils `DefaultDispatcher-worker` ; une coroutine qui attend sur la boucle principale n'en occupe aucun. Et une exception non attrapée attendait le prochain `runTest`, chez un voisin.
- **Ce que le contrôle attrape maintenant** (tout test Robolectric ordinaire) : (1) les coroutines de notre code dans la file de la boucle principale (envoi ou `delay`), lues message par message et nommées par leurs étapes suspendues, attendues 10 s boucle tournante, puis le test est rouge à son nom et elles sont annulées (une fuite, un rouge) ; (2) toute exception non attrapée d'une coroutine, rouge du test pendant lequel elle part (aussi pour les tests du banc ; un siège réel l'écrit seulement).
- **Corrections côté test** (`TestScreens` : les ViewModels ouverts comme des écrans, fermés à la fin, leur travail attendu avant de lâcher le moteur) : `WeekInTheLifeTest`, puis les six classes que le contrôle a nommées dans un passage ciblé des 59 classes qui créent un `RhythmViewModel` avec un moteur de remplacement : LiveVmTest (2, `watchPerformance` sur la boucle), LiveCellJourneyTest et LiveMidiJourneyTest (`watchLaunches`), VoiceInWholeSongTest, TakeOnTheEngineClockTest (2), SixteenBarLoopTest (exceptions natives, moteur lâché trop tôt). Aucun fichier de `app/src/main` touché.
- **Revue** : M7 (`BenchNetworkEndTest` attend l'écrivain, borné, avant de lire `isAlive`) ; M8 (`ci-rouge.sh` sans `exec` : le dossier téléchargé est supprimé, code de sortie gardé ; test ajouté).

Preuves (passages ciblés, `scripts/bench/gradle-file.sh`) :
| Passage | Tête | Résultat |
|---|---|---|
| Ordinary + WeekInTheLife + DownloadAll + LibraryChoices + LibraryRound2Zone1 | rouge (742cf41ab) | 17 tests, 2 rouges : d_ et e_ « red itself, by name: null » (LibraryRound2Zone1Test vert : la panne ne se rejoue pas sur cette courte suite) |
| même liste | vert du contrôle (96d27b63c) | 1 rouge : `WeekInTheLifeTest.aWeekWithTheApp` « uncaught exception(s)… UnsatisfiedLinkError exportCreate @ OfflineExport.<init>:17 < RhythmViewModel$estimateLiveDrive$1$job$2:11150 » : la signature de la CI, au nom du coupable |
| même liste | WeekInTheLife corrigé | 17/17 verts |
| 59 classes à ViewModel + moteur, + BenchNetworkEnd, BenchPristineAfter | avant les six corrections | 304 tests, 8 rouges dans 6 classes, chacune à son nom |
| même liste + Ordinary + LibraryRound2Zone1 | 817cfe7e4 | 311 tests, 0 rouge, 5 ignorés |
| les 60 autres classes qui créent un ViewModel | 817cfe7e4 | 0 rouge |

Reste ouvert : les ~525 autres classes n'ont pas été passées avec le nouveau contrôle (la grande vérification revient au chef) ; une classe qui laisse une coroutine sur la boucle principale y sera rouge à son nom.

## Suite 2 : rouges de la CI 43542434448 et des suites mélangées (graines 4 et 5)
Outil : `-Pshuffle=list:<classes>` rejoue des classes dans l'ordre donné (fenêtre d'un rouge : ses voisins, puis les 25 ou 60 classes d'avant).

| Rouge | Cause | À qui | Correction | Preuve |
|---|---|---|---|---|
| A `HumanRemixJourneyTest#aSongsVoiceGoesIntoTheProjectWithTouchesOnly` (CI 311, graines 4 et 5) | la nouvelle fenêtre d'avis de coquille2 | hérité de l'intégration, corrigé par coquille3 | aucune ici | vert sur ce lot avant la fusion de l'intégration (CI 308, graines 1 à 3) |
| D `Tour6TutoLineGoTest#aLinePointingAwaySaysATouchTakesYouThere` (graine 5, rang 553) | repères du tuto (`TutoHints`, `focus=5`) laissés par une classe passée avant ; `restore()` ne remettait que `active` | le banc (contrôle incomplet) | `rouge/vert: the tutorial's hints left by a class are put back…` (`OrdinaryTestsLeaveNothingTest` a_/b_) | 3 voisins : vert ; 60 classes d'avant : rouge 2 fois (« hints=5/5 » relevé) ; après : vert |
| D' `HumanLucasBugsTest#insererApresInsertsThePartPicked` (rouge dès la première version de D) | (1) « Insérer après » touché coupé au bord (menu plus haut que l'écran) ; (2) le compteur `TutoHints.ticked` remis de 2 à 0 après `NavTabletScreenshotTest` : la fenêtre « Insérer après » ne s'ouvre plus (mécanisme exact dans l'appli non établi) | le test (1), la première version de D (2) | `rouge/vert: « Insérer après » is reached whole…` ; `vert: the tutorial's change counter is kept…` (`ticked` n'est plus remis : seuls ses changements comptent) | (1) rouge seul (« touché coupé »), vert après ; (2) bissection 53 → 1 classe : paire NavTablet → Lucas rouge 3 fois, verte avec l'ancien `restore()` et en gardant `ticked` |
| B `BenchSeparationWaitTest#twoWaitsOfThirtySecondsGiveTheSeparatedSong` (graine 4, rang 26) | « performMeasureAndLayout called during measure layout » : l'état de la séparation écrit sur un fil de travail réveille, sur ce fil, la composition (la règle Compose 1.12 compose avec un `UnconfinedTestDispatcher`, `useStandardTestDispatcherForComposition = false`), pendant que le fil principal mesure | course du cadre de test, pas un héritage | aucune (pas de rouge rejoué) | 25 classes d'avant + elle : vert ; pas dans les graines 1 à 3 et 5 |
| C `Coquille2SeatTest#e2_noSongChangeSaidWhenOnlyTheFilesVolumeIsChosen` (graine 4, rang 567) | non établie : le test dure 6,3 s au lieu de 9 à 11, l'export n'a jamais été vu en cours (« Partager » actif, aucune fenêtre « C'est prêt ») ; l'attente du test est « tant que l'export tourne », vide si l'export n'a pas commencé | ? | aucune (pas de rouge rejoué) | voisins, 60 classes d'avant, seul sous 4 boucles CPU : vert |

Ouvert : B se règlerait par la règle Compose « v2 » (`StandardTestDispatcher`) pour le banc, gros changement à mesurer ; C demande un rouge rejoué (proposition : attendre le menu de partage ou « C'est prêt » sur l'état, et dire l'état de l'export si rien ne vient). Les autres classes qui comptaient sans le savoir sur des repères du tuto sales se verront dans la prochaine suite complète.

## Preuves
PREUVES_ICI

## Ce que je n'ai pas pu prouver
- Que la sauvegarde de BenchShareTest attendait bien le verrou tenu par une sauvegarde automatique (et non un disque lent) : l'artefact ne garde pas sa pile. Les deux cas sont couverts par la même correction (on la laisse finir, boucle tournante, 15 s au plus).
- Le doigt au hasard varie encore un peu d'un passage à l'autre sur disque rapide (« nothing to touch » 0 à 35 sur la graine 11 avant la correction) ; après, la graine 11 rejoue la même partie sur disque lent et rapide.

tests: com.pocketstudio.app.human.BenchPristineAfterTest com.pocketstudio.app.human.BenchNetworkEndTest com.pocketstudio.app.human.BenchShareTest com.pocketstudio.app.human.BenchSongClockTest com.pocketstudio.app.audit3.GestureFuzzTest com.pocketstudio.app.human.OrdinaryTestsLeaveNothingTest com.pocketstudio.app.journey.WeekInTheLifeTest

chemin: Partager › Annuler, puis le test suivant du banc | test: BenchPristineAfterTest#aClosingSaveWaitingForTheMainThreadIsLetEndNotNamed
chemin: Partager › Annuler, puis le test suivant du banc | test: BenchPristineAfterTest#putBackEndsAClosingSaveWaitingForTheMainThread
chemin: Partager › Annuler | test: BenchShareTest#annulerStopsAndClosesAtOnce
chemin: Assistant › catalogue des packs retenu, fin du banc | test: BenchNetworkEndTest#theEndWaitsForTheAppsNetworkWork
chemin: Assistant › catalogue des packs retenu puis repli hors réseau, fin du banc | test: BenchNetworkEndTest#theEndWaitsThroughAStepOffTheNetworkBetweenTwoCalls
chemin: Toute l'appli au doigt › Retour › Ouverture du projet… | test: GestureFuzzTest#aRandomFingerNeverCrashesTheApp
chemin: Toute l'appli au doigt sur disque lent › Ouverture du projet… | test: GestureFuzzTest#aSlowDiskDoesNotEatTheGestures
chemin: Test ordinaire qui laisse du travail de l'appli | test: OrdinaryTestsLeaveNothingTest#c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself
