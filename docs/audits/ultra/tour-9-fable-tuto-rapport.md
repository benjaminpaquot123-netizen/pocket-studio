# Tour 9 : lot « fable-tuto » (rapport)

Base : `ccr-80af1a1b-mn0wec`. Branche : `eq/tour-9-fable-tuto`. Zone : tuto (une seule zone). Tout est « tenté » tant que le chef n'a pas rangé le lot.

Le chantier « tuto » sonnait : 5 échecs de suite (tuto, tuto2, fable, p8, p8b). Chaque défaut revenu a été rejoué sur la tête, jamais jugé à l'impression.

## Par défaut

- D3 (la suite de la consigne se lit) : réparé avant ce lot. Preuve : rejeux `t9-ft-d3-suite-fr` et `t9-ft-d3-suite-ja`, siège fable-tuto3/a écran 25.
- D2 (bouton, ko), c-V4 (texte, ko), c-V3 (clavier, ko) : réparés avant ce lot. Preuve : rejeux `t9-ft-d2-bouton-ko`, `t9-ft-cv4-texte-ko`, `t9-ft-cv3-roll-ko`.
- montee-vide (une partie « Montée » vide n'est pas « Déjà fait ») : réparé avant ce lot. Preuve : `Tour7TutoEmptyPartNotDoneTest`, ancien siège fable-tuto/d écran 56 ; pas atteignable par le siège neuf (aucun geste ne vide la partie).
- cible-autre-partie (la cible est dans la partie que la consigne nomme) : corrigé dans ce lot. Rouge 53c6dfd41, vert 764f2caaa. Preuve : siège fable-tuto3/d écrans 2 et 9, rejeu `t9-ft-cymbale-drop`.
- d-fin (la variation se valide au nombre de coups que la consigne demande) : corrigé dans ce lot. Rouge 5ef1c2264, vert 2c43db29a. Preuve : siège fable-tuto3/d écrans 13 à 16.
- a-V1 (dès « Je le fais », avant toute touche sur « Touche ici », les notes en pointillé de la mesure sont en vue) : corrigé dans ce lot, en deux fois. Basse et accords : rouge 9a1505fed, vert 77c8b89e1 (pliage dès l'ouverture). Mélodie (encore là au siège fable-tuto2/a, écrans 46 et 57 : les notes tenaient mais le défilement hérité en laissait deux sous le panneau) : rouge 7b6a35ee4, vert 439e288c8. Preuve : siège fable-tuto3/a, écrans 5, 13 (basse), 25, 31, 36 (accords), 44, 54 (mélodie).
- D4 : non traité (phrase d'acceptation à reformuler, voir la décision).
- b-grille : non corrigé (contredit D14, voir la décision).

## Cause commune

1. « Corrigé » était dit sur des tests unitaires, jamais sur un rejeu enregistré du geste du testeur.
2. Les phrases d'acceptation étaient floues ou se contredisaient d'un tour à l'autre (D4 ; b-grille contre D14).
3. Chaque correction valait pour un chemin (un niveau, une leçon, un geste) et pas pour les autres (Pro, « Je le fais », mélodie, notes qui tiennent mais mal cadrées) : aucune règle « ce que le texte demande = ce que le moteur vérifie, sur tous les chemins ».

## Défauts neufs vus par les sièges (mineurs, non corrigés ici)

- Accords, mesure 1 : l'anneau est sur la note du haut alors que la consigne dit celle du bas, et ne passe pas à l'accord suivant (fable-tuto3/a, écrans 25 et 26).
- Mélodie, étape 2 : « Encore 1 note » avant « Je le fais », puis « Encore 8 notes » (fable-tuto3/a, écrans 53 et 54).
- Rythme, chapitre 12 : une croix rouge sous le panneau, il faut défiler (fable-tuto3/d, écrans 64 et 65). C'est b-grille.
- « Chapitres » manqué une fois, le panneau a bougé sous le doigt (fable-tuto3/d, écrans 53 et 54).
- Rouvrir un chapitre fini affiche « Tuto fini » (fable-tuto2/d, écrans 43 et 46).

tests: *T9FableLevelPartsTest *Tour6TutoVariationTargetTest *Tour7TutoHiddenNoteTest *Tour8TutoTouchHere* *TutoSightHintTest *Tour7TutoEmptyPartNotDoneTest *Tour9TutoGoToThePartTest

## Chemins

chemin: Tuto house, basse, « Je le fais » (Simple) | test: Tour8TutoTouchHere#simpleTheBarIsInSightBeforeAnyTouch
chemin: Tuto house, basse, « Je le fais » (Pro) | test: Tour8TutoTouchHere#proTheBarIsInSightBeforeAnyTouch
chemin: Tuto house, mélodie, « Je le fais » (Simple) | test: Tour8TutoTouchHere#simpleTheMelodyBarIsInSightBeforeAnyTouch
chemin: Tuto house, mélodie, « Je le fais » (Pro) | test: Tour8TutoTouchHere#proTheMelodyBarIsInSightBeforeAnyTouch
chemin: Tuto house, basse, ligne « Touche ici » | test: Tour8TutoTouchHere#simpleOneTouchShowsTheBar
chemin: Tuto, basse, flèche de la ligne | test: TutoSightHintTest#houseBassTheArrowIsOnlyForNotesOutOfSight
chemin: Tuto, partie « Montée » vide | test: Tour7TutoEmptyPartNotDoneTest#anEmptyPartNamedMonteeIsNotDejaFait
chemin: Tuto, « Aller à la partie » | test: Tour9TutoGoToThePartTest#goingToThePartOpensItAndTheStepGoesOn

## Corrections

corrige: a-V1 | gravité: grave | rejeu impossible : le défaut dépend du défilement hérité du chemin joué, prouvé par le siège fable-tuto3/a
corrige: cible-autre-partie | gravité: grave | rejeu: t9-ft-cymbale-drop
corrige: d-fin | gravité: moyen
ne corrige pas: D4 : phrase d'acceptation à reformuler au tri
ne corrige pas: b-grille : contredit D14, phrase proposée au chef

## Rejeux

Rejoués sur 10757a42d (`bash scripts/bench/rejeu-defauts.sh`) :

t9-ft-cymbale-drop : oracle tenu
t9-ft-d3-suite-fr : oracle tenu
t9-ft-d3-suite-ja : oracle tenu
t9-ft-d2-bouton-ko : oracle tenu
t9-ft-cv4-texte-ko : oracle tenu
t9-ft-cv3-roll-ko : oracle tenu

## Contrôle du tuto (débutants neufs, après le dernier changement du tuto)

controle: fable-tuto3/a | code testé: 257267dba5ec6396b700731001861e3779756397 | bloqués: 0 | graves neufs: 0
controle: fable-tuto3/d | code testé: 257267dba5ec6396b700731001861e3779756397 | bloqués: 0 | graves neufs: 0

Self-test du banc sur ce code : 331 verts, 0 rouge, 0 en quarantaine.
