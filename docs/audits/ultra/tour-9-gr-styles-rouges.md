# tour-9 gr-styles: the reds, measured before each fix

Base: eq/tour-9-gr-l1b 04270bd19 + ccr-80af1a1b-mn0wec + eq/tour-9-gr-hiphop b1a7ec9c9 + eq/tour-9-gr-house 3b38648b5,
StyleRecipes.kt regenerated locally (not committed: the chef regenerates it at the merge).
`./gradlew :project-model:test`: 1295 tests, 9 failed.

- i18n: ModelTextsTest.everyDataTextHasItsTranslations (6 house texts × 14 languages), I18nCatalogTest.noTranslationIsLeftUnused (old acid text).
- tracks: SimpleMelodyTest.aMelodyTrackOfItsOwnSoundingAsTheLead and aTakeIsHeardThroughTheSong (NPE: the house fills the 8 instrument tracks, Basse, Piano, Lead, Contre-chant, Doublure, Montée, Voix, Sub drop; « ma mélodie » has no room).
- guides: GuideParcoursTest.theParcoursAreWhatTheGeneratorWrites (make_guides.py: « vocal » is now an object {type, sample} in hip-hop recipes, cards_for crashes).
