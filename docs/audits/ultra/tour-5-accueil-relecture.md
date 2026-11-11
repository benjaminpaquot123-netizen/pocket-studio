# Relecture neuve du lot « accueil » (tour 5, chantier 6)
Relu : f181875bc5f203713b40010a901f02adbc69b7ed

Relecteur neuf (n'a écrit ni le code ni le rapport). Lu : `git diff 1cf5b6fc...2f6fc8a0` (4 commits : 415fe129 rouge,
1d110baf correctif des textes, d809ee9e vert, 2f6fc8a0 captures et rapport), le rapport de l'auteur, `ScrollHint.kt`,
`NewProjectPage.kt`, `ProjectsScreen.kt`, `LaunchChoice.kt`, `Welcome.kt`, `AppRoot.kt` (quickStyle, playOnOpen, goHome),
`SoundLibraryViewModel.quickCreate`, `LayoutScan.kt`, les trois nouveaux tests.

## Catalogue de textes « cassé par le lot Plus »
- Constaté sur un worktree jetable à 1cf5b6fc (supprimé ensuite) : `I18nCatalogTest` est ROUGE sur le lot « plus » seul,
  1 échec sur 13, `noTranslationIsLeftUnused` : « en/nav.json: Renommer le morceau » (traduction que plus rien n'affiche).
  Origine : d30b939b (lot plus) retire l'entrée « Renommer le morceau » du menu sans retirer ses 14 traductions.
- Réparé par 1d110baf : retrait de la clé dans les 14 `nav.json` (c'est ce qui remet `I18nCatalogTest` au vert). Le même
  commit remplace « الدليل » par « المرشد » dans `ar/plus.json` : cela ne concerne pas `I18nCatalogTest` mais
  `LexiconWordsTest` (le mot du lexique pour le Guide), lui aussi introduit par d30b939b.
- Précision : la commande `:app:testDebugUnitTest --tests '*I18nCatalogTest'` ne trouve aucun test (« No tests found ») :
  la classe vit dans `project-model` ; la bonne commande est `:project-model:test --tests '*I18nCatalogTest'`. Le rapport
  de l'auteur la range parmi les tests « touchés » sans le dire (mineur, à corriger dans la liste à rejouer).

## Tests rejoués (scripts/bench/gradle-file.sh, classes ciblées, tête 2f6fc8a0)
- `:project-model:test` `*I18nCatalogTest` (13/13) et `*LexiconWordsTest` (7/7) : verts.
- `:app:testDebugUnitTest` `*HomeScreensSayTheyScrollTest` (5/5), `*OneTouchStyleTest` (3/3), `*OneTouchSongPlaysTest`
  (le parcours joué, 19 s ; `drive` sauté comme prévu sans `-Phuman.dir`) : verts.

## Mutations (2, toutes deux tuées, code remis en place)
- M1 : `ProjectsScreen` sans le correctif (`choosingStart = startedFor == currentId`) : `OneTouchSongPlaysTest` échoue
  au 2e morceau (« nothing to touch: one_touch_techno », l'écran montre « Mes projets »). Le défaut corrigé est bien gardé.
- M2 : `ScrollHintState.recount` ne compte plus rien (seuil + 100000 px) : 3 tests de `HomeScreensSayTheyScrollTest`
  échouent (« ↓ La suite » au lieu de « ↓ 6 autres » / « 5 autres » / « 4 autres »). Le compte est vraiment vérifié.

## Captures regardées (7)
`projects_new.png`, `projects_new_preparing.png`, `nav/launch.png`, `scale130/fr_welcome.png`, `nav/land_launch.png`,
`tablet8/projects_new.png`, et la version de base de `nav/land_launch.png`. Cinq styles visibles sans défiler sur
360 × 780, la carte tient en une ligne sur tablette, la puce est lisible à 130 %, rien de coupé par le lot.

## Points cherchés
- Lecture qui démarre sans qu'on l'attende : seulement après un toucher sur un style (carte « prêt à écouter »), par le
  même `PendingNotice.playOnOpen` que « Créer et écouter ». Retour système bloqué pendant la préparation
  (`ProjectsScreen`, `BackHandler` si `creating == null`). En quittant vers l'accueil, `goHome` arrête la musique.
- Double création : les puces sont désactivées pendant la préparation et `quickCreate` refuse un 2e appel
  (`creating != null`). Rien trouvé.
- Plantages : pas d'accès non protégé ; l'état de la puce vit dans le `remember` de la liste, les cartes retirées
  sont oubliées (`DisposableEffect`). Rotation : `rememberScrollState` est sauvegardé.
- Animations coupées : `LocalReducedMotion` ou réglage système, `scrollTo` sans animation (pas de test dédié, mineur).

## Points restants (mineurs, pour docs/REVIEW.md)
- M-a. La puce (coin bas droit, liste en haut) recouvre la partie droite de la carte coupée : sur « Autres façons de
  commencer » ouvert, la droite de la carte « Techno » ; un toucher là fait défiler au lieu de créer. Sur un petit écran
  où la liste déborde de peu, elle pourrait couvrir la moitié droite de « Plus tard : voir le rythme de départ ».
  La carte reste touchable ailleurs et la puce disparaît au premier défilement.
- M-b. `LayoutScan` retire la puce de la liste des boutons pour `overButtons` et ignore les textes sous la puce ou le
  fondu : aucune règle automatique ne dirait qu'une puce recouvre un bouton entier. Une règle « puce sur un contrôle
  entièrement à l'écran » serait un bon garde-fou.
- M-c. `nav/land_launch.png` (téléphone couché) montre la carte « Continuer » vide ; c'était déjà le cas à la base
  1cf5b6fc, ce n'est pas ce lot, à regarder à part.
- M-d. Le rapport range `*I18nCatalogTest` sous l'app ; c'est `:project-model:test`.

rejeu changé : t4-26-prise-clavier : le nouvel accueil de l'assistant décalait les gestes 3, 4 et 7 ; recalculés en pixels sur la nouvelle mise en page ; oracle inchangé ; rejoué vert.

## Delta après fusion de fx-int

Relu `git diff 2f6fc8a0 HEAD` : hors apports de fx-int, seuls changent les `cmd-*.txt` du rejeu t4-26-prise-clavier (gestes 3, 4, 7, et 29 à 32 en aval, coordonnées seules) et le rapport (une ligne). L'oracle n'est pas touché. Les deux merges de fx-int (69a91848, f181875b) n'ont aucune résolution manuelle (`git show --cc` vide). Preuve du rejeu relue : key_62 « Ré 4 », key_65 « Fa 4 », record_stop, puis « oracle tenu ».

Verdict : 0 bloquant, 0 grave — fusion possible
