# Tour 9, lot L1 (D06) : relecture neuve
relu : 4744d3e6edea50f3928f45ba24a685dafd3a9a07

## Ce qui a été lu
- Commits 587e46e3, ef3653e5, 865d4357 (ligne de la partie retirée) et ed8d515c, ae6c45c2, 74e80394, 91eb9947 (compteur coupé, rejeu en hindi, docs).
- `ui/AppRoot.kt` : `rollFirst = rollToTouch && NotesView.shown && fontScale > 1.3f` ; `WithPartHeader(…, shown = !rollFirst)`. Les trois sources sont des états Compose (`TutoHints.notes`, `NotesView.shown` : `mutableStateOf` ; `LocalDensity`), donc la ligne revient dès que les notes sont touchées ou le rouleau quitté. Paramètre `shown = true` par défaut : les autres appels de `WithPartHeader` ne changent pas.
- `synth/PianoRoll.kt` (`TutoNotesLeft`) : « Touche ici » factorisé dans `goLabel`, même condition qu'avant, même tag `tuto_notes_left_go`. À 130 % et moins, il reste à droite comme avant ; le seul changement est que `tuto_roll_where` est posé dans une `Row` avec `weight(1f)` : même largeur (la colonne est déjà pondérée), même alignement à gauche, même hauteur. Tags `tuto_notes_left`, `tuto_notes_left_text`, `tuto_roll_where` gardés.
- Aucun accès nul forcé, aucune nouvelle voie de plantage ; aucun code du moteur ni du thread audio touché.

## Tests lancés
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*T9PanelTargetTest' --tests '*T9ReadMoreTest' --tests '*TutoKickSoundTest' --tests '*Tour7TutoKitBigFontTest'` : vert, 15 tests, 0 échec (T9PanelTargetTest 8, T9ReadMoreTest 3, TutoKickSoundTest 3, Tour7TutoKitBigFontTest 1). Aucune capture réécrite.

## Rejeu du rouge
- Arbre temporaire sur ed8d515c (le test du rouge, sans la correction de PianoRoll.kt) : `T9PanelTargetTest#dottedNotesAboveThePanelInSimpleAt200French` échoue avec « « encore N notes » drawn as « ↑ Encor » of « ↑ Encore 3 notes en pointillé à toucher » ». Rouge confirmé, puis vert sur la tête.

## Capture
- `app/screenshots/tour9/d06_dotted_notes_simple_fr_200.png`, écran entier : rouleau de notes au-dessus du panneau du tuto, trois notes pointillées visibles, « Touche ici » en haut à côté de « Basse · … », compteur « ↑ Encore 3 notes e… » avec son nombre lisible. Rien ne se chevauche.

## Remarques
- moyen : la seconde moitié de la phrase d'acceptation (« le compteur baisse à chaque note touchée ») n'est prouvée par aucun test du lot ; le calcul du compteur n'a pas changé, mais le contrôle par un siège neuf doit toucher une note à 200 % et lire N-1.
- léger (hors lot, déjà là) : à 200 %, l'étiquette « mes. » de la règle du rouleau est coupée en « me ».
- léger : le test unitaire ne joue que le français ; le hindi (cas qui cachait le rouleau) n'est couvert que par le rejeu `t9-d06-pointilles-200`.

Verdict : 0 bloquant, 0 grave
