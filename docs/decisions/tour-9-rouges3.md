# Décisions du lot « rouges3 » (tour 9)

- 2026-11-21 : `ScaledWindowsTest` rouge (L14, F021) : la petite fenêtre « étape cochée » passe par `ScaledPopup` (nouvelle surcharge avec un placement), comme toutes les fenêtres. Retour arrière : annuler a49ba41f2.
- 2026-11-21 : `TutoPanelFitsBigFontTest` fr_5 rouge (L13, plafond 60 % du panneau) : à 200 % sur 360 dp, sans offre de kit, la consigne n'avait plus 0 px ; ce qui manque à sa ligne et demie relève le plafond (dans 85 %), comme déjà avec l'offre de kit. Choisi plutôt que de faire disparaître la tête du panneau (le bouton « replier » serait parti sans moyen de revenir). Retour arrière : annuler dfb7f05ff.
- 2026-11-21 : `T9PanelTargetTest` fill rouge (depuis L14) : à très grande police, la mise en vue ne ramène plus que la ligne du tuto juste au-dessus des cases (pas les pages de mesures sur deux rangs, trop hautes : la vue gardait leur haut et les cases repassaient sous le panneau). Retour arrière : annuler 870e384bc.
- 2026-11-21 : `Coquille2SeatTest#cymbalGridLineNamesThePadShown` : déjà vert sur l'intégration f6d0a30b4 (lot « difficiles », b122cb4c1) ; rien changé ici.
