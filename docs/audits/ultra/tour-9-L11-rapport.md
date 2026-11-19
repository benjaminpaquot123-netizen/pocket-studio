# Tour 9, lot L11 : rapport

Branche : eq/tour-9-L11, sur l'intégration ccr-80af1a1b-mn0wec (fusionnée, L10 compris).
Défauts visés : F131, D17, et les restes de L10 (N1, N3 avec le compteur, test du vrai chemin de F139). Tous « tentés », aucun « corrigé » (un testeur neuf le dira).

tests: :app:testDebugUnitTest --tests '*T9F131*' --tests '*T9L11*' --tests '*T9D17*' --tests '*CutLocalNoteScroll*' --tests '*CutCurrentMessageScroll*' --tests 'com.pocketstudio.app.live.*' --tests '*Help*' --tests '*I18nCatalogTest*' --tests '*SpacesTest*' --tests '*WholeWords*' --tests '*Live*'
chemin: Accueil du projet, tuto replié, Retour | test: T9F131BackUnfoldsTutoTest#backUnfoldsTheFoldedTutoBeforeLeavingTheProject
chemin: tuto uk_garage, kit téléchargé mais illisible | test: T9L11KitErrorSaidTest#theKitThatCannotLoadIsSaidAndCanBeTriedAgain
chemin: tuto uk_garage, erreur du kit, « Plus tard » | test: T9L11KitErrorSaidTest#laterPutsTheErrorAway
chemin: tuto, kit du pack décodé comme sur le téléphone | test: T9D17KitDisappearsTest#a_withThePhonesDecoderTheKitStays
chemin: tuto, kit du pack avec le vrai décodeur de Robolectric (erreur dite) | test: T9D17KitDisappearsTest#b_withTheRealDecoderUnderRobolectricTheKitThatCannotLoadIsSaid
chemin: Structure, bascule Timeline / Parties, tablette 8, 100 %, Pro, id, colonne défilée | test: T9L11SwitchStaysWholeTest#theStructureSwitchStaysWholeWhenThePartsScroll
chemin: Live, barre et rangée du haut, ar, 360 dp, 200 % | test: T9L11LiveBarWholeTest#arabicAt200
chemin: Live, barre et rangée du haut, fr, 360 dp, 200 % | test: T9L11LiveBarWholeTest#frenchAt200
chemin: Live, barre et rangée du haut, fr, 360 dp, 130 % | test: T9L11LiveBarWholeTest#frenchAt130
chemin: Live, compteur à la plus petite police, de, 360 dp, 85 % | test: T9L11LiveBarWholeTest#germanAtTheSmallestSize
chemin: Découpe, note « Aucune coupe trouvée » puis touche sur la courbe (vrai chemin de F139) | test: CutLocalNoteScrollTest#theOldMessageBackUnderTheNoteDoesNotPullTheColumnDown

rejeu: t9-75-kit | siège t9-75 (nl, tablette 8, 100 %, Pro, tuto uk_garage ch. 1) | tenu : la carte du kit charge le kit (« Kit: UK Garage », sans erreur) ; l'erreur dite reste couverte par T9L11KitErrorSaidTest
corrige: D17 | gravité: grave (douteux banc) | rejeu: t9-75-kit

## Ce qui change
- F131 (tenté) : Retour déplie d'abord le panneau du tuto replié (puis ferme la feuille ou l'éditeur ouvert) ; il ne sort du projet qu'au dernier Retour (`ui/AppRoot.kt`).
- D17 (tenté) : un kit téléchargé mais illisible le dit (« Le kit n'a pas pu se charger… ») avec « Réessayer » et « Plus tard », au lieu de disparaître sans un mot ; chaînes dans toutes les langues. Le test b de L9 (qui gardait le silence) est inversé et renommé, avec l'accord du chef.
- N1 (tenté) : la bascule « Timeline | Parties » sort de la colonne des parties qui défile ; elle reste entière en haut de l'espace Structure (`ui/AppRoot.kt`, seulement la bascule). Rouge vu avant : onglets coupés après un glissé de 40 px.
- N3 (tenté) : dans la barre du Live, « BPM », l'aide « ? », « Lancer à » et « Cases » ne sont plus coupés : hauteurs minimales au lieu de fixes, « Lancer à » et sa valeur passent à la ligne quand ils ne tiennent pas, la rangée au-dessus de la grille mesure sa vraie hauteur et la grille la lui laisse ; le cercle de l'aide grandit avec son « ? » (`ui-components/Help.kt`). Le compteur de mesures s'élargit avec la police (jusqu'à 1,5 fois) : ses chiffres ne descendent plus sous 9 dp.
- F139, vrai chemin (garde) : le test passe sur la base (correction de L10) et devient rouge (la colonne descend de 1910 px) quand on retire de `CutScreen.kt` la comparaison au message trouvé à l'arrivée. Pas de rouge à faire, aucun changement de `CutScreen.kt`.

## Ce qui n'est pas vert
- Une amenée « tuto:gabber:7 » a échoué une fois au hasard (« les chapitres n'est jamais venu », faute connue du banc, voir BenchAmeneeLateKitCardTest) ; relancée sans rien changer, elle passe.
- Après la fusion de l'intégration (L8 compris), des captures hors de mes écrans ne collent plus à leurs références : feuille du tempo (`tempo_sheet`, `nl_tempo_sheet`), barre des pistes (`tracks`), et sans doute les écrans Sons (`nav_sons`, `nav_sons_synth`, `nav_simple_keys`, `nav_simple_send`, `sons_synth_take`, et leurs `land_*`). Prouvé avec le code de la base seule pour `tempo_sheet`, `nl_tempo_sheet` et `tracks` : c'est L8 (TrackBar et TempoSheet changés sans nouvelles références). Pas réenregistrées ici, ce ne sont pas mes écrans.

## Demandes (hors de mes fichiers)
Aucune.

## Captures regardées (écran entier)
86 captures changent, toutes sur mes écrans : le Live (barre, rangée du haut, grille), la Structure en Parties (bascule sortie de la colonne, N1) et le cercle « ? » à 130 % et plus (Découpe, feuille du tempo). Regardées en entier : Live arabe 360 dp (`i18n/ar_live_fx`), Live Simple 100 % (`nav/live`), Live 130 % tablette (`tabA11/130_nav_live`), Structure Pro (`tabA11/nav_structure_parts`), feuille du tempo 130 % (`lying/tempo_sheet_text130`), Découpe 130 % (`cut_bout_130`), Guide sur la Structure (`guide_spot`). Rien de coupé, rien qui chevauche ; à 100 % le cercle « ? » est identique au pixel près (18 dp).
