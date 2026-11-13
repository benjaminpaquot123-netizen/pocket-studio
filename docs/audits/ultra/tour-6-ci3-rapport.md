# Tour 6, lot du banc « ci3 » : la vérification de GitHub en parallèle

Branche : `eq/tour-6-ci3` (base `b4ac25f2`). Fichier changé : `.github/workflows/ci.yml` seulement. Décisions : `docs/decisions/tour-6-ci3.md`.

tests: aucun : lot de la CI seule (`.github/workflows/ci.yml`), aucun test Gradle changé ; preuve : le passage 43423146494 lancé sur la branche ; `python3 -I scripts/bench/test_socle.py` (chaque tâche attend « quick ») reste vert

## Ce qui change
- « build » (89 min d'un seul tenant) devient trois tâches côte à côte, avec les mêmes étapes, dans le même ordre à l'intérieur de chacune : « build » (tests et captures), « sound » (morceau, écoute en direct, vrais projets, endurance), « apk » (APK de release non signée, contrôle JNI, table R8).
- « build », « sound », « apk » et « smoke » n'attendent plus « engine » : seulement « quick ».
- La publication exige chaque tâche par son nom : `needs: [quick, engine, separation, build, sound, apk, smoke]`. Ses conditions (`if:`) ne changent pas.
- Aucun contrôle retiré : les 14 étapes de l'ancienne tâche se retrouvent une fois chacune (comparées une par une au fichier d'avant), les six artefacts aussi (`unit-test-reports`, `screenshots-diff`, `song-bench`, `live-bench`, `real-use`, `apk`, `r8-mapping`).

## Les `needs`, un par un
| Tâche | Avant | Après |
|---|---|---|
| quick | rien | rien |
| engine | quick | quick |
| separation | quick | quick |
| build (tests, captures) | engine | quick |
| sound (bancs du son) | (dans build) | quick |
| apk (APK de release) | (dans build) | quick |
| smoke (téléphone simulé) | engine | quick |
| publish | build, smoke, separation (et engine à travers eux) | quick, engine, separation, build, sound, apk, smoke |

## La preuve : passage 43423146494 (commit 5cae6a25, lancé à la main sur la branche)
Tout vert. Durée totale : 42 min (12 h 11 min 59 s à 12 h 54 min 03 s UTC), contre environ 119 min au passage 43402800379.

| Tâche | Durée | Détail |
|---|---|---|
| Quick checks | 1 min 30 | |
| Build, unit tests, screenshots | 39 min 54 | tests et captures 38 min 48 |
| Sound benches | 28 min 01 | morceau 16 min 47, écoute 5 min 58, vrais projets 1 min 34, endurance 2 min 42 |
| Release APK (unsigned) | 10 min 51 | compilation 9 min 49 |
| Emulator smoke test | 17 min 50 | parcours sur téléphone simulé 9 min 40 |
| Engine tests | 19 min 17 | |
| Separation tests | 1 min 20 | |
| Publish APK | 33 s | signature et contrôle de la clé faits ; « Publish preview » et « Publish stable release » sautées (ni PR ni `main`) : rien n'a été publié |

La durée totale est celle de la tâche la plus longue (tests et captures, 40 min) plus « quick » et la signature.

## Ce qui n'est pas vu, ou pas fait
- Un seul passage mesuré : les durées bougent d'un passage à l'autre (le moteur a mis 19 min ici, 27 au passage d'avant).
- Les deux caches de compilation neufs (`tests-ndk`, `sound-host`) sont partis à froid ; les passages suivants devraient être un peu plus courts, ce n'est pas mesuré.
- Un moteur rouge n'arrête plus les autres tâches en route : elles vont au bout (minutes de machine), mais la publication reste bloquée.
- Le chemin « pull request » et le chemin « push sur main » n'ont pas été joués (seul le lancement à la main l'a été) ; leurs conditions n'ont pas changé d'un caractère.
- Sept tâches tournent en même temps au lieu de trois : pas de file d'attente vue sur ce passage.

## Demandes (fichiers hors de la fiche)
- Demande : `scripts/bench/pre-vol.sh` (commentaire ligne 168, « every other job of the CI waits for them (needs: engine) ») : le commentaire n'est plus vrai, à corriger par un lot du banc.
- Demande : `scripts/bench/paquet-verifie.sh` : vérifier sa liste de tâches exigées (les noms « Sound benches … » et « Release APK (unsigned) » sont neufs).

## Chemins
- chemin: CI › chaque tâche attend « quick » | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace
