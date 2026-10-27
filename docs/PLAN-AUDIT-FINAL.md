# Le grand audit final (demande de Benjamin, 2026-10-27)

> « Une fois que tout est terminé et sur GitHub, prévois un énorme audit avec un beau panel. L'objectif est de trouver toutes les imperfections du programme, quel que soit le type. » — « N'hésite pas à tout mettre en même temps, pas de limite. »

## But
Trouver **toutes** les imperfections de Pocket Studio, quel que soit leur type : bugs, plantages, pertes de données, sons ratés, gestes pénibles, textes faux, lenteurs, trous de tests, fragilités du code. Puis corriger tout ce qui est grave et moyen, et vérifier.

## Le panel (tous en même temps)
Chaque auditeur lit le code et les captures, fait tourner ce qu'il peut sur la machine (tests Robolectric, banc de son, rendus hors ligne, captures), et rend des constats précis : où, comment le reproduire, gravité, correction proposée.

**Utilisateurs (parcours réels, captures et tests Robolectric)**
1. Débutant total, téléphone, « un morceau pour une soirée » (house).
2. Débutante, « remixer une chanson » (séparateur de pistes).
3. Ado, trap / drill, veut enregistrer sa voix.
4. Débutant bass music, jump up (le style de Benjamin), suit le Guide jusqu'au bout.
5. Débutante lo-fi, Mode simple, reprend son morceau des jours après.
6. Musicien confirmé, va vite : clavier, notes, couches, mixeur, automation, export.
7. Producteur live : page Live, pads, enregistrement de la performance.
8. Utilisateur du Mode simple uniquement.
9. Tablette Galaxy Tab A11, debout et couchée, 100 % et 130 %.

**Pros du son (banc, rendus, mesures, extraits)**
10. Bass music (DnB, jump up, dubstep, riddim, neurofunk) : basses, leads, Drops, largeur, téléphone.
11. Techno / house / trance / acid : groove, contraste des parties, 303, largeur.
12. Rap / trap / lo-fi / afro / pop : 808, voix, accords, mix.

**Experts techniques**
13. Moteur audio temps réel : allocations, verrous, NaN, dénormaux, craquements, CPU, latence.
14. Sauvegarde et données : écriture atomique, vieux projets, migration, export/import zip, perte possible.
15. Plantages et robustesse : projets bizarres, gestes rapides, rotation, appli tuée, mémoire basse, permissions refusées.
16. Performance : fil principal, recompositions, mémoire, rendus hors ligne, petits téléphones.
17. Qualité du code : code mort, duplication, fichiers géants, fragilités.
18. Qualité des tests : ce qui n'est pas testé, tests faibles ou instables.
19. Sécurité et vie privée : fichiers, partages, journaux, données envoyées, permissions.

**Forme**
20. Design et ergonomie : cohérence, densité, gestes, 130 %, captures.
21. Textes : français, anglais, néerlandais (ton, fautes, jargon, traductions manquantes).
22. Accessibilité : TalkBack, contrastes, tailles de cible, ordre de lecture.
23. Le Guide : aide-t-il vraiment ? Trop bavard, pas assez, faux, redondant ?

## Après les constats
- **Vérification** : chaque constat grave ou moyen est revu par un sceptique indépendant (il essaie de le réfuter) ; seuls les constats confirmés restent.
- **Synthèse** : `docs/AUDIT-FINAL.md`, classée par gravité (grave / moyen / mineur) et par thème, chaque ligne avec son auditeur et sa preuve.
- **Corrections** : tout le grave et le moyen, par lots indépendants en parallèle ; le mineur noté dans `docs/REVIEW.md`.
- **Seconde passe** : relecture neuve des corrections, tests complets, captures regardées.
- **Fin** : un seul APK compilé ici, avec un résumé court pour Benjamin et les extraits audio si le son change.

## Contraintes
- La CI de GitHub est en pause (demande de Benjamin) : tout se teste sur la machine de l'agent ; fusion avec « [skip ci] ».
- Pas d'émulateur ici : Robolectric, tests hôte, bancs de son. Ce qui demande un vrai téléphone est listé pour Benjamin.
- Le chantier Play Store n'est pas dans cet audit (sur demande de Benjamin plus tard).
