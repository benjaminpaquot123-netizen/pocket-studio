# Tour 9, lot L1 « panneau-tuto » : rapport

Branche : eq/tour-9-L1, partie de l'intégration dca571da7.
Défauts visés : D02 (bloquant), D13 (grave, partie visibilité), D14 (moyen, non traité).

Phrase d'acceptation (mot pour mot) : « Sur un téléphone de 360 dp, police 200 %, dans toute langue, à chaque étape du tuto qui demande de toucher quelque chose, la cible est visible au-dessus du panneau du tuto (au moins une rangée entière de la grille / le bouton entier) et je peux la toucher sans rien replier. »

Cause : à grande police, le panneau replié montait à environ 260 dp : le titre et la ligne du chapitre faisaient 3 lignes chacun, plus 2 lignes de consigne. Il restait alors environ 23 dp à la grille. Sur tablette, le panneau ouvert pendant qu'on touchait l'onglet Mix à la main cachait les ▶.

Ce qui change :
- Le panneau a un plafond : la place sous la barre du haut, moins 200 dp × l'échelle de police (entre 1 et 2). Replié et plus haut que ce plafond, il prend une forme compacte sur une ligne : la barre de progression, la ligne repliée et le bouton pour déplier. À 100 %, rien ne change.
- Pendant une étape « Je le fais » ouverte, un onglet touché à la main replie le panneau. Les étapes manuelles le gardent ouvert.
- D14 (« Lire la suite » doit déplier toute la consigne) : non traité dans ce lot.

Décisions : docs/decisions/tour-9-L1.md.

Captures réenregistrées : app/screenshots/tour6/shell_audio_pages_end_fr_100.png et shell_audio_pages_end_tr_130.png. Le panneau y est identique ; seuls le scintillement de la puce « Voix » et l'onde de toucher sur « Sons » ont bougé. Écrans entiers regardés.

corrige: D02 | gravité: bloquant | rejeu impossible : preuve sans banc par T9PanelTargetTest, dossier rejeux hors des fichiers du lot
corrige: D13 | gravité: grave | rejeu impossible : preuve sans banc par T9PanelTargetTest, dossier rejeux hors des fichiers du lot

tests: *T9PanelTargetTest *TutoKickSoundTest *Tour6GuideCoquilleLookTest

chemin: tuto synthwave Pro ch.12 fill, téléphone 360 dp, police 200 %, ja, grille du rythme | test: T9PanelTargetTest#fillCellsAboveThePanelIn360At200Japanese
chemin: tuto afro house Pro ch.8 variation, 392 dp, police 200 %, zh, grille des pads | test: T9PanelTargetTest#padGridAboveThePanelAt200Chinese
chemin: tuto liquid DnB Pro ch.8 lead_do, 392 dp, police 200 %, nl, piano roll | test: T9PanelTargetTest#melodyRollAboveThePanelAt200Dutch
chemin: tuto neurofunk Facile ch.14, tablette, de, onglet Mix touché à la main, boutons ▶ | test: T9PanelTargetTest#soloButtonsAboveThePanelAfterTheMixTab

État : tenté, prêt pour contrôle (pas « corrigé » : un siège neuf doit le constater).
