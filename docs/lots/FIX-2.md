# Lot FIX-2, retours de la revue d'interface : note de suivi

Branche fx-fix2 (à partir de fx-int). Sept retours d'une revue neuve de l'interface ; chacun corrigé avec un test qui échoue avant et passe après, et la même faute cherchée ailleurs (« règle partout »).

## Fait (2026-11-03)
- **1 (grave) Découper la chanson, chanson entière** (`remix/CutWave.kt`, `remix/CutModel.kt`) : zone de prise d'une ligne de coupe 12 dp (au lieu de 28), et une ligne n'est prise que si ses voisines sont à plus de deux fois cette zone (chanson entière coupée à chaque mesure : un appui place la tête de lecture). Une coupe ne bouge qu'après le seuil de glissement (un tremblement ne la déplace plus), un appui sur une ligne sans glisser place la tête. Un deuxième doigt pendant qu'une coupe suit le doigt passe au zoom. L'étape d'annulation et la sauvegarde n'arrivent qu'au premier vrai déplacement ; `endMove` dans un `finally` (geste annulé).
  - Tests : `CutModelTest.theWholeSongCutEveryBarLeavesTheTouchesToThePlayHead` (chanson de 3 min coupée à chaque mesure), `CutZoomGestureTest` (2 : appui près d'une ligne = tête de lecture, rien d'annulable ni sauvé ; pincer en partant d'une ligne = zoom). Vérifiés rouges sur l'ancien code. `CutJourneyTest` : le geste dépasse maintenant le seuil de glissement de Robolectric (16 dp) avant de revenir.
  - Règle partout : `remix/StemBoardScreen.kt` (poignées de sélection) : déjà seuil, deux doigts et retour arrière ; rien à changer.
