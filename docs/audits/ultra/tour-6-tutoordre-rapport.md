# Tour 6 — lot « tutoordre » : rapport

Fichier d'appli touché : `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt` (une condition). Un test neuf dans `Tour6TutoPanelAlreadyTest`.

## Le défaut (CI rouge)
- CI 43425565244 (tête `ce0aed6fe`, 8878 tests, 1 échec) : `Tour6TutoPanelAlreadyTest#aStepTheProjectHeldAlreadyIsSaidAlreadyDone`, ligne 84 (« Déjà fait » absent).
- **Ce n'est pas une fuite d'état entre tests** : lancé seul, après `Tour6TutoFlechesRollTest`, ou en tranche (FlechesRoll, PadNames, PanelAlready), le test passe. C'est une course : un regard sur le projet commencé sur l'étape d'avant (au démarrage) répond après l'arrivée sur l'étape « tempo ». Il coche l'étape (le projet la tient déjà) mais compare l'étape où il a commencé ; le regard demandé par l'arrivée vient ensuite, trouve l'étape déjà cochée, et ne dit rien non plus. Une machine chargée (la CI) tombe dans cet ordre.
- **Reproduction déterministe** : `Tour6TutoPanelAlreadyTest#aLookBegunBeforeTheStepStillSaysAlreadyDone` — le travail du premier regard se fait hors du fil principal avant l'arrivée sur l'étape, sa réponse est lue après. Rouge sur `713d66f6` (sur l'assertion « Déjà fait »), vert sur `ab8fd49c`.
- **Correction** : `check()` compare l'étape montrée au moment de sa réponse (`_ui.value.current`), plus celle où il a commencé. La garde « rien n'a changé depuis l'arrivée » (`arrived == p`) reste.
- Sur un vrai téléphone : la même course existe (« Déjà fait » pouvait manquer sur une étape déjà juste à l'arrivée) ; corrigée du même coup.
- rejeu impossible : défaut de la CI, pas d'un siège ; remplacé par le test déterministe ci-dessus.

## Paires
- `rouge: tuto already-done lost when a project look straddles the step change` → `vert: tuto already-done lost when a project look straddles the step change`.

tests: com.pocketstudio.app.tuto.Tour6TutoPanelAlreadyTest com.pocketstudio.app.tuto.Tour6TutoFlechesRollTest

chemin: Tuto de A à Z › techno Facile › étape « tempo » déjà juste à l'arrivée › « Déjà fait » | test: Tour6TutoPanelAlreadyTest#aLookBegunBeforeTheStepStillSaysAlreadyDone
chemin: Tuto de A à Z › techno Facile › étape « tempo » déjà juste à l'arrivée › « Déjà fait » | test: Tour6TutoPanelAlreadyTest#aStepTheProjectHeldAlreadyIsSaidAlreadyDone
