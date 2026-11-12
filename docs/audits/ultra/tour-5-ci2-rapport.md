# Tour 5, lot « ci2 » : le seul rouge de la CI 43279184052 (commit 2cfb8a3a)

Branche `claude/benstudio-audit-tour-6-xeiz3b-ci2`, partie de `ccr-80af1a1b-mn0wec` (496dbdc5). Zone : `app/src/test` (le banc). Rien dans `app/src/main` ni dans le moteur. Décisions : `docs/decisions/tour-5-ci2.md`.

| Rouge de la CI | Cause trouvée | Correctif | Preuve |
|---|---|---|---|
| `Zone3JourneysTest#redoAfterUndoInTheModeSimple`, `AssertionError at BenchPristine.kt:158` ; message complet (artefact `unit-test-reports`) : « leaves to the test after it (put back): coroutine workers = DefaultDispatcher-worker-1 RUNNABLE: PackStore.fetchCatalogue:86 < PackStore.fetchIndex:67 < SoundLibraryViewModel$refreshPacks$1$index$1.invokeSuspend:294 » | Test capricieux par le réseau, pas une régression de rejeux6. L'assistant demande à GitHub la liste des packs à son ouverture, sur un fil de fond. La fin du banc (`HumanTester.end`) attend le repos des fils, mais ignore exprès un fil qui attend une socket (`waitsOnTheNetwork`, voulu pour « attends ») : un test court (3,4 s) se terminait pendant que la liste était encore en route, et le détecteur la voyait, selon la vitesse du réseau de la CI | `HumanTester.end` attend en plus que les fils de l'appli soient libres (`BenchPristine.appThreads()`), boucle principale en marche, au plus 30 s (les délais du téléchargement : 10 s + 15 s) | rouge e49deced : `BenchNetworkEndTest` ralentit le réseau exprès (un proxy local retient la première connexion 4 s) ; il échoue avec la même pile que la CI (`PackStore.fetchCatalogue:86`). Vert : la correction seule (`HumanTester.kt`) le rend vert ; `Zone3JourneysTest` (5) et `BenchPristine*` verts |

Pourquoi pas rejeux6 : entre 1f47d889 (CI verte) et 2cfb8a3a, `git diff` ne touche ni `end()`, ni `backgroundQuiet`, ni `waitsOnTheNetwork`, ni l'appli ; `Zone3JourneysTest` touche par pixels (`centerOf`, inchangé hors des étiquettes `vue_`). La classe seule est verte ici (réseau rapide) ; le même rouge se fabrique à volonté en ralentissant le réseau.

Non rejoué ici : la suite complète de la CI (34 min) ; la suite du banc `com.pocketstudio.app.human.*` est jouée ici (voir la fin du rapport).

tests: com.pocketstudio.app.human.BenchNetworkEndTest com.pocketstudio.app.human.Zone3JourneysTest com.pocketstudio.app.human.BenchPristineAfterTest com.pocketstudio.app.human.BenchPristineRuleTest

## Liste des chemins

- chemin: Accueil › « Fais-moi un morceau » › assistant (liste des packs demandée au réseau), fin du test | test: BenchNetworkEndTest#theEndWaitsForTheAppsNetworkWork
- chemin: Mode simple, Mon morceau › tempo changé › ↶ puis Rétablir | test: Zone3JourneysTest#redoAfterUndoInTheModeSimple
