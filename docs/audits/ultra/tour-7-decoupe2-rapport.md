# Tour 7 · decoupe2 : rapport de zone

Défaut : lot 2 D2, la partie qui reste. Phrase d'acceptation : « en es, main gauche, phone à 100 %, Mode Simple, depuis la découpe, je touche les puces « Frases » et la poignée « Fin » sans quitter la zone du pouce gauche [2c #15, #20, #33] ». À ne pas casser : D1 (à 200 %, la courbe et Début/Fin/Écouter visibles ensemble), D7 (puce « Resto » entière à 200 %).

corrige: D2 | gravité: moyen

tests: *CutLeftThumbD2Test *RemixLeftThumbSeatTest com.pocketstudio.app.remix.Cut* *StemBoard*

## Ce qui change (CutScreen.kt, StemBoardModel.kt)
- Frases : les puces des pistes sous la rangée des phrases, en bas (à 100 et 130 %) ; en haut à 200 %.
- Un trozo : les puces sous la rangée du trozo à 100/130 %, en haut à 200 %.
- Le bout choisi se centre à 40 % de la largeur : la poignée Fin tombe dans la portée du pouce gauche.
- Courbe : réserve de 348 dp au lieu de 380 à taille normale.

## Liste des chemins
chemin: Découpe, Frases, puces des pistes (pouce gauche) | test: CutLeftThumbD2Test#d2a_phraseChipsWithinLeftThumb
chemin: Découpe, poignée Fin (pouce gauche, glissée) | test: CutLeftThumbD2Test#d2b_endBarGrabbedWithLeftThumb
chemin: Découpe, Un trozo, puces des pistes (pouce gauche) | test: RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb
chemin: Découpe, Un trozo, Début/Fin/Écouter entiers | test: RemixLeftThumbSeatTest#a5_spanToolsWholeOnArrival
chemin: Découpe, Frases, toucher un morceau de voix | test: RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt

## Preuve (pas de rejeu : le test du banc CutLeftThumbD2Test joue le siège es/phone/100/simple/gauche)
- Commit « rouge: » 8795e92a : d2a et d2b rouges (puces à y≈228–260 dp, poignée Fin à x≈288 dp, hors portée).
- Commit « vert: » 2191598a : d2a et d2b verts ; RemixLeftThumbSeatTest, Cut*, StemBoard* verts.
