# Tour 9 : base-rouges2 (rapport de zone)

Base : intégration `ccr-80af1a1b-mn0wec` à ceb482ad.

## Rouges reproduits sur la base (avant correction)
- tempo_sheet phone360 1.0, 12 langues + pseudo : « Touche ici à chaque temps : le tempo suit. » coupé (lignes coupées). Cause : L8 (F119) a donné sa propre ligne à « Entre 40 et 300 » ; « Taper en rythme » et son indice sont serrés sous le pavé.
- SharePathTest : « partager » en 4 gestes (launch_continue › tab_simple_Envoyer › scroll to share_bars_16 › share_bars_16), budget 3. Cause : L8 (F122, ea388482 + a078458a) a ajouté la ligne-interrupteur « Renommer aussi le projet » (48 dp) sous le nom, ce qui pousse « Longueur de la boucle » hors de l'écran.

## Corrections (vert)
- chemin: Tempo › feuille sur un petit téléphone debout (360 x 780), « Taper en rythme » et sa phrase entières dans les 12 langues | test: CaptureMatrixTest#screenFits
- chemin: Accueil › Continuer › Envoyer › 16 mesures en 3 gestes, l'interrupteur « Renommer aussi le projet » gardé (sous la longueur de boucle) | test: SharePathTest#theSheetsChipsSetTheLoopLengthAndTheDuration
- Pas de contradiction avec DECISIONS.md : le budget 3 gestes (G1) tient, aucun test modifié.
tests: --tests *CaptureMatrix*tempo_sheet* --tests *CaptureMatrix*share* --tests *SharePathTest --tests *Tour9L8CapturesTest --tests *Tour9L8ShareWordsTest --tests *TempoSheetNarrowWholeTest --tests *TempoTypingStaysTest --tests *LyingSheetsScreenshotTest --tests *LyingSheetsScrollTest --tests *ArchitectureRulesTest --tests *HelpCoverageTest --tests *RhythmBeginnerTest --tests *PadLiveTakeTest --tests *AutoTempoJourneyTest (121 verts)
