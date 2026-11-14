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
