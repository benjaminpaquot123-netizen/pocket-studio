# Tour 6, lot 2 « tuto-consignes » : relecture (troisième lecture, delta depuis ac5cd517)
Relu : 10f5b2a9433832c4884851d9225dc2d1410e42c6

Relecteur neuf (n'a écrit aucun de ces commits). Delta relu : 4d0b1e88 (décisions des équipes regroupées en un fichier), 4b082679 (fusion de l'intégration `ccr-80af1a1b-mn0wec` : lots morceauexport et cablage) et 10f5b2a9 (capture `tuto_app_bass` réenregistrée). Les deux premières lectures (ac5cd517 et avant) ne sont pas refaites ici ; leurs moyens 1, 2 et 4 et leurs mineurs restent tels qu'écrits dans le rapport (« Laissés de côté »).

Rappel : le grave de la première relecture (T2, `tutoKitQuotes` qui remplaçait la citation d'un autre bouton dans 5 langues) est **corrigé** depuis 27cdf86f, toujours en place à 10f5b2a9 (`app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt:127` : renommage seulement quand la phrase française et sa traduction citent chacune une seule chose).

## Ce qui a été vérifié

- **Regroupement des décisions (4d0b1e88)** : comparaison ligne à ligne de `docs/decisions/tour-6-tutoconsignes.md` avec `git show 9c3a47b7:docs/decisions/tour-6-tutoconsignes-a.md`, `-b.md` et l'ancien fichier principal. Aucune ligne de choix perdue : les 22 lignes de l'équipe A et les 22 de l'équipe B sont reprises mot pour mot. Seules disparaissent l'en-tête de B (« Une ligne par choix : choix · raison · retour arrière. ») et, dans la 1re ligne, le renvoi aux deux annexes. Plus aucun renvoi à `-a.md` / `-b.md` dans le dépôt (hors copies de travail `.claude/worktrees`). Le rapport (ligne 3) renvoie au seul fichier.
- **Capture (10f5b2a9)** : seule la zone 905..1175 × 306..438 px change : la rangée des pistes montre « ♪ Son » et une flèche « → » au lieu de la note seule et du « + » coupé. Ça vient de `TrackBar.kt` (intégration, R19 / « sound word »), pas du lot. Le panneau du tuto, la grille, la consigne « Encore 4 notes en pointillé, plus haut ↑ » (`PianoRoll.kt`, lot 1, hors lot) sont identiques au pixel près. Rien de faux ajouté par le lot.
- **Fusion de l'intégration (4b082679)** : elle ne touche aucun fichier du lot (ni `TutoPanel.kt`, `TutoController.kt`, `TutoChecks.kt`, tutos, i18n tuto, `PianoRoll.kt`, `RhythmScreen.kt`, `DefaultNames.kt`). Les interactions avec le tuto passent par `RhythmViewModel.kt` :
  - `loadTutoKit` (c3cc0188) : un pad à voix de studio Rumble ou 808 garde le nom « Rumble » / « 808 » avec le son du pack. « Rumble » est dans `DefaultNames.EXACT` (`project-model/.../DefaultNames.kt:40`). `padName` l'affiche donc traduit (ja ランブル, ko 럼블), exactement comme le cite la consigne traduite (`i18n/ja/tuto_techno.json` : 「ランブル」). `tutoKitQuotes` n'y touche pas : « Rumble » n'est pas un nom du kit intégré (`DrumKitInfo.names`), et `tutoPadNames` non plus. Le kick reste « Techno · Kick », renommé par `tutoPadNames` comme avant. Aucun conflit avec la correction T2.
  - `tutoEdit` (N25 : aller dans une partie pendant la lecture) et `madeHash` : sans effet sur le texte ou les vérifications du tuto ; couverts par `Lot6VmTutoPartTest`.
- **Tests rejoués ici** (une file Gradle, résultats à HEAD) : `Lot6VmTutoKitNameTest`, `Lot6VmTutoPartTest`, `Tour6TutoPadNamesTest`, `Tour6TutoPanelQuoteOrderTest`, `Tour6TutoPanelPadQuotesTest` : verts (6 tests, 0 échec). Je n'ai pas relancé verifyRoborazzi, ni le self-test du banc, ni le portail. Les chiffres de l'auteur (176 verts, rejeu t4-26 « oracle tenu ») ne sont pas contrôlés ici.

## Bloquant

Aucun.

## Grave

Aucun.

## Moyen

Aucun.

## Mineur

1. **La décision T2 décrit l'ancienne règle.** `docs/decisions/tour-6-tutoconsignes.md:38` dit « seulement si les deux phrases ont autant de citations ». Depuis 27cdf86f, le code ne renomme que si chaque phrase cite une seule chose (`TutoPanel.kt:127`). Le rapport (ligne 15) dit juste, mais le journal des décisions décrit la règle qui avait causé le grave. Il manque une ligne « T2, suite (relecture) ». Le retour arrière noté (« rendre `shown` tel quel ») reste valable.
2. **Des lignes dépassées restent sans marque, et le regroupement les place après celles qui les remplacent.** `docs/decisions/tour-6-tutoconsignes.md:24` dit que jump up garde « Le morceau » sous l'exemption `PINNED`. Or la ligne 6 dit le contraire, et c'est elle qui est vraie : `Tour6TutoTabNamesTest.kt:33` a `PINNED = emptySet()`. Avant 4d0b1e88, la ligne 24 était dans une annexe d'équipe, la ligne 6 dans le fichier principal. Maintenant, un lecteur qui lit de haut en bas tombe sur l'ancienne règle en dernier. Les lignes 25 (T3, « règle coupée ») et 26 (T8, « découpes annulées ») sont aussi remplacées par « T3, fin » et « T8, fin », mais là, on comprend l'ordre en lisant la suite. Le plus simple : préfixer les lignes 24 à 26 par « (remplacé ci-dessous / ligne 6) ».
3. **Forme du fichier regroupé.** Les lignes des équipes n'ont pas de date, contrairement aux 6 premières. Celles de B gardent le séparateur « · » (choix · raison · retour arrière) et parfois « — » comme retour arrière. La 1re ligne dit « plusieurs équipes », le rapport « quatre équipes ». Sans effet sur l'index du portail.
4. **Capture `tuto_app_bass` (hors lot, pour mémoire).** La flèche « → » de la rangée des pistes est coupée par le bord droit, comme le « + » avant elle. Ça vient de l'intégration (R19, déjà relu dans ce lot-là). Le lot n'a rien à y faire.

Verdict : 0 bloquant, 0 grave — fusion possible
