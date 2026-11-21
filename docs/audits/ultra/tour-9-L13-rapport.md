# Tour 9, lot L13 « panneau tuto : repli, boutons, pourquoi, carte » : rapport

Branche : eq/tour-9-L13, partie de l'intégration ccr-80af1a1b-mn0wec.
Défauts visés : F001 à F011 (finitions, `docs/audits/ultra/tour-9-tri/finitions.tsv`). Tentés 10 sur 11, aucun « corrigé » (un testeur neuf le dira).
Le registre `corrections.tsv` n'a aucune ligne pour ces finitions : le lot n'en ajoute pas, l'état « tenté » est écrit ici.

tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.*'
chemin: Panneau tuto replié, étape suivante | test: Tour9L13TutoFoldKeptTest#foldedByTheUserStaysFoldedWhenTheStepChanges
chemin: Panneau tuto replié, consigne d'action | test: Tour9L13TutoFoldedActionTest#foldedTheStepsActionIsStillThere
chemin: Fin de démo repliée, la démo se dit encore en cours | test: Tour9L13TutoFoldedActionTest#foldedTheStepsActionIsStillThere
chemin: Panneau tuto déplié, flèche | test: Tour9L13TutoNeverFullScreenTest#unfoldedThePanelLeavesTheWorkInSight
chemin: Panneau tuto, bouton « suivant » sous le doigt | test: Tour9L13TutoOneWayOnTest#theNextButtonStaysUnderTheFingerWhenTheExampleEnds
chemin: Panneau tuto, un seul « suivant » | test: Tour9L13TutoOneWayOnTest#neverTwoWaysOnAndEachButtonSaysWhatItDoes
chemin: Carte de fin, « Revenir à l'étape » | test: Tour9L13TutoEndCardTest#backToTheStepIsWholeAboveTheBottomBar
chemin: Carte de fin, « il reste N étapes » | test: Tour9L13TutoEndCardTest#theStepsLeftAreAllThereEachOneTouchAway
chemin: Étape d'écoute de partie (garde) | test: Tour9L13TutoMineAfterPartTest#everyPartMadeHasMyResultBesideTheExample
chemin: Panneau tuto, « Pourquoi ? » debout | test: Tour9L13TutoWhyTest#theReasonOpensUnderItsButtonAndStaysUntilClosed
chemin: Panneau tuto, « Pourquoi ? » couché | test: Tour9L13TutoWhyTest#onItsSideTooTheReasonOpensUnderItsButtonAndStays
chemin: Panneau tuto, flèche du repli | test: Tour9L13TutoNoDemoBySurpriseTest#theTouchMeantForTheFoldNeverStartsADemo
chemin: Carte des parties | test: Tour9L13TutoSongMapTest#everyPartWholeWithItsOwnColourEvenAt200
chemin: Panneau tuto, « Lire la suite » à 200 % | test: Tour9L13TutoReadAllTest#readAllShowsTheWholeInstruction
chemin: Panneau tuto couché, « Lire la suite », boutons de l'étape | test: Tour9L13TutoReadAllTest#onItsSideReadAllKeepsTheStepsButtons
chemin: Bandeau du kit, carte de la grille (L14, garde) | test: Tour9L14TutoPanelTest#kitLeavesTheMapWhole
chemin: Panneau tuto, boutons voisins (L14, garde) | test: Tour9L14TutoPanelTest#neighboursShareTextSize

corrige: F001 | gravité: mineur
corrige: F002 | gravité: mineur
corrige: F003 | gravité: mineur
corrige: F004 | gravité: mineur
corrige: F005 | gravité: mineur
corrige: F006 | gravité: mineur
corrige: F007 | gravité: mineur
corrige: F009 | gravité: mineur
corrige: F010 | gravité: mineur
corrige: F011 | gravité: mineur
ne corrige pas: F008 : non reproduit à neuf, garde gardée

## Ce qui change (tous « tentés »)
- F001 : replié par l'utilisateur, le panneau reste replié quand l'étape change. Rouge 9ba34ee2, vert 10205c62.
- F010 : la touche de la flèche ne lance plus de démo. Vert 7e793f8b (rouge dans le même test, prouvé sur la machine du lot).
- F002 : replié, la consigne d'action (ou son bouton) reste lisible. Rouge 81431812, vert 083dac41.
- F003 : déplié, le panneau s'arrête à 60 % de l'écran debout (85 % quand « Pourquoi ? » est ouvert, plus la hauteur du kit sur un pas de carte) ; jamais plein écran. Rouges d451b21d et 0362a78b, vert 219e3cdc.
- F004/F005 : un seul bouton « suivant » plein, qui reste sous le doigt ; chaque bouton dit ce qu'il fait. Rouge 7a6c43d4, vert 4faad36a.
- F006/F007 : « Revenir à l'étape » entier au-dessus de la barre du bas ; « il reste N étapes » ouvre la liste complète, chaque étape touchable. Rouge 5e736f5c, vert 23851607.
- F011 : chaque bloc de la carte montre nom et durée, chaque partie sa couleur, même à 200 %. Rouge 60aa2613, vert 5f2b8411.
- F009 : la raison s'ouvre juste sous « Pourquoi ? », ne chasse pas la consigne, reste ouverte au « Bravo » ; à 200 % elle défile dans sa propre place. Rouge 60aa2613, vert fd80b1b7.
- Garde L14 après fusion de l'intégration (4dd35036) : le plafond F003 coupait la carte quand le bandeau du kit arrivait, et le bouton plein partageait la rangée à parts égales. Vert 630ca6f7.
- Rejeu t9-d14 (« Lire la suite ») : le plafond F003 coupait la consigne entière. « Lire la suite » lève le plafond et rend aux mots toute leur hauteur. Rouge 894bb6df, vert 4c8d92ef. Relecture neuve : couché, les boutons de l'étape tombaient à 0 px ; les mots restent pondérés, la barre d'actions mesurée d'abord. Rouge fe325abe, vert c9a0bdb1.
- Garde du parcours (TutoJourneyTest, 19 cas) : replié en plein fondu de démo, la ligne F002 perdait la marque « démo en cours », le parcours dépliait trop tôt et la démo restait figée. Rouge 781beb26, vert 4db8f4e5 ; parcours complet et panneaux voisins (Tour6, L14, L15, TutoScreenTest) verts.
- F008 : non reproduit à neuf ; test de garde (Tour9L13TutoMineAfterPartTest) dans 60aa2613.

## Choix notés
- Plafond du panneau debout : 60 % ; 85 % quand la raison est ouverte ; sur un pas de carte, 60 % + hauteur du kit + 6 dp, sans dépasser 85 %. Aucun plafond une fois « Lire la suite » touché (la consigne entière est demandée).
- À 200 %, la raison défile dans sa place ; si la place manque encore, elle passe sous les mots (repli `whyShed`).
- Boutons côte à côte seulement si chacun tient dans sa part égale de la rangée (espace de tête compris), sinon empilés.

## Ce qui n'est pas vert
- Contrôle du tuto (débutant neuf) : reporté en mode boost (`PS_JUGE_BOOST=1`), à faire à la grande vérification.
- CI complète non lancée (mode boost : la grande vérification la fera).

## Notes de la relecture (mineurs, non traités)
- Le test « Pourquoi ? » ne vérifie pas la fermeture au second toucher.
- Replié, « Chapitre suivant » permet de passer la musique du « Bravo ».
- « Revenir à l'étape » masqué quand plus aucune étape ne reste : non testé.
- Le repli choisi n'est pas gardé après la mort du processus.

## Demandes (hors de mes fichiers)
Aucune.

## Captures regardées (écran entier)
- Captures du panneau prises par les tests ciblés (debout, couché, 100 % et 200 %, FR et HI) : rien ne déborde, la consigne reste en tête, la raison sous son bouton.
