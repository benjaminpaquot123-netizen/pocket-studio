# Tour 9, lot L15 (grille rythme) : rapport

Branche : eq/tour-9-L15, partie de l'intégration ccr-80af1a1b-mn0wec (b81c56d3 fusionnée).
Fichiers de l'appli touchés : app/rhythm/RhythmScreen.kt, app/tuto/TutoPanel.kt (ligne d'aide de la grille : F048, F050, F052, F054 ; cible du Swing : F057), app/tuto/TutoController.kt et app/ui/AppRoot.kt (cible du Swing, accordés par le chef), project-model DefaultNames.kt (F132, accordé par le chef). Rien dans PadSound.kt ni PackKits.kt.

État par défaut (tous « tenté » au mieux, jamais « corrigé ») :
- F048 « Je vois toujours quel pad j'édite, même quand un message s'affiche. » : tenté, rouge bbdd21a4, vert e1f142d3 (le nom du pad passe au-dessus des cases sur sa propre ligne).
- F050 (moitié tuto) « La phrase d'aide de la grille se lit en entier, même à 200 %. » : tenté, rouge bbdd21a4, vert e1f142d3.
- En-tête « PAS : KICK » écrasé (vu par L4 à 200 %) : tenté, rouge c35caf4e, vert 36c00640 (l'en-tête tient sur une ligne, la puce « Tous les pads » passe dessous).
- F049 « À 130 et 200 %, l'en-tête de la grille ne cache ni la première rangée… » : ne corrige pas: F049 : plus reproduit après 36c00640 ; gardes Tour9L15GuardTest#f049HeadClearsTheGridAt130 et #f049HeadClearsTheGridAt200.
- F052 « Le repère vers une autre mesure dit s'il faut y ajouter ou y enlever une case. » : tenté, rouge de723f21, vert 3d861e49.
- F054 « Le repère vers le pad demandé m'y emmène d'un toucher. » : tenté, rouge 3d2a3625, vert 6758f7ce.
- F057 « Le curseur Swing montre la valeur visée et permet de l'atteindre à 1 % près. » : moitié « à 1 % près » tentée (« − » et « + »), rouge de723f21, vert 3d861e49. Moitié « valeur visée » : tentée, rouge juste avant da310a7c, vert da310a7c (« Cible : 25 % » au-dessus du curseur pendant l'étape Swing du tuto).
- Puce « Kit » pâlie (vue par L4 à 200 %) : ne corrige pas: non reproduite (capture sans fond) ; garde Tour9L15GuardTest#kitChipWholeAt200.
- F132 « Les noms de sons d'un même kit suivent la même forme. » : tenté, rouge 651a1c81, vert a12c97d0 (un son du kit montre sur son pad le seul mot du pad, « Caisse », comme les autres pads ; la fiche Son garde « Drum & bass · Caisse »).

controle: reporté (mode boost, grande vérification de fin)

Registre corrections.tsv : aucune ligne du trieur pour le tour 9 dans l'intégration, et un lot n'ajoute pas de ligne (reparations.py) ; rien écrit, les états sont ici.

Captures : réenregistrées seulement nav/simple_rhythm.png, rythme_empty_project.png, rhythm_scrolled_under_part.png (écran Rythme, changé par le lot). Captures de revue (build/outputs/roborazzi/tour9-L15/) regardées en entier à 100, 130 et 200 %. sons_synth_take.png et pad_sound_name_whole.png diffèrent aussi, pas à cause de ce lot (texte du Guide, nuances de pixels) : non touchées.

Décisions : docs/decisions/tour-9-L15.md.

tests: *Tour9L15* *TutoScreenTest* *NavScreenshotTest* *HomeAndPadsTest* *PadHoldAndTrackDeleteTest* *Lot6VmTutoKitNameTest*

chemin: Rythme, ligne d'aide du tuto, pad montré au-dessus des cases | test: Tour9L15GridLineTest#f048PadOverTheCellsWithItsCount
chemin: Rythme, ligne d'aide du tuto en effacement | test: Tour9L15GridLineTest#f048PadOverTheCellsWhileErasing
chemin: Rythme, ligne d'aide du tuto avec un message | test: Tour9L15GridLineTest#f048PadOverTheCellsWhileAMessageShows
chemin: Rythme, ligne d'aide du tuto à 200 % | test: Tour9L15GridLineTest#f050ReadsInFullAt200WithCellsHere
chemin: Rythme, ligne d'aide vers une autre mesure à 200 % | test: Tour9L15GridLineTest#f050ReadsInFullAt200WithCellsElsewhere
chemin: Rythme, ligne d'aide en effacement à 200 % | test: Tour9L15GridLineTest#f050ReadsInFullAt200WhileErasing
chemin: Rythme, en-tête de la grille à 200 % | test: Tour9L15StepHeadTest#at200
chemin: Rythme, en-tête de la grille à 130 % | test: Tour9L15StepHeadTest#at130
chemin: Rythme, en-tête au-dessus de la première rangée à 130 % | test: Tour9L15GuardTest#f049HeadClearsTheGridAt130
chemin: Rythme, en-tête au-dessus de la première rangée à 200 % | test: Tour9L15GuardTest#f049HeadClearsTheGridAt200
chemin: Rythme, puce Kit à 200 % | test: Tour9L15GuardTest#kitChipWholeAt200
chemin: Rythme, repère vers une mesure où enlever | test: Tour9L15FineTest#f052FarHitToEraseSaysToTakeItAway
chemin: Rythme, repère vers une mesure où ajouter | test: Tour9L15FineTest#f052FarCellToAddSaysNothingOfTakingAway
chemin: Rythme, cases demandées sur un autre pad | test: Tour9L15PadJumpTest#f054CellsAskedOnAnotherPad
chemin: Rythme, croix à enlever sur un autre pad | test: Tour9L15PadJumpTest#f054HitsToEraseOnAnotherPad
chemin: Rythme, Swing « − » et « + » | test: Tour9L15FineTest#f057SwingOnePercentUpAndDown
chemin: Rythme, Swing à ses bouts | test: Tour9L15FineTest#f057SwingStopsAtItsEnds
chemin: Rythme, étape Swing du tuto, cible au-dessus du curseur | test: Tour9L15FineTest#f057SwingStepSaysItsAim
chemin: Rythme, hors étape Swing, pas de cible | test: Tour9L15FineTest#f057NoAimOutsideASwingStep
chemin: Rythme, pads d'un kit chargé, même forme de nom | test: Tour9L15KitSoundNamesTest#f132KitSoundsSayTheSameForm
chemin: Rythme, pads d'un kit chargé en mots de tous les jours | test: Tour9L15KitSoundNamesTest#f132KitSoundsSayTheSameFormInEverydayWords
chemin: Rythme, pad avec un son à moi | test: Tour9L15KitSoundNamesTest#f132OwnSoundsKeepTheirName
