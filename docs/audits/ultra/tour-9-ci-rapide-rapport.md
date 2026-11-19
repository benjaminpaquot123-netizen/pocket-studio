# Tour 9, lot « ci-rapide » (zone outils de la CI) : rapport

Branche : ccr-d01d15e8-jogd6p (la discussion ne peut pousser que là), partie de l'intégration ccr-80af1a1b-mn0wec (base c79d3442).
Rôle : correcteur des outils. Aucun fichier de l'appli touché (ni app/src/main ni project-model/src/main).
Demande de Benjamin (2026-11-19) : « On sait pas prendre un gros coup de boost quitte à prendre quelque risque ? »

Mesure de départ (CI 393 à 398) : 64 minutes pour le travail « Build, unit tests, screenshots », dont 58 dans une seule
tâche, `:app:testDebugUnitTest`, dans un seul processus (9507 tests, 1014 captures comparées).

Ce qui change :
- `app/build.gradle.kts` : `-Pshard=<k>/<n>` ne garde que les classes de test dont le chemin tombe dans la part k de n.
  Sans la propriété, rien ne change (les bancs, `-Pshuffle`, les lancements locaux).
- `.github/workflows/ci.yml` : travail neuf « tests », 4 machines côte à côte (« Unit tests and screenshots (part k) »),
  chacune lance sa part et dit « Tests, partie k/4 : N dans C classes, S sautés, R rouges. ». Le travail
  « Build, unit tests, screenshots » garde son nom : il rassemble les résumés de captures et les rapports, imprime la
  ligne « Captures : … » et n'est vert que si les 4 machines le sont.
- `scripts/screenshot-diff.py` : lit plusieurs résumés comme un seul ; un résumé manquant = pas de ligne « Captures : ».
- `scripts/bench/ci-rouge.sh` : range un rouge parmi les classes de sa machine (« machine 2, classe 31/160 »).
- `scripts/bench/paquet-verifie.sh` : pas changé. Il lit déjà la nouvelle forme (tests ajoutés pour le prouver).

Preuves :
- `ci-rouge.sh` : commit « rouge: » b76bc637 (`test_stable.py`, une CI coupée en deux machines : le rang et les classes
  d'avant étaient ceux des deux machines mélangées), vert au commit suivant d977d18b.
- `paquet-verifie.sh` : trois tests neufs dans `test_portail3.py` (une machine rouge est nommée avec le travail qui
  rassemble ; les captures de quatre machines font un seul compte, lu par le vrai `screenshot-diff.py` ; une machine
  sans résumé = « captures non lues »). Verts du premier coup : c'est une garde, pas un rouge puis vert, le script
  n'avait rien à corriger.
- `test_socle.py` : la liste des parts de `ci.yml` vaut 1..TEST_PARTS, le travail qui rassemble garde son nom et
  attend les quatre machines.
- `CiShardTest` : la règle du script de construction et celle du test sont la même ; aucune part ne porte plus de
  40 % des classes.
- CI réelle : voir « Durées » ci-dessous.

corrige: ci-64-minutes | gravité: moyen

tests: com.pocketstudio.app.CiShardTest

chemin: CI, suite des tests coupée en parts (la règle du script de construction) | test: CiShardTest#theBuildScriptSplitsByThisRule
chemin: CI, une classe imbriquée suit sa classe | test: CiShardTest#aNestedClassRunsWithItsOuterClass
chemin: CI, aucune part ne porte presque toute la suite | test: CiShardTest#noPartCarriesMostOfTheSuite

## Durées

À remplir à la lecture de la CI (voir plus bas dans ce fichier, section ajoutée après le passage).

## Ce qui n'est pas vert

- Le partage est par nom de classe, pas par durée : une machine peut être plus longue que les autres.
- Une classe n'a plus les mêmes voisines qu'avant : un test fragile peut changer de couleur (voir les décisions du lot).
- `Coquille2SeatTest#p5_theLiveProjectOpensAgainOnPadLive` reste capricieux tant que le lot L9 n'est pas entré.
