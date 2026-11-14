# Tour 5, lot « juge7 » : les 5 écarts du lot rejeux6 (2026-11-12)

Branche : `eq/tour-5-juge7` (base fx-int 1585bb46). Décisions : `docs/decisions/tour-5-juge7.md`.
Commande du juge, comme le portail : `python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume.txt`.

tests: aucun : lot du banc seul (le juge et ses preuves), aucun code de l'appli ni test Gradle changé ; banc : python3 -I scripts/bench/test_ultra_bench.py

## Les écarts, avant et après
- Avant (base 1585bb46, sortie du juge dans `docs/audits/ultra/sieges/tour-6/lotA/selftest-chantier.txt`) : 12 écarts de paires, 7 acceptés et 5 nouveaux du lot rejeux6.
- Après : 8, les 7 acceptés (voir le rapport juge6) et bc0e6f27, nommé ci-dessous.

| Écart | Ce qui le ferme |
|---|---|
| d56efddf (rouge) / 37e2c8ed (vert) | rejeu `paires-rejouees/37e2c8ed…` : NewReplaysAimByTag (test_socle.py), 3 tests rouges sur le rouge, verts sur le vert |
| a95d9667 (vert sans rouge) | mutation `rouges-mutes/a95d9667…` : ôter « commande[int(k)] = v » de rejeu-etiquettes.py ; NewReplaysAimByTag#test_a_command_can_stand_for_an_untagged_button rougit |
| 2e2c5f3d (vert sans rouge) | mutation `rouges-mutes/2e2c5f3d…` : ôter la ligne du vert dans SeatOptions.nearestReachable (code du banc, sans @Test) ; BenchSeatOptionsTest#aTagOutOfTheThumbsReachIsTouchedInsideTheReach rougit |
| bc0e6f27 (vert sans rouge) | reste nommé : voir plus bas |

## L'écart qui reste (nommé)
bc0e6f27 « vert: the bench's script tests ignore PS_REJEUX_DIR » : le vert ne change que `test_ultra_bench.py` (3 lignes : `os.environ.pop("PS_REJEUX_DIR")`), sans rouge avant lui. Ni rejeu (pas de rouge), ni mutation (une mutation ne touche jamais un test), ni rouge constaté (le vert touche le fichier du test) : le fermer demanderait d'assouplir le juge. Gardé.

## Ce qui a changé dans le juge (`scripts/bench/verifie-chantier.py`)
- `test_in` lit aussi les tests du banc (`py_test_in`) : une méthode `test…` écrite dans `class <Classe>(…)` d'un `scripts/bench/test_*.py`, sans `@unittest.skip` ni `expectedFailure` ; une aide, un test d'une autre classe ou hors de `scripts/bench/` ne compte pas. `git grep` sans résultat ne fait plus abandonner la recherche.
- `mutated_red` : un fichier Kotlin du dossier du banc (`…/app/human/`) sans aucun test (ni `@Test` ni `@org.junit.Test`) compte comme du code ; un fichier qui contient un test, Kotlin ou Python, ou une aide des tests de l'appli hors de ce dossier, n'est jamais muté (relecture G1, M1 : rouge et vert de `mutated_bench_helper`).
- `red_seen` refuse aussi un vert qui touche `scripts/bench/test_*.py`.
- `REPLAYS_UNTIL` : 1585bb46 (base de ce lot).
- Tests (`scripts/bench/test_ultra_bench.py`) : `replay_python`, `mutated_bench_helper`, `replay_window` (rouge d36c60ff, vert 00283ac0) ; `red_seen_bench_test` (rouge 81d5fbc4, vert f8b513fd).

## Pour la relecture
- Les XML des preuves sont la parole de ce lot (worktrees jetables). Les tests Python : `python3 -I junit.py <worktree> <xml> test_socle <Classe>.<test>` (`junit.py` est copié dans chaque dossier de preuve). Le test Kotlin : `scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '…BenchSeatOptionsTest.aTagOutOfTheThumbsReachIsTouchedInsideTheReach'` dans un worktree du vert, avec puis sans `git apply mutation.patch`.
- Aucun code de l'appli touché.

## Chemins
- chemin: banc › toucher par étiquette au pouce (mutation de 2e2c5f3d) | test: BenchSeatOptionsTest#aTagOutOfTheThumbsReachIsTouchedInsideTheReach
