# Tour 9, micro-lot D10 : rapport

Défaut D10 (grave) : « Depuis le menu « A ▾ » je peux renommer la partie, comme le tuto le demande. »

Cause : `TopBarActions.renameScene` restait `null` (AppRoot ne le passait pas), donc l'entrée « Renommer » du menu « A ▾ » était cachée.

Correction : `AppRoot.kt`, appel `TopBarActions(...)` : `renameScene = rhythmVm::renameScene` (le même renommage que Structure).

- rouge : 7d1bde54 (test seul, rouge sur la base : « Renommer » absent du menu de l'appli entière)
- vert : 7e381961 (une ligne dans AppRoot)

tests: com.pocketstudio.app.ui.Tour9RenameWiredTest com.pocketstudio.app.ui.Tour9RenameFromChipTest com.pocketstudio.app.ui.TopBarTest

chemin: barre du haut, menu « A ▾ » de l'appli entière (câblage d'AppRoot) | test: Tour9RenameWiredTest#theAppsSceneMenuOffersRename
chemin: menu « A ▾ » → « Renommer » → dialogue « Nommer la partie » → nom reçu | test: Tour9RenameFromChipTest#theSceneMenuOffersRenameAndOpensTheNamingDialog
chemin: nom d'une partie en italien gardé tel qu'enregistré | test: Tour9RenameFromChipTest#inItalianAnUnchangedNameIsKeptAsSaved

Limite : le test d'appli entière s'arrête à l'entrée du menu (sous Robolectric, un dialogue à champ texte ne devient jamais inactif) ; la suite du chemin est couverte par Tour9RenameFromChipTest. Statut : tenté (aucun testeur neuf ne l'a encore constaté).
