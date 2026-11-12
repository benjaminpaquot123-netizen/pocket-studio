# Tour 5 — lot « gaucher » : rapport

Source : rejeux en attente t5-04, t5-05, t5-03 (`docs/audits/ultra/rejeux-attente/`), synthèse du tour 5 (F d #2, #34 ; B c #23).
Rejoués le 2026-11-12 sur la base `db66a3ce` (autotest du banc vert : 172 verts, 0 rouge) avant toute correction.

tests: com.pocketstudio.app.ui.ModeStepThumbTest com.pocketstudio.app.nav.RemixBothWaysTest com.pocketstudio.app.nav.ModeChoiceTest com.pocketstudio.app.human.Tour5BloquantsTest

## 1. t5-04 — cartes « Simple ou Pro ? » hors du pouce gauche — corrigé
- Rejeu avant : « non tenu » (la carte Simple encore en 308,297).
- `ModeStep` : titre en haut, explication et cartes en bas ; un écran court défile comme avant. Même composant pour l'accueil, Nouveau projet, le Remix, l'assistant et J'apprends.
- Rouge e067da62 (3 échecs : Simple en 308,246 sur 616x1371, hors du pouce gauche), vert b71ad163.
- Captures regardées en entier : téléphone 100 % (`nav/mode_step`, `nav/mode_step_learn`), 130 % (`scale130/fr_`, `nl_`), tablette debout et couchée. L'écran est le même en Simple et en Pro, pour la main gauche comme pour la main droite (pas de réglage de main dans l'appli) ; le test vérifie les deux pouces, à 100 % et à 130 %.

chemin: Simple ou Pro ? (accueil, Nouveau projet, Remix) à 100 % | test: ModeStepThumbTest#bothCardsWithinEitherThumb
chemin: Simple ou Pro ? à 130 % | test: ModeStepThumbTest#bothCardsWithinEitherThumbAtBigText
chemin: Simple ou Pro ? de J'apprends (niveau du tuto) à 130 % | test: ModeStepThumbTest#bothTutorialCardsWithinEitherThumbAtBigText

## 2. t5-05 — « Dans un nouveau projet » retombait sur « Simple ou Pro ? » — corrigé
- Rejeu avant : « non tenu », mais il dévie dès le pas 8 (boutons de l'accueil déplacés) et n'atteint pas le Remix : la preuve est le test, pas le rejeu.
- Depuis le Remix, les deux chemins « nouveau projet » ouvrent le projet dans le dernier mode choisi, sinon dans le mode affiché, sans l'enregistrer comme choix (décision dans `docs/decisions/tour-5-gaucher.md`).
- Rouge b0f57be7 (2 échecs : « Simple ou Pro ? » demandé), vert 78e948b4. Trois anciens tests du Remix ne touchent plus de carte de mode après ces chemins.
- Aucun écran redessiné (une question en moins) : pas de capture nouvelle.

chemin: Remix › Que faire de… › Dans un nouveau projet (Simple) | test: RemixBothWaysTest#fromHomeANewProjectOpensInTheModeShownWithoutAskingAgain
chemin: Remix › Que faire de… › Faire un morceau à partir de cette chanson (Pro) | test: RemixBothWaysTest#fromHomeASongBuiltInProOpensInProWithoutAsking
chemin: Remix › Que faire de… › Faire un morceau (chanson, tempo, tonalité) | test: RemixBothWaysTest#fromHomeASongBecomesANewSongAtItsTempoAndKey
chemin: Remix › Que faire de… › Dans un nouveau projet (chanson muette) | test: RemixBothWaysTest#fromHomeIntoANewEmptyProjectTheSongWaitsSilent
chemin: Nouveau projet (les autres départs demandent le mode tant qu'aucun n'a été choisi) | test: ModeChoiceTest#noWayToStartSkipsTheModeAndItsBackMakesNothing

## 3. t5-03 — Swing du tuto Pro — l'appli est correcte, c'est le rejeu qui se trompe
- Rejeu sur la base : pas de verdict, il s'arrête au geste 14 (« rien ne change »). Au geste 1, « Ich lerne » est manqué ; au geste 2, le doigt tombe sur « Weiter » (continuer le projet) ; il n'entre jamais dans le tuto. Journal : `docs/audits/ultra/preuves-gaucher/t5-03-rejeu-journal.txt`.
- Le chemin du siège B c refait par étiquettes (allemand, tablette 8 pouces, Pro, tempo 132, Fa phrygien, « Ich mach's ») montre le Swing au-dessus du panneau du tuto : test vert sur la base (correctif 8f3cd940 du lot « bloquants »).
- Rien changé dans l'appli. Le rejeu est à convertir en étiquettes (équipe des rejeux) ; son geste 2 visait aussi la carte Pro, déplacée par t5-04.

chemin: Tuto Pro › chapitre 1 › étape Swing › Ich mach's | test: Tour5BloquantsTest#theTutorialsSwingStepShowsTheSwingSetting

## À dire au relecteur
- Aucun rejeu ajouté, retiré ni changé ; rien touché sous `rejeux-attente/`, `scripts/bench/`, Mix, BenStudio Test.
- Le rouge t5-05 a été retouché avant le push (vérification sûre pendant que l'appli se reconstruit), puis rejoué : il échoue toujours sur son assertion.
- Autotest du banc : un premier passage rouge sur `BenchSeparationWaitTest` (chrono, machine froide), vert seul puis vert en entier.
