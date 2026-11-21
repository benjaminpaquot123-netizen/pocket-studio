# Tour 9, lot « flash » (studio entre le logo et Bienvenue) : rapport

Branche : eq/tour-9-flash (venue de 8ee635e44). Rouge 1359f0cff, vert ca8b0d2bf ; banc : vert 0836068ee.
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: *HomeAndPadsTest *LaunchFlowTest *ScaledWindowsTest *FirstLaunchTest *NavScreenshotTest *PremiereInstallationTest *BenchSelfTest

## Chemins

chemin: Premier lancement, logo puis Bienvenue (le studio jamais entre les deux) | test: HomeAndPadsTest#theWelcomeIsUnderTheOpeningAtTheFirstLaunch
chemin: Premier lancement avec l'écran « Que veux-tu faire ? » demandé : Bienvenue d'abord | test: HomeAndPadsTest#theFirstLaunchWelcomeWinsOverTheHomeScreen
chemin: Premier lancement, rotation pendant le logo : Bienvenue reste | test: HomeAndPadsTest#theFirstLaunchWelcomeStaysThroughARotation
chemin: Banc, écran Bienvenue au premier lancement : la note ne nomme que Bienvenue | test: PremiereInstallationTest#aFailedStepSaysWhatWasOnScreen
chemin: Banc, écran Bienvenue : la carte coupée par le bord est dite « coupé » | test: BenchSelfTest#theScreenInTextSaysWhatIsCutNotAnArrowTheAppDoesNotDraw

## Ce qui change (« tenté »)
- Banc (`InAppDriver.kt`, `PremiereInstallationTest.kt`, `BenchSelfTest.kt`) : `ecran()` saute les nœuds effacés par un ancêtre ; voir docs/decisions/tour-9-flash.md.
- Cause : `FirstLaunch` ouvrait une fenêtre (`ScaledDialog`) seulement après le logo (`!Intro.playing`) ; entre la fin du logo et l'ouverture de la fenêtre, le studio se voyait.
- `Welcome.kt` : `FirstLaunch(inline = true)` dessine Bienvenue dans l'écran (`welcome_page`, marges sûres, `BackHandler` sur le même `dismiss`) ; le contenu et la fenêtre « démo prête » sont inchangés.
- `AppRoot.kt` : Bienvenue est montrée dès le départ, sous le logo, par-dessus le studio et hors de `key(simple)` ; le studio est couvert et muet pour l'accessibilité tant que `guideSeen` est faux.

## Tests lancés
- HomeAndPadsTest 11/11, LaunchFlowTest 12/12, onboarding.* (46, 1 sauté), nav.ScaledWindowsTest, nav.PadLiveReadyEveryWayTest, ShellFixesTest, ForegroundSoundTest, SpacesTest, RecordVoiceWaysTest, upgrade.FirstLaunchTest 23, LanguagesScanTest 110, OneTouchSongPlaysTest, JourneyPathsTest, RemixFindableTest, RetoursClearOfControlsTest, bentest.* sauf un : verts.
- verifyRoborazzi NavScreenshotTest#welcomeMode : capture inchangée.
- Après le banc, sur 0836068ee : bentest.*, human.Bench*, guide.Tour7* : 513 tests, 0 échec, 129 sautés (assume).

## Ce qui n'est pas vert
- Autres tests qui utilisent `firstLaunch` (session.Tour8/9*, tuto.*, ui.Coquille*) : non lancés (disque, tests ciblés).
- CI complète non lancée.

## Demandes (hors de mes fichiers)
Aucune (les fichiers du banc ont été ajoutés au lot par le chef).
