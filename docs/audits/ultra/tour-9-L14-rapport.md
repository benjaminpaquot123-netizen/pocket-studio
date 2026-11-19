# Tour 9, lot L14 « panneau tuto, bandeau du kit, fin du tuto » : rapport

Branche : eq/tour-9-L14, partie de l'intégration ccr-80af1a1b-mn0wec.
Défauts visés : F012 à F022 (finitions, `docs/audits/ultra/tour-9-tri/finitions.tsv`). Tentés 8 sur 11, aucun « corrigé » (un testeur neuf le dira).
Le registre `corrections.tsv` n'a aucune ligne pour ces finitions : le lot n'en ajoute pas (règle de `reparations.py`), l'état « tenté » est écrit ici.

tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.Tour9L14TutoPanelTest' --tests 'com.pocketstudio.app.nav.Tour9L14TutoEndTest' --tests 'com.pocketstudio.app.tuto.TutoKeySheetDoneTest'
chemin: Carte « Bravo » | test: Tour9L14TutoPanelTest#bravoReadsWhole
chemin: Panneau tuto, Arrêter la démo | test: Tour9L14TutoPanelTest#demoMakesNextAndPreviousInert
chemin: Panneau tuto, boutons voisins | test: Tour9L14TutoPanelTest#neighboursShareTextSize
chemin: Panneau tuto replié (garde) | test: Tour9L14TutoPanelTest#foldedWordsUnfoldInOneTouch
chemin: Bandeau « Télécharger le kit », carte de la grille | test: Tour9L14TutoPanelTest#kitLeavesTheMapWhole
chemin: Bandeau du kit, téléchargement | test: Tour9L14TutoPanelTest#kitDownloadingShowsOnlyItsProgress
chemin: Bandeau du kit épinglé, téléchargement | test: Tour9L14TutoPanelTest#kitDownloadingPinnedShowsOnlyItsProgress
chemin: Bandeau du kit, taille sur une ligne | test: Tour9L14TutoPanelTest#kitSizeStaysOnOneLine
chemin: Bandeau du kit, « Plus tard » | test: Tour9L14TutoPanelTest#kitNotOfferedAgainAfterLater
chemin: Feuille « Tonalité du morceau » | test: Tour9L14TutoPanelTest#keySheetChoosesNothingAheadAndStaysPut
chemin: Feuille « Tonalité du morceau », note visée atteinte | test: TutoKeySheetDoneTest#theKeyAskedForReachedIsSaid
chemin: Fin du tuto, en-tête | test: Tour9L14TutoPanelTest#endShowsOnlyFinished
chemin: Fin du tuto, menu sans « Reprendre » | test: Tour9L14TutoEndTest#finishedTutorialOffersNoResume
chemin: Feuille d'effets + panneau tuto | test: Tour9L14TutoPanelTest#stepTickedUnderTheEffectsWindowSaysSo
chemin: Panneau tuto, « Lire la suite » (garde) | test: Tour9L14TutoPanelTest#moreOnlyWhenMore

corrige: F012 | gravité: mineur
corrige: F013 | gravité: mineur
corrige: F014 | gravité: mineur
corrige: F016 | gravité: mineur
corrige: F017 | gravité: mineur
corrige: F018 | gravité: mineur
corrige: F019 | gravité: mineur
corrige: F021 | gravité: mineur
ne corrige pas: F015 : non reproduit à neuf, garde gardée
ne corrige pas: F020 : compteur tenu par TutoChecks.kt, hors de mes fichiers
ne corrige pas: F022 : non reproduit à neuf, garde gardée

## Ce qui change (tous « tentés »)
- F012 : la carte « Bravo » se lit en entier (trophée et deuxième ligne), plus de « ⋯ » d'arrêt dessus. Rouge e4022aa4, vert 7d5310c4.
- F013 : pendant la démo, « Étape suivante » et « Précédent » sont inertes, plus de message « Cette étape… » en touchant Arrêter. Rouge 2b19b84b, vert d19c52fa.
- F014 : les boutons voisins du panneau tuto ont la même taille de texte (partie TutoPanel). Rouge 7b89a4e0, vert 1ea40484.
- F016 : le bandeau du kit ne cache plus la carte de la grille. Rouge 231757d7, vert 60f8bcc3.
- F017 : pendant le téléchargement, le bandeau ne montre que la progression (bouton inactif). Même paire que F013 (2b19b84b, d19c52fa).
- F018 : la feuille « Tonalité du morceau » ne choisit rien d'avance, seule la puce attendue est en pointillés, la feuille ne saute plus. Rouge 340a1697, vert 0dc71449.
- F019 : en fin de tuto, l'en-tête dit « Tuto fini : bravo ! », plus d'« Étape suivante » ni de « Reprendre ». Rouge 359de392, vert e779c6d3.
- F021 : sous la feuille d'effets, une étape réussie s'annonce par « Bravo, c'est réussi ! » au-dessus de la feuille (2,5 s). Rouge 004ef652, vert 0082f363.
- F015, F022 : non reproduits à neuf ; tests de garde 92e51a5c et 0a5de0c9.

## Ce qui n'est pas vert
- F014 : la barre du haut (`ThumbBar.kt`, `Spaces.kt`) garde ses tailles, hors de mes fichiers.
- F015 : la part vue par t9-74 vient du mixeur simple (`mixer/SimpleMix.kt`), hors de mes fichiers.
- F020 : le compteur d'étape vient de `project-model/.../tuto/TutoChecks.kt` et du `tuto.json` : hors de mes fichiers.
- Contrôle du tuto (débutant neuf) : reporté en mode boost (`PS_JUGE_BOOST=1`), à faire à la grande vérification.

## Demandes (hors de mes fichiers)
Demande : F014, `ui/ThumbBar.kt` et `ui/Spaces.kt` : même taille de texte pour les boutons voisins de la barre du haut.
Demande : F015 (t9-74), `mixer/SimpleMix.kt` : la consigne repliée est cachée sous le mixeur simple.
Demande : F020 (t9-73), `project-model` `tuto/TutoChecks.kt` et `i18n/*/tuto.json` : le compteur doit suivre la consigne.

## Captures regardées (écran entier)
Voir la section ajoutée après la vérification Roborazzi.
- `TutoScreenshotTest` (12 captures) : seule `tuto_key_sheet` change (F018) : plus de « Phrygienne » ni de « Fa » choisis d'avance, les deux puces attendues restent en pointillés ; le reste de la feuille (titre, Fermer, gammes, toniques) est identique et aligné. Réenregistrée.
