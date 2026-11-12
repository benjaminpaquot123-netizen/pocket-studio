# Tour 5, lot « restes » (branche eq/tour-5-restes, base fx-int dba28dab)

Reprise après coupure : le fichier non commité (`QuickStyleCardTest.kt`, test « jamais coupé à 130 % ») était cohérent, gardé.

| Point | Rouge | Vert | État |
|---|---|---|---|
| 1. Plus de dB en simple (effets Pad Live, fiche de piste du Guide, écoute de référence) | b20768cf | cad6177b | fait ; les trois écrans dans `SimpleNoDecibelTest`, tous verts |
| 2. Bouton de carte de style « Régler avant de créer » | efd48948 | f4f65b16 | vérifié ; 14 langues |
| 2b. Jamais coupé (360 dp, 130 %, 14 langues) | rouge sur « Créer le morceau » coupé dans 12 langues | deux essais (voir ci-dessous) | fait |
| 3. Captures du bouton et de « Muet » (téléphone, 130 %) | n/a | commit « test: images » | 4 images, regardées |
| 4. « Projet sauvegardé » faux si la copie échoue | `ZipSavedLinesTest` échoue sur son assertion | `zipSavedLines` | fait, 14 langues |

Trouvaille : mon premier correctif (FlowRow) passait le test mais cachait le second bouton (hauteur intrinsèque de la carte). Seule la capture l'a montré. Remplacé par deux lignes (Column), test inchangé vert. Leçon : un test de « non coupé » ne prouve pas que l'élément est visible ; la capture reste obligatoire.

Non touchés : AppRoot.kt, barres et boutons ▶, tests `human`, `scripts/bench`. Rien poussé.
Suite complète (`:project-model:test :app:testDebugUnitTest :app:verifyRoborazziDebug`) : voir la fin du rapport final.

Suite complète : 8189 tests, 13 échecs. 1 à moi (ScreenScanTest census mixMuet, corrigé : lignes acceptées dans recensement-acceptes.tsv, ScreenScanTest vert). 12 sont des écarts de capture hors de mon périmètre (recette, cut_screen, cut_screen_magnifier, rhythm_scrolled_under_part déjà connus ; welcome en/hi/tablette, nav_plus_settings, nav_welcome_mode : images non touchées par ce lot).
