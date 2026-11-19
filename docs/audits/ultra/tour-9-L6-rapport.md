# Lot L6 (tour 9) : Structure, barre du haut, bande de lecture

Base : 7455749a5 · branche `eq/tour-9-L6`. Fichiers de l'appli : `session/SessionScreen.kt`, `ui/TopBar.kt`, `ui/PartHeader.kt`, `ui/PlayStrip.kt`, `ui/DoneBar.kt`. Décisions : `docs/decisions/tour-9-L6.md`.

tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.session.Tour9*' --tests 'com.pocketstudio.app.ui.Tour9*' --tests 'com.pocketstudio.app.ui.PlayStripLogicTest' --tests 'com.pocketstudio.app.SongArrangementTest'
chemin: Structure, ajouter une partie garde la liste en place | test: Tour9AddKeepsPlaceTest#aSmallPhoneKeepsTheButtonUnderTheFinger
chemin: Structure, titre « Ajouter », boutons et blocs lisibles en entier | test: Tour9StructureLabelsTest#theNewPartButtonsReadWholeInArabic
chemin: Structure, gestes (appui long, toucher une partie, liste qui défile) | test: Tour9StructureGesturesTest#aLongPressOnAnAddButtonOpensItsMenuWithoutAdding
chemin: Structure, phrase d'aide visible quand le tuto montre les boutons d'ajout | test: Tour9HelpUnderTopTest#theHelpStaysInSightWhenTheTutoShowsTheAddButtons
chemin: Barre du haut, nom du projet et « Sauvegardé » | test: Tour9TopBarSavedTest#theNameAndSavedFitTheBar
chemin: Barre du haut, menu de la partie « Renommer » | test: Tour9RenameFromChipTest#theSceneMenuOffersRenameAndOpensTheNamingDialog
chemin: Bande de lecture, arrêtée : dit où ▶ démarre | test: Tour9PlayStripTest#stoppedItSaysWhereThePlayButtonStarts
chemin: Bande de lecture, mesures lisibles à 200 % | test: Tour9PlayStripTest#theBarsReadWholeWithBigText
chemin: Message « Fait » qui s'en va au geste suivant | test: Tour9DoneBarGoesTest#theNextTouchOnThePageLetsTheLineGoAndStillWorks

corrige: D08 | gravité: grave | rejeu impossible : le self-test du banc était rouge, les rejeux n'ont pas été joués ; preuve par Tour9AddKeepsPlaceTest et les captures
corrige: F100 | gravité: moyen
corrige: F102 | gravité: moyen
corrige: F103 | gravité: moyen
corrige: F104 | gravité: moyen
corrige: F105 | gravité: moyen
corrige: F108 | gravité: moyen
corrige: F109 | gravité: moyen
corrige: F110 | gravité: moyen

Tests ajustés par « corrige-les tous » : LanguageTest.kt ligne 110 (la phrase d'aide de la Structure a changé, F104) ; recensement-acceptes.tsv, 4 lignes HELP de Tour9L6ScreenshotTest (la phrase d'aide explique ▶ et le toucher d'une partie : pas de « ? » à côté).

Demande accordée : Zone3JourneysTest.kt : F103 retire la copie d'une partie vide (lignes 98-99, `add_part_new_copy` devient `add_part_new_empty`).

## Restes
- F107 : moitié seulement. Le nom reste dans la ligne et peut être raccourci (« Mon prem… ») ; « Sauvegardé » est essayé plus petit, puis la coche seule (le mot reste dit à TalkBack). Le nom sur sa propre ligne prenait de la hauteur sur tous les écrans (touches coupées, étiquettes couchées).
- F101 : non reproduit côté Structure. Sur t9-53 écran 47, le doigt (270, 520) est dans une bande vide entre l'aide des répétitions (435) et l'en-tête du tuto (631) : les boutons d'ajout sont cachés par une couche invisible du tuto. Zone L1.
- F111 : `DoneBar` prête (`doneGoesAtNextGesture`), la ligne d'AppRoot.kt attend l'entrée de L1.
- D10 : tenté. Le menu « A ▾ » sait renommer (Tour9RenameFromChipTest), mais l'entrée « Renommer » reste cachée tant que la ligne `renameScene = rhythmVm::renameScene` n'est pas posée dans AppRoot.kt (hors du lot, refusée deux fois), à faire par le chef.
- F105 : moitié seulement. Les mesures se lisent en entier ; nommer la partie éditée demande que ▶ y démarre (RhythmViewModel, hors du lot, relecture G1).
- F106 : moitié seulement. La phrase d'aide reste visible avec 3 ou 4 parties (Mode simple, texte normal ou 200 %) ; avec 8 parties en Mode avancé, les boutons d'ajout remplissent la place au-dessus du panneau et la phrase sort par le haut. La garder avec eux (`revealFrom`) cassait le tuto « jump_up » Facile (cible absente après « Montre-moi »).
