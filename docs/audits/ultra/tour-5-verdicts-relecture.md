# Relecture neuve du lot « verdicts » (tour 5, étape 3, lot 6)
Relu : 809457b87e4fa1f8c70961a7857c4f3708b53100

Relecteur neuf (ni auteur du code). Lu : `git diff $(git merge-base HEAD fx-int)...HEAD` (juge, portail, `test_ultra_bench.py`, `Tour5AppProof3Test`, `HumanTester`, verdicts.tsv, journal, paires-rejouees/), le rapport de l'auteur.

## Constats
- Grave 1 **corrigé** (rouge ed5eca33 / vert 809457b8, cas `replayed_pairs`). La règle « paire rejouée » fermait n'importe quel vert, passé ou futur, sur la seule foi des XML de `paires-rejouees/<vert>/` écrits par l'équipe. Le portail exige que la relecture neuve rejoue une preuve de `rouges-constates/` (« rouge constaté : … rejoué sur … »), mais rien pour `paires-rejouees/` : un vert futur sans rouge se fermait avec deux XML faits à la main. La règle ne vaut plus que pour les verts de l'histoire écrite avant elle (ancêtres de 92258b19, la base du lot ; `--fin-rejeux` pour un jouet). Nouveau cas « late » : un vert postérieur avec une preuve honnête reste un écart. Les trois paires historiques (dcc33677, 0b1cbbc3, 87b66934) restent fermées (même liste d'écarts que l'ancien juge sur l'arbre 81fb3ba5).
- `seat_class` se trompe sur une déclaration avec des parenthèses imbriquées (moyen) : `class X(a: Int = f(1)) : HumanTester()` n'est pas reconnue comme un siège du banc. Aucune preuve actuelle n'est dans ce cas (vérifié sur toutes les classes de `preuve_appli`).
- T5-Cc-41b est prouvé seulement par le formatage de « Mes exports » (`MyExports.detail`) (moyen) : la taille réelle d'un export n'est pas mesurée. La preuve dit bien que « 1 Ko » ne vient pas de l'affichage, pas que le fichier de l'appli a la bonne taille.
- `Tour5AppProof3Test` n'est pas une tautologie : le moteur imité ne fait que ce que l'appli lui demande. Mutation : appel `armSongEnd` retiré de `RhythmViewModel`, `theSongPlaysToItsEndAndItsCounterFollowsTheEngine` rougit (« the song stopped at its end … 12000 ms »). Reste que le comportement du moteur (bascule à la barre, fin de chanson) est celui de l'imitation ; le vrai `Engine.cpp` est couvert par ses tests hôtes, pas ici (mineur).
- Les XML des trois paires rejouées n'ont pas été rejoués par moi (363 à 2482 s chacun) ; noms, classes et messages d'échec sont cohérents avec les commits rouge/vert (mineur, histoire close par la borne ci-dessus).
- Le selftest de l'auteur ne scelle plus le code (le juge a changé) : le juge sur l'atelier dit « résumé refusé », toutes les autres lignes (paires git, journal, verdicts) sont à 0. Le portail relance le selftest et le juge sur le code fusionné.

## Mutations
- `replayed_red` sans la garde « rouge.xml échoue » : `replayed_pairs` rougit (neverred). Remis par chemin.
- `seat_class` sans le cas `HumanTester` direct : « a seat of the bench is no proof without it » rougit. Remis par chemin.
- `RhythmViewModel` sans `armSongEnd` : `Tour5AppProof3Test#theSongPlaysToItsEndAndItsCounterFollowsTheEngine` rougit. Remis par chemin.

## Tests rejoués
- `python3 -I scripts/bench/test_ultra_bench.py` : OK après le vert.
- À rejouer à la fusion : `*Tour5AppProof3Test`, `*BenchSelfTest`, `*BenchSeparationWaitTest`.

Verdict : 0 bloquant, 0 grave — fusion possible
