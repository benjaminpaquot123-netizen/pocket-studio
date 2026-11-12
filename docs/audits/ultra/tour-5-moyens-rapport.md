# Lot « moyens » (tour 5) : rapport de zone

Les points moyens laissés ouverts par les relectures du tour 5 (navigation M1, M2 ; plus ; accueil M-a, M-b ; partage ; restes), corrigés par trois équipes (A barre du haut, B page Plus, C accueil et partage), puis le palier « dossier seul » de la barre du haut après la relecture neuve. Décisions : docs/decisions/tour-5-moyens.md. Relecture : docs/audits/ultra/tour-5-moyens-relecture.md.

tests: *TopBarNameRoomTest *.ui.TopBarTest *PartPlayDuringTakeTest *.nav.ModeChoiceTest *ThumbBarBigTextTest *.nav.SpacesTest *TutoBigFontTest *GestureBudgetTest *TwoPlaysNamed* *.nav.PlayThenTabsTest *PlusTilesBigTextTest *PlusWidePaneInListTest *PlusSameInBothModesTest *HomeScreensSayTheyScrollTest *LayoutScanFloatTest *LayoutScanClipTest *ZipSavedWindowTest *ZipSavedLinesTest *AppRootZipSenderTest *QuickStyleCardTest *OneTouchStyleTest *OnboardingUiTest *ScreenScanTest *CaptureMatrix*Test *I18nScreenshotTest *NavScale130ScreenshotTest

chemin: barre du haut, Mode simple à 130 % | test: TopBarNameRoomTest#aRowThatHeldTheNameStaysOneRow
chemin: barre du haut, nom du projet jamais « M… » | test: TopBarNameRoomTest#theModeSimpleBarKeepsAFewWordsOfTheName
chemin: ● prise sur le morceau › « ▶ cette partie » | test: PartPlayDuringTakeTest#duringATakeOnTheSongThePartsPlayStops
chemin: « ▶ cette partie » hors prise | test: PartPlayDuringTakeTest#withoutATakeThePartsPlayStillLoopsThePart
chemin: barre du haut pendant une prise | test: TopBarTest#duringATakeOnTheSongBothPlaysSayStop
chemin: Plus (Simple), tuiles à 130 % | test: PlusTilesBigTextTest#everyTileTitleIsWholeAtTaille130
chemin: Plus (Simple), police du téléphone à 200 % | test: PlusTilesBigTextTest#everyTileTitleIsWholeWithThePhonesFontAt200
chemin: Plus sur tablette en Simple, volet de droite | test: PlusWidePaneInListTest#aProPageLastSeenOpensTheSimpleListsFirst
chemin: accueil, puce « ↓ n autres » | test: HomeScreensSayTheyScrollTest#theWelcomeSaysItScrolls
chemin: « Que veux-tu faire ? », puce | test: HomeScreensSayTheyScrollTest#whatDoYouWantToDoSaysItScrolls
chemin: outil LayoutScan, flottant sur un contrôle | test: LayoutScanFloatTest#aChipOverACardWhollyOnScreenIsFound
chemin: Mes projets › ⋯ › Envoyer le projet, copie ratée | test: ZipSavedWindowTest#noMyFilesWhenTheCopyInDocumentsFailed
chemin: AppRoot › envoi du projet | test: AppRootZipSenderTest#appRootSendsEveryProjectZipThroughTheOneSender
chemin: Nouveau projet › famille « Hip-hop » ouverte | test: QuickStyleCardTest#anOpenedFamilyHeaderReadsWhateverLiesUnderIt
