# Tour 6 — lot 6 « morceau-export-padlive » : rapport

Source : `lots-correction.md` (lot 6 et complément 2, eq/tour-6-tri-gaucher 974986c4), `verdicts.tsv`. Base : tête de
`ccr-80af1a1b-mn0wec` (5957fac5, gaucher, langues et kick dedans). Trois sous-agents, un par groupe de fichiers, chacun
dans sa copie du dépôt ; branches fusionnées ici. Aucun texte neuf à traduire (mots déjà traduits réutilisés).

## Corrigé (rouge puis vert) : 21 sur 24, plus N25
- **RhythmViewModel.kt** : R4 (« la fin sera coupée » quand la chanson dépasse le morceau), U28 (« + Nouvelle partie »
  sur un morceau vide garde A avant B), U11 (« Fin » arrête la lecture), U20 (« Écouter » garde la ligne de la prise),
  U3 (la scène affichée seule ne rend pas l'export « changé »), U5 (le format choisi en Simple est gardé en Pro),
  U14 (« Fait : accords… » nomme les accords posés), N25 (« Aller dans la partie… » pendant la lecture montre la
  partie, qui arrive à la mesure suivante, sans arrêter), T2 part `loadTutoKit` (le pad du grondement garde « Rumble »).
- **ShareSheet.kt** : U2 (en Simple, le niveau dit est celui visé par l'export), U4 / U15 / N7 (la rangée M4A/WAV ne
  recouvre plus rien : la partie qui défile s'estompe au bord où il y a la suite), U16 (le champ dit « Nom » quand le
  partage ne renomme pas le projet).
- **MixerScreen.kt** : R23 (le Mix Pro s'ouvre sur ses tranches ; il ne descend à l'export que pour un fichier fait
  pendant qu'il est ouvert).
- **ProjectsScreen.kt** : U23 (« Chargement de la liste… » et un signe d'attente pendant la lecture du catalogue).
- **LaunchChoice.kt** : U10 (médaillons ronds de même taille, « Défis » aligné).
- **TrackBar.kt** : N18 (« ♪ Son » écrit), R19 part pistes (un chevron sur le fondu où la rangée continue).
- **PadLiveScreen.kt** : R10 (un seul Partager par écran), U18 (« Moins » montre une flèche de pli, pas ⋯), U21 (les
  pads gardent taille et place quand le bandeau « Set gardé » apparaît : les messages passent sous les pads).

## Laissé de côté (3)
- **U1** (« Joue d'ici » annonce le saut) : la file d'attente de la partie est voulue ; le dire demande `PlayStrip.kt`
  (et `AppRoot.kt`), hors du lot (ex. « %s : ensuite », déjà traduit).
- **U9** (effets ouverts depuis Pad Live titrés « Remix ») : le titre est construit dans `ui/AppRoot.kt` (dialogue des
  effets, d'après le seul numéro de tranche) ; il faut y passer l'origine (`tr("Pad Live")` existe).
- **R8** : non reproduit sans le banc (`Lot6PadWaitingRingTest`, vert du premier coup : l'anneau pâle reste jusqu'à
  la ligne) ; artefact du banc (lecture de l'écran ~1,1 s après le toucher).
- **« Boum boum »** (candidat du banc) : confirmé, le défi ne nomme pas le pad à taper ; correction dans
  `padlive/Challenge.kt` (`ChallengePanel`, lignes Ready / YourTurn, avec `padName`), une phrase neuve à traduire.
- T2 hors français, anglais, allemand : la consigne dit 럼블 / ランブル, le pad « Rumble » : `DefaultNames.kt` (modèle).

## À surveiller
- Feuille de partage Pro (capture `partage/loop_pro.png`) : « Autres fichiers » et « .zip » sont sous le fondu, il faut
  défiler ; le signe est un fondu de 24 dp, discret.
- U2 en « Auto » / « Club » : le niveau visé est lu sur l'échelle intégrée (−8,5 → « très fort »).
- Un ancien test (`NewSongPartSceneTest`) attendait l'ancien U28 : mis à jour, ce qu'il prouve est inchangé.

## Captures regardées
padlive_more, padlive_perf, padlive_recording, padlive_small, padlive_home, share_sheet, partage/loop_pro,
partage/loop_simple, tracks.

tests: *Lot6* *NewSongPartSceneTest *ShareSheet* *TrackBar* *ExportCard* *ProjectsTrash* *PadLiveTake* *TutoKit*

chemin: Morceau › chanson du Remix plus longue | test: Lot6VmSongOverrunTest#aSongLongerThanTheProjectSaysItsEndWillBeCut
chemin: Structure › + Nouvelle partie sur morceau vide | test: Lot6VmNewPartTest#aNewPartOnAnEmptySongKeepsTheLoopingPartFirst
chemin: Live › Fin d'une prise | test: Lot6VmTakeEndTest#endingATakeStopsThePlayback
chemin: Live › Écouter la prise | test: Lot6VmTakeListenTest#listeningToTheTakeKeepsItsLine
chemin: Partager › export après changement de partie affichée | test: Lot6VmExportStaleTest#theSceneShownAloneNeverMakesTheSongsExportStale
chemin: Partager Simple › Mix Pro, format | test: Lot6VmExportFormatTest#theFormatChosenInSimpleIsKeptInPro
chemin: Guide › Appliquer à tout le morceau, accords | test: Lot6VmChordsSaidTest#theChordsSaidAreTheOnesPlacedInTheSong
chemin: Tuto › Aller dans la partie pendant la lecture | test: Lot6VmTutoPartTest#goingToAPartWhileTheSongPlaysShowsItOrSaysItComes
chemin: Tuto › kit téléchargé › pad du grondement | test: Lot6VmTutoKitNameTest#theRumblesPadKeepsItsNameAfterTheKit
chemin: Partager Simple › niveau dit | test: Lot6UiShareLevelTest#theSimpleLevelIsTheExportsTarget
chemin: Partager Simple › niveau avant écoute | test: Lot6UiShareLevelTest#theSimpleLevelIsSaidBeforeAnyListen
chemin: Partager Simple › sans cible | test: Lot6UiShareLevelTest#withoutATargetTheSimpleLevelIsWhatIsHeard
chemin: Partager › cible du projet | test: Lot6UiShareLevelTest#theRouteGivesTheProjectsTarget
chemin: Partager Simple › Autres fichiers | test: Lot6UiShareFormatRowTest#otherFilesLineSimplePage
chemin: Partager Pro › .zip (italien) | test: Lot6UiShareFormatRowTest#zipLineProSheetItalian
chemin: Partager Simple › Nom du morceau | test: Lot6UiShareFormatRowTest#nameFieldShortSimplePage
chemin: Partager Pro › Nom du morceau | test: Lot6UiShareFormatRowTest#nameFieldShortProSheet
chemin: Partager › bas de la feuille | test: Lot6UiShareFormatRowTest#atTheEndNoSign
chemin: Partager Pro › champ du nom (italien) | test: Lot6UiShareNameLabelTest#italianProSheet
chemin: Partager Simple › champ du nom | test: Lot6UiShareNameLabelTest#frenchSimplePage
chemin: Mix Pro › ouverture | test: Lot6UiMixerOpensOnStripsTest#anExportAlreadyMadeOpensTheMixOnItsStrips
chemin: Mix Pro › fichier fait pendant l'ouverture | test: Lot6UiMixerOpensOnStripsTest#aFileMadeWithTheMixOpenStillComesIntoView
chemin: Mes projets › chargement | test: Lot6UiProjectsLoadingTest#whileTheCatalogueIsReadTheScreenSaysItWaits
chemin: Mes projets › liste déjà là | test: Lot6UiProjectsLoadingTest#aListAlreadyShownCarriesNoWait
chemin: Que veux-tu faire › titres alignés | test: Lot6UiLaunchMedallionsTest#everyTitleStartsOnTheSameLine
chemin: Que veux-tu faire › médaillons | test: Lot6UiLaunchMedallionsTest#everyMedallionIsRound
chemin: Sons › bouton ♪ Simple | test: Lot6UiTrackSoundWordTest#simple
chemin: Sons › bouton ♪ Pro | test: Lot6UiTrackSoundWordTest#pro
chemin: Sons › rangée des pistes (allemand) | test: Lot6UiTrackBarMoreTest#simpleGerman
chemin: Sons › rangée des pistes (coréen, Pro) | test: Lot6UiTrackBarMoreTest#proKorean
chemin: Sons › piste sous le chevron | test: Lot6UiTrackBarMoreTest#theChipUnderTheSignStillAnswers
chemin: Sons › rangée qui tient | test: Lot6UiTrackBarMoreTest#aRowThatFitsSaysNothing
chemin: Pad Live › bandeau Set gardé | test: Lot6PadKeptBandTest#padsKeepSizeAndPlaceWhenTheKeptBandShows
chemin: Pad Live › Plus ouvert | test: Lot6PadMoreIconTest#closeHasNoDotsIcon
chemin: Pad Live › Partager avec prise gardée | test: Lot6PadShareTest#oneShareButtonPerScreen
chemin: Pad Live › Partager sans prise | test: Lot6PadShareTest#withoutAKeptTakeTheShareStaysUnderPlus
chemin: Pad Live › pad en attente | test: Lot6PadWaitingRingTest#aPadTouchedWhileAnotherPlaysShowsThePaleRingUntilItsLine
chemin: Structure › copie de partie | test: NewSongPartSceneTest#aCopiedPartNeverTakesAnEmptySceneTheSongPlays
