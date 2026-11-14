# Tour 6 — lot « tuto4 » : relecture neuve
Relu : f83308049bd23fabe56450dc971c258ad60f4c98

Relecture par un sous-agent neuf qui n'a pas écrit le lot : commits du lot depuis `49e7c7996` (branches `t4-house`, `t4-techno`, fusions, rapport, décisions), lus avec les catalogues hors tuto (`i18n/*/*.json`), `RhythmScreen.kt` (feuille du pad, menu du coup), `SimpleMix.kt`, `ExportCheck.kt`, `ShareSheet.kt`, `Loudness.kt`, `TutoPanel.kt`, `TutoChecks.kt` et `AppRoot.kt` (niveau du tuto et Mode simple).

## Ce qui tient
- Fichiers : hors des catalogues `tuto_house` / `tuto_techno` (14 langues), seuls `tutos/house.json`, `tutos/techno.json`, les deux tests, le rapport et les décisions ; `CLAUDE.md`, `AUDIT-ULTRA.md` et `GABARIT-FICHES-V3.md` viennent de la fusion de l'intégration (307759b37), pas du lot. Aucun Kotlin de l'appli.
- Les phrases collent aux données et aux écrans : 8 cases du « Rim » (le test les compte sur l'indice de l'étape), 8 mesures et 4 pour la Montée, chapitre 12 « Les transitions » en techno (le ch. 15 Pro seul est après), « Créer la partie « %s » » (`TutoPanel.kt:386`), « Plus tard » de la carte du kit (`TutoPanel.kt:357`), menu du coup « Force / Doux / Roulement / Triolet » (`RhythmScreen.kt:1062-1075`), repères « +4 / 0 / −12 / −∞ » (`SimpleMix.kt:316,484`), « Pas de saturation. », « Niveau : %s », « aucun solo oublié » (`ExportCheck.kt`, `ShareSheet.kt:493`), « Effets du pad » › « Envoi réverbe ».
- M6 sans piège : l'étape « reverb » est Pro seulement (`levels: p`), et lancer le tuto Pro passe en Mode pro (`AppRoot.kt:811,823,2073`), où « Effets du pad » est affiché (`RhythmScreen.kt:1168`). La cible techno `clap` seule correspond à la phrase (pas d'« Accords » exigé).
- Langues : fr, ja, de lus en entier sur les phrases changées ; ar, hi, zh-CN, ko, it, es échantillonnés. Les libellés cités sont ceux des catalogues de chaque langue (ja パッドのエフェクト, リバーブ・センド, シェア, 音割れはありません, ベロシティ / ソフト / ロール / 3連符 ; de Pad-Effekte, Reverb-Send, Teilen, Keine Übersteuerung, Velocity / Sanft / Roll / Triole ; ar مؤثرات الباد, إرسال صدى المكان ; zh-CN 打击垫效果, 混响发送 ; hi garde « Rim » et « Clap » en latin comme son catalogue). Script : les 56 phrases neuves existent dans les 14 langues traduites, aucun reste de français, chiffres identiques (écarts seulement « 1 » écrit en chiffre en ja/ko/id), aucun `%` perdu.
- Les tests lisent les données (indice de l'étape, `lines` du cours, `song`, catalogues réels), pas des tautologies ; chaque méthode a sa paire rouge / vert.

## Bloquant
Aucun.

## Grave
Aucun. Aucun test existant ne change de couleur parmi ceux relancés ; ni sauvegarde ni son touchés ; aucune phrase ne mène un débutant vers un bouton absent.

## Moyen
- (corrigé, voir le delta) M7 pas raccord entre les deux tutos : la house dit « Partage ton morceau », la techno garde le titre « Exporte ton morceau » et son Pro dit « touche « Partager », exporte ton morceau et partage-le ». Responsable : zone tuto (techno), prochain lot du tuto.
- `Tour6Tuto4HouseTest#musicianWordsAreExplainedAtFirstSight` ne vérifie l'explication qu'en français ; les autres langues sont seulement « traduites » (lues à la main ici : ja et de ont bien les parenthèses). La techno compte les parenthèses par langue : même garde à ajouter côté house.
- House, « share » : « « −14 » veut dire aussi fort que la musique en streaming » ; l'écran ne montre « (−14) » que si le morceau est entre −16 et −12 LUFS (`Loudness.kt:28-34`) ; ailleurs le chiffre annoncé n'apparaît pas. Pas de blocage (aucun geste demandé), mais la phrase peut décrire un libellé absent.

## Mineur
- House « make_montee » Pro : « … jusqu'au silence d'avant le Drop » promet encore le silence que le ch. « Les transitions » fera ; la techno a été corrigée (« viendra avec les transitions »).
- House « make_drop2 » dit « Créer la partie » ; le bouton lit « Créer la partie « Drop 2 » » (la techno cite le libellé entier). de écrit « Teil Drop 2 erstellen » sans les guillemets intérieurs.
- ko et it/es : guillemets imbriqués identiques (« ““빌드업” 파트 만들기” », « “Crea la parte “Intro”” ») : lisible, peu élégant.
- Le rapport range en « pas pris » « en Mode simple, « Effets du pad » est caché » : sans objet, l'étape est Pro seulement et le tuto Pro met le Mode pro.

## Relancé et vu
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :project-model:test --rerun` sur d709a3256 : BUILD SUCCESSFUL, 1147 tests, 0 échec, 1 ignoré (un premier passage sans `--rerun` était resté « up-to-date », non compté).
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Tuto*'` sur d709a3256 : BUILD SUCCESSFUL en 7 min 58 s, 372 tests, 0 échec, 3 ignorés (`Tour6TutoPanelAlreadyTest` vert dans cet ordre).
- `python3 scripts/bench/rapport-zone.py eq/tour-6-tuto4 docs/audits/ultra/tour-6-tuto4-rapport.md` : sortie 0.
- rouge constaté : 07e274167 : rejoué sur 4c4b20a44 (parent du vert 1a41c4260, `git worktree` jetable) : `Tour6Tuto4HouseTest#theNumbersAreTheStepsOwn` échoue sur son assertion, « fr: the order step does not say the parts' length comes next ».
- rouge constaté : 18ca0438f : rejoué sur 18ca0438f (parent du vert 078e11b81, `git worktree` jetable) : `Tour6Tuto4TechnoTest#reverbSendFromThePadsEffects` échoue, « 30 reverb steps that do not lead to the pad's effects » (fr « dans Mix … sur la tranche du pad « Clap » »).
- Arbres jetables retirés ; l'arbre de `wt-tuto4` n'a pas été touché avant ce commit.

## Delta e7355a9d → f18962ad
Commits relus : f1cbb3003 (rouge, `Tour6Tuto4TechnoTest#theShareChapterSaysShareLikeTheHouse`) et f18962ad8 (vert).
- Moyen n° 1 (M7) : **corrigé**. `tutos/techno.json`, chapitre « export » : titre « Partage ton morceau », Pro « À toi : touche « Partager » et envoie ton morceau. », mêmes clés que la house ; plus aucun « export » dans le titre ni les étapes du chapitre.
- Vert minimal : 2 lignes dans `tutos/techno.json` et, dans les 14 `i18n/*/tuto_techno.json`, la seule clé « À toi : touche « Partager », exporte ton morceau et partage-le. » retirée. Les 15 JSON touchés se lisent (`json.tool`).
- Clé retirée vraiment inutilisée : aucun `tutos/*.json` ne la cite plus ; les 35 autres styles gardent leurs propres phrases (« À toi : exporte ton morceau et partage-le. » et sa variante M4A/WAV), qui restent traduites dans leurs catalogues.
- Traduction : `I18n.table` fusionne toutes les zones (`AREAS`, dont `tuto_house`), donc le titre et le Pro de la techno se lisent ja « 曲をシェアしよう » / « あなたの番：「シェア」をタップして曲を送ろう。 », de « Teile deinen Track » / « Du bist dran: Tippe auf „Teilen“ und schick deinen Track. » ; le test vérifie les 14 langues.
- Le test lit les vraies données (chapitre, étapes, tables de langue) : pas une tautologie.
- rouge constaté : f1cbb3003 : rejoué sur f1cbb3003 (parent du vert f18962ad8, `git worktree` jetable, retiré ensuite) : `theShareChapterSaysShareLikeTheHouse` échoue sur son assertion, « 2 share texts: fr: « Exporte ton morceau » ; fr: « À toi : touche « Partager », exporte ton morceau et partage-le. » ».
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :project-model:test --rerun` sur f18962ad8 : BUILD SUCCESSFUL, 1148 tests, 0 échec, 1 ignoré.

(verdict de la relecture précédente, sur f18962ad : 0 bloquant, 0 grave)

## Delta c14c4ab04 → f431efc40 (allers-retours après les sièges de contrôle)
Relecture neuve de `git diff c14c4ab04 f431efc40`, commits du lot seulement (premier parent, sans les fusions de l'intégration ae8727624) : textes des tutos (`tutos/*.json`, catalogues `i18n/*/tuto*.json`) et le Kotlin `DefaultNames.kt` (`KIT_VOICES`, `shownPad`), `GuideTracks.kt` (`padShownName`, `shownName`), `MixTips.kt`, `TrackSheet.kt` (`sheetName`, `sheetTargets`), `WhatPlays.kt`, `AppRoot.kt` (`simpleTracks`, révélation de « Régler le son de la piste ↓ »), `TutoPanel.kt` (partie traduite dans « Nommer la partie », zone de texte de 120 dp), `TutoChecks.kt` (croix du trou sur la partie de l'étape), `RhythmScreen.kt` (appui long sur une case vide), `SynthScreen.kt`, `PlainWords.kt`.

### Ce qui tient
- Noms tapés par l'utilisateur jamais traduits : un pad n'a pas de nom libre ; `shownPad` lit d'abord l'échantillon posé par `shownSample` (un fichier de l'utilisateur reste tel quel), puis la voix studio, puis la voix du kit traduite. `KIT_VOICES` est la même liste que `DrumKitInfo.names` (16 voix, même ordre) et entre dans `DefaultNames.TEXTS`, donc dans la garde des traductions. Les autres pistes passent encore par `DefaultNames.shown`.
- Le Mix (conseils, Mode simple), « Ce qui joue » et la fiche de piste nomment un pad comme le pad l'affiche, avec la même règle que `padName` de `RhythmScreen.kt` (seul écart : une voix studio inconnue donne la voix du kit au lieu d'un nom vide, sans effet aujourd'hui).
- Appui long sur une case vide (`RhythmScreen.kt:819-827`) : il pose le coup (`cycleVelocity`, fort comme avant) ET ouvre son menu ; le menu ne s'affiche que quand le coup existe (`rollStep == step && hit != null`), donc aucune image avec un menu vide. Hors du tuto, rien ne reposait sur « appui long = coup fort sans menu » : les seuls appuis longs de cellule dans les tests (`ScreenshotTest` step_8, `TutoJourneyTest` cellules « Doux ») visent une case déjà frappée ; `GestureFuzzTest` et `GestureBudgetTest` sont relancés verts ; les libellés d'accessibilité (« Réglages du coup ») et le glossaire (« appui long sur une case » pour la force) restent justes.
- Une étape qui demande « Doux » ne se coche pas sur un coup fort (`SOFT_HITS_CHECKED`, `onlySoftnessMissing`) : le menu ne se referme donc pas avant « Doux » (le cas « autre » de Fd-1 ne touche qu'une étape sans « Doux »).
- `TutoHints.gridInSight` et `soundButtonInSight` : posés par un `DisposableEffect` et remis à faux au départ de l'écran et dans `reset()` ; ils changent une fois par écran, la clé de `held` les suit sans boucle de recomposition. La révélation du bouton du son ne se fait qu'au changement d'étape (`LaunchedEffect(tutoUi?.current?.id)`), jamais en boucle.
- Croix du trou (`toErase`) : seulement sur la partie de l'étape, comme sa coche (`done`, ligne 993) ; les 29 étapes « gap » des tutos ont une partie (ou la Montée par défaut, que les six styles sans `part` créent bien).
- Catalogues : les 15 JSON de tuto et de langue touchés se lisent ; aucun `%s` / `%d` perdu entre clé et traduction (script sur tous les fichiers changés).
- Tests réels : ils lisent les vraies données (cours, catalogues, écrans par le banc humain), pas des tautologies ; chaque vert a son rouge du même sujet.

### Bloquant
Aucun.

### Grave
Aucun. Aucun test relancé ne change de couleur ; ni la sauvegarde ni le son ne sont touchés (l'appui long pose le même coup qu'avant) ; aucun geste de plus pour un débutant dans le tuto.

### Moyen
- `KIT_VOICES` (`DefaultNames.kt`) recopie `DrumKitInfo.names` (`app`) : aucune garde ne vérifie que les deux listes restent égales ; une voix renommée d'un côté seulement ferait nommer un pad autrement au Mix qu'au pad. À couvrir par un test d'égalité. Responsable : zone Mix / noms, prochain lot qui touche les noms.
- Hors du tuto, poser un coup par appui long ouvre maintenant aussi le menu : un geste de plus (« Fermer » ou toucher à côté) pour qui posait des coups forts ainsi. Voulu (H3, décidé) ; à confirmer au prochain siège « pro » hors tuto.

### Mineur
- `gridInSight` / `soundButtonInSight` sont des drapeaux uniques : deux grilles composées en même temps (tablette, deux volets) se les partagent ; la première qui part les remet à faux. Sans effet vu aujourd'hui.
- Sur une étape du son, la révélation du bouton (`AppRoot.kt:2048`) et un « Montre-moi » de la même étape écrivent tous deux `tutoReveal` : le dernier gagne.

### Relancé et vu (sur f431efc40, tête fusionnée avec ae8727624)
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :project-model:test --rerun` : BUILD SUCCESSFUL, 1173 tests, 0 échec, 1 ignoré.
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Tuto*' --tests '*Rhythm*' --tests '*Gesture*' --tests '*TrackSheet*' --tests '*MixTips*' --tests '*WhatPlays*' --tests '*Synth*' --tests '*LongPress*' --tests '*SimpleMixPadName*' --tests '*PlainWords*'` : BUILD SUCCESSFUL en 9 min 37 s, 140 classes, 593 tests, 0 échec, 16 ignorés (aucune capture rouge, donc rien à comparer sur ae8727624).
- rouge constaté : 6f0c3644c : rejoué sur a8dce6a0e (parent du vert a4fd94ff6, `git worktree` jetable) : `Tour6TutoCellsPanel100Test#theKickStepsDottedCellsAreWholeAboveThePanelAt100` échoue sur son assertion, « 2 dotted cells cut » (fr, téléphone, police 100, panneau ouvert sur l'étape du kick).
- rouge constaté : 96b5fa133 : rejoué sur 81da2f9f3 (parent du vert 0fbfe3f13, même arbre jetable) : `Tour6LongPressEmptyCellTest#aLongPressOnAnEmptyCellPutsTheHitAndOpensItsMenu` échoue sur son assertion, « the hit's menu not open after one long press ».
- Un redémarrage de la machine a coupé le premier passage des deux rejeux ; relancés à la main, résultats ci-dessus. Arbre jetable retiré (`git worktree remove`) ; `wt-tuto4` non modifié avant ce commit.
- `rejeu-defauts.sh t6-ha19-cases-panneau` non relancé ici (le rapport le donne « oracle tenu » sur 0e250438b, et la fusion ne touche ni `TutoPanel.kt` ni `RhythmScreen.kt`) ; le test `Tour6TutoCellsPanel100Test` qui en reprend l'oracle est vert sur la tête.

### Delta 166f0def → 33473d12a
- Seul apport : la fusion propre de la tête d'intégration `09e2f2b0c` (lot coquille4, 16 fichiers). Lu : `git diff 166f0defb HEAD`, dont les trois morceaux de coquille4 dans `AppRoot.kt` : tailles des paquets de la bibliothèque (~l. 549) et remix en attente (~l. 1381) écrits sur le fil principal ; fenêtre d'avis (~l. 1095) : bouton renommé `notice_ok` → `notice_close`, phrase reliée à « Allonger le morceau » par `HelpAbout`.
- Aucun croisement avec tuto4 : ses deux morceaux d'`AppRoot.kt` (révélation du bouton du son de la piste, désormais l. 2055 ; noms des pads de `simpleTracks`, l. 4791) sont intacts et loin de ceux de coquille4 ; les drapeaux du panneau (`TutoHints.knobs/strip/soundButtonInSight`, `tutoReveal`) ne sont touchés par aucune ligne de coquille4. Plus aucune trace de `notice_ok` dans `app/src` (aucun test tuto4 ne s'en servait).
- Le mineur « `AppRoot.kt:2048` » ci-dessus se lit maintenant l. 2055.
- Pas de relance : l'équipe donne `:project-model:test` et `:app:testDebugUnitTest` (Tuto, LongPress, SimpleMixPadName, MixTips, TrackSheet, WhatPlays) à 1598 tests, 0 échec, sur cette tête ; rien dans le delta n'en fait douter.

### Delta 33473d12 → 52c74b082
- Lu : `git diff 33473d12a 52c74b082` (deux commits : rouge a97ff3713, vert 52c74b082).
- Rouge a97ff3713 (`Coquille2SeatTest#cymbalGridLineNamesThePadShown`) : seule la mise en place change (ajout pur, aucune ligne retirée) : le morceau est construit comme le fait le cours jusqu'à l'étape `gap` (`TutoChecks.show` étape par étape), puis la Montée est montrée, avec une garde « no Montée made ». Les vérifications d'origine (`saysThePadShown`, « au moins un → à l'écran » et « il nomme le pad montré », grille tournée vers le kick puis cymbale) sont identiques : pas d'affaiblissement. Raison juste : depuis f2e3ce8aa, un coup posé sur la partie A n'est pas celui du trou et ne tourne rien.
- rouge constaté : a97ff3713 rejoué dans un `git worktree` jetable (parent du vert) : `cymbalGridLineNamesThePadShown` échoue sur son assertion, « turned to the kick: no « → » on the screen » (10 tests, 1 échec) ; arbre jetable retiré (`git worktree remove`).
- Vert 52c74b082 (`TutoPanel.kt`, une condition) : `cellsToTouch` vaut aussi quand la grille est en vue et qu'il reste des coups à barrer (`TutoHints.erase`), comme pour les cases pointillées (T4). Minimal ; même chemin que T4, donc le même comportement : la hauteur du panneau est reprise quand le dernier coup est barré (clé de `held`), comme quand la dernière case pointillée est touchée. Rien côté son ni sauvegarde.
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Coquille2*' --tests '*Tuto*' --tests '*Rhythm*'` sur 52c74b082 : BUILD SUCCESSFUL, tâche à jour (résultats de 13 h 48–13 h 55 UTC, après le vert, mêmes entrées) : 97 classes, 423 tests, 0 échec, 11 ignorés, dont `cymbalGridLineNamesThePadShown`, `Tour6TutoCellsPanel100Test` (T4), `Tour6TutoSoundButtonInSight*` et `Tour6TutoKnobLeftTest` (H5, H7) verts.

### Delta 52c74b08 → f2a9a41ec
- Lu : `git diff 1a2d9ea95 f2a9a41ec` (deux commits : rouge 392e17a37, vert f2a9a41ec ; `TutoPanel.kt` et le nouveau `Tour6TutoReadAllTest.kt`).
- Rouge 392e17a37 (`Tour6TutoReadAllTest#readOnShowsTheWholeInstructionInOneTouch`, ja, techno Facile, étape `clap_do`, téléphone) : le test exige d'abord que la consigne soit coupée dans la petite pièce (`boundsInRoot.height < size.height - 1`), puis entière après un seul « Lire la suite » : il ne peut pas passer à vide.
- rouge constaté : 392e17a37 rejoué sur son propre arbre (parent du vert f2a9a41ec, `git worktree` jetable, retiré après) : 1 test, 1 échec sur la seconde assertion, « after one « Lire la suite »: 83.0 px of the instruction's 252 in sight » (la première tient : la consigne était bien coupée).
- Vert f2a9a41ec : `lowRoom` = l'ancienne condition (cases à toucher / coups à barrer / boutons du son en vue) ; `cellsToTouch = lowRoom && !readAll`. Dans la petite pièce, « Lire la suite » met `readAll` à vrai (la hauteur maximale redevient 240 dp, `held` repart de 0 par sa clé `cellsToTouch` et se remesure une fois) ; ailleurs il fait défiler comme avant. `readAll` est gardé par `remember(step.id, ui.open, ui.progress.level)` : remis à faux à chaque étape, à chaque repli / réouverture et au changement de niveau. Écrit seulement au toucher : pas de boucle de recomposition.
- T4 tient avant le toucher (cases au-dessus du panneau, `Tour6TutoCellsPanel100Test` vert). Après le toucher, des cases peuvent passer sous le panneau : acceptable, c'est l'utilisateur qui a choisi de lire, et replier le panneau rend la petite pièce. Si la consigne dépasse encore 240 dp, « Lire la suite » reste et fait défiler (`cellsToTouch` faux).
- Mineur (noté, sans correction) : `readAll` n'est pas remis à faux quand `lowRoom` change dans la même étape (dernière case touchée puis nouvelles cases) ; sans effet vu, la pièce normale reste lisible.
- `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Tour6TutoReadAllTest' --tests '*Tour6TutoCellsPanel100Test' --tests '*Tour6TutoPanel*'` sur f2a9a41ec : BUILD SUCCESSFUL en 1 min 14 s, 22 classes, 33 tests, 0 échec, 1 ignoré (l'`assumeTrue` du banc humain de `Tour6TutoCellsPanel100Test`, déjà là).

## Delta f2a9a41e → 2d39f0f4a

- Aucun code du lot après f2a9a41ec : seulement la fusion propre de l'intégration d106ac26f (3ef5cab9a, `git show --cc` vide, aucune résolution de conflit ; CI 43612440874 verte ; rejeux t4-26, t6-da15, t6-ec17, t6-ga14, t6-ha19 « oracle tenu ») puis un commit de documents (2d39f0f4a).
- Sièges de contrôle Da-11 (50 fichiers) et Da-10 (580 fichiers) copiés sous `sieges/tour-6/controle/` : tous ajoutés, rien d'écrasé (seul le rapport du lot est modifié).
- Rapport de Da-11 : « code testé : 31aa3661 », « bloquant ou grave : aucun », « Lire la suite » corrigé au ch. 3 (écran 9, consigne entière affichée) et retour sur les cases corrigé (écran 11).
- Rapport du lot : une seule ligne `controle: Da-11 | code testé: 31aa3661 | bloqués: 0 | graves neufs: 0`, les anciens sièges gardés en prose comme preuves ; `rapport-zone.py eq/tour-6-tuto4 … --tuto` : sortie 0.
- Mineur (noté, sans correction) : le lien « 続きを読む ⌄ » reste affiché après lecture complète (finition vue par Da-11).

## Delta 2d39f0f4 → f8330804

- Lu : `git diff f9ab00eac f83308049` (deux commits : rouge 1e953171e, vert f83308049 ; `TutoPanel.kt` et `Tour6TutoReadAllTest.kt`, 20 lignes ajoutées, 1 retirée).
- rouge constaté : 1e953171e : « Lire la suite » still offered after the touch that opens the words (vu rouge sur son assertion par l'auteur avant le vert ; non rejoué par cette relecture, économie de quota).
- Rouge 1e953171e (`Tour6TutoReadAllTest#nothingIsLeftToReadOnAfterThatTouch`, ja, techno Facile, étape `clap_do`, téléphone 392 × 852 dp) : il attend d'abord les cases pointillées et « Lire la suite », touche le lien une fois, puis exige que le lien ne soit plus offert : il ne peut pas passer à vide.
- Vert f83308049 : une constante `READ_ALL_MAX = 380.dp` et une branche de plus dans `most` (`… else if (readAll) READ_ALL_MAX else 240.dp`). Minimal. Sans effet quand `readAll` est faux : la carte du morceau (290 dp), la petite pièce (120 dp) et la pièce normale (240 dp) passent avant ou restent telles quelles ; `readAll` n'est écrit qu'au toucher de « Lire la suite » dans la petite pièce et retombe à chaque étape, repli et changement de niveau. Si les mots dépassent encore 380 dp, le lien reste et fait défiler comme avant.
- Mineur de la section précédente (« 続きを読む ⌄ » resté après lecture complète) : **corrigé** par ce vert.
- Tests ciblés donnés verts par l'équipe après le vert (ReadAll, CellsPanel100, Tour6TutoPanel*, BannerChain) ; non relancés ici, rien dans le delta n'en fait douter.

### Moyen
- Petit téléphone (640 dp de haut environ) : le panneau est une colonne de la barre du bas du `Scaffold` (`AppRoot.kt:2549-2615`), sans autre plafond que la hauteur de l'écran, et ses boutons d'action sont mesurés après les mots. Avec 380 dp de mots, le panneau ouvert peut approcher 500 à 560 dp (titre, barre de progression, « Pourquoi ? », boutons, marges) ; avec la barre des onglets dessous, une très longue consigne en grande police pourrait serrer ou couper les boutons du bas. Pas grave : la pièce suit la longueur des mots (380 dp seulement pour une consigne qui les remplit), elle ne s'ouvre que par le toucher de l'utilisateur, et le repli (en haut du panneau, toujours visible) ou l'étape suivante rendent la petite pièce ; un débutant n'est pas bloqué. À couvrir par un test sur un écran de 640 dp à 130 % (plafonner la pièce à une part de la hauteur de l'écran). Responsable : zone tuto, prochain lot du panneau.

Verdict : 0 bloquant, 0 grave — fusion possible
