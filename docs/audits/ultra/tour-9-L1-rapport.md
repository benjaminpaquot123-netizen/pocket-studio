# Tour 9, lot L1 « panneau-tuto » : rapport

Branche : eq/tour-9-L1, partie de l'intégration dca571da7.
Défauts visés : D02 (bloquant), D13 (grave, partie visibilité), D14 (moyen).

Phrase d'acceptation (mot pour mot) : « Sur un téléphone de 360 dp, police 200 %, dans toute langue, à chaque étape du tuto qui demande de toucher quelque chose, la cible est visible au-dessus du panneau du tuto (au moins une rangée entière de la grille / le bouton entier) et je peux la toucher sans rien replier. »

Causes :
- D02 : à grande police, le panneau replié montait à environ 260 dp : le titre et la ligne du chapitre faisaient 3 lignes chacun, plus 2 lignes de consigne. Il restait alors environ 23 dp à la grille. Sur tablette, le panneau ouvert pendant qu'on touchait l'onglet Mix à la main cachait les ▶.
- D13 : « Je le fais » amenait la carte de la sortie principale, plus haute que la place au-dessus du panneau : elle venait par le haut, et les puces « Volume visé » de son pied restaient sous le panneau (néerlandais, pliable, 200 % : 38 des 56 px de la puce Auto cachés).
- D14 : « Lire la suite » tournait la consigne par pages dans un panneau plafonné : à 200 %, la fin de la consigne restait coupée.

Ce qui change :
- Le panneau a un plafond : la place sous la barre du haut, moins 200 dp × l'échelle de police (entre 1 et 2). Replié et plus haut que ce plafond, il prend une forme compacte sur une ligne : la barre de progression, la ligne repliée et le bouton pour déplier. À 100 %, rien ne change.
- Pendant une étape « Je le fais » ouverte, un onglet touché à la main replie le panneau. Les étapes manuelles le gardent ouvert.
- D13 : une étape de volume (« loudness: ») amène d'abord les puces « Volume visé » (repère `mixer_target`), puis la carte si elle est repliée.
- D14 : « Lire la suite » met le panneau ouvert en lecture : il peut monter jusqu'à la barre d'état, laisse tomber l'en-tête, le kit et le « Pourquoi » épinglé tant que la consigne ne tient pas, puis tourne les pages seulement si elle déborde encore. « Replier » rend le panneau d'avant.
- Hors liste, sur accord du chef : `mixer/MasterLoudness.kt` (fichier de L8), une ligne (`Modifier.spot("mixer_target")` sur la rangée des puces) et son import.

Décisions : docs/decisions/tour-9-L1.md.

Captures : aucune changée par le lot. app/screenshots/tour6/shell_audio_pages_end_fr_100.png et shell_audio_pages_end_tr_130.png, réenregistrées un temps, sont rendues à la base : leur écart (scintillement de la puce « Voix », onde de toucher sur « Sons ») change d'un passage à l'autre et rougit aussi sur la base (tour 8). Écrans entiers regardés.

corrige: D02 | gravité: bloquant | rejeu: t9-d02-cases-grille
corrige: D13 | gravité: grave | rejeu: t9-d13-volume-cible-nl
corrige: D14 | gravité: moyen | rejeu: t9-d14-lire-la-suite

Rejeux ajoutés (joués par scripts/bench/rejeu-defauts.sh avec la correction) :
t9-d02-cases-grille : oracle tenu
t9-d13-volume-cible-id : oracle tenu
t9-d13-volume-cible-nl : oracle tenu
t9-d14-lire-la-suite : oracle tenu

tests: *T9PanelTargetTest *T9ReadMoreTest *TutoKickSoundTest *Tour6GuideCoquilleLookTest

chemin: tuto synthwave Pro ch.12 fill, téléphone 360 dp, police 200 %, ja, grille du rythme | test: T9PanelTargetTest#fillCellsAboveThePanelIn360At200Japanese
chemin: tuto afro house Pro ch.8 variation, 392 dp, police 200 %, zh, grille des pads | test: T9PanelTargetTest#padGridAboveThePanelAt200Chinese
chemin: tuto liquid DnB Pro ch.8 lead_do, 392 dp, police 200 %, nl, piano roll | test: T9PanelTargetTest#melodyRollAboveThePanelAt200Dutch
chemin: tuto neurofunk Facile ch.14, tablette, de, onglet Mix touché à la main, boutons ▶ | test: T9PanelTargetTest#soloButtonsAboveThePanelAfterTheMixTab
chemin: tuto gabber Pro ch.13 mastering, 392 dp, police 130 %, id, puce « Klub » du volume visé | test: T9PanelTargetTest#clubTargetAboveThePanelOnAPhoneAt130Indonesian
chemin: tuto house Pro ch.16, pliable, police 130 %, ja, puce « Club » du volume visé | test: T9PanelTargetTest#clubTargetAboveThePanelOnAFoldableAt130Japanese
chemin: tuto boom bap Pro ch.13 mastering, pliable, police 200 %, nl, puce « Auto » du volume visé | test: T9PanelTargetTest#autoTargetAboveThePanelOnAFoldableAt200Dutch
chemin: tuto acidcore Facile ch.1, 360 dp, police 200 %, ko, « Lire la suite » avec le kit proposé | test: T9ReadMoreTest#koreanWelcomeAt200On360
chemin: tuto liquid DnB Pro ch.7, 392 dp, police 200 %, nl, « Lire la suite » | test: T9ReadMoreTest#dutchChapterSevenAt200
chemin: tuto techno Pro ch.1, 392 dp, police 200 %, zh, « Lire la suite » | test: T9ReadMoreTest#chineseTechnoAt200

État : tenté, prêt pour contrôle (pas « corrigé » : un siège neuf doit le constater).

## D06 (lot suivant)

Défaut visé : D06 (bloquant). Phrase d'acceptation (mot pour mot) : « En Simple à 200 %, les notes pointillées restent visibles et le compteur « encore N notes » baisse à chaque note touchée. »

Causes :
- À 200 % en Simple, dans l'onglet Clavier, la ligne de la partie (nom, son, flèches) et la rangée « encore N notes » ne laissaient au rouleau de notes que quelques dizaines de dp au-dessus du panneau du tuto : les notes pointillées étaient hors de vue (sièges t9-29, t9-62).
- Dans la rangée du compteur, « Touche ici » à côté du texte le coupait en « ↑ Encor… » : le nombre de notes restant ne se lisait pas.

Ce qui change :
- Tant que la partie montrée a des notes pointillées à toucher, rouleau affiché, avec une police au-delà de 130 %, la ligne de la partie se retire et laisse sa place au rouleau. Ailleurs, rien ne change.
- À une police au-delà de 130 %, « Touche ici » monte sur la ligne du haut, à côté du nom de la partie, et le compteur prend toute la largeur : on lit « ↑ Encore 3 notes e… ».
- Hors liste, sur accord du chef : `synth/PianoRoll.kt`, la rangée du compteur seulement.

Rejeu : `t9-d06-pointilles-200` passe du français (speed garage), tenu même sans la correction, à l'hindi (grime ch. 3), non tenu sans la correction : le rouleau était hors de vue.

Captures : app/screenshots/tour9/d06_dotted_notes_simple_fr_200.png (nouvelle), écran entier regardé : rouleau et notes pointillées au-dessus du panneau, « Touche ici » en haut, compteur lisible.

corrige: D06 | gravité: bloquant | rejeu: t9-d06-pointilles-200
controle: reporté (mode boost, grande vérification de fin)

Rejeu ajouté (joué par scripts/bench/rejeu-defauts.sh avec la correction) :
t9-d06-pointilles-200 : oracle tenu

tests: *T9PanelTargetTest *T9ReadMoreTest *TutoKickSoundTest *Tour6GuideCoquilleLookTest

chemin: tuto speed garage ch.3, 360 dp, police 200 %, fr, Simple, onglet Clavier, notes pointillées et « encore N notes » | test: T9PanelTargetTest#dottedNotesAboveThePanelInSimpleAt200French

Laissés de côté : F101 (rendu au chef, donné à L12), F111 (pas commencé, rendu au chef).

État : tenté, prêt pour contrôle (pas « corrigé » : un siège neuf doit le constater).
