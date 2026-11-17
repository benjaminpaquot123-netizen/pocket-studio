# Audit de la méthode : nuit du 17 novembre (reste du tour 7)

Demandé par Benjamin le 17 novembre au matin. Période : de la reprise (02 h 05) à 07 h 25, heures belges.
Fait par une discussion neuve qui n'a rien écrit dans l'appli. Sources : l'historique du chef et des quatre équipes
(coûts, réveils, messages), les branches, la liste des CI de GitHub. Les chiffres sont mesurés, pas estimés.

## En une phrase

La partie « mémoire courte, quatre équipes en parallèle » marche très bien. La partie « vérifier et faire entrer »
est le bouchon : toute la nuit s'est passée à attendre des tests lancés à la main, et la CI de GitHub n'a pas tourné une seule fois.

## Le film de la nuit

| Heure | Fait |
|---|---|
| 02 h 05 | Le rappel réveille le chef. |
| 02 h 19 | Quatre discussions neuves ouvertes (tuto2, decoupe2, guide2, banc8), fiches de 650 à 700 mots, fichiers exacts. |
| 03 h 59 | banc8 fini. Entré et poussé dans l'intégration à 04 h 20. |
| 04 h 41 | tuto2 se tait. Personne ne le remarque avant 06 h 26. |
| 06 h 16 | decoupe2 fini, fusionné chez le chef en 19 secondes, puis 47 minutes de tests. Pas poussé. |
| 06 h 43 | guide2 fini. Pas encore entré à 07 h 18. |
| 06 h 56 | tuto2 « prêt pour contrôle ». Premier essai du contrôle raté (réglage manquant), relancé. |

## Ce que ça a coûté

Environ 32 millions d'unités en un peu plus de cinq heures (repère du tour 6 : 22 millions par bug).

| Qui | Part | Remarque |
|---|---|---|
| tuto2 | 47 % | 9 sous-agents ; le défaut D4 a cassé 22 tests, à réparer ensuite |
| guide2 | 21 % | un premier tour de 58 minutes |
| banc8 | 11 % | actif 91 % du temps, le plus efficace |
| decoupe2 | 11 % | |
| le chef | 10 % | 21 réveils, dont 19 automatiques |

Huit défauts de l'appli sont « tentés » (aucun encore constaté par un testeur). Cela fait environ 4 millions par défaut
pour l'instant, mais les sièges de contrôle et la CI ne sont pas encore payés : le chiffre final sera plus haut.

## Ce qui a bien marché

1. **Mémoire courte.** Aucune discussion n'a dépassé 165 k (au tour 6 : 730 à 870 k). C'est le gros gain, et il est réel.
2. **Parallèle propre.** Quatre équipes, quatre listes de fichiers séparées, une discussion neuve chacune, toutes en Opus 5.5.
3. **Travail propre dans les lots.** Paires rouge puis vert, relecture neuve en dernier commit, messages courts.
4. **Chef réactif aux messages.** Une ou deux secondes à vingt secondes pour répondre à une équipe.
5. **Points à Benjamin.** Seize points, tous finis par « Ce qui n'est pas vert ».

## Ce qui a cloché (du plus grave au moins grave)

1. **Zéro CI de toute la nuit.** Aucune des quatre branches n'a eu de CI. La dernière CI de l'intégration date du 16 novembre, 00 h 55.
   La règle du 14 novembre (« un lot n'entre que si sa propre CI est verte ») n'a pas été appliquée, et les fiches ne la demandaient pas.
   banc8 est entré avec le seul auto-test du banc ; decoupe2 avec des tests lancés à la main.
2. **Les grands tests ont tourné à la main, partout, plusieurs fois.** Les fiches demandaient aux équipes la suite complète (47 minutes)
   et toutes les captures (50 minutes), alors que la règle dit « équipes : tests ciblés seulement ».
   Résultat : decoupe2 a passé 118 minutes dans les grands tests, tuto2 les a lancés trois fois, et le chef les a refaits (47 minutes).
   Les équipes n'ont travaillé que 31 % de leur temps (hors banc8) ; le reste, c'est de l'attente.
3. **La minuterie horaire est revenue.** Cinq rappels enchaînés, quatre à 60 minutes pile, sur la grosse discussion du chef.
   La règle dit : pas de minuterie périodique, et moins d'une heure pour le secours. Celui de 05 h 26 est arrivé 62 minutes après
   la dernière action : toute la mémoire a été réécrite (le réveil le plus cher après le démarrage).
4. **Une équipe muette pendant 1 h 45.** tuto2 s'est tu à 04 h 41. Au rappel de 05 h 25, le chef a lu « il attend ses tests » (faux).
   Il ne l'a relancé qu'à 06 h 26. La minuterie n'a donc même pas servi à ça.
5. **Tout passe à la file chez le chef.** Un seul verrou de compilation : les tests de decoupe2, puis les rejeux, puis le contrôle du tuto.
   guide2 attend depuis 06 h 43. La règle dit « paquets de trois, puis une CI » ; la nuit a fait « un lot, puis 47 minutes ».
6. **Le chef est trop vieux.** C'est la discussion du 18 octobre. Elle a compacté quatre fois en cinq heures (elle en a annoncé une seule à Benjamin),
   relu ses fichiers à chaque fois, et n'a confié aucune lecture à un sous-agent.
7. **Papiers pas tenus.** `EN-COURS.md` n'a pas bougé depuis 02 h 20 (et fait plusieurs pages d'histoire au lieu d'une).
   L'intégration n'a pas été poussée depuis 04 h 20 : la fusion de decoupe2 n'existe que chez le chef.
   Quatre décisions prises seul sur cinq ne sont pas dans `DECISIONS.md`.
8. **Petites pertes.** Trois pannes d'installation (chemin du SDK, Maven, réglage du contrôle) ; le chien de garde qui tue une compilation saine
   après 20 minutes sans texte ; deux listes de fichiers qui se chevauchaient (le Guide en espagnol) ; des réponses automatiques en anglais.

## Cinq changements proposés

1. **Les grands tests vont à GitHub, pas aux équipes.** Fiche : tests ciblés seulement, puis CI lancée sur la branche du lot.
   Le chef fait entrer un paquet de trois lots au plus, puis une seule CI. Gain attendu : environ deux heures d'attente par lot.
2. **Plus de rappel horaire.** On attend la fin d'une CI ou le message d'une équipe. Un seul secours, à 45 minutes.
   Règle neuve : une équipe muette depuis 30 minutes sans test en cours reçoit un message tout de suite.
3. **Un chef neuf à chaque tour.** Il part de `EN-COURS.md`, ramené à une page et mis à jour à chaque événement.
   L'intégration est poussée après chaque entrée.
4. **Un départ tout prêt pour chaque équipe.** Réglages et cache déjà en place, vérifiés par une commande.
   Chien de garde réglé sur la durée réelle des tests.
5. **Listes de fichiers vérifiées par un script** avant l'envoi des fiches (aucun fichier dans deux lots).

## Ce qui n'est pas vert

- Aucune CI sur le travail de la nuit ; aucun défaut encore constaté par un testeur.
- decoupe2 fusionné seulement chez le chef, pas poussé ; guide2 et tuto2 pas encore entrés.
- Contrôle du tuto : premier essai raté, second en file d'attente.
- Environ 47 captures de référence à refaire ; rejeu `t4-26` rouge ; `RemixChantier10Test` rouge ; un test capricieux sans cause claire.
- Défaut du Clavier à 200 % de taille de texte et point D7 reportés au tour 8.
- Cet audit s'arrête à 07 h 25 : la suite de la matinée n'est pas mesurée.
