# Tour 9 : lot « textes » (rapport)

Base : `ccr-80af1a1b-mn0wec`. Branche : `eq/tour-9-textes`. Tout est « tenté » (pas encore constaté par un testeur neuf).

- F081, F087, F089, F093, F099 : déjà réglés par L16 (e5ae6b3d, a2ccda05, bfce7c01), vérifiés sur la base ; rien à changer.
- F084 (es) : reste trouvé, « Arrêter la ligne %d » disait « Parar » quand le bouton dit « Detener ». Rouge 28937e0f, vert 48205aad.
- Reste de miettesB : la fiche « Son » du guide disait « Synthé » après un bouton tourné ; elle nomme maintenant le son comme l'onglet. Rouge 4d46c367, vert 94f73577.
- Rouge connu hors lot : I18nCatalogTest « TrackBar.kt: it » (déjà rouge sur la base ; fichiers du lot base-rouges).

tests: *Tour9TextesStopWordTest *Tour9TextesGuideSoundNameTest *Tour9L16TextsTest *Tour9L16ShareTextsTest *Tour9L16LongPressStateTest

## Chemins

chemin: Pad live, bouton stop d'une ligne (es) | test: Tour9TextesStopWordTest#everyStopTextUsesTheStopWord
chemin: Guide > fiche d'une piste > Son | test: Tour9TextesGuideSoundNameTest#theGuideSheetNamesTheChosenSoundLikeTheTab
chemin: Guide > fiche d'une piste > Son, chaque son de la bibliothèque | test: Tour9TextesGuideSoundNameTest#everyLibrarySoundWithAKnobTurnedReadsLikeTheTab
chemin: Guide > fiche d'une piste neuve > Son | test: Tour9TextesGuideSoundNameTest#aFreshTracksSoundIsNamedAsBefore
chemin: Grille du rythme, coups barrés (ko) | test: Tour9L16TextsTest#koreanCrossedOutHitsSayWhatToRemove
chemin: Fin de chapitre (tr) | test: Tour9L16TextsTest#turkishChapterIsNeverTheWordOfAPart
chemin: Partage, fenêtre prête | test: Tour9L16ShareTextsTest#theReadyLineDoesNotRepeatTheTitle
chemin: Partage, bouton OK (tr) | test: Tour9L16ShareTextsTest#noScreenWritesOkByHand
chemin: Grille du rythme, case en pointillé (zh) | test: Tour9L16LongPressStateTest#aLongPressCellSaysLongPress
