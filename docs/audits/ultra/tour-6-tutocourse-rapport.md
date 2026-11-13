# Tour 6 — lot « tutocourse » : rapport

Fichier d'appli touché : `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt` (`check()`). Un test neuf : `Tour6TutoLateAnswerTest`. Base : `eq/tour-6-tutoordre2` (`326fadd4`).

## La cause commune
Trois classes du tuto ont rougi selon le passage de la CI : `Tour6TutoPanelAlreadyTest` (CI 43425565244, lot tutoordre), `TutoLot5ControllerTest#aWrongOrderIsSaid` (CI 43448175188, lot tutoordre2), `Tour6TutoFlechesRollTest` (CI 43449478539, lot remix2, sans fichier du tuto). Une seule cause : les regards du contrôleur sur le projet (`check()`) travaillent hors du fil principal, plusieurs à la fois (celui de chaque modification, et ceux que lancent l'arrivée sur une étape, le démarrage, les boutons). Un regard lent pouvait répondre APRÈS un plus récent et remettre ce dont le projet d'avant avait besoin. Exemples : « Copier » proposé par le regard sur le motif écrit, puis repris par le regard lancé à la création de la piste (l'échec de `Tour6TutoFlechesRollTest` ligne 97) ; le texte sur l'ordre des parties jamais dit (`aWrongOrderIsSaid`) ; « Déjà fait » perdu (`PanelAlready`). Ce n'est pas un état global entre classes : chaque classe passe seule et en tranche dans tous les ordres essayés ; c'est l'ordre des réponses, que la charge de la machine change. Sur un téléphone lent, le même défaut existe (le travail se fait sur `Dispatchers.Default`).

## Preuve
- `Tour6TutoLateAnswerTest#aLateLookNeverTakesBackWhatANewerOneOffered` : le regard lancé par « Créer la piste Basse » est retenu (travailleur de test) jusqu'à ce que le regard sur le motif écrit ait proposé « Copier » ; il répond ensuite. Rouge sur `ea3883b2` (le commit rouge, parent du vert) : « Copier » repris (`need` nul). Vert après. Aucune pause fixe : l'ordre est tenu par le travailleur retenu.
- `Tour6TutoOrderRaceTest` (lot tutoordre2) et `Tour6TutoPanelAlreadyTest#aLookBegunBeforeTheStepStillSaysAlreadyDone` (lot tutoordre) restent verts.
- Lancé : les quatre classes ensemble ; puis la suite du tuto entière deux fois de suite avec `--rerun` (résultat dans « Passages »).

## Correction
- `check()` ignore la réponse d'un regard sur un projet qui a changé depuis son départ (`p` n'est plus `project.value`, ni égal) : un regard plus récent suit toujours (celui des modifications court à chaque changement).
- Comme les regards périmés ne cochent plus rien, le besoin de l'étape et le texte sur l'ordre se disent dès que l'étape montrée n'est pas parmi ce que ce regard a trouvé (plus seulement quand il n'a rien trouvé) ; le texte d'ordre jamais sur une étape cochée (moyen 2 de la relecture de tutoordre2).
- rejeu impossible : défaut de la CI et de la mécanique, pas d'un siège ; remplacé par le test déterministe.

## Paires
- `rouge: a late look never takes back what a newer one offered` → `vert: a late look never takes back what a newer one offered`.

## Passages
- Local : suite du tuto (`com.pocketstudio.app.tuto.*`, 45 classes) deux fois de suite avec `--rerun` sur `5838dd92` : vertes les deux (8 min 12 s, 8 min 26 s).
- CI : 43474046159 sur `5838dd92` (le code), puis une seconde sur la tête du lot.

tests: com.pocketstudio.app.tuto.Tour6TutoLateAnswerTest com.pocketstudio.app.tuto.Tour6TutoOrderRaceTest com.pocketstudio.app.tuto.TutoLot5ControllerTest com.pocketstudio.app.tuto.Tour6TutoPanelAlreadyTest com.pocketstudio.app.tuto.Tour6TutoFlechesRollTest

chemin: Tuto de A à Z › jump up Facile › « La basse » › piste créée puis motif écrit › « Copier » | test: Tour6TutoLateAnswerTest#aLateLookNeverTakesBackWhatANewerOneOffered
chemin: Tuto › techno › « La basse » › motif écrit › « Copier » | test: Tour6TutoFlechesRollTest#houseBassAfterTheCopyTheRollShowsTheNextNoteAt130
