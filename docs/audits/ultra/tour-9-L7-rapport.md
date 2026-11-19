# Tour 9, lot L7 : rapport

Correcteur, branche `eq/tour-9-L7` (base dca571da7). Zone : le rouleau de notes et le synthé (`synth/PianoRoll.kt`, `synth/SynthScreen.kt`, `synth/TrackBar.kt` ; `synth/ScaleKeys.kt` lu, pas touché). Seconde passe : les finitions F112 à F118 et F142 du tri (`docs/audits/ultra/tour-9-tri/finitions.tsv`).

tests: *PianoRoll*Dotted* *Tour9RollFinish*

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
chemin: Tuto, basse, Mode Pro, glissé qui part juste à côté d'une note en pointillé (rejeu du siège 17) | test: PianoRollBigTextDotted#dragFromBesideDottedBassPro
chemin: Tuto, basse, Mode simple, glissé qui part juste à côté d'une note en pointillé | test: PianoRollBigTextDotted#dragFromBesideDottedBassSimple

chemin: Rouleau, téléphone 360 dp, glissés vers le haut puis vers le bas sur les touches (t9-20) | test: Tour9RollFinishTest#aDragOnTheRollLeavesItsHeaderInSight
chemin: Rouleau d'une piste Accords, mode « Accords », note demandée par la consigne | test: Tour9RollFinishTest#theNoteLitInAChordIsItsBottomOne
chemin: Rouleau d'une piste Mélodie, ouverture | test: Tour9RollFinishTest#aMelodyTrackOpensWithoutTheGhostBassTicked
chemin: Rouleau, téléphone 360 dp, lettres 200 % : noms des touches et « mes. » | test: Tour9RollFinishTest#theRollsKeyAndBarNamesFitTheirPlaceAtTwoHundredPercentOnAPhone
chemin: Rouleau, tablette 800 dp, lettres 200 % : noms des touches et « mes. » | test: Tour9RollFinishTest#theRollsKeyAndBarNamesFitTheirPlaceAtTwoHundredPercentOnATablet
chemin: Barre de pistes, son de la bibliothèque puis un bouton tourné | test: Tour9RollFinishTest#aSoundWithAKnobTurnedKeepsItsNameOnTheTrack
chemin: Synthé, Clavier, lettres 200 % au-dessus du panneau du tuto, et son « ? » | test: Tour9RollFinishTest#theKeysFitAboveTheTutorialAndTheirHelpSaysWhatTheyDo
chemin: Rouleau, copier une mesure puis « Coller » (indonésien, t9-27) | test: Tour9RollFinishTest#thePasteGoesOnABarInSight
chemin: Synthé, après un enregistrement, fiche de prise refermée | test: Tour9RollFinishTest#theKeysGetTheirHeightBackOnceTheTakesCardIsGone

corrige: S17 | gravité: grave | rejeu: t9-s17-glisse-pointille
controle: reporté (mode boost, grande vérification de fin)
corrige: F112 | gravité: mineur
corrige: F113 | gravité: mineur
corrige: F114 | gravité: mineur
corrige: F115 | gravité: mineur
corrige: F116 | gravité: mineur
corrige: F117 | gravité: mineur
corrige: F118 | gravité: mineur
corrige: F142 | gravité: mineur
ne corrige pas: D06 : rejoué sur le banc (fr, 360 dp, 200 %, Simple, speed garage chapitre 3, piste Basse), aucune rangée de la grille n'est en vue : l'en-tête fixe de la page et le panneau replié du tuto (titre et chapitre sur cinq lignes) ne laissent qu'environ 55 px de page ; leur hauteur se règle dans TutoPanel.kt (lowRoom n'a pas de cas pour le rouleau) et AppRoot.kt, hors des fichiers du lot ; le lot n'en pose que la part du rouleau (rouge e5e8f6ccd, vert c68d882b4) ; rejeu en attente docs/audits/ultra/rejeux-attente/t9-d06-pointilles-200

## Phrases d'acceptation
Celles du tri (docs/audits/ultra/tour-9-tri/defauts.tsv) restent les phrases de référence. Le lot n'en tient qu'une part :
- S17, phrase du tri : « Une note longue sur une cible large valide ; « Eu faço » ne reboucle pas sur les notes déjà posées. » Le lot tient la première moitié. La seconde (la boucle « Eu faço ») est dans la validation du tuto (TutoChecks.kt), hors de ma liste : voir « Laissés de côté ».
- D06, phrase du tri : « En Simple à 200 %, les notes pointillées restent visibles et le compteur « encore N notes » baisse à chaque note touchée. » Pas tenue sur le chemin du banc à 360 dp (voir la ligne « ne corrige pas »).

Phrases du lot, pour le siège de contrôle :
- D06 (part du rouleau) : « En Simple comme en Pro, à 130 % comme à 200 %, chaque note en pointillé que je touche se pose, le compteur « encore N notes » baisse d'autant, et il reste toujours une note en pointillé en vue (ou une touche sur la ligne en montre une) jusqu'à la fin de l'étape. »
- S17 : « Un glissé qui part d'une note en pointillé, ou juste à côté (à la portée d'un toucher), la pose entière, comme un toucher ; le rouleau ne défile pas. »

## Ce qui a changé
Seconde passe (finitions, une preuve par défaut dans `Tour9RollFinishTest`, chacune vue rouge avant son correctif) ; les choix sont dans `docs/decisions/tour-9-L7.md`.
- F112 : la part d'un glissé vers le haut que le rouleau ne prend pas ne fait plus défiler la page ; un glissé vers le bas ramène l'en-tête.
- F113 : en mode « Accords », la note mise en évidence est le bas de l'accord, celle que la consigne nomme.
- F114 : « Basse fantôme » n'est plus cochée à l'ouverture.
- F115 : les noms des touches et « mes. » rétrécissent pour tenir dans leur place (téléphone et tablette, 200 %).
- F116 : l'onglet garde le nom du son choisi (le plus proche de la bibliothèque, jusqu'à 3 réglages changés).
- F117 : à 200 %, la page du synthé défile de ce qui manque au clavier ; le « ? » du Clavier dit jouer, gamme et accords.
- F118 : « Coller » vise une mesure en vue, avec un libellé traduit d'un seul tenant.
- F142 : l'espace réservé à la fiche de prise revient à zéro quand elle se ferme.

Première passe :
- D06 : à grosses lettres, le haut de la page et le panneau du tuto ne laissaient que 0 à 10 px de grille en vue. Rien ne faisait monter la page en tenant compte du panneau, donc aucune note en pointillé n'était touchable et la ligne « encore N notes » attendait des rangées qui ne venaient jamais. Le rouleau demande maintenant à la page de le montrer au-dessus du panneau quand moins de 4 rangées sont en vue.
- S17 : un glissé qui part d'une note en pointillé pas encore posée appartient à la grille : la note (ou l'accord en mode « Accords ») est posée quand le doigt se lève, et le rouleau ne défile pas (rouge 0d741b65a, vert d04041940).
- S17, rejoué sur le banc : un glissé qui partait 2 px avant la note dessinée faisait encore défiler le rouleau. Le glissé a maintenant la même portée qu'un toucher (tutoTapTarget : un pas de côté, une rangée de part et d'autre) (rouge a1dd7c414, vert 75bc675c2). Le rejeu t9-s17-glisse-pointille (pt-BR, Pro, 130 %, detroit chapitre 5 : un glissé de x 145 à x 200 sur la rangée de la note) ne posait rien avant 75bc675c2 ; il la pose depuis (« Falta tocar 1 nota pontilhada » au lieu de 2).

## Laissés de côté
- Raccords hors de ma liste, signalés au chef : `guide/TrackSheet.kt` (vers la ligne 772) montre encore `GuideSheets.soundName` (même cause que F116) ; l'aide RECORD_KEYS de `RecordBar` ne parle que d'enregistrer (même cause que F117).
- F115 sur `synth/ScaleKeys.kt` (clavier en gamme) : aucun siège ne l'a vu coupé ; pas touché.
- D06 sur un téléphone de 360 dp à 200 % : voir la ligne « ne corrige pas ». Lot de suite proposé, dans TutoPanel.kt : un drapeau « le rouleau est en vue » (comme celui de la grille de rythme) pour que lowRoom replie le panneau plus bas au-dessus du rouleau, et un haut de panneau plus court à 200 %. Vu en passant : la ligne « Encore 1 note… » s'affiche alors qu'aucune rangée n'est en vue et que l'étape en demande 4 (compte fait dans TutoRollHint, module model, hors de ma liste). Le rejeu en attente t9-d06-pointilles-200 attend qu'un dessin de la grille soit en vue après « Créer la piste Basse » ; il est « non tenu » sur cette branche.
- À 200 % en Pro (pt-BR, 360 dp, detroit chapitre 5), quand moins de 4 rangées tiennent, le rouleau aligne la ligne « Falta tocar… » en haut de la fente, pas la grille ; aucune rangée n'y est en vue non plus avant le lot (la fente montre alors la barre d'outils). Même cause de place que D06.
- Siège 17, geste 157 : « Eu faço » revient en boucle sur des notes déjà posées. Cause probable dans la validation du tuto (TutoChecks / TutoController, lot L3), hors de ma liste.
- `TutoJourneyTest` (jump_up) échoue parfois sous une grosse charge. Il passe seul, avec et sans la correction.

## Recette pour le téléphone de Benjamin
Tuto speed garage, étape de la basse, lettres à 130 % : glisser le doigt en partant d'une note en pointillé, ou juste à côté. La note se pose entière, le compteur baisse et le rouleau ne défile pas. À 200 % sur un téléphone, la grille reste cachée sous le panneau du tuto : connu, pas encore corrigé (D06).
