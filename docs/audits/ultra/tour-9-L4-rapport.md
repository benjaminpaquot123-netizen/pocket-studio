# Tour 9, lot L4 : rapport

Correcteur, branche `eq/tour-9-L4` (base 7455749a). Zone : la grille de Rythme (`rhythm/RhythmScreen.kt`) et, donné par le chef, le « ? » des aides (`ui-components/.../Help.kt`, `HelpDot`). Aucun fichier i18n changé (les mots neufs réutilisent des traductions existantes).

tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.rhythm.Tour9L4*' --tests 'com.pocketstudio.app.audit.ScreenScanTest'

chemin: Rythme, appui long sur un coup : feuille avec « Décalage du coup » | test: Tour9L4HitSheetTest#theLongPressSheetSetsTheHitLate
chemin: Rythme Simple, premier toucher après Force choisi en Pro | test: Tour9L4FirstTouchErasesTest#theSimpleModeErasesOnTheFirstTouchAfterForceWasChosenInPro
chemin: Rythme, nouvelle étape guidée : premier toucher efface | test: Tour9L4FirstTouchErasesTest#aNewTutorialStepErasesOnTheFirstTouch
chemin: Rythme, ▶ de la rangée des mesures | test: Tour9L4BarArrowTest#everyRightArrowOnTheGridLeavesTheHitsWhereTheyAre
chemin: Rythme, « Vider · Mesure N » | test: Tour9L4ClearBarTest#oneTouchEmptiesTheBarShownOnly
chemin: Rythme, « Remplir » pendant une étape guidée | test: Tour9L4FillTest#fillDuringATutorialStepPlacesOnlyItsDottedCells
chemin: Rythme, « Remplir » sur un pad muet | test: Tour9L4FillTest#fillOnAMutedPadSaysWhyNothingIsHeard
chemin: Rythme, pastilles des pads à 200 % | test: Tour9L4BadgesTest#at200
chemin: Rythme, swing réglé au point montré | test: Tour9L4SwingTest#plusAndMinusReachASwingToTheShownPoint
chemin: Rythme en arabe, case 1 et sens | test: Tour9L4RtlGridTest#theArabicGridShowsCellOneAndItsDirection
chemin: Rythme, mots entiers de 100 à 200 % | test: Tour9L4ReadsInFullTest#at200
chemin: Rythme « Tous les pads », anneau guidé et roulements | test: Tour9L4RingRollTest#theRingSkipsACellARollCovers
chemin: Tous les écrans, « ? » d'aide à 130 et 200 % | test: Tour9L4HelpDotTest#at200
chemin: Rythme, rangée des mesures expliquée par un « ? » | test: Tour9L4BarHelpTest#theBarButtonsHaveAHelp

corrige: D04 | gravité: bloquant | rejeu impossible : consigne du chef, preuve par test Compose rouge puis vert
corrige: S56 | gravité: grave | rejeu impossible : consigne du chef, preuve par test Compose rouge puis vert
corrige: S76 | gravité: grave | rejeu impossible : consigne du chef, preuve par test Compose rouge puis vert
corrige: F053 | gravité: grave | rejeu impossible : consigne du chef, preuve par test Compose rouge puis vert
corrige: F047 | gravité: mineur
corrige: F051 | gravité: mineur
corrige: F055 | gravité: mineur
corrige: F056 | gravité: mineur
corrige: F140 | gravité: mineur

## Ce qui a changé
- D04 : l'appui long sur un coup ouvre sa feuille, avec « Décalage du coup ».
- S56 : « Remplir » pendant une étape guidée ne pose que les cases pointées ; F055 : sur un pad muet, il dit pourquoi on n'entend rien.
- S76 : « Vider · Mesure N » vide la mesure montrée, en un toucher.
- F053 : ◀ ▶ de la rangée des mesures changent seulement de page ; aucun coup ne bouge.
- F047, F049/F050 (partie Rythme), F051, F056, F057 (pas fin), F140 : voir les commits « rouge: » / « vert: ».
- `HelpDot` : le « ? » est dessiné en dp dans son rond de 18 dp ; il tient entier jusqu'à 200 %, sur tous les écrans. À 100 % rien ne change (12 dp = 12 sp).
- Recensement des « sans ? » : plafond inchangé (51). Un « ? » « Mesure » (glossaire existant) explique la rangée des mesures, « Vider » et les longueurs.

## Laissés de côté (renvoyés : hors zone)
- F049 (en-tête), F048, F050 (moitié TutoGridLine), F052, F054 : fichiers ouverts ailleurs (PartHeader chez L6/L10, TutoPanel au 2e passage L1).
- F057 : la cible visible reste à faire côté panneau guidé (hors zone).

## Recette pour le téléphone de Benjamin
Rythme, 2 mesures : toucher ▶ de « Mesure » : la page passe à la mesure 2 et aucun coup ne bouge ; appui long sur un coup : « Décalage du coup » apparaît.
