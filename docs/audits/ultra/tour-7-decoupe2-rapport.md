# Tour 7 · decoupe2 : rapport de zone

Défaut : lot 2 D2, la partie qui reste. Phrase d'acceptation : « en es, main gauche, phone à 100 %, Mode Simple, depuis la découpe, je touche les puces « Frases » et la poignée « Fin » sans quitter la zone du pouce gauche [2c #15, #20, #33] ». À ne pas casser : D1 (à 200 %, la courbe et Début/Fin/Écouter visibles ensemble), D7 (puce « Resto » entière à 200 %).

Défaut ajouté par le chef : lot 2 D3, la partie Remix (siège 2c, es, phone 100 %, simple, gauche). Phrase d'acceptation : « la consigne de la Guía (es) n'est pas coupée : dans la découpe, « Coloca Inicio y Fin… » et les boutons « En los pads… » visibles, et ils restent visibles après le message [2c #9, #16] ».

corrige: D2 | gravité: moyen
corrige: D3 | gravité: moyen

tests: *CutLeftThumbD2Test *RemixLeftThumbSeatTest com.pocketstudio.app.remix.Cut* *StemBoard*

## Ce qui change (CutScreen.kt, StemBoardModel.kt)
- Frases : les puces des pistes sous la rangée des phrases, en bas (à 100 et 130 %) ; en haut à 200 %.
- Un trozo : les puces sous la rangée du trozo à 100/130 %, en haut à 200 %.
- Le bout choisi se centre à 40 % de la largeur : la poignée Fin tombe dans la portée du pouce gauche.
- Courbe : réserve de 348 dp au lieu de 380 à taille normale (puis 368 avec D3).
- D3 : en Un trozo à 100/130 %, la consigne « Coloca Inicio y Fin… » passe au-dessus de la courbe (à la place de « Pellizca para ampliar ») ; à 200 % elle reste dans la barre d'envoi.
- D3 : quand un message arrive (« Listo… »), la colonne descend jusqu'au bas : le message et les quatre boutons restent visibles.

## Liste des chemins
chemin: Découpe, Frases, puces des pistes (pouce gauche) | test: CutLeftThumbD2Test#d2a_phraseChipsWithinLeftThumb
chemin: Découpe, poignée Fin (pouce gauche, glissée) | test: CutLeftThumbD2Test#d2b_endBarGrabbedWithLeftThumb
chemin: Découpe, Un trozo, puces des pistes (pouce gauche) | test: RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb
chemin: Découpe, Un trozo, Début/Fin/Écouter entiers | test: RemixLeftThumbSeatTest#a5_spanToolsWholeOnArrival
chemin: Découpe, Frases, toucher un morceau de voix | test: RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt
chemin: Découpe, Un trozo, consigne entière à l'arrivée | test: CutLeftThumbD2Test#d3a_spanInstructionWholeOnArrival
chemin: Découpe, Un trozo, En los pads puis message, boutons entiers | test: CutLeftThumbD2Test#d3b_sendButtonsStayAfterMessage

## Preuve (pas de rejeu : le test du banc CutLeftThumbD2Test joue le siège es/phone/100/simple/gauche)
- Commit « rouge: » 8795e92a : d2a et d2b rouges (puces à y≈228–260 dp, poignée Fin à x≈288 dp, hors portée).
- Commit « vert: » 2191598a : d2a et d2b verts ; RemixLeftThumbSeatTest, Cut*, StemBoard* verts.
- D3, commit « rouge: » bbd9f7ee : d3a rouge (consigne à 0 dp de 40 dp, sous le bas de l'écran), d3b rouge (après le message, « En los pads » à y≈632–636 dp, sous la barre Guía).
- D3, commit « vert: » 1256a9f5 : d3a et d3b verts ; d2a, d2b, RemixLeftThumbSeatTest, tous les tests remix et StemBoard* verts.

## Captures (verifyRoborazziDebug, 2026-11-17)
- 59 échecs : 13 captures de la découpe (cut_*, sizes/*_cut_structure), changées par D2/D3, ré-enregistrées et regardées (100/130/200 %, écran entier) ; 1 RemixChantier10Test (référence dans build/, absente en mode vérification, pas une capture du dépôt).
- 45 autres (projets, piano roll, prise, partage, tablettes…) : hors de mon diff (seuls CutScreen.kt et StemBoardModel.kt changent, `show()` garde son comportement par défaut) ; laissés, pas à ce lot.
