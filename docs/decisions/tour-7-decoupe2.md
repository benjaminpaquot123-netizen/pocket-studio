# Tour 7 · decoupe2 : décisions

- 2026-11-17 : en « Frases », les puces Voz/Batería/Bajo/Resto passent sous la rangée des phrases (en bas, dans la zone du pouce) au lieu du haut de l'écran ; à 200 % elles restent en haut (D1, D7 : la courbe et Début/Fin/Écouter doivent tenir ensemble). Raison : à 100 % elles étaient à y≈228–260 dp, hors de portée du pouce gauche. Retour arrière : revenir le commit « vert: » de ce lot.
- 2026-11-17 : en « Un trozo » à 100 %, les puces passent aussi sous la rangée du trozo, en bas ; à 200 % rien ne change. Raison : même zone du pouce, un seul endroit pour les puces sur l'écran. Retour arrière : idem.
- 2026-11-17 : le bout choisi se centre à 40 % de la largeur (`SPAN_CENTRE`) au lieu de 50 % ; la poignée « Fin » arrive vers x≈220 dp au lieu de 288 dp, dans la portée du pouce gauche. Prix : la poignée « Début » vient plus à gauche (≈67 dp), toujours atteignable, un peu plus loin pour un droitier. Retour arrière : `SPAN_CENTRE = 0.5`.
- 2026-11-17 : la hauteur de la courbe à 100 % se calcule avec 348 dp de réserve au lieu de 380 (bornée 120–180 dp), pour garder la place des puces en bas sans défilement. 200 % inchangé. Retour arrière : 380.
