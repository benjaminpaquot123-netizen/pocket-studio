# Tour 6 — lot 3 « guide-et-coquille » : rapport

Source : `docs/audits/ultra/sieges/tour-6/lots-correction.md` (lot 3), `verdicts.tsv`, `lecture-recurrents.md` (branche `eq/tour-6-tri`). En cours de lot : N14 « piste audio seule » (reçu du lot 4), U9 et U1 (reçus du lot 6).
Fichiers touchés : `guide/GuidePanel.kt`, `guide/GuideViewModel.kt`, `ui/AppRoot.kt`, `ui/AppSpaces.kt`, `ui/DoneBar.kt`, les tests neufs et leurs captures. Deux anciens tests adaptés au nouveau comportement : `SpacesTest` (N17) et `SimpleModeAuditTest` (G1, clavier). Aucun fichier de traduction touché : tous les mots neufs à l'écran sont des textes déjà traduits dans les 15 langues.

tests: com.pocketstudio.app.nav.Tour6GuideCoversTest com.pocketstudio.app.nav.Tour6GuideLanguageTest com.pocketstudio.app.nav.Tour6GuideNextStepTest com.pocketstudio.app.nav.Tour6GuideAndTutoTest com.pocketstudio.app.nav.Tour6DoneBarTest com.pocketstudio.app.nav.Tour6HomeStopsMusicTest com.pocketstudio.app.nav.Tour6ModeSwitchAsksTest com.pocketstudio.app.nav.Tour6AudioPagesMoreTest com.pocketstudio.app.nav.Tour6RemixCancelTest com.pocketstudio.app.nav.Tour6MixerNamesTest com.pocketstudio.app.nav.Tour6PadLiveEffectsTitleTest com.pocketstudio.app.nav.SpacesTest com.pocketstudio.app.nav.SimpleModeAuditTest com.pocketstudio.app.nav.GuideJourneyTest

Méthode : chaque test a été écrit et vu rouge sur son assertion avant toute correction (un seul passage Gradle pour les dix premiers, un autre pour U9), puis les corrections, un commit « vert » par défaut. Téléphone de 360 x 780 dp, Simple et Pro, 100 % et 130 %.

## Corrigés (11 sur 11 du lot, plus U9)

### G1 — le Guide cache le contenu (3 choses)
- Le panneau ouvert se replie quand l'utilisateur change lui-même d'écran (onglet, « ⋮ Plus » du pouce ou du haut, page de Plus) ; sa barre reste. « Voir le réglage » du Guide n'est pas concerné.
- Quand la carte dépasse du panneau, son bord s'estompe et une ligne « ↓ La suite » sous la carte fait monter le reste ; elle part à la fin. Un premier essai posait ce signe par-dessus la carte : il prenait les touchers des boutons dessous (37 anciens tests du Guide rouges), retiré.
- Sur la page du clavier d'un écran court (moins de 700 dp), la barre repliée du Guide laisse sa ligne aux touches : 12 dp de touches visibles avant, environ 60 après (360 x 780 à 130 %).
- Reste : le siège F b #10 (360 x 780 à 100 %) est au-dessus du seuil ; le réglage fin de la page du clavier est dans `synth/SynthScreen.kt`, hors du lot.

chemin: Guide ouvert puis « ⋮ Plus » du pouce (Simple) | test: Tour6GuideCoversTest#thePlusPageOpenedFromTheThumbIsNotUnderTheOpenGuide
chemin: Guide ouvert puis « ⋮ » du haut à 130 % | test: Tour6GuideCoversTest#thePlusPageOpenedFromTheTopBarIsNotUnderTheOpenGuideWithBigText
chemin: Guide ouvert puis Plus › Réglages (Pro) | test: Tour6GuideCoversTest#aPageOfPlusIsNotUnderTheOpenGuideInPro
chemin: « Voir le réglage » du Guide garde le Guide à l'écran | test: Tour6GuideCoversTest#theGuidesOwnWayToAScreenKeepsItsBar
chemin: panneau du Guide, carte coupée (Simple) | test: Tour6GuideCoversTest#theOpenGuidesCutCardSaysMoreWaitsInSimple
chemin: panneau du Guide, carte coupée (Pro) | test: Tour6GuideCoversTest#theOpenGuidesCutCardSaysMoreWaitsInPro
chemin: panneau du Guide, carte coupée à 130 % | test: Tour6GuideCoversTest#theOpenGuidesCutCardSaysMoreWaitsWithBigText
chemin: Clavier d'un écran court à 130 % | test: Tour6GuideCoversTest#theGuidesBarLeavesTheKeysTheirRoomOnAShortScreen
chemin: Clavier d'un grand téléphone | test: Tour6GuideCoversTest#onATallScreenTheGuidesBarStaysOnTheKeysPage
chemin: Clavier à 130 %, touches au-dessus de la barre du Guide quand elle est là | test: SimpleModeAuditTest#theKeysAreAboveTheGuidesBarAt130
chemin: budget de gestes du Guide avec la ligne « La suite » | test: GuideJourneyTest#aFirstSongFollowedThroughTheGuideToTheExportThenResumedThreeDaysLater

### G2 — l'étape du Guide reste dans l'ancienne langue
La langue relance l'analyse du Guide : la barre et la carte sont réécrites dans la nouvelle langue en moins d'une seconde.

chemin: Réglages › Langue, français vers polonais (Pro) | test: Tour6GuideLanguageTest#theStepIsRewrittenFromFrenchToPolishInPro
chemin: Réglages › Langue, arabe vers français (Simple) | test: Tour6GuideLanguageTest#theStepIsRewrittenFromArabicToFrenchInSimple

### G3 — « À toi : touche « Plus tard » »
La phrase ne nomme plus un choix qui remet la carte à plus tard ; elle nomme l'écran à ouvrir (« Voir les parties »). Trois cartes étaient touchées : `start.parts`, `start.song` et, dans les morceaux de style, `arr.bass` (« À l'oreille, plus tard »). Ce choix n'est plus le gros bouton de la carte.

chemin: carte « Fais une deuxième partie » en polonais | test: Tour6GuideNextStepTest#aSecondPartAsksToSeeThePartsNotToPutItOff
chemin: toutes les cartes d'un morceau fait à la main (Pro) | test: Tour6GuideNextStepTest#noCardOfASongByHandAsksToPutItOff
chemin: toutes les cartes d'un morceau fait à la main (Simple) | test: Tour6GuideNextStepTest#noCardOfASongByHandAsksToPutItOffInSimple
chemin: toutes les cartes d'un morceau de style | test: Tour6GuideNextStepTest#noCardOfAStyleSongAsksToPutItOff

### T13 et N1 — le Guide et le tuto (un test et un correctif pour les deux : même condition dans AppRoot.kt)
Tant qu'un tuto tourne ou attend (« Reprendre le tuto »), la barre du Guide ne s'affiche pas. « Ouvrir le Guide de mon morceau » l'ouvre quand même, à la place de la ligne du tuto ; replié, il rend la place.
- Reste (N1, deuxième moitié) : les deux boutons de l'Aide aux noms proches sont dans `settings/SettingsScreen.kt` (lignes 212 et 217), hors du lot, et demandent un texte à traduire.

chemin: tuto quitté, « Reprendre le tuto » seul sous la page (Simple) | test: Tour6GuideAndTutoTest#aTutorialQuitShowsNoSecondGuideInSimple
chemin: tuto quitté, « Reprendre le tuto » seul sous la page (Pro) | test: Tour6GuideAndTutoTest#aTutorialQuitShowsNoSecondGuideInPro
chemin: tuto en cours, pas de barre du Guide | test: Tour6GuideAndTutoTest#aTutorialRunningShowsNoSecondGuide
chemin: tuto en cours, Plus › Aide › Ouvrir le Guide de mon morceau (Simple) | test: Tour6GuideAndTutoTest#duringATutorialTheGuideAskedForShowsInSimple
chemin: tuto en cours, Plus › Aide › Ouvrir le Guide de mon morceau (Pro) | test: Tour6GuideAndTutoTest#duringATutorialTheGuideAskedForShowsInPro
chemin: tuto quitté, Plus › Aide › Ouvrir le Guide de mon morceau | test: Tour6GuideAndTutoTest#afterATutorialQuitTheGuideAskedForShows

### N15 — « Fait : … Annuler » recouvrait des boutons
La ligne a sa place dans la barre du bas, au-dessus du Guide et des onglets ; plus rien de touchable dessous.

chemin: « Fait : … » sur les cinq onglets du Mode simple | test: Tour6DoneBarTest#theDoneLineCoversNothingToTouchInSimple
chemin: « Fait : … » sur les espaces du Pro | test: Tour6DoneBarTest#theDoneLineCoversNothingToTouchInPro
chemin: « Fait : … » à 130 % | test: Tour6DoneBarTest#theDoneLineCoversNothingToTouchWithBigText

### N16 — la musique s'arrête sans le dire au retour de Mes projets
Décision dans `docs/decisions/tour-6-guidecoquille.md` : le retour à « Que veux-tu faire ? » reste, la musique s'y arrête, et un message le dit quand elle jouait : « ■ Arrêté · Ton morceau est sauvegardé. ». Même chose pour « Accueil » de Plus.

chemin: Plus › Mes projets › « ← », musique en cours (Pro) | test: Tour6HomeStopsMusicTest#backFromMyProjectsSaysTheMusicStoppedInPro
chemin: « ⋮ Plus » › Mes projets › « ← », musique en cours (Simple) | test: Tour6HomeStopsMusicTest#backFromMyProjectsSaysTheMusicStoppedInSimple
chemin: le même en polonais | test: Tour6HomeStopsMusicTest#backFromMyProjectsSaysTheMusicStoppedInPolish
chemin: Plus › Accueil, musique en cours | test: Tour6HomeStopsMusicTest#homeFromPlusSaysTheMusicStopped
chemin: Plus › Mes projets › « ← », rien ne joue | test: Tour6HomeStopsMusicTest#nothingIsSaidWhenNothingPlayed

### N17 — « Passer en mode Simple » sans question
L'entrée de Plus ouvre « Mode de ce projet » (la question de la pastille du haut) ; le mode change au toucher d'une carte.

chemin: Plus › Passer en mode Simple (Pro) | test: Tour6ModeSwitchAsksTest#switchToSimpleFromPlusAsksFirst
chemin: « ⋮ Plus » › Passer en mode Pro (Simple) | test: Tour6ModeSwitchAsksTest#switchToProFromTheSimplePlusAsksFirst
chemin: changer de mode ne change pas le projet | test: SpacesTest#switchingModesNeverChangesTheProject

### R19 — les pages de Sons › Voix coupées au bord (la part d'AppRoot.kt)
La rangée « Pistes audio / Remix / Accordeur / Générer » s'estompe au bord où des pages attendent et y montre « › » (« ‹ » de l'autre côté). Les autres rangées citées par R19 sont aux lots 5 et 6.

chemin: Sons › Voix, rangée des pages en turc à 130 % | test: Tour6AudioPagesMoreTest#theCutRowOfPagesSaysItGoesOnInTurkishWithBigText
chemin: Sons › Voix, rangée des pages en français à 130 % | test: Tour6AudioPagesMoreTest#theCutRowOfPagesSaysItGoesOnInFrenchWithBigText
chemin: Sons › Voix sur tablette, la rangée tient | test: Tour6AudioPagesMoreTest#aRowThatFitsShowsNoSign

### U17 — « Annuler » de l'import laissait dans le projet
Quand l'import vient de la page Remix de l'accueil, « Annuler » y ramène. Testé avec le projet ouvert choisi dans la liste ; le chemin « un autre projet » passe par la même ligne de code, mais la reconstruction de l'écran sur un autre projet n'est pas jouée par ce test.

chemin: Accueil › Remix › Dans un projet existant › Annuler | test: Tour6RemixCancelTest#cancelOfTheImportIntoAnExistingProjectLeadsBackToTheRemixPage

### U27 — le Mix ne nommait pas les pistes comme Sons
Pendant un tuto, la tranche « Acid 303 » muette n'est plus retirée du Mix : une tranche pour chaque piste que Sons nomme.
- Reste : « Voix » de Sons est la page du micro, sans tranche tant qu'aucune prise n'existe ; le dire dans le Mix demande `mixer/MixerScreen.kt` (lot 6) et une phrase à traduire (lot 5).

chemin: tuto Pro › Mix, tranche Acid 303 | test: Tour6MixerNamesTest#duringTheProTutorialTheMixNamesTheAcidAsSonsDoes
chemin: Mix hors tuto, tranche Acid 303 | test: Tour6MixerNamesTest#theMixNamesTheAcidAsSonsDoes

### U9 (reçu du lot 6) — « Effets · Remix » ouvert depuis Pad Live
Ouverte depuis Pad Live, la boîte d'effets est titrée « Effets · Pad Live ».

chemin: Pad Live › Plus › Effets | test: Tour6PadLiveEffectsTitleTest#theEffectsOpenedFromPadLiveAreTitledPadLive

## Laissés de côté (reçus en cours de lot)
- **N14, cas « piste audio seule »** (reçu du lot 4) : la phrase « Ton morceau est vide » et sa condition sont dans `session/SessionScreen.kt` (`SongCard`, ligne 383 ; `SessionScreenUi`, ligne 112), fichier du lot 4. AppRoot.kt ne fait que remplir `SessionScreenUi`, qui n'a pas de champ pour une piste audio. Il faut un champ de plus dans SessionScreen.kt, lu par la condition de la ligne 383, puis une ligne dans AppRoot.kt (`SessionScreenUi(…)`, vers la ligne 2800).
- **U1** (reçu du lot 6) : le lot 7 a préparé l'en-tête (`PlayPosition.next`, « %s : ensuite » dans `ui/PlayStrip.kt`), mais l'appel qui doit lui donner la partie en file n'est pas dans AppRoot.kt : c'est `ui/UnderTopBar.kt`, ligne 60 (`playPosition(project, playing, songIndex, bar.value, playFrom, loopPart)`, à compléter par `queued`), et `report/ReportWiring.kt`, ligne 43, pour le rapport de bug. Aucun des deux n'est un fichier du lot.

## Captures regardées
`app/screenshots/tour6/shell_*.png` (classe `Tour6GuideCoquilleLookTest`, 24 images, 360 x 780 dp) : panneau du Guide avec « ↓ La suite » puis fin de carte (Simple et Pro, 100 % et 130 %), Plus après le Guide ouvert, clavier à 100 % et 130 %, ligne « Fait » (Simple et Pro, 100 % et 130 %), « Mode de ce projet » depuis Plus, pages de Sons › Voix (français 100 %, turc 130 %), tuto quitté, Guide demandé pendant un tuto, Mix pendant un tuto, carte « Fais une deuxième partie ». Cette classe n'est pas une classe du recensement des écrans (`ScreenRegistry`) : ce sont des états d'écrans déjà recensés.

## À dire au relecteur
- Aucun rejeu ajouté, retiré ni changé ; rien touché sous `scripts/bench/`, `docs/audits/ultra/portail/`, les fichiers de traduction.
- Les commits « vert » ont été découpés par défaut après le passage vert de l'ensemble : chaque commit intermédiaire n'a pas été testé seul, seul l'état final l'a été.
- T13 et N1 partagent un commit rouge et un commit vert (même condition dans AppRoot.kt).
- Le découpage par défaut laisse quelques lignes communes (G1 s'appuie sur T13/N1, U9 vient après).
