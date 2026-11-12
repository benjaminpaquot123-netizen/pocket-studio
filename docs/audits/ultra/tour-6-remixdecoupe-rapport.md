# Tour 6 — lot 4 « remix-decoupe » : rapport

Source : tri du tour 6 (`docs/audits/ultra/sieges/tour-6/lots-correction.md`, lot 4 ; `verdicts.tsv`). Base : ccr-80af1a1b-mn0wec, avec les lots langues (ccr-6f42e003-295715) et gaucher (ccr-4cc0bb93-xljmti) fusionnés d'abord. Fichiers touchés : `CutScreen.kt`, `RemixScreen.kt`, `StemBoardRouter.kt`, `SessionScreen.kt`, leurs tests et captures.

tests: com.pocketstudio.app.remix.CutBoutBigTextTest com.pocketstudio.app.remix.CutScrollSignTest com.pocketstudio.app.remix.RemixThumbWordsTest com.pocketstudio.app.remix.StemBoardRouterTest com.pocketstudio.app.remix.StemBoardVmTest com.pocketstudio.app.session.SongCardEmptyWordsTest com.pocketstudio.app.remix.CutScreenTightTest com.pocketstudio.app.remix.CutJourneyTest com.pocketstudio.app.remix.CutSpanJourneyTest

## Corrigés (4 sur 9, plus N14 en partie)
- **R3-EC17, le bloquant** (E c #17, pt-BR, 200 %) : en « Un bout », la courbe ne recevait que la place laissée par la barre d'envoi, dans une colonne qui ne défilait pas : 0 dp à 200 % (430 dp de page), 12 dp à 100 % (480 dp). Sous 700 dp, tout l'écran « Un bout » défile maintenant (puces, courbe à 180 dp, outils, barre d'envoi). Rouge c29ee3f6, vert 5a709f6b.
- **R6, grave** (G a #14) : « N phrases sur les pads 9 à 14 » dit maintenant où sont ces pads, « (Batterie · 16 pads) », avec les mots déjà traduits de l'onglet de la batterie. Rouge aba3a46c, vert 9a1b65b1 (trois anciennes attentes de message mises à jour).
- **N14, en partie** (C b #19, E c #27, doublon de R6) : « Ton morceau est vide » n'est plus dit quand une partie a de la musique ou que le morceau joue (cas de C b #19) ; la carte dit à la place comment la ligne du temps range les parties (phrases déjà traduites). Le cas de E c #27 (une piste audio seule, rien ne joue) reste : la carte ne reçoit pas les pistes audio (`SessionScreenUi`, rempli dans AppRoot.kt, hors de ce lot). Rouge 66194614, vert b20e482d.
- **R3** (B a écran 28) : en mode « Phrases », la colonne qui défile s'estompe en bas tant qu'il reste des outils dessous (le fondu des listes du tour 5). Rouge bfa80380, vert e32e4d49.
- **R1, sa part dans ces écrans** (E c #5, #17) : le titre « Cortar a música » passe sur deux lignes au lieu de « Cortar a mú… », les mots des boutons d'envoi et de la rangée du pouce ne sont plus coupés (« Guard / ar », « Baixar o modelo (174 »). Rouges 50370fe0 et 628cb6e4, verts c60d404a et f6fecec7.

## Laissés de côté
- **R7** (B a #18, #21, main gauche) : les puces Voix/Batterie/Basse/Reste en haut de l'écran de découpe restent hors du pouce gauche ; les mettre en bas est une refonte de cet écran, à décider avec la maquette (la carte Simple/Pro, partie grave de R7, est déjà réglée par le lot gaucher).
- **R5** (boutons en double) : la rangée du pouce répète volontairement le bouton de la carte (tour 1, n° 21, main gauche) ; ôter l'un ou l'autre casserait la règle du pouce ou une trentaine de tests et parcours du robot. Décision dans `docs/decisions/tour-6-remixdecoupe.md`.
- **U29** (A a, hésitation) : la rangée « Ajouter à la fin » sous 26 parties. Le test essayé ne mesurait pas la page (position rendue à 0 dp) : rien de prouvé, rien changé.
- **U13** (E c #24, doublon de R3) : probablement couvert par le défilement de R3-EC17 (la barre d'envoi ne prend plus la place des puces), mais aucun test ne le prouve à part.

- **Seuil de 700 dp** (relecture, moyen) : il ne tient pas compte de la taille du texte ; à 100 % entre environ 650 et 700 dp de page, la barre d'envoi passe sous le pli (on la trouve en faisant défiler, le fondu le dit). Laissé : un seuil par mesure du contenu est une autre refonte de l'écran.

## Captures regardées
`app/screenshots/remix/cut_bout_short_fr_100.png`, `…_fr_130.png`, `…_ptbr_200.png` (nouvelles) ; `remix_first.png` (bouton du pouce, différence au pixel près), `folded_cards.png` et `scene_drag.png` (la carte du morceau, phrase de N14) réenregistrées après les avoir regardées. Les captures `cut_bout*`, `cut_screen*` existantes n'ont pas bougé. `sizes/compact_cut_structure.png` (titre sur deux lignes) et `sizes/expanded_cut_structure.png` (fondu du bas sur « Tes bouts » tant que la colonne n'a pas défilé) changent avec ce lot : réenregistrées après les avoir regardées. Le recensement des écrans (`ScreenScanTest`) accepte les trois écrans neufs avec leur raison (`recensement-acceptes.tsv` : mêmes onglets sans « ? » que cutScreenOnePiece ; à 100 %, « Écouter le bout » à demi sous le fondu). `RemixChantier10Test#eachFixOfTheCutEndIsOneTouch` échoue sous `verifyRoborazzi` parce qu'il compare une image du dossier de construction (`build/chantier10/share_overrun_pro.png`) absente sur cette machine ; il ne touche aucun écran de ce lot (non rejoué avant le lot).

chemin: Remix › Découper la chanson › Un bout (petit écran, 100 %) | test: CutBoutBigTextTest#aPieceAtTheUsualTextOnAPhone
chemin: Remix › Découper la chanson › Un bout (pt-BR, 200 %) | test: CutBoutBigTextTest#aPieceAtTwiceTheTextOnAPhone
chemin: Remix › Découper la chanson › titre et boutons d'envoi (pt-BR, 200 %) | test: CutBoutBigTextTest#theTitleAndTheSendButtonsReadWholeInPortugueseAtTwiceTheText
chemin: Remix › Découper la chanson › Phrases (petit écran) | test: CutScrollSignTest#aSignSaysTheToolsAreBelow
chemin: Remix › rangée du pouce › Télécharger le modèle (pt-BR, 200 %) | test: RemixThumbWordsTest#theModelsButtonReadsWholeInPortugueseAtTwiceTheText
chemin: Remix › Découper › Sur les pads (message) | test: StemBoardRouterTest#piecesOnPadsSayTheyAreOnTheDrumsSixteenPads
chemin: Remix › Découper en phrases (message) | test: StemBoardRouterTest#theVoiceCutIntoItsPhrasesOnFreePadsInOneStep
chemin: Mon morceau › carte « Le morceau » (de la musique, aucune partie rangée) | test: SongCardEmptyWordsTest#aPartWithABeatPlayingInALoopIsNotAnEmptySong
chemin: Mon morceau › carte « Le morceau » (aucune musique) | test: SongCardEmptyWordsTest#noMusicAnywhereIsAnEmptySong
