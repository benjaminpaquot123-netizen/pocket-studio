# Tour 9, lot « ci-outils » : relecture neuve
Relu : cce062a7

Diff lu : d87dea73..ab37187d (scripts/bench/ci-etat.sh, scripts/bench/ci-rouge.sh, scripts/bench/test_stable.py, scripts/bench/tests/ci-outils/{artifacts-9100,artifacts-empty,jobs-9100,jobs-9101,runs-two-events}.json, docs/audits/ultra/tour-9-ci-outils-rapport.md, docs/decisions/tour-9-ci-outils.md) ; puis ab37187d..cce062a7 (scripts/bench/ci-etat.sh, scripts/bench/test_stable.py)

rouge constaté : 0f555938 : ci-etat.sh de d87dea73 remis en place, `python3 -I scripts/bench/test_stable.py` (tests de ab37187d) : 4 échecs CiEtatPlusieursRunsTest (each_run_on_its_own_line_and_a_red_verdict, a_later_green_run_does_not_hide_an_earlier_red_one, only_green_runs_give_a_green_verdict, a_running_run_is_neither_green_nor_null).
rouge constaté : 74e0841e : ci-rouge.sh de d87dea73 remis en place avec le même ci-etat ancien : 5 échecs, dont CiRougeSansGhTest#test_without_gh_the_artifact_is_read_through_the_rest_api (l'ancien script sort en 2 sans gh). Les deux gardes (pas d'artefact vivant, zip qui sort) passent déjà sur l'ancien code (il sort toujours en 2) : voir constat 1. Fichiers rendus par `git checkout ab37187d -- scripts/bench/ci-etat.sh scripts/bench/ci-rouge.sh`, `git status` propre.

## Constats
1. **moyen** : `test_a_zip_reaching_out_of_its_folder_is_refused` ne prouve pas la garde. Mutation faite : condition de refus remplacée par `if False:` dans ci-rouge.sh, les 12 tests restent verts. Raison : `zipfile.extractall` de Python retire déjà les `..` (le membre atterrit en `a/evil-TEST-x.xml`), puis le script sort en 2 parce que « evil-TEST-x.xml » ne colle pas à `TEST-*.xml`. Aucun risque réel (Python protège déjà, il ne crée pas de liens symboliques, un nom absolu est refusé), mais le test annoncé comme garde est creux. Il faudrait vérifier le message « reaches out of its folder » sur stderr, ou un membre au nom `../TEST-x.xml`.
2. **mineur** : « jamais null » n'est pas tenu pour la branche : un run dont `head_branch` vaut null (chemin gh : jq écrit « null », chemin curl : `s(None)` écrit « null ») s'affiche « null » ; `${branch:-?}` ne l'attrape pas. Même chose pour `event` en théorie. Corriger par `[ "$branch" = null ] && branch=""`.
3. **mineur** : si tous les runs d'un commit sont annulés, la ligne dit « verdict absent (2 runs) », peu lisible ; ci_build / ci_emulateur restent « absent » (cohérent avec la décision).
4. **mineur** : le chemin curl de ci-rouge.sh n'envoie aucun jeton : GitHub exige une authentification pour télécharger un artefact, même sur dépôt public. Ça ne marche que derrière le proxy qui ajoute l'accès (le commentaire le dit) ; ailleurs, message clair et sortie 2, pas de faux vert. Pas de plafond de taille au dézippage (zip bombe) : risque faible, source = notre propre CI.
5. **mineur (hors lot, déjà signalé)** : `scripts/bench/pre-vol.sh:113` lit `ci_build ` avec une espace alors que la ligne a une tabulation (déjà vrai à d87dea73) : le pré-vol ne voit jamais l'état de la CI. La demande du rapport est juste.

Vérifié sans défaut : guillemets bash, `set -euo pipefail` (les listes `[ … ] && …` en fin de bloc `if` ne font pas sortir, prouvé par le test du run en cours), le piège EXIT réécrit couvre `$dir` et `$zip` sur toutes les sorties, run id non numérique refusé avant curl, artefact expiré écarté (le test vérifie que 5551 n'est pas demandé), redirection `-L` (curl ne repasse pas l'en-tête d'accès à un autre hôte), chemin gh inchangé, anciens lecteurs : `tableau-de-bord.py` (lit hors-banc.tsv, `--ecris` écrit toujours les deux mêmes clés), `test_ultra_bench.py` `ci_state_reads_every_job` et `ci_state_without_gh` (fausse sortie gh à 4 colonnes : event et branche vides donnent « ? ») toujours verts, rejoués à part. Aucun jeton ni secret dans les fixtures ni le rapport (grep ghp_/github_pat/token/secret/authorization : rien).

## Réponses à la première relecture
1. Garde du zip (moyen) : **corrigé**. Le test vérifie maintenant « reaches out of its folder » sur stderr. Mutation refaite sur cce062a7 (refus remplacé par `if False:`) : `test_a_zip_reaching_out_of_its_folder_is_refused` passe rouge (1 échec) ; fichier rendu par `git checkout cce062a7 -- scripts/bench/ci-rouge.sh`, `git status` propre hors ce fichier.
2. Branche ou événement « null » (mineur) : **corrigé**. Ils deviennent « ? » ; le test neuf `test_a_null_branch_and_only_cancelled_runs_are_said_plainly` le vérifie.
3. Runs tous annulés (mineur) : **corrigé**. Ça donne « verdict annulé (N runs, tous annulés) », et « absent (aucun run) » s'il n'y a aucun run ; même test. Reste un détail : avec un seul run annulé, la ligne dit « 1 runs », sans gravité.
4. Curl sans jeton et pas de plafond de taille au dézippage (mineur) : **non corrigé**, accepté tel quel (on passe par le proxy, et la source est notre propre CI).
5. `pre-vol.sh:113` (mineur, hors lot) : **non corrigé**, reste une demande pour le lot qui tient pre-vol.sh.

## Relancé sur cce062a7
- `python3 -I scripts/bench/test_stable.py` : 13 tests, OK.
- `ci_state_reads_every_job` et `ci_state_without_gh` de `scripts/bench/test_ultra_bench.py`, lancés seuls : OK.
- Mutation de la garde : rouge, comme dit plus haut.

## Relancé sur ab37187d (première relecture)
- `python3 -I scripts/bench/test_stable.py` : 12 tests, OK.
- `ci_state_reads_every_job` et `ci_state_without_gh` de `scripts/bench/test_ultra_bench.py`, appelés seuls : OK.
- Mutation de la garde de traversée (constat 1) : 12 tests verts, garde non couverte ; fichier rendu.

Verdict : 0 bloquant, 0 grave
