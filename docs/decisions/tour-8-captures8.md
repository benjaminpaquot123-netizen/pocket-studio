# Tour 8 · lot captures8 : décisions

- Flèche de rangée : 6 dp tenus dans la rangée, 16 dp dessinés, collés au bord de la rangée (début pour →, fin pour ←) ; raison : ne plus couvrir le texte de la dernière pastille sans pousser les pastilles hors de vue ; retour arrière : revenir à 32222750^.
- Le test lit les bornes de l'icône dessinée (`track_bar_more_icon`), pas la place tenue ; raison : la place tenue (6 dp) cachait le chevauchement « + Spoor » à 130 %.
- Feuille de partage : la marge basse de 12 dp est retirée, et la queue de défilement n'est montrée que si la feuille défile ; raison : bande vide sous « Mes fichiers » et au-dessus de M4A / WAV ; retour arrière : revenir à 4af756e1^.
- Enregistrement complet avec `PS_STALL_MIN=60` ; raison : la suite tourne 55 min sans écrire, la coupure à 20 min n'était pas un blocage.
