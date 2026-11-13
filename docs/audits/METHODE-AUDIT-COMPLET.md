# La méthode d'audit complet de Benjamin

Écrite le 13 novembre 2026, à la demande de Benjamin, après la journée où elle a été mise au point. Elle vaut pour tout gros travail à plusieurs discussions. Le détail technique est dans `docs/audits/AUDIT-ULTRA.md` § 13 ; cette page dit l'essentiel en mots simples.

## L'idée
Celui qui fait le travail est le plus mal placé pour le juger. Donc on sépare ceux qui cherchent, ceux qui réparent et ceux qui constatent, et on ne croit que ce qui est prouvé.

## Les dix règles
1. **Trois temps, jamais mélangés** : on teste, on trie, on répare. Une zone n'est jamais testée et réparée en même temps.
2. **Un rôle par discussion** : testeur, réparateur, banc d'essai, lecteur ou vérificateur. Un chef d'orchestre, un seul, à qui tout le monde écrit en une ligne. Les discussions restent muettes.
3. **Une zone, un réparateur** : un fichier n'a qu'un réparateur à la fois. Hors de sa liste de fichiers, on ne touche à rien, on demande.
4. **La phrase d'acceptation** : chaque défaut retenu reçoit une phrase vue de l'utilisateur, avec ses conditions (« en espagnol, main gauche, je touche X et Y s'ouvre »). On répare cette phrase, pas la fiche.
5. **« Corrigé » se mérite** : une réparation n'est dite corrigée que quand un testeur neuf l'a constatée, avec son numéro d'écran, dans les conditions où le défaut a été vu, et AVANT qu'elle entre dans l'appli. Sinon elle est « tentée ».
6. **Un retour va tout de suite au même réparateur**, avec les mots du testeur. On recommence jusqu'à « corrigé ».
7. **Rien n'entre sur une base rouge.** Les réparations entrent une par une par le portail, trois au plus, puis une vérification complète (téléphone simulé compris). Le réparateur lance lui-même le contrôle du portail avant de s'annoncer.
8. **Un rouge se mesure avant de se commenter** : le test, son message, ce qui tournait avant lui. Un test qui tombe dans la suite et passe seul hérite d'un voisin : on répare le voisin. Un défaut de minutage se prouve en forçant l'ordre, puis par deux passages verts.
9. **Un seul compteur**, en trois lignes : « Bugs corrigés dans l'appli », « En salle d'attente », « Bugs encore à corriger ». Chaque point d'étape finit par « ce qui n'est pas vert ».
10. **On audite aussi la méthode.** Après chaque grosse séance, un auditeur indépendant relit la séance elle-même : le temps perdu, ce qui s'est marché dessus, ce qui a été dit sans preuve.

## La clôture
Quand Benjamin dit de finir : la liste des défauts est gelée, les testeurs ne cherchent plus (ils répondent aux phrases, et ne signalent qu'un bloquant ou un grave), et on s'arrête quand la liste est vide. Pas de liste « à faire plus tard ». Ce qu'un testeur simulé ne peut pas juger (le son, le vrai doigt) est nommé, un par un, sur la liste d'essai du téléphone de Benjamin.

## Ce que la méthode a montré le 13 novembre
- Une panne qui aurait fait planter l'appli en Mode simple sur un vrai téléphone, vue par le seul téléphone simulé.
- Des réparations « corrigées » dont un testeur ne constatait qu'une sur huit ; avec la phrase d'acceptation et le contrôle avant l'entrée : cinq sur cinq.
- Une cause de fond (des vérifications qui répondaient dans le désordre) que trois réparations de symptômes n'avaient pas trouvée, et qu'un regard neuf a prouvée en forçant l'ordre.
- Cinq heures perdues en une nuit par des équipes qui se gênaient, vues par l'audit de la méthode.
