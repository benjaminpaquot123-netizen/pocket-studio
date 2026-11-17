# Relecture decoupe2
Relu : e27a5341c
- moyen : CutScreen.kt, `LaunchedEffect(said)` fait défiler la colonne jusqu'en bas pour tout message, pas seulement après un envoi : `message` vit jusqu'à « OK » (StemBoardRouter), donc un message resté d'avant fait descendre l'écran dès l'arrivée en « Un bout » et la consigne mise en haut (D3a) sort de la vue ; une erreur de lecture (« Ce passage n'a pas pu être lu. ») ou une note (« Aucune coupe trouvée ») fait aussi sauter la colonne pendant qu'on tient la courbe. Piste : ne suivre que `busy` puis le message qui le suit, ou ne pas lancer l'effet sur la valeur présente à l'arrivée.
- mineur : en « Phrases » à 100/130 %, les puces passent dans la zone fixe du bas : la liste des bouts perd environ 56 dp (cut_screen.png : une rangée et demie visible) ; en `tight` (< 700 dp) à 130 % en es, si les puces passent sur deux lignes, la liste se réduit encore. Aucun test sur Phrases à 130 % sur petit écran.
- mineur : en « Un bout » à 100/130 %, « Pince pour zoomer » disparaît (prix noté dans les décisions, aide « ? » à compléter : chaîne i18n demandée au chef, non faite).
- mineur : commentaire trompeur dans CutScreen.kt : « 20 dp more, taken from the curve », alors que la réserve passe de 380 à 368 dp, la courbe grandit de 12 dp par rapport à la base.
- mineur : CutLeftThumbD2Test#d3b glisse à des coordonnées fixes (600 dp vers 300 dp), liées au gabarit PHONE ; fragile si le gabarit change. Les assertions des quatre tests, elles, mesurent vraiment (portée du pouce, poignée qui bouge, texte et boutons dessinés entiers).
- mineur : le rapport dit que les 45 autres échecs Roborazzi sont « hors de mon diff » sans l'avoir montré sur la base 7948ecb90 ; à confirmer par le chef au moment du rangement.
- mineur : `view.show(centre = 0.4)` : bornes (coerceIn 0.25..0.75 puis clamp) correctes, appel par défaut inchangé ; poignée « Début » un peu plus à gauche (noté). Rien à corriger.
Verdict : 0 bloquant, 0 grave
