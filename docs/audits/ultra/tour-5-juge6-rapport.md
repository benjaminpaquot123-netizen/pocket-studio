# Tour 5, lot « juge6 » : le juge du chantier redescend avant le tour 6 (2026-11-12)

Branche : `eq/tour-5-juge6` (base fx-int 9be3e12d). Décisions : `docs/decisions/tour-5-juge6.md`.
Commande du juge, comme le portail (`fusion-equipes.sh`, `selftest.sh`) :
`python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume.txt`.

## Les écarts, avant et après
- Avant (9be3e12d, résumé de la trace du portail « oreille ») : 22 écarts (23 sans résumé).
  6 anciens (déjà là avant testapk) + 16 nouveaux (lots testapk et oreille : leurs verts ne nomment pas le test du rouge).
- Après (ce lot) : 7 écarts, tous nommés ci-dessous (cible du plan : 6 ; le 7ᵉ, 411b2d51, est un commit qui ne compile pas seul).

| Écart | Classe | Ce qui le ferme |
|---|---|---|
| T5-AUTO-OPTION garde_id CACHE-B19 | ancien | donnée : ligne corrigée, la garde levée par sa correction (garde_id « - », comme T4-01) |
| 038294f5 / 72d642ff | nouveau (testapk) | rejeu `paires-rejouees/72d642ff…` (InAppDriverTest#aTouchUnderAnotherWindowFails et EclairDeuxTest) |
| e2fc7753 / ef2564d5 | nouveau (testapk) | rejeu `paires-rejouees/ef2564d5…` (NewSongOpenTest) |
| a4b8f58f / 43fbc585 | nouveau (testapk, rouge avec stubs) | rejeu `paires-rejouees/43fbc585…` (ToutTesterTest, 8 cas) |
| 5c6322e8 / d88c328b | nouveau (testapk, test renommé « rouge - » → « vert - ») | rejeu avec « test au vert: » (nouvelle règle étroite) |
| b3838180 / 35187750 | nouveau (oreille, rouge avec stubs) | rejeu `paires-rejouees/35187750…` (OreilleTest, 10 cas) |
| 0847b283 / a9f1c699 | nouveau (oreille) | rejeu `paires-rejouees/a9f1c699…` (OreilleTest, 4 cas) |
| 662c80ab (vert sans rouge) | nouveau (testapk) | mutation `rouges-mutes/662c80ab…` (ToutTesterTest) |
| c391ca38 (vert sans rouge) | nouveau (testapk) | mutation `rouges-mutes/c391ca38…` (PremiereInstallationTest) |

## Les 7 écarts qui restent (nommés)
1. 9b8fc4fd « rouge: the essai eclair on a first install fails… » : rouge sans vert. Son test (`theEssaiEclairOnAFirstInstall`) a été remplacé par trois autres dans le vert c391ca38 ; il n'a jamais verdi tel quel. Nouveau (testapk), gardé : rien ne le prouve.
2. 2b34c216 « vert: mode_step re-recorded… » : vert sans rouge (ancien).
3. a2a6ec58 « vert: Prêt à partager… (Korean) » : vert de suite, sans rouge (ancien).
4. 11c48708 « vert: the separated song's card… (exact label) » : vert de suite, sans rouge (ancien).
5. 96a7a702 « rouge: NavScreenshotTest.modeStep… » : rouge sans vert (ancien ; son vert est 2b34c216, nommé autrement).
6. ade1a174 « rouge: sheets scroll lying down: the test measures… » : second rouge du même sujet, le vert cee8fb0e ferme le premier (ancien).
7. 411b2d51 « vert: « Retester les corrections »… » : vert sans rouge. Nouveau (testapk). Son arbre ne compile pas seul (Robot.kt y lit `cpuWorst`, ajouté au commit suivant 662c80ab) : la mutation préparée n'a pas pu tourner (`compileDebugUnitTestKotlin` : « No parameter with name 'cpuWorst' »). Rien à prouver sur un commit cassé ; RetestTest est vert à la tête.

## Ce qui a changé dans le juge (`scripts/bench/verifie-chantier.py`)
- Un test Kotlin entre accents graves est un seul test, son nom entier.
- Un rejeu peut nommer le test que le vert a seulement renommé (« test au vert: Classe#nom » ; le fichier, ancien nom remis, doit être celui du parent).
- `rouges-mutes/<vert>/` : le rouge d'un vert arrivé avec son test, par une mutation qui retire une partie de sa propre correction (lignes ôtées = lignes que le vert a ajoutées, pas plus de lignes ajoutées qu'ôtées, aucun « throw », code seulement, le patch s'applique au vert ; le test, ajouté par le vert, rougit muté et verdit sans).
- La fenêtre des rejeux et des mutations finit à 9be3e12d (avant : 92258b19).
- Tests : `replay_backticks`, `mutated_red`, `replay_window` dans `scripts/bench/test_ultra_bench.py` (rouge 93a4f229, vert 83cfab09) ; chaque garde retirée du juge fait rougir un témoin (mutations du juge vérifiées à la main).

## Pour la relecture
- Les XML des rejeux et des mutations sont la parole de ce lot (worktree jetable, `gradle-file.sh` du commit) : rejouer au moins une paire et une mutation (`git apply mutation.patch` sur le vert, puis le filtre de `rejeu.txt`).
- Aucun code de l'appli touché.

## Chemins (endroits qui lisent ou montrent les mêmes données)
- chemin: Robot › toucher sous une autre fenêtre (paire rejouée 038294f5 / 72d642ff) | test: InAppDriverTest#aTouchUnderAnotherWindowFails
- chemin: Mes projets › nouveau morceau ouvert (paire rejouée e2fc7753 / ef2564d5) | test: NewSongOpenTest#theNewSongOpeningIsNotLoggedAsAFailure
- chemin: Tout tester › écoute seconde par seconde (paire rejouée a4b8f58f / 43fbc585) | test: ToutTesterTest#aSteadySongIsHeardSecondBySecondWithoutAlert
- chemin: Questions à l'oreille › les 8 questions (paire rejouée b3838180 / 35187750) | test: OreilleTest#theEightQuestionsAreAskedInOnePass
- chemin: Premier lancement › premier morceau (mutation de c391ca38) | test: PremiereInstallationTest#aFirstInstallReachesTheFirstSongsCreation

tests: aucun : lot du banc seul (le juge et ses preuves), aucun code de l'appli ni test Gradle changé ; banc : python3 -I scripts/bench/test_ultra_bench.py
