# Tour 9, lot L2 « Mix » (zone mix) : rapport

Branche : eq/tour-9-L2, partie de l'intégration ccr-80af1a1b-mn0wec (base 7455749a5).
Rôle : correcteur du Mix. Décisions : `docs/decisions/tour-9-L2.md`.

Ce qui change :
- D01 / D11 : c'est le banc (fader et effets d'une tranche marchent sans banc). Le banc tire le fader depuis son bouton.
- F024 : un fader ne se prend qu'à ±32 dp de son bouton, le pad XY qu'à son point ; ailleurs, la page défile.
- F127 : un trait de courbe qui commence juste sous la toile dessine. F129 / F130 : boucles comptées en mesures, options nommées par la piste.
- F023, F025, F026, F027, F028, F029, F030, F141 : noms entiers, têtes du Mode simple à une taille, anneau des curseurs à zéro, effet ajouté visible, signe moins arabe, pas de travail neuf à chaque image.
- S34 : l'onglet « Voir les autres pistes » est posé à côté des tranches. D12 : les envois d'un pad se trouvent depuis le Mix.
- F136 : la feuille du pad et la carte Effets disent la piste ; les appels d'`AppRoot.kt` (hors lot) doivent passer `trackName`.

tests: *Tour9MixFaderNoBenchTest* *Tour9MixScrollTest* *Tour9CurveDrawTest* *Tour9AutomationFinishTest* *Tour9MixFinishTest* *Tour9SendRailTest* *Tour9EffectAddTest* *Tour9EffectsCardNameTest* *BenchMixFaderCoverTest* *SimpleMixTest* *FadingRowsTest* *CountWordsTest*

chemin: Mix pro, glisser un fader | test: Tour9MixFaderNoBenchTest#d01_aDragMovesTheProConsolesFader
chemin: Mode simple, glisser un fader | test: Tour9MixFaderNoBenchTest#d01_aDragMovesTheSimpleMixsFader
chemin: Mix pro, toucher le nom d'une tranche | test: Tour9MixFaderNoBenchTest#d11_aTouchOnAStripsNameOpensItsEffects
chemin: Mix, envois d'un pad | test: Tour9MixFaderNoBenchTest#d12_aPadsSendsAreFoundFromTheMix
chemin: Mix pro, autres pistes à droite | test: Tour9MixFaderNoBenchTest#s34_theProConsoleSaysMoreTracksWaitOnTheRight
chemin: Mode simple, autres pistes à droite | test: Tour9MixFaderNoBenchTest#s34_theSimpleMixSaysMoreTracksWaitOnTheRight
chemin: Mix pro, glisser depuis le milieu d'un fader | test: Tour9MixScrollTest#f024_aSlideFromTheProFadersMiddleScrollsAndMovesNothing
chemin: Mix pro, glisser depuis le bouton d'un fader | test: Tour9MixScrollTest#f024_aSlideFromTheProFadersCapMovesItAndThePageStays
chemin: pad XY, glisser loin du point | test: Tour9MixScrollTest#f024_aSlideOnThePadXyAwayFromItsDotScrollsAndMovesNothing
chemin: Automation, trait depuis le bord bas | test: Tour9CurveDrawTest#f127_aStrokeFromTheBottomEdgeUpDrawsAndTheMixStaysPut
chemin: Automation, boucle de quatre mesures | test: Tour9AutomationFinishTest#f129_aFourBarLoopIsCountedInBars
chemin: Automation, options de boucle | test: Tour9AutomationFinishTest#f130_theLoopOptionsNameTheTrack
chemin: Mix pro, noms après défilement | test: Tour9MixFinishTest#f023_eachStripKeepsItsNameWholeAfterAScroll
chemin: Mix pro, feuille d'une tranche | test: Tour9MixFinishTest#f025_aStripsSheetIsTitledWithTheTracksWholeName
chemin: Mode simple, têtes des tranches | test: Tour9MixFinishTest#f026_f027_theSimpleMixsHeadsShareOneSizeAndHeight
chemin: Mix en arabe, signe moins | test: Tour9MixFinishTest#f030_inArabicTheMinusSignIsOnTheLeft
chemin: Mix, travail à chaque image | test: Tour9MixFinishTest#f141_theMixBuildsNothingNewAtEachFrame
chemin: Mix, envoi à zéro | test: Tour9SendRailTest#f028_aSendAtZeroShowsItsTrack
chemin: Effets, effet ajouté en bas de liste | test: Tour9EffectAddTest#f029_anEffectAddedFromTheBottomOfTheListIsShown
chemin: carte Effets sous un son | test: Tour9EffectsCardNameTest#f136_theCardUnderASoundSaysItsTrack
