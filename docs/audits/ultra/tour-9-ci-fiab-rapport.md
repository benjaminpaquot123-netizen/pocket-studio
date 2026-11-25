# Tour 9, lot ci-fiab : rapport

Sujet (point 4 de la fiche, les points 1 à 3 sont passés au lot ci-solide) : sur la CI 449, `Tour5BloquantsTest#theTutorialsSwingStepShowsTheSwingSetting` tombe rouge (« leaves to the test after it »), puis les deux tests suivants de la classe (`simplePartsPickedAfterTheGuidesListenAreEdited`, `aMelodyTakeLeftForPadLiveIsKept`) tombent rouges sur l'état hérité.

Cause, lue dans l'artefact de la CI 449 (lot 5) : le message nomme un ouvrier de l'appli encore dans `VerifiedDownload.fetch:39 < PackStore.fetchIndex:74 < SoundLibraryViewModel.refreshPacks`. Le test ouvre un tuto, l'appli demande le catalogue des packs à GitHub par le vrai réseau. Quand le réseau du runner traîne, ce travail peut durer 10 + 15 s (catalogue) puis 15 + 30 s (index épinglé), soit plus que les 30 s que la fin du banc attend. L'ouvrier survit au test, et les suivants en héritent. Le même passage montre `TutoScreenTest` rouge avec un ouvrier dans `PackStore.fetchCatalogue:86` : même cause, autre classe.

Correction : `BenchPristine.kt` seulement. Sous la règle `BenchPristineRule`, toute connexion vers un hôte distant est refusée tout de suite (port local fermé) : l'appli voit « hors ligne » et son travail réseau finit en quelques millisecondes, quel que soit le réseau du runner. Épargnés : les hôtes locaux (serveurs des tests), un proxy que le test pose lui-même (`BenchNetworkEndTest`), et un vrai siège du banc. Aucun fichier de l'appli n'est touché, ni `Tour5BloquantsTest.kt` (lot tests-fragiles).

- rouge : c7ecf2d1 (test seul : sous la règle, un hôte distant garde la route de la machine)
- vert : a8477c7f (la coupure dans la règle)

tests: com.pocketstudio.app.human.BenchPristineNetworkTest com.pocketstudio.app.human.BenchNetworkEndTest com.pocketstudio.app.human.BenchPristineRuleTest com.pocketstudio.app.human.Tour5BloquantsTest

chemin: banc, règle d'état propre : un test de banc ne joint aucun hôte distant | test: BenchPristineNetworkTest#aTestUnderTheRuleNeverReachesARemoteHost
chemin: banc, règle d'état propre : les serveurs locaux des tests restent joignables | test: BenchPristineNetworkTest#theTestsOwnLocalServersAreStillReached
chemin: banc, règle d'état propre : le proxy posé par le test lui-même est gardé | test: BenchPristineNetworkTest#aProxySetByTheTestItselfIsKept
chemin: banc, règle d'état propre : un vrai siège garde son réseau | test: BenchPristineNetworkTest#aRealSeatKeepsItsNetwork
chemin: tuto techno en allemand sur tablette, étape Swing (le test tombé sur la CI 449) | test: Tour5BloquantsTest#theTutorialsSwingStepShowsTheSwingSetting

Mesure : cinq passages de suite avec `--rerun` des quatre classes de la ligne « tests: » sur a8477c7f, par `scripts/bench/gradle-file.sh` : 5 verts sur 5, 21 tests à chaque fois, 0 rouge.

Limites, dites franchement :
- Le rouge de la CI 449 n'a pas été reproduit tel quel ici (il faut un réseau qui traîne plus de 30 s) ; la cause vient des piles de l'artefact et du code. Le test rouge prouve le mécanisme (un test de banc partait sur le vrai réseau), pas le hasard du runner.
- `aMelodyTakeLeftForPadLiveIsKept` : son rouge de la CI 449 est une victime de la fuite, réglée ici. Son autre rouge (« 0 notes », vu deux fois sur environ 34 passages chargés par le lot tests-fragiles) n'a pas de cause prouvée ; ce lot n'y touche pas, le lot tests-fragiles y a ajouté de quoi nommer la cause au prochain rouge.
- Les tests sans la règle (`TutoScreenTest` par exemple) partent toujours sur le vrai réseau : hors de la liste de fichiers de ce lot, signalé au chef.

Statut : tenté (aucune CI complète ne l'a encore constaté).

Self-test du banc (`scripts/bench/selftest.sh`) sur le code de a8477c7f (arbre 7257ebf2c2cc73f20eea2324bbf81867d95e3aca) : 393 verts, 0 rouge, 0 en quarantaine, 163 sautés (les sièges `drive`, comme d'habitude). Le tampon n'a pas été écrit : un commit de texte (`docs/` seul) a déplacé la tête pendant le passage, le code testé est le même. Le portail rejoue le sien.
