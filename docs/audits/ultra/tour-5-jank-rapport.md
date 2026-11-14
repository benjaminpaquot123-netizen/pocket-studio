# Tour 5, lot « jank » : images lentes de Sons, Mix et Mes projets pendant la lecture (2026-11-12)

Point de départ : BenStudio Test « Tout tester » sur le HONOR BKQ-N49 (Android 17, appli R8), chaque écran 5 s en lecture,
une image lente = plus de 16,7 ms (`FrameMetrics.TOTAL_DURATION`, fenêtre du studio) : structure 5 %, Sons 31 %, Mix 48 %,
Live 1 %, Mes projets 25 %. Branche : `eq/tour-5-jank`. Décisions : `docs/decisions/tour-5-jank.md`.

tests: *PlayingRecomposeTest *.rhythm.RhythmBeginnerTest *GestureBudgetTest *.nav.SpacesTest *.app.ScreenshotTest *WriteScreenshotTest

## Mesure

`PlayingRecomposeTest` affiche l'appli entière (AppRoot, `AppHarness`), met le transport en lecture et fait bouger les lectures
du moteur comme en vrai pendant 60 lectures (le pas 30 fois par seconde, un pas toutes les 3 lectures ; les vumètres et la
loudness 20 fois par seconde), puis compte les portées Compose recomposées dans toutes les compositions de la fenêtre
(`Recomposer.observe` + `CompositionObserver`). Robolectric ne dessine pas la fenêtre : les dessins ne sont pas comptés ici.

| Écran | Avant (rouge, 96b8934d) | Après | Cause |
|---|---|---|---|
| Sons (Rythme) | 2296 | 253 | `WithLive` recomposait tout `RhythmScreen` (16 pads, la grille, les cartes) à chaque pas ; le fondu des pads recomposait chaque pad à chaque image |
| Mix | 1934 | 374 | `MasterLoudnessPanel` lisait la ligne des mesures (changée à chaque lecture) : tout le panneau, ses 6 puces « Volume visé » comprises, 20 fois par seconde |
| Mes projets | 0, mais la frise de Structure gardée dessous | 0, studio retiré dessous | l'onglet du studio continuait de dessiner sa tête de lecture sous la fenêtre des projets : ce sont ces images-là que le banc chronomètre |

Ce qui reste sur Mix : `LoudnessChip` et `LimiterLine`, un texte chacun, recomposés quand leur texte change.

## Correctifs

- `RhythmScreen(live: () -> LiveUi)` ; la case sous la tête de lecture, le pad allumé et la page suivie lisent la position
  eux-mêmes (`rememberChangesOf`, ui-components) ; le halo d'un pad est lu au dessin (fond, nom par `WholeWordsText` à
  `ColorProducer`, bande).
- `MasterLoudnessPanel` : la ligne des mesures lue par son seul texte ; gain et perte en états dérivés au dixième de dB.
- AppRoot : `if (!showProjects)` devant le `SaveableStateProvider` de l'onglet (état sauvé, onglet reconstruit à la fermeture comme avant).

## Captures

`verifyRoborazziDebug` (--tests com.pocketstudio.app.ScreenshotTest --tests com.pocketstudio.app.WriteScreenshotTest --tests com.pocketstudio.app.TabletScreenshotTest --tests com.pocketstudio.app.UiScaleScreenshotTest --tests com.pocketstudio.app.I18nScreenshotTest --tests com.pocketstudio.app.TabletI18nScreenshotTest --tests com.pocketstudio.app.MotionScreenshotTest --tests com.pocketstudio.app.TutoScreenshotTest --tests com.pocketstudio.app.nav.* --tests com.pocketstudio.app.tuto.TutoAppScreenshotTest --tests com.pocketstudio.app.ui.* --tests com.pocketstudio.app.export.ShareScreenshotTest --tests com.pocketstudio.app.rhythm.* --tests com.pocketstudio.app.mixer.* --tests com.pocketstudio.app.projects.*) : 1722 tests, 0 échec, aucune capture changée ni réenregistrée.

## Limites

- Les dessins (vumètres tramés, crête qui retombe à chaque image) ne sont pas mesurés ici ; seul le téléphone dira la part
  d'images lentes après. Acid 303 garde `WithLive` (même défaut que Rythme, hors du parcours du banc).

## Chemins

- chemin: Sons › Rythme en lecture (pas, pads allumés) | test: PlayingRecomposeTest#sonsComposesLittleWhilePlaying
- chemin: Mix en lecture (loudness, limiteur, vumètres) | test: PlayingRecomposeTest#mixComposesLittleWhilePlaying
- chemin: Mes projets ouvert depuis Structure en lecture, puis retour | test: PlayingRecomposeTest#projectsComposeLittleWhilePlaying
- chemin: Rythme, peindre des pas pendant la lecture | test: GestureBudgetTest#aStrokeCutShortIsStillItsOwnUndoStep
