# Méthode v4 : la base des prochains audits (tri du 14 novembre)

Demande de Benjamin (2026-11-14, fin du tour 6) : trier et corriger les règles, puis rendre les corrections et les tests plus rapides sans plus de bugs. Cette page **prime sur `AUDIT-ULTRA.md` §12 et §13** quand ils se contredisent. Elle ne lance rien : le prochain audit part sur un signal de Benjamin.

## 1. Les règles, triées (ce qui tient)

**Avant une vague**
1. On vide la file : tout lot prêt est fusionné, la CI de la tête est verte (téléphone simulé compris), le **départ** (le commit testé) est écrit dans `EN-COURS.md` (§13.1).
2. Un siège pilote par mission, vingt minutes, avant la vague (§13.4).

**Pendant la vague**
3. Chaque siège joue le commit du départ, jamais « la tête », et rend du texte seul (§13.1, §13.4).
4. Rien qui change l'appli ou le banc n'entre dans l'intégration (§13.1).

**Corriger un lot**
5. Une zone, un lot ouvert, huit défauts au plus, des fichiers disjoints des autres lots (§13.3, §13.5).
6. La preuve avant le code : commit « rouge: » (le test seul, vu rouge), puis « vert: » (la correction), jamais rangés après coup (§13.5).
7. Relecture neuve qui relance les tests ; « grave » = change la couleur d'un test, bloque un débutant, touche la sauvegarde ou le son (§13.5).
8. Siège de contrôle sur la branche poussée du lot, qui rejoue la mission d'origine, pas plus. Le tuto entier (300 gestes) seulement si le lot change sa mécanique (§13.10).
9. **Le lot lance lui-même le portail à blanc** (`pre-vol.sh`) et cite sa sortie avant d'écrire « prêt » (§13.11).
10. **La CI complète du lot est verte sur une tête qui contient l'intégration** du moment (§13.11).

**Fusionner**
11. Le portail, un lot après l'autre, l'intégration poussée après chaque réussite ; paquet de trois lots au plus (§13.6).
12. **Hors portail : seulement avec les mots de Benjamin**, écrits dans le commit et dans `AUDIT-ULTRA.md` (cas tuto4, 14 novembre).

**Parler à Benjamin**
13. Une ligne par étape dans la discussion principale, qui finit par « Ce qui n'est pas vert » ; heures belges ; pas de « je soupçonne » sans un chiffre (§13.7, §13.10).
14. **Alerte boucle** : au 2e refus d'un même lot, une ligne à Benjamin avec la cause et « Fable utile ou non » ; il décide. Un lot refusé deux fois passe en priorité (§13.11).
15. Le compteur en trois lignes : corrigés, en salle d'attente, encore à corriger (§13.10).
16. Mode économie de quota : messages d'une ligne entre discussions, sorties filtrées (`EN-COURS.md`).

## 2. Ce qui est corrigé (contradictions et règles mortes)

- **Double CI** : §13.6 demandait une CI à la fin de chaque paquet, et §13.11 une CI par lot. Désormais, si l'arbre fusionné est identique à celui que la CI du lot a vérifié, la CI du lot compte pour le paquet. Sinon, on lance la CI du paquet. C'est rare au-delà du premier lot d'un paquet ; le vrai remède est le §4.2.
- **« 7 jobs »** (§13.11) devient « tous les travaux de la CI » : ils sont 8 avec la publication.
- **Le registre `corrections.tsv`** (§13.5, quatre états) n'a jamais été rempli, et le compteur final a dû être estimé. Il reste, mais c'est le portail qui doit y écrire la ligne du lot (lot du banc, astuce E). D'ici là, le chef l'écrit à chaque fusion.
- **Allers-retours** : §13.5 permettait deux allers-retours par défaut avant de le sortir du lot. C'est aligné sur l'alerte boucle : au 2e refus, Benjamin est prévenu.
- **« Le chef ne fusionne jamais hors du portail »** (§13.2) : l'exception tuto4 est encadrée par la règle 12.
- **Retirée** : la liste d'essais au téléphone pendant une clôture (§13.10). Benjamin fera un essai téléphone à part, quand il le décide (14 novembre).

## 3. Astuces techniques (corrigées après l'audit suspicieux du 14 novembre)

| | Astuce | Ce qu'elle apporte vraiment | Garde-fou |
|---|---|---|---|
| A | Découper la CI : les tests par famille (pas au hasard) et les bancs du son en deux | de la **vitesse** seulement (5 à 15 min par CI) ; le temps machine ne coûte pas de quota | la suite mélangée complète tourne encore une nuit sur deux : c'est elle qui trouve les tests qui héritent de leur voisin |
| C | `pret.sh` (§4.5) | moins de refus de forme | il prouve le rouge en lançant le test sur le parent du vert |
| D | Le juge lit aussi les tests de `project-model` (`TEST_DIRS`) | supprime le refus qui a forcé l'exception tuto4 | aucun : il voit plus de tests, pas moins |
| E | Le portail écrit la ligne du registre `corrections.tsv` | un compteur exact | il n'écrit que ce qu'il a vérifié |
| F | Les rejeux tournent dans la CI | un libellé changé qui casse un rejeu se voit tout de suite (rejeux7 : 5 refus) | le lot qui change le libellé recale le rejeu |
| G | Le siège de contrôle part en même temps que la CI du lot, branche figée | du temps d'attente en moins | toute retouche relance les deux |

## 4. Méthode rapide, version 3 (mesurée le 15 novembre)

La version 1 est retirée : un relecteur méfiant l'a démolie. La version 2 visait le temps des testeurs et les refus, sans mesure. La version 3 part d'une **mesure du quota réel** du tour 6 : l'usage de chaque discussion (`get_session`) et l'historique du chef, jour par jour.

### 4.1 Ce que coûte un tour (mesuré)
- Tour 6 (12-14 novembre) : environ 1 100 millions d'« unités » pour une cinquantaine de bugs, soit **environ 22 M d'unités par bug**. Une unité est un jeton ramené au prix d'un jeton d'entrée : sortie ×5, relecture de mémoire ×0,1, réécriture ×2.
- Par rôle : correcteurs 46 %, chef et ses sous-agents 20 %, testeurs 17 %, banc 15 %, relecteurs et lecteurs 2 %.
- Le mécanisme : à chaque action, une discussion relit toute sa mémoire (son « contexte »).
  - Ces relectures font 96 à 98 % des jetons et 68 à 80 % du coût.
  - Les réécritures de la mémoire (réveil après plus d'une heure, ou sous-agent qui a attendu plus de 5 minutes) font 15 à 44 % du coût.
  - Le texte écrit par l'IA ne fait que 5 à 10 %.
- Donc **coût ≈ nombre d'actions × taille de la mémoire**. Une action à 850 k de mémoire coûte environ 8 fois plus qu'à 100 k.
  - Les correcteurs ont fini entre 730 et 870 k, les testeurs entre 460 et 600 k. La mémoire médiane du chef était de 425 k.
  - Le compactage automatique ne passait qu'à 900 k.
- La version 2 visait surtout le nombre d'actions. Le premier levier est en fait la taille de la mémoire.

### 4.2 Les leviers, classés par gain

**1. Une mémoire courte partout : environ 35 à 45 % du coût d'un tour.**
- **Réglage, posé et vérifié le 15 novembre** : `.claude/settings.json` du dépôt fixe `CLAUDE_CODE_AUTO_COMPACT_WINDOW=200000` (plus la clé `autoCompactWindow`).
  - Une discussion de test l'a lu (« source env ») : fenêtre utile de 180 k, compactage à 144 k au lieu de 901 600. Le test a coûté 17 centimes.
  - Il vaut pour toute discussion neuve ouverte sur une branche qui contient le fichier (`main` après la fusion).
  - Dans une discussion déjà ouverte, la commande `/autocompact 200k` fait la même chose.
- **Règles** :
  - un lot = une discussion neuve (ou un sous-agent) qui s'arrête à la fin du lot ;
  - un siège = un sous-agent neuf (c'est déjà le cas) ;
  - une discussion de plus de 200 k ne reçoit plus de travail neuf. Cette règle remplace la règle « 40 % », qui laissait monter jusqu'à 400 k.
- **Calcul** : la mémoire moyenne passe de 400-450 k à environ 120-150 k, donc les relectures (68 à 80 % du coût) sont divisées par 3. On compte 15 % d'actions en plus pour relire un fichier après un compactage. Résultat : environ 50 % de moins pour les correcteurs, les testeurs, le banc et le chef, qui font 90 % du coût total.
- **Garde-fou** :
  - une section « # Compact instructions » dans CLAUDE.md, qui garde le lot, la branche, les sha, les refus, ce qui n'est pas vert et les interdits ;
  - l'état vit dans les fichiers (`EN-COURS.md`, le rapport du lot) et se relit après chaque compactage ;
  - CLAUDE.md est rechargé après un compactage.

**2. Moins de réveils à froid : environ 3 à 6 %.**
- **Sous-agents** : garder leur mémoire 1 h au lieu de 5 min (`CLAUDE_CODE_SUBAGENT_PROMPT_CACHE_TTL=1h`). 36 % du coût des sous-agents du chef venait d'actions « à froid » après une attente : les tests Gradle durent de 5 à 50 minutes.
  - Pas encore posé. À 1 h, chaque écriture coûte plus cher (×2 au lieu de ×1,25), et les sièges, qui jouent des gestes rapprochés, n'y gagnent rien.
  - À mesurer au prochain tour avant de le poser : d'abord sur les sous-agents qui attendent Gradle.
- **Minuteries** : plus de réveil toutes les 2 h sur une discussion à grosse mémoire. Chaque réveil réécrit toute la mémoire, pour environ dix fois le prix d'une action normale.
  - On attend plutôt un événement : fin de CI, message d'une discussion.
  - Une surveillance qui doit vraiment tourner se réveille avant une heure.
  - La règle des minuteries de CLAUDE.md change donc, avec l'accord de Benjamin.

**3. Moins d'actions refaites : environ 5 à 10 %.** Ce sont les points de la version 2, inchangés.
- **Ne plus courir après la tête.** Depuis le 12 novembre : 81 fusions de l'intégration dans les lots pour 38 entrées, et 108 commits de relecture.
  - La relecture et le siège d'un lot restent valables si les fusions arrivées depuis ne touchent ni ses fichiers ni les écrans de son siège. Un script le calcule.
  - Le portail lance une seule CI, sur l'arbre fusionné.
- **`pret.sh`** :
  - il calcule le niveau sur le diff : des textes seuls donnent le niveau 1 ; un `.kt` de `src/main`, au moins le niveau 2 ; une regex, la coquille, la sauvegarde, le moteur ou la mécanique du tuto, le niveau 3 ;
  - il prouve le rouge en lançant le test sur le parent du vert ;
  - il lance les tests du lot, l'auto-test du banc et les rejeux touchés ;
  - il passe le portail à blanc.
- **Machinerie gelée pendant un tour** : 10 des 33 lots du tour 6 portaient sur le banc ou la méthode.

**4. Le siège court ancré : environ 5 à 8 %, surtout pour le tuto (86 % des gestes de contrôle).**
- Il faut d'abord le construire.
  - Le banc sait rejouer des gestes (`rejeu=` et `rejeu.n=` dans `start.txt`), mais le siège s'arrête après le rejeu (`HumanTester.kt` : le verdict, puis la fin).
  - `amenee=` ne connaît que premier-morceau, remix et padlive (`SeatOptions.kt`).
- Un lot du banc ajoute deux options :
  - « rejouer tant que l'écran colle, puis rendre la main au testeur » ;
  - `amenee=tuto:<style>:<chapitre>`.
- Le testeur ne joue alors que 20 à 40 gestes sur la phrase d'acceptation. Da-11 en a joué 11, contre 141 à 174 avant.

**5. Fable sur une discussion neuve, jamais sur une longue.**
- Fable a sa propre limite. 1 426 actions du chef sur Fable 5.2, avec une mémoire médiane de 425 k, l'ont vidée pour la semaine.
- Pour un problème dur, Fable travaille dans un sous-agent ou une discussion neuve, avec une fiche courte.
- Benjamin décide toujours quand on l'utilise.

**6. Une CI plus rapide** (astuce A) : du temps gagné, pas du quota.

### 4.3 Ce qu'on peut attendre
- Le levier 1 retire 35 à 45 % du coût. Les leviers 2 à 4 retirent encore 15 à 25 % de ce qui reste.
- Au total, un tour devrait coûter **environ moitié moins**, peut-être 2,5 fois moins : environ 9 à 12 M d'unités par bug au lieu de 22.
- C'est une estimation. Le premier tour se mesure de la même façon (usage de chaque discussion, historique du chef jour par jour) avant d'annoncer un chiffre.

### 4.4 Ce qui ne bouge pas (la sécurité)
- Un constat par un testeur neuf avant l'entrée de tout lot : plus court, mais réel.
- La CI complète du lot, et avant `main`.
- Un rejeu pour chaque bloquant ou grave.
- Le circuit complet au niveau 3, et le tuto entier pour un changement de sa mécanique.
- Le risque neuf : un résumé de compactage qui oublie une consigne. Il est couvert par CLAUDE.md, rechargé après chaque compactage, par les fichiers d'état et par la section « # Compact instructions ».

### 4.5 L'ordre, au signal de Benjamin
1. Fait le 15 novembre : le réglage du compactage a été testé (compactage à 144 k), puis posé dans `.claude/settings.json`.
2. Fait le 15 novembre : « # Compact instructions » et les règles des leviers 1, 2 et 5 sont dans CLAUDE.md.
3. Entre deux tours, un lot du banc : `pret.sh`, le siège court ancré, le calcul « la fusion touche-t-elle le lot ? » et `TEST_DIRS` (astuce D).
4. Au premier tour, mesurer de la même façon et comparer aux 22 M d'unités par bug.
   Fait le 19 novembre pour le tour 8 : 88 M d'unités pour 25 discussions (245 $), 9 bugs constatés corrigés, soit environ 10 M par bug sans le chef (son compteur n'est pas séparable par tour : 4 271 M depuis octobre, point zéro du tour 9). Détail et suite : `PREPARATION-TOUR-9.md`.
