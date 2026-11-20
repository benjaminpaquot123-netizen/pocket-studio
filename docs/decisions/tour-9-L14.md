# Tour 9, lot L14 : décisions

- F018 : la feuille « Tonalité du morceau » n'a plus de gamme ni de note choisies d'avance tant que l'élève n'a pas touché ; la place de la ligne de confirmation est réservée (la feuille ne saute plus) ; seule la puce attendue est en pointillés ; les testTags ne sont posés que sur ce qui est visible (TutoKeySheetDoneTest ajusté). Retour arrière : revenir à 0dc71449^.
- F021 : la confirmation « Bravo, c'est réussi ! » n'apparaît qu'au-dessus d'une fenêtre (feuille d'effets, dialogue) quand le tuto est actif ; ailleurs la carte du tuto suffit. Popup en haut au centre, 2,5 s, annoncée aux lecteurs d'écran. Retour arrière : retirer `TutoTickedOverWindow()` de QuickFx.kt.
- F014, F015, F020 : parties hors de mes fichiers demandées au chef (voir rapport), non touchées.
- Risque noté : `WholeWordsText` réduit la taille jusqu'à ce qu'aucun mot ne soit coupé ; un texte dont la première ligne serait vide pourrait finir petit. Non observé, laissé à la relecture.
