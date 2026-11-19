# Tour 9, lot L17 « finitions du tuto » : relecture neuve
Relu : 6832498e

## Diff lu
`git diff ddf08286 6832498e` en entier : TutoController.kt, TutoPanel.kt (TutoGridLine), AcidScreen.kt, RhythmScreen.kt, TutoChecks.kt, TutoKind.kt, les 8 tests T9L17*, rapport, décisions, corrections.tsv. Rien sous `engine/` : thread audio intact. Aucun changement de sauvegarde (le seul `store.save` touché passe par `tick`, déjà existant).

## Tests relancés
- `:project-model:test --tests '*T9L17*' :app:testDebugUnitTest --tests '*T9L17*'` : 12 tests, 0 échec (8 classes T9L17). Premier passage resté « up-to-date » ; relancé avec `--rerun` : verts.
- En plus, toute la suite du modèle `:project-model:test --rerun` : 1259 tests, **1 échec** : `TripletsTest#theTutorialDetectsAndShowsATriplet` (TripletsTest.kt:157). Le même test sur ddf08286 (avant le lot, worktree jetable) : 8/8 verts. Le lot le fait passer au rouge.

## Rouge constaté
rouge constaté : 7fbb6174 : rejoué sur 7fbb6174^ : `T9L17ChapterDoneNoDottedTest` échoue, « chapter done, dotted notes left: {bass_sub=6} » (1 test, 1 échec). Le test prouve bien F044.

## Captures regardées (écran entier)
- `app/screenshots/rhythm.png` : pads, kit, grille 1 mesure, onglets, longueur ; rien ne déborde (« Hauteur » coupé par le défilement horizontal, comme avant).
- `app/screenshots/acid_editor.png` : générateur, grille 2 mesures, éditeur de pas, son ; propre. Le texte d'aide « Touche un pas pour le modifier » reste juste avec le nouveau toucher.

## Constats
- GRAVE : F032, `Triplet.done` passe par `tripletsDone`, qui exige toutes les cases du triolet de la ligne du style (trap : 12 et 14). Le test existant `TripletsTest#theTutorialDetectsAndShowsATriplet` pose un triolet vrai sur la case 4 du charley et attend « fait » : il passe au rouge (ligne 157). Il faut trancher : soit mettre à jour ce test (le comportement voulu par F032 a changé, avec une ligne dans les décisions), soit garder « n'importe quel triolet » hors de l'étape trap qui nomme les cases. En l'état, la CI du paquet sera rouge. Le rapport n'avait relancé que les tests T9L17.
- Moyen : Acid, toucher une case muette y pose maintenant une note. Pour verrouiller un silence (🔒) ou choisir la hauteur avant de jouer, il faut deux touchers (note puis silence) au lieu d'un. Hors tuto, deux touchers sur une note la rendent muette : changement de geste pour tous, voulu par F040 mais à faire valider par un testeur.
- Moyen : `TutoPadEar.heard` est un objet global qui tient le contrôleur du tuto ; il n'est rendu à null qu'à « Quitter ». Un contrôleur abandonné sans `quit()` (projet changé) reste tenu jusqu'au tuto suivant, et un pad joué entre-temps peut cocher l'étape de l'ancien projet. Petite fuite, sans perte de données visible.
- Moyen : TutoGridLine affiche maintenant « Mesure 2 · N cases → » en même temps que « Encore N cases en pointillé ici » ; sur un écran étroit, le statut (poids 1, une ligne) peut être coupé par « … ». Non vu sur les captures (aucune ne montre le cas).
- Moyen : `Crash.shown` rend null quand rien ne change (crash déjà sur toutes les parties) ; sans effet tant que l'étape est alors « faite », à surveiller.
- Rapport : « aucune image de référence ne change » et les phrases « tenté » sont justes ; mais il ne dit pas qu'un test existant passe au rouge.
- F036 (`endDemoNow`) : la démo annulée remet le projet, puis le `finally` de la démo refait `demoShow(null)` (sans effet) et ne vole plus `demoJob` grâce au test `===` : correct.

Verdict : 0 bloquant, 1 grave
