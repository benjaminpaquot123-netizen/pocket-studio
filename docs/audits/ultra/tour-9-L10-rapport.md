# Tour 9, lot L10 « divers » : rapport

Branche : eq/tour-9-L10, partie de l'intégration ccr-80af1a1b-mn0wec (tête c79d3442).
Défauts visés : F139, N1, N2, N3. Tous « tentés », aucun « corrigé » (un testeur neuf le dira).

tests: :app:testDebugUnitTest --tests '*FitTextBox*' --tests '*CutStaleMessage*' --tests '*CutCurrentMessageScroll*' --tests '*CutBoutBigText*' --tests '*LiveArabicTitles*' --tests '*Tour9TabsWhole*' --tests 'com.pocketstudio.app.live.*' --tests 'com.pocketstudio.app.remix.*' --tests '*Scene*' --tests '*WholeWords*'
chemin: Découpe, message ancien laissé sous un travail, phone 411 dp, 200 % | test: CutCurrentMessageScrollTest#theOldMessageLeftUnderAWorkDoesNotPullTheColumnDown
chemin: Découpe, message neuf, phone 411 dp, 200 % | test: CutCurrentMessageScrollTest#aNewMessageStillBringsTheColumnDown
chemin: texte ajusté, case basse (fr) | test: FitTextBoxTest#aFittedWordInALowBoxStaysInside
chemin: texte ajusté, case basse (ar) | test: FitTextBoxTest#aFittedArabicWordInALowBoxStaysInside
chemin: texte ajusté, case étroite (fr) | test: FitTextBoxTest#aFittedWordInANarrowBoxStaysInside
chemin: texte ajusté, case étroite (ar) | test: FitTextBoxTest#aFittedArabicWordInANarrowBoxStaysInside
chemin: texte ajusté, deux lignes (fr) | test: FitTextBoxTest#twoFittedLinesInASmallBoxStayInside
chemin: texte ajusté, deux lignes (ar) | test: FitTextBoxTest#twoFittedArabicLinesInASmallBoxStayInside
chemin: tuile de scène réduite plus large que sa case (garde) | test: FitTextBoxTest#aShrunkContentWiderThanItsBoxStaysInside
chemin: Un trozo (es, main gauche, phone 100 %, Simple), message resté d'avant (tour 8, gardé) | test: CutStaleMessageTest#staleMessageKeepsTheInstructionOnTop
chemin: Live, titres des briques, ar, 360 dp, 200 % | test: LiveArabicTitlesTest#arabic
chemin: Live, titres des briques, fr, 360 dp, 200 % | test: LiveArabicTitlesTest#french
chemin: Structure, onglets, tablette, 100 % | test: Tour9TabsWholeTest#tablet100
chemin: Structure, onglets, tablette, 130 % | test: Tour9TabsWholeTest#tablet130
chemin: Structure, onglets, tablette, 200 % | test: Tour9TabsWholeTest#tablet200

## Ce qui change
- F139 (tenté) : la Découpe ne descend plus vers un message déjà là à l'arrivée, même s'il revient après un travail ; un message neuf la fait toujours descendre (`remix/CutScreen.kt`). Rouge vu avant (scroll 1910 au lieu de 0).
- N2 (tenté) : un texte ajusté qui ne tient pas au plus petit pas permis descend encore (jusqu'à 1 sp) au lieu de déborder en haut (`FitText.kt`, `getFontSize`). Côtés : déjà tenus (le texte est coupé au bord de sa case), garde seulement ; la réduction en largeur de `shrinkToFit` est retirée après relecture (aucun rouge ne la prouvait).
- N3 (tenté) : le compteur de mesures du Live (« مازورة », « mesure ») passe par le texte ajusté et tenait mal ; il tient avec N2. Rouge prouvé en remettant le `FitText.kt` de la base (46 px dans 25 en ar, 34 dans 25 en fr).
- N1 : non reproduit à neuf (les onglets de la Structure se lisent en entier sur tablette à 100, 130 et 200 %) ; le test reste en garde. La coupe du siège venait de la colonne de session défilée (hors de mes fichiers, voir Demande).

## Ce qui n'est pas vert
- Relecture (moyen) : au plancher de 1 sp, un texte reste entier mais peut devenir illisible (sous-titre du compteur du Live à 200 %) ; le vrai remède est plus de place, lot `live/*`.
- Captures I18nArabic / PseudoRtl : rouges dès la base c79d3442, pas cassées par ce lot.

## Demandes (hors de mes fichiers)
Demande : N1, la coupe vue par le siège vient de la colonne de session défilée (`session/SessionScreen.kt`, `ui/AppRoot.kt`) : à rejouer dans un lot de cette zone.
Demande : N3, d'autres titres du Live débordent en ar à 360 dp et 200 % (`live/LiveDock.kt:70` libellé BPM 88 px dans 35 ; aides « ? » 88 dans 54 ; `live_quantum` « مازورة واحدة » 132 dans 126 ; `live_cases` « الخانات » 132 dans 126) : lot `live/*`.

## Captures regardées (écran entier)
- Live, 360 dp, 200 %, ar et fr : briques, compteur et lignes dans leurs cases ; le sous-titre du compteur (« مازورة », « mesure ») est très petit mais entier. Restent coupés, hors de mes fichiers : « BPM » sous le tempo (Demande N3).
- Découpe (`cut_screen`, `cut_bout`, `cut_bout_130`), Live (`live_grid`) et session (`session`) réenregistrées : aucune image de référence ne change.
- Tablette, Structure (`tablet/nav_structure_parts`) : onglets « Ligne du temps » et « Parties » entiers.
