# Étape 3, lot 4 « Les pads Live » : rapport de l'auteur (2026-11-10)

Atelier `pads-live`, branche `eq/tour-5-pads-live`, au-dessus de 594be3d (base 4437ccc). Un seul auteur, rien n'est poussé. À relire par un relecteur neuf avant fusion. Aucune ligne de `app/src/main` n'est changée (`git diff --stat 594be3d..HEAD -- app/src/main` est vide) ; `app/screenshots` n'est pas touché.

## En bref

- Sur le banc, un pad Live attend maintenant sa ligne comme sur le téléphone : un coup le prochain temps, une boucle sa grille, une rangée la mesure ; un toucher pendant l'attente l'annule. Le défaut de Benjamin (tel-17, le clap retouché qui n'est pas pris) se reproduit au banc.
- Le banc n'écrit plus lui-même ce que jouent les pads : il écoute les ordres que l'appli envoie au moteur, et ses trois gardes le vérifient. Les lignes de lancement de la vue Live passent aussi par le faux moteur.
- Les restes des deux relectures sont traités : une fuite d'état est rouge sur le test qui la laisse, le répartiteur de Compose n'est plus laissé bloqué, le juge reconnaît une garde `@KnownDefect` retirée comme une paire, le script de la mutation L1 est supprimé.

## Le lot, ligne par ligne

| Ligne | Fait ? | Ce qui a changé | Ce que le test prouve | Preuve |
|---|---|---|---|---|
| A.16 (F3) pads | fait | `BenchClips` : copie Kotlin de la machine d'états de ClipLauncher (Off / Waiting / Playing), pilotée par les commandes reçues : 45 (réglages d'un pad), 46 (toucher, −1 = tout couper), 150 (rangée), installation d'un son (sa longueur gardée à `sampleCreate`). Horloge propre aux pads, comme dans le moteur : elle tourne dès la création, repart à 0 au premier pad quand tout est éteint et le transport arrêté, se recale au pas 0 du transport. `CommandSink.clipStates` + `NativeEngine.clipStatesNow` qui interroge le substitut (module `engine`, sans effet sans substitut). | `BenchPadLiveTest` : `aHitRetouchedWaitsForTheGrid` (miroir de `known_defect_t5_padretrig`, mêmes tempos, sons et instants), `aLoopWaitsForItsGridAndATouchWhileItWaitsCancelsIt`, `aRowWaitsForTheBarLineAndAgainStopsItsLoops`, `aRowAloneStartsAtOnce`, `theTransportsStartPutsThePadsOnItsBeat`, `aPadTouchedDuringTheCountInStartsOnTheClocksBar` | rouge 103f232 / vert 844dbcd |
| A.16 (G2) lignes de lancement | fait | Le faux moteur joue `ArmLaunch` (71) et `CancelLaunch` (72) comme `Engine::launchSlots` : quantum, grâce (la ligne qu'on vient de passer), rangée entière = changement de partie, annulation par un changement de partie armé ou par ■ (sauf « quitter le morceau » quand la grille attend), décalages de toutes les colonnes. `slotStates` et `drainLaunchEvents` répondent comme le moteur natif ; l'appli lance par son propre relevé. `BenchLaunchLines` (qui lançait ce que l'écran montrait en attente) est supprimé. | `BenchPadLiveTest#aLaunchArmedWaitsForItsLineAndIsReported`, `#aLaunchArmedWhileStoppedComesOnTheOne`, `#leavingTheSongKeepsALaunchWaitingAndTheDrumsCountFromTheirLaunch` | rouge f59af32 / vert 6c2f776 |
| A.16 (G2) crochets | fait | Plus de `toggleForTest`, `liveToggledForTest`, `liveStoppedForTest`, `rowForTest` posés par les sièges : les touchers, rangées, « Tout couper » et ■ arrivent au faux moteur comme au téléphone. Les lumières, la ligne de son (« a joué une fois », « joue en boucle », « attend sa ligne ») et « Enregistrer ce que je joue » lisent ce que joue ce moteur. Les trois gardes vérifient les commandes reçues : `BenchTour3Test#padLiveRowsStartAndStopAndStopAllCutsThem`, `BenchAmeneesTest#padLiveLightsStopsAndRecordsAsOnAPhone` (et `#livePadsLightGoOutAndAreHeardAsPadLivesDo`), `BenchTour4Test#liveStopsReachTheirLaunchLineOnTheBench`. | Mutation L2 rejouée (ci-dessous) : les 3 gardes du banc deviennent rouges, comme les 3 preuves hors banc. | bc8bb91 (pas une réparation : pas de paire) |
| A.14 retirée | fait | La ligne d'écran « (banc : les pads partent tout de suite, sans grille) » est retirée de `ScreenText`. `verdicts.tsv` (en-tête), `hors-banc.tsv` (`pads_du_banc`) et AUDIT-ULTRA § 12.3 disent ce qui est vrai depuis A.16. | `BenchPlaceAndPadLiveTest#padLivesScreenTextNoLongerSaysThePadsStartWithoutTheGrid` | rouge 0ac27a1 / vert 948d8ad |
| Relecture « fuite », moyen 1 + mineurs 3, 4, 5 | fait | `StrictTestApplication.afterTest` : un test qui porte `BenchPristineRule` répond de ce qu'il laisse (rouge à son nom, état remis avant). La règle dit elle-même qu'elle a tourné (`ranSinceLastAsked`), au lieu de lire le type déclaré d'un champ. La règle `leavesNothing` de `Tour5AppProofTest` (qui ne remettait pas l'état) disparaît : c'est maintenant le cas de tous. L'attente des sauvegardes de fermeture fait tourner la boucle principale et le dit sur la sortie du test à la borne. Un siège réel (`-Phuman.dir`) n'est jamais rendu rouge (journal du siège suivant, comme avant). | `BenchPristineAfterTest#aBenchTestThatLeavesSomethingIsRedItselfAndPutsItBack`, `#aBenchTestThatLeavesNothingIsGreen`, `BenchPristineAfterLooseTypeTest#aRuleDeclaredAsATestRuleStillAnswersForItsLeaks` | rouge fb50699 / vert ec4ade5 |
| Relecture « fuite », moyen 2 | fait | `clear()` de `Tour5AppProofTest` vide la boucle principale (`idle(100)`) après avoir détruit l'écran. Sonde sur cette branche avant la réparation : après chaque preuve de la classe, le répartiteur de Compose gardait ses deux drapeaux levés et une tâche en file (celle du gestionnaire global des instantanés). | `Tour5AppProofTest#aScreenComposedAndClosedLeavesComposesDispatcherFree` | rouge 451b3f3 / vert a3feb26 |
| Relecture « partage », (c) | fait | `verifie-chantier.py` : un commit « vert: » sans rouge ouvert de son test compte comme une paire s'il retire `@KnownDefect` d'un test gardé dans le même fichier et que son message nomme ce test. Retiré d'un autre test, gardé, ou test supprimé avec : toujours « vert sans rouge ». | `test_ultra_bench.py::known_defect_lifted` | rouge f95bb55 / vert 4f99710 |
| Relecture « partage », (d) | fait | `scripts/bench/mutation-l1.sh` et `docs/audits/ultra/audit-banc/mutation-l1.diff` supprimés : aucun script, test ni CI ne les appelle (seuls l'historique et les rapports les citent). Le journal et la KDoc de `Tour5AppProofTest` renvoient à `git show 711e1b0:scripts/bench/mutation-l1.sh`. | — | 518bdab |

Chaque rouge a été lancé et vu rouge pour la bonne raison avant son vert ; les messages d'échec sont dans les commits « rouge: ».

## Fidélité au vrai moteur : ce qui a été comparé, et comment

Comme la relecture du lot 3, j'ai compilé le vrai cœur sur l'hôte (`cmake -S engine/host-tests`, cible `ps_core`, donc `libps_core.a`) et écrit une sonde C++ hors dépôt (`<scratchpad>/probe/probe.cpp`) qui poste à `ps::Engine` les mêmes commandes que les tests du banc (`SetTempo`, `InstallClip`, `SetClipParam`, `ToggleClip`, `LaunchClipRow`, `SetTransport`, `ArmLaunch`, `CancelLaunch`, `ArmSceneSwap`), rend par blocs d'une milliseconde et imprime chaque changement d'état d'un pad ou d'une colonne avec son instant. Chaque nombre attendu par `BenchPadLiveTest` est celui de la sonde :

| Scénario (sonde) | Vrai moteur | Test du banc |
|---|---|---|
| 90 BPM, clap de 800 ms retouché à 667 ms | attend, joue au temps 2 (1333 ms) | `aHitRetouchedWaitsForTheGrid`, 1er bloc |
| 90 BPM, clap de 400 ms touché toutes les 333 ms | 2e toucher attend, 3e l'annule, 4e joue tout de suite | idem, 2e bloc |
| 140 BPM, retouché à 100 ms | joue à 429 ms | idem, 3e bloc |
| 120 BPM, boucles de 4, 2 et 8 temps | 2 temps : attend 1000 ms, annulée à 600 ms ; 8 temps : joue à 2000 ms | `aLoopWaitsForItsGrid…` |
| rangée lancée à 750 ms, une boucle joue déjà | les trois pads à 2000 ms, le coup s'arrête à 2200 ms ; relancée : ses boucles s'arrêtent | `aRowWaitsForTheBarLine…` |
| rangée seule, rien ne joue | tout de suite | `aRowAloneStartsAtOnce` |
| ▶ à 700 ms, boucle touchée à 1000 ms ; ■ ; coup à 3100 ms | 2700 ms ; coup 3200 → 3400 ms | `theTransportsStartPutsThePadsOnItsBeat` |
| ▶ avec une mesure de décompte, boucle touchée pendant le décompte | 2000 ms, puis recalage au « 1 » ; boucle touchée à 2301 ms : 4001 ms | `aPadTouchedDuringTheCountIn…` |
| arrêt armé au pas 5 (quantum 16), grâce 0,5 à 16,3, annulation, rangée entière, changement de partie armé, ■ | pas 16 ; pas 17 compté de 16 ; rien ; pas 48, 19 lancements, 1 changement de partie, pas local 0 ; annulé ; tout remis à la rangée | `aLaunchArmedWaitsForItsLineAndIsReported` |
| armé à l'arrêt, ▶ avec décompte | lancé au pas 0 (2100 ms) | `aLaunchArmedWhileStoppedComesOnTheOne` |
| « quitter le morceau » pendant qu'un lancement attend | gardé ; batterie au pas 16 : `currentStep` 0, `absoluteStep` 16, `songStep` −1 | `leavingTheSongKeepsALaunch…` |

Un seul écart apparent pendant la sonde, expliqué : dans le premier scénario, ma sonde touchait une milliseconde après le début, donc le retouche tombait juste avant le temps 1 au lieu de juste après ; le test moteur (`padlive_tests.inc`) et le test du banc touchent à l'instant 0, et donnent tous deux « attend jusqu'au temps 2 ».

## Mutation L2 (G2), rejouée

`NativeEngine.toggleClip`, `launchClipRow` et `armLaunch` modifiés pour répondre « vrai » sans rien envoyer (fichier remis ensuite, `git status` propre), puis les six tests en un seul Gradle :

| Test | Résultat sous mutation |
|---|---|
| `BenchTour3Test#padLiveRowsStartAndStopAndStopAllCutsThem` | rouge « the row went to the engine » |
| `BenchAmeneesTest#padLiveLightsStopsAndRecordsAsOnAPhone` | rouge « the touch went to the engine » |
| `BenchTour4Test#liveStopsReachTheirLaunchLineOnTheBench` | rouge « the drums' ■ went to the engine » |
| `Tour3Zone1Test#aLivePadTouchedGoesToTheEngine` | rouge (aucune commande 46) |
| `Tour3AppProofTest#padLiveRowsAndStopAllGoToTheEngine` | rouge (aucune commande 150) |
| `Tour4AppProofTest#liveStopAllAndATracksStopGoToTheEngineAndTheGridFollows` | rouge (liste vide) |

Avant ce lot, les trois gardes du banc restaient vertes sous cette mutation (audit, G2) : elles vérifiaient ce que le banc écrivait lui-même.

## Données mises à jour

- `verdicts.tsv` : en-tête « Pads » réécrit (A.14 puis A.16) ; `T5-PADRETRIG` reçoit sa garde du banc (`BenchPadLiveTest#aHitRetouchedWaitsForTheGrid`) et une note, la ligne reste « appli » « ouvert » (le défaut est dans le moteur). Aucune autre ligne pad ne change de verdict : aucune n'est fermée par ce lot. `marque-corrige.sh` n'a pas été lancé : rien n'est corrigé dans l'appli.
- `sieges/tour-5/hors-banc.tsv` : ligne `pads_du_banc` réécrite. AUDIT-ULTRA § 12.3 : la puce « Pads » réécrite.
- `verifie-chantier.py` : la règle « verdict pad-timing sans preuve hors banc » est gardée telle quelle ; seul le texte entre parenthèses change (le banc est une copie de ClipLauncher, pas le moteur).
- `banc-fiable-journal.md` : les lignes du lot, chacune avec sa paire ; la ligne A.13 renvoie au script supprimé dans l'historique.
- `docs/DECISIONS.md` : six lignes ajoutées à la fin.
- Aucun fait d'essai sur appareil n'est ajouté ou changé ; `retours-benjamin.md` n'est pas touché.

## Commandes lancées, résultats vus

- `python3 -I scripts/bench/test_ultra_bench.py` : « ultra bench scripts: OK ».
- Gradle, toujours par `scripts/bench/gradle-file.sh`, ciblé : `BenchPadLiveTest` 9 verts ; `BenchSongClockTest` 15 ; `BenchEngineTest` 10 ; `BenchTour3Test`, `BenchAmeneesTest`, `BenchTour4Test`, `BenchPlaceAndPadLiveTest` verts (les `drive` des sièges sautés) ; `Tour3AppProofTest` 3, `Tour4AppProofTest` 10, `Tour3Zone1Test` 4, `Tour5AppProofTest` 6 ; `BenchPristineRuleTest` 4, `OrdinaryTestsLeaveNothingTest` 2, `BenchPristineAfterTest` 2, `BenchPristineAfterLooseTypeTest` 1.
- `bash scripts/bench/selftest.sh <scratchpad>/lot-pads-live-final` : **« Selftest vert : 164 verts, 0 rouges, 0 en quarantaine, 41 sautés »** (les 41 sautés sont les `drive` des sièges, dans la liste bornée ; rien n'y est ajouté).
- `python3 -I scripts/bench/verifie-chantier.py --resume <…>/selftest-resume.txt` : **22 écarts** (24 à la base). Les deux en moins sont `0d049be` et `2e6e828` (lot « partage ») : verts qui retirent `@KnownDefect` du test qu'ils nomment, reconnus par la règle (c). Vérifié en passant l'ancien juge (`git show 4437ccc:scripts/bench/verifie-chantier.py`) sur le même arbre et le même résumé : 24, la différence est exactement ces deux lignes. Les 22 restants sont tous d'avant le lot : 10 lignes « banc » sans preuve sans banc (T4-19, T4-16, T4-17, T5-Dc-4, T5-Fb-73, T5-Ac-61, T5-Db-16, T5-Fc-8, T5-Cc-41b, T5-Ab-14), 2 lignes « appli » sans preuve (T4-01, T5-TEL10), 6 paires historiques (verts sans rouge 0b1cbbc, dcc3367, 87b6693 ; rouges sans vert 8f36fa3, df794a9, d503254), 4 lignes cochées du journal sans preuve (4.0 deux fois, 4.2, 4.3).
- Suite complète : voir « Suite complète » plus bas.

## Ce qui n'a pas pu être fait, et les limites

- **Les lumières sont encore écrites par le banc.** Le relevé de l'appli (`pollLiveStates`) s'arrête sans moteur natif, et ce lot ne touche pas `app/src/main`. Le banc les recopie donc à chaque tic, mais depuis l'état de son moteur (plus depuis un crochet). À retirer quand l'appli lira le substitut.
- **`PadLiveController.padStates`** (appli) traite, sans moteur natif, un pad en attente comme un pad qui joue (2) pour préparer une rangée ; le téléphone distingue 1 et 2. Effet possible : le plan d'une rangée dont un pad attend. Non corrigé (appli).
- **Ce qui n'est pas copié de ClipLauncher** : le son lui-même, les niveaux (`clipLevels` reste à zéro), l'attaque, le relâchement, la vélocité, la reprise d'une boucle réajustée au tempo. Seuls les états et les instants où ils changent sont joués, c'est tout ce que l'appli lit.
- **Ce qui n'est pas copié des lignes de lancement** : ce que la colonne apporte (motifs, effets, courbes, boucles audio), sauf la longueur du motif de batterie et le départ de chaque colonne. Les commandes de mise en attente (`SetStaging`) restent ignorées hors batterie.
- **`sampleFrames` du faux moteur** répond toujours 48000 (une seconde) pour tout son ; seul le modèle des pads lit la vraie longueur. Hors du lot, noté.
- **Le pad qui joue seul à l'ouverture de Pad Live** (T5-Ad-43) : sans le crochet, il dépend de `padSamples.native` ; je n'ai pas vérifié s'il part maintenant au banc. Les gardes attendent la ligne quand un autre pad joue, comme un téléphone.
- **Ordre des preuves dans les commits.** Le Gradle qui a vu vertes `BenchTour4Test` et `Tour4AppProofTest`, cité par le vert des lignes de lancement (6c2f776), a tourné avec le changement des crochets (bc8bb91) déjà dans l'arbre. Les deux tests ne passent pas par les pads ; les tests propres à 6c2f776 (`BenchPadLiveTest`) n'utilisent pas `HumanTester`.
- **La ligne cochée (d) du journal** cite `verifie-chantier.py` comme « script » : c'est une suppression de fichiers, pas une réparation ; il n'y a pas de test à faire rougir.
- **Mineur 3 (attente des sauvegardes de fermeture)** : réparé sans test dédié (une sauvegarde qui ne finit jamais ne se fabrique pas simplement) ; la borne est dite sur la sortie du test, pas rendue rouge.
- **Le chiffre du rapport du lot « fuite »** (« SimpleMixTest : 8,6 s avant, 12,7 s après ») n'est pas corrigé dans ce rapport-là (je ne réécris pas un rapport d'un autre lot) ; le correctif du mineur 3 le rend caduc.
- Pas d'émulateur ni de téléphone : rien n'est dit de plus que `retours-benjamin.md`.

## Défauts de l'appli révélés

Aucun nouveau. Le banc reproduit maintenant T5-PADRETRIG (déjà connu, moteur). Le point `padStates` ci-dessus est une différence entre le chemin sans moteur natif et le téléphone, pas un défaut vu sur le téléphone.

## Classes à rejouer à la fusion

`BenchPadLiveTest`, `BenchSongClockTest`, `BenchEngineTest`, `BenchTour3Test`, `BenchAmeneesTest`, `BenchTour4Test`, `BenchTour1Test`, `BenchPlaceAndPadLiveTest`, `BenchPristineAfterTest`, `BenchPristineAfterLooseTypeTest`, `BenchPristineRuleTest`, `OrdinaryTestsLeaveNothingTest`, `Tour3AppProofTest`, `Tour4AppProofTest`, `Tour5AppProofTest`, `Tour5AppProof2Test`, `Tour3Zone1Test`, `ChallengeControllerTest` ; et la suite complète une fois, puisque `StrictTestApplication` change pour tous les tests.

## Suite complète

`PS_STALL_MIN=70 bash scripts/bench/gradle-file.sh :app:testDebugUnitTest`, une fois, sur 175b5fa (le code de ce rapport ; seuls ce rapport et le journal viennent après) : **BUILD SUCCESSFUL en 22 min**, 483 classes, 8131 tests, 0 échec, 0 erreur, 4479 sautés (les captures d'écran Roborazzi, sautées hors `verifyRoborazzi`, et les `drive` des sièges). Aucun test n'a été rendu rouge par la nouvelle vérification après un test du banc (aucun message « leaves to the test after it »), aucune sauvegarde de fermeture n'a atteint la borne.
