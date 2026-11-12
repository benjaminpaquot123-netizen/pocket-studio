# Relecture tour 5, lot « oreille » (questions à l'oreille de BenStudio Test)
Relu : 74a58f3a1339409ee27e7a25dcb38c00545270a7

Portée : `git diff 995f84fc..74a58f3a` (rouge b3838180, vert 35187750, docs 74a58f3a). Les tests `com.pocketstudio.bentest.*` rejoués : verts (Gradle, tâche à jour, BUILD SUCCESSFUL).

## Points demandés
1. Rien ne fuit dans la vraie appli : le diff ne touche ni `app/src/main` ni `build.gradle.kts`. Tout est dans `src/benTest` (compilé dans l'APK seulement avec `-PbenTest`, sinon seulement dans les tests unitaires) et `src/test`. OK.
2. Bandeau : la vue n'est jamais retirée mais n'existe que comme une ComposeView vide (hauteur 0, ne prend aucun toucher) tant qu'aucune question n'attend ; l'état est remis à null dans un `finally` de `Banniere.poser` (fin, délai, exception, fil interrompu). Recréation ou rotation : l'ancienne vue meurt avec son activité, la nouvelle est rattachée à `onActivityResumed` (étiquette de contrôle, pas de doublon). Crash : le processus meurt, la vue aussi. Pas de fuite de fenêtre (vue dans le contenu, pas de WindowManager). OK.
3. Attentes bornées : chaque question a un délai (90 s, 240 s pour le Bluetooth), les étapes ont 600 s et 900 s (plus que l'attente de 300 s du morceau et la question), pas de réponse = étape rouge « pas de réponse », « Passer » = étape réussie. Pas de blocage vu. OK.
4. Q8 : paquet séparé `com.pocketstudio.test`, donc les vraies données de Benjamin ne sont pas touchées. Voir constat 1 pour la valeur de la mesure.
5. Tests : ils vérifient de vrais comportements (suite des 9 étapes, sauts sans casque et sans Bluetooth sans poser la question, preuve d'un ❌ avec capture, journal et mesures, délai, réponse tardive non gardée, rapport et JSON). Pas de test vide.
6. Textes en français, pas de secret, ni audio ni capture commités (fichiers du lot : .kt et .md seulement).

## Rouges constatés
Le commit rouge b3838180 contient des doublures (`etapes()` vide, `saut()` toujours nul, `verdict()` et `section()` vides, `poser()` qui rend « sans réponse », `repondre()` sans effet) en plus de `OreilleTest.kt`. Ce sont de vraies doublures de l'API (les tests rougissent faute de comportement, pas faute de compilation) ; le vert ne change que les corps. Je n'ai pas rejoué le rouge ici.

## Constats (aucun bloquant, aucun grave)
1. Moyen : le contrôle de Q8 est faible. Le glissé est envoyé à la vue racine à x = 2 px : sur l'écran Rythme, les pads ont une marge, donc le toucher n'atteint probablement aucun pad, et aucun glissé sur un pad ne change le pan dans le code lu (le pan se règle dans la fiche du pad). De plus, le robot ne compare pas avant et après : il lit seulement `|pan| < 0,05` après. Un kick déjà décentré donnerait un faux rouge, et un glissé sans effet donne un vert vide. Pas un vrai geste « retour » système non plus (l'événement contourne la navigation par gestes). Le rapport du lot le déclare en partie (« non prouvé sur téléphone »). À durcir au prochain lot (comparer avant et après, ou départ du glissé dans le pad).
2. Faible : le test du pan est une doublure (`kickMoves`) : la lecture réelle `kickAuCentre` et le seuil 0,05 ne sont couverts par aucun test.
3. Faible : le Bluetooth « présent » est seulement `FEATURE_BLUETOOTH` (matériel), pas « un appareil est connecté » ; la question 6 est donc posée sur presque tout téléphone. Le texte demande de brancher soi-même, ce qui est cohérent.
4. Faible : en cas de rotation pendant une question, le bandeau revient mais le mot déjà tapé est perdu (`remember` sans sauvegarde).
5. Faible : `repondre` teste l'état puis dépose la réponse sans verrou (course étroite à l'instant du délai) ; absorbé par `reponses.clear()` au début de la question suivante.

Verdict : 0 bloquant, 0 grave — fusion possible
