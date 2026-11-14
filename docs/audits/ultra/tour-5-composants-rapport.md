# Lot « composants » : rapport de l'auteur

Date : 2026-11-10 · branche `eq/tour-5-composants` (base 4437ccc, tête de départ 594be3d) · un seul auteur, aucun sous-agent · rien n'est poussé.
Étape 2 du plan d'action (`PLAN-ACTION-AUDITS.md`, ligne « Trois composants partagés »), chantier 2 de l'audit de prise en main (`AUDIT-PRISE-EN-MAIN.md` § 4) : G3 (ligne-lien), G4 (repli), G7 (pastille de piste), G5 pour l'icône seulement, et les règles 12 à 22.

## En trois lignes

1. Ce qui ouvre quelque chose a maintenant l'air d'un lien : une ligne grise avec son icône, son titre, sa sous-ligne et une flèche ›. Plus de phrase orange seule dans « Prêt à partager ? », Mix, Mes projets et « Mes pistes ».
2. Les replis disent ce qu'ils cachent : « Avancé » affiche « Piste par piste · FLAC · MIDI · 48 kHz · 24 bits », ce qui a été changé en orange. Les pastilles de pistes ne sont plus rognées en grande police, et Partager n'a plus qu'une icône, celle d'Android.
3. Restent hors de ce lot : la carte Exporter encore verte (M4) et le gabarit des cartes d'accueil (M2), qui sont dans la même ligne de l'audit mais pas dans ma consigne ; et le « Pourquoi ? » du tuto.

## 1. La ligne-lien (`LinkRow`, G3, règle 12)

- **Quoi.** Nouveau composant `ui-components/.../LinkRow.kt` : fond gris (SurfaceHigh 55 %), coins 12 dp, 56 dp de haut au moins, icône (ou une marque à soi) à gauche, titre en couleur du texte (l'orange reste à l'action principale), sous-ligne grise, chevron › dessiné à droite (‹ en arabe). Les mots passent à la ligne, jamais « … » ; le titre ne se coupe jamais au milieu d'un mot.
- **Où.** « Mes fichiers » (feuille Partager, page Partager du Mode simple, carte Exporter de Mix), « Autres fichiers », « Sauvegarder le morceau en .zip », « Exporter les notes en fichier MIDI », « Importer un projet (.zip) » (Mes projets), et chaque piste de « Mes pistes » en Mode simple (« Changer le son »).
- **Test.** `LinkRowEverywhereTest#noSentenceDisguisedAsALink` : rouge `77c0007` (aucune des 5 lignes n'avait de chevron ; 7 TextButton à texte orange dans `export/` et `mixer/`), vert `bddd037`. Il vérifie à l'écran le chevron de chaque ligne, dans le code qu'aucun TextButton à texte orange ne reste dans `export/` et `mixer/` (le test de l'audit), et que Mes projets et « Mes pistes » appellent `LinkRow`.
- **Captures regardées.** `nav/sheet_share` (« Autres fichiers » et le .zip en lignes grises avec icône et ›, « Mes fichiers » sous Partager, aligné sur ses voisines), `scale130/fr_nav_simple_send` (téléphone 130 % : « Mes fichiers » sans sous-ligne, rien de coupé, Partager à l'écran), `partage/tabA11_130_loop_simple` (tablette 130 % : tout entier ; « Autres fichiers » coupé au bord de ce qui défile, il y a une suite), `projects` (« Importer un projet (.zip) » devenu une ligne grise avec son icône et ›), `nav/simple_song` (les cinq pistes en lignes grises : point de couleur, nom, « Changer le son » sur une ligne, ›, puis ▶ seul et le réglage).
- **Défaut vu puis corrigé.** Sur la première capture de `nav/simple_song`, « Changer le son » passait sur deux lignes à côté du bouton « Écouter seule » entier. Maintenant, quand « Changer le son » ne tient pas sur une ligne, le bouton ne garde que son ▶ (comme il le faisait déjà dans les cartes étroites).
- **Ce qui reste.** « Fichier final » n'est pas une `LinkRow` comme le rangeait l'audit : il s'ouvre sur place, c'est un repli (voir 2). Le lien « Mes fichiers » de la fenêtre « C'est prêt » est un bouton de fenêtre, pas une ligne : laissé tel quel. Les autres écrans de l'appli n'ont pas été parcourus (consigne).

## 2. Le repli qui résume (`Disclosure`, G4, règle 13)

- **Quoi.** Nouveau composant `ui-components/.../Disclosure.kt` : pleine largeur, 48 dp au moins, fond gris et bord fin, le nom, dessous le résumé de ce qu'il cache (une valeur changée en orange), chevron de 20 dp qui tourne, à droite. TalkBack lit le nom et le résumé et entend « développer » ou « réduire » (actions standard, aucun mot à traduire). Le résumé est **sous** le nom et non à sa droite (DECISIONS) : à 130 % sur un téléphone il passe à la ligne au lieu d'être coupé.
- **Où.** « Avancé » de la carte Exporter (« Piste par piste · FLAC · sans limiteur · MIDI · 48 kHz · 24 bits »), « Avancé : master pro » (« Plafond −1,0 dB · attaque et relâchement · grave en mono · écoute A/B · référence »), « Fichier final » (« 48 kHz · 24 bits »), « Effets » du master (les effets en place).
- **Test.** `DisclosureEverywhereTest#aClosedFoldSaysWhatItHides` : rouge `c72dc92` (« Avancé » ne disait que « Avancé »), vert `eb719f0`. Sur un téléphone de 360 dp à 130 % : fermé, chaque repli se lit nom puis résumé, dit qu'il s'ouvre, et aucun de ses mots n'est coupé ; touché, il dit qu'il se ferme. Le contrôle « mots coupés » du test a été changé dans le vert : il lisait la dernière mise en page d'un texte, qui peut être celle d'une mesure intrinsèque (« Avancé » mesuré 6 px de large) ; il lit maintenant l'écran comme `LayoutScan` (écrit dans le message du commit).
- **Captures regardées.** `mixer_export` (fermé : « Avancé » et son résumé, « FLAC » en orange quand il est choisi ; ouvert : « Fichier final » en repli avec « 48 kHz · 32 bits flottants », la ligne MIDI avec son icône piano et ›), `masterpro_panel` (« Avancé : master pro » ouvert, le résumé sur trois lignes, les valeurs changées en orange, rien de coupé), `masterpro_file` (« Fichier final » ouvert, « 96 kHz · 16 bits » en orange). `masterpro_panel_de` et `masterpro_file_de` ont changé de la même façon, pas regardées une à une.
- **Textes.** Nouvelle zone `composants` dans les 14 langues (9 mots : « avec la sortie générale », « sans limiteur », « Plafond %s dB », etc.) ; « Effets : %s » et « Fichier final : %s » retirés des catalogues (plus utilisés). Anglais et néerlandais relus pour le sens (« no limiter », « Ceiling %s dB », « bass in mono below %d Hz » ; « zonder limiter », « Plafond %s dB », « bas in mono onder %d Hz »), cohérents avec les mots déjà là (« Bass in mono below », « Bas in mono onder »). Les autres langues sont de moi, sans relecture.
- **Ce qui reste.** « Pourquoi ? » du tuto n'est pas un `Disclosure` (DECISIONS : le panneau du tuto garde sa hauteur, tout bougerait, et il ne cache aucun réglage). Le « détails » du volume dans « Prêt à partager ? » reste un petit mot orange (il n'était pas dans la liste).

## 3. La pastille de piste (`TrackChip`, G7, règle 16)

- **Quoi.** Nouveau composant `ui-components/.../TrackChip.kt`, appelé par les trois copies : `EditorChip` (Batterie, Voix, Remix dans Sons), `AcidChip` (Acid 303, aussi au Clavier du Mode simple) et chaque piste de `TrackBar`. Hauteur de 44 dp au moins et 44 dp × la taille de police au plus, mots centrés sans marge au-dessus ni au-dessous ; largeur maximale 160 dp à 100 %, 160 × la police au-delà (jusqu'à 1,5) : un nom qui tient à 100 % tient à 130 %. Les boutons « Effets » et « + Piste » de la rangée suivent la même règle. La règle « REAL defect » de `CaptureMatrixTest` est retirée.
- **Pourquoi un plafond** (DECISIONS) : la police de remplacement de Robolectric est plus haute que la vraie ; sans plafond, la rangée grandissait sous les tests et quatre parcours (`TutoKickSoundTest`, `BenchAmeneesTest`, `JourneyPathsTest`, `SpacesTest` pour la ligne-lien) ne trouvaient plus leurs boutons. Le plafond suit la police : il ne coupe pas les vraies lignes (vérifié par les scans en vraie police). Une première version gardait 4 dp de marge : « Plaits » en arabe et « シンセ », « 신스 », « 合成器 » étaient rognés à 100 % (`CaptureMatrixPhone360Test` tracks_ar/ja/ko/zh-CN_1.0) ; corrigé dans `7eb177a`.
- **Tests.** `SonsChipsTabletScanTest#sonsChips` (et ses variantes tablette 8 pouces, tablette 800 dp, plus deux nouvelles : téléphone 411 et 360 dp), de la police 1,0 à 1,5 : rien de coupé, raccourci ni perdu dans la rangée. Rouge `5ea9629` (12 cas rouges de 1,25 à 1,5, « 16 pads » rogné en bas), vert `8cf5cb5`. `TrackChipEverywhereTest#theTrackChipsAreOneComponent` (les trois copies appellent `TrackChip`, plus de `.height(44.dp)`), même paire. **Honnêteté** : quand le rouge a tourné, j'avais déjà modifié `AppSpaces.kt` et `AcidLook.kt` dans l'atelier (le test lit les sources à l'exécution) : il n'a donc signalé que `TrackBar.kt`. Il était rouge pour la bonne raison, mais sur une seule des trois copies.
- **Captures regardées.** `nav/sons` (Batterie, Acid 303 « Ligne de basse » entier, Voix, Remix au bord avec le fondu), `scale130/fr_nav_sons_synth` (téléphone 130 % : « Synthé / Synthé », « Basse / Basse ronde », « Lead / Lead » entiers, deuxième ligne pleine), `tabA11/130_nav_sons_synth` (tablette 130 % : Voix, Remix, Synthé, Basse, Lead, deuxième ligne entière : le défaut de la photo b4e53e1f n'y est plus), `acid_simple_clavier` (Mode simple, Clavier : pastilles entières, la première au bord sous le fondu). Pas regardées une à une : les autres captures de Sons (`tablet/`, `tabA11/land_*`, `nl_nav_sons*`).
- **Ce qui reste.** Le test général de l'audit « aucun `.height()` fixe autour d'un Text » sur tous les écrans (LayoutScan) n'est pas écrit : il ne couvre ici que les pastilles. La capture `scale130/fr_sons_chips` dans la recette n'est pas faite (la recette appartient au jalon).

## 4. Une seule icône Partager (G5 pour l'icône, règle 15)

- **Quoi.** `Icons.Filled.Share` partout : barre du haut, barre du pouce, bouton Partager de la feuille, onglet « Partager » du Mode simple (c'était l'avion, le mineur de la relecture du lot « partage »), Pad Live (« Plus » et le partage d'une prise), vidéo, rapport de bug, Mes projets. « Partager mon morceau » de Mes projets n'est plus vert : couleur des boutons (orange).
- **Test.** `ShareIconEverywhereTest#oneShareIconEverywhere` : aucun `IosShare` dans l'appli ni dans `ui-components` (le grep de l'audit), l'onglet et la page Partager portent `Icons.Filled.Share`, jamais en vert. Rouge `7896bfc` (7 fichiers avec `IosShare`), vert `fb0540d`.
- **Captures regardées.** `nav/simple_song` et `scale130/fr_nav_simple_send` (onglet « Partager » avec l'icône d'Android), `nav/sons` (barre du haut et barre du pouce), `projects` (icône et texte orange). Les quelque 200 autres images changées ne diffèrent que par cette icône dans les barres : pas regardées une à une.

## 5. Les règles 12 à 22 (`docs/DESIGN.md`)

Ajoutées après le principe 11, mot pour mot comme l'audit les écrit (§ 6), avec une ligne d'origine. La section Composants cite les trois nouveaux et l'icône.

## 6. Ce qui reste de la ligne de l'audit (hors consigne)

- **M4** : la carte Exporter de Mix est encore verte (titre, puces, bouton). Rien changé.
- **M2** : le gabarit commun des cartes d'accueil (médaillon rond 48 dp) et les captures `welcome.png`, `nav/launch_simple.png`. Rien changé.
- Les mots de G5 (« Exporter », « Envoyer les bouts ») : chantier 3.

## 7. Données et recensement

- `docs/DECISIONS.md` : 9 lignes ajoutées à la fin.
- `verdicts.tsv` : pas touché ; aucune ligne de la liste ne parle des pastilles, des liens ou de l'icône.
- **Plafond du recensement relevé** : `NavScreenshotTest.welcomeMode` CONTRAST de 4 à 7, contre la règle de l'en-tête, raison écrite dans la ligne. Les sept textes sont ceux des cartes de l'accueil lus pendant leur fondu d'entrée (« Écouter une démo » 1,22:1). L'accueil n'est pas touché par ce lot, la capture `nav/welcome_mode.png` ne change pas et montre tout lisible. Sur la base, la même classe jouée seule donne 4 ; avec ce lot, 7, de façon répétable. **Je n'ai pas trouvé pourquoi** le moment de la lecture a bougé. À faire valider par le relecteur.

## 8. Contrôles

| Contrôle | Résultat |
|---|---|
| Suite complète en un passage : `:project-model:test :app:testDebugUnitTest :app:verifyRoborazziDebug` (PS_STALL_MIN=70), 29 min, tête `5e13d18` | app : 8 136 tests, **4 rouges**, 4 479 sautés ; project-model : 1 074 tests, 0 rouge |
| Les 4 rouges | `ScreenshotTest#recette`, `CutScreenshotTest#cutScreen` et `#cutScreenMagnifier`, `PadHoldAndTrackDeleteTest#scrolledThePadsFadeUnderThePartLine` : les quatre images instables connues, hors lot, non réenregistrées (déjà rouges sur la base, relecture du lot « partage »). Aucune autre image ne diffère. |
| `python3 -I scripts/bench/test_ultra_bench.py` | `ultra bench scripts: OK` |
| Images enregistrées | 252 images changées, commitées par chemin ; les 5 images instables (dont `remix_stem_edit_menu`) remises à l'identique avant le commit |
| Liens symboliques | aucun commité |

Pendant le travail, deux passages complets ont montré des rouges que les classes jouées seules n'ont plus montrés ensuite : `GestureFuzzTest` (AppNotIdle après « synth_sounds_save », puis après « scene_rename »), `Tour3RenameIdleTest`, `Tour3StudioRenameIdleTest`, `Tour3NameFieldIdleTest`, `HumanLucasBugsTest`, `BenchTour4Test`, `BenchShareTest` (état hérité). Ils sont verts au passage final. Le parcours du test au hasard dépend des étiquettes à l'écran, que ce lot change : il ne prend plus le même chemin.

## 9. Classes à rejouer à la fusion

`LinkRowEverywhereTest`, `DisclosureEverywhereTest`, `TrackChipEverywhereTest`, `ShareIconEverywhereTest`, `SonsChipsTablet8ScanTest`, `SonsChipsTablet800ScanTest`, `SonsChipsPhone411ScanTest`, `SonsChipsPhone360ScanTest`, `CaptureMatrixPhone360Test` (et les autres `CaptureMatrix*`), `SpacesTest`, `TutoKickSoundTest`, `JourneyPathsTest`, `BenchAmeneesTest`, `GestureFuzzTest`, `ScreenScanTest`, `Lot2TextsTest`, `SharePathTest`, `ShareSheetTest`, `MasterProScreenshotTest`, `NavScreenshotTest` (et ses variantes), `ScreenshotTest`, `UiScaleScreenshotTest`, `TabletScreenshotTest`, et côté catalogues `:project-model:test` (`I18nCatalogTest`, `LexiconWordsTest`).

## 10. Ce dont je ne suis pas sûr

- Le plafond du recensement de l'accueil (section 7) : cause non trouvée.
- La hauteur des lignes-liens et des pastilles est réglée en partie pour la police de remplacement des tests (pas de marge verticale) : sur un vrai téléphone, les captures en vraie police sont bonnes, mais je n'ai pas vu d'appareil.
- Les traductions des 12 autres langues.
- `GestureFuzzTest` : rouge deux fois en suite complète pendant le travail, vert au passage final et seul. Je ne peux pas exclure un vrai défaut d'attente sur un chemin que le hasard prend rarement.

## 11. Essais à demander à Benjamin plus tard (pas maintenant)

1. Tablette à 130 %, Mode Pro, Sons : les pastilles des pistes montrent-elles leurs deux lignes entières (le défaut de la photo b4e53e1f) ?
2. Mode Pro, Mix, carte Exporter : « Avancé » se voit-il, et ce qui est écrit dessous dit-il ce qu'il y a dedans ?
3. Mode simple, Mon morceau : toucher une piste ouvre-t-il bien ses sons (la ligne grise avec ›) ?

## Après relecture (2026-11-10)

Relecture : `tour-5-composants-relecture.md` (1 grave, 3 moyens). Tout est corrigé dans l'atelier, rien n'est poussé.

| Constat | Ce qui a changé | Preuve |
|---|---|---|
| **Grave 1** : le contrôle « aucun mot coupé » de `DisclosureEverywhereTest` était aveugle | Deux trous, pas un : le test tournait avec les polices de remplacement de Robolectric (un texte y mesure quelques pixels, rien ne finit jamais par « … »), et il lisait la dernière mise en page gardée par le texte (celle d'une mesure intrinsèque). Il tourne maintenant en polices réelles (`GraphicsMode.NATIVE`) et remesure chaque texte à la largeur réelle de son nœud. | Rouge `25b0b4d` vu avec la mutation 5 de la relecture (résumé `maxLines = 1`, `Ellipsis`) : « Piste par piste · FLAC · MIDI · 96 kHz · 24 bits (à 840 px : 1 ligne, « … ») » ; vert `eb1d069` sur le code tel qu'il est. `docs/REVIEW.md` dit que le CUT de LayoutScan a les mêmes deux trous ailleurs. |
| **Moyen 2** : plafond CONTRAST de l'accueil relevé de 4 à 7 sans cause | Cause trouvée, et ce n'était pas un fondu : attendre 2 s avant le scan ne changeait rien. Sous Robolectric, la capture de la fenêtre plein écran de l'accueil rendait l'écran du dessous (« Mes pistes »), vérifié en enregistrant les deux captures : identiques. Les textes de l'accueil étaient donc mesurés sur le gris des lignes-liens. `LayoutScan` redessine une telle fenêtre depuis sa propre vue quand sa capture est identique à celle de l'écran. | Rouge `9e23ed3` (plafond remis à 4 : rouge dans les passages complets du lot, 7 > 4 + 2), vert `cdfc040` : l'accueil lit 0 CONTRAST, plafond baissé à 0 ; recensement complet vert (302). La ligne fausse de DECISIONS est remplacée par une nouvelle. D'autres plafonds de dialogues peuvent être trop hauts maintenant (noté dans REVIEW). |
| **Moyen 3** : « Écouter seule » réduit à ▶ sur téléphone | Le bouton garde ses mots, sur deux lignes (« Écouter » au-dessus de « seule ») quand une ligne ne tient pas ; ▶ seul seulement si même son mot le plus long ne tient pas. | `4a79ee1`. Captures `nav/simple_song` (téléphone : « Écouter / seule » sur toutes les pistes, « Changer le son » sur une ligne, rangées de même hauteur) et `tablet/nav_simple_song` regardées. `SpacesTest` vert. Pas de test dédié (choix de dessin). |
| **Moyen 4** : un repli fermé ressemblait à une ligne-lien (›) | `Disclosure` montre ⌄ fermé et ⌃ ouvert ; le › reste à la `LinkRow`. | Dans `eb1d069`. Capture `mixer_export` regardée : « Avancé ⌃ » ouvert, « Fichier final ⌄ » fermé, la ligne MIDI avec › juste dessous : on les distingue. Pas de test dédié. |
| Mineur 7 : rôle du repli | `role = Role.Button` sur le `Disclosure`. | Dans `eb1d069`. |
| Mineur 8 : orange de « Fichier final » | Orange seulement si la fréquence ou les bits diffèrent de ceux du format (`finalFileChanged`), plus pour l'artiste ou la pochette. | `37e8c1a` ; `mixer_export` : « 48 kHz · 32 bits flottants » en gris ; `masterpro_file` : « 96 kHz · 16 bits » en orange (choisi). |
| Mineurs 5, 6, 9, 10 | Non changés, une ligne chacun dans `docs/REVIEW.md`. | — |

Captures changées par ces corrections (20, toutes commitées dans `6b226ed`) : les replis (`mixer_export`, `mixer_master`, `masterpro_*`, `scale130/*_mixer_master`, `tablet/mixer`, `tablet/land_mixer`, `tablet/nav_mix`) et « Mes pistes » (`nav/simple_song`, `nav/alone_*`, `nav/guide_simple`, `nav/help_labels_simple`, `nav/simple_more`, `nav/simple_track_delete`, `tablet/guide_simple`, `tablet/nav_simple_song`). Regardées : `nav/simple_song`, `tablet/nav_simple_song`, `mixer_export` ; les autres ne changent que par ces deux éléments.

Passage complet en enregistrement après ces corrections (toute la suite, 30 min) : vert, aucun rouge. Passage ciblé de contrôle (`verifyRoborazziDebug` sur NavScreenshotTest, UiScaleScreenshotTest, TabletScreenshotTest, ScreenshotTest, MasterProScreenshotTest, ScreenScanTest, les quatre `*EverywhereTest`, SpacesTest, SonsChips*, CaptureMatrixPhone360Test) : 1 829 tests, 1 rouge, `ScreenshotTest#recette` (image instable connue, hors lot).
