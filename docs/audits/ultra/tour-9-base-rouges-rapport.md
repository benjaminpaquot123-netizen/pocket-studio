# Tour 9 : lot base-rouges (rapport)

tests: *TutoReadabilityBudgetTest *Tour9L5bTutoWordsTest
Base : ccr-80af1a1b-mn0wec (8801b1dc). Tests rouges sur la base, mesurés avant correction.

## Rouges mesurés sur la base
- TutoReadabilityBudgetTest#everyFacileConsigneFitsOrIsAKnownException : 12 consignes « appui long … sous « Roulement » » en de et hi (grille, 360 dp, 100 %), allongées par F068 (L5b).
- TutoReadabilityBudgetTest#proOutOfBudgetOnlyGoesDown : 3740 consignes Pro hors budget pour un cliquet à 3725 (le cliquet descendu par L5b sur sa branche ; la base combinée ne le tient pas).

## Corrigé (vert)
- Facile : de et hi raccourcis (« dann tippe „×4“ unter „Roll“ », « “रोल” में “×4” टैप करो ») ; le verbe de la touche reste (F068 vert).
- Pro : le « efface les coups du dernier temps » japonais raccourci (« 取り消し線のマスをタップ », F090 vert) : 3722 consignes, cliquet descendu à 3722 ; 9 exceptions Facile ja qui tiennent désormais retirées (liste gelée à 400).

chemin: Rythme, tuto chapitre « fill » (Roulement), de et hi | test: TutoReadabilityBudgetTest#everyFacileConsigneFitsOrIsAKnownException
chemin: Rythme, tuto « gap » Pro, ja | test: TutoReadabilityBudgetTest#proOutOfBudgetOnlyGoesDown
chemin: Rythme, tuto « gap », ja | test: TutoReadabilityBudgetTest#everyFacileExceptionStillMisses
chemin: Rythme, tuto « Roulement », toutes langues | test: Tour9L5bTutoWordsTest#f068RollStepNamesTheTap
chemin: Rythme, tuto « gap », ja | test: Tour9L5bTutoWordsTest#f090CellsToEraseAreStruckThrough

## Rouge mesuré : I18nCatalogTest
- I18nCatalogTest#everyTextPassedInAVariableComesFromACheckedSource : « TrackBar.kt: it », le `tr(it)` ajouté par L7 (nom d'un son de la bibliothèque près des boutons de la piste) n'est pas dans la liste des sources vérifiées.
- Corrigé : « TrackBar.kt: it » ajouté aux sources vérifiées ; ce sont les noms SoundLibrary (SYNTH, PLAITS), déjà traduits et contrôlés par ModelTextsTest. Le test n'est pas affaibli : la source est réellement vérifiée.

Test du module project-model (hors app) : I18nCatalogTest#everyTextPassedInAVariableComesFromACheckedSource, joué à part (:project-model:test), vert.

## Non corrigé ici : T9PanelTargetTest#fillCellsAboveThePanelIn360At200Japanese
- Rouge sur la base : « panel at 734.0 ; cells 16, whole above it 0 ».
- Ce n'est pas le texte : la consigne japonaise raccourcie d'un tiers laisse le panneau exactement au même endroit (734.0). La cause est dans la mise en page du panneau ou du défilement de la grille (TutoPanel.kt, RhythmScreen.kt), fichiers de L13 et L15 : renvoyé au chef.
