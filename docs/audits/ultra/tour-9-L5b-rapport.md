# Tour 9, lot L5b : rapport (contenu des cours, mots)

Tentés : 13 / 21 (F068, F073, F075, F076, F077, F082, F085, F088, F090, F094, F096, F097, F098).
Laissés de côté (hors fiche) : F078, F081, F084, F086, F087, F089, F093, F099 (voir `docs/decisions/tour-9-L5b.md`).
Finitions : pas de rejeu, la preuve est un test (un commit « rouge: » puis « vert: » par défaut).

tests: :app:testDebugUnitTest --tests '*Tour9L5bTutoWordsTest*' --tests '*I18nCatalogTest*' --tests '*LangTest*' --tests '*TutoPadNamesTest*' --tests '*Tour9TutoSweepTextTest*'

chemin: tuto basse, consigne Roll (en, de, 11 langues) | test: Tour9L5bTutoWordsTest#f068RollStepNamesTheTap
chemin: tuto, message de copie de partie (hi) | test: Tour9L5bTutoWordsTest#f073PartCopyMessageSaysWhatIsRemoved
chemin: tuto house, consigne Mix (ar) | test: Tour9L5bTutoWordsTest#f075MixTabNamedAsTheTab
chemin: tuto house, chapitre disco / Drop (14 langues) | test: Tour9L5bTutoWordsTest#f076DiscoChapterNamesTheDropPart
chemin: tutos, consigne feuille Tonalité (14 langues) | test: Tour9L5bTutoWordsTest#f077KeySheetNamedByItsTitle
chemin: tuto house, panneau intro (ja) | test: Tour9L5bTutoWordsTest#f082IntroSaysOnlyTheDrumsStay
chemin: tutos, consigne cases du dernier temps (de) | test: Tour9L5bTutoWordsTest#f085GermanLastBeatReadsRight
chemin: tutos, noms des sons (ar) | test: Tour9L5bTutoWordsTest#f088ArabicSoundNamesAreMusicWords
chemin: tutos, grille cases barrées (ja, nl) | test: Tour9L5bTutoWordsTest#f090CellsToEraseAreStruckThrough
chemin: tutos, compteur de cases (zh) | test: Tour9L5bTutoWordsTest#f094ChineseOffbeatHitsArePlural
chemin: tutos, Mix (nl) | test: Tour9L5bTutoWordsTest#f096DutchOverstuurtSpelledRight
chemin: carte des parties et tutos (tr) | test: Tour9L5bTutoWordsTest#f097TurkishPostChorusName
chemin: tutos, Mix sous le curseur (tr) | test: Tour9L5bTutoWordsTest#f098TurkishUnderTheSliderReadsRight
