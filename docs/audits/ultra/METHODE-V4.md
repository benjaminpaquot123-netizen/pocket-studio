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

- **Double CI** : §13.6 demandait une CI à la fin de chaque paquet, et §13.11 une CI par lot. Désormais, si l'arbre fusionné est identique à celui que la CI du lot a vérifié, la CI du lot compte pour le paquet. Sinon, on lance la CI du paquet. Gain : environ 50 minutes par paquet.
- **« 7 jobs »** (§13.11) devient « tous les travaux de la CI » : ils sont 8 avec la publication.
- **Le registre `corrections.tsv`** (§13.5, quatre états) n'a jamais été rempli, et le compteur final a dû être estimé. Il reste, mais c'est le portail qui doit y écrire la ligne du lot (lot du banc, astuce E). D'ici là, le chef l'écrit à chaque fusion.
- **Allers-retours** : §13.5 permettait deux allers-retours par défaut avant de le sortir du lot. C'est aligné sur l'alerte boucle : au 2e refus, Benjamin est prévenu.
- **« Le chef ne fusionne jamais hors du portail »** (§13.2) : l'exception tuto4 est encadrée par la règle 12.
- **Retirée** : la liste d'essais au téléphone pendant une clôture (§13.10). Benjamin fera un essai téléphone à part, quand il le décide (14 novembre).

## 3. Astuces pour aller plus vite sans plus de bugs (à faire dans un lot du banc, au signal de Benjamin)

Ce qui a coûté le plus cher au tour 6, ce ne sont pas les corrections : ce sont les **refus de forme** (tuto4 : 4 refus, rejeux7 : 5) et l'**attente de la CI** (environ 50 minutes, dont 49 pour une seule étape).

| | Astuce | Gain estimé | Garde-fou |
|---|---|---|---|
| A | Découper l'étape « tests et captures » de la CI en 4 morceaux qui tournent en même temps | 49 → environ 20 min par CI | Chaque morceau tourne seul : un test qui ne passait que grâce à son voisin se voit, et c'est un vrai défaut |
| B | La CI du lot compte pour le paquet si l'arbre est identique (§2) | environ 50 min par paquet | Le chef compare les arbres avec `git diff` |
| C | Un petit outil `rouge.sh` qui commit le test rouge, le lance et range sa preuve tout seul | supprime les refus « vert sans rouge » (3 refus de tuto4) | La preuve est le vrai résultat du test |
| D | Le juge lit aussi les tests de `project-model` (`TEST_DIRS` de `verifie-chantier.py`) | supprime le refus qui a forcé l'exception tuto4 | Aucun : il voit plus de tests, pas moins |
| E | Le portail écrit lui-même la ligne du registre `corrections.tsv` | compteur exact sans recompter | Il écrit ce qu'il a vérifié, rien d'autre |
| F | Les rejeux tournent dans la CI | supprime les refus « un bouton renommé casse un rejeu » (rejeux7 : 5 refus) | Une CI verte prouve aussi les rejeux |
| G | Le siège de contrôle part en même temps que la CI du lot, une fois la branche figée | environ 45 min par lot | Toute retouche après le départ relance les deux |
| H | Avant la CI complète, le lot lance en local seulement ses tests et l'auto-test du banc | moins de CI rouges, donc moins de relances | La CI complète reste obligatoire (règle 10) |

L'ordre conseillé est D, C, A, B, puis F, E, G et H. D et C sont petits et suppriment le plus de refus ; A gagne le plus de temps à chaque CI.

## 4. Méthode rapide (demande de Benjamin, 14 novembre au soir)

Constat : environ deux semaines de quota pour une cinquantaine de bugs corrigés au tour 6. Le quota n'est pas parti dans les corrections elles-mêmes, mais dans trois choses :
- **les testeurs joués par une IA geste par geste**, surtout les sièges de contrôle rejoués après chaque lot ;
- **les refus suivis d'un « tout recommencer »** : nouvelle branche, nouvelle CI, nouveau siège ;
- **le même circuit lourd pour chaque bug**, qu'il s'agisse d'un mot mal traduit ou d'une perte de sauvegarde.

Le principe : **le prix d'une correction suit son risque**. Le contrôle lourd reste là où il protège vraiment.

### 4.1 Trois niveaux de bug, trois circuits

| Niveau | Exemples | Circuit | Lot |
|---|---|---|---|
| **1 — surface** | texte, traduction, libellé, couleur, marge, capture | test ou capture → `pret.sh` → portail. Pas de relecture neuve, pas de siège | jusqu'à 20 défauts par zone |
| **2 — comportement** | un bouton qui fait la mauvaise chose, un écran qui bloque à moitié | rouge/vert + rejeu → relecture courte par un sous-agent neuf → `pret.sh` → portail. **Le rejeu remplace le siège de contrôle** | jusqu'à 8 défauts |
| **3 — critique** | bloquant, sauvegarde, son, mécanique du tuto, plantage | circuit complet actuel (§1, règles 5 à 12), siège de contrôle compris | 1 à 3 défauts |

Le trieur donne le niveau. **Dans le doute, on prend le niveau au-dessus.** Un test existant qui change de couleur fait passer le lot au niveau 3.

### 4.2 Une seule vague de contrôle par tour, pas un siège par lot
Les lots de niveau 1 et 2 entrent sur leurs tests et leurs rejeux. À la fin du tour, **une seule vague de contrôle** rejoue d'un coup les phrases de tous les défauts corrigés, une mission par zone. Ce qui revient repart en un seul lot de rattrapage. Gain : la plus grosse part du quota de contrôle.

### 4.3 Zéro refus de forme : `pret.sh` fait la paperasse
Un seul script, lancé par le lot. Il fait le commit rouge et range la preuve, lance les tests du lot et l'auto-test du banc, passe le portail à blanc contre la tête poussée, puis affiche « PRÊT » ou la raison exacte du refus. Le chef ne reçoit que des lots déjà « PRÊT ». Un refus du vrai portail devient alors un bug du script, à réparer une fois pour toutes.

### 4.4 Le temps machine ne coûte pas de quota, l'attente oui
- La CI coupée en 4 morceaux (astuce A) : 49 → environ 20 minutes.
- Personne ne surveille une CI en boucle : une attente en arrière-plan qui réveille la discussion à la fin.
- Pendant une CI, la discussion ne fait rien d'autre que le lot suivant.

### 4.5 Moins de discussions, plus petites
- Un lot = un sous-agent neuf, lancé par la discussion de sa zone. Il rend une ligne et disparaît. Pas de longues discussions qui gonflent et coûtent dix fois plus par geste.
- Trois discussions au plus pendant les corrections : le chef, un correcteur, le banc.
- **Fable dès le 2e échec sur une même cause floue**, au lieu de cinq tours de manège.

### 4.6 Ce qui ne bouge pas (la sécurité)
- Pas de fusion dans `main` sans la CI complète verte, téléphone simulé compris.
- Un rejeu pour chaque bloquant ou grave.
- Le niveau 3 garde tout le circuit actuel.
- La vague de contrôle de fin de tour reste obligatoire avant de dire « corrigé » à Benjamin.

### 4.7 Ce qu'on peut espérer (estimation, à vérifier au premier tour)
Si environ 6 bugs sur 10 sont de niveau 1, 3 de niveau 2 et 1 de niveau 3, le coût moyen d'un bug tombe à environ un tiers. Une cinquantaine de corrections tiendraient alors en quelques jours de quota au lieu de deux semaines. Avant le prochain tour, un lot du banc fait `pret.sh` (§4.3) et les astuces D, C et A du §3.
