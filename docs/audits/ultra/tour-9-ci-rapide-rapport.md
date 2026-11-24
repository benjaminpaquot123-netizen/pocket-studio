# Tour 9, lot « ci-rapide » (une CI rapide, en lots côte à côte, sans fausse erreur) : rapport

Branche : eq/tour-9-ci-rapide. Commits : 77a4759d, 7dd14931, e7db06ff, cd02945e, rouge c3d8a090, vert 5b54498a.
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: nav.MySoundsJourneyTest remix.StemBoardVmTest live.LiveCellJourneyTest

## Chemins

chemin: Ma bibliothèque : garder un son, le renommer, le retrouver sur un pad et au clavier, le supprimer, quel que soit le test joué avant dans la même machine | test: MySoundsJourneyTest#keepRenameFindOnAPadAndOnTheKeysThenDelete
chemin: Remix : un étirement gardé dans Ma bibliothèque avec le tempo et la tonalité du morceau, bibliothèque vide au départ | test: StemBoardVmTest#aStretchIsKeptInMyLibraryWithTheSongsTempoAndKey

## Ce qui change (« tenté »)
- `.github/workflows/ci.yml` : les tests de l'appli tournent en six lots côte à côte (« Unit tests (shard k/6) »). Le job « Build, unit tests, screenshots » garde son nom et ne fait plus que rassembler : il vérifie que chaque classe a tourné une seule fois, nomme chaque test rouge avec son lot, réunit les captures. Les tests du moteur avec garde-fous mémoire et l'écoute en direct ont chacun leur job. Aucun job supprimé : émulateur, bancs de son, APK, publication sont toujours là.
- `scripts/ci/shards.py` (neuf) : `plan` répartit les classes d'après les durées mesurées (`scripts/ci/test-durations.tsv`), `check` compte et nomme les rouges, `selftest` se vérifie lui-même, `policy` refuse un job sans limite ou une limite au-dessus de 50 minutes (joué dans « Quick checks »).
- `app/build.gradle.kts` : le filtre `-Pshard.classes=<fichier>` (ne fait rien sans cette propriété).
- `scripts/ci/apt-install.sh` (neuf) : ffmpeg installé avec un cache des paquets.
- `scripts/bench/ci-etat.sh` et `scripts/bench/ci-rouge.sh` : lisent les nouveaux jobs ; `ci-rouge.sh` donne le test, le message, le lot, le rang et les classes d'avant.
- Un vrai défaut d'isolation trouvé par le découpage (CI 439, lot 2) : `LiveCellJourneyTest` laissait deux sons dans « Ma bibliothèque », qui vit pour toute la machine de test ; `MySoundsJourneyTest` les trouvait. Corrigé dans les tests seulement : `LiveCellJourneyTest` vide la bibliothèque en partant, `MySoundsJourneyTest` et `StemBoardVmTest` la vident en arrivant et affirment qu'elle est vide. Rien de sauté, rien de désactivé, la composition des lots n'a pas été changée pour le cacher.
- Hors de la liste de départ, avec accord : ces trois fichiers de test (accord du chef, 11 h 35 UTC) ; une ligne « CI rapide » dans `CLAUDE.md` (demande directe de Benjamin : « Fait en sorte que ça soit ainsi sur toute la suite du projet »).

## Tests lancés
Durées, en minutes (heures belges : CI 441 de 12 h 41 à 13 h 09).

| Job | Avant (CI 433, verte) | Après (CI 441, verte, run 45386493749, tête 5b54498a) |
|---|---|---|
| Build, unit tests, screenshots | 74 (63 sur la 430, 67 sur la 437, coupé à 75 d'autres fois) | 0,8 (il rassemble) |
| Unit tests (shard 1 à 6) | dans le job du dessus | 18,8 / 14,7 / 17,3 / 17,9 / 18,0 / 15,1 |
| Engine tests (Linux) | 28 (16 sur la 430) | 8,5 |
| Engine tests (sanitizers) | dans le job du dessus | 22,7 (26,0 sur la CI 439) |
| Sound benches (song) | 27 (53 sur la 430) | 15,8 |
| Sound benches (live listening, real projects, soak) | dans le job du dessus | 22,1 |
| Emulator smoke test | 16 | 19,3 |
| Release APK (unsigned) | 7 | 6,9 |
| Quick checks | 2 | 2,8 |
| Separation tests | 1 | 1,0 |
| Publish APK | | 1,0 |
| CI entière | 74 au mieux | 28 |

- Tests avant : 878 classes, 9900 tests, 4794 sautés (CI 437, tête 8c53c0fe). Après : 876 classes, 9890 tests, 4794 sautés, 0 rouge, 6 lots sur 6 (CI 441, journal du job qui rassemble). Classe par classe, les seuls écarts viennent de sources changées hors du lot : `playback.AutoBufferTest` (7 tests) et `playback.OutputChangeBufferTest` (4) supprimées, `human.Tour5BloquantsTest` de 9 à 10. 9900 − 11 + 1 = 9890. Aucune classe en double, aucune perdue.
- Rouge prouvé sur c3d8a090, en local, les trois classes ensemble : « another test left its sounds in the library ». Vert sur 5b54498a : 7, 1 et 7 tests, 0 échec.
- `ci-rouge.sh --dossier` joué sur le vrai rouge de la CI 439 (run 45382267764) : il nomme `MySoundsJourneyTest#keepRenameFindOnAPadAndOnTheKeysThenDelete`, le message « List has more than one element », le lot 2, le rang 48/116 et les trois classes d'avant.
- `ci-etat.sh` joué sur la tête verte 5b54498a : les 16 jobs lus, `ci_build vert`, `ci_emulateur vert` ; sur la tête de la CI 439 : `ci_build rouge`, `ci_emulateur vert`.
- `python3 scripts/ci/shards.py selftest` et `policy` : verts, en local et dans « Quick checks ».

## Ce qui n'est pas vert
- « Engine tests (sanitizers) » : 22,7 puis 26,0 minutes, sous 30 mais de peu. Le couper demande un argument de partage dans le programme de tests du moteur, hors de mes fichiers (voir Demandes).
- `ci-rouge.sh` avec un numéro de CI (téléchargement par `gh`) : pas prouvé ici, `gh` n'est pas connecté sur cette machine. Le chemin `--dossier` l'est.
- Le résumé du job qui rassemble écrit « 4794 passés » pour les tests sautés : mot trompeur, pas changé pour ne pas relancer une CI entière pour un mot.
- « Publish APK » a tourné sur ma CI lancée à la main : c'était déjà son comportement, je n'y ai pas touché.
- Deux CI seulement mesurées après le lot (439 et 441) : « sous 30 minutes même sur une machine lente » reste à confirmer sur les prochaines.

## Demandes (hors de mes fichiers)
- `engine/host-tests/engine_tests.cpp` : accepter une part (« 1/2 », « 2/2 ») pour couper les 370 tests sous garde-fous mémoire en deux jobs de 12 minutes.
- `scripts/ci/shards.py` : remplacer « passés » par « sautés » au prochain passage dans la zone CI.
- `docs/DECISIONS.md` : reporter les six lignes de `docs/decisions/tour-9-ci-rapide.md`.
