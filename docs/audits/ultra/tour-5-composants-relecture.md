# Lot « composants » : relecture adversariale du correctif
Relu : 37287246e2378f99bf4c030777f13a083c3422f9

Date : 2026-11-10 · second relecteur neuf (n'a écrit ni le code, ni le correctif, ni la première relecture) · delta relu : `git log 3deb02f..3728724` (9 commits) · rien n'est poussé, aucun code corrigé.

## Ce qui a été rejoué

| Contrôle | Résultat |
|---|---|
| Passage ciblé à la tête : `:app:verifyRoborazziDebug` sur ScreenScanTest (302, recensement), CaptureMatrixPhone360Test (305), NavScreenshotTest (30), SpacesTest (9), MasterProScreenshotTest, TabletScreenshotTest, UiScaleScreenshotTest, ScreenshotTest, et tous les `*EverywhereTest` (Disclosure, LinkRow, TrackChip, ShareIcon, MuteSolo, StillPads, Delete, RemixSong) | 1 204 tests, **1 rouge : `ScreenshotTest#recette`** (image instable connue, hors lot), tout le reste vert, aucune autre image différente |
| Mutation A (celle de la première relecture) : résumé du `Disclosure` en `maxLines = 1, overflow = Ellipsis` | DisclosureEverywhereTest **rouge** pour la bonne raison : « Piste par piste · FLAC · MIDI · 96 kHz · 24 bits (at 840 px: 1 lines, « … » true, lines dropped or cut true) » |
| Mutation B : résumé plafonné en hauteur (`Modifier.heightIn(max = 18.dp).clipToBounds()`) | DisclosureEverywhereTest **rouge** : « (at 840 px: 2 lines, « … » false, lines dropped or cut true) » |
| Mutation C : résumé sur une seule ligne sans « … » (`softWrap = false`) | DisclosureEverywhereTest **reste vert** : voir constat moyen n° 1 |
| Mutation D : sous-titre de l'accueil « Ton studio de musique, dans ta poche. » en `Neon.SurfaceHigh` (presque la couleur du fond) | ScreenScanTest : le détail `app/build/recensement/NavScreenshotTest.welcomeMode.txt` montre « CONTRAST « Ton studio de musique, dans ta poche. » 1.82:1 » : LayoutScan lit bien les pixels **de l'accueil** (et non plus ceux de « Mes pistes »). Le recensement reste vert à cause de la marge d'ordre des tests (`ORDER_MARGIN = 2`, existante, hors lot) |
| Plafonds (`git diff 3deb02f..3728724`) | une seule ligne de `recensement-acceptes.tsv` change (`welcomeMode CONTRAST` 7 → 0) ; aucune autre ligne du recensement, aucune règle `Allowed` de CaptureMatrix, aucun autre test d'audit touché |
| Images instables (`recette`, `cut_screen`, `cut_screen_magnifier`, `remix_stem_edit_menu`, `rhythm_scrolled_under_part`) | aucune dans le diff (20 PNG, tous attendus : replis et « Mes pistes ») |
| Captures regardées à l'outil Read | `nav/simple_song`, `tablet/nav_simple_song`, `mixer_export`, `masterpro_file`, `scale130/fr_mixer_master`, `scale130/fr_nav_simple_song`, `tabA11/130_nav_simple_song`, `nav/welcome_mode` |

Chaque fichier muté a été remis seul (`git checkout HEAD -- <fichier>`) ; `git status` propre ensuite (hors lignes « T » des liens third_party).

## Les quatre points de la première relecture

1. **Grave n° 1 (contrôle « aucun mot coupé » aveugle) : corrigé.** Le test tourne en `GraphicsMode.NATIVE` (`xxhdpi`) et remesure chaque texte du repli avec un `TextMeasurer` à la largeur réelle de son nœud ; « … » et lignes perdues en hauteur sont vus (mutations A et B rouges). Reste le trou horizontal (moyen n° 1 ci-dessous), qui n'est pas le cas « … » du grave.
2. **Moyen n° 2 (plafond de l'accueil 4 → 7 sans cause) : corrigé, et la paire est honnête.** `9e23ed3` ne touche que le plafond (7 lu > 4 + 2 de marge : rouge) ; `cdfc040` apporte la cause et la correction (`LayoutScan` redessine depuis sa propre vue une fenêtre dont la capture est identique à celle du premier écran) et met le plafond à 0. La correction est juste pour l'accueil : c'est un `ScaledDialog` plein écran dont le contenu est une `Surface(color = Neon.Background, Modifier.fillMaxSize())` opaque, donc sa vue seule est bien ce que voit l'utilisateur. Elle ne s'applique qu'aux fenêtres de même taille dont la capture est identique pixel pour pixel (échantillonnée tous les 7 px) à celle de l'écran, c'est-à-dire précisément au cas faux ; la mutation D montre qu'un vrai défaut de contraste y est encore lu. `nav/welcome_mode.png` regardée : tous les textes de l'accueil sont lisibles, 0 est crédible. DECISIONS garde l'ancienne ligne et la déclare fausse dans une nouvelle : honnête pour un journal.
3. **Moyen n° 3 (« Écouter seule » réduit à ▶) : corrigé sur téléphone 400 dp et tablette.** `nav/simple_song` : « ▶ Écouter / seule » sur deux lignes sur les cinq pistes, « Changer le son » sur une ligne, bouton dans la hauteur de la ligne ; `tablet/nav_simple_song` : idem ; `tabA11/130_nav_simple_song` : « Écouter seule » sur une ligne. À 130 % sur 360 dp (`scale130/fr_nav_simple_song`, inchangée par le correctif) le bouton reste ▶ seul (cas prévu, phrase d'aide et TalkBack le disent) : voir mineur n° 3.
4. **Moyen n° 4 (repli pareil à une ligne-lien) : corrigé.** `mixer_export` : « Avancé » ouvert avec ⌃, « Fichier final » fermé avec ⌄, la ligne MIDI juste dessous garde son ›, on les distingue. `role = Role.Button` présent (mineur 7). Orange de « Fichier final » (mineur 8) : gris pour « 48 kHz · 32 bits flottants » (WAV par défaut) et « 48 kHz · 24 bits » (FLAC par défaut) sur `mixer_export`, orange pour « 96 kHz · 16 bits » choisi sur `masterpro_file` ; `finalFileChanged` compare au résumé des options par défaut, c'est juste.

## Bloquant

Aucun.

## Grave

Aucun.

## Moyen

1. **DisclosureEverywhereTest ne voit pas un résumé coupé à droite sans « … ».** Preuve : mutation C (`softWrap = false` sur le résumé) reste verte, alors que la mutation A prouve que ce résumé ne tient pas sur une ligne de 840 px (il serait donc rogné au bord du repli). Le contrôle remesure avec `softWrap` et `maxLines` d'origine et ne regarde que `isLineEllipsized`, `didOverflowHeight` et la hauteur ; jamais `didOverflowWidth` ni la largeur. Le composant est correct aujourd'hui (le résumé passe à la ligne), d'où « moyen » : la règle 13 « jamais coupé » n'est gardée que sur deux axes sur trois. **Correction proposée** : ajouter `again.didOverflowWidth || again.size.width > n.size.width + 1` à `dropped` (ou un troisième drapeau), puis rejouer la mutation C : rouge attendu. Le même trou est à ajouter à la ligne de `docs/REVIEW.md` sur le CUT de LayoutScan.

## Mineur

2. `ExportOptions.kt` : le KDoc « « 48 kHz · 24 bits », the card's line before it is opened. » est resté au-dessus du nouveau KDoc de `finalFileChanged` : deux KDoc de suite, `finalFileSummary` a perdu le sien. Cosmétique.
3. `scale130/fr_nav_simple_song` (360 dp, 130 %) : « Écouter seule » y est ▶ seul (le calcul `narrow` le prévoit), et pourtant « Changer le son » passe quand même sur deux lignes (« Changer le / son »). Ni coupé ni nouveau (capture inchangée par le correctif), mais le but écrit dans le code (« Changer le son » sur une ligne) n'est pas tenu là. Une question de recette suffit.
4. Le recensement tolère `ORDER_MARGIN = 2` au-dessus de chaque plafond quand l'écran a au moins une ligne acceptée : le plafond 0 de l'accueil laisse donc passer jusqu'à deux textes peu contrastés (mutation D verte). Existant, hors lot ; à garder en tête en lisant « plafond 0 ».
5. `LayoutScan.ownPixels` : la couleur d'un pixel est lue sans son alpha ; une fenêtre plein écran **transparente** redessinée depuis sa vue serait lue sur du noir. Aucun cas aujourd'hui (l'accueil est opaque, recensement et matrice verts), à noter si un dialogue plein écran translucide apparaît.

## Ce qui a été vérifié et tient

- Les deux paires rouge/vert du correctif (`9e23ed3`/`cdfc040`, `25b0b4d`/`eb1d069`) sont rouges puis vertes pour la raison écrite ; je l'ai rejoué pour la seconde (mutation A).
- Aucun plafond monté ni baissé en douce : une seule ligne changée, vers le bas.
- Aucune image instable réenregistrée ; les 20 PNG du diff sont ceux annoncés dans le rapport.
- `docs/REVIEW.md` et `docs/DECISIONS.md` disent ce qui reste (CUT de LayoutScan, autres plafonds de dialogues peut-être trop hauts, mineurs gardés) ; seul le trou horizontal du moyen n° 1 y manque.

---

# Première relecture (916b009)

Relu : 916b00920712c57e9a8e68c811d1a58782b9f2f5

Date : 2026-11-10 · relecteur neuf (n'a pas écrit le code) · base `fx-int` (merge-base 594be3d) · rien n'est poussé.

### Ce qui a été rejoué

| Contrôle | Résultat |
|---|---|
| Passage ciblé à la tête, `:project-model:test` (I18nCatalogTest, LexiconWordsTest) et `:app:verifyRoborazziDebug` sur LinkRowEverywhereTest, DisclosureEverywhereTest, TrackChipEverywhereTest, ShareIconEverywhereTest, SonsChips{Tablet8,Tablet800,Phone411,Phone360}ScanTest, CaptureMatrixPhone360Test (305), NavScreenshotTest (30), MasterProScreenshotTest, GestureFuzzTest, SpacesTest, TutoKickSoundTest, ScreenScanTest (302, recensement), UiScaleScreenshotTest | **tout vert**, aucune image différente |
| Mutation 1 : chevron retiré de `LinkRow` | LinkRowEverywhereTest **rouge** pour la bonne raison (les 5 lignes sans chevron) |
| Mutation 2 : `TrackChip` plafonné à 44 dp fixes (`heightIn(max = 44.dp)`) | SonsChipsTablet8 et SonsChipsPhone411 **rouges** à 1,25, 1,3 et 1,5 (« 16 pads » rogné), verts de 1,0 à 1,2 : le scan voit bien le défaut de la photo b4e53e1f |
| Mutation 3 : `AcidLook.kt` remis à la version de la base (copie à la main) | TrackChipEverywhereTest **rouge** (`acid/AcidLook.kt`) : le test voit aujourd'hui les trois copies, pas seulement `TrackBar.kt` comme lors du rouge 5ea9629 |
| Mutation 4 : icône Partager de Mes projets remise en vert (`tint = Neon.Acid`) | ShareIconEverywhereTest **rouge** (`ProjectsScreen.kt:311`) |
| Mutation 5 : résumé du `Disclosure` limité à une ligne avec « … » (`maxLines = 1, overflow = Ellipsis`) | DisclosureEverywhereTest **reste vert** : voir constat grave n° 1 |
| Images instables (`recette`, `cut_screen`, `cut_screen_magnifier`, `remix_stem_edit_menu`, `rhythm_scrolled_under_part`) | aucune n'est dans le diff du lot |
| Captures regardées à l'outil Read | `nav/simple_song` (et celle de la base), `nav/sheet_share` (et la base), `mixer_export`, `masterpro_panel`, `projects`, `nav/sons` (rangée des pastilles comparée à la base : identique à 100 %), `scale130/fr_nav_simple_send`, `tabA11/130_nav_sons_synth`, `tabA11/130_nav_simple_song` |

Fichiers mutés remis un par un (`git checkout HEAD -- <fichier>`) ; `git status` propre ensuite (hors lignes « T » des liens third_party).

### Bloquant

Aucun.

### Grave

1. **Le contrôle « aucun mot coupé » de DisclosureEverywhereTest ne voit pas un résumé coupé en « … ».** Preuve : mutation 5 ci-dessus. Les deux résumés (« Piste par piste · FLAC · MIDI · 96 kHz · 24 bits » et « Plafond −0,5 dB · attaque et relâchement · … ») ne tiennent évidemment pas sur une ligne à 130 % sur 360 dp (à 100 % sur 400 dp, `mixer_export` passe déjà à la ligne), et pourtant le test reste vert. Le contrôle a été changé dans le commit vert eb719f0 : il passe maintenant par `LayoutScan.scan(…CUT)`, mais LayoutScan lit le **même** `GetTextLayoutResult` que l'ancien contrôle, c'est-à-dire, ici, la dernière mise en page de la mesure intrinsèque (celle qui donnait « Avancé » à 6 px). Avant, ce piège donnait un faux rouge ; maintenant, un faux vert pour le « … ». Le vert affirme dans le rapport et dans le KDoc « son résumé n'est jamais coupé » : ce n'est pas vérifié. Le composant lui-même est correct aujourd'hui (pas de `maxLines` sur le résumé), d'où « grave » et non « bloquant » : c'est la garde de la règle 13 (recette tel-06) qui ne garde rien.
   **Correction proposée** : dans `closed(…)`, ne plus juger sur la mise en page en cache ; par exemple remesurer chaque texte du repli avec un `TextMeasurer` à la largeur réelle du nœud (`node.size.width`) et vérifier que la hauteur obtenue tient dans `node.size.height` sans ligne coupée, ou comparer l'encre rendue (`captureToImage`) au cadre du nœud. Puis rejouer la mutation 5 : le test doit rougir. Noter aussi dans `docs/REVIEW.md` que le CUT « ellipsized » de LayoutScan est aveugle partout où un parent mesure ses enfants en intrinsèque après la vraie mise en page (risque pour la matrice de captures entière).

### Moyen

2. **Plafond CONTRAST de `NavScreenshotTest.welcomeMode` relevé de 4 à 7 sans cause trouvée**, contre la règle d'en-tête de `recensement-acceptes.tsv` (« it never goes up »). Le recensement est vert à la tête (ScreenScanTest 302 tests). Hypothèse probable (non prouvée) : l'accueil du premier lancement est dessiné au-dessus du Mode simple, dont « Mes pistes » compose maintenant 5 `LinkRow` avec `WholeWordsText` et une mesure de plus (`measurer.measure("Changer le son")`) ; ce coût de composition déplace le moment où le scan lit les cartes pendant leur fondu. **Correction proposée** : faire attendre au recensement la fin du fondu de l'accueil (avancer l'horloge de la durée de l'animation avant le scan), remettre le plafond à 4, et vérifier sur la base que le chiffre ne bouge pas.

3. **« Mes pistes » (Mode simple) sur téléphone : « Écouter seule » a perdu ses mots sur toutes les lignes.** `ALONE_ROW_ROOM` a pris 56 dp ; sur `nav/simple_song` (téléphone 400 dp, la classe de celui de Benjamin) il ne reste qu'un ▶ vert à côté du › gris de la ligne : deux flèches côte à côte, l'une ouvre les sons, l'autre fait jouer. Base : « ▶ Écouter seule » en entier. La phrase d'aide de la carte et TalkBack (« Écouter « X » seul, en boucle ») compensent en partie, et la tablette garde les mots. C'est un recul de lisibilité pour un débutant, justement ce que l'audit de prise en main veut éviter. **Correction proposée** : laisser « Changer le son » passer sous le nom sans compter la largeur du chevron et des marges de la ligne dans le calcul (`maxOf(word, change)` + 56 dp est trop prudent), ou donner au bouton ses mots sur une seconde ligne ; au minimum, ajouter la question à la recette du jalon.

4. **Un repli fermé ressemble à une ligne-lien.** `Disclosure` fermé tourne son chevron de −90° : il montre ›, sur un fond gris arrondi, exactement comme `LinkRow`. Sur `mixer_export`, « Fichier final › » (s'ouvre sur place) et « Exporter les notes en fichier MIDI › » (part vers le menu de partage d'Android) sont l'un sous l'autre et se lisent pareil. Les règles 12 et 13 distinguent « ouvre ailleurs » et « s'ouvre ici » ; le dessin ne les distingue plus (seule la bordure fine diffère). **Correction proposée** : chevron ⌄ fermé et ⌃ ouvert pour `Disclosure` (convention Android), et garder › pour `LinkRow` seulement.

### Mineur

5. `TrackChip` : hauteur 44 dp minimum (cible tactile < 48 dp, comme avant le lot : pas une régression) et un `heightIn(max = 44 dp × police)` qui contredit le texte de la règle 16 (« `heightIn(min)` et padding »). Le plafond suit la police et la matrice 360 dp en 15 langues (hi compris) est verte ; la mutation 2 montre que les scans en police réelle voient bien un rognage. Risque résiduel : une police système d'appareil plus haute que celle de Robolectric. Proposition : écrire l'exception dans la règle 16 (ou dans DECISIONS) et ajouter la question à la recette.
6. `LinkRow` sans marge haute ni basse : quand titre et sous-ligne prennent plusieurs lignes, les mots touchent le bord du fond gris (« Sauvegarder le morceau en .zip », sous-ligne sur deux lignes, `nav/sheet_share`). Pas de texte coupé (la coupure visible là est le bord de ce qui défile, déjà sur la base). Proposition : 6 à 8 dp de marge verticale, la hauteur minimale de 56 dp restant le plancher.
7. `Disclosure` : `clickable` sans `role = Role.Button` ni `onClickLabel` ; les actions développer/réduire sont bien là (vérifiées par le test), TalkBack dira « Appuyez deux fois pour activer ». Proposition : `role = Role.Button`.
8. « Fichier final » et la fin du résumé d'« Avancé » passent en orange dès qu'une option quelconque de `ExportOptions` diffère du défaut (artiste, pochette, rapport…), même si fréquence et bits sont ceux par défaut : l'orange désigne alors une valeur qui n'a pas changé.
9. Traductions des 12 langues faites par l'agent : relues en diagonale (de, es, it, pt-BR, nl, pl, id, tr, ar, hi, ja, ko, zh-CN), rien de faux repéré ; I18nCatalogTest et LexiconWordsTest verts.
10. La règle 13 ajoutée dit qu'un réglage dont dépend le résultat (format) n'est jamais sous « Avancé », alors que FLAC et « Piste par piste » y sont toujours (hors consigne, M4/chantier suivant) : à noter dans la liste de ce qui reste pour que la règle ne soit pas lue comme déjà tenue.

### Ce qui a été vérifié et tient

- Les quatre rouges l'étaient pour la bonne raison (mutations 1 à 4 ; le rouge 5ea9629 n'avait vu qu'une copie, le test voit aujourd'hui les trois).
- Aucune image instable réenregistrée ; les captures regardées montrent les lignes-liens, replis et pastilles entiers ; la rangée des pastilles de `nav/sons` est identique à la base à 100 %, et entière à 130 % sur tablette.
- Rouges intermittents signalés par l'auteur (GestureFuzzTest, renommages) : GestureFuzzTest vert dans mon passage ciblé ; pas d'autre constat.

Ancien verdict : 0 bloquant, 1 grave — fusion impossible

Verdict : 0 bloquant, 0 grave — fusion possible
