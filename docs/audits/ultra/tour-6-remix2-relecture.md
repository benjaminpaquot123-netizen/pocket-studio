# Tour 6 — lot « remix2 » (zone Remix, siège B a, pouce gauche) : relecture neuve
Relu : 7046598a99b72567bd3a3e6a11498bf5aa046af4

Relecteur neuf (n'a pas écrit ce code). Lu : `docs/AIDE-FUSIONS-TESTS.md` (fiche du relecteur), le diff `ce0aed6f..HEAD` de `CutScreen.kt`, `CutWave.kt`, `RemixScreen.kt`, `RemixLeftThumbSeatTest.kt`, puis le delta `41d39ef5..7046598a` (`CutScreen.kt`, `CutBoutBigTextTest.kt`), et les captures `cut_bout.png`, `cut_bout_130.png`, `cut_screen.png`, `remix/cut_bout_short_{fr_100,fr_130,ptbr_200}.png`, regardées en entier.
Rejoué : `:app:testDebugUnitTest --tests 'com.pocketstudio.app.remix.*'` à 41d39ef5 (300 tests, 0 échec) puis à HEAD (301 tests, 0 échec) ; `verifyRoborazziDebug --tests '*SizeScreenshotTest' --tests 'com.pocketstudio.app.remix.Cut*'` et `--tests '…TabletSideBySideTest'` à HEAD : verts. Les rouges ont été rejoués dans un worktree jetable (code de production remis à l'état du parent, tests de HEAD), retiré ensuite.

## Ce qui tient

- **A8, aucun accès perdu.** L'ancien `remix_open_cut` vivait dans `InProject` (rendu si `ui.remix.setId != null && !ui.home`) sous `if (ui.board != null)`. Le bouton de la rangée du pouce (`RemixScreen.kt` l.292) s'affiche sous `ui.board != null && !ui.home && ui.remix.setId != null` : c'est exactement la même condition. `a8_oneCutButtonInProject` compte un seul « Cortar la canción ».
- **A3.** L'intro n'est masquée que si `ui.sets` n'est pas vide : un débutant neuf, sans chanson séparée, la voit toujours. Aucun test de capture ni de recensement ne rougit.
- **A5 après le delta.** En « Un bout », la colonne défile toujours derrière une courbe fixe `(maxHeight - 380.dp).coerceIn(120.dp, 180.dp)`, et le plancher de `weight` est revenu à 120 dp. Le seuil fragile de 485 dp et le plancher de 96 dp de ma première lecture (constat moyen) ont disparu ; le test de garde à 488 dp / 100 % le prouve. Sur `cut_bout_short_fr_100` / `_fr_130` : courbe et Début / Fin / Écouter entiers, l'envoi passe sous le pli et se rejoint en défilant. Sur `_ptbr_200` : puces en haut (`bigText`), courbe lisible.
- **A2.** À police normale, les puces sont sous les outils, à portée du pouce gauche, et visibles sans défiler (`cut_bout.png`, `cut_bout_short_fr_100.png`). L'ordre est inversé, mais rien n'est caché : on ne perd aucun geste. La rangée défile horizontalement (`CutScreen.kt` l.341) : la 4e puce reste atteignable quand elle déborde.
- **A7.** Sur `cut_screen.png`, les bouts cochés 3, 4 et 6 se distinguent nettement : fond plus vif (alpha 0,42 au lieu de 0,16) et cadre plein de leur couleur. La toile de la courbe est sombre ici, et les six couleurs y ressortent.
- **Son et sauvegarde non touchés.** Le diff ne contient que de l'interface Compose (mise en page, dessin `Canvas`) et des tests : pas de C++, pas de callback audio, pas de magasin ni de sauvegarde.
- **Les tests prouvent quelque chose** : les quatre tests du siège et le test de garde rougissent sans leur correctif (lignes ci-dessous).

rouge constaté : 203fcac9 : rejoué sur le code de 41d39ef5 : `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` « the curve is 0.0.dp high at 100.0 % on 488.0.dp: a finger needs 120 dp »
rouge constaté : e33521f2 : rejoué sur le code de 4eeee030 : `RemixLeftThumbSeatTest#a8_oneCutButtonInProject` « two « Découper la chanson » buttons expected:<1> but was:<2> »
rouge constaté : b6d6ede5 : rejoué sur le code de 4eeee030 : `RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb` « cut_stem_3 is out of the left thumb » et `#a5_spanToolsWholeOnArrival` « cut_start cut off »
rouge constaté : f678d1f0 : rejoué sur le code de 4eeee030 : `RemixLeftThumbSeatTest#a3_cutHintWholeNotUnderThumb` « cut_song_hint_… cut off (under the thumb bar) »

(Les trois rouges du siège ont été joués en une passe sur le code de production de 4eeee030, identique à celui de ce0aed6f puisque ce commit ne touche que le test : 4 échecs sur 4. Pour b6d6ede5 et f678d1f0, ce n'est donc pas leur parent exact, qui contient déjà A8 ; mais les assertions A2, A3 et A5 ne dépendent pas d'A8.)

## Bloquant

Aucun.

## Grave

Aucun.

## Moyen

1. **Seuil 485 dp et plancher de courbe à 96 dp, sans test entre 485 et environ 510 dp.** Vu à 41d39ef5. À police normale, la courbe pouvait tomber sous les 120 dp qu'il faut à un doigt, et aucun test ne le voyait. **corrigé** en 203fcac9 : la courbe est fixée entre 120 et 180 dp, la colonne défile toujours, le plancher de `weight` est revenu à 120 dp, et `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` (488 dp, 100 %) est vert à HEAD et rouge sans le correctif.
2. **Téléphones hauts et tablettes en portrait : la courbe de « Un bout » est plafonnée à 180 dp.** Effet de 203fcac9 (`scrollsAll = !wide && model.single`, toutes hauteurs). Avant le lot, au-delà de 700 dp, la courbe prenait toute la place libre (`weight`). Sur `cut_bout.png` (téléphone de 914 dp), elle passe à 180 dp et laisse un vide d'environ un quart d'écran sous la carte d'envoi. Rien n'est bloqué (180 dp ≥ 120, tous les outils sont visibles), mais les barres sont plus petites à saisir là où la place ne manque pas. Aucune capture de tablette ne couvre « Un bout » : les `*_cut_structure` sont en Phrases. À décider : `heightIn(min = curveHeight)` avec `weight(1f)` quand la colonne tient, ou noter le choix dans `docs/decisions/tour-6-remix2.md`.

## Mineur

3. **Commentaires périmés après 203fcac9.** `CutScreen.kt` l.217 (« in « Un bout » when the screen scrolls (a large font) ») : la colonne défile maintenant toujours, et c'est `bigText` qui décide. l.228 (« the curve takes the room ») : la courbe est fixe en « Un bout ». l.242 (« in « Un bout » that fits without scrolling »). Rien ne casse, mais le prochain lecteur sera induit en erreur.
4. **À 200 %, la 4e puce (« Resto ») est coupée net au bord droit, sans fondu** (`cut_bout_short_ptbr_200.png`). Elle reste atteignable en glissant la rangée. Le défaut existait déjà avant le lot (même constat à la relecture de remix-decoupe, point 6).
5. **A2 n'est prouvé qu'à 100 %.** À 130 %, `bigText` est faux (seuil `> 1.3f`) : les puces restent en bas, sous le pli (`cut_bout_short_fr_130.png`), et il faut un défilement pour les atteindre. Aucun test de portée pour le pouce gauche au-dessus de 100 %.
6. **A7 sur une toile claire** : le jaune (`0xFFFDD835`) et le vert clair encadrés auraient peu de contraste si la toile de la courbe était claire. Seules des captures en sombre ont été regardées ; le thème clair n'a pas été vérifié. Le commit du lot l'appelle lui-même « tenté ».

Verdict : 0 bloquant, 0 grave
