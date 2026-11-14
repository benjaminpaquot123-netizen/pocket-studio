# Tour 5, lot « ci2 » : le seul rouge de la CI 43279184052 (commit 2cfb8a3a)

Branche `claude/benstudio-audit-tour-6-xeiz3b-ci2`, partie de `ccr-80af1a1b-mn0wec` (496dbdc5). Zone : `app/src/test` (le banc). Rien dans `app/src/main` ni dans le moteur. Décisions : `docs/decisions/tour-5-ci2.md`.

| Rouge de la CI | Cause trouvée | Correctif | Preuve |
|---|---|---|---|
| `Zone3JourneysTest#redoAfterUndoInTheModeSimple`, `AssertionError at BenchPristine.kt:158` ; message complet (artefact `unit-test-reports`) : « leaves to the test after it (put back): coroutine workers = DefaultDispatcher-worker-1 RUNNABLE: PackStore.fetchCatalogue:86 < PackStore.fetchIndex:67 < SoundLibraryViewModel$refreshPacks$1$index$1.invokeSuspend:294 » | Test capricieux par le réseau, pas une régression de rejeux6. L'assistant demande à GitHub la liste des packs à son ouverture, sur un fil de fond. La fin du banc (`HumanTester.end`) attend le repos des fils, mais ignore exprès un fil qui attend une socket (`waitsOnTheNetwork`, voulu pour « attends ») : un test court (3,4 s) se terminait pendant que la liste était encore en route, et le détecteur la voyait, selon la vitesse du réseau de la CI | `HumanTester.end` attend en plus, boucle principale en marche, tant que tous les fils occupés de l'appli sont sur le réseau, 30 s au plus ; le travail hors réseau, et ce qui reste après 30 s (cas extrême : délais de lecture répétés, repli sur l'index), restent au détecteur, comme avant | rouge e49deced, vert 7182cf37, puis relecture : `BenchNetworkEndTest` ralentit le réseau exprès (un proxy local retient 10 s la seule connexion du catalogue, github.com, et refuse les autres, dont la vérification de mise à jour des versions preview/stable de la CI) et exige qu'aucun fil de l'appli ne reste après `end()`. Avec `HumanTester.kt` d'avant (496dbdc5) : rouge, `end()` revenu après 103 ms, même pile que la CI (`PackStore.fetchCatalogue:86`) ; avec la correction : vert (18 s) ; `Zone3JourneysTest` (5) et `BenchPristine*` verts |

Pourquoi pas rejeux6 : entre 1f47d889 (CI verte) et 2cfb8a3a, `git diff` ne touche ni `end()`, ni `backgroundQuiet`, ni `waitsOnTheNetwork`, ni l'appli ; `Zone3JourneysTest` touche par pixels (`centerOf`, inchangé hors des étiquettes `vue_`). La classe seule est verte ici (réseau rapide) ; le même rouge se fabrique ici en ralentissant le réseau (proxy local du test).

Non rejoué ici : la suite complète de la CI (34 min). Joué ici avec la première forme de la correction (7182cf37, qui attendait tout fil de l'appli) : la suite du banc `com.pocketstudio.app.human.*` entière, verte (302 tests, 58 sautés, 0 rouges), 9 min 52 s.

tests: com.pocketstudio.app.human.BenchNetworkEndTest com.pocketstudio.app.human.Zone3JourneysTest com.pocketstudio.app.human.BenchPristineAfterTest com.pocketstudio.app.human.BenchPristineRuleTest

## Liste des chemins

- chemin: Accueil › « Fais-moi un morceau » › assistant (liste des packs demandée au réseau), fin du test | test: BenchNetworkEndTest#theEndWaitsForTheAppsNetworkWork
- chemin: Mode simple, Mon morceau › tempo changé › ↶ puis Rétablir | test: Zone3JourneysTest#redoAfterUndoInTheModeSimple
