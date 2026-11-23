# Tour 9, zone robot1 (lot A1) : décisions

- C8 : le scénario « partie_montee » lit la courbe de montée (automation LIVE_BUILD_UP) au lieu d'une partie en plus ; « Annuler » doit la retirer. Raison : une Montée n'ajoute jamais de partie (`buildUpBefore`). Retour arrière : revenir au geste `partie:part_buildup_4:1`. Preuve : `ScenariosVraisEcransTest#aBuildUpAddsItsCurveNotAPartThenUndoTakesItBack` (téléphone et tablette), vert après le commit wip 70b0edeb5.
