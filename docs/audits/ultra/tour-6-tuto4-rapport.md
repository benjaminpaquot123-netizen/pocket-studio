# Tour 6, lot « tuto4 » : les phrases du tuto (2026-11-13, état final du 14)

Consigne du chef : réparer les phrases d'acceptation M1 à M7 (sièges de contrôle Ha-2/3/4 en français, Da-2/3/4 en japonais, Bb-2/3/4 en allemand), dans toutes les langues ; puis, au fil des allers-retours des sièges de contrôle, les défauts qu'ils ont rendus (R, P, P1 à P4, T4, H3, H5, H7).
Branche : `eq/tour-6-tuto4`, partie de `eq/tour-6-tuto3` (dcc06432c), avec la tête de `ccr-80af1a1b-mn0wec` fusionnée (dernière : ae8727624, sans conflit). Décisions : `docs/decisions/tour-6-tuto4.md`.
Fichiers : `project-model/src/main/resources/tutos/*.json`, `i18n/*/tuto*.json` (14 langues), `TutoChecks.kt`, `DefaultNames.kt`, `MixTips.kt`, `GuideTracks.kt`, et côté appli `TutoPanel.kt`, `RhythmScreen.kt`, `SynthScreen.kt`, `AppRoot.kt`, `TrackSheet.kt`, `WhatPlays.kt`, `PlainWords.kt` (texte seul), et les tests.

tests: *Tour6TutoPadNamesTest *Tour6TutoPanelPadQuotesTest *Tour6TutoPanelPartMadeTest *Tour6Tuto4HouseTest *Tour6Tuto4TechnoTest *Tour6Tuto4HouseRound3Test *Tour6Tuto4TechnoRound3Test *Tour6Tuto4AcidTechnoTest *Tour6TutoGapPartTest *Tour6TutoLongPressMenuWordsTest *Tour6TutoLongPressCellTest *Tour6TrackSheetPadNameTest *Tour6PlainWordsTest *Tour6WhatPlaysPadNameTest *Tour6LongPressEmptyCellTest *Tour6MixTipsPadNameTest *Tour6SimpleMixPadNameTest *Tour6TutoCellsPanel100Test *Tour6TutoNamePartLabelTest *Tour6TutoSoundButtonInSight* *Tour6TutoBannerChainTest

## Contrôle final (sur la branche poussée, après le dernier changement du tuto)
controle: Fd-1 | code testé: 0e250438b | bloqués: 0 | graves neufs: 0
controle: Da-9 | code testé: 0e250438b | bloqués: 0 | graves neufs: 0
controle: Bb-9 | code testé: 0e250438b | bloqués: 0 | graves neufs: 0

- Fd-1 (fr, téléphone, police 100, Mode simple, tuto Facile House, siège de contrôle du portail) : « Tuto fini : bravo ! » en 245 gestes (budget 300), 3 hésitations, 0 chapitre bloqué, aucun défaut neuf. T4 corrigé (écran 19), « Pad 5 » corrigé (écran 244), H5/H7 corrigé pour les boutons (écran 196), H3 « autre » (voir plus bas). Dossier `sieges/tour-6/controle/Fd-1/`.
- Da-9 (ja, téléphone, Mode simple, tuto Facile Techno, chapitres 1 à 12 enchaînés) : 0 impasse, 3 hésitations, aucun défaut ; P3 corrigé (écran 140 : le bandeau dit 「シンバル」, plus « → スタジオのバスドラム ») ; P4 (H3 en japonais) corrigé (écran 33). Dossier `sieges/tour-6/controle/Da-9/`.
- Bb-9 (de, tablette 8 pouces, police 130, Mode pro, tuto Pro Techno, chapitres 1 à 7) : 0 hésitation, aucun bloquant ni grave ; H3 corrigé (écran 42), H5 corrigé (écran 100), H7 corrigé (écran 142). Dossier `sieges/tour-6/controle/Bb-9/`.
- Aucun commit de code du tuto après 0e250438b : la fusion de l'intégration (ae8727624) ne change aucun fichier du tuto que le lot change (elle touche `PianoRoll.kt`, que le lot ne change pas).

## Contrôles d'avant (historique, joués sur un code plus ancien de la branche)
- (historique, joué sur 63e18fe7) Fa-2 (fr, Facile House jusqu'au ch. 14) : R corrigé (écrans 190, 203), M2 corrigé (écran 122), M4 corrigé (écrans 131, 133, 162-166) ; « Pad 5 » dans « Ce qui joue » (écran 199) vu « autre », corrigé depuis (Fd-1, écran 244).
- (historique, joué sur 63e18fe7) Fp-1 (fr, Pro) : M6 corrigé (écran 16 : pad « Clap » › « Effets du pad » › « Envoi réverbe »).
- (historique, joué sur 16098b62) Da-8 (ja, Facile, 15 chapitres, Bravo écran 174) : P1 corrigé (écran 163), P2 corrigé (écran 118), P3 encore là (écran 155), corrigé depuis (Da-9, écran 140).
- (historique, joué sur 16098b62) Bb-7 (de, Pro, 16 chapitres, Bravo écran 253) : M1 (écran 216), M2 (écran 162), M4 (écran 114), M5 (écran 245) corrigés ; H3 et H7 encore là, H5 autre : repris dans le lot, corrigés depuis (Bb-9).
- (historique, joué sur 63e18fe7) Bb-8 (de, Pro, ch. 1 à 7) : H3 encore là, H5 et H7 autre (4 gestes) : corrigés depuis (Bb-9).

## Les phrases et les défauts, point par point (état final)
| point | ce que dit le testeur | réparé où | constaté (siège, écran) |
|---|---|---|---|
| M1 pad nommé comme écrit | house « Rim » (« Bord de caisse » en Simple), techno « Rumble », « Clap », « Charley ouvert », « Rim », « Cymbale » ; pluriel du Mode simple (ja ko zh-CN it) | `tutos/*.json`, `i18n/*/tuto*.json`, `ultra2z6.json` | Bb-7, écran 216 |
| M2 mot expliqué à sa première vue | kick, Groove, Tonique, Break, relances, Mix, solo, Drop, Takt, ベロシティ, ロール, 3連符, stab ; aussi les titres, Facile et Pro | idem, `PlainWords.kt` (texte) | Fa-2, écran 122 ; Bb-7, écran 162 |
| M3 chiffres justes | « 8 cases (4 par mesure) », mesures de chaque partie (8, la Montée 4), seuil du swing « de 4 à 10 % » | idem, `TutoChecks.kt` | tenté : aucun siège copié ici ne le lit ; gardé par `Tour6Tuto4HouseTest#theNumbersAreTheStepsOwn` et `Tour6Tuto4TechnoTest#numbersTellTheTruth` |
| M4 pas de contradiction | Break : kick et basse se taisent ; Montée sans silence promis ; « Créer la partie « X » » | idem, `PlainWords.kt` | Fa-2, écrans 131-166 ; Bb-7, écran 114 |
| M5 combien, et ce que disent les chiffres | 3 croix, +4 … −∞, −14, « Pas de saturation », « aucun solo oublié », Club −7,5 | idem | Bb-7, écran 245 |
| M6 envoi réverbe du Clap | grille › pad « Clap » › « Effets du pad » › « Envoi réverbe » | idem | Fp-1, écran 16 |
| M7 un mot par chose | « Partage ton morceau », « Partager » (シェア) partout | idem, `finish.json` | tenté : aucun siège copié ici ne le lit ; gardé par `Tour6Tuto4TechnoTest#oneWordPerThing` et `#theShareChapterSaysShareLikeTheHouse` |
| R pads nommés comme le pad dans le Mix | « Cymbale », « Bord de caisse », « Maracas » | `MixTips.kt`, `DefaultNames.kt`, `GuideTracks.kt`, `AppRoot.kt` | Fa-2, écrans 190, 203 |
| P « Pad 5 » dans « Ce qui joue » | « Claquement de mains » | `WhatPlays.kt`, `TrackSheet.kt` | Fd-1, écran 244 |
| P1 alerte du Mix (ja) | le nom japonais du pad | `MixTips.kt` | Da-8, écran 163 |
| P2 bouton « Nommer la partie » (ja) | en japonais | `TutoPanel.kt` | Da-8, écran 118 |
| P3 bandeau de la grille (ja, ch. 12) | le pad montré, plus « → スタジオのバスドラム » | `TutoPanel.kt`, `TutoChecks.kt` | Da-9, écran 140 |
| P4 appui long (ja, ch. 3) | un appui long pose le coup et ouvre son menu | `RhythmScreen.kt`, `i18n/*/tuto*.json` | Da-9, écran 33 |
| T4 cases en pointillé sous le panneau (100 %) | les deux rangées entières au-dessus du panneau | `TutoPanel.kt`, `RhythmScreen.kt` | Fd-1, écran 19 |
| H3 appui long = un geste | coup posé et menu « Force / Doux » ouvert d'un seul appui | `RhythmScreen.kt`, phrases des tutos | Bb-9, écran 42 ; Da-9, écran 33 ; Fd-1, écran 31 (case vide ordinaire) |
| H5 bouton du son de la basse en vue | « Régler le son de la piste ↓ » à l'écran sans geste | `SynthScreen.kt`, `TutoPanel.kt`, `AppRoot.kt` | Bb-9, écran 100 ; Fd-1, écran 196 |
| H7 idem pour le lead | idem | idem | Bb-9, écran 142 |

## Corrections et rejeux
corrige: T4 | gravité: grave | rejeu: t6-ha19-cases-panneau
t6-ha19-cases-panneau : oracle tenu
- Le rejeu (siège Ha, écran 19, fr, téléphone, police 100) : « non tenu » avant la correction (sortie gardée, « [coupé, état « vide, en pointillé : à toucher »] » encore là), « oracle tenu » après, joué par `rejeu-defauts.sh` le 14 à 02:51 sur l'arbre de la branche, dont la tête était alors 0e250438b (pas rejoué sur la tête fusionnée : la fusion ne touche pas `TutoPanel.kt` ni `RhythmScreen.kt`). Fd-1 l'a vu corrigé (écran 19).
corrige: H3 | gravité: moyen
corrige: H5 | gravité: moyen
corrige: H7 | gravité: moyen
corrige: P4 | gravité: moyen
corrige: P3 | gravité: moyen
corrige: P1 | gravité: moyen
corrige: P2 | gravité: moyen
corrige: R | gravité: moyen
corrige: P | gravité: mineur
corrige: M1 | gravité: moyen
corrige: M2 | gravité: moyen
corrige: M3 | gravité: moyen
corrige: M4 | gravité: moyen
corrige: M5 | gravité: moyen
corrige: M6 | gravité: moyen
corrige: M7 | gravité: moyen
ne corrige pas: N19 : grave du lot 2 (TutoController.kt, avertissement de move), hors des phrases et des défauts de ce lot
- M3 et M7 sont « tentés » au sens du registre : leurs tests les gardent, aucun siège copié ici ne les a relus ; ils deviennent « constatés » au prochain contrôle qui les lit.

## Pas pris (état final)
Rien, sauf ce qui a été dit « pas un défaut » :
- Le tuto Pro saute les étapes d'écoute : voulu (le Pro sait écouter seul) ; la phrase du « Groove » est donnée dans le Pro à « groove_name » (`Tour6Tuto4TechnoTest#theProExplainsTheGrooveWhereItFirstReadsIt`).
- M5, les étiquettes de l'écran elles-mêmes (`SimpleMix.kt`, `Loudness.kt`, `ExportCheck.kt`) : le tuto les cite telles qu'elles s'affichent, elles ne sont pas fausses.
- M6, « Effets du pad » caché en Mode simple : sans objet, l'étape est Pro seulement et le tuto Pro met le Mode pro (relecture).

## Laissé pour le test sur téléphone (aucun changement)
- Fd-1, H3 « autre » : sur la DERNIÈRE case en pointillé d'une étape, l'appui long pose le coup, l'étape est finie et « Bravo, chapitre fini ! » prend la place du menu (écran 30). Le coup est bien posé, rien n'est perdu ; sur une case ordinaire le menu s'ouvre (écran 31). À regarder sur le téléphone de Benjamin.
- Fd-1, H5/H7 : les boutons viennent seuls en vue ; les notes en pointillé du Clavier, elles, demandent encore « Touche ici » (écrans 100-109 et 145-155), un geste qui les amène.
- Da-9, ch. 7 en japonais : 「ここをタップ」 amène d'abord la note du 小節 2 avant de finir le 小節 1, puis revient (écrans 89-96) : la règle de la note la plus proche, on s'y retrouve.

## Vu par les sièges, hors des points de ce lot (pour le tri suivant)
- Da-8 et Da-9 (ja) : « 低いうなり, 手拍子, ベース » avec des virgules latines dans « Partie « X » créée » (`TutoController.kt:485`, `joinToString(", ")`), mineur.
- Da-8 (ja) : 「小節」 dans la définition de « Temps » (`i18n/ja/tuto.json`) avant d'être expliqué au ch. 5 ; ゴーストベース, リズム補正, « Plaits » (libellés de l'écran du synthé) sans explication pendant le tuto ; le champ 曲名 recouvert par la rangée M4A/WAV (écran 174).
- Da-8 et Da-9 (ja, ch. 10 et 12) : 「チャプター 12 で作るよ」 puis 「最後の拍は鳴りません」 et 「もうできています！」 : léger doute, pas bloquant.
- Fd-1 (fr, ch. 6) : « Accords en pointillé à toucher : 9 » alors que 4 accords se voient (la mesure 4 est hors écran).

## Liste des chemins
Les phrases sont prouvées par `Tour6Tuto4HouseTest`, `Tour6Tuto4TechnoTest` et leurs « Round3 » (project-model, une paire rouge/vert par méthode). Les écrans de l'appli :
chemin: Tuto › chaque pad cité entre guillemets est un pad affiché | test: Tour6TutoPadNamesTest#aPadNamedIsAPadShown
chemin: Tuto en japonais › le pad cité prend le nom du kit | test: Tour6TutoPanelPadQuotesTest#japanese
chemin: Tuto en coréen › le pad cité prend le nom du kit | test: Tour6TutoPanelPadQuotesTest#korean
chemin: Tuto › Créer la partie › ce que dit le bouton suit l'étape | test: Tour6TutoPanelPartMadeTest#aPartThatLeavesBricksOutIsNotSaidTheWholeGroove
chemin: Tuto › Créer la partie › ce qui manque se lit sur la partie créée | test: Tour6TutoPanelPartMadeTest#whatAPartLeavesOutIsReadFromThePartMadeNotFromTheOneShown
chemin: Tuto Facile › kick › police 100, panneau ouvert › cases en pointillé entières | test: Tour6TutoCellsPanel100Test#theKickStepsDottedCellsAreWholeAboveThePanelAt100
chemin: Grille de batterie › appui long sur une case vide › coup posé et menu | test: Tour6LongPressEmptyCellTest#aLongPressOnAnEmptyCellPutsTheHitAndOpensItsMenu
chemin: Mon morceau › « Ce qui joue » › nom du pad | test: Tour6WhatPlaysPadNameTest#whatPlaysNamesAPadAsThePadShowsIt
chemin: Fiche de piste › nom du pad | test: Tour6TrackSheetPadNameTest#theTrackSheetNamesAPadAsThePadShowsIt
chemin: Mix › conseil › nom du pad | test: Tour6MixTipsPadNameTest#theMixNamesAPadAsThePadShowsIt
chemin: Mix en Mode simple › nom de chaque pad | test: Tour6SimpleMixPadNameTest#theSimpleMixNamesEachPadAsThePadShowsIt
chemin: Tuto › Nommer la partie › libellé du bouton | test: Tour6TutoNamePartLabelTest#theNameButtonSaysThePartAsShown
chemin: Tuto › ch. 12 › bandeau de la grille › pad montré | test: Tour6TutoBannerChainTest#theLineNamesThePadShownWhenTheCourseGoesOn

## Preuves
- Chaque méthode vue rouge sur son assertion avant la correction, puis verte (paires « rouge: » / « vert: » ; M1, M2 et M3 techno ont un deuxième « vert: » du même sujet, expliqué dans les décisions).
- Sur la tête fusionnée (ae8727624 dans la branche) : voir « Relancé » ci-dessous.

## Relancé sur la tête fusionnée
Sur 446309175 (la branche avec la tête ae8727624 fusionnée, sans conflit) :
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :project-model:test` : BUILD SUCCESSFUL, « up-to-date » (mêmes entrées que le passage précédent) : 1173 tests, 0 échec, 1 ignoré.
- `… :app:testDebugUnitTest --tests '*Tuto*' --tests '*TrackSheet*' --tests '*MixTips*' --tests '*WhatPlays*' --tests '*Rhythm*' --tests '*Synth*'` : BUILD SUCCESSFUL en 14 min 49 s, 114 classes, 478 tests, 0 échec, 16 ignorés.
- `… :app:testDebugUnitTest --tests '*Tour6LongPressEmptyCellTest' --tests '*Tour6SimpleMixPadNameTest' --tests '*Tour6PlainWordsTest'` (hors des filtres d'avant) : BUILD SUCCESSFUL, 5 tests, 0 échec.
- `python3 scripts/bench/rapport-zone.py` (simple, `--corrections`, `--tuto`) : sortie 0 ; `scripts/bench/pre-vol.sh --avant-relecture tour-6 tuto4` depuis un arbre jetable sur ae8727624 : « PRÊT (0 à voir) ».
