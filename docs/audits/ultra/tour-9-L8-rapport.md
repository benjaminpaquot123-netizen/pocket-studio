# Tour 9, lot L8 : rapport

Correcteur, branche `eq/tour-9-L8` (base 7455749a5). Zone : Partager, Tempo, feuilles Kit/Pad, + Piste, bandeau « Mode retours », noms de piste. Décisions : `docs/decisions/tour-9-L8.md`.

tests: :app:testDebugUnitTest --tests '*Tour9L8ShareWordsTest*' --tests '*Lot6UiShareLevelTest*' --tests '*ShareCancelReadableTest*' --tests '*ShareCheckOkMeaningTest*' --tests '*ShareNameFieldSeenTest*' --tests '*ShareSignedRtlTest*' --tests '*BackClosesSheetFirstTest*' --tests '*RetoursStripReadableTest*' --tests '*KitSheetNarrowTopTest*' --tests '*SheetCloseSeparateTest*' --tests '*TrackAddWholeTest*' --tests '*TempoSheetNarrowWholeTest*' --tests '*TempoTypingStaysTest*'

chemin: Partager, morceau avec reprises : « 3 parties (avec les reprises : 5) » (F125) | test: Tour9L8ShareWordsTest#theSongsRowsAreSaidBesideItsParts
chemin: Partager, projet au nom par défaut, interrupteur « renomme aussi le projet » éteint (F122) | test: Tour9L8ShareWordsTest#theRenameCanBeTurnedOff
chemin: Partager, projet déjà nommé : pas d'interrupteur (F122) | test: Tour9L8ShareWordsTest#aNamedProjectHasNoRenameSwitch
chemin: Partager, niveau Streaming / Auto en mots clairs (F121) | test: Tour9L8ShareWordsTest#eachLevelSaysWhatItMeans
chemin: Partager, détails « Mesuré pendant l'écoute » (F121) | test: Tour9L8ShareWordsTest#theDetailsSayTheyWereMeasuredWhileListening
chemin: Partager, mots neufs dans les 14 langues | test: Tour9L8ShareWordsTest#theNewWordsAreTranslatedEverywhere
chemin: Partager, Mode simple, niveau dit avant l'écoute (F121) | test: Lot6UiShareLevelTest#theSimpleLevelIsSaidBeforeAnyListen
chemin: Partager, export en cours, « Annuler » lisible dans chaque langue (F124) | test: ShareCancelReadableTest#annulerReadsDuringTheExportInEveryLanguage
chemin: Partager, rond vert « Tout est prêt » lu avec sa phrase (F126) | test: ShareCheckOkMeaningTest#theCheckIsReadWithItsSentence
chemin: Partager, lettres 200 %, nom tapé visible au-dessus de M4A / WAV (F123) | test: ShareNameFieldSeenTest#theTypedNameIsSeenAboveTheFormatChipsAt200Percent
chemin: Partager en arabe, signe moins collé à ses chiffres (F030) | test: ShareSignedRtlTest#arabicKeepsTheMinusWithItsDigits
chemin: Courbe « Automatiser » ouverte, Retour ferme la courbe avant de quitter (garde F131) | test: BackClosesSheetFirstTest#retourClosesTheAutomateSheetBeforeLeavingTheProject
chemin: Bandeau « Mode retours », toutes langues, 100 et 200 % (F135) | test: RetoursStripReadableTest#noWordOfTheStripIsCut
chemin: Feuille Kit, 360 dp, 200 % : haut jamais coupé | test: KitSheetNarrowTopTest#theTopOfTheKitSheetIsNeverCut
chemin: Feuilles Kit et Pad : « Fermer » encadré, à part du texte (F120) | test: SheetCloseSeparateTest#padSheetCloseStandsApart
chemin: Rangée Sons, « + Piste » entier au bout (F134) | test: TrackAddWholeTest#plusPisteShowsWholeAtTheRowsEnd
chemin: Feuille Tempo, 360 dp, 200 % : haut jamais coupé | test: TempoSheetNarrowWholeTest#theTopOfTheTempoSheetIsNeverCut
chemin: Feuille Tempo, tempo tapé au pavé : le pavé reste, seul le tempo tapé est marqué (F119) | test: TempoTypingStaysTest#theKeypadStaysAndOnlyTheTypedTempoIsMarked

Hors app : `project-model` TrackNameNoRepeatTest (F133, nom de piste sans mot répété, toutes langues, tous styles).

## Non faits
- F132 : le nom vient de RhythmScreen.padName (hors liste).
- F131 : passé à L10 (ici seulement le test de garde, vert sur la base).
- F127, F128, F129 : passés à L2.
