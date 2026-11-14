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

## 4. Méthode rapide, version 2 (après l'audit suspicieux du 14 novembre au soir)

La version 1 (« trois niveaux, une seule vague de contrôle à la fin ») est **retirée**. Un relecteur méfiant l'a démolie, preuves à l'appui : elle rouvrait la faille de la nuit du tour 6. Huit lots étaient entrés sans contrôle : 12 défauts corrigés sur 32, 3 graves neufs. Et elle ne gagnait que 4 à 14 % du coût des contrôles, pas les deux tiers promis.

### 4.1 Où part vraiment le quota (chiffres du tour 6)
- **Les sièges de contrôle du tuto.** 32 passages de contrôle, 3 612 gestes, dont 3 119 dans le tuto (86 %). Un siège relit tout son historique à chaque geste : son coût grandit comme le carré de sa longueur. Le tuto fait donc environ 96 % du coût des contrôles.
- **Courir après la tête.** Depuis le 12 novembre : 81 fusions de l'intégration dans les lots pour 38 entrées, et 108 commits de relecture. Des sièges ont été rejoués sur de simples commits de fusion.
- **La machinerie qui bouge pendant le tour.** 10 des 33 lots du tour 6 portaient sur le banc ou la méthode, plus 4 réparations de la CI. Sur 1 960 commits, 398 touchent l'appli.
- **Les réveils du chef** (minuteries, surveillances) : jamais mesurés. C'est une piste, pas un chiffre.

### 4.2 Le siège court ancré : le gros levier
Un lot garde son **constat avant d'entrer**, pour tous les bugs (CLAUDE.md, « corrigé » seulement si un testeur l'a vu). Mais le siège est court :
1. Un rejeu, joué par programme et donc sans quota, amène l'appli à l'endroit du défaut, dans les conditions d'origine (langue, appareil, police, mode).
2. L'IA ne joue ensuite que **20 à 40 gestes** sur la phrase d'acceptation et répond : corrigé, autre, encore là, pas atteint.

Preuve que c'est possible : le siège Da-11 a constaté sa correction en 11 gestes, contre 141 à 174 pour Da-4 à Da-9. Le tuto entier (300 gestes) n'est rejoué que pour un lot qui change sa mécanique.

### 4.3 Ne plus courir après la tête
La relecture et le siège d'un lot restent valables tant que les fusions arrivées depuis ne touchent **ni les fichiers du lot ni les écrans du chemin de son siège**. Un script le calcule. Le portail fusionne alors une seule fois et lance **une seule CI sur l'arbre fusionné**.

### 4.4 Geler la machinerie pendant un tour
Pas de lot du banc ni de changement de méthode pendant un tour, sauf une réparation qui bloque tout. Les améliorations s'écrivent dans une liste et entrent en un seul lot entre deux tours.

### 4.5 Le niveau sert à doser, pas à sauter le constat
Le niveau ne retire jamais le constat ni la CI du lot. Il règle seulement la profondeur de la relecture et la longueur du siège.
- `pret.sh` **calcule le niveau sur le diff**. Le trieur ne peut que le monter.
  - Seulement des textes, traductions ou captures : **niveau 1**, relecture courte du diff par un sous-agent neuf.
  - Un fichier `.kt` de `src/main` : **niveau 2 au moins**.
  - Une regex, un fichier de la coquille, la sauvegarde, le moteur ou la mécanique du tuto : **niveau 3**, circuit complet. (Le plantage du téléphone du 13 novembre venait d'une regex dans un lot « texte ».)
- Huit défauts par lot au plus, groupés par chapitre ou par écran.
- `pret.sh` fait aussi la paperasse :
  - il lance le test nommé sur le parent du vert et exige un échec sur son assertion, pas une erreur de compilation (fini l'appariement par les titres) ;
  - il lance les tests du lot, l'auto-test du banc et les rejeux touchés ;
  - il passe le portail à blanc contre la tête poussée.
  
  Il ne règle pas les refus dus à une tête qui bouge : c'est le rôle du §4.3.
- Un rejeu prouve qu'un défaut ne revient pas. Il ne prouve pas que le but de la personne est atteint : il ne remplace jamais le constat.

### 4.6 Mesurer avant de promettre
Le premier lot du prochain tour mesure le quota par rôle : siège, correcteur, relecteur, chef. On ne promet aucun chiffre avant. L'hypothèse à vérifier : le siège court ancré et la fin de la course après la tête attaquent les deux plus gros postes.

### 4.7 Ce qui ne bouge pas (la sécurité)
- Un constat avant l'entrée pour **tout** lot.
- La CI complète du lot, pour tous les niveaux.
- Un rejeu pour chaque bloquant ou grave.
- Le circuit complet au niveau 3.
- Le tuto entier pour un changement de sa mécanique.
- Fable proposé au 2e échec, mais Benjamin décide.
- CLAUDE.md ne change qu'avec son accord. Par exemple, alléger les minuteries « toutes les 2 h par lot » (§4.1, réveils du chef) se décidera après la mesure du §4.6.

### 4.8 À préparer entre deux tours (un seul lot du banc, au signal de Benjamin)
1. Le siège court ancré (§4.2).
2. `pret.sh` avec le niveau calculé sur le diff et la preuve rouge sur le parent (§4.5).
3. Le calcul « la fusion touche-t-elle le lot ? » (§4.3).
4. L'astuce D (`TEST_DIRS`).
5. Ensuite seulement, les astuces A et F.
