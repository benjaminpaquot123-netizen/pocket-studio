# Tour 9, lot « ci-outils » : rapport

Branche : eq/tour-9-ci-outils, partie de l'intégration ccr-80af1a1b-mn0wec (tête d87dea73).
Défauts visés : deux défauts du banc vus par le chef (ci-etat.sh, ci-rouge.sh). Scripts seulement, l'appli n'est pas touchée. Tous deux « tentés ».

tests: aucun : lot des scripts du banc seuls (ci-etat.sh, ci-rouge.sh), aucun test Gradle changé ; preuve : `python3 -I scripts/bench/test_stable.py` (13 tests, classes CiEtatPlusieursRunsTest et CiRougeSansGhTest)
preuve: ci-etat, deux runs d'un même commit (workflow_dispatch vert, push rouge) | test: CiEtatPlusieursRunsTest#test_each_run_on_its_own_line_and_a_red_verdict
preuve: ci-etat, run rouge lu avant le vert | test: CiEtatPlusieursRunsTest#test_a_later_green_run_does_not_hide_an_earlier_red_one
preuve: ci-etat, un seul run vert | test: CiEtatPlusieursRunsTest#test_only_green_runs_give_a_green_verdict
preuve: ci-etat, run en cours | test: CiEtatPlusieursRunsTest#test_a_running_run_is_neither_green_nor_null
preuve: ci-rouge <run id> sans gh | test: CiRougeSansGhTest#test_without_gh_the_artifact_is_read_through_the_rest_api
preuve: ci-rouge, pas d'artefact vivant (garde) | test: CiRougeSansGhTest#test_no_live_report_artifact_is_refused
preuve: ci-rouge, zip qui sort de son dossier (garde) | test: CiRougeSansGhTest#test_a_zip_reaching_out_of_its_folder_is_refused
Les lignes « preuve » nomment des tests Python du banc (`scripts/bench/test_stable.py`), que `rapport-zone.py` ne sait pas lire ; la ligne « chemin » ci-dessous cite un test du banc de l'appli pour la forme, comme tour-6-ci3.
chemin: banc › outils de lecture de la CI (aucun écran) | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace

## Ce qui change
- Défaut 1 (tenté) : `ci-etat.sh <sha>` écrit une ligne par run (« run <id>, événement, branche, workflow, état », jamais « null »), puis une ligne « verdict » rouge dès qu'un run du commit est rouge (« rouge (1 run rouge sur 2 : 9100 push) »). `ci_build` et `ci_emulateur` prennent le pire des runs au lieu du dernier lu ; un run annulé ne compte pas. Rouge vu avant (0f555938) : 4 tests rouges.
- Défaut 2 (tenté) : `ci-rouge.sh <run id>` sans `gh` connecté passe par curl sur l'API REST (le proxy donne l'accès) : liste des artefacts du run, l'artefact « unit-test-reports » non expiré, zip téléchargé dans un dossier neuf, ouvert par `python3 -I`, un zip qui sort de son dossier est refusé. Rouge vu avant (74e0841e).
- Réponses d'API enregistrées (sans jeton) : `scripts/bench/tests/ci-outils/`.
- Essayé en vrai (réseau) : `ci-etat.sh origin/ccr-80af1a1b-mn0wec` donne 3 runs et « verdict rouge (1 run rouge sur 3 : 45574494848 pull_request) » ; `ci-rouge.sh 45574494848` sans gh lit 880 classes et nomme le rouge `ScenariosTest#eachEffectTypeGoesOnAStripIsSavedSoundsAndIsTakenOff` (lot 5).

## Ce qui n'est pas vert
- Rien dans ce lot. Hors de mes fichiers : `pre-vol.sh` lit `ci_build` avec une espace (`sed 's/^ci_build //'`) alors que la ligne a une tabulation : il ne voit jamais l'état de la CI (voir Demande).

## Demandes (hors de mes fichiers)
Demande : `scripts/bench/pre-vol.sh:113`, lire `ci_build` avec la tabulation (ou lire la ligne « verdict » neuve) : à confier au lot qui tient pre-vol.sh.
