# Tour 9, lot « difficiles » : relecture neuve

Relu : 7d3878158 (code relu à 80563220f ; depuis, seuls des documents ont bougé : la ligne de décision passe de docs/DECISIONS.md à docs/decisions/tour-9-difficiles.md, le rapport de zone docs/audits/ultra/tour-9-difficiles-rapport.md est écrit et deux lignes « tenté » (D09, F034) sont ajoutées à docs/audits/ultra/corrections.tsv)

## Ce qui a été relu
- D09, `TutoChecks.copied` : une mesure qui répète une mesure copiée dans le même toucher prend aussi les copies déjà faites comme source (doublons écartés), puis le filtre `fresh` habituel (notes déjà là, longueur du clip). Rien ne change quand la mesure source est entièrement écrite : pas de régression pour les autres chapitres « Copier ».
- F034, `TutoController.showMe` + `GuideNav` « song.make » : le doigt va sur « Ajouter : » seulement pour une étape `part:` sans transposition (donc jamais la basse du Drop 2, qui écrit des notes) et dont le besoin est bien « Créer la partie ». Les autres étapes gardent `goFor`. Le nouveau lieu est dans `SPOTS` et retombe sur les cibles de « song.parts » si « Ajouter : » n'est pas à l'écran.
- p5, `ProjectsViewModel` : toutes les écritures `_state.value = _state.value.copy(...)` passent par `_state.update` (atomique) ; `refreshNow` lit les listes avant la mise à jour. Pas de perte d'état ni de sauvegarde touchée ; les lectures restantes de `_state.value` ne sont que des lectures.
- `HumanTester.waitWhileListLoads` : un simple affichage des fils de l'appli quand la liste ne vient pas ; aucun effet sur le test.
- Tests : T9D09 et T9F034 ont chacun un commit « rouge » avant le « vert » ; leurs assertions lisent le résultat vu (projet changé et étape finie en un toucher ; doigt sous « Ajouter : » et partie « Intro » visible).

## Constats mineurs
- `Coquille2SeatTest` : le test touche maintenant « 自分でやる » pour replier le panneau avant de lire la ligne de la grille. Adaptation du test au panneau ouvert en grand, pas un changement de l'appli ; à garder en tête si un siège signale le panneau qui cache la grille de la Montée.
- `refreshNow` : deux rafraîchissements simultanés peuvent encore se doubler (le plus vieux en dernier), comme avant ; le suivant remet tout d'aplomb.

Verdict : 0 bloquant, 0 grave
