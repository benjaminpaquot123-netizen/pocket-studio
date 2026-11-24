# Tour 9 : lot « fable6 » (rapport)

Base : `ccr-80af1a1b-mn0wec` (a1a447c90). Branche : `eq/tour-9-fable6`. Tout est « tenté » (pas encore constaté par un testeur neuf).

Les trois « défauts qui reviennent » de la zone tuto (registre, lignes 9 à 12, contrôle du tour 7) :

- **montee-vide** (sièges tuto2, fable ; d-V2) : ne se reproduit plus. Réglé par 494a125e6 (« an empty part named Montée is not Déjà fait », `TutoPlay.undone`) puis 584c3df8 (une partie vide n'est plus dite absente, constaté au tour 8). Preuve neuve, tous les styles et les deux niveaux : `Tour9Fable6EmptyPartNeverDoneTest` (project-model), verte sur la base.
- **cible-autre-partie** (sièges tuto2, fable ; a-V4) : ne se reproduit plus. Réglé par a3aafd9fc (« a step done in one part is not dotted in another », `TutoChecks.heldElsewhere`). Preuve neuve, tous les styles et les deux niveaux, pads, notes, 303 et croix : `Tour9Fable6NothingOutOfItsPartTest` (project-model), verte sur la base.
- **a-V1** (sièges p8, p8b) : **reproduit, corrigé**. Rouge e918e397f, vert a8e520d95.
  - Phrase d'acceptation : « téléphone, 100 %, house leçon 3 : à chaque note demandée, la note à poser est en vue au-dessus du panneau et le rond "Touche ici" est dessus ; je la pose d'un toucher ».
  - Mesure du rouge (téléphone 360 × 780, 100 %) : après UN toucher sur la ligne « Encore N notes en pointillé à toucher », la moitié de la mesure d'arrivée restait hors des rangées, avec une flèche ↑ ou ↓ sur la ligne : house `pad_end` (Facile et Pro, 10 notes sur 20), house `lead_do` (Pro, 4 sur 8), techno `lead_do`, pop `lead_do`, trap `bass_do` et `lead_do` (Pro). Il fallait un second toucher.
  - Cause racine : au toucher, les rangées (repli, cadrage, note cachée) étaient jugées sur la vue d'AVANT le déplacement vers la mesure suivante ; et, pour les accords, sur la seule note du bas de chaque accord. Les travaux des tours 7 et 8 (c74c0058, b455a1d0, b685bad7) réglaient la première mesure et l'anneau, pas la mesure où le toucher emmène.
  - Correction : `TutoRollHint.atBar` (la vue une fois amenée à la mesure) ; `brought`, `folded` et `lit` jugent les rangées sur cette vue et sur toutes les notes en pointillé de la mesure ; `PianoRoll` fait défiler jusqu'à la mesure avant de replier les rangées.
  - Le test joue chaque ligne de notes du parcours (toutes les étapes `notes:`), aux deux niveaux, sur house, techno, pop et trap : 43 cas, 36 joués jusqu'à la dernière note, 7 sautés (ligne déjà entière à l'arrivée, étape cochée). `PS_FABLE6_STYLES=all` joue tous les styles (plus de 20 minutes, pas lancé en entier).

## Comment l'alerte s'éteint

`scripts/bench/alerte-repetition.py` : l'alerte « défaut qui revient » d'un chantier tombe quand une ligne `ok` est notée pour ce chantier ; « constaté » dans `corrections.tsv` ne suffit pas à lui seul. Rien n'a été touché dans `pre-vol.sh`, le registre ni `corrections.tsv`. À faire par le chef, une fois qu'un testeur neuf (débutant, téléphone, house, chapitre 3, puis une Montée vidée et une partie neuve) aura constaté :

    python3 scripts/bench/alerte-repetition.py note tuto fable6 ok "" <chemin du verdict du contrôle>

et, dans `docs/audits/ultra/corrections.tsv`, une ligne (colonnes de la ligne 349) :

    tour-9	a-V1	a	-	constaté	a8e520d95	Tour9Fable6TouchHereEveryBarTest#everyDottedNoteIsOneTouchAway	un toucher sur la ligne amène toute la mesure en vue	TutoChecks.kt, PianoRoll.kt	téléphone, 100 %, house leçon 3 : à chaque note demandée, la note à poser est en vue au-dessus du panneau et le rond "Touche ici" est dessus ; je la pose d'un toucher	<siège>	<source>

tests: *Tour9Fable6TouchHereEveryBarTest *Tour9Fable6EmptyPartNeverDoneTest *Tour9Fable6NothingOutOfItsPartTest *Tour8TutoTouchHere* *PianoRollBigTextDotted* *Tour6TutoFlechesRollTest *Tour6TutoLineGoTest *T9PanelTargetTest *Tour9RollFinishTest

## Chemins

chemin: Tuto > ligne de notes (basse, accords, mélodie) > toucher la ligne « Encore N notes », chaque mesure | test: Tour9Fable6TouchHereEveryBar#everyDottedNoteIsOneTouchAway
chemin: Tuto ch. 3 house, première mesure, « Touche ici » | test: Tour8TutoTouchHere#simpleOneTouchShowsTheBar
chemin: Tuto > Drop, puis une autre partie montrée | test: Tour7TutoDottedInItsPartTest#theCrashIsNotDottedInAnotherPart
chemin: Tuto > partie « Montée » vide | test: Tour8TutoGesturesEmptyPartTest#anEmptyPartIsNotSaidMissing

## Ce qui change (tenté)

- Au tuto, un toucher sur la ligne des notes restantes amène d'un coup toute la mesure suivante en vue au-dessus du panneau, accords entiers compris : plus de flèche qui reste, plus de second toucher.
- Rien d'autre ne change à l'écran (les deux autres défauts étaient déjà réglés ; seuls des tests s'ajoutent).

## Ce qui n'est pas vert

- Aucun testeur neuf n'a constaté : tout reste « tenté », l'alerte du registre reste allumée jusqu'à la ligne `ok`.
- Le test du toucher n'a été joué en entier que sur 4 styles sur 15 ; la passe « tous les styles » a été coupée après 20 minutes (une centaine de cas passés, aucun rouge lu, résultat non gardé).
- Pas de CI complète ni de captures refaites (mode boost : tests ciblés seulement).
- Pas de rejeu au banc ajouté (lot sans siège ; `scripts/bench/` hors de la liste des fichiers).
