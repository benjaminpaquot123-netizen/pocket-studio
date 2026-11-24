# Tour 9, lot gr-bass2 : relecture neuve
Relecteur : sous-agent neuf (n'a pas écrit le code), diff ccf50247..746fde43.

relu : 746fde4319cd010c1e11449323fd4a4c94da6255

Vérifié : bass.json valide (9 styles), format identique aux entrées voisines ; StyleRecipes.kt et guides régénérés en local, BassRecipesTest 5/5, HonestMixTest 9/9, RecipeFormatV2Test 6/6, puis remis à l'état commité (rien de généré ne reste modifié). Paramètres lus : cloche basse 0,26 = 120 Hz, Q 0,24 = 0,7 ; cloche kick 0,159 = 60 Hz ; WIDENER 0,25–0,35 = 50–70 % de largeur, graves au centre sous 134 Hz ; EQ jungle 0,42 = −1,9 dB de graves.

1. mineur : gains forts, peu de marge. Basse neurofunk +10,8 dB à 120 Hz (0,8), kick dubstep +9 dB à 60 Hz (0,75), basse jump_up +7,2 dB. Le banc ne signale ni alerte ni saturation, mais le rapport note une sonie plafonnée (limiteur au travail) sur neurofunk, jump_up, riddim : risque de pompage du limiteur et de boue 100–200 Hz si un autre lot remonte encore la basse. À surveiller à l'écoute.
2. mineur : la montée du kick de dnb est à 3,0 dB, pile sur la cible (≥ +3) ; aucune marge face au bruit de mesure.
3. mineur : le test vérifie un plancher (fréquence ≤, gain ≥) mais aucun plafond de gain ; il ne protégerait pas contre une cloche à +18 dB. La largeur n'est vérifiée que sur les « pad » (pas le WIDENER du lead de neurofunk) et ni riddim ni pop.
4. mineur : jungle sort de la liste demandée (son de basse, sub retiré, doublure d'octave retirée, pompe réglée, EQ −1,9 dB de graves) ; c'est justifié et chiffré dans le rapport (Drop repassé au-dessus de la Montée), grave −5,0 toujours ≥ −6, mais c'est un changement de caractère du style à faire écouter.
5. mineur : le test dépend de StyleRecipes.kt régénéré ; sur la tête commitée (fichier généré non régénéré) il échoue. Conforme à la consigne (régénération par le chef à la fusion), à ne pas oublier avant la CI du paquet.

Rapport cohérent avec le diff et les chiffres (grave ≥ −6 sur les quatre styles visés, kick ≥ +3 sur dnb, dubstep, pop, largeur en baisse partout).

Verdict : 0 bloquant, 0 grave
