# Relecture du lot du banc « ci3 » (tour 6), par un relecteur neuf
Relu : c32133ddfc26c183fbc4b08603c152abd99224a6

Cible : `git diff b4ac25f2..c32133dd` — première passe sur `3adb3040` (`.github/workflows/ci.yml`, rapport, décisions), seconde passe sur `2ca5403d` (rouge : le test `ci_state_reads_every_job`) et `c32133dd` (vert : `scripts/bench/ci-etat.sh`). Aucun code de l'appli touché. Pas de Gradle, pas d'accès à GitHub : les durées du rapport sont lues pour leur cohérence seulement.

## Ce qui a été refait

### Première passe (3adb3040) : le découpage de la CI
- Comparaison des étapes par programme (PyYAML, script hors dépôt dans le bloc-notes) : l'ancienne tâche `build` (`git show b4ac25f2:.github/workflows/ci.yml`) contre `build` + `sound` + `apk`, chaque étape entière (`run:`, `env:`, `if:`, `with:`, actions et leurs sha). Aucune étape perdue ni changée ; les seules en plus sont les six de mise en place recopiées à l'identique dans `sound` et `apk`, plus les clés ccache `tests-ndk` et `sound-host` (`android-ndk` reste à `apk`), comme le disent les décisions. L'ordre interne est gardé (indices de l'ancienne : build 0-3,5-8,17 ; sound 0-3,5,9-15 ; apk 0-5,16,18,19). `env:` identique, aucun secret dans les trois.
- Dépendances d'ordre lues dans les scripts : les vrais projets (`TOOLS=build/live-bench/tools`) restent après l'écoute en direct, dans `sound` ; `soak.sh` refait ses propres outils ; ffmpeg/fdkaac (étape du morceau), Pillow (étape des vrais projets) et ccache sont installés dans `sound` ; `song-bench.sh` et `live-bench.sh` lancent eux-mêmes Gradle et ne lisent rien des tests unitaires ; `check-jni.sh` ne lit que des sorties d'`assembleRelease`, dans la même tâche `apk` ; `screenshot-diff.py` et l'artefact `screenshots-diff` restent dans `build`, après `compareRoborazziDebug`.
- `needs:` un par un : quick (rien), engine/separation (quick), build/sound/apk/smoke (quick), publish `[quick, engine, separation, build, sound, apk, smoke]`. Aucune autre clé ne change ailleurs. `on`, `permissions`, `concurrency`, `env` du haut, et tout le corps de `publish` (`if:` compris) : identiques ; sans `always()`, la publication exige les sept tâches vertes. Aucun événement ne publie plus qu'avant.
- Délais contre les durées mesurées : build 75 pour 40 (43 avant), sound 70 pour 28 (39 avant, 50 au pire des anciens commentaires), apk 40 pour 11 (6 avant). Plus de 20 min de marge partout.
- `python3 -I scripts/bench/test_socle.py` : 29 tests OK. `yaml.safe_load` du fichier : OK. `rapport-zone.py` : vide, code 0. Arithmétique du rapport : 42 min 04 s de bout en bout, étapes de `sound` 27:01 pour 28:01 de tâche : cohérent.

### Seconde passe (2ca5403d, c32133dd) : la correction du G1
- Rouge rejoué sur son commit : sur `2ca5403d`, `ci_state_reads_every_job` (appelée par `importlib`, le fichier entier étant long) échoue sur « a red « sound » job makes ci_build red: {'ci_build': 'vert', …} ». Sur `c32133dd` : OK.
- Sur la tête : `python3 -I scripts/bench/test_ultra_bench.py` entier vert (« ultra bench scripts: OK », code 0) et `python3 -I scripts/bench/test_socle.py` vert (29 tests).
- Le test se casse quand on casse le contrôle (deux mutations, chacune remise) : `*/rouge) build="rouge"` ôté → « a red « sound » job makes ci_build red » rouge ; `"en cours"/*)` qui écrase au lieu de garder → « a job still running: ci_build is not green » rouge.
- Attaques sur le vrai `ci-etat.sh` avec un faux `gh` (script hors dépôt) : ordre des tâches inversé avec le rouge en premier → rouge (le résultat ne dépend pas de l'ordre) ; `build` sautée → rouge ; `engine` annulée → rouge ; passage sans aucune tâche → « en cours » des deux côtés ; rouge contre « en cours » → rouge ; tâche de l'émulateur rouge → `ci_emulateur` rouge et `ci_build` vert, publication sautée ignorée.
- Lecteurs : `tableau-de-bord.py` (clé `ci_build`, critère H) et `pre-vol.sh` lisent toujours les deux mêmes lignes, au même format (`clé<TAB>valeur`) ; le format de sortie n'a pas changé. Deux défauts de ces lecteurs, antérieurs au lot et non introduits par lui, sont notés ci-dessous (m2, m3).

## Constats

| id | gravité | constat | fichier:ligne | état |
|---|---|---|---|---|
| G1 | grave | `ci-etat.sh` ne lisait que la tâche nommée « Build … » : un banc du son, un APK, un moteur ou une séparation rouges laissaient « ci_build vert ». | scripts/bench/ci-etat.sh:34-51 | **corrigé** par c32133dd (rouge 2ca5403d rejoué, vert vérifié, contrôle cassé deux fois : voir « Seconde passe ») |
| M1 | moyen | Deux passages de CI pour le même commit : le pire l'emporte sur l'ensemble des passages, donc une relance verte n'efface jamais un rouge précédent (vérifié : vert neuf + rouge ancien → rouge). Direction sûre, mais le tableau de bord restera rouge après une relance qui corrige un test instable. | scripts/bench/ci-etat.sh:24-52 | ouvert |
| M2 | moyen | Un passage d'un autre flux de travail sur le même commit écrase encore `ci_build` par simple affectation : `Packs` (tâches « build (techno) » sans nom explicite) rend vert un `ci_build` rouge de la CI (vérifié), et une tâche « build » sautée le rend rouge. Antérieur au lot, mais c'est la même porte que le G1. | scripts/bench/ci-etat.sh:47-50 ; .github/workflows/packs.yml:87 | ouvert |
| m1 | mineur | Le test neuf ne couvre que le flux « CI » : la branche « autre flux de travail » (lecture `*uild*`) n'a aucun cas, donc M2 ne rougirait nulle part. | scripts/bench/test_ultra_bench.py:2447-2477 | ouvert |
| m2 | mineur | Antérieur au lot : `pre-vol.sh` lit la sortie avec `sed -n 's/^ci_build //p'` (espace) alors que `ci-etat.sh` écrit une tabulation ; le motif ne trouve rien (vérifié), donc le garde-fou « la CI de ce commit est déjà rouge » ne se déclenche jamais. | scripts/bench/pre-vol.sh:113 ; scripts/bench/ci-etat.sh:60 | ouvert |
| m3 | mineur | Antérieur au lot : `--ecris` écrit « vert (c32133d) » et `tableau-de-bord.py` compare à « vert » exactement, donc le critère H est « non » même tout vert (vérifié avec un faux `gh`). | scripts/bench/ci-etat.sh:56-57 ; scripts/bench/tableau-de-bord.py:233 | ouvert |
| m4 | mineur | « Publish APK » est exclue des deux états : un rouge de la signature ou du contrôle de la clé n'apparaît nulle part dans `ci_build` (comportement voulu et écrit dans les décisions ; `paquet-verifie.sh` le verra quand il exigera toutes les tâches). | scripts/bench/ci-etat.sh:36 | ouvert |
| m5 | mineur | Trois caches ccache et trois caches Gradle au lieu d'un : plus de place prise dans les 10 Go de caches du dépôt (le cache de l'émulateur peut être chassé) ; non mesuré. | .github/workflows/ci.yml:153, 218, 331 | ouvert |

Les constats M1, m1 et M2 de la première passe (ligne « chemin » du rapport, « six artefacts », « sept tâches », l'étape des banques d'instruments, `paquet-verifie.sh`) sont repris dans le rapport : vérifié sur `c32133dd`.

Verdict : 0 bloquant, 0 grave
