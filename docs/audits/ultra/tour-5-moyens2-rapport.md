# Tour 5, lot « moyens2 » : trois défauts moyens de l'appli (2026-11-12)

Atelier `eq-tour-5-moyens2`, branche `eq/tour-5-moyens2` (depuis fx-int 57b16e8b). Rien de poussé. À relire par un relecteur neuf.
Décisions : `docs/decisions/tour-5-moyens2.md`. Arrêté plus court sur demande de l'orchestrateur (file Gradle donnée au tour 6) :
tests ciblés seulement, pas de suite complète ni de balayage des captures.

tests: *TopBarNameRoomTest *.ui.TopBarTest *OneLoudnessExportTest *OneTargetExportTest *WholeWordsCostTest *PadLiveNarrowWordsTest *UiScaleTest *NavScale130ScreenshotTest

## 1. Barre du haut, dossier seul (Mode simple, 130 %) : « Sauvegardé » et « REC dans … » absents

Cause : quand la rangée ne garde que le dossier (`nameShown = false`, lot « moyens »), ni la ligne du nom ni la colonne du nom
ne sont dessinées ; la coche et « REC dans … » vivaient là. Correctif : une pastille sur le coin du dossier (coche verte, ou point
rouge pendant une prise dans une autre partie), ses mots entiers dans sa description (TalkBack, banc). Aucune largeur, aucune ligne.
Rouge 39c35a7f (« top_saved is not displayed »), vert d40fa808.

## 2. Export : deux LUFS pour le même fichier (−9,4 et −9,2)

Les deux sources : la carte (`ExportBilan.levelLine`, `ExportUi.loudness`) et « Mesures pro » (`ProReport.lines`, `ExportUi.proReport`).
Elles mesurent la même chose (loudness intégrée du rendu, rapport du moteur). Un M4A qui dépasse −1 dBTP une fois décodé est rendu à
nouveau, limiteur plus bas (`M4aGuard`) ; `loudness` suivait le nouveau rendu, `proReport` gardait le premier. Correctif : `proReport`
suit le rendu gardé (une seule valeur, celle du fichier ; le rapport texte le faisait déjà). Rouge de77bd22 (« −9,2 » contre « −9,4 »), vert 22144cd6.

## 3. Gel de 789 ms à l'ouverture, siège A d (allemand, police 200 %)

Rejoué sous Robolectric avec les réglages du siège (`HumanTester.begin`, de / phone / 200 / simple, sonde non commitée) : premier
écran « dialogue:welcome », 18 libellés à taille auto, 168 mises en page de texte, 280 ms dans `WholeWords.getFontSize` (789 ms sur le
banc chargé). Cause : chaque libellé trop large essayait chaque demi-point depuis sa taille (jusqu'à 15 essais à 200 %, 17 pour
`FitText`). Correctif : taille pleine d'abord, puis dichotomie sur la même grille (`largestFitting`, ui-components) : 5 mises en page
au plus, même taille trouvée (vérifié contre l'ancienne boucle sur 10 libellés allemands). Seuil : le chien de garde du banc, 700 ms.
Non prouvé ici : le temps du gel lui-même après correctif (Robolectric ne le rejoue pas de façon stable) ; la preuve est le nombre de
mises en page. Rouge 938c3c6c (17 et 15 mises en page), vert 5872e2ed.

## Captures

Seules `NavScale130ScreenshotTest` et `UiScaleTest` rejouées (vérification). Six captures changées, réenregistrées
(`scale130/fr_nav_simple_{rhythm,keys,song,send,more}.png`, `fr_nav_help_labels_simple.png`) : Mode simple à 130 %, dossier seul ;
regardées en entier : `fr_nav_simple_rhythm` (comparaison avant/après : seule la coche verte sur le coin du dossier, pads et
onglets au pixel près) et `fr_nav_simple_send` (nouvelle) ; les quatre autres non regardées une par une (même écran, même barre).
Les 17 autres captures jouées de la classe (dont Pro) inchangées, 7 sautées par la classe elle-même. Recette, cut_screen, cut_screen_magnifier, remix_stem_edit_menu, rhythm_scrolled_under_part :
pas touchées.

## Non fait

- Pas de suite complète, pas de `recordRoborazziDebug` des autres captures (demande de l'orchestrateur).
- Pas d'essai téléphone du gel (allemand, 200 %).

## Liste des chemins

- chemin: barre du haut, Mode simple à 130 %, dossier seul › « Sauvegardé » | test: TopBarNameRoomTest#theFolderAloneStillSaysSavedAndWhereTheTakeGoes
- chemin: barre du haut, nom jamais « M… » | test: TopBarNameRoomTest#theModeSimpleBarKeepsAFewWordsOfTheName
- chemin: Mix › Exporter M4A › carte et « Mesures pro » | test: OneLoudnessExportTest#anM4aRenderedAgainSaysOneLoudness
- chemin: Mix › Exporter (Club, Streaming) › bilan | test: OneTargetExportTest#clubAndStreamingExportsDifferAndSayIt
- chemin: libellés WholeWordsText à 200 % (ouverture, siège A d) | test: WholeWordsCostTest#aWholeWordsLabelAt200FindsItsSizeInAFewLayouts
- chemin: libellés FitText à 200 % | test: WholeWordsCostTest#aFitTextLabelAt200FindsItsSizeInAFewLayouts
