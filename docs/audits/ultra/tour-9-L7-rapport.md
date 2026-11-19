# Tour 9, lot L7 : rapport

Correcteur, branche `eq/tour-9-L7` (base dca571da7). Zone : le rouleau de notes (`synth/PianoRoll.kt` ; `synth/SynthScreen.kt` lu, pas touché).

tests: :app:testDebugUnitTest --tests '*PianoRoll*Dotted*'

Les méthodes sont celles de la classe abstraite `PianoRollBigTextDotted`, jouées par ses trois classes : `PianoRollBigTextDottedTest` (360 dp, 200 %), `PianoRollBigTextDottedFoldableTest` (pliant ouvert, 200 %), `PianoRollMidTextDottedTest` (360 dp, 130 %).

chemin: Tuto speed garage, basse, Mode simple, lettres 200 % : chaque note en pointillé touchée | test: PianoRollBigTextDotted#speedGarageBassSimple
chemin: Tuto speed garage, basse, Mode Pro, lettres 200 % : chaque note en pointillé touchée | test: PianoRollBigTextDotted#speedGarageBassPro
chemin: Tuto speed garage, accords, Mode simple, lettres 200 % | test: PianoRollBigTextDotted#speedGarageChordsSimple
chemin: Tuto grime, accords, Mode simple, lettres 200 % | test: PianoRollBigTextDotted#grimeChordsSimple
chemin: Tuto speed garage, basse, bouton du panneau retouché entre deux notes (siège 62) | test: PianoRollBigTextDotted#speedGarageBassAskedAgainSimple
chemin: Tuto, téléphone pliant ouvert, lettres 200 % (PianoRollBigTextDottedFoldableTest) | test: PianoRollBigTextDotted#speedGarageBassSimple
chemin: Tuto, lettres 130 % (PianoRollMidTextDottedTest) | test: PianoRollBigTextDotted#speedGarageBassSimple
chemin: Tuto, basse, Mode Pro, glissé le long d'une note en pointillé (siège 17) | test: PianoRollBigTextDotted#dragAlongDottedBassPro
chemin: Tuto, basse, Mode simple, glissé le long d'une note en pointillé | test: PianoRollBigTextDotted#dragAlongDottedBassSimple
chemin: Tuto, lettres 130 % (PianoRollMidTextDottedTest), glissé le long d'une note en pointillé | test: PianoRollBigTextDotted#dragAlongDottedBassPro

corrige: D06 | gravité: bloquant | rejeu impossible : rejoué sans le banc, consigne du chef, par PianoRollBigTextDottedTest (rouge e5e8f6ccd, vert c68d882b4)
corrige: S17 | gravité: grave | rejeu impossible : rejoué sans le banc, consigne du chef, par dragAlongDotted* (rouge 0d741b65a, vert d04041940)

## Phrases d'acceptation
- D06 : « En Simple comme en Pro, à 130 % comme à 200 %, chaque note en pointillé que je touche se pose, le compteur « encore N notes » baisse d'autant, et il reste toujours une note en pointillé en vue (ou une touche sur la ligne en montre une) jusqu'à la fin de l'étape. »
- S17 : « Un glissé qui part d'une note en pointillé la pose entière, comme un toucher ; le rouleau ne défile pas. »

## Ce qui a changé
- D06 : à grosses lettres, le haut de la page et le panneau du tuto ne laissaient que 0 à 10 px de grille en vue. Rien ne faisait monter la page en tenant compte du panneau, donc aucune note en pointillé n'était touchable et la ligne « encore N notes » attendait des rangées qui ne venaient jamais. Le rouleau demande maintenant à la page de le montrer au-dessus du panneau quand moins de 4 rangées sont en vue.
- S17 : un glissé qui part d'une note en pointillé pas encore posée appartient à la grille : la note (ou l'accord en mode « Accords ») est posée quand le doigt se lève, et le rouleau ne défile pas.

## Laissés de côté
- Siège 17, geste 157 : « Eu faço » revient en boucle sur des notes déjà posées. Cause probable dans la validation du tuto (TutoChecks / TutoController, lot L3), hors de ma liste.
- `TutoJourneyTest` (jump_up) échoue parfois sous une grosse charge. Il passe seul, avec et sans la correction.

## Recette pour le téléphone de Benjamin
Tuto speed garage, Mode simple, lettres à 200 %, étape de la basse : toucher chaque note en pointillé, puis en glisser une le long d'elle. Chaque note se pose, le compteur baisse et le rouleau ne défile pas.
