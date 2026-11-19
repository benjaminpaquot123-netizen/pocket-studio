# Tour 9, lot L5a « contenu des cours » (zone tuto) : rapport

Branche : eq/tour-9-L5a, partie de l'intégration ccr-80af1a1b-mn0wec.
Défauts visés : F058–F067, F069–F072, F074, F079, F080, F083, F091, F092, F095, F137 (`docs/audits/ultra/tour-9-tri/finitions.tsv`, phrases d'acceptation mot pour mot dans ce fichier).

Ce qui change (données des cours et leurs 14 langues, aucun code de l'appli) :
- F058 : plus de guillemets dans des guillemets (« Créer la partie Intro »), dans toutes les langues.
- F137 : l'Intro 2 laisse de côté les mêmes pistes en Facile et en Pro.
- F059 : les cases en pointillé suivent le texte (afro_house : 3e accord La mineur ; uk_garage : 2 notes par mesure ; liquid : les notes fantômes sont en 2.4 et 3.2).
- F060 : une étape de batterie nomme le pad comme le pad s'affiche.
- F062 : detroit « Accords », gabber Montée, drill « Couplet 30 », deep_house tom hors des kicks.
- F063 : acid_techno écrit La# comme le clavier Acid ; en arabe, les noms de notes passent en solfège.
- F066, F067 : l'onglet « Acid 303 » et le kick sont nommés comme à l'écran.
- F069 : retro_techno place le roulement dans la mesure dite.
- F070 : la Fin de retro_techno est la 909 seule.
- F071 : le Pont d'uk_garage garde les deux charleys.
- F072 : le lead d'industrial dit de recopier sur les mesures suivantes.
- F080 : detroit ne parle plus de « dernière mesure » sur une grille d'une mesure.
- F083 : jungle écoute ses parties dans l'ordre du morceau.
- F091 : house demande le swing que l'étape accepte.
- F092 : plus de « sous la grille » pour le Swing.
- F095 : le mix dit « −1 ».

Demandes au chef (hors de mes fichiers, rien changé) :
- F061 : TutoPanel.kt, KitAndPadSheets.
- F064 : AppSpaces.kt, AcidLook.kt.
- F065, F079 : PlainWords.kt.
- F074 : Scenes.kt (`shownLabel`).

Laissés de côté : aucun autre. Vu hors lot : la Fin de hard_techno ne cite pas ses percussions.

Captures (360 dp, 100 % et 130 %, Simple et Pro, en arabe : l'écran touché par F063) : `app/screenshots/tour9_l5a_acid_ar_{simple,pro}_{100,130}.png`, regardées. Le Pro montre « لا# » ; le Simple passe par l'étape Facile `acid_show`, sans nom de note.

Décisions : docs/decisions/tour-9-L5a.md.

Preuves : un commit « rouge: » (test vu rouge sur son assertion) puis « vert: » par sujet ; registre `corrections.tsv` à « tenté » avec commit et test.

Siège de contrôle : le lot change les cours du tuto. Un débutant neuf doit rejouer ses chapitres sur cette branche. C'est demandé au chef, puisqu'un correcteur ne joue pas de siège. La ligne « controle: » reste à ajouter.

tests: *Tour9TutoL5a* *Tour6Tuto* *TutoOneMoment* *TutoAllCourses*

chemin: tuto, tous les cours et toutes les langues, guillemets du panneau | test: Tour9TutoL5aQuotesTest#noCourseTextNestsAQuoteInsideTheSameMarks
chemin: tuto techno Pro, partie Intro 2 | test: Tour9TutoL5aQuotesTest#aPartLeavesOutTheSameThingsAtBothLevels
chemin: tuto afro_house, rouleau des accords en pointillé | test: Tour9TutoL5aCourseTest#afroHouseThirdChordIsTheMinorFourOfTheCourse
chemin: tuto uk_garage, rouleau du lead | test: Tour9TutoL5aCourseTest#ukGarageLeadPlaysTheSameTwoNotesEveryBar
chemin: tuto liquid_dnb Pro, grille de la caisse claire | test: Tour9TutoL5aCourseTest#liquidGhostNotesAreTheOnesTheProTextNames
chemin: tuto, grille des pads, étape de batterie | test: Tour9TutoL5aPadsTest#aReportedDrumStepNamesItsPadAsThePadShows
chemin: tuto, panneau des textes de cours | test: Tour9TutoL5aCourseTest#courseTextsSayWhatTheScreenShows
chemin: tuto acid_techno, panneau en toutes les langues, notes de la 303 | test: Tour9TutoL5aNotesTest#acidTechnoNamesTheNotesAsTheAcidKeyboardDoes
chemin: tuto, panneau en arabe, noms de notes | test: Tour9TutoL5aNotesTest#arabicCourseTextsNameNotesInSolfege
chemin: tuto acid_techno, Simple, panneau en arabe à 360 dp | test: Tour9TutoL5aShotsTest#simpleAcidStepNamesNotesAsTheKeys
chemin: tuto acid_techno, Pro, panneau en arabe à 360 dp, 130 % | test: Tour9TutoL5aShotsLargeTextTest#proAcidStepNamesNotesAsTheKeys
chemin: tuto, onglet Acid 303 nommé | test: Tour9TutoL5aWordsTest#aStepNamesTheAcidTabAsTheTabShowsIt
chemin: tuto acid_techno, Intro, pad du kick | test: Tour9TutoL5aWordsTest#theAcidTechnoIntroNamesTheKickThePadShows
chemin: tuto retro_techno, roulement de caisse claire | test: Tour9TutoL5aCourseTest#retroTechnoRollIsInTheBarTheTextSays
chemin: tuto retro_techno, partie Fin | test: Tour9TutoL5aCourseTest#retroTechnoFinIsTheDrumMachineAloneNotTheIntro
chemin: tuto uk_garage, partie Pont | test: Tour9TutoL5aCourseTest#ukGaragePontKeepsBothCharleys
chemin: tuto industrial_techno, rouleau du lead | test: Tour9TutoL5aCourseTest#industrialLeadStepSaysToCopyOnTheNextBars
chemin: tuto detroit, étape du trou | test: Tour9TutoL5aCourseTest#detroitGapSpeaksOfNoBarTheGridCannotPick
chemin: tuto, carte du morceau, écoutes des parties | test: Tour9TutoL5aCourseTest#everyCourseListensToItsPartsInTheSongsOrder
chemin: tuto, étape Swing, valeur demandée | test: Tour9TutoL5aCourseTest#swingTextsAskForTheValueTheStepAccepts
chemin: tuto, étape Swing, place du curseur | test: Tour9TutoL5aCourseTest#swingStepsDoNotPlaceTheSliderUnderTheGrid
chemin: tuto, étape de mix, niveau du fader | test: Tour9TutoL5aCourseTest#mixStepsNameTheFirstLevelTheyAccept

État : tenté, prêt pour contrôle (pas « corrigé » : un siège neuf doit le constater).
