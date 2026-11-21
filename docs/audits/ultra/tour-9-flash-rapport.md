# Tour 9, lot « flash » (studio entre le logo et Bienvenue) : rapport

Branche : eq/tour-9-flash (venue de 8ee635e44). Rouge 1359f0cff, vert ca8b0d2bf.
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: *HomeAndPadsTest *LaunchFlowTest *ScaledWindowsTest *FirstLaunchTest *NavScreenshotTest

## Chemins

chemin: Premier lancement, logo puis Bienvenue (le studio jamais entre les deux) | test: HomeAndPadsTest#theWelcomeIsUnderTheOpeningAtTheFirstLaunch
chemin: Premier lancement avec l'écran « Que veux-tu faire ? » demandé : Bienvenue d'abord | test: HomeAndPadsTest#theFirstLaunchWelcomeWinsOverTheHomeScreen
chemin: Premier lancement, rotation pendant le logo : Bienvenue reste | test: HomeAndPadsTest#theFirstLaunchWelcomeStaysThroughARotation

## Ce qui change (« tenté »)
- Cause : `FirstLaunch` ouvrait une fenêtre (`ScaledDialog`) seulement après le logo (`!Intro.playing`) ; entre la fin du logo et l'ouverture de la fenêtre, le studio se voyait.
- `Welcome.kt` : `FirstLaunch(inline = true)` dessine Bienvenue dans l'écran (`welcome_page`, marges sûres, `BackHandler` sur le même `dismiss`) ; le contenu et la fenêtre « démo prête » sont inchangés.
- `AppRoot.kt` : Bienvenue est montrée dès le départ, sous le logo, par-dessus le studio et hors de `key(simple)` ; le studio est couvert et muet pour l'accessibilité tant que `guideSeen` est faux.

## Tests lancés
- HomeAndPadsTest 11/11, LaunchFlowTest 12/12, onboarding.* (46, 1 sauté), nav.ScaledWindowsTest, nav.PadLiveReadyEveryWayTest, ShellFixesTest, ForegroundSoundTest, SpacesTest, RecordVoiceWaysTest, upgrade.FirstLaunchTest 23, LanguagesScanTest 110, OneTouchSongPlaysTest, JourneyPathsTest, RemixFindableTest, RetoursClearOfControlsTest, bentest.* sauf un : verts.
- verifyRoborazzi NavScreenshotTest#welcomeMode : capture inchangée.

## Ce qui n'est pas vert
- `bentest.PremiereInstallationTest#aFailedStepSaysWhatWasOnScreen` rouge : la note commence par les étiquettes du studio couvert (`top_projects, top_mode…, welcome…`) au lieu de `welcome, welcome_*`. Cause : `InAppDriver.ecran()` lit l'arbre non fusionné, qui garde les nœuds effacés par `clearAndSetSemantics`, et Bienvenue n'est plus une fenêtre du dessus (même limite qu'avec « Que veux-tu faire ? » en ligne).
- `human.BenchSelfTest#theScreenInTextSaysWhatIsCutNotAnArrowTheAppDoesNotDraw` rouge : la carte « Extraire la voix » n'est plus « coupée » mais hors écran. Cause : Bienvenue en ligne est au-dessus de la bande des retours (60 px), comme tout écran en ligne ; la fenêtre la recouvrait. Il manque ~6 px pour qu'elle dépasse.
- Autres tests qui utilisent `firstLaunch` (guide.Tour7*, session.Tour8/9*, tuto.*, ui.Coquille*, human.Bench*) : non lancés (disque, tests ciblés).

## Demandes (hors de mes fichiers)
- Lot banc (L9) : `InAppDriver.ecran()` doit sauter les nœuds dont un ancêtre a effacé la sémantique (ou lire l'arbre fusionné pour les étiquettes) ; puis `PremiereInstallationTest` repasse.
- Lot banc (L9) : `BenchSelfTest` « coupé » : prendre un téléphone un peu plus haut (`w392dp-h900dp-hdpi`) ou chercher la carte coupée sans dépendre de la bande des retours.
