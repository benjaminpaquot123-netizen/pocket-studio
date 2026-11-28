# Relecture tour-9 D10
Relu : d2e15849

Périmètre : de 7d1bde54^ à d2e15849. Trois fichiers bougent : AppRoot.kt (+2 lignes, dont 1 commentaire), le nouveau test Tour9RenameWiredTest.kt et le rapport du lot. Rien d'autre n'est touché.

Branchement : correct. TopBarActions.renameScene a le type ((Int, String) -> Unit)? (TopBar.kt:148) ; RhythmViewModel.renameScene(index, label) (RhythmViewModel.kt:3382) a la même signature, refuse un index hors de allScenes et passe par session.edit (une étape d'annulation). C'est la fonction que Structure utilise déjà (AppRoot.kt, rename = rhythmVm::renameScene) : pas de chemin nouveau. L'index reste fixé tant que le dialogue est ouvert (TopBar.kt:289-300).

Le test prouve le câblage d'AppRoot : appli entière par AppHarness, en Pro, sans lambda fourni ; « scene_rename » ne paraît que si actions.renameScene != null (TopBar.kt:645). Une première assertion vérifie que le menu est ouvert (« scene_new »), ce qui écarte un faux rouge. Rouge mesuré avant le vert, vert après.

Mineur : le test s'arrête à l'entrée du menu (limite Robolectric des dialogues à champ texte) ; l'effet du nom est couvert par Tour9RenameFromChipTest. Acceptable.

Mineur : en Mode simple, la puce n'apparaît qu'en paysage (AppRoot.kt:2437), donc « Renommer » aussi ; cohérent avec la puce, action annulable et identique à Structure.

Thread audio : non concerné.

Verdict : 0 bloquant, 0 grave
