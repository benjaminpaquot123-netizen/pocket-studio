# Tour 9, zone robot1 (lot A1) : décisions

- C8 : le scénario « partie_montee » lit la courbe de montée (automation LIVE_BUILD_UP) au lieu d'une partie en plus ; « Annuler » doit la retirer. Raison : une Montée n'ajoute jamais de partie (`buildUpBefore`). Retour arrière : revenir au geste `partie:part_buildup_4:1`. Preuve : `ScenariosVraisEcransTest#aBuildUpAddsItsCurveNotAPartThenUndoTakesItBack` (téléphone et tablette), vert après le commit wip 70b0edeb5.
- C11 (tenté) : le robot lit les cases pleines du Live dans le modèle (`live_pad_<piste>_<rang>`, comme la grille les marque) au lieu de l'ancien nom `live_pad_<i>` ; « pad N » vise la N-ième case pleine, « non mesuré » si elle n'existe pas ou n'est pas à l'écran. Retour : revenir aux balises `live_pad_<i>` si la grille les reprend.
