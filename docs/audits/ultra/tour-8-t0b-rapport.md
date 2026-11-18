# Tour 8, lot T0b « tuto-gestes » (zone tuto) : rapport

Branche : eq/tour-8-t0b, partie de l'intégration ccr-80af1a1b-mn0wec (tête ff64ff0a).
Défauts visés : e ch.13, e ch.12, d (Montée mal placée), d-V2 (partie vide dite absente), c (croix au ch.12 en ko), a-V1 (anneau « Touche ici » sur les pads), compteur ja, morceau d'exemple.

Phrases d'acceptation (mot pour mot) :
- ch.13 : « en de, deux mains, tablet8 à 130 %, Mode Pro : au tuto techno chapitre 13, je touche « Ich mach's » et l'envoi passe à la valeur demandée, la courbe est tracée, l'étape se coche. »
- ch.12 : « …au chapitre 12, l'éditeur s'ouvre sur l'onglet demandé par l'étape, et un toucher pose un seul roll. »
- Montée : « en fr, phone à 100 %, Mode Simple : au tuto techno chapitre 12, je crée la Montée et elle se place juste avant le Drop 2. »
- a-V1 : « au tuto ch.3, l'anneau « Touche ici » entoure la case à toucher ».
- compteur ja : « le compteur annonce le nombre de points visibles à poser ».
- d-V2 (texte du défaut) : « Il n'y a pas de partie « Montée » » s'affiche alors que la partie existe, vide.

Ce qui change :
- « Je le fais » d'une étape d'effet fait le geste lui-même : l'envoi ou l'insert de la tranche montrée prend la valeur demandée, la courbe se trace, l'étape se coche (`TutoPlay.doHere`, appelé par `TutoController.doIt`).
- Quand une étape demande des cases ou des croix sur le pad montré, l'outil de la grille revient à « Coups » : un toucher retire le coup au lieu de faire un roulement ×2.
- La Montée créée se pose juste avant le Drop 2 (et non plus après le Break).
- Une partie qui existe mais qui est vide n'est plus dite « absente » : son bouton la remplit.
- Croix rouges et cases en pointillé aussi sur la vue « tous les pads ».
- L'anneau « Touche ici » entoure la première case en pointillé vide de la mesure montrée.
- Le compteur du rouleau compte les notes en pointillé visibles.
- Morceau d'exemple : jugé voulu, rien changé. `scripts/live-bench.sh` : 0 alerte ; creux brefs jusqu'à −16,2 dB puis montée régulière jusqu'à −9,5 dB (c'est une montée).
- Aucun texte touché, aucun fichier i18n (le mot « notes » est pour T0c). `ui/PlayStrip.kt` l. 321 : rien changé (position de lecture, hors zone tuto).

Tests ajustés (accord du chef) :
- TutoLot4Test : le second toucher de l'étape réverbe règle maintenant l'envoi de la caisse ; le test vérifie l'envoi > 0 et les deux « ✓ » au lieu de « à régler ici ».
- TutoJourneyTest : après « Je le fais », si rien n'est à rejouer, le test exige que l'étape soit faite (sinon : « ni fait par "Je le fais" ni faisable ») ; sinon, il joue le geste comme avant. Aucune branche ne passe sans vérifier.

Captures réenregistrées (les miennes seulement) : tuto_app_bass.png (compteur 4 → 3), tuto_app_snare.png (anneau). Captures qui diffèrent et ne sont pas de ce lot (non touchées, signalées au chef) : projects.png, remix.png, share_sheet_ok.png.

Décisions : docs/decisions/tour-8-t0b.md.

Preuves :
- rouge puis vert pour chaque défaut : Tour8TutoGesturesDoItTest, Tour8TutoGesturesToolTest, Tour8TutoGesturesMonteeTest, Tour8TutoGesturesEmptyPartTest, Tour8TutoGesturesAllPadsTest, Tour8TutoGesturesRingTest, Tour8TutoGesturesCounterTest.
- Rejeu : aucun `cmd-*.txt` de p8b dans le dépôt ; le contrôle se fait par les sièges neufs d et e (start.txt de `…/controle/tuto2/d/` et `…/tuto2/e/`).

tests: *Tour8TutoGestures* *TutoLot4Test* *TutoJourneyTest*

chemin: tuto techno ch.13, Pro, « Je le fais » d'un envoi et d'une courbe | test: Tour8TutoGesturesDoItTest#theReverbsDoItSetsTheSendOfTheStripItGoesTo
chemin: tuto techno ch.12, grille des pads après un roulement, croix à retirer | test: Tour8TutoGesturesToolTest#aStepAskingForCrossesOpensTheGridOnHits
chemin: tuto techno ch.12, Simple, « Créer la partie » Montée | test: Tour8TutoGesturesMonteeTest#theMonteeMadeLandsJustBeforeTheDropTwo
chemin: tuto techno, partie Montée vidée, panneau du tuto | test: Tour8TutoGesturesEmptyPartTest#anEmptyPartIsNotSaidMissing
chemin: tuto ch.12, vue « tous les pads », croix et pointillés | test: Tour8TutoGesturesAllPadsTest#theCrossesAndTheDottedCellsAreDrawnOnAllThePads
chemin: tuto ch.3, grille des pads, anneau « Touche ici » | test: Tour8TutoGesturesRingTest#theRingIsOnTheFirstEmptyDottedCellOfTheBar
chemin: rouleau du tuto, compteur de notes en pointillé | test: Tour8TutoGesturesCounterTest#theCountIsTheNotesInSightWhenOneMoreIsOutOfTheRows

État : tenté, prêt pour contrôle (pas « corrigé » : un siège neuf doit le constater).
